/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package android.content.cts;


import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.test.AndroidTestCase;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Test {@link ContentQueryMap}.
 */
public class ContentQueryMapTest extends AndroidTestCase {
    private static final int TEST_TIME_OUT = 5000;

    private static final String NAME0  = "name0";
    private static final String VALUE0 = "value0";
    private static final String NAME1  = "name1";
    private static final String VALUE1 = "value1";
    private static final String NAME2  = "name2";
    private static final String VALUE2 = "value2";
    private static final String NAME3  = "name3";
    private static final String VALUE3 = "value3";

    private static final String[] PROJECTIONS = new String[] {
        DummyProvider.NAME, DummyProvider.VALUE};

    private static final int ORIGINAL_ROW_COUNT = 2;
    private ContentResolver mResolver;
    private Cursor mCursor;
    private ContentQueryMap mContentQueryMap;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = mContext.getContentResolver();

        ContentValues values0 = new ContentValues();
        values0.put(DummyProvider.NAME, NAME0);
        values0.put(DummyProvider.VALUE, VALUE0);
        mResolver.insert(DummyProvider.CONTENT_URI, values0);

        ContentValues values1 = new ContentValues();
        values1.put(DummyProvider.NAME, NAME1);
        values1.put(DummyProvider.VALUE, VALUE1);
        mResolver.insert(DummyProvider.CONTENT_URI, values1);

