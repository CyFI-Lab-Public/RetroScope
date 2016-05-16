/*
 * Copyright 2013 The Android Open Source Project
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

package android.security.cts;

import junit.framework.TestCase;

import java.io.IOException;

public class LinuxRngTest extends TestCase {
    static {
        System.loadLibrary("ctssecurity_jni");
    }

    public void testDevRandomMajorMinor() throws Exception {
        // Based on Linux kernel's drivers/char/random.c
        assertEquals("/dev/random major", 1, getCharDeviceMajor("/dev/random"));
        assertEquals("/dev/random minor", 8, getCharDeviceMinor("/dev/random"));
    }

    public void testDevUrandomMajorMinor() throws Exception {
        // Based on Linux kernel's drivers/char/random.c
        assertEquals("/dev/urandom major", 1, getCharDeviceMajor("/dev/urandom"));
        assertEquals("/dev/urandom minor", 9, getCharDeviceMinor("/dev/urandom"));
    }

    public static native int getCharDeviceMajor(String file) throws IOException;
    public static native int getCharDeviceMinor(String file) throws IOException;
}
