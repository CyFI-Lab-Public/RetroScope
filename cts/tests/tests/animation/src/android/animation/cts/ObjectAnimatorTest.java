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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.test.ActivityInstrumentationTestCase2;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

public class ObjectAnimatorTest extends
        ActivityInstrumentationTestCase2<AnimationActivity> {
    private AnimationActivity mActivity;
    private ObjectAnimator mObjectAnimator;
    private long mDuration = 1000;

    public ObjectAnimatorTest() {
        super(AnimationActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        mActivity = getActivity();
        mObjectAnimator = (ObjectAnimator) mActivity.createAnimatorWithDuration(mDuration);
    }

    public void testDuration() throws Throwable {
        final long duration = 2000;
        ObjectAnimator objectAnimatorLocal = (ObjectAnimator)mActivity.createAnimatorWithDuration(
            duration);
        startAnimation(objectAnimatorLocal);
        assertEquals(duration, objectAnimatorLocal.getDuration());
    }
    public void testOfFloat() throws Throwable {
        Object object = mActivity.view.newBall;
        String property = "y";
        float startY = mActivity.mStartY;
        float endY = mActivity.mStartY + mActivity.mDeltaY;
        ObjectAnimator objAnimator = ObjectAnimator.ofFloat(object, property, startY, endY);
        assertTrue(objAnimator != null);
        objAnimator.setDuration(mDuration);
        objAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objAnimator.setInterpolator(new AccelerateInterpolator());
        objAnimator.setRepeatMode(ValueAnimator.REVERSE);
        startAnimation(objAnimator);
        assertTrue(objAnimator != null);
        Thread.sleep(100);
        float x = mActivity.view.newBall.getX();
        float y = mActivity.view.newBall.getY();
        assertTrue( y >= startY);
        assertTrue( y <= endY);
    }

    public void testOfFloatBase() throws Throwable {
        Object object = mActivity.view.newBall;
        String property = "y";
        float startY = mActivity.mStartY;
        float endY = mActivity.mStartY + mActivity.mDeltaY;
        ObjectAnimator animator = ObjectAnimator.ofFloat(object, property, startY, endY);
        ObjectAnimator objAnimator = new ObjectAnimator();
        objAnimator.setTarget(object);
        objAnimator.setPropertyName(property);
        assertEquals(animator.getTarget(), objAnimator.getTarget());
        assertEquals(animator.getPropertyName(), objAnimator.getPropertyName());
    }

    public void testOfInt() throws Throwable {
        Object object = mActivity.view.newBall;
        String property = "backgroundColor";
        int startColor = mActivity.view.RED;
        int endColor = mActivity.view.BLUE;

        ObjectAnimator colorAnimator = ObjectAnimator.ofInt(object, property,
                startColor, endColor);
        colorAnimator.setDuration(1000);
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setRepeatCount(1);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.start();
        startAnimation(mObjectAnimator, colorAnimator);
        Thread.sleep(100);
        Integer i = (Integer) colorAnimator.getAnimatedValue();
        //We are going from less negative value to a more negative value
        assertTrue(i.intValue() <= startColor);
        assertTrue(endColor <= i.intValue());
    }

    public void testOfObject() throws Throwable {
        Object object = mActivity.view.newBall;
        String property = "backgroundColor";
        int startColor = mActivity.view.RED;
        int endColor = mActivity.view.BLUE;
        Object[] values = {new Integer(startColor), new Integer(endColor)};
        ArgbEvaluator evaluator = new ArgbEvaluator();
        ObjectAnimator colorAnimator = ObjectAnimator.ofObject(object, property,
                evaluator, values);
        colorAnimator.setDuration(1000);
        colorAnimator.setRepeatCount(1);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.start();
        startAnimation(mObjectAnimator, colorAnimator);
        Thread.sleep(100);
        Integer i = (Integer) colorAnimator.getAnimatedValue();
        //We are going from less negative value to a more negative value
        assertTrue(i.intValue() <= startColor);
        assertTrue(endColor <= i.intValue());
    }

    public void testOfPropertyValuesHolder() throws Throwable {
        Object object = mActivity.view.newBall;
        String propertyName = "backgroundColor";
        int startColor = mActivity.view.RED;
        int endColor = mActivity.view.BLUE;
        int values[] = {startColor, endColor};
        ArgbEvaluator evaluator = new ArgbEvaluator();
        PropertyValuesHolder propertyValuesHolder = PropertyValuesHolder.ofInt(propertyName, values);
        ObjectAnimator colorAnimator = ObjectAnimator.ofPropertyValuesHolder(object,
            propertyValuesHolder);
        colorAnimator.setDuration(1000);
        colorAnimator.setRepeatCount(1);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.start();
        startAnimation(mObjectAnimator, colorAnimator);
        Thread.sleep(100);
        Integer i = (Integer) colorAnimator.getAnimatedValue();
        //We are going from less negative value to a more negative value
        assertTrue(i.intValue() <= startColor);
        assertTrue(endColor <= i.intValue());
    }

    public void testGetPropertyName() throws Throwable {
        Object object = mActivity.view.newBall;
        String propertyName = "backgroundColor";
        int startColor = mActivity.view.RED;
        int endColor = mActivity.view.BLUE;
        Object[] values = {new Integer(startColor), new Integer(endColor)};
        ArgbEvaluator evaluator = new ArgbEvaluator();
        ObjectAnimator colorAnimator = ObjectAnimator.ofObject(object, propertyName,
                evaluator, values);
        String actualPropertyName = colorAnimator.getPropertyName();
        assertEquals(propertyName, actualPropertyName);
    }

    public void testSetFloatValues() throws Throwable {
        Object object = mActivity.view.newBall;
        String property = "y";
        float startY = mActivity.mStartY;
        float endY = mActivity.mStartY + mActivity.mDeltaY;
        float[] values = {startY, endY};
        ObjectAnimator objAnimator = new ObjectAnimator();
        objAnimator.setTarget(object);
        objAnimator.setPropertyName(property);
        objAnimator.setFloatValues(values);
        objAnimator.setDuration(mDuration);
        objAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objAnimator.setInterpolator(new AccelerateInterpolator());
        objAnimator.setRepeatMode(ValueAnimator.REVERSE);
        startAnimation(objAnimator);
        Thread.sleep(100);
        float y = mActivity.view.newBall.getY();
        assertTrue( y >= startY);
        assertTrue( y <= endY);
    }

    public void testGetTarget() throws Throwable {
        Object object = mActivity.view.newBall;
        String propertyName = "backgroundColor";
        int startColor = mActivity.view.RED;
        int endColor = mActivity.view.BLUE;
        Object[] values = {new Integer(startColor), new Integer(endColor)};
        ArgbEvaluator evaluator = new ArgbEvaluator();
        ObjectAnimator colorAnimator = ObjectAnimator.ofObject(object, propertyName,
                evaluator, values);
        Object target = colorAnimator.getTarget();
        assertEquals(object, target);
    }

    public void testClone() throws Throwable {
        Object object = mActivity.view.newBall;
        String property = "y";
        float startY = mActivity.mStartY;
        float endY = mActivity.mStartY + mActivity.mDeltaY;
        Interpolator interpolator = new AccelerateInterpolator();
        ObjectAnimator objAnimator = ObjectAnimator.ofFloat(object, property, startY, endY);
        objAnimator.setDuration(mDuration);
        objAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objAnimator.setInterpolator(interpolator);
        objAnimator.setRepeatMode(ValueAnimator.REVERSE);
        ObjectAnimator cloneAnimator = objAnimator.clone();

        assertEquals(mDuration, cloneAnimator.getDuration());
        assertEquals(ValueAnimator.INFINITE, cloneAnimator.getRepeatCount());
        assertEquals(ValueAnimator.REVERSE, cloneAnimator.getRepeatMode());
        assertEquals(object, cloneAnimator.getTarget());
        assertEquals(property, cloneAnimator.getPropertyName());
        assertEquals(interpolator, cloneAnimator.getInterpolator());
    }

    public void testIsStarted() throws Throwable {
        Object object = mActivity.view.newBall;
        String property = "y";
        float startY = mActivity.mStartY;
        float endY = mActivity.mStartY + mActivity.mDeltaY;
        Interpolator interpolator = new AccelerateInterpolator();
        ObjectAnimator objAnimator = ObjectAnimator.ofFloat(object, property, startY, endY);
        objAnimator.setDuration(mDuration);
        objAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objAnimator.setInterpolator(interpolator);
        objAnimator.setRepeatMode(ValueAnimator.REVERSE);
        startAnimation(objAnimator);
        Thread.sleep(100);
        assertTrue(objAnimator.isStarted());
        Thread.sleep(100);
    }

    private void startAnimation(final ObjectAnimator mObjectAnimator) throws Throwable {
        Thread mAnimationRunnable = new Thread() {
            public void run() {
                mActivity.startAnimation(mObjectAnimator);
            }
        };
        this.runTestOnUiThread(mAnimationRunnable);
    }
    private void startAnimation(final ObjectAnimator mObjectAnimator, final
            ObjectAnimator colorAnimator) throws Throwable {
        Thread mAnimationRunnable = new Thread() {
            public void run() {
                mActivity.startAnimation(mObjectAnimator, colorAnimator);
            }
        };
        this.runTestOnUiThread(mAnimationRunnable);
    }
}
