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

package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class ImageFilterColorBorder extends ImageFilter {
    private static final String LOGTAG = "ImageFilterColorBorder";
    private FilterColorBorderRepresentation mParameters = null;
    Paint mPaint = new Paint();
    RectF mBounds = new RectF();
    RectF mInsideBounds = new RectF();
    Path mBorderPath = new Path();

    public ImageFilterColorBorder() {
        mName = "Border";
    }

    public FilterRepresentation getDefaultRepresentation() {
        return new FilterColorBorderRepresentation(Color.WHITE, 3, 2);
    }

    public void useRepresentation(FilterRepresentation representation) {
        FilterColorBorderRepresentation parameters =
                (FilterColorBorderRepresentation) representation;
        mParameters = parameters;
    }

    public FilterColorBorderRepresentation getParameters() {
        return mParameters;
    }

    private void applyHelper(Canvas canvas, int w, int h) {
        if (getParameters() == null) {
            return;
        }
        float size = getParameters().getBorderSize();
        float radius = getParameters().getBorderRadius();

        mPaint.reset();
        mPaint.setColor(getParameters().getColor());
        mPaint.setAntiAlias(true);
        mBounds.set(0, 0, w, h);
        mBorderPath.reset();
        mBorderPath.moveTo(0, 0);

        float bs = size / 100.f * mBounds.width();
        float r = radius / 100.f * mBounds.width();

        mInsideBounds.set(mBounds.left + bs,
                mBounds.top + bs, mBounds.right - bs,
                mBounds.bottom - bs);

        mBorderPath.moveTo(mBounds.left, mBounds.top);
        mBorderPath.lineTo(mBounds.right, mBounds.top);
        mBorderPath.lineTo(mBounds.right, mBounds.bottom);
        mBorderPath.lineTo(mBounds.left, mBounds.bottom);
        mBorderPath.addRoundRect(mInsideBounds,
                r, r, Path.Direction.CCW);

        canvas.drawPath(mBorderPath, mPaint);
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        Canvas canvas = new Canvas(bitmap);
        applyHelper(canvas, bitmap.getWidth(), bitmap.getHeight());
        return bitmap;
    }

}
