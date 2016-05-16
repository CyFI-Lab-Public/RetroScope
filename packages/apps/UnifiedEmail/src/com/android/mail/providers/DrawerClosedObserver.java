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

package com.android.mail.providers;

import android.database.DataSetObserver;

import com.android.mail.ui.AccountController;
import com.android.mail.ui.RecentFolderController;

/**
 * Observes when the drawer is closed for the purpose of computing after the drawer is,
 * potentially, off-screen.
 */
public abstract class DrawerClosedObserver extends DataSetObserver {
    private AccountController mController;

    /**
     * The no-argument constructor leaves the object unusable till
     * {@link #initialize(RecentFolderController)} is called.
     */
    public DrawerClosedObserver () {
    }

    /**
     * Initialize the {@link DrawerClosedObserver} object to receive calls when the drawer
     * is closed.
     *
     * @param controller
     */
    public void initialize(AccountController controller) {
        mController = controller;
        mController.registerDrawerClosedObserver(this);
    }

    /**
     * On drawer closed, execute necessary actions. In the case of {@link FolderListFragment}, this
     * includes changing the accounts and then redrawing.
     */
    public abstract void onDrawerClosed();

    @Override
    public final void onChanged() {
        if (mController != null) {
            onDrawerClosed();
        }
    }

    /**
     * Unregisters the {@link DrawerClosedObserver} and makes it unusable.
     */
    public void unregisterAndDestroy() {
        if (mController != null) {
            mController.unregisterDrawerClosedObserver(this);
        }
    }
}