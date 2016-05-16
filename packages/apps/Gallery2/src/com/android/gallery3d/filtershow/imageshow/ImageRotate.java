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

package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.android.gallery3d.filtershow.editors.EditorRotate;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils.GeometryHolder;

public class ImageRotate extends ImageShow {
    private EditorRotate mEditorRotate;
    private static final String TAG = ImageRotate.class.getSimpleName();
    private FilterRotateRepresentation mLocalRep = new FilterRotateRepresentation();
    private GeometryHolder mDrawHolder = new GeometryHolder();

    public ImageRotate(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageRotate(Context context) {
        super(context);
    }

    public void setFilterRotateRepresentation(FilterRotateRepresentation rep) {
        mLocalRep = (rep == null) ? new FilterRotateRepresentation() : rep;
    }

    public void rotate() {
        mLocalRep.rotateCW();
        invalidate();
    }

    public FilterRotateRepresentation getFinalRepresentation() {
        return mLocalRep;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Treat event as handled.
        return true;
    }

    public int getLocalValue() {
        return mLocalRep.getRotation().value();
    }

    @Override
    public void onDraw(Canvas canvas) {
        MasterImage master = MasterImage.getImage();
        Bitmap image = master.getFiltersOnlyImage();
        if (image == null) {
            return;
        }
        GeometryMathUtils.initializeHolder(mDrawHolder, mLocalRep);
        GeometryMathUtils.drawTransformedCropped(mDrawHolder, canvas, image, canvas.getWidth(),
                canvas.getHeight());
    }

    public void setEditor(EditorRotate editorRotate) {
        mEditorRotate = editorRotate;
    }
}
