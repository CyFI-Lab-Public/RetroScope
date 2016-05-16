package com.android.cts.verifier.nfc.hce;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

@TargetApi(19)
public abstract class HceService extends HostApduService {
    final static String TAG = "HceService";

    final static int STATE_IDLE = 0;
    final static int STATE_IN_PROGRESS = 1;
    final static int STATE_FAILED = 2;

    // Variables below only used on main thread
    String[] mCommandApdus = null;
    String[] mResponseApdus = null;
    int mApduIndex = 0;
    int mState = STATE_IDLE;
    long mStartTime;

    public void initialize(String[] commandApdus, String[] responseApdus) {
       mCommandApdus = commandApdus;
       mResponseApdus = responseApdus;
    }

    @Override
    public void onDeactivated(int arg0) {
        mApduIndex = 0;
        mState = STATE_IDLE;
    }

    public abstract ComponentName getComponent();

    public void onApduSequenceComplete() {
        Intent completionIntent = new Intent(HceUtils.ACTION_APDU_SEQUENCE_COMPLETE);
        completionIntent.putExtra(HceUtils.EXTRA_COMPONENT, getComponent());
        completionIntent.putExtra(HceUtils.EXTRA_DURATION,
                System.currentTimeMillis() - mStartTime);
        sendBroadcast(completionIntent);
    }

    public void onApduSequenceError() {
        Intent errorIntent = new Intent(HceUtils.ACTION_APDU_SEQUENCE_ERROR);
        sendBroadcast(errorIntent);
    }

    @Override
    public byte[] processCommandApdu(byte[] arg0, Bundle arg1) {
        if (mState == STATE_FAILED) {
            // Don't accept any more APDUs until deactivated
            return null;
        }

        if (mState == STATE_IDLE) {
            mState = STATE_IN_PROGRESS;
            mStartTime = System.currentTimeMillis();
        }

        if (mApduIndex >= mCommandApdus.length) {
            Log.d(TAG, "Ignoring command APDU; protocol complete.");
            // Ignore new APDUs after completion
            return null;
        } else {
            if (!Arrays.equals(HceUtils.hexStringToBytes(mCommandApdus[mApduIndex]), arg0)) {
                Log.d(TAG, "Unexpected command APDU: " + HceUtils.getHexBytes("", arg0));
                onApduSequenceError();
                return null;
            } else {
                // Send corresponding response APDU
                byte[] responseApdu = HceUtils.hexStringToBytes(mResponseApdus[mApduIndex]);
                mApduIndex++;
                if (mApduIndex == mCommandApdus.length) {
                    // Test passed
                    onApduSequenceComplete();
                }
                return responseApdu;
            }
        }
    }
}
