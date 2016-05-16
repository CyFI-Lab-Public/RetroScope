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

public class Vp8R480x360LongPlayerTest extends MediaPlayerStressTest {
    private static final String VIDEO_PATH_MIDDLE = "bbb_full/480x360/webm_libvpx_libvorbis/";
    private final String[] mMedias = {
        "bbb_full.ffmpeg.480x360.webm.libvpx_500kbps_25fps.libvorbis_stereo_128kbps_44100Hz.webm"
    };

    public void testPlay00() throws Exception {
        doTestVideoPlaybackLong(0);
    }

    @Override
    protected String getFullVideoClipName(int mediaNumber) {
        return VIDEO_TOP_DIR + VIDEO_PATH_MIDDLE + mMedias[mediaNumber];
    }

}
