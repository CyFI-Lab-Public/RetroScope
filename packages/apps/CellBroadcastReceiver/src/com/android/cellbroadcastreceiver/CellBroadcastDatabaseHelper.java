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

package com.android.cellbroadcastreceiver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Telephony;
import android.telephony.SmsCbCmasInfo;
import android.telephony.SmsCbEtwsInfo;
import android.telephony.SmsCbMessage;
import android.util.Log;

import com.android.internal.telephony.gsm.SmsCbConstants;

/**
 * Open, create, and upgrade the cell broadcast SQLite database. Previously an inner class of
 * {@code CellBroadcastDatabase}, this is now a top-level class. The column definitions in
 * {@code CellBroadcastDatabase} have been moved to {@link Telephony.CellBroadcasts} in the
 * framework, to simplify access to this database from third-party apps.
 */
public class CellBroadcastDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "CellBroadcastDatabaseHelper";

    static final String DATABASE_NAME = "cell_broadcasts.db";
    static final String TABLE_NAME = "broadcasts";

    /** Temporary table for upgrading the database version. */
    static final String TEMP_TABLE_NAME = "old_broadcasts";

    /**
     * Database version 1: initial version
     * Database version 2-9: (reserved for OEM database customization)
     * Database version 10: adds ETWS and CMAS columns and CDMA support
     * Database version 11: adds delivery time index
     */
    static final int DATABASE_VERSION = 11;

    CellBroadcastDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + Telephony.CellBroadcasts._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Telephony.CellBroadcasts.GEOGRAPHICAL_SCOPE + " INTEGER,"
                + Telephony.CellBroadcasts.PLMN + " TEXT,"
                + Telephony.CellBroadcasts.LAC + " INTEGER,"
                + Telephony.CellBroadcasts.CID + " INTEGER,"
                + Telephony.CellBroadcasts.SERIAL_NUMBER + " INTEGER,"
                + Telephony.CellBroadcasts.SERVICE_CATEGORY + " INTEGER,"
                + Telephony.CellBroadcasts.LANGUAGE_CODE + " TEXT,"
                + Telephony.CellBroadcasts.MESSAGE_BODY + " TEXT,"
                + Telephony.CellBroadcasts.DELIVERY_TIME + " INTEGER,"
                + Telephony.CellBroadcasts.MESSAGE_READ + " INTEGER,"
                + Telephony.CellBroadcasts.MESSAGE_FORMAT + " INTEGER,"
                + Telephony.CellBroadcasts.MESSAGE_PRIORITY + " INTEGER,"
                + Telephony.CellBroadcasts.ETWS_WARNING_TYPE + " INTEGER,"
                + Telephony.CellBroadcasts.CMAS_MESSAGE_CLASS + " INTEGER,"
                + Telephony.CellBroadcasts.CMAS_CATEGORY + " INTEGER,"
                + Telephony.CellBroadcasts.CMAS_RESPONSE_TYPE + " INTEGER,"
                + Telephony.CellBroadcasts.CMAS_SEVERITY + " INTEGER,"
                + Telephony.CellBroadcasts.CMAS_URGENCY + " INTEGER,"
                + Telephony.CellBroadcasts.CMAS_CERTAINTY + " INTEGER);");

        createDeliveryTimeIndex(db);
    }

    private void createDeliveryTimeIndex(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS deliveryTimeIndex ON " + TABLE_NAME
                + " (" + Telephony.CellBroadcasts.DELIVERY_TIME + ");");
    }

    /** Columns to copy on database upgrade. */
    private static final String[] COLUMNS_V1 = {
            Telephony.CellBroadcasts.GEOGRAPHICAL_SCOPE,
            Telephony.CellBroadcasts.SERIAL_NUMBER,
            Telephony.CellBroadcasts.V1_MESSAGE_CODE,
            Telephony.CellBroadcasts.V1_MESSAGE_IDENTIFIER,
            Telephony.CellBroadcasts.LANGUAGE_CODE,
            Telephony.CellBroadcasts.MESSAGE_BODY,
            Telephony.CellBroadcasts.DELIVERY_TIME,
            Telephony.CellBroadcasts.MESSAGE_READ,
    };

    private static final int COLUMN_V1_GEOGRAPHICAL_SCOPE   = 0;
    private static final int COLUMN_V1_SERIAL_NUMBER        = 1;
    private static final int COLUMN_V1_MESSAGE_CODE         = 2;
    private static final int COLUMN_V1_MESSAGE_IDENTIFIER   = 3;
    private static final int COLUMN_V1_LANGUAGE_CODE        = 4;
    private static final int COLUMN_V1_MESSAGE_BODY         = 5;
    private static final int COLUMN_V1_DELIVERY_TIME        = 6;
    private static final int COLUMN_V1_MESSAGE_READ         = 7;

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == newVersion) {
            return;
        }
        // always log database upgrade
        log("Upgrading DB from version " + oldVersion + " to " + newVersion);

        // Upgrade from V1 to V10
        if (oldVersion == 1) {
            db.beginTransaction();
            try {
                // Step 1: rename original table
                db.execSQL("DROP TABLE IF EXISTS " + TEMP_TABLE_NAME);
                db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO " + TEMP_TABLE_NAME);

                // Step 2: create new table and indices
                onCreate(db);

                // Step 3: copy each message into the new table
                Cursor cursor = db.query(TEMP_TABLE_NAME, COLUMNS_V1, null, null, null, null,
                        null);
                try {
                    while (cursor.moveToNext()) {
                        upgradeMessageV1ToV2(db, cursor);
                    }
                } finally {
                    cursor.close();
                }

                // Step 4: drop the original table and commit transaction
                db.execSQL("DROP TABLE " + TEMP_TABLE_NAME);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            oldVersion = 10;    // skip to version 10
        }

        // Note to OEMs: if you have customized the database schema since V1, you will need to
        // add your own code here to convert from your version to version 10.
        if (oldVersion < 10) {
            throw new SQLiteException("CellBroadcastDatabase doesn't know how to upgrade "
                    + " DB version " + oldVersion + " (customized by OEM?)");
        }

        if (oldVersion == 10) {
            createDeliveryTimeIndex(db);
            oldVersion++;
        }
    }

    /**
     * Upgrades a single broadcast message from version 1 to version 2.
     */
    private static void upgradeMessageV1ToV2(SQLiteDatabase db, Cursor cursor) {
        int geographicalScope = cursor.getInt(COLUMN_V1_GEOGRAPHICAL_SCOPE);
        int updateNumber = cursor.getInt(COLUMN_V1_SERIAL_NUMBER);
        int messageCode = cursor.getInt(COLUMN_V1_MESSAGE_CODE);
        int messageId = cursor.getInt(COLUMN_V1_MESSAGE_IDENTIFIER);
        String languageCode = cursor.getString(COLUMN_V1_LANGUAGE_CODE);
        String messageBody = cursor.getString(COLUMN_V1_MESSAGE_BODY);
        long deliveryTime = cursor.getLong(COLUMN_V1_DELIVERY_TIME);
        boolean isRead = (cursor.getInt(COLUMN_V1_MESSAGE_READ) != 0);

        int serialNumber = ((geographicalScope & 0x03) << 14)
                | ((messageCode & 0x3ff) << 4) | (updateNumber & 0x0f);

        ContentValues cv = new ContentValues(16);
        cv.put(Telephony.CellBroadcasts.GEOGRAPHICAL_SCOPE, geographicalScope);
        cv.put(Telephony.CellBroadcasts.SERIAL_NUMBER, serialNumber);
        cv.put(Telephony.CellBroadcasts.SERVICE_CATEGORY, messageId);
        cv.put(Telephony.CellBroadcasts.LANGUAGE_CODE, languageCode);
        cv.put(Telephony.CellBroadcasts.MESSAGE_BODY, messageBody);
        cv.put(Telephony.CellBroadcasts.DELIVERY_TIME, deliveryTime);
        cv.put(Telephony.CellBroadcasts.MESSAGE_READ, isRead);
        cv.put(Telephony.CellBroadcasts.MESSAGE_FORMAT, SmsCbMessage.MESSAGE_FORMAT_3GPP);

        int etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_UNKNOWN;
        int cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_UNKNOWN;
        int cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_UNKNOWN;
        int cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_UNKNOWN;
        int cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_UNKNOWN;
        switch (messageId) {
            case SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_WARNING:
                etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_EARTHQUAKE;
                break;

            case SmsCbConstants.MESSAGE_ID_ETWS_TSUNAMI_WARNING:
                etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_TSUNAMI;
                break;

            case SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_AND_TSUNAMI_WARNING:
                etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_EARTHQUAKE_AND_TSUNAMI;
                break;

            case SmsCbConstants.MESSAGE_ID_ETWS_TEST_MESSAGE:
                etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_TEST_MESSAGE;
                break;

            case SmsCbConstants.MESSAGE_ID_ETWS_OTHER_EMERGENCY_TYPE:
                etwsWarningType = SmsCbEtwsInfo.ETWS_WARNING_TYPE_OTHER_EMERGENCY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_PRESIDENTIAL_LEVEL:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_PRESIDENTIAL_LEVEL_ALERT;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_OBSERVED:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_EXTREME_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_EXTREME;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_LIKELY:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_EXTREME_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_EXTREME;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_OBSERVED:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_EXTREME;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_EXPECTED;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_LIKELY:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_EXTREME;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_EXPECTED;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_OBSERVED:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_SEVERE;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_LIKELY:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_SEVERE;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_OBSERVED:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_SEVERE;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_EXPECTED;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_LIKELY:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;
                cmasSeverity = SmsCbCmasInfo.CMAS_SEVERITY_SEVERE;
                cmasUrgency = SmsCbCmasInfo.CMAS_URGENCY_EXPECTED;
                cmasCertainty = SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_CHILD_ABDUCTION_EMERGENCY:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_CHILD_ABDUCTION_EMERGENCY;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_REQUIRED_MONTHLY_TEST:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_REQUIRED_MONTHLY_TEST;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXERCISE:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_CMAS_EXERCISE;
                break;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_OPERATOR_DEFINED_USE:
                cmasMessageClass = SmsCbCmasInfo.CMAS_CLASS_OPERATOR_DEFINED_USE;
                break;
        }

        if (etwsWarningType != SmsCbEtwsInfo.ETWS_WARNING_TYPE_UNKNOWN
                || cmasMessageClass != SmsCbCmasInfo.CMAS_CLASS_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.MESSAGE_PRIORITY,
                    SmsCbMessage.MESSAGE_PRIORITY_EMERGENCY);
        } else {
            cv.put(Telephony.CellBroadcasts.MESSAGE_PRIORITY,
                    SmsCbMessage.MESSAGE_PRIORITY_NORMAL);
        }

        if (etwsWarningType != SmsCbEtwsInfo.ETWS_WARNING_TYPE_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.ETWS_WARNING_TYPE, etwsWarningType);
        }

        if (cmasMessageClass != SmsCbCmasInfo.CMAS_CLASS_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.CMAS_MESSAGE_CLASS, cmasMessageClass);
        }

        if (cmasSeverity != SmsCbCmasInfo.CMAS_SEVERITY_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.CMAS_SEVERITY, cmasSeverity);
        }

        if (cmasUrgency != SmsCbCmasInfo.CMAS_URGENCY_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.CMAS_URGENCY, cmasUrgency);
        }

        if (cmasCertainty != SmsCbCmasInfo.CMAS_CERTAINTY_UNKNOWN) {
            cv.put(Telephony.CellBroadcasts.CMAS_CERTAINTY, cmasCertainty);
        }

        db.insert(TABLE_NAME, null, cv);
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
