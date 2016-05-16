/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.providers.contacts;

import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.ContactsContract.RawContacts;
import android.test.suitebuilder.annotation.MediumTest;

import com.android.internal.telephony.CallerInfo;
import com.android.providers.contacts.testutil.DataUtil;

/**
 * Integration test for {@link CallerInfo} and {@link ContactsProvider2}.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.android.providers.contacts.CallerInfoIntegrationTest -w \
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@MediumTest
public class CallerInfoIntegrationTest extends BaseContactsProvider2Test {

    public void testCallerInfo() {
        ContentValues values = new ContentValues();
        values.put(RawContacts.CUSTOM_RINGTONE, "ring");
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);

        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);

        DataUtil.insertStructuredName(mResolver, rawContactId, "Hot", "Tamale");
        insertPhoneNumber(rawContactId, "800-466-4411");

        CallerInfo callerInfo = CallerInfo.getCallerInfo(getProvider().getContext(), "18004664411");
        assertEquals("800-466-4411", callerInfo.phoneNumber);
        assertEquals("Home", callerInfo.phoneLabel);
        assertEquals("Hot Tamale", callerInfo.name);
        assertEquals("ring", String.valueOf(callerInfo.contactRingtoneUri));
        assertEquals(true, callerInfo.shouldSendToVoicemail);
        assertEquals("content://com.android.contacts/phone_lookup/18004664411",
                String.valueOf(callerInfo.contactRefUri));
    }
}
