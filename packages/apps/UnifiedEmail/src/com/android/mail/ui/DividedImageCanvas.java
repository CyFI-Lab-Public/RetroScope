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

package com.android.mail.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import com.android.mail.R;
import com.android.mail.utils.Utils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DividedImageCanvas creates a canvas that can display into a minimum of 1
 * and maximum of 4 images. As images are added, they
 * are laid out according to the following algorithm:
 * 1 Image: Draw the bitmap filling the entire canvas.
 * 2 Images: Draw 2 bitmaps split vertically down the middle.
 * 3 Images: Draw 3 bitmaps: the first takes up all vertical space; the 2nd and 3rd are stacked in
 *           the second vertical position.
 * 4 Images: Divide the Canvas into 4 equal quadrants and draws 1 bitmap in each.
 */
public class DividedImageCanvas implements ImageCanvas {
    public static final int MAX_DIVISIONS = 4;

    private final Map<String, Integer> mDivisionMap = Maps
            .newHashMapWithExpectedSize(MAX_DIVISIONS);
    private Bitmap mDividedBitmap;
    private Canvas mCanvas;
    private int mWidth;
    private int mHeight;

    private final Context mContext;
    private final InvalidateCallback mCallback;
    private final ArrayList<Bitmap> mDivisionImages = new ArrayList<Bitmap>(MAX_DIVISIONS);

    /**
     * Ignore any request to draw final output when not yet ready. This prevents partially drawn
     * canvases from appearing.
     */
    private boolean mBitmapValid = false;

    private int mGeneration;

    private static final Paint sPaint = new Paint();
    private static final Paint sClearPaint = new Paint();
    private static final Rect sSrc = new Rect();
    private static final Rect sDest = new Rect();

    private static int sDividerLineWidth = -1;
    private static int sDividerColor;

    static {
        sClearPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
    }

    public DividedImageCanvas(Context context, InvalidateCallback callback) {
        mContext = context;
        mCallback = callback;
        setupDividerLines();
    }

