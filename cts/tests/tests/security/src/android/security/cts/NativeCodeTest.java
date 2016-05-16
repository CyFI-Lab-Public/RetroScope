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

package android.security.cts;

import junit.framework.TestCase;

public class NativeCodeTest extends TestCase {

    static {
        System.loadLibrary("ctssecurity_jni");
    }

    public void testVroot() throws Exception {
        assertTrue(doVrootTest());
    }

    public void testPerfEvent() throws Exception {
        assertFalse("Device is vulnerable to CVE-2013-2094. Please apply security patch "
                    + "at http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/"
                    + "commit/?id=8176cced706b5e5d15887584150764894e94e02f",
                    doPerfEventTest());
    }

    public void testPerfEvent2() throws Exception {
        assertTrue(doPerfEventTest2());
    }

    /**
     * Returns true iff this device is vulnerable to CVE-2013-2094.
     * A patch for CVE-2013-2094 can be found at
     * http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=8176cced706b5e5d15887584150764894e94e02f
     */
    private static native boolean doPerfEventTest();

    /**
     * CVE-2013-4254
     *
     * Verifies that
     * http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=c95eb3184ea1a3a2551df57190c81da695e2144b
     * is applied to the system. Returns true if the patch is applied,
     * and crashes the system otherwise.
     *
     * While you're at it, please also apply the following patch:
     * http://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/?id=b88a2595b6d8aedbd275c07dfa784657b4f757eb
     *
     * Credit: https://github.com/deater/perf_event_tests/blob/master/exploits/arm_perf_exploit.c
     */
    private static native boolean doPerfEventTest2();

    /**
     * ANDROID-11234878
     *
     * Returns true if the device is patched against the vroot
     * vulnerability. Returns false if there was some problem running
     * the test (for example, out of memory), or the test fails but wasn't
     * able to crash the device. Most of the time, however, the device will
     * crash if the vulnerability is present.
     *
     * The following patch addresses this bug:
     * https://git.kernel.org/cgit/linux/kernel/git/torvalds/linux.git/commit/arch/arm/include/asm/uaccess.h?id=8404663f81d212918ff85f493649a7991209fa04
     */
    private static native boolean doVrootTest();
}
