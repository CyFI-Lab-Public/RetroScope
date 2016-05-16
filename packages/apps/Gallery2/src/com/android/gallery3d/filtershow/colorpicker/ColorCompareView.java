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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.android.gallery3d.R;

import java.util.ArrayList;

public class ColorCompareView extends View implements ColorListener {

    private float mRadius;
    private float mWidth;
    private Paint mBarPaint1;
    private Paint mOrigBarPaint1;
    private Paint mCheckPaint;

    private float mHeight;

    private int mBgcolor = 0;

    private float mBorder;

    private float[] mHSVO = new float[4];
    private float[] mOrigHSVO = new float[4];
    private Path mRegion;
    private Path mOrigRegion;

    public final static float BORDER_SIZE = 0;
    private int mCheckDim = 8;

    public ColorCompareView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
        float mDpToPix = metrics.density;
        mBorder = BORDER_SIZE * mDpToPix;
        mBarPaint1 = new Paint();
        mOrigBarPaint1 = new Paint();
        Resources res = ctx.getResources();
        mCheckDim = res.getDimensionPixelSize(R.dimen.draw_color_check_dim);
        mBarPaint1.setStyle(Paint.Style.FILL);
        mOrigBarPaint1.setStyle(Paint.Style.FILL);

        makeCheckPaint();
    }

    private void makeCheckPaint() {
        int imgdim = mCheckDim * 2;
        int[] colors = new int[imgdim * imgdim];
        for (int i = 0; i < colors.length; i++) {
            int y = i / (imgdim * mCheckDim);
            int x = (i / mCheckDim) % 2;
            colors[i] = (x == y) ? 0xFFAAAAAA : 0xFF444444;
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, imgdim, imgdim, Bitmap.Config.ARGB_8888);
        BitmapShader bs = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        mCheckPaint = new Paint();
        mCheckPaint.setShader(bs);
    }

    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        if (x> mWidth-2*mHeight) {
            resetToOriginal();
        }
        return true;
    }

    public void resetToOriginal(){
        System.arraycopy(mOrigHSVO, 0, mHSVO, 0, mOrigHSVO.length);
        updatePaint();
        notifyColorListeners(mHSVO);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        updatePaint();
    }

    private void updatePaint() {
        int color = Color.HSVToColor((int) (mHSVO[3] * 255), mHSVO);
        mBarPaint1.setColor(color);
        int origColor = Color.HSVToColor((int) (mOrigHSVO[3] * 255), mOrigHSVO);
        mOrigBarPaint1.setColor(origColor);
        mOrigRegion = new Path();
        mOrigRegion.moveTo(mWidth, 0);
        mOrigRegion.lineTo(mWidth, mHeight);
        mOrigRegion.lineTo(mWidth - mHeight * 2, mHeight);
        mOrigRegion.lineTo(mWidth - mHeight, 0);

        mRegion = new Path();
        mRegion.moveTo(0, 0);
        mRegion.lineTo(mWidth - mHeight, 0);
        mRegion.lineTo(mWidth - mHeight * 2, mHeight);
        mRegion.lineTo(0, mHeight);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mBgcolor);
        canvas.drawRect(mBorder, 0, mWidth, mHeight, mCheckPaint);
        canvas.drawPath(mRegion, mBarPaint1);
        canvas.drawPath(mOrigRegion, mOrigBarPaint1);
    }

    public void setOrigColor(float[] hsv) {
        System.arraycopy(hsv, 0, mOrigHSVO, 0, mOrigHSVO.length);
        int color2 = Color.HSVToColor((int) (mOrigHSVO[3] * 255), mOrigHSVO);
        mOrigBarPaint1.setColor(color2);
        updatePaint();
    }

    @Override
    public void setColor(float[] hsv) {
        System.arraycopy(hsv, 0, mHSVO, 0, mHSVO.length);
        updatePaint();
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

