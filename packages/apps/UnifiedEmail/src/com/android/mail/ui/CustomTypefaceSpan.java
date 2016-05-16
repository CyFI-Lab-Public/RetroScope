/**
 * Copyright (c) 2013, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.ui;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

/**
 * CustomTypefaceSpan allows for the use of a non-framework font supplied in the
 * assets/fonts directory of the application. Use this class whenever the
 * framework does not contain a needed font.
 */
public class CustomTypefaceSpan extends TypefaceSpan {
    private final Typeface newType;
    private int newColor;
    private int newSize;

    /**
     * @param family Ignored since this uses a completely custom included font.
     * @param type Typeface, specified as: Typeface.createFromAsset(
     *            context.getAssets(), "fonts/Roboto-Medium.ttf"),
     * @param size Desired font size; this should have already been converted
     *            from a dimension.
     * @param color Desired font color; this should have already been converted
     *            to an integer representation of a color.
     */
    public CustomTypefaceSpan(String family, Typeface type, int size, int color) {
        super(family);
        newType = type;
        newSize = size;
        newColor = color;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        applyCustomTypeFace(ds, newType, newSize, newColor);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        applyCustomTypeFace(paint, newType, newSize, newColor);
    }

    private static void applyCustomTypeFace(Paint paint, Typeface tf, int newSize, int newColor) {
        int oldStyle;
        Typeface old = paint.getTypeface();
        if (old == null) {
            oldStyle = 0;
        } else {
            oldStyle = old.getStyle();
        }

        int fake = oldStyle & ~tf.getStyle();
        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }

        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTextSize(newSize);
        paint.setColor(newColor);
        paint.setTypeface(tf);
    }
}