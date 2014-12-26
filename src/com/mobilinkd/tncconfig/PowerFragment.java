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
        public void onPowerDialogClose(PowerFragment dialog);
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
                    	   mListener.onPowerDialogClose(PowerFragment.this);
                       }
                   }
               });

        mVoltageView = (TextView) mDialogView.findViewById(R.id.textView2);
        mVoltageMeter = (ProgressBar) mDialogView.findViewById(R.id.battery_meter_bar);
        mPowerOnView = (CheckedTextView) mDialogView.findViewById(R.id.checkBox1);
        mPowerOffView = (CheckedTextView) mDialogView.findViewById(R.id.checkBox2);

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
                Log.e(TAG, "mPowerOn changed: " + mPowerOn);
            }
        });
        
        mPowerOffView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
            	((CheckedTextView) view).toggle();
	            mPowerOff = ((CheckedTextView) view).isChecked();
	            Log.e(TAG, "mPowerOff changed: " + mPowerOff);
	        }
	    });

        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        return builder.create();
    }
   
    @Override
    public void onStart() {
    	super.onStart();


        if(D) Log.e(TAG, "++ ON START ++");
     }
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if(D) Log.e(TAG, "++ ON ATTACH ++");

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
