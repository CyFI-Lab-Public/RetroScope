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
import android.test.AndroidTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Test {@link HeaderViewListAdapter}.
 */
public class HeaderViewListAdapterTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testConstructor() {
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>();
        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(5);
        new HeaderViewListAdapter(header, footer, null);

        new HeaderViewListAdapter(header, footer, new HeaderViewEmptyAdapter());
    }

    public void testGetHeadersCount() {
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(0, headerViewListAdapter.getHeadersCount());

        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        header.add(lv.new FixedViewInfo());
        headerViewListAdapter = new HeaderViewListAdapter(header, null, null);
        assertEquals(1, headerViewListAdapter.getHeadersCount());
    }

    public void testGetFootersCount() {
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(0, headerViewListAdapter.getFootersCount());

        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(4);
        footer.add(lv.new FixedViewInfo());
        headerViewListAdapter = new HeaderViewListAdapter(null, footer, null);
        assertEquals(1, headerViewListAdapter.getFootersCount());
    }

    public void testIsEmpty() {
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertTrue(headerViewListAdapter.isEmpty());

        HeaderViewEmptyAdapter emptyAdapter = new HeaderViewEmptyAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(null, null, emptyAdapter);
        assertTrue(headerViewListAdapter.isEmpty());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertFalse(headerViewListAdapter.isEmpty());
    }

    public void testRemoveHeader() {
        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        ListView lv1 = new ListView(getContext());
        ListView lv2 = new ListView(getContext());
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.view = lv1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.view = lv2;
        header.add(info1);
        header.add(info2);
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(header, null, null);
        assertEquals(2, headerViewListAdapter.getHeadersCount());
        assertFalse(headerViewListAdapter.removeHeader(new ListView(getContext())));
        assertTrue(headerViewListAdapter.removeHeader(lv1));
        assertEquals(1, headerViewListAdapter.getHeadersCount());

        headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        try {
            headerViewListAdapter.removeHeader(null);
            //fail("Removing from null header should result in NullPointerException");
        } catch (NullPointerException e) {
            // expected.
        }
    }

    public void testRemoveFooter() {
        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(4);
        ListView lv1 = new ListView(getContext());
        ListView lv2 = new ListView(getContext());
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.view = lv1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.view = lv2;
        footer.add(info1);
        footer.add(info2);
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, footer, null);
        assertEquals(2, headerViewListAdapter.getFootersCount());
        assertFalse(headerViewListAdapter.removeFooter(new ListView(getContext())));
        assertTrue(headerViewListAdapter.removeFooter(lv1));
        assertEquals(1, headerViewListAdapter.getFootersCount());

        headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        try {
            headerViewListAdapter.removeFooter(null);
            //fail("Removing from null footer should result in NullPointerException");
        } catch (NullPointerException e) {
            // expected.
        }
    }

    public void testGetCount() {
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(0, headerViewListAdapter.getCount());

        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        Object data1 = new Object();
        Object data2 = new Object();
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.data = data1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.data = data2;
        header.add(info1);
        header.add(info2);
        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(4);
        Object data3 = new Object();
        Object data4 = new Object();
        ListView.FixedViewInfo info3 = lv.new FixedViewInfo();
        info3.data = data3;
        ListView.FixedViewInfo info4 = lv.new FixedViewInfo();
        info4.data = data4;
        footer.add(info3);
        footer.add(info4);

        HeaderViewEmptyAdapter emptyAdapter = new HeaderViewEmptyAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(header, footer, emptyAdapter);
        // 4 is header's count + footer's count + emptyAdapter's count
        assertEquals(4, headerViewListAdapter.getCount());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(header, footer, fullAdapter);
        // 5 is header's count + footer's count + fullAdapter's count
        assertEquals(5, headerViewListAdapter.getCount());
    }

    public void testAreAllItemsEnabled() {
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertTrue(headerViewListAdapter.areAllItemsEnabled());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertTrue(headerViewListAdapter.areAllItemsEnabled());

        HeaderViewEmptyAdapter emptyAdapter = new HeaderViewEmptyAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(null, null, emptyAdapter);
        assertFalse(headerViewListAdapter.areAllItemsEnabled());
    }

    public void testIsEnabled() {
        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        HeaderViewListAdapter headerViewListAdapter =
            new HeaderViewListAdapter(null, null, fullAdapter);
        assertTrue(headerViewListAdapter.isEnabled(0));
        
        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        header.add(lv.new FixedViewInfo());
        headerViewListAdapter = new HeaderViewListAdapter(header, null, fullAdapter);
        assertFalse(headerViewListAdapter.isEnabled(0));
        assertTrue(headerViewListAdapter.isEnabled(1));

        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(4);
        footer.add(lv.new FixedViewInfo());
        footer.add(lv.new FixedViewInfo());
        headerViewListAdapter = new HeaderViewListAdapter(header, footer, fullAdapter);
        assertFalse(headerViewListAdapter.isEnabled(0));
        assertTrue(headerViewListAdapter.isEnabled(1));
        assertFalse(headerViewListAdapter.isEnabled(2));
        assertFalse(headerViewListAdapter.isEnabled(3));

        headerViewListAdapter = new HeaderViewListAdapter(null, footer, fullAdapter);
        assertTrue(headerViewListAdapter.isEnabled(0));
        assertFalse(headerViewListAdapter.isEnabled(1));
        assertFalse(headerViewListAdapter.isEnabled(2));
    }

    public void testGetItem() {
        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        Object data1 = new Object();
        Object data2 = new Object();
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.data = data1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.data = data2;
        header.add(info1);
        header.add(info2);
        ArrayList<ListView.FixedViewInfo> footer = new ArrayList<ListView.FixedViewInfo>(4);
        Object data3 = new Object();
        Object data4 = new Object();
        ListView.FixedViewInfo info3 = lv.new FixedViewInfo();
        info3.data = data3;
        ListView.FixedViewInfo info4 = lv.new FixedViewInfo();
        info4.data = data4;
        footer.add(info3);
        footer.add(info4);

        HeaderViewFullAdapter headerViewFullAdapter = new HeaderViewFullAdapter();
        HeaderViewListAdapter headerViewListAdapter =
            new HeaderViewListAdapter(header, footer, headerViewFullAdapter);
        assertSame(data1, headerViewListAdapter.getItem(0));
        assertSame(data2, headerViewListAdapter.getItem(1));
        assertSame(headerViewFullAdapter.getItem(0), headerViewListAdapter.getItem(2));
        assertSame(data3, headerViewListAdapter.getItem(3));
        assertSame(data4, headerViewListAdapter.getItem(4));
    }

    public void testGetItemId() {
        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        ListView lv1 = new ListView(getContext());
        ListView lv2 = new ListView(getContext());
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.view = lv1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.view = lv2;
        header.add(info1);
        header.add(info2);

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        HeaderViewListAdapter headerViewListAdapter =
            new HeaderViewListAdapter(header, null, fullAdapter);
        assertEquals(-1, headerViewListAdapter.getItemId(0));
        assertEquals(fullAdapter.getItemId(0), headerViewListAdapter.getItemId(2));
    }

    public void testHasStableIds() {
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertFalse(headerViewListAdapter.hasStableIds());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertTrue(headerViewListAdapter.hasStableIds());
    }

    public void testGetView() {
        ListView lv = new ListView(getContext());
        ArrayList<ListView.FixedViewInfo> header = new ArrayList<ListView.FixedViewInfo>(4);
        ListView lv1 = new ListView(getContext());
        ListView lv2 = new ListView(getContext());
        ListView.FixedViewInfo info1 = lv.new FixedViewInfo();
        info1.view = lv1;
        ListView.FixedViewInfo info2 = lv.new FixedViewInfo();
        info2.view = lv2;
        header.add(info1);
        header.add(info2);

        // No adapter, just header
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(header, null, null);
        assertSame(lv1, headerViewListAdapter.getView(0, null, null));
        assertSame(lv2, headerViewListAdapter.getView(1, null, null));

        // Adapter only
        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        View expected = fullAdapter.getView(0, null, null);
        headerViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertSame(expected, headerViewListAdapter.getView(0, null, null));

        // Header and adapter
        headerViewListAdapter = new HeaderViewListAdapter(header, null, fullAdapter);
        assertSame(lv1, headerViewListAdapter.getView(0, null, null));
        assertSame(lv2, headerViewListAdapter.getView(1, null, null));
        assertSame(expected, headerViewListAdapter.getView(2, null, null));
    }

    public void testGetItemViewType() {
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER,
                headerViewListAdapter.getItemViewType(0));

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertEquals(AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER,
                headerViewListAdapter.getItemViewType(-1));
        assertEquals(0, headerViewListAdapter.getItemViewType(0));
        assertEquals(AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER,
                headerViewListAdapter.getItemViewType(2));
    }

    public void testGetViewTypeCount() {
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertEquals(1, headerViewListAdapter.getViewTypeCount());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertEquals(fullAdapter.getViewTypeCount(), headerViewListAdapter.getViewTypeCount());
    }

    public void testRegisterDataSetObserver() {
        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        HeaderViewListAdapter headerViewListAdapter =
            new HeaderViewListAdapter(null, null, fullAdapter);
        DataSetObserver observer = new HeaderViewDataSetObserver();
        headerViewListAdapter.registerDataSetObserver(observer);
        assertSame(observer, fullAdapter.getDataSetObserver());
    }

    public void testUnregisterDataSetObserver() {
        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        HeaderViewListAdapter headerViewListAdapter =
            new HeaderViewListAdapter(null, null, fullAdapter);
        DataSetObserver observer = new HeaderViewDataSetObserver();
        headerViewListAdapter.registerDataSetObserver(observer);

        headerViewListAdapter.unregisterDataSetObserver(null);
        assertSame(observer, fullAdapter.getDataSetObserver());
        headerViewListAdapter.unregisterDataSetObserver(observer);
        assertNull(fullAdapter.getDataSetObserver());
    }

    public void testGetFilter() {
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertNull(headerViewListAdapter.getFilter());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertNull(headerViewListAdapter.getFilter());

        HeaderViewEmptyAdapter emptyAdapter = new HeaderViewEmptyAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(null, null, emptyAdapter);
        assertSame(emptyAdapter.getFilter(), headerViewListAdapter.getFilter());
    }

    public void testGetWrappedAdapter() {
        HeaderViewListAdapter headerViewListAdapter = new HeaderViewListAdapter(null, null, null);
        assertNull(headerViewListAdapter.getWrappedAdapter());

        HeaderViewFullAdapter fullAdapter = new HeaderViewFullAdapter();
        headerViewListAdapter = new HeaderViewListAdapter(null, null, fullAdapter);
        assertSame(fullAdapter, headerViewListAdapter.getWrappedAdapter());
    }

    private class HeaderViewEmptyAdapter implements ListAdapter, Filterable {
        private final HeaderViewFilterTest mFilter;

        public HeaderViewEmptyAdapter() {
            mFilter = new HeaderViewFilterTest();
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return true;
        }

        public void registerDataSetObserver(DataSetObserver observer) {
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        public int getCount() {
            return 0;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean hasStableIds() {
            return false;
        }
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }

        public int getItemViewType(int position) {
            return 0;
        }
        public int getViewTypeCount() {
            return 1;
        }

        public boolean isEmpty() {
            return true;
        }

        public Filter getFilter() {
            return mFilter;
        }
    }

    private class HeaderViewFullAdapter implements ListAdapter {
        private DataSetObserver mObserver;
        private Object mItem;
        private final View mView = new View(getContext());

        public DataSetObserver getDataSetObserver() {
            return mObserver;
        }

        public boolean areAllItemsEnabled() {
            return true;
        }

        public boolean isEnabled(int position) {
            return true;
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            mObserver = observer;
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (mObserver == observer) {
                mObserver = null;
            }
        }

        public int getCount() {
            return 1;
        }

        public Object getItem(int position) {
            if (mItem == null) {
                mItem = new Object();
            }
            return mItem;
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean hasStableIds() {
            return true;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return mView;
        }

        public int getItemViewType(int position) {
            return 0;
        }

        public int getViewTypeCount() {
            return 1;
        }

        public boolean isEmpty() {
            return false;
        }
    }

    private static class HeaderViewFilterTest extends Filter {
        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
        }
    }

    private class HeaderViewDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            // Do nothing
        }

        @Override
        public void onInvalidated() {
            // Do nothing
        }
    }
}
