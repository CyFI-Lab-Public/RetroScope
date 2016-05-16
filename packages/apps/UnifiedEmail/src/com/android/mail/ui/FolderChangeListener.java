/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/
package com.android.mail.ui;

import com.android.mail.providers.Folder;

/**
 * The callback interface for when a list item has been selected.
 */
public interface FolderChangeListener {
    /**
     * Handles selecting a folder from within the {@link FolderListFragment}.
     *
     * @param folder the selected folder
     * @param force <code>true</code> to force a folder change, <code>false</code> to disallow
     *          changing to the current folder
     */
    void onFolderChanged(Folder folder, boolean force);
}
