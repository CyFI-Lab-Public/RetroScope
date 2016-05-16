/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.providers;

import android.database.DataSetObserver;

import com.android.mail.ui.RecentFolderController;
import com.android.mail.ui.RecentFolderList;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

/**
 * A simple extension of {@link DataSetObserver} to provide the updated recent folders
 * {@link #onChanged()} when they change. Initializing the object registers with
 * the observer with the {@link RecentFolderObserver} provided. The object will then begin to
 * receive {@link #onChanged()} till {@link #unregisterAndDestroy()} is called.
 * The {@link RecentFolderList} returned in {@link #initialize(RecentFolderController)} is updated
 * directly, no new object is created when the recent folder list is updated.
 * <p>
 * To implement an {@link RecentFolderObserver}, you need to implement the
 * {@link #onChanged()} method.
 */
public abstract class RecentFolderObserver extends DataSetObserver {
    /**
     * The RecentFolderController that the observer is registered with.
     */
    private RecentFolderController mController;

    private static final String LOG_TAG = LogTag.getLogTag();

    /**
     * The no-argument constructor leaves the object unusable till
     * {@link #initialize(RecentFolderController)} is called.
     */
    public RecentFolderObserver () {
    }

    /**
     * Initializes an {@link RecentFolderObserver} object that receives a call to
     * {@link #onChanged()} when the controller changes the recent folder.
     *
     * @param controller
     */
    public RecentFolderList initialize(RecentFolderController controller) {
        if (controller == null) {
            LogUtils.wtf(LOG_TAG, "RecentFolderObserver initialized with null controller!");
        }
        mController = controller;
        mController.registerRecentFolderObserver(this);
        return mController.getRecentFolders();
    }

    @Override
    public abstract void onChanged();

    /**
     * Return the most current recent folder.
     * @return
     */
    public final RecentFolderList getRecentFolders() {
        if (mController == null) {
            return null;
        }
        return mController.getRecentFolders();
    }

    /**
     * Unregisters for recent folder changes and makes the object unusable.
     */
    public void unregisterAndDestroy() {
        if (mController == null) {
            return;
        }
        mController.unregisterRecentFolderObserver(this);
    }
}