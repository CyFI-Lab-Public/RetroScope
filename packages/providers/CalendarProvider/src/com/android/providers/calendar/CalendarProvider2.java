/*
**
** Copyright 2006, The Android Open Source Project
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.CalendarAlerts;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;
import android.provider.CalendarContract.SyncState;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;
import android.util.TimeUtils;

import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.Duration;
import com.android.calendarcommon2.EventRecurrence;
import com.android.calendarcommon2.RecurrenceProcessor;
import com.android.calendarcommon2.RecurrenceSet;
import com.android.providers.calendar.CalendarDatabaseHelper.Tables;
import com.android.providers.calendar.CalendarDatabaseHelper.Views;
import com.google.android.collect.Sets;
import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calendar content provider. The contract between this provider and applications
 * is defined in {@link android.provider.CalendarContract}.
 */
public class CalendarProvider2 extends SQLiteContentProvider implements OnAccountsUpdateListener {


    protected static final String TAG = "CalendarProvider2";
    static final boolean DEBUG_INSTANCES = false;

    private static final String TIMEZONE_GMT = "GMT";
    private static final String ACCOUNT_SELECTION_PREFIX = Calendars.ACCOUNT_NAME + "=? AND "
            + Calendars.ACCOUNT_TYPE + "=?";

    protected static final boolean PROFILE = false;
    private static final boolean MULTIPLE_ATTENDEES_PER_EVENT = true;

    private static final String[] ID_ONLY_PROJECTION =
            new String[] {Events._ID};

    private static final String[] EVENTS_PROJECTION = new String[] {
            Events._SYNC_ID,
            Events.RRULE,
            Events.RDATE,
            Events.ORIGINAL_ID,
            Events.ORIGINAL_SYNC_ID,
    };

    private static final int EVENTS_SYNC_ID_INDEX = 0;
    private static final int EVENTS_RRULE_INDEX = 1;
    private static final int EVENTS_RDATE_INDEX = 2;
    private static final int EVENTS_ORIGINAL_ID_INDEX = 3;
    private static final int EVENTS_ORIGINAL_SYNC_ID_INDEX = 4;

    private static final String[] COLORS_PROJECTION = new String[] {
        Colors.ACCOUNT_NAME,
        Colors.ACCOUNT_TYPE,
        Colors.COLOR_TYPE,
        Colors.COLOR_KEY,
        Colors.COLOR,
    };
    private static final int COLORS_ACCOUNT_NAME_INDEX = 0;
    private static final int COLORS_ACCOUNT_TYPE_INDEX = 1;
    private static final int COLORS_COLOR_TYPE_INDEX = 2;
    private static final int COLORS_COLOR_INDEX_INDEX = 3;
    private static final int COLORS_COLOR_INDEX = 4;

    private static final String COLOR_FULL_SELECTION = Colors.ACCOUNT_NAME + "=? AND "
            + Colors.ACCOUNT_TYPE + "=? AND " + Colors.COLOR_TYPE + "=? AND " + Colors.COLOR_KEY
            + "=?";

    private static final String GENERIC_ACCOUNT_NAME = Calendars.ACCOUNT_NAME;
    private static final String GENERIC_ACCOUNT_TYPE = Calendars.ACCOUNT_TYPE;
    private static final String[] ACCOUNT_PROJECTION = new String[] {
        GENERIC_ACCOUNT_NAME,
        GENERIC_ACCOUNT_TYPE,
    };
    private static final int ACCOUNT_NAME_INDEX = 0;
    private static final int ACCOUNT_TYPE_INDEX = 1;

    // many tables have _id and event_id; pick a representative version to use as our generic
    private static final String GENERIC_ID = Attendees._ID;
    private static final String GENERIC_EVENT_ID = Attendees.EVENT_ID;

    private static final String[] ID_PROJECTION = new String[] {
            GENERIC_ID,
            GENERIC_EVENT_ID,
    };
    private static final int ID_INDEX = 0;
    private static final int EVENT_ID_INDEX = 1;

    /**
     * Projection to query for correcting times in allDay events.
     */
    private static final String[] ALLDAY_TIME_PROJECTION = new String[] {
        Events._ID,
        Events.DTSTART,
        Events.DTEND,
        Events.DURATION
    };
    private static final int ALLDAY_ID_INDEX = 0;
    private static final int ALLDAY_DTSTART_INDEX = 1;
    private static final int ALLDAY_DTEND_INDEX = 2;
    private static final int ALLDAY_DURATION_INDEX = 3;

    private static final int DAY_IN_SECONDS = 24 * 60 * 60;

    /**
     * The cached copy of the CalendarMetaData database table.
     * Make this "package private" instead of "private" so that test code
     * can access it.
     */
    MetaData mMetaData;
    CalendarCache mCalendarCache;

    private CalendarDatabaseHelper mDbHelper;
    private CalendarInstancesHelper mInstancesHelper;

    // The extended property name for storing an Event original Timezone.
    // Due to an issue in Calendar Server restricting the length of the name we
    // had to strip it down
    // TODO - Better name would be:
    // "com.android.providers.calendar.CalendarSyncAdapter#originalTimezone"
    protected static final String EXT_PROP_ORIGINAL_TIMEZONE =
        "CalendarSyncAdapter#originalTimezone";

    private static final String SQL_SELECT_EVENTSRAWTIMES = "SELECT " +
            CalendarContract.EventsRawTimes.EVENT_ID + ", " +
            CalendarContract.EventsRawTimes.DTSTART_2445 + ", " +
            CalendarContract.EventsRawTimes.DTEND_2445 + ", " +
            Events.EVENT_TIMEZONE +
            " FROM " +
            Tables.EVENTS_RAW_TIMES + ", " +
            Tables.EVENTS +
            " WHERE " +
            CalendarContract.EventsRawTimes.EVENT_ID + " = " + Tables.EVENTS + "." + Events._ID;

    private static final String SQL_UPDATE_EVENT_SET_DIRTY_AND_MUTATORS = "UPDATE " +
            Tables.EVENTS + " SET " +
            Events.DIRTY + "=1," +
            Events.MUTATORS + "=? " +
            " WHERE " + Events._ID + "=?";

    private static final String SQL_QUERY_EVENT_MUTATORS = "SELECT " + Events.MUTATORS +
            " FROM " + Tables.EVENTS +
            " WHERE " + Events._ID + "=?";

    private static final String SQL_WHERE_CALENDAR_COLOR = Calendars.ACCOUNT_NAME + "=? AND "
            + Calendars.ACCOUNT_TYPE + "=? AND " + Calendars.CALENDAR_COLOR_KEY + "=?";

    private static final String SQL_WHERE_EVENT_COLOR = "calendar_id in (SELECT _id from "
            + Tables.CALENDARS + " WHERE " + Events.ACCOUNT_NAME + "=? AND " + Events.ACCOUNT_TYPE
            + "=?) AND " + Events.EVENT_COLOR_KEY + "=?";

    protected static final String SQL_WHERE_ID = GENERIC_ID + "=?";
    private static final String SQL_WHERE_EVENT_ID = GENERIC_EVENT_ID + "=?";
    private static final String SQL_WHERE_ORIGINAL_ID = Events.ORIGINAL_ID + "=?";
    private static final String SQL_WHERE_ORIGINAL_ID_NO_SYNC_ID = Events.ORIGINAL_ID +
            "=? AND " + Events._SYNC_ID + " IS NULL";

    private static final String SQL_WHERE_ATTENDEE_BASE =
            Tables.EVENTS + "." + Events._ID + "=" + Tables.ATTENDEES + "." + Attendees.EVENT_ID
            + " AND " +
            Tables.EVENTS + "." + Events.CALENDAR_ID + "=" + Tables.CALENDARS + "." + Calendars._ID;

    private static final String SQL_WHERE_ATTENDEES_ID =
            Tables.ATTENDEES + "." + Attendees._ID + "=? AND " + SQL_WHERE_ATTENDEE_BASE;

    private static final String SQL_WHERE_REMINDERS_ID =
            Tables.REMINDERS + "." + Reminders._ID + "=? AND " +
            Tables.EVENTS + "." + Events._ID + "=" + Tables.REMINDERS + "." + Reminders.EVENT_ID +
            " AND " +
            Tables.EVENTS + "." + Events.CALENDAR_ID + "=" + Tables.CALENDARS + "." + Calendars._ID;

    private static final String SQL_WHERE_CALENDAR_ALERT =
            Views.EVENTS + "." + Events._ID + "=" +
                    Tables.CALENDAR_ALERTS + "." + CalendarAlerts.EVENT_ID;

    private static final String SQL_WHERE_CALENDAR_ALERT_ID =
            Views.EVENTS + "." + Events._ID + "=" +
                    Tables.CALENDAR_ALERTS + "." + CalendarAlerts.EVENT_ID +
            " AND " +
            Tables.CALENDAR_ALERTS + "." + CalendarAlerts._ID + "=?";

    private static final String SQL_WHERE_EXTENDED_PROPERTIES_ID =
            Tables.EXTENDED_PROPERTIES + "." + CalendarContract.ExtendedProperties._ID + "=?";

    private static final String SQL_DELETE_FROM_CALENDARS = "DELETE FROM " + Tables.CALENDARS +
                " WHERE " + Calendars.ACCOUNT_NAME + "=? AND " +
                    Calendars.ACCOUNT_TYPE + "=?";

    private static final String SQL_DELETE_FROM_COLORS = "DELETE FROM " + Tables.COLORS + " WHERE "
            + Calendars.ACCOUNT_NAME + "=? AND " + Calendars.ACCOUNT_TYPE + "=?";

    private static final String SQL_SELECT_COUNT_FOR_SYNC_ID =
            "SELECT COUNT(*) FROM " + Tables.EVENTS + " WHERE " + Events._SYNC_ID + "=?";

    // Make sure we load at least two months worth of data.
    // Client apps can load more data in a background thread.
    private static final long MINIMUM_EXPANSION_SPAN =
            2L * 31 * 24 * 60 * 60 * 1000;

    private static final String[] sCalendarsIdProjection = new String[] { Calendars._ID };
    private static final int CALENDARS_INDEX_ID = 0;

    private static final String INSTANCE_QUERY_TABLES =
        CalendarDatabaseHelper.Tables.INSTANCES + " INNER JOIN " +
        CalendarDatabaseHelper.Views.EVENTS + " AS " +
        CalendarDatabaseHelper.Tables.EVENTS +
        " ON (" + CalendarDatabaseHelper.Tables.INSTANCES + "."
        + CalendarContract.Instances.EVENT_ID + "=" +
        CalendarDatabaseHelper.Tables.EVENTS + "."
        + CalendarContract.Events._ID + ")";

    private static final String INSTANCE_SEARCH_QUERY_TABLES = "(" +
        CalendarDatabaseHelper.Tables.INSTANCES + " INNER JOIN " +
        CalendarDatabaseHelper.Views.EVENTS + " AS " +
        CalendarDatabaseHelper.Tables.EVENTS +
        " ON (" + CalendarDatabaseHelper.Tables.INSTANCES + "."
        + CalendarContract.Instances.EVENT_ID + "=" +
        CalendarDatabaseHelper.Tables.EVENTS + "."
        + CalendarContract.Events._ID + ")" + ") LEFT OUTER JOIN " +
        CalendarDatabaseHelper.Tables.ATTENDEES +
        " ON (" + CalendarDatabaseHelper.Tables.ATTENDEES + "."
        + CalendarContract.Attendees.EVENT_ID + "=" +
        CalendarDatabaseHelper.Tables.EVENTS + "."
        + CalendarContract.Events._ID + ")";

    private static final String SQL_WHERE_INSTANCES_BETWEEN_DAY =
        CalendarContract.Instances.START_DAY + "<=? AND " +
        CalendarContract.Instances.END_DAY + ">=?";

    private static final String SQL_WHERE_INSTANCES_BETWEEN =
        CalendarContract.Instances.BEGIN + "<=? AND " +
        CalendarContract.Instances.END + ">=?";

    private static final int INSTANCES_INDEX_START_DAY = 0;
    private static final int INSTANCES_INDEX_END_DAY = 1;
    private static final int INSTANCES_INDEX_START_MINUTE = 2;
    private static final int INSTANCES_INDEX_END_MINUTE = 3;
    private static final int INSTANCES_INDEX_ALL_DAY = 4;

    /**
     * The sort order is: events with an earlier start time occur first and if
     * the start times are the same, then events with a later end time occur
     * first. The later end time is ordered first so that long-running events in
     * the calendar views appear first. If the start and end times of two events
     * are the same then we sort alphabetically on the title. This isn't
     * required for correctness, it just adds a nice touch.
     */
    public static final String SORT_CALENDAR_VIEW = "begin ASC, end DESC, title ASC";

    /**
     * A regex for describing how we split search queries into tokens. Keeps
     * quoted phrases as one token. "one \"two three\"" ==> ["one" "two three"]
     */
    private static final Pattern SEARCH_TOKEN_PATTERN =
        Pattern.compile("[^\\s\"'.?!,]+|" // first part matches unquoted words
                      + "\"([^\"]*)\"");  // second part matches quoted phrases
    /**
     * A special character that was use to escape potentially problematic
     * characters in search queries.
     *
     * Note: do not use backslash for this, as it interferes with the regex
     * escaping mechanism.
     */
    private static final String SEARCH_ESCAPE_CHAR = "#";

    /**
     * A regex for matching any characters in an incoming search query that we
     * need to escape with {@link #SEARCH_ESCAPE_CHAR}, including the escape
     * character itself.
     */
    private static final Pattern SEARCH_ESCAPE_PATTERN =
        Pattern.compile("([%_" + SEARCH_ESCAPE_CHAR + "])");

    /**
     * Alias used for aggregate concatenation of attendee e-mails when grouping
     * attendees by instance.
     */
    private static final String ATTENDEES_EMAIL_CONCAT =
        "group_concat(" + CalendarContract.Attendees.ATTENDEE_EMAIL + ")";

    /**
     * Alias used for aggregate concatenation of attendee names when grouping
     * attendees by instance.
     */
    private static final String ATTENDEES_NAME_CONCAT =
        "group_concat(" + CalendarContract.Attendees.ATTENDEE_NAME + ")";

    private static final String[] SEARCH_COLUMNS = new String[] {
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DESCRIPTION,
        CalendarContract.Events.EVENT_LOCATION,
        ATTENDEES_EMAIL_CONCAT,
        ATTENDEES_NAME_CONCAT
    };

    /**
     * Arbitrary integer that we assign to the messages that we send to this
     * thread's handler, indicating that these are requests to send an update
     * notification intent.
     */
    private static final int UPDATE_BROADCAST_MSG = 1;

    /**
     * Any requests to send a PROVIDER_CHANGED intent will be collapsed over
     * this window, to prevent spamming too many intents at once.
     */
    private static final long UPDATE_BROADCAST_TIMEOUT_MILLIS =
        DateUtils.SECOND_IN_MILLIS;

    private static final long SYNC_UPDATE_BROADCAST_TIMEOUT_MILLIS =
        30 * DateUtils.SECOND_IN_MILLIS;

    private static final HashSet<String> ALLOWED_URI_PARAMETERS = Sets.newHashSet(
            CalendarContract.CALLER_IS_SYNCADAPTER,
            CalendarContract.EventsEntity.ACCOUNT_NAME,
            CalendarContract.EventsEntity.ACCOUNT_TYPE);

    /** Set of columns allowed to be altered when creating an exception to a recurring event. */
    private static final HashSet<String> ALLOWED_IN_EXCEPTION = new HashSet<String>();
    static {
        // _id, _sync_account, _sync_account_type, dirty, _sync_mark, calendar_id
        ALLOWED_IN_EXCEPTION.add(Events._SYNC_ID);
        ALLOWED_IN_EXCEPTION.add(Events.SYNC_DATA1);
        ALLOWED_IN_EXCEPTION.add(Events.SYNC_DATA7);
        ALLOWED_IN_EXCEPTION.add(Events.SYNC_DATA3);
        ALLOWED_IN_EXCEPTION.add(Events.TITLE);
        ALLOWED_IN_EXCEPTION.add(Events.EVENT_LOCATION);
        ALLOWED_IN_EXCEPTION.add(Events.DESCRIPTION);
        ALLOWED_IN_EXCEPTION.add(Events.EVENT_COLOR);
        ALLOWED_IN_EXCEPTION.add(Events.EVENT_COLOR_KEY);
        ALLOWED_IN_EXCEPTION.add(Events.STATUS);
        ALLOWED_IN_EXCEPTION.add(Events.SELF_ATTENDEE_STATUS);
        ALLOWED_IN_EXCEPTION.add(Events.SYNC_DATA6);
        ALLOWED_IN_EXCEPTION.add(Events.DTSTART);
        // dtend -- set from duration as part of creating the exception
        ALLOWED_IN_EXCEPTION.add(Events.EVENT_TIMEZONE);
        ALLOWED_IN_EXCEPTION.add(Events.EVENT_END_TIMEZONE);
        ALLOWED_IN_EXCEPTION.add(Events.DURATION);
        ALLOWED_IN_EXCEPTION.add(Events.ALL_DAY);
        ALLOWED_IN_EXCEPTION.add(Events.ACCESS_LEVEL);
        ALLOWED_IN_EXCEPTION.add(Events.AVAILABILITY);
        ALLOWED_IN_EXCEPTION.add(Events.HAS_ALARM);
        ALLOWED_IN_EXCEPTION.add(Events.HAS_EXTENDED_PROPERTIES);
        ALLOWED_IN_EXCEPTION.add(Events.RRULE);
        ALLOWED_IN_EXCEPTION.add(Events.RDATE);
        ALLOWED_IN_EXCEPTION.add(Events.EXRULE);
        ALLOWED_IN_EXCEPTION.add(Events.EXDATE);
        ALLOWED_IN_EXCEPTION.add(Events.ORIGINAL_SYNC_ID);
        ALLOWED_IN_EXCEPTION.add(Events.ORIGINAL_INSTANCE_TIME);
        // originalAllDay, lastDate
        ALLOWED_IN_EXCEPTION.add(Events.HAS_ATTENDEE_DATA);
        ALLOWED_IN_EXCEPTION.add(Events.GUESTS_CAN_MODIFY);
        ALLOWED_IN_EXCEPTION.add(Events.GUESTS_CAN_INVITE_OTHERS);
        ALLOWED_IN_EXCEPTION.add(Events.GUESTS_CAN_SEE_GUESTS);
        ALLOWED_IN_EXCEPTION.add(Events.ORGANIZER);
        ALLOWED_IN_EXCEPTION.add(Events.CUSTOM_APP_PACKAGE);
        ALLOWED_IN_EXCEPTION.add(Events.CUSTOM_APP_URI);
        ALLOWED_IN_EXCEPTION.add(Events.UID_2445);
        // deleted, original_id, alerts
    }

    /** Don't clone these from the base event into the exception event. */
    private static final String[] DONT_CLONE_INTO_EXCEPTION = {
        Events._SYNC_ID,
        Events.SYNC_DATA1,
        Events.SYNC_DATA2,
        Events.SYNC_DATA3,
        Events.SYNC_DATA4,
        Events.SYNC_DATA5,
        Events.SYNC_DATA6,
        Events.SYNC_DATA7,
        Events.SYNC_DATA8,
        Events.SYNC_DATA9,
        Events.SYNC_DATA10,
    };

    /** set to 'true' to enable debug logging for recurrence exception code */
    private static final boolean DEBUG_EXCEPTION = false;

    private Context mContext;
    private ContentResolver mContentResolver;

    private static CalendarProvider2 mInstance;

    @VisibleForTesting
    protected CalendarAlarmManager mCalendarAlarm;

