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

package android.content.res.cts;


import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AssetFileDescriptor_AutoCloseInputStreamTest extends AndroidTestCase {
    private static final int FILE_END = -1;
    private static final String FILE_NAME = "testAssertFileDescriptorAutoCloseInputStream";
    private static final byte[] FILE_DATA = new byte[] {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08
            };
    private static final int FILE_LENGTH = FILE_DATA.length;

    private File mFile;
    private AssetFileDescriptor mFd;
    private AssetFileDescriptor.AutoCloseInputStream mInput;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mFile = new File(getContext().getFilesDir(), FILE_NAME);
        FileOutputStream outputStream = new FileOutputStream(mFile);
        outputStream.write(FILE_DATA);
        outputStream.close();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mFd != null) {
            mFd.close();
            mFd = null;
        }
        mFile.delete();
    }

    public void testSkip() throws IOException {
        openInput(0, FILE_LENGTH);
        assertEquals(FILE_DATA[0], mInput.read());
        assertEquals(0, mInput.skip(0));
        assertEquals(FILE_DATA[1], mInput.read());
        assertEquals(3, mInput.skip(3));
        assertEquals(FILE_DATA[5], mInput.read());
        assertEquals(3, mInput.skip(10));
        assertEquals(FILE_END, mInput.read());
    }

    public void testRead() throws IOException {
        openInput(0, FILE_LENGTH);
        for (int i = 0; i < FILE_LENGTH; i++) {
            assertEquals(FILE_DATA[i], mInput.read());
        }
        assertEquals(FILE_END, mInput.read());
    }

    public void testReadPartial() throws IOException {
        long len = 6;
        openInput(0, len);
        for (int i = 0; i < len; i++) {
            assertEquals(FILE_DATA[i], mInput.read());
        }
        assertEquals(FILE_END, mInput.read());
    }

    public void testReadBufferLen() throws IOException {
        openInput(0, FILE_LENGTH);
        byte[] buf = new byte[FILE_LENGTH];
        assertEquals(3, mInput.read(buf, 0, 3));
        assertEquals(3, mInput.read(buf, 3, 3));
        assertEquals(3, mInput.read(buf, 6, 4));
        MoreAsserts.assertEquals(FILE_DATA, buf);
        assertEquals(FILE_END, mInput.read(buf, 0, 4));
    }

    public void testReadBuffer() throws IOException {
        openInput(0, FILE_LENGTH);
        byte[] buf = new byte[6];
        assertEquals(6, mInput.read(buf));
        assertEquals(FILE_DATA[0], buf[0]);
        assertEquals(3, mInput.read(buf));
        assertEquals(FILE_DATA[6], buf[0]);
        assertEquals(FILE_END, mInput.read(buf));
    }

    public void testReadBufferPartial() throws IOException {
        long len = 8;
        openInput(0, len);
        byte[] buf = new byte[6];
        assertEquals(6, mInput.read(buf));
        assertEquals(FILE_DATA[0], buf[0]);
        assertEquals(2, mInput.read(buf));
        assertEquals(FILE_DATA[6], buf[0]);
        assertEquals(FILE_END, mInput.read(buf));
    }

    public void testAvailableRead() throws IOException {
        openInput(0, FILE_LENGTH);
        assertEquals(FILE_LENGTH, mInput.available());
        assertEquals(FILE_DATA[0], mInput.read());
        assertEquals(FILE_LENGTH -1 , mInput.available());
    }

    public void testAvailableReadBuffer() throws IOException {
        openInput(0, FILE_LENGTH);
        byte[] buf = new byte[3];
        assertEquals(FILE_LENGTH, mInput.available());
        assertEquals(buf.length, mInput.read(buf));
        assertEquals(FILE_LENGTH - buf.length, mInput.available());
    }

    public void testAvailableReadBufferLen() throws IOException {
        openInput(0, FILE_LENGTH);
        byte[] buf = new byte[3];
        assertEquals(FILE_LENGTH, mInput.available());
        assertEquals(2, mInput.read(buf, 0, 2));
        assertEquals(FILE_LENGTH - 2, mInput.available());
    }

    /*
     * Tests that AutoInputStream doesn't support mark().
     */
    public void testMark() throws IOException {
        openInput(0, FILE_LENGTH);
        assertFalse(mInput.markSupported());
        assertEquals(FILE_DATA[0], mInput.read());
        mInput.mark(FILE_LENGTH);  // should do nothing
        assertEquals(FILE_DATA[1], mInput.read());
        mInput.reset();  // should do nothing
        assertEquals(FILE_DATA[2], mInput.read());
    }

    private void openInput(long startOffset, long length)
            throws IOException {
        if (mFd != null) {
            mFd.close();
            mFd = null;
        }
        ParcelFileDescriptor fd =
                ParcelFileDescriptor.open(mFile, ParcelFileDescriptor.MODE_READ_WRITE);
        mFd = new AssetFileDescriptor(fd, startOffset, length);
        mInput = new AssetFileDescriptor.AutoCloseInputStream(mFd);
    }
}
