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

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;

import com.android.mail.R;
import com.android.mail.providers.Account;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.FolderType;
import com.android.mail.ui.FolderSelectorAdapter.FolderRow;
import com.android.mail.utils.Utils;
import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Displays a folder selection dialog for the conversation provided. It allows
 * the user to mark folders to assign that conversation to.
 */
public class MultiFoldersSelectionDialog extends FolderSelectionDialog {
    private final boolean mSingle;
    private final HashMap<Uri, FolderOperation> mOperations;

    /**
     * Create a new {@link MultiFoldersSelectionDialog}. It is displayed when
     * the {@link #show()} method is called.
     * @param context
     * @param account the current account
     * @param updater
     * @param target conversations that are impacted
     * @param isBatch whether the dialog is shown during Contextual Action Bar
     *            (CAB) mode
     * @param currentFolder the current folder that the
     *            {@link FolderListFragment} is showing
     */
    public MultiFoldersSelectionDialog(final Context context, final Account account,
            final ConversationUpdater updater, final Collection<Conversation> target,
            final boolean isBatch, final Folder currentFolder) {
        super(context, account, updater, target, isBatch, currentFolder);
        mSingle = !account
                .supportsCapability(UIProvider.AccountCapabilities.MULTIPLE_FOLDERS_PER_CONV);
        mOperations = new HashMap<Uri, FolderOperation>();

        mBuilder.setTitle(R.string.change_folders_selection_dialog_title);
        mBuilder.setPositiveButton(R.string.ok, this);
    }

    @Override
    protected void updateAdapterInBackground(Context context) {
        Cursor foldersCursor = null;
        try {
            foldersCursor = context.getContentResolver().query(
                    !Utils.isEmpty(mAccount.fullFolderListUri) ? mAccount.fullFolderListUri
                            : mAccount.folderListUri, UIProvider.FOLDERS_PROJECTION, null, null,
                            null);
            /** All the folders that this conversations is assigned to. */
            final HashSet<String> checked = new HashSet<String>();
            for (final Conversation conversation : mTarget) {
                final List<Folder> rawFolders = conversation.getRawFolders();
                if (rawFolders != null && rawFolders.size() > 0) {
                    // Parse the raw folders and get all the uris.
                    checked.addAll(Arrays.asList(Folder.getUriArray(rawFolders)));
                } else {
                    // There are no folders for this conversation, so it must
                    // belong to the folder we are currently looking at.
                    checked.add(mCurrentFolder.folderUri.fullUri.toString());
                }
            }
            // TODO(mindyp) : bring this back in UR8 when Email providers
            // will have divided folder sections.
            /* final String[] headers = mContext.getResources()
             .getStringArray(R.array.moveto_folder_sections);
             // Currently, the number of adapters are assumed to match the
             // number of headers in the string array.
             mAdapter.addSection(new SystemFolderSelectorAdapter(mContext,
             foldersCursor, checked, R.layout.multi_folders_view, null));

            // TODO(mindyp): we currently do not support frequently moved to
            // folders, at headers[1]; need to define what that means.*/
            mAdapter.addSection(new AddableFolderSelectorAdapter(context,
                    AddableFolderSelectorAdapter.filterFolders(foldersCursor,
                            ImmutableSet.of(FolderType.INBOX_SECTION)), checked,
                    R.layout.multi_folders_view, null));
            mBuilder.setAdapter(mAdapter, MultiFoldersSelectionDialog.this);
        } finally {
            if (foldersCursor != null) {
                foldersCursor.close();
            }
        }
    }

    @Override
    protected void onListItemClick(int position) {
        final Object item = mAdapter.getItem(position);
        if (item instanceof FolderRow) {
            update((FolderRow) item);
        }
    }

    /**
     * Call this to update the state of folders as a result of them being
     * selected / de-selected.
     *
     * @param row The item being updated.
     */
    private final void update(FolderSelectorAdapter.FolderRow row) {
        final boolean add = !row.isPresent();
        if (mSingle) {
            if (!add) {
                // This would remove the check on a single radio button, so just
                // return.
                return;
            }
            // Clear any other checked items.
            for (int i = 0, size = mAdapter.getCount(); i < size; i++) {
                final Object item = mAdapter.getItem(i);
                if (item instanceof FolderRow) {
                   ((FolderRow)item).setIsPresent(false);
                   final Folder folder = ((FolderRow)item).getFolder();
                   mOperations.put(folder.folderUri.fullUri,
                           new FolderOperation(folder, false));
                }
            }
        }
        row.setIsPresent(add);
        mAdapter.notifyDataSetChanged();
        final Folder folder = row.getFolder();
        mOperations.put(folder.folderUri.fullUri, new FolderOperation(folder, add));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (mUpdater != null) {
                    mUpdater.assignFolder(mOperations.values(), mTarget, mBatch,
                            true /* showUndo */, false /* isMoveTo */);
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
            default:
                break;
        }
    }
}
