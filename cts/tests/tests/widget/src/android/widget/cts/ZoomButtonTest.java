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
import android.cts.util.PollingCheck;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.ZoomButton;

public class ZoomButtonTest extends ActivityInstrumentationTestCase2<ZoomButtonStubActivity> {
    private ZoomButton mZoomButton;
    private Activity mActivity;

    public ZoomButtonTest() {
        super("com.android.cts.stub", ZoomButtonStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mZoomButton = (ZoomButton) getActivity().findViewById(R.id.zoombutton_test);
        mActivity = getActivity();
    }

    public void testConstructor() {
        new ZoomButton(mActivity);

        new ZoomButton(mActivity, null);

        new ZoomButton(mActivity, null, 0);

        XmlPullParser parser = mActivity.getResources().getXml(R.layout.zoombutton_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        assertNotNull(attrs);
        new ZoomButton(mActivity, attrs);
        new ZoomButton(mActivity, attrs, 0);

        try {
            new ZoomButton(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new ZoomButton(null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new ZoomButton(null, null, 0);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testSetEnabled() {
        assertFalse(mZoomButton.isPressed());
        mZoomButton.setEnabled(true);
        assertTrue(mZoomButton.isEnabled());
        assertFalse(mZoomButton.isPressed());

        mZoomButton.setPressed(true);
        assertTrue(mZoomButton.isPressed());
        mZoomButton.setEnabled(true);
        assertTrue(mZoomButton.isEnabled());
        assertTrue(mZoomButton.isPressed());

        mZoomButton.setEnabled(false);
        assertFalse(mZoomButton.isEnabled());
        assertFalse(mZoomButton.isPressed());
    }

    @UiThreadTest
    public void testDispatchUnhandledMove() {
        assertFalse(mZoomButton.dispatchUnhandledMove(new ListView(mActivity), View.FOCUS_DOWN));

        assertFalse(mZoomButton.dispatchUnhandledMove(null, View.FOCUS_DOWN));
    }

    public void testOnLongClick() {
        final MockOnClickListener listener = new MockOnClickListener();
        mZoomButton.setOnClickListener(listener);
        mZoomButton.setEnabled(true);
        long speed = 2000;
        mZoomButton.setZoomSpeed(speed);

        assertFalse(listener.hasOnClickCalled());
        mZoomButton.performLongClick();
        new PollingCheck(speed + 500) {
            @Override
            protected boolean check() {
                return listener.hasOnClickCalled();
            }
        };
    }

    public void testOnTouchEvent() {
        // Do not test. Implementation details.
    }

    public void testOnKeyUp() {
        // Do not test. Implementation details.
    }

    public void testSetZoomSpeed() {
        mZoomButton.setZoomSpeed(100);

        mZoomButton.setZoomSpeed(-1);
        // TODO: how to check?
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
