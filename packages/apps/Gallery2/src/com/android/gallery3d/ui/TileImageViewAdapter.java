/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.photos.data.GalleryBitmapPool;

public class TileImageViewAdapter implements TileImageView.TileSource {
    private static final String TAG = "TileImageViewAdapter";
    protected ScreenNail mScreenNail;
    protected boolean mOwnScreenNail;
    protected BitmapRegionDecoder mRegionDecoder;
    protected int mImageWidth;
    protected int mImageHeight;
    protected int mLevelCount;

    public TileImageViewAdapter() {
    }

    public synchronized void clear() {
        mScreenNail = null;
        mImageWidth = 0;
        mImageHeight = 0;
        mLevelCount = 0;
        mRegionDecoder = null;
    }

    // Caller is responsible to recycle the ScreenNail
    public synchronized void setScreenNail(
            ScreenNail screenNail, int width, int height) {
        Utils.checkNotNull(screenNail);
        mScreenNail = screenNail;
        mImageWidth = width;
        mImageHeight = height;
        mRegionDecoder = null;
        mLevelCount = 0;
    }

    public synchronized void setRegionDecoder(BitmapRegionDecoder decoder) {
        mRegionDecoder = Utils.checkNotNull(decoder);
        mImageWidth = decoder.getWidth();
        mImageHeight = decoder.getHeight();
        mLevelCount = calculateLevelCount();
    }

    private int calculateLevelCount() {
        return Math.max(0, Utils.ceilLog2(
                (float) mImageWidth / mScreenNail.getWidth()));
    }

    // Gets a sub image on a rectangle of the current photo. For example,
    // getTile(1, 50, 50, 100, 3, pool) means to get the region located
    // at (50, 50) with sample level 1 (ie, down sampled by 2^1) and the
    // target tile size (after sampling) 100 with border 3.
    //
    // From this spec, we can infer the actual tile size to be
    // 100 + 3x2 = 106, and the size of the region to be extracted from the
    // photo to be 200 with border 6.
    //
    // As a result, we should decode region (50-6, 50-6, 250+6, 250+6) or
    // (44, 44, 256, 256) from the original photo and down sample it to 106.
    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    @Override
    public Bitmap getTile(int level, int x, int y, int tileSize) {
        if (!ApiHelper.HAS_REUSING_BITMAP_IN_BITMAP_REGION_DECODER) {
            return getTileWithoutReusingBitmap(level, x, y, tileSize);
        }

        int t = tileSize << level;

        Rect wantRegion = new Rect(x, y, x + t, y + t);

        boolean needClear;
        BitmapRegionDecoder regionDecoder = null;

        synchronized (this) {
            regionDecoder = mRegionDecoder;
            if (regionDecoder == null) return null;

            // We need to clear a reused bitmap, if wantRegion is not fully
            // within the image.
            needClear = !new Rect(0, 0, mImageWidth, mImageHeight)
                    .contains(wantRegion);
        }

        Bitmap bitmap = GalleryBitmapPool.getInstance().get(tileSize, tileSize);
        if (bitmap != null) {
            if (needClear) bitmap.eraseColor(0);
        } else {
            bitmap = Bitmap.createBitmap(tileSize, tileSize, Config.ARGB_8888);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.ARGB_8888;
        options.inPreferQualityOverSpeed = true;
        options.inSampleSize =  (1 << level);
        options.inBitmap = bitmap;

        try {
            // In CropImage, we may call the decodeRegion() concurrently.
            synchronized (regionDecoder) {
                bitmap = regionDecoder.decodeRegion(wantRegion, options);
            }
        } finally {
            if (options.inBitmap != bitmap && options.inBitmap != null) {
                GalleryBitmapPool.getInstance().put(options.inBitmap);
                options.inBitmap = null;
            }
        }

        if (bitmap == null) {
            Log.w(TAG, "fail in decoding region");
        }
        return bitmap;
    }

    private Bitmap getTileWithoutReusingBitmap(
            int level, int x, int y, int tileSize) {
        int t = tileSize << level;
        Rect wantRegion = new Rect(x, y, x + t, y + t);

        BitmapRegionDecoder regionDecoder;
        Rect overlapRegion;

        synchronized (this) {
            regionDecoder = mRegionDecoder;
            if (regionDecoder == null) return null;
            overlapRegion = new Rect(0, 0, mImageWidth, mImageHeight);
            Utils.assertTrue(overlapRegion.intersect(wantRegion));
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.ARGB_8888;
        options.inPreferQualityOverSpeed = true;
        options.inSampleSize =  (1 << level);
        Bitmap bitmap = null;

        // In CropImage, we may call the decodeRegion() concurrently.
        synchronized (regionDecoder) {
            bitmap = regionDecoder.decodeRegion(overlapRegion, options);
        }

        if (bitmap == null) {
            Log.w(TAG, "fail in decoding region");
        }

        if (wantRegion.equals(overlapRegion)) return bitmap;

        Bitmap result = Bitmap.createBitmap(tileSize, tileSize, Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(bitmap,
                (overlapRegion.left - wantRegion.left) >> level,
                (overlapRegion.top - wantRegion.top) >> level, null);
        return result;
    }


    @Override
    public ScreenNail getScreenNail() {
        return mScreenNail;
    }

    @Override
    public int getImageHeight() {
        return mImageHeight;
    }

    @Override
    public int getImageWidth() {
        return mImageWidth;
    }

    @Override
    public int getLevelCount() {
        return mLevelCount;
    }
}
