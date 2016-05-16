/**
 * Copyright (c) 2012, Google Inc.
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

package com.android.mail.providers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.android.mail.R;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.content.CursorCreator;
import com.android.mail.providers.UIProvider.ConversationColumns;
import com.android.mail.ui.ConversationCursorLoader;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Conversation implements Parcelable {
    public static final int NO_POSITION = -1;

    private static final String LOG_TAG = LogTag.getLogTag();

    private static final String EMPTY_STRING = "";

    /**
     * @see BaseColumns#_ID
     */
    public long id;
    /**
     * @see UIProvider.ConversationColumns#URI
     */
    public Uri uri;
    /**
     * @see UIProvider.ConversationColumns#SUBJECT
     */
    public String subject;
    /**
     * @see UIProvider.ConversationColumns#DATE_RECEIVED_MS
     */
    public long dateMs;
    /**
     * @see UIProvider.ConversationColumns#SNIPPET
     */
    @Deprecated
    public String snippet;
    /**
     * @see UIProvider.ConversationColumns#HAS_ATTACHMENTS
     */
    public boolean hasAttachments;
    /**
     * Union of attachmentPreviewUri0 and attachmentPreviewUri1
     */
    public transient ArrayList<String> attachmentPreviews;
    /**
     * @see UIProvider.ConversationColumns#ATTACHMENT_PREVIEW_URI0
     */
    public String attachmentPreviewUri0;
    /**
     * @see UIProvider.ConversationColumns#ATTACHMENT_PREVIEW_URI1
     */
    public String attachmentPreviewUri1;
    /**
     * @see UIProvider.ConversationColumns#ATTACHMENT_PREVIEW_STATES
     */
    public int attachmentPreviewStates;
    /**
     * @see UIProvider.ConversationColumns#ATTACHMENT_PREVIEWS_COUNT
     */
    public int attachmentPreviewsCount;
    /**
     * @see UIProvider.ConversationColumns#MESSAGE_LIST_URI
     */
    public Uri messageListUri;
    /**
     * @see UIProvider.ConversationColumns#SENDER_INFO
     */
    @Deprecated
    public String senders;
    /**
     * @see UIProvider.ConversationColumns#NUM_MESSAGES
     */
    private int numMessages;
    /**
     * @see UIProvider.ConversationColumns#NUM_DRAFTS
     */
    private int numDrafts;
    /**
     * @see UIProvider.ConversationColumns#SENDING_STATE
     */
    public int sendingState;
    /**
     * @see UIProvider.ConversationColumns#PRIORITY
     */
    public int priority;
    /**
     * @see UIProvider.ConversationColumns#READ
     */
    public boolean read;
    /**
     * @see UIProvider.ConversationColumns#SEEN
     */
    public boolean seen;
    /**
     * @see UIProvider.ConversationColumns#STARRED
     */
    public boolean starred;
    /**
     * @see UIProvider.ConversationColumns#RAW_FOLDERS
     */
    private FolderList rawFolders;
    /**
     * @see UIProvider.ConversationColumns#FLAGS
     */
    public int convFlags;
    /**
     * @see UIProvider.ConversationColumns#PERSONAL_LEVEL
     */
    public int personalLevel;
    /**
     * @see UIProvider.ConversationColumns#SPAM
     */
    public boolean spam;
    /**
     * @see UIProvider.ConversationColumns#MUTED
     */
    public boolean muted;
    /**
     * @see UIProvider.ConversationColumns#PHISHING
     */
    public boolean phishing;
    /**
     * @see UIProvider.ConversationColumns#COLOR
     */
    public int color;
    /**
     * @see UIProvider.ConversationColumns#ACCOUNT_URI
     */
    public Uri accountUri;
    /**
     * @see UIProvider.ConversationColumns#CONVERSATION_INFO
     */
    public ConversationInfo conversationInfo;
    /**
     * @see UIProvider.ConversationColumns#CONVERSATION_BASE_URI
     */
    public Uri conversationBaseUri;
    /**
     * @see UIProvider.ConversationColumns#REMOTE
     */
    public boolean isRemote;

    /**
     * Used within the UI to indicate the adapter position of this conversation
     *
     * @deprecated Keeping this in sync with the desired value is a not always done properly, is a
     *             source of bugs, and is a bad idea in general. Do not trust this value. Try to
     *             migrate code away from using it.
     */
    @Deprecated
    public transient int position;
    // Used within the UI to indicate that a Conversation should be removed from
    // the ConversationCursor when executing an update, e.g. the the
    // Conversation is no longer in the ConversationList for the current folder,
    // that is it's now in some other folder(s)
    public transient boolean localDeleteOnUpdate;

    private transient boolean viewed;

    private static String sSubjectAndSnippet;

    // Constituents of convFlags below
    // Flag indicating that the item has been deleted, but will continue being
    // shown in the list Delete/Archive of a mostly-dead item will NOT propagate
    // the delete/archive, but WILL remove the item from the cursor
    public static final int FLAG_MOSTLY_DEAD = 1 << 0;

    /** An immutable, empty conversation list */
    public static final Collection<Conversation> EMPTY = Collections.emptyList();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeParcelable(uri, flags);
        dest.writeString(subject);
        dest.writeLong(dateMs);
        dest.writeString(snippet);
        dest.writeInt(hasAttachments ? 1 : 0);
        dest.writeParcelable(messageListUri, 0);
        dest.writeString(senders);
        dest.writeInt(numMessages);
        dest.writeInt(numDrafts);
        dest.writeInt(sendingState);
        dest.writeInt(priority);
        dest.writeInt(read ? 1 : 0);
        dest.writeInt(seen ? 1 : 0);
        dest.writeInt(starred ? 1 : 0);
        dest.writeParcelable(rawFolders, 0);
        dest.writeInt(convFlags);
        dest.writeInt(personalLevel);
        dest.writeInt(spam ? 1 : 0);
        dest.writeInt(phishing ? 1 : 0);
        dest.writeInt(muted ? 1 : 0);
        dest.writeInt(color);
        dest.writeParcelable(accountUri, 0);
        dest.writeParcelable(conversationInfo, 0);
        dest.writeParcelable(conversationBaseUri, 0);
        dest.writeInt(isRemote ? 1 : 0);
        dest.writeString(attachmentPreviewUri0);
        dest.writeString(attachmentPreviewUri1);
        dest.writeInt(attachmentPreviewStates);
        dest.writeInt(attachmentPreviewsCount);
    }

    private Conversation(Parcel in, ClassLoader loader) {
        id = in.readLong();
        uri = in.readParcelable(null);
        subject = in.readString();
        dateMs = in.readLong();
        snippet = in.readString();
        hasAttachments = (in.readInt() != 0);
        messageListUri = in.readParcelable(null);
        senders = emptyIfNull(in.readString());
        numMessages = in.readInt();
        numDrafts = in.readInt();
        sendingState = in.readInt();
        priority = in.readInt();
        read = (in.readInt() != 0);
        seen = (in.readInt() != 0);
        starred = (in.readInt() != 0);
        rawFolders = in.readParcelable(loader);
        convFlags = in.readInt();
        personalLevel = in.readInt();
        spam = in.readInt() != 0;
        phishing = in.readInt() != 0;
        muted = in.readInt() != 0;
        color = in.readInt();
        accountUri = in.readParcelable(null);
        position = NO_POSITION;
        localDeleteOnUpdate = false;
        conversationInfo = in.readParcelable(loader);
        conversationBaseUri = in.readParcelable(null);
        isRemote = in.readInt() != 0;
        attachmentPreviews = null;
        attachmentPreviewUri0 = in.readString();
        attachmentPreviewUri1 = in.readString();
        attachmentPreviewStates = in.readInt();
        attachmentPreviewsCount = in.readInt();
    }

    @Override
    public String toString() {
        // log extra info at DEBUG level or finer
        final StringBuilder sb = new StringBuilder("[conversation id=");
        sb.append(id);
        if (LogUtils.isLoggable(LOG_TAG, LogUtils.DEBUG)) {
            sb.append(", subject=");
            sb.append(subject);
        }
        sb.append("]");
        return sb.toString();
    }

    public static final ClassLoaderCreator<Conversation> CREATOR =
            new ClassLoaderCreator<Conversation>() {

        @Override
        public Conversation createFromParcel(Parcel source) {
            return new Conversation(source, null);
        }

        @Override
        public Conversation createFromParcel(Parcel source, ClassLoader loader) {
            return new Conversation(source, loader);
        }

        @Override
        public Conversation[] newArray(int size) {
            return new Conversation[size];
        }

    };

    public static final Uri MOVE_CONVERSATIONS_URI = Uri.parse("content://moveconversations");

    /**
     * The column that needs to be updated to change the folders for a conversation.
     */
    public static final String UPDATE_FOLDER_COLUMN = ConversationColumns.RAW_FOLDERS;

    public Conversation(Cursor cursor) {
        if (cursor != null) {
            id = cursor.getLong(UIProvider.CONVERSATION_ID_COLUMN);
            uri = Uri.parse(cursor.getString(UIProvider.CONVERSATION_URI_COLUMN));
            dateMs = cursor.getLong(UIProvider.CONVERSATION_DATE_RECEIVED_MS_COLUMN);
            subject = cursor.getString(UIProvider.CONVERSATION_SUBJECT_COLUMN);
            // Don't allow null subject
            if (subject == null) {
                subject = "";
            }
            hasAttachments = cursor.getInt(UIProvider.CONVERSATION_HAS_ATTACHMENTS_COLUMN) != 0;
            String messageList = cursor.getString(UIProvider.CONVERSATION_MESSAGE_LIST_URI_COLUMN);
            messageListUri = !TextUtils.isEmpty(messageList) ? Uri.parse(messageList) : null;
            sendingState = cursor.getInt(UIProvider.CONVERSATION_SENDING_STATE_COLUMN);
            priority = cursor.getInt(UIProvider.CONVERSATION_PRIORITY_COLUMN);
            read = cursor.getInt(UIProvider.CONVERSATION_READ_COLUMN) != 0;
            seen = cursor.getInt(UIProvider.CONVERSATION_SEEN_COLUMN) != 0;
            starred = cursor.getInt(UIProvider.CONVERSATION_STARRED_COLUMN) != 0;
            rawFolders = readRawFolders(cursor);
            convFlags = cursor.getInt(UIProvider.CONVERSATION_FLAGS_COLUMN);
            personalLevel = cursor.getInt(UIProvider.CONVERSATION_PERSONAL_LEVEL_COLUMN);
            spam = cursor.getInt(UIProvider.CONVERSATION_IS_SPAM_COLUMN) != 0;
            phishing = cursor.getInt(UIProvider.CONVERSATION_IS_PHISHING_COLUMN) != 0;
            muted = cursor.getInt(UIProvider.CONVERSATION_MUTED_COLUMN) != 0;
            color = cursor.getInt(UIProvider.CONVERSATION_COLOR_COLUMN);
            String account = cursor.getString(UIProvider.CONVERSATION_ACCOUNT_URI_COLUMN);
            accountUri = !TextUtils.isEmpty(account) ? Uri.parse(account) : null;
            position = NO_POSITION;
            localDeleteOnUpdate = false;
            conversationInfo = readConversationInfo(cursor);
            final String conversationBase =
                    cursor.getString(UIProvider.CONVERSATION_BASE_URI_COLUMN);
            conversationBaseUri = !TextUtils.isEmpty(conversationBase) ?
                    Uri.parse(conversationBase) : null;
            if (conversationInfo == null) {
                snippet = cursor.getString(UIProvider.CONVERSATION_SNIPPET_COLUMN);
                senders = emptyIfNull(cursor.getString(UIProvider.CONVERSATION_SENDER_INFO_COLUMN));
                numMessages = cursor.getInt(UIProvider.CONVERSATION_NUM_MESSAGES_COLUMN);
                numDrafts = cursor.getInt(UIProvider.CONVERSATION_NUM_DRAFTS_COLUMN);
            }
            isRemote = cursor.getInt(UIProvider.CONVERSATION_REMOTE_COLUMN) != 0;
            attachmentPreviews = null;
            attachmentPreviewUri0 = cursor.getString(
                    UIProvider.CONVERSATION_ATTACHMENT_PREVIEW_URI0_COLUMN);
            attachmentPreviewUri1 = cursor.getString(
                    UIProvider.CONVERSATION_ATTACHMENT_PREVIEW_URI1_COLUMN);
            attachmentPreviewStates = cursor.getInt(
                    UIProvider.CONVERSATION_ATTACHMENT_PREVIEW_STATES_COLUMN);
            attachmentPreviewsCount = cursor.getInt(
                    UIProvider.CONVERSATION_ATTACHMENT_PREVIEWS_COUNT_COLUMN);
        }
    }

    public Conversation(Conversation other) {
        if (other == null) {
            return;
        }

        id = other.id;
        uri = other.uri;
        dateMs = other.dateMs;
        subject = other.subject;
        hasAttachments = other.hasAttachments;
        messageListUri = other.messageListUri;
        sendingState = other.sendingState;
        priority = other.priority;
        read = other.read;
        seen = other.seen;
        starred = other.starred;
        rawFolders = other.rawFolders; // FolderList is immutable, shallow copy is OK
        convFlags = other.convFlags;
        personalLevel = other.personalLevel;
        spam = other.spam;
        phishing = other.phishing;
        muted = other.muted;
        color = other.color;
        accountUri = other.accountUri;
        position = other.position;
        localDeleteOnUpdate = other.localDeleteOnUpdate;
        // although ConversationInfo is mutable (see ConversationInfo.markRead), applyCachedValues
        // will overwrite this if cached changes exist anyway, so a shallow copy is OK
        conversationInfo = other.conversationInfo;
        conversationBaseUri = other.conversationBaseUri;
        snippet = other.snippet;
        senders = other.senders;
        numMessages = other.numMessages;
        numDrafts = other.numDrafts;
        isRemote = other.isRemote;
        attachmentPreviews = null;
        attachmentPreviewUri0 = other.attachmentPreviewUri0;
        attachmentPreviewUri1 = other.attachmentPreviewUri1;
        attachmentPreviewStates = other.attachmentPreviewStates;
        attachmentPreviewsCount = other.attachmentPreviewsCount;
    }

    public Conversation() {
    }

    public static Conversation create(long id, Uri uri, String subject, long dateMs, String snippet,
            boolean hasAttachment, Uri messageListUri, String senders,
            int numMessages, int numDrafts, int sendingState, int priority, boolean read,
            boolean seen, boolean starred, FolderList rawFolders, int convFlags, int personalLevel,
            boolean spam, boolean phishing, boolean muted, Uri accountUri,
            ConversationInfo conversationInfo, Uri conversationBase, boolean isRemote,
            String attachmentPreviewUri0, String attachmentPreviewUri1, int attachmentPreviewStates,
            int attachmentPreviewsCount) {
        final Conversation conversation = new Conversation();

        conversation.id = id;
        conversation.uri = uri;
        conversation.subject = subject;
        conversation.dateMs = dateMs;
        conversation.snippet = snippet;
        conversation.hasAttachments = hasAttachment;
        conversation.messageListUri = messageListUri;
        conversation.senders = emptyIfNull(senders);
        conversation.numMessages = numMessages;
        conversation.numDrafts = numDrafts;
        conversation.sendingState = sendingState;
        conversation.priority = priority;
        conversation.read = read;
        conversation.seen = seen;
        conversation.starred = starred;
        conversation.rawFolders = rawFolders;
        conversation.convFlags = convFlags;
        conversation.personalLevel = personalLevel;
        conversation.spam = spam;
        conversation.phishing = phishing;
        conversation.muted = muted;
        conversation.color = 0;
        conversation.accountUri = accountUri;
        conversation.conversationInfo = conversationInfo;
        conversation.conversationBaseUri = conversationBase;
        conversation.isRemote = isRemote;
        conversation.attachmentPreviews = null;
        conversation.attachmentPreviewUri0 = attachmentPreviewUri0;
        conversation.attachmentPreviewUri1 = attachmentPreviewUri1;
        conversation.attachmentPreviewStates = attachmentPreviewStates;
        conversation.attachmentPreviewsCount = attachmentPreviewsCount;
        return conversation;
    }

    private static final Bundle sConversationInfoRequest = new Bundle(1);
    private static final Bundle sRawFoldersRequest = new Bundle(1);

    private static ConversationInfo readConversationInfo(Cursor cursor) {
        final ConversationInfo ci;

        if (cursor instanceof ConversationCursor) {
            final byte[] blob = ((ConversationCursor) cursor).getCachedBlob(
                    UIProvider.CONVERSATION_INFO_COLUMN);
            if (blob != null && blob.length > 0) {
                return ConversationInfo.fromBlob(blob);
            }
        }

        final String key = UIProvider.ConversationCursorCommand.COMMAND_GET_CONVERSATION_INFO;
        if (sConversationInfoRequest.isEmpty()) {
            sConversationInfoRequest.putBoolean(key, true);
            sConversationInfoRequest.putInt(
                    UIProvider.ConversationCursorCommand.COMMAND_KEY_OPTIONS,
                    UIProvider.ConversationCursorCommand.OPTION_MOVE_POSITION);
        }
        final Bundle response = cursor.respond(sConversationInfoRequest);
        if (response.containsKey(key)) {
            ci = response.getParcelable(key);
        } else {
            // legacy fallback
            ci = ConversationInfo.fromBlob(cursor.getBlob(UIProvider.CONVERSATION_INFO_COLUMN));
        }
        return ci;
    }

    private static FolderList readRawFolders(Cursor cursor) {
        final FolderList fl;

        if (cursor instanceof ConversationCursor) {
            final byte[] blob = ((ConversationCursor) cursor).getCachedBlob(
                    UIProvider.CONVERSATION_RAW_FOLDERS_COLUMN);
            if (blob != null && blob.length > 0) {
                return FolderList.fromBlob(blob);
            }
        }

        final String key = UIProvider.ConversationCursorCommand.COMMAND_GET_RAW_FOLDERS;
        if (sRawFoldersRequest.isEmpty()) {
            sRawFoldersRequest.putBoolean(key, true);
            sRawFoldersRequest.putInt(
                    UIProvider.ConversationCursorCommand.COMMAND_KEY_OPTIONS,
                    UIProvider.ConversationCursorCommand.OPTION_MOVE_POSITION);
        }
        final Bundle response = cursor.respond(sRawFoldersRequest);
        if (response.containsKey(key)) {
            fl = response.getParcelable(key);
        } else {
            // legacy fallback
            // TODO: delete this once Email supports the respond call
            fl = FolderList.fromBlob(
                    cursor.getBlob(UIProvider.CONVERSATION_RAW_FOLDERS_COLUMN));
        }
        return fl;
    }

    /**
     * Apply any column values from the given {@link ContentValues} (where column names are the
     * keys) to this conversation.
     *
     */
    public void applyCachedValues(ContentValues values) {
        if (values == null) {
            return;
        }
        for (String key : values.keySet()) {
            final Object val = values.get(key);
            LogUtils.i(LOG_TAG, "Conversation: applying cached value to col=%s val=%s", key,
                    val);
            if (ConversationColumns.READ.equals(key)) {
                read = (Integer) val != 0;
            } else if (ConversationColumns.CONVERSATION_INFO.equals(key)) {
                conversationInfo = ConversationInfo.fromBlob((byte[]) val);
            } else if (ConversationColumns.FLAGS.equals(key)) {
                convFlags = (Integer) val;
            } else if (ConversationColumns.STARRED.equals(key)) {
                starred = (Integer) val != 0;
            } else if (ConversationColumns.SEEN.equals(key)) {
                seen = (Integer) val != 0;
            } else if (ConversationColumns.RAW_FOLDERS.equals(key)) {
                rawFolders = FolderList.fromBlob((byte[]) val);
            } else if (ConversationColumns.VIEWED.equals(key)) {
                // ignore. this is not read from the cursor, either.
            } else {
                LogUtils.e(LOG_TAG, new UnsupportedOperationException(),
                        "unsupported cached conv value in col=%s", key);
            }
        }
    }

    /**
     * Get the <strong>immutable</strong> list of {@link Folder}s for this conversation. To modify
     * this list, make a new {@link FolderList} and use {@link #setRawFolders(FolderList)}.
     *
     * @return <strong>Immutable</strong> list of {@link Folder}s.
     */
    public List<Folder> getRawFolders() {
        return rawFolders.folders;
    }

    public void setRawFolders(FolderList folders) {
        rawFolders = folders;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Conversation) {
            Conversation conv = (Conversation) o;
            return conv.uri.equals(uri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    /**
     * Get if this conversation is marked as high priority.
     */
    public boolean isImportant() {
        return priority == UIProvider.ConversationPriority.IMPORTANT;
    }

    /**
     * Get if this conversation is mostly dead
     */
    public boolean isMostlyDead() {
        return (convFlags & FLAG_MOSTLY_DEAD) != 0;
    }

    /**
     * Returns true if the URI of the conversation specified as the needle was
     * found in the collection of conversations specified as the haystack. False
     * otherwise. This method is safe to call with null arguments.
     *
     * @param haystack
     * @param needle
     * @return true if the needle was found in the haystack, false otherwise.
     */
    public final static boolean contains(Collection<Conversation> haystack, Conversation needle) {
        // If the haystack is empty, it cannot contain anything.
        if (haystack == null || haystack.size() <= 0) {
            return false;
        }
        // The null folder exists everywhere.
        if (needle == null) {
            return true;
        }
        final long toFind = needle.id;
        for (final Conversation c : haystack) {
            if (toFind == c.id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a collection of a single conversation. This method always returns
     * a valid collection even if the input conversation is null.
     *
     * @param in a conversation, possibly null.
     * @return a collection of the conversation.
     */
    public static Collection<Conversation> listOf(Conversation in) {
        final Collection<Conversation> target = (in == null) ? EMPTY : ImmutableList.of(in);
        return target;
    }

    /**
     * Get the snippet for this conversation. Masks that it may come from
     * conversation info or the original deprecated snippet string.
     */
    public String getSnippet() {
        return conversationInfo != null && !TextUtils.isEmpty(conversationInfo.firstSnippet) ?
                conversationInfo.firstSnippet : snippet;
    }

    /**
     * Get the number of messages for this conversation.
     */
    public int getNumMessages() {
        return conversationInfo != null ? conversationInfo.messageCount : numMessages;
    }

    /**
     * Get the number of drafts for this conversation.
     */
    public int numDrafts() {
        return conversationInfo != null ? conversationInfo.draftCount : numDrafts;
    }

    public boolean isViewed() {
        return viewed;
    }

    public void markViewed() {
        viewed = true;
    }

    public String getBaseUri(String defaultValue) {
        return conversationBaseUri != null ? conversationBaseUri.toString() : defaultValue;
    }

    public ArrayList<String> getAttachmentPreviewUris() {
        if (attachmentPreviews == null) {
            attachmentPreviews = Lists.newArrayListWithCapacity(2);
            if (!TextUtils.isEmpty(attachmentPreviewUri0)) {
                attachmentPreviews.add(attachmentPreviewUri0);
            }
            if (!TextUtils.isEmpty(attachmentPreviewUri1)) {
                attachmentPreviews.add(attachmentPreviewUri1);
            }
        }
        return attachmentPreviews;
    }

    /**
     * Create a human-readable string of all the conversations
     * @param collection Any collection of conversations
     * @return string with a human readable representation of the conversations.
     */
    public static String toString(Collection<Conversation> collection) {
        final StringBuilder out = new StringBuilder(collection.size() + " conversations:");
        int count = 0;
        for (final Conversation c : collection) {
            count++;
            // Indent the conversations to make them easy to read in debug
            // output.
            out.append("      " + count + ": " + c.toString() + "\n");
        }
        return out.toString();
    }

    /**
     * Returns an empty string if the specified string is null
     */
    private static String emptyIfNull(String in) {
        return in != null ? in : EMPTY_STRING;
    }

    /**
     * Get the properly formatted subject and snippet string for display a
     * conversation.
     *
     * @param context
     * @param filteredSubject
     * @param snippet
     */
    public static String getSubjectAndSnippetForDisplay(Context context,
            String filteredSubject, String snippet) {
        if (sSubjectAndSnippet == null) {
            sSubjectAndSnippet = context.getString(R.string.subject_and_snippet);
        }
        if (TextUtils.isEmpty(filteredSubject) && TextUtils.isEmpty(snippet)) {
            return "";
        } else if (TextUtils.isEmpty(filteredSubject)) {
            return snippet;
        } else if (TextUtils.isEmpty(snippet)) {
            return filteredSubject;
        }

        return String.format(sSubjectAndSnippet, filteredSubject, snippet);
    }

    /**
     * Public object that knows how to construct Conversation given Cursors. This is not used by
     * {@link ConversationCursor} or {@link ConversationCursorLoader}.
     */
    public static final CursorCreator<Conversation> FACTORY = new CursorCreator<Conversation>() {
        @Override
        public Conversation createFromCursor(final Cursor c) {
            return new Conversation(c);
        }

        @Override
        public String toString() {
            return "Conversation CursorCreator";
        }
    };
}
