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

package com.android.camera.data;

import android.view.View;

import com.android.camera.ui.FilmStripView.ImageData;

/**
 * A class implementing {@link LocalData} to represent a camera preview.
 */
public class CameraPreviewData extends SimpleViewData {

    private boolean mPreviewLocked;

    /**
     * Constructor.
     *
     * @param v      The {@link android.view.View} for camera preview.
     * @param width  The width of the camera preview.
     * @param height The height of the camera preview.
     */
    public CameraPreviewData(View v, int width, int height) {
        super(v, width, height, -1, -1);
        mPreviewLocked = true;
    }

    @Override
    public int getViewType() {
        return ImageData.VIEW_TYPE_STICKY;
    }

    @Override
    public int getLocalDataType() {
        return LOCAL_CAMERA_PREVIEW;
    }

    @Override
    public boolean canSwipeInFullScreen() {
        return !mPreviewLocked;
    }

    /**
     * Locks the camera preview. When the camera preview is locked, swipe
     * to film strip is not allowed. One case is when the video recording
     * is in progress.
     *
     * @param lock {@code true} if the preview should be locked. {@code false}
     *             otherwise.
     */
    public void lockPreview(boolean lock) {
        mPreviewLocked = lock;
    }
}
