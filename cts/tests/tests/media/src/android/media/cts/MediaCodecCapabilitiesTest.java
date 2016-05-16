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
package android.media.cts;

import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecList;
import android.media.MediaPlayer;

import android.util.Log;

/**
 * Basic sanity test of data returned by MediaCodeCapabilities.
 */
public class MediaCodecCapabilitiesTest extends MediaPlayerTestBase {

    private static final String TAG = "MediaCodecCapabilitiesTest";
    private static final String AVC_MIME = "video/avc";
    private static final int PLAY_TIME_MS = 30000;

    public void testAvcBaseline1() throws Exception {
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileBaseline,
                CodecProfileLevel.AVCLevel1)) {
            throw new RuntimeException("AVCLevel1 support is required by CDD");
        }
        // We don't have a test stream, but at least we're testing
        // that supports() returns true for something.
    }

    public void testAvcBaseline12() throws Exception {
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileBaseline,
                CodecProfileLevel.AVCLevel12)) {
            Log.i(TAG, "AvcBaseline12 not supported");
            return;  // TODO: Can we make this mandatory?
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=160&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&signature=341692D20FACCAE25B90EA2C131EA6ADCD8E2384."
                + "9EB08C174BE401AAD20FB85EE4DBA51A2882BB60"
                + "&key=test_key1", 256, 144, PLAY_TIME_MS);
    }

    public void testAvcBaseline30() throws Exception {
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileBaseline,
                CodecProfileLevel.AVCLevel3)) {
            Log.i(TAG, "AvcBaseline30 not supported");
            return;
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=18&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&signature=8701A45F6422229D46ABB25A22E2C00C94024606."
                + "08BCDF16C3F744C49D4C8A8AD1C38B3DC1810918"
                + "&key=test_key1", 640, 360, PLAY_TIME_MS);
    }

    public void testAvcHigh31() throws Exception {
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileHigh,
                CodecProfileLevel.AVCLevel31)) {
            Log.i(TAG, "AvcHigh31 not supported");
            return;
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=22&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&signature=42969CA8F7FFAE432B7135BC811F96F7C4172C3F."
                + "1A8A92EA714C1B7C98A05DDF2DE90854CDD7638B"
                + "&key=test_key1", 1280, 720, PLAY_TIME_MS);

    }

    public void testAvcHigh40() throws Exception {
        if (!supports(AVC_MIME, CodecProfileLevel.AVCProfileHigh,
                CodecProfileLevel.AVCLevel4)) {
            Log.i(TAG, "AvcHigh40 not supported");
            return;
        }
        playVideoWithRetries("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=37&source=youtube&user=android-device-test"
                + "&sparams=ip,ipbits,expire,id,itag,source,user"
                + "&ip=0.0.0.0&ipbits=0&expire=999999999999999999"
                + "&signature=7C3BBFB2F493E1BC396B6D31DDAF2E1367624487."
                + "64197F3BB46039669E912297DCD68D1FB2811D9F"
                + "&key=test_key1", 1920, 1080, PLAY_TIME_MS);
    }

    private boolean supports(String mimeType, int profile, int level) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (codecInfo.isEncoder()) {
                continue;
            }

            CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
            for (CodecProfileLevel profileLevel : capabilities.profileLevels) {
                if (profileLevel.profile == profile
                        && profileLevel.level >= level) {
                    return true;
                }
            }
        }

        return false;
    }

}
