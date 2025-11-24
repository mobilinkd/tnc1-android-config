package com.mobilinkd.tncconfig;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.Locale;

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
        void onAudioOutputDialogClose(AudioOutputFragment dialog);
        void onAudioOutputDialogPttStyleChanged(AudioOutputFragment dialog);
        void onAudioOutputDialogLevelChanged(AudioOutputFragment dialog);
        void onAudioOutputDialogToneChanged(AudioOutputFragment dialog);
        void onAudioOutputDialogTwistLevelChanged(AudioOutputFragment dialog);
    }

    private Context mContext = null;
    
    private boolean mHasPttStyle = false;
	private int mPttStyle = PTT_STYLE_SIMPLEX;

    private RadioGroup mPttStyleGroup = null;

    private int mVolume = 0;
    private SeekBar mOutputVolumeLevel = null;
    private TextView mOutputVolumeText = null;
    
    private int mTone = TncConfig.TONE_MARK;
    private boolean mPtt = false;

    private ToggleButton mPttButton = null;

	private Listener mListener = null;

    private long mLastOutputVolumeUpdateTimestamp = 0;

    private boolean mHasOutputTwistControl = false;
    private TextView mOutputTwistLevelText = null;
    private SeekBar mOutputTwistLevelBar = null;
    private int mOutputTwistLevel = 0;
    private long mLastOutputTwistUpdateTimestamp = 0;

    @SuppressLint("SetTextI18n")
    private View configureDialogView(View view)
	{
        mPttStyleGroup = view.findViewById(R.id.pttStyleGroup);
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

        FrameLayout mPttStyleLayout = view.findViewById(R.id.pttStyleLayout);
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

        ImageButton mPttStyleHelpButton = view.findViewById(R.id.pttStyleHelpButton);
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
        
        mOutputVolumeText = view.findViewById(R.id.outputVolumeText);
    	mOutputVolumeText.setText(Integer.toString(mVolume));
        mOutputVolumeLevel = view.findViewById(R.id.outputVolumeLevel);
        mOutputVolumeLevel.setProgress(mVolume);
        mOutputVolumeLevel.setEnabled(mPtt);
        mOutputVolumeLevel.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        	@Override
            public void onProgressChanged(SeekBar seekbar, int level, boolean fromUser) {

            	if (fromUser) {
            	    long now = System.currentTimeMillis();
            		mVolume = level;
            		if (now - mLastOutputVolumeUpdateTimestamp >= 100) {
                        mListener.onAudioOutputDialogLevelChanged(AudioOutputFragment.this);
                        mLastOutputVolumeUpdateTimestamp = now;
                    }
                }
            	mOutputVolumeText.setText(Integer.toString(mVolume));
            }
        	@Override
            public void onStartTrackingTouch(SeekBar seekbar) {
            }
        	@Override
            public void onStopTrackingTouch(SeekBar seekbar) {
                mListener.onAudioOutputDialogLevelChanged(AudioOutputFragment.this);
                mLastOutputVolumeUpdateTimestamp = System.currentTimeMillis();
            }
        });
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

        RadioGroup mToneGroup = view.findViewById(R.id.toneGroup);
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
        
        mPttButton = view.findViewById(R.id.transmitButton);
        mPttButton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		mPtt = ((ToggleButton) view).isChecked();
                for(int i = 0; i < mPttStyleGroup.getChildCount(); i++){
                    mPttStyleGroup.getChildAt(i).setEnabled(!mPtt);
                }
        		mOutputVolumeLevel.setEnabled(mPtt);
                mOutputTwistLevelBar.setEnabled(mPtt);
    			mListener.onAudioOutputDialogToneChanged(AudioOutputFragment.this);
        	}
        });
        mPttButton.getBackground().setAlpha(64);

        LinearLayout outputTwistLayout = view.findViewById(R.id.outputTwistLayout);

        if (mHasOutputTwistControl) {
            outputTwistLayout.setVisibility(LinearLayout.VISIBLE);
        } else {
            outputTwistLayout.setVisibility(LinearLayout.GONE);
        }

        mOutputTwistLevelText = view.findViewById(R.id.outputTwistLevelText);
        mOutputTwistLevelBar = view.findViewById(R.id.outputTwistLevelBar);
        mOutputTwistLevelBar.setProgress(mOutputTwistLevel);
        mOutputTwistLevelBar.setEnabled(mPtt);
        mOutputTwistLevelText.setText(String.format(Locale.getDefault(),"%d", mOutputTwistLevel));

        ImageButton outputTwistHelpButton = view.findViewById(R.id.outputTwistHelpButton);
        outputTwistHelpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.output_twist_help_title)
                        .setMessage(R.string.output_twist_help)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // pass
                            }
                        }).show();
            }
        });
        outputTwistHelpButton.getBackground().setAlpha(64);

        mOutputTwistLevelBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekbar, int level, boolean fromUser) {

                if (fromUser) {
                    long now = System.currentTimeMillis();
                    mOutputTwistLevel = level;
                    // Limit update rate to 100ms.
                    if (now - mLastOutputTwistUpdateTimestamp >= 100) {
                        mListener.onAudioOutputDialogTwistLevelChanged(AudioOutputFragment.this);
                        mLastOutputTwistUpdateTimestamp = now;
                    }
                }
                mOutputTwistLevelText.setText(String.format(Locale.getDefault(),"%d", mOutputTwistLevel));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {
                mListener.onAudioOutputDialogTwistLevelChanged(AudioOutputFragment.this);
                mLastOutputTwistUpdateTimestamp = System.currentTimeMillis();
            }
        });

        return view;
	}

    @SuppressLint("InflateParams")
	@Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
    	
        if(D) Log.d(TAG, "+++ ON CREATE VIEW +++");

        if (!getShowsDialog()) {
            View view = inflater.inflate(R.layout.audio_output_fragment, null);
            return configureDialogView(view);
        } else {
            return super.onCreateView(inflater, container, savedInstanceState);
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
        View mDialogView = inflater.inflate(R.layout.audio_output_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(R.string.audio_output_title).setIcon(R.drawable.perm_group_audio_settings)
               .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mListener != null) {
                    	   mListener.onAudioOutputDialogClose(AudioOutputFragment.this);
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
    public void onAttach(Context context) {
        super.onAttach(context);

        if(D) Log.d(TAG, "++ ON ATTACH ++");

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the listener so we can send events to the host
            mListener = (Listener) context;
            mContext = context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement AudioOutputFragment.Listener");
        }
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    	mPtt = false;
    	mPttButton.setChecked(false);
		mOutputVolumeLevel.setEnabled(false);
		mListener.onAudioOutputDialogToneChanged(AudioOutputFragment.this);
        if(D) Log.d(TAG, "++ ON PAUSE ++");
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

    public int getOutputTwist()
    {
        return mOutputTwistLevel;
    }

    public void setOutputTwist(int level)
    {
        if(D) Log.d(TAG, "setOutputTwist = " + level);

        mHasOutputTwistControl = true;
        mOutputTwistLevel = level;

        if (isAdded()) {
            mOutputTwistLevelBar.setProgress(mOutputTwistLevel);
            mOutputTwistLevelText.setText(String.format(Locale.getDefault(),"%d",level));
        }
    }

}
