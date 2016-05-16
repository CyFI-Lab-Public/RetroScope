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
import android.test.ActivityInstrumentationTestCase2;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public class AspectRatioTest extends ActivityInstrumentationTestCase2<OrientationActivity> {

    private static final int[] ORIENTATIONS = new int[] {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
    };

    public AspectRatioTest() {
        super(OrientationActivity.class);
    }

    /**
     * Get all the aspect rations in different orientations. They could be
     * different due to the system bar being different sizes. Test that
     * one of the aspect ratios is within the range.
     */
    public void testAspectRatio() throws Exception {
        List<Double> aspectRatios = getAllAspectRatios();
        for (double aspectRatio : aspectRatios) {
            if (aspectRatio >= 1.333 && aspectRatio <= 1.86) {
                return;
            }
        }
        fail("Aspect ratios were not between 1.333 and 1.86: " + aspectRatios);
    }

    private List<Double> getAllAspectRatios() throws Exception {
        List<Double> aspectRatios = new ArrayList<Double>();
        for (int i = 0; i < ORIENTATIONS.length; i++) {
            Activity activity = startOrientationActivity(ORIENTATIONS[i]);
            aspectRatios.add(getAspectRatio(activity));
            tearDown();
        }
        return aspectRatios;
    }

    private double getAspectRatio(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        int max = Math.max(metrics.widthPixels, metrics.heightPixels);
        int min = Math.min(metrics.widthPixels, metrics.heightPixels);
        return (double) max / min;
    }

    private Activity startOrientationActivity(int orientation) {
        Intent intent = new Intent();
        intent.putExtra(OrientationActivity.EXTRA_ORIENTATION, orientation);
        setActivityIntent(intent);
        return getActivity();
    }
}
