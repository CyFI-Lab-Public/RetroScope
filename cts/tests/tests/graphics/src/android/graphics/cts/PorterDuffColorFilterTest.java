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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Bitmap.Config;
import android.test.AndroidTestCase;

public class PorterDuffColorFilterTest extends AndroidTestCase {

    private static final int TOLERANCE = 5;

    public void testPorterDuffColorFilter() {
        int width = 100;
        int height = 100;
        Bitmap b1 = Bitmap.createBitmap(width / 2, height, Config.ARGB_8888);
        b1.eraseColor(Color.RED);
        Bitmap b2 = Bitmap.createBitmap(width, height / 2, Config.ARGB_8888);
        b2.eraseColor(Color.BLUE);

        Bitmap target = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        target.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(target);
        // semi-transparent green
        int filterColor = Color.argb(0x80, 0, 0xFF, 0);
        PorterDuffColorFilter filter = new PorterDuffColorFilter(filterColor, PorterDuff.Mode.SRC);
        Paint p = new Paint();
        canvas.drawBitmap(b1, 0, 0, p);
        p.setColorFilter(filter);
        canvas.drawBitmap(b2, 0, height / 2, p);
        assertEquals(Color.RED, target.getPixel(width / 4, height / 4));
        int lowerLeft = target.getPixel(width / 4, height * 3 / 4);
        assertEquals(0x80, Color.red(lowerLeft), TOLERANCE);
        assertEquals(0x80, Color.green(lowerLeft), TOLERANCE);
        int lowerRight = target.getPixel(width * 3 / 4, height * 3 / 4);
        assertEquals(filterColor, lowerRight);

        target.eraseColor(Color.BLACK);
        filter = new PorterDuffColorFilter(filterColor, PorterDuff.Mode.DST);
        p.setColorFilter(null);
        canvas.drawBitmap(b1, 0, 0, p);
        p.setColorFilter(filter);
        canvas.drawBitmap(b2, 0, height / 2, p);
        assertEquals(Color.RED, target.getPixel(width / 4, height / 4));
        assertEquals(Color.BLUE, target.getPixel(width / 4, height * 3 / 4));
        assertEquals(Color.BLUE, target.getPixel(width * 3 / 4, height * 3 / 4));

        target.eraseColor(Color.BLACK);
        filter = new PorterDuffColorFilter(Color.GREEN, PorterDuff.Mode.SCREEN);
        p.setColorFilter(null);
        canvas.drawBitmap(b1, 0, 0, p);
        p.setColorFilter(filter);
        canvas.drawBitmap(b2, 0, height / 2, p);
        assertEquals(Color.RED, target.getPixel(width / 4, height / 4));
        assertEquals(Color.CYAN, target.getPixel(width / 4, height * 3 / 4));
        assertEquals(Color.CYAN, target.getPixel(width * 3 / 4, height * 3 / 4));
    }
}
