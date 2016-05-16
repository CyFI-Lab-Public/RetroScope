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

package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract.Profile;

import com.android.providers.contacts.util.NeededForTesting;

/**
 * A separate version of the contacts database helper for storing the user's profile data.
 */
public class ProfileDatabaseHelper extends ContactsDatabaseHelper {
    private static final String TAG = "ProfileDatabaseHelper";

    private static final String DATABASE_NAME = "profile.db";

    // SQLite-standard table and columns for tracking autoincrement sequences.
    private static final String SEQUENCE_TABLE = "sqlite_sequence";
    private static final String SEQUENCE_NAME = "name";
    private static final String SEQUENCE_SEQ = "seq";

    private static ProfileDatabaseHelper sSingleton = null;

    /**
     * Returns a new instance for unit tests.
     */
    @NeededForTesting
    public static ProfileDatabaseHelper getNewInstanceForTest(Context context) {
        return new ProfileDatabaseHelper(context, null, false);
    }

    private ProfileDatabaseHelper(
            Context context, String databaseName, boolean optimizationEnabled) {
        super(context, databaseName, optimizationEnabled);
    }

    public static synchronized ProfileDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new ProfileDatabaseHelper(context, DATABASE_NAME, true);
        }
        return sSingleton;
    }

    @Override
    protected int dbForProfile() {
        return 1;
    }

    @Override
    protected void initializeAutoIncrementSequences(SQLiteDatabase db) {
        for (String table : Tables.SEQUENCE_TABLES) {
            ContentValues values = new ContentValues();
            values.put(SEQUENCE_NAME, table);
            values.put(SEQUENCE_SEQ, Profile.MIN_ID);
            db.insert(SEQUENCE_TABLE, null, values);
        }
    }
}
