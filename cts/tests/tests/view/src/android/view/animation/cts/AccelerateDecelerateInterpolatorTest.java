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


import android.app.Activity;
import android.content.res.XmlResourceParser;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

public class AccelerateDecelerateInterpolatorTest
        extends ActivityInstrumentationTestCase2<AnimationTestStubActivity> {

    private Activity mActivity;
    private static final float ALPHA_DELTA = 0.001f;
    /** It is defined in R.anim.accelarate_decelerate_alpha */
    private static final long ALPHA_DURATION = 2000;

    public AccelerateDecelerateInterpolatorTest() {
        super("com.android.cts.stub", AnimationTestStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructor() {
        new AccelerateDecelerateInterpolator();

        XmlResourceParser parser = mActivity.getResources().getAnimation(
                R.anim.accelerate_decelerate_alpha);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new AccelerateDecelerateInterpolator(mActivity, attrs);
    }

    public void testAccelerateDecelerateInterpolator() {
        final View animWindow = mActivity.findViewById(R.id.anim_window);

        // XML file of R.anim.accelerate_decelerate_alpha
        // <alpha xmlns:android="http://schemas.android.com/apk/res/android"
        //      android:interpolator="@android:anim/accelerate_decelerate_interpolator"
        //      android:fromAlpha="0.0"
        //      android:toAlpha="1.0"
        //      android:duration="2000" />
        final Animation anim = AnimationUtils.loadAnimation(mActivity,
                R.anim.accelerate_decelerate_alpha);
        assertEquals(ALPHA_DURATION, anim.getDuration());
        assertTrue(anim instanceof AlphaAnimation);
        assertFalse(anim.hasStarted());

        AnimationTestUtils.assertRunAnimation(getInstrumentation(), animWindow, anim);

        Transformation transformation = new Transformation();
        long startTime = anim.getStartTime();
        anim.getTransformation(startTime, transformation);
        float alpha1 = transformation.getAlpha();
        assertEquals(0.0f, alpha1, ALPHA_DELTA);

        anim.getTransformation(startTime + 500, transformation);
        float alpha2 = transformation.getAlpha();

        anim.getTransformation(startTime + 1000, transformation);
        float alpha3 = transformation.getAlpha();

        anim.getTransformation(startTime + 1500, transformation);
        float alpha4 = transformation.getAlpha();

        anim.getTransformation(startTime + ALPHA_DURATION, transformation);
        float alpha5 = transformation.getAlpha();
        assertEquals(1.0f, alpha5, ALPHA_DELTA);

        // check accelerating and decelerating delta alpha
        // starts and ends slowly, accelerates through middle
        float delta1 = alpha2 - alpha1;
        float delta2 = alpha3 - alpha2;
        float delta3 = alpha4 - alpha3;
        float delta4 = alpha5 - alpha4;
        assertTrue(delta1 < delta2);
        assertTrue(delta3 > delta4);
    }

    public void testGetInterpolation() {
        Interpolator interpolator = new AccelerateDecelerateInterpolator();

        float alpha1 = interpolator.getInterpolation(0f);
        float alpha2 = interpolator.getInterpolation(0.25f);
        float alpha3 = interpolator.getInterpolation(0.5f);
        float alpha4 = interpolator.getInterpolation(0.75f);
        float alpha5 = interpolator.getInterpolation(1f);
        // check accelerating and decelerating delta alpha
        // starts and ends slowly, accelerates through middle
        float delta1 = alpha2 - alpha1;
        float delta2 = alpha3 - alpha2;
        float delta3 = alpha4 - alpha3;
        float delta4 = alpha5 - alpha4;
        assertTrue(delta1 < delta2);
        assertTrue(delta3 > delta4);
    }
}
