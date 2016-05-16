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

package com.android.mail.browse;

import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.android.mail.ContactInfoSource;
import com.android.mail.FormattedDateBuilder;
import com.android.mail.R;
import com.android.mail.browse.ConversationViewHeader.ConversationViewHeaderCallbacks;
import com.android.mail.browse.MessageHeaderView.MessageHeaderViewCallbacks;
import com.android.mail.browse.SuperCollapsedBlock.OnClickListener;
import com.android.mail.providers.Address;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.UIProvider;
import com.android.mail.ui.ControllableActivity;
import com.android.mail.utils.VeiledAddressMatcher;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A specialized adapter that contains overlay views to draw on top of the underlying conversation
 * WebView. Each independently drawn overlay view gets its own item in this adapter, and indices
 * in this adapter do not necessarily line up with cursor indices. For example, an expanded
 * message may have a header and footer, and since they are not drawn coupled together, they each
 * get an adapter item.
 * <p>
 * Each item in this adapter is a {@link ConversationOverlayItem} to expose enough information
 * to {@link ConversationContainer} so that it can position overlays properly.
 *
 */
public class ConversationViewAdapter extends BaseAdapter {

    private Context mContext;
    private final FormattedDateBuilder mDateBuilder;
    private final ConversationAccountController mAccountController;
    private final LoaderManager mLoaderManager;
    private final FragmentManager mFragmentManager;
    private final MessageHeaderViewCallbacks mMessageCallbacks;
    private final ContactInfoSource mContactInfoSource;
    private ConversationViewHeaderCallbacks mConversationCallbacks;
    private OnClickListener mSuperCollapsedListener;
    private Map<String, Address> mAddressCache;
    private final LayoutInflater mInflater;

    private final List<ConversationOverlayItem> mItems;
    private final VeiledAddressMatcher mMatcher;

    public static final int VIEW_TYPE_CONVERSATION_HEADER = 0;
    public static final int VIEW_TYPE_MESSAGE_HEADER = 1;
    public static final int VIEW_TYPE_MESSAGE_FOOTER = 2;
    public static final int VIEW_TYPE_SUPER_COLLAPSED_BLOCK = 3;
    public static final int VIEW_TYPE_BORDER = 4;
    public static final int VIEW_TYPE_AD_HEADER = 5;
    public static final int VIEW_TYPE_AD_SENDER_HEADER = 6;
    public static final int VIEW_TYPE_AD_BORDER = 7;
    public static final int VIEW_TYPE_COUNT = 8;

    public class ConversationHeaderItem extends ConversationOverlayItem {
        public final Conversation mConversation;

        private ConversationHeaderItem(Conversation conv) {
            mConversation = conv;
        }

        @Override
        public int getType() {
            return VIEW_TYPE_CONVERSATION_HEADER;
        }

        @Override
        public View createView(Context context, LayoutInflater inflater, ViewGroup parent) {
            final ConversationViewHeader headerView = (ConversationViewHeader) inflater.inflate(
                    R.layout.conversation_view_header, parent, false);
            headerView.setCallbacks(mConversationCallbacks, mAccountController);
            headerView.bind(this);
            headerView.setSubject(mConversation.subject);
            if (mAccountController.getAccount().supportsCapability(
                    UIProvider.AccountCapabilities.MULTIPLE_FOLDERS_PER_CONV)) {
                headerView.setFolders(mConversation);
            }

            return headerView;
        }

        @Override
        public void bindView(View v, boolean measureOnly) {
            ConversationViewHeader header = (ConversationViewHeader) v;
            header.bind(this);
        }

        @Override
        public boolean isContiguous() {
            return true;
        }

    }

    public static class MessageHeaderItem extends ConversationOverlayItem {

        private final ConversationViewAdapter mAdapter;

        private ConversationMessage mMessage;

        // view state variables
        private boolean mExpanded;
        public boolean detailsExpanded;
        private boolean mShowImages;

