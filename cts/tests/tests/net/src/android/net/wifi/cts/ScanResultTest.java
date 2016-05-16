/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.net.wifi.cts;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.test.AndroidTestCase;
import android.util.Log;

public class ScanResultTest extends AndroidTestCase {
    private static class MySync {
        int expectedState = STATE_NULL;
    }

    private WifiManager mWifiManager;
    private WifiLock mWifiLock;
    private static MySync mMySync;

    private static final int STATE_NULL = 0;
    private static final int STATE_WIFI_CHANGING = 1;
    private static final int STATE_WIFI_CHANGED = 2;
    private static final int STATE_START_SCAN = 3;
    private static final int STATE_SCAN_RESULTS_AVAILABLE = 4;

    private static final String TAG = "WifiInfoTest";
    private static final int TIMEOUT_MSEC = 6000;
    private static final int WAIT_MSEC = 60;
    private static final int ENABLE_WAIT_MSEC = 10000;
    private static final int SCAN_WAIT_MSEC = 10000;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                synchronized (mMySync) {
                    mMySync.expectedState = STATE_WIFI_CHANGED;
                    mMySync.notify();
                }
            } else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                synchronized (mMySync) {
                    mMySync.expectedState = STATE_SCAN_RESULTS_AVAILABLE;
                    mMySync.notify();
                }
            }
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        mMySync = new MySync();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);

        mContext.registerReceiver(mReceiver, mIntentFilter);
        mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        assertNotNull(mWifiManager);
        mWifiLock = mWifiManager.createWifiLock(TAG);
        mWifiLock.acquire();
        if (!mWifiManager.isWifiEnabled())
            setWifiEnabled(true);
        Thread.sleep(ENABLE_WAIT_MSEC);
        assertTrue(mWifiManager.isWifiEnabled());
        mMySync.expectedState = STATE_NULL;
    }

    @Override
    protected void tearDown() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            super.tearDown();
            return;
        }
        mWifiLock.release();
        mContext.unregisterReceiver(mReceiver);
        if (!mWifiManager.isWifiEnabled())
            setWifiEnabled(true);
        Thread.sleep(ENABLE_WAIT_MSEC);
        super.tearDown();
    }

    private void setWifiEnabled(boolean enable) throws Exception {
        synchronized (mMySync) {
            mMySync.expectedState = STATE_WIFI_CHANGING;
            assertTrue(mWifiManager.setWifiEnabled(enable));
            waitForBroadcast(TIMEOUT_MSEC, STATE_WIFI_CHANGED);
       }
    }

    private void waitForBroadcast(long timeout, int expectedState) throws Exception {
        long waitTime = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < waitTime
                && mMySync.expectedState != expectedState)
            mMySync.wait(WAIT_MSEC);
    }

    public void testScanResultProperties() {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        List<ScanResult> scanResults = mWifiManager.getScanResults();
        // this test case should in Wifi environment
        for (int i = 0; i < scanResults.size(); i++) {
            ScanResult mScanResult = scanResults.get(i);
            assertNotNull(mScanResult.toString());
        }
    }

    private void scanAndWait() throws Exception {
        synchronized (mMySync) {
            mMySync.expectedState = STATE_START_SCAN;
            mWifiManager.startScan();
            waitForBroadcast(SCAN_WAIT_MSEC, STATE_SCAN_RESULTS_AVAILABLE);
        }
   }

    public void testScanResultTimeStamp() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }

        long timestamp = 0;
        String BSSID = null;

        /* Multiple scans to ensure bssid is updated */
        scanAndWait();
        scanAndWait();
        scanAndWait();

        List<ScanResult> scanResults = mWifiManager.getScanResults();
        for (ScanResult result : scanResults) {
            BSSID = result.BSSID;
            timestamp = result.timestamp;
            assertTrue(timestamp != 0);
            break;
        }

        scanAndWait();
        scanAndWait();
        scanAndWait();

        scanResults = mWifiManager.getScanResults();
        for (ScanResult result : scanResults) {
            if (result.BSSID.equals(BSSID)) {
                long timeDiff = (result.timestamp - timestamp) / 1000;
                assertTrue (timeDiff > 0);
                assertTrue (timeDiff < 6 * SCAN_WAIT_MSEC);
            }
        }

    }

}
