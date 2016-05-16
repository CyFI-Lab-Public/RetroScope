/*
 * Copyright (C) 2011 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mailcommon;

import com.android.mailcommon.MergedAdapter.ListSpinnerAdapter;
import com.android.mailcommon.MergedAdapter.LocalAdapterPosition;

import android.database.DataSetObserver;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.HashSet;
import java.util.Set;

@SmallTest
public class MergedAdapterTest extends AndroidTestCase {

    private class TestAdapter extends BaseAdapter implements ListSpinnerAdapter {

        private int mOffset;
        private int mCount;
        private int mViewTypeCount;

        public TestAdapter(int count) {
            mCount = count;
        }

        @Override
        public int getCount() {
            return mCount;
        }

        public void setCount(int count) {
            mCount = count;
            notifyDataSetChanged();
        }

        @Override
        public Object getItem(int position) {
            return Integer.toString(mOffset + position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = new View(mContext);
            v.setTag(getItem(position));
            return v;
        }

        public TestAdapter setOffset(int i) {
            mOffset = i;
            return this;
        }

        public TestAdapter setViewTypeCount(int i) {
            mViewTypeCount = i;
            return this;
        }

        @Override
        public int getViewTypeCount() {
            return mViewTypeCount;
        }

        @Override
        public int getItemViewType(int position) {
            return position % mViewTypeCount;
        }

    }

    private TestAdapter mFirst;
    private TestAdapter mSecond;
    boolean mChanged;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mFirst = new TestAdapter(10).setViewTypeCount(2);
        mSecond = new TestAdapter(5).setOffset(10).setViewTypeCount(3);
    }

    public void testGetSubAdapters() {
        MergedAdapter<ListSpinnerAdapter> adapter = new MergedAdapter<ListSpinnerAdapter>();

        adapter.setAdapters(mFirst, mSecond);
        assertEquals(2, adapter.getSubAdapterCount());
        assertEquals(mFirst, adapter.getSubAdapter(0));
        assertEquals(mSecond, adapter.getSubAdapter(1));
    }

    public void testGetSubAdapterOffset() {
        MergedAdapter<ListSpinnerAdapter> adapter = new MergedAdapter<ListSpinnerAdapter>();

        adapter.setAdapters(mFirst, mSecond);
        LocalAdapterPosition<ListSpinnerAdapter> p = adapter.getAdapterOffsetForItem(13);
        assertEquals(p.mAdapter, mSecond);
        assertEquals(p.mLocalPosition, 3);
    }

    public void testGetItemAndCount() {
        MergedAdapter<ListSpinnerAdapter> adapter = new MergedAdapter<ListSpinnerAdapter>();

        adapter.setAdapters(mFirst, mSecond);
        assertEquals(15, adapter.getCount());
        for (int i = 0; i < adapter.getCount(); i++) {
            assertEquals(Integer.toString(i), adapter.getItem(i));
        }
    }

    public void testGetDropDownView() {
        MergedAdapter<ListSpinnerAdapter> adapter = new MergedAdapter<ListSpinnerAdapter>();

        adapter.setAdapters(mFirst, mSecond);
        for (int i = 0; i < adapter.getCount(); i++) {
            View v = adapter.getDropDownView(i, null, null);
            assertEquals(Integer.toString(i), v.getTag());
        }
    }

    public void testViewTypeCount() {
        MergedAdapter<ListSpinnerAdapter> adapter = new MergedAdapter<ListSpinnerAdapter>();

        adapter.setAdapters(mFirst, mSecond);
        assertEquals(2 + 3, adapter.getViewTypeCount());
        for (int i = 0; i < mFirst.getCount(); i++) {
            assertTrue(adapter.getItemViewType(i) < adapter.getViewTypeCount());
        }
    }

    public void testViewTypesDoNotOverlap() {
        MergedAdapter<ListSpinnerAdapter> adapter = new MergedAdapter<ListSpinnerAdapter>();

        adapter.setAdapters(mFirst, mSecond);

        Set<Integer> firstTypes = new HashSet<Integer>();
        for (int i = 0; i < mFirst.getCount(); i++) {
            firstTypes.add(mFirst.getItemViewType(i));
        }

        for (int i = mFirst.getCount(); i < adapter.getCount(); i++) {
            int secondType = adapter.getItemViewType(i);
            assertFalse(firstTypes.contains(secondType));
        }
    }

    public void testChange() {
        MergedAdapter<ListSpinnerAdapter> adapter = new MergedAdapter<ListSpinnerAdapter>();

        mChanged = false;

        adapter.setAdapters(mFirst, mSecond);
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                mChanged = true;
            }
        });
        mSecond.setCount(3);

        assertEquals(13, adapter.getCount());
        assertTrue(mChanged);
    }

    public void testStopNotifyingChangeAfterRemoval() {
        MergedAdapter<ListSpinnerAdapter> adapter = new MergedAdapter<ListSpinnerAdapter>();

        mChanged = false;

        adapter.setAdapters(mFirst, mSecond);
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                mChanged = true;
            }
        });
        adapter.setAdapters(mFirst);
        mSecond.setCount(3);

        assertFalse(mChanged);
    }

}
