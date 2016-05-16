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

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.Extensions;
import android.provider.Contacts.GroupMembership;
import android.provider.Contacts.Groups;
import android.provider.Contacts.Organizations;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.Contacts.Photos;
import android.provider.Contacts.Presence;
import android.provider.Contacts.Settings;
import android.provider.ContactsContract;
import android.test.suitebuilder.annotation.MediumTest;

import java.io.IOException;

/**
 * Tests for legacy contacts APIs.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.android.providers.contacts.LegacyContactsProviderTest -w \
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 *
 * Note that this SHOULD be a large test, but had to be bumped down to medium due to a bug in the
 * SQLite cleanup code.
 */
@MediumTest
@SuppressWarnings("deprecation")
public class LegacyContactsProviderTest extends BaseContactsProvider2Test {

    @Override
    protected Class<? extends ContentProvider> getProviderClass() {
       return SynchronousContactsProvider2.class;
    }

    @Override
    protected String getAuthority() {
        return Contacts.AUTHORITY;
    }

    public void testPeopleInsert() {
        ContentValues values = new ContentValues();
        putContactValues(values);

        Uri uri = mResolver.insert(People.CONTENT_URI, values);
        assertStoredValues(uri, values);
        assertSelection(People.CONTENT_URI, values, "people", People._ID, ContentUris.parseId(uri));
    }

    public void testPeopleUpdate() {
        ContentValues values = new ContentValues();
        putContactValues(values);

        Uri uri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(uri);
        assertStoredValues(uri, values);
        assertSelection(People.CONTENT_URI, values, "people", People._ID, personId);

        values.clear();
        putContactValues2(values);
        mResolver.update(uri, values, null, null);
        assertStoredValues(uri, values);

        values.clear();
        putContactValues(values);
        mResolver.update(People.CONTENT_URI, values, People._ID + "=" + personId, null);
        assertStoredValues(uri, values);
    }

    public void testPeopleDelete() {
        ContentValues values = new ContentValues();
        values.put(People.NAME, "John Doe");
        Uri personId = mResolver.insert(People.CONTENT_URI, values);
        mResolver.delete(personId, null, null);

        Cursor c = mResolver.query(personId, null, People.NAME + "='John Doe'" , null, null);
        assertEquals("Record count after deletion", 0, c.getCount());
        c.close();

        try {
            mResolver.query(People.DELETED_CONTENT_URI, null, null, null, null);
        } catch (UnsupportedOperationException e) {
            // Expected exception
        }
    }

    public void testPeopleFilter() {
        ContentValues values = new ContentValues();
        values.put(People.NAME, "Deer Doe");
        mResolver.insert(People.CONTENT_URI, values);

        values.clear();
        values.put(People.NAME, "Dear Dough");
        mResolver.insert(People.CONTENT_URI, values);

        values.clear();
        values.put(People.NAME, "D.R. Dauwe");
        mResolver.insert(People.CONTENT_URI, values);

        assertFilteredContacts("d", "Deer Doe", "Dear Dough", "D.R. Dauwe");
        assertFilteredContacts("de", "Deer Doe", "Dear Dough");
        assertFilteredContacts("dee", "Deer Doe");
        assertFilteredContacts("der");
    }

    public void testDefaultDisplayName() {
        ContentValues values = new ContentValues();
        values.put(People.NAME, "John Doe");
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        assertStoredValue(personUri, People.DISPLAY_NAME, "John Doe");
    }

    public void testPrimaryOrganization() {
        ContentValues values = new ContentValues();
        final Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        // Primary
        values.clear();
        values.put(Organizations.ISPRIMARY, 1);
        values.put(Organizations.COMPANY, "Google");
        values.put(Organizations.TYPE, Organizations.TYPE_WORK);
        values.put(Organizations.PERSON_ID, personId);
        Uri orgUri1 = mResolver.insert(Organizations.CONTENT_URI, values);

        // Non-primary
        values.clear();
        values.put(Organizations.COMPANY, "Acme");
        values.put(Organizations.TYPE, Organizations.TYPE_WORK);
        values.put(Organizations.PERSON_ID, personId);
        Uri orgUri2 = mResolver.insert(Organizations.CONTENT_URI, values);

        values.clear();
        values.put(People.PRIMARY_ORGANIZATION_ID, ContentUris.parseId(orgUri1));
        values.put(People.DISPLAY_NAME, "Google");
        assertStoredValues(personUri, values);

        // Remove the original primary organization
        mResolver.delete(orgUri1, null, null);

        values.clear();
        values.put(People.PRIMARY_ORGANIZATION_ID, ContentUris.parseId(orgUri2));
        values.put(People.DISPLAY_NAME, "Acme");
        assertStoredValues(personUri, values);

        // Remove the remaining organization
        mResolver.delete(orgUri2, null, null);

        values.clear();
        values.putNull(People.PRIMARY_ORGANIZATION_ID);
        values.putNull(People.DISPLAY_NAME);
        assertStoredValues(personUri, values);
    }

