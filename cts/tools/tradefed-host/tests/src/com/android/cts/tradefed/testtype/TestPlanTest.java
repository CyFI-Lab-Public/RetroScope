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

package com.android.cts.tradefed.testtype;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Unit tests for {@link TestPlan}.
 */
public class TestPlanTest extends TestCase {

    private static final String TEST_URI1 = "foo";
    private static final String TEST_URI2 = "foo2";
    private static final String EXCLUDE_TEST_CLASS = "com.example.FooTest";
    private static final String EXCLUDE_TEST_METHOD = "testFoo";
    private static final String EXCLUDE_TEST_METHOD2 = "testFoo2";

    static final String EMPTY_DATA = "<TestPlan version=\"1.0\" />";

    static final String TEST_DATA =
        "<TestPlan version=\"1.0\">" +
            String.format("<Entry uri=\"%s\" />", TEST_URI1) +
            String.format("<Entry uri=\"%s\" />", TEST_URI2) +
        "</TestPlan>";

    static final String TEST_EXCLUDED_DATA =
        "<TestPlan version=\"1.0\">" +
            String.format("<Entry uri=\"%s\" exclude=\"%s#%s\" />", TEST_URI1, EXCLUDE_TEST_CLASS,
                    EXCLUDE_TEST_METHOD) +
        "</TestPlan>";

    static final String TEST_MULTI_EXCLUDED_DATA =
        "<TestPlan version=\"1.0\">" +
            String.format("<Entry uri=\"%s\" exclude=\"%s#%s;%s#%s\" />", TEST_URI1,
                    EXCLUDE_TEST_CLASS, EXCLUDE_TEST_METHOD, EXCLUDE_TEST_CLASS,
                    EXCLUDE_TEST_METHOD2) +
        "</TestPlan>";

    static final String TEST_CLASS_EXCLUDED_DATA =
        "<TestPlan version=\"1.0\">" +
            String.format("<Entry uri=\"%s\" exclude=\"%s\" />", TEST_URI1,
                    EXCLUDE_TEST_CLASS) +
        "</TestPlan>";

    private TestPlan mPlan;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPlan = new TestPlan("plan");
    }

    /**
     * Simple test for parsing a plan containing two uris
     */
    public void testParse() throws ParseException  {
        mPlan.parse(getStringAsStream(TEST_DATA));
        assertTestData(mPlan);
    }

    /**
     * Perform checks to ensure TEST_DATA was parsed correctly
     * @param plan
     */
    private void assertTestData(TestPlan plan) {
        assertEquals(2, plan.getTestUris().size());
        Iterator<String> iter = plan.getTestUris().iterator();
        // assert uris in order
        assertEquals(TEST_URI1, iter.next());
        assertEquals(TEST_URI2, iter.next());
        assertFalse(plan.getExcludedTestFilter(TEST_URI1).hasExclusion());
        assertFalse(plan.getExcludedTestFilter(TEST_URI2).hasExclusion());
    }

    /**
     * Test parsing a plan containing a single excluded test
     */
    public void testParse_exclude() throws ParseException  {
        mPlan.parse(getStringAsStream(TEST_EXCLUDED_DATA));
        assertEquals(1, mPlan.getTestUris().size());
        TestFilter filter = mPlan.getExcludedTestFilter(TEST_URI1);
        assertTrue(filter.getExcludedTests().contains(new TestIdentifier(EXCLUDE_TEST_CLASS,
                EXCLUDE_TEST_METHOD)));
    }

    /**
     * Test parsing a plan containing multiple excluded tests
     */
    public void testParse_multiExclude() throws ParseException  {
        mPlan.parse(getStringAsStream(TEST_MULTI_EXCLUDED_DATA));
        assertMultiExcluded(mPlan);
    }

    /**
     * Perform checks to ensure TEST_MULTI_EXCLUDED_DATA was parsed correctly
     * @param plan
     */
    private void assertMultiExcluded(TestPlan plan) {
        assertEquals(1, plan.getTestUris().size());
        TestFilter filter = plan.getExcludedTestFilter(TEST_URI1);
        assertTrue(filter.getExcludedTests().contains(new TestIdentifier(EXCLUDE_TEST_CLASS,
                EXCLUDE_TEST_METHOD)));
        assertTrue(filter.getExcludedTests().contains(new TestIdentifier(EXCLUDE_TEST_CLASS,
                EXCLUDE_TEST_METHOD2)));
    }

    /**
     * Test parsing a plan containing an excluded class
     */
    public void testParse_classExclude() throws ParseException  {
        mPlan.parse(getStringAsStream(TEST_CLASS_EXCLUDED_DATA));
        assertEquals(1, mPlan.getTestUris().size());
        TestFilter filter = mPlan.getExcludedTestFilter(TEST_URI1);
        assertTrue(filter.getExcludedClasses().contains(EXCLUDE_TEST_CLASS));
    }

    /**
     * Test serializing an empty plan
     * @throws IOException
     */
    public void testSerialize_empty() throws ParseException, IOException  {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        mPlan.serialize(outStream);
        assertTrue(outStream.toString().contains(EMPTY_DATA));
    }

    /**
     * Test serializing and deserializing plan with two packages
     * @throws IOException
     */
    public void testSerialize_packages() throws ParseException, IOException  {
        mPlan.addPackage(TEST_URI1);
        mPlan.addPackage(TEST_URI2);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        mPlan.serialize(outStream);
        TestPlan parsedPlan = new TestPlan("parsed");
        parsedPlan.parse(getStringAsStream(outStream.toString()));
        // parsedPlan should contain same contents as TEST_DATA
        assertTestData(parsedPlan);
    }

    /**
     * Test serializing and deserializing plan with multiple excluded tests
     */
    public void testSerialize_multiExclude() throws ParseException, IOException  {
        mPlan.addPackage(TEST_URI1);
        mPlan.addExcludedTest(TEST_URI1, new TestIdentifier(EXCLUDE_TEST_CLASS,
                EXCLUDE_TEST_METHOD));
        mPlan.addExcludedTest(TEST_URI1, new TestIdentifier(EXCLUDE_TEST_CLASS,
                EXCLUDE_TEST_METHOD2));
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        mPlan.serialize(outStream);
        TestPlan parsedPlan = new TestPlan("parsed");
        parsedPlan.parse(getStringAsStream(outStream.toString()));
        // parsedPlan should contain same contents as TEST_DATA
        assertMultiExcluded(parsedPlan);
    }

    private InputStream getStringAsStream(String input) {
        return new ByteArrayInputStream(input.getBytes());
    }
}
