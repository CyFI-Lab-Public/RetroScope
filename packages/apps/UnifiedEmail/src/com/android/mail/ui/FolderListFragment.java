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
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.text.BidiFormatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.android.mail.R;
import com.android.mail.adapter.DrawerItem;
import com.android.mail.analytics.Analytics;
import com.android.mail.content.ObjectCursor;
import com.android.mail.content.ObjectCursorLoader;
import com.android.mail.providers.Account;
import com.android.mail.providers.AccountObserver;
import com.android.mail.providers.AllAccountObserver;
import com.android.mail.providers.DrawerClosedObserver;
import com.android.mail.providers.Folder;
import com.android.mail.providers.FolderObserver;
import com.android.mail.providers.FolderWatcher;
import com.android.mail.providers.RecentFolderObserver;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.FolderType;
import com.android.mail.utils.FolderUri;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This fragment shows the list of folders and the list of accounts. Prior to June 2013,
 * the mail application had a spinner in the top action bar. Now, the list of accounts is displayed
 * in a drawer along with the list of folders.
 *
 * This class has the following use-cases:
 * <ul>
 *     <li>
 *         Show a list of accounts and a divided list of folders. In this case, the list shows
 *         Accounts, Inboxes, Recent Folders, All folders.
 *         Tapping on Accounts takes the user to the default Inbox for that account. Tapping on
 *         folders switches folders.
 *         This is created through XML resources as a {@link DrawerFragment}. Since it is created
 *         through resources, it receives all arguments through callbacks.
 *     </li>
 *     <li>
 *         Show a list of folders for a specific level. At the top-level, this shows Inbox, Sent,
 *         Drafts, Starred, and any user-created folders. For providers that allow nested folders,
 *         this will only show the folders at the top-level.
 *         <br /> Tapping on a parent folder creates a new fragment with the child folders at
 *         that level.
 *     </li>
 *     <li>
 *         Shows a list of folders that can be turned into widgets/shortcuts. This is used by the
 *         {@link FolderSelectionActivity} to allow the user to create a shortcut or widget for
 *         any folder for a given account.
 *     </li>
 * </ul>
 */
public class FolderListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<ObjectCursor<Folder>> {
    private static final String LOG_TAG = LogTag.getLogTag();
    /** The parent activity */
    private ControllableActivity mActivity;
    private BidiFormatter mBidiFormatter;
    /** The underlying list view */
    private ListView mListView;
    /** URI that points to the list of folders for the current account. */
    private Uri mFolderListUri;
    /**
     * True if you want a divided FolderList. A divided folder list shows the following groups:
     * Inboxes, Recent Folders, All folders.
     *
     * An undivided FolderList shows all folders without any divisions and without recent folders.
     * This is true only for the drawer: for all others it is false.
     */
    protected boolean mIsDivided = false;
    /** True if the folder list belongs to a folder selection activity (one account only) */
    protected boolean mHideAccounts = true;
    /** An {@link ArrayList} of {@link FolderType}s to exclude from displaying. */
    private ArrayList<Integer> mExcludedFolderTypes;
    /** Object that changes folders on our behalf. */
    private FolderSelector mFolderChanger;
    /** Object that changes accounts on our behalf */
    private AccountController mAccountController;

    /** The currently selected folder (the folder being viewed).  This is never null. */
    private FolderUri mSelectedFolderUri = FolderUri.EMPTY;
    /**
     * The current folder from the controller.  This is meant only to check when the unread count
     * goes out of sync and fixing it.
     */
    private Folder mCurrentFolderForUnreadCheck;
    /** Parent of the current folder, or null if the current folder is not a child. */
    private Folder mParentFolder;

    private static final int FOLDER_LIST_LOADER_ID = 0;
    /** Loader id for the list of all folders in the account */
    private static final int ALL_FOLDER_LIST_LOADER_ID = 1;
    /** Key to store {@link #mParentFolder}. */
    private static final String ARG_PARENT_FOLDER = "arg-parent-folder";
    /** Key to store {@link #mFolderListUri}. */
    private static final String ARG_FOLDER_LIST_URI = "arg-folder-list-uri";
    /** Key to store {@link #mExcludedFolderTypes} */
    private static final String ARG_EXCLUDED_FOLDER_TYPES = "arg-excluded-folder-types";

    private static final String BUNDLE_LIST_STATE = "flf-list-state";
    private static final String BUNDLE_SELECTED_FOLDER = "flf-selected-folder";
    private static final String BUNDLE_SELECTED_TYPE = "flf-selected-type";

    private FolderListFragmentCursorAdapter mCursorAdapter;
    /** Observer to wait for changes to the current folder so we can change the selected folder */
    private FolderObserver mFolderObserver = null;
    /** Listen for account changes. */
    private AccountObserver mAccountObserver = null;
    /** Listen for account changes. */
    private DrawerClosedObserver mDrawerObserver = null;
    /** Listen to changes to list of all accounts */
    private AllAccountObserver mAllAccountsObserver = null;
    /**
     * Type of currently selected folder: {@link DrawerItem#FOLDER_INBOX},
     * {@link DrawerItem#FOLDER_RECENT} or {@link DrawerItem#FOLDER_OTHER}.
     * Set as {@link DrawerItem#UNSET} to begin with, as there is nothing selected yet.
     */
    private int mSelectedFolderType = DrawerItem.UNSET;
    /** The current account according to the controller */
    private Account mCurrentAccount;
    /** The account we will change to once the drawer (if any) is closed */
    private Account mNextAccount = null;
    /** The folder we will change to once the drawer (if any) is closed */
    private Folder mNextFolder = null;

