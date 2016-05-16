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

import android.app.KeyguardManager;
import android.content.Context;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Verify the key input related operations require specific permissions.
 */
public class NoKeyPermissionTest extends AndroidTestCase {
    KeyguardManager  mKeyManager;
    KeyguardManager.KeyguardLock mKeyLock;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mKeyManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mKeyLock = mKeyManager.newKeyguardLock("testTag");
    }

    /**
     * Verify that KeyguardManager.KeyguardLock.disableKeyguard requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#DISABLE_KEYGUARD}.
     */
    @SmallTest
    public void testDisableKeyguard() {
        try {
            mKeyLock.disableKeyguard();
            fail("KeyguardManager.KeyguardLock.disableKeyguard did not throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that KeyguardManager.KeyguardLock.reenableKeyguard requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#DISABLE_KEYGUARD}.
     */
    @SmallTest
    public void testReenableKeyguard() {
        try {
            mKeyLock.reenableKeyguard();
            fail("KeyguardManager.KeyguardLock.reenableKeyguard did not throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that KeyguardManager.exitKeyguardSecurely requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#DISABLE_KEYGUARD}.
     */
    @SmallTest
    public void testExitKeyguardSecurely() {
        try {
            mKeyManager.exitKeyguardSecurely(null);
            fail("KeyguardManager.exitKeyguardSecurely did not throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }
}
