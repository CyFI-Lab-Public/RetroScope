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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageMirror;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class EditorMirror extends Editor implements EditorInfo {
    public static final String TAG = EditorMirror.class.getSimpleName();
    public static final int ID = R.id.editorFlip;
    ImageMirror mImageMirror;

    public EditorMirror() {
        super(ID);
        mChangesGeometry = true;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        if (mImageMirror == null) {
            mImageMirror = new ImageMirror(context);
        }
        mView = mImageShow = mImageMirror;
        mImageMirror.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        MasterImage master = MasterImage.getImage();
        master.setCurrentFilterRepresentation(master.getPreset()
                .getFilterWithSerializationName(FilterMirrorRepresentation.SERIALIZATION_NAME));
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || rep instanceof FilterMirrorRepresentation) {
            mImageMirror.setFilterMirrorRepresentation((FilterMirrorRepresentation) rep);
        } else {
            Log.w(TAG, "Could not reflect current filter, not of type: "
                    + FilterMirrorRepresentation.class.getSimpleName());
        }
        mImageMirror.invalidate();
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        final Button button = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mImageMirror.flip();
            }
        });
    }

    @Override
    public void finalApplyCalled() {
        commitLocalRepresentation(mImageMirror.getFinalRepresentation());
    }

    @Override
    public int getTextId() {
        return R.string.mirror;
    }

    @Override
    public int getOverlayId() {
        return R.drawable.filtershow_button_geometry_flip;
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
