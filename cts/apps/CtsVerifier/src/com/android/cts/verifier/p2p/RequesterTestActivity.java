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

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.R.id;
import com.android.cts.verifier.p2p.testcase.ReqTestCase;
import com.android.cts.verifier.p2p.testcase.TestCase;
import com.android.cts.verifier.p2p.testcase.TestCase.TestCaseListener;

/**
 * A base class for requester test activity.
 *
 * This class provides the feature to search target device and show test results.
 * A requester test activity just have to extend this class and implement getTestCase().
 */
public abstract class RequesterTestActivity  extends PassFailButtons.Activity
    implements TestCaseListener {

    /*
     * Timeout for searching devices. The unit is millisecond
     */
    private final static int SEARCH_TARGET_TIMEOUT = 8000;

    /*
     * The target device address.
     * The service discovery request test needs the responder address.
     * The target device address is reused until test failed.
     */
    private static String sTargetAddr;

    /*
     * The test case to be executed.
     */
    private ReqTestCase mTestCase;

    /*
     * The text view to print the test result
     */
    private TextView mTextView;

    /*
     * The progress bar.
     */
    private ProgressBar mProgress;

    /*
     * GUI thread handler.
     */
    private Handler mHandler = new Handler();

    /*
     * Timer object. It's used for searching devices.
     */
    private Timer mTimer;

    /*
     * p2p manager
     */
    private WifiP2pManager mP2pMgr;
    private Channel mChannel;

    /**
     * Return the specified requester test case.
     *
     * @param context
     * @param testId test id.
     * @return requester test case
     */
    protected abstract ReqTestCase getTestCase(Context context, String testId);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.p2p_requester_main);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);

        mProgress = (ProgressBar) findViewById(R.id.p2p_progress);
        mProgress.setVisibility(ProgressBar.VISIBLE);
        mTextView = (TextView) findViewById(id.p2p_req_text);

        String testId = (String) getIntent().getSerializableExtra(
                TestCase.EXTRA_TEST_NAME);
        mTestCase = getTestCase(this, testId);
        setTitle(mTestCase.getTestName());

        // Initialize p2p manager.
        mP2pMgr = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mP2pMgr.initialize(this, getMainLooper(), null);

        // keep screen on while this activity is front view.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * If the target device is NOT set, search targets and show
         * the target device list on the dialog.
         * After the user selection, the specified test will be executed.
         */
        if (sTargetAddr == null) {
            searchTarget();
            return;
        }

        mTestCase.setTargetAddress(sTargetAddr);
        mTestCase.start(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        mTestCase.stop();
    }

    @Override
    public String getTestId() {
        return mTestCase.getTestId();
    }

    @Override
    public void onTestStarted() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgress.setVisibility(ProgressBar.VISIBLE);
            }
        });
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
    public void onTestFailed(final String reason) {
        // if test failed, forget target device address.
        sTargetAddr = null;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgress.setVisibility(ProgressBar.INVISIBLE);
                mTextView.setText(reason);
            }
        });
    }

    @Override
    public void onTestSuccess() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mProgress.setVisibility(ProgressBar.INVISIBLE);
                mTextView.setText(R.string.p2p_result_success);
                getPassButton().setEnabled(true);
            }
        });
    }

    /**
     * Search devices and show the found devices on the dialog.
     * After user selection, the specified test will be executed.
     */
    private void searchTarget() {
        // Discover peers.
        mP2pMgr.discoverPeers(mChannel, null);
        mTextView.setText(R.string.p2p_searching_target);
        mProgress.setVisibility(ProgressBar.VISIBLE);

        /*
         * Show the peer list dialog after searching devices for 8 seconds.
         */
        if (mTimer == null) {
            mTimer = new Timer(true);
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mP2pMgr.requestPeers(mChannel, new PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList _peers) {
                        final WifiP2pDeviceList peers = _peers;
                        /*
                         * Need to show dialog in GUI thread.
                         */
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mProgress.setVisibility(ProgressBar.INVISIBLE);
                                if (peers.getDeviceList().size() == 0) {
                                    mTextView.setText(
                                            R.string.p2p_target_not_found_error);
                                } else {
                                    showSelectTargetDialog(peers);
                                }
                            }
                        });
                    }
                });
            }
        }, SEARCH_TARGET_TIMEOUT);
    }

    /**
     * Show the found device list on the dialog.
     * The target device address selected by user is stored in {@link #mTargetAddr}.
     * After user selection, the specified test will be executed.
     * @param peers
     * @param testIndex
     */
    private void showSelectTargetDialog(WifiP2pDeviceList peers) {
        final Collection<WifiP2pDevice> peerList = peers.getDeviceList();
        final CharSequence[] items = new CharSequence[peerList.size()];
        int i=0;
        for (WifiP2pDevice dev: peerList) {
            items[i++] = dev.deviceName;
        }

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.p2p_search_target)
                .setItems(items, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int i=0;
                for (WifiP2pDevice dev: peerList) {
                    if (i == which) {
                        sTargetAddr = dev.deviceAddress;
                        mTestCase.setTargetAddress(sTargetAddr);
                        mTestCase.start(getTestCaseListener());
                        break;
                    }
                    i++;
                }
            }
        }).show();
    }

    private TestCaseListener getTestCaseListener() {
        return this;
    }
}
