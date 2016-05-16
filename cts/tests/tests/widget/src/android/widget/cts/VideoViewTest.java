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

package android.widget.cts;

import com.android.cts.stub.R;


import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View.MeasureSpec;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test {@link VideoView}.
 */
public class VideoViewTest extends ActivityInstrumentationTestCase2<VideoViewStubActivity> {
    /** The maximum time to wait for an operation. */
    private static final long   TIME_OUT = 15000L;
    /** The interval time to wait for completing an operation. */
    private static final long   OPERATION_INTERVAL  = 1500L;
    /** The duration of R.raw.testvideo. */
    private static final int    TEST_VIDEO_DURATION = 11047;
    /** The full name of R.raw.testvideo. */
    private static final String VIDEO_NAME   = "testvideo.3gp";
    /** delta for duration in case user uses different decoders on different
        hardware that report a duration that's different by a few milliseconds */
    private static final int DURATION_DELTA = 100;

    private VideoView mVideoView;
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private String mVideoPath;
    private MediaController mMediaController;

    private static class MockListener {
        private boolean mTriggered;

        MockListener() {
            mTriggered = false;
        }

        public boolean isTriggered() {
            return mTriggered;
        }

        protected void onEvent() {
            mTriggered = true;
        }
    }

    private static class MockOnPreparedListener extends MockListener
            implements OnPreparedListener {
        public void onPrepared(MediaPlayer mp) {
            super.onEvent();
        }
    }

    private static class MockOnErrorListener extends MockListener implements OnErrorListener {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            super.onEvent();
            return false;
        }
    }

    private static class MockOnCompletionListener extends MockListener
            implements OnCompletionListener {
        public void onCompletion(MediaPlayer mp) {
            super.onEvent();
        }
    }

    /**
     * Instantiates a new video view test.
     */
    public VideoViewTest() {
        super("com.android.cts.stub", VideoViewStubActivity.class);
    }

    /**
     * Find the video view specified by id.
     *
     * @param id the id
     * @return the video view
     */
    private VideoView findVideoViewById(int id) {
        return (VideoView) mActivity.findViewById(id);
    }

    private String prepareSampleVideo() throws IOException {
        InputStream source = null;
        OutputStream target = null;

        try {
            source = mActivity.getResources().openRawResource(R.raw.testvideo);
            target = mActivity.openFileOutput(VIDEO_NAME, Context.MODE_WORLD_READABLE);

            final byte[] buffer = new byte[1024];
            for (int len = source.read(buffer); len > 0; len = source.read(buffer)) {
                target.write(buffer, 0, len);
            }
        } finally {
            if (source != null) {
                source.close();
            }
            if (target != null) {
                target.close();
            }
        }

        return mActivity.getFileStreamPath(VIDEO_NAME).getAbsolutePath();
    }

    /**
     * Wait for an asynchronous media operation complete.
     * @throws InterruptedException
     */
    private void waitForOperationComplete() throws InterruptedException {
        Thread.sleep(OPERATION_INTERVAL);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        mVideoPath = prepareSampleVideo();
        assertNotNull(mVideoPath);
        mVideoView = findVideoViewById(R.id.videoview);
        mMediaController = new MediaController(mActivity);
        mVideoView.setMediaController(mMediaController);
    }

    public void testConstructor() {
        new VideoView(mActivity);

        new VideoView(mActivity, null);

        new VideoView(mActivity, null, 0);
    }

    public void testPlayVideo1() throws Throwable {
        final MockOnPreparedListener preparedListener = new MockOnPreparedListener();
        mVideoView.setOnPreparedListener(preparedListener);
        final MockOnCompletionListener completionListener = new MockOnCompletionListener();
        mVideoView.setOnCompletionListener(completionListener);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mVideoView.setVideoPath(mVideoPath);
            }
        });
        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return preparedListener.isTriggered();
            }
        }.run();
        assertFalse(completionListener.isTriggered());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mVideoView.start();
            }
        });
        // wait time is longer than duration in case system is sluggish
        new PollingCheck(mVideoView.getDuration() + TIME_OUT) {
            @Override
            protected boolean check() {
                return completionListener.isTriggered();
            }
        }.run();
    }

    public void testSetOnErrorListener() throws Throwable {
        final MockOnErrorListener listener = new MockOnErrorListener();
        mVideoView.setOnErrorListener(listener);

        runTestOnUiThread(new Runnable() {
            public void run() {
                String path = "unknown path";
                mVideoView.setVideoPath(path);
                mVideoView.start();
            }
        });
        mInstrumentation.waitForIdleSync();

        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return listener.isTriggered();
            }
        }.run();
    }

    public void testGetBufferPercentage() throws Throwable {
        final MockOnPreparedListener prepareListener = new MockOnPreparedListener();
        mVideoView.setOnPreparedListener(prepareListener);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mVideoView.setVideoPath(mVideoPath);
            }
        });
        mInstrumentation.waitForIdleSync();

        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return prepareListener.isTriggered();
            }
        }.run();
        int percent = mVideoView.getBufferPercentage();
        assertTrue(percent >= 0 && percent <= 100);
    }

    public void testResolveAdjustedSize() {
        mVideoView = new VideoView(mActivity);

        final int desiredSize = 100;
        int resolvedSize = mVideoView.resolveAdjustedSize(desiredSize, MeasureSpec.UNSPECIFIED);
        assertEquals(desiredSize, resolvedSize);

        final int specSize = MeasureSpec.getSize(MeasureSpec.AT_MOST);
        resolvedSize = mVideoView.resolveAdjustedSize(desiredSize, MeasureSpec.AT_MOST);
        assertEquals(Math.min(desiredSize, specSize), resolvedSize);

        resolvedSize = mVideoView.resolveAdjustedSize(desiredSize, MeasureSpec.EXACTLY);
        assertEquals(specSize, resolvedSize);
    }

    public void testGetDuration() throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                mVideoView.setVideoPath(mVideoPath);
            }
        });
        waitForOperationComplete();
        assertTrue(Math.abs(mVideoView.getDuration() - TEST_VIDEO_DURATION) < DURATION_DELTA);
    }

    public void testSetMediaController() {
        final MediaController ctlr = new MediaController(mActivity);
        mVideoView.setMediaController(ctlr);
    }
}
