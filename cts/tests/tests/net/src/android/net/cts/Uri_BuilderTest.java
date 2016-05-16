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

package android.net.cts;

import junit.framework.TestCase;
import android.net.Uri.Builder;
import android.net.Uri;

public class Uri_BuilderTest extends TestCase {
    public void testBuilderOperations() {
        Uri uri = Uri.parse("http://google.com/p1?query#fragment");
        Builder builder = uri.buildUpon();
        uri = builder.appendPath("p2").build();
        assertEquals("http", uri.getScheme());
        assertEquals("google.com", uri.getAuthority());
        assertEquals("/p1/p2", uri.getPath());
        assertEquals("query", uri.getQuery());
        assertEquals("fragment", uri.getFragment());
        assertEquals(uri.toString(), builder.toString());

        uri = Uri.parse("mailto:nobody");
        builder = uri.buildUpon();
        uri = builder.build();
        assertEquals("mailto", uri.getScheme());
        assertEquals("nobody", uri.getSchemeSpecificPart());
        assertEquals(uri.toString(), builder.toString());

        uri = new Uri.Builder()
                .scheme("http")
                .encodedAuthority("google.com")
                .encodedPath("/p1")
                .appendEncodedPath("p2")
                .encodedQuery("query")
                .appendQueryParameter("query2", null)
                .encodedFragment("fragment")
                .build();
        assertEquals("http", uri.getScheme());
        assertEquals("google.com", uri.getEncodedAuthority());
        assertEquals("/p1/p2", uri.getEncodedPath());
        assertEquals("query&query2=null", uri.getEncodedQuery());
        assertEquals("fragment", uri.getEncodedFragment());

        uri = new Uri.Builder()
                .scheme("mailto")
                .encodedOpaquePart("nobody")
                .build();
        assertEquals("mailto", uri.getScheme());
        assertEquals("nobody", uri.getEncodedSchemeSpecificPart());
    }
}
