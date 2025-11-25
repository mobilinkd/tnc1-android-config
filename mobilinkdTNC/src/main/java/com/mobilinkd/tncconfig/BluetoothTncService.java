/*
 * Copyright (C) 2013-2020 Mobilinkd LLC
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
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RequiresPermission;

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
    private int mApiVersion = 0x0100;

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
    /* 16-bit signed. */
    private static final byte[] TNC_SET_OUTPUT_GAIN =
            new byte[] { (byte)0xc0, 0x06, 0x01, 0, 0, (byte)0xC0 };    // API 2.0
    private static final byte[] TNC_GET_OUTPUT_VOLUME =
    		new byte[] { (byte)0xc0, 0x06, 0x0C, (byte)0xC0 };
    private static final byte[] TNC_SET_OUTPUT_TWIST =
            new byte[] { (byte)0xc0, 0x06, 0x1A, 0, (byte)0xC0 };
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
    private static final byte[] TNC_ADJUST_INPUT_LEVELS =
            new byte[] { (byte)0xc0, 0x06, 0x2b, (byte)0xC0 };
    /* 16-bit signed. */
    private static final byte[] TNC_SET_INPUT_GAIN =
            new byte[] { (byte)0xc0, 0x06, 0x02, 0, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_INPUT_TWIST =
            new byte[] { (byte)0xc0, 0x06, 0x18, 0, (byte)0xC0 };
    /* BCD YYMMDDWWHHMMSS */
    private static final byte[] TNC_SET_DATETIME =
            new byte[] { (byte)0xc0, 0x06, 0x32, 0, 0, 0, 0, 0, 0, 0, (byte)0xC0 };

    private static final byte[] TNC_SET_PASSALL =
            new byte[] { (byte)0xc0, 0x06, 0x51, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_MODEM_TYPE =
            new byte[] { (byte)0xc0, 0x06, (byte)0xc1, (byte)0x82, 0, (byte)0xC0 };

    private static final byte[] TNC_SET_RX_REVERSE_POLARITY =
            new byte[] { (byte)0xc0, 0x06, 0x53, 0, (byte)0xC0 };
    private static final byte[] TNC_SET_TX_REVERSE_POLARITY =
            new byte[] { (byte)0xc0, 0x06, 0x55, 0, (byte)0xC0 };

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
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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
        
        byte[] c = TNC_SET_SQUELCH_LEVEL;
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
        
        byte[] c = TNC_SET_TX_DELAY;
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
        
        byte[] c = TNC_SET_PERSISTENCE;
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
        
        byte[] c = TNC_SET_SLOT_TIME;
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
        
        byte[] c = TNC_SET_TX_TAIL;
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
        
        byte[] c = TNC_SET_DUPLEX;
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
        
        byte[] c = TNC_SET_BT_CONN_TRACK;
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
        
        byte[] c = TNC_SET_VERBOSITY;
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

    public void setPassall(boolean on)
    {
        if (D) Log.d(TAG, "setPassall()");

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        byte[] c = TNC_SET_PASSALL;
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

    public void setRxReversePolarity(boolean on)
    {
        if (D) Log.d(TAG, "setRxReversePolarity()");

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        byte[] c = TNC_SET_RX_REVERSE_POLARITY;
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

    public void setTxReversePolarity(boolean on)
    {
        if (D) Log.d(TAG, "setTxReversePolarity()");

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        byte[] c = TNC_SET_TX_REVERSE_POLARITY;
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

    public void setModemType(int v)
    {
        if (D) Log.d(TAG, "setModemType()");

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        byte[] c = TNC_SET_MODEM_TYPE;
        c[4] = (byte) v;
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
        
        byte[] c = TNC_SET_USB_POWER_ON;
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
        
        byte[] c = TNC_SET_USB_POWER_OFF;
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
        
        byte[] c = TNC_SET_PTT_CHANNEL;
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
        
        byte[] c = TNC_SET_INPUT_ATTEN;
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

    public void setInputGain(int level)
    {
        if (D) Log.d(TAG, "setInputGain()");

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        byte[] c = TNC_SET_INPUT_GAIN;
        c[3] = (byte) ((level >> 8) & 0xFF);
        c[4] = (byte) (level & 0xFF);
        r.write(c);
    }

    public void setInputTwist(int level)
    {
        if (D) Log.d(TAG, "setInputTwist()");

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        byte[] c = TNC_SET_INPUT_TWIST;
        c[3] = (byte) level;
        r.write(c);
    }

    public void adjustInputLevels()
    {
        if (D) Log.d(TAG, "adjustInputLevels");

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        r.write(TNC_ADJUST_INPUT_LEVELS);
    }

    private byte bcd(int value)
    {
        assert(value < 100);

        return (byte) (((value / 10) * 16) + (value % 10));
    }

    public void setDateTime()
    {
        if (D) Log.d(TAG, "setDateTIme");

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        byte[] c = TNC_SET_DATETIME;

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(calendar.getTime());

        c[3] = bcd(calendar.get(Calendar.YEAR) - 2000);
        c[4] = bcd(calendar.get(Calendar.MONTH) + 1);
        c[5] = bcd(calendar.get(Calendar.DAY_OF_MONTH));
        c[6] = (byte) (calendar.get(Calendar.DAY_OF_WEEK) - 1);
        c[7] = bcd(calendar.get(Calendar.HOUR_OF_DAY));
        c[8] = bcd(calendar.get(Calendar.MINUTE));
        c[9] = bcd(calendar.get(Calendar.SECOND));

        r.write(c);
    }

    public void listen() {
        if (D) Log.d(TAG, "listen()");

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
        if (mApiVersion == 0x100) {
            byte[] c = TNC_SET_OUTPUT_VOLUME;
            c[3] = (byte) v;
            r.write(c);
        } else {
            byte[] c = TNC_SET_OUTPUT_GAIN;
            c[3] = (byte) ((v >> 8) & 0xFF);
            c[4] = (byte) (v & 0xFF);
            r.write(c);
        }
    }

    public void outputTwist(int v)
    {
        if (D) Log.d(TAG, "outputTwist() = " + v);

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        byte[] c = TNC_SET_OUTPUT_TWIST;
        c[3] = (byte) v;
        r.write(c);
    }

    public void modemType(int modem)
    {
        if (D) Log.d(TAG, "modemType() = " + modem);

        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        byte[] c = TNC_SET_MODEM_TYPE;
        c[4] = (byte) modem;
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

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static String byteArrayToMacAddress(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 3);
        for(byte b: a)
            sb.append(String.format("%02X:", b));
        sb.deleteCharAt(a.length * 3 - 1);
        return sb.toString();
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

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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
            mApiVersion = 0x100;

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

        public void handle_extended_command_range_1()
        {
            byte[] data = mHdlc.data();
            switch((int) data[0] & 0xff) {
                case HdlcDecoder.EXT_GET_MODEM_TYPE:
                    Log.i(TAG, "EXT_GET_MODEM_TYPE = " + (int) data[1]);
                    int modem = data[1];
                    mHandler.obtainMessage(
                            TncConfig.MESSAGE_MODEM_TYPE, modem,
                            mHdlc.size(), data).sendToTarget();
                    break;
                case HdlcDecoder.EXT_GET_MODEM_TYPES:
                    Log.i(TAG, "EXT_GET_MODEM_TYPES = " + (data.length - 1));
                    mHandler.obtainMessage(
                            TncConfig.MESSAGE_SUPPORTED_MODEM_TYPES,  0,
                            mHdlc.size(), data).sendToTarget();
                    break;
                default:
                    // Send the obtained bytes to the UI Activity
                    Log.i(TAG, "EXT_RANGE_1 = " + (int) data[0]);
                    mHandler.obtainMessage(
                            TncConfig.MESSAGE_OTHER, (int) mHdlc.getValue(),
                            mHdlc.size(), mHdlc.data()).sendToTarget();
                    break;
            }
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    byte c = (byte) mmInStream.read();
                    int level = 0;
                    mHdlc.process(c);
                    if (mHandler == null) {
                    	continue;
                    }
                    if (mHdlc.available()) {
                        byte[] data = mHdlc.data();
                    	switch (mHdlc.getType()) {
                        case HdlcDecoder.TNC_GET_API_VERSION:
                            mApiVersion = (0xFF & data[0]) * 256 + (0xFF & data[1]);
                            Log.i(TAG, "API version = " + mApiVersion);
                            break;
                    	case HdlcDecoder.TNC_INPUT_VOLUME:
                            // Send the obtained bytes to the UI Activity
                             mHandler.obtainMessage(
                                    TncConfig.MESSAGE_INPUT_VOLUME, (int) mHdlc.getValue(),
                                    mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_OUTPUT_VOLUME:
                            // Send the obtained bytes to the UI Activity
                                if (mApiVersion == 0x0100) {mHandler.obtainMessage(
                            		TncConfig.MESSAGE_OUTPUT_VOLUME, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            } else {
                                level = (0xFF & data[0]) * 256 + (0xFF & data[1]);
                                mHandler.obtainMessage(
                                        TncConfig.MESSAGE_OUTPUT_VOLUME, level,
                                        mHdlc.size(), data).sendToTarget();
                            }
                            break;
                        case HdlcDecoder.TNC_GET_OUTPUT_TWIST:
                            // Send the obtained bytes to the UI Activity
                            Log.i(TAG, "TNC_GET_OUTPUT_TWIST = " + mHdlc.getValue());
                            mHandler.obtainMessage(
                                TncConfig.MESSAGE_OUTPUT_TWIST, (int) mHdlc.getValue(),
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
                        case HdlcDecoder.TNC_GET_PASSALL:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_PASSALL, (int) mHdlc.getValue(),
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
                    	case HdlcDecoder.TNC_GET_INPUT_GAIN:
                            // Send the obtained bytes to the UI Activity
                            if (mApiVersion == 0x0100) {
                                mHandler.obtainMessage(
                                        TncConfig.MESSAGE_INPUT_ATTEN, (int) mHdlc.getValue(),
                                        mHdlc.size(), mHdlc.data()).sendToTarget();
                            } else {
                                int gain = (0xFF & data[0]) * 256 + (0xFF & data[1]);
                                mHandler.obtainMessage(
                                        TncConfig.MESSAGE_INPUT_GAIN, gain,
                                        mHdlc.size(), data).sendToTarget();
                            }
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
                        case HdlcDecoder.TNC_GET_MAX_INPUT_GAIN:
                            // Send the obtained bytes to the UI Activity
                            int max_input_gain = (0xFF & data[0]) * 256 + (0xFF & data[1]);
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_INPUT_GAIN_MAX, max_input_gain,
                                    mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                        case HdlcDecoder.TNC_GET_MIN_INPUT_GAIN:
                            // Send the obtained bytes to the UI Activity
                            int min_input_gain = (0xFF & data[0]) * 256 + (0xFF & data[1]);
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_INPUT_GAIN_MIN, min_input_gain,
                                    mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                        case HdlcDecoder.TNC_GET_INPUT_TWIST:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_INPUT_TWIST, (int) data[0],
                                    mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                        case HdlcDecoder.TNC_GET_MAX_INPUT_TWIST:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_INPUT_TWIST_MAX, (int) data[0],
                                    mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                        case HdlcDecoder.TNC_GET_MIN_INPUT_TWIST:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_INPUT_TWIST_MIN, (int) data[0],
                                    mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                        case HdlcDecoder.TNC_GET_RX_REVERSE_POLARITY:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_RX_REVERSE_POLARITY, mHdlc.getValue(),
                                    mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                        case HdlcDecoder.TNC_GET_TX_REVERSE_POLARITY:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_TX_REVERSE_POLARITY, mHdlc.getValue(),
                                    mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                    	case HdlcDecoder.TNC_GET_CAPABILITIES:
                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(
                            		TncConfig.MESSAGE_CAPABILITIES, (int) mHdlc.getValue(),
                            		mHdlc.size(), mHdlc.data()).sendToTarget();
                            break;
                        case HdlcDecoder.TNC_GET_DATETIME:
                            // Send the obtained bytes to the UI Activity
                            String dateTime = Arrays.toString(data);
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_DATE_TIME, 0,
                                    mHdlc.size(), mHdlc.data()).sendToTarget();
                            Log.i(TAG, "TNC_GET_DATETIME: " + dateTime);
                            break;
                        case HdlcDecoder.TNC_GET_SERIAL_NUMBER:
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_SERIAL_NUMBER, 0,
                                    mHdlc.size(), mHdlc.data()).sendToTarget();
                            Log.i(TAG, "TNC_GET_SERIAL_NUMBER: " + Arrays.toString(data));
                            break;
                        case HdlcDecoder.TNC_GET_MAC_ADDRESS:
                            mHandler.obtainMessage(
                                    TncConfig.MESSAGE_MAC_ADDRESS, 0,
                                    0, byteArrayToMacAddress(data)).sendToTarget();
                            Log.i(TAG, "TNC_GET_MAC_ADDRESS: " + byteArrayToMacAddress(data));
                            break;
                        case HdlcDecoder.EXT_RANGE_1:
                            Log.i(TAG, "EXT_RANGE_1 = " + byteArrayToHex(data));
                            handle_extended_command_range_1();
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
