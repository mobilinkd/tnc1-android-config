package com.mobilinkd.tncconfig;

import androidx.annotation.NonNull;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
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

    private boolean mPowerOn = false;
	private boolean mPowerOff = false;
	private int mBatteryLevel = 0;
	
    private TextView mVoltageView;
    private ProgressBar mVoltageMeter;

    private Listener mListener = null;
	private boolean mPowerControl = false;
	
	private View configureDialogView(View view) {
		
        mVoltageView = (TextView) view.findViewById(R.id.textView2);
        mVoltageMeter = (ProgressBar) view.findViewById(R.id.battery_meter_bar);
        CheckedTextView mPowerOnView = (CheckedTextView) view.findViewById(R.id.checkBox1);
        CheckedTextView mPowerOffView = (CheckedTextView) view.findViewById(R.id.checkBox2);

        String mv = String.format(getString(R.string.battery_level_mv), mBatteryLevel);
        mVoltageView.setText(mv);
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
    	
    	
        if(D) Log.d(TAG, "+++ ON CREATE VIEW +++");

        if (getShowsDialog()) {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.power_fragment, null);
            return configureDialogView(view);
        }
    }

    
    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Get the layout inflater
        FragmentActivity activity = getActivity();
        if (activity == null) {
            throw new AssertionError();
        }
        LayoutInflater inflater = activity.getLayoutInflater();

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.FullscreenTheme));

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View mDialogView = inflater.inflate(R.layout.power_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(R.string.power_settings_title)
               .setIcon(R.drawable.perm_group_affects_battery)
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if(D) Log.d(TAG, "++ ON ATTACH ++");

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (Listener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
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
        if (isAdded()) {
            String mv = String.format(getString(R.string.battery_level_mv), mBatteryLevel);
            mVoltageView.setText(mv);
            mVoltageMeter.setProgress((mBatteryLevel - 3300) / 10);
        }
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
