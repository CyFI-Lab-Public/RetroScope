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

package com.android.cts.tradefed.testtype;

import com.android.ddmlib.testrunner.TestIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filter for {@link TestIdentifier}s.
 */
public class TestFilter {

    private final Set<String> mExcludedClasses;
    private final Set<TestIdentifier> mExcludedTests;
    private String mIncludedClass = null;
    private String mIncludedMethod = null;

    /**
     * Creates a {@link TestFilter}
     */
    public TestFilter() {
        mExcludedClasses = new HashSet<String>();
        mExcludedTests = new HashSet<TestIdentifier>();
    }

    /**
     * Adds a test class to the filter.
     * <p/>
     * All tests in this class should be filtered.
     */
    public void addExcludedClass(String className) {
        mExcludedClasses.add(className);
    }

    /**
     * Adds a test class to the filter. All tests in this class should be excluded.
     */
    public void addExcludedTest(TestIdentifier test) {
        mExcludedTests.add(test);
    }

    /**
     * Get the test classes to exclude.
     * <p/>
     * Exposed for unit testing
     */
    Set<String> getExcludedClasses() {
        return mExcludedClasses;
    }

    /**
     * Get the tests to exclude.
     * <p/>
     * Exposed for unit testing
     */
    Set<TestIdentifier> getExcludedTests() {
        return mExcludedTests;
    }

    /**
     * Sets the class name and optionally method that should pass this filter. If non-null, all
     * other tests will be excluded.
     *
     * @param className the test class name to exclusively include
     * @param method the test method name to exclusively include
     */
    public void setTestInclusion(String className, String method) {
        mIncludedClass = className;
        mIncludedMethod = method;
    }

    /**
     * Filter the list of tests based on rules in this filter
     *
     * @param tests the list of tests to filter
     * @return a new sorted list of tests that passed the filter
     */
    public Collection<TestIdentifier> filter(Collection<TestIdentifier > tests) {
        List<TestIdentifier> filteredTests = new ArrayList<TestIdentifier>(tests.size());
        for (TestIdentifier test : tests) {
            if (mIncludedClass != null && !test.getClassName().equals(mIncludedClass)) {
                // skip
                continue;
            }
            if (mIncludedMethod != null && !test.getTestName().equals(mIncludedMethod)) {
                // skip
                continue;
            }
            if (mExcludedClasses.contains(test.getClassName())) {
                // skip
                continue;
            }
            if (mExcludedTests.contains(test)) {
                // skip
                continue;
            }
            filteredTests.add(test);
        }
        Collections.sort(filteredTests, new TestIdComparator());
        return filteredTests;
    }

    /**
     * Return true if there are exclusions rules defined.
     */
    public boolean hasExclusion() {
        return !mExcludedClasses.isEmpty() || !mExcludedTests.isEmpty();
    }

    /**
     * A {@link Comparator} for {@link TestIdentifier} that compares using
     * {@link TestIdentifier#toString()}
     */
    private class TestIdComparator implements Comparator<TestIdentifier> {

        @Override
        public int compare(TestIdentifier o1, TestIdentifier o2) {
            return o1.toString().compareTo(o2.toString());
        }
    }
}
