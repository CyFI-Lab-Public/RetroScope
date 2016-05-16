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
 * limitations under the License
 */

package android.permission2.cts;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StreamItems;
import android.test.AndroidTestCase;

public class ReadSocialStreamPermissionTest extends AndroidTestCase {

    private ContentResolver mResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = mContext.getContentResolver();
    }

    public void testReadSocialStreamPermission_byRawContactId() throws Exception {
        try {
            mResolver.query(Uri.withAppendedPath(
                    ContentUris.withAppendedId(RawContacts.CONTENT_URI, 1337),
                    Contacts.StreamItems.CONTENT_DIRECTORY), null, null, null, null);
            fail("Expected a READ_SOCIAL_STREAM exception");
        } catch (SecurityException e) {
            // Expect a READ_SOCIAL_STREAM exception.
        }
    }

    public void testReadSocialStreamPermission_byContactId() throws Exception {
        try {
            mResolver.query(Uri.withAppendedPath(
                    ContentUris.withAppendedId(Contacts.CONTENT_URI, 1337),
                    Contacts.StreamItems.CONTENT_DIRECTORY), null, null, null, null);
            fail("Expected a READ_SOCIAL_STREAM exception");
        } catch (SecurityException e) {
            // Expect a READ_SOCIAL_STREAM exception.
        }
    }

    public void testReadSocialStreamPermission_byLookUpKey() throws Exception {
        try {
            mResolver.query(Contacts.CONTENT_LOOKUP_URI.buildUpon()
                    .appendPath("lookDownKey")
                    .appendPath(Contacts.StreamItems.CONTENT_DIRECTORY)
                    .build(), null, null, null, null);
            fail("Expected a READ_SOCIAL_STREAM exception");
        } catch (SecurityException e) {
            // Expect a READ_SOCIAL_STREAM exception.
        }
    }

    public void testReadSocialStreamPermission_byStreamItemId() throws Exception {
        try {
            mResolver.query(ContentUris.withAppendedId(StreamItems.CONTENT_URI, 1337),
                    null, null, null, null);
            fail("Expected a READ_SOCIAL_STREAM exception");
        } catch (SecurityException e) {
            // Expect a READ_SOCIAL_STREAM exception.
        }
    }
}
