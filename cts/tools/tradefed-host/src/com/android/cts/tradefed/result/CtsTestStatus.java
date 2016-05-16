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
package com.android.cts.tradefed.result;

/**
 * An enum of possible test statuses.
 */
public enum CtsTestStatus {
    PASS("pass"),
    FAIL("fail"),
    NOT_EXECUTED("notExecuted");

    private String mValue;

    CtsTestStatus(String storedValue) {
        mValue = storedValue;
    }

    /**
     * Get the String representation of this test status that should be stored in
     * xml
     * @return
     */
    String getValue() {
       return mValue;
    }

    /**
     * Find the {@link CtsTestStatus} corresponding to given string value
     * <p/>
     * Performs a case insensitive search
     *
     * @param value
     * @return the CtsTestStatus or <code>null</code> if it could not be found
     */
    static CtsTestStatus getStatus(String  value) {
        for (CtsTestStatus status : CtsTestStatus.values()) {
            if (value.compareToIgnoreCase(status.getValue()) == 0) {
                return status;
            }
        }
        return null;
    }
}
