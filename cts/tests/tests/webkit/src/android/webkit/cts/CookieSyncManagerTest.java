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
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

public class CookieSyncManagerTest
        extends ActivityInstrumentationTestCase2<CookieSyncManagerStubActivity> {

    private final static int COOKIE_MANAGER_TIMEOUT = 5000;

    public CookieSyncManagerTest() {
        super("com.android.cts.stub", CookieSyncManagerStubActivity.class);
    }

    public void testCookieSyncManager() throws Exception {
        CookieSyncManager csm1 = CookieSyncManager.createInstance(getActivity());
        assertNotNull(csm1);

        CookieSyncManager csm2 = CookieSyncManager.getInstance();
        assertNotNull(csm2);

        assertSame(csm1, csm2);

        final CookieManager cookieManager = CookieManager.getInstance();

        // Remove all cookies from the database.
        cookieManager.removeAllCookie();
        new PollingCheck(COOKIE_MANAGER_TIMEOUT) {
            @Override
            protected boolean check() {
                return !cookieManager.hasCookies();
            }
        }.run();

        cookieManager.setAcceptCookie(true);
        assertTrue(cookieManager.acceptCookie());

        CtsTestServer server = new CtsTestServer(getActivity(), false);
        String url = server.getCookieUrl("conquest.html");
        String cookieValue = "a=b";
        cookieManager.setCookie(url, cookieValue);
        assertEquals(cookieValue, cookieManager.getCookie(url));

        // Store the cookie to the database.
        csm1.sync();
        new PollingCheck(COOKIE_MANAGER_TIMEOUT) {
            @Override
            protected boolean check() {
                return cookieManager.hasCookies();
            }
        }.run();

        // Remove all cookies from the database.
        cookieManager.removeAllCookie();
        new PollingCheck(COOKIE_MANAGER_TIMEOUT) {
            @Override
            protected boolean check() {
                return !cookieManager.hasCookies();
            }
        }.run();
    }
}
