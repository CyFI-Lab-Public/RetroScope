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
import com.android.internal.util.XmlUtils;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ViewAsserts;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.RelativeLayout;

import java.io.IOException;

/**
 * Test {@link RelativeLayout}.
 */
public class RelativeLayoutTest extends
        ActivityInstrumentationTestCase2<RelativeLayoutStubActivity> {
    private Activity mActivity;

    public RelativeLayoutTest() {
        super("com.android.cts.stub", RelativeLayoutStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructor() {
        new RelativeLayout(mActivity);

        new RelativeLayout(mActivity, null);

        new RelativeLayout(mActivity, null, 0);

        XmlPullParser parser = mActivity.getResources().getXml(R.layout.relative_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new RelativeLayout(mActivity, attrs);

        try {
            new RelativeLayout(null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testSetIgnoreGravity() {
        // Initial gravity for this RelativeLayout is Gravity.Right.
        final RelativeLayout relativeLayout = (RelativeLayout) mActivity.findViewById(
                R.id.relative_sublayout_ignore_gravity);

        View view12 = mActivity.findViewById(R.id.relative_view12);
        View view13 = mActivity.findViewById(R.id.relative_view13);

        // set in xml, android:ignoreGravity="@id/relative_view12"
        ViewAsserts.assertLeftAligned(relativeLayout, view12);
        ViewAsserts.assertRightAligned(relativeLayout, view13);

        relativeLayout.setIgnoreGravity(R.id.relative_view13);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                relativeLayout.requestLayout();
            }
        });
        getInstrumentation().waitForIdleSync();
        ViewAsserts.assertRightAligned(relativeLayout, view12);
        ViewAsserts.assertLeftAligned(relativeLayout, view13);

        relativeLayout.setIgnoreGravity(0);
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                relativeLayout.requestLayout();
            }
        });
        getInstrumentation().waitForIdleSync();
        ViewAsserts.assertRightAligned(relativeLayout, view12);
        ViewAsserts.assertRightAligned(relativeLayout, view13);
    }

    public void testSetGravity() {
        final RelativeLayout relativeLayout = (RelativeLayout) mActivity.findViewById(
                R.id.relative_sublayout_gravity);

        View view10 = mActivity.findViewById(R.id.relative_view10);
        View view11 = mActivity.findViewById(R.id.relative_view11);

        // Default: -- LEFT & TOP
        ViewAsserts.assertLeftAligned(relativeLayout, view10);
        ViewAsserts.assertTopAligned(relativeLayout, view10);
        ViewAsserts.assertLeftAligned(relativeLayout, view11);
        assertEquals(view11.getTop(), view10.getBottom());

        // -- BOTTOM && RIGHT
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                relativeLayout.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
            }
        });
        getInstrumentation().waitForIdleSync();
        ViewAsserts.assertRightAligned(relativeLayout, view10);
        assertEquals(view11.getTop(), view10.getBottom());
        ViewAsserts.assertRightAligned(relativeLayout, view11);
        ViewAsserts.assertBottomAligned(relativeLayout, view11);

        // -- BOTTOM
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                relativeLayout.setGravity(Gravity.BOTTOM);
            }
        });
        getInstrumentation().waitForIdleSync();
        ViewAsserts.assertLeftAligned(relativeLayout, view10);
        assertEquals(view11.getTop(), view10.getBottom());
        ViewAsserts.assertLeftAligned(relativeLayout, view11);
        ViewAsserts.assertBottomAligned(relativeLayout, view11);

        // CENTER_HORIZONTAL
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                relativeLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            }
        });
        getInstrumentation().waitForIdleSync();
        ViewAsserts.assertHorizontalCenterAligned(relativeLayout, view10);
        ViewAsserts.assertTopAligned(relativeLayout, view10);
        ViewAsserts.assertHorizontalCenterAligned(relativeLayout, view11);
        assertEquals(view11.getTop(), view10.getBottom());

        // CENTER_VERTICAL
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                relativeLayout.setGravity(Gravity.CENTER_VERTICAL);
            }
        });
        getInstrumentation().waitForIdleSync();
        ViewAsserts.assertLeftAligned(relativeLayout, view10);
        int topSpace = view10.getTop();
        int bottomSpace = relativeLayout.getHeight() - view11.getBottom();
        assertTrue(Math.abs(bottomSpace - topSpace) <= 1);
        ViewAsserts.assertLeftAligned(relativeLayout, view11);
        assertEquals(view11.getTop(), view10.getBottom());
    }

    public void testSetHorizontalGravity() {
        final RelativeLayout relativeLayout = (RelativeLayout) mActivity.findViewById(
                R.id.relative_sublayout_gravity);

        View view10 = mActivity.findViewById(R.id.relative_view10);
        View view11 = mActivity.findViewById(R.id.relative_view11);

        // Default: -- LEFT
        ViewAsserts.assertLeftAligned(relativeLayout, view10);
        ViewAsserts.assertTopAligned(relativeLayout, view10);
        ViewAsserts.assertLeftAligned(relativeLayout, view11);
        assertEquals(view11.getTop(), view10.getBottom());

        // RIGHT
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                relativeLayout.setHorizontalGravity(Gravity.RIGHT);
            }
        });
        getInstrumentation().waitForIdleSync();
        ViewAsserts.assertRightAligned(relativeLayout, view10);
        ViewAsserts.assertTopAligned(relativeLayout, view10);
        ViewAsserts.assertRightAligned(relativeLayout, view11);
        assertEquals(view11.getTop(), view10.getBottom());

        // CENTER_HORIZONTAL
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                relativeLayout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
            }
        });
        getInstrumentation().waitForIdleSync();
        ViewAsserts.assertHorizontalCenterAligned(relativeLayout, view10);
        ViewAsserts.assertTopAligned(relativeLayout, view10);
        ViewAsserts.assertHorizontalCenterAligned(relativeLayout, view11);
        assertEquals(view11.getTop(), view10.getBottom());
    }

    public void testSetVerticalGravity() {
        final RelativeLayout relativeLayout = (RelativeLayout) mActivity.findViewById(
                R.id.relative_sublayout_gravity);

        View view10 = mActivity.findViewById(R.id.relative_view10);
        View view11 = mActivity.findViewById(R.id.relative_view11);

        // Default: -- TOP
        ViewAsserts.assertLeftAligned(relativeLayout, view10);
        ViewAsserts.assertTopAligned(relativeLayout, view10);
        ViewAsserts.assertLeftAligned(relativeLayout, view11);
        assertEquals(view11.getTop(), view10.getBottom());

        // BOTTOM
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                relativeLayout.setVerticalGravity(Gravity.BOTTOM);
            }
        });
        getInstrumentation().waitForIdleSync();
        ViewAsserts.assertLeftAligned(relativeLayout, view10);
        assertEquals(view11.getTop(), view10.getBottom());
        ViewAsserts.assertLeftAligned(relativeLayout, view11);
        ViewAsserts.assertBottomAligned(relativeLayout, view11);

        // CENTER_VERTICAL
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                relativeLayout.setVerticalGravity(Gravity.CENTER_VERTICAL);
            }
        });
        getInstrumentation().waitForIdleSync();
        ViewAsserts.assertLeftAligned(relativeLayout, view10);
        int topSpace = view10.getTop();
        int bottomSpace = relativeLayout.getHeight() - view11.getBottom();
        assertTrue(Math.abs(bottomSpace - topSpace) <= 1);
        ViewAsserts.assertLeftAligned(relativeLayout, view11);
        assertEquals(view11.getTop(), view10.getBottom());
    }

    public void testGetBaseline() {
        RelativeLayout relativeLayout = new RelativeLayout(mActivity);
        assertEquals(-1, relativeLayout.getBaseline());

        relativeLayout = (RelativeLayout) mActivity.findViewById(R.id.relative_sublayout_attrs);
        View view = mActivity.findViewById(R.id.relative_view1);
        assertEquals(view.getBaseline(), relativeLayout.getBaseline());
    }

    public void testGenerateLayoutParams1() throws XmlPullParserException, IOException {
        RelativeLayout relativeLayout = new RelativeLayout(mActivity);

        // normal value
        XmlResourceParser parser = mActivity.getResources().getLayout(R.layout.relative_layout);
        XmlUtils.beginDocument(parser, "RelativeLayout");
        LayoutParams layoutParams = relativeLayout.generateLayoutParams(parser);
        assertEquals(LayoutParams.MATCH_PARENT, layoutParams.width);
        assertEquals(LayoutParams.MATCH_PARENT, layoutParams.height);
    }

    public void testGenerateLayoutParams2() {
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(200, 300);

        MyRelativeLayout myRelativeLayout = new MyRelativeLayout(mActivity);

        // normal value
         RelativeLayout.LayoutParams layoutParams =
                 (RelativeLayout.LayoutParams) myRelativeLayout.generateLayoutParams(p);
         assertEquals(200, layoutParams.width);
         assertEquals(300, layoutParams.height);

        // exceptional value
         try {
             myRelativeLayout.generateLayoutParams((ViewGroup.LayoutParams) null);
             fail("Should throw RuntimeException");
         } catch (RuntimeException e) {
         }
    }

    public void testGenerateDefaultLayoutParams() {
        MyRelativeLayout myRelativeLayout = new MyRelativeLayout(mActivity);

        ViewGroup.LayoutParams layoutParams = myRelativeLayout.generateDefaultLayoutParams();
        assertTrue(layoutParams instanceof RelativeLayout.LayoutParams);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, layoutParams.width);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, layoutParams.height);
    }

    public void testOnMeasure() {
        // onMeasure() is implementation details, do NOT test
    }

    public void testOnLayout() {
        // onLayout() is implementation details, do NOT test
    }

    public void testCheckLayoutParams() {
        MyRelativeLayout myRelativeLayout = new MyRelativeLayout(mActivity);

        ViewGroup.LayoutParams p1 = new ViewGroup.LayoutParams(200, 300);
        assertFalse(myRelativeLayout.checkLayoutParams(p1));

        RelativeLayout.LayoutParams p2 = new RelativeLayout.LayoutParams(200, 300);
        assertTrue(myRelativeLayout.checkLayoutParams(p2));

        AbsListView.LayoutParams p3 = new AbsListView.LayoutParams(200, 300);
        assertFalse(myRelativeLayout.checkLayoutParams(p3));
    }

    private class MyRelativeLayout extends RelativeLayout {
        public MyRelativeLayout(Context context) {
            super(context);
        }

        @Override
        protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
        }

        @Override
        protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
            return super.checkLayoutParams(p);
        }

        @Override
        protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
            return super.generateDefaultLayoutParams();
        }
        @Override
        protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
            return super.generateLayoutParams(p);
        }
    }
}
