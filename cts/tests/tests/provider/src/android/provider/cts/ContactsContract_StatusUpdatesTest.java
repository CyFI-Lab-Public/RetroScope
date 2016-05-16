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

package android.provider.cts;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;
import android.test.AndroidTestCase;

import java.util.ArrayList;

public class ContactsContract_StatusUpdatesTest extends AndroidTestCase {

    private static final String ACCOUNT_TYPE = "com.android.cts.provider";
    private static final String ACCOUNT_NAME = "ContactsContract_StatusUpdatesTest";

    private ContentResolver mResolver;
    private long dataId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = mContext.getContentResolver();

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
                .withValue(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
                .build());

        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE)
                .withValue(Im.TYPE, Im.TYPE_HOME)
                .withValue(Im.PROTOCOL, Im.PROTOCOL_GOOGLE_TALK)
                .build());

        ContentProviderResult[] results = mResolver.applyBatch(ContactsContract.AUTHORITY, ops);
        assertNotNull(results[0].uri);
        assertNotNull(results[1].uri);

        dataId = ContentUris.parseId(results[1].uri);
    }

    public void testInsertStatus() throws Exception {
        Uri uri = insertStatusUpdate(dataId, StatusUpdates.DO_NOT_DISTURB, null, null);
        assertPresence(uri, StatusUpdates.DO_NOT_DISTURB);
        assertStatus(uri, null);
        assertHasTimestamp(uri, false);
        assertRowCount(uri, 1);

        Uri uri2 = insertStatusUpdate(dataId, StatusUpdates.AVAILABLE, null, null);
        assertEquals(uri, uri2);

        assertPresence(uri, StatusUpdates.AVAILABLE);
        assertStatus(uri, null);
        assertHasTimestamp(uri, false);
        assertRowCount(uri, 1);

        Uri uri3 = insertStatusUpdate(dataId, StatusUpdates.AWAY, "Grabbing a byte", null);
        assertEquals(uri2, uri3);

        assertPresence(uri, StatusUpdates.AWAY);
        assertStatus(uri, "Grabbing a byte");
        assertHasTimestamp(uri, false);
        assertRowCount(uri, 1);

        // Inserting a new status message causes a timestamp to be inserted
        Uri uri4 = insertStatusUpdate(dataId, StatusUpdates.IDLE, "Taking a bit of a break", null);
        assertEquals(uri3, uri4);

        assertPresence(uri, StatusUpdates.IDLE);
        assertStatus(uri, "Taking a bit of a break");
        assertHasTimestamp(uri, true);
        assertRowCount(uri, 1);
    }

    private Uri insertStatusUpdate(long dataId, int presence, String status, Long timestamp)
            throws Exception {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(ContentProviderOperation.newInsert(StatusUpdates.CONTENT_URI)
                .withValue(StatusUpdates.DATA_ID, dataId)
                .withValue(StatusUpdates.PRESENCE, presence)
                .withValue(StatusUpdates.STATUS, status)
                .withValue(StatusUpdates.STATUS_TIMESTAMP, timestamp)
                .withValue(StatusUpdates.PROTOCOL, Im.PROTOCOL_GOOGLE_TALK)
                .withValue(StatusUpdates.IM_HANDLE, "mrBugDroid1337")
                .build());

        ContentProviderResult[] results = mResolver.applyBatch(ContactsContract.AUTHORITY, ops);
        assertNotNull(results[0].uri);
        return results[0].uri;
    }

    private void assertRowCount(Uri uri, int count) throws Exception {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(ContentProviderOperation.newAssertQuery(uri)
                .withExpectedCount(count)
                .build());

        mResolver.applyBatch(ContactsContract.AUTHORITY, ops);
    }

    private void assertPresence(Uri uri, int status) throws Exception {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(ContentProviderOperation.newAssertQuery(uri)
                .withValue(StatusUpdates.PRESENCE, status)
                .withExpectedCount(1)
                .build());

        mResolver.applyBatch(ContactsContract.AUTHORITY, ops);
    }

    private void assertStatus(Uri uri, String status) throws Exception {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(ContentProviderOperation.newAssertQuery(uri)
                .withValue(StatusUpdates.STATUS, status)
                .withExpectedCount(1)
                .build());

        mResolver.applyBatch(ContactsContract.AUTHORITY, ops);
    }

    private void assertHasTimestamp(Uri uri, boolean hasTimestmap) throws Exception {
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        try {
            assertTrue(cursor.moveToNext());

            if (hasTimestmap) {
                assertNotNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        StatusUpdates.STATUS_TIMESTAMP)));
            } else {
                assertNull(cursor.getString(cursor.getColumnIndexOrThrow(
                        StatusUpdates.STATUS_TIMESTAMP)));
            }
        } finally {
            cursor.close();
        }
    }

    public void testGetPresencePrecedence() {
        assertPrecedence(StatusUpdates.AVAILABLE);
        assertPrecedence(StatusUpdates.AWAY);
        assertPrecedence(StatusUpdates.DO_NOT_DISTURB);
        assertPrecedence(StatusUpdates.IDLE);
        assertPrecedence(StatusUpdates.INVISIBLE);
        assertPrecedence(StatusUpdates.OFFLINE);
    }

    private void assertPrecedence(int status) {
        assertTrue(StatusUpdates.getPresencePrecedence(status) >= 0);
    }

    public void testGetPresenceIconresourceId() {
        assertResource(StatusUpdates.AVAILABLE);
        assertResource(StatusUpdates.AWAY);
        assertResource(StatusUpdates.DO_NOT_DISTURB);
        assertResource(StatusUpdates.IDLE);
        assertResource(StatusUpdates.INVISIBLE);
        assertResource(StatusUpdates.OFFLINE);
    }

    private void assertResource(int status) {
        assertTrue(0 != StatusUpdates.getPresenceIconResourceId(status));
    }
}
