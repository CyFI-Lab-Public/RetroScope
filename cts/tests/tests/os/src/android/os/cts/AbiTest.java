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

package android.os.cts;

import android.os.cts.ReadElf;

import java.io.File;

import junit.framework.TestCase;

public class AbiTest extends TestCase {
    public void testNo64() throws Exception {
        for (String dir : new File("/").list()) {
            if (!dir.equals("data") && !dir.equals("dev") && !dir.equals("proc") && !dir.equals("sys")) {
                checkElfFilesInDirectory(new File("/" + dir));
            }
        }
    }

    private void checkElfFilesInDirectory(File dir) throws Exception {
        if (!dir.isDirectory()) {
            return;
        }

        if (isSymbolicLink(dir)) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File f : files) {
            if (f.isDirectory()) {
                checkElfFilesInDirectory(f);
            } else if (f.getName().endsWith(".so") || f.canExecute()) {
                try {
                    ReadElf.read(f);
                } catch (IllegalArgumentException ignored) {
                    // If it's not actually an ELF file, we don't care.
                }
            }
        }
    }

    private static boolean isSymbolicLink(File f) throws Exception {
        return !f.getAbsolutePath().equals(f.getCanonicalPath());
    }
}
