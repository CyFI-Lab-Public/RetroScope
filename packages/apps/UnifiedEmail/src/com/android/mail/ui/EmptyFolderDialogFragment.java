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

package com.android.mail.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.android.mail.R;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;

import java.lang.ref.WeakReference;

/**
 * A confirmation dialog for emptying folders of all contents.
 * Currently used to empty trash or spam.
 */
public class EmptyFolderDialogFragment extends DialogFragment {

    public interface EmptyFolderDialogFragmentListener {

        /**
         * Called when the folder is emptied by the DialogFragment.
         */
        void onFolderEmptied();
    }

    public static final String FRAGMENT_TAG = "EmptyFolderDialogFragment";

    private static final String ARG_NUM_CONVERSATIONS = "numConversations";
    private static final String ARG_FOLDER_TYPE = "folderType";

    private WeakReference<EmptyFolderDialogFragmentListener> mListener = null;

    private int mNumConversations;
    private int mFolderType;

    // Public no-args constructor needed for fragment re-instantiation
    public EmptyFolderDialogFragment() {}

    /**
     * Creates a new instance of {@link EmptyFolderDialogFragment}.
     * @param numConversations The number of conversations to display in the dialog.
     * @param folderType The type of dialog to show. The current available options are
     *                   {@link com.android.mail.providers.UIProvider.FolderType#TRASH} and
     *                   {@link com.android.mail.providers.UIProvider.FolderType#SPAM}.
     * @return The newly created {@link EmptyFolderDialogFragment}.
     */
    public static EmptyFolderDialogFragment newInstance(
            final int numConversations, final int folderType) {
        final EmptyFolderDialogFragment fragment =
                new EmptyFolderDialogFragment();

        final Bundle args = new Bundle(2);
        args.putInt(ARG_NUM_CONVERSATIONS, numConversations);
        args.putInt(ARG_FOLDER_TYPE, folderType);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        mNumConversations = getArguments().getInt(ARG_NUM_CONVERSATIONS);
        mFolderType = getArguments().getInt(ARG_FOLDER_TYPE);

        final String dialogMessage = getResources().getQuantityString(
                R.plurals.empty_folder_dialog_message, mNumConversations, mNumConversations);

        // Checks if we're in the spam folder, otherwise just uses trash as the default.
        final int dialogTitleId = Folder.isType(mFolderType, UIProvider.FolderType.SPAM) ?
                R.string.empty_spam_dialog_title : R.string.empty_trash_dialog_title;

        return new AlertDialog.Builder(getActivity()).setTitle(dialogTitleId)
                .setMessage(dialogMessage)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int whichButton) {
                        if (mListener != null) {
                            final EmptyFolderDialogFragmentListener listener =
                                    mListener.get();
                            if (listener != null) {
                                listener.onFolderEmptied();
                            }
                        }
                    }
                })
                .create();
    }

    public void setListener(final EmptyFolderDialogFragmentListener listener) {
        mListener = new WeakReference<EmptyFolderDialogFragmentListener>(listener);
    }
}
