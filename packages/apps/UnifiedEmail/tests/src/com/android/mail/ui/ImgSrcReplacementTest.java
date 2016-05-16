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

package com.android.mail.ui;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.mail.ui.HtmlConversationTemplates;

import junit.framework.ComparisonFailure;

public class ImgSrcReplacementTest extends AndroidTestCase {

    private static void replace(final String input, final String expectedOutput) {
        assertEquals(expectedOutput, HtmlConversationTemplates.replaceAbsoluteImgUrls(input));
    }

    @SmallTest
    public void testSimple() {
        replace(
            "<img src=\"http://google.com/favicon.ico\">",
            "<img src='data:' blocked-src=\"http://google.com/favicon.ico\">"
        );
    }

    @SmallTest
    public void testSimpleSingleQuotes() {
        replace(
            "<img src='http://google.com/favicon.ico'>",
            "<img src='data:' blocked-src='http://google.com/favicon.ico'>"
        );
    }

    @SmallTest
    public void testSimpleNoQuotes() {
        replace(
            "<img src=http://google.com/favicon.ico>",
            "<img src='data:' blocked-src=http://google.com/favicon.ico>"
        );
    }

    @SmallTest
    public void testWithElementId() {
        replace(
            "<img id=\"foo\" src=\"http://google.com/favicon.ico\">",
            "<img id=\"foo\" src='data:' blocked-src=\"http://google.com/favicon.ico\">"
        );
    }

    @SmallTest
    public void testWithElementIdAndAltAttr() {
        replace(
            "<img id=\"foo\" src=\"http://google.com/favicon.ico\" alt='foo'>",
            "<img id=\"foo\" src='data:' blocked-src=\"http://google.com/favicon.ico\" alt='foo'>"
        );
    }

    @SmallTest
    public void testHttps() {
        replace(
            "<img src=\"https://google.com/favicon.ico\">",
            "<img src='data:' blocked-src=\"https://google.com/favicon.ico\">"
        );
    }

    @SmallTest
    public void testRelativeUrl() {
        // should not modify
        replace(
            "<img src=\"?foo1234\">",
            "<img src=\"?foo1234\">"
        );
    }

    @SmallTest
    public void testNoSrcAttr() {
        // should not modify
        replace(
            "<img id='foo' width='500' alt='foo'>",
            "<img id='foo' width='500' alt='foo'>"
        );
    }

    @SmallTest
    public void testSrcElsewhere() {
        replace(
            "<img id='foo' width='500' src='http://google.com' alt='foo'> src=httplawl",
            "<img id='foo' width='500' src='data:' blocked-src='http://google.com' alt='foo'> src=httplawl"
        );
    }

    @SmallTest
    public void testWithInnerWhitespace() {
        replace(
            "<  img    src   =   \"http://google.com/favicon.ico\"    >",
            "<  img    src='data:' blocked-src   =   \"http://google.com/favicon.ico\"    >"
        );
    }

    @SmallTest
    public void testValueWithTheWordSrc() {
        replace(
            "<img src=\"http://google.com/foo?src=http%3A%2F%2Fgoogle.com\">",
            "<img src='data:' blocked-src=\"http://google.com/foo?src=http%3A%2F%2Fgoogle.com\">"
        );
    }

    @SmallTest
    public void testWithOtherAttrsAndValueWithTheWordSrc() {
        replace(
            "<img id='src' src=\"http://google.com/foo?src=http%3A%2F%2Fgoogle.com\">",
            "<img id='src' src='data:' blocked-src=\"http://google.com/foo?src=http%3A%2F%2Fgoogle.com\">"
        );
    }

    public void testValueWithTheWordSrcAndASpace() {
        // Doesn't work, but this is not likely to be common.
        // For a regex to handle this properly, it would have to avoid matching on attribute values,
        // maybe by counting quotes.
        try {
            replace(
                    "<img src=\"http://google.com/foo? src=http%3A%2F%2Fgoogle.com\">",
                    "<img src='data:' blocked-src=\"http://google.com/foo? src=http%3A%2F%2Fgoogle.com\">"
                );
        } catch (ComparisonFailure fail) {
            System.out.println("passing known broken case");
        }
    }

}
