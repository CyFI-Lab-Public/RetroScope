/*
 * Copyright (C) 2010 Google Inc.
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
 * limitations under the License
 */

package com.android.loaderapp.model;

import android.content.Context;
import android.content.CursorLoader;
import android.provider.ContactsContract.Groups;

/**
 * Loads the list of all contact groups.
 */
public class GroupsListLoader extends CursorLoader {
    public static final String[] COLUMNS = new String[] {
        Groups._ID,        // 0
        Groups.TITLE,      // 1
    };

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_TITLE = 1;

    public GroupsListLoader(Context context) {
        super(context, Groups.CONTENT_URI, COLUMNS, null, null, Groups.TITLE);
    }
}
