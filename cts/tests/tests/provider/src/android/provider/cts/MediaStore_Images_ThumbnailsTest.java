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

package android.provider.cts;

import com.android.cts.stub.R;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Images.Thumbnails;
import android.test.InstrumentationTestCase;

import java.io.File;
import java.util.ArrayList;

public class MediaStore_Images_ThumbnailsTest extends InstrumentationTestCase {
    private ArrayList<Uri> mRowsAdded;

    private Context mContext;

    private ContentResolver mContentResolver;

    private FileCopyHelper mHelper;

    @Override
    protected void tearDown() throws Exception {
        for (Uri row : mRowsAdded) {
            try {
                mContentResolver.delete(row, null, null);
            } catch (UnsupportedOperationException e) {
                // There is no way to delete rows from table "thumbnails" of internals database.
                // ignores the exception and make the loop goes on
            }
        }

        mHelper.clear();
        super.tearDown();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = getInstrumentation().getTargetContext();
        mContentResolver = mContext.getContentResolver();

        mHelper = new FileCopyHelper(mContext);
        mRowsAdded = new ArrayList<Uri>();

        String campath = Environment.getExternalStorageDirectory() + File.separator +
                Environment.DIRECTORY_DCIM + File.separator + "Camera";
        File camfile = new File(campath);
        if (!camfile.exists()) {
            assertTrue("failed to create " + campath, camfile.mkdir());
        }
    }

    public void testQueryInternalThumbnails() throws Exception {
        Cursor c = Thumbnails.queryMiniThumbnails(mContentResolver,
                Thumbnails.INTERNAL_CONTENT_URI, Thumbnails.MICRO_KIND, null);
        int previousMicroKindCount = c.getCount();
        c.close();

        // add a thumbnail
        String path = mHelper.copy(R.raw.scenery, "testThumbnails.jpg");
        ContentValues values = new ContentValues();
        values.put(Thumbnails.KIND, Thumbnails.MINI_KIND);
        values.put(Thumbnails.DATA, path);
        Uri uri = mContentResolver.insert(Thumbnails.INTERNAL_CONTENT_URI, values);
        if (uri != null) {
            mRowsAdded.add(uri);
        }

        // query with the uri of the thumbnail and the kind
        c = Thumbnails.queryMiniThumbnails(mContentResolver, uri, Thumbnails.MINI_KIND, null);
        c.moveToFirst();
        assertEquals(1, c.getCount());
        assertEquals(Thumbnails.MINI_KIND, c.getInt(c.getColumnIndex(Thumbnails.KIND)));
        assertEquals(path, c.getString(c.getColumnIndex(Thumbnails.DATA)));

        // query all thumbnails with other kind
        c = Thumbnails.queryMiniThumbnails(mContentResolver, Thumbnails.INTERNAL_CONTENT_URI,
                Thumbnails.MICRO_KIND, null);
        assertEquals(previousMicroKindCount, c.getCount());
        c.close();

        // query without kind
        c = Thumbnails.query(mContentResolver, uri, null);
        assertEquals(1, c.getCount());
        c.moveToFirst();
        assertEquals(Thumbnails.MINI_KIND, c.getInt(c.getColumnIndex(Thumbnails.KIND)));
        assertEquals(path, c.getString(c.getColumnIndex(Thumbnails.DATA)));
        c.close();
    }

