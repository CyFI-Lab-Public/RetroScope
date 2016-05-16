/*******************************************************************************
 *      Copyright (C) 2013 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

/**
 * temporary annonated FrameLayout to help find cases of b/6946182
 */
public class FolderListLayout extends FrameLayout {

    public FolderListLayout(Context c) {
        this(c, null);
    }

    public FolderListLayout(Context c, AttributeSet attrs) {
        super(c, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LogUtils.d(Utils.VIEW_DEBUGGING_TAG, "FolderListLayout(%s).onMeasure() called", this);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        LogUtils.d(Utils.VIEW_DEBUGGING_TAG, "FolderListLayout(%s).onLayout() called", this);
        super.onLayout(changed, left, top, right, bottom);
    }

}
