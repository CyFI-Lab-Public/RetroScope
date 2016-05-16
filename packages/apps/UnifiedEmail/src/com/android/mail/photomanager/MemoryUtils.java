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

package com.android.mail.photomanager;

public class MemoryUtils {
    private MemoryUtils() {
    }

    public static final int LARGE_RAM_THRESHOLD = 640 * 1024 * 1024;
    private static long sTotalMemorySize = -1;

    public static long getTotalMemorySize() {
        if (sTotalMemorySize < 0) {
            MemInfoReader reader = new MemInfoReader();
            reader.readMemInfo();

            // getTotalSize() returns the "MemTotal" value from /proc/meminfo.
            // Because the linux kernel doesn't see all the RAM on the system
            // (e.g. GPU takes some),
            // this is usually smaller than the actual RAM size.
            sTotalMemorySize = reader.getTotalSize();
        }
        return sTotalMemorySize;
    }
}
