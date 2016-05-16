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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Picks a random image from the local store.
 */
public class StockSource extends PhotoSource {
    public static final String ALBUM_ID = "com.android.dreams.phototable.StockSource";
    private static final String TAG = "PhotoTable.StockSource";
    private static final int[] PHOTOS = { R.drawable.blank_photo };

    private final ArrayList<ImageData> mImageCache;
    private final ArrayList<AlbumData> mAlbumCache;

    private final ArrayList<ImageData> mImageList;
    private final ArrayList<AlbumData> mAlbumList;

    private final String mStockPhotoName;

    public StockSource(Context context, SharedPreferences settings) {
        super(context, settings, null);
        mSourceName = TAG;
        mStockPhotoName = mResources.getString(R.string.stock_photo_album_name, "Default Photos");
        mImageCache = new ArrayList<ImageData>(PHOTOS.length);
        mAlbumCache = new ArrayList<AlbumData>(1);
        mImageList = new ArrayList<ImageData>(PHOTOS.length);
        mAlbumList = new ArrayList<AlbumData>(1);

        AlbumData albumData = new AlbumData();
        albumData.id = ALBUM_ID;
        albumData.account = mStockPhotoName;
        albumData.title = mStockPhotoName;
        mAlbumCache.add(albumData);

        for (int i = 0; i < PHOTOS.length; i++) {
            ImageData imageData = new ImageData();
            imageData.id = Integer.toString(i);
            mImageCache.add(imageData);
        }

        fillQueue();
    }

    @Override
    public Collection<AlbumData> findAlbums() {
        if (mAlbumList.isEmpty()) {
            mAlbumList.addAll(mAlbumCache);
        }
        log(TAG, "returning a list of albums: " + mAlbumList.size());
        return mAlbumList;
    }

    @Override
    protected Collection<ImageData> findImages(int howMany) {
        if (mImageList.isEmpty()) {
            mImageList.addAll(mImageCache);
        }
        return mImageList;
    }

    @Override
    protected InputStream getStream(ImageData data, int longSide) {
        InputStream is = null;
        try {
            log(TAG, "opening:" + data.id);
            int idx = Integer.valueOf(data.id);
            is = mResources.openRawResource(PHOTOS[idx]);
        } catch (Exception ex) {
            log(TAG, ex.toString());
            is = null;
        }

        return is;
    }

    @Override
    public ImageData naturalNext(ImageData current) {
        int idx = Integer.valueOf(current.id);
        idx = (idx + 1) % PHOTOS.length;
        return mImageCache.get(idx);
    }

    @Override
    public ImageData naturalPrevious(ImageData current) {
        int idx = Integer.valueOf(current.id);
        idx = (PHOTOS.length + idx - 1) % PHOTOS.length;
        return mImageCache.get(idx);
    }

    @Override
    protected void donePaging(ImageData current) {
    }
}

