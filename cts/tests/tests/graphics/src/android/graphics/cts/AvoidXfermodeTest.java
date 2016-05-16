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

import android.graphics.AvoidXfermode;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;

import junit.framework.TestCase;

public class AvoidXfermodeTest extends TestCase {
    /*
     * ToBeFixed: This test ought to work with a TOLERANCE of 0. See bug 2034547.
     */
    private static final int TOLERANCE = 255;
    private static final int BASE_SIZE = 50;
    private static final int BITMAP_HEIGHT = BASE_SIZE * 2;
    private static final int BITMAP_WIDTH = BASE_SIZE * 2;

    public void testAvoidXfermode() {
        Paint greenPaint;
        Paint redAvoidingGreenPaint;
        Paint blueTargetingGreenPaint;
        greenPaint = new Paint();
        greenPaint.setColor(Color.GREEN);
        AvoidXfermode avoidMode =
            new AvoidXfermode(greenPaint.getColor(), TOLERANCE, AvoidXfermode.Mode.AVOID);
        redAvoidingGreenPaint = new Paint();
        redAvoidingGreenPaint.setColor(Color.RED);
        redAvoidingGreenPaint.setXfermode(avoidMode);
        AvoidXfermode targetMode =
            new AvoidXfermode(greenPaint.getColor(), TOLERANCE, AvoidXfermode.Mode.TARGET);
        blueTargetingGreenPaint = new Paint();
        blueTargetingGreenPaint.setColor(Color.BLUE);
        blueTargetingGreenPaint.setXfermode(targetMode);

        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Config.ARGB_8888);
        b.eraseColor(Color.BLACK);
        Canvas canvas = new Canvas(b);
        // vertical bar on the left
        canvas.drawRect(0f, 0f, BASE_SIZE, 2 * BASE_SIZE, greenPaint);
        // horizontal bar on top
        canvas.drawRect(0f, 0f, 2 * BASE_SIZE, BASE_SIZE, redAvoidingGreenPaint);
        // horizontal bar at bottom
        canvas.drawRect(0f, BASE_SIZE, 2 * BASE_SIZE, 2 * BASE_SIZE, blueTargetingGreenPaint);

        assertEquals(Color.GREEN, b.getPixel(BASE_SIZE / 2, BASE_SIZE / 2));
        assertEquals(Color.RED, b.getPixel(BASE_SIZE + BASE_SIZE / 2, BASE_SIZE / 2));
        assertEquals(Color.BLUE, b.getPixel(BASE_SIZE / 2, BASE_SIZE + BASE_SIZE / 2));
        assertEquals(Color.BLACK, b.getPixel(BASE_SIZE + BASE_SIZE / 2, BASE_SIZE + BASE_SIZE / 2));
    }
}
