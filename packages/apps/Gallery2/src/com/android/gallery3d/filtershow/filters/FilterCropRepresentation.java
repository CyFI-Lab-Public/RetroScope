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

import android.graphics.RectF;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.EditorCrop;

import java.io.IOException;

public class FilterCropRepresentation extends FilterRepresentation {
    public static final String SERIALIZATION_NAME = "CROP";
    public static final String[] BOUNDS = {
            "C0", "C1", "C2", "C3"
    };
    private static final String TAG = FilterCropRepresentation.class.getSimpleName();

    RectF mCrop = getNil();

    public FilterCropRepresentation(RectF crop) {
        super(SERIALIZATION_NAME);
        setSerializationName(SERIALIZATION_NAME);
        setShowParameterValue(true);
        setFilterClass(FilterCropRepresentation.class);
        setFilterType(FilterRepresentation.TYPE_GEOMETRY);
        setSupportsPartialRendering(true);
        setTextId(R.string.crop);
        setEditorId(EditorCrop.ID);
        setCrop(crop);
    }

    public FilterCropRepresentation(FilterCropRepresentation m) {
        this(m.mCrop);
        setName(m.getName());
    }

    public FilterCropRepresentation() {
        this(sNilRect);
    }

    public void set(FilterCropRepresentation r) {
        mCrop.set(r.mCrop);
    }

    @Override
    public boolean equals(FilterRepresentation rep) {
        if (!(rep instanceof FilterCropRepresentation)) {
            return false;
        }
        FilterCropRepresentation crop = (FilterCropRepresentation) rep;
        if (mCrop.bottom != crop.mCrop.bottom
            || mCrop.left != crop.mCrop.left
            || mCrop.right != crop.mCrop.right
            || mCrop.top != crop.mCrop.top) {
            return false;
        }
        return true;
    }

    public RectF getCrop() {
        return new RectF(mCrop);
    }

    public void getCrop(RectF r) {
        r.set(mCrop);
    }

    public void setCrop(RectF crop) {
        if (crop == null) {
            throw new IllegalArgumentException("Argument to setCrop is null");
        }
        mCrop.set(crop);
    }

    /**
     * Takes a crop rect contained by [0, 0, 1, 1] and scales it by the height
     * and width of the image rect.
     */
    public static void findScaledCrop(RectF crop, int bitmapWidth, int bitmapHeight) {
        crop.left *= bitmapWidth;
        crop.top *= bitmapHeight;
        crop.right *= bitmapWidth;
        crop.bottom *= bitmapHeight;
    }

    /**
     * Takes crop rect and normalizes it by scaling down by the height and width
     * of the image rect.
     */
    public static void findNormalizedCrop(RectF crop, int bitmapWidth, int bitmapHeight) {
        crop.left /= bitmapWidth;
        crop.top /= bitmapHeight;
        crop.right /= bitmapWidth;
        crop.bottom /= bitmapHeight;
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    @Override
    public FilterRepresentation copy() {
        return new FilterCropRepresentation(this);
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        if (!(representation instanceof FilterCropRepresentation)) {
            throw new IllegalArgumentException("calling copyAllParameters with incompatible types!");
        }
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (!(a instanceof FilterCropRepresentation)) {
            throw new IllegalArgumentException("calling useParametersFrom with incompatible types!");
        }
        setCrop(((FilterCropRepresentation) a).mCrop);
    }

    private static final RectF sNilRect = new RectF(0, 0, 1, 1);

    @Override
    public boolean isNil() {
        return mCrop.equals(sNilRect);
    }

    public static RectF getNil() {
        return new RectF(sNilRect);
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name(BOUNDS[0]).value(mCrop.left);
        writer.name(BOUNDS[1]).value(mCrop.top);
        writer.name(BOUNDS[2]).value(mCrop.right);
        writer.name(BOUNDS[3]).value(mCrop.bottom);
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (BOUNDS[0].equals(name)) {
                mCrop.left = (float) reader.nextDouble();
            } else if (BOUNDS[1].equals(name)) {
                mCrop.top = (float) reader.nextDouble();
            } else if (BOUNDS[2].equals(name)) {
                mCrop.right = (float) reader.nextDouble();
            } else if (BOUNDS[3].equals(name)) {
                mCrop.bottom = (float) reader.nextDouble();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
}
