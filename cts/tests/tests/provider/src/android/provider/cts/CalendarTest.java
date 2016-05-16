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

package android.provider.cts;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.EntityIterator;
import android.content.Intent;
import android.content.IntentFilter;
import android.cts.util.PollingCheck;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.CalendarEntity;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.EventsEntity;
import android.provider.CalendarContract.ExtendedProperties;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;
import android.provider.CalendarContract.SyncState;
import android.test.InstrumentationCtsTestRunner;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarTest extends InstrumentationTestCase {

    private static final String TAG = "CalCTS";
    private static final String CTS_TEST_TYPE = "LOCAL";

    // an arbitrary int used by some tests
    private static final int SOME_ARBITRARY_INT = 143234;

    // 10 sec timeout for reminder broadcast (but shouldn't usually take this long).
    private static final int POLLING_TIMEOUT = 10000;

    // @formatter:off
    private static final String[] TIME_ZONES = new String[] {
            "UTC",
            "America/Los_Angeles",
            "Asia/Beirut",
            "Pacific/Auckland", };
    // @formatter:on

    private static final String SQL_WHERE_ID = Events._ID + "=?";
    private static final String SQL_WHERE_CALENDAR_ID = Events.CALENDAR_ID + "=?";

    private ContentResolver mContentResolver;

    /** If set, log verbose instance info when running recurrence tests. */
    private static final boolean DEBUG_RECURRENCE = false;

    private static class CalendarHelper {

        // @formatter:off
        public static final String[] CALENDARS_SYNC_PROJECTION = new String[] {
                Calendars._ID,
                Calendars.ACCOUNT_NAME,
                Calendars.ACCOUNT_TYPE,
                Calendars._SYNC_ID,
                Calendars.CAL_SYNC7,
                Calendars.CAL_SYNC8,
                Calendars.DIRTY,
                Calendars.NAME,
                Calendars.CALENDAR_DISPLAY_NAME,
                Calendars.CALENDAR_COLOR,
                Calendars.CALENDAR_COLOR_KEY,
                Calendars.CALENDAR_ACCESS_LEVEL,
                Calendars.VISIBLE,
                Calendars.SYNC_EVENTS,
                Calendars.CALENDAR_LOCATION,
                Calendars.CALENDAR_TIME_ZONE,
                Calendars.OWNER_ACCOUNT,
                Calendars.CAN_ORGANIZER_RESPOND,
                Calendars.CAN_MODIFY_TIME_ZONE,
                Calendars.MAX_REMINDERS,
                Calendars.ALLOWED_REMINDERS,
                Calendars.ALLOWED_AVAILABILITY,
                Calendars.ALLOWED_ATTENDEE_TYPES,
                Calendars.DELETED,
                Calendars.CAL_SYNC1,
                Calendars.CAL_SYNC2,
                Calendars.CAL_SYNC3,
                Calendars.CAL_SYNC4,
                Calendars.CAL_SYNC5,
                Calendars.CAL_SYNC6,
                };
        // @formatter:on

        private CalendarHelper() {}     // do not instantiate this class

        /**
         * Generates the e-mail address for the Calendar owner.  Use this for
         * Calendars.OWNER_ACCOUNT, Events.OWNER_ACCOUNT, and for Attendees.ATTENDEE_EMAIL
         * when you want a "self" attendee entry.
         */
        static String generateCalendarOwnerEmail(String account) {
            return "OWNER_" + account + "@example.com";
        }

        /**
         * Creates a new set of values for creating a single calendar with every
         * field.
         *
         * @param account The account name to create this calendar with
         * @param seed A number used to generate the values
         * @return A complete set of values for the calendar
         */
        public static ContentValues getNewCalendarValues(
                String account, int seed) {
            String seedString = Long.toString(seed);
            ContentValues values = new ContentValues();
            values.put(Calendars.ACCOUNT_TYPE, CTS_TEST_TYPE);

            values.put(Calendars.ACCOUNT_NAME, account);
            values.put(Calendars._SYNC_ID, "SYNC_ID:" + seedString);
            values.put(Calendars.CAL_SYNC7, "SYNC_V:" + seedString);
            values.put(Calendars.CAL_SYNC8, "SYNC_TIME:" + seedString);
            values.put(Calendars.DIRTY, 0);
            values.put(Calendars.OWNER_ACCOUNT, generateCalendarOwnerEmail(account));

            values.put(Calendars.NAME, seedString);
            values.put(Calendars.CALENDAR_DISPLAY_NAME, "DISPLAY_" + seedString);

            values.put(Calendars.CALENDAR_ACCESS_LEVEL, (seed % 8) * 100);

            values.put(Calendars.CALENDAR_COLOR, 0xff000000 + seed);
            values.put(Calendars.VISIBLE, seed % 2);
            values.put(Calendars.SYNC_EVENTS, 1);   // must be 1 for recurrence expansion
            values.put(Calendars.CALENDAR_LOCATION, "LOCATION:" + seedString);
            values.put(Calendars.CALENDAR_TIME_ZONE, TIME_ZONES[seed % TIME_ZONES.length]);
            values.put(Calendars.CAN_ORGANIZER_RESPOND, seed % 2);
            values.put(Calendars.CAN_MODIFY_TIME_ZONE, seed % 2);
            values.put(Calendars.MAX_REMINDERS, 3);
            values.put(Calendars.ALLOWED_REMINDERS, "0,1,2");   // does not include SMS (3)
            values.put(Calendars.ALLOWED_ATTENDEE_TYPES, "0,1,2,3");
            values.put(Calendars.ALLOWED_AVAILABILITY, "0,1,2,3");
            values.put(Calendars.CAL_SYNC1, "SYNC1:" + seedString);
            values.put(Calendars.CAL_SYNC2, "SYNC2:" + seedString);
            values.put(Calendars.CAL_SYNC3, "SYNC3:" + seedString);
            values.put(Calendars.CAL_SYNC4, "SYNC4:" + seedString);
            values.put(Calendars.CAL_SYNC5, "SYNC5:" + seedString);
            values.put(Calendars.CAL_SYNC6, "SYNC6:" + seedString);

            return values;
        }

        /**
         * Creates a set of values with just the updates and modifies the
         * original values to the expected values
         */
        public static ContentValues getUpdateCalendarValuesWithOriginal(
                ContentValues original, int seed) {
            ContentValues values = new ContentValues();
            String seedString = Long.toString(seed);

            values.put(Calendars.CALENDAR_DISPLAY_NAME, "DISPLAY_" + seedString);
            values.put(Calendars.CALENDAR_COLOR, 0xff000000 + seed);
            values.put(Calendars.VISIBLE, seed % 2);
            values.put(Calendars.SYNC_EVENTS, seed % 2);

            original.putAll(values);
            original.put(Calendars.DIRTY, 1);

            return values;
        }

        public static int deleteCalendarById(ContentResolver resolver, long id) {
            return resolver.delete(Calendars.CONTENT_URI, Calendars._ID + "=?",
                    new String[] { Long.toString(id) });
        }

        public static int deleteCalendarByAccount(ContentResolver resolver, String account) {
            return resolver.delete(Calendars.CONTENT_URI, Calendars.ACCOUNT_NAME + "=?",
                    new String[] { account });
        }

        public static Cursor getCalendarsByAccount(ContentResolver resolver, String account) {
            String selection = Calendars.ACCOUNT_TYPE + "=?";
            String[] selectionArgs;
            if (account != null) {
                selection += " AND " + Calendars.ACCOUNT_NAME + "=?";
                selectionArgs = new String[2];
                selectionArgs[1] = account;
            } else {
                selectionArgs = new String[1];
            }
            selectionArgs[0] = CTS_TEST_TYPE;

            return resolver.query(Calendars.CONTENT_URI, CALENDARS_SYNC_PROJECTION, selection,
                    selectionArgs, null);
        }
    }

    /**
     * Helper class for manipulating entries in the _sync_state table.
     */
    private static class SyncStateHelper {
        public static final String[] SYNCSTATE_PROJECTION = new String[] {
            SyncState._ID,
            SyncState.ACCOUNT_NAME,
            SyncState.ACCOUNT_TYPE,
            SyncState.DATA
        };

        private static final byte[] SAMPLE_SYNC_DATA = {
            (byte) 'H', (byte) 'e', (byte) 'l', (byte) 'l', (byte) 'o'
        };

        private SyncStateHelper() {}      // do not instantiate

        /**
         * Creates a new set of values for creating a new _sync_state entry.
         */
        public static ContentValues getNewSyncStateValues(String account) {
            ContentValues values = new ContentValues();
            values.put(SyncState.DATA, SAMPLE_SYNC_DATA);
            values.put(SyncState.ACCOUNT_NAME, account);
            values.put(SyncState.ACCOUNT_TYPE, CTS_TEST_TYPE);
            return values;
        }

        /**
         * Retrieves the _sync_state entry with the specified ID.
         */
        public static Cursor getSyncStateById(ContentResolver resolver, long id) {
            Uri uri = ContentUris.withAppendedId(SyncState.CONTENT_URI, id);
            return resolver.query(uri, SYNCSTATE_PROJECTION, null, null, null);
        }

        /**
         * Retrieves the _sync_state entry for the specified account.
         */
        public static Cursor getSyncStateByAccount(ContentResolver resolver, String account) {
            assertNotNull(account);
            String selection = SyncState.ACCOUNT_TYPE + "=? AND " + SyncState.ACCOUNT_NAME + "=?";
            String[] selectionArgs = new String[] { CTS_TEST_TYPE, account };

            return resolver.query(SyncState.CONTENT_URI, SYNCSTATE_PROJECTION, selection,
                    selectionArgs, null);
        }

        /**
         * Deletes the _sync_state entry with the specified ID.  Always done as app.
         */
        public static int deleteSyncStateById(ContentResolver resolver, long id) {
            Uri uri = ContentUris.withAppendedId(SyncState.CONTENT_URI, id);
            return resolver.delete(uri, null, null);
        }

        /**
         * Deletes the _sync_state entry associated with the specified account.  Can be done
         * as app or sync adapter.
         */
        public static int deleteSyncStateByAccount(ContentResolver resolver, String account,
                boolean asSyncAdapter) {
            Uri uri = SyncState.CONTENT_URI;
            if (asSyncAdapter) {
                uri = asSyncAdapter(uri, account, CTS_TEST_TYPE);
            }
            return resolver.delete(uri, SyncState.ACCOUNT_NAME + "=?",
                    new String[] { account });
        }
    }

    // @formatter:off
    private static class EventHelper {
        public static final String[] EVENTS_PROJECTION = new String[] {
            Events._ID,
            Events.ACCOUNT_NAME,
            Events.ACCOUNT_TYPE,
            Events.OWNER_ACCOUNT,
            // Events.ORGANIZER_CAN_RESPOND, from Calendars
            // Events.CAN_CHANGE_TZ, from Calendars
            // Events.MAX_REMINDERS, from Calendars
            Events.CALENDAR_ID,
            // Events.CALENDAR_DISPLAY_NAME, from Calendars
            // Events.CALENDAR_COLOR, from Calendars
            // Events.CALENDAR_ACL, from Calendars
            // Events.CALENDAR_VISIBLE, from Calendars
            Events.SYNC_DATA3,
            Events.SYNC_DATA6,
            Events.TITLE,
            Events.EVENT_LOCATION,
            Events.DESCRIPTION,
            Events.STATUS,
            Events.SELF_ATTENDEE_STATUS,
            Events.DTSTART,
            Events.DTEND,
            Events.EVENT_TIMEZONE,
            Events.EVENT_END_TIMEZONE,
            Events.EVENT_COLOR,
            Events.EVENT_COLOR_KEY,
            Events.DURATION,
            Events.ALL_DAY,
            Events.ACCESS_LEVEL,
            Events.AVAILABILITY,
            Events.HAS_ALARM,
            Events.HAS_EXTENDED_PROPERTIES,
            Events.RRULE,
            Events.RDATE,
            Events.EXRULE,
            Events.EXDATE,
            Events.ORIGINAL_ID,
            Events.ORIGINAL_SYNC_ID,
            Events.ORIGINAL_INSTANCE_TIME,
            Events.ORIGINAL_ALL_DAY,
            Events.LAST_DATE,
            Events.HAS_ATTENDEE_DATA,
            Events.GUESTS_CAN_MODIFY,
            Events.GUESTS_CAN_INVITE_OTHERS,
            Events.GUESTS_CAN_SEE_GUESTS,
            Events.ORGANIZER,
            Events.DELETED,
            Events._SYNC_ID,
            Events.SYNC_DATA4,
            Events.SYNC_DATA5,
            Events.DIRTY,
            Events.SYNC_DATA8,
            Events.SYNC_DATA2,
            Events.SYNC_DATA1,
            Events.SYNC_DATA2,
            Events.SYNC_DATA3,
            Events.SYNC_DATA4,
        };
        // @formatter:on

        private EventHelper() {}    // do not instantiate this class

        /**
         * Constructs a set of name/value pairs that can be used to create a Calendar event.
         * Various fields are generated from the seed value.
         */
        public static ContentValues getNewEventValues(
                String account, int seed, long calendarId, boolean asSyncAdapter) {
            String seedString = Long.toString(seed);
            ContentValues values = new ContentValues();
            values.put(Events.ORGANIZER, "ORGANIZER:" + seedString);

            values.put(Events.TITLE, "TITLE:" + seedString);
            values.put(Events.EVENT_LOCATION, "LOCATION_" + seedString);

            values.put(Events.CALENDAR_ID, calendarId);

            values.put(Events.DESCRIPTION, "DESCRIPTION:" + seedString);
            values.put(Events.STATUS, seed % 2);    // avoid STATUS_CANCELED for general testing

            values.put(Events.DTSTART, seed);
            values.put(Events.DTEND, seed + DateUtils.HOUR_IN_MILLIS);
            values.put(Events.EVENT_TIMEZONE, TIME_ZONES[seed % TIME_ZONES.length]);
            values.put(Events.EVENT_COLOR, seed);
            // values.put(Events.EVENT_TIMEZONE2, TIME_ZONES[(seed +1) %
            // TIME_ZONES.length]);
            if ((seed % 2) == 0) {
                // Either set to zero, or leave unset to get default zero.
                // Must be 0 or dtstart/dtend will get adjusted.
                values.put(Events.ALL_DAY, 0);
            }
            values.put(Events.ACCESS_LEVEL, seed % 4);
            values.put(Events.AVAILABILITY, seed % 2);
            values.put(Events.HAS_EXTENDED_PROPERTIES, seed % 2);
            values.put(Events.HAS_ATTENDEE_DATA, seed % 2);
            values.put(Events.GUESTS_CAN_MODIFY, seed % 2);
            values.put(Events.GUESTS_CAN_INVITE_OTHERS, seed % 2);
            values.put(Events.GUESTS_CAN_SEE_GUESTS, seed % 2);

            // Default is STATUS_TENTATIVE (0).  We either set it to that explicitly, or leave
            // it set to the default.
            if (seed != Events.STATUS_TENTATIVE) {
                values.put(Events.SELF_ATTENDEE_STATUS, Events.STATUS_TENTATIVE);
            }

            if (asSyncAdapter) {
                values.put(Events._SYNC_ID, "SYNC_ID:" + seedString);
                values.put(Events.SYNC_DATA4, "SYNC_V:" + seedString);
                values.put(Events.SYNC_DATA5, "SYNC_TIME:" + seedString);
                values.put(Events.SYNC_DATA3, "HTML:" + seedString);
                values.put(Events.SYNC_DATA6, "COMMENTS:" + seedString);
                values.put(Events.DIRTY, 0);
                values.put(Events.SYNC_DATA8, "0");
            } else {
                // only the sync adapter can set the DIRTY flag
                //values.put(Events.DIRTY, 1);
            }
            // values.put(Events.SYNC1, "SYNC1:" + seedString);
            // values.put(Events.SYNC2, "SYNC2:" + seedString);
            // values.put(Events.SYNC3, "SYNC3:" + seedString);
            // values.put(Events.SYNC4, "SYNC4:" + seedString);
            // values.put(Events.SYNC5, "SYNC5:" + seedString);
//            Events.RRULE,
//            Events.RDATE,
//            Events.EXRULE,
//            Events.EXDATE,
//            // Events.ORIGINAL_ID
//            Events.ORIGINAL_EVENT, // rename ORIGINAL_SYNC_ID
//            Events.ORIGINAL_INSTANCE_TIME,
//            Events.ORIGINAL_ALL_DAY,

            return values;
        }

        /**
         * Constructs a set of name/value pairs that can be used to create a recurring
         * Calendar event.
         *
         * A duration of "P1D" is treated as an all-day event.
         *
         * @param startWhen Starting date/time in RFC 3339 format
         * @param duration Event duration, in RFC 2445 duration format
         * @param rrule Recurrence rule
         * @return name/value pairs to use when creating event
         */
        public static ContentValues getNewRecurringEventValues(String account, int seed,
                long calendarId, boolean asSyncAdapter, String startWhen, String duration,
                String rrule) {

            // Set up some general stuff.
            ContentValues values = getNewEventValues(account, seed, calendarId, asSyncAdapter);

            // Replace the DTSTART field.
            String timeZone = values.getAsString(Events.EVENT_TIMEZONE);
            Time time = new Time(timeZone);
            time.parse3339(startWhen);
            values.put(Events.DTSTART, time.toMillis(false));

            // Add in the recurrence-specific fields, and drop DTEND.
            values.put(Events.RRULE, rrule);
            values.put(Events.DURATION, duration);
            values.remove(Events.DTEND);

            return values;
        }

        /**
         * Constructs the basic name/value pairs required for an exception to a recurring event.
         *
         * @param instanceStartMillis The start time of the instance
         * @return name/value pairs to use when creating event
         */
        public static ContentValues getNewExceptionValues(long instanceStartMillis) {
            ContentValues values = new ContentValues();
            values.put(Events.ORIGINAL_INSTANCE_TIME, instanceStartMillis);

            return values;
        }

        public static ContentValues getUpdateEventValuesWithOriginal(ContentValues original,
                int seed, boolean asSyncAdapter) {
            String seedString = Long.toString(seed);
            ContentValues values = new ContentValues();

            values.put(Events.TITLE, "TITLE:" + seedString);
            values.put(Events.EVENT_LOCATION, "LOCATION_" + seedString);
            values.put(Events.DESCRIPTION, "DESCRIPTION:" + seedString);
            values.put(Events.STATUS, seed % 3);

            values.put(Events.DTSTART, seed);
            values.put(Events.DTEND, seed + DateUtils.HOUR_IN_MILLIS);
            values.put(Events.EVENT_TIMEZONE, TIME_ZONES[seed % TIME_ZONES.length]);
            // values.put(Events.EVENT_TIMEZONE2, TIME_ZONES[(seed +1) %
            // TIME_ZONES.length]);
            values.put(Events.ACCESS_LEVEL, seed % 4);
            values.put(Events.AVAILABILITY, seed % 2);
            values.put(Events.HAS_EXTENDED_PROPERTIES, seed % 2);
            values.put(Events.HAS_ATTENDEE_DATA, seed % 2);
            values.put(Events.GUESTS_CAN_MODIFY, seed % 2);
            values.put(Events.GUESTS_CAN_INVITE_OTHERS, seed % 2);
            values.put(Events.GUESTS_CAN_SEE_GUESTS, seed % 2);
            if (asSyncAdapter) {
                values.put(Events._SYNC_ID, "SYNC_ID:" + seedString);
                values.put(Events.SYNC_DATA4, "SYNC_V:" + seedString);
                values.put(Events.SYNC_DATA5, "SYNC_TIME:" + seedString);
                values.put(Events.DIRTY, 0);
            }
            original.putAll(values);
            return values;
        }

        public static void addDefaultReadOnlyValues(ContentValues values, String account,
                boolean asSyncAdapter) {
            values.put(Events.SELF_ATTENDEE_STATUS, Events.STATUS_TENTATIVE);
            values.put(Events.DELETED, 0);
            values.put(Events.DIRTY, asSyncAdapter ? 0 : 1);
            values.put(Events.OWNER_ACCOUNT, CalendarHelper.generateCalendarOwnerEmail(account));
            values.put(Events.ACCOUNT_TYPE, CTS_TEST_TYPE);
            values.put(Events.ACCOUNT_NAME, account);
        }

        /**
         * Generates a RFC2445-format duration string.
         */
        private static String generateDurationString(long durationMillis, boolean isAllDay) {
            long durationSeconds = durationMillis / 1000;

            // The server may react differently to an all-day event specified as "P1D" than
            // it will to "PT86400S"; see b/1594638.
            if (isAllDay && (durationSeconds % 86400) == 0) {
                return "P" + durationSeconds / 86400 + "D";
            } else {
                return "PT" + durationSeconds + "S";
            }
        }

        /**
         * Deletes the event, and updates the values.
         * @param resolver The resolver to issue the query against.
         * @param uri The deletion URI.
         * @param values Set of values to update (sets DELETED and DIRTY).
         * @return The number of rows modified.
         */
        public static int deleteEvent(ContentResolver resolver, Uri uri, ContentValues values) {
            values.put(Events.DELETED, 1);
            values.put(Events.DIRTY, 1);
            return resolver.delete(uri, null, null);
        }

        public static int deleteEventAsSyncAdapter(ContentResolver resolver, Uri uri,
                String account) {
            Uri syncUri = asSyncAdapter(uri, account, CTS_TEST_TYPE);
            return resolver.delete(syncUri, null, null);
        }

        public static Cursor getEventsByAccount(ContentResolver resolver, String account) {
            String selection = Calendars.ACCOUNT_TYPE + "=?";
            String[] selectionArgs;
            if (account != null) {
                selection += " AND " + Calendars.ACCOUNT_NAME + "=?";
                selectionArgs = new String[2];
                selectionArgs[1] = account;
            } else {
                selectionArgs = new String[1];
            }
            selectionArgs[0] = CTS_TEST_TYPE;
            return resolver.query(Events.CONTENT_URI, EVENTS_PROJECTION, selection, selectionArgs,
                    null);
        }

        public static Cursor getEventByUri(ContentResolver resolver, Uri uri) {
            return resolver.query(uri, EVENTS_PROJECTION, null, null, null);
        }

        /**
         * Looks up the specified Event in the database and returns the "selfAttendeeStatus"
         * value.
         */
        public static int lookupSelfAttendeeStatus(ContentResolver resolver, long eventId) {
            return getIntFromDatabase(resolver, Events.CONTENT_URI, eventId,
                    Events.SELF_ATTENDEE_STATUS);
        }

        /**
         * Looks up the specified Event in the database and returns the "hasAlarm"
         * value.
         */
        public static int lookupHasAlarm(ContentResolver resolver, long eventId) {
            return getIntFromDatabase(resolver, Events.CONTENT_URI, eventId,
                    Events.HAS_ALARM);
        }
    }

    /**
     * Helper class for manipulating entries in the Attendees table.
     */
    private static class AttendeeHelper {
        public static final String[] ATTENDEES_PROJECTION = new String[] {
            Attendees._ID,
            Attendees.EVENT_ID,
            Attendees.ATTENDEE_NAME,
            Attendees.ATTENDEE_EMAIL,
            Attendees.ATTENDEE_STATUS,
            Attendees.ATTENDEE_RELATIONSHIP,
            Attendees.ATTENDEE_TYPE
        };
        // indexes into projection
        public static final int ATTENDEES_ID_INDEX = 0;
        public static final int ATTENDEES_EVENT_ID_INDEX = 1;

        // do not instantiate
        private AttendeeHelper() {}

        /**
         * Adds a new attendee to the specified event.
         *
         * @return the _id of the new attendee, or -1 on failure
         */
        public static long addAttendee(ContentResolver resolver, long eventId, String name,
                String email, int status, int relationship, int type) {
            Uri uri = Attendees.CONTENT_URI;

            ContentValues attendee = new ContentValues();
            attendee.put(Attendees.EVENT_ID, eventId);
            attendee.put(Attendees.ATTENDEE_NAME, name);
            attendee.put(Attendees.ATTENDEE_EMAIL, email);
            attendee.put(Attendees.ATTENDEE_STATUS, status);
            attendee.put(Attendees.ATTENDEE_RELATIONSHIP, relationship);
            attendee.put(Attendees.ATTENDEE_TYPE, type);
            Uri result = resolver.insert(uri, attendee);
            return ContentUris.parseId(result);
        }

        /**
         * Finds all Attendees rows for the specified event and email address.  The returned
         * cursor will use {@link AttendeeHelper#ATTENDEES_PROJECTION}.
         */
        public static Cursor findAttendeesByEmail(ContentResolver resolver, long eventId,
                String email) {
            return resolver.query(Attendees.CONTENT_URI, ATTENDEES_PROJECTION,
                    Attendees.EVENT_ID + "=? AND " + Attendees.ATTENDEE_EMAIL + "=?",
                    new String[] { String.valueOf(eventId), email }, null);
        }
    }

    /**
     * Helper class for manipulating entries in the Colors table.
     */
    private static class ColorHelper {
        public static final String WHERE_COLOR_ACCOUNT = Colors.ACCOUNT_NAME + "=? AND "
                + Colors.ACCOUNT_TYPE + "=?";
        public static final String WHERE_COLOR_ACCOUNT_AND_INDEX = WHERE_COLOR_ACCOUNT + " AND "
                + Colors.COLOR_KEY + "=?";

        public static final String[] COLORS_PROJECTION = new String[] {
                Colors._ID, // 0
                Colors.ACCOUNT_NAME, // 1
                Colors.ACCOUNT_TYPE, // 2
                Colors.DATA, // 3
                Colors.COLOR_TYPE, // 4
                Colors.COLOR_KEY, // 5
                Colors.COLOR, // 6
        };
        // indexes into projection
        public static final int COLORS_ID_INDEX = 0;
        public static final int COLORS_INDEX_INDEX = 5;
        public static final int COLORS_COLOR_INDEX = 6;

        public static final int[] DEFAULT_TYPES = new int[] {
                Colors.TYPE_CALENDAR, Colors.TYPE_CALENDAR, Colors.TYPE_CALENDAR,
                Colors.TYPE_CALENDAR, Colors.TYPE_EVENT, Colors.TYPE_EVENT, Colors.TYPE_EVENT,
                Colors.TYPE_EVENT,
        };
        public static final int[] DEFAULT_COLORS = new int[] {
                0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFAA00AA, 0xFF00AAAA, 0xFF333333, 0xFFAAAA00,
                0xFFAAAAAA,
        };
        public static final String[] DEFAULT_INDICES = new String[] {
                "000", "001", "010", "011", "100", "101", "110", "111",
        };

        public static final int C_COLOR_0 = 0;
        public static final int C_COLOR_1 = 1;
        public static final int C_COLOR_2 = 2;
        public static final int C_COLOR_3 = 3;
        public static final int E_COLOR_0 = 4;
        public static final int E_COLOR_1 = 5;
        public static final int E_COLOR_2 = 6;
        public static final int E_COLOR_3 = 7;

        // do not instantiate
        private ColorHelper() {
        }

        /**
         * Adds a new color to the colors table.
         *
         * @return the _id of the new color, or -1 on failure
         */
        public static long addColor(ContentResolver resolver, String accountName,
                String accountType, String data, String index, int type, int color) {
            Uri uri = asSyncAdapter(Colors.CONTENT_URI, accountName, accountType);

            ContentValues colorValues = new ContentValues();
            colorValues.put(Colors.DATA, data);
            colorValues.put(Colors.COLOR_KEY, index);
            colorValues.put(Colors.COLOR_TYPE, type);
            colorValues.put(Colors.COLOR, color);
            Uri result = resolver.insert(uri, colorValues);
            return ContentUris.parseId(result);
        }

        /**
         * Finds the color specified by an account name/type and a color index.
         * The returned cursor will use {@link ColorHelper#COLORS_PROJECTION}.
         */
        public static Cursor findColorByIndex(ContentResolver resolver, String accountName,
                String accountType, String index) {
            return resolver.query(Colors.CONTENT_URI, COLORS_PROJECTION,
                    WHERE_COLOR_ACCOUNT_AND_INDEX,
                    new String[] {accountName, accountType, index}, null);
        }

        public static Cursor findColorsByAccount(ContentResolver resolver, String accountName,
                String accountType) {
            return resolver.query(Colors.CONTENT_URI, COLORS_PROJECTION, WHERE_COLOR_ACCOUNT,
                    new String[] { accountName, accountType }, null);
        }

        /**
         * Adds a default set of test colors to the Colors table under the given
         * account.
         *
         * @return true if the default colors were added successfully
         */
        public static boolean addDefaultColorsToAccount(ContentResolver resolver,
                String accountName, String accountType) {
            for (int i = 0; i < DEFAULT_INDICES.length; i++) {
                long id = addColor(resolver, accountName, accountType, null, DEFAULT_INDICES[i],
                        DEFAULT_TYPES[i], DEFAULT_COLORS[i]);
                if (id == -1) {
                    return false;
                }
            }
            return true;
        }

        public static void deleteColorsByAccount(ContentResolver resolver, String accountName,
                String accountType) {
            Uri uri = asSyncAdapter(Colors.CONTENT_URI, accountName, accountType);
            resolver.delete(uri, WHERE_COLOR_ACCOUNT, new String[] { accountName, accountType });
        }
    }


    /**
     * Helper class for manipulating entries in the Reminders table.
     */
    private static class ReminderHelper {
        public static final String[] REMINDERS_PROJECTION = new String[] {
            Reminders._ID,
            Reminders.EVENT_ID,
            Reminders.MINUTES,
            Reminders.METHOD
        };
        // indexes into projection
        public static final int REMINDERS_ID_INDEX = 0;
        public static final int REMINDERS_EVENT_ID_INDEX = 1;
        public static final int REMINDERS_MINUTES_INDEX = 2;
        public static final int REMINDERS_METHOD_INDEX = 3;

        // do not instantiate
        private ReminderHelper() {}

        /**
         * Adds a new reminder to the specified event.
         *
         * @return the _id of the new reminder, or -1 on failure
         */
        public static long addReminder(ContentResolver resolver, long eventId, int minutes,
                int method) {
            Uri uri = Reminders.CONTENT_URI;

            ContentValues reminder = new ContentValues();
            reminder.put(Reminders.EVENT_ID, eventId);
            reminder.put(Reminders.MINUTES, minutes);
            reminder.put(Reminders.METHOD, method);
            Uri result = resolver.insert(uri, reminder);
            return ContentUris.parseId(result);
        }

        /**
         * Finds all Reminders rows for the specified event.  The returned cursor will use
         * {@link ReminderHelper#REMINDERS_PROJECTION}.
         */
        public static Cursor findRemindersByEventId(ContentResolver resolver, long eventId) {
            return resolver.query(Reminders.CONTENT_URI, REMINDERS_PROJECTION,
                    Reminders.EVENT_ID + "=?", new String[] { String.valueOf(eventId) }, null);
        }

        /**
         * Looks up the specified Reminders row and returns the "method" value.
         */
        public static int lookupMethod(ContentResolver resolver, long remId) {
            return getIntFromDatabase(resolver, Reminders.CONTENT_URI, remId,
                    Reminders.METHOD);
        }
   }

    /**
     * Helper class for manipulating entries in the ExtendedProperties table.
     */
    private static class ExtendedPropertiesHelper {
        public static final String[] EXTENDED_PROPERTIES_PROJECTION = new String[] {
            ExtendedProperties._ID,
            ExtendedProperties.EVENT_ID,
            ExtendedProperties.NAME,
            ExtendedProperties.VALUE
        };
        // indexes into projection
        public static final int EXTENDED_PROPERTIES_ID_INDEX = 0;
        public static final int EXTENDED_PROPERTIES_EVENT_ID_INDEX = 1;
        public static final int EXTENDED_PROPERTIES_NAME_INDEX = 2;
        public static final int EXTENDED_PROPERTIES_VALUE_INDEX = 3;

        // do not instantiate
        private ExtendedPropertiesHelper() {}

        /**
         * Adds a new ExtendedProperty for the specified event.  Runs as sync adapter.
         *
         * @return the _id of the new ExtendedProperty, or -1 on failure
         */
        public static long addExtendedProperty(ContentResolver resolver, String account,
                long eventId, String name, String value) {
             Uri uri = asSyncAdapter(ExtendedProperties.CONTENT_URI, account, CTS_TEST_TYPE);

            ContentValues ep = new ContentValues();
            ep.put(ExtendedProperties.EVENT_ID, eventId);
            ep.put(ExtendedProperties.NAME, name);
            ep.put(ExtendedProperties.VALUE, value);
            Uri result = resolver.insert(uri, ep);
            return ContentUris.parseId(result);
        }

        /**
         * Finds all ExtendedProperties rows for the specified event.  The returned cursor will
         * use {@link ExtendedPropertiesHelper#EXTENDED_PROPERTIES_PROJECTION}.
         */
        public static Cursor findExtendedPropertiesByEventId(ContentResolver resolver,
                long eventId) {
            return resolver.query(ExtendedProperties.CONTENT_URI, EXTENDED_PROPERTIES_PROJECTION,
                    ExtendedProperties.EVENT_ID + "=?",
                    new String[] { String.valueOf(eventId) }, null);
        }

        /**
         * Finds an ExtendedProperties entry with a matching name for the specified event, and
         * returns the value.  Throws an exception if we don't find exactly one row.
         */
        public static String lookupValueByName(ContentResolver resolver, long eventId,
                String name) {
            Cursor cursor = resolver.query(ExtendedProperties.CONTENT_URI,
                    EXTENDED_PROPERTIES_PROJECTION,
                    ExtendedProperties.EVENT_ID + "=? AND " + ExtendedProperties.NAME + "=?",
                    new String[] { String.valueOf(eventId), name }, null);

            try {
                if (cursor.getCount() != 1) {
                    throw new RuntimeException("Got " + cursor.getCount() + " results, expected 1");
                }

                cursor.moveToFirst();
                return cursor.getString(EXTENDED_PROPERTIES_VALUE_INDEX);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
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

    /**
     * Returns the value of the specified row and column in the Events table, as an integer.
     * Throws an exception if the specified row or column doesn't exist or doesn't contain
     * an integer (e.g. null entry).
     */
    private static int getIntFromDatabase(ContentResolver resolver, Uri uri, long rowId,
            String columnName) {
        String[] projection = { columnName };
        String selection = SQL_WHERE_ID;
        String[] selectionArgs = { String.valueOf(rowId) };

        Cursor c = resolver.query(uri, projection, selection, selectionArgs, null);
        try {
            assertEquals(1, c.getCount());
            c.moveToFirst();
            return c.getInt(0);
        } finally {
            c.close();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = getInstrumentation().getTargetContext().getContentResolver();
    }

    @MediumTest
    public void testCalendarCreationAndDeletion() {
        String account = "cc1_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);
        long id = createAndVerifyCalendar(account, seed++, null);

        removeAndVerifyCalendar(account, id);
    }

    /**
     * Tests whether the default projections work.  We don't need to have any data in
     * the calendar, since it's testing the database schema.
     */
    @MediumTest
    public void testDefaultProjections() {
        String account = "dproj_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);
        long id = createAndVerifyCalendar(account, seed++, null);

        Cursor c;
        Uri uri;
        // Calendars
        c = mContentResolver.query(Calendars.CONTENT_URI, null, null, null, null);
        c.close();
        // Events
        c = mContentResolver.query(Events.CONTENT_URI, null, null, null, null);
        c.close();
        // Instances
        uri = Uri.withAppendedPath(Instances.CONTENT_URI, "0/1");
        c = mContentResolver.query(uri, null, null, null, null);
        c.close();
        // Attendees
        c = mContentResolver.query(Attendees.CONTENT_URI, null, null, null, null);
        c.close();
        // Reminders (only REMINDERS_ID currently uses default projection)
        uri = ContentUris.withAppendedId(Reminders.CONTENT_URI, 0);
        c = mContentResolver.query(uri, null, null, null, null);
        c.close();
        // CalendarAlerts
        c = mContentResolver.query(CalendarContract.CalendarAlerts.CONTENT_URI,
                null, null, null, null);
        c.close();
        // CalendarCache
        c = mContentResolver.query(CalendarContract.CalendarCache.URI,
                null, null, null, null);
        c.close();
        // CalendarEntity
        c = mContentResolver.query(CalendarContract.CalendarEntity.CONTENT_URI,
                null, null, null, null);
        c.close();
        // EventEntities
        c = mContentResolver.query(CalendarContract.EventsEntity.CONTENT_URI,
                null, null, null, null);
        c.close();
        // EventDays
        uri = Uri.withAppendedPath(CalendarContract.EventDays.CONTENT_URI, "1/2");
        c = mContentResolver.query(uri, null, null, null, null);
        c.close();
        // ExtendedProperties
        c = mContentResolver.query(CalendarContract.ExtendedProperties.CONTENT_URI,
                null, null, null, null);
        c.close();

        removeAndVerifyCalendar(account, id);
    }

    /**
     * Exercises the EventsEntity class.
     */
    @MediumTest
    public void testEventsEntityQuery() {
        String account = "eeq_account";
        int seed = 0;

        // Clean up just in case.
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar.
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create three events.  We need to make sure SELF_ATTENDEE_STATUS isn't set, because
        // that causes the provider to generate an Attendees entry, and that'll throw off
        // our expected count.
        ContentValues eventValues;
        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        eventValues.remove(Events.SELF_ATTENDEE_STATUS);
        long eventId1 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId1 >= 0);

        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        eventValues.remove(Events.SELF_ATTENDEE_STATUS);
        long eventId2 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId2 >= 0);

        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        eventValues.remove(Events.SELF_ATTENDEE_STATUS);
        long eventId3 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId3 >= 0);

        /*
         * Add some attendees, reminders, and extended properties.
         */
        Uri uri, syncUri;

        syncUri = asSyncAdapter(Reminders.CONTENT_URI, account, CTS_TEST_TYPE);
        ContentValues remValues = new ContentValues();
        remValues.put(Reminders.EVENT_ID, eventId1);
        remValues.put(Reminders.MINUTES, 10);
        remValues.put(Reminders.METHOD, Reminders.METHOD_ALERT);
        mContentResolver.insert(syncUri, remValues);
        remValues.put(Reminders.MINUTES, 20);
        mContentResolver.insert(syncUri, remValues);

        syncUri = asSyncAdapter(ExtendedProperties.CONTENT_URI, account, CTS_TEST_TYPE);
        ContentValues extended = new ContentValues();
        extended.put(ExtendedProperties.NAME, "foo");
        extended.put(ExtendedProperties.VALUE, "bar");
        extended.put(ExtendedProperties.EVENT_ID, eventId2);
        mContentResolver.insert(syncUri, extended);
        extended.put(ExtendedProperties.EVENT_ID, eventId1);
        mContentResolver.insert(syncUri, extended);
        extended.put(ExtendedProperties.NAME, "foo2");
        extended.put(ExtendedProperties.VALUE, "bar2");
        mContentResolver.insert(syncUri, extended);

        syncUri = asSyncAdapter(Attendees.CONTENT_URI, account, CTS_TEST_TYPE);
        ContentValues attendee = new ContentValues();
        attendee.put(Attendees.ATTENDEE_NAME, "Joe");
        attendee.put(Attendees.ATTENDEE_EMAIL, CalendarHelper.generateCalendarOwnerEmail(account));
        attendee.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_DECLINED);
        attendee.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_REQUIRED);
        attendee.put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_PERFORMER);
        attendee.put(Attendees.EVENT_ID, eventId3);
        mContentResolver.insert(syncUri, attendee);

        /*
         * Iterate over all events on our calendar.  Peek at a few values to see if they
         * look reasonable.
         */
        EntityIterator ei = EventsEntity.newEntityIterator(
                mContentResolver.query(EventsEntity.CONTENT_URI, null, Events.CALENDAR_ID + "=?",
                        new String[] { String.valueOf(calendarId) }, null),
                mContentResolver);
        int count = 0;
        try {
            while (ei.hasNext()) {
                Entity entity = ei.next();
                ContentValues values = entity.getEntityValues();
                ArrayList<Entity.NamedContentValues> subvalues = entity.getSubValues();
                long eventId = values.getAsLong(Events._ID);
                if (eventId == eventId1) {
                    // 2 x reminder, 2 x extended properties
                    assertEquals(4, subvalues.size());
                } else if (eventId == eventId2) {
                    // Extended properties
                    assertEquals(1, subvalues.size());
                    ContentValues subContentValues = subvalues.get(0).values;
                    String name = subContentValues.getAsString(
                            CalendarContract.ExtendedProperties.NAME);
                    String value = subContentValues.getAsString(
                            CalendarContract.ExtendedProperties.VALUE);
                    assertEquals("foo", name);
                    assertEquals("bar", value);
                } else if (eventId == eventId3) {
                    // Attendees
                    assertEquals(1, subvalues.size());
                } else {
                    fail("should not be here");
                }
                count++;
            }
            assertEquals(3, count);
        } finally {
            ei.close();
        }

        // Confirm that querying for a single event yields a single event.
        ei = EventsEntity.newEntityIterator(
                mContentResolver.query(EventsEntity.CONTENT_URI, null, SQL_WHERE_ID,
                        new String[] { String.valueOf(eventId3) }, null),
                mContentResolver);
        try {
            count = 0;
            while (ei.hasNext()) {
                Entity entity = ei.next();
                count++;
            }
            assertEquals(1, count);
        } finally {
            ei.close();
        }


        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Exercises the CalendarEntity class.
     */
    @MediumTest
    public void testCalendarEntityQuery() {
        String account1 = "ceq1_account";
        String account2 = "ceq2_account";
        String account3 = "ceq3_account";
        int seed = 0;

        // Clean up just in case.
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account1);
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account2);
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account3);

        // Create calendars.
        long calendarId1 = createAndVerifyCalendar(account1, seed++, null);
        long calendarId2 = createAndVerifyCalendar(account2, seed++, null);
        long calendarId3 = createAndVerifyCalendar(account3, seed++, null);

        EntityIterator ei = CalendarEntity.newEntityIterator(
                mContentResolver.query(CalendarEntity.CONTENT_URI, null,
                        Calendars._ID + "=? OR " + Calendars._ID + "=? OR " + Calendars._ID + "=?",
                        new String[] { String.valueOf(calendarId1), String.valueOf(calendarId2),
                                String.valueOf(calendarId3) },
                        null));

        try {
            int count = 0;
            while (ei.hasNext()) {
                Entity entity = ei.next();
                count++;
            }
            assertEquals(3, count);
        } finally {
            ei.close();
        }

        removeAndVerifyCalendar(account1, calendarId1);
        removeAndVerifyCalendar(account2, calendarId2);
        removeAndVerifyCalendar(account3, calendarId3);
    }

    /**
     * Tests creation and manipulation of Attendees.
     */
    @MediumTest
    public void testAttendees() {
        String account = "att_account";
        int seed = 0;

        // Clean up just in case.
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar.
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create two events, one with a value set for SELF_ATTENDEE_STATUS, one without.
        ContentValues eventValues;
        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        eventValues.put(Events.SELF_ATTENDEE_STATUS, Events.STATUS_TENTATIVE);
        long eventId1 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId1 >= 0);

        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        eventValues.remove(Events.SELF_ATTENDEE_STATUS);
        long eventId2 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId2 >= 0);

        /*
         * Add some attendees to the first event.
         */
        long attId1 = AttendeeHelper.addAttendee(mContentResolver, eventId1,
                "Alice",
                "alice@example.com",
                Attendees.ATTENDEE_STATUS_TENTATIVE,
                Attendees.RELATIONSHIP_ATTENDEE,
                Attendees.TYPE_REQUIRED);
        long attId2 = AttendeeHelper.addAttendee(mContentResolver, eventId1,
                "Betty",
                "betty@example.com",
                Attendees.ATTENDEE_STATUS_DECLINED,
                Attendees.RELATIONSHIP_ATTENDEE,
                Attendees.TYPE_NONE);
        long attId3 = AttendeeHelper.addAttendee(mContentResolver, eventId1,
                "Carol",
                "carol@example.com",
                Attendees.ATTENDEE_STATUS_DECLINED,
                Attendees.RELATIONSHIP_ATTENDEE,
                Attendees.TYPE_OPTIONAL);

        /*
         * Find the event1 "self" attendee entry.
         */
        Cursor cursor = AttendeeHelper.findAttendeesByEmail(mContentResolver, eventId1,
                CalendarHelper.generateCalendarOwnerEmail(account));
        try {
            assertEquals(1, cursor.getCount());
            //DatabaseUtils.dumpCursor(cursor);

            cursor.moveToFirst();
            long id = cursor.getLong(AttendeeHelper.ATTENDEES_ID_INDEX);

            /*
             * Update the status field.  The provider should automatically propagate the result.
             */
            ContentValues update = new ContentValues();
            Uri uri = ContentUris.withAppendedId(Attendees.CONTENT_URI, id);

            update.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_ACCEPTED);
            int count = mContentResolver.update(uri, update, null, null);
            assertEquals(1, count);

            int status = EventHelper.lookupSelfAttendeeStatus(mContentResolver, eventId1);
            assertEquals(Attendees.ATTENDEE_STATUS_ACCEPTED, status);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        /*
         * Do a bulk update of all Attendees for this event, changing any Attendee with status
         * "declined" to "invited".
         */
        ContentValues bulkUpdate = new ContentValues();
        bulkUpdate.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_INVITED);

        int count = mContentResolver.update(Attendees.CONTENT_URI, bulkUpdate,
                Attendees.EVENT_ID + "=? AND " + Attendees.ATTENDEE_STATUS + "=?",
                new String[] {
                    String.valueOf(eventId1), String.valueOf(Attendees.ATTENDEE_STATUS_DECLINED)
                });
        assertEquals(2, count);

        /*
         * Add a new, non-self attendee to the second event.
         */
        long attId4 = AttendeeHelper.addAttendee(mContentResolver, eventId2,
                "Diana",
                "diana@example.com",
                Attendees.ATTENDEE_STATUS_ACCEPTED,
                Attendees.RELATIONSHIP_ATTENDEE,
                Attendees.TYPE_REQUIRED);

        /*
         * Confirm that the selfAttendeeStatus on the second event has the default value.
         */
        int status = EventHelper.lookupSelfAttendeeStatus(mContentResolver, eventId2);
        assertEquals(Attendees.ATTENDEE_STATUS_NONE, status);

        /*
         * Create a new "self" attendee in the second event by updating the email address to
         * match that of the calendar owner.
         */
        ContentValues newSelf = new ContentValues();
        newSelf.put(Attendees.ATTENDEE_EMAIL, CalendarHelper.generateCalendarOwnerEmail(account));
        count = mContentResolver.update(ContentUris.withAppendedId(Attendees.CONTENT_URI, attId4),
                newSelf, null, null);
        assertEquals(1, count);

        /*
         * Confirm that the event's selfAttendeeStatus has been updated.
         */
        status = EventHelper.lookupSelfAttendeeStatus(mContentResolver, eventId2);
        assertEquals(Attendees.ATTENDEE_STATUS_ACCEPTED, status);

        /*
         * TODO:  (these are unexpected usage patterns)
         * - Update an Attendee's status and event_id to move it to a different event, and
         *   confirm that the selfAttendeeStatus in the destination event is updated (rather
         *   than that of the source event).
         * - Create two Attendees with email addresses that match "self" but have different
         *   values for "status".  Delete one and confirm that selfAttendeeStatus is changed
         *   to that of the remaining Attendee.  (There is no defined behavior for
         *   selfAttendeeStatus when there are multiple matching Attendees.)
         */

        /*
         * Test deletion, singly by ID and in bulk.
         */
        count = mContentResolver.delete(ContentUris.withAppendedId(Attendees.CONTENT_URI, attId4),
                null, null);
        assertEquals(1, count);

        count = mContentResolver.delete(Attendees.CONTENT_URI, Attendees.EVENT_ID + "=?",
                new String[] { String.valueOf(eventId1) });
        assertEquals(4, count);     // 3 we created + 1 auto-added by the provider

        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Tests creation and manipulation of Reminders.
     */
    @MediumTest
    public void testReminders() {
        String account = "rem_account";
        int seed = 0;

        // Clean up just in case.
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar.
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create events.
        ContentValues eventValues;
        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        long eventId1 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId1 >= 0);
        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        long eventId2 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId2 >= 0);

        // No reminders, hasAlarm should be zero.
        int hasAlarm = EventHelper.lookupHasAlarm(mContentResolver, eventId1);
        assertEquals(0, hasAlarm);
        hasAlarm = EventHelper.lookupHasAlarm(mContentResolver, eventId2);
        assertEquals(0, hasAlarm);

        /*
         * Add some reminders.
         */
        long remId1 = ReminderHelper.addReminder(mContentResolver, eventId1,
                10, Reminders.METHOD_DEFAULT);
        long remId2 = ReminderHelper.addReminder(mContentResolver, eventId1,
                15, Reminders.METHOD_ALERT);
        long remId3 = ReminderHelper.addReminder(mContentResolver, eventId1,
                20, Reminders.METHOD_SMS);  // SMS isn't allowed for this calendar

        // Should have been set to 1 by provider.
        hasAlarm = EventHelper.lookupHasAlarm(mContentResolver, eventId1);
        assertEquals(1, hasAlarm);

        // Add a reminder to event2.
        ReminderHelper.addReminder(mContentResolver, eventId2,
                20, Reminders.METHOD_DEFAULT);
        hasAlarm = EventHelper.lookupHasAlarm(mContentResolver, eventId2);
        assertEquals(1, hasAlarm);


        /*
         * Check the entries.
         */
        Cursor cursor = ReminderHelper.findRemindersByEventId(mContentResolver, eventId1);
        try {
            assertEquals(3, cursor.getCount());
            //DatabaseUtils.dumpCursor(cursor);

            while (cursor.moveToNext()) {
                int minutes = cursor.getInt(ReminderHelper.REMINDERS_MINUTES_INDEX);
                int method = cursor.getInt(ReminderHelper.REMINDERS_METHOD_INDEX);
                switch (minutes) {
                    case 10:
                        assertEquals(Reminders.METHOD_DEFAULT, method);
                        break;
                    case 15:
                        assertEquals(Reminders.METHOD_ALERT, method);
                        break;
                    case 20:
                        assertEquals(Reminders.METHOD_SMS, method);
                        break;
                    default:
                        fail("unexpected minutes " + minutes);
                        break;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        /*
         * Use the bulk update feature to change all METHOD_DEFAULT to METHOD_EMAIL.  To make
         * this more interesting we first change remId3 to METHOD_DEFAULT.
         */
        int count;
        ContentValues newValues = new ContentValues();
        newValues.put(Reminders.METHOD, Reminders.METHOD_DEFAULT);
        count = mContentResolver.update(ContentUris.withAppendedId(Reminders.CONTENT_URI, remId3),
                newValues, null, null);
        assertEquals(1, count);

        newValues.put(Reminders.METHOD, Reminders.METHOD_EMAIL);
        count = mContentResolver.update(Reminders.CONTENT_URI, newValues,
                Reminders.EVENT_ID + "=? AND " + Reminders.METHOD + "=?",
                new String[] {
                    String.valueOf(eventId1), String.valueOf(Reminders.METHOD_DEFAULT)
                });
        assertEquals(2, count);

        // check it
        int method = ReminderHelper.lookupMethod(mContentResolver, remId3);
        assertEquals(Reminders.METHOD_EMAIL, method);

        /*
         * Delete some / all reminders and confirm that hasAlarm tracks it.
         *
         * You can also remove reminders from an event by updating the event_id column, but
         * that's defined as producing undefined behavior, so we don't do it here.
         */
        count = mContentResolver.delete(Reminders.CONTENT_URI,
                Reminders.EVENT_ID + "=? AND " + Reminders.MINUTES + ">=?",
                new String[] { String.valueOf(eventId1), "15" });
        assertEquals(2, count);
        hasAlarm = EventHelper.lookupHasAlarm(mContentResolver, eventId1);
        assertEquals(1, hasAlarm);

        // Delete all reminders from both events.
        count = mContentResolver.delete(Reminders.CONTENT_URI,
                Reminders.EVENT_ID + "=? OR " + Reminders.EVENT_ID + "=?",
                new String[] { String.valueOf(eventId1), String.valueOf(eventId2) });
        assertEquals(2, count);
        hasAlarm = EventHelper.lookupHasAlarm(mContentResolver, eventId1);
        assertEquals(0, hasAlarm);
        hasAlarm = EventHelper.lookupHasAlarm(mContentResolver, eventId2);
        assertEquals(0, hasAlarm);

        /*
         * Add a couple of reminders and then delete one with the by-ID URI.
         */
        long remId4 = ReminderHelper.addReminder(mContentResolver, eventId1,
                10, Reminders.METHOD_EMAIL);
        long remId5 = ReminderHelper.addReminder(mContentResolver, eventId1,
                15, Reminders.METHOD_EMAIL);
        count = mContentResolver.delete(ContentUris.withAppendedId(Reminders.CONTENT_URI, remId4),
                null, null);
        assertEquals(1, count);

        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * A listener for the EVENT_REMINDER broadcast that is expected to be fired by the
     * provider at the reminder time.
     */
    public class MockReminderReceiver extends BroadcastReceiver {
        public boolean received = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(CalendarContract.ACTION_EVENT_REMINDER)) {
                received = true;
            }
        }
    }

    /**
     * Test that reminders result in the expected broadcast at reminder time.
     */
    public void testRemindersAlarm() throws Exception {
        // Setup: register a mock listener for the broadcast we expect to fire at the
        // reminder time.
        final MockReminderReceiver reminderReceiver = new MockReminderReceiver();
        IntentFilter filter = new IntentFilter(CalendarContract.ACTION_EVENT_REMINDER);
        filter.addDataScheme("content");
        getInstrumentation().getTargetContext().registerReceiver(reminderReceiver, filter);

        // Clean up just in case.
        String account = "rem_account";
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar.  Use '1' as seed as this sets the VISIBLE field to 1.
        // The calendar must be visible for its notifications to occur.
        long calendarId = createAndVerifyCalendar(account, 1, null);

        // Create event for 15 min in the past, with a 10 min reminder, so that it will
        // trigger immediately.
        ContentValues eventValues;
        int seed = 0;
        long now = System.currentTimeMillis();
        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        eventValues.put(Events.DTSTART, now - DateUtils.MINUTE_IN_MILLIS * 15);
        eventValues.put(Events.DTEND, now + DateUtils.HOUR_IN_MILLIS);
        long eventId = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId >= 0);
        ReminderHelper.addReminder(mContentResolver, eventId, 10, Reminders.METHOD_ALERT);

        // Confirm that the EVENT_REMINDER broadcast was fired by the provider.
        new PollingCheck(POLLING_TIMEOUT) {
            @Override
            protected boolean check() {
                return reminderReceiver.received;
            }
        }.run();
        assertTrue(reminderReceiver.received);

        removeAndVerifyCalendar(account, calendarId);
    }

    @MediumTest
    public void testColorWriteRequirements() {
        String account = "colw_account";
        String account2 = "colw2_account";
        int seed = 0;
        Uri uri = asSyncAdapter(Colors.CONTENT_URI, account, CTS_TEST_TYPE);
        Uri uri2 = asSyncAdapter(Colors.CONTENT_URI, account2, CTS_TEST_TYPE);

        // Clean up just in case
        ColorHelper.deleteColorsByAccount(mContentResolver, account, CTS_TEST_TYPE);
        ColorHelper.deleteColorsByAccount(mContentResolver, account2, CTS_TEST_TYPE);

        ContentValues colorValues = new ContentValues();
        // Account name/type must be in the query params, so may be left
        // out here
        colorValues.put(Colors.DATA, "0");
        colorValues.put(Colors.COLOR_KEY, "1");
        colorValues.put(Colors.COLOR_TYPE, 0);
        colorValues.put(Colors.COLOR, 0xff000000);

        // Verify only a sync adapter can write to Colors
        try {
            mContentResolver.insert(Colors.CONTENT_URI, colorValues);
            fail("Should not allow non-sync adapter to insert colors");
        } catch (IllegalArgumentException e) {
            // WAI
        }

        // Verify everything except DATA is required
        ContentValues testVals = new ContentValues(colorValues);
        for (String key : colorValues.keySet()) {

            testVals.remove(key);
            try {
                Uri colUri = mContentResolver.insert(uri, testVals);
                if (!TextUtils.equals(key, Colors.DATA)) {
                    // The DATA field is allowed to be empty.
                    fail("Should not allow color creation without " + key);
                }
                ColorHelper.deleteColorsByAccount(mContentResolver, account, CTS_TEST_TYPE);
            } catch (IllegalArgumentException e) {
                if (TextUtils.equals(key, Colors.DATA)) {
                    // The DATA field is allowed to be empty.
                    fail("Should allow color creation without " + key);
                }
            }
            testVals.put(key, colorValues.getAsString(key));
        }

        // Verify writing a color works
        Uri col1 = mContentResolver.insert(uri, colorValues);

        // Verify adding the same color fails
        try {
            mContentResolver.insert(uri, colorValues);
            fail("Should not allow adding the same color twice");
        } catch (IllegalArgumentException e) {
            // WAI
        }

        // Verify specifying a different account than the query params doesn't work
        colorValues.put(Colors.ACCOUNT_NAME, account2);
        try {
            mContentResolver.insert(uri, colorValues);
            fail("Should use the account from the query params, not the values.");
        } catch (IllegalArgumentException e) {
            // WAI
        }

        // Verify adding a color to a different account works
        Uri col2 = mContentResolver.insert(uri2, colorValues);

        // And a different index on the same account
        colorValues.put(Colors.COLOR_KEY, "2");
        Uri col3 = mContentResolver.insert(uri2, colorValues);

        // Verify that all three colors are in the table
        Cursor c = ColorHelper.findColorsByAccount(mContentResolver, account, CTS_TEST_TYPE);
        assertEquals(1, c.getCount());
        c.close();
        c = ColorHelper.findColorsByAccount(mContentResolver, account2, CTS_TEST_TYPE);
        assertEquals(2, c.getCount());
        c.close();

        // Verify deleting them works
        ColorHelper.deleteColorsByAccount(mContentResolver, account, CTS_TEST_TYPE);
        ColorHelper.deleteColorsByAccount(mContentResolver, account2, CTS_TEST_TYPE);

        c = ColorHelper.findColorsByAccount(mContentResolver, account, CTS_TEST_TYPE);
        assertEquals(0, c.getCount());
        c.close();
        c = ColorHelper.findColorsByAccount(mContentResolver, account2, CTS_TEST_TYPE);
        assertEquals(0, c.getCount());
        c.close();
    }

    /**
     * Tests Colors interaction with the Calendars table.
     */
    @MediumTest
    public void testCalendarColors() {
        String account = "cc_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);
        ColorHelper.deleteColorsByAccount(mContentResolver, account, CTS_TEST_TYPE);

        // Test inserting a calendar with an invalid color index
        ContentValues cv = CalendarHelper.getNewCalendarValues(account, seed++);
        cv.put(Calendars.CALENDAR_COLOR_KEY, "badIndex");
        Uri calSyncUri = asSyncAdapter(Calendars.CONTENT_URI, account, CTS_TEST_TYPE);
        Uri colSyncUri = asSyncAdapter(Colors.CONTENT_URI, account, CTS_TEST_TYPE);

        try {
            Uri uri = mContentResolver.insert(calSyncUri, cv);
            fail("Should not allow insertion of invalid color index into Calendars");
        } catch (IllegalArgumentException e) {
            // WAI
        }

        // Test updating a calendar with an invalid color index
        long calendarId = createAndVerifyCalendar(account, seed++, null);
        cv.clear();
        cv.put(Calendars.CALENDAR_COLOR_KEY, "badIndex2");
        Uri calendarUri = ContentUris.withAppendedId(Calendars.CONTENT_URI, calendarId);
        try {
            mContentResolver.update(calendarUri, cv, null, null);
            fail("Should not allow update of invalid color index into Calendars");
        } catch (IllegalArgumentException e) {
            // WAI
        }

        assertTrue(ColorHelper.addDefaultColorsToAccount(mContentResolver, account, CTS_TEST_TYPE));

        // Test that inserting a valid color index works
        cv = CalendarHelper.getNewCalendarValues(account, seed++);
        cv.put(Calendars.CALENDAR_COLOR_KEY, ColorHelper.DEFAULT_INDICES[ColorHelper.C_COLOR_0]);

        Uri uri = mContentResolver.insert(calSyncUri, cv);
        long calendarId2 = ContentUris.parseId(uri);
        assertTrue(calendarId2 >= 0);
        // And updates the calendar's color to the one in the table
        cv.put(Calendars.CALENDAR_COLOR, ColorHelper.DEFAULT_COLORS[ColorHelper.C_COLOR_0]);
        verifyCalendar(account, cv, calendarId2, 2);

        // Test that updating a valid color index also updates the color in a
        // calendar
        cv.clear();
        cv.put(Calendars.CALENDAR_COLOR_KEY, ColorHelper.DEFAULT_INDICES[ColorHelper.C_COLOR_0]);
        mContentResolver.update(calendarUri, cv, null, null);
        Cursor c = mContentResolver.query(calendarUri,
                new String[] { Calendars.CALENDAR_COLOR_KEY, Calendars.CALENDAR_COLOR },
                null, null, null);
        try {
            c.moveToFirst();
            String index = c.getString(0);
            int color = c.getInt(1);
            assertEquals(index, ColorHelper.DEFAULT_INDICES[ColorHelper.C_COLOR_0]);
            assertEquals(color, ColorHelper.DEFAULT_COLORS[ColorHelper.C_COLOR_0]);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // And clearing it doesn't change the color
        cv.put(Calendars.CALENDAR_COLOR_KEY, (String) null);
        mContentResolver.update(calendarUri, cv, null, null);
        c = mContentResolver.query(calendarUri,
                new String[] { Calendars.CALENDAR_COLOR_KEY, Calendars.CALENDAR_COLOR },
                null, null, null);
        try {
            c.moveToFirst();
            String index = c.getString(0);
            int color = c.getInt(1);
            assertEquals(index, null);
            assertEquals(ColorHelper.DEFAULT_COLORS[ColorHelper.C_COLOR_0], color);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // Test that setting a calendar color to an event color fails
        cv.put(Calendars.CALENDAR_COLOR_KEY, ColorHelper.DEFAULT_INDICES[ColorHelper.E_COLOR_0]);
        try {
            mContentResolver.update(calendarUri, cv, null, null);
            fail("Should not allow a calendar to use an event color");
        } catch (IllegalArgumentException e) {
            // WAI
        }

        // Test that you can't remove a color that is referenced by a calendar
        cv.put(Calendars.CALENDAR_COLOR_KEY, ColorHelper.DEFAULT_INDICES[ColorHelper.C_COLOR_3]);
        mContentResolver.update(calendarUri, cv, null, null);

        try {
            mContentResolver.delete(colSyncUri, ColorHelper.WHERE_COLOR_ACCOUNT_AND_INDEX,
                    new String[] {
                            account, CTS_TEST_TYPE,
                            ColorHelper.DEFAULT_INDICES[ColorHelper.C_COLOR_3]
                    });
            fail("Should not allow deleting referenced color");
        } catch (UnsupportedOperationException e) {
            // WAI
        }

        // Clean up
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);
        ColorHelper.deleteColorsByAccount(mContentResolver, account, CTS_TEST_TYPE);
    }

    /**
     * Tests Colors interaction with the Events table.
     */
    @MediumTest
    public void testEventColors() {
        String account = "ec_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);
        ColorHelper.deleteColorsByAccount(mContentResolver, account, CTS_TEST_TYPE);

        // Test inserting an event with an invalid color index
        long cal_id = createAndVerifyCalendar(account, seed++, null);

        Uri colSyncUri = asSyncAdapter(Colors.CONTENT_URI, account, CTS_TEST_TYPE);

        ContentValues ev = EventHelper.getNewEventValues(account, seed++, cal_id, false);
        ev.put(Events.EVENT_COLOR_KEY, "badIndex");

        try {
            Uri uri = mContentResolver.insert(Events.CONTENT_URI, ev);
            fail("Should not allow insertion of invalid color index into Events");
        } catch (IllegalArgumentException e) {
            // WAI
        }

        // Test updating an event with an invalid color index fails
        long event_id = createAndVerifyEvent(account, seed++, cal_id, false, null);
        ev.clear();
        ev.put(Events.EVENT_COLOR_KEY, "badIndex2");
        Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, event_id);
        try {
            mContentResolver.update(eventUri, ev, null, null);
            fail("Should not allow update of invalid color index into Events");
        } catch (IllegalArgumentException e) {
            // WAI
        }

        assertTrue(ColorHelper.addDefaultColorsToAccount(mContentResolver, account, CTS_TEST_TYPE));

        // Test that inserting a valid color index works
        ev = EventHelper.getNewEventValues(account, seed++, cal_id, false);
        final String defaultColorIndex = ColorHelper.DEFAULT_INDICES[ColorHelper.E_COLOR_0];
        ev.put(Events.EVENT_COLOR_KEY, defaultColorIndex);

        Uri uri = mContentResolver.insert(Events.CONTENT_URI, ev);
        long eventId2 = ContentUris.parseId(uri);
        assertTrue(eventId2 >= 0);
        // And updates the event's color to the one in the table
        final int expectedColor = ColorHelper.DEFAULT_COLORS[ColorHelper.E_COLOR_0];
        ev.put(Events.EVENT_COLOR, expectedColor);
        verifyEvent(ev, eventId2);

        // Test that event iterator has COLOR columns
        final EntityIterator iterator = EventsEntity.newEntityIterator(mContentResolver.query(
                ContentUris.withAppendedId(EventsEntity.CONTENT_URI, eventId2),
                null, null, null, null), mContentResolver);
        assertTrue("Empty Iterator", iterator.hasNext());
        final Entity entity = iterator.next();
        final ContentValues values = entity.getEntityValues();
        assertTrue("Missing EVENT_COLOR", values.containsKey(EventsEntity.EVENT_COLOR));
        assertEquals("Wrong EVENT_COLOR",
                expectedColor,
                (int) values.getAsInteger(EventsEntity.EVENT_COLOR));
        assertTrue("Missing EVENT_COLOR_KEY", values.containsKey(EventsEntity.EVENT_COLOR_KEY));
        assertEquals("Wrong EVENT_COLOR_KEY",
                defaultColorIndex,
                values.getAsString(EventsEntity.EVENT_COLOR_KEY));
        iterator.close();

        // Test that updating a valid color index also updates the color in an
        // event
        ev.clear();
        ev.put(Events.EVENT_COLOR_KEY, ColorHelper.DEFAULT_INDICES[ColorHelper.E_COLOR_1]);
        mContentResolver.update(eventUri, ev, null, null);
        Cursor c = mContentResolver.query(eventUri, new String[] {
                Events.EVENT_COLOR_KEY, Events.EVENT_COLOR
        }, null, null, null);
        try {
            c.moveToFirst();
            String index = c.getString(0);
            int color = c.getInt(1);
            assertEquals(index, ColorHelper.DEFAULT_INDICES[ColorHelper.E_COLOR_1]);
            assertEquals(color, ColorHelper.DEFAULT_COLORS[ColorHelper.E_COLOR_1]);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // And clearing it doesn't change the color
        ev.put(Events.EVENT_COLOR_KEY, (String) null);
        mContentResolver.update(eventUri, ev, null, null);
        c = mContentResolver.query(eventUri, new String[] {
                Events.EVENT_COLOR_KEY, Events.EVENT_COLOR
        }, null, null, null);
        try {
            c.moveToFirst();
            String index = c.getString(0);
            int color = c.getInt(1);
            assertEquals(index, null);
            assertEquals(ColorHelper.DEFAULT_COLORS[ColorHelper.E_COLOR_1], color);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // Test that setting an event color to a calendar color fails
        ev.put(Events.EVENT_COLOR_KEY, ColorHelper.DEFAULT_INDICES[ColorHelper.C_COLOR_2]);
        try {
            mContentResolver.update(eventUri, ev, null, null);
            fail("Should not allow an event to use a calendar color");
        } catch (IllegalArgumentException e) {
            // WAI
        }

        // Test that you can't remove a color that is referenced by an event
        ev.put(Events.EVENT_COLOR_KEY, ColorHelper.DEFAULT_INDICES[ColorHelper.E_COLOR_1]);
        mContentResolver.update(eventUri, ev, null, null);
        try {
            mContentResolver.delete(colSyncUri, ColorHelper.WHERE_COLOR_ACCOUNT_AND_INDEX,
                    new String[] {
                            account, CTS_TEST_TYPE,
                            ColorHelper.DEFAULT_INDICES[ColorHelper.E_COLOR_1]
                    });
            fail("Should not allow deleting referenced color");
        } catch (UnsupportedOperationException e) {
            // WAI
        }

        // TODO test colors with exceptions

        // Clean up
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);
        ColorHelper.deleteColorsByAccount(mContentResolver, account, CTS_TEST_TYPE);
    }

    /**
     * Tests creation and manipulation of ExtendedProperties.
     */
    @MediumTest
    public void testExtendedProperties() {
        String account = "ep_account";
        int seed = 0;

        // Clean up just in case.
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar.
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create events.
        ContentValues eventValues;
        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        long eventId1 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId1 >= 0);

        /*
         * Add some extended properties.
         */
        long epId1 = ExtendedPropertiesHelper.addExtendedProperty(mContentResolver, account,
                eventId1, "first", "Jeffrey");
        long epId2 = ExtendedPropertiesHelper.addExtendedProperty(mContentResolver, account,
                eventId1, "last", "Lebowski");
        long epId3 = ExtendedPropertiesHelper.addExtendedProperty(mContentResolver, account,
                eventId1, "title", "Dude");

        /*
         * Spot-check a couple of entries.
         */
        Cursor cursor = ExtendedPropertiesHelper.findExtendedPropertiesByEventId(mContentResolver,
                eventId1);
        try {
            assertEquals(3, cursor.getCount());
            //DatabaseUtils.dumpCursor(cursor);

            while (cursor.moveToNext()) {
                String name =
                    cursor.getString(ExtendedPropertiesHelper.EXTENDED_PROPERTIES_NAME_INDEX);
                String value =
                    cursor.getString(ExtendedPropertiesHelper.EXTENDED_PROPERTIES_VALUE_INDEX);

                if (name.equals("last")) {
                    assertEquals("Lebowski", value);
                }
            }

            String title = ExtendedPropertiesHelper.lookupValueByName(mContentResolver, eventId1,
                    "title");
            assertEquals("Dude", title);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Update the title.  Must be done as a sync adapter.
        ContentValues newValues = new ContentValues();
        newValues.put(ExtendedProperties.VALUE, "Big");
        Uri uri = ContentUris.withAppendedId(ExtendedProperties.CONTENT_URI, epId3);
        uri = asSyncAdapter(uri, account, CTS_TEST_TYPE);
        int count = mContentResolver.update(uri, newValues, null, null);
        assertEquals(1, count);

        // check it
        String title = ExtendedPropertiesHelper.lookupValueByName(mContentResolver, eventId1,
                "title");
        assertEquals("Big", title);

        removeAndVerifyCalendar(account, calendarId);
    }

    private class CalendarEventHelper {

      private long mCalendarId;
      private String mAccount;
      private int mSeed;

      public CalendarEventHelper(String account, int seed) {
        mAccount = account;
        mSeed = seed;
        ContentValues values = CalendarHelper.getNewCalendarValues(account, seed);
        mCalendarId = createAndVerifyCalendar(account, seed++, values);
      }

      public ContentValues addEvent(String timeString, int timeZoneIndex, long duration) {
        long event1Start = timeInMillis(timeString, timeZoneIndex);
        ContentValues eventValues;
        eventValues = EventHelper.getNewEventValues(mAccount, mSeed++, mCalendarId, true);
        eventValues.put(Events.DESCRIPTION, timeString);
        eventValues.put(Events.DTSTART, event1Start);
        eventValues.put(Events.DTEND, event1Start + duration);
        eventValues.put(Events.EVENT_TIMEZONE, TIME_ZONES[timeZoneIndex]);
        long eventId = createAndVerifyEvent(mAccount, mSeed, mCalendarId, true, eventValues);
        assertTrue(eventId >= 0);
        return eventValues;
      }

      public long getCalendarId() {
        return mCalendarId;
      }
    }

    /**
     * Test query to retrieve instances within a certain time interval.
     */
    public void testWhenByDayQuery() {
      String account = "cser_account";
      int seed = 0;

      // Clean up just in case
      CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

      // Create a calendar
      CalendarEventHelper helper = new CalendarEventHelper(account, seed);

      // Add events to the calendar--the first two in the queried range
      List<ContentValues> eventsWithinRange = new ArrayList<ContentValues>();

      ContentValues values = helper.addEvent("2009-10-01T08:00:00", 0, DateUtils.HOUR_IN_MILLIS);
      eventsWithinRange.add(values);

      values = helper.addEvent("2010-10-01T08:00:00", 0, DateUtils.HOUR_IN_MILLIS);
      eventsWithinRange.add(values);

      helper.addEvent("2011-10-01T08:00:00", 0, DateUtils.HOUR_IN_MILLIS);

      // Prepare the start time and end time of the range to query
      String startTime = "2009-01-01T00:00:00";
      String endTime = "2011-01-01T00:00:00";
      int julianStart = getJulianDay(startTime, 0);
      int julianEnd = getJulianDay(endTime, 0);
      Uri uri = Uri.withAppendedPath(
          CalendarContract.Instances.CONTENT_BY_DAY_URI, julianStart + "/" + julianEnd);

      // Query the range, sorting by event start time
      Cursor c = mContentResolver.query(uri, null, Instances.CALENDAR_ID + "="
              + helper.getCalendarId(), null, Events.DTSTART);

      // Assert that two events are returned
      assertEquals(c.getCount(), 2);

      Set<String> keySet = new HashSet();
      keySet.add(Events.DESCRIPTION);
      keySet.add(Events.DTSTART);
      keySet.add(Events.DTEND);
      keySet.add(Events.EVENT_TIMEZONE);

      // Verify that the contents of those two events match the cursor results
      verifyContentValuesAgainstCursor(eventsWithinRange, keySet, c);
    }

    private void verifyContentValuesAgainstCursor(List<ContentValues> cvs,
        Set<String> keys, Cursor cursor) {
      assertEquals(cursor.getCount(), cvs.size());

      cursor.moveToFirst();

      int i=0;
      do {
        ContentValues cv = cvs.get(i);
        for (String key : keys) {
          assertEquals(cv.get(key).toString(),
                  cursor.getString(cursor.getColumnIndex(key)));
        }
        i++;
      } while (cursor.moveToNext());

      cursor.close();
    }

    private long timeInMillis(String timeString, int timeZoneIndex) {
      Time startTime = new Time(TIME_ZONES[timeZoneIndex]);
      startTime.parse3339(timeString);
      return startTime.toMillis(false);
    }

    private int getJulianDay(String timeString, int timeZoneIndex) {
      Time time = new Time(TIME_ZONES[timeZoneIndex]);
      time.parse3339(timeString);
      return Time.getJulianDay(time.toMillis(false), time.gmtoff);
    }

    /**
     * Test instance queries with search parameters.
     */
    @MediumTest
    public void testInstanceSearch() {
        String account = "cser_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create a calendar
        ContentValues values = CalendarHelper.getNewCalendarValues(account, seed);
        long calendarId = createAndVerifyCalendar(account, seed++, values);

        String testStart = "2009-10-01T08:00:00";
        String timeZone = TIME_ZONES[0];
        Time startTime = new Time(timeZone);
        startTime.parse3339(testStart);
        long startMillis = startTime.toMillis(false);

        // Create some events, with different descriptions.  (Could also create a single
        // recurring event and some instance exceptions.)
        ContentValues eventValues;
        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        eventValues.put(Events.DESCRIPTION, "testevent event-one fiddle");
        eventValues.put(Events.DTSTART, startMillis);
        eventValues.put(Events.DTEND, startMillis + DateUtils.HOUR_IN_MILLIS);
        eventValues.put(Events.EVENT_TIMEZONE, timeZone);
        long eventId1 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId1 >= 0);

        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        eventValues.put(Events.DESCRIPTION, "testevent event-two fuzzle");
        eventValues.put(Events.DTSTART, startMillis + DateUtils.HOUR_IN_MILLIS);
        eventValues.put(Events.DTEND, startMillis + DateUtils.HOUR_IN_MILLIS * 2);
        eventValues.put(Events.EVENT_TIMEZONE, timeZone);
        long eventId2 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId2 >= 0);

        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        eventValues.put(Events.DESCRIPTION, "testevent event-three fiddle");
        eventValues.put(Events.DTSTART, startMillis + DateUtils.HOUR_IN_MILLIS * 2);
        eventValues.put(Events.DTEND, startMillis + DateUtils.HOUR_IN_MILLIS * 3);
        eventValues.put(Events.EVENT_TIMEZONE, timeZone);
        long eventId3 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId3 >= 0);

        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        eventValues.put(Events.DESCRIPTION, "nontestevent");
        eventValues.put(Events.DTSTART, startMillis + (long) (DateUtils.HOUR_IN_MILLIS * 1.5f));
        eventValues.put(Events.DTEND, startMillis + DateUtils.HOUR_IN_MILLIS * 2);
        eventValues.put(Events.EVENT_TIMEZONE, timeZone);
        long eventId4 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        assertTrue(eventId4 >= 0);

        String rangeStart = "2009-10-01T00:00:00";
        String rangeEnd = "2009-10-01T11:59:59";
        String[] projection = new String[] { Instances.BEGIN };

        if (false) {
            Cursor instances = getInstances(timeZone, rangeStart, rangeEnd, projection,
                    new long[] { calendarId });
            dumpInstances(instances, timeZone, "all");
            instances.close();
        }

        Cursor instances;
        int count;

        // Find all matching "testevent".  The search matches on partial strings, so this
        // will also pick up "nontestevent".
        instances = getInstancesSearch(timeZone, rangeStart, rangeEnd,
                "testevent", false, projection, new long[] { calendarId });
        count = instances.getCount();
        instances.close();
        assertEquals(4, count);

        // Find all matching "fiddle" and "event".  Set the "by day" flag just to be different.
        instances = getInstancesSearch(timeZone, rangeStart, rangeEnd,
                "fiddle event", true, projection, new long[] { calendarId });
        count = instances.getCount();
        instances.close();
        assertEquals(2, count);

        // Find all matching "fiddle" and "baluchitherium".
        instances = getInstancesSearch(timeZone, rangeStart, rangeEnd,
                "baluchitherium fiddle", false, projection, new long[] { calendarId });
        count = instances.getCount();
        instances.close();
        assertEquals(0, count);

        // Find all matching "event-two".
        instances = getInstancesSearch(timeZone, rangeStart, rangeEnd,
                "event-two", false, projection, new long[] { calendarId });
        count = instances.getCount();
        instances.close();
        assertEquals(1, count);

        removeAndVerifyCalendar(account, calendarId);
    }

    @MediumTest
    public void testCalendarUpdateAsApp() {
        String account = "cu1_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create a calendar
        ContentValues values = CalendarHelper.getNewCalendarValues(account, seed);
        long id = createAndVerifyCalendar(account, seed++, values);

        Uri uri = ContentUris.withAppendedId(Calendars.CONTENT_URI, id);

        // Update the calendar using the direct Uri
        ContentValues updateValues = CalendarHelper.getUpdateCalendarValuesWithOriginal(
                values, seed++);
        assertEquals(1, mContentResolver.update(uri, updateValues, null, null));

        verifyCalendar(account, values, id, 1);

        // Update the calendar using selection + args
        String selection = Calendars._ID + "=?";
        String[] selectionArgs = new String[] { Long.toString(id) };

        updateValues = CalendarHelper.getUpdateCalendarValuesWithOriginal(values, seed++);

        assertEquals(1, mContentResolver.update(
                Calendars.CONTENT_URI, updateValues, selection, selectionArgs));

        verifyCalendar(account, values, id, 1);

        removeAndVerifyCalendar(account, id);
    }

    // TODO test calendar updates as sync adapter

    /**
     * Test access to the "syncstate" table.
     */
    @MediumTest
    public void testSyncState() {
        String account = "ss_account";
        int seed = 0;

        // Clean up just in case
        SyncStateHelper.deleteSyncStateByAccount(mContentResolver, account, true);

        // Create a new sync state entry
        ContentValues values = SyncStateHelper.getNewSyncStateValues(account);
        long id = createAndVerifySyncState(account, values);

        // Look it up with the by-ID URI
        Cursor c = SyncStateHelper.getSyncStateById(mContentResolver, id);
        assertNotNull(c);
        assertEquals(1, c.getCount());
        c.close();

        // Try to remove it as non-sync-adapter; expected to fail.
        boolean failed;
        try {
            SyncStateHelper.deleteSyncStateByAccount(mContentResolver, account, false);
            failed = false;
        } catch (IllegalArgumentException iae) {
            failed = true;
        }
        assertTrue("deletion of sync state by app was allowed", failed);

        // Remove it and verify that it's gone
        removeAndVerifySyncState(account);
    }


    private void verifyEvent(ContentValues values, long eventId) {
        Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
        // Verify
        Cursor c = mContentResolver
                .query(eventUri, EventHelper.EVENTS_PROJECTION, null, null, null);
        assertEquals(1, c.getCount());
        assertTrue(c.moveToFirst());
        assertEquals(eventId, c.getLong(0));
        for (String key : values.keySet()) {
            int index = c.getColumnIndex(key);
            assertEquals(key, values.getAsString(key), c.getString(index));
        }
        c.close();
    }

    @MediumTest
    public void testEventCreationAndDeletion() {
        String account = "ec1_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar and event
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        ContentValues eventValues = EventHelper
                .getNewEventValues(account, seed++, calendarId, true);
        long eventId = createAndVerifyEvent(account, seed, calendarId, true, eventValues);

        Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);

        removeAndVerifyEvent(eventUri, eventValues, account);

        // Attempt to create an event without a calendar ID.
        ContentValues badValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        badValues.remove(Events.CALENDAR_ID);
        try {
            createAndVerifyEvent(account, seed, calendarId, true, badValues);
            fail("was allowed to create an event without CALENDAR_ID");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // Validation may be relaxed for content providers, so test missing timezone as app.
        badValues = EventHelper.getNewEventValues(account, seed++, calendarId, false);
        badValues.remove(Events.EVENT_TIMEZONE);
        try {
            createAndVerifyEvent(account, seed, calendarId, false, badValues);
            fail("was allowed to create an event without EVENT_TIMEZONE");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        removeAndVerifyCalendar(account, calendarId);
    }

    @MediumTest
    public void testEventUpdateAsApp() {
        String account = "em1_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create event as sync adapter
        ContentValues eventValues = EventHelper
                .getNewEventValues(account, seed++, calendarId, true);
        long eventId = createAndVerifyEvent(account, seed, calendarId, true, eventValues);

        // Update event as app
        Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);

        ContentValues updateValues = EventHelper.getUpdateEventValuesWithOriginal(eventValues,
                seed++, false);
        assertEquals(1, mContentResolver.update(eventUri, updateValues, null, null));
        updateValues.put(Events.DIRTY, 1);      // provider should have marked as dirty
        verifyEvent(updateValues, eventId);

        // Try nulling out a required value.
        ContentValues badValues = new ContentValues(updateValues);
        badValues.putNull(Events.EVENT_TIMEZONE);
        badValues.remove(Events.DIRTY);
        try {
            mContentResolver.update(eventUri, badValues, null, null);
            fail("was allowed to null out EVENT_TIMEZONE");
        } catch (IllegalArgumentException iae) {
            // good
        }

        removeAndVerifyEvent(eventUri, eventValues, account);

        // delete the calendar
        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Tests update of multiple events with a single update call.
     */
    @MediumTest
    public void testBulkUpdate() {
        String account = "bup_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar
        long calendarId = createAndVerifyCalendar(account, seed++, null);
        String calendarIdStr = String.valueOf(calendarId);

        // Create events
        ContentValues eventValues;
        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        long eventId1 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);

        eventValues = EventHelper.getNewEventValues(account, seed++, calendarId, true);
        long eventId2 = createAndVerifyEvent(account, seed, calendarId, true, eventValues);

        // Update the "description" field in all events in this calendar.
        String newDescription = "bulk edit";
        ContentValues updateValues = new ContentValues();
        updateValues.put(Events.DESCRIPTION, newDescription);

        // Must be sync adapter to do a bulk update.
        Uri uri = asSyncAdapter(Events.CONTENT_URI, account, CTS_TEST_TYPE);
        int count = mContentResolver.update(uri, updateValues, SQL_WHERE_CALENDAR_ID,
                new String[] { calendarIdStr });

        // Check to see if the changes went through.
        Uri eventUri = Events.CONTENT_URI;
        Cursor c = mContentResolver.query(eventUri, new String[] { Events.DESCRIPTION },
                SQL_WHERE_CALENDAR_ID, new String[] { calendarIdStr }, null);
        assertEquals(2, c.getCount());
        while (c.moveToNext()) {
            assertEquals(newDescription, c.getString(0));
        }
        c.close();

        // delete the calendar
        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Tests the content provider's enforcement of restrictions on who is allowed to modify
     * specific columns in a Calendar.
     * <p>
     * This attempts to create a new row in the Calendar table, specifying one restricted
     * column at a time.
     */
    @MediumTest
    public void testSyncOnlyInsertEnforcement() {
        // These operations should not succeed, so there should be nothing to clean up after.
        // TODO: this should be a new event augmented with an illegal column, not a single
        //       column.  Otherwise we might be tripping over a "DTSTART must exist" test.
        ContentValues vals = new ContentValues();
        for (int i = 0; i < Calendars.SYNC_WRITABLE_COLUMNS.length; i++) {
            boolean threw = false;
            try {
                vals.clear();
                vals.put(Calendars.SYNC_WRITABLE_COLUMNS[i], "1");
                mContentResolver.insert(Calendars.CONTENT_URI, vals);
            } catch (IllegalArgumentException e) {
                threw = true;
            }
            assertTrue("Only sync adapter should be allowed to insert "
                    + Calendars.SYNC_WRITABLE_COLUMNS[i], threw);
        }
    }

    /**
     * Tests creation of a recurring event.
     * <p>
     * This (and the other recurrence tests) uses dates well in the past to reduce the likelihood
     * of encountering non-test recurring events.  (Ideally we would select events associated
     * with a specific calendar.)  With dates well in the past, it's also important to have a
     * fixed maximum count or end date; otherwise, if the metadata min/max instance values are
     * large enough, the recurrence recalculation processor could get triggered on an insert or
     * update and bump up against the 2000-instance limit.
     *
     * TODO: need some allDay tests
     */
    @MediumTest
    public void testRecurrence() {
        String account = "re_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create recurring event
        ContentValues eventValues = EventHelper.getNewRecurringEventValues(account, seed++,
                calendarId, true, "2003-08-05T09:00:00", "PT1H",
                "FREQ=WEEKLY;INTERVAL=2;COUNT=4;BYDAY=TU,SU;WKST=SU");
        long eventId = createAndVerifyEvent(account, seed, calendarId, true, eventValues);
        //Log.d(TAG, "+++ basic recurrence eventId is " + eventId);

        // Check to see if we have the expected number of instances
        String timeZone = eventValues.getAsString(Events.EVENT_TIMEZONE);
        int instanceCount = getInstanceCount(timeZone, "2003-08-05T00:00:00",
                "2003-08-31T11:59:59", new long[] { calendarId });
        if (false) {
            Cursor instances = getInstances(timeZone, "2003-08-05T00:00:00", "2003-08-31T11:59:59",
                    new String[] { Instances.BEGIN }, new long[] { calendarId });
            dumpInstances(instances, timeZone, "initial");
            instances.close();
        }
        assertEquals("recurrence instance count", 4, instanceCount);

        // delete the calendar
        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Tests conversion of a regular event to a recurring event.
     */
    @MediumTest
    public void testConversionToRecurring() {
        String account = "reconv_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar and event
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        ContentValues eventValues = EventHelper
                .getNewEventValues(account, seed++, calendarId, true);
        long eventId = createAndVerifyEvent(account, seed, calendarId, true, eventValues);

        long dtstart = eventValues.getAsLong(Events.DTSTART);
        long dtend = eventValues.getAsLong(Events.DTEND);
        long durationSecs = (dtend - dtstart) / 1000;

        ContentValues updateValues = new ContentValues();
        updateValues.put(Events.RRULE, "FREQ=WEEKLY");   // recurs forever
        updateValues.put(Events.DURATION, "P" + durationSecs + "S");
        updateValues.putNull(Events.DTEND);

        // Issue update; do it as app instead of sync adapter to exercise that path.
        updateAndVerifyEvent(account, calendarId, eventId, false, updateValues);

        // Make sure LAST_DATE got nulled out by our infinitely repeating sequence.
        Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
        Cursor c = mContentResolver.query(eventUri, new String[] { Events.LAST_DATE },
                null, null, null);
        assertEquals(1, c.getCount());
        assertTrue(c.moveToFirst());
        assertNull(c.getString(0));
        c.close();

        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Tests creation of a recurring event with single-instance exceptions.
     */
    @MediumTest
    public void testSingleRecurrenceExceptions() {
        String account = "rex_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create recurring event.
        ContentValues eventValues = EventHelper.getNewRecurringEventValues(account, seed++,
                calendarId, true, "1999-03-28T09:00:00", "PT1H", "FREQ=WEEKLY;WKST=SU;COUNT=100");
        long eventId = createAndVerifyEvent(account, seed++, calendarId, true, eventValues);

        // Add some attendees and reminders.
        addAttendees(account, eventId, seed);
        addReminders(account, eventId, seed);

        // Select a period that gives us 5 instances.  We don't want this to straddle a DST
        // transition, because we expect the startMinute field to be the same for all
        // instances, and it's stored as minutes since midnight in the device's time zone.
        // Things won't be consistent if the event and the device have different ideas about DST.
        String timeZone = eventValues.getAsString(Events.EVENT_TIMEZONE);
        String testStart = "1999-04-18T00:00:00";
        String testEnd = "1999-05-16T23:59:59";
        String[] projection = { Instances.BEGIN, Instances.START_MINUTE, Instances.END_MINUTE };

        Cursor instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "initial");
        }

        assertEquals("initial recurrence instance count", 5, instances.getCount());

        /*
         * Advance the start time of a few instances, and verify.
         */

        // Leave first instance alone.
        instances.moveToPosition(1);

        long startMillis;
        ContentValues excepValues;

        // Advance the start time of the 2nd instance.
        startMillis = instances.getLong(0);
        excepValues = EventHelper.getNewExceptionValues(startMillis);
        excepValues.put(Events.DTSTART, startMillis + 3600*1000);
        long excepEventId2 = createAndVerifyException(account, eventId, excepValues, true);
        instances.moveToNext();

        // Advance the start time of the 3rd instance.
        startMillis = instances.getLong(0);
        excepValues = EventHelper.getNewExceptionValues(startMillis);
        excepValues.put(Events.DTSTART, startMillis + 3600*1000*2);
        long excepEventId3 = createAndVerifyException(account, eventId, excepValues, true);
        instances.moveToNext();

        // Cancel the 4th instance.
        startMillis = instances.getLong(0);
        excepValues = EventHelper.getNewExceptionValues(startMillis);
        excepValues.put(Events.STATUS, Events.STATUS_CANCELED);
        long excepEventId4 = createAndVerifyException(account, eventId, excepValues, true);
        instances.moveToNext();

        // TODO: try to modify a non-existent instance.

        instances.close();

        // TODO: compare Reminders, Attendees, ExtendedProperties on one of the exception events

        // Re-query the instances and figure out if they look right.
        instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "with DTSTART exceptions");
        }
        assertEquals("exceptional recurrence instance count", 4, instances.getCount());

        long prevMinute = -1;
        while (instances.moveToNext()) {
            // expect the start times for each entry to be different from the previous entry
            long startMinute = instances.getLong(1);
            assertTrue("instance start times are different", startMinute != prevMinute);

            prevMinute = startMinute;
        }
        instances.close();


        // Delete all of our exceptions, and verify.
        int deleteCount = 0;
        deleteCount += deleteException(account, eventId, excepEventId2);
        deleteCount += deleteException(account, eventId, excepEventId3);
        deleteCount += deleteException(account, eventId, excepEventId4);
        assertEquals("events deleted", 3, deleteCount);

        // Re-query the instances and figure out if they look right.
        instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "post exception deletion");
        }
        assertEquals("post-exception deletion instance count", 5, instances.getCount());

        prevMinute = -1;
        while (instances.moveToNext()) {
            // expect the start times for each entry to be the same
            long startMinute = instances.getLong(1);
            if (prevMinute != -1) {
                assertEquals("instance start times are the same", startMinute, prevMinute);
            }
            prevMinute = startMinute;
        }
        instances.close();

        /*
         * Repeat the test, this time modifying DURATION.
         */

        instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "initial");
        }

        assertEquals("initial recurrence instance count", 5, instances.getCount());

        // Leave first instance alone.
        instances.moveToPosition(1);

        // Advance the end time of the 2nd instance.
        startMillis = instances.getLong(0);
        excepValues = EventHelper.getNewExceptionValues(startMillis);
        excepValues.put(Events.DURATION, "P" + 3600*2 + "S");
        excepEventId2 = createAndVerifyException(account, eventId, excepValues, true);
        instances.moveToNext();

        // Advance the end time of the 3rd instance, and change the self-attendee status.
        startMillis = instances.getLong(0);
        excepValues = EventHelper.getNewExceptionValues(startMillis);
        excepValues.put(Events.DURATION, "P" + 3600*3 + "S");
        excepValues.put(Events.SELF_ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_DECLINED);
        excepEventId3 = createAndVerifyException(account, eventId, excepValues, true);
        instances.moveToNext();

        // Advance the start time of the 4th instance, which will also advance the end time.
        startMillis = instances.getLong(0);
        excepValues = EventHelper.getNewExceptionValues(startMillis);
        excepValues.put(Events.DTSTART, startMillis + 3600*1000);
        excepEventId4 = createAndVerifyException(account, eventId, excepValues, true);
        instances.moveToNext();

        instances.close();

        // TODO: make sure the selfAttendeeStatus change took

        // Re-query the instances and figure out if they look right.
        instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "with DURATION exceptions");
        }
        assertEquals("exceptional recurrence instance count", 5, instances.getCount());

        prevMinute = -1;
        while (instances.moveToNext()) {
            // expect the start times for each entry to be different from the previous entry
            long endMinute = instances.getLong(2);
            assertTrue("instance end times are different", endMinute != prevMinute);

            prevMinute = endMinute;
        }
        instances.close();

        // delete the calendar
        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Tests creation of a simple recurrence exception when not pretending to be the sync
     * adapter.  One significant consequence is that we don't set the _sync_id field in the
     * events, which affects how the provider correlates recurrences and exceptions.
     */
    @MediumTest
    public void testNonAdapterRecurrenceExceptions() {
        String account = "rena_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Generate recurring event, with "asSyncAdapter" set to false.
        ContentValues eventValues = EventHelper.getNewRecurringEventValues(account, seed++,
                calendarId, false, "1991-02-03T12:00:00", "PT1H", "FREQ=DAILY;WKST=SU;COUNT=10");

        // Select a period that gives us 3 instances.
        String timeZone = eventValues.getAsString(Events.EVENT_TIMEZONE);
        String testStart = "1991-02-03T00:00:00";
        String testEnd = "1991-02-05T23:59:59";
        String[] projection = { Instances.BEGIN, Instances.START_MINUTE };

        // Expand the bounds of the instances table so we expand future events as they are added.
        expandInstanceRange(account, calendarId, testStart, testEnd, timeZone);

        // Create the event in the database.
        long eventId = createAndVerifyEvent(account, seed++, calendarId, false, eventValues);
        assertTrue(eventId >= 0);

        // Add some attendees.
        addAttendees(account, eventId, seed);

        Cursor instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "initial");
        }
        assertEquals("initial recurrence instance count", 3, instances.getCount());

        /*
         * Alter the attendee status of the second event.  This should cause the instances to
         * be updated, replacing the previous 2nd instance with the exception instance.  If the
         * code is broken we'll see four instances (because the original instance didn't get
         * removed) or one instance (because the code correctly deleted all related events but
         * couldn't correlate the exception with its original recurrence).
         */

        // Leave first instance alone.
        instances.moveToPosition(1);

        long startMillis;
        ContentValues excepValues;

        // Advance the start time of the 2nd instance.
        startMillis = instances.getLong(0);
        excepValues = EventHelper.getNewExceptionValues(startMillis);
        excepValues.put(Events.SELF_ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_DECLINED);
        long excepEventId2 = createAndVerifyException(account, eventId, excepValues, false);
        instances.moveToNext();

        instances.close();

        // Re-query the instances and figure out if they look right.
        instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "with exceptions");
        }

        // TODO: this test currently fails due to limitations in the provider
        //assertEquals("exceptional recurrence instance count", 3, instances.getCount());

        instances.close();

        // delete the calendar
        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Tests insertion of event exceptions before and after a recurring event is created.
     * <p>
     * The server may send exceptions down before the event they refer to, so the provider
     * fills in the originalId of previously-existing exceptions when a recurring event is
     * inserted.  Make sure that works.
     * <p>
     * The _sync_id column is only unique with a given calendar.  We create events with
     * identical originalSyncId values in two different calendars to verify that the provider
     * doesn't update unrelated events.
     * <p>
     * We can't use the /exception URI, because that only works if the events are created
     * in order.
     */
    @MediumTest
    public void testOutOfOrderRecurrenceExceptions() {
        String account1 = "roid1_account";
        String account2 = "roid2_account";
        String startWhen = "1987-08-09T12:00:00";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account1);
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account2);

        // Create calendars
        long calendarId1 = createAndVerifyCalendar(account1, seed++, null);
        long calendarId2 = createAndVerifyCalendar(account2, seed++, null);


        // Generate base event.
        ContentValues recurEventValues = EventHelper.getNewRecurringEventValues(account1, seed++,
                calendarId1, true, startWhen, "PT1H", "FREQ=DAILY;WKST=SU;COUNT=10");

        // Select a period that gives us 3 instances.
        String timeZone = recurEventValues.getAsString(Events.EVENT_TIMEZONE);
        String testStart = "1987-08-09T00:00:00";
        String testEnd = "1987-08-11T23:59:59";
        String[] projection = { Instances.BEGIN, Instances.START_MINUTE, Instances.EVENT_ID };

        /*
         * We're interested in exploring what the instance expansion code does with the events
         * as they arrive.  It won't do anything at event-creation time unless the instance
         * range already covers the interesting set of dates, so we need to create and remove
         * an instance in the same time frame beforehand.
         */
        expandInstanceRange(account1, calendarId1, testStart, testEnd, timeZone);

        /*
         * Instances table should be expanded.  Do the test.
         */

        final String MAGIC_SYNC_ID = "MagicSyncId";
        recurEventValues.put(Events._SYNC_ID, MAGIC_SYNC_ID);

        // Generate exceptions from base, removing the generated _sync_id and setting the
        // base event's _sync_id as originalSyncId.
        ContentValues beforeExcepValues, afterExcepValues, unrelatedExcepValues;
        beforeExcepValues = new ContentValues(recurEventValues);
        afterExcepValues = new ContentValues(recurEventValues);
        unrelatedExcepValues = new ContentValues(recurEventValues);
        beforeExcepValues.remove(Events._SYNC_ID);
        afterExcepValues.remove(Events._SYNC_ID);
        unrelatedExcepValues.remove(Events._SYNC_ID);
        beforeExcepValues.put(Events.ORIGINAL_SYNC_ID, MAGIC_SYNC_ID);
        afterExcepValues.put(Events.ORIGINAL_SYNC_ID, MAGIC_SYNC_ID);
        unrelatedExcepValues.put(Events.ORIGINAL_SYNC_ID, MAGIC_SYNC_ID);

        // Disassociate the "unrelated" exception by moving it to the other calendar.
        unrelatedExcepValues.put(Events.CALENDAR_ID, calendarId2);

        // We shift the start time by half an hour, and use the same _sync_id.
        final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;
        final long ONE_HOUR_MILLIS  = 60 * 60 * 1000;
        final long HALF_HOUR_MILLIS  = 30 * 60 * 1000;
        long dtstartMillis = recurEventValues.getAsLong(Events.DTSTART) + ONE_DAY_MILLIS;
        beforeExcepValues.put(Events.ORIGINAL_INSTANCE_TIME, dtstartMillis);
        beforeExcepValues.put(Events.DTSTART, dtstartMillis + HALF_HOUR_MILLIS);
        beforeExcepValues.put(Events.DTEND, dtstartMillis + ONE_HOUR_MILLIS);
        beforeExcepValues.remove(Events.DURATION);
        beforeExcepValues.remove(Events.RRULE);
        beforeExcepValues.put(Events.ORIGINAL_SYNC_ID, MAGIC_SYNC_ID);
        dtstartMillis += ONE_DAY_MILLIS;
        afterExcepValues.put(Events.ORIGINAL_INSTANCE_TIME, dtstartMillis);
        afterExcepValues.put(Events.DTSTART, dtstartMillis + HALF_HOUR_MILLIS);
        afterExcepValues.put(Events.DTEND, dtstartMillis + ONE_HOUR_MILLIS);
        afterExcepValues.remove(Events.DURATION);
        afterExcepValues.remove(Events.RRULE);
        afterExcepValues.put(Events.ORIGINAL_SYNC_ID, MAGIC_SYNC_ID);
        dtstartMillis += ONE_DAY_MILLIS;
        unrelatedExcepValues.put(Events.ORIGINAL_INSTANCE_TIME, dtstartMillis);
        unrelatedExcepValues.put(Events.DTSTART, dtstartMillis + HALF_HOUR_MILLIS);
        unrelatedExcepValues.put(Events.DTEND, dtstartMillis + ONE_HOUR_MILLIS);
        unrelatedExcepValues.remove(Events.DURATION);
        unrelatedExcepValues.remove(Events.RRULE);
        unrelatedExcepValues.put(Events.ORIGINAL_SYNC_ID, MAGIC_SYNC_ID);


        // Create "before" and "unrelated" exceptions.
        long beforeEventId = createAndVerifyEvent(account1, seed, calendarId1, true,
                beforeExcepValues);
        assertTrue(beforeEventId >= 0);
        long unrelatedEventId = createAndVerifyEvent(account2, seed, calendarId2, true,
                unrelatedExcepValues);
        assertTrue(unrelatedEventId >= 0);

        // Create recurring event.
        long recurEventId = createAndVerifyEvent(account1, seed, calendarId1, true,
                recurEventValues);
        assertTrue(recurEventId >= 0);

        // Create "after" exception.
        long afterEventId = createAndVerifyEvent(account1, seed, calendarId1, true,
                afterExcepValues);
        assertTrue(afterEventId >= 0);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "before=" + beforeEventId + ", unrel=" + unrelatedEventId +
                    ", recur=" + recurEventId + ", after=" + afterEventId);
        }

        // Check to see how many instances we get.  If the recurrence and the exception don't
        // get paired up correctly, we'll see too many instances.
        Cursor instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId1, calendarId2 });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "with exception");
        }

        assertEquals("initial recurrence instance count", 3, instances.getCount());

        instances.close();


        /*
         * Now we want to verify that:
         * - "before" and "after" have an originalId equal to our recurEventId
         * - "unrelated" has no originalId
         */
        Cursor c = null;
        try {
            final String[] PROJECTION = new String[] { Events.ORIGINAL_ID };
            Uri eventUri;
            Long originalId;

            eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, beforeEventId);
            c = mContentResolver.query(eventUri, PROJECTION, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToNext();
            originalId = c.getLong(0);
            assertNotNull(originalId);
            assertEquals(recurEventId, (long) originalId);
            c.close();

            eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, afterEventId);
            c = mContentResolver.query(eventUri, PROJECTION, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToNext();
            originalId = c.getLong(0);
            assertNotNull(originalId);
            assertEquals(recurEventId, (long) originalId);
            c.close();

            eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, unrelatedEventId);
            c = mContentResolver.query(eventUri, PROJECTION, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToNext();
            assertNull(c.getString(0));
            c.close();

            c = null;
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // delete the calendars
        removeAndVerifyCalendar(account1, calendarId1);
        removeAndVerifyCalendar(account2, calendarId2);
    }

    /**
     * Tests exceptions that modify all future instances of a recurring event.
     */
    @MediumTest
    public void testForwardRecurrenceExceptions() {
        String account = "refx_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create recurring event
        ContentValues eventValues = EventHelper.getNewRecurringEventValues(account, seed++,
                calendarId, true, "1999-01-01T06:00:00", "PT1H", "FREQ=WEEKLY;WKST=SU;COUNT=10");
        long eventId = createAndVerifyEvent(account, seed++, calendarId, true, eventValues);

        // Add some attendees and reminders.
        addAttendees(account, eventId, seed++);
        addReminders(account, eventId, seed++);

        // Get some instances.
        String timeZone = eventValues.getAsString(Events.EVENT_TIMEZONE);
        String testStart = "1999-01-01T00:00:00";
        String testEnd = "1999-01-29T23:59:59";
        String[] projection = { Instances.BEGIN, Instances.START_MINUTE };

        Cursor instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "initial");
        }

        assertEquals("initial recurrence instance count", 5, instances.getCount());

        // Modify starting from 3rd instance.
        instances.moveToPosition(2);

        long startMillis;
        ContentValues excepValues;

        // Replace with a new recurrence rule.  We move the start time an hour later, and cap
        // it at two instances.
        startMillis = instances.getLong(0);
        excepValues = EventHelper.getNewExceptionValues(startMillis);
        excepValues.put(Events.DTSTART, startMillis + 3600*1000);
        excepValues.put(Events.RRULE, "FREQ=WEEKLY;COUNT=2;WKST=SU");
        long excepEventId = createAndVerifyException(account, eventId, excepValues, true);
        instances.close();


        // Check to see if it took.
        instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "with new rule");
        }

        assertEquals("count with exception", 4, instances.getCount());

        long prevMinute = -1;
        for (int i = 0; i < 4; i++) {
            long startMinute;
            instances.moveToNext();
            switch (i) {
                case 0:
                    startMinute = instances.getLong(1);
                    break;
                case 1:
                case 3:
                    startMinute = instances.getLong(1);
                    assertEquals("first/last pairs match", prevMinute, startMinute);
                    break;
                case 2:
                    startMinute = instances.getLong(1);
                    assertFalse("first two != last two", prevMinute == startMinute);
                    break;
                default:
                    fail();
                    startMinute = -1;   // make compiler happy
                    break;
            }

            prevMinute = startMinute;
        }
        instances.close();

        // delete the calendar
        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Tests exceptions that modify all instances of a recurring event.  This is not really an
     * exception, since it won't create a new event, but supporting it allows us to use the
     * exception URI without having to determine whether the "start from here" instance is the
     * very first instance.
     */
    @MediumTest
    public void testFullRecurrenceUpdate() {
        String account = "ref_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create recurring event
        String rrule = "FREQ=DAILY;WKST=MO;COUNT=100";
        ContentValues eventValues = EventHelper.getNewRecurringEventValues(account, seed++,
                calendarId, true, "1997-08-29T02:14:00", "PT1H", rrule);
        long eventId = createAndVerifyEvent(account, seed++, calendarId, true, eventValues);
        //Log.i(TAG, "+++ eventId is " + eventId);

        // Get some instances.
        String timeZone = eventValues.getAsString(Events.EVENT_TIMEZONE);
        String testStart = "1997-08-01T00:00:00";
        String testEnd = "1997-08-31T23:59:59";
        String[] projection = { Instances.BEGIN, Instances.EVENT_LOCATION };
        String newLocation = "NEW!";

        Cursor instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "initial");
        }

        assertEquals("initial recurrence instance count", 3, instances.getCount());

        instances.moveToFirst();
        long startMillis = instances.getLong(0);
        ContentValues excepValues = EventHelper.getNewExceptionValues(startMillis);
        excepValues.put(Events.RRULE, rrule);   // identifies this as an "all future events" excep
        excepValues.put(Events.EVENT_LOCATION, newLocation);
        long excepEventId = createAndVerifyException(account, eventId, excepValues, true);
        instances.close();

        // Check results.
        assertEquals("full update does not create new ID", eventId, excepEventId);

        instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        assertEquals("post-update instance count", 3, instances.getCount());
        while (instances.moveToNext()) {
            assertEquals("new location", newLocation, instances.getString(1));
        }
        instances.close();

        // delete the calendar
        removeAndVerifyCalendar(account, calendarId);
    }

    @MediumTest
    public void testMultiRuleRecurrence() {
        String account = "multirule_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create recurring event
        String rrule = "FREQ=DAILY;WKST=MO;COUNT=5\nFREQ=WEEKLY;WKST=SU;COUNT=5";
        ContentValues eventValues = EventHelper.getNewRecurringEventValues(account, seed++,
                calendarId, true, "1997-08-29T02:14:00", "PT1H", rrule);
        long eventId = createAndVerifyEvent(account, seed++, calendarId, true, eventValues);

        // TODO: once multi-rule RRULEs are fully supported, verify that they work

        // delete the calendar
        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Issue bad requests and expect them to get rejected.
     */
    @MediumTest
    public void testBadRequests() {
        String account = "neg_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        // Create calendar
        long calendarId = createAndVerifyCalendar(account, seed++, null);

        // Create recurring event
        String rrule = "FREQ=OFTEN;WKST=MO";
        ContentValues eventValues = EventHelper.getNewRecurringEventValues(account, seed++,
                calendarId, true, "1997-08-29T02:14:00", "PT1H", rrule);
        try {
            createAndVerifyEvent(account, seed++, calendarId, true, eventValues);
            fail("Bad recurrence rule should have been rejected");
        } catch (IllegalArgumentException iae) {
            // good
        }

        // delete the calendar
        removeAndVerifyCalendar(account, calendarId);
    }

    /**
     * Tests correct behavior of Calendars.isPrimary column
     */
    @MediumTest
    public void testCalendarIsPrimary() {
        String account = "ec_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        int isPrimary;
        Cursor cursor;
        ContentValues values = new ContentValues();

        final long calendarId = createAndVerifyCalendar(account, seed++, null);
        final Uri uri = ContentUris.withAppendedId(Calendars.CONTENT_URI, calendarId);

        // verify when ownerAccount != account_name && isPrimary IS NULL
        cursor = mContentResolver.query(uri, new String[]{Calendars.IS_PRIMARY}, null, null, null);
        cursor.moveToFirst();
        isPrimary = cursor.getInt(0);
        cursor.close();
        assertEquals("isPrimary should be 0 if ownerAccount != account_name", 0, isPrimary);

        // verify when ownerAccount == account_name && isPrimary IS NULL
        values.clear();
        values.put(Calendars.OWNER_ACCOUNT, account);
        mContentResolver.update(asSyncAdapter(uri, account, CTS_TEST_TYPE), values, null, null);
        cursor = mContentResolver.query(uri, new String[]{Calendars.IS_PRIMARY}, null, null, null);
        cursor.moveToFirst();
        isPrimary = cursor.getInt(0);
        cursor.close();
        assertEquals("isPrimary should be 1 if ownerAccount == account_name", 1, isPrimary);

        // verify isPrimary IS NOT NULL
        values.clear();
        values.put(Calendars.IS_PRIMARY, SOME_ARBITRARY_INT);
        mContentResolver.update(uri, values, null, null);
        cursor = mContentResolver.query(uri, new String[]{Calendars.IS_PRIMARY}, null, null, null);
        cursor.moveToFirst();
        isPrimary = cursor.getInt(0);
        cursor.close();
        assertEquals("isPrimary should be the value it was set to", SOME_ARBITRARY_INT, isPrimary);

        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);
    }

    /**
     * Tests correct behavior of Events.isOrganizer column
     */
    @MediumTest
    public void testEventsIsOrganizer() {
        String account = "ec_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        int isOrganizer;
        Cursor cursor;
        ContentValues values = new ContentValues();

        final long calendarId = createAndVerifyCalendar(account, seed++, null);
        final long eventId = createAndVerifyEvent(account, seed, calendarId, true, null);
        final Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);

        // verify when ownerAccount != organizer && isOrganizer IS NULL
        cursor = mContentResolver.query(uri, new String[]{Events.IS_ORGANIZER}, null, null, null);
        cursor.moveToFirst();
        isOrganizer = cursor.getInt(0);
        cursor.close();
        assertEquals("isOrganizer should be 0 if ownerAccount != organizer", 0, isOrganizer);

        // verify when ownerAccount == account_name && isOrganizer IS NULL
        values.clear();
        values.put(Events.ORGANIZER, CalendarHelper.generateCalendarOwnerEmail(account));
        mContentResolver.update(asSyncAdapter(uri, account, CTS_TEST_TYPE), values, null, null);
        cursor = mContentResolver.query(uri, new String[]{Events.IS_ORGANIZER}, null, null, null);
        cursor.moveToFirst();
        isOrganizer = cursor.getInt(0);
        cursor.close();
        assertEquals("isOrganizer should be 1 if ownerAccount == organizer", 1, isOrganizer);

        // verify isOrganizer IS NOT NULL
        values.clear();
        values.put(Events.IS_ORGANIZER, SOME_ARBITRARY_INT);
        mContentResolver.update(uri, values, null, null);
        cursor = mContentResolver.query(uri, new String[]{Events.IS_ORGANIZER}, null, null, null);
        cursor.moveToFirst();
        isOrganizer = cursor.getInt(0);
        cursor.close();
        assertEquals(
                "isPrimary should be the value it was set to", SOME_ARBITRARY_INT, isOrganizer);
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);
    }

    /**
     * Tests correct behavior of Events.uid2445 column
     */
    @MediumTest
    public void testEventsUid2445() {
        String account = "ec_account";
        int seed = 0;

        // Clean up just in case
        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);

        final String uid = "uid_123";
        Cursor cursor;
        ContentValues values = new ContentValues();
        final long calendarId = createAndVerifyCalendar(account, seed++, null);
        final long eventId = createAndVerifyEvent(account, seed, calendarId, true, null);
        final Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);

        // Verify default is null
        cursor = mContentResolver.query(uri, new String[] {Events.UID_2445}, null, null, null);
        cursor.moveToFirst();
        assertTrue(cursor.isNull(0));
        cursor.close();

        // Write column value and read back
        values.clear();
        values.put(Events.UID_2445, uid);
        mContentResolver.update(asSyncAdapter(uri, account, CTS_TEST_TYPE), values, null, null);
        cursor = mContentResolver.query(uri, new String[] {Events.UID_2445}, null, null, null);
        cursor.moveToFirst();
        assertFalse(cursor.isNull(0));
        assertEquals("Column uid_2445 has unexpected value.", uid, cursor.getString(0));

        CalendarHelper.deleteCalendarByAccount(mContentResolver, account);
    }

    /**
     * Acquires the set of instances that appear between the specified start and end points.
     *
     * @param timeZone Time zone to use when parsing startWhen and endWhen
     * @param startWhen Start date/time, in RFC 3339 format
     * @param endWhen End date/time, in RFC 3339 format
     * @param projection Array of desired column names
     * @return Cursor with instances (caller should close when done)
     */
    private Cursor getInstances(String timeZone, String startWhen, String endWhen,
            String[] projection, long[] calendarIds) {
        Time startTime = new Time(timeZone);
        startTime.parse3339(startWhen);
        long startMillis = startTime.toMillis(false);

        Time endTime = new Time(timeZone);
        endTime.parse3339(endWhen);
        long endMillis = endTime.toMillis(false);

        // We want a list of instances that occur between the specified dates.  Use the
        // "instances/when" URI.
        Uri uri = Uri.withAppendedPath(CalendarContract.Instances.CONTENT_URI,
                startMillis + "/" + endMillis);

        String where = null;
        for (int i = 0; i < calendarIds.length; i++) {
            if (i > 0) {
                where += " OR ";
            } else {
                where = "";
            }
            where += (Instances.CALENDAR_ID + "=" + calendarIds[i]);
        }
        Cursor instances = mContentResolver.query(uri, projection, where, null,
                projection[0] + " ASC");

        return instances;
    }

    /**
     * Acquires the set of instances that appear between the specified start and end points
     * that match the search terms.
     *
     * @param timeZone Time zone to use when parsing startWhen and endWhen
     * @param startWhen Start date/time, in RFC 3339 format
     * @param endWhen End date/time, in RFC 3339 format
     * @param search A collection of tokens to search for.  The columns searched are
     *   hard-coded in the provider (currently title, description, location, attendee
     *   name, attendee email).
     * @param searchByDay If set, adjust start/end to calendar day boundaries.
     * @param projection Array of desired column names
     * @return Cursor with instances (caller should close when done)
     */
    private Cursor getInstancesSearch(String timeZone, String startWhen, String endWhen,
            String search, boolean searchByDay, String[] projection, long[] calendarIds) {
        Time startTime = new Time(timeZone);
        startTime.parse3339(startWhen);
        long startMillis = startTime.toMillis(false);

        Time endTime = new Time(timeZone);
        endTime.parse3339(endWhen);
        long endMillis = endTime.toMillis(false);

        Uri uri;
        if (searchByDay) {
            // start/end are Julian day numbers rather than time in milliseconds
            int julianStart = Time.getJulianDay(startMillis, startTime.gmtoff);
            int julianEnd = Time.getJulianDay(endMillis, endTime.gmtoff);
            uri = Uri.withAppendedPath(CalendarContract.Instances.CONTENT_SEARCH_BY_DAY_URI,
                    julianStart + "/" + julianEnd + "/" + search);
        } else {
            uri = Uri.withAppendedPath(CalendarContract.Instances.CONTENT_SEARCH_URI,
                    startMillis + "/" + endMillis + "/" + search);
        }

        String where = null;
        for (int i = 0; i < calendarIds.length; i++) {
            if (i > 0) {
                where += " OR ";
            } else {
                where = "";
            }
            where += (Instances.CALENDAR_ID + "=" + calendarIds[i]);
        }
        // We want a list of instances that occur between the specified dates and that match
        // the search terms.

        Cursor instances = mContentResolver.query(uri, projection, where, null,
                projection[0] + " ASC");

        return instances;
    }

    /** debug -- dump instances cursor */
    private static void dumpInstances(Cursor instances, String timeZone, String msg) {
        Log.d(TAG, "Instances (" + msg + ")");

        int posn = instances.getPosition();
        instances.moveToPosition(-1);

        //Log.d(TAG, "+++ instances has " + instances.getCount() + " rows, " +
        //        instances.getColumnCount() + " columns");
        while (instances.moveToNext()) {
            long beginMil = instances.getLong(0);
            Time beginT = new Time(timeZone);
            beginT.set(beginMil);
            String logMsg = "--> begin=" + beginT.format3339(false) + " (" + beginMil + ")";
            for (int i = 2; i < instances.getColumnCount(); i++) {
                logMsg += " [" + instances.getString(i) + "]";
            }
            Log.d(TAG, logMsg);
        }
        instances.moveToPosition(posn);
    }


    /**
     * Counts the number of instances that appear between the specified start and end times.
     */
    private int getInstanceCount(String timeZone, String startWhen, String endWhen,
                long[] calendarIds) {
        Cursor instances = getInstances(timeZone, startWhen, endWhen,
                new String[] { Instances._ID }, calendarIds);
        int count = instances.getCount();
        instances.close();
        return count;
    }

    /**
     * Deletes an event as app and sync adapter which removes it from the db and
     * verifies after each.
     *
     * @param eventUri The uri for the event to delete
     * @param accountName TODO
     */
    private void removeAndVerifyEvent(Uri eventUri, ContentValues eventValues, String accountName) {
        // Delete event
        EventHelper.deleteEvent(mContentResolver, eventUri, eventValues);
        // Verify
        verifyEvent(eventValues, ContentUris.parseId(eventUri));
        // Delete as sync adapter
        assertEquals(1,
                EventHelper.deleteEventAsSyncAdapter(mContentResolver, eventUri, accountName));
        // Verify
        Cursor c = EventHelper.getEventByUri(mContentResolver, eventUri);
        assertEquals(0, c.getCount());
        c.close();
    }

    /**
     * Creates an event on the given calendar and verifies it.
     *
     * @param account
     * @param seed
     * @param calendarId
     * @param asSyncAdapter
     * @param values optional pre created set of values; will have several new entries added
     * @return the _id for the new event
     */
    private long createAndVerifyEvent(String account, int seed, long calendarId,
            boolean asSyncAdapter, ContentValues values) {
        // Create an event
        if (values == null) {
            values = EventHelper.getNewEventValues(account, seed, calendarId, asSyncAdapter);
        }
        Uri insertUri = Events.CONTENT_URI;
        if (asSyncAdapter) {
            insertUri = asSyncAdapter(insertUri, account, CTS_TEST_TYPE);
        }
        Uri uri = mContentResolver.insert(insertUri, values);
        assertNotNull(uri);

        // Verify
        EventHelper.addDefaultReadOnlyValues(values, account, asSyncAdapter);
        long eventId = ContentUris.parseId(uri);
        assertTrue(eventId >= 0);

        verifyEvent(values, eventId);
        return eventId;
    }

    /**
     * Updates an event, and verifies that the updates took.
     */
    private void updateAndVerifyEvent(String account, long calendarId, long eventId,
            boolean asSyncAdapter, ContentValues updateValues) {
        Uri uri = Uri.withAppendedPath(Events.CONTENT_URI, String.valueOf(eventId));
        if (asSyncAdapter) {
            uri = asSyncAdapter(uri, account, CTS_TEST_TYPE);
        }
        int count = mContentResolver.update(uri, updateValues, null, null);

        // Verify
        assertEquals(1, count);
        verifyEvent(updateValues, eventId);
    }

    /**
     * Creates an exception to a recurring event, and verifies it.
     * @param account The account to use.
     * @param originalEventId The ID of the original event.
     * @param values Values for the exception; must include originalInstanceTime.
     * @return The _id for the new event.
     */
    private long createAndVerifyException(String account, long originalEventId,
            ContentValues values, boolean asSyncAdapter) {
        // Create the exception
        Uri uri = Uri.withAppendedPath(Events.CONTENT_EXCEPTION_URI,
                String.valueOf(originalEventId));
        if (asSyncAdapter) {
            uri = asSyncAdapter(uri, account, CTS_TEST_TYPE);
        }
        Uri resultUri = mContentResolver.insert(uri, values);
        assertNotNull(resultUri);
        long eventId = ContentUris.parseId(resultUri);
        assertTrue(eventId >= 0);
        return eventId;
    }

    /**
     * Deletes an exception to a recurring event.
     * @param account The account to use.
     * @param eventId The ID of the original recurring event.
     * @param excepId The ID of the exception event.
     * @return The number of rows deleted.
     */
    private int deleteException(String account, long eventId, long excepId) {
        Uri uri = Uri.withAppendedPath(Events.CONTENT_EXCEPTION_URI,
                eventId + "/" + excepId);
        uri = asSyncAdapter(uri, account, CTS_TEST_TYPE);
        return mContentResolver.delete(uri, null, null);
    }

    /**
     * Add some sample attendees to an event.
     */
    private void addAttendees(String account, long eventId, int seed) {
        assertTrue(eventId >= 0);
        AttendeeHelper.addAttendee(mContentResolver, eventId,
                "Attender" + seed,
                CalendarHelper.generateCalendarOwnerEmail(account),
                Attendees.ATTENDEE_STATUS_ACCEPTED,
                Attendees.RELATIONSHIP_ORGANIZER,
                Attendees.TYPE_NONE);
        seed++;

        AttendeeHelper.addAttendee(mContentResolver, eventId,
                "Attender" + seed,
                "attender" + seed + "@example.com",
                Attendees.ATTENDEE_STATUS_TENTATIVE,
                Attendees.RELATIONSHIP_NONE,
                Attendees.TYPE_NONE);
    }

    /**
     * Add some sample reminders to an event.
     */
    private void addReminders(String account, long eventId, int seed) {
        ReminderHelper.addReminder(mContentResolver, eventId, seed * 5, Reminders.METHOD_ALERT);
    }

    /**
     * Creates and removes an event that covers a specific range of dates.  Call this to
     * cause the provider to expand the CalendarMetaData min/max values to include the range.
     * Useful when you want to see the provider expand the instances as the events are added.
     */
    private void expandInstanceRange(String account, long calendarId, String testStart,
            String testEnd, String timeZone) {
        int seed = 0;

        // TODO: this should use an UNTIL rule based on testEnd, not a COUNT
        ContentValues eventValues = EventHelper.getNewRecurringEventValues(account, seed,
                calendarId, true, testStart, "PT1H", "FREQ=DAILY;WKST=SU;COUNT=100");

        /*
         * Some of the helper functions modify "eventValues", so we want to make sure we're
         * passing a copy of anything we want to re-use.
         */
        long eventId = createAndVerifyEvent(account, seed, calendarId, true,
                new ContentValues(eventValues));
        assertTrue(eventId >= 0);

        String[] projection = { Instances.BEGIN, Instances.START_MINUTE };
        Cursor instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "prep-create");
        }
        assertEquals("initial recurrence instance count", 3, instances.getCount());
        instances.close();

        Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
        removeAndVerifyEvent(eventUri, new ContentValues(eventValues), account);

        instances = getInstances(timeZone, testStart, testEnd, projection,
                new long[] { calendarId });
        if (DEBUG_RECURRENCE) {
            dumpInstances(instances, timeZone, "prep-clear");
        }
        assertEquals("initial recurrence instance count", 0, instances.getCount());
        instances.close();

    }

    /**
     * Inserts a new calendar with the given account and seed and verifies it.
     *
     * @param account The account to add the calendar to
     * @param seed A number to use to generate the values
     * @return the created calendar's id
     */
    private long createAndVerifyCalendar(String account, int seed, ContentValues values) {
        // Create a calendar
        if (values == null) {
            values = CalendarHelper.getNewCalendarValues(account, seed);
        }
        Uri syncUri = asSyncAdapter(Calendars.CONTENT_URI, account, CTS_TEST_TYPE);
        Uri uri = mContentResolver.insert(syncUri, values);
        long calendarId = ContentUris.parseId(uri);
        assertTrue(calendarId >= 0);

        verifyCalendar(account, values, calendarId, 1);
        return calendarId;
    }

    /**
     * Deletes a given calendar and verifies no calendars remain on that
     * account.
     *
     * @param account
     * @param id
     */
    private void removeAndVerifyCalendar(String account, long id) {
        // TODO Add code to delete as app and sync adapter and test both

        // Delete
        assertEquals(1, CalendarHelper.deleteCalendarById(mContentResolver, id));

        // Verify
        Cursor c = CalendarHelper.getCalendarsByAccount(mContentResolver, account);
        assertEquals(0, c.getCount());
        c.close();
    }

    /**
     * Check all the fields of a calendar contained in values + id.
     *
     * @param account the account of the calendar
     * @param values the values to check against the db
     * @param id the _id of the calendar
     * @param expectedCount the number of calendars expected on this account
     */
    private void verifyCalendar(String account, ContentValues values, long id, int expectedCount) {
        // Verify
        Cursor c = CalendarHelper.getCalendarsByAccount(mContentResolver, account);
        assertEquals(expectedCount, c.getCount());
        assertTrue(c.moveToFirst());
        while (c.getLong(0) != id) {
            assertTrue(c.moveToNext());
        }
        for (String key : values.keySet()) {
            int index = c.getColumnIndex(key);
            assertTrue("Key " + key + " not in projection", index >= 0);
            assertEquals(key, values.getAsString(key), c.getString(index));
        }
        c.close();
    }

    /**
     * Creates a new _sync_state entry and verifies the contents.
     */
    private long createAndVerifySyncState(String account, ContentValues values) {
        assertNotNull(values);
        Uri syncUri = asSyncAdapter(SyncState.CONTENT_URI, account, CTS_TEST_TYPE);
        Uri uri = mContentResolver.insert(syncUri, values);
        long syncStateId = ContentUris.parseId(uri);
        assertTrue(syncStateId >= 0);

        verifySyncState(account, values, syncStateId);
        return syncStateId;

    }

    /**
     * Removes the _sync_state entry with the specified id, then verifies that it's gone.
     */
    private void removeAndVerifySyncState(String account) {
        assertEquals(1, SyncStateHelper.deleteSyncStateByAccount(mContentResolver, account, true));

        // Verify
        Cursor c = SyncStateHelper.getSyncStateByAccount(mContentResolver, account);
        try {
            assertEquals(0, c.getCount());
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Check all the fields of a _sync_state entry contained in values + id. This assumes
     * a single _sync_state has been created on the given account.
     */
    private void verifySyncState(String account, ContentValues values, long id) {
        // Verify
        Cursor c = SyncStateHelper.getSyncStateByAccount(mContentResolver, account);
        try {
            assertEquals(1, c.getCount());
            assertTrue(c.moveToFirst());
            assertEquals(id, c.getLong(0));
            for (String key : values.keySet()) {
                int index = c.getColumnIndex(key);
                if (key.equals(SyncState.DATA)) {
                    // TODO: can't compare as string, so compare as byte[]
                } else {
                    assertEquals(key, values.getAsString(key), c.getString(index));
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }


    /**
     * Special version of the test runner that does some remote Emma coverage housekeeping.
     */
    public static class CalendarEmmaTestRunner extends InstrumentationCtsTestRunner {
        private static final Uri EMMA_CONTENT_URI =
            Uri.parse("content://" + CalendarContract.AUTHORITY + "/emma");
        private ContentResolver mContentResolver;

        @Override
        public void onStart() {
            mContentResolver = getTargetContext().getContentResolver();

            ContentValues values = new ContentValues();
            values.put("cmd", "start");
            mContentResolver.insert(EMMA_CONTENT_URI, values);

            super.onStart();
        }

        @Override
        public void finish(int resultCode, Bundle results) {
            ContentValues values = new ContentValues();
            values.put("cmd", "stop");
            values.put("outputFileName",
                    Environment.getExternalStorageDirectory() + "/calendar-provider.ec");
            mContentResolver.insert(EMMA_CONTENT_URI, values);
            super.finish(resultCode, results);
        }
    }
}
