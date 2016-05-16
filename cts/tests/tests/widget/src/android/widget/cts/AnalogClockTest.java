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
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.util.Xml;
import android.widget.AnalogClock;

public class AnalogClockTest extends ActivityInstrumentationTestCase2<FrameLayoutStubActivity> {
    private AttributeSet mAttrSet;
    private Activity mActivity;

    public AnalogClockTest() {
        super("com.android.cts.stub", FrameLayoutStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        XmlPullParser parser = getActivity().getResources().getXml(R.layout.analogclock);
        mAttrSet = Xml.asAttributeSet(parser);
        mActivity = getActivity();
    }

    public void testConstructor() {
        new AnalogClock(mActivity);
        new AnalogClock(mActivity, mAttrSet);
        new AnalogClock(mActivity, mAttrSet, 0);

        try {
            new AnalogClock(null);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new AnalogClock(null, null);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new AnalogClock(null, null, -1);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testOnMeasure() {
        // onMeasure() is implementation details, do NOT test
    }

    public void testOnSizeChanged() {
        // Do not test onSizeChanged(), implementation details
    }

    public void testOnDraw() {
        // Do not test, it's controlled by View. Implementation details
    }

    public void testOnDetachedFromWindow() {
        // Do not test
    }

    public void testOnAttachedToWindow() {
        // Do not test
    }
}
