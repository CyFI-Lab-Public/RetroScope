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


import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.ConditionVariable;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.Surface;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.lang.InterruptedException;
import java.lang.Runnable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MediaRecorderTest extends ActivityInstrumentationTestCase2<MediaStubActivity> {
    private final String TAG = "MediaRecorderTest";
    private final String OUTPUT_PATH;
    private final String OUTPUT_PATH2;
    private static final float TOLERANCE = 0.0002f;
    private static final int RECORD_TIME_MS = 3000;
    private static final int RECORD_TIME_LAPSE_MS = 4000;
    private static final int RECORD_TIME_LONG_MS = 20000;
    private static final int RECORDED_DUR_TOLERANCE_MS = 1000;
    private static final int VIDEO_WIDTH = 176;
    private static final int VIDEO_HEIGHT = 144;
    private static final int VIDEO_BIT_RATE_IN_BPS = 128000;
    private static final double VIDEO_TIMELAPSE_CAPTURE_RATE_FPS = 1.0;
    private static final int AUDIO_BIT_RATE_IN_BPS = 12200;
    private static final int AUDIO_NUM_CHANNELS = 1;
    private static final int AUDIO_SAMPLE_RATE_HZ = 8000;
    private static final long MAX_FILE_SIZE = 5000;
    private static final int MAX_FILE_SIZE_TIMEOUT_MS = 5 * 60 * 1000;
    private static final int MAX_DURATION_MSEC = 2000;
    private static final float LATITUDE = 0.0000f;
    private static final float LONGITUDE  = -180.0f;
    private boolean mOnInfoCalled;
    private boolean mOnErrorCalled;
    private File mOutFile;
    private File mOutFile2;
    private Camera mCamera;
    private MediaStubActivity mActivity = null;

    private MediaRecorder mMediaRecorder;
    private ConditionVariable mMaxDurationCond;
    private ConditionVariable mMaxFileSizeCond;

    public MediaRecorderTest() {
        super("com.android.cts.media", MediaStubActivity.class);
        OUTPUT_PATH = new File(Environment.getExternalStorageDirectory(),
                "record.out").getAbsolutePath();
        OUTPUT_PATH2 = new File(Environment.getExternalStorageDirectory(),
                "record2.out").getAbsolutePath();
    }

    private void completeOnUiThread(final Runnable runnable) {
        final CountDownLatch latch = new CountDownLatch(1);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                latch.countDown();
            }
        });
        try {
            // if UI thread does not run, things will fail anyway
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } catch (java.lang.InterruptedException e) {
            fail("should not be interrupted");
        }
    }

    @Override
    protected void setUp() throws Exception {
        mActivity = getActivity();
        completeOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMediaRecorder = new MediaRecorder();
                mOutFile = new File(OUTPUT_PATH);
                mOutFile2 = new File(OUTPUT_PATH2);

                mMaxDurationCond = new ConditionVariable();
                mMaxFileSizeCond = new ConditionVariable();

                mMediaRecorder.setOutputFile(OUTPUT_PATH);
                mMediaRecorder.setOnInfoListener(new OnInfoListener() {
                    public void onInfo(MediaRecorder mr, int what, int extra) {
                        mOnInfoCalled = true;
                        if (what ==
                            MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                            Log.v(TAG, "max duration reached");
                            mMaxDurationCond.open();
                        } else if (what ==
                            MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                            Log.v(TAG, "max file size reached");
                            mMaxFileSizeCond.open();
                        }
                    }
                });
                mMediaRecorder.setOnErrorListener(new OnErrorListener() {
                    public void onError(MediaRecorder mr, int what, int extra) {
                        mOnErrorCalled = true;
                    }
                });
            }
        });
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        mMediaRecorder.release();
        mMediaRecorder = null;
        if (mOutFile != null && mOutFile.exists()) {
            mOutFile.delete();
        }
        if (mOutFile2 != null && mOutFile2.exists()) {
            mOutFile2.delete();
        }
        if (mCamera != null)  {
            mCamera.release();
            mCamera = null;
        }
        mMaxDurationCond.close();
        mMaxDurationCond = null;
        mMaxFileSizeCond.close();
        mMaxFileSizeCond = null;
        mActivity = null;
        super.tearDown();
    }

    public void testRecorderCamera() throws Exception {
        if (!hasCamera()) {
            return;
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT);
        mMediaRecorder.setVideoEncodingBitRate(VIDEO_BIT_RATE_IN_BPS);
        mMediaRecorder.setPreviewDisplay(mActivity.getSurfaceHolder().getSurface());
        mMediaRecorder.prepare();
        mMediaRecorder.start();
        Thread.sleep(RECORD_TIME_MS);
        mMediaRecorder.stop();
        checkOutputExist();
    }

    @UiThreadTest
    public void testSetCamera() throws Exception {
        recordVideoUsingCamera(false);
    }

    public void testRecorderTimelapsedVideo() throws Exception {
        recordVideoUsingCamera(true);
    }

    private void recordVideoUsingCamera(boolean timelapse) throws Exception {
        int nCamera = Camera.getNumberOfCameras();
        int durMs = timelapse? RECORD_TIME_LAPSE_MS: RECORD_TIME_MS;
        for (int cameraId = 0; cameraId < nCamera; cameraId++) {
            mCamera = Camera.open(cameraId);
            recordVideoUsingCamera(mCamera, OUTPUT_PATH, durMs, timelapse);
            mCamera.release();
            mCamera = null;
            assertTrue(checkLocationInFile(OUTPUT_PATH));
        }
    }

    private void recordVideoUsingCamera(
            Camera camera, String fileName, int durMs, boolean timelapse) throws Exception {
        // FIXME:
        // We should add some test case to use Camera.Parameters.getPreviewFpsRange()
        // to get the supported video frame rate range.
        Camera.Parameters params = camera.getParameters();
        int frameRate = params.getPreviewFrameRate();

        camera.unlock();
        mMediaRecorder.setCamera(camera);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mMediaRecorder.setVideoFrameRate(frameRate);
        mMediaRecorder.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT);
        mMediaRecorder.setPreviewDisplay(mActivity.getSurfaceHolder().getSurface());
        mMediaRecorder.setOutputFile(fileName);
        mMediaRecorder.setLocation(LATITUDE, LONGITUDE);
        final double captureRate = VIDEO_TIMELAPSE_CAPTURE_RATE_FPS;
        if (timelapse) {
            mMediaRecorder.setCaptureRate(captureRate);
        }

        mMediaRecorder.prepare();
        mMediaRecorder.start();
        Thread.sleep(durMs);
        mMediaRecorder.stop();
        assertTrue(mOutFile.exists());

        int targetDurMs = timelapse? ((int) (durMs * (captureRate / frameRate))): durMs;
        boolean hasVideo = true;
        boolean hasAudio = timelapse? false: true;
        checkTracksAndDuration(targetDurMs, hasVideo, hasAudio, fileName);
    }

    private void checkTracksAndDuration(
            int targetMs, boolean hasVideo, boolean hasAudio, String fileName) throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(fileName);
        String hasVideoStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
        String hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
        assertTrue(hasVideo? hasVideoStr != null : hasVideoStr == null);
        assertTrue(hasAudio? hasAudioStr != null : hasAudioStr == null);
        // FIXME:
        // If we could use fixed frame rate for video recording, we could also do more accurate
        // check on the duration.
        String durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        assertTrue(durStr != null);
        assertTrue(Integer.parseInt(durStr) > 0);
        retriever.release();
        retriever = null;
    }

    private boolean checkLocationInFile(String fileName) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(fileName);
        String location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
        if (location == null) {
            retriever.release();
            Log.v(TAG, "No location information found in file " + fileName);
            return false;
        }

        // parsing String location and recover the location inforamtion in floats
        // Make sure the tolerance is very small - due to rounding errors?.
        Log.v(TAG, "location: " + location);

        // Get the position of the -/+ sign in location String, which indicates
        // the beginning of the longtitude.
        int index = location.lastIndexOf('-');
        if (index == -1) {
            index = location.lastIndexOf('+');
        }
        assertTrue("+ or - is not found", index != -1);
        assertTrue("+ or - is only found at the beginning", index != 0);
        float latitude = Float.parseFloat(location.substring(0, index - 1));
        float longitude = Float.parseFloat(location.substring(index));
        assertTrue("Incorrect latitude: " + latitude, Math.abs(latitude - LATITUDE) <= TOLERANCE);
        assertTrue("Incorrect longitude: " + longitude, Math.abs(longitude - LONGITUDE) <= TOLERANCE);
        retriever.release();
        return true;
    }

    private void checkOutputExist() {
        assertTrue(mOutFile.exists());
        assertTrue(mOutFile.length() > 0);
        assertTrue(mOutFile.delete());
    }

    public void testRecorderVideo() throws Exception {
        if (!hasCamera()) {
            return;
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mMediaRecorder.setOutputFile(OUTPUT_PATH2);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setPreviewDisplay(mActivity.getSurfaceHolder().getSurface());
        mMediaRecorder.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT);

        FileOutputStream fos = new FileOutputStream(OUTPUT_PATH2);
        FileDescriptor fd = fos.getFD();
        mMediaRecorder.setOutputFile(fd);
        long maxFileSize = MAX_FILE_SIZE * 10;
        recordMedia(maxFileSize, mOutFile2);
        assertFalse(checkLocationInFile(OUTPUT_PATH2));
        fos.close();
    }

    public void testRecordingAudioInRawFormats() throws Exception {
        testRecordAudioInRawFormat(
                MediaRecorder.OutputFormat.AMR_NB,
                MediaRecorder.AudioEncoder.AMR_NB);

        testRecordAudioInRawFormat(
                MediaRecorder.OutputFormat.AMR_WB,
                MediaRecorder.AudioEncoder.AMR_WB);

        testRecordAudioInRawFormat(
                MediaRecorder.OutputFormat.AAC_ADTS,
                MediaRecorder.AudioEncoder.AAC);
    }

    private void testRecordAudioInRawFormat(
            int fileFormat, int codec) throws Exception {

        if (!hasMicrophone()) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(fileFormat);
        mMediaRecorder.setOutputFile(OUTPUT_PATH);
        mMediaRecorder.setAudioEncoder(codec);
        recordMedia(MAX_FILE_SIZE, mOutFile);
    }

    public void testGetAudioSourceMax() throws Exception {
        final int max = MediaRecorder.getAudioSourceMax();
        assertTrue(MediaRecorder.AudioSource.DEFAULT <= max);
        assertTrue(MediaRecorder.AudioSource.MIC <= max);
        assertTrue(MediaRecorder.AudioSource.CAMCORDER <= max);
        assertTrue(MediaRecorder.AudioSource.VOICE_CALL <= max);
        assertTrue(MediaRecorder.AudioSource.VOICE_COMMUNICATION <= max);
        assertTrue(MediaRecorder.AudioSource.VOICE_DOWNLINK <= max);
        assertTrue(MediaRecorder.AudioSource.VOICE_RECOGNITION <= max);
        assertTrue(MediaRecorder.AudioSource.VOICE_UPLINK <= max);
    }

    public void testRecorderAudio() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        assertEquals(0, mMediaRecorder.getMaxAmplitude());
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setOutputFile(OUTPUT_PATH);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setAudioChannels(AUDIO_NUM_CHANNELS);
        mMediaRecorder.setAudioSamplingRate(AUDIO_SAMPLE_RATE_HZ);
        mMediaRecorder.setAudioEncodingBitRate(AUDIO_BIT_RATE_IN_BPS);
        recordMedia(MAX_FILE_SIZE, mOutFile);
    }

    public void testOnInfoListener() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setMaxDuration(MAX_DURATION_MSEC);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.prepare();
        mMediaRecorder.start();
        Thread.sleep(RECORD_TIME_MS);
        assertTrue(mOnInfoCalled);
    }

    public void testSetMaxDuration() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        testSetMaxDuration(RECORD_TIME_LONG_MS, RECORDED_DUR_TOLERANCE_MS);
    }

    private void testSetMaxDuration(long durationMs, long toleranceMs) throws Exception {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setMaxDuration((int)durationMs);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.prepare();
        mMediaRecorder.start();
        long startTimeMs = System.currentTimeMillis();
        if (!mMaxDurationCond.block(durationMs + toleranceMs)) {
            fail("timed out waiting for MEDIA_RECORDER_INFO_MAX_DURATION_REACHED");
        }
        long endTimeMs = System.currentTimeMillis();
        long actualDurationMs = endTimeMs - startTimeMs;
        mMediaRecorder.stop();
        checkRecordedTime(durationMs, actualDurationMs, toleranceMs);
    }

    private void checkRecordedTime(long expectedMs, long actualMs, long tolerance) {
        assertEquals(expectedMs, actualMs, tolerance);
        long actualFileDurationMs = getRecordedFileDurationMs(OUTPUT_PATH);
        assertEquals(actualFileDurationMs, actualMs, tolerance);
    }

    private int getRecordedFileDurationMs(final String fileName) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(fileName);
        String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        assertNotNull(durationStr);
        return Integer.parseInt(durationStr);
    }

    public void testSetMaxFileSize() throws Exception {
        if (!hasMicrophone() || !hasCamera()) {
            return;
        }
        testSetMaxFileSize(512 * 1024, 50 * 1024);
    }

    private void testSetMaxFileSize(
            long fileSize, long tolerance) throws Exception {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT);
        mMediaRecorder.setVideoEncodingBitRate(256000);
        mMediaRecorder.setPreviewDisplay(mActivity.getSurfaceHolder().getSurface());
        mMediaRecorder.setMaxFileSize(fileSize);
        mMediaRecorder.prepare();
        mMediaRecorder.start();

        // Recording a scene with moving objects would greatly help reduce
        // the time for waiting.
        if (!mMaxFileSizeCond.block(MAX_FILE_SIZE_TIMEOUT_MS)) {
            fail("timed out waiting for MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED");
        }
        mMediaRecorder.stop();
        checkOutputFileSize(OUTPUT_PATH, fileSize, tolerance);
    }

    private void checkOutputFileSize(final String fileName, long fileSize, long tolerance) {
        assertTrue(mOutFile.exists());
        assertEquals(fileSize, mOutFile.length(), tolerance);
        assertTrue(mOutFile.delete());
    }

    public void testOnErrorListener() throws Exception {
        if (!hasMicrophone()) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        recordMedia(MAX_FILE_SIZE, mOutFile);
        // TODO: how can we trigger a recording error?
        assertFalse(mOnErrorCalled);
    }

    private void recordMedia(long maxFileSize, File outFile) throws Exception {
        mMediaRecorder.setMaxFileSize(maxFileSize);
        mMediaRecorder.prepare();
        mMediaRecorder.start();
        Thread.sleep(RECORD_TIME_MS);
        mMediaRecorder.stop();

        assertTrue(outFile.exists());

        // The max file size is always guaranteed.
        // We just make sure that the margin is not too big
        assertTrue(outFile.length() < 1.1 * maxFileSize);
        assertTrue(outFile.length() > 0);
    }

    private boolean hasCamera() {
        return mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private boolean hasMicrophone() {
        return mActivity.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE);
    }
}
