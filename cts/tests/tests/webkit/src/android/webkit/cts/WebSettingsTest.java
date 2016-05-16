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

import android.content.Context;
import android.cts.util.PollingCheck;
import android.graphics.Bitmap;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebSettings.TextSize;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.cts.WebViewOnUiThread.WaitForProgressClient;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for {@link android.webkit.WebSettings}
 */
public class WebSettingsTest extends ActivityInstrumentationTestCase2<WebViewStubActivity> {

    private static final int WEBVIEW_TIMEOUT = 5000;
    private static final String LOG_TAG = "WebSettingsTest";

    private final String EMPTY_IMAGE_HEIGHT = "0";
    private final String NETWORK_IMAGE_HEIGHT = "48";  // See getNetworkImageHtml()
    private final String DATA_URL_IMAGE_HTML = "<html>" +
            "<head><script>function updateTitle(){" +
            "document.title=document.getElementById('img').naturalHeight;}</script></head>" +
            "<body onload='updateTitle()'>" +
            "<img id='img' onload='updateTitle()' src='data:image/png;base64,iVBORw0KGgoAAA" +
            "ANSUhEUgAAAAEAAAABCAAAAAA6fptVAAAAAXNSR0IArs4c6QAAAA1JREFUCB0BAgD9/wAAAAIAAc3j" +
            "0SsAAAAASUVORK5CYII=" +
            "'></body></html>";
    private final String DATA_URL_IMAGE_HEIGHT = "1";

    private WebSettings mSettings;
    private CtsTestServer mWebServer;
    private WebViewOnUiThread mOnUiThread;
    private Context mContext;

    public WebSettingsTest() {
        super("com.android.cts.stub", WebViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mOnUiThread = new WebViewOnUiThread(this, getActivity().getWebView());
        mSettings = mOnUiThread.getSettings();
        mContext = getInstrumentation().getTargetContext();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mWebServer != null) {
            mWebServer.shutdown();
        }
        mOnUiThread.cleanUp();
        super.tearDown();
    }

    /**
     * Verifies that the default user agent string follows the format defined in Android
     * compatibility definition (tokens in angle brackets are variables, tokens in square
     * brackets are optional):
     * <p/>
     * Mozilla/5.0 (Linux;[ U;] Android <version>;[ <language>-<country>;]
     * [<devicemodel>;] Build/<buildID>) AppleWebKit/<major>.<minor> (KHTML, like Gecko)
     * Version/<major>.<minor> Chrome/<major>.<minor>.<branch>.<build>[ Mobile]
     * Safari/<major>.<minor>
     */
    public void testUserAgentString_default() {
        final String actualUserAgentString = mSettings.getUserAgentString();
        Log.i(LOG_TAG, String.format("Checking user agent string %s", actualUserAgentString));
        final String patternString =
                "Mozilla/5\\.0 \\(Linux;( U;)? Android ([^;]+);( (\\w+)-(\\w+);)?" +
                "\\s?(.*)\\sBuild/(.+)\\) AppleWebKit/(\\d+)\\.(\\d+) \\(KHTML, like Gecko\\) " +
                "Version/\\d+\\.\\d+ Chrome/\\d+\\.\\d+\\.\\d+\\.\\d+( Mobile)? " +
                "Safari/(\\d+)\\.(\\d+)";
        // Groups used:
        //  1 - SSL encryption strength token " U;" (optional)
        //  2 - Android version
        //  3 - full locale string (optional)
        //  4   - country
        //  5   - language
        //  6 - device model (optional)
        //  7 - build ID
        //  8 - AppleWebKit major version number
        //  9 - AppleWebKit minor version number
        // 10 - " Mobile" string (optional)
        // 11 - Safari major version number
        // 12 - Safari minor version number
        Log.i(LOG_TAG, String.format("Trying to match pattern %s", patternString));
        final Pattern userAgentExpr = Pattern.compile(patternString);
        Matcher patternMatcher = userAgentExpr.matcher(actualUserAgentString);
        assertTrue(String.format("User agent string did not match expected pattern. \nExpected " +
                        "pattern:\n%s\nActual:\n%s", patternString, actualUserAgentString),
                        patternMatcher.find());
        if (patternMatcher.group(3) != null) {
            Locale currentLocale = Locale.getDefault();
            assertEquals(currentLocale.getLanguage().toLowerCase(), patternMatcher.group(4));
            assertEquals(currentLocale.getCountry().toLowerCase(), patternMatcher.group(5));
        }
        if ("REL".equals(Build.VERSION.CODENAME)) {
            // Model is only added in release builds
            assertEquals(Build.MODEL, patternMatcher.group(6));
            // Release version is valid only in release builds
            assertEquals(Build.VERSION.RELEASE, patternMatcher.group(2));
        }
        assertEquals(Build.ID, patternMatcher.group(7));
    }

