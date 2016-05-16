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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.util.LongSparseArray;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.WindowManager;

import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DecodeUtils;
import com.android.photos.data.GalleryBitmapPool;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.UploadedTexture;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.ThreadPool.CancelListener;
import com.android.gallery3d.util.ThreadPool.JobContext;

import java.util.concurrent.atomic.AtomicBoolean;

public class TileImageView extends GLView {
    public static final int SIZE_UNKNOWN = -1;

    @SuppressWarnings("unused")
    private static final String TAG = "TileImageView";
    private static final int UPLOAD_LIMIT = 1;

    // TILE_SIZE must be 2^N
    private static int sTileSize;

    /*
     *  This is the tile state in the CPU side.
     *  Life of a Tile:
     *      ACTIVATED (initial state)
     *              --> IN_QUEUE - by queueForDecode()
     *              --> RECYCLED - by recycleTile()
     *      IN_QUEUE --> DECODING - by decodeTile()
     *               --> RECYCLED - by recycleTile)
     *      DECODING --> RECYCLING - by recycleTile()
     *               --> DECODED  - by decodeTile()
     *               --> DECODE_FAIL - by decodeTile()
     *      RECYCLING --> RECYCLED - by decodeTile()
     *      DECODED --> ACTIVATED - (after the decoded bitmap is uploaded)
     *      DECODED --> RECYCLED - by recycleTile()
     *      DECODE_FAIL -> RECYCLED - by recycleTile()
     *      RECYCLED --> ACTIVATED - by obtainTile()
     */
    private static final int STATE_ACTIVATED = 0x01;
    private static final int STATE_IN_QUEUE = 0x02;
    private static final int STATE_DECODING = 0x04;
    private static final int STATE_DECODED = 0x08;
    private static final int STATE_DECODE_FAIL = 0x10;
    private static final int STATE_RECYCLING = 0x20;
    private static final int STATE_RECYCLED = 0x40;

    private TileSource mModel;
    private ScreenNail mScreenNail;
    protected int mLevelCount;  // cache the value of mScaledBitmaps.length

    // The mLevel variable indicates which level of bitmap we should use.
    // Level 0 means the original full-sized bitmap, and a larger value means
    // a smaller scaled bitmap (The width and height of each scaled bitmap is
    // half size of the previous one). If the value is in [0, mLevelCount), we
    // use the bitmap in mScaledBitmaps[mLevel] for display, otherwise the value
    // is mLevelCount, and that means we use mScreenNail for display.
    private int mLevel = 0;

    // The offsets of the (left, top) of the upper-left tile to the (left, top)
    // of the view.
    private int mOffsetX;
    private int mOffsetY;

    private int mUploadQuota;
    private boolean mRenderComplete;

    private final RectF mSourceRect = new RectF();
    private final RectF mTargetRect = new RectF();

    private final LongSparseArray<Tile> mActiveTiles = new LongSparseArray<Tile>();

    // The following three queue is guarded by TileImageView.this
    private final TileQueue mRecycledQueue = new TileQueue();
    private final TileQueue mUploadQueue = new TileQueue();
    private final TileQueue mDecodeQueue = new TileQueue();

    // The width and height of the full-sized bitmap
    protected int mImageWidth = SIZE_UNKNOWN;
    protected int mImageHeight = SIZE_UNKNOWN;

    protected int mCenterX;
    protected int mCenterY;
    protected float mScale;
    protected int mRotation;

    // Temp variables to avoid memory allocation
    private final Rect mTileRange = new Rect();
    private final Rect mActiveRange[] = {new Rect(), new Rect()};

    private final TileUploader mTileUploader = new TileUploader();
    private boolean mIsTextureFreed;
    private Future<Void> mTileDecoder;
    private final ThreadPool mThreadPool;
    private boolean mBackgroundTileUploaded;

    public static interface TileSource {
        public int getLevelCount();
        public ScreenNail getScreenNail();
        public int getImageWidth();
        public int getImageHeight();

