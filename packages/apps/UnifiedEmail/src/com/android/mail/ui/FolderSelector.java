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


package com.android.mail.ui;

import com.android.mail.providers.Folder;

/**
 * Interface that permits elements to implement selecting a folder.
 * The single method {@link #onFolderSelected(com.android.mail.providers.Folder)} defines what
 * happens when a folder is selected.
 */
public interface FolderSelector {
    /**
     * Selects the folder provided as an argument here.  This corresponds to the user
     * selecting a folder in the UI element, either for creating a widget/shortcut (as in the
     * case of {@link FolderSelectionActivity} or for viewing the contents of
     * the folder (as in the case of {@link AbstractActivityController}.
     * @param folder
     */
    public void onFolderSelected(Folder folder);
}
