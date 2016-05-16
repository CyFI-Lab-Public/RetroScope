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
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;

import junit.framework.TestCase;

public class PorterDuffXfermodeTest extends TestCase {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;

    public void testPorterDuffXfermode() {
        Bitmap target = Bitmap.createBitmap(WIDTH, HEIGHT, Config.ARGB_8888);
        target.eraseColor(Color.TRANSPARENT);
        Bitmap b1 = Bitmap.createBitmap(WIDTH / 2, HEIGHT, Config.ARGB_8888);
        b1.eraseColor(Color.RED);
        Bitmap b2 = Bitmap.createBitmap(WIDTH, HEIGHT / 2, Config.ARGB_8888);
        b2.eraseColor(Color.BLUE);

        Canvas canvas = new Canvas(target);
        Paint p = new Paint();
        canvas.drawBitmap(b1, 0, 0, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        canvas.drawBitmap(b2, 0, HEIGHT / 2, p);
        assertEquals(Color.RED, target.getPixel(WIDTH / 4, HEIGHT / 4));
        assertEquals(Color.BLUE, target.getPixel(WIDTH / 4, HEIGHT * 3 / 4));
        assertEquals(Color.BLUE, target.getPixel(WIDTH * 3 / 4, HEIGHT * 3 / 4));

        target.eraseColor(Color.TRANSPARENT);
        p.setXfermode(null);
        canvas.drawBitmap(b1, 0, 0, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST));
        canvas.drawBitmap(b2, 0, HEIGHT / 2, p);
        assertEquals(Color.RED, target.getPixel(WIDTH / 4, HEIGHT / 4));
        assertEquals(Color.RED, target.getPixel(WIDTH / 4, HEIGHT * 3 / 4));
        assertEquals(Color.TRANSPARENT, target.getPixel(WIDTH * 3 / 4, HEIGHT * 3 / 4));

        target.eraseColor(Color.TRANSPARENT);
        p.setXfermode(null);
        canvas.drawBitmap(b1, 0, 0, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        canvas.drawBitmap(b2, 0, HEIGHT / 2, p);
        assertEquals(Color.RED, target.getPixel(WIDTH / 4, HEIGHT / 4));
        assertEquals(Color.MAGENTA, target.getPixel(WIDTH / 4, HEIGHT * 3 / 4));
        assertEquals(Color.BLUE, target.getPixel(WIDTH * 3 / 4, HEIGHT * 3 / 4));
    }
}
