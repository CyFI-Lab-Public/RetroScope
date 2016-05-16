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

package com.android.cts.monkey;

public class VerbosityTest extends AbstractMonkeyTest {

    public void testVerbosity() throws Exception {
        String v0 = mDevice.executeShellCommand(MONKEY_CMD + " -s 1337 -p " + PKGS[0] + " 500");
        assertTrue(v0.contains("Events injected"));
        assertFalse(v0.contains("Sending Touch"));
        assertFalse(v0.contains("Sending Trackball"));
        assertFalse(v0.contains("Switch"));
        assertFalse(v0.contains("Sleeping"));

        String v1 = mDevice.executeShellCommand(MONKEY_CMD + " -v -p " + PKGS[0] + " 500");
        assertTrue(v1.contains("Events injected"));
        assertTrue(v1.contains("Sending Touch"));
        assertTrue(v1.contains("Sending Trackball"));
        assertTrue(v1.contains("Switch"));
        assertFalse(v1.contains("Sleeping"));

        String v2 = mDevice.executeShellCommand(MONKEY_CMD + " -v -v -p " + PKGS[0] + " 500");
        assertTrue(v2.contains("Events injected"));
        assertTrue(v2.contains("Sending Touch"));
        assertTrue(v2.contains("Sending Trackball"));
        assertTrue(v2.contains("Switch"));
        assertTrue(v2.contains("Sleeping"));

        assertTrue(v0.length() < v1.length());
        assertTrue(v1.length() < v2.length());
    }
}
