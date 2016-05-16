/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static com.android.providers.contacts.TestUtils.cv;

import com.google.android.collect.Lists;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.RawContacts;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import java.util.ArrayList;

/**
 * Tests to make sure we're handling DB transactions properly in regard to two databases,
 * the profile db and the contacts db.
 */
@LargeTest
public class ContactsProvider2TransactionTest extends BaseContactsProvider2Test {
    private SynchronousContactsProvider2 mProvider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mProvider = (SynchronousContactsProvider2) getProvider();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        mProvider = null;
    }

    /**
     * Make sure we start/finish transactions on the right databases for insert.
     */
    public void testTransactionCallback_insert() {

        final ContentValues values = cv(RawContacts.LAST_TIME_CONTACTED, 12345);

        // Insert a raw contact.
        mProvider.resetTrasactionCallbackCalledFlags();
        mResolver.insert(RawContacts.CONTENT_URI, values);

        // Make sure we only COMMIT on the contacts DB, but there was no transaction on the
        // profile db.
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertNoTransactionsForProfileMode();


        // Insert a profile raw contact.
        mProvider.resetTrasactionCallbackCalledFlags();
        mResolver.insert(Profile.CONTENT_RAW_CONTACTS_URI, values);

        // Even though we only touched the profile DB, we also start and finish a transaction
        // on the contacts db.  AbstractContactsProvider does that to avoid deadlocks.
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertCommitTransactionCalledForProfileMode();
    }

    /**
     * Make sure we start/finish transactions on the right databases for update.
     */
    public void testTransactionCallback_update() {

        final ContentValues values = cv(RawContacts.LAST_TIME_CONTACTED, 12345);

        // Make sure to create a raw contact and a profile raw contact.
        mResolver.insert(RawContacts.CONTENT_URI, values);
        mResolver.insert(Profile.CONTENT_RAW_CONTACTS_URI, values);

        values.clear();
        values.put(RawContacts.LAST_TIME_CONTACTED, 99999);

        // Update all raw contacts.
        mProvider.resetTrasactionCallbackCalledFlags();
        assertTrue(mResolver.update(RawContacts.CONTENT_URI, values, null, null) > 0);

        // Make sure we only COMMIT on the contacts DB, but there was no transaction on the
        // profile db.
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertNoTransactionsForProfileMode();


        // Update all profile raw contacts.
        mProvider.resetTrasactionCallbackCalledFlags();
        assertTrue(mResolver.update(Profile.CONTENT_RAW_CONTACTS_URI, values, null, null) > 0);

        // Even though we only touched the profile DB, we also start and finish a transaction
        // on the contacts db.  AbstractContactsProvider does that to avoid deadlocks.
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertCommitTransactionCalledForProfileMode();
    }

    /**
     * Make sure we start/finish transactions on the right databases for delete.
     */
    public void testTransactionCallback_delete() {

        final ContentValues values = cv(RawContacts.LAST_TIME_CONTACTED, 12345);

        // Make sure to create a raw contact and a profile raw contact.
        mResolver.insert(RawContacts.CONTENT_URI, values);
        mResolver.insert(Profile.CONTENT_RAW_CONTACTS_URI, values);

        // Delete all raw contacts.
        mProvider.resetTrasactionCallbackCalledFlags();
        assertTrue(mResolver.delete(RawContacts.CONTENT_URI, null, null) > 0);

        // Make sure we only COMMIT on the contacts DB, but there was no transaction on the
        // profile db.
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertNoTransactionsForProfileMode();

        // Delete all profile raw contact.
        mProvider.resetTrasactionCallbackCalledFlags();
        assertTrue(mResolver.delete(Profile.CONTENT_RAW_CONTACTS_URI, null, null) > 0);

        // Even though we only touched the profile DB, we also start and finish a transaction
        // on the contacts db.  AbstractContactsProvider does that to avoid deadlocks.
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertCommitTransactionCalledForProfileMode();
    }
    /**
     * Make sure we start/finish transactions on the right databases for bulk insert.
     */
    public void testTransactionCallback_bulkInsert() {

        final ContentValues values = cv(RawContacts.LAST_TIME_CONTACTED, 12345);

        // Insert a raw contact.
        mProvider.resetTrasactionCallbackCalledFlags();
        mResolver.bulkInsert(RawContacts.CONTENT_URI, new ContentValues[] {values});

        // Make sure we only COMMIT on the contacts DB, but there was no transaction on the
        // profile db.
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertNoTransactionsForProfileMode();


        // Insert a profile raw contact.
        mProvider.resetTrasactionCallbackCalledFlags();
        mResolver.bulkInsert(Profile.CONTENT_RAW_CONTACTS_URI, new ContentValues[] {values});

        // Even though we only touched the profile DB, we also start and finish a transaction
        // on the contacts db.  AbstractContactsProvider does that to avoid deadlocks.
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertCommitTransactionCalledForProfileMode();
    }

    /**
     * Add an operation to create a raw contact.
     */
    private static void addInsertContactOperations(ArrayList<ContentProviderOperation> ops) {
        ContentProviderOperation.Builder b;
        b = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
        b.withValue(RawContacts.STARRED, 1);
        b.withValue(RawContacts.TIMES_CONTACTED, 200001);
        ops.add(b.build());

        b = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        b.withValueBackReference(Data.RAW_CONTACT_ID, ops.size() - 1);
        b.withValue(StructuredName.DISPLAY_NAME, "Regular Contact");
        b.withValue(StructuredName.GIVEN_NAME, "Regular");
        b.withValue(StructuredName.FAMILY_NAME, "Contact");
        b.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        ops.add(b.build());
    }

    /**
     * Check for a contact created that'll be created for {@link #addInsertContactOperations}.
     */
    private void checkStoredContact() {
        assertStoredValues(Contacts.CONTENT_URI, cv(
                Contacts.DISPLAY_NAME, "Regular Contact",
                RawContacts.TIMES_CONTACTED, 200001
                ));
    }

    /**
     * Add an operation to create a profile raw contact.
     */
    private static void addInsertProfileOperations(ArrayList<ContentProviderOperation> ops) {
        ContentProviderOperation.Builder b;
        b = ContentProviderOperation.newInsert(Profile.CONTENT_RAW_CONTACTS_URI);
        b.withValue(RawContacts.STARRED, 1);
        b.withValue(RawContacts.TIMES_CONTACTED, 100001);
        ops.add(b.build());

        b = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        b.withValueBackReference(Data.RAW_CONTACT_ID, ops.size() - 1);
        b.withValue(StructuredName.DISPLAY_NAME, "Profile Contact");
        b.withValue(StructuredName.GIVEN_NAME, "Profile");
        b.withValue(StructuredName.FAMILY_NAME, "Contact");
        b.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        ops.add(b.build());
    }

    /**
     * Check for a profile contact created that'll be created for
     * {@link #addInsertProfileOperations}.
     */
    private void checkStoredProfile() {
        assertStoredValues(Profile.CONTENT_URI, cv(
                Contacts.DISPLAY_NAME, "Profile Contact",
                RawContacts.TIMES_CONTACTED, 100001
                ));
    }

    public void testTransactionCallback_contactBatch() throws Exception {
        final ArrayList<ContentProviderOperation> ops = Lists.newArrayList();

        addInsertContactOperations(ops);

        mProvider.resetTrasactionCallbackCalledFlags();

        // Execute the operations.
        mResolver.applyBatch(ContactsContract.AUTHORITY, ops);

        // Check the result
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertNoTransactionsForProfileMode();

        checkStoredContact();
    }

    public void testTransactionCallback_profileBatch() throws Exception {
        final ArrayList<ContentProviderOperation> ops = Lists.newArrayList();

        addInsertProfileOperations(ops);

        mProvider.resetTrasactionCallbackCalledFlags();

        // Execute the operations.
        mResolver.applyBatch(ContactsContract.AUTHORITY, ops);

        // Check the result
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertCommitTransactionCalledForProfileMode();

        checkStoredProfile();
    }

    public void testTransactionCallback_mixedBatch() throws Exception {
        final ArrayList<ContentProviderOperation> ops = Lists.newArrayList();

        // Create a raw contact and a profile raw contact in a single batch.

        addInsertContactOperations(ops);
        addInsertProfileOperations(ops);

        mProvider.resetTrasactionCallbackCalledFlags();

        // Execute the operations.
        mResolver.applyBatch(ContactsContract.AUTHORITY, ops);

        // Check the result
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertCommitTransactionCalledForProfileMode();

        checkStoredProfile();
        checkStoredContact();
    }

    public void testTransactionCallback_mixedBatchReversed() throws Exception {
        final ArrayList<ContentProviderOperation> ops = Lists.newArrayList();

        // Create a profile raw contact and a raw contact in a single batch.

        addInsertProfileOperations(ops);
        addInsertContactOperations(ops);

        mProvider.resetTrasactionCallbackCalledFlags();

        // Execute the operations.
        mResolver.applyBatch(ContactsContract.AUTHORITY, ops);

        // Check the result
        mProvider.assertCommitTransactionCalledForContactMode();
        mProvider.assertCommitTransactionCalledForProfileMode();

        checkStoredProfile();
        checkStoredContact();
    }
}
