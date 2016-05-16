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

import android.app.LoaderManager;
import android.os.Bundle;
import android.widget.Adapter;

import com.android.mail.browse.ConversationCursor;
import com.android.mail.providers.Folder;

/**
 * An interface for a view that can be inserted into an {@link AnimatedAdapter} at an arbitrary
 * point. The methods described here control whether the view gets displayed, and what it displays.
 */
public interface ConversationSpecialItemView {
    /**
     * Called when there as an update to the information being displayed.
     *
     * @param cursor The {@link ConversationCursor}. May be <code>null</code>
     */
    void onUpdate(Folder folder, ConversationCursor cursor);

    /**
     * Called before returning this view from
     * {@link Adapter#getView(int, android.view.View, android.view.ViewGroup)}
     */
    void onGetView();

    /**
     * Returns whether this view is to be displayed in the list or not. A view can be added freely
     * and it might decide to disable itself by returning false here.
     * @return true if this view should be displayed, false otherwise.
     */
    boolean getShouldDisplayInList();

    /**
     * Returns the position (0 indexed) where this element expects to be inserted.
     * @return
     */
    int getPosition();

    void setAdapter(AnimatedAdapter adapter);

    void bindFragment(LoaderManager loaderManager, Bundle savedInstanceState);

    /**
     * Called when the view is being destroyed.
     */
    void cleanup();

    /**
     * Called when a regular conversation item was clicked.
     */
    void onConversationSelected();

    /**
     * Called whenever Cab Mode has been entered via long press or selecting a sender image.
     */
    void onCabModeEntered();

    /**
     * Called whenever Cab Mode has been exited.
     */
    void onCabModeExited();

    /** Returns whether this special view is enabled (= accepts user taps). */
    boolean acceptsUserTaps();

    /** Called when the conversation list's visibility changes */
    void onConversationListVisibilityChanged(boolean visible);

    /**
     * Saves any state for the view to the fragment so it will be restored on configuration change
     */
    void saveInstanceState(Bundle outState);
}
