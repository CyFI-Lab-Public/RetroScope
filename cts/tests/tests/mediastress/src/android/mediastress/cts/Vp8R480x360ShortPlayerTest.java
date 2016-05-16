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

public class Vp8R480x360ShortPlayerTest extends MediaPlayerStressTest {
    private static final String VIDEO_PATH_MIDDLE = "bbb_short/480x360/webm_libvpx_libvorbis/";
    private final String[] mMedias = {
        "bbb_short.ffmpeg.480x360.webm.libvpx_1000kbps_25fps.libvorbis_stereo_128kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_1000kbps_25fps.libvorbis_stereo_192kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_1000kbps_30fps.libvorbis_stereo_128kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_1000kbps_30fps.libvorbis_stereo_192kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_1350kbps_25fps.libvorbis_stereo_128kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_1350kbps_25fps.libvorbis_stereo_192kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_1350kbps_30fps.libvorbis_stereo_128kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_1350kbps_30fps.libvorbis_stereo_192kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_500kbps_25fps.libvorbis_stereo_128kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_500kbps_25fps.libvorbis_stereo_192kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_500kbps_30fps.libvorbis_stereo_128kbps_44100Hz.webm",
        "bbb_short.ffmpeg.480x360.webm.libvpx_500kbps_30fps.libvorbis_stereo_192kbps_44100Hz.webm"
    };

    public void testPlay00() throws Exception {
        doTestVideoPlaybackShort(0);
    }

    public void testPlay01() throws Exception {
        doTestVideoPlaybackShort(1);
    }

    public void testPlay02() throws Exception {
        doTestVideoPlaybackShort(2);
    }

    public void testPlay03() throws Exception {
        doTestVideoPlaybackShort(3);
    }

    public void testPlay04() throws Exception {
        doTestVideoPlaybackShort(4);
    }

    public void testPlay05() throws Exception {
        doTestVideoPlaybackShort(5);
    }

    public void testPlay06() throws Exception {
        doTestVideoPlaybackShort(6);
    }

    public void testPlay07() throws Exception {
        doTestVideoPlaybackShort(7);
    }

    public void testPlay08() throws Exception {
        doTestVideoPlaybackShort(8);
    }

    public void testPlay09() throws Exception {
        doTestVideoPlaybackShort(9);
    }

    public void testPlay10() throws Exception {
        doTestVideoPlaybackShort(10);
    }

    public void testPlay11() throws Exception {
        doTestVideoPlaybackShort(11);
    }

    @Override
    protected String getFullVideoClipName(int mediaNumber) {
        return VIDEO_TOP_DIR + VIDEO_PATH_MIDDLE + mMedias[mediaNumber];
    }

}
