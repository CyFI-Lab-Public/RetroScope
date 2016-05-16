/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.provider.SyncStateContract;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import com.android.common.content.SyncStateContentProviderHelper;
import com.google.common.annotations.VisibleForTesting;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.TimeZone;

/**
 * Database helper for calendar. Designed as a singleton to make sure that all
 * {@link android.content.ContentProvider} users get the same reference.
 */
/* package */ class CalendarDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "CalendarDatabaseHelper";

    private static final boolean LOGD = false;

    @VisibleForTesting
    public boolean mInTestMode = false;

    private static final String DATABASE_NAME = "calendar.db";

    private static final int DAY_IN_SECONDS = 24 * 60 * 60;

    // Note: if you update the version number, you must also update the code
    // in upgradeDatabase() to modify the database (gracefully, if possible).
    //
    //  xx Froyo and prior
    // 1xx for Gingerbread,
    // 2xx for Honeycomb
    // 3xx for ICS
    // 4xx for JB
    // 5xx for JB MR1
    // 6xx for K
    // Bump this to the next hundred at each major release.
    static final int DATABASE_VERSION = 600;

    private static final int PRE_FROYO_SYNC_STATE_VERSION = 3;

    // columns used to duplicate an event row
    private static final String LAST_SYNCED_EVENT_COLUMNS =
            Events._SYNC_ID + "," +
            Events.CALENDAR_ID + "," +
            Events.TITLE + "," +
            Events.EVENT_LOCATION + "," +
            Events.DESCRIPTION + "," +
            Events.EVENT_COLOR + "," +
            Events.EVENT_COLOR_KEY + "," +
            Events.STATUS + "," +
            Events.SELF_ATTENDEE_STATUS + "," +
            Events.DTSTART + "," +
            Events.DTEND + "," +
            Events.EVENT_TIMEZONE + "," +
            Events.EVENT_END_TIMEZONE + "," +
            Events.DURATION + "," +
            Events.ALL_DAY + "," +
            Events.ACCESS_LEVEL + "," +
            Events.AVAILABILITY + "," +
            Events.HAS_ALARM + "," +
            Events.HAS_EXTENDED_PROPERTIES + "," +
            Events.RRULE + "," +
            Events.RDATE + "," +
            Events.EXRULE + "," +
            Events.EXDATE + "," +
            Events.ORIGINAL_SYNC_ID + "," +
            Events.ORIGINAL_ID + "," +
            Events.ORIGINAL_INSTANCE_TIME + "," +
            Events.ORIGINAL_ALL_DAY + "," +
            Events.LAST_DATE + "," +
            Events.HAS_ATTENDEE_DATA + "," +
            Events.GUESTS_CAN_MODIFY + "," +
            Events.GUESTS_CAN_INVITE_OTHERS + "," +
            Events.GUESTS_CAN_SEE_GUESTS + "," +
            Events.ORGANIZER + "," +
            Events.IS_ORGANIZER + "," +
            Events.CUSTOM_APP_PACKAGE + "," +
            Events.CUSTOM_APP_URI + "," +
            Events.UID_2445;

    // columns used to duplicate a reminder row
    private static final String LAST_SYNCED_REMINDER_COLUMNS =
            Reminders.MINUTES + "," +
            Reminders.METHOD;

    // columns used to duplicate an attendee row
    private static final String LAST_SYNCED_ATTENDEE_COLUMNS =
            Attendees.ATTENDEE_NAME + "," +
            Attendees.ATTENDEE_EMAIL + "," +
            Attendees.ATTENDEE_STATUS + "," +
            Attendees.ATTENDEE_RELATIONSHIP + "," +
            Attendees.ATTENDEE_TYPE + "," +
            Attendees.ATTENDEE_IDENTITY + "," +
            Attendees.ATTENDEE_ID_NAMESPACE;

    // columns used to duplicate an extended property row
    private static final String LAST_SYNCED_EXTENDED_PROPERTY_COLUMNS =
            CalendarContract.ExtendedProperties.NAME + "," +
            CalendarContract.ExtendedProperties.VALUE;

    public interface Tables {
        public static final String CALENDARS = "Calendars";
        public static final String EVENTS = "Events";
        public static final String EVENTS_RAW_TIMES = "EventsRawTimes";
        public static final String INSTANCES = "Instances";
        public static final String ATTENDEES = "Attendees";
        public static final String REMINDERS = "Reminders";
        public static final String CALENDAR_ALERTS = "CalendarAlerts";
        public static final String EXTENDED_PROPERTIES = "ExtendedProperties";
        public static final String CALENDAR_META_DATA = "CalendarMetaData";
        public static final String CALENDAR_CACHE = "CalendarCache";
        public static final String SYNC_STATE = "_sync_state";
        public static final String SYNC_STATE_META = "_sync_state_metadata";
        public static final String COLORS = "Colors";
    }

    public interface Views {
        public static final String EVENTS = "view_events";
    }

    // Copied from SyncStateContentProviderHelper.  Don't really want to make them public there.
    private static final String SYNC_STATE_META_VERSION_COLUMN = "version";

    // This needs to be done when all the tables are already created
    private static final String EVENTS_CLEANUP_TRIGGER_SQL =
            "DELETE FROM " + Tables.INSTANCES +
                " WHERE "+ CalendarContract.Instances.EVENT_ID + "=" +
                    "old." + CalendarContract.Events._ID + ";" +
            "DELETE FROM " + Tables.EVENTS_RAW_TIMES +
                " WHERE " + CalendarContract.EventsRawTimes.EVENT_ID + "=" +
                    "old." + CalendarContract.Events._ID + ";" +
            "DELETE FROM " + Tables.ATTENDEES +
                " WHERE " + CalendarContract.Attendees.EVENT_ID + "=" +
                    "old." + CalendarContract.Events._ID + ";" +
            "DELETE FROM " + Tables.REMINDERS +
                " WHERE " + CalendarContract.Reminders.EVENT_ID + "=" +
                    "old." + CalendarContract.Events._ID + ";" +
            "DELETE FROM " + Tables.CALENDAR_ALERTS +
                " WHERE " + CalendarContract.CalendarAlerts.EVENT_ID + "=" +
                    "old." + CalendarContract.Events._ID + ";" +
            "DELETE FROM " + Tables.EXTENDED_PROPERTIES +
                " WHERE " + CalendarContract.ExtendedProperties.EVENT_ID + "=" +
                    "old." + CalendarContract.Events._ID + ";";

    // This ensures any exceptions based on an event get their original_sync_id
    // column set when an the _sync_id is set.
    private static final String EVENTS_ORIGINAL_SYNC_TRIGGER_SQL =
            "UPDATE " + Tables.EVENTS +
                " SET " + Events.ORIGINAL_SYNC_ID + "=new." + Events._SYNC_ID +
                " WHERE " + Events.ORIGINAL_ID + "=old." + Events._ID + ";";

    private static final String SYNC_ID_UPDATE_TRIGGER_NAME = "original_sync_update";
    private static final String CREATE_SYNC_ID_UPDATE_TRIGGER =
            "CREATE TRIGGER " + SYNC_ID_UPDATE_TRIGGER_NAME + " UPDATE OF " + Events._SYNC_ID +
            " ON " + Tables.EVENTS +
            " BEGIN " +
                EVENTS_ORIGINAL_SYNC_TRIGGER_SQL +
            " END";

    private static final String CALENDAR_CLEANUP_TRIGGER_SQL = "DELETE FROM " + Tables.EVENTS +
            " WHERE " + CalendarContract.Events.CALENDAR_ID + "=" +
                "old." + CalendarContract.Events._ID + ";";

    private static final String CALENDAR_UPDATE_COLOR_TRIGGER_SQL = "UPDATE " + Tables.CALENDARS
            + " SET calendar_color=(SELECT " + Colors.COLOR + " FROM " + Tables.COLORS + " WHERE "
            + Colors.ACCOUNT_NAME + "=" + "new." + Calendars.ACCOUNT_NAME + " AND "
            + Colors.ACCOUNT_TYPE + "=" + "new." + Calendars.ACCOUNT_TYPE + " AND "
            + Colors.COLOR_KEY + "=" + "new." + Calendars.CALENDAR_COLOR_KEY + " AND "
            + Colors.COLOR_TYPE + "=" + Colors.TYPE_CALENDAR + ") "
            + " WHERE " + Calendars._ID + "=" + "old." + Calendars._ID
            + ";";
    private static final String CALENDAR_COLOR_UPDATE_TRIGGER_NAME = "calendar_color_update";
    private static final String CREATE_CALENDAR_COLOR_UPDATE_TRIGGER = "CREATE TRIGGER "
            + CALENDAR_COLOR_UPDATE_TRIGGER_NAME + " UPDATE OF " + Calendars.CALENDAR_COLOR_KEY
            + " ON " + Tables.CALENDARS + " WHEN new." + Calendars.CALENDAR_COLOR_KEY
            + " NOT NULL BEGIN " + CALENDAR_UPDATE_COLOR_TRIGGER_SQL + " END";

    private static final String EVENT_UPDATE_COLOR_TRIGGER_SQL = "UPDATE " + Tables.EVENTS
            + " SET eventColor=(SELECT " + Colors.COLOR + " FROM " + Tables.COLORS + " WHERE "
            + Colors.ACCOUNT_NAME + "=" + "(SELECT " + Calendars.ACCOUNT_NAME + " FROM "
            + Tables.CALENDARS + " WHERE " + Calendars._ID + "=new." + Events.CALENDAR_ID
            + ") AND " + Colors.ACCOUNT_TYPE + "=" + "(SELECT " + Calendars.ACCOUNT_TYPE + " FROM "
            + Tables.CALENDARS + " WHERE " + Calendars._ID + "=new." + Events.CALENDAR_ID
            + ") AND " + Colors.COLOR_KEY + "=" + "new." + Events.EVENT_COLOR_KEY + " AND "
            + Colors.COLOR_TYPE + "=" + Colors.TYPE_EVENT + ") "
            + " WHERE " + Events._ID + "=" + "old." + Events._ID + ";";
    private static final String EVENT_COLOR_UPDATE_TRIGGER_NAME = "event_color_update";
    private static final String CREATE_EVENT_COLOR_UPDATE_TRIGGER = "CREATE TRIGGER "
            + EVENT_COLOR_UPDATE_TRIGGER_NAME + " UPDATE OF " + Events.EVENT_COLOR_KEY + " ON "
            + Tables.EVENTS + " WHEN new." + Events.EVENT_COLOR_KEY + " NOT NULL BEGIN "
            + EVENT_UPDATE_COLOR_TRIGGER_SQL + " END";

    /** Selects rows from Attendees for which the event_id refers to a nonexistent Event */
    private static final String WHERE_ATTENDEES_ORPHANS =
            Attendees.EVENT_ID + " IN (SELECT " + Attendees.EVENT_ID + " FROM " +
            Tables.ATTENDEES + " LEFT OUTER JOIN " + Tables.EVENTS + " ON " +
            Attendees.EVENT_ID + "=" + Tables.EVENTS + "." + Events._ID +
            " WHERE " + Tables.EVENTS + "." + Events._ID + " IS NULL)";
    /** Selects rows from Reminders for which the event_id refers to a nonexistent Event */
    private static final String WHERE_REMINDERS_ORPHANS =
            Reminders.EVENT_ID + " IN (SELECT " + Reminders.EVENT_ID + " FROM " +
            Tables.REMINDERS + " LEFT OUTER JOIN " + Tables.EVENTS + " ON " +
            Reminders.EVENT_ID + "=" + Tables.EVENTS + "." + Events._ID +
            " WHERE " + Tables.EVENTS + "." + Events._ID + " IS NULL)";

    private static final String SCHEMA_HTTPS = "https://";
    private static final String SCHEMA_HTTP = "http://";

    private final SyncStateContentProviderHelper mSyncState;

    private static CalendarDatabaseHelper sSingleton = null;

    private DatabaseUtils.InsertHelper mCalendarsInserter;
    private DatabaseUtils.InsertHelper mColorsInserter;
    private DatabaseUtils.InsertHelper mEventsInserter;
    private DatabaseUtils.InsertHelper mEventsRawTimesInserter;
    private DatabaseUtils.InsertHelper mInstancesInserter;
    private DatabaseUtils.InsertHelper mAttendeesInserter;
    private DatabaseUtils.InsertHelper mRemindersInserter;
    private DatabaseUtils.InsertHelper mCalendarAlertsInserter;
    private DatabaseUtils.InsertHelper mExtendedPropertiesInserter;

    public long calendarsInsert(ContentValues values) {
        return mCalendarsInserter.insert(values);
    }

    public long colorsInsert(ContentValues values) {
        return mColorsInserter.insert(values);
    }

    public long eventsInsert(ContentValues values) {
        return mEventsInserter.insert(values);
    }

    public long eventsRawTimesInsert(ContentValues values) {
        return mEventsRawTimesInserter.insert(values);
    }

    public long eventsRawTimesReplace(ContentValues values) {
        return mEventsRawTimesInserter.replace(values);
    }

    public long instancesInsert(ContentValues values) {
        return mInstancesInserter.insert(values);
    }

    public long instancesReplace(ContentValues values) {
        return mInstancesInserter.replace(values);
    }

    public long attendeesInsert(ContentValues values) {
        return mAttendeesInserter.insert(values);
    }

    public long remindersInsert(ContentValues values) {
        return mRemindersInserter.insert(values);
    }

    public long calendarAlertsInsert(ContentValues values) {
        return mCalendarAlertsInserter.insert(values);
    }

    public long extendedPropertiesInsert(ContentValues values) {
        return mExtendedPropertiesInserter.insert(values);
    }

    public static synchronized CalendarDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new CalendarDatabaseHelper(context);
        }
        return sSingleton;
    }

    /**
     * Private constructor, callers except unit tests should obtain an instance through
     * {@link #getInstance(android.content.Context)} instead.
     */
    /* package */ CalendarDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        if (LOGD) Log.d(TAG, "Creating OpenHelper");

        mSyncState = new SyncStateContentProviderHelper();
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        mSyncState.onDatabaseOpened(db);

        mCalendarsInserter = new DatabaseUtils.InsertHelper(db, Tables.CALENDARS);
        mColorsInserter = new DatabaseUtils.InsertHelper(db, Tables.COLORS);
        mEventsInserter = new DatabaseUtils.InsertHelper(db, Tables.EVENTS);
        mEventsRawTimesInserter = new DatabaseUtils.InsertHelper(db, Tables.EVENTS_RAW_TIMES);
        mInstancesInserter = new DatabaseUtils.InsertHelper(db, Tables.INSTANCES);
        mAttendeesInserter = new DatabaseUtils.InsertHelper(db, Tables.ATTENDEES);
        mRemindersInserter = new DatabaseUtils.InsertHelper(db, Tables.REMINDERS);
        mCalendarAlertsInserter = new DatabaseUtils.InsertHelper(db, Tables.CALENDAR_ALERTS);
        mExtendedPropertiesInserter =
                new DatabaseUtils.InsertHelper(db, Tables.EXTENDED_PROPERTIES);
    }

    /*
     * Upgrade sync state table if necessary.  Note that the data bundle
     * in the table is not upgraded.
     *
     * The sync state used to be stored with version 3, but now uses the
     * same sync state code as contacts, which is version 1.  This code
     * upgrades from 3 to 1 if necessary.  (Yes, the numbers are unfortunately
     * backwards.)
     *
     * This code is only called when upgrading from an old calendar version,
     * so there is no problem if sync state version 3 gets used again in the
     * future.
     */
    private void upgradeSyncState(SQLiteDatabase db) {
        long version = DatabaseUtils.longForQuery(db,
                 "SELECT " + SYNC_STATE_META_VERSION_COLUMN
                 + " FROM " + Tables.SYNC_STATE_META,
                 null);
        if (version == PRE_FROYO_SYNC_STATE_VERSION) {
            Log.i(TAG, "Upgrading calendar sync state table");
            db.execSQL("CREATE TEMPORARY TABLE state_backup(_sync_account TEXT, "
                    + "_sync_account_type TEXT, data TEXT);");
            db.execSQL("INSERT INTO state_backup SELECT _sync_account, _sync_account_type, data"
                    + " FROM "
                    + Tables.SYNC_STATE
                    + " WHERE _sync_account is not NULL and _sync_account_type is not NULL;");
            db.execSQL("DROP TABLE " + Tables.SYNC_STATE + ";");
            mSyncState.onDatabaseOpened(db);
            db.execSQL("INSERT INTO " + Tables.SYNC_STATE + "("
                    + SyncStateContract.Columns.ACCOUNT_NAME + ","
                    + SyncStateContract.Columns.ACCOUNT_TYPE + ","
                    + SyncStateContract.Columns.DATA
                    + ") SELECT _sync_account, _sync_account_type, data from state_backup;");
            db.execSQL("DROP TABLE state_backup;");
        } else {
            // Wrong version to upgrade.
            // Don't need to do anything more here because mSyncState.onDatabaseOpened() will blow
            // away and recreate  the database (which will result in a resync).
            Log.w(TAG, "upgradeSyncState: current version is " + version + ", skipping upgrade.");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        bootstrapDB(db);
    }

    private void bootstrapDB(SQLiteDatabase db) {
        Log.i(TAG, "Bootstrapping database");

        mSyncState.createDatabase(db);

        createColorsTable(db);

        createCalendarsTable(db);

        createEventsTable(db);

        db.execSQL("CREATE TABLE " + Tables.EVENTS_RAW_TIMES + " (" +
                CalendarContract.EventsRawTimes._ID + " INTEGER PRIMARY KEY," +
                CalendarContract.EventsRawTimes.EVENT_ID + " INTEGER NOT NULL," +
                CalendarContract.EventsRawTimes.DTSTART_2445 + " TEXT," +
                CalendarContract.EventsRawTimes.DTEND_2445 + " TEXT," +
                CalendarContract.EventsRawTimes.ORIGINAL_INSTANCE_TIME_2445 + " TEXT," +
                CalendarContract.EventsRawTimes.LAST_DATE_2445 + " TEXT," +
                "UNIQUE (" + CalendarContract.EventsRawTimes.EVENT_ID + ")" +
                ");");

        db.execSQL("CREATE TABLE " + Tables.INSTANCES + " (" +
                CalendarContract.Instances._ID + " INTEGER PRIMARY KEY," +
                CalendarContract.Instances.EVENT_ID + " INTEGER," +
                CalendarContract.Instances.BEGIN + " INTEGER," +         // UTC millis
                CalendarContract.Instances.END + " INTEGER," +           // UTC millis
                CalendarContract.Instances.START_DAY + " INTEGER," +      // Julian start day
                CalendarContract.Instances.END_DAY + " INTEGER," +        // Julian end day
                CalendarContract.Instances.START_MINUTE + " INTEGER," +   // minutes from midnight
                CalendarContract.Instances.END_MINUTE + " INTEGER," +     // minutes from midnight
                "UNIQUE (" +
                    CalendarContract.Instances.EVENT_ID + ", " +
                    CalendarContract.Instances.BEGIN + ", " +
                    CalendarContract.Instances.END + ")" +
                ");");

        db.execSQL("CREATE INDEX instancesStartDayIndex ON " + Tables.INSTANCES + " (" +
                CalendarContract.Instances.START_DAY +
                ");");

        createCalendarMetaDataTable(db);

        createCalendarCacheTable(db, null);

        db.execSQL("CREATE TABLE " + Tables.ATTENDEES + " (" +
                CalendarContract.Attendees._ID + " INTEGER PRIMARY KEY," +
                CalendarContract.Attendees.EVENT_ID + " INTEGER," +
                CalendarContract.Attendees.ATTENDEE_NAME + " TEXT," +
                CalendarContract.Attendees.ATTENDEE_EMAIL + " TEXT," +
                CalendarContract.Attendees.ATTENDEE_STATUS + " INTEGER," +
                CalendarContract.Attendees.ATTENDEE_RELATIONSHIP + " INTEGER," +
                CalendarContract.Attendees.ATTENDEE_TYPE + " INTEGER," +
                CalendarContract.Attendees.ATTENDEE_IDENTITY + " TEXT," +
                CalendarContract.Attendees.ATTENDEE_ID_NAMESPACE + " TEXT" +
                ");");

        db.execSQL("CREATE INDEX attendeesEventIdIndex ON " + Tables.ATTENDEES + " (" +
                CalendarContract.Attendees.EVENT_ID +
                ");");

        db.execSQL("CREATE TABLE " + Tables.REMINDERS + " (" +
                CalendarContract.Reminders._ID + " INTEGER PRIMARY KEY," +
                CalendarContract.Reminders.EVENT_ID + " INTEGER," +
                CalendarContract.Reminders.MINUTES + " INTEGER," +
                CalendarContract.Reminders.METHOD + " INTEGER NOT NULL" +
                " DEFAULT " + CalendarContract.Reminders.METHOD_DEFAULT +
                ");");

        db.execSQL("CREATE INDEX remindersEventIdIndex ON " + Tables.REMINDERS + " (" +
                CalendarContract.Reminders.EVENT_ID +
                ");");

         // This table stores the Calendar notifications that have gone off.
        db.execSQL("CREATE TABLE " + Tables.CALENDAR_ALERTS + " (" +
                CalendarContract.CalendarAlerts._ID + " INTEGER PRIMARY KEY," +
                CalendarContract.CalendarAlerts.EVENT_ID + " INTEGER," +
                CalendarContract.CalendarAlerts.BEGIN + " INTEGER NOT NULL," +      // UTC millis
                CalendarContract.CalendarAlerts.END + " INTEGER NOT NULL," +        // UTC millis
                CalendarContract.CalendarAlerts.ALARM_TIME + " INTEGER NOT NULL," + // UTC millis
                // UTC millis
                CalendarContract.CalendarAlerts.CREATION_TIME + " INTEGER NOT NULL DEFAULT 0," +
                // UTC millis
                CalendarContract.CalendarAlerts.RECEIVED_TIME + " INTEGER NOT NULL DEFAULT 0," +
                // UTC millis
                CalendarContract.CalendarAlerts.NOTIFY_TIME + " INTEGER NOT NULL DEFAULT 0," +
                CalendarContract.CalendarAlerts.STATE + " INTEGER NOT NULL," +
                CalendarContract.CalendarAlerts.MINUTES + " INTEGER," +
                "UNIQUE (" +
                    CalendarContract.CalendarAlerts.ALARM_TIME + ", " +
                    CalendarContract.CalendarAlerts.BEGIN + ", " +
                    CalendarContract.CalendarAlerts.EVENT_ID + ")" +
                ");");

        db.execSQL("CREATE INDEX calendarAlertsEventIdIndex ON " + Tables.CALENDAR_ALERTS + " (" +
                CalendarContract.CalendarAlerts.EVENT_ID +
                ");");

        db.execSQL("CREATE TABLE " + Tables.EXTENDED_PROPERTIES + " (" +
                CalendarContract.ExtendedProperties._ID + " INTEGER PRIMARY KEY," +
                CalendarContract.ExtendedProperties.EVENT_ID + " INTEGER," +
                CalendarContract.ExtendedProperties.NAME + " TEXT," +
                CalendarContract.ExtendedProperties.VALUE + " TEXT" +
                ");");

        db.execSQL("CREATE INDEX extendedPropertiesEventIdIndex ON " + Tables.EXTENDED_PROPERTIES
                + " (" +
                CalendarContract.ExtendedProperties.EVENT_ID +
                ");");

        createEventsView(db);

        // Trigger to remove data tied to an event when we delete that event.
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON " + Tables.EVENTS + " " +
                "BEGIN " +
                EVENTS_CLEANUP_TRIGGER_SQL +
                "END");

        // Triggers to update the color stored in an event or a calendar when
        // the color_index is changed.
        createColorsTriggers(db);

        // Trigger to update exceptions when an original event updates its
        // _sync_id
        db.execSQL(CREATE_SYNC_ID_UPDATE_TRIGGER);

        scheduleSync(null /* all accounts */, false, null);
    }

    private void createEventsTable(SQLiteDatabase db) {
        // IMPORTANT: when adding new columns, be sure to update ALLOWED_IN_EXCEPTION and
        // DONT_CLONE_INTO_EXCEPTION in CalendarProvider2.
        //
        // TODO: do we need both dtend and duration?
        // **When updating this be sure to also update LAST_SYNCED_EVENT_COLUMNS
        db.execSQL("CREATE TABLE " + Tables.EVENTS + " (" +
                CalendarContract.Events._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                CalendarContract.Events._SYNC_ID + " TEXT," +
                CalendarContract.Events.DIRTY + " INTEGER," +
                CalendarContract.Events.MUTATORS + " TEXT," +
                CalendarContract.Events.LAST_SYNCED + " INTEGER DEFAULT 0," +
                CalendarContract.Events.CALENDAR_ID + " INTEGER NOT NULL," +
                CalendarContract.Events.TITLE + " TEXT," +
                CalendarContract.Events.EVENT_LOCATION + " TEXT," +
                CalendarContract.Events.DESCRIPTION + " TEXT," +
                CalendarContract.Events.EVENT_COLOR + " INTEGER," +
                CalendarContract.Events.EVENT_COLOR_KEY + " TEXT," +
                CalendarContract.Events.STATUS + " INTEGER," +
                CalendarContract.Events.SELF_ATTENDEE_STATUS + " INTEGER NOT NULL DEFAULT 0," +
                // dtstart in millis since epoch
                CalendarContract.Events.DTSTART + " INTEGER," +
                // dtend in millis since epoch
                CalendarContract.Events.DTEND + " INTEGER," +
                // timezone for event
                CalendarContract.Events.EVENT_TIMEZONE + " TEXT," +
                CalendarContract.Events.DURATION + " TEXT," +
                CalendarContract.Events.ALL_DAY + " INTEGER NOT NULL DEFAULT 0," +
                CalendarContract.Events.ACCESS_LEVEL + " INTEGER NOT NULL DEFAULT 0," +
                CalendarContract.Events.AVAILABILITY + " INTEGER NOT NULL DEFAULT 0," +
                CalendarContract.Events.HAS_ALARM + " INTEGER NOT NULL DEFAULT 0," +
                CalendarContract.Events.HAS_EXTENDED_PROPERTIES + " INTEGER NOT NULL DEFAULT 0," +
                CalendarContract.Events.RRULE + " TEXT," +
                CalendarContract.Events.RDATE + " TEXT," +
                CalendarContract.Events.EXRULE + " TEXT," +
                CalendarContract.Events.EXDATE + " TEXT," +
                CalendarContract.Events.ORIGINAL_ID + " INTEGER," +
                // ORIGINAL_SYNC_ID is the _sync_id of recurring event
                CalendarContract.Events.ORIGINAL_SYNC_ID + " TEXT," +
                // originalInstanceTime is in millis since epoch
                CalendarContract.Events.ORIGINAL_INSTANCE_TIME + " INTEGER," +
                CalendarContract.Events.ORIGINAL_ALL_DAY + " INTEGER," +
                // lastDate is in millis since epoch
                CalendarContract.Events.LAST_DATE + " INTEGER," +
                CalendarContract.Events.HAS_ATTENDEE_DATA + " INTEGER NOT NULL DEFAULT 0," +
                CalendarContract.Events.GUESTS_CAN_MODIFY + " INTEGER NOT NULL DEFAULT 0," +
                CalendarContract.Events.GUESTS_CAN_INVITE_OTHERS + " INTEGER NOT NULL DEFAULT 1," +
                CalendarContract.Events.GUESTS_CAN_SEE_GUESTS + " INTEGER NOT NULL DEFAULT 1," +
                CalendarContract.Events.ORGANIZER + " STRING," +
                CalendarContract.Events.IS_ORGANIZER + " INTEGER," +
                CalendarContract.Events.DELETED + " INTEGER NOT NULL DEFAULT 0," +
                // timezone for event with allDay events are in local timezone
                CalendarContract.Events.EVENT_END_TIMEZONE + " TEXT," +
                CalendarContract.Events.CUSTOM_APP_PACKAGE + " TEXT," +
                CalendarContract.Events.CUSTOM_APP_URI + " TEXT," +
                CalendarContract.Events.UID_2445 + " TEXT," +
                // SYNC_DATAX columns are available for use by sync adapters
                CalendarContract.Events.SYNC_DATA1 + " TEXT," +
                CalendarContract.Events.SYNC_DATA2 + " TEXT," +
                CalendarContract.Events.SYNC_DATA3 + " TEXT," +
                CalendarContract.Events.SYNC_DATA4 + " TEXT," +
                CalendarContract.Events.SYNC_DATA5 + " TEXT," +
                CalendarContract.Events.SYNC_DATA6 + " TEXT," +
                CalendarContract.Events.SYNC_DATA7 + " TEXT," +
                CalendarContract.Events.SYNC_DATA8 + " TEXT," +
                CalendarContract.Events.SYNC_DATA9 + " TEXT," +
                CalendarContract.Events.SYNC_DATA10 + " TEXT" + ");");

        // **When updating this be sure to also update LAST_SYNCED_EVENT_COLUMNS

        db.execSQL("CREATE INDEX eventsCalendarIdIndex ON " + Tables.EVENTS + " ("
                + CalendarContract.Events.CALENDAR_ID + ");");
    }

    private void createEventsTable307(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Events ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "_sync_id TEXT,"
                + "dirty INTEGER,"
                + "lastSynced INTEGER DEFAULT 0,"
                + "calendar_id INTEGER NOT NULL,"
                + "title TEXT,"
                + "eventLocation TEXT,"
                + "description TEXT,"
                + "eventColor INTEGER,"
                + "eventStatus INTEGER,"
                + "selfAttendeeStatus INTEGER NOT NULL DEFAULT 0,"
                // dtstart in millis since epoch
                + "dtstart INTEGER,"
                // dtend in millis since epoch
                + "dtend INTEGER,"
                // timezone for event
                + "eventTimezone TEXT,"
                + "duration TEXT,"
                + "allDay INTEGER NOT NULL DEFAULT 0,"
                + "accessLevel INTEGER NOT NULL DEFAULT 0,"
                + "availability INTEGER NOT NULL DEFAULT 0,"
                + "hasAlarm INTEGER NOT NULL DEFAULT 0,"
                + "hasExtendedProperties INTEGER NOT NULL DEFAULT 0,"
                + "rrule TEXT,"
                + "rdate TEXT,"
                + "exrule TEXT,"
                + "exdate TEXT,"
                + "original_id INTEGER,"
                // ORIGINAL_SYNC_ID is the _sync_id of recurring event
                + "original_sync_id TEXT,"
                // originalInstanceTime is in millis since epoch
                + "originalInstanceTime INTEGER,"
                + "originalAllDay INTEGER,"
                // lastDate is in millis since epoch
                + "lastDate INTEGER,"
                + "hasAttendeeData INTEGER NOT NULL DEFAULT 0,"
                + "guestsCanModify INTEGER NOT NULL DEFAULT 0,"
                + "guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1,"
                + "guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1,"
                + "organizer STRING,"
                + "deleted INTEGER NOT NULL DEFAULT 0,"
                // timezone for event with allDay events are in local timezone
                + "eventEndTimezone TEXT,"
                // SYNC_DATAX columns are available for use by sync adapters
                + "sync_data1 TEXT,"
                + "sync_data2 TEXT,"
                + "sync_data3 TEXT,"
                + "sync_data4 TEXT,"
                + "sync_data5 TEXT,"
                + "sync_data6 TEXT,"
                + "sync_data7 TEXT,"
                + "sync_data8 TEXT,"
                + "sync_data9 TEXT,"
                + "sync_data10 TEXT);");

        // **When updating this be sure to also update LAST_SYNCED_EVENT_COLUMNS

        db.execSQL("CREATE INDEX eventsCalendarIdIndex ON Events (calendar_id);");
    }

    // TODO Remove this method after merging all ICS upgrades
    private void createEventsTable300(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Events (" +
                "_id INTEGER PRIMARY KEY," +
                "_sync_id TEXT," +
                "_sync_version TEXT," +
                // sync time in UTC
                "_sync_time TEXT,"  +
                "_sync_local_id INTEGER," +
                "dirty INTEGER," +
                // sync mark to filter out new rows
                "_sync_mark INTEGER," +
                "calendar_id INTEGER NOT NULL," +
                "htmlUri TEXT," +
                "title TEXT," +
                "eventLocation TEXT," +
                "description TEXT," +
                "eventStatus INTEGER," +
                "selfAttendeeStatus INTEGER NOT NULL DEFAULT 0," +
                "commentsUri TEXT," +
                // dtstart in millis since epoch
                "dtstart INTEGER," +
                // dtend in millis since epoch
                "dtend INTEGER," +
                // timezone for event
                "eventTimezone TEXT," +
                "duration TEXT," +
                "allDay INTEGER NOT NULL DEFAULT 0," +
                "accessLevel INTEGER NOT NULL DEFAULT 0," +
                "availability INTEGER NOT NULL DEFAULT 0," +
                "hasAlarm INTEGER NOT NULL DEFAULT 0," +
                "hasExtendedProperties INTEGER NOT NULL DEFAULT 0," +
                "rrule TEXT," +
                "rdate TEXT," +
                "exrule TEXT," +
                "exdate TEXT," +
                // originalEvent is the _sync_id of recurring event
                "original_sync_id TEXT," +
                // originalInstanceTime is in millis since epoch
                "originalInstanceTime INTEGER," +
                "originalAllDay INTEGER," +
                // lastDate is in millis since epoch
                "lastDate INTEGER," +
                "hasAttendeeData INTEGER NOT NULL DEFAULT 0," +
                "guestsCanModify INTEGER NOT NULL DEFAULT 0," +
                "guestsCanInviteOthers INTEGER NOT NULL DEFAULT 1," +
                "guestsCanSeeGuests INTEGER NOT NULL DEFAULT 1," +
                "organizer STRING," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                // timezone for event with allDay events are in local timezone
                "eventEndTimezone TEXT," +
                // syncAdapterData is available for use by sync adapters
                "sync_data1 TEXT);");

        db.execSQL("CREATE INDEX eventsCalendarIdIndex ON Events (calendar_id);");
    }

    private void createCalendarsTable303(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.CALENDARS + " (" +
                "_id INTEGER PRIMARY KEY," +
                "account_name TEXT," +
                "account_type TEXT," +
                "_sync_id TEXT," +
                "_sync_version TEXT," +
                "_sync_time TEXT," +  // UTC
                "dirty INTEGER," +
                "name TEXT," +
                "displayName TEXT," +
                "calendar_color INTEGER," +
                "access_level INTEGER," +
                "visible INTEGER NOT NULL DEFAULT 1," +
                "sync_events INTEGER NOT NULL DEFAULT 0," +
                "calendar_location TEXT," +
                "calendar_timezone TEXT," +
                "ownerAccount TEXT, " +
                "canOrganizerRespond INTEGER NOT NULL DEFAULT 1," +
                "canModifyTimeZone INTEGER DEFAULT 1," +
                "maxReminders INTEGER DEFAULT 5," +
                "allowedReminders TEXT DEFAULT '0,1'," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "cal_sync1 TEXT," +
                "cal_sync2 TEXT," +
                "cal_sync3 TEXT," +
                "cal_sync4 TEXT," +
                "cal_sync5 TEXT," +
                "cal_sync6 TEXT" +
                ");");

        // Trigger to remove a calendar's events when we delete the calendar
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON " + Tables.CALENDARS + " " +
                "BEGIN " +
                CALENDAR_CLEANUP_TRIGGER_SQL +
                "END");
    }

    private void createColorsTable(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + Tables.COLORS + " (" +
                CalendarContract.Colors._ID + " INTEGER PRIMARY KEY," +
                CalendarContract.Colors.ACCOUNT_NAME + " TEXT NOT NULL," +
                CalendarContract.Colors.ACCOUNT_TYPE + " TEXT NOT NULL," +
                CalendarContract.Colors.DATA + " TEXT," +
                CalendarContract.Colors.COLOR_TYPE + " INTEGER NOT NULL," +
                CalendarContract.Colors.COLOR_KEY + " TEXT NOT NULL," +
                CalendarContract.Colors.COLOR + " INTEGER NOT NULL" +
                ");");
    }

    public void createColorsTriggers(SQLiteDatabase db) {
        db.execSQL(CREATE_EVENT_COLOR_UPDATE_TRIGGER);
        db.execSQL(CREATE_CALENDAR_COLOR_UPDATE_TRIGGER);
    }

    private void createCalendarsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.CALENDARS + " (" +
                Calendars._ID + " INTEGER PRIMARY KEY," +
                Calendars.ACCOUNT_NAME + " TEXT," +
                Calendars.ACCOUNT_TYPE + " TEXT," +
                Calendars._SYNC_ID + " TEXT," +
                Calendars.DIRTY + " INTEGER," +
                Calendars.MUTATORS + " TEXT," +
                Calendars.NAME + " TEXT," +
                Calendars.CALENDAR_DISPLAY_NAME + " TEXT," +
                Calendars.CALENDAR_COLOR + " INTEGER," +
                Calendars.CALENDAR_COLOR_KEY + " TEXT," +
                Calendars.CALENDAR_ACCESS_LEVEL + " INTEGER," +
                Calendars.VISIBLE + " INTEGER NOT NULL DEFAULT 1," +
                Calendars.SYNC_EVENTS + " INTEGER NOT NULL DEFAULT 0," +
                Calendars.CALENDAR_LOCATION + " TEXT," +
                Calendars.CALENDAR_TIME_ZONE + " TEXT," +
                Calendars.OWNER_ACCOUNT + " TEXT, " +
                Calendars.IS_PRIMARY + " INTEGER, " +
                Calendars.CAN_ORGANIZER_RESPOND + " INTEGER NOT NULL DEFAULT 1," +
                Calendars.CAN_MODIFY_TIME_ZONE + " INTEGER DEFAULT 1," +
                Calendars.CAN_PARTIALLY_UPDATE + " INTEGER DEFAULT 0," +
                Calendars.MAX_REMINDERS + " INTEGER DEFAULT 5," +
                Calendars.ALLOWED_REMINDERS + " TEXT DEFAULT '0,1'," +
                Calendars.ALLOWED_AVAILABILITY + " TEXT DEFAULT '0,1'," +
                Calendars.ALLOWED_ATTENDEE_TYPES + " TEXT DEFAULT '0,1,2'," +
                Calendars.DELETED + " INTEGER NOT NULL DEFAULT 0," +
                Calendars.CAL_SYNC1 + " TEXT," +
                Calendars.CAL_SYNC2 + " TEXT," +
                Calendars.CAL_SYNC3 + " TEXT," +
                Calendars.CAL_SYNC4 + " TEXT," +
                Calendars.CAL_SYNC5 + " TEXT," +
                Calendars.CAL_SYNC6 + " TEXT," +
                Calendars.CAL_SYNC7 + " TEXT," +
                Calendars.CAL_SYNC8 + " TEXT," +
                Calendars.CAL_SYNC9 + " TEXT," +
                Calendars.CAL_SYNC10 + " TEXT" +
                ");");

        // Trigger to remove a calendar's events when we delete the calendar
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON " + Tables.CALENDARS + " " +
                "BEGIN " +
                CALENDAR_CLEANUP_TRIGGER_SQL +
                "END");
    }

    private void createCalendarsTable305(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (" +
                "_id INTEGER PRIMARY KEY," +
                "account_name TEXT," +
                "account_type TEXT," +
                "_sync_id TEXT," +
                "dirty INTEGER," +
                "name TEXT," +
                "calendar_displayName TEXT," +
                "calendar_color INTEGER," +
                "calendar_access_level INTEGER," +
                "visible INTEGER NOT NULL DEFAULT 1," +
                "sync_events INTEGER NOT NULL DEFAULT 0," +
                "calendar_location TEXT," +
                "calendar_timezone TEXT," +
                "ownerAccount TEXT, " +
                "canOrganizerRespond INTEGER NOT NULL DEFAULT 1," +
                "canModifyTimeZone INTEGER DEFAULT 1," +
                "canPartiallyUpdate INTEGER DEFAULT 0," +
                "maxReminders INTEGER DEFAULT 5," +
                "allowedReminders TEXT DEFAULT '0,1'," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "cal_sync1 TEXT," +
                "cal_sync2 TEXT," +
                "cal_sync3 TEXT," +
                "cal_sync4 TEXT," +
                "cal_sync5 TEXT," +
                "cal_sync6 TEXT," +
                "cal_sync7 TEXT," +
                "cal_sync8 TEXT," +
                "cal_sync9 TEXT," +
                "cal_sync10 TEXT" +
                ");");

        // Trigger to remove a calendar's events when we delete the calendar
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars " +
                "BEGIN " +
                "DELETE FROM Events WHERE calendar_id=old._id;" +
                "END");
    }

    private void createCalendarsTable300(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.CALENDARS + " (" +
                "_id INTEGER PRIMARY KEY," +
                "account_name TEXT," +
                "account_type TEXT," +
                "_sync_id TEXT," +
                "_sync_version TEXT," +
                "_sync_time TEXT," +  // UTC
                "dirty INTEGER," +
                "name TEXT," +
                "displayName TEXT," +
                "calendar_color INTEGER," +
                "access_level INTEGER," +
                "visible INTEGER NOT NULL DEFAULT 1," +
                "sync_events INTEGER NOT NULL DEFAULT 0," +
                "calendar_location TEXT," +
                "calendar_timezone TEXT," +
                "ownerAccount TEXT, " +
                "canOrganizerRespond INTEGER NOT NULL DEFAULT 1," +
                "canModifyTimeZone INTEGER DEFAULT 1," +
                "maxReminders INTEGER DEFAULT 5," +
                "allowedReminders TEXT DEFAULT '0,1,2'," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "sync1 TEXT," +
                "sync2 TEXT," +
                "sync3 TEXT," +
                "sync4 TEXT," +
                "sync5 TEXT," +
                "sync6 TEXT" +
                ");");

        // Trigger to remove a calendar's events when we delete the calendar
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON " + Tables.CALENDARS + " " +
                "BEGIN " +
                CALENDAR_CLEANUP_TRIGGER_SQL +
                "END");
    }

    private void createCalendarsTable205(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (" +
                "_id INTEGER PRIMARY KEY," +
                "_sync_account TEXT," +
                "_sync_account_type TEXT," +
                "_sync_id TEXT," +
                "_sync_version TEXT," +
                "_sync_time TEXT," +  // UTC
                "_sync_dirty INTEGER," +
                "name TEXT," +
                "displayName TEXT," +
                "color INTEGER," +
                "access_level INTEGER," +
                "visible INTEGER NOT NULL DEFAULT 1," +
                "sync_events INTEGER NOT NULL DEFAULT 0," +
                "location TEXT," +
                "timezone TEXT," +
                "ownerAccount TEXT, " +
                "canOrganizerRespond INTEGER NOT NULL DEFAULT 1," +
                "canModifyTimeZone INTEGER DEFAULT 1, " +
                "maxReminders INTEGER DEFAULT 5," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "sync1 TEXT," +
                "sync2 TEXT," +
                "sync3 TEXT," +
                "sync4 TEXT," +
                "sync5 TEXT," +
                "sync6 TEXT" +
                ");");

        createCalendarsCleanup200(db);
    }

    private void createCalendarsTable202(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (" +
                "_id INTEGER PRIMARY KEY," +
                "_sync_account TEXT," +
                "_sync_account_type TEXT," +
                "_sync_id TEXT," +
                "_sync_version TEXT," +
                "_sync_time TEXT," +  // UTC
                "_sync_local_id INTEGER," +
                "_sync_dirty INTEGER," +
                "_sync_mark INTEGER," + // Used to filter out new rows
                "name TEXT," +
                "displayName TEXT," +
                "color INTEGER," +
                "access_level INTEGER," +
                "selected INTEGER NOT NULL DEFAULT 1," +
                "sync_events INTEGER NOT NULL DEFAULT 0," +
                "location TEXT," +
                "timezone TEXT," +
                "ownerAccount TEXT, " +
                "organizerCanRespond INTEGER NOT NULL DEFAULT 1," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "sync1 TEXT," +
                "sync2 TEXT," +
                "sync3 TEXT," +
                "sync4 TEXT," +
                "sync5 TEXT" +
                ");");

        createCalendarsCleanup200(db);
    }

    private void createCalendarsTable200(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Calendars (" +
                "_id INTEGER PRIMARY KEY," +
                "_sync_account TEXT," +
                "_sync_account_type TEXT," +
                "_sync_id TEXT," +
                "_sync_version TEXT," +
                "_sync_time TEXT," +  // UTC
                "_sync_local_id INTEGER," +
                "_sync_dirty INTEGER," +
                "_sync_mark INTEGER," + // Used to filter out new rows
                "name TEXT," +
                "displayName TEXT," +
                "hidden INTEGER NOT NULL DEFAULT 0," +
                "color INTEGER," +
                "access_level INTEGER," +
                "selected INTEGER NOT NULL DEFAULT 1," +
                "sync_events INTEGER NOT NULL DEFAULT 0," +
                "location TEXT," +
                "timezone TEXT," +
                "ownerAccount TEXT, " +
                "organizerCanRespond INTEGER NOT NULL DEFAULT 1," +
                "deleted INTEGER NOT NULL DEFAULT 0," +
                "sync1 TEXT," +
                "sync2 TEXT," +
                "sync3 TEXT" +
                ");");

        createCalendarsCleanup200(db);
    }

    /** Trigger to remove a calendar's events when we delete the calendar */
    private void createCalendarsCleanup200(SQLiteDatabase db) {
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars " +
                "BEGIN " +
                "DELETE FROM Events WHERE calendar_id=old._id;" +
                "END");
    }

    private void createCalendarMetaDataTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.CALENDAR_META_DATA + " (" +
                CalendarContract.CalendarMetaData._ID + " INTEGER PRIMARY KEY," +
                CalendarContract.CalendarMetaData.LOCAL_TIMEZONE + " TEXT," +
                CalendarContract.CalendarMetaData.MIN_INSTANCE + " INTEGER," +      // UTC millis
                CalendarContract.CalendarMetaData.MAX_INSTANCE + " INTEGER" +       // UTC millis
                ");");
    }

    private void createCalendarMetaDataTable59(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE CalendarMetaData (" +
                "_id INTEGER PRIMARY KEY," +
                "localTimezone TEXT," +
                "minInstance INTEGER," +      // UTC millis
                "maxInstance INTEGER" +       // UTC millis
                ");");
    }

    private void createCalendarCacheTable(SQLiteDatabase db, String oldTimezoneDbVersion) {
        // This is a hack because versioning skipped version number 61 of schema
        // TODO after version 70 this can be removed
        db.execSQL("DROP TABLE IF EXISTS " + Tables.CALENDAR_CACHE + ";");

        // IF NOT EXISTS should be normal pattern for table creation
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.CALENDAR_CACHE + " (" +
                CalendarCache.COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                CalendarCache.COLUMN_NAME_KEY + " TEXT NOT NULL," +
                CalendarCache.COLUMN_NAME_VALUE + " TEXT" +
                ");");

        initCalendarCacheTable(db, oldTimezoneDbVersion);
        updateCalendarCacheTable(db);
    }

    private void initCalendarCacheTable(SQLiteDatabase db, String oldTimezoneDbVersion) {
        String timezoneDbVersion = (oldTimezoneDbVersion != null) ?
                oldTimezoneDbVersion : CalendarCache.DEFAULT_TIMEZONE_DATABASE_VERSION;

        // Set the default timezone database version
        db.execSQL("INSERT OR REPLACE INTO " + Tables.CALENDAR_CACHE +
                " (" + CalendarCache.COLUMN_NAME_ID + ", " +
                CalendarCache.COLUMN_NAME_KEY + ", " +
                CalendarCache.COLUMN_NAME_VALUE + ") VALUES (" +
                CalendarCache.KEY_TIMEZONE_DATABASE_VERSION.hashCode() + "," +
                "'" + CalendarCache.KEY_TIMEZONE_DATABASE_VERSION + "'," +
                "'" + timezoneDbVersion + "'" +
                ");");
    }

    private void updateCalendarCacheTable(SQLiteDatabase db) {
        // Define the default timezone type for Instances timezone management
        db.execSQL("INSERT INTO " + Tables.CALENDAR_CACHE +
                " (" + CalendarCache.COLUMN_NAME_ID + ", " +
                CalendarCache.COLUMN_NAME_KEY + ", " +
                CalendarCache.COLUMN_NAME_VALUE + ") VALUES (" +
                CalendarCache.KEY_TIMEZONE_TYPE.hashCode() + "," +
                "'" + CalendarCache.KEY_TIMEZONE_TYPE + "',"  +
                "'" + CalendarCache.TIMEZONE_TYPE_AUTO + "'" +
                ");");

        String defaultTimezone = TimeZone.getDefault().getID();

        // Define the default timezone for Instances
        db.execSQL("INSERT INTO " + Tables.CALENDAR_CACHE +
                " (" + CalendarCache.COLUMN_NAME_ID + ", " +
                CalendarCache.COLUMN_NAME_KEY + ", " +
                CalendarCache.COLUMN_NAME_VALUE + ") VALUES (" +
                CalendarCache.KEY_TIMEZONE_INSTANCES.hashCode() + "," +
                "'" + CalendarCache.KEY_TIMEZONE_INSTANCES + "',"  +
                "'" + defaultTimezone + "'" +
                ");");

        // Define the default previous timezone for Instances
        db.execSQL("INSERT INTO " + Tables.CALENDAR_CACHE +
                " (" + CalendarCache.COLUMN_NAME_ID + ", " +
                CalendarCache.COLUMN_NAME_KEY + ", " +
                CalendarCache.COLUMN_NAME_VALUE + ") VALUES (" +
                CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS.hashCode() + "," +
                "'" + CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS + "',"  +
                "'" + defaultTimezone + "'" +
                ");");
    }

    private void initCalendarCacheTable203(SQLiteDatabase db, String oldTimezoneDbVersion) {
        String timezoneDbVersion = (oldTimezoneDbVersion != null) ?
                oldTimezoneDbVersion : "2009s";

        // Set the default timezone database version
        db.execSQL("INSERT OR REPLACE INTO CalendarCache" +
                " (_id, " +
                "key, " +
                "value) VALUES (" +
                "timezoneDatabaseVersion".hashCode() + "," +
                "'timezoneDatabaseVersion',"  +
                "'" + timezoneDbVersion + "'" +
                ");");
    }

    private void updateCalendarCacheTableTo203(SQLiteDatabase db) {
        // Define the default timezone type for Instances timezone management
        db.execSQL("INSERT INTO CalendarCache" +
                " (_id, key, value) VALUES (" +
                "timezoneType".hashCode() + "," +
                "'timezoneType',"  +
                "'auto'" +
                ");");

        String defaultTimezone = TimeZone.getDefault().getID();

        // Define the default timezone for Instances
        db.execSQL("INSERT INTO CalendarCache" +
                " (_id, key, value) VALUES (" +
                "timezoneInstances".hashCode() + "," +
                "'timezoneInstances',"  +
                "'" + defaultTimezone + "'" +
                ");");

        // Define the default previous timezone for Instances
        db.execSQL("INSERT INTO CalendarCache" +
                " (_id, key, value) VALUES (" +
                "timezoneInstancesPrevious".hashCode() + "," +
                "'timezoneInstancesPrevious',"  +
                "'" + defaultTimezone + "'" +
                ");");
    }

    /**
     * Removes orphaned data from the database.  Specifically:
     * <ul>
     * <li>Attendees with an event_id for a nonexistent Event
     * <li>Reminders with an event_id for a nonexistent Event
     * </ul>
     */
    static void removeOrphans(SQLiteDatabase db) {
        if (false) {        // debug mode
            String SELECT_ATTENDEES_ORPHANS = "SELECT " +
                    Attendees._ID + ", " + Attendees.EVENT_ID + " FROM " + Tables.ATTENDEES +
                    " WHERE " + WHERE_ATTENDEES_ORPHANS;

            Cursor cursor = null;
            try {
                Log.i(TAG, "Attendees orphans:");
                cursor = db.rawQuery(SELECT_ATTENDEES_ORPHANS, null);
                DatabaseUtils.dumpCursor(cursor);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            String SELECT_REMINDERS_ORPHANS = "SELECT " +
                    Attendees._ID + ", " + Reminders.EVENT_ID + " FROM " + Tables.REMINDERS +
                    " WHERE " + WHERE_REMINDERS_ORPHANS;
            cursor = null;
            try {
                Log.i(TAG, "Reminders orphans:");
                cursor = db.rawQuery(SELECT_REMINDERS_ORPHANS, null);
                DatabaseUtils.dumpCursor(cursor);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return;
        }

        Log.d(TAG, "Checking for orphaned entries");
        int count;

        count = db.delete(Tables.ATTENDEES, WHERE_ATTENDEES_ORPHANS, null);
        if (count != 0) {
            Log.i(TAG, "Deleted " + count + " orphaned Attendees");
        }

        count = db.delete(Tables.REMINDERS, WHERE_REMINDERS_ORPHANS, null);
        if (count != 0) {
            Log.i(TAG, "Deleted " + count + " orphaned Reminders");
        }
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading DB from version " + oldVersion + " to " + newVersion);
        long startWhen = System.nanoTime();

        if (oldVersion < 49) {
            dropTables(db);
            bootstrapDB(db);
            return;
        }

        // From schema versions 59 to version 66, the CalendarMetaData table definition had lost
        // the primary key leading to having the CalendarMetaData with multiple rows instead of
        // only one. The Instance table was then corrupted (during Instance expansion we are using
        // the localTimezone, minInstance and maxInstance from CalendarMetaData table.
        // This boolean helps us tracking the need to recreate the CalendarMetaData table and
        // clear the Instance table (and thus force an Instance expansion).
        boolean recreateMetaDataAndInstances = (oldVersion >= 59 && oldVersion <= 66);
        boolean createEventsView = false;

        try {
            if (oldVersion < 51) {
                upgradeToVersion51(db); // From 50 or 51
                oldVersion = 51;
            }
            if (oldVersion == 51) {
                upgradeToVersion52(db);
                oldVersion += 1;
            }
            if (oldVersion == 52) {
                upgradeToVersion53(db);
                oldVersion += 1;
            }
            if (oldVersion == 53) {
                upgradeToVersion54(db);
                oldVersion += 1;
            }
            if (oldVersion == 54) {
                upgradeToVersion55(db);
                oldVersion += 1;
            }
            if (oldVersion == 55 || oldVersion == 56) {
                // Both require resync, so just schedule it once
                upgradeResync(db);
            }
            if (oldVersion == 55) {
                upgradeToVersion56(db);
                oldVersion += 1;
            }
            if (oldVersion == 56) {
                upgradeToVersion57(db);
                oldVersion += 1;
            }
            if (oldVersion == 57) {
                // Changes are undone upgrading to 60, so don't do anything.
                oldVersion += 1;
            }
            if (oldVersion == 58) {
                upgradeToVersion59(db);
                oldVersion += 1;
            }
            if (oldVersion == 59) {
                upgradeToVersion60(db);
                createEventsView = true;
                oldVersion += 1;
            }
            if (oldVersion == 60) {
                upgradeToVersion61(db);
                oldVersion += 1;
            }
            if (oldVersion == 61) {
                upgradeToVersion62(db);
                oldVersion += 1;
            }
            if (oldVersion == 62) {
                createEventsView = true;
                oldVersion += 1;
            }
            if (oldVersion == 63) {
                upgradeToVersion64(db);
                oldVersion += 1;
            }
            if (oldVersion == 64) {
                createEventsView = true;
                oldVersion += 1;
            }
            if (oldVersion == 65) {
                upgradeToVersion66(db);
                oldVersion += 1;
            }
            if (oldVersion == 66) {
                // Changes are done thru recreateMetaDataAndInstances() method
                oldVersion += 1;
            }
            if (recreateMetaDataAndInstances) {
                recreateMetaDataAndInstances67(db);
            }
            if (oldVersion == 67 || oldVersion == 68) {
                upgradeToVersion69(db);
                oldVersion = 69;
            }
            // 69. 70 are for Froyo/old Gingerbread only and 100s are for Gingerbread only
            // 70 and 71 have been for Honeycomb but no more used
            // 72 and 73 and 74 were for Honeycomb only but are considered as obsolete for enabling
            // room for Froyo version numbers
            if(oldVersion == 69) {
                upgradeToVersion200(db);
                createEventsView = true;
                oldVersion = 200;
            }
            if (oldVersion == 70) {
                upgradeToVersion200(db);
                oldVersion = 200;
            }
            if (oldVersion == 100) {
                // note we skip past v101 and v102
                upgradeToVersion200(db);
                oldVersion = 200;
            }
            boolean need203Update = true;
            if (oldVersion == 101 || oldVersion == 102) {
                // v101 is v100 plus updateCalendarCacheTableTo203().
                // v102 is v101 with Event._id changed to autoincrement.
                // Upgrade to 200 and skip the 203 update.
                upgradeToVersion200(db);
                oldVersion = 200;
                need203Update = false;
            }
            if (oldVersion == 200) {
                upgradeToVersion201(db);
                oldVersion += 1;
            }
            if (oldVersion == 201) {
                upgradeToVersion202(db);
                createEventsView = true;
                oldVersion += 1;
            }
            if (oldVersion == 202) {
                if (need203Update) {
                    upgradeToVersion203(db);
                }
                oldVersion += 1;
            }
            if (oldVersion == 203) {
                createEventsView = true;
                oldVersion += 1;
            }
            if (oldVersion == 206) {
                // v206 exists only in HC (change Event._id to autoincrement).  Otherwise
                // identical to v204, so back it up and let the upgrade path continue.
                oldVersion -= 2;
            }
            if (oldVersion == 204) {
                // This is an ICS update, all following use 300+ versions.
                upgradeToVersion205(db);
                createEventsView = true;
                oldVersion += 1;
            }
            if (oldVersion == 205) {
                // Move ICS updates to 300 range
                upgradeToVersion300(db);
                createEventsView = true;
                oldVersion = 300;
            }
            if (oldVersion == 300) {
                upgradeToVersion301(db);
                createEventsView = true;
                oldVersion++;
            }
            if (oldVersion == 301) {
                upgradeToVersion302(db);
                oldVersion++;
            }
            if (oldVersion == 302) {
                upgradeToVersion303(db);
                oldVersion++;
                createEventsView = true;
            }
            if (oldVersion == 303) {
                upgradeToVersion304(db);
                oldVersion++;
                createEventsView = true;
            }
            if (oldVersion == 304) {
                upgradeToVersion305(db);
                oldVersion++;
                createEventsView = true;
            }
            if (oldVersion == 305) {
                upgradeToVersion306(db);
                // force a sync to update edit url and etag
                scheduleSync(null /* all accounts */, false, null);
                oldVersion++;
            }
            if (oldVersion == 306) {
                upgradeToVersion307(db);
                oldVersion++;
            }
            if (oldVersion == 307) {
                upgradeToVersion308(db);
                oldVersion++;
                createEventsView = true;
            }
            if (oldVersion == 308) {
                upgradeToVersion400(db);
                createEventsView = true;
                oldVersion = 400;
            }
            // 309 was changed to 400 since it is the first change of the J release.
            if (oldVersion == 309 || oldVersion == 400) {
                upgradeToVersion401(db);
                createEventsView = true;
                oldVersion = 401;
            }
            if (oldVersion == 401) {
                upgradeToVersion402(db);
                createEventsView = true;
                oldVersion = 402;
            }
            if (oldVersion == 402) {
                upgradeToVersion403(db);
                createEventsView = true;
                oldVersion = 403;
            }
            if (oldVersion == 403) {
                upgradeToVersion501(db);
                createEventsView = true;
                oldVersion = 501;
            }
            if (oldVersion == 501) {
                upgradeToVersion502(db);
                createEventsView = true; // This is needed if the calendars or events schema changed
                oldVersion = 502;
            }
            if (oldVersion < 600) {
                upgradeToVersion600(db);
                createEventsView = true; // This is needed if the calendars or events schema changed
                oldVersion = 600;
            }

            if (createEventsView) {
                createEventsView(db);
            }
            if (oldVersion != DATABASE_VERSION) {
                Log.e(TAG, "Need to recreate Calendar schema because of "
                        + "unknown Calendar database version: " + oldVersion);
                dropTables(db);
                bootstrapDB(db);
                oldVersion = DATABASE_VERSION;
            } else {
                removeOrphans(db);
            }
        } catch (SQLiteException e) {
            if (mInTestMode) {
                // We do want to crash if we are in test mode.
                throw e;
            }
            Log.e(TAG, "onUpgrade: SQLiteException, recreating db. ", e);
            Log.e(TAG, "(oldVersion was " + oldVersion + ")");
            dropTables(db);
            bootstrapDB(db);
            return; // this was lossy
        }

        long endWhen = System.nanoTime();
        Log.d(TAG, "Calendar upgrade took " + ((endWhen - startWhen) / 1000000) + "ms");

        /**
         * db versions < 100 correspond to Froyo and earlier. Gingerbread bumped
         * the db versioning to 100. Honeycomb bumped it to 200. ICS will begin
         * in 300. At each major release we should jump to the next
         * centiversion.
         */
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Can't downgrade DB from version " + oldVersion + " to " + newVersion);
        dropTables(db);
        bootstrapDB(db);
        return;
    }

    /**
     * If the user_version of the database if between 59 and 66 (those versions has been deployed
     * with no primary key for the CalendarMetaData table)
     */
    private void recreateMetaDataAndInstances67(SQLiteDatabase db) {
        // Recreate the CalendarMetaData table with correct primary key
        db.execSQL("DROP TABLE CalendarMetaData;");
        createCalendarMetaDataTable59(db);

        // Also clean the Instance table as this table may be corrupted
        db.execSQL("DELETE FROM Instances;");
    }

    private static boolean fixAllDayTime(Time time, String timezone, Long timeInMillis) {
        time.set(timeInMillis);
        if(time.hour != 0 || time.minute != 0 || time.second != 0) {
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            return true;
        }
        return false;
    }

    /**********************************************************/
    /* DO NOT USE CONSTANTS FOR UPGRADES, USE STRING LITERALS */
    /**********************************************************/

    /**********************************************************/
    /* 6xx db version is for K release
    /**********************************************************/

    private void upgradeToVersion600(SQLiteDatabase db) {
        /*
         * Changes from version 5xx to 600:
         * - add mutator columns to Events & calendars
         */
        db.execSQL("ALTER TABLE Events ADD COLUMN mutators TEXT;");
        db.execSQL("ALTER TABLE Calendars ADD COLUMN mutators TEXT;");
    }

    /**********************************************************/
    /* 5xx db version is for JB MR1 release
    /**********************************************************/

    private void upgradeToVersion501(SQLiteDatabase db) {
        /*
         * Changes from version 403 to 501:
         * - add isOrganizer column to Events table
         * - add isPrimary column to Calendars table
         */
        db.execSQL("ALTER TABLE Events ADD COLUMN isOrganizer INTEGER;");
        db.execSQL("ALTER TABLE Calendars ADD COLUMN isPrimary INTEGER;");
    }

    private void upgradeToVersion502(SQLiteDatabase db) {
        /*
         * Changes from version 501 to 502:
         * - add UID for events added from the RFC 2445 iCalendar format.
         */
        db.execSQL("ALTER TABLE Events ADD COLUMN uid2445 TEXT;");
    }

    /**********************************************************/
    /* 4xx db version is for J release
    /**********************************************************/

    private void upgradeToVersion403(SQLiteDatabase db) {
        /*
         * Changes from version 402 to 403:
         * - add custom app package name and uri Events table
         */
        db.execSQL("ALTER TABLE Events ADD COLUMN customAppPackage TEXT;");
        db.execSQL("ALTER TABLE Events ADD COLUMN customAppUri TEXT;");
    }

    private void upgradeToVersion402(SQLiteDatabase db) {
        /*
         * Changes from version 401 to 402:
         * - add identity and namespace to Attendees table
         */
        db.execSQL("ALTER TABLE Attendees ADD COLUMN attendeeIdentity TEXT;");
        db.execSQL("ALTER TABLE Attendees ADD COLUMN attendeeIdNamespace TEXT;");
    }

    /*
     * Changes from version 309 to 401:
     * Fix repeating events' exceptions with the wrong original_id
     */
    private void upgradeToVersion401(SQLiteDatabase db) {
        db.execSQL("UPDATE events SET original_id=(SELECT _id FROM events inner_events WHERE " +
                "inner_events._sync_id=events.original_sync_id AND " +
                "inner_events.calendar_id=events.calendar_id) WHERE NOT original_id IS NULL AND " +
                "(SELECT calendar_id FROM events ex_events WHERE " +
                "ex_events._id=events.original_id) <> calendar_id ");
    }

    private void upgradeToVersion400(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS calendar_color_update");
        // CREATE_CALENDAR_COLOR_UPDATE_TRIGGER was inlined
        db.execSQL("CREATE TRIGGER "
                + "calendar_color_update" + " UPDATE OF " + Calendars.CALENDAR_COLOR_KEY
                + " ON " + Tables.CALENDARS + " WHEN new." + Calendars.CALENDAR_COLOR_KEY
                + " NOT NULL BEGIN " + "UPDATE " + Tables.CALENDARS
                + " SET calendar_color=(SELECT " + Colors.COLOR + " FROM " + Tables.COLORS
                + " WHERE " + Colors.ACCOUNT_NAME + "=" + "new." + Calendars.ACCOUNT_NAME + " AND "
                + Colors.ACCOUNT_TYPE + "=" + "new." + Calendars.ACCOUNT_TYPE + " AND "
                + Colors.COLOR_KEY + "=" + "new." + Calendars.CALENDAR_COLOR_KEY + " AND "
                + Colors.COLOR_TYPE + "=" + Colors.TYPE_CALENDAR + ") "
                + " WHERE " + Calendars._ID + "=" + "old." + Calendars._ID
                + ";" + " END");
        db.execSQL("DROP TRIGGER IF EXISTS event_color_update");
        // CREATE_EVENT_COLOR_UPDATE_TRIGGER was inlined
        db.execSQL("CREATE TRIGGER "
                + "event_color_update" + " UPDATE OF " + Events.EVENT_COLOR_KEY + " ON "
                + Tables.EVENTS + " WHEN new." + Events.EVENT_COLOR_KEY + " NOT NULL BEGIN "
                + "UPDATE " + Tables.EVENTS
                + " SET eventColor=(SELECT " + Colors.COLOR + " FROM " + Tables.COLORS + " WHERE "
                + Colors.ACCOUNT_NAME + "=" + "(SELECT " + Calendars.ACCOUNT_NAME + " FROM "
                + Tables.CALENDARS + " WHERE " + Calendars._ID + "=new." + Events.CALENDAR_ID
                + ") AND " + Colors.ACCOUNT_TYPE + "=" + "(SELECT " + Calendars.ACCOUNT_TYPE
                + " FROM " + Tables.CALENDARS + " WHERE " + Calendars._ID + "=new."
                + Events.CALENDAR_ID + ") AND " + Colors.COLOR_KEY + "=" + "new."
                + Events.EVENT_COLOR_KEY + " AND " + Colors.COLOR_TYPE + "="
                + Colors.TYPE_EVENT + ") "
                + " WHERE " + Events._ID + "=" + "old." + Events._ID + ";" + " END");
    }

    private void upgradeToVersion308(SQLiteDatabase db) {
        /*
         * Changes from version 307 to 308:
         * - add Colors table to db
         * - add eventColor_index to Events table
         * - add calendar_color_index to Calendars table
         * - add allowedAttendeeTypes to Calendars table
         * - add allowedAvailability to Calendars table
         */
        createColorsTable(db);

        db.execSQL("ALTER TABLE Calendars ADD COLUMN allowedAvailability TEXT DEFAULT '0,1';");
        db.execSQL("ALTER TABLE Calendars ADD COLUMN allowedAttendeeTypes TEXT DEFAULT '0,1,2';");
        db.execSQL("ALTER TABLE Calendars ADD COLUMN calendar_color_index TEXT;");
        db.execSQL("ALTER TABLE Events ADD COLUMN eventColor_index TEXT;");

        // Default Exchange calendars to be supporting the 'tentative'
        // availability as well
        db.execSQL("UPDATE Calendars SET allowedAvailability='0,1,2' WHERE _id IN "
                + "(SELECT _id FROM Calendars WHERE account_type='com.android.exchange');");

        // Triggers to update the color stored in an event or a calendar when
        // the color_index is changed.
        createColorsTriggers(db);
    }

    private void upgradeToVersion307(SQLiteDatabase db) {
        /*
         * Changes from version 306 to 307:
         * - Changed _id field to AUTOINCREMENT
         */
        db.execSQL("ALTER TABLE Events RENAME TO Events_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        db.execSQL("DROP TRIGGER IF EXISTS original_sync_update");
        db.execSQL("DROP INDEX IF EXISTS eventsCalendarIdIndex");
        createEventsTable307(db);

        String FIELD_LIST =
            "_id, " +
            "_sync_id, " +
            "dirty, " +
            "lastSynced," +
            "calendar_id, " +
            "title, " +
            "eventLocation, " +
            "description, " +
            "eventColor, " +
            "eventStatus, " +
            "selfAttendeeStatus, " +
            "dtstart, " +
            "dtend, " +
            "eventTimezone, " +
            "duration, " +
            "allDay, " +
            "accessLevel, " +
            "availability, " +
            "hasAlarm, " +
            "hasExtendedProperties, " +
            "rrule, " +
            "rdate, " +
            "exrule, " +
            "exdate, " +
            "original_id," +
            "original_sync_id, " +
            "originalInstanceTime, " +
            "originalAllDay, " +
            "lastDate, " +
            "hasAttendeeData, " +
            "guestsCanModify, " +
            "guestsCanInviteOthers, " +
            "guestsCanSeeGuests, " +
            "organizer, " +
            "deleted, " +
            "eventEndTimezone, " +
            "sync_data1," +
            "sync_data2," +
            "sync_data3," +
            "sync_data4," +
            "sync_data5," +
            "sync_data6," +
            "sync_data7," +
            "sync_data8," +
            "sync_data9," +
            "sync_data10 ";

        // copy fields from old to new
        db.execSQL("INSERT INTO Events (" + FIELD_LIST + ") SELECT " + FIELD_LIST +
                "FROM Events_Backup;");

        db.execSQL("DROP TABLE Events_Backup;");

        // Trigger to remove data tied to an event when we delete that event.
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON " + Tables.EVENTS + " " +
                "BEGIN " + EVENTS_CLEANUP_TRIGGER_SQL + "END");

        // Trigger to update exceptions when an original event updates its
        // _sync_id
        db.execSQL(CREATE_SYNC_ID_UPDATE_TRIGGER);
    }

    private void upgradeToVersion306(SQLiteDatabase db) {
        /*
        * The following changes are for google.com accounts only.
        *
        * Change event id's from ".../private/full/... to .../events/...
        * Set Calendars.canPartiallyUpdate to 1 to support partial updates
        * Nuke sync state so we re-sync with a fresh etag and edit url
        *
        * We need to drop the original_sync_update trigger because it fires whenever the
        * sync_id field is touched, and dramatically slows this operation.
        */
        db.execSQL("DROP TRIGGER IF EXISTS original_sync_update");
        db.execSQL("UPDATE Events SET "
                + "_sync_id = REPLACE(_sync_id, '/private/full/', '/events/'), "
                + "original_sync_id = REPLACE(original_sync_id, '/private/full/', '/events/') "
                + "WHERE _id IN (SELECT Events._id FROM Events "
                +    "JOIN Calendars ON Events.calendar_id = Calendars._id "
                +    "WHERE account_type = 'com.google')"
        );
        db.execSQL(CREATE_SYNC_ID_UPDATE_TRIGGER);

        db.execSQL("UPDATE Calendars SET canPartiallyUpdate = 1 WHERE account_type = 'com.google'");

        db.execSQL("DELETE FROM _sync_state WHERE account_type = 'com.google'");
    }

    private void upgradeToVersion305(SQLiteDatabase db) {
        /*
         * Changes from version 304 to 305:
         * -Add CAL_SYNC columns up to 10
         * -Rename Calendars.access_level to calendar_access_level
         * -Rename calendars _sync_version to cal_sync7
         * -Rename calendars _sync_time to cal_sync8
         * -Rename displayName to calendar_displayName
         * -Rename _sync_local_id to sync_data2
         * -Rename htmlUri to sync_data3
         * -Rename events _sync_version to sync_data4
         * -Rename events _sync_time to sync_data5
         * -Rename commentsUri to sync_data6
         * -Migrate Events _sync_mark to sync_data8
         * -Change sync_data2 from INTEGER to TEXT
         * -Change sync_data8 from INTEGER to TEXT
         * -Add SYNC_DATA columns up to 10
         * -Add EVENT_COLOR to Events table
         */

        // rename old table, create new table with updated layout
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable305(db);

        // copy fields from old to new
        db.execSQL("INSERT INTO Calendars (" +
                "_id, " +
                "account_name, " +
                "account_type, " +
                "_sync_id, " +
                "cal_sync7, " +             // rename from _sync_version
                "cal_sync8, " +             // rename from _sync_time
                "dirty, " +
                "name, " +
                "calendar_displayName, " +  // rename from displayName
                "calendar_color, " +
                "calendar_access_level, " + // rename from access_level
                "visible, " +
                "sync_events, " +
                "calendar_location, " +
                "calendar_timezone, " +
                "ownerAccount, " +
                "canOrganizerRespond, " +
                "canModifyTimeZone, " +
                "maxReminders, " +
                "allowedReminders, " +
                "deleted, " +
                "canPartiallyUpdate," +
                "cal_sync1, " +
                "cal_sync2, " +
                "cal_sync3, " +
                "cal_sync4, " +
                "cal_sync5, " +
                "cal_sync6) " +
                "SELECT " +
                "_id, " +
                "account_name, " +
                "account_type, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "dirty, " +
                "name, " +
                "displayName, " +
                "calendar_color, " +
                "access_level, " +
                "visible, " +
                "sync_events, " +
                "calendar_location, " +
                "calendar_timezone, " +
                "ownerAccount, " +
                "canOrganizerRespond, " +
                "canModifyTimeZone, " +
                "maxReminders, " +
                "allowedReminders, " +
                "deleted, " +
                "canPartiallyUpdate," +
                "cal_sync1, " +
                "cal_sync2, " +
                "cal_sync3, " +
                "cal_sync4, " +
                "cal_sync5, " +
                "cal_sync6 " +
                "FROM Calendars_Backup;");

        // drop the old table
        db.execSQL("DROP TABLE Calendars_Backup;");

        db.execSQL("ALTER TABLE Events RENAME TO Events_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        db.execSQL("DROP INDEX IF EXISTS eventsCalendarIdIndex");
        // 305 and 307 can share the same createEventsTable implementation, because the
        // addition of "autoincrement" to _ID doesn't affect the upgrade path.  (Note that
        // much older databases may also already have autoincrement set because the change
        // was back-ported.)
        createEventsTable307(db);

        // copy fields from old to new
        db.execSQL("INSERT INTO Events (" +
                "_id, " +
                "_sync_id, " +
                "sync_data4, " +        // renamed from _sync_version
                "sync_data5, " +        // renamed from _sync_time
                "sync_data2, " +        // renamed from _sync_local_id
                "dirty, " +
                "sync_data8, " +        // renamed from _sync_mark
                "calendar_id, " +
                "sync_data3, " +        // renamed from htmlUri
                "title, " +
                "eventLocation, " +
                "description, " +
                "eventStatus, " +
                "selfAttendeeStatus, " +
                "sync_data6, " +        // renamed from commentsUri
                "dtstart, " +
                "dtend, " +
                "eventTimezone, " +
                "eventEndTimezone, " +
                "duration, " +
                "allDay, " +
                "accessLevel, " +
                "availability, " +
                "hasAlarm, " +
                "hasExtendedProperties, " +
                "rrule, " +
                "rdate, " +
                "exrule, " +
                "exdate, " +
                "original_id," +
                "original_sync_id, " +
                "originalInstanceTime, " +
                "originalAllDay, " +
                "lastDate, " +
                "hasAttendeeData, " +
                "guestsCanModify, " +
                "guestsCanInviteOthers, " +
                "guestsCanSeeGuests, " +
                "organizer, " +
                "deleted, " +
                "sync_data7," +
                "lastSynced," +
                "sync_data1) " +

                "SELECT " +
                "_id, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "_sync_local_id, " +
                "dirty, " +
                "_sync_mark, " +
                "calendar_id, " +
                "htmlUri, " +
                "title, " +
                "eventLocation, " +
                "description, " +
                "eventStatus, " +
                "selfAttendeeStatus, " +
                "commentsUri, " +
                "dtstart, " +
                "dtend, " +
                "eventTimezone, " +
                "eventEndTimezone, " +
                "duration, " +
                "allDay, " +
                "accessLevel, " +
                "availability, " +
                "hasAlarm, " +
                "hasExtendedProperties, " +
                "rrule, " +
                "rdate, " +
                "exrule, " +
                "exdate, " +
                "original_id," +
                "original_sync_id, " +
                "originalInstanceTime, " +
                "originalAllDay, " +
                "lastDate, " +
                "hasAttendeeData, " +
                "guestsCanModify, " +
                "guestsCanInviteOthers, " +
                "guestsCanSeeGuests, " +
                "organizer, " +
                "deleted, " +
                "sync_data7," +
                "lastSynced," +
                "sync_data1 " +

                "FROM Events_Backup;"
        );

        db.execSQL("DROP TABLE Events_Backup;");

        // Trigger to remove data tied to an event when we delete that event.
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON " + Tables.EVENTS + " " +
                "BEGIN " +
                EVENTS_CLEANUP_TRIGGER_SQL +
                "END");

        // Trigger to update exceptions when an original event updates its
        // _sync_id
        db.execSQL(CREATE_SYNC_ID_UPDATE_TRIGGER);
    }

    private void upgradeToVersion304(SQLiteDatabase db) {
        /*
         * Changes from version 303 to 304:
         * - add canPartiallyUpdate to Calendars table
         * - add sync_data7 to Calendars to Events table
         * - add lastSynced to Calendars to Events table
         */
        db.execSQL("ALTER TABLE Calendars ADD COLUMN canPartiallyUpdate INTEGER DEFAULT 0;");
        db.execSQL("ALTER TABLE Events ADD COLUMN sync_data7 TEXT;");
        db.execSQL("ALTER TABLE Events ADD COLUMN lastSynced INTEGER DEFAULT 0;");
    }

    private void upgradeToVersion303(SQLiteDatabase db) {
        /*
         * Changes from version 302 to 303:
         * - change SYNCx columns to CAL_SYNCx
         */

        // rename old table, create new table with updated layout
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable303(db);

        // copy fields from old to new
        db.execSQL("INSERT INTO Calendars (" +
                "_id, " +
                "account_name, " +
                "account_type, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "dirty, " +
                "name, " +
                "displayName, " +
                "calendar_color, " +
                "access_level, " +
                "visible, " +
                "sync_events, " +
                "calendar_location, " +
                "calendar_timezone, " +
                "ownerAccount, " +
                "canOrganizerRespond, " +
                "canModifyTimeZone, " +
                "maxReminders, " +
                "allowedReminders, " +
                "deleted, " +
                "cal_sync1, " +     // rename from sync1
                "cal_sync2, " +     // rename from sync2
                "cal_sync3, " +     // rename from sync3
                "cal_sync4, " +     // rename from sync4
                "cal_sync5, " +     // rename from sync5
                "cal_sync6) " +     // rename from sync6
                "SELECT " +
                "_id, " +
                "account_name, " +
                "account_type, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "dirty, " +
                "name, " +
                "displayName, " +
                "calendar_color, " +
                "access_level, " +
                "visible, " +
                "sync_events, " +
                "calendar_location, " +
                "calendar_timezone, " +
                "ownerAccount, " +
                "canOrganizerRespond, " +
                "canModifyTimeZone, " +
                "maxReminders, " +
                "allowedReminders," +
                "deleted, " +
                "sync1, " +
                "sync2, " +
                "sync3, " +
                "sync4," +
                "sync5," +
                "sync6 " +
                "FROM Calendars_Backup;"
        );

        // drop the old table
        db.execSQL("DROP TABLE Calendars_Backup;");
    }

    private void upgradeToVersion302(SQLiteDatabase db) {
        /*
         * Changes from version 301 to 302
         * - Move Exchange eventEndTimezone values to SYNC_DATA1
         */
        db.execSQL("UPDATE Events SET sync_data1=eventEndTimezone WHERE calendar_id IN "
                + "(SELECT _id FROM Calendars WHERE account_type='com.android.exchange');");

        db.execSQL("UPDATE Events SET eventEndTimezone=NULL WHERE calendar_id IN "
                + "(SELECT _id FROM Calendars WHERE account_type='com.android.exchange');");
    }

    private void upgradeToVersion301(SQLiteDatabase db) {
        /*
         * Changes from version 300 to 301
         * - Added original_id column to Events table
         * - Added triggers to keep original_id and original_sync_id in sync
         */

        db.execSQL("DROP TRIGGER IF EXISTS " + SYNC_ID_UPDATE_TRIGGER_NAME + ";");

        db.execSQL("ALTER TABLE Events ADD COLUMN original_id INTEGER;");

        // Fill in the original_id for all events that have an original_sync_id
        db.execSQL("UPDATE Events set original_id=" +
                "(SELECT Events2._id FROM Events AS Events2 " +
                        "WHERE Events2._sync_id=Events.original_sync_id) " +
                "WHERE Events.original_sync_id NOT NULL");
        // Trigger to update exceptions when an original event updates its
        // _sync_id
        db.execSQL(CREATE_SYNC_ID_UPDATE_TRIGGER);
    }

    private void upgradeToVersion300(SQLiteDatabase db) {

        /*
         * Changes from version 205 to 300:
         * - rename _sync_account to account_name in Calendars table
         * - remove _sync_account from Events table
         * - rename _sync_account_type to account_type in Calendars table
         * - remove _sync_account_type from Events table
         * - rename _sync_dirty to dirty in Calendars/Events table
         * - rename color to calendar_color in Calendars table
         * - rename location to calendar_location in Calendars table
         * - rename timezone to calendar_timezone in Calendars table
         * - add allowedReminders in Calendars table
         * - rename visibility to accessLevel in Events table
         * - rename transparency to availability in Events table
         * - rename originalEvent to original_sync_id in Events table
         * - remove dtstart2 and dtend2 from Events table
         * - rename syncAdapterData to sync_data1 in Events table
         */

        // rename old table, create new table with updated layout
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup;");
        createCalendarsTable300(db);

        // copy fields from old to new
        db.execSQL("INSERT INTO Calendars (" +
                "_id, " +
                "account_name, " +          // rename from _sync_account
                "account_type, " +          // rename from _sync_account_type
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "dirty, " +                 // rename from _sync_dirty
                "name, " +
                "displayName, " +
                "calendar_color, " +        // rename from color
                "access_level, " +
                "visible, " +
                "sync_events, " +
                "calendar_location, " +     // rename from location
                "calendar_timezone, " +     // rename from timezone
                "ownerAccount, " +
                "canOrganizerRespond, " +
                "canModifyTimeZone, " +
                "maxReminders, " +
                "allowedReminders," +
                "deleted, " +
                "sync1, " +
                "sync2, " +
                "sync3, " +
                "sync4," +
                "sync5," +
                "sync6) " +

                "SELECT " +
                "_id, " +
                "_sync_account, " +
                "_sync_account_type, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "_sync_dirty, " +
                "name, " +
                "displayName, " +
                "color, " +
                "access_level, " +
                "visible, " +
                "sync_events, " +
                "location, " +
                "timezone, " +
                "ownerAccount, " +
                "canOrganizerRespond, " +
                "canModifyTimeZone, " +
                "maxReminders, " +
                "'0,1,2,3'," +
                "deleted, " +
                "sync1, " +
                "sync2, " +
                "sync3, " +
                "sync4, " +
                "sync5, " +
                "sync6 " +
                "FROM Calendars_Backup;"
        );

        /* expand the set of allowed reminders for Google calendars to include email */
        db.execSQL("UPDATE Calendars SET allowedReminders = '0,1,2' " +
                "WHERE account_type = 'com.google'");

        // drop the old table
        db.execSQL("DROP TABLE Calendars_Backup;");

        db.execSQL("ALTER TABLE Events RENAME TO Events_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS events_insert");
        db.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        db.execSQL("DROP INDEX IF EXISTS eventSyncAccountAndIdIndex");
        db.execSQL("DROP INDEX IF EXISTS eventsCalendarIdIndex");
        createEventsTable300(db);

        // copy fields from old to new
        db.execSQL("INSERT INTO Events (" +
                "_id, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "_sync_local_id, " +
                "dirty, " +                 // renamed from _sync_dirty
                "_sync_mark, " +
                "calendar_id, " +
                "htmlUri, " +
                "title, " +
                "eventLocation, " +
                "description, " +
                "eventStatus, " +
                "selfAttendeeStatus, " +
                "commentsUri, " +
                "dtstart, " +
                "dtend, " +
                "eventTimezone, " +
                "eventEndTimezone, " +      // renamed from eventTimezone2
                "duration, " +
                "allDay, " +
                "accessLevel, " +           // renamed from visibility
                "availability, " +          // renamed from transparency
                "hasAlarm, " +
                "hasExtendedProperties, " +
                "rrule, " +
                "rdate, " +
                "exrule, " +
                "exdate, " +
                "original_sync_id, " +      // renamed from originalEvent
                "originalInstanceTime, " +
                "originalAllDay, " +
                "lastDate, " +
                "hasAttendeeData, " +
                "guestsCanModify, " +
                "guestsCanInviteOthers, " +
                "guestsCanSeeGuests, " +
                "organizer, " +
                "deleted, " +
                "sync_data1) " +             // renamed from syncAdapterData

                "SELECT " +
                "_id, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "_sync_local_id, " +
                "_sync_dirty, " +
                "_sync_mark, " +
                "calendar_id, " +
                "htmlUri, " +
                "title, " +
                "eventLocation, " +
                "description, " +
                "eventStatus, " +
                "selfAttendeeStatus, " +
                "commentsUri, " +
                "dtstart, " +
                "dtend, " +
                "eventTimezone, " +
                "eventTimezone2, " +
                "duration, " +
                "allDay, " +
                "visibility, " +
                "transparency, " +
                "hasAlarm, " +
                "hasExtendedProperties, " +
                "rrule, " +
                "rdate, " +
                "exrule, " +
                "exdate, " +
                "originalEvent, " +
                "originalInstanceTime, " +
                "originalAllDay, " +
                "lastDate, " +
                "hasAttendeeData, " +
                "guestsCanModify, " +
                "guestsCanInviteOthers, " +
                "guestsCanSeeGuests, " +
                "organizer, " +
                "deleted, " +
                "syncAdapterData " +

                "FROM Events_Backup;"
        );

        db.execSQL("DROP TABLE Events_Backup;");

        // Trigger to remove data tied to an event when we delete that event.
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON " + Tables.EVENTS + " " +
                "BEGIN " +
                EVENTS_CLEANUP_TRIGGER_SQL +
                "END");

    }

    private void upgradeToVersion205(SQLiteDatabase db) {
        /*
         * Changes from version 204 to 205:
         * - rename+reorder "_sync_mark" to "sync6" (and change type from INTEGER to TEXT)
         * - rename "selected" to "visible"
         * - rename "organizerCanRespond" to "canOrganizerRespond"
         * - add "canModifyTimeZone"
         * - add "maxReminders"
         * - remove "_sync_local_id" (a/k/a _SYNC_DATA)
         */

        // rename old table, create new table with updated layout
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable205(db);

        // copy fields from old to new
        db.execSQL("INSERT INTO Calendars (" +
                "_id, " +
                "_sync_account, " +
                "_sync_account_type, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "_sync_dirty, " +
                "name, " +
                "displayName, " +
                "color, " +
                "access_level, " +
                "visible, " +                   // rename from "selected"
                "sync_events, " +
                "location, " +
                "timezone, " +
                "ownerAccount, " +
                "canOrganizerRespond, " +       // rename from "organizerCanRespond"
                "canModifyTimeZone, " +
                "maxReminders, " +
                "deleted, " +
                "sync1, " +
                "sync2, " +
                "sync3, " +
                "sync4," +
                "sync5," +
                "sync6) " +                     // rename/reorder from _sync_mark
                "SELECT " +
                "_id, " +
                "_sync_account, " +
                "_sync_account_type, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "_sync_dirty, " +
                "name, " +
                "displayName, " +
                "color, " +
                "access_level, " +
                "selected, " +
                "sync_events, " +
                "location, " +
                "timezone, " +
                "ownerAccount, " +
                "organizerCanRespond, " +
                "1, " +
                "5, " +
                "deleted, " +
                "sync1, " +
                "sync2, " +
                "sync3, " +
                "sync4, " +
                "sync5, " +
                "_sync_mark " +
                "FROM Calendars_Backup;"
        );

        // set these fields appropriately for Exchange events
        db.execSQL("UPDATE Calendars SET canModifyTimeZone=0, maxReminders=1 " +
                "WHERE _sync_account_type='com.android.exchange'");

        // drop the old table
        db.execSQL("DROP TABLE Calendars_Backup;");
    }

    private void upgradeToVersion203(SQLiteDatabase db) {
        // Same as Gingerbread version 100
        Cursor cursor = db.rawQuery("SELECT value FROM CalendarCache WHERE key=?",
                new String[] {"timezoneDatabaseVersion"});

        String oldTimezoneDbVersion = null;
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    oldTimezoneDbVersion = cursor.getString(0);
                    cursor.close();
                    cursor = null;
                    // Also clean the CalendarCache table
                    db.execSQL("DELETE FROM CalendarCache;");
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        initCalendarCacheTable203(db, oldTimezoneDbVersion);

        // Same as Gingerbread version 101
        updateCalendarCacheTableTo203(db);
    }

    private void upgradeToVersion202(SQLiteDatabase db) {
        // We will drop the "hidden" column from the calendar schema and add the "sync5" column
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");

        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable202(db);

        // Populate the new Calendars table and put into the "sync5" column the value of the
        // old "hidden" column
        db.execSQL("INSERT INTO Calendars (" +
                "_id, " +
                "_sync_account, " +
                "_sync_account_type, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "_sync_local_id, " +
                "_sync_dirty, " +
                "_sync_mark, " +
                "name, " +
                "displayName, " +
                "color, " +
                "access_level, " +
                "selected, " +
                "sync_events, " +
                "location, " +
                "timezone, " +
                "ownerAccount, " +
                "organizerCanRespond, " +
                "deleted, " +
                "sync1, " +
                "sync2, " +
                "sync3, " +
                "sync4," +
                "sync5) " +
                "SELECT " +
                "_id, " +
                "_sync_account, " +
                "_sync_account_type, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "_sync_local_id, " +
                "_sync_dirty, " +
                "_sync_mark, " +
                "name, " +
                "displayName, " +
                "color, " +
                "access_level, " +
                "selected, " +
                "sync_events, " +
                "location, " +
                "timezone, " +
                "ownerAccount, " +
                "organizerCanRespond, " +
                "deleted, " +
                "sync1, " +
                "sync2, " +
                "sync3, " +
                "sync4, " +
                "hidden " +
                "FROM Calendars_Backup;"
        );

        // Drop the backup table
        db.execSQL("DROP TABLE Calendars_Backup;");
    }

    private void upgradeToVersion201(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars ADD COLUMN sync4 TEXT;");
    }

    private void upgradeToVersion200(SQLiteDatabase db) {
        // we cannot use here a Calendar.Calendars,URL constant for "url" as we are trying to make
        // it disappear so we are keeping the hardcoded name "url" in all the SQLs
        db.execSQL("ALTER TABLE Calendars RENAME TO Calendars_Backup;");

        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        createCalendarsTable200(db);

        // Populate the new Calendars table except the SYNC2 / SYNC3 columns
        db.execSQL("INSERT INTO Calendars (" +
                "_id, " +
                "_sync_account, " +
                "_sync_account_type, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "_sync_local_id, " +
                "_sync_dirty, " +
                "_sync_mark, " +
                "name, " +
                "displayName, " +
                "color, " +
                "access_level, " +
                "selected, " +
                "sync_events, " +
                "location, " +
                "timezone, " +
                "ownerAccount, " +
                "organizerCanRespond, " +
                "deleted, " +
                "sync1) " +
                "SELECT " +
                "_id, " +
                "_sync_account, " +
                "_sync_account_type, " +
                "_sync_id, " +
                "_sync_version, " +
                "_sync_time, " +
                "_sync_local_id, " +
                "_sync_dirty, " +
                "_sync_mark, " +
                "name, " +
                "displayName, " +
                "color, " +
                "access_level, " +
                "selected, " +
                "sync_events, " +
                "location, " +
                "timezone, " +
                "ownerAccount, " +
                "organizerCanRespond, " +
                "0, " +
                "url " +
                "FROM Calendars_Backup;"
        );

        // Populate SYNC2 and SYNC3 columns - SYNC1 represent the old "url" column
        // We will need to iterate over all the "com.google" type of calendars
        String selectSql = "SELECT _id, url" +
                " FROM Calendars_Backup" +
                " WHERE _sync_account_type='com.google'" +
                " AND url IS NOT NULL;";

        String updateSql = "UPDATE Calendars SET " +
                "sync2=?, " + // edit Url
                "sync3=? " + // self Url
                "WHERE _id=?;";

        Cursor cursor = db.rawQuery(selectSql, null /* selection args */);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    Object[] bindArgs = new Object[3];
                    while (cursor.moveToNext()) {
                        Long id = cursor.getLong(0);
                        String url = cursor.getString(1);
                        String selfUrl = getSelfUrlFromEventsUrl(url);
                        String editUrl = getEditUrlFromEventsUrl(url);

                        bindArgs[0] = editUrl;
                        bindArgs[1] = selfUrl;
                        bindArgs[2] = id;

                        db.execSQL(updateSql, bindArgs);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        // Drop the backup table
        db.execSQL("DROP TABLE Calendars_Backup;");
    }

    @VisibleForTesting
    public static void upgradeToVersion69(SQLiteDatabase db) {
        // Clean up allDay events which could be in an invalid state from an earlier version
        // Some allDay events had hour, min, sec not set to zero, which throws elsewhere. This
        // will go through the allDay events and make sure they have proper values and are in the
        // correct timezone. Verifies that dtstart and dtend are in UTC and at midnight, that
        // eventTimezone is set to UTC, tries to make sure duration is in days, and that dtstart2
        // and dtend2 are at midnight in their timezone.
        final String sql = "SELECT _id, " +
                "dtstart, " +
                "dtend, " +
                "duration, " +
                "dtstart2, " +
                "dtend2, " +
                "eventTimezone, " +
                "eventTimezone2, " +
                "rrule " +
                "FROM Events " +
                "WHERE allDay=?";
        Cursor cursor = db.rawQuery(sql, new String[] {"1"});
        if (cursor != null) {
            try {
                String timezone;
                String timezone2;
                String duration;
                Long dtstart;
                Long dtstart2;
                Long dtend;
                Long dtend2;
                Time time = new Time();
                Long id;
                // some things need to be in utc so we call this frequently, cache to make faster
                final String utc = Time.TIMEZONE_UTC;
                while (cursor.moveToNext()) {
                    String rrule = cursor.getString(8);
                    id = cursor.getLong(0);
                    dtstart = cursor.getLong(1);
                    dtstart2 = null;
                    timezone = cursor.getString(6);
                    timezone2 = cursor.getString(7);
                    duration = cursor.getString(3);

                    if (TextUtils.isEmpty(rrule)) {
                        // For non-recurring events dtstart and dtend should both have values
                        // and duration should be null.
                        dtend = cursor.getLong(2);
                        dtend2 = null;
                        // Since we made all three of these at the same time if timezone2 exists
                        // so should dtstart2 and dtend2.
                        if(!TextUtils.isEmpty(timezone2)) {
                            dtstart2 = cursor.getLong(4);
                            dtend2 = cursor.getLong(5);
                        }

                        boolean update = false;
                        if (!TextUtils.equals(timezone, utc)) {
                            update = true;
                            timezone = utc;
                        }

                        time.clear(timezone);
                        update |= fixAllDayTime(time, timezone, dtstart);
                        dtstart = time.normalize(false);

                        time.clear(timezone);
                        update |= fixAllDayTime(time, timezone, dtend);
                        dtend = time.normalize(false);

                        if (dtstart2 != null) {
                            time.clear(timezone2);
                            update |= fixAllDayTime(time, timezone2, dtstart2);
                            dtstart2 = time.normalize(false);
                        }

                        if (dtend2 != null) {
                            time.clear(timezone2);
                            update |= fixAllDayTime(time, timezone2, dtend2);
                            dtend2 = time.normalize(false);
                        }

                        if (!TextUtils.isEmpty(duration)) {
                            update = true;
                        }

                        if (update) {
                            // enforce duration being null
                            db.execSQL("UPDATE Events SET " +
                                    "dtstart=?, " +
                                    "dtend=?, " +
                                    "dtstart2=?, " +
                                    "dtend2=?, " +
                                    "duration=?, " +
                                    "eventTimezone=?, " +
                                    "eventTimezone2=? " +
                                    "WHERE _id=?",
                                    new Object[] {
                                            dtstart,
                                            dtend,
                                            dtstart2,
                                            dtend2,
                                            null,
                                            timezone,
                                            timezone2,
                                            id}
                            );
                        }

                    } else {
                        // For recurring events only dtstart and duration should be used.
                        // We ignore dtend since it will be overwritten if the event changes to a
                        // non-recurring event and won't be used otherwise.
                        if(!TextUtils.isEmpty(timezone2)) {
                            dtstart2 = cursor.getLong(4);
                        }

                        boolean update = false;
                        if (!TextUtils.equals(timezone, utc)) {
                            update = true;
                            timezone = utc;
                        }

                        time.clear(timezone);
                        update |= fixAllDayTime(time, timezone, dtstart);
                        dtstart = time.normalize(false);

                        if (dtstart2 != null) {
                            time.clear(timezone2);
                            update |= fixAllDayTime(time, timezone2, dtstart2);
                            dtstart2 = time.normalize(false);
                        }

                        if (TextUtils.isEmpty(duration)) {
                            // If duration was missing assume a 1 day duration
                            duration = "P1D";
                            update = true;
                        } else {
                            int len = duration.length();
                            // TODO fix durations in other formats as well
                            if (duration.charAt(0) == 'P' &&
                                    duration.charAt(len - 1) == 'S') {
                                int seconds = Integer.parseInt(duration.substring(1, len - 1));
                                int days = (seconds + DAY_IN_SECONDS - 1) / DAY_IN_SECONDS;
                                duration = "P" + days + "D";
                                update = true;
                            }
                        }

                        if (update) {
                            // If there were other problems also enforce dtend being null
                            db.execSQL("UPDATE Events SET " +
                                    "dtstart=?, " +
                                    "dtend=?, " +
                                    "dtstart2=?, " +
                                    "dtend2=?, " +
                                    "duration=?," +
                                    "eventTimezone=?, " +
                                    "eventTimezone2=? " +
                                    "WHERE _id=?",
                                    new Object[] {
                                            dtstart,
                                            null,
                                            dtstart2,
                                            null,
                                            duration,
                                            timezone,
                                            timezone2,
                                            id}
                            );
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void upgradeToVersion66(SQLiteDatabase db) {
        // Add a column to indicate whether the event organizer can respond to his own events
        // The UI should not show attendee status for events in calendars with this column = 0
        db.execSQL("ALTER TABLE Calendars" +
                " ADD COLUMN organizerCanRespond INTEGER NOT NULL DEFAULT 1;");
    }

    private void upgradeToVersion64(SQLiteDatabase db) {
        // Add a column that may be used by sync adapters
        db.execSQL("ALTER TABLE Events" +
                " ADD COLUMN syncAdapterData TEXT;");
    }

    private void upgradeToVersion62(SQLiteDatabase db) {
        // New columns are to transition to having allDay events in the local timezone
        db.execSQL("ALTER TABLE Events" +
                " ADD COLUMN dtstart2 INTEGER;");
        db.execSQL("ALTER TABLE Events" +
                " ADD COLUMN dtend2 INTEGER;");
        db.execSQL("ALTER TABLE Events" +
                " ADD COLUMN eventTimezone2 TEXT;");

        String[] allDayBit = new String[] {"0"};
        // Copy over all the data that isn't an all day event.
        db.execSQL("UPDATE Events SET " +
                "dtstart2=dtstart," +
                "dtend2=dtend," +
                "eventTimezone2=eventTimezone " +
                "WHERE allDay=?;",
                allDayBit /* selection args */);

        // "cursor" iterates over all the calendars
        allDayBit[0] = "1";
        Cursor cursor = db.rawQuery("SELECT Events._id," +
                "dtstart," +
                "dtend," +
                "eventTimezone," +
                "timezone " +
                "FROM Events INNER JOIN Calendars " +
                "WHERE Events.calendar_id=Calendars._id" +
                " AND allDay=?",
                allDayBit /* selection args */);

        Time oldTime = new Time();
        Time newTime = new Time();
        // Update the allday events in the new columns
        if (cursor != null) {
            try {
                String[] newData = new String[4];
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0); // Order from query above
                    long dtstart = cursor.getLong(1);
                    long dtend = cursor.getLong(2);
                    String eTz = cursor.getString(3); // current event timezone
                    String tz = cursor.getString(4); // Calendar timezone
                    //If there's no timezone for some reason use UTC by default.
                    if(eTz == null) {
                        eTz = Time.TIMEZONE_UTC;
                    }

                    // Convert start time for all day events into the timezone of their calendar
                    oldTime.clear(eTz);
                    oldTime.set(dtstart);
                    newTime.clear(tz);
                    newTime.set(oldTime.monthDay, oldTime.month, oldTime.year);
                    newTime.normalize(false);
                    dtstart = newTime.toMillis(false /*ignoreDst*/);

                    // Convert end time for all day events into the timezone of their calendar
                    oldTime.clear(eTz);
                    oldTime.set(dtend);
                    newTime.clear(tz);
                    newTime.set(oldTime.monthDay, oldTime.month, oldTime.year);
                    newTime.normalize(false);
                    dtend = newTime.toMillis(false /*ignoreDst*/);

                    newData[0] = String.valueOf(dtstart);
                    newData[1] = String.valueOf(dtend);
                    newData[2] = tz;
                    newData[3] = String.valueOf(id);
                    db.execSQL("UPDATE Events SET " +
                            "dtstart2=?, " +
                            "dtend2=?, " +
                            "eventTimezone2=? " +
                            "WHERE _id=?",
                            newData);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void upgradeToVersion61(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS CalendarCache;");

        // IF NOT EXISTS should be normal pattern for table creation
        db.execSQL("CREATE TABLE IF NOT EXISTS CalendarCache (" +
                "_id INTEGER PRIMARY KEY," +
                "key TEXT NOT NULL," +
                "value TEXT" +
                ");");

        db.execSQL("INSERT INTO CalendarCache (" +
                "key, " +
                "value) VALUES (" +
                "'timezoneDatabaseVersion',"  +
                "'2009s'" +
                ");");
    }

    private void upgradeToVersion60(SQLiteDatabase db) {
        // Switch to CalendarProvider2
        upgradeSyncState(db);
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars " +
                "BEGIN " +
                ("DELETE FROM Events" +
                        " WHERE calendar_id=old._id;") +
                "END");
        db.execSQL("ALTER TABLE Events" +
                " ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("DROP TRIGGER IF EXISTS events_insert");
        // Trigger to set event's sync_account
        db.execSQL("CREATE TRIGGER events_insert AFTER INSERT ON Events " +
                "BEGIN " +
                "UPDATE Events" +
                " SET _sync_account=" +
                " (SELECT _sync_account FROM Calendars" +
                " WHERE Calendars._id=new.calendar_id)," +
                "_sync_account_type=" +
                " (SELECT _sync_account_type FROM Calendars" +
                " WHERE Calendars._id=new.calendar_id) " +
                "WHERE Events._id=new._id;" +
                "END");
        db.execSQL("DROP TABLE IF EXISTS DeletedEvents;");
        db.execSQL("DROP TRIGGER IF EXISTS events_cleanup_delete");
        // Trigger to remove data tied to an event when we delete that event.
        db.execSQL("CREATE TRIGGER events_cleanup_delete DELETE ON Events " +
                "BEGIN " +
                ("DELETE FROM Instances" +
                    " WHERE event_id=old._id;" +
                "DELETE FROM EventsRawTimes" +
                    " WHERE event_id=old._id;" +
                "DELETE FROM Attendees" +
                    " WHERE event_id=old._id;" +
                "DELETE FROM Reminders" +
                    " WHERE event_id=old._id;" +
                "DELETE FROM CalendarAlerts" +
                    " WHERE event_id=old._id;" +
                "DELETE FROM ExtendedProperties" +
                    " WHERE event_id=old._id;") +
                "END");
        db.execSQL("DROP TRIGGER IF EXISTS attendees_update");
        db.execSQL("DROP TRIGGER IF EXISTS attendees_insert");
        db.execSQL("DROP TRIGGER IF EXISTS attendees_delete");
        db.execSQL("DROP TRIGGER IF EXISTS reminders_update");
        db.execSQL("DROP TRIGGER IF EXISTS reminders_insert");
        db.execSQL("DROP TRIGGER IF EXISTS reminders_delete");
        db.execSQL("DROP TRIGGER IF EXISTS extended_properties_update");
        db.execSQL("DROP TRIGGER IF EXISTS extended_properties_insert");
        db.execSQL("DROP TRIGGER IF EXISTS extended_properties_delete");
    }

    private void upgradeToVersion59(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS BusyBits;");
        db.execSQL("CREATE TEMPORARY TABLE CalendarMetaData_Backup(" +
                "_id," +
                "localTimezone," +
                "minInstance," +
                "maxInstance" +
                ");");
        db.execSQL("INSERT INTO CalendarMetaData_Backup " +
                "SELECT " +
                "_id," +
                "localTimezone," +
                "minInstance," +
                "maxInstance" +
                " FROM CalendarMetaData;");
        db.execSQL("DROP TABLE CalendarMetaData;");
        createCalendarMetaDataTable59(db);
        db.execSQL("INSERT INTO CalendarMetaData " +
                "SELECT " +
                "_id," +
                "localTimezone," +
                "minInstance," +
                "maxInstance" +
                " FROM CalendarMetaData_Backup;");
        db.execSQL("DROP TABLE CalendarMetaData_Backup;");
    }

    private void upgradeToVersion57(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Events" +
                " ADD COLUMN guestsCanModify" +
                " INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE Events" +
                " ADD COLUMN guestsCanInviteOthers" +
                " INTEGER NOT NULL DEFAULT 1;");
        db.execSQL("ALTER TABLE Events" +
                " ADD COLUMN guestsCanSeeGuests" +
                " INTEGER NOT NULL DEFAULT 1;");
        db.execSQL("ALTER TABLE Events" +
                " ADD COLUMN organizer" +
                " STRING;");
        db.execSQL("UPDATE Events SET organizer=" +
                "(SELECT attendeeEmail" +
                " FROM Attendees"  +
                " WHERE " +
                "Attendees.event_id=" +
                "Events._id" +
                " AND " +
                "Attendees.attendeeRelationship=2);");
    }

    private void upgradeToVersion56(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars" +
                " ADD COLUMN ownerAccount TEXT;");
        db.execSQL("ALTER TABLE Events" +
                " ADD COLUMN hasAttendeeData INTEGER NOT NULL DEFAULT 0;");

        // Clear _sync_dirty to avoid a client-to-server sync that could blow away
        // server attendees.
        // Clear _sync_version to pull down the server's event (with attendees)
        // Change the URLs from full-selfattendance to full
        db.execSQL("UPDATE Events"
                + " SET _sync_dirty=0, "
                + "_sync_version=NULL, "
                + "_sync_id="
                + "REPLACE(_sync_id, " +
                    "'/private/full-selfattendance', '/private/full'),"
                + "commentsUri="
                + "REPLACE(commentsUri, " +
                    "'/private/full-selfattendance', '/private/full');");

        db.execSQL("UPDATE Calendars"
                + " SET url="
                + "REPLACE(url, '/private/full-selfattendance', '/private/full');");

        // "cursor" iterates over all the calendars
        Cursor cursor = db.rawQuery("SELECT _id, " +
                "url FROM Calendars",
                null /* selection args */);
        // Add the owner column.
        if (cursor != null) {
            try {
                final String updateSql = "UPDATE Calendars" +
                        " SET ownerAccount=?" +
                        " WHERE _id=?";
                while (cursor.moveToNext()) {
                    Long id = cursor.getLong(0);
                    String url = cursor.getString(1);
                    String owner = calendarEmailAddressFromFeedUrl(url);
                    db.execSQL(updateSql, new Object[] {owner, id});
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void upgradeResync(SQLiteDatabase db) {
        // Delete sync state, so all records will be re-synced.
        db.execSQL("DELETE FROM _sync_state;");

        // "cursor" iterates over all the calendars
        Cursor cursor = db.rawQuery("SELECT _sync_account," +
                "_sync_account_type,url FROM Calendars",
                null /* selection args */);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String accountName = cursor.getString(0);
                    String accountType = cursor.getString(1);
                    final Account account = new Account(accountName, accountType);
                    String calendarUrl = cursor.getString(2);
                    scheduleSync(account, false /* two-way sync */, calendarUrl);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void upgradeToVersion55(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE Calendars ADD COLUMN " +
                "_sync_account_type TEXT;");
        db.execSQL("ALTER TABLE Events ADD COLUMN " +
                "_sync_account_type TEXT;");
        db.execSQL("ALTER TABLE DeletedEvents ADD COLUMN _sync_account_type TEXT;");
        db.execSQL("UPDATE Calendars"
                + " SET _sync_account_type='com.google'"
                + " WHERE _sync_account IS NOT NULL");
        db.execSQL("UPDATE Events"
                + " SET _sync_account_type='com.google'"
                + " WHERE _sync_account IS NOT NULL");
        db.execSQL("UPDATE DeletedEvents"
                + " SET _sync_account_type='com.google'"
                + " WHERE _sync_account IS NOT NULL");
        Log.w(TAG, "re-creating eventSyncAccountAndIdIndex");
        db.execSQL("DROP INDEX eventSyncAccountAndIdIndex");
        db.execSQL("CREATE INDEX eventSyncAccountAndIdIndex ON Events ("
                + "_sync_account_type, "
                + "_sync_account, "
                + "_sync_id);");
    }

    private void upgradeToVersion54(SQLiteDatabase db) {
        Log.w(TAG, "adding eventSyncAccountAndIdIndex");
        db.execSQL("CREATE INDEX eventSyncAccountAndIdIndex ON Events ("
                + "_sync_account, _sync_id);");
    }

    private void upgradeToVersion53(SQLiteDatabase db) {
        Log.w(TAG, "Upgrading CalendarAlerts table");
        db.execSQL("ALTER TABLE CalendarAlerts ADD COLUMN " +
                "creationTime INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE CalendarAlerts ADD COLUMN " +
                "receivedTime INTEGER NOT NULL DEFAULT 0;");
        db.execSQL("ALTER TABLE CalendarAlerts ADD COLUMN " +
                "notifyTime INTEGER NOT NULL DEFAULT 0;");
    }

    private void upgradeToVersion52(SQLiteDatabase db) {
        // We added "originalAllDay" to the Events table to keep track of
        // the allDay status of the original recurring event for entries
        // that are exceptions to that recurring event.  We need this so
        // that we can format the date correctly for the "originalInstanceTime"
        // column when we make a change to the recurrence exception and
        // send it to the server.
        db.execSQL("ALTER TABLE Events ADD COLUMN " +
                "originalAllDay INTEGER;");

        // Iterate through the Events table and for each recurrence
        // exception, fill in the correct value for "originalAllDay",
        // if possible.  The only times where this might not be possible
        // are (1) the original recurring event no longer exists, or
        // (2) the original recurring event does not yet have a _sync_id
        // because it was created on the phone and hasn't been synced to the
        // server yet.  In both cases the originalAllDay field will be set
        // to null.  In the first case we don't care because the recurrence
        // exception will not be displayed and we won't be able to make
        // any changes to it (and even if we did, the server should ignore
        // them, right?).  In the second case, the calendar client already
        // disallows making changes to an instance of a recurring event
        // until the recurring event has been synced to the server so the
        // second case should never occur.

        // "cursor" iterates over all the recurrences exceptions.
        Cursor cursor = db.rawQuery("SELECT _id," +
                "originalEvent" +
                " FROM Events" +
                " WHERE originalEvent IS NOT NULL",
                null /* selection args */);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String originalEvent = cursor.getString(1);

                    // Find the original recurring event (if it exists)
                    Cursor recur = db.rawQuery("SELECT allDay" +
                            " FROM Events" +
                            " WHERE _sync_id=?",
                            new String[] {originalEvent});
                    if (recur == null) {
                        continue;
                    }

                    try {
                        // Fill in the "originalAllDay" field of the
                        // recurrence exception with the "allDay" value
                        // from the recurring event.
                        if (recur.moveToNext()) {
                            int allDay = recur.getInt(0);
                            db.execSQL("UPDATE Events" +
                                    " SET originalAllDay=" + allDay +
                                    " WHERE _id="+id);
                        }
                    } finally {
                        recur.close();
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void upgradeToVersion51(SQLiteDatabase db) {
        Log.w(TAG, "Upgrading DeletedEvents table");

        // We don't have enough information to fill in the correct
        // value of the calendar_id for old rows in the DeletedEvents
        // table, but rows in that table are transient so it is unlikely
        // that there are any rows.  Plus, the calendar_id is used only
        // when deleting a calendar, which is a rare event.  All new rows
        // will have the correct calendar_id.
        db.execSQL("ALTER TABLE DeletedEvents ADD COLUMN calendar_id INTEGER;");

        // Trigger to remove a calendar's events when we delete the calendar
        db.execSQL("DROP TRIGGER IF EXISTS calendar_cleanup");
        db.execSQL("CREATE TRIGGER calendar_cleanup DELETE ON Calendars " +
                "BEGIN " +
                "DELETE FROM Events WHERE calendar_id=" +
                    "old._id;" +
                "DELETE FROM DeletedEvents WHERE calendar_id = old._id;" +
                "END");
        db.execSQL("DROP TRIGGER IF EXISTS event_to_deleted");
    }

    private void dropTables(SQLiteDatabase db) {
        Log.i(TAG, "Clearing database");

        String[] columns = {
                "type", "name"
        };
        Cursor cursor = db.query("sqlite_master", columns, null, null, null, null, null);
        if (cursor == null) {
            return;
        }
        try {
            while (cursor.moveToNext()) {
                final String name = cursor.getString(1);
                if (!name.startsWith("sqlite_")) {
                    // If it's not a SQL-controlled entity, drop it
                    final String sql = "DROP " + cursor.getString(0) + " IF EXISTS " + name;
                    try {
                        db.execSQL(sql);
                    } catch (SQLException e) {
                        Log.e(TAG, "Error executing " + sql + " " + e.toString());
                    }
                }
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db = super.getWritableDatabase();
        return db;
    }

    public SyncStateContentProviderHelper getSyncState() {
        return mSyncState;
    }

    /**
     * Schedule a calendar sync for the account.
     * @param account the account for which to schedule a sync
     * @param uploadChangesOnly if set, specify that the sync should only send
     *   up local changes.  This is typically used for a local sync, a user override of
     *   too many deletions, or a sync after a calendar is unselected.
     * @param url the url feed for the calendar to sync (may be null, in which case a poll of
     *   all feeds is done.)
     */
    void scheduleSync(Account account, boolean uploadChangesOnly, String url) {
        Bundle extras = new Bundle();
        if (uploadChangesOnly) {
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, uploadChangesOnly);
        }
        if (url != null) {
            extras.putString("feed", url);
        }
        ContentResolver.requestSync(account, Calendars.CONTENT_URI.getAuthority(),
                extras);
    }

    private static void createEventsView(SQLiteDatabase db) {
        db.execSQL("DROP VIEW IF EXISTS " + Views.EVENTS + ";");
        String eventsSelect = "SELECT "
                + Tables.EVENTS + "." + CalendarContract.Events._ID
                        + " AS " + CalendarContract.Events._ID + ","
                + CalendarContract.Events.TITLE + ","
                + CalendarContract.Events.DESCRIPTION + ","
                + CalendarContract.Events.EVENT_LOCATION + ","
                + CalendarContract.Events.EVENT_COLOR + ","
                + CalendarContract.Events.EVENT_COLOR_KEY + ","
                + CalendarContract.Events.STATUS + ","
                + CalendarContract.Events.SELF_ATTENDEE_STATUS + ","
                + CalendarContract.Events.DTSTART + ","
                + CalendarContract.Events.DTEND + ","
                + CalendarContract.Events.DURATION + ","
                + CalendarContract.Events.EVENT_TIMEZONE + ","
                + CalendarContract.Events.EVENT_END_TIMEZONE + ","
                + CalendarContract.Events.ALL_DAY + ","
                + CalendarContract.Events.ACCESS_LEVEL + ","
                + CalendarContract.Events.AVAILABILITY + ","
                + CalendarContract.Events.HAS_ALARM + ","
                + CalendarContract.Events.HAS_EXTENDED_PROPERTIES + ","
                + CalendarContract.Events.RRULE + ","
                + CalendarContract.Events.RDATE + ","
                + CalendarContract.Events.EXRULE + ","
                + CalendarContract.Events.EXDATE + ","
                + CalendarContract.Events.ORIGINAL_SYNC_ID + ","
                + CalendarContract.Events.ORIGINAL_ID + ","
                + CalendarContract.Events.ORIGINAL_INSTANCE_TIME + ","
                + CalendarContract.Events.ORIGINAL_ALL_DAY + ","
                + CalendarContract.Events.LAST_DATE + ","
                + CalendarContract.Events.HAS_ATTENDEE_DATA + ","
                + CalendarContract.Events.CALENDAR_ID + ","
                + CalendarContract.Events.GUESTS_CAN_INVITE_OTHERS + ","
                + CalendarContract.Events.GUESTS_CAN_MODIFY + ","
                + CalendarContract.Events.GUESTS_CAN_SEE_GUESTS + ","
                + CalendarContract.Events.ORGANIZER + ","
                + "COALESCE("
                + Events.IS_ORGANIZER + ", " + Events.ORGANIZER + " = " + Calendars.OWNER_ACCOUNT
                + ") AS " + Events.IS_ORGANIZER + ","
                + CalendarContract.Events.CUSTOM_APP_PACKAGE + ","
                + CalendarContract.Events.CUSTOM_APP_URI + ","
                + CalendarContract.Events.UID_2445 + ","
                + CalendarContract.Events.SYNC_DATA1 + ","
                + CalendarContract.Events.SYNC_DATA2 + ","
                + CalendarContract.Events.SYNC_DATA3 + ","
                + CalendarContract.Events.SYNC_DATA4 + ","
                + CalendarContract.Events.SYNC_DATA5 + ","
                + CalendarContract.Events.SYNC_DATA6 + ","
                + CalendarContract.Events.SYNC_DATA7 + ","
                + CalendarContract.Events.SYNC_DATA8 + ","
                + CalendarContract.Events.SYNC_DATA9 + ","
                + CalendarContract.Events.SYNC_DATA10 + ","
                + Tables.EVENTS + "." + CalendarContract.Events.DELETED
                + " AS " + CalendarContract.Events.DELETED + ","
                + Tables.EVENTS + "." + CalendarContract.Events._SYNC_ID
                + " AS " + CalendarContract.Events._SYNC_ID + ","
                + Tables.EVENTS + "." + CalendarContract.Events.DIRTY
                + " AS " + CalendarContract.Events.DIRTY + ","
                + Tables.EVENTS + "." + Events.MUTATORS
                + " AS " + Events.MUTATORS + ","
                + CalendarContract.Events.LAST_SYNCED + ","
                + Tables.CALENDARS + "." + Calendars.ACCOUNT_NAME
                + " AS " + CalendarContract.Events.ACCOUNT_NAME + ","
                + Tables.CALENDARS + "." + Calendars.ACCOUNT_TYPE
                + " AS " + CalendarContract.Events.ACCOUNT_TYPE + ","
                + Calendars.CALENDAR_TIME_ZONE + ","
                + Calendars.CALENDAR_DISPLAY_NAME + ","
                + Calendars.CALENDAR_LOCATION + ","
                + Calendars.VISIBLE + ","
                + Calendars.CALENDAR_COLOR + ","
                + Calendars.CALENDAR_COLOR_KEY + ","
                + Calendars.CALENDAR_ACCESS_LEVEL + ","
                + Calendars.MAX_REMINDERS + ","
                + Calendars.ALLOWED_REMINDERS + ","
                + Calendars.ALLOWED_ATTENDEE_TYPES + ","
                + Calendars.ALLOWED_AVAILABILITY + ","
                + Calendars.CAN_ORGANIZER_RESPOND + ","
                + Calendars.CAN_MODIFY_TIME_ZONE + ","
                + Calendars.CAN_PARTIALLY_UPDATE + ","
                + Calendars.CAL_SYNC1 + ","
                + Calendars.CAL_SYNC2 + ","
                + Calendars.CAL_SYNC3 + ","
                + Calendars.CAL_SYNC4 + ","
                + Calendars.CAL_SYNC5 + ","
                + Calendars.CAL_SYNC6 + ","
                + Calendars.CAL_SYNC7 + ","
                + Calendars.CAL_SYNC8 + ","
                + Calendars.CAL_SYNC9 + ","
                + Calendars.CAL_SYNC10 + ","
                + Calendars.OWNER_ACCOUNT + ","
                + Calendars.SYNC_EVENTS  + ","
                + "ifnull(" + Events.EVENT_COLOR + "," + Calendars.CALENDAR_COLOR + ") AS "
                + Events.DISPLAY_COLOR
                + " FROM " + Tables.EVENTS + " JOIN " + Tables.CALENDARS
                + " ON (" + Tables.EVENTS + "." + Events.CALENDAR_ID
                + "=" + Tables.CALENDARS + "." + Calendars._ID
                + ")";

        db.execSQL("CREATE VIEW " + Views.EVENTS + " AS " + eventsSelect);
    }

    /**
     * Extracts the calendar email from a calendar feed url.
     * @param feed the calendar feed url
     * @return the calendar email that is in the feed url or null if it can't
     * find the email address.
     * TODO: this is duplicated in CalendarSyncAdapter; move to a library
     */
    public static String calendarEmailAddressFromFeedUrl(String feed) {
        // Example feed url:
        // https://www.google.com/calendar/feeds/foo%40gmail.com/private/full-noattendees
        String[] pathComponents = feed.split("/");
        if (pathComponents.length > 5 && "feeds".equals(pathComponents[4])) {
            try {
                return URLDecoder.decode(pathComponents[5], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "unable to url decode the email address in calendar " + feed);
                return null;
            }
        }

        Log.e(TAG, "unable to find the email address in calendar " + feed);
        return null;
    }

    /**
     * Get a "allcalendars" url from a "private/full" or "private/free-busy" url
     * @param url
     * @return the rewritten Url
     *
     * For example:
     *
     *      http://www.google.com/calendar/feeds/joe%40joe.com/private/full
     *      http://www.google.com/calendar/feeds/joe%40joe.com/private/free-busy
     *
     * will be rewriten into:
     *
     *      http://www.google.com/calendar/feeds/default/allcalendars/full/joe%40joe.com
     *      http://www.google.com/calendar/feeds/default/allcalendars/full/joe%40joe.com
     */
    private static String getAllCalendarsUrlFromEventsUrl(String url) {
        if (url == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Cannot get AllCalendars url from a NULL url");
            }
            return null;
        }
        if (url.contains("/private/full")) {
            return url.replace("/private/full", "").
                    replace("/calendar/feeds", "/calendar/feeds/default/allcalendars/full");
        }
        if (url.contains("/private/free-busy")) {
            return url.replace("/private/free-busy", "").
                    replace("/calendar/feeds", "/calendar/feeds/default/allcalendars/full");
        }
        // Just log as we dont recognize the provided Url
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Cannot get AllCalendars url from the following url: " + url);
        }
        return null;
    }

    /**
     * Get "selfUrl" from "events url"
     * @param url the Events url (either "private/full" or "private/free-busy"
     * @return the corresponding allcalendar url
     */
    private static String getSelfUrlFromEventsUrl(String url) {
        return rewriteUrlFromHttpToHttps(getAllCalendarsUrlFromEventsUrl(url));
    }

    /**
     * Get "editUrl" from "events url"
     * @param url the Events url (either "private/full" or "private/free-busy"
     * @return the corresponding allcalendar url
     */
    private static String getEditUrlFromEventsUrl(String url) {
        return rewriteUrlFromHttpToHttps(getAllCalendarsUrlFromEventsUrl(url));
    }

    /**
     * Rewrite the url from "http" to "https" scheme
     * @param url the url to rewrite
     * @return the rewritten URL
     */
    private static String rewriteUrlFromHttpToHttps(String url) {
        if (url == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Cannot rewrite a NULL url");
            }
            return null;
        }
        if (url.startsWith(SCHEMA_HTTPS)) {
            return url;
        }
        if (!url.startsWith(SCHEMA_HTTP)) {
            throw new IllegalArgumentException("invalid url parameter, unknown scheme: " + url);
        }
        return SCHEMA_HTTPS + url.substring(SCHEMA_HTTP.length());
    }

    /**
     * Duplicates an event and its associated tables (Attendees, Reminders, ExtendedProperties).
     * <p>
     * Does not create a duplicate if the Calendar's "canPartiallyUpdate" is 0 or the Event's
     * "dirty" is 1 (so we don't create more than one duplicate).
     *
     * @param id The _id of the event to duplicate.
     */
    protected void duplicateEvent(final long id) {
        final SQLiteDatabase db = getWritableDatabase();
        try {
            final long canPartiallyUpdate = DatabaseUtils.longForQuery(db, "SELECT "
                    + Calendars.CAN_PARTIALLY_UPDATE + " FROM " + Views.EVENTS
                    + " WHERE " + Events._ID + " = ?", new String[] {
                String.valueOf(id)
            });
            if (canPartiallyUpdate == 0) {
                return;
            }
        } catch (SQLiteDoneException e) {
            // b/11392862
            // If no results are returned, this will be thrown. This can happen if the Events View
            // has no rows for the provided id. This might happen for example if someone inserts a
            // reminder that refers to a non existent event id.
            // Return without doing anything because there is no event to duplicate.
            return;
        }

        db.execSQL("INSERT INTO " + CalendarDatabaseHelper.Tables.EVENTS
                + "  (" + LAST_SYNCED_EVENT_COLUMNS + ","
                +         Events.DIRTY + "," + Events.LAST_SYNCED + ")"
                + " SELECT " + LAST_SYNCED_EVENT_COLUMNS + ", 0, 1"
                + " FROM " + Tables.EVENTS
                + " WHERE "  + Events._ID + " = ? AND " + Events.DIRTY + " = ?",
                new Object[]{
                        id,
                        0, // Events.DIRTY
                });
        final long newId = DatabaseUtils.longForQuery(
                db, "SELECT CASE changes() WHEN 0 THEN -1 ELSE last_insert_rowid() END", null);
        if (newId < 0) {
            return;
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Duplicating event " + id + " into new event " + newId);
        }

        copyEventRelatedTables(db, newId, id);
    }

    /**
     * Makes a copy of the Attendees, Reminders, and ExtendedProperties rows associated with
     * a specific event.
     *
     * @param db The database.
     * @param newId The ID of the new event.
     * @param id The ID of the old event.
     */
    static void copyEventRelatedTables(SQLiteDatabase db, long newId, long id) {
        db.execSQL("INSERT INTO " + Tables.REMINDERS
                + " ( "  + CalendarContract.Reminders.EVENT_ID + ", "
                        + LAST_SYNCED_REMINDER_COLUMNS + ") "
                + "SELECT ?," + LAST_SYNCED_REMINDER_COLUMNS
                + " FROM " + Tables.REMINDERS
                + " WHERE " + CalendarContract.Reminders.EVENT_ID + " = ?",
                new Object[] {newId, id});
        db.execSQL("INSERT INTO "
                + Tables.ATTENDEES
                + " (" + CalendarContract.Attendees.EVENT_ID + ","
                        + LAST_SYNCED_ATTENDEE_COLUMNS + ") "
                + "SELECT ?," + LAST_SYNCED_ATTENDEE_COLUMNS + " FROM " + Tables.ATTENDEES
                + " WHERE " + CalendarContract.Attendees.EVENT_ID + " = ?",
                new Object[] {newId, id});
        db.execSQL("INSERT INTO " + Tables.EXTENDED_PROPERTIES
                + " (" + CalendarContract.ExtendedProperties.EVENT_ID + ","
                + LAST_SYNCED_EXTENDED_PROPERTY_COLUMNS + ") "
                + "SELECT ?, " + LAST_SYNCED_EXTENDED_PROPERTY_COLUMNS
                + " FROM " + Tables.EXTENDED_PROPERTIES
                + " WHERE " + CalendarContract.ExtendedProperties.EVENT_ID + " = ?",
                new Object[]{newId, id});
    }

    protected void removeDuplicateEvent(final long id) {
        final SQLiteDatabase db = getWritableDatabase();
        final Cursor cursor = db.rawQuery("SELECT " + Events._ID + " FROM " + Tables.EVENTS
                + " WHERE " + Events._SYNC_ID
                + " = (SELECT " + Events._SYNC_ID
                + " FROM " + Tables.EVENTS
                + " WHERE " + Events._ID + " = ?) "
                + "AND " + Events.LAST_SYNCED + " = ?",
                new String[]{
                        String.valueOf(id),
                        "1", // Events.LAST_SYNCED
                });
        try {
            // there should only be at most one but this can't hurt
            if (cursor.moveToNext()) {
                final long dupId = cursor.getLong(0);

                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Removing duplicate event " + dupId + " of original event " + id);
                }
                // triggers will clean up related tables.
                db.execSQL("DELETE FROM Events WHERE " + Events._ID + " = ?", new Object[]{dupId});
            }
        } finally {
          cursor.close();
        }
    }
}
