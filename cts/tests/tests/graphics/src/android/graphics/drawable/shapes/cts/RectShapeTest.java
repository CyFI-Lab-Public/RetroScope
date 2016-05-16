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
import android.graphics.drawable.shapes.RectShape;

import junit.framework.TestCase;

public class RectShapeTest extends TestCase {
    private static final int TEST_WIDTH  = 100;
    private static final int TEST_HEIGHT = 200;

    private static final int TEST_COLOR_1 = 0xFF00FF00;
    private static final int TEST_COLOR_2 = 0xFFFF0000;

    public void testConstructor() {
        new RectShape();
    }

    private void assertDrawSuccessfully(Bitmap bitmap, int width, int height, int color) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                assertEquals(color, bitmap.getPixel(i, j));
            }
        }
    }

    public void testDraw() {
        RectShape rectShape = new RectShape();
        Bitmap bitmap = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setColor(TEST_COLOR_1);
        rectShape.resize(TEST_WIDTH, TEST_HEIGHT);

        rectShape.draw(canvas, paint);
        assertDrawSuccessfully(bitmap, TEST_WIDTH, TEST_HEIGHT, TEST_COLOR_1);

        paint.setColor(TEST_COLOR_2);
        rectShape.draw(canvas, paint);
        assertDrawSuccessfully(bitmap, TEST_WIDTH, TEST_HEIGHT, TEST_COLOR_2);
    }

    public void testClone() throws CloneNotSupportedException {
        RectShape rectShape = new RectShape();
        rectShape.resize(100f, 200f);
        RectShape clonedShape = rectShape.clone();
        assertEquals(100f, rectShape.getWidth());
        assertEquals(200f, rectShape.getHeight());

        assertNotSame(rectShape, clonedShape);
        assertEquals(rectShape.getWidth(), clonedShape.getWidth());
        assertEquals(rectShape.getHeight(), clonedShape.getHeight());
    }

    public void testRect() {
        MyRectShape rectShape = new MyRectShape();
        RectF rect = rectShape.myRect();
        assertEquals(0.0f, rect.left);
        assertEquals(0.0f, rect.top);
        assertEquals(0.0f, rect.right);
        assertEquals(0.0f, rect.bottom);

        rectShape.resize(TEST_WIDTH, TEST_HEIGHT);
        rect = rectShape.myRect();
        assertEquals(0.0f, rect.left);
        assertEquals(0.0f, rect.top);
        assertEquals((float) TEST_WIDTH, rect.right);
        assertEquals((float) TEST_HEIGHT, rect.bottom);
    }

    private static class MyRectShape extends RectShape {
        public RectF myRect() {
            return super.rect();
        }
    }
}
