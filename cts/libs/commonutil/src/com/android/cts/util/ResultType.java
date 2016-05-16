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
 * Enum for distinguishing performance results.
 */
public enum ResultType {
    /** lower score shows better performance */
    LOWER_BETTER,
    /** higher score shows better performance */
    HIGHER_BETTER,
    /** This value is not directly correlated with performance. */
    NEUTRAL,
    /** presence of this type requires some attention although it may not be an error. */
    WARNING;

    /**
     * Return string used in CTS XML report
     */
    public String getXmlString() {
        return name().toLowerCase();
    }
}
