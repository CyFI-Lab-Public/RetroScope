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

package com.android.providers.calendar;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.test.AndroidTestCase;
import android.test.IsolatedContext;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Smoke;
import android.test.suitebuilder.annotation.Suppress;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Runs various tests on an isolated Calendar provider with its own database.
 *
 * You can run the tests with the following command line:
 *
 * adb shell am instrument
 * -e debug false
 * -w
 * -e class com.android.providers.calendar.CalendarProvider2Test
 * com.android.providers.calendar.tests/android.test.InstrumentationTestRunner
 *
 * This test no longer extends ProviderTestCase2 because it actually doesn't
 * allow you to inject a custom context (which we needed to mock out the calls
 * to start a service). We the next best thing, which is copy the relevant code
 * from PTC2 and extend AndroidTestCase instead.
 */
// flaky test, add back to LargeTest when fixed - bug 2395696
// @LargeTest
public class CalendarProvider2Test extends AndroidTestCase {
    static final String TAG = "calendar";

    private static final String DEFAULT_ACCOUNT_TYPE = "com.google";
    private static final String DEFAULT_ACCOUNT = "joe@joe.com";


    private static final String WHERE_CALENDARS_SELECTED = Calendars.VISIBLE + "=?";
    private static final String[] WHERE_CALENDARS_ARGS = {
        "1"
    };
    private static final String WHERE_COLOR_ACCOUNT_AND_INDEX = Colors.ACCOUNT_NAME + "=? AND "
            + Colors.ACCOUNT_TYPE + "=? AND " + Colors.COLOR_KEY + "=?";
    private static final String DEFAULT_SORT_ORDER = "begin ASC";

    private CalendarProvider2ForTesting mProvider;
    private SQLiteDatabase mDb;
    private MetaData mMetaData;
    private Context mContext;
    private MockContentResolver mResolver;
    private Uri mEventsUri = Events.CONTENT_URI;
    private Uri mCalendarsUri = Calendars.CONTENT_URI;
    private int mCalendarId;

    protected boolean mWipe = false;
    protected boolean mForceDtend = false;

    // We need a unique id to put in the _sync_id field so that we can create
    // recurrence exceptions that refer to recurring events.
    private int mGlobalSyncId = 1000;
    private static final String CALENDAR_URL =
            "http://www.google.com/calendar/feeds/joe%40joe.com/private/full";

    private static final String TIME_ZONE_AMERICA_ANCHORAGE = "America/Anchorage";
    private static final String TIME_ZONE_AMERICA_LOS_ANGELES = "America/Los_Angeles";
    private static final String DEFAULT_TIMEZONE = TIME_ZONE_AMERICA_LOS_ANGELES;

    private static final String MOCK_TIME_ZONE_DATABASE_VERSION = "2010a";

    private static final long ONE_MINUTE_MILLIS = 60*1000;
    private static final long ONE_HOUR_MILLIS = 3600*1000;
    private static final long ONE_WEEK_MILLIS = 7 * 24 * 3600 * 1000;

    /**
     * We need a few more stub methods so that our tests can run
     */
    protected class MockContext2 extends MockContext {

        @Override
        public String getPackageName() {
            return getContext().getPackageName();
        }

        @Override
        public Resources getResources() {
            return getContext().getResources();
        }

        @Override
        public File getDir(String name, int mode) {
            // name the directory so the directory will be seperated from
            // one created through the regular Context
            return getContext().getDir("mockcontext2_" + name, mode);
        }

        @Override
        public ComponentName startService(Intent service) {
            return null;
        }

        @Override
        public boolean stopService(Intent service) {
            return false;
        }
    }

    /**
     * KeyValue is a simple class that stores a pair of strings representing
     * a (key, value) pair.  This is used for updating events.
     */
    private class KeyValue {
        String key;
        String value;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * A generic command interface.  This is used to support a sequence of
     * commands that can create events, delete or update events, and then
     * check that the state of the database is as expected.
     */
    private interface Command {
        public void execute();
    }

    /**
     * This is used to insert a new event into the database.  The event is
     * specified by its name (or "title").  All of the event fields (the
     * start and end time, whether it is an all-day event, and so on) are
     * stored in a separate table (the "mEvents" table).
     */
    private class Insert implements Command {
        EventInfo eventInfo;

        public Insert(String eventName) {
            eventInfo = findEvent(eventName);
        }

        public void execute() {
            Log.i(TAG, "insert " + eventInfo.mTitle);
            insertEvent(mCalendarId, eventInfo);
        }
    }

    /**
     * This is used to delete an event, specified by the event name.
     */
    private class Delete implements Command {
        String eventName;
        String account;
        String accountType;
        int expected;

        public Delete(String eventName, int expected, String account, String accountType) {
            this.eventName = eventName;
            this.expected = expected;
            this.account = account;
            this.accountType = accountType;
        }

        public void execute() {
            Log.i(TAG, "delete " + eventName);
            int rows = deleteMatchingEvents(eventName, account, accountType);
            assertEquals(expected, rows);
        }
    }

    /**
     * This is used to update an event.  The values to update are specified
     * with an array of (key, value) pairs.  Both the key and value are
     * specified as strings.  Event fields that are not really strings (such
     * as DTSTART which is a long) should be converted to the appropriate type
     * but that isn't supported yet.  When needed, that can be added here
     * by checking for specific keys and converting the associated values.
     */
    private class Update implements Command {
        String eventName;
        KeyValue[] pairs;

        public Update(String eventName, KeyValue[] pairs) {
            this.eventName = eventName;
            this.pairs = pairs;
        }

        public void execute() {
            Log.i(TAG, "update " + eventName);
            if (mWipe) {
                // Wipe instance table so it will be regenerated
                mMetaData.clearInstanceRange();
            }
            ContentValues map = new ContentValues();
            for (KeyValue pair : pairs) {
                String value = pair.value;
                if (CalendarContract.Events.STATUS.equals(pair.key)) {
                    // Do type conversion for STATUS
                    map.put(pair.key, Integer.parseInt(value));
                } else {
                    map.put(pair.key, value);
                }
            }
            if (map.size() == 1 && map.containsKey(Events.STATUS)) {
                updateMatchingEventsStatusOnly(eventName, map);
            } else {
                updateMatchingEvents(eventName, map);
            }
        }
    }

    /**
     * This command queries the number of events and compares it to the given
     * expected value.
     */
    private class QueryNumEvents implements Command {
        int expected;

        public QueryNumEvents(int expected) {
            this.expected = expected;
        }

        public void execute() {
            Cursor cursor = mResolver.query(mEventsUri, null, null, null, null);
            assertEquals(expected, cursor.getCount());
            cursor.close();
        }
    }


    /**
     * This command dumps the list of events to the log for debugging.
     */
    private class DumpEvents implements Command {

        public DumpEvents() {
        }

        public void execute() {
            Cursor cursor = mResolver.query(mEventsUri, null, null, null, null);
            dumpCursor(cursor);
            cursor.close();
        }
    }

    /**
     * This command dumps the list of instances to the log for debugging.
     */
    private class DumpInstances implements Command {
        long begin;
        long end;

        public DumpInstances(String startDate, String endDate) {
            Time time = new Time(DEFAULT_TIMEZONE);
            time.parse3339(startDate);
            begin = time.toMillis(false /* use isDst */);
            time.parse3339(endDate);
            end = time.toMillis(false /* use isDst */);
        }

        public void execute() {
            Cursor cursor = queryInstances(begin, end);
            dumpCursor(cursor);
            cursor.close();
        }
    }

    /**
     * This command queries the number of instances and compares it to the given
     * expected value.
     */
    private class QueryNumInstances implements Command {
        int expected;
        long begin;
        long end;

        public QueryNumInstances(String startDate, String endDate, int expected) {
            Time time = new Time(DEFAULT_TIMEZONE);
            time.parse3339(startDate);
            begin = time.toMillis(false /* use isDst */);
            time.parse3339(endDate);
            end = time.toMillis(false /* use isDst */);
            this.expected = expected;
        }

        public void execute() {
            Cursor cursor = queryInstances(begin, end);
            assertEquals(expected, cursor.getCount());
            cursor.close();
        }
    }

    /**
     * When this command runs it verifies that all of the instances in the
     * given range match the expected instances (each instance is specified by
     * a start date).
     * If you just want to verify that an instance exists in a given date
     * range, use {@link VerifyInstance} instead.
     */
    private class VerifyAllInstances implements Command {
        long[] instances;
        long begin;
        long end;

        public VerifyAllInstances(String startDate, String endDate, String[] dates) {
            Time time = new Time(DEFAULT_TIMEZONE);
            time.parse3339(startDate);
            begin = time.toMillis(false /* use isDst */);
            time.parse3339(endDate);
            end = time.toMillis(false /* use isDst */);

            if (dates == null) {
                return;
            }

            // Convert all the instance date strings to UTC milliseconds
            int len = dates.length;
            this.instances = new long[len];
            int index = 0;
            for (String instance : dates) {
                time.parse3339(instance);
                this.instances[index++] = time.toMillis(false /* use isDst */);
            }
        }

        public void execute() {
            Cursor cursor = queryInstances(begin, end);
            int len = 0;
            if (instances != null) {
                len = instances.length;
            }
            if (len != cursor.getCount()) {
                dumpCursor(cursor);
            }
            assertEquals("number of instances don't match", len, cursor.getCount());

            if (instances == null) {
                return;
            }

            int beginColumn = cursor.getColumnIndex(Instances.BEGIN);
            while (cursor.moveToNext()) {
                long begin = cursor.getLong(beginColumn);

                // Search the list of expected instances for a matching start
                // time.
                boolean found = false;
                for (long instance : instances) {
                    if (instance == begin) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    int titleColumn = cursor.getColumnIndex(Events.TITLE);
                    int allDayColumn = cursor.getColumnIndex(Events.ALL_DAY);

                    String title = cursor.getString(titleColumn);
                    boolean allDay = cursor.getInt(allDayColumn) != 0;
                    int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE |
                            DateUtils.FORMAT_24HOUR;
                    if (allDay) {
                        flags |= DateUtils.FORMAT_UTC;
                    } else {
                        flags |= DateUtils.FORMAT_SHOW_TIME;
                    }
                    String date = DateUtils.formatDateRange(mContext, begin, begin, flags);
                    String mesg = String.format("Test failed!"
                            + " unexpected instance (\"%s\") at %s",
                            title, date);
                    Log.e(TAG, mesg);
                }
                if (!found) {
                    dumpCursor(cursor);
                }
                assertTrue(found);
            }
            cursor.close();
        }
    }

    /**
     * When this command runs it verifies that the given instance exists in
     * the given date range.
     */
    private class VerifyInstance implements Command {
        long instance;
        boolean allDay;
        long begin;
        long end;

        /**
         * Creates a command to check that the given range [startDate,endDate]
         * contains a specific instance of an event (specified by "date").
         *
         * @param startDate the beginning of the date range
         * @param endDate the end of the date range
         * @param date the date or date-time string of an event instance
         */
        public VerifyInstance(String startDate, String endDate, String date) {
            Time time = new Time(DEFAULT_TIMEZONE);
            time.parse3339(startDate);
            begin = time.toMillis(false /* use isDst */);
            time.parse3339(endDate);
            end = time.toMillis(false /* use isDst */);

            // Convert the instance date string to UTC milliseconds
            time.parse3339(date);
            allDay = time.allDay;
            instance = time.toMillis(false /* use isDst */);
        }

        public void execute() {
            Cursor cursor = queryInstances(begin, end);
            int beginColumn = cursor.getColumnIndex(Instances.BEGIN);
            boolean found = false;
            while (cursor.moveToNext()) {
                long begin = cursor.getLong(beginColumn);

                if (instance == begin) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE;
                if (allDay) {
                    flags |= DateUtils.FORMAT_UTC;
                } else {
                    flags |= DateUtils.FORMAT_SHOW_TIME;
                }
                String date = DateUtils.formatDateRange(mContext, instance, instance, flags);
                String mesg = String.format("Test failed!"
                        + " cannot find instance at %s",
                        date);
                Log.e(TAG, mesg);
            }
            assertTrue(found);
            cursor.close();
        }
    }

    /**
     * This class stores all the useful information about an event.
     */
    private class EventInfo {
        String mTitle;
        String mDescription;
        String mTimezone;
        boolean mAllDay;
        long mDtstart;
        long mDtend;
        String mRrule;
        String mDuration;
        String mOriginalTitle;
        long mOriginalInstance;
        int mSyncId;
        String mCustomAppPackage;
        String mCustomAppUri;
        String mUid2445;

        // Constructor for normal events, using the default timezone
        public EventInfo(String title, String startDate, String endDate,
                boolean allDay) {
            init(title, startDate, endDate, allDay, DEFAULT_TIMEZONE);
        }

        // Constructor for normal events, specifying the timezone
        public EventInfo(String title, String startDate, String endDate,
                boolean allDay, String timezone) {
            init(title, startDate, endDate, allDay, timezone);
        }

        public void init(String title, String startDate, String endDate,
                boolean allDay, String timezone) {
            mTitle = title;
            Time time = new Time();
            if (allDay) {
                time.timezone = Time.TIMEZONE_UTC;
            } else if (timezone != null) {
                time.timezone = timezone;
            }
            mTimezone = time.timezone;
            time.parse3339(startDate);
            mDtstart = time.toMillis(false /* use isDst */);
            time.parse3339(endDate);
            mDtend = time.toMillis(false /* use isDst */);
            mDuration = null;
            mRrule = null;
            mAllDay = allDay;
            mCustomAppPackage = "CustomAppPackage-" + mTitle;
            mCustomAppUri = "CustomAppUri-" + mTitle;
            mUid2445 = null;
        }

        // Constructor for repeating events, using the default timezone
        public EventInfo(String title, String description, String startDate, String endDate,
                String rrule, boolean allDay) {
            init(title, description, startDate, endDate, rrule, allDay, DEFAULT_TIMEZONE);
        }

        // Constructor for repeating events, specifying the timezone
        public EventInfo(String title, String description, String startDate, String endDate,
                String rrule, boolean allDay, String timezone) {
            init(title, description, startDate, endDate, rrule, allDay, timezone);
        }

