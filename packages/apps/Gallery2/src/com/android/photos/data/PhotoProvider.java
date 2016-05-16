/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.photos.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.BaseColumns;

import com.android.gallery3d.common.ApiHelper;

import java.util.List;

/**
 * A provider that gives access to photo and video information for media stored
 * on the server. Only media that is or will be put on the server will be
 * accessed by this provider. Use Photos.CONTENT_URI to query all photos and
 * videos. Use Albums.CONTENT_URI to query all albums. Use Metadata.CONTENT_URI
 * to query metadata about a photo or video, based on the ID of the media. Use
 * ImageCache.THUMBNAIL_CONTENT_URI, ImageCache.PREVIEW_CONTENT_URI, or
 * ImageCache.ORIGINAL_CONTENT_URI to query the path of the thumbnail, preview,
 * or original-sized image respectfully. <br/>
 * To add or update metadata, use the update function rather than insert. All
 * values for the metadata must be in the ContentValues, even if they are also
 * in the selection. The selection and selectionArgs are not used when updating
 * metadata. If the metadata values are null, the row will be deleted.
 */
public class PhotoProvider extends SQLiteContentProvider {
    @SuppressWarnings("unused")
    private static final String TAG = PhotoProvider.class.getSimpleName();

    protected static final String DB_NAME = "photo.db";
    public static final String AUTHORITY = PhotoProviderAuthority.AUTHORITY;
    static final Uri BASE_CONTENT_URI = new Uri.Builder().scheme("content").authority(AUTHORITY)
            .build();

    // Used to allow mocking out the change notification because
    // MockContextResolver disallows system-wide notification.
    public static interface ChangeNotification {
        void notifyChange(Uri uri, boolean syncToNetwork);
    }

    /**
     * Contains columns that can be accessed via Accounts.CONTENT_URI
     */
    public static interface Accounts extends BaseColumns {
        /**
         * Internal database table used for account information
         */
        public static final String TABLE = "accounts";
        /**
         * Content URI for account information
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, TABLE);
        /**
         * User name for this account.
         */
        public static final String ACCOUNT_NAME = "name";
    }

    /**
     * Contains columns that can be accessed via Photos.CONTENT_URI.
     */
    public static interface Photos extends BaseColumns {
        /**
         * The image_type query parameter required for requesting a specific
         * size of image.
         */
        public static final String MEDIA_SIZE_QUERY_PARAMETER = "media_size";

        /** Internal database table used for basic photo information. */
        public static final String TABLE = "photos";
        /** Content URI for basic photo and video information. */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, TABLE);

