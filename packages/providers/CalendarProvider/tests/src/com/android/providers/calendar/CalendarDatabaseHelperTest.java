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
 * limitations under the License
 */
package com.android.providers.calendar;


import com.android.common.content.SyncStateContentProviderHelper;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;

import junit.framework.TestCase;

public class CalendarDatabaseHelperTest extends TestCase {
    private static final String TAG = "CDbHelperTest";

    private SQLiteDatabase mBadDb;
    private SQLiteDatabase mGoodDb;
    private DatabaseUtils.InsertHelper mBadEventsInserter;
    private DatabaseUtils.InsertHelper mGoodEventsInserter;

    @Override
    public void setUp() {
        mBadDb = SQLiteDatabase.create(null);
        assertNotNull(mBadDb);
        mGoodDb = SQLiteDatabase.create(null);
        assertNotNull(mGoodDb);
    }

    protected void bootstrapDbVersion50(SQLiteDatabase db) {

        // TODO remove the dependency on this system class
        SyncStateContentProviderHelper syncStateHelper = new SyncStateContentProviderHelper();
        syncStateHelper.createDatabase(db);

        db.execSQL("CREATE TABLE Calendars (" +
                        "_id INTEGER PRIMARY KEY," +
                        "_sync_account TEXT," +
                        "_sync_id TEXT," +
                        "_sync_version TEXT," +
                        "_sync_time TEXT," +            // UTC
                        "_sync_local_id INTEGER," +
                        "_sync_dirty INTEGER," +
                        "_sync_mark INTEGER," + // Used to filter out new rows
                        "url TEXT," +
                        "name TEXT," +
                        "displayName TEXT," +
                        "hidden INTEGER NOT NULL DEFAULT 0," +
                        "color INTEGER," +
                        "access_level INTEGER," +
                        "selected INTEGER NOT NULL DEFAULT 1," +
                        "sync_events INTEGER NOT NULL DEFAULT 0," +
                        "location TEXT," +
                        "timezone TEXT" +
                        ");");

        // Trigger to remove a calendar's events when we delete the calendar
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars " +
                    "BEGIN " +
                        "DELETE FROM Events WHERE calendar_id = old._id;" +
                        "DELETE FROM DeletedEvents WHERE calendar_id = old._id;" +
                    "END");

        // TODO: do we need both dtend and duration?
        db.execSQL("CREATE TABLE Events (" +
                        "_id INTEGER PRIMARY KEY," +
                        "_sync_account TEXT," +
                        "_sync_id TEXT," +
                        "_sync_version TEXT," +
                        "_sync_time TEXT," +            // UTC
                        "_sync_local_id INTEGER," +
                        "_sync_dirty INTEGER," +
                        "_sync_mark INTEGER," + // To filter out new rows
                        // TODO remove NOT NULL when upgrade rebuilds events to have
                        // true v50 schema
                        "calendar_id INTEGER NOT NULL," +
                        "htmlUri TEXT," +
                        "title TEXT," +
                        "eventLocation TEXT," +
                        "description TEXT," +
                        "eventStatus INTEGER," +
                        "selfAttendeeStatus INTEGER NOT NULL DEFAULT 0," +
                        "commentsUri TEXT," +
                        "dtstart INTEGER," +               // millis since epoch
                        "dtend INTEGER," +                 // millis since epoch
                        "eventTimezone TEXT," +         // timezone for event
                        "duration TEXT," +
                        "allDay INTEGER NOT NULL DEFAULT 0," +
                        "visibility INTEGER NOT NULL DEFAULT 0," +
                        "transparency INTEGER NOT NULL DEFAULT 0," +
                        "hasAlarm INTEGER NOT NULL DEFAULT 0," +
                        "hasExtendedProperties INTEGER NOT NULL DEFAULT 0," +
                        "rrule TEXT," +
                        "rdate TEXT," +
                        "exrule TEXT," +
                        "exdate TEXT," +
                        "originalEvent TEXT," +
                        "originalInstanceTime INTEGER," +  // millis since epoch
                        "lastDate INTEGER" +               // millis since epoch
                    ");");

