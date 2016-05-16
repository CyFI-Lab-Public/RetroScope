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
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.content.ObjectCursor;
import com.android.mail.content.ObjectCursorLoader;
import com.android.mail.providers.Account;
import com.android.mail.providers.Address;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.MessageInfo;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.AccountCapabilities;
import com.android.mail.providers.UIProvider.ConversationListQueryParameters;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The teaser list item in the conversation list that shows nested folders.
 */
public class NestedFolderTeaserView extends LinearLayout implements ConversationSpecialItemView {
    private static final String LOG_TAG = "NestedFolderTeaserView";

    private boolean mShouldDisplayInList = false;

    private Account mAccount;
    private Uri mFolderListUri;
    private FolderSelector mListener;

    private LoaderManager mLoaderManager = null;
    private AnimatedAdapter mAdapter = null;

    private final SparseArrayCompat<FolderHolder> mFolderHolders =
            new SparseArrayCompat<FolderHolder>();

    private final int mFolderItemUpdateDelayMs;

    private int mAnimatedHeight = -1;

    private ViewGroup mNestedFolderContainer;

    private View mShowMoreFoldersRow;
    private TextView mShowMoreFoldersTextView;
    private TextView mShowMoreFoldersCountTextView;

    /**
     * If <code>true</code> we show a limited set of folders, and a means to show all folders. If
     * <code>false</code>, we show all folders.
     */
    private boolean mCollapsed = true;

    private View mTeaserRightEdge;
    /** Whether we are on a tablet device or not */
    private final boolean mTabletDevice;
    /** When in conversation mode, true if the list is hidden */
    private final boolean mListCollapsible;

    /** If <code>true</code>, the list of folders has updated since the view was last shown. */
    private boolean mListUpdated;

    // Each folder's loader will be this value plus the folder id
    private static final int LOADER_FOLDER_LIST =
            AbstractActivityController.LAST_FRAGMENT_LOADER_ID + 100000;

    /**
     * The maximum number of senders to show in the sender snippet.
     */
    private static final String MAX_SENDERS = "20";

    /**
     * The number of folders to show when the teaser is collapsed.
     */
    private static int sCollapsedFolderThreshold = -1;

    private static class FolderHolder {
        private final View mItemView;
        private final TextView mSendersTextView;
        private final TextView mCountTextView;
        private Folder mFolder;
        private List<String> mUnreadSenders = ImmutableList.of();

        public FolderHolder(final View itemView, final TextView sendersTextView,
                final TextView countTextView) {
            mItemView = itemView;
            mSendersTextView = sendersTextView;
            mCountTextView = countTextView;
        }

        public void setFolder(final Folder folder) {
            mFolder = folder;
        }

        public View getItemView() {
            return mItemView;
        }

        public TextView getSendersTextView() {
            return mSendersTextView;
        }

        public TextView getCountTextView() {
            return mCountTextView;
        }

        public Folder getFolder() {
            return mFolder;
        }

        /**
         * @return a {@link List} of senders of unread messages
         */
        public List<String> getUnreadSenders() {
            return mUnreadSenders;
        }

        public void setUnreadSenders(final List<String> unreadSenders) {
            mUnreadSenders = unreadSenders;
        }

        public static final Comparator<FolderHolder> NAME_COMPARATOR =
                new Comparator<FolderHolder>() {
            @Override
            public int compare(final FolderHolder lhs, final FolderHolder rhs) {
                return lhs.getFolder().name.compareTo(rhs.getFolder().name);
            }
        };
    }

    public NestedFolderTeaserView(final Context context) {
        this(context, null);
    }

