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
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;

public class ActionImage extends MediaItem {
    @SuppressWarnings("unused")
    private static final String TAG = "ActionImage";
    private GalleryApp mApplication;
    private int mResourceId;

    public ActionImage(Path path, GalleryApp application, int resourceId) {
        super(path, nextVersionNumber());
        mApplication = Utils.checkNotNull(application);
        mResourceId = resourceId;
    }

    @Override
    public Job<Bitmap> requestImage(int type) {
        return new BitmapJob(type);
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage() {
        return null;
    }

    private class BitmapJob implements Job<Bitmap> {
        private int mType;

        protected BitmapJob(int type) {
            mType = type;
        }

        @Override
        public Bitmap run(JobContext jc) {
            int targetSize = MediaItem.getTargetSize(mType);
            Bitmap bitmap = BitmapFactory.decodeResource(mApplication.getResources(),
                    mResourceId);

            if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
                bitmap = BitmapUtils.resizeAndCropCenter(bitmap, targetSize, true);
            } else {
                bitmap = BitmapUtils.resizeDownBySideLength(bitmap, targetSize, true);
            }
            return bitmap;
        }
    }

    @Override
    public int getSupportedOperations() {
        return SUPPORT_ACTION;
    }

    @Override
    public int getMediaType() {
        return MEDIA_TYPE_UNKNOWN;
    }

    @Override
    public Uri getContentUri() {
        return null;
    }

    @Override
    public String getMimeType() {
        return "";
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }
}
