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

package android.jni.cts;


/**
 * Basic tests of calling the C++ functions that make up the JNI. This
 * class merely calls into native code and reports back if there was
 * a problem.
 */
public class JniCppTest extends JniTestCase {
    static {
        if (!JniTestCase.isCpuAbiNone()) {
            System.loadLibrary("jnitest");
        }
    }

    /**
     * Calls the native test, and {@code fail()}s appropriately if
     * there was a problem.
     */
    public void testEverything() {
        String msg = runAllTests();

        if (msg != null) {
            fail(msg);
        }
    }

    /**
     * The native method that does all the actual testing.
     *
     * @returns an error message or {@code null} if all went well
     */
    private static native String runAllTests();
}
