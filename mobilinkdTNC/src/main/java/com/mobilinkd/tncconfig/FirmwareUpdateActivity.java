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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity downloads firmware, connects the Bluetooth device, and uploads
 * the firmware to the TNC.
 * 
 * It does the following, in order:
 * 
 * - Verify that a Bluetooth adapter exists otherwise exit. - Verify that the
 * Bluetooth adapter is enabled, otherwise request BT enable. - Select the TNC
 * from Device List. - Connect to TNC - Download the firmware. It is small and
 * should be fast. - Verify the bootloader. - Verify the hardware. - Erase TNC
 * firmware. - Upload TNC firmware. - Verify TNC firmware. - Notify user.
 * 
 * If the Upload or Verify steps fail or the user cancels the operation, the TNC
 * is erased to ensure that the bootloader is active.
 * 
 * @author rob
 * 
 */
public class FirmwareUpdateActivity extends Activity {

	// UUID for this serial port protocol
	private static final UUID SPP_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private static final int MESSAGE_CONNECTING = 1;
	private static final int MESSAGE_CONNECT_FAILED = 2;
	private static final int MESSAGE_CONNECTED = 4;
	private static final int MESSAGE_DOWNLOADING = 5;
	private static final int MESSAGE_DOWNLOADED = 6;
	private static final int MESSAGE_INFO = 10;
	private static final int MESSAGE_TOAST = 11;

	public static final int MESSAGE_AVR109_INITIALIZING = 21;
	public static final int MESSAGE_AVR109_INITIALIZED = 22;
	public static final int MESSAGE_AVR109_INITIALIZATION_FAILED = 23;

	public static final int MESSAGE_AVR109_LOADING = 24;
	public static final int MESSAGE_AVR109_LOADED = 25;
	public static final int MESSAGE_AVR109_LOAD_FAILED = 26;

	public static final int MESSAGE_AVR109_VERIFYING = 27;
	public static final int MESSAGE_AVR109_VERIFIED = 28;
	public static final int MESSAGE_AVR109_VERIFICATION_FAILED = 29;

	public static final int MESSAGE_AVR109_COMPLETE = 30;
	public static final int MESSAGE_AVR109_ACTIVE = 31;

	private static final int STATE_NONE = 0;
	private static final int STATE_SELECTING = 1;
	private static final int STATE_CONNECTING = 2;
	private static final int STATE_CONNECTED = 3;

	public static final String TOAST = "toast";

	// Debugging
	private static final String TAG = "FirmwareUpdate";
	private static final boolean D = true;

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothSocket mSocket = null;
	private ConnectThread mConnectThread = null;
	private FirmwareDownloadThread mFirmwareDownloadThread = null;
	private Avr109 mFirmwareUploadThread = null;
	private Uri mUri;
	private Firmware mFirmware;

	private TextView mLog;
	private ProgressBar mProgressBar;
	private Button mCloseButton;
	private ProgressDialog mDialog;

	private int mState;

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
		
		if(getResources().getBoolean(R.bool.portrait_only)){
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    }
		
