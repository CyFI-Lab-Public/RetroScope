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

import android.graphics.Paint.FontMetricsInt;
import android.test.AndroidTestCase;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;

public class DynamicLayoutTest extends AndroidTestCase {

    protected static final float SPACING_MULT_NO_SCALE = 1.0f;
    protected static final float SPACING_ADD_NO_SCALE = 0.0f;
    protected static final int DEFAULT_OUTER_WIDTH = 150;
    protected static final CharSequence SINGLELINE_CHAR_SEQUENCE = "......";
    protected static final String[] TEXT = {"CharSequence\n", "Char\tSequence\n", "CharSequence"};
    protected static final CharSequence MULTLINE_CHAR_SEQUENCE = TEXT[0] + TEXT[1] + TEXT[2];
    protected static final Layout.Alignment DEFAULT_ALIGN = Layout.Alignment.ALIGN_CENTER;
    protected TextPaint mDefaultPaint;

    private static final int LINE0 = 0;
    private static final int LINE0_TOP = 0;
    private static final int LINE1 = 1;
    private static final int LINE2 = 2;
    private static final int LINE3 = 3;

    private static final int ELLIPSIS_UNDEFINED = 0x80000000;

    private DynamicLayout mDynamicLayout;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDefaultPaint = new TextPaint();
        mDynamicLayout = new DynamicLayout(MULTLINE_CHAR_SEQUENCE,
                mDefaultPaint,
                DEFAULT_OUTER_WIDTH,
                DEFAULT_ALIGN,
                SPACING_MULT_NO_SCALE,
                SPACING_ADD_NO_SCALE,
                true);
    }

    public void testConstructors() {
        new DynamicLayout(SINGLELINE_CHAR_SEQUENCE,
                MULTLINE_CHAR_SEQUENCE,
                mDefaultPaint,
                DEFAULT_OUTER_WIDTH,
                DEFAULT_ALIGN,
                SPACING_MULT_NO_SCALE,
                SPACING_ADD_NO_SCALE,
                true);
        new DynamicLayout(SINGLELINE_CHAR_SEQUENCE,
                MULTLINE_CHAR_SEQUENCE,
                mDefaultPaint,
                DEFAULT_OUTER_WIDTH,
                DEFAULT_ALIGN,
                SPACING_MULT_NO_SCALE,
                SPACING_ADD_NO_SCALE,
                true,
                TextUtils.TruncateAt.START,
                DEFAULT_OUTER_WIDTH);
        new DynamicLayout(MULTLINE_CHAR_SEQUENCE,
                mDefaultPaint,
                DEFAULT_OUTER_WIDTH,
                DEFAULT_ALIGN,
                SPACING_MULT_NO_SCALE,
                SPACING_ADD_NO_SCALE,
                true);
    }

    public void testEllipsis() {
        final DynamicLayout dynamicLayout = new DynamicLayout(SINGLELINE_CHAR_SEQUENCE,
                MULTLINE_CHAR_SEQUENCE,
                mDefaultPaint,
                DEFAULT_OUTER_WIDTH,
                DEFAULT_ALIGN,
                SPACING_MULT_NO_SCALE,
                SPACING_ADD_NO_SCALE,
                true,
                TextUtils.TruncateAt.START,
                DEFAULT_OUTER_WIDTH);
        assertEquals(0, dynamicLayout.getEllipsisCount(LINE1));
        assertEquals(ELLIPSIS_UNDEFINED, dynamicLayout.getEllipsisStart(LINE1));
        assertEquals(DEFAULT_OUTER_WIDTH, dynamicLayout.getEllipsizedWidth());
    }

    /*
     * Test whether include the padding to calculate the layout.
     * 1. Include padding while calculate the layout.
     * 2. Don't include padding while calculate the layout.
     */
    public void testIncludePadding() {
        final FontMetricsInt fontMetricsInt = mDefaultPaint.getFontMetricsInt();

        DynamicLayout dynamicLayout = new DynamicLayout(SINGLELINE_CHAR_SEQUENCE,
                mDefaultPaint,
                DEFAULT_OUTER_WIDTH,
                DEFAULT_ALIGN,
                SPACING_MULT_NO_SCALE,
                SPACING_ADD_NO_SCALE,
                true);
        assertEquals(fontMetricsInt.top - fontMetricsInt.ascent, dynamicLayout.getTopPadding());
        assertEquals(fontMetricsInt.bottom - fontMetricsInt.descent,
                dynamicLayout.getBottomPadding());

        dynamicLayout = new DynamicLayout(SINGLELINE_CHAR_SEQUENCE,
                mDefaultPaint,
                DEFAULT_OUTER_WIDTH,
                DEFAULT_ALIGN,
                SPACING_MULT_NO_SCALE,
                SPACING_ADD_NO_SCALE,
                false);
        assertEquals(0, dynamicLayout.getTopPadding());
        assertEquals(0, dynamicLayout.getBottomPadding());
    }

    /*
     * Test the line count and whether include the Tab the layout.
     * 1. Include Tab. 2. Don't include Tab Use the Y-coordinate to calculate the line number
     * Test the line top
     * 1. the Y-coordinate of line top.2. the Y-coordinate of baseline.
     */
    public void testLineLayout() {
        assertEquals(TEXT.length, mDynamicLayout.getLineCount());
        assertFalse(mDynamicLayout.getLineContainsTab(LINE0));
        assertTrue(mDynamicLayout.getLineContainsTab(LINE1));

        assertEquals(LINE0_TOP, mDynamicLayout.getLineTop(LINE0));

        assertEquals(mDynamicLayout.getLineBottom(LINE0), mDynamicLayout.getLineTop(LINE1));
        assertEquals(mDynamicLayout.getLineBottom(LINE1), mDynamicLayout.getLineTop(LINE2));
        assertEquals(mDynamicLayout.getLineBottom(LINE2), mDynamicLayout.getLineTop(LINE3));

        try {
            assertEquals(mDynamicLayout.getLineBottom(mDynamicLayout.getLineCount()),
                    mDynamicLayout.getLineTop(mDynamicLayout.getLineCount() + 1));
            fail("Test DynamicLayout fail, should throw IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        assertEquals(mDynamicLayout.getLineDescent(LINE0) - mDynamicLayout.getLineAscent(LINE0),
                mDynamicLayout.getLineBottom(LINE0));

        assertEquals(mDynamicLayout.getLineDescent(LINE1) - mDynamicLayout.getLineAscent(LINE1),
                mDynamicLayout.getLineBottom(LINE1) - mDynamicLayout.getLineBottom(LINE0));

        assertEquals(mDynamicLayout.getLineDescent(LINE2) - mDynamicLayout.getLineAscent(LINE2),
                mDynamicLayout.getLineBottom(LINE2) - mDynamicLayout.getLineBottom(LINE1));

        assertEquals(LINE0, mDynamicLayout.getLineForVertical(mDynamicLayout.getLineTop(LINE0)));

        assertNotNull(mDynamicLayout.getLineDirections(LINE0));
        assertEquals(Layout.DIR_LEFT_TO_RIGHT, mDynamicLayout.getParagraphDirection(LINE0));

        assertEquals(0, mDynamicLayout.getLineStart(LINE0));
        assertEquals(TEXT[0].length(), mDynamicLayout.getLineStart(LINE1));
        assertEquals(TEXT[0].length() + TEXT[1].length(), mDynamicLayout.getLineStart(LINE2));
    }
}
