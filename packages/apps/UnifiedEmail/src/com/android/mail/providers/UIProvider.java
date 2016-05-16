/*******************************************************************************
 *      Copyright (C) 2011 Google Inc.
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

package com.android.mail.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.regex.Pattern;

public class UIProvider {
    public static final String EMAIL_SEPARATOR = ",";
    public static final long INVALID_CONVERSATION_ID = -1;
    public static final long INVALID_MESSAGE_ID = -1;

    /**
     * Values for the current state of a Folder/Account; note that it's possible that more than one
     * sync is in progress
     */
    public static final class SyncStatus {
        /**
         * No sync in progress
         */
        public static final int NO_SYNC = 0;
        /**
         * A user-requested sync/refresh is in progress. This occurs when the user taps on the
         * refresh icon in the action bar.
         */
        public static final int USER_REFRESH = 1<<0;
        /**
         * A user-requested live query is in progress. This occurs when the user goes past the end
         * of the fetched results in the conversation list.
         */
        public static final int LIVE_QUERY = 1<<1;
        /** Please use the constant {@link #LIVE_QUERY} instead. */
        @Deprecated
        public static final int USER_QUERY = 1<<1;
        /**
         * A background sync is in progress. This happens on <b>no</b> user interaction.
         */
        public static final int BACKGROUND_SYNC = 1<<2;
        /**
         * An initial sync is needed for this Account/Folder to be used. This is account-wide, when
         * the user has added an account, and the first sync has not completed successfully.
         */
        public static final int INITIAL_SYNC_NEEDED = 1<<3;
        /**
         * Manual sync is required. This is account-wide, when the user has disabled sync on the
         * Gmail account.
         */
        public static final int MANUAL_SYNC_REQUIRED = 1<<4;
        /**
         * Account initialization is required.
         */
        public static final int ACCOUNT_INITIALIZATION_REQUIRED = 1<<5;

        public static boolean isSyncInProgress(int syncStatus) {
            return 0 != (syncStatus & (BACKGROUND_SYNC |
                    USER_REFRESH |
                    LIVE_QUERY));
        }
    }

    /**
     * Values for the result of the last attempted sync of a Folder/Account
     */
    public static final class LastSyncResult {
        /** The sync completed successfully */
        public static final int SUCCESS = 0;
        /** The sync wasn't completed due to a connection error */
        public static final int CONNECTION_ERROR = 1;
        /** The sync wasn't completed due to an authentication error */
        public static final int AUTH_ERROR = 2;
        /** The sync wasn't completed due to a security error */
        public static final int SECURITY_ERROR = 3;
        /** The sync wasn't completed due to a low memory condition */
        public static final int STORAGE_ERROR = 4;
        /** The sync wasn't completed due to an internal error/exception */
        public static final int INTERNAL_ERROR = 5;
    }

    // The actual content provider should define its own authority
    public static final String AUTHORITY = "com.android.mail.providers";

    public static final String ACCOUNT_LIST_TYPE =
            "vnd.android.cursor.dir/vnd.com.android.mail.account";
    public static final String ACCOUNT_TYPE =
            "vnd.android.cursor.item/vnd.com.android.mail.account";

    /**
     * Query parameter key that can be used to control the behavior of list queries.  The value
     * must be a serialized {@link ListParams} object.  UIProvider implementations are not
     * required to respect this query parameter
     */
    public static final String LIST_PARAMS_QUERY_PARAMETER = "listParams";
    public static final String LABEL_QUERY_PARAMETER = "label";
    public static final String SEEN_QUERY_PARAMETER = "seen";

    /**
     * Query parameter that can be used to specify a parent for a the returned folder object from a
     * query. When set, if a folder is returned that does not have a true parent, it will use this
     * uri as its parent uri.
     */
    public static final String DEFAULT_PARENT_QUERY_PARAMETER = "defaultParent";

    public static final Map<String, Class<?>> ACCOUNTS_COLUMNS_NO_CAPABILITIES =
            new ImmutableMap.Builder<String, Class<?>>()
            .put(AccountColumns._ID, Integer.class)
            .put(AccountColumns.NAME, String.class)
            .put(AccountColumns.SENDER_NAME, String.class)
            .put(AccountColumns.ACCOUNT_MANAGER_NAME, String.class)
            .put(AccountColumns.TYPE, String.class)
            .put(AccountColumns.PROVIDER_VERSION, Integer.class)
            .put(AccountColumns.URI, String.class)
            .put(AccountColumns.FOLDER_LIST_URI, String.class)
            .put(AccountColumns.FULL_FOLDER_LIST_URI, String.class)
            .put(AccountColumns.ALL_FOLDER_LIST_URI, String.class)
            .put(AccountColumns.SEARCH_URI, String.class)
            .put(AccountColumns.ACCOUNT_FROM_ADDRESSES, String.class)
            .put(AccountColumns.EXPUNGE_MESSAGE_URI, String.class)
            .put(AccountColumns.UNDO_URI, String.class)
            .put(AccountColumns.SETTINGS_INTENT_URI, String.class)
            .put(AccountColumns.SYNC_STATUS, Integer.class)
            .put(AccountColumns.HELP_INTENT_URI, String.class)
            .put(AccountColumns.SEND_FEEDBACK_INTENT_URI, String.class)
            .put(AccountColumns.REAUTHENTICATION_INTENT_URI, String.class)
            .put(AccountColumns.COMPOSE_URI, String.class)
            .put(AccountColumns.MIME_TYPE, String.class)
            .put(AccountColumns.RECENT_FOLDER_LIST_URI, String.class)
            .put(AccountColumns.COLOR, Integer.class)
            .put(AccountColumns.DEFAULT_RECENT_FOLDER_LIST_URI, String.class)
            .put(AccountColumns.MANUAL_SYNC_URI, String.class)
            .put(AccountColumns.VIEW_INTENT_PROXY_URI, String.class)
            .put(AccountColumns.ACCOUNT_COOKIE_QUERY_URI, String.class)
            .put(AccountColumns.SettingsColumns.SIGNATURE, String.class)
            .put(AccountColumns.SettingsColumns.AUTO_ADVANCE, Integer.class)
            .put(AccountColumns.SettingsColumns.MESSAGE_TEXT_SIZE, Integer.class)
            .put(AccountColumns.SettingsColumns.SNAP_HEADERS, Integer.class)
            .put(AccountColumns.SettingsColumns.REPLY_BEHAVIOR, Integer.class)
            .put(AccountColumns.SettingsColumns.CONV_LIST_ICON, Integer.class)
            .put(AccountColumns.SettingsColumns.CONV_LIST_ATTACHMENT_PREVIEWS, Integer.class)
            .put(AccountColumns.SettingsColumns.CONFIRM_DELETE, Integer.class)
            .put(AccountColumns.SettingsColumns.CONFIRM_ARCHIVE, Integer.class)
            .put(AccountColumns.SettingsColumns.CONFIRM_SEND, Integer.class)
            .put(AccountColumns.SettingsColumns.DEFAULT_INBOX, String.class)
            .put(AccountColumns.SettingsColumns.DEFAULT_INBOX_NAME, String.class)
            .put(AccountColumns.SettingsColumns.FORCE_REPLY_FROM_DEFAULT, Integer.class)
            .put(AccountColumns.SettingsColumns.MAX_ATTACHMENT_SIZE, Integer.class)
            .put(AccountColumns.SettingsColumns.SWIPE, Integer.class)
            .put(AccountColumns.SettingsColumns.PRIORITY_ARROWS_ENABLED, Integer.class)
            .put(AccountColumns.SettingsColumns.SETUP_INTENT_URI, String.class)
            .put(AccountColumns.SettingsColumns.CONVERSATION_VIEW_MODE, Integer.class)
            .put(AccountColumns.SettingsColumns.VEILED_ADDRESS_PATTERN, String.class)
            .put(AccountColumns.UPDATE_SETTINGS_URI, String.class)
            .put(AccountColumns.ENABLE_MESSAGE_TRANSFORMS, Integer.class)
            .put(AccountColumns.SYNC_AUTHORITY, String.class)
            .put(AccountColumns.QUICK_RESPONSE_URI, String.class)
            .put(AccountColumns.SettingsColumns.MOVE_TO_INBOX, String.class)
            .build();

    public static final Map<String, Class<?>> ACCOUNTS_COLUMNS =
            new ImmutableMap.Builder<String, Class<?>>()
            .putAll(ACCOUNTS_COLUMNS_NO_CAPABILITIES)
            .put(AccountColumns.CAPABILITIES, Integer.class)
            .build();

    // pull out the keyset from above to form the projection
    public static final String[] ACCOUNTS_PROJECTION =
            ACCOUNTS_COLUMNS.keySet().toArray(new String[ACCOUNTS_COLUMNS.size()]);

    public static final
            String[] ACCOUNTS_PROJECTION_NO_CAPABILITIES = ACCOUNTS_COLUMNS_NO_CAPABILITIES.keySet()
                    .toArray(new String[ACCOUNTS_COLUMNS_NO_CAPABILITIES.size()]);

    public static final class AccountCapabilities {
        /**
         * Whether folders can be synchronized back to the server.
         */
        public static final int SYNCABLE_FOLDERS = 0x0001;
        /**
         * Whether the server allows reporting spam back.
         */
        public static final int REPORT_SPAM = 0x0002;
        /**
         * Whether the server allows reporting phishing back.
         */
        public static final int REPORT_PHISHING = 0x0004;
        /**
         * Whether the server supports a concept of Archive: removing mail from the Inbox but
         * keeping it around.
         */
        public static final int ARCHIVE = 0x0008;
        /**
         * Whether the server will stop notifying on updates to this thread? This requires
         * THREADED_CONVERSATIONS to be true, otherwise it should be ignored.
         */
        public static final int MUTE = 0x0010;
        /**
         * Whether the server supports searching over all messages. This requires SYNCABLE_FOLDERS
         * to be true, otherwise it should be ignored.
         */
        public static final int SERVER_SEARCH = 0x0020;
        /**
         * Whether the server supports constraining search to a single folder. Requires
         * SYNCABLE_FOLDERS, otherwise it should be ignored.
         */
        public static final int FOLDER_SERVER_SEARCH = 0x0040;
        /**
         * Whether the server sends us sanitized HTML (guaranteed to not contain malicious HTML).
         */
        public static final int SANITIZED_HTML = 0x0080;
        /**
         * Whether the server allows synchronization of draft messages. This does NOT require
         * SYNCABLE_FOLDERS to be set.
         */
        public static final int DRAFT_SYNCHRONIZATION = 0x0100;
        /**
         * Does the server allow the user to compose mails (and reply) using addresses other than
         * their account name? For instance, GMail allows users to set FROM addresses that are
         * different from account@gmail.com address. For instance, user@gmail.com could have another
         * FROM: address like user@android.com. If the user has enabled multiple FROM address, he
         * can compose (and reply) using either address.
         */
        public static final int MULTIPLE_FROM_ADDRESSES = 0x0200;
        /**
         * Whether the server allows the original message to be included in the reply by setting a
         * flag on the reply. If we can avoid including the entire previous message, we save on
         * bandwidth (replies are shorter).
         */
        public static final int SMART_REPLY = 0x0400;
        /**
         * Does this account support searching locally, on the device? This requires the backend
         * storage to support a mechanism for searching.
         */
        public static final int LOCAL_SEARCH = 0x0800;
        /**
         * Whether the server supports a notion of threaded conversations: where replies to messages
         * are tagged to keep conversations grouped. This could be full threading (each message
         * lists its parent) or conversation-level threading (each message lists one conversation
         * which it belongs to)
         */
        public static final int THREADED_CONVERSATIONS = 0x1000;
        /**
         * Whether the server supports allowing a conversation to be in multiple folders. (Or allows
         * multiple folders on a single conversation)
         */
        public static final int MULTIPLE_FOLDERS_PER_CONV = 0x2000;
        /**
         * Whether the provider supports undoing operations. If it doesn't, never show the undo bar.
         */
        public static final int UNDO = 0x4000;
        /**
         * Whether the account provides help content.
         */
        public static final int HELP_CONTENT = 0x8000;
        /**
         * Whether the account provides a way to send feedback content.
         */
        public static final int SEND_FEEDBACK = 0x10000;
        /**
         * Whether the account provides a mechanism for marking conversations as important.
         */
        public static final int MARK_IMPORTANT = 0x20000;
        /**
         * Whether initial conversation queries should use a limit parameter
         */
        public static final int INITIAL_CONVERSATION_LIMIT = 0x40000;
        /**
         * Whether the account cannot be used for sending
         */
        public static final int SENDING_UNAVAILABLE = 0x80000;
        /**
         * Whether the account supports discarding drafts from a conversation.  This should be
         * removed when all providers support this capability
         */
        public static final int DISCARD_CONVERSATION_DRAFTS = 0x100000;
        /**
         * Whether the account supports emptying the trash folder
         */
        public static final int EMPTY_TRASH = 0x200000;
        /**
         * Whether the account supports emptying the spam folder
         */
        public static final int EMPTY_SPAM = 0x400000;
        /**
         * Whether the account supports nested folders
         */
        public static final int NESTED_FOLDERS = 0x800000;
    }

    public static final class AccountColumns implements BaseColumns {
        /**
         * This string column contains the human visible name for the account.
         */
        public static final String NAME = "name";

        /**
         * This string column contains the real name associated with the account, e.g. "John Doe"
         */
        public static final String SENDER_NAME = "senderName";

        /**
         * This string column contains the account manager name of this account.
         */

        public static final String ACCOUNT_MANAGER_NAME = "accountManagerName";

        /**
         * This integer contains the type of the account: Google versus non google. This is not
         * returned by the UIProvider, rather this is a notion in the system.
         */
        public static final String TYPE = "type";

        /**
         * This integer column returns the version of the UI provider schema from which this
         * account provider will return results.
         */
        public static final String PROVIDER_VERSION = "providerVersion";

        /**
         * This string column contains the uri to directly access the information for this account.
         */
        public static final String URI = "accountUri";

        /**
         * This integer column contains a bit field of the possible capabilities that this account
         * supports.
         */
        public static final String CAPABILITIES = "capabilities";

        /**
         * This string column contains the content provider uri to return the
         * list of top level folders for this account.
         */
        public static final String FOLDER_LIST_URI = "folderListUri";

        /**
         * This string column contains the content provider uri to return the
         * list of all real folders for this account.
         */
        public static final String FULL_FOLDER_LIST_URI = "fullFolderListUri";

        /**
         * This string column contains the content provider uri to return the
         * list of all real and synthetic folders for this account.
         */
        public static final String ALL_FOLDER_LIST_URI = "allFolderListUri";

        /**
         * This string column contains the content provider uri that can be queried for search
         * results.
         * The supported query parameters are limited to those listed
         * in {@link SearchQueryParameters}
         * The cursor returned from this query is expected have one row, where the columnm are a
         * subset of the columns specified in {@link FolderColumns}
         */
        public static final String SEARCH_URI = "searchUri";

        /**
         * This string column contains a json array of json objects representing
         * custom from addresses for this account or null if there are none.
         */
        public static final String ACCOUNT_FROM_ADDRESSES = "accountFromAddresses";

        /**
         * This string column contains the content provider uri that can be used
         * to expunge a message from this account. NOTE: This might be better to
         * be an update operation on the messageUri.
         * When {@link android.content.ContentResolver#update(Uri, ContentValues, String, String[])}
         * is called with this uri, the {@link ContentValues} object is expected to have
         * {@link BaseColumns#_ID} specified with the local message id of the message.
         */
        public static final String EXPUNGE_MESSAGE_URI = "expungeMessageUri";

        /**
         * This string column contains the content provider uri that can be used
         * to undo the last committed action.
         */
        public static final String UNDO_URI = "undoUri";

        /**
         * Uri for EDIT intent that will cause the settings screens for this account type to be
         * shown.
         * Optionally, extra values from {@link EditSettingsExtras} can be used to indicate
         * which settings the user wants to edit.
         * TODO: When we want to support a heterogeneous set of account types, this value may need
         * to be moved to a global content provider.
         */
        public static final String SETTINGS_INTENT_URI = "accountSettingsIntentUri";

        /**
         * Uri for VIEW intent that will cause the help screens for this account type to be
         * shown.
         * TODO: When we want to support a heterogeneous set of account types, this value may need
         * to be moved to a global content provider.
         */
        public static final String HELP_INTENT_URI = "helpIntentUri";

        /**
         * Uri for VIEW intent that will cause the send feedback for this account type to be
         * shown.
         * TODO: When we want to support a heterogeneous set of account types, this value may need
         * to be moved to a global content provider.
         */
        public static final String SEND_FEEDBACK_INTENT_URI = "sendFeedbackIntentUri";

        /**
         * Uri for VIEW intent that will cause the user to be prompted for authentication for
         * this account.  startActivityForResult() will be called with this intent. Activities that
         * handle this intent are expected to return {@link android.app.Activity#RESULT_OK} if the
         * user successfully authenticated.
         */
        public static final String REAUTHENTICATION_INTENT_URI = "reauthenticationUri";

        /**
         * This int column contains the current sync status of the account (the logical AND of the
         * sync status of folders in this account)
         */
        public static final String SYNC_STATUS = "syncStatus";
        /**
         * Uri for VIEW intent that will cause the compose screens for this type
         * of account to be shown.
         */
        public static final String COMPOSE_URI = "composeUri";
        /**
         * Mime-type defining this account.
         */
        public static final String MIME_TYPE = "mimeType";
        /**
         * URI for location of recent folders viewed on this account.
         */
        public static final String RECENT_FOLDER_LIST_URI = "recentFolderListUri";
        /**
         * URI for default recent folders for this account, if any.
         */
        public static final String DEFAULT_RECENT_FOLDER_LIST_URI = "defaultRecentFolderListUri";
        /**
         * Color (integer) used for this account (for Email/Combined view)
         */
        public static final String COLOR = "color";
        /**
         * URI for forcing a manual sync of this account.
         */
        public static final String MANUAL_SYNC_URI = "manualSyncUri";
        /**
         * Optional URI of this account for proxying view intents.
         */
        public static final String VIEW_INTENT_PROXY_URI = "viewProxyUri";
        /**
         * Optional URI for querying for the cookie needed for accessing inline content.  The cookie
         * specified here will be set on the uri specified in the
         * {@link ConversationColumns#CONVERSATION_BASE_URI} column. The cursor returned from this
         * query is expected have one row, where the columns are specified in
         * {@link AccountCookieColumns}
         */
        public static final String ACCOUNT_COOKIE_QUERY_URI = "accountCookieUri";
        /**
         * URI to be used with an update() ContentResolver call with a {@link ContentValues} object
         * where the keys are from the {@link AccountColumns.SettingsColumns}, and the values are
         * the new values.
         */
        public static final String UPDATE_SETTINGS_URI = "updateSettingsUri";
        /**
         * Whether message transforms (HTML DOM manipulation) should be enabled.
         */
        public static final String ENABLE_MESSAGE_TRANSFORMS = "enableMessageTransforms";
        /**
         * Sync authority to use.
         */
        public static final String SYNC_AUTHORITY = "syncAuthority";
        /**
         * URI for querying this account's quick responses
         */
        public static final String QUICK_RESPONSE_URI = "quickResponseUri";

        public static final class SettingsColumns {
            /**
             * String column containing the contents of the signature for this account.  If no
             * signature has been specified, the value will be null.
             */
            public static final String SIGNATURE = "signature";

            /**
             * Integer column containing the user's specified auto-advance policy.  This value will
             * be one of the values in {@link UIProvider.AutoAdvance}
             */
            public static final String AUTO_ADVANCE = "auto_advance";

            /**
             * Integer column containing the user's specified message text size preference.  This
             * value will be one of the values in {@link UIProvider.MessageTextSize}
             */
            public static final String MESSAGE_TEXT_SIZE = "message_text_size";

            /**
             * Integer column contaning the user's specified snap header preference.  This value
             * will be one of the values in {@link UIProvider.SnapHeaderValue}
             */
            public static final String SNAP_HEADERS = "snap_headers";

            /**
             * Integer column containing the user's specified default reply behavior.  This value
             * will be one of the values in {@link UIProvider.DefaultReplyBehavior}
             */
            public static final String REPLY_BEHAVIOR = "reply_behavior";

            /**
             * Integer column containing the user's preference for whether to show sender images
             * or not in the conversation list view.  This value will be one of the values in
             * {@link UIProvider.ConversationListIcon}.
             */
            public static final String CONV_LIST_ICON = "conversation_list_icon";

            /**
             * Integer column containing the user's preference for whether to show attachment
             * previews or not in the conversation list view. A non zero value indicates that
             * attachment previews should be displayed.
             */
            public static final String CONV_LIST_ATTACHMENT_PREVIEWS
                    = "conversation_list_attachment_previews";

            /**
             * Integer column containing the user's specified confirm delete preference value.
             * A non zero value indicates that the user has indicated that a confirmation should
             * be shown when a delete action is performed.
             */
            public static final String CONFIRM_DELETE = "confirm_delete";

            /**
             * Integer column containing the user's specified confirm archive preference value.
             * A non zero value indicates that the user has indicated that a confirmation should
             * be shown when an archive action is performed.
             */
            public static final String CONFIRM_ARCHIVE = "confirm_archive";

            /**
             * Integer column containing the user's specified confirm send preference value.
             * A non zero value indicates that the user has indicated that a confirmation should
             * be shown when a send action is performed.
             */
            public static final String CONFIRM_SEND = "confirm_send";
            /**
             * String containing the URI for the default inbox for this account.
             */
            public static final String DEFAULT_INBOX = "default_inbox";
            /**
             * String containing the name of the default Inbox for this account
             */
            public static final String DEFAULT_INBOX_NAME = "default_inbox_name";
            /**
             * Integer column containing a non zero value if replies should always be sent from
             * a default address instead of a recipient.
             */
            public static final String FORCE_REPLY_FROM_DEFAULT = "force_reply_from_default";
            /**
             * Integer column containing the max attachment size in kb.
             */
            public static final String MAX_ATTACHMENT_SIZE = "max_attachment_size";
            /**
             * Integer column containing a value matching one of the constants from {@link Swipe}
             */
            public static final String SWIPE = "swipe";
            /**
             * Integer column containing whether priority inbox arrows are enabled.
             */
            public static final String PRIORITY_ARROWS_ENABLED = "priority_inbox_arrows_enabled";
            /**
             * Uri for EDIT intent that will cause account-specific setup UI to be shown. If not
             * null, this intent should be used when an account is "entered" (i.e. viewing a folder
             * in the account, etc.)
             */
            public static final String SETUP_INTENT_URI = "setup_intent_uri";
            /**
             * The regex that defines a veiled address, something that must be hidden from user
             * view because it is temporary, long and clumsy.
             */
            public static final String VEILED_ADDRESS_PATTERN = "veiled_address_pattern";
            /**
             * Integer column containing the Conversation view mode.  This value will match one of
             * constants from  {@link ConversationViewMode}
             */
            public static final String CONVERSATION_VIEW_MODE = "conversation_view_mode";
            /**
             * String containing the URI for the inbox conversations should be moved to for this
             * account.
             */
            public static final String MOVE_TO_INBOX = "move_to_inbox";
        }
    }

    public static final String[] QUICK_RESPONSE_PROJECTION = {
        BaseColumns._ID,
        QuickResponseColumns.TEXT,
        QuickResponseColumns.URI
    };

    public static final class QuickResponseColumns {
        /**
         * Text of the Quick Response
         */
        public static final String TEXT = "quickResponse";
        /**
         * URI to access this row directly
         */
        public static final String URI = "uri";
    }

    public static final String[] ACCOUNT_COOKIE_PROJECTION = {
        AccountCookieColumns.COOKIE
    };

    public static final class AccountCookieColumns {
        /**
         * String column containing the cookie string for this account.
         */
        public static final String COOKIE = "cookie";
    }

    public static final class SearchQueryParameters {
        /**
         * Parameter used to specify the search query.
         */
        public static final String QUERY = "query";

        private SearchQueryParameters() {}
    }

    public static final class ConversationListQueryParameters {
        public static final String DEFAULT_LIMIT = "50";
        /**
         * Parameter used to limit the number of rows returned by a conversation list query
         */
        public static final String LIMIT = "limit";

        /**
         * Parameter used to control whether the this query a remote server.
         */
        public static final String USE_NETWORK = "use_network";

        /**
         * Parameter used to allow the caller to indicate desire to receive all notifications.
         * (Including ones for user initiated actions)
         */
        public static final String ALL_NOTIFICATIONS = "all_notifications";

        private ConversationListQueryParameters() {}
    }

    // We define a "folder" as anything that contains a list of conversations.
    public static final String FOLDER_LIST_TYPE =
            "vnd.android.cursor.dir/vnd.com.android.mail.folder";
    public static final String FOLDER_TYPE =
            "vnd.android.cursor.item/vnd.com.android.mail.folder";

    public static final String[] FOLDERS_PROJECTION = {
        BaseColumns._ID,
        FolderColumns.PERSISTENT_ID,
        FolderColumns.URI,
        FolderColumns.NAME,
        FolderColumns.HAS_CHILDREN,
        FolderColumns.CAPABILITIES,
        FolderColumns.SYNC_WINDOW,
        FolderColumns.CONVERSATION_LIST_URI,
        FolderColumns.CHILD_FOLDERS_LIST_URI,
        FolderColumns.UNSEEN_COUNT,
        FolderColumns.UNREAD_COUNT,
        FolderColumns.TOTAL_COUNT,
        FolderColumns.REFRESH_URI,
        FolderColumns.SYNC_STATUS,
        FolderColumns.LAST_SYNC_RESULT,
        FolderColumns.TYPE,
        FolderColumns.ICON_RES_ID,
        FolderColumns.NOTIFICATION_ICON_RES_ID,
        FolderColumns.BG_COLOR,
        FolderColumns.FG_COLOR,
        FolderColumns.LOAD_MORE_URI,
        FolderColumns.HIERARCHICAL_DESC,
        FolderColumns.LAST_MESSAGE_TIMESTAMP,
        FolderColumns.PARENT_URI
    };

    public static final String[] FOLDERS_PROJECTION_WITH_UNREAD_SENDERS =
            (new ImmutableList.Builder<String>()
                    .addAll(ImmutableList.copyOf(FOLDERS_PROJECTION))
                    .add(FolderColumns.UNREAD_SENDERS)
                    .build().toArray(new String[0]));

    public static final int FOLDER_ID_COLUMN = 0;
    public static final int FOLDER_PERSISTENT_ID_COLUMN = 1;
    public static final int FOLDER_URI_COLUMN = 2;
    public static final int FOLDER_NAME_COLUMN = 3;
    public static final int FOLDER_HAS_CHILDREN_COLUMN = 4;
    public static final int FOLDER_CAPABILITIES_COLUMN = 5;
    public static final int FOLDER_SYNC_WINDOW_COLUMN = 6;
    public static final int FOLDER_CONVERSATION_LIST_URI_COLUMN = 7;
    public static final int FOLDER_CHILD_FOLDERS_LIST_COLUMN = 8;
    public static final int FOLDER_UNSEEN_COUNT_COLUMN = 9;
    public static final int FOLDER_UNREAD_COUNT_COLUMN = 10;
    public static final int FOLDER_TOTAL_COUNT_COLUMN = 11;
    public static final int FOLDER_REFRESH_URI_COLUMN = 12;
    public static final int FOLDER_SYNC_STATUS_COLUMN = 13;
    public static final int FOLDER_LAST_SYNC_RESULT_COLUMN = 14;
    public static final int FOLDER_TYPE_COLUMN = 15;
    public static final int FOLDER_ICON_RES_ID_COLUMN = 16;
    public static final int FOLDER_NOTIFICATION_ICON_RES_ID_COLUMN = 17;
    public static final int FOLDER_BG_COLOR_COLUMN = 18;
    public static final int FOLDER_FG_COLOR_COLUMN = 19;
    public static final int FOLDER_LOAD_MORE_URI_COLUMN = 20;
    public static final int FOLDER_HIERARCHICAL_DESC_COLUMN = 21;
    public static final int FOLDER_LAST_MESSAGE_TIMESTAMP_COLUMN = 22;
    public static final int FOLDER_PARENT_URI_COLUMN = 23;

    public static final class FolderType {
        /** A user defined label. */
        public static final int DEFAULT = 1 << 0;
        /** A system defined inbox */
        public static final int INBOX = 1 << 1;
        /** A system defined containing mails to be edited before sending. */
        public static final int DRAFT = 1 << 2;
        /** A system defined folder containing mails <b>to be</b> sent */
        public static final int OUTBOX = 1 << 3;
        /** A system defined folder containing sent mails */
        public static final int SENT = 1 << 4;
        /** A system defined trash folder */
        public static final int TRASH = 1 << 5;
        /** A system defined spam folder */
        public static final int SPAM = 1 << 6;
        /** A system defined starred folder */
        public static final int STARRED = 1 << 7;
        /** Any other system label that we do not have a specific name for. */
        public static final int OTHER_PROVIDER_FOLDER = 1 << 8;
        /** All mail folder */
        public static final int ALL_MAIL = 1 << 9;
        /** Gmail's inbox sections */
        public static final int INBOX_SECTION = 1 << 10;
        /** A system defined unread folder */
        public static final int UNREAD = 1 << 11;
        /** A "fake" search folder */
        public static final int SEARCH = 1 << 12;
    }

    public static final class FolderCapabilities {
        public static final int SYNCABLE = 0x0001;
        public static final int PARENT = 0x0002;
        // FEEL FREE TO USE 0x0004 - was previous CAN_HOLD_MAIL but that was true for all
        // folders so we removed that value
        public static final int CAN_ACCEPT_MOVED_MESSAGES = 0x0008;
         /**
         * For accounts that support archive, this will indicate that this folder supports
         * the archive functionality.
         */
        public static final int ARCHIVE = 0x0010;

        /**
         * This will indicated that this folder supports the delete functionality.
         */
        public static final int DELETE = 0x0020;

        /**
         * For accounts that support report spam, this will indicate that this folder supports
         * the report spam functionality.
         */
        public static final int REPORT_SPAM = 0x0040;

        /**
         * For accounts that support report spam, this will indicate that this folder supports
         * the mark not spam functionality.
         */
        public static final int MARK_NOT_SPAM = 0x0080;

        /**
         * For accounts that support mute, this will indicate if a mute is performed from within
         * this folder, the action is destructive.
         */
        public static final int DESTRUCTIVE_MUTE = 0x0100;

        /**
         * Indicates that a folder supports settings (sync lookback, etc.)
         */
        public static final int SUPPORTS_SETTINGS = 0x0200;
        /**
         * All the messages in this folder are important.
         */
        public static final int ONLY_IMPORTANT = 0x0400;
        /**
         * Deletions in this folder can't be undone (could include archive if desirable)
         */
        public static final int DELETE_ACTION_FINAL = 0x0800;
        /**
         * This folder is virtual, i.e. contains conversations potentially pulled from other
         * folders, potentially even from different accounts.  Examples might be a "starred"
         * folder, or an "unread" folder (per account or provider-wide)
         */
        public static final int IS_VIRTUAL = 0x1000;

        /**
         * For accounts that support report phishing, this will indicate that this folder supports
         * the report phishing functionality.
         */
        public static final int REPORT_PHISHING = 0x2000;

        /**
         * The flag indicates that the user has the ability to move conversations
         * from this folder.
         */
        public static final int ALLOWS_REMOVE_CONVERSATION = 0x4000;

        /**
         * The flag indicates that the user has the ability to move conversations to or from this
         * Folder in the same operation as other Folder changes (usually through
         * {@link com.android.mail.ui.MultiFoldersSelectionDialog}).
         */
        public static final int MULTI_MOVE = 0x8000;

        /**
         * This flag indicates that a conversation may be moved from this folder into the account's
         * inbox.
         */
        public static final int ALLOWS_MOVE_TO_INBOX = 0x10000;
    }

    public static final class FolderColumns {
        /**
         * This string column contains an id for the folder that is constant across devices, or
         * null if there is no constant id.
         */
        public static final String PERSISTENT_ID = "persistentId";
        /**
         * This string column contains the uri of the folder.
         */
        public static final String URI = "folderUri";
        /**
         * This string column contains the human visible name for the folder.
         */
        public static final String NAME = "name";
        /**
         * This int column represents the capabilities of the folder specified by
         * FolderCapabilities flags.
         */
        public static final String CAPABILITIES = "capabilities";
        /**
         * This int column represents whether or not this folder has any
         * child folders.
         */
        public static final String HAS_CHILDREN = "hasChildren";
        /**
         * This int column represents how large the sync window is.
         */
        public static final String SYNC_WINDOW = "syncWindow";
        /**
         * This string column contains the content provider uri to return the
         * list of conversations for this folder.
         */
        public static final String CONVERSATION_LIST_URI = "conversationListUri";
        /**
         * This string column contains the content provider uri to return the
         * list of child folders of this folder.
         */
        public static final String CHILD_FOLDERS_LIST_URI = "childFoldersListUri";
        /**
         * This int column contains the current unseen count for the folder, if known.
         */
        public static final String UNSEEN_COUNT = "unseenCount";
        /**
         * This int column contains the current unread count for the folder.
         */
        public static final String UNREAD_COUNT = "unreadCount";

        public static final String TOTAL_COUNT = "totalCount";
        /**
         * This string column contains the content provider uri to force a
         * refresh of this folder.
         */
        public static final  String REFRESH_URI = "refreshUri";
        /**
         * This int column contains current sync status of the folder; some combination of the
         * SyncStatus bits defined above
         */
        public static final String SYNC_STATUS  = "syncStatus";
        /**
         * This int column contains the sync status of the last sync attempt; one of the
         * LastSyncStatus values defined above
         */
        public static final String LAST_SYNC_RESULT  = "lastSyncResult";
        /**
         * This int column contains the icon res id for this folder, or 0 if there is none.
         */
        public static final String ICON_RES_ID = "iconResId";
        /**
         * This int column contains the notification icon res id for this folder, or 0 if there is
         * none.
         */
        public static final String NOTIFICATION_ICON_RES_ID = "notificationIconResId";
        /**
         * This int column contains the type of the folder. Zero is default.
         */
        public static final String TYPE = "type";
        /**
         * String representing the integer background color associated with this
         * folder, or null.
         */
        public static final String BG_COLOR = "bgColor";
        /**
         * String representing the integer of the foreground color associated
         * with this folder, or null.
         */
        public static final String FG_COLOR = "fgColor";
        /**
         * String with the content provider Uri used to request more items in the folder, or null.
         */
        public static final String LOAD_MORE_URI = "loadMoreUri";

        /**
         * Possibly empty string that describes the full hierarchy of a folder
         * along with its name.
         */
        public static final String HIERARCHICAL_DESC = "hierarchicalDesc";

        /**
         * The timestamp of the last message received in this folder.
         */
        public static final String LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp";

        /**
         * The URI, possibly null, of the parent folder.
         */
        public static final String PARENT_URI = "parentUri";

        /**
         * A string of unread senders sorted by date, so we don't have to fetch this in multiple
         * queries
         */
        public static final String UNREAD_SENDERS = "unreadSenders";

        public FolderColumns() {}
    }

    // We define a "folder" as anything that contains a list of conversations.
    public static final String CONVERSATION_LIST_TYPE =
            "vnd.android.cursor.dir/vnd.com.android.mail.conversation";
    public static final String CONVERSATION_TYPE =
            "vnd.android.cursor.item/vnd.com.android.mail.conversation";


    public static final String[] CONVERSATION_PROJECTION = {
        BaseColumns._ID,
        ConversationColumns.URI,
        ConversationColumns.MESSAGE_LIST_URI,
        ConversationColumns.SUBJECT,
        ConversationColumns.SNIPPET,
        ConversationColumns.CONVERSATION_INFO,
        ConversationColumns.DATE_RECEIVED_MS,
        ConversationColumns.HAS_ATTACHMENTS,
        ConversationColumns.NUM_MESSAGES,
        ConversationColumns.NUM_DRAFTS,
        ConversationColumns.SENDING_STATE,
        ConversationColumns.PRIORITY,
        ConversationColumns.READ,
        ConversationColumns.SEEN,
        ConversationColumns.STARRED,
        ConversationColumns.RAW_FOLDERS,
        ConversationColumns.FLAGS,
        ConversationColumns.PERSONAL_LEVEL,
        ConversationColumns.SPAM,
        ConversationColumns.PHISHING,
        ConversationColumns.MUTED,
        ConversationColumns.COLOR,
        ConversationColumns.ACCOUNT_URI,
        ConversationColumns.SENDER_INFO,
        ConversationColumns.CONVERSATION_BASE_URI,
        ConversationColumns.REMOTE,
        ConversationColumns.ATTACHMENT_PREVIEW_URI0,
        ConversationColumns.ATTACHMENT_PREVIEW_URI1,
        ConversationColumns.ATTACHMENT_PREVIEW_STATES,
        ConversationColumns.ATTACHMENT_PREVIEWS_COUNT,
    };

    /**
     * This integer corresponds to the number of rows of queries that specify the
     * {@link UIProvider#CONVERSATION_PROJECTION} projection will fit in a single
     * {@link android.database.CursorWindow}
     */
    public static final int CONVERSATION_PROJECTION_QUERY_CURSOR_WINDOW_LIMT = 1500;

    // These column indexes only work when the caller uses the
    // default CONVERSATION_PROJECTION defined above.
    public static final int CONVERSATION_ID_COLUMN = 0;
    public static final int CONVERSATION_URI_COLUMN = 1;
    public static final int CONVERSATION_MESSAGE_LIST_URI_COLUMN = 2;
    public static final int CONVERSATION_SUBJECT_COLUMN = 3;
    public static final int CONVERSATION_SNIPPET_COLUMN = 4;
    public static final int CONVERSATION_INFO_COLUMN = 5;
    public static final int CONVERSATION_DATE_RECEIVED_MS_COLUMN = 6;
    public static final int CONVERSATION_HAS_ATTACHMENTS_COLUMN = 7;
    public static final int CONVERSATION_NUM_MESSAGES_COLUMN = 8;
    public static final int CONVERSATION_NUM_DRAFTS_COLUMN = 9;
    public static final int CONVERSATION_SENDING_STATE_COLUMN = 10;
    public static final int CONVERSATION_PRIORITY_COLUMN = 11;
    public static final int CONVERSATION_READ_COLUMN = 12;
    public static final int CONVERSATION_SEEN_COLUMN = 13;
    public static final int CONVERSATION_STARRED_COLUMN = 14;
    public static final int CONVERSATION_RAW_FOLDERS_COLUMN = 15;
    public static final int CONVERSATION_FLAGS_COLUMN = 16;
    public static final int CONVERSATION_PERSONAL_LEVEL_COLUMN = 17;
    public static final int CONVERSATION_IS_SPAM_COLUMN = 18;
    public static final int CONVERSATION_IS_PHISHING_COLUMN = 19;
    public static final int CONVERSATION_MUTED_COLUMN = 20;
    public static final int CONVERSATION_COLOR_COLUMN = 21;
    public static final int CONVERSATION_ACCOUNT_URI_COLUMN = 22;
    public static final int CONVERSATION_SENDER_INFO_COLUMN = 23;
    public static final int CONVERSATION_BASE_URI_COLUMN = 24;
    public static final int CONVERSATION_REMOTE_COLUMN = 25;
    public static final int CONVERSATION_ATTACHMENT_PREVIEW_URI0_COLUMN = 26;
    public static final int CONVERSATION_ATTACHMENT_PREVIEW_URI1_COLUMN = 27;
    public static final int CONVERSATION_ATTACHMENT_PREVIEW_STATES_COLUMN = 28;
    public static final int CONVERSATION_ATTACHMENT_PREVIEWS_COUNT_COLUMN = 29;

    public static final class ConversationSendingState {
        public static final int OTHER = 0;
        public static final int QUEUED = 1;
        public static final int SENDING = 2;
        public static final int SENT = 3;
        public static final int SEND_ERROR = -1;
    }

    public static final class ConversationPriority {
        public static final int DEFAULT = 0;
        public static final int IMPORTANT = 1;
        public static final int LOW = 0;
        public static final int HIGH = 1;
    }

    public static final class ConversationPersonalLevel {
        public static final int NOT_TO_ME = 0;
        public static final int TO_ME_AND_OTHERS = 1;
        public static final int ONLY_TO_ME = 2;
    }

    public static final class ConversationFlags {
        public static final int REPLIED = 1<<2;
        public static final int FORWARDED = 1<<3;
        public static final int CALENDAR_INVITE = 1<<4;
    }

    public static final class ConversationPhishing {
        public static final int NOT_PHISHING = 0;
        public static final int PHISHING = 1;
    }

    /**
     * Names of columns representing fields in a Conversation.
     */
    public static final class ConversationColumns {
        public static final String URI = "conversationUri";
        /**
         * This string column contains the content provider uri to return the
         * list of messages for this conversation.
         * The cursor returned by this query can return a {@link android.os.Bundle}
         * from a call to {@link android.database.Cursor#getExtras()}.  This Bundle may have
         * values with keys listed in {@link CursorExtraKeys}
         */
        public static final String MESSAGE_LIST_URI = "messageListUri";
        /**
         * This string column contains the subject string for a conversation.
         */
        public static final String SUBJECT = "subject";
        /**
         * This string column contains the snippet string for a conversation.
         */
        public static final String SNIPPET = "snippet";
        /**
         * @deprecated
         */
        @Deprecated
        public static final String SENDER_INFO = "senderInfo";
        /**
         * This blob column contains the byte-array representation of the Parceled
         * ConversationInfo object for a conversation.
         *
         * @deprecated providers should implement
         * {@link ConversationCursorCommand#COMMAND_GET_CONVERSATION_INFO} instead.
         */
        @Deprecated
        public static final String CONVERSATION_INFO = "conversationInfo";
        /**
         * This long column contains the time in ms of the latest update to a
         * conversation.
         */
        public static final String DATE_RECEIVED_MS = "dateReceivedMs";

        /**
         * This boolean column contains whether any messages in this conversation
         * have attachments.
         */
        public static final String HAS_ATTACHMENTS = "hasAttachments";

        /**
         * This int column contains the number of messages in this conversation.
         * For unthreaded, this will always be 1.
         */
        public static final String NUM_MESSAGES = "numMessages";

        /**
         * This int column contains the number of drafts associated with this
         * conversation.
         */
        public static final String NUM_DRAFTS = "numDrafts";

        /**
         * This int column contains the state of drafts and replies associated
         * with this conversation. Use ConversationSendingState to interpret
         * this field.
         */
        public static final String SENDING_STATE = "sendingState";

        /**
         * This int column contains the priority of this conversation. Use
         * ConversationPriority to interpret this field.
         */
        public static final String PRIORITY = "priority";

        /**
         * This int column indicates whether the conversation has been read
         */
        public static final String READ = "read";

        /**
         * This int column indicates whether the conversation has been seen
         */
        public static final String SEEN = "seen";

        /**
         * This int column indicates whether the conversation has been starred
         */
        public static final String STARRED = "starred";

        /**
         * This blob column contains the marshalled form of a Parceled
         * {@FolderList} object. Ideally, only ever use this for
         * rendering the folder list for a conversation.
         *
         * @deprecated providers should implement
         * {@link ConversationCursorCommand#COMMAND_GET_RAW_FOLDERS} instead.
         */
        @Deprecated
        public static final String RAW_FOLDERS = "rawFolders";
        public static final String FLAGS = "conversationFlags";
        /**
         * This int column indicates the personal level of a conversation per
         * {@link ConversationPersonalLevel}.
         */
        public static final String PERSONAL_LEVEL = "personalLevel";

        /**
         * This int column indicates whether the conversation is marked spam.
         */
        public static final String SPAM = "spam";

        /**
         * This int column indicates whether the conversation is marked phishing.
         */
        public static final String PHISHING = "phishing";

        /**
         * This int column indicates whether the conversation was muted.
         */
        public static final String MUTED = "muted";

        /**
         * This int column contains a color for the conversation (used in Email only)
         */
        public static final String COLOR = "color";

        /**
         * This String column contains the Uri for this conversation's account
         */
        public static final String ACCOUNT_URI = "accountUri";
        /**
         * This int column indicates whether a conversation is remote (non-local), and would require
         * a network fetch to load.
         */
        public static final String REMOTE = "remote";
        /**
         * This int column indicates whether the conversation was displayed on the UI and the
         * user got a chance to read it. The UI does not read this value, it is meant only to
         * write the status back to the provider. As a result, it is not available in the
         * {@link Conversation} object.
         */
        public static final String VIEWED = "viewed";
        /**
         * This String column contains the base uri for this conversation.  This uri can be used
         * when handling relative urls in the message content
         */
        public static final String CONVERSATION_BASE_URI = "conversationBaseUri";

        /**
         * This string column contains the uri of the first attachment preview of the first unread
         * message, denoted by UNREAD_MESSAGE_ID.
         */
        public static final String ATTACHMENT_PREVIEW_URI0 = "attachmentPreviewUri0";

        /**
         * This string column contains the uri of the second attachment preview of the first unread
         * message, denoted by UNREAD_MESSAGE_ID.
         */
        public static final String ATTACHMENT_PREVIEW_URI1 = "attachmentPreviewUri1";

        /**
         * This int column contains the states of the attachment previews of the first unread
         * message, the same message used for the snippet. The states is a packed int,
         * where the first and second bits represent the SIMPLE and BEST state of the first
         * attachment preview, while the third and fourth bits represent those states for the
         * second attachment preview. For each bit, a one means that rendition of that attachment
         * preview is downloaded.
         */
        public static final String ATTACHMENT_PREVIEW_STATES = "attachmentPreviewStates";

        /**
         * This int column contains the total count of images in the first unread message. The
         * total count may be higher than the number of ATTACHMENT_PREVIEW_URI columns.
         */
        public static final String ATTACHMENT_PREVIEWS_COUNT = "attachmentPreviewsCount";

        private ConversationColumns() {
        }
    }

    public static final class ConversationCursorCommand {

        public static final String COMMAND_RESPONSE_OK = "ok";
        public static final String COMMAND_RESPONSE_FAILED = "failed";

        /**
         * Incoming bundles may include this key with an Integer bitfield value. See below for bit
         * values.
         */
        public static final String COMMAND_KEY_OPTIONS = "options";

        /**
         * Clients must set this bit when the {@link Cursor#respond(Bundle)} call is being used to
         * fetch a {@link Parcelable}. It serves as a hint that this call requires the cursor
         * position to first safely be moved.
         */
        public static final int OPTION_MOVE_POSITION = 0x01;

        /**
         * This bundle key has a boolean value: true to indicate that this cursor has been shown
         * to the user.
         * <p>
         * A provider that implements this command should include this key in its response with a
         * value of {@link #COMMAND_RESPONSE_OK} or {@link #COMMAND_RESPONSE_FAILED}.
         */
        public static final String COMMAND_KEY_SET_VISIBILITY = "setVisibility";

        /**
         * This key has a boolean value: true to indicate that this folder list is shown to the user
         * either on first call (launcher/widget/notification) or after switching from an existing
         * folder: Inbox -> Folder. Repeated calls are sent when switching back to the folder. Inbox
         * -> Folder -> Spam -> Folder will generate two calls to respond() with the value true for
         * "Folder".
         * <p>
         * A provider that implements this command should include the
         * {@link #COMMAND_KEY_SET_VISIBILITY} key in its response with a value of
         * {@link #COMMAND_RESPONSE_OK} or {@link #COMMAND_RESPONSE_FAILED}. This is <b>always</b>
         * set with {@link #COMMAND_KEY_SET_VISIBILITY} because this is only set when the folder
         * list is made visible.
         */
        public static final String COMMAND_KEY_ENTERED_FOLDER = "enteredFolder";

        /**
         * This key has an int value, indicating the position that the UI wants to notify the
         * provider that the data from a specified row is being shown to the user.
         * <p>
         * A provider that implements this command should include the
         * {@link #COMMAND_NOTIFY_CURSOR_UI_POSITION_CHANGE} key in its response with a value of
         * {@link #COMMAND_RESPONSE_OK} or {@link #COMMAND_RESPONSE_FAILED}.
         */
        public static final String COMMAND_NOTIFY_CURSOR_UI_POSITION_CHANGE = "uiPositionChange";

        /**
         * Rather than jamming a {@link ConversationInfo} into a byte-array blob to be read out of
         * a cursor, providers can optionally implement this command to directly return the object
         * in a Bundle.
         * <p>
         * The requestor (UI code) will place a meaningless value in the request Bundle. The UI will
         * also move the cursor position to the desired place prior to calling respond(). Providers
         * should just use {@link Bundle#containsKey(String)} to check for this kind of request and
         * generate an object at the current cursor position.
         * <p>
         * A provider that implements this command should include the
         * {@link #COMMAND_GET_CONVERSATION_INFO} key in its response with a
         * {@link ConversationInfo} Parcelable object as its value.
         */
        public static final String COMMAND_GET_CONVERSATION_INFO =
                ConversationColumns.CONVERSATION_INFO;

        /**
         * Rather than jamming a {@link FolderList} into a byte-array blob to be read out of
         * a cursor, providers can optionally implement this command to directly return the object
         * in a Bundle.
         * <p>
         * The requestor (UI code) will place a meaningless value in the request Bundle. The UI will
         * also move the cursor position to the desired place prior to calling respond(). Providers
         * should just use {@link Bundle#containsKey(String)} to check for this kind of request and
         * generate an object at the current cursor position.
         * <p>
         * A provider that implements this command should include the
         * {@link #COMMAND_GET_RAW_FOLDERS} key in its response with a
         * {@link FolderList} Parcelable object as its value.
         */
        public static final String COMMAND_GET_RAW_FOLDERS = ConversationColumns.RAW_FOLDERS;

        private ConversationCursorCommand() {}
    }

    /**
     * List of operations that can can be performed on a conversation. These operations are applied
     * with {@link ContentProvider#update(Uri, ContentValues, String, String[])}
     * where the conversation uri is specified, and the ContentValues specifies the operation to
     * be performed.
     * <p/>
     * The operation to be performed is specified in the ContentValues by
     * the {@link ConversationOperations#OPERATION_KEY}
     * <p/>
     * Note not all UI providers will support these operations.  {@link AccountCapabilities} can
     * be used to determine which operations are supported.
     */
    public static final class ConversationOperations {
        /**
         * ContentValues key used to specify the operation to be performed
         */
        public static final String OPERATION_KEY = "operation";

        /**
         * Archive operation
         */
        public static final String ARCHIVE = "archive";

        /**
         * Mute operation
         */
        public static final String MUTE = "mute";

        /**
         * Report spam operation
         */
        public static final String REPORT_SPAM = "report_spam";

        /**
         * Report not spam operation
         */
        public static final String REPORT_NOT_SPAM = "report_not_spam";

        /**
         * Report phishing operation
         */
        public static final String REPORT_PHISHING = "report_phishing";

        /**
         * Discard drafts operation
         */
        public static final String DISCARD_DRAFTS = "discard_drafts";

        /**
         * Update conversation folder(s) operation. ContentValues passed as part
         * of this update will be of the format (FOLDERS_UPDATED, csv of updated
         * folders) where the comma separated values of the updated folders will
         * be of the format: folderuri/ADD_VALUE. ADD_VALUE will be true if the
         * folder was added, false if it was removed.
         */
        public static final String FOLDERS_UPDATED = "folders_updated";
        public static final String FOLDERS_UPDATED_SPLIT_PATTERN = ",";

        public static final class Parameters {
            /**
             * Boolean indicating whether the undo for this operation should be suppressed
             */
            public static final String SUPPRESS_UNDO = "suppress_undo";

            private Parameters() {}
        }

        private ConversationOperations() {
        }
    }

    /**
     * Methods that can be "called" using the account uri, through
     * {@link android.content.ContentResolver#call(Uri,String,String,Bundle)}
     * Note, the arg parmateter of call should be the account uri.
     */
    public static final class AccountCallMethods {
        /**
         * Save message method.  The Bundle for the call to
         * {@link android.content.ContentResolver#call(Uri,String,String,Bundle)} should have the
         * columns specified in {@link MessageColumns}, and if this is a save for an existing
         * message, an entry for the {@link MessageColumns#URI} should reference the existing
         * message
         *
         * The Bundle returned will contain the message uri in the returned bundled with the
         * {@link MessageColumns#URI} key.
         */
        public static final String SAVE_MESSAGE = "save_message";

        /**
         * Send message method.  The Bundle for the call to
         * {@link android.content.ContentResolver#call(Uri,String,String,Bundle)} should have the
         * columns specified in {@link MessageColumns}, and if this is a send of an existing
         * message, an entry for the {@link MessageColumns#URI} should reference the existing
         * message
         *
         * The Bundle returned will contain the message uri in the returned bundled with the
         * {@link MessageColumns#URI} key.
         */
        public static final String SEND_MESSAGE = "send_message";

        /**
         * Change account method.  The Bundle for the call to
         * {@link android.content.ContentResolver#call(Uri,String,String,Bundle)} should have the
         * columns specified in {@link SetCurrentAccountColumns}
         *
         * The Bundle returned will be empty.
         */
        public static final String SET_CURRENT_ACCOUNT = "set_current_account";

        private AccountCallMethods() {}
    }

    /**
     * Keys used for parameters to {@link AccountCallMethods#SEND_MESSAGE} or
     * {@link AccountCallMethods#SAVE_MESSAGE} methods.
     */
    public static final class SendOrSaveMethodParamKeys {
        /**
         * Bundle key used to store any opened file descriptors.
         * The keys of this Bundle are the contentUri for each attachment, and the
         * values are {@link android.os.ParcelFileDescriptor} objects.
         */
        public static final String OPENED_FD_MAP = "opened_fds";

        private SendOrSaveMethodParamKeys() {}
    }

    public static final class DraftType {
        public static final int NOT_A_DRAFT = 0;
        public static final int COMPOSE = 1;
        public static final int REPLY = 2;
        public static final int REPLY_ALL = 3;
        public static final int FORWARD = 4;

        private DraftType() {}
    }

    /**
     * Class for the enum values to determine whether this
     * string should be displayed as a high priority warning
     * or a low priority warning. The current design has
     * high priority warnings in red while low priority warnings
     * are grey.
     */
    public static final class SpamWarningLevel {
        public static final int NO_WARNING = 0;
        public static final int LOW_WARNING = 1;
        public static final int HIGH_WARNING = 2;

        private SpamWarningLevel() {}
    }

    /**
     * Class for the enum values to determine which type
     * of link to show in the spam warning.
     */
    public static final class SpamWarningLinkType {
        public static final int NO_LINK = 0;
        public static final int IGNORE_WARNING = 1;
        public static final int REPORT_PHISHING = 2;

        private SpamWarningLinkType() {}
    }

    public static final String[] MESSAGE_PROJECTION = {
        BaseColumns._ID,
        MessageColumns.SERVER_ID,
        MessageColumns.URI,
        MessageColumns.CONVERSATION_ID,
        MessageColumns.SUBJECT,
        MessageColumns.SNIPPET,
        MessageColumns.FROM,
        MessageColumns.TO,
        MessageColumns.CC,
        MessageColumns.BCC,
        MessageColumns.REPLY_TO,
        MessageColumns.DATE_RECEIVED_MS,
        MessageColumns.BODY_HTML,
        MessageColumns.BODY_TEXT,
        MessageColumns.EMBEDS_EXTERNAL_RESOURCES,
        MessageColumns.REF_MESSAGE_ID,
        MessageColumns.DRAFT_TYPE,
        MessageColumns.APPEND_REF_MESSAGE_CONTENT,
        MessageColumns.HAS_ATTACHMENTS,
        MessageColumns.ATTACHMENT_LIST_URI,
        MessageColumns.MESSAGE_FLAGS,
        MessageColumns.ALWAYS_SHOW_IMAGES,
        MessageColumns.READ,
        MessageColumns.SEEN,
        MessageColumns.STARRED,
        MessageColumns.QUOTE_START_POS,
        MessageColumns.ATTACHMENTS,
        MessageColumns.CUSTOM_FROM_ADDRESS,
        MessageColumns.MESSAGE_ACCOUNT_URI,
        MessageColumns.EVENT_INTENT_URI,
        MessageColumns.SPAM_WARNING_STRING,
        MessageColumns.SPAM_WARNING_LEVEL,
        MessageColumns.SPAM_WARNING_LINK_TYPE,
        MessageColumns.VIA_DOMAIN,
        MessageColumns.IS_SENDING
    };

    /** Separates attachment info parts in strings in a message. */
    @Deprecated
    public static final String MESSAGE_ATTACHMENT_INFO_SEPARATOR = "\n";
    public static final String MESSAGE_LIST_TYPE =
            "vnd.android.cursor.dir/vnd.com.android.mail.message";
    public static final String MESSAGE_TYPE =
            "vnd.android.cursor.item/vnd.com.android.mail.message";

    public static final int MESSAGE_ID_COLUMN = 0;
    public static final int MESSAGE_SERVER_ID_COLUMN = 1;
    public static final int MESSAGE_URI_COLUMN = 2;
    public static final int MESSAGE_CONVERSATION_URI_COLUMN = 3;
    public static final int MESSAGE_SUBJECT_COLUMN = 4;
    public static final int MESSAGE_SNIPPET_COLUMN = 5;
    public static final int MESSAGE_FROM_COLUMN = 6;
    public static final int MESSAGE_TO_COLUMN = 7;
    public static final int MESSAGE_CC_COLUMN = 8;
    public static final int MESSAGE_BCC_COLUMN = 9;
    public static final int MESSAGE_REPLY_TO_COLUMN = 10;
    public static final int MESSAGE_DATE_RECEIVED_MS_COLUMN = 11;
    public static final int MESSAGE_BODY_HTML_COLUMN = 12;
    public static final int MESSAGE_BODY_TEXT_COLUMN = 13;
    public static final int MESSAGE_EMBEDS_EXTERNAL_RESOURCES_COLUMN = 14;
    public static final int MESSAGE_REF_MESSAGE_URI_COLUMN = 15;
    public static final int MESSAGE_DRAFT_TYPE_COLUMN = 16;
    public static final int MESSAGE_APPEND_REF_MESSAGE_CONTENT_COLUMN = 17;
    public static final int MESSAGE_HAS_ATTACHMENTS_COLUMN = 18;
    public static final int MESSAGE_ATTACHMENT_LIST_URI_COLUMN = 19;
    public static final int MESSAGE_FLAGS_COLUMN = 20;
    public static final int MESSAGE_ALWAYS_SHOW_IMAGES_COLUMN = 21;
    public static final int MESSAGE_READ_COLUMN = 22;
    public static final int MESSAGE_SEEN_COLUMN = 23;
    public static final int MESSAGE_STARRED_COLUMN = 24;
    public static final int QUOTED_TEXT_OFFSET_COLUMN = 25;
    public static final int MESSAGE_ATTACHMENTS_COLUMN = 26;
    public static final int MESSAGE_CUSTOM_FROM_ADDRESS_COLUMN = 27;
    public static final int MESSAGE_ACCOUNT_URI_COLUMN = 28;
    public static final int MESSAGE_EVENT_INTENT_COLUMN = 29;
    public static final int MESSAGE_SPAM_WARNING_STRING_ID_COLUMN = 30;
    public static final int MESSAGE_SPAM_WARNING_LEVEL_COLUMN = 31;
    public static final int MESSAGE_SPAM_WARNING_LINK_TYPE_COLUMN = 32;
    public static final int MESSAGE_VIA_DOMAIN_COLUMN = 33;
    public static final int MESSAGE_IS_SENDING_COLUMN = 34;

    public static final class CursorStatus {
        // The cursor is actively loading more data
        public static final int LOADING =      1 << 0;

        // The cursor is currently not loading more data, but more data may be available
        public static final int LOADED =       1 << 1;

        // An error occured while loading data
        public static final int ERROR =        1 << 2;

        // The cursor is loaded, and there will be no more data
        public static final int COMPLETE =     1 << 3;

        public static boolean isWaitingForResults(int cursorStatus) {
            return 0 != (cursorStatus & LOADING);
        }
    }


    public static final class CursorExtraKeys {
        /**
         * This integer column contains the staus of the message cursor.  The value will be
         * one defined in {@link CursorStatus}.
         */
        public static final String EXTRA_STATUS = "cursor_status";

        /**
         * Used for finding the cause of an error.
         * TODO: define these values
         */
        public static final String EXTRA_ERROR = "cursor_error";


        /**
         * This integer column contains the total message count for this folder.
         */
        public static final String EXTRA_TOTAL_COUNT = "cursor_total_count";
    }

    public static final class AccountCursorExtraKeys {
        /**
         * This integer column contains the staus of the account cursor.  The value will be
         * 1 if all accounts have been fully loaded or 0 if the account list hasn't been fully
         * initialized
         */
        public static final String ACCOUNTS_LOADED = "accounts_loaded";
    }


    public static final class MessageFlags {
        public static final int REPLIED =           1 << 2;
        public static final int FORWARDED =         1 << 3;
        public static final int CALENDAR_INVITE =   1 << 4;
    }

    public static final class MessageColumns {
        /**
         * This string column contains a content provider URI that points to this single message.
         */
        public static final String URI = "messageUri";
        /**
         * This string column contains a server-assigned ID for this message.
         */
        public static final String SERVER_ID = "serverMessageId";
        public static final String CONVERSATION_ID = "conversationId";
        /**
         * This string column contains the subject of a message.
         */
        public static final String SUBJECT = "subject";
        /**
         * This string column contains a snippet of the message body.
         */
        public static final String SNIPPET = "snippet";
        /**
         * This string column contains the single email address (and optionally name) of the sender.
         */
        public static final String FROM = "fromAddress";
        /**
         * This string column contains a comma-delimited list of "To:" recipient email addresses.
         */
        public static final String TO = "toAddresses";
        /**
         * This string column contains a comma-delimited list of "CC:" recipient email addresses.
         */
        public static final String CC = "ccAddresses";
        /**
         * This string column contains a comma-delimited list of "BCC:" recipient email addresses.
         * This value will be null for incoming messages.
         */
        public static final String BCC = "bccAddresses";
        /**
         * This string column contains the single email address (and optionally name) of the
         * sender's reply-to address.
         */
        public static final String REPLY_TO = "replyToAddress";
        /**
         * This long column contains the timestamp (in millis) of receipt of the message.
         */
        public static final String DATE_RECEIVED_MS = "dateReceivedMs";
        /**
         * This string column contains the HTML form of the message body, if available. If not,
         * a provider must populate BODY_TEXT.
         */
        public static final String BODY_HTML = "bodyHtml";
        /**
         * This string column contains the plaintext form of the message body, if HTML is not
         * otherwise available. If HTML is available, this value should be left empty (null).
         */
        public static final String BODY_TEXT = "bodyText";
        public static final String EMBEDS_EXTERNAL_RESOURCES = "bodyEmbedsExternalResources";
        /**
         * This string column contains an opaque string used by the sendMessage api.
         */
        public static final String REF_MESSAGE_ID = "refMessageId";
        /**
         * This integer column contains the type of this draft, or zero (0) if this message is not a
         * draft. See {@link DraftType} for possible values.
         */
        public static final String DRAFT_TYPE = "draftType";
        /**
         * This boolean column indicates whether an outgoing message should trigger special quoted
         * text processing upon send. The value should default to zero (0) for protocols that do
         * not support or require this flag, and for all incoming messages.
         */
        public static final String APPEND_REF_MESSAGE_CONTENT = "appendRefMessageContent";
        /**
         * This boolean column indicates whether a message has attachments. The list of attachments
         * can be retrieved using the URI in {@link MessageColumns#ATTACHMENT_LIST_URI}.
         */
        public static final String HAS_ATTACHMENTS = "hasAttachments";
        /**
         * This string column contains the content provider URI for the list of
         * attachments associated with this message.
         */
        public static final String ATTACHMENT_LIST_URI = "attachmentListUri";
        /**
         * This long column is a bit field of flags defined in {@link MessageFlags}.
         */
        public static final String MESSAGE_FLAGS = "messageFlags";
        /**
         * This integer column represents whether the user has specified that images should always
         * be shown.  The value of "1" indicates that the user has specified that images should be
         * shown, while the value of "0" indicates that the user should be prompted before loading
         * any external images.
         */
        public static final String ALWAYS_SHOW_IMAGES = "alwaysShowImages";

        /**
         * This boolean column indicates whether the message has been read
         */
        public static final String READ = "read";

        /**
         * This boolean column indicates whether the message has been seen
         */
        public static final String SEEN = "seen";

        /**
         * This boolean column indicates whether the message has been starred
         */
        public static final String STARRED = "starred";

        /**
         * This integer column represents the offset in the message of quoted
         * text. If include_quoted_text is zero, the value contained in this
         * column is invalid.
         */
        public static final String QUOTE_START_POS = "quotedTextStartPos";

        /**
         * This string columns contains a JSON array of serialized {@link Attachment} objects.
         */
        public static final String ATTACHMENTS = "attachments";
        public static final String CUSTOM_FROM_ADDRESS = "customFrom";
        /**
         * Uri of the account associated with this message. Except in the case
         * of showing a combined view, this column is almost always empty.
         */
        public static final String MESSAGE_ACCOUNT_URI = "messageAccountUri";
        /**
         * Intent Uri to launch when the user wants to view an event in their calendar, or null.
         */
        public static final String EVENT_INTENT_URI = "eventIntentUri";
        /**
         * This string column contains the string for the spam
         * warning of this message, or null if there is no spam warning for the message.
         */
        public static final String SPAM_WARNING_STRING = "spamWarningString";
        /**
         * This integer column contains the level of spam warning of this message,
         * or zero (0) if this message does not have a warning level.
         * See {@link SpamWarningLevel} for possible values.
         */
        public static final String SPAM_WARNING_LEVEL = "spamWarningLevel";
        /**
         * This integer column contains the type of link for the spam warning
         * of this message, or zero (0) if this message does not have a link type.
         * See {@link SpamWarningLinkType} for possible values.
         */
        public static final String SPAM_WARNING_LINK_TYPE = "spamWarningLinkType";
        /**
         * This string column contains the string for the via domain
         * to be included if this message was sent via an alternate
         * domain. This column should be null if no via domain exists.
         */
        public static final String VIA_DOMAIN = "viaDomain";
        /**
         * This boolean column indicates whether the message is an outgoing message in the process
         * of being sent (will be zero for incoming messages and messages that are already sent).
         */
        public static final String IS_SENDING = "isSending";

        private MessageColumns() {}
    }

     public static final class SetCurrentAccountColumns {
        /**
         * This column contains the Account object Parcelable.
         */
        public static final String ACCOUNT = "account";

        private SetCurrentAccountColumns() {}
    }

    /**
     * List of operations that can can be performed on a message. These operations are applied
     * with {@link ContentProvider#update(Uri, ContentValues, String, String[])}
     * where the message uri is specified, and the ContentValues specifies the operation to
     * be performed, e.g. values.put(RESPOND_COLUMN, RESPOND_ACCEPT)
     * <p/>
     * Note not all UI providers will support these operations.
     */
    public static final class MessageOperations {
        /**
         * Respond to a calendar invitation
         */
        public static final String RESPOND_COLUMN = "respond";

        public static final int RESPOND_ACCEPT = 1;
        public static final int RESPOND_TENTATIVE = 2;
        public static final int RESPOND_DECLINE = 3;

        private MessageOperations() {
        }
    }

    public static final String ATTACHMENT_LIST_TYPE =
            "vnd.android.cursor.dir/vnd.com.android.mail.attachment";
    public static final String ATTACHMENT_TYPE =
            "vnd.android.cursor.item/vnd.com.android.mail.attachment";

    public static final String[] ATTACHMENT_PROJECTION = {
        AttachmentColumns.NAME,
        AttachmentColumns.SIZE,
        AttachmentColumns.URI,
        AttachmentColumns.CONTENT_TYPE,
        AttachmentColumns.STATE,
        AttachmentColumns.DESTINATION,
        AttachmentColumns.DOWNLOADED_SIZE,
        AttachmentColumns.CONTENT_URI,
        AttachmentColumns.THUMBNAIL_URI,
        AttachmentColumns.PREVIEW_INTENT_URI,
        AttachmentColumns.PROVIDER_DATA,
        AttachmentColumns.SUPPORTS_DOWNLOAD_AGAIN,
        AttachmentColumns.TYPE,
        AttachmentColumns.FLAGS
    };
    public static final int ATTACHMENT_NAME_COLUMN = 0;
    public static final int ATTACHMENT_SIZE_COLUMN = 1;
    public static final int ATTACHMENT_URI_COLUMN = 2;
    public static final int ATTACHMENT_CONTENT_TYPE_COLUMN = 3;
    public static final int ATTACHMENT_STATE_COLUMN = 4;
    public static final int ATTACHMENT_DESTINATION_COLUMN = 5;
    public static final int ATTACHMENT_DOWNLOADED_SIZE_COLUMN = 6;
    public static final int ATTACHMENT_CONTENT_URI_COLUMN = 7;
    public static final int ATTACHMENT_THUMBNAIL_URI_COLUMN = 8;
    public static final int ATTACHMENT_PREVIEW_INTENT_COLUMN = 9;
    public static final int ATTACHMENT_SUPPORTS_DOWNLOAD_AGAIN_COLUMN = 10;
    public static final int ATTACHMENT_TYPE_COLUMN = 11;
    public static final int ATTACHMENT_FLAGS_COLUMN = 12;

    /** Separates attachment info parts in strings in the database. */
    public static final String ATTACHMENT_INFO_SEPARATOR = "\n"; // use to join
    public static final Pattern ATTACHMENT_INFO_SEPARATOR_PATTERN =
            Pattern.compile(ATTACHMENT_INFO_SEPARATOR); // use to split
    public static final String ATTACHMENT_INFO_DELIMITER = "|"; // use to join
    // use to split
    public static final Pattern ATTACHMENT_INFO_DELIMITER_PATTERN = Pattern.compile("\\|");

    /**
     * Valid states for the {@link AttachmentColumns#STATE} column.
     *
     */
    public static final class AttachmentState {
        /**
         * The full attachment is not present on device. When used as a command,
         * setting this state will tell the provider to cancel a download in
         * progress.
         * <p>
         * Valid next states: {@link #DOWNLOADING}, {@link #PAUSED}
         */
        public static final int NOT_SAVED = 0;
        /**
         * The most recent attachment download attempt failed. The current UI
         * design does not require providers to persist this state, but
         * providers must return this state at least once after a download
         * failure occurs. This state may not be used as a command.
         * <p>
         * Valid next states: {@link #DOWNLOADING}
         */
        public static final int FAILED = 1;
        /**
         * The attachment is currently being downloaded by the provider.
         * {@link AttachmentColumns#DOWNLOADED_SIZE} should reflect the current
         * download progress while in this state. When used as a command,
         * setting this state will tell the provider to initiate a download to
         * the accompanying destination in {@link AttachmentColumns#DESTINATION}
         * .
         * <p>
         * Valid next states: {@link #NOT_SAVED}, {@link #FAILED},
         * {@link #SAVED}
         */
        public static final int DOWNLOADING = 2;
        /**
         * The attachment was successfully downloaded to the destination in
         * {@link AttachmentColumns#DESTINATION}. If a provider later detects
         * that a download is missing, it should reset the state to
         * {@link #NOT_SAVED}. This state may not be used as a command on its
         * own. To move a file from cache to external, update
         * {@link AttachmentColumns#DESTINATION}.
         * <p>
         * Valid next states: {@link #NOT_SAVED}, {@link #PAUSED}
         */
        public static final int SAVED = 3;
        /**
         * This is only used as a command, not as a state. The attachment is
         * currently being redownloaded by the provider.
         * {@link AttachmentColumns#DOWNLOADED_SIZE} should reflect the current
         * download progress while in this state. When used as a command,
         * setting this state will tell the provider to initiate a download to
         * the accompanying destination in {@link AttachmentColumns#DESTINATION}
         * .
         */
        public static final int REDOWNLOADING = 4;
        /**
         * The attachment is either pending or paused in the download manager.
         * {@link AttachmentColumns#DOWNLOADED_SIZE} should reflect the current
         * download progress while in this state. This state may not be used as
         * a command on its own.
         * <p>
         * Valid next states: {@link #DOWNLOADING}, {@link #FAILED}
         */
        public static final int PAUSED = 5;

        private AttachmentState() {}
    }

    public static final class AttachmentDestination {

        /**
         * The attachment will be or is already saved to the app-private cache partition.
         */
        public static final int CACHE = 0;
        /**
         * The attachment will be or is already saved to external shared device storage.
         * This value should be 1 since saveToSd is often used in a similar way
         */
        public static final int EXTERNAL = 1;

        private AttachmentDestination() {}
    }

    public static final class AttachmentColumns {
        /**
         * This string column is the attachment's file name, intended for display in UI. It is not
         * the full path of the file.
         */
        public static final String NAME = OpenableColumns.DISPLAY_NAME;
        /**
         * This integer column is the file size of the attachment, in bytes.
         */
        public static final String SIZE = OpenableColumns.SIZE;
        /**
         * This column is a {@link android.net.Uri} that can be queried to
         * monitor download state and progress for this individual attachment
         * (resulting cursor has one single row for this attachment).
         */
        public static final String URI = "uri";
        /**
         * This string column is the MIME type of the attachment.
         */
        public static final String CONTENT_TYPE = "contentType";
        /**
         * This integer column is the current downloading state of the
         * attachment as defined in {@link AttachmentState}.
         * <p>
         * Providers must accept updates to {@link #URI} with new values of
         * this column to initiate or cancel downloads.
         */
        public static final String STATE = "state";
        /**
         * This integer column is the file destination for the current download
         * in progress (when {@link #STATE} is
         * {@link AttachmentState#DOWNLOADING}) or the resulting downloaded file
         * ( when {@link #STATE} is {@link AttachmentState#SAVED}), as defined
         * in {@link AttachmentDestination}. This value is undefined in any
         * other state.
         * <p>
         * Providers must accept updates to {@link #URI} with new values of
         * this column to move an existing downloaded file.
         */
        public static final String DESTINATION = "destination";
        /**
         * This integer column is the current number of bytes downloaded when
         * {@link #STATE} is {@link AttachmentState#DOWNLOADING}. This value is
         * undefined in any other state.
         */
        public static final String DOWNLOADED_SIZE = "downloadedSize";
        /**
         * This column is a {@link android.net.Uri} that points to the
         * downloaded local file when {@link #STATE} is
         * {@link AttachmentState#SAVED}. This value is undefined in any other
         * state.
         */
        public static final String CONTENT_URI = "contentUri";
        /**
         * This column is a {@link android.net.Uri} that points to a local
         * thumbnail file for the attachment. Providers that do not support
         * downloading attachment thumbnails may leave this null.
         */
        public static final String THUMBNAIL_URI = "thumbnailUri";
        /**
         * This column is an {@link android.net.Uri} used in an
         * {@link android.content.Intent#ACTION_VIEW} Intent to launch a preview
         * activity that allows the user to efficiently view an attachment
         * without having to first download the entire file. Providers that do
         * not support previewing attachments may leave this null.
         */
        public static final String PREVIEW_INTENT_URI = "previewIntentUri";
        /**
         * This column contains provider-specific private data as JSON string.
         */
        public static final String PROVIDER_DATA = "providerData";

        /**
         * This column represents whether this attachment supports the ability to be downloaded
         * again.
         */
        public static final String SUPPORTS_DOWNLOAD_AGAIN = "supportsDownloadAgain";
        /**
         * This column represents the visibility type of this attachment. One of the
         * {@link AttachmentType} constants.
         */
        public static final String TYPE = "type";

        /**
         * This column holds various bitwise flags for status information.
         */
        public static final String FLAGS = "flags";

        private AttachmentColumns() {}
    }

    public static final class AttachmentContentValueKeys {
        public static final String RENDITION = "rendition";
        public static final String ADDITIONAL_PRIORITY = "additionalPriority";
        public static final String DELAY_DOWNLOAD = "delayDownload";
    }

    /**
     * Indicates a version of an attachment.
     */
    public static final class AttachmentRendition {

        /** A smaller or simpler version of the attachment, such as a scaled-down image or an HTML
         * version of a document. Not always available.
         */
        public static final int SIMPLE = 0;
        /**
         * The full version of an attachment if it can be handled on the device, otherwise the
         * preview.
         */
        public static final int BEST = 1;

        private static final String SIMPLE_STRING = "SIMPLE";
        private static final String BEST_STRING = "BEST";

        /**
         * Prefer renditions in this order.
         */
        public static final int[] PREFERRED_RENDITIONS = new int[]{BEST, SIMPLE};

        public static int parseRendition(String rendition) {
            if (TextUtils.equals(rendition, SIMPLE_STRING)) {
                return SIMPLE;
            } else if (TextUtils.equals(rendition, BEST_STRING)) {
                return BEST;
            }

            throw new IllegalArgumentException(String.format("Unknown rendition %s", rendition));
        }

        public static String toString(int rendition) {
            if (rendition == BEST) {
                return BEST_STRING;
            } else if (rendition == SIMPLE) {
                return SIMPLE_STRING;
            }

            throw new IllegalArgumentException(String.format("Unknown rendition %d", rendition));
        }
    }

    /**
     * Indicates the visibility type of an attachment.
     */
    public static final class AttachmentType {
        public static final int STANDARD = 0;
        public static final int INLINE_CURRENT_MESSAGE = 1;
        public static final int INLINE_QUOTED_MESSAGE = 2;
    }

    public static final String[] UNDO_PROJECTION = {
        ConversationColumns.MESSAGE_LIST_URI
    };
    public static final int UNDO_MESSAGE_LIST_COLUMN = 0;

    // Parameter used to indicate the sequence number for an undoable operation
    public static final String SEQUENCE_QUERY_PARAMETER = "seq";

    /**
     * Parameter used to force UI notifications in an operation involving
     * {@link ConversationOperations#OPERATION_KEY}.
     */
    public static final String FORCE_UI_NOTIFICATIONS_QUERY_PARAMETER = "forceUiNotifications";

    /**
     * Parameter used to allow returning hidden folders.
     */
    public static final String ALLOW_HIDDEN_FOLDERS_QUERY_PARAM = "allowHiddenFolders";

    public static final String AUTO_ADVANCE_MODE_OLDER = "older";
    public static final String AUTO_ADVANCE_MODE_NEWER = "newer";
    public static final String AUTO_ADVANCE_MODE_LIST = "list";

    /**
     * Settings for auto advancing when the current conversation has been destroyed.
     */
    public static final class AutoAdvance {
        /** No setting specified. */
        public static final int UNSET = 0;
        /** Go to the older message (if available) */
        public static final int OLDER = 1;
        /** Go to the newer message (if available) */
        public static final int NEWER = 2;
        /** Go back to conversation list*/
        public static final int LIST = 3;
        /** The default option is to go to the list */
        public static final int DEFAULT = LIST;

        /**
         * Gets the int value for the given auto advance setting.
         *
         * @param autoAdvanceSetting The string setting, such as "newer", "older", "list"
         */
        public static int getAutoAdvanceInt(final String autoAdvanceSetting) {
            final int autoAdvance;

            if (AUTO_ADVANCE_MODE_NEWER.equals(autoAdvanceSetting)) {
                autoAdvance = UIProvider.AutoAdvance.NEWER;
            } else if (AUTO_ADVANCE_MODE_OLDER.equals(autoAdvanceSetting)) {
                autoAdvance = UIProvider.AutoAdvance.OLDER;
            } else if (AUTO_ADVANCE_MODE_LIST.equals(autoAdvanceSetting)) {
                autoAdvance = UIProvider.AutoAdvance.LIST;
            } else {
                autoAdvance = UIProvider.AutoAdvance.UNSET;
            }

            return autoAdvance;
        }
    }

    /**
     * Settings for what swipe should do.
     */
    public static final class Swipe {
        /** Archive or remove label, if available. */
        public static final int ARCHIVE = 0;
        /** Delete */
        public static final int DELETE = 1;
        /** No swipe */
        public static final int DISABLED = 2;
        /** Default is delete */
        public static final int DEFAULT = ARCHIVE;
    }

    /**
     * Settings for Conversation view mode.
     */
    public static final class ConversationViewMode {
        /**
         * The user hasn't specified a mode.
         */
        public static final int UNDEFINED = -1;
        /**
         * Default to fit the conversation to screen view
         */
        public static final int OVERVIEW = 0;
        /**
         * Conversation text size should be the device default, and wide conversations may
         * require panning
         */
        public static final int READING = 1;
        public static final int DEFAULT = OVERVIEW;
    }

    public static final class SnapHeaderValue {
        public static final int ALWAYS = 0;
        public static final int PORTRAIT_ONLY = 1;
        public static final int NEVER = 2;
    }

    public static final class MessageTextSize {
        public static final int TINY = -2;
        public static final int SMALL = -1;
        public static final int NORMAL = 0;
        public static final int LARGE = 1;
        public static final int HUGE = 2;
    }

    public static final class DefaultReplyBehavior {
        public static final int REPLY = 0;
        public static final int REPLY_ALL = 1;
    }

    /**
     * Setting for whether to show sender images in conversation list.
     */
    public static final class ConversationListIcon {
        public static final int SENDER_IMAGE = 1;
        public static final int NONE = 2;
        public static final int DEFAULT = 1; // Default to show sender image
    }

    /**
     * Action for an intent used to update/create new notifications.  The mime type of this
     * intent should be set to the mimeType of the account that is generating this notification.
     * An intent of this action is required to have the following extras:
     * {@link UpdateNotificationExtras#EXTRA_FOLDER} {@link UpdateNotificationExtras#EXTRA_ACCOUNT}
     */
    public static final String ACTION_UPDATE_NOTIFICATION =
            "com.android.mail.action.update_notification";

    public static final class UpdateNotificationExtras {
        /**
         * Parcelable extra containing a {@link Uri} to a {@link Folder}
         */
        public static final String EXTRA_FOLDER = "notification_extra_folder";

        /**
         * Parcelable extra containing a {@link Uri} to an {@link Account}
         */
        public static final String EXTRA_ACCOUNT = "notification_extra_account";

        /**
         * Integer extra containing the update unread count for the account/folder.
         * If this value is 0, the UI will not block the intent to allow code to clear notifications
         * to run.
         */
        public static final String EXTRA_UPDATED_UNREAD_COUNT = "notification_updated_unread_count";
    }

    public static final class EditSettingsExtras {
        /**
         * Parcelable extra containing account for which the user wants to
         * modify settings
         */
        public static final String EXTRA_ACCOUNT = "extra_account";

        /**
         * Parcelable extra containing folder for which the user wants to
         * modify settings
         */
        public static final String EXTRA_FOLDER = "extra_folder";

        /**
         * Boolean extra which is set true if the user wants to "manage folders"
         */
        public static final String EXTRA_MANAGE_FOLDERS = "extra_manage_folders";
    }

    public static final class SendFeedbackExtras {
        /**
         * Optional boolean extras which indicates that the user is reporting a problem.
         */
        public static final String EXTRA_REPORTING_PROBLEM = "reporting_problem";
        /**
         * Optional Parcelable extra containing the screenshot of the screen where the user
         * is reporting a problem.
         */
        public static final String EXTRA_SCREEN_SHOT = "screen_shot";
    }

    public static final class ViewProxyExtras {
        /**
         * Uri extra passed to the proxy which indicates the original Uri that was intended to be
         * viewed.
         */
        public static final String EXTRA_ORIGINAL_URI = "original_uri";
        /**
         * Parcelable extra passed to the proxy which indicates the account being viewed from.
         */
        public static final String EXTRA_ACCOUNT = "account";
        /**
         * String extra passed from the proxy which indicates the salt used to generate the digest.
         */
        public static final String EXTRA_SALT = "salt";
        /**
         * Byte[] extra passed from the proxy which indicates the digest of the salted account name.
         */
        public static final String EXTRA_ACCOUNT_DIGEST = "digest";
    }
}
