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

package com.android.gallery3d.filtershow.editors;

import android.content.Context;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.Control;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.controller.ParameterInteger;
import com.android.gallery3d.filtershow.filters.FilterBasicRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;


/**
 * The basic editor that all the one parameter filters
 */
public class BasicEditor extends ParametricEditor implements ParameterInteger {
    public static int ID = R.id.basicEditor;
    private final String LOGTAG = "BasicEditor";

    public BasicEditor() {
        super(ID, R.layout.filtershow_default_editor, R.id.basicEditor);
    }

    protected BasicEditor(int id) {
        super(id, R.layout.filtershow_default_editor, R.id.basicEditor);
    }

    protected BasicEditor(int id, int layoutID, int viewID) {
        super(id, layoutID, viewID);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        if (getLocalRepresentation() != null && getLocalRepresentation() instanceof FilterBasicRepresentation) {
            FilterBasicRepresentation interval = (FilterBasicRepresentation) getLocalRepresentation();
            updateText();
        }
    }

    private FilterBasicRepresentation getBasicRepresentation() {
        FilterRepresentation tmpRep = getLocalRepresentation();
        if (tmpRep != null && tmpRep instanceof FilterBasicRepresentation) {
            return (FilterBasicRepresentation) tmpRep;

        }
        return null;
    }

    @Override
    public int getMaximum() {
        FilterBasicRepresentation rep = getBasicRepresentation();
        if (rep == null) {
            return 0;
        }
        return rep.getMaximum();
    }

    @Override
    public int getMinimum() {
        FilterBasicRepresentation rep = getBasicRepresentation();
        if (rep == null) {
            return 0;
        }
        return rep.getMinimum();
    }

    @Override
    public int getDefaultValue() {
        return 0;
    }

    @Override
    public int getValue() {
        FilterBasicRepresentation rep = getBasicRepresentation();
        if (rep == null) {
            return 0;
        }
        return rep.getValue();
    }

    @Override
    public String getValueString() {
        return null;
    }

    @Override
    public void setValue(int value) {
        FilterBasicRepresentation rep = getBasicRepresentation();
        if (rep == null) {
            return;
        }
        rep.setValue(value);
        commitLocalRepresentation();
    }

    @Override
    public String getParameterName() {
        FilterBasicRepresentation rep = getBasicRepresentation();
        return mContext.getString(rep.getTextId());
    }

    @Override
    public String getParameterType() {
        return sParameterType;
    }

    @Override
    public void setController(Control c) {
    }

    @Override
    public void setFilterView(FilterView editor) {

    }

    @Override
    public void copyFrom(Parameter src) {

    }
}
