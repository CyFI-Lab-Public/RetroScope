/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.browse;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ReplacementSpan;

/**
 * A replacement span to use when displaying folders in conversation view. Prevents a folder name
 * from wrapping mid-name, and ellipsizes very long folder names that can't fit on a single line.
 * Also ensures that folder text is drawn vertically centered within the background color chip.
 *
 */
public class FolderSpan extends ReplacementSpan {

    public interface FolderSpanDimensions {
        int getPadding();

        /**
         * Returns the padding value that corresponds to the horizontal padding
         * surrounding the text inside the background color.
         */
        int getPaddingExtraWidth();

        /**
         * Returns the padding value for the space between folders.
         */
        int getPaddingBefore();

        /**
         * Returns the spacing above each line outside of the .
         */
        int getPaddingAbove();
        int getMaxWidth();
    }

    private TextPaint mWorkPaint = new TextPaint();
    /**
     * A reference to the enclosing Spanned object to collect other CharacterStyle spans and take
     * them into account when drawing.
     */
    private final Spanned mSpanned;
    private final FolderSpanDimensions mDims;

    public FolderSpan(Spanned spanned, FolderSpanDimensions dims) {
        mSpanned = spanned;
        mDims = dims;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, FontMetricsInt fm) {
        if (fm != null) {
            final int paddingV = mDims.getPadding();
            paint.getFontMetricsInt(fm);
            // The magic set of measurements to vertically center text within a colored region!
            fm.ascent -= paddingV;
            fm.top = fm.ascent;
            fm.bottom += paddingV;
            fm.descent += paddingV;
        }
        return measureWidth(paint, text, start, end, true);
    }

    private int measureWidth(Paint paint, CharSequence text, int start, int end,
            boolean includePaddingBefore) {
        final int paddingW = mDims.getPadding() + mDims.getPaddingExtraWidth();
        final int maxWidth = mDims.getMaxWidth();

        int w = (int) paint.measureText(text, start, end) + 2 * paddingW;
        if (includePaddingBefore) {
            w += mDims.getPaddingBefore();
        }
        // TextView doesn't handle spans that are wider than the view very well, so cap their widths
        if (w > maxWidth) {
            w = maxWidth;
        }
        return w;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top,
            int y, int bottom, Paint paint) {

        final int paddingW = mDims.getPadding() + mDims.getPaddingExtraWidth();
        final int paddingBefore = mDims.getPaddingBefore();
        final int paddingAbove = mDims.getPaddingAbove();
        final int maxWidth = mDims.getMaxWidth();

        mWorkPaint.set(paint);

        // take into account the foreground/background color spans when painting
        final CharacterStyle[] otherSpans = mSpanned.getSpans(start, end, CharacterStyle.class);
        for (CharacterStyle otherSpan : otherSpans) {
            otherSpan.updateDrawState(mWorkPaint);
        }

        final int bgWidth = measureWidth(mWorkPaint, text, start, end, false);

        // paint a background if present
        if (mWorkPaint.bgColor != 0) {
            final int prevColor = mWorkPaint.getColor();
            final Paint.Style prevStyle = mWorkPaint.getStyle();

            mWorkPaint.setColor(mWorkPaint.bgColor);
            mWorkPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(x + paddingBefore, top + paddingAbove, x + bgWidth + paddingBefore, bottom,
                    mWorkPaint);

            mWorkPaint.setColor(prevColor);
            mWorkPaint.setStyle(prevStyle);
        }

        // middle-ellipsize long strings
        if (bgWidth == maxWidth) {
            text = TextUtils.ellipsize(text.subSequence(start, end).toString(), mWorkPaint,
                    bgWidth - 2 * paddingW, TextUtils.TruncateAt.MIDDLE);
            start = 0;
            end = text.length();
        }
        canvas.drawText(text, start, end, x + paddingW + paddingBefore, y + paddingAbove, mWorkPaint);
    }

}
