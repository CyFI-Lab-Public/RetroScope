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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.webkit.cts.CtsTestServer;

import org.apache.http.HttpServerConnection;

import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.io.SocketOutputBuffer;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.params.HttpParams;
import org.apache.http.util.CharArrayBuffer;

import java.io.IOException;

import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Executes a range of tests on MediaPlayer while streaming a video
 * from an HTTP server over a simulated "flaky" network.
 */
public class MediaPlayerFlakyNetworkTest extends MediaPlayerTestBase {

    private static final String[] TEST_VIDEOS = {
        "raw/video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz",
        "raw/video_480x360_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz",
        "raw/video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz",
        "raw/video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz",
        "raw/video_176x144_3gp_h263_300kbps_25fps_aac_stereo_128kbps_22050hz"
    };

    // Allow operations to block for 2500ms before assuming they will ANR.
    // We don't allow the full 5s because cpu load, etc, reduces the budget.
    private static final int ANR_TIMEOUT_MILLIS = 2500;

    private CtsTestServer mServer;

    @Override
    public void tearDown() throws Exception {
        releaseMediaPlayer();
        releaseHttpServer();
        super.tearDown();
    }

    public void test_S0P0() throws Throwable {
        doPlayStreams(0, 0);
    }

    public void test_S1P000005() throws Throwable {
        doPlayStreams(1, 0.000005f);
    }

    public void test_S2P00001() throws Throwable {
        doPlayStreams(2, 0.00001f);
    }

    public void test_S3P00001() throws Throwable {
        doPlayStreams(3, 0.00001f);
    }

    public void test_S4P00001() throws Throwable {
        doPlayStreams(4, 0.00001f);
    }

    public void test_S5P00001() throws Throwable {
        doPlayStreams(5, 0.00001f);
    }

    public void test_S6P00002() throws Throwable {
        doPlayStreams(6, 0.00002f);
    }

   private void doPlayStreams(int seed, float probability) throws Throwable {
        Random random = new Random(seed);
        createHttpServer(seed, probability);
        for (int i = 0; i < 10; i++) {
            String video = getRandomTestVideo(random);
            doPlayMp4Stream(video, 20000, 5000);
            doAsyncPrepareAndRelease(video);
            doRandomOperations(video);
        }
        doPlayMp4Stream(getRandomTestVideo(random), 30000, 20000);
        releaseHttpServer();
    }

    private String getRandomTestVideo(Random random) {
        return TEST_VIDEOS[random.nextInt(TEST_VIDEOS.length)];
    }

    private void doPlayMp4Stream(String video, int millisToPrepare, int millisToPlay)
            throws Throwable {
        createMediaPlayer();
        localHttpStreamTest(video);

        mOnPrepareCalled.waitForSignal(millisToPrepare);
        if (mOnPrepareCalled.isSignalled()) {
            mMediaPlayer.start();
            Thread.sleep(millisToPlay);
        } else {
            // This could be because the "connection" was too slow.  Assume ok.
        }

        releaseMediaPlayerAndFailIfAnr();
    }

    private void doAsyncPrepareAndRelease(String video) throws Throwable {
        Random random = new Random(1);
        for (int i = 0; i < 10; i++) {
            createMediaPlayer();
            localHttpStreamTest(video);
            Thread.sleep(random.nextInt(500));
            releaseMediaPlayerAndFailIfAnr();
        }
    }

    private void doRandomOperations(String video) throws Throwable {
        Random random = new Random(1);
        createMediaPlayer();
        localHttpStreamTest(video);
        mOnPrepareCalled.waitForSignal(10000);
        if (mOnPrepareCalled.isSignalled()) {
            mMediaPlayer.start();
            for (int i = 0; i < 50; i++) {
                Thread.sleep(random.nextInt(100));
                switch (random.nextInt(3)) {
                    case 0:
                        mMediaPlayer.start();
                        assertTrue(mMediaPlayer.isPlaying());
                        break;
                    case 1:
                        mMediaPlayer.pause();
                        assertFalse(mMediaPlayer.isPlaying());
                        break;
                    case 2:
                        mMediaPlayer.seekTo(random.nextInt(10000));
                        break;
                }
            }
        } else {
          // Prepare took more than 10s, give up.
          // This could be because the "connection" was too slow
        }
        releaseMediaPlayerAndFailIfAnr();
    }

