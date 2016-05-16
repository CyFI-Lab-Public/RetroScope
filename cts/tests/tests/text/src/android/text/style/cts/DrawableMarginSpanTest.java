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

import com.android.cts.stub.R;


import android.graphics.Canvas;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.drawable.Drawable;
import android.test.AndroidTestCase;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.DrawableMarginSpan;

public class DrawableMarginSpanTest extends AndroidTestCase {
    public void testConstructor() {
        Drawable d = mContext.getResources().getDrawable(R.drawable.pass);

        new DrawableMarginSpan(d);
        new DrawableMarginSpan(d, 1);
        new DrawableMarginSpan(null, -1);
    }

    public void testGetLeadingMargin() {
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.scenery);

        DrawableMarginSpan drawableMarginSpan = new DrawableMarginSpan(drawable, 1);
        int leadingMargin1 = drawableMarginSpan.getLeadingMargin(true);

        drawableMarginSpan = new DrawableMarginSpan(drawable, 10);
        int leadingMargin2 = drawableMarginSpan.getLeadingMargin(true);

        assertTrue(leadingMargin2 > leadingMargin1);
    }

    public void testDrawLeadingMargin() {
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.scenery);
        DrawableMarginSpan drawableMarginSpan = new DrawableMarginSpan(drawable, 0);

        assertEquals(0, drawable.getBounds().top);
        assertEquals(0, drawable.getBounds().bottom);
        assertEquals(0, drawable.getBounds().left);
        assertEquals(0, drawable.getBounds().right);

        Canvas canvas = new Canvas();
        Spanned text = Html.fromHtml("<b>hello</b>");
        TextPaint paint= new TextPaint();
        Layout layout = new StaticLayout("cts test.", paint, 200,
                Layout.Alignment.ALIGN_NORMAL, 1, 0, true);

        int x = 10;
        drawableMarginSpan.drawLeadingMargin(canvas, null, x, 0, 0,
                0, 0, text, 0, 0, true, layout);

        // 0 means the top location
        assertEquals(0, drawable.getBounds().top);
        assertEquals(0 + drawable.getIntrinsicHeight(), drawable.getBounds().bottom);
        assertEquals(x, drawable.getBounds().left);
        assertEquals(x + drawable.getIntrinsicWidth(), drawable.getBounds().right);

        try {
            drawableMarginSpan.drawLeadingMargin(null, null, 0, 0, 0, 0, 0,
                    null, 0, 0, false, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            drawableMarginSpan.drawLeadingMargin(null, null, 0, 0, 0, 0, 0,
                    "cts test.", 0, 0, false, null);
            fail("When try to use a String as the text, should throw ClassCastException.");
        } catch (ClassCastException e) {
            // expected, test success.
        }
    }

    public void testChooseHeight() {
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.scenery);
        DrawableMarginSpan drawableMarginSpan = new DrawableMarginSpan(drawable, 0);

        Spanned text = Html.fromHtml("cts test.");
        FontMetricsInt fm = new FontMetricsInt();

        assertEquals(0, fm.ascent);
        assertEquals(0, fm.bottom);
        assertEquals(0, fm.descent);
        assertEquals(0, fm.leading);
        assertEquals(0, fm.top);

        // should not change ascent, leading and top.
        drawableMarginSpan.chooseHeight(text, 0, text.getSpanEnd(drawableMarginSpan), 0, 0, fm);

        assertEquals(0, fm.ascent);
        // do not know what they should be, because missed javadoc. only check they are positive.
        assertTrue(fm.bottom > 0);
        assertTrue(fm.descent > 0);
        assertEquals(0, fm.leading);
        assertEquals(0, fm.top);

        try {
            drawableMarginSpan.chooseHeight(null, 0, 0, 0, 0, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            drawableMarginSpan.chooseHeight("cts test.", 0, 0, 0, 0, null);
            fail("When try to use a String as the text, should throw ClassCastException.");
        } catch (ClassCastException e) {
            // expected, test success.
        }
    }
}
