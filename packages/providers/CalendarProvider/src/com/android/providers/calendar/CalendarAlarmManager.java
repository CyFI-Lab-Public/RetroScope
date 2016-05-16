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

import com.android.providers.calendar.CalendarDatabaseHelper.Tables;
import com.android.providers.calendar.CalendarDatabaseHelper.Views;
import com.google.common.annotations.VisibleForTesting;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * We are using the CalendarAlertManager to be able to mock the AlarmManager as the AlarmManager
 * cannot be extended.
 *
 * CalendarAlertManager is delegating its calls to the real AlarmService.
 */
public class CalendarAlarmManager {
    protected static final String TAG = "CalendarAlarmManager";

    // SCHEDULE_ALARM_URI runs scheduleNextAlarm(false)
    // SCHEDULE_ALARM_REMOVE_URI runs scheduleNextAlarm(true)
    // TODO: use a service to schedule alarms rather than private URI
    /* package */static final String SCHEDULE_ALARM_PATH = "schedule_alarms";
    /* package */static final String SCHEDULE_ALARM_REMOVE_PATH = "schedule_alarms_remove";
    private static final String REMOVE_ALARM_VALUE = "removeAlarms";
    /* package */static final Uri SCHEDULE_ALARM_REMOVE_URI = Uri.withAppendedPath(
            CalendarContract.CONTENT_URI, SCHEDULE_ALARM_REMOVE_PATH);
    /* package */static final Uri SCHEDULE_ALARM_URI = Uri.withAppendedPath(
            CalendarContract.CONTENT_URI, SCHEDULE_ALARM_PATH);

    static final String INVALID_CALENDARALERTS_SELECTOR =
    "_id IN (SELECT ca." + CalendarAlerts._ID + " FROM "
            + Tables.CALENDAR_ALERTS + " AS ca"
            + " LEFT OUTER JOIN " + Tables.INSTANCES
            + " USING (" + Instances.EVENT_ID + ","
            + Instances.BEGIN + "," + Instances.END + ")"
            + " LEFT OUTER JOIN " + Tables.REMINDERS + " AS r ON"
            + " (ca." + CalendarAlerts.EVENT_ID + "=r." + Reminders.EVENT_ID
            + " AND ca." + CalendarAlerts.MINUTES + "=r." + Reminders.MINUTES + ")"
            + " LEFT OUTER JOIN " + Views.EVENTS + " AS e ON"
            + " (ca." + CalendarAlerts.EVENT_ID + "=e." + Events._ID + ")"
            + " WHERE " + Tables.INSTANCES + "." + Instances.BEGIN + " ISNULL"
            + "   OR ca." + CalendarAlerts.ALARM_TIME + "<?"
            + "   OR (r." + Reminders.MINUTES + " ISNULL"
            + "       AND ca." + CalendarAlerts.MINUTES + "<>0)"
            + "   OR e." + Calendars.VISIBLE + "=0)";

    /**
     * We search backward in time for event reminders that we may have missed
     * and schedule them if the event has not yet expired. The amount in the
     * past to search backwards is controlled by this constant. It should be at
     * least a few minutes to allow for an event that was recently created on
     * the web to make its way to the phone. Two hours might seem like overkill,
     * but it is useful in the case where the user just crossed into a new
     * timezone and might have just missed an alarm.
     */
    private static final long SCHEDULE_ALARM_SLACK = 2 * DateUtils.HOUR_IN_MILLIS;
    /**
     * Alarms older than this threshold will be deleted from the CalendarAlerts
     * table. This should be at least a day because if the timezone is wrong and
     * the user corrects it we might delete good alarms that appear to be old
     * because the device time was incorrectly in the future. This threshold
     * must also be larger than SCHEDULE_ALARM_SLACK. We add the
     * SCHEDULE_ALARM_SLACK to ensure this. To make it easier to find and debug
     * problems with missed reminders, set this to something greater than a day.
     */
    private static final long CLEAR_OLD_ALARM_THRESHOLD = 7 * DateUtils.DAY_IN_MILLIS
            + SCHEDULE_ALARM_SLACK;
    private static final String SCHEDULE_NEXT_ALARM_WAKE_LOCK = "ScheduleNextAlarmWakeLock";
    protected static final String ACTION_CHECK_NEXT_ALARM =
            "com.android.providers.calendar.intent.CalendarProvider2";
    static final int ALARM_CHECK_DELAY_MILLIS = 5000;

