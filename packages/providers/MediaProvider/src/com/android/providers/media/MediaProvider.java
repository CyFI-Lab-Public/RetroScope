/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.providers.media;

import static android.Manifest.permission.ACCESS_CACHE_FILESYSTEM;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static android.os.ParcelFileDescriptor.MODE_WRITE_ONLY;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaFile;
import android.media.MediaScanner;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.media.MiniThumbFile;
import android.mtp.MtpConstants;
import android.mtp.MtpStorage;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import libcore.io.ErrnoException;
import libcore.io.IoUtils;
import libcore.io.Libcore;
import libcore.io.OsConstants;
import libcore.io.StructStat;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Stack;

/**
 * Media content provider. See {@link android.provider.MediaStore} for details.
 * Separate databases are kept for each external storage card we see (using the
 * card's ID as an index).  The content visible at content://media/external/...
 * changes with the card.
 */
public class MediaProvider extends ContentProvider {
    private static final Uri MEDIA_URI = Uri.parse("content://media");
    private static final Uri ALBUMART_URI = Uri.parse("content://media/external/audio/albumart");
    private static final int ALBUM_THUMB = 1;
    private static final int IMAGE_THUMB = 2;

    private static final HashMap<String, String> sArtistAlbumsMap = new HashMap<String, String>();
    private static final HashMap<String, String> sFolderArtMap = new HashMap<String, String>();

    /** Resolved canonical path to external storage. */
    private static final String sExternalPath;
    /** Resolved canonical path to cache storage. */
    private static final String sCachePath;
    /** Resolved canonical path to legacy storage. */
    private static final String sLegacyPath;

    static {
        try {
            sExternalPath = Environment.getExternalStorageDirectory().getCanonicalPath();
            sCachePath = Environment.getDownloadCacheDirectory().getCanonicalPath();
            sLegacyPath = Environment.getLegacyExternalStorageDirectory().getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException("Unable to resolve canonical paths", e);
        }
    }

    private StorageManager mStorageManager;

    // In memory cache of path<->id mappings, to speed up inserts during media scan
    HashMap<String, Long> mDirectoryCache = new HashMap<String, Long>();

    // A HashSet of paths that are pending creation of album art thumbnails.
    private HashSet mPendingThumbs = new HashSet();

    // A Stack of outstanding thumbnail requests.
    private Stack mThumbRequestStack = new Stack();

    // The lock of mMediaThumbQueue protects both mMediaThumbQueue and mCurrentThumbRequest.
    private MediaThumbRequest mCurrentThumbRequest = null;
    private PriorityQueue<MediaThumbRequest> mMediaThumbQueue =
            new PriorityQueue<MediaThumbRequest>(MediaThumbRequest.PRIORITY_NORMAL,
            MediaThumbRequest.getComparator());

    private boolean mCaseInsensitivePaths;
    private static String[] mExternalStoragePaths;

    // For compatibility with the approximately 0 apps that used mediaprovider search in
    // releases 1.0, 1.1 or 1.5
    private String[] mSearchColsLegacy = new String[] {
            android.provider.BaseColumns._ID,
            MediaStore.Audio.Media.MIME_TYPE,
            "(CASE WHEN grouporder=1 THEN " + R.drawable.ic_search_category_music_artist +
            " ELSE CASE WHEN grouporder=2 THEN " + R.drawable.ic_search_category_music_album +
            " ELSE " + R.drawable.ic_search_category_music_song + " END END" +
            ") AS " + SearchManager.SUGGEST_COLUMN_ICON_1,
            "0 AS " + SearchManager.SUGGEST_COLUMN_ICON_2,
            "text1 AS " + SearchManager.SUGGEST_COLUMN_TEXT_1,
            "text1 AS " + SearchManager.SUGGEST_COLUMN_QUERY,
            "CASE when grouporder=1 THEN data1 ELSE artist END AS data1",
            "CASE when grouporder=1 THEN data2 ELSE " +
                "CASE WHEN grouporder=2 THEN NULL ELSE album END END AS data2",
            "match as ar",
            SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            "grouporder",
            "NULL AS itemorder" // We should be sorting by the artist/album/title keys, but that
                                // column is not available here, and the list is already sorted.
    };
    private String[] mSearchColsFancy = new String[] {
            android.provider.BaseColumns._ID,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Media.TITLE,
            "data1",
            "data2",
    };
    // If this array gets changed, please update the constant below to point to the correct item.
    private String[] mSearchColsBasic = new String[] {
            android.provider.BaseColumns._ID,
            MediaStore.Audio.Media.MIME_TYPE,
            "(CASE WHEN grouporder=1 THEN " + R.drawable.ic_search_category_music_artist +
            " ELSE CASE WHEN grouporder=2 THEN " + R.drawable.ic_search_category_music_album +
            " ELSE " + R.drawable.ic_search_category_music_song + " END END" +
            ") AS " + SearchManager.SUGGEST_COLUMN_ICON_1,
            "text1 AS " + SearchManager.SUGGEST_COLUMN_TEXT_1,
            "text1 AS " + SearchManager.SUGGEST_COLUMN_QUERY,
            "(CASE WHEN grouporder=1 THEN '%1'" +  // %1 gets replaced with localized string.
            " ELSE CASE WHEN grouporder=3 THEN artist || ' - ' || album" +
            " ELSE CASE WHEN text2!='" + MediaStore.UNKNOWN_STRING + "' THEN text2" +
            " ELSE NULL END END END) AS " + SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };
    // Position of the TEXT_2 item in the above array.
    private final int SEARCH_COLUMN_BASIC_TEXT2 = 5;

    private static final String[] sMediaTableColumns = new String[] {
            FileColumns._ID,
            FileColumns.MEDIA_TYPE,
    };

    private static final String[] sIdOnlyColumn = new String[] {
        FileColumns._ID
    };

    private static final String[] sDataOnlyColumn = new String[] {
        FileColumns.DATA
    };

    private static final String[] sMediaTypeDataId = new String[] {
        FileColumns.MEDIA_TYPE,
        FileColumns.DATA,
        FileColumns._ID
    };

    private static final String[] sPlaylistIdPlayOrder = new String[] {
        Playlists.Members.PLAYLIST_ID,
        Playlists.Members.PLAY_ORDER
    };

    private Uri mAlbumArtBaseUri = Uri.parse("content://media/external/audio/albumart");

    private static final String CANONICAL = "canonical";

