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
 * limitations under the License
 */

package android.provider.cts.contacts.account;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;

/**
 * Account authenticator with 1 hard coded account.
 *
 * Also adds the account to the account manager on instantiation.
 */
public class StaticAccountAuthenticator extends AbstractAccountAuthenticator {

    public static final String NAME = "test_account_name";
    public static final String TYPE = "com.android.cts.contactsprovider";
    public static final Account ACCOUNT_1 = new Account("cp account 1", TYPE);

    private static final String LABEL = "test_auth_token_label";
    private static final String TOKEN = "asdlkjfslkjfdklj";

    private static Bundle sAccountBundle;
    static {
        sAccountBundle = new Bundle();
        sAccountBundle.putString(AccountManager.KEY_ACCOUNT_NAME, NAME);
        sAccountBundle.putString(AccountManager.KEY_ACCOUNT_TYPE, TYPE);
        sAccountBundle.putString(AccountManager.KEY_AUTHTOKEN, TOKEN);
    }

    private static Bundle createResultBundle() {
        return sAccountBundle;
    }

    public StaticAccountAuthenticator(Context context) {
        super(context);
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return createResultBundle();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        return createResultBundle();
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
            Bundle options) throws NetworkErrorException {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return result;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        return createResultBundle();
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return LABEL;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        return createResultBundle();
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) throws NetworkErrorException {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

}
