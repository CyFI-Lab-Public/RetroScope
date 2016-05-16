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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;

import com.android.gallery3d.R;

public class GradControl {
    private float mPoint1X = Float.NaN; // used to flag parameters have not been set
    private float mPoint1Y = 0;
    private float mPoint2X = 200;
    private float mPoint2Y = 300;
    private int mMinTouchDist = 80;// should be a resource & in dips

    private float[] handlex = new float[3];
    private float[] handley = new float[3];
    private int mSliderColor;
    private int mCenterDotSize;
    private float mDownX;
    private float mDownY;
    private float mDownPoint1X;
    private float mDownPoint1Y;
    private float mDownPoint2X;
    private float mDownPoint2Y;
    Rect mImageBounds;
    int mImageHeight;
    private Matrix mScrToImg;
    Paint mPaint = new Paint();
    DashPathEffect mDash = new DashPathEffect(new float[]{30, 30}, 0);
    private boolean mShowReshapeHandles = true;
    public final static int HAN_CENTER = 0;
    public final static int HAN_NORTH = 2;
    public final static int HAN_SOUTH = 1;
    private int[] mPointColorPatern;
    private int[] mGrayPointColorPatern;
    private float[] mPointRadialPos = new float[]{0, .3f, .31f, 1};
    private int mLineColor;
    private int mlineShadowColor;

    public GradControl(Context context) {

        Resources res = context.getResources();
        mCenterDotSize = (int) res.getDimension(R.dimen.gradcontrol_dot_size);
        mMinTouchDist = (int) res.getDimension(R.dimen.gradcontrol_min_touch_dist);
        int grayPointCenterColor = res.getColor(R.color.gradcontrol_graypoint_center);
        int grayPointEdgeColor = res.getColor(R.color.gradcontrol_graypoint_edge);
        int pointCenterColor = res.getColor(R.color.gradcontrol_point_center);
        int pointEdgeColor = res.getColor(R.color.gradcontrol_point_edge);
        int pointShadowStartColor = res.getColor(R.color.gradcontrol_point_shadow_start);
        int pointShadowEndColor = res.getColor(R.color.gradcontrol_point_shadow_end);
        mPointColorPatern = new int[]{
                pointCenterColor, pointEdgeColor, pointShadowStartColor, pointShadowEndColor};
        mGrayPointColorPatern = new int[]{
                grayPointCenterColor, grayPointEdgeColor, pointShadowStartColor, pointShadowEndColor};
        mSliderColor = Color.WHITE;
        mLineColor = res.getColor(R.color.gradcontrol_line_color);
        mlineShadowColor = res.getColor(R.color.gradcontrol_line_shadow);
    }

    public void setPoint2(float x, float y) {
        mPoint2X = x;
        mPoint2Y = y;
    }

    public void setPoint1(float x, float y) {
        mPoint1X = x;
        mPoint1Y = y;
    }

    public int getCloseHandle(float x, float y) {
        float min = Float.MAX_VALUE;
        int handle = -1;
        for (int i = 0; i < handlex.length; i++) {
            float dx = handlex[i] - x;
            float dy = handley[i] - y;
            float dist = dx * dx + dy * dy;
            if (dist < min) {
                min = dist;
                handle = i;
            }
        }

        if (min < mMinTouchDist * mMinTouchDist) {
            return handle;
        }
        for (int i = 0; i < handlex.length; i++) {
            float dx = handlex[i] - x;
            float dy = handley[i] - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
        }

        return -1;
    }

    public void setScrImageInfo(Matrix scrToImg, Rect imageBounds) {
        mScrToImg = scrToImg;
        mImageBounds = new Rect(imageBounds);
    }

    private boolean centerIsOutside(float x1, float y1, float x2, float y2) {
        return (!mImageBounds.contains((int) ((x1 + x2) / 2), (int) ((y1 + y2) / 2)));
    }

    public void actionDown(float x, float y, Line line) {
        float[] point = new float[]{
                x, y};
        mScrToImg.mapPoints(point);
        mDownX = point[0];
        mDownY = point[1];
        mDownPoint1X = line.getPoint1X();
        mDownPoint1Y = line.getPoint1Y();
        mDownPoint2X = line.getPoint2X();
        mDownPoint2Y = line.getPoint2Y();
    }

