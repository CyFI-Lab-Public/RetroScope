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

import android.content.UriMatcher;
import android.net.Uri;
import android.test.AndroidTestCase;

public class UriMatcherTest extends AndroidTestCase {
    UriMatcher mUriMatcher;

    private static final String sAuthority = "ctstest";
    private static final String sPath1 = "testPath1";
    private static final String sPath2 = "testPath2";
    private static final String sPath3 = "testPath3";
    private static final String sPath4 = "testPath4";

    private static final int sCode1 = 1;
    private static final int sCode2 = 2;
    private static final int sCode3 = 3;
    private static final int sCode4 = 4;

    private Uri uri1 = Uri.parse("content://" + sAuthority + "/" + sPath1);
    private Uri uri2 = Uri.parse("content://" + sAuthority + "/" + sPath2);
    private Uri uri3 = Uri.parse("content://" + sAuthority + "/" + sPath3);
    private Uri uri4 = Uri.parse("content://" + sAuthority + "/" + sPath4);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    }

    public void testConstructor() {
        new UriMatcher(UriMatcher.NO_MATCH);
    }

    public void testMatch() {
        mUriMatcher.addURI(sAuthority, sPath1, sCode1);
        mUriMatcher.addURI(sAuthority, sPath2, sCode2);
        mUriMatcher.addURI(sAuthority, sPath3, sCode3);
        mUriMatcher.addURI(sAuthority, sPath4, sCode4);

        assertEquals(sCode1, mUriMatcher.match(uri1));
        assertEquals(sCode2, mUriMatcher.match(uri2));
        assertEquals(sCode3, mUriMatcher.match(uri3));
        assertEquals(sCode4, mUriMatcher.match(uri4));

        //test unknown uri
        Uri unknown = Uri.parse("abc");
        assertEquals(-1, mUriMatcher.match(unknown));
    }

    public void testMatchFailure() {
        try {
            mUriMatcher.match(null);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            //expected, test success.
        }
    }

    public void testAddURI() {
        assertEquals(-1, mUriMatcher.match(uri1));
        assertEquals(-1, mUriMatcher.match(uri2));
        assertEquals(-1, mUriMatcher.match(uri3));
        assertEquals(-1, mUriMatcher.match(uri4));

        mUriMatcher.addURI(sAuthority, sPath1, sCode1);
        mUriMatcher.addURI(sAuthority, sPath2, sCode2);
        mUriMatcher.addURI(sAuthority, sPath3, sCode3);
        mUriMatcher.addURI(sAuthority, sPath4, sCode4);

        assertEquals(sCode1, mUriMatcher.match(uri1));
        assertEquals(sCode2, mUriMatcher.match(uri2));
        assertEquals(sCode3, mUriMatcher.match(uri3));
        assertEquals(sCode4, mUriMatcher.match(uri4));
    }

    public void testAddURIFailure() {
        try {
            mUriMatcher.addURI(null, null, -1);
            fail("There should be an IllegalArgumentException thrown out.");
        } catch (IllegalArgumentException e) {
            //expected, test success.
        }
    }
}