        // cached values to speed up re-rendering during view recycling
        private CharSequence mTimestampShort;
        private CharSequence mTimestampLong;
        private long mTimestampMs;
        private FormattedDateBuilder mDateBuilder;
        public CharSequence recipientSummaryText;

        MessageHeaderItem(ConversationViewAdapter adapter, FormattedDateBuilder dateBuilder,
                ConversationMessage message, boolean expanded, boolean showImages) {
            mAdapter = adapter;
            mDateBuilder = dateBuilder;
            mMessage = message;
            mExpanded = expanded;
            mShowImages = showImages;

            detailsExpanded = false;
        }

        public ConversationMessage getMessage() {
            return mMessage;
        }

        @Override
        public int getType() {
            return VIEW_TYPE_MESSAGE_HEADER;
        }

        @Override
        public View createView(Context context, LayoutInflater inflater, ViewGroup parent) {
            final MessageHeaderView v = (MessageHeaderView) inflater.inflate(
                    R.layout.conversation_message_header, parent, false);
            v.initialize(mAdapter.mAccountController,
                    mAdapter.mAddressCache);
            v.setCallbacks(mAdapter.mMessageCallbacks);
            v.setContactInfoSource(mAdapter.mContactInfoSource);
            v.setVeiledMatcher(mAdapter.mMatcher);
            return v;
        }

        @Override
        public void bindView(View v, boolean measureOnly) {
            final MessageHeaderView header = (MessageHeaderView) v;
            header.bind(this, measureOnly);
        }

        @Override
        public void onModelUpdated(View v) {
            final MessageHeaderView header = (MessageHeaderView) v;
            header.refresh();
        }

        @Override
        public boolean isContiguous() {
            return !isExpanded();
        }

        @Override
        public boolean isExpanded() {
            return mExpanded;
        }

        public void setExpanded(boolean expanded) {
            if (mExpanded != expanded) {
                mExpanded = expanded;
            }
        }

        public boolean getShowImages() {
            return mShowImages;
        }

        public void setShowImages(boolean showImages) {
            mShowImages = showImages;
        }

        @Override
        public boolean canBecomeSnapHeader() {
            return isExpanded();
        }

        @Override
        public boolean canPushSnapHeader() {
            return true;
        }

        @Override
        public boolean belongsToMessage(ConversationMessage message) {
            return Objects.equal(mMessage, message);
        }

        @Override
        public void setMessage(ConversationMessage message) {
            mMessage = message;
        }

        public CharSequence getTimestampShort() {
            ensureTimestamps();
            return mTimestampShort;
        }

        public CharSequence getTimestampLong() {
            ensureTimestamps();
            return mTimestampLong;
        }

        private void ensureTimestamps() {
            if (mMessage.dateReceivedMs != mTimestampMs) {
                mTimestampMs = mMessage.dateReceivedMs;
                mTimestampShort = mDateBuilder.formatShortDate(mTimestampMs);
                mTimestampLong = mDateBuilder.formatLongDateTime(mTimestampMs);
            }
        }

        public ConversationViewAdapter getAdapter() {
            return mAdapter;
        }

        @Override
        public void rebindView(View view) {
            final MessageHeaderView header = (MessageHeaderView) view;
            header.rebind(this);
        }
    }

    public class MessageFooterItem extends ConversationOverlayItem {
        /**
         * A footer can only exist if there is a matching header. Requiring a header allows a
         * footer to stay in sync with the expanded state of the header.
         */
        private final MessageHeaderItem mHeaderitem;

        private MessageFooterItem(MessageHeaderItem item) {
            mHeaderitem = item;
        }

        @Override
        public int getType() {
            return VIEW_TYPE_MESSAGE_FOOTER;
        }

        @Override
        public View createView(Context context, LayoutInflater inflater, ViewGroup parent) {
            final MessageFooterView v = (MessageFooterView) inflater.inflate(
                    R.layout.conversation_message_footer, parent, false);
            v.initialize(mLoaderManager, mFragmentManager);
            return v;
        }

