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
 * limitations under the License.
 */

package android.text.format.cts;

import android.content.Context;
import android.test.AndroidTestCase;
import android.text.format.DateUtils;
import dalvik.annotation.KnownFailure;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtilsTest extends AndroidTestCase {

    private long mBaseTime;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        mBaseTime = System.currentTimeMillis();
    }

    public void testGetDayOfWeekString() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }

        assertEquals("Sunday",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, DateUtils.LENGTH_LONG));
        assertEquals("Sun",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, DateUtils.LENGTH_MEDIUM));
        assertEquals("Sun",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, DateUtils.LENGTH_SHORT));
        assertEquals("Sun",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, DateUtils.LENGTH_SHORTER));
        assertEquals("S",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, DateUtils.LENGTH_SHORTEST));
        // Other abbrev
        assertEquals("Sun",
                DateUtils.getDayOfWeekString(Calendar.SUNDAY, 60));
    }

    public void testGetMonthString() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }
        assertEquals("January", DateUtils.getMonthString(Calendar.JANUARY, DateUtils.LENGTH_LONG));
        assertEquals("Jan",
                DateUtils.getMonthString(Calendar.JANUARY, DateUtils.LENGTH_MEDIUM));
        assertEquals("Jan", DateUtils.getMonthString(Calendar.JANUARY, DateUtils.LENGTH_SHORT));
        assertEquals("Jan",
                DateUtils.getMonthString(Calendar.JANUARY, DateUtils.LENGTH_SHORTER));
        assertEquals("J",
                DateUtils.getMonthString(Calendar.JANUARY, DateUtils.LENGTH_SHORTEST));
        // Other abbrev
        assertEquals("Jan", DateUtils.getMonthString(Calendar.JANUARY, 60));
    }

    public void testGetAMPMString() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }
        assertEquals("AM", DateUtils.getAMPMString(Calendar.AM));
        assertEquals("PM", DateUtils.getAMPMString(Calendar.PM));
    }

    public void test_getRelativeTimeSpanString() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }

        final long ONE_SECOND_IN_MS = 1000;
        assertEquals("0 minutes ago",
                     DateUtils.getRelativeTimeSpanString(mBaseTime - ONE_SECOND_IN_MS));
        assertEquals("in 0 minutes",
                     DateUtils.getRelativeTimeSpanString(mBaseTime + ONE_SECOND_IN_MS));

        final long ONE_MINUTE_IN_MS = 60 * ONE_SECOND_IN_MS;
        assertEquals("1 minute ago",
                     DateUtils.getRelativeTimeSpanString(0, ONE_MINUTE_IN_MS, DateUtils.MINUTE_IN_MILLIS));
        assertEquals("in 1 minute",
                     DateUtils.getRelativeTimeSpanString(ONE_MINUTE_IN_MS, 0, DateUtils.MINUTE_IN_MILLIS));

        assertEquals("42 minutes ago",
                     DateUtils.getRelativeTimeSpanString(mBaseTime - (42 * ONE_MINUTE_IN_MS),
                                                         mBaseTime, DateUtils.MINUTE_IN_MILLIS));
        assertEquals("in 42 minutes",
                     DateUtils.getRelativeTimeSpanString(mBaseTime + (42 * ONE_MINUTE_IN_MS),
                                                         mBaseTime, DateUtils.MINUTE_IN_MILLIS));

        final long ONE_HOUR_IN_MS = 60 * 60 * 1000;
        final long TWO_HOURS_IN_MS = 2 * ONE_HOUR_IN_MS;
        assertEquals("2 hours ago", DateUtils.getRelativeTimeSpanString(mBaseTime - TWO_HOURS_IN_MS,
                mBaseTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_NUMERIC_DATE));
        assertEquals("in 2 hours", DateUtils.getRelativeTimeSpanString(mBaseTime + TWO_HOURS_IN_MS,
                mBaseTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_NUMERIC_DATE));

        assertEquals("in 42 mins", DateUtils.getRelativeTimeSpanString(mBaseTime + (42 * ONE_MINUTE_IN_MS),
                mBaseTime, DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE));

        final long ONE_DAY_IN_MS = 24 * ONE_HOUR_IN_MS;
        assertEquals("Tomorrow",
                     DateUtils.getRelativeTimeSpanString(ONE_DAY_IN_MS, 0, DateUtils.DAY_IN_MILLIS, 0));
        assertEquals("in 2 days",
                     DateUtils.getRelativeTimeSpanString(2 * ONE_DAY_IN_MS, 0, DateUtils.DAY_IN_MILLIS, 0));
        assertEquals("Yesterday",
                     DateUtils.getRelativeTimeSpanString(0, ONE_DAY_IN_MS, DateUtils.DAY_IN_MILLIS, 0));
        assertEquals("2 days ago",
                     DateUtils.getRelativeTimeSpanString(0, 2 * ONE_DAY_IN_MS, DateUtils.DAY_IN_MILLIS, 0));

        final long DAY_DURATION = 5 * 24 * 60 * 60 * 1000;
        assertNotNull(DateUtils.getRelativeTimeSpanString(mContext, mBaseTime - DAY_DURATION, true));
        assertNotNull(DateUtils.getRelativeTimeSpanString(mContext, mBaseTime - DAY_DURATION));
    }

    public void test_getRelativeDateTimeString() {
        final long DAY_DURATION = 5 * 24 * 60 * 60 * 1000;
        assertNotNull(DateUtils.getRelativeDateTimeString(mContext,
                                                          mBaseTime - DAY_DURATION,
                                                          DateUtils.MINUTE_IN_MILLIS,
                                                          DateUtils.DAY_IN_MILLIS,
                                                          DateUtils.FORMAT_NUMERIC_DATE));
    }

    public void test_formatElapsedTime() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }

        long MINUTES = 60;
        long HOURS = 60 * MINUTES;
        test_formatElapsedTime("02:01", 2 * MINUTES + 1);
        test_formatElapsedTime("3:02:01", 3 * HOURS + 2 * MINUTES + 1);
        // http://code.google.com/p/android/issues/detail?id=41401
        test_formatElapsedTime("123:02:01", 123 * HOURS + 2 * MINUTES + 1);
    }

    private void test_formatElapsedTime(String expected, long elapsedTime) {
        assertEquals(expected, DateUtils.formatElapsedTime(elapsedTime));
        StringBuilder sb = new StringBuilder();
        assertEquals(expected, DateUtils.formatElapsedTime(sb, elapsedTime));
        assertEquals(expected, sb.toString());
    }

    public void testFormatSameDayTime() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }

        Date date = new Date(109, 0, 19, 3, 30, 15);
        long fixedTime = date.getTime();

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        Date dateWithCurrentYear = new Date(currentYear - 1900, 0, 19, 3, 30, 15);
        long timeWithCurrentYear = dateWithCurrentYear.getTime();

        final long DAY_DURATION = 5 * 24 * 60 * 60 * 1000;
        assertEquals("Saturday, January 24, 2009", DateUtils.formatSameDayTime(
                fixedTime + DAY_DURATION, fixedTime, java.text.DateFormat.FULL,
                java.text.DateFormat.FULL));
        assertEquals("Jan 24, 2009", DateUtils.formatSameDayTime(fixedTime + DAY_DURATION,
                fixedTime, java.text.DateFormat.DEFAULT, java.text.DateFormat.FULL));
        assertEquals("January 24, 2009", DateUtils.formatSameDayTime(fixedTime + DAY_DURATION,
                fixedTime, java.text.DateFormat.LONG, java.text.DateFormat.FULL));
        assertEquals("Jan 24, 2009", DateUtils.formatSameDayTime(fixedTime + DAY_DURATION,
                fixedTime, java.text.DateFormat.MEDIUM, java.text.DateFormat.FULL));
        assertEquals("1/24/09", DateUtils.formatSameDayTime(fixedTime + DAY_DURATION,
                fixedTime, java.text.DateFormat.SHORT, java.text.DateFormat.FULL));

        final long HOUR_DURATION = 2 * 60 * 60 * 1000;
        assertEquals("5:30:15 AM GMT+00:00", DateUtils.formatSameDayTime(fixedTime + HOUR_DURATION,
                fixedTime, java.text.DateFormat.FULL, java.text.DateFormat.FULL));
        assertEquals("5:30:15 AM", DateUtils.formatSameDayTime(fixedTime + HOUR_DURATION,
                fixedTime, java.text.DateFormat.FULL, java.text.DateFormat.DEFAULT));
        assertEquals("5:30:15 AM GMT+00:00", DateUtils.formatSameDayTime(fixedTime + HOUR_DURATION,
                fixedTime, java.text.DateFormat.FULL, java.text.DateFormat.LONG));
        assertEquals("5:30:15 AM", DateUtils.formatSameDayTime(fixedTime + HOUR_DURATION,
                fixedTime, java.text.DateFormat.FULL, java.text.DateFormat.MEDIUM));
        assertEquals("5:30 AM", DateUtils.formatSameDayTime(fixedTime + HOUR_DURATION,
                fixedTime, java.text.DateFormat.FULL, java.text.DateFormat.SHORT));
    }

    // This is just to exercise the wrapper that calls the libcore/icu4c implementation.
    // Full testing, in multiple locales, is in libcore's CTS tests.
    public void testFormatDateRange() {
        if (!LocaleUtils.isCurrentLocale(mContext, Locale.US)) {
            return;
        }

        Date date = new Date(109, 0, 19, 3, 30, 15);
        long fixedTime = date.getTime();
        final long HOUR_DURATION = 2 * 60 * 60 * 1000;
        assertEquals("Monday", DateUtils.formatDateRange(mContext, fixedTime,
                     fixedTime + HOUR_DURATION, DateUtils.FORMAT_SHOW_WEEKDAY));
    }

    public void testIsToday() {
        final long ONE_DAY_IN_MS = 24 * 60 * 60 * 1000;
        assertTrue(DateUtils.isToday(mBaseTime));
        assertFalse(DateUtils.isToday(mBaseTime - ONE_DAY_IN_MS));
    }

    public void test_bug_7548161() {
        long now = System.currentTimeMillis();
        long today = now;
        long tomorrow = now + DateUtils.DAY_IN_MILLIS;
        long yesterday = now - DateUtils.DAY_IN_MILLIS;
        assertEquals("Tomorrow", DateUtils.getRelativeTimeSpanString(tomorrow, now,
                                                                     DateUtils.DAY_IN_MILLIS, 0));
        assertEquals("Yesterday", DateUtils.getRelativeTimeSpanString(yesterday, now,
                                                                      DateUtils.DAY_IN_MILLIS, 0));
        assertEquals("Today", DateUtils.getRelativeTimeSpanString(today, now,
                                                                  DateUtils.DAY_IN_MILLIS, 0));
    }
}
