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

package com.android.mail.ui;

import android.database.DataSetObserver;

import com.android.mail.providers.RecentFolderObserver;

/**
 * Controller that can provide recent folders and notify on changes to recent folders.
 */
public interface RecentFolderController {
    /**
     * Registers to receive changes to the recent folders, and obtains the current recent folders.
     */
    void registerRecentFolderObserver(DataSetObserver observer);

    /**
     * Removes a listener from receiving recent folder changes.
     */
    void unregisterRecentFolderObserver(DataSetObserver observer);

    /**
     * Returns the current recent folders. Instead of calling this method,
     * consider registering for account changes using
     * {@link RecentFolderObserver#initialize(RecentFolderController)}, which not only provides the
     * current account, but also updates to the account, in case of settings changes.
     */
    RecentFolderList getRecentFolders();

}
