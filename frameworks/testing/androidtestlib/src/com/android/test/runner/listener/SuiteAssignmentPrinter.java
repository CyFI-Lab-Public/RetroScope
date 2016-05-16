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

package com.android.test.runner.listener;

import com.android.test.runner.TestRequestBuilder;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.PrintStream;

/**
 * This class measures the elapsed run time of each test, and used it to report back to the user
 * which suite ({@link SmallSuite}, {@link MediumSuite}, {@link LargeSuite}) the test should belong
 * to.
 */
public class SuiteAssignmentPrinter extends RunListener {
    /**
     * This constant defines the maximum allowed runtime (in ms) for a test included in the "small"
     * suite. It is used to make an educated guess at what suite an unlabeled test belongs to.
     */
    private static final float SMALL_SUITE_MAX_RUNTIME = 100;

    /**
     * This constant defines the maximum allowed runtime (in ms) for a test included in the "medium"
     * suite. It is used to make an educated guess at what suite an unlabeled test belongs to.
     */
    private static final float MEDIUM_SUITE_MAX_RUNTIME = 1000;

    private final PrintStream mWriter;

    public SuiteAssignmentPrinter(PrintStream writer) {
        mWriter = writer;
    }

    private long mStartTime;
    private boolean mTimingValid;

    @Override
    public void testStarted(Description description) throws Exception {
        mTimingValid = true;
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public void testFinished(Description description) throws Exception {
        long runTime;
        String assignmentSuite;
        long endTime = System.currentTimeMillis();

        if (!mTimingValid || mStartTime < 0) {
            assignmentSuite = "NA";
            runTime = -1;
        } else {
            runTime = endTime - mStartTime;
            if (runTime < SMALL_SUITE_MAX_RUNTIME) {
                assignmentSuite = TestRequestBuilder.SMALL_SIZE;
            } else if (runTime < MEDIUM_SUITE_MAX_RUNTIME) {
                assignmentSuite = TestRequestBuilder.MEDIUM_SIZE;
            } else {
                assignmentSuite = TestRequestBuilder.LARGE_SIZE;
            }
        }
        // Clear mStartTime so that we can verify that it gets set next time.
        mStartTime = -1;

        mWriter.printf("%s#%s\n" + "in %s suite\n" + "runTime: %d ms\n",
                        description.getClassName(), description.getMethodName(), assignmentSuite,
                        runTime);
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        mTimingValid = false;
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        mTimingValid = false;
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        mTimingValid = false;
    }
}
