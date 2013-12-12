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

import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class Avr109 extends Thread {
	// Debugging
	private static final String TAG = "Avr109";
	private static final boolean D = true;

	// private final BluetoothSocket mmSocket;
	private final InputStream mmInStream;
	private final OutputStream mmOutStream;

	private static final int ESCAPE = 27; 			// <esc>
	private static final int ACKNOWLEDGED = 13; 	// <cr>
	private static final int GET_BOOTLOADER = 83; 	// 'S'
	private static final int GET_SW_VERSION = 86; 	// 'V'
	private static final int GET_PROG_TYPE = 114; 	// 'p'
	private static final int GET_DEV_LIST = 118; 	// 't'
	private static final int GET_SIGNATURE = 117; 	// 's'
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

	public Avr109(BluetoothSocket socket) {
		if (D)
			Log.d(TAG, "create ConnectedThread");
		// mmSocket = socket;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;

		// Get the BluetoothSocket input and output streams
		try {
			tmpIn = socket.getInputStream();
			tmpOut = socket.getOutputStream();

		} catch (IOException e) {
			Log.e(TAG, "temp sockets not created", e);
		}

		mmInStream = tmpIn;
		mmOutStream = tmpOut;
	}

	private byte[] read(int len, long timeout) {
		long expiration = System.currentTimeMillis() + timeout;
		byte[] buffer = new byte[len];
		int pos = 0;
		while (pos != len && System.currentTimeMillis() < expiration) {
			try {
				if (mmInStream.available() != 0) {
					buffer[pos++] = (byte) mmInStream.read();
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

	private boolean verify() {
		byte[] answer = read(1, 10000);
		return (answer.length == 1 && answer[0] == ACKNOWLEDGED);
	}

	private boolean verifyAndLog(String command) {
		boolean result = verify();

		if (result)
			Log.i(TAG, command + " succeeded");
		else
			Log.e(TAG, command + " failed");

		return result;
	}

	public void start() {
		try {
			mmOutStream.write(ESCAPE);
			mmOutStream.flush();
			read(10, 100); // Ignore result
		} catch (IOException e) {
			Log.e(TAG, "start() failed", e);
		}
	}

	public String getBootloaderSignature() {
		for (int i = 0; i < 10; i++) {
			start();
			try {
				mmOutStream.write(GET_BOOTLOADER);
				mmOutStream.flush();
			} catch (IOException e) {
				Log.e(TAG, "getBootloaderSignature() failed", e);
			}
			String loader = read(7, 100).toString();
			if (loader == "XBoot++")
				return loader;
		}
		return null;
	}

	public String getSoftwareVersion() {
		try {
			mmOutStream.write(GET_SW_VERSION);
			mmOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "getSoftwareVersion() failed", e);
		}

		byte[] version = read(2, 100);
		if (version == null || version.length != 2)
			return null;

		StringBuilder sb = new StringBuilder();
		sb.append(version[0]);
		sb.append(".");
		sb.append(version[1]);
		return sb.toString();
	}

	public String getProgrammerType() {
		try {
			mmOutStream.write(GET_PROG_TYPE);
			mmOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "getProgrammerType() failed", e);
		}

		byte[] type = read(1, 100);
		if (type == null || type.length != 1)
			return null;
		return type.toString();
	}

	public byte[] getDeviceList() {
		try {
			mmOutStream.write(GET_DEV_LIST);
			mmOutStream.flush();
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
			mmOutStream.write(GET_SIGNATURE);
			mmOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "getDeviceSignature() failed", e);
		}

		return read(3, 100);
	}

	public  boolean hasAutoIncrement() {
		try {
			mmOutStream.write(GET_INCREMENT);
			mmOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "hasAutoIncrement() failed", e);
		}

		byte[] result = read(1, 100);
		
		return (result.length == 1 & result[0] == 'Y');
	}

	public int getBlockSize() {
		int block_size = 0;
		try {
			mmOutStream.write(GET_BLOCK_SIZE);
			mmOutStream.flush();
		} catch (IOException e) {
			Log.e(TAG, "getBlockSize() failed", e);
		}

		byte[] result = read(3, 100);
		if (result.length != 3 || result[0] != 'Y') return block_size;
		
		block_size = result[1] * 256 + result[2];
		return block_size;
	}

	public boolean eraseChip() {
		try {
			mmOutStream.write(CMD_CHIP_ERASE);
			mmOutStream.flush();
			return verifyAndLog("eraseChip()");
		} catch (IOException e) {
			Log.e(TAG, "eraseChip() failed", e);
			return false;
		}
	}

	public boolean enterProgrammingMode() {
		try {
			mmOutStream.write(CMD_START_PROG);
			mmOutStream.flush();
			return verifyAndLog("enterProgrammingMode()");
		} catch (IOException e) {
			Log.e(TAG, "enterProgrammingMode() failed", e);
			return false;
		}
	}

	public boolean leaveProgrammingMode() {
		try {
			mmOutStream.write(CMD_LEAVE_PROG);
			mmOutStream.flush();
			return verifyAndLog("leaveProgrammingMode()");
		} catch (IOException e) {
			Log.e(TAG, "leaveProgrammingMode() failed", e);
			return false;
		}
	}

	public boolean exitBootloader() {
		try {
			mmOutStream.write(CMD_EXIT_LOADER);
			mmOutStream.flush();
			return verifyAndLog("exitBootloader()");
		} catch (IOException e) {
			Log.e(TAG, "exitBootloader() failed", e);
			return false;
		}
	}

	public boolean setAddress(int address) {
		int word_address = address / 2;
		int ah = (word_address >> 8) & 0xFF ;
		int al = address & 0xFF;
		try {
			mmOutStream.write(CMD_SET_ADDRESS);
			mmOutStream.write(ah);
			mmOutStream.write(al);
			mmOutStream.flush();
			return verifyAndLog("setAddress()");
		} catch (IOException e) {
			Log.e(TAG, "setAddress() failed", e);
			return false;
		}
	}

	public boolean writeBlock(char memtype, byte[] data) {
		int ah = (data.length >> 8) & 0xFF ;
		int al = data.length & 0xFF;
		try {
			mmOutStream.write(CMD_WRITE_BLOCK);
			mmOutStream.write(ah);
			mmOutStream.write(al);
			mmOutStream.write(memtype);
			mmOutStream.write(data);
			mmOutStream.flush();
			return verifyAndLog("writeBlock()");
		} catch (IOException e) {
			Log.e(TAG, "writeBlock() failed", e);
			return false;
		}
	}

	public byte[] readBlock(char memtype, int size) {
		byte[] result = new byte[size];
		int ah = (size >> 8) & 0xFF ;
		int al = size & 0xFF;
		try {
			mmOutStream.write(CMD_READ_BLOCK);
			mmOutStream.write(ah);
			mmOutStream.write(al);
			mmOutStream.write(memtype);
			mmOutStream.flush();
			return read(size, 1000);
		} catch (IOException e) {
			Log.e(TAG, "readBlock() failed", e);
			return result;
		}
	}
}
