/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.app.NotificationManager;
import android.os.Bundle;
import com.android.internal.telephony.test.SimulatedRadioControl;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * A simple activity that presents you with a UI for faking incoming phone operations.
 */
public class FakePhoneActivity extends Activity {
    private static final String TAG = "FakePhoneActivity";

    private Button mPlaceCall;
    private EditText mPhoneNumber;
    SimulatedRadioControl mRadioControl;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.fake_phone_activity);

        mPlaceCall = (Button) findViewById(R.id.placeCall);
        mPlaceCall.setOnClickListener(new ButtonListener());

        mPhoneNumber = (EditText) findViewById(R.id.phoneNumber);
        mPhoneNumber.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        mPlaceCall.requestFocus();
                    }
                });

        mRadioControl = PhoneGlobals.getPhone().getSimulatedRadioControl();

        Log.i(TAG, "- PhoneApp.getInstance(): " + PhoneGlobals.getInstance());
        Log.i(TAG, "- PhoneApp.getPhone(): " + PhoneGlobals.getPhone());
        Log.i(TAG, "- mRadioControl: " + mRadioControl);
    }

    private class ButtonListener implements OnClickListener {
        public void onClick(View v) {
            if (mRadioControl == null) {
                Log.e("Phone", "SimulatedRadioControl not available, abort!");
                NotificationManager nm =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Toast.makeText(FakePhoneActivity.this, "null mRadioControl!",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            
            mRadioControl.triggerRing(mPhoneNumber.getText().toString());
        }
    }
}
