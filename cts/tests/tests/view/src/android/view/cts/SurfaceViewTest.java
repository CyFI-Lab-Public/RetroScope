/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.view.cts;

import android.app.Instrumentation;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Region;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.cts.SurfaceViewStubActivity.MockSurfaceView;

public class SurfaceViewTest extends ActivityInstrumentationTestCase2<SurfaceViewStubActivity> {

    private Context mContext;
    private Instrumentation mInstrumentation;
    private MockSurfaceView mMockSurfaceView;

    public SurfaceViewTest() {
        super("com.android.cts.stub", SurfaceViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = mInstrumentation.getContext();
        final SurfaceViewStubActivity activity = getActivity();
        new PollingCheck() {
            @Override
                protected boolean check() {
                return activity.hasWindowFocus();
            }
        }.run();
        mMockSurfaceView = activity.getSurfaceView();
    }

    public void testConstructor() {
        new SurfaceView(mContext);
        new SurfaceView(mContext, null);
        new SurfaceView(mContext, null, 0);
    }

    public void testSurfaceView() {
        final int left = 40;
        final int top = 30;
        final int right = 320;
        final int bottom = 240;

        assertTrue(mMockSurfaceView.isDraw());
        assertTrue(mMockSurfaceView.isOnAttachedToWindow());
        assertTrue(mMockSurfaceView.isDispatchDraw());
        assertTrue(mMockSurfaceView.isDrawColor());
        assertTrue(mMockSurfaceView.isSurfaceChanged());

        assertTrue(mMockSurfaceView.isOnWindowVisibilityChanged());
        int expectedVisibility = mMockSurfaceView.getVisibility();
        int actualVisibility = mMockSurfaceView.getVInOnWindowVisibilityChanged();
        assertEquals(expectedVisibility, actualVisibility);

        assertTrue(mMockSurfaceView.isOnMeasureCalled());
        int expectedWidth = mMockSurfaceView.getMeasuredWidth();
        int expectedHeight = mMockSurfaceView.getMeasuredHeight();
        int actualWidth = mMockSurfaceView.getWidthInOnMeasure();
        int actualHeight = mMockSurfaceView.getHeightInOnMeasure();
        assertEquals(expectedWidth, actualWidth);
        assertEquals(expectedHeight, actualHeight);

        Region region = new Region();
        region.set(left, top, right, bottom);
        assertTrue(mMockSurfaceView.gatherTransparentRegion(region));

        mMockSurfaceView.setFormat(PixelFormat.TRANSPARENT);
        assertFalse(mMockSurfaceView.gatherTransparentRegion(region));

        SurfaceHolder actual = mMockSurfaceView.getHolder();
        assertNotNull(actual);
        assertTrue(actual instanceof SurfaceHolder);
    }

    @UiThreadTest
    /**
     * check point:
     * check surfaceView size before and after layout
     */
    public void testOnSizeChanged() {
        final int left = 40;
        final int top = 30;
        final int right = 320;
        final int bottom = 240;

        // change the SurfaceView size
        int beforeLayoutWidth = mMockSurfaceView.getWidth();
        int beforeLayoutHeight = mMockSurfaceView.getHeight();
        mMockSurfaceView.resetOnSizeChangedFlag(false);
        assertFalse(mMockSurfaceView.isOnSizeChangedCalled());
        mMockSurfaceView.layout(left, top, right, bottom);
        assertTrue(mMockSurfaceView.isOnSizeChangedCalled());
        assertEquals(beforeLayoutWidth, mMockSurfaceView.getOldWidth());
        assertEquals(beforeLayoutHeight, mMockSurfaceView.getOldHeight());
        assertEquals(right - left, mMockSurfaceView.getWidth());
        assertEquals(bottom - top, mMockSurfaceView.getHeight());
    }

    @UiThreadTest
    /**
     * check point:
     * check surfaceView scroll X and y before and after scrollTo
     */
    public void testOnScrollChanged() {
        final int scrollToX = 200;
        final int scrollToY = 200;

        int oldHorizontal = mMockSurfaceView.getScrollX();
        int oldVertical = mMockSurfaceView.getScrollY();
        assertFalse(mMockSurfaceView.isOnScrollChanged());
        mMockSurfaceView.scrollTo(scrollToX, scrollToY);
        assertTrue(mMockSurfaceView.isOnScrollChanged());
        assertEquals(oldHorizontal, mMockSurfaceView.getOldHorizontal());
        assertEquals(oldVertical, mMockSurfaceView.getOldVertical());
        assertEquals(scrollToX, mMockSurfaceView.getScrollX());
        assertEquals(scrollToY, mMockSurfaceView.getScrollY());
    }

    public void testOnDetachedFromWindow() {
        final MockSurfaceView mockSurfaceView = getActivity().getSurfaceView();
        assertFalse(mockSurfaceView.isDetachedFromWindow());
        assertTrue(mockSurfaceView.isShown());
        sendKeys(KeyEvent.KEYCODE_BACK);
        new PollingCheck() {
            @Override
            protected boolean check() {
                return mockSurfaceView.isDetachedFromWindow() &&
                       !mockSurfaceView.isShown();
            }
        }.run();
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            fail("error occurs when wait for an action: " + e.toString());
        }
    }
}
