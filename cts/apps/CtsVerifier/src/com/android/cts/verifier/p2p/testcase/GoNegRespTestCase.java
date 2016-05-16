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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

/**
 * Test case for go negotiation response.
 *
 * The requester devices tries to connect this device.
 */
public class GoNegRespTestCase extends TestCase {

    private final IntentFilter mIntentFilter = new IntentFilter();
    private WifiP2pBroadcastReceiver mReceiver;

    public GoNegRespTestCase(Context context) {
        super(context);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mReceiver = new WifiP2pBroadcastReceiver();
    }

    @Override
    protected void setUp() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
        super.setUp();
    }

    @Override
    protected boolean executeTest() throws InterruptedException {

        mP2pMgr.discoverPeers(mChannel, null);

        // wait until p2p device is trying go negotiation.
        return true;
    }


    @Override
    protected void tearDown() {
        // wait until p2p device is trying go negotiation.
        synchronized(this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mP2pMgr != null) {
            mP2pMgr.cancelConnect(mChannel, null);
            mP2pMgr.removeGroup(mChannel, null);
        }
        mContext.unregisterReceiver(mReceiver);

        super.tearDown();
    }


    @Override
    public String getTestName() {
        return "Go negotiation responder test";
    }

    private class WifiP2pBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                synchronized(this) {
                    WifiP2pInfo p2pInfo = (WifiP2pInfo)intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                    if (p2pInfo.groupFormed && !p2pInfo.isGroupOwner) {
                        /*
                         * Remove p2p group for next test once your device became p2p client.
                         * In the case of GO, p2p group will be removed automatically because
                         * target device will cut the connection.
                         */
                        mP2pMgr.removeGroup(mChannel, null);
                    } else if (!p2pInfo.groupFormed) {
                        /*
                         * find again.
                         */
                        mP2pMgr.discoverPeers(mChannel, null);
                    }
                }
            }
        }
    }
}
