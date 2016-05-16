/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.display.cts;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class DisplayTest extends AndroidTestCase {
    // This test is called from DisplayTestRunner which brings up an overlay display on the target
    // device. The overlay display parameters must match the ones defined there which are
    // 1281x721/214 (wxh/dpi).

    private static final int SECONDARY_DISPLAY_WIDTH = 1281;
    private static final int SECONDARY_DISPLAY_HEIGHT = 721;
    private static final int SECONDARY_DISPLAY_DPI = 214;
    private static final float SCALE_DENSITY_LOWER_BOUND =
            (float)(SECONDARY_DISPLAY_DPI - 1) / DisplayMetrics.DENSITY_DEFAULT;
    private static final float SCALE_DENSITY_UPPER_BOUND =
            (float)(SECONDARY_DISPLAY_DPI + 1) / DisplayMetrics.DENSITY_DEFAULT;
    // Matches com.android.internal.R.string.display_manager_overlay_display_name.
    private static final String OVERLAY_DISPLAY_NAME_PREFIX = "Overlay #";

    private DisplayManager mDisplayManager;
    private WindowManager mWindowManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDisplayManager = (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    private boolean isSecondarySize(Display display) {
        final Point p = new Point();
        display.getSize(p);
        return p.x == SECONDARY_DISPLAY_WIDTH && p.y == SECONDARY_DISPLAY_HEIGHT;
    }

    private Display getSecondaryDisplay(Display[] displays) {
        for (Display display : displays) {
            if (isSecondarySize(display)) {
                return display;
            }
        }
        return null;
    }

    /**
     * Verify that the getDisplays method returns both a default and an overlay display.
     */
    public void testGetDisplays() {
        Display[] displays = mDisplayManager.getDisplays();
        assertNotNull(displays);
        assertTrue(2 <= displays.length);
        boolean hasDefaultDisplay = false;
        boolean hasSecondaryDisplay = false;
        for (Display display : displays) {
            if (display.getDisplayId() == Display.DEFAULT_DISPLAY) {
                hasDefaultDisplay = true;
            }
            if (isSecondarySize(display)) {
                hasSecondaryDisplay = true;
            }
        }
        assertTrue(hasDefaultDisplay);
        assertTrue(hasSecondaryDisplay);
    }

    /**
     * Verify that the WindowManager returns the default display.
     */
    public void testDefaultDisplay() {
        assertEquals(Display.DEFAULT_DISPLAY, mWindowManager.getDefaultDisplay().getDisplayId());
    }

    /**
     * Verify that there is a secondary display.
     */
    public void testSecondaryDisplay() {
        Display display = getSecondaryDisplay(mDisplayManager.getDisplays());
        assertNotNull(display);
        assertTrue(Display.DEFAULT_DISPLAY != display.getDisplayId());
    }

    /**
     * Test the properties of the secondary Display.
     */
    public void testGetDisplayAttrs() {
        Display display = getSecondaryDisplay(mDisplayManager.getDisplays());

        assertEquals(SECONDARY_DISPLAY_WIDTH, display.getWidth());
        assertEquals(SECONDARY_DISPLAY_HEIGHT, display.getHeight());

        Point outSize = new Point();
        display.getSize(outSize);
        assertEquals(SECONDARY_DISPLAY_WIDTH, outSize.x);
        assertEquals(SECONDARY_DISPLAY_HEIGHT, outSize.y);

        assertEquals(0, display.getOrientation());

        assertEquals(PixelFormat.RGBA_8888, display.getPixelFormat());

        assertTrue(0 < display.getRefreshRate());

        assertTrue(display.getName().contains(OVERLAY_DISPLAY_NAME_PREFIX));
    }

    /**
     * Test that the getMetrics method fills in correct values.
     */
    public void testGetMetrics() {
        Display display = getSecondaryDisplay(mDisplayManager.getDisplays());

        Point outSize = new Point();
        display.getSize(outSize);

        DisplayMetrics outMetrics = new DisplayMetrics();
        outMetrics.setToDefaults();
        display.getMetrics(outMetrics);

        assertEquals(SECONDARY_DISPLAY_WIDTH, outMetrics.widthPixels);
        assertEquals(SECONDARY_DISPLAY_HEIGHT, outMetrics.heightPixels);

        // The scale is in [0.1, 3], and density is the scale factor.
        assertTrue(SCALE_DENSITY_LOWER_BOUND <= outMetrics.density
                && outMetrics.density <= SCALE_DENSITY_UPPER_BOUND);
        assertTrue(SCALE_DENSITY_LOWER_BOUND <= outMetrics.scaledDensity
                && outMetrics.scaledDensity <= SCALE_DENSITY_UPPER_BOUND);

        assertEquals(SECONDARY_DISPLAY_DPI, outMetrics.densityDpi);
        assertEquals((float)SECONDARY_DISPLAY_DPI, outMetrics.xdpi);
        assertEquals((float)SECONDARY_DISPLAY_DPI, outMetrics.ydpi);
    }

    /**
     * Test that the getCurrentSizeRange method returns correct values.
     */
    public void testGetCurrentSizeRange() {
        Display display = getSecondaryDisplay(mDisplayManager.getDisplays());

        Point smallest = new Point();
        Point largest = new Point();
        display.getCurrentSizeRange(smallest, largest);

        assertEquals(SECONDARY_DISPLAY_WIDTH, smallest.x);
        assertEquals(SECONDARY_DISPLAY_HEIGHT, smallest.y);
        assertEquals(SECONDARY_DISPLAY_WIDTH, largest.x);
        assertEquals(SECONDARY_DISPLAY_HEIGHT, largest.y);
    }

    /**
     * Test that the getFlags method returns no flag bits set for the overlay display.
     */
    public void testFlags() {
        Display display = getSecondaryDisplay(mDisplayManager.getDisplays());

        assertEquals(Display.FLAG_PRESENTATION, display.getFlags());
    }
}