        @Override
        public void bindView(View v, boolean measureOnly) {
            final MessageFooterView attachmentsView = (MessageFooterView) v;
            attachmentsView.bind(mHeaderitem, mAccountController.getAccount().uri, measureOnly);
        }

        @Override
        public boolean isContiguous() {
            return true;
        }

        @Override
        public boolean isExpanded() {
            return mHeaderitem.isExpanded();
        }

        @Override
        public int getGravity() {
            // attachments are top-aligned within their spacer area
            // Attachments should stay near the body they belong to, even when zoomed far in.
            return Gravity.TOP;
        }

        @Override
        public int getHeight() {
            // a footer may change height while its view does not exist because it is offscreen
            // (but the header is onscreen and thus collapsible)
            if (!mHeaderitem.isExpanded()) {
                return 0;
            }
            return super.getHeight();
        }
    }

    public class SuperCollapsedBlockItem extends ConversationOverlayItem {

        private final int mStart;
        private int mEnd;

        private SuperCollapsedBlockItem(int start, int end) {
            mStart = start;
            mEnd = end;
        }

        @Override
        public int getType() {
            return VIEW_TYPE_SUPER_COLLAPSED_BLOCK;
        }

        @Override
        public View createView(Context context, LayoutInflater inflater, ViewGroup parent) {
            final SuperCollapsedBlock scb = (SuperCollapsedBlock) inflater.inflate(
                    R.layout.super_collapsed_block, parent, false);
            scb.initialize(mSuperCollapsedListener);
            return scb;
        }

        @Override
        public void bindView(View v, boolean measureOnly) {
            final SuperCollapsedBlock scb = (SuperCollapsedBlock) v;
            scb.bind(this);
        }

        @Override
        public boolean isContiguous() {
            return true;
        }

        @Override
        public boolean isExpanded() {
            return false;
        }

        public int getStart() {
            return mStart;
        }

        public int getEnd() {
            return mEnd;
        }

        @Override
        public boolean canPushSnapHeader() {
            return true;
        }
    }


    public class BorderItem extends ConversationOverlayItem {
        private final boolean mContiguous;
        private boolean mExpanded;
        private final boolean mFirstBorder;
        private boolean mLastBorder;

        public BorderItem(boolean contiguous, boolean isExpanded,
                boolean firstBorder, boolean lastBorder) {
            mContiguous = contiguous;
            mExpanded = isExpanded;
            mFirstBorder = firstBorder;
            mLastBorder = lastBorder;
        }

        @Override
        public int getType() {
            return VIEW_TYPE_BORDER;
        }

        @Override
        public View createView(Context context, LayoutInflater inflater, ViewGroup parent) {
            return inflater.inflate(R.layout.card_border, parent, false);
        }

        @Override
        public void bindView(View v, boolean measureOnly) {
            final BorderView border = (BorderView) v;
            border.bind(this, measureOnly);
        }

        @Override
        public boolean isContiguous() {
            return mContiguous;
        }

        @Override
        public boolean isExpanded() {
            return mExpanded;
        }

        public void setExpanded(boolean isExpanded) {
            mExpanded = isExpanded;
        }

        @Override
        public boolean canPushSnapHeader() {
            return false;
        }

        public boolean isFirstBorder() {
            return mFirstBorder;
        }

        public boolean isLastBorder() {
            return mLastBorder;
        }

        public void setIsLastBorder(boolean isLastBorder) {
            mLastBorder = isLastBorder;
        }

        public ConversationViewAdapter getAdapter() {
            return ConversationViewAdapter.this;
        }

        @Override
        public void rebindView(View view) {
            bindView(view, false);
        }
    }

