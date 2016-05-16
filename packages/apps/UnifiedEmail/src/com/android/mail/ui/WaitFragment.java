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

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.mail.R;
import com.android.mail.providers.Account;
import com.android.mail.providers.UIProvider.SyncStatus;

public class WaitFragment extends Fragment implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    // Keys used to pass data to {@link WaitFragment}.
    private static final String ACCOUNT_KEY = "account";

    private static final String DEFAULT_KEY = "isDefault";

    private static final int MANUAL_SYNC_LOADER = 0;


    private Account mAccount;

    private LayoutInflater mInflater;

    private boolean mDefault;

    // Public no-args constructor needed for fragment re-instantiation
    public WaitFragment() {}

    public static WaitFragment newInstance(Account account) {
        return newInstance(account, false);
    }

    public static WaitFragment newInstance(Account account, boolean def) {
        WaitFragment fragment = new WaitFragment();

        final Bundle args = new Bundle();
        args.putParcelable(ACCOUNT_KEY, account);
        args.putBoolean(DEFAULT_KEY, def);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mAccount = (Account)args.getParcelable(ACCOUNT_KEY);
        mDefault = args.getBoolean(DEFAULT_KEY, false);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        ViewGroup wrapper = (ViewGroup) mInflater
                .inflate(R.layout.wait_container, container, false);
        wrapper.addView(getContent(wrapper));
        return wrapper;
    }

    private View getContent(ViewGroup root) {
        final View view;
        if (mAccount != null
                && (mAccount.syncStatus & SyncStatus.MANUAL_SYNC_REQUIRED)
                    == SyncStatus.MANUAL_SYNC_REQUIRED) {
            // A manual sync is required
            view = mInflater.inflate(R.layout.wait_for_manual_sync, root, false);

            view.findViewById(R.id.manual_sync).setOnClickListener(this);
            view.findViewById(R.id.change_sync_settings).setOnClickListener(this);

        } else if (mDefault) {
            view = mInflater.inflate(R.layout.wait_default, root, false);
        } else {
            view = mInflater.inflate(R.layout.wait_for_sync, root, false);
        }

        return view;
    }

    public void updateAccount(Account account) {
        mAccount = account;
        ViewGroup parent = (ViewGroup) getView();
        if (parent != null) {
            parent.removeAllViews();
            parent.addView(getContent(parent));
        }
    }

    Account getAccount() {
        return mAccount;
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();

        if (id == R.id.change_sync_settings) {
            Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (id == R.id.manual_sync) {
            if (mAccount != null && mAccount.manualSyncUri != null) {
                getLoaderManager().initLoader(MANUAL_SYNC_LOADER, null, this);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // Tell the account to sync manually. As a side effect, updates will come into the
        // controller for this fragment and update it as necessary.
        return new CursorLoader(getActivity(), mAccount.manualSyncUri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
        // Do nothing.
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // Do nothing.
    }
}