        db.execSQL("CREATE INDEX eventsCalendarIdIndex ON Events (calendar_id);");

        db.execSQL("CREATE TABLE EventsRawTimes (" +
                        "_id INTEGER PRIMARY KEY," +
                        "event_id INTEGER NOT NULL," +
                        "dtstart2445 TEXT," +
                        "dtend2445 TEXT," +
                        "originalInstanceTime2445 TEXT," +
                        "lastDate2445 TEXT," +
                        "UNIQUE (event_id)" +
                    ");");

        // NOTE: we do not create a trigger to delete an event's instances upon update,
        // as all rows currently get updated during a merge.

        db.execSQL("CREATE TABLE DeletedEvents (" +
                        "_sync_id TEXT," +
                        "_sync_version TEXT," +
                        "_sync_account TEXT," +
                        "_sync_mark INTEGER" + // To filter out new rows
                    ");");

        db.execSQL("CREATE TABLE Instances (" +
                        "_id INTEGER PRIMARY KEY," +
                        "event_id INTEGER," +
                        "begin INTEGER," +         // UTC millis
                        "end INTEGER," +           // UTC millis
                        "startDay INTEGER," +      // Julian start day
                        "endDay INTEGER," +        // Julian end day
                        "startMinute INTEGER," +   // minutes from midnight
                        "endMinute INTEGER," +     // minutes from midnight
                        "UNIQUE (event_id, begin, end)" +
                    ");");

        db.execSQL("CREATE INDEX instancesStartDayIndex ON Instances (startDay);");

        db.execSQL("CREATE TABLE CalendarMetaData (" +
                        "_id INTEGER PRIMARY KEY," +
                        "localTimezone TEXT," +
                        "minInstance INTEGER," +      // UTC millis
                        "maxInstance INTEGER," +      // UTC millis
                        "minBusyBits INTEGER," +      // UTC millis
                        "maxBusyBits INTEGER" +       // UTC millis
        ");");

        db.execSQL("CREATE TABLE BusyBits(" +
                        "day INTEGER PRIMARY KEY," +  // the Julian day
                        "busyBits INTEGER," +         // 24 bits for 60-minute intervals
                        "allDayCount INTEGER" +       // number of all-day events
        ");");

        db.execSQL("CREATE TABLE Attendees (" +
                        "_id INTEGER PRIMARY KEY," +
                        "event_id INTEGER," +
                        "attendeeName TEXT," +
                        "attendeeEmail TEXT," +
                        "attendeeStatus INTEGER," +
                        "attendeeRelationship INTEGER," +
                        "attendeeType INTEGER" +
                   ");");

        db.execSQL("CREATE INDEX attendeesEventIdIndex ON Attendees (event_id);");

        db.execSQL("CREATE TABLE Reminders (" +
                        "_id INTEGER PRIMARY KEY," +
                        "event_id INTEGER," +
                        "minutes INTEGER," +
                        "method INTEGER NOT NULL" +
                        " DEFAULT 0);");

        db.execSQL("CREATE INDEX remindersEventIdIndex ON Reminders (event_id);");

        // This table stores the Calendar notifications that have gone off.
        db.execSQL("CREATE TABLE CalendarAlerts (" +
                        "_id INTEGER PRIMARY KEY," +
                        "event_id INTEGER," +
                        "begin INTEGER NOT NULL," +        // UTC millis
                        "end INTEGER NOT NULL," +          // UTC millis
                        "alarmTime INTEGER NOT NULL," +    // UTC millis
                        "state INTEGER NOT NULL," +
                        "minutes INTEGER," +
                        "UNIQUE (alarmTime, begin, event_id)" +
                   ");");

        db.execSQL("CREATE INDEX calendarAlertsEventIdIndex ON CalendarAlerts (event_id);");

        db.execSQL("CREATE TABLE ExtendedProperties (" +
                        "_id INTEGER PRIMARY KEY," +
                        "event_id INTEGER," +
                        "name TEXT," +
                        "value TEXT" +
                   ");");

