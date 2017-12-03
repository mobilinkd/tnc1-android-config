package com.mobilinkd.tncconfig;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PowerFragment extends DialogFragment {
	
    // Debugging
    private static final String TAG = "PowerFragment";
    private static final boolean D = true;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     * */
    public interface Listener {
        public void onPowerDialogUpdate(PowerFragment dialog);
        public void onPowerDialogResume(PowerFragment dialog);
    }
	
    private View mDialogView = null;
    
	private boolean mPowerOn = false;
	private boolean mPowerOff = false;
	private int mBatteryLevel = 0;
	
    private TextView mVoltageView;
    private ProgressBar mVoltageMeter;
    private CheckedTextView mPowerOnView;
    private CheckedTextView mPowerOffView;
	
	private Listener mListener = null;
	private boolean mPowerControl = false;
	
	private View configureDialogView(View view) {
		
        mVoltageView = (TextView) view.findViewById(R.id.textView2);
        mVoltageMeter = (ProgressBar) view.findViewById(R.id.battery_meter_bar);
        mPowerOnView = (CheckedTextView) view.findViewById(R.id.checkBox1);
        mPowerOffView = (CheckedTextView) view.findViewById(R.id.checkBox2);

        mVoltageView.setText(mBatteryLevel + "mV");
        mVoltageMeter.setProgress((mBatteryLevel - 3300) / 10);
        mPowerOnView.setChecked(mPowerOn);
        mPowerOffView.setChecked(mPowerOff);

    	mPowerOnView.setEnabled(mPowerControl);
    	mPowerOnView.setClickable(mPowerControl);
    	mPowerOnView.setVisibility(mPowerControl ?  View.VISIBLE :  View.GONE);
    	mPowerOffView.setEnabled(mPowerControl);
    	mPowerOffView.setClickable(mPowerControl);
    	mPowerOffView.setVisibility(mPowerControl ?  View.VISIBLE :  View.GONE);

    	mPowerOnView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
            	((CheckedTextView) view).toggle();
                mPowerOn = ((CheckedTextView) view).isChecked();
                Log.i(TAG, "mPowerOn changed: " + mPowerOn);
                if (mListener != null) {
             	   mListener.onPowerDialogUpdate(PowerFragment.this);
                }
            }
        });
        
        mPowerOffView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
            	((CheckedTextView) view).toggle();
	            mPowerOff = ((CheckedTextView) view).isChecked();
	            Log.i(TAG, "mPowerOff changed: " + mPowerOff);
                if (mListener != null) {
             	   mListener.onPowerDialogUpdate(PowerFragment.this);
                }
	        }
	    });
        
		return view;
	}	


	@SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
    	
        if(D) Log.d(TAG, "+++ ON CREATE VIEW +++");

        if (getShowsDialog() == true) {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            View view = getActivity().getLayoutInflater().inflate(R.layout.power_fragment, null);    
            return configureDialogView(view);
        }
    }

    
    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        mDialogView = inflater.inflate(R.layout.power_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(R.string.power_settings)
               .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mListener != null) {
                    	   mListener.onPowerDialogUpdate(PowerFragment.this);
                       }
                   }
               });

        configureDialogView(mDialogView);        
        if(D) Log.d(TAG, "+++ ON CREATE DIALOG +++");
        
        return builder.create();
    }
   
    @Override
    public void onStart() {
    	super.onStart();


        if(D) Log.d(TAG, "++ ON START ++");
     }

    @Override
    public void onStop() {
    	super.onStop();

        if(D) Log.d(TAG, "++ ON STOP ++");
     }
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if(D) Log.d(TAG, "++ ON ATTACH ++");

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PowerFragment.Listener");
        }
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	
		mListener.onPowerDialogUpdate(PowerFragment.this);
        if(D) Log.d(TAG, "++ ON PAUSE ++");
    }

    @Override
    public void onResume() {
    	super.onResume();
    	
		mListener.onPowerDialogResume(PowerFragment.this);
        if(D) Log.d(TAG, "++ ON RESUME ++");
    }

    public void setBatteryLevel(int level) {
    	mBatteryLevel = level;
    }
    
    public void setPowerOn(boolean value) {
    	mPowerControl  = true;
    	mPowerOn = value;
    }
    
    public boolean getPowerOn() {
    	return mPowerOn;
    }

    public void setPowerOff(boolean value) {
    	mPowerControl  = true;
    	mPowerOff = value;
    }
    
    public boolean getPowerOff() {
    	return mPowerOff;
    }
}
