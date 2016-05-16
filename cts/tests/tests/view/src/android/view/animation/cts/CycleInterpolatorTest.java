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
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.CycleInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

import com.android.cts.stub.R;


/**
 * Test {@link CycleInterpolator}.
 */
public class CycleInterpolatorTest
        extends ActivityInstrumentationTestCase2<AnimationTestStubActivity> {

    private Activity mActivity;

    /** It is defined in R.anim.cycle_alpha */
    private static final long CYCLE_ALPHA_DURATION = 2000;
    private static final float ALPHA_DELTA = 0.001f;

    public CycleInterpolatorTest() {
        super("com.android.cts.stub", AnimationTestStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructors() {
        new CycleInterpolator(1.0f);

        XmlResourceParser parser = mActivity.getResources().getAnimation(R.anim.cycle_alpha);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new CycleInterpolator(mActivity, attrs);
    }

    public void testCycyleInterpolator() {
        final View animWindow = mActivity.findViewById(R.id.anim_window);
        final Animation anim = AnimationUtils.loadAnimation(mActivity, R.anim.cycle_alpha);
        assertEquals(CYCLE_ALPHA_DURATION, anim.getDuration());
        assertTrue(anim instanceof AlphaAnimation);

        // cycle only once.
        Interpolator interpolator = new CycleInterpolator(1.0f);
        anim.setInterpolator(interpolator);
        assertFalse(anim.hasStarted());

        AnimationTestUtils.assertRunAnimation(getInstrumentation(), animWindow, anim);

        Transformation transformation = new Transformation();
        long startTime = anim.getStartTime();
        anim.getTransformation(startTime, transformation);
        final float alpha1 = transformation.getAlpha();
        assertEquals(0.0f, alpha1, ALPHA_DELTA);

        anim.getTransformation(startTime + 500, transformation);
        final float alpha2 = transformation.getAlpha();
        assertEquals(1.0f, alpha2, ALPHA_DELTA);

        anim.getTransformation(startTime + 1000, transformation);
        final float alpha3 = transformation.getAlpha();

        anim.getTransformation(startTime + 1500, transformation);
        final float alpha4 = transformation.getAlpha();
        assertEquals(-1.0f, alpha4, ALPHA_DELTA);

        anim.getTransformation(startTime + CYCLE_ALPHA_DURATION, transformation);
        final float alpha5 = transformation.getAlpha();
        assertEquals(0.0f, alpha5, ALPHA_DELTA);

        // check cycle sinusoidal delta alpha
        final float delta1 = alpha2 - alpha1;
        final float delta2 = alpha3 - alpha2;
        final float delta3 = alpha4 - alpha3;
        final float delta4 = alpha5 - alpha4;
        assertEquals(alpha1, alpha3, ALPHA_DELTA);
        assertEquals(alpha3, alpha5, ALPHA_DELTA);
        assertEquals(alpha2, -alpha4, ALPHA_DELTA);
        assertEquals(delta1, -delta2, ALPHA_DELTA);
        assertEquals(delta2, delta3, ALPHA_DELTA);
        assertEquals(delta3, -delta4, ALPHA_DELTA);

        // cycle twice.
        interpolator = new CycleInterpolator(2.0f);
        anim.setInterpolator(interpolator);

        AnimationTestUtils.assertRunAnimation(getInstrumentation(), animWindow, anim);

        transformation = new Transformation();
        startTime = anim.getStartTime();
        anim.getTransformation(startTime, transformation);
        final float alpha6 = transformation.getAlpha();
        assertEquals(0.0f, alpha1, ALPHA_DELTA);

        anim.getTransformation(startTime + 250, transformation);
        final float alpha7 = transformation.getAlpha();
        assertEquals(1.0f, alpha7, ALPHA_DELTA);

        anim.getTransformation(startTime + 500, transformation);
        final float alpha8 = transformation.getAlpha();

        anim.getTransformation(startTime + 750, transformation);
        final float alpha9 = transformation.getAlpha();
        assertEquals(-1.0f, alpha9, ALPHA_DELTA);

        anim.getTransformation(startTime + 1000, transformation);
        final float alpha10 = transformation.getAlpha();

        anim.getTransformation(startTime + 1250, transformation);
        final float alpha11 = transformation.getAlpha();

        anim.getTransformation(startTime + 1500, transformation);
        final float alpha12 = transformation.getAlpha();

        anim.getTransformation(startTime + 1750, transformation);
        final float alpha13 = transformation.getAlpha();

        anim.getTransformation(startTime + CYCLE_ALPHA_DURATION, transformation);
        final float alpha14 = transformation.getAlpha();
        assertEquals(0.0f, alpha14, ALPHA_DELTA);

        // check cycle sinusoidal delta alpha
        final float delta5 = alpha7 - alpha6;
        final float delta6 = alpha8 - alpha7;
        final float delta7 = alpha9 - alpha8;
        final float delta8 = alpha10 - alpha9;
        final float delta9 = alpha11 - alpha10;
        final float delta10 = alpha12 - alpha11;
        final float delta11 = alpha13 - alpha12;
        final float delta12 = alpha14 - alpha13;
        assertEquals(alpha6, alpha8, ALPHA_DELTA);
        assertEquals(alpha8, alpha10, ALPHA_DELTA);
        assertEquals(alpha10, alpha12, ALPHA_DELTA);
        assertEquals(alpha12, alpha14, ALPHA_DELTA);
        assertEquals(alpha7, -alpha9, ALPHA_DELTA);
        assertEquals(alpha9, -alpha11, ALPHA_DELTA);
        assertEquals(alpha11, -alpha13, ALPHA_DELTA);
        assertEquals(delta6, -delta5, ALPHA_DELTA);
        assertEquals(delta7, delta6, ALPHA_DELTA);
        assertEquals(delta8, -delta7, ALPHA_DELTA);
        assertEquals(delta9, delta8, ALPHA_DELTA);
        assertEquals(delta10, -delta9, ALPHA_DELTA);
        assertEquals(delta11, delta10, ALPHA_DELTA);
        assertEquals(delta12, -delta11, ALPHA_DELTA);

        // check whether it cycle the same
        assertEquals(alpha6, alpha1, ALPHA_DELTA);
        assertEquals(alpha7, alpha2, ALPHA_DELTA);
        assertEquals(alpha8, alpha3, ALPHA_DELTA);
        assertEquals(alpha9, alpha4, ALPHA_DELTA);
        assertEquals(alpha10, alpha5, ALPHA_DELTA);
        assertEquals(alpha10, alpha1, ALPHA_DELTA);
        assertEquals(alpha11, alpha2, ALPHA_DELTA);
        assertEquals(alpha12, alpha3, ALPHA_DELTA);
        assertEquals(alpha13, alpha4, ALPHA_DELTA);
        assertEquals(alpha14, alpha5, ALPHA_DELTA);
        assertEquals(delta5, delta1, ALPHA_DELTA);
        assertEquals(delta6, delta2, ALPHA_DELTA);
        assertEquals(delta7, delta3, ALPHA_DELTA);
        assertEquals(delta8, delta4, ALPHA_DELTA);
        assertEquals(delta9, delta1, ALPHA_DELTA);
        assertEquals(delta10, delta2, ALPHA_DELTA);
        assertEquals(delta11, delta3, ALPHA_DELTA);
        assertEquals(delta12, delta4, ALPHA_DELTA);
    }

    public void testGetInterpolation() {
        CycleInterpolator cycleInterpolator = new CycleInterpolator(2.0f);
        final float out1 = cycleInterpolator.getInterpolation(0.0f);
        final float out2 = cycleInterpolator.getInterpolation(0.125f);
        final float out3 = cycleInterpolator.getInterpolation(0.25f);
        final float out4 = cycleInterpolator.getInterpolation(0.375f);
        final float out5 = cycleInterpolator.getInterpolation(0.5f);
        final float out6 = cycleInterpolator.getInterpolation(0.625f);
        final float out7 = cycleInterpolator.getInterpolation(0.75f);
        final float out8 = cycleInterpolator.getInterpolation(0.875f);
        final float out9 = cycleInterpolator.getInterpolation(1.0f);
        final float delta1 = out2 - out1;
        final float delta2 = out3 - out2;
        final float delta3 = out4 - out3;
        final float delta4 = out5 - out4;
        final float delta5 = out6 - out5;
        final float delta6 = out7 - out6;
        final float delta7 = out8 - out7;
        final float delta8 = out9 - out8;
        assertEquals(out1, out3, ALPHA_DELTA);
        assertEquals(out3, out5, ALPHA_DELTA);
        assertEquals(out5, out7, ALPHA_DELTA);
        assertEquals(out7, out9, ALPHA_DELTA);
        assertEquals(delta1, -delta2, ALPHA_DELTA);
        assertEquals(delta2, delta3, ALPHA_DELTA);
        assertEquals(delta3, -delta4, ALPHA_DELTA);
        assertEquals(delta4, delta5, ALPHA_DELTA);
        assertEquals(delta5, -delta6, ALPHA_DELTA);
        assertEquals(delta6, delta7, ALPHA_DELTA);
        assertEquals(delta7, -delta8, ALPHA_DELTA);

        final float timeDelta = 0.015f;
        assertEquals(cycleInterpolator.getInterpolation(0.0f + timeDelta),
                cycleInterpolator.getInterpolation(0.5f + timeDelta), ALPHA_DELTA);
        assertEquals(cycleInterpolator.getInterpolation(0.25f + timeDelta),
                cycleInterpolator.getInterpolation(0.75f + timeDelta), ALPHA_DELTA);
    }
}
