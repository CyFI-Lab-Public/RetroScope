/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.android.gallery3d.filtershow.crop.CropDrawingUtils;
import com.android.gallery3d.filtershow.editors.EditorStraighten;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterStraightenRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils.GeometryHolder;

import java.util.ArrayList;
import java.util.Collection;


public class ImageStraighten extends ImageShow {
    private static final String TAG = ImageStraighten.class.getSimpleName();
    private float mBaseAngle = 0;
    private float mAngle = 0;
    private float mInitialAngle = 0;
    private static final int NBLINES = 16;
    private boolean mFirstDrawSinceUp = false;
    private EditorStraighten mEditorStraighten;
    private FilterStraightenRepresentation mLocalRep = new FilterStraightenRepresentation();
    private RectF mPriorCropAtUp = new RectF();
    private RectF mDrawRect = new RectF();
    private Path mDrawPath = new Path();
    private GeometryHolder mDrawHolder = new GeometryHolder();
    private enum MODES {
        NONE, MOVE
    }
    private MODES mState = MODES.NONE;
    private ValueAnimator mAnimator = null;
    private int mDefaultGridAlpha = 60;
    private float mGridAlpha = 1f;
    private int mOnStartAnimDelay = 1000;
    private int mAnimDelay = 500;
    private static final float MAX_STRAIGHTEN_ANGLE
        = FilterStraightenRepresentation.MAX_STRAIGHTEN_ANGLE;
    private static final float MIN_STRAIGHTEN_ANGLE
        = FilterStraightenRepresentation.MIN_STRAIGHTEN_ANGLE;
    private float mCurrentX;
    private float mCurrentY;
    private float mTouchCenterX;
    private float mTouchCenterY;
    private RectF mCrop = new RectF();
    private final Paint mPaint = new Paint();

    public ImageStraighten(Context context) {
        super(context);
    }

