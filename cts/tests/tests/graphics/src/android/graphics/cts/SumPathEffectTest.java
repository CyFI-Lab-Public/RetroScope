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
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.SumPathEffect;
import android.graphics.Bitmap.Config;
import android.graphics.Path.Direction;

import junit.framework.TestCase;

public class SumPathEffectTest extends TestCase {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;

    public void testSumPathEffect() {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Config.ARGB_8888);
        bitmap.eraseColor(Color.BLACK);
        Bitmap expected = Bitmap.createBitmap(WIDTH, HEIGHT, Config.ARGB_8888);
        expected.eraseColor(Color.BLACK);

        Path path = new Path();
        path.addRect(10, 10, WIDTH - 10, HEIGHT - 10, Direction.CW);

        PathEffect first = new CornerPathEffect(40);
        Canvas canvas = new Canvas(expected);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setPathEffect(first);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0); // 1-pixel hairline
        canvas.drawPath(path, paint);

        PathEffect second = new DashPathEffect(new float[] { 10, 5 }, 5);
        paint.setPathEffect(second);
        canvas.drawPath(path, paint);

        SumPathEffect sumPathEffect = new SumPathEffect(second, first);
        paint.setPathEffect(sumPathEffect);
        canvas = new Canvas(bitmap);
        canvas.drawPath(path, paint);

        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                assertEquals(expected.getPixel(i, j), bitmap.getPixel(i, j));
            }
        }
    }
}
