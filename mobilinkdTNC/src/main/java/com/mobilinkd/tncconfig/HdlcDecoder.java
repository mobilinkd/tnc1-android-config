package com.mobilinkd.tncconfig;

import android.util.Log;

public class HdlcDecoder {

    private static final String TAG = "HdlcDecoder";

    static final int BUFFER_SIZE = 330;

    static final byte FEND = (byte) 192;
    static final byte FESC = (byte) 219;
    static final byte TFEND = (byte) 220;
    static final byte TFESC = (byte) 221;

    static final int STATE_WAIT_FEND = 0;
    static final int STATE_WAIT_HW = 1;
    static final int STATE_WAIT_ESC = 2;
    static final int STATE_WAIT_DATA = 3;

    public static final int TNC_INPUT_VOLUME = 4;
    public static final int TNC_OUTPUT_VOLUME = 12;
    public static final int TNC_GET_OUTPUT_TWIST = 27;
    public static final int TNC_GET_TXDELAY = 33;
    public static final int TNC_GET_PERSIST = 34;
    public static final int TNC_GET_SLOTTIME = 35;
    public static final int TNC_GET_TXTAIL = 36;
    public static final int TNC_GET_DUPLEX = 37;
    public static final int TNC_GET_SQUELCH_LEVEL = 14;
    public static final int TNC_GET_HW_VERSION = 41;
    public static final int TNC_GET_FW_VERSION = 40;
    public static final int TNC_GET_VERBOSITY = 17;
    public static final int TNC_GET_BATTERY_LEVEL = 6;
    public static final int TNC_GET_INPUT_ATTEN = 13;       // API 1.0
    public static final int TNC_GET_INPUT_GAIN = 13;        // API 2.0
    public static final int TNC_GET_INPUT_TWIST = 25;       // API 2.0
    public static final int TNC_GET_BT_CONN_TRACK = 70;
    public static final int TNC_GET_USB_POWER_ON = 74;
    public static final int TNC_GET_USB_POWER_OFF = 76;
    public static final int TNC_GET_PTT_CHANNEL = 80;
    public static final int TNC_GET_PASSALL = 82;               // API 2.1
    public static final int TNC_GET_MIN_OUTPUT_TWIST = 119;     // API 2.0
    public static final int TNC_GET_MAX_OUTPUT_TWIST = 120;     // API 2.0
    public static final int TNC_GET_MIN_INPUT_TWIST = 121;      // API 2.0
    public static final int TNC_GET_MAX_INPUT_TWIST = 122;      // API 2.0
    public static final int TNC_GET_API_VERSION = 123;
    public static final int TNC_GET_MIN_INPUT_GAIN = 124;       // API 2.0
    public static final int TNC_GET_MAX_INPUT_GAIN = 125;       // API 2.0
    public static final int TNC_GET_CAPABILITIES = 126;
    public static final int TNC_GET_DATETIME = 49;              // API 2.0
    public static final int TNC_GET_SERIAL_NUMBER = 47;         // API 2.0
    public static final int TNC_GET_MAC_ADDRESS = 48;           // API 2.0

    // API 2.1
    public static final int EXT_RANGE_1 = 0xC1;
    public static final int EXT_GET_MODEM_TYPE = 0x81;
    // public static final int EXT_SET_MODEM_TYPE = 0x82;
    public static final int EXT_GET_MODEM_TYPES = 0x83;

    boolean mAvailable = false;
    byte[] mBuffer = new byte[BUFFER_SIZE];
    int mPos = 0;
    int mState = STATE_WAIT_FEND;

    public HdlcDecoder() {

    }

    public boolean available() {return mAvailable;}

    public void process(byte c)
    {
        switch (mState) {
            case STATE_WAIT_FEND:
                if (c == FEND) {
                    mPos = 0;
                    mAvailable = false;
                    mState = STATE_WAIT_HW;
                }
                break;
            case STATE_WAIT_HW:
                if (c == FEND) break;
                if (c == (byte) 0x06) {
                    mState = STATE_WAIT_DATA;
                }
                else {
                    Log.e(TAG, "Invalid packet type received " + (int)c);
                    mState = STATE_WAIT_FEND;
                }
                break;
            case STATE_WAIT_ESC:
                switch (c) {
                    case TFESC:
                        mBuffer[mPos++] = FESC;
                        break;
                    case TFEND:
                        mBuffer[mPos++] = FEND;
                        break;
                    default:
                        mBuffer[mPos++] = c;
                        break;
                }
                mState = STATE_WAIT_DATA;
                break;
            case STATE_WAIT_DATA:
                switch (c) {
                    case FESC:
                        mState = STATE_WAIT_ESC;
                        break;
                    case FEND:
                        if (mPos > 1) mAvailable = true;
                        mState = STATE_WAIT_FEND;
                    default:
                        mBuffer[mPos++] = c;
                        break;
                }
        }
    }

    int getType() {return (int) mBuffer[0] & 0xff;}
    int getValue() {return (int) mBuffer[1] & 0xff;}
    int size() {return mPos - 1;}
    byte[] data()
    {
        byte[] result = new byte[mPos - 2];
        System.arraycopy(mBuffer, 1, result, 0, mPos - 2);
        return result;
    }
}
