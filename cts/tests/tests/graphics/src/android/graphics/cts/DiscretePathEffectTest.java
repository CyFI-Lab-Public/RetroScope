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
import android.graphics.DiscretePathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;

import junit.framework.TestCase;

public class DiscretePathEffectTest extends TestCase {
    private static final int BITMAP_WIDTH = 200;
    private static final int BITMAP_HEIGHT = 100;
    private static final int START_X = 10;
    private static final int END_X = BITMAP_WIDTH - START_X;
    private static final int COORD_Y = BITMAP_HEIGHT / 2;
    private static final int SEGMENT_LENGTH = 10; // must be < BITMAP_WIDTH
    private static final int DEVIATION = 10; // must be < BITMAP_HEIGHT

    public void testDiscretePathEffect() {
        DiscretePathEffect effect = new DiscretePathEffect(SEGMENT_LENGTH, DEVIATION);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(0);
        paint.setPathEffect(effect);

        Path path = new Path();
        path.moveTo(START_X, COORD_Y);
        path.lineTo(END_X, COORD_Y);

        Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        bitmap.eraseColor(Color.TRANSPARENT);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawPath(path, paint);

        // draw guide line into red channel (each segment should cross this once)
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(0);
        paint.setXfermode(new PorterDuffXfermode(Mode.SCREEN));
        canvas.drawPath(path, paint);

        // draw guide rectangle into blue channel (each segment must be completely inside this)
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(1 + 2 * DEVIATION);
        canvas.drawPath(path, paint);

        int intersect = 0;
        int numGreenPixels = 0;
        int minY = BITMAP_HEIGHT;
        int maxY = 0;
        for (int y = 0; y < BITMAP_HEIGHT; y++) {
            for (int x = 0; x < BITMAP_WIDTH; x++) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.green(pixel) > 0) {
                    numGreenPixels += 1;
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                    assertEquals(0xFF, Color.blue(pixel));
                    if (Color.red(pixel) > 0) {
                        intersect += 1;
                    }
                }
            }
        }
        int lineLength = END_X - START_X;
        // the number of pixels in all segments must be at least the same as the line length
        assertTrue(numGreenPixels >= lineLength);
        // green line must vary in y direction
        assertTrue(maxY - minY > 0);
        // ... but not too much
        assertTrue(maxY - minY <= 1 + 2 * DEVIATION);
        // intersecting pixels must be less than line length, otherwise deviation doesn't work
        assertTrue(intersect < lineLength);
        // there must be at least as many intersecting pixels as there are full segments
        assertTrue(intersect >= lineLength / SEGMENT_LENGTH);
    }
}