    public void testPrimaryPhone() {
        ContentValues values = new ContentValues();
        putContactValuesExceptName(values);

        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        // Primary
        values.clear();
        values.put(Phones.ISPRIMARY, 1);
        values.put(Phones.TYPE, Phones.TYPE_WORK);
        values.put(Phones.PERSON_ID, personId);
        values.put(Phones.NUMBER, "12345");
        Uri phoneUri1 = mResolver.insert(Phones.CONTENT_URI, values);

        // Non-primary
        values.clear();
        values.put(Phones.TYPE, Phones.TYPE_WORK);
        values.put(Phones.PERSON_ID, personId);
        values.put(Phones.NUMBER, "67890");
        Uri phoneUri2 = mResolver.insert(Phones.CONTENT_URI, values);

        values.clear();
        values.put(People.PRIMARY_PHONE_ID, ContentUris.parseId(phoneUri1));
        values.put(People.DISPLAY_NAME, "12345");
        assertStoredValues(personUri, values);

        values.clear();
        putContactValuesExceptName(values);
        values.put(People.PRIMARY_PHONE_ID, ContentUris.parseId(phoneUri1));
        assertStoredValues(phoneUri2, values);

        // Remove the primary phone number
        mResolver.delete(phoneUri1, null, null);

        values.clear();
        values.put(People.PRIMARY_PHONE_ID, ContentUris.parseId(phoneUri2));
        values.put(People.DISPLAY_NAME, "67890");
        assertStoredValues(personUri, values);

        // Remove the remaining phone number
        mResolver.delete(phoneUri2, null, null);

        values.clear();
        values.putNull(People.PRIMARY_PHONE_ID);
        values.putNull(People.DISPLAY_NAME);
        assertStoredValues(personUri, values);
    }

    public void testEmailCrud() {
        ContentValues values = new ContentValues();
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        // Primary
        values.clear();
        values.put(ContactMethods.PERSON_ID, personId);
        values.put(ContactMethods.KIND, Contacts.KIND_EMAIL);
        values.put(ContactMethods.TYPE, ContactMethods.TYPE_HOME);
        values.put(ContactMethods.DATA, "foo@acme.com");
        values.put(ContactMethods.ISPRIMARY, 1);
        Uri emailUri1 = mResolver.insert(ContactMethods.CONTENT_URI, values);

        assertEquals(ContactMethods.CONTENT_EMAIL_ITEM_TYPE, mResolver.getType(emailUri1));

        assertStoredValues(ContactMethods.CONTENT_URI,
                ContactMethods._ID + "=" + ContentUris.parseId(emailUri1), null, values);
        assertStoredValues(ContactMethods.CONTENT_EMAIL_URI,
                ContactMethods._ID + "=" + ContentUris.parseId(emailUri1), null, values);
    }

    public void testPrimaryEmail() {
        ContentValues values = new ContentValues();
        putContactValuesExceptName(values);
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        // Primary
        values.clear();
        values.put(ContactMethods.PERSON_ID, personId);
        values.put(ContactMethods.KIND, Contacts.KIND_EMAIL);
        values.put(ContactMethods.TYPE, ContactMethods.TYPE_HOME);
        values.put(ContactMethods.DATA, "foo@acme.com");
        values.put(ContactMethods.ISPRIMARY, 1);
        Uri emailUri1 = mResolver.insert(ContactMethods.CONTENT_URI, values);

        // Non-primary
        values.clear();
        values.put(ContactMethods.PERSON_ID, personId);
        values.put(ContactMethods.KIND, Contacts.KIND_EMAIL);
        values.put(ContactMethods.TYPE, ContactMethods.TYPE_WORK);
        values.put(ContactMethods.DATA, "bar@acme.com");
        Uri emailUri2 = mResolver.insert(ContactMethods.CONTENT_URI, values);

        values.clear();
        values.put(People.PRIMARY_EMAIL_ID, ContentUris.parseId(emailUri1));
        values.put(People.DISPLAY_NAME, "foo@acme.com");
        assertStoredValues(personUri, values);

        values.clear();
        putContactValuesExceptName(values);
        values.put(People.PRIMARY_EMAIL_ID, ContentUris.parseId(emailUri1));
        assertStoredValues(emailUri2, values);

        // Remove the primary email
        mResolver.delete(emailUri1, null, null);

        values.clear();
        values.put(People.PRIMARY_EMAIL_ID, ContentUris.parseId(emailUri2));
        values.put(People.DISPLAY_NAME, "bar@acme.com");
        assertStoredValues(personUri, values);

        // Remove the remaining email
        mResolver.delete(emailUri2, null, null);

        values.clear();
        values.putNull(People.PRIMARY_EMAIL_ID);
        values.putNull(People.DISPLAY_NAME);
        assertStoredValues(personUri, values);
    }

