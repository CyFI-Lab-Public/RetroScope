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

package android.os.cts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;
import android.os.MemoryFile;

public class MemoryFileTest extends TestCase {
    MemoryFile mMemoryFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMemoryFile = null;
    }

    public void testConstructor() throws IOException {
        // new the MemoryFile instance
        new MemoryFile("Test File", 1024);
    }

    public void testWriteBytes() throws IOException {
        byte[] data = new byte[512];
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);

        mMemoryFile.writeBytes(data, 0, 0, 512);

        checkWriteBytesInIllegalParameter(-1, 0, 128);
        checkWriteBytesInIllegalParameter(1000, 0, 128);
        checkWriteBytesInIllegalParameter(0, 0, -1);
        checkWriteBytesInIllegalParameter(0, 0, 1024);
        checkWriteBytesInIllegalParameter(0, -1, 512);
        checkWriteBytesInIllegalParameter(0, 2000, 512);
    }

    private void checkWriteBytesInIllegalParameter(int srcOffset, int destOffset, int count)
            throws IOException {
        try {
            byte[] data = new byte[512];
            mMemoryFile.writeBytes(data, srcOffset, destOffset, count);
            fail("MemoryFile should throw IndexOutOfBoundsException here.");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    public void testGetOutputStream() throws IOException {
        byte[] bs = new byte[] { 1, 2, 3, 4 };
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);
        OutputStream out = mMemoryFile.getOutputStream();
        assertNotNull(out);
        out.write(bs);

        InputStream in = mMemoryFile.getInputStream();
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(3, in.read());
        assertEquals(4, in.read());
    }

    public void testAllowPurging() throws IOException {
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);

        assertFalse(mMemoryFile.allowPurging(true));
        byte[] data = new byte[512];
        try {
            mMemoryFile.writeBytes(data, 0, 0, 512);
        } catch (IOException e) {
            // IOException may be thrown here since purging is allowed
        }

        assertTrue(mMemoryFile.isPurgingAllowed());
        assertTrue(mMemoryFile.allowPurging(false));
        mMemoryFile.writeBytes(data, 0, 0, 512);
        assertFalse(mMemoryFile.isPurgingAllowed());
    }

    public void testLength() throws IOException {
        mMemoryFile = new MemoryFile("Test File", 1024);
        assertEquals(1024, mMemoryFile.length());

        mMemoryFile = new MemoryFile("Test File", 512);
        assertEquals(512, mMemoryFile.length());

        mMemoryFile = new MemoryFile("Test File", Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, mMemoryFile.length());

        mMemoryFile = new MemoryFile("Test File", Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, mMemoryFile.length());
    }

    public void testReadBytes() throws IOException {
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);

        byte[] data = new byte[] { 1, 2, 3, 4 };
        mMemoryFile.writeBytes(data, 0, 0, data.length);
        byte[] gotData = new byte[4];
        mMemoryFile.readBytes(gotData, 0, 0, gotData.length);
        for (int i = 0; i < gotData.length; i++) {
            assertEquals(i + 1, gotData[i]);
        }

        checkReadBytesInIllegalParameter(0, -1, 128);

        checkReadBytesInIllegalParameter(0, 1000, 128);

        checkReadBytesInIllegalParameter(0, 0, -1);

        checkReadBytesInIllegalParameter(0, 0, 1024);

        checkReadBytesInIllegalParameter(-1, 0, 512);

        checkReadBytesInIllegalParameter(2000, 0, 512);
    }

    private void checkReadBytesInIllegalParameter(int srcOffset, int destOffset, int count)
            throws IOException {
        try {
            byte[] data = new byte[512];
            mMemoryFile.readBytes(data, srcOffset, destOffset, count);
            fail("MemoryFile should throw IndexOutOfBoundsException here.");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    public void testClose() throws IOException {
        // new the MemoryFile instance
        mMemoryFile = new MemoryFile("Test File", 1024);
        byte[] data = new byte[512];
        mMemoryFile.writeBytes(data, 0, 0, 128);

        mMemoryFile.close();

        data = new byte[512];
        try {
            mMemoryFile.readBytes(data, 0, 0, 128);
            fail("Reading from closed MemoryFile did not throw an exception.");
        } catch (IOException e) {
            // expected, since file is already closed
        }
    }

}
