/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.os.cts.ReadElf;

/**
 * Verify that ASLR is properly enabled on Android Compatible devices.
 */
public class AslrTest extends TestCase {

    public void testOneExecutableIsPie() throws IOException {
        assertTrue(ReadElf.read(new File("/system/bin/cat")).isPIE());
    }

    public void testVaRandomize() throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader("/proc/sys/kernel/randomize_va_space"));
            int level = Integer.parseInt(in.readLine().trim());
            assertTrue("Expected /proc/sys/kernel/randomize_va_space to be "
                    + "greater than or equal to 2, got " + level,
                    level >= 2);
        } catch (FileNotFoundException e) {
            // Odd. The file doesn't exist... Assume ASLR is enabled.
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

}
