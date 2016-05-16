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
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.android.gallery3d.filtershow.editors.EditorVignette;
import com.android.gallery3d.filtershow.filters.FilterVignetteRepresentation;

public class ImageVignette extends ImageShow {
    private static final String LOGTAG = "ImageVignette";

    private FilterVignetteRepresentation mVignetteRep;
    private EditorVignette mEditorVignette;
    private OvalSpaceAdapter mScreenOval = new OvalSpaceAdapter();
    private int mActiveHandle = -1;

    EclipseControl mElipse;

    public ImageVignette(Context context) {
        super(context);
        mElipse = new EclipseControl(context);
    }

    public ImageVignette(Context context, AttributeSet attrs) {
        super(context, attrs);
        mElipse = new EclipseControl(context);
    }

    static class OvalSpaceAdapter implements Oval {
        private Oval mOval;
        Matrix mToScr;
        Matrix mToImage;
        int mImgWidth;
        int mImgHeight;
        float[] mTmp = new float[2];
        float mTmpRadiusX;
        float mTmpRadiusY;

        public void setImageOval(Oval oval) {
            mOval = oval;
        }

        public void setTransform(Matrix toScr, Matrix toImage, int imgWidth, int imgHeight) {
            mToScr = toScr;
            mToImage = toImage;
            mImgWidth = imgWidth;
            mImgHeight = imgHeight;
            mTmpRadiusX = getRadiusX();
            mTmpRadiusY = getRadiusY();
        }

        @Override
        public void setCenter(float x, float y) {
            mTmp[0] = x;
            mTmp[1] = y;
            mToImage.mapPoints(mTmp);
            mOval.setCenter(mTmp[0] / mImgWidth, mTmp[1] / mImgHeight);
        }

        @Override
        public void setRadius(float w, float h) {
            mTmp[0] = mTmpRadiusX = w;
            mTmp[1] = mTmpRadiusY = h;
            mToImage.mapVectors(mTmp);
            mOval.setRadius(mTmp[0] / mImgWidth, mTmp[1] / mImgHeight);
        }

        @Override
        public float getCenterX() {
            mTmp[0] = mOval.getCenterX() * mImgWidth;
            mTmp[1] = mOval.getCenterY() * mImgHeight;
            mToScr.mapPoints(mTmp);

            return mTmp[0];
        }

        @Override
        public float getCenterY() {
            mTmp[0] = mOval.getCenterX() * mImgWidth;
            mTmp[1] = mOval.getCenterY() * mImgHeight;
            mToScr.mapPoints(mTmp);
            return mTmp[1];
        }

        @Override
        public float getRadiusX() {
            mTmp[0] = mOval.getRadiusX() * mImgWidth;
            mTmp[1] = mOval.getRadiusY() * mImgHeight;
            mToScr.mapVectors(mTmp);
            return Math.abs(mTmp[0]);
        }

        @Override
        public float getRadiusY() {
            mTmp[0] = mOval.getRadiusX() * mImgWidth;
            mTmp[1] = mOval.getRadiusY() * mImgHeight;
            mToScr.mapVectors(mTmp);
            return Math.abs(mTmp[1]);
        }

        @Override
        public void setRadiusY(float y) {
            mTmp[0] = mTmpRadiusX;
            mTmp[1] = mTmpRadiusY = y;
            mToImage.mapVectors(mTmp);
            mOval.setRadiusX(mTmp[0] / mImgWidth);
            mOval.setRadiusY(mTmp[1] / mImgHeight);
        }

        @Override
        public void setRadiusX(float x) {
            mTmp[0] = mTmpRadiusX = x;
            mTmp[1] = mTmpRadiusY;
            mToImage.mapVectors(mTmp);
            mOval.setRadiusX(mTmp[0] / mImgWidth);
            mOval.setRadiusY(mTmp[1] / mImgHeight);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int w = MasterImage.getImage().getOriginalBounds().width();
        int h = MasterImage.getImage().getOriginalBounds().height();
        int mask = event.getActionMasked();
        if (mActiveHandle == -1) {
            if (MotionEvent.ACTION_DOWN != mask) {
                return super.onTouchEvent(event);
            }
            if (event.getPointerCount() == 1) {
                mActiveHandle = mElipse.getCloseHandle(event.getX(), event.getY());
            }
            if (mActiveHandle == -1) {
                return super.onTouchEvent(event);
            }
        } else {
            switch (mask) {
                case MotionEvent.ACTION_UP:
                    mActiveHandle = -1;
                    break;
                case MotionEvent.ACTION_DOWN:
                    break;
            }
        }
        float x = event.getX();
        float y = event.getY();

        mElipse.setScrImageInfo(new Matrix(),
                MasterImage.getImage().getOriginalBounds());

        boolean didComputeEllipses = false;
        switch (mask) {
            case (MotionEvent.ACTION_DOWN):
                mElipse.actionDown(x, y, mScreenOval);
                break;
            case (MotionEvent.ACTION_UP):
            case (MotionEvent.ACTION_MOVE):

                mElipse.actionMove(mActiveHandle, x, y, mScreenOval);
                setRepresentation(mVignetteRep);
                didComputeEllipses = true;
                break;
        }
        if (!didComputeEllipses) {
            computeEllipses();
        }
        invalidate();
        return true;
    }

    public void setRepresentation(FilterVignetteRepresentation vignetteRep) {
        mVignetteRep = vignetteRep;
        mScreenOval.setImageOval(mVignetteRep);
        computeEllipses();
    }

    public void computeEllipses() {
        if (mVignetteRep == null) {
            return;
        }
        float w = MasterImage.getImage().getOriginalBounds().width();
        float h = MasterImage.getImage().getOriginalBounds().height();
        Matrix toImg = getScreenToImageMatrix(false);
        Matrix toScr = new Matrix();
        toImg.invert(toScr);
        mScreenOval.setTransform(toScr, toImg, (int) w, (int) h);

        mElipse.setCenter(mScreenOval.getCenterX(), mScreenOval.getCenterY());
        mElipse.setRadius(mScreenOval.getRadiusX(), mScreenOval.getRadiusY());

        mEditorVignette.commitLocalRepresentation();
    }

    public void setEditor(EditorVignette editorVignette) {
        mEditorVignette = editorVignette;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeEllipses();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mVignetteRep == null) {
            return;
        }
        float w = MasterImage.getImage().getOriginalBounds().width();
        float h = MasterImage.getImage().getOriginalBounds().height();
        Matrix toImg = getScreenToImageMatrix(false);
        Matrix toScr = new Matrix();
        toImg.invert(toScr);
        mScreenOval.setTransform(toScr, toImg, (int) w, (int) h);
        mElipse.setCenter(mScreenOval.getCenterX(), mScreenOval.getCenterY());
        mElipse.setRadius(mScreenOval.getRadiusX(), mScreenOval.getRadiusY());

        mElipse.draw(canvas);
    }

}
