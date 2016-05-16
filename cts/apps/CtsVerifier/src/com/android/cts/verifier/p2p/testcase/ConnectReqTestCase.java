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
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import com.android.cts.verifier.R;

import java.lang.reflect.Method;

/**
 * A base class for connection request test.
 */
public abstract class ConnectReqTestCase extends ReqTestCase {

    protected P2pBroadcastReceiverTest mReceiverTest;

    public ConnectReqTestCase(Context context) {
        super(context);
    }

    /**
     * Set up the test case.
     */
    protected void setUp() {
        super.setUp();
        mReceiverTest = new P2pBroadcastReceiverTest(mContext);
        mReceiverTest.init(mChannel);

        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(mP2pMgr, mChannel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        // Disconnect from wifi to avoid channel conflict
        mWifiMgr.disconnect();
    }

    /**
     * Tear down the test case.
     */
    protected void tearDown() {
        super.tearDown();
        if (mReceiverTest != null) {
            mReceiverTest.close();
        }
        if (mP2pMgr != null) {
            mP2pMgr.cancelConnect(mChannel, null);
            mP2pMgr.removeGroup(mChannel, null);
        }
    }

    /**
     * Tries to connect the target devices.
     * @param isJoin if true, try to join the group. otherwise, try go negotiation.
     * @param wpsConfig wps configuration method.
     * @return true if succeeded.
     * @throws InterruptedException
     */
    protected boolean connectTest(boolean isJoin, int wpsConfig) throws InterruptedException {
        notifyTestMsg(R.string.p2p_searching_target);

        /*
         * Search target device and check its capability.
         */
        ActionListenerTest actionListener = new ActionListenerTest();
        mP2pMgr.discoverPeers(mChannel, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_discover_peers_error);
            return false;
        }

        WifiP2pDevice dev = mReceiverTest.waitDeviceFound(mTargetAddress, TIMEOUT);
        if (dev == null) {
            mReason = mContext.getString(R.string.p2p_target_not_found_error);
            return false;
        }

        if (!isJoin && dev.isGroupOwner()) {
            // target device should be p2p device.
            mReason = mContext.getString(R.string.p2p_target_invalid_role_error);
            return false;
        } else if (isJoin && !dev.isGroupOwner()) {
            //target device should be group owner.
            mReason = mContext.getString(R.string.p2p_target_invalid_role_error2);
            return false;
        }

        if (wpsConfig == WpsInfo.PBC) {
            notifyTestMsg(R.string.p2p_connecting_with_pbc);
        } else {
            notifyTestMsg(R.string.p2p_connecting_with_pin);
        }

        /*
         * Try to connect the target device.
         */
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = dev.deviceAddress;
        config.wps.setup = wpsConfig;
        mP2pMgr.connect(mChannel, config, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_connect_error);
            return false;
        }

        /*
         * Check if the connection broadcast is received.
         */
        WifiP2pInfo p2pInfo = mReceiverTest.waitConnectionNotice(TIMEOUT_FOR_USER_ACTION);
        if (p2pInfo == null) {
            mReason = mContext.getString(R.string.p2p_connection_error);
            return false;
        }

        /*
         * Wait until peer gets marked conencted.
         */
        notifyTestMsg(R.string.p2p_waiting_for_peer_to_connect);
        if (mReceiverTest.waitPeerConnected(mTargetAddress, TIMEOUT) != true) {
            mReason = mContext.getString(R.string.p2p_connection_error);
            return false;
        }

        /*
         * Remove the p2p group manualy.
         */
        mP2pMgr.removeGroup(mChannel, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_remove_group_error);
            return false;
        }

        notifyTestMsg(R.string.p2p_waiting_for_peer_to_disconnect);

        /*
         * Check if p2p disconnection broadcast is received
         */
        p2pInfo = mReceiverTest.waitDisconnectionNotice(TIMEOUT);
        if (p2pInfo == null) {
            mReason = mContext.getString(R.string.p2p_connection_error);
            return false;
        }

        /* Wait until peer gets marked disconnected */

        if (mReceiverTest.waitPeerDisconnected(mTargetAddress, TIMEOUT) != true) {
            mReason = mContext.getString(R.string.p2p_detect_disconnection_error);
            return false;
        }

        return true;
    }
}