    public void testMarkAsContacted() {
        ContentValues values = new ContentValues();
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        int timesContactedBefore =
                Integer.parseInt(getStoredValue(personUri, People.TIMES_CONTACTED));
        long timeBefore = System.currentTimeMillis();
        People.markAsContacted(mResolver, personId);
        long timeAfter = System.currentTimeMillis();

        long lastContacted = Long.parseLong(getStoredValue(personUri, People.LAST_TIME_CONTACTED));
        int timesContactedAfter =
            Integer.parseInt(getStoredValue(personUri, People.TIMES_CONTACTED));

        assertTrue(lastContacted >= timeBefore);
        assertTrue(lastContacted <= timeAfter);
        assertEquals(timesContactedAfter, timesContactedBefore + 1);
    }

    public void testOrganizationsInsert() {
        ContentValues values = new ContentValues();
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(Organizations.COMPANY, "Sierra");
        values.put(Organizations.PERSON_ID, personId);
        values.put(Organizations.TYPE, Organizations.TYPE_CUSTOM);
        values.put(Organizations.LABEL, "Club");
        values.put(Organizations.TITLE, "Member");
        values.put(Organizations.ISPRIMARY, 1);

        Uri uri = mResolver.insert(Organizations.CONTENT_URI, values);
        assertStoredValues(uri, values);
        assertSelection(Organizations.CONTENT_URI, values,
                "organizations", Organizations._ID, ContentUris.parseId(uri));

        assertPersonIdConstraint(Organizations.CONTENT_URI, Organizations.TYPE,
                Organizations.TYPE_WORK);

        assertTypeAndLabelConstraints(Organizations.CONTENT_URI, Organizations.PERSON_ID, personId,
                Organizations.TYPE, Organizations.TYPE_CUSTOM, Organizations.TYPE_OTHER,
                Organizations.LABEL);


        Uri twigUri = Uri.withAppendedPath(personUri, Organizations.CONTENT_DIRECTORY);
        assertStoredValues(twigUri, values);

        Uri twigUriWithId = ContentUris.withAppendedId(twigUri, ContentUris.parseId(uri));
        assertStoredValues(twigUriWithId, values);
    }

    public void testOrganizationsUpdate() {
        ContentValues values = new ContentValues();
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(Organizations.COMPANY, "Sierra");
        values.put(Organizations.PERSON_ID, personId);
        values.put(Organizations.TYPE, Organizations.TYPE_CUSTOM);
        values.put(Organizations.LABEL, "Club");
        values.put(Organizations.TITLE, "Member");
        values.put(Organizations.ISPRIMARY, 0);

        Uri uri = mResolver.insert(Organizations.CONTENT_URI, values);

        values.clear();
        values.put(Organizations.COMPANY, "Planetary");
        values.put(Organizations.PERSON_ID, personId);
        values.put(Organizations.TYPE, Organizations.TYPE_CUSTOM);
        values.put(Organizations.LABEL, "Society");
        values.put(Organizations.TITLE, "Chair");
        values.put(Organizations.ISPRIMARY, 1);

        mResolver.update(uri, values, null, null);

        assertStoredValues(uri, values);
    }

    public void testPhonesInsert() {
        ContentValues values = new ContentValues();
        putContactValues(values);
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(Phones.PERSON_ID, personId);
        values.put(Phones.TYPE, Phones.TYPE_CUSTOM);
        values.put(Phones.LABEL, "Directory");
        values.put(Phones.NUMBER, "1-800-4664-411");
        values.put(Phones.ISPRIMARY, 1);

        Uri uri = mResolver.insert(Phones.CONTENT_URI, values);
        ContentValues expectedResults[] = new ContentValues[1];
        // Adding another value to assert
        expectedResults[0] = new ContentValues(values);
        expectedResults[0].put(Phones.NUMBER_KEY, "11446640081");

        // The result is joined with People
        putContactValues(expectedResults[0]);
        assertStoredValues(uri, expectedResults);
        assertSelection(Phones.CONTENT_URI, values, "phones",
                Phones._ID, ContentUris.parseId(uri));

        // Access the phone through People
        Uri twigUri = Uri.withAppendedPath(personUri, People.Phones.CONTENT_DIRECTORY);
        assertStoredValues(twigUri, expectedResults);

        // Now the person should be joined with Phone
        values.clear();
        putContactValues(values);
        values.put(People.TYPE, Phones.TYPE_CUSTOM);
        values.put(People.LABEL, "Directory");
        values.put(People.NUMBER, "1-800-4664-411");
        assertStoredValues(personUri, values);

        assertPersonIdConstraint(Phones.CONTENT_URI, Phones.TYPE, Phones.TYPE_WORK);

        assertTypeAndLabelConstraints(Phones.CONTENT_URI, Phones.PERSON_ID, personId, Phones.TYPE,
                Phones.TYPE_CUSTOM, Phones.TYPE_OTHER, Phones.LABEL);
    }

