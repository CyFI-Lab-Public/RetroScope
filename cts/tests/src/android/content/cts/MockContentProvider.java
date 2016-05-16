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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.ContentProvider.PipeDataWriter;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

public class MockContentProvider extends ContentProvider
        implements PipeDataWriter<String> {

    private SQLiteOpenHelper mOpenHelper;

    private static final String DEFAULT_AUTHORITY = "ctstest";
    private static final String DEFAULT_DBNAME = "ctstest.db";
    private static final int DBVERSION = 2;

    private static final int TESTTABLE1 = 1;
    private static final int TESTTABLE1_ID = 2;
    private static final int TESTTABLE1_CROSS = 3;
    private static final int TESTTABLE2 = 4;
    private static final int TESTTABLE2_ID = 5;
    private static final int SELF_ID = 6;
    private static final int CRASH_ID = 6;

    private final String mAuthority;
    private final String mDbName;
    private final UriMatcher URL_MATCHER;
    private HashMap<String, String> CTSDBTABLE1_LIST_PROJECTION_MAP;
    private HashMap<String, String> CTSDBTABLE2_LIST_PROJECTION_MAP;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context, String dbname) {
            super(context, dbname, null, DBVERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE TestTable1 ("
                    + "_id INTEGER PRIMARY KEY, " + "key TEXT, " + "value INTEGER"
                    + ");");

            db.execSQL("CREATE TABLE TestTable2 ("
                    + "_id INTEGER PRIMARY KEY, " + "key TEXT, " + "value INTEGER"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS TestTable1");
            db.execSQL("DROP TABLE IF EXISTS TestTable2");
            onCreate(db);
        }
    }

    public MockContentProvider() {
        this(DEFAULT_AUTHORITY, DEFAULT_DBNAME);
    }

    public MockContentProvider(String authority, String dbName) {
        mAuthority = authority;
        mDbName = dbName;

        URL_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URL_MATCHER.addURI(mAuthority, "testtable1", TESTTABLE1);
        URL_MATCHER.addURI(mAuthority, "testtable1/#", TESTTABLE1_ID);
        URL_MATCHER.addURI(mAuthority, "testtable1/cross", TESTTABLE1_CROSS);
        URL_MATCHER.addURI(mAuthority, "testtable2", TESTTABLE2);
        URL_MATCHER.addURI(mAuthority, "testtable2/#", TESTTABLE2_ID);
        URL_MATCHER.addURI(mAuthority, "self", SELF_ID);
        URL_MATCHER.addURI(mAuthority, "crash", CRASH_ID);

        CTSDBTABLE1_LIST_PROJECTION_MAP = new HashMap<String, String>();
        CTSDBTABLE1_LIST_PROJECTION_MAP.put("_id", "_id");
        CTSDBTABLE1_LIST_PROJECTION_MAP.put("key", "key");
        CTSDBTABLE1_LIST_PROJECTION_MAP.put("value", "value");

        CTSDBTABLE2_LIST_PROJECTION_MAP = new HashMap<String, String>();
        CTSDBTABLE2_LIST_PROJECTION_MAP.put("_id", "_id");
        CTSDBTABLE2_LIST_PROJECTION_MAP.put("key", "key");
        CTSDBTABLE2_LIST_PROJECTION_MAP.put("value", "value");
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext(), mDbName);
        if (android.provider.Settings.System.getInt(getContext().getContentResolver(),
                "__cts_crash_on_launch", 0) != 0) {
            // The test case wants us to crash our process on first launch.
            // Well, okay then!
            Log.i("MockContentProvider", "TEST IS CRASHING SELF, CROSS FINGERS!");
            android.provider.Settings.System.putInt(getContext().getContentResolver(),
                    "__cts_crash_on_launch", 0);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String segment;
        int count;

        switch (URL_MATCHER.match(uri)) {
        case TESTTABLE1:
            if (null == selection) {
                // get the count when remove all rows
                selection = "1";
            }
            count = db.delete("TestTable1", selection, selectionArgs);
            break;
        case TESTTABLE1_ID:
            segment = uri.getPathSegments().get(1);
            count = db.delete("TestTable1", "_id=" + segment +
                    (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                    selectionArgs);
            break;
        case TESTTABLE2:
            count = db.delete("TestTable2", selection, selectionArgs);
            break;
        case TESTTABLE2_ID:
            segment = uri.getPathSegments().get(1);
            count = db.delete("TestTable2", "_id=" + segment +
                    (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                    selectionArgs);
            break;
        case SELF_ID:
            // Wha...?  Delete ME?!?  O.K.!
            Log.i("MockContentProvider", "Delete self requested!");
            count = 1;
            android.os.Process.killProcess(android.os.Process.myPid());
            break;
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (URL_MATCHER.match(uri)) {
        case TESTTABLE1:
            return "vnd.android.cursor.dir/com.android.content.testtable1";
        case TESTTABLE1_ID:
            return "vnd.android.cursor.item/com.android.content.testtable1";
        case TESTTABLE1_CROSS:
            return "vnd.android.cursor.cross/com.android.content.testtable1";
        case TESTTABLE2:
            return "vnd.android.cursor.dir/com.android.content.testtable2";
        case TESTTABLE2_ID:
            return "vnd.android.cursor.item/com.android.content.testtable2";

        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        long rowID;
        ContentValues values;
        String table;
        Uri testUri;

        if (initialValues != null)
            values = new ContentValues(initialValues);
        else
            values = new ContentValues();

        if (values.containsKey("value") == false)
            values.put("value", -1);

        switch (URL_MATCHER.match(uri)) {
        case TESTTABLE1:
            table = "TestTable1";
            testUri = Uri.parse("content://" + mAuthority + "/testtable1");
            break;
        case TESTTABLE2:
            table = "TestTable2";
            testUri = Uri.parse("content://" + mAuthority + "/testtable2");
            break;
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        rowID = db.insert(table, "key", values);

        if (rowID > 0) {
            Uri url = ContentUris.withAppendedId(testUri, rowID);
            getContext().getContentResolver().notifyChange(url, null);
            return url;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return query(uri, projection, selection, selectionArgs, sortOrder, null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (URL_MATCHER.match(uri)) {
        case TESTTABLE1:
            qb.setTables("TestTable1");
            qb.setProjectionMap(CTSDBTABLE1_LIST_PROJECTION_MAP);
            break;

        case TESTTABLE1_ID:
            qb.setTables("TestTable1");
            qb.appendWhere("_id=" + uri.getPathSegments().get(1));
            break;

        case TESTTABLE1_CROSS:
            // Create a ridiculous cross-product of the test table.  This is done
            // to create an artificially long-running query to enable us to test
            // remote query cancellation in ContentResolverTest.
            qb.setTables("TestTable1 a, TestTable1 b, TestTable1 c, TestTable1 d, TestTable1 e");
            break;

        case TESTTABLE2:
            qb.setTables("TestTable2");
            qb.setProjectionMap(CTSDBTABLE2_LIST_PROJECTION_MAP);
            break;

        case TESTTABLE2_ID:
            qb.setTables("TestTable2");
            qb.appendWhere("_id=" + uri.getPathSegments().get(1));
            break;

        case CRASH_ID:
            if (android.provider.Settings.System.getInt(getContext().getContentResolver(),
                    "__cts_crash_on_launch", 0) != 0) {
                // The test case wants us to crash while querying.
                // Well, okay then!
                Log.i("MockContentProvider", "TEST IS CRASHING SELF, CROSS FINGERS!");
                android.provider.Settings.System.putInt(getContext().getContentResolver(),
                        "__cts_crash_on_launch", 0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
            qb.setTables("TestTable1");
            qb.setProjectionMap(CTSDBTABLE1_LIST_PROJECTION_MAP);
            break;

        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }

        /* If no sort order is specified use the default */
        String orderBy;
        if (TextUtils.isEmpty(sortOrder))
            orderBy = "_id";
        else
            orderBy = sortOrder;

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy,
                null, cancellationSignal);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String segment;
        int count;

        switch (URL_MATCHER.match(uri)) {
        case TESTTABLE1:
            count = db.update("TestTable1", values, selection, selectionArgs);
            break;

        case TESTTABLE1_ID:
            segment = uri.getPathSegments().get(1);
            count = db.update("TestTable1", values, "_id=" + segment +
                    (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                    selectionArgs);
            break;

        case TESTTABLE2:
            count = db.update("TestTable2", values, selection, selectionArgs);
            break;

        case TESTTABLE2_ID:
            segment = uri.getPathSegments().get(1);
            count = db.update("TestTable2", values, "_id=" + segment +
                    (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                    selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        switch (URL_MATCHER.match(uri)) {
            case CRASH_ID:
                if (android.provider.Settings.System.getInt(getContext().getContentResolver(),
                        "__cts_crash_on_launch", 0) != 0) {
                    // The test case wants us to crash while querying.
                    // Well, okay then!
                    Log.i("MockContentProvider", "TEST IS CRASHING SELF, CROSS FINGERS!");
                    android.provider.Settings.System.putInt(getContext().getContentResolver(),
                            "__cts_crash_on_launch", 0);
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
                return new AssetFileDescriptor(
                        openPipeHelper(uri, null, null,
                                "This is the openAssetFile test data!", this), 0,
                        AssetFileDescriptor.UNKNOWN_LENGTH);

            default:
                return super.openAssetFile(uri, mode);
        }
    }

    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {
        switch (URL_MATCHER.match(uri)) {
            case CRASH_ID:
                if (android.provider.Settings.System.getInt(getContext().getContentResolver(),
                        "__cts_crash_on_launch", 0) != 0) {
                    // The test case wants us to crash while querying.
                    // Well, okay then!
                    Log.i("MockContentProvider", "TEST IS CRASHING SELF, CROSS FINGERS!");
                    android.provider.Settings.System.putInt(getContext().getContentResolver(),
                            "__cts_crash_on_launch", 0);
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
                return new AssetFileDescriptor(
                        openPipeHelper(uri, null, null,
                                "This is the openTypedAssetFile test data!", this), 0,
                        AssetFileDescriptor.UNKNOWN_LENGTH);

            default:
                return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
        }
    }

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType, Bundle opts,
            String args) {
        FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
            pw.print(args);
        } catch (UnsupportedEncodingException e) {
            Log.w("MockContentProvider", "Ooops", e);
        } finally {
            if (pw != null) {
                pw.flush();
            }
            try {
                fout.close();
            } catch (IOException e) {
            }
        }
    }
}
