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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.android.mail.ConversationListContext;
import com.android.mail.R;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider.ConversationListIcon;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

/**
 * Controller for two-pane Mail activity. Two Pane is used for tablets, where screen real estate
 * abounds.
 */
public final class TwoPaneController extends AbstractActivityController {

    private static final String SAVED_MISCELLANEOUS_VIEW = "saved-miscellaneous-view";
    private static final String SAVED_MISCELLANEOUS_VIEW_TRANSACTION_ID =
            "saved-miscellaneous-view-transaction-id";

    private TwoPaneLayout mLayout;
    private Conversation mConversationToShow;

    /**
     * Used to determine whether onViewModeChanged should skip a potential
     * fragment transaction that would remove a miscellaneous view.
     */
    private boolean mSavedMiscellaneousView = false;

    public TwoPaneController(MailActivity activity, ViewMode viewMode) {
        super(activity, viewMode);
    }

    /**
     * Display the conversation list fragment.
     */
    private void initializeConversationListFragment() {
        if (Intent.ACTION_SEARCH.equals(mActivity.getIntent().getAction())) {
            if (shouldEnterSearchConvMode()) {
                mViewMode.enterSearchResultsConversationMode();
            } else {
                mViewMode.enterSearchResultsListMode();
            }
        }
        renderConversationList();
    }

    /**
     * Render the conversation list in the correct pane.
     */
    private void renderConversationList() {
        if (mActivity == null) {
            return;
        }
        FragmentTransaction fragmentTransaction = mActivity.getFragmentManager().beginTransaction();
        // Use cross fading animation.
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        final Fragment conversationListFragment =
                ConversationListFragment.newInstance(mConvListContext);
        fragmentTransaction.replace(R.id.conversation_list_pane, conversationListFragment,
                TAG_CONVERSATION_LIST);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public boolean doesActionChangeConversationListVisibility(final int action) {
        if (action == R.id.settings
                || action == R.id.compose
                || action == R.id.help_info_menu_item
                || action == R.id.manage_folders_item
                || action == R.id.folder_options
                || action == R.id.feedback_menu_item) {
            return true;
        }

        return false;
    }

    @Override
    protected boolean isConversationListVisible() {
        return !mLayout.isConversationListCollapsed();
    }

    @Override
    public void showConversationList(ConversationListContext listContext) {
        super.showConversationList(listContext);
        initializeConversationListFragment();
    }

    @Override
    public boolean onCreate(Bundle savedState) {
        mActivity.setContentView(R.layout.two_pane_activity);
        mDrawerContainer = (DrawerLayout) mActivity.findViewById(R.id.drawer_container);
        mDrawerPullout = mDrawerContainer.findViewById(R.id.content_pane);
        mLayout = (TwoPaneLayout) mActivity.findViewById(R.id.two_pane_activity);
        if (mLayout == null) {
            // We need the layout for everything. Crash/Return early if it is null.
            LogUtils.wtf(LOG_TAG, "mLayout is null!");
            return false;
        }
        mLayout.setController(this, Intent.ACTION_SEARCH.equals(mActivity.getIntent().getAction()));
        mLayout.setDrawerLayout(mDrawerContainer);

        if (savedState != null) {
            mSavedMiscellaneousView = savedState.getBoolean(SAVED_MISCELLANEOUS_VIEW, false);
            mMiscellaneousViewTransactionId =
                    savedState.getInt(SAVED_MISCELLANEOUS_VIEW_TRANSACTION_ID, -1);
        }

        // 2-pane layout is the main listener of view mode changes, and issues secondary
        // notifications upon animation completion:
        // (onConversationVisibilityChanged, onConversationListVisibilityChanged)
        mViewMode.addListener(mLayout);
        return super.onCreate(savedState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVED_MISCELLANEOUS_VIEW, mMiscellaneousViewTransactionId >= 0);
        outState.putInt(SAVED_MISCELLANEOUS_VIEW_TRANSACTION_ID, mMiscellaneousViewTransactionId);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && !mLayout.isConversationListCollapsed()) {
            // The conversation list is visible.
            informCursorVisiblity(true);
        }
    }

