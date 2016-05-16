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

package android.permission.cts;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Environment;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;

/**
 * Test that audio-related Permissions are enforced.
 */
public class AudioPermissionTest extends AndroidTestCase {

    static String PATH_PREFIX = Environment.getExternalStorageDirectory().toString();
    static String AUDIO_CAPTURE_PATH = PATH_PREFIX + "this-should-not-exist.amr";
    static int BEAUTY_SLEEP_INTERVAL = 5 * 1000;

    MediaPlayer mMediaPlayer = null;
    MediaRecorder mMediaRecorder = null;
    boolean mRecorded = false;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMediaRecorder = new MediaRecorder();
    }

    @LargeTest
    void testMicrophoneRecording() {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setOutputFile(AUDIO_CAPTURE_PATH);

        try {
            mMediaRecorder.prepare();
        }
        catch (SecurityException e) {
            // expected...?
            return;
        } catch (Exception e) {
            fail("Could not prepare MediaRecorder: " + e.toString());
        }

        try {
            mMediaRecorder.start();
        } catch (SecurityException e) {
            // expected
            return;
        }

        try {
            Thread.sleep(BEAUTY_SLEEP_INTERVAL);
        } catch (InterruptedException e) {
            // OK
        }

        try {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mRecorded = true;
            fail("Recorded from MediaRecorder.AudioSource.MIC");
        } catch (SecurityException e) {
            // expected
            mRecorded = false;
        }
    }

    void doRemoteMp3(Uri uri) {
        try {
            MediaPlayer plyr = new MediaPlayer();
            plyr.setDataSource(mContext, uri);
            plyr.setAudioStreamType(AudioManager.STREAM_MUSIC);
            plyr.prepare();
            plyr.seekTo(1000);    // Just to try.
            plyr.start();
            Thread.sleep(BEAUTY_SLEEP_INTERVAL / 10);
            plyr.stop();
            fail("We just downloaded a song off the Internet with no permissions, and uploaded arbitrary data in the query string");
            plyr.release();
        } catch (SecurityException e) {
            // expected
        } catch (Exception e) {
            fail("Got further than we should have trying to load a remote media source");
        }
    }

    @LargeTest
    void testRemoteMp3() {
        doRemoteMp3(Uri.parse("http://labs.isecpartners.com/chris/noodle.mp3?secret=1234"));
    }

}

