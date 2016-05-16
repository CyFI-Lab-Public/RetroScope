/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

import java.util.List;

/**
 * Used to display a dialog from within the Telephony service when running an USSD code
 */
public class MMIDialogActivity extends Activity {
    private static final String TAG = MMIDialogActivity.class.getSimpleName();

    private Dialog mMMIDialog;

    private Handler mHandler;

    private CallManager mCM = PhoneGlobals.getInstance().getCallManager();
    private Phone mPhone = PhoneGlobals.getPhone();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case PhoneGlobals.MMI_COMPLETE:
                            onMMIComplete((MmiCode) ((AsyncResult) msg.obj).result);
                            break;
                        case PhoneGlobals.MMI_CANCEL:
                            onMMICancel();
                            break;
                    }
                }
        };
        mCM.registerForMmiComplete(mHandler, PhoneGlobals.MMI_COMPLETE, null);
        if (mCM.getState() == PhoneConstants.State.OFFHOOK) {
            Toast.makeText(this, R.string.incall_status_dialed_mmi, Toast.LENGTH_SHORT).show();
        }
        showMMIDialog();
    }

    private void showMMIDialog() {
        final List<? extends MmiCode> codes = mPhone.getPendingMmiCodes();
        if (codes.size() > 0) {
            final MmiCode mmiCode = codes.get(0);
            final Message message = Message.obtain(mHandler, PhoneGlobals.MMI_CANCEL);
            mMMIDialog = PhoneUtils.displayMMIInitiate(this, mmiCode, message, mMMIDialog);
        } else {
            finish();
        }
    }

    /**
     * Handles an MMI_COMPLETE event, which is triggered by telephony
     */
    private void onMMIComplete(MmiCode mmiCode) {
        // Check the code to see if the request is ready to
        // finish, this includes any MMI state that is not
        // PENDING.

        // if phone is a CDMA phone display feature code completed message
        int phoneType = mPhone.getPhoneType();
        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            PhoneUtils.displayMMIComplete(mPhone, this, mmiCode, null, null);
        } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
            if (mmiCode.getState() != MmiCode.State.PENDING) {
                Log.d(TAG, "Got MMI_COMPLETE, finishing dialog activity...");
                dismissDialogsAndFinish();
            }
        }
    }

    /**
     * Handles an MMI_CANCEL event, which is triggered by the button
     * (labeled either "OK" or "Cancel") on the "MMI Started" dialog.
     * @see PhoneUtils#cancelMmiCode(Phone)
     */
    private void onMMICancel() {
        Log.v(TAG, "onMMICancel()...");

        // First of all, cancel the outstanding MMI code (if possible.)
        PhoneUtils.cancelMmiCode(mPhone);

        // Regardless of whether the current MMI code was cancelable, the
        // PhoneApp will get an MMI_COMPLETE event very soon, which will
        // take us to the MMI Complete dialog (see
        // PhoneUtils.displayMMIComplete().)
        //
        // But until that event comes in, we *don't* want to stay here on
        // the in-call screen, since we'll be visible in a
        // partially-constructed state as soon as the "MMI Started" dialog
        // gets dismissed. So let's forcibly bail out right now.
        Log.d(TAG, "onMMICancel: finishing InCallScreen...");
        dismissDialogsAndFinish();
    }

    private void dismissDialogsAndFinish() {
        if (mMMIDialog != null) {
            mMMIDialog.dismiss();
        }
        if (mHandler != null) {
            mCM.unregisterForMmiComplete(mHandler);
        }
        finish();
    }
}