    /**
     * Used for tracking if the next alarm is already scheduled
     */
    @VisibleForTesting
    protected AtomicBoolean mNextAlarmCheckScheduled;
    /**
     * Used for synchronization
     */
    @VisibleForTesting
    protected Object mAlarmLock;
    /**
     * Used to keep the process from getting killed while scheduling alarms
     */
    private final WakeLock mScheduleNextAlarmWakeLock;

    @VisibleForTesting
    protected Context mContext;
    private AlarmManager mAlarmManager;

    public CalendarAlarmManager(Context context) {
        initializeWithContext(context);

        PowerManager powerManager = (PowerManager) mContext.getSystemService(
                Context.POWER_SERVICE);
        // Create a wake lock that will be used when we are actually
        // scheduling the next alarm
        mScheduleNextAlarmWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, SCHEDULE_NEXT_ALARM_WAKE_LOCK);
        // We want the Wake Lock to be reference counted (so that we dont
        // need to take care
        // about its reference counting)
        mScheduleNextAlarmWakeLock.setReferenceCounted(true);
    }

    protected void initializeWithContext(Context context) {
        mContext = context;
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mNextAlarmCheckScheduled = new AtomicBoolean(false);
        mAlarmLock = new Object();
    }

    void scheduleNextAlarm(boolean removeAlarms) {
        // We must always run the following when 'removeAlarms' is true.  Previously it
        // was possible to have a race condition on startup between TIME_CHANGED and
        // BOOT_COMPLETED broadcast actions.  This resulted in alarms being
        // missed (Bug 7221716) when the TIME_CHANGED broadcast ('removeAlarms' = false)
        // happened right before the BOOT_COMPLETED ('removeAlarms' = true), and the
        // BOOT_COMPLETED action was skipped since there was concurrent scheduling in progress.
        if (!mNextAlarmCheckScheduled.getAndSet(true) || removeAlarms) {
            if (Log.isLoggable(CalendarProvider2.TAG, Log.DEBUG)) {
                Log.d(CalendarProvider2.TAG, "Scheduling check of next Alarm");
            }
            Intent intent = new Intent(ACTION_CHECK_NEXT_ALARM);
            intent.putExtra(REMOVE_ALARM_VALUE, removeAlarms);
            PendingIntent pending = PendingIntent.getBroadcast(mContext, 0 /* ignored */, intent,
                    PendingIntent.FLAG_NO_CREATE);
            if (pending != null) {
                // Cancel any previous Alarm check requests
                cancel(pending);
            }
            pending = PendingIntent.getBroadcast(mContext, 0 /* ignored */, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            // Trigger the check in 5s from now
            long triggerAtTime = SystemClock.elapsedRealtime() + ALARM_CHECK_DELAY_MILLIS;
            set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pending);
        }
    }

    PowerManager.WakeLock getScheduleNextAlarmWakeLock() {
        return mScheduleNextAlarmWakeLock;
    }

    void acquireScheduleNextAlarmWakeLock() {
        getScheduleNextAlarmWakeLock().acquire();
    }

    void releaseScheduleNextAlarmWakeLock() {
        try {
            getScheduleNextAlarmWakeLock().release();
        } catch (RuntimeException e) {
            if (!e.getMessage().startsWith("WakeLock under-locked ")) {
              throw e;
            }
            Log.w(TAG, "WakeLock under-locked ignored.");
        }
    }

    void rescheduleMissedAlarms() {
        rescheduleMissedAlarms(mContext.getContentResolver());
    }

    /**
     * This method runs in a background thread and schedules an alarm for the
     * next calendar event, if necessary.
     *
     * @param db TODO
     */
    void runScheduleNextAlarm(boolean removeAlarms, CalendarProvider2 cp2) {
        SQLiteDatabase db = cp2.mDb;
        if (db == null) {
            return;
        }

        // Reset so that we can accept other schedules of next alarm
        mNextAlarmCheckScheduled.set(false);
        db.beginTransaction();
        try {
            if (removeAlarms) {
                removeScheduledAlarmsLocked(db);
            }
            scheduleNextAlarmLocked(db, cp2);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void scheduleNextAlarmCheck(long triggerTime) {
        Intent intent = new Intent(CalendarReceiver.SCHEDULE);
        intent.setClass(mContext, CalendarReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(
                mContext, 0, intent, PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            // Cancel any previous alarms that do the same thing.
            cancel(pending);
        }
        pending = PendingIntent.getBroadcast(
                mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        if (Log.isLoggable(CalendarProvider2.TAG, Log.DEBUG)) {
            Time time = new Time();
            time.set(triggerTime);
            String timeStr = time.format(" %a, %b %d, %Y %I:%M%P");
            Log.d(CalendarProvider2.TAG, "scheduleNextAlarmCheck at: " + triggerTime + timeStr);
        }

        set(AlarmManager.RTC_WAKEUP, triggerTime, pending);
    }

    /**
     * This method looks at the 24-hour window from now for any events that it
     * needs to schedule. This method runs within a database transaction. It
     * also runs in a background thread. The CalendarProvider2 keeps track of
     * which alarms it has already scheduled to avoid scheduling them more than
     * once and for debugging problems with alarms. It stores this knowledge in
     * a database table called CalendarAlerts which persists across reboots. But
     * the actual alarm list is in memory and disappears if the phone loses
     * power. To avoid missing an alarm, we clear the entries in the
     * CalendarAlerts table when we start up the CalendarProvider2. Scheduling
     * an alarm multiple times is not tragic -- we filter out the extra ones
     * when we receive them. But we still need to keep track of the scheduled
     * alarms. The main reason is that we need to prevent multiple notifications
     * for the same alarm (on the receive side) in case we accidentally schedule
     * the same alarm multiple times. We don't have visibility into the system's
     * alarm list so we can never know for sure if we have already scheduled an
     * alarm and it's better to err on scheduling an alarm twice rather than
     * missing an alarm. Another reason we keep track of scheduled alarms in a
     * database table is that it makes it easy to run an SQL query to find the
     * next reminder that we haven't scheduled.
     *
     * @param db the database
     * @param cp2 TODO
     */
    private void scheduleNextAlarmLocked(SQLiteDatabase db, CalendarProvider2 cp2) {
        Time time = new Time();

        final long currentMillis = System.currentTimeMillis();
        final long start = currentMillis - SCHEDULE_ALARM_SLACK;
        final long end = start + (24 * 60 * 60 * 1000);
        if (Log.isLoggable(CalendarProvider2.TAG, Log.DEBUG)) {
            time.set(start);
            String startTimeStr = time.format(" %a, %b %d, %Y %I:%M%P");
            Log.d(CalendarProvider2.TAG, "runScheduleNextAlarm() start search: " + startTimeStr);
        }

        // Delete rows in CalendarAlert where the corresponding Instance or
        // Reminder no longer exist.
        // Also clear old alarms but keep alarms around for a while to prevent
        // multiple alerts for the same reminder. The "clearUpToTime'
        // should be further in the past than the point in time where
        // we start searching for events (the "start" variable defined above).
        String selectArg[] = new String[] { Long.toString(
                currentMillis - CLEAR_OLD_ALARM_THRESHOLD) };

        int rowsDeleted = db.delete(
                CalendarAlerts.TABLE_NAME, INVALID_CALENDARALERTS_SELECTOR, selectArg);

        long nextAlarmTime = end;
        final ContentResolver resolver = mContext.getContentResolver();
        final long tmpAlarmTime = CalendarAlerts.findNextAlarmTime(resolver, currentMillis);
        if (tmpAlarmTime != -1 && tmpAlarmTime < nextAlarmTime) {
            nextAlarmTime = tmpAlarmTime;
        }

        // Extract events from the database sorted by alarm time. The
        // alarm times are computed from Instances.begin (whose units
        // are milliseconds) and Reminders.minutes (whose units are
        // minutes).
        //
        // Also, ignore events whose end time is already in the past.
        // Also, ignore events alarms that we have already scheduled.
        //
        // Note 1: we can add support for the case where Reminders.minutes
        // equals -1 to mean use Calendars.minutes by adding a UNION for
        // that case where the two halves restrict the WHERE clause on
        // Reminders.minutes != -1 and Reminders.minutes = 1, respectively.
        //
        // Note 2: we have to name "myAlarmTime" different from the
        // "alarmTime" column in CalendarAlerts because otherwise the
        // query won't find multiple alarms for the same event.
        //
        // The CAST is needed in the query because otherwise the expression
        // will be untyped and sqlite3's manifest typing will not convert the
        // string query parameter to an int in myAlarmtime>=?, so the comparison
        // will fail. This could be simplified if bug 2464440 is resolved.

        time.setToNow();
        time.normalize(false);
        long localOffset = time.gmtoff * 1000;

        String allDayOffset = " -(" + localOffset + ") ";
        String subQueryPrefix = "SELECT " + Instances.BEGIN;
        String subQuerySuffix = " -(" + Reminders.MINUTES + "*" + +DateUtils.MINUTE_IN_MILLIS + ")"
                + " AS myAlarmTime" + "," + Tables.INSTANCES + "." + Instances.EVENT_ID
                + " AS eventId" + "," + Instances.BEGIN + "," + Instances.END + ","
                + Instances.TITLE + "," + Instances.ALL_DAY + "," + Reminders.METHOD + ","
                + Reminders.MINUTES + " FROM " + Tables.INSTANCES + " INNER JOIN " + Views.EVENTS
                + " ON (" + Views.EVENTS + "." + Events._ID + "=" + Tables.INSTANCES + "."
                + Instances.EVENT_ID + ")" + " INNER JOIN " + Tables.REMINDERS + " ON ("
                + Tables.INSTANCES + "." + Instances.EVENT_ID + "=" + Tables.REMINDERS + "."
                + Reminders.EVENT_ID + ")" + " WHERE " + Calendars.VISIBLE + "=1"
                + " AND myAlarmTime>=CAST(? AS INT)" + " AND myAlarmTime<=CAST(? AS INT)" + " AND "
                + Instances.END + ">=?" + " AND " + Reminders.METHOD + "=" + Reminders.METHOD_ALERT;

        // we query separately for all day events to convert to local time from
        // UTC
        // we need to /subtract/ the offset to get the correct resulting local
        // time
        String allDayQuery = subQueryPrefix + allDayOffset + subQuerySuffix + " AND "
                + Instances.ALL_DAY + "=1";
        String nonAllDayQuery = subQueryPrefix + subQuerySuffix + " AND " + Instances.ALL_DAY
                + "=0";

        // we use UNION ALL because we are guaranteed to have no dupes between
        // the two queries, and it is less expensive
        String query = "SELECT *" + " FROM (" + allDayQuery + " UNION ALL " + nonAllDayQuery + ")"
        // avoid rescheduling existing alarms
                + " WHERE 0=(SELECT count(*) FROM " + Tables.CALENDAR_ALERTS + " CA" + " WHERE CA."
                + CalendarAlerts.EVENT_ID + "=eventId" + " AND CA." + CalendarAlerts.BEGIN + "="
                + Instances.BEGIN + " AND CA." + CalendarAlerts.ALARM_TIME + "=myAlarmTime)"
                + " ORDER BY myAlarmTime," + Instances.BEGIN + "," + Instances.TITLE;

        String queryParams[] = new String[] { String.valueOf(start), String.valueOf(nextAlarmTime),
                String.valueOf(currentMillis), String.valueOf(start), String.valueOf(nextAlarmTime),
                String.valueOf(currentMillis) };

        String instancesTimezone = cp2.mCalendarCache.readTimezoneInstances();
        boolean isHomeTimezone = cp2.mCalendarCache.readTimezoneType().equals(
                CalendarCache.TIMEZONE_TYPE_HOME);
        // expand this range by a day on either end to account for all day
        // events
        cp2.acquireInstanceRangeLocked(
                start - DateUtils.DAY_IN_MILLIS, end + DateUtils.DAY_IN_MILLIS, false /*
                                                                                       * don't
                                                                                       * use
                                                                                       * minimum
                                                                                       * expansion
                                                                                       * windows
                                                                                       */,
                false /* do not force Instances deletion and expansion */, instancesTimezone,
                isHomeTimezone);
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, queryParams);

            final int beginIndex = cursor.getColumnIndex(Instances.BEGIN);
            final int endIndex = cursor.getColumnIndex(Instances.END);
            final int eventIdIndex = cursor.getColumnIndex("eventId");
            final int alarmTimeIndex = cursor.getColumnIndex("myAlarmTime");
            final int minutesIndex = cursor.getColumnIndex(Reminders.MINUTES);

            if (Log.isLoggable(CalendarProvider2.TAG, Log.DEBUG)) {
                time.set(nextAlarmTime);
                String alarmTimeStr = time.format(" %a, %b %d, %Y %I:%M%P");
                Log.d(CalendarProvider2.TAG,
                        "cursor results: " + cursor.getCount() + " nextAlarmTime: " + alarmTimeStr);
            }

            while (cursor.moveToNext()) {
                // Schedule all alarms whose alarm time is as early as any
                // scheduled alarm. For example, if the earliest alarm is at
                // 1pm, then we will schedule all alarms that occur at 1pm
                // but no alarms that occur later than 1pm.
                // Actually, we allow alarms up to a minute later to also
                // be scheduled so that we don't have to check immediately
                // again after an event alarm goes off.
                final long alarmTime = cursor.getLong(alarmTimeIndex);
                final long eventId = cursor.getLong(eventIdIndex);
                final int minutes = cursor.getInt(minutesIndex);
                final long startTime = cursor.getLong(beginIndex);
                final long endTime = cursor.getLong(endIndex);

                if (Log.isLoggable(CalendarProvider2.TAG, Log.DEBUG)) {
                    time.set(alarmTime);
                    String schedTime = time.format(" %a, %b %d, %Y %I:%M%P");
                    time.set(startTime);
                    String startTimeStr = time.format(" %a, %b %d, %Y %I:%M%P");

                    Log.d(CalendarProvider2.TAG,
                            "  looking at id: " + eventId + " " + startTime + startTimeStr
                                    + " alarm: " + alarmTime + schedTime);
                }

                if (alarmTime < nextAlarmTime) {
                    nextAlarmTime = alarmTime;
                } else if (alarmTime > nextAlarmTime + DateUtils.MINUTE_IN_MILLIS) {
                    // This event alarm (and all later ones) will be scheduled
                    // later.
                    if (Log.isLoggable(CalendarProvider2.TAG, Log.DEBUG)) {
                        Log.d(CalendarProvider2.TAG,
                                "This event alarm (and all later ones) will be scheduled later");
                    }
                    break;
                }

                // Avoid an SQLiteContraintException by checking if this alarm
                // already exists in the table.
                if (CalendarAlerts.alarmExists(resolver, eventId, startTime, alarmTime)) {
                    if (Log.isLoggable(CalendarProvider2.TAG, Log.DEBUG)) {
                        int titleIndex = cursor.getColumnIndex(Events.TITLE);
                        String title = cursor.getString(titleIndex);
                        Log.d(CalendarProvider2.TAG,
                                "  alarm exists for id: " + eventId + " " + title);
                    }
                    continue;
                }

                // Insert this alarm into the CalendarAlerts table
                Uri uri = CalendarAlerts.insert(
                        resolver, eventId, startTime, endTime, alarmTime, minutes);
                if (uri == null) {
                    if (Log.isLoggable(CalendarProvider2.TAG, Log.ERROR)) {
                        Log.e(CalendarProvider2.TAG, "runScheduleNextAlarm() insert into "
                                + "CalendarAlerts table failed");
                    }
                    continue;
                }

                scheduleAlarm(alarmTime);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Refresh notification bar
        if (rowsDeleted > 0) {
            scheduleAlarm(currentMillis);
        }

        // If we scheduled an event alarm, then schedule the next alarm check
        // for one minute past that alarm. Otherwise, if there were no
        // event alarms scheduled, then check again in 24 hours. If a new
        // event is inserted before the next alarm check, then this method
        // will be run again when the new event is inserted.
        if (nextAlarmTime != Long.MAX_VALUE) {
            scheduleNextAlarmCheck(nextAlarmTime + DateUtils.MINUTE_IN_MILLIS);
        } else {
            scheduleNextAlarmCheck(currentMillis + DateUtils.DAY_IN_MILLIS);
        }
    }

    /**
     * Removes the entries in the CalendarAlerts table for alarms that we have
     * scheduled but that have not fired yet. We do this to ensure that we don't
     * miss an alarm. The CalendarAlerts table keeps track of the alarms that we
     * have scheduled but the actual alarm list is in memory and will be cleared
     * if the phone reboots. We don't need to remove entries that have already
     * fired, and in fact we should not remove them because we need to display
     * the notifications until the user dismisses them. We could remove entries
     * that have fired and been dismissed, but we leave them around for a while
     * because it makes it easier to debug problems. Entries that are old enough
     * will be cleaned up later when we schedule new alarms.
     */
    private static void removeScheduledAlarmsLocked(SQLiteDatabase db) {
        if (Log.isLoggable(CalendarProvider2.TAG, Log.DEBUG)) {
            Log.d(CalendarProvider2.TAG, "removing scheduled alarms");
        }
        db.delete(CalendarAlerts.TABLE_NAME, CalendarAlerts.STATE + "="
                + CalendarAlerts.STATE_SCHEDULED, null /* whereArgs */);
    }

    public void set(int type, long triggerAtTime, PendingIntent operation) {
        mAlarmManager.setExact(type, triggerAtTime, operation);
    }

    public void cancel(PendingIntent operation) {
        mAlarmManager.cancel(operation);
    }

    public void scheduleAlarm(long alarmTime) {
        CalendarContract.CalendarAlerts.scheduleAlarm(mContext, mAlarmManager, alarmTime);
    }

    public void rescheduleMissedAlarms(ContentResolver cr) {
        CalendarContract.CalendarAlerts.rescheduleMissedAlarms(cr, mContext, mAlarmManager);
    }
}
