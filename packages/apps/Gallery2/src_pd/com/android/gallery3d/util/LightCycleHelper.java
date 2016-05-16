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

package com.android.gallery3d.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

public class LightCycleHelper {
    public static class PanoramaMetadata {
        // Whether a panorama viewer should be used
        public final boolean mUsePanoramaViewer;
        // Whether a panorama is 360 degrees
        public final boolean mIsPanorama360;

        public PanoramaMetadata(boolean usePanoramaViewer, boolean isPanorama360) {
            mUsePanoramaViewer = usePanoramaViewer;
            mIsPanorama360 = isPanorama360;
        }
    }

    public static final PanoramaMetadata NOT_PANORAMA = new PanoramaMetadata(false, false);

    public static PanoramaMetadata getPanoramaMetadata(Context context, Uri uri) {
        return NOT_PANORAMA;
    }

    /**
     * Get the file path from a Media storage URI.
     */
    public static String getPathFromURI(ContentResolver contentResolver, Uri contentUri) {
        return null;
    }

    /**
     * Get the modified time from a Media storage URI.
     */
    public static long getModifiedTimeFromURI(ContentResolver contentResolver, Uri contentUri) {
        return 0;
    }
}
