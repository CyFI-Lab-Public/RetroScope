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

package android.net.wifi.cts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_DISABLED;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED;
import android.test.AndroidTestCase;

public class ConcurrencyTest extends AndroidTestCase {
    private class MySync {
        int expectedWifiState;
        int expectedP2pState;
    }

    private WifiManager mWifiManager;
    private MySync mMySync = new MySync();

    private static final String TAG = "WifiInfoTest";
    private static final int TIMEOUT_MSEC = 6000;
    private static final int WAIT_MSEC = 60;
    private static final int DURATION = 10000;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                synchronized (mMySync) {
                    mMySync.expectedWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                            WifiManager.WIFI_STATE_DISABLED);
                    mMySync.notify();
                }
            } else if(action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
                synchronized (mMySync) {
                    mMySync.expectedP2pState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,
                            WifiP2pManager.WIFI_P2P_STATE_DISABLED);
                    mMySync.notify();
                }
            }
        }
    };

    @Override
    protected void setUp() throws Exception {
       super.setUp();
       if (!WifiFeature.isWifiSupported(getContext()) &&
                !WifiFeature.isP2pSupported(getContext())) {
            // skip the test if WiFi && p2p are not supported
            return;
        }
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        mContext.registerReceiver(mReceiver, mIntentFilter);
        mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        assertNotNull(mWifiManager);
        if (mWifiManager.isWifiEnabled()) {
            assertTrue(mWifiManager.setWifiEnabled(false));
            Thread.sleep(DURATION);
        }
        assertTrue(!mWifiManager.isWifiEnabled());
        mMySync.expectedWifiState = WifiManager.WIFI_STATE_DISABLED;
        mMySync.expectedP2pState = WifiP2pManager.WIFI_P2P_STATE_DISABLED;
    }

    @Override
    protected void tearDown() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext()) &&
                !WifiFeature.isP2pSupported(getContext())) {
            // skip the test if WiFi and p2p are not supported
            super.tearDown();
            return;
        }
        mContext.unregisterReceiver(mReceiver);

        if (mWifiManager.isWifiEnabled()) {
            assertTrue(mWifiManager.setWifiEnabled(false));
            Thread.sleep(DURATION);
        }
        super.tearDown();
    }

    private void waitForBroadcasts() {
        synchronized (mMySync) {
            long timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
            while (System.currentTimeMillis() < timeout
                    && (mMySync.expectedWifiState != WifiManager.WIFI_STATE_ENABLED ||
                    mMySync.expectedP2pState != WifiP2pManager.WIFI_P2P_STATE_ENABLED)) {
                try {
                    mMySync.wait(WAIT_MSEC);
                } catch (InterruptedException e) { }
            }
        }
    }

    public void testConcurrency() {
        // Cannot support p2p alone
        if (!WifiFeature.isWifiSupported(getContext())) {
            assertTrue(!WifiFeature.isP2pSupported(getContext()));
            return;
        }

        if (!WifiFeature.isP2pSupported(getContext())) {
            // skip the test if p2p is not supported
            return;
        }

        // Enable wifi
        assertTrue(mWifiManager.setWifiEnabled(true));

        waitForBroadcasts();

        assertTrue(mMySync.expectedWifiState == WifiManager.WIFI_STATE_ENABLED);
        assertTrue(mMySync.expectedP2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
    }

}
