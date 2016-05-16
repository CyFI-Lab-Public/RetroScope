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

public class FilterEmptyPromptSet extends MediaSet implements ContentListener {
    @SuppressWarnings("unused")
    private static final String TAG = "FilterEmptyPromptSet";

    private ArrayList<MediaItem> mEmptyItem;
    private MediaSet mBaseSet;

    public FilterEmptyPromptSet(Path path, MediaSet baseSet, MediaItem emptyItem) {
        super(path, INVALID_DATA_VERSION);
        mEmptyItem = new ArrayList<MediaItem>(1);
        mEmptyItem.add(emptyItem);
        mBaseSet = baseSet;
        mBaseSet.addContentListener(this);
    }

    @Override
    public int getMediaItemCount() {
        int itemCount = mBaseSet.getMediaItemCount();
        if (itemCount > 0) {
            return itemCount;
        } else {
            return 1;
        }
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        int itemCount = mBaseSet.getMediaItemCount();
        if (itemCount > 0) {
            return mBaseSet.getMediaItem(start, count);
        } else if (start == 0 && count == 1) {
            return mEmptyItem;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    @Override
    public boolean isCameraRoll() {
        return mBaseSet.isCameraRoll();
    }

    @Override
    public long reload() {
        return mBaseSet.reload();
    }

    @Override
    public String getName() {
        return mBaseSet.getName();
    }
}
