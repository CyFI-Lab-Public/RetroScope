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

package android.util.cts;

import java.util.Calendar;

import android.test.AndroidTestCase;
import android.util.MonthDisplayHelper;

public class MonthDisplayHelperTest extends AndroidTestCase {
    private MonthDisplayHelper mHelper;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testConstructor() {

        try {
            mHelper = new MonthDisplayHelper(2008,
                    Calendar.DECEMBER, Calendar.MONDAY);
            mHelper = new MonthDisplayHelper(2008, Calendar.DECEMBER);
        } catch (Exception e) {
            fail("shouldn't throw exception");
        }

        try {
            mHelper = new MonthDisplayHelper(2008,
                    Calendar.DECEMBER, Calendar.SUNDAY - 1);
            fail("should throw exception");
        } catch (Exception e) {
        }
        try {
            mHelper = new MonthDisplayHelper(2008,
                    Calendar.DECEMBER, Calendar.SATURDAY + 1);
            fail("should throw exception");
        } catch (Exception e) {
        }
        try {
            mHelper = new MonthDisplayHelper(-1, Calendar.DECEMBER,
                    Calendar.SATURDAY + 1);
            fail("should throw exception");
        } catch (Exception e) {
        }
        try {
            mHelper = new MonthDisplayHelper(-1,
                    Calendar.DECEMBER + 1, Calendar.SATURDAY + 1);
            fail("should throw exception");
        } catch (Exception e) {
        }

    }

    public void testNumberOfDaysInCurrentMonth() {
        assertEquals(30, new MonthDisplayHelper(2007, Calendar.SEPTEMBER)
                .getNumberOfDaysInMonth());
        assertEquals(28, new MonthDisplayHelper(2007, Calendar.FEBRUARY)
        .getNumberOfDaysInMonth());
        assertEquals(29, new MonthDisplayHelper(2008, Calendar.FEBRUARY)
        .getNumberOfDaysInMonth());
    }

    public void testNextMonth() {
        mHelper = new MonthDisplayHelper(2007, Calendar.AUGUST, Calendar.SUNDAY);

        assertArraysEqual(new int[] { 29, 30, 31, 1, 2, 3, 4 }, mHelper
                .getDigitsForRow(0));

        mHelper.nextMonth();

        assertEquals(Calendar.SEPTEMBER, mHelper.getMonth());
        assertArraysEqual(new int[] { 26, 27, 28, 29, 30, 31, 1 }, mHelper
                .getDigitsForRow(0));
    }

    public void testGetRowOf() {
        mHelper = new MonthDisplayHelper(2007,
                Calendar.AUGUST, Calendar.SUNDAY);

        assertEquals(0, mHelper.getRowOf(2));
        assertEquals(0, mHelper.getRowOf(4));
        assertEquals(2, mHelper.getRowOf(12));
        assertEquals(2, mHelper.getRowOf(18));
        assertEquals(3, mHelper.getRowOf(19));
    }

    public void testHelperProperties() {
        mHelper = new MonthDisplayHelper(2007, Calendar.AUGUST, Calendar.SUNDAY);

        assertEquals(1, mHelper.getWeekStartDay());
        assertEquals(3, mHelper.getOffset());
        mHelper = new MonthDisplayHelper(2007, Calendar.AUGUST);
        assertEquals(1, mHelper.getWeekStartDay());
        assertEquals(3, mHelper.getOffset());
    }

    public void testMonthRows() {
        mHelper = new MonthDisplayHelper(2007, Calendar.SEPTEMBER);

        assertArraysEqual(new int[] { 26, 27, 28, 29, 30, 31, 1 }, mHelper
                .getDigitsForRow(0));
        assertArraysEqual(new int[] { 2, 3, 4, 5, 6, 7, 8 }, mHelper
                .getDigitsForRow(1));
        assertArraysEqual(new int[] { 30, 1, 2, 3, 4, 5, 6 }, mHelper
                .getDigitsForRow(5));

        mHelper = new MonthDisplayHelper(2007, Calendar.SEPTEMBER,
                Calendar.MONDAY);

        assertArraysEqual(new int[] { 27, 28, 29, 30, 31, 1, 2 }, mHelper
                .getDigitsForRow(0));
        assertArraysEqual(new int[] { 3, 4, 5, 6, 7, 8, 9 }, mHelper
                .getDigitsForRow(1));
        assertArraysEqual(new int[] { 24, 25, 26, 27, 28, 29, 30 }, mHelper
                .getDigitsForRow(4));
        assertArraysEqual(new int[] { 1, 2, 3, 4, 5, 6, 7 }, mHelper
                .getDigitsForRow(5));
    }

    public void testFirstDayOfMonth() {

        assertEquals("august 2007", Calendar.WEDNESDAY, new MonthDisplayHelper(
                2007, Calendar.AUGUST).getFirstDayOfMonth());

        assertEquals("september, 2007", Calendar.SATURDAY,
                new MonthDisplayHelper(2007, Calendar.SEPTEMBER)
                        .getFirstDayOfMonth());
    }

    public void testGetColumnOf() {
        mHelper= new MonthDisplayHelper(2007,
                Calendar.AUGUST, Calendar.SUNDAY);

        assertEquals(3, mHelper.getColumnOf(1));
        assertEquals(4, mHelper.getColumnOf(9));
        assertEquals(5, mHelper.getColumnOf(17));
        assertEquals(6, mHelper.getColumnOf(25));
        assertEquals(0, mHelper.getColumnOf(26));
    }

    public void testGetDayAt() {
        mHelper = new MonthDisplayHelper(2007,
                Calendar.AUGUST, Calendar.SUNDAY);

        assertEquals(30, mHelper.getDayAt(0, 1));
    }

    public void testPrevMonth() {
        mHelper = new MonthDisplayHelper(2007, Calendar.SEPTEMBER,
                Calendar.SUNDAY);

        assertArraysEqual(new int[] { 26, 27, 28, 29, 30, 31, 1 }, mHelper
                .getDigitsForRow(0));

        mHelper.previousMonth();

        assertEquals(Calendar.AUGUST, mHelper.getMonth());
        assertArraysEqual(new int[] { 29, 30, 31, 1, 2, 3, 4 }, mHelper
                .getDigitsForRow(0));

        mHelper = new MonthDisplayHelper(2007, Calendar.JANUARY);

        mHelper.previousMonth();

        assertEquals(2006, mHelper.getYear());
        assertEquals(Calendar.DECEMBER, mHelper.getMonth());
    }

    public void testIsWithinCurrentMonth() {
        mHelper = new MonthDisplayHelper(2007, Calendar.SEPTEMBER,
                Calendar.SUNDAY);

        // out of bounds
        assertFalse(mHelper.isWithinCurrentMonth(-1, 3));
        assertFalse(mHelper.isWithinCurrentMonth(6, 3));
        assertFalse(mHelper.isWithinCurrentMonth(2, -1));
        assertFalse(mHelper.isWithinCurrentMonth(2, 7));

        // last day of previous month
        assertFalse(mHelper.isWithinCurrentMonth(0, 5));

        // first day of next month
        assertFalse(mHelper.isWithinCurrentMonth(5, 1));

        // first day in month
        assertTrue(mHelper.isWithinCurrentMonth(0, 6));

        // last day in month
        assertTrue(mHelper.isWithinCurrentMonth(5, 0));
    }

    private void assertArraysEqual(int[] expected, int[] actual) {
        assertEquals("array length", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("index " + i,
                    expected[i], actual[i]);
        }
    }
}

