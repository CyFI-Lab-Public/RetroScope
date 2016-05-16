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

import java.util.concurrent.atomic.AtomicBoolean;

// This is a simple MediaSet which contains only one MediaItem -- a SnailItem.
public class SnailAlbum extends SingleItemAlbum {
    @SuppressWarnings("unused")
    private static final String TAG = "SnailAlbum";
    private AtomicBoolean mDirty = new AtomicBoolean(false);

    public SnailAlbum(Path path, SnailItem item) {
        super(path, item);
    }

    @Override
    public long reload() {
        if (mDirty.compareAndSet(true, false)) {
            ((SnailItem) getItem()).updateVersion();
            mDataVersion = nextVersionNumber();
        }
        return mDataVersion;
    }

    public void notifyChange() {
        mDirty.set(true);
        notifyContentChanged();
    }
}
