/*
**
** Copyright 2008, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** See the License for the specific language governing permissions and
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** limitations under the License.
*/

package com.android.providers.calendar;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.CalendarContract.CalendarMetaData;

/**
 * The global meta-data used for expanding the Instances table is stored in one
 * row of the "CalendarMetaData" table.  This class is used for caching those
 * values to avoid repeatedly banging on the database.  It is also used
 * for writing the values back to the database, while maintaining the
 * consistency of the cache.
 * <p>
 * TODO: there must be only one of these active within CalendarProvider.  Enforce this.
 */
public class MetaData {
    /**
     * These fields are updated atomically with the database.
     * If fields are added or removed from the CalendarMetaData table, those
     * changes must also be reflected here.
     */
    public class Fields {
        public String timezone;     // local timezone used for Instance expansion
        public long minInstance;    // UTC millis
        public long maxInstance;    // UTC millis
    }

    /**
     * The cached copy of the meta-data fields from the database.
     */
    private Fields mFields = new Fields();

    private final SQLiteOpenHelper mOpenHelper;
    private boolean mInitialized;

    /**
     * The column names in the CalendarMetaData table.  This projection
     * must contain all of the columns.
     */
    private static final String[] sCalendarMetaDataProjection = {
        CalendarMetaData.LOCAL_TIMEZONE,
        CalendarMetaData.MIN_INSTANCE,
        CalendarMetaData.MAX_INSTANCE};

    private static final int METADATA_INDEX_LOCAL_TIMEZONE = 0;
    private static final int METADATA_INDEX_MIN_INSTANCE = 1;
    private static final int METADATA_INDEX_MAX_INSTANCE = 2;

    public MetaData(SQLiteOpenHelper openHelper) {
        mOpenHelper = openHelper;
    }

    /**
     * Returns a copy of all the MetaData fields.  This method grabs a
     * database lock to read all the fields atomically.
     *
     * @return a copy of all the MetaData fields.
     */
    public Fields getFields() {
        Fields fields = new Fields();
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            // If the fields have not been initialized from the database,
            // then read the database.
            if (!mInitialized) {
                readLocked(db);
            }
            fields.timezone = mFields.timezone;
            fields.minInstance = mFields.minInstance;
            fields.maxInstance = mFields.maxInstance;
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return fields;
    }

    /**
     * This method must be called only while holding a database lock.
     *
     * <p>
     * Returns a copy of all the MetaData fields.  This method assumes
     * the database lock has already been acquired.
     * </p>
     *
     * @return a copy of all the MetaData fields.
     */
    public Fields getFieldsLocked() {
        Fields fields = new Fields();

        // If the fields have not been initialized from the database,
        // then read the database.
        if (!mInitialized) {
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();
            readLocked(db);
        }
        fields.timezone = mFields.timezone;
        fields.minInstance = mFields.minInstance;
        fields.maxInstance = mFields.maxInstance;
        return fields;
    }

    /**
     * Reads the meta-data for the CalendarProvider from the database and
     * updates the member variables.  This method executes while the database
     * lock is held.  If there were no exceptions reading the database,
     * mInitialized is set to true.
     */
    private void readLocked(SQLiteDatabase db) {
        String timezone = null;
        long minInstance = 0, maxInstance = 0;

        // Read the database directly.  We only do this once to initialize
        // the members of this class.
        Cursor cursor = db.query("CalendarMetaData", sCalendarMetaDataProjection,
                null, null, null, null, null);
        try {
            if (cursor.moveToNext()) {
                timezone = cursor.getString(METADATA_INDEX_LOCAL_TIMEZONE);
                minInstance = cursor.getLong(METADATA_INDEX_MIN_INSTANCE);
                maxInstance = cursor.getLong(METADATA_INDEX_MAX_INSTANCE);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Cache the result of reading the database
        mFields.timezone = timezone;
        mFields.minInstance = minInstance;
        mFields.maxInstance = maxInstance;

        // Mark the fields as initialized
        mInitialized = true;
    }

    /**
     * Writes the meta-data for the CalendarProvider.  The values to write are
     * passed in as parameters.  All of the values are updated atomically,
     * including the cached copy of the meta-data.
     *
     * @param timezone the local timezone used for Instance expansion
     * @param begin the start of the Instance expansion in UTC milliseconds
     * @param end the end of the Instance expansion in UTC milliseconds
     * @param startDay the start of the BusyBit expansion (the start Julian day)
     * @param endDay the end of the BusyBit expansion (the end Julian day)
     */
    public void write(String timezone, long begin, long end, int startDay, int endDay) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            writeLocked(timezone, begin, end);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * This method must be called only while holding a database lock.
     *
     * <p>
     * Writes the meta-data for the CalendarProvider.  The values to write are
     * passed in as parameters.  All of the values are updated atomically,
     * including the cached copy of the meta-data.
     * </p>
     *
     * @param timezone the local timezone used for Instance expansion
     * @param begin the start of the Instance expansion in UTC milliseconds
     * @param end the end of the Instance expansion in UTC milliseconds
     */
    public void writeLocked(String timezone, long begin, long end) {
        ContentValues values = new ContentValues();
        values.put("_id", 1);
        values.put(CalendarMetaData.LOCAL_TIMEZONE, timezone);
        values.put(CalendarMetaData.MIN_INSTANCE, begin);
        values.put(CalendarMetaData.MAX_INSTANCE, end);

        // Atomically update the database and the cached members.
        try {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            db.replace("CalendarMetaData", null, values);
        } catch (RuntimeException e) {
            // Failed: zero the in-memory fields to force recomputation.
            mFields.timezone = null;
            mFields.minInstance = mFields.maxInstance = 0;
            throw e;
        }

        // Update the cached members last in case the database update fails
        mFields.timezone = timezone;
        mFields.minInstance = begin;
        mFields.maxInstance = end;
    }

    /**
     * Clears the time range for the Instances table.  The rows in the
     * Instances table will be deleted (and regenerated) the next time
     * that the Instances table is queried.
     *
     * Also clears the time range for the BusyBits table because that depends
     * on the Instances table.
     */
    public void clearInstanceRange() {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            // If the fields have not been initialized from the database,
            // then read the database.
            if (!mInitialized) {
                readLocked(db);
            }
            writeLocked(mFields.timezone, 0 /* begin */, 0 /* end */);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
