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

package android.text.style.cts;

import com.android.cts.stub.R;


import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.os.Parcel;
import android.test.ActivityInstrumentationTestCase2;
import android.text.style.URLSpan;
import android.widget.TextView;

public class URLSpanTest extends ActivityInstrumentationTestCase2<URLSpanStubActivity> {
    // The scheme of TEST_URL must be "ctstest" to launch MockURLSpanTestActivity
    private static final String TEST_URL = "ctstest://urlSpan/test";
    private Activity mActivity;

    public URLSpanTest() {
        super("com.android.cts.stub", URLSpanStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructor() {
        URLSpan urlSpan = new URLSpan(TEST_URL);

        final Parcel p = Parcel.obtain();
        try {
            urlSpan.writeToParcel(p, 0);
            p.setDataPosition(0);
            new URLSpan(p);
        } finally {
            p.recycle();
        }
    }

    public void testGetURL() {
        URLSpan urlSpan = new URLSpan(TEST_URL);
        assertEquals(TEST_URL, urlSpan.getURL());
    }

    public void testOnClick() {
        final URLSpan urlSpan = new URLSpan(TEST_URL);
        final TextView textView = (TextView) mActivity.findViewById(R.id.url);

        Instrumentation instrumentation = getInstrumentation();
        ActivityMonitor am = instrumentation.addMonitor(MockURLSpanTestActivity.class.getName(),
                null, false);

        try {
            runTestOnUiThread(new Runnable() {
                public void run() {
                    urlSpan.onClick(textView);
                }
            });
        } catch (Throwable e) {
            fail("Exception error!");
        }

        Activity newActivity = am.waitForActivityWithTimeout(5000);
        assertNotNull(newActivity);
        newActivity.finish();
    }

    public void testOnClickFailure() {
        URLSpan urlSpan = new URLSpan(TEST_URL);

        try {
            urlSpan.onClick(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testDescribeContents() {
        URLSpan urlSpan = new URLSpan(TEST_URL);
        urlSpan.describeContents();
    }

    public void testGetSpanTypeId() {
        URLSpan urlSpan = new URLSpan(TEST_URL);
        urlSpan.getSpanTypeId();
    }

    public void testWriteToParcel() {
        Parcel p = Parcel.obtain();
        try {
            URLSpan urlSpan = new URLSpan(TEST_URL);
            urlSpan.writeToParcel(p, 0);
            p.setDataPosition(0);
            URLSpan u = new URLSpan(p);
            assertEquals(TEST_URL, u.getURL());
        } finally {
            p.recycle();
        }
    }
}
