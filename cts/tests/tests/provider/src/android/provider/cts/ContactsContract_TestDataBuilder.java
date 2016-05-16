/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.provider.cts;

import com.google.android.collect.Lists;
import com.google.android.collect.Sets;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.ComparisonFailure;

/**
 * A test data builder for ContactsContract tests.
 */
public class ContactsContract_TestDataBuilder {
    private ContentProviderClient mProvider;
    private ArrayList<Builder<?>> mCreatedRows = Lists.newArrayList();
    private HashSet<Builder<?>> mLoadedRows = Sets.newHashSet();

    private interface IdQuery {
        String[] COLUMNS = new String[] {
            BaseColumns._ID,
        };

        int _ID = 0;
    }

    public abstract class  Builder<T extends Builder<?>> extends Assert {
        protected ContentValues mValues = new ContentValues();
        private Uri mUri;
        private long mId = -1;
        private Cursor mCursor;

        protected abstract Uri getContentUri();

        public long getId() throws Exception {
            if (mId != -1) {
                return mId;
            }

            assertNotNull("Row has not be inserted or loaded yet", mUri);

            Cursor cursor = mProvider.query(mUri, IdQuery.COLUMNS, null, null, null, null);
            if (cursor != null) {
                try {
                    cursor.moveToFirst();
                    mId = cursor.getInt(IdQuery._ID);
                } finally {
                    cursor.close();
                }

            }
            assertTrue("Could not obtain _ID for URI: " + mUri, mId != -1);
            return mId;
        }

        @SuppressWarnings("unchecked")
        public T setUri(Uri uri) {
            mUri = uri;
            return (T)this;
        }

        public Uri getUri() {
            if (mUri == null) {
                assertTrue("Neither URI nor ID has been specified", mId != -1);
                mUri = ContentUris.withAppendedId(getContentUri(), mId);
            }
            return mUri;
        }

        @SuppressWarnings("unchecked")
        public T with(String key, String value) {
            mValues.put(key, value);
            return (T)this;
        }

        @SuppressWarnings("unchecked")
        public T with(String key, long value) {
            mValues.put(key, value);
            return (T)this;
        }

        @SuppressWarnings("unchecked")
        public T with(String key, byte[] value) {
            mValues.put(key, value);
            return (T)this;
        }

        @SuppressWarnings("unchecked")
        public T insert() throws Exception {
            insertDependent();
            mCreatedRows.add(this);
            return (T)this;
        }

        @SuppressWarnings("unchecked")
        public T insertDependent() throws Exception {
            mUri = mProvider.insert(getContentUri(), mValues);
            assertNotNull("Could not insert a row in " + getContentUri(), mUri);
            mId = ContentUris.parseId(mUri);
            return (T)this;
        }

        public void delete() throws Exception {
            int count = mProvider.delete(getUri(), null, null);
            assertEquals("Wrong number of rows deleted for " + getUri(), 1, count);
        }

        public void deletePermanently() throws Exception {
            Uri uri = getUri().buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                    .build();
            int count = mProvider.delete(uri, null, null);
            assertEquals("Wrong number of rows deleted for " + uri, 1, count);
            mCreatedRows.remove(this);
        }

        @SuppressWarnings("unchecked")
        public T load() throws Exception {
            close();
            mLoadedRows.add(this);

            mCursor = mProvider.query(getUri(), null, null, null, null, null);
            if (mCursor == null || !mCursor.moveToFirst()) {
                return null;
            } else {
                return (T) this;
            }
        }

        @SuppressWarnings("unchecked")
        public T loadUsingValues() throws Exception {
            close();
            mLoadedRows.add(this);

            StringBuilder selection = new StringBuilder();
            ArrayList<String> selectionArgs = Lists.newArrayList();
            Set<Map.Entry<String, Object>> entries = mValues.valueSet();
            for (Map.Entry<String, Object> entry : entries) {
                String column = entry.getKey();
                Object value = entry.getValue();

                if (selection.length() != 0) {
                    selection.append(" AND ");
                }

                selection.append(column);
                if (value == null) {
                    selection.append(" NOT NULL");
                } else {
                    selection.append("=?");
                    selectionArgs.add(value.toString());
                }
            }
            mCursor = mProvider.query(getContentUri(), null,
                    selection.toString(),
                    selectionArgs.toArray(new String[0]), null, null);
            if (mCursor == null || !mCursor.moveToFirst()) {
                fail("No data rows for " + getContentUri() + "[" + mValues.toString() + "]");
            }
            mId = mCursor.getLong(getColumnIndex(BaseColumns._ID));
            return (T)this;
        }

