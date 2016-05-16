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
package com.android.dreams.phototable;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

/**
 * Loads images from the local store.
 */
public class LocalSource extends CursorPhotoSource {
    private static final String TAG = "PhotoTable.LocalSource";

    private final String mUnknownAlbumName;
    private final String mLocalSourceName;
    private Set<String> mFoundAlbumIds;
    private int mLastPosition;

    public LocalSource(Context context, SharedPreferences settings) {
        super(context, settings);
        mLocalSourceName = mResources.getString(R.string.local_source_name, "Photos on Device");
        mUnknownAlbumName = mResources.getString(R.string.unknown_album_name, "Unknown");
        mSourceName = TAG;
        mLastPosition = INVALID;
        fillQueue();
    }

    private Set<String> getFoundAlbums() {
        if (mFoundAlbumIds == null) {
            findAlbums();
        }
        return mFoundAlbumIds;
    }

    @Override
    public Collection<AlbumData> findAlbums() {
        log(TAG, "finding albums");
        HashMap<String, AlbumData> foundAlbums = new HashMap<String, AlbumData>();
        findAlbums(false, foundAlbums);
        findAlbums(true, foundAlbums);

        log(TAG, "found " + foundAlbums.size() + " items.");
        mFoundAlbumIds = foundAlbums.keySet();
        return foundAlbums.values();
    }

    public void findAlbums(boolean internal, HashMap<String, AlbumData> foundAlbums) {
        Uri uri = internal ? MediaStore.Images.Media.INTERNAL_CONTENT_URI
            : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DATE_TAKEN};
        // This is a horrible hack that closes the where clause and injects a grouping clause.
        Cursor cursor = mResolver.query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToPosition(-1);

            int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            int bucketIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
            int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int updatedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);

            if (bucketIndex < 0) {
                log(TAG, "can't find the ID column!");
            } else {
                while (cursor.moveToNext()) {
                    String id = constructId(internal, cursor.getString(bucketIndex));
                    AlbumData data = foundAlbums.get(id);
                    if (foundAlbums.get(id) == null) {
                        data = new AlbumData();
                        data.id = id;
                        data.account = mLocalSourceName;

                        if (dataIndex >= 0) {
                            data.thumbnailUrl = cursor.getString(dataIndex);
                        }

                        if (nameIndex >= 0) {
                            data.title = cursor.getString(nameIndex);
                        } else {
                            data.title = mUnknownAlbumName;
                        }

                        log(TAG, data.title + " found");
                        foundAlbums.put(id, data);
                    }
                    if (updatedIndex >= 0) {
                        long updated = cursor.getLong(updatedIndex);
                        data.updated = (data.updated == 0 ?
                                        updated :
                                        Math.min(data.updated, updated));
                    }
                }
            }
            cursor.close();
        }
    }

    public static String constructId(boolean internal, String bucketId) {
        return TAG + ":" + bucketId + (internal ? ":i" : "");
    }

    @Override
    protected void openCursor(ImageData data) {
        log(TAG, "opening single album");

        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
        String selection = MediaStore.Images.Media.BUCKET_ID + " = '" + data.albumId + "'";

        data.cursor = mResolver.query(data.uri, projection, selection, null, null);
    }

    @Override
    protected void findPosition(ImageData data) {
        if (data.position == -1) {
            if (data.cursor == null) {
                openCursor(data);
            }
            if (data.cursor != null) {
                int dataIndex = data.cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                data.cursor.moveToPosition(-1);
                while (data.position == -1 && data.cursor.moveToNext()) {
                    String url = data.cursor.getString(dataIndex);
                    if (url != null && url.equals(data.url)) {
                        data.position = data.cursor.getPosition();
                    }
                }
                if (data.position == -1) {
                    // oops!  The image isn't in this album. How did we get here?
                    data.position = INVALID;
                }
            }
        }
    }

    @Override
    protected ImageData unpackImageData(Cursor cursor, ImageData data) {
        if (data == null) {
            data = new ImageData();
        }
        int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        int orientationIndex = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
        int bucketIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);

        data.url = cursor.getString(dataIndex);
        data.albumId = cursor.getString(bucketIndex);
        data.position = UNINITIALIZED;
        data.cursor = null;
        data.orientation = cursor.getInt(orientationIndex);

        return data;
    }

    @Override
    protected Collection<ImageData> findImages(int howMany) {
        log(TAG, "finding images");
        LinkedList<ImageData> foundImages = new LinkedList<ImageData>();
        boolean internalFirst = mRNG.nextInt(2) == 0;  // filp a coin to be fair
        findImages(internalFirst, howMany, foundImages);
        findImages(!internalFirst, howMany - foundImages.size(), foundImages);
        log(TAG, "found " + foundImages.size() + " items.");
        return foundImages;
    }

    protected void findImages(boolean internal, int howMany, LinkedList<ImageData> foundImages ) {
        Uri uri = internal ? MediaStore.Images.Media.INTERNAL_CONTENT_URI
            : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
        String selection = "";
        for (String id : getFoundAlbums()) {
            if (isInternalId(id) == internal && mSettings.isAlbumEnabled(id)) {
                String[] parts = id.split(":");
                if (parts.length > 1) {
                    if (selection.length() > 0) {
                        selection += " OR ";
                    }
                    selection += MediaStore.Images.Media.BUCKET_ID + " = '" + parts[1] + "'";
                }
            }
        }
        if (selection.isEmpty()) {
            return;
        }
        Cursor cursor = mResolver.query(uri, projection, selection, null, null);
        if (cursor != null) {
            int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

            if (cursor.getCount() > howMany && mLastPosition == INVALID) {
                mLastPosition = pickRandomStart(cursor.getCount(), howMany);
            }
            cursor.moveToPosition(mLastPosition);

            if (dataIndex < 0) {
                log(TAG, "can't find the DATA column!");
            } else {
                while (foundImages.size() < howMany && cursor.moveToNext()) {
                    ImageData data = unpackImageData(cursor, null);
                    data.uri = uri;
                    foundImages.offer(data);
                    mLastPosition = cursor.getPosition();
                }
                if (cursor.isAfterLast()) {
                    mLastPosition = -1;
                }
                if (cursor.isBeforeFirst()) {
                    mLastPosition = INVALID;
                }
            }

            cursor.close();
        }
    }

    private boolean isInternalId(String id) {
        return id.endsWith("i");
    }

    @Override
    protected InputStream getStream(ImageData data, int longSide) {
        FileInputStream fis = null;
        try {
            log(TAG, "opening:" + data.url);
            fis = new FileInputStream(data.url);
        } catch (Exception ex) {
            log(TAG, ex.toString());
            fis = null;
        }

        return (InputStream) fis;
    }
}

