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

package android.graphics.drawable.shapes.cts;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.drawable.shapes.ArcShape;

import junit.framework.TestCase;

public class ArcShapeTest extends TestCase {
    private static final int TEST_WIDTH  = 100;
    private static final int TEST_HEIGHT = 200;

    private static final int TEST_COLOR_1 = 0xFF00FF00;
    private static final int TEST_COLOR_2 = 0xFFFF0000;

    private static final int TOLERANCE = 4; // tolerance in pixels

    public void testConstructor() {
        new ArcShape(1f, 5f);

        new ArcShape(0f, 0f);

        new ArcShape(-1f, -1f);
    }

    public void testDraw() {
        // draw completely.
        ArcShape arcShape = new ArcShape(0.0f, 360.0f);
        Bitmap bitmap = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setColor(TEST_COLOR_1);
        arcShape.resize(TEST_WIDTH, TEST_HEIGHT);

        arcShape.draw(canvas, paint);
        // check the color at the center of bitmap
        assertEquals(TEST_COLOR_1, bitmap.getPixel(TEST_WIDTH / 2, TEST_HEIGHT / 2));

        final int SQUARE = Math.min(TEST_WIDTH, TEST_HEIGHT);
        paint.setColor(TEST_COLOR_2);
        arcShape = new ArcShape(0.0f, 180.0f);
        arcShape.resize(SQUARE, SQUARE); // half circle
        arcShape.draw(canvas, paint);
        // count number of pixels with TEST_COLOR_2 along diagonal
        int count = 0;
        for (int i = 0; i < SQUARE; i++) {
            if (bitmap.getPixel(i, i) == TEST_COLOR_2) {
                count += 1;
            }
        }
        assertEquals((double)SQUARE / 2 / Math.sqrt(2), count, TOLERANCE);
    }
}