    public ConversationViewAdapter(ControllableActivity controllableActivity,
            ConversationAccountController accountController,
            LoaderManager loaderManager,
            MessageHeaderViewCallbacks messageCallbacks,
            ContactInfoSource contactInfoSource,
            ConversationViewHeaderCallbacks convCallbacks,
            SuperCollapsedBlock.OnClickListener scbListener, Map<String, Address> addressCache,
            FormattedDateBuilder dateBuilder) {
        mContext = controllableActivity.getActivityContext();
        mDateBuilder = dateBuilder;
        mAccountController = accountController;
        mLoaderManager = loaderManager;
        mFragmentManager = controllableActivity.getFragmentManager();
        mMessageCallbacks = messageCallbacks;
        mContactInfoSource = contactInfoSource;
        mConversationCallbacks = convCallbacks;
        mSuperCollapsedListener = scbListener;
        mAddressCache = addressCache;
        mInflater = LayoutInflater.from(mContext);

        mItems = Lists.newArrayList();
        mMatcher = controllableActivity.getAccountController().getVeiledAddressMatcher();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getType();
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public ConversationOverlayItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position; // TODO: ensure this works well enough
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getView(getItem(position), convertView, parent, false /* measureOnly */);
    }

    public View getView(ConversationOverlayItem item, View convertView, ViewGroup parent,
            boolean measureOnly) {
        final View v;

        if (convertView == null) {
            v = item.createView(mContext, mInflater, parent);
        } else {
            v = convertView;
        }
        item.bindView(v, measureOnly);

        return v;
    }

    public LayoutInflater getLayoutInflater() {
        return mInflater;
    }

    public FormattedDateBuilder getDateBuilder() {
        return mDateBuilder;
    }

    public int addItem(ConversationOverlayItem item) {
        final int pos = mItems.size();
        item.setPosition(pos);
        mItems.add(item);
        return pos;
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public int addConversationHeader(Conversation conv) {
        return addItem(new ConversationHeaderItem(conv));
    }

    public int addMessageHeader(ConversationMessage msg, boolean expanded, boolean showImages) {
        return addItem(new MessageHeaderItem(this, mDateBuilder, msg, expanded, showImages));
    }

    public int addMessageFooter(MessageHeaderItem headerItem) {
        return addItem(new MessageFooterItem(headerItem));
    }

    public static MessageHeaderItem newMessageHeaderItem(ConversationViewAdapter adapter,
            FormattedDateBuilder dateBuilder, ConversationMessage message,
            boolean expanded, boolean showImages) {
        return new MessageHeaderItem(adapter, dateBuilder, message, expanded, showImages);
    }

    public MessageFooterItem newMessageFooterItem(MessageHeaderItem headerItem) {
        return new MessageFooterItem(headerItem);
    }

    public int addSuperCollapsedBlock(int start, int end) {
        return addItem(new SuperCollapsedBlockItem(start, end));
    }

    public int addBorder(
            boolean contiguous, boolean expanded, boolean firstBorder, boolean lastBorder) {
        return addItem(new BorderItem(contiguous, expanded, firstBorder, lastBorder));
    }

    public BorderItem newBorderItem(boolean contiguous, boolean expanded) {
        return new BorderItem(
                contiguous, expanded, false /* firstBorder */, false /* lastBorder */);
    }

    public void replaceSuperCollapsedBlock(SuperCollapsedBlockItem blockToRemove,
            Collection<ConversationOverlayItem> replacements) {
        final int pos = mItems.indexOf(blockToRemove);
        if (pos == -1) {
            return;
        }

        mItems.remove(pos);
        mItems.addAll(pos, replacements);

        // update position for all items
        for (int i = 0, size = mItems.size(); i < size; i++) {
            mItems.get(i).setPosition(i);
        }
    }

    public void updateItemsForMessage(ConversationMessage message,
            List<Integer> affectedPositions) {
        for (int i = 0, len = mItems.size(); i < len; i++) {
            final ConversationOverlayItem item = mItems.get(i);
            if (item.belongsToMessage(message)) {
                item.setMessage(message);
                affectedPositions.add(i);
            }
        }
    }
}
