/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package android.content.cts;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;

import com.android.cts.stub.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Test {@link ContentProvider}.
 */
public class ContentProviderTest extends AndroidTestCase {
    private static final String TEST_PACKAGE_NAME = "com.android.cts.stub";
    private static final String TEST_FILE_NAME = "testFile.tmp";
    private static final String TEST_DB_NAME = "test.db";

    @Override
    protected void tearDown() throws Exception {
        mContext.deleteDatabase(TEST_DB_NAME);
        mContext.deleteFile(TEST_FILE_NAME);
        super.tearDown();
    }

    public void testOpenAssetFile() throws IOException {
        MockContentProvider mockContentProvider = new MockContentProvider();
        Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + TEST_PACKAGE_NAME + "/" + R.raw.testimage);

        try {
            mockContentProvider.openAssetFile(uri, "r");
            fail("Should always throw out FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }

        try {
            mockContentProvider.openFile(null, null);
            fail("Should always throw out FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }
    }

    public void testAttachInfo() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        ProviderInfo info1 = new ProviderInfo();
        info1.readPermission = "android.permission.READ_SMS";
        info1.writePermission = "android.permission.WRITE_SMS";
        mockContentProvider.attachInfo(getContext(), info1);
        assertSame(getContext(), mockContentProvider.getContext());
        assertEquals(info1.readPermission, mockContentProvider.getReadPermission());
        assertEquals(info1.writePermission, mockContentProvider.getWritePermission());

        ProviderInfo info2 = new ProviderInfo();
        info2.readPermission = "android.permission.READ_CONTACTS";
        info2.writePermission = "android.permission.WRITE_CONTACTS";
        mockContentProvider.attachInfo(null, info2);
        assertSame(getContext(), mockContentProvider.getContext());
        assertEquals(info1.readPermission, mockContentProvider.getReadPermission());
        assertEquals(info1.writePermission, mockContentProvider.getWritePermission());

        mockContentProvider = new MockContentProvider();
        mockContentProvider.attachInfo(null, null);
        assertNull(mockContentProvider.getContext());
        assertNull(mockContentProvider.getReadPermission());
        assertNull(mockContentProvider.getWritePermission());

        mockContentProvider.attachInfo(null, info2);
        assertNull(mockContentProvider.getContext());
        assertEquals(info2.readPermission, mockContentProvider.getReadPermission());
        assertEquals(info2.writePermission, mockContentProvider.getWritePermission());

        mockContentProvider.attachInfo(getContext(), info1);
        assertSame(getContext(), mockContentProvider.getContext());
        assertEquals(info1.readPermission, mockContentProvider.getReadPermission());
        assertEquals(info1.writePermission, mockContentProvider.getWritePermission());
    }

    public void testBulkInsert() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        int count = 2;
        ContentValues[] values = new ContentValues[count];
        for (int i = 0; i < count; i++) {
            values[i] = new ContentValues();
        }
        Uri uri = Uri.parse("content://browser/bookmarks");
        assertEquals(count, mockContentProvider.bulkInsert(uri, values));
        assertEquals(count, mockContentProvider.getInsertCount());

        mockContentProvider = new MockContentProvider();
        try {
            assertEquals(count, mockContentProvider.bulkInsert(null, values));
        } finally {
            assertEquals(count, mockContentProvider.getInsertCount());
        }
    }

    public void testGetContext() {
        MockContentProvider mockContentProvider = new MockContentProvider();
        assertNull(mockContentProvider.getContext());

        mockContentProvider.attachInfo(getContext(), null);
        assertSame(getContext(), mockContentProvider.getContext());
        mockContentProvider.attachInfo(null, null);
        assertSame(getContext(), mockContentProvider.getContext());
    }

    public void testAccessReadPermission() {
        MockContentProvider mockContentProvider = new MockContentProvider();
        assertNull(mockContentProvider.getReadPermission());

        String expected = "android.permission.READ_CONTACTS";
        mockContentProvider.setReadPermissionWrapper(expected);
        assertEquals(expected, mockContentProvider.getReadPermission());

        expected = "android.permission.READ_SMS";
        mockContentProvider.setReadPermissionWrapper(expected);
        assertEquals(expected, mockContentProvider.getReadPermission());

        mockContentProvider.setReadPermissionWrapper(null);
        assertNull(mockContentProvider.getReadPermission());
    }

    public void testAccessWritePermission() {
        MockContentProvider mockContentProvider = new MockContentProvider();
        assertNull(mockContentProvider.getWritePermission());

        String expected = "android.permission.WRITE_CONTACTS";
        mockContentProvider.setWritePermissionWrapper(expected);
        assertEquals(expected, mockContentProvider.getWritePermission());

        expected = "android.permission.WRITE_SMS";
        mockContentProvider.setWritePermissionWrapper(expected);
        assertEquals(expected, mockContentProvider.getWritePermission());

        mockContentProvider.setWritePermissionWrapper(null);
        assertNull(mockContentProvider.getWritePermission());
    }

    public void testIsTemporary() {
        MockContentProvider mockContentProvider = new MockContentProvider();
        assertFalse(mockContentProvider.isTemporary());
    }

    public void testOpenFile() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        try {
            Uri uri = Uri.parse("content://test");
            mockContentProvider.openFile(uri, "r");
            fail("Should always throw out FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }

        try {
            mockContentProvider.openFile(null, null);
            fail("Should always throw out FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }
    }

    public void testOpenFileHelper() throws IOException {

        // create a temporary File
        mContext.openFileOutput(TEST_FILE_NAME, Context.MODE_PRIVATE).close();
        File file = mContext.getFileStreamPath(TEST_FILE_NAME);
        assertTrue(file.exists());

        ContentProvider cp = new OpenFileContentProvider(file.getAbsolutePath(), TEST_DB_NAME);

        Uri uri = Uri.parse("content://test");
        assertNotNull(cp.openFile(uri, "r"));

        try {
            uri = Uri.parse("content://test");
            cp.openFile(uri, "wrong");
            fail("Should throw IllegalArgumentException for bad mode!");
        } catch (IllegalArgumentException e) {
        }

        // delete the temporary file
        file.delete();

        try {
            uri = Uri.parse("content://test");
            cp.openFile(uri, "r");
            fail("Should throw FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }

        try {
            cp.openFile((Uri) null, "r");
            fail("Should always throw FileNotFoundException!");
        } catch (FileNotFoundException e) {
        }
    }

    public void testOnConfigurationChanged() {
        // cannot trigger this callback reliably
    }

    public void testOnLowMemory() {
        // cannot trigger this callback reliably
    }

    public void testGetIContentProvider() {
        MockContentProvider mockContentProvider = new MockContentProvider();

        assertNotNull(mockContentProvider.getIContentProvider());
    }

    private class MockContentProvider extends ContentProvider {
        private int mInsertCount = 0;

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            mInsertCount++;
            return null;
        }

        public int getInsertCount() {
            return mInsertCount;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection,
                String[] selectionArgs, String sortOrder) {
            return null;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection,
                String[] selectionArgs) {
            return 0;
        }

        @Override
        public boolean onCreate() {
            return false;
        }

        // wrapper or override for the protected methods
        public void setReadPermissionWrapper(String permission) {
            super.setReadPermission(permission);
        }

        public void setWritePermissionWrapper(String permission) {
            super.setWritePermission(permission);
        }

        @Override
        protected boolean isTemporary() {
            return super.isTemporary();
        }

        public ParcelFileDescriptor openFileHelperWrapper(Uri uri, String mode)
                throws FileNotFoundException {
            return super.openFileHelper(uri, mode);
        }
    }

    /**
     * This provider implements openFile() using ContentProvider.openFileHelper().
     */
    private class OpenFileContentProvider extends ContentProvider {
        private SQLiteDatabase mDb;

        OpenFileContentProvider(String fileName, String dbName) {
            // delete the database if it already exists
            mContext.deleteDatabase(dbName);
            mDb = mContext.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);
            mDb.execSQL("CREATE TABLE files ( _data TEXT );");
            mDb.execSQL("INSERT INTO files VALUES ( \"" + fileName + "\");");
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public String getType(Uri uri) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                String sortOrder) {
            return mDb.query("files", projection, selection, selectionArgs, null, null, null);
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
            return openFileHelper(uri, mode);
        }
    }
}
