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

package com.android.cts.util;

/**
 * Enum for representing the unit of performance results.
 *
 */
public enum ResultUnit {
    /** for value with no unit */
    NONE,
    /** milli-seconds */
    MS,
    /** frames per second */
    FPS,
    /** operations per second */
    OPS,
    /** kilo-bytes-per-second, not bits-per-second */
    KBPS,
    /** mega-bytes-per-second */
    MBPS,
    /** amount of data, bytes */
    BYTE,
    /** tell how many times it did happen. */
    COUNT,
    /** unit for benchmarking with generic score. */
    SCORE;

    /**
     * Return string used in CTS XML report
     */
    public String getXmlString() {
        return name().toLowerCase();
    }
}

