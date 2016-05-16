/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.gallery3d.filtershow.colorpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.android.gallery3d.R;

import java.util.ArrayList;

public class ColorValueView extends View implements ColorListener {

    private float mRadius;
    private float mWidth;
    private Paint mBarPaint1;
    private Paint mLinePaint1;
    private Paint mLinePaint2;
    private float mHeight;
    private int mBgcolor = 0;
    private Paint mDotPaint;
    private float dotRadus;
    private float mBorder;

    private float[] mHSVO = new float[4];
    private int mSliderColor;
    private float mDotX;
    private float mDotY = mBorder;
    private final static float DOT_SIZE = ColorRectView.DOT_SIZE;
    private final static float BORDER_SIZE = ColorRectView.DOT_SIZE;

    private ArrayList<ColorListener> mColorListeners = new ArrayList<ColorListener>();

    public ColorValueView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        float mDpToPix = metrics.density;
        dotRadus = DOT_SIZE * mDpToPix;
        mBorder = BORDER_SIZE * mDpToPix;

        mBarPaint1 = new Paint();

        mDotPaint = new Paint();

        mDotPaint.setStyle(Paint.Style.FILL);
        mDotPaint.setColor(ctx.getResources().getColor(R.color.slider_dot_color));

        mBarPaint1.setStyle(Paint.Style.FILL);

        mLinePaint1 = new Paint();
        mLinePaint1.setColor(Color.GRAY);
        mLinePaint2 = new Paint();
        mSliderColor = ctx.getResources().getColor(R.color.slider_line_color);
        mLinePaint2.setColor(mSliderColor);
        mLinePaint2.setStrokeWidth(4);
    }

    public boolean onDown(MotionEvent e) {
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        float ox = mDotX;
        float oy = mDotY;

        float x = event.getX();
        float y = event.getY();

        mDotY = y;

        if (mDotY < mBorder) {
            mDotY = mBorder;
        }

        if (mDotY > mHeight - mBorder) {
            mDotY = mHeight - mBorder;
        }
        mHSVO[2] = (mDotY - mBorder) / (mHeight - mBorder * 2);
        notifyColorListeners(mHSVO);
        setupButton();
        invalidate((int) (ox - dotRadus), (int) (oy - dotRadus), (int) (ox + dotRadus),
                (int) (oy + dotRadus));
        invalidate((int) (mDotX - dotRadus), (int) (mDotY - dotRadus), (int) (mDotX + dotRadus),
                (int) (mDotY + dotRadus));

        return true;
    }

    private void setupButton() {
        float pos = mHSVO[2] * (mHeight - mBorder * 2);
        mDotY = pos + mBorder;

        int[] colors3 = new int[] {
                mSliderColor, mSliderColor, 0x66000000, 0 };
        RadialGradient g = new RadialGradient(mDotX, mDotY, dotRadus, colors3, new float[] {
        0, .3f, .31f, 1 }, Shader.TileMode.CLAMP);
        mDotPaint.setShader(g);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        mDotX = mWidth / 2;
        updatePaint();
        setupButton();
    }

    private void updatePaint() {
        float[] hsv = new float[] {
                mHSVO[0], mHSVO[1], 0f };
        int color1 = Color.HSVToColor(hsv);
        hsv[2] = 1;
        int color2 = Color.HSVToColor(hsv);

        Shader sg = new LinearGradient(mBorder, mBorder, mBorder, mHeight - mBorder, color1, color2,
                Shader.TileMode.CLAMP);
        mBarPaint1.setShader(sg);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mBgcolor);
        canvas.drawRect(mBorder, mBorder, mWidth - mBorder, mHeight - mBorder, mBarPaint1);
        canvas.drawLine(mDotX, mDotY, mDotX, mHeight - mBorder, mLinePaint2);
        canvas.drawLine(mDotX, mBorder, mDotX, mDotY, mLinePaint1);
        if (mDotX != Float.NaN) {
            canvas.drawCircle(mDotX, mDotY, dotRadus, mDotPaint);
        }
    }

    @Override
    public void setColor(float[] hsvo) {
        System.arraycopy(hsvo, 0, mHSVO, 0, mHSVO.length);

        float oy = mDotY;
        updatePaint();
        setupButton();
        invalidate();

    }

    public void notifyColorListeners(float[] hsv) {
        for (ColorListener l : mColorListeners) {
            l.setColor(hsv);
        }
    }

    public void addColorListener(ColorListener l) {
        mColorListeners.add(l);
    }

    public void removeColorListener(ColorListener l) {
        mColorListeners.remove(l);
    }
}
