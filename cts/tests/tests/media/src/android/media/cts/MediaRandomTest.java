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
package android.media.cts;


import com.android.cts.media.R;

import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.view.SurfaceHolder;
import android.test.ActivityInstrumentationTestCase2;
import android.os.Environment;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.util.Log;

import java.util.Random;

/**
 * Tests for the MediaPlayer.java and MediaRecorder.java APIs
 *
 * These testcases make randomized calls to the public APIs available, and
 * the focus is on whether the randomized calls can lead to crash in
 * mediaserver process and/or ANRs.
 *
 * The files in res/raw used by testLocalVideo* are (c) copyright 2008,
 * Blender Foundation / www.bigbuckbunny.org, and are licensed under the Creative Commons
 * Attribution 3.0 License at http://creativecommons.org/licenses/by/3.0/us/.
 */
public class MediaRandomTest extends ActivityInstrumentationTestCase2<MediaStubActivity> {
    private static final String TAG = "MediaRandomTest";

    private static final String OUTPUT_FILE =
                Environment.getExternalStorageDirectory().toString() + "/record.3gp";

    private static final int NUMBER_OF_RECORDER_RANDOM_ACTIONS = 100000;
    private static final int NUMBER_OF_PLAYER_RANDOM_ACTIONS   = 100000;

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private SurfaceHolder mSurfaceHolder;
    private Resources mResources;

    // Modified across multiple threads
    private volatile boolean mMediaServerDied;
    private volatile int mAction;
    private volatile int mParam;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getInstrumentation().waitForIdleSync();
        mMediaServerDied = false;
        mSurfaceHolder = getActivity().getSurfaceHolder();
        mResources = getInstrumentation().getTargetContext().getResources();
        try {
            // Running this on UI thread make sure that
            // onError callback can be received.
            runTestOnUiThread(new Runnable() {
                public void run() {
                    mRecorder = new MediaRecorder();
                    mPlayer = new MediaPlayer();
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        super.tearDown();
    }

    /**
     * This is a watchdog used to stop the process if it hasn't been pinged
     * for more than specified milli-seconds. It is used like:
     *
     * Watchdog w = new Watchdog(10000);  // 10 seconds.
     * w.start();       // start the watchdog.
     * ...
     * w.ping();
     * ...
     * w.ping();
     * ...
     * w.end();        // ask the watchdog to stop.
     * w.join();        // join the thread.
     */
    class Watchdog extends Thread {
        private final long mTimeoutMs;
        private boolean mWatchdogStop;
        private boolean mWatchdogPinged;

        public Watchdog(long timeoutMs) {
            mTimeoutMs = timeoutMs;
            mWatchdogStop = false;
            mWatchdogPinged = false;
        }

        public synchronized void run() {
            while (true) {
                // avoid early termination by "spurious" waitup.
                final long startTimeMs = System.currentTimeMillis();
                long remainingWaitTimeMs = mTimeoutMs;
                do {
                    try {
                        wait(remainingWaitTimeMs);
                    } catch (InterruptedException ex) {
                        // ignore.
                    }
                    remainingWaitTimeMs = mTimeoutMs - (System.currentTimeMillis() - startTimeMs);
                } while (remainingWaitTimeMs > 0);

                if (mWatchdogStop) {
                    break;
                }

                if (!mWatchdogPinged) {
                    fail("Action " + mAction + " Param " + mParam
                            + " waited over " + (mTimeoutMs - remainingWaitTimeMs) + " ms");
                    return;
                }
                mWatchdogPinged = false;
            }
        }

        public synchronized void ping() {
            mWatchdogPinged = true;
            this.notify();
        }

        public synchronized void end() {
            mWatchdogStop = true;
            this.notify();
        }
    }

    public MediaRandomTest() {
        super("com.android.cts.media", MediaStubActivity.class);
    }

    private void loadSource(int resid) throws Exception {
        AssetFileDescriptor afd = mResources.openRawResourceFd(resid);
        try {
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
        } finally {
            afd.close();
        }
    }

    public void testPlayerRandomAction() throws Exception {
        Watchdog watchdog = new Watchdog(5000);
        try {
            mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (mPlayer == mp &&
                        what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                        Log.e(TAG, "mediaserver process died");
                        mMediaServerDied = true;
                    }
                    return true;
                }
            });
            loadSource(R.raw.video_480x360_mp4_h264_500kbps_30fps_aac_stereo_128kbps_44100hz);
            mPlayer.setDisplay(mSurfaceHolder);
            mPlayer.prepare();
            mPlayer.start();

            long seed = System.currentTimeMillis();
            Log.v(TAG, "seed = " + seed);
            Random r = new Random(seed);

            watchdog.start();
            for (int i = 0; i < NUMBER_OF_PLAYER_RANDOM_ACTIONS; i++){
                watchdog.ping();
                assertTrue(!mMediaServerDied);

                mAction = (int)(r.nextInt() % 12);
                mParam = (int)(r.nextInt() % 1000000);
                try {
                    switch (mAction) {
                    case 0:
                        mPlayer.getCurrentPosition();
                        break;
                    case 1:
                        mPlayer.getDuration();
                        break;
                    case 2:
                        mPlayer.getVideoHeight();
                        break;
                    case 3:
                        mPlayer.getVideoWidth();
                       break;
                    case 4:
                        mPlayer.isPlaying();
                        break;
                    case 5:
                        mPlayer.pause();
                        break;
                    case 6:
                        // Don't add mPlayer.prepare() call here for two reasons:
                        // 1. calling prepare() is a bad idea since it is a blocking call, and
                        // 2. when prepare() is in progress, mediaserver died message will not be sent to apps
                        mPlayer.prepareAsync();
                        break;
                    case 7:
                        mPlayer.seekTo((int)(mParam));
                        break;
                    case 8:
                        mPlayer.setLooping(mParam % 2 == 0);
                        break;
                    case 9:
                        mPlayer.setVolume((mParam % 1000) / 500.0f,
                                     (mParam / 1000) / 500.0f);
                        break;
                    case 10:
                        mPlayer.start();
                        break;
                    case 11:
                        Thread.sleep(mParam % 20);
                        break;
                    }
                } catch (Exception e) {
                }
            }
            mPlayer.stop();
        } catch (Exception e) {
            Log.v(TAG, e.toString());
        } finally {
            watchdog.end();
            watchdog.join();
        }
    }

