package com.mobilinkd.tncconfig;

import android.app.Application;
import android.os.Handler;

public class TncConfigApplication extends Application {

    private BluetoothTncService mTncService = null;
    private Handler mHandler = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public BluetoothTncService getBluetoothTncService() {
        return mTncService;
    }

    public void setBluetoothTncService(BluetoothTncService tncService) {
    	mTncService = tncService;
    }
    
    public Handler getHandler() {
    	return mHandler;
    }
    
    public void setHandler(Handler handler) {
    	mHandler = handler;
    }
}