    private void releaseMediaPlayerAndFailIfAnr() throws Throwable {
        final Monitor releaseThreadRunning = new Monitor();
        Thread releaseThread = new Thread() {
            public void run() {
                releaseThreadRunning.signal();
                releaseMediaPlayer();
            }
        };
        releaseThread.start();
        releaseThreadRunning.waitForSignal();
        releaseThread.join(ANR_TIMEOUT_MILLIS);
        assertFalse("release took longer than " + ANR_TIMEOUT_MILLIS, releaseThread.isAlive());
    }

    private void createMediaPlayer() throws Throwable {
        if (mMediaPlayer == null) {
            mMediaPlayer = createMediaPlayerOnMainThread();
        }
    }

    private void releaseMediaPlayer() {
        MediaPlayer old = mMediaPlayer;
        mMediaPlayer = null;
        if (old != null) {
            old.release();
        }
    }

    private MediaPlayer createMediaPlayerOnMainThread() throws Throwable {
        Callable<MediaPlayer> callable = new Callable<MediaPlayer>() {
            @Override
            public MediaPlayer call() throws Exception {
                return new MediaPlayer();
            }
        };
        FutureTask<MediaPlayer> future = new FutureTask<MediaPlayer>(callable);
        getInstrumentation().runOnMainSync(future);
        return future.get();
    }


    private void createHttpServer(int seed, final float probability) throws Throwable {
        final Random random = new Random(seed);
        mServer = new CtsTestServer(mContext) {
            @Override
            protected DefaultHttpServerConnection createHttpServerConnection() {
                return new FlakyHttpServerConnection(random, probability);
            }
        };
    }

    private void releaseHttpServer() {
        if (mServer != null) {
            mServer.shutdown();
            mServer = null;
        }
    }

    private void localHttpStreamTest(final String name)
            throws Throwable {
        String stream_url = mServer.getAssetUrl(name);
        mMediaPlayer.setDataSource(stream_url);

        mMediaPlayer.setDisplay(getActivity().getSurfaceHolder());
        mMediaPlayer.setScreenOnWhilePlaying(true);

        mOnPrepareCalled.reset();
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mOnPrepareCalled.signal();
            }
        });

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                fail("Media player had error " + what + " extra " + extra + " playing " + name);
                return true;
            }
        });

        mMediaPlayer.prepareAsync();
    }

    private class FlakyHttpServerConnection extends DefaultHttpServerConnection {

        private final Random mRandom;
        private final float mProbability;

        public FlakyHttpServerConnection(Random random, float probability) {
            mRandom = random;
            mProbability = probability;
        }

        @Override
        protected SessionOutputBuffer createHttpDataTransmitter(
                Socket socket, int buffersize, HttpParams params) throws IOException {
            return createSessionOutputBuffer(socket, buffersize, params);
        }

        SessionOutputBuffer createSessionOutputBuffer(
                Socket socket, int buffersize, HttpParams params) throws IOException {
            return new SocketOutputBuffer(socket, buffersize, params) {
                @Override
                public void write(byte[] b) throws IOException {
                    write(b, 0, b.length);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    while (len-- > 0) {
                        write(b[off++]);
                    }
                }

                @Override
                public void writeLine(String s) throws IOException {
                    maybeDelayHeader(mProbability);
                    super.writeLine(s);
                }

                @Override
                public void writeLine(CharArrayBuffer buffer) throws IOException {
                    maybeDelayHeader(mProbability);
                    super.writeLine(buffer);
                }

                @Override
                public void write(int b) throws IOException {
                    maybeDelay(mProbability);
                    super.write(b);
                }

                private void maybeDelayHeader(float probability) throws IOException {
                    // Increase probability of delay when writing headers to simulate
                    // slow initial connection / server response.
                    maybeDelay(probability * 50);
                }

                private void maybeDelay(float probability) throws IOException {
                    try {
                        float random = mRandom.nextFloat();
                        if (random < probability) {
                            int sleepTimeMs = 1000 + mRandom.nextInt(5000);
                            Thread.sleep(sleepTimeMs);
                            flush();
                        } else if (random < probability * 100) {
                            Thread.sleep(1);
                            flush();
                        }
                    } catch (InterruptedException e) {
                        // Ignored
                    }
                }

            };
        }
    }
}
