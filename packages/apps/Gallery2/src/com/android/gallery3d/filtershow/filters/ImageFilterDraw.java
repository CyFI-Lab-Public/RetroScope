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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import com.android.gallery3d.R;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation.StrokeData;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.FilterEnvironment;

import java.util.Vector;

public class ImageFilterDraw extends ImageFilter {
    private static final String LOGTAG = "ImageFilterDraw";
    public final static byte SIMPLE_STYLE = 0;
    public final static byte BRUSH_STYLE_SPATTER = 1;
    public final static byte BRUSH_STYLE_MARKER = 2;
    public final static int NUMBER_OF_STYLES = 3;
    Bitmap mOverlayBitmap; // this accelerates interaction
    int mCachedStrokes = -1;
    int mCurrentStyle = 0;

    FilterDrawRepresentation mParameters = new FilterDrawRepresentation();

    public ImageFilterDraw() {
        mName = "Image Draw";
    }

    DrawStyle[] mDrawingsTypes = new DrawStyle[] {
            new SimpleDraw(0),
            new SimpleDraw(1),
            new Brush(R.drawable.brush_gauss),
            new Brush(R.drawable.brush_marker),
            new Brush(R.drawable.brush_spatter)
    };
    {
        for (int i = 0; i < mDrawingsTypes.length; i++) {
            mDrawingsTypes[i].setType((byte) i);
        }

    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterDrawRepresentation();
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        FilterDrawRepresentation parameters = (FilterDrawRepresentation) representation;
        mParameters = parameters;
    }

    public void setStyle(byte style) {
        mCurrentStyle = style % mDrawingsTypes.length;
    }

    public int getStyle() {
        return mCurrentStyle;
    }

    public static interface DrawStyle {
        public void setType(byte type);
        public void paint(FilterDrawRepresentation.StrokeData sd, Canvas canvas, Matrix toScrMatrix,
                int quality);
    }

    class SimpleDraw implements DrawStyle {
        byte mType;
        int mMode;

        public SimpleDraw(int mode) {
            mMode = mode;
        }

        @Override
        public void setType(byte type) {
            mType = type;
        }

        @Override
        public void paint(FilterDrawRepresentation.StrokeData sd, Canvas canvas, Matrix toScrMatrix,
                int quality) {
            if (sd == null) {
                return;
            }
            if (sd.mPath == null) {
                return;
            }
            Paint paint = new Paint();

            paint.setStyle(Style.STROKE);
            if (mMode == 0) {
                paint.setStrokeCap(Paint.Cap.SQUARE);
            } else {
                paint.setStrokeCap(Paint.Cap.ROUND);
            }
            paint.setAntiAlias(true);
            paint.setColor(sd.mColor);
            paint.setStrokeWidth(toScrMatrix.mapRadius(sd.mRadius));

            // done this way because of a bug in path.transform(matrix)
            Path mCacheTransPath = new Path();
            mCacheTransPath.addPath(sd.mPath, toScrMatrix);

            canvas.drawPath(mCacheTransPath, paint);
        }
    }

    class Brush implements DrawStyle {
        int mBrushID;
        Bitmap mBrush;
        byte mType;

        public Brush(int brushID) {
            mBrushID = brushID;
        }

        public Bitmap getBrush() {
            if (mBrush == null) {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inPreferredConfig = Bitmap.Config.ALPHA_8;
                mBrush = BitmapFactory.decodeResource(MasterImage.getImage().getActivity()
                        .getResources(), mBrushID, opt);
                mBrush = mBrush.extractAlpha();
            }
            return mBrush;
        }

        @Override
        public void paint(FilterDrawRepresentation.StrokeData sd, Canvas canvas,
                Matrix toScrMatrix,
                int quality) {
            if (sd == null || sd.mPath == null) {
                return;
            }
            Paint paint = new Paint();
            paint.setStyle(Style.STROKE);
            paint.setAntiAlias(true);
            Path mCacheTransPath = new Path();
            mCacheTransPath.addPath(sd.mPath, toScrMatrix);
            draw(canvas, paint, sd.mColor, toScrMatrix.mapRadius(sd.mRadius) * 2,
                    mCacheTransPath);
        }