    public void testPhonesUpdate() {
        ContentValues values = new ContentValues();
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(Phones.PERSON_ID, personId);
        values.put(Phones.TYPE, Phones.TYPE_CUSTOM);
        values.put(Phones.LABEL, "Directory");
        values.put(Phones.NUMBER, "1-800-4664-411");
        values.put(Phones.ISPRIMARY, 0);

        Uri uri = mResolver.insert(Phones.CONTENT_URI, values);

        values.clear();
        values.put(Phones.PERSON_ID, personId);
        values.put(Phones.TYPE, Phones.TYPE_FAX_HOME);
        values.putNull(Phones.LABEL);
        values.put(Phones.NUMBER, "1-800-555-4663");
        values.put(Phones.ISPRIMARY, 1);

        mResolver.update(uri, values, null, null);

        ContentValues[] expectedValues = new ContentValues[1];
        expectedValues[0] = values;
        expectedValues[0].put(Phones.NUMBER_KEY, "36645550081");
        assertStoredValues(uri, expectedValues);
    }

    public void testPhonesFilterQuery() {
        ContentValues values = new ContentValues();
        putContactValues(values);
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(Phones.PERSON_ID, personId);
        values.put(Phones.TYPE, Phones.TYPE_CUSTOM);
        values.put(Phones.LABEL, "Directory");
        values.put(Phones.NUMBER, "800-4664-411");
        values.put(Phones.ISPRIMARY, 1);

        Uri uri = mResolver.insert(Phones.CONTENT_URI, values);

        Uri filterUri1 = Uri.withAppendedPath(Phones.CONTENT_FILTER_URL, "18004664411");
        ContentValues[] expectedValues = new ContentValues[1];
        expectedValues[0] = values;
        expectedValues[0].put(Phones.NUMBER_KEY, "1144664008");
        assertStoredValues(filterUri1, expectedValues);

        Uri filterUri2 = Uri.withAppendedPath(Phones.CONTENT_FILTER_URL, "7773334444");
        assertEquals(0, getCount(filterUri2, null, null));
    }

    public void testEmailInsert() {
        assertContactMethodInsert(Contacts.KIND_EMAIL,
                ContactMethods.TYPE_CUSTOM, "Some other way", "foo@acme.com", null, true);
    }

    public void testEmailUpdate() {
        assertContactMethodUpdate(Contacts.KIND_EMAIL,
                ContactMethods.TYPE_CUSTOM, "Some other way", "foo@acme.com", null, false,
                ContactMethods.TYPE_HOME, null, "bar@acme.com", "aux", true);
    }

    public void testImInsertStandard() {
        assertContactMethodInsert(Contacts.KIND_IM,
                ContactMethods.TYPE_CUSTOM, "Some other way", "pre:3", "Bar", true);
    }

    public void testImUpdateStandard() {
        assertContactMethodUpdate(Contacts.KIND_IM,
                ContactMethods.TYPE_CUSTOM, "Some other way", "pre:3", "Bar", false,
                ContactMethods.TYPE_WORK, null, "pre:4", "Buz", true);
    }

    public void testImInsertCustom() {
        assertContactMethodInsert(Contacts.KIND_IM,
                ContactMethods.TYPE_CUSTOM, "Some other way", "custom:my_proto", "Bar", true);
    }

    public void testImUpdateCustom() {
        assertContactMethodUpdate(Contacts.KIND_IM,
                ContactMethods.TYPE_CUSTOM, "Some other way", "custom:my_proto", "Bar", false,
                ContactMethods.TYPE_HOME, null, "custom:my_other_proto", "Buz", true);
    }

    public void testPostalInsert() {
        assertContactMethodInsert(Contacts.KIND_POSTAL,
                ContactMethods.TYPE_CUSTOM, "Some other way", "Foo", "Bar", true);
    }

