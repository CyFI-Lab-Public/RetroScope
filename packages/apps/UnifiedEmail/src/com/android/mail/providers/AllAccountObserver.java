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


package com.android.mail.providers;

import com.android.mail.ui.AccountController;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import android.database.DataSetObserver;

/**
 * A simple extension of {@link android.database.DataSetObserver} to provide all Accounts in
 * {@link #onChanged(Account[])} when the list of Accounts changes. Initializing the object
 * registers with the observer with the {@link com.android.mail.ui.AccountController} provided.
 * The object will then begin to receive {@link #onChanged(Account[])} till {@link
 * #unregisterAndDestroy()} is called. <p> To implement an {@link com.android.mail.providers
 * .AllAccountObserver}, you need to implement the {@link #onChanged(Account[])} method.
 */
public abstract class AllAccountObserver extends DataSetObserver {
    /**
     * The AccountController that the observer is registered with.
     */
    private AccountController mController;

    private static final String LOG_TAG = LogTag.getLogTag();

    /**
     * The no-argument constructor leaves the object unusable till
     * {@link #initialize(com.android.mail.ui.AccountController)} is called.
     */
    public AllAccountObserver() {
    }

    /**
     * Initializes an {@link com.android.mail.providers.AllAccountObserver} object that receives
     * a call to {@link #onChanged(Account[])} when the controller changes the list of accounts.
     *
     * @param controller
     */
    public Account[] initialize(AccountController controller) {
        if (controller == null) {
            LogUtils.wtf(LOG_TAG, "AllAccountObserver initialized with null controller!");
        }
        mController = controller;
        mController.registerAllAccountObserver(this);
        return mController.getAllAccounts();
    }

    @Override
    public final void onChanged() {
        if (mController == null) {
            return;
        }
        onChanged(mController.getAllAccounts());
    }

    /**
     * Callback invoked when the list of Accounts changes.
     * The updated list is passed as the argument.
     * @param allAccounts the array of all accounts in the current application.
     */
    public abstract void onChanged(Account[] allAccounts);

    /**
     * Return the array of existing accounts.
     * @return the array of existing accounts.
     */
    public final Account[] getAllAccounts() {
        if (mController == null) {
            return null;
        }
        return mController.getAllAccounts();
    }

    /**
     * Unregisters for list of Account changes and makes the object unusable.
     */
    public void unregisterAndDestroy() {
        if (mController == null) {
            return;
        }
        mController.unregisterAllAccountObserver(this);
    }
}
