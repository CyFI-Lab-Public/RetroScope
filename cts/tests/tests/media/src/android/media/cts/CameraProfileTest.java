/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.media.cts;


import android.hardware.Camera;
import android.media.CameraProfile;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.List;

public class CameraProfileTest extends AndroidTestCase {

    private static final String TAG = "CameraProfileTest";

    private void checkQuality(int low, int mid, int high) {
        Log.v(TAG, "low = " + low + ", mid = " + mid + ", high = " + high);
        assertTrue(low >= 0 && low <= 100);
        assertTrue(mid >= 0 && mid <= 100);
        assertTrue(high >= 0 && high <= 100);
        assertTrue(low <= mid && mid <= high);
    }

    public void testGetImageEncodingQualityParameter() {
        int low = CameraProfile.getJpegEncodingQualityParameter(CameraProfile.QUALITY_LOW);
        int mid = CameraProfile.getJpegEncodingQualityParameter(CameraProfile.QUALITY_MEDIUM);
        int high = CameraProfile.getJpegEncodingQualityParameter(CameraProfile.QUALITY_HIGH);
        checkQuality(low, mid, high);
    }

    public void testGetWithId() {
        int nCamera = Camera.getNumberOfCameras();
        for (int id = 0; id < nCamera; id++) {
            int low = CameraProfile.getJpegEncodingQualityParameter(id, CameraProfile.QUALITY_LOW);
            int mid = CameraProfile.getJpegEncodingQualityParameter(id, CameraProfile.QUALITY_MEDIUM);
            int high = CameraProfile.getJpegEncodingQualityParameter(id, CameraProfile.QUALITY_HIGH);
            checkQuality(low, mid, high);
        }
    }
}
