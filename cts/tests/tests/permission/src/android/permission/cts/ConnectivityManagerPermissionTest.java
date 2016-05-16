/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.permission.cts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
* Test that protected android.net.ConnectivityManager methods cannot be called without
* permissions
*/
public class ConnectivityManagerPermissionTest extends AndroidTestCase {

    private ConnectivityManager mConnectivityManager = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        assertNotNull(mConnectivityManager);
    }



    /**
     * Verify that calling {@link ConnectivityManager#getNetworkInfo(int))}
     * requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#ACCESS_NETWORK_STATE}.
     */
    @SmallTest
    public void testGetNetworkInfo() {
        try {
            mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            fail("Was able to call getNetworkInfo");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that calling {@link ConnectivityManager#getNetworkPreference()}
     * requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#ACCESS_NETWORK_STATE}.
     */
    @SmallTest
    public void testGetNetworkPreference() {
        try {
            mConnectivityManager.getNetworkPreference();
            fail("Was able to call getNetworkPreference");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that calling {@link ConnectivityManager#requestRouteToHost(int, int)}
     * requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#CHANGE_NETWORK_STATE}.
     */
    @SmallTest
    public void testRequestRouteToHost() {
        try {
            mConnectivityManager.requestRouteToHost(ConnectivityManager.TYPE_MOBILE, 1);
            fail("Was able to call requestRouteToHost");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that calling {@link ConnectivityManager#setNetworkPreference(int)}
     * requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#CHANGE_NETWORK_STATE}.
     */
    @SmallTest
    public void testSetNetworkPreference() {
        try {
            mConnectivityManager.setNetworkPreference(ConnectivityManager.TYPE_MOBILE);
            fail("Was able to call setNetworkPreference");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that calling {@link ConnectivityManager#setNetworkPreference(int)}
     * requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#CHANGE_NETWORK_STATE}.
     */
    @SmallTest
    public void testStartUsingNetworkPreference() {
        try {
            mConnectivityManager.setNetworkPreference(ConnectivityManager.TYPE_MOBILE);
            fail("Was able to call setNetworkPreference");
        } catch (SecurityException e) {
            // expected
        }
    }
}

