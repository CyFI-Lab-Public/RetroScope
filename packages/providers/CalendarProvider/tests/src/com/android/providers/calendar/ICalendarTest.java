// Copyright 2007 The Android Open Source Project
package com.android.providers.calendar;

import com.android.calendarcommon2.ICalendar;

import android.test.suitebuilder.annotation.SmallTest;
import junit.framework.TestCase;

import java.util.List;

public class ICalendarTest extends TestCase {

    @SmallTest
    public void testAddParameter() throws Exception {
        ICalendar.Property prop = new ICalendar.Property("prop1", "value1");
        assertEquals(0, prop.getParameterNames().size());
        prop.addParameter(new ICalendar.Parameter("param1", "foo"));
        assertEquals(1, prop.getParameterNames().size());
        prop.addParameter(new ICalendar.Parameter("param1", "bar"));
        assertEquals(1, prop.getParameterNames().size());
        prop.addParameter(new ICalendar.Parameter("param2", "baaz"));
        assertEquals(2, prop.getParameterNames().size());
        prop.addParameter(new ICalendar.Parameter("param1", "quux"));
        assertEquals(2, prop.getParameterNames().size());
        prop.addParameter(new ICalendar.Parameter("param3", "accent"));
        assertEquals(3, prop.getParameterNames().size());
        assertEquals("prop1;param1=foo;param1=bar;param1=quux;"
                + "param2=baaz;param3=accent:value1", prop.toString());
    }

    @SmallTest
    public void testAddProperty() throws Exception {
        String text = "BEGIN:DUMMY\n" +
                "prop2:value3\n" +
                "prop1:value1\n" +
                "prop1:value2\n" +
                "END:DUMMY\n";

        ICalendar.Component component = new ICalendar.Component("DUMMY", null);
        // properties should be listed in insertion order, by property name.
        component.addProperty(new ICalendar.Property("prop2", "value3"));
        component.addProperty(new ICalendar.Property("prop1", "value1"));
        component.addProperty(new ICalendar.Property("prop1", "value2"));
        assertEquals(text, component.toString());
    }

    @SmallTest
    public void testAddComponent() throws Exception {
        String text = "BEGIN:DUMMY\n" +
                "prop1:value1\n" +
                "prop1:value12\n" +
                "BEGIN:DUMMY2\n" +
                "prop2:value2\n" +
                "END:DUMMY2\n" +
                "END:DUMMY\n";

        ICalendar.Component parent = new ICalendar.Component("DUMMY", null);
        // properties should precede components
        ICalendar.Component child = new ICalendar.Component("DUMMY2", parent);
        child.addProperty(new ICalendar.Property("prop2", "value2"));
        parent.addChild(child);
        parent.addProperty(new ICalendar.Property("prop1", "value1"));
        parent.addProperty(new ICalendar.Property("prop1", "value12"));
        assertEquals(text, parent.toString());
    }

    @SmallTest
    public void testParseBasicComponent() throws Exception {
        String text = "BEGIN:DUMMY\n" +
                "PROP1;PARAM1=foo;PARAM2=bar:VALUE1\n" +
                "PROP1;PARAM1=baaz;PARAM1=quux:VALUE2\n" +
                "PROP2:VALUE3\n" +
                "END:DUMMY\n";

        ICalendar.Component component = ICalendar.parseComponent(text);
        
        assertEquals("DUMMY", component.getName());
        assertNull(component.getComponents());
        assertEquals(2, component.getPropertyNames().size());
        ICalendar.Property prop1 = component.getFirstProperty("PROP1");
        assertEquals(2, prop1.getParameterNames().size());
        assertEquals("foo", prop1.getFirstParameter("PARAM1").value);
        assertEquals("bar", prop1.getFirstParameter("PARAM2").value);
        List<ICalendar.Property> props = component.getProperties("PROP1");
        assertEquals(2, props.size());
        List<ICalendar.Parameter> params = props.get(1).getParameters("PARAM1");
        assertEquals("baaz", params.get(0).value);
        assertEquals("quux", params.get(1).value);
    }

