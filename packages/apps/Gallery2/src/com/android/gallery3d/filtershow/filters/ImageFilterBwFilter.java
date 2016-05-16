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

import com.android.gallery3d.R;

import android.graphics.Bitmap;
import android.graphics.Color;


public class ImageFilterBwFilter extends SimpleImageFilter {
    private static final String SERIALIZATION_NAME = "BWFILTER";

    public ImageFilterBwFilter() {
        mName = "BW Filter";
    }

    public FilterRepresentation getDefaultRepresentation() {
        FilterBasicRepresentation representation = (FilterBasicRepresentation) super.getDefaultRepresentation();
        representation.setName("BW Filter");
        representation.setSerializationName(SERIALIZATION_NAME);

        representation.setFilterClass(ImageFilterBwFilter.class);
        representation.setMaximum(180);
        representation.setMinimum(-180);
        representation.setTextId(R.string.bwfilter);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    native protected void nativeApplyFilter(Bitmap bitmap, int w, int h, int r, int g, int b);

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() == null) {
            return bitmap;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float[] hsv = new float[] {
                180 + getParameters().getValue(), 1, 1
        };
        int rgb = Color.HSVToColor(hsv);
        int r = 0xFF & (rgb >> 16);
        int g = 0xFF & (rgb >> 8);
        int b = 0xFF & (rgb >> 0);
        nativeApplyFilter(bitmap, w, h, r, g, b);
        return bitmap;
    }
}