    private final Handler mBroadcastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Context context = CalendarProvider2.this.mContext;
            if (msg.what == UPDATE_BROADCAST_MSG) {
                // Broadcast a provider changed intent
                doSendUpdateNotification();
                // Because the handler does not guarantee message delivery in
                // the case that the provider is killed, we need to make sure
                // that the provider stays alive long enough to deliver the
                // notification. This empty service is sufficient to "wedge" the
                // process until we stop it here.
                context.stopService(new Intent(context, EmptyService.class));
            }
        }
    };

    /**
     * Listens for timezone changes and disk-no-longer-full events
     */
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onReceive() " + action);
            }
            if (Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                updateTimezoneDependentFields();
                mCalendarAlarm.scheduleNextAlarm(false /* do not remove alarms */);
            } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                // Try to clean up if things were screwy due to a full disk
                updateTimezoneDependentFields();
                mCalendarAlarm.scheduleNextAlarm(false /* do not remove alarms */);
            } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                mCalendarAlarm.scheduleNextAlarm(false /* do not remove alarms */);
            }
        }
    };

    /* Visible for testing */
    @Override
    protected CalendarDatabaseHelper getDatabaseHelper(final Context context) {
        return CalendarDatabaseHelper.getInstance(context);
    }

    protected static CalendarProvider2 getInstance() {
        return mInstance;
    }

    @Override
    public void shutdown() {
        if (mDbHelper != null) {
            mDbHelper.close();
            mDbHelper = null;
            mDb = null;
        }
    }

    @Override
    public boolean onCreate() {
        super.onCreate();
        setAppOps(AppOpsManager.OP_READ_CALENDAR, AppOpsManager.OP_WRITE_CALENDAR);
        try {
            return initialize();
        } catch (RuntimeException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Cannot start provider", e);
            }
            return false;
        }
    }

    private boolean initialize() {
        mInstance = this;

        mContext = getContext();
        mContentResolver = mContext.getContentResolver();

        mDbHelper = (CalendarDatabaseHelper)getDatabaseHelper();
        mDb = mDbHelper.getWritableDatabase();

        mMetaData = new MetaData(mDbHelper);
        mInstancesHelper = new CalendarInstancesHelper(mDbHelper, mMetaData);

        // Register for Intent broadcasts
        IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);

        // We don't ever unregister this because this thread always wants
        // to receive notifications, even in the background.  And if this
        // thread is killed then the whole process will be killed and the
        // memory resources will be reclaimed.
        mContext.registerReceiver(mIntentReceiver, filter);

        mCalendarCache = new CalendarCache(mDbHelper);

        // This is pulled out for testing
        initCalendarAlarm();

        postInitialize();

        return true;
    }

    protected void initCalendarAlarm() {
        mCalendarAlarm = getOrCreateCalendarAlarmManager();
    }

    synchronized CalendarAlarmManager getOrCreateCalendarAlarmManager() {
        if (mCalendarAlarm == null) {
            mCalendarAlarm = new CalendarAlarmManager(mContext);
            Log.i(TAG, "Created " + mCalendarAlarm + "(" + this + ")");
        }
        return mCalendarAlarm;
    }

    protected void postInitialize() {
        Thread thread = new PostInitializeThread();
        thread.start();
    }

    private class PostInitializeThread extends Thread {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            verifyAccounts();

            try {
                doUpdateTimezoneDependentFields();
            } catch (IllegalStateException e) {
                // Added this because tests would fail if the provider is
                // closed by the time this is executed

                // Nothing actionable here anyways.
            }
        }
    }

    private void verifyAccounts() {
        AccountManager.get(getContext()).addOnAccountsUpdatedListener(this, null, false);
        removeStaleAccounts(AccountManager.get(getContext()).getAccounts());
    }


    /**
     * This creates a background thread to check the timezone and update
     * the timezone dependent fields in the Instances table if the timezone
     * has changed.
     */
    protected void updateTimezoneDependentFields() {
        Thread thread = new TimezoneCheckerThread();
        thread.start();
    }

    private class TimezoneCheckerThread extends Thread {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            doUpdateTimezoneDependentFields();
        }
    }

    /**
     * Check if we are in the same time zone
     */
    private boolean isLocalSameAsInstancesTimezone() {
        String localTimezone = TimeZone.getDefault().getID();
        return TextUtils.equals(mCalendarCache.readTimezoneInstances(), localTimezone);
    }

    /**
     * This method runs in a background thread.  If the timezone has changed
     * then the Instances table will be regenerated.
     */
    protected void doUpdateTimezoneDependentFields() {
        try {
            String timezoneType = mCalendarCache.readTimezoneType();
            // Nothing to do if we have the "home" timezone type (timezone is sticky)
            if (timezoneType != null && timezoneType.equals(CalendarCache.TIMEZONE_TYPE_HOME)) {
                return;
            }
            // We are here in "auto" mode, the timezone is coming from the device
            if (! isSameTimezoneDatabaseVersion()) {
                String localTimezone = TimeZone.getDefault().getID();
                doProcessEventRawTimes(localTimezone, TimeUtils.getTimeZoneDatabaseVersion());
            }
            if (isLocalSameAsInstancesTimezone()) {
                // Even if the timezone hasn't changed, check for missed alarms.
                // This code executes when the CalendarProvider2 is created and
                // helps to catch missed alarms when the Calendar process is
                // killed (because of low-memory conditions) and then restarted.
                mCalendarAlarm.rescheduleMissedAlarms();
            }
        } catch (SQLException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "doUpdateTimezoneDependentFields() failed", e);
            }
            try {
                // Clear at least the in-memory data (and if possible the
                // database fields) to force a re-computation of Instances.
                mMetaData.clearInstanceRange();
            } catch (SQLException e2) {
                if (Log.isLoggable(TAG, Log.ERROR)) {
                    Log.e(TAG, "clearInstanceRange() also failed: " + e2);
                }
            }
        }
    }

    protected void doProcessEventRawTimes(String localTimezone, String timeZoneDatabaseVersion) {
        mDb.beginTransaction();
        try {
            updateEventsStartEndFromEventRawTimesLocked();
            updateTimezoneDatabaseVersion(timeZoneDatabaseVersion);
            mCalendarCache.writeTimezoneInstances(localTimezone);
            regenerateInstancesTable();
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
    }

    private void updateEventsStartEndFromEventRawTimesLocked() {
        Cursor cursor = mDb.rawQuery(SQL_SELECT_EVENTSRAWTIMES, null /* selection args */);
        try {
            while (cursor.moveToNext()) {
                long eventId = cursor.getLong(0);
                String dtStart2445 = cursor.getString(1);
                String dtEnd2445 = cursor.getString(2);
                String eventTimezone = cursor.getString(3);
                if (dtStart2445 == null && dtEnd2445 == null) {
                    if (Log.isLoggable(TAG, Log.ERROR)) {
                        Log.e(TAG, "Event " + eventId + " has dtStart2445 and dtEnd2445 null "
                                + "at the same time in EventsRawTimes!");
                    }
                    continue;
                }
                updateEventsStartEndLocked(eventId,
                        eventTimezone,
                        dtStart2445,
                        dtEnd2445);
            }
        } finally {
            cursor.close();
            cursor = null;
        }
    }

    private long get2445ToMillis(String timezone, String dt2445) {
        if (null == dt2445) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Cannot parse null RFC2445 date");
            }
            return 0;
        }
        Time time = (timezone != null) ? new Time(timezone) : new Time();
        try {
            time.parse(dt2445);
        } catch (TimeFormatException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Cannot parse RFC2445 date " + dt2445);
            }
            return 0;
        }
        return time.toMillis(true /* ignore DST */);
    }

    private void updateEventsStartEndLocked(long eventId,
            String timezone, String dtStart2445, String dtEnd2445) {

        ContentValues values = new ContentValues();
        values.put(Events.DTSTART, get2445ToMillis(timezone, dtStart2445));
        values.put(Events.DTEND, get2445ToMillis(timezone, dtEnd2445));

        int result = mDb.update(Tables.EVENTS, values, SQL_WHERE_ID,
                new String[] {String.valueOf(eventId)});
        if (0 == result) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Could not update Events table with values " + values);
            }
        }
    }

    private void updateTimezoneDatabaseVersion(String timeZoneDatabaseVersion) {
        try {
            mCalendarCache.writeTimezoneDatabaseVersion(timeZoneDatabaseVersion);
        } catch (CalendarCache.CacheException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Could not write timezone database version in the cache");
            }
        }
    }

    /**
     * Check if the time zone database version is the same as the cached one
     */
    protected boolean isSameTimezoneDatabaseVersion() {
        String timezoneDatabaseVersion = mCalendarCache.readTimezoneDatabaseVersion();
        if (timezoneDatabaseVersion == null) {
            return false;
        }
        return TextUtils.equals(timezoneDatabaseVersion, TimeUtils.getTimeZoneDatabaseVersion());
    }

    @VisibleForTesting
    protected String getTimezoneDatabaseVersion() {
        String timezoneDatabaseVersion = mCalendarCache.readTimezoneDatabaseVersion();
        if (timezoneDatabaseVersion == null) {
            return "";
        }
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "timezoneDatabaseVersion = " + timezoneDatabaseVersion);
        }
        return timezoneDatabaseVersion;
    }

    private boolean isHomeTimezone() {
        String type = mCalendarCache.readTimezoneType();
        return type.equals(CalendarCache.TIMEZONE_TYPE_HOME);
    }

    private void regenerateInstancesTable() {
        // The database timezone is different from the current timezone.
        // Regenerate the Instances table for this month.  Include events
        // starting at the beginning of this month.
        long now = System.currentTimeMillis();
        String instancesTimezone = mCalendarCache.readTimezoneInstances();
        Time time = new Time(instancesTimezone);
        time.set(now);
        time.monthDay = 1;
        time.hour = 0;
        time.minute = 0;
        time.second = 0;

        long begin = time.normalize(true);
        long end = begin + MINIMUM_EXPANSION_SPAN;

        Cursor cursor = null;
        try {
            cursor = handleInstanceQuery(new SQLiteQueryBuilder(),
                    begin, end,
                    new String[] { Instances._ID },
                    null /* selection */, null,
                    null /* sort */,
                    false /* searchByDayInsteadOfMillis */,
                    true /* force Instances deletion and expansion */,
                    instancesTimezone, isHomeTimezone());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        mCalendarAlarm.rescheduleMissedAlarms();
    }


    @Override
    protected void notifyChange(boolean syncToNetwork) {
        // Note that semantics are changed: notification is for CONTENT_URI, not the specific
        // Uri that was modified.
        mContentResolver.notifyChange(CalendarContract.CONTENT_URI, null, syncToNetwork);
    }

    /**
     * ALERT table is maintained locally so don't request a sync for changes in it
     */
    @Override
    protected boolean shouldSyncFor(Uri uri) {
        final int match = sUriMatcher.match(uri);
        return !(match == CALENDAR_ALERTS ||
                match == CALENDAR_ALERTS_ID ||
                match == CALENDAR_ALERTS_BY_INSTANCE);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        final long identity = Binder.clearCallingIdentity();
        try {
            return queryInternal(uri, projection, selection, selectionArgs, sortOrder);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private Cursor queryInternal(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "query uri - " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String groupBy = null;
        String limit = null; // Not currently implemented
        String instancesTimezone;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SYNCSTATE:
                return mDbHelper.getSyncState().query(db, projection, selection, selectionArgs,
                        sortOrder);
            case SYNCSTATE_ID:
                String selectionWithId = (SyncState._ID + "=?")
                    + (selection == null ? "" : " AND (" + selection + ")");
                // Prepend id to selectionArgs
                selectionArgs = insertSelectionArg(selectionArgs,
                        String.valueOf(ContentUris.parseId(uri)));
                return mDbHelper.getSyncState().query(db, projection, selectionWithId,
                        selectionArgs, sortOrder);

            case EVENTS:
                qb.setTables(CalendarDatabaseHelper.Views.EVENTS);
                qb.setProjectionMap(sEventsProjectionMap);
                selection = appendAccountToSelection(uri, selection, Calendars.ACCOUNT_NAME,
                        Calendars.ACCOUNT_TYPE);
                selection = appendLastSyncedColumnToSelection(selection, uri);
                break;
            case EVENTS_ID:
                qb.setTables(CalendarDatabaseHelper.Views.EVENTS);
                qb.setProjectionMap(sEventsProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere(SQL_WHERE_ID);
                break;

            case EVENT_ENTITIES:
                qb.setTables(CalendarDatabaseHelper.Views.EVENTS);
                qb.setProjectionMap(sEventEntitiesProjectionMap);
                selection = appendAccountToSelection(uri, selection, Calendars.ACCOUNT_NAME,
                        Calendars.ACCOUNT_TYPE);
                selection = appendLastSyncedColumnToSelection(selection, uri);
                break;
            case EVENT_ENTITIES_ID:
                qb.setTables(CalendarDatabaseHelper.Views.EVENTS);
                qb.setProjectionMap(sEventEntitiesProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere(SQL_WHERE_ID);
                break;

            case COLORS:
                qb.setTables(Tables.COLORS);
                qb.setProjectionMap(sColorsProjectionMap);
                selection = appendAccountToSelection(uri, selection, Calendars.ACCOUNT_NAME,
                        Calendars.ACCOUNT_TYPE);
                break;

            case CALENDARS:
            case CALENDAR_ENTITIES:
                qb.setTables(Tables.CALENDARS);
                qb.setProjectionMap(sCalendarsProjectionMap);
                selection = appendAccountToSelection(uri, selection, Calendars.ACCOUNT_NAME,
                        Calendars.ACCOUNT_TYPE);
                break;
            case CALENDARS_ID:
            case CALENDAR_ENTITIES_ID:
                qb.setTables(Tables.CALENDARS);
                qb.setProjectionMap(sCalendarsProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere(SQL_WHERE_ID);
                break;
            case INSTANCES:
            case INSTANCES_BY_DAY:
                long begin;
                long end;
                try {
                    begin = Long.valueOf(uri.getPathSegments().get(2));
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Cannot parse begin "
                            + uri.getPathSegments().get(2));
                }
                try {
                    end = Long.valueOf(uri.getPathSegments().get(3));
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Cannot parse end "
                            + uri.getPathSegments().get(3));
                }
                instancesTimezone = mCalendarCache.readTimezoneInstances();
                return handleInstanceQuery(qb, begin, end, projection, selection, selectionArgs,
                        sortOrder, match == INSTANCES_BY_DAY, false /* don't force an expansion */,
                        instancesTimezone, isHomeTimezone());
            case INSTANCES_SEARCH:
            case INSTANCES_SEARCH_BY_DAY:
                try {
                    begin = Long.valueOf(uri.getPathSegments().get(2));
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Cannot parse begin "
                            + uri.getPathSegments().get(2));
                }
                try {
                    end = Long.valueOf(uri.getPathSegments().get(3));
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Cannot parse end "
                            + uri.getPathSegments().get(3));
                }
                instancesTimezone = mCalendarCache.readTimezoneInstances();
                // this is already decoded
                String query = uri.getPathSegments().get(4);
                return handleInstanceSearchQuery(qb, begin, end, query, projection, selection,
                        selectionArgs, sortOrder, match == INSTANCES_SEARCH_BY_DAY,
                        instancesTimezone, isHomeTimezone());
            case EVENT_DAYS:
                int startDay;
                int endDay;
                try {
                    startDay = Integer.valueOf(uri.getPathSegments().get(2));
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Cannot parse start day "
                            + uri.getPathSegments().get(2));
                }
                try {
                    endDay = Integer.valueOf(uri.getPathSegments().get(3));
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Cannot parse end day "
                            + uri.getPathSegments().get(3));
                }
                instancesTimezone = mCalendarCache.readTimezoneInstances();
                return handleEventDayQuery(qb, startDay, endDay, projection, selection,
                        instancesTimezone, isHomeTimezone());
            case ATTENDEES:
                qb.setTables(Tables.ATTENDEES + ", " + Tables.EVENTS + ", " + Tables.CALENDARS);
                qb.setProjectionMap(sAttendeesProjectionMap);
                qb.appendWhere(SQL_WHERE_ATTENDEE_BASE);
                break;
            case ATTENDEES_ID:
                qb.setTables(Tables.ATTENDEES + ", " + Tables.EVENTS + ", " + Tables.CALENDARS);
                qb.setProjectionMap(sAttendeesProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere(SQL_WHERE_ATTENDEES_ID);
                break;
            case REMINDERS:
                qb.setTables(Tables.REMINDERS);
                break;
            case REMINDERS_ID:
                qb.setTables(Tables.REMINDERS + ", " + Tables.EVENTS + ", " + Tables.CALENDARS);
                qb.setProjectionMap(sRemindersProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                qb.appendWhere(SQL_WHERE_REMINDERS_ID);
                break;
            case CALENDAR_ALERTS:
                qb.setTables(Tables.CALENDAR_ALERTS + ", " + CalendarDatabaseHelper.Views.EVENTS);
                qb.setProjectionMap(sCalendarAlertsProjectionMap);
                qb.appendWhere(SQL_WHERE_CALENDAR_ALERT);
                break;
            case CALENDAR_ALERTS_BY_INSTANCE:
                qb.setTables(Tables.CALENDAR_ALERTS + ", " + CalendarDatabaseHelper.Views.EVENTS);
                qb.setProjectionMap(sCalendarAlertsProjectionMap);
                qb.appendWhere(SQL_WHERE_CALENDAR_ALERT);
                groupBy = CalendarAlerts.EVENT_ID + "," + CalendarAlerts.BEGIN;
                break;
            case CALENDAR_ALERTS_ID:
                qb.setTables(Tables.CALENDAR_ALERTS + ", " + CalendarDatabaseHelper.Views.EVENTS);
                qb.setProjectionMap(sCalendarAlertsProjectionMap);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                qb.appendWhere(SQL_WHERE_CALENDAR_ALERT_ID);
                break;
            case EXTENDED_PROPERTIES:
                qb.setTables(Tables.EXTENDED_PROPERTIES);
                break;
            case EXTENDED_PROPERTIES_ID:
                qb.setTables(Tables.EXTENDED_PROPERTIES);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere(SQL_WHERE_EXTENDED_PROPERTIES_ID);
                break;
            case PROVIDER_PROPERTIES:
                qb.setTables(Tables.CALENDAR_CACHE);
                qb.setProjectionMap(sCalendarCacheProjectionMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }

        // run the query
        return query(db, qb, projection, selection, selectionArgs, sortOrder, groupBy, limit);
    }

    private void validateUriParameters(Set<String> queryParameterNames) {
        final Set<String> parameterNames = queryParameterNames;
        for (String parameterName : parameterNames) {
            if (!ALLOWED_URI_PARAMETERS.contains(parameterName)) {
                throw new IllegalArgumentException("Invalid URI parameter: " + parameterName);
            }
        }
    }

    private Cursor query(final SQLiteDatabase db, SQLiteQueryBuilder qb, String[] projection,
            String selection, String[] selectionArgs, String sortOrder, String groupBy,
            String limit) {

        if (projection != null && projection.length == 1
                && BaseColumns._COUNT.equals(projection[0])) {
            qb.setProjectionMap(sCountProjectionMap);
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "query sql - projection: " + Arrays.toString(projection) +
                    " selection: " + selection +
                    " selectionArgs: " + Arrays.toString(selectionArgs) +
                    " sortOrder: " + sortOrder +
                    " groupBy: " + groupBy +
                    " limit: " + limit);
        }
        final Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null,
                sortOrder, limit);
        if (c != null) {
            // TODO: is this the right notification Uri?
            c.setNotificationUri(mContentResolver, CalendarContract.Events.CONTENT_URI);
        }
        return c;
    }

    /*
     * Fills the Instances table, if necessary, for the given range and then
     * queries the Instances table.
     *
     * @param qb The query
     * @param rangeBegin start of range (Julian days or ms)
     * @param rangeEnd end of range (Julian days or ms)
     * @param projection The projection
     * @param selection The selection
     * @param sort How to sort
     * @param searchByDay if true, range is in Julian days, if false, range is in ms
     * @param forceExpansion force the Instance deletion and expansion if set to true
     * @param instancesTimezone timezone we need to use for computing the instances
     * @param isHomeTimezone if true, we are in the "home" timezone
     * @return
     */
    private Cursor handleInstanceQuery(SQLiteQueryBuilder qb, long rangeBegin,
            long rangeEnd, String[] projection, String selection, String[] selectionArgs,
            String sort, boolean searchByDay, boolean forceExpansion,
            String instancesTimezone, boolean isHomeTimezone) {

        qb.setTables(INSTANCE_QUERY_TABLES);
        qb.setProjectionMap(sInstancesProjectionMap);
        if (searchByDay) {
            // Convert the first and last Julian day range to a range that uses
            // UTC milliseconds.
            Time time = new Time(instancesTimezone);
            long beginMs = time.setJulianDay((int) rangeBegin);
            // We add one to lastDay because the time is set to 12am on the given
            // Julian day and we want to include all the events on the last day.
            long endMs = time.setJulianDay((int) rangeEnd + 1);
            // will lock the database.
            acquireInstanceRange(beginMs, endMs, true /* use minimum expansion window */,
                    forceExpansion, instancesTimezone, isHomeTimezone);
            qb.appendWhere(SQL_WHERE_INSTANCES_BETWEEN_DAY);
        } else {
            // will lock the database.
            acquireInstanceRange(rangeBegin, rangeEnd, true /* use minimum expansion window */,
                    forceExpansion, instancesTimezone, isHomeTimezone);
            qb.appendWhere(SQL_WHERE_INSTANCES_BETWEEN);
        }

        String[] newSelectionArgs = new String[] {String.valueOf(rangeEnd),
                String.valueOf(rangeBegin)};
        if (selectionArgs == null) {
            selectionArgs = newSelectionArgs;
        } else {
            // The appendWhere pieces get added first, so put the
            // newSelectionArgs first.
            selectionArgs = combine(newSelectionArgs, selectionArgs);
        }
        return qb.query(mDb, projection, selection, selectionArgs, null /* groupBy */,
                null /* having */, sort);
    }

    /**
     * Combine a set of arrays in the order they are passed in. All arrays must
     * be of the same type.
     */
    private static <T> T[] combine(T[]... arrays) {
        if (arrays.length == 0) {
            throw new IllegalArgumentException("Must supply at least 1 array to combine");
        }

        int totalSize = 0;
        for (T[] array : arrays) {
            totalSize += array.length;
        }

        T[] finalArray = (T[]) (Array.newInstance(arrays[0].getClass().getComponentType(),
                totalSize));

        int currentPos = 0;
        for (T[] array : arrays) {
            int length = array.length;
            System.arraycopy(array, 0, finalArray, currentPos, length);
            currentPos += array.length;
        }
        return finalArray;
    }

    /**
     * Escape any special characters in the search token
     * @param token the token to escape
     * @return the escaped token
     */
    @VisibleForTesting
    String escapeSearchToken(String token) {
        Matcher matcher = SEARCH_ESCAPE_PATTERN.matcher(token);
        return matcher.replaceAll(SEARCH_ESCAPE_CHAR + "$1");
    }

    /**
     * Splits the search query into individual search tokens based on whitespace
     * and punctuation. Leaves both single quoted and double quoted strings
     * intact.
     *
     * @param query the search query
     * @return an array of tokens from the search query
     */
    @VisibleForTesting
    String[] tokenizeSearchQuery(String query) {
        List<String> matchList = new ArrayList<String>();
        Matcher matcher = SEARCH_TOKEN_PATTERN.matcher(query);
        String token;
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // double quoted string
                token = matcher.group(1);
            } else {
                // unquoted token
                token = matcher.group();
            }
            matchList.add(escapeSearchToken(token));
        }
        return matchList.toArray(new String[matchList.size()]);
    }

    /**
     * In order to support what most people would consider a reasonable
     * search behavior, we have to do some interesting things here. We
     * assume that when a user searches for something like "lunch meeting",
     * they really want any event that matches both "lunch" and "meeting",
     * not events that match the string "lunch meeting" itself. In order to
     * do this across multiple columns, we have to construct a WHERE clause
     * that looks like:
     * <code>
     *   WHERE (title LIKE "%lunch%"
     *      OR description LIKE "%lunch%"
     *      OR eventLocation LIKE "%lunch%")
     *     AND (title LIKE "%meeting%"
     *      OR description LIKE "%meeting%"
     *      OR eventLocation LIKE "%meeting%")
     * </code>
     * This "product of clauses" is a bit ugly, but produced a fairly good
     * approximation of full-text search across multiple columns.  The set
     * of columns is specified by the SEARCH_COLUMNS constant.
     * <p>
     * Note the "WHERE" token isn't part of the returned string.  The value
     * may be passed into a query as the "HAVING" clause.
     */
    @VisibleForTesting
    String constructSearchWhere(String[] tokens) {
        if (tokens.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String column, token;
        for (int j = 0; j < tokens.length; j++) {
            sb.append("(");
            for (int i = 0; i < SEARCH_COLUMNS.length; i++) {
                sb.append(SEARCH_COLUMNS[i]);
                sb.append(" LIKE ? ESCAPE \"");
                sb.append(SEARCH_ESCAPE_CHAR);
                sb.append("\" ");
                if (i < SEARCH_COLUMNS.length - 1) {
                    sb.append("OR ");
                }
            }
            sb.append(")");
            if (j < tokens.length - 1) {
                sb.append(" AND ");
            }
        }
        return sb.toString();
    }

    @VisibleForTesting
    String[] constructSearchArgs(String[] tokens, long rangeBegin, long rangeEnd) {
        int numCols = SEARCH_COLUMNS.length;
        int numArgs = tokens.length * numCols + 2;
        // the additional two elements here are for begin/end time
        String[] selectionArgs = new String[numArgs];
        selectionArgs[0] =  String.valueOf(rangeEnd);
        selectionArgs[1] =  String.valueOf(rangeBegin);
        for (int j = 0; j < tokens.length; j++) {
            int start = 2 + numCols * j;
            for (int i = start; i < start + numCols; i++) {
                selectionArgs[i] = "%" + tokens[j] + "%";
            }
        }
        return selectionArgs;
    }

    private Cursor handleInstanceSearchQuery(SQLiteQueryBuilder qb,
            long rangeBegin, long rangeEnd, String query, String[] projection,
            String selection, String[] selectionArgs, String sort, boolean searchByDay,
            String instancesTimezone, boolean isHomeTimezone) {
        qb.setTables(INSTANCE_SEARCH_QUERY_TABLES);
        qb.setProjectionMap(sInstancesProjectionMap);

        String[] tokens = tokenizeSearchQuery(query);
        String[] newSelectionArgs = constructSearchArgs(tokens, rangeBegin, rangeEnd);
        if (selectionArgs == null) {
            selectionArgs = newSelectionArgs;
        } else {
            // The appendWhere pieces get added first, so put the
            // newSelectionArgs first.
            selectionArgs = combine(newSelectionArgs, selectionArgs);
        }
        // we pass this in as a HAVING instead of a WHERE so the filtering
        // happens after the grouping
        String searchWhere = constructSearchWhere(tokens);

        if (searchByDay) {
            // Convert the first and last Julian day range to a range that uses
            // UTC milliseconds.
            Time time = new Time(instancesTimezone);
            long beginMs = time.setJulianDay((int) rangeBegin);
            // We add one to lastDay because the time is set to 12am on the given
            // Julian day and we want to include all the events on the last day.
            long endMs = time.setJulianDay((int) rangeEnd + 1);
            // will lock the database.
            // we expand the instances here because we might be searching over
            // a range where instance expansion has not occurred yet
            acquireInstanceRange(beginMs, endMs,
                    true /* use minimum expansion window */,
                    false /* do not force Instances deletion and expansion */,
                    instancesTimezone,
                    isHomeTimezone
            );
            qb.appendWhere(SQL_WHERE_INSTANCES_BETWEEN_DAY);
        } else {
            // will lock the database.
            // we expand the instances here because we might be searching over
            // a range where instance expansion has not occurred yet
            acquireInstanceRange(rangeBegin, rangeEnd,
                    true /* use minimum expansion window */,
                    false /* do not force Instances deletion and expansion */,
                    instancesTimezone,
                    isHomeTimezone
            );
            qb.appendWhere(SQL_WHERE_INSTANCES_BETWEEN);
        }

        return qb.query(mDb, projection, selection, selectionArgs,
                Tables.INSTANCES + "." + Instances._ID /* groupBy */,
                searchWhere /* having */, sort);
    }

    private Cursor handleEventDayQuery(SQLiteQueryBuilder qb, int begin, int end,
            String[] projection, String selection, String instancesTimezone,
            boolean isHomeTimezone) {
        qb.setTables(INSTANCE_QUERY_TABLES);
        qb.setProjectionMap(sInstancesProjectionMap);
        // Convert the first and last Julian day range to a range that uses
        // UTC milliseconds.
        Time time = new Time(instancesTimezone);
        long beginMs = time.setJulianDay(begin);
        // We add one to lastDay because the time is set to 12am on the given
        // Julian day and we want to include all the events on the last day.
        long endMs = time.setJulianDay(end + 1);

        acquireInstanceRange(beginMs, endMs, true,
                false /* do not force Instances expansion */, instancesTimezone, isHomeTimezone);
        qb.appendWhere(SQL_WHERE_INSTANCES_BETWEEN_DAY);
        String selectionArgs[] = new String[] {String.valueOf(end), String.valueOf(begin)};

        return qb.query(mDb, projection, selection, selectionArgs,
                Instances.START_DAY /* groupBy */, null /* having */, null);
    }

    /**
     * Ensure that the date range given has all elements in the instance
     * table.  Acquires the database lock and calls
     * {@link #acquireInstanceRangeLocked(long, long, boolean, boolean, String, boolean)}.
     *
     * @param begin start of range (ms)
     * @param end end of range (ms)
     * @param useMinimumExpansionWindow expand by at least MINIMUM_EXPANSION_SPAN
     * @param forceExpansion force the Instance deletion and expansion if set to true
     * @param instancesTimezone timezone we need to use for computing the instances
     * @param isHomeTimezone if true, we are in the "home" timezone
     */
    private void acquireInstanceRange(final long begin, final long end,
            final boolean useMinimumExpansionWindow, final boolean forceExpansion,
            final String instancesTimezone, final boolean isHomeTimezone) {
        mDb.beginTransaction();
        try {
            acquireInstanceRangeLocked(begin, end, useMinimumExpansionWindow,
                    forceExpansion, instancesTimezone, isHomeTimezone);
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
    }

    /**
     * Ensure that the date range given has all elements in the instance
     * table.  The database lock must be held when calling this method.
     *
     * @param begin start of range (ms)
     * @param end end of range (ms)
     * @param useMinimumExpansionWindow expand by at least MINIMUM_EXPANSION_SPAN
     * @param forceExpansion force the Instance deletion and expansion if set to true
     * @param instancesTimezone timezone we need to use for computing the instances
     * @param isHomeTimezone if true, we are in the "home" timezone
     */
    void acquireInstanceRangeLocked(long begin, long end, boolean useMinimumExpansionWindow,
            boolean forceExpansion, String instancesTimezone, boolean isHomeTimezone) {
        long expandBegin = begin;
        long expandEnd = end;

        if (DEBUG_INSTANCES) {
            Log.d(TAG + "-i", "acquireInstanceRange begin=" + begin + " end=" + end +
                    " useMin=" + useMinimumExpansionWindow + " force=" + forceExpansion);
        }

        if (instancesTimezone == null) {
            Log.e(TAG, "Cannot run acquireInstanceRangeLocked() because instancesTimezone is null");
            return;
        }

        if (useMinimumExpansionWindow) {
            // if we end up having to expand events into the instances table, expand
            // events for a minimal amount of time, so we do not have to perform
            // expansions frequently.
            long span = end - begin;
            if (span < MINIMUM_EXPANSION_SPAN) {
                long additionalRange = (MINIMUM_EXPANSION_SPAN - span) / 2;
                expandBegin -= additionalRange;
                expandEnd += additionalRange;
            }
        }

        // Check if the timezone has changed.
        // We do this check here because the database is locked and we can
        // safely delete all the entries in the Instances table.
        MetaData.Fields fields = mMetaData.getFieldsLocked();
        long maxInstance = fields.maxInstance;
        long minInstance = fields.minInstance;
        boolean timezoneChanged;
        if (isHomeTimezone) {
            String previousTimezone = mCalendarCache.readTimezoneInstancesPrevious();
            timezoneChanged = !instancesTimezone.equals(previousTimezone);
        } else {
            String localTimezone = TimeZone.getDefault().getID();
            timezoneChanged = !instancesTimezone.equals(localTimezone);
            // if we're in auto make sure we are using the device time zone
            if (timezoneChanged) {
                instancesTimezone = localTimezone;
            }
        }
        // if "home", then timezoneChanged only if current != previous
        // if "auto", then timezoneChanged, if !instancesTimezone.equals(localTimezone);
        if (maxInstance == 0 || timezoneChanged || forceExpansion) {
            if (DEBUG_INSTANCES) {
                Log.d(TAG + "-i", "Wiping instances and expanding from scratch");
            }

            // Empty the Instances table and expand from scratch.
            mDb.execSQL("DELETE FROM " + Tables.INSTANCES + ";");
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "acquireInstanceRangeLocked() deleted Instances,"
                        + " timezone changed: " + timezoneChanged);
            }
            mInstancesHelper.expandInstanceRangeLocked(expandBegin, expandEnd, instancesTimezone);

            mMetaData.writeLocked(instancesTimezone, expandBegin, expandEnd);

            String timezoneType = mCalendarCache.readTimezoneType();
            // This may cause some double writes but guarantees the time zone in
            // the db and the time zone the instances are in is the same, which
            // future changes may affect.
            mCalendarCache.writeTimezoneInstances(instancesTimezone);

            // If we're in auto check if we need to fix the previous tz value
            if (timezoneType.equals(CalendarCache.TIMEZONE_TYPE_AUTO)) {
                String prevTZ = mCalendarCache.readTimezoneInstancesPrevious();
                if (TextUtils.equals(TIMEZONE_GMT, prevTZ)) {
                    mCalendarCache.writeTimezoneInstancesPrevious(instancesTimezone);
                }
            }
            return;
        }

        // If the desired range [begin, end] has already been
        // expanded, then simply return.  The range is inclusive, that is,
        // events that touch either endpoint are included in the expansion.
        // This means that a zero-duration event that starts and ends at
        // the endpoint will be included.
        // We use [begin, end] here and not [expandBegin, expandEnd] for
        // checking the range because a common case is for the client to
        // request successive days or weeks, for example.  If we checked
        // that the expanded range [expandBegin, expandEnd] then we would
        // always be expanding because there would always be one more day
        // or week that hasn't been expanded.
        if ((begin >= minInstance) && (end <= maxInstance)) {
            if (DEBUG_INSTANCES) {
                Log.d(TAG + "-i", "instances are already expanded");
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Canceled instance query (" + expandBegin + ", " + expandEnd
                        + ") falls within previously expanded range.");
            }
            return;
        }

        // If the requested begin point has not been expanded, then include
        // more events than requested in the expansion (use "expandBegin").
        if (begin < minInstance) {
            mInstancesHelper.expandInstanceRangeLocked(expandBegin, minInstance, instancesTimezone);
            minInstance = expandBegin;
        }

        // If the requested end point has not been expanded, then include
        // more events than requested in the expansion (use "expandEnd").
        if (end > maxInstance) {
            mInstancesHelper.expandInstanceRangeLocked(maxInstance, expandEnd, instancesTimezone);
            maxInstance = expandEnd;
        }

        // Update the bounds on the Instances table.
        mMetaData.writeLocked(instancesTimezone, minInstance, maxInstance);
    }

    @Override
    public String getType(Uri url) {
        int match = sUriMatcher.match(url);
        switch (match) {
            case EVENTS:
                return "vnd.android.cursor.dir/event";
            case EVENTS_ID:
                return "vnd.android.cursor.item/event";
            case REMINDERS:
                return "vnd.android.cursor.dir/reminder";
            case REMINDERS_ID:
                return "vnd.android.cursor.item/reminder";
            case CALENDAR_ALERTS:
                return "vnd.android.cursor.dir/calendar-alert";
            case CALENDAR_ALERTS_BY_INSTANCE:
                return "vnd.android.cursor.dir/calendar-alert-by-instance";
            case CALENDAR_ALERTS_ID:
                return "vnd.android.cursor.item/calendar-alert";
            case INSTANCES:
            case INSTANCES_BY_DAY:
            case EVENT_DAYS:
                return "vnd.android.cursor.dir/event-instance";
            case TIME:
                return "time/epoch";
            case PROVIDER_PROPERTIES:
                return "vnd.android.cursor.dir/property";
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    /**
     * Determines if the event is recurrent, based on the provided values.
     */
    public static boolean isRecurrenceEvent(String rrule, String rdate, String originalId,
            String originalSyncId) {
        return (!TextUtils.isEmpty(rrule) ||
                !TextUtils.isEmpty(rdate) ||
                !TextUtils.isEmpty(originalId) ||
                !TextUtils.isEmpty(originalSyncId));
    }

    /**
     * Takes an event and corrects the hrs, mins, secs if it is an allDay event.
     * <p>
     * AllDay events should have hrs, mins, secs set to zero. This checks if this is true and
     * corrects the fields DTSTART, DTEND, and DURATION if necessary.
     *
     * @param values The values to check and correct
     * @param modValues Any updates will be stored here.  This may be the same object as
     *   <strong>values</strong>.
     * @return Returns true if a correction was necessary, false otherwise
     */
    private boolean fixAllDayTime(ContentValues values, ContentValues modValues) {
        Integer allDayObj = values.getAsInteger(Events.ALL_DAY);
        if (allDayObj == null || allDayObj == 0) {
            return false;
        }

        boolean neededCorrection = false;

        Long dtstart = values.getAsLong(Events.DTSTART);
        Long dtend = values.getAsLong(Events.DTEND);
        String duration = values.getAsString(Events.DURATION);
        Time time = new Time();
        String tempValue;

        // Change dtstart so h,m,s are 0 if necessary.
        time.clear(Time.TIMEZONE_UTC);
        time.set(dtstart.longValue());
        if (time.hour != 0 || time.minute != 0 || time.second != 0) {
            time.hour = 0;
            time.minute = 0;
            time.second = 0;
            modValues.put(Events.DTSTART, time.toMillis(true));
            neededCorrection = true;
        }

        // If dtend exists for this event make sure it's h,m,s are 0.
        if (dtend != null) {
            time.clear(Time.TIMEZONE_UTC);
            time.set(dtend.longValue());
            if (time.hour != 0 || time.minute != 0 || time.second != 0) {
                time.hour = 0;
                time.minute = 0;
                time.second = 0;
                dtend = time.toMillis(true);
                modValues.put(Events.DTEND, dtend);
                neededCorrection = true;
            }
        }

        if (duration != null) {
            int len = duration.length();
            /* duration is stored as either "P<seconds>S" or "P<days>D". This checks if it's
             * in the seconds format, and if so converts it to days.
             */
            if (len == 0) {
                duration = null;
            } else if (duration.charAt(0) == 'P' &&
                    duration.charAt(len - 1) == 'S') {
                int seconds = Integer.parseInt(duration.substring(1, len - 1));
                int days = (seconds + DAY_IN_SECONDS - 1) / DAY_IN_SECONDS;
                duration = "P" + days + "D";
                modValues.put(Events.DURATION, duration);
                neededCorrection = true;
            }
        }

        return neededCorrection;
    }


    /**
     * Determines whether the strings in the set name columns that may be overridden
     * when creating a recurring event exception.
     * <p>
     * This uses a white list because it screens out unknown columns and is a bit safer to
     * maintain than a black list.
     */
    private void checkAllowedInException(Set<String> keys) {
        for (String str : keys) {
            if (!ALLOWED_IN_EXCEPTION.contains(str.intern())) {
                throw new IllegalArgumentException("Exceptions can't overwrite " + str);
            }
        }
    }

    /**
     * Splits a recurrent event at a specified instance.  This is useful when modifying "this
     * and all future events".
     *<p>
     * If the recurrence rule has a COUNT specified, we need to split that at the point of the
     * exception.  If the exception is instance N (0-based), the original COUNT is reduced
     * to N, and the exception's COUNT is set to (COUNT - N).
     *<p>
     * If the recurrence doesn't have a COUNT, we need to update or introduce an UNTIL value,
     * so that the original recurrence will end just before the exception instance.  (Note
     * that UNTIL dates are inclusive.)
     *<p>
     * This should not be used to update the first instance ("update all events" action).
     *
     * @param values The original event values; must include EVENT_TIMEZONE and DTSTART.
     *        The RRULE value may be modified (with the expectation that this will propagate
     *        into the exception event).
     * @param endTimeMillis The time before which the event must end (i.e. the start time of the
     *        exception event instance).
     * @return Values to apply to the original event.
     */
    private static ContentValues setRecurrenceEnd(ContentValues values, long endTimeMillis) {
        boolean origAllDay = values.getAsBoolean(Events.ALL_DAY);
        String origRrule = values.getAsString(Events.RRULE);

        EventRecurrence origRecurrence = new EventRecurrence();
        origRecurrence.parse(origRrule);

        // Get the start time of the first instance in the original recurrence.
        long startTimeMillis = values.getAsLong(Events.DTSTART);
        Time dtstart = new Time();
        dtstart.timezone = values.getAsString(Events.EVENT_TIMEZONE);
        dtstart.set(startTimeMillis);

        ContentValues updateValues = new ContentValues();

        if (origRecurrence.count > 0) {
            /*
             * Generate the full set of instances for this recurrence, from the first to the
             * one just before endTimeMillis.  The list should never be empty, because this method
             * should not be called for the first instance.  All we're really interested in is
             * the *number* of instances found.
             */
            RecurrenceSet recurSet = new RecurrenceSet(values);
            RecurrenceProcessor recurProc = new RecurrenceProcessor();
            long[] recurrences;
            try {
                recurrences = recurProc.expand(dtstart, recurSet, startTimeMillis, endTimeMillis);
            } catch (DateException de) {
                throw new RuntimeException(de);
            }

            if (recurrences.length == 0) {
                throw new RuntimeException("can't use this method on first instance");
            }

            EventRecurrence excepRecurrence = new EventRecurrence();
            excepRecurrence.parse(origRrule); // TODO: add/use a copy constructor to EventRecurrence
            excepRecurrence.count -= recurrences.length;
            values.put(Events.RRULE, excepRecurrence.toString());

            origRecurrence.count = recurrences.length;

        } else {
            Time untilTime = new Time();

            // The "until" time must be in UTC time in order for Google calendar
            // to display it properly. For all-day events, the "until" time string
            // must include just the date field, and not the time field. The
            // repeating events repeat up to and including the "until" time.
            untilTime.timezone = Time.TIMEZONE_UTC;

            // Subtract one second from the exception begin time to get the "until" time.
            untilTime.set(endTimeMillis - 1000); // subtract one second (1000 millis)
            if (origAllDay) {
                untilTime.hour = untilTime.minute = untilTime.second = 0;
                untilTime.allDay = true;
                untilTime.normalize(false);

                // This should no longer be necessary -- DTSTART should already be in the correct
                // format for an all-day event.
                dtstart.hour = dtstart.minute = dtstart.second = 0;
                dtstart.allDay = true;
                dtstart.timezone = Time.TIMEZONE_UTC;
            }
            origRecurrence.until = untilTime.format2445();
        }

        updateValues.put(Events.RRULE, origRecurrence.toString());
        updateValues.put(Events.DTSTART, dtstart.normalize(true));
        return updateValues;
    }

    /**
     * Handles insertion of an exception to a recurring event.
     * <p>
     * There are two modes, selected based on the presence of "rrule" in modValues:
     * <ol>
     * <li> Create a single instance exception ("modify current event only").
     * <li> Cap the original event, and create a new recurring event ("modify this and all
     * future events").
     * </ol>
     * This may be used for "modify all instances of the event" by simply selecting the
     * very first instance as the exception target.  In that case, the ID of the "new"
     * exception event will be the same as the originalEventId.
     *
     * @param originalEventId The _id of the event to be modified
     * @param modValues Event columns to update
     * @param callerIsSyncAdapter Set if the content provider client is the sync adapter
     * @return the ID of the new "exception" event, or -1 on failure
     */
    private long handleInsertException(long originalEventId, ContentValues modValues,
            boolean callerIsSyncAdapter) {
        if (DEBUG_EXCEPTION) {
            Log.i(TAG, "RE: values: " + modValues.toString());
        }

        // Make sure they have specified an instance via originalInstanceTime.
        Long originalInstanceTime = modValues.getAsLong(Events.ORIGINAL_INSTANCE_TIME);
        if (originalInstanceTime == null) {
            throw new IllegalArgumentException("Exceptions must specify " +
                    Events.ORIGINAL_INSTANCE_TIME);
        }

        // Check for attempts to override values that shouldn't be touched.
        checkAllowedInException(modValues.keySet());

        // If this isn't the sync adapter, set the "dirty" flag in any Event we modify.
        if (!callerIsSyncAdapter) {
            modValues.put(Events.DIRTY, true);
            addMutator(modValues, Events.MUTATORS);
        }

        // Wrap all database accesses in a transaction.
        mDb.beginTransaction();
        Cursor cursor = null;
        try {
            // TODO: verify that there's an instance corresponding to the specified time
            //       (does this matter? it's weird, but not fatal?)

            // Grab the full set of columns for this event.
            cursor = mDb.query(Tables.EVENTS, null /* columns */,
                    SQL_WHERE_ID, new String[] { String.valueOf(originalEventId) },
                    null /* groupBy */, null /* having */, null /* sortOrder */);
            if (cursor.getCount() != 1) {
                Log.e(TAG, "Original event ID " + originalEventId + " lookup failed (count is " +
                        cursor.getCount() + ")");
                return -1;
            }
            //DatabaseUtils.dumpCursor(cursor);

            // If there's a color index check that it's valid
            String color_index = modValues.getAsString(Events.EVENT_COLOR_KEY);
            if (!TextUtils.isEmpty(color_index)) {
                int calIdCol = cursor.getColumnIndex(Events.CALENDAR_ID);
                Long calId = cursor.getLong(calIdCol);
                String accountName = null;
                String accountType = null;
                if (calId != null) {
                    Account account = getAccount(calId);
                    if (account != null) {
                        accountName = account.name;
                        accountType = account.type;
                    }
                }
                verifyColorExists(accountName, accountType, color_index, Colors.TYPE_EVENT);
            }

            /*
             * Verify that the original event is in fact a recurring event by checking for the
             * presence of an RRULE.  If it's there, we assume that the event is otherwise
             * properly constructed (e.g. no DTEND).
             */
            cursor.moveToFirst();
            int rruleCol = cursor.getColumnIndex(Events.RRULE);
            if (TextUtils.isEmpty(cursor.getString(rruleCol))) {
                Log.e(TAG, "Original event has no rrule");
                return -1;
            }
            if (DEBUG_EXCEPTION) {
                Log.d(TAG, "RE: old RRULE is " + cursor.getString(rruleCol));
            }

            // Verify that the original event is not itself a (single-instance) exception.
            int originalIdCol = cursor.getColumnIndex(Events.ORIGINAL_ID);
            if (!TextUtils.isEmpty(cursor.getString(originalIdCol))) {
                Log.e(TAG, "Original event is an exception");
                return -1;
            }

            boolean createSingleException = TextUtils.isEmpty(modValues.getAsString(Events.RRULE));

            // TODO: check for the presence of an existing exception on this event+instance?
            //       The caller should be modifying that, not creating another exception.
            //       (Alternatively, we could do that for them.)

            // Create a new ContentValues for the new event.  Start with the original event,
            // and drop in the new caller-supplied values.  This will set originalInstanceTime.
            ContentValues values = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(cursor, values);
            cursor.close();
            cursor = null;

            // TODO: if we're changing this to an all-day event, we should ensure that
            //       hours/mins/secs on DTSTART are zeroed out (before computing DTEND).
            //       See fixAllDayTime().

            boolean createNewEvent = true;
            if (createSingleException) {
                /*
                 * Save a copy of a few fields that will migrate to new places.
                 */
                String _id = values.getAsString(Events._ID);
                String _sync_id = values.getAsString(Events._SYNC_ID);
                boolean allDay = values.getAsBoolean(Events.ALL_DAY);

                /*
                 * Wipe out some fields that we don't want to clone into the exception event.
                 */
                for (String str : DONT_CLONE_INTO_EXCEPTION) {
                    values.remove(str);
                }

                /*
                 * Merge the new values on top of the existing values.  Note this sets
                 * originalInstanceTime.
                 */
                values.putAll(modValues);

                /*
                 * Copy some fields to their "original" counterparts:
                 *   _id --> original_id
                 *   _sync_id --> original_sync_id
                 *   allDay --> originalAllDay
                 *
                 * If this event hasn't been sync'ed with the server yet, the _sync_id field will
                 * be null.  We will need to fill original_sync_id in later.  (May not be able to
                 * do it right when our own _sync_id field gets populated, because the order of
                 * events from the server may not be what we want -- could update the exception
                 * before updating the original event.)
                 *
                 * _id is removed later (right before we write the event).
                 */
                values.put(Events.ORIGINAL_ID, _id);
                values.put(Events.ORIGINAL_SYNC_ID, _sync_id);
                values.put(Events.ORIGINAL_ALL_DAY, allDay);

                // Mark the exception event status as "tentative", unless the caller has some
                // other value in mind (like STATUS_CANCELED).
                if (!values.containsKey(Events.STATUS)) {
                    values.put(Events.STATUS, Events.STATUS_TENTATIVE);
                }

                // We're converting from recurring to non-recurring.
                // Clear out RRULE, RDATE, EXRULE & EXDATE
                // Replace DURATION with DTEND.
                values.remove(Events.RRULE);
                values.remove(Events.RDATE);
                values.remove(Events.EXRULE);
                values.remove(Events.EXDATE);

                Duration duration = new Duration();
                String durationStr = values.getAsString(Events.DURATION);
                try {
                    duration.parse(durationStr);
                } catch (Exception ex) {
                    // NullPointerException if the original event had no duration.
                    // DateException if the duration was malformed.
                    Log.w(TAG, "Bad duration in recurring event: " + durationStr, ex);
                    return -1;
                }

                /*
                 * We want to compute DTEND as an offset from the start time of the instance.
                 * If the caller specified a new value for DTSTART, we want to use that; if not,
                 * the DTSTART in "values" will be the start time of the first instance in the
                 * recurrence, so we want to replace it with ORIGINAL_INSTANCE_TIME.
                 */
                long start;
                if (modValues.containsKey(Events.DTSTART)) {
                    start = values.getAsLong(Events.DTSTART);
                } else {
                    start = values.getAsLong(Events.ORIGINAL_INSTANCE_TIME);
                    values.put(Events.DTSTART, start);
                }
                values.put(Events.DTEND, start + duration.getMillis());
                if (DEBUG_EXCEPTION) {
                    Log.d(TAG, "RE: ORIG_INST_TIME=" + start +
                            ", duration=" + duration.getMillis() +
                            ", generated DTEND=" + values.getAsLong(Events.DTEND));
                }
                values.remove(Events.DURATION);
            } else {
                /*
                 * We're going to "split" the recurring event, making the old one stop before
                 * this instance, and creating a new recurring event that starts here.
                 *
                 * No need to fill out the "original" fields -- the new event is not tied to
                 * the previous event in any way.
                 *
                 * If this is the first event in the series, we can just update the existing
                 * event with the values.
                 */
                boolean canceling = (values.getAsInteger(Events.STATUS) == Events.STATUS_CANCELED);

                if (originalInstanceTime.equals(values.getAsLong(Events.DTSTART))) {
                    /*
                     * Update fields in the existing event.  Rather than use the merged data
                     * from the cursor, we just do the update with the new value set after
                     * removing the ORIGINAL_INSTANCE_TIME entry.
                     */
                    if (canceling) {
                        // TODO: should we just call deleteEventInternal?
                        Log.d(TAG, "Note: canceling entire event via exception call");
                    }
                    if (DEBUG_EXCEPTION) {
                        Log.d(TAG, "RE: updating full event");
                    }
                    if (!validateRecurrenceRule(modValues)) {
                        throw new IllegalArgumentException("Invalid recurrence rule: " +
                                values.getAsString(Events.RRULE));
                    }
                    modValues.remove(Events.ORIGINAL_INSTANCE_TIME);
                    mDb.update(Tables.EVENTS, modValues, SQL_WHERE_ID,
                            new String[] { Long.toString(originalEventId) });
                    createNewEvent = false; // skip event creation and related-table cloning
                } else {
                    if (DEBUG_EXCEPTION) {
                        Log.d(TAG, "RE: splitting event");
                    }

                    /*
                     * Cap the original event so it ends just before the target instance.  In
                     * some cases (nonzero COUNT) this will also update the RRULE in "values",
                     * so that the exception we're creating terminates appropriately.  If a
                     * new RRULE was specified by the caller, the new rule will overwrite our
                     * changes when we merge the new values in below (which is the desired
                     * behavior).
                     */
                    ContentValues splitValues = setRecurrenceEnd(values, originalInstanceTime);
                    mDb.update(Tables.EVENTS, splitValues, SQL_WHERE_ID,
                            new String[] { Long.toString(originalEventId) });

                    /*
                     * Prepare the new event.  We remove originalInstanceTime, because we're now
                     * creating a new event rather than an exception.
                     *
                     * We're always cloning a non-exception event (we tested to make sure the
                     * event doesn't specify original_id, and we don't allow original_id in the
                     * modValues), so we shouldn't end up creating a new event that looks like
                     * an exception.
                     */
                    values.putAll(modValues);
                    values.remove(Events.ORIGINAL_INSTANCE_TIME);
                }
            }

            long newEventId;
            if (createNewEvent) {
                values.remove(Events._ID);      // don't try to set this explicitly
                if (callerIsSyncAdapter) {
                    scrubEventData(values, null);
                } else {
                    validateEventData(values);
                }

                newEventId = mDb.insert(Tables.EVENTS, null, values);
                if (newEventId < 0) {
                    Log.w(TAG, "Unable to add exception to recurring event");
                    Log.w(TAG, "Values: " + values);
                    return -1;
                }
                if (DEBUG_EXCEPTION) {
                    Log.d(TAG, "RE: new ID is " + newEventId);
                }

                // TODO: do we need to do something like this?
                //updateEventRawTimesLocked(id, updatedValues);

                /*
                 * Force re-computation of the Instances associated with the recurrence event.
                 */
                mInstancesHelper.updateInstancesLocked(values, newEventId, true, mDb);

                /*
                 * Some of the other tables (Attendees, Reminders, ExtendedProperties) reference
                 * the Event ID.  We need to copy the entries from the old event, filling in the
                 * new event ID, so that somebody doing a SELECT on those tables will find
                 * matching entries.
                 */
                CalendarDatabaseHelper.copyEventRelatedTables(mDb, newEventId, originalEventId);

                /*
                 * If we modified Event.selfAttendeeStatus, we need to keep the corresponding
                 * entry in the Attendees table in sync.
                 */
                if (modValues.containsKey(Events.SELF_ATTENDEE_STATUS)) {
                    /*
                     * Each Attendee is identified by email address.  To find the entry that
                     * corresponds to "self", we want to compare that address to the owner of
                     * the Calendar.  We're expecting to find one matching entry in Attendees.
                     */
                    long calendarId = values.getAsLong(Events.CALENDAR_ID);
                    String accountName = getOwner(calendarId);

                    if (accountName != null) {
                        ContentValues attValues = new ContentValues();
                        attValues.put(Attendees.ATTENDEE_STATUS,
                                modValues.getAsString(Events.SELF_ATTENDEE_STATUS));

                        if (DEBUG_EXCEPTION) {
                            Log.d(TAG, "Updating attendee status for event=" + newEventId +
                                    " name=" + accountName + " to " +
                                    attValues.getAsString(Attendees.ATTENDEE_STATUS));
                        }
                        int count = mDb.update(Tables.ATTENDEES, attValues,
                                Attendees.EVENT_ID + "=? AND " + Attendees.ATTENDEE_EMAIL + "=?",
                                new String[] { String.valueOf(newEventId), accountName });
                        if (count != 1 && count != 2) {
                            // We're only expecting one matching entry.  We might briefly see
                            // two during a server sync.
                            Log.e(TAG, "Attendee status update on event=" + newEventId
                                    + " touched " + count + " rows. Expected one or two rows.");
                            if (false) {
                                // This dumps PII in the log, don't ship with it enabled.
                                Cursor debugCursor = mDb.query(Tables.ATTENDEES, null,
                                        Attendees.EVENT_ID + "=? AND " +
                                            Attendees.ATTENDEE_EMAIL + "=?",
                                        new String[] { String.valueOf(newEventId), accountName },
                                        null, null, null);
                                DatabaseUtils.dumpCursor(debugCursor);
                                if (debugCursor != null) {
                                    debugCursor.close();
                                }
                            }
                            throw new RuntimeException("Status update WTF");
                        }
                    }
                }
            } else {
                /*
                 * Update any Instances changed by the update to this Event.
                 */
                mInstancesHelper.updateInstancesLocked(values, originalEventId, false, mDb);
                newEventId = originalEventId;
            }

            mDb.setTransactionSuccessful();
            return newEventId;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            mDb.endTransaction();
        }
    }

    /**
     * Fills in the originalId column for previously-created exceptions to this event.  If
     * this event is not recurring or does not have a _sync_id, this does nothing.
     * <p>
     * The server might send exceptions before the event they refer to.  When
     * this happens, the originalId field will not have been set in the
     * exception events (it's the recurrence events' _id field, so it can't be
     * known until the recurrence event is created).  When we add a recurrence
     * event with a non-empty _sync_id field, we write that event's _id to the
     * originalId field of any events whose originalSyncId matches _sync_id.
     * <p>
     * Note _sync_id is only expected to be unique within a particular calendar.
     *
     * @param id The ID of the Event
     * @param values Values for the Event being inserted
     */
    private void backfillExceptionOriginalIds(long id, ContentValues values) {
        String syncId = values.getAsString(Events._SYNC_ID);
        String rrule = values.getAsString(Events.RRULE);
        String rdate = values.getAsString(Events.RDATE);
        String calendarId = values.getAsString(Events.CALENDAR_ID);

        if (TextUtils.isEmpty(syncId) || TextUtils.isEmpty(calendarId) ||
                (TextUtils.isEmpty(rrule) && TextUtils.isEmpty(rdate))) {
            // Not a recurring event, or doesn't have a server-provided sync ID.
            return;
        }

        ContentValues originalValues = new ContentValues();
        originalValues.put(Events.ORIGINAL_ID, id);
        mDb.update(Tables.EVENTS, originalValues,
                Events.ORIGINAL_SYNC_ID + "=? AND " + Events.CALENDAR_ID + "=?",
                new String[] { syncId, calendarId });
    }

    @Override
    protected Uri insertInTransaction(Uri uri, ContentValues values, boolean callerIsSyncAdapter) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "insertInTransaction: " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        final int match = sUriMatcher.match(uri);
        verifyTransactionAllowed(TRANSACTION_INSERT, uri, values, callerIsSyncAdapter, match,
                null /* selection */, null /* selection args */);

        long id = 0;

        switch (match) {
            case SYNCSTATE:
                id = mDbHelper.getSyncState().insert(mDb, values);
                break;
            case EVENTS:
                if (!callerIsSyncAdapter) {
                    values.put(Events.DIRTY, 1);
                    addMutator(values, Events.MUTATORS);
                }
                if (!values.containsKey(Events.DTSTART)) {
                    if (values.containsKey(Events.ORIGINAL_SYNC_ID)
                            && values.containsKey(Events.ORIGINAL_INSTANCE_TIME)
                            && Events.STATUS_CANCELED == values.getAsInteger(Events.STATUS)) {
                        // event is a canceled instance of a recurring event, it doesn't these
                        // values but lets fake some to satisfy curious consumers.
                        final long origStart = values.getAsLong(Events.ORIGINAL_INSTANCE_TIME);
                        values.put(Events.DTSTART, origStart);
                        values.put(Events.DTEND, origStart);
                        values.put(Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC);
                    } else {
                        throw new RuntimeException("DTSTART field missing from event");
                    }
                }
                // TODO: do we really need to make a copy?
                ContentValues updatedValues = new ContentValues(values);
                if (callerIsSyncAdapter) {
                    scrubEventData(updatedValues, null);
                } else {
                    validateEventData(updatedValues);
                }
                // updateLastDate must be after validation, to ensure proper last date computation
                updatedValues = updateLastDate(updatedValues);
                if (updatedValues == null) {
                    throw new RuntimeException("Could not insert event.");
                    // return null;
                }
                Long calendar_id = updatedValues.getAsLong(Events.CALENDAR_ID);
                if (calendar_id == null) {
                    // validateEventData checks this for non-sync adapter
                    // inserts
                    throw new IllegalArgumentException("New events must specify a calendar id");
                }
                // Verify the color is valid if it is being set
                String color_id = updatedValues.getAsString(Events.EVENT_COLOR_KEY);
                if (!TextUtils.isEmpty(color_id)) {
                    Account account = getAccount(calendar_id);
                    String accountName = null;
                    String accountType = null;
                    if (account != null) {
                        accountName = account.name;
                        accountType = account.type;
                    }
                    int color = verifyColorExists(accountName, accountType, color_id,
                            Colors.TYPE_EVENT);
                    updatedValues.put(Events.EVENT_COLOR, color);
                }
                String owner = null;
                if (!updatedValues.containsKey(Events.ORGANIZER)) {
                    owner = getOwner(calendar_id);
                    // TODO: This isn't entirely correct.  If a guest is adding a recurrence
                    // exception to an event, the organizer should stay the original organizer.
                    // This value doesn't go to the server and it will get fixed on sync,
                    // so it shouldn't really matter.
                    if (owner != null) {
                        updatedValues.put(Events.ORGANIZER, owner);
                    }
                }
                if (updatedValues.containsKey(Events.ORIGINAL_SYNC_ID)
                        && !updatedValues.containsKey(Events.ORIGINAL_ID)) {
                    long originalId = getOriginalId(updatedValues
                            .getAsString(Events.ORIGINAL_SYNC_ID),
                            updatedValues.getAsString(Events.CALENDAR_ID));
                    if (originalId != -1) {
                        updatedValues.put(Events.ORIGINAL_ID, originalId);
                    }
                } else if (!updatedValues.containsKey(Events.ORIGINAL_SYNC_ID)
                        && updatedValues.containsKey(Events.ORIGINAL_ID)) {
                    String originalSyncId = getOriginalSyncId(updatedValues
                            .getAsLong(Events.ORIGINAL_ID));
                    if (!TextUtils.isEmpty(originalSyncId)) {
                        updatedValues.put(Events.ORIGINAL_SYNC_ID, originalSyncId);
                    }
                }
                if (fixAllDayTime(updatedValues, updatedValues)) {
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, "insertInTransaction: " +
                                "allDay is true but sec, min, hour were not 0.");
                    }
                }
                updatedValues.remove(Events.HAS_ALARM);     // should not be set by caller
                // Insert the row
                id = mDbHelper.eventsInsert(updatedValues);
                if (id != -1) {
                    updateEventRawTimesLocked(id, updatedValues);
                    mInstancesHelper.updateInstancesLocked(updatedValues, id,
                            true /* new event */, mDb);

                    // If we inserted a new event that specified the self-attendee
                    // status, then we need to add an entry to the attendees table.
                    if (values.containsKey(Events.SELF_ATTENDEE_STATUS)) {
                        int status = values.getAsInteger(Events.SELF_ATTENDEE_STATUS);
                        if (owner == null) {
                            owner = getOwner(calendar_id);
                        }
                        createAttendeeEntry(id, status, owner);
                    }

                    backfillExceptionOriginalIds(id, values);

                    sendUpdateNotification(id, callerIsSyncAdapter);
                }
                break;
            case EXCEPTION_ID:
                long originalEventId = ContentUris.parseId(uri);
                id = handleInsertException(originalEventId, values, callerIsSyncAdapter);
                break;
            case CALENDARS:
                // TODO: verify that all required fields are present
                Integer syncEvents = values.getAsInteger(Calendars.SYNC_EVENTS);
                if (syncEvents != null && syncEvents == 1) {
                    String accountName = values.getAsString(Calendars.ACCOUNT_NAME);
                    String accountType = values.getAsString(
                            Calendars.ACCOUNT_TYPE);
                    final Account account = new Account(accountName, accountType);
                    String eventsUrl = values.getAsString(Calendars.CAL_SYNC1);
                    mDbHelper.scheduleSync(account, false /* two-way sync */, eventsUrl);
                }
                String cal_color_id = values.getAsString(Calendars.CALENDAR_COLOR_KEY);
                if (!TextUtils.isEmpty(cal_color_id)) {
                    String accountName = values.getAsString(Calendars.ACCOUNT_NAME);
                    String accountType = values.getAsString(Calendars.ACCOUNT_TYPE);
                    int color = verifyColorExists(accountName, accountType, cal_color_id,
                            Colors.TYPE_CALENDAR);
                    values.put(Calendars.CALENDAR_COLOR, color);
                }
                id = mDbHelper.calendarsInsert(values);
                sendUpdateNotification(id, callerIsSyncAdapter);
                break;
            case COLORS:
                // verifyTransactionAllowed requires this be from a sync
                // adapter, all of the required fields are marked NOT NULL in
                // the db. TODO Do we need explicit checks here or should we
                // just let sqlite throw if something isn't specified?
                String accountName = uri.getQueryParameter(Colors.ACCOUNT_NAME);
                String accountType = uri.getQueryParameter(Colors.ACCOUNT_TYPE);
                String colorIndex = values.getAsString(Colors.COLOR_KEY);
                if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
                    throw new IllegalArgumentException("Account name and type must be non"
                            + " empty parameters for " + uri);
                }
                if (TextUtils.isEmpty(colorIndex)) {
                    throw new IllegalArgumentException("COLOR_INDEX must be non empty for " + uri);
                }
                if (!values.containsKey(Colors.COLOR_TYPE) || !values.containsKey(Colors.COLOR)) {
                    throw new IllegalArgumentException(
                            "New colors must contain COLOR_TYPE and COLOR");
                }
                // Make sure the account we're inserting for is the same one the
                // adapter is claiming to be. TODO should we throw if they
                // aren't the same?
                values.put(Colors.ACCOUNT_NAME, accountName);
                values.put(Colors.ACCOUNT_TYPE, accountType);

                // Verify the color doesn't already exist
                Cursor c = null;
                try {
                    final long colorType = values.getAsLong(Colors.COLOR_TYPE);
                    c = getColorByTypeIndex(accountName, accountType, colorType, colorIndex);
                    if (c.getCount() != 0) {
                        throw new IllegalArgumentException("color type " + colorType
                                + " and index " + colorIndex
                                + " already exists for account and type provided");
                    }
                } finally {
                    if (c != null)
                        c.close();
                }
                id = mDbHelper.colorsInsert(values);
                break;
            case ATTENDEES:
                if (!values.containsKey(Attendees.EVENT_ID)) {
                    throw new IllegalArgumentException("Attendees values must "
                            + "contain an event_id");
                }
                if (!callerIsSyncAdapter) {
                    final Long eventId = values.getAsLong(Attendees.EVENT_ID);
                    mDbHelper.duplicateEvent(eventId);
                    setEventDirty(eventId);
                }
                id = mDbHelper.attendeesInsert(values);

                // Copy the attendee status value to the Events table.
                updateEventAttendeeStatus(mDb, values);
                break;
            case REMINDERS:
            {
                Long eventIdObj = values.getAsLong(Reminders.EVENT_ID);
                if (eventIdObj == null) {
                    throw new IllegalArgumentException("Reminders values must "
                            + "contain a numeric event_id");
                }
                if (!callerIsSyncAdapter) {
                    mDbHelper.duplicateEvent(eventIdObj);
                    setEventDirty(eventIdObj);
                }
                id = mDbHelper.remindersInsert(values);

                // We know this event has at least one reminder, so make sure "hasAlarm" is 1.
                setHasAlarm(eventIdObj, 1);

                // Schedule another event alarm, if necessary
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "insertInternal() changing reminder");
                }
                mCalendarAlarm.scheduleNextAlarm(false /* do not remove alarms */);
                break;
            }
            case CALENDAR_ALERTS:
                if (!values.containsKey(CalendarAlerts.EVENT_ID)) {
                    throw new IllegalArgumentException("CalendarAlerts values must "
                            + "contain an event_id");
                }
                id = mDbHelper.calendarAlertsInsert(values);
                // Note: dirty bit is not set for Alerts because it is not synced.
                // It is generated from Reminders, which is synced.
                break;
            case EXTENDED_PROPERTIES:
                if (!values.containsKey(CalendarContract.ExtendedProperties.EVENT_ID)) {
                    throw new IllegalArgumentException("ExtendedProperties values must "
                            + "contain an event_id");
                }
                if (!callerIsSyncAdapter) {
                    final Long eventId = values
                            .getAsLong(CalendarContract.ExtendedProperties.EVENT_ID);
                    mDbHelper.duplicateEvent(eventId);
                    setEventDirty(eventId);
                }
                id = mDbHelper.extendedPropertiesInsert(values);
                break;
            case EMMA:
                // Special target used during code-coverage evaluation.
                handleEmmaRequest(values);
                break;
            case EVENTS_ID:
            case REMINDERS_ID:
            case CALENDAR_ALERTS_ID:
            case EXTENDED_PROPERTIES_ID:
            case INSTANCES:
            case INSTANCES_BY_DAY:
            case EVENT_DAYS:
            case PROVIDER_PROPERTIES:
                throw new UnsupportedOperationException("Cannot insert into that URL: " + uri);
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }

        if (id < 0) {
            return null;
        }

        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Handles special commands related to EMMA code-coverage testing.
     *
     * @param values Parameters from the caller.
     */
    private static void handleEmmaRequest(ContentValues values) {
        /*
         * This is not part of the public API, so we can't share constants with the CTS
         * test code.
         *
         * Bad requests, or attempting to request EMMA coverage data when the coverage libs
         * aren't linked in, will cause an exception.
         */
        String cmd = values.getAsString("cmd");
        if (cmd.equals("start")) {
            // We'd like to reset the coverage data, but according to FAQ item 3.14 at
            // http://emma.sourceforge.net/faq.html, this isn't possible in 2.0.
            Log.d(TAG, "Emma coverage testing started");
        } else if (cmd.equals("stop")) {
            // Call com.vladium.emma.rt.RT.dumpCoverageData() to cause a data dump.  We
            // may not have been built with EMMA, so we need to do this through reflection.
            String filename = values.getAsString("outputFileName");

            File coverageFile = new File(filename);
            try {
                Class<?> emmaRTClass = Class.forName("com.vladium.emma.rt.RT");
                Method dumpCoverageMethod = emmaRTClass.getMethod("dumpCoverageData",
                        coverageFile.getClass(), boolean.class, boolean.class);

                dumpCoverageMethod.invoke(null, coverageFile, false /*merge*/,
                        false /*stopDataCollection*/);
                Log.d(TAG, "Emma coverage data written to " + filename);
            } catch (Exception e) {
                throw new RuntimeException("Emma coverage dump failed", e);
            }
        }
    }

    /**
     * Validates the recurrence rule, if any.  We allow single- and multi-rule RRULEs.
     * <p>
     * TODO: Validate RDATE, EXRULE, EXDATE (possibly passing in an indication of whether we
     * believe we have the full set, so we can reject EXRULE when not accompanied by RRULE).
     *
     * @return A boolean indicating successful validation.
     */
    private boolean validateRecurrenceRule(ContentValues values) {
        String rrule = values.getAsString(Events.RRULE);

        if (!TextUtils.isEmpty(rrule)) {
            String[] ruleList = rrule.split("\n");
            for (String recur : ruleList) {
                EventRecurrence er = new EventRecurrence();
                try {
                    er.parse(recur);
                } catch (EventRecurrence.InvalidFormatException ife) {
                    Log.w(TAG, "Invalid recurrence rule: " + recur);
                    dumpEventNoPII(values);
                    return false;
                }
            }
        }

        return true;
    }

    private void dumpEventNoPII(ContentValues values) {
        if (values == null) {
            return;
        }

        StringBuilder bob = new StringBuilder();
        bob.append("dtStart:       ").append(values.getAsLong(Events.DTSTART));
        bob.append("\ndtEnd:         ").append(values.getAsLong(Events.DTEND));
        bob.append("\nall_day:       ").append(values.getAsInteger(Events.ALL_DAY));
        bob.append("\ntz:            ").append(values.getAsString(Events.EVENT_TIMEZONE));
        bob.append("\ndur:           ").append(values.getAsString(Events.DURATION));
        bob.append("\nrrule:         ").append(values.getAsString(Events.RRULE));
        bob.append("\nrdate:         ").append(values.getAsString(Events.RDATE));
        bob.append("\nlast_date:     ").append(values.getAsLong(Events.LAST_DATE));

        bob.append("\nid:            ").append(values.getAsLong(Events._ID));
        bob.append("\nsync_id:       ").append(values.getAsString(Events._SYNC_ID));
        bob.append("\nori_id:        ").append(values.getAsLong(Events.ORIGINAL_ID));
        bob.append("\nori_sync_id:   ").append(values.getAsString(Events.ORIGINAL_SYNC_ID));
        bob.append("\nori_inst_time: ").append(values.getAsLong(Events.ORIGINAL_INSTANCE_TIME));
        bob.append("\nori_all_day:   ").append(values.getAsInteger(Events.ORIGINAL_ALL_DAY));

        Log.i(TAG, bob.toString());
    }

    /**
     * Do some scrubbing on event data before inserting or updating. In particular make
     * dtend, duration, etc make sense for the type of event (regular, recurrence, exception).
     * Remove any unexpected fields.
     *
     * @param values the ContentValues to insert.
     * @param modValues if non-null, explicit null entries will be added here whenever something
     *   is removed from <strong>values</strong>.
     */
    private void scrubEventData(ContentValues values, ContentValues modValues) {
        boolean hasDtend = values.getAsLong(Events.DTEND) != null;
        boolean hasDuration = !TextUtils.isEmpty(values.getAsString(Events.DURATION));
        boolean hasRrule = !TextUtils.isEmpty(values.getAsString(Events.RRULE));
        boolean hasRdate = !TextUtils.isEmpty(values.getAsString(Events.RDATE));
        boolean hasOriginalEvent = !TextUtils.isEmpty(values.getAsString(Events.ORIGINAL_SYNC_ID));
        boolean hasOriginalInstanceTime = values.getAsLong(Events.ORIGINAL_INSTANCE_TIME) != null;
        if (hasRrule || hasRdate) {
            // Recurrence:
            // dtstart is start time of first event
            // dtend is null
            // duration is the duration of the event
            // rrule is a valid recurrence rule
            // lastDate is the end of the last event or null if it repeats forever
            // originalEvent is null
            // originalInstanceTime is null
            if (!validateRecurrenceRule(values)) {
                throw new IllegalArgumentException("Invalid recurrence rule: " +
                        values.getAsString(Events.RRULE));
            }
            if (hasDtend || !hasDuration || hasOriginalEvent || hasOriginalInstanceTime) {
                Log.d(TAG, "Scrubbing DTEND, ORIGINAL_SYNC_ID, ORIGINAL_INSTANCE_TIME");
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Invalid values for recurrence: " + values);
                }
                values.remove(Events.DTEND);
                values.remove(Events.ORIGINAL_SYNC_ID);
                values.remove(Events.ORIGINAL_INSTANCE_TIME);
                if (modValues != null) {
                    modValues.putNull(Events.DTEND);
                    modValues.putNull(Events.ORIGINAL_SYNC_ID);
                    modValues.putNull(Events.ORIGINAL_INSTANCE_TIME);
                }
            }
        } else if (hasOriginalEvent || hasOriginalInstanceTime) {
            // Recurrence exception
            // dtstart is start time of exception event
            // dtend is end time of exception event
            // duration is null
            // rrule is null
            // lastdate is same as dtend
            // originalEvent is the _sync_id of the recurrence
            // originalInstanceTime is the start time of the event being replaced
            if (!hasDtend || hasDuration || !hasOriginalEvent || !hasOriginalInstanceTime) {
                Log.d(TAG, "Scrubbing DURATION");
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Invalid values for recurrence exception: " + values);
                }
                values.remove(Events.DURATION);
                if (modValues != null) {
                    modValues.putNull(Events.DURATION);
                }
            }
        } else {
            // Regular event
            // dtstart is the start time
            // dtend is the end time
            // duration is null
            // rrule is null
            // lastDate is the same as dtend
            // originalEvent is null
            // originalInstanceTime is null
            if (!hasDtend || hasDuration) {
                Log.d(TAG, "Scrubbing DURATION");
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Invalid values for event: " + values);
                }
                values.remove(Events.DURATION);
                if (modValues != null) {
                    modValues.putNull(Events.DURATION);
                }
            }
        }
    }

    /**
     * Validates event data.  Pass in the full set of values for the event (i.e. not just
     * a part that's being updated).
     *
     * @param values Event data.
     * @throws IllegalArgumentException if bad data is found.
     */
    private void validateEventData(ContentValues values) {
        if (TextUtils.isEmpty(values.getAsString(Events.CALENDAR_ID))) {
            throw new IllegalArgumentException("Event values must include a calendar_id");
        }
        if (TextUtils.isEmpty(values.getAsString(Events.EVENT_TIMEZONE))) {
            throw new IllegalArgumentException("Event values must include an eventTimezone");
        }

        boolean hasDtstart = values.getAsLong(Events.DTSTART) != null;
        boolean hasDtend = values.getAsLong(Events.DTEND) != null;
        boolean hasDuration = !TextUtils.isEmpty(values.getAsString(Events.DURATION));
        boolean hasRrule = !TextUtils.isEmpty(values.getAsString(Events.RRULE));
        boolean hasRdate = !TextUtils.isEmpty(values.getAsString(Events.RDATE));
        if (hasRrule || hasRdate) {
            if (!validateRecurrenceRule(values)) {
                throw new IllegalArgumentException("Invalid recurrence rule: " +
                        values.getAsString(Events.RRULE));
            }
        }

        if (!hasDtstart) {
            dumpEventNoPII(values);
            throw new IllegalArgumentException("DTSTART cannot be empty.");
        }
        if (!hasDuration && !hasDtend) {
            dumpEventNoPII(values);
            throw new IllegalArgumentException("DTEND and DURATION cannot both be null for " +
                    "an event.");
        }
        if (hasDuration && hasDtend) {
            dumpEventNoPII(values);
            throw new IllegalArgumentException("Cannot have both DTEND and DURATION in an event");
        }
    }

    private void setEventDirty(long eventId) {
        final String mutators = DatabaseUtils.stringForQuery(
                mDb,
                SQL_QUERY_EVENT_MUTATORS,
                new String[]{String.valueOf(eventId)});
        final String packageName = getCallingPackageName();
        final String newMutators;
        if (TextUtils.isEmpty(mutators)) {
            newMutators = packageName;
        } else  {
            final String[] strings = mutators.split(",");
            boolean found = false;
            for (String string : strings) {
                if (string.equals(packageName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                newMutators = mutators + "," + packageName;
            } else {
                newMutators = mutators;
            }
        }
        mDb.execSQL(SQL_UPDATE_EVENT_SET_DIRTY_AND_MUTATORS,
                new Object[] {newMutators, eventId});
    }

    private long getOriginalId(String originalSyncId, String calendarId) {
        if (TextUtils.isEmpty(originalSyncId) || TextUtils.isEmpty(calendarId)) {
            return -1;
        }
        // Get the original id for this event
        long originalId = -1;
        Cursor c = null;
        try {
            c = query(Events.CONTENT_URI, ID_ONLY_PROJECTION,
                    Events._SYNC_ID + "=?"  + " AND " + Events.CALENDAR_ID + "=?",
                    new String[] {originalSyncId, calendarId}, null);
            if (c != null && c.moveToFirst()) {
                originalId = c.getLong(0);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return originalId;
    }

    private String getOriginalSyncId(long originalId) {
        if (originalId == -1) {
            return null;
        }
        // Get the original id for this event
        String originalSyncId = null;
        Cursor c = null;
        try {
            c = query(Events.CONTENT_URI, new String[] {Events._SYNC_ID},
                    Events._ID + "=?", new String[] {Long.toString(originalId)}, null);
            if (c != null && c.moveToFirst()) {
                originalSyncId = c.getString(0);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return originalSyncId;
    }

    private Cursor getColorByTypeIndex(String accountName, String accountType, long colorType,
            String colorIndex) {
        return mDb.query(Tables.COLORS, COLORS_PROJECTION, COLOR_FULL_SELECTION, new String[] {
                accountName, accountType, Long.toString(colorType), colorIndex
        }, null, null, null);
    }

    /**
     * Gets a calendar's "owner account", i.e. the e-mail address of the owner of the calendar.
     *
     * @param calId The calendar ID.
     * @return email of owner or null
     */
    private String getOwner(long calId) {
        if (calId < 0) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Calendar Id is not valid: " + calId);
            }
            return null;
        }
        // Get the email address of this user from this Calendar
        String emailAddress = null;
        Cursor cursor = null;
        try {
            cursor = query(ContentUris.withAppendedId(Calendars.CONTENT_URI, calId),
                    new String[] { Calendars.OWNER_ACCOUNT },
                    null /* selection */,
                    null /* selectionArgs */,
                    null /* sort */);
            if (cursor == null || !cursor.moveToFirst()) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Couldn't find " + calId + " in Calendars table");
                }
                return null;
            }
            emailAddress = cursor.getString(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return emailAddress;
    }

    private Account getAccount(long calId) {
        Account account = null;
        Cursor cursor = null;
        try {
            cursor = query(ContentUris.withAppendedId(Calendars.CONTENT_URI, calId),
                    ACCOUNT_PROJECTION, null /* selection */, null /* selectionArgs */,
                    null /* sort */);
            if (cursor == null || !cursor.moveToFirst()) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Couldn't find " + calId + " in Calendars table");
                }
                return null;
            }
            account = new Account(cursor.getString(ACCOUNT_NAME_INDEX),
                    cursor.getString(ACCOUNT_TYPE_INDEX));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return account;
    }

    /**
     * Creates an entry in the Attendees table that refers to the given event
     * and that has the given response status.
     *
     * @param eventId the event id that the new entry in the Attendees table
     * should refer to
     * @param status the response status
     * @param emailAddress the email of the attendee
     */
    private void createAttendeeEntry(long eventId, int status, String emailAddress) {
        ContentValues values = new ContentValues();
        values.put(Attendees.EVENT_ID, eventId);
        values.put(Attendees.ATTENDEE_STATUS, status);
        values.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_NONE);
        // TODO: The relationship could actually be ORGANIZER, but it will get straightened out
        // on sync.
        values.put(Attendees.ATTENDEE_RELATIONSHIP,
                Attendees.RELATIONSHIP_ATTENDEE);
        values.put(Attendees.ATTENDEE_EMAIL, emailAddress);

        // We don't know the ATTENDEE_NAME but that will be filled in by the
        // server and sent back to us.
        mDbHelper.attendeesInsert(values);
    }

    /**
     * Updates the attendee status in the Events table to be consistent with
     * the value in the Attendees table.
     *
     * @param db the database
     * @param attendeeValues the column values for one row in the Attendees table.
     */
    private void updateEventAttendeeStatus(SQLiteDatabase db, ContentValues attendeeValues) {
        // Get the event id for this attendee
        Long eventIdObj = attendeeValues.getAsLong(Attendees.EVENT_ID);
        if (eventIdObj == null) {
            Log.w(TAG, "Attendee update values don't include an event_id");
            return;
        }
        long eventId = eventIdObj;

        if (MULTIPLE_ATTENDEES_PER_EVENT) {
            // Get the calendar id for this event
            Cursor cursor = null;
            long calId;
            try {
                cursor = query(ContentUris.withAppendedId(Events.CONTENT_URI, eventId),
                        new String[] { Events.CALENDAR_ID },
                        null /* selection */,
                        null /* selectionArgs */,
                        null /* sort */);
                if (cursor == null || !cursor.moveToFirst()) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Couldn't find " + eventId + " in Events table");
                    }
                    return;
                }
                calId = cursor.getLong(0);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            // Get the owner email for this Calendar
            String calendarEmail = null;
            cursor = null;
            try {
                cursor = query(ContentUris.withAppendedId(Calendars.CONTENT_URI, calId),
                        new String[] { Calendars.OWNER_ACCOUNT },
                        null /* selection */,
                        null /* selectionArgs */,
                        null /* sort */);
                if (cursor == null || !cursor.moveToFirst()) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Couldn't find " + calId + " in Calendars table");
                    }
                    return;
                }
                calendarEmail = cursor.getString(0);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            if (calendarEmail == null) {
                return;
            }

            // Get the email address for this attendee
            String attendeeEmail = null;
            if (attendeeValues.containsKey(Attendees.ATTENDEE_EMAIL)) {
                attendeeEmail = attendeeValues.getAsString(Attendees.ATTENDEE_EMAIL);
            }

            // If the attendee email does not match the calendar email, then this
            // attendee is not the owner of this calendar so we don't update the
            // selfAttendeeStatus in the event.
            if (!calendarEmail.equals(attendeeEmail)) {
                return;
            }
        }

        // Select a default value for "status" based on the relationship.
        int status = Attendees.ATTENDEE_STATUS_NONE;
        Integer relationObj = attendeeValues.getAsInteger(Attendees.ATTENDEE_RELATIONSHIP);
        if (relationObj != null) {
            int rel = relationObj;
            if (rel == Attendees.RELATIONSHIP_ORGANIZER) {
                status = Attendees.ATTENDEE_STATUS_ACCEPTED;
            }
        }

        // If the status is specified, use that.
        Integer statusObj = attendeeValues.getAsInteger(Attendees.ATTENDEE_STATUS);
        if (statusObj != null) {
            status = statusObj;
        }

        ContentValues values = new ContentValues();
        values.put(Events.SELF_ATTENDEE_STATUS, status);
        db.update(Tables.EVENTS, values, SQL_WHERE_ID,
                new String[] {String.valueOf(eventId)});
    }

    /**
     * Set the "hasAlarm" column in the database.
     *
     * @param eventId The _id of the Event to update.
     * @param val The value to set it to (0 or 1).
     */
    private void setHasAlarm(long eventId, int val) {
        ContentValues values = new ContentValues();
        values.put(Events.HAS_ALARM, val);
        int count = mDb.update(Tables.EVENTS, values, SQL_WHERE_ID,
                new String[] { String.valueOf(eventId) });
        if (count != 1) {
            Log.w(TAG, "setHasAlarm on event " + eventId + " updated " + count +
                    " rows (expected 1)");
        }
    }

    /**
     * Calculates the "last date" of the event.  For a regular event this is the start time
     * plus the duration.  For a recurring event this is the start date of the last event in
     * the recurrence, plus the duration.  The event recurs forever, this returns -1.  If
     * the recurrence rule can't be parsed, this returns -1.
     *
     * @param values
     * @return the date, in milliseconds, since the start of the epoch (UTC), or -1 if an
     *   exceptional condition exists.
     * @throws DateException
     */
    long calculateLastDate(ContentValues values)
            throws DateException {
        // Allow updates to some event fields like the title or hasAlarm
        // without requiring DTSTART.
        if (!values.containsKey(Events.DTSTART)) {
            if (values.containsKey(Events.DTEND) || values.containsKey(Events.RRULE)
                    || values.containsKey(Events.DURATION)
                    || values.containsKey(Events.EVENT_TIMEZONE)
                    || values.containsKey(Events.RDATE)
                    || values.containsKey(Events.EXRULE)
                    || values.containsKey(Events.EXDATE)) {
                throw new RuntimeException("DTSTART field missing from event");
            }
            return -1;
        }
        long dtstartMillis = values.getAsLong(Events.DTSTART);
        long lastMillis = -1;

        // Can we use dtend with a repeating event?  What does that even
        // mean?
        // NOTE: if the repeating event has a dtend, we convert it to a
        // duration during event processing, so this situation should not
        // occur.
        Long dtEnd = values.getAsLong(Events.DTEND);
        if (dtEnd != null) {
            lastMillis = dtEnd;
        } else {
            // find out how long it is
            Duration duration = new Duration();
            String durationStr = values.getAsString(Events.DURATION);
            if (durationStr != null) {
                duration.parse(durationStr);
            }

            RecurrenceSet recur = null;
            try {
                recur = new RecurrenceSet(values);
            } catch (EventRecurrence.InvalidFormatException e) {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Could not parse RRULE recurrence string: " +
                            values.get(CalendarContract.Events.RRULE), e);
                }
                // TODO: this should throw an exception or return a distinct error code
                return lastMillis; // -1
            }

            if (null != recur && recur.hasRecurrence()) {
                // the event is repeating, so find the last date it
                // could appear on

                String tz = values.getAsString(Events.EVENT_TIMEZONE);

                if (TextUtils.isEmpty(tz)) {
                    // floating timezone
                    tz = Time.TIMEZONE_UTC;
                }
                Time dtstartLocal = new Time(tz);

                dtstartLocal.set(dtstartMillis);

                RecurrenceProcessor rp = new RecurrenceProcessor();
                lastMillis = rp.getLastOccurence(dtstartLocal, recur);
                if (lastMillis == -1) {
                    // repeats forever
                    return lastMillis;  // -1
                }
            } else {
                // the event is not repeating, just use dtstartMillis
                lastMillis = dtstartMillis;
            }

            // that was the beginning of the event.  this is the end.
            lastMillis = duration.addTo(lastMillis);
        }
        return lastMillis;
    }

    /**
     * Add LAST_DATE to values.
     * @param values the ContentValues (in/out); must include DTSTART and, if the event is
     *   recurring, the columns necessary to process a recurrence rule (RRULE, DURATION,
     *   EVENT_TIMEZONE, etc).
     * @return values on success, null on failure
     */
    private ContentValues updateLastDate(ContentValues values) {
        try {
            long last = calculateLastDate(values);
            if (last != -1) {
                values.put(Events.LAST_DATE, last);
            }

            return values;
        } catch (DateException e) {
            // don't add it if there was an error
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Could not calculate last date.", e);
            }
            return null;
        }
    }

    /**
     * Creates or updates an entry in the EventsRawTimes table.
     *
     * @param eventId The ID of the event that was just created or is being updated.
     * @param values For a new event, the full set of event values; for an updated event,
     *   the set of values that are being changed.
     */
    private void updateEventRawTimesLocked(long eventId, ContentValues values) {
        ContentValues rawValues = new ContentValues();

        rawValues.put(CalendarContract.EventsRawTimes.EVENT_ID, eventId);

        String timezone = values.getAsString(Events.EVENT_TIMEZONE);

        boolean allDay = false;
        Integer allDayInteger = values.getAsInteger(Events.ALL_DAY);
        if (allDayInteger != null) {
            allDay = allDayInteger != 0;
        }

        if (allDay || TextUtils.isEmpty(timezone)) {
            // floating timezone
            timezone = Time.TIMEZONE_UTC;
        }

        Time time = new Time(timezone);
        time.allDay = allDay;
        Long dtstartMillis = values.getAsLong(Events.DTSTART);
        if (dtstartMillis != null) {
            time.set(dtstartMillis);
            rawValues.put(CalendarContract.EventsRawTimes.DTSTART_2445, time.format2445());
        }

        Long dtendMillis = values.getAsLong(Events.DTEND);
        if (dtendMillis != null) {
            time.set(dtendMillis);
            rawValues.put(CalendarContract.EventsRawTimes.DTEND_2445, time.format2445());
        }

        Long originalInstanceMillis = values.getAsLong(Events.ORIGINAL_INSTANCE_TIME);
        if (originalInstanceMillis != null) {
            // This is a recurrence exception so we need to get the all-day
            // status of the original recurring event in order to format the
            // date correctly.
            allDayInteger = values.getAsInteger(Events.ORIGINAL_ALL_DAY);
            if (allDayInteger != null) {
                time.allDay = allDayInteger != 0;
            }
            time.set(originalInstanceMillis);
            rawValues.put(CalendarContract.EventsRawTimes.ORIGINAL_INSTANCE_TIME_2445,
                    time.format2445());
        }

        Long lastDateMillis = values.getAsLong(Events.LAST_DATE);
        if (lastDateMillis != null) {
            time.allDay = allDay;
            time.set(lastDateMillis);
            rawValues.put(CalendarContract.EventsRawTimes.LAST_DATE_2445, time.format2445());
        }

        mDbHelper.eventsRawTimesReplace(rawValues);
    }

    @Override
    protected int deleteInTransaction(Uri uri, String selection, String[] selectionArgs,
            boolean callerIsSyncAdapter) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "deleteInTransaction: " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        final int match = sUriMatcher.match(uri);
        verifyTransactionAllowed(TRANSACTION_DELETE, uri, null, callerIsSyncAdapter, match,
                selection, selectionArgs);

        switch (match) {
            case SYNCSTATE:
                return mDbHelper.getSyncState().delete(mDb, selection, selectionArgs);

            case SYNCSTATE_ID:
                String selectionWithId = (SyncState._ID + "=?")
                        + (selection == null ? "" : " AND (" + selection + ")");
                // Prepend id to selectionArgs
                selectionArgs = insertSelectionArg(selectionArgs,
                        String.valueOf(ContentUris.parseId(uri)));
                return mDbHelper.getSyncState().delete(mDb, selectionWithId,
                        selectionArgs);

            case COLORS:
                return deleteMatchingColors(appendAccountToSelection(uri, selection,
                        Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE),
                        selectionArgs);

            case EVENTS:
            {
                int result = 0;
                selection = appendAccountToSelection(
                        uri, selection, Events.ACCOUNT_NAME, Events.ACCOUNT_TYPE);

                // Query this event to get the ids to delete.
                Cursor cursor = mDb.query(Views.EVENTS, ID_ONLY_PROJECTION,
                        selection, selectionArgs, null /* groupBy */,
                        null /* having */, null /* sortOrder */);
                try {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(0);
                        result += deleteEventInternal(id, callerIsSyncAdapter, true /* isBatch */);
                    }
                    mCalendarAlarm.scheduleNextAlarm(false /* do not remove alarms */);
                    sendUpdateNotification(callerIsSyncAdapter);
                } finally {
                    cursor.close();
                    cursor = null;
                }
                return result;
            }
            case EVENTS_ID:
            {
                long id = ContentUris.parseId(uri);
                return deleteEventInternal(id, callerIsSyncAdapter, false /* isBatch */);
            }
            case EXCEPTION_ID2:
            {
                // This will throw NumberFormatException on missing or malformed input.
                List<String> segments = uri.getPathSegments();
                long eventId = Long.parseLong(segments.get(1));
                long excepId = Long.parseLong(segments.get(2));
                // TODO: verify that this is an exception instance (has an ORIGINAL_ID field
                //       that matches the supplied eventId)
                return deleteEventInternal(excepId, callerIsSyncAdapter, false /* isBatch */);
            }
            case ATTENDEES:
            {
                if (callerIsSyncAdapter) {
                    return mDb.delete(Tables.ATTENDEES, selection, selectionArgs);
                } else {
                    return deleteFromEventRelatedTable(Tables.ATTENDEES, uri, selection,
                            selectionArgs);
                }
            }
            case ATTENDEES_ID:
            {
                if (callerIsSyncAdapter) {
                    long id = ContentUris.parseId(uri);
                    return mDb.delete(Tables.ATTENDEES, SQL_WHERE_ID,
                            new String[] {String.valueOf(id)});
                } else {
                    return deleteFromEventRelatedTable(Tables.ATTENDEES, uri, null /* selection */,
                                           null /* selectionArgs */);
                }
            }
            case REMINDERS:
            {
                return deleteReminders(uri, false, selection, selectionArgs, callerIsSyncAdapter);
            }
            case REMINDERS_ID:
            {
                return deleteReminders(uri, true, null /*selection*/, null /*selectionArgs*/,
                        callerIsSyncAdapter);
            }
            case EXTENDED_PROPERTIES:
            {
                if (callerIsSyncAdapter) {
                    return mDb.delete(Tables.EXTENDED_PROPERTIES, selection, selectionArgs);
                } else {
                    return deleteFromEventRelatedTable(Tables.EXTENDED_PROPERTIES, uri, selection,
                            selectionArgs);
                }
            }
            case EXTENDED_PROPERTIES_ID:
            {
                if (callerIsSyncAdapter) {
                    long id = ContentUris.parseId(uri);
                    return mDb.delete(Tables.EXTENDED_PROPERTIES, SQL_WHERE_ID,
                            new String[] {String.valueOf(id)});
                } else {
                    return deleteFromEventRelatedTable(Tables.EXTENDED_PROPERTIES, uri,
                            null /* selection */, null /* selectionArgs */);
                }
            }
            case CALENDAR_ALERTS:
            {
                if (callerIsSyncAdapter) {
                    return mDb.delete(Tables.CALENDAR_ALERTS, selection, selectionArgs);
                } else {
                    return deleteFromEventRelatedTable(Tables.CALENDAR_ALERTS, uri, selection,
                            selectionArgs);
                }
            }
            case CALENDAR_ALERTS_ID:
            {
                // Note: dirty bit is not set for Alerts because it is not synced.
                // It is generated from Reminders, which is synced.
                long id = ContentUris.parseId(uri);
                return mDb.delete(Tables.CALENDAR_ALERTS, SQL_WHERE_ID,
                        new String[] {String.valueOf(id)});
            }
            case CALENDARS_ID:
                StringBuilder selectionSb = new StringBuilder(Calendars._ID + "=");
                selectionSb.append(uri.getPathSegments().get(1));
                if (!TextUtils.isEmpty(selection)) {
                    selectionSb.append(" AND (");
                    selectionSb.append(selection);
                    selectionSb.append(')');
                }
                selection = selectionSb.toString();
                // $FALL-THROUGH$ - fall through to CALENDARS for the actual delete
            case CALENDARS:
                selection = appendAccountToSelection(uri, selection, Calendars.ACCOUNT_NAME,
                        Calendars.ACCOUNT_TYPE);
                return deleteMatchingCalendars(selection, selectionArgs);
            case INSTANCES:
            case INSTANCES_BY_DAY:
            case EVENT_DAYS:
            case PROVIDER_PROPERTIES:
                throw new UnsupportedOperationException("Cannot delete that URL");
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    private int deleteEventInternal(long id, boolean callerIsSyncAdapter, boolean isBatch) {
        int result = 0;
        String selectionArgs[] = new String[] {String.valueOf(id)};

        // Query this event to get the fields needed for deleting.
        Cursor cursor = mDb.query(Tables.EVENTS, EVENTS_PROJECTION,
                SQL_WHERE_ID, selectionArgs,
                null /* groupBy */,
                null /* having */, null /* sortOrder */);
        try {
            if (cursor.moveToNext()) {
                result = 1;
                String syncId = cursor.getString(EVENTS_SYNC_ID_INDEX);
                boolean emptySyncId = TextUtils.isEmpty(syncId);

                // If this was a recurring event or a recurrence
                // exception, then force a recalculation of the
                // instances.
                String rrule = cursor.getString(EVENTS_RRULE_INDEX);
                String rdate = cursor.getString(EVENTS_RDATE_INDEX);
                String origId = cursor.getString(EVENTS_ORIGINAL_ID_INDEX);
                String origSyncId = cursor.getString(EVENTS_ORIGINAL_SYNC_ID_INDEX);
                if (isRecurrenceEvent(rrule, rdate, origId, origSyncId)) {
                    mMetaData.clearInstanceRange();
                }
                boolean isRecurrence = !TextUtils.isEmpty(rrule) || !TextUtils.isEmpty(rdate);

                // we clean the Events and Attendees table if the caller is CalendarSyncAdapter
                // or if the event is local (no syncId)
                //
                // The EVENTS_CLEANUP_TRIGGER_SQL trigger will remove all associated data
                // (Attendees, Instances, Reminders, etc).
                if (callerIsSyncAdapter || emptySyncId) {
                    mDb.delete(Tables.EVENTS, SQL_WHERE_ID, selectionArgs);

                    // If this is a recurrence, and the event was never synced with the server,
                    // we want to delete any exceptions as well.  (If it has been to the server,
                    // we'll let the sync adapter delete the events explicitly.)  We assume that,
                    // if the recurrence hasn't been synced, the exceptions haven't either.
                    if (isRecurrence && emptySyncId) {
                        mDb.delete(Tables.EVENTS, SQL_WHERE_ORIGINAL_ID, selectionArgs);
                    }
                } else {
                    // Event is on the server, so we "soft delete", i.e. mark as deleted so that
                    // the sync adapter has a chance to tell the server about the deletion.  After
                    // the server sees the change, the sync adapter will do the "hard delete"
                    // (above).
                    ContentValues values = new ContentValues();
                    values.put(Events.DELETED, 1);
                    values.put(Events.DIRTY, 1);
                    addMutator(values, Events.MUTATORS);
                    mDb.update(Tables.EVENTS, values, SQL_WHERE_ID, selectionArgs);

                    // Exceptions that have been synced shouldn't be deleted -- the sync
                    // adapter will take care of that -- but we want to "soft delete" them so
                    // that they will be removed from the instances list.
                    // TODO: this seems to confuse the sync adapter, and leaves you with an
                    //       invisible "ghost" event after the server sync.  Maybe we can fix
                    //       this by making instance generation smarter?  Not vital, since the
                    //       exception instances disappear after the server sync.
                    //mDb.update(Tables.EVENTS, values, SQL_WHERE_ORIGINAL_ID_HAS_SYNC_ID,
                    //        selectionArgs);

                    // It's possible for the original event to be on the server but have
                    // exceptions that aren't.  We want to remove all events with a matching
                    // original_id and an empty _sync_id.
                    mDb.delete(Tables.EVENTS, SQL_WHERE_ORIGINAL_ID_NO_SYNC_ID,
                            selectionArgs);

                    // Delete associated data; attendees, however, are deleted with the actual event
                    //  so that the sync adapter is able to notify attendees of the cancellation.
                    mDb.delete(Tables.INSTANCES, SQL_WHERE_EVENT_ID, selectionArgs);
                    mDb.delete(Tables.EVENTS_RAW_TIMES, SQL_WHERE_EVENT_ID, selectionArgs);
                    mDb.delete(Tables.REMINDERS, SQL_WHERE_EVENT_ID, selectionArgs);
                    mDb.delete(Tables.CALENDAR_ALERTS, SQL_WHERE_EVENT_ID, selectionArgs);
                    mDb.delete(Tables.EXTENDED_PROPERTIES, SQL_WHERE_EVENT_ID,
                            selectionArgs);
                }
            }
        } finally {
            cursor.close();
            cursor = null;
        }

        if (!isBatch) {
            mCalendarAlarm.scheduleNextAlarm(false /* do not remove alarms */);
            sendUpdateNotification(callerIsSyncAdapter);
        }
        return result;
    }

    /**
     * Delete rows from an Event-related table (e.g. Attendees) and mark corresponding events
     * as dirty.
     *
     * @param table The table to delete from
     * @param uri The URI specifying the rows
     * @param selection for the query
     * @param selectionArgs for the query
     */
    private int deleteFromEventRelatedTable(String table, Uri uri, String selection,
            String[] selectionArgs) {
        if (table.equals(Tables.EVENTS)) {
            throw new IllegalArgumentException("Don't delete Events with this method "
                    + "(use deleteEventInternal)");
        }

        ContentValues dirtyValues = new ContentValues();
        dirtyValues.put(Events.DIRTY, "1");
        addMutator(dirtyValues, Events.MUTATORS);

        /*
         * Re-issue the delete URI as a query.  Note that, if this is a by-ID request, the ID
         * will be in the URI, not selection/selectionArgs.
         *
         * Note that the query will return data according to the access restrictions,
         * so we don't need to worry about deleting data we don't have permission to read.
         */
        Cursor c = query(uri, ID_PROJECTION, selection, selectionArgs, GENERIC_EVENT_ID);
        int count = 0;
        try {
            long prevEventId = -1;
            while (c.moveToNext()) {
                long id = c.getLong(ID_INDEX);
                long eventId = c.getLong(EVENT_ID_INDEX);
                // Duplicate the event.  As a minor optimization, don't try to duplicate an
                // event that we just duplicated on the previous iteration.
                if (eventId != prevEventId) {
                    mDbHelper.duplicateEvent(eventId);
                    prevEventId = eventId;
                }
                mDb.delete(table, SQL_WHERE_ID, new String[]{String.valueOf(id)});
                if (eventId != prevEventId) {
                    mDb.update(Tables.EVENTS, dirtyValues, SQL_WHERE_ID,
                            new String[] { String.valueOf(eventId)} );
                }
                count++;
            }
        } finally {
            c.close();
        }
        return count;
    }

    /**
     * Deletes rows from the Reminders table and marks the corresponding events as dirty.
     * Ensures the hasAlarm column in the Event is updated.
     *
     * @return The number of rows deleted.
     */
    private int deleteReminders(Uri uri, boolean byId, String selection, String[] selectionArgs,
            boolean callerIsSyncAdapter) {
        /*
         * If this is a by-ID URI, make sure we have a good ID.  Also, confirm that the
         * selection is null, since we will be ignoring it.
         */
        long rowId = -1;
        if (byId) {
            if (!TextUtils.isEmpty(selection)) {
                throw new UnsupportedOperationException("Selection not allowed for " + uri);
            }
            rowId = ContentUris.parseId(uri);
            if (rowId < 0) {
                throw new IllegalArgumentException("ID expected but not found in " + uri);
            }
        }

        /*
         * Determine the set of events affected by this operation.  There can be multiple
         * reminders with the same event_id, so to avoid beating up the database with "how many
         * reminders are left" and "duplicate this event" requests, we want to generate a list
         * of affected event IDs and work off that.
         *
         * TODO: use GROUP BY to reduce the number of rows returned in the cursor.  (The content
         * provider query() doesn't take it as an argument.)
         */
        HashSet<Long> eventIdSet = new HashSet<Long>();
        Cursor c = query(uri, new String[] { Attendees.EVENT_ID }, selection, selectionArgs, null);
        try {
            while (c.moveToNext()) {
                eventIdSet.add(c.getLong(0));
            }
        } finally {
            c.close();
        }

        /*
         * If this isn't a sync adapter, duplicate each event (along with its associated tables),
         * and mark each as "dirty".  This is for the benefit of partial-update sync.
         */
        if (!callerIsSyncAdapter) {
            ContentValues dirtyValues = new ContentValues();
            dirtyValues.put(Events.DIRTY, "1");
            addMutator(dirtyValues, Events.MUTATORS);

            Iterator<Long> iter = eventIdSet.iterator();
            while (iter.hasNext()) {
                long eventId = iter.next();
                mDbHelper.duplicateEvent(eventId);
                mDb.update(Tables.EVENTS, dirtyValues, SQL_WHERE_ID,
                        new String[] { String.valueOf(eventId) });
            }
        }

        /*
         * Issue the original deletion request.  If we were called with a by-ID URI, generate
         * a selection.
         */
        if (byId) {
            selection = SQL_WHERE_ID;
            selectionArgs = new String[] { String.valueOf(rowId) };
        }
        int delCount = mDb.delete(Tables.REMINDERS, selection, selectionArgs);

        /*
         * For each event, set "hasAlarm" to zero if we've deleted the last of the reminders.
         * (If the event still has reminders, hasAlarm should already be 1.)  Because we're
         * executing in an exclusive transaction there's no risk of racing against other
         * database updates.
         */
        ContentValues noAlarmValues = new ContentValues();
        noAlarmValues.put(Events.HAS_ALARM, 0);
        Iterator<Long> iter = eventIdSet.iterator();
        while (iter.hasNext()) {
            long eventId = iter.next();

            // Count up the number of reminders still associated with this event.
            Cursor reminders = mDb.query(Tables.REMINDERS, new String[] { GENERIC_ID },
                    SQL_WHERE_EVENT_ID, new String[] { String.valueOf(eventId) },
                    null, null, null);
            int reminderCount = reminders.getCount();
            reminders.close();

            if (reminderCount == 0) {
                mDb.update(Tables.EVENTS, noAlarmValues, SQL_WHERE_ID,
                        new String[] { String.valueOf(eventId) });
            }
        }

        return delCount;
    }

    /**
     * Update rows in a table and, if this is a non-sync-adapter update, mark the corresponding
     * events as dirty.
     * <p>
     * This only works for tables that are associated with an event.  It is assumed that the
     * link to the Event row is a numeric identifier in a column called "event_id".
     *
     * @param uri The original request URI.
     * @param byId Set to true if the URI is expected to include an ID.
     * @param updateValues The new values to apply.  Not all columns need be represented.
     * @param selection For non-by-ID operations, the "where" clause to use.
     * @param selectionArgs For non-by-ID operations, arguments to apply to the "where" clause.
     * @param callerIsSyncAdapter Set to true if the caller is a sync adapter.
     * @return The number of rows updated.
     */
    private int updateEventRelatedTable(Uri uri, String table, boolean byId,
            ContentValues updateValues, String selection, String[] selectionArgs,
            boolean callerIsSyncAdapter)
    {
        /*
         * Confirm that the request has either an ID or a selection, but not both.  It's not
         * actually "wrong" to have both, but it's not useful, and having neither is likely
         * a mistake.
         *
         * If they provided an ID in the URI, convert it to an ID selection.
         */
        if (byId) {
            if (!TextUtils.isEmpty(selection)) {
                throw new UnsupportedOperationException("Selection not allowed for " + uri);
            }
            long rowId = ContentUris.parseId(uri);
            if (rowId < 0) {
                throw new IllegalArgumentException("ID expected but not found in " + uri);
            }
            selection = SQL_WHERE_ID;
            selectionArgs = new String[] { String.valueOf(rowId) };
        } else {
            if (TextUtils.isEmpty(selection)) {
                throw new UnsupportedOperationException("Selection is required for " + uri);
            }
        }

        /*
         * Query the events to update.  We want all the columns from the table, so we us a
         * null projection.
         */
        Cursor c = mDb.query(table, null /*projection*/, selection, selectionArgs,
                null, null, null);
        int count = 0;
        try {
            if (c.getCount() == 0) {
                Log.d(TAG, "No query results for " + uri + ", selection=" + selection +
                        " selectionArgs=" + Arrays.toString(selectionArgs));
                return 0;
            }

            ContentValues dirtyValues = null;
            if (!callerIsSyncAdapter) {
                dirtyValues = new ContentValues();
                dirtyValues.put(Events.DIRTY, "1");
                addMutator(dirtyValues, Events.MUTATORS);
            }

            final int idIndex = c.getColumnIndex(GENERIC_ID);
            final int eventIdIndex = c.getColumnIndex(GENERIC_EVENT_ID);
            if (idIndex < 0 || eventIdIndex < 0) {
                throw new RuntimeException("Lookup on _id/event_id failed for " + uri);
            }

            /*
             * For each row found:
             * - merge original values with update values
             * - update database
             * - if not sync adapter, set "dirty" flag in corresponding event to 1
             * - update Event attendee status
             */
            while (c.moveToNext()) {
                /* copy the original values into a ContentValues, then merge the changes in */
                ContentValues values = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(c, values);
                values.putAll(updateValues);

                long id = c.getLong(idIndex);
                long eventId = c.getLong(eventIdIndex);
                if (!callerIsSyncAdapter) {
                    // Make a copy of the original, so partial-update code can see diff.
                    mDbHelper.duplicateEvent(eventId);
                }
                mDb.update(table, values, SQL_WHERE_ID, new String[] { String.valueOf(id) });
                if (!callerIsSyncAdapter) {
                    mDb.update(Tables.EVENTS, dirtyValues, SQL_WHERE_ID,
                            new String[] { String.valueOf(eventId) });
                }
                count++;

                /*
                 * The Events table has a "selfAttendeeStatus" field that usually mirrors the
                 * "attendeeStatus" column of one row in the Attendees table.  It's the provider's
                 * job to keep these in sync, so we have to check for changes here.  (We have
                 * to do it way down here because this is the only point where we have the
                 * merged Attendees values.)
                 *
                 * It's possible, but not expected, to have multiple Attendees entries with
                 * matching attendeeEmail.  The behavior in this case is not defined.
                 *
                 * We could do this more efficiently for "bulk" updates by caching the Calendar
                 * owner email and checking it here.
                 */
                if (table.equals(Tables.ATTENDEES)) {
                    updateEventAttendeeStatus(mDb, values);
                    sendUpdateNotification(eventId, callerIsSyncAdapter);
                }
            }
        } finally {
            c.close();
        }
        return count;
    }

    private int deleteMatchingColors(String selection, String[] selectionArgs) {
        // query to find all the colors that match, for each
        // - verify no one references it
        // - delete color
        Cursor c = mDb.query(Tables.COLORS, COLORS_PROJECTION, selection, selectionArgs, null,
                null, null);
        if (c == null) {
            return 0;
        }
        try {
            Cursor c2 = null;
            while (c.moveToNext()) {
                String index = c.getString(COLORS_COLOR_INDEX_INDEX);
                String accountName = c.getString(COLORS_ACCOUNT_NAME_INDEX);
                String accountType = c.getString(COLORS_ACCOUNT_TYPE_INDEX);
                boolean isCalendarColor = c.getInt(COLORS_COLOR_TYPE_INDEX) == Colors.TYPE_CALENDAR;
                try {
                    if (isCalendarColor) {
                        c2 = mDb.query(Tables.CALENDARS, ID_ONLY_PROJECTION,
                                SQL_WHERE_CALENDAR_COLOR, new String[] {
                                        accountName, accountType, index
                                }, null, null, null);
                        if (c2.getCount() != 0) {
                            throw new UnsupportedOperationException("Cannot delete color " + index
                                    + ". Referenced by " + c2.getCount() + " calendars.");

                        }
                    } else {
                        c2 = query(Events.CONTENT_URI, ID_ONLY_PROJECTION, SQL_WHERE_EVENT_COLOR,
                                new String[] {accountName, accountType, index}, null);
                        if (c2.getCount() != 0) {
                            throw new UnsupportedOperationException("Cannot delete color " + index
                                    + ". Referenced by " + c2.getCount() + " events.");

                        }
                    }
                } finally {
                    if (c2 != null) {
                        c2.close();
                    }
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return mDb.delete(Tables.COLORS, selection, selectionArgs);
    }

    private int deleteMatchingCalendars(String selection, String[] selectionArgs) {
        // query to find all the calendars that match, for each
        // - delete calendar subscription
        // - delete calendar
        Cursor c = mDb.query(Tables.CALENDARS, sCalendarsIdProjection, selection,
                selectionArgs,
                null /* groupBy */,
                null /* having */,
                null /* sortOrder */);
        if (c == null) {
            return 0;
        }
        try {
            while (c.moveToNext()) {
                long id = c.getLong(CALENDARS_INDEX_ID);
                modifyCalendarSubscription(id, false /* not selected */);
            }
        } finally {
            c.close();
        }
        return mDb.delete(Tables.CALENDARS, selection, selectionArgs);
    }

    private boolean doesEventExistForSyncId(String syncId) {
        if (syncId == null) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "SyncID cannot be null: " + syncId);
            }
            return false;
        }
        long count = DatabaseUtils.longForQuery(mDb, SQL_SELECT_COUNT_FOR_SYNC_ID,
                new String[] { syncId });
        return (count > 0);
    }

    // Check if an UPDATE with STATUS_CANCEL means that we will need to do an Update (instead of
    // a Deletion)
    //
    // Deletion will be done only and only if:
    // - event status = canceled
    // - event is a recurrence exception that does not have its original (parent) event anymore
    //
    // This is due to the Server semantics that generate STATUS_CANCELED for both creation
    // and deletion of a recurrence exception
    // See bug #3218104
    private boolean doesStatusCancelUpdateMeanUpdate(ContentValues values,
            ContentValues modValues) {
        boolean isStatusCanceled = modValues.containsKey(Events.STATUS) &&
                (modValues.getAsInteger(Events.STATUS) == Events.STATUS_CANCELED);
        if (isStatusCanceled) {
            String originalSyncId = values.getAsString(Events.ORIGINAL_SYNC_ID);

            if (!TextUtils.isEmpty(originalSyncId)) {
                // This event is an exception.  See if the recurring event still exists.
                return doesEventExistForSyncId(originalSyncId);
            }
        }
        // This is the normal case, we just want an UPDATE
        return true;
    }

    private int handleUpdateColors(ContentValues values, String selection, String[] selectionArgs) {
        Cursor c = null;
        int result = mDb.update(Tables.COLORS, values, selection, selectionArgs);
        if (values.containsKey(Colors.COLOR)) {
            try {
                c = mDb.query(Tables.COLORS, COLORS_PROJECTION, selection, selectionArgs,
                        null /* groupBy */, null /* having */, null /* orderBy */);
                while (c.moveToNext()) {
                    boolean calendarColor =
                            c.getInt(COLORS_COLOR_TYPE_INDEX) == Colors.TYPE_CALENDAR;
                    int color = c.getInt(COLORS_COLOR_INDEX);
                    String[] args = {
                            c.getString(COLORS_ACCOUNT_NAME_INDEX),
                            c.getString(COLORS_ACCOUNT_TYPE_INDEX),
                            c.getString(COLORS_COLOR_INDEX_INDEX)
                    };
                    ContentValues colorValue = new ContentValues();
                    if (calendarColor) {
                        colorValue.put(Calendars.CALENDAR_COLOR, color);
                        mDb.update(Tables.CALENDARS, colorValue, SQL_WHERE_CALENDAR_COLOR, args);
                    } else {
                        colorValue.put(Events.EVENT_COLOR, color);
                        mDb.update(Tables.EVENTS, colorValue, SQL_WHERE_EVENT_COLOR, args);
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        return result;
    }


    /**
     * Handles a request to update one or more events.
     * <p>
     * The original event(s) will be loaded from the database, merged with the new values,
     * and the result checked for validity.  In some cases this will alter the supplied
     * arguments (e.g. zeroing out the times on all-day events), change additional fields (e.g.
     * update LAST_DATE when DTSTART changes), or cause modifications to other tables (e.g. reset
     * Instances when a recurrence rule changes).
     *
     * @param cursor The set of events to update.
     * @param updateValues The changes to apply to each event.
     * @param callerIsSyncAdapter Indicates if the request comes from the sync adapter.
     * @return the number of rows updated
     */
    private int handleUpdateEvents(Cursor cursor, ContentValues updateValues,
            boolean callerIsSyncAdapter) {
        /*
         * This field is considered read-only.  It should not be modified by applications or
         * by the sync adapter.
         */
        updateValues.remove(Events.HAS_ALARM);

        /*
         * For a single event, we can just load the event, merge modValues in, perform any
         * fix-ups (putting changes into modValues), check validity, and then update().  We have
         * to be careful that our fix-ups don't confuse the sync adapter.
         *
         * For multiple events, we need to load, merge, and validate each event individually.
         * If no single-event-specific changes need to be made, we could just issue the original
         * bulk update, which would be more efficient than a series of individual updates.
         * However, doing so would prevent us from taking advantage of the partial-update
         * mechanism.
         */
        if (cursor.getCount() > 1) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Performing update on " + cursor.getCount() + " events");
            }
        }
        while (cursor.moveToNext()) {
            // Make a copy of updateValues so we can make some local changes.
            ContentValues modValues = new ContentValues(updateValues);

            // Load the event into a ContentValues object.
            ContentValues values = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(cursor, values);
            boolean doValidate = false;
            if (!callerIsSyncAdapter) {
                try {
                    // Check to see if the data in the database is valid.  If not, we will skip
                    // validation of the update, so that we don't blow up on attempts to
                    // modify existing badly-formed events.
                    validateEventData(values);
                    doValidate = true;
                } catch (IllegalArgumentException iae) {
                    Log.d(TAG, "Event " + values.getAsString(Events._ID) +
                            " malformed, not validating update (" +
                            iae.getMessage() + ")");
                }
            }

            // Merge the modifications in.
            values.putAll(modValues);

            // If a color_index is being set make sure it's valid
            String color_id = modValues.getAsString(Events.EVENT_COLOR_KEY);
            if (!TextUtils.isEmpty(color_id)) {
                String accountName = null;
                String accountType = null;
                Cursor c = mDb.query(Tables.CALENDARS, ACCOUNT_PROJECTION, SQL_WHERE_ID,
                        new String[] { values.getAsString(Events.CALENDAR_ID) }, null, null, null);
                try {
                    if (c.moveToFirst()) {
                        accountName = c.getString(ACCOUNT_NAME_INDEX);
                        accountType = c.getString(ACCOUNT_TYPE_INDEX);
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
                verifyColorExists(accountName, accountType, color_id, Colors.TYPE_EVENT);
            }

            // Scrub and/or validate the combined event.
            if (callerIsSyncAdapter) {
                scrubEventData(values, modValues);
            }
            if (doValidate) {
                validateEventData(values);
            }

            // Look for any updates that could affect LAST_DATE.  It's defined as the end of
            // the last meeting, so we need to pay attention to DURATION.
            if (modValues.containsKey(Events.DTSTART) ||
                    modValues.containsKey(Events.DTEND) ||
                    modValues.containsKey(Events.DURATION) ||
                    modValues.containsKey(Events.EVENT_TIMEZONE) ||
                    modValues.containsKey(Events.RRULE) ||
                    modValues.containsKey(Events.RDATE) ||
                    modValues.containsKey(Events.EXRULE) ||
                    modValues.containsKey(Events.EXDATE)) {
                long newLastDate;
                try {
                    newLastDate = calculateLastDate(values);
                } catch (DateException de) {
                    throw new IllegalArgumentException("Unable to compute LAST_DATE", de);
                }
                Long oldLastDateObj = values.getAsLong(Events.LAST_DATE);
                long oldLastDate = (oldLastDateObj == null) ? -1 : oldLastDateObj;
                if (oldLastDate != newLastDate) {
                    // This overwrites any caller-supplied LAST_DATE.  This is okay, because the
                    // caller isn't supposed to be messing with the LAST_DATE field.
                    if (newLastDate < 0) {
                        modValues.putNull(Events.LAST_DATE);
                    } else {
                        modValues.put(Events.LAST_DATE, newLastDate);
                    }
                }
            }

            if (!callerIsSyncAdapter) {
                modValues.put(Events.DIRTY, 1);
                addMutator(modValues, Events.MUTATORS);
            }

            // Disallow updating the attendee status in the Events
            // table.  In the future, we could support this but we
            // would have to query and update the attendees table
            // to keep the values consistent.
            if (modValues.containsKey(Events.SELF_ATTENDEE_STATUS)) {
                throw new IllegalArgumentException("Updating "
                        + Events.SELF_ATTENDEE_STATUS
                        + " in Events table is not allowed.");
            }

            if (fixAllDayTime(values, modValues)) {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "handleUpdateEvents: " +
                            "allDay is true but sec, min, hour were not 0.");
                }
            }

            // For taking care about recurrences exceptions cancelations, check if this needs
            //  to be an UPDATE or a DELETE
            boolean isUpdate = doesStatusCancelUpdateMeanUpdate(values, modValues);

            long id = values.getAsLong(Events._ID);

            if (isUpdate) {
                // If a user made a change, possibly duplicate the event so we can do a partial
                // update. If a sync adapter made a change and that change marks an event as
                // un-dirty, remove any duplicates that may have been created earlier.
                if (!callerIsSyncAdapter) {
                    mDbHelper.duplicateEvent(id);
                } else {
                    if (modValues.containsKey(Events.DIRTY)
                            && modValues.getAsInteger(Events.DIRTY) == 0) {
                        modValues.put(Events.MUTATORS, (String) null);
                        mDbHelper.removeDuplicateEvent(id);
                    }
                }
                int result = mDb.update(Tables.EVENTS, modValues, SQL_WHERE_ID,
                        new String[] { String.valueOf(id) });
                if (result > 0) {
                    updateEventRawTimesLocked(id, modValues);
                    mInstancesHelper.updateInstancesLocked(modValues, id,
                            false /* not a new event */, mDb);

                    // XXX: should we also be doing this when RRULE changes (e.g. instances
                    //      are introduced or removed?)
                    if (modValues.containsKey(Events.DTSTART) ||
                            modValues.containsKey(Events.STATUS)) {
                        // If this is a cancellation knock it out
                        // of the instances table
                        if (modValues.containsKey(Events.STATUS) &&
                                modValues.getAsInteger(Events.STATUS) == Events.STATUS_CANCELED) {
                            String[] args = new String[] {String.valueOf(id)};
                            mDb.delete(Tables.INSTANCES, SQL_WHERE_EVENT_ID, args);
                        }

                        // The start time or status of the event changed, so run the
                        // event alarm scheduler.
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "updateInternal() changing event");
                        }
                        mCalendarAlarm.scheduleNextAlarm(false /* do not remove alarms */);
                    }

                    sendUpdateNotification(id, callerIsSyncAdapter);
                }
            } else {
                deleteEventInternal(id, callerIsSyncAdapter, true /* isBatch */);
                mCalendarAlarm.scheduleNextAlarm(false /* do not remove alarms */);
                sendUpdateNotification(callerIsSyncAdapter);
            }
        }

        return cursor.getCount();
    }

    @Override
    protected int updateInTransaction(Uri uri, ContentValues values, String selection,
            String[] selectionArgs, boolean callerIsSyncAdapter) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "updateInTransaction: " + uri);
        }
        validateUriParameters(uri.getQueryParameterNames());
        final int match = sUriMatcher.match(uri);
        verifyTransactionAllowed(TRANSACTION_UPDATE, uri, values, callerIsSyncAdapter, match,
                selection, selectionArgs);

        switch (match) {
            case SYNCSTATE:
                return mDbHelper.getSyncState().update(mDb, values,
                        appendAccountToSelection(uri, selection, Calendars.ACCOUNT_NAME,
                                Calendars.ACCOUNT_TYPE), selectionArgs);

            case SYNCSTATE_ID: {
                selection = appendAccountToSelection(uri, selection, Calendars.ACCOUNT_NAME,
                        Calendars.ACCOUNT_TYPE);
                String selectionWithId = (SyncState._ID + "=?")
                        + (selection == null ? "" : " AND (" + selection + ")");
                // Prepend id to selectionArgs
                selectionArgs = insertSelectionArg(selectionArgs,
                        String.valueOf(ContentUris.parseId(uri)));
                return mDbHelper.getSyncState().update(mDb, values, selectionWithId, selectionArgs);
            }

            case COLORS:
                int validValues = 0;
                if (values.getAsInteger(Colors.COLOR) != null) {
                    validValues++;
                }
                if (values.getAsString(Colors.DATA) != null) {
                    validValues++;
                }

                if (values.size() != validValues) {
                    throw new UnsupportedOperationException("You may only change the COLOR and"
                            + " DATA columns for an existing Colors entry.");
                }
                return handleUpdateColors(values, appendAccountToSelection(uri, selection,
                        Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE),
                        selectionArgs);

            case CALENDARS:
            case CALENDARS_ID:
            {
                long id;
                if (match == CALENDARS_ID) {
                    id = ContentUris.parseId(uri);
                } else {
                    // TODO: for supporting other sync adapters, we will need to
                    // be able to deal with the following cases:
                    // 1) selection to "_id=?" and pass in a selectionArgs
                    // 2) selection to "_id IN (1, 2, 3)"
                    // 3) selection to "delete=0 AND _id=1"
                    if (selection != null && TextUtils.equals(selection,"_id=?")) {
                        id = Long.parseLong(selectionArgs[0]);
                    } else if (selection != null && selection.startsWith("_id=")) {
                        // The ContentProviderOperation generates an _id=n string instead of
                        // adding the id to the URL, so parse that out here.
                        id = Long.parseLong(selection.substring(4));
                    } else {
                        return mDb.update(Tables.CALENDARS, values, selection, selectionArgs);
                    }
                }
                if (!callerIsSyncAdapter) {
                    values.put(Calendars.DIRTY, 1);
                    addMutator(values, Calendars.MUTATORS);
                } else {
                    if (values.containsKey(Calendars.DIRTY)
                            && values.getAsInteger(Calendars.DIRTY) == 0) {
                        values.put(Calendars.MUTATORS, (String) null);
                    }
                }
                Integer syncEvents = values.getAsInteger(Calendars.SYNC_EVENTS);
                if (syncEvents != null) {
                    modifyCalendarSubscription(id, syncEvents == 1);
                }
                String color_id = values.getAsString(Calendars.CALENDAR_COLOR_KEY);
                if (!TextUtils.isEmpty(color_id)) {
                    String accountName = values.getAsString(Calendars.ACCOUNT_NAME);
                    String accountType = values.getAsString(Calendars.ACCOUNT_TYPE);
                    if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
                        Account account = getAccount(id);
                        if (account != null) {
                            accountName = account.name;
                            accountType = account.type;
                        }
                    }
                    verifyColorExists(accountName, accountType, color_id, Colors.TYPE_CALENDAR);
                }

                int result = mDb.update(Tables.CALENDARS, values, SQL_WHERE_ID,
                        new String[] {String.valueOf(id)});

                if (result > 0) {
                    // if visibility was toggled, we need to update alarms
                    if (values.containsKey(Calendars.VISIBLE)) {
                        // pass false for removeAlarms since the call to
                        // scheduleNextAlarmLocked will remove any alarms for
                        // non-visible events anyways. removeScheduledAlarmsLocked
                        // does not actually have the effect we want
                        mCalendarAlarm.scheduleNextAlarm(false);
                    }
                    // update the widget
                    sendUpdateNotification(callerIsSyncAdapter);
                }

                return result;
            }
            case EVENTS:
            case EVENTS_ID:
            {
                Cursor events = null;

                // Grab the full set of columns for each selected event.
                // TODO: define a projection with just the data we need (e.g. we don't need to
                //       validate the SYNC_* columns)

                try {
                    if (match == EVENTS_ID) {
                        // Single event, identified by ID.
                        long id = ContentUris.parseId(uri);
                        events = mDb.query(Tables.EVENTS, null /* columns */,
                                SQL_WHERE_ID, new String[] { String.valueOf(id) },
                                null /* groupBy */, null /* having */, null /* sortOrder */);
                    } else {
                        // One or more events, identified by the selection / selectionArgs.
                        events = mDb.query(Tables.EVENTS, null /* columns */,
                                selection, selectionArgs,
                                null /* groupBy */, null /* having */, null /* sortOrder */);
                    }

                    if (events.getCount() == 0) {
                        Log.i(TAG, "No events to update: uri=" + uri + " selection=" + selection +
                                " selectionArgs=" + Arrays.toString(selectionArgs));
                        return 0;
                    }

                    return handleUpdateEvents(events, values, callerIsSyncAdapter);
                } finally {
                    if (events != null) {
                        events.close();
                    }
                }
            }
            case ATTENDEES:
                return updateEventRelatedTable(uri, Tables.ATTENDEES, false, values, selection,
                        selectionArgs, callerIsSyncAdapter);
            case ATTENDEES_ID:
                return updateEventRelatedTable(uri, Tables.ATTENDEES, true, values, null, null,
                        callerIsSyncAdapter);

            case CALENDAR_ALERTS_ID: {
                // Note: dirty bit is not set for Alerts because it is not synced.
                // It is generated from Reminders, which is synced.
                long id = ContentUris.parseId(uri);
                return mDb.update(Tables.CALENDAR_ALERTS, values, SQL_WHERE_ID,
                        new String[] {String.valueOf(id)});
            }
            case CALENDAR_ALERTS: {
                // Note: dirty bit is not set for Alerts because it is not synced.
                // It is generated from Reminders, which is synced.
                return mDb.update(Tables.CALENDAR_ALERTS, values, selection, selectionArgs);
            }

            case REMINDERS:
                return updateEventRelatedTable(uri, Tables.REMINDERS, false, values, selection,
                        selectionArgs, callerIsSyncAdapter);
            case REMINDERS_ID: {
                int count = updateEventRelatedTable(uri, Tables.REMINDERS, true, values, null, null,
                        callerIsSyncAdapter);

                // Reschedule the event alarms because the
                // "minutes" field may have changed.
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "updateInternal() changing reminder");
                }
                mCalendarAlarm.scheduleNextAlarm(false /* do not remove alarms */);
                return count;
            }

            case EXTENDED_PROPERTIES_ID:
                return updateEventRelatedTable(uri, Tables.EXTENDED_PROPERTIES, true, values,
                        null, null, callerIsSyncAdapter);

            // TODO: replace the SCHEDULE_ALARM private URIs with a
            // service
            case SCHEDULE_ALARM: {
                mCalendarAlarm.scheduleNextAlarm(false);
                return 0;
            }
            case SCHEDULE_ALARM_REMOVE: {
                mCalendarAlarm.scheduleNextAlarm(true);
                return 0;
            }

            case PROVIDER_PROPERTIES: {
                if (!selection.equals("key=?")) {
                    throw new UnsupportedOperationException("Selection should be key=? for " + uri);
                }

                List<String> list = Arrays.asList(selectionArgs);

                if (list.contains(CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS)) {
                    throw new UnsupportedOperationException("Invalid selection key: " +
                            CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS + " for " + uri);
                }

                // Before it may be changed, save current Instances timezone for later use
                String timezoneInstancesBeforeUpdate = mCalendarCache.readTimezoneInstances();

                // Update the database with the provided values (this call may change the value
                // of timezone Instances)
                int result = mDb.update(Tables.CALENDAR_CACHE, values, selection, selectionArgs);

                // if successful, do some house cleaning:
                // if the timezone type is set to "home", set the Instances
                // timezone to the previous
                // if the timezone type is set to "auto", set the Instances
                // timezone to the current
                // device one
                // if the timezone Instances is set AND if we are in "home"
                // timezone type, then save the timezone Instance into
                // "previous" too
                if (result > 0) {
                    // If we are changing timezone type...
                    if (list.contains(CalendarCache.KEY_TIMEZONE_TYPE)) {
                        String value = values.getAsString(CalendarCache.COLUMN_NAME_VALUE);
                        if (value != null) {
                            // if we are setting timezone type to "home"
                            if (value.equals(CalendarCache.TIMEZONE_TYPE_HOME)) {
                                String previousTimezone =
                                        mCalendarCache.readTimezoneInstancesPrevious();
                                if (previousTimezone != null) {
                                    mCalendarCache.writeTimezoneInstances(previousTimezone);
                                }
                                // Regenerate Instances if the "home" timezone has changed
                                // and notify widgets
                                if (!timezoneInstancesBeforeUpdate.equals(previousTimezone) ) {
                                    regenerateInstancesTable();
                                    sendUpdateNotification(callerIsSyncAdapter);
                                }
                            }
                            // if we are setting timezone type to "auto"
                            else if (value.equals(CalendarCache.TIMEZONE_TYPE_AUTO)) {
                                String localTimezone = TimeZone.getDefault().getID();
                                mCalendarCache.writeTimezoneInstances(localTimezone);
                                if (!timezoneInstancesBeforeUpdate.equals(localTimezone)) {
                                    regenerateInstancesTable();
                                    sendUpdateNotification(callerIsSyncAdapter);
                                }
                            }
                        }
                    }
                    // If we are changing timezone Instances...
                    else if (list.contains(CalendarCache.KEY_TIMEZONE_INSTANCES)) {
                        // if we are in "home" timezone type...
                        if (isHomeTimezone()) {
                            String timezoneInstances = mCalendarCache.readTimezoneInstances();
                            // Update the previous value
                            mCalendarCache.writeTimezoneInstancesPrevious(timezoneInstances);
                            // Recompute Instances if the "home" timezone has changed
                            // and send notifications to any widgets
                            if (timezoneInstancesBeforeUpdate != null &&
                                    !timezoneInstancesBeforeUpdate.equals(timezoneInstances)) {
                                regenerateInstancesTable();
                                sendUpdateNotification(callerIsSyncAdapter);
                            }
                        }
                    }
                }
                return result;
            }

            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    /**
     * Verifies that a color with the given index exists for the given Calendar
     * entry.
     *
     * @param accountName The email of the account the color is for
     * @param accountType The type of account the color is for
     * @param colorIndex The color_index being set for the calendar
     * @param colorType The type of color expected (Calendar/Event)
     * @return The color specified by the index
     */
    private int verifyColorExists(String accountName, String accountType, String colorIndex,
            int colorType) {
        if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
            throw new IllegalArgumentException("Cannot set color. A valid account does"
                    + " not exist for this calendar.");
        }
        int color;
        Cursor c = null;
        try {
            c = getColorByTypeIndex(accountName, accountType, colorType, colorIndex);
            if (!c.moveToFirst()) {
                throw new IllegalArgumentException("Color type: " + colorType + " and index "
                        + colorIndex + " does not exist for account.");
            }
            color = c.getInt(COLORS_COLOR_INDEX);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return color;
    }

    private String appendLastSyncedColumnToSelection(String selection, Uri uri) {
        if (getIsCallerSyncAdapter(uri)) {
            return selection;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(CalendarContract.Events.LAST_SYNCED).append(" = 0");
        return appendSelection(sb, selection);
    }

    private String appendAccountToSelection(
            Uri uri,
            String selection,
            String accountNameColumn,
            String accountTypeColumn) {
        final String accountName = QueryParameterUtils.getQueryParameter(uri,
                CalendarContract.EventsEntity.ACCOUNT_NAME);
        final String accountType = QueryParameterUtils.getQueryParameter(uri,
                CalendarContract.EventsEntity.ACCOUNT_TYPE);
        if (!TextUtils.isEmpty(accountName)) {
            final StringBuilder sb = new StringBuilder()
                    .append(accountNameColumn)
                    .append("=")
                    .append(DatabaseUtils.sqlEscapeString(accountName))
                    .append(" AND ")
                    .append(accountTypeColumn)
                    .append("=")
                    .append(DatabaseUtils.sqlEscapeString(accountType));
            return appendSelection(sb, selection);
        } else {
            return selection;
        }
    }

    private String appendSelection(StringBuilder sb, String selection) {
        if (!TextUtils.isEmpty(selection)) {
            sb.append(" AND (");
            sb.append(selection);
            sb.append(')');
        }
        return sb.toString();
    }

    /**
     * Verifies that the operation is allowed and throws an exception if it
     * isn't. This defines the limits of a sync adapter call vs an app call.
     * <p>
     * Also rejects calls that have a selection but shouldn't, or that don't have a selection
     * but should.
     *
     * @param type The type of call, {@link #TRANSACTION_QUERY},
     *            {@link #TRANSACTION_INSERT}, {@link #TRANSACTION_UPDATE}, or
     *            {@link #TRANSACTION_DELETE}
     * @param uri
     * @param values
     * @param isSyncAdapter
     */
    private void verifyTransactionAllowed(int type, Uri uri, ContentValues values,
            boolean isSyncAdapter, int uriMatch, String selection, String[] selectionArgs) {
        // Queries are never restricted to app- or sync-adapter-only, and we don't
        // restrict the set of columns that may be accessed.
        if (type == TRANSACTION_QUERY) {
            return;
        }

        if (type == TRANSACTION_UPDATE || type == TRANSACTION_DELETE) {
            // TODO review this list, document in contract.
            if (!TextUtils.isEmpty(selection)) {
                // Only allow selections for the URIs that can reasonably use them.
                // Whitelist of URIs allowed selections
                switch (uriMatch) {
                    case SYNCSTATE:
                    case CALENDARS:
                    case EVENTS:
                    case ATTENDEES:
                    case CALENDAR_ALERTS:
                    case REMINDERS:
                    case EXTENDED_PROPERTIES:
                    case PROVIDER_PROPERTIES:
                    case COLORS:
                        break;
                    default:
                        throw new IllegalArgumentException("Selection not permitted for " + uri);
                }
            } else {
                // Disallow empty selections for some URIs.
                // Blacklist of URIs _not_ allowed empty selections
                switch (uriMatch) {
                    case EVENTS:
                    case ATTENDEES:
                    case REMINDERS:
                    case PROVIDER_PROPERTIES:
                        throw new IllegalArgumentException("Selection must be specified for "
                                + uri);
                    default:
                        break;
                }
            }
        }

        // Only the sync adapter can use these to make changes.
        if (!isSyncAdapter) {
            switch (uriMatch) {
                case SYNCSTATE:
                case SYNCSTATE_ID:
                case EXTENDED_PROPERTIES:
                case EXTENDED_PROPERTIES_ID:
                case COLORS:
                    throw new IllegalArgumentException("Only sync adapters may write using " + uri);
                default:
                    break;
            }
        }

        switch (type) {
            case TRANSACTION_INSERT:
                if (uriMatch == INSTANCES) {
                    throw new UnsupportedOperationException(
                            "Inserting into instances not supported");
                }
                // Check there are no columns restricted to the provider
                verifyColumns(values, uriMatch);
                if (isSyncAdapter) {
                    // check that account and account type are specified
                    verifyHasAccount(uri, selection, selectionArgs);
                } else {
                    // check that sync only columns aren't included
                    verifyNoSyncColumns(values, uriMatch);
                }
                return;
            case TRANSACTION_UPDATE:
                if (uriMatch == INSTANCES) {
                    throw new UnsupportedOperationException("Updating instances not supported");
                }
                // Check there are no columns restricted to the provider
                verifyColumns(values, uriMatch);
                if (isSyncAdapter) {
                    // check that account and account type are specified
                    verifyHasAccount(uri, selection, selectionArgs);
                } else {
                    // check that sync only columns aren't included
                    verifyNoSyncColumns(values, uriMatch);
                }
                return;
            case TRANSACTION_DELETE:
                if (uriMatch == INSTANCES) {
                    throw new UnsupportedOperationException("Deleting instances not supported");
                }
                if (isSyncAdapter) {
                    // check that account and account type are specified
                    verifyHasAccount(uri, selection, selectionArgs);
                }
                return;
        }
    }

    private void verifyHasAccount(Uri uri, String selection, String[] selectionArgs) {
        String accountName = QueryParameterUtils.getQueryParameter(uri, Calendars.ACCOUNT_NAME);
        String accountType = QueryParameterUtils.getQueryParameter(uri,
                Calendars.ACCOUNT_TYPE);
        if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
            if (selection != null && selection.startsWith(ACCOUNT_SELECTION_PREFIX)) {
                accountName = selectionArgs[0];
                accountType = selectionArgs[1];
            }
        }
        if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
            throw new IllegalArgumentException(
                    "Sync adapters must specify an account and account type: " + uri);
        }
    }

    private void verifyColumns(ContentValues values, int uriMatch) {
        if (values == null || values.size() == 0) {
            return;
        }
        String[] columns;
        switch (uriMatch) {
            case EVENTS:
            case EVENTS_ID:
            case EVENT_ENTITIES:
            case EVENT_ENTITIES_ID:
                columns = Events.PROVIDER_WRITABLE_COLUMNS;
                break;
            default:
                columns = PROVIDER_WRITABLE_DEFAULT_COLUMNS;
                break;
        }

        for (int i = 0; i < columns.length; i++) {
            if (values.containsKey(columns[i])) {
                throw new IllegalArgumentException("Only the provider may write to " + columns[i]);
            }
        }
    }

    private void verifyNoSyncColumns(ContentValues values, int uriMatch) {
        if (values == null || values.size() == 0) {
            return;
        }
        String[] syncColumns;
        switch (uriMatch) {
            case CALENDARS:
            case CALENDARS_ID:
            case CALENDAR_ENTITIES:
            case CALENDAR_ENTITIES_ID:
                syncColumns = Calendars.SYNC_WRITABLE_COLUMNS;
                break;
            case EVENTS:
            case EVENTS_ID:
            case EVENT_ENTITIES:
            case EVENT_ENTITIES_ID:
                syncColumns = Events.SYNC_WRITABLE_COLUMNS;
                break;
            default:
                syncColumns = SYNC_WRITABLE_DEFAULT_COLUMNS;
                break;

        }
        for (int i = 0; i < syncColumns.length; i++) {
            if (values.containsKey(syncColumns[i])) {
                throw new IllegalArgumentException("Only sync adapters may write to "
                        + syncColumns[i]);
            }
        }
    }

    private void modifyCalendarSubscription(long id, boolean syncEvents) {
        // get the account, url, and current selected state
        // for this calendar.
        Cursor cursor = query(ContentUris.withAppendedId(Calendars.CONTENT_URI, id),
                new String[] {Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE,
                        Calendars.CAL_SYNC1, Calendars.SYNC_EVENTS},
                null /* selection */,
                null /* selectionArgs */,
                null /* sort */);

        Account account = null;
        String calendarUrl = null;
        boolean oldSyncEvents = false;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    final String accountName = cursor.getString(0);
                    final String accountType = cursor.getString(1);
                    account = new Account(accountName, accountType);
                    calendarUrl = cursor.getString(2);
                    oldSyncEvents = (cursor.getInt(3) != 0);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        if (account == null) {
            // should not happen?
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Cannot update subscription because account "
                        + "is empty -- should not happen.");
            }
            return;
        }

        if (TextUtils.isEmpty(calendarUrl)) {
            // Passing in a null Url will cause it to not add any extras
            // Should only happen for non-google calendars.
            calendarUrl = null;
        }

        if (oldSyncEvents == syncEvents) {
            // nothing to do
            return;
        }

        // If the calendar is not selected for syncing, then don't download
        // events.
        mDbHelper.scheduleSync(account, !syncEvents, calendarUrl);
    }

    /**
     * Call this to trigger a broadcast of the ACTION_PROVIDER_CHANGED intent.
     * This also provides a timeout, so any calls to this method will be batched
     * over a period of BROADCAST_TIMEOUT_MILLIS defined in this class.
     *
     * @param callerIsSyncAdapter whether or not the update is being triggered by a sync
     */
    private void sendUpdateNotification(boolean callerIsSyncAdapter) {
        // We use -1 to represent an update to all events
        sendUpdateNotification(-1, callerIsSyncAdapter);
    }

    /**
     * Call this to trigger a broadcast of the ACTION_PROVIDER_CHANGED intent.
     * This also provides a timeout, so any calls to this method will be batched
     * over a period of BROADCAST_TIMEOUT_MILLIS defined in this class.  The
     * actual sending of the intent is done in
     * {@link #doSendUpdateNotification()}.
     *
     * TODO add support for eventId
     *
     * @param eventId the ID of the event that changed, or -1 for no specific event
     * @param callerIsSyncAdapter whether or not the update is being triggered by a sync
     */
    private void sendUpdateNotification(long eventId,
            boolean callerIsSyncAdapter) {
        // Are there any pending broadcast requests?
        if (mBroadcastHandler.hasMessages(UPDATE_BROADCAST_MSG)) {
            // Delete any pending requests, before requeuing a fresh one
            mBroadcastHandler.removeMessages(UPDATE_BROADCAST_MSG);
        } else {
            // Because the handler does not guarantee message delivery in
            // the case that the provider is killed, we need to make sure
            // that the provider stays alive long enough to deliver the
            // notification. This empty service is sufficient to "wedge" the
            // process until we stop it here.
            mContext.startService(new Intent(mContext, EmptyService.class));
        }
        // We use a much longer delay for sync-related updates, to prevent any
        // receivers from slowing down the sync
        long delay = callerIsSyncAdapter ?
                SYNC_UPDATE_BROADCAST_TIMEOUT_MILLIS :
                UPDATE_BROADCAST_TIMEOUT_MILLIS;
        // Despite the fact that we actually only ever use one message at a time
        // for now, it is really important to call obtainMessage() to get a
        // clean instance.  This avoids potentially infinite loops resulting
        // adding the same instance to the message queue twice, since the
        // message queue implements its linked list using a field from Message.
        Message msg = mBroadcastHandler.obtainMessage(UPDATE_BROADCAST_MSG);
        mBroadcastHandler.sendMessageDelayed(msg, delay);
    }

    /**
     * This method should not ever be called directly, to prevent sending too
     * many potentially expensive broadcasts.  Instead, call
     * {@link #sendUpdateNotification(boolean)} instead.
     *
     * @see #sendUpdateNotification(boolean)
     */
    private void doSendUpdateNotification() {
        Intent intent = new Intent(Intent.ACTION_PROVIDER_CHANGED,
                CalendarContract.CONTENT_URI);
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "Sending notification intent: " + intent);
        }
        mContext.sendBroadcast(intent, null);
    }

    private static final int TRANSACTION_QUERY = 0;
    private static final int TRANSACTION_INSERT = 1;
    private static final int TRANSACTION_UPDATE = 2;
    private static final int TRANSACTION_DELETE = 3;

    // @formatter:off
    private static final String[] SYNC_WRITABLE_DEFAULT_COLUMNS = new String[] {
        CalendarContract.Calendars.DIRTY,
        CalendarContract.Calendars._SYNC_ID
    };
    private static final String[] PROVIDER_WRITABLE_DEFAULT_COLUMNS = new String[] {
    };
    // @formatter:on

    private static final int EVENTS = 1;
    private static final int EVENTS_ID = 2;
    private static final int INSTANCES = 3;
    private static final int CALENDARS = 4;
    private static final int CALENDARS_ID = 5;
    private static final int ATTENDEES = 6;
    private static final int ATTENDEES_ID = 7;
    private static final int REMINDERS = 8;
    private static final int REMINDERS_ID = 9;
    private static final int EXTENDED_PROPERTIES = 10;
    private static final int EXTENDED_PROPERTIES_ID = 11;
    private static final int CALENDAR_ALERTS = 12;
    private static final int CALENDAR_ALERTS_ID = 13;
    private static final int CALENDAR_ALERTS_BY_INSTANCE = 14;
    private static final int INSTANCES_BY_DAY = 15;
    private static final int SYNCSTATE = 16;
    private static final int SYNCSTATE_ID = 17;
    private static final int EVENT_ENTITIES = 18;
    private static final int EVENT_ENTITIES_ID = 19;
    private static final int EVENT_DAYS = 20;
    private static final int SCHEDULE_ALARM = 21;
    private static final int SCHEDULE_ALARM_REMOVE = 22;
    private static final int TIME = 23;
    private static final int CALENDAR_ENTITIES = 24;
    private static final int CALENDAR_ENTITIES_ID = 25;
    private static final int INSTANCES_SEARCH = 26;
    private static final int INSTANCES_SEARCH_BY_DAY = 27;
    private static final int PROVIDER_PROPERTIES = 28;
    private static final int EXCEPTION_ID = 29;
    private static final int EXCEPTION_ID2 = 30;
    private static final int EMMA = 31;
    private static final int COLORS = 32;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final HashMap<String, String> sInstancesProjectionMap;
    private static final HashMap<String, String> sColorsProjectionMap;
    protected static final HashMap<String, String> sCalendarsProjectionMap;
    protected static final HashMap<String, String> sEventsProjectionMap;
    private static final HashMap<String, String> sEventEntitiesProjectionMap;
    private static final HashMap<String, String> sAttendeesProjectionMap;
    private static final HashMap<String, String> sRemindersProjectionMap;
    private static final HashMap<String, String> sCalendarAlertsProjectionMap;
    private static final HashMap<String, String> sCalendarCacheProjectionMap;
    private static final HashMap<String, String> sCountProjectionMap;

    static {
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "instances/when/*/*", INSTANCES);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "instances/whenbyday/*/*", INSTANCES_BY_DAY);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "instances/search/*/*/*", INSTANCES_SEARCH);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "instances/searchbyday/*/*/*",
                INSTANCES_SEARCH_BY_DAY);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "instances/groupbyday/*/*", EVENT_DAYS);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "events", EVENTS);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "events/#", EVENTS_ID);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "event_entities", EVENT_ENTITIES);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "event_entities/#", EVENT_ENTITIES_ID);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "calendars", CALENDARS);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "calendars/#", CALENDARS_ID);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "calendar_entities", CALENDAR_ENTITIES);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "calendar_entities/#", CALENDAR_ENTITIES_ID);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "attendees", ATTENDEES);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "attendees/#", ATTENDEES_ID);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "reminders", REMINDERS);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "reminders/#", REMINDERS_ID);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "extendedproperties", EXTENDED_PROPERTIES);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "extendedproperties/#",
                EXTENDED_PROPERTIES_ID);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "calendar_alerts", CALENDAR_ALERTS);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "calendar_alerts/#", CALENDAR_ALERTS_ID);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "calendar_alerts/by_instance",
                           CALENDAR_ALERTS_BY_INSTANCE);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "syncstate", SYNCSTATE);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "syncstate/#", SYNCSTATE_ID);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, CalendarAlarmManager.SCHEDULE_ALARM_PATH,
                SCHEDULE_ALARM);
        sUriMatcher.addURI(CalendarContract.AUTHORITY,
                CalendarAlarmManager.SCHEDULE_ALARM_REMOVE_PATH, SCHEDULE_ALARM_REMOVE);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "time/#", TIME);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "time", TIME);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "properties", PROVIDER_PROPERTIES);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "exception/#", EXCEPTION_ID);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "exception/#/#", EXCEPTION_ID2);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "emma", EMMA);
        sUriMatcher.addURI(CalendarContract.AUTHORITY, "colors", COLORS);

        /** Contains just BaseColumns._COUNT */
        sCountProjectionMap = new HashMap<String, String>();
        sCountProjectionMap.put(BaseColumns._COUNT, "COUNT(*)");

        sColorsProjectionMap = new HashMap<String, String>();
        sColorsProjectionMap.put(Colors._ID, Colors._ID);
        sColorsProjectionMap.put(Colors.DATA, Colors.DATA);
        sColorsProjectionMap.put(Colors.ACCOUNT_NAME, Colors.ACCOUNT_NAME);
        sColorsProjectionMap.put(Colors.ACCOUNT_TYPE, Colors.ACCOUNT_TYPE);
        sColorsProjectionMap.put(Colors.COLOR_KEY, Colors.COLOR_KEY);
        sColorsProjectionMap.put(Colors.COLOR_TYPE, Colors.COLOR_TYPE);
        sColorsProjectionMap.put(Colors.COLOR, Colors.COLOR);

        sCalendarsProjectionMap = new HashMap<String, String>();
        sCalendarsProjectionMap.put(Calendars._ID, Calendars._ID);
        sCalendarsProjectionMap.put(Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_NAME);
        sCalendarsProjectionMap.put(Calendars.ACCOUNT_TYPE, Calendars.ACCOUNT_TYPE);
        sCalendarsProjectionMap.put(Calendars._SYNC_ID, Calendars._SYNC_ID);
        sCalendarsProjectionMap.put(Calendars.DIRTY, Calendars.DIRTY);
        sCalendarsProjectionMap.put(Calendars.MUTATORS, Calendars.MUTATORS);
        sCalendarsProjectionMap.put(Calendars.NAME, Calendars.NAME);
        sCalendarsProjectionMap.put(
                Calendars.CALENDAR_DISPLAY_NAME, Calendars.CALENDAR_DISPLAY_NAME);
        sCalendarsProjectionMap.put(Calendars.CALENDAR_COLOR, Calendars.CALENDAR_COLOR);
        sCalendarsProjectionMap.put(Calendars.CALENDAR_COLOR_KEY, Calendars.CALENDAR_COLOR_KEY);
        sCalendarsProjectionMap.put(Calendars.CALENDAR_ACCESS_LEVEL,
                Calendars.CALENDAR_ACCESS_LEVEL);
        sCalendarsProjectionMap.put(Calendars.VISIBLE, Calendars.VISIBLE);
        sCalendarsProjectionMap.put(Calendars.SYNC_EVENTS, Calendars.SYNC_EVENTS);
        sCalendarsProjectionMap.put(Calendars.CALENDAR_LOCATION, Calendars.CALENDAR_LOCATION);
        sCalendarsProjectionMap.put(Calendars.CALENDAR_TIME_ZONE, Calendars.CALENDAR_TIME_ZONE);
        sCalendarsProjectionMap.put(Calendars.OWNER_ACCOUNT, Calendars.OWNER_ACCOUNT);
        sCalendarsProjectionMap.put(Calendars.IS_PRIMARY,
                "COALESCE(" + Events.IS_PRIMARY + ", "
                        + Calendars.OWNER_ACCOUNT + " = " + Calendars.ACCOUNT_NAME + ")");
        sCalendarsProjectionMap.put(Calendars.CAN_ORGANIZER_RESPOND,
                Calendars.CAN_ORGANIZER_RESPOND);
        sCalendarsProjectionMap.put(Calendars.CAN_MODIFY_TIME_ZONE, Calendars.CAN_MODIFY_TIME_ZONE);
        sCalendarsProjectionMap.put(Calendars.CAN_PARTIALLY_UPDATE, Calendars.CAN_PARTIALLY_UPDATE);
        sCalendarsProjectionMap.put(Calendars.MAX_REMINDERS, Calendars.MAX_REMINDERS);
        sCalendarsProjectionMap.put(Calendars.ALLOWED_REMINDERS, Calendars.ALLOWED_REMINDERS);
        sCalendarsProjectionMap.put(Calendars.ALLOWED_AVAILABILITY, Calendars.ALLOWED_AVAILABILITY);
        sCalendarsProjectionMap.put(Calendars.ALLOWED_ATTENDEE_TYPES,
                Calendars.ALLOWED_ATTENDEE_TYPES);
        sCalendarsProjectionMap.put(Calendars.DELETED, Calendars.DELETED);
        sCalendarsProjectionMap.put(Calendars.CAL_SYNC1, Calendars.CAL_SYNC1);
        sCalendarsProjectionMap.put(Calendars.CAL_SYNC2, Calendars.CAL_SYNC2);
        sCalendarsProjectionMap.put(Calendars.CAL_SYNC3, Calendars.CAL_SYNC3);
        sCalendarsProjectionMap.put(Calendars.CAL_SYNC4, Calendars.CAL_SYNC4);
        sCalendarsProjectionMap.put(Calendars.CAL_SYNC5, Calendars.CAL_SYNC5);
        sCalendarsProjectionMap.put(Calendars.CAL_SYNC6, Calendars.CAL_SYNC6);
        sCalendarsProjectionMap.put(Calendars.CAL_SYNC7, Calendars.CAL_SYNC7);
        sCalendarsProjectionMap.put(Calendars.CAL_SYNC8, Calendars.CAL_SYNC8);
        sCalendarsProjectionMap.put(Calendars.CAL_SYNC9, Calendars.CAL_SYNC9);
        sCalendarsProjectionMap.put(Calendars.CAL_SYNC10, Calendars.CAL_SYNC10);

        sEventsProjectionMap = new HashMap<String, String>();
        // Events columns
        sEventsProjectionMap.put(Events.ACCOUNT_NAME, Events.ACCOUNT_NAME);
        sEventsProjectionMap.put(Events.ACCOUNT_TYPE, Events.ACCOUNT_TYPE);
        sEventsProjectionMap.put(Events.TITLE, Events.TITLE);
        sEventsProjectionMap.put(Events.EVENT_LOCATION, Events.EVENT_LOCATION);
        sEventsProjectionMap.put(Events.DESCRIPTION, Events.DESCRIPTION);
        sEventsProjectionMap.put(Events.STATUS, Events.STATUS);
        sEventsProjectionMap.put(Events.EVENT_COLOR, Events.EVENT_COLOR);
        sEventsProjectionMap.put(Events.EVENT_COLOR_KEY, Events.EVENT_COLOR_KEY);
        sEventsProjectionMap.put(Events.SELF_ATTENDEE_STATUS, Events.SELF_ATTENDEE_STATUS);
        sEventsProjectionMap.put(Events.DTSTART, Events.DTSTART);
        sEventsProjectionMap.put(Events.DTEND, Events.DTEND);
        sEventsProjectionMap.put(Events.EVENT_TIMEZONE, Events.EVENT_TIMEZONE);
        sEventsProjectionMap.put(Events.EVENT_END_TIMEZONE, Events.EVENT_END_TIMEZONE);
        sEventsProjectionMap.put(Events.DURATION, Events.DURATION);
        sEventsProjectionMap.put(Events.ALL_DAY, Events.ALL_DAY);
        sEventsProjectionMap.put(Events.ACCESS_LEVEL, Events.ACCESS_LEVEL);
        sEventsProjectionMap.put(Events.AVAILABILITY, Events.AVAILABILITY);
        sEventsProjectionMap.put(Events.HAS_ALARM, Events.HAS_ALARM);
        sEventsProjectionMap.put(Events.HAS_EXTENDED_PROPERTIES, Events.HAS_EXTENDED_PROPERTIES);
        sEventsProjectionMap.put(Events.RRULE, Events.RRULE);
        sEventsProjectionMap.put(Events.RDATE, Events.RDATE);
        sEventsProjectionMap.put(Events.EXRULE, Events.EXRULE);
        sEventsProjectionMap.put(Events.EXDATE, Events.EXDATE);
        sEventsProjectionMap.put(Events.ORIGINAL_SYNC_ID, Events.ORIGINAL_SYNC_ID);
        sEventsProjectionMap.put(Events.ORIGINAL_ID, Events.ORIGINAL_ID);
        sEventsProjectionMap.put(Events.ORIGINAL_INSTANCE_TIME, Events.ORIGINAL_INSTANCE_TIME);
        sEventsProjectionMap.put(Events.ORIGINAL_ALL_DAY, Events.ORIGINAL_ALL_DAY);
        sEventsProjectionMap.put(Events.LAST_DATE, Events.LAST_DATE);
        sEventsProjectionMap.put(Events.HAS_ATTENDEE_DATA, Events.HAS_ATTENDEE_DATA);
        sEventsProjectionMap.put(Events.CALENDAR_ID, Events.CALENDAR_ID);
        sEventsProjectionMap.put(Events.GUESTS_CAN_INVITE_OTHERS, Events.GUESTS_CAN_INVITE_OTHERS);
        sEventsProjectionMap.put(Events.GUESTS_CAN_MODIFY, Events.GUESTS_CAN_MODIFY);
        sEventsProjectionMap.put(Events.GUESTS_CAN_SEE_GUESTS, Events.GUESTS_CAN_SEE_GUESTS);
        sEventsProjectionMap.put(Events.ORGANIZER, Events.ORGANIZER);
        sEventsProjectionMap.put(Events.IS_ORGANIZER, Events.IS_ORGANIZER);
        sEventsProjectionMap.put(Events.CUSTOM_APP_PACKAGE, Events.CUSTOM_APP_PACKAGE);
        sEventsProjectionMap.put(Events.CUSTOM_APP_URI, Events.CUSTOM_APP_URI);
        sEventsProjectionMap.put(Events.UID_2445, Events.UID_2445);
        sEventsProjectionMap.put(Events.DELETED, Events.DELETED);
        sEventsProjectionMap.put(Events._SYNC_ID, Events._SYNC_ID);

        // Put the shared items into the Attendees, Reminders projection map
        sAttendeesProjectionMap = new HashMap<String, String>(sEventsProjectionMap);
        sRemindersProjectionMap = new HashMap<String, String>(sEventsProjectionMap);

        // Calendar columns
        sEventsProjectionMap.put(Calendars.CALENDAR_COLOR, Calendars.CALENDAR_COLOR);
        sEventsProjectionMap.put(Calendars.CALENDAR_COLOR_KEY, Calendars.CALENDAR_COLOR_KEY);
        sEventsProjectionMap.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CALENDAR_ACCESS_LEVEL);
        sEventsProjectionMap.put(Calendars.VISIBLE, Calendars.VISIBLE);
        sEventsProjectionMap.put(Calendars.CALENDAR_TIME_ZONE, Calendars.CALENDAR_TIME_ZONE);
        sEventsProjectionMap.put(Calendars.OWNER_ACCOUNT, Calendars.OWNER_ACCOUNT);
        sEventsProjectionMap.put(Calendars.CALENDAR_DISPLAY_NAME, Calendars.CALENDAR_DISPLAY_NAME);
        sEventsProjectionMap.put(Calendars.ALLOWED_REMINDERS, Calendars.ALLOWED_REMINDERS);
        sEventsProjectionMap
                .put(Calendars.ALLOWED_ATTENDEE_TYPES, Calendars.ALLOWED_ATTENDEE_TYPES);
        sEventsProjectionMap.put(Calendars.ALLOWED_AVAILABILITY, Calendars.ALLOWED_AVAILABILITY);
        sEventsProjectionMap.put(Calendars.MAX_REMINDERS, Calendars.MAX_REMINDERS);
        sEventsProjectionMap.put(Calendars.CAN_ORGANIZER_RESPOND, Calendars.CAN_ORGANIZER_RESPOND);
        sEventsProjectionMap.put(Calendars.CAN_MODIFY_TIME_ZONE, Calendars.CAN_MODIFY_TIME_ZONE);
        sEventsProjectionMap.put(Events.DISPLAY_COLOR, Events.DISPLAY_COLOR);

        // Put the shared items into the Instances projection map
        // The Instances and CalendarAlerts are joined with Calendars, so the projections include
        // the above Calendar columns.
        sInstancesProjectionMap = new HashMap<String, String>(sEventsProjectionMap);
        sCalendarAlertsProjectionMap = new HashMap<String, String>(sEventsProjectionMap);

        sEventsProjectionMap.put(Events._ID, Events._ID);
        sEventsProjectionMap.put(Events.SYNC_DATA1, Events.SYNC_DATA1);
        sEventsProjectionMap.put(Events.SYNC_DATA2, Events.SYNC_DATA2);
        sEventsProjectionMap.put(Events.SYNC_DATA3, Events.SYNC_DATA3);
        sEventsProjectionMap.put(Events.SYNC_DATA4, Events.SYNC_DATA4);
        sEventsProjectionMap.put(Events.SYNC_DATA5, Events.SYNC_DATA5);
        sEventsProjectionMap.put(Events.SYNC_DATA6, Events.SYNC_DATA6);
        sEventsProjectionMap.put(Events.SYNC_DATA7, Events.SYNC_DATA7);
        sEventsProjectionMap.put(Events.SYNC_DATA8, Events.SYNC_DATA8);
        sEventsProjectionMap.put(Events.SYNC_DATA9, Events.SYNC_DATA9);
        sEventsProjectionMap.put(Events.SYNC_DATA10, Events.SYNC_DATA10);
        sEventsProjectionMap.put(Calendars.CAL_SYNC1, Calendars.CAL_SYNC1);
        sEventsProjectionMap.put(Calendars.CAL_SYNC2, Calendars.CAL_SYNC2);
        sEventsProjectionMap.put(Calendars.CAL_SYNC3, Calendars.CAL_SYNC3);
        sEventsProjectionMap.put(Calendars.CAL_SYNC4, Calendars.CAL_SYNC4);
        sEventsProjectionMap.put(Calendars.CAL_SYNC5, Calendars.CAL_SYNC5);
        sEventsProjectionMap.put(Calendars.CAL_SYNC6, Calendars.CAL_SYNC6);
        sEventsProjectionMap.put(Calendars.CAL_SYNC7, Calendars.CAL_SYNC7);
        sEventsProjectionMap.put(Calendars.CAL_SYNC8, Calendars.CAL_SYNC8);
        sEventsProjectionMap.put(Calendars.CAL_SYNC9, Calendars.CAL_SYNC9);
        sEventsProjectionMap.put(Calendars.CAL_SYNC10, Calendars.CAL_SYNC10);
        sEventsProjectionMap.put(Events.DIRTY, Events.DIRTY);
        sEventsProjectionMap.put(Events.MUTATORS, Events.MUTATORS);
        sEventsProjectionMap.put(Events.LAST_SYNCED, Events.LAST_SYNCED);

        sEventEntitiesProjectionMap = new HashMap<String, String>();
        sEventEntitiesProjectionMap.put(Events.TITLE, Events.TITLE);
        sEventEntitiesProjectionMap.put(Events.EVENT_LOCATION, Events.EVENT_LOCATION);
        sEventEntitiesProjectionMap.put(Events.DESCRIPTION, Events.DESCRIPTION);
        sEventEntitiesProjectionMap.put(Events.STATUS, Events.STATUS);
        sEventEntitiesProjectionMap.put(Events.EVENT_COLOR, Events.EVENT_COLOR);
        sEventEntitiesProjectionMap.put(Events.EVENT_COLOR_KEY, Events.EVENT_COLOR_KEY);
        sEventEntitiesProjectionMap.put(Events.SELF_ATTENDEE_STATUS, Events.SELF_ATTENDEE_STATUS);
        sEventEntitiesProjectionMap.put(Events.DTSTART, Events.DTSTART);
        sEventEntitiesProjectionMap.put(Events.DTEND, Events.DTEND);
        sEventEntitiesProjectionMap.put(Events.EVENT_TIMEZONE, Events.EVENT_TIMEZONE);
        sEventEntitiesProjectionMap.put(Events.EVENT_END_TIMEZONE, Events.EVENT_END_TIMEZONE);
        sEventEntitiesProjectionMap.put(Events.DURATION, Events.DURATION);
        sEventEntitiesProjectionMap.put(Events.ALL_DAY, Events.ALL_DAY);
        sEventEntitiesProjectionMap.put(Events.ACCESS_LEVEL, Events.ACCESS_LEVEL);
        sEventEntitiesProjectionMap.put(Events.AVAILABILITY, Events.AVAILABILITY);
        sEventEntitiesProjectionMap.put(Events.HAS_ALARM, Events.HAS_ALARM);
        sEventEntitiesProjectionMap.put(Events.HAS_EXTENDED_PROPERTIES,
                Events.HAS_EXTENDED_PROPERTIES);
        sEventEntitiesProjectionMap.put(Events.RRULE, Events.RRULE);
        sEventEntitiesProjectionMap.put(Events.RDATE, Events.RDATE);
        sEventEntitiesProjectionMap.put(Events.EXRULE, Events.EXRULE);
        sEventEntitiesProjectionMap.put(Events.EXDATE, Events.EXDATE);
        sEventEntitiesProjectionMap.put(Events.ORIGINAL_SYNC_ID, Events.ORIGINAL_SYNC_ID);
        sEventEntitiesProjectionMap.put(Events.ORIGINAL_ID, Events.ORIGINAL_ID);
        sEventEntitiesProjectionMap.put(Events.ORIGINAL_INSTANCE_TIME,
                Events.ORIGINAL_INSTANCE_TIME);
        sEventEntitiesProjectionMap.put(Events.ORIGINAL_ALL_DAY, Events.ORIGINAL_ALL_DAY);
        sEventEntitiesProjectionMap.put(Events.LAST_DATE, Events.LAST_DATE);
        sEventEntitiesProjectionMap.put(Events.HAS_ATTENDEE_DATA, Events.HAS_ATTENDEE_DATA);
        sEventEntitiesProjectionMap.put(Events.CALENDAR_ID, Events.CALENDAR_ID);
        sEventEntitiesProjectionMap.put(Events.GUESTS_CAN_INVITE_OTHERS,
                Events.GUESTS_CAN_INVITE_OTHERS);
        sEventEntitiesProjectionMap.put(Events.GUESTS_CAN_MODIFY, Events.GUESTS_CAN_MODIFY);
        sEventEntitiesProjectionMap.put(Events.GUESTS_CAN_SEE_GUESTS, Events.GUESTS_CAN_SEE_GUESTS);
        sEventEntitiesProjectionMap.put(Events.ORGANIZER, Events.ORGANIZER);
        sEventEntitiesProjectionMap.put(Events.IS_ORGANIZER, Events.IS_ORGANIZER);
        sEventEntitiesProjectionMap.put(Events.CUSTOM_APP_PACKAGE, Events.CUSTOM_APP_PACKAGE);
        sEventEntitiesProjectionMap.put(Events.CUSTOM_APP_URI, Events.CUSTOM_APP_URI);
        sEventEntitiesProjectionMap.put(Events.UID_2445, Events.UID_2445);
        sEventEntitiesProjectionMap.put(Events.DELETED, Events.DELETED);
        sEventEntitiesProjectionMap.put(Events._ID, Events._ID);
        sEventEntitiesProjectionMap.put(Events._SYNC_ID, Events._SYNC_ID);
        sEventEntitiesProjectionMap.put(Events.SYNC_DATA1, Events.SYNC_DATA1);
        sEventEntitiesProjectionMap.put(Events.SYNC_DATA2, Events.SYNC_DATA2);
        sEventEntitiesProjectionMap.put(Events.SYNC_DATA3, Events.SYNC_DATA3);
        sEventEntitiesProjectionMap.put(Events.SYNC_DATA4, Events.SYNC_DATA4);
        sEventEntitiesProjectionMap.put(Events.SYNC_DATA5, Events.SYNC_DATA5);
        sEventEntitiesProjectionMap.put(Events.SYNC_DATA6, Events.SYNC_DATA6);
        sEventEntitiesProjectionMap.put(Events.SYNC_DATA7, Events.SYNC_DATA7);
        sEventEntitiesProjectionMap.put(Events.SYNC_DATA8, Events.SYNC_DATA8);
        sEventEntitiesProjectionMap.put(Events.SYNC_DATA9, Events.SYNC_DATA9);
        sEventEntitiesProjectionMap.put(Events.SYNC_DATA10, Events.SYNC_DATA10);
        sEventEntitiesProjectionMap.put(Events.DIRTY, Events.DIRTY);
        sEventEntitiesProjectionMap.put(Events.MUTATORS, Events.MUTATORS);
        sEventEntitiesProjectionMap.put(Events.LAST_SYNCED, Events.LAST_SYNCED);
        sEventEntitiesProjectionMap.put(Calendars.CAL_SYNC1, Calendars.CAL_SYNC1);
        sEventEntitiesProjectionMap.put(Calendars.CAL_SYNC2, Calendars.CAL_SYNC2);
        sEventEntitiesProjectionMap.put(Calendars.CAL_SYNC3, Calendars.CAL_SYNC3);
        sEventEntitiesProjectionMap.put(Calendars.CAL_SYNC4, Calendars.CAL_SYNC4);
        sEventEntitiesProjectionMap.put(Calendars.CAL_SYNC5, Calendars.CAL_SYNC5);
        sEventEntitiesProjectionMap.put(Calendars.CAL_SYNC6, Calendars.CAL_SYNC6);
        sEventEntitiesProjectionMap.put(Calendars.CAL_SYNC7, Calendars.CAL_SYNC7);
        sEventEntitiesProjectionMap.put(Calendars.CAL_SYNC8, Calendars.CAL_SYNC8);
        sEventEntitiesProjectionMap.put(Calendars.CAL_SYNC9, Calendars.CAL_SYNC9);
        sEventEntitiesProjectionMap.put(Calendars.CAL_SYNC10, Calendars.CAL_SYNC10);

        // Instances columns
        sInstancesProjectionMap.put(Events.DELETED, "Events.deleted as deleted");
        sInstancesProjectionMap.put(Instances.BEGIN, "begin");
        sInstancesProjectionMap.put(Instances.END, "end");
        sInstancesProjectionMap.put(Instances.EVENT_ID, "Instances.event_id AS event_id");
        sInstancesProjectionMap.put(Instances._ID, "Instances._id AS _id");
        sInstancesProjectionMap.put(Instances.START_DAY, "startDay");
        sInstancesProjectionMap.put(Instances.END_DAY, "endDay");
        sInstancesProjectionMap.put(Instances.START_MINUTE, "startMinute");
        sInstancesProjectionMap.put(Instances.END_MINUTE, "endMinute");

        // Attendees columns
        sAttendeesProjectionMap.put(Attendees.EVENT_ID, "event_id");
        sAttendeesProjectionMap.put(Attendees._ID, "Attendees._id AS _id");
        sAttendeesProjectionMap.put(Attendees.ATTENDEE_NAME, "attendeeName");
        sAttendeesProjectionMap.put(Attendees.ATTENDEE_EMAIL, "attendeeEmail");
        sAttendeesProjectionMap.put(Attendees.ATTENDEE_STATUS, "attendeeStatus");
        sAttendeesProjectionMap.put(Attendees.ATTENDEE_RELATIONSHIP, "attendeeRelationship");
        sAttendeesProjectionMap.put(Attendees.ATTENDEE_TYPE, "attendeeType");
        sAttendeesProjectionMap.put(Attendees.ATTENDEE_IDENTITY, "attendeeIdentity");
        sAttendeesProjectionMap.put(Attendees.ATTENDEE_ID_NAMESPACE, "attendeeIdNamespace");
        sAttendeesProjectionMap.put(Events.DELETED, "Events.deleted AS deleted");
        sAttendeesProjectionMap.put(Events._SYNC_ID, "Events._sync_id AS _sync_id");

        // Reminders columns
        sRemindersProjectionMap.put(Reminders.EVENT_ID, "event_id");
        sRemindersProjectionMap.put(Reminders._ID, "Reminders._id AS _id");
        sRemindersProjectionMap.put(Reminders.MINUTES, "minutes");
        sRemindersProjectionMap.put(Reminders.METHOD, "method");
        sRemindersProjectionMap.put(Events.DELETED, "Events.deleted AS deleted");
        sRemindersProjectionMap.put(Events._SYNC_ID, "Events._sync_id AS _sync_id");

        // CalendarAlerts columns
        sCalendarAlertsProjectionMap.put(CalendarAlerts.EVENT_ID, "event_id");
        sCalendarAlertsProjectionMap.put(CalendarAlerts._ID, "CalendarAlerts._id AS _id");
        sCalendarAlertsProjectionMap.put(CalendarAlerts.BEGIN, "begin");
        sCalendarAlertsProjectionMap.put(CalendarAlerts.END, "end");
        sCalendarAlertsProjectionMap.put(CalendarAlerts.ALARM_TIME, "alarmTime");
        sCalendarAlertsProjectionMap.put(CalendarAlerts.NOTIFY_TIME, "notifyTime");
        sCalendarAlertsProjectionMap.put(CalendarAlerts.STATE, "state");
        sCalendarAlertsProjectionMap.put(CalendarAlerts.MINUTES, "minutes");

        // CalendarCache columns
        sCalendarCacheProjectionMap = new HashMap<String, String>();
        sCalendarCacheProjectionMap.put(CalendarCache.COLUMN_NAME_KEY, "key");
        sCalendarCacheProjectionMap.put(CalendarCache.COLUMN_NAME_VALUE, "value");
    }


    /**
     * This is called by AccountManager when the set of accounts is updated.
     * <p>
     * We are overriding this since we need to delete from the
     * Calendars table, which is not syncable, which has triggers that
     * will delete from the Events and  tables, which are
     * syncable.  TODO: update comment, make sure deletes don't get synced.
     *
     * @param accounts The list of currently active accounts.
     */
    @Override
    public void onAccountsUpdated(Account[] accounts) {
        Thread thread = new AccountsUpdatedThread(accounts);
        thread.start();
    }

    private class AccountsUpdatedThread extends Thread {
        private Account[] mAccounts;

        AccountsUpdatedThread(Account[] accounts) {
            mAccounts = accounts;
        }

        @Override
        public void run() {
            // The process could be killed while the thread runs.  Right now that isn't a problem,
            // because we'll just call removeStaleAccounts() again when the provider restarts, but
            // if we want to do additional actions we may need to use a service (e.g. start
            // EmptyService in onAccountsUpdated() and stop it when we finish here).

            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            removeStaleAccounts(mAccounts);
        }
    }

    /**
     * Makes sure there are no entries for accounts that no longer exist.
     */
    private void removeStaleAccounts(Account[] accounts) {
        if (mDb == null) {
            mDb = mDbHelper.getWritableDatabase();
        }
        if (mDb == null) {
            return;
        }

        HashSet<Account> validAccounts = new HashSet<Account>();
        for (Account account : accounts) {
            validAccounts.add(new Account(account.name, account.type));
        }
        ArrayList<Account> accountsToDelete = new ArrayList<Account>();

        mDb.beginTransaction();
        Cursor c = null;
        try {

            for (String table : new String[]{Tables.CALENDARS, Tables.COLORS}) {
                // Find all the accounts the calendar DB knows about, mark the ones that aren't
                // in the valid set for deletion.
                c = mDb.rawQuery("SELECT DISTINCT " +
                                            Calendars.ACCOUNT_NAME +
                                            "," +
                                            Calendars.ACCOUNT_TYPE +
                                        " FROM " + table, null);
                while (c.moveToNext()) {
                    // ACCOUNT_TYPE_LOCAL is to store calendars not associated
                    // with a system account. Typically, a calendar must be
                    // associated with an account on the device or it will be
                    // deleted.
                    if (c.getString(0) != null
                            && c.getString(1) != null
                            && !TextUtils.equals(c.getString(1),
                                    CalendarContract.ACCOUNT_TYPE_LOCAL)) {
                        Account currAccount = new Account(c.getString(0), c.getString(1));
                        if (!validAccounts.contains(currAccount)) {
                            accountsToDelete.add(currAccount);
                        }
                    }
                }
                c.close();
                c = null;
            }

            for (Account account : accountsToDelete) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "removing data for removed account " + account);
                }
                String[] params = new String[]{account.name, account.type};
                mDb.execSQL(SQL_DELETE_FROM_CALENDARS, params);
                // This will be a no-op for accounts without a color palette.
                mDb.execSQL(SQL_DELETE_FROM_COLORS, params);
            }
            mDbHelper.getSyncState().onAccountsChanged(mDb, accounts);
            mDb.setTransactionSuccessful();
        } finally {
            if (c != null) {
                c.close();
            }
            mDb.endTransaction();
        }

        // make sure the widget reflects the account changes
        sendUpdateNotification(false);
    }

    /**
     * Inserts an argument at the beginning of the selection arg list.
     *
     * The {@link android.database.sqlite.SQLiteQueryBuilder}'s where clause is
     * prepended to the user's where clause (combined with 'AND') to generate
     * the final where close, so arguments associated with the QueryBuilder are
     * prepended before any user selection args to keep them in the right order.
     */
    private String[] insertSelectionArg(String[] selectionArgs, String arg) {
        if (selectionArgs == null) {
            return new String[] {arg};
        } else {
            int newLength = selectionArgs.length + 1;
            String[] newSelectionArgs = new String[newLength];
            newSelectionArgs[0] = arg;
            System.arraycopy(selectionArgs, 0, newSelectionArgs, 1, selectionArgs.length);
            return newSelectionArgs;
        }
    }

    private String getCallingPackageName() {
        final PackageManager pm = getContext().getPackageManager();
        final int uid = Binder.getCallingUid();
        final String[] packages = pm.getPackagesForUid(uid);
        if (packages != null && packages.length == 1) {
            return packages[0];
        }
        final String name = pm.getNameForUid(uid);
        if (name != null) {
            return name;
        }
        return String.valueOf(uid);
    }

    private void addMutator(ContentValues values, String columnName) {
        final String packageName = getCallingPackageName();
        final String mutators = values.getAsString(columnName);
        if (TextUtils.isEmpty(mutators)) {
            values.put(columnName, packageName);
        } else {
            values.put(columnName, mutators + "," + packageName);
        }
    }
}
