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

package com.android.providers.userdictionary;


import java.util.HashMap;

import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.UserDictionary;
import android.provider.UserDictionary.Words;
import android.text.TextUtils;
import android.util.Log;

/**
 * Provides access to a database of user defined words. Each item has a word and a frequency.
 */
public class UserDictionaryProvider extends ContentProvider {

    /**
     * DB versions are as follow:
     *
     * Version 1:
     *   Up to IceCreamSandwich 4.0.3 - API version 15
     *   Contient ID (INTEGER PRIMARY KEY), WORD (TEXT), FREQUENCY (INTEGER),
     *   LOCALE (TEXT), APP_ID (INTEGER).
     *
     * Version 2:
     *   From IceCreamSandwich, 4.1 - API version 16
     *   Adds SHORTCUT (TEXT).
     */

    private static final String AUTHORITY = UserDictionary.AUTHORITY;

    private static final String TAG = "UserDictionaryProvider";

    private static final Uri CONTENT_URI = UserDictionary.CONTENT_URI;

    private static final String DATABASE_NAME = "user_dict.db";
    private static final int DATABASE_VERSION = 2;

    private static final String USERDICT_TABLE_NAME = "words";

    private static HashMap<String, String> sDictProjectionMap;
        
    private static final UriMatcher sUriMatcher;

    private static final int WORDS = 1;
    
    private static final int WORD_ID = 2;

    private BackupManager mBackupManager;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + USERDICT_TABLE_NAME + " ("
                    + Words._ID + " INTEGER PRIMARY KEY,"
                    + Words.WORD + " TEXT,"
                    + Words.FREQUENCY + " INTEGER,"
                    + Words.LOCALE + " TEXT,"
                    + Words.APP_ID + " INTEGER,"
                    + Words.SHORTCUT + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 1 && newVersion == 2) {
                Log.i(TAG, "Upgrading database from version " + oldVersion
                        + " to version 2: adding " + Words.SHORTCUT + " column");
                db.execSQL("ALTER TABLE " + USERDICT_TABLE_NAME
                        + " ADD " + Words.SHORTCUT + " TEXT;");
            } else {
                Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
                db.execSQL("DROP TABLE IF EXISTS " + USERDICT_TABLE_NAME);
                onCreate(db);
            }
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        mBackupManager = new BackupManager(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case WORDS:
            qb.setTables(USERDICT_TABLE_NAME);
            qb.setProjectionMap(sDictProjectionMap);
            break;

        case WORD_ID:
            qb.setTables(USERDICT_TABLE_NAME);
            qb.setProjectionMap(sDictProjectionMap);
            qb.appendWhere("_id" + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = Words.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case WORDS:
            return Words.CONTENT_TYPE;

        case WORD_ID:
            return Words.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != WORDS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        if (values.containsKey(Words.WORD) == false) {
            throw new SQLException("Word must be specified");
        }

        if (values.containsKey(Words.FREQUENCY) == false) {
            values.put(Words.FREQUENCY, "1");
        }

        if (values.containsKey(Words.LOCALE) == false) {
            values.put(Words.LOCALE, (String) null);
        }

        if (values.containsKey(Words.SHORTCUT) == false) {
            values.put(Words.SHORTCUT, (String) null);
        }

        values.put(Words.APP_ID, 0);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(USERDICT_TABLE_NAME, Words.WORD, values);
        if (rowId > 0) {
            Uri wordUri = ContentUris.withAppendedId(UserDictionary.Words.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(wordUri, null);
            mBackupManager.dataChanged();
            return wordUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case WORDS:
            count = db.delete(USERDICT_TABLE_NAME, where, whereArgs);
            break;

        case WORD_ID:
            String wordId = uri.getPathSegments().get(1);
            count = db.delete(USERDICT_TABLE_NAME, Words._ID + "=" + wordId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        mBackupManager.dataChanged();
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case WORDS:
            count = db.update(USERDICT_TABLE_NAME, values, where, whereArgs);
            break;

        case WORD_ID:
            String wordId = uri.getPathSegments().get(1);
            count = db.update(USERDICT_TABLE_NAME, values, Words._ID + "=" + wordId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        mBackupManager.dataChanged();
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "words", WORDS);
        sUriMatcher.addURI(AUTHORITY, "words/#", WORD_ID);

        sDictProjectionMap = new HashMap<String, String>();
        sDictProjectionMap.put(Words._ID, Words._ID);
        sDictProjectionMap.put(Words.WORD, Words.WORD);
        sDictProjectionMap.put(Words.FREQUENCY, Words.FREQUENCY);
        sDictProjectionMap.put(Words.LOCALE, Words.LOCALE);
        sDictProjectionMap.put(Words.APP_ID, Words.APP_ID);
        sDictProjectionMap.put(Words.SHORTCUT, Words.SHORTCUT);
    }
}