    @Override
    public void onFolderSelected(Folder folder) {
        // It's possible that we are not in conversation list mode
        if (mViewMode.getMode() != ViewMode.CONVERSATION_LIST) {
            mViewMode.enterConversationListMode();
        }

        if (folder.parent != Uri.EMPTY) {
            // Show the up affordance when digging into child folders.
            mActionBarView.setBackButton();
        }
        setHierarchyFolder(folder);
        super.onFolderSelected(folder);
    }

    @Override
    public void onViewModeChanged(int newMode) {
        if (!mSavedMiscellaneousView && mMiscellaneousViewTransactionId >= 0) {
            final FragmentManager fragmentManager = mActivity.getFragmentManager();
            fragmentManager.popBackStackImmediate(mMiscellaneousViewTransactionId,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            mMiscellaneousViewTransactionId = -1;
        }
        mSavedMiscellaneousView = false;

        super.onViewModeChanged(newMode);
        if (newMode != ViewMode.WAITING_FOR_ACCOUNT_INITIALIZATION) {
            // Clear the wait fragment
            hideWaitForInitialization();
        }
        // In conversation mode, if the conversation list is not visible, then the user cannot
        // see the selected conversations. Disable the CAB mode while leaving the selected set
        // untouched.
        // When the conversation list is made visible again, try to enable the CAB
        // mode if any conversations are selected.
        if (newMode == ViewMode.CONVERSATION || newMode == ViewMode.CONVERSATION_LIST
                || ViewMode.isAdMode(newMode)) {
            enableOrDisableCab();
        }
    }

    @Override
    public void onConversationVisibilityChanged(boolean visible) {
        super.onConversationVisibilityChanged(visible);
        if (!visible) {
            mPagerController.hide(false /* changeVisibility */);
        } else if (mConversationToShow != null) {
            mPagerController.show(mAccount, mFolder, mConversationToShow,
                    false /* changeVisibility */);
            mConversationToShow = null;
        }
    }

    @Override
    public void onConversationListVisibilityChanged(boolean visible) {
        super.onConversationListVisibilityChanged(visible);
        enableOrDisableCab();
    }

    @Override
    public void resetActionBarIcon() {
        if (isDrawerEnabled()) {
            return;
        }
        // On two-pane, the back button is only removed in the conversation list mode for top level
        // folders, and shown for every other condition.
        if ((mViewMode.isListMode() && (mFolder == null || mFolder.parent == null
                || mFolder.parent == Uri.EMPTY)) || mViewMode.isWaitingForSync()) {
            mActionBarView.removeBackButton();
        } else {
            mActionBarView.setBackButton();
        }
    }

    /**
     * Enable or disable the CAB mode based on the visibility of the conversation list fragment.
     */
    private void enableOrDisableCab() {
        if (mLayout.isConversationListCollapsed()) {
            disableCabMode();
        } else {
            enableCabMode();
        }
    }

    @Override
    public void onSetPopulated(ConversationSelectionSet set) {
        super.onSetPopulated(set);

        boolean showSenderImage =
                (mAccount.settings.convListIcon == ConversationListIcon.SENDER_IMAGE);
        if (!showSenderImage && mViewMode.isListMode()) {
            getConversationListFragment().setChoiceNone();
        }
    }

    @Override
    public void onSetEmpty() {
        super.onSetEmpty();

        boolean showSenderImage =
                (mAccount.settings.convListIcon == ConversationListIcon.SENDER_IMAGE);
        if (!showSenderImage && mViewMode.isListMode()) {
            getConversationListFragment().revertChoiceMode();
        }
    }

    @Override
    protected void showConversation(Conversation conversation, boolean inLoaderCallbacks) {
        super.showConversation(conversation, inLoaderCallbacks);

        // 2-pane can ignore inLoaderCallbacks because it doesn't use
        // FragmentManager.popBackStack().

        if (mActivity == null) {
            return;
        }
        if (conversation == null) {
            handleBackPress();
            return;
        }
        // If conversation list is not visible, then the user cannot see the CAB mode, so exit it.
        // This is needed here (in addition to during viewmode changes) because orientation changes
        // while viewing a conversation don't change the viewmode: the mode stays
        // ViewMode.CONVERSATION and yet the conversation list goes in and out of visibility.
        enableOrDisableCab();

        // When a mode change is required, wait for onConversationVisibilityChanged(), the signal
        // that the mode change animation has finished, before rendering the conversation.
        mConversationToShow = conversation;

        final int mode = mViewMode.getMode();
        LogUtils.i(LOG_TAG, "IN TPC.showConv, oldMode=%s conv=%s", mode, mConversationToShow);
        if (mode == ViewMode.SEARCH_RESULTS_LIST || mode == ViewMode.SEARCH_RESULTS_CONVERSATION) {
            mViewMode.enterSearchResultsConversationMode();
        } else {
            mViewMode.enterConversationMode();
        }
        // load the conversation immediately if we're already in conversation mode
        if (!mLayout.isModeChangePending()) {
            onConversationVisibilityChanged(true);
        } else {
            LogUtils.i(LOG_TAG, "TPC.showConversation will wait for TPL.animationEnd to show!");
        }
    }

    @Override
    public void setCurrentConversation(Conversation conversation) {
        // Order is important! We want to calculate different *before* the superclass changes
        // mCurrentConversation, so before super.setCurrentConversation().
        final long oldId = mCurrentConversation != null ? mCurrentConversation.id : -1;
        final long newId = conversation != null ? conversation.id : -1;
        final boolean different = oldId != newId;

        // This call might change mCurrentConversation.
        super.setCurrentConversation(conversation);

        final ConversationListFragment convList = getConversationListFragment();
        if (convList != null && conversation != null) {
            convList.setSelected(conversation.position, different);
        }
    }

    @Override
    public void showWaitForInitialization() {
        super.showWaitForInitialization();

        FragmentTransaction fragmentTransaction = mActivity.getFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.replace(R.id.conversation_list_pane, getWaitFragment(), TAG_WAIT);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    protected void hideWaitForInitialization() {
        final WaitFragment waitFragment = getWaitFragment();
        if (waitFragment == null) {
            // We aren't showing a wait fragment: nothing to do
            return;
        }
        // Remove the existing wait fragment from the back stack.
        final FragmentTransaction fragmentTransaction =
                mActivity.getFragmentManager().beginTransaction();
        fragmentTransaction.remove(waitFragment);
        fragmentTransaction.commitAllowingStateLoss();
        super.hideWaitForInitialization();
        if (mViewMode.isWaitingForSync()) {
            // We should come out of wait mode and display the account inbox.
            loadAccountInbox();
        }
    }

    /**
     * Up works as follows:
     * 1) If the user is in a conversation and:
     *  a) the conversation list is hidden (portrait mode), shows the conv list and
     *  stays in conversation view mode.
     *  b) the conversation list is shown, goes back to conversation list mode.
     * 2) If the user is in search results, up exits search.
     * mode and returns the user to whatever view they were in when they began search.
     * 3) If the user is in conversation list mode, there is no up.
     */
    @Override
    public boolean handleUpPress() {
        int mode = mViewMode.getMode();
        if (mode == ViewMode.CONVERSATION || mViewMode.isAdMode()) {
            handleBackPress();
        } else if (mode == ViewMode.SEARCH_RESULTS_CONVERSATION) {
            if (mLayout.isConversationListCollapsed()
                    || (ConversationListContext.isSearchResult(mConvListContext) && !Utils.
                            showTwoPaneSearchResults(mActivity.getApplicationContext()))) {
                handleBackPress();
            } else {
                mActivity.finish();
            }
        } else if (mode == ViewMode.SEARCH_RESULTS_LIST) {
            mActivity.finish();
        } else if (mode == ViewMode.CONVERSATION_LIST
                || mode == ViewMode.WAITING_FOR_ACCOUNT_INITIALIZATION) {
            final boolean isTopLevel = (mFolder == null) || (mFolder.parent == Uri.EMPTY);

            if (isTopLevel) {
                // Show the drawer
                toggleDrawerState();
            } else {
                popView(true);
            }
        }
        return true;
    }

    @Override
    public boolean handleBackPress() {
        // Clear any visible undo bars.
        mToastBar.hide(false, false /* actionClicked */);
        popView(false);
        return true;
    }

    /**
     * Pops the "view stack" to the last screen the user was viewing.
     *
     * @param preventClose Whether to prevent closing the app if the stack is empty.
     */
    protected void popView(boolean preventClose) {
        // If the user is in search query entry mode, or the user is viewing
        // search results, exit
        // the mode.
        int mode = mViewMode.getMode();
        if (mode == ViewMode.SEARCH_RESULTS_LIST) {
            mActivity.finish();
        } else if (mode == ViewMode.CONVERSATION || mViewMode.isAdMode()) {
            // Go to conversation list.
            mViewMode.enterConversationListMode();
        } else if (mode == ViewMode.SEARCH_RESULTS_CONVERSATION) {
            mViewMode.enterSearchResultsListMode();
        } else {
            // The Folder List fragment can be null for monkeys where we get a back before the
            // folder list has had a chance to initialize.
            final FolderListFragment folderList = getFolderListFragment();
            if (mode == ViewMode.CONVERSATION_LIST && folderList != null
                    && mFolder != null && mFolder.parent != Uri.EMPTY) {
                // If the user navigated via the left folders list into a child folder,
                // back should take the user up to the parent folder's conversation list.
                navigateUpFolderHierarchy();
            // Otherwise, if we are in the conversation list but not in the default
            // inbox and not on expansive layouts, we want to switch back to the default
            // inbox. This fixes b/9006969 so that on smaller tablets where we have this
            // hybrid one and two-pane mode, we will return to the inbox. On larger tablets,
            // we will instead exit the app.
            } else {
                // Don't think mLayout could be null but checking just in case
                if (mLayout == null) {
                    LogUtils.wtf(LOG_TAG, new Throwable(), "mLayout is null");
                }
                // mFolder could be null if back is pressed while account is waiting for sync
                final boolean shouldLoadInbox = mode == ViewMode.CONVERSATION_LIST &&
                        mFolder != null &&
                        !mFolder.folderUri.equals(mAccount.settings.defaultInbox) &&
                        mLayout != null && !mLayout.isExpansiveLayout();
                if (shouldLoadInbox) {
                    loadAccountInbox();
                } else if (!preventClose) {
                    // There is nothing else to pop off the stack.
                    mActivity.finish();
                }
            }
        }
    }

    @Override
    public void exitSearchMode() {
        final int mode = mViewMode.getMode();
        if (mode == ViewMode.SEARCH_RESULTS_LIST
                || (mode == ViewMode.SEARCH_RESULTS_CONVERSATION
                        && Utils.showTwoPaneSearchResults(mActivity.getApplicationContext()))) {
            mActivity.finish();
        }
    }

    @Override
    public boolean shouldShowFirstConversation() {
        return Intent.ACTION_SEARCH.equals(mActivity.getIntent().getAction())
                && shouldEnterSearchConvMode();
    }

    @Override
    public void onUndoAvailable(ToastBarOperation op) {
        final int mode = mViewMode.getMode();
        final ConversationListFragment convList = getConversationListFragment();

        repositionToastBar(op);

        switch (mode) {
            case ViewMode.SEARCH_RESULTS_LIST:
            case ViewMode.CONVERSATION_LIST:
            case ViewMode.SEARCH_RESULTS_CONVERSATION:
            case ViewMode.CONVERSATION:
                if (convList != null) {
                    mToastBar.show(getUndoClickedListener(convList.getAnimatedAdapter()),
                            0,
                            Utils.convertHtmlToPlainText
                                (op.getDescription(mActivity.getActivityContext())),
                            true, /* showActionIcon */
                            R.string.undo,
                            true,  /* replaceVisibleToast */
                            op);
                }
        }
    }

    public void repositionToastBar(ToastBarOperation op) {
        repositionToastBar(op.isBatchUndo());
    }

    /**
     * Set the toast bar's layout params to position it in the right place
     * depending the current view mode.
     *
     * @param convModeShowInList if we're in conversation mode, should the toast
     *            bar appear over the list? no effect when not in conversation mode.
     */
    private void repositionToastBar(boolean convModeShowInList) {
        final int mode = mViewMode.getMode();
        final FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) mToastBar.getLayoutParams();
        switch (mode) {
            case ViewMode.SEARCH_RESULTS_LIST:
            case ViewMode.CONVERSATION_LIST:
                params.width = mLayout.computeConversationListWidth() - params.leftMargin
                        - params.rightMargin;
                params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                mToastBar.setLayoutParams(params);
                break;
            case ViewMode.SEARCH_RESULTS_CONVERSATION:
            case ViewMode.CONVERSATION:
                if (convModeShowInList && !mLayout.isConversationListCollapsed()) {
                    // Show undo bar in the conversation list.
                    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                    params.width = mLayout.computeConversationListWidth() - params.leftMargin
                            - params.rightMargin;
                    mToastBar.setLayoutParams(params);
                } else {
                    // Show undo bar in the conversation.
                    params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                    params.width = mLayout.computeConversationWidth() - params.leftMargin
                            - params.rightMargin;
                    mToastBar.setLayoutParams(params);
                }
                break;
        }
    }

    @Override
    protected void hideOrRepositionToastBar(final boolean animated) {
        final int oldViewMode = mViewMode.getMode();
        mLayout.postDelayed(new Runnable() {
                @Override
            public void run() {
                if (/* the touch did not open a conversation */oldViewMode == mViewMode.getMode() ||
                /* animation has ended */!mToastBar.isAnimating()) {
                    mToastBar.hide(animated, false /* actionClicked */);
                } else {
                    // the touch opened a conversation, reposition undo bar
                    repositionToastBar(mToastBar.getOperation());
                }
            }
        },
        /* Give time for ViewMode to change from the touch */
        mContext.getResources().getInteger(R.integer.dismiss_undo_bar_delay_ms));
    }

    @Override
    public void onError(final Folder folder, boolean replaceVisibleToast) {
        repositionToastBar(true /* convModeShowInList */);
        showErrorToast(folder, replaceVisibleToast);
    }

    @Override
    public boolean isDrawerEnabled() {
        return mLayout.isDrawerEnabled();
    }

    @Override
    public int getFolderListViewChoiceMode() {
        // By default, we want to allow one item to be selected in the folder list
        return ListView.CHOICE_MODE_SINGLE;
    }

    private int mMiscellaneousViewTransactionId = -1;

    @Override
    public void launchFragment(final Fragment fragment, final int selectPosition) {
        final int containerViewId = TwoPaneLayout.MISCELLANEOUS_VIEW_ID;

        final FragmentManager fragmentManager = mActivity.getFragmentManager();
        if (fragmentManager.findFragmentByTag(TAG_CUSTOM_FRAGMENT) == null) {
            final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.replace(containerViewId, fragment, TAG_CUSTOM_FRAGMENT);
            mMiscellaneousViewTransactionId = fragmentTransaction.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }

        if (selectPosition >= 0) {
            getConversationListFragment().setRawSelected(selectPosition, true);
        }
    }
}
