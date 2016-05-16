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

package com.android.cts.dram;

public class MemoryNative {
    static {
        System.loadLibrary("ctsdram_jni");
    }
    /**
     * run memcpy for given number of repetition from a source to a destination buffers
     * with each having the size of bufferSize.
     * @param bufferSize
     * @param repeatition
     * @return time spent in copying in ms.
     */
    public static native double runMemcpy(int bufferSize, int repetition);

    /**
     * run memset for given number of repetition from a source to a destination buffers
     * with each having the size of bufferSize.
     * @param bufferSize
     * @param repetition
     * @param c char to set. Only LSBs will be used to get char value.
     * @return time spent in memset in ms.
     */
    public static native double runMemset(int bufferSize, int repetition, int c);
}
