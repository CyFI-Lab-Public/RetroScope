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

import android.app.Fragment;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.android.mail.browse.ConversationAccountController;
import com.android.mail.browse.ConversationMessage;
import com.android.mail.browse.ConversationViewHeader;
import com.android.mail.browse.MessageCursor;
import com.android.mail.browse.MessageHeaderView;
import com.android.mail.content.ObjectCursor;
import com.android.mail.providers.Account;
import com.android.mail.providers.Address;
import com.android.mail.providers.Conversation;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SecureConversationViewFragment extends AbstractConversationViewFragment
        implements SecureConversationViewControllerCallbacks {
    private static final String LOG_TAG = LogTag.getLogTag();

    private SecureConversationViewController mViewController;

    private class SecureConversationWebViewClient extends AbstractConversationWebViewClient {
        public SecureConversationWebViewClient(Account account) {
            super(account);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // Ignore unsafe calls made after a fragment is detached from an activity.
            // This method needs to, for example, get at the loader manager, which needs
            // the fragment to be added.
            if (!isAdded()) {
                LogUtils.d(LOG_TAG, "ignoring SCVF.onPageFinished, url=%s fragment=%s", url,
                        SecureConversationViewFragment.this);
                return;
            }

            if (isUserVisible()) {
                onConversationSeen();
            }

            mViewController.dismissLoadingStatus();

            final Set<String> emailAddresses = Sets.newHashSet();
            final List<Address> cacheCopy;
            synchronized (mAddressCache) {
                cacheCopy = ImmutableList.copyOf(mAddressCache.values());
            }
            for (Address addr : cacheCopy) {
                emailAddresses.add(addr.getAddress());
            }
            final ContactLoaderCallbacks callbacks = getContactInfoSource();
            callbacks.setSenders(emailAddresses);
            getLoaderManager().restartLoader(CONTACT_LOADER, Bundle.EMPTY, callbacks);
        }
    }

    /**
     * Creates a new instance of {@link ConversationViewFragment}, initialized
     * to display a conversation with other parameters inherited/copied from an
     * existing bundle, typically one created using {@link #makeBasicArgs}.
     */
    public static SecureConversationViewFragment newInstance(Bundle existingArgs,
            Conversation conversation) {
        SecureConversationViewFragment f = new SecureConversationViewFragment();
        Bundle args = new Bundle(existingArgs);
        args.putParcelable(ARG_CONVERSATION, conversation);
        f.setArguments(args);
        return f;
    }

    /**
     * Constructor needs to be public to handle orientation changes and activity
     * lifecycle events.
     */
    public SecureConversationViewFragment() {}

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mWebViewClient = new SecureConversationWebViewClient(mAccount);
        mViewController = new SecureConversationViewController(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return mViewController.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewController.onActivityCreated(savedInstanceState);
    }

    // Start implementations of SecureConversationViewControllerCallbacks

    @Override
    public Fragment getFragment() {
        return this;
    }

    @Override
    public AbstractConversationWebViewClient getWebViewClient() {
        return mWebViewClient;
    }

    @Override
    public void setupConversationHeaderView(ConversationViewHeader headerView) {
        headerView.setCallbacks(this, this);
        headerView.setFolders(mConversation);
        headerView.setSubject(mConversation.subject);
    }

    @Override
    public boolean isViewVisibleToUser() {
        return isUserVisible();
    }

    @Override
    public ConversationAccountController getConversationAccountController() {
        return this;
    }

    @Override
    public Map<String, Address> getAddressCache() {
        return mAddressCache;
    }

    @Override
    public void setupMessageHeaderVeiledMatcher(MessageHeaderView messageHeaderView) {
        messageHeaderView.setVeiledMatcher(
                ((ControllableActivity) getActivity()).getAccountController()
                        .getVeiledAddressMatcher());
    }

    @Override
    public void startMessageLoader() {
        getLoaderManager().initLoader(MESSAGE_LOADER, null, getMessageLoaderCallbacks());
    }

    @Override
    public String getBaseUri() {
        return mBaseUri;
    }

    @Override
    public boolean isViewOnlyMode() {
        return false;
    }

    @Override
    public Uri getAccountUri() {
        return mAccount.uri;
    }

    // End implementations of SecureConversationViewControllerCallbacks

    @Override
    protected void markUnread() {
        super.markUnread();
        // Ignore unsafe calls made after a fragment is detached from an activity
        final ControllableActivity activity = (ControllableActivity) getActivity();
        final ConversationMessage message = mViewController.getMessage();
        if (activity == null || mConversation == null || message == null) {
            LogUtils.w(LOG_TAG, "ignoring markUnread for conv=%s",
                    mConversation != null ? mConversation.id : 0);
            return;
        }
        final HashSet<Uri> uris = new HashSet<Uri>();
        uris.add(message.uri);
        activity.getConversationUpdater().markConversationMessagesUnread(mConversation, uris,
                mViewState.getConversationInfo());
    }

    @Override
    public void onAccountChanged(Account newAccount, Account oldAccount) {
        renderMessage(getMessageCursor());
    }

    @Override
    public void onConversationViewHeaderHeightChange(int newHeight) {
        // Do nothing.
    }

    @Override
    public void onUserVisibleHintChanged() {
        if (mActivity == null) {
            return;
        }
        if (isUserVisible()) {
            onConversationSeen();
        }
    }

    @Override
    protected void onMessageCursorLoadFinished(Loader<ObjectCursor<ConversationMessage>> loader,
            MessageCursor newCursor, MessageCursor oldCursor) {
        renderMessage(newCursor);
    }

    private void renderMessage(MessageCursor newCursor) {
        // ignore cursors that are still loading results
        if (newCursor == null || !newCursor.isLoaded()) {
            LogUtils.i(LOG_TAG, "CONV RENDER: existing cursor is null, rendering from scratch");
            return;
        }
        if (mActivity == null || mActivity.isFinishing()) {
            // Activity is finishing, just bail.
            return;
        }
        if (!newCursor.moveToFirst()) {
            LogUtils.e(LOG_TAG, "unable to open message cursor");
            return;
        }

        mViewController.renderMessage(newCursor.getMessage());
    }

    @Override
    public void onConversationUpdated(Conversation conv) {
        final ConversationViewHeader headerView = mViewController.getConversationHeaderView();
        if (headerView != null) {
            headerView.onConversationUpdated(conv);
            headerView.setSubject(conv.subject);
        }
    }

    // Need this stub here
    @Override
    public boolean supportsMessageTransforms() {
        return false;
    }
}
