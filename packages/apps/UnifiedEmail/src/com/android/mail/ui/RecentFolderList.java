/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.ui;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.android.mail.content.ObjectCursor;
import com.android.mail.providers.Account;
import com.android.mail.providers.AccountObserver;
import com.android.mail.providers.Folder;
import com.android.mail.providers.Settings;
import com.android.mail.providers.UIProvider.FolderType;
import com.android.mail.utils.FolderUri;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.LruCache;
import com.android.mail.utils.Utils;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A self-updating list of folder canonical names for the N most recently touched folders, ordered
 * from least-recently-touched to most-recently-touched. N is a fixed size determined upon
 * creation.
 *
 * RecentFoldersCache returns lists of this type, and will keep them updated when observers are
 * registered on them.
 *
 */
public final class RecentFolderList {
    private static final String TAG = "RecentFolderList";
    /** The application context */
    private final Context mContext;
    /** The current account */
    private Account mAccount = null;

    /** The actual cache: map of folder URIs to folder objects. */
    private final LruCache<String, RecentFolderListEntry> mFolderCache;
    /**
     *  We want to show at most five recent folders
     */
    private final static int MAX_RECENT_FOLDERS = 5;
    /**
     *  We exclude the default inbox for the account and the current folder; these might be the
     *  same, but we'll allow for both
     */
    private final static int MAX_EXCLUDED_FOLDERS = 2;

    private final AccountObserver mAccountObserver = new AccountObserver() {
        @Override
        public void onChanged(Account newAccount) {
            setCurrentAccount(newAccount);
        }
    };

    /**
     * Compare based on alphanumeric name of the folder, ignoring case.
     */
    private static final Comparator<Folder> ALPHABET_IGNORECASE = new Comparator<Folder>() {
        @Override
        public int compare(Folder lhs, Folder rhs) {
            return lhs.name.compareToIgnoreCase(rhs.name);
        }
    };
    /**
     * Class to store the recent folder list asynchronously.
     */
    private class StoreRecent extends AsyncTask<Void, Void, Void> {
        /**
         * Copy {@link RecentFolderList#mAccount} in case the account changes between when the
         * AsyncTask is created and when it is executed.
         */
        @SuppressWarnings("hiding")
        private final Account mAccount;
        private final Folder mFolder;

        /**
         * Create a new asynchronous task to store the recent folder list. Both the account
         * and the folder should be non-null.
         * @param account the current account for this folder.
         * @param folder the folder which is to be stored.
         */
        public StoreRecent(Account account, Folder folder) {
            assert (account != null && folder != null);
            mAccount = account;
            mFolder = folder;
        }

        @Override
        protected Void doInBackground(Void... v) {
            final Uri uri = mAccount.recentFolderListUri;
            if (!Utils.isEmpty(uri)) {
                ContentValues values = new ContentValues();
                // Only the folder URIs are provided. Providers are free to update their specific
                // information, though most will probably write the current timestamp.
                values.put(mFolder.folderUri.fullUri.toString(), 0);
                LogUtils.i(TAG, "Save: %s", mFolder.name);
                mContext.getContentResolver().update(uri, values, null, null);
            }
            return null;
        }
    }

    /**
     * Create a Recent Folder List from the given account. This will query the UIProvider to
     * retrieve the RecentFolderList from persistent storage (if any).
     * @param context the context for the activity
     */
    public RecentFolderList(Context context) {
        mFolderCache = new LruCache<String, RecentFolderListEntry>(
                MAX_RECENT_FOLDERS + MAX_EXCLUDED_FOLDERS);
        mContext = context;
    }

    /**
     * Initialize the {@link RecentFolderList} with a controllable activity.
     * @param activity the underlying activity
     */
    public void initialize(ControllableActivity activity){
        setCurrentAccount(mAccountObserver.initialize(activity.getAccountController()));
    }

    /**
     * Change the current account. When a cursor over the recent folders for this account is
     * available, the client <b>must</b> call {@link
     * #loadFromUiProvider(com.android.mail.content.ObjectCursor)} with the updated
     * cursor. Till then, the recent account list will be empty.
     * @param account the new current account
     */
    private void setCurrentAccount(Account account) {
        final boolean accountSwitched = (mAccount == null) || !mAccount.matches(account);
        mAccount = account;
        // Clear the cache only if we moved from alice@example.com -> alice@work.com
        if (accountSwitched) {
            mFolderCache.clear();
        }
    }

