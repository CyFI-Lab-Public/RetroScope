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


import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Parcelable;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

/**
 * Test {@link TimePicker}.
 */
public class TimePickerTest extends ActivityInstrumentationTestCase2<StubActivity> {
    private TimePicker mTimePicker;

    private Activity mActivity;

    private Context mContext;

    private Instrumentation mInstrumentation;

    public TimePickerTest() {
        super("com.android.cts.stub", StubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
        mActivity = getActivity();
    }

    public void testConstructors() {
        AttributeSet attrs =
            mContext.getResources().getLayout(com.android.cts.stub.R.layout.timepicker);
        assertNotNull(attrs);

        new TimePicker(mContext);
        try {
            new TimePicker(null);
            fail("did not throw NullPointerException when param context is null.");
        } catch (NullPointerException e) {
            // expected
        }

        new TimePicker(mContext, attrs);
        try {
            new TimePicker(null, attrs);
            fail("did not throw NullPointerException when param context is null.");
        } catch (NullPointerException e) {
            // expected
        }
        new TimePicker(mContext, null);

        new TimePicker(mContext, attrs, 0);
        try {
            new TimePicker(null, attrs, 0);
            fail("did not throw NullPointerException when param context is null.");
        } catch (NullPointerException e) {
            // expected
        }
        new TimePicker(mContext, null, 0);
        new TimePicker(mContext, attrs, 0);
        new TimePicker(mContext, attrs, Integer.MIN_VALUE);
    }

    public void testSetEnabled() {
        mTimePicker = new TimePicker(mContext);
        assertTrue(mTimePicker.isEnabled());

        mTimePicker.setEnabled(false);
        assertFalse(mTimePicker.isEnabled());

        mTimePicker.setEnabled(true);
        assertTrue(mTimePicker.isEnabled());
    }

    public void testSetOnTimeChangedListener() {
        int initialHour = 13;
        int initialMinute = 50;
        mTimePicker = new TimePicker(mContext);

        MockOnTimeChangeListener listener = new MockOnTimeChangeListener();
        mTimePicker.setOnTimeChangedListener(listener);
        mTimePicker.setCurrentHour(Integer.valueOf(initialHour));
        mTimePicker.setCurrentMinute(Integer.valueOf(initialMinute));
        assertEquals(initialHour, listener.getNotifiedHourOfDay());
        assertEquals(initialMinute, listener.getNotifiedMinute());

        // set the same hour as current
        listener.reset();
        mTimePicker.setCurrentHour(Integer.valueOf(initialHour));
        assertFalse(listener.hasCalledOnTimeChanged());

        mTimePicker.setCurrentHour(Integer.valueOf(initialHour + 1));
        assertTrue(listener.hasCalledOnTimeChanged());
        assertEquals(initialHour + 1, listener.getNotifiedHourOfDay());
        assertEquals(initialMinute, listener.getNotifiedMinute());
        assertSame(mTimePicker, listener.getNotifiedView());

        // set the same minute as current
        listener.reset();
        mTimePicker.setCurrentMinute(initialMinute);
        assertFalse(listener.hasCalledOnTimeChanged());

        listener.reset();
        mTimePicker.setCurrentMinute(initialMinute + 1);
        assertTrue(listener.hasCalledOnTimeChanged());
        assertEquals(initialHour + 1, listener.getNotifiedHourOfDay());
        assertEquals(initialMinute + 1, listener.getNotifiedMinute());
        assertSame(mTimePicker, listener.getNotifiedView());

        // change time picker mode
        listener.reset();
        mTimePicker.setIs24HourView( !mTimePicker.is24HourView() );
        assertFalse(listener.hasCalledOnTimeChanged());
    }

    public void testAccessCurrentHour() {
        mTimePicker = new TimePicker(mContext);

        // AM/PM mode
        mTimePicker.setIs24HourView(false);

        mTimePicker.setCurrentHour(0);
        assertEquals(Integer.valueOf(0), mTimePicker.getCurrentHour());

        mTimePicker.setCurrentHour(12);
        assertEquals(Integer.valueOf(12), mTimePicker.getCurrentHour());

        mTimePicker.setCurrentHour(13);
        assertEquals(Integer.valueOf(13), mTimePicker.getCurrentHour());

        mTimePicker.setCurrentHour(23);
        assertEquals(Integer.valueOf(23), mTimePicker.getCurrentHour());

        // for 24 hour mode
        mTimePicker.setIs24HourView(true);

        mTimePicker.setCurrentHour(0);
        assertEquals(Integer.valueOf(0), mTimePicker.getCurrentHour());

        mTimePicker.setCurrentHour(13);
        assertEquals(Integer.valueOf(13), mTimePicker.getCurrentHour());

        mTimePicker.setCurrentHour(23);
        assertEquals(Integer.valueOf(23), mTimePicker.getCurrentHour());
    }

    public void testAccessIs24HourView() {
        mTimePicker = new TimePicker(mContext);
        assertFalse(mTimePicker.is24HourView());

        mTimePicker.setIs24HourView(true);
        assertTrue(mTimePicker.is24HourView());

        mTimePicker.setIs24HourView(false);
        assertFalse(mTimePicker.is24HourView());
    }

    public void testAccessCurrentMinute() {
        mTimePicker = new TimePicker(mContext);

        mTimePicker.setCurrentMinute(0);
        assertEquals(Integer.valueOf(0), mTimePicker.getCurrentMinute());

        mTimePicker.setCurrentMinute(12);
        assertEquals(Integer.valueOf(12), mTimePicker.getCurrentMinute());

        mTimePicker.setCurrentMinute(33);
        assertEquals(Integer.valueOf(33), mTimePicker.getCurrentMinute());

        mTimePicker.setCurrentMinute(59);
        assertEquals(Integer.valueOf(59), mTimePicker.getCurrentMinute());
    }

    public void testGetBaseline() {
        mTimePicker = new TimePicker(mContext);
        assertEquals(-1, mTimePicker.getBaseline());
    }

    public void testOnSaveInstanceStateAndOnRestoreInstanceState() {
        MyTimePicker source = new MyTimePicker(mContext);
        MyTimePicker dest = new MyTimePicker(mContext);
        int expectHour = (dest.getCurrentHour() + 10) % 24;
        int expectMinute = (dest.getCurrentMinute() + 10) % 60;
        source.setCurrentHour(expectHour);
        source.setCurrentMinute(expectMinute);

        Parcelable p = source.onSaveInstanceState();
        dest.onRestoreInstanceState(p);

        assertEquals(Integer.valueOf(expectHour), dest.getCurrentHour());
        assertEquals(Integer.valueOf(expectMinute), dest.getCurrentMinute());
    }

    private class MockOnTimeChangeListener implements OnTimeChangedListener {
        private TimePicker mNotifiedView;

        private boolean mHasCalledOnTimeChanged;

        private int mNotifiedHourOfDay;

        private int mNotifiedMinute;;

        public boolean hasCalledOnTimeChanged() {
            return mHasCalledOnTimeChanged;
        }

        public TimePicker getNotifiedView() {
            return mNotifiedView;
        }

        public int getNotifiedHourOfDay() {
            return mNotifiedHourOfDay;
        }

        public int getNotifiedMinute() {
            return mNotifiedMinute;
        }

        public void reset() {
            mNotifiedView = null;
            mNotifiedHourOfDay = 0;
            mNotifiedMinute = 0;
            mHasCalledOnTimeChanged = false;
        }

        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            mNotifiedView = view;
            mNotifiedHourOfDay = hourOfDay;
            mNotifiedMinute = minute;
            mHasCalledOnTimeChanged = true;
        }
    }

    private class MyTimePicker extends TimePicker {
        public MyTimePicker(Context context) {
            super(context);
        }

        @Override
        protected void onRestoreInstanceState(Parcelable state) {
            super.onRestoreInstanceState(state);
        }

        @Override
        protected Parcelable onSaveInstanceState() {
            return super.onSaveInstanceState();
        }
    }
}
