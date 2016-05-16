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

import android.app.LoaderManager.LoaderCallbacks;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Parcelable;

import com.android.mail.browse.ConversationCursor;
import com.android.mail.providers.Conversation;

/**
 * A controller interface that is to receive user initiated events and handle them.
 */
public interface ConversationListCallbacks {
    /**
     * Show the conversation provided here. If the conversation is null, this is a request to pop
     * <em>out</em> of conversation view mode and head back to conversation list mode, or whatever
     * should best show in its place.
     * @param conversation conversation to display, possibly null.
     * @param inLoaderCallbacks whether we are in the scope of a {@link LoaderCallbacks} method
     * (when fragment transactions are disallowed)
     */
    void onConversationSelected(Conversation conversation, boolean inLoaderCallbacks);

    /**
     * Called whenever CAB mode has been entered via long press or selecting a sender image.
     */
    void onCabModeEntered();

    /**
     * Called whenever CAB mode has been exited.
     */
    void onCabModeExited();

    ConversationCursor getConversationListCursor();

    Conversation getCurrentConversation();
    void setCurrentConversation(Conversation c);

    /**
     * Returns whether the initial conversation has begun but not finished loading. If this returns
     * true, you can register to be told when the load in progress finishes
     * ({@link #registerConversationLoadedObserver(DataSetObserver)}).
     * <p>
     * This flag only applies to the first conversation in a set (e.g. when using ViewPager).
     *
     * @return true if the initial conversation has begun but not finished loading
     */
    boolean isInitialConversationLoading();
    void registerConversationLoadedObserver(DataSetObserver observer);
    void unregisterConversationLoadedObserver(DataSetObserver observer);
    /**
     * Coordinates actions that might occur in response to a conversation that has finished loading
     * and is now user-visible.
     */
    void onConversationSeen();

    void registerConversationListObserver(DataSetObserver observer);
    void unregisterConversationListObserver(DataSetObserver observer);

    /**
     * Commit any destructive action leave behind items so that it is no longer
     * possible to undo them.
     */
    void commitDestructiveActions(boolean animate);

    /**
     * Detect if there are any animations occurring in the conversation list.
     */
    boolean isAnimating();

    /**
     * Tell the controller that the conversation view has entered detached mode.
     */
    void setDetachedMode();

    String CONVERSATION_LIST_SCROLL_POSITION_INDEX = "index";
    String CONVERSATION_LIST_SCROLL_POSITION_OFFSET = "offset";

    /**
     * Gets the last save scroll position of the conversation list for the specified Folder.
     *
     * @return A {@link Bundle} containing two ints,
     *         {@link #CONVERSATION_LIST_SCROLL_POSITION_INDEX} and
     *         {@link #CONVERSATION_LIST_SCROLL_POSITION_OFFSET}, or <code>null</code>
     */
    Parcelable getConversationListScrollPosition(String folderUri);

    /**
     * Sets the last save scroll position of the conversation list for the specified Folder for
     * restoration on returning to this list.
     *
     * @param savedPosition A {@link Bundle} containing two ints,
     *            {@link #CONVERSATION_LIST_SCROLL_POSITION_INDEX} and
     *            {@link #CONVERSATION_LIST_SCROLL_POSITION_OFFSET}
     */
    void setConversationListScrollPosition(String folderUri, Parcelable savedPosition);
}
