/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.ArrayList;

// FilterTypeSet filters a base MediaSet according to a matching media type.
public class FilterTypeSet extends MediaSet implements ContentListener {
    @SuppressWarnings("unused")
    private static final String TAG = "FilterTypeSet";

    private final DataManager mDataManager;
    private final MediaSet mBaseSet;
    private final int mMediaType;
    private final ArrayList<Path> mPaths = new ArrayList<Path>();
    private final ArrayList<MediaSet> mAlbums = new ArrayList<MediaSet>();

    public FilterTypeSet(Path path, DataManager dataManager, MediaSet baseSet,
            int mediaType) {
        super(path, INVALID_DATA_VERSION);
        mDataManager = dataManager;
        mBaseSet = baseSet;
        mMediaType = mediaType;
        mBaseSet.addContentListener(this);
    }

    @Override
    public String getName() {
        return mBaseSet.getName();
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    @Override
    public int getSubMediaSetCount() {
        return mAlbums.size();
    }

    @Override
    public int getMediaItemCount() {
        return mPaths.size();
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        return ClusterAlbum.getMediaItemFromPath(
                mPaths, start, count, mDataManager);
    }

    @Override
    public long reload() {
        if (mBaseSet.reload() > mDataVersion) {
            updateData();
            mDataVersion = nextVersionNumber();
        }
        return mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    private void updateData() {
        // Albums
        mAlbums.clear();
        String basePath = "/filter/mediatype/" + mMediaType;

        for (int i = 0, n = mBaseSet.getSubMediaSetCount(); i < n; i++) {
            MediaSet set = mBaseSet.getSubMediaSet(i);
            String filteredPath = basePath + "/{" + set.getPath().toString() + "}";
            MediaSet filteredSet = mDataManager.getMediaSet(filteredPath);
            filteredSet.reload();
            if (filteredSet.getMediaItemCount() > 0
                    || filteredSet.getSubMediaSetCount() > 0) {
                mAlbums.add(filteredSet);
            }
        }

        // Items
        mPaths.clear();
        final int total = mBaseSet.getMediaItemCount();
        final Path[] buf = new Path[total];

        mBaseSet.enumerateMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                if (item.getMediaType() == mMediaType) {
                    if (index < 0 || index >= total) return;
                    Path path = item.getPath();
                    buf[index] = path;
                }
            }
        });

        for (int i = 0; i < total; i++) {
            if (buf[i] != null) {
                mPaths.add(buf[i]);
            }
        }
    }

    @Override
    public int getSupportedOperations() {
        return SUPPORT_SHARE | SUPPORT_DELETE;
    }

    @Override
    public void delete() {
        ItemConsumer consumer = new ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                if ((item.getSupportedOperations() & SUPPORT_DELETE) != 0) {
                    item.delete();
                }
            }
        };
        mDataManager.mapMediaItems(mPaths, consumer, 0);
    }
}