        public void init(String title, String description, String startDate, String endDate,
                String rrule, boolean allDay, String timezone) {
            mTitle = title;
            mDescription = description;
            Time time = new Time();
            if (allDay) {
                time.timezone = Time.TIMEZONE_UTC;
            } else if (timezone != null) {
                time.timezone = timezone;
            }
            mTimezone = time.timezone;
            time.parse3339(startDate);
            mDtstart = time.toMillis(false /* use isDst */);
            if (endDate != null) {
                time.parse3339(endDate);
                mDtend = time.toMillis(false /* use isDst */);
            }
            if (allDay) {
                long days = 1;
                if (endDate != null) {
                    days = (mDtend - mDtstart) / DateUtils.DAY_IN_MILLIS;
                }
                mDuration = "P" + days + "D";
            } else {
                long seconds = (mDtend - mDtstart) / DateUtils.SECOND_IN_MILLIS;
                mDuration = "P" + seconds + "S";
            }
            mRrule = rrule;
            mAllDay = allDay;
        }

        // Constructor for recurrence exceptions, using the default timezone
        public EventInfo(String originalTitle, String originalInstance, String title,
                String description, String startDate, String endDate, boolean allDay,
                String customPackageName, String customPackageUri, String mUid2445) {
            init(originalTitle, originalInstance,
                    title, description, startDate, endDate, allDay, DEFAULT_TIMEZONE,
                    customPackageName, customPackageUri, mUid2445);
        }

        public void init(String originalTitle, String originalInstance,
                String title, String description, String startDate, String endDate,
                boolean allDay, String timezone, String customPackageName,
                String customPackageUri, String uid2445) {
            mOriginalTitle = originalTitle;
            Time time = new Time(timezone);
            time.parse3339(originalInstance);
            mOriginalInstance = time.toMillis(false /* use isDst */);
            mCustomAppPackage = customPackageName;
            mCustomAppUri = customPackageUri;
            mUid2445 = uid2445;
            init(title, description, startDate, endDate, null /* rrule */, allDay, timezone);
        }
    }

    private class InstanceInfo {
        EventInfo mEvent;
        long mBegin;
        long mEnd;
        int mExpectedOccurrences;

        public InstanceInfo(String eventName, String startDate, String endDate, int expected) {
            // Find the test index that contains the given event name
            mEvent = findEvent(eventName);
            Time time = new Time(mEvent.mTimezone);
            time.parse3339(startDate);
            mBegin = time.toMillis(false /* use isDst */);
            time.parse3339(endDate);
            mEnd = time.toMillis(false /* use isDst */);
            mExpectedOccurrences = expected;
        }
    }

    /**
     * This is the main table of events.  The events in this table are
     * referred to by name in other places.
     */
    private EventInfo[] mEvents = {
            new EventInfo("normal0", "2008-05-01T00:00:00", "2008-05-02T00:00:00", false),
            new EventInfo("normal1", "2008-05-26T08:30:00", "2008-05-26T09:30:00", false),
            new EventInfo("normal2", "2008-05-26T14:30:00", "2008-05-26T15:30:00", false),
            new EventInfo("allday0", "2008-05-02T00:00:00", "2008-05-03T00:00:00", true),
            new EventInfo("allday1", "2008-05-02T00:00:00", "2008-05-31T00:00:00", true),
            new EventInfo("daily0", "daily from 5/1/2008 12am to 1am",
                    "2008-05-01T00:00:00", "2008-05-01T01:00:00",
                    "FREQ=DAILY;WKST=SU", false),
            new EventInfo("daily1", "daily from 5/1/2008 8:30am to 9:30am until 5/3/2008 8am",
                    "2008-05-01T08:30:00", "2008-05-01T09:30:00",
                    "FREQ=DAILY;UNTIL=20080503T150000Z;WKST=SU", false),
            new EventInfo("daily2", "daily from 5/1/2008 8:45am to 9:15am until 5/3/2008 10am",
                    "2008-05-01T08:45:00", "2008-05-01T09:15:00",
                    "FREQ=DAILY;UNTIL=20080503T170000Z;WKST=SU", false),
            new EventInfo("allday daily0", "all-day daily from 5/1/2008",
                    "2008-05-01", null,
                    "FREQ=DAILY;WKST=SU", true),
            new EventInfo("allday daily1", "all-day daily from 5/1/2008 until 5/3/2008",
                    "2008-05-01", null,
                    "FREQ=DAILY;UNTIL=20080503T000000Z;WKST=SU", true),
            new EventInfo("allday weekly0", "all-day weekly from 5/1/2008",
                    "2008-05-01", null,
                    "FREQ=WEEKLY;WKST=SU", true),
            new EventInfo("allday weekly1", "all-day for 2 days weekly from 5/1/2008",
                    "2008-05-01", "2008-05-03",
                    "FREQ=WEEKLY;WKST=SU", true),
            new EventInfo("allday yearly0", "all-day yearly on 5/1/2008",
                    "2008-05-01T", null,
                    "FREQ=YEARLY;WKST=SU", true),
            new EventInfo("weekly0", "weekly from 5/6/2008 on Tue 1pm to 2pm",
                    "2008-05-06T13:00:00", "2008-05-06T14:00:00",
                    "FREQ=WEEKLY;BYDAY=TU;WKST=MO", false),
            new EventInfo("weekly1", "every 2 weeks from 5/6/2008 on Tue from 2:30pm to 3:30pm",
                    "2008-05-06T14:30:00", "2008-05-06T15:30:00",
                    "FREQ=WEEKLY;INTERVAL=2;BYDAY=TU;WKST=MO", false),
            new EventInfo("monthly0", "monthly from 5/20/2008 on the 3rd Tues from 3pm to 4pm",
                    "2008-05-20T15:00:00", "2008-05-20T16:00:00",
                    "FREQ=MONTHLY;BYDAY=3TU;WKST=SU", false),
            new EventInfo("monthly1", "monthly from 5/1/2008 on the 1st from 12:00am to 12:10am",
                    "2008-05-01T00:00:00", "2008-05-01T00:10:00",
                    "FREQ=MONTHLY;WKST=SU;BYMONTHDAY=1", false),
            new EventInfo("monthly2", "monthly from 5/31/2008 on the 31st 11pm to midnight",
                    "2008-05-31T23:00:00", "2008-06-01T00:00:00",
                    "FREQ=MONTHLY;WKST=SU;BYMONTHDAY=31", false),
            new EventInfo("daily0", "2008-05-01T00:00:00",
                    "except0", "daily0 exception for 5/1/2008 12am, change to 5/1/2008 2am to 3am",
                    "2008-05-01T02:00:00", "2008-05-01T01:03:00", false, "AppPkg1", "AppUri1",
                    "uid2445-1"),
            new EventInfo("daily0", "2008-05-03T00:00:00",
                    "except1", "daily0 exception for 5/3/2008 12am, change to 5/3/2008 2am to 3am",
                    "2008-05-03T02:00:00", "2008-05-03T01:03:00", false, "AppPkg2", "AppUri2",
                    null),
            new EventInfo("daily0", "2008-05-02T00:00:00",
                    "except2", "daily0 exception for 5/2/2008 12am, change to 1/2/2008",
                    "2008-01-02T00:00:00", "2008-01-02T01:00:00", false, "AppPkg3", "AppUri3",
                    "12345@uid2445"),
            new EventInfo("weekly0", "2008-05-13T13:00:00",
                    "except3", "daily0 exception for 5/11/2008 1pm, change to 12/11/2008 1pm",
                    "2008-12-11T13:00:00", "2008-12-11T14:00:00", false, "AppPkg4", "AppUri4",
                    null),
            new EventInfo("weekly0", "2008-05-13T13:00:00",
                    "cancel0", "weekly0 exception for 5/13/2008 1pm",
                    "2008-05-13T13:00:00", "2008-05-13T14:00:00", false, "AppPkg5", "AppUri5",
                    null),
            new EventInfo("yearly0", "yearly on 5/1/2008 from 1pm to 2pm",
                    "2008-05-01T13:00:00", "2008-05-01T14:00:00",
                    "FREQ=YEARLY;WKST=SU", false),
    };

    /**
     * This table is used to verify the events generated by mEvents.  It checks that the
     * number of instances within a given range matches the expected number
     * of instances.
     */
    private InstanceInfo[] mInstanceRanges = {
            new InstanceInfo("daily0", "2008-05-01T00:00:00", "2008-05-01T00:01:00", 1),
            new InstanceInfo("daily0", "2008-05-01T00:00:00", "2008-05-01T01:00:00", 1),
            new InstanceInfo("daily0", "2008-05-01T00:00:00", "2008-05-02T00:00:00", 2),
            new InstanceInfo("daily0", "2008-05-01T00:00:00", "2008-05-02T23:59:00", 2),
            new InstanceInfo("daily0", "2008-05-02T00:00:00", "2008-05-02T00:01:00", 1),
            new InstanceInfo("daily0", "2008-05-02T00:00:00", "2008-05-02T01:00:00", 1),
            new InstanceInfo("daily0", "2008-05-02T00:00:00", "2008-05-03T00:00:00", 2),
            new InstanceInfo("daily0", "2008-05-01T00:00:00", "2008-05-31T23:59:00", 31),
            new InstanceInfo("daily0", "2008-05-01T00:00:00", "2008-06-01T23:59:00", 32),

            new InstanceInfo("daily1", "2008-05-01T00:00:00", "2008-05-02T00:00:00", 1),
            new InstanceInfo("daily1", "2008-05-01T00:00:00", "2008-05-31T23:59:00", 2),

            new InstanceInfo("daily2", "2008-05-01T00:00:00", "2008-05-02T00:00:00", 1),
            new InstanceInfo("daily2", "2008-05-01T00:00:00", "2008-05-31T23:59:00", 3),

            new InstanceInfo("allday daily0", "2008-05-01", "2008-05-07", 7),
            new InstanceInfo("allday daily1", "2008-05-01", "2008-05-07", 3),
            new InstanceInfo("allday weekly0", "2008-05-01", "2008-05-07", 1),
            new InstanceInfo("allday weekly0", "2008-05-01", "2008-05-08", 2),
            new InstanceInfo("allday weekly0", "2008-05-01", "2008-05-31", 5),
            new InstanceInfo("allday weekly1", "2008-05-01", "2008-05-31", 5),
            new InstanceInfo("allday yearly0", "2008-05-01", "2009-04-30", 1),
            new InstanceInfo("allday yearly0", "2008-05-01", "2009-05-02", 2),

            new InstanceInfo("weekly0", "2008-05-01T00:00:00", "2008-05-02T00:00:00", 0),
            new InstanceInfo("weekly0", "2008-05-06T00:00:00", "2008-05-07T00:00:00", 1),
            new InstanceInfo("weekly0", "2008-05-01T00:00:00", "2008-05-31T00:00:00", 4),
            new InstanceInfo("weekly0", "2008-05-01T00:00:00", "2008-06-30T00:00:00", 8),

            new InstanceInfo("weekly1", "2008-05-01T00:00:00", "2008-05-02T00:00:00", 0),
            new InstanceInfo("weekly1", "2008-05-06T00:00:00", "2008-05-07T00:00:00", 1),
            new InstanceInfo("weekly1", "2008-05-01T00:00:00", "2008-05-31T00:00:00", 2),
            new InstanceInfo("weekly1", "2008-05-01T00:00:00", "2008-06-30T00:00:00", 4),

            new InstanceInfo("monthly0", "2008-05-01T00:00:00", "2008-05-20T13:00:00", 0),
            new InstanceInfo("monthly0", "2008-05-01T00:00:00", "2008-05-20T15:00:00", 1),
            new InstanceInfo("monthly0", "2008-05-20T16:01:00", "2008-05-31T00:00:00", 0),
            new InstanceInfo("monthly0", "2008-05-20T16:01:00", "2008-06-17T14:59:00", 0),
            new InstanceInfo("monthly0", "2008-05-20T16:01:00", "2008-06-17T15:00:00", 1),
            new InstanceInfo("monthly0", "2008-05-01T00:00:00", "2008-05-31T00:00:00", 1),
            new InstanceInfo("monthly0", "2008-05-01T00:00:00", "2008-06-30T00:00:00", 2),

            new InstanceInfo("monthly1", "2008-05-01T00:00:00", "2008-05-01T01:00:00", 1),
            new InstanceInfo("monthly1", "2008-05-01T00:00:00", "2008-05-31T00:00:00", 1),
            new InstanceInfo("monthly1", "2008-05-01T00:10:00", "2008-05-31T23:59:00", 1),
            new InstanceInfo("monthly1", "2008-05-01T00:11:00", "2008-05-31T23:59:00", 0),
            new InstanceInfo("monthly1", "2008-05-01T00:00:00", "2008-06-01T00:00:00", 2),

            new InstanceInfo("monthly2", "2008-05-01T00:00:00", "2008-05-31T00:00:00", 0),
            new InstanceInfo("monthly2", "2008-05-01T00:10:00", "2008-05-31T23:00:00", 1),
            new InstanceInfo("monthly2", "2008-05-01T00:00:00", "2008-07-01T00:00:00", 1),
            new InstanceInfo("monthly2", "2008-05-01T00:00:00", "2008-08-01T00:00:00", 2),

            new InstanceInfo("yearly0", "2008-05-01", "2009-04-30", 1),
            new InstanceInfo("yearly0", "2008-05-01", "2009-05-02", 2),
    };

