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

package android.dpi.cts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.test.ActivityInstrumentationTestCase2;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.ViewConfiguration;
import android.view.WindowManager;

public class ConfigurationScreenLayoutTest
        extends ActivityInstrumentationTestCase2<OrientationActivity> {

    private static final int[] ORIENTATIONS = new int[] {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
    };

    public ConfigurationScreenLayoutTest() {
        super(OrientationActivity.class);
    }

    public void testScreenLayout() throws Exception {
        int expectedScreenLayout = computeScreenLayout();
        int expectedSize = expectedScreenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        int expectedLong = expectedScreenLayout & Configuration.SCREENLAYOUT_LONG_MASK;

        // Check that all four orientations report the same configuration value.
        for (int i = 0; i < ORIENTATIONS.length; i++) {
            Activity activity = startOrientationActivity(ORIENTATIONS[i]);
            Configuration mConfig = activity.getResources().getConfiguration();
            int actualSize = mConfig.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
            int actualLong = mConfig.screenLayout & Configuration.SCREENLAYOUT_LONG_MASK;

            assertEquals("Expected screen size value of " + expectedSize + " but got " + actualSize
                    + " for orientation " + ORIENTATIONS[i], expectedSize, actualSize);
            assertEquals("Expected screen long value of " + expectedLong + " but got " + actualLong
                    + " for orientation " + ORIENTATIONS[i], expectedLong, actualLong);
            tearDown();
        }
    }

    /**
     * @return expected value of {@link Configuration#screenLayout} with the
     *         {@link Configuration#SCREENLAYOUT_LONG_MASK} and
     *         {@link Configuration#SCREENLAYOUT_SIZE_MASK} defined
     */
    private int computeScreenLayout() throws Exception {
        // 1. Start with the biggest configuration possible.
        // 2. For each orientation start an activity and compute what it's screenLayout value is
        // 3. Reduce the screenLayout values if they are smaller
        // 4. Return the reduced value which is what should be reported
        int screenLayout = Configuration.SCREENLAYOUT_SIZE_XLARGE
                | Configuration.SCREENLAYOUT_LONG_YES;
        for (int i = 0; i < ORIENTATIONS.length; i++) {
            Activity activity = startOrientationActivity(ORIENTATIONS[i]);
            screenLayout = reduceScreenLayout(activity, screenLayout);
            tearDown();
        }
        return screenLayout;
    }

    private Activity startOrientationActivity(int orientation) {
        Intent intent = new Intent();
        intent.putExtra(OrientationActivity.EXTRA_ORIENTATION, orientation);
        setActivityIntent(intent);
        return getActivity();
    }

    // Logic copied from WindowManagerService but using DisplayMetrics instead of internal APIs
    private int reduceScreenLayout(Context context, int screenLayout) {
        int screenLayoutSize;
        boolean screenLayoutLong;

        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        int max = Math.max(metrics.widthPixels, metrics.heightPixels);
        int min = Math.min(metrics.widthPixels, metrics.heightPixels);
        int longSize = (int) (max / metrics.density);
        int shortSize = (int) (min / metrics.density);

        if (longSize < 470) {
            screenLayoutSize = Configuration.SCREENLAYOUT_SIZE_SMALL;
            screenLayoutLong = false;
        } else {
            if (longSize >= 960 && shortSize >= 720) {
                screenLayoutSize = Configuration.SCREENLAYOUT_SIZE_XLARGE;
            } else if (longSize >= 640 && shortSize >= 480) {
                screenLayoutSize = Configuration.SCREENLAYOUT_SIZE_LARGE;
            } else {
                screenLayoutSize = Configuration.SCREENLAYOUT_SIZE_NORMAL;
            }

            if (((longSize * 3) / 5) >= (shortSize - 1)) {
                screenLayoutLong = true;
            } else {
                screenLayoutLong = false;
            }
        }

        if (!screenLayoutLong) {
            screenLayout = (screenLayout & ~Configuration.SCREENLAYOUT_LONG_MASK)
                    | Configuration.SCREENLAYOUT_LONG_NO;
        }
        int curSize = screenLayout&Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenLayoutSize < curSize) {
            screenLayout = (screenLayout&~Configuration.SCREENLAYOUT_SIZE_MASK)
                    | screenLayoutSize;
        }
        return screenLayout;
    }
}
