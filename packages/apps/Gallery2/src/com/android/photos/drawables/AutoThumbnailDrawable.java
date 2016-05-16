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

package com.android.photos.drawables;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.android.photos.data.GalleryBitmapPool;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AutoThumbnailDrawable<T> extends Drawable {

    private static final String TAG = "AutoThumbnailDrawable";

    private static ExecutorService sThreadPool = Executors.newSingleThreadExecutor();
    private static GalleryBitmapPool sBitmapPool = GalleryBitmapPool.getInstance();
    private static byte[] sTempStorage = new byte[64 * 1024];

    // UI thread only
    private Paint mPaint = new Paint();
    private Matrix mDrawMatrix = new Matrix();

    // Decoder thread only
    private BitmapFactory.Options mOptions = new BitmapFactory.Options();

    // Shared, guarded by mLock
    private Object mLock = new Object();
    private Bitmap mBitmap;
    protected T mData;
    private boolean mIsQueued;
    private int mImageWidth, mImageHeight;
    private Rect mBounds = new Rect();
    private int mSampleSize = 1;

    public AutoThumbnailDrawable() {
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mDrawMatrix.reset();
        mOptions.inTempStorage = sTempStorage;
    }

    protected abstract byte[] getPreferredImageBytes(T data);
    protected abstract InputStream getFallbackImageStream(T data);
    protected abstract boolean dataChangedLocked(T data);

    public void setImage(T data, int width, int height) {
        if (!dataChangedLocked(data)) return;
        synchronized (mLock) {
            mImageWidth = width;
            mImageHeight = height;
            mData = data;
            setBitmapLocked(null);
            refreshSampleSizeLocked();
        }
        invalidateSelf();
    }

    private void setBitmapLocked(Bitmap b) {
        if (b == mBitmap) {
            return;
        }
        if (mBitmap != null) {
            sBitmapPool.put(mBitmap);
        }
        mBitmap = b;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        synchronized (mLock) {
            mBounds.set(bounds);
            if (mBounds.isEmpty()) {
                mBitmap = null;
            } else {
                refreshSampleSizeLocked();
                updateDrawMatrixLocked();
            }
        }
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mBitmap != null) {
            canvas.save();
            canvas.clipRect(mBounds);
            canvas.concat(mDrawMatrix);
            canvas.drawBitmap(mBitmap, 0, 0, mPaint);
            canvas.restore();
        } else {
            // TODO: Draw placeholder...?
        }
    }

    private void updateDrawMatrixLocked() {
        if (mBitmap == null || mBounds.isEmpty()) {
            mDrawMatrix.reset();
            return;
        }

        float scale;
        float dx = 0, dy = 0;

        int dwidth = mBitmap.getWidth();
        int dheight = mBitmap.getHeight();
        int vwidth = mBounds.width();
        int vheight = mBounds.height();

        // Calculates a matrix similar to ScaleType.CENTER_CROP
        if (dwidth * vheight > vwidth * dheight) {
            scale = (float) vheight / (float) dheight;
            dx = (vwidth - dwidth * scale) * 0.5f;
        } else {
            scale = (float) vwidth / (float) dwidth;
            dy = (vheight - dheight * scale) * 0.5f;
        }
        if (scale < .8f) {
            Log.w(TAG, "sample size was too small! Overdrawing! " + scale + ", " + mSampleSize);
        } else if (scale > 1.5f) {
            Log.w(TAG, "Potential quality loss! " + scale + ", " + mSampleSize);
        }

        mDrawMatrix.setScale(scale, scale);
        mDrawMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    private int calculateSampleSizeLocked(int dwidth, int dheight) {
        float scale;

        int vwidth = mBounds.width();
        int vheight = mBounds.height();

        // Inverse of updateDrawMatrixLocked
        if (dwidth * vheight > vwidth * dheight) {
            scale = (float) dheight / (float) vheight;
        } else {
            scale = (float) dwidth / (float) vwidth;
        }
        int result = Math.round(scale);
        return result > 0 ? result : 1;
    }

    private void refreshSampleSizeLocked() {
        if (mBounds.isEmpty() || mImageWidth == 0 || mImageHeight == 0) {
            return;
        }

        int sampleSize = calculateSampleSizeLocked(mImageWidth, mImageHeight);
        if (sampleSize != mSampleSize || mBitmap == null) {
            mSampleSize = sampleSize;
            loadBitmapLocked();
        }
    }

    private void loadBitmapLocked() {
        if (!mIsQueued && !mBounds.isEmpty()) {
            unscheduleSelf(mUpdateBitmap);
            sThreadPool.execute(mLoadBitmap);
            mIsQueued = true;
        }
    }

    public float getAspectRatio() {
        return (float) mImageWidth / (float) mImageHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return -1;
    }

    @Override
    public int getOpacity() {
        Bitmap bm = mBitmap;
        return (bm == null || bm.hasAlpha() || mPaint.getAlpha() < 255) ?
                PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    @Override
    public void setAlpha(int alpha) {
        int oldAlpha = mPaint.getAlpha();
        if (alpha != oldAlpha) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    private final Runnable mLoadBitmap = new Runnable() {
        @Override
        public void run() {
            T data;
            synchronized (mLock) {
                data = mData;
            }
            int preferredSampleSize = 1;
            byte[] preferred = getPreferredImageBytes(data);
            boolean hasPreferred = (preferred != null && preferred.length > 0);
            if (hasPreferred) {
                mOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(preferred, 0, preferred.length, mOptions);
                mOptions.inJustDecodeBounds = false;
            }
            int sampleSize, width, height;
            synchronized (mLock) {
                if (dataChangedLocked(data)) {
                    return;
                }
                width = mImageWidth;
                height = mImageHeight;
                if (hasPreferred) {
                    preferredSampleSize = calculateSampleSizeLocked(
                            mOptions.outWidth, mOptions.outHeight);
                }
                sampleSize = calculateSampleSizeLocked(width, height);
                mIsQueued = false;
            }
            Bitmap b = null;
            InputStream is = null;
            try {
                if (hasPreferred) {
                    mOptions.inSampleSize = preferredSampleSize;
                    mOptions.inBitmap = sBitmapPool.get(
                            mOptions.outWidth / preferredSampleSize,
                            mOptions.outHeight / preferredSampleSize);
                    b = BitmapFactory.decodeByteArray(preferred, 0, preferred.length, mOptions);
                    if (mOptions.inBitmap != null && b != mOptions.inBitmap) {
                        sBitmapPool.put(mOptions.inBitmap);
                        mOptions.inBitmap = null;
                    }
                }
                if (b == null) {
                    is = getFallbackImageStream(data);
                    mOptions.inSampleSize = sampleSize;
                    mOptions.inBitmap = sBitmapPool.get(width / sampleSize, height / sampleSize);
                    b = BitmapFactory.decodeStream(is, null, mOptions);
                    if (mOptions.inBitmap != null && b != mOptions.inBitmap) {
                        sBitmapPool.put(mOptions.inBitmap);
                        mOptions.inBitmap = null;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Failed to fetch bitmap", e);
                return;
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (Exception e) {}
                if (b != null) {
                    synchronized (mLock) {
                        if (!dataChangedLocked(data)) {
                            setBitmapLocked(b);
                            scheduleSelf(mUpdateBitmap, 0);
                        }
                    }
                }
            }
        }
    };

    private final Runnable mUpdateBitmap = new Runnable() {
        @Override
        public void run() {
            synchronized (AutoThumbnailDrawable.this) {
                updateDrawMatrixLocked();
                invalidateSelf();
            }
        }
    };

}
