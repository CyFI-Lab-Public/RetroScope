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

package android.app.cts;


import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.test.InstrumentationTestCase;

/**
 * Test {@link Application}.
 */
public class ApplicationTest extends InstrumentationTestCase {

    public void testApplication() throws Throwable {
        final Instrumentation instrumentation = getInstrumentation();
        final Context targetContext = instrumentation.getTargetContext();

        final Intent intent = new Intent(targetContext, MockApplicationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        final Activity activity = instrumentation.startActivitySync(intent);
        final MockApplication mockApp = (MockApplication) activity.getApplication();
        assertTrue(mockApp.isConstructorCalled);
        assertTrue(mockApp.isOnCreateCalled);

        //skip if the device doesn't support both of portrait and landscape orientation screens.
        final PackageManager pm = targetContext.getPackageManager();
        if(!(pm.hasSystemFeature(PackageManager.FEATURE_SCREEN_LANDSCAPE)
                && pm.hasSystemFeature(PackageManager.FEATURE_SCREEN_PORTRAIT))){
            return;
        }

        runTestOnUiThread(new Runnable() {
            public void run() {
               OrientationTestUtils.toggleOrientation(activity);
            }
        });
        instrumentation.waitForIdleSync();
        assertTrue(mockApp.isOnConfigurationChangedCalled);
    }

}