    public void testAccessUserAgentString() throws Exception {
        startWebServer();
        String url = mWebServer.getUserAgentUrl();

        String defaultUserAgent = mSettings.getUserAgentString();
        assertNotNull(defaultUserAgent);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(defaultUserAgent, mOnUiThread.getTitle());

        // attempting to set a null string has no effect
        mSettings.setUserAgentString(null);
        assertEquals(defaultUserAgent, mSettings.getUserAgentString());
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(defaultUserAgent, mOnUiThread.getTitle());

        // attempting to set an empty string has no effect
        mSettings.setUserAgentString("");
        assertEquals(defaultUserAgent, mSettings.getUserAgentString());
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(defaultUserAgent, mOnUiThread.getTitle());

        String customUserAgent = "Cts/test";
        mSettings.setUserAgentString(customUserAgent);
        assertEquals(customUserAgent, mSettings.getUserAgentString());
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(customUserAgent, mOnUiThread.getTitle());
    }

    public void testAccessAllowFileAccess() {
        // This test is not compatible with 4.0.3
        if ("4.0.3".equals(Build.VERSION.RELEASE)) {
            return;
        }

        assertTrue(mSettings.getAllowFileAccess());

        String fileUrl = TestHtmlConstants.getFileUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(fileUrl);
        assertEquals(TestHtmlConstants.HELLO_WORLD_TITLE, mOnUiThread.getTitle());

        fileUrl = TestHtmlConstants.getFileUrl(TestHtmlConstants.BR_TAG_URL);
        mSettings.setAllowFileAccess(false);
        assertFalse(mSettings.getAllowFileAccess());
        mOnUiThread.loadUrlAndWaitForCompletion(fileUrl);
        // android_asset URLs should still be loaded when even with file access
        // disabled.
        assertEquals(TestHtmlConstants.BR_TAG_TITLE, mOnUiThread.getTitle());

        // Files on the file system should not be loaded.
        mOnUiThread.loadUrlAndWaitForCompletion(TestHtmlConstants.LOCAL_FILESYSTEM_URL);
        assertEquals(TestHtmlConstants.WEBPAGE_NOT_AVAILABLE_TITLE, mOnUiThread.getTitle());
    }

