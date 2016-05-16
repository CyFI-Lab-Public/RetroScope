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

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

public class ExpandableListViewTest extends AndroidTestCase {
    public void testConstructor() {
        new ExpandableListView(mContext);

        new ExpandableListView(mContext, null);

        new ExpandableListView(mContext, null, 0);

        XmlPullParser parser =
            getContext().getResources().getXml(R.layout.expandablelistview_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new ExpandableListView(mContext, attrs);
        new ExpandableListView(mContext, attrs, 0);

        try {
            new ExpandableListView(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new ExpandableListView(null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new ExpandableListView(null, null, 0);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testSetChildDivider() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.scenery);
        expandableListView.setChildDivider(drawable);
    }

    public void testSetAdapter() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        try {
            expandableListView.setAdapter((ListAdapter) null);
            fail("setAdapter(ListAdapter) should throw RuntimeException here.");
        } catch (RuntimeException e) {
        }
    }

    public void testGetAdapter() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        assertNull(expandableListView.getAdapter());

        ExpandableListAdapter expandableAdapter = new MockExpandableListAdapter();
        expandableListView.setAdapter(expandableAdapter);
        assertNotNull(expandableListView.getAdapter());
    }

    public void testAccessExpandableListAdapter() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        ExpandableListAdapter expandableAdapter = new MockExpandableListAdapter();

        assertNull(expandableListView.getExpandableListAdapter());
        expandableListView.setAdapter(expandableAdapter);
        assertSame(expandableAdapter, expandableListView.getExpandableListAdapter());
    }

