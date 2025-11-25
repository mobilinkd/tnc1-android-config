package com.mobilinkd.tncconfig;

import com.google.speech.levelmeter.BarLevelDrawable;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class AudioInputFragment extends DialogFragment {
	
    // Debugging
    private static final String TAG = "AudioInputFragment";
    private static final boolean D = true;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     * */
    public interface Listener {
        void onAudioInputDialogClose(AudioInputFragment dialog);
        void onAudioInputDialogPause(AudioInputFragment dialog);
        void onAudioInputDialogResume(AudioInputFragment dialog);
        void onAudioInputDialogChanged(AudioInputFragment dialog);
        void onAudioInputDialogGainLevelChanged(AudioInputFragment dialog);
        void onAudioInputDialogTwistLevelChanged(AudioInputFragment dialog);
        void onAudioInputAdjustButtonChanged(AudioInputFragment dialog);
    }

    private boolean mHasInputAtten = false;
	private boolean mInputAtten = true;

	private BarLevelDrawable mInputVolumeLevel = null;

    private SeekBar mInputGainLevelBar = null;
    private SeekBar mInputTwistLevelBar = null;

    private TextView mInputGainMinText = null;
    private TextView mInputGainMaxText = null;
    private TextView mInputGainLevelText = null;
    private int mInputGainLevel = 0;
    private int mInputGainMin = 0;
    private int mInputGainMax = 4;

    private TextView mInputTwistMinText = null;
    private TextView mInputTwistMaxText = null;
    private TextView mInputTwistLevelText = null;
    private int mInputTwistLevel = 6;
    private int mInputTwistMin = -3;
    private int mInputTwistMax = 9;


    private Boolean mHasInputControls = false;

    private Listener mListener = null;
    private Context mContext = null;

    private long mLastInputGainUpdateTimestamp = 0;
    private long mLastInputTwistUpdateTimestamp = 0;

	@SuppressLint("SetTextI18n")
    private View configureDialogView(View view) {
		
        mInputVolumeLevel = view.findViewById(R.id.bar_level_drawable_view);
        CheckedTextView inputAttenView = view.findViewById(R.id.inputAttenCheckBox);

        inputAttenView.setChecked(mInputAtten);
        inputAttenView.setEnabled(mHasInputAtten);

        LinearLayout attenLayout = view.findViewById(R.id.input_atten_layout);
        LinearLayout inputControlsLayout = view.findViewById(R.id.input_controls_layout);

        if (mHasInputControls) {
            attenLayout.setVisibility(LinearLayout.GONE);
            inputControlsLayout.setVisibility(LinearLayout.VISIBLE);
        } else {
            attenLayout.setVisibility(LinearLayout.VISIBLE);
            inputControlsLayout.setVisibility(LinearLayout.GONE);
        }

        mInputGainLevelBar = view.findViewById(R.id.inputGainControl);
        mInputGainLevelBar.setMax(mInputGainMax - mInputGainMin);
        mInputGainLevelBar.setProgress(mInputGainLevel - mInputGainMin);
        mInputGainMinText = view.findViewById(R.id.input_gain_min);
        mInputGainMinText.setText(Integer.toString(mInputGainMin));
        mInputGainMaxText =  view.findViewById(R.id.input_gain_max);
        mInputGainMaxText.setText(Integer.toString(mInputGainMax));
        mInputGainLevelText = view.findViewById(R.id.input_gain_level);
        mInputGainLevelText.setText(Integer.toString(mInputGainLevel - mInputGainMin));

        mInputTwistLevelBar = view.findViewById(R.id.inputTwistControl);
        mInputTwistLevelBar.setMax(mInputTwistMax - mInputTwistMin);
        mInputTwistLevelBar.setProgress(mInputTwistLevel - mInputTwistMin);
        mInputTwistMinText = view.findViewById(R.id.input_twist_min);
        mInputTwistMinText.setText(String.format(getString(R.string.input_twist_db), mInputTwistMin));
        mInputTwistMaxText = view.findViewById(R.id.input_twist_max);
        mInputTwistMaxText.setText(String.format(getString(R.string.input_twist_db), mInputTwistMax));
        mInputTwistLevelText = view.findViewById(R.id.input_twist_level);
        mInputTwistLevelText.setText(String.format(getString(R.string.input_twist_db), mInputTwistLevel));

        inputAttenView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
            	((CheckedTextView) view).toggle();

                mInputAtten = ((CheckedTextView) view).isChecked();
                Log.i(TAG, "mInputAtten changed: " + mInputAtten);
                mListener.onAudioInputDialogChanged(AudioInputFragment.this);
            }
        });

        mInputGainLevelBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekbar, int level, boolean fromUser) {

                if (fromUser) {
                    long now = System.currentTimeMillis();
                    mInputGainLevel = level + mInputGainMin;
                    // Limit update rate to 100ms.
                    if (now - mLastInputGainUpdateTimestamp >= 100) {
                        mListener.onAudioInputDialogGainLevelChanged(AudioInputFragment.this);
                        mLastInputGainUpdateTimestamp = now;
                    }
                }
                mInputGainLevelText.setText(Integer.toString(mInputGainLevel));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {
                mListener.onAudioInputDialogGainLevelChanged(AudioInputFragment.this);
                mLastInputGainUpdateTimestamp = System.currentTimeMillis();
            }
        });

        mInputTwistLevelBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekbar, int level, boolean fromUser) {

                if (fromUser) {
                    long now = System.currentTimeMillis();
                    mInputTwistLevel = level + mInputTwistMin;
                    // Limit update rate to 100ms.
                    if (now - mLastInputTwistUpdateTimestamp >= 100) {
                        mListener.onAudioInputDialogTwistLevelChanged(AudioInputFragment.this);
                        mLastInputTwistUpdateTimestamp = now;
                    }
                }
                String db = String.format(getString(R.string.input_twist_db), mInputTwistLevel);
                mInputTwistLevelText.setText(db);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {
                mListener.onAudioInputDialogTwistLevelChanged(AudioInputFragment.this);
                mLastInputTwistUpdateTimestamp = System.currentTimeMillis();
            }
        });

        Button inputAdjustButton = view.findViewById(R.id.inputAdjustButton);
        inputAdjustButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onAudioInputAdjustButtonChanged(AudioInputFragment.this);
            }
        });
        inputAdjustButton.getBackground().setAlpha(64);

        ImageButton inputGainHelpButton = view.findViewById(R.id.inputGainHelpButton);
        inputGainHelpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.label_input_gain_icon)
                        .setMessage(R.string.label_input_gain_hint)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // pass
                            }
                        }).show();
            }
        });
        inputGainHelpButton.getBackground().setAlpha(64);

        ImageButton inputTwistHelpButton = view.findViewById(R.id.inputTwistHelpButton);
        inputTwistHelpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.label_input_twist_icon)
                        .setMessage(R.string.label_input_twist_hint)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // pass
                            }
                        }).show();
            }
        });
        inputTwistHelpButton.getBackground().setAlpha(64);

        return view;
	}	
    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
    	
    	
        if(D) Log.d(TAG, "+++ ON CREATE VIEW +++");

        if (getShowsDialog()) {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            View view = inflater.inflate(R.layout.audio_input_fragment, null);
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
        View dialogView = inflater.inflate(R.layout.audio_input_fragment, null);
        builder.setView(dialogView)
        // Add action buttons
               .setTitle(R.string.audio_input_title).setIcon(R.drawable.ic_voice_search)
               .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mListener != null) {
                    	   mListener.onAudioInputDialogClose(AudioInputFragment.this);
                       }
                   }
               });

        configureDialogView(dialogView);
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
    public void onAttach(@androidx.annotation.NonNull Context context) {
        super.onAttach(context);

        if(D) Log.d(TAG, "++ ON ATTACH ++");

        try {
            // Instantiate the listener so we can send events to the host
            mListener = (Listener) context;
            mContext = context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
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

    	if (isAdded()) {
    		mInputVolumeLevel.setLevel(level);
    	}
    }

    public void setInputGain(int level)
    {
        if(D) Log.d(TAG, "setInputGain = " + level);

        mHasInputControls = true;
        mInputGainLevel = level;

        if (isAdded()) {
            if(D) Log.d(TAG, "** setInputGain = " + level);
            mInputGainLevelBar.setProgress(level + mInputGainMin);
            mInputGainLevelText.setText(String.format(Locale.getDefault(),"%d",level));
        }
    }

    public void setInputGainMin(int level)
    {
        if(D) Log.d(TAG, "setInputGainMin = " + level);

        mHasInputControls = true;
        mInputGainMin = level;
        if (isAdded()) {
            mInputGainLevelBar.setMax(mInputGainMax - mInputGainMin);
            mInputGainLevelBar.setProgress(mInputGainLevel - mInputGainMin);
            mInputGainMinText.setText(String.format(Locale.getDefault(),"%d",level));
        }
    }

    public void setInputGainMax(int level)
    {
        if(D) Log.d(TAG, "setInputGainMax = " + level);

        mHasInputControls = true;
        mInputGainMax = level;
        if (isAdded()) {
            mInputGainLevelBar.setMax(mInputGainMax - mInputGainMin);
            mInputGainLevelBar.setProgress(mInputGainLevel - mInputGainMin);
            mInputGainMaxText.setText(String.format(Locale.getDefault(),"%d",level));
        }
    }

    public int getInputGain()
    {
        return mInputGainLevel;
    }

    public void setInputTwist(int level)
    {
        if(D) Log.d(TAG, "setInputTwist = " + level);

        mHasInputControls = true;
        mInputTwistLevel = level;

        if (isAdded()) {
            mInputTwistLevelBar.setProgress(mInputTwistLevel - mInputTwistMin);
            mInputTwistLevelText.setText(String.format(getString(R.string.input_twist_db), level));
        }
    }

    public void setInputTwistMin(int level)
    {
        if(D) Log.d(TAG, "setInputTwistMin = " + level);

        mHasInputControls = true;
        mInputTwistMin = level;

        if (isAdded()) {
            mInputTwistLevelBar.setMax(mInputTwistMax - mInputTwistMin);
            mInputTwistLevelBar.setProgress(mInputTwistLevel - mInputTwistMin);
            mInputTwistMinText.setText(String.format(getString(R.string.input_twist_db), level));
        }
    }

    public void setInputTwistMax(int level)
    {
        if(D) Log.d(TAG, "setInputTwistMax = " + level);

        mHasInputControls = true;
        mInputTwistMax = level;

        if (isAdded()) {
            mInputTwistLevelBar.setMax(mInputTwistMax - mInputTwistMin);
            mInputTwistLevelBar.setProgress(mInputTwistLevel - mInputTwistMin);
            mInputTwistMaxText.setText(String.format(getString(R.string.input_twist_db), level));
        }
    }

    public int getInputTwist()
    {
        return mInputTwistLevel;
    }
}
