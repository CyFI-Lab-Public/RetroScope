/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.providers.partnerbookmarks;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.UriMatcher;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Default partner bookmarks provider implementation of {@link PartnerBookmarksContract} API.
 * It reads the flat list of bookmarks and the name of the root partner
 * bookmarks folder using getResources() API.
 *
 * Sample resources structure:
 *     res/
 *         values/
 *             strings.xml
 *                  string name="bookmarks_folder_name"
 *                  string-array name="bookmarks"
 *                      item TITLE1
 *                      item URL1
 *                      item TITLE2
 *                      item URL2...
 *             bookmarks_icons.xml
 *                  array name="bookmark_preloads"
 *                      item @raw/favicon1
 *                      item @raw/touchicon1
 *                      item @raw/favicon2
 *                      item @raw/touchicon2
 *                      ...
 */
public class PartnerBookmarksProvider extends ContentProvider {
    private static final String TAG = "PartnerBookmarksProvider";

    // URI matcher
    private static final int URI_MATCH_BOOKMARKS = 1000;
    private static final int URI_MATCH_BOOKMARKS_ID = 1001;
    private static final int URI_MATCH_BOOKMARKS_FOLDER = 1002;
    private static final int URI_MATCH_BOOKMARKS_FOLDER_ID = 1003;
    private static final int URI_MATCH_BOOKMARKS_PARTNER_BOOKMARKS_FOLDER_ID = 1004;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final Map<String, String> BOOKMARKS_PROJECTION_MAP
            = new HashMap<String, String>();

    // Default sort order for unsync'd bookmarks
    private static final String DEFAULT_BOOKMARKS_SORT_ORDER =
            PartnerBookmarksContract.Bookmarks.ID + " DESC, "
                    + PartnerBookmarksContract.Bookmarks.ID + " ASC";

    // Initial bookmark id when for getResources() importing
    // Make sure to fix tests if you are changing this
    private static final long FIXED_ID_PARTNER_BOOKMARKS_ROOT =
            PartnerBookmarksContract.Bookmarks.BOOKMARK_PARENT_ROOT_ID + 1;

    // DB table name
    private static final String TABLE_BOOKMARKS = "bookmarks";

    static {
        final UriMatcher matcher = URI_MATCHER;
        final String authority = PartnerBookmarksContract.AUTHORITY;
        matcher.addURI(authority, "bookmarks", URI_MATCH_BOOKMARKS);
        matcher.addURI(authority, "bookmarks/#", URI_MATCH_BOOKMARKS_ID);
        matcher.addURI(authority, "bookmarks/folder", URI_MATCH_BOOKMARKS_FOLDER);
        matcher.addURI(authority, "bookmarks/folder/#", URI_MATCH_BOOKMARKS_FOLDER_ID);
        matcher.addURI(authority, "bookmarks/folder/id",
                URI_MATCH_BOOKMARKS_PARTNER_BOOKMARKS_FOLDER_ID);
        // Projection maps
        Map<String, String> map = BOOKMARKS_PROJECTION_MAP;
        map.put(PartnerBookmarksContract.Bookmarks.ID,
                PartnerBookmarksContract.Bookmarks.ID);
        map.put(PartnerBookmarksContract.Bookmarks.TITLE,
                PartnerBookmarksContract.Bookmarks.TITLE);
        map.put(PartnerBookmarksContract.Bookmarks.URL,
                PartnerBookmarksContract.Bookmarks.URL);
        map.put(PartnerBookmarksContract.Bookmarks.TYPE,
                PartnerBookmarksContract.Bookmarks.TYPE);
        map.put(PartnerBookmarksContract.Bookmarks.PARENT,
                PartnerBookmarksContract.Bookmarks.PARENT);
        map.put(PartnerBookmarksContract.Bookmarks.FAVICON,
                PartnerBookmarksContract.Bookmarks.FAVICON);
        map.put(PartnerBookmarksContract.Bookmarks.TOUCHICON,
                PartnerBookmarksContract.Bookmarks.TOUCHICON);
    }

