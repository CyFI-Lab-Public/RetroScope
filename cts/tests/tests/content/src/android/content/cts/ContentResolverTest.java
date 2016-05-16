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

package android.content.cts;

import com.android.cts.stub.R;


import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.cts.util.PollingCheck;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ContentResolverTest extends AndroidTestCase {
    private final static String COLUMN_ID_NAME = "_id";
    private final static String COLUMN_KEY_NAME = "key";
    private final static String COLUMN_VALUE_NAME = "value";

    private static final String AUTHORITY = "ctstest";
    private static final Uri TABLE1_URI = Uri.parse("content://" + AUTHORITY + "/testtable1/");
    private static final Uri TABLE1_CROSS_URI =
            Uri.parse("content://" + AUTHORITY + "/testtable1/cross");
    private static final Uri TABLE2_URI = Uri.parse("content://" + AUTHORITY + "/testtable2/");
    private static final Uri SELF_URI = Uri.parse("content://" + AUTHORITY + "/self/");
    private static final Uri CRASH_URI = Uri.parse("content://" + AUTHORITY + "/crash/");

    private static final String REMOTE_AUTHORITY = "remotectstest";
    private static final Uri REMOTE_TABLE1_URI = Uri.parse("content://"
                + REMOTE_AUTHORITY + "/testtable1/");
    private static final Uri REMOTE_SELF_URI = Uri.parse("content://"
                + REMOTE_AUTHORITY + "/self/");
    private static final Uri REMOTE_CRASH_URI = Uri.parse("content://"
            + REMOTE_AUTHORITY + "/crash/");

    private static final Account ACCOUNT = new Account("cts", "cts");

    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";
    private static final String KEY3 = "key3";
    private static final int VALUE1 = 1;
    private static final int VALUE2 = 2;
    private static final int VALUE3 = 3;

    private static final String TEST_PACKAGE_NAME = "com.android.cts.stub";

    private Context mContext;
    private ContentResolver mContentResolver;
    private Cursor mCursor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = getContext();
        mContentResolver = mContext.getContentResolver();

        android.provider.Settings.System.putInt(mContentResolver, "__cts_crash_on_launch", 0);

        // add three rows to database when every test case start.
        ContentValues values = new ContentValues();

        values.put(COLUMN_KEY_NAME, KEY1);
        values.put(COLUMN_VALUE_NAME, VALUE1);
        mContentResolver.insert(TABLE1_URI, values);
        mContentResolver.insert(REMOTE_TABLE1_URI, values);

        values.put(COLUMN_KEY_NAME, KEY2);
        values.put(COLUMN_VALUE_NAME, VALUE2);
        mContentResolver.insert(TABLE1_URI, values);
        mContentResolver.insert(REMOTE_TABLE1_URI, values);

        values.put(COLUMN_KEY_NAME, KEY3);
        values.put(COLUMN_VALUE_NAME, VALUE3);
        mContentResolver.insert(TABLE1_URI, values);
        mContentResolver.insert(REMOTE_TABLE1_URI, values);
    }

    @Override
    protected void tearDown() throws Exception {
        mContentResolver.delete(TABLE1_URI, null, null);
        if ( null != mCursor && !mCursor.isClosed() ) {
            mCursor.close();
        }
        mContentResolver.delete(REMOTE_TABLE1_URI, null, null);
        if ( null != mCursor && !mCursor.isClosed() ) {
            mCursor.close();
        }
        super.tearDown();
    }

    public void testConstructor() {
        assertNotNull(mContentResolver);
    }

    public void testCrashOnLaunch() {
        // This test is going to make sure that the platform deals correctly
        // with a content provider process going away while a client is waiting
        // for it to come up.
        // First, we need to make sure our provider process is gone.  Goodbye!
        ContentProviderClient client = mContentResolver.acquireContentProviderClient(
                REMOTE_AUTHORITY);
        // We are going to do something wrong here...  release the client first,
        // so the act of killing it doesn't kill our own process.
        client.release();
        try {
            client.delete(REMOTE_SELF_URI, null, null);
        } catch (RemoteException e) {
        }
        // Now make sure the thing is actually gone.
        boolean gone = true;
        try {
            client.getType(REMOTE_TABLE1_URI);
            gone = false;
        } catch (RemoteException e) {
        }
        if (!gone) {
            fail("Content provider process is not gone!");
        }
        try {
            android.provider.Settings.System.putInt(mContentResolver, "__cts_crash_on_launch", 1);
            String type1 = mContentResolver.getType(REMOTE_TABLE1_URI);
            assertEquals(android.provider.Settings.System.getInt(mContentResolver,
                "__cts_crash_on_launch", 0), 0);
            assertTrue(type1.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));
        } finally {
            android.provider.Settings.System.putInt(mContentResolver, "__cts_crash_on_launch", 0);
        }
    }

    public void testUnstableToStableRefs() {
        // Get an unstable refrence on the remote content provider.
        ContentProviderClient uClient = mContentResolver.acquireUnstableContentProviderClient(
                REMOTE_AUTHORITY);
        // Verify we can access it.
        String type1 = mContentResolver.getType(REMOTE_TABLE1_URI);
        assertTrue(type1.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));

        // Get a stable reference on the remote content provider.
        ContentProviderClient sClient = mContentResolver.acquireContentProviderClient(
                REMOTE_AUTHORITY);
        // Verify we can still access it.
        type1 = mContentResolver.getType(REMOTE_TABLE1_URI);
        assertTrue(type1.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));

        // Release unstable reference.
        uClient.release();
        // Verify we can still access it.
        type1 = mContentResolver.getType(REMOTE_TABLE1_URI);
        assertTrue(type1.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));

        // Release stable reference, removing last ref.
        sClient.release();
        // Kill it.  Note that a bug at this point where it causes our own
        // process to be killed will result in the entire test failing.
        try {
            Log.i("ContentResolverTest",
                    "Killing remote client -- if test process goes away, that is why!");
            uClient.delete(REMOTE_SELF_URI, null, null);
        } catch (RemoteException e) {
        }
        // Make sure the remote client is actually gone.
        boolean gone = true;
        try {
            sClient.getType(REMOTE_TABLE1_URI);
            gone = false;
        } catch (RemoteException e) {
        }
        if (!gone) {
            fail("Content provider process is not gone!");
        }
    }

    public void testStableToUnstableRefs() {
        // Get a stable reference on the remote content provider.
        ContentProviderClient sClient = mContentResolver.acquireContentProviderClient(
                REMOTE_AUTHORITY);
        // Verify we can still access it.
        String type1 = mContentResolver.getType(REMOTE_TABLE1_URI);
        assertTrue(type1.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));
        
        // Get an unstable refrence on the remote content provider.
        ContentProviderClient uClient = mContentResolver.acquireUnstableContentProviderClient(
                REMOTE_AUTHORITY);
        // Verify we can access it.
        type1 = mContentResolver.getType(REMOTE_TABLE1_URI);
        assertTrue(type1.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));

        // Release stable reference, leaving only an unstable ref.
        sClient.release();

        // Kill it.  Note that a bug at this point where it causes our own
        // process to be killed will result in the entire test failing.
        try {
            Log.i("ContentResolverTest",
                    "Killing remote client -- if test process goes away, that is why!");
            uClient.delete(REMOTE_SELF_URI, null, null);
        } catch (RemoteException e) {
        }
        // Make sure the remote client is actually gone.
        boolean gone = true;
        try {
            uClient.getType(REMOTE_TABLE1_URI);
            gone = false;
        } catch (RemoteException e) {
        }
        if (!gone) {
            fail("Content provider process is not gone!");
        }

        // Release unstable reference.
        uClient.release();
    }

    public void testGetType() {
        String type1 = mContentResolver.getType(TABLE1_URI);
        assertTrue(type1.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));

        String type2 = mContentResolver.getType(TABLE2_URI);
        assertTrue(type2.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));

        Uri invalidUri = Uri.parse("abc");
        assertNull(mContentResolver.getType(invalidUri));

        try {
            mContentResolver.getType(null);
            fail("did not throw NullPointerException when Uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }
    }

    public void testUnstableGetType() {
        // Get an unstable refrence on the remote content provider.
        ContentProviderClient client = mContentResolver.acquireUnstableContentProviderClient(
                REMOTE_AUTHORITY);
        // Verify we can access it.
        String type1 = mContentResolver.getType(REMOTE_TABLE1_URI);
        assertTrue(type1.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));

        // Kill it.  Note that a bug at this point where it causes our own
        // process to be killed will result in the entire test failing.
        try {
            Log.i("ContentResolverTest",
                    "Killing remote client -- if test process goes away, that is why!");
            client.delete(REMOTE_SELF_URI, null, null);
        } catch (RemoteException e) {
        }
        // Make sure the remote client is actually gone.
        boolean gone = true;
        try {
            client.getType(REMOTE_TABLE1_URI);
            gone = false;
        } catch (RemoteException e) {
        }
        if (!gone) {
            fail("Content provider process is not gone!");
        }

        // Now the remote client is gone, can we recover?
        // Release our old reference.
        client.release();
        // Get a new reference.
        client = mContentResolver.acquireUnstableContentProviderClient(REMOTE_AUTHORITY);
        // Verify we can access it.
        type1 = mContentResolver.getType(REMOTE_TABLE1_URI);
        assertTrue(type1.startsWith(ContentResolver.CURSOR_DIR_BASE_TYPE, 0));
    }

    public void testQuery() {
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);

        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());
        assertEquals(3, mCursor.getColumnCount());

        mCursor.moveToLast();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY3, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToPrevious();
        assertEquals(2, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY2, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE2, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        String selection = COLUMN_ID_NAME + "=1";
        mCursor = mContentResolver.query(TABLE1_URI, null, selection, null, null);
        assertNotNull(mCursor);
        assertEquals(1, mCursor.getCount());
        assertEquals(3, mCursor.getColumnCount());

        mCursor.moveToFirst();
        assertEquals(1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY1, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        selection = COLUMN_KEY_NAME + "=\"" + KEY3 + "\"";
        mCursor = mContentResolver.query(TABLE1_URI, null, selection, null, null);
        assertNotNull(mCursor);
        assertEquals(1, mCursor.getCount());
        assertEquals(3, mCursor.getColumnCount());

        mCursor.moveToFirst();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY3, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        try {
            mContentResolver.query(null, null, null, null, null);
            fail("did not throw NullPointerException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }
    }

    public void testCrashingQuery() {
        try {
            android.provider.Settings.System.putInt(mContentResolver, "__cts_crash_on_launch", 1);
            mCursor = mContentResolver.query(REMOTE_CRASH_URI, null, null, null, null);
            assertEquals(android.provider.Settings.System.getInt(mContentResolver,
                "__cts_crash_on_launch", 0), 0);
        } finally {
            android.provider.Settings.System.putInt(mContentResolver, "__cts_crash_on_launch", 0);
        }

        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());
        assertEquals(3, mCursor.getColumnCount());

        mCursor.moveToLast();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY3, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToPrevious();
        assertEquals(2, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY2, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE2, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();
    }

    public void testCancelableQuery_WhenNotCanceled_ReturnsResultSet() {
        CancellationSignal cancellationSignal = new CancellationSignal();

        Cursor cursor = mContentResolver.query(TABLE1_URI, null, null, null, null,
                cancellationSignal);
        assertEquals(3, cursor.getCount());
        cursor.close();
    }

    public void testCancelableQuery_WhenCanceledBeforeQuery_ThrowsImmediately() {
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.cancel();

        try {
            mContentResolver.query(TABLE1_URI, null, null, null, null, cancellationSignal);
            fail("Expected OperationCanceledException");
        } catch (OperationCanceledException ex) {
            // expected
        }
    }

    public void testCancelableQuery_WhenCanceledDuringLongRunningQuery_CancelsQueryAndThrows() {
        // Populate a table with a bunch of integers.
        mContentResolver.delete(TABLE1_URI, null, null);
        ContentValues values = new ContentValues();
        for (int i = 0; i < 100; i++) {
            values.put(COLUMN_KEY_NAME, i);
            values.put(COLUMN_VALUE_NAME, i);
            mContentResolver.insert(TABLE1_URI, values);
        }

        for (int i = 0; i < 5; i++) {
            final CancellationSignal cancellationSignal = new CancellationSignal();
            Thread cancellationThread = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ex) {
                    }
                    cancellationSignal.cancel();
                }
            };
            try {
                // Build an unsatisfiable 5-way cross-product query over 100 values but
                // produces no output.  This should force SQLite to loop for a long time
                // as it tests 10^10 combinations.
                cancellationThread.start();

                final long startTime = System.nanoTime();
                try {
                    mContentResolver.query(TABLE1_CROSS_URI, null,
                            "a.value + b.value + c.value + d.value + e.value > 1000000",
                            null, null, cancellationSignal);
                    fail("Expected OperationCanceledException");
                } catch (OperationCanceledException ex) {
                    // expected
                }

                // We want to confirm that the query really was running and then got
                // canceled midway.
                final long waitTime = System.nanoTime() - startTime;
                if (waitTime > 150 * 1000000L && waitTime < 600 * 1000000L) {
                    return; // success!
                }
            } finally {
                try {
                    cancellationThread.join();
                } catch (InterruptedException e) {
                }
            }
        }

        // Occasionally we might miss the timing deadline due to factors in the
        // environment, but if after several trials we still couldn't demonstrate
        // that the query was canceled, then the test must be broken.
        fail("Could not prove that the query actually canceled midway during execution.");
    }

    public void testOpenInputStream() throws IOException {
        final Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + TEST_PACKAGE_NAME + "/" + R.drawable.pass);

        InputStream is = mContentResolver.openInputStream(uri);
        assertNotNull(is);
        is.close();

        final Uri invalidUri = Uri.parse("abc");
        try {
            mContentResolver.openInputStream(invalidUri);
            fail("did not throw FileNotFoundException when uri is invalid.");
        } catch (FileNotFoundException e) {
            //expected.
        }
    }

    public void testOpenOutputStream() throws IOException {
        Uri uri = Uri.parse(ContentResolver.SCHEME_FILE + "://" +
                getContext().getCacheDir().getAbsolutePath() +
                "/temp.jpg");
        OutputStream os = mContentResolver.openOutputStream(uri);
        assertNotNull(os);
        os.close();

        os = mContentResolver.openOutputStream(uri, "wa");
        assertNotNull(os);
        os.close();

        uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + TEST_PACKAGE_NAME + "/" + R.raw.testimage);
        try {
            mContentResolver.openOutputStream(uri);
            fail("did not throw FileNotFoundException when scheme is not accepted.");
        } catch (FileNotFoundException e) {
            //expected.
        }

        try {
            mContentResolver.openOutputStream(uri, "w");
            fail("did not throw FileNotFoundException when scheme is not accepted.");
        } catch (FileNotFoundException e) {
            //expected.
        }

        Uri invalidUri = Uri.parse("abc");
        try {
            mContentResolver.openOutputStream(invalidUri);
            fail("did not throw FileNotFoundException when uri is invalid.");
        } catch (FileNotFoundException e) {
            //expected.
        }

        try {
            mContentResolver.openOutputStream(invalidUri, "w");
            fail("did not throw FileNotFoundException when uri is invalid.");
        } catch (FileNotFoundException e) {
            //expected.
        }
    }

    public void testOpenAssetFileDescriptor() throws IOException {
        Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + TEST_PACKAGE_NAME + "/" + R.raw.testimage);

        AssetFileDescriptor afd = mContentResolver.openAssetFileDescriptor(uri, "r");
        assertNotNull(afd);
        afd.close();

        try {
            mContentResolver.openAssetFileDescriptor(uri, "d");
            fail("did not throw FileNotFoundException when mode is unknown.");
        } catch (FileNotFoundException e) {
            //expected.
        }

        Uri invalidUri = Uri.parse("abc");
        try {
            mContentResolver.openAssetFileDescriptor(invalidUri, "r");
            fail("did not throw FileNotFoundException when uri is invalid.");
        } catch (FileNotFoundException e) {
            //expected.
        }
    }

    private String consumeAssetFileDescriptor(AssetFileDescriptor afd)
            throws IOException {
        FileInputStream stream = null;
        try {
            stream = afd.createInputStream();
            InputStreamReader reader = new InputStreamReader(stream, "UTF-8");

            // Got it...  copy the stream into a local string and return it.
            StringBuilder builder = new StringBuilder(128);
            char[] buffer = new char[8192];
            int len;
            while ((len=reader.read(buffer)) > 0) {
                builder.append(buffer, 0, len);
            }
            return builder.toString();

        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
        
    }

    public void testCrashingOpenAssetFileDescriptor() throws IOException {
        AssetFileDescriptor afd = null;
        try {
            android.provider.Settings.System.putInt(mContentResolver, "__cts_crash_on_launch", 1);
            afd = mContentResolver.openAssetFileDescriptor(REMOTE_CRASH_URI, "rw");
            assertEquals(android.provider.Settings.System.getInt(mContentResolver,
                    "__cts_crash_on_launch", 0), 0);
            assertNotNull(afd);
            String str = consumeAssetFileDescriptor(afd);
            afd = null;
            assertEquals(str, "This is the openAssetFile test data!");
        } finally {
            android.provider.Settings.System.putInt(mContentResolver, "__cts_crash_on_launch", 0);
            if (afd != null) {
                afd.close();
            }
        }

        // Make sure a content provider crash at this point won't hurt us.
        ContentProviderClient uClient = mContentResolver.acquireUnstableContentProviderClient(
                REMOTE_AUTHORITY);
        // Kill it.  Note that a bug at this point where it causes our own
        // process to be killed will result in the entire test failing.
        try {
            Log.i("ContentResolverTest",
                    "Killing remote client -- if test process goes away, that is why!");
            uClient.delete(REMOTE_SELF_URI, null, null);
        } catch (RemoteException e) {
        }
        uClient.release();
    }

    public void testCrashingOpenTypedAssetFileDescriptor() throws IOException {
        AssetFileDescriptor afd = null;
        try {
            android.provider.Settings.System.putInt(mContentResolver, "__cts_crash_on_launch", 1);
            afd = mContentResolver.openTypedAssetFileDescriptor(
                    REMOTE_CRASH_URI, "text/plain", null);
            assertEquals(android.provider.Settings.System.getInt(mContentResolver,
                    "__cts_crash_on_launch", 0), 0);
            assertNotNull(afd);
            String str = consumeAssetFileDescriptor(afd);
            afd = null;
            assertEquals(str, "This is the openTypedAssetFile test data!");
        } finally {
            android.provider.Settings.System.putInt(mContentResolver, "__cts_crash_on_launch", 0);
            if (afd != null) {
                afd.close();
            }
        }

        // Make sure a content provider crash at this point won't hurt us.
        ContentProviderClient uClient = mContentResolver.acquireUnstableContentProviderClient(
                REMOTE_AUTHORITY);
        // Kill it.  Note that a bug at this point where it causes our own
        // process to be killed will result in the entire test failing.
        try {
            Log.i("ContentResolverTest",
                    "Killing remote client -- if test process goes away, that is why!");
            uClient.delete(REMOTE_SELF_URI, null, null);
        } catch (RemoteException e) {
        }
        uClient.release();
    }

    public void testOpenFileDescriptor() throws IOException {
        Uri uri = Uri.parse(ContentResolver.SCHEME_FILE + "://" +
                getContext().getCacheDir().getAbsolutePath() +
                "/temp.jpg");
        ParcelFileDescriptor pfd = mContentResolver.openFileDescriptor(uri, "w");
        assertNotNull(pfd);
        pfd.close();

        try {
            mContentResolver.openFileDescriptor(uri, "d");
            fail("did not throw IllegalArgumentException when mode is unknown.");
        } catch (IllegalArgumentException e) {
            //expected.
        }

        Uri invalidUri = Uri.parse("abc");
        try {
            mContentResolver.openFileDescriptor(invalidUri, "w");
            fail("did not throw FileNotFoundException when uri is invalid.");
        } catch (FileNotFoundException e) {
            //expected.
        }

        uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + TEST_PACKAGE_NAME + "/" + R.raw.testimage);
        try {
            mContentResolver.openFileDescriptor(uri, "w");
            fail("did not throw FileNotFoundException when scheme is not accepted.");
        } catch (FileNotFoundException e) {
            //expected.
        }
    }

    public void testInsert() {
        String key4 = "key4";
        String key5 = "key5";
        int value4 = 4;
        int value5 = 5;
        String key4Selection = COLUMN_KEY_NAME + "=\"" + key4 + "\"";

        mCursor = mContentResolver.query(TABLE1_URI, null, key4Selection, null, null);
        assertEquals(0, mCursor.getCount());
        mCursor.close();

        ContentValues values = new ContentValues();
        values.put(COLUMN_KEY_NAME, key4);
        values.put(COLUMN_VALUE_NAME, value4);
        Uri uri = mContentResolver.insert(TABLE1_URI, values);
        assertNotNull(uri);

        mCursor = mContentResolver.query(TABLE1_URI, null, key4Selection, null, null);
        assertNotNull(mCursor);
        assertEquals(1, mCursor.getCount());

        mCursor.moveToFirst();
        assertEquals(4, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key4, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value4, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        values.put(COLUMN_KEY_NAME, key5);
        values.put(COLUMN_VALUE_NAME, value5);
        uri = mContentResolver.insert(TABLE1_URI, values);
        assertNotNull(uri);

        // check returned uri
        mCursor = mContentResolver.query(uri, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(1, mCursor.getCount());

        mCursor.moveToLast();
        assertEquals(5, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key5, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value5, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        try {
            mContentResolver.insert(null, values);
            fail("did not throw NullPointerException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }
    }

    public void testBulkInsert() {
        String key4 = "key4";
        String key5 = "key5";
        int value4 = 4;
        int value5 = 5;

        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());
        mCursor.close();

        ContentValues[] cvs = new ContentValues[2];
        cvs[0] = new ContentValues();
        cvs[0].put(COLUMN_KEY_NAME, key4);
        cvs[0].put(COLUMN_VALUE_NAME, value4);

        cvs[1] = new ContentValues();
        cvs[1].put(COLUMN_KEY_NAME, key5);
        cvs[1].put(COLUMN_VALUE_NAME, value5);

        assertEquals(2, mContentResolver.bulkInsert(TABLE1_URI, cvs));
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(5, mCursor.getCount());

        mCursor.moveToLast();
        assertEquals(5, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key5, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value5, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToPrevious();
        assertEquals(4, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key4, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value4, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        try {
            mContentResolver.bulkInsert(null, cvs);
            fail("did not throw NullPointerException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }
    }

    public void testDelete() {
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());
        mCursor.close();

        assertEquals(3, mContentResolver.delete(TABLE1_URI, null, null));
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(0, mCursor.getCount());
        mCursor.close();

        // add three rows to database.
        ContentValues values = new ContentValues();
        values.put(COLUMN_KEY_NAME, KEY1);
        values.put(COLUMN_VALUE_NAME, VALUE1);
        mContentResolver.insert(TABLE1_URI, values);

        values.put(COLUMN_KEY_NAME, KEY2);
        values.put(COLUMN_VALUE_NAME, VALUE2);
        mContentResolver.insert(TABLE1_URI, values);

        values.put(COLUMN_KEY_NAME, KEY3);
        values.put(COLUMN_VALUE_NAME, VALUE3);
        mContentResolver.insert(TABLE1_URI, values);

        // test delete row using selection
        String selection = COLUMN_ID_NAME + "=2";
        assertEquals(1, mContentResolver.delete(TABLE1_URI, selection, null));

        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(2, mCursor.getCount());

        mCursor.moveToFirst();
        assertEquals(1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY1, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToNext();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY3, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        selection = COLUMN_VALUE_NAME + "=3";
        assertEquals(1, mContentResolver.delete(TABLE1_URI, selection, null));

        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(1, mCursor.getCount());

        mCursor.moveToFirst();
        assertEquals(1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(KEY1, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(VALUE1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        selection = COLUMN_KEY_NAME + "=\"" + KEY1 + "\"";
        assertEquals(1, mContentResolver.delete(TABLE1_URI, selection, null));

        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(0, mCursor.getCount());
        mCursor.close();

        try {
            mContentResolver.delete(null, null, null);
            fail("did not throw NullPointerException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }
    }

    public void testUpdate() {
        ContentValues values = new ContentValues();
        String key10 = "key10";
        String key20 = "key20";
        int value10 = 10;
        int value20 = 20;

        values.put(COLUMN_KEY_NAME, key10);
        values.put(COLUMN_VALUE_NAME, value10);

        // test update all the rows.
        assertEquals(3, mContentResolver.update(TABLE1_URI, values, null, null));
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());

        mCursor.moveToFirst();
        assertEquals(1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key10, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value10, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToNext();
        assertEquals(2, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key10, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value10, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToLast();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key10, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value10, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        // test update one row using selection.
        String selection = COLUMN_ID_NAME + "=1";
        values.put(COLUMN_KEY_NAME, key20);
        values.put(COLUMN_VALUE_NAME, value20);

        assertEquals(1, mContentResolver.update(TABLE1_URI, values, selection, null));
        mCursor = mContentResolver.query(TABLE1_URI, null, null, null, null);
        assertNotNull(mCursor);
        assertEquals(3, mCursor.getCount());

        mCursor.moveToFirst();
        assertEquals(1, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key20, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value20, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToNext();
        assertEquals(2, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key10, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value10, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));

        mCursor.moveToLast();
        assertEquals(3, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_ID_NAME)));
        assertEquals(key10, mCursor.getString(mCursor.getColumnIndexOrThrow(COLUMN_KEY_NAME)));
        assertEquals(value10, mCursor.getInt(mCursor.getColumnIndexOrThrow(COLUMN_VALUE_NAME)));
        mCursor.close();

        try {
            mContentResolver.update(null, values, null, null);
            fail("did not throw NullPointerException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        }

        // javadoc says it will throw NullPointerException when values are null,
        // but actually, it throws IllegalArgumentException here.
        try {
            mContentResolver.update(TABLE1_URI, null, null, null);
            fail("did not throw IllegalArgumentException when values are null.");
        } catch (IllegalArgumentException e) {
            //expected.
        }
    }

    public void testRegisterContentObserver() {
        final MockContentObserver mco = new MockContentObserver();

        mContentResolver.registerContentObserver(TABLE1_URI, true, mco);
        assertFalse(mco.hadOnChanged());

        ContentValues values = new ContentValues();
        values.put(COLUMN_KEY_NAME, "key10");
        values.put(COLUMN_VALUE_NAME, 10);
        mContentResolver.update(TABLE1_URI, values, null, null);
        new PollingCheck() {
            @Override
            protected boolean check() {
                return mco.hadOnChanged();
            }
        }.run();

        mco.reset();
        mContentResolver.unregisterContentObserver(mco);
        assertFalse(mco.hadOnChanged());
        mContentResolver.update(TABLE1_URI, values, null, null);

        assertFalse(mco.hadOnChanged());

        try {
            mContentResolver.registerContentObserver(null, false, mco);
            fail("did not throw NullPointerException or IllegalArgumentException when uri is null.");
        } catch (NullPointerException e) {
            //expected.
        } catch (IllegalArgumentException e) {
            // also expected
        }

        try {
            mContentResolver.registerContentObserver(TABLE1_URI, false, null);
            fail("did not throw NullPointerException when register null content observer.");
        } catch (NullPointerException e) {
            //expected.
        }

        try {
            mContentResolver.unregisterContentObserver(null);
            fail("did not throw NullPointerException when unregister null content observer.");
        } catch (NullPointerException e) {
            //expected.
        }
    }

    public void testNotifyChange1() {
        final MockContentObserver mco = new MockContentObserver();

        mContentResolver.registerContentObserver(TABLE1_URI, true, mco);
        assertFalse(mco.hadOnChanged());

        mContentResolver.notifyChange(TABLE1_URI, mco);
        new PollingCheck() {
            @Override
            protected boolean check() {
                return mco.hadOnChanged();
            }
        }.run();

        mContentResolver.unregisterContentObserver(mco);
    }

    public void testNotifyChange2() {
        final MockContentObserver mco = new MockContentObserver();

        mContentResolver.registerContentObserver(TABLE1_URI, true, mco);
        assertFalse(mco.hadOnChanged());

        mContentResolver.notifyChange(TABLE1_URI, mco, false);
        new PollingCheck() {
            @Override
            protected boolean check() {
                return mco.hadOnChanged();
            }
        }.run();

        mContentResolver.unregisterContentObserver(mco);
    }

    public void testStartCancelSync() {
        Bundle extras = new Bundle();

        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

        ContentResolver.requestSync(ACCOUNT, AUTHORITY, extras);
        //FIXME: how to get the result to assert.

        ContentResolver.cancelSync(ACCOUNT, AUTHORITY);
        //FIXME: how to assert.
    }

    public void testStartSyncFailure() {
        try {
            ContentResolver.requestSync(null, null, null);
            fail("did not throw IllegalArgumentException when extras is null.");
        } catch (IllegalArgumentException e) {
            //expected.
        }
    }

    public void testValidateSyncExtrasBundle() {
        Bundle extras = new Bundle();
        extras.putInt("Integer", 20);
        extras.putLong("Long", 10l);
        extras.putBoolean("Boolean", true);
        extras.putFloat("Float", 5.5f);
        extras.putDouble("Double", 2.5);
        extras.putString("String", "cts");
        extras.putCharSequence("CharSequence", null);

        ContentResolver.validateSyncExtrasBundle(extras);

        extras.putChar("Char", 'a'); // type Char is invalid
        try {
            ContentResolver.validateSyncExtrasBundle(extras);
            fail("did not throw IllegalArgumentException when extras is invalide.");
        } catch (IllegalArgumentException e) {
            //expected.
        }
    }

    private class MockContentObserver extends ContentObserver {
        private boolean mHadOnChanged = false;

        public MockContentObserver() {
            super(null);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public synchronized void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mHadOnChanged = true;
        }

        public synchronized boolean hadOnChanged() {
            return mHadOnChanged;
        }

        public synchronized void reset() {
            mHadOnChanged = false;
        }
    }
}
