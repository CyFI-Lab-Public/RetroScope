/*
 * Copyright 2012 The Android Open Source Project
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
package com.android.cts.tradefed.testtype;

import com.android.ddmlib.testrunner.ITestRunListener;

import junit.framework.TestCase;



/**
 * Unit tests for {@link WrappedGTestResultParser}.
 */
public class WrappedGTestResultParserTest extends TestCase {

    private WrappedGTestResultParser mParser;
    private final String[] INPUT1 = new String[] {
        "INSTRUMENTATION_STATUS: gtest=[==========] Running 9 tests from 2 test cases.",
        "INSTRUMENTATION_STATUS_CODE: 1",
        "INSTRUMENTATION_STATUS: gtest=[ RUN      ] GLTest.Test1",
        "INSTRUMENTATION_STATUS: gtest=[       OK ] GLTest.Test1 (10 ms)",
        "INSTRUMENTATION_STATUS: gtest=/tests/SomeTestFile.cpp:1337: Failure",
        "Value of: 1 == 0",
        "  Actual: false",
        "Expected: true",
        "INSTRUMENTATION_STATUS: gtest=[  FAILED  ] GLTest.Test2 (1016 ms)",
        "INSTRUMENTATION_STATUS: gtest=[==========] 2 tests from 1 test cases ran. (17 ms total)",
        "INSTRUMENTATION_CODE: -1"
    };

    private final String[] EXPECTED_OUTPUT1 = new String[] {
        "[==========] Running 9 tests from 2 test cases.",
        "[ RUN      ] GLTest.Test1",
        "[       OK ] GLTest.Test1 (10 ms)",
        "/tests/SomeTestFile.cpp:1337: Failure",
        "Value of: 1 == 0",
        "  Actual: false",
        "Expected: true",
        "[  FAILED  ] GLTest.Test2 (1016 ms)",
        "[==========] 2 tests from 1 test cases ran. (17 ms total)",
    };

    private final String[] INPUT2 = new String[] {
        "INSTRUMENTATION_STATUS_CODE: 1",
        "invalid text",
        "INSTRUMENTATION_STATUS: gtest=[==========] Running 9 tests from 2 test cases.",
        "INSTRUMENTATION_RESULT: some error",
        "INSTRUMENTATION_STATUS: gtest=[ RUN      ] GLTest.ExpectTestThatShouldBeSuccessful",
    };

    private final String[] EXPECTED_OUTPUT2 = new String[] {
        "[==========] Running 9 tests from 2 test cases.",
    };

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mParser = new WrappedGTestResultParser("unused", (ITestRunListener)null);
    }

    private void assertArrayEquals(String[] expected, String[] result) throws Exception {
        if (expected == null) {
            assertNull(result);
            return;
        }

        assertEquals(expected.length, result.length);

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i]);
        }
    }

    /**
     * Test normal case {@link WrappedGTestResultParser#getRawGTestOutput(java.lang.String[])}
     * with all kinds of valid input lines.
     */
    public void testGetRawGTestOutput_valid() throws Exception {
        assertArrayEquals(EXPECTED_OUTPUT1, mParser.parseInstrumentation(INPUT1));
    }

    /**
     * Test normal case {@link WrappedGTestResultParser#getRawGTestOutput(java.lang.String[])}
     * with a instrumentation error/invalid input.
     */
    public void testGetRawGTestOutput_invalid() throws Exception {
        assertArrayEquals(EXPECTED_OUTPUT2, mParser.parseInstrumentation(INPUT2));
    }
}
