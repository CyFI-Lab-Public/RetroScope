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


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.test.AndroidTestCase;

public class PaintFlagsDrawFilterTest extends AndroidTestCase {

    private static final float TEXT_SIZE = 20;
    private static final float TEXT_X = 50;
    private static final float TEXT_Y = 50;
    private static final String TEXT = "Test";
    private static final int BITMAP_WIDTH = 100;
    private static final int BITMAP_HEIGHT = 100;
    private float mTextWidth;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testPaintFlagsDrawFilter() {

        Bitmap bitmapWithoutFilter = drawText(null);

        PaintFlagsDrawFilter filter = new PaintFlagsDrawFilter(Paint.UNDERLINE_TEXT_FLAG, 0);
        Bitmap bitmapWithFilter = drawText(filter);

        Bitmap combined = delta(bitmapWithoutFilter, bitmapWithFilter);
        assertUnderline(combined);
    }

    private Bitmap drawText(PaintFlagsDrawFilter filter) {
        Paint p = new Paint(Paint.UNDERLINE_TEXT_FLAG);
        p.setColor(Color.RED);
        p.setTextSize(TEXT_SIZE);
        p.setTextAlign(Align.CENTER);
        mTextWidth = p.measureText(TEXT);
        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.setDrawFilter(filter);
        c.drawColor(Color.BLACK);
        c.drawText(TEXT, TEXT_X, TEXT_Y, p);
        return b;
    }

    private Bitmap delta(Bitmap bitmapWithoutFilter, Bitmap bitmapWithFilter) {
        Bitmap combinedBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        combinedBitmap.eraseColor(Color.BLACK);
        int pixelWithoutFilter;
        int pixelWithFilter;
        for (int i = 0; i < BITMAP_WIDTH; i++) {
            for (int j = 0; j < BITMAP_HEIGHT; j++) {
                pixelWithoutFilter = bitmapWithoutFilter.getPixel(i, j);
                pixelWithFilter = bitmapWithFilter.getPixel(i, j);
                if (pixelWithoutFilter != pixelWithFilter) {
                    assertEquals(Color.RED, pixelWithoutFilter);
                    assertEquals(Color.BLACK, pixelWithFilter);
                    combinedBitmap.setPixel(i, j, Color.RED);
                }
            }
        }
        return combinedBitmap;
    }

    private void assertUnderline(Bitmap bitmap) {
        // Find smallest rectangle containing all RED pixels
        Rect rect = new Rect(BITMAP_WIDTH, BITMAP_HEIGHT, 0, 0);
        for (int y = 0; y < BITMAP_HEIGHT; y++) {
            for (int x = 0; x < BITMAP_WIDTH; x++) {
                int pixel = bitmap.getPixel(x, y);
                if (pixel == Color.RED) {
                    rect.left = Math.min(rect.left, x);
                    rect.right = Math.max(rect.right, x);
                    rect.top = Math.min(rect.top, y);
                    rect.bottom = Math.max(rect.bottom, y);
                }
            }
        }
        // underline is at least one pixel high
        assertTrue(rect.top <= rect.bottom);
        // underline is roughly the same length at the text (5% tolerance)
        assertEquals(mTextWidth, rect.right - rect.left, mTextWidth * 0.053);
        // underline is under the text or at least at the bottom of it
        assertTrue(rect.top >= TEXT_Y);
    }
}
