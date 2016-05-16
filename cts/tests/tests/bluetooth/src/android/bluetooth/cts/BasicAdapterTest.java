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

package android.bluetooth.cts;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.content.pm.PackageManager;
import android.test.AndroidTestCase;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Very basic test, just of the static methods of {@link
 * BluetoothAdapter}.
 */
public class BasicAdapterTest extends AndroidTestCase {
    private static final int DISABLE_TIMEOUT = 8000;  // ms timeout for BT disable
    private static final int ENABLE_TIMEOUT = 10000;  // ms timeout for BT enable
    private static final int POLL_TIME = 400;         // ms to poll BT state
    private static final int CHECK_WAIT_TIME = 1000;  // ms to wait before enable/disable

    private boolean mHasBluetooth;

    public void setUp() throws Exception {
        super.setUp();

        mHasBluetooth = getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH);
    }

    public void test_getDefaultAdapter() {
        /*
         * Note: If the target doesn't support Bluetooth at all, then
         * this method should return null.
         */
        if (mHasBluetooth) {
            assertNotNull(BluetoothAdapter.getDefaultAdapter());
        } else {
            assertNull(BluetoothAdapter.getDefaultAdapter());
        }
    }

    public void test_checkBluetoothAddress() {
        // Can't be null.
        assertFalse(BluetoothAdapter.checkBluetoothAddress(null));

        // Must be 17 characters long.
        assertFalse(BluetoothAdapter.checkBluetoothAddress(""));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("0"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:0"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00:"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00:0"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00:00:"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00:00:0"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00:00:00:"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00:00:00:0"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00:00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress("00:00:00:00:00:"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "00:00:00:00:00:0"));

        // Must have colons between octets.
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "00x00:00:00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "00:00.00:00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "00:00:00-00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "00:00:00:00900:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "00:00:00:00:00?00"));

        // Hex letters must be uppercase.
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "a0:00:00:00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "0b:00:00:00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "00:c0:00:00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "00:0d:00:00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "00:00:e0:00:00:00"));
        assertFalse(BluetoothAdapter.checkBluetoothAddress(
            "00:00:0f:00:00:00"));

        assertTrue(BluetoothAdapter.checkBluetoothAddress(
            "00:00:00:00:00:00"));
        assertTrue(BluetoothAdapter.checkBluetoothAddress(
            "12:34:56:78:9A:BC"));
        assertTrue(BluetoothAdapter.checkBluetoothAddress(
            "DE:F0:FE:DC:B8:76"));
    }

    /** Checks enable(), disable(), getState(), isEnabled() */
    public void test_enableDisable() {
        if (!mHasBluetooth) {
            // Skip the test if bluetooth is not present.
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        for (int i=0; i<5; i++) {
            disable(adapter);
            enable(adapter);
        }
    }

    public void test_getAddress() {
        if (!mHasBluetooth) {
            // Skip the test if bluetooth is not present.
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        enable(adapter);

        assertTrue(BluetoothAdapter.checkBluetoothAddress(adapter.getAddress()));
    }

    public void test_getName() {
        if (!mHasBluetooth) {
            // Skip the test if bluetooth is not present.
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        enable(adapter);

        String name = adapter.getName();
        assertNotNull(name);
    }

    public void test_getBondedDevices() {
        if (!mHasBluetooth) {
            // Skip the test if bluetooth is not present.
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        enable(adapter);

        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        assertNotNull(devices);
        for (BluetoothDevice device : devices) {
            assertTrue(BluetoothAdapter.checkBluetoothAddress(device.getAddress()));
        }
    }

    public void test_getRemoteDevice() {
        if (!mHasBluetooth) {
            // Skip the test if bluetooth is not present.
            return;
        }
        // getRemoteDevice() should work even with Bluetooth disabled
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        disable(adapter);

        // test bad addresses
        try {
            adapter.getRemoteDevice((String)null);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}
        try {
            adapter.getRemoteDevice("00:00:00:00:00:00:00:00");
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}
        try {
            adapter.getRemoteDevice((byte[])null);
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}
        try {
            adapter.getRemoteDevice(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00});
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        // test success
        BluetoothDevice device = adapter.getRemoteDevice("00:11:22:AA:BB:CC");
        assertNotNull(device);
        assertEquals("00:11:22:AA:BB:CC", device.getAddress());
        device = adapter.getRemoteDevice(
                new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
        assertNotNull(device);
        assertEquals("01:02:03:04:05:06", device.getAddress());
    }

    public void test_listenUsingRfcommWithServiceRecord() throws IOException {
        if (!mHasBluetooth) {
            // Skip the test if bluetooth is not present.
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        enable(adapter);

        BluetoothServerSocket socket = adapter.listenUsingRfcommWithServiceRecord(
                "test", UUID.randomUUID());
        assertNotNull(socket);
        socket.close();
    }

    /** Helper to turn BT off.
     * This method will either fail on an assert, or return with BT turned off.
     * Behavior of getState() and isEnabled() are validated along the way.
     */
    private void disable(BluetoothAdapter adapter) {
        sleep(CHECK_WAIT_TIME);
        if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
            assertFalse(adapter.isEnabled());
            return;
        }

        assertEquals(BluetoothAdapter.STATE_ON, adapter.getState());
        assertTrue(adapter.isEnabled());
        adapter.disable();
        boolean turnOff = false;
        for (int i=0; i<DISABLE_TIMEOUT/POLL_TIME; i++) {
            sleep(POLL_TIME);
            int state = adapter.getState();
            switch (state) {
            case BluetoothAdapter.STATE_OFF:
                assertFalse(adapter.isEnabled());
                return;
            default:
                if (state != BluetoothAdapter.STATE_ON || turnOff) {
                    assertEquals(BluetoothAdapter.STATE_TURNING_OFF, state);
                    turnOff = true;
                }
                break;
            }
        }
        fail("disable() timeout");
    }

    /** Helper to turn BT on.
     * This method will either fail on an assert, or return with BT turned on.
     * Behavior of getState() and isEnabled() are validated along the way.
     */
    private void enable(BluetoothAdapter adapter) {
        sleep(CHECK_WAIT_TIME);
        if (adapter.getState() == BluetoothAdapter.STATE_ON) {
            assertTrue(adapter.isEnabled());
            return;
        }

        assertEquals(BluetoothAdapter.STATE_OFF, adapter.getState());
        assertFalse(adapter.isEnabled());
        adapter.enable();
        boolean turnOn = false;
        for (int i=0; i<ENABLE_TIMEOUT/POLL_TIME; i++) {
            sleep(POLL_TIME);
            int state = adapter.getState();
            switch (state) {
            case BluetoothAdapter.STATE_ON:
                assertTrue(adapter.isEnabled());
                return;
            default:
                if (state != BluetoothAdapter.STATE_OFF || turnOn) {
                    assertEquals(BluetoothAdapter.STATE_TURNING_ON, state);
                    turnOn = true;
                }
                break;
            }
        }
        fail("enable() timeout");
    }

    private static void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {}
    }
}
