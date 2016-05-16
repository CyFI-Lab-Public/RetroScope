/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.accessibilityservice.cts;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.List;

/**
 * This test case is responsible to verify that the intent for launching
 * accessibility settings has an activity that handles it.
 */
public class AccessibilitySettingsTest extends AndroidTestCase {

    @MediumTest
    public void testAccessibilitySettingsIntentHandled() throws Throwable {
        PackageManager packageManager = mContext.getPackageManager();
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        List<ResolveInfo> resolvedActivities = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        // make sure accessibility settings exist
        String message = "Accessibility settings activity must be launched via Intent " +
                "Settings.ACTION_ACCESSIBILITY_SETTINGS";
        assertTrue(message, !resolvedActivities.isEmpty());
    }
}

