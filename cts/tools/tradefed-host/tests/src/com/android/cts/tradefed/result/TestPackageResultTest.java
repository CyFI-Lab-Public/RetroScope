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

import com.android.ddmlib.testrunner.TestIdentifier;

import junit.framework.TestCase;

import java.util.Collection;

/**
 * Unit tests for {@link TestPackageResult}.
 */
public class TestPackageResultTest extends TestCase {

    /**
     * Simple test for {@link TestPackageResult#getTestsWithStatus(CtsTestStatus)}.
     */
    public void testGetTestsWithStatus() {
        TestPackageResult pkgResult = new TestPackageResult();
        TestIdentifier excludedTest = new TestIdentifier("com.example.ExampleTest", "testPass");
        pkgResult.insertTest(excludedTest);
        pkgResult.reportTestEnded(excludedTest);
        TestIdentifier includedTest = new TestIdentifier("com.example.ExampleTest",
                "testNotExecuted");
        pkgResult.insertTest(includedTest);
        Collection<TestIdentifier> tests =  pkgResult.getTestsWithStatus(
                CtsTestStatus.NOT_EXECUTED);
        assertEquals(1, tests.size());
        assertEquals(includedTest, tests.iterator().next());
    }
}
