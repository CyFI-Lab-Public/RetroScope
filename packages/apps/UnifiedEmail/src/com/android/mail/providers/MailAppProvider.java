/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.providers;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.Loader.OnLoadCompleteListener;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

import com.android.mail.R;
import com.android.mail.providers.UIProvider.AccountCursorExtraKeys;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.MatrixCursorWithExtra;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The Mail App provider allows email providers to register "accounts" and the UI has a single
 * place to query for the list of accounts.
 *
 * During development this will allow new account types to be added, and allow them to be shown in
 * the application.  For example, the mock accounts can be enabled/disabled.
 * In the future, once other processes can add new accounts, this could allow other "mail"
 * applications have their content appear within the application
 */
public abstract class MailAppProvider extends ContentProvider
        implements OnLoadCompleteListener<Cursor>{

    private static final String SHARED_PREFERENCES_NAME = "MailAppProvider";
    private static final String ACCOUNT_LIST_KEY = "accountList";
    private static final String LAST_VIEWED_ACCOUNT_KEY = "lastViewedAccount";
    private static final String LAST_SENT_FROM_ACCOUNT_KEY = "lastSendFromAccount";

    /**
     * Extra used in the result from the activity launched by the intent specified
     * by {@link #getNoAccountsIntent} to return the list of accounts.  The data
     * specified by this extra key should be a ParcelableArray.
     */
    public static final String ADD_ACCOUNT_RESULT_ACCOUNTS_EXTRA = "addAccountResultAccounts";

    private final static String LOG_TAG = LogTag.getLogTag();

    private final LinkedHashMap<Uri, AccountCacheEntry> mAccountCache =
            new LinkedHashMap<Uri, AccountCacheEntry>();

    private final Map<Uri, CursorLoader> mCursorLoaderMap = Maps.newHashMap();

    private ContentResolver mResolver;
    private static String sAuthority;
    private static MailAppProvider sInstance;

    private volatile boolean mAccountsFullyLoaded = false;

    private SharedPreferences mSharedPrefs;

    /**
     * Allows the implementing provider to specify the authority for this provider. Email and Gmail
     * must specify different authorities.
     */
    protected abstract String getAuthority();

    /**
     * Authority for the suggestions provider. Email and Gmail must specify different authorities,
     * much like the implementation of {@link #getAuthority()}.
     * @return the suggestion authority associated with this provider.
     */
    public abstract String getSuggestionAuthority();

    /**
     * Allows the implementing provider to specify an intent that should be used in a call to
     * {@link Context#startActivityForResult(android.content.Intent)} when the account provider
     * doesn't return any accounts.
     *
     * The result from the {@link Activity} activity should include the list of accounts in
     * the returned intent, in the

     * @return Intent or null, if the provider doesn't specify a behavior when no accounts are
     * specified.
     */
    protected abstract Intent getNoAccountsIntent(Context context);

    /**
     * The cursor returned from a call to {@link android.content.ContentResolver#query()} with this
     * uri will return a cursor that with columns that are a subset of the columns specified
     * in {@link UIProvider.ConversationColumns}
     * The cursor returned by this query can return a {@link android.os.Bundle}
     * from a call to {@link android.database.Cursor#getExtras()}.  This Bundle may have
     * values with keys listed in {@link AccountCursorExtraKeys}
     */
    public static Uri getAccountsUri() {
        return Uri.parse("content://" + sAuthority + "/");
    }

    public static MailAppProvider getInstance() {
        return sInstance;
    }

    /** Default constructor */
    protected MailAppProvider() {
    }

    @Override
    public boolean onCreate() {
        sAuthority = getAuthority();
        sInstance = this;
        mResolver = getContext().getContentResolver();

        // Load the previously saved account list
        loadCachedAccountList();

        final Resources res = getContext().getResources();
        // Load the uris for the account list
        final String[] accountQueryUris = res.getStringArray(R.array.account_providers);

        for (String accountQueryUri : accountQueryUris) {
            final Uri uri = Uri.parse(accountQueryUri);
            addAccountsForUriAsync(uri);
        }

        return true;
    }

    @Override
    public void shutdown() {
        sInstance = null;

        for (CursorLoader loader : mCursorLoaderMap.values()) {
            loader.stopLoading();
        }
        mCursorLoaderMap.clear();
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // This content provider currently only supports one query (to return the list of accounts).
        // No reason to check the uri.  Currently only checking the projections

        // Validates and returns the projection that should be used.
        final String[] resultProjection = UIProviderValidator.validateAccountProjection(projection);
        final Bundle extras = new Bundle();
        extras.putInt(AccountCursorExtraKeys.ACCOUNTS_LOADED, mAccountsFullyLoaded ? 1 : 0);

        // Make a copy of the account cache
        final List<AccountCacheEntry> accountList;
        synchronized (mAccountCache) {
            accountList = ImmutableList.copyOf(mAccountCache.values());
        }

        final MatrixCursor cursor =
                new MatrixCursorWithExtra(resultProjection, accountList.size(), extras);

        for (AccountCacheEntry accountEntry : accountList) {
            final Account account = accountEntry.mAccount;
            final MatrixCursor.RowBuilder builder = cursor.newRow();
            final Map<String, Object> accountValues = account.getValueMap();

            for (final String columnName : resultProjection) {
                if (accountValues.containsKey(columnName)) {
                    builder.add(accountValues.get(columnName));
                } else {
                    throw new IllegalStateException("Unexpected column: " + columnName);
                }
            }
        }

        cursor.setNotificationUri(mResolver, getAccountsUri());
        return cursor;
    }

    @Override
    public Uri insert(Uri url, ContentValues values) {
        return url;
    }

    @Override
    public int update(Uri url, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri url, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    /**
     * Asynchronously adds all of the accounts that are specified by the result set returned by
     * {@link ContentProvider#query()} for the specified uri.  The content provider handling the
     * query needs to handle the {@link UIProvider.ACCOUNTS_PROJECTION}
     * Any changes to the underlying provider will automatically be reflected.
     * @param accountsQueryUri
     */
    private void addAccountsForUriAsync(Uri accountsQueryUri) {
        startAccountsLoader(accountsQueryUri);
    }

    /**
     * Returns the intent that should be used in a call to
     * {@link Context#startActivity(android.content.Intent)} when the account provider doesn't
     * return any accounts
     * @return Intent or null, if the provider doesn't specify a behavior when no acccounts are
     * specified.
     */
    public static Intent getNoAccountIntent(Context context) {
        return getInstance().getNoAccountsIntent(context);
    }

    private synchronized void startAccountsLoader(Uri accountsQueryUri) {
        final CursorLoader accountsCursorLoader = new CursorLoader(getContext(), accountsQueryUri,
                UIProvider.ACCOUNTS_PROJECTION, null, null, null);

        // Listen for the results
        accountsCursorLoader.registerListener(accountsQueryUri.hashCode(), this);
        accountsCursorLoader.startLoading();

        // If there is a previous loader for the given uri, stop it
        final CursorLoader oldLoader = mCursorLoaderMap.get(accountsQueryUri);
        if (oldLoader != null) {
            oldLoader.stopLoading();
        }
        mCursorLoaderMap.put(accountsQueryUri, accountsCursorLoader);
    }

    private void addAccountImpl(Account account, Uri accountsQueryUri, boolean notify) {
        addAccountImpl(account.uri, new AccountCacheEntry(account, accountsQueryUri));

        // Explicitly calling this out of the synchronized block in case any of the observers get
        // called synchronously.
        if (notify) {
            broadcastAccountChange();
        }
    }

    private void addAccountImpl(Uri key, AccountCacheEntry accountEntry) {
        synchronized (mAccountCache) {
            LogUtils.v(LOG_TAG, "adding account %s", accountEntry.mAccount);
            // LinkedHashMap will not change the iteration order when re-inserting a key
            mAccountCache.put(key, accountEntry);
        }
    }

    private static void broadcastAccountChange() {
        final MailAppProvider provider = sInstance;

        if (provider != null) {
            provider.mResolver.notifyChange(getAccountsUri(), null);
        }
    }

    /**
     * Returns the {@link Account#uri} (in String form) of the last viewed account.
     */
    public String getLastViewedAccount() {
        return getPreferences().getString(LAST_VIEWED_ACCOUNT_KEY, null);
    }

    /**
     * Persists the {@link Account#uri} (in String form) of the last viewed account.
     */
    public void setLastViewedAccount(String accountUriStr) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(LAST_VIEWED_ACCOUNT_KEY, accountUriStr);
        editor.apply();
    }

    /**
     * Returns the {@link Account#uri} (in String form) of the last account the
     * user compose a message from.
     */
    public String getLastSentFromAccount() {
        return getPreferences().getString(LAST_SENT_FROM_ACCOUNT_KEY, null);
    }

    /**
     * Persists the {@link Account#uri} (in String form) of the last account the
     * user compose a message from.
     */
    public void setLastSentFromAccount(String accountUriStr) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(LAST_SENT_FROM_ACCOUNT_KEY, accountUriStr);
        editor.apply();
    }

    private void loadCachedAccountList() {
        JSONArray accounts = null;
        try {
            final String accountsJson = getPreferences().getString(ACCOUNT_LIST_KEY, null);
            if (accountsJson != null) {
                accounts = new JSONArray(accountsJson);
            }
        } catch (Exception e) {
            LogUtils.e(LOG_TAG, e, "ignoring unparsable accounts cache");
        }

        if (accounts == null) {
            return;
        }

        for (int i = 0; i < accounts.length(); i++) {
            try {
                final AccountCacheEntry accountEntry = new AccountCacheEntry(
                        accounts.getJSONObject(i));

                if (accountEntry.mAccount.settings == null) {
                    LogUtils.e(LOG_TAG, "Dropping account that doesn't specify settings");
                    continue;
                }

                Account account = accountEntry.mAccount;
                ContentProviderClient client =
                        mResolver.acquireContentProviderClient(account.uri);
                if (client != null) {
                    client.release();
                    addAccountImpl(account.uri, accountEntry);
                } else {
                    LogUtils.e(LOG_TAG, "Dropping account without provider: %s",
                            account.name);
                }

            } catch (Exception e) {
                // Unable to create account object, skip to next
                LogUtils.e(LOG_TAG, e,
                        "Unable to create account object from serialized form");
            }
        }
        broadcastAccountChange();
    }

    private void cacheAccountList() {
        final List<AccountCacheEntry> accountList;

        synchronized (mAccountCache) {
            accountList = ImmutableList.copyOf(mAccountCache.values());
        }

        final JSONArray arr = new JSONArray();
        for (AccountCacheEntry accountEntry : accountList) {
            arr.put(accountEntry.toJSONObject());
        }

        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(ACCOUNT_LIST_KEY, arr.toString());
        editor.apply();
    }

    private SharedPreferences getPreferences() {
        if (mSharedPrefs == null) {
            mSharedPrefs = getContext().getSharedPreferences(
                    SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        }
        return mSharedPrefs;
    }

    static public Account getAccountFromAccountUri(Uri accountUri) {
        MailAppProvider provider = getInstance();
        if (provider != null && provider.mAccountsFullyLoaded) {
            synchronized(provider.mAccountCache) {
                AccountCacheEntry entry = provider.mAccountCache.get(accountUri);
                if (entry != null) {
                    return entry.mAccount;
                }
            }
        }
        return null;
    }

    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
        if (data == null) {
            LogUtils.d(LOG_TAG, "null account cursor returned");
            return;
        }

        LogUtils.d(LOG_TAG, "Cursor with %d accounts returned", data.getCount());
        final CursorLoader cursorLoader = (CursorLoader)loader;
        final Uri accountsQueryUri = cursorLoader.getUri();

        // preserve ordering on partial updates
        // also preserve ordering on complete updates for any that existed previously


        final List<AccountCacheEntry> accountList;
        synchronized (mAccountCache) {
            accountList = ImmutableList.copyOf(mAccountCache.values());
        }

        // Build a set of the account uris that had been associated with that query
        final Set<Uri> previousQueryUriSet = Sets.newHashSet();
        for (AccountCacheEntry entry : accountList) {
            if (accountsQueryUri.equals(entry.mAccountsQueryUri)) {
                previousQueryUriSet.add(entry.mAccount.uri);
            }
        }

        // Update the internal state of this provider if the returned result set
        // represents all accounts
        // TODO: determine what should happen with a heterogeneous set of accounts
        final Bundle extra = data.getExtras();
        mAccountsFullyLoaded = extra.getInt(AccountCursorExtraKeys.ACCOUNTS_LOADED) != 0;

        final Set<Uri> newQueryUriMap = Sets.newHashSet();

        // We are relying on the fact that all accounts are added in the order specified in the
        // cursor.  Initially assume that we insert these items to at the end of the list
        while (data.moveToNext()) {
            final Account account = new Account(data);
            final Uri accountUri = account.uri;
            newQueryUriMap.add(accountUri);
            // preserve existing order if already present and this is a partial update,
            // otherwise add to the end
            //
            // N.B. this ordering policy means the order in which providers respond will affect
            // the order of accounts.
            if (mAccountsFullyLoaded) {
                synchronized (mAccountCache) {
                    // removing the existing item will prevent LinkedHashMap from preserving the
                    // original insertion order
                    mAccountCache.remove(accountUri);
                }
            }
            addAccountImpl(account, accountsQueryUri, false /* don't notify */);
        }
        // Remove all of the accounts that are in the new result set
        previousQueryUriSet.removeAll(newQueryUriMap);

        // For all of the entries that had been in the previous result set, and are not
        // in the new result set, remove them from the cache
        if (previousQueryUriSet.size() > 0 && mAccountsFullyLoaded) {
            synchronized (mAccountCache) {
                for (Uri accountUri : previousQueryUriSet) {
                    LogUtils.d(LOG_TAG, "Removing account %s", accountUri);
                    mAccountCache.remove(accountUri);
                }
            }
        }
        broadcastAccountChange();

        // Cache the updated account list
        cacheAccountList();
    }

    /**
     * Object that allows the Account Cache provider to associate the account with the content
     * provider uri that originated that account.
     */
    private static class AccountCacheEntry {
        final Account mAccount;
        final Uri mAccountsQueryUri;

        private static final String KEY_ACCOUNT = "acct";
        private static final String KEY_QUERY_URI = "queryUri";

        public AccountCacheEntry(Account account, Uri accountQueryUri) {
            mAccount = account;
            mAccountsQueryUri = accountQueryUri;
        }

        public AccountCacheEntry(JSONObject o) throws JSONException {
            mAccount = Account.newinstance(o.getString(KEY_ACCOUNT));
            if (mAccount == null) {
                throw new IllegalArgumentException("AccountCacheEntry de-serializing failed. "
                        + "Account object could not be created from the JSONObject: "
                        + o);
            }
            if (mAccount.settings == Settings.EMPTY_SETTINGS) {
                throw new IllegalArgumentException("AccountCacheEntry de-serializing failed. "
                        + "Settings could not be created from the JSONObject: " + o);
            }
            final String uriStr = o.optString(KEY_QUERY_URI, null);
            if (uriStr != null) {
                mAccountsQueryUri = Uri.parse(uriStr);
            } else {
                mAccountsQueryUri = null;
            }
        }

        public JSONObject toJSONObject() {
            try {
                return new JSONObject()
                .put(KEY_ACCOUNT, mAccount.serialize())
                .putOpt(KEY_QUERY_URI, mAccountsQueryUri);
            } catch (JSONException e) {
                // shouldn't happen
                throw new IllegalArgumentException(e);
            }
        }

    }
}
