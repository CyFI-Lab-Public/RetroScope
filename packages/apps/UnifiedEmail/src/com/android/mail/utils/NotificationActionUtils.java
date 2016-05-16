/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.mail.utils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import com.android.mail.MailIntentService;
import com.android.mail.NotificationActionIntentService;
import com.android.mail.R;
import com.android.mail.compose.ComposeActivity;
import com.android.mail.providers.Account;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.Message;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.ConversationOperations;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationActionUtils {
    private static final String LOG_TAG = "NotifActionUtils";

    private static long sUndoTimeoutMillis = -1;

    /**
     * If an {@link NotificationAction} exists here for a given notification key, then we should
     * display this undo notification rather than an email notification.
     */
    public static final ObservableSparseArrayCompat<NotificationAction> sUndoNotifications =
            new ObservableSparseArrayCompat<NotificationAction>();

    /**
     * If a {@link Conversation} exists in this set, then the undo notification for this
     * {@link Conversation} was tapped by the user in the notification drawer.
     * We need to properly handle notification actions for this case.
     */
    public static final Set<Conversation> sUndoneConversations = Sets.newHashSet();

    /**
     * If an undo notification is displayed, its timestamp
     * ({@link android.app.Notification.Builder#setWhen(long)}) is stored here so we can use it for
     * the original notification if the action is undone.
     */
    public static final SparseLongArray sNotificationTimestamps = new SparseLongArray();

    public enum NotificationActionType {
        ARCHIVE_REMOVE_LABEL("archive", true, R.drawable.ic_menu_archive_holo_dark,
                R.drawable.ic_menu_remove_label_holo_dark, R.string.notification_action_archive,
                R.string.notification_action_remove_label, new ActionToggler() {
            @Override
            public boolean shouldDisplayPrimary(final Folder folder,
                    final Conversation conversation, final Message message) {
                return folder == null || folder.isInbox();
            }
        }),
        DELETE("delete", true, R.drawable.ic_menu_delete_holo_dark,
                R.string.notification_action_delete),
        REPLY("reply", false, R.drawable.ic_reply_holo_dark, R.string.notification_action_reply),
        REPLY_ALL("reply_all", false, R.drawable.ic_reply_all_holo_dark,
                R.string.notification_action_reply_all);

        private final String mPersistedValue;
        private final boolean mIsDestructive;

        private final int mActionIcon;
        private final int mActionIcon2;

        private final int mDisplayString;
        private final int mDisplayString2;

        private final ActionToggler mActionToggler;

        private static final Map<String, NotificationActionType> sPersistedMapping;

        private interface ActionToggler {
            /**
             * Determines if we should display the primary or secondary text/icon.
             *
             * @return <code>true</code> to display primary, <code>false</code> to display secondary
             */
            boolean shouldDisplayPrimary(Folder folder, Conversation conversation, Message message);
        }

        static {
            final NotificationActionType[] values = values();
            final ImmutableMap.Builder<String, NotificationActionType> mapBuilder =
                    new ImmutableMap.Builder<String, NotificationActionType>();

            for (int i = 0; i < values.length; i++) {
                mapBuilder.put(values[i].getPersistedValue(), values[i]);
            }

            sPersistedMapping = mapBuilder.build();
        }

        private NotificationActionType(final String persistedValue, final boolean isDestructive,
                final int actionIcon, final int displayString) {
            mPersistedValue = persistedValue;
            mIsDestructive = isDestructive;
            mActionIcon = actionIcon;
            mActionIcon2 = -1;
            mDisplayString = displayString;
            mDisplayString2 = -1;
            mActionToggler = null;
        }

        private NotificationActionType(final String persistedValue, final boolean isDestructive,
                final int actionIcon, final int actionIcon2, final int displayString,
                final int displayString2, final ActionToggler actionToggler) {
            mPersistedValue = persistedValue;
            mIsDestructive = isDestructive;
            mActionIcon = actionIcon;
            mActionIcon2 = actionIcon2;
            mDisplayString = displayString;
            mDisplayString2 = displayString2;
            mActionToggler = actionToggler;
        }

        public static NotificationActionType getActionType(final String persistedValue) {
            return sPersistedMapping.get(persistedValue);
        }

        public String getPersistedValue() {
            return mPersistedValue;
        }

        public boolean getIsDestructive() {
            return mIsDestructive;
        }

        public int getActionIconResId(final Folder folder, final Conversation conversation,
                final Message message) {
            if (mActionToggler == null || mActionToggler.shouldDisplayPrimary(folder, conversation,
                    message)) {
                return mActionIcon;
            }

            return mActionIcon2;
        }

        public int getDisplayStringResId(final Folder folder, final Conversation conversation,
                final Message message) {
            if (mActionToggler == null || mActionToggler.shouldDisplayPrimary(folder, conversation,
                    message)) {
                return mDisplayString;
            }

            return mDisplayString2;
        }
    }

    /**
     * Adds the appropriate notification actions to the specified
     * {@link android.support.v4.app.NotificationCompat.Builder}
     *
     * @param notificationIntent The {@link Intent} used when the notification is clicked
     * @param when The value passed into {@link android.app.Notification.Builder#setWhen(long)}.
     *        This is used for maintaining notification ordering with the undo bar
     * @param notificationActions A {@link Set} set of the actions to display
     */
    public static void addNotificationActions(final Context context,
            final Intent notificationIntent, final NotificationCompat.Builder notification,
            final Account account, final Conversation conversation, final Message message,
            final Folder folder, final int notificationId, final long when,
            final Set<String> notificationActions) {
        final List<NotificationActionType> sortedActions =
                getSortedNotificationActions(folder, notificationActions);

        for (final NotificationActionType notificationAction : sortedActions) {
            notification.addAction(notificationAction.getActionIconResId(
                    folder, conversation, message), context.getString(notificationAction
                    .getDisplayStringResId(folder, conversation, message)),
                    getNotificationActionPendingIntent(context, account, conversation, message,
                            folder, notificationIntent, notificationAction, notificationId, when));
        }
    }

    /**
     * Sorts the notification actions into the appropriate order, based on current label
     *
     * @param folder The {@link Folder} being notified
     * @param notificationActionStrings The action strings to sort
     */
    private static List<NotificationActionType> getSortedNotificationActions(
            final Folder folder, final Collection<String> notificationActionStrings) {
        final List<NotificationActionType> unsortedActions =
                new ArrayList<NotificationActionType>(notificationActionStrings.size());
        for (final String action : notificationActionStrings) {
            unsortedActions.add(NotificationActionType.getActionType(action));
        }

        final List<NotificationActionType> sortedActions =
                new ArrayList<NotificationActionType>(unsortedActions.size());

        if (folder.isInbox()) {
            // Inbox
            /*
             * Action 1: Archive, Delete, Mute, Mark read, Add star, Mark important, Reply, Reply
             * all, Forward
             */
            /*
             * Action 2: Reply, Reply all, Forward, Mark important, Add star, Mark read, Mute,
             * Delete, Archive
             */
            if (unsortedActions.contains(NotificationActionType.ARCHIVE_REMOVE_LABEL)) {
                sortedActions.add(NotificationActionType.ARCHIVE_REMOVE_LABEL);
            }
            if (unsortedActions.contains(NotificationActionType.DELETE)) {
                sortedActions.add(NotificationActionType.DELETE);
            }
            if (unsortedActions.contains(NotificationActionType.REPLY)) {
                sortedActions.add(NotificationActionType.REPLY);
            }
            if (unsortedActions.contains(NotificationActionType.REPLY_ALL)) {
                sortedActions.add(NotificationActionType.REPLY_ALL);
            }
        } else if (folder.isProviderFolder()) {
            // Gmail system labels
            /*
             * Action 1: Delete, Mute, Mark read, Add star, Mark important, Reply, Reply all,
             * Forward
             */
            /*
             * Action 2: Reply, Reply all, Forward, Mark important, Add star, Mark read, Mute,
             * Delete
             */
            if (unsortedActions.contains(NotificationActionType.DELETE)) {
                sortedActions.add(NotificationActionType.DELETE);
            }
            if (unsortedActions.contains(NotificationActionType.REPLY)) {
                sortedActions.add(NotificationActionType.REPLY);
            }
            if (unsortedActions.contains(NotificationActionType.REPLY_ALL)) {
                sortedActions.add(NotificationActionType.REPLY_ALL);
            }
        } else {
            // Gmail user created labels
            /*
             * Action 1: Remove label, Delete, Mark read, Add star, Mark important, Reply, Reply
             * all, Forward
             */
            /*
             * Action 2: Reply, Reply all, Forward, Mark important, Add star, Mark read, Delete
             */
            if (unsortedActions.contains(NotificationActionType.ARCHIVE_REMOVE_LABEL)) {
                sortedActions.add(NotificationActionType.ARCHIVE_REMOVE_LABEL);
            }
            if (unsortedActions.contains(NotificationActionType.DELETE)) {
                sortedActions.add(NotificationActionType.DELETE);
            }
            if (unsortedActions.contains(NotificationActionType.REPLY)) {
                sortedActions.add(NotificationActionType.REPLY);
            }
            if (unsortedActions.contains(NotificationActionType.REPLY_ALL)) {
                sortedActions.add(NotificationActionType.REPLY_ALL);
            }
        }

        return sortedActions;
    }

    /**
     * Creates a {@link PendingIntent} for the specified notification action.
     */
    private static PendingIntent getNotificationActionPendingIntent(final Context context,
            final Account account, final Conversation conversation, final Message message,
            final Folder folder, final Intent notificationIntent,
            final NotificationActionType action, final int notificationId, final long when) {
        final Uri messageUri = message.uri;

        final NotificationAction notificationAction = new NotificationAction(action, account,
                conversation, message, folder, conversation.id, message.serverId, message.id, when);

        switch (action) {
            case REPLY: {
                // Build a task stack that forces the conversation view on the stack before the
                // reply activity.
                final TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);

                final Intent intent = createReplyIntent(context, account, messageUri, false);
                intent.setPackage(context.getPackageName());
                intent.putExtra(ComposeActivity.EXTRA_NOTIFICATION_FOLDER, folder);
                // To make sure that the reply intents one notification don't clobber over
                // intents for other notification, force a data uri on the intent
                final Uri notificationUri =
                        Uri.parse("mailfrom://mail/account/" + "reply/" + notificationId);
                intent.setData(notificationUri);

                taskStackBuilder.addNextIntent(notificationIntent).addNextIntent(intent);

                return taskStackBuilder.getPendingIntent(
                        notificationId, PendingIntent.FLAG_UPDATE_CURRENT);
            } case REPLY_ALL: {
                // Build a task stack that forces the conversation view on the stack before the
                // reply activity.
                final TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);

                final Intent intent = createReplyIntent(context, account, messageUri, true);
                intent.setPackage(context.getPackageName());
                intent.putExtra(ComposeActivity.EXTRA_NOTIFICATION_FOLDER, folder);
                // To make sure that the reply intents one notification don't clobber over
                // intents for other notification, force a data uri on the intent
                final Uri notificationUri =
                        Uri.parse("mailfrom://mail/account/" + "replyall/" + notificationId);
                intent.setData(notificationUri);

                taskStackBuilder.addNextIntent(notificationIntent).addNextIntent(intent);

                return taskStackBuilder.getPendingIntent(
                        notificationId, PendingIntent.FLAG_UPDATE_CURRENT);
            } case ARCHIVE_REMOVE_LABEL: {
                final String intentAction =
                        NotificationActionIntentService.ACTION_ARCHIVE_REMOVE_LABEL;

                final Intent intent = new Intent(intentAction);
                intent.setPackage(context.getPackageName());
                putNotificationActionExtra(intent, notificationAction);

                return PendingIntent.getService(
                        context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            } case DELETE: {
                final String intentAction = NotificationActionIntentService.ACTION_DELETE;

                final Intent intent = new Intent(intentAction);
                intent.setPackage(context.getPackageName());
                putNotificationActionExtra(intent, notificationAction);

                return PendingIntent.getService(
                        context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
        }

        throw new IllegalArgumentException("Invalid NotificationActionType");
    }

    /**
     * @return an intent which, if launched, will reply to the conversation
     */
    public static Intent createReplyIntent(final Context context, final Account account,
            final Uri messageUri, final boolean isReplyAll) {
        final Intent intent = ComposeActivity.createReplyIntent(context, account, messageUri,
                isReplyAll);
        intent.putExtra(Utils.EXTRA_FROM_NOTIFICATION, true);
        return intent;
    }

    /**
     * @return an intent which, if launched, will forward the conversation
     */
    public static Intent createForwardIntent(
            final Context context, final Account account, final Uri messageUri) {
        final Intent intent = ComposeActivity.createForwardIntent(context, account, messageUri);
        intent.putExtra(Utils.EXTRA_FROM_NOTIFICATION, true);
        return intent;
    }

    public static class NotificationAction implements Parcelable {
        private final NotificationActionType mNotificationActionType;
        private final Account mAccount;
        private final Conversation mConversation;
        private final Message mMessage;
        private final Folder mFolder;
        private final long mConversationId;
        private final String mMessageId;
        private final long mLocalMessageId;
        private final long mWhen;

        public NotificationAction(final NotificationActionType notificationActionType,
                final Account account, final Conversation conversation, final Message message,
                final Folder folder, final long conversationId, final String messageId,
                final long localMessageId, final long when) {
            mNotificationActionType = notificationActionType;
            mAccount = account;
            mConversation = conversation;
            mMessage = message;
            mFolder = folder;
            mConversationId = conversationId;
            mMessageId = messageId;
            mLocalMessageId = localMessageId;
            mWhen = when;
        }

        public NotificationActionType getNotificationActionType() {
            return mNotificationActionType;
        }

        public Account getAccount() {
            return mAccount;
        }

        public Conversation getConversation() {
            return mConversation;
        }

        public Message getMessage() {
            return mMessage;
        }

        public Folder getFolder() {
            return mFolder;
        }

        public long getConversationId() {
            return mConversationId;
        }

        public String getMessageId() {
            return mMessageId;
        }

        public long getLocalMessageId() {
            return mLocalMessageId;
        }

        public long getWhen() {
            return mWhen;
        }

        public int getActionTextResId() {
            switch (mNotificationActionType) {
                case ARCHIVE_REMOVE_LABEL:
                    if (mFolder.isInbox()) {
                        return R.string.notification_action_undo_archive;
                    } else {
                        return R.string.notification_action_undo_remove_label;
                    }
                case DELETE:
                    return R.string.notification_action_undo_delete;
                default:
                    throw new IllegalStateException(
                            "There is no action text for this NotificationActionType.");
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel out, final int flags) {
            out.writeInt(mNotificationActionType.ordinal());
            out.writeParcelable(mAccount, 0);
            out.writeParcelable(mConversation, 0);
            out.writeParcelable(mMessage, 0);
            out.writeParcelable(mFolder, 0);
            out.writeLong(mConversationId);
            out.writeString(mMessageId);
            out.writeLong(mLocalMessageId);
            out.writeLong(mWhen);
        }

        public static final Parcelable.ClassLoaderCreator<NotificationAction> CREATOR =
                new Parcelable.ClassLoaderCreator<NotificationAction>() {
                    @Override
                    public NotificationAction createFromParcel(final Parcel in) {
                        return new NotificationAction(in, null);
                    }

                    @Override
                    public NotificationAction[] newArray(final int size) {
                        return new NotificationAction[size];
                    }

                    @Override
                    public NotificationAction createFromParcel(
                            final Parcel in, final ClassLoader loader) {
                        return new NotificationAction(in, loader);
                    }
                };

        private NotificationAction(final Parcel in, final ClassLoader loader) {
            mNotificationActionType = NotificationActionType.values()[in.readInt()];
            mAccount = in.readParcelable(loader);
            mConversation = in.readParcelable(loader);
            mMessage = in.readParcelable(loader);
            mFolder = in.readParcelable(loader);
            mConversationId = in.readLong();
            mMessageId = in.readString();
            mLocalMessageId = in.readLong();
            mWhen = in.readLong();
        }
    }

    public static Notification createUndoNotification(final Context context,
            final NotificationAction notificationAction, final int notificationId) {
        LogUtils.i(LOG_TAG, "createUndoNotification for %s",
                notificationAction.getNotificationActionType());

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setSmallIcon(R.drawable.stat_notify_email);
        builder.setWhen(notificationAction.getWhen());

        final RemoteViews undoView =
                new RemoteViews(context.getPackageName(), R.layout.undo_notification);
        undoView.setTextViewText(
                R.id.description_text, context.getString(notificationAction.getActionTextResId()));

        final String packageName = context.getPackageName();

        final Intent clickIntent = new Intent(NotificationActionIntentService.ACTION_UNDO);
        clickIntent.setPackage(packageName);
        putNotificationActionExtra(clickIntent, notificationAction);
        final PendingIntent clickPendingIntent = PendingIntent.getService(context, notificationId,
                clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        undoView.setOnClickPendingIntent(R.id.status_bar_latest_event_content, clickPendingIntent);

        builder.setContent(undoView);

        // When the notification is cleared, we perform the destructive action
        final Intent deleteIntent = new Intent(NotificationActionIntentService.ACTION_DESTRUCT);
        deleteIntent.setPackage(packageName);
        putNotificationActionExtra(deleteIntent, notificationAction);
        final PendingIntent deletePendingIntent = PendingIntent.getService(context,
                notificationId, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setDeleteIntent(deletePendingIntent);

        final Notification notification = builder.build();

        return notification;
    }

    /**
     * Registers a timeout for the undo notification such that when it expires, the undo bar will
     * disappear, and the action will be performed.
     */
    public static void registerUndoTimeout(
            final Context context, final NotificationAction notificationAction) {
        LogUtils.i(LOG_TAG, "registerUndoTimeout for %s",
                notificationAction.getNotificationActionType());

        if (sUndoTimeoutMillis == -1) {
            sUndoTimeoutMillis =
                    context.getResources().getInteger(R.integer.undo_notification_timeout);
        }

        final AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        final long triggerAtMills = SystemClock.elapsedRealtime() + sUndoTimeoutMillis;

        final PendingIntent pendingIntent =
                createUndoTimeoutPendingIntent(context, notificationAction);

        alarmManager.set(AlarmManager.ELAPSED_REALTIME, triggerAtMills, pendingIntent);
    }

    /**
     * Cancels the undo timeout for a notification action. This should be called if the undo
     * notification is clicked (to prevent the action from being performed anyway) or cleared (since
     * we have already performed the action).
     */
    public static void cancelUndoTimeout(
            final Context context, final NotificationAction notificationAction) {
        LogUtils.i(LOG_TAG, "cancelUndoTimeout for %s",
                notificationAction.getNotificationActionType());

        final AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        final PendingIntent pendingIntent =
                createUndoTimeoutPendingIntent(context, notificationAction);

        alarmManager.cancel(pendingIntent);
    }

    /**
     * Creates a {@link PendingIntent} to be used for creating and canceling the undo timeout
     * alarm.
     */
    private static PendingIntent createUndoTimeoutPendingIntent(
            final Context context, final NotificationAction notificationAction) {
        final Intent intent = new Intent(NotificationActionIntentService.ACTION_UNDO_TIMEOUT);
        intent.setPackage(context.getPackageName());
        putNotificationActionExtra(intent, notificationAction);

        final int requestCode = notificationAction.getAccount().hashCode()
                ^ notificationAction.getFolder().hashCode();
        final PendingIntent pendingIntent =
                PendingIntent.getService(context, requestCode, intent, 0);

        return pendingIntent;
    }

    /**
     * Processes the specified destructive action (archive, delete, mute) on the message.
     */
    public static void processDestructiveAction(
            final Context context, final NotificationAction notificationAction) {
        LogUtils.i(LOG_TAG, "processDestructiveAction: %s",
                notificationAction.getNotificationActionType());

        final NotificationActionType destructAction =
                notificationAction.getNotificationActionType();
        final Conversation conversation = notificationAction.getConversation();
        final Folder folder = notificationAction.getFolder();

        final ContentResolver contentResolver = context.getContentResolver();
        final Uri uri = conversation.uri.buildUpon().appendQueryParameter(
                UIProvider.FORCE_UI_NOTIFICATIONS_QUERY_PARAMETER, Boolean.TRUE.toString()).build();

        switch (destructAction) {
            case ARCHIVE_REMOVE_LABEL: {
                if (folder.isInbox()) {
                    // Inbox, so archive
                    final ContentValues values = new ContentValues(1);
                    values.put(UIProvider.ConversationOperations.OPERATION_KEY,
                            UIProvider.ConversationOperations.ARCHIVE);

                    contentResolver.update(uri, values, null, null);
                } else {
                    // Not inbox, so remove label
                    final ContentValues values = new ContentValues(1);

                    final String removeFolderUri = folder.folderUri.fullUri.buildUpon()
                            .appendPath(Boolean.FALSE.toString()).toString();
                    values.put(ConversationOperations.FOLDERS_UPDATED, removeFolderUri);

                    contentResolver.update(uri, values, null, null);
                }
                break;
            }
            case DELETE: {
                contentResolver.delete(uri, null, null);
                break;
            }
            default:
                throw new IllegalArgumentException(
                        "The specified NotificationActionType is not a destructive action.");
        }
    }

    /**
     * Creates and displays an Undo notification for the specified {@link NotificationAction}.
     */
    public static void createUndoNotification(final Context context,
            final NotificationAction notificationAction) {
        LogUtils.i(LOG_TAG, "createUndoNotification for %s",
                notificationAction.getNotificationActionType());

        final int notificationId = NotificationUtils.getNotificationId(
                notificationAction.getAccount().getAccountManagerAccount(),
                notificationAction.getFolder());

        final Notification notification =
                createUndoNotification(context, notificationAction, notificationId);

        final NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);

        sUndoNotifications.put(notificationId, notificationAction);
        sNotificationTimestamps.put(notificationId, notificationAction.getWhen());
    }

    /**
     * Called when an Undo notification has been tapped.
     */
    public static void cancelUndoNotification(final Context context,
            final NotificationAction notificationAction) {
        LogUtils.i(LOG_TAG, "cancelUndoNotification for %s",
                notificationAction.getNotificationActionType());

        final Account account = notificationAction.getAccount();
        final Folder folder = notificationAction.getFolder();
        final Conversation conversation = notificationAction.getConversation();
        final int notificationId =
                NotificationUtils.getNotificationId(account.getAccountManagerAccount(), folder);

        // Note: we must add the conversation before removing the undo notification
        // Otherwise, the observer for sUndoNotifications gets called, which calls
        // handleNotificationActions before the undone conversation has been added to the set.
        sUndoneConversations.add(conversation);
        removeUndoNotification(context, notificationId, false);
        resendNotifications(context, account, folder);
    }

    /**
     * If an undo notification is left alone for a long enough time, it will disappear, this method
     * will be called, and the action will be finalized.
     */
    public static void processUndoNotification(final Context context,
            final NotificationAction notificationAction) {
        LogUtils.i(LOG_TAG, "processUndoNotification, %s",
                notificationAction.getNotificationActionType());

        final Account account = notificationAction.getAccount();
        final Folder folder = notificationAction.getFolder();
        final int notificationId = NotificationUtils.getNotificationId(
                account.getAccountManagerAccount(), folder);
        removeUndoNotification(context, notificationId, true);
        sNotificationTimestamps.delete(notificationId);
        processDestructiveAction(context, notificationAction);

        resendNotifications(context, account, folder);
    }

    /**
     * Removes the undo notification.
     *
     * @param removeNow <code>true</code> to remove it from the drawer right away,
     *        <code>false</code> to just remove the reference to it
     */
    private static void removeUndoNotification(
            final Context context, final int notificationId, final boolean removeNow) {
        sUndoNotifications.delete(notificationId);

        if (removeNow) {
            final NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }

    /**
     * Broadcasts an {@link Intent} to inform the app to resend its notifications.
     */
    public static void resendNotifications(final Context context, final Account account,
            final Folder folder) {
        LogUtils.i(LOG_TAG, "resendNotifications account: %s, folder: %s",
                LogUtils.sanitizeName(LOG_TAG, account.name),
                LogUtils.sanitizeName(LOG_TAG, folder.name));

        final Intent intent = new Intent(MailIntentService.ACTION_RESEND_NOTIFICATIONS);
        intent.setPackage(context.getPackageName()); // Make sure we only deliver this to ourself
        intent.putExtra(Utils.EXTRA_ACCOUNT_URI, account.uri);
        intent.putExtra(Utils.EXTRA_FOLDER_URI, folder.folderUri.fullUri);
        context.startService(intent);
    }

    public static void registerUndoNotificationObserver(final DataSetObserver observer) {
        sUndoNotifications.getDataSetObservable().registerObserver(observer);
    }

    public static void unregisterUndoNotificationObserver(final DataSetObserver observer) {
        sUndoNotifications.getDataSetObservable().unregisterObserver(observer);
    }

    /**
     * <p>
     * This is a slight hack to avoid an exception in the remote AlarmManagerService process. The
     * AlarmManager adds extra data to this Intent which causes it to inflate. Since the remote
     * process does not know about the NotificationAction class, it throws a ClassNotFoundException.
     * </p>
     * <p>
     * To avoid this, we marshall the data ourselves and then parcel a plain byte[] array. The
     * NotificationActionIntentService class knows to build the NotificationAction object from the
     * byte[] array.
     * </p>
     */
    private static void putNotificationActionExtra(final Intent intent,
            final NotificationAction notificationAction) {
        final Parcel out = Parcel.obtain();
        notificationAction.writeToParcel(out, 0);
        out.setDataPosition(0);
        intent.putExtra(NotificationActionIntentService.EXTRA_NOTIFICATION_ACTION, out.marshall());
    }
}
