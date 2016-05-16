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
import android.graphics.Matrix;
import android.graphics.RectF;

import java.util.Vector;

public class ImageFilterRedEye extends ImageFilter {
    private static final String LOGTAG = "ImageFilterRedEye";
    FilterRedEyeRepresentation mParameters = new FilterRedEyeRepresentation();

    public ImageFilterRedEye() {
        mName = "Red Eye";
    }

    @Override
    public FilterRepresentation getDefaultRepresentation() {
        return new FilterRedEyeRepresentation();
    }

    public boolean isNil() {
        return mParameters.isNil();
    }

    public Vector<FilterPoint> getCandidates() {
        return mParameters.getCandidates();
    }

    public void clear() {
        mParameters.clearCandidates();
    }

    native protected void nativeApplyFilter(Bitmap bitmap, int w, int h, short[] matrix);

    @Override
    public void useRepresentation(FilterRepresentation representation) {
        FilterRedEyeRepresentation parameters = (FilterRedEyeRepresentation) representation;
        mParameters = parameters;
    }

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        short[] rect = new short[4];

        int size = mParameters.getNumberOfCandidates();
        Matrix originalToScreen = getOriginalToScreenMatrix(w, h);
        for (int i = 0; i < size; i++) {
            RectF r = new RectF(((RedEyeCandidate) (mParameters.getCandidate(i))).mRect);
            originalToScreen.mapRect(r);
            if (r.intersect(0, 0, w, h)) {
                rect[0] = (short) r.left;
                rect[1] = (short) r.top;
                rect[2] = (short) r.width();
                rect[3] = (short) r.height();
                nativeApplyFilter(bitmap, w, h, rect);
            }
        }
        return bitmap;
    }
}
