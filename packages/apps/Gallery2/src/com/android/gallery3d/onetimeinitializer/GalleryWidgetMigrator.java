/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.onetimeinitializer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.gadget.WidgetDatabaseHelper;
import com.android.gallery3d.gadget.WidgetDatabaseHelper.Entry;
import com.android.gallery3d.util.GalleryUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * This one-timer migrates local-album gallery app widgets from old paths from prior releases 
 * to updated paths in the current build version. This migration is needed because of
 * bucket ID (i.e., directory hash) change in JB and JB MR1 (The external storage path has changed
 * from /mnt/sdcard in pre-JB releases, to /storage/sdcard0 in JB, then again
 * to /external/storage/sdcard/0 in JB MR1).
 */
public class GalleryWidgetMigrator {
    private static final String TAG = "GalleryWidgetMigrator";
    private static final String PRE_JB_EXT_PATH = "/mnt/sdcard";
    private static final String JB_EXT_PATH = "/storage/sdcard0";
    private static final String NEW_EXT_PATH =
            Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final int RELATIVE_PATH_START = NEW_EXT_PATH.length();
    private static final String KEY_EXT_PATH = "external_storage_path";

    /**
     * Migrates local-album gallery widgets from prior releases to current release
     * due to bucket ID (i.e., directory hash) change.
     */
    public static void migrateGalleryWidgets(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Migration is only needed when external storage path has changed
        String extPath = prefs.getString(KEY_EXT_PATH, null);
        boolean isDone = NEW_EXT_PATH.equals(extPath);
        if (isDone) return;

        try {
            migrateGalleryWidgetsInternal(context);
            prefs.edit().putString(KEY_EXT_PATH, NEW_EXT_PATH).commit();
        } catch (Throwable t) {
            // exception may be thrown if external storage is not available(?)
            Log.w(TAG, "migrateGalleryWidgets", t);
        }
    }

    private static void migrateGalleryWidgetsInternal(Context context) {
        GalleryApp galleryApp = (GalleryApp) context.getApplicationContext();
        DataManager manager = galleryApp.getDataManager();
        WidgetDatabaseHelper dbHelper = new WidgetDatabaseHelper(context);

        // only need to migrate local-album entries of type TYPE_ALBUM
        List<Entry> entries = dbHelper.getEntries(WidgetDatabaseHelper.TYPE_ALBUM);
        if (entries == null) return;

        // Check each entry's relativePath. If exists, update bucket id using relative
        // path combined with external storage path. Otherwise, iterate through old external
        // storage paths to find the relative path that matches the old bucket id, and then update
        // bucket id and relative path
        HashMap<Integer, Entry> localEntries = new HashMap<Integer, Entry>(entries.size());
        for (Entry entry : entries) {
            Path path = Path.fromString(entry.albumPath);
            MediaSet mediaSet = (MediaSet) manager.getMediaObject(path);
            if (mediaSet instanceof LocalAlbum) {
                if (entry.relativePath != null && entry.relativePath.length() > 0) {
                    // update entry using relative path + external storage path
                    updateEntryUsingRelativePath(entry, dbHelper);
                } else {
                    int bucketId = Integer.parseInt(path.getSuffix());
                    localEntries.put(bucketId, entry);
                }
            }
        }
        if (!localEntries.isEmpty()) migrateLocalEntries(context, localEntries, dbHelper);
    }

    private static void migrateLocalEntries(Context context,
            HashMap<Integer, Entry> entries, WidgetDatabaseHelper dbHelper) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String oldExtPath = prefs.getString(KEY_EXT_PATH, null);
        if (oldExtPath != null) {
            migrateLocalEntries(entries, dbHelper, oldExtPath);
            return;
        }
        // If old external storage path is unknown, it could be either Pre-JB or JB version
        // we need to try both.
        migrateLocalEntries(entries, dbHelper, PRE_JB_EXT_PATH);
        if (!entries.isEmpty() &&
                Build.VERSION.SDK_INT > ApiHelper.VERSION_CODES.JELLY_BEAN) {
            migrateLocalEntries(entries, dbHelper, JB_EXT_PATH);
        }
    }

    private static void migrateLocalEntries(HashMap<Integer, Entry> entries,
             WidgetDatabaseHelper dbHelper, String oldExtPath) {
        File root = Environment.getExternalStorageDirectory();
        // check the DCIM directory first; this should take care of 99% use cases
        updatePath(new File(root, "DCIM"), entries, dbHelper, oldExtPath);
        // check other directories if DCIM doesn't cut it
        if (!entries.isEmpty()) updatePath(root, entries, dbHelper, oldExtPath);
    }
    private static void updatePath(File root, HashMap<Integer, Entry> entries,
            WidgetDatabaseHelper dbHelper, String oldExtStorage) {
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !entries.isEmpty()) {
                    String path = file.getAbsolutePath();
                    String oldPath = oldExtStorage + path.substring(RELATIVE_PATH_START);
                    int oldBucketId = GalleryUtils.getBucketId(oldPath);
                    Entry entry = entries.remove(oldBucketId);
                    if (entry != null) {
                        int newBucketId = GalleryUtils.getBucketId(path);
                        String newAlbumPath = Path.fromString(entry.albumPath)
                                .getParent()
                                .getChild(newBucketId)
                                .toString();
                        Log.d(TAG, "migrate from " + entry.albumPath + " to " + newAlbumPath);
                        entry.albumPath = newAlbumPath;
                        // update entry's relative path
                        entry.relativePath = path.substring(RELATIVE_PATH_START);
                        dbHelper.updateEntry(entry);
                    }
                    updatePath(file, entries, dbHelper, oldExtStorage); // recursion
                }
            }
        }
    }

    private static void updateEntryUsingRelativePath(Entry entry, WidgetDatabaseHelper dbHelper) {
        String newPath = NEW_EXT_PATH + entry.relativePath;
        int newBucketId = GalleryUtils.getBucketId(newPath);
        String newAlbumPath = Path.fromString(entry.albumPath)
                .getParent()
                .getChild(newBucketId)
                .toString();
        entry.albumPath = newAlbumPath;
        dbHelper.updateEntry(entry);
    }
}
