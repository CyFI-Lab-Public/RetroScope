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
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterTinyPlanetRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageTinyPlanet;

public class EditorTinyPlanet extends BasicEditor {
    public static final int ID = R.id.tinyPlanetEditor;
    private static final String LOGTAG = "EditorTinyPlanet";
    ImageTinyPlanet mImageTinyPlanet;

    public EditorTinyPlanet() {
        super(ID, R.layout.filtershow_tiny_planet_editor, R.id.imageTinyPlanet);
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        mImageTinyPlanet = (ImageTinyPlanet) mImageShow;
        mImageTinyPlanet.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep != null && rep instanceof FilterTinyPlanetRepresentation) {
            FilterTinyPlanetRepresentation drawRep = (FilterTinyPlanetRepresentation) rep;
            mImageTinyPlanet.setRepresentation(drawRep);
        }
    }

    public void updateUI() {
        if (mControl != null) {
            mControl.updateUI();
        }
    }
}