        mCursor = mResolver.query(DummyProvider.CONTENT_URI, PROJECTIONS, null, null, null);
        assertNotNull(mCursor);
    }

    @Override
    protected void tearDown() throws Exception {
        if (mContentQueryMap != null) {
            mContentQueryMap.close();
            mContentQueryMap = null;
        }
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }

        mResolver.delete(DummyProvider.CONTENT_URI, null, null);

        super.tearDown();
    }

    public void testConstructor() {
        new ContentQueryMap(mCursor, DummyProvider.NAME, true, null);

        new ContentQueryMap(mCursor, DummyProvider.VALUE, false, new Handler());

        try {
            new ContentQueryMap(mCursor, null, false, new Handler());
            fail("Should throw NullPointerException if param columnNameOfKey is null");
        } catch (NullPointerException e) {
        }

        try {
            new ContentQueryMap(null, DummyProvider.NAME, false, new Handler());
            fail("Should throw NullPointerException if param cursor is null");
        } catch (NullPointerException e) {
        }
    }

    public void testGetRows() {
        // handler can be null
        mContentQueryMap = new ContentQueryMap(mCursor, DummyProvider.NAME, true, null);
        Map<String, ContentValues> rows = mContentQueryMap.getRows();
        assertEquals(ORIGINAL_ROW_COUNT, rows.size());
        assertTrue(rows.containsKey(NAME0));
        assertEquals(VALUE0, rows.get(NAME0).getAsString(DummyProvider.VALUE));
        assertTrue(rows.containsKey(NAME1));
        assertEquals(VALUE1, rows.get(NAME1).getAsString(DummyProvider.VALUE));
        mContentQueryMap.close();

        // the mCursor has been close
        mContentQueryMap = new ContentQueryMap(mCursor, DummyProvider.NAME, false, new Handler());
        rows = mContentQueryMap.getRows();
        assertFalse(rows.containsKey(NAME0));
        mContentQueryMap.requery();
        rows = mContentQueryMap.getRows();
        assertFalse(rows.containsKey(NAME0));
    }

    public void testRequery() {
        // Disable the keepUpdated to make sure requery() will not be called
        // from somewhere else
        mContentQueryMap = new ContentQueryMap(mCursor, DummyProvider.NAME, false, null);
        ContentValues contentValues = mContentQueryMap.getValues(NAME0);
        assertNotNull(contentValues);
        assertEquals(VALUE0, contentValues.getAsString(DummyProvider.VALUE));

        contentValues = mContentQueryMap.getValues(NAME1);
        assertNotNull(contentValues);
        assertEquals(VALUE1, contentValues.getAsString(DummyProvider.VALUE));

        contentValues = mContentQueryMap.getValues(NAME2);
        assertNull(contentValues);

        // update NAME0 and VALUE0
        ContentValues values = new ContentValues();
        values.put(DummyProvider.NAME, NAME2);
        values.put(DummyProvider.VALUE, VALUE2);
        mResolver.update(DummyProvider.CONTENT_URI, values,
                DummyProvider.NAME + " = '" + NAME0 + "'", null);
        mContentQueryMap.requery();

        contentValues = mContentQueryMap.getValues(NAME0);
        assertNull(contentValues);

        contentValues = mContentQueryMap.getValues(NAME1);
        assertNotNull(contentValues);
        assertEquals(VALUE1, contentValues.getAsString(DummyProvider.VALUE));

        contentValues = mContentQueryMap.getValues(NAME2);
        assertNotNull(contentValues);
        assertEquals(VALUE2, contentValues.getAsString(DummyProvider.VALUE));
    }

    public void testSetKeepUpdated() throws InterruptedException {
        MockObserver observer = new MockObserver();

        // keepUpdated is false
        mContentQueryMap = new ContentQueryMap(mCursor, DummyProvider.NAME, false, null);
        mContentQueryMap.addObserver(observer);
        assertFalse(observer.hadUpdated(0));

        ContentValues contentValues = mContentQueryMap.getValues(NAME0);
        assertNotNull(contentValues);
        assertEquals(VALUE0, contentValues.getAsString(DummyProvider.VALUE));
        contentValues = mContentQueryMap.getValues(NAME2);
        assertNull(contentValues);

        // update NAME0 and VALUE0
        ContentValues values = new ContentValues();
        values.put(DummyProvider.NAME, NAME2);
        values.put(DummyProvider.VALUE, VALUE2);
        mResolver.update(DummyProvider.CONTENT_URI, values,
                DummyProvider.NAME + " = '" + NAME0 + "'", null);

        // values have not been updated
        assertFalse(observer.hadUpdated(0));
        contentValues = mContentQueryMap.getValues(NAME0);
        assertNotNull(contentValues);
        assertEquals(VALUE0, contentValues.getAsString(DummyProvider.VALUE));
        contentValues = mContentQueryMap.getValues(NAME2);
        assertNull(contentValues);

        // have to update manually
        mContentQueryMap.requery();
        assertTrue(observer.hadUpdated(0));
        assertSame(mContentQueryMap, observer.getObservable());

        contentValues = mContentQueryMap.getValues(NAME0);
        assertNull(contentValues);
        contentValues = mContentQueryMap.getValues(NAME2);
        assertNotNull(contentValues);
        assertEquals(VALUE2, contentValues.getAsString(DummyProvider.VALUE));

        observer.reset();
        contentValues = mContentQueryMap.getValues(NAME3);
        assertNull(contentValues);
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                mContentQueryMap.setKeepUpdated(true);
                synchronized (ContentQueryMapTest.this) {
                    //listener is ready, release the sender thread
                    ContentQueryMapTest.this.notify();
                }
                Looper.loop();
            }
        }).start();
        synchronized (this) {
            wait(TEST_TIME_OUT);
        }//wait the listener to be ready before launching onChange event

        // insert NAME3 and VALUE3
        values = new ContentValues();
        values.put(DummyProvider.NAME, NAME3);
        values.put(DummyProvider.VALUE, VALUE3);
        mResolver.insert(DummyProvider.CONTENT_URI, values);

        // should be updated automatically
        assertTrue(observer.hadUpdated(TEST_TIME_OUT));
        assertSame(mContentQueryMap, observer.getObservable());
        contentValues = mContentQueryMap.getValues(NAME3);
        assertNotNull(contentValues);
        assertEquals(VALUE3, contentValues.getAsString(DummyProvider.VALUE));

        observer.reset();
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                mContentQueryMap.setKeepUpdated(false);
                synchronized (ContentQueryMapTest.this) {
                    //listener is ready, release the sender thread
                    ContentQueryMapTest.this.notify();
                }
                Looper.loop();
            }
        }).start();
        synchronized (this) {
            wait(TEST_TIME_OUT);
        }//wait the listener to be ready before launching onChange event
        // update NAME3 and VALUE3
        values = new ContentValues();
        values.put(DummyProvider.NAME, NAME0);
        values.put(DummyProvider.VALUE, VALUE0);
        mResolver.update(DummyProvider.CONTENT_URI, values,
                DummyProvider.NAME + " = '" + NAME3 + "'", null);

        // values have not been updated
        assertFalse(observer.hadUpdated(TEST_TIME_OUT));
        contentValues = mContentQueryMap.getValues(NAME3);
        assertNotNull(contentValues);
        assertEquals(VALUE3, contentValues.getAsString(DummyProvider.VALUE));
    }

    public void testSetKeepUpdatedWithHandler() throws InterruptedException {
        MockObserver observer = new MockObserver();
        HandlerThread thread = new HandlerThread("testSetKeepUpdatedWithHandler");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        // keepUpdated is false
        mContentQueryMap = new ContentQueryMap(mCursor, DummyProvider.NAME, false, handler);
        mContentQueryMap.addObserver(observer);
        assertFalse(observer.hadUpdated(0));

        ContentValues contentValues = mContentQueryMap.getValues(NAME0);
        assertNotNull(contentValues);
        assertEquals(VALUE0, contentValues.getAsString(DummyProvider.VALUE));
        contentValues = mContentQueryMap.getValues(NAME2);
        assertNull(contentValues);

        // update NAME0 and VALUE0
        ContentValues values = new ContentValues();
        values.put(DummyProvider.NAME, NAME2);
        values.put(DummyProvider.VALUE, VALUE2);
        mResolver.update(DummyProvider.CONTENT_URI, values,
                DummyProvider.NAME + " = '" + NAME0 + "'", null);

        // values have not been updated
        assertFalse(observer.hadUpdated(TEST_TIME_OUT));
        contentValues = mContentQueryMap.getValues(NAME0);
        assertNotNull(contentValues);
        assertEquals(VALUE0, contentValues.getAsString(DummyProvider.VALUE));
        contentValues = mContentQueryMap.getValues(NAME2);
        assertNull(contentValues);

        // have to update manually
        mContentQueryMap.requery();
        assertTrue(observer.hadUpdated(0));
        assertSame(mContentQueryMap, observer.getObservable());

        contentValues = mContentQueryMap.getValues(NAME0);
        assertNull(contentValues);
        contentValues = mContentQueryMap.getValues(NAME2);
        assertNotNull(contentValues);
        assertEquals(VALUE2, contentValues.getAsString(DummyProvider.VALUE));

        observer.reset();
        contentValues = mContentQueryMap.getValues(NAME3);
        assertNull(contentValues);
        mContentQueryMap.setKeepUpdated(true);

        // insert NAME3 and VALUE3
        values = new ContentValues();
        values.put(DummyProvider.NAME, NAME3);
        values.put(DummyProvider.VALUE, VALUE3);
        mResolver.insert(DummyProvider.CONTENT_URI, values);

        // should be updated automatically
        assertTrue(observer.hadUpdated(TEST_TIME_OUT));
        assertSame(mContentQueryMap, observer.getObservable());
        contentValues = mContentQueryMap.getValues(NAME3);
        assertNotNull(contentValues);
        assertEquals(VALUE3, contentValues.getAsString(DummyProvider.VALUE));

        observer.reset();
        mContentQueryMap.setKeepUpdated(false);
        // update NAME3 and VALUE3
        values = new ContentValues();
        values.put(DummyProvider.NAME, NAME0);
        values.put(DummyProvider.VALUE, VALUE0);
        mResolver.update(DummyProvider.CONTENT_URI, values,
                DummyProvider.NAME + " = '" + NAME3 + "'", null);

        // values have not been updated
        assertFalse(observer.hadUpdated(TEST_TIME_OUT));
        contentValues = mContentQueryMap.getValues(NAME3);
        assertNotNull(contentValues);
        assertEquals(VALUE3, contentValues.getAsString(DummyProvider.VALUE));
    }

    public void testGetValuesBoundary() {
        mContentQueryMap = new ContentQueryMap(mCursor, DummyProvider.NAME, false, null);
        assertNull(mContentQueryMap.getValues(null));
        assertNull(mContentQueryMap.getValues(""));
    }

    private static final class MockObserver implements Observer {
        private boolean mHadUpdated = false;
        private Observable mObservable;

        public void reset() {
            mHadUpdated = false;
            mObservable = null;
        }

        public synchronized void update(Observable observable, Object data) {
            mObservable = observable;
            mHadUpdated = true;
            notify();
        }

        public Observable getObservable() {
            return mObservable;
        }

        public synchronized boolean hadUpdated(long timeout) throws InterruptedException {
            // do not wait if timeout is 0
            if (timeout > 0 && !mHadUpdated) {
                wait(timeout);
            }
            return mHadUpdated;
        }
    }
}
