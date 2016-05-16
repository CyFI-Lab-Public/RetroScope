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

import com.android.mail.R;
import com.android.mail.providers.Account;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.ui.FolderSelectorAdapter.FolderRow;
import com.android.mail.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Displays a folder selection dialog for the conversation provided. It allows
 * the user to switch a conversation from one folder to another.
 */
public class SingleFolderSelectionDialog extends FolderSelectionDialog {

    /**
     * Create a new {@link SingleFolderSelectionDialog}. It is displayed when
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
    public SingleFolderSelectionDialog(final Context context, final Account account,
            final ConversationUpdater updater, final Collection<Conversation> target,
            final boolean isBatch, final Folder currentFolder) {
        super(context, account, updater, target, isBatch, currentFolder);

        mBuilder.setTitle(R.string.move_to_selection_dialog_title);
    }

    @Override
    protected void updateAdapterInBackground(Context context) {
        Cursor foldersCursor = null;
        try {
            foldersCursor = context.getContentResolver().query(
                    !Utils.isEmpty(mAccount.fullFolderListUri) ? mAccount.fullFolderListUri
                            : mAccount.folderListUri, UIProvider.FOLDERS_PROJECTION, null,
                    null, null);
            // TODO(mindyp) : bring this back in UR8 when Email providers
            // will have divided folder sections.
            final String[] headers = context.getResources().getStringArray(
                    R.array.moveto_folder_sections);
            // Currently, the number of adapters are assumed to match the
            // number of headers in the string array.
            mAdapter.addSection(new SystemFolderSelectorAdapter(context, foldersCursor,
                    R.layout.single_folders_view, headers[0], mCurrentFolder));

            // TODO(mindyp): we currently do not support frequently moved to
            // folders, at headers[1]; need to define what that means.*/
            // TODO(pwestbro): determine if we need to call filterFolders
            mAdapter.addSection(new UserFolderHierarchicalFolderSelectorAdapter(context,
                    AddableFolderSelectorAdapter.filterFolders(foldersCursor, null),
                    R.layout.single_folders_view, headers[2], mCurrentFolder));
            mBuilder.setAdapter(mAdapter, SingleFolderSelectionDialog.this);
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
            final Folder folder = ((FolderRow) item).getFolder();
            ArrayList<FolderOperation> ops = new ArrayList<FolderOperation>();
            // Remove the current folder and add the new folder.
            ops.add(new FolderOperation(mCurrentFolder, false));
            ops.add(new FolderOperation(folder, true));
            mUpdater.assignFolder(ops, mTarget, mBatch, true /* showUndo */, true /* isMoveTo */);
            mDialog.dismiss();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // Do nothing.
    }
}
