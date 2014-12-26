package com.mobilinkd.tncconfig;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class AudioOutputFragment extends DialogFragment {
    // Debugging
    private static final String TAG = "AudioOutputFragment";
    private static final boolean D = true;

    public static final int PTT_STYLE_SIMPLEX = 0;
    public static final int PTT_STYLE_MULTIPLEX = 1;
    
    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     * */
    public interface Listener {
        public void onAudioOutputDialogClose(AudioOutputFragment dialog);
        public void onAudioOutputDialogPttStyleChanged(AudioOutputFragment dialog);
        public void onAudioOutputDialogLevelChanged(AudioOutputFragment dialog);
        public void onAudioOutputDialogToneChanged(AudioOutputFragment dialog);
    }
	
    private View mDialogView = null;
    private Context mContext = null;
    
    private boolean mHasPttStyle = false;
	private int mPttStyle = PTT_STYLE_SIMPLEX;

	private FrameLayout mPttStyleLayout = null;
    private RadioGroup mPttStyleGroup = null;
    private ImageButton mPttStyleHelpButton = null;

	private int mVolume = 0;
    private SeekBar mOutputVolumeLevel = null;
    private TextView mOutputVolumeText = null;
    
    private int mTone = TncConfig.TONE_MARK;
    private boolean mPtt = false;
    
    private RadioGroup mToneGroup = null;
    private ToggleButton mPttButton = null;

	private Listener mListener = null;

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        mDialogView = inflater.inflate(R.layout.audio_output_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(R.string.audio_output_settings)
               .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mListener != null) {
                    	   mListener.onAudioOutputDialogClose(AudioOutputFragment.this);
                       }
                   }
               });

        mPttStyleGroup = (RadioGroup) mDialogView.findViewById(R.id.pttStyleGroup);
        mPttStyleGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        	@Override
        	public void onCheckedChanged(RadioGroup group, int selected) {
        		switch (selected) {
        		case R.id.pttStyleSimplexButton:
        			mPttStyle = PTT_STYLE_SIMPLEX;
        			break;
        		case R.id.pttStyleMultiplexButton:
        			mPttStyle = PTT_STYLE_MULTIPLEX;
        			break;
        		default:
        			if(D) Log.e(TAG, "Invalid button ID");
        			break;
        		}
        		mListener.onAudioOutputDialogPttStyleChanged(AudioOutputFragment.this);
        	}
        });
        
        mPttStyleLayout = (FrameLayout) mDialogView.findViewById(R.id.pttStyleLayout);
        if (mHasPttStyle) {
        	mPttStyleLayout.setVisibility(FrameLayout.VISIBLE);

        	switch (mPttStyle) {
        	case PTT_STYLE_SIMPLEX:
        		mPttStyleGroup.check(R.id.pttStyleSimplexButton);
        		break;
        	case PTT_STYLE_MULTIPLEX:
        		mPttStyleGroup.check(R.id.pttStyleMultiplexButton);
        		break;
        	}
        } else {
        	mPttStyleLayout.setVisibility(FrameLayout.GONE);
        }
        
        mPttStyleHelpButton = (ImageButton) mDialogView.findViewById(R.id.pttStyleHelpButton);
        mPttStyleHelpButton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		new AlertDialog.Builder(mContext)
        			.setTitle(R.string.label_ptt_style) 
        			.setMessage(R.string.label_ptt_hint)
        			.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// pass
						}
					}).show();
        	}
        });
        mPttStyleHelpButton.getBackground().setAlpha(64);
        
        mOutputVolumeText = (TextView) mDialogView.findViewById(R.id.outputVolumeText);
    	mOutputVolumeText.setText(Integer.toString(mVolume));
        mOutputVolumeLevel = (SeekBar) mDialogView.findViewById(R.id.outputVolumeLevel);
        mOutputVolumeLevel.setProgress(mVolume);
        mOutputVolumeLevel.setEnabled(mPtt);
        mOutputVolumeLevel.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        	@Override
            public void onProgressChanged(SeekBar seekbar, int level, boolean fromUser) {

            	if (fromUser) {
            		mVolume = level;
            		mListener.onAudioOutputDialogLevelChanged(AudioOutputFragment.this);
                }
            	mOutputVolumeText.setText(Integer.toString(mVolume));
            }
        	@Override
            public void onStartTrackingTouch(SeekBar seekbar) {
            }
        	@Override
            public void onStopTrackingTouch(SeekBar seekbar) {
            }
        });
        
        mToneGroup = (RadioGroup) mDialogView.findViewById(R.id.toneGroup);
        mToneGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        	@Override
        	public void onCheckedChanged(RadioGroup group, int selected) {
        		switch (selected) {
        		case R.id.markToneRadioButton:
        			mTone = TncConfig.TONE_MARK;
        			break;
        		case R.id.spaceToneRadioButton:
        			mTone = TncConfig.TONE_SPACE;
        			break;
        		case R.id.bothToneRadioButton:
        			mTone = TncConfig.TONE_BOTH;
        			break;
        		default:
        			if(D) Log.e(TAG, "Invalid button ID");
        			break;
        		}
        		if(D) Log.i(TAG, "Tone changed: " + mTone);
        		
        		if (mPtt) {
        			mListener.onAudioOutputDialogToneChanged(AudioOutputFragment.this);
        		}
        	}
        });
        
        mPttButton = (ToggleButton) mDialogView.findViewById(R.id.transmitButton);
        mPttButton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		mPtt = ((ToggleButton) view).isChecked();
                for(int i = 0; i < mPttStyleGroup.getChildCount(); i++){
                    ((RadioButton) mPttStyleGroup.getChildAt(i)).setEnabled(!mPtt);
                }
        		mOutputVolumeLevel.setEnabled(mPtt);
    			mListener.onAudioOutputDialogToneChanged(AudioOutputFragment.this);
        	}
        });
        mPttButton.getBackground().setAlpha(64);

        
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
            mContext = (Context) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement AudioOutputFragment.Listener");
        }
    }

    public void setPttStyle(int style) {
    	mHasPttStyle = true;
    	mPttStyle = style;
    }
    
    public int getPttStyle() {
    	return mPttStyle;
    }
    
    public void setVolume(int level) {
    	mVolume = level;
    }

    public int getVolume() {
    	return mVolume;
    }

    public int getTone() {
    	return mTone;
    }
    
    public boolean getPtt() {
    	return mPtt;
    }
}
