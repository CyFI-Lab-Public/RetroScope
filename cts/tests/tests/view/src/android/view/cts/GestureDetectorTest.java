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

package android.view.cts;

import android.content.Context;
import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;

public class GestureDetectorTest extends
        ActivityInstrumentationTestCase2<GestureDetectorStubActivity> {

    private GestureDetector mGestureDetector;
    private GestureDetectorStubActivity mActivity;
    private Context mContext;

    public GestureDetectorTest() {
        super("com.android.cts.stub", GestureDetectorStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mGestureDetector = mActivity.getGestureDetector();
        mContext = getInstrumentation().getTargetContext();
        mActivity.isDown = false;
        mActivity.isScroll = false;
        mActivity.isFling = false;
        mActivity.isSingleTapUp = false;
        mActivity.onShowPress = false;
        mActivity.onLongPress = false;
        mActivity.onDoubleTap = false;
        mActivity.onDoubleTapEvent = false;
        mActivity.onSingleTapConfirmed = false;
    }

    public void testConstructor() {

        new GestureDetector(mContext, new SimpleOnGestureListener(), new Handler());
        new GestureDetector(mContext, new SimpleOnGestureListener());
        new GestureDetector(new SimpleOnGestureListener(), new Handler());
        new GestureDetector(new SimpleOnGestureListener());

        try {
            mGestureDetector = new GestureDetector(null);
            fail("should throw null exception");
        } catch (RuntimeException e) {
            // expected
        }
    }

    public void testLongpressEnabled() {
        mGestureDetector.setIsLongpressEnabled(true);
        assertTrue(mGestureDetector.isLongpressEnabled());
        mGestureDetector.setIsLongpressEnabled(false);
        assertFalse(mGestureDetector.isLongpressEnabled());
    }
}
