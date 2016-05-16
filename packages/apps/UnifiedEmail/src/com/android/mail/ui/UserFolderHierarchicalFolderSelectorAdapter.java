/*******************************************************************************
 *      Copyright (C) 2013 Google Inc.
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

import android.content.Context;
import android.database.Cursor;
import com.android.mail.providers.Folder;

public class UserFolderHierarchicalFolderSelectorAdapter extends HierarchicalFolderSelectorAdapter {
    public UserFolderHierarchicalFolderSelectorAdapter(Context context, Cursor folders, int layout,
                                                       String header, Folder excludedFolder) {
        super(context, folders, layout, header, excludedFolder);
    }

    /**
     * Return whether the supplied folder meets the requirements to be displayed
     * in the folder list.
     */
    @Override
    protected boolean meetsRequirements(Folder folder) {
        if (folder.isProviderFolder()) {
            return false;
        }
        return super.meetsRequirements(folder);
    }
}
