/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaMetadataRetriever;
import android.media.TimedText;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Visualizer;
import android.media.cts.MediaPlayerTestBase.Monitor;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

/**
 * Tests for the MediaPlayer API and local video/audio playback.
 *
 * The files in res/raw used by testLocalVideo* are (c) copyright 2008,
 * Blender Foundation / www.bigbuckbunny.org, and are licensed under the Creative Commons
 * Attribution 3.0 License at http://creativecommons.org/licenses/by/3.0/us/.
 */
public class MediaPlayerTest extends MediaPlayerTestBase {

    private String RECORDED_FILE;
    private static final String LOG_TAG = "MediaPlayerTest";

    private static final int  RECORDED_VIDEO_WIDTH  = 176;
    private static final int  RECORDED_VIDEO_HEIGHT = 144;
    private static final long RECORDED_DURATION_MS  = 3000;
    private Vector<Integer> mTimedTextTrackIndex = new Vector<Integer>();
    private int mSelectedTimedTextIndex;
    private Monitor mOnTimedTextCalled = new Monitor();

    private File mOutFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        RECORDED_FILE = new File(Environment.getExternalStorageDirectory(),
                "mediaplayer_record.out").getAbsolutePath();
        mOutFile = new File(RECORDED_FILE);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mOutFile != null && mOutFile.exists()) {
            mOutFile.delete();
        }
    }
    public void testPlayNullSource() throws Exception {
        try {
            mMediaPlayer.setDataSource((String) null);
            fail("Null URI was accepted");
        } catch (RuntimeException e) {
            // expected
        }
    }

    public void testPlayAudio() throws Exception {
        final int mp3Duration = 34909;
        final int tolerance = 70;
        final int seekDuration = 100;
        final int resid = R.raw.testmp3_2;

        MediaPlayer mp = MediaPlayer.create(mContext, resid);
        try {
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);

            assertFalse(mp.isPlaying());
            mp.start();
            assertTrue(mp.isPlaying());

            assertFalse(mp.isLooping());
            mp.setLooping(true);
            assertTrue(mp.isLooping());

            assertEquals(mp3Duration, mp.getDuration(), tolerance);
            int pos = mp.getCurrentPosition();
            assertTrue(pos >= 0);
            assertTrue(pos < mp3Duration - seekDuration);

            mp.seekTo(pos + seekDuration);
            assertEquals(pos + seekDuration, mp.getCurrentPosition(), tolerance);

            // test pause and restart
            mp.pause();
            Thread.sleep(SLEEP_TIME);
            assertFalse(mp.isPlaying());
            mp.start();
            assertTrue(mp.isPlaying());

            // test stop and restart
            mp.stop();
            mp.reset();
            AssetFileDescriptor afd = mResources.openRawResourceFd(resid);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            assertFalse(mp.isPlaying());
            mp.start();
            assertTrue(mp.isPlaying());

            // waiting to complete
            while(mp.isPlaying()) {
                Thread.sleep(SLEEP_TIME);
            }
        } finally {
            mp.release();
        }
    }

    public void testPlayVideo() throws Exception {
        playVideoTest(R.raw.testvideo, 352, 288);
    }

    private void initMediaPlayer(MediaPlayer player) throws Exception {
        AssetFileDescriptor afd = mResources.openRawResourceFd(R.raw.test1m1s);
        try {
            player.reset();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.prepare();
            player.seekTo(56000);
        } finally {
            afd.close();
        }
    }

    public void testSetNextMediaPlayerWithReset() throws Exception {

        initMediaPlayer(mMediaPlayer);

        try {
            initMediaPlayer(mMediaPlayer2);
            mMediaPlayer2.reset();
            mMediaPlayer.setNextMediaPlayer(mMediaPlayer2);
            fail("setNextMediaPlayer() succeeded with unprepared player");
        } catch (RuntimeException e) {
            // expected
        } finally {
            mMediaPlayer.reset();
        }
    }

    public void testSetNextMediaPlayerWithRelease() throws Exception {

        initMediaPlayer(mMediaPlayer);

        try {
            initMediaPlayer(mMediaPlayer2);
            mMediaPlayer2.release();
            mMediaPlayer.setNextMediaPlayer(mMediaPlayer2);
            fail("setNextMediaPlayer() succeeded with unprepared player");
        } catch (RuntimeException e) {
            // expected
        } finally {
            mMediaPlayer.reset();
        }
    }

    public void testSetNextMediaPlayer() throws Exception {
        initMediaPlayer(mMediaPlayer);

        final Monitor mTestCompleted = new Monitor();

        Thread timer = new Thread(new Runnable() {

            @Override
            public void run() {
                long startTime = SystemClock.elapsedRealtime();
                while(true) {
                    SystemClock.sleep(SLEEP_TIME);
                    if (mTestCompleted.isSignalled()) {
                        // done
                        return;
                    }
                    long now = SystemClock.elapsedRealtime();
                    if ((now - startTime) > 25000) {
                        // We've been running for 25 seconds and still aren't done, so we're stuck
                        // somewhere. Signal ourselves to dump the thread stacks.
                        android.os.Process.sendSignal(android.os.Process.myPid(), 3);
                        SystemClock.sleep(2000);
                        fail("Test is stuck, see ANR stack trace for more info. You may need to" +
                                " create /data/anr first");
                        return;
                    }
                }
            }
        });

        timer.start();

        try {
            for (int i = 0; i < 3; i++) {

                initMediaPlayer(mMediaPlayer2);
                mOnCompletionCalled.reset();
                mOnInfoCalled.reset();
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        assertEquals(mMediaPlayer, mp);
                        mOnCompletionCalled.signal();
                    }
                });
                mMediaPlayer2.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mp, int what, int extra) {
                        assertEquals(mMediaPlayer2, mp);
                        if (what == MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT) {
                            mOnInfoCalled.signal();
                        }
                        return false;
                    }
                });

                mMediaPlayer.setNextMediaPlayer(mMediaPlayer2);
                mMediaPlayer.start();
                assertTrue(mMediaPlayer.isPlaying());
                assertFalse(mOnCompletionCalled.isSignalled());
                assertFalse(mMediaPlayer2.isPlaying());
                assertFalse(mOnInfoCalled.isSignalled());
                while(mMediaPlayer.isPlaying()) {
                    Thread.sleep(SLEEP_TIME);
                }
                // wait a little longer in case the callbacks haven't quite made it through yet
                Thread.sleep(100);
                assertTrue(mMediaPlayer2.isPlaying());
                assertTrue(mOnCompletionCalled.isSignalled());
                assertTrue(mOnInfoCalled.isSignalled());

                // At this point the 1st player is done, and the 2nd one is playing.
                // Now swap them, and go through the loop again.
                MediaPlayer tmp = mMediaPlayer;
                mMediaPlayer = mMediaPlayer2;
                mMediaPlayer2 = tmp;
            }

            // Now test that setNextMediaPlayer(null) works. 1 is still playing, 2 is done
            mOnCompletionCalled.reset();
            mOnInfoCalled.reset();
            initMediaPlayer(mMediaPlayer2);
            mMediaPlayer.setNextMediaPlayer(mMediaPlayer2);

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    assertEquals(mMediaPlayer, mp);
                    mOnCompletionCalled.signal();
                }
            });
            mMediaPlayer2.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    assertEquals(mMediaPlayer2, mp);
                    if (what == MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT) {
                        mOnInfoCalled.signal();
                    }
                    return false;
                }
            });
            assertTrue(mMediaPlayer.isPlaying());
            assertFalse(mOnCompletionCalled.isSignalled());
            assertFalse(mMediaPlayer2.isPlaying());
            assertFalse(mOnInfoCalled.isSignalled());
            Thread.sleep(SLEEP_TIME);
            mMediaPlayer.setNextMediaPlayer(null);
            while(mMediaPlayer.isPlaying()) {
                Thread.sleep(SLEEP_TIME);
            }
            // wait a little longer in case the callbacks haven't quite made it through yet
            Thread.sleep(100);
            assertFalse(mMediaPlayer.isPlaying());
            assertFalse(mMediaPlayer2.isPlaying());
            assertTrue(mOnCompletionCalled.isSignalled());
            assertFalse(mOnInfoCalled.isSignalled());

        } finally {
            mMediaPlayer.reset();
            mMediaPlayer2.reset();
        }
        mTestCompleted.signal();

    }

    // The following tests are all a bit flaky, which is why they're retried a
    // few times in a loop.

    // This test uses one mp3 that is silent but has a strong positive DC offset,
    // and a second mp3 that is also silent but has a strong negative DC offset.
    // If the two are played back overlapped, they will cancel each other out,
    // and result in zeroes being detected. If there is a gap in playback, that
    // will also result in zeroes being detected.
    // Note that this test does NOT guarantee that the correct data is played
    public void testGapless1() throws Exception {
        flakyTestWrapper(R.raw.monodcpos, R.raw.monodcneg);
    }

    // This test is similar, but uses two identical m4a files that have some noise
    // with a strong positive DC offset. This is used to detect if there is
    // a gap in playback
    // Note that this test does NOT guarantee that the correct data is played
    public void testGapless2() throws Exception {
        flakyTestWrapper(R.raw.stereonoisedcpos, R.raw.stereonoisedcpos);
    }

    // same as above, but with a mono file
    public void testGapless3() throws Exception {
        flakyTestWrapper(R.raw.mononoisedcpos, R.raw.mononoisedcpos);
    }

    private void flakyTestWrapper(int resid1, int resid2) throws Exception {
        boolean success = false;
        // test usually succeeds within a few tries, but occasionally may fail
        // many times in a row, so be aggressive and try up to 20 times
        for (int i = 0; i < 20 && !success; i++) {
            try {
                testGapless(resid1, resid2);
                success = true;
            } catch (Throwable t) {
                SystemClock.sleep(1000);
            }
        }
        // Try one more time. If this succeeds, we'll consider the test a success,
        // otherwise the exception gets thrown
        if (!success) {
            testGapless(resid1, resid2);
        }
    }

    private void testGapless(int resid1, int resid2) throws Exception {

        MediaPlayer mp1 = new MediaPlayer();
        mp1.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(resid1);
            mp1.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp1.prepare();
        } catch (Exception e) {
            assertTrue(false);
        }
        int session = mp1.getAudioSessionId();

        MediaPlayer mp2 = new MediaPlayer();
        mp2.setAudioSessionId(session);
        mp2.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(resid2);
            mp2.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp2.prepare();
        } catch (Exception e) {
            assertTrue(false);
        }
        // creating a volume controller on output mix ensures that ro.audio.silent mutes
        // audio after the effects and not before
        AudioEffect vc = new AudioEffect(
                            AudioEffect.EFFECT_TYPE_NULL,
                            UUID.fromString("119341a0-8469-11df-81f9-0002a5d5c51b"),
                            0,
                            session);
        vc.setEnabled(true);
        int captureintervalms = mp1.getDuration() + mp2.getDuration() - 2000;
        int size = 256;
        int[] range = Visualizer.getCaptureSizeRange();
        if (size < range[0]) {
            size = range[0];
        }
        if (size > range[1]) {
            size = range[1];
        }
        byte [] vizdata = new byte[size];
        Visualizer vis = new Visualizer(session);
        assertTrue(vis.setCaptureSize(vizdata.length) == Visualizer.SUCCESS);
        assertTrue(vis.setEnabled(true) == Visualizer.SUCCESS);
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int oldRingerMode = am.getRingerMode();
        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        int oldvolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);
        try {
            mp1.setNextMediaPlayer(mp2);
            mp1.start();
            assertTrue(mp1.isPlaying());
            assertFalse(mp2.isPlaying());
            // allow playback to get started
            Thread.sleep(SLEEP_TIME);
            long start = SystemClock.elapsedRealtime();
            // there should be no consecutive zeroes (-128) in the capture buffer
            // when going to the next file. If silence is detected right away, then
            // the volume is probably turned all the way down (visualizer data
            // is captured after volume adjustment).
            boolean first = true;
            while((SystemClock.elapsedRealtime() - start) < captureintervalms) {
                assertTrue(vis.getWaveForm(vizdata) == Visualizer.SUCCESS);
                for (int i = 0; i < vizdata.length - 1; i++) {
                    if (vizdata[i] == -128 && vizdata[i + 1] == -128) {
                        if (first) {
                            fail("silence detected, please increase volume and rerun test");
                        } else {
                            fail("gap or overlap detected at t=" +
                                    (SLEEP_TIME + SystemClock.elapsedRealtime() - start) +
                                    ", offset " + i);
                        }
                        break;
                    }
                }
                first = false;
            }
        } finally {
            mp1.release();
            mp2.release();
            vis.release();
            vc.release();
            am.setRingerMode(oldRingerMode);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, oldvolume, 0);
        }
    }

    /**
     * Test for reseting a surface during video playback
     * After reseting, the video should continue playing
     * from the time setDisplay() was called
     */
    public void testVideoSurfaceResetting() throws Exception {
        final int tolerance = 150;
        final int audioLatencyTolerance = 1000;  /* covers audio path latency variability */
        final int seekPos = 5000;

        final CountDownLatch seekDone = new CountDownLatch(1);

        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                seekDone.countDown();
            }
        });

        loadResource(R.raw.testvideo);
        playLoadedVideo(352, 288, -1);

        Thread.sleep(SLEEP_TIME);

        int posBefore = mMediaPlayer.getCurrentPosition();
        mMediaPlayer.setDisplay(getActivity().getSurfaceHolder2());
        int posAfter = mMediaPlayer.getCurrentPosition();

        assertEquals(posAfter, posBefore, tolerance);
        assertTrue(mMediaPlayer.isPlaying());

        Thread.sleep(SLEEP_TIME);

        mMediaPlayer.seekTo(seekPos);
        seekDone.await();
        posAfter = mMediaPlayer.getCurrentPosition();
        assertEquals(seekPos, posAfter, tolerance + audioLatencyTolerance);

        Thread.sleep(SLEEP_TIME / 2);
        posBefore = mMediaPlayer.getCurrentPosition();
        mMediaPlayer.setDisplay(null);
        posAfter = mMediaPlayer.getCurrentPosition();
        assertEquals(posAfter, posBefore, tolerance);
        assertTrue(mMediaPlayer.isPlaying());

        Thread.sleep(SLEEP_TIME);

        posBefore = mMediaPlayer.getCurrentPosition();
        mMediaPlayer.setDisplay(getActivity().getSurfaceHolder());
        posAfter = mMediaPlayer.getCurrentPosition();

        assertEquals(posAfter, posBefore, tolerance);
        assertTrue(mMediaPlayer.isPlaying());

        Thread.sleep(SLEEP_TIME);
    }

    public void testRecordedVideoPlayback0() throws Exception {
        testRecordedVideoPlaybackWithAngle(0);
    }

    public void testRecordedVideoPlayback90() throws Exception {
        testRecordedVideoPlaybackWithAngle(90);
    }

    public void testRecordedVideoPlayback180() throws Exception {
        testRecordedVideoPlaybackWithAngle(180);
    }

    public void testRecordedVideoPlayback270() throws Exception {
        testRecordedVideoPlaybackWithAngle(270);
    }

    private boolean hasCamera() {
        return getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private void testRecordedVideoPlaybackWithAngle(int angle) throws Exception {
        final int width = RECORDED_VIDEO_WIDTH;
        final int height = RECORDED_VIDEO_HEIGHT;
        final String file = RECORDED_FILE;
        final long durationMs = RECORDED_DURATION_MS;

        if (!hasCamera()) {
            return;
        }
        checkOrientation(angle);
        recordVideo(width, height, angle, file, durationMs);
        checkDisplayedVideoSize(width, height, angle, file);
        checkVideoRotationAngle(angle, file);
    }

    private void checkOrientation(int angle) throws Exception {
        assertTrue(angle >= 0);
        assertTrue(angle < 360);
        assertTrue((angle % 90) == 0);
    }

    private void recordVideo(
            int w, int h, int angle, String file, long durationMs) throws Exception {

        MediaRecorder recorder = new MediaRecorder();
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(file);
        recorder.setOrientationHint(angle);
        recorder.setVideoSize(w, h);
        recorder.setPreviewDisplay(getActivity().getSurfaceHolder2().getSurface());
        recorder.prepare();
        recorder.start();
        Thread.sleep(durationMs);
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    private void checkDisplayedVideoSize(
            int w, int h, int angle, String file) throws Exception {

        int displayWidth  = w;
        int displayHeight = h;
        if ((angle % 180) != 0) {
            displayWidth  = h;
            displayHeight = w;
        }
        playVideoTest(file, displayWidth, displayHeight);
    }

    private void checkVideoRotationAngle(int angle, String file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(file);
        String rotation = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        retriever.release();
        retriever = null;
        assertNotNull(rotation);
        assertEquals(Integer.parseInt(rotation), angle);
    }

    public void testLocalVideo_MP4_H264_480x360_500kbps_25fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_500kbps_25fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    public void testLocalVideo_MP4_H264_480x360_500kbps_30fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_500kbps_30fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    public void testLocalVideo_MP4_H264_480x360_1000kbps_25fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1000kbps_25fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    public void testLocalVideo_MP4_H264_480x360_1000kbps_30fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1000kbps_30fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    public void testLocalVideo_MP4_H264_480x360_1350kbps_25fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1350kbps_25fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    public void testLocalVideo_MP4_H264_480x360_1350kbps_30fps_AAC_Stereo_128kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_128kbps_44100hz, 480, 360);
    }

    public void testLocalVideo_MP4_H264_480x360_1350kbps_30fps_AAC_Stereo_192kbps_44110Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz, 480, 360);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Mono_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_mono_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Mono_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_mono_24kbps_22050hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Stereo_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Stereo_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Stereo_128kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_12fps_AAC_Stereo_128kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_12fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Mono_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_mono_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Mono_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_mono_24kbps_22050hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Stereo_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Stereo_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Stereo_128kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_56kbps_25fps_AAC_Stereo_128kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_56kbps_25fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Mono_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_mono_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Mono_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_mono_24kbps_22050hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Stereo_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Stereo_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Stereo_128kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_12fps_AAC_Stereo_128kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_12fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Mono_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_mono_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Mono_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_mono_24kbps_22050hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Stereo_24kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Stereo_24kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_stereo_24kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Stereo_128kbps_11025Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_stereo_128kbps_11025hz, 176, 144);
    }

    public void testLocalVideo_3gp_H263_176x144_300kbps_25fps_AAC_Stereo_128kbps_22050Hz()
            throws Exception {
        playVideoTest(
                R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_stereo_128kbps_22050hz, 176, 144);
    }

    private void readTimedTextTracks() throws Exception {
        mTimedTextTrackIndex.clear();
        MediaPlayer.TrackInfo[] trackInfos = mMediaPlayer.getTrackInfo();
        if (trackInfos == null || trackInfos.length == 0) {
            return;
        }
        for (int i = 0; i < trackInfos.length; ++i) {
            assertTrue(trackInfos[i] != null);
            if (trackInfos[i].getTrackType() ==
                 MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
                mTimedTextTrackIndex.add(i);
            }
        }
    }

    private int getTimedTextTrackCount() {
        return mTimedTextTrackIndex.size();
    }

    private void selectSubtitleTrack(int index) throws Exception {
        int trackIndex = mTimedTextTrackIndex.get(index);
        mMediaPlayer.selectTrack(trackIndex);
        mSelectedTimedTextIndex = index;
    }

    private void deselectSubtitleTrack(int index) throws Exception {
        int trackIndex = mTimedTextTrackIndex.get(index);
        mMediaPlayer.deselectTrack(trackIndex);
        if (mSelectedTimedTextIndex == index) {
            mSelectedTimedTextIndex = -1;
        }
    }

    public void testDeselectTrack() throws Exception {
        loadResource(R.raw.testvideo_with_2_subtitles);
        loadSubtitleSource(R.raw.test_subtitle1_srt);
        readTimedTextTracks();
        assertEquals(getTimedTextTrackCount(), 3);

        mMediaPlayer.setDisplay(getActivity().getSurfaceHolder());
        mMediaPlayer.setScreenOnWhilePlaying(true);
        mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setOnTimedTextListener(new MediaPlayer.OnTimedTextListener() {
            @Override
            public void onTimedText(MediaPlayer mp, TimedText text) {
                if (text != null) {
                    String plainText = text.getText();
                    if (plainText != null) {
                        mOnTimedTextCalled.signal();
                        Log.d(LOG_TAG, "text: " + plainText.trim());
                    }
                }
            }
        });
        mMediaPlayer.prepare();
        mMediaPlayer.start();
        assertTrue(mMediaPlayer.isPlaying());

        // Run twice to check if repeated selection-deselection on the same track works well.
        for (int i = 0; i < 2; i++) {
            // Waits until at least one subtitle is fired. Timeout is 1 sec.
            selectSubtitleTrack(0);
            mOnTimedTextCalled.reset();
            assertTrue(mOnTimedTextCalled.waitForSignal(1000));

            // Try deselecting track.
            deselectSubtitleTrack(0);
            mOnTimedTextCalled.reset();
            assertFalse(mOnTimedTextCalled.waitForSignal(1000));
        }

        // Run the same test for external subtitle track.
        for (int i = 0; i < 2; i++) {
            selectSubtitleTrack(2);
            mOnTimedTextCalled.reset();
            assertTrue(mOnTimedTextCalled.waitForSignal(1000));

            // Try deselecting track.
            deselectSubtitleTrack(2);
            mOnTimedTextCalled.reset();
            assertFalse(mOnTimedTextCalled.waitForSignal(1000));
        }

        try {
            deselectSubtitleTrack(0);
            fail("Deselecting unselected track: expected RuntimeException, " +
                 "but no exception has been triggered.");
        } catch (RuntimeException e) {
            // expected
        }

        mMediaPlayer.stop();
    }

    public void testChangeSubtitleTrack() throws Exception {
        loadResource(R.raw.testvideo_with_2_subtitles);
        readTimedTextTracks();
        assertEquals(getTimedTextTrackCount(), 2);

        // Adds two more external subtitle files.
        loadSubtitleSource(R.raw.test_subtitle1_srt);
        loadSubtitleSource(R.raw.test_subtitle2_srt);
        readTimedTextTracks();
        assertEquals(getTimedTextTrackCount(), 4);

        mMediaPlayer.setDisplay(getActivity().getSurfaceHolder());
        mMediaPlayer.setScreenOnWhilePlaying(true);
        mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setOnTimedTextListener(new MediaPlayer.OnTimedTextListener() {
            @Override
            public void onTimedText(MediaPlayer mp, TimedText text) {
                final int toleranceMs = 100;
                final int durationMs = 500;
                int posMs = mMediaPlayer.getCurrentPosition();
                if (text != null) {
                    String plainText = text.getText();
                    if (plainText != null) {
                        StringTokenizer tokens = new StringTokenizer(plainText.trim(), ":");
                        int subtitleTrackIndex = Integer.parseInt(tokens.nextToken());
                        int startMs = Integer.parseInt(tokens.nextToken());
                        Log.d(LOG_TAG, "text: " + plainText.trim() +
                              ", trackId: " + subtitleTrackIndex + ", posMs: " + posMs);
                        assertTrue("The diff between subtitle's start time " + startMs +
                                   " and current time " + posMs +
                                   " is over tolerance " + toleranceMs,
                                   (posMs >= startMs - toleranceMs) &&
                                   (posMs < startMs + durationMs + toleranceMs) );
                        assertEquals("Expected track: " + mSelectedTimedTextIndex +
                                     ", actual track: " + subtitleTrackIndex,
                                     mSelectedTimedTextIndex, subtitleTrackIndex);
                        mOnTimedTextCalled.signal();
                    }
                }
            }
        });

        mMediaPlayer.prepare();
        assertFalse(mMediaPlayer.isPlaying());

        selectSubtitleTrack(0);
        mOnTimedTextCalled.reset();

        mMediaPlayer.start();
        assertTrue(mMediaPlayer.isPlaying());

        // Waits until at least two subtitles are fired. Timeout is 2 sec.
        // Please refer the test srt files:
        // test_subtitle1_srt.3gp and test_subtitle2_srt.3gp
        assertTrue(mOnTimedTextCalled.waitForCountedSignals(2, 2000) >= 2);

        selectSubtitleTrack(1);
        mOnTimedTextCalled.reset();
        assertTrue(mOnTimedTextCalled.waitForCountedSignals(2, 2000) >= 2);

        selectSubtitleTrack(2);
        mOnTimedTextCalled.reset();
        assertTrue(mOnTimedTextCalled.waitForCountedSignals(2, 2000) >= 2);

        selectSubtitleTrack(3);
        mOnTimedTextCalled.reset();
        assertTrue(mOnTimedTextCalled.waitForCountedSignals(2, 2000) >= 2);
        mMediaPlayer.stop();
    }

    public void testGetTrackInfo() throws Exception {
        loadResource(R.raw.testvideo_with_2_subtitles);
        loadSubtitleSource(R.raw.test_subtitle1_srt);
        loadSubtitleSource(R.raw.test_subtitle2_srt);
        mMediaPlayer.prepare();
        mMediaPlayer.start();

        readTimedTextTracks();
        selectSubtitleTrack(2);

        int count = 0;
        MediaPlayer.TrackInfo[] trackInfos = mMediaPlayer.getTrackInfo();
        assertTrue(trackInfos != null && trackInfos.length != 0);
        for (int i = 0; i < trackInfos.length; ++i) {
            assertTrue(trackInfos[i] != null);
            if (trackInfos[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
                String trackLanguage = trackInfos[i].getLanguage();
                assertTrue(trackLanguage != null);
                trackLanguage.trim();
                Log.d(LOG_TAG, "track info lang: " + trackLanguage);
                assertTrue("Should not see empty track language with our test data.",
                           trackLanguage.length() > 0);
                count++;
            }
        }
        // There are 4 subtitle tracks in total in our test data.
        assertEquals(4, count);
    }

    public void testCallback() throws Throwable {
        final int mp4Duration = 8484;

        loadResource(R.raw.testvideo);
        mMediaPlayer.setDisplay(getActivity().getSurfaceHolder());
        mMediaPlayer.setScreenOnWhilePlaying(true);

        mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                mOnVideoSizeChangedCalled.signal();
            }
        });

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mOnPrepareCalled.signal();
            }
        });

        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                mOnSeekCompleteCalled.signal();
            }
        });

        mOnCompletionCalled.reset();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mOnCompletionCalled.signal();
            }
        });

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mOnErrorCalled.signal();
                return false;
            }
        });

        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                mOnInfoCalled.signal();
                return false;
            }
        });

        assertFalse(mOnPrepareCalled.isSignalled());
        assertFalse(mOnVideoSizeChangedCalled.isSignalled());
        mMediaPlayer.prepare();
        mOnPrepareCalled.waitForSignal();
        mOnVideoSizeChangedCalled.waitForSignal();
        mOnSeekCompleteCalled.reset();
        mMediaPlayer.seekTo(mp4Duration >> 1);
        mOnSeekCompleteCalled.waitForSignal();
        assertFalse(mOnCompletionCalled.isSignalled());
        mMediaPlayer.start();
        while(mMediaPlayer.isPlaying()) {
            Thread.sleep(SLEEP_TIME);
        }
        assertFalse(mMediaPlayer.isPlaying());
        mOnCompletionCalled.waitForSignal();
        assertFalse(mOnErrorCalled.isSignalled());
        mMediaPlayer.stop();
        mMediaPlayer.start();
        mOnErrorCalled.waitForSignal();
    }

    public void testRecordAndPlay() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        File outputFile = new File(Environment.getExternalStorageDirectory(),
                "record_and_play.3gp");
        String outputFileLocation = outputFile.getAbsolutePath();
        try {
            recordMedia(outputFileLocation);
            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(outputFileLocation);
                mp.prepareAsync();
                Thread.sleep(SLEEP_TIME);
                playAndStop(mp);
            } finally {
                mp.release();
            }

            Uri uri = Uri.parse(outputFileLocation);
            mp = new MediaPlayer();
            try {
                mp.setDataSource(mContext, uri);
                mp.prepareAsync();
                Thread.sleep(SLEEP_TIME);
                playAndStop(mp);
            } finally {
                mp.release();
            }

            try {
                mp = MediaPlayer.create(mContext, uri);
                playAndStop(mp);
            } finally {
                if (mp != null) {
                    mp.release();
                }
            }

            try {
                mp = MediaPlayer.create(mContext, uri, getActivity().getSurfaceHolder());
                playAndStop(mp);
            } finally {
                if (mp != null) {
                    mp.release();
                }
            }
        } finally {
            outputFile.delete();
        }
    }

    private void playAndStop(MediaPlayer mp) throws Exception {
        mp.start();
        Thread.sleep(SLEEP_TIME);
        mp.stop();
    }

    private void recordMedia(String outputFile) throws Exception {
        MediaRecorder mr = new MediaRecorder();
        try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mr.setOutputFile(outputFile);

            mr.prepare();
            mr.start();
            Thread.sleep(SLEEP_TIME);
            mr.stop();
        } finally {
            mr.release();
        }
    }

    private boolean hasMicrophone() {
        return getActivity().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }
}
