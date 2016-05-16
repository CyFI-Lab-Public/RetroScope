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
import com.android.gallery3d.filtershow.editors.ImageOnlyEditor;

import android.graphics.Bitmap;

public class ImageFilterWBalance extends ImageFilter {
    private static final String SERIALIZATION_NAME = "WBALANCE";
    private static final String TAG = "ImageFilterWBalance";

    public ImageFilterWBalance() {
        mName = "WBalance";
    }

    public FilterRepresentation getDefaultRepresentation() {
        FilterRepresentation representation = new FilterDirectRepresentation("WBalance");
        representation.setSerializationName(SERIALIZATION_NAME);
        representation.setFilterClass(ImageFilterWBalance.class);
        representation.setFilterType(FilterRepresentation.TYPE_WBALANCE);
        representation.setTextId(R.string.wbalance);
        representation.setShowParameterValue(false);
        representation.setEditorId(ImageOnlyEditor.ID);
        representation.setSupportsPartialRendering(true);
        representation.setIsBooleanFilter(true);
        return representation;
    }

    @Override
    public void useRepresentation(FilterRepresentation representation) {

    }

    native protected void nativeApplyFilter(Bitmap bitmap, int w, int h, int locX, int locY);

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        nativeApplyFilter(bitmap, w, h, -1, -1);
        return bitmap;
    }

}
