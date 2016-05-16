/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.test.AndroidTestCase;

import com.google.common.util.concurrent.AbstractFuture;

import junit.framework.ComparisonFailure;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test various cross-process {@link ParcelFileDescriptor} interactions.
 */
public class ParcelFileDescriptorProcessTest extends AndroidTestCase {

    private Intent redIntent;
    private Intent blueIntent;
    private PeerConnection redConn;
    private PeerConnection blueConn;
    private IParcelFileDescriptorPeer red;
    private IParcelFileDescriptorPeer blue;

    public static class PeerConnection extends AbstractFuture<IParcelFileDescriptorPeer>
            implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            set(IParcelFileDescriptorPeer.Stub.asInterface(service));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public IParcelFileDescriptorPeer get() throws InterruptedException, ExecutionException {
            try {
                return get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void assertContains(String expected, String actual) {
        if (actual.contains(expected)) return;
        throw new ComparisonFailure("", expected, actual);
    }

    private static void crash(IParcelFileDescriptorPeer peer) {
        try {
            peer.crash();
        } catch (RemoteException e) {
        }
        SystemClock.sleep(500);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final Context context = getContext();

        // Bring up both remote processes and wire them to each other
        redIntent = new Intent();
        redIntent.setComponent(new ComponentName(
                "com.android.cts.os", "android.os.cts.ParcelFileDescriptorPeer$Red"));
        blueIntent = new Intent();
        blueIntent.setComponent(new ComponentName(
                "com.android.cts.os", "android.os.cts.ParcelFileDescriptorPeer$Blue"));
        redConn = new PeerConnection();
        blueConn = new PeerConnection();
        context.startService(redIntent);
        context.startService(blueIntent);
        getContext().bindService(redIntent, redConn, 0);
        getContext().bindService(blueIntent, blueConn, 0);
        red = redConn.get();
        blue = blueConn.get();
        red.setPeer(blue);
        blue.setPeer(red);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        final Context context = getContext();
        context.unbindService(redConn);
        context.unbindService(blueConn);
        context.stopService(redIntent);
        context.stopService(blueIntent);

        final ActivityManager am = (ActivityManager) mContext.getSystemService(
                Context.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(context.getPackageName());
    }

    public void testPullPipeNormal() throws Exception {
        // red <-- blue
        red.setupReadPipe();
        blue.doGet();

        blue.write(1);
        assertEquals(1, red.read());

        blue.close();
        assertEquals(-1, red.read());
        assertEquals(null, red.checkError());
    }

    public void testPushPipeNormal() throws Exception {
        // red --> blue
        red.setupWritePipe();
        red.doSet();

        red.write(2);
        assertEquals(2, blue.read());

        red.close();
        assertEquals(-1, blue.read());
        assertEquals(null, blue.checkError());
    }

    public void testPipeWriterError() throws Exception {
        // red --> blue
        red.setupWritePipe();
        red.doSet();

        red.write(3);
        red.closeWithError("OMG MUFFINS");

        // even though closed we should still drain pipe
        assertEquals(3, blue.read());
        assertEquals(-1, blue.read());
        assertContains("OMG MUFFINS", blue.checkError());
    }

    public void testPipeWriterCrash() throws Exception {
        // red --> blue
        red.setupWritePipe();
        blue.doGet();

        red.write(4);
        crash(red);

        // even though dead we should still drain pipe
        assertEquals(4, blue.read());
        assertEquals(-1, blue.read());
        assertContains("Remote side is dead", blue.checkError());
    }

    public void testSocketCrash() throws Exception {
        // red <--> blue
        red.setupSocket();
        blue.doGet();

        red.write(5);
        blue.write(6);

        assertEquals(5, blue.read());
        crash(blue);
        assertEquals(6, red.read());
        assertEquals(-1, red.read());
        assertContains("Remote side is dead", red.checkError());
    }

    public void testFileNormal() throws Exception {
        red.setupFile();
        blue.doGet();

        blue.write(7);
        blue.close();

        // make sure red heard us finish
        assertEquals("null", red.checkListener());
    }

    public void testFileError() throws Exception {
        red.setupFile();
        blue.doGet();

        blue.write(8);
        blue.closeWithError("OMG BANANAS");

        // make sure red heard us error
        assertContains("OMG BANANAS", red.checkListener());
    }

    public void testFileCrash() throws Exception {
        red.setupFile();
        blue.doGet();

        blue.write(9);
        crash(blue);

        // make sure red heard us die
        assertContains("Remote side is dead", red.checkListener());
    }

    public void testFileDetach() throws Exception {
        red.setupFile();
        blue.doGet();
        blue.detachFd();

        // make sure red heard us detach
        assertContains("DETACHED", red.checkListener());
    }

    public void testFileLeak() throws Exception {
        red.setupFile();
        blue.doGet();
        blue.leak();

        // make sure red heard us get leaked
        assertContains("leaked", red.checkListener());
    }

    public void testSocketErrorAfterClose() throws Exception {
        // red <--> blue
        red.setupSocket();
        blue.doGet();

        // both sides throw their hands in the air
        blue.closeWithError("BLUE RAWR");
        red.closeWithError("RED RAWR");

        // red noticed the blue error, but after that the comm pipe was dead so
        // blue had no way of seeing the red error.
        assertContains("BLUE RAWR", red.checkError());
        assertNull(blue.checkError());
    }

    public void testSocketDeathBeforeClose() throws Exception {
        // red <--> blue
        red.setupSocket();
        blue.doGet();

        crash(blue);
        assertContains("Remote side is dead", red.checkError());
        red.close();
    }

    public void testSocketDeathAfterClose() throws Exception {
        // red <--> blue
        red.setupSocket();
        blue.doGet();

        crash(blue);
        red.close();
        assertContains("Remote side is dead", red.checkError());
    }

    public void testSocketMultipleCheck() throws Exception {
        // red <--> blue
        red.setupSocket();
        blue.doGet();

        // allow checking before closed
        assertNull(blue.checkError());
        assertNull(blue.checkError());
        assertNull(blue.checkError());

        // and verify we actually see it
        red.closeWithError("RAWR RED");
        assertContains("RAWR RED", blue.checkError());
    }

    public void testSocketGiantError() throws Exception {
        // red <--> blue
        red.setupSocket();
        blue.doGet();

        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1024; i++) {
            builder.append(i).append(",");
        }
        final String msg = builder.toString();
        red.closeWithError(msg);

        // we should at least see the first 512 chars
        assertContains(msg.substring(0, 512), blue.checkError());
    }
}
