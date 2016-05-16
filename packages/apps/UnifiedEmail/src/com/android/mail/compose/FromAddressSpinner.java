/**
 * Copyright (c) 2012, Google Inc.
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
package com.android.mail.compose;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

import com.android.mail.providers.Account;
import com.android.mail.providers.Message;
import com.android.mail.providers.ReplyFromAccount;
import com.android.mail.utils.AccountUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

public class FromAddressSpinner extends Spinner implements OnItemSelectedListener {
    private List<Account> mAccounts;
    private ReplyFromAccount mAccount;
    private final List<ReplyFromAccount> mReplyFromAccounts = Lists.newArrayList();
    private OnAccountChangedListener mAccountChangedListener;

    public FromAddressSpinner(Context context) {
        this(context, null);
    }

    public FromAddressSpinner(Context context, AttributeSet set) {
        super(context, set);
    }

    public void setCurrentAccount(ReplyFromAccount account) {
        mAccount = account;
        selectCurrentAccount();
    }

    private void selectCurrentAccount() {
        if (mAccount == null) {
            return;
        }
        int currentIndex = 0;
        for (ReplyFromAccount acct : mReplyFromAccounts) {
            if (TextUtils.equals(mAccount.name, acct.name)
                    && TextUtils.equals(mAccount.address, acct.address)) {
                setSelection(currentIndex, true);
                break;
            }
            currentIndex++;
        }
    }

    public ReplyFromAccount getMatchingReplyFromAccount(String accountString) {
        if (!TextUtils.isEmpty(accountString)) {
            for (ReplyFromAccount acct : mReplyFromAccounts) {
                // TODO: Do not key off ReplyFromAccount.name b/11292541
                if (accountString.equals(acct.name)) {
                    return acct;
                }
            }
        }
        return null;
    }

    public ReplyFromAccount getCurrentAccount() {
        return mAccount;
    }

    /**
     * @param action Action being performed; if this is COMPOSE, show all
     *            accounts. Otherwise, show just the account this was launched
     *            with.
     * @param currentAccount Account used to launch activity.
     * @param syncingAccounts
     */
    public void initialize(int action, Account currentAccount, Account[] syncingAccounts,
                Message refMessage) {
        final List<Account> accounts = AccountUtils.mergeAccountLists(mAccounts,
                syncingAccounts, true /* prioritizeAccountList */);
        if (action == ComposeActivity.COMPOSE) {
            mAccounts = accounts;
        } else {
            // First assume that we are going to use the current account as the reply account
            Account replyAccount = currentAccount;

            if (refMessage != null && refMessage.accountUri != null) {
                // This is a reply or forward of a message access through the "combined" account.
                // We want to make sure that the real account is in the spinner
                for (Account account : accounts) {
                    if (account.uri.equals(refMessage.accountUri)) {
                        replyAccount = account;
                        break;
                    }
                }
            }
            mAccounts = ImmutableList.of(replyAccount);
        }
        initFromSpinner();
    }

    @VisibleForTesting
    protected void initFromSpinner() {
        // If there are not yet any accounts in the cached synced accounts
        // because this is the first time mail was opened, and it was opened
        // directly to the compose activity, don't bother populating the reply
        // from spinner yet.
        if (mAccounts == null || mAccounts.size() == 0) {
            return;
        }
        FromAddressSpinnerAdapter adapter = new FromAddressSpinnerAdapter(getContext());

        mReplyFromAccounts.clear();
        for (Account account : mAccounts) {
            mReplyFromAccounts.addAll(account.getReplyFroms());
        }
        adapter.addAccounts(mReplyFromAccounts);

        setAdapter(adapter);
        selectCurrentAccount();
        setOnItemSelectedListener(this);
    }

    public List<ReplyFromAccount> getReplyFromAccounts() {
        return mReplyFromAccounts;
    }

    public void setOnAccountChangedListener(OnAccountChangedListener listener) {
        mAccountChangedListener = listener;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ReplyFromAccount selection = (ReplyFromAccount) getItemAtPosition(position);
        // TODO: Do not key off ReplyFromAccount.name b/11292541
        if (!selection.name.equals(mAccount.name)) {
            mAccount = selection;
            mAccountChangedListener.onAccountChanged();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }

    /**
     * Classes that want to know when a different account in the
     * FromAddressSpinner has been selected should implement this interface.
     * Note: if the user chooses the same account as the one that has already
     * been selected, this method will not be called.
     */
    public static interface OnAccountChangedListener {
        public void onAccountChanged();
    }
}
