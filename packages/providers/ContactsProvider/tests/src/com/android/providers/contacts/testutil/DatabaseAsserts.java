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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.test.MoreAsserts;

import junit.framework.Assert;

/**
 * Common methods for asserting database related operations.
 */
public class DatabaseAsserts {

    public static void assertDeleteIsUnsupported(ContentResolver resolver, Uri uri) {
        try {
            resolver.delete(uri, null, null);
            Assert.fail("delete operation should have failed with UnsupportedOperationException on"
                    + uri);
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    public static void assertInsertIsUnsupported(ContentResolver resolver, Uri  uri) {
        try {
            ContentValues values = new ContentValues();
            resolver.insert(uri, values);
            Assert.fail("insert operation should have failed with UnsupportedOperationException on"
                    + uri);
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    /**
     * Create a contact and assert that the record exists.
     *
     * @return The created contact id pair.
     */
    public static ContactIdPair assertAndCreateContact(ContentResolver resolver) {
        long rawContactId = RawContactUtil.createRawContactWithName(resolver);

        long contactId = RawContactUtil.queryContactIdByRawContactId(resolver, rawContactId);
        MoreAsserts.assertNotEqual(CommonDatabaseUtils.NOT_FOUND, contactId);

        return new ContactIdPair(contactId, rawContactId);
    }

    /**
     * Asserts that a contact id was deleted, has a delete log, and that log has a timestamp greater
     * than the given timestamp.
     *
     * @param contactId The contact id to check.
     * @param start The timestamp that the delete log should be greater than.
     */
    public static void assertHasDeleteLogGreaterThan(ContentResolver resolver, long contactId,
            long start) {
        Assert.assertFalse(ContactUtil.recordExistsForContactId(resolver, contactId));

        long deletedTimestamp = DeletedContactUtil.queryDeletedTimestampForContactId(resolver,
                contactId);
        MoreAsserts.assertNotEqual(CommonDatabaseUtils.NOT_FOUND, deletedTimestamp);
        Assert.assertTrue(deletedTimestamp > start);
    }

    /**
     * Holds a single contact id and raw contact id relationship.
     */
    public static class ContactIdPair {
        public long mContactId;
        public long mRawContactId;

        public ContactIdPair(long contactId, long rawContactId) {
            this.mContactId = contactId;
            this.mRawContactId = rawContactId;
        }
    }
}
