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
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
import android.test.AndroidTestCase;

public class PowerManagerTest extends AndroidTestCase {
    private static final String TAG = "PowerManagerTest";
    public static final long TIME = 3000;
    public static final int MORE_TIME = 300;

    /**
     * test points:
     * 1 Get a wake lock at the level of the flags parameter
     * 2 Force the device to go to sleep
     * 3 User activity happened
     */
    public void testPowerManager() throws InterruptedException {
        PowerManager pm = (PowerManager)getContext().getSystemService(Context.POWER_SERVICE);

        WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        wl.acquire(TIME);
        assertTrue(wl.isHeld());
        Thread.sleep(TIME + MORE_TIME);
        assertFalse(wl.isHeld());

        try {
            pm.goToSleep(SystemClock.uptimeMillis());
            fail("goToSleep should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        }

        try {
            pm.wakeUp(SystemClock.uptimeMillis());
            fail("wakeUp should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        }

        try {
            pm.nap(SystemClock.uptimeMillis());
            fail("nap should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        }

        try {
            pm.reboot("Testing");
            fail("reboot should throw SecurityException");
        } catch (SecurityException e) {
            // expected
        }

        // This method requires DEVICE_POWER but does not throw a SecurityException
        // for historical reasons.  So this call should be a no-op.
        pm.userActivity(SystemClock.uptimeMillis(), false);
    }
}
