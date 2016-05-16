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

package com.android.gallery3d.picasasource;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.ParcelFileDescriptor;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.MediaSource;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.PathMatcher;

import java.io.FileNotFoundException;

public class PicasaSource extends MediaSource {
    private static final String TAG = "PicasaSource";

    private static final int NO_MATCH = -1;
    private static final int IMAGE_MEDIA_ID = 1;

    private static final int PICASA_ALBUMSET = 0;
    private static final int MAP_BATCH_COUNT = 100;

    private GalleryApp mApplication;
    private PathMatcher mMatcher;

    public static final Path ALBUM_PATH = Path.fromString("/picasa/all");

    public PicasaSource(GalleryApp application) {
        super("picasa");
        mApplication = application;
        mMatcher = new PathMatcher();
        mMatcher.add("/picasa/all", PICASA_ALBUMSET);
        mMatcher.add("/picasa/image", PICASA_ALBUMSET);
        mMatcher.add("/picasa/video", PICASA_ALBUMSET);
    }

    private static class EmptyAlbumSet extends MediaSet {

        public EmptyAlbumSet(Path path, long version) {
            super(path, version);
        }

        @Override
        public String getName() {
            return "picasa";
        }

        @Override
        public long reload() {
            return mDataVersion;
        }
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        switch (mMatcher.match(path)) {
            case PICASA_ALBUMSET:
                return new EmptyAlbumSet(path, MediaObject.nextVersionNumber());
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }

    public static MediaItem getFaceItem(Context context, MediaItem item, int faceIndex) {
        throw new UnsupportedOperationException();
    }

    public static boolean isPicasaImage(MediaObject object) {
        return false;
    }

    public static String getImageTitle(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static int getImageSize(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static String getContentType(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static long getDateTaken(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static double getLatitude(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static double getLongitude(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static int getRotation(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static long getPicasaId(MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static String getUserAccount(Context context, MediaObject image) {
        throw new UnsupportedOperationException();
    }

    public static ParcelFileDescriptor openFile(Context context, MediaObject image, String mode)
            throws FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    public static void initialize(Context context) {/*do nothing*/}

    public static void requestSync(Context context) {/*do nothing*/}

    public static void showSignInReminder(Activity context) {/*do nothing*/}

    public static void onPackageAdded(Context context, String packageName) {/*do nothing*/}

    public static void onPackageRemoved(Context context, String packageName) {/*do nothing*/}

    public static void onPackageChanged(Context context, String packageName) {/*do nothing*/}

    public static Dialog getVersionCheckDialog(Activity activity){
        return null;
    }
}
