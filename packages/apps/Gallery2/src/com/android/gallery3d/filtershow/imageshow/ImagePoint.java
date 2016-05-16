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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;

import com.android.gallery3d.filtershow.editors.EditorRedEye;
import com.android.gallery3d.filtershow.filters.FilterPoint;
import com.android.gallery3d.filtershow.filters.FilterRedEyeRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilterRedEye;

public abstract class ImagePoint extends ImageShow {

    private static final String LOGTAG = "ImageRedEyes";
    protected EditorRedEye mEditorRedEye;
    protected FilterRedEyeRepresentation mRedEyeRep;
    protected static float mTouchPadding = 80;

    public static void setTouchPadding(float padding) {
        mTouchPadding = padding;
    }

    public ImagePoint(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImagePoint(Context context) {
        super(context);
    }

    @Override
    public void resetParameter() {
        ImageFilterRedEye filter = (ImageFilterRedEye) getCurrentFilter();
        if (filter != null) {
            filter.clear();
        }
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setStyle(Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(2);

        Matrix originalToScreen = getImageToScreenMatrix(false);
        Matrix originalRotateToScreen = getImageToScreenMatrix(true);

        if (mRedEyeRep != null) {
            for (FilterPoint candidate : mRedEyeRep.getCandidates()) {
                drawPoint(candidate, canvas, originalToScreen, originalRotateToScreen, paint);
            }
        }
    }

    protected abstract void drawPoint(
            FilterPoint candidate, Canvas canvas, Matrix originalToScreen,
            Matrix originalRotateToScreen, Paint paint);

    public void setEditor(EditorRedEye editorRedEye) {
        mEditorRedEye = editorRedEye;
    }

    public void setRepresentation(FilterRedEyeRepresentation redEyeRep) {
        mRedEyeRep = redEyeRep;
    }
}