    public void testRecorderRandomAction() throws Exception {
        Watchdog watchdog = new Watchdog(5000);
        try {
            long seed = System.currentTimeMillis();
            Log.v(TAG, "seed = " + seed);
            Random r = new Random(seed);

            mMediaServerDied = false;
            mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                @Override
                public void onError(MediaRecorder recorder, int what, int extra) {
                    if (mRecorder == recorder &&
                        what == MediaRecorder.MEDIA_ERROR_SERVER_DIED) {
                        Log.e(TAG, "mediaserver process died");
                        mMediaServerDied = true;
                    }
                }
            });

            final int[] width  = {176, 352, 320, 640, 1280, 1920};
            final int[] height = {144, 288, 240, 480,  720, 1080};

            watchdog.start();
            for (int i = 0; i < NUMBER_OF_RECORDER_RANDOM_ACTIONS; i++) {
                watchdog.ping();
                assertTrue(!mMediaServerDied);

                mAction = (int)(r.nextInt(14));
                mParam = (int)(r.nextInt(1000000));
                try {
                    switch (mAction) {
                    case 0:
                        mRecorder.setAudioSource(mParam % 3);
                        break;
                    case 1:
                        // XXX:
                        // Fix gralloc source and change
                        // mRecorder.setVideoSource(mParam % 3);
                        mRecorder.setVideoSource(mParam % 2);
                        break;
                    case 2:
                        mRecorder.setOutputFormat(mParam % 5);
                        break;
                    case 3:
                        mRecorder.setAudioEncoder(mParam % 3);
                        break;
                    case 4:
                        mRecorder.setVideoEncoder(mParam % 5);
                        break;
                    case 5:
                        mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
                        break;
                    case 6:
                        int index = mParam % width.length;
                        mRecorder.setVideoSize(width[index], height[index]);
                        break;
                    case 7:
                        mRecorder.setVideoFrameRate(mParam % 40 - 5);
                        break;
                    case 8:
                        mRecorder.setOutputFile(OUTPUT_FILE);
                        break;
                    case 9:
                        mRecorder.prepare();
                        break;
                    case 10:
                        mRecorder.start();
                        break;
                    case 11:
                        Thread.sleep(mParam % 20);
                        break;
                    case 12:
                        mRecorder.stop();
                        break;
                    case 13:
                        mRecorder.reset();
                        break;
                    default:
                        break;
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            Log.v(TAG, e.toString());
        } finally {
            watchdog.end();
            watchdog.join();
        }
    }
}
