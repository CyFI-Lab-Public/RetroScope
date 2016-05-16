/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.test.ActivityInstrumentationTestCase2;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * {@link ActivityInstrumentationTestCase2} that tests {@link ActivityManager#getMemoryClass()}
 * by checking that the memory class matches the proper screen density and by launching an
 * application that attempts to allocate memory on the heap.
 */
public class ActivityManagerMemoryClassTest
        extends ActivityInstrumentationTestCase2<ActivityManagerMemoryClassLaunchActivity> {

    public ActivityManagerMemoryClassTest() {
        super(ActivityManagerMemoryClassLaunchActivity.class);
    }

    public void testGetMemoryClass() throws Exception {
        int memoryClass = getMemoryClass();
        int screenDensity = getScreenDensity();
        int screenSize = getScreenSize();
        assertMemoryForScreenDensity(memoryClass, screenDensity, screenSize);

        runHeapTestApp(memoryClass);
    }

    private int getMemoryClass() {
        Context context = getInstrumentation().getTargetContext();
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return activityManager.getMemoryClass();
    }

    private int getScreenDensity() {
        Context context = getInstrumentation().getTargetContext();
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.densityDpi;
    }

    private int getScreenSize() {
        Context context = getInstrumentation().getTargetContext();
        Configuration config = context.getResources().getConfiguration();
        return config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
    }

    private void assertMemoryForScreenDensity(int memoryClass, int screenDensity, int screenSize) {
        int expectedMinimumMemory = -1;
        boolean isXLarge = screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
        switch (screenDensity) {
            case DisplayMetrics.DENSITY_LOW:
                expectedMinimumMemory = 16;
                break;

            case DisplayMetrics.DENSITY_MEDIUM:
                expectedMinimumMemory = isXLarge ? 32 : 16;
                break;

            case DisplayMetrics.DENSITY_HIGH:
            case DisplayMetrics.DENSITY_TV:
                expectedMinimumMemory = isXLarge ? 64 : 32;
                break;

            case DisplayMetrics.DENSITY_XHIGH:
                expectedMinimumMemory = isXLarge ? 128 : 64;
                break;

            case DisplayMetrics.DENSITY_XXHIGH:
                expectedMinimumMemory = isXLarge ? 256 : 128;
                break;

            default:
                throw new IllegalArgumentException("No memory requirement specified "
                        + " for screen density " + screenDensity);
        }

        assertTrue("Expected to have at least " + expectedMinimumMemory
                + "mb of memory for screen density " + screenDensity,
                        memoryClass >= expectedMinimumMemory);
    }

    private void runHeapTestApp(int memoryClass) throws InterruptedException {
        Intent intent = new Intent();
        intent.putExtra(ActivityManagerMemoryClassLaunchActivity.MEMORY_CLASS_EXTRA,
                memoryClass);
        setActivityIntent(intent);
        ActivityManagerMemoryClassLaunchActivity activity = getActivity();
        assertEquals("The test application couldn't allocate memory close to the amount "
                + " specified by the memory class.", Activity.RESULT_OK, activity.getResult());
    }
}
