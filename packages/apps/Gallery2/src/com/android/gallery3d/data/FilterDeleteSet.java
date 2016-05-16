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

import java.util.ArrayList;

// FilterDeleteSet filters a base MediaSet to remove some deletion items (we
// expect the number to be small). The user can use the following methods to
// add/remove deletion items:
//
// void addDeletion(Path path, int index);
// void removeDelection(Path path);
// void clearDeletion();
// int getNumberOfDeletions();
//
public class FilterDeleteSet extends MediaSet implements ContentListener {
    @SuppressWarnings("unused")
    private static final String TAG = "FilterDeleteSet";

    private static final int REQUEST_ADD = 1;
    private static final int REQUEST_REMOVE = 2;
    private static final int REQUEST_CLEAR = 3;

    private static class Request {
        int type;  // one of the REQUEST_* constants
        Path path;
        int indexHint;
        public Request(int type, Path path, int indexHint) {
            this.type = type;
            this.path = path;
            this.indexHint = indexHint;
        }
    }

    private static class Deletion {
        Path path;
        int index;
        public Deletion(Path path, int index) {
            this.path = path;
            this.index = index;
        }
    }

    // The underlying MediaSet
    private final MediaSet mBaseSet;

    // Pending Requests
    private ArrayList<Request> mRequests = new ArrayList<Request>();

    // Deletions currently in effect, ordered by index
    private ArrayList<Deletion> mCurrent = new ArrayList<Deletion>();

    public FilterDeleteSet(Path path, MediaSet baseSet) {
        super(path, INVALID_DATA_VERSION);
        mBaseSet = baseSet;
        mBaseSet.addContentListener(this);
    }

    @Override
    public boolean isCameraRoll() {
        return mBaseSet.isCameraRoll();
    }

    @Override
    public String getName() {
        return mBaseSet.getName();
    }

    @Override
    public int getMediaItemCount() {
        return mBaseSet.getMediaItemCount() - mCurrent.size();
    }

    // Gets the MediaItems whose (post-deletion) index are in the range [start,
    // start + count). Because we remove some of the MediaItems, the index need
    // to be adjusted.
    //
    // For example, if there are 12 items in total. The deleted items are 3, 5,
    // 10, and the the requested range is [3, 7]:
    //
    // The original index:   0 1 2 3 4 5 6 7 8 9 A B C
    // The deleted items:          X   X         X
    // The new index:        0 1 2   3   4 5 6 7   8 9
    // Requested:                    *   * * * *
    //
    // We need to figure out the [3, 7] actually maps to the original index 4,
    // 6, 7, 8, 9.
    //
    // We can break the MediaItems into segments, each segment other than the
    // last one ends in a deleted item. The difference between the new index and
    // the original index increases with each segment:
    //
    // 0 1 2 X     (new index = old index)
    // 4 X         (new index = old index - 1)
    // 6 7 8 9 X   (new index = old index - 2)
    // B C         (new index = old index - 3)
    //
    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        if (count <= 0) return new ArrayList<MediaItem>();

        int end = start + count - 1;
        int n = mCurrent.size();
        // Find the segment that "start" falls into. Count the number of items
        // not yet deleted until it reaches "start".
        int i = 0;
        for (i = 0; i < n; i++) {
            Deletion d = mCurrent.get(i);
            if (d.index - i > start) break;
        }
        // Find the segment that "end" falls into.
        int j = i;
        for (; j < n; j++) {
            Deletion d = mCurrent.get(j);
            if (d.index - j > end) break;
        }

        // Now get enough to cover deleted items in [start, end]
        ArrayList<MediaItem> base = mBaseSet.getMediaItem(start + i, count + (j - i));

        // Remove the deleted items.
        for (int m = j - 1; m >= i; m--) {
            Deletion d = mCurrent.get(m);
            int k = d.index - (start + i);
            base.remove(k);
        }
        return base;
    }

    // We apply the pending requests in the mRequests to construct mCurrent in reload().
    @Override
    public long reload() {
        boolean newData = mBaseSet.reload() > mDataVersion;
        synchronized (mRequests) {
            if (!newData && mRequests.isEmpty()) {
                return mDataVersion;
            }
            for (int i = 0; i < mRequests.size(); i++) {
                Request r = mRequests.get(i);
                switch (r.type) {
                    case REQUEST_ADD: {
                        // Add the path into mCurrent if there is no duplicate.
                        int n = mCurrent.size();
                        int j;
                        for (j = 0; j < n; j++) {
                            if (mCurrent.get(j).path == r.path) break;
                        }
                        if (j == n) {
                            mCurrent.add(new Deletion(r.path, r.indexHint));
                        }
                        break;
                    }
                    case REQUEST_REMOVE: {
                        // Remove the path from mCurrent.
                        int n = mCurrent.size();
                        for (int j = 0; j < n; j++) {
                            if (mCurrent.get(j).path == r.path) {
                                mCurrent.remove(j);
                                break;
                            }
                        }
                        break;
                    }
                    case REQUEST_CLEAR: {
                        mCurrent.clear();
                        break;
                    }
                }
            }
            mRequests.clear();
        }

        if (!mCurrent.isEmpty()) {
            // See if the elements in mCurrent can be found in the MediaSet. We
            // don't want to search the whole mBaseSet, so we just search a
            // small window that contains the index hints (plus some margin).
            int minIndex = mCurrent.get(0).index;
            int maxIndex = minIndex;
            for (int i = 1; i < mCurrent.size(); i++) {
                Deletion d = mCurrent.get(i);
                minIndex = Math.min(d.index, minIndex);
                maxIndex = Math.max(d.index, maxIndex);
            }

            int n = mBaseSet.getMediaItemCount();
            int from = Math.max(minIndex - 5, 0);
            int to = Math.min(maxIndex + 5, n);
            ArrayList<MediaItem> items = mBaseSet.getMediaItem(from, to - from);
            ArrayList<Deletion> result = new ArrayList<Deletion>();
            for (int i = 0; i < items.size(); i++) {
                MediaItem item = items.get(i);
                if (item == null) continue;
                Path p = item.getPath();
                // Find the matching path in mCurrent, if found move it to result
                for (int j = 0; j < mCurrent.size(); j++) {
                    Deletion d = mCurrent.get(j);
                    if (d.path == p) {
                        d.index = from + i;
                        result.add(d);
                        mCurrent.remove(j);
                        break;
                    }
                }
            }
            mCurrent = result;
        }

        mDataVersion = nextVersionNumber();
        return mDataVersion;
    }

    private void sendRequest(int type, Path path, int indexHint) {
        Request r = new Request(type, path, indexHint);
        synchronized (mRequests) {
            mRequests.add(r);
        }
        notifyContentChanged();
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    public void addDeletion(Path path, int indexHint) {
        sendRequest(REQUEST_ADD, path, indexHint);
    }

    public void removeDeletion(Path path) {
        sendRequest(REQUEST_REMOVE, path, 0 /* unused */);
    }

    public void clearDeletion() {
        sendRequest(REQUEST_CLEAR, null /* unused */ , 0 /* unused */);
    }

    // Returns number of deletions _in effect_ (the number will only gets
    // updated after a reload()).
    public int getNumberOfDeletions() {
        return mCurrent.size();
    }
}
