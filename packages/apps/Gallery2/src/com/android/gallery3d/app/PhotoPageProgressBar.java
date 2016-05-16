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
package com.android.gallery3d.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.android.gallery3d.R;

public class PhotoPageProgressBar {
    private ViewGroup mContainer;
    private View mProgress;

    public PhotoPageProgressBar(Context context, RelativeLayout parentLayout) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater.inflate(R.layout.photopage_progress_bar, parentLayout,
                false);
        parentLayout.addView(mContainer);
        mProgress = mContainer.findViewById(R.id.photopage_progress_foreground);
    }

    public void setProgress(int progressPercent) {
        mContainer.setVisibility(View.VISIBLE);
        LayoutParams layoutParams = mProgress.getLayoutParams();
        layoutParams.width = mContainer.getWidth() * progressPercent / 100;
        mProgress.setLayoutParams(layoutParams);
    }

    public void hideProgress() {
        mContainer.setVisibility(View.INVISIBLE);
    }
}