    public void testPerformItemClick() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);

        assertFalse(expandableListView.performItemClick(null, 100, 99));

        MockOnItemClickListener onClickListener = new MockOnItemClickListener();
        expandableListView.setOnItemClickListener(onClickListener);
        assertTrue(expandableListView.performItemClick(null, 100, 99));
    }

    public void testSetOnItemClickListener() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        MockOnItemClickListener listener = new MockOnItemClickListener();

        assertNull(expandableListView.getOnItemClickListener());
        expandableListView.setOnItemClickListener(listener);
        assertSame(listener, expandableListView.getOnItemClickListener());
    }

    public void testExpandGroup() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        ExpandableListAdapter expandableAdapter = new MockExpandableListAdapter();
        expandableListView.setAdapter(expandableAdapter);

        MockOnGroupExpandListener mockOnGroupExpandListener = new MockOnGroupExpandListener();
        expandableListView.setOnGroupExpandListener(mockOnGroupExpandListener);

        assertFalse(mockOnGroupExpandListener.hasCalledOnGroupExpand());
        assertTrue(expandableListView.expandGroup(0));
        assertTrue(mockOnGroupExpandListener.hasCalledOnGroupExpand());
        mockOnGroupExpandListener.reset();
        assertFalse(expandableListView.expandGroup(0));
        assertTrue(mockOnGroupExpandListener.hasCalledOnGroupExpand());
        mockOnGroupExpandListener.reset();
        assertTrue(expandableListView.expandGroup(1));
        assertTrue(mockOnGroupExpandListener.hasCalledOnGroupExpand());
        mockOnGroupExpandListener.reset();
        assertFalse(expandableListView.expandGroup(1));
        assertTrue(mockOnGroupExpandListener.hasCalledOnGroupExpand());
        mockOnGroupExpandListener.reset();

        expandableListView.setAdapter((ExpandableListAdapter) null);
        try {
            expandableListView.expandGroup(0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testCollapseGroup() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        ExpandableListAdapter expandableAdapter = new MockExpandableListAdapter();
        expandableListView.setAdapter(expandableAdapter);

        MockOnGroupCollapseListener mockOnGroupCollapseListener =
            new MockOnGroupCollapseListener();
        expandableListView.setOnGroupCollapseListener(mockOnGroupCollapseListener);

        assertFalse(mockOnGroupCollapseListener.hasCalledOnGroupCollapse());
        assertFalse(expandableListView.collapseGroup(0));
        assertTrue(mockOnGroupCollapseListener.hasCalledOnGroupCollapse());
        mockOnGroupCollapseListener.reset();

        expandableListView.expandGroup(0);
        assertTrue(expandableListView.collapseGroup(0));
        assertTrue(mockOnGroupCollapseListener.hasCalledOnGroupCollapse());
        mockOnGroupCollapseListener.reset();
        assertFalse(expandableListView.collapseGroup(1));
        assertTrue(mockOnGroupCollapseListener.hasCalledOnGroupCollapse());
        mockOnGroupCollapseListener.reset();

        expandableListView.setAdapter((ExpandableListAdapter) null);
        try {
            expandableListView.collapseGroup(0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testSetOnGroupClickListener() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());
        MockOnGroupClickListener listener = new MockOnGroupClickListener();

        expandableListView.setOnGroupClickListener(listener);
        assertFalse(listener.hasCalledOnGroupClick());
        expandableListView.performItemClick(null, 0, 0);
        assertTrue(listener.hasCalledOnGroupClick());
    }

    public void testSetOnChildClickListener() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());
        MockOnChildClickListener listener = new MockOnChildClickListener();

        expandableListView.setOnChildClickListener(listener);
        assertFalse(listener.hasCalledOnChildClick());
        // first let the list expand
        expandableListView.expandGroup(0);
        // click on the child list of the first group
        expandableListView.performItemClick(null, 1, 0);
        assertTrue(listener.hasCalledOnChildClick());
    }

    public void testGetExpandableListPosition() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());

        assertEquals(0, expandableListView.getExpandableListPosition(0));

        // Group 0 is not expanded, position 1 is invalid
        assertEquals(ExpandableListView.PACKED_POSITION_VALUE_NULL,
                expandableListView.getExpandableListPosition(1));

        // Position 1 becomes valid when group 0 is expanded
        expandableListView.expandGroup(0);
        assertEquals(ExpandableListView.getPackedPositionForChild(0, 0),
                expandableListView.getExpandableListPosition(1));

        // Position 2 is still invalid (only one child).
        assertEquals(ExpandableListView.PACKED_POSITION_VALUE_NULL,
                expandableListView.getExpandableListPosition(2));
    }

    public void testGetFlatListPosition() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());

        try {
            expandableListView.getFlatListPosition(ExpandableListView.PACKED_POSITION_VALUE_NULL);
        } catch (NullPointerException e) {
        }
        assertEquals(0, expandableListView.getFlatListPosition(
                ExpandableListView.PACKED_POSITION_TYPE_CHILD<<32));
        // 0x8000000100000000L means this is a child and group position is 1.
        assertEquals(1, expandableListView.getFlatListPosition(0x8000000100000000L));
    }

    public void testGetSelectedPosition() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);

        assertEquals(ExpandableListView.PACKED_POSITION_VALUE_NULL,
                expandableListView.getSelectedPosition());

        expandableListView.setAdapter(new MockExpandableListAdapter());

        expandableListView.setSelectedGroup(0);
        assertEquals(0, expandableListView.getSelectedPosition());

        expandableListView.setSelectedGroup(1);
        assertEquals(0, expandableListView.getSelectedPosition());
    }

    public void testGetSelectedId() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);

        assertEquals(-1, expandableListView.getSelectedId());
        expandableListView.setAdapter(new MockExpandableListAdapter());

        expandableListView.setSelectedGroup(0);
        assertEquals(0, expandableListView.getSelectedId());

        expandableListView.setSelectedGroup(1);
        assertEquals(0, expandableListView.getSelectedId());
    }

    public void testSetSelectedGroup() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());

        expandableListView.setSelectedGroup(0);
        assertEquals(0, expandableListView.getSelectedPosition());

        expandableListView.setSelectedGroup(1);
        assertEquals(0, expandableListView.getSelectedPosition());
    }

    public void testSetSelectedChild() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());

        assertTrue(expandableListView.setSelectedChild(0, 0, false));
        assertTrue(expandableListView.setSelectedChild(0, 1, true));
    }

    public void testIsGroupExpanded() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setAdapter(new MockExpandableListAdapter());

        expandableListView.expandGroup(1);
        assertFalse(expandableListView.isGroupExpanded(0));
        assertTrue(expandableListView.isGroupExpanded(1));
    }

    public void testGetPackedPositionType() {
        assertEquals(ExpandableListView.PACKED_POSITION_TYPE_NULL,
                ExpandableListView.getPackedPositionType(
                        ExpandableListView.PACKED_POSITION_VALUE_NULL));

        assertEquals(ExpandableListView.PACKED_POSITION_TYPE_GROUP,
                ExpandableListView.getPackedPositionType(0));

        // 0x8000000000000000L is PACKED_POSITION_MASK_TYPE, but it is private,
        // so we just use its value.
        assertEquals(ExpandableListView.PACKED_POSITION_TYPE_CHILD,
                ExpandableListView.getPackedPositionType(0x8000000000000000L));
    }

    public void testGetPackedPositionGroup() {
        assertEquals(-1, ExpandableListView.getPackedPositionGroup(
                ExpandableListView.PACKED_POSITION_VALUE_NULL));

        assertEquals(0, ExpandableListView.getPackedPositionGroup(0));

        // 0x123400000000L means its group position is 0x1234
        assertEquals(0x1234, ExpandableListView.getPackedPositionGroup(0x123400000000L));

        // 0x7FFFFFFF00000000L means its group position is 0x7FFFFFFF
        assertEquals(0x7FFFFFFF, ExpandableListView.getPackedPositionGroup(0x7FFFFFFF00000000L));
    }

    public void testGetPackedPositionChild() {
        assertEquals(-1, ExpandableListView.getPackedPositionChild(
                ExpandableListView.PACKED_POSITION_VALUE_NULL));

        assertEquals(-1, ExpandableListView.getPackedPositionChild(1));

        // 0x8000000000000000L means its child position is 0
        assertEquals(0, ExpandableListView.getPackedPositionChild(0x8000000000000000L));

        // 0x80000000ffffffffL means its child position is 0xffffffff
        assertEquals(0xffffffff, ExpandableListView.getPackedPositionChild(0x80000000ffffffffL));
    }

    public void testGetPackedPositionForChild() {
        assertEquals(0x8000000000000000L,
                ExpandableListView.getPackedPositionForChild(0, 0));

        assertEquals(0xffffffffffffffffL,
                ExpandableListView.getPackedPositionForChild(Integer.MAX_VALUE, 0xffffffff));
    }

    public void testGetPackedPositionForGroup() {
        assertEquals(0, ExpandableListView.getPackedPositionForGroup(0));

        assertEquals(0x7fffffff00000000L,
                ExpandableListView.getPackedPositionForGroup(Integer.MAX_VALUE));
    }

    public void testSetChildIndicator() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setChildIndicator(null);
    }

    public void testSetChildIndicatorBounds() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setChildIndicatorBounds(10, 10);
    }

    public void testSetGroupIndicator() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        Drawable drawable = new BitmapDrawable();
        expandableListView.setGroupIndicator(drawable);
    }

    public void testSetIndicatorBounds() {
        ExpandableListView expandableListView = new ExpandableListView(mContext);
        expandableListView.setIndicatorBounds(10,10);
    }

    public void testOnSaveInstanceState() {
        ExpandableListView src = new ExpandableListView(mContext);
        Parcelable p1 = src.onSaveInstanceState();

        ExpandableListView dest = new ExpandableListView(mContext);
        dest.onRestoreInstanceState(p1);
        Parcelable p2 = dest.onSaveInstanceState();

        assertNotNull(p1);
        assertNotNull(p2);
    }

    public void testDispatchDraw() {
        MockExpandableListView expandableListView = new MockExpandableListView(mContext);
        expandableListView.dispatchDraw(null);
    }

    private class MockExpandableListAdapter implements ExpandableListAdapter {
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        public int getGroupCount() {
            return 1;
        }

        public int getChildrenCount(int groupPosition) {
            switch (groupPosition) {
            case 0:
                return 1;
            default:
                return 0;
            }
        }

        public Object getGroup(int groupPosition) {
            switch (groupPosition) {
            case 0:
                return "Data";
            default:
                return null;
            }
        }

        public Object getChild(int groupPosition, int childPosition) {
            if (groupPosition == 0 && childPosition == 0)
                return "child data";
            else
                return null;
        }

        public long getGroupId(int groupPosition) {
            return 0;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        public boolean hasStableIds() {
            return true;
        }

        public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            return null;
        }

        public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            return null;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isEmpty() {
            return true;
        }

        public void onGroupExpanded(int groupPosition) {
        }

        public void onGroupCollapsed(int groupPosition) {
        }

        public long getCombinedChildId(long groupId, long childId) {
            return 0;
        }

        public long getCombinedGroupId(long groupId) {
            return 0;
        }
    }

    private class MockOnGroupExpandListener implements ExpandableListView.OnGroupExpandListener {
        private boolean mCalledOnGroupExpand = false;

        public void onGroupExpand(int groupPosition) {
            mCalledOnGroupExpand = true;
        }

        public boolean hasCalledOnGroupExpand() {
            return mCalledOnGroupExpand;
        }

        public void reset() {
            mCalledOnGroupExpand = false;
        }
    }

    private class MockOnGroupCollapseListener implements
            ExpandableListView.OnGroupCollapseListener {
        private boolean mCalledOnGroupCollapse = false;

        public void onGroupCollapse(int groupPosition) {
            mCalledOnGroupCollapse = true;
        }

        public boolean hasCalledOnGroupCollapse() {
            return mCalledOnGroupCollapse;
        }

        public void reset() {
            mCalledOnGroupCollapse = false;
        }
    }

    private class MockOnItemClickListener implements OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }
    }

    private class MockOnGroupClickListener implements OnGroupClickListener {
        private boolean mCalledOnGroupClick = false;

        public boolean onGroupClick(ExpandableListView parent, View v,
                int groupPosition, long id) {
            mCalledOnGroupClick = true;
            return true;
        }

        public boolean hasCalledOnGroupClick() {
            return mCalledOnGroupClick;
        }
    }

    private class MockOnChildClickListener implements OnChildClickListener {
        private boolean mCalledOnChildClick = false;

        public boolean onChildClick(ExpandableListView parent, View v,
                int groupPosition, int childPosition, long id) {
            mCalledOnChildClick = true;
            return true;
        }

        public boolean hasCalledOnChildClick() {
            return mCalledOnChildClick;
        }
    }

    private class MockExpandableListView extends ExpandableListView {
        public MockExpandableListView(Context context) {
            super(context);
        }

        public MockExpandableListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MockExpandableListView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
        }
    }
}