    @SmallTest
    public void testParseQuotedParam() throws Exception {
        ICalendar.Component component
                = new ICalendar.Component("DUMMY", null /* parent */);
        ICalendar.parseComponent(
                component,
                "DTSTART;TZID=\"GMT+03:00\";TEST=test1;TEST=\"test2\":20101221T090000");
        ICalendar.Property property = component.getFirstProperty("DTSTART");
        assertEquals(2, property.getParameterNames().size());
        assertEquals("GMT+03:00", property.getFirstParameter("TZID").value);
        final List<ICalendar.Parameter> testParameters = property.getParameters("TEST");
        assertEquals(2, testParameters.size());
        assertEquals("test1", testParameters.get(0).value);
        assertEquals("test2", testParameters.get(1).value);
        assertEquals("20101221T090000", component.getFirstProperty("DTSTART").getValue());
    }

    @SmallTest
    public void testParseBadQuotedParam() throws Exception {
        ICalendar.Component component
                = new ICalendar.Component("DUMMY", null /* parent */);

        ICalendar.parseComponent(
                component,
                "FOO;PARAM1=\"param1\"\";PARAM=quote-before-param:value");
        assertNull("Invalid quote before param value", component.getFirstProperty("FOO"));

        ICalendar.parseComponent(
                component,
                "FOO;PARAM\"=expected-equal:value");
        assertNull("Expected equal in param", component.getFirstProperty("FOO"));

        ICalendar.parseComponent(
                component,
                "FOO;PARAM=text-not-allowed\"before-quote:value");
        assertNull("Invalid quote in param value", component.getFirstProperty("FOO"));

        ICalendar.parseComponent(
                component,
                "FOO;PARAM=\"missing-end-quote:value");
        assertNull("missing-end-quote", component.getFirstProperty("FOO"));
    }

    @SmallTest
    public void testParseChildComponent() throws Exception {
        String childText = "BEGIN:CHILD\n" +
                "PROP1;PARAM1=foo;PARAM2=bar:VALUE1\n" +
                "PROP1;PARAM1=baaz;PARAM1=quux:VALUE2\n" +
                "PROP2:VALUE3\n" +
                "END:CHILD\n";

        String completeText = "BEGIN:DUMMY\n" +
                childText +
                "END:DUMMY\n";

        ICalendar.Component component = new ICalendar.Component("DUMMY", null);
        component = ICalendar.parseComponent(component, childText);
        assertEquals("DUMMY", component.getName());
        assertEquals(1, component.getComponents().size());
        assertEquals(completeText, component.toString());
    }

    @SmallTest
    public void testParseBareEvent() throws Exception {
        String text = "BEGIN:VEVENT\nEND:VEVENT\n";
        ICalendar.Component event = ICalendar.parseEvent(text);

        assertEquals("VEVENT", event.getName());
        assertNull(event.getComponents());
        assertEquals(0, event.getPropertyNames().size());
    }

    @SmallTest
    public void testParseEvent1() throws Exception {
        String text = "BEGIN:VEVENT\n" +
                "DTSTART:19970714T170000Z\n" +
                "DTEND:19970715T035959Z\n" +
                "SUMMARY:Bastille Day Party\n" +
                "END:VEVENT\n";

        ICalendar.Component event = ICalendar.parseEvent(text);

        assertEquals("VEVENT", event.getName());
        assertNull(event.getComponents());
        assertEquals(3, event.getPropertyNames().size());
        assertEquals(1, event.getProperties("DTSTART").size());
        assertEquals("19970714T170000Z", event.getFirstProperty("DTSTART").getValue());
        assertEquals(0, event.getFirstProperty("DTSTART").getParameterNames().size());
        assertEquals(1, event.getProperties("DTEND").size());
        assertEquals(0, event.getFirstProperty("DTEND").getParameterNames().size());
        assertEquals("19970715T035959Z", event.getFirstProperty("DTEND").getValue());
        assertEquals(1, event.getProperties("SUMMARY").size());
        assertEquals(0, event.getFirstProperty("SUMMARY").getParameterNames().size());
        assertEquals("Bastille Day Party", event.getFirstProperty("SUMMARY").getValue());
    }

    @SmallTest
    public void testParseEvent2() throws Exception {
        String text = "BEGIN:VEVENT\n" +
                "DTSTART;TZID=America/Los_Angeles:19970714T170000\n" +
                "DURATION:+P3600S\n" +
                "SUMMARY;FOO=1;BAR=2:Bastille Day Party\n" +
                "END:VEVENT\n";

        ICalendar.Component event = ICalendar.parseEvent(text);

        assertEquals("VEVENT", event.getName());
        assertNull(event.getComponents());
        assertEquals(3, event.getPropertyNames().size());
        assertEquals(1, event.getProperties("DTSTART").size());
        assertEquals("19970714T170000", event.getFirstProperty("DTSTART").getValue());
        assertEquals(1, event.getFirstProperty("DTSTART").getParameterNames().size());
        assertEquals(1, event.getProperties("SUMMARY").size());
    }

