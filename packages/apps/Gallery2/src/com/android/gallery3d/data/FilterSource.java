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

import com.android.gallery3d.app.GalleryApp;

public class FilterSource extends MediaSource {
    @SuppressWarnings("unused")
    private static final String TAG = "FilterSource";
    private static final int FILTER_BY_MEDIATYPE = 0;
    private static final int FILTER_BY_DELETE = 1;
    private static final int FILTER_BY_EMPTY = 2;
    private static final int FILTER_BY_EMPTY_ITEM = 3;
    private static final int FILTER_BY_CAMERA_SHORTCUT = 4;
    private static final int FILTER_BY_CAMERA_SHORTCUT_ITEM = 5;

    public static final String FILTER_EMPTY_ITEM = "/filter/empty_prompt";
    public static final String FILTER_CAMERA_SHORTCUT = "/filter/camera_shortcut";
    private static final String FILTER_CAMERA_SHORTCUT_ITEM = "/filter/camera_shortcut_item";

    private GalleryApp mApplication;
    private PathMatcher mMatcher;
    private MediaItem mEmptyItem;
    private MediaItem mCameraShortcutItem;

    public FilterSource(GalleryApp application) {
        super("filter");
        mApplication = application;
        mMatcher = new PathMatcher();
        mMatcher.add("/filter/mediatype/*/*", FILTER_BY_MEDIATYPE);
        mMatcher.add("/filter/delete/*", FILTER_BY_DELETE);
        mMatcher.add("/filter/empty/*", FILTER_BY_EMPTY);
        mMatcher.add(FILTER_EMPTY_ITEM, FILTER_BY_EMPTY_ITEM);
        mMatcher.add(FILTER_CAMERA_SHORTCUT, FILTER_BY_CAMERA_SHORTCUT);
        mMatcher.add(FILTER_CAMERA_SHORTCUT_ITEM, FILTER_BY_CAMERA_SHORTCUT_ITEM);

        mEmptyItem = new EmptyAlbumImage(Path.fromString(FILTER_EMPTY_ITEM),
                mApplication);
        mCameraShortcutItem = new CameraShortcutImage(
                Path.fromString(FILTER_CAMERA_SHORTCUT_ITEM), mApplication);
    }

    // The name we accept are:
    // /filter/mediatype/k/{set}    where k is the media type we want.
    // /filter/delete/{set}
    @Override
    public MediaObject createMediaObject(Path path) {
        int matchType = mMatcher.match(path);
        DataManager dataManager = mApplication.getDataManager();
        switch (matchType) {
            case FILTER_BY_MEDIATYPE: {
                int mediaType = mMatcher.getIntVar(0);
                String setsName = mMatcher.getVar(1);
                MediaSet[] sets = dataManager.getMediaSetsFromString(setsName);
                return new FilterTypeSet(path, dataManager, sets[0], mediaType);
            }
            case FILTER_BY_DELETE: {
                String setsName = mMatcher.getVar(0);
                MediaSet[] sets = dataManager.getMediaSetsFromString(setsName);
                return new FilterDeleteSet(path, sets[0]);
            }
            case FILTER_BY_EMPTY: {
                String setsName = mMatcher.getVar(0);
                MediaSet[] sets = dataManager.getMediaSetsFromString(setsName);
                return new FilterEmptyPromptSet(path, sets[0], mEmptyItem);
            }
            case FILTER_BY_EMPTY_ITEM: {
                return mEmptyItem;
            }
            case FILTER_BY_CAMERA_SHORTCUT: {
                return new SingleItemAlbum(path, mCameraShortcutItem);
            }
            case FILTER_BY_CAMERA_SHORTCUT_ITEM: {
                return mCameraShortcutItem;
            }
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }
}
