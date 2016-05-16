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
import android.test.ActivityInstrumentationTestCase2;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;


import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CookieManagerTest extends
        ActivityInstrumentationTestCase2<CookieSyncManagerStubActivity> {

    private static final int TEST_TIMEOUT = 5000;

    private WebViewOnUiThread mOnUiThread;
    private CookieManager mCookieManager;

    public CookieManagerTest() {
        super("com.android.cts.stub", CookieSyncManagerStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mOnUiThread = new WebViewOnUiThread(this, getActivity().getWebView());

        mCookieManager = CookieManager.getInstance();
        assertNotNull(mCookieManager);
    }

    public void testGetInstance() {
        mOnUiThread.cleanUp();
        CookieManager c1 = CookieManager.getInstance();
        CookieManager c2 = CookieManager.getInstance();

        assertSame(c1, c2);
    }

    public void testClone() {
    }

    public void testAcceptCookie() throws Exception {
        mCookieManager.removeAllCookie();
        mCookieManager.setAcceptCookie(false);
        assertFalse(mCookieManager.acceptCookie());
        assertFalse(mCookieManager.hasCookies());

        CtsTestServer server = new CtsTestServer(getActivity(), false);
        String url = server.getCookieUrl("conquest.html");
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals("0", mOnUiThread.getTitle()); // no cookies passed
        Thread.sleep(500);
        assertNull(mCookieManager.getCookie(url));

        mCookieManager.setAcceptCookie(true);
        assertTrue(mCookieManager.acceptCookie());

        url = server.getCookieUrl("war.html");
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals("0", mOnUiThread.getTitle()); // no cookies passed
        waitForCookie(url);
        String cookie = mCookieManager.getCookie(url);
        assertNotNull(cookie);
        // 'count' value of the returned cookie is 0
        final Pattern pat = Pattern.compile("count=(\\d+)");
        Matcher m = pat.matcher(cookie);
        assertTrue(m.matches());
        assertEquals("0", m.group(1));

        url = server.getCookieUrl("famine.html");
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals("1|count=0", mOnUiThread.getTitle()); // outgoing cookie
        waitForCookie(url);
        cookie = mCookieManager.getCookie(url);
        assertNotNull(cookie);
        m = pat.matcher(cookie);
        assertTrue(m.matches());
        assertEquals("1", m.group(1)); // value got incremented

        url = server.getCookieUrl("death.html");
        mCookieManager.setCookie(url, "count=41");
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals("1|count=41", mOnUiThread.getTitle()); // outgoing cookie
        waitForCookie(url);
        cookie = mCookieManager.getCookie(url);
        assertNotNull(cookie);
        m = pat.matcher(cookie);
        assertTrue(m.matches());
        assertEquals("42", m.group(1)); // value got incremented

        // clean up all cookies
        mCookieManager.removeAllCookie();
    }

    public void testCookieManager() {
        // enable cookie
        mCookieManager.setAcceptCookie(true);
        assertTrue(mCookieManager.acceptCookie());

        // first there should be no cookie stored
        assertFalse(mCookieManager.hasCookies());

        String url = "http://www.example.com";
        String cookie = "name=test";
        mCookieManager.setCookie(url, cookie);
        assertEquals(cookie, mCookieManager.getCookie(url));

        // sync cookie from RAM to FLASH, because hasCookies() only counts FLASH cookies
        CookieSyncManager.getInstance().sync();
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return mCookieManager.hasCookies();
            }
        }.run();

        // clean up all cookies
        mCookieManager.removeAllCookie();
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return !mCookieManager.hasCookies();
            }
        }.run();
    }

    @SuppressWarnings("deprecation")
    public void testRemoveCookies() throws InterruptedException {
        // enable cookie
        mCookieManager.setAcceptCookie(true);
        assertTrue(mCookieManager.acceptCookie());
        assertFalse(mCookieManager.hasCookies());

        final String url = "http://www.example.com";
        final String cookie1 = "cookie1=peter";
        final String cookie2 = "cookie2=sue";
        final String cookie3 = "cookie3=marc";

        mCookieManager.setCookie(url, cookie1); // session cookie

        Date date = new Date();
        date.setTime(date.getTime() + 1000 * 600);
        String value2 = cookie2 + "; expires=" + date.toGMTString();
        mCookieManager.setCookie(url, value2); // expires in 10min

        long expiration = 3000;
        date = new Date();
        date.setTime(date.getTime() + expiration);
        String value3 = cookie3 + "; expires=" + date.toGMTString();
        mCookieManager.setCookie(url, value3); // expires in 3s

        String allCookies = mCookieManager.getCookie(url);
        assertTrue(allCookies.contains(cookie1));
        assertTrue(allCookies.contains(cookie2));
        assertTrue(allCookies.contains(cookie3));

        mCookieManager.removeSessionCookie();
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                String c = mCookieManager.getCookie(url);
                return !c.contains(cookie1) && c.contains(cookie2) && c.contains(cookie3);
            }
        }.run();

        Thread.sleep(expiration + 1000); // wait for cookie to expire
        mCookieManager.removeExpiredCookie();
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                String c = mCookieManager.getCookie(url);
                return !c.contains(cookie1) && c.contains(cookie2) && !c.contains(cookie3);
            }
        }.run();

        mCookieManager.removeAllCookie();
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return mCookieManager.getCookie(url) == null;
            }
        }.run();
    }

    private void waitForCookie(final String url) {
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return mCookieManager.getCookie(url) != null;
            }
        }.run();
    }

    public void testb3167208() throws Exception {
        String uri = "http://host.android.com/path/";
        // note the space after the domain=
        String problemCookie = "foo=bar; domain= .android.com; path=/";
        mCookieManager.setCookie(uri, problemCookie);
        String cookie = mCookieManager.getCookie(uri);
        assertNotNull(cookie);
        assertTrue(cookie.contains("foo=bar"));
    }
}
