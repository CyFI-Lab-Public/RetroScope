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

import android.widget.ListView;

/**
 * A drawer that is shown in one pane mode, as a pull-out from the left.  All the
 * implementation is inherited from the FolderListFragment.
 *
 * The drawer shows a list of accounts, the recent folders, and a list of top-level folders for
 * the given account. This fragment is created using no arguments, it gets all its state from the
 * controller in {@link #onActivityCreated(android.os.Bundle)}. In particular, it gets the current
 * account, the list of accounts, and the current folder from the {@link ControllableActivity}.
 *
 * Once it has this information, the drawer sets itself up to observe for changes and allows the
 * user to change folders and accounts.
 *
 * The drawer is always instantiated through XML resources: in one_pane_activity.xml and in
 * two_pane_activity.xml
 */
public class DrawerFragment extends FolderListFragment {
    /**
     * The only way a drawer is constructed is through XML layouts, and so it needs no constructor
     * like {@link FolderListFragment#ofTopLevelTree(android.net.Uri, java.util.ArrayList}
     */
    public DrawerFragment() {
        super();
        // Drawer is always divided: it shows groups for inboxes, recent folders and all other
        // folders.
        mIsDivided = true;
        // The drawer also switches accounts, so don't hide accounts.
        mHideAccounts = false;
    }

    @Override
    protected int getListViewChoiceMode() {
        // Always let one item be selected
        return ListView.CHOICE_MODE_SINGLE;
    }
}
