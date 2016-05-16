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
 * limitations under the License
 */

package android.provider.cts;

import android.content.ContentResolver;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.io.FileNotFoundException;

public class ContactsContract_DumpFileProviderTest extends AndroidTestCase {

    private static final String URI_PREFIX = "content://com.android.contacts.dumpfile/";

    private static final String[] NOT_ALLOWED_FILES = {
            "not_allowed.txt",
            "../A-contacts-db.zip",   // ".." is not allowed.
            "/A-contacts-db.zip",     // "/" is not allowed
            "-contacts-db.zip",       // no name prefix
            "asdf-contacts-db.zip"};

    private static final String[] ALLOWED_FILES = {
            "1234567890abcdefABCDEF-contacts-db.zip",
            "a-contacts-db.zip",
            "0-contacts-db.zip",
            "A-contacts-db.zip",
            "abcdefabcdefabcdefabcdef-contacts-db.zip"};

    private ContentResolver mResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = getContext().getContentResolver();
    }

    public void testOpenFileDescriptor_throwsErrorWithIllegalFileName() {
        for (String fileName : NOT_ALLOWED_FILES) {
            Uri uri = Uri.parse(URI_PREFIX + fileName);
            assertOpenFileDescriptorThrowsError(uri);
        }
    }

    public void testOpenFileDescriptor_worksWithValidFileName() {
        for (String fileName : ALLOWED_FILES) {
            final Uri uri = Uri.parse(URI_PREFIX + fileName);
            try {
                mResolver.openFileDescriptor(uri, "r");
            } catch (FileNotFoundException e) {

            }
        }
    }

    public void testQuery_throwsErrorWithIllegalFileName() {
        for (String fileName : NOT_ALLOWED_FILES) {
            final Uri uri = Uri.parse(URI_PREFIX + fileName);
            assertQueryThrowsError(uri);
        }
    }

    public void testQuery_worksWithValidFileName() {
        for (String fileName : ALLOWED_FILES) {
            final Uri uri = Uri.parse(URI_PREFIX + fileName);
            mResolver.query(uri, null, null, null, null);
        }
    }

    private void assertQueryThrowsError(Uri uri) {
        try {
            mResolver.query(uri, null, null, null, null);
        } catch (IllegalArgumentException e) {
            // pass
            return;
        }

        fail("IllegalArgumentException expected but not thrown.");
    }

    private void assertOpenFileDescriptorThrowsError(Uri uri) {
        try {
            mResolver.openFileDescriptor(uri, "r");
        } catch (IllegalArgumentException e) {
            // pass
            return;
        } catch (FileNotFoundException e) {

        }

        fail("IllegalArgumentException expected but not thrown.");
    }
}
