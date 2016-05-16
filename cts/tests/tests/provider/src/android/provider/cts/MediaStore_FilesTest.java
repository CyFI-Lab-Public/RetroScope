/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.cts.stub.R;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MediaStore_FilesTest extends AndroidTestCase {

    private ContentResolver mResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = mContext.getContentResolver();
    }

    public void testGetContentUri() {
        String volumeName = MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME;
        Uri allFilesUri = MediaStore.Files.getContentUri(volumeName);

        // Get the current file count. We will check if this increases after
        // adding a file to the provider.
        int fileCount = getFileCount(allFilesUri);

        // Check that inserting empty values causes an exception.
        ContentValues values = new ContentValues();
        try {
            mResolver.insert(allFilesUri, values);
            fail("Should throw an exception");
        } catch (IllegalArgumentException e) {
            // Expecting an exception
        }

        // Add a path for a file and check that the returned uri appends a
        // path properly.
        String dataPath = "does_not_really_exist.txt";
        values.put(MediaColumns.DATA, dataPath);
        Uri fileUri = mResolver.insert(allFilesUri, values);
        long fileId = ContentUris.parseId(fileUri);
        assertEquals(fileUri, ContentUris.withAppendedId(allFilesUri, fileId));

        // Check that getContentUri with the file id produces the same url
        Uri rowUri = MediaStore.Files.getContentUri(volumeName, fileId);
        assertEquals(fileUri, rowUri);

        // Check that the file count has increased.
        int newFileCount = getFileCount(allFilesUri);
        assertEquals(fileCount + 1, newFileCount);

        // Check that the path we inserted was stored properly.
        assertStringColumn(fileUri, MediaColumns.DATA, dataPath);

        // Update the path and check that the database changed.
        String updatedPath = "still_does_not_exist.txt";
        values.put(MediaColumns.DATA, updatedPath);
        assertEquals(1, mResolver.update(fileUri, values, null, null));
        assertStringColumn(fileUri, MediaColumns.DATA, updatedPath);

        // check that inserting a duplicate entry fails
        Uri foo = mResolver.insert(allFilesUri, values);
        assertNull(foo);

        // Delete the file and observe that the file count decreased.
        assertEquals(1, mResolver.delete(fileUri, null, null));
        assertEquals(fileCount, getFileCount(allFilesUri));

        // Make sure the deleted file is not returned by the cursor.
        Cursor cursor = mResolver.query(fileUri, null, null, null, null);
        try {
            assertFalse(cursor.moveToNext());
        } finally {
            cursor.close();
        }

        // insert file and check its parent
        values.clear();
        try {
            String b = mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getCanonicalPath();
            values.put(MediaColumns.DATA, b + "/testing");
            fileUri = mResolver.insert(allFilesUri, values);
            cursor = mResolver.query(fileUri, new String[] { MediaStore.Files.FileColumns.PARENT },
                    null, null, null);
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            long parentid = cursor.getLong(0);
            assertTrue("got 0 parent for non root file", parentid != 0);

            cursor.close();
            cursor = mResolver.query(ContentUris.withAppendedId(allFilesUri, parentid),
                    new String[] { MediaColumns.DATA }, null, null, null);
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            String parentPath = cursor.getString(0);
            assertEquals(b, parentPath);

            mResolver.delete(fileUri, null, null);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            cursor.close();
        }
    }

    public void testCaseSensitivity() throws IOException {
        String fileDir = Environment.getExternalStorageDirectory() +
                "/" + getClass().getCanonicalName();
        String fileName = fileDir + "/Test.Mp3";
        writeFile(R.raw.testmp3, fileName);

        String volumeName = MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME;
        Uri allFilesUri = MediaStore.Files.getContentUri(volumeName);
        ContentValues values = new ContentValues();
        values.put(MediaColumns.DATA, fileDir + "/test.mp3");
        Uri fileUri = mResolver.insert(allFilesUri, values);
        try {
            ParcelFileDescriptor pfd = mResolver.openFileDescriptor(fileUri, "r");
            pfd.close();
        } finally {
            mResolver.delete(fileUri, null, null);
            new File(fileName).delete();
            new File(fileDir).delete();
        }
    }

    public void testAccess() throws IOException {
        // clean up from previous run
        mResolver.delete(MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                "_data NOT LIKE ?", new String[] { "/system/%" } );

        // insert some dummy starter data into the provider
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "My Bitmap");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATA, "/foo/bar/dummy.jpg");
        Uri uri = mResolver.insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values);

        // point _data at directory and try to get an fd for it
        values = new ContentValues();
        values.put("_data", "/data/media");
        mResolver.update(uri, values, null, null);
        ParcelFileDescriptor pfd = null;
        try {
            pfd = mResolver.openFileDescriptor(uri, "r");
            pfd.close();
            fail("shouldn't be here");
        } catch (FileNotFoundException e) {
            // expected
        }

        // try to create a file in a place we don't have access to
        values = new ContentValues();
        values.put("_data", "/data/media/test.dat");
        mResolver.update(uri, values, null, null);
        try {
            pfd = mResolver.openFileDescriptor(uri, "w");
            pfd.close();
            fail("shouldn't be here");
        } catch (FileNotFoundException e) {
            // expected
        }
        // read file back
        try {
            pfd = mResolver.openFileDescriptor(uri, "r");
            pfd.close();
            fail("shouldn't be here");
        } catch (FileNotFoundException e) {
            // expected
        }

        // point _data at media database and read it
        values = new ContentValues();
        values.put("_data", "/data/data/com.android.providers.media/databases/internal.db");
        mResolver.update(uri, values, null, null);
        try {
            pfd = mResolver.openFileDescriptor(uri, "r");
            pfd.close();
            fail("shouldn't be here");
        } catch (FileNotFoundException e) {
            // expected
        }

        // Insert a private file into the database. Since it's private, the media provider won't
        // be able to open it
        FileOutputStream fos = mContext.openFileOutput("dummy.dat", Context.MODE_PRIVATE);
        fos.write(0);
        fos.close();
        File path = mContext.getFileStreamPath("dummy.dat");
        values = new ContentValues();
        values.put("_data", path.getAbsolutePath());

        mResolver.update(uri, values, null, null);
        try {
            pfd = mResolver.openFileDescriptor(uri, "r");
            pfd.close();
            fail("shouldn't be here");
        } catch (FileNotFoundException e) {
            // expected
        }
        // now make the file world-readable
        fos = mContext.openFileOutput("dummy.dat", Context.MODE_WORLD_READABLE);
        fos.write(0);
        fos.close();
        try {
            pfd = mResolver.openFileDescriptor(uri, "r");
            pfd.close();
        } catch (FileNotFoundException e) {
            fail("failed to open file");
        }
        path.delete();

        File sdfile = null;
        if (Environment.isExternalStorageEmulated()) {
            // create file on sdcard and check access via real path
            String fileDir = Environment.getExternalStorageDirectory() +
                    "/" + getClass().getCanonicalName() + "/test.mp3";
            sdfile = new File(fileDir);
            writeFile(R.raw.testmp3, sdfile.getCanonicalPath());
            assertTrue(sdfile.exists());
            values = new ContentValues();
            values.put("_data", sdfile.getCanonicalPath());
            mResolver.update(uri, values, null, null);
            try {
                pfd = mResolver.openFileDescriptor(uri, "r");

                // get the real path from the file descriptor (this relies on the media provider
                // having opened the path via the real path instead of the emulated path).
                File real = new File("/proc/self/fd/" + pfd.getFd());
                values = new ContentValues();
                values.put("_data", real.getCanonicalPath());
                mResolver.update(uri, values, null, null);
                pfd.close();

                // we shouldn't be able to access this
                try {
                    pfd = mResolver.openFileDescriptor(uri, "r");
                    pfd.close();
                    fail("shouldn't be here");
                } catch (FileNotFoundException e) {
                    // expected
                }
            } catch (FileNotFoundException e) {
                fail("couldn't open file");
            }
        }

        // clean up
        assertEquals(1, mResolver.delete(uri, null, null));
        if (sdfile != null) {
            assertEquals(true, sdfile.delete());
        }
    }

    private void writeFile(int resid, String path) throws IOException {
        File out = new File(path);
        File dir = out.getParentFile();
        dir.mkdirs();
        FileCopyHelper copier = new FileCopyHelper(mContext);
        copier.copyToExternalStorage(resid, out);
    }

    private int getFileCount(Uri uri) {
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    private void assertStringColumn(Uri fileUri, String columnName, String expectedValue) {
        Cursor cursor = mResolver.query(fileUri, null, null, null, null);
        try {
            assertTrue(cursor.moveToNext());
            int index = cursor.getColumnIndexOrThrow(columnName);
            assertEquals(expectedValue, cursor.getString(index));
        } finally {
            cursor.close();
        }
    }
}
