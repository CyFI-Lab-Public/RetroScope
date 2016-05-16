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

package com.android.providers.media;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Random;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MiniThumbFile;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

/**
 * Instances of this class are created and put in a queue to be executed sequentially to see if
 * it needs to (re)generate the thumbnails.
 */
class MediaThumbRequest {
    private static final String TAG = "MediaThumbRequest";
    static final int PRIORITY_LOW = 20;
    static final int PRIORITY_NORMAL = 10;
    static final int PRIORITY_HIGH = 5;
    static final int PRIORITY_CRITICAL = 0;
    static enum State {WAIT, DONE, CANCEL}
    private static final String[] THUMB_PROJECTION = new String[] {
        BaseColumns._ID // 0
    };

    ContentResolver mCr;
    String mPath;
    long mRequestTime = System.currentTimeMillis();
    int mCallingPid = Binder.getCallingPid();
    long mGroupId;
    int mPriority;
    Uri mUri;
    Uri mThumbUri;
    String mOrigColumnName;
    boolean mIsVideo;
    long mOrigId;
    State mState = State.WAIT;
    long mMagic;

    private static final Random sRandom = new Random();

    static Comparator<MediaThumbRequest> getComparator() {
        return new Comparator<MediaThumbRequest>() {
            public int compare(MediaThumbRequest r1, MediaThumbRequest r2) {
                if (r1.mPriority != r2.mPriority) {
                    return r1.mPriority < r2.mPriority ? -1 : 1;
                }
                return r1.mRequestTime == r2.mRequestTime ? 0 :
                        r1.mRequestTime < r2.mRequestTime ? -1 : 1;
            }
        };
    }

    MediaThumbRequest(ContentResolver cr, String path, Uri uri, int priority, long magic) {
        mCr = cr;
        mPath = path;
        mPriority = priority;
        mMagic = magic;
        mUri = uri;
        mIsVideo = "video".equals(uri.getPathSegments().get(1));
        mOrigId = ContentUris.parseId(uri);
        mThumbUri = mIsVideo
                ? Video.Thumbnails.EXTERNAL_CONTENT_URI
                : Images.Thumbnails.EXTERNAL_CONTENT_URI;
        mOrigColumnName = mIsVideo
                ? Video.Thumbnails.VIDEO_ID
                : Images.Thumbnails.IMAGE_ID;
        // Only requests from Thumbnail API has this group_id parameter. In other cases,
        // mGroupId will always be zero and can't be canceled due to pid mismatch.
        String groupIdParam = uri.getQueryParameter("group_id");
        if (groupIdParam != null) {
            mGroupId = Long.parseLong(groupIdParam);
        }
    }

    Uri updateDatabase(Bitmap thumbnail) {
        Cursor c = mCr.query(mThumbUri, THUMB_PROJECTION,
                mOrigColumnName+ " = " + mOrigId, null, null);
        if (c == null) return null;
        try {
            if (c.moveToFirst()) {
                return ContentUris.withAppendedId(mThumbUri, c.getLong(0));
            }
        } finally {
            if (c != null) c.close();
        }

        ContentValues values = new ContentValues(4);
        values.put(Images.Thumbnails.KIND, Images.Thumbnails.MINI_KIND);
        values.put(mOrigColumnName, mOrigId);
        values.put(Images.Thumbnails.WIDTH, thumbnail.getWidth());
        values.put(Images.Thumbnails.HEIGHT, thumbnail.getHeight());
        try {
            return mCr.insert(mThumbUri, values);
        } catch (Exception ex) {
            Log.w(TAG, ex);
            return null;
        }
    }

    /**
     * Check if the corresponding thumbnail and mini-thumb have been created
     * for the given uri. This method creates both of them if they do not
     * exist yet or have been changed since last check. After thumbnails are
     * created, MINI_KIND thumbnail is stored in JPEG file and MICRO_KIND
     * thumbnail is stored in a random access file (MiniThumbFile).
     *
     * @throws IOException
     */
    void execute() throws IOException {
        MiniThumbFile miniThumbFile = MiniThumbFile.instance(mUri);
        long magic = mMagic;
        if (magic != 0) {
            long fileMagic = miniThumbFile.getMagic(mOrigId);
            if (fileMagic == magic) {
                Cursor c = null;
                ParcelFileDescriptor pfd = null;
                try {
                    c = mCr.query(mThumbUri, THUMB_PROJECTION,
                            mOrigColumnName + " = " + mOrigId, null, null);
                    if (c != null && c.moveToFirst()) {
                        pfd = mCr.openFileDescriptor(
                                mThumbUri.buildUpon().appendPath(c.getString(0)).build(), "r");
                    }
                } catch (IOException ex) {
                    // MINI_THUMBNAIL not exists, ignore the exception and generate one.
                } finally {
                    if (c != null) c.close();
                    if (pfd != null) {
                        pfd.close();
                        return;
                    }
                }
            }
        }

        // If we can't retrieve the thumbnail, first check if there is one
        // embedded in the EXIF data. If not, or it's not big enough,
        // decompress the full size image.
        Bitmap bitmap = null;

        if (mPath != null) {
            if (mIsVideo) {
                bitmap = ThumbnailUtils.createVideoThumbnail(mPath,
                        Video.Thumbnails.MINI_KIND);
            } else {
                bitmap = ThumbnailUtils.createImageThumbnail(mPath,
                        Images.Thumbnails.MINI_KIND);
            }
            if (bitmap == null) {
                Log.w(TAG, "Can't create mini thumbnail for " + mPath);
                return;
            }

            Uri uri = updateDatabase(bitmap);
            if (uri != null) {
                OutputStream thumbOut = mCr.openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, thumbOut);
                thumbOut.close();
            }
        }

        bitmap = ThumbnailUtils.extractThumbnail(bitmap,
                        ThumbnailUtils.TARGET_SIZE_MICRO_THUMBNAIL,
                        ThumbnailUtils.TARGET_SIZE_MICRO_THUMBNAIL,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        if (bitmap != null) {
            ByteArrayOutputStream miniOutStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, miniOutStream);
            bitmap.recycle();
            byte [] data = null;

            try {
                miniOutStream.close();
                data = miniOutStream.toByteArray();
            } catch (java.io.IOException ex) {
                Log.e(TAG, "got exception ex " + ex);
            }

            // We may consider retire this proprietary format, after all it's size is only
            // 128 x 128 at most, which is still reasonable to be stored in database.
            // Gallery application can use the MINI_THUMB_MAGIC value to determine if it's
            // time to query and fetch by using Cursor.getBlob
            if (data != null) {
                // make a new magic number since things are out of sync
                do {
                    magic = sRandom.nextLong();
                } while (magic == 0);

                miniThumbFile.saveMiniThumbToFile(data, mOrigId, magic);
                ContentValues values = new ContentValues();
                // both video/images table use the same column name "mini_thumb_magic"
                values.put(ImageColumns.MINI_THUMB_MAGIC, magic);
                mCr.update(mUri, values, null, null);
            }
        } else {
            Log.w(TAG, "can't create bitmap for thumbnail.");
        }
    }
}
