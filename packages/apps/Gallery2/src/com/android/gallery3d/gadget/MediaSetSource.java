/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.gallery3d.gadget;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;

import java.util.ArrayList;
import java.util.Arrays;

public class MediaSetSource implements WidgetSource, ContentListener {
    private static final String TAG = "MediaSetSource";

    private DataManager mDataManager;
    private Path mAlbumPath;

    private WidgetSource mSource;

    private MediaSet mRootSet;
    private ContentListener mListener;

    public MediaSetSource(DataManager manager, String albumPath) {
        MediaSet mediaSet = (MediaSet) manager.getMediaObject(albumPath);
        if (mediaSet != null) {
            mSource = new CheckedMediaSetSource(mediaSet);
            return;
        }

        // Initialize source to an empty source until the album path can be resolved
        mDataManager = Utils.checkNotNull(manager);
        mAlbumPath = Path.fromString(albumPath);
        mSource = new EmptySource();
        monitorRootPath();
    }

    @Override
    public int size() {
        return mSource.size();
    }

    @Override
    public Bitmap getImage(int index) {
        return mSource.getImage(index);
    }

    @Override
    public Uri getContentUri(int index) {
        return mSource.getContentUri(index);
    }

    @Override
    public synchronized void setContentListener(ContentListener listener) {
        if (mRootSet != null) {
            mListener = listener;
        } else {
            mSource.setContentListener(listener);
        }
    }

    @Override
    public void reload() {
        mSource.reload();
    }

    @Override
    public void close() {
        mSource.close();
    }

    @Override
    public void onContentDirty() {
        resolveAlbumPath();
    }

    private void monitorRootPath() {
        String rootPath = mDataManager.getTopSetPath(DataManager.INCLUDE_ALL);
        mRootSet = (MediaSet) mDataManager.getMediaObject(rootPath);
        mRootSet.addContentListener(this);
    }

    private synchronized void resolveAlbumPath() {
        if (mDataManager == null) return;
        MediaSet mediaSet = (MediaSet) mDataManager.getMediaObject(mAlbumPath);
        if (mediaSet != null) {
            // Clear the reference instead of removing the listener
            // to get around a concurrent modification exception.
            mRootSet = null;

            mSource = new CheckedMediaSetSource(mediaSet);
            if (mListener != null) {
                mListener.onContentDirty();
                mSource.setContentListener(mListener);
                mListener = null;
            }
            mDataManager = null;
            mAlbumPath = null;
        }
    }

    private static class CheckedMediaSetSource implements WidgetSource, ContentListener {
        private static final int CACHE_SIZE = 32;

        @SuppressWarnings("unused")
        private static final String TAG = "CheckedMediaSetSource";

        private MediaSet mSource;
        private MediaItem mCache[] = new MediaItem[CACHE_SIZE];
        private int mCacheStart;
        private int mCacheEnd;
        private long mSourceVersion = MediaObject.INVALID_DATA_VERSION;

        private ContentListener mContentListener;

        public CheckedMediaSetSource(MediaSet source) {
            mSource = Utils.checkNotNull(source);
            mSource.addContentListener(this);
        }

        @Override
        public void close() {
            mSource.removeContentListener(this);
        }

        private void ensureCacheRange(int index) {
            if (index >= mCacheStart && index < mCacheEnd) return;

            long token = Binder.clearCallingIdentity();
            try {
                mCacheStart = index;
                ArrayList<MediaItem> items = mSource.getMediaItem(mCacheStart, CACHE_SIZE);
                mCacheEnd = mCacheStart + items.size();
                items.toArray(mCache);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public synchronized Uri getContentUri(int index) {
            ensureCacheRange(index);
            if (index < mCacheStart || index >= mCacheEnd) return null;
            return mCache[index - mCacheStart].getContentUri();
        }

        @Override
        public synchronized Bitmap getImage(int index) {
            ensureCacheRange(index);
            if (index < mCacheStart || index >= mCacheEnd) return null;
            return WidgetUtils.createWidgetBitmap(mCache[index - mCacheStart]);
        }

        @Override
        public void reload() {
            long version = mSource.reload();
            if (mSourceVersion != version) {
                mSourceVersion = version;
                mCacheStart = 0;
                mCacheEnd = 0;
                Arrays.fill(mCache, null);
            }
        }

        @Override
        public void setContentListener(ContentListener listener) {
            mContentListener = listener;
        }

        @Override
        public int size() {
            long token = Binder.clearCallingIdentity();
            try {
                return mSource.getMediaItemCount();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void onContentDirty() {
            if (mContentListener != null) mContentListener.onContentDirty();
        }
    }

    private static class EmptySource implements WidgetSource {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Bitmap getImage(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uri getContentUri(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setContentListener(ContentListener listener) {}

        @Override
        public void reload() {}

        @Override
        public void close() {}
    }
}
