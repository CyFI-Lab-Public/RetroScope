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

package com.android.gallery3d.common;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.android.gallery3d.common.Entry.Table;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class FileCache implements Closeable {
    private static final int LRU_CAPACITY = 4;
    private static final int MAX_DELETE_COUNT = 16;

    private static final String TAG = "FileCache";
    private static final String TABLE_NAME = FileEntry.SCHEMA.getTableName();
    private static final String FILE_PREFIX = "download";
    private static final String FILE_POSTFIX = ".tmp";

    private static final String QUERY_WHERE =
            FileEntry.Columns.HASH_CODE + "=? AND " + FileEntry.Columns.CONTENT_URL + "=?";
    private static final String ID_WHERE = FileEntry.Columns.ID + "=?";
    private static final String[] PROJECTION_SIZE_SUM =
            {String.format("sum(%s)", FileEntry.Columns.SIZE)};
    private static final String FREESPACE_PROJECTION[] = {
            FileEntry.Columns.ID, FileEntry.Columns.FILENAME,
            FileEntry.Columns.CONTENT_URL, FileEntry.Columns.SIZE};
    private static final String FREESPACE_ORDER_BY =
            String.format("%s ASC", FileEntry.Columns.LAST_ACCESS);

    private final LruCache<String, CacheEntry> mEntryMap =
            new LruCache<String, CacheEntry>(LRU_CAPACITY);

    private File mRootDir;
    private long mCapacity;
    private boolean mInitialized = false;
    private long mTotalBytes;

    private DatabaseHelper mDbHelper;

    public static final class CacheEntry {
        private long id;
        public String contentUrl;
        public File cacheFile;

        private CacheEntry(long id, String contentUrl, File cacheFile) {
            this.id = id;
            this.contentUrl = contentUrl;
            this.cacheFile = cacheFile;
        }
    }

    public static void deleteFiles(Context context, File rootDir, String dbName) {
        try {
            context.getDatabasePath(dbName).delete();
            File[] files = rootDir.listFiles();
            if (files == null) return;
            for (File file : rootDir.listFiles()) {
                String name = file.getName();
                if (file.isFile() && name.startsWith(FILE_PREFIX)
                        && name.endsWith(FILE_POSTFIX)) file.delete();
            }
        } catch (Throwable t) {
            Log.w(TAG, "cannot reset database", t);
        }
    }

    public FileCache(Context context, File rootDir, String dbName, long capacity) {
        mRootDir = Utils.checkNotNull(rootDir);
        mCapacity = capacity;
        mDbHelper = new DatabaseHelper(context, dbName);
    }

    @Override
    public void close() {
        mDbHelper.close();
    }

    public void store(String downloadUrl, File file) {
        if (!mInitialized) initialize();

        Utils.assertTrue(file.getParentFile().equals(mRootDir));
        FileEntry entry = new FileEntry();
        entry.hashCode = Utils.crc64Long(downloadUrl);
        entry.contentUrl = downloadUrl;
        entry.filename = file.getName();
        entry.size = file.length();
        entry.lastAccess = System.currentTimeMillis();
        if (entry.size >= mCapacity) {
            file.delete();
            throw new IllegalArgumentException("file too large: " + entry.size);
        }
        synchronized (this) {
            FileEntry original = queryDatabase(downloadUrl);
            if (original != null) {
                file.delete();
                entry.filename = original.filename;
                entry.size = original.size;
            } else {
                mTotalBytes += entry.size;
            }
            FileEntry.SCHEMA.insertOrReplace(
                    mDbHelper.getWritableDatabase(), entry);
            if (mTotalBytes > mCapacity) freeSomeSpaceIfNeed(MAX_DELETE_COUNT);
        }
    }

    public CacheEntry lookup(String downloadUrl) {
        if (!mInitialized) initialize();
        CacheEntry entry;
        synchronized (mEntryMap) {
            entry = mEntryMap.get(downloadUrl);
        }

        if (entry != null) {
            synchronized (this) {
                updateLastAccess(entry.id);
            }
            return entry;
        }

        synchronized (this) {
            FileEntry file = queryDatabase(downloadUrl);
            if (file == null) return null;
            entry = new CacheEntry(
                    file.id, downloadUrl, new File(mRootDir, file.filename));
            if (!entry.cacheFile.isFile()) { // file has been removed
                try {
                    mDbHelper.getWritableDatabase().delete(
                            TABLE_NAME, ID_WHERE, new String[] {String.valueOf(file.id)});
                    mTotalBytes -= file.size;
                } catch (Throwable t) {
                    Log.w(TAG, "cannot delete entry: " + file.filename, t);
                }
                return null;
            }
            synchronized (mEntryMap) {
                mEntryMap.put(downloadUrl, entry);
            }
            return entry;
        }
    }

    private FileEntry queryDatabase(String downloadUrl) {
        long hash = Utils.crc64Long(downloadUrl);
        String whereArgs[] = new String[] {String.valueOf(hash), downloadUrl};
        Cursor cursor = mDbHelper.getReadableDatabase().query(TABLE_NAME,
                FileEntry.SCHEMA.getProjection(),
                QUERY_WHERE, whereArgs, null, null, null);
        try {
            if (!cursor.moveToNext()) return null;
            FileEntry entry = new FileEntry();
            FileEntry.SCHEMA.cursorToObject(cursor, entry);
            updateLastAccess(entry.id);
            return entry;
        } finally {
            cursor.close();
        }
    }

    private void updateLastAccess(long id) {
        ContentValues values = new ContentValues();
        values.put(FileEntry.Columns.LAST_ACCESS, System.currentTimeMillis());
        mDbHelper.getWritableDatabase().update(TABLE_NAME,
                values,  ID_WHERE, new String[] {String.valueOf(id)});
    }

    public File createFile() throws IOException {
        return File.createTempFile(FILE_PREFIX, FILE_POSTFIX, mRootDir);
    }

    private synchronized void initialize() {
        if (mInitialized) return;

        if (!mRootDir.isDirectory()) {
            mRootDir.mkdirs();
            if (!mRootDir.isDirectory()) {
                throw new RuntimeException("cannot create: " + mRootDir.getAbsolutePath());
            }
        }

        Cursor cursor = mDbHelper.getReadableDatabase().query(
                TABLE_NAME, PROJECTION_SIZE_SUM,
                null, null, null, null, null);
        try {
            if (cursor.moveToNext()) mTotalBytes = cursor.getLong(0);
        } finally {
            cursor.close();
        }
        if (mTotalBytes > mCapacity) freeSomeSpaceIfNeed(MAX_DELETE_COUNT);

        // Mark initialized when everything above went through. If an exception was thrown,
        // initialize() will be retried later.
        mInitialized = true;
    }

    private void freeSomeSpaceIfNeed(int maxDeleteFileCount) {
        Cursor cursor = mDbHelper.getReadableDatabase().query(
                TABLE_NAME, FREESPACE_PROJECTION,
                null, null, null, null, FREESPACE_ORDER_BY);
        try {
            while (maxDeleteFileCount > 0
                    && mTotalBytes > mCapacity && cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String path = cursor.getString(1);
                String url = cursor.getString(2);
                long size = cursor.getLong(3);

                synchronized (mEntryMap) {
                    // if some one still uses it
                    if (mEntryMap.containsKey(url)) continue;
                }

                --maxDeleteFileCount;
                if (new File(mRootDir, path).delete()) {
                    mTotalBytes -= size;
                    mDbHelper.getWritableDatabase().delete(TABLE_NAME,
                            ID_WHERE, new String[]{String.valueOf(id)});
                } else {
                    Log.w(TAG, "unable to delete file: " + path);
                }
            }
        } finally {
            cursor.close();
        }
    }

    @Table("files")
    private static class FileEntry extends Entry {
        public static final EntrySchema SCHEMA = new EntrySchema(FileEntry.class);

        public interface Columns extends Entry.Columns {
            public static final String HASH_CODE = "hash_code";
            public static final String CONTENT_URL = "content_url";
            public static final String FILENAME = "filename";
            public static final String SIZE = "size";
            public static final String LAST_ACCESS = "last_access";
        }

        @Column(value = Columns.HASH_CODE, indexed = true)
        public long hashCode;

        @Column(Columns.CONTENT_URL)
        public String contentUrl;

        @Column(Columns.FILENAME)
        public String filename;

        @Column(Columns.SIZE)
        public long size;

        @Column(value = Columns.LAST_ACCESS, indexed = true)
        public long lastAccess;

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("hash_code: ").append(hashCode).append(", ")
                    .append("content_url").append(contentUrl).append(", ")
                    .append("last_access").append(lastAccess).append(", ")
                    .append("filename").append(filename).toString();
        }
    }

    private final class DatabaseHelper extends SQLiteOpenHelper {
        public static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context, String dbName) {
            super(context, dbName, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            FileEntry.SCHEMA.createTables(db);

            // delete old files
            for (File file : mRootDir.listFiles()) {
                if (!file.delete()) {
                    Log.w(TAG, "fail to remove: " + file.getAbsolutePath());
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //reset everything
            FileEntry.SCHEMA.dropTables(db);
            onCreate(db);
        }
    }
}
