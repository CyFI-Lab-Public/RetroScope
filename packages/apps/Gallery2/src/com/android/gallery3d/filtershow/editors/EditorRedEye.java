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

package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.widget.FrameLayout;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.FilterRedEyeRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageRedEye;

/**
 * The editor with no slider for filters without UI
 */
public class EditorRedEye extends Editor {
    public static int ID = R.id.editorRedEye;
    private final String LOGTAG = "EditorRedEye";
    ImageRedEye mImageRedEyes;

    public EditorRedEye() {
        super(ID);
    }

    protected EditorRedEye(int id) {
        super(id);
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        mView = mImageShow = mImageRedEyes=  new ImageRedEye(context);
        mImageRedEyes.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep != null && getLocalRepresentation() instanceof FilterRedEyeRepresentation) {
            FilterRedEyeRepresentation redEyeRep = (FilterRedEyeRepresentation) rep;

            mImageRedEyes.setRepresentation(redEyeRep);
        }
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }
}
