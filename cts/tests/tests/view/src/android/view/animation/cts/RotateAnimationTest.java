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
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;

import com.android.cts.stub.R;


public class RotateAnimationTest
        extends ActivityInstrumentationTestCase2<AnimationTestStubActivity> {

    private Activity mActivity;

    private static final long DURATION = 1000;
    private static final float ROTATE_DELTA = 0.001f;
    private static final float FROM_DEGREE = 0.0f;
    private static final float TO_DEGREE = 90.0f;

    public RotateAnimationTest() {
        super("com.android.cts.stub", AnimationTestStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructors() {

        // Test with null AttributeSet
        new RotateAnimation(mActivity, null);

        final XmlResourceParser parser = mActivity.getResources().getAnimation(
                R.anim.anim_rotate);
        final AttributeSet attr = Xml.asAttributeSet(parser);
        assertNotNull(attr);
        // Test with real AttributeSet
        new RotateAnimation(mActivity, attr);

        // Test {@link RotateAnimation#RotateAnimation(float, float)}
        new RotateAnimation(0.6f, 0.6f);
        // Test negative input values
        new RotateAnimation(-0.6f, -0.6f);

        // Test {@link RotateAnimation#RotateAnimation(float, float, float, float)}
        new RotateAnimation(0.6f, 0.6f, 0.6f, 0.6f);
        // Test negative input values
        new RotateAnimation(-0.6f, -0.6f, -0.6f, -0.6f);

        // Test {@link RotateAnimation#RotateAnimation(float, float, int, float, int, float)}
        new RotateAnimation(0.6f, 0.6f, Animation.ABSOLUTE, 0.6f, Animation.ABSOLUTE, 0.6f);
        // Test negative input values
        new RotateAnimation(-0.6f, -0.6f, Animation.ABSOLUTE, -0.6f, Animation.ABSOLUTE, -0.6f);
    }

    public void testRotateAgainstOrigin(){
        final View animWindowParent = mActivity.findViewById(R.id.anim_window_parent);
        final View animWindow = mActivity.findViewById(R.id.anim_window);
        Transformation transformation = new Transformation();
        // Test when mPivot x and y equal to 0.
        MyRotateAnimation rotateAnimation = new MyRotateAnimation(FROM_DEGREE, TO_DEGREE);
        rotateAnimation.setDuration(DURATION);
        rotateAnimation.setInterpolator(new LinearInterpolator());

        assertFalse(rotateAnimation.isInitialized());
        rotateAnimation.initialize(animWindow.getWidth(), animWindow.getHeight(),
                animWindowParent.getWidth(), animWindowParent.getHeight());
        assertTrue(rotateAnimation.isInitialized());

        AnimationTestUtils.assertRunAnimation(getInstrumentation(), animWindow, rotateAnimation);
        final long startTime = rotateAnimation.getStartTime();

        Matrix expectedMatrix = new Matrix();
        expectedMatrix.setRotate(FROM_DEGREE);
        rotateAnimation.getTransformation(startTime, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());
        transformation.clear();
        rotateAnimation.applyTransformation(0.0f, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());

        expectedMatrix.reset();
        expectedMatrix.setRotate((FROM_DEGREE + TO_DEGREE) / 2);
        rotateAnimation.getTransformation(startTime + DURATION / 2, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());
        transformation.clear();
        rotateAnimation.applyTransformation(0.5f, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());

        expectedMatrix.reset();
        expectedMatrix.setRotate(TO_DEGREE);
        rotateAnimation.getTransformation(startTime + DURATION, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());
        rotateAnimation.applyTransformation(1.0f, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());
    }

    private void assertMatrixEquals(Matrix expectedMatrix, Matrix actualMatrix) {
        final float[] expectedMatrixValues = new float[9];
        final float[] actualMatrixValues = new float[9];
        expectedMatrix.getValues(expectedMatrixValues);
        actualMatrix.getValues(actualMatrixValues);
        for (int i = 0; i < 9; i++) {
            assertEquals(expectedMatrixValues[i], actualMatrixValues[i], ROTATE_DELTA);
        }
    }

    public void testRotateAgainstPoint(){
        final View animWindowParent = mActivity.findViewById(R.id.anim_window_parent);
        final View animWindow = mActivity.findViewById(R.id.anim_window);
        Transformation transformation = new Transformation();
        final float pivotX = 0.2f;
        final float pivotY = 0.2f;
        final float actualPivotX = pivotX * animWindowParent.getWidth();
        final float actualPivotY = pivotY * animWindow.getHeight();
        // Test when mPivot x and y are not origin
        MyRotateAnimation rotateAnimation = new MyRotateAnimation(FROM_DEGREE, TO_DEGREE,
                    Animation.RELATIVE_TO_PARENT, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
        rotateAnimation.setDuration(DURATION);
        rotateAnimation.setInterpolator(new LinearInterpolator());

        assertFalse(rotateAnimation.isInitialized());
        rotateAnimation.initialize(animWindow.getWidth(), animWindow.getHeight(),
                animWindowParent.getWidth(), animWindowParent.getHeight());
        assertTrue(rotateAnimation.isInitialized());

        AnimationTestUtils.assertRunAnimation(getInstrumentation(), animWindow, rotateAnimation);
        final long startTime = rotateAnimation.getStartTime();

        Matrix expectedMatrix = new Matrix();
        expectedMatrix.setRotate(FROM_DEGREE, actualPivotX, actualPivotY);
        rotateAnimation.getTransformation(startTime, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());
        transformation.clear();
        rotateAnimation.applyTransformation(0.0f, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());

        expectedMatrix.reset();
        expectedMatrix.setRotate((FROM_DEGREE + TO_DEGREE) / 2, actualPivotX, actualPivotY);
        rotateAnimation.getTransformation(startTime + DURATION / 2, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());
        transformation.clear();
        rotateAnimation.applyTransformation(0.5f, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());

        expectedMatrix.reset();
        expectedMatrix.setRotate(TO_DEGREE, actualPivotX, actualPivotY);
        rotateAnimation.getTransformation(startTime + DURATION, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());
        transformation.clear();
        rotateAnimation.applyTransformation(1.0f, transformation);
        assertMatrixEquals(expectedMatrix, transformation.getMatrix());
    }

    private static class MyRotateAnimation extends RotateAnimation {

        public MyRotateAnimation(float fromDegrees, float toDegrees) {
            super(fromDegrees, toDegrees);
        }

        public MyRotateAnimation(float fromDegrees, float toDegrees, float pivotX, float pivotY) {
            super(fromDegrees, toDegrees, pivotX, pivotY);
        }

        public MyRotateAnimation(float fromDegrees, float toDegrees, int pivotXType,
                float pivotX, int pivotYType, float pivotY) {
            super(fromDegrees, toDegrees, pivotXType, pivotX, pivotYType, pivotY);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
        }
    }
}
