/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.example.imsframework;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;

import java.util.Arrays;

/**
 * Top-level Application class for the example IMS framework.
 */
public class ImsFrameworkApp extends Application {
    private static final String TAG = "ImsFrameworkApp";

    // Broadcast receiver for telephony intent broadcasts
    private final BroadcastReceiver mReceiver = new ImsFrameworkBroadcastReceiver();

    // Handler for ISIM authentication callback
    private final IsimAuthenticationHandler mHandler = new IsimAuthenticationHandler();

    private static final int EVENT_ISIM_AUTHENTICATION_DONE = 100;

    @Override
    public void onCreate() {
        // Register for telephony intent broadcasts
        Log.d(TAG, "onCreate(): registering for telephony state change broadcasts");
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }

    /**
     * Receiver for telephony broadcasts that the IMS framework cares about.
     */
    private class ImsFrameworkBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mReceiver received action " + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                Log.d(TAG, "mReceiver: ACTION_AIRPLANE_MODE_CHANGED");
                Log.d(TAG, "- state: " + intent.getBooleanExtra(Phone.STATE_KEY, false));
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                Log.d(TAG, "mReceiver: ACTION_ANY_DATA_CONNECTION_STATE_CHANGED");
                Log.d(TAG, "- apnName: " + intent.getStringExtra(Phone.DATA_APN_KEY));
                Log.d(TAG, "- apnType: " + intent.getStringExtra(Phone.DATA_APN_TYPE_KEY));
                Log.d(TAG, "- state: " + intent.getStringExtra(Phone.STATE_KEY));
                Log.d(TAG, "- reason: " + intent.getStringExtra(Phone.STATE_CHANGE_REASON_KEY));
                Log.d(TAG, "- network unavailable: " +
                        intent.getBooleanExtra(Phone.NETWORK_UNAVAILABLE_KEY, false));
                // demonstrate calling ISIM authentication when data state changes
                tryIsimAuthentication();
            } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                Log.d(TAG, "mReceiver: ACTION_SIM_STATE_CHANGED");
                Log.d(TAG, "- phoneName: " + intent.getStringExtra(Phone.PHONE_NAME_KEY));
                String state = intent.getStringExtra(IccCard.INTENT_KEY_ICC_STATE);
                Log.d(TAG, "- state: " + state);
                Log.d(TAG, "- reason: " + intent.getStringExtra(Phone.STATE_CHANGE_REASON_KEY));
                if (IccCard.INTENT_VALUE_ICC_LOADED.equals(state)) {
                    // all ISIM records are loaded, query them through TelephonyManager
                    handleSimRecordsLoaded();
                }
            } else if (action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)) {
                Log.d(TAG, "mReceiver: ACTION_RADIO_TECHNOLOGY_CHANGED");
                Log.d(TAG, "- phoneName: " + intent.getStringExtra(Phone.PHONE_NAME_KEY));
            } else if (action.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
                Log.d(TAG, "mReceiver: ACTION_SERVICE_STATE_CHANGED");
                ServiceState ss = ServiceState.newFromBundle(intent.getExtras());
                Log.d(TAG, "- ServiceState: " + ss);
            }
        }
    }

    void handleSimRecordsLoaded() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        Log.d(TAG, "ISIM IMPI: " + telephonyManager.getIsimImpi());
        Log.d(TAG, "ISIM Domain: " + telephonyManager.getIsimDomain());
        Log.d(TAG, "ISIM IMPU: " + Arrays.toString(telephonyManager.getIsimImpu()));
    }

    private final class IsimAuthenticationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                Log.d(TAG, "requestIsimAuthentication exception: " + ar.exception);
            } else {
                String response = (String) ar.result;
                Log.d(TAG, "requestIsimAuthentication response: " + response);
            }
        }
    }

    void tryIsimAuthentication() {
        Message response = mHandler.obtainMessage(EVENT_ISIM_AUTHENTICATION_DONE);
        // Note: this only works when running inside the phone process
        Phone phone = PhoneFactory.getDefaultPhone();
        if (phone != null) {
            phone.requestIsimAuthentication("DUMMY-BASE64-NONCE", response);
        }
    }
}
