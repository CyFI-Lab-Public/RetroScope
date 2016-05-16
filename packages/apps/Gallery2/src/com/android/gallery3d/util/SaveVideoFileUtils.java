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

package com.android.gallery3d.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;

import com.android.gallery3d.filtershow.tools.SaveImage.ContentResolverQueryCallback;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;

public class SaveVideoFileUtils {
    // This function can decide which folder to save the video file, and generate
    // the needed information for the video file including filename.
    public static SaveVideoFileInfo getDstMp4FileInfo(String fileNameFormat,
            ContentResolver contentResolver, Uri uri, String defaultFolderName) {
        SaveVideoFileInfo dstFileInfo = new SaveVideoFileInfo();
        // Use the default save directory if the source directory cannot be
        // saved.
        dstFileInfo.mDirectory = getSaveDirectory(contentResolver, uri);
        if ((dstFileInfo.mDirectory == null) || !dstFileInfo.mDirectory.canWrite()) {
            dstFileInfo.mDirectory = new File(Environment.getExternalStorageDirectory(),
                    BucketNames.DOWNLOAD);
            dstFileInfo.mFolderName = defaultFolderName;
        } else {
            dstFileInfo.mFolderName = dstFileInfo.mDirectory.getName();
        }
        dstFileInfo.mFileName = new SimpleDateFormat(fileNameFormat).format(
                new Date(System.currentTimeMillis()));

        dstFileInfo.mFile = new File(dstFileInfo.mDirectory, dstFileInfo.mFileName + ".mp4");
        return dstFileInfo;
    }

    private static void querySource(ContentResolver contentResolver, Uri uri,
            String[] projection, ContentResolverQueryCallback callback) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, projection, null, null, null);
            if ((cursor != null) && cursor.moveToNext()) {
                callback.onCursorResult(cursor);
            }
        } catch (Exception e) {
            // Ignore error for lacking the data column from the source.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static File getSaveDirectory(ContentResolver contentResolver, Uri uri) {
        final File[] dir = new File[1];
        querySource(contentResolver, uri,
                new String[] { VideoColumns.DATA },
                new ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                dir[0] = new File(cursor.getString(0)).getParentFile();
            }
        });
        return dir[0];
    }


    /**
     * Insert the content (saved file) with proper video properties.
     */
    public static Uri insertContent(SaveVideoFileInfo mDstFileInfo,
            ContentResolver contentResolver, Uri uri ) {
        long nowInMs = System.currentTimeMillis();
        long nowInSec = nowInMs / 1000;
        final ContentValues values = new ContentValues(13);
        values.put(Video.Media.TITLE, mDstFileInfo.mFileName);
        values.put(Video.Media.DISPLAY_NAME, mDstFileInfo.mFile.getName());
        values.put(Video.Media.MIME_TYPE, "video/mp4");
        values.put(Video.Media.DATE_TAKEN, nowInMs);
        values.put(Video.Media.DATE_MODIFIED, nowInSec);
        values.put(Video.Media.DATE_ADDED, nowInSec);
        values.put(Video.Media.DATA, mDstFileInfo.mFile.getAbsolutePath());
        values.put(Video.Media.SIZE, mDstFileInfo.mFile.length());
        int durationMs = retriveVideoDurationMs(mDstFileInfo.mFile.getPath());
        values.put(Video.Media.DURATION, durationMs);
        // Copy the data taken and location info from src.
        String[] projection = new String[] {
                VideoColumns.DATE_TAKEN,
                VideoColumns.LATITUDE,
                VideoColumns.LONGITUDE,
                VideoColumns.RESOLUTION,
        };

        // Copy some info from the source file.
        querySource(contentResolver, uri, projection,
                new ContentResolverQueryCallback() {
                @Override
                    public void onCursorResult(Cursor cursor) {
                        long timeTaken = cursor.getLong(0);
                        if (timeTaken > 0) {
                            values.put(Video.Media.DATE_TAKEN, timeTaken);
                        }
                        double latitude = cursor.getDouble(1);
                        double longitude = cursor.getDouble(2);
                        // TODO: Change || to && after the default location
                        // issue is
                        // fixed.
                        if ((latitude != 0f) || (longitude != 0f)) {
                            values.put(Video.Media.LATITUDE, latitude);
                            values.put(Video.Media.LONGITUDE, longitude);
                        }
                        values.put(Video.Media.RESOLUTION, cursor.getString(3));

                    }
                });

        return contentResolver.insert(Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static int retriveVideoDurationMs(String path) {
        int durationMs = 0;
        // Calculate the duration of the destination file.
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (duration != null) {
            durationMs = Integer.parseInt(duration);
        }
        retriever.release();
        return durationMs;
    }

}
