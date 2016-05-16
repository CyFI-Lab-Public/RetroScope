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

import java.lang.Math;
import java.util.Calendar;
import java.util.TimeZone;

import android.test.AndroidTestCase;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

public class TimeTest extends AndroidTestCase {
    private static final String TAG = "TimeTest";
    public void testConstructor() {
        Time time = new Time();
        new Time(Time.getCurrentTimezone());
        time.set(System.currentTimeMillis());
        Time anotherTime = new Time(time);
        assertTime(time, anotherTime);
    }

    public void testNormalize() {
        final int expectedMonth = 3;
        final int expectedDate = 1;
        Time time = new Time();
        // set date to March 32.
        time.year = 2008;
        time.month = 2;
        time.monthDay = 32;
        long timeMilliseconds = time.normalize(false);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMilliseconds);
        assertEquals(2008, cal.get(Calendar.YEAR));
        assertEquals(3, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(expectedMonth, time.month);
        assertEquals(expectedDate, time.monthDay);

        // reset date to March 32.
        time.month = 2;
        time.monthDay = 32;
        assertEquals(timeMilliseconds, time.normalize(true));
        assertEquals(expectedMonth, time.month);
        assertEquals(expectedDate, time.monthDay);
    }

    public void testSwitchTimezone() {
        String timeZone = "US/Pacific";
        String anotherTimeZone = "Asia/Chongqing";
        Time time = new Time(timeZone);
        assertEquals(timeZone, time.timezone);
        time.switchTimezone(anotherTimeZone);
        assertEquals(anotherTimeZone, time.timezone);
    }

    public void testSet() {
        final int year = 2008;
        final int month = 5;
        final int date = 10;
        Time time = new Time();
        time.set(date, month, year);
        assertEquals(year, time.year);
        assertEquals(month, time.month);
        assertEquals(date, time.monthDay);

        Time anotherTime = new Time();
        anotherTime.set(time);
        assertTime(time, anotherTime);
    }

    private void assertTime(Time time, Time anotherTime) {
        assertEquals(time.timezone, anotherTime.timezone);
        assertEquals(time.allDay, anotherTime.allDay);
        assertEquals(time.second, anotherTime.second);
        assertEquals(time.minute, anotherTime.minute);
        assertEquals(time.hour, anotherTime.hour);
        assertEquals(time.monthDay, anotherTime.monthDay);
        assertEquals(time.month, anotherTime.month);
        assertEquals(time.year, anotherTime.year);
        assertEquals(time.weekDay, anotherTime.weekDay);
        assertEquals(time.yearDay, anotherTime.yearDay);
        assertEquals(time.isDst, anotherTime.isDst);
        assertEquals(time.gmtoff, anotherTime.gmtoff);
    }

    public void testGetWeekNumber() {
        Time time = new Time();
        time.normalize(false);
        time.set(12, 10, 2008);
        assertEquals(45, time.getWeekNumber());

        assertEquals("20081112", time.format2445());

        assertEquals("2008-11-12", time.format3339(true));
        assertEquals("2008-11-12T00:00:00.000+00:00", time.format3339(false));

        Time anotherTime = new Time();
        assertTrue(anotherTime.parse3339("2008-11-12T00:00:00.000+00:00"));
        assertEquals(time.year, anotherTime.year);
        assertEquals(time.month, anotherTime.month);
        assertEquals(time.monthDay, anotherTime.monthDay);
        assertEquals(Time.TIMEZONE_UTC, anotherTime.timezone);

        try {
            anotherTime.parse3339("2008-111-12T00:00:00.000+00:00");
            fail("should throw exception");
        } catch (TimeFormatException e) {
            // expected
        }
    }

