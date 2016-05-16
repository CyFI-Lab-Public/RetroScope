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

package android.permission.cts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import java.net.InetAddress;

/**
 * Verify ConnectivityManager related methods without specific network state permissions.
 */
public class NoNetworkStatePermissionTest extends AndroidTestCase {
    private ConnectivityManager mConnectivityManager;
    private static final int TEST_NETWORK_TYPE = 1;
    private static final int TEST_PREFERENCE = 1;
    private static final String TEST_FEATURE = "feature";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        assertNotNull(mConnectivityManager);
    }

    /**
     * Verify that ConnectivityManager#getNetworkPreference() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#ACCESS_NETWORK_STATE}.
     */
    @SmallTest
    public void testGetNetworkPreference() {
        try {
            mConnectivityManager.getNetworkPreference();
            fail("ConnectivityManager.getNetworkPreference didn't throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that ConnectivityManager#getActiveNetworkInfo() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#ACCESS_NETWORK_STATE}.
     */
    @SmallTest
    public void testGetActiveNetworkInfo() {
        try {
            mConnectivityManager.getActiveNetworkInfo();
            fail("ConnectivityManager.getActiveNetworkInfo didn't throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that ConnectivityManager#getNetworkInfo() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#ACCESS_NETWORK_STATE}.
     */
    @SmallTest
    public void testGetNetworkInfo() {
        try {
            mConnectivityManager.getNetworkInfo(TEST_NETWORK_TYPE);
            fail("ConnectivityManager.getNetworkInfo didn't throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that ConnectivityManager#getAllNetworkInfo() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#ACCESS_NETWORK_STATE}.
     */
    @SmallTest
    public void testGetAllNetworkInfo() {
        try {
            mConnectivityManager.getAllNetworkInfo();
            fail("ConnectivityManager.getAllNetworkInfo didn't throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that ConnectivityManager#setNetworkPreference() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_NETWORK_STATE}.
     */
    @SmallTest
    public void testSetNetworkPreference() {
        try {
            mConnectivityManager.setNetworkPreference(TEST_PREFERENCE);
            fail("ConnectivityManager.setNetworkPreference didn't throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that ConnectivityManager#startUsingNetworkFeature() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_NETWORK_STATE}.
     */
    @SmallTest
    public void testStartUsingNetworkFeature() {
        try {
            mConnectivityManager.startUsingNetworkFeature(TEST_NETWORK_TYPE, TEST_FEATURE);
            fail("ConnectivityManager.startUsingNetworkFeature didn't throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that ConnectivityManager#stopUsingNetworkFeature() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_NETWORK_STATE}.
     */
    @SmallTest
    public void testStopUsingNetworkFeature() {
        try {
            mConnectivityManager.stopUsingNetworkFeature(TEST_NETWORK_TYPE, TEST_FEATURE);
            fail("ConnectivityManager.stopUsingNetworkFeature didn't throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that ConnectivityManager#requestRouteToHost() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#CHANGE_NETWORK_STATE}.
     */
    @SmallTest
    public void testRequestRouteToHost() {
        try {
            mConnectivityManager.requestRouteToHost(TEST_NETWORK_TYPE, 0xffffffff);
            fail("ConnectivityManager.requestRouteToHost didn't throw SecurityException as"
                    + " expected");
        } catch (SecurityException e) {
            // expected
        }
    }

    @SmallTest
    public void testSecurityExceptionFromDns() throws Exception {
        try {
            InetAddress.getByName("www.google.com");
            fail();
        } catch (SecurityException expected) {
        }
    }
}
