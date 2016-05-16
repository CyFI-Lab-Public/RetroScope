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

package android.webkit.cts;


import android.content.Context;
import android.cts.util.PollingCheck;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebViewStartupTest
        extends ActivityInstrumentationTestCase2<WebViewStartupStubActivity> {

    private static final int TEST_TIMEOUT = 5000;
    private static final String TAG = "WebViewStartupTest";

    private WebViewStartupStubActivity mActivity;

    public WebViewStartupTest() {
        super("com.android.cts.stub", WebViewStartupStubActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        mActivity = getActivity();
    }

    @UiThreadTest
    public void testCookieManagerBlockingUiThread() throws Throwable {
        CtsTestServer server = new CtsTestServer(mActivity, false);
        final String url = server.getCookieUrl("death.html");

        Thread background = new Thread(new Runnable() {
            @Override
            public void run() {
                CookieSyncManager csm = CookieSyncManager.createInstance(mActivity);
                CookieManager cookieManager = CookieManager.getInstance();

                cookieManager.removeAllCookie();
                cookieManager.setAcceptCookie(true);
                cookieManager.setCookie(url, "count=41");
                Log.i(TAG, "done setting cookie before creating webview");
            }
        });
        background.start();
        background.join();

        // Now create WebView and test that setting the cookie beforehand really worked.
        mActivity.createAndAttachWebView();
        WebViewOnUiThread onUiThread = new WebViewOnUiThread(this, mActivity.getWebView());
        onUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals("1|count=41", onUiThread.getTitle()); // outgoing cookie
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie(url);
        assertNotNull(cookie);
        final Pattern pat = Pattern.compile("count=(\\d+)");
        Matcher m = pat.matcher(cookie);
        assertTrue(m.matches());
        assertEquals("42", m.group(1)); // value got incremented
    }

}
