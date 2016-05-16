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

import android.cts.util.EvaluateJsResultPollingCheck;
import android.cts.util.PollingCheck;
import android.graphics.Bitmap;
import android.os.Message;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.HttpAuthHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.cts.WebViewOnUiThread.WaitForLoadedClient;


public class WebViewClientTest extends ActivityInstrumentationTestCase2<WebViewStubActivity> {
    private static final long TEST_TIMEOUT = 5000;
    private static final String TEST_URL = "http://foo.com/";

    private WebViewOnUiThread mOnUiThread;
    private CtsTestServer mWebServer;

    public WebViewClientTest() {
        super("com.android.cts.stub", WebViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final WebViewStubActivity activity = getActivity();
        new PollingCheck(TEST_TIMEOUT) {
            @Override
                protected boolean check() {
                return activity.hasWindowFocus();
            }
        }.run();
        mOnUiThread = new WebViewOnUiThread(this, activity.getWebView());
    }

    @Override
    protected void tearDown() throws Exception {
        mOnUiThread.cleanUp();
        if (mWebServer != null) {
            mWebServer.shutdown();
        }
        super.tearDown();
    }

    // Verify that the shouldoverrideurlloading is false by default
    public void testShouldOverrideUrlLoadingDefault() {
        final WebViewClient webViewClient = new WebViewClient();
        assertFalse(webViewClient.shouldOverrideUrlLoading(mOnUiThread.getWebView(), null));
    }

    // Verify shouldoverrideurlloading called on top level navigation
    public void testShouldOverrideUrlLoading() {
        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);
        mOnUiThread.getSettings().setJavaScriptEnabled(true);
        String data = "<html><body>" +
                "<a href=\"" + TEST_URL + "\" id=\"link\">new page</a>" +
                "</body></html>";
        mOnUiThread.loadDataAndWaitForCompletion(data, "text/html", null);
        clickOnLinkUsingJs("link");
        assertEquals(TEST_URL, webViewClient.getLastShouldOverrideUrl());
    }

