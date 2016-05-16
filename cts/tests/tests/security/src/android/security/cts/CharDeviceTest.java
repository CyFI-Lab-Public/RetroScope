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

package android.security.cts;

import junit.framework.TestCase;

public class CharDeviceTest extends TestCase {

    static {
        System.loadLibrary("ctssecurity_jni");
    }

    /**
     * Detect Exynos 4xxx rooting vuln.  CVE-2012-6422.
     *
     * Reference: http://forum.xda-developers.com/showthread.php?t=2048511
     */
    public void testExynosRootingVuln() throws Exception {
        assertTrue(doExynosWriteTest());
    }

    /**
     * Detect Exynos 4xxx kernel memory leak to userspace. CVE-2012-6422.
     *
     * Reference: http://forum.xda-developers.com/showthread.php?t=2048511
     */
    public void testExynosKernelMemoryRead() throws Exception {
        assertTrue(doExynosReadTest());
    }

    /**
     * @return true iff the device does not have the exynos rooting
     *     vulnerability.  If the device has the rooting vulnerability,
     *     this method will cause the device to crash and this function
     *     will never return.
     */
    private static native boolean doExynosWriteTest();

    /**
     * @return true iff the device does not provide read-access to kernel memory
     */
    private static native boolean doExynosReadTest();
}
