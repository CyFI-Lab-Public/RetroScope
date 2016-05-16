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

import android.app.Activity;
import android.cts.util.PollingCheck;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.cts.AnimationTestUtils;

import com.android.cts.stub.R;


/**
 * Test {@link View}.
 */
public class View_AnimationTest extends ActivityInstrumentationTestCase2<ViewTestStubActivity> {

    private static final int TIME_OUT = 5000;
    private static final int DURATION = 2000;

    private Activity mActivity;

    private TranslateAnimation mAnimation;

    public View_AnimationTest() {
        super("com.android.cts.stub", ViewTestStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mAnimation =  new TranslateAnimation(0.0f, 10.0f, 0.0f, 10.0f);
        mAnimation.setDuration(DURATION);
    }

    public void testAnimation() throws Throwable {
        final View view = mActivity.findViewById(R.id.mock_view);
        // set null animation
        view.setAnimation(null);
        assertNull(view.getAnimation());

        view.setAnimation(mAnimation);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.invalidate();
            }
        });

        AnimationTestUtils.assertRunAnimation(getInstrumentation(), view, mAnimation, TIME_OUT);
    }

    public void testStartAnimation() throws Throwable {
        final View view = mActivity.findViewById(R.id.mock_view);
        // start null animation
        try {
            view.startAnimation(null);
            fail("did not throw NullPointerException when start null animation");
        } catch (NullPointerException e) {
            // expected
        }

        runTestOnUiThread(new Runnable() {
            public void run() {
                view.startAnimation(mAnimation);
            }
        });

        AnimationTestUtils.assertRunAnimation(getInstrumentation(), view, mAnimation, TIME_OUT);
    }

    public void testClearBeforeAnimation() throws Throwable {
        final View view = mActivity.findViewById(R.id.mock_view);
        assertFalse(mAnimation.hasStarted());

        view.setAnimation(mAnimation);

        assertSame(mAnimation, view.getAnimation());

        runTestOnUiThread(new Runnable() {
            public void run() {
                view.clearAnimation();
                view.invalidate();
            }
        });

        Thread.sleep(TIME_OUT);
        assertFalse(mAnimation.hasStarted());
        assertNull(view.getAnimation());
    }

    public void testClearDuringAnimation() throws Throwable {
        final View view = mActivity.findViewById(R.id.mock_view);
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.startAnimation(mAnimation);
                assertNotNull(view.getAnimation());
            }
        });

        new PollingCheck(TIME_OUT) {
            @Override
            protected boolean check() {
                return mAnimation.hasStarted();
            }
        }.run();
        runTestOnUiThread(new Runnable() {
            public void run() {
                view.clearAnimation();
            }
        });
        Thread.sleep(TIME_OUT);
        assertTrue(mAnimation.hasStarted());
        assertTrue(mAnimation.hasEnded());
        assertNull(view.getAnimation());
    }
}