    public void testAccessCacheMode() throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // getInstance must run on the UI thread
                WebIconDatabase iconDb = WebIconDatabase.getInstance();
                String dbPath = getActivity().getFilesDir().toString() + "/icons";
                iconDb.open(dbPath);
            }
        });
        getInstrumentation().waitForIdleSync();
        Thread.sleep(100); // Wait for open to be received on the icon db thread.
        assertEquals(WebSettings.LOAD_DEFAULT, mSettings.getCacheMode());

        mSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        assertEquals(WebSettings.LOAD_CACHE_ELSE_NETWORK, mSettings.getCacheMode());
        final IconListenerClient iconListener = new IconListenerClient();
        mOnUiThread.setWebChromeClient(iconListener);
        loadAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return iconListener.mReceivedIcon;
            }
        }.run();
        int firstFetch = mWebServer.getRequestCount();
        loadAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        assertEquals(firstFetch, mWebServer.getRequestCount());

        mSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        assertEquals(WebSettings.LOAD_NO_CACHE, mSettings.getCacheMode());
        iconListener.mReceivedIcon = false;
        loadAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return iconListener.mReceivedIcon;
            }
        }.run();
        int secondFetch = mWebServer.getRequestCount();
        iconListener.mReceivedIcon = false;
        loadAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return iconListener.mReceivedIcon;
            }
        }.run();
        int thirdFetch = mWebServer.getRequestCount();
        assertTrue(firstFetch < secondFetch);
        assertTrue(secondFetch < thirdFetch);

        mSettings.setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        assertEquals(WebSettings.LOAD_CACHE_ONLY, mSettings.getCacheMode());
        iconListener.mReceivedIcon = false;
        loadAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return iconListener.mReceivedIcon;
            }
        }.run();
        assertEquals(thirdFetch, mWebServer.getRequestCount());
    }

    public void testAccessCursiveFontFamily() throws Exception {
        assertNotNull(mSettings.getCursiveFontFamily());

        String newCusiveFamily = "Apple Chancery";
        mSettings.setCursiveFontFamily(newCusiveFamily);
        assertEquals(newCusiveFamily, mSettings.getCursiveFontFamily());
    }

    public void testAccessFantasyFontFamily() {
        assertNotNull(mSettings.getFantasyFontFamily());

        String newFantasyFamily = "Papyrus";
        mSettings.setFantasyFontFamily(newFantasyFamily);
        assertEquals(newFantasyFamily, mSettings.getFantasyFontFamily());
    }

    public void testAccessFixedFontFamily() {
        assertNotNull(mSettings.getFixedFontFamily());

        String newFixedFamily = "Courier";
        mSettings.setFixedFontFamily(newFixedFamily);
        assertEquals(newFixedFamily, mSettings.getFixedFontFamily());
    }

    public void testAccessSansSerifFontFamily() {
        assertNotNull(mSettings.getSansSerifFontFamily());

        String newFixedFamily = "Verdana";
        mSettings.setSansSerifFontFamily(newFixedFamily);
        assertEquals(newFixedFamily, mSettings.getSansSerifFontFamily());
    }

    public void testAccessSerifFontFamily() {
        assertNotNull(mSettings.getSerifFontFamily());

        String newSerifFamily = "Times";
        mSettings.setSerifFontFamily(newSerifFamily);
        assertEquals(newSerifFamily, mSettings.getSerifFontFamily());
    }

    public void testAccessStandardFontFamily() {
        assertNotNull(mSettings.getStandardFontFamily());

        String newStandardFamily = "Times";
        mSettings.setStandardFontFamily(newStandardFamily);
        assertEquals(newStandardFamily, mSettings.getStandardFontFamily());
    }

    public void testAccessDefaultFontSize() {
        int defaultSize = mSettings.getDefaultFontSize();
        assertTrue(defaultSize > 0);

        mSettings.setDefaultFontSize(1000);
        int maxSize = mSettings.getDefaultFontSize();
        // cannot check exact size set, since the implementation caps it at an arbitrary limit
        assertTrue(maxSize > defaultSize);

        mSettings.setDefaultFontSize(-10);
        int minSize = mSettings.getDefaultFontSize();
        assertTrue(minSize > 0);
        assertTrue(minSize < maxSize);

        mSettings.setDefaultFontSize(10);
        assertEquals(10, mSettings.getDefaultFontSize());
    }

    public void testAccessDefaultFixedFontSize() {
        int defaultSize = mSettings.getDefaultFixedFontSize();
        assertTrue(defaultSize > 0);

        mSettings.setDefaultFixedFontSize(1000);
        int maxSize = mSettings.getDefaultFixedFontSize();
        // cannot check exact size, since the implementation caps it at an arbitrary limit
        assertTrue(maxSize > defaultSize);

        mSettings.setDefaultFixedFontSize(-10);
        int minSize = mSettings.getDefaultFixedFontSize();
        assertTrue(minSize > 0);
        assertTrue(minSize < maxSize);

        mSettings.setDefaultFixedFontSize(10);
        assertEquals(10, mSettings.getDefaultFixedFontSize());
    }

    public void testAccessDefaultTextEncodingName() {
        assertNotNull(mSettings.getDefaultTextEncodingName());

        String newEncodingName = "iso-8859-1";
        mSettings.setDefaultTextEncodingName(newEncodingName);
        assertEquals(newEncodingName, mSettings.getDefaultTextEncodingName());
    }

    public void testAccessJavaScriptCanOpenWindowsAutomatically() throws Exception {
        mSettings.setJavaScriptEnabled(true);

        mSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        assertFalse(mSettings.getJavaScriptCanOpenWindowsAutomatically());
        loadAssetUrl(TestHtmlConstants.POPUP_URL);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return "Popup blocked".equals(mOnUiThread.getTitle());
            }
        }.run();

        mSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        assertTrue(mSettings.getJavaScriptCanOpenWindowsAutomatically());
        loadAssetUrl(TestHtmlConstants.POPUP_URL);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return "Popup allowed".equals(mOnUiThread.getTitle());
            }
        }.run();
    }

    public void testAccessJavaScriptEnabled() throws Exception {
        mSettings.setJavaScriptEnabled(true);
        assertTrue(mSettings.getJavaScriptEnabled());
        loadAssetUrl(TestHtmlConstants.JAVASCRIPT_URL);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return "javascript on".equals(mOnUiThread.getTitle());
            }
        }.run();

        mSettings.setJavaScriptEnabled(false);
        assertFalse(mSettings.getJavaScriptEnabled());
        loadAssetUrl(TestHtmlConstants.JAVASCRIPT_URL);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return "javascript off".equals(mOnUiThread.getTitle());
            }
        }.run();

    }

    public void testAccessLayoutAlgorithm() {
        assertEquals(WebSettings.LayoutAlgorithm.NARROW_COLUMNS, mSettings.getLayoutAlgorithm());

        mSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        assertEquals(WebSettings.LayoutAlgorithm.NORMAL, mSettings.getLayoutAlgorithm());

        mSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        assertEquals(WebSettings.LayoutAlgorithm.SINGLE_COLUMN, mSettings.getLayoutAlgorithm());
    }

    public void testAccessMinimumFontSize() {
        assertEquals(8, mSettings.getMinimumFontSize());

        mSettings.setMinimumFontSize(100);
        assertEquals(72, mSettings.getMinimumFontSize());

        mSettings.setMinimumFontSize(-10);
        assertEquals(1, mSettings.getMinimumFontSize());

        mSettings.setMinimumFontSize(10);
        assertEquals(10, mSettings.getMinimumFontSize());
    }

    public void testAccessMinimumLogicalFontSize() {
        assertEquals(8, mSettings.getMinimumLogicalFontSize());

        mSettings.setMinimumLogicalFontSize(100);
        assertEquals(72, mSettings.getMinimumLogicalFontSize());

        mSettings.setMinimumLogicalFontSize(-10);
        assertEquals(1, mSettings.getMinimumLogicalFontSize());

        mSettings.setMinimumLogicalFontSize(10);
        assertEquals(10, mSettings.getMinimumLogicalFontSize());
    }

    public void testAccessPluginsEnabled() {
        assertFalse(mSettings.getPluginsEnabled());

        mSettings.setPluginsEnabled(true);
        assertTrue(mSettings.getPluginsEnabled());
    }

    public void testAccessPluginsPath() {
        assertNotNull(mSettings.getPluginsPath());

        String pluginPath = "pluginPath";
        mSettings.setPluginsPath(pluginPath);
        // plugin path always empty
        assertEquals("", mSettings.getPluginsPath());
    }

    public void testAccessSaveFormData() {
        assertTrue(mSettings.getSaveFormData());

        mSettings.setSaveFormData(false);
        assertFalse(mSettings.getSaveFormData());
    }

    public void testAccessTextSize() {
        assertEquals(TextSize.NORMAL, mSettings.getTextSize());

        mSettings.setTextSize(TextSize.LARGER);
        assertEquals(TextSize.LARGER, mSettings.getTextSize());

        mSettings.setTextSize(TextSize.LARGEST);
        assertEquals(TextSize.LARGEST, mSettings.getTextSize());

        mSettings.setTextSize(TextSize.SMALLER);
        assertEquals(TextSize.SMALLER, mSettings.getTextSize());

        mSettings.setTextSize(TextSize.SMALLEST);
        assertEquals(TextSize.SMALLEST, mSettings.getTextSize());
    }

    public void testAccessUseDoubleTree() {
        assertFalse(mSettings.getUseDoubleTree());

        mSettings.setUseDoubleTree(true);
        // setUseDoubleTree is a no-op
        assertFalse(mSettings.getUseDoubleTree());
    }

    public void testAccessUseWideViewPort() {
        assertFalse(mSettings.getUseWideViewPort());

        mSettings.setUseWideViewPort(true);
        assertTrue(mSettings.getUseWideViewPort());
    }

    public void testSetNeedInitialFocus() {
        mSettings.setNeedInitialFocus(false);

        mSettings.setNeedInitialFocus(true);
    }

    public void testSetRenderPriority() {
        mSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        mSettings.setRenderPriority(WebSettings.RenderPriority.LOW);

        mSettings.setRenderPriority(WebSettings.RenderPriority.NORMAL);
    }

    public void testAccessSupportMultipleWindows() {
        assertFalse(mSettings.supportMultipleWindows());

        mSettings.setSupportMultipleWindows(true);
        assertTrue(mSettings.supportMultipleWindows());
    }

    public void testAccessSupportZoom() throws Throwable {
        assertTrue(mSettings.supportZoom());

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSettings.setSupportZoom(false);
            }
        });
        assertFalse(mSettings.supportZoom());
    }

    public void testAccessBuiltInZoomControls() throws Throwable {
        assertFalse(mSettings.getBuiltInZoomControls());

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSettings.setBuiltInZoomControls(true);
            }
        });
        assertTrue(mSettings.getBuiltInZoomControls());
    }

    public void testAppCacheDisabled() throws Throwable {
        // Test that when AppCache is disabled, we don't get any AppCache
        // callbacks.
        startWebServer();
        final String url = mWebServer.getAppCacheUrl();
        mSettings.setJavaScriptEnabled(true);

        mOnUiThread.loadUrlAndWaitForCompletion(url);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            protected boolean check() {
                return "Loaded".equals(mOnUiThread.getTitle());
            }
        }.run();
        // The page is now loaded. Wait for a further 1s to check no AppCache
        // callbacks occur.
        Thread.sleep(1000);
        assertEquals("Loaded", mOnUiThread.getTitle());
    }

    public void testAppCacheEnabled() throws Throwable {
        // Note that the AppCache path can only be set once. This limits the
        // amount of testing we can do, and means that we must test all aspects
        // of setting the AppCache path in a single test to guarantee ordering.

        // Test that when AppCache is enabled but no valid path is provided,
        // we don't get any AppCache callbacks.
        startWebServer();
        final String url = mWebServer.getAppCacheUrl();
        mSettings.setAppCacheEnabled(true);
        mSettings.setJavaScriptEnabled(true);

        mOnUiThread.loadUrlAndWaitForCompletion(url);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return "Loaded".equals(mOnUiThread.getTitle());
            }
        }.run();
        // The page is now loaded. Wait for a further 1s to check no AppCache
        // callbacks occur.
        Thread.sleep(1000);
        assertEquals("Loaded", mOnUiThread.getTitle());

        // Test that when AppCache is enabled and a valid path is provided, we
        // get an AppCache callback of some kind.
        mSettings.setAppCachePath(getActivity().getDir("appcache", 0).getPath());
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return mOnUiThread.getTitle() != null
                        && mOnUiThread.getTitle().endsWith("Callback");
            }
        }.run();
    }

    // Ideally, we need a test case for the enabled case. However, it seems that
    // enabling the database should happen prior to navigating the first url due to
    // some internal limitations of webview. For this reason, we only provide a
    // test case for "disabled" behavior.
    // Also loading as data rather than using URL should work, but it causes a
    // security exception in JS, most likely due to cross domain access. So we load
    // using a URL. Finally, it looks like enabling database requires creating a
    // webChromeClient and listening to Quota callbacks, which is not documented.
    public void testDatabaseDisabled() throws Throwable {
        // Verify that websql database does not work when disabled.
        startWebServer();

        mOnUiThread.setWebChromeClient(new ChromeClient(mOnUiThread) {
            @Override
            public void onExceededDatabaseQuota(String url, String databaseId, long quota,
                long estimatedSize, long total, WebStorage.QuotaUpdater updater) {
                updater.updateQuota(estimatedSize);
            }
        });
        mSettings.setJavaScriptEnabled(true);
        mSettings.setDatabaseEnabled(false);
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.DATABASE_ACCESS_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals("No database", mOnUiThread.getTitle());
    }

    public void testLoadsImagesAutomatically() throws Throwable {
        assertTrue(mSettings.getLoadsImagesAutomatically());

        startWebServer();
        mSettings.setJavaScriptEnabled(true);

        // Check that by default network and data url images are loaded.
        mOnUiThread.loadDataAndWaitForCompletion(getNetworkImageHtml(), "text/html", null);
        assertEquals(NETWORK_IMAGE_HEIGHT, mOnUiThread.getTitle());
        mOnUiThread.loadDataAndWaitForCompletion(DATA_URL_IMAGE_HTML, "text/html", null);
        assertEquals(DATA_URL_IMAGE_HEIGHT, mOnUiThread.getTitle());

        // Check that with auto-loading turned off no images are loaded.
        // Also check that images are loaded automatically once we enable the setting,
        // without reloading the page.
        mSettings.setLoadsImagesAutomatically(false);
        mOnUiThread.clearCache(true);
        mOnUiThread.loadDataAndWaitForCompletion(getNetworkImageHtml(), "text/html", null);
        assertEquals(EMPTY_IMAGE_HEIGHT, mOnUiThread.getTitle());
        mSettings.setLoadsImagesAutomatically(true);
        waitForNonEmptyImage();
        assertEquals(NETWORK_IMAGE_HEIGHT, mOnUiThread.getTitle());

        mSettings.setLoadsImagesAutomatically(false);
        mOnUiThread.clearCache(true);
        mOnUiThread.loadDataAndWaitForCompletion(DATA_URL_IMAGE_HTML, "text/html", null);
        assertEquals(EMPTY_IMAGE_HEIGHT, mOnUiThread.getTitle());
        mSettings.setLoadsImagesAutomatically(true);
        waitForNonEmptyImage();
        assertEquals(DATA_URL_IMAGE_HEIGHT, mOnUiThread.getTitle());
    }

    public void testBlockNetworkImage() throws Throwable {
        assertFalse(mSettings.getBlockNetworkImage());

        startWebServer();
        mSettings.setJavaScriptEnabled(true);

        // Check that by default network and data url images are loaded.
        mOnUiThread.loadDataAndWaitForCompletion(getNetworkImageHtml(), "text/html", null);
        assertEquals(NETWORK_IMAGE_HEIGHT, mOnUiThread.getTitle());
        mOnUiThread.loadDataAndWaitForCompletion(DATA_URL_IMAGE_HTML, "text/html", null);
        assertEquals(DATA_URL_IMAGE_HEIGHT, mOnUiThread.getTitle());

        // Check that only network images are blocked, data url images are still loaded.
        // Also check that network images are loaded automatically once we disable the setting,
        // without reloading the page.
        mSettings.setBlockNetworkImage(true);
        mOnUiThread.clearCache(true);
        mOnUiThread.loadDataAndWaitForCompletion(getNetworkImageHtml(), "text/html", null);
        assertEquals(EMPTY_IMAGE_HEIGHT, mOnUiThread.getTitle());
        mSettings.setBlockNetworkImage(false);
        waitForNonEmptyImage();
        assertEquals(NETWORK_IMAGE_HEIGHT, mOnUiThread.getTitle());

        mSettings.setBlockNetworkImage(true);
        mOnUiThread.clearCache(true);
        mOnUiThread.loadDataAndWaitForCompletion(DATA_URL_IMAGE_HTML, "text/html", null);
        assertEquals(DATA_URL_IMAGE_HEIGHT, mOnUiThread.getTitle());
    }

    public void testBlockNetworkLoads() throws Throwable {
        assertFalse(mSettings.getBlockNetworkLoads());

        startWebServer();
        mSettings.setJavaScriptEnabled(true);

        // Check that by default network resources and data url images are loaded.
        mOnUiThread.loadUrlAndWaitForCompletion(
            mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL));
        assertEquals(TestHtmlConstants.HELLO_WORLD_TITLE, mOnUiThread.getTitle());
        mOnUiThread.loadDataAndWaitForCompletion(getNetworkImageHtml(), "text/html", null);
        assertEquals(NETWORK_IMAGE_HEIGHT, mOnUiThread.getTitle());
        mOnUiThread.loadDataAndWaitForCompletion(DATA_URL_IMAGE_HTML, "text/html", null);
        assertEquals(DATA_URL_IMAGE_HEIGHT, mOnUiThread.getTitle());

        // Check that only network resources are blocked, data url images are still loaded.
        mSettings.setBlockNetworkLoads(true);
        mOnUiThread.clearCache(true);
        mOnUiThread.loadUrlAndWaitForCompletion(
            mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL));
        assertFalse(TestHtmlConstants.HELLO_WORLD_TITLE.equals(mOnUiThread.getTitle()));
        mOnUiThread.loadDataAndWaitForCompletion(getNetworkImageHtml(), "text/html", null);
        assertEquals(EMPTY_IMAGE_HEIGHT, mOnUiThread.getTitle());
        mOnUiThread.loadDataAndWaitForCompletion(DATA_URL_IMAGE_HTML, "text/html", null);
        assertEquals(DATA_URL_IMAGE_HEIGHT, mOnUiThread.getTitle());

        // Check that network resources are loaded once we disable the setting and reload the page.
        mSettings.setBlockNetworkLoads(false);
        mOnUiThread.loadUrlAndWaitForCompletion(
            mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL));
        assertEquals(TestHtmlConstants.HELLO_WORLD_TITLE, mOnUiThread.getTitle());
        mOnUiThread.loadDataAndWaitForCompletion(getNetworkImageHtml(), "text/html", null);
        assertEquals(NETWORK_IMAGE_HEIGHT, mOnUiThread.getTitle());
    }

    // Verify that an image in local file system can be loaded by an asset
    public void testLocalImageLoads() throws Throwable {

        mSettings.setJavaScriptEnabled(true);
        // Check that local images are loaded without issues regardless of domain checkings
        mSettings.setAllowUniversalAccessFromFileURLs(false);
        mSettings.setAllowFileAccessFromFileURLs(false);
        String url = TestHtmlConstants.getFileUrl(TestHtmlConstants.IMAGE_ACCESS_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        waitForNonEmptyImage();
        assertEquals(NETWORK_IMAGE_HEIGHT, mOnUiThread.getTitle());
    }

    // Verify that javascript cross-domain request permissions matches file domain settings
    // for iframes
    public void testIframesWhenAccessFromFileURLsEnabled() throws Throwable {

        mSettings.setJavaScriptEnabled(true);
        // disable universal access from files
        mSettings.setAllowUniversalAccessFromFileURLs(false);
        mSettings.setAllowFileAccessFromFileURLs(true);

        // when cross file scripting is enabled, make sure cross domain requests succeed
        String url = TestHtmlConstants.getFileUrl(TestHtmlConstants.IFRAME_ACCESS_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        String iframeUrl = TestHtmlConstants.getFileUrl(TestHtmlConstants.HELLO_WORLD_URL);
        assertEquals(iframeUrl, mOnUiThread.getTitle());
    }

    // Verify that javascript cross-domain request permissions matches file domain settings
    // for iframes
    public void testIframesWhenAccessFromFileURLsDisabled() throws Throwable {

        mSettings.setJavaScriptEnabled(true);
        // disable universal access from files
        mSettings.setAllowUniversalAccessFromFileURLs(false);
        mSettings.setAllowFileAccessFromFileURLs(false);

        // when cross file scripting is disabled, make sure cross domain requests fail
        final ChromeClient webChromeClient = new ChromeClient(mOnUiThread);
        mOnUiThread.setWebChromeClient(webChromeClient);
        String url = TestHtmlConstants.getFileUrl(TestHtmlConstants.IFRAME_ACCESS_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        String iframeUrl = TestHtmlConstants.getFileUrl(TestHtmlConstants.HELLO_WORLD_URL);
        assertFalse(iframeUrl.equals(mOnUiThread.getTitle()));
        assertEquals(ConsoleMessage.MessageLevel.ERROR, webChromeClient.getMessageLevel(10000));
    }

    // Verify that enabling file access from file URLs enable XmlHttpRequest (XHR) across files
    public void testXHRWhenAccessFromFileURLsEnabled() throws Throwable {
        verifyFileXHR(true);
    }

    // Verify that disabling file access from file URLs disable XmlHttpRequest (XHR) accross files
    public void testXHRWhenAccessFromFileURLsDisabled() throws Throwable {

        final ChromeClient webChromeClient = new ChromeClient(mOnUiThread);
        mOnUiThread.setWebChromeClient(webChromeClient);
        verifyFileXHR(false);
        assertEquals(ConsoleMessage.MessageLevel.ERROR, webChromeClient.getMessageLevel(10000));
    }

    // verify XHR across files matches the allowFileAccessFromFileURLs setting
    private void verifyFileXHR(boolean enableXHR) throws Throwable {
        // target file content
        String target ="<html><body>target</body><html>";

        String targetPath = mContext.getFileStreamPath("target.html").getAbsolutePath();
        // local file content that use XHR to read the target file
        String local ="" +
            "<html><body><script>" +
            "var client = new XMLHttpRequest();" +
            "client.open('GET', 'file://" + targetPath + "',false);" +
            "client.send();" +
            "document.title = client.responseText;" +
            "</script></body></html>";

        // create files in internal storage
        writeFile("local.html", local);
        writeFile("target.html", target);

        mSettings.setJavaScriptEnabled(true);
        // disable universal access from files
        mSettings.setAllowUniversalAccessFromFileURLs(false);
        mSettings.setAllowFileAccessFromFileURLs(enableXHR);
        String localPath = mContext.getFileStreamPath("local.html").getAbsolutePath();
        // when cross file scripting is enabled, make sure cross domain requests succeed
        mOnUiThread.loadUrlAndWaitForCompletion("file://" + localPath);
        if (enableXHR) assertEquals(target, mOnUiThread.getTitle());
        else assertFalse(target.equals(mOnUiThread.getTitle()));
    }

    // Create a private file on internal storage from the given string
    private void writeFile(String filename, String content) throws Exception {

        FileOutputStream fos = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
        fos.write(content.getBytes());
        fos.close();
    }

    /**
     * Starts the internal web server. The server will be shut down automatically
     * during tearDown().
     *
     * @throws Exception
     */
    private void startWebServer() throws Exception {
        assertNull(mWebServer);
        mWebServer = new CtsTestServer(getActivity(), false);
    }

    /**
     * Load the given asset from the internal web server. Starts the server if
     * it is not already running.
     *
     * @param asset The name of the asset to load.
     * @throws Exception
     */
    private void loadAssetUrl(String asset) throws Exception {
        if (mWebServer == null) {
            startWebServer();
        }
        String url = mWebServer.getAssetUrl(asset);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
    }

    private String getNetworkImageHtml() {
        return "<html>" +
                "<head><script>function updateTitle(){" +
                "document.title=document.getElementById('img').naturalHeight;}</script></head>" +
                "<body onload='updateTitle()'>" +
                "<img id='img' onload='updateTitle()' src='" +
                mWebServer.getAssetUrl(TestHtmlConstants.SMALL_IMG_URL) +
                "'></body></html>";
    }

    private void waitForNonEmptyImage() {
        new PollingCheck(WEBVIEW_TIMEOUT) {
            @Override
            protected boolean check() {
                return !EMPTY_IMAGE_HEIGHT.equals(mOnUiThread.getTitle());
            }
        }.run();
    }

    private class IconListenerClient extends WaitForProgressClient {
        public boolean mReceivedIcon;

        public IconListenerClient() {
            super(mOnUiThread);
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            mReceivedIcon = true;
        }
    }
}
