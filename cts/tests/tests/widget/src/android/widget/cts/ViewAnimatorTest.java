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

package android.widget.cts;

import com.android.cts.stub.R;


import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.RelativeLayout;
import android.widget.ViewAnimator;

public class ViewAnimatorTest extends
        ActivityInstrumentationTestCase2<ViewAnimatorStubActivity> {
    private ViewAnimator mViewAnimator;
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private AttributeSet mAttributeSet;

    public ViewAnimatorTest() {
        super("com.android.cts.stub", ViewAnimatorStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        mInstrumentation = getInstrumentation();

        XmlPullParser parser = mActivity.getResources().getXml(R.layout.viewanimator_layout);
        mAttributeSet = Xml.asAttributeSet(parser);
        mViewAnimator = new ViewAnimator(mActivity, mAttributeSet);

        assertNotNull(mActivity);
        assertNotNull(mInstrumentation);
        assertNotNull(mViewAnimator);
    }

    public void testConstructor() {
        new ViewAnimator(mActivity);
        new ViewAnimator(mActivity, mAttributeSet);
    }

    public void testAccessInAnimation() {
        AnimationSet expected = new AnimationSet(mActivity, mAttributeSet);
        assertNull(mViewAnimator.getInAnimation());

        mViewAnimator.setInAnimation(expected);
        assertSame(expected, mViewAnimator.getInAnimation());

        // input null as param
        mViewAnimator.setInAnimation(null);
        assertNull(mViewAnimator.getInAnimation());

        mViewAnimator.setInAnimation(mActivity, R.anim.anim_alpha);
        Animation animation = mViewAnimator.getInAnimation();
        assertTrue(animation.getInterpolator() instanceof AccelerateInterpolator);
        assertEquals(500, animation.getDuration());
    }

    @UiThreadTest
    public void testShowNext() {
        final View v1 = mActivity.findViewById(R.id.ok);
        final View v2 = mActivity.findViewById(R.id.cancel);
        final View v3 = mActivity.findViewById(R.id.label);
        final View v4 = mActivity.findViewById(R.id.entry);
        final RelativeLayout parent = (RelativeLayout) v1.getParent();

        parent.removeView(v1);
        parent.removeView(v2);
        parent.removeView(v3);
        parent.removeView(v4);
        assertEquals(0, mViewAnimator.getChildCount());

        mViewAnimator.addView(v1);
        mViewAnimator.addView(v2);
        mViewAnimator.addView(v3);
        mViewAnimator.addView(v4);
        assertEquals(4, mViewAnimator.getChildCount());

        int current = 0;

        mViewAnimator.setDisplayedChild(current);
        assertEquals(current, mViewAnimator.getDisplayedChild());

        mViewAnimator.showNext();
        assertEquals(1, mViewAnimator.getDisplayedChild());

        mViewAnimator.showNext();
        assertEquals(2, mViewAnimator.getDisplayedChild());

        mViewAnimator.showNext();
        assertEquals(3, mViewAnimator.getDisplayedChild());

        mViewAnimator.removeAllViews();
        assertEquals(0, mViewAnimator.getChildCount());
    }

    public void testSetAnimateFirstView() {
        mViewAnimator.setAnimateFirstView(true);
        mViewAnimator.setAnimateFirstView(false);

        // TODO: how to check
    }

    @UiThreadTest
    public void testAccessDisplayedChild() {
        final View v1 = mActivity.findViewById(R.id.ok);
        final View v2 = mActivity.findViewById(R.id.cancel);
        final RelativeLayout parent = (RelativeLayout) v1.getParent();

        parent.removeView(v1);
        parent.removeView(v2);
        assertEquals(0, mViewAnimator.getChildCount());

        mViewAnimator.addView(v1);
        assertEquals(1, mViewAnimator.getChildCount());

        mViewAnimator.addView(v2);
        assertEquals(2, mViewAnimator.getChildCount());

        mViewAnimator.setDisplayedChild(0);
        assertEquals(0, mViewAnimator.getDisplayedChild());

        // set a negative value, then switch to getChildCount()-1.
        mViewAnimator.setDisplayedChild(-1);
        assertEquals(1, mViewAnimator.getDisplayedChild());

        // set larger than ChildCount, then switch to 0.
        mViewAnimator.setDisplayedChild(2);
        assertEquals(0, mViewAnimator.getDisplayedChild());

        mViewAnimator.setDisplayedChild(1);
        assertEquals(1, mViewAnimator.getDisplayedChild());

        mViewAnimator.removeAllViews();
        assertEquals(0, mViewAnimator.getChildCount());
    }

    @UiThreadTest
    public void testAccessDisplayedChildBoundary() {
        final View v1 = mActivity.findViewById(R.id.ok);
        final View v2 = mActivity.findViewById(R.id.cancel);
        final RelativeLayout parent = (RelativeLayout) v1.getParent();

        parent.removeView(v1);
        parent.removeView(v2);
        assertEquals(0, mViewAnimator.getChildCount());

        mViewAnimator.addView(v1);
        assertEquals(1, mViewAnimator.getChildCount());

        mViewAnimator.addView(v2);
        assertEquals(2, mViewAnimator.getChildCount());

        int index = -1;
        mViewAnimator.setDisplayedChild(index);
        assertEquals(1, mViewAnimator.getDisplayedChild());

        index = 2;
        mViewAnimator.setDisplayedChild(index);
        assertEquals(0, mViewAnimator.getDisplayedChild());

        mViewAnimator.removeAllViews();
        assertEquals(0, mViewAnimator.getChildCount());
    }

    @UiThreadTest
    public void testGetBaseline() {
        final View v1 = mActivity.findViewById(R.id.ok);
        final View v2 = mActivity.findViewById(R.id.cancel);
        final RelativeLayout parent = (RelativeLayout) v1.getParent();

        parent.removeView(v1);
        parent.removeView(v2);
        assertEquals(0, mViewAnimator.getChildCount());

        mViewAnimator.addView(v1);
        mViewAnimator.addView(v2);
        assertEquals(2, mViewAnimator.getChildCount());

        int expected = v1.getBaseline();
        mViewAnimator.setDisplayedChild(0);
        assertEquals(expected, mViewAnimator.getBaseline());

        expected = v2.getBaseline();
        mViewAnimator.setDisplayedChild(1);
        assertEquals(expected, mViewAnimator.getBaseline());

        mViewAnimator.removeAllViews();
        assertEquals(0, mViewAnimator.getChildCount());
    }

    @UiThreadTest
    public void testShowPrevious() {
        final View v1 = mActivity.findViewById(R.id.ok);
        final View v2 = mActivity.findViewById(R.id.cancel);
        final View v3 = mActivity.findViewById(R.id.label);
        final View v4 = mActivity.findViewById(R.id.entry);
        final RelativeLayout parent = (RelativeLayout) v1.getParent();

        parent.removeView(v1);
        parent.removeView(v2);
        parent.removeView(v3);
        parent.removeView(v4);
        assertEquals(0, mViewAnimator.getChildCount());

        mViewAnimator.addView(v1);
        mViewAnimator.addView(v2);
        mViewAnimator.addView(v3);
        mViewAnimator.addView(v4);
        assertEquals(4, mViewAnimator.getChildCount());

        int current = 3;

        // set DisplayedChild by {@link mViewAnimator#setDisplayedChild(int)}
        mViewAnimator.setDisplayedChild(current);
        assertEquals(current, mViewAnimator.getDisplayedChild());

        mViewAnimator.showPrevious();
        assertEquals(2, mViewAnimator.getDisplayedChild());

        mViewAnimator.showPrevious();
        assertEquals(1, mViewAnimator.getDisplayedChild());

        mViewAnimator.showPrevious();
        assertEquals(0, mViewAnimator.getDisplayedChild());

        mViewAnimator.removeAllViews();
        assertEquals(0, mViewAnimator.getChildCount());
    }

    @UiThreadTest
    public void testGetCurrentView() {
        final View v = mActivity.findViewById(R.id.label);
        final RelativeLayout parent = (RelativeLayout) v.getParent();

        parent.removeView(v);
        assertEquals(0, mViewAnimator.getChildCount());

        mViewAnimator.addView(v);
        assertEquals(1, mViewAnimator.getChildCount());

        int current = 0;
        mViewAnimator.setDisplayedChild(current);
        assertSame(v, mViewAnimator.getCurrentView());

        mViewAnimator.removeAllViews();
        assertEquals(0, mViewAnimator.getChildCount());
    }

    @UiThreadTest
    public void testAddView() {
        final View v1 = mActivity.findViewById(R.id.ok);
        final View v2 = mActivity.findViewById(R.id.cancel);
        final RelativeLayout parent = (RelativeLayout) v1.getParent();

        parent.removeView(v1);
        parent.removeView(v2);
        assertEquals(0, mViewAnimator.getChildCount());

        LayoutParams p =
            new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        mViewAnimator.addView(v1, 0, p);
        assertEquals(1, mViewAnimator.getChildCount());
        assertEquals(0, mViewAnimator.indexOfChild(v1));

        mViewAnimator.addView(v2, 1, p);
        assertEquals(2, mViewAnimator.getChildCount());
        assertEquals(1, mViewAnimator.indexOfChild(v2));

        mViewAnimator.removeAllViews();
        assertEquals(0, mViewAnimator.getChildCount());
    }

    public void testAccessOutAnimation() {
        AnimationSet expected = new AnimationSet(mActivity, mAttributeSet);
        assertNull(mViewAnimator.getOutAnimation());

        mViewAnimator.setOutAnimation(expected);
        assertSame(expected, mViewAnimator.getOutAnimation());

        mViewAnimator.setOutAnimation(null);
        assertNull(mViewAnimator.getOutAnimation());

        mViewAnimator.setOutAnimation(mActivity, R.anim.anim_alpha);
        Animation animation = mViewAnimator.getOutAnimation();
        assertTrue(animation.getInterpolator() instanceof AccelerateInterpolator);
        assertEquals(500, animation.getDuration());
    }

    @UiThreadTest
    public void testRemoveViews() {
        final View v1 = mActivity.findViewById(R.id.ok);
        final View v2 = mActivity.findViewById(R.id.cancel);
        final View v3 = mActivity.findViewById(R.id.label);
        final View v4 = mActivity.findViewById(R.id.entry);
        final RelativeLayout parent = (RelativeLayout) v1.getParent();

        parent.removeView(v1);
        parent.removeView(v2);
        parent.removeView(v3);
        parent.removeView(v4);
        assertEquals(0, mViewAnimator.getChildCount());

        mViewAnimator.addView(v1);
        mViewAnimator.addView(v2);
        mViewAnimator.addView(v3);
        mViewAnimator.addView(v4);
        assertEquals(4, mViewAnimator.getChildCount());

        mViewAnimator.removeViewAt(3);
        assertEquals(3, mViewAnimator.getChildCount());
        assertSame(v1, mViewAnimator.getChildAt(0));
        assertSame(v2, mViewAnimator.getChildAt(1));
        assertSame(v3, mViewAnimator.getChildAt(2));

        mViewAnimator.removeView(v3);
        assertEquals(2, mViewAnimator.getChildCount());
        assertSame(v1, mViewAnimator.getChildAt(0));
        assertSame(v2, mViewAnimator.getChildAt(1));

        mViewAnimator.removeViewInLayout(v2);
        assertEquals(1, mViewAnimator.getChildCount());
        assertSame(v1, mViewAnimator.getChildAt(0));

        mViewAnimator.addView(v2);
        mViewAnimator.addView(v3);
        mViewAnimator.addView(v4);
        assertEquals(4, mViewAnimator.getChildCount());

        mViewAnimator.removeViews(0, 2);
        assertEquals(2, mViewAnimator.getChildCount());

        // the remain views should be v3, v4
        assertSame(v3, mViewAnimator.getChildAt(0));
        assertSame(v4, mViewAnimator.getChildAt(1));

        mViewAnimator.removeViewsInLayout(1, 1);
        assertEquals(1, mViewAnimator.getChildCount());

        // the remain view should be v3
        assertSame(v3, mViewAnimator.getChildAt(0));

        mViewAnimator.removeAllViews();
        assertEquals(0, mViewAnimator.getChildCount());
    }
}
