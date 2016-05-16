/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PhotoDreamSettingsReceiver extends BroadcastReceiver {
    private static final String TAG = "PhotoDreamSettingsReceiver";
    private static final String LOCAL_AUTHORITY = "media";
    private static final String INTERNAL = "internal";
    private static final boolean DEBUG = false;

    public static final String ACTION_ADD_ALBUM = "add";
    public static final String ACTION_REMOVE_ALBUM = "remove";
    public static final String EXTRA_ALBUMS = "albums";

    @Override
    public void onReceive(Context context, Intent intent) {
        AlbumSettings settings[] = {
                AlbumSettings.getAlbumSettings(
                        context.getSharedPreferences(FlipperDreamSettings.PREFS_NAME, 0)),
                AlbumSettings.getAlbumSettings(
                        context.getSharedPreferences(PhotoTableDreamSettings.PREFS_NAME, 0))
        };

        boolean shown = ACTION_ADD_ALBUM.equals(intent.getAction());
        ArrayList<String> albumUris = intent.getStringArrayListExtra(EXTRA_ALBUMS);
        for (String albumUriString: albumUris) {
            Uri albumUri = Uri.parse(albumUriString);
            String type = albumUri.getEncodedAuthority();
            List<String> path = albumUri.getPathSegments();

            String albumId = null;
            if (LOCAL_AUTHORITY.equals(type)) {
                if (path.size() > 3) {
                    albumId = LocalSource.constructId(INTERNAL.equals(path.get(0)), path.get(3));
                }
            } else {
                if (path.size() > 1) {
                    albumId = PicasaSource.constructId(path.get(1));
                }
            }
            if (DEBUG) Log.d(TAG, "receive: " + albumId + " is " + shown);
            for (int idx = 0; idx < settings.length; idx++) {
                settings[idx].setAlbumEnabled(albumId, shown);
            }
        }
    }
}
