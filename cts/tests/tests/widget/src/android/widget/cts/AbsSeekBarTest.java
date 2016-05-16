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
import android.content.Context;
import android.content.res.Resources;
import android.cts.util.PollingCheck;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.AbsSeekBar;
import android.widget.SeekBar;

/**
 * Test {@link AbsSeekBar}.
 */
public class AbsSeekBarTest extends ActivityInstrumentationTestCase2<ProgressBarStubActivity> {
    public AbsSeekBarTest() {
        super("com.android.cts.stub", ProgressBarStubActivity.class);
    }

    private Activity mActivity;
    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mResources = mActivity.getResources();
    }

    public void testConstructor() {
        new MyAbsSeekBar(mActivity);

        new MyAbsSeekBar(mActivity, null);

        new MyAbsSeekBar(mActivity, null, com.android.internal.R.attr.progressBarStyle);
    }

    public void testAccessThumbOffset() {
        AbsSeekBar myAbsSeekBar = new MyAbsSeekBar(mActivity);
        final int positive = 5;
        final int negative = -5;
        final int zero = 0;

        myAbsSeekBar.setThumbOffset(positive);
        assertEquals(positive, myAbsSeekBar.getThumbOffset());

        myAbsSeekBar.setThumbOffset(zero);
        assertEquals(zero, myAbsSeekBar.getThumbOffset());

        myAbsSeekBar.setThumbOffset(negative);
        assertEquals(negative, myAbsSeekBar.getThumbOffset());
    }

    public void testSetThumb() {
        MyAbsSeekBar myAbsSeekBar = new MyAbsSeekBar(mActivity);
        Drawable drawable1 = mResources.getDrawable(R.drawable.scenery);
        Drawable drawable2 = mResources.getDrawable(R.drawable.pass);

        assertFalse(myAbsSeekBar.verifyDrawable(drawable1));
        assertFalse(myAbsSeekBar.verifyDrawable(drawable2));

        myAbsSeekBar.setThumb(drawable1);
        assertTrue(myAbsSeekBar.verifyDrawable(drawable1));
        assertFalse(myAbsSeekBar.verifyDrawable(drawable2));

        myAbsSeekBar.setThumb(drawable2);
        assertFalse(myAbsSeekBar.verifyDrawable(drawable1));
        assertTrue(myAbsSeekBar.verifyDrawable(drawable2));
    }

    public void testDrawableStateChanged() {
        MyAbsSeekBar myAbsSeekBar = new MyAbsSeekBar(mActivity);
        MockDrawable drawable = new MockDrawable();
        myAbsSeekBar.setProgressDrawable(drawable);

        myAbsSeekBar.setEnabled(false);
        myAbsSeekBar.drawableStateChanged();
        assertEquals(0, drawable.getAlpha());

        myAbsSeekBar.setEnabled(true);
        myAbsSeekBar.drawableStateChanged();
        assertEquals(0xFF, drawable.getAlpha());
    }

    public void testVerifyDrawable() {
        MyAbsSeekBar myAbsSeekBar = new MyAbsSeekBar(mActivity);
        Drawable drawable1 = mResources.getDrawable(R.drawable.scenery);
        Drawable drawable2 = mResources.getDrawable(R.drawable.pass);
        Drawable drawable3 = mResources.getDrawable(R.drawable.blue);
        Drawable drawable4 = mResources.getDrawable(R.drawable.black);

        assertFalse(myAbsSeekBar.verifyDrawable(drawable1));
        assertFalse(myAbsSeekBar.verifyDrawable(drawable2));
        assertFalse(myAbsSeekBar.verifyDrawable(drawable3));
        assertFalse(myAbsSeekBar.verifyDrawable(drawable4));
        assertTrue(myAbsSeekBar.verifyDrawable(null));

        myAbsSeekBar.setThumb(drawable1);
        assertTrue(myAbsSeekBar.verifyDrawable(drawable1));
        assertFalse(myAbsSeekBar.verifyDrawable(drawable2));
        assertFalse(myAbsSeekBar.verifyDrawable(drawable3));
        assertFalse(myAbsSeekBar.verifyDrawable(drawable4));
        assertTrue(myAbsSeekBar.verifyDrawable(null));

        myAbsSeekBar.setThumb(drawable2);
        assertFalse(myAbsSeekBar.verifyDrawable(drawable1));
        assertTrue(myAbsSeekBar.verifyDrawable(drawable2));
        assertFalse(myAbsSeekBar.verifyDrawable(drawable3));
        assertFalse(myAbsSeekBar.verifyDrawable(drawable4));
        assertTrue(myAbsSeekBar.verifyDrawable(null));

        myAbsSeekBar.setBackgroundDrawable(drawable2);
        myAbsSeekBar.setProgressDrawable(drawable3);
        myAbsSeekBar.setIndeterminateDrawable(drawable4);
        assertFalse(myAbsSeekBar.verifyDrawable(drawable1));
        assertTrue(myAbsSeekBar.verifyDrawable(drawable2));
        assertTrue(myAbsSeekBar.verifyDrawable(drawable3));
        assertTrue(myAbsSeekBar.verifyDrawable(drawable4));
        assertFalse(myAbsSeekBar.verifyDrawable(null));
    }

    public void testAccessKeyProgressIncrement() throws Throwable {
        // AbsSeekBar is an abstract class, use its subclass: SeekBar to do this test.
        runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setContentView(R.layout.seekbar);
            }
        });
        getInstrumentation().waitForIdleSync();

        final SeekBar seekBar = (SeekBar) mActivity.findViewById(R.id.seekBar);
        final int keyProgressIncrement = 2;
        runTestOnUiThread(new Runnable() {
            public void run() {
                seekBar.setKeyProgressIncrement(keyProgressIncrement);
                seekBar.setFocusable(true);
                seekBar.requestFocus();
            }
        });
        new PollingCheck(1000) {
            @Override
            protected boolean check() {
                return seekBar.hasWindowFocus();
            }
        }.run();
        assertEquals(keyProgressIncrement, seekBar.getKeyProgressIncrement());

        int oldProgress = seekBar.getProgress();
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
        getInstrumentation().sendKeySync(keyEvent);
        assertEquals(oldProgress + keyProgressIncrement, seekBar.getProgress());
        oldProgress = seekBar.getProgress();
        keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);
        getInstrumentation().sendKeySync(keyEvent);
        assertEquals(oldProgress - keyProgressIncrement, seekBar.getProgress());
    }

    public void testSetMax() {
        MyAbsSeekBar myAbsSeekBar = new MyAbsSeekBar(mActivity, null, R.style.TestProgressBar);

        int progress = 10;
        myAbsSeekBar.setProgress(progress);
        int max = progress + 1;
        myAbsSeekBar.setMax(max);
        assertEquals(max, myAbsSeekBar.getMax());
        assertEquals(progress, myAbsSeekBar.getProgress());
        assertEquals(1, myAbsSeekBar.getKeyProgressIncrement());

        max = progress - 1;
        myAbsSeekBar.setMax(max);
        assertEquals(max, myAbsSeekBar.getMax());
        assertEquals(max, myAbsSeekBar.getProgress());
        assertEquals(1, myAbsSeekBar.getKeyProgressIncrement());

        int keyProgressIncrement = 10;
        myAbsSeekBar.setKeyProgressIncrement(keyProgressIncrement);
        assertEquals(keyProgressIncrement, myAbsSeekBar.getKeyProgressIncrement());
        max = (keyProgressIncrement - 1) * 20;
        myAbsSeekBar.setMax(max);
        assertEquals(keyProgressIncrement, myAbsSeekBar.getKeyProgressIncrement());
        max = (keyProgressIncrement + 1) * 20;
        myAbsSeekBar.setMax(max);
        assertEquals(keyProgressIncrement + 1, myAbsSeekBar.getKeyProgressIncrement());
    }

    public void testFoo() {
        // Do not test these APIs. They are callbacks which:
        // 1. The callback machanism has been tested in super class
        // 2. The functionality is implmentation details, no need to test
    }

    private static class MyAbsSeekBar extends AbsSeekBar {
        public MyAbsSeekBar(Context context) {
            super(context);
        }

        public MyAbsSeekBar(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyAbsSeekBar(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
        }

        @Override
        protected boolean verifyDrawable(Drawable who) {
            return super.verifyDrawable(who);
        }
    }

    private static class MockDrawable extends Drawable {
        private int mAlpha;
        private boolean mCalledDraw = false;

        @Override
        public void draw(Canvas canvas) {
            mCalledDraw = true;
        }

        public boolean hasCalledDraw() {
            return mCalledDraw;
        }

        public void reset() {
            mCalledDraw = false;
        }

        @Override
        public int getOpacity() {
            return 0;
        }

        @Override
        public void setAlpha(int alpha) {
            mAlpha = alpha;
        }

        public int getAlpha() {
            return mAlpha;
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }
    }
}
