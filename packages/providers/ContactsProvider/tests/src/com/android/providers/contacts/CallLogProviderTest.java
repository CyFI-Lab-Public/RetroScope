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

import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.PhoneConstants;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.VoicemailContract.Voicemails;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link CallLogProvider}.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.android.providers.contacts.CallLogProviderTest -w \
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@MediumTest
public class CallLogProviderTest extends BaseContactsProvider2Test {
    private static final String READ_WRITE_ALL_PERMISSION =
            "com.android.voicemail.permission.READ_WRITE_ALL_VOICEMAIL";
    private static final String ADD_VOICEMAIL_PERMISSION =
            "com.android.voicemail.permission.ADD_VOICEMAIL";

    /** Fields specific to voicemail provider that should not be exposed by call_log*/
    private static final String[] VOICEMAIL_PROVIDER_SPECIFIC_COLUMNS = new String[] {
            Voicemails._DATA,
            Voicemails.HAS_CONTENT,
            Voicemails.MIME_TYPE,
            Voicemails.SOURCE_PACKAGE,
            Voicemails.SOURCE_DATA,
            Voicemails.STATE};
    /** Total number of columns exposed by call_log provider. */
    private static final int NUM_CALLLOG_FIELDS = 19;

    @Override
    protected Class<? extends ContentProvider> getProviderClass() {
       return SynchronousContactsProvider2.class;
    }

    @Override
    protected String getAuthority() {
        return ContactsContract.AUTHORITY;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        addProvider(TestCallLogProvider.class, CallLog.AUTHORITY);
    }

    @Override
    protected void tearDown() throws Exception {
        setUpWithVoicemailPermissions();
        mResolver.delete(Calls.CONTENT_URI_WITH_VOICEMAIL, null, null);
        super.tearDown();
    }

    public void testInsert_RegularCallRecord() {
        ContentValues values = getDefaultCallValues();
        Uri uri = mResolver.insert(Calls.CONTENT_URI, values);
        values.put(Calls.COUNTRY_ISO, "us");
        assertStoredValues(uri, values);
        assertSelection(uri, values, Calls._ID, ContentUris.parseId(uri));
    }

    private void setUpWithVoicemailPermissions() {
        mActor.addPermissions(ADD_VOICEMAIL_PERMISSION);
        mActor.addPermissions(READ_WRITE_ALL_PERMISSION);
    }

    private void setUpWithNoVoicemailPermissions() {
        mActor.removePermissions(ADD_VOICEMAIL_PERMISSION);
        mActor.removePermissions(READ_WRITE_ALL_PERMISSION);
    }