    public ImageStraighten(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void attach() {
        super.attach();
        mGridAlpha = 1f;
        hidesGrid(mOnStartAnimDelay);
    }

    private void hidesGrid(int delay) {
        mAnimator = ValueAnimator.ofFloat(1, 0);
        mAnimator.setStartDelay(delay);
        mAnimator.setDuration(mAnimDelay);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mGridAlpha = ((Float) animation.getAnimatedValue());
                invalidate();
            }
        });
        mAnimator.start();
    }

    public void setFilterStraightenRepresentation(FilterStraightenRepresentation rep) {
        mLocalRep = (rep == null) ? new FilterStraightenRepresentation() : rep;
        mInitialAngle = mBaseAngle = mAngle = mLocalRep.getStraighten();
    }

    public Collection<FilterRepresentation> getFinalRepresentation() {
        ArrayList<FilterRepresentation> reps = new ArrayList<FilterRepresentation>(2);
        reps.add(mLocalRep);
        if (mInitialAngle != mLocalRep.getStraighten()) {
            reps.add(new FilterCropRepresentation(mCrop));
        }
        return reps;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case (MotionEvent.ACTION_DOWN):
                if (mState == MODES.NONE) {
                    mTouchCenterX = x;
                    mTouchCenterY = y;
                    mCurrentX = x;
                    mCurrentY = y;
                    mState = MODES.MOVE;
                    mBaseAngle = mAngle;
                }
                break;
            case (MotionEvent.ACTION_UP):
                if (mState == MODES.MOVE) {
                    mState = MODES.NONE;
                    mCurrentX = x;
                    mCurrentY = y;
                    computeValue();
                    mFirstDrawSinceUp = true;
                    hidesGrid(0);
                }
                break;
            case (MotionEvent.ACTION_MOVE):
                if (mState == MODES.MOVE) {
                    mCurrentX = x;
                    mCurrentY = y;
                    computeValue();
                }
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    private static float angleFor(float dx, float dy) {
        return (float) (Math.atan2(dx, dy) * 180 / Math.PI);
    }

    private float getCurrentTouchAngle() {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        if (mCurrentX == mTouchCenterX && mCurrentY == mTouchCenterY) {
            return 0;
        }
        float dX1 = mTouchCenterX - centerX;
        float dY1 = mTouchCenterY - centerY;
        float dX2 = mCurrentX - centerX;
        float dY2 = mCurrentY - centerY;
        float angleA = angleFor(dX1, dY1);
        float angleB = angleFor(dX2, dY2);
        return (angleB - angleA) % 360;
    }

    private void computeValue() {
        float angle = getCurrentTouchAngle();
        mAngle = (mBaseAngle - angle) % 360;
        mAngle = Math.max(MIN_STRAIGHTEN_ANGLE, mAngle);
        mAngle = Math.min(MAX_STRAIGHTEN_ANGLE, mAngle);
    }

    public static void getUntranslatedStraightenCropBounds(RectF outRect, float straightenAngle) {
        float deg = straightenAngle;
        if (deg < 0) {
            deg = -deg;
        }
        double a = Math.toRadians(deg);
        double sina = Math.sin(a);
        double cosa = Math.cos(a);
        double rw = outRect.width();
        double rh = outRect.height();
        double h1 = rh * rh / (rw * sina + rh * cosa);
        double h2 = rh * rw / (rw * cosa + rh * sina);
        double hh = Math.min(h1, h2);
        double ww = hh * rw / rh;
        float left = (float) ((rw - ww) * 0.5f);
        float top = (float) ((rh - hh) * 0.5f);
        float right = (float) (left + ww);
        float bottom = (float) (top + hh);
        outRect.set(left, top, right, bottom);
    }

    private void updateCurrentCrop(Matrix m, GeometryHolder h, RectF tmp, int imageWidth,
            int imageHeight, int viewWidth, int viewHeight) {
        tmp.set(0, 0, imageHeight, imageWidth);
        m.mapRect(tmp);
        float top = tmp.top;
        float bottom = tmp.bottom;
        float left = tmp.left;
        float right = tmp.right;
        m.mapRect(tmp);
        int iw,ih;
        if (GeometryMathUtils.needsDimensionSwap(h.rotation)) {
            tmp.set(0, 0, imageHeight, imageWidth);
            iw = imageHeight;
            ih = imageWidth;
        } else {
            tmp.set(0, 0, imageWidth, imageHeight);
            iw = imageWidth;
            ih = imageHeight;
        }
        float scale = GeometryMathUtils.scale(iw, ih, viewWidth, viewHeight);
        scale *= GeometryMathUtils.SHOW_SCALE;
        GeometryMathUtils.scaleRect(tmp, scale);
        getUntranslatedStraightenCropBounds(tmp, mAngle);
        tmp.offset(viewWidth / 2f - tmp.centerX(), viewHeight / 2f - tmp.centerY());
        h.straighten = 0;
        Matrix m1 = GeometryMathUtils.getFullGeometryToScreenMatrix(h, imageWidth,
                imageHeight, viewWidth, viewHeight);
        m.reset();
        m1.invert(m);
        mCrop.set(tmp);
        m.mapRect(mCrop);
        FilterCropRepresentation.findNormalizedCrop(mCrop, imageWidth, imageHeight);
    }


    @Override
    public void onDraw(Canvas canvas) {
        MasterImage master = MasterImage.getImage();
        Bitmap image = master.getFiltersOnlyImage();
        if (image == null) {
            MasterImage.getImage().invalidateFiltersOnly();
            return;
        }
        GeometryMathUtils.initializeHolder(mDrawHolder, mLocalRep);
        mDrawHolder.straighten = mAngle;
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int viewWidth = canvas.getWidth();
        int viewHeight = canvas.getHeight();

        // Get matrix for drawing bitmap
        Matrix m = GeometryMathUtils.getFullGeometryToScreenMatrix(mDrawHolder, imageWidth,
                imageHeight, viewWidth, viewHeight);
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        canvas.drawBitmap(image, m, mPaint);

        mPaint.setFilterBitmap(false);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        updateCurrentCrop(m, mDrawHolder, mDrawRect, imageWidth,
                imageHeight, viewWidth, viewHeight);
        if (mFirstDrawSinceUp) {
            mPriorCropAtUp.set(mCrop);
            mLocalRep.setStraighten(mAngle);
            mFirstDrawSinceUp = false;
        }
        CropDrawingUtils.drawShade(canvas, mDrawRect);
        // Draw the grid
        if (mState == MODES.MOVE || mGridAlpha > 0) {
            canvas.save();
            canvas.clipRect(mDrawRect);

            float step = Math.max(viewWidth, viewHeight) / NBLINES;
            float p = 0;
            for (int i = 1; i < NBLINES; i++) {
                p = i * step;
                int alpha = (int) (mDefaultGridAlpha * mGridAlpha);
                if (alpha == 0 && mState == MODES.MOVE) {
                    alpha = mDefaultGridAlpha;
                }
                mPaint.setAlpha(alpha);
                canvas.drawLine(p, 0, p, viewHeight, mPaint);
                canvas.drawLine(0, p, viewWidth, p, mPaint);
            }
            canvas.restore();
        }
        mPaint.reset();
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(3);
        mDrawPath.reset();


        mDrawPath.addRect(mDrawRect, Path.Direction.CW);
        canvas.drawPath(mDrawPath, mPaint);
    }

    public void setEditor(EditorStraighten editorStraighten) {
        mEditorStraighten = editorStraighten;
    }

}
