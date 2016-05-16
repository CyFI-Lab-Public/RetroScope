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
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.android.gallery3d.R;

import java.util.ArrayList;

public class ColorRectView extends View implements ColorListener {
    private float mDpToPix;
    private float mRadius = 80;
    private float mCtrY = 100;
    private Paint mWheelPaint1;
    private Paint mWheelPaint2;
    private Paint mWheelPaint3;
    private float mCtrX = 100;
    private Paint mDotPaint;
    private float mDotRadus;
    private float mBorder;
    private int mBgcolor = 0;
    private float mDotX = Float.NaN;
    private float mDotY;
    private int mSliderColor = 0xFF33B5E5;
    private float[] mHSVO = new float[4];
    private int[] mColors = new int[] {
            0xFFFF0000,// red
            0xFFFFFF00,// yellow
            0xFF00FF00,// green
            0xFF00FFFF,// cyan
            0xFF0000FF,// blue
            0xFFFF00FF,// magenta
            0xFFFF0000,// red
    };
    private int mWidth;
    private int mHeight;
    public final static float DOT_SIZE = 20;
    public final static float BORDER_SIZE = 10;

    public ColorRectView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);

        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        mDpToPix = metrics.density;
        mDotRadus = DOT_SIZE * mDpToPix;
        mBorder = BORDER_SIZE * mDpToPix;

        mWheelPaint1 = new Paint();
        mWheelPaint2 = new Paint();
        mWheelPaint3 = new Paint();
        mDotPaint = new Paint();

        mDotPaint.setStyle(Paint.Style.FILL);
        mDotPaint.setColor(ctx.getResources().getColor(R.color.slider_dot_color));
        mSliderColor = ctx.getResources().getColor(R.color.slider_line_color);
        mWheelPaint1.setStyle(Paint.Style.FILL);
        mWheelPaint2.setStyle(Paint.Style.FILL);
        mWheelPaint3.setStyle(Paint.Style.FILL);
    }

    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        invalidate((int) (mDotX - mDotRadus), (int) (mDotY - mDotRadus), (int) (mDotX + mDotRadus),
                (int) (mDotY + mDotRadus));
        float x = event.getX();
        float y = event.getY();

        x = Math.max(Math.min(x, mWidth - mBorder), mBorder);
        y = Math.max(Math.min(y, mHeight - mBorder), mBorder);
        mDotX = x;
        mDotY = y;
        float sat = 1 - (mDotY - mBorder) / (mHeight - 2 * mBorder);
        if (sat > 1) {
            sat = 1;
        }

        double hue = Math.PI * 2 * (mDotX - mBorder) / (mHeight - 2 * mBorder);
        mHSVO[0] = ((float) Math.toDegrees(hue) + 360) % 360;
        mHSVO[1] = sat;
        notifyColorListeners(mHSVO);
        updateDotPaint();
        invalidate((int) (mDotX - mDotRadus), (int) (mDotY - mDotRadus), (int) (mDotX + mDotRadus),
                (int) (mDotY + mDotRadus));

        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        mCtrY = h / 2f;
        mCtrX = w / 2f;
        mRadius = Math.min(mCtrY, mCtrX) - 2 * mBorder;
        setUpColorPanel();
    }

    private void setUpColorPanel() {
        float val = mHSVO[2];
        int v = 0xFF000000 | 0x10101 * (int) (val * 0xFF);
        int[] colors = new int[] {
                0x0000000, v };
        int[] colors2 = new int[] {
                0x0000000, 0xFF000000 };
        int[] wheelColor = new int[mColors.length];
        float[] hsv = new float[3];
        for (int i = 0; i < wheelColor.length; i++) {
            Color.colorToHSV(mColors[i], hsv);
            hsv[2] = mHSVO[2];
            wheelColor[i] = Color.HSVToColor(hsv);
        }
        updateDot();
        updateDotPaint();
        SweepGradient sg = new SweepGradient(mCtrX, mCtrY, wheelColor, null);
        LinearGradient lg = new LinearGradient(
                mBorder, 0, mWidth - mBorder, 0, wheelColor, null, Shader.TileMode.CLAMP);

        mWheelPaint1.setShader(lg);
        LinearGradient rg = new LinearGradient(
                0, mBorder, 0, mHeight - mBorder, colors, null, Shader.TileMode.CLAMP);
        mWheelPaint2.setShader(rg);
        LinearGradient rg2 = new LinearGradient(
                0, mBorder, 0, mHeight - mBorder, colors2, null, Shader.TileMode.CLAMP);
        mWheelPaint3.setShader(rg2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mBgcolor);
        RectF rect = new RectF();
        rect.left = mBorder;
        rect.right = mWidth - mBorder;
        rect.top = mBorder;
        rect.bottom = mHeight - mBorder;

        canvas.drawRect(rect, mWheelPaint1);
        canvas.drawRect(rect, mWheelPaint3);
        canvas.drawRect(rect, mWheelPaint2);

        if (mDotX != Float.NaN) {

            canvas.drawCircle(mDotX, mDotY, mDotRadus, mDotPaint);
        }
    }

    private void updateDot() {

        double hue = mHSVO[0];
        double sat = mHSVO[1];

        mDotX = (float) (mBorder + (mHeight - 2 * mBorder) * Math.toRadians(hue) / (Math.PI * 2));
        mDotY = (float) ((1 - sat) * (mHeight - 2 * mBorder) + mBorder);

    }

    private void updateDotPaint() {
        int[] colors3 = new int[] {
                mSliderColor, mSliderColor, 0x66000000, 0 };
        RadialGradient g = new RadialGradient(mDotX, mDotY, mDotRadus, colors3, new float[] {
                0, .3f, .31f, 1 }, Shader.TileMode.CLAMP);
        mDotPaint.setShader(g);

    }

    @Override
    public void setColor(float[] hsvo) {
        System.arraycopy(hsvo, 0, mHSVO, 0, mHSVO.length);

        setUpColorPanel();
        invalidate();

        updateDot();
        updateDotPaint();

    }

    ArrayList<ColorListener> mColorListeners = new ArrayList<ColorListener>();

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
