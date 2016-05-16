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

package android.text.style.cts;


import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;

public class TextAppearanceSpanTest extends AndroidTestCase {
    public void testConstructor() {
        new TextAppearanceSpan(mContext, 1);
        new TextAppearanceSpan(mContext, 1, 1);

        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        Parcel p = Parcel.obtain();
        try {
            textAppearanceSpan.writeToParcel(p, 0);
            p.setDataPosition(0);
            new TextAppearanceSpan(p);
        } finally {
            p.recycle();
        }
        try {
            new TextAppearanceSpan(null, -1);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success
        }

        try {
            new TextAppearanceSpan(null, -1, -1);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success
        }

        new TextAppearanceSpan(null, -1, -1, null, null);
    }

    public void testGetFamily() {
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(mContext, 1);
        assertNull(textAppearanceSpan.getFamily());

        textAppearanceSpan = new TextAppearanceSpan(mContext, 1, 1);
        assertNull(textAppearanceSpan.getFamily());

        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        assertEquals("sans", textAppearanceSpan.getFamily());
    }

    public void testUpdateMeasureState() {
        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        TextPaint tp = new TextPaint();
        tp.setTextSize((float) 1);
        assertEquals((float) 1, tp.getTextSize());

        textAppearanceSpan.updateMeasureState(tp);

        assertEquals((float) 6, tp.getTextSize());

        try {
            textAppearanceSpan.updateMeasureState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success
        }
    }

    public void testGetTextColor() {
        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        assertSame(csl, textAppearanceSpan.getTextColor());

        textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, null, csl);
        assertNull(textAppearanceSpan.getTextColor());
    }

    public void testGetTextSize() {
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(mContext, 1);
        assertEquals(-1, textAppearanceSpan.getTextSize());

        textAppearanceSpan = new TextAppearanceSpan(mContext, 1, 1);
        assertEquals(-1, textAppearanceSpan.getTextSize());

        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        assertEquals(6, textAppearanceSpan.getTextSize());
    }

    public void testGetTextStyle() {
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(mContext, 1);
        assertEquals(0, textAppearanceSpan.getTextStyle());

        textAppearanceSpan = new TextAppearanceSpan(mContext, 1, 1);
        assertEquals(0, textAppearanceSpan.getTextStyle());

        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        assertEquals(1, textAppearanceSpan.getTextStyle());
    }

    public void testGetLinkTextColor() {
        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        assertSame(csl, textAppearanceSpan.getLinkTextColor());

        textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, null);
        assertNull(textAppearanceSpan.getLinkTextColor());
    }

    public void testUpdateDrawState() {
        int[][] states = new int[][] { new int[0], new int[0] };
        int[] colors = new int[] { Color.rgb(0, 0, 255), Color.BLACK };
        ColorStateList csl = new ColorStateList(states, colors);

        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan("sans", 1, 6, csl, csl);
        TextPaint tp = new TextPaint();
        tp.setColor(0);
        tp.linkColor = 0;
        assertEquals(0, tp.getColor());

        textAppearanceSpan.updateDrawState(tp);

        int expected = csl.getColorForState(tp.drawableState, 0);
        assertEquals(expected, tp.getColor());
        assertEquals(expected, tp.linkColor);

        try {
            textAppearanceSpan.updateDrawState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success
        }
    }

    public void testDescribeContents() {
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(mContext, 1);
        textAppearanceSpan.describeContents();
    }

    public void testGetSpanTypeId() {
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(mContext, 1);
        textAppearanceSpan.getSpanTypeId();
    }

    public void testWriteToParcel() {
        Parcel p = Parcel.obtain();
        String family = "sans";
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(family, 1, 6, null, null);
        textAppearanceSpan.writeToParcel(p, 0);
        p.setDataPosition(0);
        TextAppearanceSpan newSpan = new TextAppearanceSpan(p);
        assertEquals(family, newSpan.getFamily());
        p.recycle();
    }
}
