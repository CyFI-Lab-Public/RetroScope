/*
 * Copyright (C) 2008 The Android Open Source Project
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


import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test {@link MediaController}.
 */
public class MediaControllerTest extends
        ActivityInstrumentationTestCase2<MediaControllerStubActivity> {
    private MediaController mMediaController;
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private static final long DEFAULT_TIMEOUT = 3000;

    public MediaControllerTest() {
        super("com.android.cts.stub", MediaControllerStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
    }

    public void testConstructor() {
        new MediaController(mActivity, null);

        new MediaController(mActivity, true);

        new MediaController(mActivity);

        final XmlPullParser parser =
                mActivity.getResources().getXml(R.layout.mediacontroller_layout);
        final AttributeSet attrs = Xml.asAttributeSet(parser);
        new MediaController(mActivity, attrs);
    }

    /**
     * scenario description:
     * 1. Show the MediaController.
     *
     */
    @UiThreadTest
    public void testMediaController() {
        mMediaController = new MediaController(mActivity);
        final MockMediaPlayerControl mediaPlayerControl = new MockMediaPlayerControl();
        mMediaController.setMediaPlayer(mediaPlayerControl);

        assertFalse(mMediaController.isShowing());
        mMediaController.show();
        // setAnchorView() must be called before show(),
        // otherwise MediaController never show.
        assertFalse(mMediaController.isShowing());

        View videoview = mActivity.findViewById(R.id.mediacontroller_videoview);
        mMediaController.setAnchorView(videoview);

        mMediaController.show();
        assertTrue(mMediaController.isShowing());

        // ideally test would trigger pause/play/ff/rew here and test response, but no way
        // to trigger those actions from MediaController

        mMediaController = new MediaController(mActivity, false);
        mMediaController.setMediaPlayer(mediaPlayerControl);
        videoview = mActivity.findViewById(R.id.mediacontroller_videoview);
        mMediaController.setAnchorView(videoview);

        mMediaController.show();
        assertTrue(mMediaController.isShowing());
    }

    public void testShow() {
        mMediaController = new MediaController(mActivity, true);
        assertFalse(mMediaController.isShowing());

        final MockMediaPlayerControl mediaPlayerControl = new MockMediaPlayerControl();
        mMediaController.setMediaPlayer(mediaPlayerControl);

        final VideoView videoView =
                (VideoView) mActivity.findViewById(R.id.mediacontroller_videoview);
        mMediaController.setAnchorView(videoView);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mMediaController.show();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(mMediaController.isShowing());

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mMediaController.hide();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertFalse(mMediaController.isShowing());

        final int timeout = 2000;
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mMediaController.show(timeout);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(mMediaController.isShowing());

        // isShowing() should return false, but MediaController still shows, this may be a bug.
        new PollingCheck(timeout + 500) {
            @Override
            protected boolean check() {
                return mMediaController.isShowing();
            }
        }.run();
    }

    private String prepareSampleVideo() {
        InputStream source = null;
        OutputStream target = null;
        final String VIDEO_NAME   = "testvideo.3gp";

        try {
            source = mActivity.getResources().openRawResource(R.raw.testvideo);
            target = mActivity.openFileOutput(VIDEO_NAME, Context.MODE_WORLD_READABLE);

            final byte[] buffer = new byte[1024];
            for (int len = source.read(buffer); len > 0; len = source.read(buffer)) {
                target.write(buffer, 0, len);
            }
        } catch (final IOException e) {
            fail(e.getMessage());
        } finally {
            try {
                if (source != null) {
                    source.close();
                }
                if (target != null) {
                    target.close();
                }
            } catch (final IOException _) {
                // Ignore the IOException.
            }
        }

        return mActivity.getFileStreamPath(VIDEO_NAME).getAbsolutePath();
    }

    public void testOnTrackballEvent() {
        mMediaController = new MediaController(mActivity);
        final MockMediaPlayerControl mediaPlayerControl = new MockMediaPlayerControl();
        mMediaController.setMediaPlayer(mediaPlayerControl);

        final VideoView videoView =
                (VideoView) mActivity.findViewById(R.id.mediacontroller_videoview);
        videoView.setMediaController(mMediaController);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                videoView.setVideoPath(prepareSampleVideo());
                videoView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        final long curTime = System.currentTimeMillis();
        // get the center of the VideoView.
        final int[] xy = new int[2];
        videoView.getLocationOnScreen(xy);

        final int viewWidth = videoView.getWidth();
        final int viewHeight = videoView.getHeight();

        final float x = xy[0] + viewWidth / 2.0f;
        final float y = xy[1] + viewHeight / 2.0f;
        final MotionEvent event = MotionEvent.obtain(curTime, 100,
                MotionEvent.ACTION_DOWN, x, y, 0);
        mInstrumentation.sendTrackballEventSync(event);
        mInstrumentation.waitForIdleSync();
    }

    @UiThreadTest
    public void testSetEnabled() {
        final View videoView = mActivity.findViewById(R.id.mediacontroller_videoview);
        final MockMediaPlayerControl mediaPlayerControl = new MockMediaPlayerControl();

        mMediaController = new MediaController(mActivity);
        mMediaController.setAnchorView(videoView);
        mMediaController.setMediaPlayer(mediaPlayerControl);

        final MockOnClickListener next = new MockOnClickListener();
        final MockOnClickListener prev = new MockOnClickListener();
        mMediaController.setPrevNextListeners(next, prev);

        mMediaController.show();

        mMediaController.setEnabled(true);
        assertTrue(mMediaController.isEnabled());

        mMediaController.setEnabled(false);
        assertFalse(mMediaController.isEnabled());
    }

    public void testSetPrevNextListeners() {
        final View videoView = mActivity.findViewById(R.id.mediacontroller_videoview);
        final MockMediaPlayerControl mediaPlayerControl = new MockMediaPlayerControl();

        mMediaController = new MediaController(mActivity);
        mMediaController.setAnchorView(videoView);
        mMediaController.setMediaPlayer(mediaPlayerControl);

        final MockOnClickListener next = new MockOnClickListener();
        final MockOnClickListener prev = new MockOnClickListener();
        mMediaController.setPrevNextListeners(next, prev);
    }

    private static class MockMediaPlayerControl implements MediaController.MediaPlayerControl {
        private boolean mIsPlayingCalled = false;
        private boolean mIsPlaying = false;
        private int mPosition = 0;

        public boolean hasIsPlayingCalled() {
            return mIsPlayingCalled;
        }

        public void start() {
            mIsPlaying = true;
        }

        public void pause() {
            mIsPlaying = false;
        }

        public int getDuration() {
            return 0;
        }

        public int getCurrentPosition() {
            return mPosition;
        }

        public void seekTo(int pos) {
            mPosition = pos;
        }

        public boolean isPlaying() {
            mIsPlayingCalled = true;
            return mIsPlaying;
        }

        public int getBufferPercentage() {
            return 0;
        }

        public boolean canPause() {
            return true;
        }

        public boolean canSeekBackward() {
            return true;
        }

        public boolean canSeekForward() {
            return true;
        }

        @Override
        public int getAudioSessionId() {
            return 0;
        }
    }

    private static class MockOnClickListener implements OnClickListener {
        private boolean mOnClickCalled = false;

        public boolean hasOnClickCalled() {
            return mOnClickCalled;
        }

        public void onClick(View v) {
            mOnClickCalled = true;
        }
    }
}