    public void testPostalUpdate() {
        assertContactMethodUpdate(Contacts.KIND_POSTAL,
                ContactMethods.TYPE_CUSTOM, "Some other way", "Foo", "Bar", false,
                ContactMethods.TYPE_WORK, null, "Biz", "Baz", true);
    }

    private void assertContactMethodInsert(int kind, int type, String label, String data,
            String auxData, boolean primary) {
        ContentValues values = new ContentValues();
        putContactValues(values);
        final Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(ContactMethods.PERSON_ID, personId);
        values.put(ContactMethods.KIND, kind);
        values.put(ContactMethods.TYPE, type);
        values.put(ContactMethods.LABEL, label);
        values.put(ContactMethods.DATA, data);
        values.put(ContactMethods.AUX_DATA, auxData);
        values.put(ContactMethods.ISPRIMARY, primary ? 1 : 0);

        Uri uri = mResolver.insert(ContactMethods.CONTENT_URI, values);

        // The result is joined with People
        putContactValues(values);
        assertStoredValues(uri, values);
        assertSelection(ContactMethods.CONTENT_URI, values, "contact_methods",
                ContactMethods._ID, ContentUris.parseId(uri));

        // Access the contact method through People
        Uri twigUri = Uri.withAppendedPath(personUri, People.ContactMethods.CONTENT_DIRECTORY);
        assertStoredValues(twigUri, values);

        assertPersonIdConstraint(ContactMethods.CONTENT_URI, ContactMethods.TYPE,
                ContactMethods.TYPE_WORK);

        assertTypeAndLabelConstraints(ContactMethods.CONTENT_URI, ContactMethods.PERSON_ID,
                personId, ContactMethods.TYPE, ContactMethods.TYPE_CUSTOM,
                ContactMethods.TYPE_OTHER, ContactMethods.LABEL);
    }

    private void assertContactMethodUpdate(int kind,
            int type1, String label1, String data1, String auxData1, boolean primary1,
            int type2, String label2, String data2, String auxData2, boolean primary2) {
        ContentValues values = new ContentValues();
        putContactValues(values);
        final Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(ContactMethods.PERSON_ID, personId);
        values.put(ContactMethods.KIND, kind);
        values.put(ContactMethods.TYPE, type1);
        values.put(ContactMethods.LABEL, label1);
        values.put(ContactMethods.DATA, data1);
        values.put(ContactMethods.AUX_DATA, auxData1);
        values.put(ContactMethods.ISPRIMARY, primary1 ? 1 : 0);

        Uri uri = mResolver.insert(ContactMethods.CONTENT_URI, values);

        values.clear();
        values.put(ContactMethods.TYPE, type2);
        values.put(ContactMethods.LABEL, label2);
        values.put(ContactMethods.DATA, data2);
        values.put(ContactMethods.AUX_DATA, auxData2);
        values.put(ContactMethods.ISPRIMARY, primary2 ? 1 : 0);
        mResolver.update(uri, values, null, null);

        assertStoredValues(uri, values);
    }

    public void testExtensionsInsert() {
        ContentValues values = new ContentValues();
        final Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(Extensions.PERSON_ID, personId);
        values.put(Extensions.NAME, "Foo");
        values.put(Extensions.VALUE, "Bar");

        Uri uri = mResolver.insert(Extensions.CONTENT_URI, values);
        assertStoredValues(uri, values);
        assertSelection(Extensions.CONTENT_URI, values, "extensions",
                Extensions._ID, ContentUris.parseId(uri));

        // Access the extensions through People
        Uri twigUri = Uri.withAppendedPath(personUri, People.Extensions.CONTENT_DIRECTORY);
        assertStoredValues(twigUri, values);
    }

    public void testExtensionsUpdate() {
        ContentValues values = new ContentValues();
        final Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(Extensions.PERSON_ID, personId);
        values.put(Extensions.NAME, "Foo");
        values.put(Extensions.VALUE, "Bar");

        Uri uri = mResolver.insert(Extensions.CONTENT_URI, values);

        values.clear();
        values.put(Extensions.NAME, "Biz");
        values.put(Extensions.VALUE, "Baz");
        mResolver.update(uri, values, null, null);

        assertStoredValues(uri, values);
    }

    public void testGroupsInsert() {
        ContentValues values = new ContentValues();
        values.put(Groups.NAME, "Galois");
        values.put(Groups.NOTES, "Abel");
        values.put(Groups.SYSTEM_ID, "12345");

        Uri groupUri = mResolver.insert(Groups.CONTENT_URI, values);
        assertStoredValues(groupUri, values);
    }

