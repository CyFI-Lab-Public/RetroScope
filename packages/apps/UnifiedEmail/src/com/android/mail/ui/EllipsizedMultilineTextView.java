/*
 * Copyright (C) 2013 Google Inc.
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

package com.android.mail.ui;

import android.content.Context;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A special MultiLine TextView that will apply ellipsize logic to only the last
 * line of text, such that the last line may be shorter than any previous lines.
 */
public class EllipsizedMultilineTextView extends TextView {

    public static final int ALL_AVAILABLE = -1;
    private int mMaxLines;

    public EllipsizedMultilineTextView(Context context) {
        this(context, null);
    }

    public EllipsizedMultilineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setMaxLines(int maxlines) {
        super.setMaxLines(maxlines);
        mMaxLines = maxlines;
    }

    /**
     * Ellipsize just the last line of text in this view and set the text to the
     * new ellipsized value.
     * @param text Text to set and ellipsize
     * @param avail available width in pixels for the last line
     * @param paint Paint that has the proper properties set to measure the text
     *            for this view
     * @return the {@link CharSequence} that was set on the {@link TextView}
     */
    public CharSequence setText(final CharSequence text, int avail) {
        if (text == null || text.length() == 0) {
            return text;
        }

        setEllipsize(null);
        setText(text);

        if (avail == ALL_AVAILABLE) {
            return text;
        }
        Layout layout = getLayout();

        if (layout == null) {
            final int w = getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
            layout = new StaticLayout(text, 0, text.length(), getPaint(), w, Alignment.ALIGN_NORMAL,
                    1.0f, 0f, false);
        }

        // find the last line of text and chop it according to available space
        final int lastLineStart = layout.getLineStart(mMaxLines - 1);
        final CharSequence remainder = TextUtils.ellipsize(text.subSequence(lastLineStart,
                text.length()), getPaint(), avail, TextUtils.TruncateAt.END);

        // assemble just the text portion, without spans
        final SpannableStringBuilder builder = new SpannableStringBuilder();

        builder.append(text.toString(), 0, lastLineStart);

        if (!TextUtils.isEmpty(remainder)) {
            builder.append(remainder.toString());
        }

        // Now copy the original spans into the assembled string, modified for any ellipsizing.
        //
        // Merely assembling the Spanned pieces together would result in duplicate CharacterStyle
        // spans in the assembled version if a CharacterStyle spanned across the lastLineStart
        // offset.
        if (text instanceof Spanned) {
            final Spanned s = (Spanned) text;
            final Object[] spans = s.getSpans(0, s.length(), Object.class);
            final int destLen = builder.length();
            for (int i = 0; i < spans.length; i++) {
                final Object span = spans[i];
                final int start = s.getSpanStart(span);
                final int end = s.getSpanEnd(span);
                final int flags = s.getSpanFlags(span);
                if (start <= destLen) {
                    builder.setSpan(span, start, Math.min(end, destLen), flags);
                }
            }
        }

        setText(builder);

        return builder;
    }
}