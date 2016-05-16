/*
 * Copyright (C) 2012 The Android Open Source Project
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
package android.animation.cts;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.test.ActivityInstrumentationTestCase2;
import android.view.animation.AccelerateInterpolator;

import java.util.List;

public class AnimatorTest extends ActivityInstrumentationTestCase2<AnimationActivity> {
    private AnimationActivity mActivity;
    private Animator mAnimator;
    private long mDuration = 1000;
    public AnimatorTest() {
        super(AnimationActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mActivity = getActivity();
        mAnimator = mActivity.createAnimatorWithDuration(mDuration);
    }

    public void testConstructor() {
        mAnimator = new ValueAnimator();
        assertNotNull(mAnimator);
    }

    public void testClone() {
        Animator animatorClone = mAnimator.clone();
        assertEquals(mAnimator.getDuration(), animatorClone.getDuration());
    }

    public void testStartDelay() {
        long startDelay = 1000;
        mAnimator.setStartDelay(startDelay);
        assertEquals(startDelay, mAnimator.getStartDelay());
    }

    public void testStart() {
        mAnimator.start();
        assertTrue(mAnimator.isRunning());
        assertTrue(mAnimator.isStarted());
    }

    public void testGetDuration() throws Throwable {
        final long duration = 2000;
        Animator animatorLocal = mActivity.createAnimatorWithDuration(duration);
        startAnimation(animatorLocal);
        assertEquals(duration, animatorLocal.getDuration());
    }

    public void testIsRunning() throws Throwable {
        assertFalse(mAnimator.isRunning());
        startAnimation(mAnimator);
        assertTrue(mAnimator.isRunning());
    }

    public void testIsStarted() throws Throwable {
        assertFalse(mAnimator.isRunning());
        assertFalse(mAnimator.isStarted());
        long startDelay = 10000;
        mAnimator.setStartDelay(startDelay);
        startAnimation(mAnimator);
        assertFalse(mAnimator.isRunning());
        assertTrue(mAnimator.isStarted());
    }

    public void testSetInterpolator() throws Throwable {
        AccelerateInterpolator interpolator = new AccelerateInterpolator();
        ValueAnimator mValueAnimator = mActivity.createAnimatorWithInterpolator(interpolator);
        startAnimation(mValueAnimator);
        assertTrue(interpolator.equals(mValueAnimator.getInterpolator()));
    }

    public void testCancel() throws Throwable {
        startAnimation(mAnimator);
        Thread.sleep(100);
        runTestOnUiThread(new Runnable() {
            public void run() {
                mAnimator.cancel();
            }
        });
        assertFalse(mAnimator.isRunning());
    }

    public void testEnd() throws Throwable {
        Object object = mActivity.view.newBall;
        String property = "y";
        float startY = mActivity.mStartY;
        float endY = mActivity.mStartY + mActivity.mDeltaY;
        Animator animator = ObjectAnimator.ofFloat(object, property, startY, endY);
        animator.setDuration(mDuration);
        ((ObjectAnimator)animator).setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateInterpolator());
        ((ObjectAnimator)animator).setRepeatMode(ValueAnimator.REVERSE);
        startAnimation(animator);
        Thread.sleep(100);
        endAnimation(animator);
        float y = mActivity.view.newBall.getY();
        assertEquals(y, endY);
    }

    public void testSetListener() throws Throwable {
        List<Animator.AnimatorListener> listListeners = mAnimator.getListeners();
        assertNull(listListeners);
        MyListener listener = new MyListener();
        assertFalse(listener.mStart);
        assertFalse(listener.mEnd);
        assertEquals(listener.mRepeat, 0);
        mAnimator.addListener(listener);
        mAnimator.setDuration(100l);
        startAnimation(mAnimator);
        Thread.sleep(200);

        assertTrue(listener.mStart);
        assertFalse(listener.mEnd);
        assertTrue(listener.mRepeat >= 0);

        mAnimator.cancel();
        assertTrue(listener.mCancel);

        mAnimator.end();
        assertTrue(listener.mEnd);
    }

    public void testRemoveListener() throws Throwable {
        List<Animator.AnimatorListener> listListenersOne = mAnimator.getListeners();
        assertNull(listListenersOne);
        MyListener listener = new MyListener();
        mAnimator.addListener(listener);

        List<Animator.AnimatorListener> listListenersTwo = mAnimator.getListeners();
        assertEquals(listListenersTwo.size(), 1);
        mAnimator.removeListener(listener);

        List<Animator.AnimatorListener> listListenersThree = mAnimator.getListeners();
        assertNull(listListenersThree);
    }

    public void testRemoveAllListenerers() throws Throwable {
        MyListener listener1 = new MyListener();
        MyListener listener2 = new MyListener();
        mAnimator.addListener(listener1);
        mAnimator.addListener(listener2);

        List<Animator.AnimatorListener> listListenersOne = mAnimator.getListeners();
        assertEquals(listListenersOne.size(), 2);
        mAnimator.removeAllListeners();

        List<Animator.AnimatorListener> listListenersTwo = mAnimator.getListeners();
        assertNull(listListenersTwo);
    }

    class MyListener implements Animator.AnimatorListener{
        boolean mStart = false;
        boolean mEnd = false;
        boolean mCancel = false;
        int mRepeat = 0;

        public void onAnimationCancel(Animator animation) {
            mCancel = true;
        }

        public void onAnimationEnd(Animator animation) {
            mEnd = true;
        }

        public void onAnimationRepeat(Animator animation) {
            mRepeat++;
        }

        public void onAnimationStart(Animator animation) {
            mStart = true;
        }
    }
    private void startAnimation(final Animator animator) throws Throwable {
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.startAnimation(animator);
            }
        });
    }

    private void endAnimation(final Animator animator) throws Throwable {
        Thread animationRunnable = new Thread() {
            public void run() {
                animator.end();
            }
        };
        this.runTestOnUiThread(animationRunnable);
    }
}

