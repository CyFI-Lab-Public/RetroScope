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
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

/**
 * Original framework tests for CookieManager
 */
public class CookieTest extends AndroidTestCase {

    private CookieManager mCookieManager;
    private static final long WAIT_TIME = 50;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CookieSyncManager.createInstance(getContext());
        mCookieManager = CookieManager.getInstance();
        mCookieManager.removeAllCookie();
        // cookies are removed asynchronously, wait a bit for cookies to be removed
        int maxWait = 10;
        for (int i=0; i < maxWait; i++) {
            // this is unfortuately non-deterministic, but ensure sleep a least once to limit
            // chance of remove thread running after test has begun
            Thread.sleep(WAIT_TIME);
            if (!mCookieManager.hasCookies()) {
                break;
            }
        }
        assertFalse(mCookieManager.hasCookies());
    }

    public void testDomain() {
        String url = "http://www.foo.com";

        // basic
        mCookieManager.setCookie(url, "a=b");
        String cookie = mCookieManager.getCookie(url);
        assertTrue(cookie.equals("a=b"));

        // no cross domain cookie
        cookie = mCookieManager.getCookie("http://bar.com");
        assertTrue(cookie == null);

        // more than one cookie
        mCookieManager.setCookie(url, "c=d; domain=.foo.com");
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie.contains("a=b;"));
        assertTrue(cookie.contains("c=d"));

        // host cookie should not be accessible from a sub-domain.
        cookie = mCookieManager.getCookie("http://bar.www.foo.com");
        assertTrue(cookie.equals("c=d"));

        // test setting a domain= that doesn't start w/ a dot, should
        // treat it as a domain cookie, as if there was a pre-pended dot.
        mCookieManager.setCookie(url, "e=f; domain=www.foo.com");
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie.contains("a=b"));
        assertTrue(cookie.contains("c=d"));
        assertTrue(cookie.contains("e=f"));

        cookie = mCookieManager.getCookie("http://sub.www.foo.com");
        assertTrue(cookie.contains("c=d"));
        assertTrue(cookie.contains("e=f"));

        cookie = mCookieManager.getCookie("http://foo.com");
        assertTrue(cookie.equals("c=d"));
    }

    public void testSubDomain() {
        String url_abcd = "http://a.b.c.d.com";
        String url_bcd = "http://b.c.d.com";
        String url_cd = "http://c.d.com";
        String url_d = "http://d.com";

        mCookieManager.setCookie(url_abcd, "a=1; domain=.a.b.c.d.com");
        mCookieManager.setCookie(url_abcd, "b=2; domain=.b.c.d.com");
        mCookieManager.setCookie(url_abcd, "c=3; domain=.c.d.com");
        mCookieManager.setCookie(url_abcd, "d=4; domain=.d.com");

        String cookie = mCookieManager.getCookie(url_abcd);
        assertTrue(cookie.contains("a=1"));
        assertTrue(cookie.contains("b=2"));
        assertTrue(cookie.contains("c=3"));
        assertTrue(cookie.contains("d=4"));
        cookie = mCookieManager.getCookie(url_bcd);
        assertTrue(cookie.contains("b=2"));
        assertTrue(cookie.contains("c=3"));
        assertTrue(cookie.contains("d=4"));
        cookie = mCookieManager.getCookie(url_cd);
        assertTrue(cookie.contains("c=3"));
        assertTrue(cookie.contains("d=4"));
        cookie = mCookieManager.getCookie(url_d);
        assertTrue(cookie.equals("d=4"));

        // check that the same cookie can exist on different sub-domains.
        mCookieManager.setCookie(url_bcd, "x=bcd; domain=.b.c.d.com");
        mCookieManager.setCookie(url_bcd, "x=cd; domain=.c.d.com");
        cookie = mCookieManager.getCookie(url_bcd);
        assertTrue(cookie.contains("b=2"));
        assertTrue(cookie.contains("c=3"));
        assertTrue(cookie.contains("d=4"));
        assertTrue(cookie.contains("x=bcd"));
        assertTrue(cookie.contains("x=cd"));
        cookie = mCookieManager.getCookie(url_cd);
        assertTrue(cookie.contains("c=3"));
        assertTrue(cookie.contains("d=4"));
        assertTrue(cookie.contains("x=cd"));
    }

    public void testInvalidDomain() {
        String url = "http://foo.bar.com";

        mCookieManager.setCookie(url, "a=1; domain=.yo.foo.bar.com");
        String cookie = mCookieManager.getCookie(url);
        assertTrue(cookie == null);

        mCookieManager.setCookie(url, "b=2; domain=.foo.com");
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie == null);

        mCookieManager.setCookie(url, "c=3; domain=.bar.foo.com");
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie == null);

        mCookieManager.setCookie(url, "d=4; domain=.foo.bar.com.net");
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie == null);

        mCookieManager.setCookie(url, "e=5; domain=.ar.com");
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie == null);

        mCookieManager.setCookie(url, "f=6; domain=.com");
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie == null);

        mCookieManager.setCookie(url, "g=7; domain=.co.uk");
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie == null);

        mCookieManager.setCookie(url, "h=8; domain=.foo.bar.com.com");
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie == null);
    }

    public void testPath() {
        String url = "http://www.foo.com";

        mCookieManager.setCookie(url, "a=b; path=/wee");
        String cookie = mCookieManager.getCookie(url + "/wee");
        assertTrue(cookie.equals("a=b"));
        cookie = mCookieManager.getCookie(url + "/wee/");
        assertTrue(cookie.equals("a=b"));
        cookie = mCookieManager.getCookie(url + "/wee/hee");
        assertTrue(cookie.equals("a=b"));
        cookie = mCookieManager.getCookie(url + "/wee/hee/more");
        assertTrue(cookie.equals("a=b"));
        cookie = mCookieManager.getCookie(url + "/weehee");
        assertTrue(cookie == null);
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie == null);

        mCookieManager.setCookie(url, "a=c; path=");
        cookie = mCookieManager.getCookie(url + "/wee");
        // order of contents matters in this case, per spec
        assertTrue(cookie.equals("a=b; a=c"));
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie.equals("a=c"));

        mCookieManager.setCookie(url, "a=d");
        cookie = mCookieManager.getCookie(url + "/wee");
        assertTrue(cookie.equals("a=b; a=d"));
    }

    public void testEmptyValue() {
        String url = "http://www.foobar.com";

        mCookieManager.setCookie(url, "bar=");
        String cookie = mCookieManager.getCookie(url);
        assertTrue(cookie.equals("bar="));

        mCookieManager.setCookie(url, "foobar=;");
        cookie = mCookieManager.getCookie(url);
        assertTrue(cookie.equals("bar=; foobar="));

        mCookieManager.setCookie(url, "baz=; path=/wee");
        cookie = mCookieManager.getCookie(url + "/wee");
        assertTrue(cookie.equals("baz=; bar=; foobar="));
    }
}
