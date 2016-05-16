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
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.text.TextUtils;

import com.android.providers.contacts.SearchIndexManager.IndexBuilder;
import com.android.providers.contacts.aggregation.ContactAggregator;

/**
 * Handler for nickname data rows.
 */
public class DataRowHandlerForNickname extends DataRowHandlerForCommonDataKind {

    public DataRowHandlerForNickname(
            Context context, ContactsDatabaseHelper dbHelper, ContactAggregator aggregator) {
        super(context, dbHelper, aggregator, Nickname.CONTENT_ITEM_TYPE, Nickname.TYPE,
                Nickname.LABEL);
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId,
            ContentValues values) {
        String nickname = values.getAsString(Nickname.NAME);

        long dataId = super.insert(db, txContext, rawContactId, values);

        if (!TextUtils.isEmpty(nickname)) {
            fixRawContactDisplayName(db, txContext, rawContactId);
            mDbHelper.insertNameLookupForNickname(rawContactId, dataId, nickname);
            triggerAggregation(txContext, rawContactId);
        }
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values,
            Cursor c, boolean callerIsSyncAdapter) {
        long dataId = c.getLong(DataUpdateQuery._ID);
        long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);

        if (!super.update(db, txContext, values, c, callerIsSyncAdapter)) {
            return false;
        }

        if (values.containsKey(Nickname.NAME)) {
            String nickname = values.getAsString(Nickname.NAME);
            mDbHelper.deleteNameLookup(dataId);
            mDbHelper.insertNameLookupForNickname(rawContactId, dataId, nickname);
            fixRawContactDisplayName(db, txContext, rawContactId);
            triggerAggregation(txContext, rawContactId);
        }

        return true;
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long dataId = c.getLong(DataDeleteQuery._ID);
        long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);

        int count = super.delete(db, txContext, c);

        mDbHelper.deleteNameLookup(dataId);
        fixRawContactDisplayName(db, txContext, rawContactId);
        triggerAggregation(txContext, rawContactId);
        return count;
    }

    @Override
    public boolean containsSearchableColumns(ContentValues values) {
        return values.containsKey(Nickname.NAME);
    }

    @Override
    public void appendSearchableData(IndexBuilder builder) {
        builder.appendNameFromColumn(Nickname.NAME);
        builder.appendContentFromColumn(Nickname.NAME);
    }
}
