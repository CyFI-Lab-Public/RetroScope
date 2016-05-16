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
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.browse.ConversationAccountController;
import com.android.mail.browse.ConversationMessage;
import com.android.mail.browse.ConversationViewHeader.ConversationViewHeaderCallbacks;
import com.android.mail.browse.MessageCursor;
import com.android.mail.browse.MessageCursor.ConversationController;
import com.android.mail.content.ObjectCursor;
import com.android.mail.content.ObjectCursorLoader;
import com.android.mail.providers.Account;
import com.android.mail.providers.AccountObserver;
import com.android.mail.providers.Address;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.ListParams;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.CursorStatus;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public abstract class AbstractConversationViewFragment extends Fragment implements
        ConversationController, ConversationAccountController,
        ConversationViewHeaderCallbacks {

    protected static final String ARG_ACCOUNT = "account";
    public static final String ARG_CONVERSATION = "conversation";
    private static final String LOG_TAG = LogTag.getLogTag();
    protected static final int MESSAGE_LOADER = 0;
    protected static final int CONTACT_LOADER = 1;
    protected ControllableActivity mActivity;
    private final MessageLoaderCallbacks mMessageLoaderCallbacks = new MessageLoaderCallbacks();
    private ContactLoaderCallbacks mContactLoaderCallbacks;
    private MenuItem mChangeFoldersMenuItem;
    protected Conversation mConversation;
    protected String mBaseUri;
    protected Account mAccount;

    /**
     * Must be instantiated in a derived class's onCreate.
     */
    protected AbstractConversationWebViewClient mWebViewClient;

    /**
     * Cache of email address strings to parsed Address objects.
     * <p>
     * Remember to synchronize on the map when reading or writing to this cache, because some
     * instances use it off the UI thread (e.g. from WebView).
     */
    protected final Map<String, Address> mAddressCache = Collections.synchronizedMap(
            new HashMap<String, Address>());
    private MessageCursor mCursor;
    private Context mContext;
    /**
     * A backwards-compatible version of {{@link #getUserVisibleHint()}. Like the framework flag,
     * this flag is saved and restored.
     */
    private boolean mUserVisible;

    private final Handler mHandler = new Handler();
    /** True if we want to avoid marking the conversation as viewed and read. */
    private boolean mSuppressMarkingViewed;
    /**
     * Parcelable state of the conversation view. Can safely be used without null checking any time
     * after {@link #onCreate(Bundle)}.
     */
    protected ConversationViewState mViewState;

    private boolean mIsDetached;

    private boolean mHasConversationBeenTransformed;
    private boolean mHasConversationTransformBeenReverted;

    private final AccountObserver mAccountObserver = new AccountObserver() {
        @Override
        public void onChanged(Account newAccount) {
            final Account oldAccount = mAccount;
            mAccount = newAccount;
            mWebViewClient.setAccount(mAccount);
            onAccountChanged(newAccount, oldAccount);
        }
    };

    private static final String BUNDLE_VIEW_STATE =
            AbstractConversationViewFragment.class.getName() + "viewstate";
    /**
     * We save the user visible flag so the various transitions that occur during rotation do not
     * cause unnecessary visibility change.
     */
    private static final String BUNDLE_USER_VISIBLE =
            AbstractConversationViewFragment.class.getName() + "uservisible";

    private static final String BUNDLE_DETACHED =
            AbstractConversationViewFragment.class.getName() + "detached";

    private static final String BUNDLE_KEY_HAS_CONVERSATION_BEEN_TRANSFORMED =
            AbstractConversationViewFragment.class.getName() + "conversationtransformed";
    private static final String BUNDLE_KEY_HAS_CONVERSATION_BEEN_REVERTED =
            AbstractConversationViewFragment.class.getName() + "conversationreverted";

    public static Bundle makeBasicArgs(Account account) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_ACCOUNT, account);
        return args;
    }

    /**
     * Constructor needs to be public to handle orientation changes and activity
     * lifecycle events.
     */
    public AbstractConversationViewFragment() {
        super();
    }

    /**
     * Subclasses must override, since this depends on how many messages are
     * shown in the conversation view.
     */
    protected void markUnread() {
        // Do not automatically mark this conversation viewed and read.
        mSuppressMarkingViewed = true;
    }

    /**
     * Subclasses must override this, since they may want to display a single or
     * many messages related to this conversation.
     */
    protected abstract void onMessageCursorLoadFinished(
            Loader<ObjectCursor<ConversationMessage>> loader,
            MessageCursor newCursor, MessageCursor oldCursor);

    /**
     * Subclasses must override this, since they may want to display a single or
     * many messages related to this conversation.
     */
    @Override
    public abstract void onConversationViewHeaderHeightChange(int newHeight);

    public abstract void onUserVisibleHintChanged();

    /**
     * Subclasses must override this.
     */
    protected abstract void onAccountChanged(Account newAccount, Account oldAccount);

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        parseArguments();
        setBaseUri();

        LogUtils.d(LOG_TAG, "onCreate in ConversationViewFragment (this=%s)", this);
        // Not really, we just want to get a crack to store a reference to the change_folder item
        setHasOptionsMenu(true);

        if (savedState != null) {
            mViewState = savedState.getParcelable(BUNDLE_VIEW_STATE);
            mUserVisible = savedState.getBoolean(BUNDLE_USER_VISIBLE);
            mIsDetached = savedState.getBoolean(BUNDLE_DETACHED, false);
            mHasConversationBeenTransformed =
                    savedState.getBoolean(BUNDLE_KEY_HAS_CONVERSATION_BEEN_TRANSFORMED, false);
            mHasConversationTransformBeenReverted =
                    savedState.getBoolean(BUNDLE_KEY_HAS_CONVERSATION_BEEN_REVERTED, false);
        } else {
            mViewState = getNewViewState();
            mHasConversationBeenTransformed = false;
            mHasConversationTransformBeenReverted = false;
        }
    }

    /**
     * Can be overridden in case a subclass needs to get additional arguments.
     */
    protected void parseArguments() {
        final Bundle args = getArguments();
        mAccount = args.getParcelable(ARG_ACCOUNT);
        mConversation = args.getParcelable(ARG_CONVERSATION);
    }

    /**
     * Can be overridden in case a subclass needs a different uri format
     * (such as one that does not rely on account and/or conversation.
     */
    protected void setBaseUri() {
        // Since the uri specified in the conversation base uri may not be unique, we specify a
        // base uri that us guaranteed to be unique for this conversation.
        mBaseUri = "x-thread://" + mAccount.getEmailAddress().hashCode() + "/" + mConversation.id;
    }

    @Override
    public String toString() {
        // log extra info at DEBUG level or finer
        final String s = super.toString();
        if (!LogUtils.isLoggable(LOG_TAG, LogUtils.DEBUG) || mConversation == null) {
            return s;
        }
        return "(" + s + " conv=" + mConversation + ")";
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Activity activity = getActivity();
        if (!(activity instanceof ControllableActivity)) {
            LogUtils.wtf(LOG_TAG, "ConversationViewFragment expects only a ControllableActivity to"
                    + "create it. Cannot proceed.");
        }
        if (activity == null || activity.isFinishing()) {
            // Activity is finishing, just bail.
            return;
        }
        mActivity = (ControllableActivity) activity;
        mContext = activity.getApplicationContext();
        mWebViewClient.setActivity(activity);
        mAccount = mAccountObserver.initialize(mActivity.getAccountController());
        mWebViewClient.setAccount(mAccount);
    }

    @Override
    public ConversationUpdater getListController() {
        final ControllableActivity activity = (ControllableActivity) getActivity();
        return activity != null ? activity.getConversationUpdater() : null;
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public Conversation getConversation() {
        return mConversation;
    }

    @Override
    public MessageCursor getMessageCursor() {
        return mCursor;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public MessageLoaderCallbacks getMessageLoaderCallbacks() {
        return mMessageLoaderCallbacks;
    }

    public ContactLoaderCallbacks getContactInfoSource() {
        if (mContactLoaderCallbacks == null) {
            mContactLoaderCallbacks = new ContactLoaderCallbacks(mActivity.getActivityContext());
        }
        return mContactLoaderCallbacks;
    }

    @Override
    public Account getAccount() {
        return mAccount;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mChangeFoldersMenuItem = menu.findItem(R.id.change_folders);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!isUserVisible()) {
            // Unclear how this is happening. Current theory is that this fragment was scheduled
            // to be removed, but the remove transaction failed. When the Activity is later
            // restored, the FragmentManager restores this fragment, but Fragment.mMenuVisible is
            // stuck at its initial value (true), which makes this zombie fragment eligible for
            // menu item clicks.
            //
            // Work around this by relying on the (properly restored) extra user visible hint.
            LogUtils.e(LOG_TAG,
                    "ACVF ignoring onOptionsItemSelected b/c userVisibleHint is false. f=%s", this);
            if (LogUtils.isLoggable(LOG_TAG, LogUtils.DEBUG)) {
                LogUtils.e(LOG_TAG, Utils.dumpFragment(this));  // the dump has '%' chars in it...
            }
            return false;
        }

        boolean handled = false;
        final int itemId = item.getItemId();
        if (itemId == R.id.inside_conversation_unread) {
            markUnread();
            handled = true;
        } else if (itemId == R.id.show_original) {
            showUntransformedConversation();
            handled = true;
        }
        return handled;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Only show option if we support message transforms and message has been transformed.
        Utils.setMenuItemVisibility(menu, R.id.show_original, supportsMessageTransforms() &&
                mHasConversationBeenTransformed && !mHasConversationTransformBeenReverted);
    }

    abstract boolean supportsMessageTransforms();

    // BEGIN conversation header callbacks
    @Override
    public void onFoldersClicked() {
        if (mChangeFoldersMenuItem == null) {
            LogUtils.e(LOG_TAG, "unable to open 'change folders' dialog for a conversation");
            return;
        }
        mActivity.onOptionsItemSelected(mChangeFoldersMenuItem);
    }
    // END conversation header callbacks

    @Override
    public void onStart() {
        super.onStart();

        Analytics.getInstance().sendView(getClass().getName());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mViewState != null) {
            outState.putParcelable(BUNDLE_VIEW_STATE, mViewState);
        }
        outState.putBoolean(BUNDLE_USER_VISIBLE, mUserVisible);
        outState.putBoolean(BUNDLE_DETACHED, mIsDetached);
        outState.putBoolean(BUNDLE_KEY_HAS_CONVERSATION_BEEN_TRANSFORMED,
                mHasConversationBeenTransformed);
        outState.putBoolean(BUNDLE_KEY_HAS_CONVERSATION_BEEN_REVERTED,
                mHasConversationTransformBeenReverted);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAccountObserver.unregisterAndDestroy();
    }

    /**
     * {@link #setUserVisibleHint(boolean)} only works on API >= 15, so implement our own for
     * reliability on older platforms.
     */
    public void setExtraUserVisibleHint(boolean isVisibleToUser) {
        LogUtils.v(LOG_TAG, "in CVF.setHint, val=%s (%s)", isVisibleToUser, this);
        if (mUserVisible != isVisibleToUser) {
            mUserVisible = isVisibleToUser;
            MessageCursor cursor = getMessageCursor();
            if (mUserVisible && (cursor != null && cursor.isLoaded() && cursor.getCount() == 0)) {
                // Pop back to conversation list and show error.
                onError();
                return;
            }
            onUserVisibleHintChanged();
        }
    }

    public boolean isUserVisible() {
        return mUserVisible;
    }

    protected void timerMark(String msg) {
        if (isUserVisible()) {
            Utils.sConvLoadTimer.mark(msg);
        }
    }

    private class MessageLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<ObjectCursor<ConversationMessage>> {

        @Override
        public Loader<ObjectCursor<ConversationMessage>> onCreateLoader(int id, Bundle args) {
            return new MessageLoader(mActivity.getActivityContext(), mConversation.messageListUri);
        }

        @Override
        public void onLoadFinished(Loader<ObjectCursor<ConversationMessage>> loader,
                    ObjectCursor<ConversationMessage> data) {
            // ignore truly duplicate results
            // this can happen when restoring after rotation
            if (mCursor == data) {
                return;
            } else {
                final MessageCursor messageCursor = (MessageCursor) data;

                // bind the cursor to this fragment so it can access to the current list controller
                messageCursor.setController(AbstractConversationViewFragment.this);

                if (LogUtils.isLoggable(LOG_TAG, LogUtils.DEBUG)) {
                    LogUtils.d(LOG_TAG, "LOADED CONVERSATION= %s", messageCursor.getDebugDump());
                }

                // We have no messages: exit conversation view.
                if (messageCursor.getCount() == 0
                        && (!CursorStatus.isWaitingForResults(messageCursor.getStatus())
                                || mIsDetached)) {
                    if (mUserVisible) {
                        onError();
                    } else {
                        // we expect that the pager adapter will remove this
                        // conversation fragment on its own due to a separate
                        // conversation cursor update (we might get here if the
                        // message list update fires first. nothing to do
                        // because we expect to be torn down soon.)
                        LogUtils.i(LOG_TAG, "CVF: offscreen conv has no messages, ignoring update"
                                + " in anticipation of conv cursor update. c=%s",
                                mConversation.uri);
                    }
                    // existing mCursor will imminently be closed, must stop referencing it
                    // since we expect to be kicked out soon, it doesn't matter what mCursor
                    // becomes
                    mCursor = null;
                    return;
                }

                // ignore cursors that are still loading results
                if (!messageCursor.isLoaded()) {
                    // existing mCursor will imminently be closed, must stop referencing it
                    // in this case, the new cursor is also no good, and since don't expect to get
                    // here except in initial load situations, it's safest to just ensure the
                    // reference is null
                    mCursor = null;
                    return;
                }
                final MessageCursor oldCursor = mCursor;
                mCursor = messageCursor;
                onMessageCursorLoadFinished(loader, mCursor, oldCursor);
            }
        }

        @Override
        public void onLoaderReset(Loader<ObjectCursor<ConversationMessage>>  loader) {
            mCursor = null;
        }

    }

    private void onError() {
        // need to exit this view- conversation may have been
        // deleted, or for whatever reason is now invalid (e.g.
        // discard single draft)
        //
        // N.B. this may involve a fragment transaction, which
        // FragmentManager will refuse to execute directly
        // within onLoadFinished. Make sure the controller knows.
        LogUtils.i(LOG_TAG, "CVF: visible conv has no messages, exiting conv mode");
        // TODO(mindyp): handle ERROR status by showing an error
        // message to the user that there are no messages in
        // this conversation
        popOut();
    }

    private void popOut() {
        mHandler.post(new FragmentRunnable("popOut", this) {
            @Override
            public void go() {
                if (mActivity != null) {
                    mActivity.getListHandler()
                            .onConversationSelected(null, true /* inLoaderCallbacks */);
                }
            }
        });
    }

    protected void onConversationSeen() {
        LogUtils.d(LOG_TAG, "AbstractConversationViewFragment#onConversationSeen()");

        // Ignore unsafe calls made after a fragment is detached from an activity
        final ControllableActivity activity = (ControllableActivity) getActivity();
        if (activity == null) {
            LogUtils.w(LOG_TAG, "ignoring onConversationSeen for conv=%s", mConversation.id);
            return;
        }

        mViewState.setInfoForConversation(mConversation);

        LogUtils.d(LOG_TAG, "onConversationSeen() - mSuppressMarkingViewed = %b",
                mSuppressMarkingViewed);
        // In most circumstances we want to mark the conversation as viewed and read, since the
        // user has read it.  However, if the user has already marked the conversation unread, we
        // do not want a  later mark-read operation to undo this.  So we check this variable which
        // is set in #markUnread() which suppresses automatic mark-read.
        if (!mSuppressMarkingViewed) {
            // mark viewed/read if not previously marked viewed by this conversation view,
            // or if unread messages still exist in the message list cursor
            // we don't want to keep marking viewed on rotation or restore
            // but we do want future re-renders to mark read (e.g. "New message from X" case)
            final MessageCursor cursor = getMessageCursor();
            LogUtils.d(LOG_TAG, "onConversationSeen() - mConversation.isViewed() = %b, "
                    + "cursor null = %b, cursor.isConversationRead() = %b",
                    mConversation.isViewed(), cursor == null,
                    cursor != null && cursor.isConversationRead());
            if (!mConversation.isViewed() || (cursor != null && !cursor.isConversationRead())) {
                // Mark the conversation viewed and read.
                activity.getConversationUpdater()
                        .markConversationsRead(Arrays.asList(mConversation), true, true);

                // and update the Message objects in the cursor so the next time a cursor update
                // happens with these messages marked read, we know to ignore it
                if (cursor != null && !cursor.isClosed()) {
                    cursor.markMessagesRead();
                }
            }
        }
        activity.getListHandler().onConversationSeen();
    }

    protected ConversationViewState getNewViewState() {
        return new ConversationViewState();
    }

    private static class MessageLoader extends ObjectCursorLoader<ConversationMessage> {
        private boolean mDeliveredFirstResults = false;

        public MessageLoader(Context c, Uri messageListUri) {
            super(c, messageListUri, UIProvider.MESSAGE_PROJECTION, ConversationMessage.FACTORY);
        }

        @Override
        public void deliverResult(ObjectCursor<ConversationMessage> result) {
            // We want to deliver these results, and then we want to make sure
            // that any subsequent
            // queries do not hit the network
            super.deliverResult(result);

            if (!mDeliveredFirstResults) {
                mDeliveredFirstResults = true;
                Uri uri = getUri();

                // Create a ListParams that tells the provider to not hit the
                // network
                final ListParams listParams = new ListParams(ListParams.NO_LIMIT,
                        false /* useNetwork */);

                // Build the new uri with this additional parameter
                uri = uri
                        .buildUpon()
                        .appendQueryParameter(UIProvider.LIST_PARAMS_QUERY_PARAMETER,
                                listParams.serialize()).build();
                setUri(uri);
            }
        }

        @Override
        protected ObjectCursor<ConversationMessage> getObjectCursor(Cursor inner) {
            return new MessageCursor(inner);
        }
    }

    public abstract void onConversationUpdated(Conversation conversation);

    public void onDetachedModeEntered() {
        // If we have no messages, then we have nothing to display, so leave this view.
        // Otherwise, just set the detached flag.
        final Cursor messageCursor = getMessageCursor();

        if (messageCursor == null || messageCursor.getCount() == 0) {
            popOut();
        } else {
            mIsDetached = true;
        }
    }

    /**
     * Called when the JavaScript reports that it transformed a message.
     * Sets a flag to true and invalidates the options menu so it will
     * include the "Revert auto-sizing" menu option.
     */
    public void onConversationTransformed() {
        mHasConversationBeenTransformed = true;
        mHandler.post(new FragmentRunnable("invalidateOptionsMenu", this) {
            @Override
            public void go() {
                mActivity.invalidateOptionsMenu();
            }
        });
    }

    /**
     * Called when the "Revert auto-sizing" option is selected. Default
     * implementation simply sets a value on whether transforms should be
     * applied. Derived classes should override this class and force a
     * re-render so that the conversation renders without
     */
    public void showUntransformedConversation() {
        // must set the value to true so we don't show the options menu item again
        mHasConversationTransformBeenReverted = true;
    }

    /**
     * Returns {@code true} if the conversation should be transformed. {@code false}, otherwise.
     * @return {@code true} if the conversation should be transformed. {@code false}, otherwise.
     */
    public boolean shouldApplyTransforms() {
        return (mAccount.enableMessageTransforms > 0) &&
                !mHasConversationTransformBeenReverted;
    }
}
