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

package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.crop.CropDrawingUtils;
import com.android.gallery3d.filtershow.crop.CropMath;
import com.android.gallery3d.filtershow.crop.CropObject;
import com.android.gallery3d.filtershow.editors.EditorCrop;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils.GeometryHolder;

public class ImageCrop extends ImageShow {
    private static final String TAG = ImageCrop.class.getSimpleName();
    private RectF mImageBounds = new RectF();
    private RectF mScreenCropBounds = new RectF();
    private Paint mPaint = new Paint();
    private CropObject mCropObj = null;
    private GeometryHolder mGeometry = new GeometryHolder();
    private GeometryHolder mUpdateHolder = new GeometryHolder();
    private Drawable mCropIndicator;
    private int mIndicatorSize;
    private boolean mMovingBlock = false;
    private Matrix mDisplayMatrix = null;
    private Matrix mDisplayCropMatrix = null;
    private Matrix mDisplayMatrixInverse = null;
    private float mPrevX = 0;
    private float mPrevY = 0;
    private int mMinSideSize = 90;
    private int mTouchTolerance = 40;
    private enum Mode {
        NONE, MOVE
    }
    private Mode mState = Mode.NONE;
    private boolean mValidDraw = false;
    FilterCropRepresentation mLocalRep = new FilterCropRepresentation();
    EditorCrop mEditorCrop;

    public ImageCrop(Context context) {
        super(context);
        setup(context);
    }

    public ImageCrop(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public ImageCrop(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context);
    }

    private void setup(Context context) {
        Resources rsc = context.getResources();
        mCropIndicator = rsc.getDrawable(R.drawable.camera_crop);
        mIndicatorSize = (int) rsc.getDimension(R.dimen.crop_indicator_size);
        mMinSideSize = (int) rsc.getDimension(R.dimen.crop_min_side);
        mTouchTolerance = (int) rsc.getDimension(R.dimen.crop_touch_tolerance);
    }

    public void setFilterCropRepresentation(FilterCropRepresentation crop) {
        mLocalRep = (crop == null) ? new FilterCropRepresentation() : crop;
        GeometryMathUtils.initializeHolder(mUpdateHolder, mLocalRep);
        mValidDraw = true;
    }

    public FilterCropRepresentation getFinalRepresentation() {
        return mLocalRep;
    }

