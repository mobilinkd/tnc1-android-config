package com.mobilinkd.tncconfig;

import net.simonvt.numberpicker.NumberPicker;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

public class NumberPickerFragment extends DialogFragment implements NumberPicker.OnValueChangeListener {

	// Debugging
    private static final String TAG = "NumberPickerFragment";
    private static final boolean D = true;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     * */
    public interface Listener {
        public void onDialogPositiveClick(NumberPickerFragment dialog);
        public void onDialogNegativeClick(NumberPickerFragment dialog);
    }
	
    private View mDialogView = null;
	private Listener mListener = null;

    private String mTitle = null;
    private Integer mMinValue = null;
    private Integer mMaxValue = null;
    private int mValue = 0;
    private NumberPicker mPicker;
	private Activity mActivity;
    
    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
    	mValue = newVal;
    }
    
    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        mDialogView = inflater.inflate(R.layout.number_picker_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(mTitle)
               .setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mListener != null) {
                    	   mListener.onDialogPositiveClick(NumberPickerFragment.this);
                       }
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            	   @Override
            	   public void onClick(DialogInterface dialog, int id) {
            		   if (mListener != null) {
            			   mListener.onDialogNegativeClick(NumberPickerFragment.this);
            		   }
            	   }
               });

        mPicker = (NumberPicker) mDialogView.findViewById(R.id.numberPicker1);
        mPicker.setOnValueChangedListener(this);
        mPicker.setMinValue(mMinValue);
        mPicker.setMaxValue(mMaxValue);
        mPicker.setValue(mValue);

        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        return builder.create();
    }
    
    NumberPickerFragment setActivity(Activity activity) {
    	mActivity = activity;
    	return this;
    }
    
    NumberPickerFragment setListener(Listener listener) {
    	mListener = listener;
    	return this;
    }
    
    NumberPickerFragment setValue(int value) {
    	mValue = value;
    	return this;
    }
    
    int getValue() {
    	return mValue;
    }
    
    NumberPickerFragment setMax(int value) {
    	mMaxValue = value;
    	return this;
    }
    
    NumberPickerFragment setMin(int value) {
    	mMinValue = value;
    	return this;
    }
    
    NumberPickerFragment setTitle(String value) {
    	mTitle = value;
    	return this;
    }
}
