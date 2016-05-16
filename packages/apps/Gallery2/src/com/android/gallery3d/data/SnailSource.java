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

import com.android.gallery3d.app.GalleryApp;

public class SnailSource extends MediaSource {
    @SuppressWarnings("unused")
    private static final String TAG = "SnailSource";
    private static final int SNAIL_ALBUM = 0;
    private static final int SNAIL_ITEM = 1;

    private GalleryApp mApplication;
    private PathMatcher mMatcher;
    private static int sNextId;

    public SnailSource(GalleryApp application) {
        super("snail");
        mApplication = application;
        mMatcher = new PathMatcher();
        mMatcher.add("/snail/set/*", SNAIL_ALBUM);
        mMatcher.add("/snail/item/*", SNAIL_ITEM);
    }

    // The only path we accept is "/snail/set/id" and "/snail/item/id"
    @Override
    public MediaObject createMediaObject(Path path) {
        DataManager dataManager = mApplication.getDataManager();
        switch (mMatcher.match(path)) {
            case SNAIL_ALBUM:
                String itemPath = "/snail/item/" + mMatcher.getVar(0);
                SnailItem item =
                        (SnailItem) dataManager.getMediaObject(itemPath);
                return new SnailAlbum(path, item);
            case SNAIL_ITEM: {
                int id = mMatcher.getIntVar(0);
                return new SnailItem(path);
            }
        }
        return null;
    }

    // Registers a new SnailAlbum containing a SnailItem and returns the id of
    // them. You can obtain the Path of the SnailAlbum and SnailItem associated
    // with the id by getSetPath and getItemPath().
    public static synchronized int newId() {
        return sNextId++;
    }

    public static Path getSetPath(int id) {
        return Path.fromString("/snail/set").getChild(id);
    }

    public static Path getItemPath(int id) {
        return Path.fromString("/snail/item").getChild(id);
    }
}
