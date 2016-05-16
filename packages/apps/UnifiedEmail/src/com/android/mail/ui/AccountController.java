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

package com.android.mail.ui;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.widget.ListView;

import com.android.mail.providers.Account;
import com.android.mail.providers.AccountObserver;
import com.android.mail.providers.Folder;
import com.android.mail.providers.FolderWatcher;
import com.android.mail.utils.VeiledAddressMatcher;

/**
 * This class consolidates account-specific actions taken by a mail activity.
 */
public interface AccountController {
    /**
     * Registers to receive changes to the current account, and obtain the current account.
     */
    void registerAccountObserver(DataSetObserver observer);

    /**
     * Removes a listener from receiving current account changes.
     */
    void unregisterAccountObserver(DataSetObserver observer);

    /**
     * Returns the current account in use by the controller. Instead of calling this method,
     * consider registering for account changes using
     * {@link AccountObserver#initialize(AccountController)}, which not only provides the current
     * account, but also updates to the account, in case of settings changes.
     */
    Account getAccount();


    /**
     * Registers to receive changes to the list of accounts, and obtain the current list.
     */
    void registerAllAccountObserver(DataSetObserver observer);

    /**
     * Removes a listener from receiving account list changes.
     */
    void unregisterAllAccountObserver(DataSetObserver observer);

    /** Returns a list of all accounts currently known. */
    Account[] getAllAccounts();

    /**
     * Returns an object that can check veiled addresses.
     * @return
     */
    VeiledAddressMatcher getVeiledAddressMatcher();

    /**
     * Handles selecting an account from within the {@link FolderListFragment}.
     *
     * @param account the account to change to.
     */
    void changeAccount(Account account);

    /**
     * Handles selecting the currently active account from within
     * the {@link FolderListFragment}.
     */
    void switchToDefaultInboxOrChangeAccount(Account account);

    /**
     * Registers to receive changes upon drawer closing when a changeAccount is called.
     */
    void registerDrawerClosedObserver(final DataSetObserver observer);

    /**
     * Removes a listener from receiving current account changes.
     */
    void unregisterDrawerClosedObserver(final DataSetObserver observer);

    /**
     * When the {@link FolderListFragment} has a new account ready for changing to,
     * close the drawer and then wait for {@link DataSetObservable#notifyChanged()}.
     * @param hasNewFolderOrAccount true if we need to load conversations for a different folder
     *            or account, false otherwise.
     */
    void closeDrawer(boolean hasNewFolderOrAccount, Account nextAccount, Folder nextFolder);

    /**
     * Set the folderWatcher
     */
    void setFolderWatcher(FolderWatcher watcher);

    /**
     * @return <code>true</code> if the drawer pull action is enabled, <code>false</code> otherwise
     */
    boolean isDrawerPullEnabled();

    /**
     * @return the choice mode to use in the {@link ListView} in the default folder list (subclasses
     * of {@link FolderListFragment} may override this
     */
    int getFolderListViewChoiceMode();
}
