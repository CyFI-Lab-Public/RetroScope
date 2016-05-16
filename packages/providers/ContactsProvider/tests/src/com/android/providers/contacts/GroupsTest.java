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
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Settings;
import android.test.suitebuilder.annotation.MediumTest;

import com.google.android.collect.Lists;

import com.android.providers.contacts.testutil.RawContactUtil;

import java.util.ArrayList;

/**
 * Unit tests for {@link Groups} and {@link GroupMembership}.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.android.providers.contacts.GroupsTest -w \
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@MediumTest
public class GroupsTest extends BaseContactsProvider2Test {

    private static final String GROUP_GREY = "Grey";
    private static final String GROUP_RED = "Red";
    private static final String GROUP_GREEN = "Green";
    private static final String GROUP_BLUE = "Blue";

    private static final String PERSON_ALPHA = "Alpha";
    private static final String PERSON_BRAVO = "Bravo";
    private static final String PERSON_CHARLIE = "Charlie";
    private static final String PERSON_DELTA = "Delta";

    private static final String PHONE_ALPHA = "555-1111";
    private static final String PHONE_BRAVO_1 = "555-2222";
    private static final String PHONE_BRAVO_2 = "555-3333";
    private static final String PHONE_CHARLIE_1 = "555-4444";
    private static final String PHONE_CHARLIE_2 = "555-5555";

    public void testGroupSummary() {

        // Clear any existing data before starting
        // TODO make the provider wipe data automatically
        ((SynchronousContactsProvider2)mActor.provider).wipeData();

        // Create a handful of groups
        long groupGrey = mActor.createGroup(GROUP_GREY);
        long groupRed = mActor.createGroup(GROUP_RED);
        long groupGreen = mActor.createGroup(GROUP_GREEN);
        long groupBlue = mActor.createGroup(GROUP_BLUE);

        // Create a handful of contacts
        long contactAlpha = mActor.createRawContact(PERSON_ALPHA);
        long contactBravo = mActor.createRawContact(PERSON_BRAVO);
        long contactCharlie = mActor.createRawContact(PERSON_CHARLIE);
        long contactCharlieDupe = mActor.createRawContact(PERSON_CHARLIE);
        setAggregationException(
                AggregationExceptions.TYPE_KEEP_TOGETHER, contactCharlie, contactCharlieDupe);
        long contactDelta = mActor.createRawContact(PERSON_DELTA);

        assertAggregated(contactCharlie, contactCharlieDupe);

        // Add phone numbers to specific contacts
        mActor.createPhone(contactAlpha, PHONE_ALPHA);
        mActor.createPhone(contactBravo, PHONE_BRAVO_1);
        mActor.createPhone(contactBravo, PHONE_BRAVO_2);
        mActor.createPhone(contactCharlie, PHONE_CHARLIE_1);
        mActor.createPhone(contactCharlieDupe, PHONE_CHARLIE_2);

        // Add contacts to various mixture of groups. Grey will have all
        // contacts, Red only with phone numbers, Green with no phones, and Blue
        // with no contacts at all.
        mActor.createGroupMembership(contactAlpha, groupGrey);
        mActor.createGroupMembership(contactBravo, groupGrey);
        mActor.createGroupMembership(contactCharlie, groupGrey);
        mActor.createGroupMembership(contactDelta, groupGrey);

        mActor.createGroupMembership(contactAlpha, groupRed);
        mActor.createGroupMembership(contactBravo, groupRed);
        mActor.createGroupMembership(contactCharlie, groupRed);

        mActor.createGroupMembership(contactDelta, groupGreen);

        // Walk across groups summary cursor and verify returned counts.
        final Cursor cursor = mActor.resolver.query(Groups.CONTENT_SUMMARY_URI,
                Projections.PROJ_SUMMARY, null, null, null);

        // Require that each group has a summary row
        assertTrue("Didn't return summary for all groups", (cursor.getCount() == 4));

        while (cursor.moveToNext()) {
            final long groupId = cursor.getLong(Projections.COL_ID);
            final int summaryCount = cursor.getInt(Projections.COL_SUMMARY_COUNT);
            final int summaryWithPhones = cursor.getInt(Projections.COL_SUMMARY_WITH_PHONES);

            if (groupId == groupGrey) {
                // Grey should have four aggregates, three with phones.
                assertEquals("Incorrect Grey count", 4, summaryCount);
                assertEquals("Incorrect Grey with phones count", 3, summaryWithPhones);
            } else if (groupId == groupRed) {
                // Red should have 3 aggregates, all with phones.
                assertEquals("Incorrect Red count", 3, summaryCount);
                assertEquals("Incorrect Red with phones count", 3, summaryWithPhones);
            } else if (groupId == groupGreen) {
                // Green should have 1 aggregate, none with phones.
                assertEquals("Incorrect Green count", 1, summaryCount);
                assertEquals("Incorrect Green with phones count", 0, summaryWithPhones);
            } else if (groupId == groupBlue) {
                // Blue should have no contacts.
                assertEquals("Incorrect Blue count", 0, summaryCount);
                assertEquals("Incorrect Blue with phones count", 0, summaryWithPhones);
            } else {
                fail("Unrecognized group in summary cursor");
            }
        }
        cursor.close();
    }

    public void testGroupDirtySetOnChange() {
        Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI,
                createGroup(mAccount, "gsid1", "title1"));
        assertDirty(uri, true);
        clearDirty(uri);
        assertDirty(uri, false);
    }

    public void testMarkAsDirtyParameter() {
        Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI,
                createGroup(mAccount, "gsid1", "title1"));
        clearDirty(uri);
        Uri updateUri = setCallerIsSyncAdapter(uri, mAccount);

        ContentValues values = new ContentValues();
        values.put(Groups.NOTES, "New notes");
        mResolver.update(updateUri, values, null, null);
        assertDirty(uri, false);
    }

    public void testGroupDirtyClearedWhenSetExplicitly() {
        Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI,
                createGroup(mAccount, "gsid1", "title1"));
        assertDirty(uri, true);

        ContentValues values = new ContentValues();
        values.put(Groups.DIRTY, 0);
        values.put(Groups.NOTES, "other notes");
        assertEquals(1, mResolver.update(uri, values, null, null));

        assertDirty(uri, false);
    }

    public void testGroupDeletion1() {
        long groupId = createGroup(mAccount, "g1", "gt1");
        Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI, groupId);

        assertEquals(1, getCount(uri, null, null));
        mResolver.delete(uri, null, null);
        assertEquals(1, getCount(uri, null, null));
        assertStoredValue(uri, Groups.DELETED, "1");

        Uri permanentDeletionUri = setCallerIsSyncAdapter(uri, mAccount);
        mResolver.delete(permanentDeletionUri, null, null);
        assertEquals(0, getCount(uri, null, null));
    }

    public void testGroupDeletion2() {
        long groupId = createGroup(mAccount, "g1", "gt1");
        Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI, groupId);

        assertEquals(1, getCount(uri, null, null));
        Uri permanentDeletionUri = setCallerIsSyncAdapter(uri, mAccount);
        mResolver.delete(permanentDeletionUri, null, null);
        assertEquals(0, getCount(uri, null, null));
    }

    public void testGroupVersionUpdates() {
        Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI,
                createGroup(mAccount, "gsid1", "title1"));
        long version = getVersion(uri);
        ContentValues values = new ContentValues();
        values.put(Groups.TITLE, "title2");
        mResolver.update(uri, values, null, null);
        assertEquals(version + 1, getVersion(uri));
    }

    private interface Projections {
        public static final String[] PROJ_SUMMARY = new String[] {
            Groups._ID,
            Groups.SUMMARY_COUNT,
            Groups.SUMMARY_WITH_PHONES,
        };

        public static final int COL_ID = 0;
        public static final int COL_SUMMARY_COUNT = 1;
        public static final int COL_SUMMARY_WITH_PHONES = 2;
    }

    private static final Account sTestAccount = new Account("user@example.com", "com.example");
    private static final Account sSecondAccount = new Account("other@example.net", "net.example");
    private static final String GROUP_ID = "testgroup";

    public void assertRawContactVisible(long rawContactId, boolean expected) {
        final long contactId = this.queryContactId(rawContactId);
        assertContactVisible(contactId, expected);
    }

    public void assertContactVisible(long contactId, boolean expected) {
        final Cursor cursor = mResolver.query(Contacts.CONTENT_URI, new String[] {
            Contacts.IN_VISIBLE_GROUP
        }, Contacts._ID + "=" + contactId, null, null);
        assertTrue("Contact not found", cursor.moveToFirst());
        final boolean actual = (cursor.getInt(0) != 0);
        cursor.close();
        assertEquals("Unexpected visibility", expected, actual);
    }

    public ContentProviderOperation buildVisibleAssert(long contactId, boolean visible) {
        return ContentProviderOperation.newAssertQuery(Contacts.CONTENT_URI).withSelection(
                Contacts._ID + "=" + contactId + " AND " + Contacts.IN_VISIBLE_GROUP + "="
                        + (visible ? 1 : 0), null).withExpectedCount(1).build();
    }

    public void testDelayVisibleTransaction() throws RemoteException, OperationApplicationException {
        final ContentValues values = new ContentValues();

        final long groupId = this.createGroup(sTestAccount, GROUP_ID, GROUP_ID, 1);
        final Uri groupUri = ContentUris.withAppendedId(Groups.CONTENT_URI, groupId);

        // Create contact with specific membership
        final long rawContactId = RawContactUtil.createRawContact(this.mResolver, sTestAccount);
        final long contactId = this.queryContactId(rawContactId);
        final Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        this.insertGroupMembership(rawContactId, groupId);

        final ArrayList<ContentProviderOperation> oper = Lists.newArrayList();

        // Update visibility inside a transaction and assert that inside the
        // transaction it hasn't been updated yet.
        oper.add(buildVisibleAssert(contactId, true));
        oper.add(ContentProviderOperation.newUpdate(groupUri).withValue(Groups.GROUP_VISIBLE, 0)
                .build());
        oper.add(buildVisibleAssert(contactId, true));
        mResolver.applyBatch(ContactsContract.AUTHORITY, oper);

        // After previous transaction finished, visibility should be updated
        oper.clear();
        oper.add(buildVisibleAssert(contactId, false));
        mResolver.applyBatch(ContactsContract.AUTHORITY, oper);
    }

    public void testLocalSingleVisible() {
        final long rawContactId = RawContactUtil.createRawContact(this.mResolver);

        // Single, local contacts should always be visible
        assertRawContactVisible(rawContactId, true);
    }

    public void testLocalMixedVisible() {
        // Aggregate, when mixed with local, should become visible
        final long rawContactId1 = RawContactUtil.createRawContact(this.mResolver);
        final long rawContactId2 = RawContactUtil.createRawContact(this.mResolver, sTestAccount);

        final long groupId = this.createGroup(sTestAccount, GROUP_ID, GROUP_ID, 0);
        this.insertGroupMembership(rawContactId2, groupId);

        // Make sure they are still apart
        assertNotAggregated(rawContactId1, rawContactId2);
        assertRawContactVisible(rawContactId1, true);
        assertRawContactVisible(rawContactId2, false);

        // Force together and see what happens
        final ContentValues values = new ContentValues();
        values.put(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER);
        values.put(AggregationExceptions.RAW_CONTACT_ID1, rawContactId1);
        values.put(AggregationExceptions.RAW_CONTACT_ID2, rawContactId2);
        mResolver.update(AggregationExceptions.CONTENT_URI, values, null, null);

        assertRawContactVisible(rawContactId1, true);
        assertRawContactVisible(rawContactId2, true);
    }

    public void testUngroupedVisible() {
        final long rawContactId = RawContactUtil.createRawContact(this.mResolver, sTestAccount);

        final ContentValues values = new ContentValues();
        values.put(Settings.ACCOUNT_NAME, sTestAccount.name);
        values.put(Settings.ACCOUNT_TYPE, sTestAccount.type);
        values.put(Settings.UNGROUPED_VISIBLE, 0);
        mResolver.insert(Settings.CONTENT_URI, values);

        assertRawContactVisible(rawContactId, false);

        values.clear();
        values.put(Settings.UNGROUPED_VISIBLE, 1);
        mResolver.update(Settings.CONTENT_URI, values, Settings.ACCOUNT_NAME + "=? AND "
                + Settings.ACCOUNT_TYPE + "=?", new String[] {
                sTestAccount.name, sTestAccount.type
        });

        assertRawContactVisible(rawContactId, true);
    }

    public void testMultipleSourcesVisible() {
        final long rawContactId1 = RawContactUtil.createRawContact(this.mResolver, sTestAccount);
        final long rawContactId2 = RawContactUtil.createRawContact(this.mResolver, sSecondAccount);

        final long groupId = this.createGroup(sTestAccount, GROUP_ID, GROUP_ID, 0);
        this.insertGroupMembership(rawContactId1, groupId);

        // Make sure still invisible
        assertRawContactVisible(rawContactId1, false);
        assertRawContactVisible(rawContactId2, false);

        // Make group visible
        final ContentValues values = new ContentValues();
        values.put(Groups.GROUP_VISIBLE, 1);
        mResolver.update(Groups.CONTENT_URI, values, Groups._ID + "=" + groupId, null);

        assertRawContactVisible(rawContactId1, true);
        assertRawContactVisible(rawContactId2, false);

        // Force them together
        values.clear();
        values.put(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER);
        values.put(AggregationExceptions.RAW_CONTACT_ID1, rawContactId1);
        values.put(AggregationExceptions.RAW_CONTACT_ID2, rawContactId2);
        mResolver.update(AggregationExceptions.CONTENT_URI, values, null, null);

        assertRawContactVisible(rawContactId1, true);
        assertRawContactVisible(rawContactId2, true);

        // Make group invisible
        values.clear();
        values.put(Groups.GROUP_VISIBLE, 0);
        mResolver.update(Groups.CONTENT_URI, values, Groups._ID + "=" + groupId, null);

        assertRawContactVisible(rawContactId1, false);
        assertRawContactVisible(rawContactId2, false);

        // Turn on ungrouped for first
        values.clear();
        values.put(Settings.ACCOUNT_NAME, sTestAccount.name);
        values.put(Settings.ACCOUNT_TYPE, sTestAccount.type);
        values.put(Settings.UNGROUPED_VISIBLE, 1);
        mResolver.insert(Settings.CONTENT_URI, values);

        assertRawContactVisible(rawContactId1, false);
        assertRawContactVisible(rawContactId2, false);

        // Turn on ungrouped for second account
        values.clear();
        values.put(Settings.ACCOUNT_NAME, sSecondAccount.name);
        values.put(Settings.ACCOUNT_TYPE, sSecondAccount.type);
        values.put(Settings.UNGROUPED_VISIBLE, 1);
        mResolver.insert(Settings.CONTENT_URI, values);

        assertRawContactVisible(rawContactId1, true);
        assertRawContactVisible(rawContactId2, true);
    }
}