    /**
     * Constructor needs to be public to handle orientation changes and activity lifecycle events.
     */
    public FolderListFragment() {
        super();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.setLength(sb.length() - 1);
        sb.append(" folder=");
        sb.append(mFolderListUri);
        sb.append(" parent=");
        sb.append(mParentFolder);
        sb.append(" adapterCount=");
        sb.append(mCursorAdapter != null ? mCursorAdapter.getCount() : -1);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Creates a new instance of {@link FolderListFragment}, initialized
     * to display the folder and its immediate children.
     * @param folder parent folder whose children are shown
     *
     */
    public static FolderListFragment ofTree(Folder folder) {
        final FolderListFragment fragment = new FolderListFragment();
        fragment.setArguments(getBundleFromArgs(folder, folder.childFoldersListUri, null));
        return fragment;
    }

    /**
     * Creates a new instance of {@link FolderListFragment}, initialized
     * to display the top level: where we have no parent folder, but we have a list of folders
     * from the account.
     * @param folderListUri the URI which contains all the list of folders
     * @param excludedFolderTypes A list of {@link FolderType}s to exclude from displaying
     */
    public static FolderListFragment ofTopLevelTree(Uri folderListUri,
            final ArrayList<Integer> excludedFolderTypes) {
        final FolderListFragment fragment = new FolderListFragment();
        fragment.setArguments(getBundleFromArgs(null, folderListUri, excludedFolderTypes));
        return fragment;
    }

    /**
     * Construct a bundle that represents the state of this fragment.
     *
     * @param parentFolder non-null for trees, the parent of this list
     * @param folderListUri the URI which contains all the list of folders
     * @param excludedFolderTypes if non-null, this indicates folders to exclude in lists.
     * @return Bundle containing parentFolder, divided list boolean and
     *         excluded folder types
     */
    private static Bundle getBundleFromArgs(Folder parentFolder, Uri folderListUri,
            final ArrayList<Integer> excludedFolderTypes) {
        final Bundle args = new Bundle(3);
        if (parentFolder != null) {
            args.putParcelable(ARG_PARENT_FOLDER, parentFolder);
        }
        if (folderListUri != null) {
            args.putString(ARG_FOLDER_LIST_URI, folderListUri.toString());
        }
        if (excludedFolderTypes != null) {
            args.putIntegerArrayList(ARG_EXCLUDED_FOLDER_TYPES, excludedFolderTypes);
        }
        return args;
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);
        // Strictly speaking, we get back an android.app.Activity from getActivity. However, the
        // only activity creating a ConversationListContext is a MailActivity which is of type
        // ControllableActivity, so this cast should be safe. If this cast fails, some other
        // activity is creating ConversationListFragments. This activity must be of type
        // ControllableActivity.
        final Activity activity = getActivity();
        if (! (activity instanceof ControllableActivity)){
            LogUtils.wtf(LOG_TAG, "FolderListFragment expects only a ControllableActivity to" +
                    "create it. Cannot proceed.");
            return;
        }
        mActivity = (ControllableActivity) activity;
        mBidiFormatter = BidiFormatter.getInstance();
        final FolderController controller = mActivity.getFolderController();
        // Listen to folder changes in the future
        mFolderObserver = new FolderObserver() {
            @Override
            public void onChanged(Folder newFolder) {
                setSelectedFolder(newFolder);
            }
        };
        final Folder currentFolder;
        if (controller != null) {
            // Only register for selected folder updates if we have a controller.
            currentFolder = mFolderObserver.initialize(controller);
            mCurrentFolderForUnreadCheck = currentFolder;
        } else {
            currentFolder = null;
        }

        // Initialize adapter for folder/heirarchical list.  Note this relies on
        // mActivity being initialized.
        final Folder selectedFolder;
        if (mParentFolder != null) {
            mCursorAdapter = new HierarchicalFolderListAdapter(null, mParentFolder);
            selectedFolder = mActivity.getHierarchyFolder();
        } else {
            mCursorAdapter = new FolderListAdapter(mIsDivided);
            selectedFolder = currentFolder;
        }
        // Is the selected folder fresher than the one we have restored from a bundle?
        if (selectedFolder != null
                && !selectedFolder.folderUri.equals(mSelectedFolderUri)) {
            setSelectedFolder(selectedFolder);
        }

        // Assign observers for current account & all accounts
        final AccountController accountController = mActivity.getAccountController();
        mAccountObserver = new AccountObserver() {
            @Override
            public void onChanged(Account newAccount) {
                setSelectedAccount(newAccount);
            }
        };
        mFolderChanger = mActivity.getFolderSelector();
        if (accountController != null) {
            // Current account and its observer.
            setSelectedAccount(mAccountObserver.initialize(accountController));
            // List of all accounts and its observer.
            mAllAccountsObserver = new AllAccountObserver(){
                @Override
                public void onChanged(Account[] allAccounts) {
                    mCursorAdapter.notifyAllAccountsChanged();
                }
            };
            mAllAccountsObserver.initialize(accountController);
            mAccountController = accountController;

            // Observer for when the drawer is closed
            mDrawerObserver = new DrawerClosedObserver() {
                @Override
                public void onDrawerClosed() {
                    // First, check if there's a folder to change to
                    if (mNextFolder != null) {
                        mFolderChanger.onFolderSelected(mNextFolder);
                        mNextFolder = null;
                    }
                    // Next, check if there's an account to change to
                    if (mNextAccount != null) {
                        mAccountController.switchToDefaultInboxOrChangeAccount(mNextAccount);
                        mNextAccount = null;
                    }
                }
            };
            mDrawerObserver.initialize(accountController);
        }

        if (mActivity.isFinishing()) {
            // Activity is finishing, just bail.
            return;
        }

        mListView.setChoiceMode(getListViewChoiceMode());

        setListAdapter(mCursorAdapter);
    }

