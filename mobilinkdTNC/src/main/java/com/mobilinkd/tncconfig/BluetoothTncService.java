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
import java.util.UUID;









// import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class sets up and manages the Bluetooth connection with the TNC.
 * It has a thread for connecting with the device and a thread for
 * performing data transmissions when connected.  The structure of this
 * code is based on BluetoothChat from the Android SDK examples.
 */
public class BluetoothTncService {
    // Debugging
    private static final String TAG = "BluetoothTncService";
    private static final boolean D = true;

    // UUID for this serial port protocol
    private static final UUID SPP_UUID = 
    		UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private String mDeviceName = null;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    public static final int STATE_CONFIG = 4;	  // running config
    public static final int STATE_DOWNLOAD = 8;   // running firmware download
    public static final int STATE_UPLOAD = 16;	  // running firmware upload
    
    
    public static final int SEND_SPACE = 1;
    public static final int SEND_MARK = 2;
    public static final int SEND_BOTH = 3;
    
    private static final byte[] TNC_SET_TX_DELAY = 
    		new byte[] { (byte)0xc0, 0x01, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_PERSISTENCE = 
    		new byte[] { (byte)0xc0, 0x02, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_SLOT_TIME = 
    		new byte[] { (byte)0xc0, 0x03, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_TX_TAIL = 
    		new byte[] { (byte)0xc0, 0x04, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_DUPLEX = 
    		new byte[] { (byte)0xc0, 0x05, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_BT_CONN_TRACK = 
    		new byte[] { (byte)0xc0, 0x06, 0x45, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_VERBOSITY = 
    		new byte[] { (byte)0xc0, 0x06, 0x10, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_INPUT_ATTEN = 
    		new byte[] { (byte)0xc0, 0x06, 0x02, 0, (byte)0xC0 };
    private static final byte[] TNC_STREAM_VOLUME =
    		new byte[] { (byte)0xc0, 0x06, 0x05, (byte)0xC0 };
    private static final byte[] TNC_PTT_MARK = 
    		new byte[] { (byte)0xc0, 0x06, 0x07, (byte)0xC0 }; 
    private static final byte[] TNC_PTT_SPACE =
    		new byte[] { (byte)0xc0, 0x06, 0x08, (byte)0xC0 };
    private static final byte[] TNC_PTT_BOTH =
    		new byte[] { (byte)0xc0, 0x06, 0x09, (byte)0xC0 };
    private static final byte[] TNC_PTT_OFF = 
    		new byte[] { (byte)0xc0, 0x06, 0x0A, (byte)0xC0 };
    private static final byte[] TNC_SET_OUTPUT_VOLUME = 
    		new byte[] { (byte)0xc0, 0x06, 0x01, 0, (byte)0xC0 };
    private static final byte[] TNC_GET_OUTPUT_VOLUME =
    		new byte[] { (byte)0xc0, 0x06, 0x0C, (byte)0xC0 };
    private static final byte[] TNC_SET_SQUELCH_LEVEL = 
    		new byte[] { (byte)0xc0, 0x06, 0x03, 0, (byte)0xC0 };
    private static final byte[] TNC_GET_ALL_VALUES = 
    		new byte[] { (byte)0xc0, 0x06, 0x7F, (byte)0xC0 };
    private static final byte[] TNC_SET_USB_POWER_ON = 
    		new byte[] { (byte)0xc0, 0x06, 0x49, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_USB_POWER_OFF = 
    		new byte[] { (byte)0xc0, 0x06, 0x4b, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_PTT_CHANNEL = 
    		new byte[] { (byte)0xc0, 0x06, 0x4f, 0, (byte)0xC0 };
    @SuppressWarnings("unused")
	private static final byte[] TNC_GET_PTT_CHANNEL = 
    		new byte[] { (byte)0xc0, 0x06, 0x50, (byte)0xC0 };
	private static final byte[] TNC_SAVE_EEPROM = 
    		new byte[] { (byte)0xc0, 0x06, 0x2a, (byte)0xC0 };
    private static final byte[] TNC_GET_BATTERY_LEVEL =
    		new byte[] { (byte)0xc0, 0x06, 0x06, (byte)0xC0 };

    /**
     * Constructor. Prepares a new Bluetooth session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothTncService(Context context, Handler handler) {
        // mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }
    
    public void resetHandler() {
    	mHandler = null;
    }
    
    public boolean setHandler(Handler handler) {
    	if (mHandler != null) {
    		return false;
    	}
    	mHandler = handler;
    	return true;
    }

    /**
     * Set the current state of the TNC connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        if (mHandler == null) return;

        // Give the new state to the Handler so the UI Activity can update
        Message msg = mHandler.obtainMessage(TncConfig.MESSAGE_STATE_CHANGE, state, -1);
        // Without a delay, battery level is incorrect on newer devices.
        // This is due to the electrical characteristics of the TNC2.
        mHandler.sendMessageDelayed(msg, 20);
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the TNC service. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_NONE);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        mDeviceName = device.getName();
        
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(TncConfig.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(TncConfig.DEVICE_NAME, mDeviceName);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mState == STATE_CONNECTED) {
        	mConnectedThread.write(TNC_PTT_OFF);
        }
        
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        setState(STATE_NONE);
    }
    
    public void getAllValues()
    {
        if (D) Log.d(TAG, "getAllValues()");
        
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        r.write(TNC_GET_ALL_VALUES);
    }
    
    public void getBatteryLevel()
    {
        if (D) Log.d(TAG, "getBatteryLevel()");
        
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        r.write(TNC_GET_BATTERY_LEVEL);
    }
    
    public boolean isConnected() {
    	return mState == STATE_CONNECTED;
    }
    
    public String getDeviceName() {
    	return mDeviceName;
    }

    public void getOutputVolume()
    {
        if (D) Log.d(TAG, "getOutputVolume()");
        
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        r.write(TNC_GET_OUTPUT_VOLUME);
    }

    public void ptt(int mode)
    {
        if (D) Log.d(TAG, "ptt() " + mode);
        
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        switch(mode)
        {
        case SEND_MARK:
        	r.write(TNC_PTT_MARK);
        	break;
        case SEND_SPACE:
        	r.write(TNC_PTT_SPACE);
        	break;
        case SEND_BOTH:
        	r.write(TNC_PTT_BOTH);
        	break;
        default:
        	r.write(TNC_PTT_OFF);
        	break;
        }
    }
    
    public void setDcd(boolean on)
    {
        if (D) Log.d(TAG, "setDcd()");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_SQUELCH_LEVEL;
        if (!on)
        {
        	c[3] = 2;
        }
        else
        {
        	c[3] = 0;
        }
    	r.write(c);
    }
    
    public void setTxDelay(int value)
    {
        if (D) Log.d(TAG, "setTxDelay()");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_TX_DELAY;
        c[2] = (byte) value;
    	r.write(c);
    }
    
    public void setPersistence(int value)
    {
        if (D) Log.d(TAG, "setPersistence()");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_PERSISTENCE;
        c[2] = (byte) value;
    	r.write(c);
    }
    
    public void setSlotTime(int value)
    {
        if (D) Log.d(TAG, "setSlotTime()");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_SLOT_TIME;
        c[2] = (byte) value;
    	r.write(c);
    }
    
    public void setTxTail(int value)
    {
        if (D) Log.d(TAG, "setTxTail()");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_TX_TAIL;
        c[2] = (byte) value;
    	r.write(c);
    }
    
    public void setDuplex(boolean on)
    {
        if (D) Log.d(TAG, "setDuplex()");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_DUPLEX;
        if (on)
        {
        	c[2] = 1;
        }
        else
        {
        	c[2] = 0;
        }
    	r.write(c);
    }
    
    public void setConnTrack(boolean on)
    {
        if (D) Log.d(TAG, "setConnTrack()");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_BT_CONN_TRACK;
        if (on)
        {
        	c[3] = 1;
        }
        else
        {
        	c[3] = 0;
        }
    	r.write(c);
    }
    
    public void setVerbosity(boolean on)
    {
        if (D) Log.d(TAG, "setVerbosity(" + on + ")");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_VERBOSITY;
        if (on)
        {
        	c[3] = 1;
        }
        else
        {
        	c[3] = 0;
        }
    	r.write(c);
    }
    
    public void setUsbPowerOn(boolean value)
    {
        if (D) Log.d(TAG, "setUsbPowerOn(" + value + ")");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_USB_POWER_ON;
        c[3] = (byte) (value ? 1 : 0);
    	r.write(c);
    }

    public void setUsbPowerOff(boolean value)
    {
        if (D) Log.d(TAG, "setUsbPowerOff(" + value + ")");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_USB_POWER_OFF;
        c[3] = (byte) (value ? 1 : 0);
    	r.write(c);
    }

    public void setPttChannel(int channel)
    {
        if (D) Log.d(TAG, "setPttChannel(" + channel + ")");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_PTT_CHANNEL;
        c[3] = (byte) channel;
    	r.write(c);
    }

    public void saveEeprom()
    {
        if (D) Log.d(TAG, "saveEeprom");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
    	r.write(TNC_SAVE_EEPROM);
    }

    public void setInputAtten(boolean on)
    {
        if (D) Log.d(TAG, "setInputAtten()");
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_INPUT_ATTEN;
        if (on)
        {
        	c[3] = 2;
        }
        else
        {
        	c[3] = 0;
        }
    	r.write(c);
    }
    
    @SuppressWarnings("unused")
	private class ListenTask extends AsyncTask<Integer, Void, Void> {
        /** The system calls this to perform work in a worker thread and
          * delivers it the parameters given to AsyncTask.execute() 
         * @return */
        protected Void doInBackground(Integer... delays) {
        	SystemClock.sleep(delays[0].intValue());
            return null;
        }
        
        /** The system calls this to perform work in the UI thread and delivers
          * the result from doInBackground() */
        protected void onPostExecute(Void v) {
            if (D) Log.d(TAG, "async listen()");
            ConnectedThread r;
            // Synchronize a copy of the ConnectedThread
            synchronized (this) {
                if (mState != STATE_CONNECTED) return;
                r = mConnectedThread;
            }
           	r.listen();
        }
    }

    public void listen() {
        if (D) Log.d(TAG, "listen()");
        
        // new ListenTask().execute(Integer.valueOf(250));

        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
       	r.listen();
    }
    
    public void volume(int v)
    {
        if (D) Log.d(TAG, "volume() = " + v);
    	
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        
        byte c[] = TNC_SET_OUTPUT_VOLUME;
        c[3] = (byte) v;
    	r.write(c);
    }
    
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_NONE);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(TncConfig.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putInt(TncConfig.TOAST, R.string.msg_unable_to_connect);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_NONE);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(TncConfig.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putInt(TncConfig.TOAST, R.string.msg_connection_lost);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BluetoothTncService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothTncService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private class HdlcDecoder {
        	
        	static final int BUFFER_SIZE = 330;
        	
        	static final byte FEND = (byte) 192;
        	static final byte FESC = (byte) 219;
        	static final byte TFEND = (byte) 220;
        	static final byte TFESC = (byte) 221;
        	
        	static final int STATE_WAIT_FEND = 0;
        	static final int STATE_WAIT_HW = 1;
        	static final int STATE_WAIT_ESC = 2;
        	static final int STATE_WAIT_DATA = 3;
        	
        	public static final int TNC_INPUT_VOLUME = 4;
        	public static final int TNC_OUTPUT_VOLUME = 12;
        	public static final int TNC_GET_TXDELAY = 33;
        	public static final int TNC_GET_PERSIST = 34;
        	public static final int TNC_GET_SLOTTIME = 35;
        	public static final int TNC_GET_TXTAIL = 36;
        	public static final int TNC_GET_DUPLEX = 37;
        	public static final int TNC_GET_SQUELCH_LEVEL = 14;
        	public static final int TNC_GET_HW_VERSION = 41;
        	public static final int TNC_GET_FW_VERSION = 40;
        	public static final int TNC_GET_VERBOSITY = 17;
        	public static final int TNC_GET_BATTERY_LEVEL = 6;
        	public static final int TNC_GET_INPUT_ATTEN = 13;
        	public static final int TNC_GET_BT_CONN_TRACK = 70;
        	public static final int TNC_GET_USB_POWER_ON = 74;
        	public static final int TNC_GET_USB_POWER_OFF = 76;
        	public static final int TNC_GET_PTT_CHANNEL = 80;
        	public static final int TNC_GET_CAPABILITIES = 126;
        	
        	boolean mAvailable = false;
        	byte[] mBuffer = new byte[BUFFER_SIZE];
        	int mPos = 0;
        	int mState = STATE_WAIT_FEND;
        	
        	public HdlcDecoder() {
        		
        	}
        	
        	public boolean available() {return mAvailable;}
        	
        	public void process(byte c)
        	{
        		switch (mState) {
        		case STATE_WAIT_FEND:
        			if (c == FEND) {
        				mPos = 0;
        				mAvailable = false;
        				mState = STATE_WAIT_HW;
        			}
        			break;
        		case STATE_WAIT_HW:
        			if (c == FEND) break;
        			if (c == (byte) 0x06) {
        				mState = STATE_WAIT_DATA;
        			}
        			else {
        				Log.e(TAG, "Invalid packet type received " + (int)c);
        				mState = STATE_WAIT_FEND;
        			}
        			break;
        		case STATE_WAIT_ESC:
        			switch (c) {
        			case TFESC:
        				mBuffer[mPos++] = FESC;
        				break;
        			case TFEND:
        				mBuffer[mPos++] = FEND;
        				break;
        			default:
            			mBuffer[mPos++] = c;
            			break;
        			}
        			mState = STATE_WAIT_DATA;
        			break;
        		case STATE_WAIT_DATA:
        			switch (c) {
        			case FESC:
        				mState = STATE_WAIT_ESC;
        				break;
        			case FEND:
        				if (mPos > 1) mAvailable = true;
        				mState = STATE_WAIT_FEND;
        			default:
        				mBuffer[mPos++] = c;
        				break;
        			}
        		}
        	}
        	
        	int getType() {return (int) mBuffer[0] & 0xff;}
        	int getValue() {return (int) mBuffer[1] & 0xff;}
        	int size() {return mPos - 1;}
        	byte[] data()
        	{
        		byte[] result = new byte[mPos - 2];
        		System.arraycopy(mBuffer, 1, result, 0, mPos - 2);
        		return result;
        	}
        }
        
    	public void listen() {
        	write(TNC_PTT_OFF);
        	write(TNC_STREAM_VOLUME);
    	}
    	
		private final HdlcDecoder mHdlc;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mHdlc = new HdlcDecoder();

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

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    byte c = (byte) mmInStream.read();
                    mHdlc.process(c);
                    if (mHandler == null) {
                    	continue;
                    }
                    if (mHdlc.available()) {
                    	switch (mHdlc.getType()) {
                    	case HdlcDecoder.TNC_INPUT_VOLUME:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_INPUT_VOLUME, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_OUTPUT_VOLUME:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_OUTPUT_VOLUME, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_TXDELAY:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_TX_DELAY, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_PERSIST:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_PERSISTENCE, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_SLOTTIME:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_SLOT_TIME, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_TXTAIL:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_TX_TAIL, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_DUPLEX:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_DUPLEX, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_SQUELCH_LEVEL:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_SQUELCH_LEVEL, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_HW_VERSION:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_HW_VERSION, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_FW_VERSION:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_FW_VERSION, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_BATTERY_LEVEL:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_BATTERY_LEVEL, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_BT_CONN_TRACK:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_BT_CONN_TRACK, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_VERBOSITY:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_VERBOSITY, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_INPUT_ATTEN:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_INPUT_ATTEN, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_USB_POWER_ON:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_USB_POWER_ON, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_USB_POWER_OFF:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_USB_POWER_OFF, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_PTT_CHANNEL:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_PTT_STYLE, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_CAPABILITIES:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_CAPABILITIES, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	default:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_OTHER, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	}
                    }

                } catch (IOException e) {
                    Log.i(TAG, "disconnected");
                    connectionLost();
                    return;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
