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
package android.net.cts;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.test.AndroidTestCase;

import java.io.File;
import java.net.DatagramSocket;
import java.net.Socket;

/**
 * VpnService API is built with security in mind. However, its security also
 * blocks us from writing tests for positive cases. For now we only test for
 * negative cases, and we will try to cover the rest in the future.
 */
public class VpnServiceTest extends AndroidTestCase {

    private static final String TAG = VpnServiceTest.class.getSimpleName();

    private VpnService mVpnService = new VpnService();

    public void testPrepare() throws Exception {
        // Should never return null since we are not prepared.
        Intent intent = VpnService.prepare(mContext);
        assertNotNull(intent);

        // Should be always resolved by only one activity.
        int count = mContext.getPackageManager().queryIntentActivities(intent, 0).size();
        assertEquals(count, 1);
    }

    public void testEstablish() throws Exception {
        ParcelFileDescriptor descriptor = null;
        try {
            // Should always return null since we are not prepared.
            descriptor = mVpnService.new Builder().addAddress("8.8.8.8", 30).establish();
            assertNull(descriptor);
        } finally {
            try {
                descriptor.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public void testProtect_DatagramSocket() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        try {
            // Should always return false since we are not prepared.
            assertFalse(mVpnService.protect(socket));
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public void testProtect_Socket() throws Exception {
        Socket socket = new Socket();
        try {
            // Should always return false since we are not prepared.
            assertFalse(mVpnService.protect(socket));
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public void testProtect_int() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        ParcelFileDescriptor descriptor = ParcelFileDescriptor.fromDatagramSocket(socket);
        try {
            // Should always return false since we are not prepared.
            assertFalse(mVpnService.protect(descriptor.getFd()));
        } finally {
            try {
                descriptor.close();
            } catch (Exception e) {
                // ignore
            }
            try {
                socket.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public void testTunDevice() throws Exception {
        File file = new File("/dev/tun");
        assertTrue(file.exists());
        assertFalse(file.isFile());
        assertFalse(file.isDirectory());
        assertFalse(file.canExecute());
        assertFalse(file.canRead());
        assertFalse(file.canWrite());
    }
}
