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

import com.android.calendarcommon2.ICalendar;
import com.android.calendarcommon2.RecurrenceSet;

import android.content.ContentValues;
import android.provider.CalendarContract;
import android.test.suitebuilder.annotation.SmallTest;
import junit.framework.TestCase;

import java.util.List;

public class RecurrenceSetTest extends TestCase {

    private static String MOCK_COMPONENT_NAME = "mockComponent";

    private static final String RRULE_LESS_THAN_75_CHARS =
            "FREQ=WEEKLY;BYDAY=SU,FR,SA;UNTIL=20100326T190000Z;WKST=MO";

    private static final String RRULE_MORE_THAN_75_CHARS =
            "FREQ=WEEKLY;WKST=MO;UNTIL=20100129T130000Z;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR, SA,SU";

    private static final String RRULE_MORE_THAN_75_CHARS_FOLDED =
            "FREQ=WEEKLY;WKST=MO;UNTIL=20100129T130000Z;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR,\r\n  SA,SU";

    private static final String STRING_WITH_160_CHARS = "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789";

    private static final String STRING_WITH_160_CHARS_FOLDED = "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "01234"
            + "\r\n "
            + "56789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "0123456789"
            + "\r\n "
            + "0123456789";

    @SmallTest
    public void testFoldPerRFC2445() {
        assertEquals(RRULE_LESS_THAN_75_CHARS,
                RecurrenceSet.fold(RRULE_LESS_THAN_75_CHARS));

        assertEquals(RRULE_MORE_THAN_75_CHARS_FOLDED,
                RecurrenceSet.fold(RRULE_MORE_THAN_75_CHARS));

        assertEquals(STRING_WITH_160_CHARS_FOLDED,
                RecurrenceSet.fold(STRING_WITH_160_CHARS));
    }

    @SmallTest
    public void testUnFoldPerRFC2445() {
        assertEquals(RRULE_LESS_THAN_75_CHARS,
                RecurrenceSet.unfold(RRULE_LESS_THAN_75_CHARS));

        assertEquals(RRULE_MORE_THAN_75_CHARS,
                RecurrenceSet.unfold(RRULE_MORE_THAN_75_CHARS_FOLDED));

        assertEquals(STRING_WITH_160_CHARS,
                RecurrenceSet.unfold(STRING_WITH_160_CHARS_FOLDED));
    }

    @SmallTest
    public void testRRULEfolding() {
        ICalendar.Component component = new ICalendar.Component(MOCK_COMPONENT_NAME, null);

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, 0);
        values.put(CalendarContract.Events.DURATION, "P3600S");
        values.put(CalendarContract.Events.RRULE, RRULE_LESS_THAN_75_CHARS);

        assertTrue(RecurrenceSet.populateComponent(values, component));
        List<ICalendar.Property> list = component.getProperties("DTSTART");
        assertTrue(list.size() == 1);
        assertEquals("19700101T000000Z", list.get(0).getValue());

        list = component.getProperties("RRULE");
        assertTrue(list.size() == 1);
        assertEquals(RRULE_LESS_THAN_75_CHARS,list.get(0).getValue());

        component = new ICalendar.Component(MOCK_COMPONENT_NAME, null);

        values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, 0);
        values.put(CalendarContract.Events.DURATION, "P3600S");
        values.put(CalendarContract.Events.RRULE, RRULE_MORE_THAN_75_CHARS);

        assertTrue(RecurrenceSet.populateComponent(values, component));

        list = component.getProperties("RRULE");
        assertTrue(list.size() == 1);
        assertEquals(RRULE_MORE_THAN_75_CHARS_FOLDED, list.get(0).getValue());

        component = new ICalendar.Component(MOCK_COMPONENT_NAME, null);

        values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, 0);
        values.put(CalendarContract.Events.DURATION, "P3600S");
        values.put(CalendarContract.Events.RRULE, STRING_WITH_160_CHARS);

        assertTrue(RecurrenceSet.populateComponent(values, component));

        list = component.getProperties("RRULE");
        assertTrue(list.size() == 1);
        assertEquals(STRING_WITH_160_CHARS_FOLDED, list.get(0).getValue());
    }
}
