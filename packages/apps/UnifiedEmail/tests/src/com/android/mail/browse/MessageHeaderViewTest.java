/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.browse;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class MessageHeaderViewTest extends AndroidTestCase {

    @SmallTest
    public void testRecipientSummaryLongTo() {
        String[] to = makeRecipientArray("TO", 60);
        String[] cc = makeRecipientArray("CC", 60);
        String summary = MessageHeaderView.getRecipientSummaryText(getContext(), "", "", to, cc,
                null, null, null).toString();

        assertTrue(summary.contains("TO00"));
        assertTrue(summary.contains("TO49"));
        assertFalse(summary.contains("TO50"));
    }

    @SmallTest
    public void testRecipientSummaryLongMultipleLists() {
        String[] to = makeRecipientArray("TO", 20);
        String[] cc = makeRecipientArray("CC", 10);
        String[] bcc = makeRecipientArray("BB", 60);
        String summary = MessageHeaderView.getRecipientSummaryText(getContext(), "", "", to, cc,
                bcc, null, null).toString();

        assertTrue(summary.contains("TO00"));
        assertTrue(summary.contains("TO19"));
        assertTrue(summary.contains("CC00"));
        assertTrue(summary.contains("CC09"));
        assertTrue(summary.contains("BB00"));
        assertTrue(summary.contains("BB19"));
        assertFalse(summary.contains("BB20"));
    }

    private static String[] makeRecipientArray(String prefix, int len) {
        String[] arr = new String[len];
        for (int i=0; i < arr.length; i++) {
            arr[i] = String.format("\"%s%02d\" <foo@bar.com>", prefix, i);
        }
        return arr;
    }

    @SmallTest
    public void testMakeSnippet() {
        assertSnippetEquals("Hello, world!",
                "Hello, world!");
        assertSnippetEquals(" Foo Bar ",
                "\nFoo\n \nBar\t  ");
        assertSnippetEquals("Hello, World...",
                "<p><span style=\"color:red\">Hello, <b>World</b></span>...</p>");
        assertSnippetEquals("Simon & Garfunkel",
                "Simon &amp; Garfunkel");
    }

    private static void assertSnippetEquals(final String expectedSnippet,
            final String messageBody) {
        assertEquals(expectedSnippet, MessageHeaderView.makeSnippet(messageBody));
    }

}
