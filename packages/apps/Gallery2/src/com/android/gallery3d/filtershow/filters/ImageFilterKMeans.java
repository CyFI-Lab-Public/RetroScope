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
import android.text.format.Time;

import com.android.gallery3d.R;

public class ImageFilterKMeans extends SimpleImageFilter {
    private static final String SERIALIZATION_NAME = "KMEANS";
    private int mSeed = 0;

    public ImageFilterKMeans() {
        mName = "KMeans";

        // set random seed for session
        Time t = new Time();
        t.setToNow();
        mSeed = (int) t.toMillis(false);
    }

    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation representation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("KMeans");
        representation.setSerializationName(SERIALIZATION_NAME);
        representation.setFilterClass(ImageFilterKMeans.class);
        representation.setMaximum(20);
        representation.setMinimum(2);
        representation.setValue(4);
        representation.setDefaultValue(4);
        representation.setPreviewValue(4);
        representation.setTextId(R.string.kmeans);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    native protected void nativeApplyFilter(Bitmap bitmap, int width, int height,
            Bitmap large_ds_bm, int lwidth, int lheight, Bitmap small_ds_bm,
            int swidth, int sheight, int p, int seed);

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() == null) {
            return bitmap;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Bitmap large_bm_ds = bitmap;
        Bitmap small_bm_ds = bitmap;

        // find width/height for larger downsampled bitmap
        int lw = w;
        int lh = h;
        while (lw > 256 && lh > 256) {
            lw /= 2;
            lh /= 2;
        }
        if (lw != w) {
            large_bm_ds = Bitmap.createScaledBitmap(bitmap, lw, lh, true);
        }

        // find width/height for smaller downsampled bitmap
        int sw = lw;
        int sh = lh;
        while (sw > 64 && sh > 64) {
            sw /= 2;
            sh /= 2;
        }
        if (sw != lw) {
            small_bm_ds = Bitmap.createScaledBitmap(large_bm_ds, sw, sh, true);
        }

        if (getParameters() != null) {
            int p = Math.max(getParameters().getValue(), getParameters().getMinimum()) % (getParameters().getMaximum() + 1);
            nativeApplyFilter(bitmap, w, h, large_bm_ds, lw, lh, small_bm_ds, sw, sh, p, mSeed);
        }
        return bitmap;
    }
}
