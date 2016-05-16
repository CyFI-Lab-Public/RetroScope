/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.Status;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.TxPacketCountListener;
import android.net.wifi.WifiManager.WifiLock;
import android.test.AndroidTestCase;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class WifiManagerTest extends AndroidTestCase {
    private static class MySync {
        int expectedState = STATE_NULL;
    }

    private WifiManager mWifiManager;
    private WifiLock mWifiLock;
    private static MySync mMySync;
    private List<ScanResult> mScanResult = null;
    private NetworkInfo mNetworkInfo;

    // Please refer to WifiManager
    private static final int MIN_RSSI = -100;
    private static final int MAX_RSSI = -55;

    private static final int STATE_NULL = 0;
    private static final int STATE_WIFI_CHANGING = 1;
    private static final int STATE_WIFI_ENABLED = 2;
    private static final int STATE_WIFI_DISABLED = 3;
    private static final int STATE_SCANNING = 4;
    private static final int STATE_SCAN_RESULTS_AVAILABLE = 5;

    private static final String TAG = "WifiManagerTest";
    private static final String SSID1 = "\"WifiManagerTest\"";
    private static final String SSID2 = "\"WifiManagerTestModified\"";
    private static final int TIMEOUT_MSEC = 6000;
    private static final int WAIT_MSEC = 60;
    private static final int DURATION = 10000;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                synchronized (mMySync) {
                    if (mWifiManager.getScanResults() != null) {
                        mScanResult = mWifiManager.getScanResults();
                        mMySync.expectedState = STATE_SCAN_RESULTS_AVAILABLE;
                        mScanResult = mWifiManager.getScanResults();
                        mMySync.notifyAll();
                    }
                }
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int newState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN);
                synchronized (mMySync) {
                    if (newState == WifiManager.WIFI_STATE_ENABLED) {
                        Log.d(TAG, "*** New WiFi state is ENABLED ***");
                        mMySync.expectedState = STATE_WIFI_ENABLED;
                        mMySync.notifyAll();
                    } else if (newState == WifiManager.WIFI_STATE_DISABLED) {
                        Log.d(TAG, "*** New WiFi state is DISABLED ***");
                        mMySync.expectedState = STATE_WIFI_DISABLED;
                        mMySync.notifyAll();
                    }
                }
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                synchronized (mMySync) {
                    mNetworkInfo =
                            (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (mNetworkInfo.getState() == NetworkInfo.State.CONNECTED)
                        mMySync.notifyAll();
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
        Thread.sleep(DURATION);
        assertTrue(mWifiManager.isWifiEnabled());
        synchronized (mMySync) {
            mMySync.expectedState = STATE_NULL;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            super.tearDown();
            return;
        }
        if (!mWifiManager.isWifiEnabled())
            setWifiEnabled(true);
        mWifiLock.release();
        mContext.unregisterReceiver(mReceiver);
        Thread.sleep(DURATION);
        super.tearDown();
    }

    private void setWifiEnabled(boolean enable) throws Exception {
        synchronized (mMySync) {
            assertTrue(mWifiManager.setWifiEnabled(enable));
            if (mWifiManager.isWifiEnabled() != enable) {
                mMySync.expectedState = STATE_WIFI_CHANGING;
                long timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
                int expectedState = (enable ? STATE_WIFI_ENABLED : STATE_WIFI_DISABLED);
                while (System.currentTimeMillis() < timeout
                        && mMySync.expectedState != expectedState)
                    mMySync.wait(WAIT_MSEC);
            }
        }
    }

    private void startScan() throws Exception {
        synchronized (mMySync) {
            mMySync.expectedState = STATE_SCANNING;
            assertTrue(mWifiManager.startScan());
            long timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
            while (System.currentTimeMillis() < timeout && mMySync.expectedState == STATE_SCANNING)
                mMySync.wait(WAIT_MSEC);
        }
    }

    private void connectWifi() throws Exception {
        synchronized (mMySync) {
            if (mNetworkInfo.getState() == NetworkInfo.State.CONNECTED) return;
            assertTrue(mWifiManager.reconnect());
            long timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
            while (System.currentTimeMillis() < timeout
                    && mNetworkInfo.getState() != NetworkInfo.State.CONNECTED)
                mMySync.wait(WAIT_MSEC);
            assertTrue(mNetworkInfo.getState() == NetworkInfo.State.CONNECTED);
        }
    }

    private boolean existSSID(String ssid) {
        for (final WifiConfiguration w : mWifiManager.getConfiguredNetworks()) {
            if (w.SSID.equals(ssid))
                return true;
        }
        return false;
    }

    private int findConfiguredNetworks(String SSID, List<WifiConfiguration> networks) {
        for (final WifiConfiguration w : networks) {
            if (w.SSID.equals(SSID))
                return networks.indexOf(w);
        }
        return -1;
    }

    private void assertDisableOthers(WifiConfiguration wifiConfiguration, boolean disableOthers) {
        for (WifiConfiguration w : mWifiManager.getConfiguredNetworks()) {
            if ((!w.SSID.equals(wifiConfiguration.SSID)) && w.status != Status.CURRENT) {
                if (disableOthers)
                    assertEquals(Status.DISABLED, w.status);
            }
        }
    }

    /**
     * test point of wifiManager actions:
     * 1.reconnect
     * 2.reassociate
     * 3.disconnect
     * 4.pingSupplicant
     * 5.satrtScan
     */
    public void testWifiManagerActions() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        assertTrue(mWifiManager.reconnect());
        assertTrue(mWifiManager.reassociate());
        assertTrue(mWifiManager.disconnect());
        assertTrue(mWifiManager.pingSupplicant());
        startScan();
        setWifiEnabled(false);
        Thread.sleep(DURATION);
        assertTrue(mWifiManager.pingSupplicant() == mWifiManager.isScanAlwaysAvailable());
        final String TAG = "Test";
        assertNotNull(mWifiManager.createWifiLock(TAG));
        assertNotNull(mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG));
    }

    /**
     * test point of wifiManager properties:
     * 1.enable properties
     * 2.DhcpInfo properties
     * 3.wifi state
     * 4.ConnectionInfo
     */
    public void testWifiManagerProperties() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        setWifiEnabled(true);
        assertTrue(mWifiManager.isWifiEnabled());
        assertNotNull(mWifiManager.getDhcpInfo());
        assertEquals(WifiManager.WIFI_STATE_ENABLED, mWifiManager.getWifiState());
        mWifiManager.getConnectionInfo();
        setWifiEnabled(false);
        assertFalse(mWifiManager.isWifiEnabled());
    }

    /**
     * test point of wifiManager NetWork:
     * 1.add NetWork
     * 2.update NetWork
     * 3.remove NetWork
     * 4.enable NetWork
     * 5.disable NetWork
     * 6.configured Networks
     * 7.save configure;
     */
    public void testWifiManagerNetWork() throws Exception {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        // store the list of enabled networks, so they can be re-enabled after test completes
        Set<String> enabledSsids = getEnabledNetworks(mWifiManager.getConfiguredNetworks());
        try {
            WifiConfiguration wifiConfiguration;
            // add a WifiConfig
            final int notExist = -1;
            List<WifiConfiguration> wifiConfiguredNetworks = mWifiManager.getConfiguredNetworks();
            int pos = findConfiguredNetworks(SSID1, wifiConfiguredNetworks);
            if (notExist != pos) {
                wifiConfiguration = wifiConfiguredNetworks.get(pos);
                mWifiManager.removeNetwork(wifiConfiguration.networkId);
            }
            pos = findConfiguredNetworks(SSID1, wifiConfiguredNetworks);
            assertEquals(notExist, pos);
            final int size = wifiConfiguredNetworks.size();

            wifiConfiguration = new WifiConfiguration();
            wifiConfiguration.SSID = SSID1;
            int netId = mWifiManager.addNetwork(wifiConfiguration);
            assertTrue(existSSID(SSID1));

            wifiConfiguredNetworks = mWifiManager.getConfiguredNetworks();
            assertEquals(size + 1, wifiConfiguredNetworks.size());
            pos = findConfiguredNetworks(SSID1, wifiConfiguredNetworks);
            assertTrue(notExist != pos);

            // Enable & disable network
            boolean disableOthers = false;
            assertTrue(mWifiManager.enableNetwork(netId, disableOthers));
            wifiConfiguration = mWifiManager.getConfiguredNetworks().get(pos);
            assertDisableOthers(wifiConfiguration, disableOthers);
            assertEquals(Status.ENABLED, wifiConfiguration.status);
            disableOthers = true;

            assertTrue(mWifiManager.enableNetwork(netId, disableOthers));
            wifiConfiguration = mWifiManager.getConfiguredNetworks().get(pos);
            assertDisableOthers(wifiConfiguration, disableOthers);

            assertTrue(mWifiManager.disableNetwork(netId));
            wifiConfiguration = mWifiManager.getConfiguredNetworks().get(pos);
            assertEquals(Status.DISABLED, wifiConfiguration.status);

            // Update a WifiConfig
            wifiConfiguration = wifiConfiguredNetworks.get(pos);
            wifiConfiguration.SSID = SSID2;
            netId = mWifiManager.updateNetwork(wifiConfiguration);
            assertFalse(existSSID(SSID1));
            assertTrue(existSSID(SSID2));

            // Remove a WifiConfig
            assertTrue(mWifiManager.removeNetwork(netId));
            assertFalse(mWifiManager.removeNetwork(notExist));
            assertFalse(existSSID(SSID1));
            assertFalse(existSSID(SSID2));

            assertTrue(mWifiManager.saveConfiguration());
        } finally {
            reEnableNetworks(enabledSsids, mWifiManager.getConfiguredNetworks());
            mWifiManager.saveConfiguration();
        }
    }

    private Set<String> getEnabledNetworks(List<WifiConfiguration> configuredNetworks) {
        Set<String> ssids = new HashSet<String>();
        for (WifiConfiguration wifiConfig : configuredNetworks) {
            if (Status.ENABLED == wifiConfig.status || Status.CURRENT == wifiConfig.status) {
                ssids.add(wifiConfig.SSID);
                Log.i(TAG, String.format("remembering enabled network %s", wifiConfig.SSID));
            }
        }
        return ssids;
    }

    private void reEnableNetworks(Set<String> enabledSsids,
            List<WifiConfiguration> configuredNetworks) {
        for (WifiConfiguration wifiConfig : configuredNetworks) {
            if (enabledSsids.contains(wifiConfig.SSID)) {
                mWifiManager.enableNetwork(wifiConfig.networkId, false);
                Log.i(TAG, String.format("re-enabling network %s", wifiConfig.SSID));
            }
        }
    }

    public void testSignal() {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        final int numLevels = 9;
        int expectLevel = 0;
        assertEquals(expectLevel, WifiManager.calculateSignalLevel(MIN_RSSI, numLevels));
        assertEquals(numLevels - 1, WifiManager.calculateSignalLevel(MAX_RSSI, numLevels));
        expectLevel = 4;
        assertEquals(expectLevel, WifiManager.calculateSignalLevel((MIN_RSSI + MAX_RSSI) / 2,
                numLevels));
        int rssiA = 4;
        int rssiB = 5;
        assertTrue(WifiManager.compareSignalLevel(rssiA, rssiB) < 0);
        rssiB = 4;
        assertTrue(WifiManager.compareSignalLevel(rssiA, rssiB) == 0);
        rssiA = 5;
        rssiB = 4;
        assertTrue(WifiManager.compareSignalLevel(rssiA, rssiB) > 0);
    }

    private int getTxPacketCount() throws Exception {
        final AtomicInteger ret = new AtomicInteger(-1);

        mWifiManager.getTxPacketCount(new TxPacketCountListener() {
            @Override
            public void onSuccess(int count) {
                ret.set(count);
            }
            @Override
            public void onFailure(int reason) {
                ret.set(0);
            }
        });

        long timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
        while (ret.get() < 0 && System.currentTimeMillis() < timeout)
            Thread.sleep(WAIT_MSEC);
        assertTrue(ret.get() >= 0);
        return ret.get();
    }

    /**
     * The new WiFi watchdog requires kernel/driver to export some packet loss
     * counters. This CTS tests whether those counters are correctly exported.
     * To pass this CTS test, a connected WiFi link is required.
     */
    public void testWifiWatchdog() throws Exception {
        // Make sure WiFi is enabled
        if (!mWifiManager.isWifiEnabled()) {
            setWifiEnabled(true);
            Thread.sleep(DURATION);
        }
        assertTrue(mWifiManager.isWifiEnabled());

        int i = 0;
        for (; i < 15; i++) {
            // Wait for a WiFi connection
            connectWifi();

            // Read TX packet counter
            int txcount1 = getTxPacketCount();

            // Do some network operations
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://www.google.com/");
                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(TIMEOUT_MSEC);
                connection.setReadTimeout(TIMEOUT_MSEC);
                connection.setUseCaches(false);
                connection.getInputStream();
            } catch (Exception e) {
                // ignore
            } finally {
                if (connection != null) connection.disconnect();
            }

            // Read TX packet counter again and make sure it increases
            int txcount2 = getTxPacketCount();

            if (txcount2 > txcount1) {
                break;
            } else {
                Thread.sleep(DURATION);
            }
        }
        assertTrue(i < 15);
    }
}
