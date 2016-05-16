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
import android.graphics.Path;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.drawable.shapes.PathShape;

import junit.framework.TestCase;

public class PathShapeTest extends TestCase {
    private static final int TEST_COLOR_1 = 0xFF00FF00;
    private static final int TEST_COLOR_2 = 0xFFFF0000;

    private static final int TOLERANCE = 4;

    public void testConstructor() {
        new PathShape(new Path(), 1f, 5f);

        new PathShape(new Path(), -1f, -1f);

        new PathShape(null, 0f, 0f);
    }

    public void testDraw() {
        final int SHAPE_SIZE = 200;

        // draw a square rotated by 45 degrees centered on (50, 50)
        Path path = new Path();
        path.moveTo(50, 0);
        path.lineTo(0, 50);
        path.lineTo(50, 100);
        path.lineTo(100, 50);
        path.close();
        PathShape pathShape = new PathShape(path, SHAPE_SIZE, SHAPE_SIZE);
        Bitmap bitmap = Bitmap.createBitmap(SHAPE_SIZE, SHAPE_SIZE, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStyle(Style.FILL);
        paint.setColor(TEST_COLOR_1);
        pathShape.resize(SHAPE_SIZE, SHAPE_SIZE);

        pathShape.draw(canvas, paint);
        // check center point
        assertEquals(TEST_COLOR_1, bitmap.getPixel(50, 50));

        paint.setColor(TEST_COLOR_2);
        // scale down to half size; diagonal is now 50px
        pathShape.resize(SHAPE_SIZE / 2, SHAPE_SIZE / 2);
        pathShape.draw(canvas, paint);
        // count number of pixels with TEST_COLOR_2 horizontally, vertically and diagonally
        int horizontal = 0;
        int vertical = 0;
        int diagonal = 0;
        for (int i = 0; i < 50; i++) {
            if (bitmap.getPixel(25, i) == TEST_COLOR_2) {
                vertical += 1;
            }
            if (bitmap.getPixel(i, 25) == TEST_COLOR_2) {
                horizontal += 1;
            }
            if (bitmap.getPixel(i, i) == TEST_COLOR_2) {
                diagonal += 1;
            }
        }
        assertEquals(50, horizontal, TOLERANCE);
        assertEquals(50, vertical, TOLERANCE);
        assertEquals(25, diagonal, TOLERANCE);
    }

    public void testClone() throws CloneNotSupportedException {
        PathShape pathShape = new PathShape(new Path(), 1f, 5f);
        pathShape.resize(100f, 200f);
        PathShape clonedShape = pathShape.clone();
        assertEquals(100f, pathShape.getWidth());
        assertEquals(200f, pathShape.getHeight());

        assertNotSame(pathShape, clonedShape);
        assertEquals(pathShape.getWidth(), clonedShape.getWidth());
        assertEquals(pathShape.getHeight(), clonedShape.getHeight());
    }
}
