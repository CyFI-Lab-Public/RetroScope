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
import java.util.Collection;
import java.util.LinkedList;

/**
 * Loads images from a variety of sources.
 */
public class PhotoSourcePlexor extends PhotoSource {
    private static final String TAG = "PhotoTable.PhotoSourcePlexor";

    private final PhotoSource mPicasaSource;
    private final PhotoSource mLocalSource;

    public PhotoSourcePlexor(Context context, SharedPreferences settings) {
        super(context, settings);
        mSourceName = TAG;
        mPicasaSource = new PicasaSource(context, settings);
        mLocalSource = new LocalSource(context, settings);
    }

    @Override
    public Collection<AlbumData> findAlbums() {
        log(TAG, "finding albums");
        LinkedList<AlbumData> foundAlbums = new LinkedList<AlbumData>();

        foundAlbums.addAll(mPicasaSource.findAlbums());
        log(TAG, "found " + foundAlbums.size() + " network albums");

        foundAlbums.addAll(mLocalSource.findAlbums());
        log(TAG, "found " + foundAlbums.size() + " user albums");

        return foundAlbums;
    }

    @Override
    protected Collection<ImageData> findImages(int howMany) {
        log(TAG, "finding images");
        LinkedList<ImageData> foundImages = new LinkedList<ImageData>();

        foundImages.addAll(mPicasaSource.findImages(howMany));
        log(TAG, "found " + foundImages.size() + " network images");

        foundImages.addAll(mLocalSource.findImages(howMany));
        log(TAG, "found " + foundImages.size() + " user images");

        return foundImages;
    }

    @Override
    protected InputStream getStream(ImageData data, int longSide) {
        return data.getStream(longSide);
    }

    @Override
    protected ImageData naturalNext(ImageData current) {
        return current.naturalNext();
    }

    @Override
    protected ImageData naturalPrevious(ImageData current) {
        return current.naturalPrevious();
    }

    @Override
    protected void donePaging(ImageData current) {
        current.donePaging();
    }
}
