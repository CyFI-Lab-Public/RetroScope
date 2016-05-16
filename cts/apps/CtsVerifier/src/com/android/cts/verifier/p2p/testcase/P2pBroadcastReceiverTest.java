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
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

/**
 * The utility class for testing wifi direct broadcast intent.
 */
public class P2pBroadcastReceiverTest extends BroadcastReceiver
    implements PeerListListener {

    private static final String TAG = "P2pBroadcastReceiverTest";

    private final IntentFilter mIntentFilter = new IntentFilter();
    private Context mContext;
    private WifiP2pManager mP2pMgr;
    private Channel mChannel;

    private WifiP2pDeviceList mPeers;
    private WifiP2pInfo mP2pInfo;

    public P2pBroadcastReceiverTest(Context context) {
        this.mContext = context;
        mP2pMgr = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    }

    public void init(Channel c) {
        mChannel = c;
        mContext.registerReceiver(this, mIntentFilter);
    }

    public synchronized void close() {
        mContext.unregisterReceiver(this);
        notifyAll();
    }

    /**
     * Wait until the specified target device is found.
     *
     * @param targetAddr the p2p device address of the target device.
     * @param msec timeout value.
     * @return the found device. Return null if the target device is not found
     * within the given timeout.
     * @throws InterruptedException
     */
    public synchronized WifiP2pDevice waitDeviceFound(String targetAddr, long msec)
            throws InterruptedException {

        Timeout t = new Timeout(msec);
        while (!t.isTimeout()) {
            if (mPeers != null) {
                for (WifiP2pDevice dev: mPeers.getDeviceList()) {
                    if (dev.deviceAddress.equals(targetAddr)) {
                        return dev;
                    }
                }
            }
            wait(t.getRemainTime());
        }
        Log.e(TAG, "Appropriate WIFI_P2P_PEERS_CHANGED_ACTION didn't occur");

        return null;
    }

    /**
     * Wait until a p2p group is created.
     *
     * @param msec timeout value
     * @return a established p2p information. Return null if a connection is NOT
     * established within the given timeout.
     * @throws InterruptedException
     */
    public synchronized WifiP2pInfo waitConnectionNotice(long msec) throws InterruptedException {
        Timeout t = new Timeout(msec);
        while (!t.isTimeout()) {
            if (mP2pInfo != null && mP2pInfo.groupFormed) {
                return mP2pInfo;
            }
            wait(t.getRemainTime());
        }
        Log.e(TAG, "Appropriate WIFI_P2P_CONNECTION_CHANGED_ACTION didn't occur");

        return null;
    }

    /**
     * Wait until a station gets connected.
     * @param msec
     * @return
     * @throws InterruptedException
     */
    public synchronized boolean waitPeerConnected(String targetAddr, long msec)
            throws InterruptedException {

        Timeout t = new Timeout(msec);
        while (!t.isTimeout()) {
            if (mPeers != null) {
                for (WifiP2pDevice dev: mPeers.getDeviceList()) {
                    if (dev.deviceAddress.equals(targetAddr)) {
                        if (dev.status == WifiP2pDevice.CONNECTED) {
                            return true;
                        }
                    }
                }
            }
            wait(t.getRemainTime());
        }
        Log.e(TAG, "Appropriate WIFI_P2P_PEERS_CHANGED_ACTION didn't occur");

        return false;
    }

    /**
     * Wait until a station gets disconnected.
     * @param msec
     * @return
     * @throws InterruptedException
     */
    public synchronized boolean waitPeerDisconnected(String targetAddr, long msec)
            throws InterruptedException {

        Timeout t = new Timeout(msec);

        boolean devicePresent;

        while (!t.isTimeout()) {
            devicePresent = false;
            if (mPeers != null) {
                for (WifiP2pDevice dev: mPeers.getDeviceList()) {
                    if (dev.deviceAddress.equals(targetAddr)) {
                        if (dev.status != WifiP2pDevice.CONNECTED) {
                            return true;
                        }
                        devicePresent = true;
                    }
                }
            }
            if (!devicePresent) return true;
            wait(t.getRemainTime());
        }
        Log.e(TAG, "Appropriate WIFI_P2P_PEERS_CHANGED_ACTION didn't occur");

        return false;
    }

    /**
     * Wait until a connection is disconnected.
     *
     * @param msec timeout value.
     * @return a disconnected p2p information. Return null if a connection is NOT disconnected
     * within the given timeout.
     * @throws InterruptedException
     */
    public synchronized WifiP2pInfo waitDisconnectionNotice(long msec) throws InterruptedException {
        Timeout t = new Timeout(msec);
        while (!t.isTimeout()) {
            if (mP2pInfo != null && !mP2pInfo.groupFormed) {
                return mP2pInfo;
            }
            wait(t.getRemainTime());
        }
        Log.e(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION didn't occur");

        return null;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
            mP2pMgr.requestPeers(mChannel, this);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
            synchronized(this) {
                mP2pInfo = (WifiP2pInfo)intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                notifyAll();
            }
        }
    }

    @Override
    public synchronized void onPeersAvailable(WifiP2pDeviceList peers) {
        Log.d(TAG, "onPeersAvailable()");
        mPeers = peers;
        notifyAll();
    }
}
