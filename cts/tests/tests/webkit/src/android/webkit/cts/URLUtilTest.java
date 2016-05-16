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

package android.webkit.cts;


import android.test.AndroidTestCase;
import android.test.MoreAsserts;
import android.webkit.URLUtil;

public class URLUtilTest extends AndroidTestCase {
    private final String VALID_HTTP_URL = "http://www.google.com";
    private final String VALID_HTTPS_URL = "https://www.google.com";
    private final String VALID_ASSET_URL = "file:///android_asset/test";
    private final String VALID_PROXY_URL = "file:///cookieless_proxy/test";
    private final String VALID_JAVASCRIPT_URL = "javascript:wave()";
    private final String VALID_CONTENT_URL = "content://test";
    private final String VALID_DATA_URL = "data://test";
    private final String VALID_ABOUT_URL = "about://test";
    private final String VALID_FILE_URL = "file://test";
    private final String VALID_FTP_URL = "ftp://www.domain.com";

    public void testIsAssetUrl() {
        assertFalse(URLUtil.isAssetUrl(null));
        assertFalse(URLUtil.isAssetUrl(VALID_HTTP_URL));
        assertTrue(URLUtil.isAssetUrl(VALID_ASSET_URL));
    }

    public void testIsAboutUrl() {
        assertFalse(URLUtil.isAboutUrl(null));
        assertFalse(URLUtil.isAboutUrl(VALID_DATA_URL));
        assertTrue(URLUtil.isAboutUrl(VALID_ABOUT_URL));
    }

    public void testIsContentUrl() {
        assertFalse(URLUtil.isContentUrl(null));
        assertFalse(URLUtil.isContentUrl(VALID_DATA_URL));
        assertTrue(URLUtil.isContentUrl(VALID_CONTENT_URL));
    }

    @SuppressWarnings("deprecation")
    public void testIsCookielessProxyUrl() {
        assertFalse(URLUtil.isCookielessProxyUrl(null));
        assertFalse(URLUtil.isCookielessProxyUrl(VALID_HTTP_URL));
        assertTrue(URLUtil.isCookielessProxyUrl(VALID_PROXY_URL));
    }

    public void testIsDataUrl() {
        assertFalse(URLUtil.isDataUrl(null));
        assertFalse(URLUtil.isDataUrl(VALID_CONTENT_URL));
        assertTrue(URLUtil.isDataUrl(VALID_DATA_URL));
    }

    public void testIsFileUrl() {
        assertFalse(URLUtil.isFileUrl(null));
        assertFalse(URLUtil.isFileUrl(VALID_CONTENT_URL));
        assertFalse(URLUtil.isFileUrl(VALID_ASSET_URL));
        assertFalse(URLUtil.isFileUrl(VALID_PROXY_URL));
        assertTrue(URLUtil.isFileUrl(VALID_FILE_URL));
    }

    public void testIsHttpsUrl() {
        assertFalse(URLUtil.isHttpsUrl(null));
        assertFalse(URLUtil.isHttpsUrl(VALID_HTTP_URL));
        assertTrue(URLUtil.isHttpsUrl(VALID_HTTPS_URL));
    }

    public void testIsHttpUrl() {
        assertFalse(URLUtil.isHttpUrl(null));
        assertFalse(URLUtil.isHttpUrl(VALID_FTP_URL));
        assertTrue(URLUtil.isHttpUrl(VALID_HTTP_URL));
    }

    public void testIsJavaScriptUrl() {
        assertFalse(URLUtil.isJavaScriptUrl(null));
        assertFalse(URLUtil.isJavaScriptUrl(VALID_FTP_URL));
        assertTrue(URLUtil.isJavaScriptUrl(VALID_JAVASCRIPT_URL));
    }

    public void testIsNetworkUrl() {
        assertFalse(URLUtil.isNetworkUrl(null));
        assertFalse(URLUtil.isNetworkUrl(""));
        assertFalse(URLUtil.isNetworkUrl(VALID_FTP_URL));
        assertTrue(URLUtil.isNetworkUrl(VALID_HTTP_URL));
        assertTrue(URLUtil.isNetworkUrl(VALID_HTTPS_URL));
    }

    public void testIsValidUrl() {
        assertFalse(URLUtil.isValidUrl(null));
        assertFalse(URLUtil.isValidUrl(""));
        assertFalse(URLUtil.isValidUrl("Error URL"));
        assertTrue(URLUtil.isValidUrl(VALID_ASSET_URL));
        assertTrue(URLUtil.isValidUrl(VALID_FILE_URL));
        assertTrue(URLUtil.isValidUrl(VALID_ABOUT_URL));
        assertTrue(URLUtil.isValidUrl(VALID_HTTP_URL));
        assertTrue(URLUtil.isValidUrl(VALID_HTTPS_URL));
        assertTrue(URLUtil.isValidUrl(VALID_JAVASCRIPT_URL));
        assertTrue(URLUtil.isValidUrl(VALID_CONTENT_URL));
    }

    public void testComposeSearchUrl() {
        assertNull(URLUtil.composeSearchUrl("", "template", "no such holder"));

        String expected = "http://story/test";
        assertEquals(expected, URLUtil.composeSearchUrl("story", "http://holder/test", "holder"));

        expected = "file://query/test";
        assertEquals(expected, URLUtil.composeSearchUrl("query", "file://holder/test", "holder"));
    }

    public void testDecode() {
        byte[] url = new byte[0];
        byte[] result = URLUtil.decode(url);
        assertEquals(0, result.length);

        url = new byte[] { 'w', 'w', 'w', '.', 'n', 'a', 'm', 'e', '.', 'c', 'o', 'm', '/',
                '%', '2', '0', '%', '4', '5', '/' };
        result = URLUtil.decode(url);
        byte[] expected = new byte[] { 'w', 'w', 'w', '.', 'n', 'a', 'm', 'e', '.', 'c', 'o', 'm',
                '/', ' ', 'E', '/' };
        MoreAsserts.assertEquals(expected, result);

        url = new byte[] { 'w', 'w', 'w', '.', 'n', 'a', 'm', 'e', '.', 'c', 'o', 'm', '/',
                '%', '2', '0', '%', '4' };
        try {
            result = URLUtil.decode(url);
            fail("should throw IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGuessFileName() {
        String url = "ftp://example.url/test";
        assertEquals("test.jpeg", URLUtil.guessFileName(url, null, "image/jpeg"));

        assertEquals("test.bin", URLUtil.guessFileName(url, null, "application/octet-stream"));
    }

    public void testGuessUrl() {
        assertEquals(VALID_FILE_URL, URLUtil.guessUrl(VALID_FILE_URL));
        assertEquals(VALID_ABOUT_URL, URLUtil.guessUrl(VALID_ABOUT_URL));
        assertEquals(VALID_DATA_URL, URLUtil.guessUrl(VALID_DATA_URL));
        assertEquals(VALID_JAVASCRIPT_URL, URLUtil.guessUrl(VALID_JAVASCRIPT_URL));

        String url = "domainName";
        assertEquals("http://www.domainName.com/", URLUtil.guessUrl(url));

        try {
            URLUtil.guessUrl(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testStripAnchor() {
        assertEquals(VALID_HTTP_URL, URLUtil.stripAnchor(VALID_HTTP_URL));

        String url = "http://www.google.com#test";
        String expected = "http://www.google.com";
        assertEquals(expected, URLUtil.stripAnchor(url));
    }
}
