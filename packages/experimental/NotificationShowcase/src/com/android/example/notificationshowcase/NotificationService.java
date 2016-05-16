/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.example.notificationshowcase;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;

import java.util.ArrayList;

public class NotificationService extends IntentService {

    private static final String TAG = "NotificationService";

    public static final String ACTION_CREATE = "create";
    public static final int NOTIFICATION_ID = 31338;

    public NotificationService() {
        super(TAG);
    }

    public NotificationService(String name) {
        super(name);
    }

    private static Bitmap getBitmap(Context context, int resId) {
        int largeIconWidth = (int) context.getResources()
                .getDimension(R.dimen.notification_large_icon_width);
        int largeIconHeight = (int) context.getResources()
                .getDimension(R.dimen.notification_large_icon_height);
        Drawable d = context.getResources().getDrawable(resId);
        Bitmap b = Bitmap.createBitmap(largeIconWidth, largeIconHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, largeIconWidth, largeIconHeight);
        d.draw(c);
        return b;
    }

    private static PendingIntent makeEmailIntent(Context context, String who) {
        final Intent intent = new Intent(android.content.Intent.ACTION_SENDTO,
                Uri.parse("mailto:" + who));
        return PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static Notification makeBigTextNotification(Context context, int update, int id,
            long when) {
        String addendum = update > 0 ? "(updated) " : "";
        String longSmsText = "Hey, looks like\nI'm getting kicked out of this conference" +
                " room";
        if (update > 1) {
            longSmsText += ", so stay in the hangout and I'll rejoin in about 5-10 minutes" +
                    ". If you don't see me, assume I got pulled into another meeting. And" +
                    " now \u2026 I have to find my shoes.";
        }
        if (update > 2) {
            when = System.currentTimeMillis();
        }
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.bigText(addendum + longSmsText);
        NotificationCompat.Builder bigTextNotification = new NotificationCompat.Builder(context)
                .setContentTitle(addendum + "Mike Cleron")
                .setContentIntent(ToastService.getPendingIntent(context, "Clicked on bigText"))
                .setContentText(addendum + longSmsText)
                .setTicker(addendum + "Mike Cleron: " + longSmsText)
                .setWhen(when)
                .setLargeIcon(getBitmap(context, R.drawable.bucket))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_media_next,
                        "update: " + update,
                        UpdateService.getPendingIntent(context, update + 1, id, when))
                .setSmallIcon(R.drawable.stat_notify_talk_text)
                .setStyle(bigTextStyle);
        return bigTextNotification.build();
    }

    public static Notification makeUploadNotification(Context context, int progress, long when) {
        NotificationCompat.Builder uploadNotification = new NotificationCompat.Builder(context)
                .setContentTitle("File Upload")
                .setContentText("foo.txt")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(ToastService.getPendingIntent(context, "Clicked on Upload"))
                .setWhen(when)
                .setSmallIcon(R.drawable.ic_menu_upload)
                .setProgress(100, Math.min(progress, 100), false);
        return uploadNotification.build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ArrayList<Notification> mNotifications = new ArrayList<Notification>();
        NotificationManager noMa =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int bigtextId = mNotifications.size();
        mNotifications.add(makeBigTextNotification(this, 0, bigtextId, System.currentTimeMillis()));

        int uploadId = mNotifications.size();
        long uploadWhen = System.currentTimeMillis();
        mNotifications.add(makeUploadNotification(this, 10, uploadWhen));

        Notification phoneCall = new NotificationCompat.Builder(this)
                .setContentTitle("Incoming call")
                .setContentText("Matias Duarte")
                .setLargeIcon(getBitmap(this, R.drawable.matias_hed))
                .setSmallIcon(R.drawable.stat_sys_phone_call)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(ToastService.getPendingIntent(this, "Clicked on Matias"))
                .addAction(R.drawable.ic_dial_action_call, "Answer",
                        ToastService.getPendingIntent(this, "call answered"))
                .addAction(R.drawable.ic_end_call, "Ignore",
                        ToastService.getPendingIntent(this, "call ignored"))
                .setAutoCancel(true)
                .build();
        phoneCall.flags |= Notification.FLAG_INSISTENT;
        mNotifications.add(phoneCall);

        mNotifications.add(new NotificationCompat.Builder(this)
                .setContentTitle("Stopwatch PRO")
                .setContentText("Counting up")
                .setContentIntent(ToastService.getPendingIntent(this, "Clicked on Stopwatch"))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setUsesChronometer(true)
                .build());

        mNotifications.add(new NotificationCompat.Builder(this)
                .setContentTitle("J Planning")
                .setContentText("The Botcave")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.stat_notify_calendar)
                .setContentIntent(ToastService.getPendingIntent(this, "Clicked on calendar event"))
                .setContentInfo("7PM")
                .addAction(R.drawable.stat_notify_snooze, "+10 min",
                        ToastService.getPendingIntent(this, "snoozed 10 min"))
                .addAction(R.drawable.stat_notify_snooze_longer, "+1 hour",
                        ToastService.getPendingIntent(this, "snoozed 1 hr"))
                .addAction(R.drawable.stat_notify_email, "Email",
                        makeEmailIntent(this,
                                "gabec@example.com,mcleron@example.com,dsandler@example.com"))
                .build());

