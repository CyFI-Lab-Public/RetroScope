/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License
 */
package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;

import com.android.providers.contacts.ContactsDatabaseHelper.DataColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.MimetypesColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.PresenceColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.aggregation.ContactAggregator;

/**
 * Handles inserts and update for a specific Data type.
 */
public abstract class DataRowHandler {

    public interface DataDeleteQuery {
        public static final String TABLE = Tables.DATA_JOIN_MIMETYPES;

        public static final String[] CONCRETE_COLUMNS = new String[] {
            DataColumns.CONCRETE_ID,
            MimetypesColumns.MIMETYPE,
            Data.RAW_CONTACT_ID,
            Data.IS_PRIMARY,
            Data.DATA1,
        };

        public static final String[] COLUMNS = new String[] {
            Data._ID,
            MimetypesColumns.MIMETYPE,
            Data.RAW_CONTACT_ID,
            Data.IS_PRIMARY,
            Data.DATA1,
        };

        public static final int _ID = 0;
        public static final int MIMETYPE = 1;
        public static final int RAW_CONTACT_ID = 2;
        public static final int IS_PRIMARY = 3;
        public static final int DATA1 = 4;
    }

    public interface DataUpdateQuery {
        String[] COLUMNS = { Data._ID, Data.RAW_CONTACT_ID, Data.MIMETYPE };

        int _ID = 0;
        int RAW_CONTACT_ID = 1;
        int MIMETYPE = 2;
    }

    protected final Context mContext;
    protected final ContactsDatabaseHelper mDbHelper;
    protected final ContactAggregator mContactAggregator;
    protected String[] mSelectionArgs1 = new String[1];
    protected final String mMimetype;
    protected long mMimetypeId;

    @SuppressWarnings("all")
    public DataRowHandler(Context context, ContactsDatabaseHelper dbHelper,
            ContactAggregator aggregator, String mimetype) {
        mContext = context;
        mDbHelper = dbHelper;
        mContactAggregator = aggregator;
        mMimetype = mimetype;

        // To ensure the data column position. This is dead code if properly configured.
        if (StructuredName.DISPLAY_NAME != Data.DATA1 || Nickname.NAME != Data.DATA1
                || Organization.COMPANY != Data.DATA1 || Phone.NUMBER != Data.DATA1
                || Email.DATA != Data.DATA1) {
            throw new AssertionError("Some of ContactsContract.CommonDataKinds class primary"
                    + " data is not in DATA1 column");
        }
    }

    protected long getMimeTypeId() {
        if (mMimetypeId == 0) {
            mMimetypeId = mDbHelper.getMimeTypeId(mMimetype);
        }
        return mMimetypeId;
    }

