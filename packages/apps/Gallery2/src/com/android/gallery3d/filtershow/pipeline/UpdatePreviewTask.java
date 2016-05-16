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

package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;

import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class UpdatePreviewTask extends ProcessingTask {
    private static final String LOGTAG = "UpdatePreviewTask";
    private CachingPipeline mPreviewPipeline = null;
    private boolean mHasUnhandledPreviewRequest = false;
    private boolean mPipelineIsOn = false;

    public UpdatePreviewTask() {
        mPreviewPipeline = new CachingPipeline(
                FiltersManager.getPreviewManager(), "Preview");
    }

    public void setOriginal(Bitmap bitmap) {
        mPreviewPipeline.setOriginal(bitmap);
        mPipelineIsOn = true;
    }

    public void updatePreview() {
        if (!mPipelineIsOn) {
            return;
        }
        mHasUnhandledPreviewRequest = true;
        if (postRequest(null)) {
            mHasUnhandledPreviewRequest = false;
        }
    }

    @Override
    public boolean isPriorityTask() {
        return true;
    }

    @Override
    public Result doInBackground(Request message) {
        SharedBuffer buffer = MasterImage.getImage().getPreviewBuffer();
        SharedPreset preset = MasterImage.getImage().getPreviewPreset();
        ImagePreset renderingPreset = preset.dequeuePreset();
        if (renderingPreset != null) {
            mPreviewPipeline.compute(buffer, renderingPreset, 0);
            // set the preset we used in the buffer for later inspection UI-side
            buffer.getProducer().setPreset(renderingPreset);
            buffer.getProducer().sync();
            buffer.swapProducer(); // push back the result
        }
        return null;
    }

    @Override
    public void onResult(Result message) {
        MasterImage.getImage().notifyObservers();
        if (mHasUnhandledPreviewRequest) {
            updatePreview();
        }
    }

    public void setPipelineIsOn(boolean pipelineIsOn) {
        mPipelineIsOn = pipelineIsOn;
    }
}
