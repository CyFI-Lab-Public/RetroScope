/**
 * Copyright (c) 2011, Google Inc.
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

package com.android.mail.providers.protos.mock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.Html;

import com.android.mail.providers.ConversationInfo;
import com.android.mail.providers.Folder;
import com.android.mail.providers.FolderList;
import com.android.mail.providers.MessageInfo;
import com.android.mail.providers.ReplyFromAccount;
import com.android.mail.providers.UIProvider.AccountCapabilities;
import com.android.mail.providers.UIProvider.AccountColumns;
import com.android.mail.providers.UIProvider.AccountColumns.SettingsColumns;
import com.android.mail.providers.UIProvider.AttachmentColumns;
import com.android.mail.providers.UIProvider.ConversationColumns;
import com.android.mail.providers.UIProvider.ConversationCursorCommand;
import com.android.mail.providers.UIProvider.FolderCapabilities;
import com.android.mail.providers.UIProvider.FolderColumns;
import com.android.mail.providers.UIProvider.MessageColumns;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MockUiProvider extends ContentProvider {

    public static final String AUTHORITY = "com.android.mail.mockprovider";

    private static final int NUM_ACCOUNTS = 5;


    private static final Uri MOCK_ACCOUNTS_URI = Uri.parse("content://" + AUTHORITY + "/accounts");

    // A map of query result for uris
    // TODO(pwestbro) read this map from an external file
    private static Map<String, List<Map<String, Object>>> MOCK_QUERY_RESULTS = Maps.newHashMap();

    private static void initializeAccount(int accountId,
            Map<String, List<Map<String, Object>>> resultMap) {
        final Map<String, Object> inboxfolderDetailsMap =
                createFolderDetailsMap(0, accountId, "zero", true, 0, 2);
        resultMap.put(inboxfolderDetailsMap.get(FolderColumns.URI).toString(),
                ImmutableList.of(inboxfolderDetailsMap));

        final Map<String, Object> accountDetailsMap = createAccountDetailsMap(accountId,
                (String)inboxfolderDetailsMap.get(FolderColumns.URI));
        resultMap.put(((Uri) accountDetailsMap.get(AccountColumns.URI)).toString(),
                ImmutableList.of(accountDetailsMap));

        final Map<String, Object> secondFolderDetailsMap =
                createFolderDetailsMap(2, accountId, "two", 2, 2);
        resultMap.put(secondFolderDetailsMap.get(FolderColumns.URI).toString(),
                ImmutableList.of(secondFolderDetailsMap));

        resultMap.put(
                inboxfolderDetailsMap.get(FolderColumns.CHILD_FOLDERS_LIST_URI).toString(),
                ImmutableList.of(createFolderDetailsMap(10, accountId, "zeroChild0", 0, 0),
                        createFolderDetailsMap(11, accountId, "zeroChild1", 0, 0)));

        final ArrayList<Map<String, Object>> conversations = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < 100; i++) {
            final String name = "zeroConv"+i;
            conversations.add(createConversationDetailsMap(accountId, name.hashCode(),
                    name, 1, 5, i % 2));
        }
        resultMap.put(inboxfolderDetailsMap.get(FolderColumns.CONVERSATION_LIST_URI).toString(),
                conversations);

        final Map<String, Object> message0 =
                createMessageDetailsMap(accountId, "zeroConv0".hashCode(), "zeroConv0", 1, false);
        resultMap.put(message0.get(MessageColumns.URI).toString(), ImmutableList.of(message0));
        resultMap.put(conversations.get(0).get(ConversationColumns.MESSAGE_LIST_URI).toString(),
                ImmutableList.of(message0));
        resultMap.put(message0.get(MessageColumns.ATTACHMENT_LIST_URI).toString(),
                ImmutableList.of(createAttachmentDetailsMap(0, "zero")));
        final Map<String, Object> message1 =
                createMessageDetailsMap(accountId, "zeroConv1".hashCode(), "zeroConv1", 1, false);
        resultMap.put(message1.get(MessageColumns.URI).toString(), ImmutableList.of(message1));
        final Map<String, Object> message1a =
                createMessageDetailsMap(accountId, "zeroConv1a".hashCode(), "zeroConv1a", 2, false);
        resultMap.put(message1a.get(MessageColumns.URI).toString(), ImmutableList.of(message1a));
        resultMap.put(conversations.get(1).get(ConversationColumns.MESSAGE_LIST_URI).toString(),
                ImmutableList.of(message1, message1a));
        resultMap.put(message1.get(MessageColumns.ATTACHMENT_LIST_URI).toString(),
                ImmutableList.of(createAttachmentDetailsMap(1, "one")));

        final Map<String, Object> folderDetailsMap1 =
                createFolderDetailsMap(1, accountId,  "one", 0, 0);
        resultMap.put(folderDetailsMap1.get(FolderColumns.URI).toString(),
                ImmutableList.of(folderDetailsMap1));

        // We currently have two configurations for accounts
        if (accountId % 2 == 0) {
            resultMap.put(accountDetailsMap.get(AccountColumns.FOLDER_LIST_URI).toString(),
                    ImmutableList.of(inboxfolderDetailsMap, folderDetailsMap1));
        } else {
            resultMap.put(secondFolderDetailsMap.get(FolderColumns.URI).toString(),
                    ImmutableList.of(secondFolderDetailsMap));
            final Map<String, Object> folderDetailsMap3 =
                    createFolderDetailsMap(3, accountId, "three", 0, 0);
            resultMap.put(folderDetailsMap3.get(FolderColumns.URI).toString(),
                    ImmutableList.of(folderDetailsMap3));

            resultMap.put(accountDetailsMap.get(AccountColumns.FOLDER_LIST_URI).toString(),
                    ImmutableList.of(secondFolderDetailsMap, folderDetailsMap3));
        }

        final Map<String, Object> conv3 =
                createConversationDetailsMap(accountId, "zeroConv3".hashCode(), "zeroConv3",
                        0, 1, 0);
        resultMap.put(conv3.get(ConversationColumns.URI).toString(),
                ImmutableList.of(conv3));
        final Map<String, Object> conv4 =
                createConversationDetailsMap(accountId, "zeroConv4".hashCode(), "zeroConv4",
                        0, 1, 0);
        resultMap.put(conv4.get(ConversationColumns.URI).toString(),
                ImmutableList.of(conv4));
        resultMap.put(secondFolderDetailsMap.get(FolderColumns.CONVERSATION_LIST_URI).toString(),
                ImmutableList.of(conv3, conv4));

        final Map<String, Object> message2 =
                createMessageDetailsMap(accountId, "zeroConv3".hashCode(), "zeroConv3", 0, true);
        resultMap.put(message2.get(MessageColumns.URI).toString(), ImmutableList.of(message2));
        resultMap.put(conv3.get(ConversationColumns.MESSAGE_LIST_URI).toString(),
                ImmutableList.of(message2));
        final Map<String, Object> message3 =
                createMessageDetailsMap(accountId, "zeroConv4".hashCode(), "zeroConv4", 0, true);
        resultMap.put(message3.get(MessageColumns.URI).toString(), ImmutableList.of(message3));
        resultMap.put(conv4.get(ConversationColumns.MESSAGE_LIST_URI).toString(),
                ImmutableList.of(message3));

        // Add the account to the list of accounts
        List<Map<String, Object>> accountList = resultMap.get(getAccountsUri().toString());
        if (accountList == null) {
            accountList = Lists.newArrayList();
            resultMap.put(getAccountsUri().toString(), accountList);
        }
        accountList.add(accountDetailsMap);
    }

    public static void initializeMockProvider() {
        MOCK_QUERY_RESULTS = Maps.newHashMap();

        for (int accountId = 0; accountId < NUM_ACCOUNTS; accountId++) {
            initializeAccount(accountId, MOCK_QUERY_RESULTS);
        }
    }

    private static Map<String, Object> createConversationDetailsMap(int accountId,
            int conversationId, String subject, int hasAttachments, int messageCount,
            int draftCount) {
        final String conversationUri = getMockConversationUri(accountId, conversationId);
        Map<String, Object> conversationMap = Maps.newHashMap();
        conversationMap.put(BaseColumns._ID, Long.valueOf(conversationId));
        conversationMap.put(ConversationColumns.URI, conversationUri);
        conversationMap.put(ConversationColumns.MESSAGE_LIST_URI, conversationUri + "/getMessages");
        conversationMap.put(ConversationColumns.SUBJECT, "Conversation " + subject);
        conversationMap.put(ConversationColumns.SNIPPET, "snippet");
        conversationMap.put(ConversationColumns.SENDER_INFO,
                "account1@mock.com, account2@mock.com");
        conversationMap.put(ConversationColumns.DATE_RECEIVED_MS, new Date().getTime());
        conversationMap.put(ConversationColumns.HAS_ATTACHMENTS, hasAttachments);
        conversationMap.put(ConversationColumns.NUM_MESSAGES, 1);
        conversationMap.put(ConversationColumns.NUM_DRAFTS, 1);
        conversationMap.put(ConversationColumns.SENDING_STATE, 1);
        conversationMap.put(ConversationColumns.READ, 0);
        conversationMap.put(ConversationColumns.SEEN, 0);
        conversationMap.put(ConversationColumns.STARRED, 0);
        conversationMap.put(ConversationColumns.CONVERSATION_INFO,
                generateConversationInfo(messageCount, draftCount));
        final List<Folder> folders = new ArrayList<Folder>(3);
        for (int i = 0; i < 3; i++) {
            final Folder folder = Folder.newUnsafeInstance();
            folder.name = "folder" + i;
            switch (i) {
                case 0:
                    folder.bgColor = "#fff000";
                    break;
                case 1:
                    folder.bgColor = "#0000FF";
                    break;
                case 2:
                    folder.bgColor = "#FFFF00";
                    break;
                default:
                    folder.bgColor = null;
                    break;
            }

            folders.add(folder);
        }
        final FolderList folderList = FolderList.copyOf(folders);
        conversationMap.put(
                MockRespondMatrixCursor.MOCK_RESPOND_PREFIX +
                        ConversationCursorCommand.COMMAND_GET_RAW_FOLDERS, folderList);
        return conversationMap;
    }

    private static byte[] generateConversationInfo(int messageCount, int draftCount) {
        ConversationInfo info = new ConversationInfo(messageCount, draftCount, "first",
                "firstUnread", "last");
        for (int i = 0; i < messageCount; i++) {
            if (i % 2 == 0) {
                info.addMessage(new MessageInfo(false, false,
                        i + "Test <testsender@test.com>", -1, "testsender@test.com"));
            } else if (i % 3 == 0) {
                info.addMessage(new MessageInfo(true, false, i + "sender@test.com", -1,
                        "sender@test.com"));
            } else {
                info.addMessage(new MessageInfo(false, false, MessageInfo.SENDER_LIST_TOKEN_ELIDED,
                        -1, null));
            }
        }
        return info.toBlob();
    }

    private static Map<String, Object> createMessageDetailsMap(int accountId, int messageId,
            String subject, int hasAttachments, boolean addReplyTo) {
        final String accountUri = getMockAccountUri(accountId);
        final String messageUri = getMockMessageUri(accountId, messageId);
        Map<String, Object> messageMap = Maps.newHashMap();
        messageMap.put(BaseColumns._ID, Long.valueOf(messageId));
        messageMap.put(MessageColumns.URI, messageUri);
        messageMap.put(MessageColumns.SUBJECT, "Message " + subject);
        messageMap.put(MessageColumns.SNIPPET, "SNIPPET");
        String html = "<html><body><b><i>This is some html!!!</i></b></body></html>";
        messageMap.put(MessageColumns.BODY_HTML, html);
        messageMap.put(MessageColumns.BODY_TEXT, Html.fromHtml(html));
        messageMap.put(MessageColumns.HAS_ATTACHMENTS, hasAttachments);
        messageMap.put(MessageColumns.DATE_RECEIVED_MS, new Date().getTime());
        messageMap.put(MessageColumns.ATTACHMENT_LIST_URI, messageUri + "/getAttachments");
        messageMap.put(MessageColumns.TO, "account1@mock.com, account2@mock.com");
        messageMap.put(MessageColumns.FROM, "fromaccount1@mock.com");
        messageMap.put(MessageColumns.MESSAGE_ACCOUNT_URI, accountUri);
        return messageMap;
    }

    private static Map<String, Object> createAttachmentDetailsMap(int attachmentId, String name) {
        Map<String, Object> attachmentMap = Maps.newHashMap();
        attachmentMap.put(BaseColumns._ID, Long.valueOf(attachmentId));
        attachmentMap.put(AttachmentColumns.NAME, "Attachment " + name);
        attachmentMap.put(AttachmentColumns.URI,
                "attachmentUri/" + attachmentMap.get(AttachmentColumns.NAME));
        return attachmentMap;
    }

    private static Map<String, Object> createFolderDetailsMap(int folderId, int accountId,
            String name, int unread, int total) {
        return createFolderDetailsMap(folderId, accountId, name, false, unread, total);
    }

    private static Map<String, Object> createFolderDetailsMap(int folderId, int accountId,
            String name, boolean hasChildren, int unread, int total) {
        final String folderUri = getMockAccountFolderUri(accountId, folderId);

        Map<String, Object> folderMap = Maps.newHashMap();
        folderMap.put(BaseColumns._ID, Long.valueOf(folderId));
        folderMap.put(FolderColumns.URI, folderUri);
        folderMap.put(FolderColumns.NAME, "Folder " + name);
        folderMap.put(FolderColumns.HAS_CHILDREN, new Integer(hasChildren ? 1 : 0));
        folderMap.put(FolderColumns.CONVERSATION_LIST_URI, folderUri + "/getConversations");
        folderMap.put(FolderColumns.CHILD_FOLDERS_LIST_URI, folderUri + "/getChildFolders");
        folderMap.put(FolderColumns.CAPABILITIES,
                Long.valueOf(
                        FolderCapabilities.SYNCABLE |
                        FolderCapabilities.PARENT |
                        FolderCapabilities.CAN_ACCEPT_MOVED_MESSAGES));
        folderMap.put(FolderColumns.UNREAD_COUNT, unread);
        folderMap.put(FolderColumns.TOTAL_COUNT, total);
        folderMap.put(FolderColumns.SYNC_STATUS, 0);
        folderMap.put(FolderColumns.LAST_SYNC_RESULT, 0);
        return folderMap;
    }

    // Temporarily made this public to allow the Gmail accounts to use the mock ui provider uris
    public static Map<String, Object> createAccountDetailsMap(int accountId,String defaultInbox) {
        final String accountUri = getMockAccountUri(accountId);
        Map<String, Object> accountMap = Maps.newHashMap();
        accountMap.put(BaseColumns._ID, Long.valueOf(accountId));
        accountMap.put(AccountColumns.NAME, "account" + accountId + "@mockuiprovider.com");
        accountMap.put(AccountColumns.TYPE, "com.android.mail.providers.protos.mock");
        accountMap.put(AccountColumns.ACCOUNT_MANAGER_NAME,
                "account" + accountId + "@mockuiprovider.com");
        accountMap.put(AccountColumns.PROVIDER_VERSION, Long.valueOf(1));
        accountMap.put(AccountColumns.URI, Uri.parse(accountUri));
        accountMap.put(AccountColumns.CAPABILITIES,
                Integer.valueOf(
                        AccountCapabilities.SYNCABLE_FOLDERS |
                        AccountCapabilities.REPORT_SPAM |
                        AccountCapabilities.ARCHIVE |
                        AccountCapabilities.MUTE |
                        AccountCapabilities.SERVER_SEARCH |
                        AccountCapabilities.FOLDER_SERVER_SEARCH |
                        AccountCapabilities.SANITIZED_HTML |
                        AccountCapabilities.DRAFT_SYNCHRONIZATION |
                        AccountCapabilities.MULTIPLE_FROM_ADDRESSES |
                        AccountCapabilities.SMART_REPLY |
                        AccountCapabilities.LOCAL_SEARCH |
                        AccountCapabilities.THREADED_CONVERSATIONS |
                        AccountCapabilities.MULTIPLE_FOLDERS_PER_CONV));
        JSONArray replyFroms = new JSONArray();
        ArrayList<ReplyFromAccount> list = new ArrayList<ReplyFromAccount>();
        list.add(new ReplyFromAccount(null, Uri.parse(accountUri), "customAddress1@custom.com",
                "customAddress2@custom.com", "Custom1", false, true));
        list.add(new ReplyFromAccount(null, Uri.parse(accountUri), "customAddress2@custom.com",
                "customAddress2@custom.com", "Custom2", false, true));
        for (ReplyFromAccount a : list) {
            replyFroms.put(a.serialize());
        }
        accountMap.put(AccountColumns.ACCOUNT_FROM_ADDRESSES, replyFroms.toString());
        accountMap.put(AccountColumns.FOLDER_LIST_URI, Uri.parse(accountUri + "/folders"));
        accountMap.put(AccountColumns.FULL_FOLDER_LIST_URI, Uri.parse(accountUri + "/folders"));
        accountMap.put(AccountColumns.ALL_FOLDER_LIST_URI, Uri.parse(accountUri + "/folders"));
        accountMap.put(AccountColumns.SEARCH_URI, Uri.parse(accountUri + "/search"));
        accountMap.put(AccountColumns.EXPUNGE_MESSAGE_URI,
                Uri.parse(accountUri + "/expungeMessage"));
        accountMap.put(AccountColumns.UNDO_URI, Uri.parse(accountUri + "/undo"));
        accountMap.put(AccountColumns.SETTINGS_INTENT_URI, Uri.EMPTY);
        accountMap.put(AccountColumns.HELP_INTENT_URI, Uri.EMPTY);
        accountMap.put(AccountColumns.SEND_FEEDBACK_INTENT_URI, Uri.EMPTY);
        accountMap.put(AccountColumns.REAUTHENTICATION_INTENT_URI, Uri.EMPTY);
        accountMap.put(AccountColumns.SYNC_STATUS, 0);
        accountMap.put(AccountColumns.COMPOSE_URI, Uri.parse(accountUri + "/compose"));
        accountMap.put(AccountColumns.RECENT_FOLDER_LIST_URI,
                Uri.parse(accountUri + "/recentFolderListUri"));
        accountMap.put(AccountColumns.MIME_TYPE, "account/mock");
        accountMap.put(AccountColumns.COLOR, 0);
        accountMap.put(AccountColumns.RECENT_FOLDER_LIST_URI, Uri.EMPTY);
        accountMap.put(AccountColumns.DEFAULT_RECENT_FOLDER_LIST_URI, Uri.EMPTY);
        accountMap.put(AccountColumns.MANUAL_SYNC_URI, Uri.EMPTY);
        accountMap.put(AccountColumns.VIEW_INTENT_PROXY_URI, Uri.EMPTY);
        accountMap.put(AccountColumns.ACCOUNT_COOKIE_QUERY_URI, Uri.EMPTY);
        accountMap.put(AccountColumns.UPDATE_SETTINGS_URI, Uri.EMPTY);
        accountMap.put(AccountColumns.ENABLE_MESSAGE_TRANSFORMS, 1);


        // Add settings columns
        accountMap.put(SettingsColumns.SIGNATURE, "");
        accountMap.put(SettingsColumns.AUTO_ADVANCE, 1);
        accountMap.put(SettingsColumns.MESSAGE_TEXT_SIZE, 1);
        accountMap.put(SettingsColumns.SNAP_HEADERS, 1);
        accountMap.put(SettingsColumns.REPLY_BEHAVIOR, 1);
        accountMap.put(SettingsColumns.CONV_LIST_ICON, 1);
        accountMap.put(SettingsColumns.CONV_LIST_ATTACHMENT_PREVIEWS, 1);
        accountMap.put(SettingsColumns.CONFIRM_DELETE, 1);
        accountMap.put(SettingsColumns.CONFIRM_ARCHIVE, 1);
        accountMap.put(SettingsColumns.CONFIRM_SEND, 1);
        accountMap.put(SettingsColumns.DEFAULT_INBOX, defaultInbox);
        accountMap.put(SettingsColumns.DEFAULT_INBOX_NAME, "Inbox");
        accountMap.put(SettingsColumns.FORCE_REPLY_FROM_DEFAULT, 1);
        accountMap.put(SettingsColumns.MAX_ATTACHMENT_SIZE, 25 * 1024 * 1024);
        accountMap.put(SettingsColumns.SWIPE, 1);
        accountMap.put(SettingsColumns.PRIORITY_ARROWS_ENABLED, 1);
        accountMap.put(SettingsColumns.SETUP_INTENT_URI, Uri.EMPTY);
        accountMap.put(SettingsColumns.CONVERSATION_VIEW_MODE, 1);
        accountMap.put(SettingsColumns.VEILED_ADDRESS_PATTERN, null);
        accountMap.put(SettingsColumns.MOVE_TO_INBOX, Uri.EMPTY);
        return accountMap;
    }

    public static String getMockAccountUri(int accountId) {
        return "content://" + AUTHORITY + "/account/" + accountId;
    }

    private static String getMockAccountFolderUri(int accountId, int folderId) {
        return getMockAccountUri(accountId) + "/folder/" + folderId;
    }

    private static String getMockConversationUri(int accountId, int conversationId) {
        return getMockAccountUri(accountId) + "/conversation/" + conversationId;
    }

    private static String getMockMessageUri(int accountId, int messageId) {
        return getMockAccountUri(accountId) + "/message/" + messageId;
    }

    @Override
    public boolean onCreate() {
        MockUiProvider.initializeMockProvider();
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {

        final List<Map<String, Object>> queryResults = MOCK_QUERY_RESULTS.get(url.toString());

        if (queryResults != null && queryResults.size() > 0) {
            // Get the projection.  If there are rows in the result set, pick the first item to
            // generate the projection
            // TODO (pwestbro): handle the case where we want to return an empty result.\
            if (projection == null) {
                Set<String> keys = queryResults.get(0).keySet();
                projection = keys.toArray(new String[keys.size()]);
            }
            final MatrixCursor matrixCursor =
                    new MockRespondMatrixCursor(projection, queryResults.size(), queryResults);

            for (Map<String, Object> queryResult : queryResults) {
                MatrixCursor.RowBuilder rowBuilder = matrixCursor.newRow();

                for (String key : projection) {
                    rowBuilder.add(queryResult.get(key));
                }
            }
            return matrixCursor;
        }

        return null;
    }

    @Override
    public Uri insert(Uri url, ContentValues values) {
        return url;
    }

    @Override
    public int update(Uri url, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri url, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @VisibleForTesting
    static Uri getAccountsUri() {
        return MOCK_ACCOUNTS_URI;
    }
}

