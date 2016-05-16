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
 * limitations under the License
 */
package com.android.providers.contacts.aggregation;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.provider.ContactsContract.Contacts;

import com.android.providers.contacts.ContactLookupKey;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.PhotoPriorityResolver;
import com.android.providers.contacts.TransactionContext;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;

/**
 * A version of the ContactAggregator for use against the profile database.
 */
public class ProfileAggregator extends ContactAggregator {

    private long mContactId;

    public ProfileAggregator(ContactsProvider2 contactsProvider,
            ContactsDatabaseHelper contactsDatabaseHelper,
            PhotoPriorityResolver photoPriorityResolver, NameSplitter nameSplitter,
            CommonNicknameCache commonNicknameCache) {
        super(contactsProvider, contactsDatabaseHelper, photoPriorityResolver, nameSplitter,
                commonNicknameCache);
    }

    @Override
    protected String computeLookupKeyForContact(SQLiteDatabase db, long contactId) {
        return ContactLookupKey.PROFILE_LOOKUP_KEY;
    }

    @Override
    protected void appendLookupKey(StringBuilder sb, String accountTypeWithDataSet,
            String accountName, long rawContactId, String sourceId, String displayName) {

        // The profile's lookup key should always be "profile".
        sb.setLength(0);
        sb.append(ContactLookupKey.PROFILE_LOOKUP_KEY);
    }

    @Override
    public long onRawContactInsert(TransactionContext txContext, SQLiteDatabase db,
            long rawContactId) {
        aggregateContact(txContext, db, rawContactId);
        return mContactId;
    }

    @Override
    public void aggregateInTransaction(TransactionContext txContext, SQLiteDatabase db) {
        // Do nothing.  The contact should already be aggregated.
    }

    @Override
    public void aggregateContact(TransactionContext txContext, SQLiteDatabase db,
            long rawContactId) {
        // Profile aggregation is simple - find the single contact in the database and attach to
        // that.  We look it up each time in case the profile was deleted by a previous operation
        // and needs re-creation.
        SQLiteStatement profileContactIdLookup = db.compileStatement(
                "SELECT " + Contacts._ID +
                        " FROM " + Tables.CONTACTS +
                        " ORDER BY " + Contacts._ID +
                        " LIMIT 1");
        try {
            mContactId = profileContactIdLookup.simpleQueryForLong();
            updateAggregateData(txContext, mContactId);
        } catch (SQLiteDoneException e) {
            // No valid contact ID found, so create one.
            mContactId = insertContact(db, rawContactId);
        } finally {
            profileContactIdLookup.close();
        }
        setContactId(rawContactId, mContactId);
    }
}
