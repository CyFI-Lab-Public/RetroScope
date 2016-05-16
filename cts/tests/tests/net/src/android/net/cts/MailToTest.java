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

package android.net.cts;

import android.net.MailTo;
import android.test.AndroidTestCase;
import android.util.Log;

public class MailToTest extends AndroidTestCase {
    private static final String MAILTOURI_1 = "mailto:chris@example.com";
    private static final String MAILTOURI_2 = "mailto:infobot@example.com?subject=current-issue";
    private static final String MAILTOURI_3 =
            "mailto:infobot@example.com?body=send%20current-issue";
    private static final String MAILTOURI_4 = "mailto:infobot@example.com?body=send%20current-" +
                                              "issue%0D%0Asend%20index";
    private static final String MAILTOURI_5 = "mailto:joe@example.com?" +
                                              "cc=bob@example.com&body=hello";
    private static final String MAILTOURI_6 = "mailto:?to=joe@example.com&" +
                                              "cc=bob@example.com&body=hello";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testParseMailToURI() {
        assertFalse(MailTo.isMailTo(null));
        assertFalse(MailTo.isMailTo(""));
        assertFalse(MailTo.isMailTo("http://www.google.com"));

        assertTrue(MailTo.isMailTo(MAILTOURI_1));
        MailTo mailTo_1 = MailTo.parse(MAILTOURI_1);
        Log.d("Trace", mailTo_1.toString());
        assertEquals("chris@example.com", mailTo_1.getTo());
        assertEquals(1, mailTo_1.getHeaders().size());
        assertNull(mailTo_1.getBody());
        assertNull(mailTo_1.getCc());
        assertNull(mailTo_1.getSubject());
        assertEquals("mailto:?to=chris%40example.com&", mailTo_1.toString());

        assertTrue(MailTo.isMailTo(MAILTOURI_2));
        MailTo mailTo_2 = MailTo.parse(MAILTOURI_2);
        Log.d("Trace", mailTo_2.toString());
        assertEquals(2, mailTo_2.getHeaders().size());
        assertEquals("infobot@example.com", mailTo_2.getTo());
        assertEquals("current-issue", mailTo_2.getSubject());
        assertNull(mailTo_2.getBody());
        assertNull(mailTo_2.getCc());
        String stringUrl = mailTo_2.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("to=infobot%40example.com&"));
        assertTrue(stringUrl.contains("subject=current-issue&"));

        assertTrue(MailTo.isMailTo(MAILTOURI_3));
        MailTo mailTo_3 = MailTo.parse(MAILTOURI_3);
        Log.d("Trace", mailTo_3.toString());
        assertEquals(2, mailTo_3.getHeaders().size());
        assertEquals("infobot@example.com", mailTo_3.getTo());
        assertEquals("send current-issue", mailTo_3.getBody());
        assertNull(mailTo_3.getCc());
        assertNull(mailTo_3.getSubject());
        stringUrl = mailTo_3.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("to=infobot%40example.com&"));
        assertTrue(stringUrl.contains("body=send%20current-issue&"));

        assertTrue(MailTo.isMailTo(MAILTOURI_4));
        MailTo mailTo_4 = MailTo.parse(MAILTOURI_4);
        Log.d("Trace", mailTo_4.toString() + " " + mailTo_4.getBody());
        assertEquals(2, mailTo_4.getHeaders().size());
        assertEquals("infobot@example.com", mailTo_4.getTo());
        assertEquals("send current-issue\r\nsend index", mailTo_4.getBody());
        assertNull(mailTo_4.getCc());
        assertNull(mailTo_4.getSubject());
        stringUrl = mailTo_4.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("to=infobot%40example.com&"));
        assertTrue(stringUrl.contains("body=send%20current-issue%0D%0Asend%20index&"));


        assertTrue(MailTo.isMailTo(MAILTOURI_5));
        MailTo mailTo_5 = MailTo.parse(MAILTOURI_5);
        Log.d("Trace", mailTo_5.toString() + mailTo_5.getHeaders().toString()
                + mailTo_5.getHeaders().size());
        assertEquals(3, mailTo_5.getHeaders().size());
        assertEquals("joe@example.com", mailTo_5.getTo());
        assertEquals("bob@example.com", mailTo_5.getCc());
        assertEquals("hello", mailTo_5.getBody());
        assertNull(mailTo_5.getSubject());
        stringUrl = mailTo_5.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("cc=bob%40example.com&"));
        assertTrue(stringUrl.contains("body=hello&"));
        assertTrue(stringUrl.contains("to=joe%40example.com&"));

        assertTrue(MailTo.isMailTo(MAILTOURI_6));
        MailTo mailTo_6 = MailTo.parse(MAILTOURI_6);
        Log.d("Trace", mailTo_6.toString() + mailTo_6.getHeaders().toString()
                + mailTo_6.getHeaders().size());
        assertEquals(3, mailTo_6.getHeaders().size());
        assertEquals(", joe@example.com", mailTo_6.getTo());
        assertEquals("bob@example.com", mailTo_6.getCc());
        assertEquals("hello", mailTo_6.getBody());
        assertNull(mailTo_6.getSubject());
        stringUrl = mailTo_6.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("cc=bob%40example.com&"));
        assertTrue(stringUrl.contains("body=hello&"));
        assertTrue(stringUrl.contains("to=%2C%20joe%40example.com&"));
    }
}
