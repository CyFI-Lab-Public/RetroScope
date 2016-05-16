package com.xtremelabs.robolectric.shadows;

import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.xtremelabs.robolectric.WithTestDefaultsRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;

@RunWith(WithTestDefaultsRunner.class)
public class AccountManagerTest {

    private static final Account ACCOUNT = new Account("name", "type");

    private AccountManager accountManager;
    private Activity activity;
    private String accountType;
    private String authTokenType;
    private String[] features;

    @Before
    public void setUp() {
        activity = new Activity();
        accountManager = AccountManager.get(activity);
        accountManager.invalidateAuthToken(null, null);
        accountType = "accountType";
        authTokenType = "authTokenType";
        features = new String[]{};
    }

    @Test
    public void testGetAuthTokenByFeatures_isCancelled() throws Exception {
        AccountManagerFuture<Bundle> future =
                accountManager.getAuthTokenByFeatures(accountType, authTokenType, features, activity, null, null, null, null);

        assertThat(future.isCancelled(), equalTo(false));
        future.cancel(true);
        assertThat(future.isCancelled(), equalTo(true));
    }

    @Test
    public void testGetAuthTokenByFeatures_isDoneWithCancel() throws Exception {
        AccountManagerFuture<Bundle> future =
                accountManager.getAuthTokenByFeatures(accountType, authTokenType, features, activity, null, null, null, null);

        assertThat(future.isDone(), equalTo(false));
        future.cancel(true);
        assertThat(future.isDone(), equalTo(true));
    }

    @Test
    public void testGetAuthTokenByFeatures_isDoneWithGetResult() throws Exception {
        AccountManagerFuture<Bundle> future =
                accountManager.getAuthTokenByFeatures(accountType, authTokenType, features, activity, null, null, null, null);

        assertThat(future.isDone(), equalTo(false));
        future.getResult();
        assertThat(future.isDone(), equalTo(true));
    }

    @Test
    public void testInvalidateAuthToken() throws Exception {
        // Check that it doesn't crash
        accountManager.invalidateAuthToken(accountType, null);
    }

    @Test
    public void testGetAuthTokenByFeatures_getResult() throws Exception {
        AccountManagerFuture<Bundle> future =
                accountManager.getAuthTokenByFeatures(accountType, authTokenType, features, activity, null, null, null, null);

        Bundle result = future.getResult();
        assertThat(result.containsKey(AccountManager.KEY_AUTHTOKEN), equalTo(true));
        assertThat(result.containsKey(AccountManager.KEY_ACCOUNT_NAME), equalTo(true));
        assertThat(result.containsKey(AccountManager.KEY_ACCOUNT_TYPE), equalTo(true));
    }

    @Test
    public void testGetAuthToken_getResult() throws Exception {
        AccountManagerFuture<Bundle> future =
                accountManager.getAuthToken(ACCOUNT, authTokenType, null, activity, null, null);

        Bundle result = future.getResult();
        assertThat(ACCOUNT.name, equalTo(result.getString(AccountManager.KEY_ACCOUNT_NAME)));
        assertThat(ACCOUNT.type, equalTo(result.getString(AccountManager.KEY_ACCOUNT_TYPE)));
    }

    @Test
    public void testAccountManagerIsSingleton() throws Exception {
        AccountManager ref = AccountManager.get(activity);

        assertTrue(ref == accountManager);
        assertThat(ref, equalTo(accountManager));
    }

    @Test
    public void testGetAccounts() throws Exception {
        Account[] origAccounts = new Account[] { ACCOUNT, ACCOUNT };
        shadowOf(accountManager).setAccounts(origAccounts);

        Account[] accounts = accountManager.getAccounts();

        assertArrayEquals(origAccounts, accounts);
    }

    @Test
    public void testGetAccountsByType() throws Exception {
        Account diffAccount = new Account("myName", "myType");
        Account[] origAccounts = new Account[] { ACCOUNT, diffAccount };
        shadowOf(accountManager).setAccounts(origAccounts);

        Account[] accounts = accountManager.getAccountsByType("myType");

        assertThat(accounts.length, equalTo(1));
        assertThat(accounts[0], equalTo(diffAccount));
    }

    @Test
    public void testPeek() throws Exception {
        shadowOf(accountManager).setCachedAuthToken(ACCOUNT, authTokenType, "myToken");

        String authToken = accountManager.peekAuthToken(ACCOUNT, authTokenType);

        assertThat(authToken, equalTo("myToken"));
    }

    @Test
    public void testPeek_differentAccount() throws Exception {
        shadowOf(accountManager).setCachedAuthToken(ACCOUNT, authTokenType, "myToken");

        String authToken =
                accountManager.peekAuthToken(new Account("other", "type"), authTokenType);

        assertNull(authToken);
    }

    @Test
    public void testPeek_differentAuthTokenType() throws Exception {
        shadowOf(accountManager).setCachedAuthToken(ACCOUNT, authTokenType, "myToken");

        String authToken = accountManager.peekAuthToken(ACCOUNT, "other");

        assertNull(authToken);
    }
}
