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

package com.android.providers.contacts;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.ContactCounts;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.providers.contacts.util.MockSharedPreferences;

@SmallTest
public class FastScrollingIndexCacheTest extends AndroidTestCase {
    private MockSharedPreferences mPrefs;
    private FastScrollingIndexCache mCache;

    private static final String[] TITLES_0 = new String[] {};
    private static final String[] TITLES_1 = new String[] {"a"};
    private static final String[] TITLES_2 = new String[] {"", "b"};
    private static final String[] TITLES_3 = new String[] {"", "b", "aaa"};

    private static final int[] COUNTS_0 = new int[] {};
    private static final int[] COUNTS_1 = new int[] {1};
    private static final int[] COUNTS_2 = new int[] {2, 3};
    private static final int[] COUNTS_3 = new int[] {0, -1, 2};

    private static final String[] PROJECTION_0 = new String[] {};
    private static final String[] PROJECTION_1 = new String[] {"c1"};
    private static final String[] PROJECTION_2 = new String[] {"c3", "c4"};

    private static final Uri URI_A = Contacts.CONTENT_URI;
    private static final Uri URI_B = RawContacts.CONTENT_URI;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mPrefs = new MockSharedPreferences();
        mCache = FastScrollingIndexCache.getInstanceForTest(mPrefs);
    }

    private void assertBundle(String[] expectedTitles, int[] expectedCounts, Bundle actual) {
        assertNotNull(actual);
        MoreAsserts.assertEquals(expectedTitles,
                actual.getStringArray(ContactCounts.EXTRA_ADDRESS_BOOK_INDEX_TITLES));
        MoreAsserts.assertEquals(expectedCounts,
                actual.getIntArray(ContactCounts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS));
    }

    /**
     * Test for {@link FastScrollingIndexCache#buildExtraBundleFromValue} and
     * {@link FastScrollingIndexCache#buildCacheValue}.
     */
    public void testBuildCacheValue() {
        assertBundle(TITLES_0, COUNTS_0,
                FastScrollingIndexCache.buildExtraBundleFromValue(
                        FastScrollingIndexCache.buildCacheValue(TITLES_0, COUNTS_0)));
        assertBundle(TITLES_1, COUNTS_1,
                FastScrollingIndexCache.buildExtraBundleFromValue(
                        FastScrollingIndexCache.buildCacheValue(TITLES_1, COUNTS_1)));
        assertBundle(TITLES_2, COUNTS_2,
                FastScrollingIndexCache.buildExtraBundleFromValue(
                        FastScrollingIndexCache.buildCacheValue(TITLES_2, COUNTS_2)));
    }

    private static final Bundle putAndGetBundle(FastScrollingIndexCache cache, Uri queryUri,
            String selection, String[] selectionArgs, String sortOrder, String countExpression,
            String[] titles, int[] counts) {
        Bundle bundle = FastScrollingIndexCache.buildExtraBundle(titles, counts);
        cache.put(queryUri, selection, selectionArgs, sortOrder, countExpression, bundle);
        return bundle;
    }

    public void testPutAndGet() {
        // Initially the cache is empty
        assertNull(mCache.get(null, null, null, null, null));
        assertNull(mCache.get(URI_A, "*s*", PROJECTION_0, "*so*", "*ce*"));
        assertNull(mCache.get(URI_A, "*s*", PROJECTION_1, "*so*", "*ce*"));
        assertNull(mCache.get(URI_B, "s", PROJECTION_2, "so", "ce"));

        // Put...
        Bundle b;
        b = putAndGetBundle(mCache, null, null, null, null, null, TITLES_0, COUNTS_0);
        assertBundle(TITLES_0, COUNTS_0, b);

        b = putAndGetBundle(mCache, URI_A, "*s*", PROJECTION_0, "*so*", "*ce*", TITLES_1, COUNTS_1);
        assertBundle(TITLES_1, COUNTS_1, b);

        b = putAndGetBundle(mCache, URI_A, "*s*", PROJECTION_1, "*so*", "*ce*", TITLES_2, COUNTS_2);
        assertBundle(TITLES_2, COUNTS_2, b);

        b = putAndGetBundle(mCache, URI_B, "s", PROJECTION_2, "so", "ce", TITLES_3, COUNTS_3);
        assertBundle(TITLES_3, COUNTS_3, b);

        // Get...
        assertBundle(TITLES_0, COUNTS_0, mCache.get(null, null, null, null, null));
        assertBundle(TITLES_1, COUNTS_1, mCache.get(URI_A, "*s*", PROJECTION_0, "*so*", "*ce*"));
        assertBundle(TITLES_2, COUNTS_2, mCache.get(URI_A, "*s*", PROJECTION_1, "*so*", "*ce*"));
        assertBundle(TITLES_3, COUNTS_3, mCache.get(URI_B, "s", PROJECTION_2, "so", "ce"));

        // Invalidate...
        mCache.invalidate();

        // Get again... Nothing shoul be cached...
        assertNull(mCache.get(null, null, null, null, null));
        assertNull(mCache.get(URI_A, "*s*", PROJECTION_0, "*so*", "*ce*"));
        assertNull(mCache.get(URI_A, "*s*", PROJECTION_1, "*so*", "*ce*"));
        assertNull(mCache.get(URI_B, "s", PROJECTION_2, "so", "ce"));

        // Put again...
        b = putAndGetBundle(mCache, null, null, null, null, null, TITLES_0, COUNTS_0);
        assertBundle(TITLES_0, COUNTS_0, b);

        b = putAndGetBundle(mCache, URI_A, "*s*", PROJECTION_0, "*so*", "*ce*", TITLES_1, COUNTS_1);
        assertBundle(TITLES_1, COUNTS_1, b);

        b = putAndGetBundle(mCache, URI_A, "*s*", PROJECTION_1, "*so*", "*ce*", TITLES_2, COUNTS_2);
        assertBundle(TITLES_2, COUNTS_2, b);

        b = putAndGetBundle(mCache, URI_B, "s", PROJECTION_2, "so", "ce", TITLES_2, COUNTS_2);
        assertBundle(TITLES_2, COUNTS_2, b);

        // Now, create a new cache instance (with the same shared preferences)
        // It should restore the cache content from the preferences...

        FastScrollingIndexCache cache2 = FastScrollingIndexCache.getInstanceForTest(mPrefs);
        assertBundle(TITLES_0, COUNTS_0, cache2.get(null, null, null, null, null));
        assertBundle(TITLES_1, COUNTS_1, cache2.get(URI_A, "*s*", PROJECTION_0, "*so*", "*ce*"));
        assertBundle(TITLES_2, COUNTS_2, cache2.get(URI_A, "*s*", PROJECTION_1, "*so*", "*ce*"));
        assertBundle(TITLES_2, COUNTS_2, cache2.get(URI_B, "s", PROJECTION_2, "so", "ce"));
    }

    public void testMalformedPreferences() {
        mPrefs.edit().putString(FastScrollingIndexCache.PREFERENCE_KEY, "123");
        // get() shouldn't crash
        assertNull(mCache.get(null, null, null, null, null));
    }
}
