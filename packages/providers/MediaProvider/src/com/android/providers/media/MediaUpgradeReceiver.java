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

package com.android.providers.media;

import java.io.File;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;

/**
 * This will be launched during system boot, after the core system has
 * been brought up but before any non-persistent processes have been
 * started.  It is launched in a special state, with no content provider
 * or custom application class associated with the process running.
 *
 * It's job is to prime the contacts database. Either create it
 * if it doesn't exist, or open it and force any necessary upgrades.
 * All of this heavy lifting happens before the boot animation ends.
 */
public class MediaUpgradeReceiver extends BroadcastReceiver {
    static final String TAG = "MediaUpgradeReceiver";
    static final String PREF_DB_VERSION = "db_version";

    @Override
    public void onReceive(Context context, Intent intent) {
        // We are now running with the system up, but no apps started,
        // so can do whatever cleanup after an upgrade that we want.

        // Lookup the last known database version
        SharedPreferences prefs = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        int prefVersion = prefs.getInt(PREF_DB_VERSION, 0);
        int dbVersion = MediaProvider.getDatabaseVersion(context);
        if (prefVersion == dbVersion) {
            return;
        }
        prefs.edit().putInt(PREF_DB_VERSION, dbVersion).commit();

        try {
            File dbDir = context.getDatabasePath("foo").getParentFile();
            String[] files = dbDir.list();
            if (files == null) return;
            for (int i=0; i<files.length; i++) {
                String file = files[i];
                if (MediaProvider.isMediaDatabaseName(file)) {
                    try {
                        ActivityManagerNative.getDefault().showBootMessage(
                                context.getText(R.string.upgrade_msg), true);
                    } catch (RemoteException e) {
                    }

                    long startTime = System.currentTimeMillis();
                    Slog.i(TAG, "---> Start upgrade of media database " + file);
                    SQLiteDatabase db = null;
                    try {
                        MediaProvider.DatabaseHelper helper = new MediaProvider.DatabaseHelper(
                                context, file, MediaProvider.isInternalMediaDatabaseName(file),
                                false, null);
                        db = helper.getWritableDatabase();
                    } catch (Throwable t) {
                        Log.wtf(TAG, "Error during upgrade of media db " + file, t);
                    } finally {
                        if (db != null) {
                            db.close();
                        }
                    }
                    Slog.i(TAG, "<--- Finished upgrade of media database " + file
                            + " in " + (System.currentTimeMillis()-startTime) + "ms");
                }
            }
        } catch (Throwable t) {
            // Something has gone terribly wrong.
            Log.wtf(TAG, "Error during upgrade attempt.", t);
        }
    }
}
