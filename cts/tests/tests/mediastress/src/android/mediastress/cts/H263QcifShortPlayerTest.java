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

public class H263QcifShortPlayerTest extends MediaPlayerStressTest {
    private final static String VIDEO_PATH_MIDDLE = "bbb_short/176x144/3gp_h263_libfaac/";
    private final String[] mMedias = {
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_12fps.libfaac_mono_24kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_12fps.libfaac_mono_24kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_12fps.libfaac_stereo_128kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_12fps.libfaac_stereo_128kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_12fps.libfaac_stereo_24kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_12fps.libfaac_stereo_24kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_25fps.libfaac_mono_24kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_25fps.libfaac_mono_24kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_25fps.libfaac_stereo_128kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_25fps.libfaac_stereo_128kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_25fps.libfaac_stereo_24kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_300kbps_25fps.libfaac_stereo_24kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_12fps.libfaac_mono_24kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_12fps.libfaac_mono_24kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_12fps.libfaac_stereo_128kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_12fps.libfaac_stereo_128kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_12fps.libfaac_stereo_24kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_12fps.libfaac_stereo_24kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_25fps.libfaac_mono_24kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_25fps.libfaac_mono_24kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_25fps.libfaac_stereo_128kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_25fps.libfaac_stereo_128kbps_22050Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_25fps.libfaac_stereo_24kbps_11025Hz.3gp",
        "bbb_short.ffmpeg.176x144.3gp.h263_56kbps_25fps.libfaac_stereo_24kbps_22050Hz.3gp"
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

    public void testPlay12() throws Exception {
        doTestVideoPlaybackShort(12);
    }

    public void testPlay13() throws Exception {
        doTestVideoPlaybackShort(13);
    }

    public void testPlay14() throws Exception {
        doTestVideoPlaybackShort(14);
    }

    public void testPlay15() throws Exception {
        doTestVideoPlaybackShort(15);
    }

    public void testPlay16() throws Exception {
        doTestVideoPlaybackShort(16);
    }

    public void testPlay17() throws Exception {
        doTestVideoPlaybackShort(17);
    }

    public void testPlay18() throws Exception {
        doTestVideoPlaybackShort(18);
    }

    public void testPlay19() throws Exception {
        doTestVideoPlaybackShort(19);
    }

    public void testPlay20() throws Exception {
        doTestVideoPlaybackShort(20);
    }

    public void testPlay21() throws Exception {
        doTestVideoPlaybackShort(21);
    }

    public void testPlay22() throws Exception {
        doTestVideoPlaybackShort(22);
    }

    public void testPlay23() throws Exception {
        doTestVideoPlaybackShort(23);
    }

    @Override
    protected String getFullVideoClipName(int mediaNumber) {
        return VIDEO_TOP_DIR + VIDEO_PATH_MIDDLE + mMedias[mediaNumber];
    }

}
