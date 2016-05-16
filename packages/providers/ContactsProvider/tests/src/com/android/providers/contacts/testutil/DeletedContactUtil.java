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

package com.android.providers.contacts.testutil;

import static android.provider.ContactsContract.DeletedContacts;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;

import java.util.List;

/**
 * Convenience methods for operating on the DeletedContacts table.
 */
public class DeletedContactUtil {

    private static final Uri URI = DeletedContacts.CONTENT_URI;

    public static long queryDeletedTimestampForContactId(ContentResolver resolver, long contactId) {
        String[] projection = new String[]{
                DeletedContacts.CONTACT_DELETED_TIMESTAMP
        };
        Uri uri = ContentUris.withAppendedId(URI, contactId);
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        if (cursor.moveToNext()) {
            return cursor.getLong(0);
        }
        return CommonDatabaseUtils.NOT_FOUND;
    }

    public static long getCount(ContentResolver resolver) {
        String[] projection = new String[] {
                DeletedContacts.CONTACT_ID
        };
        Cursor cursor = resolver.query(URI, projection, null, null, null);
        try {
            return cursor.getCount();
        } finally {
            CommonDatabaseUtils.closeQuietly(cursor);
        }
    }

    /**
     * Queries all records.
     *
     * @return A list of records.  Where each record is represented as an array of strings.
     */
    public static List<String[]> query(ContentResolver resolver, String[] projection) {
        Cursor cursor = resolver.query(URI, projection, null, null, null);
        return CommonDatabaseUtils.multiRecordToArray(cursor);
    }

    /**
     * Queries all records after a given timestamp.
     *
     * @return A list of records.  Where each record is represented as an array of strings.
     */
    public static List<String[]> querySinceTimestamp(ContentResolver resolver, String[] projection,
            long timestamp) {
        String selection = DeletedContacts.CONTACT_DELETED_TIMESTAMP + ">?";
        String[] args = new String[] {timestamp + ""};
        Cursor cursor = resolver.query(URI, projection, selection, args, null);
        return CommonDatabaseUtils.multiRecordToArray(cursor);
    }
}
