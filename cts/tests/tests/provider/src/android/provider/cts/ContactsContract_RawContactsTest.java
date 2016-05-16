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
import android.content.ContentValues;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.RawContacts;
import android.provider.cts.ContactsContract_TestDataBuilder.TestContact;
import android.provider.cts.ContactsContract_TestDataBuilder.TestRawContact;
import android.provider.cts.contacts.CommonDatabaseUtils;
import android.provider.cts.contacts.ContactUtil;
import android.provider.cts.contacts.DatabaseAsserts;
import android.provider.cts.contacts.RawContactUtil;
import android.provider.cts.contacts.account.StaticAccountAuthenticator;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

public class ContactsContract_RawContactsTest extends AndroidTestCase {
    private ContentResolver mResolver;
    private ContactsContract_TestDataBuilder mBuilder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = getContext().getContentResolver();
        ContentProviderClient provider =
                mResolver.acquireContentProviderClient(ContactsContract.AUTHORITY);
        mBuilder = new ContactsContract_TestDataBuilder(provider);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mBuilder.cleanup();
    }

    public void testGetLookupUriBySourceId() throws Exception {
        TestRawContact rawContact = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_type")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .with(RawContacts.SOURCE_ID, "source_id")
                .insert();

        // TODO remove this. The method under test is currently broken: it will not
        // work without at least one data row in the raw contact.
        rawContact.newDataRow(StructuredName.CONTENT_ITEM_TYPE)
                .with(StructuredName.DISPLAY_NAME, "test name")
                .insert();

        Uri lookupUri = RawContacts.getContactLookupUri(mResolver, rawContact.getUri());
        assertNotNull("Could not produce a lookup URI", lookupUri);

        TestContact lookupContact = mBuilder.newContact().setUri(lookupUri).load();
        assertEquals("Lookup URI matched the wrong contact",
                lookupContact.getId(), rawContact.load().getContactId());
    }

    public void testGetLookupUriByDisplayName() throws Exception {
        TestRawContact rawContact = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_type")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .insert();
        rawContact.newDataRow(StructuredName.CONTENT_ITEM_TYPE)
                .with(StructuredName.DISPLAY_NAME, "test name")
                .insert();

        Uri lookupUri = RawContacts.getContactLookupUri(mResolver, rawContact.getUri());
        assertNotNull("Could not produce a lookup URI", lookupUri);

        TestContact lookupContact = mBuilder.newContact().setUri(lookupUri).load();
        assertEquals("Lookup URI matched the wrong contact",
                lookupContact.getId(), rawContact.load().getContactId());
    }

    public void testRawContactDelete_setsDeleteFlag() {
        long rawContactid = RawContactUtil.insertRawContact(mResolver,
                StaticAccountAuthenticator.ACCOUNT_1);

        assertTrue(RawContactUtil.rawContactExistsById(mResolver, rawContactid));

        RawContactUtil.delete(mResolver, rawContactid, false);

        String[] projection = new String[]{
                ContactsContract.RawContacts.CONTACT_ID,
                ContactsContract.RawContacts.DELETED
        };
        String[] result = RawContactUtil.queryByRawContactId(mResolver, rawContactid,
                projection);

        // Contact id should be null
        assertNull(result[0]);
        // Record should be marked deleted.
        assertEquals("1", result[1]);
    }

    public void testRawContactDelete_removesRecord() {
        long rawContactid = RawContactUtil.insertRawContact(mResolver,
                StaticAccountAuthenticator.ACCOUNT_1);
        assertTrue(RawContactUtil.rawContactExistsById(mResolver, rawContactid));

        RawContactUtil.delete(mResolver, rawContactid, true);

        assertFalse(RawContactUtil.rawContactExistsById(mResolver, rawContactid));

        // already clean
    }


    // This implicitly tests the Contact create case.
    public void testRawContactCreate_updatesContactUpdatedTimestamp() {
        long startTime = System.currentTimeMillis();

        DatabaseAsserts.ContactIdPair ids = DatabaseAsserts.assertAndCreateContact(mResolver);
        long lastUpdated = getContactLastUpdatedTimestampByRawContactId(mResolver,
                ids.mRawContactId);

        assertTrue(lastUpdated > startTime);

        // Clean up
        RawContactUtil.delete(mResolver, ids.mRawContactId, true);
    }

    public void testRawContactUpdate_updatesContactUpdatedTimestamp() {
        DatabaseAsserts.ContactIdPair ids = DatabaseAsserts.assertAndCreateContact(mResolver);

        long baseTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, ids.mContactId);

        ContentValues values = new ContentValues();
        values.put(ContactsContract.RawContacts.STARRED, 1);
        RawContactUtil.update(mResolver, ids.mRawContactId, values);

        long newTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, ids.mContactId);
        assertTrue(newTime > baseTime);

        // Clean up
        RawContactUtil.delete(mResolver, ids.mRawContactId, true);
    }

    public void testRawContactPsuedoDelete_hasDeleteLogForContact() {
        DatabaseAsserts.ContactIdPair ids = DatabaseAsserts.assertAndCreateContact(mResolver);

        long baseTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, ids.mContactId);

        RawContactUtil.delete(mResolver, ids.mRawContactId, false);

        DatabaseAsserts.assertHasDeleteLogGreaterThan(mResolver, ids.mContactId, baseTime);

        // clean up
        RawContactUtil.delete(mResolver, ids.mRawContactId, true);
    }

    public void testRawContactDelete_hasDeleteLogForContact() {
        DatabaseAsserts.ContactIdPair ids = DatabaseAsserts.assertAndCreateContact(mResolver);

        long baseTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, ids.mContactId);

        RawContactUtil.delete(mResolver, ids.mRawContactId, true);

        DatabaseAsserts.assertHasDeleteLogGreaterThan(mResolver, ids.mContactId, baseTime);

        // already clean
    }

    private long getContactLastUpdatedTimestampByRawContactId(ContentResolver resolver,
            long rawContactId) {
        long contactId = RawContactUtil.queryContactIdByRawContactId(mResolver, rawContactId);
        MoreAsserts.assertNotEqual(CommonDatabaseUtils.NOT_FOUND, contactId);

        return ContactUtil.queryContactLastUpdatedTimestamp(mResolver, contactId);
    }
}
