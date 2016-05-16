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

package android.os.cts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.Parcelable;
import android.os.cts.ParcelFileDescriptorPeer.FutureCloseListener;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

import com.google.common.util.concurrent.AbstractFuture;

import junit.framework.ComparisonFailure;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ParcelFileDescriptorTest extends AndroidTestCase {
    private static final long DURATION = 100l;

    public void testConstructorAndOpen() throws Exception {
        ParcelFileDescriptor tempFile = makeParcelFileDescriptor(getContext());

        ParcelFileDescriptor pfd = new ParcelFileDescriptor(tempFile);
        AutoCloseInputStream in = new AutoCloseInputStream(pfd);
        try {
            // read the data that was wrote previously
            assertEquals(0, in.read());
            assertEquals(1, in.read());
            assertEquals(2, in.read());
            assertEquals(3, in.read());
        } finally {
            in.close();
        }
    }

    private static class DoneSignal extends AbstractFuture<Void> {
        public boolean set() {
            return super.set(null);
        }

        @Override
        public boolean setException(Throwable t) {
            return super.setException(t);
        }
    }

    public void testFromSocket() throws Throwable {
        final int PORT = 12222;
        final int DATA = 1;

        final DoneSignal done = new DoneSignal();

        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket ss;
                    ss = new ServerSocket(PORT);
                    Socket sSocket = ss.accept();
                    OutputStream out = sSocket.getOutputStream();
                    out.write(DATA);
                    Thread.sleep(DURATION);
                    out.close();
                    done.set();
                } catch (Exception e) {
                    done.setException(e);
                }
            }
        });
        t.start();

        Thread.sleep(DURATION);
        Socket socket;
        socket = new Socket(InetAddress.getLocalHost(), PORT);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);
        AutoCloseInputStream in = new AutoCloseInputStream(pfd);
        assertEquals(DATA, in.read());
        in.close();
        socket.close();
        pfd.close();

        done.get(5, TimeUnit.SECONDS);
    }

    public void testFromData() throws IOException {
        assertNull(ParcelFileDescriptor.fromData(null, null));
        byte[] data = new byte[] { 0 };
        assertFileDescriptorContent(data, ParcelFileDescriptor.fromData(data, null));
        data = new byte[] { 0, 1, 2, 3 };
        assertFileDescriptorContent(data, ParcelFileDescriptor.fromData(data, null));
        data = new byte[0];
        assertFileDescriptorContent(data, ParcelFileDescriptor.fromData(data, null));

        // Check that modifying the data does not modify the data in the FD
        data = new byte[] { 0, 1, 2, 3 };
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromData(data, null);
        data[1] = 42;
        assertFileDescriptorContent(new byte[] { 0, 1, 2, 3 }, pfd);
    }

    private static void assertFileDescriptorContent(byte[] expected, ParcelFileDescriptor fd)
        throws IOException {
        assertInputStreamContent(expected, new ParcelFileDescriptor.AutoCloseInputStream(fd));
    }

    private static void assertInputStreamContent(byte[] expected, InputStream is)
            throws IOException {
        try {
            byte[] observed = new byte[expected.length];
            int count = is.read(observed);
            assertEquals(expected.length, count);
            assertEquals(-1, is.read());
            MoreAsserts.assertEquals(expected, observed);
        } finally {
            is.close();
        }
    }

    public void testFromDataSkip() throws IOException {
        byte[] data = new byte[] { 40, 41, 42, 43, 44, 45, 46 };
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromData(data, null);
        assertNotNull(pfd);
        FileDescriptor fd = pfd.getFileDescriptor();
        assertNotNull(fd);
        assertTrue(fd.valid());
        FileInputStream is = new FileInputStream(fd);
        try {
            assertEquals(1, is.skip(1));
            assertEquals(41, is.read());
            assertEquals(42, is.read());
            assertEquals(2, is.skip(2));
            assertEquals(45, is.read());
            assertEquals(46, is.read());
            assertEquals(-1, is.read());
        } finally {
            is.close();
        }
    }

    public void testToString() {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(new Socket());
        assertNotNull(pfd.toString());
    }

    public void testWriteToParcel() throws Exception {
        ParcelFileDescriptor pf = makeParcelFileDescriptor(getContext());

        Parcel pl = Parcel.obtain();
        pf.writeToParcel(pl, ParcelFileDescriptor.PARCELABLE_WRITE_RETURN_VALUE);
        pl.setDataPosition(0);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.CREATOR.createFromParcel(pl);
        AutoCloseInputStream in = new AutoCloseInputStream(pfd);
        try {
            // read the data that was wrote previously
            assertEquals(0, in.read());
            assertEquals(1, in.read());
            assertEquals(2, in.read());
            assertEquals(3, in.read());
        } finally {
            in.close();
        }
    }

    public void testClose() throws Exception {
        ParcelFileDescriptor pf = makeParcelFileDescriptor(getContext());
        AutoCloseInputStream in1 = new AutoCloseInputStream(pf);
        try {
            assertEquals(0, in1.read());
        } finally {
            in1.close();
        }

        pf.close();

        AutoCloseInputStream in2 = new AutoCloseInputStream(pf);
        try {
            assertEquals(0, in2.read());
            fail("Failed to throw exception.");
        } catch (Exception e) {
            // expected
        } finally {
            in2.close();
        }
    }

    public void testGetStatSize() throws Exception {
        ParcelFileDescriptor pf = makeParcelFileDescriptor(getContext());
        assertTrue(pf.getStatSize() >= 0);
    }

    public void testGetFileDescriptor() {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(new Socket());
        assertNotNull(pfd.getFileDescriptor());

        ParcelFileDescriptor p = new ParcelFileDescriptor(pfd);
        assertSame(pfd.getFileDescriptor(), p.getFileDescriptor());
    }

    public void testDescribeContents() {
        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(new Socket());
        assertTrue((Parcelable.CONTENTS_FILE_DESCRIPTOR & pfd.describeContents()) != 0);
    }

    private static void assertContains(String expected, String actual) {
        if (actual.contains(expected)) return;
        throw new ComparisonFailure("", expected, actual);
    }

    private static void write(ParcelFileDescriptor pfd, int oneByte)  throws IOException{
        new FileOutputStream(pfd.getFileDescriptor()).write(oneByte);
    }

    private static int read(ParcelFileDescriptor pfd) throws IOException {
        return new FileInputStream(pfd.getFileDescriptor()).read();
    }

    public void testPipeNormal() throws Exception {
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
        final ParcelFileDescriptor red = pipe[0];
        final ParcelFileDescriptor blue = pipe[1];

        write(blue, 1);
        assertEquals(1, read(red));

        blue.close();
        assertEquals(-1, read(red));
        red.checkError();
    }

    public void testPipeError() throws Exception {
        final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
        final ParcelFileDescriptor red = pipe[0];
        final ParcelFileDescriptor blue = pipe[1];

        write(blue, 2);
        blue.closeWithError("OMG MUFFINS");

        // even though closed we should still drain pipe
        assertEquals(2, read(red));
        assertEquals(-1, read(red));
        try {
            red.checkError();
            fail("expected throw!");
        } catch (IOException e) {
            assertContains("OMG MUFFINS", e.getMessage());
        }
    }

    public void testFileNormal() throws Exception {
        final Handler handler = new Handler(Looper.getMainLooper());
        final FutureCloseListener listener = new FutureCloseListener();
        final ParcelFileDescriptor file = ParcelFileDescriptor.open(
                File.createTempFile("pfd", "bbq"), ParcelFileDescriptor.MODE_READ_WRITE, handler,
                listener);

        write(file, 7);
        file.close();

        // make sure we were notified
        assertEquals(null, listener.get());
    }

    public void testFileError() throws Exception {
        final Handler handler = new Handler(Looper.getMainLooper());
        final FutureCloseListener listener = new FutureCloseListener();
        final ParcelFileDescriptor file = ParcelFileDescriptor.open(
                File.createTempFile("pfd", "bbq"), ParcelFileDescriptor.MODE_READ_WRITE, handler,
                listener);

        write(file, 8);
        file.closeWithError("OMG BANANAS");

        // make sure error came through
        assertContains("OMG BANANAS", listener.get().getMessage());
    }

    public void testFileDetach() throws Exception {
        final Handler handler = new Handler(Looper.getMainLooper());
        final FutureCloseListener listener = new FutureCloseListener();
        final ParcelFileDescriptor file = ParcelFileDescriptor.open(
                File.createTempFile("pfd", "bbq"), ParcelFileDescriptor.MODE_READ_WRITE, handler,
                listener);

        file.detachFd();

        // make sure detach came through
        assertContains("DETACHED", listener.get().getMessage());
    }

    public void testSocketErrorAfterClose() throws Exception {
        final ParcelFileDescriptor[] pair = ParcelFileDescriptor.createReliableSocketPair();
        final ParcelFileDescriptor red = pair[0];
        final ParcelFileDescriptor blue = pair[1];

        // both sides throw their hands in the air
        blue.closeWithError("BLUE RAWR");
        red.closeWithError("RED RAWR");

        // red noticed the blue error, but after that the comm pipe was dead so
        // blue had no way of seeing the red error.
        try {
            red.checkError();
            fail("expected throw!");
        } catch (IOException e) {
            assertContains("BLUE RAWR", e.getMessage());
        }

        // expected to not throw; no error
        blue.checkError();
    }

    public void testSocketMultipleCheck() throws Exception {
        final ParcelFileDescriptor[] pair = ParcelFileDescriptor.createReliableSocketPair();
        final ParcelFileDescriptor red = pair[0];
        final ParcelFileDescriptor blue = pair[1];

        // allow checking before closed; they should all pass
        blue.checkError();
        blue.checkError();
        blue.checkError();

        // and verify we actually see it
        red.closeWithError("RAWR RED");
        try {
            blue.checkError();
            fail("expected throw!");
        } catch (IOException e) {
            assertContains("RAWR RED", e.getMessage());
        }
    }

    static ParcelFileDescriptor makeParcelFileDescriptor(Context con) throws Exception {
        final String fileName = "testParcelFileDescriptor";

        FileOutputStream fout = null;

        fout = con.openFileOutput(fileName, Context.MODE_WORLD_WRITEABLE);

        try {
            fout.write(new byte[] { 0x0, 0x1, 0x2, 0x3 });
        } finally {
            fout.close();
        }

        File dir = con.getFilesDir();
        File file = new File(dir, fileName);
        ParcelFileDescriptor pf = null;

        pf = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);

        return pf;
    }
}
