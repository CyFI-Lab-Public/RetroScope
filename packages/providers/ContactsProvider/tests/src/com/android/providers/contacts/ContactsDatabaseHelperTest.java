/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.test.suitebuilder.annotation.SmallTest;

import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.google.android.collect.Sets;

import java.util.Set;

@SmallTest
public class ContactsDatabaseHelperTest extends BaseContactsProvider2Test {
    private ContactsDatabaseHelper mDbHelper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDbHelper = getContactsProvider().getDatabaseHelper(getContext());
    }

    public void testGetOrCreateAccountId() {
        final AccountWithDataSet a1 = null;
        final AccountWithDataSet a2 = new AccountWithDataSet("a", null, null);
        final AccountWithDataSet a3 = new AccountWithDataSet(null, "b", null);
        final AccountWithDataSet a4 = new AccountWithDataSet(null, null, "c");
        final AccountWithDataSet a5 = new AccountWithDataSet("a", "b", "c");

        // First, there's no accounts.  getAccountIdOrNull() always returns null.
        assertNull(mDbHelper.getAccountIdOrNull(a1));
        assertNull(mDbHelper.getAccountIdOrNull(a2));
        assertNull(mDbHelper.getAccountIdOrNull(a3));
        assertNull(mDbHelper.getAccountIdOrNull(a4));
        assertNull(mDbHelper.getAccountIdOrNull(a5));

        // getOrCreateAccountId should create accounts.
        final long a1id = mDbHelper.getOrCreateAccountIdInTransaction(a1);
        final long a2id = mDbHelper.getOrCreateAccountIdInTransaction(a2);
        final long a3id = mDbHelper.getOrCreateAccountIdInTransaction(a3);
        final long a4id = mDbHelper.getOrCreateAccountIdInTransaction(a4);
        final long a5id = mDbHelper.getOrCreateAccountIdInTransaction(a5);

        // The IDs should be all positive and unique.
        assertTrue(a1id > 0);
        assertTrue(a2id > 0);
        assertTrue(a3id > 0);
        assertTrue(a4id > 0);
        assertTrue(a5id > 0);

        final Set<Long> ids = Sets.newHashSet();
        ids.add(a1id);
        ids.add(a2id);
        ids.add(a3id);
        ids.add(a4id);
        ids.add(a5id);
        assertEquals(5, ids.size());

        // Second call: This time getOrCreateAccountId will return the existing IDs.
        assertEquals(a1id, mDbHelper.getOrCreateAccountIdInTransaction(a1));
        assertEquals(a2id, mDbHelper.getOrCreateAccountIdInTransaction(a2));
        assertEquals(a3id, mDbHelper.getOrCreateAccountIdInTransaction(a3));
        assertEquals(a4id, mDbHelper.getOrCreateAccountIdInTransaction(a4));
        assertEquals(a5id, mDbHelper.getOrCreateAccountIdInTransaction(a5));

        // Now getAccountIdOrNull() returns IDs too.
        assertEquals((Long) a1id, mDbHelper.getAccountIdOrNull(a1));
        assertEquals((Long) a2id, mDbHelper.getAccountIdOrNull(a2));
        assertEquals((Long) a3id, mDbHelper.getAccountIdOrNull(a3));
        assertEquals((Long) a4id, mDbHelper.getAccountIdOrNull(a4));
        assertEquals((Long) a5id, mDbHelper.getAccountIdOrNull(a5));

        // null and AccountWithDataSet.NULL should be treated as the same thing.
        assertEquals(a1id, mDbHelper.getOrCreateAccountIdInTransaction(AccountWithDataSet.LOCAL));
        assertEquals((Long) a1id, mDbHelper.getAccountIdOrNull(AccountWithDataSet.LOCAL));

        // Remove all accounts.
        mDbHelper.getWritableDatabase().execSQL("delete from " + Tables.ACCOUNTS);

        assertNull(mDbHelper.getAccountIdOrNull(AccountWithDataSet.LOCAL));
        assertNull(mDbHelper.getAccountIdOrNull(a1));
        assertNull(mDbHelper.getAccountIdOrNull(a2));
        assertNull(mDbHelper.getAccountIdOrNull(a3));
        assertNull(mDbHelper.getAccountIdOrNull(a4));
        assertNull(mDbHelper.getAccountIdOrNull(a5));

        // Logically same as a5, but physically different object.
        final AccountWithDataSet a5b = new AccountWithDataSet("a", "b", "c");
        // a5 and a5b should have the same ID.
        assertEquals(
                mDbHelper.getOrCreateAccountIdInTransaction(a5),
                mDbHelper.getOrCreateAccountIdInTransaction(a5b));
    }
}
