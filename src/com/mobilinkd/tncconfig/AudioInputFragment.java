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
        public void onAudioInputDialogChanged(AudioInputFragment dialog);
    }
	
    private View mDialogView = null;
    
    private boolean mHasInputAtten = false;
	private boolean mInputAtten = true;

	private BarLevelDrawable mInputVolumeLevel = null;
    private CheckedTextView mInputAttenView = null;

	private Listener mListener = null;

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

        mInputVolumeLevel = (BarLevelDrawable) mDialogView.findViewById(R.id.bar_level_drawable_view);
        mInputAttenView = (CheckedTextView) mDialogView.findViewById(R.id.inputAttenCheckBox);

        mInputAttenView.setChecked(mInputAtten);
        mInputAttenView.setEnabled(mHasInputAtten);

        mInputAttenView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
            	((CheckedTextView) view).toggle();
            	mInputAtten = ((CheckedTextView) view).isChecked();
                Log.e(TAG, "mInputAtten changed: " + mInputAtten);
                mListener.onAudioInputDialogChanged(AudioInputFragment.this);
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
            // Instantiate the listener so we can send events to the host
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement AudioInputFragment.Listener");
        }
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
