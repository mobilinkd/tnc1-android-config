package com.mobilinkd.tncconfig;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class KissFragment extends DialogFragment {
	
    // Debugging
    private static final String TAG = "KissFragment";
    private static final boolean D = true;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     * */
    public interface Listener {
        public void onKissDialogUpdate(KissFragment dialog);
        public void onKissDialogResume(KissFragment dialog);
    }

    abstract private class NumberPickerListener implements NumberPickerFragment.Listener {

		abstract public void onDialogPositiveClick(NumberPickerFragment dialog);

		abstract public void onDialogNegativeClick(NumberPickerFragment dialog);
    }
    
    private View mDialogView = null;
    
	private int mTxDelay = 0;
	private int mPersistence = 0;
	private int mSlotTime = 0;
	private int mTxTail = 2;
	private boolean mDuplex = false;
	
    private TextView mTxDelayText;
    private TextView mPersistenceText;
    private TextView mSlotTimeText;
    private TextView mTxTailText;
    private CheckedTextView mDuplexView;
    
    private LinearLayout mTxDelayView;
    private LinearLayout mPersistenceView;
    private LinearLayout mSlotTimeView;
	
	private Listener mListener = null;

	private View configureDialogView(View view) {
		
        mTxDelayText = (TextView) view.findViewById(R.id.txDelayText);
        mPersistenceText = (TextView) view.findViewById(R.id.persistenceText);
        mSlotTimeText = (TextView) view.findViewById(R.id.slotTimeText);
        mTxTailText = (TextView) view.findViewById(R.id.txTailText);
        mDuplexView = (CheckedTextView) view.findViewById(R.id.duplexCheckBox);

        mTxDelayView = (LinearLayout) view.findViewById(R.id.txDelayView);
        mPersistenceView = (LinearLayout) view.findViewById(R.id.persistenceView);
        mSlotTimeView = (LinearLayout) view.findViewById(R.id.slotTimeView);

        mTxDelayView.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View dialog) {
        		NumberPickerFragment numberPicker = new NumberPickerFragment()
        		.setActivity(getActivity())
        		.setTitle(getString(R.string.label_tx_delay))
        		.setMin(0)
        		.setMax(255)
        		.setValue(mTxDelay)
                .setListener(new NumberPickerListener() {
                	@Override
                    public void onDialogPositiveClick(NumberPickerFragment dialog) {
                       mTxDelay = dialog.getValue();
                       mTxDelayText.setText(Integer.toString(mTxDelay));
                       if (mListener != null) {
                    	   mListener.onKissDialogUpdate(KissFragment.this);
                       }
                    }
                	@Override
                	public void onDialogNegativeClick(NumberPickerFragment dialog) {
                		// pass
                	}
                });
        		numberPicker.show(getChildFragmentManager(), "NumberPickerFragment");
        	}
        });
        
        mPersistenceView.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View dialog) {
        		NumberPickerFragment numberPicker = new NumberPickerFragment()
        		.setActivity(getActivity())
        		.setTitle(getString(R.string.label_persistence))
        		.setMin(0)
        		.setMax(255)
        		.setValue(mPersistence)
                .setListener(new NumberPickerListener() {
                	@Override
                    public void onDialogPositiveClick(NumberPickerFragment dialog) {
                		mPersistence = dialog.getValue();
                        mPersistenceText.setText(Integer.toString(mPersistence));
                        if (mListener != null) {
                     	   mListener.onKissDialogUpdate(KissFragment.this);
                        }
                    }
                	@Override
                	public void onDialogNegativeClick(NumberPickerFragment dialog) {
                		// pass
                	}
                });
        		numberPicker.show(getChildFragmentManager(), "NumberPickerFragment");
        	}
        });
        
        mSlotTimeView.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View dialog) {
        		NumberPickerFragment numberPicker = new NumberPickerFragment()
        		.setActivity(getActivity())
        		.setTitle(getString(R.string.label_slot_time))
        		.setMin(0)
        		.setMax(255)
        		.setValue(mSlotTime)
                .setListener(new NumberPickerListener() {
                	@Override
                    public void onDialogPositiveClick(NumberPickerFragment dialog) {
                		mSlotTime = dialog.getValue();
                        mSlotTimeText.setText(Integer.toString(mSlotTime));
                        if (mListener != null) {
                     	   mListener.onKissDialogUpdate(KissFragment.this);
                        }
                    }
                	@Override
                	public void onDialogNegativeClick(NumberPickerFragment dialog) {
                		// pass
                	}
                });
        		numberPicker.show(getChildFragmentManager(), "NumberPickerFragment");
        	}
        });
        
        mTxDelayText.setText(Integer.toString(mTxDelay));
        mPersistenceText.setText(Integer.toString(mPersistence));
        mSlotTimeText.setText(Integer.toString(mSlotTime));
        mTxTailText.setText(Integer.toString(mTxTail));
        mDuplexView.setChecked(mDuplex);
        
        mDuplexView.setEnabled(true);
        mDuplexView.setClickable(true);
        mDuplexView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
            	((CheckedTextView) view).toggle();
                mDuplex = ((CheckedTextView) view).isChecked();
                Log.i(TAG, "mDuplex changed: " + mDuplex);
                if (mListener != null) {
             	   mListener.onKissDialogUpdate(KissFragment.this);
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
            View view = getActivity().getLayoutInflater().inflate(R.layout.kiss_fragment, null);    
            return configureDialogView(view);
        }
    }

    
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
        mDialogView = inflater.inflate(R.layout.kiss_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(R.string.kiss_parameters_title)
               .setIcon(R.drawable.perm_group_network)
               .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mListener != null) {
                    	   mListener.onKissDialogUpdate(KissFragment.this);
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
    public void onAttach(Context context) {
        super.onAttach(context);

        if(D) Log.d(TAG, "++ ON ATTACH ++");

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (Listener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement KissParametersFragment.Listener");
        }
    }

    @Override
    public void onPause() {
    	super.onPause();
    	
		mListener.onKissDialogUpdate(KissFragment.this);
        if(D) Log.d(TAG, "++ ON PAUSE ++");
    }

    @Override
    public void onResume() {
    	super.onResume();
    	
		mListener.onKissDialogResume(KissFragment.this);
        if(D) Log.d(TAG, "++ ON RESUME ++");
    }

    public void setTxDelay(int value) {
    	mTxDelay = value;
    	if (mTxDelayText != null) {
    		mTxDelayText.setText(Integer.toString(mTxDelay));
    	}
    }
    
    public int getTxDelay() {
    	return mTxDelay;
    }

    void setPersistence(int value) {
    	mPersistence = value;
    	if (mPersistenceText != null) {
    		mPersistenceText.setText(Integer.toString(mPersistence));
    	}
    }
    
    public int getPersistence() {
    	return mPersistence;
    }

    public void setSlotTime(int value) {
    	mSlotTime = value;
    	if (mSlotTimeText != null) {
    		mSlotTimeText.setText(Integer.toString(mSlotTime));
    	}
    }

    public int getSlotTime() {
    	return mSlotTime;
    }
    
    public void setTxTail(int value) {
    	mTxTail = value;;
    	if (mTxTailText != null) {
    		mTxTailText.setText(Integer.toString(mTxTail));
    	}
   }
    
    public int getTxTail() {
    	return mTxTail;
    }
    
    public void setDuplex(boolean value) {
    	mDuplex = value;
    }
    
    public boolean getDuplex() {
    	return mDuplex;
    }
}
