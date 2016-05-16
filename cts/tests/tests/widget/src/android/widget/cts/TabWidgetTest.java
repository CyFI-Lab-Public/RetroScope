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


import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

/**
 * Test {@link TabWidget}.
 */
public class TabWidgetTest extends ActivityInstrumentationTestCase2<TabHostStubActivity> {
    private Activity mActivity;

    public TabWidgetTest() {
        super("com.android.cts.stub", TabHostStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructor() {
        new TabWidget(mActivity);

        new TabWidget(mActivity, null);

        new TabWidget(mActivity, null, 0);
    }

    public void testChildDrawableStateChanged() {
        MockTabWidget mockTabWidget = new MockTabWidget(mActivity);
        TextView tv0 = new TextView(mActivity);
        TextView tv1 = new TextView(mActivity);
        mockTabWidget.addView(tv0);
        mockTabWidget.addView(tv1);
        mockTabWidget.setCurrentTab(1);
        mockTabWidget.reset();
        mockTabWidget.childDrawableStateChanged(tv0);
        assertFalse(mockTabWidget.hasCalledInvalidate());

        mockTabWidget.reset();
        mockTabWidget.childDrawableStateChanged(tv1);
        assertTrue(mockTabWidget.hasCalledInvalidate());

        mockTabWidget.reset();
        mockTabWidget.childDrawableStateChanged(null);
        assertFalse(mockTabWidget.hasCalledInvalidate());
    }

    public void testDispatchDraw() {
        // implementation details
    }

    @UiThreadTest
    public void testSetCurrentTab() {
        TabHostStubActivity activity = getActivity();
        TabWidget tabWidget = activity.getTabWidget();
        tabWidget.addView(new TextView(mActivity));

        assertTrue(tabWidget.getChildAt(0).isSelected());
        assertFalse(tabWidget.getChildAt(1).isSelected());
        assertTrue(tabWidget.getChildAt(0).isFocused());
        assertFalse(tabWidget.getChildAt(1).isFocused());

        tabWidget.setCurrentTab(1);
        assertFalse(tabWidget.getChildAt(0).isSelected());
        assertTrue(tabWidget.getChildAt(1).isSelected());
        assertTrue(tabWidget.getChildAt(0).isFocused());
        assertFalse(tabWidget.getChildAt(1).isFocused());
    }

    @UiThreadTest
    public void testFocusCurrentTab() {
        TabHostStubActivity activity = getActivity();
        TabWidget tabWidget = activity.getTabWidget();
        tabWidget.addView(new TextView(mActivity));

        assertTrue(tabWidget.getChildAt(0).isSelected());
        assertFalse(tabWidget.getChildAt(1).isSelected());
        assertEquals(tabWidget.getChildAt(0), tabWidget.getFocusedChild());
        assertTrue(tabWidget.getChildAt(0).isFocused());
        assertFalse(tabWidget.getChildAt(1).isFocused());

        // normal
        tabWidget.focusCurrentTab(1);
        assertFalse(tabWidget.getChildAt(0).isSelected());
        assertTrue(tabWidget.getChildAt(1).isSelected());
        assertEquals(tabWidget.getChildAt(1), tabWidget.getFocusedChild());
        assertFalse(tabWidget.getChildAt(0).isFocused());
        assertTrue(tabWidget.getChildAt(1).isFocused());

        tabWidget.focusCurrentTab(0);
        assertTrue(tabWidget.getChildAt(0).isSelected());
        assertFalse(tabWidget.getChildAt(1).isSelected());
        assertEquals(tabWidget.getChildAt(0), tabWidget.getFocusedChild());
        assertTrue(tabWidget.getChildAt(0).isFocused());
        assertFalse(tabWidget.getChildAt(1).isFocused());

        // exceptional
        try {
            tabWidget.focusCurrentTab(-1);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected exception
        }

        try {
            tabWidget.focusCurrentTab(tabWidget.getChildCount() + 1);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected exception
        }
    }

    public void testSetEnabled() {
        TabWidget tabWidget = new TabWidget(mActivity);
        tabWidget.addView(new TextView(mActivity));
        tabWidget.addView(new TextView(mActivity));
        assertTrue(tabWidget.isEnabled());
        assertTrue(tabWidget.getChildAt(0).isEnabled());
        assertTrue(tabWidget.getChildAt(1).isEnabled());

        tabWidget.setEnabled(false);
        assertFalse(tabWidget.isEnabled());
        assertFalse(tabWidget.getChildAt(0).isEnabled());
        assertFalse(tabWidget.getChildAt(1).isEnabled());

        tabWidget.setEnabled(true);
        assertTrue(tabWidget.isEnabled());
        assertTrue(tabWidget.getChildAt(0).isEnabled());
        assertTrue(tabWidget.getChildAt(1).isEnabled());
    }

    public void testAddView() {
        MockTabWidget mockTabWidget = new MockTabWidget(mActivity);

        // normal value
        View view1 = new TextView(mActivity);
        mockTabWidget.addView(view1);
        assertSame(view1, mockTabWidget.getChildAt(0));
        LayoutParams defaultLayoutParam = mockTabWidget.generateDefaultLayoutParams();
        if (mockTabWidget.getOrientation() == LinearLayout.VERTICAL) {
            assertEquals(defaultLayoutParam.height, LayoutParams.WRAP_CONTENT);
            assertEquals(defaultLayoutParam.width, LayoutParams.MATCH_PARENT);
        } else if (mockTabWidget.getOrientation() == LinearLayout.HORIZONTAL) {
            assertEquals(defaultLayoutParam.height, LayoutParams.WRAP_CONTENT);
            assertEquals(defaultLayoutParam.width, LayoutParams.WRAP_CONTENT);
        } else {
            assertNull(defaultLayoutParam);
        }

        View view2 = new RelativeLayout(mActivity);
        mockTabWidget.addView(view2);
        assertSame(view2, mockTabWidget.getChildAt(1));

        try {
            mockTabWidget.addView(new ListView(mActivity));
            fail("did not throw RuntimeException when adding invalid view");
        } catch (RuntimeException e) {
            // issue 1695243
        }

        try {
            mockTabWidget.addView(null);
            fail("did not throw NullPointerException when child is null");
        } catch (NullPointerException e) {
            // issue 1695243
        }
    }

    public void testOnFocusChange() {
        // onFocusChange() is implementation details, do NOT test
    }

    public void testOnSizeChanged() {
        // implementation details
    }

    /*
     * Mock class for TabWidget to be used in test cases.
     */
    private class MockTabWidget extends TabWidget {
        private boolean mCalledInvalidate = false;

        public MockTabWidget(Context context) {
            super(context);
        }

        @Override
        protected LayoutParams generateDefaultLayoutParams() {
            return super.generateDefaultLayoutParams();
        }

        @Override
        public void invalidate() {
            super.invalidate();
            mCalledInvalidate = true;
        }

        public boolean hasCalledInvalidate() {
            return mCalledInvalidate;
        }

        public void reset() {
            mCalledInvalidate = false;
        }
    }
}