    /**
     * Load the account information from the UI provider given the cursor over the recent folders.
     * @param c a cursor over the recent folders.
     */
    public void loadFromUiProvider(ObjectCursor<Folder> c) {
        if (mAccount == null || c == null) {
            LogUtils.e(TAG, "RecentFolderList.loadFromUiProvider: bad input. mAccount=%s,cursor=%s",
                    mAccount, c);
            return;
        }
        LogUtils.d(TAG, "Number of recents = %d", c.getCount());
        if (!c.moveToLast()) {
            LogUtils.e(TAG, "Not able to move to last in recent labels cursor");
            return;
        }
        // Add them backwards, since the most recent values are at the beginning in the cursor.
        // This enables older values to fall off the LRU cache. Also, read all values, just in case
        // there are duplicates in the cursor.
        do {
            final Folder folder = c.getModel();
            final RecentFolderListEntry entry = new RecentFolderListEntry(folder);
            mFolderCache.putElement(folder.folderUri.fullUri.toString(), entry);
            LogUtils.v(TAG, "Account %s, Recent: %s", mAccount.name, folder.name);
        } while (c.moveToPrevious());
    }

    /**
     * Marks the given folder as 'accessed' by the user interface, its entry is updated in the
     * recent folder list, and the current time is written to the provider. This should never
     * be called with a null folder.
     * @param folder the folder we touched
     */
    public void touchFolder(Folder folder, Account account) {
        // We haven't got a valid account yet, cannot proceed.
        if (mAccount == null || !mAccount.equals(account)) {
            if (account != null) {
                setCurrentAccount(account);
            } else {
                LogUtils.w(TAG, "No account set for setting recent folders?");
                return;
            }
        }
        assert (folder != null);

        if (folder.isProviderFolder() || folder.isType(FolderType.SEARCH)) {
            LogUtils.d(TAG, "Not touching recent folder because it's provider or search folder");
            return;
        }

        final RecentFolderListEntry entry = new RecentFolderListEntry(folder);
        mFolderCache.putElement(folder.folderUri.fullUri.toString(), entry);
        new StoreRecent(mAccount, folder).execute();
    }

    /**
     * Generate a sorted list of recent folders, excluding the passed in folder (if any) and
     * default inbox for the current account. This must be called <em>after</em>
     * {@link #setCurrentAccount(Account)} has been called.
     * Returns a list of size {@value #MAX_RECENT_FOLDERS} or smaller.
     * @param excludedFolderUri the uri of folder to be excluded (typically the current folder)
     */
    public ArrayList<Folder> getRecentFolderList(final FolderUri excludedFolderUri) {
        final ArrayList<FolderUri> excludedUris = new ArrayList<FolderUri>();
        if (excludedFolderUri != null) {
            excludedUris.add(excludedFolderUri);
        }
        final FolderUri defaultInbox = (mAccount == null)
                ? FolderUri.EMPTY
                : new FolderUri(Settings.getDefaultInboxUri(mAccount.settings));
        if (!defaultInbox.equals(FolderUri.EMPTY)) {
            excludedUris.add(defaultInbox);
        }
        final List<RecentFolderListEntry> recent = Lists.newArrayList();
        recent.addAll(mFolderCache.values());
        Collections.sort(recent);

        final ArrayList<Folder> recentFolders = Lists.newArrayList();
        for (final RecentFolderListEntry entry : recent) {
            if (!excludedUris.contains(entry.mFolder.folderUri)) {
                recentFolders.add(entry.mFolder);
            }
            if (recentFolders.size() == MAX_RECENT_FOLDERS) {
                break;
            }
        }

        // Sort the values as the very last step.
        Collections.sort(recentFolders, ALPHABET_IGNORECASE);

        return recentFolders;
    }

    /**
     * Destroys this instance. The object is unusable after this has been called.
     */
    public void destroy() {
        mAccountObserver.unregisterAndDestroy();
    }

    private static class RecentFolderListEntry implements Comparable<RecentFolderListEntry> {
        private static final AtomicInteger SEQUENCE_GENERATOR = new AtomicInteger();

        private final Folder mFolder;
        private final int mSequence;

        RecentFolderListEntry(Folder folder) {
            mFolder = folder;
            mSequence = SEQUENCE_GENERATOR.getAndIncrement();
        }

        /**
         * Ensure that RecentFolderListEntry objects with greater sequence number will appear
         * before objects with lower sequence numbers
         */
        @Override
        public int compareTo(RecentFolderListEntry t) {
            return t.mSequence - mSequence;
        }
    }
}
