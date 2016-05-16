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


import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.List;

public class CamcorderProfileTest extends AndroidTestCase {

    private static final String TAG = "CamcorderProfileTest";

    // Uses get without id if cameraId == -1 and get with id otherwise.
    private CamcorderProfile getWithOptionalId(int quality, int cameraId) {
        if (cameraId == -1) {
            return CamcorderProfile.get(quality);
        } else {
            return CamcorderProfile.get(cameraId, quality);
        }
    }

    private void checkProfile(CamcorderProfile profile, List<Size> videoSizes) {
        Log.v(TAG, String.format("profile: duration=%d, quality=%d, " +
            "fileFormat=%d, videoCodec=%d, videoBitRate=%d, videoFrameRate=%d, " +
            "videoFrameWidth=%d, videoFrameHeight=%d, audioCodec=%d, " +
            "audioBitRate=%d, audioSampleRate=%d, audioChannels=%d",
            profile.duration,
            profile.quality,
            profile.fileFormat,
            profile.videoCodec,
            profile.videoBitRate,
            profile.videoFrameRate,
            profile.videoFrameWidth,
            profile.videoFrameHeight,
            profile.audioCodec,
            profile.audioBitRate,
            profile.audioSampleRate,
            profile.audioChannels));
        assertTrue(profile.duration > 0);
        assertTrue(profile.quality == CamcorderProfile.QUALITY_LOW ||
                   profile.quality == CamcorderProfile.QUALITY_HIGH ||
                   profile.quality == CamcorderProfile.QUALITY_QCIF ||
                   profile.quality == CamcorderProfile.QUALITY_CIF ||
                   profile.quality == CamcorderProfile.QUALITY_480P ||
                   profile.quality == CamcorderProfile.QUALITY_720P ||
                   profile.quality == CamcorderProfile.QUALITY_1080P ||
                   profile.quality == CamcorderProfile.QUALITY_TIME_LAPSE_LOW ||
                   profile.quality == CamcorderProfile.QUALITY_TIME_LAPSE_HIGH ||
                   profile.quality == CamcorderProfile.QUALITY_TIME_LAPSE_QCIF ||
                   profile.quality == CamcorderProfile.QUALITY_TIME_LAPSE_CIF ||
                   profile.quality == CamcorderProfile.QUALITY_TIME_LAPSE_480P ||
                   profile.quality == CamcorderProfile.QUALITY_TIME_LAPSE_720P ||
                   profile.quality == CamcorderProfile.QUALITY_TIME_LAPSE_1080P);
        assertTrue(profile.videoBitRate > 0);
        assertTrue(profile.videoFrameRate > 0);
        assertTrue(profile.videoFrameWidth > 0);
        assertTrue(profile.videoFrameHeight > 0);
        assertTrue(profile.audioBitRate > 0);
        assertTrue(profile.audioSampleRate > 0);
        assertTrue(profile.audioChannels > 0);
        assertTrue(isSizeSupported(profile.videoFrameWidth,
                                   profile.videoFrameHeight,
                                   videoSizes));
    }

    private void assertProfileEquals(CamcorderProfile expectedProfile,
            CamcorderProfile actualProfile) {
        assertEquals(expectedProfile.duration, actualProfile.duration);
        assertEquals(expectedProfile.fileFormat, actualProfile.fileFormat);
        assertEquals(expectedProfile.videoCodec, actualProfile.videoCodec);
        assertEquals(expectedProfile.videoBitRate, actualProfile.videoBitRate);
        assertEquals(expectedProfile.videoFrameRate, actualProfile.videoFrameRate);
        assertEquals(expectedProfile.videoFrameWidth, actualProfile.videoFrameWidth);
        assertEquals(expectedProfile.videoFrameHeight, actualProfile.videoFrameHeight);
        assertEquals(expectedProfile.audioCodec, actualProfile.audioCodec);
        assertEquals(expectedProfile.audioBitRate, actualProfile.audioBitRate);
        assertEquals(expectedProfile.audioSampleRate, actualProfile.audioSampleRate);
        assertEquals(expectedProfile.audioChannels, actualProfile.audioChannels);
    }

    private void checkSpecificProfileDimensions(CamcorderProfile profile, int quality) {
        Log.v(TAG, String.format("specific profile: quality=%d, width = %d, height = %d",
                    profile.quality, profile.videoFrameWidth, profile.videoFrameHeight));

        switch (quality) {
            case CamcorderProfile.QUALITY_QCIF:
            case CamcorderProfile.QUALITY_TIME_LAPSE_QCIF:
                assertEquals(176, profile.videoFrameWidth);
                assertEquals(144, profile.videoFrameHeight);
                break;

            case CamcorderProfile.QUALITY_CIF:
            case CamcorderProfile.QUALITY_TIME_LAPSE_CIF:
                assertEquals(352, profile.videoFrameWidth);
                assertEquals(288, profile.videoFrameHeight);
                break;

            case CamcorderProfile.QUALITY_480P:
            case CamcorderProfile.QUALITY_TIME_LAPSE_480P:
                assertTrue(720 == profile.videoFrameWidth ||  // SMPTE 293M/ITU-R Rec. 601
                           640 == profile.videoFrameWidth ||  // ATSC/NTSC (square sampling)
                           704 == profile.videoFrameWidth);   // ATSC/NTSC (non-square sampling)
                assertEquals(480, profile.videoFrameHeight);
                break;

            case CamcorderProfile.QUALITY_720P:
            case CamcorderProfile.QUALITY_TIME_LAPSE_720P:
                assertEquals(1280, profile.videoFrameWidth);
                assertEquals(720, profile.videoFrameHeight);
                break;

            case CamcorderProfile.QUALITY_1080P:
            case CamcorderProfile.QUALITY_TIME_LAPSE_1080P:
                // 1080p could be either 1920x1088 or 1920x1080.
                assertEquals(1920, profile.videoFrameWidth);
                assertTrue(1088 == profile.videoFrameHeight ||
                           1080 == profile.videoFrameHeight);
                break;
        }
    }