    // Verify shouldoverrideurlloading called on webview called via onCreateWindow
    public void testShouldOverrideUrlLoadingOnCreateWindow() throws Exception {
        mWebServer = new CtsTestServer(getActivity());
        // WebViewClient for main window
        final MockWebViewClient mainWebViewClient = new MockWebViewClient();
        // WebViewClient for child window
        final MockWebViewClient childWebViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(mainWebViewClient);
        mOnUiThread.getSettings().setJavaScriptEnabled(true);
        mOnUiThread.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mOnUiThread.getSettings().setSupportMultipleWindows(true);
        mOnUiThread.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(
                WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                WebView childWebView = new WebView(view.getContext());
                childWebView.setWebViewClient(childWebViewClient);
                childWebView.getSettings().setJavaScriptEnabled(true);
                transport.setWebView(childWebView);
                getActivity().addContentView(childWebView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.FILL_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                resultMsg.sendToTarget();
                return true;
            }
        });
        mOnUiThread.loadUrl(mWebServer.getAssetUrl(TestHtmlConstants.BLANK_TAG_URL));

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return childWebViewClient.hasOnPageFinishedCalled();
            }
        }.run();
        assertEquals(mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL),
                childWebViewClient.getLastShouldOverrideUrl());
    }

    private void clickOnLinkUsingJs(final String linkId) {
        EvaluateJsResultPollingCheck jsResult = new EvaluateJsResultPollingCheck("null");
        mOnUiThread.evaluateJavascript(
                "document.getElementById('" + linkId + "').click();" +
                "console.log('element with id [" + linkId + "] clicked');", jsResult);
        jsResult.run();
    }

    public void testLoadPage() throws Exception {
        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);
        mWebServer = new CtsTestServer(getActivity());
        String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);

        assertFalse(webViewClient.hasOnPageStartedCalled());
        assertFalse(webViewClient.hasOnLoadResourceCalled());
        assertFalse(webViewClient.hasOnPageFinishedCalled());
        mOnUiThread.loadUrlAndWaitForCompletion(url);

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webViewClient.hasOnPageStartedCalled();
            }
        }.run();

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webViewClient.hasOnLoadResourceCalled();
            }
        }.run();

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webViewClient.hasOnPageFinishedCalled();
            }
        }.run();
    }

    public void testOnReceivedError() throws Exception {
        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);

        String wrongUri = "invalidscheme://some/resource";
        assertEquals(0, webViewClient.hasOnReceivedErrorCode());
        mOnUiThread.loadUrlAndWaitForCompletion(wrongUri);
        assertEquals(WebViewClient.ERROR_UNSUPPORTED_SCHEME,
                webViewClient.hasOnReceivedErrorCode());
    }

    public void testOnFormResubmission() throws Exception {
        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);
        final WebSettings settings = mOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);
        mWebServer = new CtsTestServer(getActivity());

        assertFalse(webViewClient.hasOnFormResubmissionCalled());
        String url = mWebServer.getAssetUrl(TestHtmlConstants.JS_FORM_URL);
        // this loads a form, which automatically posts itself
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        // wait for JavaScript to post the form
        mOnUiThread.waitForLoadCompletion();
        // the URL should have changed when the form was posted
        assertFalse(url.equals(mOnUiThread.getUrl()));
        // reloading the current URL should trigger the callback
        mOnUiThread.reload();
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webViewClient.hasOnFormResubmissionCalled();
            }
        }.run();
    }

    public void testDoUpdateVisitedHistory() throws Exception {
        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);
        mWebServer = new CtsTestServer(getActivity());

        assertFalse(webViewClient.hasDoUpdateVisitedHistoryCalled());
        String url1 = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        String url2 = mWebServer.getAssetUrl(TestHtmlConstants.BR_TAG_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url1);
        mOnUiThread.loadUrlAndWaitForCompletion(url2);
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webViewClient.hasDoUpdateVisitedHistoryCalled();
            }
        }.run();
    }

    public void testOnReceivedHttpAuthRequest() throws Exception {
        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);
        mWebServer = new CtsTestServer(getActivity());

        assertFalse(webViewClient.hasOnReceivedHttpAuthRequestCalled());
        String url = mWebServer.getAuthAssetUrl(TestHtmlConstants.EMBEDDED_IMG_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertTrue(webViewClient.hasOnReceivedHttpAuthRequestCalled());
    }

    public void testShouldOverrideKeyEvent() {
        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);

        assertFalse(webViewClient.shouldOverrideKeyEvent(mOnUiThread.getWebView(), null));
    }

    public void testOnUnhandledKeyEvent() throws Throwable {
        requireLoadedPage();
        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);

        mOnUiThread.requestFocus();
        getInstrumentation().waitForIdleSync();

        assertFalse(webViewClient.hasOnUnhandledKeyEventCalled());
        sendKeys(KeyEvent.KEYCODE_1);

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webViewClient.hasOnUnhandledKeyEventCalled();
            }
        }.run();
    }

    public void testOnScaleChanged() throws Throwable {
        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);
        mWebServer = new CtsTestServer(getActivity());

        assertFalse(webViewClient.hasOnScaleChangedCalled());
        String url1 = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url1);

        mOnUiThread.zoomIn();
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webViewClient.hasOnScaleChangedCalled();
            }
        }.run();
    }

    private void requireLoadedPage() throws Throwable {
        mOnUiThread.loadUrlAndWaitForCompletion("about:blank");
    }

    private class MockWebViewClient extends WaitForLoadedClient {
        private boolean mOnPageStartedCalled;
        private boolean mOnPageFinishedCalled;
        private boolean mOnLoadResourceCalled;
        private int mOnReceivedErrorCode;
        private boolean mOnFormResubmissionCalled;
        private boolean mDoUpdateVisitedHistoryCalled;
        private boolean mOnReceivedHttpAuthRequestCalled;
        private boolean mOnUnhandledKeyEventCalled;
        private boolean mOnScaleChangedCalled;
        private String mLastShouldOverrideUrl;

        public MockWebViewClient() {
            super(mOnUiThread);
        }

        public boolean hasOnPageStartedCalled() {
            return mOnPageStartedCalled;
        }

        public boolean hasOnPageFinishedCalled() {
            return mOnPageFinishedCalled;
        }

        public boolean hasOnLoadResourceCalled() {
            return mOnLoadResourceCalled;
        }

        public int hasOnReceivedErrorCode() {
            return mOnReceivedErrorCode;
        }

        public boolean hasOnFormResubmissionCalled() {
            return mOnFormResubmissionCalled;
        }

        public boolean hasDoUpdateVisitedHistoryCalled() {
            return mDoUpdateVisitedHistoryCalled;
        }

        public boolean hasOnReceivedHttpAuthRequestCalled() {
            return mOnReceivedHttpAuthRequestCalled;
        }

        public boolean hasOnUnhandledKeyEventCalled() {
            return mOnUnhandledKeyEventCalled;
        }

        public boolean hasOnScaleChangedCalled() {
            return mOnScaleChangedCalled;
        }

        public String getLastShouldOverrideUrl() {
            return mLastShouldOverrideUrl;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mOnPageStartedCalled = true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            assertTrue(mOnPageStartedCalled);
            assertTrue(mOnLoadResourceCalled);
            mOnPageFinishedCalled = true;
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            assertTrue(mOnPageStartedCalled);
            mOnLoadResourceCalled = true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            mOnReceivedErrorCode = errorCode;
        }

        @Override
        public void onFormResubmission(WebView view, Message dontResend, Message resend) {
            mOnFormResubmissionCalled = true;
            dontResend.sendToTarget();
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
            mDoUpdateVisitedHistoryCalled = true;
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                HttpAuthHandler handler, String host, String realm) {
            super.onReceivedHttpAuthRequest(view, handler, host, realm);
            mOnReceivedHttpAuthRequestCalled = true;
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            super.onUnhandledKeyEvent(view, event);
            mOnUnhandledKeyEventCalled = true;
        }

        @Override
        public void onScaleChanged(WebView view, float oldScale, float newScale) {
            super.onScaleChanged(view, oldScale, newScale);
            mOnScaleChangedCalled = true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            mLastShouldOverrideUrl = url;
            return false;
        }
    }
}
