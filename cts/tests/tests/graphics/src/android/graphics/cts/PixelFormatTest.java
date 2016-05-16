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

package android.graphics.cts;

import junit.framework.TestCase;
import android.graphics.PixelFormat;

public class PixelFormatTest extends TestCase {

    private PixelFormat mPixelFormat;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPixelFormat = null;
    }

    public void testConstructor() {
        mPixelFormat = null;
        // new the PixelFormat instance
        mPixelFormat = new PixelFormat();
    }

    public void testGetPixelFormatInfo() {

        // new the PixelFormat instance
        mPixelFormat = new PixelFormat();

        PixelFormat.getPixelFormatInfo(PixelFormat.RGBA_8888, mPixelFormat);
        assertEquals(4, mPixelFormat.bytesPerPixel);
        assertEquals(32, mPixelFormat.bitsPerPixel);

        PixelFormat.getPixelFormatInfo(PixelFormat.RGBX_8888, mPixelFormat);
        assertEquals(4, mPixelFormat.bytesPerPixel);
        assertEquals(32, mPixelFormat.bitsPerPixel);

        PixelFormat.getPixelFormatInfo(PixelFormat.RGB_888, mPixelFormat);
        assertEquals(3, mPixelFormat.bytesPerPixel);
        assertEquals(24, mPixelFormat.bitsPerPixel);

        PixelFormat.getPixelFormatInfo(PixelFormat.RGB_565, mPixelFormat);
        assertEquals(2, mPixelFormat.bytesPerPixel);
        assertEquals(16, mPixelFormat.bitsPerPixel);

        PixelFormat.getPixelFormatInfo(PixelFormat.RGBA_5551, mPixelFormat);
        assertEquals(2, mPixelFormat.bytesPerPixel);
        assertEquals(16, mPixelFormat.bitsPerPixel);

        PixelFormat.getPixelFormatInfo(PixelFormat.RGBA_4444, mPixelFormat);
        assertEquals(2, mPixelFormat.bytesPerPixel);
        assertEquals(16, mPixelFormat.bitsPerPixel);

        PixelFormat.getPixelFormatInfo(PixelFormat.A_8, mPixelFormat);
        assertEquals(1, mPixelFormat.bytesPerPixel);
        assertEquals(8, mPixelFormat.bitsPerPixel);

        PixelFormat.getPixelFormatInfo(PixelFormat.L_8, mPixelFormat);
        assertEquals(1, mPixelFormat.bytesPerPixel);
        assertEquals(8, mPixelFormat.bitsPerPixel);

        PixelFormat.getPixelFormatInfo(PixelFormat.LA_88, mPixelFormat);
        assertEquals(2, mPixelFormat.bytesPerPixel);
        assertEquals(16, mPixelFormat.bitsPerPixel);

        PixelFormat.getPixelFormatInfo(PixelFormat.RGB_332, mPixelFormat);
        assertEquals(1, mPixelFormat.bytesPerPixel);
        assertEquals(8, mPixelFormat.bitsPerPixel);

        try {
            PixelFormat.getPixelFormatInfo(PixelFormat.UNKNOWN, mPixelFormat);
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            //excepted
        }

        try {
            PixelFormat.getPixelFormatInfo(PixelFormat.JPEG, mPixelFormat);
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            //excepted
        }

        try {
            PixelFormat.getPixelFormatInfo(PixelFormat.TRANSLUCENT, mPixelFormat);
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            //excepted
        }

        try {
            PixelFormat.getPixelFormatInfo(PixelFormat.TRANSPARENT, mPixelFormat);
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            //excepted
        }

        try {
            PixelFormat.getPixelFormatInfo(PixelFormat.OPAQUE, mPixelFormat);
            fail("Should throw IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            //excepted
        }
    }

    public void testFormatHasAlpha() {

        assertTrue(PixelFormat.formatHasAlpha(PixelFormat.RGBA_8888));
        assertFalse(PixelFormat.formatHasAlpha(PixelFormat.RGBX_8888));
        assertFalse(PixelFormat.formatHasAlpha(PixelFormat.RGB_888));
        assertFalse(PixelFormat.formatHasAlpha(PixelFormat.RGB_565));
        assertTrue(PixelFormat.formatHasAlpha(PixelFormat.RGBA_5551));
        assertTrue(PixelFormat.formatHasAlpha(PixelFormat.RGBA_4444));
        assertTrue(PixelFormat.formatHasAlpha(PixelFormat.A_8));
        assertFalse(PixelFormat.formatHasAlpha(PixelFormat.L_8));
        assertTrue(PixelFormat.formatHasAlpha(PixelFormat.LA_88));
        assertFalse(PixelFormat.formatHasAlpha(PixelFormat.RGB_332));
        assertFalse(PixelFormat.formatHasAlpha(PixelFormat.UNKNOWN));
    }

}
