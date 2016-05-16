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

package com.android.gallery3d.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.StitchingChangeListener;
import com.android.gallery3d.util.MediaSetUtils;

import java.util.ArrayList;

// This class lists all media items added by the client.
public class SecureAlbum extends MediaSet implements StitchingChangeListener {
    @SuppressWarnings("unused")
    private static final String TAG = "SecureAlbum";
    private static final String[] PROJECTION = {MediaColumns._ID};
    private int mMinImageId = Integer.MAX_VALUE; // the smallest id of images
    private int mMaxImageId = Integer.MIN_VALUE; // the biggest id in images
    private int mMinVideoId = Integer.MAX_VALUE; // the smallest id of videos
    private int mMaxVideoId = Integer.MIN_VALUE; // the biggest id of videos
    // All the media items added by the client.
    private ArrayList<Path> mAllItems = new ArrayList<Path>();
    // The types of items in mAllItems. True is video and false is image.
    private ArrayList<Boolean> mAllItemTypes = new ArrayList<Boolean>();
    private ArrayList<Path> mExistingItems = new ArrayList<Path>();
    private Context mContext;
    private DataManager mDataManager;
    private static final Uri[] mWatchUris =
        {Images.Media.EXTERNAL_CONTENT_URI, Video.Media.EXTERNAL_CONTENT_URI};
    private final ChangeNotifier mNotifier;
    // A placeholder image in the end of secure album. When it is tapped, it
    // will take the user to the lock screen.
    private MediaItem mUnlockItem;
    private boolean mShowUnlockItem;

    public SecureAlbum(Path path, GalleryApp application, MediaItem unlock) {
        super(path, nextVersionNumber());
        mContext = application.getAndroidContext();
        mDataManager = application.getDataManager();
        mNotifier = new ChangeNotifier(this, mWatchUris, application);
        mUnlockItem = unlock;
        mShowUnlockItem = (!isCameraBucketEmpty(Images.Media.EXTERNAL_CONTENT_URI)
                || !isCameraBucketEmpty(Video.Media.EXTERNAL_CONTENT_URI));
    }

    public void addMediaItem(boolean isVideo, int id) {
        Path pathBase;
        if (isVideo) {
            pathBase = LocalVideo.ITEM_PATH;
            mMinVideoId = Math.min(mMinVideoId, id);
            mMaxVideoId = Math.max(mMaxVideoId, id);
        } else {
            pathBase = LocalImage.ITEM_PATH;
            mMinImageId = Math.min(mMinImageId, id);
            mMaxImageId = Math.max(mMaxImageId, id);
        }
        Path path = pathBase.getChild(id);
        if (!mAllItems.contains(path)) {
            mAllItems.add(path);
            mAllItemTypes.add(isVideo);
            mNotifier.fakeChange();
        }
    }

    // The sequence is stitching items, local media items, and unlock image.
    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        int existingCount = mExistingItems.size();
        if (start >= existingCount + 1) {
            return new ArrayList<MediaItem>();
        }

        // Add paths of requested stitching items.
        int end = Math.min(start + count, existingCount);
        ArrayList<Path> subset = new ArrayList<Path>(mExistingItems.subList(start, end));

        // Convert paths to media items.
        final MediaItem[] buf = new MediaItem[end - start];
        ItemConsumer consumer = new ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                buf[index] = item;
            }
        };
        mDataManager.mapMediaItems(subset, consumer, 0);
        ArrayList<MediaItem> result = new ArrayList<MediaItem>(end - start);
        for (int i = 0; i < buf.length; i++) {
            result.add(buf[i]);
        }
        if (mShowUnlockItem) result.add(mUnlockItem);
        return result;
    }

    @Override
    public int getMediaItemCount() {
        return (mExistingItems.size() + (mShowUnlockItem ? 1 : 0));
    }

    @Override
    public String getName() {
        return "secure";
    }

    @Override
    public long reload() {
        if (mNotifier.isDirty()) {
            mDataVersion = nextVersionNumber();
            updateExistingItems();
        }
        return mDataVersion;
    }

    private ArrayList<Integer> queryExistingIds(Uri uri, int minId, int maxId) {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        if (minId == Integer.MAX_VALUE || maxId == Integer.MIN_VALUE) return ids;

        String[] selectionArgs = {String.valueOf(minId), String.valueOf(maxId)};
        Cursor cursor = mContext.getContentResolver().query(uri, PROJECTION,
                "_id BETWEEN ? AND ?", selectionArgs, null);
        if (cursor == null) return ids;
        try {
            while (cursor.moveToNext()) {
                ids.add(cursor.getInt(0));
            }
        } finally {
            cursor.close();
        }
        return ids;
    }

    private boolean isCameraBucketEmpty(Uri baseUri) {
        Uri uri = baseUri.buildUpon()
                .appendQueryParameter("limit", "1").build();
        String[] selection = {String.valueOf(MediaSetUtils.CAMERA_BUCKET_ID)};
        Cursor cursor = mContext.getContentResolver().query(uri, PROJECTION,
                "bucket_id = ?", selection, null);
        if (cursor == null) return true;
        try {
            return (cursor.getCount() == 0);
        } finally {
            cursor.close();
        }
    }

    private void updateExistingItems() {
        if (mAllItems.size() == 0) return;

        // Query existing ids.
        ArrayList<Integer> imageIds = queryExistingIds(
                Images.Media.EXTERNAL_CONTENT_URI, mMinImageId, mMaxImageId);
        ArrayList<Integer> videoIds = queryExistingIds(
                Video.Media.EXTERNAL_CONTENT_URI, mMinVideoId, mMaxVideoId);

        // Construct the existing items list.
        mExistingItems.clear();
        for (int i = mAllItems.size() - 1; i >= 0; i--) {
            Path path = mAllItems.get(i);
            boolean isVideo = mAllItemTypes.get(i);
            int id = Integer.parseInt(path.getSuffix());
            if (isVideo) {
                if (videoIds.contains(id)) mExistingItems.add(path);
            } else {
                if (imageIds.contains(id)) mExistingItems.add(path);
            }
        }
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    @Override
    public void onStitchingQueued(Uri uri) {
        int id = Integer.parseInt(uri.getLastPathSegment());
        addMediaItem(false, id);
    }

    @Override
    public void onStitchingResult(Uri uri) {
    }

    @Override
    public void onStitchingProgress(Uri uri, final int progress) {
    }
}