    /**
     * This sequence of commands inserts and deletes some events.
     */
    private Command[] mNormalInsertDelete = {
            new Insert("normal0"),
            new Insert("normal1"),
            new Insert("normal2"),
            new QueryNumInstances("2008-05-01T00:00:00", "2008-05-31T00:01:00", 3),
            new Delete("normal1", 1, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
            new QueryNumEvents(2),
            new QueryNumInstances("2008-05-01T00:00:00", "2008-05-31T00:01:00", 2),
            new Delete("normal1", 0, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
            new Delete("normal2", 1, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
            new QueryNumEvents(1),
            new Delete("normal0", 1, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
            new QueryNumEvents(0),
    };

    /**
     * This sequence of commands inserts and deletes some all-day events.
     */
    private Command[] mAlldayInsertDelete = {
            new Insert("allday0"),
            new Insert("allday1"),
            new QueryNumEvents(2),
            new QueryNumInstances("2008-05-01T00:00:00", "2008-05-01T00:01:00", 0),
            new QueryNumInstances("2008-05-02T00:00:00", "2008-05-02T00:01:00", 2),
            new QueryNumInstances("2008-05-03T00:00:00", "2008-05-03T00:01:00", 1),
            new Delete("allday0", 1, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
            new QueryNumEvents(1),
            new QueryNumInstances("2008-05-02T00:00:00", "2008-05-02T00:01:00", 1),
            new QueryNumInstances("2008-05-03T00:00:00", "2008-05-03T00:01:00", 1),
            new Delete("allday1", 1, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
            new QueryNumEvents(0),
    };

    /**
     * This sequence of commands inserts and deletes some repeating events.
     */
    private Command[] mRecurringInsertDelete = {
            new Insert("daily0"),
            new Insert("daily1"),
            new QueryNumEvents(2),
            new QueryNumInstances("2008-05-01T00:00:00", "2008-05-02T00:01:00", 3),
            new QueryNumInstances("2008-05-01T01:01:00", "2008-05-02T00:01:00", 2),
            new QueryNumInstances("2008-05-01T00:00:00", "2008-05-04T00:01:00", 6),
            new Delete("daily1", 1, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
            new QueryNumEvents(1),
            new QueryNumInstances("2008-05-01T00:00:00", "2008-05-02T00:01:00", 2),
            new QueryNumInstances("2008-05-01T00:00:00", "2008-05-04T00:01:00", 4),
            new Delete("daily0", 1, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
            new QueryNumEvents(0),
    };

    /**
     * This sequence of commands creates a recurring event with a recurrence
     * exception that moves an event outside the expansion window.  It checks that the
     * recurrence exception does not occur in the Instances database table.
     * Bug 1642665
     */
    private Command[] mExceptionWithMovedRecurrence = {
            new Insert("daily0"),
            new VerifyAllInstances("2008-05-01T00:00:00", "2008-05-03T00:01:00",
                    new String[] {"2008-05-01T00:00:00", "2008-05-02T00:00:00",
                            "2008-05-03T00:00:00", }),
            new Insert("except2"),
            new VerifyAllInstances("2008-05-01T00:00:00", "2008-05-03T00:01:00",
                    new String[] {"2008-05-01T00:00:00", "2008-05-03T00:00:00"}),
    };

    /**
     * This sequence of commands deletes (cancels) one instance of a recurrence.
     */
    private Command[] mCancelInstance = {
            new Insert("weekly0"),
            new VerifyAllInstances("2008-05-01T00:00:00", "2008-05-22T00:01:00",
                    new String[] {"2008-05-06T13:00:00", "2008-05-13T13:00:00",
                            "2008-05-20T13:00:00", }),
            new Insert("cancel0"),
            new Update("cancel0", new KeyValue[] {
                    new KeyValue(CalendarContract.Events.STATUS,
                        Integer.toString(CalendarContract.Events.STATUS_CANCELED)),
            }),
            new VerifyAllInstances("2008-05-01T00:00:00", "2008-05-22T00:01:00",
                    new String[] {"2008-05-06T13:00:00",
                            "2008-05-20T13:00:00", }),
    };
    /**
     * This sequence of commands creates a recurring event with a recurrence
     * exception that moves an event from outside the expansion window into the
     * expansion window.
     */
    private Command[] mExceptionWithMovedRecurrence2 = {
            new Insert("weekly0"),
            new VerifyAllInstances("2008-12-01T00:00:00", "2008-12-22T00:01:00",
                    new String[] {"2008-12-02T13:00:00", "2008-12-09T13:00:00",
                            "2008-12-16T13:00:00", }),
            new Insert("except3"),
            new VerifyAllInstances("2008-12-01T00:00:00", "2008-12-22T00:01:00",
                    new String[] {"2008-12-02T13:00:00", "2008-12-09T13:00:00",
                            "2008-12-11T13:00:00", "2008-12-16T13:00:00", }),
    };
    /**
     * This sequence of commands creates a recurring event with a recurrence
     * exception and then changes the end time of the recurring event.  It then
     * checks that the recurrence exception does not occur in the Instances
     * database table.
     */
    private Command[]
            mExceptionWithTruncatedRecurrence = {
            new Insert("daily0"),
            // Verify 4 occurrences of the "daily0" repeating event
            new VerifyAllInstances("2008-05-01T00:00:00", "2008-05-04T00:01:00",
                    new String[] {"2008-05-01T00:00:00", "2008-05-02T00:00:00",
                            "2008-05-03T00:00:00", "2008-05-04T00:00:00"}),
            new Insert("except1"),
            new QueryNumEvents(2),

            // Verify that one of the 4 occurrences has its start time changed
            // so that it now matches the recurrence exception.
            new VerifyAllInstances("2008-05-01T00:00:00", "2008-05-04T00:01:00",
                    new String[] {"2008-05-01T00:00:00", "2008-05-02T00:00:00",
                            "2008-05-03T02:00:00", "2008-05-04T00:00:00"}),

            // Change the end time of "daily0" but it still includes the
            // recurrence exception.
            new Update("daily0", new KeyValue[] {
                    new KeyValue(Events.RRULE, "FREQ=DAILY;UNTIL=20080505T150000Z;WKST=SU"),
            }),

            // Verify that the recurrence exception is still there
            new VerifyAllInstances("2008-05-01T00:00:00", "2008-05-04T00:01:00",
                    new String[] {"2008-05-01T00:00:00", "2008-05-02T00:00:00",
                            "2008-05-03T02:00:00", "2008-05-04T00:00:00"}),
            // This time change the end time of "daily0" so that it excludes
            // the recurrence exception.
            new Update("daily0", new KeyValue[] {
                    new KeyValue(Events.RRULE, "FREQ=DAILY;UNTIL=20080502T150000Z;WKST=SU"),
            }),
            // The server will cancel the out-of-range exception.
            // It would be nice for the provider to handle this automatically,
            // but for now simulate the server-side cancel.
            new Update("except1", new KeyValue[] {
                new KeyValue(CalendarContract.Events.STATUS,
                        Integer.toString(CalendarContract.Events.STATUS_CANCELED)),
            }),
            // Verify that the recurrence exception does not appear.
            new VerifyAllInstances("2008-05-01T00:00:00", "2008-05-04T00:01:00",
                    new String[] {"2008-05-01T00:00:00", "2008-05-02T00:00:00"}),
    };

    /**
     * Bug 135848.  Ensure that a recurrence exception is displayed even if the recurrence
     * is not present.
     */
    private Command[] mExceptionWithNoRecurrence = {
            new Insert("except0"),
            new QueryNumEvents(1),
            new VerifyAllInstances("2008-05-01T00:00:00", "2008-05-03T00:01:00",
                    new String[] {"2008-05-01T02:00:00"}),
    };

    private EventInfo findEvent(String name) {
        int len = mEvents.length;
        for (int ii = 0; ii < len; ii++) {
            EventInfo event = mEvents[ii];
            if (name.equals(event.mTitle)) {
                return event;
            }
        }
        return null;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // This code here is the code that was originally in ProviderTestCase2
        mResolver = new MockContentResolver();

        final String filenamePrefix = "test.";
        RenamingDelegatingContext targetContextWrapper = new RenamingDelegatingContext(
                new MockContext2(), // The context that most methods are delegated to
                getContext(), // The context that file methods are delegated to
                filenamePrefix);
        mContext = new IsolatedContext(mResolver, targetContextWrapper);

        mProvider = new CalendarProvider2ForTesting();
        mProvider.attachInfo(mContext, null);

        mResolver.addProvider(CalendarContract.AUTHORITY, mProvider);
        mResolver.addProvider("subscribedfeeds", new MockProvider("subscribedfeeds"));
        mResolver.addProvider("sync", new MockProvider("sync"));

        mMetaData = getProvider().mMetaData;
        mForceDtend = false;

        CalendarDatabaseHelper helper = (CalendarDatabaseHelper) getProvider().getDatabaseHelper();
        mDb = helper.getWritableDatabase();
        wipeAndInitData(helper, mDb);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            mDb.close();
            mDb = null;
            getProvider().getDatabaseHelper().close();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        super.tearDown();
    }

    public void wipeAndInitData(SQLiteOpenHelper helper, SQLiteDatabase db)
            throws CalendarCache.CacheException {
        db.beginTransaction();

        // Clean tables
        db.delete("Calendars", null, null);
        db.delete("Events", null, null);
        db.delete("EventsRawTimes", null, null);
        db.delete("Instances", null, null);
        db.delete("CalendarMetaData", null, null);
        db.delete("CalendarCache", null, null);
        db.delete("Attendees", null, null);
        db.delete("Reminders", null, null);
        db.delete("CalendarAlerts", null, null);
        db.delete("ExtendedProperties", null, null);

        // Set CalendarCache data
        initCalendarCacheLocked(helper, db);

        // set CalendarMetaData data
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("localTimezone", "America/Los_Angeles");
        values.put("minInstance", 1207008000000L); // 1st April 2008
        values.put("maxInstance", now + ONE_WEEK_MILLIS);
        db.insert("CalendarMetaData", null, values);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void initCalendarCacheLocked(SQLiteOpenHelper helper, SQLiteDatabase db)
            throws CalendarCache.CacheException {
        CalendarCache cache = new CalendarCache(helper);

        String localTimezone = TimeZone.getDefault().getID();

        // Set initial values
        cache.writeDataLocked(db, CalendarCache.KEY_TIMEZONE_DATABASE_VERSION, "2010k");
        cache.writeDataLocked(db, CalendarCache.KEY_TIMEZONE_TYPE, CalendarCache.TIMEZONE_TYPE_AUTO);
        cache.writeDataLocked(db, CalendarCache.KEY_TIMEZONE_INSTANCES, localTimezone);
        cache.writeDataLocked(db, CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS, localTimezone);
    }

    protected CalendarProvider2ForTesting getProvider() {
        return mProvider;
    }

    /**
     * Dumps the contents of the given cursor to the log.  For debugging.
     * @param cursor the database cursor
     */
    private void dumpCursor(Cursor cursor) {
        cursor.moveToPosition(-1);
        String[] cols = cursor.getColumnNames();

        Log.i(TAG, "dumpCursor() count: " + cursor.getCount());
        int index = 0;
        while (cursor.moveToNext()) {
            Log.i(TAG, index + " {");
            for (int i = 0; i < cols.length; i++) {
                Log.i(TAG, "    " + cols[i] + '=' + cursor.getString(i));
            }
            Log.i(TAG, "}");
            index += 1;
        }
        cursor.moveToPosition(-1);
    }

    private int insertCal(String name, String timezone) {
        return insertCal(name, timezone, DEFAULT_ACCOUNT);
    }

    /**
     * Creates a new calendar, with the provided name, time zone, and account name.
     *
     * @return the new calendar's _ID value
     */
    private int insertCal(String name, String timezone, String account) {
        ContentValues m = new ContentValues();
        m.put(Calendars.NAME, name);
        m.put(Calendars.CALENDAR_DISPLAY_NAME, name);
        m.put(Calendars.CALENDAR_COLOR, 0xff123456);
        m.put(Calendars.CALENDAR_TIME_ZONE, timezone);
        m.put(Calendars.VISIBLE, 1);
        m.put(Calendars.CAL_SYNC1, CALENDAR_URL);
        m.put(Calendars.OWNER_ACCOUNT, account);
        m.put(Calendars.ACCOUNT_NAME,  account);
        m.put(Calendars.ACCOUNT_TYPE, DEFAULT_ACCOUNT_TYPE);
        m.put(Calendars.SYNC_EVENTS,  1);

        Uri url = mResolver.insert(
                addSyncQueryParams(mCalendarsUri, account, DEFAULT_ACCOUNT_TYPE), m);
        String id = url.getLastPathSegment();
        return Integer.parseInt(id);
    }

    private String obsToString(Object... objs) {
        StringBuilder bob = new StringBuilder();

        for (Object obj : objs) {
            bob.append(obj.toString());
            bob.append('#');
        }

        return bob.toString();
    }

    private Uri insertColor(long colorType, String colorKey, long color) {
        ContentValues m = new ContentValues();
        m.put(Colors.ACCOUNT_NAME, DEFAULT_ACCOUNT);
        m.put(Colors.ACCOUNT_TYPE, DEFAULT_ACCOUNT_TYPE);
        m.put(Colors.DATA, obsToString(colorType, colorKey, color));
        m.put(Colors.COLOR_TYPE, colorType);
        m.put(Colors.COLOR_KEY, colorKey);
        m.put(Colors.COLOR, color);

        Uri uri = CalendarContract.Colors.CONTENT_URI;

        return mResolver.insert(addSyncQueryParams(uri, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE), m);
    }

    private void updateAndCheckColor(long colorId, long colorType, String colorKey, long color) {

        Uri uri = CalendarContract.Colors.CONTENT_URI;

        final String where = Colors.ACCOUNT_NAME + "=? AND " + Colors.ACCOUNT_TYPE + "=? AND "
                + Colors.COLOR_TYPE + "=? AND " + Colors.COLOR_KEY + "=?";

        String[] selectionArgs = new String[] {
                DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE, Long.toString(colorType), colorKey
        };

        ContentValues cv = new ContentValues();
        cv.put(Colors.COLOR, color);
        cv.put(Colors.DATA, obsToString(colorType, colorKey, color));

        int count = mResolver.update(
                addSyncQueryParams(uri, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE), cv, where,
                selectionArgs);

        checkColor(colorId, colorType, colorKey, color);

        assertEquals(1, count);
    }

    /**
     * Constructs a URI from a base URI (e.g. "content://com.android.calendar/calendars"),
     * an account name, and an account type.
     */
    private Uri addSyncQueryParams(Uri uri, String account, String accountType) {
        return uri.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(Calendars.ACCOUNT_NAME, account)
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
    }

    private int deleteMatchingCalendars(String selection, String[] selectionArgs) {
        return mResolver.delete(mCalendarsUri, selection, selectionArgs);
    }

    private Uri insertEvent(int calId, EventInfo event) {
        return insertEvent(calId, event, null);
    }

    private Uri insertEvent(int calId, EventInfo event, ContentValues cv) {
        if (mWipe) {
            // Wipe instance table so it will be regenerated
            mMetaData.clearInstanceRange();
        }

        if (cv == null) {
            cv = eventInfoToContentValues(calId, event);
        }

        Uri url = mResolver.insert(mEventsUri, cv);

        // Create a fake _sync_id and add it to the event.  Update the database
        // directly so that we don't trigger any validation checks in the
        // CalendarProvider.
        long id = ContentUris.parseId(url);
        mDb.execSQL("UPDATE Events SET _sync_id=" + mGlobalSyncId + " WHERE _id=" + id);
        event.mSyncId = mGlobalSyncId;
        mGlobalSyncId += 1;

        return url;
    }

    private ContentValues eventInfoToContentValues(int calId, EventInfo event) {
        ContentValues m = new ContentValues();
        m.put(Events.CALENDAR_ID, calId);
        m.put(Events.TITLE, event.mTitle);
        m.put(Events.DTSTART, event.mDtstart);
        m.put(Events.ALL_DAY, event.mAllDay ? 1 : 0);

        if (event.mRrule == null || mForceDtend) {
            // This is a normal event
            m.put(Events.DTEND, event.mDtend);
            m.remove(Events.DURATION);
        }
        if (event.mRrule != null) {
            // This is a repeating event
            m.put(Events.RRULE, event.mRrule);
            m.put(Events.DURATION, event.mDuration);
            m.remove(Events.DTEND);
        }

        if (event.mDescription != null) {
            m.put(Events.DESCRIPTION, event.mDescription);
        }
        if (event.mTimezone != null) {
            m.put(Events.EVENT_TIMEZONE, event.mTimezone);
        }
        if (event.mCustomAppPackage != null) {
            m.put(Events.CUSTOM_APP_PACKAGE, event.mCustomAppPackage);
        }
        if (event.mCustomAppUri != null) {
            m.put(Events.CUSTOM_APP_URI, event.mCustomAppUri);
        }
        if (event.mUid2445 != null) {
            m.put(Events.UID_2445, event.mUid2445);
        }

        if (event.mOriginalTitle != null) {
            // This is a recurrence exception.
            EventInfo recur = findEvent(event.mOriginalTitle);
            assertNotNull(recur);
            String syncId = String.format("%d", recur.mSyncId);
            m.put(Events.ORIGINAL_SYNC_ID, syncId);
            m.put(Events.ORIGINAL_ALL_DAY, recur.mAllDay ? 1 : 0);
            m.put(Events.ORIGINAL_INSTANCE_TIME, event.mOriginalInstance);
        }
        return m;
    }

    /**
     * Deletes all the events that match the given title.
     * @param title the given title to match events on
     * @return the number of rows deleted
     */
    private int deleteMatchingEvents(String title, String account, String accountType) {
        Cursor cursor = mResolver.query(mEventsUri, new String[] { Events._ID },
                "title=?", new String[] { title }, null);
        int numRows = 0;
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            // Do delete as a sync adapter so event is really deleted, not just marked
            // as deleted.
            Uri uri = updatedUri(ContentUris.withAppendedId(Events.CONTENT_URI, id), true, account,
                    accountType);
            numRows += mResolver.delete(uri, null, null);
        }
        cursor.close();
        return numRows;
    }

    /**
     * Updates all the events that match the given title.
     * @param title the given title to match events on
     * @return the number of rows updated
     */
    private int updateMatchingEvents(String title, ContentValues values) {
        String[] projection = new String[] {
                Events._ID,
                Events.DTSTART,
                Events.DTEND,
                Events.DURATION,
                Events.ALL_DAY,
                Events.RRULE,
                Events.EVENT_TIMEZONE,
                Events.ORIGINAL_SYNC_ID,
        };
        Cursor cursor = mResolver.query(mEventsUri, projection,
                "title=?", new String[] { title }, null);
        int numRows = 0;
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);

            // If any of the following fields are being changed, then we need
            // to include all of them.
            if (values.containsKey(Events.DTSTART) || values.containsKey(Events.DTEND)
                    || values.containsKey(Events.DURATION) || values.containsKey(Events.ALL_DAY)
                    || values.containsKey(Events.RRULE)
                    || values.containsKey(Events.EVENT_TIMEZONE)
                    || values.containsKey(CalendarContract.Events.STATUS)) {
                long dtstart = cursor.getLong(1);
                long dtend = cursor.getLong(2);
                String duration = cursor.getString(3);
                boolean allDay = cursor.getInt(4) != 0;
                String rrule = cursor.getString(5);
                String timezone = cursor.getString(6);
                String originalEvent = cursor.getString(7);

                if (!values.containsKey(Events.DTSTART)) {
                    values.put(Events.DTSTART, dtstart);
                }
                // Don't add DTEND for repeating events
                if (!values.containsKey(Events.DTEND) && rrule == null) {
                    values.put(Events.DTEND, dtend);
                }
                if (!values.containsKey(Events.DURATION) && duration != null) {
                    values.put(Events.DURATION, duration);
                }
                if (!values.containsKey(Events.ALL_DAY)) {
                    values.put(Events.ALL_DAY, allDay ? 1 : 0);
                }
                if (!values.containsKey(Events.RRULE) && rrule != null) {
                    values.put(Events.RRULE, rrule);
                }
                if (!values.containsKey(Events.EVENT_TIMEZONE) && timezone != null) {
                    values.put(Events.EVENT_TIMEZONE, timezone);
                }
                if (!values.containsKey(Events.ORIGINAL_SYNC_ID) && originalEvent != null) {
                    values.put(Events.ORIGINAL_SYNC_ID, originalEvent);
                }
            }

            Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, id);
            numRows += mResolver.update(uri, values, null, null);
        }
        cursor.close();
        return numRows;
    }

    /**
     * Updates the status of all the events that match the given title.
     * @param title the given title to match events on
     * @return the number of rows updated
     */
    private int updateMatchingEventsStatusOnly(String title, ContentValues values) {
        String[] projection = new String[] {
                Events._ID,
        };
        if (values.size() != 1 && !values.containsKey(Events.STATUS)) {
            return 0;
        }
        Cursor cursor = mResolver.query(mEventsUri, projection,
                "title=?", new String[] { title }, null);
        int numRows = 0;
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);

            Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, id);
            numRows += mResolver.update(uri, values, null, null);
        }
        cursor.close();
        return numRows;
    }


    private void deleteAllEvents() {
        mDb.execSQL("DELETE FROM Events;");
        mMetaData.clearInstanceRange();
    }

    /**
     * Creates an updated URI that includes query parameters that identify the source as a
     * sync adapter.
     */
    static Uri asSyncAdapter(Uri uri, String account, String accountType) {
        return uri.buildUpon()
                .appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER,
                        "true")
                .appendQueryParameter(Calendars.ACCOUNT_NAME, account)
                .appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
    }
    
    public void testInsertUpdateDeleteColor() throws Exception {
        // Calendar Color
        long colorType = Colors.TYPE_CALENDAR;
        String colorKey = "123";
        long colorValue = 11;
        long colorId = insertAndCheckColor(colorType, colorKey, colorValue);

        try {
            insertAndCheckColor(colorType, colorKey, colorValue);
            fail("Expected to fail with duplicate insertion");
        } catch (IllegalArgumentException iae) {
            // good
        }

        // Test Update
        colorValue += 11;
        updateAndCheckColor(colorId, colorType, colorKey, colorValue);

        // Event Color
        colorType = Colors.TYPE_EVENT;
        colorValue += 11;
        colorId = insertAndCheckColor(colorType, colorKey, colorValue);
        try {
            insertAndCheckColor(colorType, colorKey, colorValue);
            fail("Expected to fail with duplicate insertion");
        } catch (IllegalArgumentException iae) {
            // good
        }

        // Create an event with the old color value.
        int calendarId0 = insertCal("Calendar0", DEFAULT_TIMEZONE);
        String title = "colorTest";
        ContentValues cv = this.eventInfoToContentValues(calendarId0, mEvents[0]);
        cv.put(Events.EVENT_COLOR_KEY, colorKey);
        cv.put(Events.TITLE, title);
        Uri uri = insertEvent(calendarId0, mEvents[0], cv);
        Cursor c = mResolver.query(uri, new String[] {Events.EVENT_COLOR},  null, null, null);
        try {
            // Confirm the color is set.
            c.moveToFirst();
            assertEquals(colorValue, c.getInt(0));
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // Test Update
        colorValue += 11;
        updateAndCheckColor(colorId, colorType, colorKey, colorValue);

        // Check if color was updated in event.
        c = mResolver.query(uri, new String[] {Events.EVENT_COLOR}, null, null, null);
        try {
            c.moveToFirst();
            assertEquals(colorValue, c.getInt(0));
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // Test Delete
        Uri colSyncUri = asSyncAdapter(Colors.CONTENT_URI, DEFAULT_ACCOUNT,
                DEFAULT_ACCOUNT_TYPE);
        try {
            // Delete should fail if color referenced by an event.
            mResolver.delete(colSyncUri, WHERE_COLOR_ACCOUNT_AND_INDEX,
                    new String[] {DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE, colorKey});
            fail("Should not allow deleting referenced color");
        } catch (UnsupportedOperationException e) {
            // Exception expected.
        }
        Cursor cursor = mResolver.query(Colors.CONTENT_URI, new String[] {Colors.COLOR_KEY},
                Colors.COLOR_KEY + "=? AND " + Colors.COLOR_TYPE + "=?",
                new String[] {colorKey, Long.toString(colorType)}, null);
        assertEquals(1, cursor.getCount());

        // Try again, by deleting the event, then the color.
        assertEquals(1, deleteMatchingEvents(title, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE));
        mResolver.delete(colSyncUri, WHERE_COLOR_ACCOUNT_AND_INDEX,
                new String[] {DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE, colorKey});
        cursor = mResolver.query(Colors.CONTENT_URI, new String[] {Colors.COLOR_KEY},
                Colors.COLOR_KEY + "=? AND " + Colors.COLOR_TYPE + "=?",
                new String[] {colorKey, Long.toString(colorType)}, null);
        assertEquals(0, cursor.getCount());
    }

    private void checkColor(long colorId, long colorType, String colorKey, long color) {
        String[] projection = new String[] {
                Colors.ACCOUNT_NAME, // 0
                Colors.ACCOUNT_TYPE, // 1
                Colors.COLOR_TYPE,   // 2
                Colors.COLOR_KEY,    // 3
                Colors.COLOR,        // 4
                Colors._ID,          // 5
                Colors.DATA,         // 6
        };
        Cursor cursor = mResolver.query(Colors.CONTENT_URI, projection, Colors.COLOR_KEY
                + "=? AND " + Colors.COLOR_TYPE + "=?", new String[] {
                colorKey, Long.toString(colorType)
        }, null /* sortOrder */);

        assertEquals(1, cursor.getCount());

        assertTrue(cursor.moveToFirst());
        assertEquals(DEFAULT_ACCOUNT, cursor.getString(0));
        assertEquals(DEFAULT_ACCOUNT_TYPE, cursor.getString(1));
        assertEquals(colorType, cursor.getLong(2));
        assertEquals(colorKey, cursor.getString(3));
        assertEquals(color, cursor.getLong(4));
        assertEquals(colorId, cursor.getLong(5));
        assertEquals(obsToString(colorType, colorKey, color), cursor.getString(6));
        cursor.close();
    }

    private long insertAndCheckColor(long colorType, String colorKey, long color) {
        Uri uri = insertColor(colorType, colorKey, color);
        long id = Long.parseLong(uri.getLastPathSegment());

        checkColor(id, colorType, colorKey, color);
        return id;
    }

    public void testInsertNormalEvents() throws Exception {
        final int calId = insertCal("Calendar0", DEFAULT_TIMEZONE);
        Cursor cursor = mResolver.query(mEventsUri, null, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // Keep track of the number of normal events
        int numOfInserts = 0;

        // "begin" is the earliest start time of all the normal events,
        // and "end" is the latest end time of all the normal events.
        long begin = 0, end = 0;

        int len = mEvents.length;
        Uri[] uris = new Uri[len];
        ContentValues[] cvs = new ContentValues[len];
        for (int ii = 0; ii < len; ii++) {
            EventInfo event = mEvents[ii];
            // Skip repeating events and recurrence exceptions
            if (event.mRrule != null || event.mOriginalTitle != null) {
                continue;
            }
            if (numOfInserts == 0) {
                begin = event.mDtstart;
                end = event.mDtend;
            } else {
                if (begin > event.mDtstart) {
                    begin = event.mDtstart;
                }
                if (end < event.mDtend) {
                    end = event.mDtend;
                }
            }

            cvs[ii] = eventInfoToContentValues(calId, event);
            uris[ii] = insertEvent(calId, event, cvs[ii]);
            numOfInserts += 1;
        }

        // Verify
        for (int i = 0; i < len; i++) {
            if (cvs[i] == null) continue;
            assertNotNull(uris[i]);
            cursor = mResolver.query(uris[i], null, null, null, null);
            assertEquals("Item " + i + " not found", 1, cursor.getCount());
            verifyContentValueAgainstCursor(cvs[i], cvs[i].keySet(), cursor);
            cursor.close();
        }

        // query all
        cursor = mResolver.query(mEventsUri, null, null, null, null);
        assertEquals(numOfInserts, cursor.getCount());
        cursor.close();

        // Check that the Instances table has one instance of each of the
        // normal events.
        cursor = queryInstances(begin, end);
        assertEquals(numOfInserts, cursor.getCount());
        cursor.close();
    }

    public void testInsertRepeatingEvents() throws Exception {
        Cursor cursor;
        Uri url = null;

        int calId = insertCal("Calendar0", "America/Los_Angeles");

        cursor = mResolver.query(mEventsUri, null, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // Keep track of the number of repeating events
        int numOfInserts = 0;

        int len = mEvents.length;
        Uri[] uris = new Uri[len];
        ContentValues[] cvs = new ContentValues[len];
        for (int ii = 0; ii < len; ii++) {
            EventInfo event = mEvents[ii];
            // Skip normal events
            if (event.mRrule == null) {
                continue;
            }
            cvs[ii] = eventInfoToContentValues(calId, event);
            uris[ii] = insertEvent(calId, event, cvs[ii]);
            numOfInserts += 1;
        }

        // Verify
        for (int i = 0; i < len; i++) {
            if (cvs[i] == null) continue;
            assertNotNull(uris[i]);
            cursor = mResolver.query(uris[i], null, null, null, null);
            assertEquals("Item " + i + " not found", 1, cursor.getCount());
            verifyContentValueAgainstCursor(cvs[i], cvs[i].keySet(), cursor);
            cursor.close();
        }

        // query all
        cursor = mResolver.query(mEventsUri, null, null, null, null);
        assertEquals(numOfInserts, cursor.getCount());
        cursor.close();
    }

    // Force a dtend value to be set and make sure instance expansion still works
    public void testInstanceRangeDtend() throws Exception {
        mForceDtend = true;
        testInstanceRange();
    }

    public void testInstanceRange() throws Exception {
        Cursor cursor;
        Uri url = null;

        int calId = insertCal("Calendar0", "America/Los_Angeles");

        cursor = mResolver.query(mEventsUri, null, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        int len = mInstanceRanges.length;
        for (int ii = 0; ii < len; ii++) {
            InstanceInfo instance = mInstanceRanges[ii];
            EventInfo event = instance.mEvent;
            url = insertEvent(calId, event);
            cursor = queryInstances(instance.mBegin, instance.mEnd);
            if (instance.mExpectedOccurrences != cursor.getCount()) {
                Log.e(TAG, "Test failed! Instance index: " + ii);
                Log.e(TAG, "title: " + event.mTitle + " desc: " + event.mDescription
                        + " [begin,end]: [" + instance.mBegin + " " + instance.mEnd + "]"
                        + " expected: " + instance.mExpectedOccurrences);
                dumpCursor(cursor);
            }
            assertEquals(instance.mExpectedOccurrences, cursor.getCount());
            cursor.close();
            // Delete as sync_adapter so event is really deleted.
            int rows = mResolver.delete(
                    updatedUri(url, true, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
                    null /* selection */, null /* selection args */);
            assertEquals(1, rows);
        }
    }

    public static <T> void assertArrayEquals(T[] expected, T[] actual) {
        if (!Arrays.equals(expected, actual)) {
            fail("expected:<" + Arrays.toString(expected) +
                "> but was:<" + Arrays.toString(actual) + ">");
        }
    }

    @SmallTest @Smoke
    public void testEscapeSearchToken() {
        String token = "test";
        String expected = "test";
        assertEquals(expected, mProvider.escapeSearchToken(token));

        token = "%";
        expected = "#%";
        assertEquals(expected, mProvider.escapeSearchToken(token));

        token = "_";
        expected = "#_";
        assertEquals(expected, mProvider.escapeSearchToken(token));

        token = "#";
        expected = "##";
        assertEquals(expected, mProvider.escapeSearchToken(token));

        token = "##";
        expected = "####";
        assertEquals(expected, mProvider.escapeSearchToken(token));

        token = "%_#";
        expected = "#%#_##";
        assertEquals(expected, mProvider.escapeSearchToken(token));

        token = "blah%blah";
        expected = "blah#%blah";
        assertEquals(expected, mProvider.escapeSearchToken(token));
    }

    @SmallTest @Smoke
    public void testTokenizeSearchQuery() {
        String query = "";
        String[] expectedTokens = new String[] {};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));

        query = "a";
        expectedTokens = new String[] {"a"};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));

        query = "word";
        expectedTokens = new String[] {"word"};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));

        query = "two words";
        expectedTokens = new String[] {"two", "words"};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));

