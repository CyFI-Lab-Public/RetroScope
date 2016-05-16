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

import com.android.cts.util.TimeoutReq;

public class H263QcifLongPlayerTest extends MediaPlayerStressTest {
    private final static String VIDEO_PATH_MIDDLE = "bbb_full/176x144/3gp_h263_libfaac/";
    private final String[] mMedias = { // indentation shortened due to long file name
        "bbb_full.ffmpeg.176x144.3gp.h263_56kbps_12fps.libfaac_mono_24kbps_11025Hz.3gp"
    };

    @TimeoutReq(minutes = 11)
    public void testPlay00() throws Exception {
        doTestVideoPlaybackLong(0);
    }

    @Override
    protected String getFullVideoClipName(int mediaNumber) {
        return VIDEO_TOP_DIR + VIDEO_PATH_MIDDLE + mMedias[mediaNumber];
    }

}
