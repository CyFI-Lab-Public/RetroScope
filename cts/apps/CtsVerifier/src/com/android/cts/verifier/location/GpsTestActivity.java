/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.verifier.location;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

interface PassFailLog {
    public void log(String s);
    public void pass();
    public void fail(String s);
}

/**
 * CTS Verifier case for verifying GPS.
 */
public class GpsTestActivity extends PassFailButtons.Activity implements PassFailLog {
    private LocationManager mLocationManager;
    private TextView mTextView;
    private ScrollView mScrollView;

    LocationVerifier mLocationVerifier;
    private int mTestNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocationManager = (LocationManager) getApplicationContext().getSystemService(
                Context.LOCATION_SERVICE);

        setContentView(R.layout.pass_fail_text);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.location_gps_test, R.string.location_gps_test_info, -1);
        mTextView = (TextView) findViewById(R.id.text);
        mTextView.setTypeface(Typeface.MONOSPACE);
        mTextView.setTextSize(15.0f);
        mScrollView = (ScrollView) findViewById(R.id.scroll);
    }

    @Override
    public void onResume() {
        super.onResume();

        mTextView.setText("");
        getPassButton().setEnabled(false);
        mTestNumber = 0;
        nextTest();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mLocationVerifier != null) {
            mLocationVerifier.stop();
        }
    }

    @Override
    public String getTestDetails() {
        return mTextView.getText().toString();
    }

    private void nextTest() {
        mTestNumber++;
        switch (mTestNumber) {
            case 1:
                // Test GPS with minTime = 0
                mLocationVerifier = new LocationVerifier(this, mLocationManager,
                        LocationManager.GPS_PROVIDER, 0, 8, false);
                mLocationVerifier.start();
                break;
            case 2:
                // Test GPS with minTime = 1s
                mLocationVerifier = new LocationVerifier(this, mLocationManager,
                        LocationManager.GPS_PROVIDER, 1 * 1000, 8, false);
                mLocationVerifier.start();
                break;
            case 3:
                // Test GPS with minTime = 5s
                mLocationVerifier = new LocationVerifier(this, mLocationManager,
                        LocationManager.GPS_PROVIDER, 5 * 1000, 8, false);
                mLocationVerifier.start();
                break;
            case 4:
                // Test GPS with minTime = 15s
                mLocationVerifier = new LocationVerifier(this, mLocationManager,
                        LocationManager.GPS_PROVIDER, 15 * 1000, 8, false);
                mLocationVerifier.start();
                break;
            case 5:
                log("All GPS tests complete", Color.GREEN);
                getPassButton().setEnabled(true);
                break;
        }
    }

    @Override
    public void log(String s) {
        mTextView.append(s);
        mTextView.append("\n");

        // Scroll to bottom
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void log(String s, int color) {
        int start = mTextView.getText().length();
        mTextView.append(s);
        mTextView.append("\n");
        int end = mTextView.getText().length();
        Spannable spanText = (Spannable) mTextView.getText();
        spanText.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }

    @Override
    public void fail(String s) {
        log(s, Color.RED);
        mLocationVerifier = null;
    }

    @Override
    public void pass() {
        log("OK!\n", Color.GREEN);
        try {
            Thread.sleep(2000);
            log("(Sleep 2 second) \n", Color.GREEN);
        } catch (InterruptedException e) {
            fail("unexpected InterruptedException when sleep");
        }
        mLocationVerifier = null;
        nextTest();
    }
}
