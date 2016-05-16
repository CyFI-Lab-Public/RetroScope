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

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.Filter;
import android.widget.FilterQueryProvider;
import android.widget.TextView;

import com.android.cts.stub.R;


/**
 * Test {@link CursorTreeAdapter}.
 */
public class CursorTreeAdapterTest extends AndroidTestCase {
    private static final int NAME_INDEX = 1;
    private static final int VALUE_INDEX = 1;
    private static final String GROUP_ONE         = "group_one";
    private static final String GROUP_TWO         = "group_two";
    private static final String CHILD_VALUE_ONE   = "child_value_one";
    private static final String CHILD_VALUE_TWO   = "child_value_two";
    private static final String CHILD_VALUE_THREE = "child_value_three";
    private static final String[] NAME_PROJECTION = new String[] {
        "_id",           // 0
        "name"           // 1
    };

    private static final String[] VALUE_PROJECTION = new String[] {
        "_id",            // 0
        "value"           // 1
    };

    private SQLiteDatabase mDatabase;
    private File mDatabaseFile;
    private Cursor mGroupCursor;
    private Cursor mChildCursor1;
    private Cursor mChildCursor2;
    private ViewGroup mParent;

    private Cursor createGroupCursor() {
        mDatabase.execSQL("CREATE TABLE group_table (_id INTEGER PRIMARY KEY, name TEXT);");
        mDatabase.execSQL("INSERT INTO group_table (name) VALUES ('" + GROUP_ONE + "');");
        mDatabase.execSQL("INSERT INTO group_table (name) VALUES ('" + GROUP_TWO + "');");
        return mDatabase.query("group_table", NAME_PROJECTION, null, null, null, null, null);
    }

    private Cursor createChild1Cursor() {
        mDatabase.execSQL("CREATE TABLE child1 (_id INTEGER PRIMARY KEY, value TEXT);");
        mDatabase.execSQL("INSERT INTO child1 (value) VALUES ('" + CHILD_VALUE_ONE + "');");
        mDatabase.execSQL("INSERT INTO child1 (value) VALUES ('" + CHILD_VALUE_TWO + "');");
        return getChild1Cursor();
    }

    private Cursor getChild1Cursor() {
        return mDatabase.query("child1", VALUE_PROJECTION, null, null, null, null, null);
    }

    private Cursor createChild2Cursor() {
        mDatabase.execSQL("CREATE TABLE child2 (_id INTEGER PRIMARY KEY, value TEXT);");
        mDatabase.execSQL("INSERT INTO child2 (value) VALUES ('" + CHILD_VALUE_THREE + "');");
        return getChild2Cursor();
    }

    private Cursor getChild2Cursor() {
        return mDatabase.query("child2", VALUE_PROJECTION, null, null, null, null, null);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File dbDir = getContext().getDir("tests", Context.MODE_WORLD_WRITEABLE);
        mDatabaseFile = new File(dbDir, "database_test.db");
        if (mDatabaseFile.exists()) {
            mDatabaseFile.delete();
        }
        mDatabase = SQLiteDatabase.openOrCreateDatabase(mDatabaseFile.getPath(), null);
        assertNotNull(mDatabase);

        mGroupCursor = createGroupCursor();
        assertNotNull(mGroupCursor);

        mChildCursor1 = createChild1Cursor();
        assertNotNull(mChildCursor1);

        mChildCursor2 = createChild2Cursor();
        assertNotNull(mChildCursor2);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        mParent = (ViewGroup)inflater.inflate(R.layout.cursoradapter_host, null);
        assertNotNull(mParent);
    }

    @Override
    protected void tearDown() throws Exception {
        if (null != mGroupCursor) {
            mGroupCursor.close();
            mGroupCursor = null;
        }

        if (null != mChildCursor1) {
            mChildCursor1.close();
            mChildCursor1 = null;
        }

        if (null != mChildCursor2) {
            mChildCursor2.close();
            mChildCursor2 = null;
        }
        mDatabase.close();
        mDatabaseFile.delete();
        super.tearDown();
    }

    public void testConstructor() {
        new MockCursorTreeAdapter(mGroupCursor, mContext);

        new MockCursorTreeAdapter(null, null);

        new MockCursorTreeAdapter(mGroupCursor, mContext, true);

        new MockCursorTreeAdapter(null, null, false);
    }

