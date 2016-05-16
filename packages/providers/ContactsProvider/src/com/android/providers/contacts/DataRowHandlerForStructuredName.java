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
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.FullNameStyle;
import android.provider.ContactsContract.PhoneticNameStyle;
import android.text.TextUtils;

import com.android.providers.contacts.SearchIndexManager.IndexBuilder;
import com.android.providers.contacts.aggregation.ContactAggregator;

/**
 * Handler for email address data rows.
 */
public class DataRowHandlerForStructuredName extends DataRowHandler {
    private final NameSplitter mSplitter;
    private final NameLookupBuilder mNameLookupBuilder;
    private final StringBuilder mSb = new StringBuilder();

    public DataRowHandlerForStructuredName(Context context, ContactsDatabaseHelper dbHelper,
            ContactAggregator aggregator, NameSplitter splitter,
            NameLookupBuilder nameLookupBuilder) {
        super(context, dbHelper, aggregator, StructuredName.CONTENT_ITEM_TYPE);
        mSplitter = splitter;
        mNameLookupBuilder = nameLookupBuilder;
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId,
            ContentValues values) {
        fixStructuredNameComponents(values, values);

        long dataId = super.insert(db, txContext, rawContactId, values);

        String name = values.getAsString(StructuredName.DISPLAY_NAME);
        Integer fullNameStyle = values.getAsInteger(StructuredName.FULL_NAME_STYLE);
        mNameLookupBuilder.insertNameLookup(rawContactId, dataId, name,
                fullNameStyle != null
                        ? mSplitter.getAdjustedFullNameStyle(fullNameStyle)
                        : FullNameStyle.UNDEFINED);
        insertNameLookupForPhoneticName(rawContactId, dataId, values);
        fixRawContactDisplayName(db, txContext, rawContactId);
        triggerAggregation(txContext, rawContactId);
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values,
            Cursor c, boolean callerIsSyncAdapter) {
        final long dataId = c.getLong(DataUpdateQuery._ID);
        final long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);

        final ContentValues augmented = getAugmentedValues(db, dataId, values);
        if (augmented == null) {  // No change
            return false;
        }

        fixStructuredNameComponents(augmented, values);

        super.update(db, txContext, values, c, callerIsSyncAdapter);
        if (values.containsKey(StructuredName.DISPLAY_NAME) ||
                values.containsKey(StructuredName.PHONETIC_FAMILY_NAME) ||
                values.containsKey(StructuredName.PHONETIC_MIDDLE_NAME) ||
                values.containsKey(StructuredName.PHONETIC_GIVEN_NAME)) {
            augmented.putAll(values);
            String name = augmented.getAsString(StructuredName.DISPLAY_NAME);
            mDbHelper.deleteNameLookup(dataId);
            Integer fullNameStyle = augmented.getAsInteger(StructuredName.FULL_NAME_STYLE);
            mNameLookupBuilder.insertNameLookup(rawContactId, dataId, name,
                    fullNameStyle != null
                            ? mSplitter.getAdjustedFullNameStyle(fullNameStyle)
                            : FullNameStyle.UNDEFINED);
            insertNameLookupForPhoneticName(rawContactId, dataId, augmented);
        }
        fixRawContactDisplayName(db, txContext, rawContactId);
        triggerAggregation(txContext, rawContactId);
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

    /**
     * Specific list of structured fields.
     */
    private final String[] STRUCTURED_FIELDS = new String[] {
            StructuredName.PREFIX, StructuredName.GIVEN_NAME, StructuredName.MIDDLE_NAME,
            StructuredName.FAMILY_NAME, StructuredName.SUFFIX
    };

