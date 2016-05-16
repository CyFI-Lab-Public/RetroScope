package com.xtremelabs.robolectric.shadows;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.internal.RealObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.xtremelabs.robolectric.Robolectric.newInstanceOf;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

/**
 * Shadows the {@code android.accounts.AccountManager} class.
 */
@SuppressWarnings({"UnusedDeclaration"})
@Implements(AccountManager.class)
public class ShadowAccountManager {

    public static final String AUTH_TOKEN_VALUE = "authToken";

    private static AccountManager singleton;

    private Account[] accounts;
    private HashMap<Account, HashMap<String, String>> cachedAuthTokenValues =
            new HashMap<Account, HashMap<String, String>>();

    @Implementation
    public static AccountManager get(Context context) {
        if (singleton == null) {
            singleton = Robolectric.newInstanceOf(AccountManager.class);
        }
        return singleton;
    }

    @Implementation
    public AccountManagerFuture<Bundle> getAuthToken(Account account, String authTokenType, Bundle options, Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
        //TODO: Add complete activity to perform the account intent dance.
        final Account finalAccount = account;
        return new AccountManagerFuture<Bundle>() {

            private boolean isFutureCancelled;
            private boolean isFutureDone;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (isFutureDone) {
                    return false;
                }
                isFutureCancelled = true;
                return isCancelled();
            }

            @Override
            public Bundle getResult(long timeout, TimeUnit unit) throws OperationCanceledException,
                    AuthenticatorException, IOException {
                Bundle result = new Bundle();
                if (!isCancelled()) {
                    addBundleResults(result, finalAccount);
                    isFutureDone = true;
                }
                return result;
            }

            @Override
            public Bundle getResult() throws OperationCanceledException,
                    AuthenticatorException, IOException {
                Bundle result = new Bundle();
                if (!isCancelled()) {
                    addBundleResults(result, finalAccount);
                    isFutureDone = true;
                }
                return result;
            }

            @Override
            public boolean isCancelled() {
                return isFutureCancelled;
            }

            @Override
            public boolean isDone() {
                return isFutureDone || isFutureCancelled;
            }

            private void addBundleResults(Bundle bundle, final Account account) {
                bundle.putString(AccountManager.KEY_AUTHTOKEN, AUTH_TOKEN_VALUE);
                bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            }
        };
    }

    @Implementation
    public AccountManagerFuture<Bundle> getAuthTokenByFeatures(String accountType, String authTokenType, String[] features, Activity activity, Bundle addAccountOptions, Bundle getAuthTokenOptions, AccountManagerCallback<Bundle> callback, Handler handler) {
        //TODO: Add complete activity to perform the account intent dance.
        final String finalAccountType = accountType;
        return new AccountManagerFuture<Bundle>() {

            private boolean isFutureCancelled;
            private boolean isFutureDone;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (isFutureDone) {
                    return false;
                }
                isFutureCancelled = true;
                return isCancelled();
            }

            @Override
            public Bundle getResult(long timeout, TimeUnit unit) throws OperationCanceledException,
                    AuthenticatorException, IOException {
                Bundle result = new Bundle();
                if (!isCancelled()) {
                    addBundleResults(result, finalAccountType);
                    isFutureDone = true;
                }
                return result;
            }

            @Override
            public Bundle getResult() throws OperationCanceledException,
                    AuthenticatorException, IOException {
                Bundle result = new Bundle();
                if (!isCancelled()) {
                    addBundleResults(result, finalAccountType);
                    isFutureDone = true;
                }
                return result;
            }

            @Override
            public boolean isCancelled() {
                return isFutureCancelled;
            }

            @Override
            public boolean isDone() {
                return isFutureDone || isFutureCancelled;
            }

            private void addBundleResults(Bundle bundle, final String accountType) {
                bundle.putString(AccountManager.KEY_AUTHTOKEN, AUTH_TOKEN_VALUE);
                bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                bundle.putString(AccountManager.KEY_ACCOUNT_NAME, "accountName");
            }
        };
    }

    @Implementation
    public void invalidateAuthToken(String accountType, String authToken) {}

    @Implementation
    public Account[] getAccounts() {
        return getAccountsByType(null);
    }

    @Implementation
    public Account[] getAccountsByType(String accountType) {
        if (accountType == null) {
            return accounts;
        }

        ArrayList<Account> accountList = new ArrayList<Account>();
        if (accounts != null) {
            for (Account account : accounts) {
                if (accountType.equals(account.type)) {
                    accountList.add(account);
                }
            }
        }
        return accountList.toArray(new Account[accountList.size()]);
    }

    @Implementation
    public String peekAuthToken(Account account, String authTokenType) {
        HashMap<String, String> tokens = cachedAuthTokenValues.get(account);
        return (tokens != null) ? tokens.get(authTokenType) : null;
    }

    public void setCachedAuthToken(Account account, String authTokenType, String authTokenValue) {
        if (!cachedAuthTokenValues.containsKey(account)) {
            cachedAuthTokenValues.put(account, new HashMap<String, String>());
        }
        cachedAuthTokenValues.get(account).put(authTokenType, authTokenValue);
    }

    public void setAccounts(Account[] accounts) {
        this.accounts = accounts;
    }
}
