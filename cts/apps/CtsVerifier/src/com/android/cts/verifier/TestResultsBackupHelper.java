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

package com.android.cts.verifier;

import com.android.cts.verifier.backup.BackupTestActivity;

import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** {@link BackupHelper} for the test results database. */
class TestResultsBackupHelper implements BackupHelper {

    private static final String TAG = TestResultsBackupHelper.class.getSimpleName();

    private static final String DB_BACKUP_KEY = "db";

    private final Context mContext;

    TestResultsBackupHelper(Context context) {
        mContext = context;
    }

    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) {
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(TestResultsProvider.RESULTS_CONTENT_URI,
                    null, null, null, null);
            int nameIndex = cursor.getColumnIndex(TestResultsProvider.COLUMN_TEST_NAME);
            int resultIndex = cursor.getColumnIndex(TestResultsProvider.COLUMN_TEST_RESULT);
            int infoSeenIndex = cursor.getColumnIndex(TestResultsProvider.COLUMN_TEST_INFO_SEEN);
            int detailsIndex = cursor.getColumnIndex(TestResultsProvider.COLUMN_TEST_DETAILS);

            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            DataOutputStream dataOutput = new DataOutputStream(byteOutput);

            dataOutput.writeInt(cursor.getCount());
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIndex);
                int result = cursor.getInt(resultIndex);
                int infoSeen = cursor.getInt(infoSeenIndex);
                String details = cursor.getString(detailsIndex);

                dataOutput.writeUTF(name);
                dataOutput.writeInt(result);
                dataOutput.writeInt(infoSeen);
                dataOutput.writeUTF(details != null ? details : "");
            }

            byte[] rawBytes = byteOutput.toByteArray();
            data.writeEntityHeader(DB_BACKUP_KEY, rawBytes.length);
            data.writeEntityData(rawBytes, rawBytes.length);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't backup test results...", e);
            failBackupTest();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void restoreEntity(BackupDataInputStream data) {
        try {
            if (DB_BACKUP_KEY.equals(data.getKey())) {
                byte[] rawBytes = new byte[data.size()];
                data.read(rawBytes, 0, data.size());

                ByteArrayInputStream byteInput = new ByteArrayInputStream(rawBytes);
                DataInputStream dataInput = new DataInputStream(byteInput);

                int numRows = dataInput.readInt();
                ContentValues[] values = new ContentValues[numRows];
                for (int i = 0; i < numRows; i++) {
                    String name = dataInput.readUTF();
                    int result = dataInput.readInt();
                    int infoSeen = dataInput.readInt();
                    String details = dataInput.readUTF();

                    values[i] = new ContentValues();
                    values[i].put(TestResultsProvider.COLUMN_TEST_NAME, name);
                    values[i].put(TestResultsProvider.COLUMN_TEST_RESULT, result);
                    values[i].put(TestResultsProvider.COLUMN_TEST_INFO_SEEN, infoSeen);
                    values[i].put(TestResultsProvider.COLUMN_TEST_DETAILS, details);
                }

                ContentResolver resolver = mContext.getContentResolver();
                resolver.bulkInsert(TestResultsProvider.RESULTS_CONTENT_URI, values);
            } else {
                Log.e(TAG, "Skipping key: " + data.getKey());
            }
        } catch (IOException e) {
            Log.e(TAG, "Couldn't restore test results...", e);
            failBackupTest();
        }
    }

    private void failBackupTest() {
        TestResultsProvider.setTestResult(mContext, BackupTestActivity.class.getName(),
                TestResult.TEST_RESULT_FAILED, null);
    }

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState) {
    }
}
