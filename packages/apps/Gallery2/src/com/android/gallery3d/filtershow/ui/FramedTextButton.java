/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.filtershow.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.android.gallery3d.R;

public class FramedTextButton extends ImageButton {
    private static final String LOGTAG = "FramedTextButton";
    private String mText = null;
    private static int mTextSize = 24;
    private static int mTextPadding = 20;
    private static Paint gPaint = new Paint();
    private static Path gPath = new Path();
    private static int mTrianglePadding = 2;
    private static int mTriangleSize = 30;

    public static void setTextSize(int value) {
        mTextSize = value;
    }

    public static void setTextPadding(int value) {
        mTextPadding = value;
    }

    public static void setTrianglePadding(int value) {
        mTrianglePadding = value;
    }

    public static void setTriangleSize(int value) {
        mTriangleSize = value;
    }

    public void setText(String text) {
        mText = text;
        invalidate();
    }

    public void setTextFrom(int itemId) {
        switch (itemId) {
            case R.id.curve_menu_rgb: {
                setText(getContext().getString(R.string.curves_channel_rgb));
                break;
            }
            case R.id.curve_menu_red: {
                setText(getContext().getString(R.string.curves_channel_red));
                break;
            }
            case R.id.curve_menu_green: {
                setText(getContext().getString(R.string.curves_channel_green));
                break;
            }
            case R.id.curve_menu_blue: {
                setText(getContext().getString(R.string.curves_channel_blue));
                break;
            }
        }
        invalidate();
    }

    public FramedTextButton(Context context) {
        this(context, null);
    }

    public FramedTextButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (attrs == null) {
            return;
        }
        TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ImageButtonTitle);

        mText = a.getString(R.styleable.ImageButtonTitle_android_text);
    }

    public String getText(){
        return mText;
    }

    @Override
    public void onDraw(Canvas canvas) {
        gPaint.setARGB(96, 255, 255, 255);
        gPaint.setStrokeWidth(2);
        gPaint.setStyle(Paint.Style.STROKE);
        int w = getWidth();
        int h = getHeight();
        canvas.drawRect(mTextPadding, mTextPadding, w - mTextPadding,
                h - mTextPadding, gPaint);
        gPath.reset();
        gPath.moveTo(w - mTextPadding - mTrianglePadding - mTriangleSize,
                     h - mTextPadding - mTrianglePadding);
        gPath.lineTo(w - mTextPadding - mTrianglePadding,
                     h - mTextPadding - mTrianglePadding - mTriangleSize);
        gPath.lineTo(w - mTextPadding - mTrianglePadding,
                     h - mTextPadding - mTrianglePadding);
        gPath.close();
        gPaint.setARGB(128, 255, 255, 255);
        gPaint.setStrokeWidth(1);
        gPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawPath(gPath, gPaint);
        if (mText != null) {
            gPaint.reset();
            gPaint.setARGB(255, 255, 255, 255);
            gPaint.setTextSize(mTextSize);
            float textWidth = gPaint.measureText(mText);
            Rect bounds = new Rect();
            gPaint.getTextBounds(mText, 0, mText.length(), bounds);
            int x = (int) ((w - textWidth) / 2);
            int y = (h + bounds.height()) / 2;

            canvas.drawText(mText, x, y, gPaint);
        }
    }

}
