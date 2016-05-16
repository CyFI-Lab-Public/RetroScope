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

import com.android.gallery3d.filtershow.editors.ImageOnlyEditor;

public class FilterFxRepresentation extends FilterRepresentation {
   private static final String LOGTAG = "FilterFxRepresentation";
    // TODO: When implementing serialization, we should find a unique way of
    // specifying bitmaps / names (the resource IDs being random)
    private int mBitmapResource = 0;
    private int mNameResource = 0;

    public FilterFxRepresentation(String name, int bitmapResource, int nameResource) {
        super(name);
        setFilterClass(ImageFilterFx.class);
        mBitmapResource = bitmapResource;
        mNameResource = nameResource;
        setFilterType(FilterRepresentation.TYPE_FX);
        setTextId(nameResource);
        setEditorId(ImageOnlyEditor.ID);
        setShowParameterValue(false);
        setSupportsPartialRendering(true);
    }

    @Override
    public String toString() {
        return "FilterFx: " + hashCode() + " : " + getName() + " bitmap rsc: " + mBitmapResource;
    }

    @Override
    public FilterRepresentation copy() {
        FilterFxRepresentation representation = new FilterFxRepresentation(getName(),0,0);
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    @Override
    public synchronized void useParametersFrom(FilterRepresentation a) {
        if (a instanceof FilterFxRepresentation) {
            FilterFxRepresentation representation = (FilterFxRepresentation) a;
            setName(representation.getName());
            setSerializationName(representation.getSerializationName());
            setBitmapResource(representation.getBitmapResource());
            setNameResource(representation.getNameResource());
        }
    }

    @Override
    public boolean equals(FilterRepresentation representation) {
        if (!super.equals(representation)) {
            return false;
        }
        if (representation instanceof FilterFxRepresentation) {
            FilterFxRepresentation fx = (FilterFxRepresentation) representation;
            if (fx.mNameResource == mNameResource
                    && fx.mBitmapResource == mBitmapResource) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean same(FilterRepresentation representation) {
        if (!super.same(representation)) {
            return false;
        }
        return equals(representation);
    }

    @Override
    public boolean allowsSingleInstanceOnly() {
        return true;
    }

    public int getNameResource() {
        return mNameResource;
    }

    public void setNameResource(int nameResource) {
        mNameResource = nameResource;
    }

    public int getBitmapResource() {
        return mBitmapResource;
    }

    public void setBitmapResource(int bitmapResource) {
        mBitmapResource = bitmapResource;
    }
}
