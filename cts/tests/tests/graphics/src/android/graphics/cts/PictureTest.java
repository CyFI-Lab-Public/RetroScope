/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Paint.Style;


public class PictureTest extends TestCase {

    private static final int TEST_WIDTH = 4; // must be >= 2
    private static final int TEST_HEIGHT = 3; // must >= 2

    public void testPicture() throws Exception {

        Picture picture = new Picture();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        Canvas canvas = picture.beginRecording(TEST_WIDTH, TEST_HEIGHT);
        assertNotNull(canvas);
        drawPicture(canvas);
        picture.endRecording();

        Bitmap bitmap = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        picture.draw(canvas);
        checkSize(picture);
        checkBitmap(bitmap);

        picture.writeToStream(bout);
        picture = Picture.createFromStream(new ByteArrayInputStream(bout.toByteArray()));

        // create a new Canvas with a new bitmap
        bitmap = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        picture.draw(canvas);
        checkSize(picture);
        checkBitmap(bitmap);

        Picture pic = new Picture(picture);
        bitmap = Bitmap.createBitmap(TEST_WIDTH, TEST_HEIGHT, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        pic.draw(canvas);
        checkSize(pic);
        checkBitmap(bitmap);
    }

    private void checkSize(Picture picture) {
        assertEquals(TEST_WIDTH, picture.getWidth());
        assertEquals(TEST_HEIGHT, picture.getHeight());
    }

    private void drawPicture(Canvas canvas) {
        Paint paint = new Paint();
        // GREEN rectangle covering the entire canvas
        paint.setColor(Color.GREEN);
        paint.setStyle(Style.FILL);
        canvas.drawRect(0, 0, TEST_WIDTH, TEST_HEIGHT, paint);
        // horizontal red line starting from (0,0); overwrites first line of the rectangle
        paint.setColor(Color.RED);
        canvas.drawLine(0, 0, TEST_WIDTH, 0, paint);
        // overwrite (0,0) with a blue dot
        paint.setColor(Color.BLUE);
        canvas.drawPoint(0, 0, paint);
    }

    private void checkBitmap(Bitmap bitmap) {
        // first pixel is BLUE, rest of the line is RED
        assertEquals(Color.BLUE, bitmap.getPixel(0, 0));
        for (int x = 1; x < TEST_WIDTH; x++) {
            assertEquals(Color.RED, bitmap.getPixel(x, 0));
        }
        // remaining lines are all green
        for (int y = 1; y < TEST_HEIGHT; y++) {
            for (int x = 0; x < TEST_WIDTH; x++) {
                assertEquals(Color.GREEN, bitmap.getPixel(x, y));
            }
        }
    }
}