    private BroadcastReceiver mUnmountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)) {
                StorageVolume storage = (StorageVolume)intent.getParcelableExtra(
                        StorageVolume.EXTRA_STORAGE_VOLUME);
                // If primary external storage is ejected, then remove the external volume
                // notify all cursors backed by data on that volume.
                if (storage.getPath().equals(mExternalStoragePaths[0])) {
                    detachVolume(Uri.parse("content://media/external"));
                    sFolderArtMap.clear();
                    MiniThumbFile.reset();
                } else {
                    // If secondary external storage is ejected, then we delete all database
                    // entries for that storage from the files table.
                    synchronized (mDatabases) {
                        DatabaseHelper database = mDatabases.get(EXTERNAL_VOLUME);
                        Uri uri = Uri.parse("file://" + storage.getPath());
                        if (database != null) {
                            try {
                                // Send media scanner started and stopped broadcasts for apps that rely
                                // on these Intents for coarse grained media database notifications.
                                context.sendBroadcast(
                                        new Intent(Intent.ACTION_MEDIA_SCANNER_STARTED, uri));

                                // don't send objectRemoved events - MTP be sending StorageRemoved anyway
                                mDisableMtpObjectCallbacks = true;
                                Log.d(TAG, "deleting all entries for storage " + storage);
                                SQLiteDatabase db = database.getWritableDatabase();
                                // First clear the file path to disable the _DELETE_FILE database hook.
                                // We do this to avoid deleting files if the volume is remounted while
                                // we are still processing the unmount event.
                                ContentValues values = new ContentValues();
                                values.putNull(Files.FileColumns.DATA);
                                String where = FileColumns.STORAGE_ID + "=?";
                                String[] whereArgs = new String[] { Integer.toString(storage.getStorageId()) };
                                database.mNumUpdates++;
                                db.update("files", values, where, whereArgs);
                                // now delete the records
                                database.mNumDeletes++;
                                int numpurged = db.delete("files", where, whereArgs);
                                logToDb(db, "removed " + numpurged +
                                        " rows for ejected filesystem " + storage.getPath());
                                // notify on media Uris as well as the files Uri
                                context.getContentResolver().notifyChange(
                                        Audio.Media.getContentUri(EXTERNAL_VOLUME), null);
                                context.getContentResolver().notifyChange(
                                        Images.Media.getContentUri(EXTERNAL_VOLUME), null);
                                context.getContentResolver().notifyChange(
                                        Video.Media.getContentUri(EXTERNAL_VOLUME), null);
                                context.getContentResolver().notifyChange(
                                        Files.getContentUri(EXTERNAL_VOLUME), null);
                            } catch (Exception e) {
                                Log.e(TAG, "exception deleting storage entries", e);
                            } finally {
                                context.sendBroadcast(
                                        new Intent(Intent.ACTION_MEDIA_SCANNER_FINISHED, uri));
                                mDisableMtpObjectCallbacks = false;
                            }
                        }
                    }
                }
            }
        }
    };

    // set to disable sending events when the operation originates from MTP
    private boolean mDisableMtpObjectCallbacks;

    private final SQLiteDatabase.CustomFunction mObjectRemovedCallback =
                new SQLiteDatabase.CustomFunction() {
        public void callback(String[] args) {
            // We could remove only the deleted entry from the cache, but that
            // requires the path, which we don't have here, so instead we just
            // clear the entire cache.
            // TODO: include the path in the callback and only remove the affected
            // entry from the cache
            mDirectoryCache.clear();
            // do nothing if the operation originated from MTP
            if (mDisableMtpObjectCallbacks) return;

            Log.d(TAG, "object removed " + args[0]);
            IMtpService mtpService = mMtpService;
            if (mtpService != null) {
                try {
                    sendObjectRemoved(Integer.parseInt(args[0]));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "NumberFormatException in mObjectRemovedCallback", e);
                }
            }
        }
    };

    /**
     * Wrapper class for a specific database (associated with one particular
     * external card, or with internal storage).  Can open the actual database
     * on demand, create and upgrade the schema, etc.
     */
    static final class DatabaseHelper extends SQLiteOpenHelper {
        final Context mContext;
        final String mName;
        final boolean mInternal;  // True if this is the internal database
        final boolean mEarlyUpgrade;
        final SQLiteDatabase.CustomFunction mObjectRemovedCallback;
        boolean mUpgradeAttempted; // Used for upgrade error handling
        int mNumQueries;
        int mNumUpdates;
        int mNumInserts;
        int mNumDeletes;
        long mScanStartTime;
        long mScanStopTime;

        // In memory caches of artist and album data.
        HashMap<String, Long> mArtistCache = new HashMap<String, Long>();
        HashMap<String, Long> mAlbumCache = new HashMap<String, Long>();

        public DatabaseHelper(Context context, String name, boolean internal,
                boolean earlyUpgrade,
                SQLiteDatabase.CustomFunction objectRemovedCallback) {
            super(context, name, null, getDatabaseVersion(context));
            mContext = context;
            mName = name;
            mInternal = internal;
            mEarlyUpgrade = earlyUpgrade;
            mObjectRemovedCallback = objectRemovedCallback;
            setWriteAheadLoggingEnabled(true);
        }

        /**
         * Creates database the first time we try to open it.
         */
        @Override
        public void onCreate(final SQLiteDatabase db) {
            updateDatabase(mContext, db, mInternal, 0, getDatabaseVersion(mContext));
        }

        /**
         * Updates the database format when a new content provider is used
         * with an older database format.
         */
        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldV, final int newV) {
            mUpgradeAttempted = true;
            updateDatabase(mContext, db, mInternal, oldV, newV);
        }

        @Override
        public synchronized SQLiteDatabase getWritableDatabase() {
            SQLiteDatabase result = null;
            mUpgradeAttempted = false;
            try {
                result = super.getWritableDatabase();
            } catch (Exception e) {
                if (!mUpgradeAttempted) {
                    Log.e(TAG, "failed to open database " + mName, e);
                    return null;
                }
            }

            // If we failed to open the database during an upgrade, delete the file and try again.
            // This will result in the creation of a fresh database, which will be repopulated
            // when the media scanner runs.
            if (result == null && mUpgradeAttempted) {
                mContext.deleteDatabase(mName);
                result = super.getWritableDatabase();
            }
            return result;
        }

        /**
         * For devices that have removable storage, we support keeping multiple databases
         * to allow users to switch between a number of cards.
         * On such devices, touch this particular database and garbage collect old databases.
         * An LRU cache system is used to clean up databases for old external
         * storage volumes.
         */
        @Override
        public void onOpen(SQLiteDatabase db) {

            if (mInternal) return;  // The internal database is kept separately.

            if (mEarlyUpgrade) return; // Doing early upgrade.

            if (mObjectRemovedCallback != null) {
                db.addCustomFunction("_OBJECT_REMOVED", 1, mObjectRemovedCallback);
            }

            // the code below is only needed on devices with removable storage
            if (!Environment.isExternalStorageRemovable()) return;

            // touch the database file to show it is most recently used
            File file = new File(db.getPath());
            long now = System.currentTimeMillis();
            file.setLastModified(now);

            // delete least recently used databases if we are over the limit
            String[] databases = mContext.databaseList();
            int count = databases.length;
            int limit = MAX_EXTERNAL_DATABASES;

            // delete external databases that have not been used in the past two months
            long twoMonthsAgo = now - OBSOLETE_DATABASE_DB;
            for (int i = 0; i < databases.length; i++) {
                File other = mContext.getDatabasePath(databases[i]);
                if (INTERNAL_DATABASE_NAME.equals(databases[i]) || file.equals(other)) {
                    databases[i] = null;
                    count--;
                    if (file.equals(other)) {
                        // reduce limit to account for the existence of the database we
                        // are about to open, which we removed from the list.
                        limit--;
                    }
                } else {
                    long time = other.lastModified();
                    if (time < twoMonthsAgo) {
                        if (LOCAL_LOGV) Log.v(TAG, "Deleting old database " + databases[i]);
                        mContext.deleteDatabase(databases[i]);
                        databases[i] = null;
                        count--;
                    }
                }
            }

            // delete least recently used databases until
            // we are no longer over the limit
            while (count > limit) {
                int lruIndex = -1;
                long lruTime = 0;

                for (int i = 0; i < databases.length; i++) {
                    if (databases[i] != null) {
                        long time = mContext.getDatabasePath(databases[i]).lastModified();
                        if (lruTime == 0 || time < lruTime) {
                            lruIndex = i;
                            lruTime = time;
                        }
                    }
                }

                // delete least recently used database
                if (lruIndex != -1) {
                    if (LOCAL_LOGV) Log.v(TAG, "Deleting old database " + databases[lruIndex]);
                    mContext.deleteDatabase(databases[lruIndex]);
                    databases[lruIndex] = null;
                    count--;
                }
            }
        }
    }

    // synchronize on mMtpServiceConnection when accessing mMtpService
    private IMtpService mMtpService;

    private final ServiceConnection mMtpServiceConnection = new ServiceConnection() {
         public void onServiceConnected(ComponentName className, android.os.IBinder service) {
            synchronized (this) {
                mMtpService = IMtpService.Stub.asInterface(service);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            synchronized (this) {
                mMtpService = null;
            }
        }
    };

    private static final String[] sDefaultFolderNames = {
        Environment.DIRECTORY_MUSIC,
        Environment.DIRECTORY_PODCASTS,
        Environment.DIRECTORY_RINGTONES,
        Environment.DIRECTORY_ALARMS,
        Environment.DIRECTORY_NOTIFICATIONS,
        Environment.DIRECTORY_PICTURES,
        Environment.DIRECTORY_MOVIES,
        Environment.DIRECTORY_DOWNLOADS,
        Environment.DIRECTORY_DCIM,
    };

    // creates default folders (Music, Downloads, etc)
    private void createDefaultFolders(DatabaseHelper helper, SQLiteDatabase db) {
        // Use a SharedPreference to ensure we only do this once.
        // We don't want to annoy the user by recreating the directories
        // after she has deleted them.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (prefs.getInt("created_default_folders", 0) == 0) {
            for (String folderName : sDefaultFolderNames) {
                File file = Environment.getExternalStoragePublicDirectory(folderName);
                if (!file.exists()) {
                    file.mkdirs();
                    insertDirectory(helper, db, file.getAbsolutePath());
                }
            }

            SharedPreferences.Editor e = prefs.edit();
            e.clear();
            e.putInt("created_default_folders", 1);
            e.commit();
        }
    }

    public static int getDatabaseVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("couldn't get version code for " + context);
        }
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();

        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

        sArtistAlbumsMap.put(MediaStore.Audio.Albums._ID, "audio.album_id AS " +
                MediaStore.Audio.Albums._ID);
        sArtistAlbumsMap.put(MediaStore.Audio.Albums.ALBUM, "album");
        sArtistAlbumsMap.put(MediaStore.Audio.Albums.ALBUM_KEY, "album_key");
        sArtistAlbumsMap.put(MediaStore.Audio.Albums.FIRST_YEAR, "MIN(year) AS " +
                MediaStore.Audio.Albums.FIRST_YEAR);
        sArtistAlbumsMap.put(MediaStore.Audio.Albums.LAST_YEAR, "MAX(year) AS " +
                MediaStore.Audio.Albums.LAST_YEAR);
        sArtistAlbumsMap.put(MediaStore.Audio.Media.ARTIST, "artist");
        sArtistAlbumsMap.put(MediaStore.Audio.Media.ARTIST_ID, "artist");
        sArtistAlbumsMap.put(MediaStore.Audio.Media.ARTIST_KEY, "artist_key");
        sArtistAlbumsMap.put(MediaStore.Audio.Albums.NUMBER_OF_SONGS, "count(*) AS " +
                MediaStore.Audio.Albums.NUMBER_OF_SONGS);
        sArtistAlbumsMap.put(MediaStore.Audio.Albums.ALBUM_ART, "album_art._data AS " +
                MediaStore.Audio.Albums.ALBUM_ART);

        mSearchColsBasic[SEARCH_COLUMN_BASIC_TEXT2] =
                mSearchColsBasic[SEARCH_COLUMN_BASIC_TEXT2].replaceAll(
                        "%1", context.getString(R.string.artist_label));
        mDatabases = new HashMap<String, DatabaseHelper>();
        attachVolume(INTERNAL_VOLUME);

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
        iFilter.addDataScheme("file");
        context.registerReceiver(mUnmountReceiver, iFilter);

        StorageManager storageManager =
                (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        mExternalStoragePaths = storageManager.getVolumePaths();

        // open external database if external storage is mounted
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            attachVolume(EXTERNAL_VOLUME);
        }

        HandlerThread ht = new HandlerThread("thumbs thread", Process.THREAD_PRIORITY_BACKGROUND);
        ht.start();
        mThumbHandler = new Handler(ht.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == IMAGE_THUMB) {
                    synchronized (mMediaThumbQueue) {
                        mCurrentThumbRequest = mMediaThumbQueue.poll();
                    }
                    if (mCurrentThumbRequest == null) {
                        Log.w(TAG, "Have message but no request?");
                    } else {
                        try {
                            File origFile = new File(mCurrentThumbRequest.mPath);
                            if (origFile.exists() && origFile.length() > 0) {
                                mCurrentThumbRequest.execute();
                            } else {
                                // original file hasn't been stored yet
                                synchronized (mMediaThumbQueue) {
                                    Log.w(TAG, "original file hasn't been stored yet: " + mCurrentThumbRequest.mPath);
                                }
                            }
                        } catch (IOException ex) {
                            Log.w(TAG, ex);
                        } catch (UnsupportedOperationException ex) {
                            // This could happen if we unplug the sd card during insert/update/delete
                            // See getDatabaseForUri.
                            Log.w(TAG, ex);
                        } catch (OutOfMemoryError err) {
                            /*
                             * Note: Catching Errors is in most cases considered
                             * bad practice. However, in this case it is
                             * motivated by the fact that corrupt or very large
                             * images may cause a huge allocation to be
                             * requested and denied. The bitmap handling API in
                             * Android offers no other way to guard against
                             * these problems than by catching OutOfMemoryError.
                             */
                            Log.w(TAG, err);
                        } finally {
                            synchronized (mCurrentThumbRequest) {
                                mCurrentThumbRequest.mState = MediaThumbRequest.State.DONE;
                                mCurrentThumbRequest.notifyAll();
                            }
                        }
                    }
                } else if (msg.what == ALBUM_THUMB) {
                    ThumbData d;
                    synchronized (mThumbRequestStack) {
                        d = (ThumbData)mThumbRequestStack.pop();
                    }

                    IoUtils.closeQuietly(makeThumbInternal(d));
                    synchronized (mPendingThumbs) {
                        mPendingThumbs.remove(d.path);
                    }
                }
            }
        };

        return true;
    }

    private static final String TABLE_FILES = "files";
    private static final String TABLE_ALBUM_ART = "album_art";
    private static final String TABLE_THUMBNAILS = "thumbnails";
    private static final String TABLE_VIDEO_THUMBNAILS = "videothumbnails";

    private static final String IMAGE_COLUMNS =
                        "_data,_size,_display_name,mime_type,title,date_added," +
                        "date_modified,description,picasa_id,isprivate,latitude,longitude," +
                        "datetaken,orientation,mini_thumb_magic,bucket_id,bucket_display_name," +
                        "width,height";

    private static final String IMAGE_COLUMNSv407 =
                        "_data,_size,_display_name,mime_type,title,date_added," +
                        "date_modified,description,picasa_id,isprivate,latitude,longitude," +
                        "datetaken,orientation,mini_thumb_magic,bucket_id,bucket_display_name";

    private static final String AUDIO_COLUMNSv99 =
                        "_data,_display_name,_size,mime_type,date_added," +
                        "date_modified,title,title_key,duration,artist_id,composer,album_id," +
                        "track,year,is_ringtone,is_music,is_alarm,is_notification,is_podcast," +
                        "bookmark";

    private static final String AUDIO_COLUMNSv100 =
                        "_data,_display_name,_size,mime_type,date_added," +
                        "date_modified,title,title_key,duration,artist_id,composer,album_id," +
                        "track,year,is_ringtone,is_music,is_alarm,is_notification,is_podcast," +
                        "bookmark,album_artist";

    private static final String AUDIO_COLUMNSv405 =
                        "_data,_display_name,_size,mime_type,date_added,is_drm," +
                        "date_modified,title,title_key,duration,artist_id,composer,album_id," +
                        "track,year,is_ringtone,is_music,is_alarm,is_notification,is_podcast," +
                        "bookmark,album_artist";

    private static final String VIDEO_COLUMNS =
                        "_data,_display_name,_size,mime_type,date_added,date_modified," +
                        "title,duration,artist,album,resolution,description,isprivate,tags," +
                        "category,language,mini_thumb_data,latitude,longitude,datetaken," +
                        "mini_thumb_magic,bucket_id,bucket_display_name,bookmark,width," +
                        "height";

    private static final String VIDEO_COLUMNSv407 =
                        "_data,_display_name,_size,mime_type,date_added,date_modified," +
                        "title,duration,artist,album,resolution,description,isprivate,tags," +
                        "category,language,mini_thumb_data,latitude,longitude,datetaken," +
                        "mini_thumb_magic,bucket_id,bucket_display_name, bookmark";

    private static final String PLAYLIST_COLUMNS = "_data,name,date_added,date_modified";

    /**
     * This method takes care of updating all the tables in the database to the
     * current version, creating them if necessary.
     * This method can only update databases at schema 63 or higher, which was
     * created August 1, 2008. Older database will be cleared and recreated.
     * @param db Database
     * @param internal True if this is the internal media database
     */
    private static void updateDatabase(Context context, SQLiteDatabase db, boolean internal,
            int fromVersion, int toVersion) {

        // sanity checks
        int dbversion = getDatabaseVersion(context);
        if (toVersion != dbversion) {
            Log.e(TAG, "Illegal update request. Got " + toVersion + ", expected " + dbversion);
            throw new IllegalArgumentException();
        } else if (fromVersion > toVersion) {
            Log.e(TAG, "Illegal update request: can't downgrade from " + fromVersion +
                    " to " + toVersion + ". Did you forget to wipe data?");
            throw new IllegalArgumentException();
        }
        long startTime = SystemClock.currentTimeMicro();

        // Revisions 84-86 were a failed attempt at supporting the "album artist" id3 tag.
        // We can't downgrade from those revisions, so start over.
        // (the initial change to do this was wrong, so now we actually need to start over
        // if the database version is 84-89)
        // Post-gingerbread, revisions 91-94 were broken in a way that is not easy to repair.
        // However version 91 was reused in a divergent development path for gingerbread,
        // so we need to support upgrades from 91.
        // Therefore we will only force a reset for versions 92 - 94.
        if (fromVersion < 63 || (fromVersion >= 84 && fromVersion <= 89) ||
                    (fromVersion >= 92 && fromVersion <= 94)) {
            // Drop everything and start over.
            Log.i(TAG, "Upgrading media database from version " +
                    fromVersion + " to " + toVersion + ", which will destroy all old data");
            fromVersion = 63;
            db.execSQL("DROP TABLE IF EXISTS images");
            db.execSQL("DROP TRIGGER IF EXISTS images_cleanup");
            db.execSQL("DROP TABLE IF EXISTS thumbnails");
            db.execSQL("DROP TRIGGER IF EXISTS thumbnails_cleanup");
            db.execSQL("DROP TABLE IF EXISTS audio_meta");
            db.execSQL("DROP TABLE IF EXISTS artists");
            db.execSQL("DROP TABLE IF EXISTS albums");
            db.execSQL("DROP TABLE IF EXISTS album_art");
            db.execSQL("DROP VIEW IF EXISTS artist_info");
            db.execSQL("DROP VIEW IF EXISTS album_info");
            db.execSQL("DROP VIEW IF EXISTS artists_albums_map");
            db.execSQL("DROP TRIGGER IF EXISTS audio_meta_cleanup");
            db.execSQL("DROP TABLE IF EXISTS audio_genres");
            db.execSQL("DROP TABLE IF EXISTS audio_genres_map");
            db.execSQL("DROP TRIGGER IF EXISTS audio_genres_cleanup");
            db.execSQL("DROP TABLE IF EXISTS audio_playlists");
            db.execSQL("DROP TABLE IF EXISTS audio_playlists_map");
            db.execSQL("DROP TRIGGER IF EXISTS audio_playlists_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS albumart_cleanup1");
            db.execSQL("DROP TRIGGER IF EXISTS albumart_cleanup2");
            db.execSQL("DROP TABLE IF EXISTS video");
            db.execSQL("DROP TRIGGER IF EXISTS video_cleanup");
            db.execSQL("DROP TABLE IF EXISTS objects");
            db.execSQL("DROP TRIGGER IF EXISTS images_objects_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS audio_objects_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS video_objects_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS playlists_objects_cleanup");

            db.execSQL("CREATE TABLE IF NOT EXISTS images (" +
                    "_id INTEGER PRIMARY KEY," +
                    "_data TEXT," +
                    "_size INTEGER," +
                    "_display_name TEXT," +
                    "mime_type TEXT," +
                    "title TEXT," +
                    "date_added INTEGER," +
                    "date_modified INTEGER," +
                    "description TEXT," +
                    "picasa_id TEXT," +
                    "isprivate INTEGER," +
                    "latitude DOUBLE," +
                    "longitude DOUBLE," +
                    "datetaken INTEGER," +
                    "orientation INTEGER," +
                    "mini_thumb_magic INTEGER," +
                    "bucket_id TEXT," +
                    "bucket_display_name TEXT" +
                   ");");

            db.execSQL("CREATE INDEX IF NOT EXISTS mini_thumb_magic_index on images(mini_thumb_magic);");

            db.execSQL("CREATE TRIGGER IF NOT EXISTS images_cleanup DELETE ON images " +
                    "BEGIN " +
                        "DELETE FROM thumbnails WHERE image_id = old._id;" +
                        "SELECT _DELETE_FILE(old._data);" +
                    "END");

            // create image thumbnail table
            db.execSQL("CREATE TABLE IF NOT EXISTS thumbnails (" +
                       "_id INTEGER PRIMARY KEY," +
                       "_data TEXT," +
                       "image_id INTEGER," +
                       "kind INTEGER," +
                       "width INTEGER," +
                       "height INTEGER" +
                       ");");

            db.execSQL("CREATE INDEX IF NOT EXISTS image_id_index on thumbnails(image_id);");

            db.execSQL("CREATE TRIGGER IF NOT EXISTS thumbnails_cleanup DELETE ON thumbnails " +
                    "BEGIN " +
                        "SELECT _DELETE_FILE(old._data);" +
                    "END");

            // Contains meta data about audio files
            db.execSQL("CREATE TABLE IF NOT EXISTS audio_meta (" +
                       "_id INTEGER PRIMARY KEY," +
                       "_data TEXT UNIQUE NOT NULL," +
                       "_display_name TEXT," +
                       "_size INTEGER," +
                       "mime_type TEXT," +
                       "date_added INTEGER," +
                       "date_modified INTEGER," +
                       "title TEXT NOT NULL," +
                       "title_key TEXT NOT NULL," +
                       "duration INTEGER," +
                       "artist_id INTEGER," +
                       "composer TEXT," +
                       "album_id INTEGER," +
                       "track INTEGER," +    // track is an integer to allow proper sorting
                       "year INTEGER CHECK(year!=0)," +
                       "is_ringtone INTEGER," +
                       "is_music INTEGER," +
                       "is_alarm INTEGER," +
                       "is_notification INTEGER" +
                       ");");

            // Contains a sort/group "key" and the preferred display name for artists
            db.execSQL("CREATE TABLE IF NOT EXISTS artists (" +
                        "artist_id INTEGER PRIMARY KEY," +
                        "artist_key TEXT NOT NULL UNIQUE," +
                        "artist TEXT NOT NULL" +
                       ");");

            // Contains a sort/group "key" and the preferred display name for albums
            db.execSQL("CREATE TABLE IF NOT EXISTS albums (" +
                        "album_id INTEGER PRIMARY KEY," +
                        "album_key TEXT NOT NULL UNIQUE," +
                        "album TEXT NOT NULL" +
                       ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS album_art (" +
                    "album_id INTEGER PRIMARY KEY," +
                    "_data TEXT" +
                   ");");

            recreateAudioView(db);


            // Provides some extra info about artists, like the number of tracks
            // and albums for this artist
            db.execSQL("CREATE VIEW IF NOT EXISTS artist_info AS " +
                        "SELECT artist_id AS _id, artist, artist_key, " +
                        "COUNT(DISTINCT album) AS number_of_albums, " +
                        "COUNT(*) AS number_of_tracks FROM audio WHERE is_music=1 "+
                        "GROUP BY artist_key;");

            // Provides extra info albums, such as the number of tracks
            db.execSQL("CREATE VIEW IF NOT EXISTS album_info AS " +
                    "SELECT audio.album_id AS _id, album, album_key, " +
                    "MIN(year) AS minyear, " +
                    "MAX(year) AS maxyear, artist, artist_id, artist_key, " +
                    "count(*) AS " + MediaStore.Audio.Albums.NUMBER_OF_SONGS +
                    ",album_art._data AS album_art" +
                    " FROM audio LEFT OUTER JOIN album_art ON audio.album_id=album_art.album_id" +
                    " WHERE is_music=1 GROUP BY audio.album_id;");

            // For a given artist_id, provides the album_id for albums on
            // which the artist appears.
            db.execSQL("CREATE VIEW IF NOT EXISTS artists_albums_map AS " +
                    "SELECT DISTINCT artist_id, album_id FROM audio_meta;");

            /*
             * Only external media volumes can handle genres, playlists, etc.
             */
            if (!internal) {
                // Cleans up when an audio file is deleted
                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_meta_cleanup DELETE ON audio_meta " +
                           "BEGIN " +
                               "DELETE FROM audio_genres_map WHERE audio_id = old._id;" +
                               "DELETE FROM audio_playlists_map WHERE audio_id = old._id;" +
                           "END");

                // Contains audio genre definitions
                db.execSQL("CREATE TABLE IF NOT EXISTS audio_genres (" +
                           "_id INTEGER PRIMARY KEY," +
                           "name TEXT NOT NULL" +
                           ");");

                // Contains mappings between audio genres and audio files
                db.execSQL("CREATE TABLE IF NOT EXISTS audio_genres_map (" +
                           "_id INTEGER PRIMARY KEY," +
                           "audio_id INTEGER NOT NULL," +
                           "genre_id INTEGER NOT NULL" +
                           ");");

                // Cleans up when an audio genre is delete
                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_genres_cleanup DELETE ON audio_genres " +
                           "BEGIN " +
                               "DELETE FROM audio_genres_map WHERE genre_id = old._id;" +
                           "END");

                // Contains audio playlist definitions
                db.execSQL("CREATE TABLE IF NOT EXISTS audio_playlists (" +
                           "_id INTEGER PRIMARY KEY," +
                           "_data TEXT," +  // _data is path for file based playlists, or null
                           "name TEXT NOT NULL," +
                           "date_added INTEGER," +
                           "date_modified INTEGER" +
                           ");");

                // Contains mappings between audio playlists and audio files
                db.execSQL("CREATE TABLE IF NOT EXISTS audio_playlists_map (" +
                           "_id INTEGER PRIMARY KEY," +
                           "audio_id INTEGER NOT NULL," +
                           "playlist_id INTEGER NOT NULL," +
                           "play_order INTEGER NOT NULL" +
                           ");");

                // Cleans up when an audio playlist is deleted
                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_playlists_cleanup DELETE ON audio_playlists " +
                           "BEGIN " +
                               "DELETE FROM audio_playlists_map WHERE playlist_id = old._id;" +
                               "SELECT _DELETE_FILE(old._data);" +
                           "END");

                // Cleans up album_art table entry when an album is deleted
                db.execSQL("CREATE TRIGGER IF NOT EXISTS albumart_cleanup1 DELETE ON albums " +
                        "BEGIN " +
                            "DELETE FROM album_art WHERE album_id = old.album_id;" +
                        "END");

                // Cleans up album_art when an album is deleted
                db.execSQL("CREATE TRIGGER IF NOT EXISTS albumart_cleanup2 DELETE ON album_art " +
                        "BEGIN " +
                            "SELECT _DELETE_FILE(old._data);" +
                        "END");
            }

            // Contains meta data about video files
            db.execSQL("CREATE TABLE IF NOT EXISTS video (" +
                       "_id INTEGER PRIMARY KEY," +
                       "_data TEXT NOT NULL," +
                       "_display_name TEXT," +
                       "_size INTEGER," +
                       "mime_type TEXT," +
                       "date_added INTEGER," +
                       "date_modified INTEGER," +
                       "title TEXT," +
                       "duration INTEGER," +
                       "artist TEXT," +
                       "album TEXT," +
                       "resolution TEXT," +
                       "description TEXT," +
                       "isprivate INTEGER," +   // for YouTube videos
                       "tags TEXT," +           // for YouTube videos
                       "category TEXT," +       // for YouTube videos
                       "language TEXT," +       // for YouTube videos
                       "mini_thumb_data TEXT," +
                       "latitude DOUBLE," +
                       "longitude DOUBLE," +
                       "datetaken INTEGER," +
                       "mini_thumb_magic INTEGER" +
                       ");");

            db.execSQL("CREATE TRIGGER IF NOT EXISTS video_cleanup DELETE ON video " +
                    "BEGIN " +
                        "SELECT _DELETE_FILE(old._data);" +
                    "END");
        }

        // At this point the database is at least at schema version 63 (it was
        // either created at version 63 by the code above, or was already at
        // version 63 or later)

        if (fromVersion < 64) {
            // create the index that updates the database to schema version 64
            db.execSQL("CREATE INDEX IF NOT EXISTS sort_index on images(datetaken ASC, _id ASC);");
        }

        /*
         *  Android 1.0 shipped with database version 64
         */

        if (fromVersion < 65) {
            // create the index that updates the database to schema version 65
            db.execSQL("CREATE INDEX IF NOT EXISTS titlekey_index on audio_meta(title_key);");
        }

        // In version 66, originally we updateBucketNames(db, "images"),
        // but we need to do it in version 89 and therefore save the update here.

        if (fromVersion < 67) {
            // create the indices that update the database to schema version 67
            db.execSQL("CREATE INDEX IF NOT EXISTS albumkey_index on albums(album_key);");
            db.execSQL("CREATE INDEX IF NOT EXISTS artistkey_index on artists(artist_key);");
        }

        if (fromVersion < 68) {
            // Create bucket_id and bucket_display_name columns for the video table.
            db.execSQL("ALTER TABLE video ADD COLUMN bucket_id TEXT;");
            db.execSQL("ALTER TABLE video ADD COLUMN bucket_display_name TEXT");

            // In version 68, originally we updateBucketNames(db, "video"),
            // but we need to do it in version 89 and therefore save the update here.
        }

        if (fromVersion < 69) {
            updateDisplayName(db, "images");
        }

        if (fromVersion < 70) {
            // Create bookmark column for the video table.
            db.execSQL("ALTER TABLE video ADD COLUMN bookmark INTEGER;");
        }

        if (fromVersion < 71) {
            // There is no change to the database schema, however a code change
            // fixed parsing of metadata for certain files bought from the
            // iTunes music store, so we want to rescan files that might need it.
            // We do this by clearing the modification date in the database for
            // those files, so that the media scanner will see them as updated
            // and rescan them.
            db.execSQL("UPDATE audio_meta SET date_modified=0 WHERE _id IN (" +
                    "SELECT _id FROM audio where mime_type='audio/mp4' AND " +
                    "artist='" + MediaStore.UNKNOWN_STRING + "' AND " +
                    "album='" + MediaStore.UNKNOWN_STRING + "'" +
                    ");");
        }

        if (fromVersion < 72) {
            // Create is_podcast and bookmark columns for the audio table.
            db.execSQL("ALTER TABLE audio_meta ADD COLUMN is_podcast INTEGER;");
            db.execSQL("UPDATE audio_meta SET is_podcast=1 WHERE _data LIKE '%/podcasts/%';");
            db.execSQL("UPDATE audio_meta SET is_music=0 WHERE is_podcast=1" +
                    " AND _data NOT LIKE '%/music/%';");
            db.execSQL("ALTER TABLE audio_meta ADD COLUMN bookmark INTEGER;");

            // New columns added to tables aren't visible in views on those tables
            // without opening and closing the database (or using the 'vacuum' command,
            // which we can't do here because all this code runs inside a transaction).
            // To work around this, we drop and recreate the affected view and trigger.
            recreateAudioView(db);
        }

        /*
         *  Android 1.5 shipped with database version 72
         */

        if (fromVersion < 73) {
            // There is no change to the database schema, but we now do case insensitive
            // matching of folder names when determining whether something is music, a
            // ringtone, podcast, etc, so we might need to reclassify some files.
            db.execSQL("UPDATE audio_meta SET is_music=1 WHERE is_music=0 AND " +
                    "_data LIKE '%/music/%';");
            db.execSQL("UPDATE audio_meta SET is_ringtone=1 WHERE is_ringtone=0 AND " +
                    "_data LIKE '%/ringtones/%';");
            db.execSQL("UPDATE audio_meta SET is_notification=1 WHERE is_notification=0 AND " +
                    "_data LIKE '%/notifications/%';");
            db.execSQL("UPDATE audio_meta SET is_alarm=1 WHERE is_alarm=0 AND " +
                    "_data LIKE '%/alarms/%';");
            db.execSQL("UPDATE audio_meta SET is_podcast=1 WHERE is_podcast=0 AND " +
                    "_data LIKE '%/podcasts/%';");
        }

        if (fromVersion < 74) {
            // This view is used instead of the audio view by the union below, to force
            // sqlite to use the title_key index. This greatly reduces memory usage
            // (no separate copy pass needed for sorting, which could cause errors on
            // large datasets) and improves speed (by about 35% on a large dataset)
            db.execSQL("CREATE VIEW IF NOT EXISTS searchhelpertitle AS SELECT * FROM audio " +
                    "ORDER BY title_key;");

            db.execSQL("CREATE VIEW IF NOT EXISTS search AS " +
                    "SELECT _id," +
                    "'artist' AS mime_type," +
                    "artist," +
                    "NULL AS album," +
                    "NULL AS title," +
                    "artist AS text1," +
                    "NULL AS text2," +
                    "number_of_albums AS data1," +
                    "number_of_tracks AS data2," +
                    "artist_key AS match," +
                    "'content://media/external/audio/artists/'||_id AS suggest_intent_data," +
                    "1 AS grouporder " +
                    "FROM artist_info WHERE (artist!='" + MediaStore.UNKNOWN_STRING + "') " +
                "UNION ALL " +
                    "SELECT _id," +
                    "'album' AS mime_type," +
                    "artist," +
                    "album," +
                    "NULL AS title," +
                    "album AS text1," +
                    "artist AS text2," +
                    "NULL AS data1," +
                    "NULL AS data2," +
                    "artist_key||' '||album_key AS match," +
                    "'content://media/external/audio/albums/'||_id AS suggest_intent_data," +
                    "2 AS grouporder " +
                    "FROM album_info WHERE (album!='" + MediaStore.UNKNOWN_STRING + "') " +
                "UNION ALL " +
                    "SELECT searchhelpertitle._id AS _id," +
                    "mime_type," +
                    "artist," +
                    "album," +
                    "title," +
                    "title AS text1," +
                    "artist AS text2," +
                    "NULL AS data1," +
                    "NULL AS data2," +
                    "artist_key||' '||album_key||' '||title_key AS match," +
                    "'content://media/external/audio/media/'||searchhelpertitle._id AS " +
                    "suggest_intent_data," +
                    "3 AS grouporder " +
                    "FROM searchhelpertitle WHERE (title != '') "
                    );
        }

        if (fromVersion < 75) {
            // Force a rescan of the audio entries so we can apply the new logic to
            // distinguish same-named albums.
            db.execSQL("UPDATE audio_meta SET date_modified=0;");
            db.execSQL("DELETE FROM albums");
        }

        if (fromVersion < 76) {
            // We now ignore double quotes when building the key, so we have to remove all of them
            // from existing keys.
            db.execSQL("UPDATE audio_meta SET title_key=" +
                    "REPLACE(title_key,x'081D08C29F081D',x'081D') " +
                    "WHERE title_key LIKE '%'||x'081D08C29F081D'||'%';");
            db.execSQL("UPDATE albums SET album_key=" +
                    "REPLACE(album_key,x'081D08C29F081D',x'081D') " +
                    "WHERE album_key LIKE '%'||x'081D08C29F081D'||'%';");
            db.execSQL("UPDATE artists SET artist_key=" +
                    "REPLACE(artist_key,x'081D08C29F081D',x'081D') " +
                    "WHERE artist_key LIKE '%'||x'081D08C29F081D'||'%';");
        }

        /*
         *  Android 1.6 shipped with database version 76
         */

        if (fromVersion < 77) {
            // create video thumbnail table
            db.execSQL("CREATE TABLE IF NOT EXISTS videothumbnails (" +
                    "_id INTEGER PRIMARY KEY," +
                    "_data TEXT," +
                    "video_id INTEGER," +
                    "kind INTEGER," +
                    "width INTEGER," +
                    "height INTEGER" +
                    ");");

            db.execSQL("CREATE INDEX IF NOT EXISTS video_id_index on videothumbnails(video_id);");

            db.execSQL("CREATE TRIGGER IF NOT EXISTS videothumbnails_cleanup DELETE ON videothumbnails " +
                    "BEGIN " +
                        "SELECT _DELETE_FILE(old._data);" +
                    "END");
        }

        /*
         *  Android 2.0 and 2.0.1 shipped with database version 77
         */

        if (fromVersion < 78) {
            // Force a rescan of the video entries so we can update
            // latest changed DATE_TAKEN units (in milliseconds).
            db.execSQL("UPDATE video SET date_modified=0;");
        }

        /*
         *  Android 2.1 shipped with database version 78
         */

        if (fromVersion < 79) {
            // move /sdcard/albumthumbs to
            // /sdcard/Android/data/com.android.providers.media/albumthumbs,
            // and update the database accordingly

            String oldthumbspath = mExternalStoragePaths[0] + "/albumthumbs";
            String newthumbspath = mExternalStoragePaths[0] + "/" + ALBUM_THUMB_FOLDER;
            File thumbsfolder = new File(oldthumbspath);
            if (thumbsfolder.exists()) {
                // move folder to its new location
                File newthumbsfolder = new File(newthumbspath);
                newthumbsfolder.getParentFile().mkdirs();
                if(thumbsfolder.renameTo(newthumbsfolder)) {
                    // update the database
                    db.execSQL("UPDATE album_art SET _data=REPLACE(_data, '" +
                            oldthumbspath + "','" + newthumbspath + "');");
                }
            }
        }

        if (fromVersion < 80) {
            // Force rescan of image entries to update DATE_TAKEN as UTC timestamp.
            db.execSQL("UPDATE images SET date_modified=0;");
        }

        if (fromVersion < 81 && !internal) {
            // Delete entries starting with /mnt/sdcard. This is for the benefit
            // of users running builds between 2.0.1 and 2.1 final only, since
            // users updating from 2.0 or earlier will not have such entries.

            // First we need to update the _data fields in the affected tables, since
            // otherwise deleting the entries will also delete the underlying files
            // (via a trigger), and we want to keep them.
            db.execSQL("UPDATE audio_playlists SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE images SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE video SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE videothumbnails SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE thumbnails SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE album_art SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE audio_meta SET _data='////' WHERE _data LIKE '/mnt/sdcard/%';");
            // Once the paths have been renamed, we can safely delete the entries
            db.execSQL("DELETE FROM audio_playlists WHERE _data IS '////';");
            db.execSQL("DELETE FROM images WHERE _data IS '////';");
            db.execSQL("DELETE FROM video WHERE _data IS '////';");
            db.execSQL("DELETE FROM videothumbnails WHERE _data IS '////';");
            db.execSQL("DELETE FROM thumbnails WHERE _data IS '////';");
            db.execSQL("DELETE FROM audio_meta WHERE _data  IS '////';");
            db.execSQL("DELETE FROM album_art WHERE _data  IS '////';");

            // rename existing entries starting with /sdcard to /mnt/sdcard
            db.execSQL("UPDATE audio_meta" +
                    " SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE audio_playlists" +
                    " SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE images" +
                    " SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE video" +
                    " SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE videothumbnails" +
                    " SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE thumbnails" +
                    " SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");
            db.execSQL("UPDATE album_art" +
                    " SET _data='/mnt/sdcard'||SUBSTR(_data,8) WHERE _data LIKE '/sdcard/%';");

            // Delete albums and artists, then clear the modification time on songs, which
            // will cause the media scanner to rescan everything, rebuilding the artist and
            // album tables along the way, while preserving playlists.
            // We need this rescan because ICU also changed, and now generates different
            // collation keys
            db.execSQL("DELETE from albums");
            db.execSQL("DELETE from artists");
            db.execSQL("UPDATE audio_meta SET date_modified=0;");
        }

        if (fromVersion < 82) {
            // recreate this view with the correct "group by" specifier
            db.execSQL("DROP VIEW IF EXISTS artist_info");
            db.execSQL("CREATE VIEW IF NOT EXISTS artist_info AS " +
                        "SELECT artist_id AS _id, artist, artist_key, " +
                        "COUNT(DISTINCT album_key) AS number_of_albums, " +
                        "COUNT(*) AS number_of_tracks FROM audio WHERE is_music=1 "+
                        "GROUP BY artist_key;");
        }

        /* we skipped over version 83, and reverted versions 84, 85 and 86 */

        if (fromVersion < 87) {
            // The fastscroll thumb needs an index on the strings being displayed,
            // otherwise the queries it does to determine the correct position
            // becomes really inefficient
            db.execSQL("CREATE INDEX IF NOT EXISTS title_idx on audio_meta(title);");
            db.execSQL("CREATE INDEX IF NOT EXISTS artist_idx on artists(artist);");
            db.execSQL("CREATE INDEX IF NOT EXISTS album_idx on albums(album);");
        }

        if (fromVersion < 88) {
            // Clean up a few more things from versions 84/85/86, and recreate
            // the few things worth keeping from those changes.
            db.execSQL("DROP TRIGGER IF EXISTS albums_update1;");
            db.execSQL("DROP TRIGGER IF EXISTS albums_update2;");
            db.execSQL("DROP TRIGGER IF EXISTS albums_update3;");
            db.execSQL("DROP TRIGGER IF EXISTS albums_update4;");
            db.execSQL("DROP TRIGGER IF EXISTS artist_update1;");
            db.execSQL("DROP TRIGGER IF EXISTS artist_update2;");
            db.execSQL("DROP TRIGGER IF EXISTS artist_update3;");
            db.execSQL("DROP TRIGGER IF EXISTS artist_update4;");
            db.execSQL("DROP VIEW IF EXISTS album_artists;");
            db.execSQL("CREATE INDEX IF NOT EXISTS album_id_idx on audio_meta(album_id);");
            db.execSQL("CREATE INDEX IF NOT EXISTS artist_id_idx on audio_meta(artist_id);");
            // For a given artist_id, provides the album_id for albums on
            // which the artist appears.
            db.execSQL("CREATE VIEW IF NOT EXISTS artists_albums_map AS " +
                    "SELECT DISTINCT artist_id, album_id FROM audio_meta;");
        }

        // In version 89, originally we updateBucketNames(db, "images") and
        // updateBucketNames(db, "video"), but in version 101 we now updateBucketNames
        //  for all files and therefore can save the update here.

        if (fromVersion < 91) {
            // Never query by mini_thumb_magic_index
            db.execSQL("DROP INDEX IF EXISTS mini_thumb_magic_index");

            // sort the items by taken date in each bucket
            db.execSQL("CREATE INDEX IF NOT EXISTS image_bucket_index ON images(bucket_id, datetaken)");
            db.execSQL("CREATE INDEX IF NOT EXISTS video_bucket_index ON video(bucket_id, datetaken)");
        }


        // Gingerbread ended up going to version 100, but didn't yet have the "files"
        // table, so we need to create that if we're at 100 or lower. This means
        // we won't be able to upgrade pre-release Honeycomb.
        if (fromVersion <= 100) {
            // Remove various stages of work in progress for MTP support
            db.execSQL("DROP TABLE IF EXISTS objects");
            db.execSQL("DROP TABLE IF EXISTS files");
            db.execSQL("DROP TRIGGER IF EXISTS images_objects_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS audio_objects_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS video_objects_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS playlists_objects_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup_images;");
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup_audio;");
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup_video;");
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup_playlists;");
            db.execSQL("DROP TRIGGER IF EXISTS media_cleanup;");

            // Create a new table to manage all files in our storage.
            // This contains a union of all the columns from the old
            // images, audio_meta, videos and audio_playlist tables.
            db.execSQL("CREATE TABLE files (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "_data TEXT," +     // this can be null for playlists
                        "_size INTEGER," +
                        "format INTEGER," +
                        "parent INTEGER," +
                        "date_added INTEGER," +
                        "date_modified INTEGER," +
                        "mime_type TEXT," +
                        "title TEXT," +
                        "description TEXT," +
                        "_display_name TEXT," +

                        // for images
                        "picasa_id TEXT," +
                        "orientation INTEGER," +

                        // for images and video
                        "latitude DOUBLE," +
                        "longitude DOUBLE," +
                        "datetaken INTEGER," +
                        "mini_thumb_magic INTEGER," +
                        "bucket_id TEXT," +
                        "bucket_display_name TEXT," +
                        "isprivate INTEGER," +

                        // for audio
                        "title_key TEXT," +
                        "artist_id INTEGER," +
                        "album_id INTEGER," +
                        "composer TEXT," +
                        "track INTEGER," +
                        "year INTEGER CHECK(year!=0)," +
                        "is_ringtone INTEGER," +
                        "is_music INTEGER," +
                        "is_alarm INTEGER," +
                        "is_notification INTEGER," +
                        "is_podcast INTEGER," +
                        "album_artist TEXT," +

                        // for audio and video
                        "duration INTEGER," +
                        "bookmark INTEGER," +

                        // for video
                        "artist TEXT," +
                        "album TEXT," +
                        "resolution TEXT," +
                        "tags TEXT," +
                        "category TEXT," +
                        "language TEXT," +
                        "mini_thumb_data TEXT," +

                        // for playlists
                        "name TEXT," +

                        // media_type is used by the views to emulate the old
                        // images, audio_meta, videos and audio_playlist tables.
                        "media_type INTEGER," +

                        // Value of _id from the old media table.
                        // Used only for updating other tables during database upgrade.
                        "old_id INTEGER" +
                       ");");

            db.execSQL("CREATE INDEX path_index ON files(_data);");
            db.execSQL("CREATE INDEX media_type_index ON files(media_type);");

            // Copy all data from our obsolete tables to the new files table

            // Copy audio records first, preserving the _id column.
            // We do this to maintain compatibility for content Uris for ringtones.
            // Unfortunately we cannot do this for images and videos as well.
            // We choose to do this for the audio table because the fragility of Uris
            // for ringtones are the most common problem we need to avoid.
            db.execSQL("INSERT INTO files (_id," + AUDIO_COLUMNSv99 + ",old_id,media_type)" +
                    " SELECT _id," + AUDIO_COLUMNSv99 + ",_id," + FileColumns.MEDIA_TYPE_AUDIO +
                    " FROM audio_meta;");

            db.execSQL("INSERT INTO files (" + IMAGE_COLUMNSv407 + ",old_id,media_type) SELECT "
                    + IMAGE_COLUMNSv407 + ",_id," + FileColumns.MEDIA_TYPE_IMAGE + " FROM images;");
            db.execSQL("INSERT INTO files (" + VIDEO_COLUMNSv407 + ",old_id,media_type) SELECT "
                    + VIDEO_COLUMNSv407 + ",_id," + FileColumns.MEDIA_TYPE_VIDEO + " FROM video;");
            if (!internal) {
                db.execSQL("INSERT INTO files (" + PLAYLIST_COLUMNS + ",old_id,media_type) SELECT "
                        + PLAYLIST_COLUMNS + ",_id," + FileColumns.MEDIA_TYPE_PLAYLIST
                        + " FROM audio_playlists;");
            }

            // Delete the old tables
            db.execSQL("DROP TABLE IF EXISTS images");
            db.execSQL("DROP TABLE IF EXISTS audio_meta");
            db.execSQL("DROP TABLE IF EXISTS video");
            db.execSQL("DROP TABLE IF EXISTS audio_playlists");

            // Create views to replace our old tables
            db.execSQL("CREATE VIEW images AS SELECT _id," + IMAGE_COLUMNSv407 +
                        " FROM files WHERE " + FileColumns.MEDIA_TYPE + "="
                        + FileColumns.MEDIA_TYPE_IMAGE + ";");
            db.execSQL("CREATE VIEW audio_meta AS SELECT _id," + AUDIO_COLUMNSv100 +
                        " FROM files WHERE " + FileColumns.MEDIA_TYPE + "="
                        + FileColumns.MEDIA_TYPE_AUDIO + ";");
            db.execSQL("CREATE VIEW video AS SELECT _id," + VIDEO_COLUMNSv407 +
                        " FROM files WHERE " + FileColumns.MEDIA_TYPE + "="
                        + FileColumns.MEDIA_TYPE_VIDEO + ";");
            if (!internal) {
                db.execSQL("CREATE VIEW audio_playlists AS SELECT _id," + PLAYLIST_COLUMNS +
                        " FROM files WHERE " + FileColumns.MEDIA_TYPE + "="
                        + FileColumns.MEDIA_TYPE_PLAYLIST + ";");
            }

            // create temporary index to make the updates go faster
            db.execSQL("CREATE INDEX tmp ON files(old_id);");

            // update the image_id column in the thumbnails table.
            db.execSQL("UPDATE thumbnails SET image_id = (SELECT _id FROM files "
                        + "WHERE files.old_id = thumbnails.image_id AND files.media_type = "
                        + FileColumns.MEDIA_TYPE_IMAGE + ");");

            if (!internal) {
                // update audio_id in the audio_genres_map table, and
                // audio_playlists_map tables and playlist_id in the audio_playlists_map table
                db.execSQL("UPDATE audio_genres_map SET audio_id = (SELECT _id FROM files "
                        + "WHERE files.old_id = audio_genres_map.audio_id AND files.media_type = "
                        + FileColumns.MEDIA_TYPE_AUDIO + ");");
                db.execSQL("UPDATE audio_playlists_map SET audio_id = (SELECT _id FROM files "
                        + "WHERE files.old_id = audio_playlists_map.audio_id "
                        + "AND files.media_type = " + FileColumns.MEDIA_TYPE_AUDIO + ");");
                db.execSQL("UPDATE audio_playlists_map SET playlist_id = (SELECT _id FROM files "
                        + "WHERE files.old_id = audio_playlists_map.playlist_id "
                        + "AND files.media_type = " + FileColumns.MEDIA_TYPE_PLAYLIST + ");");
            }

            // update video_id in the videothumbnails table.
            db.execSQL("UPDATE videothumbnails SET video_id = (SELECT _id FROM files "
                        + "WHERE files.old_id = videothumbnails.video_id AND files.media_type = "
                        + FileColumns.MEDIA_TYPE_VIDEO + ");");

            // we don't need this index anymore now
            db.execSQL("DROP INDEX tmp;");

            // update indices to work on the files table
            db.execSQL("DROP INDEX IF EXISTS title_idx");
            db.execSQL("DROP INDEX IF EXISTS album_id_idx");
            db.execSQL("DROP INDEX IF EXISTS image_bucket_index");
            db.execSQL("DROP INDEX IF EXISTS video_bucket_index");
            db.execSQL("DROP INDEX IF EXISTS sort_index");
            db.execSQL("DROP INDEX IF EXISTS titlekey_index");
            db.execSQL("DROP INDEX IF EXISTS artist_id_idx");
            db.execSQL("CREATE INDEX title_idx ON files(title);");
            db.execSQL("CREATE INDEX album_id_idx ON files(album_id);");
            db.execSQL("CREATE INDEX bucket_index ON files(bucket_id, datetaken);");
            db.execSQL("CREATE INDEX sort_index ON files(datetaken ASC, _id ASC);");
            db.execSQL("CREATE INDEX titlekey_index ON files(title_key);");
            db.execSQL("CREATE INDEX artist_id_idx ON files(artist_id);");

            // Recreate triggers for our obsolete tables on the new files table
            db.execSQL("DROP TRIGGER IF EXISTS images_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS audio_meta_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS video_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS audio_playlists_cleanup");
            db.execSQL("DROP TRIGGER IF EXISTS audio_delete");

            db.execSQL("CREATE TRIGGER IF NOT EXISTS images_cleanup DELETE ON files " +
                    "WHEN old.media_type = " + FileColumns.MEDIA_TYPE_IMAGE + " " +
                    "BEGIN " +
                        "DELETE FROM thumbnails WHERE image_id = old._id;" +
                        "SELECT _DELETE_FILE(old._data);" +
                    "END");

            db.execSQL("CREATE TRIGGER IF NOT EXISTS video_cleanup DELETE ON files " +
                    "WHEN old.media_type = " + FileColumns.MEDIA_TYPE_VIDEO + " " +
                    "BEGIN " +
                        "SELECT _DELETE_FILE(old._data);" +
                    "END");

            if (!internal) {
                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_meta_cleanup DELETE ON files " +
                       "WHEN old.media_type = " + FileColumns.MEDIA_TYPE_AUDIO + " " +
                       "BEGIN " +
                           "DELETE FROM audio_genres_map WHERE audio_id = old._id;" +
                           "DELETE FROM audio_playlists_map WHERE audio_id = old._id;" +
                       "END");

                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_playlists_cleanup DELETE ON files " +
                       "WHEN old.media_type = " + FileColumns.MEDIA_TYPE_PLAYLIST + " " +
                       "BEGIN " +
                           "DELETE FROM audio_playlists_map WHERE playlist_id = old._id;" +
                           "SELECT _DELETE_FILE(old._data);" +
                       "END");

                db.execSQL("CREATE TRIGGER IF NOT EXISTS audio_delete INSTEAD OF DELETE ON audio " +
                        "BEGIN " +
                            "DELETE from files where _id=old._id;" +
                            "DELETE from audio_playlists_map where audio_id=old._id;" +
                            "DELETE from audio_genres_map where audio_id=old._id;" +
                        "END");
            }
        }

        if (fromVersion < 301) {
            db.execSQL("DROP INDEX IF EXISTS bucket_index");
            db.execSQL("CREATE INDEX bucket_index on files(bucket_id, media_type, datetaken, _id)");
            db.execSQL("CREATE INDEX bucket_name on files(bucket_id, media_type, bucket_display_name)");
        }

        if (fromVersion < 302) {
            db.execSQL("CREATE INDEX parent_index ON files(parent);");
            db.execSQL("CREATE INDEX format_index ON files(format);");
        }

        if (fromVersion < 303) {
            // the album disambiguator hash changed, so rescan songs and force
            // albums to be updated. Artists are unaffected.
            db.execSQL("DELETE from albums");
            db.execSQL("UPDATE files SET date_modified=0 WHERE " + FileColumns.MEDIA_TYPE + "="
                    + FileColumns.MEDIA_TYPE_AUDIO + ";");
        }

        if (fromVersion < 304 && !internal) {
            // notifies host when files are deleted
            db.execSQL("CREATE TRIGGER IF NOT EXISTS files_cleanup DELETE ON files " +
                    "BEGIN " +
                        "SELECT _OBJECT_REMOVED(old._id);" +
                    "END");

        }

        if (fromVersion < 305 && internal) {
            // version 304 erroneously added this trigger to the internal database
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup");
        }

        if (fromVersion < 306 && !internal) {
            // The genre list was expanded and genre string parsing was tweaked, so
            // rebuild the genre list
            db.execSQL("UPDATE files SET date_modified=0 WHERE " + FileColumns.MEDIA_TYPE + "="
                    + FileColumns.MEDIA_TYPE_AUDIO + ";");
            db.execSQL("DELETE FROM audio_genres_map");
            db.execSQL("DELETE FROM audio_genres");
        }

        if (fromVersion < 307 && !internal) {
            // Force rescan of image entries to update DATE_TAKEN by either GPSTimeStamp or
            // EXIF local time.
            db.execSQL("UPDATE files SET date_modified=0 WHERE " + FileColumns.MEDIA_TYPE + "="
                    + FileColumns.MEDIA_TYPE_IMAGE + ";");
        }

        // Honeycomb went up to version 307, ICS started at 401

        // Database version 401 did not add storage_id to the internal database.
        // We need it there too, so add it in version 402
        if (fromVersion < 401 || (fromVersion == 401 && internal)) {
            // Add column for MTP storage ID
            db.execSQL("ALTER TABLE files ADD COLUMN storage_id INTEGER;");
            // Anything in the database before this upgrade step will be in the primary storage
            db.execSQL("UPDATE files SET storage_id=" + MtpStorage.getStorageId(0) + ";");
        }

        if (fromVersion < 403 && !internal) {
            db.execSQL("CREATE VIEW audio_genres_map_noid AS " +
                    "SELECT audio_id,genre_id from audio_genres_map;");
        }

        if (fromVersion < 404) {
            // There was a bug that could cause distinct same-named albums to be
            // combined again. Delete albums and force a rescan.
            db.execSQL("DELETE from albums");
            db.execSQL("UPDATE files SET date_modified=0 WHERE " + FileColumns.MEDIA_TYPE + "="
                    + FileColumns.MEDIA_TYPE_AUDIO + ";");
        }

        if (fromVersion < 405) {
            // Add is_drm column.
            db.execSQL("ALTER TABLE files ADD COLUMN is_drm INTEGER;");

            db.execSQL("DROP VIEW IF EXISTS audio_meta");
            db.execSQL("CREATE VIEW audio_meta AS SELECT _id," + AUDIO_COLUMNSv405 +
                        " FROM files WHERE " + FileColumns.MEDIA_TYPE + "="
                        + FileColumns.MEDIA_TYPE_AUDIO + ";");

            recreateAudioView(db);
        }

        if (fromVersion < 407) {
            // Rescan files in the media database because a new column has been added
            // in table files in version 405 and to recover from problems populating
            // the genre tables
            db.execSQL("UPDATE files SET date_modified=0;");
        }

        if (fromVersion < 408) {
            // Add the width/height columns for images and video
            db.execSQL("ALTER TABLE files ADD COLUMN width INTEGER;");
            db.execSQL("ALTER TABLE files ADD COLUMN height INTEGER;");

            // Rescan files to fill the columns
            db.execSQL("UPDATE files SET date_modified=0;");

            // Update images and video views to contain the width/height columns
            db.execSQL("DROP VIEW IF EXISTS images");
            db.execSQL("DROP VIEW IF EXISTS video");
            db.execSQL("CREATE VIEW images AS SELECT _id," + IMAGE_COLUMNS +
                        " FROM files WHERE " + FileColumns.MEDIA_TYPE + "="
                        + FileColumns.MEDIA_TYPE_IMAGE + ";");
            db.execSQL("CREATE VIEW video AS SELECT _id," + VIDEO_COLUMNS +
                        " FROM files WHERE " + FileColumns.MEDIA_TYPE + "="
                        + FileColumns.MEDIA_TYPE_VIDEO + ";");
        }

        if (fromVersion < 409 && !internal) {
            // A bug that prevented numeric genres from being parsed was fixed, so
            // rebuild the genre list
            db.execSQL("UPDATE files SET date_modified=0 WHERE " + FileColumns.MEDIA_TYPE + "="
                    + FileColumns.MEDIA_TYPE_AUDIO + ";");
            db.execSQL("DELETE FROM audio_genres_map");
            db.execSQL("DELETE FROM audio_genres");
        }

        // ICS went out with database version 409, JB started at 500

        if (fromVersion < 500) {
            // we're now deleting the file in mediaprovider code, rather than via a trigger
            db.execSQL("DROP TRIGGER IF EXISTS videothumbnails_cleanup;");
        }
        if (fromVersion < 501) {
            // we're now deleting the file in mediaprovider code, rather than via a trigger
            // the images_cleanup trigger would delete the image file and the entry
            // in the thumbnail table, which in turn would trigger thumbnails_cleanup
            // to delete the thumbnail image
            db.execSQL("DROP TRIGGER IF EXISTS images_cleanup;");
            db.execSQL("DROP TRIGGER IF EXISTS thumbnails_cleanup;");
        }
        if (fromVersion < 502) {
            // we're now deleting the file in mediaprovider code, rather than via a trigger
            db.execSQL("DROP TRIGGER IF EXISTS video_cleanup;");
        }
        if (fromVersion < 503) {
            // genre and playlist cleanup now done in mediaprovider code, instead of in a trigger
            db.execSQL("DROP TRIGGER IF EXISTS audio_delete");
            db.execSQL("DROP TRIGGER IF EXISTS audio_meta_cleanup");
        }
        if (fromVersion < 504) {
            // add an index to help with case-insensitive matching of paths
            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS path_index_lower ON files(_data COLLATE NOCASE);");
        }
        if (fromVersion < 505) {
            // Starting with schema 505 we fill in the width/height/resolution columns for videos,
            // so force a rescan of videos to fill in the blanks
            db.execSQL("UPDATE files SET date_modified=0 WHERE " + FileColumns.MEDIA_TYPE + "="
                    + FileColumns.MEDIA_TYPE_VIDEO + ";");
        }
        if (fromVersion < 506) {
            // sd card storage got moved to /storage/sdcard0
            // first delete everything that already got scanned in /storage before this
            // update step was added
            db.execSQL("DROP TRIGGER IF EXISTS files_cleanup");
            db.execSQL("DELETE FROM files WHERE _data LIKE '/storage/%';");
            db.execSQL("DELETE FROM album_art WHERE _data LIKE '/storage/%';");
            db.execSQL("DELETE FROM thumbnails WHERE _data LIKE '/storage/%';");
            db.execSQL("DELETE FROM videothumbnails WHERE _data LIKE '/storage/%';");
            // then rename everything from /mnt/sdcard/ to /storage/sdcard0,
            // and from /mnt/external1 to /storage/sdcard1
            db.execSQL("UPDATE files SET " +
                "_data='/storage/sdcard0'||SUBSTR(_data,12) WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE files SET " +
                "_data='/storage/sdcard1'||SUBSTR(_data,15) WHERE _data LIKE '/mnt/external1/%';");
            db.execSQL("UPDATE album_art SET " +
                "_data='/storage/sdcard0'||SUBSTR(_data,12) WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE album_art SET " +
                "_data='/storage/sdcard1'||SUBSTR(_data,15) WHERE _data LIKE '/mnt/external1/%';");
            db.execSQL("UPDATE thumbnails SET " +
                "_data='/storage/sdcard0'||SUBSTR(_data,12) WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE thumbnails SET " +
                "_data='/storage/sdcard1'||SUBSTR(_data,15) WHERE _data LIKE '/mnt/external1/%';");
            db.execSQL("UPDATE videothumbnails SET " +
                "_data='/storage/sdcard0'||SUBSTR(_data,12) WHERE _data LIKE '/mnt/sdcard/%';");
            db.execSQL("UPDATE videothumbnails SET " +
                "_data='/storage/sdcard1'||SUBSTR(_data,15) WHERE _data LIKE '/mnt/external1/%';");

            if (!internal) {
                db.execSQL("CREATE TRIGGER IF NOT EXISTS files_cleanup DELETE ON files " +
                    "BEGIN " +
                        "SELECT _OBJECT_REMOVED(old._id);" +
                    "END");
            }
        }
        if (fromVersion < 507) {
            // we update _data in version 506, we need to update the bucket_id as well
            updateBucketNames(db);
        }
        if (fromVersion < 508 && !internal) {
            // ensure we don't get duplicate entries in the genre map
            db.execSQL("CREATE TABLE IF NOT EXISTS audio_genres_map_tmp (" +
                    "_id INTEGER PRIMARY KEY," +
                    "audio_id INTEGER NOT NULL," +
                    "genre_id INTEGER NOT NULL," +
                    "UNIQUE (audio_id,genre_id) ON CONFLICT IGNORE" +
                    ");");
            db.execSQL("INSERT INTO audio_genres_map_tmp (audio_id,genre_id)" +
                    " SELECT DISTINCT audio_id,genre_id FROM audio_genres_map;");
            db.execSQL("DROP TABLE audio_genres_map;");
            db.execSQL("ALTER TABLE audio_genres_map_tmp RENAME TO audio_genres_map;");
        }

        if (fromVersion < 509) {
            db.execSQL("CREATE TABLE IF NOT EXISTS log (time DATETIME PRIMARY KEY, message TEXT);");
        }

        // Emulated external storage moved to user-specific paths
        if (fromVersion < 510 && Environment.isExternalStorageEmulated()) {
            // File.fixSlashes() removes any trailing slashes
            final String externalStorage = Environment.getExternalStorageDirectory().toString();
            Log.d(TAG, "Adjusting external storage paths to: " + externalStorage);

            final String[] tables = {
                    TABLE_FILES, TABLE_ALBUM_ART, TABLE_THUMBNAILS, TABLE_VIDEO_THUMBNAILS };
            for (String table : tables) {
                db.execSQL("UPDATE " + table + " SET " + "_data='" + externalStorage
                        + "'||SUBSTR(_data,17) WHERE _data LIKE '/storage/sdcard0/%';");
            }
        }
        if (fromVersion < 511) {
            // we update _data in version 510, we need to update the bucket_id as well
            updateBucketNames(db);
        }

        // JB 4.2 went out with database version 511, starting next release with 600

        if (fromVersion < 600) {
            // modify _data column to be unique and collate nocase. Because this drops the original
            // table and replaces it with a new one by the same name, we need to also recreate all
            // indices and triggers that refer to the files table.
            // Views don't need to be recreated.

            db.execSQL("CREATE TABLE files2 (_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "_data TEXT UNIQUE" +
                    // the internal filesystem is case-sensitive
                    (internal ? "," : " COLLATE NOCASE,") +
                    "_size INTEGER,format INTEGER,parent INTEGER,date_added INTEGER," +
                    "date_modified INTEGER,mime_type TEXT,title TEXT,description TEXT," +
                    "_display_name TEXT,picasa_id TEXT,orientation INTEGER,latitude DOUBLE," +
                    "longitude DOUBLE,datetaken INTEGER,mini_thumb_magic INTEGER,bucket_id TEXT," +
                    "bucket_display_name TEXT,isprivate INTEGER,title_key TEXT,artist_id INTEGER," +
                    "album_id INTEGER,composer TEXT,track INTEGER,year INTEGER CHECK(year!=0)," +
                    "is_ringtone INTEGER,is_music INTEGER,is_alarm INTEGER," +
                    "is_notification INTEGER,is_podcast INTEGER,album_artist TEXT," +
                    "duration INTEGER,bookmark INTEGER,artist TEXT,album TEXT,resolution TEXT," +
                    "tags TEXT,category TEXT,language TEXT,mini_thumb_data TEXT,name TEXT," +
                    "media_type INTEGER,old_id INTEGER,storage_id INTEGER,is_drm INTEGER," +
                    "width INTEGER, height INTEGER);");

            // copy data from old table, squashing entries with duplicate _data
            db.execSQL("INSERT OR REPLACE INTO files2 SELECT * FROM files;");
            db.execSQL("DROP TABLE files;");
            db.execSQL("ALTER TABLE files2 RENAME TO files;");

            // recreate indices and triggers
            db.execSQL("CREATE INDEX album_id_idx ON files(album_id);");
            db.execSQL("CREATE INDEX artist_id_idx ON files(artist_id);");
            db.execSQL("CREATE INDEX bucket_index on files(bucket_id,media_type," +
                    "datetaken, _id);");
            db.execSQL("CREATE INDEX bucket_name on files(bucket_id,media_type," +
                    "bucket_display_name);");
            db.execSQL("CREATE INDEX format_index ON files(format);");
            db.execSQL("CREATE INDEX media_type_index ON files(media_type);");
            db.execSQL("CREATE INDEX parent_index ON files(parent);");
            db.execSQL("CREATE INDEX path_index ON files(_data);");
            db.execSQL("CREATE INDEX sort_index ON files(datetaken ASC, _id ASC);");
            db.execSQL("CREATE INDEX title_idx ON files(title);");
            db.execSQL("CREATE INDEX titlekey_index ON files(title_key);");
            if (!internal) {
                db.execSQL("CREATE TRIGGER audio_playlists_cleanup DELETE ON files" +
                        " WHEN old.media_type=4" +
                        " BEGIN DELETE FROM audio_playlists_map WHERE playlist_id = old._id;" +
                        "SELECT _DELETE_FILE(old._data);END;");
                db.execSQL("CREATE TRIGGER files_cleanup DELETE ON files" +
                        " BEGIN SELECT _OBJECT_REMOVED(old._id);END;");
            }
        }

        if (fromVersion < 601) {
            // remove primary key constraint because column time is not necessarily unique
            db.execSQL("CREATE TABLE IF NOT EXISTS log_tmp (time DATETIME, message TEXT);");
            db.execSQL("DELETE FROM log_tmp;");
            db.execSQL("INSERT INTO log_tmp SELECT time, message FROM log order by rowid;");
            db.execSQL("DROP TABLE log;");
            db.execSQL("ALTER TABLE log_tmp RENAME TO log;");
        }

        if (fromVersion < 700) {
            // fix datetaken fields that were added with an incorrect timestamp
            // datetaken needs to be in milliseconds, so should generally be a few orders of
            // magnitude larger than date_modified. If it's within the same order of magnitude, it
            // is probably wrong.
            // (this could do the wrong thing if your picture was actually taken before ~3/21/1970)
            db.execSQL("UPDATE files set datetaken=date_modified*1000"
                    + " WHERE date_modified IS NOT NULL"
                    + " AND datetaken IS NOT NULL"
                    + " AND datetaken<date_modified*5;");
        }


        sanityCheck(db, fromVersion);
        long elapsedSeconds = (SystemClock.currentTimeMicro() - startTime) / 1000000;
        logToDb(db, "Database upgraded from version " + fromVersion + " to " + toVersion
                + " in " + elapsedSeconds + " seconds");
    }

    /**
     * Write a persistent diagnostic message to the log table.
     */
    static void logToDb(SQLiteDatabase db, String message) {
        db.execSQL("INSERT OR REPLACE" +
                " INTO log (time,message) VALUES (strftime('%Y-%m-%d %H:%M:%f','now'),?);",
                new String[] { message });
        // delete all but the last 500 rows
        db.execSQL("DELETE FROM log WHERE rowid IN" +
                " (SELECT rowid FROM log ORDER BY rowid DESC LIMIT 500,-1);");
    }

    /**
     * Perform a simple sanity check on the database. Currently this tests
     * whether all the _data entries in audio_meta are unique
     */
    private static void sanityCheck(SQLiteDatabase db, int fromVersion) {
        Cursor c1 = db.query("audio_meta", new String[] {"count(*)"},
                null, null, null, null, null);
        Cursor c2 = db.query("audio_meta", new String[] {"count(distinct _data)"},
                null, null, null, null, null);
        c1.moveToFirst();
        c2.moveToFirst();
        int num1 = c1.getInt(0);
        int num2 = c2.getInt(0);
        c1.close();
        c2.close();
        if (num1 != num2) {
            Log.e(TAG, "audio_meta._data column is not unique while upgrading" +
                    " from schema " +fromVersion + " : " + num1 +"/" + num2);
            // Delete all audio_meta rows so they will be rebuilt by the media scanner
            db.execSQL("DELETE FROM audio_meta;");
        }
    }

    private static void recreateAudioView(SQLiteDatabase db) {
        // Provides a unified audio/artist/album info view.
        db.execSQL("DROP VIEW IF EXISTS audio");
        db.execSQL("CREATE VIEW IF NOT EXISTS audio as SELECT * FROM audio_meta " +
                    "LEFT OUTER JOIN artists ON audio_meta.artist_id=artists.artist_id " +
                    "LEFT OUTER JOIN albums ON audio_meta.album_id=albums.album_id;");
    }

    /**
     * Update the bucket_id and bucket_display_name columns for images and videos
     * @param db
     * @param tableName
     */
    private static void updateBucketNames(SQLiteDatabase db) {
        // Rebuild the bucket_display_name column using the natural case rather than lower case.
        db.beginTransaction();
        try {
            String[] columns = {BaseColumns._ID, MediaColumns.DATA};
            // update only images and videos
            Cursor cursor = db.query("files", columns, "media_type=1 OR media_type=3",
                    null, null, null, null);
            try {
                final int idColumnIndex = cursor.getColumnIndex(BaseColumns._ID);
                final int dataColumnIndex = cursor.getColumnIndex(MediaColumns.DATA);
                String [] rowId = new String[1];
                ContentValues values = new ContentValues();
                while (cursor.moveToNext()) {
                    String data = cursor.getString(dataColumnIndex);
                    rowId[0] = cursor.getString(idColumnIndex);
                    if (data != null) {
                        values.clear();
                        computeBucketValues(data, values);
                        db.update("files", values, "_id=?", rowId);
                    } else {
                        Log.w(TAG, "null data at id " + rowId);
                    }
                }
            } finally {
                cursor.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Iterate through the rows of a table in a database, ensuring that the
     * display name column has a value.
     * @param db
     * @param tableName
     */
    private static void updateDisplayName(SQLiteDatabase db, String tableName) {
        // Fill in default values for null displayName values
        db.beginTransaction();
        try {
            String[] columns = {BaseColumns._ID, MediaColumns.DATA, MediaColumns.DISPLAY_NAME};
            Cursor cursor = db.query(tableName, columns, null, null, null, null, null);
            try {
                final int idColumnIndex = cursor.getColumnIndex(BaseColumns._ID);
                final int dataColumnIndex = cursor.getColumnIndex(MediaColumns.DATA);
                final int displayNameIndex = cursor.getColumnIndex(MediaColumns.DISPLAY_NAME);
                ContentValues values = new ContentValues();
                while (cursor.moveToNext()) {
                    String displayName = cursor.getString(displayNameIndex);
                    if (displayName == null) {
                        String data = cursor.getString(dataColumnIndex);
                        values.clear();
                        computeDisplayName(data, values);
                        int rowId = cursor.getInt(idColumnIndex);
                        db.update(tableName, values, "_id=" + rowId, null);
                    }
                }
            } finally {
                cursor.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * @param data The input path
     * @param values the content values, where the bucked id name and bucket display name are updated.
     *
     */
    private static void computeBucketValues(String data, ContentValues values) {
        File parentFile = new File(data).getParentFile();
        if (parentFile == null) {
            parentFile = new File("/");
        }

        // Lowercase the path for hashing. This avoids duplicate buckets if the
        // filepath case is changed externally.
        // Keep the original case for display.
        String path = parentFile.toString().toLowerCase();
        String name = parentFile.getName();

        // Note: the BUCKET_ID and BUCKET_DISPLAY_NAME attributes are spelled the
        // same for both images and video. However, for backwards-compatibility reasons
        // there is no common base class. We use the ImageColumns version here
        values.put(ImageColumns.BUCKET_ID, path.hashCode());
        values.put(ImageColumns.BUCKET_DISPLAY_NAME, name);
    }

    /**
     * @param data The input path
     * @param values the content values, where the display name is updated.
     *
     */
    private static void computeDisplayName(String data, ContentValues values) {
        String s = (data == null ? "" : data.toString());
        int idx = s.lastIndexOf('/');
        if (idx >= 0) {
            s = s.substring(idx + 1);
        }
        values.put("_display_name", s);
    }

    /**
     * Copy taken time from date_modified if we lost the original value (e.g. after factory reset)
     * This works for both video and image tables.
     *
     * @param values the content values, where taken time is updated.
     */
    private static void computeTakenTime(ContentValues values) {
        if (! values.containsKey(Images.Media.DATE_TAKEN)) {
            // This only happens when MediaScanner finds an image file that doesn't have any useful
            // reference to get this value. (e.g. GPSTimeStamp)
            Long lastModified = values.getAsLong(MediaColumns.DATE_MODIFIED);
            if (lastModified != null) {
                values.put(Images.Media.DATE_TAKEN, lastModified * 1000);
            }
        }
    }

    /**
     * This method blocks until thumbnail is ready.
     *
     * @param thumbUri
     * @return
     */
    private boolean waitForThumbnailReady(Uri origUri) {
        Cursor c = this.query(origUri, new String[] { ImageColumns._ID, ImageColumns.DATA,
                ImageColumns.MINI_THUMB_MAGIC}, null, null, null);
        if (c == null) return false;

        boolean result = false;

        if (c.moveToFirst()) {
            long id = c.getLong(0);
            String path = c.getString(1);
            long magic = c.getLong(2);

            MediaThumbRequest req = requestMediaThumbnail(path, origUri,
                    MediaThumbRequest.PRIORITY_HIGH, magic);
            if (req == null) {
                return false;
            }
            synchronized (req) {
                try {
                    while (req.mState == MediaThumbRequest.State.WAIT) {
                        req.wait();
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, e);
                }
                if (req.mState == MediaThumbRequest.State.DONE) {
                    result = true;
                }
            }
        }
        c.close();

        return result;
    }

    private boolean matchThumbRequest(MediaThumbRequest req, int pid, long id, long gid,
            boolean isVideo) {
        boolean cancelAllOrigId = (id == -1);
        boolean cancelAllGroupId = (gid == -1);
        return (req.mCallingPid == pid) &&
                (cancelAllGroupId || req.mGroupId == gid) &&
                (cancelAllOrigId || req.mOrigId == id) &&
                (req.mIsVideo == isVideo);
    }

    private boolean queryThumbnail(SQLiteQueryBuilder qb, Uri uri, String table,
            String column, boolean hasThumbnailId) {
        qb.setTables(table);
        if (hasThumbnailId) {
            // For uri dispatched to this method, the 4th path segment is always
            // the thumbnail id.
            qb.appendWhere("_id = " + uri.getPathSegments().get(3));
            // client already knows which thumbnail it wants, bypass it.
            return true;
        }
        String origId = uri.getQueryParameter("orig_id");
        // We can't query ready_flag unless we know original id
        if (origId == null) {
            // this could be thumbnail query for other purpose, bypass it.
            return true;
        }

        boolean needBlocking = "1".equals(uri.getQueryParameter("blocking"));
        boolean cancelRequest = "1".equals(uri.getQueryParameter("cancel"));
        Uri origUri = uri.buildUpon().encodedPath(
                uri.getPath().replaceFirst("thumbnails", "media"))
                .appendPath(origId).build();

        if (needBlocking && !waitForThumbnailReady(origUri)) {
            Log.w(TAG, "original media doesn't exist or it's canceled.");
            return false;
        } else if (cancelRequest) {
            String groupId = uri.getQueryParameter("group_id");
            boolean isVideo = "video".equals(uri.getPathSegments().get(1));
            int pid = Binder.getCallingPid();
            long id = -1;
            long gid = -1;

            try {
                id = Long.parseLong(origId);
                gid = Long.parseLong(groupId);
            } catch (NumberFormatException ex) {
                // invalid cancel request
                return false;
            }

            synchronized (mMediaThumbQueue) {
                if (mCurrentThumbRequest != null &&
                        matchThumbRequest(mCurrentThumbRequest, pid, id, gid, isVideo)) {
                    synchronized (mCurrentThumbRequest) {
                        mCurrentThumbRequest.mState = MediaThumbRequest.State.CANCEL;
                        mCurrentThumbRequest.notifyAll();
                    }
                }
                for (MediaThumbRequest mtq : mMediaThumbQueue) {
                    if (matchThumbRequest(mtq, pid, id, gid, isVideo)) {
                        synchronized (mtq) {
                            mtq.mState = MediaThumbRequest.State.CANCEL;
                            mtq.notifyAll();
                        }

                        mMediaThumbQueue.remove(mtq);
                    }
                }
            }
        }

        if (origId != null) {
            qb.appendWhere(column + " = " + origId);
        }
        return true;
    }

    @Override
    public Uri canonicalize(Uri uri) {
        int match = URI_MATCHER.match(uri);

        // only support canonicalizing specific audio Uris
        if (match != AUDIO_MEDIA_ID) {
            return null;
        }

        Cursor c = query(uri, null, null, null, null);
        if (c == null) {
            return null;
        }
        if (c.getCount() != 1 || !c.moveToNext()) {
            c.close();
            return null;
        }

        // Construct a canonical Uri by tacking on some query parameters
        Uri.Builder builder = uri.buildUpon();
        builder.appendQueryParameter(CANONICAL, "1");
        String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
        c.close();
        if (TextUtils.isEmpty(title)) {
            return null;
        }
        builder.appendQueryParameter(MediaStore.Audio.Media.TITLE, title);
        Uri newUri = builder.build();
        return newUri;
    }

    @Override
    public Uri uncanonicalize(Uri uri) {
        if (uri != null && "1".equals(uri.getQueryParameter(CANONICAL))) {
            int match = URI_MATCHER.match(uri);
            if (match != AUDIO_MEDIA_ID) {
                // this type of canonical Uri is not supported
                return null;
            }
            String titleFromUri = uri.getQueryParameter(MediaStore.Audio.Media.TITLE);
            if (titleFromUri == null) {
                // the required parameter is missing
                return null;
            }
            // clear the query parameters, we don't need them anymore
            uri = uri.buildUpon().clearQuery().build();

            Cursor c = query(uri, null, null, null, null);

            int titleIdx = c.getColumnIndex(MediaStore.Audio.Media.TITLE);
            if (c != null && c.getCount() == 1 && c.moveToNext() &&
                    titleFromUri.equals(c.getString(titleIdx))) {
                // the result matched perfectly
                c.close();
                return uri;
            }

            c.close();
            // do a lookup by title
            Uri newUri = MediaStore.Audio.Media.getContentUri(uri.getPathSegments().get(0));

            c = query(newUri, null, MediaStore.Audio.Media.TITLE + "=?",
                    new String[] {titleFromUri}, null);
            if (c == null) {
                return null;
            }
            if (!c.moveToNext()) {
                c.close();
                return null;
            }
            // get the first matching entry and return a Uri for it
            long id = c.getLong(c.getColumnIndex(MediaStore.Audio.Media._ID));
            c.close();
            return ContentUris.withAppendedId(newUri, id);
        }
        return uri;
    }

    private Uri safeUncanonicalize(Uri uri) {
        Uri newUri = uncanonicalize(uri);
        if (newUri != null) {
            return newUri;
        }
        return uri;
    }

    @SuppressWarnings("fallthrough")
    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {

        uri = safeUncanonicalize(uri);

        int table = URI_MATCHER.match(uri);
        List<String> prependArgs = new ArrayList<String>();

        // Log.v(TAG, "query: uri="+uri+", selection="+selection);
        // handle MEDIA_SCANNER before calling getDatabaseForUri()
        if (table == MEDIA_SCANNER) {
            if (mMediaScannerVolume == null) {
                return null;
            } else {
                // create a cursor to return volume currently being scanned by the media scanner
                MatrixCursor c = new MatrixCursor(new String[] {MediaStore.MEDIA_SCANNER_VOLUME});
                c.addRow(new String[] {mMediaScannerVolume});
                return c;
            }
        }

        // Used temporarily (until we have unique media IDs) to get an identifier
        // for the current sd card, so that the music app doesn't have to use the
        // non-public getFatVolumeId method
        if (table == FS_ID) {
            MatrixCursor c = new MatrixCursor(new String[] {"fsid"});
            c.addRow(new Integer[] {mVolumeId});
            return c;
        }

        if (table == VERSION) {
            MatrixCursor c = new MatrixCursor(new String[] {"version"});
            c.addRow(new Integer[] {getDatabaseVersion(getContext())});
            return c;
        }

        String groupBy = null;
        DatabaseHelper helper = getDatabaseForUri(uri);
        if (helper == null) {
            return null;
        }
        helper.mNumQueries++;
        SQLiteDatabase db = helper.getReadableDatabase();
        if (db == null) return null;
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String limit = uri.getQueryParameter("limit");
        String filter = uri.getQueryParameter("filter");
        String [] keywords = null;
        if (filter != null) {
            filter = Uri.decode(filter).trim();
            if (!TextUtils.isEmpty(filter)) {
                String [] searchWords = filter.split(" ");
                keywords = new String[searchWords.length];
                for (int i = 0; i < searchWords.length; i++) {
                    String key = MediaStore.Audio.keyFor(searchWords[i]);
                    key = key.replace("\\", "\\\\");
                    key = key.replace("%", "\\%");
                    key = key.replace("_", "\\_");
                    keywords[i] = key;
                }
            }
        }
        if (uri.getQueryParameter("distinct") != null) {
            qb.setDistinct(true);
        }

        boolean hasThumbnailId = false;

        switch (table) {
            case IMAGES_MEDIA:
                qb.setTables("images");
                if (uri.getQueryParameter("distinct") != null)
                    qb.setDistinct(true);

                // set the project map so that data dir is prepended to _data.
                //qb.setProjectionMap(mImagesProjectionMap, true);
                break;

            case IMAGES_MEDIA_ID:
                qb.setTables("images");
                if (uri.getQueryParameter("distinct") != null)
                    qb.setDistinct(true);

                // set the project map so that data dir is prepended to _data.
                //qb.setProjectionMap(mImagesProjectionMap, true);
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(3));
                break;

            case IMAGES_THUMBNAILS_ID:
                hasThumbnailId = true;
            case IMAGES_THUMBNAILS:
                if (!queryThumbnail(qb, uri, "thumbnails", "image_id", hasThumbnailId)) {
                    return null;
                }
                break;

            case AUDIO_MEDIA:
                if (projectionIn != null && projectionIn.length == 1 &&  selectionArgs == null
                        && (selection == null || selection.equalsIgnoreCase("is_music=1")
                          || selection.equalsIgnoreCase("is_podcast=1") )
                        && projectionIn[0].equalsIgnoreCase("count(*)")
                        && keywords != null) {
                    //Log.i("@@@@", "taking fast path for counting songs");
                    qb.setTables("audio_meta");
                } else {
                    qb.setTables("audio");
                    for (int i = 0; keywords != null && i < keywords.length; i++) {
                        if (i > 0) {
                            qb.appendWhere(" AND ");
                        }
                        qb.appendWhere(MediaStore.Audio.Media.ARTIST_KEY +
                                "||" + MediaStore.Audio.Media.ALBUM_KEY +
                                "||" + MediaStore.Audio.Media.TITLE_KEY + " LIKE ? ESCAPE '\\'");
                        prependArgs.add("%" + keywords[i] + "%");
                    }
                }
                break;

            case AUDIO_MEDIA_ID:
                qb.setTables("audio");
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(3));
                break;

            case AUDIO_MEDIA_ID_GENRES:
                qb.setTables("audio_genres");
                qb.appendWhere("_id IN (SELECT genre_id FROM " +
                        "audio_genres_map WHERE audio_id=?)");
                prependArgs.add(uri.getPathSegments().get(3));
                break;

            case AUDIO_MEDIA_ID_GENRES_ID:
                qb.setTables("audio_genres");
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(5));
                break;

            case AUDIO_MEDIA_ID_PLAYLISTS:
                qb.setTables("audio_playlists");
                qb.appendWhere("_id IN (SELECT playlist_id FROM " +
                        "audio_playlists_map WHERE audio_id=?)");
                prependArgs.add(uri.getPathSegments().get(3));
                break;

            case AUDIO_MEDIA_ID_PLAYLISTS_ID:
                qb.setTables("audio_playlists");
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(5));
                break;

            case AUDIO_GENRES:
                qb.setTables("audio_genres");
                break;

            case AUDIO_GENRES_ID:
                qb.setTables("audio_genres");
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(3));
                break;

            case AUDIO_GENRES_ALL_MEMBERS:
            case AUDIO_GENRES_ID_MEMBERS:
                {
                    // if simpleQuery is true, we can do a simpler query on just audio_genres_map
                    // we can do this if we have no keywords and our projection includes just columns
                    // from audio_genres_map
                    boolean simpleQuery = (keywords == null && projectionIn != null
                            && (selection == null || selection.equalsIgnoreCase("genre_id=?")));
                    if (projectionIn != null) {
                        for (int i = 0; i < projectionIn.length; i++) {
                            String p = projectionIn[i];
                            if (p.equals("_id")) {
                                // note, this is different from playlist below, because
                                // "_id" used to (wrongly) be the audio id in this query, not
                                // the row id of the entry in the map, and we preserve this
                                // behavior for backwards compatibility
                                simpleQuery = false;
                            }
                            if (simpleQuery && !(p.equals("audio_id") ||
                                    p.equals("genre_id"))) {
                                simpleQuery = false;
                            }
                        }
                    }
                    if (simpleQuery) {
                        qb.setTables("audio_genres_map_noid");
                        if (table == AUDIO_GENRES_ID_MEMBERS) {
                            qb.appendWhere("genre_id=?");
                            prependArgs.add(uri.getPathSegments().get(3));
                        }
                    } else {
                        qb.setTables("audio_genres_map_noid, audio");
                        qb.appendWhere("audio._id = audio_id");
                        if (table == AUDIO_GENRES_ID_MEMBERS) {
                            qb.appendWhere(" AND genre_id=?");
                            prependArgs.add(uri.getPathSegments().get(3));
                        }
                        for (int i = 0; keywords != null && i < keywords.length; i++) {
                            qb.appendWhere(" AND ");
                            qb.appendWhere(MediaStore.Audio.Media.ARTIST_KEY +
                                    "||" + MediaStore.Audio.Media.ALBUM_KEY +
                                    "||" + MediaStore.Audio.Media.TITLE_KEY +
                                    " LIKE ? ESCAPE '\\'");
                            prependArgs.add("%" + keywords[i] + "%");
                        }
                    }
                }
                break;

            case AUDIO_PLAYLISTS:
                qb.setTables("audio_playlists");
                break;

            case AUDIO_PLAYLISTS_ID:
                qb.setTables("audio_playlists");
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(3));
                break;

            case AUDIO_PLAYLISTS_ID_MEMBERS_ID:
            case AUDIO_PLAYLISTS_ID_MEMBERS:
                // if simpleQuery is true, we can do a simpler query on just audio_playlists_map
                // we can do this if we have no keywords and our projection includes just columns
                // from audio_playlists_map
                boolean simpleQuery = (keywords == null && projectionIn != null
                        && (selection == null || selection.equalsIgnoreCase("playlist_id=?")));
                if (projectionIn != null) {
                    for (int i = 0; i < projectionIn.length; i++) {
                        String p = projectionIn[i];
                        if (simpleQuery && !(p.equals("audio_id") ||
                                p.equals("playlist_id") || p.equals("play_order"))) {
                            simpleQuery = false;
                        }
                        if (p.equals("_id")) {
                            projectionIn[i] = "audio_playlists_map._id AS _id";
                        }
                    }
                }
                if (simpleQuery) {
                    qb.setTables("audio_playlists_map");
                    qb.appendWhere("playlist_id=?");
                    prependArgs.add(uri.getPathSegments().get(3));
                } else {
                    qb.setTables("audio_playlists_map, audio");
                    qb.appendWhere("audio._id = audio_id AND playlist_id=?");
                    prependArgs.add(uri.getPathSegments().get(3));
                    for (int i = 0; keywords != null && i < keywords.length; i++) {
                        qb.appendWhere(" AND ");
                        qb.appendWhere(MediaStore.Audio.Media.ARTIST_KEY +
                                "||" + MediaStore.Audio.Media.ALBUM_KEY +
                                "||" + MediaStore.Audio.Media.TITLE_KEY +
                                " LIKE ? ESCAPE '\\'");
                        prependArgs.add("%" + keywords[i] + "%");
                    }
                }
                if (table == AUDIO_PLAYLISTS_ID_MEMBERS_ID) {
                    qb.appendWhere(" AND audio_playlists_map._id=?");
                    prependArgs.add(uri.getPathSegments().get(5));
                }
                break;

            case VIDEO_MEDIA:
                qb.setTables("video");
                break;
            case VIDEO_MEDIA_ID:
                qb.setTables("video");
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(3));
                break;

            case VIDEO_THUMBNAILS_ID:
                hasThumbnailId = true;
            case VIDEO_THUMBNAILS:
                if (!queryThumbnail(qb, uri, "videothumbnails", "video_id", hasThumbnailId)) {
                    return null;
                }
                break;

            case AUDIO_ARTISTS:
                if (projectionIn != null && projectionIn.length == 1 &&  selectionArgs == null
                        && (selection == null || selection.length() == 0)
                        && projectionIn[0].equalsIgnoreCase("count(*)")
                        && keywords != null) {
                    //Log.i("@@@@", "taking fast path for counting artists");
                    qb.setTables("audio_meta");
                    projectionIn[0] = "count(distinct artist_id)";
                    qb.appendWhere("is_music=1");
                } else {
                    qb.setTables("artist_info");
                    for (int i = 0; keywords != null && i < keywords.length; i++) {
                        if (i > 0) {
                            qb.appendWhere(" AND ");
                        }
                        qb.appendWhere(MediaStore.Audio.Media.ARTIST_KEY +
                                " LIKE ? ESCAPE '\\'");
                        prependArgs.add("%" + keywords[i] + "%");
                    }
                }
                break;

            case AUDIO_ARTISTS_ID:
                qb.setTables("artist_info");
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(3));
                break;

            case AUDIO_ARTISTS_ID_ALBUMS:
                String aid = uri.getPathSegments().get(3);
                qb.setTables("audio LEFT OUTER JOIN album_art ON" +
                        " audio.album_id=album_art.album_id");
                qb.appendWhere("is_music=1 AND audio.album_id IN (SELECT album_id FROM " +
                        "artists_albums_map WHERE artist_id=?)");
                prependArgs.add(aid);
                for (int i = 0; keywords != null && i < keywords.length; i++) {
                    qb.appendWhere(" AND ");
                    qb.appendWhere(MediaStore.Audio.Media.ARTIST_KEY +
                            "||" + MediaStore.Audio.Media.ALBUM_KEY +
                            " LIKE ? ESCAPE '\\'");
                    prependArgs.add("%" + keywords[i] + "%");
                }
                groupBy = "audio.album_id";
                sArtistAlbumsMap.put(MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
                        "count(CASE WHEN artist_id==" + aid + " THEN 'foo' ELSE NULL END) AS " +
                        MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST);
                qb.setProjectionMap(sArtistAlbumsMap);
                break;

            case AUDIO_ALBUMS:
                if (projectionIn != null && projectionIn.length == 1 &&  selectionArgs == null
                        && (selection == null || selection.length() == 0)
                        && projectionIn[0].equalsIgnoreCase("count(*)")
                        && keywords != null) {
                    //Log.i("@@@@", "taking fast path for counting albums");
                    qb.setTables("audio_meta");
                    projectionIn[0] = "count(distinct album_id)";
                    qb.appendWhere("is_music=1");
                } else {
                    qb.setTables("album_info");
                    for (int i = 0; keywords != null && i < keywords.length; i++) {
                        if (i > 0) {
                            qb.appendWhere(" AND ");
                        }
                        qb.appendWhere(MediaStore.Audio.Media.ARTIST_KEY +
                                "||" + MediaStore.Audio.Media.ALBUM_KEY +
                                " LIKE ? ESCAPE '\\'");
                        prependArgs.add("%" + keywords[i] + "%");
                    }
                }
                break;

            case AUDIO_ALBUMS_ID:
                qb.setTables("album_info");
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(3));
                break;

            case AUDIO_ALBUMART_ID:
                qb.setTables("album_art");
                qb.appendWhere("album_id=?");
                prependArgs.add(uri.getPathSegments().get(3));
                break;

            case AUDIO_SEARCH_LEGACY:
                Log.w(TAG, "Legacy media search Uri used. Please update your code.");
                // fall through
            case AUDIO_SEARCH_FANCY:
            case AUDIO_SEARCH_BASIC:
                return doAudioSearch(db, qb, uri, projectionIn, selection,
                        combine(prependArgs, selectionArgs), sort, table, limit);

            case FILES_ID:
            case MTP_OBJECTS_ID:
                qb.appendWhere("_id=?");
                prependArgs.add(uri.getPathSegments().get(2));
                // fall through
            case FILES:
            case MTP_OBJECTS:
                qb.setTables("files");
                break;

            case MTP_OBJECT_REFERENCES:
                int handle = Integer.parseInt(uri.getPathSegments().get(2));
                return getObjectReferences(helper, db, handle);

            default:
                throw new IllegalStateException("Unknown URL: " + uri.toString());
        }

        // Log.v(TAG, "query = "+ qb.buildQuery(projectionIn, selection,
        //        combine(prependArgs, selectionArgs), groupBy, null, sort, limit));
        Cursor c = qb.query(db, projectionIn, selection,
                combine(prependArgs, selectionArgs), groupBy, null, sort, limit);

        if (c != null) {
            String nonotify = uri.getQueryParameter("nonotify");
            if (nonotify == null || !nonotify.equals("1")) {
                c.setNotificationUri(getContext().getContentResolver(), uri);
            }
        }

        return c;
    }

    private String[] combine(List<String> prepend, String[] userArgs) {
        int presize = prepend.size();
        if (presize == 0) {
            return userArgs;
        }

        int usersize = (userArgs != null) ? userArgs.length : 0;
        String [] combined = new String[presize + usersize];
        for (int i = 0; i < presize; i++) {
            combined[i] = prepend.get(i);
        }
        for (int i = 0; i < usersize; i++) {
            combined[presize + i] = userArgs[i];
        }
        return combined;
    }

    private Cursor doAudioSearch(SQLiteDatabase db, SQLiteQueryBuilder qb,
            Uri uri, String[] projectionIn, String selection,
            String[] selectionArgs, String sort, int mode,
            String limit) {

        String mSearchString = uri.getPath().endsWith("/") ? "" : uri.getLastPathSegment();
        mSearchString = mSearchString.replaceAll("  ", " ").trim().toLowerCase();

        String [] searchWords = mSearchString.length() > 0 ?
                mSearchString.split(" ") : new String[0];
        String [] wildcardWords = new String[searchWords.length];
        int len = searchWords.length;
        for (int i = 0; i < len; i++) {
            // Because we match on individual words here, we need to remove words
            // like 'a' and 'the' that aren't part of the keys.
            String key = MediaStore.Audio.keyFor(searchWords[i]);
            key = key.replace("\\", "\\\\");
            key = key.replace("%", "\\%");
            key = key.replace("_", "\\_");
            wildcardWords[i] =
                (searchWords[i].equals("a") || searchWords[i].equals("an") ||
                        searchWords[i].equals("the")) ? "%" : "%" + key + "%";
        }

        String where = "";
        for (int i = 0; i < searchWords.length; i++) {
            if (i == 0) {
                where = "match LIKE ? ESCAPE '\\'";
            } else {
                where += " AND match LIKE ? ESCAPE '\\'";
            }
        }

        qb.setTables("search");
        String [] cols;
        if (mode == AUDIO_SEARCH_FANCY) {
            cols = mSearchColsFancy;
        } else if (mode == AUDIO_SEARCH_BASIC) {
            cols = mSearchColsBasic;
        } else {
            cols = mSearchColsLegacy;
        }
        return qb.query(db, cols, where, wildcardWords, null, null, null, limit);
    }

    @Override
    public String getType(Uri url)
    {
        switch (URI_MATCHER.match(url)) {
            case IMAGES_MEDIA_ID:
            case AUDIO_MEDIA_ID:
            case AUDIO_PLAYLISTS_ID_MEMBERS_ID:
            case VIDEO_MEDIA_ID:
            case FILES_ID:
                Cursor c = null;
                try {
                    c = query(url, MIME_TYPE_PROJECTION, null, null, null);
                    if (c != null && c.getCount() == 1) {
                        c.moveToFirst();
                        String mimeType = c.getString(1);
                        c.deactivate();
                        return mimeType;
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
                break;

            case IMAGES_MEDIA:
            case IMAGES_THUMBNAILS:
                return Images.Media.CONTENT_TYPE;
            case AUDIO_ALBUMART_ID:
            case IMAGES_THUMBNAILS_ID:
                return "image/jpeg";

            case AUDIO_MEDIA:
            case AUDIO_GENRES_ID_MEMBERS:
            case AUDIO_PLAYLISTS_ID_MEMBERS:
                return Audio.Media.CONTENT_TYPE;

            case AUDIO_GENRES:
            case AUDIO_MEDIA_ID_GENRES:
                return Audio.Genres.CONTENT_TYPE;
            case AUDIO_GENRES_ID:
            case AUDIO_MEDIA_ID_GENRES_ID:
                return Audio.Genres.ENTRY_CONTENT_TYPE;
            case AUDIO_PLAYLISTS:
            case AUDIO_MEDIA_ID_PLAYLISTS:
                return Audio.Playlists.CONTENT_TYPE;
            case AUDIO_PLAYLISTS_ID:
            case AUDIO_MEDIA_ID_PLAYLISTS_ID:
                return Audio.Playlists.ENTRY_CONTENT_TYPE;

            case VIDEO_MEDIA:
                return Video.Media.CONTENT_TYPE;
        }
        throw new IllegalStateException("Unknown URL : " + url);
    }

    /**
     * Ensures there is a file in the _data column of values, if one isn't
     * present a new filename is generated. The file itself is not created.
     *
     * @param initialValues the values passed to insert by the caller
     * @return the new values
     */
    private ContentValues ensureFile(boolean internal, ContentValues initialValues,
            String preferredExtension, String directoryName) {
        ContentValues values;
        String file = initialValues.getAsString(MediaStore.MediaColumns.DATA);
        if (TextUtils.isEmpty(file)) {
            file = generateFileName(internal, preferredExtension, directoryName);
            values = new ContentValues(initialValues);
            values.put(MediaStore.MediaColumns.DATA, file);
        } else {
            values = initialValues;
        }

        // we used to create the file here, but now defer this until openFile() is called
        return values;
    }

    private void sendObjectAdded(long objectHandle) {
        synchronized (mMtpServiceConnection) {
            if (mMtpService != null) {
                try {
                    mMtpService.sendObjectAdded((int)objectHandle);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in sendObjectAdded", e);
                    mMtpService = null;
                }
            }
        }
    }

    private void sendObjectRemoved(long objectHandle) {
        synchronized (mMtpServiceConnection) {
            if (mMtpService != null) {
                try {
                    mMtpService.sendObjectRemoved((int)objectHandle);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in sendObjectRemoved", e);
                    mMtpService = null;
                }
            }
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues values[]) {
        int match = URI_MATCHER.match(uri);
        if (match == VOLUMES) {
            return super.bulkInsert(uri, values);
        }
        DatabaseHelper helper = getDatabaseForUri(uri);
        if (helper == null) {
            throw new UnsupportedOperationException(
                    "Unknown URI: " + uri);
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        if (db == null) {
            throw new IllegalStateException("Couldn't open database for " + uri);
        }

        if (match == AUDIO_PLAYLISTS_ID || match == AUDIO_PLAYLISTS_ID_MEMBERS) {
            return playlistBulkInsert(db, uri, values);
        } else if (match == MTP_OBJECT_REFERENCES) {
            int handle = Integer.parseInt(uri.getPathSegments().get(2));
            return setObjectReferences(helper, db, handle, values);
        }


        db.beginTransaction();
        ArrayList<Long> notifyRowIds = new ArrayList<Long>();
        int numInserted = 0;
        try {
            int len = values.length;
            for (int i = 0; i < len; i++) {
                if (values[i] != null) {
                    insertInternal(uri, match, values[i], notifyRowIds);
                }
            }
            numInserted = len;
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // Notify MTP (outside of successful transaction)
        notifyMtp(notifyRowIds);

        getContext().getContentResolver().notifyChange(uri, null);
        return numInserted;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        int match = URI_MATCHER.match(uri);

        ArrayList<Long> notifyRowIds = new ArrayList<Long>();
        Uri newUri = insertInternal(uri, match, initialValues, notifyRowIds);
        notifyMtp(notifyRowIds);

        // do not signal notification for MTP objects.
        // we will signal instead after file transfer is successful.
        if (newUri != null && match != MTP_OBJECTS) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return newUri;
    }

    private void notifyMtp(ArrayList<Long> rowIds) {
        int size = rowIds.size();
        for (int i = 0; i < size; i++) {
            sendObjectAdded(rowIds.get(i).longValue());
        }
    }

    private int playlistBulkInsert(SQLiteDatabase db, Uri uri, ContentValues values[]) {
        DatabaseUtils.InsertHelper helper =
            new DatabaseUtils.InsertHelper(db, "audio_playlists_map");
        int audioidcolidx = helper.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
        int playlistididx = helper.getColumnIndex(Audio.Playlists.Members.PLAYLIST_ID);
        int playorderidx = helper.getColumnIndex(MediaStore.Audio.Playlists.Members.PLAY_ORDER);
        long playlistId = Long.parseLong(uri.getPathSegments().get(3));

        db.beginTransaction();
        int numInserted = 0;
        try {
            int len = values.length;
            for (int i = 0; i < len; i++) {
                helper.prepareForInsert();
                // getting the raw Object and converting it long ourselves saves
                // an allocation (the alternative is ContentValues.getAsLong, which
                // returns a Long object)
                long audioid = ((Number) values[i].get(
                        MediaStore.Audio.Playlists.Members.AUDIO_ID)).longValue();
                helper.bind(audioidcolidx, audioid);
                helper.bind(playlistididx, playlistId);
                // convert to int ourselves to save an allocation.
                int playorder = ((Number) values[i].get(
                        MediaStore.Audio.Playlists.Members.PLAY_ORDER)).intValue();
                helper.bind(playorderidx, playorder);
                helper.execute();
            }
            numInserted = len;
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            helper.close();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return numInserted;
    }

    private long insertDirectory(DatabaseHelper helper, SQLiteDatabase db, String path) {
        if (LOCAL_LOGV) Log.v(TAG, "inserting directory " + path);
        ContentValues values = new ContentValues();
        values.put(FileColumns.FORMAT, MtpConstants.FORMAT_ASSOCIATION);
        values.put(FileColumns.DATA, path);
        values.put(FileColumns.PARENT, getParent(helper, db, path));
        values.put(FileColumns.STORAGE_ID, getStorageId(path));
        File file = new File(path);
        if (file.exists()) {
            values.put(FileColumns.DATE_MODIFIED, file.lastModified() / 1000);
        }
        helper.mNumInserts++;
        long rowId = db.insert("files", FileColumns.DATE_MODIFIED, values);
        sendObjectAdded(rowId);
        return rowId;
    }

    private long getParent(DatabaseHelper helper, SQLiteDatabase db, String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            String parentPath = path.substring(0, lastSlash);
            for (int i = 0; i < mExternalStoragePaths.length; i++) {
                if (parentPath.equals(mExternalStoragePaths[i])) {
                    return 0;
                }
            }
            Long cid = mDirectoryCache.get(parentPath);
            if (cid != null) {
                if (LOCAL_LOGV) Log.v(TAG, "Returning cached entry for " + parentPath);
                return cid;
            }

            String selection = MediaStore.MediaColumns.DATA + "=?";
            String [] selargs = { parentPath };
            helper.mNumQueries++;
            Cursor c = db.query("files", sIdOnlyColumn, selection, selargs, null, null, null);
            try {
                long id;
                if (c == null || c.getCount() == 0) {
                    // parent isn't in the database - so add it
                    id = insertDirectory(helper, db, parentPath);
                    if (LOCAL_LOGV) Log.v(TAG, "Inserted " + parentPath);
                } else {
                    if (c.getCount() > 1) {
                        Log.e(TAG, "more than one match for " + parentPath);
                    }
                    c.moveToFirst();
                    id = c.getLong(0);
                    if (LOCAL_LOGV) Log.v(TAG, "Queried " + parentPath);
                }
                mDirectoryCache.put(parentPath, id);
                return id;
            } finally {
                if (c != null) c.close();
            }
        } else {
            return 0;
        }
    }

    private int getStorageId(String path) {
        for (int i = 0; i < mExternalStoragePaths.length; i++) {
            String test = mExternalStoragePaths[i];
            if (path.startsWith(test)) {
                int length = test.length();
                if (path.length() == length || path.charAt(length) == '/') {
                    return MtpStorage.getStorageId(i);
                }
            }
        }
        // default to primary storage
        return MtpStorage.getStorageId(0);
    }

    private long insertFile(DatabaseHelper helper, Uri uri, ContentValues initialValues, int mediaType,
                            boolean notify, ArrayList<Long> notifyRowIds) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = null;

        switch (mediaType) {
            case FileColumns.MEDIA_TYPE_IMAGE: {
                values = ensureFile(helper.mInternal, initialValues, ".jpg", "DCIM/Camera");

                values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
                String data = values.getAsString(MediaColumns.DATA);
                if (! values.containsKey(MediaColumns.DISPLAY_NAME)) {
                    computeDisplayName(data, values);
                }
                computeTakenTime(values);
                break;
            }

            case FileColumns.MEDIA_TYPE_AUDIO: {
                // SQLite Views are read-only, so we need to deconstruct this
                // insert and do inserts into the underlying tables.
                // If doing this here turns out to be a performance bottleneck,
                // consider moving this to native code and using triggers on
                // the view.
                values = new ContentValues(initialValues);

                String albumartist = values.getAsString(MediaStore.Audio.Media.ALBUM_ARTIST);
                String compilation = values.getAsString(MediaStore.Audio.Media.COMPILATION);
                values.remove(MediaStore.Audio.Media.COMPILATION);

                // Insert the artist into the artist table and remove it from
                // the input values
                Object so = values.get("artist");
                String s = (so == null ? "" : so.toString());
                values.remove("artist");
                long artistRowId;
                HashMap<String, Long> artistCache = helper.mArtistCache;
                String path = values.getAsString(MediaStore.MediaColumns.DATA);
                synchronized(artistCache) {
                    Long temp = artistCache.get(s);
                    if (temp == null) {
                        artistRowId = getKeyIdForName(helper, db,
                                "artists", "artist_key", "artist",
                                s, s, path, 0, null, artistCache, uri);
                    } else {
                        artistRowId = temp.longValue();
                    }
                }
                String artist = s;

                // Do the same for the album field
                so = values.get("album");
                s = (so == null ? "" : so.toString());
                values.remove("album");
                long albumRowId;
                HashMap<String, Long> albumCache = helper.mAlbumCache;
                synchronized(albumCache) {
                    int albumhash = 0;
                    if (albumartist != null) {
                        albumhash = albumartist.hashCode();
                    } else if (compilation != null && compilation.equals("1")) {
                        // nothing to do, hash already set
                    } else {
                        albumhash = path.substring(0, path.lastIndexOf('/')).hashCode();
                    }
                    String cacheName = s + albumhash;
                    Long temp = albumCache.get(cacheName);
                    if (temp == null) {
                        albumRowId = getKeyIdForName(helper, db,
                                "albums", "album_key", "album",
                                s, cacheName, path, albumhash, artist, albumCache, uri);
                    } else {
                        albumRowId = temp;
                    }
                }

                values.put("artist_id", Integer.toString((int)artistRowId));
                values.put("album_id", Integer.toString((int)albumRowId));
                so = values.getAsString("title");
                s = (so == null ? "" : so.toString());
                values.put("title_key", MediaStore.Audio.keyFor(s));
                // do a final trim of the title, in case it started with the special
                // "sort first" character (ascii \001)
                values.remove("title");
                values.put("title", s.trim());

                computeDisplayName(values.getAsString(MediaStore.MediaColumns.DATA), values);
                break;
            }

            case FileColumns.MEDIA_TYPE_VIDEO: {
                values = ensureFile(helper.mInternal, initialValues, ".3gp", "video");
                String data = values.getAsString(MediaStore.MediaColumns.DATA);
                computeDisplayName(data, values);
                computeTakenTime(values);
                break;
            }
        }

        if (values == null) {
            values = new ContentValues(initialValues);
        }
        // compute bucket_id and bucket_display_name for all files
        String path = values.getAsString(MediaStore.MediaColumns.DATA);
        if (path != null) {
            computeBucketValues(path, values);
        }
        values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);

        long rowId = 0;
        Integer i = values.getAsInteger(
                MediaStore.MediaColumns.MEDIA_SCANNER_NEW_OBJECT_ID);
        if (i != null) {
            rowId = i.intValue();
            values = new ContentValues(values);
            values.remove(MediaStore.MediaColumns.MEDIA_SCANNER_NEW_OBJECT_ID);
        }

        String title = values.getAsString(MediaStore.MediaColumns.TITLE);
        if (title == null && path != null) {
            title = MediaFile.getFileTitle(path);
        }
        values.put(FileColumns.TITLE, title);

        String mimeType = values.getAsString(MediaStore.MediaColumns.MIME_TYPE);
        Integer formatObject = values.getAsInteger(FileColumns.FORMAT);
        int format = (formatObject == null ? 0 : formatObject.intValue());
        if (format == 0) {
            if (TextUtils.isEmpty(path)) {
                // special case device created playlists
                if (mediaType == FileColumns.MEDIA_TYPE_PLAYLIST) {
                    values.put(FileColumns.FORMAT, MtpConstants.FORMAT_ABSTRACT_AV_PLAYLIST);
                    // create a file path for the benefit of MTP
                    path = mExternalStoragePaths[0]
                            + "/Playlists/" + values.getAsString(Audio.Playlists.NAME);
                    values.put(MediaStore.MediaColumns.DATA, path);
                    values.put(FileColumns.PARENT, getParent(helper, db, path));
                } else {
                    Log.e(TAG, "path is empty in insertFile()");
                }
            } else {
                format = MediaFile.getFormatCode(path, mimeType);
            }
        }
        if (format != 0) {
            values.put(FileColumns.FORMAT, format);
            if (mimeType == null) {
                mimeType = MediaFile.getMimeTypeForFormatCode(format);
            }
        }

        if (mimeType == null && path != null) {
            mimeType = MediaFile.getMimeTypeForFile(path);
        }
        if (mimeType != null) {
            values.put(FileColumns.MIME_TYPE, mimeType);

            if (mediaType == FileColumns.MEDIA_TYPE_NONE && !MediaScanner.isNoMediaPath(path)) {
                int fileType = MediaFile.getFileTypeForMimeType(mimeType);
                if (MediaFile.isAudioFileType(fileType)) {
                    mediaType = FileColumns.MEDIA_TYPE_AUDIO;
                } else if (MediaFile.isVideoFileType(fileType)) {
                    mediaType = FileColumns.MEDIA_TYPE_VIDEO;
                } else if (MediaFile.isImageFileType(fileType)) {
                    mediaType = FileColumns.MEDIA_TYPE_IMAGE;
                } else if (MediaFile.isPlayListFileType(fileType)) {
                    mediaType = FileColumns.MEDIA_TYPE_PLAYLIST;
                }
            }
        }
        values.put(FileColumns.MEDIA_TYPE, mediaType);

        if (rowId == 0) {
            if (mediaType == FileColumns.MEDIA_TYPE_PLAYLIST) {
                String name = values.getAsString(Audio.Playlists.NAME);
                if (name == null && path == null) {
                    // MediaScanner will compute the name from the path if we have one
                    throw new IllegalArgumentException(
                            "no name was provided when inserting abstract playlist");
                }
            } else {
                if (path == null) {
                    // path might be null for playlists created on the device
                    // or transfered via MTP
                    throw new IllegalArgumentException(
                            "no path was provided when inserting new file");
                }
            }

            // make sure modification date and size are set
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    values.put(FileColumns.DATE_MODIFIED, file.lastModified() / 1000);
                    if (!values.containsKey(FileColumns.SIZE)) {
                        values.put(FileColumns.SIZE, file.length());
                    }
                    // make sure date taken time is set
                    if (mediaType == FileColumns.MEDIA_TYPE_IMAGE
                            || mediaType == FileColumns.MEDIA_TYPE_VIDEO) {
                        computeTakenTime(values);
                    }
                }
            }

            Long parent = values.getAsLong(FileColumns.PARENT);
            if (parent == null) {
                if (path != null) {
                    long parentId = getParent(helper, db, path);
                    values.put(FileColumns.PARENT, parentId);
                }
            }
            Integer storage = values.getAsInteger(FileColumns.STORAGE_ID);
            if (storage == null) {
                int storageId = getStorageId(path);
                values.put(FileColumns.STORAGE_ID, storageId);
            }

            helper.mNumInserts++;
            rowId = db.insert("files", FileColumns.DATE_MODIFIED, values);
            if (LOCAL_LOGV) Log.v(TAG, "insertFile: values=" + values + " returned: " + rowId);

            if (rowId != -1 && notify) {
                notifyRowIds.add(rowId);
            }
        } else {
            helper.mNumUpdates++;
            db.update("files", values, FileColumns._ID + "=?",
                    new String[] { Long.toString(rowId) });
        }
        if (format == MtpConstants.FORMAT_ASSOCIATION) {
            mDirectoryCache.put(path, rowId);
        }

        return rowId;
    }

    private Cursor getObjectReferences(DatabaseHelper helper, SQLiteDatabase db, int handle) {
        helper.mNumQueries++;
        Cursor c = db.query("files", sMediaTableColumns, "_id=?",
                new String[] {  Integer.toString(handle) },
                null, null, null);
        try {
            if (c != null && c.moveToNext()) {
                long playlistId = c.getLong(0);
                int mediaType = c.getInt(1);
                if (mediaType != FileColumns.MEDIA_TYPE_PLAYLIST) {
                    // we only support object references for playlist objects
                    return null;
                }
                helper.mNumQueries++;
                return db.rawQuery(OBJECT_REFERENCES_QUERY,
                        new String[] { Long.toString(playlistId) } );
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    private int setObjectReferences(DatabaseHelper helper, SQLiteDatabase db,
            int handle, ContentValues values[]) {
        // first look up the media table and media ID for the object
        long playlistId = 0;
        helper.mNumQueries++;
        Cursor c = db.query("files", sMediaTableColumns, "_id=?",
                new String[] {  Integer.toString(handle) },
                null, null, null);
        try {
            if (c != null && c.moveToNext()) {
                int mediaType = c.getInt(1);
                if (mediaType != FileColumns.MEDIA_TYPE_PLAYLIST) {
                    // we only support object references for playlist objects
                    return 0;
                }
                playlistId = c.getLong(0);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        if (playlistId == 0) {
            return 0;
        }

        // next delete any existing entries
        helper.mNumDeletes++;
        db.delete("audio_playlists_map", "playlist_id=?",
                new String[] { Long.toString(playlistId) });

        // finally add the new entries
        int count = values.length;
        int added = 0;
        ContentValues[] valuesList = new ContentValues[count];
        for (int i = 0; i < count; i++) {
            // convert object ID to audio ID
            long audioId = 0;
            long objectId = values[i].getAsLong(MediaStore.MediaColumns._ID);
            helper.mNumQueries++;
            c = db.query("files", sMediaTableColumns, "_id=?",
                    new String[] {  Long.toString(objectId) },
                    null, null, null);
            try {
                if (c != null && c.moveToNext()) {
                    int mediaType = c.getInt(1);
                    if (mediaType != FileColumns.MEDIA_TYPE_AUDIO) {
                        // we only allow audio files in playlists, so skip
                        continue;
                    }
                    audioId = c.getLong(0);
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            if (audioId != 0) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Audio.Playlists.Members.PLAYLIST_ID, playlistId);
                v.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
                v.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, added);
                valuesList[added++] = v;
            }
        }
        if (added < count) {
            // we weren't able to find everything on the list, so lets resize the array
            // and pass what we have.
            ContentValues[] newValues = new ContentValues[added];
            System.arraycopy(valuesList, 0, newValues, 0, added);
            valuesList = newValues;
        }
        return playlistBulkInsert(db,
                Audio.Playlists.Members.getContentUri(EXTERNAL_VOLUME, playlistId),
                valuesList);
    }

    private static final String[] GENRE_LOOKUP_PROJECTION = new String[] {
            Audio.Genres._ID, // 0
            Audio.Genres.NAME, // 1
    };

    private void updateGenre(long rowId, String genre) {
        Uri uri = null;
        Cursor cursor = null;
        Uri genresUri = MediaStore.Audio.Genres.getContentUri("external");
        try {
            // see if the genre already exists
            cursor = query(genresUri, GENRE_LOOKUP_PROJECTION, MediaStore.Audio.Genres.NAME + "=?",
                            new String[] { genre }, null);
            if (cursor == null || cursor.getCount() == 0) {
                // genre does not exist, so create the genre in the genre table
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Genres.NAME, genre);
                uri = insert(genresUri, values);
            } else {
                // genre already exists, so compute its Uri
                cursor.moveToNext();
                uri = ContentUris.withAppendedId(genresUri, cursor.getLong(0));
            }
            if (uri != null) {
                uri = Uri.withAppendedPath(uri, MediaStore.Audio.Genres.Members.CONTENT_DIRECTORY);
            }
        } finally {
            // release the cursor if it exists
            if (cursor != null) {
                cursor.close();
            }
        }

        if (uri != null) {
            // add entry to audio_genre_map
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Genres.Members.AUDIO_ID, Long.valueOf(rowId));
            insert(uri, values);
        }
    }

    private Uri insertInternal(Uri uri, int match, ContentValues initialValues,
                               ArrayList<Long> notifyRowIds) {
        final String volumeName = getVolumeName(uri);

        long rowId;

        if (LOCAL_LOGV) Log.v(TAG, "insertInternal: "+uri+", initValues="+initialValues);
        // handle MEDIA_SCANNER before calling getDatabaseForUri()
        if (match == MEDIA_SCANNER) {
            mMediaScannerVolume = initialValues.getAsString(MediaStore.MEDIA_SCANNER_VOLUME);
            DatabaseHelper database = getDatabaseForUri(
                    Uri.parse("content://media/" + mMediaScannerVolume + "/audio"));
            if (database == null) {
                Log.w(TAG, "no database for scanned volume " + mMediaScannerVolume);
            } else {
                database.mScanStartTime = SystemClock.currentTimeMicro();
            }
            return MediaStore.getMediaScannerUri();
        }

        String genre = null;
        String path = null;
        if (initialValues != null) {
            genre = initialValues.getAsString(Audio.AudioColumns.GENRE);
            initialValues.remove(Audio.AudioColumns.GENRE);
            path = initialValues.getAsString(MediaStore.MediaColumns.DATA);
        }


        Uri newUri = null;
        DatabaseHelper helper = getDatabaseForUri(uri);
        if (helper == null && match != VOLUMES && match != MTP_CONNECTED) {
            throw new UnsupportedOperationException(
                    "Unknown URI: " + uri);
        }

        SQLiteDatabase db = ((match == VOLUMES || match == MTP_CONNECTED) ? null
                : helper.getWritableDatabase());

        switch (match) {
            case IMAGES_MEDIA: {
                rowId = insertFile(helper, uri, initialValues,
                        FileColumns.MEDIA_TYPE_IMAGE, true, notifyRowIds);
                if (rowId > 0) {
                    MediaDocumentsProvider.onMediaStoreInsert(
                            getContext(), volumeName, FileColumns.MEDIA_TYPE_IMAGE, rowId);
                    newUri = ContentUris.withAppendedId(
                            Images.Media.getContentUri(volumeName), rowId);
                }
                break;
            }

            // This will be triggered by requestMediaThumbnail (see getThumbnailUri)
            case IMAGES_THUMBNAILS: {
                ContentValues values = ensureFile(helper.mInternal, initialValues, ".jpg",
                        "DCIM/.thumbnails");
                helper.mNumInserts++;
                rowId = db.insert("thumbnails", "name", values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(Images.Thumbnails.
                            getContentUri(volumeName), rowId);
                }
                break;
            }

            // This is currently only used by MICRO_KIND video thumbnail (see getThumbnailUri)
            case VIDEO_THUMBNAILS: {
                ContentValues values = ensureFile(helper.mInternal, initialValues, ".jpg",
                        "DCIM/.thumbnails");
                helper.mNumInserts++;
                rowId = db.insert("videothumbnails", "name", values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(Video.Thumbnails.
                            getContentUri(volumeName), rowId);
                }
                break;
            }

            case AUDIO_MEDIA: {
                rowId = insertFile(helper, uri, initialValues,
                        FileColumns.MEDIA_TYPE_AUDIO, true, notifyRowIds);
                if (rowId > 0) {
                    MediaDocumentsProvider.onMediaStoreInsert(
                            getContext(), volumeName, FileColumns.MEDIA_TYPE_AUDIO, rowId);
                    newUri = ContentUris.withAppendedId(
                            Audio.Media.getContentUri(volumeName), rowId);
                    if (genre != null) {
                        updateGenre(rowId, genre);
                    }
                }
                break;
            }

            case AUDIO_MEDIA_ID_GENRES: {
                Long audioId = Long.parseLong(uri.getPathSegments().get(2));
                ContentValues values = new ContentValues(initialValues);
                values.put(Audio.Genres.Members.AUDIO_ID, audioId);
                helper.mNumInserts++;
                rowId = db.insert("audio_genres_map", "genre_id", values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(uri, rowId);
                }
                break;
            }

            case AUDIO_MEDIA_ID_PLAYLISTS: {
                Long audioId = Long.parseLong(uri.getPathSegments().get(2));
                ContentValues values = new ContentValues(initialValues);
                values.put(Audio.Playlists.Members.AUDIO_ID, audioId);
                helper.mNumInserts++;
                rowId = db.insert("audio_playlists_map", "playlist_id",
                        values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(uri, rowId);
                }
                break;
            }

            case AUDIO_GENRES: {
                helper.mNumInserts++;
                rowId = db.insert("audio_genres", "audio_id", initialValues);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(
                            Audio.Genres.getContentUri(volumeName), rowId);
                }
                break;
            }

            case AUDIO_GENRES_ID_MEMBERS: {
                Long genreId = Long.parseLong(uri.getPathSegments().get(3));
                ContentValues values = new ContentValues(initialValues);
                values.put(Audio.Genres.Members.GENRE_ID, genreId);
                helper.mNumInserts++;
                rowId = db.insert("audio_genres_map", "genre_id", values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(uri, rowId);
                }
                break;
            }

            case AUDIO_PLAYLISTS: {
                ContentValues values = new ContentValues(initialValues);
                values.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis() / 1000);
                rowId = insertFile(helper, uri, values,
                        FileColumns.MEDIA_TYPE_PLAYLIST, true, notifyRowIds);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(
                            Audio.Playlists.getContentUri(volumeName), rowId);
                }
                break;
            }

            case AUDIO_PLAYLISTS_ID:
            case AUDIO_PLAYLISTS_ID_MEMBERS: {
                Long playlistId = Long.parseLong(uri.getPathSegments().get(3));
                ContentValues values = new ContentValues(initialValues);
                values.put(Audio.Playlists.Members.PLAYLIST_ID, playlistId);
                helper.mNumInserts++;
                rowId = db.insert("audio_playlists_map", "playlist_id", values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(uri, rowId);
                }
                break;
            }

            case VIDEO_MEDIA: {
                rowId = insertFile(helper, uri, initialValues,
                        FileColumns.MEDIA_TYPE_VIDEO, true, notifyRowIds);
                if (rowId > 0) {
                    MediaDocumentsProvider.onMediaStoreInsert(
                            getContext(), volumeName, FileColumns.MEDIA_TYPE_VIDEO, rowId);
                    newUri = ContentUris.withAppendedId(
                            Video.Media.getContentUri(volumeName), rowId);
                }
                break;
            }

            case AUDIO_ALBUMART: {
                if (helper.mInternal) {
                    throw new UnsupportedOperationException("no internal album art allowed");
                }
                ContentValues values = null;
                try {
                    values = ensureFile(false, initialValues, "", ALBUM_THUMB_FOLDER);
                } catch (IllegalStateException ex) {
                    // probably no more room to store albumthumbs
                    values = initialValues;
                }
                helper.mNumInserts++;
                rowId = db.insert("album_art", MediaStore.MediaColumns.DATA, values);
                if (rowId > 0) {
                    newUri = ContentUris.withAppendedId(uri, rowId);
                }
                break;
            }

            case VOLUMES:
            {
                String name = initialValues.getAsString("name");
                Uri attachedVolume = attachVolume(name);
                if (mMediaScannerVolume != null && mMediaScannerVolume.equals(name)) {
                    DatabaseHelper dbhelper = getDatabaseForUri(attachedVolume);
                    if (dbhelper == null) {
                        Log.e(TAG, "no database for attached volume " + attachedVolume);
                    } else {
                        dbhelper.mScanStartTime = SystemClock.currentTimeMicro();
                    }
                }
                return attachedVolume;
            }

            case MTP_CONNECTED:
                synchronized (mMtpServiceConnection) {
                    if (mMtpService == null) {
                        Context context = getContext();
                        // MTP is connected, so grab a connection to MtpService
                        context.bindService(new Intent(context, MtpService.class),
                                mMtpServiceConnection, Context.BIND_AUTO_CREATE);
                    }
                }
                break;

            case FILES:
                rowId = insertFile(helper, uri, initialValues,
                        FileColumns.MEDIA_TYPE_NONE, true, notifyRowIds);
                if (rowId > 0) {
                    newUri = Files.getContentUri(volumeName, rowId);
                }
                break;

            case MTP_OBJECTS:
                // We don't send a notification if the insert originated from MTP
                rowId = insertFile(helper, uri, initialValues,
                        FileColumns.MEDIA_TYPE_NONE, false, notifyRowIds);
                if (rowId > 0) {
                    newUri = Files.getMtpObjectsUri(volumeName, rowId);
                }
                break;

            default:
                throw new UnsupportedOperationException("Invalid URI " + uri);
        }

        if (path != null && path.toLowerCase(Locale.US).endsWith("/.nomedia")) {
            // need to set the media_type of all the files below this folder to 0
            processNewNoMediaPath(helper, db, path);
        }
        return newUri;
    }

    /*
     * Sets the media type of all files below the newly added .nomedia file or
     * hidden folder to 0, so the entries no longer appear in e.g. the audio and
     * images views.
     *
     * @param path The path to the new .nomedia file or hidden directory
     */
    private void processNewNoMediaPath(final DatabaseHelper helper, final SQLiteDatabase db,
            final String path) {
        final File nomedia = new File(path);
        if (nomedia.exists()) {
            hidePath(helper, db, path);
        } else {
            // File doesn't exist. Try again in a little while.
            // XXX there's probably a better way of doing this
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SystemClock.sleep(2000);
                    if (nomedia.exists()) {
                        hidePath(helper, db, path);
                    } else {
                        Log.w(TAG, "does not exist: " + path, new Exception());
                    }
                }}).start();
        }
    }

    private void hidePath(DatabaseHelper helper, SQLiteDatabase db, String path) {
        File nomedia = new File(path);
        String hiddenroot = nomedia.isDirectory() ? path : nomedia.getParent();
        ContentValues mediatype = new ContentValues();
        mediatype.put("media_type", 0);
        int numrows = db.update("files", mediatype,
                "_data >= ? AND _data < ?",
                new String[] { hiddenroot  + "/", hiddenroot + "0"});
        helper.mNumUpdates += numrows;
        ContentResolver res = getContext().getContentResolver();
        res.notifyChange(Uri.parse("content://media/"), null);
    }

    /*
     * Rescan files for missing metadata and set their type accordingly.
     * There is code for detecting the removal of a nomedia file or renaming of
     * a directory from hidden to non-hidden in the MediaScanner and MtpDatabase,
     * both of which call here.
     */
    private void processRemovedNoMediaPath(final String path) {
        final DatabaseHelper helper;
        if (path.startsWith(mExternalStoragePaths[0])) {
            helper = getDatabaseForUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        } else {
            helper = getDatabaseForUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        new ScannerClient(getContext(), db, path);
    }

    private static final class ScannerClient implements MediaScannerConnectionClient {
        String mPath = null;
        MediaScannerConnection mScannerConnection;
        SQLiteDatabase mDb;

        public ScannerClient(Context context, SQLiteDatabase db, String path) {
            mDb = db;
            mPath = path;
            mScannerConnection = new MediaScannerConnection(context, this);
            mScannerConnection.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            Cursor c = mDb.query("files", openFileColumns,
                    "_data >= ? AND _data < ?",
                    new String[] { mPath + "/", mPath + "0"},
                    null, null, null);
            while (c.moveToNext()) {
                String d = c.getString(0);
                File f = new File(d);
                if (f.isFile()) {
                    mScannerConnection.scanFile(d, null);
                }
            }
            mScannerConnection.disconnect();
            c.close();
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
                throws OperationApplicationException {

        // The operations array provides no overall information about the URI(s) being operated
        // on, so begin a transaction for ALL of the databases.
        DatabaseHelper ihelper = getDatabaseForUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
        DatabaseHelper ehelper = getDatabaseForUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        SQLiteDatabase idb = ihelper.getWritableDatabase();
        idb.beginTransaction();
        SQLiteDatabase edb = null;
        if (ehelper != null) {
            edb = ehelper.getWritableDatabase();
            edb.beginTransaction();
        }
        try {
            ContentProviderResult[] result = super.applyBatch(operations);
            idb.setTransactionSuccessful();
            if (edb != null) {
                edb.setTransactionSuccessful();
            }
            // Rather than sending targeted change notifications for every Uri
            // affected by the batch operation, just invalidate the entire internal
            // and external name space.
            ContentResolver res = getContext().getContentResolver();
            res.notifyChange(Uri.parse("content://media/"), null);
            return result;
        } finally {
            idb.endTransaction();
            if (edb != null) {
                edb.endTransaction();
            }
        }
    }


    private MediaThumbRequest requestMediaThumbnail(String path, Uri uri, int priority, long magic) {
        synchronized (mMediaThumbQueue) {
            MediaThumbRequest req = null;
            try {
                req = new MediaThumbRequest(
                        getContext().getContentResolver(), path, uri, priority, magic);
                mMediaThumbQueue.add(req);
                // Trigger the handler.
                Message msg = mThumbHandler.obtainMessage(IMAGE_THUMB);
                msg.sendToTarget();
            } catch (Throwable t) {
                Log.w(TAG, t);
            }
            return req;
        }
    }

    private String generateFileName(boolean internal, String preferredExtension, String directoryName)
    {
        // create a random file
        String name = String.valueOf(System.currentTimeMillis());

        if (internal) {
            throw new UnsupportedOperationException("Writing to internal storage is not supported.");
//            return Environment.getDataDirectory()
//                + "/" + directoryName + "/" + name + preferredExtension;
        } else {
            return mExternalStoragePaths[0] + "/" + directoryName + "/" + name + preferredExtension;
        }
    }

    private boolean ensureFileExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            return true;
        } else {
            // we will not attempt to create the first directory in the path
            // (for example, do not create /sdcard if the SD card is not mounted)
            int secondSlash = path.indexOf('/', 1);
            if (secondSlash < 1) return false;
            String directoryPath = path.substring(0, secondSlash);
            File directory = new File(directoryPath);
            if (!directory.exists())
                return false;
            file.getParentFile().mkdirs();
            try {
                return file.createNewFile();
            } catch(IOException ioe) {
                Log.e(TAG, "File creation failed", ioe);
            }
            return false;
        }
    }

    private static final class GetTableAndWhereOutParameter {
        public String table;
        public String where;
    }

    static final GetTableAndWhereOutParameter sGetTableAndWhereParam =
            new GetTableAndWhereOutParameter();

    private void getTableAndWhere(Uri uri, int match, String userWhere,
            GetTableAndWhereOutParameter out) {
        String where = null;
        switch (match) {
            case IMAGES_MEDIA:
                out.table = "files";
                where = FileColumns.MEDIA_TYPE + "=" + FileColumns.MEDIA_TYPE_IMAGE;
                break;

            case IMAGES_MEDIA_ID:
                out.table = "files";
                where = "_id = " + uri.getPathSegments().get(3);
                break;

            case IMAGES_THUMBNAILS_ID:
                where = "_id=" + uri.getPathSegments().get(3);
            case IMAGES_THUMBNAILS:
                out.table = "thumbnails";
                break;

            case AUDIO_MEDIA:
                out.table = "files";
                where = FileColumns.MEDIA_TYPE + "=" + FileColumns.MEDIA_TYPE_AUDIO;
                break;

            case AUDIO_MEDIA_ID:
                out.table = "files";
                where = "_id=" + uri.getPathSegments().get(3);
                break;

            case AUDIO_MEDIA_ID_GENRES:
                out.table = "audio_genres";
                where = "audio_id=" + uri.getPathSegments().get(3);
                break;

            case AUDIO_MEDIA_ID_GENRES_ID:
                out.table = "audio_genres";
                where = "audio_id=" + uri.getPathSegments().get(3) +
                        " AND genre_id=" + uri.getPathSegments().get(5);
               break;

            case AUDIO_MEDIA_ID_PLAYLISTS:
                out.table = "audio_playlists";
                where = "audio_id=" + uri.getPathSegments().get(3);
                break;

            case AUDIO_MEDIA_ID_PLAYLISTS_ID:
                out.table = "audio_playlists";
                where = "audio_id=" + uri.getPathSegments().get(3) +
                        " AND playlists_id=" + uri.getPathSegments().get(5);
                break;

            case AUDIO_GENRES:
                out.table = "audio_genres";
                break;

            case AUDIO_GENRES_ID:
                out.table = "audio_genres";
                where = "_id=" + uri.getPathSegments().get(3);
                break;

            case AUDIO_GENRES_ID_MEMBERS:
                out.table = "audio_genres";
                where = "genre_id=" + uri.getPathSegments().get(3);
                break;

            case AUDIO_PLAYLISTS:
                out.table = "files";
                where = FileColumns.MEDIA_TYPE + "=" + FileColumns.MEDIA_TYPE_PLAYLIST;
                break;

            case AUDIO_PLAYLISTS_ID:
                out.table = "files";
                where = "_id=" + uri.getPathSegments().get(3);
                break;

            case AUDIO_PLAYLISTS_ID_MEMBERS:
                out.table = "audio_playlists_map";
                where = "playlist_id=" + uri.getPathSegments().get(3);
                break;

            case AUDIO_PLAYLISTS_ID_MEMBERS_ID:
                out.table = "audio_playlists_map";
                where = "playlist_id=" + uri.getPathSegments().get(3) +
                        " AND _id=" + uri.getPathSegments().get(5);
                break;

            case AUDIO_ALBUMART_ID:
                out.table = "album_art";
                where = "album_id=" + uri.getPathSegments().get(3);
                break;

            case VIDEO_MEDIA:
                out.table = "files";
                where = FileColumns.MEDIA_TYPE + "=" + FileColumns.MEDIA_TYPE_VIDEO;
                break;

            case VIDEO_MEDIA_ID:
                out.table = "files";
                where = "_id=" + uri.getPathSegments().get(3);
                break;

            case VIDEO_THUMBNAILS_ID:
                where = "_id=" + uri.getPathSegments().get(3);
            case VIDEO_THUMBNAILS:
                out.table = "videothumbnails";
                break;

            case FILES_ID:
            case MTP_OBJECTS_ID:
                where = "_id=" + uri.getPathSegments().get(2);
            case FILES:
            case MTP_OBJECTS:
                out.table = "files";
                break;

            default:
                throw new UnsupportedOperationException(
                        "Unknown or unsupported URL: " + uri.toString());
        }

        // Add in the user requested WHERE clause, if needed
        if (!TextUtils.isEmpty(userWhere)) {
            if (!TextUtils.isEmpty(where)) {
                out.where = where + " AND (" + userWhere + ")";
            } else {
                out.where = userWhere;
            }
        } else {
            out.where = where;
        }
    }

    @Override
    public int delete(Uri uri, String userWhere, String[] whereArgs) {
        uri = safeUncanonicalize(uri);
        int count;
        int match = URI_MATCHER.match(uri);

        // handle MEDIA_SCANNER before calling getDatabaseForUri()
        if (match == MEDIA_SCANNER) {
            if (mMediaScannerVolume == null) {
                return 0;
            }
            DatabaseHelper database = getDatabaseForUri(
                    Uri.parse("content://media/" + mMediaScannerVolume + "/audio"));
            if (database == null) {
                Log.w(TAG, "no database for scanned volume " + mMediaScannerVolume);
            } else {
                database.mScanStopTime = SystemClock.currentTimeMicro();
                String msg = dump(database, false);
                logToDb(database.getWritableDatabase(), msg);
            }
            mMediaScannerVolume = null;
            return 1;
        }

        if (match == VOLUMES_ID) {
            detachVolume(uri);
            count = 1;
        } else if (match == MTP_CONNECTED) {
            synchronized (mMtpServiceConnection) {
                if (mMtpService != null) {
                    // MTP has disconnected, so release our connection to MtpService
                    getContext().unbindService(mMtpServiceConnection);
                    count = 1;
                    // mMtpServiceConnection.onServiceDisconnected might not get called,
                    // so set mMtpService = null here
                    mMtpService = null;
                } else {
                    count = 0;
                }
            }
        } else {
            final String volumeName = getVolumeName(uri);

            DatabaseHelper database = getDatabaseForUri(uri);
            if (database == null) {
                throw new UnsupportedOperationException(
                        "Unknown URI: " + uri + " match: " + match);
            }
            database.mNumDeletes++;
            SQLiteDatabase db = database.getWritableDatabase();

            synchronized (sGetTableAndWhereParam) {
                getTableAndWhere(uri, match, userWhere, sGetTableAndWhereParam);
                if (sGetTableAndWhereParam.table.equals("files")) {
                    String deleteparam = uri.getQueryParameter(MediaStore.PARAM_DELETE_DATA);
                    if (deleteparam == null || ! deleteparam.equals("false")) {
                        database.mNumQueries++;
                        Cursor c = db.query(sGetTableAndWhereParam.table,
                                sMediaTypeDataId,
                                sGetTableAndWhereParam.where, whereArgs, null, null, null);
                        String [] idvalue = new String[] { "" };
                        String [] playlistvalues = new String[] { "", "" };
                        while (c.moveToNext()) {
                            final int mediaType = c.getInt(0);
                            final String data = c.getString(1);
                            final long id = c.getLong(2);

                            if (mediaType == FileColumns.MEDIA_TYPE_IMAGE) {
                                deleteIfAllowed(uri, data);
                                MediaDocumentsProvider.onMediaStoreDelete(getContext(), volumeName,
                                        FileColumns.MEDIA_TYPE_IMAGE, id);

                                idvalue[0] = String.valueOf(id);
                                database.mNumQueries++;
                                Cursor cc = db.query("thumbnails", sDataOnlyColumn,
                                        "image_id=?", idvalue, null, null, null);
                                while (cc.moveToNext()) {
                                    deleteIfAllowed(uri, cc.getString(0));
                                }
                                cc.close();
                                database.mNumDeletes++;
                                db.delete("thumbnails", "image_id=?", idvalue);

                            } else if (mediaType == FileColumns.MEDIA_TYPE_VIDEO) {
                                deleteIfAllowed(uri, data);
                                MediaDocumentsProvider.onMediaStoreDelete(getContext(), volumeName,
                                        FileColumns.MEDIA_TYPE_VIDEO, id);

                            } else if (mediaType == FileColumns.MEDIA_TYPE_AUDIO) {
                                if (!database.mInternal) {
                                    MediaDocumentsProvider.onMediaStoreDelete(getContext(),
                                            volumeName, FileColumns.MEDIA_TYPE_AUDIO, id);

                                    idvalue[0] = String.valueOf(id);
                                    database.mNumDeletes += 2; // also count the one below
                                    db.delete("audio_genres_map", "audio_id=?", idvalue);
                                    // for each playlist that the item appears in, move
                                    // all the items behind it forward by one
                                    Cursor cc = db.query("audio_playlists_map",
                                            sPlaylistIdPlayOrder,
                                            "audio_id=?", idvalue, null, null, null);
                                    while (cc.moveToNext()) {
                                        playlistvalues[0] = "" + cc.getLong(0);
                                        playlistvalues[1] = "" + cc.getInt(1);
                                        database.mNumUpdates++;
                                        db.execSQL("UPDATE audio_playlists_map" +
                                                " SET play_order=play_order-1" +
                                                " WHERE playlist_id=? AND play_order>?",
                                                playlistvalues);
                                    }
                                    cc.close();
                                    db.delete("audio_playlists_map", "audio_id=?", idvalue);
                                }
                            } else if (mediaType == FileColumns.MEDIA_TYPE_PLAYLIST) {
                                // TODO, maybe: remove the audio_playlists_cleanup trigger and implement
                                // it functionality here (clean up the playlist map)
                            }
                        }
                        c.close();
                    }
                }

                switch (match) {
                    case MTP_OBJECTS:
                    case MTP_OBJECTS_ID:
                        try {
                            // don't send objectRemoved event since this originated from MTP
                            mDisableMtpObjectCallbacks = true;
                            database.mNumDeletes++;
                            count = db.delete("files", sGetTableAndWhereParam.where, whereArgs);
                        } finally {
                            mDisableMtpObjectCallbacks = false;
                        }
                        break;
                    case AUDIO_GENRES_ID_MEMBERS:
                        database.mNumDeletes++;
                        count = db.delete("audio_genres_map",
                                sGetTableAndWhereParam.where, whereArgs);
                        break;

                    case IMAGES_THUMBNAILS_ID:
                    case IMAGES_THUMBNAILS:
                    case VIDEO_THUMBNAILS_ID:
                    case VIDEO_THUMBNAILS:
                        // Delete the referenced files first.
                        Cursor c = db.query(sGetTableAndWhereParam.table,
                                sDataOnlyColumn,
                                sGetTableAndWhereParam.where, whereArgs, null, null, null);
                        if (c != null) {
                            while (c.moveToNext()) {
                                deleteIfAllowed(uri, c.getString(0));
                            }
                            c.close();
                        }
                        database.mNumDeletes++;
                        count = db.delete(sGetTableAndWhereParam.table,
                                sGetTableAndWhereParam.where, whereArgs);
                        break;

                    default:
                        database.mNumDeletes++;
                        count = db.delete(sGetTableAndWhereParam.table,
                                sGetTableAndWhereParam.where, whereArgs);
                        break;
                }

                // Since there are multiple Uris that can refer to the same files
                // and deletes can affect other objects in storage (like subdirectories
                // or playlists) we will notify a change on the entire volume to make
                // sure no listeners miss the notification.
                Uri notifyUri = Uri.parse("content://" + MediaStore.AUTHORITY + "/" + volumeName);
                getContext().getContentResolver().notifyChange(notifyUri, null);
            }
        }

        return count;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (MediaStore.UNHIDE_CALL.equals(method)) {
            processRemovedNoMediaPath(arg);
            return null;
        }
        throw new UnsupportedOperationException("Unsupported call: " + method);
    }

    @Override
    public int update(Uri uri, ContentValues initialValues, String userWhere,
            String[] whereArgs) {
        uri = safeUncanonicalize(uri);
        int count;
        // Log.v(TAG, "update for uri="+uri+", initValues="+initialValues);
        int match = URI_MATCHER.match(uri);
        DatabaseHelper helper = getDatabaseForUri(uri);
        if (helper == null) {
            throw new UnsupportedOperationException(
                    "Unknown URI: " + uri);
        }
        helper.mNumUpdates++;

        SQLiteDatabase db = helper.getWritableDatabase();

        String genre = null;
        if (initialValues != null) {
            genre = initialValues.getAsString(Audio.AudioColumns.GENRE);
            initialValues.remove(Audio.AudioColumns.GENRE);
        }

        synchronized (sGetTableAndWhereParam) {
            getTableAndWhere(uri, match, userWhere, sGetTableAndWhereParam);

            // special case renaming directories via MTP.
            // in this case we must update all paths in the database with
            // the directory name as a prefix
            if ((match == MTP_OBJECTS || match == MTP_OBJECTS_ID)
                    && initialValues != null && initialValues.size() == 1) {
                String oldPath = null;
                String newPath = initialValues.getAsString(MediaStore.MediaColumns.DATA);
                mDirectoryCache.remove(newPath);
                // MtpDatabase will rename the directory first, so we test the new file name
                File f = new File(newPath);
                if (newPath != null && f.isDirectory()) {
                    helper.mNumQueries++;
                    Cursor cursor = db.query(sGetTableAndWhereParam.table, PATH_PROJECTION,
                        userWhere, whereArgs, null, null, null);
                    try {
                        if (cursor != null && cursor.moveToNext()) {
                            oldPath = cursor.getString(1);
                        }
                    } finally {
                        if (cursor != null) cursor.close();
                    }
                    if (oldPath != null) {
                        mDirectoryCache.remove(oldPath);
                        // first rename the row for the directory
                        helper.mNumUpdates++;
                        count = db.update(sGetTableAndWhereParam.table, initialValues,
                                sGetTableAndWhereParam.where, whereArgs);
                        if (count > 0) {
                            // update the paths of any files and folders contained in the directory
                            Object[] bindArgs = new Object[] {
                                    newPath,
                                    oldPath.length() + 1,
                                    oldPath + "/",
                                    oldPath + "0",
                                    // update bucket_display_name and bucket_id based on new path
                                    f.getName(),
                                    f.toString().toLowerCase().hashCode()
                                    };
                            helper.mNumUpdates++;
                            db.execSQL("UPDATE files SET _data=?1||SUBSTR(_data, ?2)" +
                                    // also update bucket_display_name
                                    ",bucket_display_name=?5" +
                                    ",bucket_id=?6" +
                                    " WHERE _data >= ?3 AND _data < ?4;",
                                    bindArgs);
                        }

                        if (count > 0 && !db.inTransaction()) {
                            getContext().getContentResolver().notifyChange(uri, null);
                        }
                        if (f.getName().startsWith(".")) {
                            // the new directory name is hidden
                            processNewNoMediaPath(helper, db, newPath);
                        }
                        return count;
                    }
                } else if (newPath.toLowerCase(Locale.US).endsWith("/.nomedia")) {
                    processNewNoMediaPath(helper, db, newPath);
                }
            }

            switch (match) {
                case AUDIO_MEDIA:
                case AUDIO_MEDIA_ID:
                    {
                        ContentValues values = new ContentValues(initialValues);
                        String albumartist = values.getAsString(MediaStore.Audio.Media.ALBUM_ARTIST);
                        String compilation = values.getAsString(MediaStore.Audio.Media.COMPILATION);
                        values.remove(MediaStore.Audio.Media.COMPILATION);

                        // Insert the artist into the artist table and remove it from
                        // the input values
                        String artist = values.getAsString("artist");
                        values.remove("artist");
                        if (artist != null) {
                            long artistRowId;
                            HashMap<String, Long> artistCache = helper.mArtistCache;
                            synchronized(artistCache) {
                                Long temp = artistCache.get(artist);
                                if (temp == null) {
                                    artistRowId = getKeyIdForName(helper, db,
                                            "artists", "artist_key", "artist",
                                            artist, artist, null, 0, null, artistCache, uri);
                                } else {
                                    artistRowId = temp.longValue();
                                }
                            }
                            values.put("artist_id", Integer.toString((int)artistRowId));
                        }

                        // Do the same for the album field.
                        String so = values.getAsString("album");
                        values.remove("album");
                        if (so != null) {
                            String path = values.getAsString(MediaStore.MediaColumns.DATA);
                            int albumHash = 0;
                            if (albumartist != null) {
                                albumHash = albumartist.hashCode();
                            } else if (compilation != null && compilation.equals("1")) {
                                // nothing to do, hash already set
                            } else {
                                if (path == null) {
                                    if (match == AUDIO_MEDIA) {
                                        Log.w(TAG, "Possible multi row album name update without"
                                                + " path could give wrong album key");
                                    } else {
                                        //Log.w(TAG, "Specify path to avoid extra query");
                                        Cursor c = query(uri,
                                                new String[] { MediaStore.Audio.Media.DATA},
                                                null, null, null);
                                        if (c != null) {
                                            try {
                                                int numrows = c.getCount();
                                                if (numrows == 1) {
                                                    c.moveToFirst();
                                                    path = c.getString(0);
                                                } else {
                                                    Log.e(TAG, "" + numrows + " rows for " + uri);
                                                }
                                            } finally {
                                                c.close();
                                            }
                                        }
                                    }
                                }
                                if (path != null) {
                                    albumHash = path.substring(0, path.lastIndexOf('/')).hashCode();
                                }
                            }

                            String s = so.toString();
                            long albumRowId;
                            HashMap<String, Long> albumCache = helper.mAlbumCache;
                            synchronized(albumCache) {
                                String cacheName = s + albumHash;
                                Long temp = albumCache.get(cacheName);
                                if (temp == null) {
                                    albumRowId = getKeyIdForName(helper, db,
                                            "albums", "album_key", "album",
                                            s, cacheName, path, albumHash, artist, albumCache, uri);
                                } else {
                                    albumRowId = temp.longValue();
                                }
                            }
                            values.put("album_id", Integer.toString((int)albumRowId));
                        }

                        // don't allow the title_key field to be updated directly
                        values.remove("title_key");
                        // If the title field is modified, update the title_key
                        so = values.getAsString("title");
                        if (so != null) {
                            String s = so.toString();
                            values.put("title_key", MediaStore.Audio.keyFor(s));
                            // do a final trim of the title, in case it started with the special
                            // "sort first" character (ascii \001)
                            values.remove("title");
                            values.put("title", s.trim());
                        }

                        helper.mNumUpdates++;
                        count = db.update(sGetTableAndWhereParam.table, values,
                                sGetTableAndWhereParam.where, whereArgs);
                        if (genre != null) {
                            if (count == 1 && match == AUDIO_MEDIA_ID) {
                                long rowId = Long.parseLong(uri.getPathSegments().get(3));
                                updateGenre(rowId, genre);
                            } else {
                                // can't handle genres for bulk update or for non-audio files
                                Log.w(TAG, "ignoring genre in update: count = "
                                        + count + " match = " + match);
                            }
                        }
                    }
                    break;
                case IMAGES_MEDIA:
                case IMAGES_MEDIA_ID:
                case VIDEO_MEDIA:
                case VIDEO_MEDIA_ID:
                    {
                        ContentValues values = new ContentValues(initialValues);
                        // Don't allow bucket id or display name to be updated directly.
                        // The same names are used for both images and table columns, so
                        // we use the ImageColumns constants here.
                        values.remove(ImageColumns.BUCKET_ID);
                        values.remove(ImageColumns.BUCKET_DISPLAY_NAME);
                        // If the data is being modified update the bucket values
                        String data = values.getAsString(MediaColumns.DATA);
                        if (data != null) {
                            computeBucketValues(data, values);
                        }
                        computeTakenTime(values);
                        helper.mNumUpdates++;
                        count = db.update(sGetTableAndWhereParam.table, values,
                                sGetTableAndWhereParam.where, whereArgs);
                        // if this is a request from MediaScanner, DATA should contains file path
                        // we only process update request from media scanner, otherwise the requests
                        // could be duplicate.
                        if (count > 0 && values.getAsString(MediaStore.MediaColumns.DATA) != null) {
                            helper.mNumQueries++;
                            Cursor c = db.query(sGetTableAndWhereParam.table,
                                    READY_FLAG_PROJECTION, sGetTableAndWhereParam.where,
                                    whereArgs, null, null, null);
                            if (c != null) {
                                try {
                                    while (c.moveToNext()) {
                                        long magic = c.getLong(2);
                                        if (magic == 0) {
                                            requestMediaThumbnail(c.getString(1), uri,
                                                    MediaThumbRequest.PRIORITY_NORMAL, 0);
                                        }
                                    }
                                } finally {
                                    c.close();
                                }
                            }
                        }
                    }
                    break;

                case AUDIO_PLAYLISTS_ID_MEMBERS_ID:
                    String moveit = uri.getQueryParameter("move");
                    if (moveit != null) {
                        String key = MediaStore.Audio.Playlists.Members.PLAY_ORDER;
                        if (initialValues.containsKey(key)) {
                            int newpos = initialValues.getAsInteger(key);
                            List <String> segments = uri.getPathSegments();
                            long playlist = Long.valueOf(segments.get(3));
                            int oldpos = Integer.valueOf(segments.get(5));
                            return movePlaylistEntry(helper, db, playlist, oldpos, newpos);
                        }
                        throw new IllegalArgumentException("Need to specify " + key +
                                " when using 'move' parameter");
                    }
                    // fall through
                default:
                    helper.mNumUpdates++;
                    count = db.update(sGetTableAndWhereParam.table, initialValues,
                        sGetTableAndWhereParam.where, whereArgs);
                    break;
            }
        }
        // in a transaction, the code that began the transaction should be taking
        // care of notifications once it ends the transaction successfully
        if (count > 0 && !db.inTransaction()) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    private int movePlaylistEntry(DatabaseHelper helper, SQLiteDatabase db,
            long playlist, int from, int to) {
        if (from == to) {
            return 0;
        }
        db.beginTransaction();
        int numlines = 0;
        try {
            helper.mNumUpdates += 3;
            Cursor c = db.query("audio_playlists_map",
                    new String [] {"play_order" },
                    "playlist_id=?", new String[] {"" + playlist}, null, null, "play_order",
                    from + ",1");
            c.moveToFirst();
            int from_play_order = c.getInt(0);
            c.close();
            c = db.query("audio_playlists_map",
                    new String [] {"play_order" },
                    "playlist_id=?", new String[] {"" + playlist}, null, null, "play_order",
                    to + ",1");
            c.moveToFirst();
            int to_play_order = c.getInt(0);
            c.close();
            db.execSQL("UPDATE audio_playlists_map SET play_order=-1" +
                    " WHERE play_order=" + from_play_order +
                    " AND playlist_id=" + playlist);
            // We could just run both of the next two statements, but only one of
            // of them will actually do anything, so might as well skip the compile
            // and execute steps.
            if (from  < to) {
                db.execSQL("UPDATE audio_playlists_map SET play_order=play_order-1" +
                        " WHERE play_order<=" + to_play_order +
                        " AND play_order>" + from_play_order +
                        " AND playlist_id=" + playlist);
                numlines = to - from + 1;
            } else {
                db.execSQL("UPDATE audio_playlists_map SET play_order=play_order+1" +
                        " WHERE play_order>=" + to_play_order +
                        " AND play_order<" + from_play_order +
                        " AND playlist_id=" + playlist);
                numlines = from - to + 1;
            }
            db.execSQL("UPDATE audio_playlists_map SET play_order=" + to_play_order +
                    " WHERE play_order=-1 AND playlist_id=" + playlist);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
                .buildUpon().appendEncodedPath(String.valueOf(playlist)).build();
        // notifyChange() must be called after the database transaction is ended
        // or the listeners will read the old data in the callback
        getContext().getContentResolver().notifyChange(uri, null);

        return numlines;
    }

    private static final String[] openFileColumns = new String[] {
        MediaStore.MediaColumns.DATA,
    };

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {

        uri = safeUncanonicalize(uri);
        ParcelFileDescriptor pfd = null;

        if (URI_MATCHER.match(uri) == AUDIO_ALBUMART_FILE_ID) {
            // get album art for the specified media file
            DatabaseHelper database = getDatabaseForUri(uri);
            if (database == null) {
                throw new IllegalStateException("Couldn't open database for " + uri);
            }
            SQLiteDatabase db = database.getReadableDatabase();
            if (db == null) {
                throw new IllegalStateException("Couldn't open database for " + uri);
            }
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            int songid = Integer.parseInt(uri.getPathSegments().get(3));
            qb.setTables("audio_meta");
            qb.appendWhere("_id=" + songid);
            Cursor c = qb.query(db,
                    new String [] {
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.ALBUM_ID },
                    null, null, null, null, null);
            if (c.moveToFirst()) {
                String audiopath = c.getString(0);
                int albumid = c.getInt(1);
                // Try to get existing album art for this album first, which
                // could possibly have been obtained from a different file.
                // If that fails, try to get it from this specific file.
                Uri newUri = ContentUris.withAppendedId(ALBUMART_URI, albumid);
                try {
                    pfd = openFileAndEnforcePathPermissionsHelper(newUri, mode);
                } catch (FileNotFoundException ex) {
                    // That didn't work, now try to get it from the specific file
                    pfd = getThumb(database, db, audiopath, albumid, null);
                }
            }
            c.close();
            return pfd;
        }

        try {
            pfd = openFileAndEnforcePathPermissionsHelper(uri, mode);
        } catch (FileNotFoundException ex) {
            if (mode.contains("w")) {
                // if the file couldn't be created, we shouldn't extract album art
                throw ex;
            }

            if (URI_MATCHER.match(uri) == AUDIO_ALBUMART_ID) {
                // Tried to open an album art file which does not exist. Regenerate.
                DatabaseHelper database = getDatabaseForUri(uri);
                if (database == null) {
                    throw ex;
                }
                SQLiteDatabase db = database.getReadableDatabase();
                if (db == null) {
                    throw new IllegalStateException("Couldn't open database for " + uri);
                }
                SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
                int albumid = Integer.parseInt(uri.getPathSegments().get(3));
                qb.setTables("audio_meta");
                qb.appendWhere("album_id=" + albumid);
                Cursor c = qb.query(db,
                        new String [] {
                            MediaStore.Audio.Media.DATA },
                        null, null, null, null, MediaStore.Audio.Media.TRACK);
                if (c.moveToFirst()) {
                    String audiopath = c.getString(0);
                    pfd = getThumb(database, db, audiopath, albumid, uri);
                }
                c.close();
            }
            if (pfd == null) {
                throw ex;
            }
        }
        return pfd;
    }

    /**
     * Return the {@link MediaColumns#DATA} field for the given {@code Uri}.
     */
    private File queryForDataFile(Uri uri) throws FileNotFoundException {
        final Cursor cursor = query(
                uri, new String[] { MediaColumns.DATA }, null, null, null);
        if (cursor == null) {
            throw new FileNotFoundException("Missing cursor for " + uri);
        }

        try {
            switch (cursor.getCount()) {
                case 0:
                    throw new FileNotFoundException("No entry for " + uri);
                case 1:
                    if (cursor.moveToFirst()) {
                        return new File(cursor.getString(0));
                    } else {
                        throw new FileNotFoundException("Unable to read entry for " + uri);
                    }
                default:
                    throw new FileNotFoundException("Multiple items at " + uri);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Replacement for {@link #openFileHelper(Uri, String)} which enforces any
     * permissions applicable to the path before returning.
     */
    private ParcelFileDescriptor openFileAndEnforcePathPermissionsHelper(Uri uri, String mode)
            throws FileNotFoundException {
        final int modeBits = ParcelFileDescriptor.parseMode(mode);

        File file = queryForDataFile(uri);

        checkAccess(uri, file, modeBits);

        // Bypass emulation layer when file is opened for reading, but only
        // when opening read-only and we have an exact match.
        if (modeBits == MODE_READ_ONLY) {
            file = Environment.maybeTranslateEmulatedPathToInternal(file);
        }

        return ParcelFileDescriptor.open(file, modeBits);
    }

    private void deleteIfAllowed(Uri uri, String path) {
        try {
            File file = new File(path);
            checkAccess(uri, file, ParcelFileDescriptor.MODE_WRITE_ONLY);
            file.delete();
        } catch (Exception e) {
            Log.e(TAG, "Couldn't delete " + path);
        }
    }

    private void checkAccess(Uri uri, File file, int modeBits) throws FileNotFoundException {
        final boolean isWrite = (modeBits & MODE_WRITE_ONLY) != 0;
        final String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to resolve canonical path for " + file, e);
        }

        Context c = getContext();
        boolean readGranted =
                (c.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                == PackageManager.PERMISSION_GRANTED);

        if (path.startsWith(sExternalPath) || path.startsWith(sLegacyPath)) {
            if (!readGranted) {
                c.enforceCallingOrSelfPermission(
                        READ_EXTERNAL_STORAGE, "External path: " + path);
            }

            if (isWrite) {
                if (c.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        != PackageManager.PERMISSION_GRANTED) {
                    c.enforceCallingOrSelfPermission(
                            WRITE_EXTERNAL_STORAGE, "External path: " + path);
                }
            }

        } else if (path.startsWith(sCachePath)) {
            if (!readGranted) {
                c.enforceCallingOrSelfPermission(
                        ACCESS_CACHE_FILESYSTEM, "Cache path: " + path);
            }
        } else if (isWrite) {
            // don't write to non-cache, non-sdcard files.
            throw new FileNotFoundException("Can't access " + file);
        } else if (isSecondaryExternalPath(path)) {
            // read access is OK with the appropriate permission
            if (!readGranted) {
                c.enforceCallingOrSelfPermission(
                        READ_EXTERNAL_STORAGE, "External path: " + path);
            }
        } else {
            checkWorldReadAccess(path);
        }
    }

    private boolean isSecondaryExternalPath(String path) {
        for (int i = mExternalStoragePaths.length - 1; i >= 0; --i) {
            if (path.startsWith(mExternalStoragePaths[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the path is a world-readable file
     */
    private void checkWorldReadAccess(String path) throws FileNotFoundException {

        try {
            StructStat stat = Libcore.os.stat(path);
            int accessBits = OsConstants.S_IROTH;
            if (OsConstants.S_ISREG(stat.st_mode) &&
                ((stat.st_mode & accessBits) == accessBits)) {
                checkLeadingPathComponentsWorldExecutable(path);
                return;
            }
        } catch (ErrnoException e) {
            // couldn't stat the file, either it doesn't exist or isn't
            // accessible to us
        }

        throw new FileNotFoundException("Can't access " + path);
    }

    private void checkLeadingPathComponentsWorldExecutable(String filePath)
            throws FileNotFoundException {
        File parent = new File(filePath).getParentFile();

        int accessBits = OsConstants.S_IXOTH;

        while (parent != null) {
            if (! parent.exists()) {
                // parent dir doesn't exist, give up
                throw new FileNotFoundException("access denied");
            }
            try {
                StructStat stat = Libcore.os.stat(parent.getPath());
                if ((stat.st_mode & accessBits) != accessBits) {
                    // the parent dir doesn't have the appropriate access
                    throw new FileNotFoundException("Can't access " + filePath);
                }
            } catch (ErrnoException e1) {
                // couldn't stat() parent
                throw new FileNotFoundException("Can't access " + filePath);
            }
            parent = parent.getParentFile();
        }
    }

    private class ThumbData {
        DatabaseHelper helper;
        SQLiteDatabase db;
        String path;
        long album_id;
        Uri albumart_uri;
    }

    private void makeThumbAsync(DatabaseHelper helper, SQLiteDatabase db,
            String path, long album_id) {
        synchronized (mPendingThumbs) {
            if (mPendingThumbs.contains(path)) {
                // There's already a request to make an album art thumbnail
                // for this audio file in the queue.
                return;
            }

            mPendingThumbs.add(path);
        }

        ThumbData d = new ThumbData();
        d.helper = helper;
        d.db = db;
        d.path = path;
        d.album_id = album_id;
        d.albumart_uri = ContentUris.withAppendedId(mAlbumArtBaseUri, album_id);

        // Instead of processing thumbnail requests in the order they were
        // received we instead process them stack-based, i.e. LIFO.
        // The idea behind this is that the most recently requested thumbnails
        // are most likely the ones still in the user's view, whereas those
        // requested earlier may have already scrolled off.
        synchronized (mThumbRequestStack) {
            mThumbRequestStack.push(d);
        }

        // Trigger the handler.
        Message msg = mThumbHandler.obtainMessage(ALBUM_THUMB);
        msg.sendToTarget();
    }

    //Return true if the artPath is the dir as it in mExternalStoragePaths
    //for multi storage support
    private static boolean isRootStorageDir(String artPath) {
        for ( int i = 0; i < mExternalStoragePaths.length; i++) {
            if ((mExternalStoragePaths[i] != null) &&
                    (artPath.equalsIgnoreCase(mExternalStoragePaths[i])))
                return true;
        }
        return false;
    }

    // Extract compressed image data from the audio file itself or, if that fails,
    // look for a file "AlbumArt.jpg" in the containing directory.
    private static byte[] getCompressedAlbumArt(Context context, String path) {
        byte[] compressed = null;

        try {
            File f = new File(path);
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f,
                    ParcelFileDescriptor.MODE_READ_ONLY);

            MediaScanner scanner = new MediaScanner(context);
            compressed = scanner.extractAlbumArt(pfd.getFileDescriptor());
            pfd.close();

            // If no embedded art exists, look for a suitable image file in the
            // same directory as the media file, except if that directory is
            // is the root directory of the sd card or the download directory.
            // We look for, in order of preference:
            // 0 AlbumArt.jpg
            // 1 AlbumArt*Large.jpg
            // 2 Any other jpg image with 'albumart' anywhere in the name
            // 3 Any other jpg image
            // 4 any other png image
            if (compressed == null && path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash > 0) {

                    String artPath = path.substring(0, lastSlash);
                    String dwndir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

                    String bestmatch = null;
                    synchronized (sFolderArtMap) {
                        if (sFolderArtMap.containsKey(artPath)) {
                            bestmatch = sFolderArtMap.get(artPath);
                        } else if (!isRootStorageDir(artPath) &&
                                !artPath.equalsIgnoreCase(dwndir)) {
                            File dir = new File(artPath);
                            String [] entrynames = dir.list();
                            if (entrynames == null) {
                                return null;
                            }
                            bestmatch = null;
                            int matchlevel = 1000;
                            for (int i = entrynames.length - 1; i >=0; i--) {
                                String entry = entrynames[i].toLowerCase();
                                if (entry.equals("albumart.jpg")) {
                                    bestmatch = entrynames[i];
                                    break;
                                } else if (entry.startsWith("albumart")
                                        && entry.endsWith("large.jpg")
                                        && matchlevel > 1) {
                                    bestmatch = entrynames[i];
                                    matchlevel = 1;
                                } else if (entry.contains("albumart")
                                        && entry.endsWith(".jpg")
                                        && matchlevel > 2) {
                                    bestmatch = entrynames[i];
                                    matchlevel = 2;
                                } else if (entry.endsWith(".jpg") && matchlevel > 3) {
                                    bestmatch = entrynames[i];
                                    matchlevel = 3;
                                } else if (entry.endsWith(".png") && matchlevel > 4) {
                                    bestmatch = entrynames[i];
                                    matchlevel = 4;
                                }
                            }
                            // note that this may insert null if no album art was found
                            sFolderArtMap.put(artPath, bestmatch);
                        }
                    }

                    if (bestmatch != null) {
                        File file = new File(artPath, bestmatch);
                        if (file.exists()) {
                            FileInputStream stream = null;
                            try {
                                compressed = new byte[(int)file.length()];
                                stream = new FileInputStream(file);
                                stream.read(compressed);
                            } catch (IOException ex) {
                                compressed = null;
                            } catch (OutOfMemoryError ex) {
                                Log.w(TAG, ex);
                                compressed = null;
                            } finally {
                                if (stream != null) {
                                    stream.close();
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
        }

        return compressed;
    }

    // Return a URI to write the album art to and update the database as necessary.
    Uri getAlbumArtOutputUri(DatabaseHelper helper, SQLiteDatabase db, long album_id, Uri albumart_uri) {
        Uri out = null;
        // TODO: this could be done more efficiently with a call to db.replace(), which
        // replaces or inserts as needed, making it unnecessary to query() first.
        if (albumart_uri != null) {
            Cursor c = query(albumart_uri, new String [] { MediaStore.MediaColumns.DATA },
                    null, null, null);
            try {
                if (c != null && c.moveToFirst()) {
                    String albumart_path = c.getString(0);
                    if (ensureFileExists(albumart_path)) {
                        out = albumart_uri;
                    }
                } else {
                    albumart_uri = null;
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        if (albumart_uri == null){
            ContentValues initialValues = new ContentValues();
            initialValues.put("album_id", album_id);
            try {
                ContentValues values = ensureFile(false, initialValues, "", ALBUM_THUMB_FOLDER);
                helper.mNumInserts++;
                long rowId = db.insert("album_art", MediaStore.MediaColumns.DATA, values);
                if (rowId > 0) {
                    out = ContentUris.withAppendedId(ALBUMART_URI, rowId);
                    // ensure the parent directory exists
                    String albumart_path = values.getAsString(MediaStore.MediaColumns.DATA);
                    ensureFileExists(albumart_path);
                }
            } catch (IllegalStateException ex) {
                Log.e(TAG, "error creating album thumb file");
            }
        }
        return out;
    }

    // Write out the album art to the output URI, recompresses the given Bitmap
    // if necessary, otherwise writes the compressed data.
    private void writeAlbumArt(
            boolean need_to_recompress, Uri out, byte[] compressed, Bitmap bm) throws IOException {
        OutputStream outstream = null;
        try {
            outstream = getContext().getContentResolver().openOutputStream(out);

            if (!need_to_recompress) {
                // No need to recompress here, just write out the original
                // compressed data here.
                outstream.write(compressed);
            } else {
                if (!bm.compress(Bitmap.CompressFormat.JPEG, 85, outstream)) {
                    throw new IOException("failed to compress bitmap");
                }
            }
        } finally {
            IoUtils.closeQuietly(outstream);
        }
    }

    private ParcelFileDescriptor getThumb(DatabaseHelper helper, SQLiteDatabase db, String path,
            long album_id, Uri albumart_uri) {
        ThumbData d = new ThumbData();
        d.helper = helper;
        d.db = db;
        d.path = path;
        d.album_id = album_id;
        d.albumart_uri = albumart_uri;
        return makeThumbInternal(d);
    }

    private ParcelFileDescriptor makeThumbInternal(ThumbData d) {
        byte[] compressed = getCompressedAlbumArt(getContext(), d.path);

        if (compressed == null) {
            return null;
        }

        Bitmap bm = null;
        boolean need_to_recompress = true;

        try {
            // get the size of the bitmap
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            opts.inSampleSize = 1;
            BitmapFactory.decodeByteArray(compressed, 0, compressed.length, opts);

            // request a reasonably sized output image
            final Resources r = getContext().getResources();
            final int maximumThumbSize = r.getDimensionPixelSize(R.dimen.maximum_thumb_size);
            while (opts.outHeight > maximumThumbSize || opts.outWidth > maximumThumbSize) {
                opts.outHeight /= 2;
                opts.outWidth /= 2;
                opts.inSampleSize *= 2;
            }

            if (opts.inSampleSize == 1) {
                // The original album art was of proper size, we won't have to
                // recompress the bitmap later.
                need_to_recompress = false;
            } else {
                // get the image for real now
                opts.inJustDecodeBounds = false;
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                bm = BitmapFactory.decodeByteArray(compressed, 0, compressed.length, opts);

                if (bm != null && bm.getConfig() == null) {
                    Bitmap nbm = bm.copy(Bitmap.Config.RGB_565, false);
                    if (nbm != null && nbm != bm) {
                        bm.recycle();
                        bm = nbm;
                    }
                }
            }
        } catch (Exception e) {
        }

        if (need_to_recompress && bm == null) {
            return null;
        }

        if (d.albumart_uri == null) {
            // this one doesn't need to be saved (probably a song with an unknown album),
            // so stick it in a memory file and return that
            try {
                return ParcelFileDescriptor.fromData(compressed, "albumthumb");
            } catch (IOException e) {
            }
        } else {
            // This one needs to actually be saved on the sd card.
            // This is wrapped in a transaction because there are various things
            // that could go wrong while generating the thumbnail, and we only want
            // to update the database when all steps succeeded.
            d.db.beginTransaction();
            Uri out = null;
            ParcelFileDescriptor pfd = null;
            try {
                out = getAlbumArtOutputUri(d.helper, d.db, d.album_id, d.albumart_uri);

                if (out != null) {
                    writeAlbumArt(need_to_recompress, out, compressed, bm);
                    getContext().getContentResolver().notifyChange(MEDIA_URI, null);
                    pfd = openFileHelper(out, "r");
                    d.db.setTransactionSuccessful();
                    return pfd;
                }
            } catch (IOException ex) {
                // do nothing, just return null below
            } catch (UnsupportedOperationException ex) {
                // do nothing, just return null below
            } finally {
                d.db.endTransaction();
                if (bm != null) {
                    bm.recycle();
                }
                if (pfd == null && out != null) {
                    // Thumbnail was not written successfully, delete the entry that refers to it.
                    // Note that this only does something if getAlbumArtOutputUri() reused an
                    // existing entry from the database. If a new entry was created, it will
                    // have been rolled back as part of backing out the transaction.
                    getContext().getContentResolver().delete(out, null, null);
                }
            }
        }
        return null;
    }

    /**
     * Look up the artist or album entry for the given name, creating that entry
     * if it does not already exists.
     * @param db        The database
     * @param table     The table to store the key/name pair in.
     * @param keyField  The name of the key-column
     * @param nameField The name of the name-column
     * @param rawName   The name that the calling app was trying to insert into the database
     * @param cacheName The string that will be inserted in to the cache
     * @param path      The full path to the file being inserted in to the audio table
     * @param albumHash A hash to distinguish between different albums of the same name
     * @param artist    The name of the artist, if known
     * @param cache     The cache to add this entry to
     * @param srcuri    The Uri that prompted the call to this method, used for determining whether this is
     *                  the internal or external database
     * @return          The row ID for this artist/album, or -1 if the provided name was invalid
     */
    private long getKeyIdForName(DatabaseHelper helper, SQLiteDatabase db,
            String table, String keyField, String nameField,
            String rawName, String cacheName, String path, int albumHash,
            String artist, HashMap<String, Long> cache, Uri srcuri) {
        long rowId;

        if (rawName == null || rawName.length() == 0) {
            rawName = MediaStore.UNKNOWN_STRING;
        }
        String k = MediaStore.Audio.keyFor(rawName);

        if (k == null) {
            // shouldn't happen, since we only get null keys for null inputs
            Log.e(TAG, "null key", new Exception());
            return -1;
        }

        boolean isAlbum = table.equals("albums");
        boolean isUnknown = MediaStore.UNKNOWN_STRING.equals(rawName);

        // To distinguish same-named albums, we append a hash. The hash is based
        // on the "album artist" tag if present, otherwise on the "compilation" tag
        // if present, otherwise on the path.
        // Ideally we would also take things like CDDB ID in to account, so
        // we can group files from the same album that aren't in the same
        // folder, but this is a quick and easy start that works immediately
        // without requiring support from the mp3, mp4 and Ogg meta data
        // readers, as long as the albums are in different folders.
        if (isAlbum) {
            k = k + albumHash;
            if (isUnknown) {
                k = k + artist;
            }
        }

        String [] selargs = { k };
        helper.mNumQueries++;
        Cursor c = db.query(table, null, keyField + "=?", selargs, null, null, null);

        try {
            switch (c.getCount()) {
                case 0: {
                        // insert new entry into table
                        ContentValues otherValues = new ContentValues();
                        otherValues.put(keyField, k);
                        otherValues.put(nameField, rawName);
                        helper.mNumInserts++;
                        rowId = db.insert(table, "duration", otherValues);
                        if (path != null && isAlbum && ! isUnknown) {
                            // We just inserted a new album. Now create an album art thumbnail for it.
                            makeThumbAsync(helper, db, path, rowId);
                        }
                        if (rowId > 0) {
                            String volume = srcuri.toString().substring(16, 24); // extract internal/external
                            Uri uri = Uri.parse("content://media/" + volume + "/audio/" + table + "/" + rowId);
                            getContext().getContentResolver().notifyChange(uri, null);
                        }
                    }
                    break;
                case 1: {
                        // Use the existing entry
                        c.moveToFirst();
                        rowId = c.getLong(0);

                        // Determine whether the current rawName is better than what's
                        // currently stored in the table, and update the table if it is.
                        String currentFancyName = c.getString(2);
                        String bestName = makeBestName(rawName, currentFancyName);
                        if (!bestName.equals(currentFancyName)) {
                            // update the table with the new name
                            ContentValues newValues = new ContentValues();
                            newValues.put(nameField, bestName);
                            helper.mNumUpdates++;
                            db.update(table, newValues, "rowid="+Integer.toString((int)rowId), null);
                            String volume = srcuri.toString().substring(16, 24); // extract internal/external
                            Uri uri = Uri.parse("content://media/" + volume + "/audio/" + table + "/" + rowId);
                            getContext().getContentResolver().notifyChange(uri, null);
                        }
                    }
                    break;
                default:
                    // corrupt database
                    Log.e(TAG, "Multiple entries in table " + table + " for key " + k);
                    rowId = -1;
                    break;
            }
        } finally {
            if (c != null) c.close();
        }

        if (cache != null && ! isUnknown) {
            cache.put(cacheName, rowId);
        }
        return rowId;
    }

    /**
     * Returns the best string to use for display, given two names.
     * Note that this function does not necessarily return either one
     * of the provided names; it may decide to return a better alternative
     * (for example, specifying the inputs "Police" and "Police, The" will
     * return "The Police")
     *
     * The basic assumptions are:
     * - longer is better ("The police" is better than "Police")
     * - prefix is better ("The Police" is better than "Police, The")
     * - accents are better ("Mot&ouml;rhead" is better than "Motorhead")
     *
     * @param one The first of the two names to consider
     * @param two The last of the two names to consider
     * @return The actual name to use
     */
    String makeBestName(String one, String two) {
        String name;

        // Longer names are usually better.
        if (one.length() > two.length()) {
            name = one;
        } else {
            // Names with accents are usually better, and conveniently sort later
            if (one.toLowerCase().compareTo(two.toLowerCase()) > 0) {
                name = one;
            } else {
                name = two;
            }
        }

        // Prefixes are better than postfixes.
        if (name.endsWith(", the") || name.endsWith(",the") ||
            name.endsWith(", an") || name.endsWith(",an") ||
            name.endsWith(", a") || name.endsWith(",a")) {
            String fix = name.substring(1 + name.lastIndexOf(','));
            name = fix.trim() + " " + name.substring(0, name.lastIndexOf(','));
        }

        // TODO: word-capitalize the resulting name
        return name;
    }


    /**
     * Looks up the database based on the given URI.
     *
     * @param uri The requested URI
     * @returns the database for the given URI
     */
    private DatabaseHelper getDatabaseForUri(Uri uri) {
        synchronized (mDatabases) {
            if (uri.getPathSegments().size() >= 1) {
                return mDatabases.get(uri.getPathSegments().get(0));
            }
        }
        return null;
    }

    static boolean isMediaDatabaseName(String name) {
        if (INTERNAL_DATABASE_NAME.equals(name)) {
            return true;
        }
        if (EXTERNAL_DATABASE_NAME.equals(name)) {
            return true;
        }
        if (name.startsWith("external-") && name.endsWith(".db")) {
            return true;
        }
        return false;
    }

    static boolean isInternalMediaDatabaseName(String name) {
        if (INTERNAL_DATABASE_NAME.equals(name)) {
            return true;
        }
        return false;
    }

    /**
     * Attach the database for a volume (internal or external).
     * Does nothing if the volume is already attached, otherwise
     * checks the volume ID and sets up the corresponding database.
     *
     * @param volume to attach, either {@link #INTERNAL_VOLUME} or {@link #EXTERNAL_VOLUME}.
     * @return the content URI of the attached volume.
     */
    private Uri attachVolume(String volume) {
        if (Binder.getCallingPid() != Process.myPid()) {
            throw new SecurityException(
                    "Opening and closing databases not allowed.");
        }

        synchronized (mDatabases) {
            if (mDatabases.get(volume) != null) {  // Already attached
                return Uri.parse("content://media/" + volume);
            }

            Context context = getContext();
            DatabaseHelper helper;
            if (INTERNAL_VOLUME.equals(volume)) {
                helper = new DatabaseHelper(context, INTERNAL_DATABASE_NAME, true,
                        false, mObjectRemovedCallback);
            } else if (EXTERNAL_VOLUME.equals(volume)) {
                if (Environment.isExternalStorageRemovable()) {
                    final StorageVolume actualVolume = mStorageManager.getPrimaryVolume();
                    final int volumeId = actualVolume.getFatVolumeId();

                    // Must check for failure!
                    // If the volume is not (yet) mounted, this will create a new
                    // external-ffffffff.db database instead of the one we expect.  Then, if
                    // android.process.media is later killed and respawned, the real external
                    // database will be attached, containing stale records, or worse, be empty.
                    if (volumeId == -1) {
                        String state = Environment.getExternalStorageState();
                        if (Environment.MEDIA_MOUNTED.equals(state) ||
                                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                            // This may happen if external storage was _just_ mounted.  It may also
                            // happen if the volume ID is _actually_ 0xffffffff, in which case it
                            // must be changed since FileUtils::getFatVolumeId doesn't allow for
                            // that.  It may also indicate that FileUtils::getFatVolumeId is broken
                            // (missing ioctl), which is also impossible to disambiguate.
                            Log.e(TAG, "Can't obtain external volume ID even though it's mounted.");
                        } else {
                            Log.i(TAG, "External volume is not (yet) mounted, cannot attach.");
                        }

                        throw new IllegalArgumentException("Can't obtain external volume ID for " +
                                volume + " volume.");
                    }

                    // generate database name based on volume ID
                    String dbName = "external-" + Integer.toHexString(volumeId) + ".db";
                    helper = new DatabaseHelper(context, dbName, false,
                            false, mObjectRemovedCallback);
                    mVolumeId = volumeId;
                } else {
                    // external database name should be EXTERNAL_DATABASE_NAME
                    // however earlier releases used the external-XXXXXXXX.db naming
                    // for devices without removable storage, and in that case we need to convert
                    // to this new convention
                    File dbFile = context.getDatabasePath(EXTERNAL_DATABASE_NAME);
                    if (!dbFile.exists()) {
                        // find the most recent external database and rename it to
                        // EXTERNAL_DATABASE_NAME, and delete any other older
                        // external database files
                        File recentDbFile = null;
                        for (String database : context.databaseList()) {
                            if (database.startsWith("external-") && database.endsWith(".db")) {
                                File file = context.getDatabasePath(database);
                                if (recentDbFile == null) {
                                    recentDbFile = file;
                                } else if (file.lastModified() > recentDbFile.lastModified()) {
                                    context.deleteDatabase(recentDbFile.getName());
                                    recentDbFile = file;
                                } else {
                                    context.deleteDatabase(file.getName());
                                }
                            }
                        }
                        if (recentDbFile != null) {
                            if (recentDbFile.renameTo(dbFile)) {
                                Log.d(TAG, "renamed database " + recentDbFile.getName() +
                                        " to " + EXTERNAL_DATABASE_NAME);
                            } else {
                                Log.e(TAG, "Failed to rename database " + recentDbFile.getName() +
                                        " to " + EXTERNAL_DATABASE_NAME);
                                // This shouldn't happen, but if it does, continue using
                                // the file under its old name
                                dbFile = recentDbFile;
                            }
                        }
                        // else DatabaseHelper will create one named EXTERNAL_DATABASE_NAME
                    }
                    helper = new DatabaseHelper(context, dbFile.getName(), false,
                            false, mObjectRemovedCallback);
                }
            } else {
                throw new IllegalArgumentException("There is no volume named " + volume);
            }

            mDatabases.put(volume, helper);

            if (!helper.mInternal) {
                // create default directories (only happens on first boot)
                createDefaultFolders(helper, helper.getWritableDatabase());

                // clean up stray album art files: delete every file not in the database
                File[] files = new File(mExternalStoragePaths[0], ALBUM_THUMB_FOLDER).listFiles();
                HashSet<String> fileSet = new HashSet();
                for (int i = 0; files != null && i < files.length; i++) {
                    fileSet.add(files[i].getPath());
                }

                Cursor cursor = query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Audio.Albums.ALBUM_ART }, null, null, null);
                try {
                    while (cursor != null && cursor.moveToNext()) {
                        fileSet.remove(cursor.getString(0));
                    }
                } finally {
                    if (cursor != null) cursor.close();
                }

                Iterator<String> iterator = fileSet.iterator();
                while (iterator.hasNext()) {
                    String filename = iterator.next();
                    if (LOCAL_LOGV) Log.v(TAG, "deleting obsolete album art " + filename);
                    new File(filename).delete();
                }
            }
        }

        if (LOCAL_LOGV) Log.v(TAG, "Attached volume: " + volume);
        return Uri.parse("content://media/" + volume);
    }

    /**
     * Detach the database for a volume (must be external).
     * Does nothing if the volume is already detached, otherwise
     * closes the database and sends a notification to listeners.
     *
     * @param uri The content URI of the volume, as returned by {@link #attachVolume}
     */
    private void detachVolume(Uri uri) {
        if (Binder.getCallingPid() != Process.myPid()) {
            throw new SecurityException(
                    "Opening and closing databases not allowed.");
        }

        String volume = uri.getPathSegments().get(0);
        if (INTERNAL_VOLUME.equals(volume)) {
            throw new UnsupportedOperationException(
                    "Deleting the internal volume is not allowed");
        } else if (!EXTERNAL_VOLUME.equals(volume)) {
            throw new IllegalArgumentException(
                    "There is no volume named " + volume);
        }

        synchronized (mDatabases) {
            DatabaseHelper database = mDatabases.get(volume);
            if (database == null) return;

            try {
                // touch the database file to show it is most recently used
                File file = new File(database.getReadableDatabase().getPath());
                file.setLastModified(System.currentTimeMillis());
            } catch (Exception e) {
                Log.e(TAG, "Can't touch database file", e);
            }

            mDatabases.remove(volume);
            database.close();
        }

        getContext().getContentResolver().notifyChange(uri, null);
        if (LOCAL_LOGV) Log.v(TAG, "Detached volume: " + volume);
    }

    private static String TAG = "MediaProvider";
    private static final boolean LOCAL_LOGV = false;

    private static final String INTERNAL_DATABASE_NAME = "internal.db";
    private static final String EXTERNAL_DATABASE_NAME = "external.db";

    // maximum number of cached external databases to keep
    private static final int MAX_EXTERNAL_DATABASES = 3;

    // Delete databases that have not been used in two months
    // 60 days in milliseconds (1000 * 60 * 60 * 24 * 60)
    private static final long OBSOLETE_DATABASE_DB = 5184000000L;

    private HashMap<String, DatabaseHelper> mDatabases;

    private Handler mThumbHandler;

    // name of the volume currently being scanned by the media scanner (or null)
    private String mMediaScannerVolume;

    // current FAT volume ID
    private int mVolumeId = -1;

    static final String INTERNAL_VOLUME = "internal";
    static final String EXTERNAL_VOLUME = "external";
    static final String ALBUM_THUMB_FOLDER = "Android/data/com.android.providers.media/albumthumbs";

    // path for writing contents of in memory temp database
    private String mTempDatabasePath;

    // WARNING: the values of IMAGES_MEDIA, AUDIO_MEDIA, and VIDEO_MEDIA and AUDIO_PLAYLISTS
    // are stored in the "files" table, so do not renumber them unless you also add
    // a corresponding database upgrade step for it.
    private static final int IMAGES_MEDIA = 1;
    private static final int IMAGES_MEDIA_ID = 2;
    private static final int IMAGES_THUMBNAILS = 3;
    private static final int IMAGES_THUMBNAILS_ID = 4;

    private static final int AUDIO_MEDIA = 100;
    private static final int AUDIO_MEDIA_ID = 101;
    private static final int AUDIO_MEDIA_ID_GENRES = 102;
    private static final int AUDIO_MEDIA_ID_GENRES_ID = 103;
    private static final int AUDIO_MEDIA_ID_PLAYLISTS = 104;
    private static final int AUDIO_MEDIA_ID_PLAYLISTS_ID = 105;
    private static final int AUDIO_GENRES = 106;
    private static final int AUDIO_GENRES_ID = 107;
    private static final int AUDIO_GENRES_ID_MEMBERS = 108;
    private static final int AUDIO_GENRES_ALL_MEMBERS = 109;
    private static final int AUDIO_PLAYLISTS = 110;
    private static final int AUDIO_PLAYLISTS_ID = 111;
    private static final int AUDIO_PLAYLISTS_ID_MEMBERS = 112;
    private static final int AUDIO_PLAYLISTS_ID_MEMBERS_ID = 113;
    private static final int AUDIO_ARTISTS = 114;
    private static final int AUDIO_ARTISTS_ID = 115;
    private static final int AUDIO_ALBUMS = 116;
    private static final int AUDIO_ALBUMS_ID = 117;
    private static final int AUDIO_ARTISTS_ID_ALBUMS = 118;
    private static final int AUDIO_ALBUMART = 119;
    private static final int AUDIO_ALBUMART_ID = 120;
    private static final int AUDIO_ALBUMART_FILE_ID = 121;

    private static final int VIDEO_MEDIA = 200;
    private static final int VIDEO_MEDIA_ID = 201;
    private static final int VIDEO_THUMBNAILS = 202;
    private static final int VIDEO_THUMBNAILS_ID = 203;

    private static final int VOLUMES = 300;
    private static final int VOLUMES_ID = 301;

    private static final int AUDIO_SEARCH_LEGACY = 400;
    private static final int AUDIO_SEARCH_BASIC = 401;
    private static final int AUDIO_SEARCH_FANCY = 402;

    private static final int MEDIA_SCANNER = 500;

    private static final int FS_ID = 600;
    private static final int VERSION = 601;

    private static final int FILES = 700;
    private static final int FILES_ID = 701;

    // Used only by the MTP implementation
    private static final int MTP_OBJECTS = 702;
    private static final int MTP_OBJECTS_ID = 703;
    private static final int MTP_OBJECT_REFERENCES = 704;
    // UsbReceiver calls insert() and delete() with this URI to tell us
    // when MTP is connected and disconnected
    private static final int MTP_CONNECTED = 705;

    private static final UriMatcher URI_MATCHER =
            new UriMatcher(UriMatcher.NO_MATCH);

    private static final String[] ID_PROJECTION = new String[] {
        MediaStore.MediaColumns._ID
    };

    private static final String[] PATH_PROJECTION = new String[] {
        MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
    };

    private static final String[] MIME_TYPE_PROJECTION = new String[] {
            MediaStore.MediaColumns._ID, // 0
            MediaStore.MediaColumns.MIME_TYPE, // 1
    };

    private static final String[] READY_FLAG_PROJECTION = new String[] {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            Images.Media.MINI_THUMB_MAGIC
    };

    private static final String OBJECT_REFERENCES_QUERY =
        "SELECT " + Audio.Playlists.Members.AUDIO_ID + " FROM audio_playlists_map"
        + " WHERE " + Audio.Playlists.Members.PLAYLIST_ID + "=?"
        + " ORDER BY " + Audio.Playlists.Members.PLAY_ORDER;

    static
    {
        URI_MATCHER.addURI("media", "*/images/media", IMAGES_MEDIA);
        URI_MATCHER.addURI("media", "*/images/media/#", IMAGES_MEDIA_ID);
        URI_MATCHER.addURI("media", "*/images/thumbnails", IMAGES_THUMBNAILS);
        URI_MATCHER.addURI("media", "*/images/thumbnails/#", IMAGES_THUMBNAILS_ID);

        URI_MATCHER.addURI("media", "*/audio/media", AUDIO_MEDIA);
        URI_MATCHER.addURI("media", "*/audio/media/#", AUDIO_MEDIA_ID);
        URI_MATCHER.addURI("media", "*/audio/media/#/genres", AUDIO_MEDIA_ID_GENRES);
        URI_MATCHER.addURI("media", "*/audio/media/#/genres/#", AUDIO_MEDIA_ID_GENRES_ID);
        URI_MATCHER.addURI("media", "*/audio/media/#/playlists", AUDIO_MEDIA_ID_PLAYLISTS);
        URI_MATCHER.addURI("media", "*/audio/media/#/playlists/#", AUDIO_MEDIA_ID_PLAYLISTS_ID);
        URI_MATCHER.addURI("media", "*/audio/genres", AUDIO_GENRES);
        URI_MATCHER.addURI("media", "*/audio/genres/#", AUDIO_GENRES_ID);
        URI_MATCHER.addURI("media", "*/audio/genres/#/members", AUDIO_GENRES_ID_MEMBERS);
        URI_MATCHER.addURI("media", "*/audio/genres/all/members", AUDIO_GENRES_ALL_MEMBERS);
        URI_MATCHER.addURI("media", "*/audio/playlists", AUDIO_PLAYLISTS);
        URI_MATCHER.addURI("media", "*/audio/playlists/#", AUDIO_PLAYLISTS_ID);
        URI_MATCHER.addURI("media", "*/audio/playlists/#/members", AUDIO_PLAYLISTS_ID_MEMBERS);
        URI_MATCHER.addURI("media", "*/audio/playlists/#/members/#", AUDIO_PLAYLISTS_ID_MEMBERS_ID);
        URI_MATCHER.addURI("media", "*/audio/artists", AUDIO_ARTISTS);
        URI_MATCHER.addURI("media", "*/audio/artists/#", AUDIO_ARTISTS_ID);
        URI_MATCHER.addURI("media", "*/audio/artists/#/albums", AUDIO_ARTISTS_ID_ALBUMS);
        URI_MATCHER.addURI("media", "*/audio/albums", AUDIO_ALBUMS);
        URI_MATCHER.addURI("media", "*/audio/albums/#", AUDIO_ALBUMS_ID);
        URI_MATCHER.addURI("media", "*/audio/albumart", AUDIO_ALBUMART);
        URI_MATCHER.addURI("media", "*/audio/albumart/#", AUDIO_ALBUMART_ID);
        URI_MATCHER.addURI("media", "*/audio/media/#/albumart", AUDIO_ALBUMART_FILE_ID);

        URI_MATCHER.addURI("media", "*/video/media", VIDEO_MEDIA);
        URI_MATCHER.addURI("media", "*/video/media/#", VIDEO_MEDIA_ID);
        URI_MATCHER.addURI("media", "*/video/thumbnails", VIDEO_THUMBNAILS);
        URI_MATCHER.addURI("media", "*/video/thumbnails/#", VIDEO_THUMBNAILS_ID);

        URI_MATCHER.addURI("media", "*/media_scanner", MEDIA_SCANNER);

        URI_MATCHER.addURI("media", "*/fs_id", FS_ID);
        URI_MATCHER.addURI("media", "*/version", VERSION);

        URI_MATCHER.addURI("media", "*/mtp_connected", MTP_CONNECTED);

        URI_MATCHER.addURI("media", "*", VOLUMES_ID);
        URI_MATCHER.addURI("media", null, VOLUMES);

        // Used by MTP implementation
        URI_MATCHER.addURI("media", "*/file", FILES);
        URI_MATCHER.addURI("media", "*/file/#", FILES_ID);
        URI_MATCHER.addURI("media", "*/object", MTP_OBJECTS);
        URI_MATCHER.addURI("media", "*/object/#", MTP_OBJECTS_ID);
        URI_MATCHER.addURI("media", "*/object/#/references", MTP_OBJECT_REFERENCES);

        /**
         * @deprecated use the 'basic' or 'fancy' search Uris instead
         */
        URI_MATCHER.addURI("media", "*/audio/" + SearchManager.SUGGEST_URI_PATH_QUERY,
                AUDIO_SEARCH_LEGACY);
        URI_MATCHER.addURI("media", "*/audio/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                AUDIO_SEARCH_LEGACY);

        // used for search suggestions
        URI_MATCHER.addURI("media", "*/audio/search/" + SearchManager.SUGGEST_URI_PATH_QUERY,
                AUDIO_SEARCH_BASIC);
        URI_MATCHER.addURI("media", "*/audio/search/" + SearchManager.SUGGEST_URI_PATH_QUERY +
                "/*", AUDIO_SEARCH_BASIC);

        // used by the music app's search activity
        URI_MATCHER.addURI("media", "*/audio/search/fancy", AUDIO_SEARCH_FANCY);
        URI_MATCHER.addURI("media", "*/audio/search/fancy/*", AUDIO_SEARCH_FANCY);
    }

    private static String getVolumeName(Uri uri) {
        final List<String> segments = uri.getPathSegments();
        if (segments != null && segments.size() > 0) {
            return segments.get(0);
        } else {
            return null;
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        Collection<DatabaseHelper> foo = mDatabases.values();
        for (DatabaseHelper dbh: foo) {
            writer.println(dump(dbh, true));
        }
        writer.flush();
    }

    private String dump(DatabaseHelper dbh, boolean dumpDbLog) {
        StringBuilder s = new StringBuilder();
        s.append(dbh.mName);
        s.append(": ");
        SQLiteDatabase db = dbh.getReadableDatabase();
        if (db == null) {
            s.append("null");
        } else {
            s.append("version " + db.getVersion() + ", ");
            Cursor c = db.query("files", new String[] {"count(*)"}, null, null, null, null, null);
            try {
                if (c != null && c.moveToFirst()) {
                    int num = c.getInt(0);
                    s.append(num + " rows, ");
                } else {
                    s.append("couldn't get row count, ");
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            s.append(dbh.mNumInserts + " inserts, ");
            s.append(dbh.mNumUpdates + " updates, ");
            s.append(dbh.mNumDeletes + " deletes, ");
            s.append(dbh.mNumQueries + " queries, ");
            if (dbh.mScanStartTime != 0) {
                s.append("scan started " + DateUtils.formatDateTime(getContext(),
                        dbh.mScanStartTime / 1000,
                        DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_ABBREV_ALL));
                long now = dbh.mScanStopTime;
                if (now < dbh.mScanStartTime) {
                    now = SystemClock.currentTimeMicro();
                }
                s.append(" (" + DateUtils.formatElapsedTime(
                        (now - dbh.mScanStartTime) / 1000000) + ")");
                if (dbh.mScanStopTime < dbh.mScanStartTime) {
                    if (mMediaScannerVolume != null &&
                            dbh.mName.startsWith(mMediaScannerVolume)) {
                        s.append(" (ongoing)");
                    } else {
                        s.append(" (scanning " + mMediaScannerVolume + ")");
                    }
                }
            }
            if (dumpDbLog) {
                c = db.query("log", new String[] {"time", "message"},
                        null, null, null, null, "rowid");
                try {
                    if (c != null) {
                        while (c.moveToNext()) {
                            String when = c.getString(0);
                            String msg = c.getString(1);
                            s.append("\n" + when + " : " + msg);
                        }
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
        return s.toString();
    }
}
