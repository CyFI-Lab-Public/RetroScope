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
 * limitations under the License
 */

package com.android.providers.contacts;

import com.google.android.collect.Maps;
import com.google.android.collect.Sets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Accumulates information for an entire transaction. {@link ContactsProvider2} consumes
 * it at commit time.
 */
public class TransactionContext  {

    private final boolean mForProfile;
    /** Map from raw contact id to account Id */
    private HashMap<Long, Long> mInsertedRawContactsAccounts;
    private HashSet<Long> mUpdatedRawContacts;
    private HashSet<Long> mDirtyRawContacts;
    // Set used to track what has been changed and deleted. This is needed so we can update the
    // contact last touch timestamp.  Dirty set above is only set when sync adapter is false.
    // {@see android.provider.ContactsContract#CALLER_IS_SYNCADAPTER}. While the set below will
    // contain all changed contacts.
    private HashSet<Long> mChangedRawContacts;
    private HashSet<Long> mStaleSearchIndexRawContacts;
    private HashSet<Long> mStaleSearchIndexContacts;
    private HashMap<Long, Object> mUpdatedSyncStates;

    public TransactionContext(boolean forProfile) {
        mForProfile = forProfile;
    }

    public boolean isForProfile() {
        return mForProfile;
    }

    public void rawContactInserted(long rawContactId, long accountId) {
        if (mInsertedRawContactsAccounts == null) mInsertedRawContactsAccounts = Maps.newHashMap();
        mInsertedRawContactsAccounts.put(rawContactId, accountId);

        markRawContactChangedOrDeletedOrInserted(rawContactId);
    }

    public void rawContactUpdated(long rawContactId) {
        if (mUpdatedRawContacts == null) mUpdatedRawContacts = Sets.newHashSet();
        mUpdatedRawContacts.add(rawContactId);
    }

    public void markRawContactDirtyAndChanged(long rawContactId, boolean isSyncAdapter) {
        if (!isSyncAdapter) {
            if (mDirtyRawContacts == null) {
                mDirtyRawContacts = Sets.newHashSet();
            }
            mDirtyRawContacts.add(rawContactId);
        }

        markRawContactChangedOrDeletedOrInserted(rawContactId);
    }

    public void markRawContactChangedOrDeletedOrInserted(long rawContactId) {
        if (mChangedRawContacts == null) {
            mChangedRawContacts = Sets.newHashSet();
        }
        mChangedRawContacts.add(rawContactId);
    }

    public void syncStateUpdated(long rowId, Object data) {
        if (mUpdatedSyncStates == null) mUpdatedSyncStates = Maps.newHashMap();
        mUpdatedSyncStates.put(rowId, data);
    }

    public void invalidateSearchIndexForRawContact(long rawContactId) {
        if (mStaleSearchIndexRawContacts == null) mStaleSearchIndexRawContacts = Sets.newHashSet();
        mStaleSearchIndexRawContacts.add(rawContactId);
    }

    public void invalidateSearchIndexForContact(long contactId) {
        if (mStaleSearchIndexContacts == null) mStaleSearchIndexContacts = Sets.newHashSet();
        mStaleSearchIndexContacts.add(contactId);
    }

    public Set<Long> getInsertedRawContactIds() {
        if (mInsertedRawContactsAccounts == null) mInsertedRawContactsAccounts = Maps.newHashMap();
        return mInsertedRawContactsAccounts.keySet();
    }

    public Set<Long> getUpdatedRawContactIds() {
        if (mUpdatedRawContacts == null) mUpdatedRawContacts = Sets.newHashSet();
        return mUpdatedRawContacts;
    }

    public Set<Long> getDirtyRawContactIds() {
        if (mDirtyRawContacts == null) mDirtyRawContacts = Sets.newHashSet();
        return mDirtyRawContacts;
    }

    public Set<Long> getChangedRawContactIds() {
        if (mChangedRawContacts == null) mChangedRawContacts = Sets.newHashSet();
        return mChangedRawContacts;
    }

    public Set<Long> getStaleSearchIndexRawContactIds() {
        if (mStaleSearchIndexRawContacts == null) mStaleSearchIndexRawContacts = Sets.newHashSet();
        return mStaleSearchIndexRawContacts;
    }

    public Set<Long> getStaleSearchIndexContactIds() {
        if (mStaleSearchIndexContacts == null) mStaleSearchIndexContacts = Sets.newHashSet();
        return mStaleSearchIndexContacts;
    }

    public Set<Entry<Long, Object>> getUpdatedSyncStates() {
        if (mUpdatedSyncStates == null) mUpdatedSyncStates = Maps.newHashMap();
        return mUpdatedSyncStates.entrySet();
    }

    public Long getAccountIdOrNullForRawContact(long rawContactId) {
        if (mInsertedRawContactsAccounts == null) mInsertedRawContactsAccounts = Maps.newHashMap();
        return mInsertedRawContactsAccounts.get(rawContactId);
    }

    public boolean isNewRawContact(long rawContactId) {
        if (mInsertedRawContactsAccounts == null) mInsertedRawContactsAccounts = Maps.newHashMap();
        return mInsertedRawContactsAccounts.containsKey(rawContactId);
    }

    public void clearExceptSearchIndexUpdates() {
        mInsertedRawContactsAccounts = null;
        mUpdatedRawContacts = null;
        mUpdatedSyncStates = null;
        mDirtyRawContacts = null;
        mChangedRawContacts = null;
    }

    public void clearSearchIndexUpdates() {
        mStaleSearchIndexRawContacts = null;
        mStaleSearchIndexContacts = null;
    }

    public void clearAll() {
        clearExceptSearchIndexUpdates();
        clearSearchIndexUpdates();
    }
}
