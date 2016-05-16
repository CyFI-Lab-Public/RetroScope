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


import android.util.Log;

import com.android.gallery3d.filtershow.controller.Control;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.controller.ParameterInteger;

public class FilterBasicRepresentation extends FilterRepresentation implements ParameterInteger {
    private static final String LOGTAG = "FilterBasicRep";
    private int mMinimum;
    private int mValue;
    private int mMaximum;
    private int mDefaultValue;
    private int mPreviewValue;
    public static final String SERIAL_NAME = "Name";
    public static final String SERIAL_VALUE = "Value";
    private boolean mLogVerbose = Log.isLoggable(LOGTAG, Log.VERBOSE);

    public FilterBasicRepresentation(String name, int minimum, int value, int maximum) {
        super(name);
        mMinimum = minimum;
        mMaximum = maximum;
        setValue(value);
    }

    @Override
    public String toString() {
        return getName() + " : " + mMinimum + " < " + mValue + " < " + mMaximum;
    }

    @Override
    public FilterRepresentation copy() {
        FilterBasicRepresentation representation = new FilterBasicRepresentation(getName(),0,0,0);
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
        if (a instanceof FilterBasicRepresentation) {
            FilterBasicRepresentation representation = (FilterBasicRepresentation) a;
            setMinimum(representation.getMinimum());
            setMaximum(representation.getMaximum());
            setValue(representation.getValue());
            setDefaultValue(representation.getDefaultValue());
            setPreviewValue(representation.getPreviewValue());
        }
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation)) {
            return false;
        }
        if (representation instanceof FilterBasicRepresentation) {
            FilterBasicRepresentation basic = (FilterBasicRepresentation) representation;
            if (basic.mMinimum == mMinimum
                    && basic.mMaximum == mMaximum
                    && basic.mValue == mValue
                    && basic.mDefaultValue == mDefaultValue
                    && basic.mPreviewValue == mPreviewValue) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getMinimum() {
        return mMinimum;
    }

    public void setMinimum(int minimum) {
        mMinimum = minimum;
    }

    @Override
    public int getValue() {
        return mValue;
    }

    @Override
    public void setValue(int value) {
        mValue = value;
        if (mValue < mMinimum) {
            mValue = mMinimum;
        }
        if (mValue > mMaximum) {
            mValue = mMaximum;
        }
    }

    @Override
    public int getMaximum() {
        return mMaximum;
    }

    public void setMaximum(int maximum) {
        mMaximum = maximum;
    }

    public void setDefaultValue(int defaultValue) {
        mDefaultValue = defaultValue;
    }

    @Override
    public int getDefaultValue() {
        return mDefaultValue;
    }

    public int getPreviewValue() {
        return mPreviewValue;
    }

    public void setPreviewValue(int previewValue) {
        mPreviewValue = previewValue;
    }

    @Override
    public String getStateRepresentation() {
        int val = getValue();
        return ((val > 0) ? "+" : "") + val;
    }

    @Override
    public String getParameterType(){
        return sParameterType;
    }

    @Override
    public void setController(Control control) {
    }

    @Override
    public String getValueString() {
        return getStateRepresentation();
    }

    @Override
    public String getParameterName() {
        return getName();
    }

    @Override
    public void setFilterView(FilterView editor) {
    }

    @Override
    public void copyFrom(Parameter src) {
        useParametersFrom((FilterBasicRepresentation) src);
    }

    @Override
    public String[][] serializeRepresentation() {
        String[][] ret = {
                {SERIAL_NAME  , getName() },
                {SERIAL_VALUE , Integer.toString(mValue)}};
        return ret;
    }

    @Override
    public void deSerializeRepresentation(String[][] rep) {
        super.deSerializeRepresentation(rep);
        for (int i = 0; i < rep.length; i++) {
            if (SERIAL_VALUE.equals(rep[i][0])) {
                mValue = Integer.parseInt(rep[i][1]);
                break;
            }
        }
    }
}
