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

package android.graphics.drawable.cts;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Picture;
import android.graphics.PixelFormat;
import android.graphics.drawable.PictureDrawable;
import android.test.AndroidTestCase;

public class PictureDrawableTest extends AndroidTestCase {
    public void testConstructor() {
        assertNull((new PictureDrawable(null)).getPicture());
        assertNotNull((new PictureDrawable(new Picture())).getPicture());
    }

    public void testDraw() {
        PictureDrawable pictureDrawable = new PictureDrawable(null);

        // Create Picture for drawing
        Picture picture = new Picture();
        Canvas recodingCanvas = picture.beginRecording(100, 200);
        recodingCanvas.drawARGB(255, 0xa, 0xc, 0xb);
        picture.endRecording();
        pictureDrawable.setPicture(picture);
        pictureDrawable.setBounds(0, 0, 100, 200);

        // Create drawable bitmap for rendering into
        Bitmap destBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
        // Set of drawing routines
        Canvas canvas = new Canvas(destBitmap);
        assertEquals(100, canvas.getClipBounds().width());
        assertEquals(200, canvas.getClipBounds().height());
        canvas.drawARGB(255, 0x0f, 0x0b, 0x0c);

        // Check the color has been set.
        assertEquals(0xff0f0b0c, destBitmap.getPixel(10, 10));
        pictureDrawable.draw(canvas);
        // Check the target pixle's color, ensure it has been changed.
        assertEquals(0xff0a0c0b, destBitmap.getPixel(10, 10));
    }

    public void testGetIntrinsicSize() {
        PictureDrawable pictureDrawable = new PictureDrawable(null);
        // Test with null Picture object
        assertEquals(-1, pictureDrawable.getIntrinsicWidth());
        assertEquals(-1, pictureDrawable.getIntrinsicHeight());

        Picture picture = new Picture();
        picture.beginRecording(99, 101);
        // Test with Picture object
        pictureDrawable.setPicture(picture);
        assertEquals(99, pictureDrawable.getIntrinsicWidth());
        assertEquals(101, pictureDrawable.getIntrinsicHeight());
    }

    public void testGetOpacity() {
        PictureDrawable pictureDrawable = new PictureDrawable(null);
        assertEquals(PixelFormat.TRANSLUCENT, pictureDrawable.getOpacity());
    }

    public void testSetAlpha() {
        PictureDrawable pictureDrawable = new PictureDrawable(null);
        pictureDrawable.setAlpha(0);
    }

    public void testSetColorFilter() {
        PictureDrawable pictureDrawable = new PictureDrawable(null);

        ColorFilter colorFilter = new ColorFilter();
        pictureDrawable.setColorFilter(colorFilter);
    }

    public void testSetDither() {
        PictureDrawable pictureDrawable = new PictureDrawable(null);
        pictureDrawable.setDither(true);
    }

    public void testSetFilterBitmap() {
        PictureDrawable pictureDrawable = new PictureDrawable(null);
        pictureDrawable.setFilterBitmap(true);
    }

    public void testAccessPicture() {
        PictureDrawable pictureDrawable = new PictureDrawable(null);
        assertNull(pictureDrawable.getPicture());

        // Test with real picture object.
        Picture picture = new Picture();
        pictureDrawable.setPicture(picture);
        assertNotNull(pictureDrawable.getPicture());
        assertEquals(picture, pictureDrawable.getPicture());

        // Test with null input.
        pictureDrawable.setPicture(null);
        assertNull(pictureDrawable.getPicture());
    }
}
