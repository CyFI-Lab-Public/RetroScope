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

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.StatusUpdates;
import android.test.suitebuilder.annotation.MediumTest;

import com.android.providers.contacts.testutil.DataUtil;
import com.android.providers.contacts.testutil.RawContactUtil;

/**
 * Unit tests for {@link GlobalSearchSupport}.
 * <p>
 * Run the test like this:
 * <p>
 * <code><pre>
 * adb shell am instrument -e class com.android.providers.contacts.GlobalSearchSupportTest -w \
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </pre></code>
 */
@MediumTest
public class GlobalSearchSupportTest extends BaseContactsProvider2Test {

    public void testSearchSuggestionsNotInDefaultDirectory() throws Exception {
        Account account = new Account("actname", "acttype");

        // Creating an AUTO_ADD group will exclude all ungrouped contacts from global search
        createGroup(account, "any", "any", 0 /* visible */, true /* auto-add */, false /* fav */);

        long rawContactId = RawContactUtil.createRawContact(mResolver, account);
        DataUtil.insertStructuredName(mResolver, rawContactId, "Deer", "Dough");

        // Remove the new contact from all groups
        mResolver.delete(Data.CONTENT_URI, Data.RAW_CONTACT_ID + "=" + rawContactId
                + " AND " + Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE + "'", null);

        Uri searchUri = new Uri.Builder().scheme("content").authority(ContactsContract.AUTHORITY)
                .appendPath(SearchManager.SUGGEST_URI_PATH_QUERY).appendPath("D").build();

        // If the contact is not in the "my contacts" group, nothing should be found
        Cursor c = mResolver.query(searchUri, null, null, null, null);
        assertEquals(0, c.getCount());
        c.close();
    }

    public void testSearchSuggestionsByNameWithPhoto() throws Exception {
        GoldenContact contact = new GoldenContactBuilder().name("Deer", "Dough").photo(
                loadTestPhoto()).build();
        new SuggestionTesterBuilder(contact).query("D").expectIcon1Uri(true).expectedText1(
                "Deer Dough").build().test();
    }

    public void testSearchSuggestionsByEmailWithPhoto() {
        GoldenContact contact = new GoldenContactBuilder().name("Deer", "Dough").photo(
                loadTestPhoto()).email("foo@acme.com").build();
        new SuggestionTesterBuilder(contact).query("foo@ac").expectIcon1Uri(true).expectedIcon2(
                String.valueOf(StatusUpdates.getPresenceIconResourceId(StatusUpdates.OFFLINE)))
                .expectedText1("Deer Dough").expectedText2("foo@acme.com").build().test();
    }

    public void testSearchSuggestionsByName() {
        GoldenContact contact = new GoldenContactBuilder().name("Deer", "Dough").company("Google")
                .build();
        new SuggestionTesterBuilder(contact).query("D").expectedText1("Deer Dough").expectedText2(
                null).build().test();
    }

    public void testSearchByNickname() {
        GoldenContact contact = new GoldenContactBuilder().name("Deer", "Dough").nickname(
                "Little Fawn").company("Google").build();
        new SuggestionTesterBuilder(contact).query("L").expectedText1("Deer Dough").expectedText2(
                "Little Fawn").build().test();
    }

    public void testSearchByCompany() {
        GoldenContact contact = new GoldenContactBuilder().name("Deer", "Dough").company("Google")
                .build();
        new SuggestionTesterBuilder(contact).query("G").expectedText1("Deer Dough").expectedText2(
                "Google").build().test();
    }

    public void testSearchByTitleWithCompany() {
        GoldenContact contact = new GoldenContactBuilder().name("Deer", "Dough").company("Google")
                .title("Software Engineer").build();
        new SuggestionTesterBuilder(contact).query("S").expectIcon1Uri(false).expectedText1(
                "Deer Dough").expectedText2("Software Engineer, Google").build().test();
    }

    public void testSearchSuggestionsByPhoneNumberOnNonPhone() throws Exception {
        getContactsProvider().setIsPhone(false);

        GoldenContact contact = new GoldenContactBuilder().name("Deer", "Dough").photo(
                loadTestPhoto()).phone("1-800-4664-411").build();
        new SuggestionTesterBuilder(contact).query("1800").expectIcon1Uri(true).expectedText1(
                "Deer Dough").expectedText2("1-800-4664-411").build().test();
    }

    /**
     * Tests that the quick search suggestion returns the expected contact
     * information.
     */
    private final class SuggestionTester {

        private final GoldenContact contact;

        private final String query;

        private final boolean expectIcon1Uri;

        private final String expectedIcon2;

        private final String expectedText1;

        private final String expectedText2;

        public SuggestionTester(SuggestionTesterBuilder builder) {
            contact = builder.contact;
            query = builder.query;
            expectIcon1Uri = builder.expectIcon1Uri;
            expectedIcon2 = builder.expectedIcon2;
            expectedText1 = builder.expectedText1;
            expectedText2 = builder.expectedText2;
        }

        /**
         * Tests suggest and refresh queries from quick search box, then deletes the contact from
         * the data base.
         */
        public void test() {

            testQsbSuggest();
            testContactIdQsbRefresh();
            testLookupKeyQsbRefresh();

            // Cleanup
            contact.delete();
        }

