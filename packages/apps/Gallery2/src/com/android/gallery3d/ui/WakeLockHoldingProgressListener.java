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

package com.android.gallery3d.ui;

import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;

import com.android.gallery3d.app.AbstractGalleryActivity;

public class WakeLockHoldingProgressListener implements MenuExecutor.ProgressListener {
    static private final String DEFAULT_WAKE_LOCK_LABEL = "Gallery Progress Listener";
    private AbstractGalleryActivity mActivity;
    private PowerManager.WakeLock mWakeLock;

    public WakeLockHoldingProgressListener(AbstractGalleryActivity galleryActivity) {
        this(galleryActivity, DEFAULT_WAKE_LOCK_LABEL);
    }

    public WakeLockHoldingProgressListener(AbstractGalleryActivity galleryActivity, String label) {
        mActivity = galleryActivity;
        PowerManager pm =
                (PowerManager) ((Activity) mActivity).getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, label);
    }

    @Override
    public void onProgressComplete(int result) {
        mWakeLock.release();
    }

    @Override
    public void onProgressStart() {
        mWakeLock.acquire();
    }

    protected AbstractGalleryActivity getActivity() {
        return mActivity;
    }

    @Override
    public void onProgressUpdate(int index) {
    }

    @Override
    public void onConfirmDialogDismissed(boolean confirmed) {
    }

    @Override
    public void onConfirmDialogShown() {
    }
}
