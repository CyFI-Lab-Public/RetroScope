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

import android.graphics.Color;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.BasicParameterInt;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.controller.ParameterColor;
import com.android.gallery3d.filtershow.editors.EditorColorBorder;

import java.io.IOException;

public class FilterColorBorderRepresentation extends FilterRepresentation {
    private static final String LOGTAG = "FilterColorBorderRepresentation";
    private static final String SERIALIZATION_NAME = "COLORBORDER";

    public static final int PARAM_SIZE = 0;
    public static final int PARAM_RADIUS = 1;
    public static final int PARAM_COLOR = 2;
    public static int DEFAULT_MENU_COLOR1 = Color.WHITE;
    public static int DEFAULT_MENU_COLOR2 = Color.BLACK;
    public static int DEFAULT_MENU_COLOR3 = Color.GRAY;
    public static int DEFAULT_MENU_COLOR4 = 0xFFFFCCAA;
    public static int DEFAULT_MENU_COLOR5 = 0xFFAAAAAA;
    private BasicParameterInt mParamSize = new BasicParameterInt(PARAM_SIZE, 3, 2, 30);
    private BasicParameterInt mParamRadius = new BasicParameterInt(PARAM_RADIUS, 2, 0, 100);
    private ParameterColor mParamColor = new ParameterColor(PARAM_COLOR, DEFAULT_MENU_COLOR1);

    private Parameter[] mAllParam = {
            mParamSize,
            mParamRadius,
            mParamColor
    };
    private int mPramMode;

    public FilterColorBorderRepresentation(int color, int size, int radius) {
        super("ColorBorder");
        setSerializationName(SERIALIZATION_NAME);
        setFilterType(FilterRepresentation.TYPE_BORDER);
        setTextId(R.string.borders);
        setEditorId(EditorColorBorder.ID);
        setShowParameterValue(false);
        setFilterClass(ImageFilterColorBorder.class);
        mParamColor.setValue(color);
        mParamSize.setValue(size);
        mParamRadius.setValue(radius);
        mParamColor.setColorpalette(new int[]{
                DEFAULT_MENU_COLOR1,
                DEFAULT_MENU_COLOR2,
                DEFAULT_MENU_COLOR3,
                DEFAULT_MENU_COLOR4,
                DEFAULT_MENU_COLOR5});
    }

    public String toString() {
        return "FilterBorder: " + getName();
    }

    @Override
    public FilterRepresentation copy() {
        FilterColorBorderRepresentation representation =
                new FilterColorBorderRepresentation(0, 0, 0);
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    public void useParametersFrom(FilterRepresentation a) {
        if (a instanceof FilterColorBorderRepresentation) {
            FilterColorBorderRepresentation representation = (FilterColorBorderRepresentation) a;
            setName(representation.getName());
            setColor(representation.getColor());
            mParamColor.copyPalletFrom(representation.mParamColor);
            setBorderSize(representation.getBorderSize());
            setBorderRadius(representation.getBorderRadius());
        }
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation)) {
            return false;
        }
        if (representation instanceof FilterColorBorderRepresentation) {
            FilterColorBorderRepresentation border = (FilterColorBorderRepresentation) representation;
            if (border.mParamColor.getValue() == mParamColor.getValue()
                    && border.mParamRadius.getValue() == mParamRadius.getValue()
                    && border.mParamSize.getValue() == mParamSize.getValue()) {

                return true;
            }
        }
        return false;
    }

    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    public Parameter getParam(int mode) {
        return mAllParam[mode];
    }

    @Override
    public int getTextId() {
        if (super.getTextId() == 0) {
            return R.string.borders;
        }
        return super.getTextId();
    }

    public int getColor() {
        return mParamColor.getValue();
    }

    public void setColor(int color) {
        mParamColor.setValue(color);
    }

    public int getBorderSize() {
        return mParamSize.getValue();
    }

    public void setBorderSize(int borderSize) {
        mParamSize.setValue(borderSize);
    }

    public int getBorderRadius() {
        return mParamRadius.getValue();
    }

    public void setBorderRadius(int borderRadius) {
        mParamRadius.setValue(borderRadius);
    }

    public void setPramMode(int pramMode) {
        this.mPramMode = pramMode;
    }

    public Parameter getCurrentParam() {
        return mAllParam[mPramMode];
    }

    public String getValueString() {
        return "";
    }

    // Serialization...

    public void serializeRepresentation(JsonWriter writer) throws IOException {
        writer.beginObject();
        {
            writer.name("size");
            writer.value(mParamSize.getValue());
            writer.name("radius");
            writer.value(mParamRadius.getValue());
            writer.name("color");
            writer.value(mParamColor.getValue());
        }
        writer.endObject();
    }

    public void deSerializeRepresentation(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equalsIgnoreCase("size")) {
                mParamSize.setValue(reader.nextInt());
            } else if (name.equalsIgnoreCase("radius")) {
                mParamRadius.setValue(reader.nextInt());
            } else if (name.equalsIgnoreCase("color")) {
                mParamColor.setValue(reader.nextInt());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }
}
