/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.providers.Account;
import com.android.mail.providers.MailAppProvider;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogTag;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * An activity that shows the list of all the available accounts and return the
 * one selected in onResult().
 */
public class MailboxSelectionActivity extends ListActivity implements OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    // Used to save our instance state
    private static final String CREATE_SHORTCUT_KEY = "createShortcut";
    private static final String CREATE_WIDGET_KEY = "createWidget";
    private static final String WIDGET_ID_KEY = "widgetId";
    private static final String WAITING_FOR_ADD_ACCOUNT_RESULT_KEY = "waitingForAddAccountResult";

    private static final String ACCOUNT = "name";
    private static final String[] COLUMN_NAMES = { ACCOUNT };
    protected static final String LOG_TAG = LogTag.getLogTag();
    private static final int RESULT_CREATE_ACCOUNT = 2;
    private static final int LOADER_ACCOUNT_CURSOR = 0;
    private static final String TAG_WAIT = "wait-fragment";
    private final int[] VIEW_IDS = { R.id.mailbox_name };
    private boolean mCreateShortcut = false;
    private boolean mConfigureWidget = false;
    private SimpleCursorAdapter mAdapter;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    // Boolean to indicate that we are waiting for the result from an add account
    // operation.  This boolean is necessary, as there is no guarantee on whether the
    // AccountManager callback or onResume will be called first.
    boolean mWaitingForAddAccountResult = false;

    // Can only do certain actions if the Activity is resumed (e.g. setVisible)
    private boolean mResumed = false;
    private Handler mHandler = new Handler();
    private View mContent;
    private View mWait;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.mailbox_selection_activity);
        mContent = findViewById(R.id.content);
        mWait = findViewById(R.id.wait);
        if (icicle != null) {
            restoreState(icicle);
        } else {
            if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
                mCreateShortcut = true;
            }
            mAppWidgetId = getIntent().getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                mConfigureWidget = true;
            }
        }
        // We set the default title to "Gmail" or "Google Mail" for consistency
        // in Task Switcher. If this is for create shortcut or configure widget,
        // we should set the title to "Select account".
        if (mCreateShortcut || mConfigureWidget) {
            setTitle(getResources().getString(R.string.activity_mailbox_selection));
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setIcon(R.mipmap.ic_launcher_shortcut_folder);
            }
        }
        ((Button) findViewById(R.id.first_button)).setOnClickListener(this);

        // Initially, assume that the main view is invisible.  It will be made visible,
        // if we display the account list
        setVisible(false);
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);

        icicle.putBoolean(CREATE_SHORTCUT_KEY, mCreateShortcut);
        icicle.putBoolean(CREATE_WIDGET_KEY, mConfigureWidget);
        if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            icicle.putInt(WIDGET_ID_KEY, mAppWidgetId);
        }
        icicle.putBoolean(WAITING_FOR_ADD_ACCOUNT_RESULT_KEY, mWaitingForAddAccountResult);
    }

    @Override
    public void onStart() {
        super.onStart();

        Analytics.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.getInstance().activityStop(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mResumed = true;
        // Only fetch the accounts, if we are not handling a response from the
        // launched child activity.
        if (!mWaitingForAddAccountResult) {
            setupWithAccounts();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mResumed = false;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /**
     * Restores the activity state from a bundle
     */
    private void restoreState(Bundle icicle) {
        if (icicle.containsKey(CREATE_SHORTCUT_KEY)) {
            mCreateShortcut = icicle.getBoolean(CREATE_SHORTCUT_KEY);
        }
        if (icicle.containsKey(CREATE_WIDGET_KEY)) {
            mConfigureWidget = icicle.getBoolean(CREATE_WIDGET_KEY);
        }
        if (icicle.containsKey(WIDGET_ID_KEY)) {
            mAppWidgetId = icicle.getInt(WIDGET_ID_KEY);
        }
        if (icicle.containsKey(WAITING_FOR_ADD_ACCOUNT_RESULT_KEY)) {
            mWaitingForAddAccountResult = icicle.getBoolean(WAITING_FOR_ADD_ACCOUNT_RESULT_KEY);
        }
    }

    private void setupWithAccounts() {
        final ContentResolver resolver = getContentResolver();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Cursor cursor = null;
                try {
                    cursor = resolver.query(MailAppProvider.getAccountsUri(),
                            UIProvider.ACCOUNTS_PROJECTION, null, null, null);
                    completeSetupWithAccounts(cursor);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                return null;
            }

        }.execute();
    }

    private void completeSetupWithAccounts(final Cursor accounts) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateAccountList(accounts);
            }
        });
    }

    private void updateAccountList(final Cursor accounts) {
        boolean displayAccountList = true;
        // Configuring a widget or shortcut.
        if (mConfigureWidget || mCreateShortcut) {
            if (accounts == null || accounts.getCount() == 0) {
                // No account found, show Add Account screen, for both the widget or
                // shortcut creation process
                // No account found, show Add Account screen, for both the widget or
                // shortcut creation process
                final Intent noAccountIntent = MailAppProvider.getNoAccountIntent(this);
                if (noAccountIntent != null) {
                    startActivityForResult(noAccountIntent, RESULT_CREATE_ACCOUNT);
                }
                // No reason to display the account list
                displayAccountList = false;

                // Indicate that we need to handle the response from the add account action
                // This allows us to process the results that we get in the AddAccountCallback
                mWaitingForAddAccountResult = true;
            } else if (mConfigureWidget && accounts.getCount() == 1) {
                mWait.setVisibility(View.GONE);
                // When configuring a widget, if there is only one account, automatically
                // choose that account.
                accounts.moveToFirst();
                selectAccount(new Account(accounts));
                // No reason to display the account list
                displayAccountList = false;
            }
        }

        if (displayAccountList) {
            mContent.setVisibility(View.VISIBLE);
            // We are about to display the list, make this activity visible
            // But only if the Activity is not paused!
            if (mResumed) {
                setVisible(true);
            }

            mAdapter = new SimpleCursorAdapter(this, R.layout.mailbox_item, accounts,
                    COLUMN_NAMES, VIEW_IDS, 0) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    TextView accountView = (TextView) v.findViewById(R.id.mailbox_name);
                    accountView.setText(new Account((Cursor) getItem(position)).name);
                    return v;
                }
            };
            setListAdapter(mAdapter);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        selectAccount(new Account((Cursor)mAdapter.getItem(position)));
    }

    private void selectAccount(Account account) {
        if (mCreateShortcut || mConfigureWidget) {
            // Invoked for a shortcut creation
            final Intent intent = new Intent(this, getFolderSelectionActivity());
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            intent.setAction(mCreateShortcut ?
                    Intent.ACTION_CREATE_SHORTCUT : AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            if (mConfigureWidget) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            }
            intent.putExtra(FolderSelectionActivity.EXTRA_ACCOUNT_SHORTCUT, account);
            startActivity(intent);
            finish();
        } else {
            // TODO: (mindyp) handle changing the account for this shortcut.
            finish();
        }
    }

    /**
     * Return the class responsible for launching the folder selection activity.
     */
    protected Class<?> getFolderSelectionActivity() {
        return FolderSelectionActivity.class;
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.first_button) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected final void onActivityResult(int request, int result, Intent data) {
        if (request == RESULT_CREATE_ACCOUNT) {
            // We were waiting for the user to create an account
            if (result != RESULT_OK) {
                finish();
            } else {
                // Watch for accounts to show up!
                // restart the loader to get the updated list of accounts
                getLoaderManager().initLoader(LOADER_ACCOUNT_CURSOR, null, this);
                showWaitFragment(null);
            }
        }
    }

    private void showWaitFragment(Account account) {
        WaitFragment fragment = getWaitFragment();
        if (fragment != null) {
            fragment.updateAccount(account);
        } else {
            mWait.setVisibility(View.VISIBLE);
            replaceFragment(WaitFragment.newInstance(account, true),
                    FragmentTransaction.TRANSIT_FRAGMENT_OPEN, TAG_WAIT);
        }
        mContent.setVisibility(View.GONE);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ACCOUNT_CURSOR:
                return new CursorLoader(this, MailAppProvider.getAccountsUri(),
                        UIProvider.ACCOUNTS_PROJECTION, null, null, null);
        }
        return null;
    }

    private WaitFragment getWaitFragment() {
        return (WaitFragment) getFragmentManager().findFragmentByTag(TAG_WAIT);
    }

    private int replaceFragment(Fragment fragment, int transition, String tag) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(transition);
        fragmentTransaction.replace(R.id.wait, fragment, tag);
        final int transactionId = fragmentTransaction.commitAllowingStateLoss();
        return transactionId;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // Do nothing.
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursor, Cursor data) {
        if (data != null && data.moveToFirst()) {
            // there are accounts now!
            Account account;
            ArrayList<Account> accounts = new ArrayList<Account>();
            ArrayList<Account> initializedAccounts = new ArrayList<Account>();
            do {
                account = new Account(data);
                if (account.isAccountReady()) {
                    initializedAccounts.add(account);
                }
                accounts.add(account);
            } while (data.moveToNext());
            if (initializedAccounts.size() > 0) {
                mWait.setVisibility(View.GONE);
                getLoaderManager().destroyLoader(LOADER_ACCOUNT_CURSOR);
                mContent.setVisibility(View.VISIBLE);
                updateAccountList(data);
            } else {
                // Show "waiting"
                account = accounts.size() > 0 ? accounts.get(0) : null;
                showWaitFragment(account);
            }
        }
    }

    @Override
    public void onBackPressed() {
        mWaitingForAddAccountResult = false;
        // If we are showing the wait fragment, just exit.
        if (getWaitFragment() != null) {
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
