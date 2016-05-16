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

package android.net.cts;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.test.AndroidTestCase;

public class NetworkInfoTest extends AndroidTestCase {

    public static final int TYPE_MOBILE = ConnectivityManager.TYPE_MOBILE;
    public static final int TYPE_WIFI = ConnectivityManager.TYPE_WIFI;
    public static final String MOBILE_TYPE_NAME = "mobile";
    public static final String WIFI_TYPE_NAME = "WIFI";

    public void testAccessNetworkInfoProperties() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] ni = cm.getAllNetworkInfo();
        assertTrue(ni.length >= 1);

        for (NetworkInfo netInfo: ni) {
            switch (netInfo.getType()) {
                case TYPE_MOBILE:
                    assertNetworkInfo(netInfo, MOBILE_TYPE_NAME);
                    break;
                case TYPE_WIFI:
                    assertNetworkInfo(netInfo, WIFI_TYPE_NAME);
                    break;
                 // TODO: Add BLUETOOTH_TETHER testing
                 default:
                     break;
            }
        }
    }

    private void assertNetworkInfo(NetworkInfo netInfo, String expectedTypeName) {
        assertEquals(expectedTypeName.compareToIgnoreCase(netInfo.getTypeName()), 0);
        if(netInfo.isConnectedOrConnecting()) {
            assertTrue(netInfo.isAvailable());
            if (State.CONNECTED == netInfo.getState()) {
                assertTrue(netInfo.isConnected());
            }
            assertTrue(State.CONNECTING == netInfo.getState()
                    || State.CONNECTED == netInfo.getState());
            assertTrue(DetailedState.SCANNING == netInfo.getDetailedState()
                    || DetailedState.CONNECTING == netInfo.getDetailedState()
                    || DetailedState.AUTHENTICATING == netInfo.getDetailedState()
                    || DetailedState.CONNECTED == netInfo.getDetailedState());
        }
        assertNotNull(netInfo.toString());
    }
}
