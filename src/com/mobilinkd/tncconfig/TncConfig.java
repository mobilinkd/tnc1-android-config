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

import net.simonvt.numberpicker.NumberPicker;
import net.simonvt.numberpicker.NumberPicker.OnValueChangeListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.speech.levelmeter.BarLevelDrawable;

public class TncConfig extends Activity {
    // Debugging
    private static final String TAG = "TncConfig";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_INPUT_VOLUME = 2;
    public static final int MESSAGE_OUTPUT_VOLUME = 3;
    public static final int MESSAGE_OTHER = 4;
    public static final int MESSAGE_DEVICE_NAME = 5;
    public static final int MESSAGE_TOAST = 6;
    public static final int MESSAGE_TX_DELAY = 7;
    public static final int MESSAGE_PERSISTENCE = 8;
    public static final int MESSAGE_SLOT_TIME = 9;
    public static final int MESSAGE_TX_TAIL = 10;
    public static final int MESSAGE_DUPLEX = 11;
    public static final int MESSAGE_SQUELCH_LEVEL = 12;
    public static final int MESSAGE_HW_VERSION = 13;
    public static final int MESSAGE_FW_VERSION = 14;
    public static final int MESSAGE_INPUT_ATTEN = 15;
    public static final int MESSAGE_VERBOSITY = 16;
    public static final int MESSAGE_BATTERY_LEVEL = 17;
    public static final int MESSAGE_BT_CONN_TRACK = 18;
    
    
    // Key names received from the BluetoothTncService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    private static final int TONE_NONE = 0;
    private static final int TONE_SPACE = 1;
    private static final int TONE_MARK = 2;
    // private static final int TONE_BOTH = 3;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothTncService mTncService = null;
    
