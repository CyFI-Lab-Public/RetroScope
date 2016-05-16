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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.text.style.ReplacementSpan;

import java.lang.ref.WeakReference;

/**
 * Acts like an {@link ImageSpan}, with some important distinctions:
 * <ol>
 * <li>modifies font metrics in a manner consistent with {@link FolderSpan}
 * (will not increase line height beyond {@link FolderSpan} height)</li>
 * <li>draws its image vertically centered within its line</li>
 * <li>allows horizontal and vertical padding</li>
 * </ol>
 * Much of this code was copied from {@link DynamicDrawableSpan} and ImageSpan
 * because
 * {@link DynamicDrawableSpan#draw(Canvas, CharSequence, int, int, float, int, int, int, Paint)}
 * is difficult to override efficiently owing to the private getCachedDrawable()
 * method (filed as bug 5354674).
 */
public class PriorityIndicatorSpan extends ReplacementSpan {

    private final Context mContext;
    private final int mResId;
    private final int mPaddingV;
    private final int mPaddingH;
    private final int mPaddingAbove;

    private WeakReference<Drawable> mDrawableRef;

    public PriorityIndicatorSpan(Context context, int resId, int paddingV, int paddingH,
            int paddingAbove) {
        mContext = context;
        mResId = resId;
        mPaddingV = paddingV;
        mPaddingH = paddingH;
        mPaddingAbove = paddingAbove;
    }

    private Drawable getDrawable() {
        Drawable d = mContext.getResources().getDrawable(mResId);
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        return d;
    }

    private Drawable getCachedDrawable() {
        WeakReference<Drawable> wr = mDrawableRef;
        Drawable d = null;

        if (wr != null)
            d = wr.get();

        if (d == null) {
            d = getDrawable();
            mDrawableRef = new WeakReference<Drawable>(d);
        }

        return d;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, FontMetricsInt fm) {
        Drawable d = getCachedDrawable();
        Rect rect = d.getBounds();

        if (fm != null) {
            paint.getFontMetricsInt(fm);
            fm.ascent -= mPaddingV;
            fm.top = fm.ascent;
            fm.bottom += mPaddingV;
            fm.descent += mPaddingV;
        }

        return rect.right + 2 * mPaddingH;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top,
            int y, int bottom, Paint paint) {
        Drawable b = getCachedDrawable();
        canvas.save();

        final int transY = (top + bottom + mPaddingAbove - b.getBounds().bottom) / 2;

        canvas.translate(x + mPaddingH, transY);
        b.draw(canvas);
        canvas.restore();
    }

}
