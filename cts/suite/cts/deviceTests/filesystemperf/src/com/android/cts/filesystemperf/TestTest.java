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


// code for testing, will be removed / moved before release

package com.android.cts.filesystemperf;

import android.cts.util.CtsAndroidTestCase;

/**
 * This class is for testing CTS logging. Will be disabled in release.
 *
 */
public class TestTest extends CtsAndroidTestCase {
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
/* code for testing CTS logging. Disabled.
    @TimeoutReq(minutes = 15)
    public void testPass() {
        double[] array = new double[] {1.0, 2.0, 3.0};
        getReportLog().printArray(" ", array, true);
        getReportLog().printArray(" ", array, false);
        getReportLog().printValue(" ", 1.0);
        getReportLog().printValue(" ", 2.0);
        getReportLog().printSummary("This should be shown", 0, 0);
    }

    @TimeoutReq(minutes = 10)
    public void testFail() throws Exception {
        getReportLog().printValue(" ", 1.0);
        getReportLog().printValue(" ", 2.0);
        getReportLog().printSummary("This should not be shown", 0, 0);
        throw new Exception("failed");
    }
*/
}
