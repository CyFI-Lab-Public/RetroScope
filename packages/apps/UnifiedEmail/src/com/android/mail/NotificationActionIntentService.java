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
package com.android.mail;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;

import com.android.mail.analytics.Analytics;
import com.android.mail.providers.Message;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.NotificationActionUtils;
import com.android.mail.utils.NotificationActionUtils.NotificationAction;

/**
 * Processes notification action {@link Intent}s that need to run off the main thread.
 */
public class NotificationActionIntentService extends IntentService {
    private static final String LOG_TAG = "NotifActionIS";

    // Compose actions
    public static final String ACTION_REPLY = "com.android.mail.action.notification.REPLY";
    public static final String ACTION_REPLY_ALL = "com.android.mail.action.notification.REPLY_ALL";
    public static final String ACTION_FORWARD = "com.android.mail.action.notification.FORWARD";
    // Toggle actions
    public static final String ACTION_MARK_READ = "com.android.mail.action.notification.MARK_READ";

    // Destructive actions - These just display the undo bar
    public static final String ACTION_ARCHIVE_REMOVE_LABEL =
            "com.android.mail.action.notification.ARCHIVE";
    public static final String ACTION_DELETE = "com.android.mail.action.notification.DELETE";

    /**
     * This action cancels the undo notification, and does not commit any changes.
     */
    public static final String ACTION_UNDO = "com.android.mail.action.notification.UNDO";

    /**
     * This action performs the actual destructive action.
     */
    public static final String ACTION_DESTRUCT = "com.android.mail.action.notification.DESTRUCT";

    public static final String EXTRA_NOTIFICATION_ACTION =
            "com.android.mail.extra.EXTRA_NOTIFICATION_ACTION";
    public static final String ACTION_UNDO_TIMEOUT =
            "com.android.mail.action.notification.UNDO_TIMEOUT";

    public NotificationActionIntentService() {
        super("NotificationActionIntentService");
    }

    private static void logNotificationAction(String intentAction, NotificationAction action) {
        final String eventAction;
        final String eventLabel;

        if (ACTION_ARCHIVE_REMOVE_LABEL.equals(intentAction)) {
            eventAction = "archive_remove_label";
            eventLabel = action.getFolder().getTypeDescription();
        } else if (ACTION_DELETE.equals(intentAction)) {
            eventAction = "delete";
            eventLabel = null;
        } else {
            eventAction = intentAction;
            eventLabel = null;
        }

        Analytics.getInstance().sendEvent("notification_action", eventAction, eventLabel, 0);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final Context context = this;
        final String action = intent.getAction();

        /*
         * Grab the alarm from the intent. Since the remote AlarmManagerService fills in the Intent
         * to add some extra data, it must unparcel the NotificationAction object. It throws a
         * ClassNotFoundException when unparcelling.
         * To avoid this, do the marshalling ourselves.
         */
        final NotificationAction notificationAction;
        final byte[] data = intent.getByteArrayExtra(EXTRA_NOTIFICATION_ACTION);
        if (data != null) {
            final Parcel in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            notificationAction = NotificationAction.CREATOR.createFromParcel(in,
                    NotificationAction.class.getClassLoader());
        } else {
            LogUtils.wtf(LOG_TAG, "data was null trying to unparcel the NotificationAction");
            return;
        }

        final Message message = notificationAction.getMessage();

        final ContentResolver contentResolver = getContentResolver();

        LogUtils.i(LOG_TAG, "Handling %s", action);

        logNotificationAction(action, notificationAction);

        if (ACTION_UNDO.equals(action)) {
            NotificationActionUtils.cancelUndoTimeout(context, notificationAction);
            NotificationActionUtils.cancelUndoNotification(context, notificationAction);
        } else if (ACTION_ARCHIVE_REMOVE_LABEL.equals(action) || ACTION_DELETE.equals(action)) {
            // All we need to do is switch to an Undo notification
            NotificationActionUtils.createUndoNotification(context, notificationAction);

            NotificationActionUtils.registerUndoTimeout(context, notificationAction);
        } else {
            if (ACTION_UNDO_TIMEOUT.equals(action) || ACTION_DESTRUCT.equals(action)) {
                // Process the action
                NotificationActionUtils.cancelUndoTimeout(this, notificationAction);
                NotificationActionUtils.processUndoNotification(this, notificationAction);
            } else if (ACTION_MARK_READ.equals(action)) {
                final Uri uri = message.uri;

                final ContentValues values = new ContentValues(1);
                values.put(UIProvider.MessageColumns.READ, 1);

                contentResolver.update(uri, values, null, null);
            }

            NotificationActionUtils.resendNotifications(context, notificationAction.getAccount(),
                    notificationAction.getFolder());
        }
    }
}
