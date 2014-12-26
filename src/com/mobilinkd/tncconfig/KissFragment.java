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
        public void onKissDialogClose(KissFragment dialog);
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

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        mDialogView = inflater.inflate(R.layout.kiss_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(R.string.kiss_parameters_label)
               .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mListener != null) {
                    	   mListener.onKissDialogClose(KissFragment.this);
                       }
                   }
               });

        mTxDelayText = (TextView) mDialogView.findViewById(R.id.txDelayText);
        mPersistenceText = (TextView) mDialogView.findViewById(R.id.persistenceText);
        mSlotTimeText = (TextView) mDialogView.findViewById(R.id.slotTimeText);
        mTxTailText = (TextView) mDialogView.findViewById(R.id.txTailText);
        mDuplexView = (CheckedTextView) mDialogView.findViewById(R.id.duplexCheckBox);

        mTxDelayView = (LinearLayout) mDialogView.findViewById(R.id.txDelayView);
        mPersistenceView = (LinearLayout) mDialogView.findViewById(R.id.persistenceView);
        mSlotTimeView = (LinearLayout) mDialogView.findViewById(R.id.slotTimeView);

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
                Log.e(TAG, "mDuplex changed: " + mDuplex);
            }
        });
        
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        return builder.create();
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
                    + " must implement KissParametersFragment.Listener");
        }
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
