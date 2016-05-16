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
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Verify the Wake Lock related operations require specific permissions.
 */
public class NoWakeLockPermissionTest extends AndroidTestCase {
    private PowerManager mPowerManager;

    private PowerManager.WakeLock mWakeLock;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "tag");
    }

    /**
     * Verify that WifiManager.WifiLock.acquire() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#WAKE_LOCK}.
     */
    @SmallTest
    public void testWifiLockAcquire() {
        final WifiManager wifiManager = (WifiManager) mContext.getSystemService(
                Context.WIFI_SERVICE);
        final WifiLock wifiLock = wifiManager.createWifiLock("WakeLockPermissionTest");
        try {
            wifiLock.acquire();
            fail("WifiManager.WifiLock.acquire() didn't throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that MediaPlayer.start() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#WAKE_LOCK}.
     */
    @SmallTest
    public void testMediaPlayerWakeLock() {
        final MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(mContext, PowerManager.FULL_WAKE_LOCK);
        try {
            mediaPlayer.start();
            fail("MediaPlayer.setWakeMode() did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }

        mediaPlayer.stop();
    }

    /**
     * Verify that PowerManager.WakeLock.acquire() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#WAKE_LOCK}.
     */
    @SmallTest
    public void testPowerManagerWakeLockAcquire() {
        try {
            mWakeLock.acquire();
            fail("WakeLock.acquire() did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that PowerManager.WakeLock.acquire(long) requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#WAKE_LOCK}.
     */
    @SmallTest
    public void testPowerManagerWakeLockAcquire2() {
        // Tset acquire(long)
        try {
            mWakeLock.acquire(1);
            fail("WakeLock.acquire(long) did not throw SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }
}
