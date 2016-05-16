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

package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;

import com.android.gallery3d.R;

public class ImageFilterHighlights extends SimpleImageFilter {
    private static final String SERIALIZATION_NAME = "HIGHLIGHTS";
    private static final String LOGTAG = "ImageFilterVignette";

    public ImageFilterHighlights() {
        mName = "Highlights";
    }

    SplineMath mSpline = new SplineMath(5);
    double[] mHighlightCurve = { 0.0, 0.32, 0.418, 0.476, 0.642 };

    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation representation =
                (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("Highlights");
        representation.setSerializationName(SERIALIZATION_NAME);
        representation.setFilterClass(ImageFilterHighlights.class);
        representation.setTextId(R.string.highlight_recovery);
        representation.setMinimum(-100);
        representation.setMaximum(100);
        representation.setDefaultValue(0);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    native protected void nativeApplyFilter(Bitmap bitmap, int w, int h, float[] luminanceMap);

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() == null) {
            return bitmap;
        }
        float p = getParameters().getValue();
        double t = p/100.;
        for (int i = 0; i < 5; i++) {
            double x = i / 4.;
            double y = mHighlightCurve[i] *t+x*(1-t);
            mSpline.setPoint(i, x, y);
        }

        float[][] curve = mSpline.calculatetCurve(256);
        float[] luminanceMap = new float[curve.length];
        for (int i = 0; i < luminanceMap.length; i++) {
            luminanceMap[i] = curve[i][1];
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        nativeApplyFilter(bitmap, w, h, luminanceMap);
        return bitmap;
    }
}
