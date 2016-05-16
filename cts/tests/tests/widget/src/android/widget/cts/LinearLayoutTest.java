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

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase;
import android.test.ViewAsserts;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.AbsoluteLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.cts.stub.R;

/**
 * Test {@link LinearLayout}.
 */
public class LinearLayoutTest extends ActivityInstrumentationTestCase<LinearLayoutStubActivity> {
    private Context mContext;
    private Activity mActivity;

    public LinearLayoutTest() {
        super("com.android.cts.stub", LinearLayoutStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mContext = getInstrumentation().getTargetContext();
    }

    public void testConstructor() {
        new LinearLayout(mContext);

        new LinearLayout(mContext, null);

        XmlPullParser parser = mContext.getResources().getXml(R.layout.linearlayout_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new LinearLayout(mContext, attrs);

        try {
            new LinearLayout(null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testAccessBaselineAligned() {
        LinearLayout linearLayout = new LinearLayout(mContext);
        linearLayout.setBaselineAligned(true);
        assertTrue(linearLayout.isBaselineAligned());

        linearLayout.setBaselineAligned(false);
        assertFalse(linearLayout.isBaselineAligned());

        // android:baselineAligned="false" in LinearLayout weightsum
        linearLayout = (LinearLayout) mActivity.findViewById(R.id.weightsum);
        assertFalse(linearLayout.isBaselineAligned());

        // default mBaselineAligned is true.
        linearLayout = (LinearLayout) mActivity.findViewById(R.id.horizontal);
        assertTrue(linearLayout.isBaselineAligned());

        // default mBaselineAligned is true.
        // Only applicable if {@link #mOrientation} is horizontal
        linearLayout = (LinearLayout) mActivity.findViewById(R.id.vertical);
        assertTrue(linearLayout.isBaselineAligned());
    }

    public void testGetBaseline() {
        LinearLayout linearLayout = new LinearLayout(mContext);

        ListView lv1 = new ListView(mContext);
        linearLayout.addView(lv1);
        assertEquals(-1, linearLayout.getBaseline());

        ListView lv2 = new ListView(mContext);
        linearLayout.addView(lv2);
        linearLayout.setBaselineAlignedChildIndex(1);
        try {
            linearLayout.getBaseline();
            fail("LinearLayout.getBaseline() should throw exception here.");
        } catch (RuntimeException e) {
        }

        MockListView lv3 = new MockListView(mContext);
        linearLayout.addView(lv3);
        linearLayout.setBaselineAlignedChildIndex(2);
        assertEquals(lv3.getBaseline(), linearLayout.getBaseline());
    }

    public void testAccessBaselineAlignedChildIndex() {
        LinearLayout linearLayout = new LinearLayout(mContext);
        // set BaselineAlignedChildIndex
        ListView lv1 = new ListView(mContext);
        ListView lv2 = new ListView(mContext);
        ListView lv3 = new ListView(mContext);
        linearLayout.addView(lv1);
        linearLayout.addView(lv2);
        linearLayout.addView(lv3);
        linearLayout.setBaselineAlignedChildIndex(1);
        assertEquals(1, linearLayout.getBaselineAlignedChildIndex());

        linearLayout.setBaselineAlignedChildIndex(2);
        assertEquals(2, linearLayout.getBaselineAlignedChildIndex());

        try {
            linearLayout.setBaselineAlignedChildIndex(-1);
            fail("LinearLayout should throw IllegalArgumentException here.");
        } catch (IllegalArgumentException e) {
        }
        try {
            linearLayout.setBaselineAlignedChildIndex(3);
            fail("LinearLayout should throw IllegalArgumentException here.");
        } catch (IllegalArgumentException e) {
        }

        linearLayout = (LinearLayout) mActivity.findViewById(R.id.baseline_aligned_child_index);
        assertEquals(1, linearLayout.getBaselineAlignedChildIndex());
    }

    /**
     * weightsum is a horizontal LinearLayout. There are three children in it.
     */
    public void testAccessWeightSum() {
        LinearLayout parent = (LinearLayout) mActivity.findViewById(R.id.weightsum);
        TextView weight02 = (TextView) mActivity.findViewById(R.id.weight_0_2);
        TextView weight05 = (TextView) mActivity.findViewById(R.id.weight_0_5);
        TextView weight03 = (TextView) mActivity.findViewById(R.id.weight_0_3);

        assertNotNull(parent);
        assertNotNull(weight02);
        assertNotNull(weight05);
        assertNotNull(weight03);

        assertEquals(mContext.getResources().getString(R.string.horizontal_text_1),
                weight02.getText().toString());
        assertEquals(mContext.getResources().getString(R.string.horizontal_text_2),
                weight05.getText().toString());
        assertEquals(mContext.getResources().getString(R.string.horizontal_text_3),
                weight03.getText().toString());

        assertEquals(LinearLayout.HORIZONTAL, parent.getOrientation());
        assertEquals(1.0f, parent.getWeightSum());

        int parentWidth = parent.getWidth();
        assertEquals(Math.ceil(parentWidth * 0.2), weight02.getWidth(), 1.0);
        assertEquals(Math.ceil(parentWidth * 0.5), weight05.getWidth(), 1.0);
        assertEquals(Math.ceil(parentWidth * 0.3), weight03.getWidth(), 1.0);
    }

    public void testGenerateLayoutParams() {
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(320, 240);
        MockLinearLayout mockLinearLayout = new MockLinearLayout(mContext);
        LayoutParams layoutParams1 = mockLinearLayout.generateLayoutParams(lp);
        assertEquals(320, layoutParams1.width);
        assertEquals(240, layoutParams1.height);

        // generateLayoutParams() always throw  a RuntimeException.
//        XmlPullParser parser = mContext.getResources().getXml(R.layout.linearlayout_layout);
//        AttributeSet attrs = Xml.asAttributeSet(parser);
//        LinearLayout linearLayout = new LinearLayout(mContext, attrs);
//        LayoutParams layoutParams2 = linearLayout.generateLayoutParams(attrs);
//        assertEquals(LayoutParams.MATCH_PARENT, layoutParams2.width);
//        assertEquals(LayoutParams.WRAP_CONTENT, layoutParams2.height);
    }

    public void testCheckLayoutParams() {
        MockLinearLayout mockLinearLayout = new MockLinearLayout(mContext);

        ViewGroup.LayoutParams params = new AbsoluteLayout.LayoutParams(240, 320, 0, 0);
        assertFalse(mockLinearLayout.checkLayoutParams(params));

        params = new LinearLayout.LayoutParams(240, 320);
        assertTrue(mockLinearLayout.checkLayoutParams(params));
    }

    public void testGenerateDefaultLayoutParams() {
        MockLinearLayout mockLinearLayout = new MockLinearLayout(mContext);

        mockLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        ViewGroup.LayoutParams param = mockLinearLayout.generateDefaultLayoutParams();
        assertNotNull(param);
        assertTrue(param instanceof LinearLayout.LayoutParams);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, param.width);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, param.height);

        mockLinearLayout.setOrientation(LinearLayout.VERTICAL);
        param = mockLinearLayout.generateDefaultLayoutParams();
        assertNotNull(param);
        assertTrue(param instanceof LinearLayout.LayoutParams);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, param.width);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, param.height);

        mockLinearLayout.setOrientation(-1);
        assertNull(mockLinearLayout.generateDefaultLayoutParams());
    }

    /**
     * layout of horizontal LinearLayout.
     * ----------------------------------------------------
     * | ------------ |                 |                 |
     * | | top view | | --------------- |                 |
     * | |          | | | center view | | --------------- |
     * | ------------ | |             | | | bottom view | |
     * |              | --------------- | |             | |
     * |     parent   |                 | --------------- |
     * ----------------------------------------------------
     */
    public void testLayoutHorizontal() {
        LinearLayout parent = (LinearLayout) mActivity.findViewById(R.id.horizontal);
        TextView topView = (TextView) mActivity.findViewById(R.id.gravity_top);
        TextView centerView = (TextView) mActivity.findViewById(R.id.gravity_center_vertical);
        TextView bottomView = (TextView) mActivity.findViewById(R.id.gravity_bottom);

        assertNotNull(parent);
        assertNotNull(topView);
        assertNotNull(centerView);
        assertNotNull(bottomView);

        assertEquals(mContext.getResources().getString(R.string.horizontal_text_1),
                topView.getText().toString());
        assertEquals(mContext.getResources().getString(R.string.horizontal_text_2),
                centerView.getText().toString());
        assertEquals(mContext.getResources().getString(R.string.horizontal_text_3),
                bottomView.getText().toString());

        assertEquals(LinearLayout.HORIZONTAL, parent.getOrientation());

        ViewAsserts.assertTopAligned(parent, topView);
        ViewAsserts.assertVerticalCenterAligned(parent, centerView);
        ViewAsserts.assertBottomAligned(parent, bottomView);

        assertEquals(0, topView.getTop());
        assertEquals(topView.getHeight(), topView.getBottom());
        assertEquals(0, topView.getLeft());
        assertEquals(centerView.getLeft(), topView.getRight());

        int offset = (parent.getHeight() - centerView.getHeight()) / 2;
        assertEquals(offset, centerView.getTop());
        assertEquals(offset + centerView.getHeight(), centerView.getBottom());
        assertEquals(topView.getRight(), centerView.getLeft());
        assertEquals(bottomView.getLeft(), centerView.getRight());

        assertEquals(parent.getHeight() - bottomView.getHeight(), bottomView.getTop());
        assertEquals(parent.getHeight(), bottomView.getBottom());
        assertEquals(centerView.getRight(), bottomView.getLeft());
        assertEquals(parent.getWidth(), bottomView.getRight());
    }

    /**
     * layout of vertical LinearLayout.
     * -----------------------------------
     * | -------------                   |
     * | | left view |                   |
     * | -------------                   |
     * | - - - - - - - - - - - - - - - - |
     * |        ---------------          |
     * |        | center view |          |
     * |        ---------------          |
     * | - - - - - - - - - - - - - - - - |
     * |                  -------------- |
     * | parent           | right view | |
     * |                  -------------- |
     * -----------------------------------
     */
    public void testLayoutVertical() {
        LinearLayout parent = (LinearLayout) mActivity.findViewById(R.id.vertical);
        TextView leftView = (TextView) mActivity.findViewById(R.id.gravity_left);
        TextView centerView = (TextView) mActivity.findViewById(R.id.gravity_center_horizontal);
        TextView rightView = (TextView) mActivity.findViewById(R.id.gravity_right);

        assertNotNull(parent);
        assertNotNull(leftView);
        assertNotNull(centerView);
        assertNotNull(rightView);

        assertEquals(mContext.getResources().getString(R.string.vertical_text_1),
                leftView.getText().toString());
        assertEquals(mContext.getResources().getString(R.string.vertical_text_2),
                centerView.getText().toString());
        assertEquals(mContext.getResources().getString(R.string.vertical_text_3),
                rightView.getText().toString());

        assertEquals(LinearLayout.VERTICAL, parent.getOrientation());

        ViewAsserts.assertLeftAligned(parent, leftView);
        ViewAsserts.assertHorizontalCenterAligned(parent, centerView);
        ViewAsserts.assertRightAligned(parent, rightView);

        assertEquals(0, leftView.getTop());
        assertEquals(centerView.getTop(), leftView.getBottom());
        assertEquals(0, leftView.getLeft());
        assertEquals(leftView.getWidth(), leftView.getRight());

        int offset = (parent.getWidth() - centerView.getWidth()) / 2;
        assertEquals(leftView.getBottom(), centerView.getTop());
        assertEquals(rightView.getTop(), centerView.getBottom());
        assertEquals(offset, centerView.getLeft());
        assertEquals(offset + centerView.getWidth(), centerView.getRight());

        assertEquals(centerView.getBottom(), rightView.getTop());
        assertEquals(parent.getHeight(), rightView.getBottom());
        assertEquals(parent.getWidth() - rightView.getWidth(), rightView.getLeft());
        assertEquals(parent.getWidth(), rightView.getRight());
    }

    private class MockListView extends ListView {
        private final static int DEFAULT_CHILD_BASE_LINE = 1;

        public MockListView(Context context) {
            super(context);
        }

        public int getBaseline() {
            return DEFAULT_CHILD_BASE_LINE;
        }
    }

    /**
     * Add MockLinearLayout to help for testing protected methods in LinearLayout.
     * Because we can not access protected methods in LinearLayout directly, we have to
     * extends from it and override protected methods so that we can access them in
     * our test codes.
     */
    private class MockLinearLayout extends LinearLayout {
        public MockLinearLayout(Context c) {
            super(c);
        }

        @Override
        protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
            return super.checkLayoutParams(p);
        }

        @Override
        protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
            return super.generateDefaultLayoutParams();
        }

        @Override
        protected LinearLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
            return super.generateLayoutParams(p);
        }
    }
}