        // The tile returned by this method can be specified this way: Assuming
        // the image size is (width, height), first take the intersection of (0,
        // 0) - (width, height) and (x, y) - (x + tileSize, y + tileSize). If
        // in extending the region, we found some part of the region are outside
        // the image, those pixels are filled with black.
        //
        // If level > 0, it does the same operation on a down-scaled version of
        // the original image (down-scaled by a factor of 2^level), but (x, y)
        // still refers to the coordinate on the original image.
        //
        // The method would be called in another thread.
        public Bitmap getTile(int level, int x, int y, int tileSize);
    }

    public static boolean isHighResolution(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)
                context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels > 2048 ||  metrics.widthPixels > 2048;
    }

    public TileImageView(GalleryContext context) {
        mThreadPool = context.getThreadPool();
        mTileDecoder = mThreadPool.submit(new TileDecoder());
        if (sTileSize == 0) {
            if (isHighResolution(context.getAndroidContext())) {
                sTileSize = 512 ;
            } else {
                sTileSize = 256;
            }
        }
    }

    public void setModel(TileSource model) {
        mModel = model;
        if (model != null) notifyModelInvalidated();
    }

    public void setScreenNail(ScreenNail s) {
        mScreenNail = s;
    }

    public void notifyModelInvalidated() {
        invalidateTiles();
        if (mModel == null) {
            mScreenNail = null;
            mImageWidth = 0;
            mImageHeight = 0;
            mLevelCount = 0;
        } else {
            setScreenNail(mModel.getScreenNail());
            mImageWidth = mModel.getImageWidth();
            mImageHeight = mModel.getImageHeight();
            mLevelCount = mModel.getLevelCount();
        }
        layoutTiles(mCenterX, mCenterY, mScale, mRotation);
        invalidate();
    }

    @Override
    protected void onLayout(
            boolean changeSize, int left, int top, int right, int bottom) {
        super.onLayout(changeSize, left, top, right, bottom);
        if (changeSize) layoutTiles(mCenterX, mCenterY, mScale, mRotation);
    }

    // Prepare the tiles we want to use for display.
    //
    // 1. Decide the tile level we want to use for display.
    // 2. Decide the tile levels we want to keep as texture (in addition to
    //    the one we use for display).
    // 3. Recycle unused tiles.
    // 4. Activate the tiles we want.
    private void layoutTiles(int centerX, int centerY, float scale, int rotation) {
        // The width and height of this view.
        int width = getWidth();
        int height = getHeight();

        // The tile levels we want to keep as texture is in the range
        // [fromLevel, endLevel).
        int fromLevel;
        int endLevel;

        // We want to use a texture larger than or equal to the display size.
        mLevel = Utils.clamp(Utils.floorLog2(1f / scale), 0, mLevelCount);

        // We want to keep one more tile level as texture in addition to what
        // we use for display. So it can be faster when the scale moves to the
        // next level. We choose a level closer to the current scale.
        if (mLevel != mLevelCount) {
            Rect range = mTileRange;
            getRange(range, centerX, centerY, mLevel, scale, rotation);
            mOffsetX = Math.round(width / 2f + (range.left - centerX) * scale);
            mOffsetY = Math.round(height / 2f + (range.top - centerY) * scale);
            fromLevel = scale * (1 << mLevel) > 0.75f ? mLevel - 1 : mLevel;
        } else {
            // Activate the tiles of the smallest two levels.
            fromLevel = mLevel - 2;
            mOffsetX = Math.round(width / 2f - centerX * scale);
            mOffsetY = Math.round(height / 2f - centerY * scale);
        }

        fromLevel = Math.max(0, Math.min(fromLevel, mLevelCount - 2));
        endLevel = Math.min(fromLevel + 2, mLevelCount);

        Rect range[] = mActiveRange;
        for (int i = fromLevel; i < endLevel; ++i) {
            getRange(range[i - fromLevel], centerX, centerY, i, rotation);
        }

        // If rotation is transient, don't update the tile.
        if (rotation % 90 != 0) return;

        synchronized (this) {
            mDecodeQueue.clean();
            mUploadQueue.clean();
            mBackgroundTileUploaded = false;

            // Recycle unused tiles: if the level of the active tile is outside the
            // range [fromLevel, endLevel) or not in the visible range.
            int n = mActiveTiles.size();
            for (int i = 0; i < n; i++) {
                Tile tile = mActiveTiles.valueAt(i);
                int level = tile.mTileLevel;
                if (level < fromLevel || level >= endLevel
                        || !range[level - fromLevel].contains(tile.mX, tile.mY)) {
                    mActiveTiles.removeAt(i);
                    i--;
                    n--;
                    recycleTile(tile);
                }
            }
        }

        for (int i = fromLevel; i < endLevel; ++i) {
            int size = sTileSize << i;
            Rect r = range[i - fromLevel];
            for (int y = r.top, bottom = r.bottom; y < bottom; y += size) {
                for (int x = r.left, right = r.right; x < right; x += size) {
                    activateTile(x, y, i);
                }
            }
        }
        invalidate();
    }

    protected synchronized void invalidateTiles() {
        mDecodeQueue.clean();
        mUploadQueue.clean();

        // TODO disable decoder
        int n = mActiveTiles.size();
        for (int i = 0; i < n; i++) {
            Tile tile = mActiveTiles.valueAt(i);
            recycleTile(tile);
        }
        mActiveTiles.clear();
    }

    private void getRange(Rect out, int cX, int cY, int level, int rotation) {
        getRange(out, cX, cY, level, 1f / (1 << (level + 1)), rotation);
    }

    // If the bitmap is scaled by the given factor "scale", return the
    // rectangle containing visible range. The left-top coordinate returned is
    // aligned to the tile boundary.
    //
    // (cX, cY) is the point on the original bitmap which will be put in the
    // center of the ImageViewer.
    private void getRange(Rect out,
            int cX, int cY, int level, float scale, int rotation) {

        double radians = Math.toRadians(-rotation);
        double w = getWidth();
        double h = getHeight();

        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        int width = (int) Math.ceil(Math.max(
                Math.abs(cos * w - sin * h), Math.abs(cos * w + sin * h)));
        int height = (int) Math.ceil(Math.max(
                Math.abs(sin * w + cos * h), Math.abs(sin * w - cos * h)));

        int left = (int) FloatMath.floor(cX - width / (2f * scale));
        int top = (int) FloatMath.floor(cY - height / (2f * scale));
        int right = (int) FloatMath.ceil(left + width / scale);
        int bottom = (int) FloatMath.ceil(top + height / scale);

        // align the rectangle to tile boundary
        int size = sTileSize << level;
        left = Math.max(0, size * (left / size));
        top = Math.max(0, size * (top / size));
        right = Math.min(mImageWidth, right);
        bottom = Math.min(mImageHeight, bottom);

        out.set(left, top, right, bottom);
    }

    // Calculate where the center of the image is, in the view coordinates.
    public void getImageCenter(Point center) {
        // The width and height of this view.
        int viewW = getWidth();
        int viewH = getHeight();

        // The distance between the center of the view to the center of the
        // bitmap, in bitmap units. (mCenterX and mCenterY are the bitmap
        // coordinates correspond to the center of view)
        int distW, distH;
        if (mRotation % 180 == 0) {
            distW = mImageWidth / 2 - mCenterX;
            distH = mImageHeight / 2 - mCenterY;
        } else {
            distW = mImageHeight / 2 - mCenterY;
            distH = mImageWidth / 2 - mCenterX;
        }

        // Convert to view coordinates. mScale translates from bitmap units to
        // view units.
        center.x = Math.round(viewW / 2f + distW * mScale);
        center.y = Math.round(viewH / 2f + distH * mScale);
    }

    public boolean setPosition(int centerX, int centerY, float scale, int rotation) {
        if (mCenterX == centerX && mCenterY == centerY
                && mScale == scale && mRotation == rotation) return false;
        mCenterX = centerX;
        mCenterY = centerY;
        mScale = scale;
        mRotation = rotation;
        layoutTiles(centerX, centerY, scale, rotation);
        invalidate();
        return true;
    }

    public void freeTextures() {
        mIsTextureFreed = true;

        if (mTileDecoder != null) {
            mTileDecoder.cancel();
            mTileDecoder.get();
            mTileDecoder = null;
        }

        int n = mActiveTiles.size();
        for (int i = 0; i < n; i++) {
            Tile texture = mActiveTiles.valueAt(i);
            texture.recycle();
        }
        mActiveTiles.clear();
        mTileRange.set(0, 0, 0, 0);

        synchronized (this) {
            mUploadQueue.clean();
            mDecodeQueue.clean();
            Tile tile = mRecycledQueue.pop();
            while (tile != null) {
                tile.recycle();
                tile = mRecycledQueue.pop();
            }
        }
        setScreenNail(null);
    }

    public void prepareTextures() {
        if (mTileDecoder == null) {
            mTileDecoder = mThreadPool.submit(new TileDecoder());
        }
        if (mIsTextureFreed) {
            layoutTiles(mCenterX, mCenterY, mScale, mRotation);
            mIsTextureFreed = false;
            setScreenNail(mModel == null ? null : mModel.getScreenNail());
        }
    }

    @Override
    protected void render(GLCanvas canvas) {
        mUploadQuota = UPLOAD_LIMIT;
        mRenderComplete = true;

        int level = mLevel;
        int rotation = mRotation;
        int flags = 0;
        if (rotation != 0) flags |= GLCanvas.SAVE_FLAG_MATRIX;

        if (flags != 0) {
            canvas.save(flags);
            if (rotation != 0) {
                int centerX = getWidth() / 2, centerY = getHeight() / 2;
                canvas.translate(centerX, centerY);
                canvas.rotate(rotation, 0, 0, 1);
                canvas.translate(-centerX, -centerY);
            }
        }
        try {
            if (level != mLevelCount && !isScreenNailAnimating()) {
                if (mScreenNail != null) {
                    mScreenNail.noDraw();
                }

                int size = (sTileSize << level);
                float length = size * mScale;
                Rect r = mTileRange;

                for (int ty = r.top, i = 0; ty < r.bottom; ty += size, i++) {
                    float y = mOffsetY + i * length;
                    for (int tx = r.left, j = 0; tx < r.right; tx += size, j++) {
                        float x = mOffsetX + j * length;
                        drawTile(canvas, tx, ty, level, x, y, length);
                    }
                }
            } else if (mScreenNail != null) {
                mScreenNail.draw(canvas, mOffsetX, mOffsetY,
                        Math.round(mImageWidth * mScale),
                        Math.round(mImageHeight * mScale));
                if (isScreenNailAnimating()) {
                    invalidate();
                }
            }
        } finally {
            if (flags != 0) canvas.restore();
        }

        if (mRenderComplete) {
            if (!mBackgroundTileUploaded) uploadBackgroundTiles(canvas);
        } else {
            invalidate();
        }
    }

    private boolean isScreenNailAnimating() {
        return (mScreenNail instanceof TiledScreenNail)
                && ((TiledScreenNail) mScreenNail).isAnimating();
    }

    private void uploadBackgroundTiles(GLCanvas canvas) {
        mBackgroundTileUploaded = true;
        int n = mActiveTiles.size();
        for (int i = 0; i < n; i++) {
            Tile tile = mActiveTiles.valueAt(i);
            if (!tile.isContentValid()) queueForDecode(tile);
        }
    }

    void queueForUpload(Tile tile) {
        synchronized (this) {
            mUploadQueue.push(tile);
        }
        if (mTileUploader.mActive.compareAndSet(false, true)) {
            getGLRoot().addOnGLIdleListener(mTileUploader);
        }
    }

    synchronized void queueForDecode(Tile tile) {
        if (tile.mTileState == STATE_ACTIVATED) {
            tile.mTileState = STATE_IN_QUEUE;
            if (mDecodeQueue.push(tile)) notifyAll();
        }
    }

    boolean decodeTile(Tile tile) {
        synchronized (this) {
            if (tile.mTileState != STATE_IN_QUEUE) return false;
            tile.mTileState = STATE_DECODING;
        }
        boolean decodeComplete = tile.decode();
        synchronized (this) {
            if (tile.mTileState == STATE_RECYCLING) {
                tile.mTileState = STATE_RECYCLED;
                if (tile.mDecodedTile != null) {
                    GalleryBitmapPool.getInstance().put(tile.mDecodedTile);
                    tile.mDecodedTile = null;
                }
                mRecycledQueue.push(tile);
                return false;
            }
            tile.mTileState = decodeComplete ? STATE_DECODED : STATE_DECODE_FAIL;
            return decodeComplete;
        }
    }

    private synchronized Tile obtainTile(int x, int y, int level) {
        Tile tile = mRecycledQueue.pop();
        if (tile != null) {
            tile.mTileState = STATE_ACTIVATED;
            tile.update(x, y, level);
            return tile;
        }
        return new Tile(x, y, level);
    }

    synchronized void recycleTile(Tile tile) {
        if (tile.mTileState == STATE_DECODING) {
            tile.mTileState = STATE_RECYCLING;
            return;
        }
        tile.mTileState = STATE_RECYCLED;
        if (tile.mDecodedTile != null) {
            GalleryBitmapPool.getInstance().put(tile.mDecodedTile);
            tile.mDecodedTile = null;
        }
        mRecycledQueue.push(tile);
    }

    private void activateTile(int x, int y, int level) {
        long key = makeTileKey(x, y, level);
        Tile tile = mActiveTiles.get(key);
        if (tile != null) {
            if (tile.mTileState == STATE_IN_QUEUE) {
                tile.mTileState = STATE_ACTIVATED;
            }
            return;
        }
        tile = obtainTile(x, y, level);
        mActiveTiles.put(key, tile);
    }

    private Tile getTile(int x, int y, int level) {
        return mActiveTiles.get(makeTileKey(x, y, level));
    }

    private static long makeTileKey(int x, int y, int level) {
        long result = x;
        result = (result << 16) | y;
        result = (result << 16) | level;
        return result;
    }

    private class TileUploader implements GLRoot.OnGLIdleListener {
        AtomicBoolean mActive = new AtomicBoolean(false);

        @Override
        public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
            // Skips uploading if there is a pending rendering request.
            // Returns true to keep uploading in next rendering loop.
            if (renderRequested) return true;
            int quota = UPLOAD_LIMIT;
            Tile tile = null;
            while (quota > 0) {
                synchronized (TileImageView.this) {
                    tile = mUploadQueue.pop();
                }
                if (tile == null) break;
                if (!tile.isContentValid()) {
                    boolean hasBeenLoaded = tile.isLoaded();
                    Utils.assertTrue(tile.mTileState == STATE_DECODED);
                    tile.updateContent(canvas);
                    if (!hasBeenLoaded) tile.draw(canvas, 0, 0);
                    --quota;
                }
            }
            if (tile == null) mActive.set(false);
            return tile != null;
        }
    }

    // Draw the tile to a square at canvas that locates at (x, y) and
    // has a side length of length.
    public void drawTile(GLCanvas canvas,
            int tx, int ty, int level, float x, float y, float length) {
        RectF source = mSourceRect;
        RectF target = mTargetRect;
        target.set(x, y, x + length, y + length);
        source.set(0, 0, sTileSize, sTileSize);

        Tile tile = getTile(tx, ty, level);
        if (tile != null) {
            if (!tile.isContentValid()) {
                if (tile.mTileState == STATE_DECODED) {
                    if (mUploadQuota > 0) {
                        --mUploadQuota;
                        tile.updateContent(canvas);
                    } else {
                        mRenderComplete = false;
                    }
                } else if (tile.mTileState != STATE_DECODE_FAIL){
                    mRenderComplete = false;
                    queueForDecode(tile);
                }
            }
            if (drawTile(tile, canvas, source, target)) return;
        }
        if (mScreenNail != null) {
            int size = sTileSize << level;
            float scaleX = (float) mScreenNail.getWidth() / mImageWidth;
            float scaleY = (float) mScreenNail.getHeight() / mImageHeight;
            source.set(tx * scaleX, ty * scaleY, (tx + size) * scaleX,
                    (ty + size) * scaleY);
            mScreenNail.draw(canvas, source, target);
        }
    }

    static boolean drawTile(
            Tile tile, GLCanvas canvas, RectF source, RectF target) {
        while (true) {
            if (tile.isContentValid()) {
                canvas.drawTexture(tile, source, target);
                return true;
            }

            // Parent can be divided to four quads and tile is one of the four.
            Tile parent = tile.getParentTile();
            if (parent == null) return false;
            if (tile.mX == parent.mX) {
                source.left /= 2f;
                source.right /= 2f;
            } else {
                source.left = (sTileSize + source.left) / 2f;
                source.right = (sTileSize + source.right) / 2f;
            }
            if (tile.mY == parent.mY) {
                source.top /= 2f;
                source.bottom /= 2f;
            } else {
                source.top = (sTileSize + source.top) / 2f;
                source.bottom = (sTileSize + source.bottom) / 2f;
            }
            tile = parent;
        }
    }

    private class Tile extends UploadedTexture {
        public int mX;
        public int mY;
        public int mTileLevel;
        public Tile mNext;
        public Bitmap mDecodedTile;
        public volatile int mTileState = STATE_ACTIVATED;

        public Tile(int x, int y, int level) {
            mX = x;
            mY = y;
            mTileLevel = level;
        }

        @Override
        protected void onFreeBitmap(Bitmap bitmap) {
            GalleryBitmapPool.getInstance().put(bitmap);
        }

        boolean decode() {
            // Get a tile from the original image. The tile is down-scaled
            // by (1 << mTilelevel) from a region in the original image.
            try {
                mDecodedTile = DecodeUtils.ensureGLCompatibleBitmap(mModel.getTile(
                        mTileLevel, mX, mY, sTileSize));
            } catch (Throwable t) {
                Log.w(TAG, "fail to decode tile", t);
            }
            return mDecodedTile != null;
        }

        @Override
        protected Bitmap onGetBitmap() {
            Utils.assertTrue(mTileState == STATE_DECODED);

            // We need to override the width and height, so that we won't
            // draw beyond the boundaries.
            int rightEdge = ((mImageWidth - mX) >> mTileLevel);
            int bottomEdge = ((mImageHeight - mY) >> mTileLevel);
            setSize(Math.min(sTileSize, rightEdge), Math.min(sTileSize, bottomEdge));

            Bitmap bitmap = mDecodedTile;
            mDecodedTile = null;
            mTileState = STATE_ACTIVATED;
            return bitmap;
        }

        // We override getTextureWidth() and getTextureHeight() here, so the
        // texture can be re-used for different tiles regardless of the actual
        // size of the tile (which may be small because it is a tile at the
        // boundary).
        @Override
        public int getTextureWidth() {
            return sTileSize;
        }

        @Override
        public int getTextureHeight() {
            return sTileSize;
        }

        public void update(int x, int y, int level) {
            mX = x;
            mY = y;
            mTileLevel = level;
            invalidateContent();
        }

        public Tile getParentTile() {
            if (mTileLevel + 1 == mLevelCount) return null;
            int size = sTileSize << (mTileLevel + 1);
            int x = size * (mX / size);
            int y = size * (mY / size);
            return getTile(x, y, mTileLevel + 1);
        }

        @Override
        public String toString() {
            return String.format("tile(%s, %s, %s / %s)",
                    mX / sTileSize, mY / sTileSize, mLevel, mLevelCount);
        }
    }

    private static class TileQueue {
        private Tile mHead;

        public Tile pop() {
            Tile tile = mHead;
            if (tile != null) mHead = tile.mNext;
            return tile;
        }

        public boolean push(Tile tile) {
            boolean wasEmpty = mHead == null;
            tile.mNext = mHead;
            mHead = tile;
            return wasEmpty;
        }

        public void clean() {
            mHead = null;
        }
    }

    private class TileDecoder implements ThreadPool.Job<Void> {

        private CancelListener mNotifier = new CancelListener() {
            @Override
            public void onCancel() {
                synchronized (TileImageView.this) {
                    TileImageView.this.notifyAll();
                }
            }
        };

        @Override
        public Void run(JobContext jc) {
            jc.setMode(ThreadPool.MODE_NONE);
            jc.setCancelListener(mNotifier);
            while (!jc.isCancelled()) {
                Tile tile = null;
                synchronized(TileImageView.this) {
                    tile = mDecodeQueue.pop();
                    if (tile == null && !jc.isCancelled()) {
                        Utils.waitWithoutInterrupt(TileImageView.this);
                    }
                }
                if (tile == null) continue;
                if (decodeTile(tile)) queueForUpload(tile);
            }
            return null;
        }
    }
}
