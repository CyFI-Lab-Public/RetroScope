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

package com.android.cts.appwithdata;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.TrafficStats;
import android.test.AndroidTestCase;
import android.util.Log;

import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test that will create private app data.
 *
 * This is not really a test per-say. Its just used as a hook so the test controller can trigger
 * the creation of private app data.
 */
public class CreatePrivateDataTest extends AndroidTestCase {

    /**
     * The Android package name of the application that owns the private data
     */
    private static final String APP_WITH_DATA_PKG = "com.android.cts.appwithdata";

    /**
     * Name of private file to create.
     */
    private static final String PRIVATE_FILE_NAME = "private_file.txt";
    private static final String PUBLIC_FILE_NAME = "public_file.txt";

    private static final String PREFERENCES_FILE_NAME = "preferences";
    private static final String PREFERENCE_KEY = "preference_key";
    private static final String PREFERENCE_VALUE = "preference_value";

    static final String DB_TABLE_NAME = "test_table";
    static final String DB_COLUMN = "test_column";
    static final String DB_VALUE = "test_value";

    /**
     * Creates the private data for this app, which includes
     * file, database entries, and traffic stats.
     * @throws IOException if any error occurred when creating the file
     */
    public void testCreatePrivateData() throws IOException {
        FileOutputStream outputStream = getContext().openFileOutput(PRIVATE_FILE_NAME,
                Context.MODE_PRIVATE);
        outputStream.write("file contents".getBytes());
        outputStream.close();
        assertTrue(getContext().getFileStreamPath(PRIVATE_FILE_NAME).exists());

        outputStream = getContext().openFileOutput(PUBLIC_FILE_NAME,
                Context.MODE_WORLD_READABLE);
        DataOutputStream dataOut = new DataOutputStream(outputStream);
        dataOut.writeInt(getContext().getApplicationInfo().uid);
        dataOut.close();
        outputStream.close();
        // Ensure that some file will be accessible via the same path that will be used by other app.
        accessPublicData();

        writeToPreferences();
        writeToDatabase();
        createTrafficStatsWithTags();
    }

    private void accessPublicData() throws IOException {
        try {
            // construct the absolute file path to the app's public's file the same
            // way as the appaccessdata package will.
            String publicFilePath = String.format("/data/data/%s/files/%s", APP_WITH_DATA_PKG,
                    PUBLIC_FILE_NAME);
            DataInputStream inputStream = new DataInputStream(new FileInputStream(publicFilePath));
            int otherAppUid = (int)inputStream.readInt();
            inputStream.close();
        } catch (FileNotFoundException e) {
            fail("Was not able to access own public file: " + e);
        } catch (SecurityException e) {
            fail("Was not able to access own public file: " + e);
        }
    }

    private void writeToPreferences() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_FILE_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREFERENCE_KEY, PREFERENCE_VALUE);
        editor.commit();
        assertEquals(PREFERENCE_VALUE, prefs.getString(PREFERENCE_KEY, null));
    }

    private void writeToDatabase() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = new TestDatabaseOpenHelper(mContext).getWritableDatabase();
            ContentValues values = new ContentValues(1);
            values.put(DB_COLUMN, DB_VALUE);
            assertTrue(db.insert(DB_TABLE_NAME, null, values) != -1);

            cursor = db.query(DB_TABLE_NAME, new String[] {DB_COLUMN},
                    null, null, null, null, null);
            assertEquals(1, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * Check to ensure the private file created in testCreatePrivateData does not exist.
     * Used to check that uninstall of an app deletes the app's data.
     */
    public void testEnsurePrivateDataNotExist() throws IOException {
        assertFalse(getContext().getFileStreamPath(PRIVATE_FILE_NAME).exists());

        assertPreferencesDataDoesNotExist();
        assertDatabaseDataDoesNotExist();
    }

    private void assertPreferencesDataDoesNotExist() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFERENCES_FILE_NAME, 0);
        assertNull(prefs.getString(PREFERENCE_KEY, null));
    }

    private void assertDatabaseDataDoesNotExist() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = new TestDatabaseOpenHelper(mContext).getWritableDatabase();
            cursor = db.query(DB_TABLE_NAME, new String[] {DB_COLUMN},
                    null, null, null, null, null);
            assertEquals(0, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
    }

    private void accessOwnTrafficStats() throws IOException {
        final int ownAppUid = getContext().getApplicationInfo().uid;

        boolean foundOwnDetailedStats = false;
        try {
            BufferedReader qtaguidReader = new BufferedReader(new FileReader("/proc/net/xt_qtaguid/stats"));
            String line;
            while ((line = qtaguidReader.readLine()) != null) {
                String tokens[] = line.split(" ");
                if (tokens.length > 3 && tokens[3].equals(String.valueOf(ownAppUid))) {
                    if (!tokens[2].equals("0x0")) {
                      foundOwnDetailedStats = true;
                    }
                }
            }
            qtaguidReader.close();
        } catch (FileNotFoundException e) {
            fail("Was not able to access qtaguid/stats: " + e);
        }
        assertTrue("Was expecting to find own traffic stats", foundOwnDetailedStats);
    }

    private void createTrafficStatsWithTags() throws IOException {

        // Transfer 1MB of data across an explicitly localhost socket.
        final int byteCount = 1024;
        final int packetCount = 1024;

        final ServerSocket server = new ServerSocket(0);
        new Thread("CreatePrivateDataTest.createTrafficStatsWithTags") {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("localhost", server.getLocalPort());
                    // Make sure that each write()+flush() turns into a packet:
                    // disable Nagle.
                    socket.setTcpNoDelay(true);
                    OutputStream out = socket.getOutputStream();
                    byte[] buf = new byte[byteCount];
                    for (int i = 0; i < packetCount; i++) {
                        TrafficStats.setThreadStatsTag(i % 10);
                        TrafficStats.tagSocket(socket);
                        out.write(buf);
                        out.flush();
                    }
                    out.close();
                    socket.close();
                } catch (IOException e) {
                  assertTrue("io exception" + e, false);
                }
            }
        }.start();

        try {
            Socket socket = server.accept();
            InputStream in = socket.getInputStream();
            byte[] buf = new byte[byteCount];
            int read = 0;
            while (read < byteCount * packetCount) {
                int n = in.read(buf);
                assertTrue("Unexpected EOF", n > 0);
                read += n;
            }
        } finally {
            server.close();
        }

        accessOwnTrafficStats();
    }

    static class TestDatabaseOpenHelper extends SQLiteOpenHelper {

        static final String _ID = "_id";

        public TestDatabaseOpenHelper(Context context) {
            super(context, "test.db", null, 1337);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + DB_TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + DB_COLUMN + " TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_NAME);
            onCreate(db);
        }
    }
}
