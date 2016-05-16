package com.android.phone;

import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

/**
 * {@link ListPreference} for CLIR (Calling Line Identification Restriction).
 * Right now this is used for "Caller ID" setting.
 */
public class CLIRListPreference extends ListPreference {
    private static final String LOG_TAG = "CLIRListPreference";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private final MyHandler mHandler = new MyHandler();
    private final Phone mPhone;
    private TimeConsumingPreferenceListener mTcpListener;

    int clirArray[];

    public CLIRListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPhone = PhoneGlobals.getPhone();
    }

    public CLIRListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        mPhone.setOutgoingCallerIdDisplay(findIndexOfValue(getValue()),
                mHandler.obtainMessage(MyHandler.MESSAGE_SET_CLIR));
        if (mTcpListener != null) {
            mTcpListener.onStarted(this, false);
        }
    }

    /* package */ void init(TimeConsumingPreferenceListener listener, boolean skipReading) {
        mTcpListener = listener;
        if (!skipReading) {
            mPhone.getOutgoingCallerIdDisplay(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CLIR,
                    MyHandler.MESSAGE_GET_CLIR, MyHandler.MESSAGE_GET_CLIR));
            if (mTcpListener != null) {
                mTcpListener.onStarted(this, true);
            }
        }
    }

    /* package */ void handleGetCLIRResult(int tmpClirArray[]) {
        clirArray = tmpClirArray;
        final boolean enabled =
                tmpClirArray[1] == 1 || tmpClirArray[1] == 3 || tmpClirArray[1] == 4;
        setEnabled(enabled);

        // set the value of the preference based upon the clirArgs.
        int value = CommandsInterface.CLIR_DEFAULT;
        switch (tmpClirArray[1]) {
            case 1: // Permanently provisioned
            case 3: // Temporary presentation disallowed
            case 4: // Temporary presentation allowed
                switch (tmpClirArray[0]) {
                    case 1: // CLIR invoked
                        value = CommandsInterface.CLIR_INVOCATION;
                        break;
                    case 2: // CLIR suppressed
                        value = CommandsInterface.CLIR_SUPPRESSION;
                        break;
                    case 0: // Network default
                    default:
                        value = CommandsInterface.CLIR_DEFAULT;
                        break;
                }
                break;
            case 0: // Not Provisioned
            case 2: // Unknown (network error, etc)
            default:
                value = CommandsInterface.CLIR_DEFAULT;
                break;
        }
        setValueIndex(value);

        // set the string summary to reflect the value
        int summary = R.string.sum_default_caller_id;
        switch (value) {
            case CommandsInterface.CLIR_SUPPRESSION:
                summary = R.string.sum_show_caller_id;
                break;
            case CommandsInterface.CLIR_INVOCATION:
                summary = R.string.sum_hide_caller_id;
                break;
            case CommandsInterface.CLIR_DEFAULT:
                summary = R.string.sum_default_caller_id;
                break;
        }
        setSummary(summary);
    }

    private class MyHandler extends Handler {
        static final int MESSAGE_GET_CLIR = 0;
        static final int MESSAGE_SET_CLIR = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_CLIR:
                    handleGetCLIRResponse(msg);
                    break;
                case MESSAGE_SET_CLIR:
                    handleSetCLIRResponse(msg);
                    break;
            }
        }

        private void handleGetCLIRResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (msg.arg2 == MESSAGE_SET_CLIR) {
                mTcpListener.onFinished(CLIRListPreference.this, false);
            } else {
                mTcpListener.onFinished(CLIRListPreference.this, true);
            }
            clirArray = null;
            if (ar.exception != null) {
                if (DBG) Log.d(LOG_TAG, "handleGetCLIRResponse: ar.exception="+ar.exception);
                mTcpListener.onException(CLIRListPreference.this, (CommandException) ar.exception);
            } else if (ar.userObj instanceof Throwable) {
                mTcpListener.onError(CLIRListPreference.this, RESPONSE_ERROR);
            } else {
                int clirArray[] = (int[]) ar.result;
                if (clirArray.length != 2) {
                    mTcpListener.onError(CLIRListPreference.this, RESPONSE_ERROR);
                } else {
                    if (DBG) {
                        Log.d(LOG_TAG, "handleGetCLIRResponse: CLIR successfully queried,"
                                + " clirArray[0]=" + clirArray[0]
                                + ", clirArray[1]=" + clirArray[1]);
                    }
                    handleGetCLIRResult(clirArray);
                }
            }
        }

        private void handleSetCLIRResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                if (DBG) Log.d(LOG_TAG, "handleSetCallWaitingResponse: ar.exception="+ar.exception);
                //setEnabled(false);
            }
            if (DBG) Log.d(LOG_TAG, "handleSetCallWaitingResponse: re get");

            mPhone.getOutgoingCallerIdDisplay(obtainMessage(MESSAGE_GET_CLIR,
                    MESSAGE_SET_CLIR, MESSAGE_SET_CLIR, ar.exception));
        }
    }
}
