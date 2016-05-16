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


import android.app.Activity;
import android.app.Instrumentation;
import android.app.cts.MockActivity;
import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class TouchDelegateTest extends ActivityInstrumentationTestCase2<MockActivity> {
    private static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int ACTION_DOWN = MotionEvent.ACTION_DOWN;

    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private Button mButton;
    private Rect mRect;

    private int mXInside;
    private int mYInside;

    private Exception mException;

    public TouchDelegateTest() {
        super("com.android.cts.stub", MockActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();

        mButton = new Button(mActivity);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    mActivity.addContentView(mButton, new LinearLayout.LayoutParams(WRAP_CONTENT,
                                                                                    WRAP_CONTENT));
                } catch (Exception e) {
                    mException = e;
                }
            }
        });
        mInstrumentation.waitForIdleSync();

        if(mException != null) {
            throw mException;
        }

        int right = mButton.getRight();
        int bottom = mButton.getBottom();
        mXInside = (mButton.getLeft() + right) / 3;
        mYInside = (mButton.getTop() + bottom) / 3;

        mRect = new Rect();
        mButton.getHitRect(mRect);
    }

    @UiThreadTest
    public void testOnTouchEvent() {
        // test callback of onTouchEvent
        View view = new View(mActivity);
        MockTouchDelegate touchDelegate = new MockTouchDelegate(mRect, mButton);
        view.setTouchDelegate(touchDelegate);
        assertFalse(touchDelegate.mOnTouchEventCalled);
        view.onTouchEvent(MotionEvent.obtain(0, 0, ACTION_DOWN, mXInside, mYInside, 0));
        assertTrue(touchDelegate.mOnTouchEventCalled);
    }

    class MockTouchDelegate extends TouchDelegate {
        private boolean mOnTouchEventCalled;

        public MockTouchDelegate(Rect bounds, View delegateView) {
            super(bounds, delegateView);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            mOnTouchEventCalled = true;
            return true;
        }
    }
}
