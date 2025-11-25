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
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class InfoFragment extends DialogFragment {
	
    // Debugging
    private static final String TAG = "InfoFragment";
    private static final boolean D = true;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     * */
    public interface Listener {
        public void onInfoDialogUpdate(InfoFragment dialog);
        public void onInfoDialogResume(InfoFragment dialog);
    }

    private String mHwVersion;
    private String mFwVersion;
    private String mMacAddress;
    private String mSerialNumber;
    private String mDateTime;

    private TextView mHwVersionText;
    private TextView mFwVersionText;
    private TextView mMacAddressText;
    private TextView mSerialNumberText;
    private TextView mDateTimeText;

	private Listener mListener = null;

	private View configureDialogView(View view) {
		
        mHwVersionText = (TextView) view.findViewById(R.id.hwVersion);
        mFwVersionText = (TextView) view.findViewById(R.id.fwVersion);
        mMacAddressText = (TextView) view.findViewById(R.id.macAddress);
        mSerialNumberText = (TextView) view.findViewById(R.id.serialNumber);
        mDateTimeText = (TextView) view.findViewById(R.id.dateTime);

        mHwVersionText.setText(mHwVersion);
        mFwVersionText.setText(mFwVersion);
        mMacAddressText.setText(mMacAddress);
        mSerialNumberText.setText(mSerialNumber);
        mDateTimeText.setText(mDateTime);

		return view;
	}	

	private int from_bcd(byte value) {
        return ((value >> 4) * 10) + (value & 15);
    }

	private void updateDate(byte[] value) {
        Calendar date = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

        date.set(
            from_bcd(value[0]) + 2000,  // YEAR
            from_bcd(value[1]) - 1,   // MONTH
            from_bcd(value[2]),         // DAY
            from_bcd(value[4]),         // HOUR
            from_bcd(value[5]),         // MINUTE
            from_bcd(value[6]));        // SECOND

        date.set(Calendar.DAY_OF_WEEK, value[3] + 1);

        @SuppressLint("SimpleDateFormat") DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        format.setCalendar(date);
        mDateTime = format.format(date.getTime()) + " UTC";
    }
	
	@SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
    	
    	
        if(D) Log.d(TAG, "+++ ON CREATE VIEW +++");

        if (getShowsDialog()) {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.info_fragment, null);
            return configureDialogView(view);
        }
    }

    
	@androidx.annotation.NonNull
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
        View mDialogView = inflater.inflate(R.layout.info_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(R.string.device_information_title)
               .setIcon(R.drawable.ic_menu_info_details)
               .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       if (mListener != null) {
                    	   mListener.onInfoDialogUpdate(InfoFragment.this);
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
    public void onAttach(@androidx.annotation.NonNull Context context) {
        super.onAttach(context);

        if(D) Log.d(TAG, "++ ON ATTACH ++");

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (Listener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement InfoFragment.Listener");
        }
    }

    @Override
    public void onPause() {
    	super.onPause();
    	
		mListener.onInfoDialogUpdate(InfoFragment.this);
        if(D) Log.d(TAG, "++ ON PAUSE ++");
    }

    @Override
    public void onResume() {
    	super.onResume();
    	
		mListener.onInfoDialogResume(InfoFragment.this);
        if(D) Log.d(TAG, "++ ON RESUME ++");
    }

    public void setHardwareVersion(String value) {
    	mHwVersion = value;
    	if (mHwVersionText != null) {
            mHwVersionText.setText(mHwVersion);
    	}
    }
    
    public String getHardwareVersion() {
    	return mHwVersion;
    }

    void setFirmwareVersion(String value) {
        mFwVersion = value;
    	if (mFwVersionText != null) {
            mFwVersionText.setText(mFwVersion);
    	}
    }

    public String getFirmwareVersion() {
    	return mFwVersion;
    }

    void setMacAddress(String value) {
        mMacAddress = value;
        if (mMacAddressText != null) {
            mMacAddressText.setText(mMacAddress);
        }
    }

    public String getMacAddress() {
        return mMacAddress;
    }

    void setSerialNumber(String value) {
        mSerialNumber = value;
        if (mSerialNumberText != null) {
            mSerialNumberText.setText(mSerialNumber);
        }
    }

    public String getSerialNumber() {
        return mSerialNumber;
    }

    void setDateTime(byte[] value) {
        updateDate(value);
        if (mDateTimeText != null) {
            mDateTimeText.setText(mDateTime);
        }
    }

    public String getDateTime() {
        return mDateTime;
    }

}