        public long getLong(String columnName) {
            return mCursor.getLong(mCursor.getColumnIndex(columnName));
        }

        public String getString(String columnName) {
            return mCursor.getString(mCursor.getColumnIndex(columnName));
        }

        public void assertColumn(String columnName, long value) {
            assertEquals(value, mCursor.getLong(getColumnIndex(columnName)));
        }

        public void assertColumn(String columnName, String value) {
            assertEquals(value, mCursor.getString(getColumnIndex(columnName)));
        }

        public void assertColumn(String columnName, byte[] value) {
            assertEquals(value, mCursor.getBlob(getColumnIndex(columnName)));
        }

        public void assertBlobColumnNotNull(String columnName) {
            assertNotNull(mCursor.getBlob(getColumnIndex(columnName)));
        }

        public void assertBlobColumnNull(String columnName) {
            assertNull(mCursor.getBlob(getColumnIndex(columnName)));
        }

        public void assertEquals(byte[] expected, byte[] actual) {
            assertEquals(null, expected, actual);
        }

        public void assertEquals(String message, byte[] expected, byte[] actual) {
            if (expected == null && actual == null)
                return;
            if (expected != null && Arrays.equals(actual, expected))
                return;
            throw new ComparisonFailure(message, expected.toString(), actual.toString());
        }

        private int getColumnIndex(String columnName) {
            int index = mCursor.getColumnIndex(columnName);
            assertTrue("No such column: " + columnName +
                    ". Available columns: " + TextUtils.join(", ", mCursor.getColumnNames()),
                    index != -1);
            return index;
        }

        public void close() {
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
            mLoadedRows.remove(this);
        }

    }

    public class TestRawContact extends Builder<TestRawContact> {

        private TestContact mContact;

        @Override
        protected Uri getContentUri() {
            return RawContacts.CONTENT_URI;
        }

        public TestData newDataRow(String mimeType) {
            return new TestData(this, mimeType);
        }

        public long getContactId() {
            return getLong(RawContacts.CONTACT_ID);
        }

        public TestContact getContact() throws Exception {
            if (mContact == null) {
                mContact = new NestedTestContact(this);
            }
            return mContact;
        }
    }

    public class TestContact extends Builder<TestContact> {

        @Override
        protected Uri getContentUri() {
            return Contacts.CONTENT_URI;
        }
    }

    private class NestedTestContact extends TestContact {
        private final TestRawContact mRawContact;

        public NestedTestContact(TestRawContact rawContact) {
            mRawContact = rawContact;
        }

        @Override
        public long getId() throws Exception {
            return mRawContact.getContactId();
        }

        @Override
        public TestContact load() throws Exception {
            return with(Contacts._ID, mRawContact.getContactId()).loadUsingValues();
        }
    }

    public class TestGroup extends Builder<TestGroup> {

        @Override
        protected Uri getContentUri() {
            return Groups.CONTENT_URI;
        }
    }

    public class TestData extends Builder<TestData> {
        private final TestRawContact mRawContact;

        public TestData(TestRawContact rawContact, String mimeType) {
            this.mRawContact = rawContact;
            mValues.put(Data.MIMETYPE, mimeType);
        }

        @Override
        protected Uri getContentUri() {
            return Data.CONTENT_URI;
        }

        @Override
        public TestData insert() throws Exception {
            mValues.put(Data.RAW_CONTACT_ID, mRawContact.getId());
            return super.insertDependent();
        }

        public long getRawContactId() {
            return getLong(Data.RAW_CONTACT_ID);
        }

        public TestRawContact getRawContact() throws Exception {
            return mRawContact;
        }
    }

    public ContactsContract_TestDataBuilder(ContentProviderClient provider) {
        this.mProvider = provider;
    }

    public TestRawContact newRawContact() {
        return new TestRawContact();
    }

    public TestContact newContact() {
        return new TestContact();
    }

    public TestGroup newGroup() {
        return new TestGroup();
    }

    public void cleanup() throws Exception {
        for (Builder<?> builder : new ArrayList<Builder<?>>(mCreatedRows)) {
            builder.deletePermanently();
        }

        for (Builder<?> builder : new HashSet<Builder<?>>(mLoadedRows)) {
            builder.close();
        }
    }

}
