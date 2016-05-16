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
import android.util.Log;
import android.widget.FrameLayout;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterStraightenRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageStraighten;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class EditorStraighten extends Editor implements EditorInfo {
    public static final String TAG = EditorStraighten.class.getSimpleName();
    public static final int ID = R.id.editorStraighten;
    ImageStraighten mImageStraighten;

    public EditorStraighten() {
        super(ID);
        mShowParameter = SHOW_VALUE_INT;
        mChangesGeometry = true;
    }

    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        String apply = context.getString(R.string.apply_effect);
        apply += " " + effectName;
        return apply.toUpperCase();
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        if (mImageStraighten == null) {
            mImageStraighten = new ImageStraighten(context);
        }
        mView = mImageShow = mImageStraighten;
        mImageStraighten.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        MasterImage master = MasterImage.getImage();
        master.setCurrentFilterRepresentation(master.getPreset().getFilterWithSerializationName(
                FilterStraightenRepresentation.SERIALIZATION_NAME));
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || rep instanceof FilterStraightenRepresentation) {
            mImageStraighten
                    .setFilterStraightenRepresentation((FilterStraightenRepresentation) rep);
        } else {
            Log.w(TAG, "Could not reflect current filter, not of type: "
                    + FilterStraightenRepresentation.class.getSimpleName());
        }
        mImageStraighten.invalidate();
    }

    @Override
    public void finalApplyCalled() {
        commitLocalRepresentation(mImageStraighten.getFinalRepresentation());
    }

    @Override
    public int getTextId() {
        return R.string.straighten;
    }

    @Override
    public int getOverlayId() {
        return R.drawable.filtershow_button_geometry_straighten;
    }

    @Override
    public boolean getOverlayOnly() {
        return true;
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }

    @Override
    public boolean showsPopupIndicator() {
        return false;
    }
}
