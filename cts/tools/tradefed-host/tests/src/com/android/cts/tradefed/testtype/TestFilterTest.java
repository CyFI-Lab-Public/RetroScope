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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class TestFilterTest extends TestCase {

    private TestFilter mFilter;
    private List<TestIdentifier> mTestList;

    private static final TestIdentifier TEST1 = new TestIdentifier("FooTest", "testFoo");
    private static final TestIdentifier TEST2 = new TestIdentifier("FooTest", "testFoo2");
    private static final TestIdentifier TEST3 = new TestIdentifier("FooTest2", "testFoo3");

    @Override
    protected void setUp() throws Exception {
        mFilter = new TestFilter();
        mTestList = new ArrayList<TestIdentifier>();
        mTestList.add(TEST1);
        mTestList.add(TEST2);
        mTestList.add(TEST3);
    }

    /**
     * Test {@link TestFilter#filter(java.util.Collection)} with no rules defined
     */
    public void testFilter_empty() {
        assertEquals(mTestList.size(), mFilter.filter(mTestList).size());
    }

    /**
     * Test {@link TestFilter#filter(java.util.Collection)} with an excluded test filter
     */
    public void testFilter_excludeTest() {
        mFilter.addExcludedTest(TEST1);
        Collection<TestIdentifier> filteredList = mFilter.filter(mTestList);
        assertEquals(2, filteredList.size());
        Iterator<TestIdentifier> iter = filteredList.iterator();
        assertEquals(TEST2, iter.next());
        assertEquals(TEST3, iter.next());
    }

    /**
     * Test {@link TestFilter#filter(java.util.Collection)} with an excluded test filter
     */
    public void testFilter_excludeClass() {
        mFilter.addExcludedClass(TEST1.getClassName());
        Collection<TestIdentifier> filteredList = mFilter.filter(mTestList);
        assertEquals(1, filteredList.size());
        assertEquals(TEST3, filteredList.iterator().next());
    }

    /**
     * Test {@link TestFilter#filter(java.util.Collection)} with a class inclusion rule
     */
    public void testFilter_includeClass() {
        mFilter.setTestInclusion(TEST1.getClassName(), null);
        Collection<TestIdentifier> filteredList = mFilter.filter(mTestList);
        assertEquals(2, filteredList.size());
        Iterator<TestIdentifier> iter = filteredList.iterator();
        assertEquals(TEST1, iter.next());
        assertEquals(TEST2, iter.next());
    }

    /**
     * Test {@link TestFilter#filter(java.util.Collection)} with at class
     */
    public void testFilter_includeTest() {
        mFilter.setTestInclusion(TEST1.getClassName(), TEST1.getTestName());
        Collection<TestIdentifier> filteredList = mFilter.filter(mTestList);
        assertEquals(1, filteredList.size());
        Iterator<TestIdentifier> iter = filteredList.iterator();
        assertEquals(TEST1, iter.next());
    }
}
