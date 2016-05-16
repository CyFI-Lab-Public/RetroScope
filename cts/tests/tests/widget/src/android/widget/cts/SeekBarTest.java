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


import android.app.Activity;
import android.app.Instrumentation;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * Test {@link SeekBar}.
 */
public class SeekBarTest extends ActivityInstrumentationTestCase2<SeekBarStubActivity> {
    private SeekBar mSeekBar;

    private Activity mActivity;

    private Instrumentation mInstrumentation;

    public SeekBarTest() {
        super("com.android.cts.stub", SeekBarStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mActivity = getActivity();
        mSeekBar = (SeekBar) mActivity.findViewById(R.id.seekBar);
    }

    public void testConstructor() {
        new SeekBar(mActivity);

        new SeekBar(mActivity, null);

        new SeekBar(mActivity, null, com.android.internal.R.attr.seekBarStyle);
    }

    public void testSetOnSeekBarChangeListener() {
        MockOnSeekBarListener listener = new MockOnSeekBarListener();

        mSeekBar.setOnSeekBarChangeListener(listener);
        listener.reset();
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        int seekBarXY[] = new int[2];
        mSeekBar.getLocationInWindow(seekBarXY);
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN,
                seekBarXY[0], seekBarXY[1], 0);
        mInstrumentation.sendPointerSync(event);
        mInstrumentation.waitForIdleSync();
        assertTrue(listener.hasCalledOnStartTrackingTouch());
        // while starting to track, the progress is changed also
        assertTrue(listener.hasCalledOnProgressChanged());

        listener.reset();
        downTime = SystemClock.uptimeMillis();
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE,
                seekBarXY[0] + (mSeekBar.getWidth() >> 1), seekBarXY[1], 0);
        mInstrumentation.sendPointerSync(event);
        mInstrumentation.waitForIdleSync();
        assertTrue(listener.hasCalledOnProgressChanged());

        listener.reset();
        downTime = SystemClock.uptimeMillis();
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP,
                seekBarXY[0] + (mSeekBar.getWidth() >> 1), seekBarXY[1], 0);
        mInstrumentation.sendPointerSync(event);
        mInstrumentation.waitForIdleSync();
        assertTrue(listener.hasCalledOnStopTrackingTouch());

        mSeekBar.setOnSeekBarChangeListener(null);
    }

    private class MockOnSeekBarListener implements OnSeekBarChangeListener {
        private boolean mHasCalledOnProgressChanged;

        private boolean mHasCalledOnStartTrackingTouch;

        private boolean mHasCalledOnStopTrackingTouch;

        public boolean hasCalledOnProgressChanged() {
            return mHasCalledOnProgressChanged;
        }

        public boolean hasCalledOnStartTrackingTouch() {
            return mHasCalledOnStartTrackingTouch;
        }

        public boolean hasCalledOnStopTrackingTouch() {
            return mHasCalledOnStopTrackingTouch;
        }

        public void reset(){
            mHasCalledOnProgressChanged = false;
            mHasCalledOnStartTrackingTouch = false;
            mHasCalledOnStopTrackingTouch = false;
        }

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            mHasCalledOnProgressChanged = true;
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            mHasCalledOnStartTrackingTouch = true;
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            mHasCalledOnStopTrackingTouch = true;
        }
    }
}
