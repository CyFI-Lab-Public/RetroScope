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
 * limitations under the License
 */

package android.hardware.cts;

import android.app.ActivityManager;
import android.content.Context;
import android.test.AndroidTestCase;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Tests that devices with low RAM specify themselves as Low RAM devices
 */
public class LowRamDeviceTest extends AndroidTestCase {

    private static final int LOW_RAM_DEVICE_MEMORY_THRESHOLD_KB = 512 * 1024;

    public void testLowRamProductProperty() throws Exception {
        ActivityManager am =
                (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);

        if (totalAvailableSystemMemory() <= LOW_RAM_DEVICE_MEMORY_THRESHOLD_KB) {
            assertTrue("Device must specify low RAM property: ro.config.low_ram=true",
                    am.isLowRamDevice());
        }
    }

    /**
     * Returns the total amount of memory in kilobytes available to the system.
     */
    private int totalAvailableSystemMemory() throws IOException {
        final String property = "MemTotal";
        InputStream is = new FileInputStream("/proc/meminfo");
        try {
            Scanner scanner = new Scanner(is);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(property)) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    if (tokenizer.countTokens() != 3) {
                        throw new IOException("Malformed " + property + " line");
                    }

                    // Skips over "MemTotal:"
                    tokenizer.nextToken();

                    return Integer.parseInt(tokenizer.nextToken());
                }
            }
            throw new IOException(property + " could not be found");

        } finally {
            is.close();
        }
    }

}