    public void testGroupsUpdate() {
        ContentValues values = new ContentValues();
        values.put(Groups.NAME, "Galois");
        values.put(Groups.NOTES, "Abel");
        values.put(Groups.SYSTEM_ID, "12345");

        Uri groupUri = mResolver.insert(Groups.CONTENT_URI, values);

        values.clear();
        values.put(Groups.NAME, "Klein");
        values.put(Groups.NOTES, "Vierergruppe");
        values.put(Groups.SYSTEM_ID, "1234");
        mResolver.update(groupUri, values, null, null);
        assertStoredValues(groupUri, values);
    }

    public void testGroupMembershipsInsert() {
        ContentValues values = new ContentValues();
        values.put(Groups.NOTES, "Abel");
        Uri groupUri = insertLegacyGroup("Galois", values);
        Uri personUri = insertPerson("Klein", values);
        Uri membershipUri = insertLegacyGroupMembership(groupUri, personUri, values);
        values.put(Groups.NAME, "Galois");
        values.put(Groups.NOTES, "Abel");

        assertStoredValues(membershipUri, values);
        assertSelection(GroupMembership.CONTENT_URI, values, "groupmembership",
                GroupMembership._ID, ContentUris.parseId(membershipUri));

        Uri personsGroupsUri = Uri.withAppendedPath(personUri, GroupMembership.CONTENT_DIRECTORY);
        assertStoredValues(personsGroupsUri, values);
    }

    public void testAddToGroup() {
        ContentValues values = new ContentValues();
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(Groups.NAME, "Galois");
        Uri groupUri = mResolver.insert(Groups.CONTENT_URI, values);

        People.addToGroup(mResolver, personId, "Galois");

        values.clear();
        values.put(GroupMembership.GROUP_ID, ContentUris.parseId(groupUri));
        values.put(GroupMembership.PERSON_ID, personId);

        Uri personsGroupsUri = Uri.withAppendedPath(personUri, GroupMembership.CONTENT_DIRECTORY);
        assertStoredValues(personsGroupsUri, values);
    }

    public void testGroupMembersByGroupName() {
        ContentValues values = new ContentValues();
        Uri groupUri1 = insertLegacyGroup("Galois", values);
        Uri personUri1 = insertPerson("Klein", values);
        insertLegacyGroupMembership(groupUri1, personUri1, values);

        Uri groupUri2 = insertLegacyGroup("Euler", values);
        Uri personUri2 = insertPerson("Lagrange", values);
        insertLegacyGroupMembership(groupUri2, personUri2, values);

        // NOTE: testing non-public API support
        assertStoredValue(Uri.parse("content://contacts/groups/name/Galois/members"),
                People.NAME, "Klein");
        assertStoredValue(Uri.parse("content://contacts/groups/name/Euler/members"),
                People.NAME, "Lagrange");
    }

    public void testPhotoUpdate() throws Exception {
        byte[] photo = loadPhotoFromResource(
                com.android.providers.contacts.tests.R.drawable.earth_small, PhotoSize.ORIGINAL);
        byte[] thumbnailedPhoto = loadPhotoFromResource(
                com.android.providers.contacts.tests.R.drawable.earth_small, PhotoSize.THUMBNAIL);

        ContentValues values = new ContentValues();
        Uri personUri = mResolver.insert(People.CONTENT_URI, values);

        values.clear();
        values.put(Photos.DATA, photo);
        values.put(Photos.LOCAL_VERSION, "10");
        // FIXME this column was unavailable for update in legacy ContactsProvider
        // values.put(Photos.DOWNLOAD_REQUIRED, 1);
        values.put(Photos.EXISTS_ON_SERVER, 1);
        values.put(Photos.SYNC_ERROR, "404 does not exist");

        Uri photoUri = Uri.withAppendedPath(personUri, Photos.CONTENT_DIRECTORY);
        mResolver.update(photoUri, values, null, null);
        values.put(Photos.DATA, thumbnailedPhoto);
        assertStoredValues(photoUri, values);

        long photoId = Long.parseLong(getStoredValue(photoUri, Photos._ID));

        values.put(Photos.LOCAL_VERSION, "11");
        values.put(Photos.DATA, photo);
        Uri contentUri = ContentUris.withAppendedId(Photos.CONTENT_URI, photoId);
        mResolver.update(contentUri, values, null, null);
        values.put(Photos.DATA, thumbnailedPhoto);
        assertStoredValues(contentUri, values);
        assertStoredValues(photoUri, values);
    }

    /**
     * Capturing the search suggestion requirements in test cases as a reference.
     */
    public void testSearchSuggestionsNotInMyContacts() throws Exception {
        // We don't provide compatibility for search suggestions
        return;
    }

