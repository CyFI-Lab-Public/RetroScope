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

public class SecureSource extends MediaSource {
    private GalleryApp mApplication;
    private static PathMatcher mMatcher = new PathMatcher();
    private static final int SECURE_ALBUM = 0;
    private static final int SECURE_UNLOCK = 1;

    static {
        mMatcher.add("/secure/all/*", SECURE_ALBUM);
        mMatcher.add("/secure/unlock", SECURE_UNLOCK);
    }

    public SecureSource(GalleryApp context) {
        super("secure");
        mApplication = context;
    }

    public static boolean isSecurePath(String path) {
        return (SECURE_ALBUM == mMatcher.match(Path.fromString(path)));
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        switch (mMatcher.match(path)) {
            case SECURE_ALBUM: {
                DataManager dataManager = mApplication.getDataManager();
                MediaItem unlock = (MediaItem) dataManager.getMediaObject(
                        "/secure/unlock");
                return new SecureAlbum(path, mApplication, unlock);
            }
            case SECURE_UNLOCK:
                return new UnlockImage(path, mApplication);
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }
}
