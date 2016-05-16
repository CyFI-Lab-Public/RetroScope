/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package android.provider.cts;

import static android.provider.cts.contacts.DatabaseAsserts.ContactIdPair;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.cts.contacts.CommonDatabaseUtils;
import android.provider.cts.contacts.DatabaseAsserts;
import android.provider.cts.contacts.DeletedContactUtil;
import android.provider.cts.contacts.RawContactUtil;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

import java.util.HashSet;
import java.util.List;

public class ContactsContract_DeletedContacts extends AndroidTestCase {

    private static final Uri URI = ContactsContract.DeletedContacts.CONTENT_URI;

    private ContentResolver mResolver;

    @Override
    public void setUp() {
        mResolver = getContext().getContentResolver();
    }

    public void testDelete_isUnsupported() {

        DatabaseAsserts.assertDeleteIsUnsupported(mResolver, URI);

        Uri uri = ContentUris.withAppendedId(URI, 1L);
        DatabaseAsserts.assertDeleteIsUnsupported(mResolver, uri);
    }

    public void testInsert_isUnsupported() {
        DatabaseAsserts.assertInsertIsUnsupported(mResolver, URI);
    }

    public void testQuery_returnsProperColumns() {
        long start = System.currentTimeMillis();
        ContactIdPair ids = createAndDeleteContact();

        String[] projection = new String[] {
                ContactsContract.DeletedContacts.CONTACT_ID,
                ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP
        };
        List<String[]> records = DeletedContactUtil.query(mResolver, projection);
        boolean found = false;
        for (String[] record : records) {
            if (Long.parseLong(record[0]) == ids.mContactId) {
                found = true;
                assertTrue(Long.parseLong(record[1]) > start);
            }
        }
        assertTrue(found);
    }

    public void testQueryByContactId() {
        ContactIdPair ids = createAndDeleteContact();

        MoreAsserts.assertNotEqual(CommonDatabaseUtils.NOT_FOUND,
                DeletedContactUtil.queryDeletedTimestampForContactId(mResolver, ids.mContactId));
    }

    public void testQueryAll() {
        int numDeletes = 10;

        // Since we cannot clean out delete log from previous tests, we need to account for that
        // by querying for the count first.
        long startCount = DeletedContactUtil.getCount(mResolver);

        for (int i = 0; i < numDeletes; i++) {
            createAndDeleteContact();
        }

        long endCount = DeletedContactUtil.getCount(mResolver);

        assertEquals(numDeletes, endCount - startCount);
    }

    public void testQuerySinceTimestamp() {

        // Before
        HashSet<Long> beforeIds = new HashSet<Long>();
        beforeIds.add(createAndDeleteContact().mContactId);
        beforeIds.add(createAndDeleteContact().mContactId);

        long start = System.currentTimeMillis();

        // After
        HashSet<Long> afterIds = new HashSet<Long>();
        afterIds.add(createAndDeleteContact().mContactId);
        afterIds.add(createAndDeleteContact().mContactId);
        afterIds.add(createAndDeleteContact().mContactId);

        String[] projection = new String[]{
                ContactsContract.DeletedContacts.CONTACT_ID,
                ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP
        };
        List<String[]> records = DeletedContactUtil.querySinceTimestamp(mResolver, projection,
                start);
        for (String[] record : records) {
            // Check ids to make sure we only have the ones that came after the time.
            long contactId = Long.parseLong(record[0]);
            assertFalse(beforeIds.contains(contactId));
            assertTrue(afterIds.contains(contactId));

            // Check times to make sure they came after
            assertTrue(Long.parseLong(record[1]) > start);
        }
    }

    private ContactIdPair createAndDeleteContact() {
        ContactIdPair ids = DatabaseAsserts.assertAndCreateContact(mResolver);
        assertEquals(CommonDatabaseUtils.NOT_FOUND,
                DeletedContactUtil.queryDeletedTimestampForContactId(mResolver, ids.mContactId));
        SystemClock.sleep(1);
        RawContactUtil.delete(mResolver, ids.mRawContactId, true);
        return ids;
    }
}
