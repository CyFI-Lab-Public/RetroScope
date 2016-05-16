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

package android.accounts.cts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * A simple Mock Account Authenticator
 */
public class MockAccountAuthenticator extends AbstractAccountAuthenticator {

    private AccountAuthenticatorResponse mResponse;
    private String mAccountType;
    private String mAuthTokenType;
    private String[] mRequiredFeatures;
    public Bundle mOptionsUpdateCredentials;
    public Bundle mOptionsConfirmCredentials;
    public Bundle mOptionsAddAccount;
    public Bundle mOptionsGetAuthToken;
    private Account mAccount;
    private String[] mFeatures;

    private final ArrayList<String> mockFeatureList = new ArrayList<String>();

    public MockAccountAuthenticator(Context context) {
        super(context);

        // Create some mock features
        mockFeatureList.add(AccountManagerTest.FEATURE_1);
        mockFeatureList.add(AccountManagerTest.FEATURE_2);
    }

    public AccountAuthenticatorResponse getResponse() {
        return mResponse;
    }

    public String getAccountType() {
        return mAccountType;
    }

    public String getAuthTokenType() {
        return mAuthTokenType;
    }

    public String[] getRequiredFeatures() {
        return mRequiredFeatures;
    }

    public Account getAccount() {
        return mAccount;
    }

    public String[] getFeatures() {
        return mFeatures;
    }

    public void clearData() {
        mResponse = null;
        mAccountType = null;
        mAuthTokenType = null;
        mRequiredFeatures = null;
        mOptionsUpdateCredentials = null;
        mOptionsAddAccount = null;
        mOptionsGetAuthToken = null;
        mOptionsConfirmCredentials = null;
        mAccount = null;
        mFeatures = null;
    }

    private Bundle createResultBundle() {
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, AccountManagerTest.ACCOUNT_NAME);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, AccountManagerTest.ACCOUNT_TYPE);
        result.putString(AccountManager.KEY_AUTHTOKEN, AccountManagerTest.AUTH_TOKEN);

        return result;
    }

    /**
     * Adds an account of the specified accountType.
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {

        this.mResponse = response;
        this.mAccountType = accountType;
        this.mAuthTokenType = authTokenType;
        this.mRequiredFeatures = requiredFeatures;
        this.mOptionsAddAccount = options;

        return createResultBundle();
    }

    /**
     * Update the locally stored credentials for an account.
     */
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {

        this.mResponse = response;
        this.mAccount = account;
        this.mAuthTokenType = authTokenType;
        this.mOptionsUpdateCredentials = options;

        return createResultBundle();
    }

    /**
     * Returns a Bundle that contains the Intent of the activity that can be used to edit the
     * properties. In order to indicate success the activity should call response.setResult()
     * with a non-null Bundle.
     */
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {

        this.mResponse = response;
        this.mAccountType = accountType;

        return createResultBundle();
    }

    /**
     * Checks that the user knows the credentials of an account.
     */
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
            Bundle options) throws NetworkErrorException {

        this.mResponse = response;
        this.mAccount = account;
        this.mOptionsConfirmCredentials = options;

        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);

        return result;
    }

    /**
     * Gets the authtoken for an account.
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {

        this.mResponse = response;
        this.mAccount = account;
        this.mAuthTokenType = authTokenType;
        this.mOptionsGetAuthToken = options;

        return createResultBundle();
    }

    /**
     * Ask the authenticator for a localized label for the given authTokenType.
     */
    @Override
    public String getAuthTokenLabel(String authTokenType) {
        this.mAuthTokenType = authTokenType;

        return AccountManagerTest.AUTH_TOKEN_LABEL;
    }

    /**
     * Checks if the account supports all the specified authenticator specific features.
     */
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) throws NetworkErrorException {

        this.mResponse = response;
        this.mAccount = account;
        this.mFeatures = features;

        Bundle result = new Bundle();
        if (null == features) {
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        }
        else {
            boolean booleanResult = true;
            for (String feature: features) {
                if (!mockFeatureList.contains(feature)) {
                    booleanResult = false;
                    break;
                }
            }
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, booleanResult);
        }
        return result;
    }
}