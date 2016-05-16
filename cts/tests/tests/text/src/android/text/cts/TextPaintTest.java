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

package android.text.cts;

import android.graphics.Color;
import android.graphics.Typeface;
import android.test.AndroidTestCase;
import android.text.TextPaint;

/**
 * Test {@link TextPaint}.
 */
public class TextPaintTest extends AndroidTestCase {
    private static final int DEFAULT_PAINT_FLAGS = TextPaint.DEV_KERN_TEXT_FLAG
            | TextPaint.EMBEDDED_BITMAP_TEXT_FLAG;

    public void testConstructor() {
        TextPaint textPaint;

        textPaint = new TextPaint();
        assertEquals(DEFAULT_PAINT_FLAGS, textPaint.getFlags());

        textPaint = new TextPaint(TextPaint.DITHER_FLAG);
        assertEquals((TextPaint.DITHER_FLAG | DEFAULT_PAINT_FLAGS),
                textPaint.getFlags());
    }

    public void testSet() {
        TextPaint textPaintSrc = new TextPaint(TextPaint.DITHER_FLAG);
        int[] drawableState = new int[] { 0, 1 };
        textPaintSrc.bgColor = Color.GREEN;
        textPaintSrc.baselineShift = 10;
        textPaintSrc.linkColor = Color.BLUE;
        textPaintSrc.drawableState = drawableState;
        textPaintSrc.setTypeface(Typeface.DEFAULT_BOLD);

        TextPaint textPaint = new TextPaint();
        assertEquals(0, textPaint.bgColor);
        assertEquals(0, textPaint.baselineShift);
        assertEquals(0, textPaint.linkColor);
        assertNull(textPaint.drawableState);
        assertNull(textPaint.getTypeface());

        textPaint.set(textPaintSrc);
        assertEquals(textPaintSrc.bgColor, textPaint.bgColor);
        assertEquals(textPaintSrc.baselineShift, textPaint.baselineShift);
        assertEquals(textPaintSrc.linkColor, textPaint.linkColor);
        assertSame(textPaintSrc.drawableState, textPaint.drawableState);
        assertEquals(textPaintSrc.getTypeface(), textPaint.getTypeface());
        assertEquals(textPaintSrc.getFlags(), textPaint.getFlags());

        try {
            textPaint.set(null);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }
    }
}
