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

package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.android.gallery3d.filtershow.editors.EditorMirror;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils.GeometryHolder;

public class ImageMirror extends ImageShow {
    private static final String TAG = ImageMirror.class.getSimpleName();
    private EditorMirror mEditorMirror;
    private FilterMirrorRepresentation mLocalRep = new FilterMirrorRepresentation();
    private GeometryHolder mDrawHolder = new GeometryHolder();

    public ImageMirror(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageMirror(Context context) {
        super(context);
    }

    public void setFilterMirrorRepresentation(FilterMirrorRepresentation rep) {
        mLocalRep = (rep == null) ? new FilterMirrorRepresentation() : rep;
    }

    public void flip() {
        mLocalRep.cycle();
        invalidate();
    }

    public FilterMirrorRepresentation getFinalRepresentation() {
        return mLocalRep;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Treat event as handled.
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        MasterImage master = MasterImage.getImage();
        Bitmap image = master.getFiltersOnlyImage();
        if (image == null) {
            return;
        }
        GeometryMathUtils.initializeHolder(mDrawHolder, mLocalRep);
        GeometryMathUtils.drawTransformedCropped(mDrawHolder, canvas, image, getWidth(),
                getHeight());
    }

    public void setEditor(EditorMirror editorFlip) {
        mEditorMirror = editorFlip;
    }

}
