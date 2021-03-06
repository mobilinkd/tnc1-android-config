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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModemFragment extends DialogFragment {
	
    // Debugging
    private static final String TAG = "ModemFragment";
    private static final boolean D = true;

    public ModemFragment() {
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     * */
    public interface Listener {
        public void onModemDialogResume(ModemFragment dialog);
        public void onModemDialogUpdate(ModemFragment dialog);
    }
	
    private View mDialogView = null;

    private boolean mHasDcd = false;
	private boolean mDcd = false;
    private boolean mHasConnTrack = false;
	private boolean mConnTrack = false;
	private boolean mHasVerbose = false;
	private boolean mVerbose = false;

	private boolean mHasModemType = false;
	private int mModemType = 1;
	private int[] mSupportedModemTypes = null;

	private boolean mHasPassall = false;
	private boolean mPassall = false;

	private class ModemType {
	    public String name;
	    public int code;
    }

	private static Map<Integer, Integer> ModemTypes;
    static {
        ModemTypes = new HashMap<>();
        ModemTypes.put(1, R.string.modem_1200_afsk);
        ModemTypes.put(2, R.string.modem_300_afsk);
        ModemTypes.put(3, R.string.modem_9600_fsk);
    }

    private CheckedTextView mDcdView;
    private CheckedTextView mConnTrackView;
    private CheckedTextView mVerboseView;

    private LinearLayout mModemLayout;
    private Spinner mModemSpinner;

    private LinearLayout mPassallLayout;
    private CheckedTextView mPassallView;

	private Listener mListener = null;


    // from https://stackoverflow.com/a/14640612
    private int getIndex(Spinner spinner, String itemValue)
    {
        for (int i=0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(itemValue)) {
                return i;
            }
        }

        return 0;
    }
    
    private int getModemTypeNumber(String name)
    {
        for (Map.Entry<Integer, Integer> entry : ModemTypes.entrySet()) {
            if (getString(entry.getValue()) == name) return entry.getKey();
        }

        return 1;
    }
    
    private View configureDialogView(View view) {

	    mModemLayout = (LinearLayout)  view.findViewById(R.id.modemLayout);
	    mModemSpinner = (Spinner) view.findViewById(R.id.modemSpinner);

	    mPassallLayout = (LinearLayout) view.findViewById(R.id.passallLayout);
	    mPassallView = (CheckedTextView) view.findViewById(R.id.passallCheckBox);
		
        mDcdView = (CheckedTextView) view.findViewById(R.id.dcdCheckBox);
        mConnTrackView = (CheckedTextView) view.findViewById(R.id.connTrackCheckBox);
        mVerboseView = (CheckedTextView) view.findViewById(R.id.verboseCheckBox);

        if (mSupportedModemTypes != null) {
            String[] items = new String[mSupportedModemTypes.length];
            for (int i = 0; i != mSupportedModemTypes.length; i++) {
                items[i] = getString(ModemTypes.get(mSupportedModemTypes[i]));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    getContext(), android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mModemSpinner.setAdapter(adapter);
            mModemLayout.setVisibility(View.VISIBLE);
        }

        if (mHasModemType) {
            mModemLayout.setVisibility(View.VISIBLE);
            mModemSpinner.setSelection(getIndex(mModemSpinner, getString(ModemTypes.get(mModemType))));
        } else {
            mModemLayout.setVisibility(View.GONE);
        }

        if (mHasPassall) {
            mPassallLayout.setVisibility(View.VISIBLE);
            mPassallView.setChecked(mPassall);
        } else {
            mPassallLayout.setVisibility(View.GONE);
        }

        mDcdView.setChecked(mDcd);
        mDcdView.setEnabled(mHasDcd);
        mDcdView.setClickable(mHasDcd);

        mConnTrackView.setChecked(mConnTrack);
        mConnTrackView.setEnabled(mHasConnTrack);
        mConnTrackView.setClickable(mHasConnTrack);

        mVerboseView.setChecked(mVerbose);
        mVerboseView.setEnabled(mHasVerbose);
        mVerboseView.setClickable(mHasVerbose);

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

        mPassallView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                ((CheckedTextView) view).toggle();
                mPassall = ((CheckedTextView) view).isChecked();
                Log.i(TAG, "mPassall changed: " + mPassall);
                if (mListener != null) {
                    mListener.onModemDialogUpdate(ModemFragment.this);
                }
            }
        });

        mModemSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                mModemType = getModemTypeNumber(item.toString());
                Log.i(TAG, "mModemType changed: " + mModemType + " " + item.toString());
                if (mListener != null) {
                    mListener.onModemDialogUpdate(ModemFragment.this);
                }
            }
            public void onNothingSelected(AdapterView<?> parent) {
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

        // Get the layout inflater
        FragmentActivity activity = getActivity();
        if (activity == null) {
            throw new AssertionError();
        }
        LayoutInflater inflater = activity.getLayoutInflater();

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.FullscreenTheme));

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        mDialogView = inflater.inflate(R.layout.modem_fragment, null);
        builder.setView(mDialogView)
        // Add action buttons
               .setTitle(R.string.modem_settings_title)
               .setIcon(R.drawable.perm_group_system_tools)
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
    public void onAttach(Context context) {
        super.onAttach(context);

        if(D) Log.d(TAG, "++ ON ATTACH ++");

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the listener so we can send events to the host
            mListener = (Listener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
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
	    mHasDcd = true;
    }
    
    public boolean getDcd() {
    	return mDcd;
    }

    public void setConnTrack(boolean value) {
    	mConnTrack = value;
    	mHasConnTrack = true;
    }

    public void setModemType(int value) {
        mModemType = value;
        mHasModemType = true;
    }

    public void setSupportedModemTypes(int[] supportedModemTypes) {
        mSupportedModemTypes = supportedModemTypes;

        if (isAdded()) {
            String[] items = new String[supportedModemTypes.length];
            for (int i = 0; i != supportedModemTypes.length; i++) {
                if (D) Log.d(TAG, "** mSupportedModemTypes = " + supportedModemTypes[i]);
                items[i] = getString(ModemTypes.get(supportedModemTypes[i]));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    getContext(), android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mModemSpinner.setAdapter(adapter);
            mModemLayout.setVisibility(View.VISIBLE);
        }
    }

    public void setPassall(boolean value) {
        mPassall = value;
        mHasPassall = true;
        if (isAdded()) {
            if(D) Log.d(TAG, "** setPassall = " + value);
            mPassallView.setChecked(value);
            mPassallLayout.setVisibility(View.VISIBLE);
        }
    }

    public boolean hasModemType() { return mHasModemType; }
    public int getModemType() { return mModemType; }

    public boolean hasPassall() { return mHasPassall; }
    public boolean getPassall() { return mPassall; }

    public boolean hasSupportedModemTypes() { return mSupportedModemTypes != null;}

    public boolean hasConnTrack() {
    	return mHasConnTrack;
    }
    
    public boolean getConnTrack() {
    	return mConnTrack;
    }

    public boolean hasVerbose() { return mHasVerbose; }

    public void setVerbose(boolean value) {
    	mVerbose = value;
    	mHasVerbose = true;
    }
    
    public boolean getVerbose() {
    	return mVerbose;
    }
}
