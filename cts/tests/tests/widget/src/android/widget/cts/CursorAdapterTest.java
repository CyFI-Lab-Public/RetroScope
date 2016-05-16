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
import android.cts.util.PollingCheck;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import android.os.cts.TestThread;
import android.test.AndroidTestCase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filter;
import android.widget.FilterQueryProvider;
import android.widget.TextView;

import com.android.cts.stub.R;


/**
 * Test {@link CursorAdapter}.
 */
public class CursorAdapterTest extends AndroidTestCase {
    private static final long TEST_TIME_OUT = 5000;
    private static final int NUMBER_INDEX = 1;
    private static final String FIRST_NUMBER = "123";
    private static final String SECOND_NUMBER = "5555";
    private static final int FIRST_ITEM_ID = 1;
    private static final int SECOND_ITEM_ID = 2;
    private static final String[] NUMBER_PROJECTION = new String[] {
        "_id",             // 0
        "number"           // 1
    };
    private SQLiteDatabase mDatabase;
    private File mDatabaseFile;
    private Cursor mCursor;
    private ViewGroup mParent;
    private MockCursorAdapter mMockCursorAdapter;

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
        mDatabase.execSQL("CREATE TABLE test (_id INTEGER PRIMARY KEY, number TEXT);");
        mDatabase.execSQL("INSERT INTO test (number) VALUES ('" + FIRST_NUMBER + "');");
        mDatabase.execSQL("INSERT INTO test (number) VALUES ('" + SECOND_NUMBER + "');");
        mCursor = mDatabase.query("test", NUMBER_PROJECTION, null, null, null, null, null);
        assertNotNull(mCursor);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        mParent = (ViewGroup) inflater.inflate(R.layout.cursoradapter_host, null);
        assertNotNull(mParent);
    }

    @Override
    protected void tearDown() throws Exception {
        if (null != mCursor) {
            mCursor.close();
            mCursor = null;
        }
        mDatabase.close();
        mDatabaseFile.delete();
        super.tearDown();
    }

    public void testConstructor() {
        new MockCursorAdapter(mContext, mCursor);

        new MockCursorAdapter(null, null);

        new MockCursorAdapter(mContext, mCursor, true);

        new MockCursorAdapter(null, null, false);
    }

    public void testInit() {
        MockCursorAdapter cursorAdapter = new MockCursorAdapter(null, null, false);
        cursorAdapter.init(null, null, false);
        assertNull(cursorAdapter.getContext());
        assertNull(cursorAdapter.getCursor());
        assertFalse(cursorAdapter.getAutoRequery());
        assertFalse(cursorAdapter.getDataValid());
        assertEquals(-1, cursorAdapter.getRowIDColumn());

        // add context
        cursorAdapter.init(mContext, null, false);
        assertSame(mContext, cursorAdapter.getContext());
        assertNull(cursorAdapter.getCursor());
        assertFalse(cursorAdapter.getAutoRequery());
        assertFalse(cursorAdapter.getDataValid());
        assertEquals(-1, cursorAdapter.getRowIDColumn());

        // add autoRequery
        cursorAdapter.init(mContext, null, true);
        assertSame(mContext, cursorAdapter.getContext());
        assertNull(cursorAdapter.getCursor());
        assertTrue(cursorAdapter.getAutoRequery());
        assertFalse(cursorAdapter.getDataValid());
        assertEquals(-1, cursorAdapter.getRowIDColumn());

        // add cursor
        cursorAdapter.init(mContext, mCursor, true);
        assertSame(mContext, cursorAdapter.getContext());
        assertSame(mCursor, cursorAdapter.getCursor());
        assertTrue(cursorAdapter.getAutoRequery());
        assertTrue(cursorAdapter.getDataValid());
        assertEquals(0, cursorAdapter.getRowIDColumn());
        try {
            mCursor.registerContentObserver(cursorAdapter.getContentObserver());
            fail("the ContentObserver has not been registered");
        } catch (IllegalStateException e) {
        }
        try {
            mCursor.registerDataSetObserver(cursorAdapter.getDataSetObserver());
            fail("the DataSetObserver has not been registered");
        } catch (IllegalStateException e) {
        }
    }

    public void testGetCount() {
        CursorAdapter cursorAdapter = new MockCursorAdapter(mContext, null);
        assertEquals(0, cursorAdapter.getCount());

        cursorAdapter.changeCursor(mCursor);
        assertEquals(mCursor.getCount(), cursorAdapter.getCount());
    }

    public void testAccessCursor() {
        CursorAdapter cursorAdapter = new MockCursorAdapter(mContext, null);
        assertNull(cursorAdapter.getCursor());

        cursorAdapter.changeCursor(mCursor);
        assertSame(mCursor, cursorAdapter.getCursor());

        cursorAdapter.changeCursor(null);
        assertNull(cursorAdapter.getCursor());
    }

    public void testConvertToString() {
        CursorAdapter cursorAdapter = new MockCursorAdapter(mContext, null);
        assertEquals("", cursorAdapter.convertToString(null));

        assertEquals(mCursor.toString(), cursorAdapter.convertToString(mCursor));
    }

    public void testHasStableIds() {
        CursorAdapter cursorAdapter = new MockCursorAdapter(mContext, mCursor);
        assertTrue(cursorAdapter.hasStableIds());

        cursorAdapter  = new MockCursorAdapter(null, null);
        assertTrue(cursorAdapter.hasStableIds());
    }

    public void testGetView() {
        TextView textView = new TextView(mContext);
        textView.setText("getView test");

        MockCursorAdapter cursorAdapter = new MockCursorAdapter(mContext, null);
        // null cursor
        assertFalse(cursorAdapter.getDataValid());
        try {
            cursorAdapter.getView(0, textView, mParent);
            fail("does not throw IllegalStateException when cursor is invalid");
        } catch (IllegalStateException e) {
        }

        cursorAdapter = new MockCursorAdapter(mContext, mCursor);
        try {
            cursorAdapter.getView(100, textView, mParent);
            fail("does not throw IllegalStateException when position is out of bound");
        } catch (IllegalStateException e) {
        }

        // null convertView
        TextView retView = (TextView) cursorAdapter.getView(0, null, mParent);
        assertNotNull(retView);
        assertEquals(FIRST_NUMBER, retView.getText().toString());

        // convertView is not null
        retView = (TextView) cursorAdapter.getView(1, textView, mParent);
        assertNotNull(retView);
        assertEquals(SECOND_NUMBER, retView.getText().toString());
    }

    public void testNewDropDownView() {
        CursorAdapter cursorAdapter = new MockCursorAdapter(mContext, mCursor);
        // null cursor
        assertNull(cursorAdapter.newDropDownView(mContext, null, mParent));

        TextView textView = (TextView) cursorAdapter.newDropDownView(mContext, mCursor, mParent);
        assertEquals(FIRST_NUMBER, textView.getText().toString());
    }

    public void testGetDropDownView() {
        MockCursorAdapter cursorAdapter = new MockCursorAdapter(mContext, null);
        // null cursor
        assertFalse(cursorAdapter.getDataValid());
        TextView textView = new TextView(mContext);
        textView.setText("getDropDownView test");

        assertNull(cursorAdapter.getDropDownView(0, textView, mParent));

        // null convertView
        cursorAdapter = new MockCursorAdapter(mContext, mCursor);
        TextView retView = (TextView) cursorAdapter.getDropDownView(0, null, mParent);
        assertNotNull(retView);
        assertEquals(FIRST_NUMBER, retView.getText().toString());

        // convertView is not null
        retView = (TextView) cursorAdapter.getDropDownView(1, textView, mParent);
        assertNotNull(retView);
        assertEquals(SECOND_NUMBER, retView.getText().toString());
    }

    public void testGetFilter() {
        CursorAdapter cursorAdapter = new MockCursorAdapter(mContext, mCursor);
        Filter filter = cursorAdapter.getFilter();
        assertNotNull(filter);
    }

    public void testGetItem() {
        CursorAdapter cursorAdapter = new MockCursorAdapter(mContext, null);
        // cursor is null
        assertNull(cursorAdapter.getItem(0));

        cursorAdapter = new MockCursorAdapter(mContext, mCursor);
        Cursor c = (Cursor) cursorAdapter.getItem(0);
        assertNotNull(c);
        assertEquals(0, c.getPosition());
        assertEquals(FIRST_NUMBER, c.getString(NUMBER_INDEX));

        c = (Cursor) cursorAdapter.getItem(1);
        assertNotNull(c);
        assertEquals(1, c.getPosition());
        assertEquals(SECOND_NUMBER, c.getString(NUMBER_INDEX));
    }

    public void testGetItemId() {
        CursorAdapter cursorAdapter = new MockCursorAdapter(mContext, null);
        // cursor is null
        assertEquals(0, cursorAdapter.getItemId(0));

        cursorAdapter = new MockCursorAdapter(mContext, mCursor);
        assertEquals(FIRST_ITEM_ID, cursorAdapter.getItemId(0));

        assertEquals(SECOND_ITEM_ID, cursorAdapter.getItemId(1));

        // position is out of bound
        assertEquals(0, cursorAdapter.getItemId(2));
    }

    public void testAccessFilterQueryProvider() {
        CursorAdapter cursorAdapter = new MockCursorAdapter(mContext, mCursor);
        FilterQueryProvider filterProvider = new MockFilterQueryProvider();
        assertNotNull(filterProvider);

        assertNull(cursorAdapter.getFilterQueryProvider());

        cursorAdapter.setFilterQueryProvider(filterProvider);
        assertSame(filterProvider, cursorAdapter.getFilterQueryProvider());
    }

    public void testRunQueryOnBackgroundThread() {
        CursorAdapter cursorAdapter = new MockCursorAdapter(mContext, mCursor);
        final String constraint = "constraint";

        // FilterQueryProvider is null, return mCursor
        assertSame(mCursor, cursorAdapter.runQueryOnBackgroundThread(constraint));

        FilterQueryProvider filterProvider = new MockFilterQueryProvider();
        assertNotNull(filterProvider);
        cursorAdapter.setFilterQueryProvider(filterProvider);
        assertNull(cursorAdapter.runQueryOnBackgroundThread(constraint));
    }

    public void testOnContentChanged() throws Throwable {
        TestThread testThread = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();
                mMockCursorAdapter = new MockCursorAdapter(mContext, mCursor);
            }
        });
        testThread.runTest(TEST_TIME_OUT);
        assertFalse(mMockCursorAdapter.hasContentChanged());
        // insert a new row
        mDatabase.execSQL("INSERT INTO test (number) VALUES ('" + FIRST_NUMBER + "');");
        new PollingCheck(TEST_TIME_OUT) {
            @Override
            protected boolean check() {
                return mMockCursorAdapter.hasContentChanged();
            }
        };
    }

    private final class MockCursorAdapter extends CursorAdapter {
        private boolean mContentChanged = false;

        public MockCursorAdapter(Context context, Cursor c) {
            super(context, c);
        }

        public MockCursorAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        public Context getContext() {
            return mContext;
        }

        public boolean getAutoRequery() {
            return mAutoRequery;
        }

        public boolean getDataValid() {
            return mDataValid;
        }

        public int getRowIDColumn() {
            return mRowIDColumn;
        }

        public ContentObserver getContentObserver() {
            return mChangeObserver;
        }

        public DataSetObserver getDataSetObserver() {
            return mDataSetObserver;
        }

        @Override
        public void init(Context context, Cursor c, boolean autoRequery) {
            super.init(context, c, autoRequery);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            if (null == context || null == cursor || null == view) {
                return;
            }

            if (view instanceof TextView) {
                String number = cursor.getString(NUMBER_INDEX);
                TextView textView = (TextView) view;
                textView.setText(number);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            if (null == context || null == cursor || null == parent) {
                return null;
            }

            if (cursor.moveToFirst()) {
                String number = cursor.getString(NUMBER_INDEX);
                TextView textView = new TextView(context);
                textView.setText(number);
                return textView;
            }
            return null;
        }

        @Override
        protected void onContentChanged() {
            super.onContentChanged();
            mContentChanged = true;
        }

        public boolean hasContentChanged() {
            return mContentChanged;
        }
    }

    private final class MockFilterQueryProvider implements FilterQueryProvider {
        public Cursor runQuery(CharSequence constraint) {
            return null;
        }
    }
}
