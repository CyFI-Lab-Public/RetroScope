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


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

public class ProgressBarTest extends InstrumentationTestCase {
    // The target context.
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
    }

    public void testConstructor() {
        new ProgressBar(mContext);

        new ProgressBar(mContext, null);

        new ProgressBar(mContext, null, android.R.attr.progressBarStyle);
    }

    public void testSetIndeterminate() {
        ProgressBar progressBar = new ProgressBar(mContext);
        assertTrue(progressBar.isIndeterminate());

        progressBar.setIndeterminate(true);
        assertTrue(progressBar.isIndeterminate());

        progressBar.setIndeterminate(false);
        // because default is Indeterminate only progressBar, can't change the status
        assertTrue(progressBar.isIndeterminate());

        progressBar = new ProgressBar(mContext, null, android.R.attr.progressBarStyleHorizontal);
        assertFalse(progressBar.isIndeterminate());

        progressBar.setIndeterminate(true);
        assertTrue(progressBar.isIndeterminate());

        progressBar.setIndeterminate(false);
        assertFalse(progressBar.isIndeterminate());
    }

    public void testAccessIndeterminateDrawable() {
        ProgressBar progressBar = new ProgressBar(mContext);

        // set IndeterminateDrawable
        // normal value
        MockDrawable mockDrawable = new MockDrawable();
        progressBar.setIndeterminateDrawable(mockDrawable);
        assertSame(mockDrawable, progressBar.getIndeterminateDrawable());
        assertFalse(mockDrawable.hasCalledDraw());
        progressBar.draw(new Canvas());
        assertTrue(mockDrawable.hasCalledDraw());

        // exceptional value
        progressBar.setIndeterminateDrawable(null);
        assertNull(progressBar.getIndeterminateDrawable());
    }

    public void testAccessProgressDrawable() {
        ProgressBar progressBar = new ProgressBar(mContext, null,
                android.R.attr.progressBarStyleHorizontal);

        // set ProgressDrawable
        // normal value
        MockDrawable mockDrawable = new MockDrawable();
        progressBar.setProgressDrawable(mockDrawable);
        assertSame(mockDrawable, progressBar.getProgressDrawable());
        assertFalse(mockDrawable.hasCalledDraw());
        progressBar.draw(new Canvas());
        assertTrue(mockDrawable.hasCalledDraw());

        // exceptional value
        progressBar.setProgressDrawable(null);
        assertNull(progressBar.getProgressDrawable());
    }

    public void testAccessProgress() {
        ProgressBar progressBar = new ProgressBar(mContext, null,
                android.R.attr.progressBarStyleHorizontal);
        assertEquals(0, progressBar.getProgress());

        final int maxProgress = progressBar.getMax();
        // set Progress
        // normal value
        progressBar.setProgress(maxProgress >> 1);
        assertEquals(maxProgress >> 1, progressBar.getProgress());

        // exceptional values
        progressBar.setProgress(-1);
        assertEquals(0, progressBar.getProgress());

        progressBar.setProgress(maxProgress + 1);
        assertEquals(maxProgress, progressBar.getProgress());

        progressBar.setProgress(Integer.MAX_VALUE);
        assertEquals(maxProgress, progressBar.getProgress());

        // when in indeterminate mode
        progressBar.setIndeterminate(true);
        progressBar.setProgress(maxProgress >> 1);
        assertEquals(0, progressBar.getProgress());
    }

    public void testAccessSecondaryProgress() {
        ProgressBar progressBar = new ProgressBar(mContext, null,
                android.R.attr.progressBarStyleHorizontal);
        assertEquals(0, progressBar.getSecondaryProgress());

        final int maxProgress = progressBar.getMax();
        // set SecondaryProgress
        // normal value
        progressBar.setSecondaryProgress(maxProgress >> 1);
        assertEquals(maxProgress >> 1, progressBar.getSecondaryProgress());

        // exceptional value
        progressBar.setSecondaryProgress(-1);
        assertEquals(0, progressBar.getSecondaryProgress());

        progressBar.setSecondaryProgress(maxProgress + 1);
        assertEquals(maxProgress, progressBar.getSecondaryProgress());

        progressBar.setSecondaryProgress(Integer.MAX_VALUE);
        assertEquals(maxProgress, progressBar.getSecondaryProgress());

        // when in indeterminate mode
        progressBar.setIndeterminate(true);
        progressBar.setSecondaryProgress(maxProgress >> 1);
        assertEquals(0, progressBar.getSecondaryProgress());
    }

    public void testIncrementProgressBy() {
        ProgressBar progressBar = new ProgressBar(mContext, null,
                android.R.attr.progressBarStyleHorizontal);

        // normal value
        int increment = 1;
        int oldProgress = progressBar.getProgress();
        progressBar.incrementProgressBy(increment);
        assertEquals(oldProgress + increment, progressBar.getProgress());

        increment = progressBar.getMax() >> 1;
        oldProgress = progressBar.getProgress();
        progressBar.incrementProgressBy(increment);
        assertEquals(oldProgress + increment, progressBar.getProgress());

        // exceptional values
        progressBar.setProgress(0);
        progressBar.incrementProgressBy(Integer.MAX_VALUE);
        assertEquals(progressBar.getMax(), progressBar.getProgress());

        progressBar.setProgress(0);
        progressBar.incrementProgressBy(Integer.MIN_VALUE);
        assertEquals(0, progressBar.getProgress());
    }

    public void testIncrementSecondaryProgressBy() {
        ProgressBar progressBar = new ProgressBar(mContext, null,
                android.R.attr.progressBarStyleHorizontal);

        // normal value
        int increment = 1;
        int oldSecondaryProgress = progressBar.getSecondaryProgress();
        progressBar.incrementSecondaryProgressBy(increment);
        assertEquals(oldSecondaryProgress + increment, progressBar.getSecondaryProgress());

        increment = progressBar.getMax() >> 1;
        oldSecondaryProgress = progressBar.getSecondaryProgress();
        progressBar.incrementSecondaryProgressBy(increment);
        assertEquals(oldSecondaryProgress + increment, progressBar.getSecondaryProgress());

        // exceptional values
        progressBar.setSecondaryProgress(0);
        progressBar.incrementSecondaryProgressBy(Integer.MAX_VALUE);
        assertEquals(progressBar.getMax(), progressBar.getSecondaryProgress());

        progressBar.setSecondaryProgress(0);
        progressBar.incrementSecondaryProgressBy(Integer.MIN_VALUE);
        assertEquals(0, progressBar.getSecondaryProgress());
    }

    public void testAccessInterpolator() {
        ProgressBar progressBar = new ProgressBar(mContext);

        // default should be LinearInterpolator
        assertTrue(progressBar.getInterpolator() instanceof LinearInterpolator);

        // normal value
        Interpolator i = new AccelerateDecelerateInterpolator();
        progressBar.setInterpolator(i);
        assertEquals(i, progressBar.getInterpolator());
    }

    public void testSetVisibility() {
        ProgressBar progressBar = new ProgressBar(mContext, null,
                android.R.attr.progressBarStyleHorizontal);

        // set visibility
        // normal value
        int visibility = View.VISIBLE;
        progressBar.setVisibility(visibility);
        assertEquals(visibility, progressBar.getVisibility());

        visibility = View.GONE;
        progressBar.setVisibility(visibility);
        assertEquals(visibility, progressBar.getVisibility());

        // exceptional value
        visibility = 0xfffffff5; // -11
        int mask = 0x0000000C; // View.VISIBILITY_MASK
        int expected = (progressBar.getVisibility() & ~mask) | (visibility & mask);
        progressBar.setVisibility(visibility);
        assertEquals(expected, progressBar.getVisibility());

        visibility = 0x7fffffff; // Integer.MAX_VALUE;
        expected = (progressBar.getVisibility() & ~mask) | (visibility & mask);
        progressBar.setVisibility(Integer.MAX_VALUE);
        assertEquals(expected, progressBar.getVisibility());
    }

    public void testInvalidateDrawable() {
        MockProgressBar mockProgressBar = new MockProgressBar(mContext);

        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        mockProgressBar.setBackgroundDrawable(mockDrawable1);

        mockProgressBar.invalidateDrawable(mockDrawable1);
        assertTrue(mockProgressBar.hasCalledInvalidate());

        mockProgressBar.reset();
        mockProgressBar.invalidateDrawable(mockDrawable2);
        assertFalse(mockProgressBar.hasCalledInvalidate());

        try {
            mockProgressBar.invalidateDrawable(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // issue 1695243, not clear what is supposed to happen if drawable is null.
        }

        mockProgressBar.setIndeterminateDrawable(mockDrawable1);
        mockProgressBar.setProgressDrawable(mockDrawable2);

        mockProgressBar.invalidateDrawable(null);
        assertFalse(mockProgressBar.hasCalledInvalidate());
    }

    public void testPostInvalidate() {
        MockProgressBar mockProgressBar = new MockProgressBar(mContext);
        mockProgressBar.postInvalidate();
    }

    public void testAccessMax() {
        ProgressBar progressBar = new ProgressBar(mContext, null,
                android.R.attr.progressBarStyleHorizontal);

        // set Progress
        int progress = 10;
        progressBar.setProgress(progress);

        // normal value
        int max = progress + 1;
        progressBar.setMax(max);
        assertEquals(max, progressBar.getMax());
        assertEquals(progress, progressBar.getProgress());

        max = progress - 1;
        progressBar.setMax(max);
        assertEquals(max, progressBar.getMax());
        assertEquals(max, progressBar.getProgress());

        // exceptional values
        progressBar.setMax(-1);
        assertEquals(0, progressBar.getMax());
        assertEquals(0, progressBar.getProgress());

        progressBar.setMax(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, progressBar.getMax());
        assertEquals(0, progressBar.getProgress());
    }

    public void testOnDraw() {
        // Do not test, it's controlled by View. Implementation details
    }

    private class MockDrawable extends Drawable {
        private boolean mCalledDraw = false;

        @Override
        public void draw(Canvas canvas) {
            mCalledDraw = true;
        }

        @Override
        public int getOpacity() {
            return 0;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        public boolean hasCalledDraw() {
            return mCalledDraw;
        }

        public void reset() {
            mCalledDraw = false;
        }

    }

    public void testOnMeasure() {
        // onMeasure() is implementation details, do NOT test
    }

    public void testOnSizeChange() {
        // onSizeChanged() is implementation details, do NOT test
    }

    public void testVerifyDrawable() {
        MockProgressBar mockProgressBar = new MockProgressBar(mContext);
        assertTrue(mockProgressBar.verifyDrawable(null));

        Drawable d1 = mContext.getResources().getDrawable(R.drawable.blue);
        Drawable d2 = mContext.getResources().getDrawable(R.drawable.red);
        Drawable d3 = mContext.getResources().getDrawable(R.drawable.yellow);

        mockProgressBar.setBackgroundDrawable(d1);
        assertTrue(mockProgressBar.verifyDrawable(null));
        assertTrue(mockProgressBar.verifyDrawable(d1));
        assertFalse(mockProgressBar.verifyDrawable(d2));
        assertFalse(mockProgressBar.verifyDrawable(d3));

        mockProgressBar.setIndeterminateDrawable(d2);
        assertTrue(mockProgressBar.verifyDrawable(null));
        assertTrue(mockProgressBar.verifyDrawable(d1));
        assertTrue(mockProgressBar.verifyDrawable(d2));
        assertFalse(mockProgressBar.verifyDrawable(d3));

        mockProgressBar.setProgressDrawable(d3);
        assertFalse(mockProgressBar.verifyDrawable(null));
        assertTrue(mockProgressBar.verifyDrawable(d1));
        assertTrue(mockProgressBar.verifyDrawable(d2));
        assertTrue(mockProgressBar.verifyDrawable(d3));
    }

    public void testDrawableStateChanged() {
        // drawableStateChanged() is implementation details, do NOT test
    }

    public void testOnSaveAndRestoreInstanceState() {
        ProgressBar progressBar = new ProgressBar(mContext, null,
                android.R.attr.progressBarStyleHorizontal);
        int oldProgress = 1;
        int oldSecondaryProgress = progressBar.getMax() - 1;
        progressBar.setProgress(oldProgress);
        progressBar.setSecondaryProgress(oldSecondaryProgress);
        assertEquals(oldProgress, progressBar.getProgress());
        assertEquals(oldSecondaryProgress, progressBar.getSecondaryProgress());

        Parcelable state = progressBar.onSaveInstanceState();

        int newProgress = 2;
        int newSecondaryProgress = progressBar.getMax() - 2;
        progressBar.setProgress(newProgress);
        progressBar.setSecondaryProgress(newSecondaryProgress);
        assertEquals(newProgress, progressBar.getProgress());
        assertEquals(newSecondaryProgress, progressBar.getSecondaryProgress());

        progressBar.onRestoreInstanceState(state);
        assertEquals(oldProgress, progressBar.getProgress());
        assertEquals(oldSecondaryProgress, progressBar.getSecondaryProgress());
    }

    /*
     * Mock class for ProgressBar to test protected methods
     */
    private class MockProgressBar extends ProgressBar {
        private boolean mCalledInvalidate = false;

        /**
         * @param context
         */
        public MockProgressBar(Context context) {
            super(context);
        }

        @Override
        protected boolean verifyDrawable(Drawable who) {
            return super.verifyDrawable(who);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }

        @Override
        protected synchronized void onMeasure(int widthMeasureSpec,
                int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected synchronized void onDraw(Canvas canvas) {
            super.onDraw(canvas);
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
        }

        public void invalidate(int l, int t, int r, int b) {
            mCalledInvalidate = true;
            super.invalidate(l, t, r, b);
        }

        public void invalidate() {
            mCalledInvalidate = true;
            super.invalidate();
        }

        public boolean hasCalledInvalidate() {
            return mCalledInvalidate;
        }

        public void reset() {
            mCalledInvalidate = false;
        }
    }
}
