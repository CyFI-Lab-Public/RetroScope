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

package com.android.providers.contacts;

import android.test.suitebuilder.annotation.SmallTest;

import com.android.providers.contacts.PostalSplitter.Postal;

import junit.framework.TestCase;

import java.util.Locale;

/**
 * Tests for {@link PostalSplitter}, especially for ja_JP locale.
 * This class depends on the assumption that all the tests in {@link NameSplitterTest} pass.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.android.providers.contacts.PostalSplitterForJapaneseTest -w
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@SmallTest
public class PostalSplitterForJapaneseTest extends TestCase {
    private PostalSplitter mPostalSplitter;

    // Postal address for Tokyo Metropolitan City Hall (Tokyo-Tocho) as of 2009 + pseudo PO box.
    // Japanese don't use neighborhood, so it is not used in this test suite.
    //
    // "Nihon" in Kanji
    private static final String COUNTRY = "\u65E5\u672C";
    private static final String POSTCODE = "163-8001";
    // "Tokyo-to" in Kanji
    private static final String REGION = "\u6771\u4EAC\u90FD";
    // "Sinjuku-ku" in Kanji
    private static final String CITY = "\u65B0\u5BBF\u533A";
    // Nishi-Sinjuku 2-8-1
    private static final String STREET = "\u897F\u65B0\u5BBF 2-8-1";
    // Pseudo PO box for test: "Sisyobako 404"
    private static final String POBOX = "\u79C1\u66F8\u7BB1";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mPostalSplitter = new PostalSplitter(Locale.JAPAN);
    }

    public void testNull() {
        assertSplitPostal(null, null, null, null, null, null, null, null);
        assertJoinedPostal(null, null, null, null, null, null, null, null);
    }

    public void testEmpty() {
        assertSplitPostal("", null, null, null, null, null, null, null);
        assertJoinedPostal(null, null, null, null, null, null, null, null);
    }

    public void testSpaces() {
        assertSplitPostal(" ", " ", null, null, null, null, null, null);
        assertJoinedPostal(" ", " ", null, null, null, null, null, null);
    }

    public void testPobox() {
        assertJoinedPostal(CITY + "\n" + POBOX, null, POBOX, null, CITY, null, null, null);
    }

    public void testNormal() {
        assertJoinedPostal(POSTCODE + "\n" + REGION + " " + CITY + "\n" + STREET,
                STREET, null, null, CITY, REGION, POSTCODE, null);
    }

    public void testMissingRegion() {
        assertJoinedPostal(POSTCODE + "\n" + REGION + "\n" + STREET,
                STREET, null, null, REGION, null, POSTCODE, null);

        assertJoinedPostal(POSTCODE + "\n" + STREET,
                STREET, null, null, null, null, POSTCODE, null);

        assertJoinedPostal(COUNTRY + " " + POSTCODE + "\n" + STREET,
                STREET, null, null, null, null, POSTCODE, COUNTRY);
    }

    public void testMissingPostcode() {
        assertJoinedPostal(REGION + " " + CITY + "\n" + STREET,
                STREET, null, null, CITY, REGION, null, null);

        assertJoinedPostal(COUNTRY + "\n" + REGION + " " + CITY + "\n" + STREET,
                STREET, null, null, CITY, REGION, null, COUNTRY);

        assertJoinedPostal(COUNTRY + "\n" + STREET,
                STREET, null, null, null, null, null, COUNTRY);
    }

    public void testMissingStreet() {
        assertJoinedPostal(COUNTRY + "\n" + STREET,
                null, null, STREET, null, null, null, COUNTRY);
    }

    private void assertSplitPostal(String formattedPostal, String street, String pobox,
            String neighborhood, String city, String region, String postcode, String country) {
        final Postal postal = new Postal();
        mPostalSplitter.split(postal, formattedPostal);
        assertEquals(street, postal.street);
        assertEquals(pobox, postal.pobox);
        assertEquals(neighborhood, postal.neighborhood);
        assertEquals(city, postal.city);
        assertEquals(region, postal.region);
        assertEquals(postcode, postal.postcode);
        assertEquals(country, postal.country);
    }

    private void assertJoinedPostal(String formattedPostal, String street, String pobox,
            String neighborhood, String city, String region, String postcode, String country) {
        final Postal postal = new Postal();
        postal.street = street;
        postal.pobox = pobox;
        postal.neighborhood = neighborhood;
        postal.city = city;
        postal.region = region;
        postal.postcode = postcode;
        postal.country = country;

        final String joined = mPostalSplitter.join(postal);
        assertEquals(formattedPostal, joined);
    }
}
