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

package android.util.cts;

import android.os.Process;
import android.util.EventLog;
import android.util.EventLog.Event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class EventLogTest extends TestCase {
    private static final int ANSWER_TAG = 42;
    private static final int PI_TAG = 314;
    private static final int E_TAG = 2718;

    public void testWriteEvent() throws Exception {
        long markerData = System.currentTimeMillis();
        EventLog.writeEvent(ANSWER_TAG, markerData);
        EventLog.writeEvent(ANSWER_TAG, 12345);
        EventLog.writeEvent(ANSWER_TAG, 23456L);
        EventLog.writeEvent(ANSWER_TAG, "Test");
        EventLog.writeEvent(ANSWER_TAG, 12345, 23456L, "Test");

        List<EventLog.Event> events = getEventsAfterMarker(markerData, ANSWER_TAG);
        assertEquals(4, events.size());
        assertEquals(ANSWER_TAG, events.get(0).getTag());
        assertEquals(12345, events.get(0).getData());
        assertEquals(23456L, events.get(1).getData());
        assertEquals("Test", events.get(2).getData());

        Object[] arr = (Object[]) events.get(3).getData();
        assertEquals(3, arr.length);
        assertEquals(12345, arr[0]);
        assertEquals(23456L, arr[1]);
        assertEquals("Test", arr[2]);
    }

    public void testWriteEventWithOversizeValue() throws Exception {
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 1000; i++) longString.append("xyzzy");

        Object[] longArray = new Object[1000];
        for (int i = 0; i < 1000; i++) longArray[i] = 12345;

        Long markerData = System.currentTimeMillis();
        EventLog.writeEvent(ANSWER_TAG, markerData);
        EventLog.writeEvent(ANSWER_TAG, longString.toString());
        EventLog.writeEvent(ANSWER_TAG, "hi", longString.toString());
        EventLog.writeEvent(ANSWER_TAG, 12345, longString.toString());
        EventLog.writeEvent(ANSWER_TAG, 12345L, longString.toString());
        EventLog.writeEvent(ANSWER_TAG, longString.toString(), longString.toString());
        EventLog.writeEvent(ANSWER_TAG, longArray);

        List<Event> events = getEventsAfterMarker(markerData, ANSWER_TAG);
        assertEquals(6, events.size());

        // subtract: log header, type byte, final newline
        final int max = 4096 - 20 - 4 - 1;

        // subtract: string header (type + length)
        String val0 = (String) events.get(0).getData();
        assertEquals(max - 5, val0.length());

        // subtract: array header, "hi" header, "hi", string header
        Object[] arr1 = (Object[]) events.get(1).getData();
        assertEquals(2, arr1.length);
        assertEquals("hi", arr1[0]);
        assertEquals(max - 2 - 5 - 2 - 5, ((String) arr1[1]).length());

        // subtract: array header, int (type + value), string header
        Object[] arr2 = (Object[]) events.get(2).getData();
        assertEquals(2, arr2.length);
        assertEquals(12345, arr2[0]);
        assertEquals(max - 2 - 5 - 5, ((String) arr2[1]).length());

        // subtract: array header, long, string header
        Object[] arr3 = (Object[]) events.get(3).getData();
        assertEquals(2, arr3.length);
        assertEquals(12345L, arr3[0]);
        assertEquals(max - 2 - 9 - 5, ((String) arr3[1]).length());

        // subtract: array header, string header (second string is dropped entirely)
        Object[] arr4 = (Object[]) events.get(4).getData();
        assertEquals(1, arr4.length);
        assertEquals(max - 2 - 5, ((String) arr4[0]).length());

        Object[] arr5 = (Object[]) events.get(5).getData();
        assertEquals(255, arr5.length);
        assertEquals(12345, arr5[0]);
        assertEquals(12345, arr5[arr5.length - 1]);
    }

    public void testWriteNullEvent() throws Exception {
        Long markerData = System.currentTimeMillis();
        EventLog.writeEvent(ANSWER_TAG, markerData);
        EventLog.writeEvent(ANSWER_TAG, (String) null);
        EventLog.writeEvent(ANSWER_TAG, 12345, (String) null);

        List<EventLog.Event> events = getEventsAfterMarker(markerData, ANSWER_TAG);
        assertEquals(2, events.size());
        assertEquals("NULL", events.get(0).getData());

        Object[] arr = (Object[]) events.get(1).getData();
        assertEquals(2, arr.length);
        assertEquals(12345, arr[0]);
        assertEquals("NULL", arr[1]);
    }

    public void testReadEvents() throws Exception {
        Long markerData = System.currentTimeMillis();
        EventLog.writeEvent(ANSWER_TAG, markerData);

        Long data0 = markerData + 1;
        EventLog.writeEvent(ANSWER_TAG, data0);

        Long data1 = data0 + 1;
        EventLog.writeEvent(PI_TAG, data1);

        Long data2 = data1 + 1;
        EventLog.writeEvent(E_TAG, data2);

        List<Event> events = getEventsAfterMarker(markerData, ANSWER_TAG, PI_TAG, E_TAG);
        assertEquals(3, events.size());
        assertEvent(events.get(0), ANSWER_TAG, data0);
        assertEvent(events.get(1), PI_TAG, data1);
        assertEvent(events.get(2), E_TAG, data2);

        events = getEventsAfterMarker(markerData, ANSWER_TAG, E_TAG);
        assertEquals(2, events.size());
        assertEvent(events.get(0), ANSWER_TAG, data0);
        assertEvent(events.get(1), E_TAG, data2);

        events = getEventsAfterMarker(markerData, ANSWER_TAG);
        assertEquals(1, events.size());
        assertEvent(events.get(0), ANSWER_TAG, data0);
    }

    /** Return elements after and the event that has the marker data and matching tag. */
    private List<Event> getEventsAfterMarker(Object marker, int... tags) throws IOException {
        List<Event> events = new ArrayList<Event>();
        EventLog.readEvents(tags, events);

        for (Iterator<Event> itr = events.iterator(); itr.hasNext(); ) {
            Event event = itr.next();
            itr.remove();
            if (marker.equals(event.getData())) {
                break;
            }
        }

        assertEventTimes(events);

        return events;
    }

    private void assertEvent(Event event, int expectedTag, Object expectedData) {
        assertEquals(Process.myPid(), event.getProcessId());
        assertEquals(Process.myTid(), event.getThreadId());
        assertEquals(expectedTag, event.getTag());
        assertEquals(expectedData, event.getData());
    }

    private void assertEventTimes(List<Event> events) {
        for (int i = 0; i + 1 < events.size(); i++) {
            long time = events.get(i).getTimeNanos();
            long nextTime = events.get(i).getTimeNanos();
            assertTrue(time <= nextTime);
        }
    }

    public void testGetTagName() throws Exception {
        assertEquals("answer", EventLog.getTagName(ANSWER_TAG));
        assertEquals("pi", EventLog.getTagName(PI_TAG));
        assertEquals("e", EventLog.getTagName(E_TAG));
        assertEquals(null, EventLog.getTagName(999999999));
    }

    public void testGetTagCode() throws Exception {
        assertEquals(ANSWER_TAG, EventLog.getTagCode("answer"));
        assertEquals(PI_TAG, EventLog.getTagCode("pi"));
        assertEquals(E_TAG, EventLog.getTagCode("e"));
        assertEquals(-1, EventLog.getTagCode("does_not_exist"));
    }
}
