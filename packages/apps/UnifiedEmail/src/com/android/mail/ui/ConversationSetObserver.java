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

/**
 * Interface for classes that want to respond to changes in conversation sets.  A conversation set
 * is a list of conversations selected by the user to perform an action on. The user could select
 * five conversations and delete them. The five conversations form a set. Constructing such a set
 * involves many user actions: tapping on multiple checkboxes. This interface allows the class to
 * listen to such user actions.
 */
public interface ConversationSetObserver {
    // TODO(viki): Consider passing a mutable instance of ConversationSelectionSet. In the current
    // implementation, the observers can wreck the selection set unknowingly!!

    /**
     * Called when the selection set becomes empty.
     */
    void onSetEmpty();

    /**
     * Handle when the selection set is populated with some items. The observer should not make any
     * modifications to the set while handling this event.
     */
    void onSetPopulated(ConversationSelectionSet set);

    /**
     * Handle when the selection set gets an element added or removed. The observer should not
     * make any modifications to the set while handling this event.
     */
    void onSetChanged(ConversationSelectionSet set);
}
