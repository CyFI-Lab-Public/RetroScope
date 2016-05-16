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

import android.app.Activity;
import android.content.AsyncTaskLoader;
import android.net.Uri;

import com.android.mail.browse.ConversationCursor;
import com.android.mail.providers.Account;
import com.android.mail.providers.UIProvider.AccountCapabilities;
import com.android.mail.utils.LogUtils;

import java.util.ArrayList;

public class ConversationCursorLoader extends AsyncTaskLoader<ConversationCursor> {
    private static final String TAG = "ConversationCursorLoader";
    private final Uri mUri;
    private boolean mInitialConversationLimit;
    private final ConversationCursor mConversationCursor;
    private boolean mInit = false;
    private boolean mClosed = false;
    private boolean mRetain = false;
    private boolean mRetained = false;
    private final String mName;

    /** Only used for debugging. Turn {@link #DEBUG} on to make this useful. */
    private static final boolean DEBUG = false;
    private static final ArrayList<ConversationCursorLoader> sLoaders =
            new ArrayList<ConversationCursorLoader>();

    public ConversationCursorLoader(Activity activity, Account account, Uri uri, String name) {
        super(activity);
        mUri = uri;
        mName = name;
        mInitialConversationLimit =
                account.supportsCapability(AccountCapabilities.INITIAL_CONVERSATION_LIMIT);
        // Initialize the state of the conversation cursor
        mConversationCursor = new ConversationCursor(
                activity, mUri, mInitialConversationLimit, name);
        addLoader();
    }

    private static void dumpLoaders() {
        if (DEBUG) {
            LogUtils.d(TAG, "Loaders: ");
            for (ConversationCursorLoader loader: sLoaders) {
                LogUtils.d(TAG, " >> " + loader.mName + " (" + loader.mUri + ")");
            }
        }
    }

    private void addLoader() {
        if (DEBUG) {
            LogUtils.d(TAG, "Add loader: " + mUri);
            sLoaders.add(this);
            if (sLoaders.size() > 1) {
                dumpLoaders();
            }
        }
    }

    /**
     * Indicate whether the loader's cursor should be retained after reset
     * @param state whether this laoder's cursor should be retained
     */
    public void retainCursor(boolean state) {
        mRetain = state;
    }

    @Override
    public void onReset() {
        if (!mRetain) {
            // Mark the cursor as disabled
            mConversationCursor.disable();
            mClosed = true;
            if (DEBUG) {
                LogUtils.d(TAG, "Reset loader/disable cursor: " + mName);
                sLoaders.remove(this);
                if (!sLoaders.isEmpty()) {
                    dumpLoaders();
                }
            }
        } else {
            if (DEBUG) {
                LogUtils.d(TAG, "Reset loader/retain cursor: " + mName);
                mRetained = true;
            }
        }
    }

    @Override
    public ConversationCursor loadInBackground() {
        if (!mInit) {
            mConversationCursor.load();
            mInit = true;
        }
        return mConversationCursor;
    }

    @Override
    protected void onStartLoading() {
        if (mClosed) {
            mClosed = false;
            mConversationCursor.load();
            addLoader();
            if (DEBUG) {
                LogUtils.d(TAG, "Restarting reset loader: " + mName);
            }
        } else if (mRetained) {
            mRetained = false;
            if (DEBUG) {
                LogUtils.d(TAG, "Resuming retained loader: " + mName);
            }
        }
        forceLoad();
        mConversationCursor.resume();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
        mConversationCursor.pause();
    }
}
