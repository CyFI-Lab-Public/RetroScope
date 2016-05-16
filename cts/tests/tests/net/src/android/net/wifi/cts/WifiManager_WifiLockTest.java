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

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.test.AndroidTestCase;

public class WifiManager_WifiLockTest extends AndroidTestCase {

    private static final String WIFI_TAG = "WifiManager_WifiLockTest";

    public void testWifiLock() {
        if (!WifiFeature.isWifiSupported(getContext())) {
            // skip the test if WiFi is not supported
            return;
        }
        WifiManager wm = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        WifiLock wl = wm.createWifiLock(WIFI_TAG);

        wl.setReferenceCounted(true);
        assertFalse(wl.isHeld());
        wl.acquire();
        assertTrue(wl.isHeld());
        wl.release();
        assertFalse(wl.isHeld());
        wl.acquire();
        wl.acquire();
        assertTrue(wl.isHeld());
        wl.release();
        assertTrue(wl.isHeld());
        wl.release();
        assertFalse(wl.isHeld());
        assertNotNull(wl.toString());
        try {
            wl.release();
            fail("should throw out exception because release is called"
                    +" a greater number of times than acquire");
        } catch (RuntimeException e) {
            // expected
        }

        wl = wm.createWifiLock(WIFI_TAG);
        wl.setReferenceCounted(false);
        assertFalse(wl.isHeld());
        wl.acquire();
        assertTrue(wl.isHeld());
        wl.release();
        assertFalse(wl.isHeld());
        wl.acquire();
        wl.acquire();
        assertTrue(wl.isHeld());
        wl.release();
        assertFalse(wl.isHeld());
        assertNotNull(wl.toString());
        // should be ignored
        wl.release();
    }
}