    /**
     * Parses the supplied display name, but only if the incoming values do
     * not already contain structured name parts. Also, if the display name
     * is not provided, generate one by concatenating first name and last
     * name.
     */
    public void fixStructuredNameComponents(ContentValues augmented, ContentValues update) {
        final String unstruct = update.getAsString(StructuredName.DISPLAY_NAME);

        final boolean touchedUnstruct = !TextUtils.isEmpty(unstruct);
        final boolean touchedStruct = !areAllEmpty(update, STRUCTURED_FIELDS);

        if (touchedUnstruct && !touchedStruct) {
            NameSplitter.Name name = new NameSplitter.Name();
            mSplitter.split(name, unstruct);
            name.toValues(update);
        } else if (!touchedUnstruct
                && (touchedStruct || areAnySpecified(update, STRUCTURED_FIELDS))) {
            // We need to update the display name when any structured components
            // are specified, even when they are null, which is why we are checking
            // areAnySpecified.  The touchedStruct in the condition is an optimization:
            // if there are non-null values, we know for a fact that some values are present.
            NameSplitter.Name name = new NameSplitter.Name();
            name.fromValues(augmented);
            // As the name could be changed, let's guess the name style again.
            name.fullNameStyle = FullNameStyle.UNDEFINED;
            name.phoneticNameStyle = PhoneticNameStyle.UNDEFINED;
            mSplitter.guessNameStyle(name);
            int unadjustedFullNameStyle = name.fullNameStyle;
            name.fullNameStyle = mSplitter.getAdjustedFullNameStyle(name.fullNameStyle);
            final String joined = mSplitter.join(name, true, true);
            update.put(StructuredName.DISPLAY_NAME, joined);

            update.put(StructuredName.FULL_NAME_STYLE, unadjustedFullNameStyle);
            update.put(StructuredName.PHONETIC_NAME_STYLE, name.phoneticNameStyle);
        } else if (touchedUnstruct && touchedStruct){
            if (!update.containsKey(StructuredName.FULL_NAME_STYLE)) {
                update.put(StructuredName.FULL_NAME_STYLE,
                        mSplitter.guessFullNameStyle(unstruct));
            }
            if (!update.containsKey(StructuredName.PHONETIC_NAME_STYLE)) {
                NameSplitter.Name name = new NameSplitter.Name();
                name.fromValues(update);
                name.phoneticNameStyle = PhoneticNameStyle.UNDEFINED;
                mSplitter.guessNameStyle(name);
                update.put(StructuredName.PHONETIC_NAME_STYLE, name.phoneticNameStyle);
            }
        }
    }

    public void insertNameLookupForPhoneticName(long rawContactId, long dataId,
            ContentValues values) {
        if (values.containsKey(StructuredName.PHONETIC_FAMILY_NAME)
                || values.containsKey(StructuredName.PHONETIC_GIVEN_NAME)
                || values.containsKey(StructuredName.PHONETIC_MIDDLE_NAME)) {
            mDbHelper.insertNameLookupForPhoneticName(rawContactId, dataId,
                    values.getAsString(StructuredName.PHONETIC_FAMILY_NAME),
                    values.getAsString(StructuredName.PHONETIC_MIDDLE_NAME),
                    values.getAsString(StructuredName.PHONETIC_GIVEN_NAME));
        }
    }

    @Override
    public boolean hasSearchableData() {
        return true;
    }

    @Override
    public boolean containsSearchableColumns(ContentValues values) {
        return values.containsKey(StructuredName.FAMILY_NAME)
                || values.containsKey(StructuredName.GIVEN_NAME)
                || values.containsKey(StructuredName.MIDDLE_NAME)
                || values.containsKey(StructuredName.PHONETIC_FAMILY_NAME)
                || values.containsKey(StructuredName.PHONETIC_GIVEN_NAME)
                || values.containsKey(StructuredName.PHONETIC_MIDDLE_NAME)
                || values.containsKey(StructuredName.PREFIX)
                || values.containsKey(StructuredName.SUFFIX);
    }

    @Override
    public void appendSearchableData(IndexBuilder builder) {
        String name = builder.getString(StructuredName.DISPLAY_NAME);
        Integer fullNameStyle = builder.getInt(StructuredName.FULL_NAME_STYLE);

        mNameLookupBuilder.appendToSearchIndex(builder, name, fullNameStyle != null
                        ? mSplitter.getAdjustedFullNameStyle(fullNameStyle)
                        : FullNameStyle.UNDEFINED);

        String phoneticFamily = builder.getString(StructuredName.PHONETIC_FAMILY_NAME);
        String phoneticMiddle = builder.getString(StructuredName.PHONETIC_MIDDLE_NAME);
        String phoneticGiven = builder.getString(StructuredName.PHONETIC_GIVEN_NAME);

        // Phonetic name is often spelled without spaces
        if (!TextUtils.isEmpty(phoneticFamily) || !TextUtils.isEmpty(phoneticMiddle)
                || !TextUtils.isEmpty(phoneticGiven)) {
            mSb.setLength(0);
            if (!TextUtils.isEmpty(phoneticFamily)) {
                builder.appendName(phoneticFamily);
                mSb.append(phoneticFamily);
            }
            if (!TextUtils.isEmpty(phoneticMiddle)) {
                builder.appendName(phoneticMiddle);
                mSb.append(phoneticMiddle);
            }
            if (!TextUtils.isEmpty(phoneticGiven)) {
                builder.appendName(phoneticGiven);
                mSb.append(phoneticGiven);
            }
            final String phoneticName = mSb.toString().trim();
            int phoneticNameStyle = builder.getInt(StructuredName.PHONETIC_NAME_STYLE);
            if (phoneticNameStyle == PhoneticNameStyle.UNDEFINED) {
                phoneticNameStyle = mSplitter.guessPhoneticNameStyle(phoneticName);
            }
            builder.appendName(phoneticName);
            mNameLookupBuilder.appendNameShorthandLookup(builder, phoneticName,
                    phoneticNameStyle);
        }
    }
}
