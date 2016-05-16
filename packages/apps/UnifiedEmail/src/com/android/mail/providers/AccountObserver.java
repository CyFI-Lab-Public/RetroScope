/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.providers;

import android.database.DataSetObserver;

import com.android.mail.ui.AccountController;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

/**
 * A simple extension of {@link DataSetObserver} to provide the updated account in
 * {@link #onChanged(Account)} when the account changes. Initializing the object registers with
 * the observer with the {@link AccountController} provided. The object will then begin to
 * receive {@link #onChanged(Account)} till {@link #unregisterAndDestroy()} is called.
 * <p>
 * To implement an {@link AccountObserver}, you need to implement the {@link #onChanged(Account)}
 * method.
 */
public abstract class AccountObserver extends DataSetObserver {
    /**
     * The AccountController that the observer is registered with.
     */
    private AccountController mController;

    private static final String LOG_TAG = LogTag.getLogTag();

    /**
     * The no-argument constructor leaves the object unusable till
     * {@link #initialize(AccountController)} is called.
     */
    public AccountObserver () {
    }

    /**
     * Initializes an {@link AccountObserver} object that receives a call to
     * {@link #onChanged(Account)} when the controller changes the account.
     *
     * @param controller
     */
    public Account initialize(AccountController controller) {
        if (controller == null) {
            LogUtils.wtf(LOG_TAG, "AccountObserver initialized with null controller!");
        }
        mController = controller;
        mController.registerAccountObserver(this);
        return mController.getAccount();
    }

    @Override
    public final void onChanged() {
        if (mController == null) {
            return;
        }
        onChanged(mController.getAccount());
    }

    /**
     * Callback invoked when the account object is changed.  Since {@link Account} objects are
     * immutable, updates can be received on changes to individual settings (sync on/off)
     * in addition to changes of accounts: alice@example.com -> bob@example.com.
     * The updated account is passed as the argument.
     * @param newAccount
     */
    public abstract void onChanged(Account newAccount);

    /**
     * Return the most current account.
     * @return
     */
    public final Account getAccount() {
        if (mController == null) {
            return null;
        }
        return mController.getAccount();
    }

    /**
     * Unregisters for account changes and makes the object unusable.
     */
    public void unregisterAndDestroy() {
        if (mController == null) {
            return;
        }
        mController.unregisterAccountObserver(this);
    }
}