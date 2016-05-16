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
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.Data;

import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.SearchIndexManager.IndexBuilder;
import com.android.providers.contacts.aggregation.ContactAggregator;

/**
 * Handler for organization data rows.
 */
public class DataRowHandlerForOrganization extends DataRowHandlerForCommonDataKind {

    public DataRowHandlerForOrganization(Context context, ContactsDatabaseHelper dbHelper,
            ContactAggregator aggregator) {
        super(context, dbHelper, aggregator,
                Organization.CONTENT_ITEM_TYPE, Organization.TYPE, Organization.LABEL);
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId,
            ContentValues values) {
        String company = values.getAsString(Organization.COMPANY);
        String title = values.getAsString(Organization.TITLE);

        long dataId = super.insert(db, txContext, rawContactId, values);

        fixRawContactDisplayName(db, txContext, rawContactId);
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values,
            Cursor c, boolean callerIsSyncAdapter) {
        if (!super.update(db, txContext, values, c, callerIsSyncAdapter)) {
            return false;
        }

        boolean containsCompany = values.containsKey(Organization.COMPANY);
        boolean containsTitle = values.containsKey(Organization.TITLE);
        if (containsCompany || containsTitle) {
            long dataId = c.getLong(DataUpdateQuery._ID);
            long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);

            String company;

            if (containsCompany) {
                company = values.getAsString(Organization.COMPANY);
            } else {
                mSelectionArgs1[0] = String.valueOf(dataId);
                company = DatabaseUtils.stringForQuery(db,
                        "SELECT " + Organization.COMPANY +
                        " FROM " + Tables.DATA +
                        " WHERE " + Data._ID + "=?", mSelectionArgs1);
            }

            String title;
            if (containsTitle) {
                title = values.getAsString(Organization.TITLE);
            } else {
                mSelectionArgs1[0] = String.valueOf(dataId);
                title = DatabaseUtils.stringForQuery(db,
                        "SELECT " + Organization.TITLE +
                        " FROM " + Tables.DATA +
                        " WHERE " + Data._ID + "=?", mSelectionArgs1);
            }

            mDbHelper.deleteNameLookup(dataId);
            fixRawContactDisplayName(db, txContext, rawContactId);
        }
        return true;
    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long dataId = c.getLong(DataUpdateQuery._ID);
        long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);

        int count = super.delete(db, txContext, c);
        fixRawContactDisplayName(db, txContext, rawContactId);
        mDbHelper.deleteNameLookup(dataId);
        return count;
    }

    @Override
    protected int getTypeRank(int type) {
        switch (type) {
            case Organization.TYPE_WORK: return 0;
            case Organization.TYPE_CUSTOM: return 1;
            case Organization.TYPE_OTHER: return 2;
            default: return 1000;
        }
    }

    @Override
    public boolean containsSearchableColumns(ContentValues values) {
        return values.containsKey(Organization.COMPANY)
                || values.containsKey(Organization.DEPARTMENT)
                || values.containsKey(Organization.JOB_DESCRIPTION)
                || values.containsKey(Organization.OFFICE_LOCATION)
                || values.containsKey(Organization.PHONETIC_NAME)
                || values.containsKey(Organization.SYMBOL)
                || values.containsKey(Organization.TITLE);
    }

    @Override
    public void appendSearchableData(IndexBuilder builder) {
        builder.appendNameFromColumn(Organization.TITLE);
        builder.appendNameFromColumn(Organization.COMPANY);
        builder.appendContentFromColumn(Organization.TITLE);
        builder.appendContentFromColumn(Organization.COMPANY, IndexBuilder.SEPARATOR_COMMA);
        builder.appendContentFromColumn(Organization.PHONETIC_NAME,
                IndexBuilder.SEPARATOR_PARENTHESES);
        builder.appendContentFromColumn(Organization.SYMBOL, IndexBuilder.SEPARATOR_PARENTHESES);
        builder.appendContentFromColumn(Organization.DEPARTMENT, IndexBuilder.SEPARATOR_SLASH);
        builder.appendContentFromColumn(Organization.OFFICE_LOCATION, IndexBuilder.SEPARATOR_SLASH);
        builder.appendContentFromColumn(Organization.JOB_DESCRIPTION, IndexBuilder.SEPARATOR_SLASH);
    }
}
