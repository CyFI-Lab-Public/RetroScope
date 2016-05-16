/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.phone.tests;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.android.phone.Constants;

/**
 * Test activity that mimics the behavior of 3rd party apps firing off
 * CALL and DIAL intents.
 */
public class CallDialTest extends Activity implements View.OnClickListener {
    private static final String LOG_TAG = "CallDialTest";

    // UI elements
    private TextView mLabel;
    private EditText mNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        log("onCreate: intent = " + intent);

        // Construct our basic UI:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_dial_test);

        mLabel = (TextView) findViewById(R.id.label1);

        mNumber = (EditText) findViewById(R.id.number);
        mNumber.setText("6505551234");  // Preload it with something useful

        ((Button) findViewById(R.id.callButton)).setOnClickListener(this);
        ((Button) findViewById(R.id.dialButton)).setOnClickListener(this);
        ((Button) findViewById(R.id.itelephonyCallButton)).setOnClickListener(this);
        ((Button) findViewById(R.id.itelephonyDialButton)).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        log("onResume()...");
        super.onResume();
    }

    @Override
    protected void onPause() {
        log("onPause()...");
        super.onPause();
    }

    // View.OnClickListener implementation
    @Override
    public void onClick(View view) {
        int id = view.getId();
        log("onClick(View " + view + ", id " + id + ")...");

        switch (id) {
            case R.id.callButton:
                log("onClick: CALL...");
                fireIntent(Intent.ACTION_CALL);
                break;
            case R.id.dialButton:
                log("onClick: DIAL...");
                fireIntent(Intent.ACTION_DIAL);
                break;
            case R.id.itelephonyCallButton:
                log("onClick: ITelephony.call()...");
                doITelephonyCall();
                break;
            case R.id.itelephonyDialButton:
                log("onClick: ITelephony.dial()...");
                doITelephonyDial();
                break;
            default:
                Log.wtf(LOG_TAG, "onClick: unexpected View: " + view);
                break;
        }
    }

    private void fireIntent(String action) {
        log("fireIntent(action = '" + action + "')...");

        // Get a phone number or SIP address from the EditText widget
        String number = mNumber.getText().toString();
        log("==> number: '" + number + "'");

        // Based on the number, fire off a CALL or DIAL intent:
        // - if it's a fully qualified URI (with scheme), use it directly
        // - if it looks like a SIP address, prepend sip:
        // - if it's just a number, prepend tel: automatically
        // - if it's blank, fire off a blank CALL or DIAL intent

        Uri uri = null;
        if (!TextUtils.isEmpty(number)) {
            if (number.contains(":")) {
                uri = Uri.parse(number);
            } else if (PhoneNumberUtils.isUriNumber(number)) {
                uri = Uri.fromParts(Constants.SCHEME_SIP, number, null);
            } else {
                uri = Uri.fromParts(Constants.SCHEME_TEL, number, null);
            }
        }
        log("==> uri: " + uri);

        Intent intent = new Intent(action, uri);
        log("==> intent: " + intent);

        try {
            startActivity(intent);
            Toast.makeText(this, "Starting activity...", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Log.w(LOG_TAG, "testCall: ActivityNotFoundException for intent: " + intent);
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.w(LOG_TAG, "testCall: Unexpected exception from startActivity(): " + e);
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void doITelephonyCall() {
        log("doITelephonyCall()...");

        // Get a phone number from the EditText widget
        String number = mNumber.getText().toString();
        log("==> number: '" + number + "'");

        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            log("- phone: " + phone);
            log("- calling call()...");
            phone.call(getPackageName(), number);
            log("  Done.");
        } catch (RemoteException ex) {
            Log.w(LOG_TAG, "RemoteException!", ex);
        }
    }

    private void doITelephonyDial() {
        log("doITelephonyDial()...");

        // Get a phone number from the EditText widget
        String number = mNumber.getText().toString();
        log("==> number: '" + number + "'");

        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            log("- phone: " + phone);
            log("- calling dial()...");
            phone.dial(number);
            log("  Done.");
        } catch (RemoteException ex) {
            Log.w(LOG_TAG, "RemoteException!", ex);
        }
    }

    private void log(String msg) {
        Log.i(LOG_TAG, msg);
    }
}