        query = "test, punctuation.";
        expectedTokens = new String[] {"test", "punctuation"};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));

        query = "\"test phrase\"";
        expectedTokens = new String[] {"test phrase"};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));

        query = "unquoted \"this is quoted\"";
        expectedTokens = new String[] {"unquoted", "this is quoted"};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));

        query = " \"this is quoted\"  unquoted ";
        expectedTokens = new String[] {"this is quoted", "unquoted"};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));

        query = "escap%e m_e";
        expectedTokens = new String[] {"escap#%e", "m#_e"};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));

        query = "'a bunch' of malformed\" things";
        expectedTokens = new String[] {"a", "bunch", "of", "malformed", "things"};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));

        query = "''''''....,.''trim punctuation";
        expectedTokens = new String[] {"trim", "punctuation"};
        assertArrayEquals(expectedTokens, mProvider.tokenizeSearchQuery(query));
    }

    @SmallTest @Smoke
    public void testConstructSearchWhere() {
        String[] tokens = new String[] {"red"};
        String expected = "(title LIKE ? ESCAPE \"#\" OR "
            + "description LIKE ? ESCAPE \"#\" OR "
            + "eventLocation LIKE ? ESCAPE \"#\" OR "
            + "group_concat(attendeeEmail) LIKE ? ESCAPE \"#\" OR "
            + "group_concat(attendeeName) LIKE ? ESCAPE \"#\" )";
        assertEquals(expected, mProvider.constructSearchWhere(tokens));

        tokens = new String[] {};
        expected = "";
        assertEquals(expected, mProvider.constructSearchWhere(tokens));

        tokens = new String[] {"red", "green"};
        expected = "(title LIKE ? ESCAPE \"#\" OR "
                + "description LIKE ? ESCAPE \"#\" OR "
                + "eventLocation LIKE ? ESCAPE \"#\" OR "
                + "group_concat(attendeeEmail) LIKE ? ESCAPE \"#\" OR "
                + "group_concat(attendeeName) LIKE ? ESCAPE \"#\" ) AND "
                + "(title LIKE ? ESCAPE \"#\" OR "
                + "description LIKE ? ESCAPE \"#\" OR "
                + "eventLocation LIKE ? ESCAPE \"#\" OR "
                + "group_concat(attendeeEmail) LIKE ? ESCAPE \"#\" OR "
                + "group_concat(attendeeName) LIKE ? ESCAPE \"#\" )";
        assertEquals(expected, mProvider.constructSearchWhere(tokens));

        tokens = new String[] {"red blue", "green"};
        expected = "(title LIKE ? ESCAPE \"#\" OR "
            + "description LIKE ? ESCAPE \"#\" OR "
            + "eventLocation LIKE ? ESCAPE \"#\" OR "
            + "group_concat(attendeeEmail) LIKE ? ESCAPE \"#\" OR "
            + "group_concat(attendeeName) LIKE ? ESCAPE \"#\" ) AND "
            + "(title LIKE ? ESCAPE \"#\" OR "
            + "description LIKE ? ESCAPE \"#\" OR "
            + "eventLocation LIKE ? ESCAPE \"#\" OR "
            + "group_concat(attendeeEmail) LIKE ? ESCAPE \"#\" OR "
            + "group_concat(attendeeName) LIKE ? ESCAPE \"#\" )";
        assertEquals(expected, mProvider.constructSearchWhere(tokens));
    }

    @SmallTest @Smoke
    public void testConstructSearchArgs() {
        long rangeBegin = 0;
        long rangeEnd = 10;

        String[] tokens = new String[] {"red"};
        String[] expected = new String[] {"10", "0", "%red%", "%red%",
                "%red%", "%red%", "%red%" };
        assertArrayEquals(expected, mProvider.constructSearchArgs(tokens,
                rangeBegin, rangeEnd));

        tokens = new String[] {"red", "blue"};
        expected = new String[] { "10", "0", "%red%", "%red%", "%red%",
                "%red%", "%red%", "%blue%", "%blue%",
                "%blue%", "%blue%","%blue%"};
        assertArrayEquals(expected, mProvider.constructSearchArgs(tokens,
                rangeBegin, rangeEnd));

        tokens = new String[] {};
        expected = new String[] {"10", "0" };
        assertArrayEquals(expected, mProvider.constructSearchArgs(tokens,
                rangeBegin, rangeEnd));
    }

    public void testInstanceSearchQuery() throws Exception {
        final String[] PROJECTION = new String[] {
                Instances.TITLE,                 // 0
                Instances.EVENT_LOCATION,        // 1
                Instances.ALL_DAY,               // 2
                Instances.CALENDAR_COLOR,        // 3
                Instances.EVENT_TIMEZONE,        // 4
                Instances.EVENT_ID,              // 5
                Instances.BEGIN,                 // 6
                Instances.END,                   // 7
                Instances._ID,                   // 8
                Instances.START_DAY,             // 9
                Instances.END_DAY,               // 10
                Instances.START_MINUTE,          // 11
                Instances.END_MINUTE,            // 12
                Instances.HAS_ALARM,             // 13
                Instances.RRULE,                 // 14
                Instances.RDATE,                 // 15
                Instances.SELF_ATTENDEE_STATUS,  // 16
                Events.ORGANIZER,                // 17
                Events.GUESTS_CAN_MODIFY,        // 18
        };

        String orderBy = CalendarProvider2.SORT_CALENDAR_VIEW;
        String where = Instances.SELF_ATTENDEE_STATUS + "!=" +
                CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED;

        int calId = insertCal("Calendar0", DEFAULT_TIMEZONE);
        final String START = "2008-05-01T00:00:00";
        final String END = "2008-05-01T20:00:00";

        EventInfo event1 = new EventInfo("search orange",
                START,
                END,
                false /* allDay */,
                DEFAULT_TIMEZONE);
        event1.mDescription = "this is description1";

        EventInfo event2 = new EventInfo("search purple",
                START,
                END,
                false /* allDay */,
                DEFAULT_TIMEZONE);
        event2.mDescription = "lasers, out of nowhere";

        EventInfo event3 = new EventInfo("",
                START,
                END,
                false /* allDay */,
                DEFAULT_TIMEZONE);
        event3.mDescription = "kapow";

        EventInfo[] events = { event1, event2, event3 };

        insertEvent(calId, events[0]);
        insertEvent(calId, events[1]);
        insertEvent(calId, events[2]);

        Time time = new Time(DEFAULT_TIMEZONE);
        time.parse3339(START);
        long startMs = time.toMillis(true /* ignoreDst */);
        // Query starting from way in the past to one hour into the event.
        // Query is more than 2 months so the range won't get extended by the provider.
        Cursor cursor = null;

        try {
            cursor = queryInstances(mResolver, PROJECTION,
                    startMs - DateUtils.YEAR_IN_MILLIS,
                    startMs + DateUtils.HOUR_IN_MILLIS,
                    "search", where, null, orderBy);
            assertEquals(2, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        try {
            cursor = queryInstances(mResolver, PROJECTION,
                    startMs - DateUtils.YEAR_IN_MILLIS,
                    startMs + DateUtils.HOUR_IN_MILLIS,
                    "purple", where, null, orderBy);
            assertEquals(1, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        try {
            cursor = queryInstances(mResolver, PROJECTION,
                    startMs - DateUtils.YEAR_IN_MILLIS,
                    startMs + DateUtils.HOUR_IN_MILLIS,
                    "puurple", where, null, orderBy);
            assertEquals(0, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        try {
            cursor = queryInstances(mResolver, PROJECTION,
                    startMs - DateUtils.YEAR_IN_MILLIS,
                    startMs + DateUtils.HOUR_IN_MILLIS,
                    "purple lasers", where, null, orderBy);
            assertEquals(1, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        try {
            cursor = queryInstances(mResolver, PROJECTION,
                    startMs - DateUtils.YEAR_IN_MILLIS,
                    startMs + DateUtils.HOUR_IN_MILLIS,
                    "lasers kapow", where, null, orderBy);
            assertEquals(0, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        try {
            cursor = queryInstances(mResolver, PROJECTION,
                    startMs - DateUtils.YEAR_IN_MILLIS,
                    startMs + DateUtils.HOUR_IN_MILLIS,
                    "\"search purple\"", where, null, orderBy);
            assertEquals(1, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        try {
            cursor = queryInstances(mResolver, PROJECTION,
                    startMs - DateUtils.YEAR_IN_MILLIS,
                    startMs + DateUtils.HOUR_IN_MILLIS,
                    "\"purple search\"", where, null, orderBy);
            assertEquals(0, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        try {
            cursor = queryInstances(mResolver, PROJECTION,
                    startMs - DateUtils.YEAR_IN_MILLIS,
                    startMs + DateUtils.HOUR_IN_MILLIS,
                    "%", where, null, orderBy);
            assertEquals(0, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testDeleteCalendar() throws Exception {
        int calendarId0 = insertCal("Calendar0", DEFAULT_TIMEZONE);
        int calendarId1 = insertCal("Calendar1", DEFAULT_TIMEZONE, "user2@google.com");
        insertEvent(calendarId0, mEvents[0]);
        insertEvent(calendarId1, mEvents[1]);
        // Should have 2 calendars and 2 events
        testQueryCount(CalendarContract.Calendars.CONTENT_URI, null /* where */, 2);
        testQueryCount(CalendarContract.Events.CONTENT_URI, null /* where */, 2);

        int deletes = mResolver.delete(CalendarContract.Calendars.CONTENT_URI,
                "ownerAccount='user2@google.com'", null /* selectionArgs */);

        assertEquals(1, deletes);
        // Should have 1 calendar and 1 event
        testQueryCount(CalendarContract.Calendars.CONTENT_URI, null /* where */, 1);
        testQueryCount(CalendarContract.Events.CONTENT_URI, null /* where */, 1);

        deletes = mResolver.delete(Uri.withAppendedPath(CalendarContract.Calendars.CONTENT_URI,
                String.valueOf(calendarId0)),
                null /* selection*/ , null /* selectionArgs */);

        assertEquals(1, deletes);
        // Should have 0 calendars and 0 events
        testQueryCount(CalendarContract.Calendars.CONTENT_URI, null /* where */, 0);
        testQueryCount(CalendarContract.Events.CONTENT_URI, null /* where */, 0);

        deletes = mResolver.delete(CalendarContract.Calendars.CONTENT_URI,
                "ownerAccount=?", new String[] {"user2@google.com"} /* selectionArgs */);

        assertEquals(0, deletes);
    }
    
    public void testCalendarAlerts() throws Exception {
        // This projection is from AlertActivity; want to make sure it works.
        String[] projection = new String[] {
                CalendarContract.CalendarAlerts._ID,              // 0
                CalendarContract.CalendarAlerts.TITLE,            // 1
                CalendarContract.CalendarAlerts.EVENT_LOCATION,   // 2
                CalendarContract.CalendarAlerts.ALL_DAY,          // 3
                CalendarContract.CalendarAlerts.BEGIN,            // 4
                CalendarContract.CalendarAlerts.END,              // 5
                CalendarContract.CalendarAlerts.EVENT_ID,         // 6
                CalendarContract.CalendarAlerts.CALENDAR_COLOR,   // 7
                CalendarContract.CalendarAlerts.RRULE,            // 8
                CalendarContract.CalendarAlerts.HAS_ALARM,        // 9
                CalendarContract.CalendarAlerts.STATE,            // 10
                CalendarContract.CalendarAlerts.ALARM_TIME,       // 11
        };
        testInsertNormalEvents(); // To initialize

        Uri alertUri = CalendarContract.CalendarAlerts.insert(mResolver, 1 /* eventId */,
                2 /* begin */, 3 /* end */, 4 /* alarmTime */, 5 /* minutes */);
        CalendarContract.CalendarAlerts.insert(mResolver, 1 /* eventId */,
                2 /* begin */, 7 /* end */, 8 /* alarmTime */, 9 /* minutes */);

        // Regular query
        Cursor cursor = mResolver.query(CalendarContract.CalendarAlerts.CONTENT_URI, projection,
                null /* selection */, null /* selectionArgs */, null /* sortOrder */);

        assertEquals(2, cursor.getCount());
        cursor.close();

        // Instance query
        cursor = mResolver.query(alertUri, projection,
                null /* selection */, null /* selectionArgs */, null /* sortOrder */);

        assertEquals(1, cursor.getCount());
        cursor.close();

        // Grouped by event query
        cursor = mResolver.query(CalendarContract.CalendarAlerts.CONTENT_URI_BY_INSTANCE,
                projection, null /* selection */, null /* selectionArgs */, null /* sortOrder */);

        assertEquals(1, cursor.getCount());
        cursor.close();
    }

    void checkEvents(int count, SQLiteDatabase db) {
        Cursor cursor = db.query("Events", null, null, null, null, null, null);
        try {
            assertEquals(count, cursor.getCount());
        } finally {
            cursor.close();
        }
    }

    void checkEvents(int count, SQLiteDatabase db, String calendar) {
        Cursor cursor = db.query("Events", null, Events.CALENDAR_ID + "=?", new String[] {calendar},
                null, null, null);
        try {
            assertEquals(count, cursor.getCount());
        } finally {
            cursor.close();
        }
    }


//    TODO Reenable this when we are ready to work on this
//
//    public void testToShowInsertIsSlowForRecurringEvents() throws Exception {
//        mCalendarId = insertCal("CalendarTestToShowInsertIsSlowForRecurringEvents", DEFAULT_TIMEZONE);
//        String calendarIdString = Integer.toString(mCalendarId);
//        long testStart = System.currentTimeMillis();
//
//        final int testTrials = 100;
//
//        for (int i = 0; i < testTrials; i++) {
//            checkEvents(i, mDb, calendarIdString);
//            long insertStartTime = System.currentTimeMillis();
//            Uri eventUri = insertEvent(mCalendarId, findEvent("daily0"));
//            Log.e(TAG, i + ") insertion time " + (System.currentTimeMillis() - insertStartTime));
//        }
//        Log.e(TAG, " Avg insertion time = " + (System.currentTimeMillis() - testStart)/testTrials);
//    }

    /**
     * Test attendee processing
     * @throws Exception
     */
    public void testAttendees() throws Exception {
        mCalendarId = insertCal("CalendarTestAttendees", DEFAULT_TIMEZONE);
        String calendarIdString = Integer.toString(mCalendarId);
        checkEvents(0, mDb, calendarIdString);
        Uri eventUri = insertEvent(mCalendarId, findEvent("normal0"));
        checkEvents(1, mDb, calendarIdString);
        long eventId = ContentUris.parseId(eventUri);

        ContentValues attendee = new ContentValues();
        attendee.put(CalendarContract.Attendees.ATTENDEE_NAME, "Joe");
        attendee.put(CalendarContract.Attendees.ATTENDEE_EMAIL, DEFAULT_ACCOUNT);
        attendee.put(CalendarContract.Attendees.ATTENDEE_TYPE,
                CalendarContract.Attendees.TYPE_REQUIRED);
        attendee.put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP,
                CalendarContract.Attendees.RELATIONSHIP_ORGANIZER);
        attendee.put(CalendarContract.Attendees.EVENT_ID, eventId);
        attendee.put(CalendarContract.Attendees.ATTENDEE_IDENTITY, "ID1");
        attendee.put(CalendarContract.Attendees.ATTENDEE_ID_NAMESPACE, "IDNS1");
        Uri attendeesUri = mResolver.insert(CalendarContract.Attendees.CONTENT_URI, attendee);

        Cursor cursor = mResolver.query(CalendarContract.Attendees.CONTENT_URI, null,
                "event_id=" + eventId, null, null);
        assertEquals("Created event is missing - cannot find EventUri = " + eventUri, 1,
                cursor.getCount());
        Set<String> attendeeColumns = attendee.keySet();
        verifyContentValueAgainstCursor(attendee, attendeeColumns, cursor);
        cursor.close();

        cursor = mResolver.query(eventUri, null, null, null, null);
        // TODO figure out why this test fails. App works fine for this case.
        assertEquals("Created event is missing - cannot find EventUri = " + eventUri, 1,
                cursor.getCount());
        int selfColumn = cursor.getColumnIndex(CalendarContract.Events.SELF_ATTENDEE_STATUS);
        cursor.moveToNext();
        long selfAttendeeStatus = cursor.getInt(selfColumn);
        assertEquals(CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED, selfAttendeeStatus);
        cursor.close();

        // Update status to declined and change identity
        ContentValues attendeeUpdate = new ContentValues();
        attendeeUpdate.put(CalendarContract.Attendees.ATTENDEE_IDENTITY, "ID2");
        attendee.put(CalendarContract.Attendees.ATTENDEE_IDENTITY, "ID2");
        attendeeUpdate.put(CalendarContract.Attendees.ATTENDEE_STATUS,
                CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED);
        attendee.put(CalendarContract.Attendees.ATTENDEE_STATUS,
                CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED);
        mResolver.update(attendeesUri, attendeeUpdate, null, null);

        // Check in attendees table
        cursor = mResolver.query(attendeesUri, null, null, null, null);
        cursor.moveToNext();
        verifyContentValueAgainstCursor(attendee, attendeeColumns, cursor);
        cursor.close();

        // Test that the self status in events table is updated
        cursor = mResolver.query(eventUri, null, null, null, null);
        cursor.moveToNext();
        selfAttendeeStatus = cursor.getInt(selfColumn);
        assertEquals(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED, selfAttendeeStatus);
        cursor.close();

        // Add another attendee
        attendee.put(CalendarContract.Attendees.ATTENDEE_NAME, "Dude");
        attendee.put(CalendarContract.Attendees.ATTENDEE_EMAIL, "dude@dude.com");
        attendee.put(CalendarContract.Attendees.ATTENDEE_STATUS,
                CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED);
        mResolver.insert(CalendarContract.Attendees.CONTENT_URI, attendee);

        cursor = mResolver.query(CalendarContract.Attendees.CONTENT_URI, null,
                "event_id=" + eventId, null, null);
        assertEquals(2, cursor.getCount());
        cursor.close();

        cursor = mResolver.query(eventUri, null, null, null, null);
        cursor.moveToNext();
        selfAttendeeStatus = cursor.getInt(selfColumn);
        assertEquals(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED, selfAttendeeStatus);
        cursor.close();
    }

    private void verifyContentValueAgainstCursor(ContentValues cv,
            Set<String> keys, Cursor cursor) {
        cursor.moveToFirst();
        for (String key : keys) {
            assertEquals(cv.get(key).toString(),
                    cursor.getString(cursor.getColumnIndex(key)));
        }
        cursor.close();
    }

    /**
     * Test the event's dirty status and clear it.
     *
     * @param eventId event to fetch.
     * @param wanted the wanted dirty status
     */
    private void testAndClearDirty(long eventId, int wanted) {
        Cursor cursor = mResolver.query(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
                null, null, null, null);
        try {
            assertEquals("Event count", 1, cursor.getCount());
            cursor.moveToNext();
            int dirty = cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.DIRTY));
            assertEquals("dirty flag", wanted, dirty);
            if (dirty == 1) {
                // Have to access database directly since provider will set dirty again.
                mDb.execSQL("UPDATE Events SET " + Events.DIRTY + "=0 WHERE _id=" + eventId);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Test the count of results from a query.
     * @param uri The URI to query
     * @param where The where string or null.
     * @param wanted The number of results wanted.  An assertion is thrown if it doesn't match.
     */
    private void testQueryCount(Uri uri, String where, int wanted) {
        Cursor cursor = mResolver.query(uri, null/* projection */, where, null /* selectionArgs */,
                null /* sortOrder */);
        try {
            assertEquals("query results", wanted, cursor.getCount());
        } finally {
            cursor.close();
        }
    }

    /**
     * Test dirty flag processing.
     * @throws Exception
     */
    public void testDirty() throws Exception {
        internalTestDirty(false);
    }

    /**
     * Test dirty flag processing for updates from a sync adapter.
     * @throws Exception
     */
    public void testDirtyWithSyncAdapter() throws Exception {
        internalTestDirty(true);
    }

    /**
     * Adds CALLER_IS_SYNCADAPTER to URI if this is a sync adapter operation.  Otherwise,
     * returns the original URI.
     */
    private Uri updatedUri(Uri uri, boolean syncAdapter, String account, String accountType) {
        if (syncAdapter) {
            return addSyncQueryParams(uri, account, accountType);
        } else {
            return uri;
        }
    }

    /**
     * Test dirty flag processing either for syncAdapter operations or client operations.
     * The main difference is syncAdapter operations don't set the dirty bit.
     */
    private void internalTestDirty(boolean syncAdapter) throws Exception {
        mCalendarId = insertCal("Calendar0", DEFAULT_TIMEZONE);

        long now = System.currentTimeMillis();
        long begin = (now / 1000) * 1000;
        long end = begin + ONE_HOUR_MILLIS;
        Time time = new Time(DEFAULT_TIMEZONE);
        time.set(begin);
        String startDate = time.format3339(false);
        time.set(end);
        String endDate = time.format3339(false);

        EventInfo eventInfo = new EventInfo("current", startDate, endDate, false);
        Uri eventUri = insertEvent(mCalendarId, eventInfo);

        long eventId = ContentUris.parseId(eventUri);
        testAndClearDirty(eventId, 1);

        ContentValues attendee = new ContentValues();
        attendee.put(CalendarContract.Attendees.ATTENDEE_NAME, "Joe");
        attendee.put(CalendarContract.Attendees.ATTENDEE_EMAIL, DEFAULT_ACCOUNT);
        attendee.put(CalendarContract.Attendees.ATTENDEE_TYPE,
                CalendarContract.Attendees.TYPE_REQUIRED);
        attendee.put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP,
                CalendarContract.Attendees.RELATIONSHIP_ORGANIZER);
        attendee.put(CalendarContract.Attendees.EVENT_ID, eventId);

        Uri attendeeUri = mResolver.insert(
                updatedUri(CalendarContract.Attendees.CONTENT_URI, syncAdapter, DEFAULT_ACCOUNT,
                        DEFAULT_ACCOUNT_TYPE),
                attendee);
        testAndClearDirty(eventId, syncAdapter ? 0 : 1);
        testQueryCount(CalendarContract.Attendees.CONTENT_URI, "event_id=" + eventId, 1);

        ContentValues reminder = new ContentValues();
        reminder.put(CalendarContract.Reminders.MINUTES, 30);
        reminder.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_EMAIL);
        reminder.put(CalendarContract.Attendees.EVENT_ID, eventId);

        Uri reminderUri = mResolver.insert(
                updatedUri(CalendarContract.Reminders.CONTENT_URI, syncAdapter, DEFAULT_ACCOUNT,
                        DEFAULT_ACCOUNT_TYPE), reminder);
        testAndClearDirty(eventId, syncAdapter ? 0 : 1);
        testQueryCount(CalendarContract.Reminders.CONTENT_URI, "event_id=" + eventId, 1);

        long alarmTime = begin + 5 * ONE_MINUTE_MILLIS;

        ContentValues alert = new ContentValues();
        alert.put(CalendarContract.CalendarAlerts.BEGIN, begin);
        alert.put(CalendarContract.CalendarAlerts.END, end);
        alert.put(CalendarContract.CalendarAlerts.ALARM_TIME, alarmTime);
        alert.put(CalendarContract.CalendarAlerts.CREATION_TIME, now);
        alert.put(CalendarContract.CalendarAlerts.RECEIVED_TIME, now);
        alert.put(CalendarContract.CalendarAlerts.NOTIFY_TIME, now);
        alert.put(CalendarContract.CalendarAlerts.STATE,
                CalendarContract.CalendarAlerts.STATE_SCHEDULED);
        alert.put(CalendarContract.CalendarAlerts.MINUTES, 30);
        alert.put(CalendarContract.CalendarAlerts.EVENT_ID, eventId);

        Uri alertUri = mResolver.insert(
                updatedUri(CalendarContract.CalendarAlerts.CONTENT_URI, syncAdapter,
                        DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE), alert);
        // Alerts don't dirty the event
        testAndClearDirty(eventId, 0);
        testQueryCount(CalendarContract.CalendarAlerts.CONTENT_URI, "event_id=" + eventId, 1);

        ContentValues extended = new ContentValues();
        extended.put(CalendarContract.ExtendedProperties.NAME, "foo");
        extended.put(CalendarContract.ExtendedProperties.VALUE, "bar");
        extended.put(CalendarContract.ExtendedProperties.EVENT_ID, eventId);

        Uri extendedUri = null;
        if (syncAdapter) {
            // Only the sync adapter is allowed to modify ExtendedProperties.
            extendedUri = mResolver.insert(
                    updatedUri(CalendarContract.ExtendedProperties.CONTENT_URI, syncAdapter,
                            DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE), extended);
            testAndClearDirty(eventId, syncAdapter ? 0 : 1);
            testQueryCount(CalendarContract.ExtendedProperties.CONTENT_URI,
                    "event_id=" + eventId, 2);
        } else {
            // Confirm that inserting as app fails.
            try {
                extendedUri = mResolver.insert(
                        updatedUri(CalendarContract.ExtendedProperties.CONTENT_URI, syncAdapter,
                                DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE), extended);
                fail("Only sync adapter should be allowed to insert into ExtendedProperties");
            } catch (IllegalArgumentException iae) {}
        }

        // Now test updates

        attendee = new ContentValues();
        attendee.put(CalendarContract.Attendees.ATTENDEE_NAME, "Sam");

        assertEquals("update", 1, mResolver.update(
                updatedUri(attendeeUri, syncAdapter, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
                attendee,
                null /* where */, null /* selectionArgs */));
        testAndClearDirty(eventId, syncAdapter ? 0 : 1);

        testQueryCount(CalendarContract.Attendees.CONTENT_URI, "event_id=" + eventId, 1);

        alert = new ContentValues();
        alert.put(CalendarContract.CalendarAlerts.STATE,
                CalendarContract.CalendarAlerts.STATE_DISMISSED);

        assertEquals("update", 1, mResolver.update(
                updatedUri(alertUri, syncAdapter, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE), alert,
                null /* where */, null /* selectionArgs */));
        // Alerts don't dirty the event
        testAndClearDirty(eventId, 0);
        testQueryCount(CalendarContract.CalendarAlerts.CONTENT_URI, "event_id=" + eventId, 1);

        extended = new ContentValues();
        extended.put(CalendarContract.ExtendedProperties.VALUE, "baz");

        if (syncAdapter) {
            assertEquals("update", 1, mResolver.update(
                    updatedUri(extendedUri, syncAdapter, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
                    extended,
                    null /* where */, null /* selectionArgs */));
            testAndClearDirty(eventId, syncAdapter ? 0 : 1);
            testQueryCount(CalendarContract.ExtendedProperties.CONTENT_URI,
                    "event_id=" + eventId, 2);
        }

        // Now test deletes

        assertEquals("delete", 1, mResolver.delete(
                updatedUri(attendeeUri, syncAdapter, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
                null, null /* selectionArgs */));
        testAndClearDirty(eventId, syncAdapter ? 0 : 1);
        testQueryCount(CalendarContract.Attendees.CONTENT_URI, "event_id=" + eventId, 0);

        assertEquals("delete", 1, mResolver.delete(
                updatedUri(reminderUri, syncAdapter, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
                null /* where */, null /* selectionArgs */));

        testAndClearDirty(eventId, syncAdapter ? 0 : 1);
        testQueryCount(CalendarContract.Reminders.CONTENT_URI, "event_id=" + eventId, 0);

        assertEquals("delete", 1, mResolver.delete(
                updatedUri(alertUri, syncAdapter, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
                null /* where */, null /* selectionArgs */));

        // Alerts don't dirty the event
        testAndClearDirty(eventId, 0);
        testQueryCount(CalendarContract.CalendarAlerts.CONTENT_URI, "event_id=" + eventId, 0);

        if (syncAdapter) {
            assertEquals("delete", 1, mResolver.delete(
                    updatedUri(extendedUri, syncAdapter, DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE),
                    null /* where */, null /* selectionArgs */));

            testAndClearDirty(eventId, syncAdapter ? 0 : 1);
            testQueryCount(CalendarContract.ExtendedProperties.CONTENT_URI, "event_id=" + eventId, 1);
        }
    }

    /**
     * Test calendar deletion
     * @throws Exception
     */
    public void testCalendarDeletion() throws Exception {
        mCalendarId = insertCal("Calendar0", DEFAULT_TIMEZONE);
        Uri eventUri = insertEvent(mCalendarId, findEvent("daily0"));
        long eventId = ContentUris.parseId(eventUri);
        testAndClearDirty(eventId, 1);
        Uri eventUri1 = insertEvent(mCalendarId, findEvent("daily1"));
        long eventId1 = ContentUris.parseId(eventUri);
        assertEquals("delete", 1, mResolver.delete(eventUri1, null, null));
        // Calendar has one event and one deleted event
        testQueryCount(CalendarContract.Events.CONTENT_URI, null, 2);

        assertEquals("delete", 1, mResolver.delete(CalendarContract.Calendars.CONTENT_URI,
                "_id=" + mCalendarId, null));
        // Calendar should be deleted
        testQueryCount(CalendarContract.Calendars.CONTENT_URI, null, 0);
        // Event should be gone
        testQueryCount(CalendarContract.Events.CONTENT_URI, null, 0);
    }

    /**
     * Test multiple account support.
     */
    public void testMultipleAccounts() throws Exception {
        mCalendarId = insertCal("Calendar0", DEFAULT_TIMEZONE);
        int calendarId1 = insertCal("Calendar1", DEFAULT_TIMEZONE, "user2@google.com");
        Uri eventUri0 = insertEvent(mCalendarId, findEvent("daily0"));
        Uri eventUri1 = insertEvent(calendarId1, findEvent("daily1"));

        testQueryCount(CalendarContract.Events.CONTENT_URI, null, 2);
        Uri eventsWithAccount = CalendarContract.Events.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.EventsEntity.ACCOUNT_NAME, DEFAULT_ACCOUNT)
                .appendQueryParameter(CalendarContract.EventsEntity.ACCOUNT_TYPE,
                        DEFAULT_ACCOUNT_TYPE)
                .build();
        // Only one event for that account
        testQueryCount(eventsWithAccount, null, 1);

        // Test deletion with account and selection

        long eventId = ContentUris.parseId(eventUri1);
        // Wrong account, should not be deleted
        assertEquals("delete", 0, mResolver.delete(
                updatedUri(eventsWithAccount, true /* syncAdapter */, DEFAULT_ACCOUNT,
                        DEFAULT_ACCOUNT_TYPE),
                "_id=" + eventId, null /* selectionArgs */));
        testQueryCount(CalendarContract.Events.CONTENT_URI, null, 2);
        // Right account, should be deleted
        assertEquals("delete", 1, mResolver.delete(
                updatedUri(CalendarContract.Events.CONTENT_URI, true /* syncAdapter */,
                        "user2@google.com", DEFAULT_ACCOUNT_TYPE),
                "_id=" + eventId, null /* selectionArgs */));
        testQueryCount(CalendarContract.Events.CONTENT_URI, null, 1);
    }

    /**
     * Run commands, wiping instance table at each step.
     * This tests full instance expansion.
     * @throws Exception
     */
    public void testCommandSequences1() throws Exception {
        commandSequences(true);
    }

    /**
     * Run commands normally.
     * This tests incremental instance expansion.
     * @throws Exception
     */
    public void testCommandSequences2() throws Exception {
        commandSequences(false);
    }

    /**
     * Run thorough set of command sequences
     * @param wipe true if instances should be wiped and regenerated
     * @throws Exception
     */
    private void commandSequences(boolean wipe) throws Exception {
        Cursor cursor;
        Uri url = null;
        mWipe = wipe; // Set global flag

        mCalendarId = insertCal("Calendar0", DEFAULT_TIMEZONE);

        cursor = mResolver.query(mEventsUri, null, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();
        Command[] commands;

        Log.i(TAG, "Normal insert/delete");
        commands = mNormalInsertDelete;
        for (Command command : commands) {
            command.execute();
        }

        deleteAllEvents();

        Log.i(TAG, "All-day insert/delete");
        commands = mAlldayInsertDelete;
        for (Command command : commands) {
            command.execute();
        }

        deleteAllEvents();

        Log.i(TAG, "Recurring insert/delete");
        commands = mRecurringInsertDelete;
        for (Command command : commands) {
            command.execute();
        }

        deleteAllEvents();

        Log.i(TAG, "Exception with truncated recurrence");
        commands = mExceptionWithTruncatedRecurrence;
        for (Command command : commands) {
            command.execute();
        }

        deleteAllEvents();

        Log.i(TAG, "Exception with moved recurrence");
        commands = mExceptionWithMovedRecurrence;
        for (Command command : commands) {
            command.execute();
        }

        deleteAllEvents();

        Log.i(TAG, "Exception with cancel");
        commands = mCancelInstance;
        for (Command command : commands) {
            command.execute();
        }

        deleteAllEvents();

        Log.i(TAG, "Exception with moved recurrence2");
        commands = mExceptionWithMovedRecurrence2;
        for (Command command : commands) {
            command.execute();
        }

        deleteAllEvents();

        Log.i(TAG, "Exception with no recurrence");
        commands = mExceptionWithNoRecurrence;
        for (Command command : commands) {
            command.execute();
        }
    }

    /**
     * Test Time toString.
     * @throws Exception
     */
    // Suppressed because toString currently hangs.
    @Suppress
    public void testTimeToString() throws Exception {
        Time time = new Time(Time.TIMEZONE_UTC);
        String str = "2039-01-01T23:00:00.000Z";
        String result = "20390101T230000UTC(0,0,0,-1,0)";
        time.parse3339(str);
        assertEquals(result, time.toString());
    }

    /**
     * Test the query done by Event.loadEvents
     * Also test that instance queries work when an event straddles the expansion range
     * @throws Exception
     */
    public void testInstanceQuery() throws Exception {
        final String[] PROJECTION = new String[] {
                Instances.TITLE,                 // 0
                Instances.EVENT_LOCATION,        // 1
                Instances.ALL_DAY,               // 2
                Instances.CALENDAR_COLOR,        // 3
                Instances.EVENT_TIMEZONE,        // 4
                Instances.EVENT_ID,              // 5
                Instances.BEGIN,                 // 6
                Instances.END,                   // 7
                Instances._ID,                   // 8
                Instances.START_DAY,             // 9
                Instances.END_DAY,               // 10
                Instances.START_MINUTE,          // 11
                Instances.END_MINUTE,            // 12
                Instances.HAS_ALARM,             // 13
                Instances.RRULE,                 // 14
                Instances.RDATE,                 // 15
                Instances.SELF_ATTENDEE_STATUS,  // 16
                Events.ORGANIZER,                // 17
                Events.GUESTS_CAN_MODIFY,        // 18
        };

        String orderBy = CalendarProvider2.SORT_CALENDAR_VIEW;
        String where = Instances.SELF_ATTENDEE_STATUS + "!="
                + CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED;

        int calId = insertCal("Calendar0", DEFAULT_TIMEZONE);
        final String START = "2008-05-01T00:00:00";
        final String END = "2008-05-01T20:00:00";

        EventInfo[] events = { new EventInfo("normal0",
                START,
                END,
                false /* allDay */,
                DEFAULT_TIMEZONE) };

        insertEvent(calId, events[0]);

        Time time = new Time(DEFAULT_TIMEZONE);
        time.parse3339(START);
        long startMs = time.toMillis(true /* ignoreDst */);
        // Query starting from way in the past to one hour into the event.
        // Query is more than 2 months so the range won't get extended by the provider.
        Cursor cursor = queryInstances(mResolver, PROJECTION,
                startMs - DateUtils.YEAR_IN_MILLIS, startMs + DateUtils.HOUR_IN_MILLIS,
                where, null, orderBy);
        try {
            assertEquals(1, cursor.getCount());
        } finally {
            cursor.close();
        }

        // Now expand the instance range.  The event overlaps the new part of the range.
        cursor = queryInstances(mResolver, PROJECTION,
                startMs - DateUtils.YEAR_IN_MILLIS, startMs + 2 * DateUtils.HOUR_IN_MILLIS,
                where, null, orderBy);
        try {
            assertEquals(1, cursor.getCount());
        } finally {
            cursor.close();
        }
    }

    /**
     * Performs a query to return all visible instances in the given range that
     * match the given selection. This is a blocking function and should not be
     * done on the UI thread. This will cause an expansion of recurring events
     * to fill this time range if they are not already expanded and will slow
     * down for larger time ranges with many recurring events.
     *
     * @param cr The ContentResolver to use for the query
     * @param projection The columns to return
     * @param begin The start of the time range to query in UTC millis since
     *            epoch
     * @param end The end of the time range to query in UTC millis since epoch
     * @param selection Filter on the query as an SQL WHERE statement
     * @param selectionArgs Args to replace any '?'s in the selection
     * @param orderBy How to order the rows as an SQL ORDER BY statement
     * @return A Cursor of instances matching the selection
     */
    private static final Cursor queryInstances(ContentResolver cr, String[] projection, long begin,
            long end, String selection, String[] selectionArgs, String orderBy) {

        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, begin);
        ContentUris.appendId(builder, end);
        if (TextUtils.isEmpty(selection)) {
            selection = WHERE_CALENDARS_SELECTED;
            selectionArgs = WHERE_CALENDARS_ARGS;
        } else {
            selection = "(" + selection + ") AND " + WHERE_CALENDARS_SELECTED;
            if (selectionArgs != null && selectionArgs.length > 0) {
                selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
                selectionArgs[selectionArgs.length - 1] = WHERE_CALENDARS_ARGS[0];
            } else {
                selectionArgs = WHERE_CALENDARS_ARGS;
            }
        }
        return cr.query(builder.build(), projection, selection, selectionArgs,
                orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
    }

    /**
     * Performs a query to return all visible instances in the given range that
     * match the given selection. This is a blocking function and should not be
     * done on the UI thread. This will cause an expansion of recurring events
     * to fill this time range if they are not already expanded and will slow
     * down for larger time ranges with many recurring events.
     *
     * @param cr The ContentResolver to use for the query
     * @param projection The columns to return
     * @param begin The start of the time range to query in UTC millis since
     *            epoch
     * @param end The end of the time range to query in UTC millis since epoch
     * @param searchQuery A string of space separated search terms. Segments
     *            enclosed by double quotes will be treated as a single term.
     * @param selection Filter on the query as an SQL WHERE statement
     * @param selectionArgs Args to replace any '?'s in the selection
     * @param orderBy How to order the rows as an SQL ORDER BY statement
     * @return A Cursor of instances matching the selection
     */
    public static final Cursor queryInstances(ContentResolver cr, String[] projection, long begin,
            long end, String searchQuery, String selection, String[] selectionArgs, String orderBy)
            {
        Uri.Builder builder = Instances.CONTENT_SEARCH_URI.buildUpon();
        ContentUris.appendId(builder, begin);
        ContentUris.appendId(builder, end);
        builder = builder.appendPath(searchQuery);
        if (TextUtils.isEmpty(selection)) {
            selection = WHERE_CALENDARS_SELECTED;
            selectionArgs = WHERE_CALENDARS_ARGS;
        } else {
            selection = "(" + selection + ") AND " + WHERE_CALENDARS_SELECTED;
            if (selectionArgs != null && selectionArgs.length > 0) {
                selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
                selectionArgs[selectionArgs.length - 1] = WHERE_CALENDARS_ARGS[0];
            } else {
                selectionArgs = WHERE_CALENDARS_ARGS;
            }
        }
        return cr.query(builder.build(), projection, selection, selectionArgs,
                orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
    }

    private Cursor queryInstances(long begin, long end) {
        Uri url = Uri.withAppendedPath(CalendarContract.Instances.CONTENT_URI, begin + "/" + end);
        return mResolver.query(url, null, null, null, null);
    }

    protected static class MockProvider extends ContentProvider {

        private String mAuthority;

        private int mNumItems = 0;

        public MockProvider(String authority) {
            mAuthority = authority;
        }

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection,
                String[] selectionArgs, String sortOrder) {
            return new MatrixCursor(new String[]{ "_id" }, 0);
        }

        @Override
        public String getType(Uri uri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            mNumItems++;
            return Uri.parse("content://" + mAuthority + "/" + mNumItems);
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection,
                String[] selectionArgs) {
            return 0;
        }
    }

    private void cleanCalendarDataTable(SQLiteOpenHelper helper) {
        if (null == helper) {
            return;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("DELETE FROM CalendarCache;");
    }

    public void testGetAndSetTimezoneDatabaseVersion() throws CalendarCache.CacheException {
        CalendarDatabaseHelper helper = (CalendarDatabaseHelper) getProvider().getDatabaseHelper();
        cleanCalendarDataTable(helper);
        CalendarCache cache = new CalendarCache(helper);

        boolean hasException = false;
        try {
            String value = cache.readData(null);
        } catch (CalendarCache.CacheException e) {
            hasException = true;
        }
        assertTrue(hasException);

        assertNull(cache.readTimezoneDatabaseVersion());

        cache.writeTimezoneDatabaseVersion("1234");
        assertEquals("1234", cache.readTimezoneDatabaseVersion());

        cache.writeTimezoneDatabaseVersion("5678");
        assertEquals("5678", cache.readTimezoneDatabaseVersion());
    }

    private void checkEvent(int eventId, String title, long dtStart, long dtEnd, boolean allDay) {
        Uri uri = Uri.parse("content://" + CalendarContract.AUTHORITY + "/events");
        Log.i(TAG, "Looking for EventId = " + eventId);

        Cursor cursor = mResolver.query(uri, null, null, null, null);
        assertEquals(1, cursor.getCount());

        int colIndexTitle = cursor.getColumnIndex(CalendarContract.Events.TITLE);
        int colIndexDtStart = cursor.getColumnIndex(CalendarContract.Events.DTSTART);
        int colIndexDtEnd = cursor.getColumnIndex(CalendarContract.Events.DTEND);
        int colIndexAllDay = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY);
        if (!cursor.moveToNext()) {
            Log.e(TAG,"Could not find inserted event");
            assertTrue(false);
        }
        assertEquals(title, cursor.getString(colIndexTitle));
        assertEquals(dtStart, cursor.getLong(colIndexDtStart));
        assertEquals(dtEnd, cursor.getLong(colIndexDtEnd));
        assertEquals(allDay, (cursor.getInt(colIndexAllDay) != 0));
        cursor.close();
    }

    public void testChangeTimezoneDB() {
        int calId = insertCal("Calendar0", DEFAULT_TIMEZONE);

        Cursor cursor = mResolver
                .query(CalendarContract.Events.CONTENT_URI, null, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        EventInfo[] events = { new EventInfo("normal0",
                                        "2008-05-01T00:00:00",
                                        "2008-05-02T00:00:00",
                                        false,
                                        DEFAULT_TIMEZONE) };

        Uri uri = insertEvent(calId, events[0]);
        assertNotNull(uri);

        // check the inserted event
        checkEvent(1, events[0].mTitle, events[0].mDtstart, events[0].mDtend, events[0].mAllDay);

        // inject a new time zone
        getProvider().doProcessEventRawTimes(TIME_ZONE_AMERICA_ANCHORAGE,
                MOCK_TIME_ZONE_DATABASE_VERSION);

        // check timezone database version
        assertEquals(MOCK_TIME_ZONE_DATABASE_VERSION, getProvider().getTimezoneDatabaseVersion());

        // check that the inserted event has *not* been updated
        checkEvent(1, events[0].mTitle, events[0].mDtstart, events[0].mDtend, events[0].mAllDay);
    }

    public static final Uri PROPERTIES_CONTENT_URI =
            Uri.parse("content://" + CalendarContract.AUTHORITY + "/properties");

    public static final int COLUMN_KEY_INDEX = 1;
    public static final int COLUMN_VALUE_INDEX = 0;

    public void testGetProviderProperties() throws CalendarCache.CacheException {
        CalendarDatabaseHelper helper = (CalendarDatabaseHelper) getProvider().getDatabaseHelper();
        cleanCalendarDataTable(helper);
        CalendarCache cache = new CalendarCache(helper);

        cache.writeTimezoneDatabaseVersion("2010k");
        cache.writeTimezoneInstances("America/Denver");
        cache.writeTimezoneInstancesPrevious("America/Los_Angeles");
        cache.writeTimezoneType(CalendarCache.TIMEZONE_TYPE_AUTO);

        Cursor cursor = mResolver.query(PROPERTIES_CONTENT_URI, null, null, null, null);
        assertEquals(4, cursor.getCount());

        assertEquals(CalendarCache.COLUMN_NAME_KEY, cursor.getColumnName(COLUMN_KEY_INDEX));
        assertEquals(CalendarCache.COLUMN_NAME_VALUE, cursor.getColumnName(COLUMN_VALUE_INDEX));

        Map<String, String> map = new HashMap<String, String>();

        while (cursor.moveToNext()) {
            String key = cursor.getString(COLUMN_KEY_INDEX);
            String value = cursor.getString(COLUMN_VALUE_INDEX);
            map.put(key, value);
        }

        assertTrue(map.containsKey(CalendarCache.KEY_TIMEZONE_DATABASE_VERSION));
        assertTrue(map.containsKey(CalendarCache.KEY_TIMEZONE_TYPE));
        assertTrue(map.containsKey(CalendarCache.KEY_TIMEZONE_INSTANCES));
        assertTrue(map.containsKey(CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS));

        assertEquals("2010k", map.get(CalendarCache.KEY_TIMEZONE_DATABASE_VERSION));
        assertEquals("America/Denver", map.get(CalendarCache.KEY_TIMEZONE_INSTANCES));
        assertEquals("America/Los_Angeles", map.get(CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS));
        assertEquals(CalendarCache.TIMEZONE_TYPE_AUTO, map.get(CalendarCache.KEY_TIMEZONE_TYPE));

        cursor.close();
    }

    public void testGetProviderPropertiesByKey() throws CalendarCache.CacheException {
        CalendarDatabaseHelper helper = (CalendarDatabaseHelper) getProvider().getDatabaseHelper();
        cleanCalendarDataTable(helper);
        CalendarCache cache = new CalendarCache(helper);

        cache.writeTimezoneDatabaseVersion("2010k");
        cache.writeTimezoneInstances("America/Denver");
        cache.writeTimezoneInstancesPrevious("America/Los_Angeles");
        cache.writeTimezoneType(CalendarCache.TIMEZONE_TYPE_AUTO);

        checkValueForKey(CalendarCache.TIMEZONE_TYPE_AUTO, CalendarCache.KEY_TIMEZONE_TYPE);
        checkValueForKey("2010k", CalendarCache.KEY_TIMEZONE_DATABASE_VERSION);
        checkValueForKey("America/Denver", CalendarCache.KEY_TIMEZONE_INSTANCES);
        checkValueForKey("America/Los_Angeles", CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS);
    }

    private void checkValueForKey(String value, String key) {
        Cursor cursor = mResolver.query(PROPERTIES_CONTENT_URI, null,
                "key=?", new String[] {key}, null);

        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertEquals(cursor.getString(COLUMN_KEY_INDEX), key);
        assertEquals(cursor.getString(COLUMN_VALUE_INDEX), value);

        cursor.close();
    }

    public void testUpdateProviderProperties() throws CalendarCache.CacheException {
        CalendarDatabaseHelper helper = (CalendarDatabaseHelper) getProvider().getDatabaseHelper();
        cleanCalendarDataTable(helper);
        CalendarCache cache = new CalendarCache(helper);

        String localTimezone = TimeZone.getDefault().getID();

        // Set initial value
        cache.writeTimezoneDatabaseVersion("2010k");

        updateValueForKey("2009s", CalendarCache.KEY_TIMEZONE_DATABASE_VERSION);
        checkValueForKey("2009s", CalendarCache.KEY_TIMEZONE_DATABASE_VERSION);

        // Set initial values
        cache.writeTimezoneType(CalendarCache.TIMEZONE_TYPE_AUTO);
        cache.writeTimezoneInstances("America/Chicago");
        cache.writeTimezoneInstancesPrevious("America/Denver");

        updateValueForKey(CalendarCache.TIMEZONE_TYPE_AUTO, CalendarCache.KEY_TIMEZONE_TYPE);
        checkValueForKey(localTimezone, CalendarCache.KEY_TIMEZONE_INSTANCES);
        checkValueForKey("America/Denver", CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS);

        updateValueForKey(CalendarCache.TIMEZONE_TYPE_HOME, CalendarCache.KEY_TIMEZONE_TYPE);
        checkValueForKey("America/Denver", CalendarCache.KEY_TIMEZONE_INSTANCES);
        checkValueForKey("America/Denver", CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS);

        // Set initial value
        cache.writeTimezoneInstancesPrevious("");
        updateValueForKey(localTimezone, CalendarCache.KEY_TIMEZONE_INSTANCES);
        checkValueForKey(localTimezone, CalendarCache.KEY_TIMEZONE_INSTANCES);
        checkValueForKey(localTimezone, CalendarCache.KEY_TIMEZONE_INSTANCES_PREVIOUS);
    }

    private void updateValueForKey(String value, String key) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CalendarCache.COLUMN_NAME_VALUE, value);

        int result = mResolver.update(PROPERTIES_CONTENT_URI,
                contentValues,
                CalendarCache.COLUMN_NAME_KEY + "=?",
                new String[] {key});

        assertEquals(1, result);
    }

    public void testInsertOriginalTimezoneInExtProperties() throws Exception {
        int calId = insertCal("Calendar0", DEFAULT_TIMEZONE);


        EventInfo[] events = { new EventInfo("normal0",
                                        "2008-05-01T00:00:00",
                                        "2008-05-02T00:00:00",
                                        false,
                                        DEFAULT_TIMEZONE) };

        Uri eventUri = insertEvent(calId, events[0]);
        assertNotNull(eventUri);

        long eventId = ContentUris.parseId(eventUri);
        assertTrue(eventId > -1);

        // check the inserted event
        checkEvent(1, events[0].mTitle, events[0].mDtstart, events[0].mDtend, events[0].mAllDay);

        // Should have 1 calendars and 1 event
        testQueryCount(CalendarContract.Calendars.CONTENT_URI, null /* where */, 1);
        testQueryCount(CalendarContract.Events.CONTENT_URI, null /* where */, 1);

        // Verify that the original timezone is correct
        Cursor cursor = mResolver.query(CalendarContract.ExtendedProperties.CONTENT_URI,
                null/* projection */,
                "event_id=" + eventId,
                null /* selectionArgs */,
                null /* sortOrder */);
        try {
            // Should have 1 extended property for the original timezone
            assertEquals(1, cursor.getCount());

            if (cursor.moveToFirst()) {
                long id = cursor.getLong(1);
                assertEquals(id, eventId);

                assertEquals(CalendarProvider2.EXT_PROP_ORIGINAL_TIMEZONE, cursor.getString(2));
                assertEquals(DEFAULT_TIMEZONE, cursor.getString(3));
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Verifies that the number of defined calendars meets expectations.
     *
     * @param expectedCount The number of calendars we expect to find.
     */
    private void checkCalendarCount(int expectedCount) {
        Cursor cursor = mResolver.query(mCalendarsUri,
                null /* projection */,
                null /* selection */,
                null /* selectionArgs */,
                null /* sortOrder */);
        assertEquals(expectedCount, cursor.getCount());
        cursor.close();
    }

    private void checkCalendarExists(int calId) {
        assertTrue(isCalendarExists(calId));
    }

    private void checkCalendarDoesNotExists(int calId) {
        assertFalse(isCalendarExists(calId));
    }

    private boolean isCalendarExists(int calId) {
        Cursor cursor = mResolver.query(mCalendarsUri,
                new String[] {Calendars._ID},
                null /* selection */,
                null /* selectionArgs */,
                null /* sortOrder */);
        boolean found = false;
        while (cursor.moveToNext()) {
            if (calId == cursor.getInt(0)) {
                found = true;
                break;
            }
        }
        cursor.close();
        return found;
    }

    public void testDeleteAllCalendars() {
        checkCalendarCount(0);

        insertCal("Calendar1", "America/Los_Angeles");
        insertCal("Calendar2", "America/Los_Angeles");

        checkCalendarCount(2);

        deleteMatchingCalendars(null /* selection */, null /* selectionArgs*/);
        checkCalendarCount(0);
    }

    public void testDeleteCalendarsWithSelection() {
        checkCalendarCount(0);

        int calId1 = insertCal("Calendar1", "America/Los_Angeles");
        int calId2 = insertCal("Calendar2", "America/Los_Angeles");

        checkCalendarCount(2);
        checkCalendarExists(calId1);
        checkCalendarExists(calId2);

        deleteMatchingCalendars(Calendars._ID + "=" + calId2, null /* selectionArgs*/);
        checkCalendarCount(1);
        checkCalendarExists(calId1);
        checkCalendarDoesNotExists(calId2);
    }

    public void testDeleteCalendarsWithSelectionAndArgs() {
        checkCalendarCount(0);

        int calId1 = insertCal("Calendar1", "America/Los_Angeles");
        int calId2 = insertCal("Calendar2", "America/Los_Angeles");

        checkCalendarCount(2);
        checkCalendarExists(calId1);
        checkCalendarExists(calId2);

        deleteMatchingCalendars(Calendars._ID + "=?",
                new String[] { Integer.toString(calId2) });
        checkCalendarCount(1);
        checkCalendarExists(calId1);
        checkCalendarDoesNotExists(calId2);

        deleteMatchingCalendars(Calendars._ID + "=?" + " AND " + Calendars.NAME + "=?",
                new String[] { Integer.toString(calId1), "Calendar1" });
        checkCalendarCount(0);
    }
}
