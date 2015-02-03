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

public class ModemFragment extends DialogFragment {
	
    // Debugging
    private static final String TAG = "ModemFragment";
    private static final boolean D = true;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     * */
    public interface Listener {
        public void onModemDialogResume(ModemFragment dialog);
        public void onModemDialogUpdate(ModemFragment dialog);
    }
	
    private View mDialogView = null;
    
	private boolean mDcd = false;
    private boolean mHasConnTrack = false;
	private boolean mConnTrack = false;
	private boolean mVerbose = false;
	
    private CheckedTextView mDcdView;
    private CheckedTextView mConnTrackView;
    private CheckedTextView mVerboseView;
	
	private Listener mListener = null;

	private View configureDialogView(View view) {
		
        mDcdView = (CheckedTextView) view.findViewById(R.id.dcdCheckBox);
        mConnTrackView = (CheckedTextView) view.findViewById(R.id.connTrackCheckBox);
        mVerboseView = (CheckedTextView) view.findViewById(R.id.verboseCheckBox);

        mDcdView.setChecked(mDcd);
        mConnTrackView.setChecked(mConnTrack);
        mConnTrackView.setEnabled(mHasConnTrack);
        mVerboseView.setChecked(mVerbose);

        mConnTrackView.setEnabled(mHasConnTrack);
        mConnTrackView.setClickable(mHasConnTrack);

        mDcdView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
            	((CheckedTextView) view).toggle();
                mDcd = ((CheckedTextView) view).isChecked();
                Log.i(TAG, "mDcd changed: " + mDcd);
                if (mListener != null) {
             	   mListener.onModemDialogUpdate(ModemFragment.this);
                }
            }
        });
        
        mConnTrackView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
            	((CheckedTextView) view).toggle();
            	mConnTrack = ((CheckedTextView) view).isChecked();
                Log.i(TAG, "mConnTrack changed: " + mConnTrack);
                if (mListener != null) {
              	   mListener.onModemDialogUpdate(ModemFragment.this);
                 }
            }
        });
        
        mVerboseView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
            	((CheckedTextView) view).toggle();
            	mVerbose = ((CheckedTextView) view).isChecked();
                Log.i(TAG, "mVerbose changed: " + mVerbose);
                if (mListener != null) {
              	   mListener.onModemDialogUpdate(ModemFragment.this);
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
            View view = getActivity().getLayoutInflater().inflate(R.layout.modem_fragment, null);    
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
        mDialogView = inflater.inflate(R.layout.modem_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(R.string.modem_settings)
               .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mListener != null) {
                     	   mListener.onModemDialogUpdate(ModemFragment.this);
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
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if(D) Log.d(TAG, "++ ON ATTACH ++");

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the listener so we can send events to the host
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement ModemFragment.Listener");
        }
    }

    @Override
    public void onPause() {
    	super.onPause();
    	
		mListener.onModemDialogUpdate(ModemFragment.this);
        if(D) Log.d(TAG, "++ ON PAUSE ++");
    }

    @Override
    public void onResume() {
    	super.onResume();
    	
		mListener.onModemDialogResume(ModemFragment.this);
        if(D) Log.d(TAG, "++ ON RESUME ++");
    }

    public void setDcd(boolean value) {
    	mDcd = value;
    }
    
    public boolean getDcd() {
    	return mDcd;
    }

    public void setConnTrack(boolean value) {
    	mConnTrack = value;
    	mHasConnTrack = true;
    }
    
    public boolean hasConnTrack() {
    	return mHasConnTrack;
    }
    
    public boolean getConnTrack() {
    	return mConnTrack;
    }

    public void setVerbose(boolean value) {
    	mVerbose = value;
    }
    
    public boolean getVerbose() {
    	return mVerbose;
    }
}
