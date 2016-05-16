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
import android.graphics.PixelXorXfermode;
import android.graphics.Bitmap.Config;
import android.test.AndroidTestCase;

public class PixelXorXfermodeTest extends AndroidTestCase {

    public void testPixelXorXfermode() {
        int width = 100;
        int height = 100;
        Bitmap b1 = Bitmap.createBitmap(width / 2, height, Config.ARGB_8888);
        b1.eraseColor(Color.WHITE);
        Bitmap b2 = Bitmap.createBitmap(width, height / 2, Config.ARGB_8888);
        b2.eraseColor(Color.CYAN);

        Bitmap target = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        target.eraseColor(Color.BLACK);
        Canvas canvas = new Canvas(target);
        Paint p = new Paint();
        canvas.drawBitmap(b1, 0, 0, p);
        p.setXfermode(new PixelXorXfermode(Color.GREEN));
        canvas.drawBitmap(b2, 0, height / 2, p);
        assertEquals(Color.WHITE, target.getPixel(width / 4, height / 4));
        // white ^ green ^ cyan = yellow
        assertEquals(Color.YELLOW, target.getPixel(width / 4, height * 3 / 4));
        // black ^ green ^ cyan = blue
        assertEquals(Color.BLUE, target.getPixel(width * 3 / 4, height * 3 / 4));

        // XOR transfer always results in an opaque image
        p.setXfermode(new PixelXorXfermode(alphaColor(Color.GREEN, 25)));
        target.eraseColor(alphaColor(Color.BLACK, 42));
        p.setColor(alphaColor(Color.CYAN, 5));
        canvas.drawPaint(p);
        // result is always opaque, even though all inputs have alpha
        assertEquals(255, Color.alpha(target.getPixel(0, 0)));
    }

    private int alphaColor(int color, int alpha) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}
