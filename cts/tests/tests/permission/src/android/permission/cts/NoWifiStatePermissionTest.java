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

package android.permission.cts;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Verify WifiManager related methods without specific Wifi state permissions.
 */
@SmallTest
public class NoWifiStatePermissionTest extends AndroidTestCase {
    private static final int TEST_NET_ID = 1;
    private static final WifiConfiguration TEST_WIFI_CONFIGURATION = new WifiConfiguration();
    private WifiManager mWifiManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        assertNotNull(mWifiManager);
    }

    /**
     * Verify that WifiManager#getWifiState() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#ACCESS_WIFI_STATE}.
     */
    public void testGetWifiState() {
        try {
            mWifiManager.getWifiState();
            fail("WifiManager.getWifiState didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#getConfiguredNetworks() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#ACCESS_WIFI_STATE}.
     */
    public void testGetConfiguredNetworks() {
        try {
            mWifiManager.getConfiguredNetworks();
            fail("WifiManager.getConfiguredNetworks didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#getConnectionInfo() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#ACCESS_WIFI_STATE}.
     */
    public void testGetConnectionInfo() {
        try {
            mWifiManager.getConnectionInfo();
            fail("WifiManager.getConnectionInfo didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#getScanResults() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#ACCESS_WIFI_STATE}.
     */
    public void testGetScanResults() {
        try {
            mWifiManager.getScanResults();
            fail("WifiManager.getScanResults didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#getDhcpInfo() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#ACCESS_WIFI_STATE}.
     */
    public void testGetDhcpInfo() {
        try {
            mWifiManager.getDhcpInfo();
            fail("WifiManager.getDhcpInfo didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#disconnect() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testDisconnect() {
        try {
            mWifiManager.disconnect();
            fail("WifiManager.disconnect didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#reconnect() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testReconnect() {
        try {
            mWifiManager.reconnect();
            fail("WifiManager.reconnect didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#reassociate() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testReassociate() {
        try {
            mWifiManager.reassociate();
            fail("WifiManager.reassociate didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#addNetwork() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testAddNetwork() {
        try {
            mWifiManager.addNetwork(TEST_WIFI_CONFIGURATION);
            fail("WifiManager.addNetwork didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#updateNetwork() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testUpdateNetwork() {
        TEST_WIFI_CONFIGURATION.networkId = 2;

        try {
            mWifiManager.updateNetwork(TEST_WIFI_CONFIGURATION);
            fail("WifiManager.updateNetwork didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#removeNetwork() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testRemoveNetwork() {
        try {
            mWifiManager.removeNetwork(TEST_NET_ID);
            fail("WifiManager.removeNetwork didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#enableNetwork() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testEnableNetwork() {
        try {
            mWifiManager.enableNetwork(TEST_NET_ID, false);
            fail("WifiManager.enableNetwork didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#disableNetwork() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testDisableNetwork() {
        try {
            mWifiManager.disableNetwork(TEST_NET_ID);
            fail("WifiManager.disableNetwork didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#saveConfiguration() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testSaveConfiguration() {
        try {
            mWifiManager.saveConfiguration();
            fail("WifiManager.saveConfiguration didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#pingSupplicant() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testPingSupplicant() {
        try {
            mWifiManager.pingSupplicant();
            fail("WifiManager.pingSupplicant didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#startScan() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testStartScan() {
        try {
            mWifiManager.startScan();
            fail("WifiManager.startScan didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that WifiManager#setWifiEnabled() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_WIFI_STATE}.
     */
    public void testSetWifiEnabled() {
        try {
            mWifiManager.setWifiEnabled(true);
            fail("WifiManager.setWifiEnabled didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }
}
