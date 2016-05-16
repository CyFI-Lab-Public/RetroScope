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
import android.test.ActivityInstrumentationTestCase2;
import android.webkit.WebBackForwardList;
import android.webkit.cts.WebViewOnUiThread.WaitForProgressClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;

public class WebHistoryItemTest extends ActivityInstrumentationTestCase2<WebViewStubActivity> {
    private final static long TEST_TIMEOUT = 10000;
    private CtsTestServer mWebServer;
    private WebViewOnUiThread mOnUiThread;
    private WebIconDatabase mIconDb;

    class WaitForIconClient extends WaitForProgressClient {
        private boolean mReceivedIcon;

        public WaitForIconClient(WebViewOnUiThread onUiThread) {
            super(onUiThread);
        }

        @Override
        public synchronized void onReceivedIcon(WebView webview, Bitmap icon) {
            mReceivedIcon = true;
        }

        public synchronized boolean receivedIcon() { return mReceivedIcon; }
    };

    public WebHistoryItemTest() {
        super("com.android.cts.stub", WebViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWebServer = new CtsTestServer(getActivity());
        mOnUiThread = new WebViewOnUiThread(this, getActivity().getWebView());
    }

    @Override
    protected void tearDown() throws Exception {
        mOnUiThread.cleanUp();
        mWebServer.shutdown();
        super.tearDown();
        if (mIconDb != null) {
            mIconDb.removeAllIcons();
            mIconDb.close();
        }
    }

    public void testWebHistoryItem() throws Throwable {
        final WaitForIconClient waitForIconClient = new WaitForIconClient(mOnUiThread);
        mOnUiThread.setWebChromeClient(waitForIconClient);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // getInstance must run on the UI thread
                mIconDb = WebIconDatabase.getInstance();
                String dbPath = getActivity().getFilesDir().toString() + "/icons";
                mIconDb.open(dbPath);
            }
        });

        WebBackForwardList list = mOnUiThread.copyBackForwardList();
        assertEquals(0, list.getSize());

        String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        new PollingCheck() {
            @Override
            protected boolean check() {
                return waitForIconClient.receivedIcon();
            }
        }.run();

        list = mOnUiThread.copyBackForwardList();
        assertEquals(1, list.getSize());
        WebHistoryItem item = list.getCurrentItem();
        assertNotNull(item);
        assertEquals(url, item.getUrl());
        assertEquals(url, item.getOriginalUrl());
        assertEquals(TestHtmlConstants.HELLO_WORLD_TITLE, item.getTitle());
        Bitmap icon = mOnUiThread.getFavicon();
        assertTrue(icon.sameAs(item.getFavicon()));

        url = mWebServer.getAssetUrl(TestHtmlConstants.BR_TAG_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        list = mOnUiThread.copyBackForwardList();
        assertEquals(2, list.getSize());
        item = list.getCurrentItem();
        assertNotNull(item);
        assertEquals(TestHtmlConstants.BR_TAG_TITLE, item.getTitle());
    }
}
