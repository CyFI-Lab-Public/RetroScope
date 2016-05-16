/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.mail.utils;

import android.test.AndroidTestCase;

import com.android.mail.utils.NotificationUtils.MailMessagePlainTextConverter;
import com.google.android.mail.common.html.parser.HtmlTree;

public class NotificationUtilsTest extends AndroidTestCase {
    /**
     * Verifies that we strip out <style /> tags.
     */
    public void testMailMessagePlainTextConverterStyles() {
        final String expectedText = "This test passed!";
        final String html = "<body style=3D=22margin:0; padding:0;=22>"
                + "<style type=3D=22text/css=22>=20"
                + "=2EReadMsgBody =7B width: 100%;=7D"
                + "       img =7Bdisplay: block;=7D"
                + "       html =7B -webkit-text-size-adjust:none; =7D</style>"
                + expectedText + "</body>";

        // Get the html "tree" for this message body
        final HtmlTree htmlTree = Utils.getHtmlTree(html);
        htmlTree.setPlainTextConverterFactory(new HtmlTree.PlainTextConverterFactory() {
            @Override
            public HtmlTree.PlainTextConverter createInstance() {
                return new MailMessagePlainTextConverter();
            }
        });

        final String resultText = htmlTree.getPlainText();

        assertEquals(expectedText, resultText);
    }

    /**
     * Verifies that we strip out nested <style /> tags.
     */
    public void testMailMessagePlainTextConverterNestedStyles() {
        final String expectedText = "This test passed!";
        final String html = "<body style=3D=22margin:0; padding:0;=22>"
                + "<style type=3D=22text/css=22>=20"
                + "=2EReadMsgBody =7B width: 100%;=7D"
                + "       img =7Bdisplay: block;=7D"
                + "       html =7B -webkit-text-size-adjust:none; =7D"
                + "       <style>html =7B -webkit-text-size-adjust:none; =7D</style></style>"
                + expectedText + "</body>";

        // Get the html "tree" for this message body
        final HtmlTree htmlTree = Utils.getHtmlTree(html);
        htmlTree.setPlainTextConverterFactory(new HtmlTree.PlainTextConverterFactory() {
            @Override
            public HtmlTree.PlainTextConverter createInstance() {
                return new MailMessagePlainTextConverter();
            }
        });

        final String resultText = htmlTree.getPlainText();

        assertEquals(expectedText, resultText);
    }
}
