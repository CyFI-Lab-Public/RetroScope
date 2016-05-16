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

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.util.Future;

// ComboAlbumSet combines multiple media sets into one. It lists all sub
// media sets from the input album sets.
// This only handles SubMediaSets, not MediaItems. (That's all we need now)
public class ComboAlbumSet extends MediaSet implements ContentListener {
    @SuppressWarnings("unused")
    private static final String TAG = "ComboAlbumSet";
    private final MediaSet[] mSets;
    private final String mName;

    public ComboAlbumSet(Path path, GalleryApp application, MediaSet[] mediaSets) {
        super(path, nextVersionNumber());
        mSets = mediaSets;
        for (MediaSet set : mSets) {
            set.addContentListener(this);
        }
        mName = application.getResources().getString(
                R.string.set_label_all_albums);
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        for (MediaSet set : mSets) {
            int size = set.getSubMediaSetCount();
            if (index < size) {
                return set.getSubMediaSet(index);
            }
            index -= size;
        }
        return null;
    }

    @Override
    public int getSubMediaSetCount() {
        int count = 0;
        for (MediaSet set : mSets) {
            count += set.getSubMediaSetCount();
        }
        return count;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public boolean isLoading() {
        for (int i = 0, n = mSets.length; i < n; ++i) {
            if (mSets[i].isLoading()) return true;
        }
        return false;
    }

    @Override
    public long reload() {
        boolean changed = false;
        for (int i = 0, n = mSets.length; i < n; ++i) {
            long version = mSets[i].reload();
            if (version > mDataVersion) changed = true;
        }
        if (changed) mDataVersion = nextVersionNumber();
        return mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    @Override
    public Future<Integer> requestSync(SyncListener listener) {
        return requestSyncOnMultipleSets(mSets, listener);
    }
}
