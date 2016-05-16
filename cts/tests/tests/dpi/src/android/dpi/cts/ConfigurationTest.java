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

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Test for verifying a device's screen configuration.
 */
public class ConfigurationTest extends AndroidTestCase {

    public void testScreenConfiguration() {
        WindowManager windowManager =
                (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        double xInches = (double) metrics.widthPixels / metrics.xdpi;
        double yInches = (double) metrics.heightPixels / metrics.ydpi;
        double diagonalInches = Math.sqrt(Math.pow(xInches, 2) + Math.pow(yInches, 2));
        assertTrue("Screen diagonal must be at least 2.5 inches: " + diagonalInches,
                diagonalInches >= 2.5d);

        double density = 160.0d * metrics.density;
        assertTrue("Screen density must be at least 100 dpi: " + density, density >= 100.0d);

        Set<Integer> allowedDensities = new HashSet<Integer>();
        allowedDensities.add(DisplayMetrics.DENSITY_LOW);
        allowedDensities.add(DisplayMetrics.DENSITY_MEDIUM);
        allowedDensities.add(DisplayMetrics.DENSITY_TV);
        allowedDensities.add(DisplayMetrics.DENSITY_HIGH);
        allowedDensities.add(DisplayMetrics.DENSITY_XHIGH);
        allowedDensities.add(DisplayMetrics.DENSITY_XXHIGH);
        assertTrue("DisplayMetrics#densityDpi must be one of the DisplayMetrics.DENSITY_* values: "
                + allowedDensities, allowedDensities.contains(metrics.densityDpi));

        assertEquals(metrics.density,
                (float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT,
                0.5f / DisplayMetrics.DENSITY_DEFAULT);
    }
}
