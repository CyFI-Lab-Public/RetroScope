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

package com.android.providers.contacts.util;

import com.android.internal.util.MemInfoReader;

public final class MemoryUtils {
    private MemoryUtils() {
    }

    private static long sTotalMemorySize = -1;

    /**
     * Returns the amount of RAM available to the Linux kernel, i.e. whatever is left after all the
     * other chips stake their claim, including GPUs, DSPs, cell radios, and any other greedy chips
     * (in other words: this is probably less than the "RAM: 1 GB" that was printed on the
     * box in far too big letters)
     */
    public static long getTotalMemorySize() {
        if (sTotalMemorySize < 0) {
            MemInfoReader reader = new MemInfoReader();
            reader.readMemInfo();

            // getTotalSize() returns the "MemTotal" value from /proc/meminfo.
            sTotalMemorySize = reader.getTotalSize();
        }
        return sTotalMemorySize;
    }
}
