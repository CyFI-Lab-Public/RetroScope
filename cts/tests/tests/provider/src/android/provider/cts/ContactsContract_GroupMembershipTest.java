/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.provider.cts;


import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.cts.ContactsContract_TestDataBuilder.TestData;
import android.provider.cts.ContactsContract_TestDataBuilder.TestGroup;
import android.provider.cts.ContactsContract_TestDataBuilder.TestRawContact;
import android.test.InstrumentationTestCase;

public class ContactsContract_GroupMembershipTest extends InstrumentationTestCase {
    private ContactsContract_TestDataBuilder mBuilder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ContentResolver contentResolver =
                getInstrumentation().getTargetContext().getContentResolver();
        ContentProviderClient provider =
                contentResolver.acquireContentProviderClient(ContactsContract.AUTHORITY);
        mBuilder = new ContactsContract_TestDataBuilder(provider);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mBuilder.cleanup();
    }

    public void testAddGroupMembershipWithGroupRowId() throws Exception {
        TestRawContact rawContact = mBuilder.newRawContact().insert();
        TestGroup group = mBuilder.newGroup().insert();
        TestData groupMembership = rawContact.newDataRow(GroupMembership.CONTENT_ITEM_TYPE)
                .with(GroupMembership.GROUP_ROW_ID, group.getId())
                .insert();

        groupMembership.load();
        groupMembership.assertColumn(GroupMembership.RAW_CONTACT_ID, rawContact.getId());
        groupMembership.assertColumn(GroupMembership.GROUP_ROW_ID, group.getId());
    }

    public void testAddGroupMembershipWithGroupSourceId() throws Exception {
        TestRawContact rawContact = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_type")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .insert();
        TestGroup group = mBuilder.newGroup()
                .with(Groups.SOURCE_ID, "test_source_id")
                .with(Groups.ACCOUNT_TYPE, "test_type")
                .with(Groups.ACCOUNT_NAME, "test_name")
                .insert();
        TestData groupMembership = rawContact.newDataRow(GroupMembership.CONTENT_ITEM_TYPE)
                .with(GroupMembership.GROUP_SOURCE_ID, "test_source_id")
                .insert();

        groupMembership.load();
        groupMembership.assertColumn(GroupMembership.RAW_CONTACT_ID, rawContact.getId());
        groupMembership.assertColumn(GroupMembership.GROUP_SOURCE_ID, "test_source_id");
        groupMembership.assertColumn(GroupMembership.GROUP_ROW_ID, group.getId());
    }

    public void testAddGroupMembershipWithUnknownGroupSourceId() throws Exception {
        TestRawContact rawContact = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_type")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .insert();
        TestData groupMembership = rawContact.newDataRow(GroupMembership.CONTENT_ITEM_TYPE)
                .with(GroupMembership.GROUP_SOURCE_ID, "test_source_id")
                .insert();
        TestGroup group = mBuilder.newGroup()
                .with(Groups.ACCOUNT_TYPE, "test_type")
                .with(Groups.ACCOUNT_NAME, "test_name")
                .with(Groups.SOURCE_ID, "test_source_id")
                .with(Groups.DELETED, 0)
                .loadUsingValues();

        groupMembership.load();
        groupMembership.assertColumn(GroupMembership.RAW_CONTACT_ID, rawContact.getId());
        groupMembership.assertColumn(GroupMembership.GROUP_SOURCE_ID, "test_source_id");
        groupMembership.assertColumn(GroupMembership.GROUP_ROW_ID, group.getId());

        group.deletePermanently();
    }
}

