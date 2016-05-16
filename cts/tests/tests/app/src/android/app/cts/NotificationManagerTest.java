/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.app.cts;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony.Threads;
import android.test.AndroidTestCase;

import com.android.cts.stub.R;


public class NotificationManagerTest extends AndroidTestCase {

    private NotificationManager mNotificationManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mNotificationManager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mNotificationManager.cancelAll();
    }

    public void testNotify() {
        final int id = 1;
        sendNotification(id, R.drawable.black);
    }

    public void testCancel() {
        final int id = 9;
        sendNotification(id, R.drawable.black);
        mNotificationManager.cancel(id);
    }

    public void testCancelAll() {
        sendNotification(1, R.drawable.black);
        sendNotification(2, R.drawable.blue);
        sendNotification(3, R.drawable.yellow);
        mNotificationManager.cancelAll();
    }

    private void sendNotification(final int id, final int icon) {
        final Notification notification = new Notification(
                icon, "No intent", System.currentTimeMillis());

        final Intent intent = new Intent(Intent.ACTION_MAIN, Threads.CONTENT_URI);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Intent.ACTION_MAIN);

        final PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        notification.setLatestEventInfo(mContext, "notify#" + id, "This is #" + id
                + "notification  ", pendingIntent);
        mNotificationManager.notify(id, notification);
    }

}
