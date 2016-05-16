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
package android.util.cts;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class DisplayMetricsTest extends AndroidTestCase {
    private Display initDisplay() {
        WindowManager windowManager = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        assertNotNull(windowManager);
        Display display = windowManager.getDefaultDisplay();
        assertNotNull(display);
        return display;
    }

    public void testDisplayMetricsOp() {
        DisplayMetrics outMetrics = new DisplayMetrics();
        outMetrics.setToDefaults();
        assertEquals(0, outMetrics.widthPixels);
        assertEquals(0, outMetrics.heightPixels);
        // according to Android enmulator doc UI -scale confine density should between 0.1 to 3
        assertTrue((0.1 <= outMetrics.density) && (outMetrics.density <= 3));
        assertTrue((0.1 <= outMetrics.scaledDensity) && (outMetrics.scaledDensity <= 3));
        assertTrue(0 < outMetrics.xdpi);
        assertTrue(0 < outMetrics.ydpi);

        Display display = initDisplay();
        display.getMetrics(outMetrics);
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setTo(outMetrics);
        assertEquals(display.getHeight(), metrics.heightPixels);
        assertEquals(display.getWidth(), metrics.widthPixels);
        // according to Android enmulator doc UI -scale confine density should between 0.1 to 3
        assertTrue((0.1 <= metrics.density) && (metrics.density <= 3));
        assertTrue((0.1 <= metrics.scaledDensity) && (metrics.scaledDensity <= 3));
        assertTrue(0 < metrics.xdpi);
        assertTrue(0 < metrics.ydpi);
    }
}
