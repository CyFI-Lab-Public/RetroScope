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


import android.content.Context;
import android.content.res.XmlResourceParser;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.GridLayoutAnimationController;
import android.view.animation.Transformation;
import android.view.animation.GridLayoutAnimationController.AnimationParameters;
import android.widget.AbsListView;
import android.widget.GridView;

public class GridLayoutAnimationControllerTest
    extends ActivityInstrumentationTestCase2<GridLayoutAnimStubActivity> {

    private GridLayoutAnimStubActivity mActivity;
    private Animation mDefaultAnimation;
    private GridLayoutAnimationController mController;
    /** The GridView will be 3*3 */
    private GridView mGridView;
    /** Default delay of GridLayoutAnimationController */
    private static final float DEFAULT_DELAY = 0.5f;
    /** Default max duration of running */
    private static final long DEFAULT_MAX_DURATION = 5000;
    private static final float DELTA = 0.1f;
    private static final int INDEX_OF_CHILD1 = 0;
    private static final int INDEX_OF_CHILD2 = 1;
    private static final int INDEX_OF_CHILD3 = 2;
    private static final int INDEX_OF_CHILD4 = 3;
    private static final int INDEX_OF_CHILD5 = 4;
    private static final int INDEX_OF_CHILD6 = 5;
    private static final int INDEX_OF_CHILD7 = 6;
    private static final int INDEX_OF_CHILD8 = 7;
    private static final int INDEX_OF_CHILD9 = 8;

    public GridLayoutAnimationControllerTest() {
        super("com.android.cts.stub", GridLayoutAnimStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mDefaultAnimation = AnimationUtils.loadAnimation(mActivity,
                R.anim.layout_anim_controller_animation);
        mController = new GridLayoutAnimationController(mDefaultAnimation, DEFAULT_DELAY,
                DEFAULT_DELAY);
        mGridView = mActivity.getGridView();
    }

    public void testConstructor() {
        XmlResourceParser parser = mActivity.getResources().getAnimation(
                R.anim.accelerate_decelerate_alpha);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new GridLayoutAnimationController(mActivity, attrs);
        GridLayoutAnimationController controller =
                new GridLayoutAnimationController(mDefaultAnimation);
        // Default rowDelay and columnDelay is 0.5f
        assertEquals(DEFAULT_DELAY, controller.getRowDelay());
        assertEquals(DEFAULT_DELAY, controller.getColumnDelay());
        new GridLayoutAnimationController(mDefaultAnimation, 0.5f, 0.5f);
    }

    public void testAccessDelay() throws InterruptedException {
        float delay = 1.5f;
        long maxDuration = 13000;
        mController.setRowDelay(delay);
        assertEquals(delay, mController.getRowDelay());
        AnimationTestUtils.assertRunController(getInstrumentation(), mGridView, mController,
                maxDuration);

        Animation childAnimation1 = mGridView.getChildAt(INDEX_OF_CHILD1).getAnimation();
        Animation childAnimation4 = mGridView.getChildAt(INDEX_OF_CHILD4).getAnimation();
        Animation childAnimation7 = mGridView.getChildAt(INDEX_OF_CHILD7).getAnimation();
        assertChildrenDelay(childAnimation1, childAnimation4, childAnimation7);

        mController.setColumnDelay(delay);
        assertEquals(delay, mController.getColumnDelay());
        AnimationTestUtils.assertRunController(getInstrumentation(), mGridView, mController,
                maxDuration);

        childAnimation1 = mGridView.getChildAt(INDEX_OF_CHILD1).getAnimation();
        Animation childAnimation2 = mGridView.getChildAt(INDEX_OF_CHILD2).getAnimation();
        Animation childAnimation3 = mGridView.getChildAt(INDEX_OF_CHILD3).getAnimation();
        assertChildrenDelay(childAnimation1, childAnimation2, childAnimation3);
    }

    /**
     * This method is used to assert child animation's attrs to check setDelay() works.
     * The set delay is 1.5f.
     * @param child1
     * @param child2
     * @param child3
     * @param delay
     */
    private void assertChildrenDelay(Animation child1, Animation child2, Animation child3) {
        long startTime = child1.getStartTime();
        long offsetTime1 = child1.getStartOffset();
        long offsetTime2 = child2.getStartOffset();
        long offsetTime3 = child3.getStartOffset();
        assertEquals(0, offsetTime1);
        assertEquals(1500, offsetTime2);
        assertEquals(3000, offsetTime3);

        Transformation transformation1 = new Transformation();
        Transformation transformation2 = new Transformation();
        Transformation transformation3 = new Transformation();

        // child1 has started animation, child2 and child3 haven't started
        child1.getTransformation(startTime + 500, transformation1);
        child2.getTransformation(startTime + 500, transformation2);
        child3.getTransformation(startTime + 500, transformation3);
        assertIsRunningAnimation(transformation1.getAlpha());
        assertEquals(0.0f, transformation2.getAlpha(), DELTA);
        assertEquals(0.0f, transformation3.getAlpha(), DELTA);

        // child1 has finished, child2 and child3 haven't started
        child1.getTransformation(startTime + 1200, transformation1);
        child2.getTransformation(startTime + 1200, transformation2);
        child3.getTransformation(startTime + 1200, transformation3);
        assertEquals(1.0f, transformation1.getAlpha(), DELTA);
        assertEquals(0.0f, transformation2.getAlpha(), DELTA);
        assertEquals(0.0f, transformation3.getAlpha(), DELTA);

        // child1 has finished, child2 has started animation, child3 hasn't started.
        child1.getTransformation(startTime + 2000, transformation1);
        child2.getTransformation(startTime + 2000, transformation2);
        child3.getTransformation(startTime + 2000, transformation3);
        assertEquals(1.0f, transformation1.getAlpha(), DELTA);
        assertIsRunningAnimation(transformation2.getAlpha());
        assertEquals(0.0f, transformation3.getAlpha(), DELTA);

        // child1 and child2 have finished, child3 hasn't started
        child1.getTransformation(startTime + 2700, transformation1);
        child2.getTransformation(startTime + 2700, transformation2);
        child3.getTransformation(startTime + 2700, transformation3);
        assertEquals(1.0f, transformation1.getAlpha(), DELTA);
        assertEquals(1.0f, transformation2.getAlpha(), DELTA);
        assertEquals(0.0f, transformation3.getAlpha(), DELTA);

        // child1 and child2 have finished, child3 has started animation
        child1.getTransformation(startTime + 3500, transformation1);
        child2.getTransformation(startTime + 3500, transformation2);
        child3.getTransformation(startTime + 3500, transformation3);
        assertIsRunningAnimation(transformation3.getAlpha());
    }

    private void assertIsRunningAnimation(float alpha) {
        assertTrue(alpha > 0.0f);
        assertTrue(alpha < 1.0f);
    }

    public void testAccessDirection() throws InterruptedException {
        mController.setDirection(GridLayoutAnimationController.DIRECTION_BOTTOM_TO_TOP);
        assertEquals(GridLayoutAnimationController.DIRECTION_BOTTOM_TO_TOP,
                mController.getDirection());
        AnimationTestUtils.assertRunController(getInstrumentation(), mGridView, mController,
                DEFAULT_MAX_DURATION);

        Animation childAnimation1 = mGridView.getChildAt(INDEX_OF_CHILD1).getAnimation();
        Animation childAnimation4 = mGridView.getChildAt(INDEX_OF_CHILD4).getAnimation();
        Animation childAnimation7 = mGridView.getChildAt(INDEX_OF_CHILD7).getAnimation();

        // Test bottom_to_top direction
        long startTime = childAnimation7.getStartTime();
        assertEquals(1000, childAnimation1.getStartOffset());
        assertEquals(500, childAnimation4.getStartOffset());
        assertEquals(0, childAnimation7.getStartOffset());

        Transformation transformation1 = new Transformation();
        Transformation transformation2 = new Transformation();
        Transformation transformation3 = new Transformation();

        // childAnimation7 has started animation, childAnimation4 and childAnimation1 haven't
        // started
        childAnimation1.getTransformation(startTime + 500, transformation1);
        childAnimation4.getTransformation(startTime + 500, transformation2);
        childAnimation7.getTransformation(startTime + 500, transformation3);
        assertEquals(0.0f, transformation1.getAlpha(), DELTA);
        assertEquals(0.0f, transformation2.getAlpha(), DELTA);
        assertIsRunningAnimation(transformation3.getAlpha());

        // childAnimation4 has started animation, childAnimation7 has finished and childAnimation1
        // hasn't started
        childAnimation1.getTransformation(startTime + 1000, transformation1);
        childAnimation4.getTransformation(startTime + 1000, transformation2);
        childAnimation7.getTransformation(startTime + 1000, transformation3);
        assertEquals(0.0f, transformation1.getAlpha(), DELTA);
        assertIsRunningAnimation(transformation2.getAlpha());
        assertEquals(1.0f, transformation3.getAlpha(), DELTA);

        // childAnimation1 has started animation, childAnimation4, childAnimation7 have finished
        childAnimation1.getTransformation(startTime + 1500, transformation1);
        childAnimation4.getTransformation(startTime + 1500, transformation2);
        childAnimation7.getTransformation(startTime + 1500, transformation3);
        assertIsRunningAnimation(transformation1.getAlpha());
        assertEquals(1.0f, transformation2.getAlpha(), DELTA);
        assertEquals(1.0f, transformation3.getAlpha(), DELTA);

        // Test top_to_bottom direction
        mController.setDirection(GridLayoutAnimationController.DIRECTION_TOP_TO_BOTTOM);
        assertEquals(GridLayoutAnimationController.DIRECTION_TOP_TO_BOTTOM,
                mController.getDirection());
        AnimationTestUtils.assertRunController(getInstrumentation(), mGridView, mController,
                DEFAULT_MAX_DURATION);

        transformation1 = new Transformation();
        transformation2 = new Transformation();
        transformation3 = new Transformation();
        childAnimation1 = mGridView.getChildAt(INDEX_OF_CHILD1).getAnimation();
        childAnimation4 = mGridView.getChildAt(INDEX_OF_CHILD4).getAnimation();
        childAnimation7 = mGridView.getChildAt(INDEX_OF_CHILD7).getAnimation();
        startTime = childAnimation1.getStartTime();
        assertEquals(0, childAnimation1.getStartOffset());
        assertEquals(500, childAnimation4.getStartOffset());
        assertEquals(1000, childAnimation7.getStartOffset());

        // childAnimation1 has started animation, childAnimation4 and childAnimation7 haven't
        // started
        childAnimation1.getTransformation(startTime + 500, transformation1);
        childAnimation4.getTransformation(startTime + 500, transformation2);
        childAnimation7.getTransformation(startTime + 500, transformation3);
        assertIsRunningAnimation(transformation1.getAlpha());
        assertEquals(0.0f, transformation2.getAlpha(), DELTA);
        assertEquals(0.0f, transformation3.getAlpha(), DELTA);

        // childAnimation4 has started animation, childAnimation1 has finished and childAnimation7
        // hasn't started
        childAnimation1.getTransformation(startTime + 1000, transformation1);
        childAnimation4.getTransformation(startTime + 1000, transformation2);
        childAnimation7.getTransformation(startTime + 1000, transformation3);
        assertEquals(1.0f, transformation1.getAlpha(), DELTA);
        assertIsRunningAnimation(transformation2.getAlpha());
        assertEquals(0.0f, transformation3.getAlpha(), DELTA);

        // childAnimation7 has started animation, childAnimation1 and childAnimation4 has finished
        childAnimation1.getTransformation(startTime + 1500, transformation1);
        childAnimation4.getTransformation(startTime + 1500, transformation2);
        childAnimation7.getTransformation(startTime + 1500, transformation3);
        assertEquals(1.0f, transformation1.getAlpha(), DELTA);
        assertEquals(1.0f, transformation2.getAlpha(), DELTA);
        assertIsRunningAnimation(transformation3.getAlpha());
    }

    public void testGetDelayForView() throws Throwable {
        Animation animation = AnimationUtils.loadAnimation(mActivity, R.anim.decelerate_alpha);
        animation.setFillAfter(true);
        MyGridLayoutAnimationController controller = new MyGridLayoutAnimationController(animation);

        // child1's animationParams
        final AbsListView.LayoutParams layoutParams1 = setAnimationParameters(0);
        // child2's animationParams
        final AbsListView.LayoutParams layoutParams2 = setAnimationParameters(1);
        // child3's animationParams
        final AbsListView.LayoutParams layoutParams3 = setAnimationParameters(2);
        // child4's animationParams
        final AbsListView.LayoutParams layoutParams4 = setAnimationParameters(3);
        // child5's animationParams
        final AbsListView.LayoutParams layoutParams5 = setAnimationParameters(4);
        // child6's animationParams
        final AbsListView.LayoutParams layoutParams6 = setAnimationParameters(5);
        // child7's animationParams
        final AbsListView.LayoutParams layoutParams7 = setAnimationParameters(6);
        // child8's animationParams
        final AbsListView.LayoutParams layoutParams8 = setAnimationParameters(7);
        // child9's animationParams
        final AbsListView.LayoutParams layoutParams9 = setAnimationParameters(8);

        final View child1 = mGridView.getChildAt(INDEX_OF_CHILD1);
        final View child2 = mGridView.getChildAt(INDEX_OF_CHILD2);
        final View child3 = mGridView.getChildAt(INDEX_OF_CHILD3);
        final View child4 = mGridView.getChildAt(INDEX_OF_CHILD4);
        final View child5 = mGridView.getChildAt(INDEX_OF_CHILD5);
        final View child6 = mGridView.getChildAt(INDEX_OF_CHILD6);
        final View child7 = mGridView.getChildAt(INDEX_OF_CHILD7);
        final View child8 = mGridView.getChildAt(INDEX_OF_CHILD8);
        final View child9 = mGridView.getChildAt(INDEX_OF_CHILD9);
        runTestOnUiThread(new Runnable() {
            public void run() {
                child1.setLayoutParams(layoutParams1);
                child2.setLayoutParams(layoutParams2);
                child3.setLayoutParams(layoutParams3);
                child4.setLayoutParams(layoutParams4);
                child5.setLayoutParams(layoutParams5);
                child6.setLayoutParams(layoutParams6);
                child7.setLayoutParams(layoutParams7);
                child8.setLayoutParams(layoutParams8);
                child9.setLayoutParams(layoutParams9);
            }
        });

        AnimationTestUtils.assertRunController(getInstrumentation(), mGridView, controller,
                DEFAULT_MAX_DURATION);

        assertEquals(0, controller.getDelayForView(child1));
        assertEquals(1000, controller.getDelayForView(child2));
        assertEquals(2000, controller.getDelayForView(child3));
        assertEquals(1000, controller.getDelayForView(child4));
        assertEquals(2000, controller.getDelayForView(child5));
        assertEquals(3000, controller.getDelayForView(child6));
        assertEquals(2000, controller.getDelayForView(child7));
        assertEquals(3000, controller.getDelayForView(child8));
        assertEquals(4000, controller.getDelayForView(child9));
    }

    private AbsListView.LayoutParams setAnimationParameters(int index) {
        AnimationParameters animationParams = new AnimationParameters();
        animationParams.index = index;
        animationParams.count = 9;
        final AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layoutParams.layoutAnimationParameters = animationParams;
        return layoutParams;
    }

    public void testAccessDirectionPriority() throws InterruptedException {
        // Before setting DirectionPriority, childAnimation7 will be later than childAnimation2,
        // and childAnimation8 will be later than childAnimation3
        AnimationTestUtils.assertRunController(getInstrumentation(), mGridView, mController,
                DEFAULT_MAX_DURATION);
        Animation childAnimation1 = mGridView.getChildAt(INDEX_OF_CHILD1).getAnimation();
        Animation childAnimation2 = mGridView.getChildAt(INDEX_OF_CHILD2).getAnimation();
        Animation childAnimation3 = mGridView.getChildAt(INDEX_OF_CHILD3).getAnimation();
        Animation childAnimation7 = mGridView.getChildAt(INDEX_OF_CHILD7).getAnimation();
        Animation childAnimation8 = mGridView.getChildAt(INDEX_OF_CHILD8).getAnimation();

        long startTime = childAnimation1.getStartTime();
        assertEquals(500, childAnimation2.getStartOffset());
        assertEquals(1000, childAnimation3.getStartOffset());
        assertEquals(1000, childAnimation7.getStartOffset());
        assertEquals(1500, childAnimation8.getStartOffset());


        Transformation transformation1 = new Transformation();
        Transformation transformation2 = new Transformation();

        childAnimation2.getTransformation(startTime + 700, transformation1);
        childAnimation7.getTransformation(startTime + 700, transformation2);
        assertIsRunningAnimation(transformation1.getAlpha());
        assertEquals(0.0f, transformation2.getAlpha(), DELTA);

        childAnimation3.getTransformation(startTime + 1200, transformation1);
        childAnimation8.getTransformation(startTime + 1200, transformation2);
        assertIsRunningAnimation(transformation1.getAlpha());
        assertEquals(0.0f, transformation2.getAlpha(), DELTA);

        // After setting DirectionPriority to PRIORITY_COLUMN, childAnimation7 will be earlier
        // than childAnimation2, and childAnimation8 will be earlier than childAnimation3
        mController.setDirectionPriority(GridLayoutAnimationController.PRIORITY_COLUMN);
        assertEquals(GridLayoutAnimationController.PRIORITY_COLUMN,
                mController.getDirectionPriority());
        AnimationTestUtils.assertRunController(getInstrumentation(), mGridView, mController,
                DEFAULT_MAX_DURATION);
        childAnimation1 = mGridView.getChildAt(INDEX_OF_CHILD1).getAnimation();
        childAnimation2 = mGridView.getChildAt(INDEX_OF_CHILD2).getAnimation();
        childAnimation3 = mGridView.getChildAt(INDEX_OF_CHILD3).getAnimation();
        childAnimation7 = mGridView.getChildAt(INDEX_OF_CHILD7).getAnimation();
        childAnimation8 = mGridView.getChildAt(INDEX_OF_CHILD8).getAnimation();

        startTime = childAnimation1.getStartTime();
        assertEquals(1500, childAnimation2.getStartOffset());
        assertEquals(3000, childAnimation3.getStartOffset());
        assertEquals(1000, childAnimation7.getStartOffset());
        assertEquals(2500, childAnimation8.getStartOffset());

        transformation1 = new Transformation();
        transformation2 = new Transformation();

        childAnimation2.getTransformation(startTime + 1200, transformation1);
        childAnimation7.getTransformation(startTime + 1200, transformation2);
        assertEquals(0.0f, transformation1.getAlpha(), DELTA);
        assertIsRunningAnimation(transformation2.getAlpha());

        childAnimation3.getTransformation(startTime + 2700, transformation1);
        childAnimation8.getTransformation(startTime + 2700, transformation2);
        assertEquals(0.0f, transformation1.getAlpha(), DELTA);
        assertIsRunningAnimation(transformation2.getAlpha());
    }

    public void testWillOverlap() {
        GridLayoutAnimationController controller = new GridLayoutAnimationController(
                mDefaultAnimation);

        controller.setColumnDelay(0.5f);
        controller.setRowDelay(0.5f);
        assertTrue(controller.willOverlap());

        controller.setColumnDelay(0.5f);
        controller.setRowDelay(1.0f);
        assertTrue(controller.willOverlap());

        controller.setColumnDelay(1.0f);
        controller.setRowDelay(0.5f);
        assertTrue(controller.willOverlap());

        controller.setColumnDelay(1.0f);
        controller.setRowDelay(1.0f);
        assertFalse(controller.willOverlap());
    }

    private class MyGridLayoutAnimationController extends GridLayoutAnimationController {
        public MyGridLayoutAnimationController(Animation animation) {
            super(animation);
        }

        protected long getDelayForView(View view) {
            return super.getDelayForView(view);
        }
    }
}