    /**
     * Capturing the search suggestion requirements in test cases as a reference.
     */
    public void testSearchSuggestionsByName() throws Exception {

        // We don't provide compatibility for search suggestions
        return;
    }

    private void assertSearchSuggestion(boolean name, boolean photo, boolean organization,
            boolean phone, boolean email, String query, boolean expectIcon1Uri, String expectedIcon2,
            String expectedText1, String expectedText2) throws IOException {
        ContentValues values = new ContentValues();

        if (name) {
            values.put(People.NAME, "Deer Dough");
        }

        final Uri personUri = mResolver.insert(People.CONTENT_URI, values);
        long personId = ContentUris.parseId(personUri);

        People.addToMyContactsGroup(mResolver, personId);

        if (photo) {
            values.clear();
            byte[] photoData = loadTestPhoto();
            values.put(Photos.DATA, photoData);
            values.put(Photos.LOCAL_VERSION, "1");
            values.put(Photos.EXISTS_ON_SERVER, 0);
            Uri photoUri = Uri.withAppendedPath(personUri, Photos.CONTENT_DIRECTORY);
            mResolver.update(photoUri, values, null, null);
        }

        if (organization) {
            values.clear();
            values.put(Organizations.ISPRIMARY, 1);
            values.put(Organizations.COMPANY, "Google");
            values.put(Organizations.TYPE, Organizations.TYPE_WORK);
            values.put(Organizations.PERSON_ID, personId);
            mResolver.insert(Organizations.CONTENT_URI, values);
        }

        if (email) {
            values.clear();
            values.put(ContactMethods.PERSON_ID, personId);
            values.put(ContactMethods.KIND, Contacts.KIND_EMAIL);
            values.put(ContactMethods.TYPE, ContactMethods.TYPE_HOME);
            values.put(ContactMethods.DATA, "foo@acme.com");
            values.put(ContactMethods.ISPRIMARY, 1);
            mResolver.insert(ContactMethods.CONTENT_URI, values);


            String protocol = ContactMethods
                    .encodePredefinedImProtocol(ContactMethods.PROTOCOL_GOOGLE_TALK);
            values.clear();
            values.put(Presence.IM_PROTOCOL, protocol);
            values.put(Presence.IM_HANDLE, "foo@acme.com");
            values.put(Presence.IM_ACCOUNT, "foo");
            values.put(Presence.PRESENCE_STATUS, Presence.OFFLINE);
            values.put(Presence.PRESENCE_CUSTOM_STATUS, "Coding for Android");
            mResolver.insert(Presence.CONTENT_URI, values);
        }

        if (phone) {
            values.clear();
            values.put(Phones.PERSON_ID, personId);
            values.put(Phones.TYPE, Phones.TYPE_HOME);
            values.put(Phones.NUMBER, "1-800-4664-411");
            values.put(Phones.ISPRIMARY, 1);
            mResolver.insert(Phones.CONTENT_URI, values);
        }

        Uri searchUri = new Uri.Builder().scheme("content").authority(Contacts.AUTHORITY)
                .appendPath(SearchManager.SUGGEST_URI_PATH_QUERY).appendPath(query).build();

        Cursor c = mResolver.query(searchUri, null, null, null, null);
        assertEquals(1, c.getCount());
        c.moveToFirst();
        values.clear();

        String icon1 = c.getString(c.getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1));
        if (expectIcon1Uri) {
            assertTrue(icon1.startsWith("content:"));
        } else {
            assertEquals(String.valueOf(com.android.internal.R.drawable.ic_contact_picture), icon1);
        }