    private TextView mTitle;
    private ToggleButton mConnectButton;
    private ToggleButton mMarkButton;
    private ToggleButton mSpaceButton;
    private ToggleButton mPttButton;
    private SeekBar mOutputVolumeLevel;
    private TextView mOutputVolumeText;
    private BarLevelDrawable mInputVolumeLevel;
    private NumberPicker mTxDelayPicker;
    private NumberPicker mPersistencePicker;
    private NumberPicker mSlotTimePicker;
    private NumberPicker mTxTailPicker;
    private ToggleButton mDcdButton;
    private ToggleButton mDuplexButton;
    private ToggleButton mConnTrackButton;
    private ToggleButton mInfoButton;
    private ToggleButton mInputAttenButton;
    private int mTone = 0;
    private String fwVersion = "282";
    private String hwVersion = "1.12";

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}


	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		}
		setContentView(R.layout.activity_main);
		
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_right_text);

        mInputVolumeLevel = (BarLevelDrawable) findViewById(R.id.bar_level_drawable_view);
	}

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mTncService == null) setupConfig();
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth TNC services
        if (mTncService != null) mTncService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }


    private void setupConfig() {
        Log.d(TAG, "setupConfig()");

        // Initialize the Connect/Disconnect button.
        // Initialize the 1200Hz button
        // Initialize the 2200Hz button
        // Initialize the Listen button
        // Initialize the Volume control
        // Initialize the Volume meter
        mConnectButton = (ToggleButton) findViewById(R.id.toggleButtonConnect);
        mConnectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                boolean on = ((ToggleButton) view).isChecked();
                if (on) {
                    // Launch the DeviceListActivity to see devices and do scan
                    Intent selectDeviceIntent = new Intent(
                    		TncConfig.this, DeviceListActivity.class);
                    startActivityForResult(
                    		selectDeviceIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    // Disconnect Bluetooth
                	mTncService.stop();
                }
            }
        });


        mPttButton = (ToggleButton) findViewById(R.id.toggleButtonPTT);
        mPttButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                boolean on = ((ToggleButton) view).isChecked();
                
                if (on) {
                	mTncService.ptt(mTone);
               } else {
               		mTncService.ptt(TONE_NONE);
               		mTncService.listen();
               }
            }
        });

        mSpaceButton = (ToggleButton) findViewById(R.id.toggleButtonSpace);
        mSpaceButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                boolean on = ((ToggleButton) view).isChecked();
                
                if (on) {
                	mTone |= TONE_SPACE;
                } else {
                    mTone &= ~TONE_SPACE;
                }
                
                if (TncConfig.this.mPttButton.isChecked()) {
                	mTncService.ptt(mTone);
                }
            }
        });


        mMarkButton = (ToggleButton) findViewById(R.id.toggleButtonMark);
        mMarkButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                boolean on = ((ToggleButton) view).isChecked();
                
                if (on) {
                	mTone |= TONE_MARK;
                } else {
                    mTone &= ~TONE_MARK;
                }
                
                if (TncConfig.this.mPttButton.isChecked()) {
                	mTncService.ptt(mTone);
                }
            }
        });
        
        mOutputVolumeText = (TextView) findViewById(R.id.outputVolumeText);
        mOutputVolumeLevel = (SeekBar) findViewById(R.id.outputVolumeLevel);
        mOutputVolumeLevel.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        	@Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {

            	if (fromUser) {
                	mTncService.volume(progress);
                }
            	mOutputVolumeText.setText(Integer.toString(progress));
            }
        	@Override
            public void onStartTrackingTouch(SeekBar seekbar) {
            }
        	@Override
            public void onStopTrackingTouch(SeekBar seekbar) {
            }
        });
        
        

        mTxDelayPicker = (NumberPicker) findViewById(R.id.numberPickerTxDelay);
        mTxDelayPicker.setMaxValue(255);
        mTxDelayPicker.setMinValue(0);
        mTxDelayPicker.setWrapSelectorWheel(false);
        mTxDelayPicker.setEnabled(false);
        mTxDelayPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mTxDelayPicker.setOnValueChangedListener(new OnValueChangeListener() {
            public void onValueChange(NumberPicker picker, int oldValue, int newValue) {
                mTncService.setTxDelay(newValue);
            }
        });

        mPersistencePicker = (NumberPicker) findViewById(R.id.numberPickerPersistence);
        mPersistencePicker.setMaxValue(255);
        mPersistencePicker.setMinValue(0);
        mPersistencePicker.setWrapSelectorWheel(false);
        mPersistencePicker.setEnabled(false);
        mPersistencePicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mPersistencePicker.setOnValueChangedListener(new OnValueChangeListener() {
            public void onValueChange(NumberPicker picker, int oldValue, int newValue) {
                mTncService.setPersistence(newValue);
            }
        });

        mSlotTimePicker = (NumberPicker) findViewById(R.id.numberPickerSlotTime);
        mSlotTimePicker.setMaxValue(255);
        mSlotTimePicker.setMinValue(0);
        mSlotTimePicker.setWrapSelectorWheel(false);
        mSlotTimePicker.setEnabled(false);
        mSlotTimePicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mSlotTimePicker.setOnValueChangedListener(new OnValueChangeListener() {
            public void onValueChange(NumberPicker picker, int oldValue, int newValue) {
                mTncService.setSlotTime(newValue);
            }
        });

        mTxTailPicker = (NumberPicker) findViewById(R.id.numberPickerTxTail);
        mTxTailPicker.setMaxValue(255);
        mTxTailPicker.setMinValue(0);
        mTxTailPicker.setWrapSelectorWheel(false);
        mTxTailPicker.setEnabled(false);
        mTxTailPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mTxTailPicker.setOnValueChangedListener(new OnValueChangeListener() {
            public void onValueChange(NumberPicker picker, int oldValue, int newValue) {
                mTncService.setTxTail(newValue);
            }
        });

        mDcdButton = (ToggleButton) findViewById(R.id.toggleButtonDCD);
        mDcdButton.setEnabled(false);
        mDcdButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                boolean on = ((ToggleButton) view).isChecked();
                
                mTncService.setDcd(on);
            }
        });
        
        mDuplexButton = (ToggleButton) findViewById(R.id.toggleButtonDuplex);
        mDuplexButton.setEnabled(false);
        mDuplexButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                boolean on = ((ToggleButton) view).isChecked();
                
                mTncService.setDuplex(on);
            }
        });
        
        mConnTrackButton = (ToggleButton) findViewById(R.id.toggleButtonConnTrack);
        mConnTrackButton.setEnabled(false);
        mConnTrackButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                boolean on = ((ToggleButton) view).isChecked();
                
                mTncService.setConnTrack(on);
            }
        });
        
        mInfoButton = (ToggleButton) findViewById(R.id.toggleButtonInfo);
        mInfoButton.setEnabled(false);
        mInfoButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                boolean on = ((ToggleButton) view).isChecked();
                
                mTncService.setVerbosity(on);
            }
        });
        
        mInputAttenButton = (ToggleButton) findViewById(R.id.toggleButtonInputAtten);
        mInputAttenButton.setEnabled(false);
        mInputAttenButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                boolean on = ((ToggleButton) view).isChecked();
                
                mTncService.setInputAtten(on);
            }
        });
        
        // Initialize the BluetoothChatService to perform bluetooth connections
        mTncService = new BluetoothTncService(this, mHandler);
    }


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_bluetooth_settings:
            // Launch the DeviceListActivity to see devices and do scan
            Intent bluetoothSettingsIntent = new Intent();
            bluetoothSettingsIntent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(bluetoothSettingsIntent);
            return true;
        case R.id.action_firmware_update:
            // Launch the FirmwareUpdateActivity to upload new firmware.
            Intent updateFirmwareIntent = new Intent(this, FirmwareUpdateActivity.class);
            startActivity(updateFirmwareIntent);
            return true;
        case R.id.action_about:
            // Launch the AboutActivity to learn about the program.
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        }
        return false;
    }

    // The Handler that gets information back from the BluetoothTncService
    @SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	byte[] buffer = null;
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothTncService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mConnectButton.setChecked(true);
                    mPttButton.setEnabled(true);
                    mTxDelayPicker.setEnabled(true);
                    mTxDelayPicker.setValue(TncConfigDefaults.TX_DELAY);
                    mPersistencePicker.setEnabled(true);
                    mPersistencePicker.setValue(TncConfigDefaults.PERSISTENCE);
                    mSlotTimePicker.setEnabled(true);
                    mSlotTimePicker.setValue(TncConfigDefaults.SLOT_TIME);
                    mTxTailPicker.setEnabled(true);
                    mTxTailPicker.setValue(TncConfigDefaults.TX_TAIL);
                    mDcdButton.setChecked(TncConfigDefaults.SQUELCH_LEVEL == 0);
                    mDuplexButton.setEnabled(true);
                    mDuplexButton.setChecked(TncConfigDefaults.DUPLEX);
                    mTitle.append(mConnectedDeviceName);
                    mTncService.getAllValues();
                    mTncService.getOutputVolume();                    
               		mTncService.listen();
                    break;
                case BluetoothTncService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    mConnectButton.setChecked(false);
                    mPttButton.setEnabled(false);
                    mPttButton.setChecked(false);
                    mTxDelayPicker.setEnabled(false);
                    mPersistencePicker.setEnabled(false);
                    mSlotTimePicker.setEnabled(false);
                    mTxTailPicker.setEnabled(false);
                    mDcdButton.setEnabled(false);
                    mDuplexButton.setEnabled(false);
                    mConnTrackButton.setEnabled(false);
                    mInfoButton.setEnabled(false);
                    mInputAttenButton.setEnabled(false);
                    break;
                case BluetoothTncService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    mConnectButton.setChecked(false);
                    mPttButton.setEnabled(false);
                    mPttButton.setChecked(false);
                    mTxDelayPicker.setEnabled(false);
                    mPersistencePicker.setEnabled(false);
                    mSlotTimePicker.setEnabled(false);
                    mTxTailPicker.setEnabled(false);
                    mDcdButton.setEnabled(false);
                    mDuplexButton.setEnabled(false);
                    mConnTrackButton.setEnabled(false);
                    mInfoButton.setEnabled(false);
                    mInputAttenButton.setEnabled(false);
                    break;
                }
                break;
            case MESSAGE_OUTPUT_VOLUME:
                int outputVolume = msg.arg1;
                TncConfig.this.mOutputVolumeLevel.setProgress(outputVolume);
                TncConfig.this.mOutputVolumeText.setText(Integer.toString(msg.arg1));
                if(D) Log.d(TAG, "output volume: " + outputVolume);
                break;
            case MESSAGE_TX_DELAY:
                TncConfig.this.mTxDelayPicker.setValue(msg.arg1);
                if(D) Log.d(TAG, "tx delay: " + msg.arg1);
                break;
            case MESSAGE_PERSISTENCE:
                TncConfig.this.mPersistencePicker.setValue(msg.arg1);
                if(D) Log.d(TAG, "persistence: " + msg.arg1);
                break;
            case MESSAGE_SLOT_TIME:
                TncConfig.this.mSlotTimePicker.setValue(msg.arg1);
                if(D) Log.d(TAG, "slot time: " + msg.arg1);
                break;
            case MESSAGE_TX_TAIL:
                TncConfig.this.mTxTailPicker.setValue(msg.arg1);
                if(D) Log.d(TAG, "tx tail: " + msg.arg1);
                break;
            case MESSAGE_DUPLEX:
                TncConfig.this.mDuplexButton.setChecked(msg.arg1 != 0);
                if(D) Log.d(TAG, "duplex: " + msg.arg1);
                break;
            case MESSAGE_SQUELCH_LEVEL:
            	TncConfig.this.mDcdButton.setEnabled(true);
                TncConfig.this.mDcdButton.setChecked(msg.arg1 == 0);
                if(D) Log.d(TAG, "DCD: " + msg.arg1);
               break;
            case MESSAGE_VERBOSITY:
            	TncConfig.this.mInfoButton.setEnabled(true);
                TncConfig.this.mInfoButton.setChecked(msg.arg1 != 0);
                if(D) Log.d(TAG, "info: " + msg.arg1);
                break;
            case MESSAGE_BT_CONN_TRACK:
            	TncConfig.this.mConnTrackButton.setEnabled(true);
                TncConfig.this.mConnTrackButton.setChecked(msg.arg1 != 0);
                if(D) Log.d(TAG, "bt conn track: " + msg.arg1);
                break;
            case MESSAGE_INPUT_ATTEN:
            	TncConfig.this.mInputAttenButton.setEnabled(true);
                TncConfig.this.mInputAttenButton.setChecked(msg.arg1 != 0);
                if(D) Log.d(TAG, "input atten: " + msg.arg1);
                break;
            case MESSAGE_BATTERY_LEVEL:
//                TncConfig.this.mDuplexButton.setChecked(msg.arg1 != 0);
            	buffer = (byte[]) msg.obj;
            	int battery_level = buffer[0] * 256 + buffer[1];
                if(D) Log.d(TAG, "battery level: " + battery_level + "mV");
                break;
            case MESSAGE_HW_VERSION:
            	buffer = (byte[]) msg.obj;
                String hwVersion = new String(buffer);
                TncConfig.this.hwVersion = hwVersion;
                if(D) Log.d(TAG, "hw version: " + hwVersion);
                break;
            case MESSAGE_FW_VERSION:
            	buffer = (byte[]) msg.obj;
                String fwVersion = new String(buffer);
                TncConfig.this.fwVersion = fwVersion;
                if(D) Log.d(TAG, "fw version: " + fwVersion);
                break;
            case MESSAGE_INPUT_VOLUME:
                int inputVolume = msg.arg1;
                double level = (Math.log((double)inputVolume) / Math.log(2.0)) / 8.0;
                TncConfig.this.mInputVolumeLevel.setLevel(level);
                // if(D) Log.d(TAG, "input volume: " + level);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == ActionBarActivity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mTncService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode != ActionBarActivity.RESULT_OK) {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
