/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiEnterpriseConfig.Phase2;
import android.net.wifi.WifiManager;
import android.test.AndroidTestCase;

public class WifiEnterpriseConfigTest extends AndroidTestCase {
    private  WifiManager mWifiManager;

    private static final String SSID = "\"TestSSID\"";
    private static final String IDENTITY = "identity";
    private static final String PASSWORD = "password";
    private static final String SUBJECT_MATCH = "subjectmatch";
    private static final String ANON_IDENTITY = "anonidentity";
    private static final int ENABLE_DELAY = 10000;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWifiManager = (WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE);
        assertNotNull(mWifiManager);
        mWifiManager.setWifiEnabled(true);
        Thread.sleep(ENABLE_DELAY);
        assertTrue(mWifiManager.isWifiEnabled());
    }

    public void testSettersAndGetters() {
        WifiEnterpriseConfig config = new WifiEnterpriseConfig();
        assertTrue(config.getEapMethod() == Eap.NONE);
        config.setEapMethod(Eap.PEAP);
        assertTrue(config.getEapMethod() == Eap.PEAP);
        config.setEapMethod(Eap.PWD);
        assertTrue(config.getEapMethod() == Eap.PWD);
        config.setEapMethod(Eap.TLS);
        assertTrue(config.getEapMethod() == Eap.TLS);
        config.setEapMethod(Eap.TTLS);
        assertTrue(config.getEapMethod() == Eap.TTLS);
        assertTrue(config.getPhase2Method() == Phase2.NONE);
        config.setPhase2Method(Phase2.PAP);
        assertTrue(config.getPhase2Method() == Phase2.PAP);
        config.setPhase2Method(Phase2.MSCHAP);
        assertTrue(config.getPhase2Method() == Phase2.MSCHAP);
        config.setPhase2Method(Phase2.MSCHAPV2);
        assertTrue(config.getPhase2Method() == Phase2.MSCHAPV2);
        config.setPhase2Method(Phase2.GTC);
        assertTrue(config.getPhase2Method() == Phase2.GTC);
        config.setIdentity(IDENTITY);
        assertTrue(config.getIdentity().equals(IDENTITY));
        config.setAnonymousIdentity(ANON_IDENTITY);
        assertTrue(config.getAnonymousIdentity().equals(ANON_IDENTITY));
        config.setPassword(PASSWORD);
        assertTrue(config.getPassword().equals(PASSWORD));
        config.setCaCertificate(null);
        config.setClientKeyEntry(null, null);
        config.setSubjectMatch(SUBJECT_MATCH);
        assertTrue(config.getSubjectMatch().equals(SUBJECT_MATCH));
    }

    public void testAddEapNetwork() {
        WifiConfiguration config = new WifiConfiguration();
        WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
        enterpriseConfig.setEapMethod(Eap.PWD);
        enterpriseConfig.setIdentity(IDENTITY);
        enterpriseConfig.setPassword(PASSWORD);
        config.SSID = SSID;
        config.enterpriseConfig = enterpriseConfig;

        int netId = mWifiManager.addNetwork(config);
        assertTrue(doesSsidExist(SSID));
        mWifiManager.removeNetwork(netId);
        assertFalse(doesSsidExist(SSID));
    }

    private boolean doesSsidExist(String ssid) {
        for (final WifiConfiguration w : mWifiManager.getConfiguredNetworks()) {
            if (w.SSID.equals(ssid))
                return true;
        }
        return false;
    }
}
