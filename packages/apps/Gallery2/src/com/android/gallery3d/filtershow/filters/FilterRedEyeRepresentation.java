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

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.EditorRedEye;

import java.util.Vector;

public class FilterRedEyeRepresentation extends FilterPointRepresentation {
    private static final String LOGTAG = "FilterRedEyeRepresentation";

    public FilterRedEyeRepresentation() {
        super("RedEye",R.string.redeye,EditorRedEye.ID);
        setSerializationName("REDEYE");
        setFilterClass(ImageFilterRedEye.class);
        setOverlayId(R.drawable.photoeditor_effect_redeye);
        setOverlayOnly(true);
    }

    @Override
    public FilterRepresentation copy() {
        FilterRedEyeRepresentation representation = new FilterRedEyeRepresentation();
        copyAllParameters(representation);
        return representation;
    }

    @Override
    protected void copyAllParameters(FilterRepresentation representation) {
        super.copyAllParameters(representation);
        representation.useParametersFrom(this);
    }

    public void addRect(RectF rect, RectF bounds) {
        Vector<RedEyeCandidate> intersects = new Vector<RedEyeCandidate>();
        for (int i = 0; i < getCandidates().size(); i++) {
            RedEyeCandidate r = (RedEyeCandidate) getCandidate(i);
            if (r.intersect(rect)) {
                intersects.add(r);
            }
        }
        for (int i = 0; i < intersects.size(); i++) {
            RedEyeCandidate r = intersects.elementAt(i);
            rect.union(r.mRect);
            bounds.union(r.mBounds);
            removeCandidate(r);
        }
        addCandidate(new RedEyeCandidate(rect, bounds));
    }

}
