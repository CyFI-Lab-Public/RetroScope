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

import static com.android.providers.contacts.ContactsActor.PACKAGE_GREY;
import static com.android.providers.contacts.TestUtils.cv;

import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Identity;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.ContactsContract.StreamItems;
import android.test.MoreAsserts;
import android.test.mock.MockContentResolver;
import android.util.Log;

import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.testutil.CommonDatabaseUtils;
import com.android.providers.contacts.testutil.DataUtil;
import com.android.providers.contacts.testutil.RawContactUtil;
import com.android.providers.contacts.testutil.TestUtil;
import com.android.providers.contacts.util.Hex;
import com.android.providers.contacts.util.MockClock;
import com.google.android.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A common superclass for {@link ContactsProvider2}-related tests.
 */
public abstract class BaseContactsProvider2Test extends PhotoLoadingTestCase {

    protected static final String PACKAGE = "ContactsProvider2Test";
    public static final String READ_ONLY_ACCOUNT_TYPE =
            SynchronousContactsProvider2.READ_ONLY_ACCOUNT_TYPE;

    protected ContactsActor mActor;
    protected MockContentResolver mResolver;
    protected Account mAccount = new Account("account1", "account type1");
    protected Account mAccountTwo = new Account("account2", "account type2");

    protected final static Long NO_LONG = new Long(0);
    protected final static String NO_STRING = new String("");
    protected final static Account NO_ACCOUNT = new Account("a", "b");

    /**
     * Use {@link MockClock#install()} to start using it.
     * It'll be automatically uninstalled by {@link #tearDown()}.
     */
    protected static final MockClock sMockClock = new MockClock();

    protected Class<? extends ContentProvider> getProviderClass() {
        return SynchronousContactsProvider2.class;
    }

    protected String getAuthority() {
        return ContactsContract.AUTHORITY;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActor = new ContactsActor(getContext(), PACKAGE_GREY, getProviderClass(), getAuthority());
        mResolver = mActor.resolver;
        if (mActor.provider instanceof SynchronousContactsProvider2) {
            getContactsProvider().wipeData();
        }

        // Give the actor access to read/write contacts and profile data by default.
        mActor.addPermissions(
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.READ_SOCIAL_STREAM",
                "android.permission.WRITE_SOCIAL_STREAM",
                "android.permission.READ_PROFILE",
                "android.permission.WRITE_PROFILE");
    }

    @Override
    protected void tearDown() throws Exception {
        sMockClock.uninstall();
        super.tearDown();
    }

    public SynchronousContactsProvider2 getContactsProvider() {
        return (SynchronousContactsProvider2) mActor.provider;
    }

    public Context getMockContext() {
        return mActor.context;
    }

    public void addAuthority(String authority) {
        mActor.addAuthority(authority);
    }

    public ContentProvider addProvider(Class<? extends ContentProvider> providerClass,
            String authority) throws Exception {
        return mActor.addProvider(providerClass, authority);
    }

    public ContentProvider getProvider() {
        return mActor.provider;
    }

