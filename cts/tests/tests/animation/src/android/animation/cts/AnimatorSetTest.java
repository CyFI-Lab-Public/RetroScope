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

import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.test.ActivityInstrumentationTestCase2;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;

public class AnimatorSetTest extends
        ActivityInstrumentationTestCase2<AnimationActivity> {
    private AnimationActivity mActivity;
    private AnimatorSet mAnimatorSet;
    private long mDuration = 1000;
    private Object object;
    private ObjectAnimator yAnimator;
    private ObjectAnimator xAnimator;

    public AnimatorSetTest() {
        super(AnimationActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mActivity = getActivity();
        object = mActivity.view.newBall;
        yAnimator = getYAnimator(object);
        xAnimator = getXAnimator(object);
    }

     public void testPlaySequentially() throws Throwable {
         Animator[] animatorArray = {xAnimator, yAnimator};

         mAnimatorSet = new AnimatorSet();
         mAnimatorSet.playSequentially(animatorArray);

         assertFalse(mAnimatorSet.isRunning());
         startAnimation(mAnimatorSet);
         Thread.sleep(100);
         assertTrue(mAnimatorSet.isRunning());
    }

    public void testPlayTogether() throws Throwable {
        xAnimator.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = {xAnimator, yAnimator};

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);

        assertFalse(mAnimatorSet.isRunning());
        startAnimation(mAnimatorSet);
        Thread.sleep(100);
        assertTrue(mAnimatorSet.isRunning());
   }

    public void testDuration() throws Throwable {
        xAnimator.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = { xAnimator, yAnimator };

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);
        mAnimatorSet.setDuration(1000);

        startAnimation(mAnimatorSet);
        Thread.sleep(100);
        assertEquals(mAnimatorSet.getDuration(), 1000);
    }

    public void testStartDelay() throws Throwable {
        xAnimator.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = { xAnimator, yAnimator };

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);
        mAnimatorSet.setStartDelay(10);

        startAnimation(mAnimatorSet);
        Thread.sleep(100);
        assertEquals(mAnimatorSet.getStartDelay(), 10);
    }

    public void testgetChildAnimations() throws Throwable {
        Animator[] animatorArray = { xAnimator, yAnimator };

        mAnimatorSet = new AnimatorSet();
        ArrayList<Animator> childAnimations = mAnimatorSet.getChildAnimations();
        assertEquals(0, mAnimatorSet.getChildAnimations().size());
        mAnimatorSet.playSequentially(animatorArray);
        assertEquals(2, mAnimatorSet.getChildAnimations().size());
    }

    public void testSetInterpolator() throws Throwable {
        xAnimator.setRepeatCount(ValueAnimator.INFINITE);
        Animator[] animatorArray = {xAnimator, yAnimator};
        TimeInterpolator interpolator = new AccelerateDecelerateInterpolator();
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorArray);
        mAnimatorSet.setInterpolator(interpolator);

        assertFalse(mAnimatorSet.isRunning());
        startAnimation(mAnimatorSet);
        Thread.sleep(100);

        ArrayList<Animator> animatorList = mAnimatorSet.getChildAnimations();
        assertEquals(interpolator, ((ObjectAnimator)animatorList.get(0)).getInterpolator());
        assertEquals(interpolator, ((ObjectAnimator)animatorList.get(1)).getInterpolator());
    }

    public ObjectAnimator getXAnimator(Object object) {
        String propertyX = "x";
        float startX = mActivity.mStartX;
        float endX = mActivity.mStartX + mActivity.mDeltaX;
        ObjectAnimator xAnimator = ObjectAnimator.ofFloat(object, propertyX, startX, endX);
        xAnimator.setDuration(mDuration);
        xAnimator.setRepeatCount(ValueAnimator.INFINITE);
        xAnimator.setInterpolator(new AccelerateInterpolator());
        xAnimator.setRepeatMode(ValueAnimator.REVERSE);
        return xAnimator;
    }

    public ObjectAnimator getYAnimator(Object object) {
         String property = "y";
         float startY = mActivity.mStartY;
         float endY = mActivity.mStartY + mActivity.mDeltaY;
         ObjectAnimator yAnimator = ObjectAnimator.ofFloat(object, property, startY, endY);
         yAnimator.setDuration(mDuration);
         yAnimator.setRepeatCount(2);
         yAnimator.setInterpolator(new AccelerateInterpolator());
         yAnimator.setRepeatMode(ValueAnimator.REVERSE);
        return yAnimator;
    }

    private void startAnimation(final AnimatorSet animatorSet) throws Throwable {
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.startAnimatorSet(animatorSet);
            }
        });
    }
}