    public void testQueryExternalMiniThumbnails() {
        // insert the image by bitmap
        Bitmap src = BitmapFactory.decodeResource(mContext.getResources(), R.raw.scenery);
        String stringUrl = null;
        try{
            stringUrl = Media.insertImage(mContentResolver, src, null, null);
        } catch (UnsupportedOperationException e) {
            // the tests will be aborted because the image will be put in sdcard
            fail("There is no sdcard attached! " + e.getMessage());
        }
        assertNotNull(stringUrl);
        mRowsAdded.add(Uri.parse(stringUrl));

        // get the original image id and path
        Cursor c = mContentResolver.query(Uri.parse(stringUrl),
                new String[]{ Media._ID, Media.DATA }, null, null, null);
        c.moveToFirst();
        long imageId = c.getLong(c.getColumnIndex(Media._ID));
        String imagePath = c.getString(c.getColumnIndex(Media.DATA));
        c.close();

        assertTrue("image file does not exist", new File(imagePath).exists());

        String[] sizeProjection = new String[] { Thumbnails.WIDTH, Thumbnails.HEIGHT };
        c = Thumbnails.queryMiniThumbnail(mContentResolver, imageId, Thumbnails.MINI_KIND,
                sizeProjection);
        assertEquals(1, c.getCount());
        assertTrue(c.moveToFirst());
        assertTrue(c.getLong(c.getColumnIndex(Thumbnails.WIDTH)) >= Math.min(src.getWidth(), 240));
        assertTrue(c.getLong(c.getColumnIndex(Thumbnails.HEIGHT)) >= Math.min(src.getHeight(), 240));
        c.close();
        c = Thumbnails.queryMiniThumbnail(mContentResolver, imageId, Thumbnails.MICRO_KIND,
                sizeProjection);
        assertEquals(1, c.getCount());
        assertTrue(c.moveToFirst());
        assertEquals(50, c.getLong(c.getColumnIndex(Thumbnails.WIDTH)));
        assertEquals(50, c.getLong(c.getColumnIndex(Thumbnails.HEIGHT)));
        c.close();

        c = Thumbnails.queryMiniThumbnail(mContentResolver, imageId, Thumbnails.MINI_KIND,
                new String[] { Thumbnails._ID, Thumbnails.DATA, Thumbnails.IMAGE_ID});

        c.moveToNext();
        long img = c.getLong(c.getColumnIndex(Thumbnails.IMAGE_ID));
        assertEquals(imageId, img);
        String thumbPath = c.getString(c.getColumnIndex(Thumbnails.DATA));
        assertTrue("thumbnail file does not exist", new File(thumbPath).exists());

        // deleting the image from the database also deletes the image file, and the
        // corresponding entry in the thumbnail table, which in turn triggers deletion
        // of the thumbnail file on disk
        mContentResolver.delete(Uri.parse(stringUrl), null, null);
        mRowsAdded.remove(stringUrl);

        assertFalse("image file should no longer exist", new File(imagePath).exists());

        Cursor c2 = Thumbnails.queryMiniThumbnail(mContentResolver, imageId, Thumbnails.MINI_KIND,
                new String[] { Thumbnails._ID, Thumbnails.DATA, Thumbnails.IMAGE_ID});
        assertEquals(0, c2.getCount());
        c2.close();

        assertFalse("thumbnail file should no longer exist", new File(thumbPath).exists());
        c.close();

        // insert image, then delete it via the files table
        stringUrl = Media.insertImage(mContentResolver, src, null, null);
        c = mContentResolver.query(Uri.parse(stringUrl),
                new String[]{ Media._ID, Media.DATA}, null, null, null);
        c.moveToFirst();
        imageId = c.getLong(c.getColumnIndex(Media._ID));
        imagePath = c.getString(c.getColumnIndex(Media.DATA));
        c.close();
        assertTrue("image file does not exist", new File(imagePath).exists());
        Uri fileuri = MediaStore.Files.getContentUri("external", imageId);
        mContentResolver.delete(fileuri, null, null);
        assertFalse("image file should no longer exist", new File(imagePath).exists());


        // insert image, then delete its thumbnail
        stringUrl = Media.insertImage(mContentResolver, src, null, null);
        c = mContentResolver.query(Uri.parse(stringUrl),
                new String[]{ Media._ID, Media.DATA}, null, null, null);
        c.moveToFirst();
        imageId = c.getLong(c.getColumnIndex(Media._ID));
        imagePath = c.getString(c.getColumnIndex(Media.DATA));
        c.close();
        c2 = Thumbnails.queryMiniThumbnail(mContentResolver, imageId, Thumbnails.MINI_KIND,
                new String[] { Thumbnails._ID, Thumbnails.DATA, Thumbnails.IMAGE_ID});
        c2.moveToFirst();
        thumbPath = c2.getString(c2.getColumnIndex(Thumbnails.DATA));
        assertTrue("thumbnail file does not exist", new File(thumbPath).exists());

        Uri imguri = Uri.parse(stringUrl);
        long imgid = ContentUris.parseId(imguri);
        assertEquals(imgid, imageId);
        mRowsAdded.add(imguri);
        mContentResolver.delete(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                MediaStore.Images.Thumbnails.IMAGE_ID + "=?", new String[]{ "" + imgid});
        assertFalse("thumbnail file should no longer exist", new File(thumbPath).exists());
        assertTrue("image file should still exist", new File(imagePath).exists());
    }

