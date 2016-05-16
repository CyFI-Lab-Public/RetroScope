package com.android.gallery3d.filtershow.imageshow;
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.EditorGrad;
import com.android.gallery3d.filtershow.filters.FilterGradRepresentation;

public class ImageGrad extends ImageShow {
    private static final String LOGTAG = "ImageGrad";
    private FilterGradRepresentation mGradRep;
    private EditorGrad mEditorGrad;
    private float mMinTouchDist;
    private int mActiveHandle = -1;
    private GradControl mEllipse;

    Matrix mToScr = new Matrix();
    float[] mPointsX = new float[FilterGradRepresentation.MAX_POINTS];
    float[] mPointsY = new float[FilterGradRepresentation.MAX_POINTS];

    public ImageGrad(Context context) {
        super(context);
        Resources res = context.getResources();
        mMinTouchDist = res.getDimensionPixelSize(R.dimen.gradcontrol_min_touch_dist);
        mEllipse = new GradControl(context);
        mEllipse.setShowReshapeHandles(false);
    }

    public ImageGrad(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        mMinTouchDist = res.getDimensionPixelSize(R.dimen.gradcontrol_min_touch_dist);
        mEllipse = new GradControl(context);
        mEllipse.setShowReshapeHandles(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int mask = event.getActionMasked();

        if (mActiveHandle == -1) {
            if (MotionEvent.ACTION_DOWN != mask) {
                return super.onTouchEvent(event);
            }
            if (event.getPointerCount() == 1) {
                mActiveHandle = mEllipse.getCloseHandle(event.getX(), event.getY());
                if (mActiveHandle == -1) {
                    float x = event.getX();
                    float y = event.getY();
                    float min_d = Float.MAX_VALUE;
                    int pos = -1;
                    for (int i = 0; i < mPointsX.length; i++) {
                        if (mPointsX[i] == -1) {
                            continue;
                        }
                        float d = (float) Math.hypot(x - mPointsX[i], y - mPointsY[i]);
                        if ( min_d > d) {
                            min_d = d;
                            pos = i;
                        }
                    }
                    if (min_d > mMinTouchDist){
                        pos = -1;
                    }

                    if (pos != -1) {
                        mGradRep.setSelectedPoint(pos);
                        resetImageCaches(this);
                        mEditorGrad.updateSeekBar(mGradRep);
                        mEditorGrad.commitLocalRepresentation();
                        invalidate();
                    }
                }
            }
            if (mActiveHandle == -1) {
                return super.onTouchEvent(event);
            }
        } else {
            switch (mask) {
                case MotionEvent.ACTION_UP: {

                    mActiveHandle = -1;
                    break;
                }
                case MotionEvent.ACTION_DOWN: {
                    break;
                }
            }
        }
        float x = event.getX();
        float y = event.getY();

        mEllipse.setScrImageInfo(getScreenToImageMatrix(true),
                MasterImage.getImage().getOriginalBounds());

        switch (mask) {
            case (MotionEvent.ACTION_DOWN): {
                mEllipse.actionDown(x, y, mGradRep);
                break;
            }
            case (MotionEvent.ACTION_UP):
            case (MotionEvent.ACTION_MOVE): {
                mEllipse.actionMove(mActiveHandle, x, y, mGradRep);
                setRepresentation(mGradRep);
                break;
            }
        }
        invalidate();
        mEditorGrad.commitLocalRepresentation();
        return true;
    }

    public void setRepresentation(FilterGradRepresentation pointRep) {
        mGradRep = pointRep;
        Matrix toImg = getScreenToImageMatrix(false);

        toImg.invert(mToScr);

        float[] c1 = new float[] { mGradRep.getPoint1X(), mGradRep.getPoint1Y() };
        float[] c2 = new float[] { mGradRep.getPoint2X(), mGradRep.getPoint2Y() };

        if (c1[0] == -1) {
            float cx = MasterImage.getImage().getOriginalBounds().width() / 2;
            float cy = MasterImage.getImage().getOriginalBounds().height() / 2;
            float rx = Math.min(cx, cy) * .4f;

            mGradRep.setPoint1(cx, cy-rx);
            mGradRep.setPoint2(cx, cy+rx);
            c1[0] = cx;
            c1[1] = cy-rx;
            mToScr.mapPoints(c1);
            if (getWidth() != 0) {
                mEllipse.setPoint1(c1[0], c1[1]);
                c2[0] = cx;
                c2[1] = cy+rx;
                mToScr.mapPoints(c2);
                mEllipse.setPoint2(c2[0], c2[1]);
            }
            mEditorGrad.commitLocalRepresentation();
        } else {
            mToScr.mapPoints(c1);
            mToScr.mapPoints(c2);
            mEllipse.setPoint1(c1[0], c1[1]);
            mEllipse.setPoint2(c2[0], c2[1]);
        }
    }

    public void drawOtherPoints(Canvas canvas) {
        computCenterLocations();
        for (int i = 0; i < mPointsX.length; i++) {
            if (mPointsX[i] != -1) {
                mEllipse.paintGrayPoint(canvas, mPointsX[i], mPointsY[i]);
            }
        }
    }

    public void computCenterLocations() {
        int x1[] = mGradRep.getXPos1();
        int y1[] = mGradRep.getYPos1();
        int x2[] = mGradRep.getXPos2();
        int y2[] = mGradRep.getYPos2();
        int selected = mGradRep.getSelectedPoint();
        boolean m[] = mGradRep.getMask();
        float[] c = new float[2];
        for (int i = 0; i < m.length; i++) {
            if (selected == i || !m[i]) {
                mPointsX[i] = -1;
                continue;
            }

            c[0] = (x1[i]+x2[i])/2;
            c[1] = (y1[i]+y2[i])/2;
            mToScr.mapPoints(c);

            mPointsX[i] = c[0];
            mPointsY[i] = c[1];
        }
    }

    public void setEditor(EditorGrad editorGrad) {
        mEditorGrad = editorGrad;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mGradRep == null) {
            return;
        }
        setRepresentation(mGradRep);
        mEllipse.draw(canvas);
        drawOtherPoints(canvas);
    }

}
