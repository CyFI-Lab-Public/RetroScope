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

import android.cts.util.PollingCheck;
import android.graphics.Bitmap;
import android.os.Message;
import android.test.ActivityInstrumentationTestCase2;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.cts.WebViewOnUiThread.WaitForProgressClient;

public class WebChromeClientTest extends ActivityInstrumentationTestCase2<WebViewStubActivity> {
    private static final long TEST_TIMEOUT = 5000L;

    private CtsTestServer mWebServer;
    private WebIconDatabase mIconDb;
    private WebViewOnUiThread mOnUiThread;
    private boolean mBlockWindowCreationSync;
    private boolean mBlockWindowCreationAsync;

    public WebChromeClientTest() {
        super(WebViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mOnUiThread = new WebViewOnUiThread(this, getActivity().getWebView());
        mWebServer = new CtsTestServer(getActivity());
    }

    @Override
    protected void tearDown() throws Exception {
        mOnUiThread.cleanUp();
        if (mWebServer != null) {
            mWebServer.shutdown();
        }
        if (mIconDb != null) {
            mIconDb.removeAllIcons();
            mIconDb.close();
        }
        super.tearDown();
    }

    public void testOnProgressChanged() {
        final MockWebChromeClient webChromeClient = new MockWebChromeClient();
        mOnUiThread.setWebChromeClient(webChromeClient);

        assertFalse(webChromeClient.hadOnProgressChanged());
        String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webChromeClient.hadOnProgressChanged();
            }
        }.run();
    }

    public void testOnReceivedTitle() throws Exception {
        final MockWebChromeClient webChromeClient = new MockWebChromeClient();
        mOnUiThread.setWebChromeClient(webChromeClient);

        assertFalse(webChromeClient.hadOnReceivedTitle());
        String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webChromeClient.hadOnReceivedTitle();
            }
        }.run();
        assertTrue(webChromeClient.hadOnReceivedTitle());
        assertEquals(TestHtmlConstants.HELLO_WORLD_TITLE, webChromeClient.getPageTitle());
    }

    public void testOnReceivedIcon() throws Throwable {
        final MockWebChromeClient webChromeClient = new MockWebChromeClient();
        mOnUiThread.setWebChromeClient(webChromeClient);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // getInstance must run on the UI thread
                mIconDb = WebIconDatabase.getInstance();
                String dbPath = getActivity().getFilesDir().toString() + "/icons";
                mIconDb.open(dbPath);
            }
        });
        getInstrumentation().waitForIdleSync();
        Thread.sleep(100); // Wait for open to be received on the icon db thread.

        assertFalse(webChromeClient.hadOnReceivedIcon());

        String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webChromeClient.hadOnReceivedIcon();
            }
        }.run();
    }

    public void runWindowTest(boolean expectWindowClose) throws Exception {
        final MockWebChromeClient webChromeClient = new MockWebChromeClient();
        mOnUiThread.setWebChromeClient(webChromeClient);

        final WebSettings settings = mOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);

        assertFalse(webChromeClient.hadOnCreateWindow());

        // Load a page that opens a child window and sets a timeout after which the child
        // will be closed.
        mOnUiThread.loadUrlAndWaitForCompletion(mWebServer.
                getAssetUrl(TestHtmlConstants.JS_WINDOW_URL));

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webChromeClient.hadOnCreateWindow();
            }
        }.run();

        if (expectWindowClose) {
            new PollingCheck(TEST_TIMEOUT) {
                @Override
                protected boolean check() {
                    return webChromeClient.hadOnCloseWindow();
                }
            }.run();
        } else {
            assertFalse(webChromeClient.hadOnCloseWindow());
        }
    }
    public void testWindows() throws Exception {
        runWindowTest(true);
    }

    public void testBlockWindowsSync() throws Exception {
        mBlockWindowCreationSync = true;
        runWindowTest(false);
    }

    public void testBlockWindowsAsync() throws Exception {
        mBlockWindowCreationAsync = true;
        runWindowTest(false);
    }

    public void testOnJsBeforeUnload() throws Exception {
        final MockWebChromeClient webChromeClient = new MockWebChromeClient();
        mOnUiThread.setWebChromeClient(webChromeClient);

        final WebSettings settings = mOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        assertFalse(webChromeClient.hadOnJsBeforeUnload());

        mOnUiThread.loadUrlAndWaitForCompletion(mWebServer.getAssetUrl(TestHtmlConstants.JS_UNLOAD_URL));
        // unload should trigger when we try to navigate away
        mOnUiThread.loadUrlAndWaitForCompletion(mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL));

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webChromeClient.hadOnJsBeforeUnload();
            }
        }.run();
        assertEquals(webChromeClient.getMessage(), "testOnJsBeforeUnload");
    }

    public void testOnJsAlert() throws Exception {
        final MockWebChromeClient webChromeClient = new MockWebChromeClient();
        mOnUiThread.setWebChromeClient(webChromeClient);

        final WebSettings settings = mOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        assertFalse(webChromeClient.hadOnJsAlert());

        String url = mWebServer.getAssetUrl(TestHtmlConstants.JS_ALERT_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webChromeClient.hadOnJsAlert();
            }
        }.run();
        assertEquals(webChromeClient.getMessage(), "testOnJsAlert");
    }

    public void testOnJsConfirm() throws Exception {
        final MockWebChromeClient webChromeClient = new MockWebChromeClient();
        mOnUiThread.setWebChromeClient(webChromeClient);

        final WebSettings settings = mOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        assertFalse(webChromeClient.hadOnJsConfirm());

        String url = mWebServer.getAssetUrl(TestHtmlConstants.JS_CONFIRM_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webChromeClient.hadOnJsConfirm();
            }
        }.run();
        assertEquals(webChromeClient.getMessage(), "testOnJsConfirm");
    }

    public void testOnJsPrompt() throws Exception {
        final MockWebChromeClient webChromeClient = new MockWebChromeClient();
        mOnUiThread.setWebChromeClient(webChromeClient);

        final WebSettings settings = mOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        assertFalse(webChromeClient.hadOnJsPrompt());

        final String promptResult = "CTS";
        webChromeClient.setPromptResult(promptResult);
        String url = mWebServer.getAssetUrl(TestHtmlConstants.JS_PROMPT_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webChromeClient.hadOnJsPrompt();
            }
        }.run();
        // the result returned by the client gets set as the page title
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return mOnUiThread.getTitle().equals(promptResult);
            }
        }.run();
        assertEquals(webChromeClient.getMessage(), "testOnJsPrompt");
    }

    private class MockWebChromeClient extends WaitForProgressClient {
        private boolean mHadOnProgressChanged;
        private boolean mHadOnReceivedTitle;
        private String mPageTitle;
        private boolean mHadOnJsAlert;
        private boolean mHadOnJsConfirm;
        private boolean mHadOnJsPrompt;
        private boolean mHadOnJsBeforeUnload;
        private String mMessage;
        private String mPromptResult;
        private boolean mHadOnCloseWindow;
        private boolean mHadOnCreateWindow;
        private boolean mHadOnRequestFocus;
        private boolean mHadOnReceivedIcon;

        public MockWebChromeClient() {
            super(mOnUiThread);
        }

        public void setPromptResult(String promptResult) {
            mPromptResult = promptResult;
        }

        public boolean hadOnProgressChanged() {
            return mHadOnProgressChanged;
        }

        public boolean hadOnReceivedTitle() {
            return mHadOnReceivedTitle;
        }

        public String getPageTitle() {
            return mPageTitle;
        }

        public boolean hadOnJsAlert() {
            return mHadOnJsAlert;
        }

        public boolean hadOnJsConfirm() {
            return mHadOnJsConfirm;
        }

        public boolean hadOnJsPrompt() {
            return mHadOnJsPrompt;
        }

        public boolean hadOnJsBeforeUnload() {
            return mHadOnJsBeforeUnload;
        }

        public boolean hadOnCreateWindow() {
            return mHadOnCreateWindow;
        }

        public boolean hadOnCloseWindow() {
            return mHadOnCloseWindow;
        }

        public boolean hadOnRequestFocus() {
            return mHadOnRequestFocus;
        }

        public boolean hadOnReceivedIcon() {
            return mHadOnReceivedIcon;
        }

        public String getMessage() {
            return mMessage;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            mHadOnProgressChanged = true;
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            mPageTitle = title;
            mHadOnReceivedTitle = true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            super.onJsAlert(view, url, message, result);
            mHadOnJsAlert = true;
            mMessage = message;
            result.confirm();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            super.onJsConfirm(view, url, message, result);
            mHadOnJsConfirm = true;
            mMessage = message;
            result.confirm();
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message,
                String defaultValue, JsPromptResult result) {
            super.onJsPrompt(view, url, message, defaultValue, result);
            mHadOnJsPrompt = true;
            mMessage = message;
            result.confirm(mPromptResult);
            return true;
        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
            super.onJsBeforeUnload(view, url, message, result);
            mHadOnJsBeforeUnload = true;
            mMessage = message;
            result.confirm();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            mHadOnCloseWindow = true;
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture,
                Message resultMsg) {
            mHadOnCreateWindow = true;
            if (mBlockWindowCreationSync) {
                return false;
            }
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            if (mBlockWindowCreationAsync) {
                transport.setWebView(null);
            } else {
                WebView childView = new WebView(getActivity());
                final WebSettings settings = childView.getSettings();
                settings.setJavaScriptEnabled(true);
                childView.setWebChromeClient(this);
                transport.setWebView(childView);
                getActivity().addContentView(childView, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onRequestFocus(WebView view) {
            mHadOnRequestFocus = true;
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            mHadOnReceivedIcon = true;
        }
    }
}