		if(getResources().getBoolean(R.bool.landscape_only)){
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	    }
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		}
		setContentView(R.layout.activity_firmware_update);

		mLog = (TextView) findViewById(R.id.firmware_update_log);
		mProgressBar = (ProgressBar) findViewById(R.id.upload_progress_bar);
		mCloseButton = (Button) findViewById(R.id.firmware_close_button);

		mCloseButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				try {
					mSocket.close();
				} catch (IOException e) {
					// ignore
				}
				finish();
			}
		});

		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		Intent intent = getIntent();
		mUri = intent.getData();
		mLog.append("Firmware URI: " + mUri + "\n");

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			mLog.append("Bluetooth is not available\n");
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.firmware_update, menu);
		return true;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		if (mState == STATE_NONE) {
			new AlertDialog.Builder(FirmwareUpdateActivity.this)
					.setTitle("Mobilinkd Firmware")
					.setMessage(R.string.firmware_upload_notification)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									onStartCont();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							}).show();
		}
		else
		{
			onStartCont();
		}
	}
	
	private void onStartCont() {
		
		if (!mBluetoothAdapter.isEnabled()) {
			mLog.append("Bluetooth not enabled\n");
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			if (mState == STATE_NONE) {
				mLog.append("Bluetooth is enabled\n");
				selectBluetooth();
			}
		}
	}

	@Override
	public synchronized void onPause() {
		// erase firmware.
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
		// Reset the ConnectThread because we're done
		synchronized (this) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth TNC services
		// if (mTncService != null) mTncService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	private void setState(int state) {
		mState = state;
	}

	private void selectBluetooth() {
		setState(STATE_SELECTING);
		mLog.append("Selecting Bluetooth device...\n");
		new AlertDialog.Builder(FirmwareUpdateActivity.this)
		.setTitle("Select Device")
		.setMessage(R.string.select_device_notification)
		.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						// Launch the DeviceListActivity to see devices and do scan
						Intent selectDeviceIntent = new Intent(FirmwareUpdateActivity.this,
								DeviceListActivity.class);
						startActivityForResult(selectDeviceIntent, REQUEST_CONNECT_DEVICE);
					}
				})
		.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int which) {
						finish();
					}
				}).show();
	}

	private void connectBluetooth(BluetoothDevice device) {
		setState(STATE_CONNECTING);
		mLog.append("Connecting to Bluetooth device...\n");
		mConnectThread = new ConnectThread(device, mHandler);
		mConnectThread.start();
	}

	private void downloadFirmware() {
		assert (mUri != null);
		assert (mHandler != null);
		assert (mSocket != null);

		FirmwareUpdateActivity.this.mProgressBar.setVisibility(View.VISIBLE);

		mFirmwareDownloadThread = new FirmwareDownloadThread(mUri, mHandler);
		mFirmwareDownloadThread.start();
	}

	private void uploadFirmware() {
		assert (mFirmware != null);
		setState(STATE_CONNECTED);

		FirmwareUpdateActivity.this.mProgressBar.setVisibility(View.INVISIBLE);

		mLog.append("Uploading firmware...\n");
		
		mDialog = new ProgressDialog(this);
		mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mDialog.setMax(100);
        mDialog.setMessage(getString(R.string.initializing_firmware));
        mDialog.setCancelable(false);
        mDialog.show();
        
		mFirmwareUploadThread = new Avr109(this, mSocket, mFirmware, mHandler);
		mFirmwareUploadThread.start();
	}

	// The Handler that gets information back from the BluetoothTncService
	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_CONNECTING:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			case MESSAGE_CONNECT_FAILED:
				Toast.makeText(getApplicationContext(),
						"Connection failed.\nIs the device on?",
						Toast.LENGTH_LONG).show();
				finish();
				break;
			case MESSAGE_CONNECTED:
				if (D)
					Log.i(TAG, "MESSAGE_CONNECTED");
				downloadFirmware();
				break;
			case MESSAGE_DOWNLOADING:
				if (D)
					Log.i(TAG, "MESSAGE_DOWNLOADING");
				mLog.append(msg.getData().getString(TOAST));
				break;
			case MESSAGE_DOWNLOADED:
				if (D)
					Log.i(TAG, "MESSAGE_DOWNLOADED");
				mLog.append("Firmware downloaded\n");
				mFirmware = (Firmware) msg.obj;
				new AlertDialog.Builder(FirmwareUpdateActivity.this)
						.setTitle("Upload Firmware")
						.setMessage(R.string.firmware_upload_warning)
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										uploadFirmware();
									}
								})
						.setNegativeButton("No",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										finish();
									}
								}).show();
				break;
			case MESSAGE_INFO:
				mLog.append(msg.getData().getString(TOAST));
				break;
			case MESSAGE_TOAST:
				FirmwareUpdateActivity.this.mProgressBar.setVisibility(View.INVISIBLE);
				mLog.append("failed\n");
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			case MESSAGE_AVR109_INITIALIZING:
				mDialog.setProgress(50);
				mLog.append("Initializing bootloader...");
				break;
			case MESSAGE_AVR109_INITIALIZED:
				mDialog.setProgress(100);
				mLog.append("complete\n");
				break;
			case MESSAGE_AVR109_INITIALIZATION_FAILED:
				mDialog.dismiss();
				mLog.append("failed\n");
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_LONG)
						.show();
				finish();
				break;
			case MESSAGE_AVR109_LOADING:
				mLog.append("Writing firmware...");
		        mDialog.setMessage(getString(R.string.writing_firmware));
				break;
			case MESSAGE_AVR109_LOADED:
				mLog.append("complete\n");
				break;
			case MESSAGE_AVR109_LOAD_FAILED:
				mDialog.dismiss();
				mLog.append("failed\n");
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_LONG)
						.show();
				FirmwareUpdateActivity.this.mProgressBar
						.setVisibility(View.INVISIBLE);
				break;
			case MESSAGE_AVR109_VERIFYING:
				mLog.append("Verifying firmware...");
		        mDialog.setMessage("Verifying firmware...");
				break;
			case MESSAGE_AVR109_VERIFIED:
				mDialog.dismiss();
				mLog.append("success!\n");
				FirmwareUpdateActivity.this.mProgressBar
						.setVisibility(View.INVISIBLE);
				break;
			case MESSAGE_AVR109_VERIFICATION_FAILED:
				mDialog.dismiss();
				mLog.append("failed\n");
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_LONG)
						.show();
				finish();
				break;
			case MESSAGE_AVR109_ACTIVE:
				mDialog.setMax(msg.arg1);
				mDialog.setProgress(msg.arg2);
				break;
			}
		}
	};

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed(String message) {
		setState(STATE_NONE);

		Log.i(TAG, message);

		mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_CONNECT_FAILED)
				.sendToTarget();
	}

	private void send(int type, String message) {
		Message msg = mHandler.obtainMessage(type);
		Bundle bundle = new Bundle();
		bundle.putString(FirmwareUpdateActivity.TOAST, message + "\n");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	private void connecting(BluetoothDevice device) {
		setState(STATE_CONNECTING);

		StringBuilder builder = new StringBuilder();
		builder.append("Connecting to '");
		builder.append(device.getName());
		builder.append("'");

		Message msg = mHandler
				.obtainMessage(FirmwareUpdateActivity.MESSAGE_CONNECTING);
		Bundle bundle = new Bundle();
		bundle.putString(FirmwareUpdateActivity.TOAST, builder.toString());
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	private void connected(BluetoothSocket socket, BluetoothDevice device) {
		setState(STATE_CONNECTED);

		mSocket = socket;

		mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_CONNECTED)
				.sendToTarget();
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothDevice mDevice;
		private BluetoothSocket mSocket;
		@SuppressWarnings("unused")
		private Handler mHandler;

		public ConnectThread(BluetoothDevice device, Handler handler) {
			mDevice = device;
			mHandler = handler;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			connecting(mDevice);

			// Make a connection to the BluetoothSocket
			try {
				mSocket = mDevice.createRfcommSocketToServiceRecord(SPP_UUID);
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mSocket.connect();
			} catch (IOException e) {
				// Close the socket
				try {
					mSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,
							"unable to close() socket during connection failure",
							e2);
				}
				connectionFailed(e.toString());
				return;
			}

			connected(mSocket, mDevice);

			// Reset the ConnectThread because we're done
			synchronized (FirmwareUpdateActivity.this) {
				mConnectThread = null;
			}
		}

		public void cancel() {
			try {
				mSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	private void firmwareDownloadFailed(Uri uri) {
		Message msg = mHandler
				.obtainMessage(FirmwareUpdateActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(FirmwareUpdateActivity.TOAST, "Unable to download "
				+ uri.toString());
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	private void downloaded(Firmware firmware) {
		mHandler.obtainMessage(FirmwareUpdateActivity.MESSAGE_DOWNLOADED, 0, 0,
				firmware).sendToTarget();
	}

	/**
	 * This thread reads the firmware to be uploaded from the given URI.
	 */
	private class FirmwareDownloadThread extends Thread {

		private final Uri mUri;
		@SuppressWarnings("unused")
		private Handler mHandler;
		@SuppressWarnings("unused")
		private BluetoothDevice mDevice;
		private Firmware mFirmware;

		public FirmwareDownloadThread(Uri uri, Handler handler) {
			mUri = uri;
			mHandler = handler;
			mFirmware = null;
		}

		private InputStream getInputStream(Uri uri) throws IOException {
			String scheme = mUri.getScheme();
			try {
				if ("content".equals(scheme)) {
					return FirmwareUpdateActivity.this.getContentResolver().openInputStream(mUri);
				} else if ("file".equals(scheme)) {
					File file = new File(mUri.toString());
					return new FileInputStream(file);
				} else {
					URL url = new URL(mUri.toString());
					return url.openConnection().getInputStream();
				}
			} catch (IOException x) {
			    System.err.format("IOException: %s%n", x);
				throw(x);
			}
		}
		
		public void run() {
			Log.i(TAG, "BEGIN FirmwareDownloadThread");
			setName("FirmwareDownloadThread");

			send(FirmwareUpdateActivity.MESSAGE_INFO, "Downloading firmware...");

			try {
				if (D) Log.e(TAG, "Firmware URI: " + mUri);
				
				InputStream inputStream = getInputStream(mUri);
				mFirmware = new Firmware(inputStream);

				synchronized (FirmwareUpdateActivity.this) {
					mFirmwareDownloadThread = null;
				}

				downloaded(mFirmware);
			} catch (IOException e) {
				firmwareDownloadFailed(mUri);
			} catch (IllegalArgumentException e) {
				firmwareDownloadFailed(mUri);
			}
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult for " + requestCode + " is "
					+ resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode != ActionBarActivity.RESULT_OK) {
				mLog.append("No Bluetooth device was selected\n");
				Log.d(TAG, "BT not selected");
				Toast.makeText(this, "No Bluetooth device was selected",
						Toast.LENGTH_SHORT).show();
				finish();
				return;
			}

			// Get the device MAC address
			String address = data.getExtras().getString(
					DeviceListActivity.EXTRA_DEVICE_ADDRESS);
			mLog.append("Selected Bluetooth device " + address + "\n");
			connectBluetooth(mBluetoothAdapter.getRemoteDevice(address));
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode != ActionBarActivity.RESULT_OK) {
				mLog.append("Bluetooth is not enabled\n");
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			selectBluetooth();
			break;
		}
	}

}