    /**
     * Get application context for this object.
     */
    public Context getContext() {
        return mContext;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append(super.toString());
        sb.append(" mDivisionMap=");
        sb.append(mDivisionMap);
        sb.append(" mDivisionImages=");
        sb.append(mDivisionImages);
        sb.append(" mWidth=");
        sb.append(mWidth);
        sb.append(" mHeight=");
        sb.append(mHeight);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Set the id associated with each quadrant. The quadrants are laid out:
     * TopLeft, TopRight, Bottom Left, Bottom Right
     * @param keys
     */
    public void setDivisionIds(List<Object> keys) {
        if (keys.size() > MAX_DIVISIONS) {
            throw new IllegalArgumentException("too many divisionIds: " + keys);
        }

        boolean needClear = getDivisionCount() != keys.size();
        if (!needClear) {
            for (int i = 0; i < keys.size(); i++) {
                String divisionId = transformKeyToDivisionId(keys.get(i));
                // different item or different place
                if (!mDivisionMap.containsKey(divisionId) || mDivisionMap.get(divisionId) != i) {
                    needClear = true;
                    break;
                }
            }
        }

        if (needClear) {
            mDivisionMap.clear();
            mDivisionImages.clear();
            int i = 0;
            for (Object key : keys) {
                String divisionId = transformKeyToDivisionId(key);
                mDivisionMap.put(divisionId, i);
                mDivisionImages.add(null);
                i++;
            }
        }
    }

    private void draw(Bitmap b, int left, int top, int right, int bottom) {
        if (b != null) {
            // Some times we load taller images compared to the destination rect on the canvas
            int srcTop = 0;
            int srcBottom = b.getHeight();
            int destHeight = bottom - top;
            if (b.getHeight() > bottom - top) {
                srcTop = b.getHeight() / 2 - destHeight/2;
                srcBottom = b.getHeight() / 2 + destHeight/2;
            }

//            todo:markwei do not scale very small bitmaps
            // l t r b
            sSrc.set(0, srcTop, b.getWidth(), srcBottom);
            sDest.set(left, top, right, bottom);
            mCanvas.drawRect(sDest, sClearPaint);
            mCanvas.drawBitmap(b, sSrc, sDest, sPaint);
        } else {
            // clear
            mCanvas.drawRect(left, top, right, bottom, sClearPaint);
        }
    }

    /**
     * Get the desired dimensions and scale for the bitmap to be placed in the
     * location corresponding to id. Caller must allocate the Dimensions object.
     * @param key
     * @param outDim a {@link ImageCanvas.Dimensions} object to write results into
     */
    @Override
    public void getDesiredDimensions(Object key, Dimensions outDim) {
        Utils.traceBeginSection("get desired dimensions");
        int w = 0, h = 0;
        float scale = 0;
        final Integer pos = mDivisionMap.get(transformKeyToDivisionId(key));
        if (pos != null && pos >= 0) {
            final int size = mDivisionMap.size();
            switch (size) {
                case 0:
                    break;
                case 1:
                    w = mWidth;
                    h = mHeight;
                    scale = Dimensions.SCALE_ONE;
                    break;
                case 2:
                    w = mWidth / 2;
                    h = mHeight;
                    scale = Dimensions.SCALE_HALF;
                    break;
                case 3:
                    switch (pos) {
                        case 0:
                            w = mWidth / 2;
                            h = mHeight;
                            scale = Dimensions.SCALE_HALF;
                            break;
                        default:
                            w = mWidth / 2;
                            h = mHeight / 2;
                            scale = Dimensions.SCALE_QUARTER;
                    }
                    break;
                case 4:
                    w = mWidth / 2;
                    h = mHeight / 2;
                    scale = Dimensions.SCALE_QUARTER;
                    break;
            }
        }
        outDim.width = w;
        outDim.height = h;
        outDim.scale = scale;
        Utils.traceEndSection();
    }

    @Override
    public void drawImage(Bitmap b, Object key) {
        addDivisionImage(b, key);
    }

    /**
     * Add a bitmap to this view in the quadrant matching its id.
     * @param b Bitmap
     * @param key Id to look for that was previously set in setDivisionIds.
     */
    public void addDivisionImage(Bitmap b, Object key) {
        if (b != null) {
            addOrClearDivisionImage(b, key);
        }
    }

    public void clearDivisionImage(Object key) {
        addOrClearDivisionImage(null, key);
    }
    private void addOrClearDivisionImage(Bitmap b, Object key) {
        Utils.traceBeginSection("add or clear division image");
        final Integer pos = mDivisionMap.get(transformKeyToDivisionId(key));
        if (pos != null && pos >= 0) {
            mDivisionImages.set(pos, b);
            boolean complete = false;
            final int width = mWidth;
            final int height = mHeight;
            // Different layouts depending on count.
            final int size = mDivisionMap.size();
            switch (size) {
                case 0:
                    // Do nothing.
                    break;
                case 1:
                    // Draw the bitmap filling the entire canvas.
                    draw(mDivisionImages.get(0), 0, 0, width, height);
                    complete = true;
                    break;
                case 2:
                    // Draw 2 bitmaps split vertically down the middle
                    switch (pos) {
                        case 0:
                            draw(mDivisionImages.get(0), 0, 0, width / 2, height);
                            break;
                        case 1:
                            draw(mDivisionImages.get(1), width / 2, 0, width, height);
                            break;
                    }
                    complete = mDivisionImages.get(0) != null && mDivisionImages.get(1) != null
                            || isPartialBitmapComplete();
                    if (complete) {
                        // Draw dividers
                        drawVerticalDivider(width, height);
                    }
                    break;
                case 3:
                    // Draw 3 bitmaps: the first takes up all vertical
                    // space, the 2nd and 3rd are stacked in the second vertical
                    // position.
                    switch (pos) {
                        case 0:
                            draw(mDivisionImages.get(0), 0, 0, width / 2, height);
                            break;
                        case 1:
                            draw(mDivisionImages.get(1), width / 2, 0, width, height / 2);
                            break;
                        case 2:
                            draw(mDivisionImages.get(2), width / 2, height / 2, width, height);
                            break;
                    }
                    complete = mDivisionImages.get(0) != null && mDivisionImages.get(1) != null
                            && mDivisionImages.get(2) != null || isPartialBitmapComplete();
                    if (complete) {
                        // Draw dividers
                        drawVerticalDivider(width, height);
                        drawHorizontalDivider(width / 2, height / 2, width, height / 2);
                    }
                    break;
                default:
                    // Draw all 4 bitmaps in a grid
                    switch (pos) {
                        case 0:
                            draw(mDivisionImages.get(0), 0, 0, width / 2, height / 2);
                            break;
                        case 1:
                            draw(mDivisionImages.get(1), width / 2, 0, width, height / 2);
                            break;
                        case 2:
                            draw(mDivisionImages.get(2), 0, height / 2, width / 2, height);
                            break;
                        case 3:
                            draw(mDivisionImages.get(3), width / 2, height / 2, width, height);
                            break;
                    }
                    complete = mDivisionImages.get(0) != null && mDivisionImages.get(1) != null
                            && mDivisionImages.get(2) != null && mDivisionImages.get(3) != null
                            || isPartialBitmapComplete();
                    if (complete) {
                        // Draw dividers
                        drawVerticalDivider(width, height);
                        drawHorizontalDivider(0, height / 2, width, height / 2);
                    }
                    break;
            }
            // Create the new image bitmap.
            if (complete) {
                mBitmapValid = true;
                mCallback.invalidate();
            }
        }
        Utils.traceEndSection();
    }

    public boolean hasImageFor(Object key) {
        final Integer pos = mDivisionMap.get(transformKeyToDivisionId(key));
        return pos != null && mDivisionImages.get(pos) != null;
    }

    private void setupDividerLines() {
        if (sDividerLineWidth == -1) {
            Resources res = getContext().getResources();
            sDividerLineWidth = res
                    .getDimensionPixelSize(R.dimen.tile_divider_width);
            sDividerColor = res.getColor(R.color.tile_divider_color);
        }
    }

    private static void setupPaint() {
        sPaint.setStrokeWidth(sDividerLineWidth);
        sPaint.setColor(sDividerColor);
    }

    protected void drawVerticalDivider(int width, int height) {
        int x1 = width / 2, y1 = 0, x2 = width/2, y2 = height;
        setupPaint();
        mCanvas.drawLine(x1, y1, x2, y2, sPaint);
    }

    protected void drawHorizontalDivider(int x1, int y1, int x2, int y2) {
        setupPaint();
        mCanvas.drawLine(x1, y1, x2, y2, sPaint);
    }

    protected boolean isPartialBitmapComplete() {
        return false;
    }

    protected String transformKeyToDivisionId(Object key) {
        return key.toString();
    }

    /**
     * Draw the contents of the DividedImageCanvas to the supplied canvas.
     */
    public void draw(Canvas canvas) {
        if (mDividedBitmap != null && mBitmapValid) {
            canvas.drawBitmap(mDividedBitmap, 0, 0, null);
        }
    }

    /**
     * Draw the contents of the DividedImageCanvas to the supplied canvas.
     */
    public void draw(final Canvas canvas, final Matrix matrix) {
        if (mDividedBitmap != null && mBitmapValid) {
            canvas.drawBitmap(mDividedBitmap, matrix, null);
        }
    }

    @Override
    public void reset() {
        if (mCanvas != null && mDividedBitmap != null) {
            mBitmapValid = false;
        }
        mDivisionMap.clear();
        mDivisionImages.clear();
        mGeneration++;
    }

    @Override
    public int getGeneration() {
        return mGeneration;
    }

    /**
     * Set the width and height of the canvas.
     * @param width
     * @param height
     */
    public void setDimensions(int width, int height) {
        Utils.traceBeginSection("set dimensions");
        if (mWidth == width && mHeight == height) {
            Utils.traceEndSection();
            return;
        }

        mWidth = width;
        mHeight = height;

        mDividedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mDividedBitmap);

        for (int i = 0; i < getDivisionCount(); i++) {
            mDivisionImages.set(i, null);
        }
        mBitmapValid = false;
        Utils.traceEndSection();
    }

    /**
     * Get the resulting canvas width.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Get the resulting canvas height.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * The class that will provided the canvas to which the DividedImageCanvas
     * should render its contents must implement this interface.
     */
    public interface InvalidateCallback {
        public void invalidate();
    }

    public int getDivisionCount() {
        return mDivisionMap.size();
    }

    /**
     * Get the division ids currently associated with this DivisionImageCanvas.
     */
    public ArrayList<String> getDivisionIds() {
        return Lists.newArrayList(mDivisionMap.keySet());
    }
}
