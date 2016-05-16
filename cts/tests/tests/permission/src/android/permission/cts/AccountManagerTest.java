/*
 * Copyright (C) 2013 The Android Open Source Project
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
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Verify GET_ACCOUNTS permissions are enforced.
 */
public class AccountManagerTest extends AndroidTestCase {

    private AccountManager mAccountManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mAccountManager = AccountManager.get(getContext());
        assertNotNull(mAccountManager);
    }

    /**
     * Verifies that AccountManager.getAccounts() requires Permission.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#GET_ACCOUNTS}.
     */
    @SmallTest
    public void testGetAccounts() {
        try {
            mAccountManager.getAccounts();
            fail("AccountManager.getAccounts() did not throw SecurityException as expected");
        } catch (SecurityException se) {
            // Expected Exception
        }
    }

    /**
     * Verifies that AccountManager.getAccountsByType() requires Permission.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#GET_ACCOUNTS}.
     */
    @SmallTest
    public void testGetAccountsByType() {
        try {
            mAccountManager.getAccountsByType(null);
            fail("AccountManager.getAccountsByType() did not throw SecurityException as expected");
        } catch (SecurityException se) {
            // Expected Exception
        }
    }

    /**
     * Verifies that AccountManager.getAccountsByTypeAndFeatures() requires
     * Permission.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#GET_ACCOUNTS}.
     */
    @SmallTest
    public void testGetAccountsByTypeAndFeatures() {
        try {
            mAccountManager.getAccountsByTypeAndFeatures("", null, null, null);
            fail("AccountManager.getAccountsByTypeAndFeatures() did not throw SecurityException as expected");
        } catch (SecurityException se) {
            // Expected Exception
        }
    }
}
