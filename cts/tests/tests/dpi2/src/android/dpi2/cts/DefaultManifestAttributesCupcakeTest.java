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

package android.dpi2.cts;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.test.AndroidTestCase;

import android.dpi.cts.DefaultManifestAttributesTest;

/**
 * This class actually tests the manifest attributes from
 * DefaultManifestAttributesTest for the cupcake sdk
 */
public class DefaultManifestAttributesCupcakeTest extends DefaultManifestAttributesTest {
    protected String getPackageName() {
        return "com.android.cts.dpi2";
    }

    // This is a sanity test to make sure that we're instrumenting the proper package
    public void testPackageHasExpectedSdkVersion() {
        assertEquals(3, getAppInfo().targetSdkVersion);
    }
}
