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

import android.content.Context;
import android.graphics.Bitmap;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.CharacterStyle;
import android.util.LruCache;
import android.util.Pair;

import com.android.mail.R;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.MessageInfo;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.FolderUri;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the view model for the conversation header. It includes all the
 * information needed to layout a conversation header view. Each view model is
 * associated with a conversation and is cached to improve the relayout time.
 */
public class ConversationItemViewModel {
    private static final int MAX_CACHE_SIZE = 100;

    int fontColor;
    @VisibleForTesting
    static LruCache<Pair<String, Long>, ConversationItemViewModel> sConversationHeaderMap
        = new LruCache<Pair<String, Long>, ConversationItemViewModel>(MAX_CACHE_SIZE);

    /**
     * The Folder associated with the cache of models.
     */
    private static Folder sCachedModelsFolder;

    // The hashcode used to detect if the conversation has changed.
    private int mDataHashCode;
    private int mLayoutHashCode;

    // Unread
    public boolean unread;

    // Date
    CharSequence dateText;
    public CharSequence dateOverrideText;

    // Personal level
    Bitmap personalLevelBitmap;

    public Bitmap infoIcon;

    // Paperclip
    Bitmap paperclip;

    /** If <code>true</code>, we will not apply any formatting to {@link #sendersText}. */
    public boolean preserveSendersText = false;

    // Senders
    public String sendersText;

    // A list of all the fragments that cover sendersText
    final ArrayList<SenderFragment> senderFragments;

    SpannableStringBuilder sendersDisplayText;
    StaticLayout sendersDisplayLayout;

    boolean hasDraftMessage;

    // Attachment Previews overflow
    String overflowText;

    // View Width
    public int viewWidth;

    // Standard scaled dimen used to detect if the scale of text has changed.
    @Deprecated
    public int standardScaledDimen;

    public long maxMessageId;

    public int gadgetMode;

    public Conversation conversation;

    public ConversationItemView.ConversationItemFolderDisplayer folderDisplayer;

    public boolean hasBeenForwarded;

    public boolean hasBeenRepliedTo;

    public boolean isInvite;

    public ArrayList<SpannableString> styledSenders;

    public SpannableStringBuilder styledSendersString;

    public SpannableStringBuilder messageInfoString;

    public int styledMessageInfoStringOffset;

    private String mContentDescription;

    /**
     * Email address corresponding to the senders that will be displayed in the
     * senders field.
     */
    public ArrayList<String> displayableSenderEmails;

    /**
     * Display names corresponding to the email address corresponding to the
     * senders that will be displayed in the senders field.
     */
    public ArrayList<String> displayableSenderNames;

    /**
     * Returns the view model for a conversation. If the model doesn't exist for this conversation
     * null is returned. Note: this should only be called from the UI thread.
     *
     * @param account the account contains this conversation
     * @param conversationId the Id of this conversation
     * @return the view model for this conversation, or null
     */
    @VisibleForTesting
    static ConversationItemViewModel forConversationIdOrNull(
            String account, long conversationId) {
        final Pair<String, Long> key = new Pair<String, Long>(account, conversationId);
        synchronized(sConversationHeaderMap) {
            return sConversationHeaderMap.get(key);
        }
    }

    static ConversationItemViewModel forConversation(String account, Conversation conv) {
        ConversationItemViewModel header = ConversationItemViewModel.forConversationId(account,
                conv.id);
        header.conversation = conv;
        header.unread = !conv.read;
        header.hasBeenForwarded =
                (conv.convFlags & UIProvider.ConversationFlags.FORWARDED)
                == UIProvider.ConversationFlags.FORWARDED;
        header.hasBeenRepliedTo =
                (conv.convFlags & UIProvider.ConversationFlags.REPLIED)
                == UIProvider.ConversationFlags.REPLIED;
        header.isInvite =
                (conv.convFlags & UIProvider.ConversationFlags.CALENDAR_INVITE)
                == UIProvider.ConversationFlags.CALENDAR_INVITE;
        return header;
    }

    /**
     * Returns the view model for a conversation. If this is the first time
     * call, a new view model will be returned. Note: this should only be called
     * from the UI thread.
     *
     * @param account the account contains this conversation
     * @param conversationId the Id of this conversation
     * @param cursor the cursor to use in populating/ updating the model.
     * @return the view model for this conversation
     */
    static ConversationItemViewModel forConversationId(String account, long conversationId) {
        synchronized(sConversationHeaderMap) {
            ConversationItemViewModel header =
                    forConversationIdOrNull(account, conversationId);
            if (header == null) {
                final Pair<String, Long> key = new Pair<String, Long>(account, conversationId);
                header = new ConversationItemViewModel();
                sConversationHeaderMap.put(key, header);
            }
            return header;
        }
    }

    public ConversationItemViewModel() {
        senderFragments = Lists.newArrayList();
    }

    /**
     * Adds a sender fragment.
     *
     * @param start the start position of this fragment
     * @param end the start position of this fragment
     * @param style the style of this fragment
     * @param isFixed whether this fragment is fixed or not
     */
    void addSenderFragment(int start, int end, CharacterStyle style, boolean isFixed) {
        SenderFragment senderFragment = new SenderFragment(start, end, sendersText, style, isFixed);
        senderFragments.add(senderFragment);
    }

