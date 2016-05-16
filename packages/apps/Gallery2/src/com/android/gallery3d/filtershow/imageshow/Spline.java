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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.Collections;
import java.util.Vector;

public class Spline {
    private final Vector<ControlPoint> mPoints;
    private static Drawable mCurveHandle;
    private static int mCurveHandleSize;
    private static int mCurveWidth;

    public static final int RGB = 0;
    public static final int RED = 1;
    public static final int GREEN = 2;
    public static final int BLUE = 3;
    private static final String LOGTAG = "Spline";

    private final Paint gPaint = new Paint();
    private ControlPoint mCurrentControlPoint = null;

    public Spline() {
        mPoints = new Vector<ControlPoint>();
    }

    public Spline(Spline spline) {
        mPoints = new Vector<ControlPoint>();
        for (int i = 0; i < spline.mPoints.size(); i++) {
            ControlPoint p = spline.mPoints.elementAt(i);
            ControlPoint newPoint = new ControlPoint(p);
            mPoints.add(newPoint);
            if (spline.mCurrentControlPoint == p) {
                mCurrentControlPoint = newPoint;
            }
        }
        Collections.sort(mPoints);
    }

    public static void setCurveHandle(Drawable drawable, int size) {
        mCurveHandle = drawable;
        mCurveHandleSize = size;
    }

    public static void setCurveWidth(int width) {
        mCurveWidth = width;
    }

    public static int curveHandleSize() {
        return mCurveHandleSize;
    }

    public static int colorForCurve(int curveIndex) {
        switch (curveIndex) {
            case Spline.RED:
                return Color.RED;
            case GREEN:
                return Color.GREEN;
            case BLUE:
                return Color.BLUE;
        }
        return Color.WHITE;
    }

    public boolean sameValues(Spline other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }

        if (getNbPoints() != other.getNbPoints()) {
            return false;
        }

