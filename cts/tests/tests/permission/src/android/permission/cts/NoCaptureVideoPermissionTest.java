/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.permission.cts;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.DisplayMetrics;

/**
 * Verify the capture system video output permission requirements.
 */
public class NoCaptureVideoPermissionTest extends AndroidTestCase {
    private static final String NAME = "VirtualDisplayTest";
    private static final int WIDTH = 720;
    private static final int HEIGHT = 480;
    private static final int DENSITY = DisplayMetrics.DENSITY_MEDIUM;

    /**
     * Verify that DisplayManager.createVirtualDisplay() requires permissions to
     * create public displays.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CAPTURE_VIDEO_OUTPUT} or
     *   {@link android.Manifest.permission#CAPTURE_SECURE_VIDEO_OUTPUT}.
     */
    @SmallTest
    public void testCreatePublicVirtualDisplay() {
        DisplayManager displayManager =
                (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
        ImageReader reader = ImageReader.newInstance(WIDTH, HEIGHT, PixelFormat.RGBX_8888, 1);
        try {
            displayManager.createVirtualDisplay(NAME, WIDTH, HEIGHT, DENSITY,
                    reader.getSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC);
            fail("DisplayManager.createVirtualDisplay() didn't throw SecurityException "
                    + "as expected when creating public virtual display.");
        } catch (SecurityException e) {
            // expected
        } finally {
            reader.close();
        }
    }

    /**
     * Verify that DisplayManager.createVirtualDisplay() requires permissions to
     * create secure displays.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CAPTURE_SECURE_VIDEO_OUTPUT}.
     */
    @SmallTest
    public void testCreateSecureVirtualDisplay() {
        DisplayManager displayManager =
                (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
        ImageReader reader = ImageReader.newInstance(WIDTH, HEIGHT, PixelFormat.RGBX_8888, 1);
        try {
            displayManager.createVirtualDisplay(NAME, WIDTH, HEIGHT, DENSITY,
                    reader.getSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE);
            fail("DisplayManager.createVirtualDisplay() didn't throw SecurityException "
                    + "as expected when creating secure virtual display.");
        } catch (SecurityException e) {
            // expected
        } finally {
            reader.close();
        }
    }

    /**
     * Verify that DisplayManager.createVirtualDisplay() does not requires permissions to
     * create private displays.
     */
    @SmallTest
    public void testCreatePrivateVirtualDisplay() {
        DisplayManager displayManager =
                (DisplayManager)mContext.getSystemService(Context.DISPLAY_SERVICE);
        ImageReader reader = ImageReader.newInstance(WIDTH, HEIGHT, PixelFormat.RGBX_8888, 1);
        try {
            VirtualDisplay display = displayManager.createVirtualDisplay(
                    NAME, WIDTH, HEIGHT, DENSITY,
                    reader.getSurface(), 0);
            display.release();
        } catch (SecurityException e) {
            fail("DisplayManager.createVirtualDisplay() should not throw SecurityException "
                    + "when creating private virtual display.");
        } finally {
            reader.close();
        }
    }
}