    /**
     * Set the instance variables from the arguments provided here.
     * @param args bundle of arguments with keys named ARG_*
     */
    private void setInstanceFromBundle(Bundle args) {
        if (args == null) {
            return;
        }
        mParentFolder = args.getParcelable(ARG_PARENT_FOLDER);
        final String folderUri = args.getString(ARG_FOLDER_LIST_URI);
        if (folderUri != null) {
            mFolderListUri = Uri.parse(folderUri);
        }
        mExcludedFolderTypes = args.getIntegerArrayList(ARG_EXCLUDED_FOLDER_TYPES);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedState) {
        setInstanceFromBundle(getArguments());

        final View rootView = inflater.inflate(R.layout.folder_list, null);
        mListView = (ListView) rootView.findViewById(android.R.id.list);
        mListView.setEmptyView(null);
        mListView.setDivider(null);
        if (savedState != null && savedState.containsKey(BUNDLE_LIST_STATE)) {
            mListView.onRestoreInstanceState(savedState.getParcelable(BUNDLE_LIST_STATE));
        }
        if (savedState != null && savedState.containsKey(BUNDLE_SELECTED_FOLDER)) {
            mSelectedFolderUri =
                    new FolderUri(Uri.parse(savedState.getString(BUNDLE_SELECTED_FOLDER)));
            mSelectedFolderType = savedState.getInt(BUNDLE_SELECTED_TYPE);
        } else if (mParentFolder != null) {
            mSelectedFolderUri = mParentFolder.folderUri;
            // No selected folder type required for hierarchical lists.
        }

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mListView != null) {
            outState.putParcelable(BUNDLE_LIST_STATE, mListView.onSaveInstanceState());
        }
        if (mSelectedFolderUri != null) {
            outState.putString(BUNDLE_SELECTED_FOLDER, mSelectedFolderUri.toString());
        }
        outState.putInt(BUNDLE_SELECTED_TYPE, mSelectedFolderType);
    }

    @Override
    public void onDestroyView() {
        if (mCursorAdapter != null) {
            mCursorAdapter.destroy();
        }
        // Clear the adapter.
        setListAdapter(null);
        if (mFolderObserver != null) {
            mFolderObserver.unregisterAndDestroy();
            mFolderObserver = null;
        }
        if (mAccountObserver != null) {
            mAccountObserver.unregisterAndDestroy();
            mAccountObserver = null;
        }
        if (mAllAccountsObserver != null) {
            mAllAccountsObserver.unregisterAndDestroy();
            mAllAccountsObserver = null;
        }
        if (mDrawerObserver != null) {
            mDrawerObserver.unregisterAndDestroy();
            mDrawerObserver = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        viewFolderOrChangeAccount(position);
    }

    private Folder getDefaultInbox(Account account) {
        if (account == null || mCursorAdapter == null) {
            return null;
        }
        return mCursorAdapter.getDefaultInbox(account);
    }

    private void changeAccount(final Account account) {
        // Switching accounts takes you to the default inbox for that account.
        mSelectedFolderType = DrawerItem.FOLDER_INBOX;
        mNextAccount = account;
        mAccountController.closeDrawer(true, mNextAccount, getDefaultInbox(mNextAccount));
        Analytics.getInstance().sendEvent("switch_account", "drawer_account_switch", null, 0);
    }

    /**
     * Display the conversation list from the folder at the position given.
     * @param position a zero indexed position into the list.
     */
    private void viewFolderOrChangeAccount(int position) {
        final Object item = getListAdapter().getItem(position);
        LogUtils.d(LOG_TAG, "viewFolderOrChangeAccount(%d): %s", position, item);
        final Folder folder;
        int folderType = DrawerItem.UNSET;

        if (item instanceof DrawerItem) {
            final DrawerItem drawerItem = (DrawerItem) item;
            // Could be a folder or account.
            final int itemType = mCursorAdapter.getItemType(drawerItem);
            if (itemType == DrawerItem.VIEW_ACCOUNT) {
                // Account, so switch.
                folder = null;
                final Account account = drawerItem.mAccount;

                if (account != null && account.settings.defaultInbox.equals(mSelectedFolderUri)) {
                    // We're already in the default inbox for account, just re-check item ...
                    final int defaultInboxPosition = position + 1;
                    if (mListView.getChildAt(defaultInboxPosition) != null) {
                        mListView.setItemChecked(defaultInboxPosition, true);
                    }
                    // ... and close the drawer (no new target folders/accounts)
                    mAccountController.closeDrawer(false, mNextAccount,
                            getDefaultInbox(mNextAccount));
                } else {
                    changeAccount(account);
                }
            } else if (itemType == DrawerItem.VIEW_FOLDER) {
                // Folder type, so change folders only.
                folder = drawerItem.mFolder;
                mSelectedFolderType = folderType = drawerItem.mFolderType;
                LogUtils.d(LOG_TAG, "FLF.viewFolderOrChangeAccount folder=%s, type=%d",
                        folder, mSelectedFolderType);
            } else {
                // Do nothing.
                LogUtils.d(LOG_TAG, "FolderListFragment: viewFolderOrChangeAccount():"
                        + " Clicked on unset item in drawer. Offending item is " + item);
                return;
            }
        } else if (item instanceof Folder) {
            folder = (Folder) item;
        } else {
            // Don't know how we got here.
            LogUtils.wtf(LOG_TAG, "viewFolderOrChangeAccount(): invalid item");
            folder = null;
        }
        if (folder != null) {
            // Not changing the account.
            final Account nextAccount = null;
            // Go to the conversation list for this folder.
            if (!folder.folderUri.equals(mSelectedFolderUri)) {
                mNextFolder = folder;
                mAccountController.closeDrawer(true, nextAccount, folder);

                final String label = (folderType == DrawerItem.FOLDER_RECENT) ? "recent" : "normal";
                Analytics.getInstance().sendEvent("switch_folder", folder.getTypeDescription(),
                        label, 0);

            } else {
                // Clicked on same folder, just close drawer
                mAccountController.closeDrawer(false, nextAccount, folder);
            }
        }
    }

    @Override
    public Loader<ObjectCursor<Folder>> onCreateLoader(int id, Bundle args) {
        mListView.setEmptyView(null);
        final Uri folderListUri;
        if (id == FOLDER_LIST_LOADER_ID) {
            if (mFolderListUri != null) {
                // Folder trees, they specify a URI at construction time.
                folderListUri = mFolderListUri;
            } else {
                // Drawers get the folder list from the current account.
                folderListUri = mCurrentAccount.folderListUri;
            }
        } else if (id == ALL_FOLDER_LIST_LOADER_ID) {
            folderListUri = mCurrentAccount.allFolderListUri;
        } else {
            LogUtils.wtf(LOG_TAG, "FLF.onCreateLoader() with weird type");
            return null;
        }
        return new ObjectCursorLoader<Folder>(mActivity.getActivityContext(), folderListUri,
                UIProvider.FOLDERS_PROJECTION, Folder.FACTORY);
    }

    @Override
    public void onLoadFinished(Loader<ObjectCursor<Folder>> loader, ObjectCursor<Folder> data) {
        if (mCursorAdapter != null) {
            if (loader.getId() == FOLDER_LIST_LOADER_ID) {
                mCursorAdapter.setCursor(data);
            } else if (loader.getId() == ALL_FOLDER_LIST_LOADER_ID) {
                mCursorAdapter.setAllFolderListCursor(data);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<ObjectCursor<Folder>> loader) {
        if (mCursorAdapter != null) {
            if (loader.getId() == FOLDER_LIST_LOADER_ID) {
                mCursorAdapter.setCursor(null);
            } else if (loader.getId() == ALL_FOLDER_LIST_LOADER_ID) {
                mCursorAdapter.setAllFolderListCursor(null);
            }
        }
    }

    /**
     *  Returns the sorted list of accounts. The AAC always has the current list, sorted by
     *  frequency of use.
     * @return a list of accounts, sorted by frequency of use
     */
    private Account[] getAllAccounts() {
        if (mAllAccountsObserver != null) {
            return mAllAccountsObserver.getAllAccounts();
        }
        return new Account[0];
    }

    /**
     * Interface for all cursor adapters that allow setting a cursor and being destroyed.
     */
    private interface FolderListFragmentCursorAdapter extends ListAdapter {
        /** Update the folder list cursor with the cursor given here. */
        void setCursor(ObjectCursor<Folder> cursor);
        /** Update the all folder list cursor with the cursor given here. */
        void setAllFolderListCursor(ObjectCursor<Folder> cursor);
        /**
         * Given an item, find the type of the item, which should only be {@link
         * DrawerItem#VIEW_FOLDER} or {@link DrawerItem#VIEW_ACCOUNT}
         * @return item the type of the item.
         */
        int getItemType(DrawerItem item);
        /** Notify that the all accounts changed. */
        void notifyAllAccountsChanged();
        /** Remove all observers and destroy the object. */
        void destroy();
        /** Notifies the adapter that the data has changed. */
        void notifyDataSetChanged();
        /** Returns default inbox for this account. */
        Folder getDefaultInbox(Account account);
        /** Returns the index of the first selected item, or -1 if no selection */
        int getSelectedPosition();
    }

    /**
     * An adapter for flat folder lists.
     */
    private class FolderListAdapter extends BaseAdapter implements FolderListFragmentCursorAdapter {

        private final RecentFolderObserver mRecentFolderObserver = new RecentFolderObserver() {
            @Override
            public void onChanged() {
                if (!isCursorInvalid()) {
                    recalculateList();
                }
            }
        };
        /** No resource used for string header in folder list */
        private static final int NO_HEADER_RESOURCE = -1;
        /** Cache of most recently used folders */
        private final RecentFolderList mRecentFolders;
        /** True if the list is divided, false otherwise. See the comment on
         * {@link FolderListFragment#mIsDivided} for more information */
        private final boolean mIsDivided;
        /** All the items */
        private List<DrawerItem> mItemList = new ArrayList<DrawerItem>();
        /** Cursor into the folder list. This might be null. */
        private ObjectCursor<Folder> mCursor = null;
        /** Cursor into the all folder list. This might be null. */
        private ObjectCursor<Folder> mAllFolderListCursor = null;
        /** Watcher for tracking and receiving unread counts for mail */
        private FolderWatcher mFolderWatcher = null;
        private boolean mRegistered = false;

        /**
         * Creates a {@link FolderListAdapter}.This is a list of all the accounts and folders.
         *
         * @param isDivided true if folder list is flat, false if divided by label group. See
         *                   the comments on {@link #mIsDivided} for more information
         */
        public FolderListAdapter(boolean isDivided) {
            super();
            mIsDivided = isDivided;
            final RecentFolderController controller = mActivity.getRecentFolderController();
            if (controller != null && mIsDivided) {
                mRecentFolders = mRecentFolderObserver.initialize(controller);
            } else {
                mRecentFolders = null;
            }
            mFolderWatcher = new FolderWatcher(mActivity, this);
            mFolderWatcher.updateAccountList(getAllAccounts());
        }

        @Override
        public void notifyAllAccountsChanged() {
            if (!mRegistered && mAccountController != null) {
                // TODO(viki): Round-about way of setting the watcher. http://b/8750610
                mAccountController.setFolderWatcher(mFolderWatcher);
                mRegistered = true;
            }
            mFolderWatcher.updateAccountList(getAllAccounts());
            recalculateList();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final DrawerItem item = (DrawerItem) getItem(position);
            final View view = item.getView(convertView, parent);
            final int type = item.mType;
            final boolean isSelected = item.isHighlighted(mSelectedFolderUri, mSelectedFolderType);
            if (type == DrawerItem.VIEW_FOLDER) {
                mListView.setItemChecked(position, isSelected);
            }
            // If this is the current folder, also check to verify that the unread count
            // matches what the action bar shows.
            if (type == DrawerItem.VIEW_FOLDER
                    && isSelected
                    && (mCurrentFolderForUnreadCheck != null)
                    && item.mFolder.unreadCount != mCurrentFolderForUnreadCheck.unreadCount) {
                ((FolderItemView) view).overrideUnreadCount(
                        mCurrentFolderForUnreadCheck.unreadCount);
            }
            return view;
        }

        @Override
        public int getViewTypeCount() {
            // Accounts, headers, folders (all parts of drawer view types)
            return DrawerItem.getViewTypes();
        }

        @Override
        public int getItemViewType(int position) {
            return ((DrawerItem) getItem(position)).mType;
        }

        @Override
        public int getCount() {
            return mItemList.size();
        }

        @Override
        public boolean isEnabled(int position) {
            final DrawerItem drawerItem = ((DrawerItem) getItem(position));
            return drawerItem != null && drawerItem.isItemEnabled();
        }

        private Uri getCurrentAccountUri() {
            return mCurrentAccount == null ? Uri.EMPTY : mCurrentAccount.uri;
        }

        @Override
        public boolean areAllItemsEnabled() {
            // We have headers and thus some items are not enabled.
            return false;
        }

        /**
         * Returns all the recent folders from the list given here. Safe to call with a null list.
         * @param recentList a list of all recently accessed folders.
         * @return a valid list of folders, which are all recent folders.
         */
        private List<Folder> getRecentFolders(RecentFolderList recentList) {
            final List<Folder> folderList = new ArrayList<Folder>();
            if (recentList == null) {
                return folderList;
            }
            // Get all recent folders, after removing system folders.
            for (final Folder f : recentList.getRecentFolderList(null)) {
                if (!f.isProviderFolder()) {
                    folderList.add(f);
                }
            }
            return folderList;
        }

        /**
         * Responsible for verifying mCursor, and ensuring any recalculate
         * conditions are met. Also calls notifyDataSetChanged once it's finished
         * populating {@link FolderListAdapter#mItemList}
         */
        private void recalculateList() {
            final List<DrawerItem> newFolderList = new ArrayList<DrawerItem>();
            // Don't show accounts for single-account-based folder selection (i.e. widgets)
            if (!mHideAccounts) {
                recalculateListAccounts(newFolderList);
            }
            recalculateListFolders(newFolderList);
            mItemList = newFolderList;
            // Ask the list to invalidate its views.
            notifyDataSetChanged();
        }

        /**
         * Recalculates the accounts if not null and adds them to the list.
         *
         * @param itemList List of drawer items to populate
         */
        private void recalculateListAccounts(List<DrawerItem> itemList) {
            final Account[] allAccounts = getAllAccounts();
            // Add all accounts and then the current account
            final Uri currentAccountUri = getCurrentAccountUri();
            for (final Account account : allAccounts) {
                final int unreadCount = mFolderWatcher.getUnreadCount(account);
                itemList.add(DrawerItem.ofAccount(mActivity, account, unreadCount,
                        currentAccountUri.equals(account.uri), mBidiFormatter));
            }
            if (mCurrentAccount == null) {
                LogUtils.wtf(LOG_TAG, "recalculateListAccounts() with null current account.");
            }
        }

        /**
         * Recalculates the system, recent and user label lists.
         * This method modifies all the three lists on every single invocation.
         *
         * @param itemList List of drawer items to populate
         */
        private void recalculateListFolders(List<DrawerItem> itemList) {
            // If we are waiting for folder initialization, we don't have any kinds of folders,
            // just the "Waiting for initialization" item. Note, this should only be done
            // when we're waiting for account initialization or initial sync.
            if (isCursorInvalid()) {
                if(!mCurrentAccount.isAccountReady()) {
                    itemList.add(DrawerItem.ofWaitView(mActivity, mBidiFormatter));
                }
                return;
            }

            if (!mIsDivided) {
                // Adapter for a flat list. Everything is a FOLDER_OTHER, and there are no headers.
                do {
                    final Folder f = mCursor.getModel();
                    if (!isFolderTypeExcluded(f)) {
                        itemList.add(DrawerItem.ofFolder(mActivity, f, DrawerItem.FOLDER_OTHER,
                                mBidiFormatter));
                    }
                } while (mCursor.moveToNext());

                return;
            }

            // Otherwise, this is an adapter for a divided list.
            final List<DrawerItem> allFoldersList = new ArrayList<DrawerItem>();
            final List<DrawerItem> inboxFolders = new ArrayList<DrawerItem>();
            do {
                final Folder f = mCursor.getModel();
                if (!isFolderTypeExcluded(f)) {
                    if (f.isInbox()) {
                        inboxFolders.add(DrawerItem.ofFolder(mActivity, f, DrawerItem.FOLDER_INBOX,
                                mBidiFormatter));
                    } else {
                        allFoldersList.add(DrawerItem.ofFolder(mActivity, f,
                                DrawerItem.FOLDER_OTHER, mBidiFormatter));
                    }
                }
            } while (mCursor.moveToNext());

            // If we have the all folder list, verify that the current folder exists
            boolean currentFolderFound = false;
            if (mAllFolderListCursor != null) {
                final String folderName = mSelectedFolderUri.toString();
                LogUtils.d(LOG_TAG, "Checking if all folder list contains %s", folderName);

                if (mAllFolderListCursor.moveToFirst()) {
                    LogUtils.d(LOG_TAG, "Cursor for %s seems reasonably valid", folderName);
                    do {
                        final Folder f = mAllFolderListCursor.getModel();
                        if (!isFolderTypeExcluded(f)) {
                            if (f.folderUri.equals(mSelectedFolderUri)) {
                                LogUtils.d(LOG_TAG, "Found %s !", folderName);
                                currentFolderFound = true;
                            }
                        }
                    } while (!currentFolderFound && mAllFolderListCursor.moveToNext());
                }

                if (!currentFolderFound && mSelectedFolderUri != FolderUri.EMPTY
                        && mCurrentAccount != null && mAccountController != null
                        && mAccountController.isDrawerPullEnabled()) {
                    LogUtils.d(LOG_TAG, "Current folder (%1$s) has disappeared for %2$s",
                            folderName, mCurrentAccount.name);
                    changeAccount(mCurrentAccount);
                }
            }

            // Add all inboxes (sectioned Inboxes included) before recent folders.
            addFolderDivision(itemList, inboxFolders, R.string.inbox_folders_heading);

            // Add recent folders next.
            addRecentsToList(itemList);

            // Add the remaining folders.
            addFolderDivision(itemList, allFoldersList, R.string.all_folders_heading);
        }

        /**
         * Given a list of folders as {@link DrawerItem}s, add them as a group.
         * Passing in a non-0 integer for the resource will enable a header.
         *
         * @param destination List of drawer items to populate
         * @param source List of drawer items representing folders to add to the drawer
         * @param headerStringResource
         *            {@link FolderListAdapter#NO_HEADER_RESOURCE} if no header
         *            is required, or res-id otherwise. The integer is interpreted as the string
         *            for the header's title.
         */
        private void addFolderDivision(List<DrawerItem> destination, List<DrawerItem> source,
                int headerStringResource) {
            if (source.size() > 0) {
                if(headerStringResource != NO_HEADER_RESOURCE) {
                    destination.add(DrawerItem.ofHeader(mActivity, headerStringResource,
                            mBidiFormatter));
                }
                destination.addAll(source);
            }
        }

        /**
         * Add recent folders to the list in order as acquired by the {@link RecentFolderList}.
         *
         * @param destination List of drawer items to populate
         */
        private void addRecentsToList(List<DrawerItem> destination) {
            // If there are recent folders, add them.
            final List<Folder> recentFolderList = getRecentFolders(mRecentFolders);

            // Remove any excluded folder types
            if (mExcludedFolderTypes != null) {
                final Iterator<Folder> iterator = recentFolderList.iterator();
                while (iterator.hasNext()) {
                    if (isFolderTypeExcluded(iterator.next())) {
                        iterator.remove();
                    }
                }
            }

            if (recentFolderList.size() > 0) {
                destination.add(DrawerItem.ofHeader(mActivity, R.string.recent_folders_heading,
                        mBidiFormatter));
                // Recent folders are not queried for position.
                for (Folder f : recentFolderList) {
                    destination.add(DrawerItem.ofFolder(mActivity, f, DrawerItem.FOLDER_RECENT,
                            mBidiFormatter));
                }
            }
        }

        /**
         * Check if the cursor provided is valid.
         * @return True if cursor is invalid, false otherwise
         */
        private boolean isCursorInvalid() {
            return mCursor == null || mCursor.isClosed()|| mCursor.getCount() <= 0
                    || !mCursor.moveToFirst();
        }

        @Override
        public void setCursor(ObjectCursor<Folder> cursor) {
            mCursor = cursor;
            recalculateList();
        }

        @Override
        public void setAllFolderListCursor(final ObjectCursor<Folder> cursor) {
            mAllFolderListCursor = cursor;
            recalculateList();
        }

        @Override
        public Object getItem(int position) {
            // Is there an attempt made to access outside of the drawer item list?
            if (position >= mItemList.size()) {
                return null;
            } else {
                return mItemList.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public final void destroy() {
            mRecentFolderObserver.unregisterAndDestroy();
        }

        @Override
        public Folder getDefaultInbox(Account account) {
            if (mFolderWatcher != null) {
                return mFolderWatcher.getDefaultInbox(account);
            }
            return null;
        }

        @Override
        public int getItemType(DrawerItem item) {
            return item.mType;
        }

        @Override
        public int getSelectedPosition() {
            for (int i = 0; i < mItemList.size(); i++) {
                final DrawerItem item = (DrawerItem) getItem(i);
                final boolean isSelected =
                        item.isHighlighted(mSelectedFolderUri, mSelectedFolderType);
                if (isSelected) {
                    return i;
                }
            }

            return -1;
        }
    }

    private class HierarchicalFolderListAdapter extends ArrayAdapter<Folder>
            implements FolderListFragmentCursorAdapter {

        private static final int PARENT = 0;
        private static final int CHILD = 1;
        private final FolderUri mParentUri;
        private final Folder mParent;
        private final FolderItemView.DropHandler mDropHandler;

        public HierarchicalFolderListAdapter(ObjectCursor<Folder> c, Folder parentFolder) {
            super(mActivity.getActivityContext(), R.layout.folder_item);
            mDropHandler = mActivity;
            mParent = parentFolder;
            mParentUri = parentFolder.folderUri;
            setCursor(c);
        }

        @Override
        public int getViewTypeCount() {
            // Child and Parent
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            final Folder f = getItem(position);
            return f.folderUri.equals(mParentUri) ? PARENT : CHILD;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final FolderItemView folderItemView;
            final Folder folder = getItem(position);
            boolean isParent = folder.folderUri.equals(mParentUri);
            if (convertView != null) {
                folderItemView = (FolderItemView) convertView;
            } else {
                int resId = isParent ? R.layout.folder_item : R.layout.child_folder_item;
                folderItemView = (FolderItemView) LayoutInflater.from(
                        mActivity.getActivityContext()).inflate(resId, null);
            }
            folderItemView.bind(folder, mDropHandler, mBidiFormatter);
            if (folder.folderUri.equals(mSelectedFolderUri)) {
                getListView().setItemChecked(position, true);
                // If this is the current folder, also check to verify that the unread count
                // matches what the action bar shows.
                final boolean unreadCountDiffers = (mCurrentFolderForUnreadCheck != null)
                        && folder.unreadCount != mCurrentFolderForUnreadCheck.unreadCount;
                if (unreadCountDiffers) {
                    folderItemView.overrideUnreadCount(mCurrentFolderForUnreadCheck.unreadCount);
                }
            }
            Folder.setFolderBlockColor(folder, folderItemView.findViewById(R.id.color_block));
            Folder.setIcon(folder, (ImageView) folderItemView.findViewById(R.id.folder_icon));
            return folderItemView;
        }

        @Override
        public void setCursor(ObjectCursor<Folder> cursor) {
            clear();
            if (mParent != null) {
                add(mParent);
            }
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    add(cursor.getModel());
                } while (cursor.moveToNext());
            }
        }

        @Override
        public void setAllFolderListCursor(final ObjectCursor<Folder> cursor) {
            // Not necessary in HierarchicalFolderListAdapter
        }

        @Override
        public void destroy() {
            // Do nothing.
        }

        @Override
        public Folder getDefaultInbox(Account account) {
            return null;
        }

        @Override
        public int getItemType(DrawerItem item) {
            // Always returns folders for now.
            return DrawerItem.VIEW_FOLDER;
        }

        @Override
        public void notifyAllAccountsChanged() {
            // Do nothing. We don't care about changes to all accounts.
        }

        @Override
        public int getSelectedPosition() {
            final int count = getCount();
            for (int i = 0; i < count; i++) {
                final Folder folder = getItem(i);
                final boolean isSelected = folder.folderUri.equals(mSelectedFolderUri);
                if (isSelected) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Sets the currently selected folder safely.
     * @param folder the folder to change to. It is an error to pass null here.
     */
    private void setSelectedFolder(Folder folder) {
        if (folder == null) {
            mSelectedFolderUri = FolderUri.EMPTY;
            mCurrentFolderForUnreadCheck = null;
            LogUtils.e(LOG_TAG, "FolderListFragment.setSelectedFolder(null) called!");
            return;
        }

        final boolean viewChanged =
                !FolderItemView.areSameViews(folder, mCurrentFolderForUnreadCheck);

        // There are two cases in which the folder type is not set by this class.
        // 1. The activity starts up: from notification/widget/shortcut/launcher. Then we have a
        //    folder but its type was never set.
        // 2. The user backs into the default inbox. Going 'back' from the conversation list of
        //    any folder will take you to the default inbox for that account. (If you are in the
        //    default inbox already, back exits the app.)
        // In both these cases, the selected folder type is not set, and must be set.
        if (mSelectedFolderType == DrawerItem.UNSET || (mCurrentAccount != null
                && folder.folderUri.equals(mCurrentAccount.settings.defaultInbox))) {
            mSelectedFolderType =
                    folder.isInbox() ? DrawerItem.FOLDER_INBOX : DrawerItem.FOLDER_OTHER;
        }

        mCurrentFolderForUnreadCheck = folder;
        mSelectedFolderUri = folder.folderUri;
        if (mCursorAdapter != null && viewChanged) {
            mCursorAdapter.notifyDataSetChanged();
        }
    }

    public void updateScroll() {
        final int selectedPosition = mCursorAdapter.getSelectedPosition();
        if (selectedPosition >= 0) {
            // TODO: setSelection() jumps the item to the top of the list "hiding" the accounts
            // TODO: and smoothScrollToPosition() is too slow for lots of labels/folders
            // It's called "setSelection" but it's really more like "jumpScrollToPosition"
            // mListView.setSelection(selectedPosition);
        }
    }

    /**
     * Sets the current account to the one provided here.
     * @param account the current account to set to.
     */
    private void setSelectedAccount(Account account){
        final boolean changed = (account != null) && (mCurrentAccount == null
                || !mCurrentAccount.uri.equals(account.uri));
        mCurrentAccount = account;
        if (changed) {
            // We no longer have proper folder objects. Let the new ones come in
            mCursorAdapter.setCursor(null);
            // If currentAccount is different from the one we set, restart the loader. Look at the
            // comment on {@link AbstractActivityController#restartOptionalLoader} to see why we
            // don't just do restartLoader.
            final LoaderManager manager = getLoaderManager();
            manager.destroyLoader(FOLDER_LIST_LOADER_ID);
            manager.restartLoader(FOLDER_LIST_LOADER_ID, Bundle.EMPTY, this);
            manager.destroyLoader(ALL_FOLDER_LIST_LOADER_ID);
            manager.restartLoader(ALL_FOLDER_LIST_LOADER_ID, Bundle.EMPTY, this);
            // An updated cursor causes the entire list to refresh. No need to refresh the list.
            // But we do need to blank out the current folder, since the account might not be
            // synced.
            mSelectedFolderUri = FolderUri.EMPTY;
            mCurrentFolderForUnreadCheck = null;
        } else if (account == null) {
            // This should never happen currently, but is a safeguard against a very incorrect
            // non-null account -> null account transition.
            LogUtils.e(LOG_TAG, "FLF.setSelectedAccount(null) called! Destroying existing loader.");
            final LoaderManager manager = getLoaderManager();
            manager.destroyLoader(FOLDER_LIST_LOADER_ID);
            manager.destroyLoader(ALL_FOLDER_LIST_LOADER_ID);
        }
    }

    /**
     * Checks if the specified {@link Folder} is a type that we want to exclude from displaying.
     */
    private boolean isFolderTypeExcluded(final Folder folder) {
        if (mExcludedFolderTypes == null) {
            return false;
        }

        for (final int excludedType : mExcludedFolderTypes) {
            if (folder.isType(excludedType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return the choice mode to use for the {@link ListView}
     */
    protected int getListViewChoiceMode() {
        return mAccountController.getFolderListViewChoiceMode();
    }
}
