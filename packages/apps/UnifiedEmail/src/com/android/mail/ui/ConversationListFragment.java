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
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.android.mail.ConversationListContext;
import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.browse.ConversationItemView;
import com.android.mail.browse.ConversationItemViewModel;
import com.android.mail.browse.ConversationListFooterView;
import com.android.mail.browse.ToggleableItem;
import com.android.mail.providers.Account;
import com.android.mail.providers.AccountObserver;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.FolderObserver;
import com.android.mail.providers.Settings;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.AccountCapabilities;
import com.android.mail.providers.UIProvider.ConversationListIcon;
import com.android.mail.providers.UIProvider.FolderCapabilities;
import com.android.mail.providers.UIProvider.FolderType;
import com.android.mail.providers.UIProvider.Swipe;
import com.android.mail.ui.AnimatedAdapter.ConversationListListener;
import com.android.mail.ui.SwipeableListView.ListItemSwipedListener;
import com.android.mail.ui.SwipeableListView.ListItemsRemovedListener;
import com.android.mail.ui.ViewMode.ModeChangeListener;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

/**
 * The conversation list UI component.
 */
public final class ConversationListFragment extends ListFragment implements
        OnItemLongClickListener, ModeChangeListener, ListItemSwipedListener {
    /** Key used to pass data to {@link ConversationListFragment}. */
    private static final String CONVERSATION_LIST_KEY = "conversation-list";
    /** Key used to keep track of the scroll state of the list. */
    private static final String LIST_STATE_KEY = "list-state";

    private static final String LOG_TAG = LogTag.getLogTag();
    /** Key used to save the ListView choice mode, since ListView doesn't save it automatically! */
    private static final String CHOICE_MODE_KEY = "choice-mode-key";

    // True if we are on a tablet device
    private static boolean mTabletDevice;

    /**
     * Frequency of update of timestamps. Initialized in
     * {@link #onCreate(Bundle)} and final afterwards.
     */
    private static int TIMESTAMP_UPDATE_INTERVAL = 0;

    private static long NO_NEW_MESSAGE_DURATION = 1 * DateUtils.SECOND_IN_MILLIS;

    private ControllableActivity mActivity;

    // Control state.
    private ConversationListCallbacks mCallbacks;

    private final Handler mHandler = new Handler();

    private ConversationListView mConversationListView;

    // The internal view objects.
    private SwipeableListView mListView;

    private TextView mSearchResultCountTextView;
    private TextView mSearchStatusTextView;

    private View mSearchStatusView;

    /**
     * Current Account being viewed
     */
    private Account mAccount;
    /**
     * Current folder being viewed.
     */
    private Folder mFolder;

    /**
     * A simple method to update the timestamps of conversations periodically.
     */
    private Runnable mUpdateTimestampsRunnable = null;

    private ConversationListContext mViewContext;

    private AnimatedAdapter mListAdapter;

    private ConversationListFooterView mFooterView;
    private View mEmptyView;
    private ErrorListener mErrorListener;
    private FolderObserver mFolderObserver;
    private DataSetObserver mConversationCursorObserver;

    private ConversationSelectionSet mSelectedSet;
    private final AccountObserver mAccountObserver = new AccountObserver() {
        @Override
        public void onChanged(Account newAccount) {
            mAccount = newAccount;
            setSwipeAction();
        }
    };
    private ConversationUpdater mUpdater;
    /** Hash of the Conversation Cursor we last obtained from the controller. */
    private int mConversationCursorHash;

    /** Duration, in milliseconds, of the CAB mode (peek icon) animation. */
    private static long sSelectionModeAnimationDuration = -1;
    /** The time at which we last exited CAB mode. */
    private long mSelectionModeExitedTimestamp = -1;

    /**
     * If <code>true</code>, we have restored (or attempted to restore) the list's scroll position
     * from when we were last on this conversation list.
     */
    private boolean mScrollPositionRestored = false;

    /**
     * Constructor needs to be public to handle orientation changes and activity
     * lifecycle events.
     */
    public ConversationListFragment() {
        super();
    }

    private class ConversationCursorObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onConversationListStatusUpdated();
        }
    }

    /**
     * Creates a new instance of {@link ConversationListFragment}, initialized
     * to display conversation list context.
     */
    public static ConversationListFragment newInstance(ConversationListContext viewContext) {
        final ConversationListFragment fragment = new ConversationListFragment();
        final Bundle args = new Bundle(1);
        args.putBundle(CONVERSATION_LIST_KEY, viewContext.toBundle());
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Show the header if the current conversation list is showing search
     * results.
     */
    void configureSearchResultHeader() {
        if (mActivity == null) {
            return;
        }
        // Only show the header if the context is for a search result
        final Resources res = getResources();
        final boolean showHeader = ConversationListContext.isSearchResult(mViewContext);
        // TODO(viki): This code contains intimate understanding of the view.
        // Much of this logic
        // needs to reside in a separate class that handles the text view in
        // isolation. Then,
        // that logic can be reused in other fragments.
        if (showHeader) {
            mSearchStatusTextView.setText(res.getString(R.string.search_results_searching_header));
            // Initially reset the count
            mSearchResultCountTextView.setText("");
        }
        mSearchStatusView.setVisibility(showHeader ? View.VISIBLE : View.GONE);
        int marginTop = showHeader ? (int) res.getDimension(R.dimen.notification_view_height) : 0;
        MarginLayoutParams layoutParams = (MarginLayoutParams) mListView.getLayoutParams();
        layoutParams.topMargin = marginTop;
        mListView.setLayoutParams(layoutParams);
    }

    /**
     * Show the header if the current conversation list is showing search
     * results.
     */
    private void updateSearchResultHeader(int count) {
        if (mActivity == null) {
            return;
        }
        // Only show the header if the context is for a search result
        final Resources res = getResources();
        final boolean showHeader = ConversationListContext.isSearchResult(mViewContext);
        if (showHeader) {
            mSearchStatusTextView.setText(res.getString(R.string.search_results_header));
            mSearchResultCountTextView
                    .setText(res.getString(R.string.search_results_loaded, count));
        }
    }

    /**
     * Initializes all internal state for a rendering.
     */
    private void initializeUiForFirstDisplay() {
        // TODO(mindyp): find some way to make the notification container more
        // re-usable.
        // TODO(viki): refactor according to comment in
        // configureSearchResultHandler()
        mSearchStatusView = mActivity.findViewById(R.id.search_status_view);
        mSearchStatusTextView = (TextView) mActivity.findViewById(R.id.search_status_text_view);
        mSearchResultCountTextView = (TextView) mActivity
                .findViewById(R.id.search_result_count_view);
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        if (sSelectionModeAnimationDuration < 0) {
            sSelectionModeAnimationDuration = getResources().getInteger(
                    R.integer.conv_item_view_cab_anim_duration);
        }

        // Strictly speaking, we get back an android.app.Activity from
        // getActivity. However, the
        // only activity creating a ConversationListContext is a MailActivity
        // which is of type
        // ControllableActivity, so this cast should be safe. If this cast
        // fails, some other
        // activity is creating ConversationListFragments. This activity must be
        // of type
        // ControllableActivity.
        final Activity activity = getActivity();
        if (!(activity instanceof ControllableActivity)) {
            LogUtils.e(LOG_TAG, "ConversationListFragment expects only a ControllableActivity to"
                    + "create it. Cannot proceed.");
        }
        mActivity = (ControllableActivity) activity;
        // Since we now have a controllable activity, load the account from it,
        // and register for
        // future account changes.
        mAccount = mAccountObserver.initialize(mActivity.getAccountController());
        mCallbacks = mActivity.getListHandler();
        mErrorListener = mActivity.getErrorListener();
        // Start off with the current state of the folder being viewed.
        Context activityContext = mActivity.getActivityContext();
        mFooterView = (ConversationListFooterView) LayoutInflater.from(
                activityContext).inflate(R.layout.conversation_list_footer_view,
                null);
        mFooterView.setClickListener(mActivity);
        mConversationListView.setActivity(mActivity);
        final ConversationCursor conversationCursor = getConversationListCursor();
        final LoaderManager manager = getLoaderManager();

        // TODO: These special views are always created, doesn't matter whether they will
        // be shown or not, as we add more views this will get more expensive. Given these are
        // tips that are only shown once to the user, we should consider creating these on demand.
        final ConversationListHelper helper = mActivity.getConversationListHelper();
        final List<ConversationSpecialItemView> specialItemViews = helper != null ?
                ImmutableList.copyOf(helper.makeConversationListSpecialViews(
                        activity, mActivity, mAccount))
                : null;
        if (specialItemViews != null) {
            // Attach to the LoaderManager
            for (final ConversationSpecialItemView view : specialItemViews) {
                view.bindFragment(manager, savedState);
            }
        }

        mListAdapter = new AnimatedAdapter(mActivity.getApplicationContext(), conversationCursor,
                mActivity.getSelectedSet(), mActivity, mConversationListListener, mListView,
                specialItemViews);
        mListAdapter.addFooter(mFooterView);
        mListView.setAdapter(mListAdapter);
        mSelectedSet = mActivity.getSelectedSet();
        mListView.setSelectionSet(mSelectedSet);
        mListAdapter.setFooterVisibility(false);
        mFolderObserver = new FolderObserver(){
            @Override
            public void onChanged(Folder newFolder) {
                onFolderUpdated(newFolder);
            }
        };
        mFolderObserver.initialize(mActivity.getFolderController());
        mConversationCursorObserver = new ConversationCursorObserver();
        mUpdater = mActivity.getConversationUpdater();
        mUpdater.registerConversationListObserver(mConversationCursorObserver);
        mTabletDevice = Utils.useTabletUI(mActivity.getApplicationContext().getResources());
        initializeUiForFirstDisplay();
        configureSearchResultHeader();
        // The onViewModeChanged callback doesn't get called when the mode
        // object is created, so
        // force setting the mode manually this time around.
        onViewModeChanged(mActivity.getViewMode().getMode());
        mActivity.getViewMode().addListener(this);

        if (mActivity.isFinishing()) {
            // Activity is finishing, just bail.
            return;
        }
        mConversationCursorHash = (conversationCursor == null) ? 0 : conversationCursor.hashCode();
        // Belt and suspenders here; make sure we do any necessary sync of the
        // ConversationCursor
        if (conversationCursor != null && conversationCursor.isRefreshReady()) {
            conversationCursor.sync();
        }

        // On a phone we never highlight a conversation, so the default is to select none.
        // On a tablet, we highlight a SINGLE conversation in landscape conversation view.
        int choice = getDefaultChoiceMode(mTabletDevice);
        if (savedState != null) {
            // Restore the choice mode if it was set earlier, or NONE if creating a fresh view.
            // Choice mode here represents the current conversation only. CAB mode does not rely on
            // the platform: checked state is a local variable {@link ConversationItemView#mChecked}
            choice = savedState.getInt(CHOICE_MODE_KEY, choice);
            if (savedState.containsKey(LIST_STATE_KEY)) {
                // TODO: find a better way to unset the selected item when restoring
                mListView.clearChoices();
            }
        }
        setChoiceMode(choice);

        // Show list and start loading list.
        showList();
        ToastBarOperation pendingOp = mActivity.getPendingToastOperation();
        if (pendingOp != null) {
            // Clear the pending operation
            mActivity.setPendingToastOperation(null);
            mActivity.onUndoAvailable(pendingOp);
        }
    }

    /**
     * Returns the default choice mode for the list based on whether the list is displayed on tablet
     * or not.
     * @param isTablet
     * @return
     */
    private final static int getDefaultChoiceMode(boolean isTablet) {
        return isTablet ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE;
    }

    public AnimatedAdapter getAnimatedAdapter() {
        return mListAdapter;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        // Initialize fragment constants from resources
        final Resources res = getResources();
        TIMESTAMP_UPDATE_INTERVAL = res.getInteger(R.integer.timestamp_update_interval);
        mUpdateTimestampsRunnable = new Runnable() {
            @Override
            public void run() {
                mListView.invalidateViews();
                mHandler.postDelayed(mUpdateTimestampsRunnable, TIMESTAMP_UPDATE_INTERVAL);
            }
        };

        // Get the context from the arguments
        final Bundle args = getArguments();
        mViewContext = ConversationListContext.forBundle(args.getBundle(CONVERSATION_LIST_KEY));
        mAccount = mViewContext.account;

        setRetainInstance(false);
    }

    @Override
    public String toString() {
        final String s = super.toString();
        if (mViewContext == null) {
            return s;
        }
        final StringBuilder sb = new StringBuilder(s);
        sb.setLength(sb.length() - 1);
        sb.append(" mListAdapter=");
        sb.append(mListAdapter);
        sb.append(" folder=");
        sb.append(mViewContext.folder);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.conversation_list, null);
        mEmptyView = rootView.findViewById(R.id.empty_view);
        mConversationListView =
                (ConversationListView) rootView.findViewById(R.id.conversation_list);
        mConversationListView.setConversationContext(mViewContext);
        mListView = (SwipeableListView) rootView.findViewById(android.R.id.list);
        mListView.setHeaderDividersEnabled(false);
        mListView.setOnItemLongClickListener(this);
        mListView.enableSwipe(mAccount.supportsCapability(AccountCapabilities.UNDO));
        mListView.setSwipedListener(this);

        if (savedState != null && savedState.containsKey(LIST_STATE_KEY)) {
            mListView.onRestoreInstanceState(savedState.getParcelable(LIST_STATE_KEY));
        }

        return rootView;
    }

    /**
     * Sets the choice mode of the list view
     * @param choiceMode ListView#
     */
    private final void setChoiceMode(int choiceMode) {
        mListView.setChoiceMode(choiceMode);
    }

    /**
     * Tell the list to select nothing.
     */
    public final void setChoiceNone() {
        // On a phone, the default choice mode is already none, so nothing to do.
        if (!mTabletDevice) {
            return;
        }
        clearChoicesAndActivated();
        setChoiceMode(ListView.CHOICE_MODE_NONE);
    }

    /**
     * Tell the list to get out of selecting none.
     */
    public final void revertChoiceMode() {
        // On a phone, the default choice mode is always none, so nothing to do.
        if (!mTabletDevice) {
            return;
        }
        setChoiceMode(getDefaultChoiceMode(mTabletDevice));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {

        // Clear the list's adapter
        mListAdapter.destroy();
        mListView.setAdapter(null);

        mActivity.getViewMode().removeListener(this);
        if (mFolderObserver != null) {
            mFolderObserver.unregisterAndDestroy();
            mFolderObserver = null;
        }
        if (mConversationCursorObserver != null) {
            mUpdater.unregisterConversationListObserver(mConversationCursorObserver);
            mConversationCursorObserver = null;
        }
        mAccountObserver.unregisterAndDestroy();
        getAnimatedAdapter().cleanup();
        super.onDestroyView();
    }

    /**
     * There are three binary variables, which determine what we do with a
     * message. checkbEnabled: Whether check boxes are enabled or not (forced
     * true on tablet) cabModeOn: Whether CAB mode is currently on or not.
     * pressType: long or short tap (There is a third possibility: phone or
     * tablet, but they have <em>identical</em> behavior) The matrix of
     * possibilities is:
     * <p>
     * Long tap: Always toggle selection of conversation. If CAB mode is not
     * started, then start it.
     * <pre>
     *              | Checkboxes | No Checkboxes
     *    ----------+------------+---------------
     *    CAB mode  |   Select   |     Select
     *    List mode |   Select   |     Select
     *
     * </pre>
     *
     * Reference: http://b/issue?id=6392199
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // Ignore anything that is not a conversation item. Could be a footer.
        if (!(view instanceof ConversationItemView)) {
            return false;
        }
        return ((ConversationItemView) view).toggleSelectedStateOrBeginDrag();
    }

    /**
     * See the comment for
     * {@link #onItemLongClick(AdapterView, View, int, long)}.
     * <p>
     * Short tap behavior:
     *
     * <pre>
     *              | Checkboxes | No Checkboxes
     *    ----------+------------+---------------
     *    CAB mode  |    Peek    |     Select
     *    List mode |    Peek    |      Peek
     * </pre>
     *
     * Reference: http://b/issue?id=6392199
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        if (view instanceof ToggleableItem) {
            final boolean showSenderImage =
                    (mAccount.settings.convListIcon == ConversationListIcon.SENDER_IMAGE);
            final boolean inCabMode = !mSelectedSet.isEmpty();
            if (!showSenderImage && inCabMode) {
                ((ToggleableItem) view).toggleSelectedState();
            } else {
                if (inCabMode) {
                    // this is a peek.
                    Analytics.getInstance().sendEvent("peek", null, null, mSelectedSet.size());
                }
                viewConversation(position);
            }
        } else {
            // Ignore anything that is not a conversation item. Could be a footer.
            // If we are using a keyboard, the highlighted item is the parent;
            // otherwise, this is a direct call from the ConverationItemView
            return;
        }
        // When a new list item is clicked, commit any existing leave behind
        // items. Wait until we have opened the desired conversation to cause
        // any position changes.
        commitDestructiveActions(Utils.useTabletUI(mActivity.getActivityContext().getResources()));
    }

    @Override
    public void onResume() {
        super.onResume();

        final ConversationCursor conversationCursor = getConversationListCursor();
        if (conversationCursor != null) {
            conversationCursor.handleNotificationActions();

            restoreLastScrolledPosition();
        }

        mSelectedSet.addObserver(mConversationSetObserver);
    }

    @Override
    public void onPause() {
        super.onPause();

        mSelectedSet.removeObserver(mConversationSetObserver);

        saveLastScrolledPosition();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mListView != null) {
            outState.putParcelable(LIST_STATE_KEY, mListView.onSaveInstanceState());
            outState.putInt(CHOICE_MODE_KEY, mListView.getChoiceMode());
        }

        if (mListAdapter != null) {
            mListAdapter.saveSpecialItemInstanceState(outState);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler.postDelayed(mUpdateTimestampsRunnable, TIMESTAMP_UPDATE_INTERVAL);
        Analytics.getInstance().sendView(getClass().getName());
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mUpdateTimestampsRunnable);
    }

    @Override
    public void onViewModeChanged(int newMode) {
        if (mTabletDevice) {
            if (ViewMode.isListMode(newMode)) {
                // There are no selected conversations when in conversation list mode.
                clearChoicesAndActivated();
            }
        }
        if (mFooterView != null) {
            mFooterView.onViewModeChanged(newMode);
        }
    }

    public boolean isAnimating() {
        final AnimatedAdapter adapter = getAnimatedAdapter();
        return (adapter != null && adapter.isAnimating()) ||
                (mListView != null && mListView.isScrolling());
    }

    private void clearChoicesAndActivated() {
        final int currentSelected = mListView.getCheckedItemPosition();
        if (currentSelected != ListView.INVALID_POSITION) {
            mListView.setItemChecked(mListView.getCheckedItemPosition(), false);
        }
    }

    /**
     * Handles a request to show a new conversation list, either from a search
     * query or for viewing a folder. This will initiate a data load, and hence
     * must be called on the UI thread.
     */
    private void showList() {
        mListView.setEmptyView(null);
        onFolderUpdated(mActivity.getFolderController().getFolder());
        onConversationListStatusUpdated();
    }

    /**
     * View the message at the given position.
     *
     * @param position The position of the conversation in the list (as opposed to its position
     *        in the cursor)
     */
    private void viewConversation(final int position) {
        LogUtils.d(LOG_TAG, "ConversationListFragment.viewConversation(%d)", position);

        final ConversationCursor cursor =
                (ConversationCursor) getAnimatedAdapter().getItem(position);

        if (cursor == null) {
            LogUtils.e(LOG_TAG,
                    "unable to open conv at cursor pos=%s cursor=%s getPositionOffset=%s",
                    position, cursor, getAnimatedAdapter().getPositionOffset(position));
            return;
        }

        final Conversation conv = cursor.getConversation();
        /*
         * The cursor position may be different than the position method parameter because of
         * special views in the list.
         */
        conv.position = cursor.getPosition();
        setSelected(conv.position, true);
        mCallbacks.onConversationSelected(conv, false /* inLoaderCallbacks */);
    }

    private final ConversationListListener mConversationListListener =
            new ConversationListListener() {
        @Override
        public boolean isExitingSelectionMode() {
            return System.currentTimeMillis() <
                    (mSelectionModeExitedTimestamp + sSelectionModeAnimationDuration);
        }
    };

    /**
     * Sets the selected conversation to the position given here.
     * @param cursorPosition The position of the conversation in the cursor (as opposed to
     * in the list)
     * @param different if the currently selected conversation is different from the one provided
     * here.  This is a difference in conversations, not a difference in positions. For example, a
     * conversation at position 2 can move to position 4 as a result of new mail.
     */
    public void setSelected(final int cursorPosition, boolean different) {
        if (mListView.getChoiceMode() == ListView.CHOICE_MODE_NONE) {
            return;
        }

        final int position =
                cursorPosition + getAnimatedAdapter().getPositionOffset(cursorPosition);

        setRawSelected(position, different);
    }

    /**
     * Sets the selected conversation to the position given here.
     * @param position The position of the item in the list
     * @param different if the currently selected conversation is different from the one provided
     * here.  This is a difference in conversations, not a difference in positions. For example, a
     * conversation at position 2 can move to position 4 as a result of new mail.
     */
    public void setRawSelected(final int position, final boolean different) {
        if (mListView.getChoiceMode() == ListView.CHOICE_MODE_NONE) {
            return;
        }

        if (different) {
            mListView.smoothScrollToPosition(position);
        }
        mListView.setItemChecked(position, true);
    }

    /**
     * Returns the cursor associated with the conversation list.
     * @return
     */
    private ConversationCursor getConversationListCursor() {
        return mCallbacks != null ? mCallbacks.getConversationListCursor() : null;
    }

    /**
     * Request a refresh of the list. No sync is carried out and none is
     * promised.
     */
    public void requestListRefresh() {
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Change the UI to delete the conversations provided and then call the
     * {@link DestructiveAction} provided here <b>after</b> the UI has been
     * updated.
     * @param conversations
     * @param action
     */
    public void requestDelete(int actionId, final Collection<Conversation> conversations,
            final DestructiveAction action) {
        for (Conversation conv : conversations) {
            conv.localDeleteOnUpdate = true;
        }
        final ListItemsRemovedListener listener = new ListItemsRemovedListener() {
            @Override
            public void onListItemsRemoved() {
                action.performAction();
            }
        };
        final SwipeableListView listView = (SwipeableListView) getListView();
        if (listView.getSwipeAction() == actionId) {
            if (!listView.destroyItems(conversations, listener)) {
                // The listView failed to destroy the items, perform the action manually
                LogUtils.e(LOG_TAG, "ConversationListFragment.requestDelete: " +
                        "listView failed to destroy items.");
                action.performAction();
            }
            return;
        }
        // Delete the local delete items (all for now) and when done,
        // update...
        mListAdapter.delete(conversations, listener);
    }

    public void onFolderUpdated(Folder folder) {
        mFolder = folder;
        setSwipeAction();
        if (mFolder == null) {
            return;
        }
        mListAdapter.setFolder(mFolder);
        mFooterView.setFolder(mFolder);
        if (!mFolder.wasSyncSuccessful()) {
            mErrorListener.onError(mFolder, false);
        }

        // Notify of changes to the Folder.
        onFolderStatusUpdated();

        // Blow away conversation items cache.
        ConversationItemViewModel.onFolderUpdated(mFolder);
    }

    /**
     * Updates the footer visibility and updates the conversation cursor
     */
    public void onConversationListStatusUpdated() {
        final ConversationCursor cursor = getConversationListCursor();
        final boolean showFooter = mFooterView.updateStatus(cursor);
        // Update the folder status, in case the cursor could affect it.
        onFolderStatusUpdated();
        mListAdapter.setFooterVisibility(showFooter);

        // Also change the cursor here.
        onCursorUpdated();
    }

    private void onFolderStatusUpdated() {
        // Update the sync status bar with sync results if needed
        checkSyncStatus();

        final ConversationCursor cursor = getConversationListCursor();
        Bundle extras = cursor != null ? cursor.getExtras() : Bundle.EMPTY;
        int errorStatus = extras.containsKey(UIProvider.CursorExtraKeys.EXTRA_ERROR) ?
                extras.getInt(UIProvider.CursorExtraKeys.EXTRA_ERROR)
                : UIProvider.LastSyncResult.SUCCESS;
        int cursorStatus = extras.getInt(UIProvider.CursorExtraKeys.EXTRA_STATUS);
        // We want to update the UI with this information if either we are loaded or complete, or
        // we have a folder with a non-0 count.
        final int folderCount = mFolder != null ? mFolder.totalCount : 0;
        if (errorStatus == UIProvider.LastSyncResult.SUCCESS
                && (cursorStatus == UIProvider.CursorStatus.LOADED
                || cursorStatus == UIProvider.CursorStatus.COMPLETE) || folderCount > 0) {
            updateSearchResultHeader(folderCount);
            if (folderCount == 0) {
                mListView.setEmptyView(mEmptyView);
            }
        }
    }

    private void setSwipeAction() {
        int swipeSetting = Settings.getSwipeSetting(mAccount.settings);
        if (swipeSetting == Swipe.DISABLED
                || !mAccount.supportsCapability(AccountCapabilities.UNDO)
                || (mFolder != null && mFolder.isTrash())) {
            mListView.enableSwipe(false);
        } else {
            final int action;
            mListView.enableSwipe(true);
            if (ConversationListContext.isSearchResult(mViewContext)
                    || (mFolder != null && mFolder.isType(FolderType.SPAM))) {
                action = R.id.delete;
            } else if (mFolder == null) {
                action = R.id.remove_folder;
            } else {
                // We have enough information to respect user settings.
                switch (swipeSetting) {
                    case Swipe.ARCHIVE:
                        if (mAccount.supportsCapability(AccountCapabilities.ARCHIVE)) {
                            if (mFolder.supportsCapability(FolderCapabilities.ARCHIVE)) {
                                action = R.id.archive;
                                break;
                            } else if (mFolder.supportsCapability
                                    (FolderCapabilities.CAN_ACCEPT_MOVED_MESSAGES)) {
                                action = R.id.remove_folder;
                                break;
                            }
                        }

                        /*
                         * If we get here, we don't support archive, on either the account or the
                         * folder, so we want to fall through into the delete case.
                         */
                        //$FALL-THROUGH$
                    case Swipe.DELETE:
                    default:
                        action = R.id.delete;
                        break;
                }
            }
            mListView.setSwipeAction(action);
        }
        mListView.setCurrentAccount(mAccount);
        mListView.setCurrentFolder(mFolder);
    }

    /**
     * Changes the conversation cursor in the list and sets selected position if none is set.
     */
    private void onCursorUpdated() {
        if (mCallbacks == null || mListAdapter == null) {
            return;
        }
        // Check against the previous cursor here and see if they are the same. If they are, then
        // do a notifyDataSetChanged.
        final ConversationCursor newCursor = mCallbacks.getConversationListCursor();

        if (newCursor == null && mListAdapter.getCursor() != null) {
            // We're losing our cursor, so save our scroll position
            saveLastScrolledPosition();
        }

        mListAdapter.swapCursor(newCursor);
        // When the conversation cursor is *updated*, we get back the same instance. In that
        // situation, CursorAdapter.swapCursor() silently returns, without forcing a
        // notifyDataSetChanged(). So let's force a call to notifyDataSetChanged, since an updated
        // cursor means that the dataset has changed.
        final int newCursorHash = (newCursor == null) ? 0 : newCursor.hashCode();
        if (mConversationCursorHash == newCursorHash && mConversationCursorHash != 0) {
            mListAdapter.notifyDataSetChanged();
        }
        mConversationCursorHash = newCursorHash;

        if (newCursor != null && newCursor.getCount() > 0) {
            newCursor.markContentsSeen();
            restoreLastScrolledPosition();
        }

        // If a current conversation is available, and none is selected in the list, then ask
        // the list to select the current conversation.
        final Conversation conv = mCallbacks.getCurrentConversation();
        if (conv != null) {
            if (mListView.getChoiceMode() != ListView.CHOICE_MODE_NONE
                    && mListView.getCheckedItemPosition() == -1) {
                setSelected(conv.position, true);
            }
        }
    }

    public void commitDestructiveActions(boolean animate) {
        if (mListView != null) {
            mListView.commitDestructiveActions(animate);

        }
    }

    @Override
    public void onListItemSwiped(Collection<Conversation> conversations) {
        mUpdater.showNextConversation(conversations);
    }

    private void checkSyncStatus() {
        if (mFolder != null && mFolder.isSyncInProgress()) {
            LogUtils.d(LOG_TAG, "CLF.checkSyncStatus still syncing");
            // Still syncing, ignore
        } else {
            // Finished syncing:
            LogUtils.d(LOG_TAG, "CLF.checkSyncStatus done syncing");
            mConversationListView.onSyncFinished();
        }
    }

    /**
     * Displays the indefinite progress bar indicating a sync is in progress.  This
     * should only be called if user manually requested a sync, and not for background syncs.
     */
    protected void showSyncStatusBar() {
        mConversationListView.showSyncStatusBar();
    }

    /**
     * Clears all items in the list.
     */
    public void clear() {
        mListView.setAdapter(null);
    }

    private final ConversationSetObserver mConversationSetObserver = new ConversationSetObserver() {
        @Override
        public void onSetPopulated(final ConversationSelectionSet set) {
            // Do nothing
        }

        @Override
        public void onSetEmpty() {
            mSelectionModeExitedTimestamp = System.currentTimeMillis();
        }

        @Override
        public void onSetChanged(final ConversationSelectionSet set) {
            // Do nothing
        }
    };

    private void saveLastScrolledPosition() {
        if (mListAdapter.getCursor() == null) {
            // If you save your scroll position in an empty list, you're gonna have a bad time
            return;
        }

        final Parcelable savedState = mListView.onSaveInstanceState();

        mActivity.getListHandler().setConversationListScrollPosition(
                mFolder.conversationListUri.toString(), savedState);
    }

    private void restoreLastScrolledPosition() {
        // Scroll to our previous position, if necessary
        if (!mScrollPositionRestored && mFolder != null) {
            final String key = mFolder.conversationListUri.toString();
            final Parcelable savedState = mActivity.getListHandler()
                    .getConversationListScrollPosition(key);
            if (savedState != null) {
                mListView.onRestoreInstanceState(savedState);
            }
            mScrollPositionRestored = true;
        }
    }
}
