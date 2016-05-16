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

package android.content.cts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncAdapterType;
import android.os.Bundle;
import android.test.AndroidTestCase;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ContentResolverSyncTestCase extends AndroidTestCase {

    private static final String AUTHORITY = "android.content.cts.authority";

    private static final Account ACCOUNT = new Account(MockAccountAuthenticator.ACCOUNT_NAME,
            MockAccountAuthenticator.ACCOUNT_TYPE);

    private static final int LATCH_TIMEOUT_MS = 5000;

    private static AccountManager sAccountManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getMockSyncAdapter();
        sAccountManager = AccountManager.get(getContext());
    }

    @Override
    public void tearDown() throws Exception {
        getMockSyncAdapter().clearData();

        // Need to clean up created account
        removeAccount(sAccountManager, ACCOUNT, null /* callback */);

        // Need to cancel any sync that was started.
        cancelSync(null, AUTHORITY, LATCH_TIMEOUT_MS);

        super.tearDown();
    }

    public static synchronized MockSyncAdapter getMockSyncAdapter() {
        return MockSyncAdapter.getMockSyncAdapter();

    }

    public static synchronized MockAccountAuthenticator getMockAuthenticator(Context context) {
        return MockAccountAuthenticator.getMockAuthenticator(context);
    }

    private void addAccountExplicitly(Account account, String password, Bundle userdata) {
        assertTrue(sAccountManager.addAccountExplicitly(account, password, userdata));
    }

    private boolean removeAccount(AccountManager am, Account account,
            AccountManagerCallback<Boolean> callback) throws IOException, AuthenticatorException,
                OperationCanceledException {

        AccountManagerFuture<Boolean> futureBoolean = am.removeAccount(account,
                callback,
                null /* handler */);
        Boolean resultBoolean = futureBoolean.getResult();
        assertTrue(futureBoolean.isDone());

        return resultBoolean;
    }

    private CountDownLatch setNewLatch(CountDownLatch latch) {
        getMockSyncAdapter().clearData();
        getMockSyncAdapter().setLatch(latch);
        return latch;
    }

    private void addAccountAndVerifyInitSync(Account account, String password,
            String authority, int latchTimeoutMs, int accountIndex) {

        CountDownLatch latch = setNewLatch(new CountDownLatch(1));

        addAccountExplicitly(account, password, null /* userData */);

        // Wait with timeout for the callback to do its work
        try {
            if (!latch.await(latchTimeoutMs, TimeUnit.MILLISECONDS)) {
                fail("should not time out waiting on latch");
            }
        } catch (InterruptedException e) {
            fail("should not throw an InterruptedException");
        }

        assertFalse(getMockSyncAdapter().isStartSync());
        assertFalse(getMockSyncAdapter().isCancelSync());
        assertTrue(getMockSyncAdapter().isInitialized());
        assertEquals(account, getMockSyncAdapter().getAccounts().get(accountIndex));
        assertEquals(authority, getMockSyncAdapter().getAuthority());
    }

    private void cancelSync(Account account, String authority, int latchTimeoutMillis) {
        CountDownLatch latch = setNewLatch(new CountDownLatch(1));

        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

        ContentResolver.cancelSync(account, authority);

        // Wait with timeout for the callback to do its work
        try {
            latch.await(latchTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("should not throw an InterruptedException");
        }
    }

    private void requestSync(Account account, String authority, int latchTimeoutMillis) {
        CountDownLatch latch = setNewLatch(new CountDownLatch(1));

        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

        ContentResolver.requestSync(account, authority, extras);

        // Wait with timeout for the callback to do its work
        try {
            latch.await(latchTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("should not throw an InterruptedException");
        }
    }

    private void setIsSyncable(Account account, String authority, boolean b) {
        ContentResolver.setIsSyncable(account, authority, (b) ? 1 : 0);
    }

    /**
     * Test a sync request
     */
    public void testRequestSync() throws IOException, AuthenticatorException,
            OperationCanceledException {

        // Prevent auto sync
        ContentResolver.setMasterSyncAutomatically(false);
        assertEquals(false, ContentResolver.getMasterSyncAutomatically());

        addAccountAndVerifyInitSync(ACCOUNT,
                MockAccountAuthenticator.ACCOUNT_PASSWORD,
                AUTHORITY,
                LATCH_TIMEOUT_MS,
                0);

        getMockSyncAdapter().clearData();

        setIsSyncable(ACCOUNT, AUTHORITY, true);
        cancelSync(ACCOUNT, AUTHORITY, LATCH_TIMEOUT_MS);

        getMockSyncAdapter().clearData();

        requestSync(ACCOUNT, AUTHORITY, LATCH_TIMEOUT_MS);

        assertTrue(getMockSyncAdapter().isStartSync());
        assertFalse(getMockSyncAdapter().isCancelSync());
        assertFalse(getMockSyncAdapter().isInitialized());
        assertEquals(ACCOUNT, getMockSyncAdapter().getAccounts().get(0));
        assertEquals(AUTHORITY, getMockSyncAdapter().getAuthority());
    }

    /**
     * Test a sync cancel
     */
    public void testCancelSync() throws IOException, AuthenticatorException,
            OperationCanceledException {

        // Prevent auto sync
        ContentResolver.setMasterSyncAutomatically(false);
        assertEquals(false, ContentResolver.getMasterSyncAutomatically());

        addAccountAndVerifyInitSync(ACCOUNT,
                MockAccountAuthenticator.ACCOUNT_PASSWORD,
                AUTHORITY,
                LATCH_TIMEOUT_MS,
                0);

        getMockSyncAdapter().clearData();

        setIsSyncable(ACCOUNT, AUTHORITY, true);
        requestSync(ACCOUNT, AUTHORITY, LATCH_TIMEOUT_MS);

        getMockSyncAdapter().clearData();

        cancelSync(ACCOUNT, AUTHORITY, LATCH_TIMEOUT_MS);

        assertFalse(getMockSyncAdapter().isStartSync());
        assertTrue(getMockSyncAdapter().isCancelSync());
        assertFalse(getMockSyncAdapter().isInitialized());

        assertFalse(ContentResolver.isSyncActive(ACCOUNT, AUTHORITY));
        assertFalse(ContentResolver.isSyncPending(ACCOUNT, AUTHORITY));
    }

    /**
     * Test if we can set and get the MasterSyncAutomatically switch
     */
    public void testGetAndSetMasterSyncAutomatically() throws Exception {
        ContentResolver.setMasterSyncAutomatically(true);
        assertEquals(true, ContentResolver.getMasterSyncAutomatically());

        ContentResolver.setMasterSyncAutomatically(false);
        assertEquals(false, ContentResolver.getMasterSyncAutomatically());
        Thread.sleep(3000);
    }

    /**
     * Test if we can set and get the SyncAutomatically switch for an account
     */
    public void testGetAndSetSyncAutomatically() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        // Prevent auto sync
        ContentResolver.setMasterSyncAutomatically(false);
        assertEquals(false, ContentResolver.getMasterSyncAutomatically());

        ContentResolver.setSyncAutomatically(ACCOUNT, AUTHORITY, false);
        assertEquals(false, ContentResolver.getSyncAutomatically(ACCOUNT, AUTHORITY));

        ContentResolver.setSyncAutomatically(ACCOUNT, AUTHORITY, true);
        assertEquals(true, ContentResolver.getSyncAutomatically(ACCOUNT, AUTHORITY));
    }

    /**
     * Test if we can set and get the IsSyncable switch for an account
     */
    public void testGetAndSetIsSyncable() {
        // Prevent auto sync
        ContentResolver.setMasterSyncAutomatically(false);
        assertEquals(false, ContentResolver.getMasterSyncAutomatically());

        addAccountExplicitly(ACCOUNT, MockAccountAuthenticator.ACCOUNT_PASSWORD, null /* userData */);

        ContentResolver.setIsSyncable(ACCOUNT, AUTHORITY, 2);
        assertTrue(ContentResolver.getIsSyncable(ACCOUNT, AUTHORITY) > 0);

        ContentResolver.setIsSyncable(ACCOUNT, AUTHORITY, 1);
        assertTrue(ContentResolver.getIsSyncable(ACCOUNT, AUTHORITY) > 0);

        ContentResolver.setIsSyncable(ACCOUNT, AUTHORITY, 0);
        assertEquals(0, ContentResolver.getIsSyncable(ACCOUNT, AUTHORITY));

        ContentResolver.setIsSyncable(ACCOUNT, AUTHORITY, -1);
        assertTrue(ContentResolver.getIsSyncable(ACCOUNT, AUTHORITY) < 0);

        ContentResolver.setIsSyncable(ACCOUNT, AUTHORITY, -2);
        assertTrue(ContentResolver.getIsSyncable(ACCOUNT, AUTHORITY) < 0);
    }

    /**
     * Test if we can get the sync adapter types
     */
    public void testGetSyncAdapterTypes() {
        SyncAdapterType[] types = ContentResolver.getSyncAdapterTypes();
        assertNotNull(types);
        int length = types.length;
        assertTrue(length > 0);
        boolean found = false;
        for (int n=0; n < length; n++) {
            SyncAdapterType type = types[n];
            if (MockAccountAuthenticator.ACCOUNT_TYPE.equals(type.accountType) &&
                    AUTHORITY.equals(type.authority)) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    /**
     * Test if a badly formed sync request is throwing exceptions
     */
    public void testStartSyncFailure() {
        try {
            ContentResolver.requestSync(null, null, null);
            fail("did not throw IllegalArgumentException when extras is null.");
        } catch (IllegalArgumentException e) {
            //expected.
        }
    }

    /**
     * Test validate sync extra bundle
     */
    public void testValidateSyncExtrasBundle() {
        Bundle extras = new Bundle();
        extras.putInt("Integer", 20);
        extras.putLong("Long", 10l);
        extras.putBoolean("Boolean", true);
        extras.putFloat("Float", 5.5f);
        extras.putDouble("Double", 2.5);
        extras.putString("String", MockAccountAuthenticator.ACCOUNT_NAME);
        extras.putCharSequence("CharSequence", null);

        ContentResolver.validateSyncExtrasBundle(extras);

        extras.putChar("Char", 'a'); // type Char is invalid
        try {
            ContentResolver.validateSyncExtrasBundle(extras);
            fail("did not throw IllegalArgumentException when extras is invalide.");
        } catch (IllegalArgumentException e) {
            //expected.
        }
    }

    /**
     * Test to verify that a SyncAdapter is called on all the accounts accounts
     */
    public void testCallMultipleAccounts() {
        // Prevent auto sync
        ContentResolver.setMasterSyncAutomatically(false);
        assertEquals(false, ContentResolver.getMasterSyncAutomatically());

        addAccountAndVerifyInitSync(ACCOUNT,
                MockAccountAuthenticator.ACCOUNT_PASSWORD,
                AUTHORITY,
                LATCH_TIMEOUT_MS,
                0);

        getMockSyncAdapter().clearData();

        setIsSyncable(ACCOUNT, AUTHORITY, true);
        cancelSync(ACCOUNT, AUTHORITY, LATCH_TIMEOUT_MS);

        getMockSyncAdapter().clearData();

        requestSync(null /* all accounts */, AUTHORITY, LATCH_TIMEOUT_MS);

        assertTrue(getMockSyncAdapter().isStartSync());
        assertFalse(getMockSyncAdapter().isCancelSync());
        assertFalse(getMockSyncAdapter().isInitialized());
        assertEquals(ACCOUNT, getMockSyncAdapter().getAccounts().get(0));
        assertEquals(AUTHORITY, getMockSyncAdapter().getAuthority());

    }
}
