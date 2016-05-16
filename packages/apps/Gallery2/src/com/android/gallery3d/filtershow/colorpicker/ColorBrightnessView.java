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
import android.graphics.BitmapShader;
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
import java.util.Arrays;

public class ColorBrightnessView extends View implements ColorListener {

    private float mRadius;
    private float mWidth;
    private Paint mBarPaint1;
    private Paint mLinePaint1;
    private Paint mLinePaint2;
    private Paint mCheckPaint;

    private float mHeight;
    private Paint mDotPaint;
    private int mBgcolor = 0;

    private float mDotRadius;
    private float mBorder;

    private float[] mHSVO = new float[4];
    private int mSliderColor;
    private float mDotX = mBorder;
    private float mDotY = mBorder;
    private final static float DOT_SIZE = ColorRectView.DOT_SIZE;
    public final static float BORDER_SIZE = 20;;

    private ArrayList<ColorListener> mColorListeners = new ArrayList<ColorListener>();

    public ColorBrightnessView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        float mDpToPix = metrics.density;
        mDotRadius = DOT_SIZE * mDpToPix;
        mBorder = BORDER_SIZE * mDpToPix;
        mBarPaint1 = new Paint();

        mDotPaint = new Paint();

        mDotPaint.setStyle(Paint.Style.FILL);
        mDotPaint.setColor(ctx.getResources().getColor(R.color.slider_dot_color));
        mSliderColor = ctx.getResources().getColor(R.color.slider_line_color);

        mBarPaint1.setStyle(Paint.Style.FILL);

        mLinePaint1 = new Paint();
        mLinePaint1.setColor(Color.GRAY);
        mLinePaint2 = new Paint();
        mLinePaint2.setColor(mSliderColor);
        mLinePaint2.setStrokeWidth(4);

        int[] colors = new int[16 * 16];
        for (int i = 0; i < colors.length; i++) {
            int y = i / (16 * 8);
            int x = (i / 8) % 2;
            colors[i] = (x == y) ? 0xFFAAAAAA : 0xFF444444;
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, 16, 16, Bitmap.Config.ARGB_8888);
        BitmapShader bs = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        mCheckPaint = new Paint();
        mCheckPaint.setShader(bs);
    }

    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float ox = mDotX;
        float oy = mDotY;

        float x = event.getX();
        float y = event.getY();

        mDotX = x;

        if (mDotX < mBorder) {
            mDotX = mBorder;
        }

        if (mDotX > mWidth - mBorder) {
            mDotX = mWidth - mBorder;
        }
        mHSVO[3] = (mDotX - mBorder) / (mWidth - mBorder * 2);
        notifyColorListeners(mHSVO);
        setupButton();
        invalidate((int) (ox - mDotRadius), (int) (oy - mDotRadius), (int) (ox + mDotRadius),
                (int) (oy + mDotRadius));
        invalidate(
                (int) (mDotX - mDotRadius), (int) (mDotY - mDotRadius), (int) (mDotX + mDotRadius),
                (int) (mDotY + mDotRadius));

        return true;
    }

    private void setupButton() {
        float pos = mHSVO[3] * (mWidth - mBorder * 2);
        mDotX = pos + mBorder;

        int[] colors3 = new int[] {
        mSliderColor, mSliderColor, 0x66000000, 0 };
        RadialGradient g = new RadialGradient(mDotX, mDotY, mDotRadius, colors3, new float[] {
        0, .3f, .31f, 1 }, Shader.TileMode.CLAMP);
        mDotPaint.setShader(g);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        mDotY = mHeight / 2;
        updatePaint();
        setupButton();
    }

    private void updatePaint() {
        float[] hsvo = Arrays.copyOf(mHSVO, 4);
        hsvo[2] = 1;
        hsvo[1] = 1;
        hsvo[3] = 1;
        int color2 = Color.HSVToColor(hsvo);
        hsvo[2] = 0;
        int color1 = Color.HSVToColor(hsvo);

        Shader sg = new LinearGradient(
                mBorder, mBorder, mWidth - mBorder, mBorder,
                color1, color2, Shader.TileMode.CLAMP);
        mBarPaint1.setShader(sg);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mBgcolor);
        canvas.drawRect(mBorder, mBorder, mWidth - mBorder, mHeight - mBorder, mCheckPaint);
        canvas.drawRect(mBorder, mBorder, mWidth - mBorder, mHeight - mBorder, mBarPaint1);
        canvas.drawLine(mDotX, mDotY, mWidth - mBorder, mDotY, mLinePaint1);
        canvas.drawLine(mBorder, mDotY, mDotX, mDotY, mLinePaint2);
        if (mDotX != Float.NaN) {
            canvas.drawCircle(mDotX, mDotY, mDotRadius, mDotPaint);
        }
    }

    @Override
    public void setColor(float[] hsv) {
        System.arraycopy(hsv, 0, mHSVO, 0, mHSVO.length);

        float oy = mDotY;

        updatePaint();
        setupButton();
        invalidate();
    }

    public void notifyColorListeners(float[] hsvo) {
        for (ColorListener l : mColorListeners) {
            l.setColor(hsvo);
        }
    }

    public void addColorListener(ColorListener l) {
        mColorListeners.add(l);
    }

    public void removeColorListener(ColorListener l) {
        mColorListeners.remove(l);
    }
}