    @SmallTest
    public void testParseInvalidProperty() throws Exception {
        String text = "BEGIN:VEVENT\n" +
                "FOO;BAR\n" + // invalid line
                "END:VEVENT\n";

        ICalendar.Component event = ICalendar.parseEvent(text);

        assertEquals("VEVENT", event.getName());
        assertNull(event.getComponents());
        assertEquals(0, event.getPropertyNames().size());
    }

    @SmallTest
    public void testParseEventDoesNotStartWithBegin() throws Exception {
        String text = "NOTBEGIN:DUMMY\n" +
                "END:DUMMY\n";

        try {
            ICalendar.parseEvent(text);
            fail("expected exception not thrown");
        } catch (ICalendar.FormatException e) {
            assertEquals("Expected " + ICalendar.Component.VEVENT, e.getMessage());
        }
    }

    @SmallTest
    public void testParseCalendarDoesNotStartWithBegin() throws Exception {
        String text = "NOTBEGIN:DUMMY\n" +
                "END:DUMMY\n";

        try {
            ICalendar.parseCalendar(text);
            fail("expected exception not thrown");
        } catch (ICalendar.FormatException e) {
            assertEquals("Expected " + ICalendar.Component.VCALENDAR, e.getMessage());
        }
    }

    @SmallTest
    public void testParseComponentDoesNotStartWithBegin() throws Exception {
        String text = "NOTBEGIN:DUMMY\n" +
                "END:DUMMY\n";

        ICalendar.Component component = ICalendar.parseComponent(text);
        assertNull(component);
    }
    
    @SmallTest
    public void testParseUnexpectedEndComponent() throws Exception {
        String text = "BEGIN:PARENT\n" +
                "END:BADPARENT\n";

        ICalendar.Component component = ICalendar.parseComponent(text);
        assertNotNull(component);
    }

    @SmallTest
    public void testParseNoEndComponent() throws Exception {
        String text = "BEGIN:DUMMY\n" +
                "END:\n";

        ICalendar.Component component = ICalendar.parseComponent(text);
        assertNotNull(component);
    }

    @SmallTest
    public void testNormalize() throws Exception {
        String text = "BEGIN:VEVENT\n" +
                "RRULE:FREQ=SECONDLY;BYSECOND=0,1,2,\r\n 3,4,5\r\n ,6,7,8\r\n" +
                "END:VEVENT\n";

        ICalendar.Component event = ICalendar.parseEvent(text);

        assertEquals("VEVENT", event.getName());
        assertNull(event.getComponents());
        assertEquals(1, event.getPropertyNames().size());
        assertEquals(1, event.getProperties("RRULE").size());
        assertEquals("FREQ=SECONDLY;BYSECOND=0,1,2,3,4,5,6,7,8", event.getFirstProperty("RRULE").getValue());

    }

    @SmallTest
    public void testNormalizeBadSep() throws Exception {
        String text = "BEGIN:VEVENT\n" +
                "RRULE:FREQ=SECONDLY;BYSECOND=0,1,2,\n 3,4,5\n ,6,7,8\n" +
                "END:VEVENT\n";

        ICalendar.Component event = ICalendar.parseEvent(text);

        assertEquals("VEVENT", event.getName());
        assertNull(event.getComponents());
        assertEquals(1, event.getPropertyNames().size());
        assertEquals(1, event.getProperties("RRULE").size());
        assertEquals("FREQ=SECONDLY;BYSECOND=0,1,2,3,4,5,6,7,8", event.getFirstProperty("RRULE").getValue());
    }


    @SmallTest
    public void testBad() throws Exception {
        String text = "BEGIN:VEVENT\n" +
                "RRULE=foo\n" +
                "END:VEVENT\n";

        ICalendar.Component event = ICalendar.parseEvent(text);

        // Note that parseEvent doesn't throw the FormatException you might expect because
        // ICalendar.parseComponentImpl catches the exception due to misformatted GData.
        // TODO: update this test after cleaning up the ICalendar behavior
    }
}