        db.execSQL("CREATE INDEX extendedPropertiesEventIdIndex ON ExtendedProperties (event_id);");

        // Trigger to remove data tied to an event when we delete that event.
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events " +
                    "BEGIN " +
                        "DELETE FROM Instances WHERE event_id = old._id;" +
                        "DELETE FROM EventsRawTimes WHERE event_id = old._id;" +
                        "DELETE FROM Attendees WHERE event_id = old._id;" +
                        "DELETE FROM Reminders WHERE event_id = old._id;" +
                        "DELETE FROM CalendarAlerts WHERE event_id = old._id;" +
                        "DELETE FROM ExtendedProperties WHERE event_id = old._id;" +
                    "END");

        // Triggers to set the _sync_dirty flag when an attendee is changed,
        // inserted or deleted
        db.execSQL("CREATE TRIGGER attendees_update UPDATE ON Attendees " +
                    "BEGIN " +
                        "UPDATE Events SET _sync_dirty=1 WHERE Events._id=old.event_id;" +
                    "END");
        db.execSQL("CREATE TRIGGER attendees_insert INSERT ON Attendees " +
                    "BEGIN " +
                        "UPDATE Events SET _sync_dirty=1 WHERE Events._id=new.event_id;" +
                    "END");
        db.execSQL("CREATE TRIGGER attendees_delete DELETE ON Attendees " +
                    "BEGIN " +
                        "UPDATE Events SET _sync_dirty=1 WHERE Events._id=old.event_id;" +
                    "END");

        // Triggers to set the _sync_dirty flag when a reminder is changed,
        // inserted or deleted
        db.execSQL("CREATE TRIGGER reminders_update UPDATE ON Reminders " +
                    "BEGIN " +
                        "UPDATE Events SET _sync_dirty=1 WHERE Events._id=old.event_id;" +
                    "END");
        db.execSQL("CREATE TRIGGER reminders_insert INSERT ON Reminders " +
                    "BEGIN " +
                        "UPDATE Events SET _sync_dirty=1 WHERE Events._id=new.event_id;" +
                    "END");
        db.execSQL("CREATE TRIGGER reminders_delete DELETE ON Reminders " +
                    "BEGIN " +
                        "UPDATE Events SET _sync_dirty=1 WHERE Events._id=old.event_id;" +
                    "END");
        // Triggers to set the _sync_dirty flag when an extended property is changed,
        // inserted or deleted
        db.execSQL("CREATE TRIGGER extended_properties_update UPDATE ON ExtendedProperties " +
                    "BEGIN " +
                        "UPDATE Events SET _sync_dirty=1 WHERE Events._id=old.event_id;" +
                    "END");
        db.execSQL("CREATE TRIGGER extended_properties_insert UPDATE ON ExtendedProperties " +
                    "BEGIN " +
                        "UPDATE Events SET _sync_dirty=1 WHERE Events._id=new.event_id;" +
                    "END");
        db.execSQL("CREATE TRIGGER extended_properties_delete UPDATE ON ExtendedProperties " +
                    "BEGIN " +
                        "UPDATE Events SET _sync_dirty=1 WHERE Events._id=old.event_id;" +
                    "END");
    }

    private void createVersion67EventsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Events (" +
                "_id INTEGER PRIMARY KEY," +
                "_sync_account TEXT," +
                "_sync_account_type TEXT," +
                "_sync_id TEXT," +
                "_sync_version TEXT," +
                "_sync_time TEXT," +            // UTC
                "_sync_local_id INTEGER," +
                "_sync_dirty INTEGER," +
                "_sync_mark INTEGER," + // To filter out new rows
                "calendar_id INTEGER NOT NULL," +
                "htmlUri TEXT," +
                "title TEXT," +
                "eventLocation TEXT," +
                "description TEXT," +
                "eventStatus INTEGER," +
                "selfAttendeeStatus INTEGER NOT NULL DEFAULT 0," +
                "commentsUri TEXT," +
                "dtstart INTEGER," +               // millis since epoch
                "dtend INTEGER," +                 // millis since epoch
                "eventTimezone TEXT," +         // timezone for event
                "duration TEXT," +
                "allDay INTEGER NOT NULL DEFAULT 0," +
                "visibility INTEGER NOT NULL DEFAULT 0," +
                "transparency INTEGER NOT NULL DEFAULT 0," +
                "hasAlarm INTEGER NOT NULL DEFAULT 0," +
                "hasExtendedProperties INTEGER NOT NULL DEFAULT 0," +
                "rrule TEXT," +
                "rdate TEXT," +
                "exrule TEXT," +
                "exdate TEXT," +
                "originalEvent TEXT," +  // _sync_id of recurring event
                "originalInstanceTime INTEGER," +  // millis since epoch
                "originalAllDay INTEGER," +
                "lastDate INTEGER," +               // millis since epoch
                "hasAttendeeData INTEGER NOT NULL DEFAULT 0," +
                "guestsCanModify INTEGER NOT NULL DEFAULT 0," +
                "guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1," +
                "guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1," +
                "organizer STRING," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "dtstart2 INTEGER," + //millis since epoch, allDay events in local timezone
                "dtend2 INTEGER," + //millis since epoch, allDay events in local timezone
                "eventTimezone2 TEXT," + //timezone for event with allDay events in local timezone
                "syncAdapterData TEXT" + //available for use by sync adapters
                ");");
    }

    private void addVersion50Events() {
        // April 5th 1:01:01 AM to April 6th 1:01:01
        mBadDb.execSQL("INSERT INTO Events (_id,dtstart,dtend,duration," +
                "eventTimezone,allDay,calendar_id) " +
                "VALUES (1,1270454471000,1270540872000,'P10S'," +
                "'America/Los_Angeles',1,1);");

        // April 5th midnight to April 6th midnight, duration cleared
        mGoodDb.execSQL("INSERT INTO Events (_id,dtstart,dtend,duration," +
                "eventTimezone,allDay,calendar_id) " +
                "VALUES (1,1270425600000,1270512000000,null," +
                "'UTC',1,1);");

        // April 5th 1:01:01 AM to April 6th 1:01:01, recurring weekly (We only check for the
        // existence of an rrule so it doesn't matter if the day is correct)
        mBadDb.execSQL("INSERT INTO Events (_id,dtstart,dtend,duration," +
                "eventTimezone,allDay,rrule,calendar_id) " +
                "VALUES (2,1270454462000,1270540863000," +
                "'P10S','America/Los_Angeles',1," +
                "'WEEKLY:MON',1);");

        // April 5th midnight with 1 day duration, if only dtend was wrong we wouldn't fix it, but
        // if anything else is wrong we clear dtend to be sure.
        mGoodDb.execSQL("INSERT INTO Events (" +
                "_id,dtstart,dtend,duration," +
                "eventTimezone,allDay,rrule,calendar_id)" +
                "VALUES (2,1270425600000,null,'P1D'," +
                "'UTC',1," +
                "'WEEKLY:MON',1);");

        assertEquals(mBadDb.rawQuery("SELECT _id FROM Events;", null).getCount(), 2);
        assertEquals(mGoodDb.rawQuery("SELECT _id FROM Events;", null).getCount(), 2);
    }

    private void addVersion67Events() {
        // April 5th 1:01:01 AM to April 6th 1:01:01
        mBadDb.execSQL("INSERT INTO Events (_id,dtstart,dtend,duration,dtstart2,dtend2," +
                "eventTimezone,eventTimezone2,allDay,calendar_id) " +
                "VALUES (1,1270454471000,1270540872000,'P10S'," +
                "1270454460000,1270540861000,'America/Los_Angeles','America/Los_Angeles',1,1);");

        // April 5th midnight to April 6th midnight, duration cleared
        mGoodDb.execSQL("INSERT INTO Events (_id,dtstart,dtend,duration,dtstart2,dtend2," +
                "eventTimezone,eventTimezone2,allDay,calendar_id) " +
                "VALUES (1,1270425600000,1270512000000,null," +
                "1270450800000,1270537200000,'UTC','America/Los_Angeles',1,1);");

        // April 5th 1:01:01 AM to April 6th 1:01:01, recurring weekly (We only check for the
        // existence of an rrule so it doesn't matter if the day is correct)
        mBadDb.execSQL("INSERT INTO Events (_id,dtstart,dtend,duration,dtstart2,dtend2," +
                "eventTimezone,eventTimezone2,allDay,rrule,calendar_id) " +
                "VALUES (2,1270454462000,1270540863000," +
                "'P10S',1270454461000,1270540861000,'America/Los_Angeles','America/Los_Angeles',1," +
                "'WEEKLY:MON',1);");

        // April 5th midnight with 1 day duration, if only dtend was wrong we wouldn't fix it, but
        // if anything else is wrong we clear dtend to be sure.
        mGoodDb.execSQL("INSERT INTO Events (" +
                "_id,dtstart,dtend,duration,dtstart2,dtend2," +
                "eventTimezone,eventTimezone2,allDay,rrule,calendar_id)" +
                "VALUES (2,1270425600000,null,'P1D',1270450800000,null," +
                "'UTC','America/Los_Angeles',1," +
                "'WEEKLY:MON',1);");

        assertEquals(mBadDb.rawQuery("SELECT _id FROM Events;", null).getCount(), 2);
        assertEquals(mGoodDb.rawQuery("SELECT _id FROM Events;", null).getCount(), 2);
    }

    @MediumTest
    public void testUpgradeToVersion69() {
        // Create event tables
        createVersion67EventsTable(mBadDb);
        createVersion67EventsTable(mGoodDb);
        // Fill in good and bad events
        addVersion67Events();
        // Run the upgrade on the bad events
        CalendarDatabaseHelper.upgradeToVersion69(mBadDb);
        Cursor badCursor = null;
        Cursor goodCursor = null;
        try {
            badCursor = mBadDb.rawQuery("SELECT _id,dtstart,dtend,duration,dtstart2,dtend2," +
                    "eventTimezone,eventTimezone2,rrule FROM Events WHERE allDay=?",
                    new String[] {"1"});
            goodCursor = mGoodDb.rawQuery("SELECT _id,dtstart,dtend,duration,dtstart2,dtend2," +
                    "eventTimezone,eventTimezone2,rrule FROM Events WHERE allDay=?",
                    new String[] {"1"});
            // Check that we get the correct results back
            assertTrue(compareCursors(badCursor, goodCursor));
        } finally {
            if (badCursor != null) {
                badCursor.close();
            }
            if (goodCursor != null) {
                goodCursor.close();
            }
        }
    }

    @MediumTest
    public void testUpgradeToCurrentVersion() {
        // Create event tables
        bootstrapDbVersion50(mBadDb);
        bootstrapDbVersion50(mGoodDb);
        // Fill in good and bad events
        addVersion50Events();
        // Run the upgrade on the bad events
        CalendarDatabaseHelper cDbHelper = new CalendarDatabaseHelper(new MockContext());
        cDbHelper.mInTestMode = true;
        cDbHelper.onUpgrade(mBadDb, 50, CalendarDatabaseHelper.DATABASE_VERSION);
        Cursor badCursor = null;
        Cursor goodCursor = null;
        try {
            badCursor = mBadDb.rawQuery("SELECT _id,dtstart,dtend,duration," +
                    "eventTimezone,rrule FROM Events WHERE allDay=?",
                    new String[] {"1"});
            goodCursor = mGoodDb.rawQuery("SELECT _id,dtstart,dtend,duration," +
                    "eventTimezone,rrule FROM Events WHERE allDay=?",
                    new String[] {"1"});
            // Check that we get the correct results back
            assertTrue(compareCursors(badCursor, goodCursor));
        } finally {
            if (badCursor != null) {
                badCursor.close();
            }
            if (goodCursor != null) {
                goodCursor.close();
            }
        }
    }

    private static final String SQLITE_MASTER = "sqlite_master";

    private static final String[] PROJECTION = {"tbl_name", "sql"};

    public void testSchemasEqualForAllTables() {

        CalendarDatabaseHelper cDbHelper = new CalendarDatabaseHelper(new MockContext());
        cDbHelper.mInTestMode = true;
        bootstrapDbVersion50(mBadDb);
        cDbHelper.onCreate(mGoodDb);
        cDbHelper.onUpgrade(mBadDb, 50, CalendarDatabaseHelper.DATABASE_VERSION);
        // Check that for all tables, schema definitions are the same between updated db and new db.
        Cursor goodCursor = mGoodDb.query(SQLITE_MASTER, PROJECTION, null, null, null, null,
                "tbl_name,sql" /* orderBy */);
        Cursor badCursor = mBadDb.query(SQLITE_MASTER, PROJECTION, null, null, null, null,
                "tbl_name,sql" /* orderBy */);

        while (goodCursor.moveToNext()) {
            String goodTableName = goodCursor.getString(0);
            // Ignore tables that do not belong to calendar
            if (goodTableName.startsWith("sqlite_") || goodTableName.equals("android_metadata")) {
                continue;
            }

            // Ignore tables that do not belong to calendar
            String badTableName;
            do {
                assertTrue("Should have same number of tables", badCursor.moveToNext());
                badTableName = badCursor.getString(0);
            } while (badTableName.startsWith("sqlite_") || badTableName.equals("android_metadata"));

            assertEquals("Table names different between upgraded schema and freshly-created scheme",
                    goodTableName, badTableName);

            String badString = badCursor.getString(1);
            String goodString = goodCursor.getString(1);
            if (badString == null && goodString == null) {
                continue;
            }
            // Have to strip out some special characters and collapse spaces to
            // get reasonable output
            badString = badString.replaceAll("[()]", "");
            goodString = goodString.replaceAll("[()]", "");
            badString = badString.replaceAll(" +", " ");
            goodString = goodString.replaceAll(" +", " ");
            // And then split on commas and trim whitespace
            String[] badSql = badString.split(",");
            String[] goodSql = goodString.split(",");
            for (int i = 0; i < badSql.length; i++) {
                badSql[i] = badSql[i].trim();
            }
            for (int i = 0; i < goodSql.length; i++) {
                goodSql[i] = goodSql[i].trim();
            }
            Arrays.sort(badSql);
            Arrays.sort(goodSql);
            assertTrue("Table schema different for table " + goodCursor.getString(0) + ": <"
                    + Arrays.toString(goodSql) + "> -- <" + Arrays.toString(badSql) + ">",
                    Arrays.equals(goodSql, badSql));
        }
        assertFalse("Should have same number of tables", badCursor.moveToNext());
    }

    /**
     * Compares two cursors to see if they contain the same data.
     *
     * @return Returns true of the cursors contain the same data and are not null, false
     * otherwise
     */
    private static boolean compareCursors(Cursor c1, Cursor c2) {
        if(c1 == null || c2 == null) {
            Log.d("CDBT","c1 is " + c1 + " and c2 is " + c2);
            return false;
        }

        int numColumns = c1.getColumnCount();
        if (numColumns != c2.getColumnCount()) {
            Log.d("CDBT","c1 has " + numColumns + " columns and c2 has " + c2.getColumnCount());
            return false;
        }

        if (c1.getCount() != c2.getCount()) {
            Log.d("CDBT","c1 has " + c1.getCount() + " rows and c2 has " + c2.getCount());
            return false;
        }

        c1.moveToPosition(-1);
        c2.moveToPosition(-1);
        while(c1.moveToNext() && c2.moveToNext()) {
            for(int i = 0; i < numColumns; i++) {
                if(!TextUtils.equals(c1.getString(i),c2.getString(i))) {
                    Log.d("CDBT", c1.getString(i) + "\n" + c2.getString(i));
                    return false;
                }
            }
        }

        return true;
    }
}