    private void internallyUpdateLocalRep(RectF crop, RectF image) {
        FilterCropRepresentation
                .findNormalizedCrop(crop, (int) image.width(), (int) image.height());
        mGeometry.crop.set(crop);
        mUpdateHolder.set(mGeometry);
        mLocalRep.setCrop(crop);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (mDisplayMatrix == null || mDisplayMatrixInverse == null) {
            return true;
        }
        float[] touchPoint = {
                x, y
        };
        mDisplayMatrixInverse.mapPoints(touchPoint);
        x = touchPoint[0];
        y = touchPoint[1];
        switch (event.getActionMasked()) {
            case (MotionEvent.ACTION_DOWN):
                if (mState == Mode.NONE) {
                    if (!mCropObj.selectEdge(x, y)) {
                        mMovingBlock = mCropObj.selectEdge(CropObject.MOVE_BLOCK);
                    }
                    mPrevX = x;
                    mPrevY = y;
                    mState = Mode.MOVE;
                }
                break;
            case (MotionEvent.ACTION_UP):
                if (mState == Mode.MOVE) {
                    mCropObj.selectEdge(CropObject.MOVE_NONE);
                    mMovingBlock = false;
                    mPrevX = x;
                    mPrevY = y;
                    mState = Mode.NONE;
                    internallyUpdateLocalRep(mCropObj.getInnerBounds(), mCropObj.getOuterBounds());
                }
                break;
            case (MotionEvent.ACTION_MOVE):
                if (mState == Mode.MOVE) {
                    float dx = x - mPrevX;
                    float dy = y - mPrevY;
                    mCropObj.moveCurrentSelection(dx, dy);
                    mPrevX = x;
                    mPrevY = y;
                }
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    private void clearDisplay() {
        mDisplayMatrix = null;
        mDisplayMatrixInverse = null;
        invalidate();
    }

    public void applyFreeAspect() {
        mCropObj.unsetAspectRatio();
        invalidate();
    }

    public void applyOriginalAspect() {
        RectF outer = mCropObj.getOuterBounds();
        float w = outer.width();
        float h = outer.height();
        if (w > 0 && h > 0) {
            applyAspect(w, h);
            mCropObj.resetBoundsTo(outer, outer);
            internallyUpdateLocalRep(mCropObj.getInnerBounds(), mCropObj.getOuterBounds());
        } else {
            Log.w(TAG, "failed to set aspect ratio original");
        }
        invalidate();
    }

    public void applyAspect(float x, float y) {
        if (x <= 0 || y <= 0) {
            throw new IllegalArgumentException("Bad arguments to applyAspect");
        }
        // If we are rotated by 90 degrees from horizontal, swap x and y
        if (GeometryMathUtils.needsDimensionSwap(mGeometry.rotation)) {
            float tmp = x;
            x = y;
            y = tmp;
        }
        if (!mCropObj.setInnerAspectRatio(x, y)) {
            Log.w(TAG, "failed to set aspect ratio");
        }
        internallyUpdateLocalRep(mCropObj.getInnerBounds(), mCropObj.getOuterBounds());
        invalidate();
    }

    /**
     * Rotates first d bits in integer x to the left some number of times.
     */
    private int bitCycleLeft(int x, int times, int d) {
        int mask = (1 << d) - 1;
        int mout = x & mask;
        times %= d;
        int hi = mout >> (d - times);
        int low = (mout << times) & mask;
        int ret = x & ~mask;
        ret |= low;
        ret |= hi;
        return ret;
    }

    /**
     * Find the selected edge or corner in screen coordinates.
     */
    private int decode(int movingEdges, float rotation) {
        int rot = CropMath.constrainedRotation(rotation);
        switch (rot) {
            case 90:
                return bitCycleLeft(movingEdges, 1, 4);
            case 180:
                return bitCycleLeft(movingEdges, 2, 4);
            case 270:
                return bitCycleLeft(movingEdges, 3, 4);
            default:
                return movingEdges;
        }
    }

    private void forceStateConsistency() {
        MasterImage master = MasterImage.getImage();
        Bitmap image = master.getFiltersOnlyImage();
        int width = image.getWidth();
        int height = image.getHeight();
        if (mCropObj == null || !mUpdateHolder.equals(mGeometry)
                || mImageBounds.width() != width || mImageBounds.height() != height
                || !mLocalRep.getCrop().equals(mUpdateHolder.crop)) {
            mImageBounds.set(0, 0, width, height);
            mGeometry.set(mUpdateHolder);
            mLocalRep.setCrop(mUpdateHolder.crop);
            RectF scaledCrop = new RectF(mUpdateHolder.crop);
            FilterCropRepresentation.findScaledCrop(scaledCrop, width, height);
            mCropObj = new CropObject(mImageBounds, scaledCrop, (int) mUpdateHolder.straighten);
            mState = Mode.NONE;
            clearDisplay();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        clearDisplay();
    }

    @Override
    public void onDraw(Canvas canvas) {
        Bitmap bitmap = MasterImage.getImage().getFiltersOnlyImage();
        if (bitmap == null) {
            MasterImage.getImage().invalidateFiltersOnly();
        }
        if (!mValidDraw || bitmap == null) {
            return;
        }
        forceStateConsistency();
        mImageBounds.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        // If display matrix doesn't exist, create it and its dependencies
        if (mDisplayCropMatrix == null || mDisplayMatrix == null || mDisplayMatrixInverse == null) {
            mCropObj.unsetAspectRatio();
            mDisplayMatrix = GeometryMathUtils.getFullGeometryToScreenMatrix(mGeometry,
                    bitmap.getWidth(), bitmap.getHeight(), canvas.getWidth(), canvas.getHeight());
            float straighten = mGeometry.straighten;
            mGeometry.straighten = 0;
            mDisplayCropMatrix = GeometryMathUtils.getFullGeometryToScreenMatrix(mGeometry,
                    bitmap.getWidth(), bitmap.getHeight(), canvas.getWidth(), canvas.getHeight());
            mGeometry.straighten = straighten;
            mDisplayMatrixInverse = new Matrix();
            mDisplayMatrixInverse.reset();
            if (!mDisplayCropMatrix.invert(mDisplayMatrixInverse)) {
                Log.w(TAG, "could not invert display matrix");
                mDisplayMatrixInverse = null;
                return;
            }
            // Scale min side and tolerance by display matrix scale factor
            mCropObj.setMinInnerSideSize(mDisplayMatrixInverse.mapRadius(mMinSideSize));
            mCropObj.setTouchTolerance(mDisplayMatrixInverse.mapRadius(mTouchTolerance));
            // drive Crop engine to clamp to crop bounds
            int[] sides = {CropObject.MOVE_TOP,
                    CropObject.MOVE_BOTTOM,
                    CropObject.MOVE_LEFT,
                    CropObject.MOVE_RIGHT};
            int delta = Math.min(canvas.getWidth(), canvas.getHeight()) / 4;
            int[] dy = {delta, -delta, 0, 0};
            int[] dx = {0, 0, delta, -delta};

            for (int i = 0; i < sides.length; i++) {
                mCropObj.selectEdge(sides[i]);

                mCropObj.moveCurrentSelection(dx[i], dy[i]);
                mCropObj.moveCurrentSelection(-dx[i], -dy[i]);
            }
            mCropObj.selectEdge(CropObject.MOVE_NONE);
        }
        // Draw actual bitmap
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, mDisplayMatrix, mPaint);
        mCropObj.getInnerBounds(mScreenCropBounds);
        RectF outer = mCropObj.getOuterBounds();
        FilterCropRepresentation.findNormalizedCrop(mScreenCropBounds, (int) outer.width(),
                (int) outer.height());
        FilterCropRepresentation.findScaledCrop(mScreenCropBounds, bitmap.getWidth(),
                bitmap.getHeight());
        if (mDisplayCropMatrix.mapRect(mScreenCropBounds)) {
            // Draw crop rect and markers
            CropDrawingUtils.drawCropRect(canvas, mScreenCropBounds);
            CropDrawingUtils.drawShade(canvas, mScreenCropBounds);
            CropDrawingUtils.drawRuleOfThird(canvas, mScreenCropBounds);
            CropDrawingUtils.drawIndicators(canvas, mCropIndicator, mIndicatorSize,
                    mScreenCropBounds, mCropObj.isFixedAspect(),
                    decode(mCropObj.getSelectState(), mGeometry.rotation.value()));
        }
    }

    public void setEditor(EditorCrop editorCrop) {
        mEditorCrop = editorCrop;
    }
}
