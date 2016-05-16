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
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelXorXfermode;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.graphics.Bitmap.Config;
import android.graphics.Shader.TileMode;
import android.util.Log;

import junit.framework.TestCase;

public class ComposeShaderTest extends TestCase {

    private static final int SIZE = 255;
    private static final int TOLERANCE = 5;

    public void testPorterDuff() {
        LinearGradient blueGradient = new LinearGradient(0, 0, SIZE, 0,
                Color.GREEN, Color.BLUE, Shader.TileMode.CLAMP);
        LinearGradient redGradient = new LinearGradient(0, 0, 0, SIZE,
                Color.GREEN, Color.RED, Shader.TileMode.CLAMP);
        ComposeShader shader = new ComposeShader(blueGradient, redGradient, PorterDuff.Mode.SCREEN);

        Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setShader(shader);
        canvas.drawPaint(paint);

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float greenX = 1f - (x / 255f);
                float greenY = 1f - (y / 255f);
                int green = (int)((greenX + greenY - greenX * greenY) * 255);
                int pixel = bitmap.getPixel(x, y);
                try {
                    assertEquals(0xFF, Color.alpha(pixel), TOLERANCE);
                    assertEquals(y, Color.red(pixel), TOLERANCE);
                    assertEquals(green, Color.green(pixel), TOLERANCE);
                    assertEquals(x, Color.blue(pixel), TOLERANCE);
                } catch (Error e) {
                    Log.w(getClass().getName(), "Failed at (" + x + "," + y + ")");
                    throw e;
                }
            }
        }
    }

    public void testXfermode() {
        Bitmap greenBitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        greenBitmap.eraseColor(Color.GREEN);
        Bitmap cyanBitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        cyanBitmap.eraseColor(Color.CYAN);

        BitmapShader blueShader = new BitmapShader(greenBitmap, TileMode.CLAMP, TileMode.CLAMP);
        BitmapShader redShader = new BitmapShader(cyanBitmap, TileMode.CLAMP, TileMode.CLAMP);

        PixelXorXfermode xferMode = new PixelXorXfermode(Color.WHITE);

        ComposeShader shader = new ComposeShader(blueShader, redShader, xferMode);

        Bitmap bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setShader(shader);
        canvas.drawPaint(paint);

        // white ^ green ^ cyan = yellow
        assertEquals(Color.YELLOW, bitmap.getPixel(0, 0));
    }
}
