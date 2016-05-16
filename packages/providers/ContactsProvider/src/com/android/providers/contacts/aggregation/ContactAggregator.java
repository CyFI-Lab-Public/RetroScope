/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Identity;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.AggregationSuggestions;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.FullNameStyle;
import android.provider.ContactsContract.PhotoFiles;
import android.provider.ContactsContract.PinnedPositions;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;

import com.android.providers.contacts.ContactLookupKey;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsDatabaseHelper.AccountsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.AggregatedPresenceColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.ContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.DataColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupType;
import com.android.providers.contacts.ContactsDatabaseHelper.PhoneLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.PresenceColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.RawContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.StatusUpdatesColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.NameLookupBuilder;
import com.android.providers.contacts.NameNormalizer;
import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.PhotoPriorityResolver;
import com.android.providers.contacts.ReorderingCursorWrapper;
import com.android.providers.contacts.TransactionContext;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;
import com.android.providers.contacts.aggregation.util.ContactMatcher;
import com.android.providers.contacts.aggregation.util.ContactMatcher.MatchScore;
import com.android.providers.contacts.database.ContactsTableUtil;
import com.android.providers.contacts.util.Clock;

import com.google.android.collect.Maps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * ContactAggregator deals with aggregating contact information coming from different sources.
 * Two John Doe contacts from two disjoint sources are presumed to be the same
 * person unless the user declares otherwise.
 */
public class ContactAggregator {

    private static final String TAG = "ContactAggregator";

    private static final boolean DEBUG_LOGGING = Log.isLoggable(TAG, Log.DEBUG);
    private static final boolean VERBOSE_LOGGING = Log.isLoggable(TAG, Log.VERBOSE);

    private static final String STRUCTURED_NAME_BASED_LOOKUP_SQL =
            NameLookupColumns.NAME_TYPE + " IN ("
                    + NameLookupType.NAME_EXACT + ","
                    + NameLookupType.NAME_VARIANT + ","
                    + NameLookupType.NAME_COLLATION_KEY + ")";


    /**
     * SQL statement that sets the {@link ContactsColumns#LAST_STATUS_UPDATE_ID} column
     * on the contact to point to the latest social status update.
     */
    private static final String UPDATE_LAST_STATUS_UPDATE_ID_SQL =
            "UPDATE " + Tables.CONTACTS +
            " SET " + ContactsColumns.LAST_STATUS_UPDATE_ID + "=" +
                    "(SELECT " + DataColumns.CONCRETE_ID +
                    " FROM " + Tables.STATUS_UPDATES +
                    " JOIN " + Tables.DATA +
                    "   ON (" + StatusUpdatesColumns.DATA_ID + "="
                            + DataColumns.CONCRETE_ID + ")" +
                    " JOIN " + Tables.RAW_CONTACTS +
                    "   ON (" + DataColumns.CONCRETE_RAW_CONTACT_ID + "="
                            + RawContactsColumns.CONCRETE_ID + ")" +
                    " WHERE " + RawContacts.CONTACT_ID + "=?" +
                    " ORDER BY " + StatusUpdates.STATUS_TIMESTAMP + " DESC,"
                            + StatusUpdates.STATUS +
                    " LIMIT 1)" +
            " WHERE " + ContactsColumns.CONCRETE_ID + "=?";

    // From system/core/logcat/event-log-tags
    // aggregator [time, count] will be logged for each aggregator cycle.
    // For the query (as opposed to the merge), count will be negative
    public static final int LOG_SYNC_CONTACTS_AGGREGATION = 2747;

    // If we encounter more than this many contacts with matching names, aggregate only this many
    private static final int PRIMARY_HIT_LIMIT = 15;
    private static final String PRIMARY_HIT_LIMIT_STRING = String.valueOf(PRIMARY_HIT_LIMIT);

    // If we encounter more than this many contacts with matching phone number or email,
    // don't attempt to aggregate - this is likely an error or a shared corporate data element.
    private static final int SECONDARY_HIT_LIMIT = 20;
    private static final String SECONDARY_HIT_LIMIT_STRING = String.valueOf(SECONDARY_HIT_LIMIT);

    // If we encounter more than this many contacts with matching name during aggregation
    // suggestion lookup, ignore the remaining results.
    private static final int FIRST_LETTER_SUGGESTION_HIT_LIMIT = 100;

    private final ContactsProvider2 mContactsProvider;
    private final ContactsDatabaseHelper mDbHelper;
    private PhotoPriorityResolver mPhotoPriorityResolver;
    private final NameSplitter mNameSplitter;
    private final CommonNicknameCache mCommonNicknameCache;

    private boolean mEnabled = true;

    /** Precompiled sql statement for setting an aggregated presence */
    private SQLiteStatement mAggregatedPresenceReplace;
    private SQLiteStatement mPresenceContactIdUpdate;
    private SQLiteStatement mRawContactCountQuery;
    private SQLiteStatement mAggregatedPresenceDelete;
    private SQLiteStatement mMarkForAggregation;
    private SQLiteStatement mPhotoIdUpdate;
    private SQLiteStatement mDisplayNameUpdate;
    private SQLiteStatement mLookupKeyUpdate;
    private SQLiteStatement mStarredUpdate;
    private SQLiteStatement mPinnedUpdate;
    private SQLiteStatement mContactIdAndMarkAggregatedUpdate;
    private SQLiteStatement mContactIdUpdate;
    private SQLiteStatement mMarkAggregatedUpdate;
    private SQLiteStatement mContactUpdate;
    private SQLiteStatement mContactInsert;
    private SQLiteStatement mResetPinnedForRawContact;

    private HashMap<Long, Integer> mRawContactsMarkedForAggregation = Maps.newHashMap();

    private String[] mSelectionArgs1 = new String[1];
    private String[] mSelectionArgs2 = new String[2];
    private String[] mSelectionArgs3 = new String[3];
    private long mMimeTypeIdIdentity;
    private long mMimeTypeIdEmail;
    private long mMimeTypeIdPhoto;
    private long mMimeTypeIdPhone;
    private String mRawContactsQueryByRawContactId;
    private String mRawContactsQueryByContactId;
    private StringBuilder mSb = new StringBuilder();
    private MatchCandidateList mCandidates = new MatchCandidateList();
    private ContactMatcher mMatcher = new ContactMatcher();
    private DisplayNameCandidate mDisplayNameCandidate = new DisplayNameCandidate();

    /**
     * Parameter for the suggestion lookup query.
     */
    public static final class AggregationSuggestionParameter {
        public final String kind;
        public final String value;

        public AggregationSuggestionParameter(String kind, String value) {
            this.kind = kind;
            this.value = value;
        }
    }

    /**
     * Captures a potential match for a given name. The matching algorithm
     * constructs a bunch of NameMatchCandidate objects for various potential matches
     * and then executes the search in bulk.
     */
    private static class NameMatchCandidate {
        String mName;
        int mLookupType;

        public NameMatchCandidate(String name, int nameLookupType) {
            mName = name;
            mLookupType = nameLookupType;
        }
    }

    /**
     * A list of {@link NameMatchCandidate} that keeps its elements even when the list is
     * truncated. This is done for optimization purposes to avoid excessive object allocation.
     */
    private static class MatchCandidateList {
        private final ArrayList<NameMatchCandidate> mList = new ArrayList<NameMatchCandidate>();
        private int mCount;

        /**
         * Adds a {@link NameMatchCandidate} element or updates the next one if it already exists.
         */
        public void add(String name, int nameLookupType) {
            if (mCount >= mList.size()) {
                mList.add(new NameMatchCandidate(name, nameLookupType));
            } else {
                NameMatchCandidate candidate = mList.get(mCount);
                candidate.mName = name;
                candidate.mLookupType = nameLookupType;
            }
            mCount++;
        }

        public void clear() {
            mCount = 0;
        }

        public boolean isEmpty() {
            return mCount == 0;
        }
    }

    /**
     * A convenience class used in the algorithm that figures out which of available
     * display names to use for an aggregate contact.
     */
    private static class DisplayNameCandidate {
        long rawContactId;
        String displayName;
        int displayNameSource;
        boolean verified;
        boolean writableAccount;

        public DisplayNameCandidate() {
            clear();
        }

        public void clear() {
            rawContactId = -1;
            displayName = null;
            displayNameSource = DisplayNameSources.UNDEFINED;
            verified = false;
            writableAccount = false;
        }
    }

