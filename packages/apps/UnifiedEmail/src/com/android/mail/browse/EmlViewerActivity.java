/*
 * Copyright (C) 2013 Google Inc.
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

package com.android.mail.browse;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.android.mail.R;
import com.android.mail.content.CursorCreator;
import com.android.mail.content.ObjectCursor;
import com.android.mail.content.ObjectCursorLoader;
import com.android.mail.providers.Account;
import com.android.mail.providers.UIProvider;
import com.android.mail.ui.FeedbackEnabledActivity;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.MimeType;
import com.android.mail.utils.Utils;

public class EmlViewerActivity extends Activity implements FeedbackEnabledActivity,
        ConversationAccountController {
    public static final String EXTRA_ACCOUNT_URI = "extra-account-uri";

    private static final String LOG_TAG = LogTag.getLogTag();

    private static final String FRAGMENT_TAG = "eml_message_fragment";

    private static final int ACCOUNT_LOADER = 0;

    private static final String SAVED_ACCOUNT = "saved-account";

    private MenuItem mHelpItem;
    private MenuItem mSendFeedbackItem;

    private Uri mAccountUri;
    private Account mAccount;

    private final AccountLoadCallbacks mAccountLoadCallbacks = new AccountLoadCallbacks();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eml_viewer_activity);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();
        mAccountUri = intent.getParcelableExtra(EXTRA_ACCOUNT_URI);

        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW.equals(action) &&
                    MimeType.isEmlMimeType(type)) {
                final FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.add(R.id.eml_root, EmlMessageViewFragment.newInstance(
                        intent.getData(), mAccountUri), FRAGMENT_TAG);
                transaction.commit();
            } else {
                LogUtils.wtf(LOG_TAG,
                        "Entered EmlViewerActivity with wrong intent action or type: %s, %s",
                        action, type);
                finish(); // we should not be here. bail out. bail out.
                return;
            }
        } else {
            if (savedInstanceState.containsKey(SAVED_ACCOUNT)) {
                mAccount = savedInstanceState.getParcelable(SAVED_ACCOUNT);
            }
        }

        // Account uri will be null if we launched from outside of the app.
        // So just don't load an account at all.
        if (mAccountUri != null) {
            getLoaderManager().initLoader(ACCOUNT_LOADER, Bundle.EMPTY, mAccountLoadCallbacks);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mAccountUri == null) {
            return false;
        }

        getMenuInflater().inflate(R.menu.eml_viewer_menu, menu);
        mHelpItem = menu.findItem(R.id.help_info_menu_item);
        mSendFeedbackItem = menu.findItem(R.id.feedback_menu_item);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mHelpItem != null) {
            mHelpItem.setVisible(mAccount != null
                    && mAccount.supportsCapability(UIProvider.AccountCapabilities.HELP_CONTENT));
        }
        if (mSendFeedbackItem != null) {
            mSendFeedbackItem.setVisible(mAccount != null
                    && mAccount.supportsCapability(UIProvider.AccountCapabilities.SEND_FEEDBACK));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.settings) {
            Utils.showSettings(this, mAccount);
        } else if (itemId == R.id.help_info_menu_item) {
            Utils.showHelp(this, mAccount, getString(R.string.main_help_context));
        } else if (itemId == R.id.feedback_menu_item) {
            Utils.sendFeedback(this, mAccount, false);
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public Context getActivityContext() {
        return this;
    }

    @Override
    public Account getAccount() {
        return mAccount;
    }

    private class AccountLoadCallbacks
            implements LoaderManager.LoaderCallbacks<ObjectCursor<Account>> {

        @Override
        public Loader<ObjectCursor<Account>> onCreateLoader(int id, Bundle args) {
            final String[] projection = UIProvider.ACCOUNTS_PROJECTION;
            final CursorCreator<Account> factory = Account.FACTORY;
            return new ObjectCursorLoader<Account>(
                    EmlViewerActivity.this, mAccountUri, projection, factory);
        }

        @Override
        public void onLoadFinished(Loader<ObjectCursor<Account>> loader,
                ObjectCursor<Account> data) {
            if (data != null && data.moveToFirst()) {
                mAccount = data.getModel();
            }
        }

        @Override
        public void onLoaderReset(Loader<ObjectCursor<Account>> loader) {
        }
    }
}
