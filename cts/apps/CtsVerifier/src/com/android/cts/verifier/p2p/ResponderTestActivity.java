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
package com.android.cts.verifier.p2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.R.id;
import com.android.cts.verifier.p2p.testcase.TestCase;
import com.android.cts.verifier.p2p.testcase.TestCase.TestCaseListener;

/**
 * A base class for responder test activity.
 *
 * This class provides the feature to show own device name and test results.
 * A responder test activity just have to extend this class and implement getTestCase()
 * and getReadyMsgId().
 */
public abstract class ResponderTestActivity extends PassFailButtons.Activity
    implements TestCaseListener {

    /*
     * Test case to be executed
     */
    private TestCase mTestCase;

    /*
     * Text view to show the result of the test.
     */
    private TextView mTextView;

    /*
     * Text view to show own device name.
     * It would be helpful to select this device on the other device.
     */
    private TextView mMyDeviceView;

    /*
     * BroadcastReceiver to obtain own device information.
     */
    private final P2pBroadcastReceiver mReceiver = new P2pBroadcastReceiver();
    private final IntentFilter mIntentFilter = new IntentFilter();
    private Handler mHandler = new Handler();

    /**
     * Constructor
     */
    public ResponderTestActivity() {
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    abstract protected TestCase getTestCase(Context context);

    abstract protected int getReadyMsgId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.p2p_responder_main);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);

        // Get UI component.
        mTextView = (TextView) findViewById(id.p2p_resp_text);
        mMyDeviceView = (TextView) findViewById(id.p2p_my_device);

        // Initialize test components.
        mTestCase = getTestCase(this);

        // keep screen on while this activity is front view.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTestCase.start(this);
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTestCase.stop();
        unregisterReceiver(mReceiver);
    }

    /**
     * Receive the WIFI_P2P_THIS_DEVICE_CHANGED_ACTION action and show the
     * this device information in the text view.
     */
    class P2pBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                final WifiP2pDevice myDevice = (WifiP2pDevice) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                if (myDevice != null) {
                    // need to show in the GUI thread.
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mMyDeviceView.setText("Device Name: " + myDevice.deviceName);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onTestStarted() {
    }

    public void onTestMsgReceived(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(msg);
            }
        });
    }

    @Override
    public void onTestSuccess() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(getReadyMsgId());
                getPassButton().setEnabled(true);
            }
        });
    }

    @Override
    public void onTestFailed(final String reason) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(reason);
            }
        });
    }
}
