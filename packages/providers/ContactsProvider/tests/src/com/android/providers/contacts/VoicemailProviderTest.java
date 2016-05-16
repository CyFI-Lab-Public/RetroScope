/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.providers.contacts;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.CallLog.Calls;
import android.provider.VoicemailContract;
import android.provider.VoicemailContract.Status;
import android.provider.VoicemailContract.Voicemails;
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.common.io.MoreCloseables;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link VoicemailContentProvider}.
 *
 * Run the test like this:
 * <code>
 * runtest -c com.android.providers.contacts.VoicemailProviderTest contactsprov
 * </code>
 */
// TODO: Test that calltype and voicemail_uri are auto populated by the provider.
@SmallTest
public class VoicemailProviderTest extends BaseVoicemailProviderTest {
    /** Fields specific to call_log provider that should not be exposed by voicemail provider. */
    private static final String[] CALLLOG_PROVIDER_SPECIFIC_COLUMNS = {
            Calls.CACHED_NAME,
            Calls.CACHED_NUMBER_LABEL,
            Calls.CACHED_NUMBER_TYPE,
            Calls.TYPE,
            Calls.VOICEMAIL_URI,
            Calls.COUNTRY_ISO
    };
    /** Total number of columns exposed by voicemail provider. */
    private static final int NUM_VOICEMAIL_FIELDS = 13;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setUpForOwnPermission();
    }

    /** Returns the appropriate /voicemail URI. */
    private Uri voicemailUri() {
        return mUseSourceUri ?
                Voicemails.buildSourceUri(mActor.packageName) : Voicemails.CONTENT_URI;
    }

    /** Returns the appropriate /status URI. */
    private Uri statusUri() {
        return mUseSourceUri ?
                Status.buildSourceUri(mActor.packageName) : Status.CONTENT_URI;
    }

    public void testInsert() throws Exception {
        Uri uri = mResolver.insert(voicemailUri(), getTestVoicemailValues());
        // We create on purpose a new set of ContentValues here, because the code above modifies
        // the copy it gets.
        assertStoredValues(uri, getTestVoicemailValues());
        assertSelection(uri, getTestVoicemailValues(), Voicemails._ID, ContentUris.parseId(uri));
        assertEquals(1, countFilesInTestDirectory());
    }

    // Test to ensure that media content can be written and read back.
    public void testFileContent() throws Exception {
        Uri uri = insertVoicemail();
        OutputStream out = mResolver.openOutputStream(uri);
        byte[] outBuffer = {0x1, 0x2, 0x3, 0x4};
        out.write(outBuffer);
        out.flush();
        out.close();
        InputStream in = mResolver.openInputStream(uri);
        byte[] inBuffer = new byte[4];
        int numBytesRead = in.read(inBuffer);
        assertEquals(numBytesRead, outBuffer.length);
        MoreAsserts.assertEquals(outBuffer, inBuffer);
        // No more data should be left.
        assertEquals(-1, in.read(inBuffer));
        in.close();
    }

    public void testUpdate() {
        Uri uri = insertVoicemail();
        ContentValues values = new ContentValues();
        values.put(Voicemails.NUMBER, "1-800-263-7643");
        values.put(Voicemails.DATE, 2000);
        values.put(Voicemails.DURATION, 40);
        values.put(Voicemails.STATE, 2);
        values.put(Voicemails.HAS_CONTENT, 1);
        values.put(Voicemails.SOURCE_DATA, "foo");
        int count = mResolver.update(uri, values, null, null);
        assertEquals(1, count);
        assertStoredValues(uri, values);
    }

    public void testDelete() {
        Uri uri = insertVoicemail();
        int count = mResolver.delete(voicemailUri(), Voicemails._ID + "="
                + ContentUris.parseId(uri), null);
        assertEquals(1, count);
        assertEquals(0, getCount(uri, null, null));
    }

    public void testGetType_ItemUri() throws Exception {
        // Random item uri.
        assertEquals(Voicemails.ITEM_TYPE,
                mResolver.getType(ContentUris.withAppendedId(Voicemails.CONTENT_URI, 100)));
        // Item uri of an inserted voicemail.
        ContentValues values = getTestVoicemailValues();
        values.put(Voicemails.MIME_TYPE, "foo/bar");
        Uri uri = mResolver.insert(voicemailUri(), values);
        assertEquals(Voicemails.ITEM_TYPE, mResolver.getType(uri));
    }

    public void testGetType_DirUri() throws Exception {
        assertEquals(Voicemails.DIR_TYPE, mResolver.getType(Voicemails.CONTENT_URI));
        assertEquals(Voicemails.DIR_TYPE, mResolver.getType(Voicemails.buildSourceUri("foo")));
    }

    // Test to ensure that without full permission it is not possible to use the base uri (i.e. with
    // no package URI specified).
    public void testMustUsePackageUriWithoutFullPermission() {
        setUpForOwnPermission();
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.insert(Voicemails.CONTENT_URI, getTestVoicemailValues());
            }
        });

        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.update(Voicemails.CONTENT_URI, getTestVoicemailValues(), null, null);
            }
        });

        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.query(Voicemails.CONTENT_URI, null, null, null, null);
            }
        });

        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.delete(Voicemails.CONTENT_URI, null, null);
            }
        });
    }

    public void testPermissions_InsertAndQuery() {
        setUpForFullPermission();
        // Insert two records - one each with own and another package.
        insertVoicemail();
        insertVoicemailForSourcePackage("another-package");
        assertEquals(2, getCount(voicemailUri(), null, null));

        // Now give away full permission and check that only 1 message is accessible.
        setUpForOwnPermission();
        assertEquals(1, getCount(voicemailUri(), null, null));

        // Once again try to insert message for another package. This time
        // it should fail.
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                insertVoicemailForSourcePackage("another-package");
            }
        });
    }

    public void testPermissions_UpdateAndDelete() {
        setUpForFullPermission();
        // Insert two records - one each with own and another package.
        final Uri ownVoicemail = insertVoicemail();
        final Uri anotherVoicemail = insertVoicemailForSourcePackage("another-package");
        assertEquals(2, getCount(voicemailUri(), null, null));

        // Now give away full permission and check that we can update and delete only
        // the own voicemail.
        setUpForOwnPermission();
        mResolver.update(withSourcePackageParam(ownVoicemail),
                getTestVoicemailValues(), null, null);
        mResolver.delete(withSourcePackageParam(ownVoicemail), null, null);

        // However, attempting to update or delete another-package's voicemail should fail.
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.update(anotherVoicemail, null, null, null);
            }
        });
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.delete(anotherVoicemail, null, null);
            }
        });
    }

    private Uri withSourcePackageParam(Uri uri) {
        return uri.buildUpon()
            .appendQueryParameter(VoicemailContract.PARAM_KEY_SOURCE_PACKAGE, mActor.packageName)
            .build();
    }

    public void testUriPermissions() {
        setUpForFullPermission();
        final Uri uri1 = insertVoicemail();
        final Uri uri2 = insertVoicemail();
        // Give away all permissions before querying. Access to both uris should be denied.
        setUpForNoPermission();
        checkHasNoAccessToUri(uri1);
        checkHasNoAccessToUri(uri2);

        // Just grant permission to uri1. uri1 should pass but uri2 should still fail.
        mActor.addUriPermissions(uri1);
        checkHasReadOnlyAccessToUri(uri1);
        checkHasNoAccessToUri(uri2);

        // Cleanup.
        mActor.removeUriPermissions(uri1);
    }

    private void checkHasNoAccessToUri(final Uri uri) {
        checkHasNoReadAccessToUri(uri);
        checkHasNoWriteAccessToUri(uri);
    }

    private void checkHasReadOnlyAccessToUri(final Uri uri) {
        checkHasReadAccessToUri(uri);
        checkHasNoWriteAccessToUri(uri);
    }

    private void checkHasReadAccessToUri(final Uri uri) {
        Cursor cursor = null;
        try {
            cursor = mResolver.query(uri, null, null ,null, null);
            assertEquals(1, cursor.getCount());
            try {
                ParcelFileDescriptor fd = mResolver.openFileDescriptor(uri, "r");
                assertNotNull(fd);
                fd.close();
            } catch (FileNotFoundException e) {
                fail(e.getMessage());
            } catch (IOException e) {
                fail(e.getMessage());
            }
        } finally {
            MoreCloseables.closeQuietly(cursor);
        }
    }

    private void checkHasNoReadAccessToUri(final Uri uri) {
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.query(uri, null, null ,null, null);
            }
        });
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                try {
                    mResolver.openFileDescriptor(uri, "r");
                } catch (FileNotFoundException e) {
                    fail(e.getMessage());
                }
            }
        });
    }

    private void checkHasNoWriteAccessToUri(final Uri uri) {
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.update(uri, getTestVoicemailValues(), null, null);
            }
        });
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.delete(uri, null, null);
            }
        });
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                try {
                    mResolver.openFileDescriptor(uri, "w");
                } catch (FileNotFoundException e) {
                    fail(e.getMessage());
                }
            }
        });
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                try {
                    mResolver.openFileDescriptor(uri, "rw");
                } catch (FileNotFoundException e) {
                    fail(e.getMessage());
                }
            }
        });
    }

    // Test to ensure that all operations fail when no voicemail permission is granted.
    public void testNoPermissions() {
        setUpForNoPermission();
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.insert(voicemailUri(), getTestVoicemailValues());
            }
        });
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.update(voicemailUri(), getTestVoicemailValues(), null, null);
            }
        });
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.query(voicemailUri(), null, null, null, null);
            }
        });
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.delete(voicemailUri(), null, null);
            }
        });
    }

    // Test to check that none of the call_log provider specific fields are
    // insertable through voicemail provider.
    public void testCannotAccessCallLogSpecificFields_Insert() {
        for (String callLogColumn : CALLLOG_PROVIDER_SPECIFIC_COLUMNS) {
            final ContentValues values = getTestVoicemailValues();
            values.put(callLogColumn, "foo");
            EvenMoreAsserts.assertThrows("Column: " + callLogColumn,
                    IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        mResolver.insert(voicemailUri(), values);
                    }
                });
        }
    }

    // Test to check that none of the call_log provider specific fields are
    // exposed through voicemail provider query.
    public void testCannotAccessCallLogSpecificFields_Query() {
        // Query.
        Cursor cursor = mResolver.query(voicemailUri(), null, null, null, null);
        List<String> columnNames = Arrays.asList(cursor.getColumnNames());
        assertEquals(NUM_VOICEMAIL_FIELDS, columnNames.size());
        // None of the call_log provider specific columns should be present.
        for (String callLogColumn : CALLLOG_PROVIDER_SPECIFIC_COLUMNS) {
            assertFalse("Unexpected column: '" + callLogColumn + "' returned.",
                    columnNames.contains(callLogColumn));
        }
    }

    // Test to check that none of the call_log provider specific fields are
    // updatable through voicemail provider.
    public void testCannotAccessCallLogSpecificFields_Update() {
        for (String callLogColumn : CALLLOG_PROVIDER_SPECIFIC_COLUMNS) {
            final Uri insertedUri = insertVoicemail();
            final ContentValues values = getTestVoicemailValues();
            values.put(callLogColumn, "foo");
            EvenMoreAsserts.assertThrows("Column: " + callLogColumn,
                    IllegalArgumentException.class, new Runnable() {
                    @Override
                    public void run() {
                        mResolver.update(insertedUri, values, null, null);
                    }
                });
        }
    }

    // Tests for voicemail status table.

    public void testStatusInsert() throws Exception {
        ContentValues values = getTestStatusValues();
        Uri uri = mResolver.insert(statusUri(), values);
        assertStoredValues(uri, values);
        assertSelection(uri, values, Status._ID, ContentUris.parseId(uri));
    }

    // Test to ensure that duplicate entries for the same package still end up as the same record.
    public void testStatusInsertDuplicate() throws Exception {
        setUpForFullPermission();
        ContentValues values = getTestStatusValues();
        assertNotNull(mResolver.insert(statusUri(), values));
        assertEquals(1, getCount(statusUri(), null, null));

        // Insertion request for the same package should fail with no change in count.
        values.put(Status.DATA_CHANNEL_STATE, Status.DATA_CHANNEL_STATE_NO_CONNECTION);
        assertNull(mResolver.insert(statusUri(), values));
        assertEquals(1, getCount(statusUri(), null, null));

        // Now insert entry for another source package, and it should end up as a separate record.
        values.put(Status.SOURCE_PACKAGE, "another.package");
        assertNotNull(mResolver.insert(statusUri(), values));
        assertEquals(2, getCount(statusUri(), null, null));
    }

    public void testStatusUpdate() throws Exception {
        Uri uri = insertTestStatusEntry();
        ContentValues values = getTestStatusValues();
        values.put(Status.DATA_CHANNEL_STATE, Status.DATA_CHANNEL_STATE_NO_CONNECTION);
        values.put(Status.NOTIFICATION_CHANNEL_STATE,
                Status.NOTIFICATION_CHANNEL_STATE_MESSAGE_WAITING);
        int count = mResolver.update(uri, values, null, null);
        assertEquals(1, count);
        assertStoredValues(uri, values);
    }

    public void testStatusDelete() {
        Uri uri = insertTestStatusEntry();
        int count = mResolver.delete(statusUri(), Status._ID + "="
                + ContentUris.parseId(uri), null);
        assertEquals(1, count);
        assertEquals(0, getCount(uri, null, null));
    }

    public void testStatusGetType() throws Exception {
        // Item URI.
        Uri uri = insertTestStatusEntry();
        assertEquals(Status.ITEM_TYPE, mResolver.getType(uri));

        // base URIs.
        assertEquals(Status.DIR_TYPE, mResolver.getType(Status.CONTENT_URI));
        assertEquals(Status.DIR_TYPE, mResolver.getType(Status.buildSourceUri("foo")));
    }

    // Basic permission checks for the status table.
    public void testStatusPermissions() throws Exception {
        final ContentValues values = getTestStatusValues();
        // Inserting for another package should fail with any of the URIs.
        values.put(Status.SOURCE_PACKAGE, "another.package");
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.insert(Status.CONTENT_URI, values);
            }
        });
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.insert(Status.buildSourceUri(mActor.packageName), values);
            }
        });

        // But insertion with own package should succeed with the right uri.
        values.put(Status.SOURCE_PACKAGE, mActor.packageName);
        final Uri uri = mResolver.insert(Status.buildSourceUri(mActor.packageName), values);
        assertNotNull(uri);

        // Updating source_package should not work as well.
        values.put(Status.SOURCE_PACKAGE, "another.package");
        EvenMoreAsserts.assertThrows(SecurityException.class, new Runnable() {
            @Override
            public void run() {
                mResolver.update(uri, values, null, null);
            }
        });
    }

    // File operation is not supported by /status URI.
    public void testStatusFileOperation() throws Exception {
        final Uri uri = insertTestStatusEntry();
        EvenMoreAsserts.assertThrows(UnsupportedOperationException.class, new Runnable() {
            @Override
            public void run() {
                try {
                    mResolver.openOutputStream(uri);
                } catch (FileNotFoundException e) {
                    fail("Unexpected exception " + e);
                }
            }
        });

        EvenMoreAsserts.assertThrows(UnsupportedOperationException.class, new Runnable() {
            @Override
            public void run() {
                try {
                    mResolver.openInputStream(uri);
                } catch (FileNotFoundException e) {
                    fail("Unexpected exception " + e);
                }
            }
        });
    }

    /**
     * Inserts a voicemail record with no source package set. The content provider
     * will detect source package.
     */
    private Uri insertVoicemail() {
        return mResolver.insert(voicemailUri(), getTestVoicemailValues());
    }

    /** Inserts a voicemail record for the specified source package. */
    private Uri insertVoicemailForSourcePackage(String sourcePackage) {
        ContentValues values = getTestVoicemailValues();
        values.put(Voicemails.SOURCE_PACKAGE, sourcePackage);
        return mResolver.insert(voicemailUri(), values);
    }

    private ContentValues getTestVoicemailValues() {
        ContentValues values = new ContentValues();
        values.put(Voicemails.NUMBER, "1-800-4664-411");
        values.put(Voicemails.DATE, 1000);
        values.put(Voicemails.DURATION, 30);
        values.put(Voicemails.IS_READ, 0);
        values.put(Voicemails.HAS_CONTENT, 0);
        values.put(Voicemails.SOURCE_DATA, "1234");
        values.put(Voicemails.STATE, Voicemails.STATE_INBOX);
        return values;
    }

    private Uri insertTestStatusEntry() {
        return mResolver.insert(statusUri(), getTestStatusValues());
    }

    private ContentValues getTestStatusValues() {
        ContentValues values = new ContentValues();
        values.put(Status.SOURCE_PACKAGE, mActor.packageName);
        values.put(Status.VOICEMAIL_ACCESS_URI, "tel:901");
        values.put(Status.SETTINGS_URI, "com.example.voicemail.source.SettingsActivity");
        values.put(Status.CONFIGURATION_STATE, Status.CONFIGURATION_STATE_OK);
        values.put(Status.DATA_CHANNEL_STATE, Status.DATA_CHANNEL_STATE_OK);
        values.put(Status.NOTIFICATION_CHANNEL_STATE, Status.NOTIFICATION_CHANNEL_STATE_OK);
        return values;
    }
}
