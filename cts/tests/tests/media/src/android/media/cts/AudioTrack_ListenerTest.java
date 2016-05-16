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


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.os.Handler;
import android.os.Message;
import android.test.AndroidTestCase;

public class AudioTrack_ListenerTest extends AndroidTestCase {
    private boolean mOnMarkerReachedCalled;
    private boolean mOnPeriodicNotificationCalled;
    private boolean mIsHandleMessageCalled;
    private final int TEST_SR = 11025;
    private final int TEST_CONF = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private final int TEST_FORMAT = AudioFormat.ENCODING_PCM_8BIT;
    private final int TEST_MODE = AudioTrack.MODE_STREAM;
    private final int TEST_STREAM_TYPE1 = AudioManager.STREAM_MUSIC;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mIsHandleMessageCalled = true;
            super.handleMessage(msg);
        }
    };
    private final int mMinBuffSize = AudioTrack.getMinBufferSize(TEST_SR, TEST_CONF, TEST_FORMAT);
    private AudioTrack mAudioTrack = new AudioTrack(TEST_STREAM_TYPE1, TEST_SR, TEST_CONF,
            TEST_FORMAT, 2 * mMinBuffSize, TEST_MODE);
    private OnPlaybackPositionUpdateListener mListener =
                                new MockOnPlaybackPositionUpdateListener();

    public void testAudioTrackCallback() throws Exception {
        mAudioTrack.setPlaybackPositionUpdateListener(mListener);
        doTest();
    }

    public void testAudioTrackCallbackWithHandler() throws Exception {
        mAudioTrack.setPlaybackPositionUpdateListener(mListener, mHandler);
        doTest();
        // ToBeFixed: Handler#handleMessage() is never called
        assertFalse(mIsHandleMessageCalled);
    }

    private void doTest() throws Exception {
        mOnMarkerReachedCalled = false;
        mOnPeriodicNotificationCalled = false;
        byte[] vai = AudioTrackTest.createSoundDataInByteArray(2 * mMinBuffSize, TEST_SR);
        int markerInFrames = vai.length / 4;
        assertEquals(AudioTrack.SUCCESS, mAudioTrack.setNotificationMarkerPosition(markerInFrames));
        int periodInFrames = vai.length / 2;
        assertEquals(AudioTrack.SUCCESS, mAudioTrack.setPositionNotificationPeriod(periodInFrames));

        boolean hasPlayed = false;
        int written = 0;
        while (written < vai.length) {
            written += mAudioTrack.write(vai, written, vai.length - written);
            if (!hasPlayed) {
                mAudioTrack.play();
                hasPlayed = true;
            }
        }

        final int numChannels = (TEST_CONF == AudioFormat.CHANNEL_CONFIGURATION_STEREO) ? 2 : 1;
        final int bytesPerSample = (TEST_FORMAT == AudioFormat.ENCODING_PCM_16BIT) ? 2 : 1;
        final int bytesPerFrame = numChannels * bytesPerSample;
        final int sampleLengthMs = (int)(1000 * ((float)vai.length / TEST_SR / bytesPerFrame));
        Thread.sleep(sampleLengthMs + 1000);
        assertTrue(mOnMarkerReachedCalled);
        assertTrue(mOnPeriodicNotificationCalled);
        mAudioTrack.stop();
    }

    private class MockOnPlaybackPositionUpdateListener
                                        implements OnPlaybackPositionUpdateListener {

        public void onMarkerReached(AudioTrack track) {
            mOnMarkerReachedCalled = true;
        }

        public void onPeriodicNotification(AudioTrack track) {
            mOnPeriodicNotificationCalled = true;
        }

    }

    @Override
    protected void tearDown() throws Exception {
        mAudioTrack.release();
        super.tearDown();
    }

}