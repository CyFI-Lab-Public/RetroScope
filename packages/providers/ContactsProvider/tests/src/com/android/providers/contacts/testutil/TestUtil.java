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

package com.android.providers.contacts.testutil;

import android.accounts.Account;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

/**
 * Common methods used for testing.
 */
public class TestUtil {
    private static String TAG = TestUtil.class.getSimpleName();

    public static final Account ACCOUNT_1 = new Account("account_name_1", "account_type_1");
    public static final Account ACCOUNT_2 = new Account("account_name_2", "account_type_2");

    /**
     * Sleep for 1ms.
     */
    public static void sleep() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Log.w(TAG, "Sleep interrupted.");
        }
    }

    public static Uri maybeAddAccountQueryParameters(Uri uri, Account account) {
        if (account == null) {
            return uri;
        }
        return uri.buildUpon()
                .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
                .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
                .build();
    }
}
