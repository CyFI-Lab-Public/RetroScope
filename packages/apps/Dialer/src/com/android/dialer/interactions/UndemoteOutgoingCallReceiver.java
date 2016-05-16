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

package com.android.dialer.interactions;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.PinnedPositions;
import android.text.TextUtils;

/**
 * This broadcast receiver is used to listen to outgoing calls and undemote formerly demoted
 * contacts if a phone call is made to a phone number belonging to that contact.
 */
public class UndemoteOutgoingCallReceiver extends BroadcastReceiver {

    private static final long NO_CONTACT_FOUND = -1;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent != null && Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
            final String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            if (TextUtils.isEmpty(number)) {
                return;
            }
            final long id = getContactIdFromPhoneNumber(context, number);
            if (id != NO_CONTACT_FOUND) {
                final Thread thread = new Thread() {
                    @Override
                    public void run() {
                        undemoteContactWithId(context, id);
                    }
                };
                thread.start();
            }
        }
    }

    private void undemoteContactWithId(Context context, long id) {
        final ContentValues cv = new ContentValues(1);
        cv.put(String.valueOf(id), PinnedPositions.UNDEMOTE);
        // If the contact is not demoted, this will not do anything. Otherwise, it will
        // restore it to an unpinned position. If it was a frequently called contact, it will
        // show up once again show up on the favorites screen.
        context.getContentResolver().update(PinnedPositions.UPDATE_URI, cv, null, null);
    }

    private long getContactIdFromPhoneNumber(Context context, String number) {
        final Uri contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        final Cursor cursor = context.getContentResolver().query(contactUri, new String[] {
                PhoneLookup._ID}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                final long id = cursor.getLong(0);
                return id;
            } else {
                return NO_CONTACT_FOUND;
            }
        } finally {
            cursor.close();
        }
    }
}
