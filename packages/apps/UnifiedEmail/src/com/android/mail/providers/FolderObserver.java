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


package com.android.mail.providers;

import com.android.mail.ui.FolderController;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import android.database.DataSetObserver;

/**
 * A simple extension of {@link android.database.DataSetObserver} to provide the updated Folder in
 * {@link #onChanged(Folder)} when the Folder changes. Initializing the object registers with
 * the observer with the {@link com.android.mail.ui.FolderController} provided. The object will then begin to
 * receive {@link #onChanged(Folder)} till {@link #unregisterAndDestroy()} is called.
 * <p>
 * To implement an {@link FolderObserver}, you need to implement the {@link #onChanged(Folder)}
 * method.
 */
public abstract class FolderObserver extends DataSetObserver {
    /**
     * The FolderController that the observer is registered with.
     */
    private FolderController mController;

    private static final String LOG_TAG = LogTag.getLogTag();

    /**
     * The no-argument constructor leaves the object unusable till
     * {@link #initialize(FolderController)} is called.
     */
    public FolderObserver () {
    }

    /**
     * Initializes an {@link FolderObserver} object that receives a call to
     * {@link #onChanged(Folder)} when the controller changes the Folder.
     *
     * @param controller
     */
    public Folder initialize(FolderController controller) {
        if (controller == null) {
            LogUtils.wtf(LOG_TAG, "FolderObserver initialized with null controller!");
        }
        mController = controller;
        mController.registerFolderObserver(this);
        return mController.getFolder();
    }

    @Override
    public final void onChanged() {
        if (mController == null) {
            return;
        }
        onChanged(mController.getFolder());
    }

    /**
     * Callback invoked when the Folder object is changed.  Since {@link Folder} objects are
     * immutable, updates can be received on changes to individual settings (sync on/off)
     * in addition to changes of Folders: alice@example.com -> bob@example.com.
     * The updated Folder is passed as the argument.
     * @param newFolder
     */
    public abstract void onChanged(Folder newFolder);

    /**
     * Return the current folder.
     * @return
     */
    public final Folder getFolder() {
        if (mController == null) {
            return null;
        }
        return mController.getFolder();
    }

    /**
     * Unregisters for Folder changes and makes the object unusable.
     */
    public void unregisterAndDestroy() {
        if (mController == null) {
            return;
        }
        mController.unregisterFolderObserver(this);
    }
}
