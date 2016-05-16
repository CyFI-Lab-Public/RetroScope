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
import android.provider.ContactsContract.CommonDataKinds.BaseTypes;
import android.text.TextUtils;

import com.android.providers.contacts.aggregation.ContactAggregator;

/**
 * Superclass for data row handlers that deal with types (e.g. Home, Work, Other) and
 * labels, which are custom types.
 */
public class DataRowHandlerForCommonDataKind extends DataRowHandler {

    private final String mTypeColumn;
    private final String mLabelColumn;

    public DataRowHandlerForCommonDataKind(Context context, ContactsDatabaseHelper dbHelper,
            ContactAggregator aggregator, String mimetype, String typeColumn, String labelColumn) {
        super(context, dbHelper, aggregator, mimetype);
        mTypeColumn = typeColumn;
        mLabelColumn = labelColumn;
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId,
            ContentValues values) {
        enforceTypeAndLabel(values);
        return super.insert(db, txContext, rawContactId, values);
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values,
            Cursor c, boolean callerIsSyncAdapter) {
        final long dataId = c.getLong(DataUpdateQuery._ID);
        final ContentValues augmented = getAugmentedValues(db, dataId, values);
        if (augmented == null) {        // No change
            return false;
        }
        enforceTypeAndLabel(augmented);
        return super.update(db, txContext, values, c, callerIsSyncAdapter);
    }

    /**
     * If the given {@link ContentValues} defines {@link #mTypeColumn},
     * enforce that {@link #mLabelColumn} only appears when type is
     * {@link BaseTypes#TYPE_CUSTOM}. Exception is thrown otherwise.
     */
    private void enforceTypeAndLabel(ContentValues augmented) {
        final boolean hasType = !TextUtils.isEmpty(augmented.getAsString(mTypeColumn));
        final boolean hasLabel = !TextUtils.isEmpty(augmented.getAsString(mLabelColumn));

        if (hasLabel && !hasType) {
            // When label exists, assert that some type is defined
            throw new IllegalArgumentException(mTypeColumn + " must be specified when "
                    + mLabelColumn + " is defined.");
        }
    }

    @Override
    public boolean hasSearchableData() {
        return true;
    }
}
