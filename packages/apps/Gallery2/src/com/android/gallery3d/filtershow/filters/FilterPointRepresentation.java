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

import java.util.Vector;

public abstract class FilterPointRepresentation extends FilterRepresentation {
    private static final String LOGTAG = "FilterPointRepresentation";
    private Vector<FilterPoint> mCandidates = new Vector<FilterPoint>();

    public FilterPointRepresentation(String type, int textid, int editorID) {
        super(type);
        setFilterClass(ImageFilterRedEye.class);
        setFilterType(FilterRepresentation.TYPE_NORMAL);
        setTextId(textid);
        setEditorId(editorID);
    }

    @Override
    public abstract FilterRepresentation copy();

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    public boolean hasCandidates() {
        return mCandidates != null;
    }

    public Vector<FilterPoint> getCandidates() {
        return mCandidates;
    }

    @Override
    public boolean isNil() {
        if (getCandidates() != null && getCandidates().size() > 0) {
            return false;
        }
        return true;
    }

    public Object getCandidate(int index) {
        return this.mCandidates.get(index);
    }

    public void addCandidate(FilterPoint c) {
        this.mCandidates.add(c);
    }

    @Override
    public void useParametersFrom(FilterRepresentation a) {
        if (a instanceof FilterPointRepresentation) {
            FilterPointRepresentation representation = (FilterPointRepresentation) a;
            mCandidates.clear();
            for (FilterPoint redEyeCandidate : representation.mCandidates) {
                mCandidates.add(redEyeCandidate);
            }
        }
    }

    public void removeCandidate(RedEyeCandidate c) {
        this.mCandidates.remove(c);
    }

    public void clearCandidates() {
        this.mCandidates.clear();
    }

    public int getNumberOfCandidates() {
        return mCandidates.size();
    }
}
