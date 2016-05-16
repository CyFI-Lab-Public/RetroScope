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


import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.UiThreadTest;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ZoomControls;

/**
 * Test {@link ZoomControls}.
 */
public class ZoomControlsTest extends InstrumentationTestCase {
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
    }

    public void testConstructor() {
        new ZoomControls(mContext);

        new ZoomControls(mContext, null);
    }

    public void testSetOnZoomInClickListener() {
        ZoomControls zoomControls = new ZoomControls(mContext);

        // normal parameters
        final MockOnClickListener clickListener = new MockOnClickListener();
        zoomControls.setOnZoomInClickListener(clickListener);

        // exceptional parameters
        zoomControls.setOnZoomInClickListener(null);
    }

    private class MockOnClickListener implements OnClickListener {
        public void onClick(View v) {
            // ignore
        }
    }

    public void testSetOnZoomOutClickListener() {
        ZoomControls zoomControls = new ZoomControls(mContext);

        // normal parameters
        final MockOnClickListener clickListener = new MockOnClickListener();
        zoomControls.setOnZoomOutClickListener(clickListener);

        // exceptional parameters
        zoomControls.setOnZoomOutClickListener(null);
    }

    public void testSetZoomSpeed() {
        ZoomControls zoomControls = new ZoomControls(mContext);

        zoomControls.setZoomSpeed(500);

        // TODO: how to check?
    }

    public void testOnTouchEvent() {
        // onTouchEvent() is implementation details, do NOT test
    }

    public void testShowAndHide() {
        final ZoomControls zoomControls = new ZoomControls(mContext);
        assertEquals(View.VISIBLE, zoomControls.getVisibility());

        zoomControls.hide();
        assertEquals(View.GONE, zoomControls.getVisibility());

        zoomControls.show();
        assertEquals(View.VISIBLE, zoomControls.getVisibility());
    }

    public void testSetIsZoomInEnabled() {
        ZoomControls zoomControls = new ZoomControls(mContext);
        zoomControls.setIsZoomInEnabled(false);
        zoomControls.setIsZoomInEnabled(true);
    }

    public void testSetIsZoomOutEnabled() {
        ZoomControls zoomControls = new ZoomControls(mContext);
        zoomControls.setIsZoomOutEnabled(false);
        zoomControls.setIsZoomOutEnabled(true);
    }

    @UiThreadTest
    public void testHasFocus() {
        ZoomControls zoomControls = new ZoomControls(mContext);
        assertFalse(zoomControls.hasFocus());

        zoomControls.requestFocus();
        assertTrue(zoomControls.hasFocus());
    }
}
