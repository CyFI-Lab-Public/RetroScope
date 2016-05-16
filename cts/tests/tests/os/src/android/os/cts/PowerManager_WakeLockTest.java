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

package android.os.cts;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.test.AndroidTestCase;

public class PowerManager_WakeLockTest extends AndroidTestCase {
    private static final String TAG = "PowerManager_WakeLockTest";

    /**
     * Test points:
     * 1 Makes sure the device is on at the level you asked when you created the wake lock
     * 2 Release your claim to the CPU or screen being on
     * 3 Sets whether this WakeLock is ref counted
     */
    public void testPowerManagerWakeLock() throws InterruptedException {
        PowerManager pm = (PowerManager)  getContext().getSystemService(Context.POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        assertNotNull(wl.toString());

        wl.acquire();
        assertTrue(wl.isHeld());
        wl.release();
        assertFalse(wl.isHeld());

        // Try ref-counted acquire/release
        wl.setReferenceCounted(true);
        wl.acquire();
        assertTrue(wl.isHeld());
        wl.acquire();
        assertTrue(wl.isHeld());
        wl.release();
        assertTrue(wl.isHeld());
        wl.release();
        assertFalse(wl.isHeld());

        // Try non-ref-counted
        wl.setReferenceCounted(false);
        wl.acquire();
        assertTrue(wl.isHeld());
        wl.acquire();
        assertTrue(wl.isHeld());
        wl.release();
        assertFalse(wl.isHeld());

        // test acquire(long)
        wl.acquire(PowerManagerTest.TIME);
        assertTrue(wl.isHeld());
        Thread.sleep(PowerManagerTest.TIME + PowerManagerTest.MORE_TIME);
        assertFalse(wl.isHeld());
    }
}
