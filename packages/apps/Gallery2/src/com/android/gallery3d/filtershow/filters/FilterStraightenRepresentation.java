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

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.EditorStraighten;

import java.io.IOException;

public class FilterStraightenRepresentation extends FilterRepresentation {
    public static final String SERIALIZATION_NAME = "STRAIGHTEN";
    public static final String SERIALIZATION_STRAIGHTEN_VALUE = "value";
    private static final String TAG = FilterStraightenRepresentation.class.getSimpleName();
    public static final int MAX_STRAIGHTEN_ANGLE = 45;
    public static final int MIN_STRAIGHTEN_ANGLE = -45;

    float mStraighten;

    public FilterStraightenRepresentation(float straighten) {
        super(SERIALIZATION_NAME);
        setSerializationName(SERIALIZATION_NAME);
        setShowParameterValue(true);
        setFilterClass(FilterStraightenRepresentation.class);
        setFilterType(FilterRepresentation.TYPE_GEOMETRY);
        setSupportsPartialRendering(true);
        setTextId(R.string.straighten);
        setEditorId(EditorStraighten.ID);
        setStraighten(straighten);
    }

    public FilterStraightenRepresentation(FilterStraightenRepresentation s) {
        this(s.getStraighten());
        setName(s.getName());
    }

    public FilterStraightenRepresentation() {
        this(getNil());
    }

    public void set(FilterStraightenRepresentation r) {
        mStraighten = r.mStraighten;
    }

    @Override
    public boolean equals(FilterRepresentation rep) {
        if (!(rep instanceof FilterStraightenRepresentation)) {
            return false;
        }
        FilterStraightenRepresentation straighten = (FilterStraightenRepresentation) rep;
        if (straighten.mStraighten != mStraighten) {
            return false;
        }
        return true;
    }

    public float getStraighten() {
        return mStraighten;
    }

    public void setStraighten(float straighten) {
        if (!rangeCheck(straighten)) {
            straighten = Math.min(Math.max(straighten, MIN_STRAIGHTEN_ANGLE), MAX_STRAIGHTEN_ANGLE);
        }
        mStraighten = straighten;
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    @Override
    public FilterRepresentation copy() {
        return new FilterStraightenRepresentation(this);
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        if (!(representation instanceof FilterStraightenRepresentation)) {
            throw new IllegalArgumentException("calling copyAllParameters with incompatible types!");
        }
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (!(a instanceof FilterStraightenRepresentation)) {
            throw new IllegalArgumentException("calling useParametersFrom with incompatible types!");
        }
        setStraighten(((FilterStraightenRepresentation) a).getStraighten());
    }

    @Override
    public boolean isNil() {
        return mStraighten == getNil();
    }

    public static float getNil() {
        return 0;
    }

    @Override
    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name(SERIALIZATION_STRAIGHTEN_VALUE).value(mStraighten);
        writer.endObject();
    }

    @Override
    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        boolean unset = true;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (SERIALIZATION_STRAIGHTEN_VALUE.equals(name)) {
                float s = (float) reader.nextDouble();
                if (rangeCheck(s)) {
                    setStraighten(s);
                    unset = false;
                }
            } else {
                reader.skipValue();
            }
        }
        if (unset) {
            Log.w(TAG, "WARNING: bad value when deserializing " + SERIALIZATION_NAME);
        }
        reader.endObject();
    }

    private boolean rangeCheck(double s) {
        if (s < -45 || s > 45) {
            return false;
        }
        return true;
    }
}
