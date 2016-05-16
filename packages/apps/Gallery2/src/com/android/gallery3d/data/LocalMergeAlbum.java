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

import android.net.Uri;
import android.provider.MediaStore;

import com.android.gallery3d.common.ApiHelper;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

// MergeAlbum merges items from two or more MediaSets. It uses a Comparator to
// determine the order of items. The items are assumed to be sorted in the input
// media sets (with the same order that the Comparator uses).
//
// This only handles MediaItems, not SubMediaSets.
public class LocalMergeAlbum extends MediaSet implements ContentListener {
    @SuppressWarnings("unused")
    private static final String TAG = "LocalMergeAlbum";
    private static final int PAGE_SIZE = 64;

    private final Comparator<MediaItem> mComparator;
    private final MediaSet[] mSources;

    private FetchCache[] mFetcher;
    private int mSupportedOperation;
    private int mBucketId;

    // mIndex maps global position to the position of each underlying media sets.
    private TreeMap<Integer, int[]> mIndex = new TreeMap<Integer, int[]>();

    public LocalMergeAlbum(
            Path path, Comparator<MediaItem> comparator, MediaSet[] sources, int bucketId) {
        super(path, INVALID_DATA_VERSION);
        mComparator = comparator;
        mSources = sources;
        mBucketId = bucketId;
        for (MediaSet set : mSources) {
            set.addContentListener(this);
        }
        reload();
    }

    @Override
    public boolean isCameraRoll() {
        if (mSources.length == 0) return false;
        for(MediaSet set : mSources) {
            if (!set.isCameraRoll()) return false;
        }
        return true;
    }

    private void updateData() {
        ArrayList<MediaSet> matches = new ArrayList<MediaSet>();
        int supported = mSources.length == 0 ? 0 : MediaItem.SUPPORT_ALL;
        mFetcher = new FetchCache[mSources.length];
        for (int i = 0, n = mSources.length; i < n; ++i) {
            mFetcher[i] = new FetchCache(mSources[i]);
            supported &= mSources[i].getSupportedOperations();
        }
        mSupportedOperation = supported;
        mIndex.clear();
        mIndex.put(0, new int[mSources.length]);
    }

    private void invalidateCache() {
        for (int i = 0, n = mSources.length; i < n; i++) {
            mFetcher[i].invalidate();
        }
        mIndex.clear();
        mIndex.put(0, new int[mSources.length]);
    }

    @Override
    public Uri getContentUri() {
        String bucketId = String.valueOf(mBucketId);
        if (ApiHelper.HAS_MEDIA_PROVIDER_FILES_TABLE) {
            return MediaStore.Files.getContentUri("external").buildUpon()
                    .appendQueryParameter(LocalSource.KEY_BUCKET_ID, bucketId)
                    .build();
        } else {
            // We don't have a single URL for a merged image before ICS
            // So we used the image's URL as a substitute.
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendQueryParameter(LocalSource.KEY_BUCKET_ID, bucketId)
                    .build();
        }
    }

    @Override
    public String getName() {
        return mSources.length == 0 ? "" : mSources[0].getName();
    }

    @Override
    public int getMediaItemCount() {
        return getTotalMediaItemCount();
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {

        // First find the nearest mark position <= start.
        SortedMap<Integer, int[]> head = mIndex.headMap(start + 1);
        int markPos = head.lastKey();
        int[] subPos = head.get(markPos).clone();
        MediaItem[] slot = new MediaItem[mSources.length];

        int size = mSources.length;

        // fill all slots
        for (int i = 0; i < size; i++) {
            slot[i] = mFetcher[i].getItem(subPos[i]);
        }

        ArrayList<MediaItem> result = new ArrayList<MediaItem>();

        for (int i = markPos; i < start + count; i++) {
            int k = -1;  // k points to the best slot up to now.
            for (int j = 0; j < size; j++) {
                if (slot[j] != null) {
                    if (k == -1 || mComparator.compare(slot[j], slot[k]) < 0) {
                        k = j;
                    }
                }
            }

            // If we don't have anything, all streams are exhausted.
            if (k == -1) break;

            // Pick the best slot and refill it.
            subPos[k]++;
            if (i >= start) {
                result.add(slot[k]);
            }
            slot[k] = mFetcher[k].getItem(subPos[k]);

            // Periodically leave a mark in the index, so we can come back later.
            if ((i + 1) % PAGE_SIZE == 0) {
                mIndex.put(i + 1, subPos.clone());
            }
        }

        return result;
    }

    @Override
    public int getTotalMediaItemCount() {
        int count = 0;
        for (MediaSet set : mSources) {
            count += set.getTotalMediaItemCount();
        }
        return count;
    }

    @Override
    public long reload() {
        boolean changed = false;
        for (int i = 0, n = mSources.length; i < n; ++i) {
            if (mSources[i].reload() > mDataVersion) changed = true;
        }
        if (changed) {
            mDataVersion = nextVersionNumber();
            updateData();
            invalidateCache();
        }
        return mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    @Override
    public int getSupportedOperations() {
        return mSupportedOperation;
    }

    @Override
    public void delete() {
        for (MediaSet set : mSources) {
            set.delete();
        }
    }

    @Override
    public void rotate(int degrees) {
        for (MediaSet set : mSources) {
            set.rotate(degrees);
        }
    }

    private static class FetchCache {
        private MediaSet mBaseSet;
        private SoftReference<ArrayList<MediaItem>> mCacheRef;
        private int mStartPos;

        public FetchCache(MediaSet baseSet) {
            mBaseSet = baseSet;
        }

        public void invalidate() {
            mCacheRef = null;
        }

        public MediaItem getItem(int index) {
            boolean needLoading = false;
            ArrayList<MediaItem> cache = null;
            if (mCacheRef == null
                    || index < mStartPos || index >= mStartPos + PAGE_SIZE) {
                needLoading = true;
            } else {
                cache = mCacheRef.get();
                if (cache == null) {
                    needLoading = true;
                }
            }

            if (needLoading) {
                cache = mBaseSet.getMediaItem(index, PAGE_SIZE);
                mCacheRef = new SoftReference<ArrayList<MediaItem>>(cache);
                mStartPos = index;
            }

            if (index < mStartPos || index >= mStartPos + cache.size()) {
                return null;
            }

            return cache.get(index - mStartPos);
        }
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }
}
