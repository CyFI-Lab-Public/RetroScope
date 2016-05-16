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
package com.android.cts.verifier.p2p.testcase;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import com.android.cts.verifier.R;

/**
 * A test case defines the fixture to run p2p tests. To define a test case <br>
 *
 * 1) implement a subclass of TestCase<br>
 * 2) define instance variables that store the state of the fixture<br>
 * 3) initialize the fixture state by overriding setUp if needed. At default, p2p channel
 *  and p2p manager is initialized.<br>
 * 4) implement test case by overriding executeTest. executeTest must return the message
 * id to be shown in result text view.
 * 5) clean-up after a test by overriding tearDown if needed. At default, all services
 *  and requests are cleared.<br>
 */
public abstract class TestCase {

    /*
     * The test case id.
     */
    public static final String EXTRA_TEST_NAME =
            "com.android.cts.verifier.p2p.testcase.EXTRA_TEST_NAME";

    protected static final int TIMEOUT = 25000;
    protected static final int TIMEOUT_FOR_USER_ACTION = 60000;
    protected static final int SUCCESS = 0;

    protected Context mContext;
    protected String mReason;

    protected WifiP2pManager mP2pMgr;
    protected WifiManager mWifiMgr;
    protected Channel mChannel;
    // this is used for multi-client test
    protected Channel mSubChannel;

    private Thread mThread;
    private TestCaseListener mListener;

    /**
     * Constructor
     * @param context Activity context cannot be null.
     * @param handler Must be the handler of GUI thread. cannot be null.
     * @param textView The result message to be shown. cannot be null.
     * @param listener The test listener. can be null.
     */
    public TestCase(Context context) {
        mContext = context;
    }

    /**
     * Start test case.
     *
     * Test case is executed in another thread.
     * After the test, the result message is shown in text view.
     */
    public void start(TestCaseListener listener) {
        mListener = listener;

        stop();
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mListener.onTestStarted();
                try {
                    setUp();
                } catch(Exception e) {
                    mListener.onTestFailed(mContext.getString(R.string.p2p_setup_error));
                    return;
                }

                try {
                    if (executeTest()) {
                        mListener.onTestSuccess();
                    } else {
                        mListener.onTestFailed(getReason());
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    mListener.onTestFailed(
                            mContext.getString(R.string.p2p_unexpected_error));
                } finally {
                    tearDown();
                }
            }});
        mThread.start();
    }

    /**
     * Stop test case.
     */
    public void stop() {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }

    /**
     * Return test name.
     * @return test name.
     */
    abstract public String getTestName();

    /**
     * Return test id. It must be unique.
     * @return test id.
     */
    public String getTestId() {
        return this.getClass().getName();
    }

    /**
     * Execute test case.
     * @return the message id to be shown in text view.
     * @throws InterruptedException
     */
    abstract protected boolean executeTest() throws InterruptedException;

    /**
     * Set up the test case.
     */
    protected void setUp() {
        mP2pMgr = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mChannel = mP2pMgr.initialize(mContext, mContext.getMainLooper(), null);
        mSubChannel = mP2pMgr.initialize(mContext, mContext.getMainLooper(), null);
    }

    /**
     * Tear down the test case.
     */
    protected void tearDown() {
        mP2pMgr.clearLocalServices(mChannel, null);
        mP2pMgr.clearServiceRequests(mChannel, null);
        mP2pMgr.clearLocalServices(mSubChannel, null);
        mP2pMgr.clearServiceRequests(mSubChannel, null);
    }

    /**
     * Notify a message to the application.
     * @param id
     */
    protected void notifyTestMsg(int id) {
        mListener.onTestMsgReceived(mContext.getString(id));
    }

    /**
     * Get reason for the failure.
     * @return
     */
    private String getReason() {
       if (mReason == null) {
           return mContext.getString(R.string.p2p_unexpected_error);
       }
       return mReason;
    }

    public static interface TestCaseListener {

        /**
         * This function is invoked when the test case starts.
         */
        public void onTestStarted();

        /**
         * This function is invoked when the test notify a message to application.
         * @param msg
         */
        public void onTestMsgReceived(String msg);

        /**
         * This function is invoked when the test is success.
         */
        public void onTestSuccess();

        /**
         * This function is invoked when the test is failed.
         * @param reason
         */
        public void onTestFailed(String reason);
    }
}
