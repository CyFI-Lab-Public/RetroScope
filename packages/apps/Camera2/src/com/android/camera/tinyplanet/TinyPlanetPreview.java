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

package com.android.camera.tinyplanet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.concurrent.locks.Lock;

/**
 * Shows a preview of the TinyPlanet on the screen while editing.
 */
public class TinyPlanetPreview extends View {
    /**
     * Classes implementing this interface get informed about changes to the
     * preview size.
     */
    public static interface PreviewSizeListener {
        /**
         * Called when the preview size has changed.
         *
         * @param sizePx the size in pixels of the square preview area
         */
        public void onSizeChanged(int sizePx);
    }

    private Paint mPaint = new Paint();
    private Bitmap mPreview;
    private Lock mLock;
    private PreviewSizeListener mPreviewSizeListener;
    private int mSize = 0;

    public TinyPlanetPreview(Context context) {
        super(context);
    }

    public TinyPlanetPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TinyPlanetPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the bitmap and waits for a draw to happen before returning.
     */
    public void setBitmap(Bitmap preview, Lock lock) {
        mPreview = preview;
        mLock = lock;
        invalidate();
    }

    public void setPreviewSizeChangeListener(PreviewSizeListener listener) {
        mPreviewSizeListener = listener;
        if (mSize > 0) {
            mPreviewSizeListener.onSizeChanged(mSize);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mLock != null && mLock.tryLock()) {
            try {
                if (mPreview != null && !mPreview.isRecycled()) {
                    canvas.drawBitmap(mPreview, 0, 0, mPaint);
                }
            } finally {
                mLock.unlock();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Make sure the view is square
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && mPreviewSizeListener != null) {
            int width = right - left;
            int height = bottom - top;

            // These should be the same as we enforce a square layout, but let's
            // be safe.
            int mSize = Math.min(width, height);

            // Tell the listener about our new size so the renderer can adapt.
            if (mSize > 0 && mPreviewSizeListener != null) {
                mPreviewSizeListener.onSizeChanged(mSize);
            }
        }
    }
}