    public void testGetCursor() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertSame(mGroupCursor, adapter.getCursor());

        adapter.changeCursor(null);
        assertNull(adapter.getCursor());

        adapter.setGroupCursor(mGroupCursor);
        assertSame(mGroupCursor, adapter.getCursor());
    }

    public void testSetGroupCursor() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertSame(mGroupCursor, adapter.getCursor());

        adapter.setGroupCursor(null);
        assertNull(adapter.getCursor());

        adapter.setGroupCursor(mGroupCursor);
        assertSame(mGroupCursor, adapter.getCursor());
    }

    public void testSetChildrenCursor() {
        MockCursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertTrue(mGroupCursor.moveToFirst());

        assertSame(mChildCursor1, adapter.getChild(0, 0));
        adapter.setChildrenCursor(0, mChildCursor2);
        assertSame(mChildCursor2, adapter.getChild(0, 0));
    }

    public void testChangeCursor() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(null, mContext);
        assertNull(adapter.getCursor());

        adapter.changeCursor(mGroupCursor);
        assertSame(mGroupCursor, adapter.getCursor());

        adapter.changeCursor(null);
        assertNull(adapter.getCursor());
    }

    public void testNotifyDataSetChangedBoolean() {
        MockCursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        MockDataSetObserver observer = new MockDataSetObserver();
        adapter.registerDataSetObserver(observer);

        // mChildrenCursorHelpers is empty
        assertFalse(observer.hasCalledOnChanged());
        adapter.notifyDataSetChanged(false);
        assertTrue(observer.hasCalledOnChanged());

        // add group 0 into mChildrenCursorHelpers
        adapter.getChild(0, 0);
        // add group 1 into mChildrenCursorHelpers
        adapter.getChild(1, 0);
        observer.reset();
        assertFalse(observer.hasCalledOnChanged());

        adapter.notifyDataSetChanged(true);
        assertTrue(observer.hasCalledOnChanged());
        adapter.reset();
        adapter.getChild(0, 0);
        assertTrue(adapter.hasAddedChild1IntoCache());
        adapter.getChild(1, 0);
        assertTrue(adapter.hasAddedChild2IntoCache());

        // both group 0 and group 1 are in mChildrenCursorHelpers
        observer.reset();
        assertFalse(observer.hasCalledOnChanged());
        // does not release cursors
        adapter.notifyDataSetChanged(false);
        assertTrue(observer.hasCalledOnChanged());
        adapter.reset();
        adapter.getChild(0, 0);
        assertFalse(adapter.hasAddedChild1IntoCache());
        adapter.getChild(1, 0);
        assertFalse(adapter.hasAddedChild2IntoCache());
    }

    public void testNotifyDataSetChanged() {
        MockCursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        MockDataSetObserver observer = new MockDataSetObserver();
        adapter.registerDataSetObserver(observer);

        // mChildrenCursorHelpers is empty
        assertFalse(observer.hasCalledOnChanged());
        adapter.notifyDataSetChanged();
        assertTrue(observer.hasCalledOnChanged());

        // add group 0 into mChildrenCursorHelpers
        adapter.getChild(0, 0);
        // add group 1 into mChildrenCursorHelpers
        adapter.getChild(1, 0);
        observer.reset();
        assertFalse(observer.hasCalledOnChanged());
        adapter.notifyDataSetChanged();
        assertTrue(observer.hasCalledOnChanged());
        adapter.reset();
        adapter.getChild(0, 0);
        assertTrue(adapter.hasAddedChild1IntoCache());
        adapter.getChild(1, 0);
        assertTrue(adapter.hasAddedChild2IntoCache());
    }

    public void testNotifyDataSetInvalidated() {
        MockCursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        MockDataSetObserver observer = new MockDataSetObserver();
        adapter.registerDataSetObserver(observer);

        assertFalse(observer.hasCalledOnInvalidated());
        // mChildrenCursorHelpers is empty
        adapter.notifyDataSetInvalidated();
        assertTrue(observer.hasCalledOnInvalidated());

        // add group 0 into mChildrenCursorHelpers
        adapter.getChild(0, 0);
        // add group 1 into mChildrenCursorHelpers
        adapter.getChild(1, 0);
        observer.reset();
        assertFalse(observer.hasCalledOnInvalidated());
        adapter.notifyDataSetInvalidated();
        assertTrue(observer.hasCalledOnInvalidated());
        adapter.reset();
        adapter.getChild(0, 0);
        assertTrue(adapter.hasAddedChild1IntoCache());
        adapter.getChild(1, 0);
        assertTrue(adapter.hasAddedChild2IntoCache());
    }

    public void testOnGroupCollapsed() {
        MockCursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);

        // mChildrenCursorHelpers is empty
        adapter.onGroupCollapsed(0);

        // add group 0 into mChildrenCursorHelpers
        adapter.getChild(0, 0);
        // add group 1 into mChildrenCursorHelpers
        adapter.getChild(1, 0);
        adapter.reset();

        // remove group 0 from mChildrenCursorHelpers
        adapter.onGroupCollapsed(0);
        // add group 0 into mChildrenCursorHelpers again
        adapter.getChild(0, 0);
        assertTrue(adapter.hasAddedChild1IntoCache());
        // should not add group 1 into mChildrenCursorHelpers again
        adapter.getChild(1, 0);
        assertFalse(adapter.hasAddedChild2IntoCache());

        adapter.reset();
        adapter.onGroupCollapsed(1);
        adapter.getChild(0, 0);
        assertFalse(adapter.hasAddedChild1IntoCache());
        adapter.getChild(1, 0);
        assertTrue(adapter.hasAddedChild2IntoCache());

        // groupPosition is out of bound
        assertEquals(2, adapter.getGroupCount());
        try {
            adapter.onGroupCollapsed(2);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testHasStableIds() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertTrue(adapter.hasStableIds());

        adapter  = new MockCursorTreeAdapter(null, null);
        assertTrue(adapter.hasStableIds());
    }

    public void testIsChildSelectable() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertTrue(adapter.isChildSelectable(0, 0));
        assertTrue(adapter.isChildSelectable(100, 100));

        adapter  = new MockCursorTreeAdapter(null, null);
        assertTrue(adapter.isChildSelectable(0, 0));
    }

    public void testConvertToString() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertEquals("", adapter.convertToString(null));

        assertEquals(mGroupCursor.toString(), adapter.convertToString(mGroupCursor));
    }

    public void testGetFilter() {
        MockCursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        Filter filter = adapter.getFilter();
        assertNotNull(filter);
        // to prove filter instanceof CursorFilter
        assertFalse(adapter.hasCalledConvertToString());
        assertEquals(adapter.convertToString(mGroupCursor),
                filter.convertResultToString(mGroupCursor));
        assertTrue(adapter.hasCalledConvertToString());
    }

    public void testAccessQueryProvider() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        FilterQueryProvider filterProvider = new MockFilterQueryProvider();
        assertNotNull(filterProvider);

        assertNull(adapter.getFilterQueryProvider());

        adapter.setFilterQueryProvider(filterProvider);
        assertSame(filterProvider, adapter.getFilterQueryProvider());
    }

    public void testRunQueryOnBackgroundThread() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        final String constraint = "constraint";

        // FilterQueryProvider is null, return mGroupCursor
        assertSame(mGroupCursor, adapter.runQueryOnBackgroundThread(constraint));

        FilterQueryProvider filterProvider = new MockFilterQueryProvider();
        assertNotNull(filterProvider);
        adapter.setFilterQueryProvider(filterProvider);
        assertNull(adapter.runQueryOnBackgroundThread(constraint));
    }

    public void testGetGroup() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(null, mContext);

        // null group cursor
        assertNull(adapter.getGroup(0));

        adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        Cursor retCursor = adapter.getGroup(0);
        assertEquals(2, retCursor.getCount());
        assertEquals(GROUP_ONE, retCursor.getString(NAME_INDEX));

        retCursor = adapter.getGroup(1);
        assertEquals(GROUP_TWO, retCursor.getString(NAME_INDEX));

        // groupPosition is out of bound
        assertEquals(2, adapter.getGroupCount());
        assertNull(adapter.getGroup(2));
    }

    public void testGetGroupCount() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertEquals(mGroupCursor.getCount(), adapter.getGroupCount());

        adapter.setGroupCursor(null);
        assertEquals(0, adapter.getGroupCount());
    }

    public void testGetGroupId() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(null, mContext);

        // null group cursor
        assertEquals(0, adapter.getGroupId(0));

        adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertEquals(1, adapter.getGroupId(0));
        assertEquals(2, adapter.getGroupId(1));

        // groupPosition is out of bound
        assertEquals(2, adapter.getGroupCount());
        assertEquals(0, adapter.getGroupId(2));
    }

    public void testGetGroupView() {
        final String expectedStr = "getGroupView test";
        TextView retView;
        TextView textView = new TextView(mContext);
        textView.setText(expectedStr);
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(null, mContext);

        // null cursor
        try {
            assertNull(adapter.getCursor());
            adapter.getGroupView(0, true, textView, mParent);
            fail("does not throw IllegalStateException when cursor is invalid");
        } catch (IllegalStateException e) {
        }

        // groupPosition is out of bound
        adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        try {
            assertEquals(2, adapter.getGroupCount());
            adapter.getGroupView(10, true, textView, mParent);
            fail("does not throw IllegalStateException when position is out of bound");
        } catch (IllegalStateException e) {
        }

        // null convertView
        retView = (TextView) adapter.getGroupView(1, true, null, mParent);
        assertNotNull(retView);
        assertEquals(GROUP_TWO, retView.getText().toString());

        // convertView is not null
        retView = (TextView) adapter.getGroupView(0, true, textView, mParent);
        assertNotNull(retView);
        assertEquals(GROUP_ONE, retView.getText().toString());
    }

    public void testGetChild() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertEquals(2, adapter.getGroupCount());
        // groupPosition is out of bound.
        try {
            adapter.getChild(3, 1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        Cursor retCursor = adapter.getChild(0, 0);
        assertSame(mChildCursor1, retCursor);
        assertEquals(CHILD_VALUE_ONE, retCursor.getString(VALUE_INDEX));

        retCursor = adapter.getChild(0, 1);
        assertSame(mChildCursor1, retCursor);
        assertEquals(CHILD_VALUE_TWO, retCursor.getString(VALUE_INDEX));

        // childPosition is out of bound
        assertEquals(2, adapter.getChildrenCount(0));
        assertNull(adapter.getChild(0, 2));

        retCursor = adapter.getChild(1, 0);
        assertSame(mChildCursor2, retCursor);
        assertEquals(CHILD_VALUE_THREE, retCursor.getString(VALUE_INDEX));
    }

    public void testGetChildId() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(null, mContext);

        // null group cursor
        try {
            adapter.getChildId(0, 0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertEquals(1, adapter.getChildId(0, 0));
        assertEquals(2, adapter.getChildId(0, 1));
        assertEquals(1, adapter.getChildId(1, 0));

        // groupPosition is out of bound
        assertEquals(2, adapter.getGroupCount());
        try {
            adapter.getChildId(3, 0);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        // childPosition is out of bound
        assertEquals(2, adapter.getChildrenCount(0));
        assertEquals(0, adapter.getChildId(0, 2));
    }

    public void testGetChildrenCount() {
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(null, mContext);

        // null group cursor
        assertEquals(0, adapter.getChildrenCount(0));

        adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        assertEquals(2, adapter.getChildrenCount(0));
        assertEquals(1, adapter.getChildrenCount(1));

        // groupPosition is out of bound
        assertEquals(2, adapter.getGroupCount());
        assertEquals(0, adapter.getChildrenCount(2));
    }

    public void testGetChildView() {
        final String expectedStr = "getChildView test";
        TextView retView;
        TextView textView = new TextView(mContext);
        textView.setText(expectedStr);
        CursorTreeAdapter adapter = new MockCursorTreeAdapter(null, mContext);

        // null cursor
        assertNull(adapter.getCursor());
        try {
            adapter.getChildView(0, 0, true, textView, mParent);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        // groupPosition is out of bound
        adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        try {
            adapter.getChildView(10, 0, true, textView, mParent);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        // childPosition is out of bound
        adapter = new MockCursorTreeAdapter(mGroupCursor, mContext);
        try {
            assertEquals(2, adapter.getChildrenCount(0));
            adapter.getChildView(0, 2, true, textView, mParent);
            fail("does not throw IllegalStateException when position is out of bound");
        } catch (IllegalStateException e) {
        }

        // null convertView
        retView = (TextView) adapter.getChildView(1, 0, true, null, mParent);
        assertNotNull(retView);
        assertEquals(CHILD_VALUE_THREE, retView.getText().toString());

        // convertView is not null
        retView = (TextView) adapter.getChildView(0, 1, true, textView, mParent);
        assertNotNull(retView);
        assertEquals(CHILD_VALUE_TWO, retView.getText().toString());
    }

    private final class MockCursorTreeAdapter extends CursorTreeAdapter {
        private boolean mHasAddedChild1IntoCache = false;
        private boolean mHasAddedChild2IntoCache = false;
        private boolean mHasCalledConvertToString = false;

        public MockCursorTreeAdapter(Cursor cursor, Context context) {
            super(cursor, context);
        }

        public MockCursorTreeAdapter(Cursor cursor, Context context, boolean autoRequery) {
            super(cursor, context, autoRequery);
        }

        public boolean hasCalledConvertToString() {
            return mHasCalledConvertToString;
        }

        @Override
        public String convertToString(Cursor cursor) {
            mHasCalledConvertToString = true;
            return super.convertToString(cursor);
        }

        @Override
        public void bindChildView(View view, Context context,
                Cursor cursor, boolean isLastChild) {
            if (null == context || null == cursor || null == view) {
                return;
            }

            if (view instanceof TextView) {
                String name = cursor.getString(VALUE_INDEX);
                TextView textView = (TextView) view;
                textView.setText(name);
            }
        }

        @Override
        public void bindGroupView(View view, Context context,
                Cursor cursor, boolean isExpanded) {
            if (null == context || null == cursor || null == view) {
                return;
            }

            if (view instanceof TextView) {
                String name = cursor.getString(NAME_INDEX);
                TextView textView = (TextView) view;
                textView.setText(name);
            }
        }

        public boolean hasAddedChild1IntoCache() {
            return mHasAddedChild1IntoCache;
        }

        public boolean hasAddedChild2IntoCache() {
            return mHasAddedChild2IntoCache;
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            if (null == groupCursor) {
                return null;
            }

            if (0 == groupCursor.getPosition()) {
                mHasAddedChild1IntoCache = true;
                if (mChildCursor1.isClosed()) {
                    mChildCursor1 = getChild1Cursor();
                }
                return mChildCursor1;
            } else if (1 == groupCursor.getPosition()) {
                mHasAddedChild2IntoCache = true;
                if (mChildCursor2.isClosed()) {
                    mChildCursor2 = getChild2Cursor();
                }
                return mChildCursor2;
            }
            return null;
        }

        @Override
        public View newChildView(Context context, Cursor cursor,
                boolean isLastChild, ViewGroup parent) {
            if (null == context || null == cursor || null == parent) {
                return null;
            }

            String childValue = cursor.getString(VALUE_INDEX);
            TextView textView = new TextView(context);
            textView.setText(childValue);
            return textView;
        }

        @Override
        public View newGroupView(Context context, Cursor cursor,
                boolean isExpanded, ViewGroup parent) {
            if (null == context || null == cursor || null == parent) {
                return null;
            }

            String groupName = cursor.getString(NAME_INDEX);
            TextView textView = new TextView(context);
            textView.setText(groupName);
            return textView;
        }

        public void reset() {
            mHasAddedChild1IntoCache = false;
            mHasAddedChild2IntoCache = false;
            mHasCalledConvertToString = false;
        }
    }

    private final class MockFilterQueryProvider implements FilterQueryProvider {
        public Cursor runQuery(CharSequence constraint) {
            return null;
        }
    }

    private final class MockDataSetObserver extends DataSetObserver {
        private boolean mHasCalledOnChanged = false;
        private boolean mHasCalledOnInvalidated = false;

        @Override
        public void onChanged() {
            super.onChanged();
            mHasCalledOnChanged = true;
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mHasCalledOnInvalidated = true;
        }

        public boolean hasCalledOnChanged() {
            return mHasCalledOnChanged;
        }

        public boolean hasCalledOnInvalidated() {
            return mHasCalledOnInvalidated;
        }

        public void reset() {
            mHasCalledOnChanged = false;
            mHasCalledOnInvalidated = false;
        }
    }
}