        // SearchManager does not declare a constant for _id
        values.put("_id", personId);
        values.put(SearchManager.SUGGEST_COLUMN_ICON_2, expectedIcon2);
        values.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, personId);
        values.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, personId);
        values.put(SearchManager.SUGGEST_COLUMN_TEXT_1, expectedText1);
        values.put(SearchManager.SUGGEST_COLUMN_TEXT_2, expectedText2);
        assertCursorValues(c, values);
        c.close();

        // Cleanup
        mResolver.delete(personUri, null, null);
    }

    /**
     * Capturing the search suggestion requirements in test cases as a reference.
     */
    public void testSearchSuggestionsByPhoneNumber() throws Exception {
        // We don't provide compatibility for search suggestions
        return;
    }

    public void testSettings() throws Exception {
        mActor.addAuthority(ContactsContract.AUTHORITY);

        ContentValues values = new ContentValues();
        values.put(Settings._SYNC_ACCOUNT, "foo");
        values.put(Settings._SYNC_ACCOUNT_TYPE, "bar");
        values.put(Settings.KEY, Settings.SYNC_EVERYTHING);
        values.put(Settings.VALUE, 7);
        mResolver.update(Settings.CONTENT_URI, values, null, null);

        assertStoredValue(Settings.CONTENT_URI, Settings._SYNC_ACCOUNT + "='foo' AND "
                + Settings.KEY + "='" + Settings.SYNC_EVERYTHING + "'", null, Settings.VALUE, "7");

        assertStoredValue(ContactsContract.Settings.CONTENT_URI,
                ContactsContract.Settings.ACCOUNT_NAME + "='foo'",
                null, ContactsContract.Settings.SHOULD_SYNC, "7");

        values.clear();
        values.put(ContactsContract.Settings.SHOULD_SYNC, 8);
        mResolver.update(ContactsContract.Settings.CONTENT_URI, values,
                ContactsContract.Settings.ACCOUNT_NAME + "='foo'", null);

        assertStoredValue(Settings.CONTENT_URI, Settings._SYNC_ACCOUNT + "='foo' AND "
                + Settings.KEY + "='" + Settings.SYNC_EVERYTHING + "'", null, Settings.VALUE, "8");
    }

    private Uri insertPerson(String name, ContentValues values) {
        values.put(People.NAME, name);
        return mResolver.insert(People.CONTENT_URI, values);
    }

    private Uri insertLegacyGroup(String name, ContentValues values) {
        values.put(Groups.NAME, name);
        return mResolver.insert(Groups.CONTENT_URI, values);
    }

    private Uri insertLegacyGroupMembership(Uri groupUri, Uri personUri, ContentValues values) {
        long groupId = ContentUris.parseId(groupUri);
        long personId = ContentUris.parseId(personUri);

        values.clear();
        values.put(GroupMembership.GROUP_ID, groupId);
        values.put(GroupMembership.PERSON_ID, personId);
        Uri membershipUri = mResolver.insert(GroupMembership.CONTENT_URI, values);
        return membershipUri;
    }

    private void putContactValues(ContentValues values) {
        putContactValuesExceptName(values);
        values.put(People.NAME, "Deer Dough");
    }

    private void putContactValuesExceptName(ContentValues values) {
        // Populating only unhidden columns
        values.put(People.NOTES, "Cash Cow");
        values.put(People.TIMES_CONTACTED, 3);
        values.put(People.LAST_TIME_CONTACTED, 10);
        values.put(People.CUSTOM_RINGTONE, "ringtone");
        values.put(People.SEND_TO_VOICEMAIL, 1);
        values.put(People.STARRED, 1);
    }

    private void putContactValues2(ContentValues values) {
        // Populating only unhidden columns
        values.put(People.NAME, "John Doe");
        values.put(People.PHONETIC_NAME, "jon dawe");
        values.put(People.NOTES, "Poor Johnny");
        values.put(People.TIMES_CONTACTED, 4);
        values.put(People.LAST_TIME_CONTACTED, 11);
        values.put(People.CUSTOM_RINGTONE, "rangtone");
        values.put(People.SEND_TO_VOICEMAIL, 0);
        values.put(People.STARRED, 0);
    }

    private void assertFilteredContacts(String filter, String... expectedNames) {
        Uri filterUri = Uri.withAppendedPath(People.CONTENT_FILTER_URI, filter);
        Cursor c = mResolver.query(filterUri, null, null, null, null);
        try {
            assertEquals("Record count", expectedNames.length, c.getCount());
            int column = c.getColumnIndex(People.NAME);
            for (int i = 0; i < expectedNames.length; i++) {
                c.moveToNext();
                assertEquals(expectedNames[i], c.getString(column));
            }
        } finally {
            c.close();
        }
    }

    private void assertPersonIdConstraint(Uri uri, String typeColumn, int defaultType) {
        ContentValues values = new ContentValues();
        values.put(typeColumn, defaultType);
        try {
            mResolver.insert(uri, values);
            fail("Inserted row without person ID");
        } catch (Exception e) {
            // Exception expected
        }
    }

    private void assertTypeAndLabelConstraints(Uri uri, String personIdColumn, long personId,
            String typeColumn, int defaultType, int otherType, String labelColumn) {
        ContentValues values = new ContentValues();
        values.put(personIdColumn, personId);
        values.put(labelColumn, "Foo");

        try {
            mResolver.insert(uri, values);
            fail("Inserted row with label without defining type");
        } catch (Exception e) {
            // Exception expected
        }
    }

    protected void assertSelection(Uri uri, ContentValues values, String legacyTable,
            String idColumn, long id) {
        values.put(idColumn, id);
        String qualified = legacyTable + "." + idColumn;
        super.assertSelection(uri, values, qualified, id);
    }
}
