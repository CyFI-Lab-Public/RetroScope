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

import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaObject.PanoramaSupportCallback;
import com.android.gallery3d.data.PanoramaMetadataJob;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.LightCycleHelper;
import com.android.gallery3d.util.LightCycleHelper.PanoramaMetadata;

import java.util.ArrayList;

/**
 * This class breaks out the off-thread panorama support checks so that the
 * complexity can be shared between UriImage and LocalImage, which need to
 * support panoramas.
 */
public class PanoramaMetadataSupport implements FutureListener<PanoramaMetadata> {
    private Object mLock = new Object();
    private Future<PanoramaMetadata> mGetPanoMetadataTask;
    private PanoramaMetadata mPanoramaMetadata;
    private ArrayList<PanoramaSupportCallback> mCallbacksWaiting;
    private MediaObject mMediaObject;

    public PanoramaMetadataSupport(MediaObject mediaObject) {
        mMediaObject = mediaObject;
    }

    public void getPanoramaSupport(GalleryApp app, PanoramaSupportCallback callback) {
        synchronized (mLock) {
            if (mPanoramaMetadata != null) {
                callback.panoramaInfoAvailable(mMediaObject, mPanoramaMetadata.mUsePanoramaViewer,
                        mPanoramaMetadata.mIsPanorama360);
            } else {
                if (mCallbacksWaiting == null) {
                    mCallbacksWaiting = new ArrayList<PanoramaSupportCallback>();
                    mGetPanoMetadataTask = app.getThreadPool().submit(
                            new PanoramaMetadataJob(app.getAndroidContext(),
                                    mMediaObject.getContentUri()), this);

                }
                mCallbacksWaiting.add(callback);
            }
        }
    }

    public void clearCachedValues() {
        synchronized (mLock) {
            if (mPanoramaMetadata != null) {
                mPanoramaMetadata = null;
            } else if (mGetPanoMetadataTask != null) {
                mGetPanoMetadataTask.cancel();
                for (PanoramaSupportCallback cb : mCallbacksWaiting) {
                    cb.panoramaInfoAvailable(mMediaObject, false, false);
                }
                mGetPanoMetadataTask = null;
                mCallbacksWaiting = null;
            }
        }
    }

    @Override
    public void onFutureDone(Future<PanoramaMetadata> future) {
        synchronized (mLock) {
            mPanoramaMetadata = future.get();
            if (mPanoramaMetadata == null) {
                // Error getting panorama data from file. Treat as not panorama.
                mPanoramaMetadata = LightCycleHelper.NOT_PANORAMA;
            }
            for (PanoramaSupportCallback cb : mCallbacksWaiting) {
                cb.panoramaInfoAvailable(mMediaObject, mPanoramaMetadata.mUsePanoramaViewer,
                        mPanoramaMetadata.mIsPanorama360);
            }
            mGetPanoMetadataTask = null;
            mCallbacksWaiting = null;
        }
    }
}