        /**
         * Tests that the contacts provider return the appropriate information from the golden
         * contact in response to the suggestion query from the quick search box.
         */
        private void testQsbSuggest() {

            Uri searchUri = new Uri.Builder().scheme("content").authority(
                    ContactsContract.AUTHORITY).appendPath(SearchManager.SUGGEST_URI_PATH_QUERY)
                    .appendPath(query).build();

            Cursor c = mResolver.query(searchUri, null, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();

            String icon1 = c.getString(c.getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1));
            if (expectIcon1Uri) {
                assertTrue(icon1.startsWith("content:"));
            } else {
                assertEquals(String.valueOf(com.android.internal.R.drawable.ic_contact_picture),
                        icon1);
            }

            // SearchManager does not declare a constant for _id
            ContentValues values = getContactValues();
            assertCursorValues(c, values);

            c.close();
        }

        /**
         * Returns the expected Quick Search Box content values for the golden contact.
         */
        private ContentValues getContactValues() {

            ContentValues values = new ContentValues();
            values.put("_id", contact.getContactId());
            values.put(SearchManager.SUGGEST_COLUMN_TEXT_1, expectedText1);
            values.put(SearchManager.SUGGEST_COLUMN_TEXT_2, expectedText2);

            values.put(SearchManager.SUGGEST_COLUMN_ICON_2, expectedIcon2);
            values.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                    Contacts.getLookupUri(contact.getContactId(), contact.getLookupKey())
                            .toString());
            values.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, contact.getLookupKey());
            values.put(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, query);
            return values;
        }

        /**
         * Returns the expected Quick Search Box content values for the golden contact.
         */
        private ContentValues getRefreshValues() {

            ContentValues values = new ContentValues();
            values.put("_id", contact.getContactId());
            values.put(SearchManager.SUGGEST_COLUMN_TEXT_1, expectedText1);
            values.put(SearchManager.SUGGEST_COLUMN_TEXT_2, expectedText2);

            values.put(SearchManager.SUGGEST_COLUMN_ICON_2, expectedIcon2);
            values.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, contact.getLookupKey());
            values.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, contact.getLookupKey());
            return values;
        }

        /**
         * Performs the refresh query and returns a cursor to the results.
         *
         * @param refreshId the final component path of the refresh query, which identifies which
         *        contact to refresh.
         */
        private Cursor refreshQuery(String refreshId) {

            // See if the same result is returned by a shortcut refresh
            Uri refershUri = ContactsContract.AUTHORITY_URI.buildUpon().appendPath(
                    SearchManager.SUGGEST_URI_PATH_SHORTCUT)
                    .appendPath(refreshId)
                    .appendQueryParameter(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, query)
                    .build();

            String[] projection = new String[] {
                    SearchManager.SUGGEST_COLUMN_ICON_1, SearchManager.SUGGEST_COLUMN_ICON_2,
                    SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2,
                    SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
                    SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "_id",
            };

            return mResolver.query(refershUri, projection, null, null, null);
        }

        /**
         * Tests that the contacts provider returns an empty result in response to a refresh query
         * from the quick search box that uses the contact id to identify the contact.  The empty
         * result indicates that the shortcut is no longer valid, and the QSB will replace it with
         * a new-style shortcut the next time they click on the contact.
         *
         * @see #testLookupKeyQsbRefresh()
         */
        private void testContactIdQsbRefresh() {

            Cursor c = refreshQuery(String.valueOf(contact.getContactId()));
            try {
                assertEquals("Record count", 0, c.getCount());
            } finally {
                c.close();
            }
        }

        /**
         * Tests that the contacts provider return the appropriate information from the golden
         * contact in response to the refresh query from the quick search box.  The refresh query
         * uses the currently-supported mechanism of identifying the contact by the lookup key,
         * which is more stable than the previously used contact id.
         */
        private void testLookupKeyQsbRefresh() {

            Cursor c = refreshQuery(contact.getLookupKey());
            try {
                assertEquals("Record count", 1, c.getCount());
                c.moveToFirst();
                assertCursorValues(c, getRefreshValues());
            } finally {
                c.close();
            }
        }
    }

    /**
     * Builds {@link SuggestionTester} objects. Unspecified boolean objects default to
     * false. Unspecified String objects default to null.
     */
    private final class SuggestionTesterBuilder {

        private final GoldenContact contact;

        private String query;

        private boolean expectIcon1Uri;

        private String expectedIcon2;

        private String expectedText1;

        private String expectedText2;

        public SuggestionTesterBuilder(GoldenContact contact) {
            this.contact = contact;
        }

        /**
         * Builds the {@link SuggestionTester} specified by this builder.
         */
        public SuggestionTester build() {
            return new SuggestionTester(this);
        }

        /**
         * The text of the user's query to quick search (i.e., what they typed
         * in the search box).
         */
        public SuggestionTesterBuilder query(String value) {
            query = value;
            return this;
        }

        /**
         * Whether to set Icon1, which in practice is the contact's photo.
         * <p>
         * TODO(tomo): Replace with actual expected value? This might be hard
         * because the values look non-deterministic, such as
         * "content://com.android.contacts/contacts/2015/photo"
         */
        public SuggestionTesterBuilder expectIcon1Uri(boolean value) {
            expectIcon1Uri = value;
            return this;
        }

        /**
         * The value for Icon2, which in practice is the contact's Chat status
         * (available, busy, etc.)
         */
        public SuggestionTesterBuilder expectedIcon2(String value) {
            expectedIcon2 = value;
            return this;
        }

        /**
         * First line of suggestion text expected to be returned (required).
         */
        public SuggestionTesterBuilder expectedText1(String value) {
            expectedText1 = value;
            return this;
        }

        /**
         * Second line of suggestion text expected to return (optional).
         */
        public SuggestionTesterBuilder expectedText2(String value) {
            expectedText2 = value;
            return this;
        }
    }
}
