/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.media.MediaPlayer;
import android.webkit.cts.CtsTestServer;


/**
 * Tests of MediaPlayer streaming capabilities.
 */
public class StreamingMediaPlayerTest extends MediaPlayerTestBase {
    private CtsTestServer mServer;

/* RTSP tests are more flaky and vulnerable to network condition.
   Disable until better solution is available
    // Streaming RTSP video from YouTube
    public void testRTSP_H263_AMR_Video1() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0x271de9756065677e"
                + "&fmt=13&user=android-device-test", 176, 144);
    }
    public void testRTSP_H263_AMR_Video2() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0xc80658495af60617"
                + "&fmt=13&user=android-device-test", 176, 144);
    }

    public void testRTSP_MPEG4SP_AAC_Video1() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0x271de9756065677e"
                + "&fmt=17&user=android-device-test", 176, 144);
    }
    public void testRTSP_MPEG4SP_AAC_Video2() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0xc80658495af60617"
                + "&fmt=17&user=android-device-test", 176, 144);
    }

    public void testRTSP_H264Base_AAC_Video1() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0x271de9756065677e"
                + "&fmt=18&user=android-device-test", 480, 270);
    }
    public void testRTSP_H264Base_AAC_Video2() throws Exception {
        playVideoTest("rtsp://v2.cache7.c.youtube.com/video.3gp?cid=0xc80658495af60617"
                + "&fmt=18&user=android-device-test", 480, 270);
    }
*/
    // Streaming HTTP video from YouTube
    public void testHTTP_H263_AMR_Video1() throws Exception {
        playVideoTest("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=13&source=youtube&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&sparams=ip,ipbits,expire,id,itag,source"
                + "&signature=6F5AEC448AAF88466D7A10EBB76020745405D33F."
                + "5050D35AE997E1535FE828B0DE99EF31A699D9D0"
                + "&key=test_key1&user=android-device-test", 176, 144);
    }
    public void testHTTP_H263_AMR_Video2() throws Exception {
        playVideoTest("http://redirector.c.youtube.com/videoplayback?id=c80658495af60617"
                + "&itag=13&source=youtube&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&sparams=ip,ipbits,expire,id,itag,source"
                + "&signature=71749754E28115FD1C233E3BE96CDDC3F430CB74."
                + "49D1506DE694CC8FCEE63CB4F3AD41EB76C198CE"
                + "&key=test_key1&user=android-device-test", 176, 144);
    }

    public void testHTTP_MPEG4SP_AAC_Video1() throws Exception {
        playVideoTest("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=17&source=youtube&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&sparams=ip,ipbits,expire,id,itag,source"
                + "&signature=197A9742C1EFCA95725F2F26DFFD512FC48C149F."
                + "A59B42FD490F6B591B292F3B2659A9723B980351"
                + "&key=test_key1&user=android-device-test", 176, 144);
    }
    public void testHTTP_MPEG4SP_AAC_Video2() throws Exception {
        playVideoTest("http://redirector.c.youtube.com/videoplayback?id=c80658495af60617"
                + "&itag=17&source=youtube&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&sparams=ip,ipbits,expire,id,itag,source"
                + "&signature=0F84740A7E06F884127E78A6D7DE6DEA8F4B8BFD."
                + "248DF1E90B8137C30769C79BF23147F6BB3DFCDF"
                + "&key=test_key1&user=android-device-test", 176, 144);
    }

    public void testHTTP_H264Base_AAC_Video1() throws Exception {
        playVideoTest("http://redirector.c.youtube.com/videoplayback?id=271de9756065677e"
                + "&itag=18&source=youtube&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&sparams=ip,ipbits,expire,id,itag,source"
                + "&signature=3CFCAFB87EB9FC943FACDC54FEC8C725A801642C."
                + "7D77ACBC4CAF40349BF093E302B635757E45F345"
                + "&key=test_key1&user=android-device-test", 640, 360);
    }
    public void testHTTP_H264Base_AAC_Video2() throws Exception {
        playVideoTest("http://redirector.c.youtube.com/videoplayback?id=c80658495af60617"
                + "&itag=18&source=youtube&ip=0.0.0.0&ipbits=0&expire=19000000000"
                + "&sparams=ip,ipbits,expire,id,itag,source"
                + "&signature=A11D8BA0AA67A27F1409BE0C0B96B756625DB88B."
                + "9BF4C93A130583ADBDF2B953AD5A8A58F518B012"
                + "&key=test_key1&user=android-device-test", 640, 360);
    }

    // Streaming HLS video from YouTube
    public void testHLS() throws Exception {
        // Play stream for 60 seconds
        playLiveVideoTest("http://www.youtube.com/api/manifest/hls/ns/yt-live/id/UeHRu5LFHaU"
                + "?ip=0.0.0.0&ipbits=0&expire=19000000000&sparams=ip,ipbits,expire&signature"
                + "=313BE90526F2D815EB207156E1460C7E8EEC2503.799EE7B8B7CE3F2957060DB27C216077"
                + "0303EBD2&key=test_key1&user=android-device-test&m3u8=1", 60 * 1000);
    }

    // Streaming audio from local HTTP server
    public void testPlayMp3Stream1() throws Throwable {
        localHttpAudioStreamTest("ringer.mp3", false, false);
    }
    public void testPlayMp3Stream2() throws Throwable {
        localHttpAudioStreamTest("ringer.mp3", false, false);
    }
    public void testPlayMp3StreamRedirect() throws Throwable {
        localHttpAudioStreamTest("ringer.mp3", true, false);
    }
    public void testPlayMp3StreamNoLength() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.mp3", false, true);
    }
    public void testPlayOggStream() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.ogg", false, false);
    }
    public void testPlayOggStreamRedirect() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.ogg", true, false);
    }
    public void testPlayOggStreamNoLength() throws Throwable {
        localHttpAudioStreamTest("noiseandchirps.ogg", false, true);
    }

    private void localHttpAudioStreamTest(final String name, boolean redirect, boolean nolength)
            throws Throwable {
        mServer = new CtsTestServer(mContext);
        try {
            String stream_url = null;
            if (redirect) {
                // Stagefright doesn't have a limit, but we can't test support of infinite redirects
                // Up to 4 redirects seems reasonable though.
                stream_url = mServer.getRedirectingAssetUrl(name, 4);
            } else {
                stream_url = mServer.getAssetUrl(name);
            }
            if (nolength) {
                stream_url = stream_url + "?" + CtsTestServer.NOLENGTH_POSTFIX;
            }

            mMediaPlayer.setDataSource(stream_url);

            mMediaPlayer.setDisplay(getActivity().getSurfaceHolder());
            mMediaPlayer.setScreenOnWhilePlaying(true);

            mOnBufferingUpdateCalled.reset();
            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    mOnBufferingUpdateCalled.signal();
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    fail("Media player had error " + what + " playing " + name);
                    return true;
                }
            });

            assertFalse(mOnBufferingUpdateCalled.isSignalled());
            mMediaPlayer.prepare();

            if (nolength) {
                mMediaPlayer.start();
                Thread.sleep(LONG_SLEEP_TIME);
                assertFalse(mMediaPlayer.isPlaying());
            } else {
                mOnBufferingUpdateCalled.waitForSignal();
                mMediaPlayer.start();
                Thread.sleep(SLEEP_TIME);
            }
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        } finally {
            mServer.shutdown();
        }
    }
}
