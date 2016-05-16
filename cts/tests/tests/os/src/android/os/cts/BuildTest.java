/*
 * Copyright (C) 2010 The Android Open Source Project
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


import android.os.Build;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class BuildTest extends TestCase {

    private static final String RO_PRODUCT_CPU_ABI = "ro.product.cpu.abi";

    private static final String RO_PRODUCT_CPU_ABI2 = "ro.product.cpu.abi2";

    /** Tests that check the values of {@link Build#CPU_ABI} and {@link Build#CPU_ABI2}. */
    public void testCpuAbi() throws IOException {
        if (CpuFeatures.isArmCpu()) {
            assertArmCpuAbiConstants();
        }
    }

    private void assertArmCpuAbiConstants() throws IOException {
        if (CpuFeatures.isArm7Compatible()) {
            String cpuAbi = getProperty(RO_PRODUCT_CPU_ABI);
            String cpuAbi2 = getProperty(RO_PRODUCT_CPU_ABI2);
            //if CPU_ABI is armv7, CPU_ABI2 is either of {armeabi, NULL}
            if (cpuAbi.equals(CpuFeatures.ARMEABI_V7)) {
                String message = "CPU is ARM v7 compatible, so "
                    + RO_PRODUCT_CPU_ABI  + " must be set to " + CpuFeatures.ARMEABI_V7 + " and "
                    + RO_PRODUCT_CPU_ABI2 + " must be set to " + CpuFeatures.ARMEABI + " or NULL";
                assertEquals(message, CpuFeatures.ARMEABI_V7, Build.CPU_ABI);
                if (cpuAbi2.equals(CpuFeatures.ARMEABI)){
                    assertEquals(message, cpuAbi2, Build.CPU_ABI2);
                } else {
                    assertNoPropertySet(message, RO_PRODUCT_CPU_ABI2);
                    assertEquals(message, Build.UNKNOWN, Build.CPU_ABI2);
                }
            }
            //if CPU_ABI is x86, then CPU_ABI2 is either of {armeabi, armv7, NULL}
            else if (cpuAbi.equals(CpuFeatures.X86ABI)) {
                String message = "CPU is x86 but ARM v7 compatible, so "
                    + RO_PRODUCT_CPU_ABI  + " must be set to " + CpuFeatures.X86ABI + " and "
                    + RO_PRODUCT_CPU_ABI2 + " must be set to " + CpuFeatures.ARMEABI + " or "
                    + CpuFeatures.ARMEABI_V7 + " or NULL";
                assertEquals(message, CpuFeatures.X86ABI, Build.CPU_ABI);
                if (cpuAbi2.equals(CpuFeatures.ARMEABI_V7) || cpuAbi2.equals(CpuFeatures.ARMEABI))
                    assertEquals(message, cpuAbi2, Build.CPU_ABI2);
                else {
                    assertNoPropertySet(message, RO_PRODUCT_CPU_ABI2);
                    assertEquals(message, Build.UNKNOWN, Build.CPU_ABI2);
                }
            }
        }
        else {
            String message = "CPU is not ARM v7 compatible. "
                    + RO_PRODUCT_CPU_ABI  + " must be set to " + CpuFeatures.ARMEABI + " and "
                    + RO_PRODUCT_CPU_ABI2 + " must not be set.";
            assertProperty(message, RO_PRODUCT_CPU_ABI, CpuFeatures.ARMEABI);
            assertNoPropertySet(message, RO_PRODUCT_CPU_ABI2);
            assertEquals(message, CpuFeatures.ARMEABI, Build.CPU_ABI);
            assertEquals(message, Build.UNKNOWN, Build.CPU_ABI2);
        }
    }
    /**
     * @param property name passed to getprop
     */
    private String getProperty(String property)
            throws IOException {
        Process process = new ProcessBuilder("getprop", property).start();
        Scanner scanner = null;
        String line = "";
        try {
            scanner = new Scanner(process.getInputStream());
            line = scanner.nextLine();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return line;
    }
    /**
     * @param message shown when the test fails
     * @param property name passed to getprop
     * @param expected value of the property
     */
    private void assertProperty(String message, String property, String expected)
            throws IOException {
        Process process = new ProcessBuilder("getprop", property).start();
        Scanner scanner = null;
        try {
            scanner = new Scanner(process.getInputStream());
            String line = scanner.nextLine();
            assertEquals(message + " Value found: " + line , expected, line);
            assertFalse(scanner.hasNext());
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * Check that a property is not set by scanning through the list of properties returned by
     * getprop, since calling getprop on an property set to "" and on a non-existent property
     * yields the same output.
     *
     * @param message shown when the test fails
     * @param property name passed to getprop
     */
    private void assertNoPropertySet(String message, String property) throws IOException {
        Process process = new ProcessBuilder("getprop").start();
        Scanner scanner = null;
        try {
            scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                assertFalse(message + "Property found: " + line,
                        line.startsWith("[" + property + "]"));
            }
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private static final Pattern BOARD_PATTERN =
        Pattern.compile("^([0-9A-Za-z._-]+)$");
    private static final Pattern BRAND_PATTERN =
        Pattern.compile("^([0-9A-Za-z._-]+)$");
    private static final Pattern DEVICE_PATTERN =
        Pattern.compile("^([0-9A-Za-z._-]+)$");
    private static final Pattern ID_PATTERN =
        Pattern.compile("^([0-9A-Za-z._-]+)$");
    private static final Pattern HARDWARE_PATTERN =
        Pattern.compile("^([0-9A-Za-z.,_-]+)$");
    private static final Pattern PRODUCT_PATTERN =
        Pattern.compile("^([0-9A-Za-z._-]+)$");
    private static final Pattern SERIAL_NUMBER_PATTERN =
        Pattern.compile("^([0-9A-Za-z]{6,20})$");
    private static final Pattern TAGS_PATTERN =
        Pattern.compile("^([0-9A-Za-z.,_-]+)$");
    private static final Pattern TYPE_PATTERN =
        Pattern.compile("^([0-9A-Za-z._-]+)$");

    /** Tests that check for valid values of constants in Build. */
    public void testBuildConstants() {
        // Build.VERSION.* constants tested by BuildVersionTest

        assertTrue(BOARD_PATTERN.matcher(Build.BOARD).matches());

        assertTrue(BRAND_PATTERN.matcher(Build.BRAND).matches());

        assertTrue(DEVICE_PATTERN.matcher(Build.DEVICE).matches());

        // Build.FINGERPRINT tested by BuildVersionTest

        assertTrue(HARDWARE_PATTERN.matcher(Build.HARDWARE).matches());

        assertNotEmpty(Build.HOST);

        assertTrue(ID_PATTERN.matcher(Build.ID).matches());

        assertNotEmpty(Build.MANUFACTURER);

        assertNotEmpty(Build.MODEL);

        assertTrue(PRODUCT_PATTERN.matcher(Build.PRODUCT).matches());

        assertTrue(SERIAL_NUMBER_PATTERN.matcher(Build.SERIAL).matches());

        assertTrue(TAGS_PATTERN.matcher(Build.TAGS).matches());

        // No format requirements stated in CDD for Build.TIME

        assertTrue(TYPE_PATTERN.matcher(Build.TYPE).matches());

        assertNotEmpty(Build.USER);
    }

    private void assertNotEmpty(String value) {
        assertNotNull(value);
        assertFalse(value.isEmpty());
    }
}