    /**
     * Constructor.
     */
    public ContactAggregator(ContactsProvider2 contactsProvider,
            ContactsDatabaseHelper contactsDatabaseHelper,
            PhotoPriorityResolver photoPriorityResolver, NameSplitter nameSplitter,
            CommonNicknameCache commonNicknameCache) {
        mContactsProvider = contactsProvider;
        mDbHelper = contactsDatabaseHelper;
        mPhotoPriorityResolver = photoPriorityResolver;
        mNameSplitter = nameSplitter;
        mCommonNicknameCache = commonNicknameCache;

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Since we have no way of determining which custom status was set last,
        // we'll just pick one randomly.  We are using MAX as an approximation of randomness
        final String replaceAggregatePresenceSql =
                "INSERT OR REPLACE INTO " + Tables.AGGREGATED_PRESENCE + "("
                + AggregatedPresenceColumns.CONTACT_ID + ", "
                + StatusUpdates.PRESENCE + ", "
                + StatusUpdates.CHAT_CAPABILITY + ")"
                + " SELECT " + PresenceColumns.CONTACT_ID + ","
                + StatusUpdates.PRESENCE + ","
                + StatusUpdates.CHAT_CAPABILITY
                + " FROM " + Tables.PRESENCE
                + " WHERE "
                + " (" + StatusUpdates.PRESENCE
                +       " * 10 + " + StatusUpdates.CHAT_CAPABILITY + ")"
                + " = (SELECT "
                + "MAX (" + StatusUpdates.PRESENCE
                +       " * 10 + " + StatusUpdates.CHAT_CAPABILITY + ")"
                + " FROM " + Tables.PRESENCE
                + " WHERE " + PresenceColumns.CONTACT_ID
                + "=?)"
                + " AND " + PresenceColumns.CONTACT_ID
                + "=?;";
        mAggregatedPresenceReplace = db.compileStatement(replaceAggregatePresenceSql);

        mRawContactCountQuery = db.compileStatement(
                "SELECT COUNT(" + RawContacts._ID + ")" +
                " FROM " + Tables.RAW_CONTACTS +
                " WHERE " + RawContacts.CONTACT_ID + "=?"
                        + " AND " + RawContacts._ID + "<>?");

        mAggregatedPresenceDelete = db.compileStatement(
                "DELETE FROM " + Tables.AGGREGATED_PRESENCE +
                " WHERE " + AggregatedPresenceColumns.CONTACT_ID + "=?");

        mMarkForAggregation = db.compileStatement(
                "UPDATE " + Tables.RAW_CONTACTS +
                " SET " + RawContactsColumns.AGGREGATION_NEEDED + "=1" +
                " WHERE " + RawContacts._ID + "=?"
                        + " AND " + RawContactsColumns.AGGREGATION_NEEDED + "=0");

        mPhotoIdUpdate = db.compileStatement(
                "UPDATE " + Tables.CONTACTS +
                " SET " + Contacts.PHOTO_ID + "=?," + Contacts.PHOTO_FILE_ID + "=? " +
                " WHERE " + Contacts._ID + "=?");

        mDisplayNameUpdate = db.compileStatement(
                "UPDATE " + Tables.CONTACTS +
                " SET " + Contacts.NAME_RAW_CONTACT_ID + "=? " +
                " WHERE " + Contacts._ID + "=?");

        mLookupKeyUpdate = db.compileStatement(
                "UPDATE " + Tables.CONTACTS +
                " SET " + Contacts.LOOKUP_KEY + "=? " +
                " WHERE " + Contacts._ID + "=?");

        mStarredUpdate = db.compileStatement("UPDATE " + Tables.CONTACTS + " SET "
                + Contacts.STARRED + "=(SELECT (CASE WHEN COUNT(" + RawContacts.STARRED
                + ")=0 THEN 0 ELSE 1 END) FROM " + Tables.RAW_CONTACTS + " WHERE "
                + RawContacts.CONTACT_ID + "=" + ContactsColumns.CONCRETE_ID + " AND "
                + RawContacts.STARRED + "=1)" + " WHERE " + Contacts._ID + "=?");

        mPinnedUpdate = db.compileStatement("UPDATE " + Tables.CONTACTS + " SET "
                + Contacts.PINNED + "=(SELECT MIN(" + RawContacts.PINNED + ") FROM "
                + Tables.RAW_CONTACTS + " WHERE " + RawContacts.CONTACT_ID + "="
                + ContactsColumns.CONCRETE_ID + " AND " + RawContacts.PINNED + ">"
                + PinnedPositions.DEMOTED + ") WHERE " + Contacts._ID + "=?");

        mContactIdAndMarkAggregatedUpdate = db.compileStatement(
                "UPDATE " + Tables.RAW_CONTACTS +
                " SET " + RawContacts.CONTACT_ID + "=?, "
                        + RawContactsColumns.AGGREGATION_NEEDED + "=0" +
                " WHERE " + RawContacts._ID + "=?");

        mContactIdUpdate = db.compileStatement(
                "UPDATE " + Tables.RAW_CONTACTS +
                " SET " + RawContacts.CONTACT_ID + "=?" +
                " WHERE " + RawContacts._ID + "=?");

        mMarkAggregatedUpdate = db.compileStatement(
                "UPDATE " + Tables.RAW_CONTACTS +
                " SET " + RawContactsColumns.AGGREGATION_NEEDED + "=0" +
                " WHERE " + RawContacts._ID + "=?");

        mPresenceContactIdUpdate = db.compileStatement(
                "UPDATE " + Tables.PRESENCE +
                " SET " + PresenceColumns.CONTACT_ID + "=?" +
                " WHERE " + PresenceColumns.RAW_CONTACT_ID + "=?");

        mContactUpdate = db.compileStatement(ContactReplaceSqlStatement.UPDATE_SQL);
        mContactInsert = db.compileStatement(ContactReplaceSqlStatement.INSERT_SQL);

        mResetPinnedForRawContact = db.compileStatement(
                "UPDATE " + Tables.RAW_CONTACTS +
                " SET " + RawContacts.PINNED + "=" + PinnedPositions.UNPINNED +
                " WHERE " + RawContacts._ID + "=?");

        mMimeTypeIdEmail = mDbHelper.getMimeTypeId(Email.CONTENT_ITEM_TYPE);
        mMimeTypeIdIdentity = mDbHelper.getMimeTypeId(Identity.CONTENT_ITEM_TYPE);
        mMimeTypeIdPhoto = mDbHelper.getMimeTypeId(Photo.CONTENT_ITEM_TYPE);
        mMimeTypeIdPhone = mDbHelper.getMimeTypeId(Phone.CONTENT_ITEM_TYPE);

        // Query used to retrieve data from raw contacts to populate the corresponding aggregate
        mRawContactsQueryByRawContactId = String.format(Locale.US,
                RawContactsQuery.SQL_FORMAT_BY_RAW_CONTACT_ID,
                mMimeTypeIdPhoto, mMimeTypeIdPhone);

        mRawContactsQueryByContactId = String.format(Locale.US,
                RawContactsQuery.SQL_FORMAT_BY_CONTACT_ID,
                mMimeTypeIdPhoto, mMimeTypeIdPhone);
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    private interface AggregationQuery {
        String SQL =
                "SELECT " + RawContacts._ID + "," + RawContacts.CONTACT_ID +
                        ", " + RawContactsColumns.ACCOUNT_ID +
                " FROM " + Tables.RAW_CONTACTS +
                " WHERE " + RawContacts._ID + " IN(";

        int _ID = 0;
        int CONTACT_ID = 1;
        int ACCOUNT_ID = 2;
    }

    /**
     * Aggregate all raw contacts that were marked for aggregation in the current transaction.
     * Call just before committing the transaction.
     */
    public void aggregateInTransaction(TransactionContext txContext, SQLiteDatabase db) {
        final int markedCount = mRawContactsMarkedForAggregation.size();
        if (markedCount == 0) {
            return;
        }

        final long start = System.currentTimeMillis();
        if (DEBUG_LOGGING) {
            Log.d(TAG, "aggregateInTransaction for " + markedCount + " contacts");
        }

        EventLog.writeEvent(LOG_SYNC_CONTACTS_AGGREGATION, start, -markedCount);

        int index = 0;

        // We don't use the cached string builder (namely mSb)  here, as this string can be very
        // long when upgrading (where we re-aggregate all visible contacts) and StringBuilder won't
        // shrink the internal storage.
        // Note: don't use selection args here.  We just include all IDs directly in the selection,
        // because there's a limit for the number of parameters in a query.
        final StringBuilder sbQuery = new StringBuilder();
        sbQuery.append(AggregationQuery.SQL);
        for (long rawContactId : mRawContactsMarkedForAggregation.keySet()) {
            if (index > 0) {
                sbQuery.append(',');
            }
            sbQuery.append(rawContactId);
            index++;
        }

        sbQuery.append(')');

        final long[] rawContactIds;
        final long[] contactIds;
        final long[] accountIds;
        final int actualCount;
        final Cursor c = db.rawQuery(sbQuery.toString(), null);
        try {
            actualCount = c.getCount();
            rawContactIds = new long[actualCount];
            contactIds = new long[actualCount];
            accountIds = new long[actualCount];

            index = 0;
            while (c.moveToNext()) {
                rawContactIds[index] = c.getLong(AggregationQuery._ID);
                contactIds[index] = c.getLong(AggregationQuery.CONTACT_ID);
                accountIds[index] = c.getLong(AggregationQuery.ACCOUNT_ID);
                index++;
            }
        } finally {
            c.close();
        }

        if (DEBUG_LOGGING) {
            Log.d(TAG, "aggregateInTransaction: initial query done.");
        }

        for (int i = 0; i < actualCount; i++) {
            aggregateContact(txContext, db, rawContactIds[i], accountIds[i], contactIds[i],
                    mCandidates, mMatcher);
        }

        long elapsedTime = System.currentTimeMillis() - start;
        EventLog.writeEvent(LOG_SYNC_CONTACTS_AGGREGATION, elapsedTime, actualCount);

        if (DEBUG_LOGGING) {
            Log.d(TAG, "Contact aggregation complete: " + actualCount +
                    (actualCount == 0 ? "" : ", " + (elapsedTime / actualCount)
                            + " ms per raw contact"));
        }
    }

    @SuppressWarnings("deprecation")
    public void triggerAggregation(TransactionContext txContext, long rawContactId) {
        if (!mEnabled) {
            return;
        }

        int aggregationMode = mDbHelper.getAggregationMode(rawContactId);
        switch (aggregationMode) {
            case RawContacts.AGGREGATION_MODE_DISABLED:
                break;

            case RawContacts.AGGREGATION_MODE_DEFAULT: {
                markForAggregation(rawContactId, aggregationMode, false);
                break;
            }

            case RawContacts.AGGREGATION_MODE_SUSPENDED: {
                long contactId = mDbHelper.getContactId(rawContactId);

                if (contactId != 0) {
                    updateAggregateData(txContext, contactId);
                }
                break;
            }

            case RawContacts.AGGREGATION_MODE_IMMEDIATE: {
                aggregateContact(txContext, mDbHelper.getWritableDatabase(), rawContactId);
                break;
            }
        }
    }

    public void clearPendingAggregations() {
        // HashMap woulnd't shrink the internal table once expands it, so let's just re-create
        // a new one instead of clear()ing it.
        mRawContactsMarkedForAggregation = Maps.newHashMap();
    }

    public void markNewForAggregation(long rawContactId, int aggregationMode) {
        mRawContactsMarkedForAggregation.put(rawContactId, aggregationMode);
    }

    public void markForAggregation(long rawContactId, int aggregationMode, boolean force) {
        final int effectiveAggregationMode;
        if (!force && mRawContactsMarkedForAggregation.containsKey(rawContactId)) {
            // As per ContactsContract documentation, default aggregation mode
            // does not override a previously set mode
            if (aggregationMode == RawContacts.AGGREGATION_MODE_DEFAULT) {
                effectiveAggregationMode = mRawContactsMarkedForAggregation.get(rawContactId);
            } else {
                effectiveAggregationMode = aggregationMode;
            }
        } else {
            mMarkForAggregation.bindLong(1, rawContactId);
            mMarkForAggregation.execute();
            effectiveAggregationMode = aggregationMode;
        }

        mRawContactsMarkedForAggregation.put(rawContactId, effectiveAggregationMode);
    }

    private static class RawContactIdAndAggregationModeQuery {
        public static final String TABLE = Tables.RAW_CONTACTS;

        public static final String[] COLUMNS = { RawContacts._ID, RawContacts.AGGREGATION_MODE };

        public static final String SELECTION = RawContacts.CONTACT_ID + "=?";

        public static final int _ID = 0;
        public static final int AGGREGATION_MODE = 1;
    }

    /**
     * Marks all constituent raw contacts of an aggregated contact for re-aggregation.
     */
    private void markContactForAggregation(SQLiteDatabase db, long contactId) {
        mSelectionArgs1[0] = String.valueOf(contactId);
        Cursor cursor = db.query(RawContactIdAndAggregationModeQuery.TABLE,
                RawContactIdAndAggregationModeQuery.COLUMNS,
                RawContactIdAndAggregationModeQuery.SELECTION, mSelectionArgs1, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                long rawContactId = cursor.getLong(RawContactIdAndAggregationModeQuery._ID);
                int aggregationMode = cursor.getInt(
                        RawContactIdAndAggregationModeQuery.AGGREGATION_MODE);
                // Don't re-aggregate AGGREGATION_MODE_SUSPENDED / AGGREGATION_MODE_DISABLED.
                // (Also just ignore deprecated AGGREGATION_MODE_IMMEDIATE)
                if (aggregationMode == RawContacts.AGGREGATION_MODE_DEFAULT) {
                    markForAggregation(rawContactId, aggregationMode, true);
                }
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Mark all visible contacts for re-aggregation.
     *
     * - Set {@link RawContactsColumns#AGGREGATION_NEEDED} For all visible raw_contacts with
     *   {@link RawContacts#AGGREGATION_MODE_DEFAULT}.
     * - Also put them into {@link #mRawContactsMarkedForAggregation}.
     */
    public int markAllVisibleForAggregation(SQLiteDatabase db) {
        final long start = System.currentTimeMillis();

        // Set AGGREGATION_NEEDED for all visible raw_cotnacts with AGGREGATION_MODE_DEFAULT.
        // (Don't re-aggregate AGGREGATION_MODE_SUSPENDED / AGGREGATION_MODE_DISABLED)
        db.execSQL("UPDATE " + Tables.RAW_CONTACTS + " SET " +
                RawContactsColumns.AGGREGATION_NEEDED + "=1" +
                " WHERE " + RawContacts.CONTACT_ID + " IN " + Tables.DEFAULT_DIRECTORY +
                " AND " + RawContacts.AGGREGATION_MODE + "=" + RawContacts.AGGREGATION_MODE_DEFAULT
                );

        final int count;
        final Cursor cursor = db.rawQuery("SELECT " + RawContacts._ID +
                " FROM " + Tables.RAW_CONTACTS +
                " WHERE " + RawContactsColumns.AGGREGATION_NEEDED + "=1", null);
        try {
            count = cursor.getCount();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                final long rawContactId = cursor.getLong(0);
                mRawContactsMarkedForAggregation.put(rawContactId,
                        RawContacts.AGGREGATION_MODE_DEFAULT);
            }
        } finally {
            cursor.close();
        }

        final long end = System.currentTimeMillis();
        Log.i(TAG, "Marked all visible contacts for aggregation: " + count + " raw contacts, " +
                (end - start) + " ms");
        return count;
    }

    /**
     * Creates a new contact based on the given raw contact.  Does not perform aggregation.  Returns
     * the ID of the contact that was created.
     */
    public long onRawContactInsert(
            TransactionContext txContext, SQLiteDatabase db, long rawContactId) {
        long contactId = insertContact(db, rawContactId);
        setContactId(rawContactId, contactId);
        mDbHelper.updateContactVisible(txContext, contactId);
        return contactId;
    }

    protected long insertContact(SQLiteDatabase db, long rawContactId) {
        mSelectionArgs1[0] = String.valueOf(rawContactId);
        computeAggregateData(db, mRawContactsQueryByRawContactId, mSelectionArgs1, mContactInsert);
        return mContactInsert.executeInsert();
    }

    private static final class RawContactIdAndAccountQuery {
        public static final String TABLE = Tables.RAW_CONTACTS;

        public static final String[] COLUMNS = {
                RawContacts.CONTACT_ID,
                RawContactsColumns.ACCOUNT_ID
        };

        public static final String SELECTION = RawContacts._ID + "=?";

        public static final int CONTACT_ID = 0;
        public static final int ACCOUNT_ID = 1;
    }

    public void aggregateContact(
            TransactionContext txContext, SQLiteDatabase db, long rawContactId) {
        if (!mEnabled) {
            return;
        }

        MatchCandidateList candidates = new MatchCandidateList();
        ContactMatcher matcher = new ContactMatcher();

        long contactId = 0;
        long accountId = 0;
        mSelectionArgs1[0] = String.valueOf(rawContactId);
        Cursor cursor = db.query(RawContactIdAndAccountQuery.TABLE,
                RawContactIdAndAccountQuery.COLUMNS, RawContactIdAndAccountQuery.SELECTION,
                mSelectionArgs1, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                contactId = cursor.getLong(RawContactIdAndAccountQuery.CONTACT_ID);
                accountId = cursor.getLong(RawContactIdAndAccountQuery.ACCOUNT_ID);
            }
        } finally {
            cursor.close();
        }

        aggregateContact(txContext, db, rawContactId, accountId, contactId,
                candidates, matcher);
    }

    public void updateAggregateData(TransactionContext txContext, long contactId) {
        if (!mEnabled) {
            return;
        }

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        computeAggregateData(db, contactId, mContactUpdate);
        mContactUpdate.bindLong(ContactReplaceSqlStatement.CONTACT_ID, contactId);
        mContactUpdate.execute();

        mDbHelper.updateContactVisible(txContext, contactId);
        updateAggregatedStatusUpdate(contactId);
    }

    private void updateAggregatedStatusUpdate(long contactId) {
        mAggregatedPresenceReplace.bindLong(1, contactId);
        mAggregatedPresenceReplace.bindLong(2, contactId);
        mAggregatedPresenceReplace.execute();
        updateLastStatusUpdateId(contactId);
    }

    /**
     * Adjusts the reference to the latest status update for the specified contact.
     */
    public void updateLastStatusUpdateId(long contactId) {
        String contactIdString = String.valueOf(contactId);
        mDbHelper.getWritableDatabase().execSQL(UPDATE_LAST_STATUS_UPDATE_ID_SQL,
                new String[]{contactIdString, contactIdString});
    }

    /**
     * Given a specific raw contact, finds all matching aggregate contacts and chooses the one
     * with the highest match score.  If no such contact is found, creates a new contact.
     */
    private synchronized void aggregateContact(TransactionContext txContext, SQLiteDatabase db,
            long rawContactId, long accountId, long currentContactId, MatchCandidateList candidates,
            ContactMatcher matcher) {

        if (VERBOSE_LOGGING) {
            Log.v(TAG, "aggregateContact: rid=" + rawContactId + " cid=" + currentContactId);
        }

        int aggregationMode = RawContacts.AGGREGATION_MODE_DEFAULT;

        Integer aggModeObject = mRawContactsMarkedForAggregation.remove(rawContactId);
        if (aggModeObject != null) {
            aggregationMode = aggModeObject;
        }

        long contactId = -1; // Best matching contact ID.
        long contactIdToSplit = -1;

        if (aggregationMode == RawContacts.AGGREGATION_MODE_DEFAULT) {
            candidates.clear();
            matcher.clear();

            contactId = pickBestMatchBasedOnExceptions(db, rawContactId, matcher);
            if (contactId == -1) {

                // If this is a newly inserted contact or a visible contact, look for
                // data matches.
                if (currentContactId == 0
                        || mDbHelper.isContactInDefaultDirectory(db, currentContactId)) {
                    contactId = pickBestMatchBasedOnData(db, rawContactId, candidates, matcher);
                }

                // If we found an aggregate to join, but it already contains raw contacts from
                // the same account, not only will we not join it, but also we will split
                // that other aggregate
                if (contactId != -1 && contactId != currentContactId &&
                        !canJoinIntoContact(db, contactId, rawContactId, accountId)) {
                    contactIdToSplit = contactId;
                    contactId = -1;
                }
            }
        } else if (aggregationMode == RawContacts.AGGREGATION_MODE_DISABLED) {
            return;
        }

        // # of raw_contacts in the [currentContactId] contact excluding the [rawContactId]
        // raw_contact.
        long currentContactContentsCount = 0;

        if (currentContactId != 0) {
            mRawContactCountQuery.bindLong(1, currentContactId);
            mRawContactCountQuery.bindLong(2, rawContactId);
            currentContactContentsCount = mRawContactCountQuery.simpleQueryForLong();
        }

        // If there are no other raw contacts in the current aggregate, we might as well reuse it.
        // Also, if the aggregation mode is SUSPENDED, we must reuse the same aggregate.
        if (contactId == -1
                && currentContactId != 0
                && (currentContactContentsCount == 0
                        || aggregationMode == RawContacts.AGGREGATION_MODE_SUSPENDED)) {
            contactId = currentContactId;
        }

        if (contactId == currentContactId) {
            // Aggregation unchanged
            markAggregated(rawContactId);
        } else if (contactId == -1) {
            // Splitting an aggregate
            createNewContactForRawContact(txContext, db, rawContactId);
            if (currentContactContentsCount > 0) {
                updateAggregateData(txContext, currentContactId);
            }
        } else {
            // Joining with an existing aggregate
            if (currentContactContentsCount == 0) {
                // Delete a previous aggregate if it only contained this raw contact
                ContactsTableUtil.deleteContact(db, currentContactId);

                mAggregatedPresenceDelete.bindLong(1, currentContactId);
                mAggregatedPresenceDelete.execute();
            }

            setContactIdAndMarkAggregated(rawContactId, contactId);
            computeAggregateData(db, contactId, mContactUpdate);
            mContactUpdate.bindLong(ContactReplaceSqlStatement.CONTACT_ID, contactId);
            mContactUpdate.execute();
            mDbHelper.updateContactVisible(txContext, contactId);
            updateAggregatedStatusUpdate(contactId);
            // Make sure the raw contact does not contribute to the current contact
            if (currentContactId != 0) {
                updateAggregateData(txContext, currentContactId);
            }
        }

        if (contactIdToSplit != -1) {
            splitAutomaticallyAggregatedRawContacts(txContext, db, contactIdToSplit);
        }
    }

    /**
     * @return true if the raw contact of {@code rawContactId} can be joined into the existing
     * contact of {@code of contactId}.
     *
     * Now a raw contact can be merged into a contact containing raw contacts from
     * the same account if there's at least one raw contact in those raw contacts
     * that shares at least one email address, phone number, or identity.
     */
    private boolean canJoinIntoContact(SQLiteDatabase db, long contactId,
            long rawContactId, long rawContactAccountId) {
        // First, list all raw contact IDs in contact [contactId] on account [rawContactAccountId],
        // excluding raw_contact [rawContactId].

        // Append all found raw contact IDs into this SB to create a comma separated list of
        // the IDs.
        // We don't always need it, so lazily initialize it.
        StringBuilder rawContactIdsBuilder;

        mSelectionArgs3[0] = String.valueOf(contactId);
        mSelectionArgs3[1] = String.valueOf(rawContactId);
        mSelectionArgs3[2] = String.valueOf(rawContactAccountId);
        final Cursor duplicatesCursor = db.rawQuery(
                "SELECT " + RawContacts._ID +
                " FROM " + Tables.RAW_CONTACTS +
                " WHERE " + RawContacts.CONTACT_ID + "=?" +
                " AND " + RawContacts._ID + "!=?" +
                " AND " + RawContactsColumns.ACCOUNT_ID +"=?",
                mSelectionArgs3);
        try {
            final int duplicateCount = duplicatesCursor.getCount();
            if (duplicateCount == 0) {
                return true; // No duplicates -- common case -- bail early.
            }
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "canJoinIntoContact: " + duplicateCount + " duplicate(s) found");
            }

            rawContactIdsBuilder = new StringBuilder();

            duplicatesCursor.moveToPosition(-1);
            while (duplicatesCursor.moveToNext()) {
                if (rawContactIdsBuilder.length() > 0) {
                    rawContactIdsBuilder.append(',');
                }
                rawContactIdsBuilder.append(duplicatesCursor.getLong(0));
            }
        } finally {
            duplicatesCursor.close();
        }

        // Comma separated raw_contacts IDs.
        final String rawContactIds = rawContactIdsBuilder.toString();

        // See if there's any raw_contacts that share an email address, a phone number, or
        // an identity with raw_contact [rawContactId].

        // First, check for the email address.
        mSelectionArgs2[0] = String.valueOf(mMimeTypeIdEmail);
        mSelectionArgs2[1] = String.valueOf(rawContactId);
        if (isFirstColumnGreaterThanZero(db,
                "SELECT count(*)" +
                " FROM " + Tables.DATA + " AS d1" +
                " JOIN " + Tables.DATA + " AS d2"
                    + " ON (d1." + Email.ADDRESS + " = d2." + Email.ADDRESS + ")" +
                " WHERE d1." + DataColumns.MIMETYPE_ID + " = ?1" +
                " AND d2." + DataColumns.MIMETYPE_ID + " = ?1" +
                " AND d1." + Data.RAW_CONTACT_ID + " = ?2" +
                " AND d2." + Data.RAW_CONTACT_ID + " IN (" + rawContactIds + ")",
                mSelectionArgs2)) {
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "Relaxing rule SA: email match found for rid=" + rawContactId);
            }
            return true;
        }

        // Next, check for the identity.
        mSelectionArgs2[0] = String.valueOf(mMimeTypeIdIdentity);
        mSelectionArgs2[1] = String.valueOf(rawContactId);
        if (isFirstColumnGreaterThanZero(db,
                "SELECT count(*)" +
                " FROM " + Tables.DATA + " AS d1" +
                " JOIN " + Tables.DATA + " AS d2"
                    + " ON (d1." + Identity.IDENTITY + " = d2." + Identity.IDENTITY + " AND" +
                           " d1." + Identity.NAMESPACE + " = d2." + Identity.NAMESPACE + " )" +
                " WHERE d1." + DataColumns.MIMETYPE_ID + " = ?1" +
                " AND d2." + DataColumns.MIMETYPE_ID + " = ?1" +
                " AND d1." + Data.RAW_CONTACT_ID + " = ?2" +
                " AND d2." + Data.RAW_CONTACT_ID + " IN (" + rawContactIds + ")",
                mSelectionArgs2)) {
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "Relaxing rule SA: identity match found for rid=" + rawContactId);
            }
            return true;
        }

        // Lastly, the phone number.
        // It's a bit tricker because it has to be consistent with
        // updateMatchScoresBasedOnPhoneMatches().
        mSelectionArgs3[0] = String.valueOf(mMimeTypeIdPhone);
        mSelectionArgs3[1] = String.valueOf(rawContactId);
        mSelectionArgs3[2] = String.valueOf(mDbHelper.getUseStrictPhoneNumberComparisonParameter());

        if (isFirstColumnGreaterThanZero(db,
                "SELECT count(*)" +
                " FROM " + Tables.PHONE_LOOKUP + " AS p1" +
                " JOIN " + Tables.DATA + " AS d1 ON " +
                    "(d1." + Data._ID + "=p1." + PhoneLookupColumns.DATA_ID + ")" +
                " JOIN " + Tables.PHONE_LOOKUP + " AS p2 ON (p1." + PhoneLookupColumns.MIN_MATCH +
                    "=p2." + PhoneLookupColumns.MIN_MATCH + ")" +
                " JOIN " + Tables.DATA + " AS d2 ON " +
                    "(d2." + Data._ID + "=p2." + PhoneLookupColumns.DATA_ID + ")" +
                " WHERE d1." + DataColumns.MIMETYPE_ID + " = ?1" +
                " AND d2." + DataColumns.MIMETYPE_ID + " = ?1" +
                " AND d1." + Data.RAW_CONTACT_ID + " = ?2" +
                " AND d2." + Data.RAW_CONTACT_ID + " IN (" + rawContactIds + ")" +
                " AND PHONE_NUMBERS_EQUAL(d1." + Phone.NUMBER + ",d2." + Phone.NUMBER + ",?3)",
                mSelectionArgs3)) {
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "Relaxing rule SA: phone match found for rid=" + rawContactId);
            }
            return true;
        }
        if (VERBOSE_LOGGING) {
            Log.v(TAG, "Rule SA splitting up cid=" + contactId + " for rid=" + rawContactId);
        }
        return false;
    }

    private boolean isFirstColumnGreaterThanZero(SQLiteDatabase db, String query,
            String[] selectionArgs) {
        final Cursor cursor = db.rawQuery(query, selectionArgs);
        try {
            return cursor.moveToFirst() && (cursor.getInt(0) > 0);
        } finally {
            cursor.close();
        }
    }

    /**
     * Breaks up an existing aggregate when a new raw contact is inserted that has
     * come from the same account as one of the raw contacts in this aggregate.
     */
    private void splitAutomaticallyAggregatedRawContacts(
            TransactionContext txContext, SQLiteDatabase db, long contactId) {
        mSelectionArgs1[0] = String.valueOf(contactId);
        int count = (int) DatabaseUtils.longForQuery(db,
                "SELECT COUNT(" + RawContacts._ID + ")" +
                " FROM " + Tables.RAW_CONTACTS +
                " WHERE " + RawContacts.CONTACT_ID + "=?", mSelectionArgs1);
        if (count < 2) {
            // A single-raw-contact aggregate does not need to be split up
            return;
        }

        // Find all constituent raw contacts that are not held together by
        // an explicit aggregation exception
        String query =
                "SELECT " + RawContacts._ID +
                " FROM " + Tables.RAW_CONTACTS +
                " WHERE " + RawContacts.CONTACT_ID + "=?" +
                "   AND " + RawContacts._ID + " NOT IN " +
                        "(SELECT " + AggregationExceptions.RAW_CONTACT_ID1 +
                        " FROM " + Tables.AGGREGATION_EXCEPTIONS +
                        " WHERE " + AggregationExceptions.TYPE + "="
                                + AggregationExceptions.TYPE_KEEP_TOGETHER +
                        " UNION SELECT " + AggregationExceptions.RAW_CONTACT_ID2 +
                        " FROM " + Tables.AGGREGATION_EXCEPTIONS +
                        " WHERE " + AggregationExceptions.TYPE + "="
                                + AggregationExceptions.TYPE_KEEP_TOGETHER +
                        ")";

        Cursor cursor = db.rawQuery(query, mSelectionArgs1);
        try {
            // Process up to count-1 raw contact, leaving the last one alone.
            for (int i = 0; i < count - 1; i++) {
                if (!cursor.moveToNext()) {
                    break;
                }
                long rawContactId = cursor.getLong(0);
                createNewContactForRawContact(txContext, db, rawContactId);
            }
        } finally {
            cursor.close();
        }
        if (contactId > 0) {
            updateAggregateData(txContext, contactId);
        }
    }

    /**
     * Creates a stand-alone Contact for the given raw contact ID. This is only called
     * when splitting an existing merged contact into separate raw contacts.
     */
    private void createNewContactForRawContact(
            TransactionContext txContext, SQLiteDatabase db, long rawContactId) {
        // All split contacts should automatically be unpinned.
        unpinRawContact(rawContactId);
        mSelectionArgs1[0] = String.valueOf(rawContactId);
        computeAggregateData(db, mRawContactsQueryByRawContactId, mSelectionArgs1,
                mContactInsert);
        long contactId = mContactInsert.executeInsert();
        setContactIdAndMarkAggregated(rawContactId, contactId);
        mDbHelper.updateContactVisible(txContext, contactId);
        setPresenceContactId(rawContactId, contactId);
        updateAggregatedStatusUpdate(contactId);
    }

    private static class RawContactIdQuery {
        public static final String TABLE = Tables.RAW_CONTACTS;
        public static final String[] COLUMNS = { RawContacts._ID };
        public static final String SELECTION = RawContacts.CONTACT_ID + "=?";
        public static final int RAW_CONTACT_ID = 0;
    }

    /**
     * Ensures that automatic aggregation rules are followed after a contact
     * becomes visible or invisible. Specifically, consider this case: there are
     * three contacts named Foo. Two of them come from account A1 and one comes
     * from account A2. The aggregation rules say that in this case none of the
     * three Foo's should be aggregated: two of them are in the same account, so
     * they don't get aggregated; the third has two affinities, so it does not
     * join either of them.
     * <p>
     * Consider what happens if one of the "Foo"s from account A1 becomes
     * invisible. Nothing stands in the way of aggregating the other two
     * anymore, so they should get joined.
     * <p>
     * What if the invisible "Foo" becomes visible after that? We should split the
     * aggregate between the other two.
     */
    public void updateAggregationAfterVisibilityChange(long contactId) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        boolean visible = mDbHelper.isContactInDefaultDirectory(db, contactId);
        if (visible) {
            markContactForAggregation(db, contactId);
        } else {
            // Find all contacts that _could be_ aggregated with this one and
            // rerun aggregation for all of them
            mSelectionArgs1[0] = String.valueOf(contactId);
            Cursor cursor = db.query(RawContactIdQuery.TABLE, RawContactIdQuery.COLUMNS,
                    RawContactIdQuery.SELECTION, mSelectionArgs1, null, null, null);
            try {
                while (cursor.moveToNext()) {
                    long rawContactId = cursor.getLong(RawContactIdQuery.RAW_CONTACT_ID);
                    mMatcher.clear();

                    updateMatchScoresBasedOnIdentityMatch(db, rawContactId, mMatcher);
                    updateMatchScoresBasedOnNameMatches(db, rawContactId, mMatcher);
                    List<MatchScore> bestMatches =
                            mMatcher.pickBestMatches(ContactMatcher.SCORE_THRESHOLD_PRIMARY);
                    for (MatchScore matchScore : bestMatches) {
                        markContactForAggregation(db, matchScore.getContactId());
                    }

                    mMatcher.clear();
                    updateMatchScoresBasedOnEmailMatches(db, rawContactId, mMatcher);
                    updateMatchScoresBasedOnPhoneMatches(db, rawContactId, mMatcher);
                    bestMatches =
                            mMatcher.pickBestMatches(ContactMatcher.SCORE_THRESHOLD_SECONDARY);
                    for (MatchScore matchScore : bestMatches) {
                        markContactForAggregation(db, matchScore.getContactId());
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Updates the contact ID for the specified contact.
     */
    protected void setContactId(long rawContactId, long contactId) {
        mContactIdUpdate.bindLong(1, contactId);
        mContactIdUpdate.bindLong(2, rawContactId);
        mContactIdUpdate.execute();
    }

    /**
     * Marks the specified raw contact ID as aggregated
     */
    private void markAggregated(long rawContactId) {
        mMarkAggregatedUpdate.bindLong(1, rawContactId);
        mMarkAggregatedUpdate.execute();
    }

    /**
     * Updates the contact ID for the specified contact and marks the raw contact as aggregated.
     */
    private void setContactIdAndMarkAggregated(long rawContactId, long contactId) {
        mContactIdAndMarkAggregatedUpdate.bindLong(1, contactId);
        mContactIdAndMarkAggregatedUpdate.bindLong(2, rawContactId);
        mContactIdAndMarkAggregatedUpdate.execute();
    }

    private void setPresenceContactId(long rawContactId, long contactId) {
        mPresenceContactIdUpdate.bindLong(1, contactId);
        mPresenceContactIdUpdate.bindLong(2, rawContactId);
        mPresenceContactIdUpdate.execute();
    }

    private void unpinRawContact(long rawContactId) {
        mResetPinnedForRawContact.bindLong(1, rawContactId);
        mResetPinnedForRawContact.execute();
    }

    interface AggregateExceptionPrefetchQuery {
        String TABLE = Tables.AGGREGATION_EXCEPTIONS;

        String[] COLUMNS = {
            AggregationExceptions.RAW_CONTACT_ID1,
            AggregationExceptions.RAW_CONTACT_ID2,
        };

        int RAW_CONTACT_ID1 = 0;
        int RAW_CONTACT_ID2 = 1;
    }

    // A set of raw contact IDs for which there are aggregation exceptions
    private final HashSet<Long> mAggregationExceptionIds = new HashSet<Long>();
    private boolean mAggregationExceptionIdsValid;

    public void invalidateAggregationExceptionCache() {
        mAggregationExceptionIdsValid = false;
    }

    /**
     * Finds all raw contact IDs for which there are aggregation exceptions. The list of
     * ids is used as an optimization in aggregation: there is no point to run a query against
     * the agg_exceptions table if it is known that there are no records there for a given
     * raw contact ID.
     */
    private void prefetchAggregationExceptionIds(SQLiteDatabase db) {
        mAggregationExceptionIds.clear();
        final Cursor c = db.query(AggregateExceptionPrefetchQuery.TABLE,
                AggregateExceptionPrefetchQuery.COLUMNS,
                null, null, null, null, null);

        try {
            while (c.moveToNext()) {
                long rawContactId1 = c.getLong(AggregateExceptionPrefetchQuery.RAW_CONTACT_ID1);
                long rawContactId2 = c.getLong(AggregateExceptionPrefetchQuery.RAW_CONTACT_ID2);
                mAggregationExceptionIds.add(rawContactId1);
                mAggregationExceptionIds.add(rawContactId2);
            }
        } finally {
            c.close();
        }

        mAggregationExceptionIdsValid = true;
    }

    interface AggregateExceptionQuery {
        String TABLE = Tables.AGGREGATION_EXCEPTIONS
            + " JOIN raw_contacts raw_contacts1 "
                    + " ON (agg_exceptions.raw_contact_id1 = raw_contacts1._id) "
            + " JOIN raw_contacts raw_contacts2 "
                    + " ON (agg_exceptions.raw_contact_id2 = raw_contacts2._id) ";

        String[] COLUMNS = {
            AggregationExceptions.TYPE,
            AggregationExceptions.RAW_CONTACT_ID1,
            "raw_contacts1." + RawContacts.CONTACT_ID,
            "raw_contacts1." + RawContactsColumns.AGGREGATION_NEEDED,
            "raw_contacts2." + RawContacts.CONTACT_ID,
            "raw_contacts2." + RawContactsColumns.AGGREGATION_NEEDED,
        };

        int TYPE = 0;
        int RAW_CONTACT_ID1 = 1;
        int CONTACT_ID1 = 2;
        int AGGREGATION_NEEDED_1 = 3;
        int CONTACT_ID2 = 4;
        int AGGREGATION_NEEDED_2 = 5;
    }

    /**
     * Computes match scores based on exceptions entered by the user: always match and never match.
     * Returns the aggregate contact with the always match exception if any.
     */
    private long pickBestMatchBasedOnExceptions(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {
        if (!mAggregationExceptionIdsValid) {
            prefetchAggregationExceptionIds(db);
        }

        // If there are no aggregation exceptions involving this raw contact, there is no need to
        // run a query and we can just return -1, which stands for "nothing found"
        if (!mAggregationExceptionIds.contains(rawContactId)) {
            return -1;
        }

        final Cursor c = db.query(AggregateExceptionQuery.TABLE,
                AggregateExceptionQuery.COLUMNS,
                AggregationExceptions.RAW_CONTACT_ID1 + "=" + rawContactId
                        + " OR " + AggregationExceptions.RAW_CONTACT_ID2 + "=" + rawContactId,
                null, null, null, null);

        try {
            while (c.moveToNext()) {
                int type = c.getInt(AggregateExceptionQuery.TYPE);
                long rawContactId1 = c.getLong(AggregateExceptionQuery.RAW_CONTACT_ID1);
                long contactId = -1;
                if (rawContactId == rawContactId1) {
                    if (c.getInt(AggregateExceptionQuery.AGGREGATION_NEEDED_2) == 0
                            && !c.isNull(AggregateExceptionQuery.CONTACT_ID2)) {
                        contactId = c.getLong(AggregateExceptionQuery.CONTACT_ID2);
                    }
                } else {
                    if (c.getInt(AggregateExceptionQuery.AGGREGATION_NEEDED_1) == 0
                            && !c.isNull(AggregateExceptionQuery.CONTACT_ID1)) {
                        contactId = c.getLong(AggregateExceptionQuery.CONTACT_ID1);
                    }
                }
                if (contactId != -1) {
                    if (type == AggregationExceptions.TYPE_KEEP_TOGETHER) {
                        matcher.keepIn(contactId);
                    } else {
                        matcher.keepOut(contactId);
                    }
                }
            }
        } finally {
            c.close();
        }

        return matcher.pickBestMatch(ContactMatcher.MAX_SCORE, true);
    }

    /**
     * Picks the best matching contact based on matches between data elements.  It considers
     * name match to be primary and phone, email etc matches to be secondary.  A good primary
     * match triggers aggregation, while a good secondary match only triggers aggregation in
     * the absence of a strong primary mismatch.
     * <p>
     * Consider these examples:
     * <p>
     * John Doe with phone number 111-111-1111 and Jon Doe with phone number 111-111-1111 should
     * be aggregated (same number, similar names).
     * <p>
     * John Doe with phone number 111-111-1111 and Deborah Doe with phone number 111-111-1111 should
     * not be aggregated (same number, different names).
     */
    private long pickBestMatchBasedOnData(SQLiteDatabase db, long rawContactId,
            MatchCandidateList candidates, ContactMatcher matcher) {

        // Find good matches based on name alone
        long bestMatch = updateMatchScoresBasedOnDataMatches(db, rawContactId, matcher);
        if (bestMatch == ContactMatcher.MULTIPLE_MATCHES) {
            // We found multiple matches on the name - do not aggregate because of the ambiguity
            return -1;
        } else if (bestMatch == -1) {
            // We haven't found a good match on name, see if we have any matches on phone, email etc
            bestMatch = pickBestMatchBasedOnSecondaryData(db, rawContactId, candidates, matcher);
            if (bestMatch == ContactMatcher.MULTIPLE_MATCHES) {
                return -1;
            }
        }

        return bestMatch;
    }


    /**
     * Picks the best matching contact based on secondary data matches.  The method loads
     * structured names for all candidate contacts and recomputes match scores using approximate
     * matching.
     */
    private long pickBestMatchBasedOnSecondaryData(SQLiteDatabase db,
            long rawContactId, MatchCandidateList candidates, ContactMatcher matcher) {
        List<Long> secondaryContactIds = matcher.prepareSecondaryMatchCandidates(
                ContactMatcher.SCORE_THRESHOLD_PRIMARY);
        if (secondaryContactIds == null || secondaryContactIds.size() > SECONDARY_HIT_LIMIT) {
            return -1;
        }

        loadNameMatchCandidates(db, rawContactId, candidates, true);

        mSb.setLength(0);
        mSb.append(RawContacts.CONTACT_ID).append(" IN (");
        for (int i = 0; i < secondaryContactIds.size(); i++) {
            if (i != 0) {
                mSb.append(',');
            }
            mSb.append(secondaryContactIds.get(i));
        }

        // We only want to compare structured names to structured names
        // at this stage, we need to ignore all other sources of name lookup data.
        mSb.append(") AND " + STRUCTURED_NAME_BASED_LOOKUP_SQL);

        matchAllCandidates(db, mSb.toString(), candidates, matcher,
                ContactMatcher.MATCHING_ALGORITHM_CONSERVATIVE, null);

        return matcher.pickBestMatch(ContactMatcher.SCORE_THRESHOLD_SECONDARY, false);
    }

    private interface NameLookupQuery {
        String TABLE = Tables.NAME_LOOKUP;

        String SELECTION = NameLookupColumns.RAW_CONTACT_ID + "=?";
        String SELECTION_STRUCTURED_NAME_BASED =
                SELECTION + " AND " + STRUCTURED_NAME_BASED_LOOKUP_SQL;

        String[] COLUMNS = new String[] {
                NameLookupColumns.NORMALIZED_NAME,
                NameLookupColumns.NAME_TYPE
        };

        int NORMALIZED_NAME = 0;
        int NAME_TYPE = 1;
    }

    private void loadNameMatchCandidates(SQLiteDatabase db, long rawContactId,
            MatchCandidateList candidates, boolean structuredNameBased) {
        candidates.clear();
        mSelectionArgs1[0] = String.valueOf(rawContactId);
        Cursor c = db.query(NameLookupQuery.TABLE, NameLookupQuery.COLUMNS,
                structuredNameBased
                        ? NameLookupQuery.SELECTION_STRUCTURED_NAME_BASED
                        : NameLookupQuery.SELECTION,
                mSelectionArgs1, null, null, null);
        try {
            while (c.moveToNext()) {
                String normalizedName = c.getString(NameLookupQuery.NORMALIZED_NAME);
                int type = c.getInt(NameLookupQuery.NAME_TYPE);
                candidates.add(normalizedName, type);
            }
        } finally {
            c.close();
        }
    }

    /**
     * Computes scores for contacts that have matching data rows.
     */
    private long updateMatchScoresBasedOnDataMatches(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {

        updateMatchScoresBasedOnIdentityMatch(db, rawContactId, matcher);
        updateMatchScoresBasedOnNameMatches(db, rawContactId, matcher);
        long bestMatch = matcher.pickBestMatch(ContactMatcher.SCORE_THRESHOLD_PRIMARY, false);
        if (bestMatch != -1) {
            return bestMatch;
        }

        updateMatchScoresBasedOnEmailMatches(db, rawContactId, matcher);
        updateMatchScoresBasedOnPhoneMatches(db, rawContactId, matcher);

        return -1;
    }

    private interface IdentityLookupMatchQuery {
        final String TABLE = Tables.DATA + " dataA"
                + " JOIN " + Tables.DATA + " dataB" +
                " ON (dataA." + Identity.NAMESPACE + "=dataB." + Identity.NAMESPACE +
                " AND dataA." + Identity.IDENTITY + "=dataB." + Identity.IDENTITY + ")"
                + " JOIN " + Tables.RAW_CONTACTS +
                " ON (dataB." + Data.RAW_CONTACT_ID + " = "
                + Tables.RAW_CONTACTS + "." + RawContacts._ID + ")";

        final String SELECTION = "dataA." + Data.RAW_CONTACT_ID + "=?1"
                + " AND dataA." + DataColumns.MIMETYPE_ID + "=?2"
                + " AND dataA." + Identity.NAMESPACE + " NOT NULL"
                + " AND dataA." + Identity.IDENTITY + " NOT NULL"
                + " AND dataB." + DataColumns.MIMETYPE_ID + "=?2"
                + " AND " + RawContactsColumns.AGGREGATION_NEEDED + "=0"
                + " AND " + RawContacts.CONTACT_ID + " IN " + Tables.DEFAULT_DIRECTORY;

        final String[] COLUMNS = new String[] {
            RawContacts.CONTACT_ID
        };

        int CONTACT_ID = 0;
    }

    /**
     * Finds contacts with exact identity matches to the the specified raw contact.
     */
    private void updateMatchScoresBasedOnIdentityMatch(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {
        mSelectionArgs2[0] = String.valueOf(rawContactId);
        mSelectionArgs2[1] = String.valueOf(mMimeTypeIdIdentity);
        Cursor c = db.query(IdentityLookupMatchQuery.TABLE, IdentityLookupMatchQuery.COLUMNS,
                IdentityLookupMatchQuery.SELECTION,
                mSelectionArgs2, RawContacts.CONTACT_ID, null, null);
        try {
            while (c.moveToNext()) {
                final long contactId = c.getLong(IdentityLookupMatchQuery.CONTACT_ID);
                matcher.matchIdentity(contactId);
            }
        } finally {
            c.close();
        }

    }

    private interface NameLookupMatchQuery {
        String TABLE = Tables.NAME_LOOKUP + " nameA"
                + " JOIN " + Tables.NAME_LOOKUP + " nameB" +
                " ON (" + "nameA." + NameLookupColumns.NORMALIZED_NAME + "="
                        + "nameB." + NameLookupColumns.NORMALIZED_NAME + ")"
                + " JOIN " + Tables.RAW_CONTACTS +
                " ON (nameB." + NameLookupColumns.RAW_CONTACT_ID + " = "
                        + Tables.RAW_CONTACTS + "." + RawContacts._ID + ")";

        String SELECTION = "nameA." + NameLookupColumns.RAW_CONTACT_ID + "=?"
                + " AND " + RawContactsColumns.AGGREGATION_NEEDED + "=0"
                + " AND " + RawContacts.CONTACT_ID + " IN " + Tables.DEFAULT_DIRECTORY;

        String[] COLUMNS = new String[] {
            RawContacts.CONTACT_ID,
            "nameA." + NameLookupColumns.NORMALIZED_NAME,
            "nameA." + NameLookupColumns.NAME_TYPE,
            "nameB." + NameLookupColumns.NAME_TYPE,
        };

        int CONTACT_ID = 0;
        int NAME = 1;
        int NAME_TYPE_A = 2;
        int NAME_TYPE_B = 3;
    }

    /**
     * Finds contacts with names matching the name of the specified raw contact.
     */
    private void updateMatchScoresBasedOnNameMatches(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {
        mSelectionArgs1[0] = String.valueOf(rawContactId);
        Cursor c = db.query(NameLookupMatchQuery.TABLE, NameLookupMatchQuery.COLUMNS,
                NameLookupMatchQuery.SELECTION,
                mSelectionArgs1, null, null, null, PRIMARY_HIT_LIMIT_STRING);
        try {
            while (c.moveToNext()) {
                long contactId = c.getLong(NameLookupMatchQuery.CONTACT_ID);
                String name = c.getString(NameLookupMatchQuery.NAME);
                int nameTypeA = c.getInt(NameLookupMatchQuery.NAME_TYPE_A);
                int nameTypeB = c.getInt(NameLookupMatchQuery.NAME_TYPE_B);
                matcher.matchName(contactId, nameTypeA, name,
                        nameTypeB, name, ContactMatcher.MATCHING_ALGORITHM_EXACT);
                if (nameTypeA == NameLookupType.NICKNAME &&
                        nameTypeB == NameLookupType.NICKNAME) {
                    matcher.updateScoreWithNicknameMatch(contactId);
                }
            }
        } finally {
            c.close();
        }
    }

    private interface NameLookupMatchQueryWithParameter {
        String TABLE = Tables.NAME_LOOKUP
                + " JOIN " + Tables.RAW_CONTACTS +
                " ON (" + NameLookupColumns.RAW_CONTACT_ID + " = "
                        + Tables.RAW_CONTACTS + "." + RawContacts._ID + ")";

        String[] COLUMNS = new String[] {
            RawContacts.CONTACT_ID,
            NameLookupColumns.NORMALIZED_NAME,
            NameLookupColumns.NAME_TYPE,
        };

        int CONTACT_ID = 0;
        int NAME = 1;
        int NAME_TYPE = 2;
    }

    private final class NameLookupSelectionBuilder extends NameLookupBuilder {

        private final MatchCandidateList mNameLookupCandidates;

        private StringBuilder mSelection = new StringBuilder(
                NameLookupColumns.NORMALIZED_NAME + " IN(");


        public NameLookupSelectionBuilder(NameSplitter splitter, MatchCandidateList candidates) {
            super(splitter);
            this.mNameLookupCandidates = candidates;
        }

        @Override
        protected String[] getCommonNicknameClusters(String normalizedName) {
            return mCommonNicknameCache.getCommonNicknameClusters(normalizedName);
        }

        @Override
        protected void insertNameLookup(
                long rawContactId, long dataId, int lookupType, String string) {
            mNameLookupCandidates.add(string, lookupType);
            DatabaseUtils.appendEscapedSQLString(mSelection, string);
            mSelection.append(',');
        }

        public boolean isEmpty() {
            return mNameLookupCandidates.isEmpty();
        }

        public String getSelection() {
            mSelection.setLength(mSelection.length() - 1);      // Strip last comma
            mSelection.append(')');
            return mSelection.toString();
        }

        public int getLookupType(String name) {
            for (int i = 0; i < mNameLookupCandidates.mCount; i++) {
                if (mNameLookupCandidates.mList.get(i).mName.equals(name)) {
                    return mNameLookupCandidates.mList.get(i).mLookupType;
                }
            }
            throw new IllegalStateException();
        }
    }

    /**
     * Finds contacts with names matching the specified name.
     */
    private void updateMatchScoresBasedOnNameMatches(SQLiteDatabase db, String query,
            MatchCandidateList candidates, ContactMatcher matcher) {
        candidates.clear();
        NameLookupSelectionBuilder builder = new NameLookupSelectionBuilder(
                mNameSplitter, candidates);
        builder.insertNameLookup(0, 0, query, FullNameStyle.UNDEFINED);
        if (builder.isEmpty()) {
            return;
        }

        Cursor c = db.query(NameLookupMatchQueryWithParameter.TABLE,
                NameLookupMatchQueryWithParameter.COLUMNS, builder.getSelection(), null, null, null,
                null, PRIMARY_HIT_LIMIT_STRING);
        try {
            while (c.moveToNext()) {
                long contactId = c.getLong(NameLookupMatchQueryWithParameter.CONTACT_ID);
                String name = c.getString(NameLookupMatchQueryWithParameter.NAME);
                int nameTypeA = builder.getLookupType(name);
                int nameTypeB = c.getInt(NameLookupMatchQueryWithParameter.NAME_TYPE);
                matcher.matchName(contactId, nameTypeA, name, nameTypeB, name,
                        ContactMatcher.MATCHING_ALGORITHM_EXACT);
                if (nameTypeA == NameLookupType.NICKNAME && nameTypeB == NameLookupType.NICKNAME) {
                    matcher.updateScoreWithNicknameMatch(contactId);
                }
            }
        } finally {
            c.close();
        }
    }

    private interface EmailLookupQuery {
        String TABLE = Tables.DATA + " dataA"
                + " JOIN " + Tables.DATA + " dataB" +
                " ON (" + "dataA." + Email.DATA + "=dataB." + Email.DATA + ")"
                + " JOIN " + Tables.RAW_CONTACTS +
                " ON (dataB." + Data.RAW_CONTACT_ID + " = "
                        + Tables.RAW_CONTACTS + "." + RawContacts._ID + ")";

        String SELECTION = "dataA." + Data.RAW_CONTACT_ID + "=?1"
                + " AND dataA." + DataColumns.MIMETYPE_ID + "=?2"
                + " AND dataA." + Email.DATA + " NOT NULL"
                + " AND dataB." + DataColumns.MIMETYPE_ID + "=?2"
                + " AND " + RawContactsColumns.AGGREGATION_NEEDED + "=0"
                + " AND " + RawContacts.CONTACT_ID + " IN " + Tables.DEFAULT_DIRECTORY;

        String[] COLUMNS = new String[] {
            RawContacts.CONTACT_ID
        };

        int CONTACT_ID = 0;
    }

    private void updateMatchScoresBasedOnEmailMatches(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {
        mSelectionArgs2[0] = String.valueOf(rawContactId);
        mSelectionArgs2[1] = String.valueOf(mMimeTypeIdEmail);
        Cursor c = db.query(EmailLookupQuery.TABLE, EmailLookupQuery.COLUMNS,
                EmailLookupQuery.SELECTION,
                mSelectionArgs2, null, null, null, SECONDARY_HIT_LIMIT_STRING);
        try {
            while (c.moveToNext()) {
                long contactId = c.getLong(EmailLookupQuery.CONTACT_ID);
                matcher.updateScoreWithEmailMatch(contactId);
            }
        } finally {
            c.close();
        }
    }

    private interface PhoneLookupQuery {
        String TABLE = Tables.PHONE_LOOKUP + " phoneA"
                + " JOIN " + Tables.DATA + " dataA"
                + " ON (dataA." + Data._ID + "=phoneA." + PhoneLookupColumns.DATA_ID + ")"
                + " JOIN " + Tables.PHONE_LOOKUP + " phoneB"
                + " ON (phoneA." + PhoneLookupColumns.MIN_MATCH + "="
                        + "phoneB." + PhoneLookupColumns.MIN_MATCH + ")"
                + " JOIN " + Tables.DATA + " dataB"
                + " ON (dataB." + Data._ID + "=phoneB." + PhoneLookupColumns.DATA_ID + ")"
                + " JOIN " + Tables.RAW_CONTACTS
                + " ON (dataB." + Data.RAW_CONTACT_ID + " = "
                        + Tables.RAW_CONTACTS + "." + RawContacts._ID + ")";

        String SELECTION = "dataA." + Data.RAW_CONTACT_ID + "=?"
                + " AND PHONE_NUMBERS_EQUAL(dataA." + Phone.NUMBER + ", "
                        + "dataB." + Phone.NUMBER + ",?)"
                + " AND " + RawContactsColumns.AGGREGATION_NEEDED + "=0"
                + " AND " + RawContacts.CONTACT_ID + " IN " + Tables.DEFAULT_DIRECTORY;

        String[] COLUMNS = new String[] {
            RawContacts.CONTACT_ID
        };

        int CONTACT_ID = 0;
    }

    private void updateMatchScoresBasedOnPhoneMatches(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {
        mSelectionArgs2[0] = String.valueOf(rawContactId);
        mSelectionArgs2[1] = mDbHelper.getUseStrictPhoneNumberComparisonParameter();
        Cursor c = db.query(PhoneLookupQuery.TABLE, PhoneLookupQuery.COLUMNS,
                PhoneLookupQuery.SELECTION,
                mSelectionArgs2, null, null, null, SECONDARY_HIT_LIMIT_STRING);
        try {
            while (c.moveToNext()) {
                long contactId = c.getLong(PhoneLookupQuery.CONTACT_ID);
                matcher.updateScoreWithPhoneNumberMatch(contactId);
            }
        } finally {
            c.close();
        }
    }

    /**
     * Loads name lookup rows for approximate name matching and updates match scores based on that
     * data.
     */
    private void lookupApproximateNameMatches(SQLiteDatabase db, MatchCandidateList candidates,
            ContactMatcher matcher) {
        HashSet<String> firstLetters = new HashSet<String>();
        for (int i = 0; i < candidates.mCount; i++) {
            final NameMatchCandidate candidate = candidates.mList.get(i);
            if (candidate.mName.length() >= 2) {
                String firstLetter = candidate.mName.substring(0, 2);
                if (!firstLetters.contains(firstLetter)) {
                    firstLetters.add(firstLetter);
                    final String selection = "(" + NameLookupColumns.NORMALIZED_NAME + " GLOB '"
                            + firstLetter + "*') AND "
                            + "(" + NameLookupColumns.NAME_TYPE + " IN("
                                    + NameLookupType.NAME_COLLATION_KEY + ","
                                    + NameLookupType.EMAIL_BASED_NICKNAME + ","
                                    + NameLookupType.NICKNAME + ")) AND "
                            + RawContacts.CONTACT_ID + " IN " + Tables.DEFAULT_DIRECTORY;
                    matchAllCandidates(db, selection, candidates, matcher,
                            ContactMatcher.MATCHING_ALGORITHM_APPROXIMATE,
                            String.valueOf(FIRST_LETTER_SUGGESTION_HIT_LIMIT));
                }
            }
        }
    }

    private interface ContactNameLookupQuery {
        String TABLE = Tables.NAME_LOOKUP_JOIN_RAW_CONTACTS;

        String[] COLUMNS = new String[] {
                RawContacts.CONTACT_ID,
                NameLookupColumns.NORMALIZED_NAME,
                NameLookupColumns.NAME_TYPE
        };

        int CONTACT_ID = 0;
        int NORMALIZED_NAME = 1;
        int NAME_TYPE = 2;
    }

    /**
     * Loads all candidate rows from the name lookup table and updates match scores based
     * on that data.
     */
    private void matchAllCandidates(SQLiteDatabase db, String selection,
            MatchCandidateList candidates, ContactMatcher matcher, int algorithm, String limit) {
        final Cursor c = db.query(ContactNameLookupQuery.TABLE, ContactNameLookupQuery.COLUMNS,
                selection, null, null, null, null, limit);

        try {
            while (c.moveToNext()) {
                Long contactId = c.getLong(ContactNameLookupQuery.CONTACT_ID);
                String name = c.getString(ContactNameLookupQuery.NORMALIZED_NAME);
                int nameType = c.getInt(ContactNameLookupQuery.NAME_TYPE);

                // Note the N^2 complexity of the following fragment. This is not a huge concern
                // since the number of candidates is very small and in general secondary hits
                // in the absence of primary hits are rare.
                for (int i = 0; i < candidates.mCount; i++) {
                    NameMatchCandidate candidate = candidates.mList.get(i);
                    matcher.matchName(contactId, candidate.mLookupType, candidate.mName,
                            nameType, name, algorithm);
                }
            }
        } finally {
            c.close();
        }
    }

    private interface RawContactsQuery {
        String SQL_FORMAT =
                "SELECT "
                        + RawContactsColumns.CONCRETE_ID + ","
                        + RawContactsColumns.DISPLAY_NAME + ","
                        + RawContactsColumns.DISPLAY_NAME_SOURCE + ","
                        + AccountsColumns.CONCRETE_ACCOUNT_TYPE + ","
                        + AccountsColumns.CONCRETE_ACCOUNT_NAME + ","
                        + AccountsColumns.CONCRETE_DATA_SET + ","
                        + RawContacts.SOURCE_ID + ","
                        + RawContacts.CUSTOM_RINGTONE + ","
                        + RawContacts.SEND_TO_VOICEMAIL + ","
                        + RawContacts.LAST_TIME_CONTACTED + ","
                        + RawContacts.TIMES_CONTACTED + ","
                        + RawContacts.STARRED + ","
                        + RawContacts.PINNED + ","
                        + RawContacts.NAME_VERIFIED + ","
                        + DataColumns.CONCRETE_ID + ","
                        + DataColumns.CONCRETE_MIMETYPE_ID + ","
                        + Data.IS_SUPER_PRIMARY + ","
                        + Photo.PHOTO_FILE_ID +
                " FROM " + Tables.RAW_CONTACTS +
                " JOIN " + Tables.ACCOUNTS + " ON ("
                    + AccountsColumns.CONCRETE_ID + "=" + RawContactsColumns.CONCRETE_ACCOUNT_ID
                    + ")" +
                " LEFT OUTER JOIN " + Tables.DATA +
                " ON (" + DataColumns.CONCRETE_RAW_CONTACT_ID + "=" + RawContactsColumns.CONCRETE_ID
                        + " AND ((" + DataColumns.MIMETYPE_ID + "=%d"
                                + " AND " + Photo.PHOTO + " NOT NULL)"
                        + " OR (" + DataColumns.MIMETYPE_ID + "=%d"
                                + " AND " + Phone.NUMBER + " NOT NULL)))";

        String SQL_FORMAT_BY_RAW_CONTACT_ID = SQL_FORMAT +
                " WHERE " + RawContactsColumns.CONCRETE_ID + "=?";

        String SQL_FORMAT_BY_CONTACT_ID = SQL_FORMAT +
                " WHERE " + RawContacts.CONTACT_ID + "=?"
                + " AND " + RawContacts.DELETED + "=0";

        int RAW_CONTACT_ID = 0;
        int DISPLAY_NAME = 1;
        int DISPLAY_NAME_SOURCE = 2;
        int ACCOUNT_TYPE = 3;
        int ACCOUNT_NAME = 4;
        int DATA_SET = 5;
        int SOURCE_ID = 6;
        int CUSTOM_RINGTONE = 7;
        int SEND_TO_VOICEMAIL = 8;
        int LAST_TIME_CONTACTED = 9;
        int TIMES_CONTACTED = 10;
        int STARRED = 11;
        int PINNED = 12;
        int NAME_VERIFIED = 13;
        int DATA_ID = 14;
        int MIMETYPE_ID = 15;
        int IS_SUPER_PRIMARY = 16;
        int PHOTO_FILE_ID = 17;
    }

    private interface ContactReplaceSqlStatement {
        String UPDATE_SQL =
                "UPDATE " + Tables.CONTACTS +
                " SET "
                        + Contacts.NAME_RAW_CONTACT_ID + "=?, "
                        + Contacts.PHOTO_ID + "=?, "
                        + Contacts.PHOTO_FILE_ID + "=?, "
                        + Contacts.SEND_TO_VOICEMAIL + "=?, "
                        + Contacts.CUSTOM_RINGTONE + "=?, "
                        + Contacts.LAST_TIME_CONTACTED + "=?, "
                        + Contacts.TIMES_CONTACTED + "=?, "
                        + Contacts.STARRED + "=?, "
                        + Contacts.PINNED + "=?, "
                        + Contacts.HAS_PHONE_NUMBER + "=?, "
                        + Contacts.LOOKUP_KEY + "=?, "
                        + Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + "=? " +
                " WHERE " + Contacts._ID + "=?";

        String INSERT_SQL =
                "INSERT INTO " + Tables.CONTACTS + " ("
                        + Contacts.NAME_RAW_CONTACT_ID + ", "
                        + Contacts.PHOTO_ID + ", "
                        + Contacts.PHOTO_FILE_ID + ", "
                        + Contacts.SEND_TO_VOICEMAIL + ", "
                        + Contacts.CUSTOM_RINGTONE + ", "
                        + Contacts.LAST_TIME_CONTACTED + ", "
                        + Contacts.TIMES_CONTACTED + ", "
                        + Contacts.STARRED + ", "
                        + Contacts.PINNED + ", "
                        + Contacts.HAS_PHONE_NUMBER + ", "
                        + Contacts.LOOKUP_KEY + ", "
                        + Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
                        + ") " +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

        int NAME_RAW_CONTACT_ID = 1;
        int PHOTO_ID = 2;
        int PHOTO_FILE_ID = 3;
        int SEND_TO_VOICEMAIL = 4;
        int CUSTOM_RINGTONE = 5;
        int LAST_TIME_CONTACTED = 6;
        int TIMES_CONTACTED = 7;
        int STARRED = 8;
        int PINNED = 9;
        int HAS_PHONE_NUMBER = 10;
        int LOOKUP_KEY = 11;
        int CONTACT_LAST_UPDATED_TIMESTAMP = 12;
        int CONTACT_ID = 13;
    }

    /**
     * Computes aggregate-level data for the specified aggregate contact ID.
     */
    private void computeAggregateData(SQLiteDatabase db, long contactId,
            SQLiteStatement statement) {
        mSelectionArgs1[0] = String.valueOf(contactId);
        computeAggregateData(db, mRawContactsQueryByContactId, mSelectionArgs1, statement);
    }

    /**
     * Indicates whether the given photo entry and priority gives this photo a higher overall
     * priority than the current best photo entry and priority.
     */
    private boolean hasHigherPhotoPriority(PhotoEntry photoEntry, int priority,
            PhotoEntry bestPhotoEntry, int bestPriority) {
        int photoComparison = photoEntry.compareTo(bestPhotoEntry);
        return photoComparison < 0 || photoComparison == 0 && priority > bestPriority;
    }

    /**
     * Computes aggregate-level data from constituent raw contacts.
     */
    private void computeAggregateData(final SQLiteDatabase db, String sql, String[] sqlArgs,
            SQLiteStatement statement) {
        long currentRawContactId = -1;
        long bestPhotoId = -1;
        long bestPhotoFileId = 0;
        PhotoEntry bestPhotoEntry = null;
        boolean foundSuperPrimaryPhoto = false;
        int photoPriority = -1;
        int totalRowCount = 0;
        int contactSendToVoicemail = 0;
        String contactCustomRingtone = null;
        long contactLastTimeContacted = 0;
        int contactTimesContacted = 0;
        int contactStarred = 0;
        int contactPinned = PinnedPositions.UNPINNED;
        int hasPhoneNumber = 0;
        StringBuilder lookupKey = new StringBuilder();

        mDisplayNameCandidate.clear();

        Cursor c = db.rawQuery(sql, sqlArgs);
        try {
            while (c.moveToNext()) {
                long rawContactId = c.getLong(RawContactsQuery.RAW_CONTACT_ID);
                if (rawContactId != currentRawContactId) {
                    currentRawContactId = rawContactId;
                    totalRowCount++;

                    // Assemble sub-account.
                    String accountType = c.getString(RawContactsQuery.ACCOUNT_TYPE);
                    String dataSet = c.getString(RawContactsQuery.DATA_SET);
                    String accountWithDataSet = (!TextUtils.isEmpty(dataSet))
                            ? accountType + "/" + dataSet
                            : accountType;

                    // Display name
                    String displayName = c.getString(RawContactsQuery.DISPLAY_NAME);
                    int displayNameSource = c.getInt(RawContactsQuery.DISPLAY_NAME_SOURCE);
                    int nameVerified = c.getInt(RawContactsQuery.NAME_VERIFIED);
                    processDisplayNameCandidate(rawContactId, displayName, displayNameSource,
                            mContactsProvider.isWritableAccountWithDataSet(accountWithDataSet),
                            nameVerified != 0);

                    // Contact options
                    if (!c.isNull(RawContactsQuery.SEND_TO_VOICEMAIL)) {
                        boolean sendToVoicemail =
                                (c.getInt(RawContactsQuery.SEND_TO_VOICEMAIL) != 0);
                        if (sendToVoicemail) {
                            contactSendToVoicemail++;
                        }
                    }

                    if (contactCustomRingtone == null
                            && !c.isNull(RawContactsQuery.CUSTOM_RINGTONE)) {
                        contactCustomRingtone = c.getString(RawContactsQuery.CUSTOM_RINGTONE);
                    }

                    long lastTimeContacted = c.getLong(RawContactsQuery.LAST_TIME_CONTACTED);
                    if (lastTimeContacted > contactLastTimeContacted) {
                        contactLastTimeContacted = lastTimeContacted;
                    }

                    int timesContacted = c.getInt(RawContactsQuery.TIMES_CONTACTED);
                    if (timesContacted > contactTimesContacted) {
                        contactTimesContacted = timesContacted;
                    }

                    if (c.getInt(RawContactsQuery.STARRED) != 0) {
                        contactStarred = 1;
                    }

                    // contactPinned should be the lowest value of its constituent raw contacts,
                    // excluding negative integers
                    final int rawContactPinned = c.getInt(RawContactsQuery.PINNED);
                    if (rawContactPinned >= 0) {
                        contactPinned = Math.min(contactPinned, rawContactPinned);
                    }

                    appendLookupKey(
                            lookupKey,
                            accountWithDataSet,
                            c.getString(RawContactsQuery.ACCOUNT_NAME),
                            rawContactId,
                            c.getString(RawContactsQuery.SOURCE_ID),
                            displayName);
                }

                if (!c.isNull(RawContactsQuery.DATA_ID)) {
                    long dataId = c.getLong(RawContactsQuery.DATA_ID);
                    long photoFileId = c.getLong(RawContactsQuery.PHOTO_FILE_ID);
                    int mimetypeId = c.getInt(RawContactsQuery.MIMETYPE_ID);
                    boolean superPrimary = c.getInt(RawContactsQuery.IS_SUPER_PRIMARY) != 0;
                    if (mimetypeId == mMimeTypeIdPhoto) {
                        if (!foundSuperPrimaryPhoto) {
                            // Lookup the metadata for the photo, if available.  Note that data set
                            // does not come into play here, since accounts are looked up in the
                            // account manager in the priority resolver.
                            PhotoEntry photoEntry = getPhotoMetadata(db, photoFileId);
                            String accountType = c.getString(RawContactsQuery.ACCOUNT_TYPE);
                            int priority = mPhotoPriorityResolver.getPhotoPriority(accountType);
                            if (superPrimary || hasHigherPhotoPriority(
                                    photoEntry, priority, bestPhotoEntry, photoPriority)) {
                                bestPhotoEntry = photoEntry;
                                photoPriority = priority;
                                bestPhotoId = dataId;
                                bestPhotoFileId = photoFileId;
                                foundSuperPrimaryPhoto |= superPrimary;
                            }
                        }
                    } else if (mimetypeId == mMimeTypeIdPhone) {
                        hasPhoneNumber = 1;
                    }
                }
            }
        } finally {
            c.close();
        }

        statement.bindLong(ContactReplaceSqlStatement.NAME_RAW_CONTACT_ID,
                mDisplayNameCandidate.rawContactId);

        if (bestPhotoId != -1) {
            statement.bindLong(ContactReplaceSqlStatement.PHOTO_ID, bestPhotoId);
        } else {
            statement.bindNull(ContactReplaceSqlStatement.PHOTO_ID);
        }

        if (bestPhotoFileId != 0) {
            statement.bindLong(ContactReplaceSqlStatement.PHOTO_FILE_ID, bestPhotoFileId);
        } else {
            statement.bindNull(ContactReplaceSqlStatement.PHOTO_FILE_ID);
        }

        statement.bindLong(ContactReplaceSqlStatement.SEND_TO_VOICEMAIL,
                totalRowCount == contactSendToVoicemail ? 1 : 0);
        DatabaseUtils.bindObjectToProgram(statement, ContactReplaceSqlStatement.CUSTOM_RINGTONE,
                contactCustomRingtone);
        statement.bindLong(ContactReplaceSqlStatement.LAST_TIME_CONTACTED,
                contactLastTimeContacted);
        statement.bindLong(ContactReplaceSqlStatement.TIMES_CONTACTED,
                contactTimesContacted);
        statement.bindLong(ContactReplaceSqlStatement.STARRED,
                contactStarred);
        statement.bindLong(ContactReplaceSqlStatement.PINNED,
                contactPinned);
        statement.bindLong(ContactReplaceSqlStatement.HAS_PHONE_NUMBER,
                hasPhoneNumber);
        statement.bindString(ContactReplaceSqlStatement.LOOKUP_KEY,
                Uri.encode(lookupKey.toString()));
        statement.bindLong(ContactReplaceSqlStatement.CONTACT_LAST_UPDATED_TIMESTAMP,
                Clock.getInstance().currentTimeMillis());
    }

    /**
     * Builds a lookup key using the given data.
     */
    protected void appendLookupKey(StringBuilder sb, String accountTypeWithDataSet,
            String accountName, long rawContactId, String sourceId, String displayName) {
        ContactLookupKey.appendToLookupKey(sb, accountTypeWithDataSet, accountName, rawContactId,
                sourceId, displayName);
    }

    /**
     * Uses the supplied values to determine if they represent a "better" display name
     * for the aggregate contact currently evaluated.  If so, it updates
     * {@link #mDisplayNameCandidate} with the new values.
     */
    private void processDisplayNameCandidate(long rawContactId, String displayName,
            int displayNameSource, boolean writableAccount, boolean verified) {

        boolean replace = false;
        if (mDisplayNameCandidate.rawContactId == -1) {
            // No previous values available
            replace = true;
        } else if (!TextUtils.isEmpty(displayName)) {
            if (!mDisplayNameCandidate.verified && verified) {
                // A verified name is better than any other name
                replace = true;
            } else if (mDisplayNameCandidate.verified == verified) {
                if (mDisplayNameCandidate.displayNameSource < displayNameSource) {
                    // New values come from an superior source, e.g. structured name vs phone number
                    replace = true;
                } else if (mDisplayNameCandidate.displayNameSource == displayNameSource) {
                    if (!mDisplayNameCandidate.writableAccount && writableAccount) {
                        replace = true;
                    } else if (mDisplayNameCandidate.writableAccount == writableAccount) {
                        if (NameNormalizer.compareComplexity(displayName,
                                mDisplayNameCandidate.displayName) > 0) {
                            // New name is more complex than the previously found one
                            replace = true;
                        }
                    }
                }
            }
        }

        if (replace) {
            mDisplayNameCandidate.rawContactId = rawContactId;
            mDisplayNameCandidate.displayName = displayName;
            mDisplayNameCandidate.displayNameSource = displayNameSource;
            mDisplayNameCandidate.verified = verified;
            mDisplayNameCandidate.writableAccount = writableAccount;
        }
    }

    private interface PhotoIdQuery {
        final String[] COLUMNS = new String[] {
            AccountsColumns.CONCRETE_ACCOUNT_TYPE,
            DataColumns.CONCRETE_ID,
            Data.IS_SUPER_PRIMARY,
            Photo.PHOTO_FILE_ID,
        };

        int ACCOUNT_TYPE = 0;
        int DATA_ID = 1;
        int IS_SUPER_PRIMARY = 2;
        int PHOTO_FILE_ID = 3;
    }

    public void updatePhotoId(SQLiteDatabase db, long rawContactId) {

        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }

        long bestPhotoId = -1;
        long bestPhotoFileId = 0;
        int photoPriority = -1;

        long photoMimeType = mDbHelper.getMimeTypeId(Photo.CONTENT_ITEM_TYPE);

        String tables = Tables.RAW_CONTACTS
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                    + AccountsColumns.CONCRETE_ID + "=" + RawContactsColumns.CONCRETE_ACCOUNT_ID
                    + ")"
                + " JOIN " + Tables.DATA + " ON("
                + DataColumns.CONCRETE_RAW_CONTACT_ID + "=" + RawContactsColumns.CONCRETE_ID
                + " AND (" + DataColumns.MIMETYPE_ID + "=" + photoMimeType + " AND "
                        + Photo.PHOTO + " NOT NULL))";

        mSelectionArgs1[0] = String.valueOf(contactId);
        final Cursor c = db.query(tables, PhotoIdQuery.COLUMNS,
                RawContacts.CONTACT_ID + "=?", mSelectionArgs1, null, null, null);
        try {
            PhotoEntry bestPhotoEntry = null;
            while (c.moveToNext()) {
                long dataId = c.getLong(PhotoIdQuery.DATA_ID);
                long photoFileId = c.getLong(PhotoIdQuery.PHOTO_FILE_ID);
                boolean superPrimary = c.getInt(PhotoIdQuery.IS_SUPER_PRIMARY) != 0;
                PhotoEntry photoEntry = getPhotoMetadata(db, photoFileId);

                // Note that data set does not come into play here, since accounts are looked up in
                // the account manager in the priority resolver.
                String accountType = c.getString(PhotoIdQuery.ACCOUNT_TYPE);
                int priority = mPhotoPriorityResolver.getPhotoPriority(accountType);
                if (superPrimary || hasHigherPhotoPriority(
                        photoEntry, priority, bestPhotoEntry, photoPriority)) {
                    bestPhotoEntry = photoEntry;
                    photoPriority = priority;
                    bestPhotoId = dataId;
                    bestPhotoFileId = photoFileId;
                    if (superPrimary) {
                        break;
                    }
                }
            }
        } finally {
            c.close();
        }

        if (bestPhotoId == -1) {
            mPhotoIdUpdate.bindNull(1);
        } else {
            mPhotoIdUpdate.bindLong(1, bestPhotoId);
        }

        if (bestPhotoFileId == 0) {
            mPhotoIdUpdate.bindNull(2);
        } else {
            mPhotoIdUpdate.bindLong(2, bestPhotoFileId);
        }

        mPhotoIdUpdate.bindLong(3, contactId);
        mPhotoIdUpdate.execute();
    }

    private interface PhotoFileQuery {
        final String[] COLUMNS = new String[] {
                PhotoFiles.HEIGHT,
                PhotoFiles.WIDTH,
                PhotoFiles.FILESIZE
        };

        int HEIGHT = 0;
        int WIDTH = 1;
        int FILESIZE = 2;
    }

    private class PhotoEntry implements Comparable<PhotoEntry> {
        // Pixel count (width * height) for the image.
        final int pixelCount;

        // File size (in bytes) of the image.  Not populated if the image is a thumbnail.
        final int fileSize;

        private PhotoEntry(int pixelCount, int fileSize) {
            this.pixelCount = pixelCount;
            this.fileSize = fileSize;
        }

        @Override
        public int compareTo(PhotoEntry pe) {
            if (pe == null) {
                return -1;
            }
            if (pixelCount == pe.pixelCount) {
                return pe.fileSize - fileSize;
            } else {
                return pe.pixelCount - pixelCount;
            }
        }
    }

    private PhotoEntry getPhotoMetadata(SQLiteDatabase db, long photoFileId) {
        if (photoFileId == 0) {
            // Assume standard thumbnail size.  Don't bother getting a file size for priority;
            // we should fall back to photo priority resolver if all we have are thumbnails.
            int thumbDim = mContactsProvider.getMaxThumbnailDim();
            return new PhotoEntry(thumbDim * thumbDim, 0);
        } else {
            Cursor c = db.query(Tables.PHOTO_FILES, PhotoFileQuery.COLUMNS, PhotoFiles._ID + "=?",
                    new String[]{String.valueOf(photoFileId)}, null, null, null);
            try {
                if (c.getCount() == 1) {
                    c.moveToFirst();
                    int pixelCount =
                            c.getInt(PhotoFileQuery.HEIGHT) * c.getInt(PhotoFileQuery.WIDTH);
                    return new PhotoEntry(pixelCount, c.getInt(PhotoFileQuery.FILESIZE));
                }
            } finally {
                c.close();
            }
        }
        return new PhotoEntry(0, 0);
    }

    private interface DisplayNameQuery {
        String[] COLUMNS = new String[] {
            RawContacts._ID,
            RawContactsColumns.DISPLAY_NAME,
            RawContactsColumns.DISPLAY_NAME_SOURCE,
            RawContacts.NAME_VERIFIED,
            RawContacts.SOURCE_ID,
            RawContacts.ACCOUNT_TYPE_AND_DATA_SET,
        };

        int _ID = 0;
        int DISPLAY_NAME = 1;
        int DISPLAY_NAME_SOURCE = 2;
        int NAME_VERIFIED = 3;
        int SOURCE_ID = 4;
        int ACCOUNT_TYPE_AND_DATA_SET = 5;
    }

    public void updateDisplayNameForRawContact(SQLiteDatabase db, long rawContactId) {
        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }

        updateDisplayNameForContact(db, contactId);
    }

    public void updateDisplayNameForContact(SQLiteDatabase db, long contactId) {
        boolean lookupKeyUpdateNeeded = false;

        mDisplayNameCandidate.clear();

        mSelectionArgs1[0] = String.valueOf(contactId);
        final Cursor c = db.query(Views.RAW_CONTACTS, DisplayNameQuery.COLUMNS,
                RawContacts.CONTACT_ID + "=?", mSelectionArgs1, null, null, null);
        try {
            while (c.moveToNext()) {
                long rawContactId = c.getLong(DisplayNameQuery._ID);
                String displayName = c.getString(DisplayNameQuery.DISPLAY_NAME);
                int displayNameSource = c.getInt(DisplayNameQuery.DISPLAY_NAME_SOURCE);
                int nameVerified = c.getInt(DisplayNameQuery.NAME_VERIFIED);
                String accountTypeAndDataSet = c.getString(
                        DisplayNameQuery.ACCOUNT_TYPE_AND_DATA_SET);
                processDisplayNameCandidate(rawContactId, displayName, displayNameSource,
                        mContactsProvider.isWritableAccountWithDataSet(accountTypeAndDataSet),
                        nameVerified != 0);

                // If the raw contact has no source id, the lookup key is based on the display
                // name, so the lookup key needs to be updated.
                lookupKeyUpdateNeeded |= c.isNull(DisplayNameQuery.SOURCE_ID);
            }
        } finally {
            c.close();
        }

        if (mDisplayNameCandidate.rawContactId != -1) {
            mDisplayNameUpdate.bindLong(1, mDisplayNameCandidate.rawContactId);
            mDisplayNameUpdate.bindLong(2, contactId);
            mDisplayNameUpdate.execute();
        }

        if (lookupKeyUpdateNeeded) {
            updateLookupKeyForContact(db, contactId);
        }
    }


    /**
     * Updates the {@link Contacts#HAS_PHONE_NUMBER} flag for the aggregate contact containing the
     * specified raw contact.
     */
    public void updateHasPhoneNumber(SQLiteDatabase db, long rawContactId) {

        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }

        final SQLiteStatement hasPhoneNumberUpdate = db.compileStatement(
                "UPDATE " + Tables.CONTACTS +
                " SET " + Contacts.HAS_PHONE_NUMBER + "="
                        + "(SELECT (CASE WHEN COUNT(*)=0 THEN 0 ELSE 1 END)"
                        + " FROM " + Tables.DATA_JOIN_RAW_CONTACTS
                        + " WHERE " + DataColumns.MIMETYPE_ID + "=?"
                                + " AND " + Phone.NUMBER + " NOT NULL"
                                + " AND " + RawContacts.CONTACT_ID + "=?)" +
                " WHERE " + Contacts._ID + "=?");
        try {
            hasPhoneNumberUpdate.bindLong(1, mDbHelper.getMimeTypeId(Phone.CONTENT_ITEM_TYPE));
            hasPhoneNumberUpdate.bindLong(2, contactId);
            hasPhoneNumberUpdate.bindLong(3, contactId);
            hasPhoneNumberUpdate.execute();
        } finally {
            hasPhoneNumberUpdate.close();
        }
    }

    private interface LookupKeyQuery {
        String TABLE = Views.RAW_CONTACTS;
        String[] COLUMNS = new String[] {
            RawContacts._ID,
            RawContactsColumns.DISPLAY_NAME,
            RawContacts.ACCOUNT_TYPE_AND_DATA_SET,
            RawContacts.ACCOUNT_NAME,
            RawContacts.SOURCE_ID,
        };

        int ID = 0;
        int DISPLAY_NAME = 1;
        int ACCOUNT_TYPE_AND_DATA_SET = 2;
        int ACCOUNT_NAME = 3;
        int SOURCE_ID = 4;
    }

    public void updateLookupKeyForRawContact(SQLiteDatabase db, long rawContactId) {
        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }

        updateLookupKeyForContact(db, contactId);
    }

    private void updateLookupKeyForContact(SQLiteDatabase db, long contactId) {
        String lookupKey = computeLookupKeyForContact(db, contactId);

        if (lookupKey == null) {
            mLookupKeyUpdate.bindNull(1);
        } else {
            mLookupKeyUpdate.bindString(1, Uri.encode(lookupKey));
        }
        mLookupKeyUpdate.bindLong(2, contactId);

        mLookupKeyUpdate.execute();
    }

    protected String computeLookupKeyForContact(SQLiteDatabase db, long contactId) {
        StringBuilder sb = new StringBuilder();
        mSelectionArgs1[0] = String.valueOf(contactId);
        final Cursor c = db.query(LookupKeyQuery.TABLE, LookupKeyQuery.COLUMNS,
                RawContacts.CONTACT_ID + "=?", mSelectionArgs1, null, null, RawContacts._ID);
        try {
            while (c.moveToNext()) {
                ContactLookupKey.appendToLookupKey(sb,
                        c.getString(LookupKeyQuery.ACCOUNT_TYPE_AND_DATA_SET),
                        c.getString(LookupKeyQuery.ACCOUNT_NAME),
                        c.getLong(LookupKeyQuery.ID),
                        c.getString(LookupKeyQuery.SOURCE_ID),
                        c.getString(LookupKeyQuery.DISPLAY_NAME));
            }
        } finally {
            c.close();
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Execute {@link SQLiteStatement} that will update the
     * {@link Contacts#STARRED} flag for the given {@link RawContacts#_ID}.
     */
    public void updateStarred(long rawContactId) {
        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }

        mStarredUpdate.bindLong(1, contactId);
        mStarredUpdate.execute();
    }

    /**
     * Execute {@link SQLiteStatement} that will update the
     * {@link Contacts#PINNED} flag for the given {@link RawContacts#_ID}.
     */
    public void updatePinned(long rawContactId) {
        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }
        mPinnedUpdate.bindLong(1, contactId);
        mPinnedUpdate.execute();
    }

    /**
     * Finds matching contacts and returns a cursor on those.
     */
    public Cursor queryAggregationSuggestions(SQLiteQueryBuilder qb,
            String[] projection, long contactId, int maxSuggestions, String filter,
            ArrayList<AggregationSuggestionParameter> parameters) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            List<MatchScore> bestMatches = findMatchingContacts(db, contactId, parameters);
            return queryMatchingContacts(qb, db, projection, bestMatches, maxSuggestions, filter);
        } finally {
            db.endTransaction();
        }
    }

    private interface ContactIdQuery {
        String[] COLUMNS = new String[] {
            Contacts._ID
        };

        int _ID = 0;
    }

    /**
     * Loads contacts with specified IDs and returns them in the order of IDs in the
     * supplied list.
     */
    private Cursor queryMatchingContacts(SQLiteQueryBuilder qb, SQLiteDatabase db,
            String[] projection, List<MatchScore> bestMatches, int maxSuggestions, String filter) {
        StringBuilder sb = new StringBuilder();
        sb.append(Contacts._ID);
        sb.append(" IN (");
        for (int i = 0; i < bestMatches.size(); i++) {
            MatchScore matchScore = bestMatches.get(i);
            if (i != 0) {
                sb.append(",");
            }
            sb.append(matchScore.getContactId());
        }
        sb.append(")");

        if (!TextUtils.isEmpty(filter)) {
            sb.append(" AND " + Contacts._ID + " IN ");
            mContactsProvider.appendContactFilterAsNestedQuery(sb, filter);
        }

        // Run a query and find ids of best matching contacts satisfying the filter (if any)
        HashSet<Long> foundIds = new HashSet<Long>();
        Cursor cursor = db.query(qb.getTables(), ContactIdQuery.COLUMNS, sb.toString(),
                null, null, null, null);
        try {
            while(cursor.moveToNext()) {
                foundIds.add(cursor.getLong(ContactIdQuery._ID));
            }
        } finally {
            cursor.close();
        }

        // Exclude all contacts that did not match the filter
        Iterator<MatchScore> iter = bestMatches.iterator();
        while (iter.hasNext()) {
            long id = iter.next().getContactId();
            if (!foundIds.contains(id)) {
                iter.remove();
            }
        }

        // Limit the number of returned suggestions
        final List<MatchScore> limitedMatches;
        if (bestMatches.size() > maxSuggestions) {
            limitedMatches = bestMatches.subList(0, maxSuggestions);
        } else {
            limitedMatches = bestMatches;
        }

        // Build an in-clause with the remaining contact IDs
        sb.setLength(0);
        sb.append(Contacts._ID);
        sb.append(" IN (");
        for (int i = 0; i < limitedMatches.size(); i++) {
            MatchScore matchScore = limitedMatches.get(i);
            if (i != 0) {
                sb.append(",");
            }
            sb.append(matchScore.getContactId());
        }
        sb.append(")");

        // Run the final query with the required projection and contact IDs found by the first query
        cursor = qb.query(db, projection, sb.toString(), null, null, null, Contacts._ID);

        // Build a sorted list of discovered IDs
        ArrayList<Long> sortedContactIds = new ArrayList<Long>(limitedMatches.size());
        for (MatchScore matchScore : limitedMatches) {
            sortedContactIds.add(matchScore.getContactId());
        }

        Collections.sort(sortedContactIds);

        // Map cursor indexes according to the descending order of match scores
        int[] positionMap = new int[limitedMatches.size()];
        for (int i = 0; i < positionMap.length; i++) {
            long id = limitedMatches.get(i).getContactId();
            positionMap[i] = sortedContactIds.indexOf(id);
        }

        return new ReorderingCursorWrapper(cursor, positionMap);
    }

    /**
     * Finds contacts with data matches and returns a list of {@link MatchScore}'s in the
     * descending order of match score.
     * @param parameters
     */
    private List<MatchScore> findMatchingContacts(final SQLiteDatabase db, long contactId,
            ArrayList<AggregationSuggestionParameter> parameters) {

        MatchCandidateList candidates = new MatchCandidateList();
        ContactMatcher matcher = new ContactMatcher();

        // Don't aggregate a contact with itself
        matcher.keepOut(contactId);

        if (parameters == null || parameters.size() == 0) {
            final Cursor c = db.query(RawContactIdQuery.TABLE, RawContactIdQuery.COLUMNS,
                    RawContacts.CONTACT_ID + "=" + contactId, null, null, null, null);
            try {
                while (c.moveToNext()) {
                    long rawContactId = c.getLong(RawContactIdQuery.RAW_CONTACT_ID);
                    updateMatchScoresForSuggestionsBasedOnDataMatches(db, rawContactId, candidates,
                            matcher);
                }
            } finally {
                c.close();
            }
        } else {
            updateMatchScoresForSuggestionsBasedOnDataMatches(db, candidates,
                    matcher, parameters);
        }

        return matcher.pickBestMatches(ContactMatcher.SCORE_THRESHOLD_SUGGEST);
    }

    /**
     * Computes scores for contacts that have matching data rows.
     */
    private void updateMatchScoresForSuggestionsBasedOnDataMatches(SQLiteDatabase db,
            long rawContactId, MatchCandidateList candidates, ContactMatcher matcher) {

        updateMatchScoresBasedOnIdentityMatch(db, rawContactId, matcher);
        updateMatchScoresBasedOnNameMatches(db, rawContactId, matcher);
        updateMatchScoresBasedOnEmailMatches(db, rawContactId, matcher);
        updateMatchScoresBasedOnPhoneMatches(db, rawContactId, matcher);
        loadNameMatchCandidates(db, rawContactId, candidates, false);
        lookupApproximateNameMatches(db, candidates, matcher);
    }

    private void updateMatchScoresForSuggestionsBasedOnDataMatches(SQLiteDatabase db,
            MatchCandidateList candidates, ContactMatcher matcher,
            ArrayList<AggregationSuggestionParameter> parameters) {
        for (AggregationSuggestionParameter parameter : parameters) {
            if (AggregationSuggestions.PARAMETER_MATCH_NAME.equals(parameter.kind)) {
                updateMatchScoresBasedOnNameMatches(db, parameter.value, candidates, matcher);
            }

            // TODO: add support for other parameter kinds
        }
    }
}
