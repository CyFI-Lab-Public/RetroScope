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

import android.content.SharedPreferences;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Common utilities for album settings.
 */
public class AlbumSettings {
    public static final String ALBUM_SET = "Enabled Album Set V2";

    private static HashMap<SharedPreferences, AlbumSettings> singletons;

    private final SharedPreferences mSettings;
    private final HashSet<String> mEnabledAlbums;

    public static AlbumSettings getAlbumSettings(SharedPreferences settings) {
        if (singletons == null) {
            singletons = new HashMap<SharedPreferences, AlbumSettings>();
        }
        if (!singletons.containsKey(settings)) {
            singletons.put(settings, new AlbumSettings(settings));
        }
        return singletons.get(settings);
    }

    public void readEnabledAlbums() {
        synchronized (mEnabledAlbums) {
            readEnabledAlbumsLocked();
        }
    }

    public boolean isAlbumEnabled(String albumId) {
        synchronized (mEnabledAlbums) {
            return mEnabledAlbums.contains(albumId);
        }
    }

    public boolean areAllEnabled(Collection<String> validAlbums) {
        synchronized (mEnabledAlbums) {
            return mEnabledAlbums.containsAll(validAlbums);
        }
    }

    public void setAlbumEnabled(String albumId, boolean enabled) {
        if (isAlbumEnabled(albumId) != enabled) {
            synchronized (mEnabledAlbums) {
                readEnabledAlbumsLocked();
                if (enabled) {
                    mEnabledAlbums.add(albumId);
                } else {
                    mEnabledAlbums.remove(albumId);
                }
                writeEnabledAlbumsLocked();
            }
        }
    }

    public void disableAllAlbums() {
        synchronized (mEnabledAlbums) {
            mEnabledAlbums.clear();
            writeEnabledAlbumsLocked();
        }
    }

    public void enableAllAlbums(Collection<String> validAlbums) {
        synchronized (mEnabledAlbums) {
            mEnabledAlbums.clear();
            mEnabledAlbums.addAll(validAlbums);
            writeEnabledAlbumsLocked();
        }
    }

    public void pruneObsoleteSettings(Collection<String> validAlbums) {
        if (!validAlbums.containsAll(mEnabledAlbums)) {
            synchronized (mEnabledAlbums) {
                readEnabledAlbumsLocked();
                mEnabledAlbums.retainAll(validAlbums);
                writeEnabledAlbumsLocked();
            }
        }
    }

    public  boolean isConfigured() {
        synchronized (mEnabledAlbums) {
            return mEnabledAlbums.size() != 0;
        }
    }

    private AlbumSettings(SharedPreferences settings) {
        mSettings = settings;
        mEnabledAlbums = new HashSet<String>();
        readEnabledAlbums();
    }

    private void readEnabledAlbumsLocked() {
        Set<String> enabledAlbums = mSettings.getStringSet(ALBUM_SET, null);
        mEnabledAlbums.clear();
        if (enabledAlbums != null) {
            mEnabledAlbums.addAll(enabledAlbums);
        }
    }

    private void writeEnabledAlbumsLocked() {
        SharedPreferences.Editor editor = mSettings.edit();
        // Give SharedSettings a copy, so that we are free to manipulate ours.
        editor.putStringSet(ALBUM_SET, new HashSet<String>(mEnabledAlbums));
        editor.commit();
    }
}