    protected Uri setCallerIsSyncAdapter(Uri uri, Account account) {
        if (account == null) {
            return uri;
        }
        final Uri.Builder builder = uri.buildUpon();
        builder.appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name);
        builder.appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type);
        builder.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
        return builder.build();
    }

    protected int updateItem(Uri uri, long id, String... extras) {
        Uri itemUri = ContentUris.withAppendedId(uri, id);
        return updateItem(itemUri, extras);
    }

    protected int updateItem(Uri uri, String... extras) {
        ContentValues values = new ContentValues();
        CommonDatabaseUtils.extrasVarArgsToValues(values, extras);
        return mResolver.update(uri, values, null, null);
    }

    protected long createGroup(Account account, String sourceId, String title) {
        return createGroup(account, sourceId, title, 1, false, false);
    }

    protected long createGroup(Account account, String sourceId, String title, int visible) {
        return createGroup(account, sourceId, title, visible, false, false);
    }

    protected long createAutoAddGroup(Account account) {
        return createGroup(account, "auto", "auto",
                0 /* visible */,  true /* auto-add */, false /* fav */);
    }

    protected long createGroup(Account account, String sourceId, String title,
            int visible, boolean autoAdd, boolean favorite) {
        ContentValues values = new ContentValues();
        values.put(Groups.SOURCE_ID, sourceId);
        values.put(Groups.TITLE, title);
        values.put(Groups.GROUP_VISIBLE, visible);
        values.put(Groups.AUTO_ADD, autoAdd ? 1 : 0);
        values.put(Groups.FAVORITES, favorite ? 1 : 0);
        final Uri uri = TestUtil.maybeAddAccountQueryParameters(Groups.CONTENT_URI, account);
        return ContentUris.parseId(mResolver.insert(uri, values));
    }

    protected void createSettings(Account account, String shouldSync, String ungroupedVisible) {
        createSettings(new AccountWithDataSet(account.name, account.type, null),
                shouldSync, ungroupedVisible);
    }

    protected void createSettings(AccountWithDataSet account, String shouldSync,
            String ungroupedVisible) {
        ContentValues values = new ContentValues();
        values.put(Settings.ACCOUNT_NAME, account.getAccountName());
        values.put(Settings.ACCOUNT_TYPE, account.getAccountType());
        if (account.getDataSet() != null) {
            values.put(Settings.DATA_SET, account.getDataSet());
        }
        values.put(Settings.SHOULD_SYNC, shouldSync);
        values.put(Settings.UNGROUPED_VISIBLE, ungroupedVisible);
        mResolver.insert(Settings.CONTENT_URI, values);
    }

    protected Uri insertOrganization(long rawContactId, ContentValues values) {
        return insertOrganization(rawContactId, values, false);
    }

    protected Uri insertOrganization(long rawContactId, ContentValues values, boolean primary) {
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
        values.put(Organization.TYPE, Organization.TYPE_WORK);
        if (primary) {
            values.put(Data.IS_PRIMARY, 1);
        }

        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertPhoneNumber(long rawContactId, String phoneNumber) {
        return insertPhoneNumber(rawContactId, phoneNumber, false);
    }

    protected Uri insertPhoneNumber(long rawContactId, String phoneNumber, boolean primary) {
        return insertPhoneNumber(rawContactId, phoneNumber, primary, Phone.TYPE_HOME);
    }

    protected Uri insertPhoneNumber(long rawContactId, String phoneNumber, boolean primary,
            int type) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        values.put(Phone.NUMBER, phoneNumber);
        values.put(Phone.TYPE, type);
        if (primary) {
            values.put(Data.IS_PRIMARY, 1);
        }

        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertEmail(long rawContactId, String email) {
        return insertEmail(rawContactId, email, false);
    }

    protected Uri insertEmail(long rawContactId, String email, boolean primary) {
        return insertEmail(rawContactId, email, primary, Email.TYPE_HOME, null);
    }

    protected Uri insertEmail(long rawContactId, String email, boolean primary,
            boolean superPrimary) {
        return insertEmail(rawContactId, email, primary, superPrimary, Email.TYPE_HOME, null);
    }

    protected Uri insertEmail(long rawContactId, String email, boolean primary, int type,
            String label) {
        return insertEmail(rawContactId, email, primary, false, type, label);
    }

    protected Uri insertEmail(long rawContactId, String email, boolean primary,
            boolean superPrimary, int type,  String label) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
        values.put(Email.DATA, email);
        values.put(Email.TYPE, type);
        values.put(Email.LABEL, label);
        if (primary) {
            values.put(Data.IS_PRIMARY, 1);
        }
        if (superPrimary) {
            values.put(Data.IS_SUPER_PRIMARY, 1);
        }

        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertSipAddress(long rawContactId, String sipAddress) {
        return insertSipAddress(rawContactId, sipAddress, false);
    }

    protected Uri insertSipAddress(long rawContactId, String sipAddress, boolean primary) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, SipAddress.CONTENT_ITEM_TYPE);
        values.put(SipAddress.SIP_ADDRESS, sipAddress);
        if (primary) {
            values.put(Data.IS_PRIMARY, 1);
        }

        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertNickname(long rawContactId, String nickname) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
        values.put(Nickname.NAME, nickname);
        values.put(Nickname.TYPE, Nickname.TYPE_OTHER_NAME);

        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertPostalAddress(long rawContactId, String formattedAddress) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
        values.put(StructuredPostal.FORMATTED_ADDRESS, formattedAddress);

        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertPostalAddress(long rawContactId, ContentValues values) {
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertPhoto(long rawContactId) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        values.put(Photo.PHOTO, loadTestPhoto());
        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertPhoto(long rawContactId, int resourceId) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        values.put(Photo.PHOTO, loadPhotoFromResource(resourceId, PhotoSize.ORIGINAL));
        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertGroupMembership(long rawContactId, String sourceId) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        values.put(GroupMembership.GROUP_SOURCE_ID, sourceId);
        return mResolver.insert(Data.CONTENT_URI, values);
    }

    protected Uri insertGroupMembership(long rawContactId, Long groupId) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        values.put(GroupMembership.GROUP_ROW_ID, groupId);
        return mResolver.insert(Data.CONTENT_URI, values);
    }

    public void removeGroupMemberships(long rawContactId) {
        mResolver.delete(Data.CONTENT_URI,
                Data.MIMETYPE + "=? AND " + GroupMembership.RAW_CONTACT_ID + "=?",
                new String[] { GroupMembership.CONTENT_ITEM_TYPE, String.valueOf(rawContactId) });
    }

    protected Uri insertStatusUpdate(int protocol, String customProtocol, String handle,
            int presence, String status, int chatMode) {
        return insertStatusUpdate(protocol, customProtocol, handle, presence, status, chatMode,
                false);
    }

    protected Uri insertStatusUpdate(int protocol, String customProtocol, String handle,
            int presence, String status, int chatMode, boolean isUserProfile) {
        return insertStatusUpdate(protocol, customProtocol, handle, presence, status, 0, chatMode,
                isUserProfile);
    }

    protected Uri insertStatusUpdate(int protocol, String customProtocol, String handle,
            int presence, String status, long timestamp, int chatMode, boolean isUserProfile) {
        ContentValues values = new ContentValues();
        values.put(StatusUpdates.PROTOCOL, protocol);
        values.put(StatusUpdates.CUSTOM_PROTOCOL, customProtocol);
        values.put(StatusUpdates.IM_HANDLE, handle);
        return insertStatusUpdate(values, presence, status, timestamp, chatMode, isUserProfile);
    }

    protected Uri insertStatusUpdate(
            long dataId, int presence, String status, long timestamp, int chatMode) {
        return insertStatusUpdate(dataId, presence, status, timestamp, chatMode, false);
    }

    protected Uri insertStatusUpdate(
            long dataId, int presence, String status, long timestamp, int chatMode,
            boolean isUserProfile) {
        ContentValues values = new ContentValues();
        values.put(StatusUpdates.DATA_ID, dataId);
        return insertStatusUpdate(values, presence, status, timestamp, chatMode, isUserProfile);
    }

    private Uri insertStatusUpdate(
            ContentValues values, int presence, String status, long timestamp, int chatMode,
            boolean isUserProfile) {
        if (presence != 0) {
            values.put(StatusUpdates.PRESENCE, presence);
            values.put(StatusUpdates.CHAT_CAPABILITY, chatMode);
        }
        if (status != null) {
            values.put(StatusUpdates.STATUS, status);
        }
        if (timestamp != 0) {
            values.put(StatusUpdates.STATUS_TIMESTAMP, timestamp);
        }

        Uri insertUri = isUserProfile
                ? StatusUpdates.PROFILE_CONTENT_URI
                : StatusUpdates.CONTENT_URI;
        Uri resultUri = mResolver.insert(insertUri, values);
        return resultUri;
    }

    protected Uri insertStreamItem(long rawContactId, ContentValues values, Account account) {
        return mResolver.insert(
                TestUtil.maybeAddAccountQueryParameters(Uri.withAppendedPath(
                        ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                        RawContacts.StreamItems.CONTENT_DIRECTORY), account),
                values);
    }

    protected Uri insertStreamItemPhoto(long streamItemId, ContentValues values, Account account) {
        return mResolver.insert(
                TestUtil.maybeAddAccountQueryParameters(Uri.withAppendedPath(
                        ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                        StreamItems.StreamItemPhotos.CONTENT_DIRECTORY), account),
                values);
    }

    protected Uri insertImHandle(long rawContactId, int protocol, String customProtocol,
            String handle) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE);
        values.put(Im.PROTOCOL, protocol);
        values.put(Im.CUSTOM_PROTOCOL, customProtocol);
        values.put(Im.DATA, handle);
        values.put(Im.TYPE, Im.TYPE_HOME);

        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertEvent(long rawContactId, int type, String date) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE);
        values.put(Event.TYPE, type);
        values.put(Event.START_DATE, date);
        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertNote(long rawContactId, String note) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE);
        values.put(Note.NOTE, note);
        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected Uri insertIdentity(long rawContactId, String identity, String namespace) {
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Identity.CONTENT_ITEM_TYPE);
        values.put(Identity.NAMESPACE, namespace);
        values.put(Identity.IDENTITY, identity);

        Uri resultUri = mResolver.insert(Data.CONTENT_URI, values);
        return resultUri;
    }

    protected void setContactAccount(long rawContactId, String accountType, String accountName) {
        ContentValues values = new ContentValues();
        values.put(RawContacts.ACCOUNT_TYPE, accountType);
        values.put(RawContacts.ACCOUNT_NAME, accountName);

        mResolver.update(ContentUris.withAppendedId(
                RawContacts.CONTENT_URI, rawContactId), values, null, null);
    }

    protected void setAggregationException(int type, long rawContactId1, long rawContactId2) {
        ContentValues values = new ContentValues();
        values.put(AggregationExceptions.RAW_CONTACT_ID1, rawContactId1);
        values.put(AggregationExceptions.RAW_CONTACT_ID2, rawContactId2);
        values.put(AggregationExceptions.TYPE, type);
        assertEquals(1, mResolver.update(AggregationExceptions.CONTENT_URI, values, null, null));
    }

    protected void markInvisible(long contactId) {
        // There's no api for this, so we just tweak the DB directly.
        SQLiteDatabase db = ((ContactsProvider2) getProvider()).getDatabaseHelper()
                .getWritableDatabase();
        db.execSQL("DELETE FROM " + Tables.DEFAULT_DIRECTORY +
                " WHERE " + BaseColumns._ID + "=" + contactId);
    }

    protected Cursor queryRawContact(long rawContactId) {
        return mResolver.query(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                null, null, null, null);
    }

    protected Cursor queryContact(long contactId) {
        return mResolver.query(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                null, null, null, null);
    }

    protected Cursor queryContact(long contactId, String[] projection) {
        return mResolver.query(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                projection, null, null, null);
    }

    protected Uri getContactUriForRawContact(long rawContactId) {
        return ContentUris.withAppendedId(Contacts.CONTENT_URI, queryContactId(rawContactId));
    }

    protected long queryContactId(long rawContactId) {
        Cursor c = queryRawContact(rawContactId);
        assertTrue(c.moveToFirst());
        long contactId = c.getLong(c.getColumnIndex(RawContacts.CONTACT_ID));
        c.close();
        return contactId;
    }

    protected long queryPhotoId(long contactId) {
        Cursor c = queryContact(contactId);
        assertTrue(c.moveToFirst());
        long photoId = c.getInt(c.getColumnIndex(Contacts.PHOTO_ID));
        c.close();
        return photoId;
    }

    protected long queryPhotoFileId(long contactId) {
        return getStoredLongValue(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.PHOTO_FILE_ID);
    }

    protected boolean queryRawContactIsStarred(long rawContactId) {
        Cursor c = queryRawContact(rawContactId);
        try {
            assertTrue(c.moveToFirst());
            return c.getLong(c.getColumnIndex(RawContacts.STARRED)) != 0;
        } finally {
            c.close();
        }
    }

    protected String queryDisplayName(long contactId) {
        Cursor c = queryContact(contactId);
        assertTrue(c.moveToFirst());
        String displayName = c.getString(c.getColumnIndex(Contacts.DISPLAY_NAME));
        c.close();
        return displayName;
    }

    protected String queryLookupKey(long contactId) {
        Cursor c = queryContact(contactId);
        assertTrue(c.moveToFirst());
        String lookupKey = c.getString(c.getColumnIndex(Contacts.LOOKUP_KEY));
        c.close();
        return lookupKey;
    }

    protected void assertAggregated(long rawContactId1, long rawContactId2) {
        long contactId1 = queryContactId(rawContactId1);
        long contactId2 = queryContactId(rawContactId2);
        assertTrue(contactId1 == contactId2);
    }

    protected void assertAggregated(long rawContactId1, long rawContactId2,
            String expectedDisplayName) {
        long contactId1 = queryContactId(rawContactId1);
        long contactId2 = queryContactId(rawContactId2);
        assertTrue(contactId1 == contactId2);

        String displayName = queryDisplayName(contactId1);
        assertEquals(expectedDisplayName, displayName);
    }

    protected void assertNotAggregated(long rawContactId1, long rawContactId2) {
        long contactId1 = queryContactId(rawContactId1);
        long contactId2 = queryContactId(rawContactId2);
        assertTrue(contactId1 != contactId2);
    }

    protected void assertStructuredName(long rawContactId, String prefix, String givenName,
            String middleName, String familyName, String suffix) {
        Uri uri = Uri.withAppendedPath(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                RawContacts.Data.CONTENT_DIRECTORY);

        final String[] projection = new String[] {
                StructuredName.PREFIX, StructuredName.GIVEN_NAME, StructuredName.MIDDLE_NAME,
                StructuredName.FAMILY_NAME, StructuredName.SUFFIX
        };

        Cursor c = mResolver.query(uri, projection, Data.MIMETYPE + "='"
                + StructuredName.CONTENT_ITEM_TYPE + "'", null, null);

        assertTrue(c.moveToFirst());
        assertEquals(prefix, c.getString(0));
        assertEquals(givenName, c.getString(1));
        assertEquals(middleName, c.getString(2));
        assertEquals(familyName, c.getString(3));
        assertEquals(suffix, c.getString(4));
        c.close();
    }

    protected long assertSingleGroup(Long rowId, Account account, String sourceId, String title) {
        Cursor c = mResolver.query(Groups.CONTENT_URI, null, null, null, null);
        try {
            assertTrue(c.moveToNext());
            long actualRowId = assertGroup(c, rowId, account, sourceId, title);
            assertFalse(c.moveToNext());
            return actualRowId;
        } finally {
            c.close();
        }
    }

    protected long assertSingleGroupMembership(Long rowId, Long rawContactId, Long groupRowId,
            String sourceId) {
        Cursor c = mResolver.query(ContactsContract.Data.CONTENT_URI, null, null, null, null);
        try {
            assertTrue(c.moveToNext());
            long actualRowId = assertGroupMembership(c, rowId, rawContactId, groupRowId, sourceId);
            assertFalse(c.moveToNext());
            return actualRowId;
        } finally {
            c.close();
        }
    }

    protected long assertGroupMembership(Cursor c, Long rowId, Long rawContactId, Long groupRowId,
            String sourceId) {
        assertNullOrEquals(c, rowId, Data._ID);
        assertNullOrEquals(c, rawContactId, GroupMembership.RAW_CONTACT_ID);
        assertNullOrEquals(c, groupRowId, GroupMembership.GROUP_ROW_ID);
        assertNullOrEquals(c, sourceId, GroupMembership.GROUP_SOURCE_ID);
        return c.getLong(c.getColumnIndexOrThrow("_id"));
    }

    protected long assertGroup(Cursor c, Long rowId, Account account, String sourceId, String title) {
        assertNullOrEquals(c, rowId, Groups._ID);
        assertNullOrEquals(c, account);
        assertNullOrEquals(c, sourceId, Groups.SOURCE_ID);
        assertNullOrEquals(c, title, Groups.TITLE);
        return c.getLong(c.getColumnIndexOrThrow("_id"));
    }

    private void assertNullOrEquals(Cursor c, Account account) {
        if (account == NO_ACCOUNT) {
            return;
        }
        if (account == null) {
            assertTrue(c.isNull(c.getColumnIndexOrThrow(Groups.ACCOUNT_NAME)));
            assertTrue(c.isNull(c.getColumnIndexOrThrow(Groups.ACCOUNT_TYPE)));
        } else {
            assertEquals(account.name, c.getString(c.getColumnIndexOrThrow(Groups.ACCOUNT_NAME)));
            assertEquals(account.type, c.getString(c.getColumnIndexOrThrow(Groups.ACCOUNT_TYPE)));
        }
    }

    private void assertNullOrEquals(Cursor c, Long value, String columnName) {
        if (value != NO_LONG) {
            if (value == null) assertTrue(c.isNull(c.getColumnIndexOrThrow(columnName)));
            else assertEquals((long) value, c.getLong(c.getColumnIndexOrThrow(columnName)));
        }
    }

    private void assertNullOrEquals(Cursor c, String value, String columnName) {
        if (value != NO_STRING) {
            if (value == null) assertTrue(c.isNull(c.getColumnIndexOrThrow(columnName)));
            else assertEquals(value, c.getString(c.getColumnIndexOrThrow(columnName)));
        }
    }

    protected void assertDataRow(ContentValues actual, String expectedMimetype,
            Object... expectedArguments) {
        assertEquals(actual.toString(), expectedMimetype, actual.getAsString(Data.MIMETYPE));
        for (int i = 0; i < expectedArguments.length; i += 2) {
            String columnName = (String) expectedArguments[i];
            Object expectedValue = expectedArguments[i + 1];
            if (expectedValue instanceof Uri) {
                expectedValue = ContentUris.parseId((Uri) expectedValue);
            }
            if (expectedValue == null) {
                assertNull(actual.toString(), actual.get(columnName));
            }
            if (expectedValue instanceof Long) {
                assertEquals("mismatch at " + columnName + " from " + actual.toString(),
                        expectedValue, actual.getAsLong(columnName));
            } else if (expectedValue instanceof Integer) {
                assertEquals("mismatch at " + columnName + " from " + actual.toString(),
                        expectedValue, actual.getAsInteger(columnName));
            } else if (expectedValue instanceof String) {
                assertEquals("mismatch at " + columnName + " from " + actual.toString(),
                        expectedValue, actual.getAsString(columnName));
            } else {
                assertEquals("mismatch at " + columnName + " from " + actual.toString(),
                        expectedValue, actual.get(columnName));
            }
        }
    }

    protected void assertNoRowsAndClose(Cursor c) {
        try {
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }
    }

    protected static class IdComparator implements Comparator<ContentValues> {
        @Override
        public int compare(ContentValues o1, ContentValues o2) {
            long id1 = o1.getAsLong(ContactsContract.Data._ID);
            long id2 = o2.getAsLong(ContactsContract.Data._ID);
            if (id1 == id2) return 0;
            return (id1 < id2) ? -1 : 1;
        }
    }

    protected ContentValues[] asSortedContentValuesArray(
            ArrayList<Entity.NamedContentValues> subValues) {
        ContentValues[] result = new ContentValues[subValues.size()];
        int i = 0;
        for (Entity.NamedContentValues subValue : subValues) {
            result[i] = subValue.values;
            i++;
        }
        Arrays.sort(result, new IdComparator());
        return result;
    }

    protected void assertDirty(Uri uri, boolean state) {
        Cursor c = mResolver.query(uri, new String[]{"dirty"}, null, null, null);
        assertTrue(c.moveToNext());
        assertEquals(state, c.getLong(0) != 0);
        assertFalse(c.moveToNext());
        c.close();
    }

    protected long getVersion(Uri uri) {
        Cursor c = mResolver.query(uri, new String[]{"version"}, null, null, null);
        assertTrue(c.moveToNext());
        long version = c.getLong(0);
        assertFalse(c.moveToNext());
        c.close();
        return version;
    }

    protected void clearDirty(Uri uri) {
        ContentValues values = new ContentValues();
        values.put("dirty", 0);
        mResolver.update(uri, values, null, null);
    }

    protected void storeValue(Uri contentUri, long id, String column, String value) {
        storeValue(ContentUris.withAppendedId(contentUri, id), column, value);
    }

    protected void storeValue(Uri contentUri, String column, String value) {
        ContentValues values = new ContentValues();
        values.put(column, value);

        mResolver.update(contentUri, values, null, null);
    }

    protected void storeValue(Uri contentUri, long id, String column, long value) {
        storeValue(ContentUris.withAppendedId(contentUri, id), column, value);
    }

    protected void storeValue(Uri contentUri, String column, long value) {
        ContentValues values = new ContentValues();
        values.put(column, value);

        mResolver.update(contentUri, values, null, null);
    }

    protected void assertStoredValue(Uri contentUri, long id, String column, Object expectedValue) {
        assertStoredValue(ContentUris.withAppendedId(contentUri, id), column, expectedValue);
    }

    protected void assertStoredValue(Uri rowUri, String column, Object expectedValue) {
        String value = getStoredValue(rowUri, column);
        if (expectedValue == null) {
            assertNull("Column value " + column, value);
        } else {
            assertEquals("Column value " + column, String.valueOf(expectedValue), value);
        }
    }

    protected void assertStoredValue(Uri rowUri, String selection, String[] selectionArgs,
            String column, Object expectedValue) {
        String value = getStoredValue(rowUri, selection, selectionArgs, column);
        if (expectedValue == null) {
            assertNull("Column value " + column, value);
        } else {
            assertEquals("Column value " + column, String.valueOf(expectedValue), value);
        }
    }

    protected String getStoredValue(Uri rowUri, String column) {
        return getStoredValue(rowUri, null, null, column);
    }

    protected String getStoredValue(Uri uri, String selection, String[] selectionArgs,
            String column) {
        String value = null;
        Cursor c = mResolver.query(uri, new String[] { column }, selection, selectionArgs, null);
        try {
            assertEquals("Record count for " + uri, 1, c.getCount());

            if (c.moveToFirst()) {
                value = c.getString(c.getColumnIndex(column));
            }
        } finally {
            c.close();
        }
        return value;
    }

    protected Long getStoredLongValue(Uri uri, String selection, String[] selectionArgs,
            String column) {
        Long value = null;
        Cursor c = mResolver.query(uri, new String[] { column }, selection, selectionArgs, null);
        try {
            assertEquals("Record count", 1, c.getCount());

            if (c.moveToFirst()) {
                value = c.getLong(c.getColumnIndex(column));
            }
        } finally {
            c.close();
        }
        return value;
    }

    protected Long getStoredLongValue(Uri uri, String column) {
        return getStoredLongValue(uri, null, null, column);
    }

    protected void assertStoredValues(Uri rowUri, ContentValues expectedValues) {
        assertStoredValues(rowUri, null, null, expectedValues);
    }

    protected void assertStoredValues(Uri rowUri, ContentValues... expectedValues) {
        assertStoredValues(rowUri, null, null, expectedValues);
    }

    protected void assertStoredValues(Uri rowUri, String selection, String[] selectionArgs,
            ContentValues expectedValues) {
        Cursor c = mResolver.query(rowUri, null, selection, selectionArgs, null);
        try {
            assertEquals("Record count", 1, c.getCount());
            c.moveToFirst();
            assertCursorValues(c, expectedValues);
        } catch (Error e) {
            TestUtils.dumpCursor(c);
            throw e;
        } finally {
            c.close();
        }
    }

    protected void assertContainsValues(Uri rowUri, ContentValues expectedValues) {
        Cursor c = mResolver.query(rowUri, null, null, null, null);
        try {
            assertEquals("Record count", 1, c.getCount());
            c.moveToFirst();
            assertCursorValuesPartialMatch(c, expectedValues);
        } catch (Error e) {
            TestUtils.dumpCursor(c);
            throw e;
        } finally {
            c.close();
        }
    }

    protected void assertStoredValuesWithProjection(Uri rowUri, ContentValues expectedValues) {
        assertStoredValuesWithProjection(rowUri, new ContentValues[] {expectedValues});
    }

    protected void assertStoredValuesWithProjection(Uri rowUri, ContentValues... expectedValues) {
        assertTrue("Need at least one ContentValues for this test", expectedValues.length > 0);
        Cursor c = mResolver.query(rowUri, buildProjection(expectedValues[0]), null, null, null);
        try {
            assertEquals("Record count", expectedValues.length, c.getCount());
            c.moveToFirst();
            assertCursorValues(c, expectedValues);
        } catch (Error e) {
            TestUtils.dumpCursor(c);
            throw e;
        } finally {
            c.close();
        }
    }

    protected void assertStoredValues(
            Uri rowUri, String selection, String[] selectionArgs, ContentValues... expectedValues) {
        assertStoredValues(mResolver.query(rowUri, null, selection, selectionArgs, null),
                expectedValues);
    }

    private void assertStoredValues(Cursor c, ContentValues... expectedValues) {
        try {
            assertEquals("Record count", expectedValues.length, c.getCount());
            assertCursorValues(c, expectedValues);
        } catch (Error e) {
            TestUtils.dumpCursor(c);
            throw e;
        } finally {
            c.close();
        }
    }

    /**
     * A variation of {@link #assertStoredValues}, but it queries directly to the DB.
     */
    protected void assertStoredValuesDb(
            String sql, String[] selectionArgs, ContentValues... expectedValues) {
        SQLiteDatabase db = ((ContactsProvider2) getProvider()).getDatabaseHelper()
                .getReadableDatabase();
        assertStoredValues(db.rawQuery(sql, selectionArgs), expectedValues);
    }

    protected void assertStoredValuesOrderly(Uri rowUri, ContentValues... expectedValues) {
        assertStoredValuesOrderly(rowUri, null, null, expectedValues);
    }

    protected void assertStoredValuesOrderly(Uri rowUri, String selection,
            String[] selectionArgs, ContentValues... expectedValues) {
        Cursor c = mResolver.query(rowUri, null, selection, selectionArgs, null);
        try {
            assertEquals("Record count", expectedValues.length, c.getCount());
            assertCursorValuesOrderly(c, expectedValues);
        } catch (Error e) {
            TestUtils.dumpCursor(c);
            throw e;
        } finally {
            c.close();
        }
    }

    /**
     * Constructs a selection (where clause) out of all supplied values, uses it
     * to query the provider and verifies that a single row is returned and it
     * has the same values as requested.
     */
    protected void assertSelection(Uri uri, ContentValues values, String idColumn, long id) {
        assertSelection(uri, values, idColumn, id, null);
    }

    public void assertSelectionWithProjection(Uri uri, ContentValues values, String idColumn,
            long id) {
        assertSelection(uri, values, idColumn, id, buildProjection(values));
    }

    private void assertSelection(Uri uri, ContentValues values, String idColumn, long id,
            String[] projection) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> selectionArgs = new ArrayList<String>(values.size());
        if (idColumn != null) {
            sb.append(idColumn).append("=").append(id);
        }
        Set<Map.Entry<String, Object>> entries = values.valueSet();
        for (Map.Entry<String, Object> entry : entries) {
            String column = entry.getKey();
            Object value = entry.getValue();
            if (sb.length() != 0) {
                sb.append(" AND ");
            }
            sb.append(column);
            if (value == null) {
                sb.append(" IS NULL");
            } else {
                sb.append("=?");
                selectionArgs.add(String.valueOf(value));
            }
        }

        Cursor c = mResolver.query(uri, projection, sb.toString(), selectionArgs.toArray(new String[0]),
                null);
        try {
            assertEquals("Record count", 1, c.getCount());
            c.moveToFirst();
            assertCursorValues(c, values);
        } catch (Error e) {
            TestUtils.dumpCursor(c);
            throw e;
        } finally {
            c.close();
        }
    }

    protected void assertCursorValue(Cursor cursor, String column, Object expectedValue) {
        String actualValue = cursor.getString(cursor.getColumnIndex(column));
        assertEquals("Column " + column, String.valueOf(expectedValue),
                String.valueOf(actualValue));
    }

    protected void assertCursorValues(Cursor cursor, ContentValues expectedValues) {
        StringBuilder message = new StringBuilder();
        boolean result = equalsWithExpectedValues(cursor, expectedValues, message);
        assertTrue(message.toString(), result);
    }

    protected void assertCursorValuesPartialMatch(Cursor cursor, ContentValues expectedValues) {
        StringBuilder message = new StringBuilder();
        boolean result = expectedValuePartiallyMatches(cursor, expectedValues, message);
        assertTrue(message.toString(), result);
    }

    protected void assertCursorHasAnyRecordMatch(Cursor cursor, ContentValues expectedValues) {
        final StringBuilder message = new StringBuilder();
        boolean found = false;
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            message.setLength(0);
            final int pos = cursor.getPosition();
            found = equalsWithExpectedValues(cursor, expectedValues, message);
            if (found) {
                break;
            }
        }
        assertTrue("Expected values can not be found " + expectedValues + "," + message.toString(),
                found);
    }

    protected void assertCursorValues(Cursor cursor, ContentValues... expectedValues) {
        StringBuilder message = new StringBuilder();

        // In case if expectedValues contains multiple identical values, remember which cursor
        // rows are "consumed" to prevent multiple ContentValues from hitting the same row.
        final BitSet used = new BitSet(cursor.getCount());

        for (ContentValues v : expectedValues) {
            boolean found = false;
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                final int pos = cursor.getPosition();
                if (used.get(pos)) continue;
                found = equalsWithExpectedValues(cursor, v, message);
                if (found) {
                    used.set(pos);
                    break;
                }
            }
            assertTrue("Expected values can not be found " + v + "," + message.toString(), found);
        }
    }

    private void assertCursorValuesOrderly(Cursor cursor, ContentValues... expectedValues) {
        StringBuilder message = new StringBuilder();
        cursor.moveToPosition(-1);
        for (ContentValues v : expectedValues) {
            assertTrue(cursor.moveToNext());
            boolean ok = equalsWithExpectedValues(cursor, v, message);
            assertTrue("ContentValues didn't match.  Pos=" + cursor.getPosition() + ", values=" +
                    v + message.toString(), ok);
        }
    }

    private boolean expectedValuePartiallyMatches(Cursor cursor, ContentValues expectedValues,
            StringBuilder msgBuffer) {
        for (String column : expectedValues.keySet()) {
            int index = cursor.getColumnIndex(column);
            if (index == -1) {
                msgBuffer.append(" No such column: ").append(column);
                return false;
            }
            String expectedValue = expectedValues.getAsString(column);
            String value = cursor.getString(cursor.getColumnIndex(column));
            if (value != null && !value.contains(expectedValue)) {
                msgBuffer.append(" Column value ").append(column).append(" expected to contain <")
                        .append(expectedValue).append(">, but was <").append(value).append('>');
                return false;
            }
        }
        return true;
    }

    private boolean equalsWithExpectedValues(Cursor cursor, ContentValues expectedValues,
            StringBuilder msgBuffer) {
        for (String column : expectedValues.keySet()) {
            int index = cursor.getColumnIndex(column);
            if (index == -1) {
                msgBuffer.append(" No such column: ").append(column);
                return false;
            }
            Object expectedValue = expectedValues.get(column);
            String value;
            if (expectedValue instanceof byte[]) {
                expectedValue = Hex.encodeHex((byte[])expectedValue, false);
                value = Hex.encodeHex(cursor.getBlob(index), false);
            } else {
                expectedValue = expectedValues.getAsString(column);
                value = cursor.getString(cursor.getColumnIndex(column));
            }
            if (expectedValue != null && !expectedValue.equals(value) || value != null
                    && !value.equals(expectedValue)) {
                msgBuffer
                        .append(" Column value ")
                        .append(column)
                        .append(" expected <")
                        .append(expectedValue)
                        .append(">, but was <")
                        .append(value)
                        .append('>');
                return false;
            }
        }
        return true;
    }

    private static final String[] DATA_USAGE_PROJECTION =
            new String[] {Data.DATA1, Data.TIMES_USED, Data.LAST_TIME_USED};

    protected void assertDataUsageCursorContains(Uri uri, String data1, int timesUsed,
            int lastTimeUsed) {
        final Cursor cursor = mResolver.query(uri, DATA_USAGE_PROJECTION, null, null,
                null);
        try {
            assertCursorHasAnyRecordMatch(cursor, cv(Data.DATA1, data1, Data.TIMES_USED, timesUsed,
                    Data.LAST_TIME_USED, lastTimeUsed));
        } finally {
            cursor.close();
        }
    }

    private String[] buildProjection(ContentValues values) {
        String[] projection = new String[values.size()];
        Iterator<Entry<String, Object>> iter = values.valueSet().iterator();
        for (int i = 0; i < projection.length; i++) {
            projection[i] = iter.next().getKey();
        }
        return projection;
    }

    protected int getCount(Uri uri, String selection, String[] selectionArgs) {
        Cursor c = mResolver.query(uri, null, selection, selectionArgs, null);
        try {
            return c.getCount();
        } finally {
            c.close();
        }
    }

    public static void dump(ContentResolver resolver, boolean aggregatedOnly) {
        String[] projection = new String[] {
                Contacts._ID,
                Contacts.DISPLAY_NAME
        };
        String selection = null;
        if (aggregatedOnly) {
            selection = Contacts._ID
                    + " IN (SELECT contact_id" +
                    		" FROM raw_contacts GROUP BY contact_id HAVING count(*) > 1)";
        }

        Cursor c = resolver.query(Contacts.CONTENT_URI, projection, selection, null,
                Contacts.DISPLAY_NAME);
        while(c.moveToNext()) {
            long contactId = c.getLong(0);
            Log.i("Contact   ", String.format("%5d %s", contactId, c.getString(1)));
            dumpRawContacts(resolver, contactId);
            Log.i("          ", ".");
        }
        c.close();
    }

    private static void dumpRawContacts(ContentResolver resolver, long contactId) {
        String[] projection = new String[] {
                RawContacts._ID,
        };
        Cursor c = resolver.query(RawContacts.CONTENT_URI, projection, RawContacts.CONTACT_ID + "="
                + contactId, null, null);
        while(c.moveToNext()) {
            long rawContactId = c.getLong(0);
            Log.i("RawContact", String.format("      %-5d", rawContactId));
            dumpData(resolver, rawContactId);
        }
        c.close();
    }

    private static void dumpData(ContentResolver resolver, long rawContactId) {
        String[] projection = new String[] {
                Data.MIMETYPE,
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
        };
        Cursor c = resolver.query(Data.CONTENT_URI, projection, Data.RAW_CONTACT_ID + "="
                + rawContactId, null, Data.MIMETYPE);
        while(c.moveToNext()) {
            String mimetype = c.getString(0);
            if (Photo.CONTENT_ITEM_TYPE.equals(mimetype)) {
                Log.i("Photo     ", "");
            } else {
                mimetype = mimetype.substring(mimetype.indexOf('/') + 1);
                Log.i("Data      ", String.format("            %-10s %s,%s,%s", mimetype,
                        c.getString(1), c.getString(2), c.getString(3)));
            }
        }
        c.close();
    }

    protected void assertNetworkNotified(boolean expected) {
        assertEquals(expected, (getContactsProvider()).isNetworkNotified());
    }

    protected void assertProjection(Uri uri, String[] expectedProjection) {
        Cursor cursor = mResolver.query(uri, null, "0", null, null);
        String[] actualProjection = cursor.getColumnNames();
        MoreAsserts.assertEquals("Incorrect projection for URI: " + uri,
                Sets.newHashSet(expectedProjection), Sets.newHashSet(actualProjection));
        cursor.close();
    }

    protected void assertRowCount(int expectedCount, Uri uri, String selection, String[] args) {
        Cursor cursor = mResolver.query(uri, null, selection, args, null);

        try {
            assertEquals(expectedCount, cursor.getCount());
        } catch (Error e) {
            TestUtils.dumpCursor(cursor);
            throw e;
        } finally {
            cursor.close();
        }
    }

    /**
     * A contact in the database, and the attributes used to create it.  Construct using
     * {@link GoldenContactBuilder#build()}.
     */
    public final class GoldenContact {

        private final long rawContactId;

        private final long contactId;

        private final String givenName;

        private final String familyName;

        private final String nickname;

        private final byte[] photo;

        private final String company;

        private final String title;

        private final String phone;

        private final String email;

        private GoldenContact(GoldenContactBuilder builder, long rawContactId, long contactId) {

            this.rawContactId = rawContactId;
            this.contactId = contactId;
            givenName = builder.givenName;
            familyName = builder.familyName;
            nickname = builder.nickname;
            photo = builder.photo;
            company = builder.company;
            title = builder.title;
            phone = builder.phone;
            email = builder.email;
        }

        public void delete() {
            Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
            mResolver.delete(rawContactUri, null, null);
        }

        /**
         * Returns the index of the contact in table "raw_contacts"
         */
        public long getRawContactId() {
            return rawContactId;
        }

        /**
         * Returns the index of the contact in table "contacts"
         */
        public long getContactId() {
            return contactId;
        }

        /**
         * Returns the lookup key for the contact.
         */
        public String getLookupKey() {
            return queryLookupKey(contactId);
        }

        /**
         * Returns the contact's given name.
         */
        public String getGivenName() {
            return givenName;
        }

        /**
         * Returns the contact's family name.
         */
        public String getFamilyName() {
            return familyName;
        }

        /**
         * Returns the contact's nickname.
         */
        public String getNickname() {
            return nickname;
        }

        /**
         * Return's the contact's photo
         */
        public byte[] getPhoto() {
            return photo;
        }

        /**
         * Return's the company at which the contact works.
         */
        public String getCompany() {
            return company;
        }

        /**
         * Returns the contact's job title.
         */
        public String getTitle() {
            return title;
        }

        /**
         * Returns the contact's phone number
         */
        public String getPhone() {
            return phone;
        }

        /**
         * Returns the contact's email address
         */
        public String getEmail() {
            return email;
        }
     }

    /**
     * Builds {@link GoldenContact} objects.  Unspecified boolean objects default to false.
     * Unspecified String objects default to null.
     */
    public final class GoldenContactBuilder {

        private String givenName;

        private String familyName;

        private String nickname;

        private byte[] photo;

        private String company;

        private String title;

        private String phone;

        private String email;

        /**
         * The contact's given and family names.
         *
         * TODO(dplotnikov): inline, or should we require them to set both names if they set either?
         */
        public GoldenContactBuilder name(String givenName, String familyName) {
            return givenName(givenName).familyName(familyName);
        }

        /**
         * The contact's given name.
         */
        public GoldenContactBuilder givenName(String value) {
            givenName = value;
            return this;
        }

        /**
         * The contact's family name.
         */
        public GoldenContactBuilder familyName(String value) {
            familyName = value;
            return this;
        }

        /**
         * The contact's nickname.
         */
        public GoldenContactBuilder nickname(String value) {
            nickname = value;
            return this;
        }

        /**
         * The contact's photo.
         */
        public GoldenContactBuilder photo(byte[] value) {
            photo = value;
            return this;
        }

        /**
         * The company at which the contact works.
         */
        public GoldenContactBuilder company(String value) {
            company = value;
            return this;
        }

        /**
         * The contact's job title.
         */
        public GoldenContactBuilder title(String value) {
            title = value;
            return this;
        }

        /**
         * The contact's phone number.
         */
        public GoldenContactBuilder phone(String value) {
            phone = value;
            return this;
        }

        /**
         * The contact's email address; also sets their IM status to {@link StatusUpdates#OFFLINE}
         * with a presence of "Coding for Android".
         */
        public GoldenContactBuilder email(String value) {
            email = value;
            return this;
        }

        /**
         * Builds the {@link GoldenContact} specified by this builder.
         */
        public GoldenContact build() {

            final long groupId = createGroup(mAccount, "gsid1", "title1");

            long rawContactId = RawContactUtil.createRawContact(mResolver);
            insertGroupMembership(rawContactId, groupId);

            if (givenName != null || familyName != null) {
                DataUtil.insertStructuredName(mResolver, rawContactId, givenName, familyName);
            }
            if (nickname != null) {
                insertNickname(rawContactId, nickname);
            }
            if (photo != null) {
                insertPhoto(rawContactId);
            }
            if (company != null || title != null) {
                insertOrganization(rawContactId);
            }
            if (email != null) {
                insertEmail(rawContactId);
            }
            if (phone != null) {
                insertPhone(rawContactId);
            }

            long contactId = queryContactId(rawContactId);

            return new GoldenContact(this, rawContactId, contactId);
        }

        private void insertPhoto(long rawContactId) {
            ContentValues values = new ContentValues();
            values.put(Data.RAW_CONTACT_ID, rawContactId);
            values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
            values.put(Photo.PHOTO, photo);
            mResolver.insert(Data.CONTENT_URI, values);
        }

        private void insertOrganization(long rawContactId) {

            ContentValues values = new ContentValues();
            values.put(Data.RAW_CONTACT_ID, rawContactId);
            values.put(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
            values.put(Organization.TYPE, Organization.TYPE_WORK);
            if (company != null) {
                values.put(Organization.COMPANY, company);
            }
            if (title != null) {
                values.put(Organization.TITLE, title);
            }
            mResolver.insert(Data.CONTENT_URI, values);
        }

        private void insertEmail(long rawContactId) {

            ContentValues values = new ContentValues();
            values.put(Data.RAW_CONTACT_ID, rawContactId);
            values.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            values.put(Email.TYPE, Email.TYPE_WORK);
            values.put(Email.DATA, "foo@acme.com");
            mResolver.insert(Data.CONTENT_URI, values);

            int protocol = Im.PROTOCOL_GOOGLE_TALK;

            values.clear();
            values.put(StatusUpdates.PROTOCOL, protocol);
            values.put(StatusUpdates.IM_HANDLE, email);
            values.put(StatusUpdates.IM_ACCOUNT, "foo");
            values.put(StatusUpdates.PRESENCE_STATUS, StatusUpdates.OFFLINE);
            values.put(StatusUpdates.CHAT_CAPABILITY, StatusUpdates.CAPABILITY_HAS_CAMERA);
            values.put(StatusUpdates.PRESENCE_CUSTOM_STATUS, "Coding for Android");
            mResolver.insert(StatusUpdates.CONTENT_URI, values);
        }

        private void insertPhone(long rawContactId) {
            ContentValues values = new ContentValues();
            values.put(Data.RAW_CONTACT_ID, rawContactId);
            values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            values.put(Data.IS_PRIMARY, 1);
            values.put(Phone.TYPE, Phone.TYPE_HOME);
            values.put(Phone.NUMBER, phone);
            mResolver.insert(Data.CONTENT_URI, values);
        }
    }
}
