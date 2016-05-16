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
import android.os.PowerManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import dalvik.annotation.KnownFailure;

/**
 * Verify that various PowerManagement functionality requires Permission.
 */
public class DevicePowerPermissionTest extends AndroidTestCase {
    PowerManager mPowerManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
    }

    /**
     * Verify that going to sleep requires Permission.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#DEVICE_POWER}.
     */
    @LargeTest
    public void testGoToSleep() {
        try {
            mPowerManager.goToSleep(0);
            fail("Was able to call PowerManager.goToSleep without DEVICE_POWER Permission.");
        } catch (SecurityException e) {
            // expected
        }
    }
}
