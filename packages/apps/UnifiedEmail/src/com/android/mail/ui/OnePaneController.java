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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.widget.ListView;

import com.android.mail.ConversationListContext;
import com.android.mail.R;
import com.android.mail.providers.Account;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.FolderUri;
import com.android.mail.utils.Utils;

/**
 * Controller for one-pane Mail activity. One Pane is used for phones, where screen real estate is
 * limited. This controller also does the layout, since the layout is simpler in the one pane case.
 */

public final class OnePaneController extends AbstractActivityController {
    /** Key used to store {@link #mLastConversationListTransactionId} */
    private static final String CONVERSATION_LIST_TRANSACTION_KEY = "conversation-list-transaction";
    /** Key used to store {@link #mLastConversationTransactionId}. */
    private static final String CONVERSATION_TRANSACTION_KEY = "conversation-transaction";
    /** Key used to store {@link #mConversationListVisible}. */
    private static final String CONVERSATION_LIST_VISIBLE_KEY = "conversation-list-visible";
    /** Key used to store {@link #mConversationListNeverShown}. */
    private static final String CONVERSATION_LIST_NEVER_SHOWN_KEY = "conversation-list-never-shown";

    private static final int INVALID_ID = -1;
    private boolean mConversationListVisible = false;
    private int mLastConversationListTransactionId = INVALID_ID;
    private int mLastConversationTransactionId = INVALID_ID;
    /** Whether a conversation list for this account has ever been shown.*/
    private boolean mConversationListNeverShown = true;

    public OnePaneController(MailActivity activity, ViewMode viewMode) {
        super(activity, viewMode);
    }

