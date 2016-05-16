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

import android.database.sqlite.SQLiteOpenHelper;
import android.test.AndroidTestCase;

/**
 * Run various tests on CalendarCache
 *
 * Use the following command line:
 *
 * adb shell am instrument -e debug false -w -e class com.android.providers.calendar.CalendarCacheTest com.android.providers.calendar.tests/android.test.InstrumentationTestRunner
 *
 */
public class CalendarCacheTest extends AndroidTestCase {

    private SQLiteOpenHelper mDbHelper;
    private CalendarCache mCalendarCache;
    private static final String TIMEZONE_DB_2011A = "2011a";

    private static final String TIMEZONE_AMERICA_LOS_ANGELES = "America/Los_Angeles";
    private static final String TIMEZONE_AMERICA_DENVER = "America/Denver";

    @Override
    public void setUp() {
        mDbHelper = CalendarDatabaseHelper.getInstance(getContext());
        mCalendarCache = new CalendarCache(mDbHelper);
    }

    @Override
    public void tearDown() {
        if (mDbHelper != null) {
            mDbHelper.close();
            mDbHelper = null;
            mCalendarCache = null;
        }
    }

    public void testGenerateCacheException() {
        boolean hasException = false;
        try {
            String value = mCalendarCache.readData(null);
        } catch (CalendarCache.CacheException e) {
            hasException = true;
        }
        assertTrue(hasException);
    }

    public void testWriteAndReadTimezoneDatabaseVersion() throws CalendarCache.CacheException {
        mCalendarCache.writeTimezoneDatabaseVersion(TIMEZONE_DB_2011A);
        assertEquals(TIMEZONE_DB_2011A, mCalendarCache.readTimezoneDatabaseVersion());
    }

    public void testWriteAndReadTimezone() throws CalendarCache.CacheException {
        mCalendarCache.writeTimezoneInstances(TIMEZONE_AMERICA_DENVER);
        assertEquals(TIMEZONE_AMERICA_DENVER, mCalendarCache.readTimezoneInstances());
    }

    public void testWriteAndReadTimezonePrevious() throws CalendarCache.CacheException {
        mCalendarCache.writeTimezoneInstancesPrevious(TIMEZONE_AMERICA_LOS_ANGELES);
        assertEquals(TIMEZONE_AMERICA_LOS_ANGELES, mCalendarCache.readTimezoneInstancesPrevious());
    }

    public void testWriteAndReadTimezoneType() throws CalendarCache.CacheException {
        mCalendarCache.writeTimezoneType(CalendarCache.TIMEZONE_TYPE_AUTO);
        assertEquals(CalendarCache.TIMEZONE_TYPE_AUTO, mCalendarCache.readTimezoneType());

        mCalendarCache.writeTimezoneType(CalendarCache.TIMEZONE_TYPE_HOME);
        assertEquals(CalendarCache.TIMEZONE_TYPE_HOME, mCalendarCache.readTimezoneType());
    }
}
