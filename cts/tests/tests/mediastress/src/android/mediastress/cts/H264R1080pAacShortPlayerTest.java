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

public class H264R1080pAacShortPlayerTest extends MediaPlayerStressTest {
    private static final String VIDEO_PATH_MIDDLE = "bbb_short/1920x1080/mp4_libx264_libfaac/";
    private final String[] mMedias = {
        "bbb_short.ffmpeg.1920x1080.mp4.libx264_10000kbps_30fps.libfaac_stereo_192kbps_48000Hz.mp4",
        "bbb_short.ffmpeg.1920x1080.mp4.libx264_1750kbps_30fps.libfaac_stereo_192kbps_48000Hz.mp4",
        "bbb_short.ffmpeg.1920x1080.mp4.libx264_5000kbps_30fps.libfaac_stereo_192kbps_48000Hz.mp4"
    };

    public H264R1080pAacShortPlayerTest() {
        super(CamcorderProfile.QUALITY_1080P, VideoEncoder.H264, AudioEncoder.AAC);
    }

    public void testPlay00() throws Exception {
        doTestVideoPlaybackShort(0);
    }

    public void testPlay01() throws Exception {
        doTestVideoPlaybackShort(1);
    }

    public void testPlay02() throws Exception {
        doTestVideoPlaybackShort(2);
    }

    @Override
    protected String getFullVideoClipName(int mediaNumber) {
        return VIDEO_TOP_DIR + VIDEO_PATH_MIDDLE + mMedias[mediaNumber];
    }

}
