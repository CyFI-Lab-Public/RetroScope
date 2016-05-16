/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.providers.calendar;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.google.common.annotations.VisibleForTesting;

import java.util.TimeZone;

/**
 * Class for managing a persistent Cache of (key, value) pairs. The persistent storage used is
 * a SQLite database.
 */
public class CalendarCache {
    private static final String TAG = "CalendarCache";

    public static final String DATABASE_NAME = "CalendarCache";

    public static final String KEY_TIMEZONE_DATABASE_VERSION = "timezoneDatabaseVersion";
    public static final String DEFAULT_TIMEZONE_DATABASE_VERSION = "2009s";

    public static final String KEY_TIMEZONE_TYPE = "timezoneType";
    public static final String TIMEZONE_TYPE_AUTO = "auto";
    public static final String TIMEZONE_TYPE_HOME = "home";

    public static final String KEY_TIMEZONE_INSTANCES = "timezoneInstances";
    public static final String KEY_TIMEZONE_INSTANCES_PREVIOUS = "timezoneInstancesPrevious";

    public static final String COLUMN_NAME_ID = "_id";
    public static final String COLUMN_NAME_KEY = "key";
    public static final String COLUMN_NAME_VALUE = "value";

    private static final String[] sProjection = {
        COLUMN_NAME_KEY,
        COLUMN_NAME_VALUE
    };

    private static final int COLUMN_INDEX_KEY = 0;
    private static final int COLUMN_INDEX_VALUE = 1;

    private final SQLiteOpenHelper mOpenHelper;

    /**
     * This exception is thrown when the cache encounter a null key or a null database reference
     */
    public static class CacheException extends Exception {
        public CacheException() {
        }

        public CacheException(String detailMessage) {
            super(detailMessage);
        }
    }

    public CalendarCache(SQLiteOpenHelper openHelper) {
        mOpenHelper = openHelper;
    }

    public void writeTimezoneDatabaseVersion(String timezoneDatabaseVersion) throws CacheException {
        writeData(KEY_TIMEZONE_DATABASE_VERSION, timezoneDatabaseVersion);
    }

    public String readTimezoneDatabaseVersion() {
        try {
            return readData(KEY_TIMEZONE_DATABASE_VERSION);
        } catch (CacheException e) {
            Log.e(TAG, "Could not read timezone database version from CalendarCache");
        }
        return null;
    }

    @VisibleForTesting
    public void writeTimezoneType(String timezoneType) throws CacheException {
        writeData(KEY_TIMEZONE_TYPE, timezoneType);
    }

    public String readTimezoneType() {
        try {
            return readData(KEY_TIMEZONE_TYPE);
        } catch (CacheException e) {
            Log.e(TAG, "Cannot read timezone type from CalendarCache - using AUTO as default", e);
        }
        return TIMEZONE_TYPE_AUTO;
    }

    public void writeTimezoneInstances(String timezone) {
        try {
            writeData(KEY_TIMEZONE_INSTANCES, timezone);
        } catch (CacheException e) {
            Log.e(TAG, "Cannot write instances timezone to CalendarCache");
        }
    }

    public String readTimezoneInstances() {
        try {
            return readData(KEY_TIMEZONE_INSTANCES);
        } catch (CacheException e) {
            String localTimezone = TimeZone.getDefault().getID();
            Log.e(TAG, "Cannot read instances timezone from CalendarCache - using device one: " +
                    localTimezone, e);
            return localTimezone;
        }
    }

    public void writeTimezoneInstancesPrevious(String timezone) {
        try {
            writeData(KEY_TIMEZONE_INSTANCES_PREVIOUS, timezone);
        } catch (CacheException e) {
            Log.e(TAG, "Cannot write previous instance timezone to CalendarCache");
        }
    }

    public String readTimezoneInstancesPrevious() {
        try {
            return readData(KEY_TIMEZONE_INSTANCES_PREVIOUS);
        } catch (CacheException e) {
            Log.e(TAG, "Cannot read previous instances timezone from CalendarCache", e);
        }
        return null;
    }

    /**
     * Write a (key, value) pair in the Cache.
     *
     * @param key the key (must not be null)
     * @param value the value (can be null)
     * @throws CacheException when key is null
     */
    public void writeData(String key, String value) throws CacheException {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            writeDataLocked(db, key, value);
            db.setTransactionSuccessful();
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.i(TAG, "Wrote (key, value) = [ " + key + ", " + value + "] ");
            }
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Write a (key, value) pair in the database used by the cache. This method should be called in
     * a transaction.
     *
     * @param db the database (must not be null)
     * @param key the key (must not be null)
     * @param value the value
     * @throws CacheException when key or database are null
     */
    protected void writeDataLocked(SQLiteDatabase db, String key, String value)
            throws CacheException {
        if (null == db) {
            throw new CacheException("Database cannot be null");
        }
        if (null == key) {
            throw new CacheException("Cannot use null key for write");
        }

        /*
         * Storing the hash code of a String into the _id column carries a (very) small risk
         * of weird behavior, because we're using it as a unique key, but hash codes aren't
         * guaranteed to be unique.  CalendarCache has a small set of keys that are known
         * ahead of time, so we should be okay.
         */
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_ID, key.hashCode());
        values.put(COLUMN_NAME_KEY, key);
        values.put(COLUMN_NAME_VALUE, value);

        db.replace(DATABASE_NAME, null /* null column hack */, values);
    }

    /**
     * Read a value from the database used by the cache and depending on a key.
     *
     * @param key the key from which we want the value (must not be null)
     * @return the value that was found for the key. Can be null if no key has been found
     * @throws CacheException when key is null
     */
    public String readData(String key) throws CacheException {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        return readDataLocked(db, key);
    }

    /**
     * Read a value from the database used by the cache and depending on a key. The database should
     * be "readable" at minimum
     *
     * @param db the database (must not be null)
     * @param key the key from which we want the value (must not be null)
     * @return the value that was found for the key. Can be null if no value has been found for the
     * key.
     * @throws CacheException when key or database are null
     */
    protected String readDataLocked(SQLiteDatabase db, String key) throws CacheException {
        if (null == db) {
            throw new CacheException("Database cannot be null");
        }
        if (null == key) {
            throw new CacheException("Cannot use null key for read");
        }

        String rowValue = null;

        Cursor cursor = db.query(DATABASE_NAME, sProjection,
                COLUMN_NAME_KEY + "=?", new String[] { key }, null, null, null);
        try {
            if (cursor.moveToNext()) {
                rowValue = cursor.getString(COLUMN_INDEX_VALUE);
            }
            else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.i(TAG, "Could not find key = [ " + key + " ]");
                }
            }
        } finally {
            cursor.close();
            cursor = null;
        }
        return rowValue;
    }
}