        BitmapDrawable d =
                (BitmapDrawable) getResources().getDrawable(R.drawable.romainguy_rockaway);
        mNotifications.add(new NotificationCompat.BigPictureStyle(
                new NotificationCompat.Builder(this)
                        .setContentTitle("Romain Guy")
                        .setContentText("I was lucky to find a Canon 5D Mk III at a local Bay Area "
                                + "store last week but I had not been able to try it in the field "
                                + "until tonight. After a few days of rain the sky finally cleared "
                                + "up. Rockaway Beach did not disappoint and I was finally able to "
                                + "see what my new camera feels like when shooting landscapes.")
                        .setSmallIcon(R.drawable.ic_stat_gplus)
                        .setContentIntent(
                                ToastService.getPendingIntent(this, "Clicked on bigPicture"))
                        .setLargeIcon(getBitmap(this, R.drawable.romainguy_hed))
                        .addAction(R.drawable.add, "Add to Gallery",
                                ToastService.getPendingIntent(this, "added! (just kidding)"))
                        .setSubText("talk rocks!"))
                .bigPicture(d.getBitmap())
                .build());

        // Note: this may conflict with real email notifications
        StyleSpan bold = new StyleSpan(Typeface.BOLD);
        SpannableString line1 = new SpannableString("Alice: hey there!");
        line1.setSpan(bold, 0, 5, 0);
        SpannableString line2 = new SpannableString("Bob: hi there!");
        line2.setSpan(bold, 0, 3, 0);
        SpannableString line3 = new SpannableString("Charlie: Iz IN UR EMAILZ!!");
        line3.setSpan(bold, 0, 7, 0);
        mNotifications.add(new NotificationCompat.InboxStyle(
                new NotificationCompat.Builder(this)
                        .setContentTitle("24 new messages")
                        .setContentText("You have mail!")
                        .setSubText("test.hugo2@gmail.com")
                        .setContentIntent(ToastService.getPendingIntent(this, "Clicked on Email"))
                        .setSmallIcon(R.drawable.stat_notify_email))
                .setSummaryText("+21 more")
                .addLine(line1)
                .addLine(line2)
                .addLine(line3)
                .build());

        mNotifications.add(new NotificationCompat.Builder(this)
                .setContentTitle("Twitter")
                .setContentText("New mentions")
                .setContentIntent(ToastService.getPendingIntent(this, "Clicked on Twitter"))
                .setSmallIcon(R.drawable.twitter_icon)
                .setNumber(15)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build());


        for (int i=0; i<mNotifications.size(); i++) {
            noMa.notify(NOTIFICATION_ID + i, mNotifications.get(i));
        }

        ProgressService.startProgressUpdater(this, uploadId, uploadWhen, 0);
    }
}
