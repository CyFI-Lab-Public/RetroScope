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
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Bitmap.Config;

import junit.framework.TestCase;

public class SweepGradientTest extends TestCase {

    private Paint mPaint;
    private Canvas mCanvas;
    private Bitmap mBitmap;

    private static final int SIZE = 200;
    private static final int CENTER = SIZE / 2;
    private static final int RADIUS = 80;
    private static final int NUM_STEPS = 100;
    private static final int TOLERANCE = 10;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPaint = new Paint();
        mBitmap = Bitmap.createBitmap(SIZE, SIZE, Config.ARGB_8888);
        mBitmap.eraseColor(Color.TRANSPARENT);
        mCanvas = new Canvas(mBitmap);
    }

    public void test2Colors() {
        final int[] colors = new int[] { Color.GREEN, Color.RED };
        final float[] positions = new float[] { 0f, 1f };
        Shader shader = new SweepGradient(CENTER, CENTER, colors[0], colors[1]);
        mPaint.setShader(shader);
        mCanvas.drawRect(new Rect(0, 0, SIZE, SIZE), mPaint);
        checkColors(colors, positions, TOLERANCE);
    }

    public void testColorArray() {
        final int[] colors = new int[] { Color.GREEN, Color.RED, Color.BLUE };
        final float[] positions = new float[] { 0f, 0.3f, 1f };
        Shader shader = new SweepGradient(CENTER, CENTER, colors, positions);
        mPaint.setShader(shader);
        mCanvas.drawRect(new Rect(0, 0, SIZE, SIZE), mPaint);

        checkColors(colors, positions, TOLERANCE);
    }

    public void testMultiColor() {
        final int[] colors = new int[] { Color.GREEN, Color.RED, Color.BLUE, Color.GREEN };
        final float[] positions = new float[] { 0f, 0.25f, 0.5f, 1f };

        Shader shader = new SweepGradient(CENTER, CENTER, colors, positions);
        mPaint.setShader(shader);
        mCanvas.drawRect(new Rect(0, 0, SIZE, SIZE), mPaint);

        checkColors(colors, positions, TOLERANCE);
    }

    private void checkColors(int[] colors, float[] positions, int tolerance) {
        final double twoPi = Math.PI * 2;
        final double step = twoPi / NUM_STEPS;

        // exclude angle 0, which is not defined
        for (double rad = step; rad <= twoPi - step; rad += step) {
            int x = CENTER + (int)(Math.cos(rad) * RADIUS);
            int y = CENTER + (int)(Math.sin(rad) * RADIUS);

            float relPos = (float)(rad / twoPi);
            int idx;
            int color;
            for (idx = 0; idx < positions.length; idx++) {
                if (positions[idx] > relPos) {
                    break;
                }
            }
            if (idx == 0) {
                // use start color
                color = colors[0];
            } else if (idx == positions.length) {
                // clamp to end color
                color = colors[positions.length - 1];
            } else {
                // linear interpolation
                int i1 = idx - 1; // index of next lower color and position
                int i2 = idx; // index of next higher color and position
                double delta = (relPos - positions[i1]) / (positions[i2] - positions[i1]);
                int alpha = (int) ((1d - delta) * Color.alpha(colors[i1]) +
                        delta * Color.alpha(colors[i2]));
                int red = (int) ((1d - delta) * Color.red(colors[i1]) +
                        delta * Color.red(colors[i2]));
                int green = (int) ((1d - delta) * Color.green(colors[i1]) +
                        delta * Color.green(colors[i2]));
                int blue = (int) ((1d - delta) * Color.blue(colors[i1]) +
                        delta * Color.blue(colors[i2]));
                color = Color.argb(alpha, red, green, blue);
            }

            int pixel = mBitmap.getPixel(x, y);

            try {
            assertEquals(Color.alpha(color), Color.alpha(pixel), tolerance);
            assertEquals(Color.red(color), Color.red(pixel), tolerance);
            assertEquals(Color.green(color), Color.green(pixel), tolerance);
            assertEquals(Color.blue(color), Color.blue(pixel), tolerance);
            } catch (Error e) {
                System.out.println("***************");
                System.out.println(rad);
                System.out.println(x);
                System.out.println(y);
                System.out.println(Integer.toHexString(pixel));
                System.out.println(Integer.toHexString(color));
                throw e;
            }
        }
    }
}