    @Override
    public void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
        if (inState == null) {
            return;
        }
        mLastConversationListTransactionId =
                inState.getInt(CONVERSATION_LIST_TRANSACTION_KEY, INVALID_ID);
        mLastConversationTransactionId = inState.getInt(CONVERSATION_TRANSACTION_KEY, INVALID_ID);
        mConversationListVisible = inState.getBoolean(CONVERSATION_LIST_VISIBLE_KEY);
        mConversationListNeverShown = inState.getBoolean(CONVERSATION_LIST_NEVER_SHOWN_KEY);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CONVERSATION_LIST_TRANSACTION_KEY, mLastConversationListTransactionId);
        outState.putInt(CONVERSATION_TRANSACTION_KEY, mLastConversationTransactionId);
        outState.putBoolean(CONVERSATION_LIST_VISIBLE_KEY, mConversationListVisible);
        outState.putBoolean(CONVERSATION_LIST_NEVER_SHOWN_KEY, mConversationListNeverShown);
    }

    @Override
    public void resetActionBarIcon() {
        // Calling resetActionBarIcon should never remove the up affordance
        // even when waiting for sync (Folder list should still show with one
        // account. Currently this method is blank to avoid any changes.
    }

    /**
     * Returns true if the candidate URI is the URI for the default inbox for the given account.
     * @param candidate the URI to check
     * @param account the account whose default Inbox the candidate might be
     * @return true if the candidate is indeed the default inbox for the given account.
     */
    private static boolean isDefaultInbox(FolderUri candidate, Account account) {
        return (candidate != null && account != null)
                && candidate.equals(account.settings.defaultInbox);
    }

    /**
     * Returns true if the user is currently in the conversation list view, viewing the default
     * inbox.
     * @return true if user is in conversation list mode, viewing the default inbox.
     */
    private static boolean inInbox(final Account account, final ConversationListContext context) {
        // If we don't have valid state, then we are not in the inbox.
        return !(account == null || context == null || context.folder == null
                || account.settings == null) && !ConversationListContext.isSearchResult(context)
                && isDefaultInbox(context.folder.folderUri, account);
    }

    /**
     * On account change, carry out super implementation, load FolderListFragment
     * into drawer (to avoid repetitive calls to replaceFragment).
     */
    @Override
    public void changeAccount(Account account) {
        super.changeAccount(account);
        mConversationListNeverShown = true;
        closeDrawerIfOpen();
    }

    @Override
    public boolean onCreate(Bundle savedInstanceState) {
        mActivity.setContentView(R.layout.one_pane_activity);
        mDrawerContainer = (DrawerLayout) mActivity.findViewById(R.id.drawer_container);
        mDrawerPullout = mDrawerContainer.findViewById(R.id.drawer_pullout);
        mDrawerPullout.setBackgroundResource(R.color.list_background_color);

        // The parent class sets the correct viewmode and starts the application off.
        return super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean isConversationListVisible() {
        return mConversationListVisible;
    }

    @Override
    public void onViewModeChanged(int newMode) {
        super.onViewModeChanged(newMode);

        // When entering conversation list mode, hide and clean up any currently visible
        // conversation.
        if (ViewMode.isListMode(newMode)) {
            mPagerController.hide(true /* changeVisibility */);
        }
        // When we step away from the conversation mode, we don't have a current conversation
        // anymore. Let's blank it out so clients calling getCurrentConversation are not misled.
        if (!ViewMode.isConversationMode(newMode)) {
            setCurrentConversation(null);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" lastConvListTransId=");
        sb.append(mLastConversationListTransactionId);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void showConversationList(ConversationListContext listContext) {
        super.showConversationList(listContext);
        enableCabMode();
        mConversationListVisible = true;
        if (ConversationListContext.isSearchResult(listContext)) {
            mViewMode.enterSearchResultsListMode();
        } else {
            mViewMode.enterConversationListMode();
        }
        final int transition = mConversationListNeverShown
                ? FragmentTransaction.TRANSIT_FRAGMENT_FADE
                : FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
        final Fragment conversationListFragment =
                ConversationListFragment.newInstance(listContext);

        if (!inInbox(mAccount, listContext)) {
            // Maintain fragment transaction history so we can get back to the
            // fragment used to launch this list.
            mLastConversationListTransactionId = replaceFragment(conversationListFragment,
                    transition, TAG_CONVERSATION_LIST, R.id.content_pane);
        } else {
            // If going to the inbox, clear the folder list transaction history.
            mInbox = listContext.folder;
            replaceFragment(conversationListFragment, transition, TAG_CONVERSATION_LIST,
                    R.id.content_pane);

            // If we ever to to the inbox, we want to unset the transation id for any other
            // non-inbox folder.
            mLastConversationListTransactionId = INVALID_ID;
        }

        mActivity.getFragmentManager().executePendingTransactions();

        onConversationVisibilityChanged(false);
        onConversationListVisibilityChanged(true);
        mConversationListNeverShown = false;
    }

    @Override
    protected void showConversation(Conversation conversation, boolean inLoaderCallbacks) {
        super.showConversation(conversation, inLoaderCallbacks);
        mConversationListVisible = false;
        if (conversation == null) {
            transitionBackToConversationListMode();
            return;
        }
        disableCabMode();
        if (ConversationListContext.isSearchResult(mConvListContext)) {
            mViewMode.enterSearchResultsConversationMode();
        } else {
            mViewMode.enterConversationMode();
        }
        final FragmentManager fm = mActivity.getFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        // Switching to conversation view is an incongruous transition:
        // we are not replacing a fragment with another fragment as
        // usual. Instead, reveal the heretofore inert conversation
        // ViewPager and just remove the previously visible fragment
        // e.g. conversation list, or possibly label list?).
        final Fragment f = fm.findFragmentById(R.id.content_pane);
        // FragmentManager#findFragmentById can return fragments that are not added to the activity.
        // We want to make sure that we don't attempt to remove fragments that are not added to the
        // activity, as when the transaction is popped off, the FragmentManager will attempt to
        // readd the same fragment twice
        if (f != null && f.isAdded()) {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.remove(f);
            ft.commitAllowingStateLoss();
            fm.executePendingTransactions();
        }
        mPagerController.show(mAccount, mFolder, conversation, true /* changeVisibility */);
        onConversationVisibilityChanged(true);
        onConversationListVisibilityChanged(false);
    }

    @Override
    public void showWaitForInitialization() {
        super.showWaitForInitialization();
        replaceFragment(getWaitFragment(), FragmentTransaction.TRANSIT_FRAGMENT_OPEN, TAG_WAIT,
                R.id.content_pane);
    }

    @Override
    protected void hideWaitForInitialization() {
        transitionToInbox();
        super.hideWaitForInitialization();
    }

    @Override
    public boolean doesActionChangeConversationListVisibility(final int action) {
        if (action == R.id.archive
                || action == R.id.remove_folder
                || action == R.id.delete
                || action == R.id.discard_drafts
                || action == R.id.mark_important
                || action == R.id.mark_not_important
                || action == R.id.mute
                || action == R.id.report_spam
                || action == R.id.mark_not_spam
                || action == R.id.report_phishing
                || action == R.id.refresh
                || action == R.id.change_folders) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Replace the content_pane with the fragment specified here. The tag is specified so that
     * the {@link ActivityController} can look up the fragments through the
     * {@link android.app.FragmentManager}.
     * @param fragment the new fragment to put
     * @param transition the transition to show
     * @param tag a tag for the fragment manager.
     * @param anchor ID of view to replace fragment in
     * @return transaction ID returned when the transition is committed.
     */
    private int replaceFragment(Fragment fragment, int transition, String tag, int anchor) {
        final FragmentManager fm = mActivity.getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.setTransition(transition);
        fragmentTransaction.replace(anchor, fragment, tag);
        final int id = fragmentTransaction.commitAllowingStateLoss();
        fm.executePendingTransactions();
        return id;
    }

    /**
     * Back works as follows:
     * 1) If the drawer is pulled out (Or mid-drag), close it - handled.
     * 2) If the user is in the folder list view, go back
     * to the account default inbox.
     * 3) If the user is in a conversation list
     * that is not the inbox AND:
     *  a) they got there by going through the folder
     *  list view, go back to the folder list view.
     *  b) they got there by using some other means (account dropdown), go back to the inbox.
     * 4) If the user is in a conversation, go back to the conversation list they were last in.
     * 5) If the user is in the conversation list for the default account inbox,
     * back exits the app.
     */
    @Override
    public boolean handleBackPress() {
        final int mode = mViewMode.getMode();

        if (mode == ViewMode.SEARCH_RESULTS_LIST) {
            mActivity.finish();
        } else if (mViewMode.isListMode() && !inInbox(mAccount, mConvListContext)) {
            navigateUpFolderHierarchy();
        } else if (mViewMode.isConversationMode() || mViewMode.isAdMode()) {
            transitionBackToConversationListMode();
        } else {
            mActivity.finish();
        }
        mToastBar.hide(false, false /* actionClicked */);
        return true;
    }

    /**
     * Switch to the Inbox by creating a new conversation list context that loads the inbox.
     */
    private void transitionToInbox() {
        // The inbox could have changed, in which case we should load it again.
        if (mInbox == null || !isDefaultInbox(mInbox.folderUri, mAccount)) {
            loadAccountInbox();
        } else {
            onFolderChanged(mInbox, false /* force */);
        }
    }

    @Override
    public void onFolderSelected(Folder folder) {
        setHierarchyFolder(folder);
        super.onFolderSelected(folder);
    }

    /**
     * Up works as follows:
     * 1) If the user is in a conversation list that is not the default account inbox,
     * a conversation, or the folder list, up follows the rules of back.
     * 2) If the user is in search results, up exits search
     * mode and returns the user to whatever view they were in when they began search.
     * 3) If the user is in the inbox, there is no up.
     */
    @Override
    public boolean handleUpPress() {
        final int mode = mViewMode.getMode();
        if (mode == ViewMode.SEARCH_RESULTS_LIST) {
            mActivity.finish();
            // Not needed, the activity is going away anyway.
        } else if (mode == ViewMode.CONVERSATION_LIST
                || mode == ViewMode.WAITING_FOR_ACCOUNT_INITIALIZATION) {
            final boolean isTopLevel = (mFolder == null) || (mFolder.parent == Uri.EMPTY);

            if (isTopLevel) {
                // Show the drawer.
                toggleDrawerState();
            } else {
                navigateUpFolderHierarchy();
            }
        } else if (mode == ViewMode.CONVERSATION || mode == ViewMode.SEARCH_RESULTS_CONVERSATION
                || mode == ViewMode.AD) {
            // Same as go back.
            handleBackPress();
        }
        return true;
    }

    private void transitionBackToConversationListMode() {
        final int mode = mViewMode.getMode();
        enableCabMode();
        mConversationListVisible = true;
        if (mode == ViewMode.SEARCH_RESULTS_CONVERSATION) {
            mViewMode.enterSearchResultsListMode();
        } else {
            mViewMode.enterConversationListMode();
        }

        final Folder folder = mFolder != null ? mFolder : mInbox;
        onFolderChanged(folder, true /* force */);

        onConversationVisibilityChanged(false);
        onConversationListVisibilityChanged(true);
    }

    @Override
    public boolean shouldShowFirstConversation() {
        return false;
    }

    @Override
    public void onUndoAvailable(ToastBarOperation op) {
        if (op != null && mAccount.supportsCapability(UIProvider.AccountCapabilities.UNDO)) {
            final int mode = mViewMode.getMode();
            final ConversationListFragment convList = getConversationListFragment();
            switch (mode) {
                case ViewMode.SEARCH_RESULTS_CONVERSATION:
                case ViewMode.CONVERSATION:
                    mToastBar.show(getUndoClickedListener(
                            convList != null ? convList.getAnimatedAdapter() : null),
                            0,
                            Utils.convertHtmlToPlainText
                                (op.getDescription(mActivity.getActivityContext())),
                            true, /* showActionIcon */
                            R.string.undo,
                            true,  /* replaceVisibleToast */
                            op);
                    break;
                case ViewMode.SEARCH_RESULTS_LIST:
                case ViewMode.CONVERSATION_LIST:
                    if (convList != null) {
                        mToastBar.show(
                                getUndoClickedListener(convList.getAnimatedAdapter()),
                                0,
                                Utils.convertHtmlToPlainText
                                    (op.getDescription(mActivity.getActivityContext())),
                                true, /* showActionIcon */
                                R.string.undo,
                                true,  /* replaceVisibleToast */
                                op);
                    } else {
                        mActivity.setPendingToastOperation(op);
                    }
                    break;
            }
        }
    }

    @Override
    protected void hideOrRepositionToastBar(boolean animated) {
        mToastBar.hide(animated, false /* actionClicked */);
    }

    @Override
    public void onError(final Folder folder, boolean replaceVisibleToast) {
        final int mode = mViewMode.getMode();
        switch (mode) {
            case ViewMode.SEARCH_RESULTS_LIST:
            case ViewMode.CONVERSATION_LIST:
                showErrorToast(folder, replaceVisibleToast);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean isDrawerEnabled() {
        // The drawer is enabled for one pane mode
        return true;
    }

    @Override
    public int getFolderListViewChoiceMode() {
        // By default, we do not want to allow any item to be selected in the folder list
        return ListView.CHOICE_MODE_NONE;
    }

    @Override
    public void launchFragment(final Fragment fragment, final int selectPosition) {
        replaceFragment(fragment, FragmentTransaction.TRANSIT_FRAGMENT_OPEN,
                TAG_CUSTOM_FRAGMENT, R.id.content_pane);
    }
}
