/*
 * Copyright (C) 2013 Mobilinkd LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobilinkd.tncconfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

public class Avr109 extends Thread {
	// Debugging
	private static final String TAG = "Avr109";
	private static final boolean D = false;

	private final BluetoothSocket mSocket;
	private final Firmware mFirmware;
	private final Handler mHandler;
	private final Context mContext;
	private int mBlockSize;

	private final InputStream mInStream;
	private final OutputStream mOutStream;

	private static final int ESCAPE = 27; 			// <esc>
	private static final byte ACKNOWLEDGED = 13; 	// <cr>
	private static final int GET_BOOTLOADER = 83; 	// 'S'
	private static final int GET_SW_VERSION = 86; 	// 'V'
	private static final int GET_PROG_TYPE = 112; 	// 'p'
	private static final int GET_DEV_LIST = 116; 	// 't'
	private static final int GET_SIGNATURE = 115; 	// 's'
	private static final int GET_INCREMENT = 97;	// 'a'
	private static final int GET_BLOCK_SIZE = 98;	// 'b'
	private static final int CMD_CHIP_ERASE = 101; 	// 'e'
	private static final int CMD_START_PROG = 80; 	// 'P'
	private static final int CMD_LEAVE_PROG = 76; 	// 'L'
	private static final int CMD_EXIT_LOADER = 69; 	// 'E'
	private static final int CMD_SET_ADDRESS = 65;	// 'A'
	private static final int CMD_WRITE_BLOCK = 66;	// 'B'
	private static final int CMD_READ_BLOCK = 103;	// 'g'
	// private static final int CMD_GET_CRC = -1;		// TODO add CRC support.
	
	public static final char MEMTYPE_EEPROM = 'E';
	public static final char MEMTYPE_FLASH = 'F';
		
	private static final byte[] DEVICE_SIGNATURE = {(byte)0x0f, (byte)0x95, (byte)0x1e};
	private static final String BOOTLOADER_SIGNATURE = "XBoot++";
	

	public Avr109(Context context, BluetoothSocket socket, Firmware firmware, Handler handler) {
		if (D) Log.d(TAG, "CREATE");
		mSocket = socket;
		mFirmware = firmware;
		mHandler = handler;
		mContext = context;
		
		InputStream tmpIn = null;
		OutputStream tmpOut = null;

		// Get the BluetoothSocket input and output streams
		try {
			tmpIn = mSocket.getInputStream();
			tmpOut = mSocket.getOutputStream();

		} catch (IOException e) {
			Log.e(TAG, "temp sockets not created", e);
		}

		mInStream = tmpIn;
		mOutStream = tmpOut;
	}
	
	public void run() {
		if (D) Log.d(TAG, "BEGIN");

		PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Mobilinkd:FirmwareUpload");

		wakeLock.acquire(10*60*1000L /*10 minutes*/);
		   
		if (initialize() && loadFirmware() && verifyFirmware()) {
			if (D) Log.i(TAG, "Success");
		} else {
			if (D) Log.i(TAG, "Failure");			
		}
		exitBootloader();
		
		wakeLock.release();
	}

	private byte[] read(int len, long timeout) {
		long expiration = System.currentTimeMillis() + timeout;
		byte[] buffer = new byte[len];
		int pos = 0;
		while (pos != len && System.currentTimeMillis() < expiration) {
			try {
				if (mInStream.available() != 0) {
					int c = mInStream.read();
					if (c == -1) {
						break; // EOF
					}
					// Log.d(TAG, "read: " + Integer.toString(c));
					buffer[pos++] = (byte) c;
					continue;
				}
				Thread.sleep(1);
			} catch (IOException e) {
				Log.e(TAG, "read() failed", e);
				break;
			} catch (InterruptedException e) {
				Log.w(TAG, "read() interrupted", e);
				break;
			}
		}

		byte[] result = new byte[pos];
		if (pos > 0)
			System.arraycopy(buffer, 0, result, 0, pos);
		return result;
	}

	private boolean verifyCommand() {
		byte[] answer = read(1, 10000);
		return ((answer.length == 1) && (answer[0] == ACKNOWLEDGED));
	}

	private boolean verifyAndLog(String command) {
		boolean result = verifyCommand();

		if (result) {
			if (D) Log.d(TAG, command + " succeeded");
		}
		else {
			if (D) Log.e(TAG, command + " failed");
		}

		return result;
	}
	
	public boolean initialize()
	{
		mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_INITIALIZING).sendToTarget();
		
		String bootloaderSignature = getBootloaderSignature();
		
		if (bootloaderSignature == null)
		{
			Message msg = mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_INITIALIZATION_FAILED);
	        Bundle bundle = new Bundle();
	        bundle.putString(FirmwareUpdateActivity.TOAST, "Could not enter bootloader");
	        msg.setData(bundle);
	        mHandler.sendMessage(msg);
			return false;
		}
		
		String programmerType = getProgrammerType();
		if (D) Log.d(TAG, "Programmer type: " + programmerType);
		
		String softwareVersion = getSoftwareVersion();
		if (D) Log.d(TAG, "Software version: " + softwareVersion);
		
		boolean autoIncrement = hasAutoIncrement();
		if (D) Log.d(TAG, "Has auto-increment: " + Boolean.toString(autoIncrement));
		
		mBlockSize = getBlockSize();
		if (D) Log.d(TAG, "Block size: " + Integer.toString(mBlockSize));

		byte[] deviceSignature = getDeviceSignature();
		
		if (!Arrays.equals(deviceSignature, DEVICE_SIGNATURE))
		{
			Message msg = mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_INITIALIZATION_FAILED);
	        Bundle bundle = new Bundle();
	        bundle.putString(FirmwareUpdateActivity.TOAST, "Wrong device signature");
	        msg.setData(bundle);
	        mHandler.sendMessage(msg);
			return false;
		}
		
		mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_INITIALIZED).sendToTarget();
		return true;
	}
	
	public boolean loadFirmware() {
		mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_LOADING).sendToTarget();
		
		List<Firmware.Segment> segments = mFirmware.getSegments();
		
		assert(mBlockSize != 0);
		
		if (!enterProgrammingMode()) {
			Log.e(TAG, "Could not enter programming mode");
			Message msg = mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_LOAD_FAILED);
	        Bundle bundle = new Bundle();
	        bundle.putString(FirmwareUpdateActivity.TOAST, "Could not enter programming mode");
	        msg.setData(bundle);
	        mHandler.sendMessage(msg);
	        return false;
		}
	    
	   	if (!eraseChip()) {
			Log.e(TAG, "Could not erase chip");
			Message msg = mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_LOAD_FAILED);
	        Bundle bundle = new Bundle();
	        bundle.putString(FirmwareUpdateActivity.TOAST, "Could not erase chip");
	        msg.setData(bundle);
	        mHandler.sendMessage(msg);
	        leaveProgrammingMode();
	        return false;
		}

		for (Firmware.Segment segment: segments) {
			
			setAddress(segment.address);
			int address = segment.address;
			
			for (int i = 0; i < segment.data.length; i += mBlockSize) {
				
				mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_ACTIVE, segment.data.length, i).sendToTarget();

				int blockSize = mBlockSize;
				if (i + blockSize > segment.data.length) {
					blockSize = segment.data.length - i;
				}
				
				if (D) {
					Log.d(TAG, "loadFirmware: address = " + Integer.toHexString(address) +
						", length = " + Integer.toString(blockSize));
				}

				byte[] segmentData = new byte[blockSize];
				System.arraycopy(segment.data, i, segmentData, 0, blockSize);

				if (!writeBlock(segment.memoryType, segmentData)) {
					Log.e(TAG, "Write failure at address " + Integer.toHexString(address));
					
					Message msg = mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_LOAD_FAILED);
			        Bundle bundle = new Bundle();
			        bundle.putString(FirmwareUpdateActivity.TOAST, "Write failure");
			        msg.setData(bundle);
			        mHandler.sendMessage(msg);
			        
			        eraseChip();
			        leaveProgrammingMode();
			        
					return false;
				}
				address += blockSize;
			}
		}
		
		leaveProgrammingMode();
		
		mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_LOADED).sendToTarget();
		return true;
	}
	
	public boolean verifyFirmware() {
		mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_VERIFYING).sendToTarget();
		List<Firmware.Segment> segments = mFirmware.getSegments();
		
		assert(mBlockSize != 0);
		
		for (Firmware.Segment segment: segments) {
			
			setAddress(segment.address);
			int address = segment.address;
			
			for (int i = 0; i < segment.data.length; i += mBlockSize) {

				mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_ACTIVE, segment.data.length, i).sendToTarget();

				int blockSize = mBlockSize;
				if (i + blockSize > segment.data.length) {
					blockSize = segment.data.length - i;
				}
				
				byte[] data = readBlock(segment.memoryType, blockSize);
				byte[] segmentData = new byte[blockSize];
				System.arraycopy(segment.data, i, segmentData, 0, blockSize);
				
				if (!Arrays.equals(data, segmentData)) {
					Log.e(TAG, "Firmware mismatch at address " + Integer.toHexString(address));
					
					Message msg = mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_VERIFICATION_FAILED);
			        Bundle bundle = new Bundle();
			        bundle.putString(FirmwareUpdateActivity.TOAST, "Firmware mismatch");
			        msg.setData(bundle);
			        mHandler.sendMessage(msg);
			        
					return false;
				}
				address += blockSize;
			}
		}
		
		mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_AVR109_VERIFIED).sendToTarget();
		return true;
	}

	public void knock() {
		try {
			mOutStream.write(ESCAPE);
			mOutStream.flush();
			read(10, 100); // Ignore result
		} catch (IOException e) {
			Log.e(TAG, "start() failed", e);
		}
	}

	public String getBootloaderSignature() {
		for (int i = 0; i < 10; i++) {
			knock();
			try {
				mOutStream.write(GET_BOOTLOADER);
				mOutStream.flush();
			} catch (IOException e) {
				Log.e(TAG, "getBootloaderSignature() failed", e);
			}

			String loader = new String(read(7, 100));
			
	        if(D) Log.i(TAG, "Loader = '" + loader + "'");

			if (loader.equals(BOOTLOADER_SIGNATURE)) {
				return loader;
			}
			else {
		        if(D) Log.e(TAG, loader + " does not match " + BOOTLOADER_SIGNATURE);				
			}
		}
		return null;
	}

	public String getSoftwareVersion() {
		try {
			mOutStream.write(GET_SW_VERSION);
			mOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "getSoftwareVersion() failed", e);
		}

		byte[] version = read(2, 100);
		if (version.length != 2)
			return null;

		
		StringBuilder sb = new StringBuilder();
		sb.append((char) version[0]);
		sb.append(".");
		sb.append((char) version[1]);
		
        if(D) Log.i(TAG, "Software version = " + sb.toString());
		
		return sb.toString();
	}

	public String getProgrammerType() {
		try {
			mOutStream.write(GET_PROG_TYPE);
			mOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "getProgrammerType() failed", e);
		}

		byte[] type = read(1, 100);
		if (type.length != 1)
			return null;

		String result = new String(type);
		
        if(D) Log.i(TAG, "Programmer type = " + result);

        return result;
	}

	public byte[] getDeviceList() {
		try {
			mOutStream.write(GET_DEV_LIST);
			mOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "getDeviceList() failed", e);
		}

		StringBuilder sb = new StringBuilder();

		while (true) {
			byte[] device = read(1, 100);
			if (device.length == 0 || device[0] == 0) {
				return sb.toString().getBytes();
			}
			sb.append(device[0]);
		}
	}

	public byte[] getDeviceSignature() {
		try {
			mOutStream.write(GET_SIGNATURE);
			mOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "getDeviceSignature() failed", e);
		}

		return read(3, 100);
	}

	public  boolean hasAutoIncrement() {
		try {
			mOutStream.write(GET_INCREMENT);
			mOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "hasAutoIncrement() failed", e);
		}

		byte[] result = read(1, 100);
		
        if(D) Log.i(TAG, "Has Auto-increment = " + (char) result[0]);

		return ((result.length == 1) & ((char) result[0] == 'Y'));
	}

	public int getBlockSize() {
		int block_size = 0;
		try {
			mOutStream.write(GET_BLOCK_SIZE);
			mOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "getBlockSize() failed", e);
		}

		byte[] result = read(3, 100);
		if (result.length != 3 || result[0] != 'Y') return block_size;
		
		block_size = ((result[1] << 8) & 0xff00) | (result[2] & 0xff);
		
        if(D) Log.i(TAG, "Block size = " + Integer.toString(block_size));

		return block_size;
	}

	public boolean eraseChip() {
		try {
			mOutStream.write(CMD_CHIP_ERASE);
			mOutStream.flush();
			return verifyAndLog("eraseChip()");
		} catch (IOException e) {
			Log.e(TAG, "eraseChip() failed", e);
			return false;
		}
	}

	public boolean enterProgrammingMode() {
		try {
			mOutStream.write(CMD_START_PROG);
			mOutStream.flush();
			return verifyAndLog("enterProgrammingMode()");
		} catch (IOException e) {
			Log.e(TAG, "enterProgrammingMode() failed", e);
			return false;
		}
	}

	public boolean leaveProgrammingMode() {
		try {
			mOutStream.write(CMD_LEAVE_PROG);
			mOutStream.flush();
			return verifyAndLog("leaveProgrammingMode()");
		} catch (IOException e) {
			Log.e(TAG, "leaveProgrammingMode() failed", e);
			return false;
		}
	}

	public boolean exitBootloader() {
		try {
			mOutStream.write(CMD_EXIT_LOADER);
			mOutStream.flush();
			return verifyAndLog("exitBootloader()");
		} catch (IOException e) {
			Log.e(TAG, "exitBootloader() failed", e);
			return false;
		}
	}

	public boolean setAddress(int address) {
		
		Log.i(TAG, "setAddress(" + Integer.toHexString(address) + ")");

		int word_address = address / 2;
		int ah = (word_address >> 8) & 0xFF ;
		int al = (address >> 0) & 0xFF;
		try {
			mOutStream.write(CMD_SET_ADDRESS);
			mOutStream.write(ah);
			mOutStream.write(al);
			mOutStream.flush();
			return verifyAndLog("setAddress()");
		} catch (IOException e) {
			Log.e(TAG, "setAddress() failed", e);
			return false;
		}
	}

	public boolean writeBlock(char memtype, byte[] data) {		
		assert(data.length % 2 == 0);
		
		int ah = (data.length >> 8) & 0xFF ;
		int al = data.length & 0xFF;
		try {
			mOutStream.write(CMD_WRITE_BLOCK);
			mOutStream.write(ah);
			mOutStream.write(al);
			mOutStream.write(memtype);
			mOutStream.write(data);
			mOutStream.flush();
			return verifyAndLog("writeBlock()");
		} catch (IOException e) {
			Log.e(TAG, "writeBlock() failed", e);
			return false;
		}
	}

	public byte[] readBlock(char memtype, int size) {
		
		Log.i(TAG, "readBlock(" + memtype + ", " + Integer.toString(size) + ")");
		
		byte[] result = new byte[size];
		int ah = (size >> 8) & 0xFF;
		int al = size & 0xFF;
		try {
			mOutStream.write(CMD_READ_BLOCK);
			mOutStream.write(ah);
			mOutStream.write(al);
			mOutStream.write(memtype);
			mOutStream.flush();
			return read(size, 1000);
		} catch (IOException e) {
			Log.e(TAG, "readBlock() failed", e);
			return result;
		}
	}
}
