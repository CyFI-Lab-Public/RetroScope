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

import android.net.Uri;

import com.android.gallery3d.data.MediaSet.ItemConsumer;

import java.util.ArrayList;

public abstract class MediaSource {
    private static final String TAG = "MediaSource";
    private String mPrefix;

    protected MediaSource(String prefix) {
        mPrefix = prefix;
    }

    public String getPrefix() {
        return mPrefix;
    }

    public Path findPathByUri(Uri uri, String type) {
        return null;
    }

    public abstract MediaObject createMediaObject(Path path);

    public void pause() {
    }

    public void resume() {
    }

    public Path getDefaultSetOf(Path item) {
        return null;
    }

    public long getTotalUsedCacheSize() {
        return 0;
    }

    public long getTotalTargetCacheSize() {
        return 0;
    }

    public static class PathId {
        public PathId(Path path, int id) {
            this.path = path;
            this.id = id;
        }
        public Path path;
        public int id;
    }

    // Maps a list of Paths (all belong to this MediaSource) to MediaItems,
    // and invoke consumer.consume() for each MediaItem with the given id.
    //
    // This default implementation uses getMediaObject for each Path. Subclasses
    // may override this and provide more efficient implementation (like
    // batching the database query).
    public void mapMediaItems(ArrayList<PathId> list, ItemConsumer consumer) {
        int n = list.size();
        for (int i = 0; i < n; i++) {
            PathId pid = list.get(i);
            MediaObject obj;
            synchronized (DataManager.LOCK) {
                obj = pid.path.getObject();
                if (obj == null) {
                    try {
                        obj = createMediaObject(pid.path);
                    } catch (Throwable th) {
                        Log.w(TAG, "cannot create media object: " + pid.path, th);
                    }
                }
            }
            if (obj != null) {
                consumer.consume(pid.id, (MediaItem) obj);
            }
        }
    }
}