    // Checks if the existing specific profiles have the correct dimensions.
    // Also checks that the mimimum quality specific profile matches the low profile and
    // similarly that the maximum quality specific profile matches the high profile.
    private void checkSpecificProfiles(
            int cameraId,
            CamcorderProfile low,
            CamcorderProfile high,
            int[] specificQualities,
            List<Size> videoSizes) {

        CamcorderProfile minProfile = null;
        CamcorderProfile maxProfile = null;

        for (int i = 0; i < specificQualities.length; i++) {
            int quality = specificQualities[i];
            if ((cameraId != -1 && CamcorderProfile.hasProfile(cameraId, quality)) ||
                (cameraId == -1 && CamcorderProfile.hasProfile(quality))) {
                CamcorderProfile profile = getWithOptionalId(quality, cameraId);
                checkSpecificProfileDimensions(profile, quality);

                assertTrue(isSizeSupported(profile.videoFrameWidth,
                                           profile.videoFrameHeight,
                                           videoSizes));

                if (minProfile == null) {
                    minProfile = profile;
                }
                maxProfile = profile;
            }
        }

        assertNotNull(minProfile);
        assertNotNull(maxProfile);

        Log.v(TAG, String.format("min profile: quality=%d, width = %d, height = %d",
                    minProfile.quality, minProfile.videoFrameWidth, minProfile.videoFrameHeight));
        Log.v(TAG, String.format("max profile: quality=%d, width = %d, height = %d",
                    maxProfile.quality, maxProfile.videoFrameWidth, maxProfile.videoFrameHeight));

        assertProfileEquals(low, minProfile);
        assertProfileEquals(high, maxProfile);

    }

    private void checkGet(int cameraId) {
        Log.v(TAG, (cameraId == -1)
                   ? "Checking get without id"
                   : "Checking get with id = " + cameraId);

        final List<Size> videoSizes = getSupportedVideoSizes(cameraId);

        CamcorderProfile lowProfile =
            getWithOptionalId(CamcorderProfile.QUALITY_LOW, cameraId);
        CamcorderProfile highProfile =
            getWithOptionalId(CamcorderProfile.QUALITY_HIGH, cameraId);
        checkProfile(lowProfile, videoSizes);
        checkProfile(highProfile, videoSizes);

        CamcorderProfile lowTimeLapseProfile =
            getWithOptionalId(CamcorderProfile.QUALITY_TIME_LAPSE_LOW, cameraId);
        CamcorderProfile highTimeLapseProfile =
            getWithOptionalId(CamcorderProfile.QUALITY_TIME_LAPSE_HIGH, cameraId);
        checkProfile(lowTimeLapseProfile, null);
        checkProfile(highTimeLapseProfile, null);

        int[] specificProfileQualities = {CamcorderProfile.QUALITY_QCIF,
                                          CamcorderProfile.QUALITY_QVGA,
                                          CamcorderProfile.QUALITY_CIF,
                                          CamcorderProfile.QUALITY_480P,
                                          CamcorderProfile.QUALITY_720P,
                                          CamcorderProfile.QUALITY_1080P};

        int[] specificTimeLapseProfileQualities = {CamcorderProfile.QUALITY_TIME_LAPSE_QCIF,
                                                   CamcorderProfile.QUALITY_TIME_LAPSE_QVGA,
                                                   CamcorderProfile.QUALITY_TIME_LAPSE_CIF,
                                                   CamcorderProfile.QUALITY_TIME_LAPSE_480P,
                                                   CamcorderProfile.QUALITY_TIME_LAPSE_720P,
                                                   CamcorderProfile.QUALITY_TIME_LAPSE_1080P};

        checkSpecificProfiles(cameraId, lowProfile, highProfile,
                specificProfileQualities, videoSizes);
        checkSpecificProfiles(cameraId, lowTimeLapseProfile, highTimeLapseProfile,
                specificTimeLapseProfileQualities, null);
    }

    public void testGet() {
        /*
         * Device may not have rear camera for checkGet(-1).
         * Checking PackageManager.FEATURE_CAMERA is included or not to decide the flow.
         * Continue if the feature is included.
         * Otherwise, exit test.
         */
        PackageManager pm = mContext.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return;
        }
        checkGet(-1);
    }

    public void testGetWithId() {
        int nCamera = Camera.getNumberOfCameras();
        for (int cameraId = 0; cameraId < nCamera; cameraId++) {
            checkGet(cameraId);
        }
    }

    private List<Size> getSupportedVideoSizes(int cameraId) {
        Camera camera = (cameraId == -1)? Camera.open(): Camera.open(cameraId);
        Parameters parameters = camera.getParameters();
        List<Size> videoSizes = parameters.getSupportedVideoSizes();
        if (videoSizes == null) {
            videoSizes = parameters.getSupportedPreviewSizes();
            assertNotNull(videoSizes);
        }
        camera.release();
        return videoSizes;
    }

    private boolean isSizeSupported(int width, int height, List<Size> sizes) {
        if (sizes == null) return true;

        for (Size size: sizes) {
            if (size.width == width && size.height == height) {
                return true;
            }
        }
        Log.e(TAG, "Size (" + width + "x" + height + ") is not supported");
        return false;
    }
}
