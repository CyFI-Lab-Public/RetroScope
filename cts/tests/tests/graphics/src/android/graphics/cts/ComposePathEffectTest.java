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
import android.graphics.ComposePathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Bitmap.Config;

import junit.framework.TestCase;

public class ComposePathEffectTest extends TestCase {
    private static final int BITMAP_WIDTH = 110;
    private static final int BITMAP_HEIGHT = 20;
    private static final int START_X = 10;
    private static final int END_X = BITMAP_WIDTH - 10;
    private static final int CENTER = BITMAP_HEIGHT / 2;

    public void testComposePathEffect() {
        Path path = new Path();
        path.moveTo(START_X, CENTER);
        path.lineTo(END_X, CENTER);

        // ----- ----- ----- -----
        PathEffect innerEffect = new DashPathEffect(new float[] {25, 5}, 0);
        // outer effect is applied to every segment of the inner effect
        // --- --- --- ---
        PathEffect outerEffect = new DashPathEffect(new float[] {15, 5}, 0);
        // --- - --- - --- - --- -
        PathEffect composedEffect = new ComposePathEffect(outerEffect, innerEffect);
        // --- - --- - --- - --- -
        PathEffect expectedEffect = new DashPathEffect(new float[] {15, 5, 5, 5}, 0);

        Bitmap actual = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        Canvas canvas = new Canvas(actual);
        Paint paint = makePaint();
        paint.setPathEffect(composedEffect);
        canvas.drawPath(path, paint);

        Bitmap expected = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        canvas = new Canvas(expected);
        paint = makePaint();
        paint.setPathEffect(expectedEffect);
        canvas.drawPath(path, paint);

        for (int y = 0; y < BITMAP_HEIGHT; y++) {
            for (int x = 0; x < BITMAP_WIDTH; x++) {
                assertEquals(expected.getPixel(x, y), actual.getPixel(x, y));
            }
        }
    }

    private Paint makePaint() {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0);
        return paint;
    }
}