    public void testGetContentUri() {
        Cursor c = null;
        assertNotNull(c = mContentResolver.query(Thumbnails.getContentUri("internal"), null, null,
                null, null));
        c.close();
        assertNotNull(c = mContentResolver.query(Thumbnails.getContentUri("external"), null, null,
                null, null));
        c.close();

        // can not accept any other volume names
        String volume = "fakeVolume";
        assertNull(mContentResolver.query(Thumbnails.getContentUri(volume), null, null, null,
                null));
    }

    public void testStoreImagesMediaExternal() {
        final String externalImgPath = Environment.getExternalStorageDirectory() +
                "/testimage.jpg";
        final String externalImgPath2 = Environment.getExternalStorageDirectory() +
                "/testimage1.jpg";
        ContentValues values = new ContentValues();
        values.put(Thumbnails.KIND, Thumbnails.FULL_SCREEN_KIND);
        values.put(Thumbnails.IMAGE_ID, 0);
        values.put(Thumbnails.HEIGHT, 480);
        values.put(Thumbnails.WIDTH, 320);
        values.put(Thumbnails.DATA, externalImgPath);

        // insert
        Uri uri = mContentResolver.insert(Thumbnails.EXTERNAL_CONTENT_URI, values);
        assertNotNull(uri);

        // query
        Cursor c = mContentResolver.query(uri, null, null, null, null);
        assertEquals(1, c.getCount());
        c.moveToFirst();
        long id = c.getLong(c.getColumnIndex(Thumbnails._ID));
        assertTrue(id > 0);
        assertEquals(Thumbnails.FULL_SCREEN_KIND, c.getInt(c.getColumnIndex(Thumbnails.KIND)));
        assertEquals(0, c.getLong(c.getColumnIndex(Thumbnails.IMAGE_ID)));
        assertEquals(480, c.getInt(c.getColumnIndex(Thumbnails.HEIGHT)));
        assertEquals(320, c.getInt(c.getColumnIndex(Thumbnails.WIDTH)));
        assertEquals(externalImgPath, c.getString(c.getColumnIndex(Thumbnails.DATA)));
        c.close();

        // update
        values.clear();
        values.put(Thumbnails.KIND, Thumbnails.MICRO_KIND);
        values.put(Thumbnails.IMAGE_ID, 1);
        values.put(Thumbnails.HEIGHT, 50);
        values.put(Thumbnails.WIDTH, 50);
        values.put(Thumbnails.DATA, externalImgPath2);
        assertEquals(1, mContentResolver.update(uri, values, null, null));

        // delete
        assertEquals(1, mContentResolver.delete(uri, null, null));
    }

    public void testStoreImagesMediaInternal() {
        // can not insert any data, so other operations can not be tested
        try {
            mContentResolver.insert(Thumbnails.INTERNAL_CONTENT_URI, new ContentValues());
            fail("Should throw UnsupportedOperationException when inserting into internal "
                    + "database");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
