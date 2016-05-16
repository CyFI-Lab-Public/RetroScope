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

package android.mediastress.cts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.media.CamcorderProfile;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.VideoEncoder;
import android.os.Environment;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import android.test.InstrumentationTestCase;

/**
 * Helper for implementing video playback stress test
 */
abstract class MediaPlayerStressTest extends InstrumentationTestCase {
    protected static final String VIDEO_TOP_DIR = WorkDir.getMediaDirString();
    protected static final int REPEAT_NUMBER_FOR_SHORT_CLIPS = 2;
    protected static final int REPEAT_NUMBER_FOR_LONG_CLIPS = 1;
    protected static final int REPEAT_NUMBER_FOR_REPEATED_PLAYBACK = 20;
    private static final String TAG = "MediaPlayerStressTest";
    // whether a video format is supported or not.
    private final boolean mSupported;

    /**
     * construct a test case with check of whether the format is supported or not.
     * @param quality
     * @param videoCodec
     * @param audioCodec
     */
    protected MediaPlayerStressTest(int quality, int videoCodec, int audioCodec) {
        mSupported = VideoPlayerCapability.formatSupported(quality, videoCodec, audioCodec);
    }

    protected MediaPlayerStressTest() {
        mSupported = true; // supported if nothing specified
    }

    /**
     * provides full path name of video clip for the given media number
     * @param mediaNumber
     * @return video file name
     */
    abstract protected String getFullVideoClipName(int mediaNumber);

    private int mTotalPlaybackError;
    private int mTotalComplete;
    private int mTotalInfoUnknown;
    private int mTotalVideoTrackLagging;
    private int mTotalBadInterleaving;
    private int mTotalNotSeekable;
    private int mTotalMetaDataUpdate;

    private void writeTestOutput(String filename, Writer output) throws Exception{
        output.write("File Name: " + filename);
        output.write(" Complete: " + CodecTest.mOnCompleteSuccess);
        output.write(" Error: " + CodecTest.mPlaybackError);
        output.write(" Unknown Info: " + CodecTest.mMediaInfoUnknownCount);
        output.write(" Track Lagging: " +  CodecTest.mMediaInfoVideoTrackLaggingCount);
        output.write(" Bad Interleaving: " + CodecTest.mMediaInfoBadInterleavingCount);
        output.write(" Not Seekable: " + CodecTest.mMediaInfoNotSeekableCount);
        output.write(" Info Meta data update: " + CodecTest.mMediaInfoMetdataUpdateCount);
        output.write("\n");
    }

    private void writeTestSummary(Writer output) throws Exception{
        output.write("Total Result:\n");
        output.write("Total Complete: " + mTotalComplete + "\n");
        output.write("Total Error: " + mTotalPlaybackError + "\n");
        output.write("Total Unknown Info: " + mTotalInfoUnknown + "\n");
        output.write("Total Track Lagging: " + mTotalVideoTrackLagging + "\n" );
        output.write("Total Bad Interleaving: " + mTotalBadInterleaving + "\n");
        output.write("Total Not Seekable: " + mTotalNotSeekable + "\n");
        output.write("Total Info Meta data update: " + mTotalMetaDataUpdate + "\n");
        output.write("\n");
    }

    private void updateTestResult(){
        if (CodecTest.mOnCompleteSuccess){
            mTotalComplete++;
        }
        else if (CodecTest.mPlaybackError){
            mTotalPlaybackError++;
        }
        mTotalInfoUnknown += CodecTest.mMediaInfoUnknownCount;
        mTotalVideoTrackLagging += CodecTest.mMediaInfoVideoTrackLaggingCount;
        mTotalBadInterleaving += CodecTest.mMediaInfoBadInterleavingCount;
        mTotalNotSeekable += CodecTest.mMediaInfoNotSeekableCount;
        mTotalMetaDataUpdate += CodecTest.mMediaInfoMetdataUpdateCount;
    }

    /**
     * runs video playback test for the given mediaNumber
     * @param mediaNumber number of media to be used for playback.
     *         This number is passed to getFullVideoClipName.
     * @param  repeatCounter repeat playback for the given number
     * @throws Exception
     */
    protected void doTestVideoPlayback(int mediaNumber, int repeatCounter) throws Exception {
        if (!mSupported) {
            return;
        }

        File playbackOutput = new File(WorkDir.getTopDir(), "PlaybackTestResult.txt");
        Writer output = new BufferedWriter(new FileWriter(playbackOutput, true));

        boolean testResult = true;
        boolean onCompleteSuccess = false;

        Instrumentation inst = getInstrumentation();
        Intent intent = new Intent();

        intent.setClass(inst.getTargetContext(), MediaFrameworkTest.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Activity act = inst.startActivitySync(intent);

        String mediaName = getFullVideoClipName(mediaNumber);
        for (int i = 0; i < repeatCounter; i++) {
            Log.v(TAG, "start playing " + mediaName);
            onCompleteSuccess =
                CodecTest.playMediaSample(mediaName);
            if (!onCompleteSuccess) {
                //Don't fail the test right away, print out the failure file.
                Log.v(TAG, "Failure File : " + mediaName);
                testResult = false;
            }
        }
        Thread.sleep(1000);

        act.finish();
        //Write test result to an output file
        writeTestOutput(mediaName, output);
        //Get the summary
        updateTestResult();

        writeTestSummary(output);
        output.close();
        assertTrue("playback " + mediaName, testResult);
    }

    protected void doTestVideoPlaybackShort(int mediaNumber) throws Exception {
        doTestVideoPlayback(mediaNumber, REPEAT_NUMBER_FOR_SHORT_CLIPS);
    }

    protected void doTestVideoPlaybackLong(int mediaNumber) throws Exception {
        doTestVideoPlayback(mediaNumber, REPEAT_NUMBER_FOR_LONG_CLIPS);
    }

    protected void doTestVideoPlaybackRepeated(int mediaNumber) throws Exception {
        doTestVideoPlayback(mediaNumber, REPEAT_NUMBER_FOR_REPEATED_PLAYBACK);
    }
}