    private final class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_FILENAME = "partnerBookmarks.db";
        private static final int DATABASE_VERSION = 1;
        private static final String PREFERENCES_FILENAME = "pbppref";
        private static final String ACTIVE_CONFIGURATION_PREFNAME = "config";
        private final SharedPreferences sharedPreferences;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_FILENAME, null, DATABASE_VERSION);
            sharedPreferences = context.getSharedPreferences(
                    PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        }

        private String getConfigSignature(Configuration config) {
            return "mmc=" + Integer.toString(config.mcc)
                    + "-mnc=" + Integer.toString(config.mnc)
                    + "-loc=" + config.locale.toString();
        }

        public synchronized void prepareForConfiguration(Configuration config) {
            final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            String newSignature = getConfigSignature(config);
            String activeSignature =
                    sharedPreferences.getString(ACTIVE_CONFIGURATION_PREFNAME, null);
            if (activeSignature == null || !activeSignature.equals(newSignature)) {
                db.delete(TABLE_BOOKMARKS, null, null);
                if (!createDefaultBookmarks(db)) {
                    // Failure to read/insert bookmarks should be treated as "no bookmarks"
                    db.delete(TABLE_BOOKMARKS, null, null);
                }
            }
        }

        private void setActiveConfiguration(Configuration config) {
            Editor editor = sharedPreferences.edit();
            editor.putString(ACTIVE_CONFIGURATION_PREFNAME, getConfigSignature(config));
            editor.apply();
        }

        private void createTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_BOOKMARKS + "(" +
                    PartnerBookmarksContract.Bookmarks.ID +
                    " INTEGER NOT NULL DEFAULT 0," +
                    PartnerBookmarksContract.Bookmarks.TITLE +
                    " TEXT," +
                    PartnerBookmarksContract.Bookmarks.URL +
                    " TEXT," +
                    PartnerBookmarksContract.Bookmarks.TYPE +
                    " INTEGER NOT NULL DEFAULT 0," +
                    PartnerBookmarksContract.Bookmarks.PARENT +
                    " INTEGER," +
                    PartnerBookmarksContract.Bookmarks.FAVICON +
                    " BLOB," +
                    PartnerBookmarksContract.Bookmarks.TOUCHICON +
                    " BLOB" + ");");
        }

        private void dropTable(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            synchronized (this) {
                createTable(db);
                if (!createDefaultBookmarks(db)) {
                    // Failure to read/insert bookmarks should be treated as "no bookmarks"
                    dropTable(db);
                    createTable(db);
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            dropTable(db);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            dropTable(db);
            onCreate(db);
        }

        private boolean createDefaultBookmarks(SQLiteDatabase db) {
            Resources res = getContext().getResources();
            try {
                CharSequence bookmarksFolderName = res.getText(R.string.bookmarks_folder_name);
                final CharSequence[] bookmarks = res.getTextArray(R.array.bookmarks);
                if (bookmarks.length >= 1) {
                    if (bookmarksFolderName.length() < 1) {
                        Log.i(TAG, "bookmarks_folder_name was not specified; bailing out");
                        return false;
                    }
                    if (!addRootFolder(db,
                            FIXED_ID_PARTNER_BOOKMARKS_ROOT, bookmarksFolderName.toString())) {
                        Log.i(TAG, "failed to insert root folder; bailing out");
                        return false;
                    }
                    if (!addDefaultBookmarks(db,
                            FIXED_ID_PARTNER_BOOKMARKS_ROOT, FIXED_ID_PARTNER_BOOKMARKS_ROOT + 1)) {
                        Log.i(TAG, "failed to insert bookmarks; bailing out");
                        return false;
                    }
                }
                setActiveConfiguration(res.getConfiguration());
            } catch (android.content.res.Resources.NotFoundException e) {
                Log.i(TAG, "failed to fetch resources; bailing out");
                return false;
            }
            return true;
        }

        private boolean addRootFolder(SQLiteDatabase db, long id, String bookmarksFolderName) {
            ContentValues values = new ContentValues();
            values.put(PartnerBookmarksContract.Bookmarks.ID, id);
            values.put(PartnerBookmarksContract.Bookmarks.TITLE,
                    bookmarksFolderName);
            values.put(PartnerBookmarksContract.Bookmarks.PARENT,
                    PartnerBookmarksContract.Bookmarks.BOOKMARK_PARENT_ROOT_ID);
            values.put(PartnerBookmarksContract.Bookmarks.TYPE,
                    PartnerBookmarksContract.Bookmarks.BOOKMARK_TYPE_FOLDER);
            return db.insertOrThrow(TABLE_BOOKMARKS, null, values) != -1;
        }

        private boolean addDefaultBookmarks(SQLiteDatabase db, long parentId, long firstBookmarkId) {
            long bookmarkId = firstBookmarkId;
            Resources res = getContext().getResources();
            final CharSequence[] bookmarks = res.getTextArray(R.array.bookmarks);
            int size = bookmarks.length;
            TypedArray preloads = res.obtainTypedArray(R.array.bookmark_preloads);
            DatabaseUtils.InsertHelper insertHelper = null;
            try {
                insertHelper = new DatabaseUtils.InsertHelper(db, TABLE_BOOKMARKS);
                final int idColumn = insertHelper.getColumnIndex(
                        PartnerBookmarksContract.Bookmarks.ID);
                final int titleColumn = insertHelper.getColumnIndex(
                        PartnerBookmarksContract.Bookmarks.TITLE);
                final int urlColumn = insertHelper.getColumnIndex(
                        PartnerBookmarksContract.Bookmarks.URL);
                final int typeColumn = insertHelper.getColumnIndex(
                        PartnerBookmarksContract.Bookmarks.TYPE);
                final int parentColumn = insertHelper.getColumnIndex(
                        PartnerBookmarksContract.Bookmarks.PARENT);
                final int faviconColumn = insertHelper.getColumnIndex(
                        PartnerBookmarksContract.Bookmarks.FAVICON);
                final int touchiconColumn = insertHelper.getColumnIndex(
                        PartnerBookmarksContract.Bookmarks.TOUCHICON);

                for (int i = 0; i + 1 < size; i = i + 2) {
                    CharSequence bookmarkDestination = bookmarks[i + 1];

                    String bookmarkTitle = bookmarks[i].toString();
                    String bookmarkUrl = bookmarkDestination.toString();
                    byte[] favicon = null;
                    if (i < preloads.length()) {
                        int faviconId = preloads.getResourceId(i, 0);
                        try {
                            favicon = readRaw(res, faviconId);
                        } catch (IOException e) {
                            Log.i(TAG, "Failed to read favicon for " + bookmarkTitle, e);
                        }
                    }
                    byte[] touchicon = null;
                    if (i + 1 < preloads.length()) {
                        int touchiconId = preloads.getResourceId(i + 1, 0);
                        try {
                            touchicon = readRaw(res, touchiconId);
                        } catch (IOException e) {
                            Log.i(TAG, "Failed to read touchicon for " + bookmarkTitle, e);
                        }
                    }
                    insertHelper.prepareForInsert();
                    insertHelper.bind(idColumn, bookmarkId);
                    insertHelper.bind(titleColumn, bookmarkTitle);
                    insertHelper.bind(urlColumn, bookmarkUrl);
                    insertHelper.bind(typeColumn,
                            PartnerBookmarksContract.Bookmarks.BOOKMARK_TYPE_BOOKMARK);
                    insertHelper.bind(parentColumn, parentId);
                    if (favicon != null) {
                        insertHelper.bind(faviconColumn, favicon);
                    }
                    if (touchicon != null) {
                        insertHelper.bind(touchiconColumn, touchicon);
                    }
                    bookmarkId++;
                    if (insertHelper.execute() == -1) {
                        Log.i(TAG, "Failed to insert bookmark " + bookmarkTitle);
                        return false;
                    }
                }
            } finally {
                preloads.recycle();
                insertHelper.close();
            }
            return true;
        }

        private byte[] readRaw(Resources res, int id) throws IOException {
            if (id == 0) return null;
            InputStream is = res.openRawResource(id);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                byte[] buf = new byte[4096];
                int read;
                while ((read = is.read(buf)) > 0) {
                    bos.write(buf, 0, read);
                }
                bos.flush();
                return bos.toByteArray();
            } finally {
                is.close();
                bos.close();
            }
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mOpenHelper.prepareForConfiguration(getContext().getResources().getConfiguration());
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        final int match = URI_MATCHER.match(uri);
        mOpenHelper.prepareForConfiguration(getContext().getResources().getConfiguration());
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String limit = uri.getQueryParameter(PartnerBookmarksContract.PARAM_LIMIT);
        String groupBy = uri.getQueryParameter(PartnerBookmarksContract.PARAM_GROUP_BY);
        switch (match) {
            case URI_MATCH_BOOKMARKS_FOLDER_ID:
            case URI_MATCH_BOOKMARKS_ID:
            case URI_MATCH_BOOKMARKS: {
                if (match == URI_MATCH_BOOKMARKS_ID) {
                    // Tack on the ID of the specific bookmark requested
                    selection = DatabaseUtils.concatenateWhere(selection,
                            TABLE_BOOKMARKS + "." +
                                    PartnerBookmarksContract.Bookmarks.ID + "=?");
                    selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs,
                            new String[] { Long.toString(ContentUris.parseId(uri)) });
                } else if (match == URI_MATCH_BOOKMARKS_FOLDER_ID) {
                    // Tack on the ID of the specific folder requested
                    selection = DatabaseUtils.concatenateWhere(selection,
                            TABLE_BOOKMARKS + "." +
                                    PartnerBookmarksContract.Bookmarks.PARENT + "=?");
                    selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs,
                            new String[] { Long.toString(ContentUris.parseId(uri)) });
                }
                // Set a default sort order if one isn't specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DEFAULT_BOOKMARKS_SORT_ORDER;
                }
                qb.setProjectionMap(BOOKMARKS_PROJECTION_MAP);
                qb.setTables(TABLE_BOOKMARKS);
                break;
            }

            case URI_MATCH_BOOKMARKS_FOLDER: {
                qb.setTables(TABLE_BOOKMARKS);
                String[] args;
                String query;
                // Set a default sort order if one isn't specified
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = DEFAULT_BOOKMARKS_SORT_ORDER;
                }
                qb.setProjectionMap(BOOKMARKS_PROJECTION_MAP);
                String where = PartnerBookmarksContract.Bookmarks.PARENT + "=?";
                where = DatabaseUtils.concatenateWhere(where, selection);
                args = new String[] { Long.toString(FIXED_ID_PARTNER_BOOKMARKS_ROOT) };
                if (selectionArgs != null) {
                    args = DatabaseUtils.appendSelectionArgs(args, selectionArgs);
                }
                query = qb.buildQuery(projection, where, null, null, sortOrder, null);
                Cursor cursor = db.rawQuery(query, args);
                return cursor;
            }

            case URI_MATCH_BOOKMARKS_PARTNER_BOOKMARKS_FOLDER_ID: {
                MatrixCursor c = new MatrixCursor(
                        new String[] {PartnerBookmarksContract.Bookmarks.ID});
                c.newRow().add(FIXED_ID_PARTNER_BOOKMARKS_ROOT);
                return c;
            }

            default: {
                throw new UnsupportedOperationException("Unknown URL " + uri.toString());
            }
        }

        return qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder, limit);
    }

    @Override
    public String getType(Uri uri) {
        final int match = URI_MATCHER.match(uri);
        if (match == UriMatcher.NO_MATCH) return null;
        return PartnerBookmarksContract.Bookmarks.CONTENT_ITEM_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
