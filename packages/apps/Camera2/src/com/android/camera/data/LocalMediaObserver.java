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

import android.database.ContentObserver;

/**
 * Listening to the changes to the local image and video data. onChange will
 * happen on the main thread.
 */
public class LocalMediaObserver extends ContentObserver {

    private boolean mActivityPaused = false;
    private boolean mMediaDataChangedDuringPause = false;

    public LocalMediaObserver() {
        super(null);
    }

    /**
     * When the activity is paused and MediaObserver get onChange() call, then
     * we would like to set a dirty bit to reload the data at onResume().
     */
    @Override
    public void onChange(boolean selfChange) {
        if (mActivityPaused) {
            mMediaDataChangedDuringPause = true;
        }
    }

    public void setActivityPaused(boolean paused) {
        mActivityPaused = paused;
        if (!paused) {
            mMediaDataChangedDuringPause = false;
        }
    }

    public boolean isMediaDataChangedDuringPause() {
        return mMediaDataChangedDuringPause;
    }
}