        /** Long foreign key to Accounts._ID */
        public static final String ACCOUNT_ID = "account_id";
        /** Column name for the width of the original image. Integer value. */
        public static final String WIDTH = "width";
        /** Column name for the height of the original image. Integer value. */
        public static final String HEIGHT = "height";
        /**
         * Column name for the date that the original image was taken. Long
         * value indicating the milliseconds since epoch in the GMT time zone.
         */
        public static final String DATE_TAKEN = "date_taken";
        /**
         * Column name indicating the long value of the album id that this image
         * resides in. Will be NULL if it it has not been uploaded to the
         * server.
         */
        public static final String ALBUM_ID = "album_id";
        /** The column name for the mime-type String. */
        public static final String MIME_TYPE = "mime_type";
        /** The title of the photo. String value. */
        public static final String TITLE = "title";
        /** The date the photo entry was last updated. Long value. */
        public static final String DATE_MODIFIED = "date_modified";
        /**
         * The rotation of the photo in degrees, if rotation has not already
         * been applied. Integer value.
         */
        public static final String ROTATION = "rotation";
    }

    /**
     * Contains columns and Uri for accessing album information.
     */
    public static interface Albums extends BaseColumns {
        /** Internal database table used album information. */
        public static final String TABLE = "albums";
        /** Content URI for album information. */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, TABLE);

        /** Long foreign key to Accounts._ID */
        public static final String ACCOUNT_ID = "account_id";
        /** Parent directory or null if this is in the root. */
        public static final String PARENT_ID = "parent_id";
        /** The type of album. Non-null, if album is auto-generated. String value. */
        public static final String ALBUM_TYPE = "album_type";
        /**
         * Column name for the visibility level of the album. Can be any of the
         * VISIBILITY_* values.
         */
        public static final String VISIBILITY = "visibility";
        /** The user-specified location associated with the album. String value. */
        public static final String LOCATION_STRING = "location_string";
        /** The title of the album. String value. */
        public static final String TITLE = "title";
        /** A short summary of the contents of the album. String value. */
        public static final String SUMMARY = "summary";
        /** The date the album was created. Long value */
        public static final String DATE_PUBLISHED = "date_published";
        /** The date the album entry was last updated. Long value. */
        public static final String DATE_MODIFIED = "date_modified";

        // Privacy values for Albums.VISIBILITY
        public static final int VISIBILITY_PRIVATE = 1;
        public static final int VISIBILITY_SHARED = 2;
        public static final int VISIBILITY_PUBLIC = 3;
    }

    /**
     * Contains columns and Uri for accessing photo and video metadata
     */
    public static interface Metadata extends BaseColumns {
        /** Internal database table used metadata information. */
        public static final String TABLE = "metadata";
        /** Content URI for photo and video metadata. */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, TABLE);
        /** Foreign key to photo_id. Long value. */
        public static final String PHOTO_ID = "photo_id";
        /** Metadata key. String value */
        public static final String KEY = "key";
        /**
         * Metadata value. Type is based on key.
         */
        public static final String VALUE = "value";

        /** A short summary of the photo. String value. */
        public static final String KEY_SUMMARY = "summary";
        /** The date the photo was added. Long value. */
        public static final String KEY_PUBLISHED = "date_published";
        /** The date the photo was last updated. Long value. */
        public static final String KEY_DATE_UPDATED = "date_updated";
        /** The size of the photo is bytes. Integer value. */
        public static final String KEY_SIZE_IN_BTYES = "size";
        /** The latitude associated with the photo. Double value. */
        public static final String KEY_LATITUDE = "latitude";
        /** The longitude associated with the photo. Double value. */
        public static final String KEY_LONGITUDE = "longitude";

        /** The make of the camera used. String value. */
        public static final String KEY_EXIF_MAKE = ExifInterface.TAG_MAKE;
        /** The model of the camera used. String value. */
        public static final String KEY_EXIF_MODEL = ExifInterface.TAG_MODEL;;
        /** The exposure time used. Float value. */
        public static final String KEY_EXIF_EXPOSURE = ExifInterface.TAG_EXPOSURE_TIME;
        /** Whether the flash was used. Boolean value. */
        public static final String KEY_EXIF_FLASH = ExifInterface.TAG_FLASH;
        /** The focal length used. Float value. */
        public static final String KEY_EXIF_FOCAL_LENGTH = ExifInterface.TAG_FOCAL_LENGTH;
        /** The fstop value used. Float value. */
        public static final String KEY_EXIF_FSTOP = ExifInterface.TAG_APERTURE;
        /** The ISO equivalent value used. Integer value. */
        public static final String KEY_EXIF_ISO = ExifInterface.TAG_ISO;
    }

    // SQL used within this class.
    protected static final String WHERE_ID = BaseColumns._ID + " = ?";
    protected static final String WHERE_METADATA_ID = Metadata.PHOTO_ID + " = ? AND "
            + Metadata.KEY + " = ?";

    protected static final String SELECT_ALBUM_ID = "SELECT " + Albums._ID + " FROM "
            + Albums.TABLE;
    protected static final String SELECT_PHOTO_ID = "SELECT " + Photos._ID + " FROM "
            + Photos.TABLE;
    protected static final String SELECT_PHOTO_COUNT = "SELECT COUNT(*) FROM " + Photos.TABLE;
    protected static final String DELETE_PHOTOS = "DELETE FROM " + Photos.TABLE;
    protected static final String DELETE_METADATA = "DELETE FROM " + Metadata.TABLE;
    protected static final String SELECT_METADATA_COUNT = "SELECT COUNT(*) FROM " + Metadata.TABLE;
    protected static final String WHERE = " WHERE ";
    protected static final String IN = " IN ";
    protected static final String NESTED_SELECT_START = "(";
    protected static final String NESTED_SELECT_END = ")";
    protected static final String[] PROJECTION_COUNT = {
        "COUNT(*)"
    };

    /**
     * For selecting the mime-type for an image.
     */
    private static final String[] PROJECTION_MIME_TYPE = {
        Photos.MIME_TYPE,
    };

    protected static final String[] BASE_COLUMNS_ID = {
        BaseColumns._ID,
    };

    protected ChangeNotification mNotifier = null;
    protected static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    protected static final int MATCH_PHOTO = 1;
    protected static final int MATCH_PHOTO_ID = 2;
    protected static final int MATCH_ALBUM = 3;
    protected static final int MATCH_ALBUM_ID = 4;
    protected static final int MATCH_METADATA = 5;
    protected static final int MATCH_METADATA_ID = 6;
    protected static final int MATCH_ACCOUNT = 7;
    protected static final int MATCH_ACCOUNT_ID = 8;

    static {
        sUriMatcher.addURI(AUTHORITY, Photos.TABLE, MATCH_PHOTO);
        // match against Photos._ID
        sUriMatcher.addURI(AUTHORITY, Photos.TABLE + "/#", MATCH_PHOTO_ID);
        sUriMatcher.addURI(AUTHORITY, Albums.TABLE, MATCH_ALBUM);
        // match against Albums._ID
        sUriMatcher.addURI(AUTHORITY, Albums.TABLE + "/#", MATCH_ALBUM_ID);
        sUriMatcher.addURI(AUTHORITY, Metadata.TABLE, MATCH_METADATA);
        // match against metadata/<Metadata._ID>
        sUriMatcher.addURI(AUTHORITY, Metadata.TABLE + "/#", MATCH_METADATA_ID);
        sUriMatcher.addURI(AUTHORITY, Accounts.TABLE, MATCH_ACCOUNT);
        // match against Accounts._ID
        sUriMatcher.addURI(AUTHORITY, Accounts.TABLE + "/#", MATCH_ACCOUNT_ID);
    }

    @Override
    public int deleteInTransaction(Uri uri, String selection, String[] selectionArgs,
            boolean callerIsSyncAdapter) {
        int match = matchUri(uri);
        selection = addIdToSelection(match, selection);
        selectionArgs = addIdToSelectionArgs(match, uri, selectionArgs);
        return deleteCascade(uri, match, selection, selectionArgs);
    }

    @Override
    public String getType(Uri uri) {
        Cursor cursor = query(uri, PROJECTION_MIME_TYPE, null, null, null);
        String mimeType = null;
        if (cursor.moveToNext()) {
            mimeType = cursor.getString(0);
        }
        cursor.close();
        return mimeType;
    }

    @Override
    public Uri insertInTransaction(Uri uri, ContentValues values, boolean callerIsSyncAdapter) {
        int match = matchUri(uri);
        validateMatchTable(match);
        String table = getTableFromMatch(match, uri);
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        Uri insertedUri = null;
        long id = db.insert(table, null, values);
        if (id != -1) {
            // uri already matches the table.
            insertedUri = ContentUris.withAppendedId(uri, id);
            postNotifyUri(insertedUri);
        }
        return insertedUri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return query(uri, projection, selection, selectionArgs, sortOrder, null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder, CancellationSignal cancellationSignal) {
        projection = replaceCount(projection);
        int match = matchUri(uri);
        selection = addIdToSelection(match, selection);
        selectionArgs = addIdToSelectionArgs(match, uri, selectionArgs);
        String table = getTableFromMatch(match, uri);
        Cursor c = query(table, projection, selection, selectionArgs, sortOrder, cancellationSignal);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public int updateInTransaction(Uri uri, ContentValues values, String selection,
            String[] selectionArgs, boolean callerIsSyncAdapter) {
        int match = matchUri(uri);
        int rowsUpdated = 0;
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        if (match == MATCH_METADATA) {
            rowsUpdated = modifyMetadata(db, values);
        } else {
            selection = addIdToSelection(match, selection);
            selectionArgs = addIdToSelectionArgs(match, uri, selectionArgs);
            String table = getTableFromMatch(match, uri);
            rowsUpdated = db.update(table, values, selection, selectionArgs);
        }
        postNotifyUri(uri);
        return rowsUpdated;
    }

    public void setMockNotification(ChangeNotification notification) {
        mNotifier = notification;
    }

    protected static String addIdToSelection(int match, String selection) {
        String where;
        switch (match) {
            case MATCH_PHOTO_ID:
            case MATCH_ALBUM_ID:
            case MATCH_METADATA_ID:
                where = WHERE_ID;
                break;
            default:
                return selection;
        }
        return DatabaseUtils.concatenateWhere(selection, where);
    }

    protected static String[] addIdToSelectionArgs(int match, Uri uri, String[] selectionArgs) {
        String[] whereArgs;
        switch (match) {
            case MATCH_PHOTO_ID:
            case MATCH_ALBUM_ID:
            case MATCH_METADATA_ID:
                whereArgs = new String[] {
                    uri.getPathSegments().get(1),
                };
                break;
            default:
                return selectionArgs;
        }
        return DatabaseUtils.appendSelectionArgs(selectionArgs, whereArgs);
    }

    protected static String[] addMetadataKeysToSelectionArgs(String[] selectionArgs, Uri uri) {
        List<String> segments = uri.getPathSegments();
        String[] additionalArgs = {
                segments.get(1),
                segments.get(2),
        };

        return DatabaseUtils.appendSelectionArgs(selectionArgs, additionalArgs);
    }

    protected static String getTableFromMatch(int match, Uri uri) {
        String table;
        switch (match) {
            case MATCH_PHOTO:
            case MATCH_PHOTO_ID:
                table = Photos.TABLE;
                break;
            case MATCH_ALBUM:
            case MATCH_ALBUM_ID:
                table = Albums.TABLE;
                break;
            case MATCH_METADATA:
            case MATCH_METADATA_ID:
                table = Metadata.TABLE;
                break;
            case MATCH_ACCOUNT:
            case MATCH_ACCOUNT_ID:
                table = Accounts.TABLE;
                break;
            default:
                throw unknownUri(uri);
        }
        return table;
    }

    @Override
    public SQLiteOpenHelper getDatabaseHelper(Context context) {
        return new PhotoDatabase(context, DB_NAME);
    }

    private int modifyMetadata(SQLiteDatabase db, ContentValues values) {
        int rowCount;
        if (values.get(Metadata.VALUE) == null) {
            String[] selectionArgs = {
                    values.getAsString(Metadata.PHOTO_ID), values.getAsString(Metadata.KEY),
            };
            rowCount = db.delete(Metadata.TABLE, WHERE_METADATA_ID, selectionArgs);
        } else {
            long rowId = db.replace(Metadata.TABLE, null, values);
            rowCount = (rowId == -1) ? 0 : 1;
        }
        return rowCount;
    }

    private int matchUri(Uri uri) {
        int match = sUriMatcher.match(uri);
        if (match == UriMatcher.NO_MATCH) {
            throw unknownUri(uri);
        }
        return match;
    }

    @Override
    protected void notifyChange(ContentResolver resolver, Uri uri, boolean syncToNetwork) {
        if (mNotifier != null) {
            mNotifier.notifyChange(uri, syncToNetwork);
        } else {
            super.notifyChange(resolver, uri, syncToNetwork);
        }
    }

    protected static IllegalArgumentException unknownUri(Uri uri) {
        return new IllegalArgumentException("Unknown Uri format: " + uri);
    }

    protected static String nestWhere(String matchColumn, String table, String nestedWhere) {
        String query = SQLiteQueryBuilder.buildQueryString(false, table, BASE_COLUMNS_ID,
                nestedWhere, null, null, null, null);
        return matchColumn + IN + NESTED_SELECT_START + query + NESTED_SELECT_END;
    }

    protected static String metadataSelectionFromPhotos(String where) {
        return nestWhere(Metadata.PHOTO_ID, Photos.TABLE, where);
    }

    protected static String photoSelectionFromAlbums(String where) {
        return nestWhere(Photos.ALBUM_ID, Albums.TABLE, where);
    }

    protected static String photoSelectionFromAccounts(String where) {
        return nestWhere(Photos.ACCOUNT_ID, Accounts.TABLE, where);
    }

    protected static String albumSelectionFromAccounts(String where) {
        return nestWhere(Albums.ACCOUNT_ID, Accounts.TABLE, where);
    }

    protected int deleteCascade(Uri uri, int match, String selection, String[] selectionArgs) {
        switch (match) {
            case MATCH_PHOTO:
            case MATCH_PHOTO_ID:
                deleteCascade(Metadata.CONTENT_URI, MATCH_METADATA,
                        metadataSelectionFromPhotos(selection), selectionArgs);
                break;
            case MATCH_ALBUM:
            case MATCH_ALBUM_ID:
                deleteCascade(Photos.CONTENT_URI, MATCH_PHOTO,
                        photoSelectionFromAlbums(selection), selectionArgs);
                break;
            case MATCH_ACCOUNT:
            case MATCH_ACCOUNT_ID:
                deleteCascade(Photos.CONTENT_URI, MATCH_PHOTO,
                        photoSelectionFromAccounts(selection), selectionArgs);
                deleteCascade(Albums.CONTENT_URI, MATCH_ALBUM,
                        albumSelectionFromAccounts(selection), selectionArgs);
                break;
        }
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        String table = getTableFromMatch(match, uri);
        int deleted = db.delete(table, selection, selectionArgs);
        if (deleted > 0) {
            postNotifyUri(uri);
        }
        return deleted;
    }

    private static void validateMatchTable(int match) {
        switch (match) {
            case MATCH_PHOTO:
            case MATCH_ALBUM:
            case MATCH_METADATA:
            case MATCH_ACCOUNT:
                break;
            default:
                throw new IllegalArgumentException("Operation not allowed on an existing row.");
        }
    }

    protected Cursor query(String table, String[] columns, String selection,
            String[] selectionArgs, String orderBy, CancellationSignal cancellationSignal) {
        SQLiteDatabase db = getDatabaseHelper().getReadableDatabase();
        if (ApiHelper.HAS_CANCELLATION_SIGNAL) {
            return db.query(false, table, columns, selection, selectionArgs, null, null,
                    orderBy, null, cancellationSignal);
        } else {
            return db.query(table, columns, selection, selectionArgs, null, null, orderBy);
        }
    }

    protected static String[] replaceCount(String[] projection) {
        if (projection != null && projection.length == 1
                && BaseColumns._COUNT.equals(projection[0])) {
            return PROJECTION_COUNT;
        }
        return projection;
    }
}
