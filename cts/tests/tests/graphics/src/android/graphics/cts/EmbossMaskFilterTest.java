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

package android.graphics.cts;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;

import junit.framework.TestCase;

public class EmbossMaskFilterTest extends TestCase {
    private static final int BITMAP_WIDTH = 100;
    private static final int BITMAP_HEIGHT = 100;
    private static final int START_X = 10;
    private static final int END_X = BITMAP_WIDTH - START_X;
    private static final int CENTER_X = (START_X + END_X) / 2;
    private static final int CENTER_Y = BITMAP_HEIGHT / 2;
    private static final int STROKE_WIDTH = 10;

    public void testEmbossMaskFilter() {
        EmbossMaskFilter filter = new EmbossMaskFilter(new float[] { 1, 1, 1 }, 0.5f, 8, 3);

        Paint paint = new Paint();
        paint.setMaskFilter(filter);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setColor(Color.GRAY);

        Path path = new Path();
        path.moveTo(START_X, CENTER_Y);
        path.lineTo(END_X, CENTER_Y);

        Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        bitmap.eraseColor(Color.BLACK);

        Canvas c = new Canvas(bitmap);
        c.drawPath(path, paint);

        Rect top = new Rect(0, 0, BITMAP_WIDTH, CENTER_Y);
        Rect bottom = new Rect(0, CENTER_Y, BITMAP_WIDTH, BITMAP_HEIGHT);
        Rect left = new Rect(0, 0, CENTER_X, BITMAP_HEIGHT);
        Rect right = new Rect(CENTER_X, 0, BITMAP_WIDTH, BITMAP_HEIGHT);

        assertTrue(brightness(bitmap, top) > brightness(bitmap, bottom));
        assertTrue(brightness(bitmap, left) > brightness(bitmap, right));

        // emboss must not change anything outside the drawn shape
        top.bottom = CENTER_Y - STROKE_WIDTH / 2;
        assertEquals(0, brightness(bitmap, top));
        bottom.top = CENTER_Y + STROKE_WIDTH / 2;
        assertEquals(0, brightness(bitmap, bottom));
        left.right = START_X;
        assertEquals(0, brightness(bitmap, left));
        right.left = END_X;
        assertEquals(0, brightness(bitmap, right));
    }

    /**
     * Calculate the cumulative brightness of all pixels in the given rectangle.
     * Ignores alpha channel. Maximum returned value depends on the size of the rectangle.
     */
    private long brightness(Bitmap b, Rect rect) {
        long color = 0;
        for (int y = rect.top; y < rect.bottom; y++) {
            for (int x = rect.left; x < rect.right; x++) {
                int pixel = b.getPixel(x, y);
                color += Color.red(pixel) + Color.green(pixel) + Color.blue(pixel);
            }
        }
        return color;
    }
}
