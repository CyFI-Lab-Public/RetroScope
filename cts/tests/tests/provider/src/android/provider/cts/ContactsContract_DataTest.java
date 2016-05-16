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


import static android.provider.ContactsContract.CommonDataKinds;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Contactables;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.cts.ContactsContract_TestDataBuilder.TestContact;
import android.provider.cts.ContactsContract_TestDataBuilder.TestData;
import android.provider.cts.ContactsContract_TestDataBuilder.TestRawContact;
import android.provider.cts.contacts.ContactUtil;
import android.provider.cts.contacts.DataUtil;
import android.provider.cts.contacts.DatabaseAsserts;
import android.provider.cts.contacts.RawContactUtil;
import android.test.InstrumentationTestCase;

import java.util.ArrayList;

public class ContactsContract_DataTest extends InstrumentationTestCase {
    private ContentResolver mResolver;
    private ContactsContract_TestDataBuilder mBuilder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = getInstrumentation().getTargetContext().getContentResolver();
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
        TestData data = rawContact.newDataRow(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .with(CommonDataKinds.StructuredName.DISPLAY_NAME, "test name")
                .insert();

        Uri lookupUri = Data.getContactLookupUri(mResolver, data.getUri());
        assertNotNull("Could not produce a lookup URI", lookupUri);

        TestContact lookupContact = mBuilder.newContact().setUri(lookupUri).load();
        assertEquals("Lookup URI matched the wrong contact",
                lookupContact.getId(), data.load().getRawContact().load().getContactId());
    }

    public void testGetLookupUriByDisplayName() throws Exception {
        TestRawContact rawContact = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_type")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .insert();
        TestData data = rawContact.newDataRow(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .with(CommonDataKinds.StructuredName.DISPLAY_NAME, "test name")
                .insert();

        Uri lookupUri = Data.getContactLookupUri(mResolver, data.getUri());
        assertNotNull("Could not produce a lookup URI", lookupUri);

        TestContact lookupContact = mBuilder.newContact().setUri(lookupUri).load();
        assertEquals("Lookup URI matched the wrong contact",
                lookupContact.getId(), data.load().getRawContact().load().getContactId());
    }

    public void testContactablesUri() throws Exception {
        TestRawContact rawContact = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_account")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .insert();
        rawContact.newDataRow(CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .with(Email.DATA, "test@test.com")
                .with(Email.TYPE, Email.TYPE_WORK)
                .insert();
        ContentValues cv = new ContentValues();
        cv.put(Email.DATA, "test@test.com");
        cv.put(Email.TYPE, Email.TYPE_WORK);

        Uri contentUri = ContactsContract.CommonDataKinds.Contactables.CONTENT_URI;
        try {
            assertCursorStoredValuesWithRawContactsFilter(contentUri,
                    new long[] {rawContact.getId()}, cv);
            rawContact.newDataRow(CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                    .with(CommonDataKinds.StructuredPostal.DATA1, "100 Sesame Street")
                    .insert();

            rawContact.newDataRow(Phone.CONTENT_ITEM_TYPE)
                    .with(Phone.DATA, "123456789")
                    .with(Phone.TYPE, Phone.TYPE_MOBILE)
                    .insert();

            ContentValues cv2 = new ContentValues();
            cv.put(Phone.DATA, "123456789");
            cv.put(Phone.TYPE, Phone.TYPE_MOBILE);

            // Contactables Uri should return only email and phone data items.
            DatabaseAsserts.assertStoredValuesInUriMatchExactly(mResolver, contentUri, null,
                    Data.RAW_CONTACT_ID + "=?", new String[] {String.valueOf(rawContact.getId())},
                    null, cv, cv2);
        } finally {
            // Clean up
            rawContact.delete();
        }
    }

    public void testContactablesFilterByLastName_returnsCorrectDataRows() throws Exception {
        long[] ids = setupContactablesTestData();
        Uri filterUri = Uri.withAppendedPath(Contactables.CONTENT_FILTER_URI, "tamale");
        assertCursorStoredValuesWithRawContactsFilter(filterUri, ids,
                ContactablesTestHelper.getContentValues(0));
    }

    public void testContactablesFilterByFirstName_returnsCorrectDataRows() throws Exception {
        long[] ids = setupContactablesTestData();
        Uri filterUri = Uri.withAppendedPath(Contactables.CONTENT_FILTER_URI, "hot");
        assertCursorStoredValuesWithRawContactsFilter(filterUri, ids,
                ContactablesTestHelper.getContentValues(0));
        Uri filterUri2 = Uri.withAppendedPath(Contactables.CONTENT_FILTER_URI, "tam");
        assertCursorStoredValuesWithRawContactsFilter(filterUri2, ids,
                ContactablesTestHelper.getContentValues(0, 1));
    }

    public void testContactablesFilterByPhonePrefix_returnsCorrectDataRows() throws Exception {
        long[] ids = setupContactablesTestData();
        Uri filterUri = Uri.withAppendedPath(Contactables.CONTENT_FILTER_URI, "518");
        assertCursorStoredValuesWithRawContactsFilter(filterUri, ids,
                ContactablesTestHelper.getContentValues(2));
        Uri filterUri2 = Uri.withAppendedPath(Contactables.CONTENT_FILTER_URI, "51");
        assertCursorStoredValuesWithRawContactsFilter(filterUri2, ids,
                ContactablesTestHelper.getContentValues(0, 2));
    }

    public void testContactablesFilterByEmailPrefix_returnsCorrectDataRows() throws Exception {
        long[] ids = setupContactablesTestData();
        Uri filterUri = Uri.withAppendedPath(Contactables.CONTENT_FILTER_URI, "doeassoc");
        assertCursorStoredValuesWithRawContactsFilter(filterUri, ids,
                ContactablesTestHelper.getContentValues(2));
    }

    public void testContactablesFilter_doesNotExist_returnsCorrectDataRows() throws Exception {
        long[] ids = setupContactablesTestData();
        Uri filterUri = Uri.withAppendedPath(Contactables.CONTENT_FILTER_URI, "doesnotexist");
        assertCursorStoredValuesWithRawContactsFilter(filterUri, ids, new ContentValues[0]);
    }

    public void testDataInsert_updatesContactLastUpdatedTimestamp() {
        DatabaseAsserts.ContactIdPair ids = DatabaseAsserts.assertAndCreateContact(mResolver);
        long baseTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, ids.mContactId);

        SystemClock.sleep(1);
        createData(ids.mRawContactId);

        long newTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, ids.mContactId);
        assertTrue(newTime > baseTime);

        // Clean up
        RawContactUtil.delete(mResolver, ids.mRawContactId, true);
    }

    public void testDataDelete_updatesContactLastUpdatedTimestamp() {
        DatabaseAsserts.ContactIdPair ids = DatabaseAsserts.assertAndCreateContact(mResolver);

        long dataId = createData(ids.mRawContactId);

        long baseTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, ids.mContactId);

        SystemClock.sleep(1);
        DataUtil.delete(mResolver, dataId);

        long newTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, ids.mContactId);
        assertTrue(newTime > baseTime);

