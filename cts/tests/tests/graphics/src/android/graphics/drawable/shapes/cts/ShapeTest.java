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
import android.graphics.drawable.shapes.Shape;

import junit.framework.TestCase;

public class ShapeTest extends TestCase {
    private static final int TEST_WIDTH  = 100;
    private static final int TEST_HEIGHT = 200;

    private static final int TEST_COLOR_1 = 0xFF00FF00;
    private static final int TEST_COLOR_2 = 0xFFFF0000;

    public void testSize() {
        MockShape mockShape = new MockShape();
        assertFalse(mockShape.hasCalledOnResize());

        mockShape.resize(200f, 300f);
        assertEquals(200f, mockShape.getWidth());
        assertEquals(300f, mockShape.getHeight());
        assertTrue(mockShape.hasCalledOnResize());

        mockShape.resize(0f, 0f);
        assertEquals(0f, mockShape.getWidth());
        assertEquals(0f, mockShape.getHeight());

        mockShape.resize(Float.MAX_VALUE, Float.MAX_VALUE);
        assertEquals(Float.MAX_VALUE, mockShape.getWidth());
        assertEquals(Float.MAX_VALUE, mockShape.getHeight());

        mockShape.resize(-1, -1);
        assertEquals(0f, mockShape.getWidth());
        assertEquals(0f, mockShape.getHeight());
    }

    public void testOnResize() {
        MockShape mockShape = new MockShape();
        assertFalse(mockShape.hasCalledOnResize());

        mockShape.resize(200f, 300f);
        assertTrue(mockShape.hasCalledOnResize());

        // size does not change
        mockShape.reset();
        mockShape.resize(200f, 300f);
        assertFalse(mockShape.hasCalledOnResize());

        // size changes
        mockShape.reset();
        mockShape.resize(100f, 200f);
        assertTrue(mockShape.hasCalledOnResize());
    }

    public void testClone() throws CloneNotSupportedException {
        Shape shape = new MockShape();
        shape.resize(100f, 200f);
        Shape clonedShape = shape.clone();
        assertEquals(100f, shape.getWidth());
        assertEquals(200f, shape.getHeight());

        assertNotSame(shape, clonedShape);
        assertEquals(shape.getWidth(), clonedShape.getWidth());
        assertEquals(shape.getHeight(), clonedShape.getHeight());
    }

    public void testHasAlpha() {
        Shape shape = new MockShape();
        assertTrue(shape.hasAlpha());
    }

    public void testDraw() {
        Shape shape = new MockShape();
        Bitmap bitmap = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setColor(TEST_COLOR_1);
        shape.resize(TEST_WIDTH, TEST_HEIGHT);

        shape.draw(canvas, paint);
        assertEquals(0, bitmap.getPixel(0, 0));

        paint.setColor(TEST_COLOR_2);
        shape.draw(canvas, paint);
        assertEquals(0, bitmap.getPixel(0, 0));
    }

    private static class MockShape extends Shape {
        private boolean mCalledOnResize = false;

        @Override
        public void draw(Canvas canvas, Paint paint) {
        }

        @Override
        protected void onResize(float width, float height) {
            super.onResize(width, height);
            mCalledOnResize = true;
        }

        public boolean hasCalledOnResize() {
            return mCalledOnResize;
        }

        public void reset() {
            mCalledOnResize = false;
        }
    }
}
