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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.cts.stub.R;


/**
 * Test {@link TableRow}.
 */
public class TableRowTest extends ActivityInstrumentationTestCase2<TableStubActivity> {
    Context mContext;
    Context mTargetContext;

    public TableRowTest() {
        super("com.android.cts.stub", TableStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
        mTargetContext = getInstrumentation().getTargetContext();
    }

    public void testConstructor() {
        new TableRow(mContext);

        new TableRow(mContext, null);
    }

    public void testSetOnHierarchyChangeListener() {
        TableRow tableRow = new TableRow(mContext);

        MockOnHierarchyChangeListener listener = new MockOnHierarchyChangeListener();
        tableRow.setOnHierarchyChangeListener(listener);

        tableRow.addView(new TextView(mContext));
        assertTrue(listener.hasCalledOnChildViewAdded());
        tableRow.removeViewAt(0);
        assertTrue(listener.hasCalledOnChildViewRemoved());

        listener.reset();

        tableRow.setOnHierarchyChangeListener(null);
        tableRow.addView(new TextView(mContext));
        assertFalse(listener.hasCalledOnChildViewAdded());
        tableRow.removeViewAt(0);
        assertFalse(listener.hasCalledOnChildViewRemoved());
    }

    @UiThreadTest
    public void testGetVirtualChildAt() {
        TableStubActivity activity = getActivity();
        activity.setContentView(com.android.cts.stub.R.layout.table_layout_1);
        TableLayout tableLayout = (TableLayout) activity
                .findViewById(com.android.cts.stub.R.id.table1);

        TableRow tableRow = (TableRow) tableLayout.getChildAt(0);
        Resources resources = activity.getResources();
        assertEquals(resources.getString(R.string.table_layout_first),
                ((TextView) tableRow.getVirtualChildAt(0)).getText().toString());
        assertEquals(resources.getString(R.string.table_layout_second),
                ((TextView) tableRow.getVirtualChildAt(1)).getText().toString());
        assertEquals(resources.getString(R.string.table_layout_third),
                ((TextView) tableRow.getVirtualChildAt(2)).getText().toString());

        activity.setContentView(com.android.cts.stub.R.layout.table_layout_2);
        tableLayout = (TableLayout) activity.findViewById(com.android.cts.stub.R.id.table2);

        tableRow = (TableRow) tableLayout.getChildAt(0);
        assertNull(tableRow.getVirtualChildAt(0));
        assertEquals(resources.getString(R.string.table_layout_long),
                ((TextView) tableRow.getVirtualChildAt(1)).getText().toString());
        assertEquals(resources.getString(R.string.table_layout_second),
                ((TextView) tableRow.getVirtualChildAt(2)).getText().toString());
        assertEquals(resources.getString(R.string.table_layout_second),
                ((TextView) tableRow.getVirtualChildAt(3)).getText().toString());
        assertEquals(resources.getString(R.string.table_layout_third),
                ((TextView) tableRow.getVirtualChildAt(4)).getText().toString());
    }

    @UiThreadTest
    public void testGetVirtualChildCount() {
        TableStubActivity activity = getActivity();
        activity.setContentView(com.android.cts.stub.R.layout.table_layout_1);
        TableLayout tableLayout = (TableLayout) activity
                .findViewById(com.android.cts.stub.R.id.table1);

        TableRow tableRow = (TableRow) tableLayout.getChildAt(0);
        assertEquals(3, tableRow.getVirtualChildCount());

        activity.setContentView(com.android.cts.stub.R.layout.table_layout_2);
        tableLayout = (TableLayout) activity.findViewById(com.android.cts.stub.R.id.table2);

        tableRow = (TableRow) tableLayout.getChildAt(0);
        assertEquals(5, tableRow.getVirtualChildCount());
    }

    public void testGenerateLayoutParams() {
        TableRow tableRow = new TableRow(mContext);

        Resources resources = mTargetContext.getResources();
        XmlResourceParser parser = resources.getLayout(R.layout.table_layout_1);
        AttributeSet attr = Xml.asAttributeSet(parser);

        assertNotNull(tableRow.generateLayoutParams(attr));

        assertNotNull(tableRow.generateLayoutParams((AttributeSet) null));
    }

    public void testCheckLayoutParams() {
        MockTableRow mockTableRow = new MockTableRow(mContext);

        assertTrue(mockTableRow.checkLayoutParams(new TableRow.LayoutParams(200, 300)));

        assertFalse(mockTableRow.checkLayoutParams(new ViewGroup.LayoutParams(200, 300)));

        assertFalse(mockTableRow.checkLayoutParams(new RelativeLayout.LayoutParams(200, 300)));

        assertFalse(mockTableRow.checkLayoutParams(null));
    }

    public void testGenerateDefaultLayoutParams() {
        MockTableRow mockTableRow = new MockTableRow(mContext);

        LinearLayout.LayoutParams layoutParams = mockTableRow.generateDefaultLayoutParams();
        assertNotNull(layoutParams);
        assertTrue(layoutParams instanceof TableRow.LayoutParams);
    }

    public void testGenerateLayoutParams2() {
        MockTableRow mockTableRow = new MockTableRow(mContext);

        LinearLayout.LayoutParams layoutParams = mockTableRow.generateLayoutParams(
                new ViewGroup.LayoutParams(200, 300));
        assertNotNull(layoutParams);
        assertEquals(200, layoutParams.width);
        assertEquals(300, layoutParams.height);
        assertTrue(layoutParams instanceof TableRow.LayoutParams);

        try {
            layoutParams = mockTableRow.generateLayoutParams((ViewGroup.LayoutParams) null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testOnLayout() {
        MockTableRow mockTableRow = new MockTableRow(mContext);

        mockTableRow.onLayout(false, 0, 0, 200, 300);
    }

    public void testOnMeasure() {
        MockTableRow mockTableRow = new MockTableRow(mContext);

        mockTableRow.onMeasure(MeasureSpec.EXACTLY, MeasureSpec.EXACTLY);
    }

    private class MockOnHierarchyChangeListener implements OnHierarchyChangeListener {
        private boolean mCalledOnChildViewAdded = false;
        private boolean mCalledOnChildViewRemoved = false;

        /*
         * (non-Javadoc)
         *
         * @see
         * android.view.ViewGroup.OnHierarchyChangeListener#onChildViewAdded
         * (View, View)
         */
        public void onChildViewAdded(View parent, View child) {
            mCalledOnChildViewAdded = true;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * android.view.ViewGroup.OnHierarchyChangeListener#onChildViewRemoved
         * (View, View)
         */
        public void onChildViewRemoved(View parent, View child) {
            mCalledOnChildViewRemoved = true;
        }

        public boolean hasCalledOnChildViewAdded() {
            return mCalledOnChildViewAdded;
        }

        public boolean hasCalledOnChildViewRemoved() {
            return mCalledOnChildViewRemoved;
        }

        public void reset() {
            mCalledOnChildViewAdded = false;
            mCalledOnChildViewRemoved = false;
        }
    }

    /*
     * Mock class for TableRow to test protected methods
     */
    private class MockTableRow extends TableRow {
        public MockTableRow(Context context) {
            super(context);
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
        protected LinearLayout.LayoutParams generateLayoutParams(
                ViewGroup.LayoutParams p) {
            return super.generateLayoutParams(p);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
