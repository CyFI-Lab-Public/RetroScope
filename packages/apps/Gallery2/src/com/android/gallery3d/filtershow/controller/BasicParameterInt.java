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

package com.android.gallery3d.filtershow.controller;

import android.util.Log;

public class BasicParameterInt implements ParameterInteger {
    protected String mParameterName;
    protected Control mControl;
    protected int mMaximum = 100;
    protected int mMinimum = 0;
    protected int mDefaultValue;
    protected int mValue;
    public final int ID;
    protected FilterView mEditor;
    private final String LOGTAG = "BasicParameterInt";

    @Override
    public void copyFrom(Parameter src) {
        if (!(src instanceof BasicParameterInt)) {
            throw new IllegalArgumentException(src.getClass().getName());
        }
        BasicParameterInt p = (BasicParameterInt) src;
        mMaximum = p.mMaximum;
        mMinimum = p.mMinimum;
        mDefaultValue = p.mDefaultValue;
        mValue = p.mValue;
    }

    public BasicParameterInt(int id, int value) {
        ID = id;
        mValue = value;
    }

    public BasicParameterInt(int id, int value, int min, int max) {
        ID = id;
        mValue = value;
        mMinimum = min;
        mMaximum = max;
    }

    @Override
    public String getParameterName() {
        return mParameterName;
    }

    @Override
    public String getParameterType() {
        return sParameterType;
    }

    @Override
    public String getValueString() {
        return mParameterName + mValue;
    }

    @Override
    public void setController(Control control) {
        mControl = control;
    }

    @Override
    public int getMaximum() {
        return mMaximum;
    }

    @Override
    public int getMinimum() {
        return mMinimum;
    }

    @Override
    public int getDefaultValue() {
        return mDefaultValue;
    }

    @Override
    public int getValue() {
        return mValue;
    }

    @Override
    public void setValue(int value) {
        mValue = value;
        if (mEditor != null) {
            mEditor.commitLocalRepresentation();
        }
    }

    @Override
    public String toString() {
        return getValueString();
    }

    @Override
    public void setFilterView(FilterView editor) {
        mEditor = editor;
    }
}
