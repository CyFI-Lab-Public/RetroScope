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
import android.test.ActivityInstrumentationTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * Test {@link ViewFlipper}.
 */
public class ViewFlipperTest extends ActivityInstrumentationTestCase<ViewFlipperStubActivity> {
    private Activity mActivity;

    public ViewFlipperTest() {
        super("com.android.cts.stub", ViewFlipperStubActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        assertNotNull(mActivity);
    }

    public void testConstructor() {
        new ViewFlipper(mActivity);

        new ViewFlipper(mActivity, null);

        XmlPullParser parser = mActivity.getResources().getXml(R.layout.viewflipper_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new ViewFlipper(mActivity, attrs);

        try {
            new ViewFlipper(null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testSetFlipInterval() {
        ViewFlipper viewFlipper = new ViewFlipper(mActivity);
        viewFlipper.setFlipInterval(0);
        viewFlipper.setFlipInterval(-1);
    }

    public void testViewFlipper() throws Throwable {
        // NOTE: This value needs to be kept in sync with the value set in
        // layout/viewflipper_layout.xml
        final int FLIP_INTERVAL = 1000;

        runTestOnUiThread(new Runnable() {
            public void run() {
                ViewFlipper viewFlipper =
                        (ViewFlipper) mActivity.findViewById(R.id.viewflipper_test);

                TextView iv1 = (TextView) mActivity.findViewById(R.id.viewflipper_textview1);
                TextView iv2 = (TextView) mActivity.findViewById(R.id.viewflipper_textview2);

                assertFalse(viewFlipper.isFlipping());
                assertSame(iv1, viewFlipper.getCurrentView());

                viewFlipper.startFlipping();
                assertTrue(viewFlipper.isFlipping());
                assertSame(iv1, viewFlipper.getCurrentView());
                assertEquals(View.VISIBLE, iv1.getVisibility());
                assertEquals(View.GONE, iv2.getVisibility());
            }
        });

        // wait for a longer time to make sure the view flipping is completed.
        waitForViewFlipping(FLIP_INTERVAL + 200);
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(new Runnable() {
            public void run() {
                ViewFlipper viewFlipper =
                        (ViewFlipper) mActivity.findViewById(R.id.viewflipper_test);

                TextView iv1 = (TextView) mActivity.findViewById(R.id.viewflipper_textview1);
                TextView iv2 = (TextView) mActivity.findViewById(R.id.viewflipper_textview2);

                assertSame(iv2, viewFlipper.getCurrentView());
                assertEquals(View.GONE, iv1.getVisibility());
                assertEquals(View.VISIBLE, iv2.getVisibility());
            }
        });

        waitForViewFlipping(FLIP_INTERVAL + 200);
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(new Runnable() {
            public void run() {
                ViewFlipper viewFlipper =
                        (ViewFlipper) mActivity.findViewById(R.id.viewflipper_test);

                TextView iv1 = (TextView) mActivity.findViewById(R.id.viewflipper_textview1);
                TextView iv2 = (TextView) mActivity.findViewById(R.id.viewflipper_textview2);

                assertSame(iv1, viewFlipper.getCurrentView());
                assertEquals(View.VISIBLE, iv1.getVisibility());
                assertEquals(View.GONE, iv2.getVisibility());

                viewFlipper.stopFlipping();
                assertFalse(viewFlipper.isFlipping());
            }
        });
    }

    private void waitForViewFlipping(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}
