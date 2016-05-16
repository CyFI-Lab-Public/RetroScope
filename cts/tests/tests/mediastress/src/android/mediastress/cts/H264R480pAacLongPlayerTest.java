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

package android.mediastress.cts;

import android.media.CamcorderProfile;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.VideoEncoder;

public class H264R480pAacLongPlayerTest extends MediaPlayerStressTest {
    private static final String VIDEO_PATH_MIDDLE = "bbb_full/720x480/mp4_libx264_libfaac/";
    private final String[] mMedias = {
        "bbb_full.ffmpeg.720x480.mp4.libx264_500kbps_25fps.libfaac_stereo_128kbps_44100Hz.mp4"
    };

    public H264R480pAacLongPlayerTest() {
        super(CamcorderProfile.QUALITY_480P, VideoEncoder.H264, AudioEncoder.AAC);
    }

    public void testPlay00() throws Exception {
        doTestVideoPlaybackLong(0);
    }

    @Override
    protected String getFullVideoClipName(int mediaNumber) {
        return VIDEO_TOP_DIR + VIDEO_PATH_MIDDLE + mMedias[mediaNumber];
    }

}
