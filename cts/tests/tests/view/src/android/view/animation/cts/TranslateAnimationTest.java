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

package android.view.animation.cts;

import android.app.Activity;
import android.content.res.XmlResourceParser;
import android.graphics.Matrix;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;

import com.android.cts.stub.R;


public class TranslateAnimationTest
        extends ActivityInstrumentationTestCase2<AnimationTestStubActivity> {

    private Activity mActivity;

    private static final long DURATION = 1000;
    private static final float POSITION_DELTA = 0.001f;
    private static final float FROM_X_DETLTA = 0.0f;
    private static final float TO_X_DELTA = 10.0f;
    private static final float FROM_Y_DELTA = 0.0f;
    private static final float TO_Y_DELTA = 20.0f;
    private static final float RELATIVE_FROM_X_DELTA = 0.0f;
    private static final float RELATIVE_TO_X_DELTA = 0.2f;
    private static final float RELATIVE_FROM_Y_DELTA = 0.0f;
    private static final float RELATIVE_TO_Y_DELTA = 0.4f;

    public TranslateAnimationTest() {
        super("com.android.cts.stub", AnimationTestStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructors() {

        // Test with null AttributeSet
        new TranslateAnimation(mActivity, null);

        final XmlResourceParser parser = mActivity.getResources().getAnimation(
                R.anim.anim_translate);
        final AttributeSet attr = Xml.asAttributeSet(parser);
        assertNotNull(attr);
        // Test with real AttributeSet
        new TranslateAnimation(mActivity, attr);

        // Test {@link TranslateAnimation#TranslateAnimation(float, float, float, float)}
        new TranslateAnimation(0.6f, 0.6f, 0.6f, 0.6f);
        // Test negative input values
        new TranslateAnimation(-0.6f, -0.6f, -0.6f, -0.6f);

        // Test {@link TranslateAnimation#TranslateAnimation(int, float, int, float, int, float,
        // int, float)}
        new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.6f, Animation.RELATIVE_TO_SELF, 0.6f,
                Animation.RELATIVE_TO_SELF, 0.6f, Animation.RELATIVE_TO_SELF, 0.6f);
        // Test negative input values
        new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.6f, Animation.RELATIVE_TO_SELF, -0.6f,
                Animation.RELATIVE_TO_SELF, -0.6f, Animation.RELATIVE_TO_SELF, -0.6f);
    }

    public void testApplyTransformation(){
        final View animWindow = mActivity.findViewById(R.id.anim_window);
        final Transformation transformation = new Transformation();
        final MyTranslateAnimation translateAnimation =
                new MyTranslateAnimation(FROM_X_DETLTA, TO_X_DELTA, FROM_Y_DELTA, TO_Y_DELTA);
        translateAnimation.setDuration(DURATION);
        translateAnimation.setInterpolator(new LinearInterpolator());
        assertFalse(translateAnimation.isInitialized());
        translateAnimation.initialize(0, 0, 0, 0);
        assertTrue(translateAnimation.isInitialized());

        AnimationTestUtils.assertRunAnimation(getInstrumentation(), animWindow, translateAnimation);
        final long startTime = translateAnimation.getStartTime();

        float values[] = new float[9];
        // Test applyTransformation() in method getTransformation()
        translateAnimation.getTransformation(startTime, transformation);
        transformation.getMatrix().getValues(values);
        assertEquals(FROM_X_DETLTA, values[Matrix.MTRANS_X], POSITION_DELTA);
        assertEquals(FROM_Y_DELTA, values[Matrix.MTRANS_Y], POSITION_DELTA);

        transformation.clear();
        translateAnimation.getTransformation(startTime + DURATION / 2, transformation);
        transformation.getMatrix().getValues(values);
        assertEquals((TO_X_DELTA + FROM_X_DETLTA) / 2, values[Matrix.MTRANS_X], POSITION_DELTA);
        assertEquals((TO_Y_DELTA + FROM_Y_DELTA) / 2, values[Matrix.MTRANS_Y], POSITION_DELTA);

        transformation.clear();
        translateAnimation.getTransformation(startTime + DURATION, transformation);
        transformation.getMatrix().getValues(values);
        assertEquals(TO_X_DELTA, values[Matrix.MTRANS_X], POSITION_DELTA);
        assertEquals(TO_Y_DELTA, values[Matrix.MTRANS_Y], POSITION_DELTA);

        // Test applyTransformation() directly
        // Test time start
        transformation.clear();
        translateAnimation.applyTransformation(0.0f, transformation);
        transformation.getMatrix().getValues(values);
        assertEquals(FROM_X_DETLTA, values[Matrix.MTRANS_X], POSITION_DELTA);
        assertEquals(FROM_Y_DELTA, values[Matrix.MTRANS_Y], POSITION_DELTA);

        // Test time of middle 0.5
        transformation.clear();
        translateAnimation.applyTransformation(0.5f, transformation);
        transformation.getMatrix().getValues(values);
        assertEquals((TO_X_DELTA + FROM_X_DETLTA) / 2,
                values[Matrix.MTRANS_X], POSITION_DELTA);
        assertEquals((TO_Y_DELTA + FROM_Y_DELTA) / 2,
                values[Matrix.MTRANS_Y], POSITION_DELTA);

        // Test time end
        transformation.clear();
        translateAnimation.applyTransformation(1.0f, transformation);
        transformation.getMatrix().getValues(values);
        assertEquals(TO_X_DELTA, values[Matrix.MTRANS_X], POSITION_DELTA);
        assertEquals(TO_Y_DELTA, values[Matrix.MTRANS_Y], POSITION_DELTA);
    }

    public void testInitialize() {
        final View parent = mActivity.findViewById(R.id.anim_window_parent);
        final View animWindow = mActivity.findViewById(R.id.anim_window);
        final Transformation transformation = new Transformation();
        final int selfWidth = animWindow.getWidth();
        final int selfHeight = animWindow.getHeight();
        final int parentWidth = parent.getWidth();
        final int parentHeight = parent.getHeight();
        final int actualWidth = selfWidth;
        final int actualHeight = parentHeight;
        final TranslateAnimation translateAnimation =
                new TranslateAnimation(Animation.RELATIVE_TO_SELF, RELATIVE_FROM_X_DELTA,
                        Animation.RELATIVE_TO_SELF, RELATIVE_TO_X_DELTA,
                        Animation.RELATIVE_TO_PARENT, RELATIVE_FROM_Y_DELTA,
                        Animation.RELATIVE_TO_PARENT, RELATIVE_TO_Y_DELTA);
        assertFalse(translateAnimation.isInitialized());
        translateAnimation.initialize(selfWidth, selfHeight, parentWidth, parentHeight);
        assertTrue(translateAnimation.isInitialized());
        translateAnimation.setDuration(DURATION);
        translateAnimation.setInterpolator(new LinearInterpolator());

        AnimationTestUtils.assertRunAnimation(getInstrumentation(), animWindow, translateAnimation);
        final long startTime = translateAnimation.getStartTime();

        float values[] = new float[9];
        translateAnimation.getTransformation(startTime, transformation);
        transformation.getMatrix().getValues(values);
        assertEquals(RELATIVE_FROM_X_DELTA * actualWidth, values[Matrix.MTRANS_X], POSITION_DELTA);
        assertEquals(RELATIVE_FROM_Y_DELTA * actualHeight, values[Matrix.MTRANS_Y], POSITION_DELTA);

        transformation.clear();
        translateAnimation.getTransformation(startTime + DURATION / 2, transformation);
        transformation.getMatrix().getValues(values);
        assertEquals(((RELATIVE_TO_X_DELTA + RELATIVE_FROM_X_DELTA) / 2) * actualWidth,
                values[Matrix.MTRANS_X], POSITION_DELTA);
        assertEquals(((RELATIVE_TO_Y_DELTA + RELATIVE_FROM_Y_DELTA) / 2) * actualHeight,
                values[Matrix.MTRANS_Y], POSITION_DELTA);

        transformation.clear();
        translateAnimation.getTransformation(startTime + DURATION, transformation);
        transformation.getMatrix().getValues(values);
        assertEquals(RELATIVE_TO_X_DELTA * actualWidth, values[Matrix.MTRANS_X], POSITION_DELTA);
        assertEquals(RELATIVE_TO_Y_DELTA * actualHeight, values[Matrix.MTRANS_Y], POSITION_DELTA);
    }

    private static class MyTranslateAnimation extends TranslateAnimation {

        public MyTranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta,
                float toYDelta) {
            super(fromXDelta, toXDelta, fromYDelta, toYDelta);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
        }
    }
}