    public void testParseNull() {
        Time t = new Time();
        try {
            t.parse(null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            t.parse3339(null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    // http://code.google.com/p/android/issues/detail?id=16002
    // We'd leak one JNI global reference each time parsing failed.
    // This would cause a crash when we filled the global reference table.
    public void testBug16002() {
        Time t = new Time();
        for (int i = 0; i < 8192; ++i) {
            try {
                t.parse3339("xxx");
                fail();
            } catch (TimeFormatException expected) {
            }
        }
    }

    // http://code.google.com/p/android/issues/detail?id=22225
    // We'd leak one JNI global reference each time parsing failed.
    // This would cause a crash when we filled the global reference table.
    public void testBug22225() {
        Time t = new Time();
        for (int i = 0; i < 8192; ++i) {
            try {
                t.parse("xxx");
                fail();
            } catch (TimeFormatException expected) {
            }
        }
    }

    public void testIsEpoch() {
        Time time = new Time();
        assertTrue(Time.isEpoch(time));
        time.set(1, 2, 1970);
        time.normalize(false);
        assertFalse(Time.isEpoch(time));
    }

    public void testAfterBefore() {
        Time a = new Time(Time.TIMEZONE_UTC);
        Time b = new Time("America/Los_Angeles");
        assertFalse(a.after(b));
        assertTrue(b.after(a));
        assertFalse(b.before(a));
        assertTrue(a.before(b));
    }

    // Below are tests merged from Google
    private static class DateTest {
        public int year1;
        public int month1;
        public int day1;
        public int hour1;
        public int minute1;
        public int dst1;

        public int offset;

        public int year2;
        public int month2;
        public int day2;
        public int hour2;
        public int minute2;
        public int dst2;

        public DateTest(int year1, int month1, int day1, int hour1, int minute1, int dst1,
                int offset, int year2, int month2, int day2, int hour2, int minute2,
                int dst2) {
            this.year1 = year1;
            this.month1 = month1;
            this.day1 = day1;
            this.hour1 = hour1;
            this.minute1 = minute1;
            this.dst1 = dst1;
            this.offset = offset;
            this.year2 = year2;
            this.month2 = month2;
            this.day2 = day2;
            this.hour2 = hour2;
            this.minute2 = minute2;
            this.dst2 = dst2;
        }

        public DateTest(int year1, int month1, int day1, int hour1, int minute1,
                int offset, int year2, int month2, int day2, int hour2, int minute2) {
            this.year1 = year1;
            this.month1 = month1;
            this.day1 = day1;
            this.hour1 = hour1;
            this.minute1 = minute1;
            this.dst1 = -1;
            this.offset = offset;
            this.year2 = year2;
            this.month2 = month2;
            this.day2 = day2;
            this.hour2 = hour2;
            this.minute2 = minute2;
            this.dst2 = -1;
        }
    }

    // These tests assume that DST changes on Nov 4, 2007 at 2am (to 1am).

    // The "offset" field in "dayTests" represents days.
    // Use normalize(true) with these tests to change the date by 1 day.
    private DateTest[] dayTests = {
            // The month numbers are 0-relative, so Jan=0, Feb=1,...Dec=11

            // Nov 4, 12am + 0 day = Nov 4, 12am
            // Nov 5, 12am + 0 day = Nov 5, 12am
            new DateTest(2007, 10, 4, 0, 0, 0, 2007, 10, 4, 0, 0),
            new DateTest(2007, 10, 5, 0, 0, 0, 2007, 10, 5, 0, 0),

            // Nov 3, 12am + 1 day = Nov 4, 12am
            // Nov 4, 12am + 1 day = Nov 5, 12am
            // Nov 5, 12am + 1 day = Nov 6, 12am
            new DateTest(2007, 10, 3, 0, 0, 1, 2007, 10, 4, 0, 0),
            new DateTest(2007, 10, 4, 0, 0, 1, 2007, 10, 5, 0, 0),
            new DateTest(2007, 10, 5, 0, 0, 1, 2007, 10, 6, 0, 0),

            // Nov 3, 1am + 1 day = Nov 4, 1am
            // Nov 4, 1am + 1 day = Nov 5, 1am
            // Nov 5, 1am + 1 day = Nov 6, 1am
            new DateTest(2007, 10, 3, 1, 0, 1, 2007, 10, 4, 1, 0),
            new DateTest(2007, 10, 4, 1, 0, 1, 2007, 10, 5, 1, 0),
            new DateTest(2007, 10, 5, 1, 0, 1, 2007, 10, 6, 1, 0),

            // Nov 3, 2am + 1 day = Nov 4, 2am
            // Nov 4, 2am + 1 day = Nov 5, 2am
            // Nov 5, 2am + 1 day = Nov 6, 2am
            new DateTest(2007, 10, 3, 2, 0, 1, 2007, 10, 4, 2, 0),
            new DateTest(2007, 10, 4, 2, 0, 1, 2007, 10, 5, 2, 0),
            new DateTest(2007, 10, 5, 2, 0, 1, 2007, 10, 6, 2, 0),
    };

    // The "offset" field in "minuteTests" represents minutes.
    // Use normalize(false) with these tests.
    private DateTest[] minuteTests = {
            // The month numbers are 0-relative, so Jan=0, Feb=1,...Dec=11

            // Nov 4, 12am + 0 minutes = Nov 4, 12am
            // Nov 5, 12am + 0 minutes = Nov 5, 12am
            new DateTest(2007, 10, 4, 0, 0, 0, 2007, 10, 4, 0, 0),
            new DateTest(2007, 10, 5, 0, 0, 0, 2007, 10, 5, 0, 0),

            // Nov 3, 12am + 60 minutes = Nov 3, 1am
            // Nov 4, 12am + 60 minutes = Nov 4, 1am
            // Nov 5, 12am + 60 minutes = Nov 5, 1am
            new DateTest(2007, 10, 3, 0, 0, 60, 2007, 10, 3, 1, 0),
            new DateTest(2007, 10, 4, 0, 0, 60, 2007, 10, 4, 1, 0),
            new DateTest(2007, 10, 5, 0, 0, 60, 2007, 10, 5, 1, 0),

            // Nov 3, 1am + 60 minutes = Nov 3, 2am
            // Nov 4, 1am (PDT) + 30 minutes = Nov 4, 1:30am (PDT)
            // Nov 4, 1am (PDT) + 60 minutes = Nov 4, 1am (PST)
            new DateTest(2007, 10, 3, 1, 0, 60, 2007, 10, 3, 2, 0),
            new DateTest(2007, 10, 4, 1, 0, 1, 30, 2007, 10, 4, 1, 30, 1),
            new DateTest(2007, 10, 4, 1, 0, 1, 60, 2007, 10, 4, 1, 0, 0),

            // Nov 4, 1:30am (PDT) + 15 minutes = Nov 4, 1:45am (PDT)
            // Nov 4, 1:30am (PDT) + 30 minutes = Nov 4, 1:00am (PST)
            // Nov 4, 1:30am (PDT) + 60 minutes = Nov 4, 1:30am (PST)
            new DateTest(2007, 10, 4, 1, 30, 1, 15, 2007, 10, 4, 1, 45, 1),
            new DateTest(2007, 10, 4, 1, 30, 1, 30, 2007, 10, 4, 1, 0, 0),
            new DateTest(2007, 10, 4, 1, 30, 1, 60, 2007, 10, 4, 1, 30, 0),

            // Nov 4, 1:30am (PST) + 15 minutes = Nov 4, 1:45am (PST)
            // Nov 4, 1:30am (PST) + 30 minutes = Nov 4, 2:00am (PST)
            // Nov 5, 1am + 60 minutes = Nov 5, 2am
            new DateTest(2007, 10, 4, 1, 30, 0, 15, 2007, 10, 4, 1, 45, 0),
            new DateTest(2007, 10, 4, 1, 30, 0, 30, 2007, 10, 4, 2, 0, 0),
            new DateTest(2007, 10, 5, 1, 0, 60, 2007, 10, 5, 2, 0),

            // Nov 3, 2am + 60 minutes = Nov 3, 3am
            // Nov 4, 2am + 30 minutes = Nov 4, 2:30am
            // Nov 4, 2am + 60 minutes = Nov 4, 3am
            // Nov 5, 2am + 60 minutes = Nov 5, 3am
            new DateTest(2007, 10, 3, 2, 0, 60, 2007, 10, 3, 3, 0),
            new DateTest(2007, 10, 4, 2, 0, 30, 2007, 10, 4, 2, 30),
            new DateTest(2007, 10, 4, 2, 0, 60, 2007, 10, 4, 3, 0),
            new DateTest(2007, 10, 5, 2, 0, 60, 2007, 10, 5, 3, 0),
    };

    public void testNormalize1() throws Exception {
        Time local = new Time("America/Los_Angeles");

        int len = dayTests.length;
        for (int index = 0; index < len; index++) {
            DateTest test = dayTests[index];
            local.set(0, test.minute1, test.hour1, test.day1, test.month1, test.year1);
            // call normalize() to make sure that isDst is set
            local.normalize(false /* use isDst */);
            local.monthDay += test.offset;
            local.normalize(true /* ignore isDst */);
            if (local.year != test.year2 || local.month != test.month2
                    || local.monthDay != test.day2 || local.hour != test.hour2
                    || local.minute != test.minute2) {
                String expectedTime = String.format("%d-%02d-%02d %02d:%02d",
                        test.year2, test.month2, test.day2, test.hour2, test.minute2);
                String actualTime = String.format("%d-%02d-%02d %02d:%02d",
                        local.year, local.month, local.monthDay, local.hour, local.minute);
                throw new RuntimeException(
                        "day test index " + index + ", normalize(): expected local " + expectedTime
                                + " got: " + actualTime);
            }

            local.set(0, test.minute1, test.hour1, test.day1, test.month1, test.year1);
            // call normalize() to make sure that isDst is set
            local.normalize(false /* use isDst */);
            local.monthDay += test.offset;
            long millis = local.toMillis(true /* ignore isDst */);
            local.set(millis);
            if (local.year != test.year2 || local.month != test.month2
                    || local.monthDay != test.day2 || local.hour != test.hour2
                    || local.minute != test.minute2) {
                String expectedTime = String.format("%d-%02d-%02d %02d:%02d",
                        test.year2, test.month2, test.day2, test.hour2, test.minute2);
                String actualTime = String.format("%d-%02d-%02d %02d:%02d",
                        local.year, local.month, local.monthDay, local.hour, local.minute);
                throw new RuntimeException(
                        "day test index " + index + ", toMillis(): expected local " + expectedTime
                                + " got: " + actualTime);
            }
        }

        len = minuteTests.length;
        for (int index = 0; index < len; index++) {
            DateTest test = minuteTests[index];
            local.set(0, test.minute1, test.hour1, test.day1, test.month1, test.year1);
            local.isDst = test.dst1;
            // call normalize() to make sure that isDst is set
            local.normalize(false /* use isDst */);
            if (test.dst2 == -1) test.dst2 = local.isDst;
            local.minute += test.offset;
            local.normalize(false /* use isDst */);
            if (local.year != test.year2 || local.month != test.month2
                    || local.monthDay != test.day2 || local.hour != test.hour2
                    || local.minute != test.minute2 || local.isDst != test.dst2) {
                String expectedTime = String.format("%d-%02d-%02d %02d:%02d isDst: %d",
                        test.year2, test.month2, test.day2, test.hour2, test.minute2,
                        test.dst2);
                String actualTime = String.format("%d-%02d-%02d %02d:%02d isDst: %d",
                        local.year, local.month, local.monthDay, local.hour, local.minute,
                        local.isDst);
                throw new RuntimeException(
                        "minute test index " + index + ", normalize(): expected local "
                                + expectedTime + " got: " + actualTime);
            }

            local.set(0, test.minute1, test.hour1, test.day1, test.month1, test.year1);
            local.isDst = test.dst1;
            // call normalize() to make sure that isDst is set
            local.normalize(false /* use isDst */);
            if (test.dst2 == -1) test.dst2 = local.isDst;
            local.minute += test.offset;
            long millis = local.toMillis(false /* use isDst */);
            local.set(millis);
            if (local.year != test.year2 || local.month != test.month2
                    || local.monthDay != test.day2 || local.hour != test.hour2
                    || local.minute != test.minute2 || local.isDst != test.dst2) {
                String expectedTime = String.format("%d-%02d-%02d %02d:%02d isDst: %d",
                        test.year2, test.month2, test.day2, test.hour2, test.minute2,
                        test.dst2);
                String actualTime = String.format("%d-%02d-%02d %02d:%02d isDst: %d",
                        local.year, local.month, local.monthDay, local.hour, local.minute,
                        local.isDst);
                throw new RuntimeException(
                        "minute test index " + index + ", toMillis(): expected local "
                                + expectedTime + " got: " + actualTime);
            }
        }
    }

    public void testSwitchTimezone0() throws Exception {
        final String timeZone = "America/Los_Angeles";
        Time t = new Time(Time.TIMEZONE_UTC);
        t.parse("20061005T120000");
        t.switchTimezone(timeZone);
        assertEquals(timeZone, t.timezone);
    }

    public void testCtor0() throws Exception {
        Time t = new Time(Time.TIMEZONE_UTC);
        assertEquals(Time.TIMEZONE_UTC, t.timezone);
    }

    public void testGetActualMaximum0() throws Exception {
        Time t = new Time(Time.TIMEZONE_UTC);
        assertEquals(59, t.getActualMaximum(Time.SECOND));
        assertEquals(59, t.getActualMaximum(Time.MINUTE));
        assertEquals(23, t.getActualMaximum(Time.HOUR));
        assertEquals(11, t.getActualMaximum(Time.MONTH));
        assertEquals(2037, t.getActualMaximum(Time.YEAR));
        assertEquals(6, t.getActualMaximum(Time.WEEK_DAY));
        assertEquals(364, t.getActualMaximum(Time.YEAR_DAY));
        t.year = 2000;
        assertEquals(365, t.getActualMaximum(Time.YEAR_DAY));

        try {
            t.getActualMaximum(Time.WEEK_NUM);
            fail("should throw runtime exception");
        } catch (Exception e) {
            // expected
        }
        final int noExistField = -1;
        try {
            t.getActualMaximum(noExistField);
        } catch (Exception e) {
            // expected
        }

        t.year = 2001;
        final int[] DAYS_PER_MONTH = {
                31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        };
        assertMonth(t, DAYS_PER_MONTH);

        t.year = 2000;
        DAYS_PER_MONTH[1] = 29;
        assertMonth(t, DAYS_PER_MONTH);
    }

    private void assertMonth(Time t, final int[] DAYS_PER_MONTH) {
        for (int i = 0; i < t.getActualMaximum(Time.MONTH); i++) {
            t.month = i;
            assertEquals(DAYS_PER_MONTH[i], t.getActualMaximum(Time.MONTH_DAY));
        }
    }

    public void testClear0() throws Exception {
        Time t = new Time(Time.getCurrentTimezone());
        t.clear(Time.TIMEZONE_UTC);
        assertEquals(Time.TIMEZONE_UTC, t.timezone);
        assertFalse(t.allDay);
        assertEquals(0, t.second);
        assertEquals(0, t.minute);
        assertEquals(0, t.hour);
        assertEquals(0, t.monthDay);
        assertEquals(0, t.month);
        assertEquals(0, t.year);
        assertEquals(0, t.weekDay);
        assertEquals(0, t.yearDay);
        assertEquals(0, t.gmtoff);
        assertEquals(-1, t.isDst);
    }

    public void testCompare0() throws Exception {
        Time a = new Time(Time.TIMEZONE_UTC);
        Time b = new Time("America/Los_Angeles");
        assertTrue(Time.compare(a, b) < 0);
        assertTrue(Time.compare(b, a) > 0);
        assertTrue(Time.compare(b, b) == 0);
        assertTrue(Time.compare(a, a) == 0);
    }

    public void testCompareNullFailure() throws Exception {
        Time a = new Time(Time.TIMEZONE_UTC);

        try {
            Time.compare(a, null);
            fail("Should throw NullPointerException on second argument");
        } catch (NullPointerException e) {
            // pass
        }

        try {
            Time.compare(null, a);
            fail("Should throw NullPointerException on first argument");
        } catch (NullPointerException e) {
            // pass
        }

        try {
            Time.compare(null, null);
            fail("Should throw NullPointerException because both args are null");
        } catch (NullPointerException e) {
            // pass
        }
    }

    public void testFormat0() throws Exception {
        Time t = new Time(Time.TIMEZONE_UTC);
        String r = t.format("%Y%m%dT%H%M%S");
        assertEquals("19700101T000000", r);
    }

    public void testSetToNow0() throws Exception {
        Time t = new Time(Time.TIMEZONE_UTC);
        // Time has resolution of 1 second. So round-off to second and compare
        long currentTime = System.currentTimeMillis() / 1000;
        t.setToNow();
        long time = t.toMillis(false) / 1000;
        // 1 sec of delta can happen
        if (Math.abs(currentTime - time) > 1) {
            fail("currentTime " + currentTime + " time " + time);
        }
    }

    public void testMillis0() throws Exception {
        Time t = new Time(Time.TIMEZONE_UTC);
        t.set(0, 0, 0, 1, 1, 2006);
        long r = t.toMillis(true);
        assertEquals(1138752000000l, r);

        t.set(1, 0, 0, 1, 1, 2006);
        r = t.toMillis(true);
        assertEquals(1138752001000l, r);

        t.set(1, 0, 0, 1, 0, 1970);
        r = t.toMillis(true);
        assertEquals(1000, r);
    }

    public void testParse0() throws Exception {
        Time t = new Time(Time.TIMEZONE_UTC);
        assertFalse(t.parse("12345678T901234"));
        assertTrue(t.parse("20081013T160000Z"));
        assertEquals(2008, t.year);
        assertEquals(9, t.month);
        assertEquals(13, t.monthDay);
        assertEquals(16, t.hour);
        assertEquals(0, t.minute);
        assertEquals(0, t.second);

    }

    public void testSet0() throws Exception {
        Time t = new Time(Time.TIMEZONE_UTC);
        long time = System.currentTimeMillis();
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        date.setTimeInMillis(time);
        t.set(time);
        assertEquals(date.get(Calendar.YEAR), t.year);
        assertEquals(date.get(Calendar.MONTH), t.month);
        assertEquals(date.get(Calendar.DAY_OF_MONTH), t.monthDay);
        assertEquals(date.get(Calendar.HOUR_OF_DAY), t.hour);
        assertEquals(date.get(Calendar.MINUTE), t.minute);
        assertEquals(date.get(Calendar.SECOND), t.second);
    }

    public void testSet1() throws Exception {
        final int year = 2008;
        final int month = 6;
        final int date = 12;
        final int hour = 16;
        final int minute = 21;
        final int second = 35;
        Time t = new Time(Time.TIMEZONE_UTC);
        t.set(second, minute, hour, date, month, year);
        assertEquals(year, t.year);
        assertEquals(month, t.month);
        assertEquals(date, t.monthDay);
        assertEquals(hour, t.hour);
        assertEquals(minute, t.minute);
        assertEquals(second, t.second);
    }

    // Timezones that cover the world.  Some GMT offsets occur more than
    // once in case some cities decide to change their GMT offset.
    private static final String[] mTimeZones = {
        "Pacific/Kiritimati",
        "Pacific/Enderbury",
        "Pacific/Fiji",
        "Antarctica/South_Pole",
        "Pacific/Norfolk",
        "Pacific/Ponape",
        "Asia/Magadan",
        "Australia/Lord_Howe",
        "Australia/Sydney",
        "Australia/Adelaide",
        "Asia/Tokyo",
        "Asia/Seoul",
        "Asia/Taipei",
        "Asia/Singapore",
        "Asia/Hong_Kong",
        "Asia/Saigon",
        "Asia/Bangkok",
        "Indian/Cocos",
        "Asia/Rangoon",
        "Asia/Omsk",
        "Antarctica/Mawson",
        "Asia/Colombo",
        "Asia/Calcutta",
        "Asia/Oral",
        "Asia/Kabul",
        "Asia/Dubai",
        "Asia/Tehran",
        "Europe/Moscow",
        "Asia/Baghdad",
        "Africa/Mogadishu",
        "Europe/Athens",
        "Africa/Cairo",
        "Europe/Rome",
        "Europe/Berlin",
        "Europe/Amsterdam",
        "Africa/Tunis",
        "Europe/London",
        "Europe/Dublin",
        "Atlantic/St_Helena",
        "Africa/Monrovia",
        "Africa/Accra",
        "Atlantic/Azores",
        "Atlantic/South_Georgia",
        "America/Noronha",
        "America/Sao_Paulo",
        "America/Cayenne",
        "America/St_Johns",
        "America/Puerto_Rico",
        "America/Aruba",
        "America/New_York",
        "America/Chicago",
        "America/Denver",
        "America/Los_Angeles",
        "America/Anchorage",
        "Pacific/Marquesas",
        "America/Adak",
        "Pacific/Honolulu",
        "Pacific/Midway",
    };

    public void testGetJulianDay() throws Exception {
        Time time = new Time();

        // For every 15th day of 2008, and for each of the timezones listed above,
        // get the Julian day for 12am and then check that if we change the time we get the
        // same Julian day. Note that one of the many problems with the Time class
        // is its lack of error handling. If we accidentally hit a time that doesn't
        // exist (because it was skipped by a daylight savings transition), rather than
        // an error, you'll silently get 1970-01-01. We should @deprecate Time.
        for (int monthDay = 1; monthDay <= 366; monthDay += 15) {
            for (int zoneIndex = 0; zoneIndex < mTimeZones.length; zoneIndex++) {
                // We leave the "month" as zero because we are changing the
                // "monthDay" from 1 to 366. The call to normalize() will
                // then change the "month" (but we don't really care).
                time.set(0, 0, 0, monthDay, 0, 2008);
                time.timezone = mTimeZones[zoneIndex];
                long millis = time.normalize(true);
                if (zoneIndex == 0) {
                    Log.i(TAG, time.format("%B %d, %Y"));
                }

                // This is the Julian day for 12am for this day of the year
                int julianDay = Time.getJulianDay(millis, time.gmtoff);

                // Change the time during the day and check that we get the same
                // Julian day.
                for (int hour = 0; hour < 24; hour++) {
                    for (int minute = 0; minute < 60; minute += 15) {
                        time.set(0, minute, hour, monthDay, 0, 2008);
                        millis = time.normalize(true);
                        int day = Time.getJulianDay(millis, time.gmtoff);
                        if (day != julianDay) {
                            Log.e("TimeTest", "Julian day: " + day + " at time " + time.hour + ":"
                                    + time.minute + " != today's Julian day: " + julianDay
                                    + " timezone: " + time.timezone);
                        }
                        assertEquals(day, julianDay);
                    }
                }
            }
        }
    }

    public void testSetJulianDay() throws Exception {
        Time time = new Time();

        // For each day of the year in 2008, and for each timezone,
        // test that we can set the Julian day correctly.
        for (int monthDay = 1; monthDay <= 366; monthDay += 20) {
            for (int zoneIndex = 0; zoneIndex < mTimeZones.length; zoneIndex++) {
                // We leave the "month" as zero because we are changing the
                // "monthDay" from 1 to 366. The call to normalize() will
                // then change the "month" (but we don't really care).
                time.set(0, 0, 0, monthDay, 0, 2008);
                time.timezone = mTimeZones[zoneIndex];
                long millis = time.normalize(true);
                if (zoneIndex == 0) {
                    Log.i(TAG, time.format("%B %d, %Y"));
                }
                int julianDay = Time.getJulianDay(millis, time.gmtoff);

                time.setJulianDay(julianDay);

                // Some places change daylight saving time at 12am and so there
                // is no 12am on some days in some timezones. In those cases,
                // the time is set to 1am.
                // Examples: Africa/Cairo on April 25, 2008
                // America/Sao_Paulo on October 12, 2008
                // Atlantic/Azores on March 30, 2008
                assertTrue(time.hour == 0 || time.hour == 1);
                assertEquals(0, time.minute);
                assertEquals(0, time.second);

                millis = time.toMillis(false);
                int day = Time.getJulianDay(millis, time.gmtoff);
                if (day != julianDay) {
                    Log.i(TAG, "Error: gmtoff " + (time.gmtoff / 3600.0) + " day "
                            + julianDay + " millis " + millis + " " + time.format("%B %d, %Y")
                            + " " + time.timezone);
                }
                assertEquals(day, julianDay);
            }
        }
    }
}
