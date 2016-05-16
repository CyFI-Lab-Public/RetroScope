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

import java.util.ArrayList;

import android.content.Context;
import android.net.Uri;

import com.android.camera.util.PhotoSphereHelper;
import com.android.camera.util.PhotoSphereHelper.PanoramaMetadata;

/**
 * This class breaks out the off-thread panorama support.
 */
public class PanoramaMetadataLoader {
    /**
     * Classes implementing this interface can get information about loaded
     * photo sphere metadata.
     */
    public static interface PanoramaMetadataCallback {
        /**
         * Called with the loaded metadata or <code>null</code>.
         */
        public void onPanoramaMetadataLoaded(PanoramaMetadata metadata);
    }

    private PanoramaMetadata mPanoramaMetadata;
    private ArrayList<PanoramaMetadataCallback> mCallbacksWaiting;
    private Uri mMediaUri;

    /**
     * Instantiated the meta data loader for the image resource with the given
     * URI.
     */
    public PanoramaMetadataLoader(Uri uri) {
        mMediaUri = uri;
    }

    /**
     * Asynchronously extract and return panorama metadata from the item with
     * the given URI.
     * <p>
     * NOTE: This call is backed by a cache to speed up successive calls, which
     * will return immediately. Use {@link #clearCachedValues()} is called.
     */
    public synchronized void getPanoramaMetadata(final Context context,
            PanoramaMetadataCallback callback) {
        if (mPanoramaMetadata != null) {
            // Return the cached data right away, no need to fetch it again.
            callback.onPanoramaMetadataLoaded(mPanoramaMetadata);
        } else {
            if (mCallbacksWaiting == null) {
                mCallbacksWaiting = new ArrayList<PanoramaMetadataCallback>();

                // TODO: Don't create a new thread each time, use a pool or
                // single instance.
                (new Thread() {
                    @Override
                    public void run() {
                        onLoadingDone(PhotoSphereHelper.getPanoramaMetadata(context,
                                mMediaUri));
                    }
                }).start();
            }
            mCallbacksWaiting.add(callback);
        }
    }

    /**
     * Clear cached value and stop all running loading threads.
     */
    public synchronized void clearCachedValues() {
        if (mPanoramaMetadata != null) {
            mPanoramaMetadata = null;
        }

        // TODO: Cancel running loading thread if active.
     }

    private synchronized void onLoadingDone(PanoramaMetadata metadata) {
        mPanoramaMetadata = metadata;
        if (mPanoramaMetadata == null) {
            // Error getting panorama data from file. Treat as not panorama.
            mPanoramaMetadata = PhotoSphereHelper.NOT_PANORAMA;
        }
        for (PanoramaMetadataCallback cb : mCallbacksWaiting) {
            cb.onPanoramaMetadataLoaded(mPanoramaMetadata);
        }
        mCallbacksWaiting = null;
    }
}
