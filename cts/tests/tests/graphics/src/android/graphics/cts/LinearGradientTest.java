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
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.Shader.TileMode;
import android.test.AndroidTestCase;

public class LinearGradientTest extends AndroidTestCase {

    public void testLinearGradient() {
        Bitmap b;
        LinearGradient lg;
        int[] color = { Color.BLUE, Color.GREEN, Color.RED };
        float[] position = { 0.0f, 1.0f / 3.0f, 2.0f / 3.0f };

        lg = new LinearGradient(0, 0, 0, 40, color, position, TileMode.CLAMP);
        b = drawLinearGradient(lg);

        // The pixels in same gradient line should be equivalent
        assertEquals(b.getPixel(10, 10), b.getPixel(20, 10));
        // BLUE -> GREEN, B sub-value decreasing while G sub-value increasing
        assertTrue(Color.blue(b.getPixel(10, 0)) > Color.blue(b.getPixel(10, 5)));
        assertTrue(Color.blue(b.getPixel(10, 5)) > Color.blue(b.getPixel(10, 10)));
        assertTrue(Color.green(b.getPixel(10, 0)) < Color.green(b.getPixel(10, 5)));
        assertTrue(Color.green(b.getPixel(10, 5)) < Color.green(b.getPixel(10, 10)));
        // GREEN -> RED, G sub-value decreasing while R sub-value increasing
        assertTrue(Color.green(b.getPixel(10, 15)) > Color.green(b.getPixel(10, 20)));
        assertTrue(Color.green(b.getPixel(10, 20)) > Color.green(b.getPixel(10, 25)));
        assertTrue(Color.red(b.getPixel(10, 15)) < Color.red(b.getPixel(10, 20)));
        assertTrue(Color.red(b.getPixel(10, 20)) < Color.red(b.getPixel(10, 25)));

        lg = new LinearGradient(0, 0, 0, 40, Color.RED, Color.BLUE, TileMode.CLAMP);
        b= drawLinearGradient(lg);

        // The pixels in same gradient line should be equivalent
        assertEquals(b.getPixel(10, 10), b.getPixel(20, 10));
        // RED -> BLUE, R sub-value decreasing while B sub-value increasing
        assertTrue(Color.red(b.getPixel(10, 0)) > Color.red(b.getPixel(10, 15)));
        assertTrue(Color.red(b.getPixel(10, 15)) > Color.red(b.getPixel(10, 30)));
        assertTrue(Color.blue(b.getPixel(10, 0)) < Color.blue(b.getPixel(10, 15)));
        assertTrue(Color.blue(b.getPixel(10, 15)) < Color.blue(b.getPixel(10, 30)));
    }

    private Bitmap drawLinearGradient(LinearGradient lg) {
        Paint paint = new Paint();
        paint.setShader(lg);
        Bitmap b = Bitmap.createBitmap(40, 40, Config.ARGB_8888);
        b.eraseColor(Color.BLACK);
        Canvas canvas = new Canvas(b);
        canvas.drawPaint(paint);
        return b;
    }
}
