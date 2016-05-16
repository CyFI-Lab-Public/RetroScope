/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.quicksearchbox;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Tests for {@link SuggestionUtils}.
 */
@SmallTest
public class SuggestionUtilsTest extends AndroidTestCase {

    public void testUrlsWithAndWithoutSchemeEquivalent() {
        assertsUrlsEquivalent("http://www.google.com", "www.google.com");
    }

    public void testUrlsWithAndWithoutPathEquivalent() {
        assertsUrlsEquivalent("http://www.google.com/", "www.google.com");
        assertsUrlsEquivalent("www.google.com/", "http://www.google.com");
        assertsUrlsNotEquivalent("www.google.com/search/", "http://www.google.com/search");
    }

    public void testHttpAndHttpsUrlsNotEquivalent() {
        assertsUrlsNotEquivalent("https://www.google.com/", "http://www.google.com/");
        assertsUrlsNotEquivalent("https://www.google.com", "www.google.com");
    }

    public void testNonHttpUrlsEquivalent() {
        assertsUrlsEquivalent("gopher://www.google.com/", "gopher://www.google.com");
    }

    public void testNonHttpUrlsAndNoSchemeNotEquivalent() {
        assertsUrlsNotEquivalent("gopher://www.google.com", "www.google.com");
    }

    public void testUrlsWithDifferentPathsNotEquivalent() {
        assertsUrlsNotEquivalent("www.google.com/search", "www.google.com");
        assertsUrlsNotEquivalent("http://www.google.com/search", "www.google.com");
        assertsUrlsNotEquivalent("www.google.com/search", "http://www.google.com");
    }

    private void assertsUrlsEquivalent(String url1, String url2) {
        assertTrue("Urls " + url1 + " and " + url2 + " not equal",
                SuggestionUtils.normalizeUrl(url1).equals(SuggestionUtils.normalizeUrl(url2)));
    }

    private void assertsUrlsNotEquivalent(String url1, String url2) {
        assertFalse("Urls " + url1 + " and " + url2 + " equal",
                SuggestionUtils.normalizeUrl(url1).equals(SuggestionUtils.normalizeUrl(url2)));
    }
}
