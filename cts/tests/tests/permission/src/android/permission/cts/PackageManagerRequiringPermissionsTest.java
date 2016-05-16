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

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Verify the PackageManager related operations require specific permissions.
 */
@SmallTest
public class PackageManagerRequiringPermissionsTest extends AndroidTestCase {
    private static final String PACKAGE_NAME = "com.android.cts.stub";
    private PackageManager mPackageManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPackageManager = getContext().getPackageManager();
        assertNotNull(mPackageManager);
    }

    /**
     * Verify that PackageManager.setApplicationEnabledSetting requires permission.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_COMPONENT_ENABLED_STATE}.
     */
    public void testSetApplicationEnabledSetting() {
        try {
            mPackageManager.setApplicationEnabledSetting(PACKAGE_NAME,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            fail("PackageManager.setApplicationEnabledSetting did not throw SecurityException as"
                    + "expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that PackageManager.addPreferredActivity requires permission.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#SET_PREFERRED_APPLICATIONS}.
     */
    public void testAddPreferredActivity() {
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
            filter.addCategory(Intent.CATEGORY_HOME);
            mPackageManager.addPreferredActivity(filter, 0, null, null);
            fail("PackageManager.addPreferredActivity did not throw" +
                    " SecurityException as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that PackageManager.clearPackagePreferredActivities requires permission.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#SET_PREFERRED_APPLICATIONS}.
     */
    public void testClearPackagePreferredActivities() {
        try {
            mPackageManager.clearPackagePreferredActivities(null);
            fail("PackageManager.clearPackagePreferredActivities did not throw SecurityException"
                    + " as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that PackageManager.verifyPendingInstall requires permission.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#PACKAGE_VERIFICATION_AGENT}
     */
    public void testVerifyPendingInstall() {
        try {
            mPackageManager.verifyPendingInstall(1, 1);
            fail("PackageManager.verifyPendingInstall did not throw SecurityException"
                    + " as expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that PackageManager.extendVerificationTimeout requires permission.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#PACKAGE_VERIFICATION_AGENT}.
     */
    public void testExtendVerificationTimeout() {
        try {
            mPackageManager.extendVerificationTimeout(1, 1, 10000);
            fail("PackageManager.extendVerificationTimeout did not throw SecurityException"
                    + " as expected");
        } catch (SecurityException e) {
            // expected
        }
    }
}
