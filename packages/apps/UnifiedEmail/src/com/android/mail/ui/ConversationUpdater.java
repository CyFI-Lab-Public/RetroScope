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

import android.app.AlertDialog;
import android.content.ContentValues;
import android.net.Uri;

import com.android.mail.browse.ConfirmDialogFragment;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.browse.ConversationMessage;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.ConversationInfo;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;

import java.util.Collection;
import java.util.Set;

/**
 * Classes that can update conversations implement this interface.
 */
public interface ConversationUpdater extends ConversationListCallbacks {
    /**
     * Modify the given conversation by changing the column provided here to contain the value
     * provided. Column names are listed in {@link UIProvider.ConversationColumns}, for example
     * {@link UIProvider.ConversationColumns#FOLDER_LIST}
     * @param target
     * @param columnName
     * @param value
     */
    void updateConversation(Collection <Conversation> target, String columnName, String value);

    /**
     * Modify the given conversation by changing the column provided here to contain the value
     * provided. Column names are listed in {@link UIProvider.ConversationColumns}, for example
     * {@link UIProvider.ConversationColumns#READ}
     * @param target
     * @param columnName
     * @param value
     */
    void updateConversation(Collection <Conversation> target, String columnName, int value);

    /**
     * Modify the given conversation by changing the column provided here to contain the value
     * provided. Column names are listed in {@link UIProvider.ConversationColumns}, for example
     * {@link UIProvider.ConversationColumns#HAS_ATTACHMENTS}
     * @param target
     * @param columnName
     * @param value
     */
    void updateConversation(Collection <Conversation> target, String columnName, boolean value);

    /**
     * Modify the given conversation by changing the columns provided here to
     * contain the values provided. Column names are listed in
     * {@link UIProvider.ConversationColumns}, for example
     * {@link UIProvider.ConversationColumns#HAS_ATTACHMENTS}
     * @param target
     * @param values
     */
    void updateConversation(Collection <Conversation> target, ContentValues values);

    /**
     * Requests the removal of the current conversation with the specified destructive action.
     * @param actionId the unique id for the action responsible for this delete: R.id.archive, ...
     * @param target the conversations to act upon.
     * @param action to perform after the UI has been updated to remove the conversations
     * @param isBatch true if this is a batch action, false otherwise.
     */
    void delete(
            int actionId, final Collection<Conversation> target, final DestructiveAction action,
            boolean isBatch);

    /**
     * Mark a number of conversations as read or unread.
     * @param targets the conversations to act upon
     * @param read true if the conversations are marked read, false if they are marked unread.
     * @param viewed whether the conversations are marked viewed as well. This indicates that the
     * conversations are shown on the UI.
     */
    void markConversationsRead(Collection<Conversation> targets, boolean read, boolean viewed);

    /**
     * Mark a single conversation unread, either entirely or for just a subset of the messages in a
     * conversation and the view <b>returns to Conversation List</b> mode.
     *
     * @param conv conversation to mark unread
     * @param unreadMessageUris URIs for the subset of the conversation's messages to mark unread,
     * or null/empty set to mark the entire conversation unread.
     * @param originalConversationInfo the original unread state of the {@link ConversationInfo}
     * that {@link ConversationCursor} will temporarily use until the commit is complete.
     */
    void markConversationMessagesUnread(Conversation conv, Set<Uri> unreadMessageUris,
            byte[] originalConversationInfo);

    /**
     * Star a single message within a conversation. This method requires a
     * {@link ConversationMessage} to propagate the change to the owning {@link Conversation}.
     *
     */
    void starMessage(ConversationMessage msg, boolean starred);

    /**
     * Get a destructive action for selected conversations. The action corresponds to Menu item
     * identifiers, for example R.id.unread, or R.id.delete.
     * @param action
     * @return
     */
    public DestructiveAction getBatchAction(int action);

    /**
     * Get a destructive action for selected conversations. The action corresponds to Menu item
     * identifiers, for example R.id.unread, or R.id.delete.
     * @param action
     * @return
     */
    public DestructiveAction getDeferredBatchAction(int action);

    /**
     * Get destructive folder change for selected conversations.
     * The caller must explicitly call performAction.
     * @param action
     * @return
     */
    public DestructiveAction getDeferredRemoveFolder(Collection<Conversation> target,
            Folder toRemove, boolean isDestructive, boolean isBatch,
            boolean showUndo);

    /**
     * Assign the target conversations to the given folders, and remove them from all other folders
     * that they might be assigned to.
     * @param folders the folders to assign the conversations to.
     * @param target the conversations to act upon.
     * @param batch whether this is a batch operation
     * @param showUndo whether to show the undo bar
     * @param isMoveTo <code>true</code> if this is a move operation, <code>false</code> if it is
     *        some other type of folder change operation
     */
    public void assignFolder(Collection<FolderOperation> folders, Collection<Conversation> target,
            boolean batch, boolean showUndo, boolean isMoveTo);

    /**
     * Refreshes the conversation list, if one exists.
     */
    void refreshConversationList();

    /**
     * Show the next conversation after a destructive action. The next
     * conversation is determined by list state and user preferences.
     * @param conversations Conversations that were removed as part of the
     *            destructive action.
     */
    void showNextConversation(Collection<Conversation> conversations);

    /**
     * Make an action listener for a confirmation dialog, and the currently selected set of
     * conversations. The action is specified as an integer which marks the menu resource:
     * R.id.delete, R.id.discard_drafts, etc.
     * @param action the resource ID of the menu action: R.id.delete, for example
     * @param fromSelectedSet true if the listener acts on the selected set, false if the listener
     *        acts on the current conversation.
     */
    public void makeDialogListener(final int action, boolean fromSelectedSet);

    /**
     * If set, get the listener associated with the existing {@link ConfirmDialogFragment}.  This
     * listener needs to be set centrally, because the dialog fragment can get torn down, along with
     * the current activity, and the listener has to be created afresh.
     * @return the current listener for the positive action in the current confirmation dialog, if
     * any. Returns null if no confirmation dialog is currently shown.
     */
    public AlertDialog.OnClickListener getListener();
}
