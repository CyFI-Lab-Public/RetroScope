/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.cts.verifier;

import android.app.Activity;
import android.content.Intent;

/**
 * Object representing the result of a test activity like whether it succeeded or failed.
 * Use {@link #setPassedResult(Activity, String, String)} or
 * {@link #setFailedResult(Activity, String, String)} from a test activity like you would
 * {@link Activity#setResult(int)} so that {@link TestListActivity}
 * will persist the test result and update its adapter and thus the list view.
 */
public class TestResult {

    public static final int TEST_RESULT_NOT_EXECUTED = 0;
    public static final int TEST_RESULT_PASSED = 1;
    public static final int TEST_RESULT_FAILED = 2;

    private static final String TEST_NAME = "name";
    private static final String TEST_RESULT = "result";
    private static final String TEST_DETAILS = "details";

    private final String mName;
    private final int mResult;
    private final String mDetails;

    /** Sets the test activity's result to pass. */
    public static void setPassedResult(Activity activity, String testId, String testDetails) {
        activity.setResult(Activity.RESULT_OK, createResult(activity, TEST_RESULT_PASSED, testId,
                testDetails));
    }

    /** Sets the test activity's result to failed. */
    public static void setFailedResult(Activity activity, String testId, String testDetails) {
        activity.setResult(Activity.RESULT_OK, createResult(activity, TEST_RESULT_FAILED, testId,
                testDetails));
    }

    private static Intent createResult(Activity activity, int testResult, String testName,
            String testDetails) {
        Intent data = new Intent(activity, activity.getClass());
        data.putExtra(TEST_NAME, testName);
        data.putExtra(TEST_RESULT, testResult);
        data.putExtra(TEST_DETAILS, testDetails);
        return data;
    }

    /**
     * Convert the test activity's result into a {@link TestResult}. Only meant to be used by
     * {@link TestListActivity}.
     */
    static TestResult fromActivityResult(int resultCode, Intent data) {
        String name = data.getStringExtra(TEST_NAME);
        int result = data.getIntExtra(TEST_RESULT, TEST_RESULT_NOT_EXECUTED);
        String details = data.getStringExtra(TEST_DETAILS);
        return new TestResult(name, result, details);
    }

    private TestResult(String name, int result, String details) {
        this.mName = name;
        this.mResult = result;
        this.mDetails = details;
    }

    /** Return the name of the test like "com.android.cts.verifier.foo.FooTest" */
    public String getName() {
        return mName;
    }

    /** Return integer test result. See test result constants. */
    public int getResult() {
        return mResult;
    }

    /** Return null or string containing test output. */
    public String getDetails() {
        return mDetails;
    }
}
