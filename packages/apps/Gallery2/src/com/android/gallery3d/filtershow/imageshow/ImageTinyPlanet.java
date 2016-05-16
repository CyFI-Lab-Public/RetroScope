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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

import com.android.gallery3d.filtershow.editors.BasicEditor;
import com.android.gallery3d.filtershow.editors.EditorTinyPlanet;
import com.android.gallery3d.filtershow.filters.FilterTinyPlanetRepresentation;

public class ImageTinyPlanet extends ImageShow {
    private static final String LOGTAG = "ImageTinyPlanet";

    private float mTouchCenterX = 0;
    private float mTouchCenterY = 0;
    private float mCurrentX = 0;
    private float mCurrentY = 0;
    private float mCenterX = 0;
    private float mCenterY = 0;
    private float mStartAngle = 0;
    private FilterTinyPlanetRepresentation mTinyPlanetRep;
    private EditorTinyPlanet mEditorTinyPlanet;
    private ScaleGestureDetector mScaleGestureDetector = null;
    boolean mInScale = false;
    RectF mDestRect = new RectF();

    OnScaleGestureListener mScaleGestureListener = new OnScaleGestureListener() {
        private float mScale = 100;
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mInScale = false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mInScale = true;
            mScale = mTinyPlanetRep.getValue();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            int value = mTinyPlanetRep.getValue();
            mScale *= detector.getScaleFactor();
            value = (int) (mScale);
            value = Math.min(mTinyPlanetRep.getMaximum(), value);
            value = Math.max(mTinyPlanetRep.getMinimum(), value);
            mTinyPlanetRep.setValue(value);
            invalidate();
            mEditorTinyPlanet.commitLocalRepresentation();
            mEditorTinyPlanet.updateUI();
            return true;
        }
    };

    public ImageTinyPlanet(Context context) {
        super(context);
        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
    }

    public ImageTinyPlanet(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScaleGestureDetector = new ScaleGestureDetector(context,mScaleGestureListener );
    }

    protected static float angleFor(float dx, float dy) {
        return (float) (Math.atan2(dx, dy) * 180 / Math.PI);
    }

    protected float getCurrentTouchAngle() {
        if (mCurrentX == mTouchCenterX && mCurrentY == mTouchCenterY) {
            return 0;
        }
        float dX1 = mTouchCenterX - mCenterX;
        float dY1 = mTouchCenterY - mCenterY;
        float dX2 = mCurrentX - mCenterX;
        float dY2 = mCurrentY - mCenterY;

        float angleA = angleFor(dX1, dY1);
        float angleB = angleFor(dX2, dY2);
        return (float) (((angleB - angleA) % 360) * Math.PI / 180);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        mCurrentX = x;
        mCurrentY = y;
        mCenterX = getWidth() / 2;
        mCenterY = getHeight() / 2;
        mScaleGestureDetector.onTouchEvent(event);
        if (mInScale) {
            return true;
        }
        switch (event.getActionMasked()) {
            case (MotionEvent.ACTION_DOWN):
                mTouchCenterX = x;
                mTouchCenterY = y;
                mStartAngle = mTinyPlanetRep.getAngle();
                break;

            case (MotionEvent.ACTION_MOVE):
                mTinyPlanetRep.setAngle(mStartAngle + getCurrentTouchAngle());
                break;
        }
        invalidate();
        mEditorTinyPlanet.commitLocalRepresentation();
        return true;
    }

    public void setRepresentation(FilterTinyPlanetRepresentation tinyPlanetRep) {
        mTinyPlanetRep = tinyPlanetRep;
    }

    public void setEditor(BasicEditor editorTinyPlanet) {
        mEditorTinyPlanet = (EditorTinyPlanet) editorTinyPlanet;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Bitmap bitmap = MasterImage.getImage().getHighresImage();
        if (bitmap == null) {
            bitmap = MasterImage.getImage().getFilteredImage();
        }

        if (bitmap != null) {
            display(canvas, bitmap);
        }
    }

    private void display(Canvas canvas, Bitmap bitmap) {
        float sw = canvas.getWidth();
        float sh = canvas.getHeight();
        float iw = bitmap.getWidth();
        float ih = bitmap.getHeight();
        float nsw = sw;
        float nsh = sh;

        if (sw * ih > sh * iw) {
            nsw = sh * iw / ih;
        } else {
            nsh = sw * ih / iw;
        }

        mDestRect.left = (sw - nsw) / 2;
        mDestRect.top = (sh - nsh) / 2;
        mDestRect.right = sw - mDestRect.left;
        mDestRect.bottom = sh - mDestRect.top;

        canvas.drawBitmap(bitmap, null, mDestRect, mPaint);
    }
}
