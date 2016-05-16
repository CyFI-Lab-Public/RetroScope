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

import com.android.mail.browse.ConversationListFooterView;
import com.android.mail.providers.Folder;

/**
 * A controllable activity is an Activity that has a Controller attached. This activity must be
 * able to attach the various view fragments and delegate the method calls between them.
 */
public interface ControllableActivity extends HelpCallback, RestrictedActivity,
        FeedbackEnabledActivity, FolderItemView.DropHandler, UndoListener,
        AnimatedAdapter.Listener, ConversationListFooterView.FooterViewClickListener {
    /**
     * Returns the ViewMode the activity is updating.
     * @see com.android.mail.ui.ViewMode
     * @return ViewMode.
     */
    ViewMode getViewMode();

    /**
     * Returns the object that handles {@link ConversationListCallbacks} that is associated with
     * this activity.
     * @return
     */
    ConversationListCallbacks getListHandler();

    /**
     * Return the folder change listener for this activity
     * @return
     */
    FolderChangeListener getFolderChangeListener();

    /**
     * Get the set of currently selected conversations. This method returns a non-null value.
     * In case no conversation is currently selected, it returns an empty selection set.
     * @return
     */
    ConversationSelectionSet getSelectedSet();

    /**
     * Returns the listener for folder list selection changes in the folder list
     * fragment so that activity controllers can track the last folder list
     * pushed for hierarchical folders.
     */
    FolderSelector getFolderSelector();

    /**
     * Get the folder currently being accessed by the activity.
     */
    Folder getHierarchyFolder();

    /**
     * Returns an object that can update conversation state. Holding a reference to the
     * ConversationUpdater is safe since the ConversationUpdater is guaranteed to persist across
     * changes to the conversation cursor.
     * @return
     */
    ConversationUpdater getConversationUpdater();

    ErrorListener getErrorListener();

    /**
     * Returns the {@link FolderController} object associated with this activity, if any.
     * @return
     */
    FolderController getFolderController();

    /**
     * Returns the {@link AccountController} object associated with this activity, if any.
     * @return
     */
    AccountController getAccountController();

    /**
     * Returns the {@link RecentFolderController} object associated with this activity, if any.
     * @return
     */
    RecentFolderController getRecentFolderController();

    UpOrBackController getUpOrBackController();

    void startDragMode();

    void stopDragMode();

    boolean isAccessibilityEnabled();

    /**
     * Gets a helper to provide addition features in the conversation list. This may be null.
     */
    ConversationListHelper getConversationListHelper();

    /**
     * Returns the {@link FragmentLauncher} object associated with this activity, if any.
     */
    FragmentLauncher getFragmentLauncher();
}