        // Clean up
        RawContactUtil.delete(mResolver, ids.mRawContactId, true);
    }

    public void testDataUpdate_updatesContactLastUpdatedTimestamp() {
        DatabaseAsserts.ContactIdPair ids = DatabaseAsserts.assertAndCreateContact(mResolver);
        long dataId = createData(ids.mRawContactId);
        long baseTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, ids.mContactId);

        SystemClock.sleep(1);
        ContentValues values = new ContentValues();
        values.put(CommonDataKinds.Phone.NUMBER, "555-5555");
        DataUtil.update(mResolver, dataId, values);

        long newTime = ContactUtil.queryContactLastUpdatedTimestamp(mResolver, ids.mContactId);
        assertTrue("Expected contact " + ids.mContactId + " last updated to be greater than " +
                baseTime + ". But was " + newTime, newTime > baseTime);

        // Clean up
        RawContactUtil.delete(mResolver, ids.mRawContactId, true);
    }

    private long createData(long rawContactId) {
        ContentValues values = new ContentValues();
        values.put(Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        values.put(CommonDataKinds.Phone.NUMBER, "1-800-GOOG-411");
        values.put(CommonDataKinds.Phone.TYPE, CommonDataKinds.Phone.TYPE_CUSTOM);
        values.put(CommonDataKinds.Phone.LABEL, "free directory assistance");
        return DataUtil.insertData(mResolver, rawContactId, values);
    }

    private void assertCursorStoredValuesWithRawContactsFilter(Uri uri, long[] rawContactsId,
            ContentValues... expected) {
        // We need this helper function to add a filter for specific raw contacts because
        // otherwise tests will fail if performed on a device with existing contacts data
        StringBuilder sb = new StringBuilder();
        sb.append(Data.RAW_CONTACT_ID + " in ");
        sb.append("(");
        for (int i = 0; i < rawContactsId.length; i++) {
            if (i != 0) sb.append(",");
            sb.append(rawContactsId[i]);
        }
        sb.append(")");
        DatabaseAsserts.assertStoredValuesInUriMatchExactly(mResolver, uri, null, sb.toString(),
                null, null, expected);
    }


    private long[] setupContactablesTestData() throws Exception {
        TestRawContact rawContact = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_account")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .insert();
        rawContact.newDataRow(StructuredName.CONTENT_ITEM_TYPE)
                .with(StructuredName.DISPLAY_NAME, "Hot Tamale")
                .insert();
        rawContact.newDataRow(Email.CONTENT_ITEM_TYPE)
                .with(Email.DATA, "tamale@acme.com")
                .with(Email.TYPE, Email.TYPE_HOME)
                .insert();
        rawContact.newDataRow(Email.CONTENT_ITEM_TYPE)
                .with(Email.DATA, "hot@google.com")
                .with(Email.TYPE, Email.TYPE_WORK)
                .insert();
        rawContact.newDataRow(Phone.CONTENT_ITEM_TYPE)
                .with(Phone.DATA, "510-123-5769")
                .with(Email.TYPE, Phone.TYPE_HOME)
                .insert();

        TestRawContact rawContact2 = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_account")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .insert();
        rawContact2.newDataRow(StructuredName.CONTENT_ITEM_TYPE)
                .with(StructuredName.DISPLAY_NAME, "Cold Tamago")
                .insert();
        rawContact2.newDataRow(Email.CONTENT_ITEM_TYPE)
                .with(Email.DATA, "eggs@farmers.org")
                .with(Email.TYPE, Email.TYPE_HOME)
                .insert();

        TestRawContact rawContact3 = mBuilder.newRawContact()
                .with(RawContacts.ACCOUNT_TYPE, "test_account")
                .with(RawContacts.ACCOUNT_NAME, "test_name")
                .insert();
        rawContact3.newDataRow(StructuredName.CONTENT_ITEM_TYPE)
                .with(StructuredName.DISPLAY_NAME, "John Doe")
                .insert();
        rawContact3.newDataRow(Email.CONTENT_ITEM_TYPE)
                .with(Email.DATA, "doeassociates@deer.com")
                .with(Email.TYPE, Email.TYPE_WORK)
                .insert();
        rawContact3.newDataRow(Phone.CONTENT_ITEM_TYPE)
                .with(Phone.DATA, "518-354-1111")
                .with(Phone.TYPE, Phone.TYPE_HOME)
                .insert();
        rawContact3.newDataRow(Organization.CONTENT_ITEM_TYPE)
                .with(Organization.DATA, "Doe Industries")
                .insert();
        return new long[] {rawContact.getId(), rawContact2.getId(), rawContact3.getId()};
    }

    // Provides functionality to set up content values for the Contactables tests
    private static class ContactablesTestHelper {
        private static ContentValues[] sContentValues = new ContentValues[6];
        static {
            ContentValues cv1 = new ContentValues();
            cv1.put(Contacts.DISPLAY_NAME, "Hot Tamale");
            cv1.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            cv1.put(Email.DATA, "tamale@acme.com");
            cv1.put(Email.TYPE, Email.TYPE_HOME);
            sContentValues[0] = cv1;

            ContentValues cv2 = new ContentValues();
            cv2.put(Contacts.DISPLAY_NAME, "Hot Tamale");
            cv2.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            cv2.put(Phone.DATA, "510-123-5769");
            cv2.put(Phone.TYPE, Phone.TYPE_HOME);
            sContentValues[1] = cv2;

            ContentValues cv3 = new ContentValues();
            cv3.put(Contacts.DISPLAY_NAME, "Hot Tamale");
            cv3.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            cv3.put(Email.DATA, "hot@google.com");
            cv3.put(Email.TYPE, Email.TYPE_WORK);
            sContentValues[2] = cv3;

            ContentValues cv4 = new ContentValues();
            cv4.put(Contacts.DISPLAY_NAME, "Cold Tamago");
            cv4.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            cv4.put(Email.DATA, "eggs@farmers.org");
            cv4.put(Email.TYPE, Email.TYPE_HOME);
            sContentValues[3] = cv4;

            ContentValues cv5 = new ContentValues();
            cv5.put(Contacts.DISPLAY_NAME, "John Doe");
            cv5.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            cv5.put(Email.DATA, "doeassociates@deer.com");
            cv5.put(Email.TYPE, Email.TYPE_WORK);
            sContentValues[4] = cv5;

            ContentValues cv6 = new ContentValues();
            cv6.put(Contacts.DISPLAY_NAME, "John Doe");
            cv6.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            cv6.put(Phone.DATA, "518-354-1111");
            cv6.put(Phone.TYPE, Phone.TYPE_HOME);
            sContentValues[5] = cv6;
        }

        /**
         * @return An arraylist of contentValues that correspond to the provided raw contacts
         */
        public static ContentValues[] getContentValues(int... rawContacts) {
            ArrayList<ContentValues> cv = new ArrayList<ContentValues>();
            for (int i = 0; i < rawContacts.length; i++) {
                switch (rawContacts[i]) {
                    case 0:
                        // rawContact 0 "Hot Tamale" contains ContentValues 0, 1, and 2
                        cv.add(sContentValues[0]);
                        cv.add(sContentValues[1]);
                        cv.add(sContentValues[2]);
                        break;
                    case 1:
                        // rawContact 1 "Cold Tamago" contains ContentValues 3
                        cv.add(sContentValues[3]);
                        break;
                    case 2:
                        // rawContact 1 "John Doe" contains ContentValues 4, 5
                        cv.add(sContentValues[4]);
                        cv.add(sContentValues[5]);
                        break;
                }
            }
            ContentValues[] toReturn = new ContentValues[cv.size()];
            for (int i = 0; i < cv.size(); i++) {
                toReturn[i] = cv.get(i);
            }
            return toReturn;
        }
    }
}

