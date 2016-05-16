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

package android.dpi.cts;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.test.AndroidTestCase;


/**
 * This the base class for verifying that the correct defaults are
 * loaded for the following manifest attributes:
 *
 * android:smallScreens
 * android:normalScreens
 * android:largeScreens
 * android:resizable
 * android:anyDensity
 *
 * The default value depends on the sdk version declared in the
 * manifest of the package.  sdk version <=3 defaults all values to
 * false.  It is true otherwise.
 */
public abstract class DefaultManifestAttributesTest extends AndroidTestCase {
    // Allow subclass to change the package name in seTUp
    protected String packageName;

    // Calculated during setUp
    private boolean expectedResult;
    private ApplicationInfo appInfo;

    protected ApplicationInfo getAppInfo() {
        return appInfo;
    }

    protected abstract String getPackageName();

    protected void setUp() {
        packageName = getPackageName();

        PackageManager pm = getContext().getPackageManager();
        try {
            appInfo = pm.getApplicationInfo(packageName, 0);

            // Setup expected info based on sdk version in ai
            if (appInfo.targetSdkVersion <= 3) {
                expectedResult = false;
            } else {
                expectedResult = true;
            }
        } catch (NameNotFoundException e) {
            fail("Should be able to find application info for this package");
        }
    }

    public void testSmallScreenDefault() {
        assertEquals(expectedResult,
                     (getAppInfo().flags & ApplicationInfo.FLAG_SUPPORTS_SMALL_SCREENS) != 0);
    }

    public void testNormalScreenDefault() {
        // Normal screens are always supported regardless of SDK
        // version.
        assertEquals(true,
                    (getAppInfo().flags & ApplicationInfo.FLAG_SUPPORTS_NORMAL_SCREENS) != 0);
    }

    public void testLargeScreenDefault() {
        assertEquals(expectedResult,
                    (getAppInfo().flags & ApplicationInfo.FLAG_SUPPORTS_LARGE_SCREENS) != 0);
    }

    public void testResizableDefault() {
        assertEquals(expectedResult,
                    (getAppInfo().flags & ApplicationInfo.FLAG_RESIZEABLE_FOR_SCREENS) != 0);
    }

    public void testAnyDensityDefault() {
        assertEquals(expectedResult,
                    (getAppInfo().flags & ApplicationInfo.FLAG_SUPPORTS_SCREEN_DENSITIES) != 0);
    }
}
