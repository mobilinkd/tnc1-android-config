package com.mobilinkd.tncconfig;

import com.google.speech.levelmeter.BarLevelDrawable;

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
import android.view.ViewGroup;
import android.widget.CheckedTextView;

public class AudioInputFragment extends DialogFragment {
	
    // Debugging
    private static final String TAG = "AudioInputFragment";
    private static final boolean D = true;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     * */
    public interface Listener {
        public void onAudioInputDialogClose(AudioInputFragment dialog);
        public void onAudioInputDialogPause(AudioInputFragment dialog);
        public void onAudioInputDialogResume(AudioInputFragment dialog);
        public void onAudioInputDialogChanged(AudioInputFragment dialog);
    }
	
    private View mDialogView = null;
    
    private boolean mHasInputAtten = false;
	private boolean mInputAtten = true;

	private BarLevelDrawable mInputVolumeLevel = null;
    private CheckedTextView mInputAttenView = null;

	private Listener mListener = null;

	private View configureDialogView(View view) {
		
        mInputVolumeLevel = (BarLevelDrawable) view.findViewById(R.id.bar_level_drawable_view);
        mInputAttenView = (CheckedTextView) view.findViewById(R.id.inputAttenCheckBox);

        mInputAttenView.setChecked(mInputAtten);
        mInputAttenView.setEnabled(mHasInputAtten);

        mInputAttenView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
            	((CheckedTextView) view).toggle();
            	mInputAtten = ((CheckedTextView) view).isChecked();
                Log.i(TAG, "mInputAtten changed: " + mInputAtten);
                mListener.onAudioInputDialogChanged(AudioInputFragment.this);
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
            View view = getActivity().getLayoutInflater().inflate(R.layout.audio_input_fragment, null);    
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
        mDialogView = inflater.inflate(R.layout.audio_input_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(R.string.audio_input_settings)
               .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mListener != null) {
                    	   mListener.onAudioInputDialogClose(AudioInputFragment.this);
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
            // Instantiate the listener so we can send events to the host
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement AudioInputFragment.Listener");
        }
    }

    @Override
    public void onPause() {
    	super.onPause();
    	
		mListener.onAudioInputDialogPause(AudioInputFragment.this);
        if(D) Log.d(TAG, "++ ON PAUSE ++");
    }

    @Override
    public void onResume() {
    	super.onResume();
    	
		mListener.onAudioInputDialogResume(AudioInputFragment.this);
        if(D) Log.d(TAG, "++ ON RESUME ++");
    }

    public void setInputAtten(boolean value) {
    	mHasInputAtten = true;
    	mInputAtten = value;
    }
    
    public boolean getInputAtten() {
    	return mInputAtten;
    }
    
    public void setInputVolume(double level) {
    	if (mInputVolumeLevel != null) {
    		mInputVolumeLevel.setLevel(level);
    	}
    }
}