    /**
     * Inserts a row into the {@link Data} table.
     */
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId,
            ContentValues values) {
        final long dataId = db.insert(Tables.DATA, null, values);

        final Integer primary = values.getAsInteger(Data.IS_PRIMARY);
        final Integer superPrimary = values.getAsInteger(Data.IS_SUPER_PRIMARY);
        if ((primary != null && primary != 0) || (superPrimary != null && superPrimary != 0)) {
            final long mimeTypeId = getMimeTypeId();
            mDbHelper.setIsPrimary(rawContactId, dataId, mimeTypeId);

            // We also have to make sure that no other data item on this raw_contact is
            // configured super primary
            if (superPrimary != null) {
                if (superPrimary != 0) {
                    mDbHelper.setIsSuperPrimary(rawContactId, dataId, mimeTypeId);
                } else {
                    mDbHelper.clearSuperPrimary(rawContactId, mimeTypeId);
                }
            } else {
                // if there is already another data item configured as super-primary,
                // take over the flag (which will automatically remove it from the other item)
                if (mDbHelper.rawContactHasSuperPrimary(rawContactId, mimeTypeId)) {
                    mDbHelper.setIsSuperPrimary(rawContactId, dataId, mimeTypeId);
                }
            }
        }

        if (containsSearchableColumns(values)) {
            txContext.invalidateSearchIndexForRawContact(rawContactId);
        }

        return dataId;
    }

    /**
     * Validates data and updates a {@link Data} row using the cursor, which contains
     * the current data.
     *
     * @return true if update changed something
     */
    public boolean update(SQLiteDatabase db, TransactionContext txContext,
            ContentValues values, Cursor c, boolean callerIsSyncAdapter) {
        long dataId = c.getLong(DataUpdateQuery._ID);
        long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);

        handlePrimaryAndSuperPrimary(values, dataId, rawContactId);

        if (values.size() > 0) {
            mSelectionArgs1[0] = String.valueOf(dataId);
            db.update(Tables.DATA, values, Data._ID + " =?", mSelectionArgs1);
        }

        if (containsSearchableColumns(values)) {
            txContext.invalidateSearchIndexForRawContact(rawContactId);
        }

        txContext.markRawContactDirtyAndChanged(rawContactId, callerIsSyncAdapter);

        return true;
    }

    public boolean hasSearchableData() {
        return false;
    }

    public boolean containsSearchableColumns(ContentValues values) {
        return false;
    }

    public void appendSearchableData(SearchIndexManager.IndexBuilder builder) {
    }

    /**
     * Ensures that all super-primary and primary flags of this raw_contact are
     * configured correctly
     */
    private void handlePrimaryAndSuperPrimary(ContentValues values, long dataId,
            long rawContactId) {
        final boolean hasPrimary = values.getAsInteger(Data.IS_PRIMARY) != null;
        final boolean hasSuperPrimary = values.getAsInteger(Data.IS_SUPER_PRIMARY) != null;

        // Nothing to do? Bail out early
        if (!hasPrimary && !hasSuperPrimary) return;

        final long mimeTypeId = getMimeTypeId();

        // Check if we want to clear values
        final boolean clearPrimary = hasPrimary &&
                values.getAsInteger(Data.IS_PRIMARY) == 0;
        final boolean clearSuperPrimary = hasSuperPrimary &&
                values.getAsInteger(Data.IS_SUPER_PRIMARY) == 0;

        if (clearPrimary || clearSuperPrimary) {
            // Test whether these values are currently set
            mSelectionArgs1[0] = String.valueOf(dataId);
            final String[] cols = new String[] { Data.IS_PRIMARY, Data.IS_SUPER_PRIMARY };
            final Cursor c = mDbHelper.getReadableDatabase().query(Tables.DATA,
                    cols, Data._ID + "=?", mSelectionArgs1, null, null, null);
            try {
                if (c.moveToFirst()) {
                    final boolean isPrimary = c.getInt(0) != 0;
                    final boolean isSuperPrimary = c.getInt(1) != 0;
                    // Clear values if they are currently set
                    if (isSuperPrimary) {
                        mDbHelper.clearSuperPrimary(rawContactId, mimeTypeId);
                    }
                    if (clearPrimary && isPrimary) {
                        mDbHelper.setIsPrimary(rawContactId, -1, mimeTypeId);
                    }
                }
            } finally {
                c.close();
            }
        } else {
            // Check if we want to set values
            final boolean setPrimary = hasPrimary &&
                    values.getAsInteger(Data.IS_PRIMARY) != 0;
            final boolean setSuperPrimary = hasSuperPrimary &&
                    values.getAsInteger(Data.IS_SUPER_PRIMARY) != 0;
            if (setSuperPrimary) {
                // Set both super primary and primary
                mDbHelper.setIsSuperPrimary(rawContactId, dataId, mimeTypeId);
                mDbHelper.setIsPrimary(rawContactId, dataId, mimeTypeId);
            } else if (setPrimary) {
                // Primary was explicitly set, but super-primary was not.
                // In this case we set super-primary on this data item, if
                // any data item of the same raw-contact already is super-primary
                if (mDbHelper.rawContactHasSuperPrimary(rawContactId, mimeTypeId)) {
                    mDbHelper.setIsSuperPrimary(rawContactId, dataId, mimeTypeId);
                }
                mDbHelper.setIsPrimary(rawContactId, dataId, mimeTypeId);
            }
        }

        // Now that we've taken care of clearing this, remove it from "values".
        values.remove(Data.IS_SUPER_PRIMARY);
        values.remove(Data.IS_PRIMARY);
    }

    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long dataId = c.getLong(DataDeleteQuery._ID);
        long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
        boolean primary = c.getInt(DataDeleteQuery.IS_PRIMARY) != 0;
        mSelectionArgs1[0] = String.valueOf(dataId);
        int count = db.delete(Tables.DATA, Data._ID + "=?", mSelectionArgs1);
        mSelectionArgs1[0] = String.valueOf(rawContactId);
        db.delete(Tables.PRESENCE, PresenceColumns.RAW_CONTACT_ID + "=?", mSelectionArgs1);
        if (count != 0 && primary) {
            fixPrimary(db, rawContactId);
        }

        if (hasSearchableData()) {
            txContext.invalidateSearchIndexForRawContact(rawContactId);
        }

        return count;
    }

    private void fixPrimary(SQLiteDatabase db, long rawContactId) {
        long mimeTypeId = getMimeTypeId();
        long primaryId = -1;
        int primaryType = -1;
        mSelectionArgs1[0] = String.valueOf(rawContactId);
        Cursor c = db.query(DataDeleteQuery.TABLE,
                DataDeleteQuery.CONCRETE_COLUMNS,
                Data.RAW_CONTACT_ID + "=?" +
                    " AND " + DataColumns.MIMETYPE_ID + "=" + mimeTypeId,
                mSelectionArgs1, null, null, null);
        try {
            while (c.moveToNext()) {
                long dataId = c.getLong(DataDeleteQuery._ID);
                int type = c.getInt(DataDeleteQuery.DATA1);
                if (primaryType == -1 || getTypeRank(type) < getTypeRank(primaryType)) {
                    primaryId = dataId;
                    primaryType = type;
                }
            }
        } finally {
            c.close();
        }
        if (primaryId != -1) {
            mDbHelper.setIsPrimary(rawContactId, primaryId, mimeTypeId);
        }
    }

    /**
     * Returns the rank of a specific record type to be used in determining the primary
     * row. Lower number represents higher priority.
     */
    protected int getTypeRank(int type) {
        return 0;
    }

    protected void fixRawContactDisplayName(SQLiteDatabase db, TransactionContext txContext,
            long rawContactId) {
        if (!isNewRawContact(txContext, rawContactId)) {
            mDbHelper.updateRawContactDisplayName(db, rawContactId);
            mContactAggregator.updateDisplayNameForRawContact(db, rawContactId);
        }
    }

    private boolean isNewRawContact(TransactionContext txContext, long rawContactId) {
        return txContext.isNewRawContact(rawContactId);
    }

    /**
     * Return set of values, using current values at given {@link Data#_ID}
     * as baseline, but augmented with any updates.  Returns null if there is
     * no change.
     */
    public ContentValues getAugmentedValues(SQLiteDatabase db, long dataId,
            ContentValues update) {
        boolean changing = false;
        final ContentValues values = new ContentValues();
        mSelectionArgs1[0] = String.valueOf(dataId);
        final Cursor cursor = db.query(Tables.DATA, null, Data._ID + "=?",
                mSelectionArgs1, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    final String key = cursor.getColumnName(i);
                    final String value = cursor.getString(i);
                    if (!changing && update.containsKey(key)) {
                        Object newValue = update.get(key);
                        String newString = newValue == null ? null : newValue.toString();
                        changing |= !TextUtils.equals(newString, value);
                    }
                    values.put(key, value);
                }
            }
        } finally {
            cursor.close();
        }
        if (!changing) {
            return null;
        }

        values.putAll(update);
        return values;
    }

    public void triggerAggregation(TransactionContext txContext, long rawContactId) {
        mContactAggregator.triggerAggregation(txContext, rawContactId);
    }

    /**
     * Test all against {@link TextUtils#isEmpty(CharSequence)}.
     */
    public boolean areAllEmpty(ContentValues values, String[] keys) {
        for (String key : keys) {
            if (!TextUtils.isEmpty(values.getAsString(key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if a value (possibly null) is specified for at least one of the supplied keys.
     */
    public boolean areAnySpecified(ContentValues values, String[] keys) {
        for (String key : keys) {
            if (values.containsKey(key)) {
                return true;
            }
        }
        return false;
    }
}
