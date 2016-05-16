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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.database.DataSetObserver;
import android.test.AndroidTestCase;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.android.cts.stub.R;


public class ArrayAdapterTest extends AndroidTestCase {

    private static final int INVALD_ID = -1;
    private static final String STR1 = "string1";
    private static final String STR2 = "string2";
    private static final String STR3 = "string3";

    private ArrayAdapter<String> mArrayAdapter;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
          mArrayAdapter = new ArrayAdapter<String>(mContext, R.layout.simple_dropdown_item_1line);
    }

    public void testConstructor() {

        new ArrayAdapter<String>(mContext, R.layout.simple_dropdown_item_1line);
        new ArrayAdapter<String>(mContext, INVALD_ID);// invalid resource id

        new ArrayAdapter<String>(mContext, R.layout.simple_dropdown_item_1line, R.id.text1);
        new ArrayAdapter<String>(mContext, R.layout.simple_dropdown_item_1line, INVALD_ID);

        new ArrayAdapter<String>(mContext, R.layout.simple_dropdown_item_1line,
                new String[] {"str1", "str2"});

        new ArrayAdapter<String>(mContext, R.layout.simple_dropdown_item_1line, R.id.text1,
                new String[] {"str1", "str2"});

        List<String> list = new ArrayList<String>();
        list.add(STR1);
        list.add(STR2);

        new ArrayAdapter<String>(mContext, R.layout.simple_dropdown_item_1line, list);

        new ArrayAdapter<String>(mContext, R.layout.simple_dropdown_item_1line, R.id.text1, list);

        // invalid input
        try {
            new ArrayAdapter<String>(null, R.layout.simple_dropdown_item_1line);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected exception
        }
    }

    public void testDataChangeEvent() {
        final MockDataSetObserver mockDataSetObserver = new MockDataSetObserver();
        mArrayAdapter.registerDataSetObserver(mockDataSetObserver);

        // enable automatically notifying.
        mArrayAdapter.setNotifyOnChange(true);
        assertEquals(0, mockDataSetObserver.getCalledOnChangedCount());
        mArrayAdapter.add(STR1);
        assertEquals(1, mArrayAdapter.getCount());
        assertEquals(1, mockDataSetObserver.getCalledOnChangedCount());
        mArrayAdapter.add(STR2);
        assertEquals(2, mArrayAdapter.getCount());
        assertEquals(2, mockDataSetObserver.getCalledOnChangedCount());

        // reset data
        mArrayAdapter.clear();
        // clear notify changed
        assertEquals(3, mockDataSetObserver.getCalledOnChangedCount());
        assertEquals(0, mArrayAdapter.getCount());
        // if empty before, clear also notify changed
        mArrayAdapter.clear();
        assertEquals(4, mockDataSetObserver.getCalledOnChangedCount());
        mockDataSetObserver.clearCount();
        assertEquals(0, mockDataSetObserver.getCalledOnChangedCount());

        // disable auto notify
        mArrayAdapter.setNotifyOnChange(false);

        mArrayAdapter.add(STR3);
        assertEquals(1, mArrayAdapter.getCount());
        assertEquals(0, mockDataSetObserver.getCalledOnChangedCount());

        // manually notify
        mArrayAdapter.notifyDataSetChanged();
        assertEquals(1, mockDataSetObserver.getCalledOnChangedCount());
        // no data changed, but force notify
        mArrayAdapter.notifyDataSetChanged();
        assertEquals(2, mockDataSetObserver.getCalledOnChangedCount());
        // once called notify, auto notify enabled
        mArrayAdapter.add(STR3);
        assertEquals(3, mockDataSetObserver.getCalledOnChangedCount());
    }

    public void testAccessView() {
        final TextView textView = new TextView(mContext);
        textView.setText(STR3);

        assertNotNull(mArrayAdapter.getContext());

        assertEquals(0, mArrayAdapter.getCount());

        mArrayAdapter.add(STR1);
        mArrayAdapter.add(STR2);
        mArrayAdapter.add(STR3);

        assertEquals(3, mArrayAdapter.getCount());

        assertEquals(STR1, ((TextView) mArrayAdapter.getView(0, null, null)).getText());
        assertEquals(STR2, ((TextView) mArrayAdapter.getView(1, null, null)).getText());
        assertEquals(STR3, ((TextView) mArrayAdapter.getDropDownView(2, null, null)).getText());

        assertEquals(STR3, textView.getText());
        assertSame(textView, mArrayAdapter.getView(0, textView, null));
        assertSame(textView, mArrayAdapter.getDropDownView(0, textView, null));
        assertEquals(STR1, textView.getText());

        try {
            assertEquals(textView, mArrayAdapter.getView(-1, textView, null));
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            assertEquals(textView, mArrayAdapter.getDropDownView(-1, textView, null));
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            assertEquals(textView,
                    mArrayAdapter.getView(mArrayAdapter.getCount(), textView, null));
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            assertEquals(textView,
                    mArrayAdapter.getDropDownView(mArrayAdapter.getCount(), textView, null));
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testGetFilter() {
        Filter filter = mArrayAdapter.getFilter();

        assertNotNull(mArrayAdapter.getFilter());
        assertSame(filter, mArrayAdapter.getFilter());
    }

    /**
     * just simple change the resource id from which the drop view inflate from
     * we set a xml that not contain a textview, so exception should throw to lete us know
     * sucessfully change the dropdown xml, but should not affect the normal view by getview
     */
    public void testSetDropDownViewResouce() {
        mArrayAdapter.add(STR1);

        mArrayAdapter.getDropDownView(0, null, null);

        mArrayAdapter.setDropDownViewResource(R.layout.tabhost_layout);
        // getview is ok
        mArrayAdapter.getView(0, null, null);
        // getDropDownView error for it changed
        try {
            mArrayAdapter.getDropDownView(0, null, null);
            fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected exception
        }

        mArrayAdapter.setDropDownViewResource(INVALD_ID);
    }

    /**
     * insert the item to the specific position, notify data changed
     * check -1, normal, > count
     */
    public void testInsert() {
        mArrayAdapter.setNotifyOnChange(true);
        final MockDataSetObserver mockDataSetObserver = new MockDataSetObserver();
        mArrayAdapter.registerDataSetObserver(mockDataSetObserver);

        mArrayAdapter.insert(STR1, 0);
        assertEquals(1, mArrayAdapter.getCount());
        assertEquals(0, mArrayAdapter.getPosition(STR1));
        assertEquals(1, mockDataSetObserver.getCalledOnChangedCount());

        mArrayAdapter.insert(STR2, 0);
        assertEquals(2, mArrayAdapter.getCount());
        assertEquals(1, mArrayAdapter.getPosition(STR1));
        assertEquals(0, mArrayAdapter.getPosition(STR2));

        mArrayAdapter.insert(STR3, mArrayAdapter.getCount());
        assertEquals(mArrayAdapter.getCount() - 1, mArrayAdapter.getPosition(STR3));

        mArrayAdapter.insert(null, 0);
        assertEquals(0, mArrayAdapter.getPosition(null));

        try {
            mArrayAdapter.insert(STR1, -1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            mArrayAdapter.insert(STR1, mArrayAdapter.getCount() + 1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }
    }

    /**
     * return the given position obj
     * test range: -1, normal, > count
     */
    public void testGetItem() {
        mArrayAdapter.add(STR1);
        mArrayAdapter.add(STR2);
        mArrayAdapter.add(STR3);

        assertSame(STR1, mArrayAdapter.getItem(0));
        assertSame(STR2, mArrayAdapter.getItem(1));
        assertSame(STR3, mArrayAdapter.getItem(2));

        // test invalid input
        try {
            mArrayAdapter.getItem(-1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            mArrayAdapter.getItem(mArrayAdapter.getCount());
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }
    }

    /**
     * just return the given position
     */
    public void testGetItemId() {
        mArrayAdapter.add(STR1);
        mArrayAdapter.add(STR2);
        mArrayAdapter.add(STR3);

        assertEquals(0, mArrayAdapter.getItemId(0));
        assertEquals(1, mArrayAdapter.getItemId(1));
        assertEquals(2, mArrayAdapter.getItemId(2));

        // test invalid input
        assertEquals(-1, mArrayAdapter.getItemId(-1));
        assertEquals(mArrayAdapter.getCount(),
                mArrayAdapter.getItemId(mArrayAdapter.getCount()));
    }

    /*
     * return the obj position that in the array, if there are same objs, return the first one
     */
    public void testGetPosition() {
        mArrayAdapter.add(STR1);
        mArrayAdapter.add(STR2);
        mArrayAdapter.add(STR1);

        assertEquals(0, mArrayAdapter.getPosition(STR1));
        assertEquals(1, mArrayAdapter.getPosition(STR2));
        // return the first one if same obj exsit
        assertEquals(0, mArrayAdapter.getPosition(STR1));

        assertEquals(-1, mArrayAdapter.getPosition(STR3));

        // test invalid input
        assertEquals(-1, mArrayAdapter.getPosition(null));
    }

    /**
     * Removes the specified object from the array. notify data changed
     * remove first one if duplicated string in the array
     */
    public void testRemove() {
        final MockDataSetObserver mockDataSetObserver = new MockDataSetObserver();
        mArrayAdapter.registerDataSetObserver(mockDataSetObserver);
        mArrayAdapter.setNotifyOnChange(true);

        // remove the not exist one
        assertEquals(0, mArrayAdapter.getCount());
        assertEquals(0, mockDataSetObserver.getCalledOnChangedCount());
        // remove the item not exist also notify change
        mArrayAdapter.remove(STR1);
        assertEquals(1, mockDataSetObserver.getCalledOnChangedCount());

        mArrayAdapter.add(STR1);
        mArrayAdapter.add(STR2);
        mArrayAdapter.add(STR3);
        mArrayAdapter.add(STR2);
        mockDataSetObserver.clearCount();
        assertEquals(0, mockDataSetObserver.getCalledOnChangedCount());
        assertEquals(4, mArrayAdapter.getCount());

        mArrayAdapter.remove(STR1);
        assertEquals(3, mArrayAdapter.getCount());
        assertEquals(-1, mArrayAdapter.getPosition(STR1));
        assertEquals(0, mArrayAdapter.getPosition(STR2));
        assertEquals(1, mArrayAdapter.getPosition(STR3));
        assertEquals(1, mockDataSetObserver.getCalledOnChangedCount());

        mArrayAdapter.remove(STR2);
        assertEquals(2, mArrayAdapter.getCount());
        // remove the first one if dumplicated
        assertEquals(1, mArrayAdapter.getPosition(STR2));
        assertEquals(0, mArrayAdapter.getPosition(STR3));

        mArrayAdapter.remove(STR2);
        assertEquals(1, mArrayAdapter.getCount());
        assertEquals(-1, mArrayAdapter.getPosition(STR2));
        assertEquals(0, mArrayAdapter.getPosition(STR3));
    }

    /*
     * Creates a new ArrayAdapter from external resources. The content of the array is
     * obtained through {@link android.content.res.Resources#getTextArray(int)}.
     */
    public void testCreateFromResource() {
        ArrayAdapter.createFromResource(mContext, R.array.string, R.layout.simple_spinner_item);

        // invalid input
        try {
            ArrayAdapter.createFromResource(null, R.array.string, R.layout.simple_spinner_item);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected exception
        }

        try {
            ArrayAdapter.createFromResource(mContext, INVALD_ID, R.layout.simple_spinner_item);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected exception
        }

       ArrayAdapter.createFromResource(mContext, R.array.string, INVALD_ID);
    }

    public void testSort() {
        final MockDataSetObserver mockDataSetObserver = new MockDataSetObserver();
        mArrayAdapter.registerDataSetObserver(mockDataSetObserver);
        mArrayAdapter.setNotifyOnChange(true);
        assertEquals(0, mockDataSetObserver.getCalledOnChangedCount());

        mArrayAdapter.sort( new Comparator<String>() {
            public int compare(String o1, String o2) {
                return 0;
            }
        });
        assertEquals(1, mockDataSetObserver.getCalledOnChangedCount());

        mArrayAdapter.sort(null);
        assertEquals(2, mockDataSetObserver.getCalledOnChangedCount());
    }

    /**
     * insert multiple items via add, notify data changed
     * check count and content
     */
    public void testAdd() {
        mArrayAdapter.setNotifyOnChange(true);
        final MockDataSetObserver mockDataSetObserver = new MockDataSetObserver();
        mArrayAdapter.registerDataSetObserver(mockDataSetObserver);

        mArrayAdapter.clear();
        assertEquals(mArrayAdapter.getCount(), 0);

        mArrayAdapter.add("testing");
        mArrayAdapter.add("android");
        assertEquals(mArrayAdapter.getCount(), 2);
        assertEquals(mArrayAdapter.getItem(0), "testing");
        assertEquals(mArrayAdapter.getItem(1), "android");
    }

    /**
     * insert multiple items via addAll, notify data changed
     * check count and content
     */
    public void testAddAllCollection() {
        mArrayAdapter.setNotifyOnChange(true);
        final MockDataSetObserver mockDataSetObserver = new MockDataSetObserver();
        mArrayAdapter.registerDataSetObserver(mockDataSetObserver);

        List<String> list = new ArrayList<String>();
        list.add("");
        list.add("hello");
        list.add("android");
        list.add("!");

        mArrayAdapter.clear();
        assertEquals(mArrayAdapter.getCount(), 0);

        mArrayAdapter.addAll(list);
        assertEquals(mArrayAdapter.getCount(), list.size());

        assertEquals(mArrayAdapter.getItem(0), list.get(0));
        assertEquals(mArrayAdapter.getItem(1), list.get(1));
        assertEquals(mArrayAdapter.getItem(2), list.get(2));
        assertEquals(mArrayAdapter.getItem(3), list.get(3));
    }

    /**
     * insert multiple items via addAll, notify data changed
     * check count and content
     */
    public void testAddAllParams() {
        mArrayAdapter.setNotifyOnChange(true);
        final MockDataSetObserver mockDataSetObserver = new MockDataSetObserver();
        mArrayAdapter.registerDataSetObserver(mockDataSetObserver);

        mArrayAdapter.clear();
        assertEquals(mArrayAdapter.getCount(), 0);

        mArrayAdapter.addAll("this", "is", "a", "unit", "test");
        assertEquals(mArrayAdapter.getCount(), 5);
        assertEquals(mArrayAdapter.getItem(0), "this");
        assertEquals(mArrayAdapter.getItem(1), "is");
        assertEquals(mArrayAdapter.getItem(2), "a");
        assertEquals(mArrayAdapter.getItem(3), "unit");
        assertEquals(mArrayAdapter.getItem(4), "test");
    }

    private static class MockDataSetObserver extends DataSetObserver {

        private int mCalledOnChangedCount;
        private int mOnCalledInvalidatedCount;

        public MockDataSetObserver() {
            clearCount();
        }

        public int getCalledOnChangedCount() {
            return mCalledOnChangedCount;
        }

        public int getCalledOnInvalidatedCount() {
            return mOnCalledInvalidatedCount;
        }

        public void clearCount() {
            mCalledOnChangedCount = 0;
            mOnCalledInvalidatedCount = 0;
        }

        public void onChanged() {
            mCalledOnChangedCount++;
        }

        public void onInvalidated() {
            mOnCalledInvalidatedCount++;
        }
    }
}
