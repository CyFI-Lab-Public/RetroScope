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


import android.graphics.Rect;
import android.test.AndroidTestCase;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.text.style.StrikethroughSpan;

public class LayoutTest extends AndroidTestCase {
    private final static int LINE_COUNT = 5;
    private final static int LINE_HEIGHT = 12;
    private final static int LINE_DESCENT = 4;
    private final static CharSequence LAYOUT_TEXT = "alwei\t;sdfs\ndf @";

    private int mWidth;
    private Layout.Alignment mAlign;
    private float mSpacingmult;
    private float mSpacingadd;
    private SpannableString mSpannedText;

    private TextPaint mTextPaint;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTextPaint = new TextPaint();
        mSpannedText = new SpannableString(LAYOUT_TEXT);
        mSpannedText.setSpan(new StrikethroughSpan(), 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        mWidth = 11;
        mAlign = Alignment.ALIGN_CENTER;
        mSpacingmult = 1;
        mSpacingadd = 2;
    }

    public void testConstructor() {
        new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth, mAlign, mSpacingmult, mSpacingadd);

        try {
            new MockLayout(null, null, -1, null, 0, 0);
            fail("should throw IllegalArgumentException here");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetText() {
        CharSequence text = "test case 1";
        Layout layout = new MockLayout(text, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(text, layout.getText());

        layout = new MockLayout(null, mTextPaint, mWidth, mAlign, mSpacingmult, mSpacingadd);
        assertNull(layout.getText());
    }

    public void testGetPaint() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);

        assertSame(mTextPaint, layout.getPaint());

        layout = new MockLayout(LAYOUT_TEXT, null, mWidth, mAlign, mSpacingmult, mSpacingadd);
        assertNull(layout.getPaint());
    }

    public void testGetWidth() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, 10,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(10,  layout.getWidth());

