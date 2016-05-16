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

import android.database.DataSetObserver;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

/**
 * Test {@link BaseExpandableListAdapter}.
 */
public class BaseExpandableListAdapterTest extends InstrumentationTestCase {
    public void testAreAllItemsEnabled() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        assertTrue(adapter.areAllItemsEnabled());
    }

    public void testGetCombinedId() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();

        long childID = adapter.getCombinedChildId(10, 100);
        long groupID = adapter.getCombinedGroupId(10);

        // there should be no clash in group and child IDs
        assertTrue(childID != groupID);

        childID = adapter.getCombinedChildId(0, 0);
        groupID = adapter.getCombinedGroupId(0);
        assertTrue(childID != groupID);
    }

    public void testIsEmpty() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        assertTrue(adapter.isEmpty());
        adapter.setGroupCount(10);
        assertFalse(adapter.isEmpty());
    }

    public void testNotifyDataSetChanged() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        MockDataSetObserver dataSetObserver = new MockDataSetObserver();
        adapter.registerDataSetObserver(dataSetObserver);

        assertFalse(dataSetObserver.hasCalledOnChanged());
        adapter.notifyDataSetChanged();
        assertTrue(dataSetObserver.hasCalledOnChanged());
    }

    public void testNotifyDataSetInvalidated() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        MockDataSetObserver dataSetObserver = new MockDataSetObserver();
        adapter.registerDataSetObserver(dataSetObserver);

        assertFalse(dataSetObserver.hasCalledOnInvalidated());
        adapter.notifyDataSetInvalidated();
        assertTrue(dataSetObserver.hasCalledOnInvalidated());
    }

    public void testOnGroupCollapsed() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        // this function is non-operation.
        adapter.onGroupCollapsed(0);
    }

    public void testOnGroupExpanded() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        // this function is non-operation.
        adapter.onGroupExpanded(0);
    }

    public void testDataSetObserver() {
        MockBaseExpandableListAdapter adapter = new MockBaseExpandableListAdapter();
        MockDataSetObserver dataSetObserver = new MockDataSetObserver();
        adapter.registerDataSetObserver(dataSetObserver);

        assertFalse(dataSetObserver.hasCalledOnChanged());
        adapter.notifyDataSetChanged();
        assertTrue(dataSetObserver.hasCalledOnChanged());

        dataSetObserver.reset();
        assertFalse(dataSetObserver.hasCalledOnChanged());
        adapter.unregisterDataSetObserver(dataSetObserver);
        adapter.notifyDataSetChanged();
        assertFalse(dataSetObserver.hasCalledOnChanged());
    }

    private class MockDataSetObserver extends DataSetObserver {
        private boolean mCalledOnChanged = false;
        private boolean mCalledOnInvalidated = false;

        @Override
        public void onChanged() {
            super.onChanged();
            mCalledOnChanged = true;
        }

        public boolean hasCalledOnChanged() {
            return mCalledOnChanged;
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mCalledOnInvalidated = true;
        }

        public boolean hasCalledOnInvalidated() {
            return mCalledOnInvalidated;
        }

        public void reset() {
            mCalledOnChanged = false;
        }
    }

    private class MockBaseExpandableListAdapter extends BaseExpandableListAdapter {
        private int mGroupCount;

        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            return null;
        }

        public int getChildrenCount(int groupPosition) {
            return 0;
        }

        public Object getGroup(int groupPosition) {
            return null;
        }

        public int getGroupCount() {
            return mGroupCount;
        }

        public void setGroupCount(int count) {
            mGroupCount = count;
        }

        public long getGroupId(int groupPosition) {
            return 0;
        }

        public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            return null;
        }

        public boolean hasStableIds() {
            return false;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }
}
