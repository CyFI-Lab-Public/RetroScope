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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.android.gallery3d.R;

import java.util.ArrayList;

public class ColorSVRectView extends View implements ColorListener {
    private float mDpToPix;

    private float mCtrY = 100;
    private Paint mPaint1;

    private float mCtrX = 100;
    private Paint mDotPaint = new Paint();
    private float mDotRadus;
    private float mBorder;

    private float mDotX = Float.NaN;
    private float mDotY;
    private int mSliderColor = 0xFF33B5E5;
    private float[] mHSVO = new float[]{0, 1, 1, 1};
    RectF mRect = new RectF();

    private int mWidth;
    private int mHeight;
    public final static float DOT_SIZE = 20;
    public final static float BORDER_SIZE = 20;
    Bitmap mBitmap;

    public ColorSVRectView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);

        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        mDpToPix = metrics.density;
        mDotRadus = DOT_SIZE * mDpToPix;
        mBorder = BORDER_SIZE * mDpToPix;

        mPaint1 = new Paint();


        mDotPaint.setStyle(Paint.Style.FILL);
        if (isInEditMode()) {
            mDotPaint.setColor(0x646464);
            mSliderColor = 0x888888;
        } else {
            mDotPaint.setColor(ctx.getResources().getColor(R.color.slider_dot_color));
            mSliderColor = ctx.getResources().getColor(R.color.slider_line_color);
        }
        mPaint1.setStyle(Paint.Style.FILL);
        mPaint1.setAntiAlias(true);
        mPaint1.setFilterBitmap(true);

        mBitmap = Bitmap.createBitmap(64, 46, Bitmap.Config.ARGB_8888);
        fillBitmap();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    void fillBitmap() {
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        int[] buff = new int[w * h];
        float[] hsv = new float[3];
        hsv[0] = mHSVO[0];
        for (int i = 0; i < w * h; i++) {
            float sat = (i % w) / (float) w;
            float val = (w - i / w) / (float) w;
            hsv[1] = sat;
            hsv[2] = val;
            buff[i] = Color.HSVToColor(hsv);
        }
        mBitmap.setPixels(buff, 0, w, 0, 0, w, h);
    }

    private void setUpColorPanel() {
        updateDot();
        updateDotPaint();
        fillBitmap();

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect r = canvas.getClipBounds();
        mRect.set(r);
        mRect.top += mBorder;
        mRect.bottom -= mBorder;
        mRect.left += mBorder;
        mRect.right -= mBorder;
        canvas.drawBitmap(mBitmap, null, mRect, mPaint1);

        if (mDotX != Float.NaN) {

            canvas.drawCircle(mDotX, mDotY, mDotRadus, mDotPaint);
        }
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

        float value = (mDotX - mBorder) / (mHeight - 2 * mBorder);
        mHSVO[2] = sat;
        mHSVO[1] = value;
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

        setUpColorPanel();
    }


    private void updateDot() {

        double hue = mHSVO[0];
        double sat = mHSVO[1];
        double val = mHSVO[2];
        double opc = mHSVO[3];

        mDotX = (float) (mBorder + (mHeight - 2 * mBorder) * sat);
        mDotY = (float) ((1 - val) * (mHeight - 2 * mBorder) + mBorder);

    }

    private void updateDotPaint() {
        int[] colors3 = new int[]{
                mSliderColor, mSliderColor, 0x66000000, 0};
        RadialGradient g = new RadialGradient(mDotX, mDotY, mDotRadus, colors3, new float[]{
                0, .3f, .31f, 1}, Shader.TileMode.CLAMP);
        mDotPaint.setShader(g);

    }

    @Override
    public void setColor(float[] hsvo) {
        if (hsvo[0] == mHSVO[0]
                && hsvo[1] == mHSVO[1]
                && hsvo[2] == mHSVO[2]) {
            mHSVO[3] = hsvo[3]; // we don't update if color stays the same
            return;
        }
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
