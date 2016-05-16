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

package android.drm.cts;

import junit.framework.TestCase;

public class NativeCodeTest extends TestCase {

    static {
        System.loadLibrary("ctsdrm_jni");
    }

    public void testInstallDrmEngine() throws Exception {
        assertFalse("Device is vulnerable to arbitrary code execution in drmserver process.",
                    doInstallDrmEngineTest());
    }

    /**
     * Returns true iff this device is vulnerable to arbitrary code execution in drm server
     */
    private static native boolean doInstallDrmEngineTest();
}
