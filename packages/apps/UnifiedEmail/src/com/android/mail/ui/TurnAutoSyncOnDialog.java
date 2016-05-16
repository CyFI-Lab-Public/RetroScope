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

package com.android.mail.ui;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;

import com.android.mail.R;
import com.android.mail.utils.Utils;

/**
 * Confirmation dialog for turning global auto-sync setting on.
 */
public class TurnAutoSyncOnDialog extends DialogFragment {

    private static final String ACCOUNT = "account";
    private static final String SYNC_AUTHORITY = "syncAuthority";

    public static final String DIALOG_TAG = "auto sync";

    private static String sDefaultSyncAuthority;

    // Should be called once per app.
    static public void setDefaultSyncAuthority(String defaultSyncAuthority) {
        sDefaultSyncAuthority = defaultSyncAuthority;
    }

    public interface TurnAutoSyncOnDialogListener {
        void onEnableAutoSync();
        void onCancelAutoSync();
    }

    private TurnAutoSyncOnDialogListener mListener;

    // Public no-args constructor needed for fragment re-instantiation
    public TurnAutoSyncOnDialog() {}

    public static TurnAutoSyncOnDialog newInstance(Account account,
            String syncAuthority) {
        final TurnAutoSyncOnDialog frag = new TurnAutoSyncOnDialog();
        final Bundle args = new Bundle(3);
        args.putParcelable(ACCOUNT, account);
        args.putString(SYNC_AUTHORITY, syncAuthority);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Account account = getArguments().getParcelable(ACCOUNT);
        final String syncAuthority = getArguments().getString(SYNC_AUTHORITY);
        final Resources resources = getResources();
        final boolean isTablet = Utils.useTabletUI(resources);
        final String bodyText = resources.getString(
                R.string.turn_auto_sync_on_dialog_body,
                resources.getString(isTablet ? R.string.tablet : R.string.phone));
        return new AlertDialog.Builder(getActivity())
                .setMessage(bodyText)
                .setTitle(R.string.turn_auto_sync_on_dialog_title)
                .setPositiveButton(R.string.turn_auto_sync_on_dialog_confirm_btn,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Turn on auto-sync
                                ContentResolver.setMasterSyncAutomatically(true);
                                // Also enable auto-sync for Gmail.
                                // Note it's possible for syncAuthority to be empty on the
                                // account (used for constructing this dialog) was
                                // cached from shared prefs.  This can happen during app upgrade.
                                // As work around use default value set by app.
                                final String authority = TextUtils.isEmpty(syncAuthority) ?
                                        sDefaultSyncAuthority : syncAuthority;
                                if (!TextUtils.isEmpty(authority)) {
                                    ContentResolver.setSyncAutomatically(
                                            account,
                                            authority,
                                            true);
                                    if (mListener != null) {
                                        mListener.onEnableAutoSync();
                                    }
                                }
                            }
                        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null) {
                            mListener.onCancelAutoSync();
                        }
                    }
                })
                .create();
    }

    public void setListener(final TurnAutoSyncOnDialogListener listener) {
        mListener = listener;
    }
}