    public NestedFolderTeaserView(final Context context, final AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public NestedFolderTeaserView(
            final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = context.getResources();

        if (sCollapsedFolderThreshold < 0) {
            sCollapsedFolderThreshold =
                    resources.getInteger(R.integer.nested_folders_collapse_threshold);
        }

        mFolderItemUpdateDelayMs =
                resources.getInteger(R.integer.folder_item_refresh_delay_ms);

        mTabletDevice = com.android.mail.utils.Utils.useTabletUI(resources);
        mListCollapsible = resources.getBoolean(R.bool.list_collapsible);
    }

    @Override
    protected void onFinishInflate() {
        mNestedFolderContainer = (ViewGroup) findViewById(R.id.nested_folder_container);
        mTeaserRightEdge = findViewById(R.id.teaser_right_edge);

        mShowMoreFoldersRow = findViewById(R.id.show_more_folders_row);
        mShowMoreFoldersRow.setOnClickListener(mShowMoreOnClickListener);

        mShowMoreFoldersTextView = (TextView) findViewById(R.id.show_more_folders_textView);
        mShowMoreFoldersCountTextView =
                (TextView) findViewById(R.id.show_more_folders_count_textView);
    }

    public void bind(final Account account, final FolderSelector listener) {
        mAccount = account;
        mListener = listener;
    }

    /**
     * Creates a {@link FolderHolder}.
     */
    private FolderHolder createFolderHolder(final CharSequence folderName) {
        final View itemView =
                LayoutInflater.from(getContext()).inflate(R.layout.folder_teaser_item, null);

        final ImageView imageView = (ImageView) itemView.findViewById(R.id.folder_imageView);
        imageView.setImageResource(R.drawable.ic_menu_folders_holo_light);
        // Remove background
        imageView.setBackgroundColor(Color.TRANSPARENT);

        ((TextView) itemView.findViewById(R.id.folder_textView)).setText(folderName);
        final TextView sendersTextView = (TextView) itemView.findViewById(R.id.senders_textView);
        final TextView countTextView = (TextView) itemView.findViewById(R.id.count_textView);
        final FolderHolder holder = new FolderHolder(itemView, sendersTextView, countTextView);

        attachOnClickListener(itemView, holder);

        return holder;
    }

    private void attachOnClickListener(final View view, final FolderHolder holder) {
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                mListener.onFolderSelected(holder.getFolder());
            }
        });
    }

    @Override
    public void onUpdate(final Folder folder, final ConversationCursor cursor) {
        mShouldDisplayInList = false; // Assume disabled

        if (folder == null) {
            return;
        }

        final Uri folderListUri = folder.childFoldersListUri;
        if (folderListUri == null) {
            return;
        }

        // If we don't support nested folders, don't show this view
        if (!mAccount.supportsCapability(AccountCapabilities.NESTED_FOLDERS)) {
            return;
        }

        if (mFolderListUri == null || !mFolderListUri.equals(folder.childFoldersListUri)) {
            // We have a new uri
            mFolderListUri = folderListUri;

            // Restart the loader
            mLoaderManager.destroyLoader(LOADER_FOLDER_LIST);
            mLoaderManager.initLoader(LOADER_FOLDER_LIST, null, mFolderListLoaderCallbacks);
        }

        mShouldDisplayInList = true; // Now we know we have something to display
    }

    @Override
    public void onGetView() {
        if (mListUpdated) {
            // Clear out the folder views
            mNestedFolderContainer.removeAllViews();

            // Sort the folders by name
            // TODO(skennedy) recents? starred?
            final ImmutableSortedSet.Builder<FolderHolder> folderHoldersBuilder =
                    new ImmutableSortedSet.Builder<FolderHolder>(FolderHolder.NAME_COMPARATOR);

            for (int i = 0; i < mFolderHolders.size(); i++) {
                folderHoldersBuilder.add(mFolderHolders.valueAt(i));
            }

            final ImmutableSortedSet<FolderHolder> folderHolders = folderHoldersBuilder.build();

            // Add all folder views to the teaser
            int added = 0;
            for (final FolderHolder folderHolder : folderHolders) {
                mNestedFolderContainer.addView(folderHolder.getItemView());
                added++;

                if (added >= sCollapsedFolderThreshold && mCollapsed) {
                    // We will display the rest when "Show more" is clicked
                    break;
                }
            }

            updateShowMoreView();

            mListUpdated = false;
        }
    }

    private final OnClickListener mShowMoreOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            mCollapsed = !mCollapsed;
            mListUpdated = true;
            mAdapter.notifyDataSetChanged();
        }
    };

    private void updateShowMoreView() {
        final int total = mFolderHolders.size();
        final int displayed = mNestedFolderContainer.getChildCount();
        final int notShown = total - displayed;

        if (notShown > 0) {
            // We are not displaying all the folders
            mShowMoreFoldersRow.setVisibility(VISIBLE);
            mShowMoreFoldersTextView.setText(String.format(
                    getContext().getString(R.string.show_n_more_folders), notShown));
            mShowMoreFoldersCountTextView.setVisibility(VISIBLE);

            // Get a count of unread messages in other folders
            int unreadCount = 0;
            for (int i = 0; i < mFolderHolders.size(); i++) {
                final FolderHolder holder = mFolderHolders.valueAt(i);

                if (holder.getItemView().getParent() == null) {
                    // This view is not shown, so we want to use its unread count
                    // TODO(skennedy) We want a "nested" unread count, that includes the unread
                    // count of nested folders
                    unreadCount += holder.getFolder().unreadCount;
                }
            }

            mShowMoreFoldersCountTextView.setText(Integer.toString(unreadCount));
        } else if (displayed > sCollapsedFolderThreshold) {
            // We are expanded
            mShowMoreFoldersRow.setVisibility(VISIBLE);
            mShowMoreFoldersTextView.setText(R.string.hide_folders);
            mShowMoreFoldersCountTextView.setVisibility(GONE);
        } else {
            // We don't need to collapse the folders
            mShowMoreFoldersRow.setVisibility(GONE);
        }
    }

    private void updateViews(final FolderHolder folderHolder) {
        final Folder folder = folderHolder.getFolder();

        final String unreadText = Utils.getUnreadCountString(getContext(), folder.unreadCount);
        folderHolder.getCountTextView().setText(unreadText.isEmpty() ? "0" : unreadText);

        final String sendersText = TextUtils.join(", ", folderHolder.getUnreadSenders());
        folderHolder.getSendersTextView().setText(sendersText);
    }

    @Override
    public boolean getShouldDisplayInList() {
        return mShouldDisplayInList;
    }

    @Override
    public int getPosition() {
        return 0;
    }

    @Override
    public void setAdapter(final AnimatedAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public void bindFragment(final LoaderManager loaderManager, final Bundle savedInstanceState) {
        if (mLoaderManager != null) {
            throw new IllegalStateException("This view has already been bound to a LoaderManager.");
        }

        mLoaderManager = loaderManager;
    }

    @Override
    public void cleanup() {
        // Do nothing
    }

    @Override
    public void onConversationSelected() {
        // Do nothing
    }

    @Override
    public void onCabModeEntered() {
        // Do nothing
    }

    @Override
    public void onCabModeExited() {
        // Do nothing
    }

    @Override
    public void onConversationListVisibilityChanged(final boolean visible) {
        // Do nothing
    }

    @Override
    public void saveInstanceState(final Bundle outState) {
        // Do nothing
    }

    @Override
    public boolean acceptsUserTaps() {
        // The teaser does not allow user tap in the list.
        return false;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (com.android.mail.utils.Utils.getDisplayListRightEdgeEffect(mTabletDevice,
                mListCollapsible, mAdapter.getViewMode())) {
            mTeaserRightEdge.setVisibility(VISIBLE);
        } else {
            mTeaserRightEdge.setVisibility(GONE);
        }

        if (mAnimatedHeight == -1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mAnimatedHeight);
        }
    }

    private static int getLoaderId(final int folderId) {
        return folderId + LOADER_FOLDER_LIST;
    }

    private static int getFolderId(final int loaderId) {
        return loaderId - LOADER_FOLDER_LIST;
    }

    private final LoaderCallbacks<ObjectCursor<Folder>> mFolderListLoaderCallbacks =
            new LoaderCallbacks<ObjectCursor<Folder>>() {
        @Override
        public void onLoaderReset(final Loader<ObjectCursor<Folder>> loader) {
            // Do nothing
        }

        @Override
        public void onLoadFinished(final Loader<ObjectCursor<Folder>> loader,
                final ObjectCursor<Folder> data) {
            if (data != null) {
                // We need to keep track of all current folders in case one has been removed
                final List<Integer> oldFolderIds = new ArrayList<Integer>(mFolderHolders.size());
                for (int i = 0; i < mFolderHolders.size(); i++) {
                    oldFolderIds.add(mFolderHolders.keyAt(i));
                }

                if (data.moveToFirst()) {
                    do {
                        final Folder folder = data.getModel();
                        final FolderHolder holder = mFolderHolders.get(folder.id);

                        if (holder != null) {
                            final Folder oldFolder = holder.getFolder();
                            holder.setFolder(folder);

                            /*
                             * We only need to change anything if the old Folder was null, or the
                             * unread count has changed.
                             */
                            if (oldFolder == null || oldFolder.unreadCount != folder.unreadCount) {
                                populateUnreadSenders(holder, folder.unreadSenders);
                                updateViews(holder);
                            }
                        } else {
                            // Create the holder, and init a loader
                            final FolderHolder newHolder = createFolderHolder(folder.name);
                            newHolder.setFolder(folder);
                            mFolderHolders.put(folder.id, newHolder);

                            // We can not support displaying sender info with nested folders
                            // because it doesn't scale. Disabling it for now, until we can
                            // optimize it.
                            // initFolderLoader(getLoaderId(folder.id));
                            populateUnreadSenders(newHolder, folder.unreadSenders);

                            updateViews(newHolder);

                            mListUpdated = true;
                        }

                        // Note: #remove(int) removes from that POSITION
                        //       #remove(Integer) removes that OBJECT
                        oldFolderIds.remove(Integer.valueOf(folder.id));
                    } while (data.moveToNext());
                }

                for (final int folderId : oldFolderIds) {
                    // We have a folder that no longer exists
                    mFolderHolders.remove(folderId);
                    mLoaderManager.destroyLoader(getLoaderId(folderId));
                    mListUpdated = true;
                }

                // If the list has not changed, we've already updated the counts, etc.
                // If the list has changed, we need to rebuild it
                if (mListUpdated) {
                    mAdapter.notifyDataSetChanged();
                }
            } else {
                LogUtils.w(LOG_TAG, "Problem with folder list cursor returned from loader");
            }
        }

        private void initFolderLoader(final int loaderId) {
            LogUtils.d(LOG_TAG, "Initializing folder loader %d", loaderId);
            mLoaderManager.initLoader(loaderId, null, mFolderLoaderCallbacks);
        }

        @Override
        public Loader<ObjectCursor<Folder>> onCreateLoader(final int id, final Bundle args) {
            final ObjectCursorLoader<Folder> loader = new ObjectCursorLoader<Folder>(getContext(),
                    mFolderListUri, UIProvider.FOLDERS_PROJECTION_WITH_UNREAD_SENDERS,
                    Folder.FACTORY);
            loader.setUpdateThrottle(mFolderItemUpdateDelayMs);
            return loader;
        }
    };

    /**
     * This code is intended to roughly duplicate the FolderLoaderCallback's onLoadFinished
     */
    private void populateUnreadSenders(final FolderHolder folderHolder,
            final String unreadSenders) {
        if (TextUtils.isEmpty(unreadSenders)) {
            folderHolder.setUnreadSenders(Collections.<String>emptyList());
            return;
        }
        // Use a LinkedHashMap here to maintain ordering
        final Map<String, String> emailtoNameMap = Maps.newLinkedHashMap();

        final Address[] senderAddresses = Address.parse(unreadSenders);

        for (final Address senderAddress : senderAddresses) {
            String sender = senderAddress.getName();
            final String senderEmail = senderAddress.getAddress();

            if (!TextUtils.isEmpty(sender)) {
                final String existingSender = emailtoNameMap.get(senderEmail);
                if (!TextUtils.isEmpty(existingSender)) {
                    // Prefer longer names
                    if (existingSender.length() >= sender.length()) {
                        // old name is longer
                        sender = existingSender;
                    }
                }
                emailtoNameMap.put(senderEmail, sender);
            }
            if (emailtoNameMap.size() >= 20) {
                break;
            }
        }

        final List<String> senders = Lists.newArrayList(emailtoNameMap.values());
        folderHolder.setUnreadSenders(senders);
    }

    private final LoaderCallbacks<ObjectCursor<Conversation>> mFolderLoaderCallbacks =
            new LoaderCallbacks<ObjectCursor<Conversation>>() {
        @Override
        public void onLoaderReset(final Loader<ObjectCursor<Conversation>> loader) {
            // Do nothing
        }

        @Override
        public void onLoadFinished(final Loader<ObjectCursor<Conversation>> loader,
                final ObjectCursor<Conversation> data) {
            // Sometimes names are condensed to just the first name.
            // This data structure keeps a map of emails to names
            final Map<String, String> emailToNameMap = Maps.newHashMap();
            final List<String> senders = Lists.newArrayList();

            final int folderId = getFolderId(loader.getId());

            final FolderHolder folderHolder = mFolderHolders.get(folderId);
            final int maxSenders = folderHolder.mFolder.unreadCount;

            if (maxSenders > 0 && data != null && data.moveToFirst()) {
                LogUtils.d(LOG_TAG, "Folder id %d loader finished", folderId);

                // Look through all conversations until we find 'maxSenders' unread
                int sendersFound = 0;

                do {
                    final Conversation conversation = data.getModel();

                    if (!conversation.read) {
                        String sender = null;
                        String senderEmail = null;
                        int priority = Integer.MIN_VALUE;

                        // Find the highest priority sender
                        for (final MessageInfo messageInfo :
                            conversation.conversationInfo.messageInfos) {
                            if (sender == null || priority < messageInfo.priority) {
                                sender = messageInfo.sender;
                                senderEmail = messageInfo.senderEmail;
                                priority = messageInfo.priority;
                            }
                        }

                        if (sender != null) {
                            sendersFound++;
                            final String existingSender = emailToNameMap.get(senderEmail);
                            if (existingSender != null) {
                                // Prefer longer names
                                if (existingSender.length() >= sender.length()) {
                                    // old name is longer
                                    sender = existingSender;
                                } else {
                                    // new name is longer
                                    int index = senders.indexOf(existingSender);
                                    senders.set(index, sender);
                                }
                            } else {
                                senders.add(sender);
                            }
                            emailToNameMap.put(senderEmail, sender);
                        }
                    }
                } while (data.moveToNext() && sendersFound < maxSenders);
            } else {
                LogUtils.w(LOG_TAG, "Problem with folder cursor returned from loader");
            }

            folderHolder.setUnreadSenders(senders);

            /*
             * Just update the views in place. We don't need to call notifyDataSetChanged()
             * because we aren't changing the teaser's visibility or position.
             */
            updateViews(folderHolder);
        }

        @Override
        public Loader<ObjectCursor<Conversation>> onCreateLoader(final int id, final Bundle args) {
            final int folderId = getFolderId(id);
            final Uri uri = mFolderHolders.get(folderId).mFolder.conversationListUri
                    .buildUpon()
                    .appendQueryParameter(ConversationListQueryParameters.USE_NETWORK,
                            Boolean.FALSE.toString())
                    .appendQueryParameter(ConversationListQueryParameters.LIMIT, MAX_SENDERS)
                    .build();
            return new ObjectCursorLoader<Conversation>(getContext(), uri,
                    UIProvider.CONVERSATION_PROJECTION, Conversation.FACTORY);
        }
    };
}
