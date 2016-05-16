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
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.android.gallery3d.R;

import java.util.ArrayList;

public class ColorHueView extends View implements ColorListener {

    private float mWidth;

    private Paint mLinePaint1;
    private Paint mLinePaint2;
    private Paint mPaint = new Paint();
    private float mHeight;
    private Paint mDotPaint;
    private int mBgcolor = 0;
    Bitmap mBitmap;
    private float mDotRadius;
    private float mBorder;

    private float[] mHSVO = {0.f,0.f,0.f,0.f};
    private int mSliderColor;
    private float mDotX = mBorder;
    private float mDotY = mBorder;

    public final static float DOT_SIZE = 20;
    public final static float BORDER_SIZE = 20;
    RectF mRect = new RectF();
    int[] mTmpBuff;
    float[] mTmpHSV = new float[3];
    private Paint mCheckPaint;

    public ColorHueView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        float mDpToPix = metrics.density;
        mDotRadius = DOT_SIZE * mDpToPix;
        mBorder = BORDER_SIZE * mDpToPix;

        mDotPaint = new Paint();

        mDotPaint.setStyle(Paint.Style.FILL);
        mDotPaint.setColor(ctx.getResources().getColor(R.color.slider_dot_color));
        mSliderColor = ctx.getResources().getColor(R.color.slider_line_color);


        mLinePaint1 = new Paint();
        mLinePaint1.setColor(Color.GRAY);
        mLinePaint2 = new Paint();
        mLinePaint2.setColor(mSliderColor);
        mLinePaint2.setStrokeWidth(4);

        mBitmap = Bitmap.createBitmap(256, 2, Bitmap.Config.ARGB_8888);
        mTmpBuff = new int[mBitmap.getWidth() * mBitmap.getHeight()];
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        fillBitmap();
        makeCheckPaint();
    }

    void fillBitmap() {
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();

        for (int x = 0; x < w; x++) {
            float hue = 360 * (x) / (float) w;

            mTmpHSV[0] = hue;
            mTmpHSV[1] = 1;
            mTmpHSV[2] = 1;
            int color = Color.HSVToColor(mTmpHSV);
            mTmpBuff[x] = color;
            mTmpBuff[x + w] = color;

        }

        mBitmap.setPixels(mTmpBuff, 0, w, 0, 0, w, h);
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
        mHSVO[0] = 360 * (mDotX - mBorder) / (mWidth - mBorder * 2);
        notifyColorListeners(mHSVO);
        setupButton();
        fillBitmap();
//        invalidate((int) (ox - mDotRadius), (int) (oy - mDotRadius), (int) (ox + mDotRadius),
//                (int) (oy + mDotRadius));
//        invalidate(
//                (int) (mDotX - mDotRadius), (int) (mDotY - mDotRadius), (int) (mDotX + mDotRadius),
//                (int) (mDotY + mDotRadius));
        invalidate();

        return true;
    }

    private void setupButton() {
        float pos = mHSVO[0] / 360 * (mWidth - mBorder * 2);
        mDotX = pos + mBorder;

        int[] colors3 = new int[]{
                mSliderColor, mSliderColor, 0x66000000, 0};
        RadialGradient g = new RadialGradient(mDotX, mDotY, mDotRadius, colors3, new float[]{
                0, .3f, .31f, 1}, Shader.TileMode.CLAMP);
        mDotPaint.setShader(g);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        mDotY = mHeight / 2;
        setupButton();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mBgcolor);

        mRect.left = mBorder;
        mRect.right = mWidth - mBorder;
        mRect.top = 0;
        mRect.bottom = mHeight;
        canvas.drawRect(mRect,mCheckPaint);
        canvas.drawBitmap(mBitmap, null, mRect, mPaint);


        canvas.drawLine(mDotX, mDotY, mWidth - mBorder, mDotY, mLinePaint1);
        canvas.drawLine(mBorder, mDotY, mDotX, mDotY, mLinePaint2);
        if (mDotX != Float.NaN) {
            canvas.drawCircle(mDotX, mDotY, mDotRadius, mDotPaint);
        }
    }

    private void makeCheckPaint(){
        int block = 16;
        int checkdim = block*2;
        int[] colors = new int[checkdim * checkdim];
        for (int i = 0; i < colors.length; i++) {
            int y = i / (checkdim * block);
            int x = (i / block) % 2;
            colors[i] = (x == y) ? 0xFFAAAAAA : 0xFF444444;
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, 16, 16, Bitmap.Config.ARGB_8888);
        BitmapShader bs = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        mCheckPaint = new Paint();
        mCheckPaint.setShader(bs);
    }

    @Override
    public void setColor(float[] hsv) {
        System.arraycopy(hsv, 0, mHSVO, 0, mHSVO.length);
        fillBitmap();
        setupButton();
        invalidate();
    }

    ArrayList<ColorListener> mColorListeners = new ArrayList<ColorListener>();

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
