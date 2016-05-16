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

package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;

import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;

// SnailItem is a MediaItem which can provide a ScreenNail. This is
// used so we can show an foreign component (like an
// android.view.View) instead of a Bitmap.
public class SnailItem extends MediaItem {
    @SuppressWarnings("unused")
    private static final String TAG = "SnailItem";
    private ScreenNail mScreenNail;

    public SnailItem(Path path) {
        super(path, nextVersionNumber());
    }

    @Override
    public Job<Bitmap> requestImage(int type) {
        // nothing to return
        return new Job<Bitmap>() {
            @Override
            public Bitmap run(JobContext jc) {
                return null;
            }
        };
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage() {
        // nothing to return
        return new Job<BitmapRegionDecoder>() {
            @Override
            public BitmapRegionDecoder run(JobContext jc) {
                return null;
            }
        };
    }

    // We do not provide requestImage or requestLargeImage, instead we
    // provide a ScreenNail.
    @Override
    public ScreenNail getScreenNail() {
        return mScreenNail;
    }

    @Override
    public String getMimeType() {
        return "";
    }

    // Returns width and height of the media item.
    // Returns 0, 0 if the information is not available.
    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    //////////////////////////////////////////////////////////////////////////
    //  Extra methods for SnailItem
    //////////////////////////////////////////////////////////////////////////

    public void setScreenNail(ScreenNail screenNail) {
        mScreenNail = screenNail;
    }

    public void updateVersion() {
        mDataVersion = nextVersionNumber();
    }
}
