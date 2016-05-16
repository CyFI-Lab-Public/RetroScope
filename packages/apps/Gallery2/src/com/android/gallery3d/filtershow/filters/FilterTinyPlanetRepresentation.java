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
import com.android.gallery3d.filtershow.editors.EditorTinyPlanet;

public class FilterTinyPlanetRepresentation extends FilterBasicRepresentation {
    private static final String SERIALIZATION_NAME = "TINYPLANET";
    private static final String LOGTAG = "FilterTinyPlanetRepresentation";
    private static final String SERIAL_ANGLE = "Angle";
    private float mAngle = 0;

    public FilterTinyPlanetRepresentation() {
        super("TinyPlanet", 0, 50, 100);
        setSerializationName(SERIALIZATION_NAME);
        setShowParameterValue(true);
        setFilterClass(ImageFilterTinyPlanet.class);
        setFilterType(FilterRepresentation.TYPE_TINYPLANET);
        setTextId(R.string.tinyplanet);
        setEditorId(EditorTinyPlanet.ID);
        setMinimum(1);
        setSupportsPartialRendering(false);
    }

    @Override
    public FilterRepresentation copy() {
        FilterTinyPlanetRepresentation representation = new FilterTinyPlanetRepresentation();
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        FilterTinyPlanetRepresentation representation = (FilterTinyPlanetRepresentation) a;
        super.useParametersFrom(a);
        mAngle = representation.mAngle;
        setZoom(representation.getZoom());
    }

    public void setAngle(float angle) {
        mAngle = angle;
    }

    public float getAngle() {
        return mAngle;
    }

    public int getZoom() {
        return getValue();
    }

    public void setZoom(int zoom) {
        setValue(zoom);
    }

    public boolean isNil() {
        // TinyPlanet always has an effect
        return false;
    }

    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation)) {
            return false;
        }
        if (mAngle == ((FilterTinyPlanetRepresentation) representation).mAngle) {
            return true;
        }
        return false;
    }

    @Override
    public String[][] serializeRepresentation() {
        String[][] ret = {
                {SERIAL_NAME  , getName() },
                {SERIAL_VALUE , Integer.toString(getValue())},
                {SERIAL_ANGLE , Float.toString(mAngle)}};
        return ret;
    }

    @Override
    public void deSerializeRepresentation(String[][] rep) {
        super.deSerializeRepresentation(rep);
        for (int i = 0; i < rep.length; i++) {
            if (SERIAL_VALUE.equals(rep[i][0])) {
                setValue(Integer.parseInt(rep[i][1]));
            } else if (SERIAL_ANGLE.equals(rep[i][0])) {
                setAngle(Float.parseFloat(rep[i][1]));
            }
        }
    }
}
