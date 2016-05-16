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
 * Tests for {@link PostalSplitter}, especially for en_US locale.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.android.providers.contacts.PostalSplitterTest -w \
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@SmallTest
public class PostalSplitterTest extends TestCase {
    private PostalSplitter mPostalSplitter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mPostalSplitter = new PostalSplitter(Locale.US);
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
        assertJoinedPostal("PO Box 2600\nImaginationland", null, "PO Box 2600", null,
                "Imaginationland", null, null, null);
    }

    public void testNormal() {
        assertJoinedPostal("1600 Amphitheatre Parkway\nMountain View, CA 94043",
                "1600 Amphitheatre Parkway", null, null, "Mountain View", "CA", "94043", null);
    }

    public void testMissingRegion() {
        assertJoinedPostal("1600 Amphitheatre Parkway\nMountain View 94043",
                "1600 Amphitheatre Parkway", null, null, "Mountain View", null, "94043", null);

        assertJoinedPostal("1600 Amphitheatre Parkway\n94043",
                "1600 Amphitheatre Parkway", null, null, null, null, "94043", null);

        assertJoinedPostal("1600 Amphitheatre Parkway\n94043\nUSA",
                "1600 Amphitheatre Parkway", null, null, null, null, "94043", "USA");
    }

    public void testMissingPostcode() {
        assertJoinedPostal("1600 Amphitheatre Parkway\nMountain View, CA",
                "1600 Amphitheatre Parkway", null, null, "Mountain View", "CA", null, null);

        assertJoinedPostal("1600 Amphitheatre Parkway\nMountain View, CA\nUSA",
                "1600 Amphitheatre Parkway", null, null, "Mountain View", "CA", null, "USA");

        assertJoinedPostal("1600 Amphitheatre Parkway\nUSA",
                "1600 Amphitheatre Parkway", null, null, null, null, null, "USA");
    }

    public void testMissingStreet() {
        assertJoinedPostal("Mr. Rogers\nUSA", null, null, "Mr. Rogers", null, null, null, "USA");
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
