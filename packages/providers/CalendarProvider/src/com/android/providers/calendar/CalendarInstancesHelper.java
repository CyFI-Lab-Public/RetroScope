/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.Duration;
import com.android.calendarcommon2.EventRecurrence;
import com.android.calendarcommon2.RecurrenceProcessor;
import com.android.calendarcommon2.RecurrenceSet;
import com.android.providers.calendar.CalendarDatabaseHelper.Tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Debug;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class CalendarInstancesHelper {
    public static final class EventInstancesMap extends
            HashMap<String, CalendarInstancesHelper.InstancesList> {
        public void add(String syncIdKey, ContentValues values) {
            CalendarInstancesHelper.InstancesList instances = get(syncIdKey);
            if (instances == null) {
                instances = new CalendarInstancesHelper.InstancesList();
                put(syncIdKey, instances);
            }
            instances.add(values);
        }
    }

    public static final class InstancesList extends ArrayList<ContentValues> {
    }

    private static final String TAG = "CalInstances";
    private CalendarDatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private MetaData mMetaData;
    private CalendarCache mCalendarCache;

    private static final String SQL_WHERE_GET_EVENTS_ENTRIES =
            "((" + Events.DTSTART + " <= ? AND "
                    + "(" + Events.LAST_DATE + " IS NULL OR " + Events.LAST_DATE + " >= ?)) OR "
            + "(" + Events.ORIGINAL_INSTANCE_TIME + " IS NOT NULL AND "
                    + Events.ORIGINAL_INSTANCE_TIME
                    + " <= ? AND " + Events.ORIGINAL_INSTANCE_TIME + " >= ?)) AND "
            + "(" + Calendars.SYNC_EVENTS + " != ?) AND "
            + "(" + Events.LAST_SYNCED + " = ?)";

    /**
     * Determines the set of Events where the _id matches the first query argument, or the
     * originalId matches the second argument.  Returns the _id field from the set of
     * Instances whose event_id field matches one of those events.
     */
    private static final String SQL_WHERE_ID_FROM_INSTANCES_NOT_SYNCED =
            Instances._ID + " IN " +
            "(SELECT " + Tables.INSTANCES + "." + Instances._ID + " as _id" +
            " FROM " + Tables.INSTANCES +
            " INNER JOIN " + Tables.EVENTS +
            " ON (" +
            Tables.EVENTS + "." + Events._ID + "=" + Tables.INSTANCES + "." + Instances.EVENT_ID +
            ")" +
            " WHERE " + Tables.EVENTS + "." + Events._ID + "=? OR " +
                    Tables.EVENTS + "." + Events.ORIGINAL_ID + "=?)";

    /**
     * Determines the set of Events where the _sync_id matches the first query argument, or the
     * originalSyncId matches the second argument.  Returns the _id field from the set of
     * Instances whose event_id field matches one of those events.
     */
    private static final String SQL_WHERE_ID_FROM_INSTANCES_SYNCED =
            Instances._ID + " IN " +
            "(SELECT " + Tables.INSTANCES + "." + Instances._ID + " as _id" +
            " FROM " + Tables.INSTANCES +
            " INNER JOIN " + Tables.EVENTS +
            " ON (" +
            Tables.EVENTS + "." + Events._ID + "=" + Tables.INSTANCES + "." + Instances.EVENT_ID +
            ")" +
            " WHERE " + Tables.EVENTS + "." + Events._SYNC_ID + "=?" + " OR " +
                    Tables.EVENTS + "." + Events.ORIGINAL_SYNC_ID + "=?)";

    private static final String[] EXPAND_COLUMNS = new String[] {
            Events._ID,
            Events._SYNC_ID,
            Events.STATUS,
            Events.DTSTART,
            Events.DTEND,
            Events.EVENT_TIMEZONE,
            Events.RRULE,
            Events.RDATE,
            Events.EXRULE,
            Events.EXDATE,
            Events.DURATION,
            Events.ALL_DAY,
            Events.ORIGINAL_SYNC_ID,
            Events.ORIGINAL_INSTANCE_TIME,
            Events.CALENDAR_ID,
            Events.DELETED
    };

    // To determine if a recurrence exception originally overlapped the
    // window, we need to assume a maximum duration, since we only know
    // the original start time.
    private static final int MAX_ASSUMED_DURATION = 7 * 24 * 60 * 60 * 1000;

    public CalendarInstancesHelper(CalendarDatabaseHelper calendarDbHelper, MetaData metaData) {
        mDbHelper = calendarDbHelper;
        mDb = mDbHelper.getWritableDatabase();
        mMetaData = metaData;
        mCalendarCache = new CalendarCache(mDbHelper);
    }

    /**
     * Extract the value from the specifed row and column of the Events table.
     *
     * @param db The database to access.
     * @param rowId The Event's _id.
     * @param columnName The name of the column to access.
     * @return The value in string form.
     */
    private static String getEventValue(SQLiteDatabase db, long rowId, String columnName) {
        String where = "SELECT " + columnName + " FROM " + Tables.EVENTS +
            " WHERE " + Events._ID + "=?";
        return DatabaseUtils.stringForQuery(db, where,
                new String[] { String.valueOf(rowId) });
    }

    /**
     * Perform instance expansion on the given entries.
     *
     * @param begin Window start (ms).
     * @param end Window end (ms).
     * @param localTimezone
     * @param entries The entries to process.
     */
    protected void performInstanceExpansion(long begin, long end, String localTimezone,
            Cursor entries) {
        // TODO: this only knows how to work with events that have been synced with the server
        RecurrenceProcessor rp = new RecurrenceProcessor();

        // Key into the instance values to hold the original event concatenated
        // with calendar id.
        final String ORIGINAL_EVENT_AND_CALENDAR = "ORIGINAL_EVENT_AND_CALENDAR";

        int statusColumn = entries.getColumnIndex(Events.STATUS);
        int dtstartColumn = entries.getColumnIndex(Events.DTSTART);
        int dtendColumn = entries.getColumnIndex(Events.DTEND);
        int eventTimezoneColumn = entries.getColumnIndex(Events.EVENT_TIMEZONE);
        int durationColumn = entries.getColumnIndex(Events.DURATION);
        int rruleColumn = entries.getColumnIndex(Events.RRULE);
        int rdateColumn = entries.getColumnIndex(Events.RDATE);
        int exruleColumn = entries.getColumnIndex(Events.EXRULE);
        int exdateColumn = entries.getColumnIndex(Events.EXDATE);
        int allDayColumn = entries.getColumnIndex(Events.ALL_DAY);
        int idColumn = entries.getColumnIndex(Events._ID);
        int syncIdColumn = entries.getColumnIndex(Events._SYNC_ID);
        int originalEventColumn = entries.getColumnIndex(Events.ORIGINAL_SYNC_ID);
        int originalInstanceTimeColumn = entries.getColumnIndex(Events.ORIGINAL_INSTANCE_TIME);
        int calendarIdColumn = entries.getColumnIndex(Events.CALENDAR_ID);
        int deletedColumn = entries.getColumnIndex(Events.DELETED);

        ContentValues initialValues;
        CalendarInstancesHelper.EventInstancesMap instancesMap =
            new CalendarInstancesHelper.EventInstancesMap();

        Duration duration = new Duration();
        Time eventTime = new Time();

        // Invariant: entries contains all events that affect the current
        // window.  It consists of:
        // a) Individual events that fall in the window.  These will be
        //    displayed.
        // b) Recurrences that included the window.  These will be displayed
        //    if not canceled.
        // c) Recurrence exceptions that fall in the window.  These will be
        //    displayed if not cancellations.
        // d) Recurrence exceptions that modify an instance inside the
        //    window (subject to 1 week assumption above), but are outside
        //    the window.  These will not be displayed.  Cases c and d are
        //    distinguished by the start / end time.

        while (entries.moveToNext()) {
            try {
                initialValues = null;

                boolean allDay = entries.getInt(allDayColumn) != 0;

                String eventTimezone = entries.getString(eventTimezoneColumn);
                if (allDay || TextUtils.isEmpty(eventTimezone)) {
                    // in the events table, allDay events start at midnight.
                    // this forces them to stay at midnight for all day events
                    // TODO: check that this actually does the right thing.
                    eventTimezone = Time.TIMEZONE_UTC;
                }

                long dtstartMillis = entries.getLong(dtstartColumn);
                Long eventId = Long.valueOf(entries.getLong(idColumn));

                String durationStr = entries.getString(durationColumn);
                if (durationStr != null) {
                    try {
                        duration.parse(durationStr);
                    }
                    catch (DateException e) {
                        if (Log.isLoggable(CalendarProvider2.TAG, Log.ERROR)) {
                            Log.w(CalendarProvider2.TAG, "error parsing duration for event "
                                    + eventId + "'" + durationStr + "'", e);
                        }
                        duration.sign = 1;
                        duration.weeks = 0;
                        duration.days = 0;
                        duration.hours = 0;
                        duration.minutes = 0;
                        duration.seconds = 0;
                        durationStr = "+P0S";
                    }
                }

                String syncId = entries.getString(syncIdColumn);
                String originalEvent = entries.getString(originalEventColumn);

                long originalInstanceTimeMillis = -1;
                if (!entries.isNull(originalInstanceTimeColumn)) {
                    originalInstanceTimeMillis= entries.getLong(originalInstanceTimeColumn);
                }
                int status = entries.getInt(statusColumn);
                boolean deleted = (entries.getInt(deletedColumn) != 0);

                String rruleStr = entries.getString(rruleColumn);
                String rdateStr = entries.getString(rdateColumn);
                String exruleStr = entries.getString(exruleColumn);
                String exdateStr = entries.getString(exdateColumn);
                long calendarId = entries.getLong(calendarIdColumn);
                // key into instancesMap
                String syncIdKey = CalendarInstancesHelper.getSyncIdKey(syncId, calendarId);

                RecurrenceSet recur = null;
                try {
                    recur = new RecurrenceSet(rruleStr, rdateStr, exruleStr, exdateStr);
                } catch (EventRecurrence.InvalidFormatException e) {
                    if (Log.isLoggable(CalendarProvider2.TAG, Log.ERROR)) {
                        Log.w(CalendarProvider2.TAG, "Could not parse RRULE recurrence string: "
                                + rruleStr, e);
                    }
                    continue;
                }

                if (null != recur && recur.hasRecurrence()) {
                    // the event is repeating

                    if (status == Events.STATUS_CANCELED) {
                        // should not happen!
                        if (Log.isLoggable(CalendarProvider2.TAG, Log.ERROR)) {
                            Log.e(CalendarProvider2.TAG, "Found canceled recurring event in "
                                    + "Events table.  Ignoring.");
                        }
                        continue;
                    }
                    if (deleted) {
                        if (Log.isLoggable(CalendarProvider2.TAG, Log.DEBUG)) {
                            Log.d(CalendarProvider2.TAG, "Found deleted recurring event in "
                                    + "Events table.  Ignoring.");
                        }
                        continue;
                    }

                    // need to parse the event into a local calendar.
                    eventTime.timezone = eventTimezone;
                    eventTime.set(dtstartMillis);
                    eventTime.allDay = allDay;

                    if (durationStr == null) {
                        // should not happen.
                        if (Log.isLoggable(CalendarProvider2.TAG, Log.ERROR)) {
                            Log.e(CalendarProvider2.TAG, "Repeating event has no duration -- "
                                    + "should not happen.");
                        }
                        if (allDay) {
                            // set to one day.
                            duration.sign = 1;
                            duration.weeks = 0;
                            duration.days = 1;
                            duration.hours = 0;
                            duration.minutes = 0;
                            duration.seconds = 0;
                            durationStr = "+P1D";
                        } else {
                            // compute the duration from dtend, if we can.
                            // otherwise, use 0s.
                            duration.sign = 1;
                            duration.weeks = 0;
                            duration.days = 0;
                            duration.hours = 0;
                            duration.minutes = 0;
                            if (!entries.isNull(dtendColumn)) {
                                long dtendMillis = entries.getLong(dtendColumn);
                                duration.seconds = (int) ((dtendMillis - dtstartMillis) / 1000);
                                durationStr = "+P" + duration.seconds + "S";
                            } else {
                                duration.seconds = 0;
                                durationStr = "+P0S";
                            }
                        }
                    }

                    long[] dates;
                    dates = rp.expand(eventTime, recur, begin, end);

                    // Initialize the "eventTime" timezone outside the loop.
                    // This is used in computeTimezoneDependentFields().
                    if (allDay) {
                        eventTime.timezone = Time.TIMEZONE_UTC;
                    } else {
                        eventTime.timezone = localTimezone;
                    }

                    long durationMillis = duration.getMillis();
                    for (long date : dates) {
                        initialValues = new ContentValues();
                        initialValues.put(Instances.EVENT_ID, eventId);

                        initialValues.put(Instances.BEGIN, date);
                        long dtendMillis = date + durationMillis;
                        initialValues.put(Instances.END, dtendMillis);

                        CalendarInstancesHelper.computeTimezoneDependentFields(date, dtendMillis,
                                eventTime, initialValues);
                        instancesMap.add(syncIdKey, initialValues);
                    }
                } else {
                    // the event is not repeating
                    initialValues = new ContentValues();

                    // if this event has an "original" field, then record
                    // that we need to cancel the original event (we can't
                    // do that here because the order of this loop isn't
                    // defined)
                    if (originalEvent != null && originalInstanceTimeMillis != -1) {
                        // The ORIGINAL_EVENT_AND_CALENDAR holds the
                        // calendar id concatenated with the ORIGINAL_EVENT to form
                        // a unique key, matching the keys for instancesMap.
                        initialValues.put(ORIGINAL_EVENT_AND_CALENDAR,
                                CalendarInstancesHelper.getSyncIdKey(originalEvent, calendarId));
                        initialValues.put(Events.ORIGINAL_INSTANCE_TIME,
                                originalInstanceTimeMillis);
                        initialValues.put(Events.STATUS, status);
                    }

                    long dtendMillis = dtstartMillis;
                    if (durationStr == null) {
                        if (!entries.isNull(dtendColumn)) {
                            dtendMillis = entries.getLong(dtendColumn);
                        }
                    } else {
                        dtendMillis = duration.addTo(dtstartMillis);
                    }

                    // this non-recurring event might be a recurrence exception that doesn't
                    // actually fall within our expansion window, but instead was selected
                    // so we can correctly cancel expanded recurrence instances below.  do not
                    // add events to the instances map if they don't actually fall within our
                    // expansion window.
                    if ((dtendMillis < begin) || (dtstartMillis > end)) {
                        if (originalEvent != null && originalInstanceTimeMillis != -1) {
                            initialValues.put(Events.STATUS, Events.STATUS_CANCELED);
                        } else {
                            if (Log.isLoggable(CalendarProvider2.TAG, Log.ERROR)) {
                                Log.w(CalendarProvider2.TAG, "Unexpected event outside window: "
                                        + syncId);
                            }
                            continue;
                        }
                    }

                    initialValues.put(Instances.EVENT_ID, eventId);

                    initialValues.put(Instances.BEGIN, dtstartMillis);
                    initialValues.put(Instances.END, dtendMillis);

                    // we temporarily store the DELETED status (will be cleaned later)
                    initialValues.put(Events.DELETED, deleted);

                    if (allDay) {
                        eventTime.timezone = Time.TIMEZONE_UTC;
                    } else {
                        eventTime.timezone = localTimezone;
                    }
                    CalendarInstancesHelper.computeTimezoneDependentFields(dtstartMillis,
                            dtendMillis, eventTime, initialValues);

                    instancesMap.add(syncIdKey, initialValues);
                }
            } catch (DateException e) {
                if (Log.isLoggable(CalendarProvider2.TAG, Log.ERROR)) {
                    Log.w(CalendarProvider2.TAG, "RecurrenceProcessor error ", e);
                }
            } catch (TimeFormatException e) {
                if (Log.isLoggable(CalendarProvider2.TAG, Log.ERROR)) {
                    Log.w(CalendarProvider2.TAG, "RecurrenceProcessor error ", e);
                }
            }
        }

        // Invariant: instancesMap contains all instances that affect the
        // window, indexed by original sync id concatenated with calendar id.
        // It consists of:
        // a) Individual events that fall in the window.  They have:
        //   EVENT_ID, BEGIN, END
        // b) Instances of recurrences that fall in the window.  They may
        //   be subject to exceptions.  They have:
        //   EVENT_ID, BEGIN, END
        // c) Exceptions that fall in the window.  They have:
        //   ORIGINAL_EVENT_AND_CALENDAR, ORIGINAL_INSTANCE_TIME, STATUS (since they can
        //   be a modification or cancellation), EVENT_ID, BEGIN, END
        // d) Recurrence exceptions that modify an instance inside the
        //   window but fall outside the window.  They have:
        //   ORIGINAL_EVENT_AND_CALENDAR, ORIGINAL_INSTANCE_TIME, STATUS =
        //   STATUS_CANCELED, EVENT_ID, BEGIN, END

        // First, delete the original instances corresponding to recurrence
        // exceptions.  We do this by iterating over the list and for each
        // recurrence exception, we search the list for an instance with a
        // matching "original instance time".  If we find such an instance,
        // we remove it from the list.  If we don't find such an instance
        // then we cancel the recurrence exception.
        Set<String> keys = instancesMap.keySet();
        for (String syncIdKey : keys) {
            CalendarInstancesHelper.InstancesList list = instancesMap.get(syncIdKey);
            for (ContentValues values : list) {

                // If this instance is not a recurrence exception, then
                // skip it.
                if (!values.containsKey(ORIGINAL_EVENT_AND_CALENDAR)) {
                    continue;
                }

                String originalEventPlusCalendar = values.getAsString(ORIGINAL_EVENT_AND_CALENDAR);
                long originalTime = values.getAsLong(Events.ORIGINAL_INSTANCE_TIME);
                CalendarInstancesHelper.InstancesList originalList = instancesMap
                        .get(originalEventPlusCalendar);
                if (originalList == null) {
                    // The original recurrence is not present, so don't try canceling it.
                    continue;
                }

                // Search the original event for a matching original
                // instance time.  If there is a matching one, then remove
                // the original one.  We do this both for exceptions that
                // change the original instance as well as for exceptions
                // that delete the original instance.
                for (int num = originalList.size() - 1; num >= 0; num--) {
                    ContentValues originalValues = originalList.get(num);
                    long beginTime = originalValues.getAsLong(Instances.BEGIN);
                    if (beginTime == originalTime) {
                        // We found the original instance, so remove it.
                        originalList.remove(num);
                    }
                }
            }
        }

        // Invariant: instancesMap contains filtered instances.
        // It consists of:
        // a) Individual events that fall in the window.
        // b) Instances of recurrences that fall in the window and have not
        //   been subject to exceptions.
        // c) Exceptions that fall in the window.  They will have
        //   STATUS_CANCELED if they are cancellations.
        // d) Recurrence exceptions that modify an instance inside the
        //   window but fall outside the window.  These are STATUS_CANCELED.

        // Now do the inserts.  Since the db lock is held when this method is executed,
        // this will be done in a transaction.
        // NOTE: if there is lock contention (e.g., a sync is trying to merge into the db
        // while the calendar app is trying to query the db (expanding instances)), we will
        // not be "polite" and yield the lock until we're done.  This will favor local query
        // operations over sync/write operations.
        for (String syncIdKey : keys) {
            CalendarInstancesHelper.InstancesList list = instancesMap.get(syncIdKey);
            for (ContentValues values : list) {

                // If this instance was cancelled or deleted then don't create a new
                // instance.
                Integer status = values.getAsInteger(Events.STATUS);
                boolean deleted = values.containsKey(Events.DELETED) ?
                        values.getAsBoolean(Events.DELETED) : false;
                if ((status != null && status == Events.STATUS_CANCELED) || deleted) {
                    continue;
                }

                // We remove this useless key (not valid in the context of Instances table)
                values.remove(Events.DELETED);

                // Remove these fields before inserting a new instance
                values.remove(ORIGINAL_EVENT_AND_CALENDAR);
                values.remove(Events.ORIGINAL_INSTANCE_TIME);
                values.remove(Events.STATUS);

                mDbHelper.instancesReplace(values);
            }
        }
    }

    /**
     * Make instances for the given range.
     */
    protected void expandInstanceRangeLocked(long begin, long end, String localTimezone) {

        if (CalendarProvider2.PROFILE) {
            Debug.startMethodTracing("expandInstanceRangeLocked");
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Expanding events between " + begin + " and " + end);
        }

        Cursor entries = getEntries(begin, end);
        try {
            performInstanceExpansion(begin, end, localTimezone, entries);
        } finally {
            if (entries != null) {
                entries.close();
            }
        }
        if (CalendarProvider2.PROFILE) {
            Debug.stopMethodTracing();
        }
    }

    /**
     * Get all entries affecting the given window.
     *
     * @param begin Window start (ms).
     * @param end Window end (ms).
     * @return Cursor for the entries; caller must close it.
     */
    private Cursor getEntries(long begin, long end) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(CalendarDatabaseHelper.Views.EVENTS);
        qb.setProjectionMap(CalendarProvider2.sEventsProjectionMap);

        String beginString = String.valueOf(begin);
        String endString = String.valueOf(end);

        // grab recurrence exceptions that fall outside our expansion window but
        // modify
        // recurrences that do fall within our window. we won't insert these
        // into the output
        // set of instances, but instead will just add them to our cancellations
        // list, so we
        // can cancel the correct recurrence expansion instances.
        // we don't have originalInstanceDuration or end time. for now, assume
        // the original
        // instance lasts no longer than 1 week.
        // also filter with syncable state (we dont want the entries from a non
        // syncable account)
        // also filter with last_synced=0 so we don't expand events that were
        // dup'ed for partial updates.
        // TODO: compute the originalInstanceEndTime or get this from the
        // server.
        qb.appendWhere(SQL_WHERE_GET_EVENTS_ENTRIES);
        String selectionArgs[] = new String[] {
                endString,
                beginString,
                endString,
                String.valueOf(begin - MAX_ASSUMED_DURATION),
                "0", // Calendars.SYNC_EVENTS
                "0", // Events.LAST_SYNCED
        };
        Cursor c = qb.query(mDb, EXPAND_COLUMNS, null /* selection */, selectionArgs,
                null /* groupBy */, null /* having */, null /* sortOrder */);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Instance expansion:  got " + c.getCount() + " entries");
        }
        return c;
    }

    /**
     * Updates the instances table when an event is added or updated.
     *
     * @param values The new values of the event.
     * @param rowId The database row id of the event.
     * @param newEvent true if the event is new.
     * @param db The database
     */
    public void updateInstancesLocked(ContentValues values, long rowId, boolean newEvent,
            SQLiteDatabase db) {
        /*
         * This may be a recurring event (has an RRULE or RDATE), an exception to a recurring
         * event (has ORIGINAL_ID or ORIGINAL_SYNC_ID), or a regular event.  Recurring events
         * and exceptions require additional handling.
         *
         * If this is not a new event, it may already have entries in Instances, so we want
         * to delete those before we do any additional work.
         */

        // If there are no expanded Instances, then return.
        MetaData.Fields fields = mMetaData.getFieldsLocked();
        if (fields.maxInstance == 0) {
            return;
        }

        Long dtstartMillis = values.getAsLong(Events.DTSTART);
        if (dtstartMillis == null) {
            if (newEvent) {
                // must be present for a new event.
                throw new RuntimeException("DTSTART missing.");
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Missing DTSTART.  No need to update instance.");
            }
            return;
        }

        if (!newEvent) {
            // Want to do this for regular event, recurrence, or exception.
            // For recurrence or exception, more deletion may happen below if we
            // do an instance expansion. This deletion will suffice if the
            // exception
            // is moved outside the window, for instance.
            db.delete(Tables.INSTANCES, Instances.EVENT_ID + "=?", new String[] {
                String.valueOf(rowId)
            });
        }

        String rrule = values.getAsString(Events.RRULE);
        String rdate = values.getAsString(Events.RDATE);
        String originalId = values.getAsString(Events.ORIGINAL_ID);
        String originalSyncId = values.getAsString(Events.ORIGINAL_SYNC_ID);
        if (CalendarProvider2.isRecurrenceEvent(rrule, rdate, originalId, originalSyncId)) {
            Long lastDateMillis = values.getAsLong(Events.LAST_DATE);
            Long originalInstanceTime = values.getAsLong(Events.ORIGINAL_INSTANCE_TIME);

            // The recurrence or exception needs to be (re-)expanded if:
            // a) Exception or recurrence that falls inside window
            boolean insideWindow = dtstartMillis <= fields.maxInstance
                    && (lastDateMillis == null || lastDateMillis >= fields.minInstance);
            // b) Exception that affects instance inside window
            // These conditions match the query in getEntries
            // See getEntries comment for explanation of subtracting 1 week.
            boolean affectsWindow = originalInstanceTime != null
                    && originalInstanceTime <= fields.maxInstance
                    && originalInstanceTime >= fields.minInstance - MAX_ASSUMED_DURATION;
            if (CalendarProvider2.DEBUG_INSTANCES) {
                Log.d(TAG + "-i", "Recurrence: inside=" + insideWindow +
                        ", affects=" + affectsWindow);
            }
            if (insideWindow || affectsWindow) {
                updateRecurrenceInstancesLocked(values, rowId, db);
            }
            // TODO: an exception creation or update could be optimized by
            // updating just the affected instances, instead of regenerating
            // the recurrence.
            return;
        }

        Long dtendMillis = values.getAsLong(Events.DTEND);
        if (dtendMillis == null) {
            dtendMillis = dtstartMillis;
        }

        // if the event is in the expanded range, insert
        // into the instances table.
        // TODO: deal with durations. currently, durations are only used in
        // recurrences.

        if (dtstartMillis <= fields.maxInstance && dtendMillis >= fields.minInstance) {
            ContentValues instanceValues = new ContentValues();
            instanceValues.put(Instances.EVENT_ID, rowId);
            instanceValues.put(Instances.BEGIN, dtstartMillis);
            instanceValues.put(Instances.END, dtendMillis);

            boolean allDay = false;
            Integer allDayInteger = values.getAsInteger(Events.ALL_DAY);
            if (allDayInteger != null) {
                allDay = allDayInteger != 0;
            }

            // Update the timezone-dependent fields.
            Time local = new Time();
            if (allDay) {
                local.timezone = Time.TIMEZONE_UTC;
            } else {
                local.timezone = fields.timezone;
            }

            CalendarInstancesHelper.computeTimezoneDependentFields(dtstartMillis, dtendMillis,
                    local, instanceValues);
            mDbHelper.instancesInsert(instanceValues);
        }
    }

    /**
     * Do incremental Instances update of a recurrence or recurrence exception.
     * This method does performInstanceExpansion on just the modified
     * recurrence, to avoid the overhead of recomputing the entire instance
     * table.
     *
     * @param values The new values of the event.
     * @param rowId The database row id of the event.
     * @param db The database
     */
    private void updateRecurrenceInstancesLocked(ContentValues values, long rowId,
            SQLiteDatabase db) {
        /*
         *  There are two categories of event that "rowId" may refer to:
         *  (1) Recurrence event.
         *  (2) Exception to recurrence event.  Has non-empty originalId (if it originated
         *      locally), originalSyncId (if it originated from the server), or both (if
         *      it's fully synchronized).
         *
         * Exceptions may arrive from the server before the recurrence event, which means:
         *  - We could find an originalSyncId but a lookup on originalSyncId could fail (in
         *    which case we can just ignore the exception for now).
         *  - There may be a brief period between the time we receive a recurrence and the
         *    time we set originalId in related exceptions where originalSyncId is the only
         *    way to find exceptions for a recurrence.  Thus, an empty originalId field may
         *    not be used to decide if an event is an exception.
         */

        MetaData.Fields fields = mMetaData.getFieldsLocked();
        String instancesTimezone = mCalendarCache.readTimezoneInstances();

        // Get the originalSyncId.  If it's not in "values", check the database.
        String originalSyncId = values.getAsString(Events.ORIGINAL_SYNC_ID);
        if (originalSyncId == null) {
            originalSyncId = getEventValue(db, rowId, Events.ORIGINAL_SYNC_ID);
        }

        String recurrenceSyncId;
        if (originalSyncId != null) {
            // This event is an exception; set recurrenceSyncId to the original.
            recurrenceSyncId = originalSyncId;
        } else {
            // This could be a recurrence or an exception.  If it has been synced with the
            // server we can get the _sync_id and know for certain that it's a recurrence.
            // If not, we'll deal with it below.
            recurrenceSyncId = values.getAsString(Events._SYNC_ID);
            if (recurrenceSyncId == null) {
                // Not in "values", check the database.
                recurrenceSyncId = getEventValue(db, rowId, Events._SYNC_ID);
            }
        }

        // Clear out old instances
        int delCount;
        if (recurrenceSyncId == null) {
            // We're creating or updating a recurrence or exception that hasn't been to the
            // server.  If this is a recurrence event, the event ID is simply the rowId.  If
            // it's an exception, we will find the value in the originalId field.
            String originalId = values.getAsString(Events.ORIGINAL_ID);
            if (originalId == null) {
                // Not in "values", check the database.
                originalId = getEventValue(db, rowId, Events.ORIGINAL_ID);
            }
            String recurrenceId;
            if (originalId != null) {
                // This event is an exception; set recurrenceId to the original.
                recurrenceId = originalId;
            } else {
                // This event is a recurrence, so we just use the ID that was passed in.
                recurrenceId = String.valueOf(rowId);
            }

            // Delete Instances entries for this Event (_id == recurrenceId) and for exceptions
            // to this Event (originalId == recurrenceId).
            String where = SQL_WHERE_ID_FROM_INSTANCES_NOT_SYNCED;
            delCount = db.delete(Tables.INSTANCES, where, new String[] {
                    recurrenceId, recurrenceId
            });
        } else {
            // We're creating or updating a recurrence or exception that has been synced with
            // the server.  Delete Instances entries for this Event (_sync_id == recurrenceSyncId)
            // and for exceptions to this Event (originalSyncId == recurrenceSyncId).
            String where = SQL_WHERE_ID_FROM_INSTANCES_SYNCED;
            delCount = db.delete(Tables.INSTANCES, where, new String[] {
                    recurrenceSyncId, recurrenceSyncId
            });
        }

        //Log.d(TAG, "Recurrence: deleted " + delCount + " instances");
        //dumpInstancesTable(db);

        // Now do instance expansion
        // TODO: passing "rowId" is wrong if this is an exception - need originalId then
        Cursor entries = getRelevantRecurrenceEntries(recurrenceSyncId, rowId);
        try {
            performInstanceExpansion(fields.minInstance, fields.maxInstance,
                    instancesTimezone, entries);
        } finally {
            if (entries != null) {
                entries.close();
            }
        }
    }

    /**
     * Determines the recurrence entries associated with a particular
     * recurrence. This set is the base recurrence and any exception. Normally
     * the entries are indicated by the sync id of the base recurrence (which is
     * the originalSyncId in the exceptions). However, a complication is that a
     * recurrence may not yet have a sync id. In that case, the recurrence is
     * specified by the rowId.
     *
     * @param recurrenceSyncId The sync id of the base recurrence, or null.
     * @param rowId The row id of the base recurrence.
     * @return the relevant entries.
     */
    private Cursor getRelevantRecurrenceEntries(String recurrenceSyncId, long rowId) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(CalendarDatabaseHelper.Views.EVENTS);
        qb.setProjectionMap(CalendarProvider2.sEventsProjectionMap);
        String selectionArgs[];
        if (recurrenceSyncId == null) {
            String where = CalendarProvider2.SQL_WHERE_ID;
            qb.appendWhere(where);
            selectionArgs = new String[] {
                String.valueOf(rowId)
            };
        } else {
            // don't expand events that were dup'ed for partial updates
            String where = "(" + Events._SYNC_ID + "=? OR " + Events.ORIGINAL_SYNC_ID + "=?) AND "
                    + Events.LAST_SYNCED + " = ?";
            qb.appendWhere(where);
            selectionArgs = new String[] {
                    recurrenceSyncId,
                    recurrenceSyncId,
                    "0", // Events.LAST_SYNCED
            };
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Retrieving events to expand: " + qb.toString());
        }

        return qb.query(mDb, EXPAND_COLUMNS, null /* selection */, selectionArgs,
                null /* groupBy */, null /* having */, null /* sortOrder */);
    }

    /**
     * Generates a unique key from the syncId and calendarId. The purpose of
     * this is to prevent collisions if two different calendars use the same
     * sync id. This can happen if a Google calendar is accessed by two
     * different accounts, or with Exchange, where ids are not unique between
     * calendars.
     *
     * @param syncId Id for the event
     * @param calendarId Id for the calendar
     * @return key
     */
    static String getSyncIdKey(String syncId, long calendarId) {
        return calendarId + ":" + syncId;
    }

    /**
     * Computes the timezone-dependent fields of an instance of an event and
     * updates the "values" map to contain those fields.
     *
     * @param begin the start time of the instance (in UTC milliseconds)
     * @param end the end time of the instance (in UTC milliseconds)
     * @param local a Time object with the timezone set to the local timezone
     * @param values a map that will contain the timezone-dependent fields
     */
    static void computeTimezoneDependentFields(long begin, long end,
            Time local, ContentValues values) {
        local.set(begin);
        int startDay = Time.getJulianDay(begin, local.gmtoff);
        int startMinute = local.hour * 60 + local.minute;

        local.set(end);
        int endDay = Time.getJulianDay(end, local.gmtoff);
        int endMinute = local.hour * 60 + local.minute;

        // Special case for midnight, which has endMinute == 0.  Change
        // that to +24 hours on the previous day to make everything simpler.
        // Exception: if start and end minute are both 0 on the same day,
        // then leave endMinute alone.
        if (endMinute == 0 && endDay > startDay) {
            endMinute = 24 * 60;
            endDay -= 1;
        }

        values.put(Instances.START_DAY, startDay);
        values.put(Instances.END_DAY, endDay);
        values.put(Instances.START_MINUTE, startMinute);
        values.put(Instances.END_MINUTE, endMinute);
    }

    /**
     * Dumps the contents of the Instances table to the log file.
     */
    private static void dumpInstancesTable(SQLiteDatabase db) {
        Cursor cursor = db.query(Tables.INSTANCES, null, null, null, null, null, null);
        DatabaseUtils.dumpCursor(cursor);
        if (cursor != null) {
            cursor.close();
        }
    }
}
