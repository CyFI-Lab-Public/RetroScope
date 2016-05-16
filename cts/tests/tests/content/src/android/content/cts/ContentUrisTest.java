/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.content.cts;

import android.content.ContentUris;
import android.net.Uri;
import android.net.Uri.Builder;
import android.test.AndroidTestCase;

public class ContentUrisTest extends AndroidTestCase {
    private static final String AUTHORITY = "ctstest";
    private static final String PATH1 = "testPath1";
    private static final String PATH2 = "testPath2";

    private static final int CODE1 = 1;
    private static final int CODE2 = 2;

    private Uri uri1 = Uri.parse("content://" + AUTHORITY + "/" + PATH1);
    private Uri uri2 = Uri.parse("content://" + AUTHORITY + "/" + PATH2);

    public void testConstructor() {
        new ContentUris();
    }

    public void testParseId() {
        Uri result = ContentUris.withAppendedId(uri1, CODE1);
        assertEquals(CODE1, ContentUris.parseId(result));

        result = ContentUris.withAppendedId(uri2, CODE2);
        assertEquals(CODE2, ContentUris.parseId(result));

        // no code in Uri
        assertEquals(-1, ContentUris.parseId(Uri.parse("")));
    }

    public void testParseIdFailure() {
        try {
            ContentUris.parseId(uri1);
            fail("There should be a NumberFormatException thrown out.");
        } catch (NumberFormatException e) {
            //expected, test success.
        }

        Uri uri = Uri.fromParts("abc", "123", null);
        ContentUris.parseId(uri);

        try {
            ContentUris.parseId(null);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testWithAppendedId() {
        String expected = "content://" + AUTHORITY + "/" + PATH1 + "/" + CODE1;
        Uri actually;

        assertNotNull(actually = ContentUris.withAppendedId(uri1, CODE1));
        assertEquals(expected, actually.toString());

        expected = "content://" + AUTHORITY + "/" + PATH2 + "/" + CODE2;
        assertNotNull(actually = ContentUris.withAppendedId(uri2, CODE2));
        assertEquals(expected, actually.toString());
    }

    public void testWithAppendedIdFailure() {
        try {
            ContentUris.withAppendedId(null, -1);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testAppendId() {
        String expected = "content://" + AUTHORITY + "/" + PATH1 + "/" + CODE1;
        Builder actually;
        Builder b = uri1.buildUpon();

        assertNotNull(actually = ContentUris.appendId(b, CODE1));
        assertEquals(expected, actually.toString());

        expected = "content://" + AUTHORITY + "/" + PATH2 + "/" + CODE2;
        b = uri2.buildUpon();

        assertNotNull(actually = ContentUris.appendId(b, CODE2));
        assertEquals(expected, actually.toString());
    }

    public void testAppendIdFailure() {
        try {
            ContentUris.appendId(null, -1);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }
}
