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

package android.content.res.cts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import android.content.res.AssetFileDescriptor;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;

public class AssetFileDescriptorTest extends AndroidTestCase {
    private static final long START_OFFSET = 0;
    private static final long LENGTH = 100;
    private static final String FILE_NAME = "testAssetFileDescriptor";
    private static final byte[] FILE_DATA =
        new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
    private static final int FILE_END = -1;
    private AssetFileDescriptor mAssetFileDes;
    private File mFile;
    private ParcelFileDescriptor mFd;
    private FileOutputStream mOutputStream;
    private FileInputStream mInputStream;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mFile = new File(getContext().getFilesDir(), FILE_NAME);
        mFile.createNewFile();
        initAssetFileDescriptor();
    }

    private void initAssetFileDescriptor() throws FileNotFoundException {
        mFd = ParcelFileDescriptor.open(mFile, ParcelFileDescriptor.MODE_READ_WRITE);
        mAssetFileDes = new AssetFileDescriptor(mFd, START_OFFSET, LENGTH);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // As {@link AssetFileDescripter#createOutputStream()}
        // and {@link AssetFileDescripter#createInputStream()} doc,
        // the input and output stream will be auto closed when the AssetFileDescriptor closed.
        if (mAssetFileDes != null) {
            mAssetFileDes.close();
        }
        getContext().deleteFile(FILE_NAME);
    }

    public void testInputOutputStream() throws IOException {
        /*
         * test createOutputStream() and createInputStrean()
         * test point
         * 1. createOutputStream() and createInputStrean() should only call this once
         * for a particular asset.
         * 2. outputStream can write and inputStream can read.
         * 3. auto close.
         */
        mOutputStream = mAssetFileDes.createOutputStream();
        assertNotNull(mOutputStream);
        mOutputStream.write(FILE_DATA);
        mOutputStream.flush();
        mOutputStream.close();
        mOutputStream = null;
        try {
            mOutputStream = mAssetFileDes.createOutputStream();
            fail("Should throw IOException");
        } catch (IOException e) {
            // expect
        }
        try {
            mInputStream = mAssetFileDes.createInputStream();
            fail("Should throw IOException");
        } catch (IOException e) {
            // expect
        }
        mAssetFileDes.close();
        mAssetFileDes = null;

        initAssetFileDescriptor();
        mInputStream = mAssetFileDes.createInputStream();
        assertNotNull(mInputStream);
        byte[] dataFromFile = new byte[FILE_DATA.length];
        int readLength = 0;
        int readByte = 0;
        while ((readByte != FILE_END) && (readLength < FILE_DATA.length)) {
            readLength += readByte;
            readByte = mInputStream.read(dataFromFile,
                    readLength, FILE_DATA.length - readLength);
        }
        assertEquals(FILE_DATA.length, readLength);
        assertTrue(Arrays.equals(FILE_DATA, dataFromFile));
        assertEquals(FILE_END, mInputStream.read());
        mInputStream.close();
        mInputStream = null;
        try {
            mInputStream = mAssetFileDes.createInputStream();
            fail("Should throw IOException");
        } catch (IOException e) {
            // expect
        }
        try {
            mOutputStream = mAssetFileDes.createOutputStream();
            fail("Should throw IOException");
        } catch (IOException e) {
            // expect
        }
        mAssetFileDes.close();
        mAssetFileDes = null;

        initAssetFileDescriptor();
        mOutputStream = mAssetFileDes.createOutputStream();
        mAssetFileDes.close();
        mAssetFileDes = null;
        try {
            mOutputStream.write(FILE_DATA);
            fail("Should throw IOException");
        } catch (IOException e) {
            // expect
        }

        initAssetFileDescriptor();
        mInputStream = mAssetFileDes.createInputStream();
        mAssetFileDes.close();
        mAssetFileDes = null;
        try {
            mInputStream.read();
            fail("Should throw IOException");
        } catch (IOException e) {
            // expect
        }
    }

    public void testMiscMethod() {
        // test getLength()
        assertEquals(LENGTH, mAssetFileDes.getLength());

        // test getStartOffset()
        assertEquals(START_OFFSET, mAssetFileDes.getStartOffset());

        // test getParcelFileDescriptor() getFileDescriptor() toString() and describeContents()
        assertSame(mFd, mAssetFileDes.getParcelFileDescriptor());
        assertSame(mFd.getFileDescriptor(), mAssetFileDes.getFileDescriptor());
        assertNotNull(mAssetFileDes.toString());
        assertEquals(mFd.describeContents(), mAssetFileDes.describeContents());

        // test writeToParcel(), test by assert source and out FileDescriptor content equals.
        Parcel parcel = Parcel.obtain();
        mAssetFileDes.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        AssetFileDescriptor out = AssetFileDescriptor.CREATOR.createFromParcel(parcel);
        assertEquals(out.getStartOffset(), mAssetFileDes.getStartOffset());
        assertEquals(out.getDeclaredLength(), mAssetFileDes.getDeclaredLength());
        assertEquals(out.getParcelFileDescriptor().getStatSize(),
                mAssetFileDes.getParcelFileDescriptor().getStatSize());
    }
}