        for (int i = 0; i < getNbPoints(); i++) {
            ControlPoint p = mPoints.elementAt(i);
            ControlPoint otherPoint = other.mPoints.elementAt(i);
            if (!p.sameValues(otherPoint)) {
                return false;
            }
        }
        return true;
    }

    private void didMovePoint(ControlPoint point) {
        mCurrentControlPoint = point;
    }

    public void movePoint(int pick, float x, float y) {
        if (pick < 0 || pick > mPoints.size() - 1) {
            return;
        }
        ControlPoint point = mPoints.elementAt(pick);
        point.x = x;
        point.y = y;
        didMovePoint(point);
    }

    public boolean isOriginal() {
        if (this.getNbPoints() != 2) {
            return false;
        }
        if (mPoints.elementAt(0).x != 0 || mPoints.elementAt(0).y != 1) {
            return false;
        }
        if (mPoints.elementAt(1).x != 1 || mPoints.elementAt(1).y != 0) {
            return false;
        }
        return true;
    }

    public void reset() {
        mPoints.clear();
        addPoint(0.0f, 1.0f);
        addPoint(1.0f, 0.0f);
    }

    private void drawHandles(Canvas canvas, Drawable indicator, float centerX, float centerY) {
        int left = (int) centerX - mCurveHandleSize / 2;
        int top = (int) centerY - mCurveHandleSize / 2;
        indicator.setBounds(left, top, left + mCurveHandleSize, top + mCurveHandleSize);
        indicator.draw(canvas);
    }

    public float[] getAppliedCurve() {
        float[] curve = new float[256];
        ControlPoint[] points = new ControlPoint[mPoints.size()];
        for (int i = 0; i < mPoints.size(); i++) {
            ControlPoint p = mPoints.get(i);
            points[i] = new ControlPoint(p.x, p.y);
        }
        double[] derivatives = solveSystem(points);
        int start = 0;
        int end = 256;
        if (points[0].x != 0) {
            start = (int) (points[0].x * 256);
        }
        if (points[points.length - 1].x != 1) {
            end = (int) (points[points.length - 1].x * 256);
        }
        for (int i = 0; i < start; i++) {
            curve[i] = 1.0f - points[0].y;
        }
        for (int i = end; i < 256; i++) {
            curve[i] = 1.0f - points[points.length - 1].y;
        }
        for (int i = start; i < end; i++) {
            ControlPoint cur = null;
            ControlPoint next = null;
            double x = i / 256.0;
            int pivot = 0;
            for (int j = 0; j < points.length - 1; j++) {
                if (x >= points[j].x && x <= points[j + 1].x) {
                    pivot = j;
                }
            }
            cur = points[pivot];
            next = points[pivot + 1];
            if (x <= next.x) {
                double x1 = cur.x;
                double x2 = next.x;
                double y1 = cur.y;
                double y2 = next.y;

                // Use the second derivatives to apply the cubic spline
                // equation:
                double delta = (x2 - x1);
                double delta2 = delta * delta;
                double b = (x - x1) / delta;
                double a = 1 - b;
                double ta = a * y1;
                double tb = b * y2;
                double tc = (a * a * a - a) * derivatives[pivot];
                double td = (b * b * b - b) * derivatives[pivot + 1];
                double y = ta + tb + (delta2 / 6) * (tc + td);
                if (y > 1.0f) {
                    y = 1.0f;
                }
                if (y < 0) {
                    y = 0;
                }
                curve[i] = (float) (1.0f - y);
            } else {
                curve[i] = 1.0f - next.y;
            }
        }
        return curve;
    }

    private void drawGrid(Canvas canvas, float w, float h) {
        // Grid
        gPaint.setARGB(128, 150, 150, 150);
        gPaint.setStrokeWidth(1);

        float stepH = h / 9;
        float stepW = w / 9;

        // central diagonal
        gPaint.setARGB(255, 100, 100, 100);
        gPaint.setStrokeWidth(2);
        canvas.drawLine(0, h, w, 0, gPaint);

        gPaint.setARGB(128, 200, 200, 200);
        gPaint.setStrokeWidth(4);
        stepH = h / 3;
        stepW = w / 3;
        for (int j = 1; j < 3; j++) {
            canvas.drawLine(0, j * stepH, w, j * stepH, gPaint);
            canvas.drawLine(j * stepW, 0, j * stepW, h, gPaint);
        }
        canvas.drawLine(0, 0, 0, h, gPaint);
        canvas.drawLine(w, 0, w, h, gPaint);
        canvas.drawLine(0, 0, w, 0, gPaint);
        canvas.drawLine(0, h, w, h, gPaint);
    }

    public void draw(Canvas canvas, int color, int canvasWidth, int canvasHeight,
            boolean showHandles, boolean moving) {
        float w = canvasWidth - mCurveHandleSize;
        float h = canvasHeight - mCurveHandleSize;
        float dx = mCurveHandleSize / 2;
        float dy = mCurveHandleSize / 2;

        // The cubic spline equation is (from numerical recipes in C):
        // y = a(y_i) + b(y_i+1) + c(y"_i) + d(y"_i+1)
        //
        // with c(y"_i) and d(y"_i+1):
        // c(y"_i) = 1/6 (a^3 - a) delta^2 (y"_i)
        // d(y"_i_+1) = 1/6 (b^3 - b) delta^2 (y"_i+1)
        //
        // and delta:
        // delta = x_i+1 - x_i
        //
        // To find the second derivatives y", we can rearrange the equation as:
        // A(y"_i-1) + B(y"_i) + C(y"_i+1) = D
        //
        // With the coefficients A, B, C, D:
        // A = 1/6 (x_i - x_i-1)
        // B = 1/3 (x_i+1 - x_i-1)
        // C = 1/6 (x_i+1 - x_i)
        // D = (y_i+1 - y_i)/(x_i+1 - x_i) - (y_i - y_i-1)/(x_i - x_i-1)
        //
        // We can now easily solve the equation to find the second derivatives:
        ControlPoint[] points = new ControlPoint[mPoints.size()];
        for (int i = 0; i < mPoints.size(); i++) {
            ControlPoint p = mPoints.get(i);
            points[i] = new ControlPoint(p.x * w, p.y * h);
        }
        double[] derivatives = solveSystem(points);

        Path path = new Path();
        path.moveTo(0, points[0].y);
        for (int i = 0; i < points.length - 1; i++) {
            double x1 = points[i].x;
            double x2 = points[i + 1].x;
            double y1 = points[i].y;
            double y2 = points[i + 1].y;

            for (double x = x1; x < x2; x += 20) {
                // Use the second derivatives to apply the cubic spline
                // equation:
                double delta = (x2 - x1);
                double delta2 = delta * delta;
                double b = (x - x1) / delta;
                double a = 1 - b;
                double ta = a * y1;
                double tb = b * y2;
                double tc = (a * a * a - a) * derivatives[i];
                double td = (b * b * b - b) * derivatives[i + 1];
                double y = ta + tb + (delta2 / 6) * (tc + td);
                if (y > h) {
                    y = h;
                }
                if (y < 0) {
                    y = 0;
                }
                path.lineTo((float) x, (float) y);
            }
        }
        canvas.save();
        canvas.translate(dx, dy);
        drawGrid(canvas, w, h);
        ControlPoint lastPoint = points[points.length - 1];
        path.lineTo(lastPoint.x, lastPoint.y);
        path.lineTo(w, lastPoint.y);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        int curveWidth = mCurveWidth;
        if (showHandles) {
            curveWidth *= 1.5;
        }
        paint.setStrokeWidth(curveWidth + 2);
        paint.setColor(Color.BLACK);
        canvas.drawPath(path, paint);

        if (moving && mCurrentControlPoint != null) {
            float px = mCurrentControlPoint.x * w;
            float py = mCurrentControlPoint.y * h;
            paint.setStrokeWidth(3);
            paint.setColor(Color.BLACK);
            canvas.drawLine(px, py, px, h, paint);
            canvas.drawLine(0, py, px, py, paint);
            paint.setStrokeWidth(1);
            paint.setColor(color);
            canvas.drawLine(px, py, px, h, paint);
            canvas.drawLine(0, py, px, py, paint);
        }

        paint.setStrokeWidth(curveWidth);
        paint.setColor(color);
        canvas.drawPath(path, paint);
        if (showHandles) {
            for (int i = 0; i < points.length; i++) {
                float x = points[i].x;
                float y = points[i].y;
                drawHandles(canvas, mCurveHandle, x, y);
            }
        }
        canvas.restore();
    }

    double[] solveSystem(ControlPoint[] points) {
        int n = points.length;
        double[][] system = new double[n][3];
        double[] result = new double[n]; // d
        double[] solution = new double[n]; // returned coefficients
        system[0][1] = 1;
        system[n - 1][1] = 1;
        double d6 = 1.0 / 6.0;
        double d3 = 1.0 / 3.0;

        // let's create a tridiagonal matrix representing the
        // system, and apply the TDMA algorithm to solve it
        // (see http://en.wikipedia.org/wiki/Tridiagonal_matrix_algorithm)
        for (int i = 1; i < n - 1; i++) {
            double deltaPrevX = points[i].x - points[i - 1].x;
            double deltaX = points[i + 1].x - points[i - 1].x;
            double deltaNextX = points[i + 1].x - points[i].x;
            double deltaNextY = points[i + 1].y - points[i].y;
            double deltaPrevY = points[i].y - points[i - 1].y;
            system[i][0] = d6 * deltaPrevX; // a_i
            system[i][1] = d3 * deltaX; // b_i
            system[i][2] = d6 * deltaNextX; // c_i
            result[i] = (deltaNextY / deltaNextX) - (deltaPrevY / deltaPrevX); // d_i
        }

        // Forward sweep
        for (int i = 1; i < n; i++) {
            // m = a_i/b_i-1
            double m = system[i][0] / system[i - 1][1];
            // b_i = b_i - m(c_i-1)
            system[i][1] = system[i][1] - m * system[i - 1][2];
            // d_i = d_i - m(d_i-1)
            result[i] = result[i] - m * result[i - 1];
        }

        // Back substitution
        solution[n - 1] = result[n - 1] / system[n - 1][1];
        for (int i = n - 2; i >= 0; --i) {
            solution[i] = (result[i] - system[i][2] * solution[i + 1]) / system[i][1];
        }
        return solution;
    }

    public int addPoint(float x, float y) {
        return addPoint(new ControlPoint(x, y));
    }

    public int addPoint(ControlPoint v) {
        mPoints.add(v);
        Collections.sort(mPoints);
        return mPoints.indexOf(v);
    }

    public void deletePoint(int n) {
        mPoints.remove(n);
        if (mPoints.size() < 2) {
            reset();
        }
        Collections.sort(mPoints);
    }

    public int getNbPoints() {
        return mPoints.size();
    }

    public ControlPoint getPoint(int n) {
        return mPoints.elementAt(n);
    }

    public boolean isPointContained(float x, int n) {
        for (int i = 0; i < n; i++) {
            ControlPoint point = mPoints.elementAt(i);
            if (point.x > x) {
                return false;
            }
        }
        for (int i = n + 1; i < mPoints.size(); i++) {
            ControlPoint point = mPoints.elementAt(i);
            if (point.x < x) {
                return false;
            }
        }
        return true;
    }

    public Spline copy() {
        Spline spline = new Spline();
        for (int i = 0; i < mPoints.size(); i++) {
            ControlPoint point = mPoints.elementAt(i);
            spline.addPoint(point.copy());
        }
        return spline;
    }

    public void show() {
        Log.v(LOGTAG, "show curve " + this);
        for (int i = 0; i < mPoints.size(); i++) {
            ControlPoint point = mPoints.elementAt(i);
            Log.v(LOGTAG, "point " + i + " is (" + point.x + ", " + point.y + ")");
        }
    }

}