    public void testInsert_VoicemailCallRecord() {
        setUpWithVoicemailPermissions();
        final ContentValues values = getDefaultCallValues();
        values.put(Calls.TYPE, Calls.VOICEMAIL_TYPE);
        values.put(Calls.VOICEMAIL_URI, "content://foo/voicemail/2");

        // Should fail with the base content uri without the voicemail param.
        EvenMoreAsserts.assertThrows(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.insert(Calls.CONTENT_URI, values);
            }
        });

        // Now grant voicemail permission - should succeed.
        Uri uri  = mResolver.insert(Calls.CONTENT_URI_WITH_VOICEMAIL, values);
        assertStoredValues(uri, values);
        assertSelection(uri, values, Calls._ID, ContentUris.parseId(uri));
    }

    public void testUpdate() {
        Uri uri = insertCallRecord();
        ContentValues values = new ContentValues();
        values.put(Calls.TYPE, Calls.OUTGOING_TYPE);
        values.put(Calls.NUMBER, "1-800-263-7643");
        values.put(Calls.NUMBER_PRESENTATION, Calls.PRESENTATION_ALLOWED);
        values.put(Calls.DATE, 2000);
        values.put(Calls.DURATION, 40);
        values.put(Calls.CACHED_NAME, "1-800-GOOG-411");
        values.put(Calls.CACHED_NUMBER_TYPE, Phone.TYPE_CUSTOM);
        values.put(Calls.CACHED_NUMBER_LABEL, "Directory");

        int count = mResolver.update(uri, values, null, null);
        assertEquals(1, count);
        assertStoredValues(uri, values);
    }

    public void testDelete() {
        Uri uri = insertCallRecord();
        try {
            mResolver.delete(uri, null, null);
            fail();
        } catch (UnsupportedOperationException ex) {
            // Expected
        }

        int count = mResolver.delete(Calls.CONTENT_URI, Calls._ID + "="
                + ContentUris.parseId(uri), null);
        assertEquals(1, count);
        assertEquals(0, getCount(uri, null, null));
    }

    public void testCallLogFilter() {
        ContentValues values = getDefaultCallValues();
        mResolver.insert(Calls.CONTENT_URI, values);

        Uri filterUri = Uri.withAppendedPath(Calls.CONTENT_FILTER_URI, "1-800-4664-411");
        Cursor c = mResolver.query(filterUri, null, null, null, null);
        assertEquals(1, c.getCount());
        c.moveToFirst();
        assertCursorValues(c, values);
        c.close();

        filterUri = Uri.withAppendedPath(Calls.CONTENT_FILTER_URI, "1-888-4664-411");
        c = mResolver.query(filterUri, null, null, null, null);
        assertEquals(0, c.getCount());
        c.close();
    }

    public void testAddCall() {
        CallerInfo ci = new CallerInfo();
        ci.name = "1-800-GOOG-411";
        ci.numberType = Phone.TYPE_CUSTOM;
        ci.numberLabel = "Directory";
        Uri uri = Calls.addCall(ci, getMockContext(), "1-800-263-7643",
                PhoneConstants.PRESENTATION_ALLOWED, Calls.OUTGOING_TYPE, 2000, 40);

        ContentValues values = new ContentValues();
        values.put(Calls.TYPE, Calls.OUTGOING_TYPE);
        values.put(Calls.NUMBER, "1-800-263-7643");
        values.put(Calls.NUMBER_PRESENTATION, Calls.PRESENTATION_ALLOWED);
        values.put(Calls.DATE, 2000);
        values.put(Calls.DURATION, 40);
        values.put(Calls.CACHED_NAME, "1-800-GOOG-411");
        values.put(Calls.CACHED_NUMBER_TYPE, Phone.TYPE_CUSTOM);
        values.put(Calls.CACHED_NUMBER_LABEL, "Directory");
        values.put(Calls.COUNTRY_ISO, "us");
        values.put(Calls.GEOCODED_LOCATION, "usa");
        assertStoredValues(uri, values);
    }

    // Test to check that the calls and voicemail uris returns expected results.
    public void testDifferentContentUris() {
        setUpWithVoicemailPermissions();
        // Insert one voicemaail and two regular call record.
        insertVoicemailRecord();
        insertCallRecord();
        insertCallRecord();

        // With the default uri, only 2 call entries should be returned.
        // With the voicemail uri all 3 should be returned.
        assertEquals(2, getCount(Calls.CONTENT_URI, null, null));
        assertEquals(3, getCount(Calls.CONTENT_URI_WITH_VOICEMAIL, null, null));
    }

    public void testLimitParamReturnsCorrectLimit() {
        for (int i=0; i<10; i++) {
            insertCallRecord();
        }
        Uri uri = Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter(Calls.LIMIT_PARAM_KEY, "4")
                .build();
        assertEquals(4, getCount(uri, null, null));
    }

    public void testLimitAndOffsetParamReturnsCorrectEntries() {
        for (int i=0; i<10; i++) {
            mResolver.insert(Calls.CONTENT_URI, getDefaultValues(Calls.INCOMING_TYPE));
        }
        for (int i=0; i<10; i++) {
            mResolver.insert(Calls.CONTENT_URI, getDefaultValues(Calls.MISSED_TYPE));
        }
        // Limit 4 records.  Discard first 8.
        Uri uri = Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter(Calls.LIMIT_PARAM_KEY, "4")
                .appendQueryParameter(Calls.OFFSET_PARAM_KEY, "8")
                .build();
        String[] projection = new String[] {Calls._ID, Calls.TYPE};
        Cursor c = mResolver.query(uri, projection, null, null, null);
        try {
            // First two should be incoming, next two should be missed.
            for (int i = 0; i < 2; i++) {
                c.moveToNext();
                assertEquals(Calls.INCOMING_TYPE, c.getInt(1));
            }
            for (int i = 0; i < 2; i++) {
                c.moveToNext();
                assertEquals(Calls.MISSED_TYPE, c.getInt(1));
            }
        } finally {
            c.close();
        }
    }

    public void testUriWithBadLimitParamThrowsException() {
        assertParamThrowsIllegalArgumentException(Calls.LIMIT_PARAM_KEY, "notvalid");
    }

    public void testUriWithBadOffsetParamThrowsException() {
        assertParamThrowsIllegalArgumentException(Calls.OFFSET_PARAM_KEY, "notvalid");
    }

    private void assertParamThrowsIllegalArgumentException(String key, String value) {
        Uri uri = Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter(key, value)
                .build();
        try {
            mResolver.query(uri, null, null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue("Error does not contain value in question.",
                    e.toString().contains(value));
        }
    }

    // Test to check that none of the voicemail provider specific fields are
    // insertable through call_log provider.
    public void testCannotAccessVoicemailSpecificFields_Insert() {
        for (String voicemailColumn : VOICEMAIL_PROVIDER_SPECIFIC_COLUMNS) {
            final ContentValues values = getDefaultCallValues();
            values.put(voicemailColumn, "foo");
            EvenMoreAsserts.assertThrows("Column: " + voicemailColumn,
                    IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        mResolver.insert(Calls.CONTENT_URI, values);
                    }
                });
        }
    }

    // Test to check that none of the voicemail provider specific fields are
    // exposed through call_log provider query.
    public void testCannotAccessVoicemailSpecificFields_Query() {
        // Query.
        Cursor cursor = mResolver.query(Calls.CONTENT_URI, null, null, null, null);
        List<String> columnNames = Arrays.asList(cursor.getColumnNames());
        assertEquals(NUM_CALLLOG_FIELDS, columnNames.size());
        // None of the voicemail provider specific columns should be present.
        for (String voicemailColumn : VOICEMAIL_PROVIDER_SPECIFIC_COLUMNS) {
            assertFalse("Unexpected column: '" + voicemailColumn + "' returned.",
                    columnNames.contains(voicemailColumn));
        }
    }

    // Test to check that none of the voicemail provider specific fields are
    // updatable through call_log provider.
    public void testCannotAccessVoicemailSpecificFields_Update() {
        for (String voicemailColumn : VOICEMAIL_PROVIDER_SPECIFIC_COLUMNS) {
            final Uri insertedUri = insertCallRecord();
            final ContentValues values = new ContentValues();
            values.put(voicemailColumn, "foo");
            EvenMoreAsserts.assertThrows("Column: " + voicemailColumn,
                    IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        mResolver.update(insertedUri, values, null, null);
                    }
                });
        }
    }

    public void testVoicemailPermissions_Insert() {
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.insert(Calls.CONTENT_URI_WITH_VOICEMAIL, getDefaultVoicemailValues());
            }
        });
        // Should now succeed with permissions granted.
        setUpWithVoicemailPermissions();
        mResolver.insert(Calls.CONTENT_URI_WITH_VOICEMAIL, getDefaultVoicemailValues());
    }

    public void testVoicemailPermissions_Update() {
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.update(Calls.CONTENT_URI_WITH_VOICEMAIL, getDefaultVoicemailValues(),
                        null, null);
            }
        });
        // Should now succeed with permissions granted.
        setUpWithVoicemailPermissions();
        mResolver.update(Calls.CONTENT_URI_WITH_VOICEMAIL, getDefaultCallValues(), null, null);
    }

    public void testVoicemailPermissions_Query() {
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.query(Calls.CONTENT_URI_WITH_VOICEMAIL, null, null, null, null);
            }
        });
        // Should now succeed with permissions granted.
        setUpWithVoicemailPermissions();
        mResolver.query(Calls.CONTENT_URI_WITH_VOICEMAIL, null, null, null, null);
    }

    public void testVoicemailPermissions_Delete() {
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.delete(Calls.CONTENT_URI_WITH_VOICEMAIL, null, null);
            }
        });
        // Should now succeed with permissions granted.
        setUpWithVoicemailPermissions();
        mResolver.delete(Calls.CONTENT_URI_WITH_VOICEMAIL, null, null);
    }

    private ContentValues getDefaultValues(int callType) {
        ContentValues values = new ContentValues();
        values.put(Calls.TYPE, callType);
        values.put(Calls.NUMBER, "1-800-4664-411");
        values.put(Calls.NUMBER_PRESENTATION, Calls.PRESENTATION_ALLOWED);
        values.put(Calls.DATE, 1000);
        values.put(Calls.DURATION, 30);
        values.put(Calls.NEW, 1);
        return values;
    }

    private ContentValues getDefaultCallValues() {
        return getDefaultValues(Calls.INCOMING_TYPE);
    }

    private ContentValues getDefaultVoicemailValues() {
        return getDefaultValues(Calls.VOICEMAIL_TYPE);
    }

    private Uri insertCallRecord() {
        return mResolver.insert(Calls.CONTENT_URI, getDefaultCallValues());
    }

    private Uri insertVoicemailRecord() {
        return mResolver.insert(Calls.CONTENT_URI_WITH_VOICEMAIL, getDefaultVoicemailValues());
    }

    public static class TestCallLogProvider extends CallLogProvider {
        private static ContactsDatabaseHelper mDbHelper;

        @Override
        protected ContactsDatabaseHelper getDatabaseHelper(final Context context) {
            if (mDbHelper == null) {
                mDbHelper = ContactsDatabaseHelper.getNewInstanceForTest(context);
            }
            return mDbHelper;
        }

        @Override
        protected CallLogInsertionHelper createCallLogInsertionHelper(Context context) {
            return new CallLogInsertionHelper() {
                @Override
                public String getGeocodedLocationFor(String number, String countryIso) {
                    return "usa";
                }

                @Override
                public void addComputedValues(ContentValues values) {
                    values.put(Calls.COUNTRY_ISO, "us");
                    values.put(Calls.GEOCODED_LOCATION, "usa");
                }
            };
        }

        @Override
        protected Context context() {
            return new ContextWrapper(super.context()) {
                @Override
                public PackageManager getPackageManager() {
                    return new MockPackageManager("com.test.package1", "com.test.package2");
                }

                @Override
                public void sendBroadcast(Intent intent, String receiverPermission) {
                   // Do nothing for now.
                }
            };
        }
    }
}
