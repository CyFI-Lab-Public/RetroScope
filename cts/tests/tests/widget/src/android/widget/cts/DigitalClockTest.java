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

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.test.ActivityInstrumentationTestCase;
import android.test.UiThreadTest;
import android.util.AttributeSet;
import android.util.Xml;
import android.widget.DigitalClock;
import android.widget.LinearLayout;

import com.android.cts.stub.R;
import com.android.internal.util.XmlUtils;


/**
 * Test {@link DigitalClock}.
 */
public class DigitalClockTest extends ActivityInstrumentationTestCase<DigitalClockStubActivity> {
    private Activity mActivity;
    private Context mContext;

    public DigitalClockTest() {
        super("com.android.cts.stub", DigitalClockStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mContext = getInstrumentation().getContext();
    }

    public void testConstructor() {
        // new the DigitalClock instance
        new DigitalClock(mContext);

        // new the DigitalClock instance with null AttributeSet
        new DigitalClock(mContext, null);

        // new the DigitalClock instance with real AttributeSet
        new DigitalClock(mContext, getAttributeSet(R.layout.digitalclock_layout));

        // Test constructor with null Context, in fact, DigitalClock(mContext) function will
        //finally invoke this version.
        try {
            // Test with null Context
            new DigitalClock(null, getAttributeSet(R.layout.digitalclock_layout));
            fail("should throw NullPointerException");
        } catch (Exception e) {
        }
    }

    @UiThreadTest
    public void testOnDetachedFromWindow() {
        final MockDigitalClock digitalClock = createDigitalClock();

        final LinearLayout linearLayout = (LinearLayout) mActivity.findViewById(
                R.id.digitalclock_root);

        assertFalse(digitalClock.hasCalledOnAttachedToWindow());
        linearLayout.addView(digitalClock);

        assertTrue(digitalClock.hasCalledOnAttachedToWindow());
        linearLayout.removeView(digitalClock);
    }

    @UiThreadTest
    public void testOnAttachedToWindow() {
        final MockDigitalClock digitalClock = createDigitalClock();

        final LinearLayout linearLayout = (LinearLayout) mActivity.findViewById(
                R.id.digitalclock_root);

        linearLayout.addView(digitalClock);

        assertFalse(digitalClock.hasCalledOnDetachedFromWindow());

        // Clear linearLayout
        linearLayout.removeView(digitalClock);

        assertTrue(digitalClock.hasCalledOnDetachedFromWindow());
    }

    private MockDigitalClock createDigitalClock() {
        MockDigitalClock datePicker = new MockDigitalClock(mContext,
                getAttributeSet(R.layout.digitalclock_layout));

        return datePicker;
    }

    private AttributeSet getAttributeSet(int resourceId) {
        XmlResourceParser parser = mActivity.getResources().getXml(resourceId);
        try {
            XmlUtils.beginDocument(parser, "com.android.cts.stub.alarmclock.DigitalClock");
        } catch (XmlPullParserException e) {
            fail("unexpected XmlPullParserException.");
        } catch (IOException e) {
            fail("unexpected IOException.");
        }
        AttributeSet attr = Xml.asAttributeSet(parser);
        assertNotNull(attr);
        return attr;
    }

    private class MockDigitalClock extends DigitalClock {
        private boolean mCalledOnAttachedToWindow   = false;
        private boolean mCalledOnDetachedFromWindow = false;

        public MockDigitalClock(Context context) {
            super(context);
        }

        public MockDigitalClock(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            mCalledOnAttachedToWindow = true;
        }

        public boolean hasCalledOnAttachedToWindow() {
            return mCalledOnAttachedToWindow;
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mCalledOnDetachedFromWindow = true;
        }

        public boolean hasCalledOnDetachedFromWindow() {
            return mCalledOnDetachedFromWindow;
        }
    }
}
