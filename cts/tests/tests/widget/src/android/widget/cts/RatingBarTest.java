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
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;

/**
 * Test {@link RatingBar}.
 */
public class RatingBarTest extends ActivityInstrumentationTestCase2<RatingBarStubActivity> {
    private Context mContext;
    private RatingBarStubActivity mActivity;

    public RatingBarTest() {
        super("com.android.cts.stub", RatingBarStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        assertNotNull(mActivity);
        mContext = getInstrumentation().getContext();
    }

    public void testConstructor() {
        new RatingBar(mContext, null, com.android.internal.R.attr.ratingBarStyle);

        new RatingBar(mContext, null);

        new RatingBar(mContext);

        RatingBar ratingBar = (RatingBar) mActivity.findViewById(R.id.ratingbar_constructor);
        assertNotNull(ratingBar);
        assertFalse(ratingBar.isIndicator());
        assertEquals(50, ratingBar.getNumStars());
        assertEquals(1.2f, ratingBar.getRating());
        assertEquals(0.2f, ratingBar.getStepSize());
    }

    @UiThreadTest
    public void testAccessOnRatingBarChangeListener() {
        RatingBar ratingBar = (RatingBar)mActivity.findViewById(R.id.ratingbar_constructor);

        MockOnRatingBarChangeListener listener = new MockOnRatingBarChangeListener();

        // set OnRatingBarChangeListener
        // normal value
        ratingBar.setOnRatingBarChangeListener(listener);
        assertSame(listener, ratingBar.getOnRatingBarChangeListener());
        ratingBar.setRating(2.2f);
        assertTrue(listener.hasCalledOnRatingChanged());

        // exceptional value
        listener.reset();
        ratingBar.setOnRatingBarChangeListener(null);
        assertNull(ratingBar.getOnRatingBarChangeListener());
        ratingBar.setRating(1.2f);
        assertFalse(listener.hasCalledOnRatingChanged());
    }

    public void testAccessIndicator() {
        RatingBar ratingBar = new RatingBar(mContext);

        ratingBar.setIsIndicator(true);
        assertTrue(ratingBar.isIndicator());

        ratingBar.setIsIndicator(false);
        assertFalse(ratingBar.isIndicator());
    }

    public void testAccessNumStars() {
        MockRatingBar mockRatingBar = new MockRatingBar(mContext);

        // set NumStars
        // normal value
        assertFalse(mockRatingBar.hasCalledRequestLayout());
        mockRatingBar.setNumStars(20);
        assertTrue(mockRatingBar.hasCalledRequestLayout());
        assertEquals(20, mockRatingBar.getNumStars());

        // exceptional value
        mockRatingBar.reset();
        mockRatingBar.setNumStars(-10);
        assertFalse(mockRatingBar.hasCalledRequestLayout());
        assertEquals(20, mockRatingBar.getNumStars());

        mockRatingBar.reset();
        mockRatingBar.setNumStars(Integer.MAX_VALUE);
        assertTrue(mockRatingBar.hasCalledRequestLayout());
        assertEquals(Integer.MAX_VALUE, mockRatingBar.getNumStars());
    }

    public void testAccessRating() {
        RatingBar ratingBar = new RatingBar(mContext);

        // set Rating
        // normal value
        ratingBar.setRating(2.0f);
        assertEquals(2.0f, ratingBar.getRating());

        // exceptional value
        ratingBar.setRating(-2.0f);
        assertEquals(0f, ratingBar.getRating());

        ratingBar.setRating(Float.MAX_VALUE);
        assertEquals((float) ratingBar.getNumStars(), ratingBar.getRating());
    }

    public void testSetMax() {
        RatingBar ratingBar = new RatingBar(mContext);

        // normal value
        ratingBar.setMax(10);
        assertEquals(10, ratingBar.getMax());

        ratingBar.setProgress(10);

        // exceptional values
        ratingBar.setMax(-10);
        assertEquals(10, ratingBar.getMax());
        assertEquals(10, ratingBar.getProgress());

        ratingBar.setMax(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, ratingBar.getMax());
    }

    public void testAccessStepSize() {
        RatingBar ratingBar = new RatingBar(mContext);

        // normal value
        ratingBar.setStepSize(1.5f);
        float expectedMax = ratingBar.getNumStars() / ratingBar.getStepSize();
        float expectedProgress = expectedMax / ratingBar.getMax() * ratingBar.getProgress();
        assertEquals((int) expectedMax, ratingBar.getMax());
        assertEquals((int) expectedProgress, ratingBar.getProgress());
        assertEquals((float) ratingBar.getNumStars() / (int) (ratingBar.getNumStars() / 1.5f),
                ratingBar.getStepSize());

        int currentMax = ratingBar.getMax();
        int currentProgress = ratingBar.getProgress();
        float currentStepSize = ratingBar.getStepSize();
        // exceptional value
        ratingBar.setStepSize(-1.5f);
        assertEquals(currentMax, ratingBar.getMax());
        assertEquals(currentProgress, ratingBar.getProgress());
        assertEquals(currentStepSize, ratingBar.getStepSize());

        ratingBar.setStepSize(0f);
        assertEquals(currentMax, ratingBar.getMax());
        assertEquals(currentProgress, ratingBar.getProgress());
        assertEquals(currentStepSize, ratingBar.getStepSize());

        ratingBar.setStepSize(ratingBar.getNumStars() + 0.1f);
        assertEquals(currentMax, ratingBar.getMax());
        assertEquals(currentProgress, ratingBar.getProgress());
        assertEquals(currentStepSize, ratingBar.getStepSize());

        ratingBar.setStepSize(Float.MAX_VALUE);
        assertEquals(currentMax, ratingBar.getMax());
        assertEquals(currentProgress, ratingBar.getProgress());
        assertEquals(currentStepSize, ratingBar.getStepSize());
    }

    /**
     * The listener interface for receiving OnRatingBarChangeListener events.
     * The class that is interested in processing a OnRatingBarChangeListener
     * event implements this interface, and the object created with that class
     * is registered with a component using the component's
     * <code>setOnRatingBarChangeListener<code> method. When
     * the OnRatingBarChangeListener event occurs, that object's appropriate
     * method is invoked.
     */
    private class MockOnRatingBarChangeListener implements OnRatingBarChangeListener {
        private boolean mCalledOnRatingChanged = false;

        boolean hasCalledOnRatingChanged() {
            return mCalledOnRatingChanged;
        }

        /*
         * (non-Javadoc)
         * @see android.widget.RadioGroup.OnRatingBarChangeListener#onRatingChanged
         * (RatingBar ratingBar, float rating, boolean fromTouch)
         */
        public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromTouch) {
            mCalledOnRatingChanged = true;
        }

        public void reset() {
            mCalledOnRatingChanged = false;
        }
    }

    /*
     * Mock class for ProgressBar to test protected methods
     */
    private class MockRatingBar extends RatingBar {
        public MockRatingBar(Context context) {
            super(context);
        }

        private boolean mCalledOnMeasure = false;
        private boolean mCalledRequestLayout = false;

        public boolean hasCalledOnMeasure() {
            return mCalledOnMeasure;
        }

        @Override
        protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            mCalledOnMeasure = true;
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        public void requestLayout() {
            mCalledRequestLayout = true;
            super.requestLayout();
        }

        public boolean hasCalledRequestLayout() {
            return mCalledRequestLayout;
        }

        public void reset() {
            mCalledOnMeasure = false;
            mCalledRequestLayout = false;
        }
    }
}
