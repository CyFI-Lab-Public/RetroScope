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

import com.android.cts.stub.R;


import android.content.res.XmlResourceParser;
import android.graphics.Matrix;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;

public class ScaleAnimationTest
        extends ActivityInstrumentationTestCase2<AnimationTestStubActivity> {
    private static long DURATION = 1000;
    private static float DELTA = 0.001f;
    private static float FROM_X = 1.0f;
    private static float TO_X = 0.6f;
    private static float FROM_Y = 3.0f;
    private static float TO_Y = 3.6f;
    private static float PIVOT_X = 0.6f;
    private static float PIVOT_Y = 0.6f;
    private static float MID_X = 0.8f;
    private static float MID_Y = 3.3f;
    private AnimationTestStubActivity mActivity;

    public ScaleAnimationTest() {
        super("com.android.cts.stub", AnimationTestStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructors() {
        final XmlResourceParser parser = mActivity.getResources().getAnimation(
                R.anim.anim_scale);
        final AttributeSet attr = Xml.asAttributeSet(parser);
        assertNotNull(attr);
        new ScaleAnimation(mActivity, attr);

        new ScaleAnimation(FROM_X, TO_X, FROM_Y, TO_Y);

        new ScaleAnimation(FROM_X, TO_X, FROM_Y, TO_Y, Animation.RELATIVE_TO_SELF, PIVOT_X,
                Animation.RELATIVE_TO_SELF, PIVOT_Y);

        new ScaleAnimation(FROM_X, TO_X, FROM_Y, TO_Y, PIVOT_X, PIVOT_Y);
    }

    public void testApplyTransformation() {
        final Transformation transformation = new Transformation();
        transformation.setTransformationType(Transformation.TYPE_MATRIX);

        MyScaleAnimation scaleAnimation = new MyScaleAnimation(FROM_X, TO_X, FROM_Y, TO_Y,
                PIVOT_X, PIVOT_Y);

        float values[] = new float[9];
        scaleAnimation.initialize(50, 50, 100, 100);
        // interpolatedTime == 0.0
        scaleAnimation.applyTransformation(0.0f, transformation);
        transformation.getMatrix().getValues(values);
        assertMatrixValue(FROM_X, FROM_Y, values);
        float trans1X = values[Matrix.MTRANS_X];
        float trans1Y = values[Matrix.MTRANS_Y];
        // interpolatedTime == 0.5 (Default interpolation is linear)
        scaleAnimation.applyTransformation(0.5f, transformation);
        transformation.getMatrix().getValues(values);
        assertMatrixValue(MID_X, MID_Y, values);
        float trans2X = values[Matrix.MTRANS_X];
        float trans2Y = values[Matrix.MTRANS_Y];
        // interpolatedTime == 1.0
        scaleAnimation.applyTransformation(1.0f, transformation);
        transformation.getMatrix().getValues(values);
        assertMatrixValue(TO_X, TO_Y, values);
        float trans3X = values[Matrix.MTRANS_X];
        float trans3Y = values[Matrix.MTRANS_Y];
        // Transaction will be more larger when time goes by
        assertTrue(Math.abs(trans1X) < Math.abs(trans2X));
        assertTrue(Math.abs(trans2X) < Math.abs(trans3X));
        assertTrue(Math.abs(trans1Y) < Math.abs(trans2Y));
        assertTrue(Math.abs(trans2Y) < Math.abs(trans3Y));
    }

    public void testApplyTransformationIndirectly() {
        final View animWindow = mActivity.findViewById(R.id.anim_window);
        final Transformation transformation = new Transformation();
        transformation.setTransformationType(Transformation.TYPE_MATRIX);

        MyScaleAnimation scaleAnimation = new MyScaleAnimation(FROM_X, TO_X, FROM_Y, TO_Y,
                PIVOT_X, PIVOT_Y);
        scaleAnimation.setDuration(DURATION);
        scaleAnimation.initialize(50, 50, 100, 100);
        AnimationTestUtils.assertRunAnimation(getInstrumentation(), animWindow, scaleAnimation);

        float values[] = new float[9];
        long startTime = scaleAnimation.getStartTime();
        // interpolatedTime == startTime
        scaleAnimation.getTransformation(startTime, transformation);
        assertNotNull(scaleAnimation.getInterpolator());
        transformation.getMatrix().getValues(values);
        // The scale value will equal to the set value in scaleAnimation's constructor
        assertMatrixValue(FROM_X, FROM_Y, values);
        float trans1X = values[Matrix.MTRANS_X];
        float trans1Y = values[Matrix.MTRANS_Y];
        // interpolatedTime == startTime + (DURATION / 2)
        scaleAnimation.getTransformation(startTime + (DURATION / 2), transformation);
        transformation.getMatrix().getValues(values);
        assertMatrixValue(MID_X, MID_Y, values);
        float trans2X = values[Matrix.MTRANS_X];
        float trans2Y = values[Matrix.MTRANS_Y];
        // interpolatedTime == startTime + DURATION
        scaleAnimation.getTransformation(startTime + DURATION, transformation);
        transformation.getMatrix().getValues(values);
        assertMatrixValue(TO_X, TO_Y, values);
        float trans3X = values[Matrix.MTRANS_X];
        float trans3Y = values[Matrix.MTRANS_Y];
        // Transaction will be more larger when time goes by
        assertTrue(Math.abs(trans1X) < Math.abs(trans2X));
        assertTrue(Math.abs(trans2X) < Math.abs(trans3X));
        assertTrue(Math.abs(trans1Y) < Math.abs(trans2Y));
        assertTrue(Math.abs(trans2Y) < Math.abs(trans3Y));
    }

    private void assertMatrixValue(float expectedX, float expectedY, float[] values) {
        assertEquals(expectedX, values[Matrix.MSCALE_X], DELTA);
        assertEquals(expectedY, values[Matrix.MSCALE_Y], DELTA);
    }

    private class MyScaleAnimation extends ScaleAnimation {

        public MyScaleAnimation(float fromX, float toX, float fromY, float toY,
                float pivotXValue, float pivotYValue) {
            super(fromX, toX, fromY, toY, pivotXValue, pivotYValue);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
        }
    }
}
