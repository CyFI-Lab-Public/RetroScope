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
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.widget.RemoteViews;

public class NotificationTest extends AndroidTestCase {

    private Notification mNotification;
    private Context mContext;

    private static final String TICKER_TEXT = "tickerText";
    private static final String CONTENT_TITLE = "contentTitle";
    private static final String CONTENT_TEXT = "contentText";
    private static final String URI_STRING = "uriString";
    private static final int TOLERANCE = 200;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mNotification = new Notification();
    }

    public void testConstructor() {
        mNotification = null;
        mNotification = new Notification();
        assertNotNull(mNotification);
        assertTrue(System.currentTimeMillis() - mNotification.when < TOLERANCE);

        mNotification = null;
        final int notificationTime = 200;
        mNotification = new Notification(0, TICKER_TEXT, notificationTime);
        assertEquals(notificationTime, mNotification.when);
        assertEquals(0, mNotification.icon);
        assertEquals(TICKER_TEXT, mNotification.tickerText);
    }

    public void testDescribeContents() {
        final int expected = 0;
        mNotification = new Notification();
        assertEquals(expected, mNotification.describeContents());
    }

    public void testWriteToParcel() {
        mNotification = new Notification();
        mNotification.icon = 0;
        mNotification.number = 1;
        final Intent intent = new Intent();
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        mNotification.contentIntent = pendingIntent;
        final Intent deleteIntent = new Intent();
        final PendingIntent delPendingIntent = PendingIntent.getBroadcast(
                mContext, 0, deleteIntent, 0);
        mNotification.deleteIntent = delPendingIntent;
        mNotification.tickerText = TICKER_TEXT;

        final RemoteViews contentView = new RemoteViews(mContext.getPackageName(),
                com.android.internal.R.layout.status_bar_latest_event_content);
        mNotification.contentView = contentView;
        mNotification.defaults = 0;
        mNotification.flags = 0;
        final Uri uri = Uri.parse(URI_STRING);
        mNotification.sound = uri;
        mNotification.audioStreamType = 0;
        final long[] longArray = { 1l, 2l, 3l };
        mNotification.vibrate = longArray;
        mNotification.ledARGB = 0;
        mNotification.ledOnMS = 0;
        mNotification.ledOffMS = 0;
        mNotification.iconLevel = 0;
        Parcel parcel = Parcel.obtain();
        mNotification.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        // Test Notification(Parcel)
        Notification result = new Notification(parcel);
        assertEquals(mNotification.icon, result.icon);
        assertEquals(mNotification.when, result.when);
        assertEquals(mNotification.number, result.number);
        assertNotNull(result.contentIntent);
        assertNotNull(result.deleteIntent);
        assertEquals(mNotification.tickerText, result.tickerText);
        assertNotNull(result.contentView);
        assertEquals(mNotification.defaults, result.defaults);
        assertEquals(mNotification.flags, result.flags);
        assertNotNull(result.sound);
        assertEquals(mNotification.audioStreamType, result.audioStreamType);
        assertEquals(mNotification.vibrate[0], result.vibrate[0]);
        assertEquals(mNotification.vibrate[1], result.vibrate[1]);
        assertEquals(mNotification.vibrate[2], result.vibrate[2]);
        assertEquals(mNotification.ledARGB, result.ledARGB);
        assertEquals(mNotification.ledOnMS, result.ledOnMS);
        assertEquals(mNotification.ledOffMS, result.ledOffMS);
        assertEquals(mNotification.iconLevel, result.iconLevel);

        mNotification.contentIntent = null;
        parcel = Parcel.obtain();
        mNotification.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        result = new Notification(parcel);
        assertNull(result.contentIntent);

        mNotification.deleteIntent = null;
        parcel = Parcel.obtain();
        mNotification.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        result = new Notification(parcel);
        assertNull(result.deleteIntent);

        mNotification.tickerText = null;
        parcel = Parcel.obtain();
        mNotification.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        result = new Notification(parcel);
        assertNull(result.tickerText);

        mNotification.contentView = null;
        parcel = Parcel.obtain();
        mNotification.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        result = new Notification(parcel);
        assertNull(result.contentView);

        mNotification.sound = null;
        parcel = Parcel.obtain();
        mNotification.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        result = new Notification(parcel);
        assertNull(result.sound);
    }

    public void testSetLatestEventInfo() {
        mNotification = new Notification();
        mNotification.icon = 1;
        final Intent intent = new Intent();
        final PendingIntent contentIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        mNotification.setLatestEventInfo(mContext, CONTENT_TITLE, CONTENT_TEXT, contentIntent);
        assertTrue(mNotification.contentView instanceof RemoteViews);
        assertNotNull(mNotification.contentView);
    }

    public void testToString() {
        mNotification = new Notification();
        assertNotNull(mNotification.toString());
    }
}
