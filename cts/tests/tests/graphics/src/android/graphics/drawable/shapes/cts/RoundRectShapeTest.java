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
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.drawable.shapes.RoundRectShape;

import junit.framework.TestCase;

public class RoundRectShapeTest extends TestCase {
    private static final int TEST_WIDTH  = 100;
    private static final int TEST_HEIGHT = 200;

    private static final int TEST_COLOR_1 = 0xFF00FF00;
    private static final int TEST_COLOR_2 = 0xFFFF0000;

    public void testConstructor() {
        new RoundRectShape(new float[8], new RectF(), new float[8]);

        new RoundRectShape(new float[9], new RectF(), new float[9]);

        new RoundRectShape(new float[8], null, new float[8]);

        new RoundRectShape(new float[8], new RectF(), null);

        try {
            new RoundRectShape(new float[7], new RectF(), new float[8]);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        new RoundRectShape(null, new RectF(), new float[8]);

        try {
            new RoundRectShape(new float[8], new RectF(), new float[7]);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    public void testDraw() {
        float[] outerR = new float[] { 12, 12, 0, 0, 0, 0, 0, 0 };
        RectF   inset = new RectF(6, 6, 6, 6);
        float[] innerR = new float[] { 12, 12, 0, 0, 0, 0, 0, 0 };
        RoundRectShape roundRectShape = new RoundRectShape(outerR, inset, innerR);
        Bitmap bitmap = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setColor(TEST_COLOR_1);
        roundRectShape.resize(TEST_WIDTH, TEST_HEIGHT);

        roundRectShape.draw(canvas, paint);
        assertEquals(0, bitmap.getPixel(0, 0));
        assertEquals(TEST_COLOR_1, bitmap.getPixel(TEST_WIDTH / 2, 0));

        paint.setColor(TEST_COLOR_2);
        roundRectShape.draw(canvas, paint);
        assertEquals(0, bitmap.getPixel(0, 0));
        assertEquals(TEST_COLOR_2, bitmap.getPixel(TEST_WIDTH / 2, 0));
    }

    public void testClone() throws CloneNotSupportedException {
        RoundRectShape roundRectShape = new RoundRectShape(new float[8], new RectF(), new float[8]);
        roundRectShape.resize(100f, 200f);
        RoundRectShape clonedShape = roundRectShape.clone();
        assertEquals(100f, roundRectShape.getWidth());
        assertEquals(200f, roundRectShape.getHeight());

        assertNotSame(roundRectShape, clonedShape);
        assertEquals(roundRectShape.getWidth(), clonedShape.getWidth());
        assertEquals(roundRectShape.getHeight(), clonedShape.getHeight());
    }
}
