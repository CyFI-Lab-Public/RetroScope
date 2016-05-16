/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.view.View.MeasureSpec;

class MeasureHelper {

    private static MeasureHelper sInstance = new MeasureHelper(null);

    private GLView mComponent;
    private int mPreferredWidth;
    private int mPreferredHeight;

    private MeasureHelper(GLView component) {
        mComponent = component;
    }

    public static MeasureHelper getInstance(GLView component) {
        sInstance.mComponent = component;
        return sInstance;
    }

    public MeasureHelper setPreferredContentSize(int width, int height) {
        mPreferredWidth = width;
        mPreferredHeight = height;
        return this;
    }

    public void measure(int widthSpec, int heightSpec) {
        Rect p = mComponent.getPaddings();
        setMeasuredSize(
                getLength(widthSpec, mPreferredWidth + p.left + p.right),
                getLength(heightSpec, mPreferredHeight + p.top + p.bottom));
    }

    private static int getLength(int measureSpec, int prefered) {
        int specLength = MeasureSpec.getSize(measureSpec);
        switch(MeasureSpec.getMode(measureSpec)) {
            case MeasureSpec.EXACTLY: return specLength;
            case MeasureSpec.AT_MOST: return Math.min(prefered, specLength);
            default: return prefered;
        }
    }

    protected void setMeasuredSize(int width, int height) {
        mComponent.setMeasuredSize(width, height);
    }

}