        public Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter)
        {
            Matrix m = new Matrix();
            m.setScale(dstWidth / (float) src.getWidth(), dstHeight / (float) src.getHeight());
            Bitmap result = Bitmap.createBitmap(dstWidth, dstHeight, src.getConfig());
            Canvas canvas = new Canvas(result);

            Paint paint = new Paint();
            paint.setFilterBitmap(filter);
            canvas.drawBitmap(src, m, paint);

            return result;

        }
        void draw(Canvas canvas, Paint paint, int color, float size, Path path) {
            PathMeasure mPathMeasure = new PathMeasure();
            float[] mPosition = new float[2];
            float[] mTan = new float[2];

            mPathMeasure.setPath(path, false);

            paint.setAntiAlias(true);
            paint.setColor(color);

            paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
            Bitmap brush;
            // done this way because of a bug in
            // Bitmap.createScaledBitmap(getBrush(),(int) size,(int) size,true);
            brush = createScaledBitmap(getBrush(), (int) size, (int) size, true);
            float len = mPathMeasure.getLength();
            float s2 = size / 2;
            float step = s2 / 8;
            for (float i = 0; i < len; i += step) {
                mPathMeasure.getPosTan(i, mPosition, mTan);
                //                canvas.drawCircle(pos[0], pos[1], size, paint);
                canvas.drawBitmap(brush, mPosition[0] - s2, mPosition[1] - s2, paint);
            }
        }

        @Override
        public void setType(byte type) {
            mType = type;
        }
    }

    void paint(FilterDrawRepresentation.StrokeData sd, Canvas canvas, Matrix toScrMatrix,
            int quality) {
        mDrawingsTypes[sd.mType].paint(sd, canvas, toScrMatrix, quality);
    }

    public void drawData(Canvas canvas, Matrix originalRotateToScreen, int quality) {
        Paint paint = new Paint();
        if (quality == FilterEnvironment.QUALITY_FINAL) {
            paint.setAntiAlias(true);
        }
        paint.setStyle(Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(40);

        if (mParameters.getDrawing().isEmpty() && mParameters.getCurrentDrawing() == null) {
            mOverlayBitmap = null;
            mCachedStrokes = -1;
            return;
        }
        if (quality == FilterEnvironment.QUALITY_FINAL) {

            for (FilterDrawRepresentation.StrokeData strokeData : mParameters.getDrawing()) {
                paint(strokeData, canvas, originalRotateToScreen, quality);
            }
            return;
        }

        if (mOverlayBitmap == null ||
                mOverlayBitmap.getWidth() != canvas.getWidth() ||
                mOverlayBitmap.getHeight() != canvas.getHeight() ||
                mParameters.getDrawing().size() < mCachedStrokes) {

            mOverlayBitmap = Bitmap.createBitmap(
                    canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            mCachedStrokes = 0;
        }

        if (mCachedStrokes < mParameters.getDrawing().size()) {
            fillBuffer(originalRotateToScreen);
        }
        canvas.drawBitmap(mOverlayBitmap, 0, 0, paint);

        StrokeData stroke = mParameters.getCurrentDrawing();
        if (stroke != null) {
            paint(stroke, canvas, originalRotateToScreen, quality);
        }
    }

    public void fillBuffer(Matrix originalRotateToScreen) {
        Canvas drawCache = new Canvas(mOverlayBitmap);
        Vector<FilterDrawRepresentation.StrokeData> v = mParameters.getDrawing();
        int n = v.size();

        for (int i = mCachedStrokes; i < n; i++) {
            paint(v.get(i), drawCache, originalRotateToScreen, FilterEnvironment.QUALITY_PREVIEW);
        }
        mCachedStrokes = n;
    }

    public void draw(Canvas canvas, Matrix originalRotateToScreen) {
        for (FilterDrawRepresentation.StrokeData strokeData : mParameters.getDrawing()) {
            paint(strokeData, canvas, originalRotateToScreen, FilterEnvironment.QUALITY_PREVIEW);
        }
        mDrawingsTypes[mCurrentStyle].paint(
                null, canvas, originalRotateToScreen, FilterEnvironment.QUALITY_PREVIEW);
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix m = getOriginalToScreenMatrix(w, h);
        drawData(new Canvas(bitmap), m, quality);
        return bitmap;
    }

}
