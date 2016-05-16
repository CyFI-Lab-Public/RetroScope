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
 * limitations under the License.
 */

package com.android.providers.userdictionary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupAgentHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.UserDictionary.Words;
import android.text.TextUtils;
import android.util.Log;

import libcore.io.IoUtils;

/**
 * Performs backup and restore of the User Dictionary.
 */
public class DictionaryBackupAgent extends BackupAgentHelper {

    private static final String KEY_DICTIONARY = "userdictionary";

    private static final int STATE_DICTIONARY = 0;
    private static final int STATE_SIZE = 1;

    private static final String SEPARATOR = "|";

    private static final byte[] EMPTY_DATA = new byte[0];

    private static final String TAG = "DictionaryBackupAgent";

    private static final int COLUMN_WORD = 1;
    private static final int COLUMN_FREQUENCY = 2;
    private static final int COLUMN_LOCALE = 3;
    private static final int COLUMN_APPID = 4;
    private static final int COLUMN_SHORTCUT = 5;

    private static final String[] PROJECTION = {
        Words._ID,
        Words.WORD,
        Words.FREQUENCY,
        Words.LOCALE,
        Words.APP_ID,
        Words.SHORTCUT
    };

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) throws IOException {

        byte[] userDictionaryData = getDictionary();

        long[] stateChecksums = readOldChecksums(oldState);

        stateChecksums[STATE_DICTIONARY] =
                writeIfChanged(stateChecksums[STATE_DICTIONARY], KEY_DICTIONARY,
                        userDictionaryData, data);

        writeNewChecksums(stateChecksums, newState);
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
            ParcelFileDescriptor newState) throws IOException {

        while (data.readNextHeader()) {
            final String key = data.getKey();
            final int size = data.getDataSize();
            if (KEY_DICTIONARY.equals(key)) {
                restoreDictionary(data, Words.CONTENT_URI);
            } else {
                data.skipEntityData();
            }
        }
    }

    private long[] readOldChecksums(ParcelFileDescriptor oldState) throws IOException {
        long[] stateChecksums = new long[STATE_SIZE];

        DataInputStream dataInput = new DataInputStream(
                new FileInputStream(oldState.getFileDescriptor()));
        for (int i = 0; i < STATE_SIZE; i++) {
            try {
                stateChecksums[i] = dataInput.readLong();
            } catch (EOFException eof) {
                break;
            }
        }
        dataInput.close();
        return stateChecksums;
    }

    private void writeNewChecksums(long[] checksums, ParcelFileDescriptor newState)
            throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(
                new FileOutputStream(newState.getFileDescriptor()));
        for (int i = 0; i < STATE_SIZE; i++) {
            dataOutput.writeLong(checksums[i]);
        }
        dataOutput.close();
    }

    private long writeIfChanged(long oldChecksum, String key, byte[] data,
            BackupDataOutput output) {
        CRC32 checkSummer = new CRC32();
        checkSummer.update(data);
        long newChecksum = checkSummer.getValue();
        if (oldChecksum == newChecksum) {
            return oldChecksum;
        }
        try {
            output.writeEntityHeader(key, data.length);
            output.writeEntityData(data, data.length);
        } catch (IOException ioe) {
            // Bail
        }
        return newChecksum;
    }

    private byte[] getDictionary() {
        Cursor cursor = getContentResolver().query(Words.CONTENT_URI, PROJECTION,
                null, null, Words.WORD);
        if (cursor == null) return EMPTY_DATA;
        if (!cursor.moveToFirst()) {
            Log.e(TAG, "Couldn't read from the cursor");
            cursor.close();
            return EMPTY_DATA;
        }
        byte[] sizeBytes = new byte[4];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(cursor.getCount() * 10);
        GZIPOutputStream gzip = null;
        try {
            gzip = new GZIPOutputStream(baos);
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(COLUMN_WORD);
                int frequency = cursor.getInt(COLUMN_FREQUENCY);
                String locale = cursor.getString(COLUMN_LOCALE);
                int appId = cursor.getInt(COLUMN_APPID);
                String shortcut = cursor.getString(COLUMN_SHORTCUT);
                if (TextUtils.isEmpty(shortcut)) shortcut = "";
                // TODO: escape the string
                String out = name + SEPARATOR + frequency + SEPARATOR + locale + SEPARATOR + appId
                        + SEPARATOR + shortcut;
                byte[] line = out.getBytes();
                writeInt(sizeBytes, 0, line.length);
                gzip.write(sizeBytes);
                gzip.write(line);
                cursor.moveToNext();
            }
            gzip.finish();
        } catch (IOException ioe) {
            Log.e(TAG, "Couldn't compress the dictionary:\n" + ioe);
            return EMPTY_DATA;
        } finally {
            IoUtils.closeQuietly(gzip);
            cursor.close();
        }
        return baos.toByteArray();
    }

    private void restoreDictionary(BackupDataInput data, Uri contentUri) {
        ContentValues cv = new ContentValues(2);
        byte[] dictCompressed = new byte[data.getDataSize()];
        byte[] dictionary = null;
        try {
            data.readEntityData(dictCompressed, 0, dictCompressed.length);
            GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(dictCompressed));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] tempData = new byte[1024];
            int got;
            while ((got = gzip.read(tempData)) > 0) {
                baos.write(tempData, 0, got);
            }
            gzip.close();
            dictionary = baos.toByteArray();
        } catch (IOException ioe) {
            Log.e(TAG, "Couldn't read and uncompress entity data:\n" + ioe);
            return;
        }
        int pos = 0;
        while (pos + 4 < dictionary.length) {
            int length = readInt(dictionary, pos);
            pos += 4;
            if (pos + length > dictionary.length) {
                Log.e(TAG, "Insufficient data");
            }
            String line = new String(dictionary, pos, length);
            pos += length;
            // TODO: unescape the string
            StringTokenizer st = new StringTokenizer(line, SEPARATOR);
            String word;
            String frequency;
            try {
                word = st.nextToken();
                frequency = st.nextToken();
                String locale = null;
                String appid = null;
                String shortcut = null;
                if (st.hasMoreTokens()) locale = st.nextToken();
                if ("null".equalsIgnoreCase(locale)) locale = null;
                if (st.hasMoreTokens()) appid = st.nextToken();
                if (st.hasMoreTokens()) shortcut = st.nextToken();
                if (TextUtils.isEmpty(shortcut)) shortcut = null;
                int frequencyInt = Integer.parseInt(frequency);
                int appidInt = appid != null? Integer.parseInt(appid) : 0;

                if (!TextUtils.isEmpty(frequency)) {
                    cv.clear();
                    cv.put(Words.WORD, word);
                    cv.put(Words.FREQUENCY, frequencyInt);
                    cv.put(Words.LOCALE, locale);
                    cv.put(Words.APP_ID, appidInt);
                    cv.put(Words.SHORTCUT, shortcut);
                    // Remove duplicate first
                    getContentResolver().delete(contentUri, Words.WORD + "=? and "
                            + Words.SHORTCUT + "=?", new String[] {word, shortcut});
                    getContentResolver().insert(contentUri, cv);
                }
            } catch (NoSuchElementException nsee) {
                Log.e(TAG, "Token format error\n" + nsee);
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "Number format error\n" + nfe);
            }
        }
    }

    /**
     * Write an int in BigEndian into the byte array.
     * @param out byte array
     * @param pos current pos in array
     * @param value integer to write
     * @return the index after adding the size of an int (4)
     */
    private int writeInt(byte[] out, int pos, int value) {
        out[pos + 0] = (byte) ((value >> 24) & 0xFF);
        out[pos + 1] = (byte) ((value >> 16) & 0xFF);
        out[pos + 2] = (byte) ((value >>  8) & 0xFF);
        out[pos + 3] = (byte) ((value >>  0) & 0xFF);
        return pos + 4;
    }

    private int readInt(byte[] in, int pos) {
        int result =
                ((in[pos    ] & 0xFF) << 24) |
                ((in[pos + 1] & 0xFF) << 16) |
                ((in[pos + 2] & 0xFF) <<  8) |
                ((in[pos + 3] & 0xFF) <<  0);
        return result;
    }
}