    public void actionMove(int handle, float x, float y, Line line) {
        float[] point = new float[]{
                x, y};
        mScrToImg.mapPoints(point);
        x = point[0];
        y = point[1];

        // Test if the matrix is swapping x and y
        point[0] = 0;
        point[1] = 1;
        mScrToImg.mapVectors(point);
        boolean swapxy = (point[0] > 0.0f);

        int sign = 1;

        float dx = x - mDownX;
        float dy = y - mDownY;
        switch (handle) {
            case HAN_CENTER:
                if (centerIsOutside(mDownPoint1X + dx, mDownPoint1Y + dy,
                        mDownPoint2X + dx, mDownPoint2Y + dy)) {
                    break;
                }
                line.setPoint1(mDownPoint1X + dx, mDownPoint1Y + dy);
                line.setPoint2(mDownPoint2X + dx, mDownPoint2Y + dy);
                break;
            case HAN_SOUTH:
                if (centerIsOutside(mDownPoint1X + dx, mDownPoint1Y + dy,
                        mDownPoint2X, mDownPoint2Y)) {
                    break;
                }
                line.setPoint1(mDownPoint1X + dx, mDownPoint1Y + dy);
                break;
            case HAN_NORTH:
                if (centerIsOutside(mDownPoint1X, mDownPoint1Y,
                        mDownPoint2X + dx, mDownPoint2Y + dy)) {
                    break;
                }
                line.setPoint2(mDownPoint2X + dx, mDownPoint2Y + dy);
                break;
        }
    }

    public void paintGrayPoint(Canvas canvas, float x, float y) {
        if (isUndefined()) {
            return;
        }

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        RadialGradient g = new RadialGradient(x, y, mCenterDotSize, mGrayPointColorPatern,
                mPointRadialPos, Shader.TileMode.CLAMP);
        paint.setShader(g);
        canvas.drawCircle(x, y, mCenterDotSize, paint);
    }

    public void paintPoint(Canvas canvas, float x, float y) {
        if (isUndefined()) {
            return;
        }

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        RadialGradient g = new RadialGradient(x, y, mCenterDotSize, mPointColorPatern,
                mPointRadialPos, Shader.TileMode.CLAMP);
        paint.setShader(g);
        canvas.drawCircle(x, y, mCenterDotSize, paint);
    }

    void paintLines(Canvas canvas, float p1x, float p1y, float p2x, float p2y) {
        if (isUndefined()) {
            return;
        }

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);

        mPaint.setStrokeWidth(6);
        mPaint.setColor(mlineShadowColor);
        mPaint.setPathEffect(mDash);
        paintOvallines(canvas, mPaint, p1x, p1y, p2x, p2y);

        mPaint.setStrokeWidth(3);
        mPaint.setColor(mLineColor);
        mPaint.setPathEffect(mDash);
        paintOvallines(canvas, mPaint, p1x, p1y, p2x, p2y);
    }

    public void paintOvallines(
            Canvas canvas, Paint paint, float p1x, float p1y, float p2x, float p2y) {



        canvas.drawLine(p1x, p1y, p2x, p2y, paint);

        float cx = (p1x + p2x) / 2;
        float cy = (p1y + p2y) / 2;
        float dx = p1x - p2x;
        float dy = p1y - p2y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        dx *= 2048 / len;
        dy *= 2048 / len;

        canvas.drawLine(p1x + dy, p1y - dx, p1x - dy, p1y + dx, paint);
        canvas.drawLine(p2x + dy, p2y - dx, p2x - dy, p2y + dx, paint);
    }

    public void fillHandles(Canvas canvas, float p1x, float p1y, float p2x, float p2y) {
        float cx = (p1x + p2x) / 2;
        float cy = (p1y + p2y) / 2;
        handlex[0] = cx;
        handley[0] = cy;
        handlex[1] = p1x;
        handley[1] = p1y;
        handlex[2] = p2x;
        handley[2] = p2y;

    }

    public void draw(Canvas canvas) {
        paintLines(canvas, mPoint1X, mPoint1Y, mPoint2X, mPoint2Y);
        fillHandles(canvas, mPoint1X, mPoint1Y, mPoint2X, mPoint2Y);
        paintPoint(canvas, mPoint2X, mPoint2Y);
        paintPoint(canvas, mPoint1X, mPoint1Y);
        paintPoint(canvas, (mPoint1X + mPoint2X) / 2, (mPoint1Y + mPoint2Y) / 2);
    }

    public boolean isUndefined() {
        return Float.isNaN(mPoint1X);
    }

    public void setShowReshapeHandles(boolean showReshapeHandles) {
        this.mShowReshapeHandles = showReshapeHandles;
    }
}
