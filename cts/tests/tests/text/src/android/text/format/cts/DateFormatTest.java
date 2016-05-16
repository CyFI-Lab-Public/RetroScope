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


import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.test.AndroidTestCase;
import android.text.format.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateFormatTest extends AndroidTestCase {

    private Context mContext;
    private ContentResolver mContentResolver;

    // Date: 2008-12-18 05:30
    private static final int YEAR_FROM_1900 = 108;
    private static final int YEAR = 2008;
    private static final int MONTH = Calendar.DECEMBER; // java.util.Calendar months are 0-based.
    private static final int DAY = 18;
    private static final int HOUR = 5;
    private static final int MINUTE = 30;

    private boolean mIs24HourFormat;
    private Locale mDefaultLocale;
    private String mDefaultFormat;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mContentResolver = mContext.getContentResolver();
        mIs24HourFormat = DateFormat.is24HourFormat(mContext);
        mDefaultLocale = Locale.getDefault();
        mDefaultFormat = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.DATE_FORMAT);
    }

    @Override
    protected void tearDown() throws Exception {
        if (!mIs24HourFormat) {
            Settings.System.putString(mContentResolver, Settings.System.TIME_12_24, "12");
        }
        if (!Locale.getDefault().equals(mDefaultLocale)) {
            Locale.setDefault(mDefaultLocale);
        }
        Settings.System.putString(mContentResolver, Settings.System.DATE_FORMAT, mDefaultFormat);
        super.tearDown();
    }


    public void test_is24HourFormat() {
        Settings.System.putString(mContentResolver, Settings.System.TIME_12_24, "24");
        assertTrue(DateFormat.is24HourFormat(mContext));
        Settings.System.putString(mContentResolver, Settings.System.TIME_12_24, "12");
        assertFalse(DateFormat.is24HourFormat(mContext));
    }

    public void test_format_M() {
        Calendar c = new GregorianCalendar(2008, Calendar.DECEMBER, 18);
        assertEquals("D", DateFormat.format("MMMMM", c));
        assertEquals("December", DateFormat.format("MMMM", c));
        assertEquals("Dec", DateFormat.format("MMM", c));
        assertEquals("12", DateFormat.format("MM", c));
        assertEquals("12", DateFormat.format("M", c));
    }

    public void test_format_L() {
        // TODO: we can't test other locales with this API so we can't test 'L' properly!
        Calendar c = new GregorianCalendar(2008, Calendar.DECEMBER, 18);
        assertEquals("D", DateFormat.format("LLLLL", c));
        assertEquals("December", DateFormat.format("LLLL", c));
        assertEquals("Dec", DateFormat.format("LLL", c));
        assertEquals("12", DateFormat.format("LL", c));
        assertEquals("12", DateFormat.format("L", c));
    }

    public void test_format_E() {
        Calendar c = new GregorianCalendar(2008, Calendar.DECEMBER, 18);
        assertEquals("T", DateFormat.format("EEEEE", c));
        assertEquals("Thursday", DateFormat.format("EEEE", c));
        assertEquals("Thu", DateFormat.format("EEE", c));
        assertEquals("Thu", DateFormat.format("EE", c));
        assertEquals("Thu", DateFormat.format("E", c));
    }

    public void test_format_c() {
        // TODO: we can't test other locales with this API, so we can't test 'c' properly!
        Calendar c = new GregorianCalendar(2008, Calendar.DECEMBER, 18);
        assertEquals("T", DateFormat.format("ccccc", c));
        assertEquals("Thursday", DateFormat.format("cccc", c));
        assertEquals("Thu", DateFormat.format("ccc", c));
        assertEquals("Thu", DateFormat.format("cc", c));
        assertEquals("Thu", DateFormat.format("c", c));
    }

    @SuppressWarnings("deprecation")
    public void testFormatMethods() throws ParseException {
        if (!mDefaultLocale.equals(Locale.US)) {
            Locale.setDefault(Locale.US);
        }

        java.text.DateFormat dateFormat = DateFormat.getDateFormat(mContext);
        assertNotNull(dateFormat);
        Date date = new Date(YEAR_FROM_1900, MONTH, DAY, HOUR, MINUTE);
        String source = dateFormat.format(date);
        Date parseDate = dateFormat.parse(source);
        assertEquals(date.getYear(), parseDate.getYear());
        assertEquals(date.getMonth(), parseDate.getMonth());
        assertEquals(date.getDay(), date.getDay());

        dateFormat = DateFormat.getLongDateFormat(mContext);
        assertNotNull(dateFormat);
        source = dateFormat.format(date);
        assertTrue(source.indexOf("December") >= 0);
        dateFormat = DateFormat.getMediumDateFormat(mContext);
        assertNotNull(dateFormat);
        source = dateFormat.format(date);
        assertTrue(source.indexOf("Dec") >= 0);
        assertTrue(source.indexOf("December") < 0);
        dateFormat = DateFormat.getTimeFormat(mContext);
        source = dateFormat.format(date);
        assertTrue(source.indexOf("5") >= 0);
        assertTrue(source.indexOf("30") >= 0);

        String testFormat = "yyyy-MM-dd";
        String testOrder = "yMd";
        Settings.System.putString(mContentResolver, Settings.System.DATE_FORMAT, testFormat);
        String actualOrder = String.valueOf(DateFormat.getDateFormatOrder(mContext));
        assertEquals(testOrder, actualOrder);

        String format = "MM/dd/yy";
        String expectedString = "12/18/08";
        Calendar calendar = new GregorianCalendar(YEAR, MONTH, DAY);
        CharSequence actual = DateFormat.format(format, calendar);
        assertEquals(expectedString, actual.toString());
        Date formatDate = new Date(YEAR_FROM_1900, MONTH, DAY);
        actual = DateFormat.format(format, formatDate);
        assertEquals(expectedString, actual.toString());
        actual = DateFormat.format(format, formatDate.getTime());
        assertEquals(expectedString, actual.toString());
    }

    public void test2038() {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+00:00"));

        calendar.setTimeInMillis(((long) Integer.MIN_VALUE + Integer.MIN_VALUE) * 1000L);
        assertEquals("Sun Nov 24 17:31:44 GMT+00:00 1833",
                DateFormat.format("EEE MMM dd HH:mm:ss zzz yyyy", calendar));

        calendar.setTimeInMillis(Integer.MIN_VALUE * 1000L);
        assertEquals("Fri Dec 13 20:45:52 GMT+00:00 1901",
                DateFormat.format("EEE MMM dd HH:mm:ss zzz yyyy", calendar));

        calendar.setTimeInMillis(0L);
        assertEquals("Thu Jan 01 00:00:00 GMT+00:00 1970",
                DateFormat.format("EEE MMM dd HH:mm:ss zzz yyyy", calendar));

        calendar.setTimeInMillis(Integer.MAX_VALUE * 1000L);
        assertEquals("Tue Jan 19 03:14:07 GMT+00:00 2038",
                DateFormat.format("EEE MMM dd HH:mm:ss zzz yyyy", calendar));

        calendar.setTimeInMillis((2L + Integer.MAX_VALUE + Integer.MAX_VALUE) * 1000L);
        assertEquals("Sun Feb 07 06:28:16 GMT+00:00 2106",
                DateFormat.format("EEE MMM dd HH:mm:ss zzz yyyy", calendar));
    }

    private static void checkFormat(String expected, String pattern, int hour) {
        TimeZone utc = TimeZone.getTimeZone("UTC");

        Calendar c = new GregorianCalendar(utc);
        c.set(2013, Calendar.JANUARY, 1, hour, 00);

        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(utc);

        if (pattern.equals("k") && (hour == 0 || hour == 24)) {
          // http://b/8359981: 'k' has always been broken on Android, and we keep it broken
          // for compatibility. Maybe one day we'll be able to fix this...
          assertEquals("0", DateFormat.format(pattern, c));
        } else {
          assertEquals(expected, DateFormat.format(pattern, c));
        }
        assertEquals(expected, sdf.format(c.getTime()));
    }

    public void test_bug_8359981() {
        checkFormat("24", "k", 00);
        checkFormat( "0", "K", 00);
        checkFormat("12", "h", 00);
        checkFormat( "0", "H", 00);

        checkFormat( "1", "k", 01);
        checkFormat( "1", "K", 01);
        checkFormat( "1", "h", 01);
        checkFormat( "1", "H", 01);

        checkFormat("12", "k", 12);
        checkFormat( "0", "K", 12);
        checkFormat("12", "h", 12);
        checkFormat("12", "H", 12);

        checkFormat("13", "k", 13);
        checkFormat( "1", "K", 13);
        checkFormat( "1", "h", 13);
        checkFormat("13", "H", 13);

        checkFormat("24", "k", 24);
        checkFormat( "0", "K", 24);
        checkFormat("12", "h", 24);
        checkFormat( "0", "H", 24);
    }
}
