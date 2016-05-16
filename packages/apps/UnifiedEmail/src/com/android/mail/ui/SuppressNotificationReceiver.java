/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.ui;

import com.android.mail.providers.Account;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.ConversationListContext;

import android.net.Uri;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.text.TextUtils;


/**
 * A simple {@code BroadcastReceiver} which suppresses new e-mail notifications for a given folder.
 */
public class SuppressNotificationReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = LogTag.getLogTag();

    private Context mContext;
    private AbstractActivityController mController;
    private String mMimeType;

    /**
     * Registers this receiver to suppress the new mail notifications for a given folder so
     * that other {@code BroadcastReceiver}s don't receive them.
     */
    public boolean activate(Context context, AbstractActivityController controller) {
        final Account account = controller.getCurrentAccount();

        mContext = context;
        mController = controller;

        final IntentFilter filter = new IntentFilter(UIProvider.ACTION_UPDATE_NOTIFICATION);

        // Note: the real notification receiver must have a lower (i.e. negative) priority
        // than this receiver.
        filter.setPriority(0);
        if (account != null) {
            mMimeType = account.mimeType;
            try {
                filter.addDataType(mMimeType);
            } catch (MalformedMimeTypeException e) {
                LogUtils.wtf(LOG_TAG, "Malformed mimetype: %s", mMimeType);
            }
        } else {
            // If the current account is null, still register the receiver.  This allows the
            // internal state of the receiver to match what the caller requested.
            LogUtils.d(LOG_TAG, "Registering receiver with no mime type");
        }
        context.registerReceiver(this, filter);

        return true;
    }

    /**
     * Returns true if this suppressNotificationReceiver is activated
     */
    public boolean activated() {
        return mContext != null;
    }

    /**
     * Unregisters this receiver.
     */
    public void deactivate() {
        try {
            if (mContext != null) {
                mContext.unregisterReceiver(this);
                mContext = null;
                mMimeType = null;
            }
        } catch (IllegalArgumentException e) {
            // May throw if already unregistered. Ignore exception.
        }
    }

    /**
     * Returns a boolean indicating whether notifications are suppressed for the specified account.
     */
    public boolean notificationsDisabledForAccount(Account account) {
        return mContext != null && TextUtils.equals(account.mimeType, mMimeType);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (!UIProvider.ACTION_UPDATE_NOTIFICATION.equals(action)) {
            return;
        }

        if (!mController.isConversationListVisible()) {
            // The conversation list is not visible, don't suppress notifications.
            return;
        }

        final ConversationListContext listContext = mController.getCurrentListContext();
        if (listContext == null) {
            // A non-null list context was expected
            LogUtils.e(LOG_TAG, "unexpected null context");
            return;
        }

        if (ConversationListContext.isSearchResult(listContext)) {
            // The user is looking at a search result, don't suppress notifications.
            return;
        }

        final Account listContextAccount = listContext.account;
        final Folder listContextFolder = listContext.folder;
        // Guard against degenerate state in the controller
        if (listContextAccount == null || listContextFolder == null) {
            LogUtils.e(LOG_TAG, "SuppressNotificationReceiver.onReceive: account=%s, folder=%s",
                    listContextAccount, listContextFolder);
            return;
        }

        final Uri intentAccountUri =
                (Uri)intent.getParcelableExtra(UIProvider.UpdateNotificationExtras.EXTRA_ACCOUNT);
        if (!listContextAccount.uri.equals(intentAccountUri)) {
            return;
        }
        final Uri intentFolderUri =
                (Uri)intent.getParcelableExtra(UIProvider.UpdateNotificationExtras.EXTRA_FOLDER);

        if (!listContextFolder.folderUri.equals(intentFolderUri)) {
            return;
        }
        final int count = intent.getIntExtra(
                UIProvider.UpdateNotificationExtras.EXTRA_UPDATED_UNREAD_COUNT, 0);
        // If the count is zero we want to let the intent through so that the
        // regular receiver can remove the notification.
        // This will allow a server change, that modifies the unread count to 0, to be handled
        // by the intended recpient to clear the notification.
        if (count == 0) {
            return;
        }
        LogUtils.i(LOG_TAG, "Aborting broadcast of intent %s, folder uri is %s",
                intent, intentFolderUri);
        abortBroadcast();
    }
}