    /**
     * Returns the hashcode to compare if the data in the header is valid.
     */
    private static int getHashCode(CharSequence dateText, Object convInfo,
            List<Folder> rawFolders, boolean starred, boolean read, int priority,
            int sendingState) {
        if (dateText == null) {
            return -1;
        }
        return Objects.hashCode(convInfo, dateText, rawFolders, starred, read, priority,
                sendingState);
    }

    /**
     * Returns the layout hashcode to compare to see if the layout state has changed.
     */
    private int getLayoutHashCode() {
        return Objects.hashCode(mDataHashCode, viewWidth, standardScaledDimen, gadgetMode);
    }

    private Object getConvInfo() {
        return conversation.conversationInfo != null ?
                conversation.conversationInfo : conversation.getSnippet();
    }

    /**
     * Marks this header as having valid data and layout.
     */
    void validate() {
        mDataHashCode = getHashCode(dateText,
                getConvInfo(), conversation.getRawFolders(), conversation.starred,
                conversation.read, conversation.priority, conversation.sendingState);
        mLayoutHashCode = getLayoutHashCode();
    }

    /**
     * Returns if the data in this model is valid.
     */
    boolean isDataValid() {
        return mDataHashCode == getHashCode(dateText,
                getConvInfo(), conversation.getRawFolders(), conversation.starred,
                conversation.read, conversation.priority, conversation.sendingState);
    }

    /**
     * Returns if the layout in this model is valid.
     */
    boolean isLayoutValid() {
        return isDataValid() && mLayoutHashCode == getLayoutHashCode();
    }

    /**
     * Describes the style of a Senders fragment.
     */
    static class SenderFragment {
        // Indices that determine which substring of mSendersText we are
        // displaying.
        int start;
        int end;

        // The style to apply to the TextPaint object.
        CharacterStyle style;

        // Width of the fragment.
        int width;

        // Ellipsized text.
        String ellipsizedText;

        // Whether the fragment is fixed or not.
        boolean isFixed;

        // Should the fragment be displayed or not.
        boolean shouldDisplay;

        SenderFragment(int start, int end, CharSequence sendersText, CharacterStyle style,
                boolean isFixed) {
            this.start = start;
            this.end = end;
            this.style = style;
            this.isFixed = isFixed;
        }
    }


    /**
     * Reset the content description; enough content has changed that we need to
     * regenerate it.
     */
    public void resetContentDescription() {
        mContentDescription = null;
    }

    /**
     * Get conversation information to use for accessibility.
     */
    public CharSequence getContentDescription(Context context) {
        if (mContentDescription == null) {
            // If any are unread, get the first unread sender.
            // If all are unread, get the first sender.
            // If all are read, get the last sender.
            String sender = "";
            if (conversation.conversationInfo != null) {
                String lastSender = "";
                int last = conversation.conversationInfo.messageInfos != null ?
                        conversation.conversationInfo.messageInfos.size() - 1 : -1;
                if (last != -1) {
                    lastSender = conversation.conversationInfo.messageInfos.get(last).sender;
                }
                if (conversation.read) {
                    sender = TextUtils.isEmpty(lastSender) ?
                            SendersView.getMe(context) : lastSender;
                } else {
                    MessageInfo firstUnread = null;
                    for (MessageInfo m : conversation.conversationInfo.messageInfos) {
                        if (!m.read) {
                            firstUnread = m;
                            break;
                        }
                    }
                    if (firstUnread != null) {
                        sender = TextUtils.isEmpty(firstUnread.sender) ?
                                SendersView.getMe(context) : firstUnread.sender;
                    }
                }
                if (TextUtils.isEmpty(sender)) {
                    // Just take the last sender
                    sender = lastSender;
                }
            }
            boolean isToday = DateUtils.isToday(conversation.dateMs);
            String date = DateUtils.getRelativeTimeSpanString(context, conversation.dateMs)
                    .toString();
            String readString = context.getString(
                    conversation.read ? R.string.read_string : R.string.unread_string);
            int res = isToday ? R.string.content_description_today : R.string.content_description;
            mContentDescription = context.getString(res, sender,
                    conversation.subject, conversation.getSnippet(), date, readString);
        }
        return mContentDescription;
    }

    /**
     * Clear cached header model objects when accessibility changes.
     */

    public static void onAccessibilityUpdated() {
        sConversationHeaderMap.evictAll();
    }

    /**
     * Clear cached header model objects when the folder changes.
     */
    public static void onFolderUpdated(Folder folder) {
        final FolderUri old = sCachedModelsFolder != null
                ? sCachedModelsFolder.folderUri : FolderUri.EMPTY;
        final FolderUri newUri = folder != null ? folder.folderUri : FolderUri.EMPTY;
        if (!old.equals(newUri)) {
            sCachedModelsFolder = folder;
            sConversationHeaderMap.evictAll();
        }
    }
}