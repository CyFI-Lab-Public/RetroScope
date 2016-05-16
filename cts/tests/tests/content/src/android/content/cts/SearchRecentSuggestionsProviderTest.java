/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.IsolatedContext;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

public class SearchRecentSuggestionsProviderTest extends AndroidTestCase {
    private final static String AUTHORITY_HEAD = "content://" + MockSRSProvider.AUTHORITY;
    private final static Uri TEST_URI = Uri.parse(AUTHORITY_HEAD  + "/suggestions");

    private IsolatedContext mProviderContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        final String filenamePrefix = "test.";
        final RenamingDelegatingContext targetContextWrapper = new RenamingDelegatingContext(
                new MockContext(), getContext(), filenamePrefix);
        mProviderContext = new IsolatedContext(new MockContentResolver(), targetContextWrapper);
    }

    public void testSearchRecentSuggestionsProvider() {
        assertFalse(MockSRSProvider.setupSuggestCalled);
        final MockSRSProvider s = new MockSRSProvider();
        assertTrue(MockSRSProvider.setupSuggestCalled);

        assertFalse(s.isOnCreateCalled());
        s.attachInfo(mProviderContext, null);
        assertTrue(s.isOnCreateCalled());

        assertNotNull(s.getType(TEST_URI));

        final String uriStr = AUTHORITY_HEAD + '/' + SearchManager.SUGGEST_URI_PATH_QUERY;
        final Uri contentUri = Uri.parse(uriStr);
        String[] selArgs = new String[] { null };

        Cursor c = s.query(contentUri, null, null, selArgs, null);
        assertEquals(0, c.getCount());

        s.insert(TEST_URI, new ContentValues());
        c = s.query(contentUri, null, null, selArgs, null);
        assertEquals(1, c.getCount());

        s.insert(TEST_URI, new ContentValues());
        c = s.query(contentUri, null, null, selArgs, null);
        assertEquals(2, c.getCount());

        s.delete(TEST_URI, null, null);
        c = s.query(contentUri, null, null, selArgs, null);
        assertEquals(0, c.getCount());

        try {
            s.update(TEST_URI, null, null, null);
            fail("testUpdate failed");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
