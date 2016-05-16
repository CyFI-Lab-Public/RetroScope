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

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.ServiceState;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.google.common.base.Preconditions;

/**
 * Starts and displays status for Hands Free Activation (HFA).
 *
 * This class operates with Hands Free Activation apps.
 * It starts by broadcasting the intent com.android.action.START_HFA.
 * An HFA app will pick that up and start the HFA process.
 * If it fails it return ERROR_HFA Intent and upon success returns COMPLETE_HFA.
 *
 * If successful, we bounce the radio so that the service picks up the new number.
 * Once the radio is back on we callback the requestor.
 *
 * If there is an error, we do not bounce the radio but still callback with a failure.
 *
 * TODO(klp): We need system-only permissions for the HFA intents.
 */
public class HfaLogic {
    private static final String TAG = HfaLogic.class.getSimpleName();

    private static final String ACTION_START = "com.android.action.START_HFA";
    private static final String ACTION_ERROR = "com.android.action.ERROR_HFA";
    private static final String ACTION_CANCEL = "com.android.action.CANCEL_HFA";
    private static final String ACTION_COMPLETE = "com.android.action.COMPLETE_HFA";

    private static final int SERVICE_STATE_CHANGED = 1;

    public static final int NOT_WAITING = 0;
    public static final int WAITING_FOR_RADIO_OFF = 1;
    public static final int WAITING_FOR_RADIO_ON = 2;

    public static final int OTASP_UNKNOWN = 0;
    public static final int OTASP_USER_SKIPPED = 1;
    public static final int OTASP_SUCCESS = 2;
    public static final int OTASP_FAILURE = 3;

    private int mPhoneMonitorState = NOT_WAITING;
    private BroadcastReceiver mReceiver;
    private HfaLogicCallback mCallback;
    private PendingIntent mResponseIntent;
    private Context mContext;

    private static final int DEFAULT_RETRY_COUNT = 1;
    private int mRetryCount;

    public interface HfaLogicCallback {
        public void onSuccess();
        public void onError(String errorMsg);
    }

    public HfaLogic(Context context, HfaLogicCallback callback, PendingIntent intent) {
        mCallback = Preconditions.checkNotNull(callback);
        mContext = Preconditions.checkNotNull(context);
        mResponseIntent = intent;
    }

    public void start() {
        Log.i(TAG, "start:");
        mRetryCount = DEFAULT_RETRY_COUNT;
        startHfaIntentReceiver();
        startProvisioning();
    }

    private void startProvisioning() {
        Log.i(TAG, "startProvisioning:");
        sendHfaCommand(ACTION_START);
    }

    private void sendHfaCommand(String action) {
        Log.i(TAG, "sendHfaCommand: command=" + action);
        mContext.sendBroadcast(new Intent(action));
    }

    private void onHfaError(String errorMsg) {
        Log.i(TAG, "onHfaError: call mCallBack.onError errorMsg=" + errorMsg
                + " mRetryCount=" + mRetryCount);
        mRetryCount -= 1;
        if (mRetryCount >= 0) {
            Log.i(TAG, "onHfaError: retry");
            startProvisioning();
        } else {
            Log.i(TAG, "onHfaError: Declare OTASP_FAILURE");
            mRetryCount = 0;
            stopHfaIntentReceiver();
            sendFinalResponse(OTASP_FAILURE, errorMsg);
            mCallback.onError(errorMsg);
        }
    }

    private void onHfaSuccess() {
        Log.i(TAG, "onHfaSuccess: NOT bouncing radio call onTotalSuccess");
        stopHfaIntentReceiver();
        // bounceRadio();
        onTotalSuccess();
    }

    private void onTotalSuccess() {
        Log.i(TAG, "onTotalSuccess: call mCallBack.onSuccess");
        sendFinalResponse(OTASP_SUCCESS, null);
        mCallback.onSuccess();
    }

    private void bounceRadio() {
        final Phone phone = PhoneGlobals.getInstance().getPhone();
        phone.registerForServiceStateChanged(mHandler, SERVICE_STATE_CHANGED, null);

        mPhoneMonitorState = WAITING_FOR_RADIO_OFF;
        phone.setRadioPower(false);
        onServiceStateChange(phone.getServiceState());
    }

    private void onServiceStateChange(ServiceState state) {
        final boolean radioIsOff = state.getVoiceRegState() == ServiceState.STATE_POWER_OFF;
        final Phone phone = PhoneGlobals.getInstance().getPhone();

        Log.i(TAG, "Radio is on: " + !radioIsOff);

        if (mPhoneMonitorState == WAITING_FOR_RADIO_OFF) {
            if (radioIsOff) {
                mPhoneMonitorState = WAITING_FOR_RADIO_ON;
                phone.setRadioPower(true);
            }
        } else if (mPhoneMonitorState == WAITING_FOR_RADIO_ON) {
            if (!radioIsOff) {
                mPhoneMonitorState = NOT_WAITING;
                phone.unregisterForServiceStateChanged(mHandler);

                onTotalSuccess();
            }
        }
    }

    private void startHfaIntentReceiver() {
        final IntentFilter filter = new IntentFilter(ACTION_COMPLETE);
        filter.addAction(ACTION_ERROR);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(ACTION_ERROR)) {
                    onHfaError(intent.getStringExtra("errorCode"));
                } else if (action.equals(ACTION_COMPLETE)) {
                    Log.i(TAG, "Hfa Successful");
                    onHfaSuccess();
                }
            }
        };

        mContext.registerReceiver(mReceiver, filter);
    }

    private void stopHfaIntentReceiver() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private void sendFinalResponse(int responseCode, String errorCode) {
        if (mResponseIntent != null) {
            final Intent extraStuff = new Intent();
            extraStuff.putExtra(OtaUtils.EXTRA_OTASP_RESULT_CODE, responseCode);

            if (responseCode == OTASP_FAILURE && errorCode != null) {
                extraStuff.putExtra(OtaUtils.EXTRA_OTASP_ERROR_CODE, errorCode);
            }

            try {
                Log.i(TAG, "Sending OTASP confirmation with result code: "
                        + responseCode);
                mResponseIntent.send(mContext, 0 /* resultCode (not used) */, extraStuff);
            } catch (CanceledException e) {
                Log.e(TAG, "Pending Intent canceled");
            }
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SERVICE_STATE_CHANGED:
                    ServiceState state = (ServiceState) ((AsyncResult) msg.obj).result;
                    onServiceStateChange(state);
                    break;
                default:
                    break;
            }
        }
    };

}
