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

import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.test.suitebuilder.annotation.MediumTest;

import com.android.providers.contacts.ContactLookupKey.LookupKeySegment;
import com.android.providers.contacts.testutil.RawContactUtil;

import java.util.ArrayList;

/**
 * Unit tests for {@link ContactLookupKey}.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.android.providers.contacts.ContactLookupKeyTest -w \
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@MediumTest
public class ContactLookupKeyTest extends BaseContactsProvider2Test {

    public void testLookupKeyUsingDisplayNameAndNoAccount() {
        long rawContactId1 = RawContactUtil.createRawContactWithName(mResolver, "John", "Doe");
        long rawContactId2 = RawContactUtil.createRawContactWithName(mResolver, "johndoe", null);
        setAggregationException(
                AggregationExceptions.TYPE_KEEP_TOGETHER, rawContactId1, rawContactId2);

        // Normalized display name
        String normalizedName = NameNormalizer.normalize("johndoe");
        String expectedLookupKey = "0r" + rawContactId1 + "-" + normalizedName + ".0r"
                + rawContactId2 + "-" + normalizedName;

        long contactId = queryContactId(rawContactId1);
        assertStoredValue(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.LOOKUP_KEY, expectedLookupKey);

        // Find the contact using lookup key by itself
        Uri lookupUri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, expectedLookupKey);
        assertStoredValue(lookupUri, Contacts._ID, contactId);

        // Find the contact using both the lookup key and the correct contact ID
        assertStoredValue(ContentUris.withAppendedId(lookupUri, contactId),
                Contacts._ID, contactId);

        // Find the contact using both the lookup key and an incorrect contact ID
        assertStoredValue(ContentUris.withAppendedId(lookupUri, contactId + 1),
                Contacts._ID, contactId);
    }

    public void testLookupKeyUsingSourceIdAndNoAccount() {
        long rawContactId1 = RawContactUtil.createRawContactWithName(mResolver, "John", "Doe");
        storeValue(RawContacts.CONTENT_URI, rawContactId1, RawContacts.SOURCE_ID, "123");

        long rawContactId2 = RawContactUtil.createRawContactWithName(mResolver, "johndoe", null);
        storeValue(RawContacts.CONTENT_URI, rawContactId2, RawContacts.SOURCE_ID, "4.5.6");

        long rawContactId3 = RawContactUtil.createRawContactWithName(mResolver, "john", "dough");
        storeValue(RawContacts.CONTENT_URI, rawContactId3, RawContacts.SOURCE_ID, "http://foo?bar");

        setAggregationException(
                AggregationExceptions.TYPE_KEEP_TOGETHER, rawContactId1, rawContactId2);
        setAggregationException(
                AggregationExceptions.TYPE_KEEP_TOGETHER, rawContactId1, rawContactId3);

        // Two source ids, of them escaped
        String expectedLookupKey = "0i123.0e4..5..6.0ihttp%3A%2F%2Ffoo%3Fbar";

        long contactId = queryContactId(rawContactId1);
        assertStoredValue(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.LOOKUP_KEY, expectedLookupKey);

        Uri lookupUri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, expectedLookupKey);
        assertStoredValue(lookupUri, Contacts._ID, contactId);
    }

    public void testLookupKeySameSourceIdDifferentAccounts() {
        long rawContactId1 = RawContactUtil.createRawContactWithName(mResolver, "Dear", "Doe");
        storeValue(RawContacts.CONTENT_URI, rawContactId1, RawContacts.ACCOUNT_TYPE, "foo");
        storeValue(RawContacts.CONTENT_URI, rawContactId1, RawContacts.ACCOUNT_NAME, "FOO");
        storeValue(RawContacts.CONTENT_URI, rawContactId1, RawContacts.SOURCE_ID, "1");

        long rawContactId2 = RawContactUtil.createRawContactWithName(mResolver, "Deer", "Dough");
        storeValue(RawContacts.CONTENT_URI, rawContactId2, RawContacts.ACCOUNT_TYPE, "bar");
        storeValue(RawContacts.CONTENT_URI, rawContactId2, RawContacts.ACCOUNT_NAME, "BAR");
        storeValue(RawContacts.CONTENT_URI, rawContactId2, RawContacts.SOURCE_ID, "1");

        assertNotAggregated(rawContactId1, rawContactId2);

        int accountHashCode1 = ContactLookupKey.getAccountHashCode("foo", "FOO");
        int accountHashCode2 = ContactLookupKey.getAccountHashCode("bar", "BAR");

        long contactId1 = queryContactId(rawContactId1);
        assertStoredValue(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId1),
                Contacts.LOOKUP_KEY, accountHashCode1 + "i1");

        long contactId2 = queryContactId(rawContactId2);
        assertStoredValue(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId2),
                Contacts.LOOKUP_KEY, accountHashCode2 + "i1");

        Uri lookupUri1 = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, accountHashCode1 + "i1");
        assertStoredValue(lookupUri1, Contacts._ID, contactId1);

        Uri lookupUri2 = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, accountHashCode2 + "i1");
        assertStoredValue(lookupUri2, Contacts._ID, contactId2);
    }

    public void testLookupKeyChoosingLargestContact() {
        long rawContactId1 = RawContactUtil.createRawContactWithName(mResolver, "John", "Doe");
        storeValue(RawContacts.CONTENT_URI, rawContactId1, RawContacts.SOURCE_ID, "1");

        long rawContactId2 = RawContactUtil.createRawContactWithName(mResolver, "John", "Doe");
        storeValue(RawContacts.CONTENT_URI, rawContactId2, RawContacts.SOURCE_ID, "2");

        long rawContactId3 = RawContactUtil.createRawContactWithName(mResolver, "John", "Doe");
        storeValue(RawContacts.CONTENT_URI, rawContactId3, RawContacts.SOURCE_ID, "3");
        setAggregationException(
                AggregationExceptions.TYPE_KEEP_TOGETHER, rawContactId1, rawContactId2);
        setAggregationException(
                AggregationExceptions.TYPE_KEEP_TOGETHER, rawContactId1, rawContactId3);

        String lookupKey = "0i1.0i2.0i3";

        long contactId = queryContactId(rawContactId1);
        assertStoredValue(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.LOOKUP_KEY, lookupKey);

        setAggregationException(AggregationExceptions.TYPE_KEEP_SEPARATE, rawContactId1,
                rawContactId3);
        setAggregationException(AggregationExceptions.TYPE_KEEP_SEPARATE, rawContactId2,
                rawContactId3);
        assertAggregated(rawContactId1, rawContactId2);
        assertNotAggregated(rawContactId1, rawContactId3);
        assertNotAggregated(rawContactId2, rawContactId3);

        long largerContactId = queryContactId(rawContactId1);
        assertStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, largerContactId),
                Contacts.LOOKUP_KEY, "0i1.0i2");
        assertStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, queryContactId(rawContactId3)),
                Contacts.LOOKUP_KEY, "0i3");

        Uri lookupUri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey);
        assertStoredValue(lookupUri, Contacts._ID, largerContactId);
    }

    public void testGetLookupUri() {
        long rawContactId1 = RawContactUtil.createRawContactWithName(mResolver, "John", "Doe");
        storeValue(RawContacts.CONTENT_URI, rawContactId1, RawContacts.SOURCE_ID, "1");

        long contactId = queryContactId(rawContactId1);
        String lookupUri = "content://com.android.contacts/contacts/lookup/0i1/" + contactId;

        Uri contentUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        assertEquals(lookupUri,
                Contacts.getLookupUri(mResolver, contentUri).toString());

        Uri staleLookupUri = ContentUris.withAppendedId(
                Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, "0i1"),
                contactId+2);
        assertEquals(lookupUri,
                Contacts.getLookupUri(mResolver, staleLookupUri).toString());
    }

    public void testParseLookupKey() {
        // Display name
        assertLookupKey("123n1248AC",
                new int[]{123},
                new int[]{ContactLookupKey.LOOKUP_TYPE_DISPLAY_NAME},
                new String[]{"1248AC"});

        // Raw contact ID + display name
        assertLookupKey("123r20-1248AC",
                new int[]{123},
                new int[]{ContactLookupKey.LOOKUP_TYPE_RAW_CONTACT_ID},
                new String[]{"1248AC"});

        // Unescaped source ID
        assertLookupKey("0i1248AC-X",
                new int[]{0},
                new int[]{ContactLookupKey.LOOKUP_TYPE_SOURCE_ID},
                new String[]{"1248AC-X"});

        // Escaped source ID
        assertLookupKey("432e12..48AC",
                new int[]{432},
                new int[]{ContactLookupKey.LOOKUP_TYPE_SOURCE_ID},
                new String[]{"12.48AC"});

        // Compound lookup key
        assertLookupKey("123n1248AC.0i1248AC.432e12..48AC.123n1248AC.123r30-2184CA",
                new int[]{123, 0, 432, 123},
                new int[] {
                        ContactLookupKey.LOOKUP_TYPE_DISPLAY_NAME,
                        ContactLookupKey.LOOKUP_TYPE_SOURCE_ID,
                        ContactLookupKey.LOOKUP_TYPE_SOURCE_ID,
                        ContactLookupKey.LOOKUP_TYPE_DISPLAY_NAME,
                        ContactLookupKey.LOOKUP_TYPE_RAW_CONTACT_ID,
                },
                new String[]{"1248AC", "1248AC", "12.48AC", "1248AC", "2184CA"});
    }

    private void assertLookupKey(String lookupKey, int[] accountHashCodes, int[] types,
            String[] keys) {
        ContactLookupKey key = new ContactLookupKey();
        ArrayList<LookupKeySegment> list = key.parse(lookupKey);
        assertEquals(types.length, list.size());

        for (int i = 0; i < accountHashCodes.length; i++) {
            LookupKeySegment segment = list.get(i);
            assertEquals(accountHashCodes[i], segment.accountHashCode);
            assertEquals(types[i], segment.lookupType);
            assertEquals(keys[i], segment.key);
        }
    }
}