        layout = new MockLayout(LAYOUT_TEXT, mTextPaint, 0, mAlign, mSpacingmult, mSpacingadd);
        assertEquals(0,  layout.getWidth());
    }

    public void testGetEllipsizedWidth() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, 15,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(15, layout.getEllipsizedWidth());

        layout = new MockLayout(LAYOUT_TEXT, mTextPaint, 0, mAlign, mSpacingmult, mSpacingadd);
        assertEquals(0,  layout.getEllipsizedWidth());
    }

    public void testIncreaseWidthTo() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        int oldWidth = layout.getWidth();

        layout.increaseWidthTo(oldWidth);
        assertEquals(oldWidth, layout.getWidth());

        try {
            layout.increaseWidthTo(oldWidth - 1);
            fail("should throw runtime exception attempted to reduce Layout width");
        } catch (RuntimeException e) {
        }

        layout.increaseWidthTo(oldWidth + 1);
        assertEquals(oldWidth + 1, layout.getWidth());
    }

    public void testGetHeight() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(60, layout.getHeight());
    }

    public void testGetAlignment() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertSame(mAlign, layout.getAlignment());

        layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth, null, mSpacingmult, mSpacingadd);
        assertNull(layout.getAlignment());
    }

    public void testGetSpacingMultiplier() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth, mAlign, -1, mSpacingadd);
        assertEquals(-1.0f, layout.getSpacingMultiplier());

        layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth, mAlign, 5, mSpacingadd);
        assertEquals(5.0f, layout.getSpacingMultiplier());
    }

    public void testGetSpacingAdd() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth, mAlign, mSpacingmult, -1);
        assertEquals(-1.0f, layout.getSpacingAdd());

        layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth, mAlign, mSpacingmult, 20);
        assertEquals(20.0f, layout.getSpacingAdd());
    }

    public void testGetLineBounds() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        Rect bounds = new Rect();

        assertEquals(32, layout.getLineBounds(2, bounds));
        assertEquals(0, bounds.left);
        assertEquals(mWidth, bounds.right);
        assertEquals(24, bounds.top);
        assertEquals(36, bounds.bottom);
    }

    public void testGetLineForVertical() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(0, layout.getLineForVertical(-1));
        assertEquals(0, layout.getLineForVertical(0));
        assertEquals(0, layout.getLineForVertical(LINE_COUNT));
        assertEquals(LINE_COUNT - 1, layout.getLineForVertical(1000));
    }

    public void testGetLineForOffset() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(0, layout.getLineForOffset(-1));
        assertEquals(1, layout.getLineForOffset(1));
        assertEquals(LINE_COUNT - 1, layout.getLineForOffset(LINE_COUNT - 1));
        assertEquals(LINE_COUNT - 1, layout.getLineForOffset(1000));
    }

    public void testGetLineEnd() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(2, layout.getLineEnd(1));
    }

    public void testGetLineVisibleEnd() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);

        assertEquals(2, layout.getLineVisibleEnd(1));
        assertEquals(LINE_COUNT, layout.getLineVisibleEnd(LINE_COUNT - 1));
        assertEquals(LAYOUT_TEXT.length(), layout.getLineVisibleEnd(LAYOUT_TEXT.length() - 1));
        try {
            layout.getLineVisibleEnd(LAYOUT_TEXT.length());
            fail("should throw .StringIndexOutOfBoundsException here");
        } catch (StringIndexOutOfBoundsException e) {
        }
    }

    public void testGetLineBottom() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(LINE_HEIGHT, layout.getLineBottom(0));
    }

    public void testGetLineBaseline() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(8, layout.getLineBaseline(0));
    }

    public void testGetLineAscent() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(-8, layout.getLineAscent(0));
    }

    public void testGetParagraphAlignment() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertSame(mAlign, layout.getParagraphAlignment(0));

        layout = new MockLayout(mSpannedText, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertSame(mAlign, layout.getParagraphAlignment(0));
        assertSame(mAlign, layout.getParagraphAlignment(1));
    }

    public void testGetParagraphLeft() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(0, layout.getParagraphLeft(0));
    }

    public void testGetParagraphRight() {
        Layout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertEquals(mWidth, layout.getParagraphRight(0));
    }

    public void testIsSpanned() {
        MockLayout layout = new MockLayout(LAYOUT_TEXT, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        // default is not spanned text
        assertFalse(layout.mockIsSpanned());

        // try to create a spanned text
        layout = new MockLayout(mSpannedText, mTextPaint, mWidth,
                mAlign, mSpacingmult, mSpacingadd);
        assertTrue(layout.mockIsSpanned());
    }

    public void testGetDesiredWidthRange() {
        CharSequence textShort = "test";
        CharSequence textLonger = "test\ngetDesiredWidth";
        CharSequence textLongest = "test getDesiredWidth";
        TextPaint paint = new TextPaint();
        float widthShort = Layout.getDesiredWidth(textShort, 0, textShort.length(), paint);
        float widthLonger = Layout.getDesiredWidth(textLonger, 0, textLonger.length(), paint);
        float widthLongest = Layout.getDesiredWidth(textLongest, 0, textLongest.length(), paint);
        float widthPartShort = Layout.getDesiredWidth(textShort, 2, textShort.length(), paint);
        float widthZero = Layout.getDesiredWidth(textLonger, 5, textShort.length() - 3, paint);
        assertTrue(widthLonger > widthShort);
        assertTrue(widthLongest > widthLonger);
        assertEquals(0f, widthZero);
        assertTrue(widthShort > widthPartShort);
    }

    public void testGetDesiredWidth() {
        CharSequence textShort = "test";
        CharSequence textLonger = "test\ngetDesiredWidth";
        CharSequence textLongest = "test getDesiredWidth";
        TextPaint paint = new TextPaint();
        float widthShort = Layout.getDesiredWidth(textShort, paint);
        float widthLonger = Layout.getDesiredWidth(textLonger, paint);
        float widthLongest = Layout.getDesiredWidth(textLongest, paint);
        assertTrue(widthLonger > widthShort);
        assertTrue(widthLongest > widthLonger);
    }

    private final class MockLayout extends Layout {
        public MockLayout(CharSequence text, TextPaint paint, int width,
                Alignment align, float spacingmult, float spacingadd) {
            super(text, paint, width, align, spacingmult, spacingadd);
        }

        protected boolean mockIsSpanned() {
            return super.isSpanned();
        }

        @Override
        public int getBottomPadding() {
            return 0;
        }

        @Override
        public int getEllipsisCount(int line) {
            return 0;
        }

        @Override
        public int getEllipsisStart(int line) {
            return 0;
        }

        @Override
        public boolean getLineContainsTab(int line) {
            return false;
        }

        @Override
        public int getLineCount() {
            return LINE_COUNT;
        }

        @Override
        public int getLineDescent(int line) {
            return LINE_DESCENT;
        }

        @Override
        public Directions getLineDirections(int line) {
            return null;
        }

        @Override
        public int getLineStart(int line) {
            if (line < 0) {
                return 0;
            }
            return line;
        }

        @Override
        public int getLineTop(int line) {
            if (line < 0) {
                return 0;
            }
            return LINE_HEIGHT * (line);
        }

        @Override
        public int getParagraphDirection(int line) {
            return 0;
        }

        @Override
        public int getTopPadding() {
            return 0;
        }
    }
}
