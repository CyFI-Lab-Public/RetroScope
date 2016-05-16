/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
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
import android.net.Uri;
import android.provider.ContactsContract.Contacts;

/**
 * Helper for loading contact lists.
 */
public class ContactsListLoader {
    public static final String[] COLUMNS = new String[] {
        Contacts._ID,                       // 0
        Contacts.DISPLAY_NAME_PRIMARY,      // 1
        Contacts.DISPLAY_NAME_ALTERNATIVE,  // 2
        Contacts.SORT_KEY_PRIMARY,          // 3
        Contacts.STARRED,                   // 4
        Contacts.TIMES_CONTACTED,           // 5
        Contacts.CONTACT_PRESENCE,          // 6
        Contacts.PHOTO_ID,                  // 7
        Contacts.LOOKUP_KEY,                // 8
        Contacts.PHONETIC_NAME,             // 9
        Contacts.HAS_PHONE_NUMBER,          // 10
    };

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_LOOKUP_KEY = 8;

    public static CursorLoader newVisibleContactsLoader(Context context) {
        return new CursorLoader(context, Contacts.CONTENT_URI, COLUMNS, 
                Contacts.IN_VISIBLE_GROUP + "=1", null, Contacts.SORT_KEY_PRIMARY);
    }

    public static CursorLoader newStrequentContactsLoader(Context context) {
        return new CursorLoader(context, Contacts.CONTENT_STREQUENT_URI, COLUMNS, null, null, null);
    }

    public static CursorLoader newContactGroupLoader(Context context, String groupTitle) {
        Uri uri = Uri.withAppendedPath(Contacts.CONTENT_GROUP_URI, groupTitle);
        return new CursorLoader(context, uri, COLUMNS, null, null, Contacts.SORT_KEY_PRIMARY);
    }
}
