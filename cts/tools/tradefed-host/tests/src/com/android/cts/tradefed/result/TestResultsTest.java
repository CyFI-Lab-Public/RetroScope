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

import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import java.io.StringReader;

/**
 * Unit tests for {@link TestResults} parsing.
 */
public class TestResultsTest extends junit.framework.TestCase {

    private static final String RESULT_START = "<TestResult>";
    private static final String RESULT_END = "</TestResult>";
    private static final String TEST_PACKAGE_START =
        "<TestPackage name=\"pkgName\" appPackageName=\"appPkgName\" digest=\"digValue\" >";
    private static final String TEST_PACKAGE_END = "</TestPackage>";

    private static final String TEST_PACKAGE_FULL =
        RESULT_START +TEST_PACKAGE_START + TEST_PACKAGE_END + RESULT_END;

    private static final String TEST_FULL =
        RESULT_START + TEST_PACKAGE_START +
        "<TestSuite name=\"com\" >" +
            "<TestSuite name=\"example\" >" +
                "<TestCase name=\"ExampleTest\" >" +
                     "<Test name=\"testExample\"  endtime=\"et\" starttime=\"st\" result=\"fail\" >" +
                         "<FailedScene message=\"msg\" >" +
                             "<StackTrace>at ExampleTest.testExample()" +
                             "</StackTrace>" +
                         "</FailedScene>" +
                      "</Test>" +
                "</TestCase>" +
            "</TestSuite>" +
        "</TestSuite>";

    /**
     * Test parsing data with no result content
     */
    public void testParse_empty() throws Exception {
        TestResults parser = new TestResults();
        parser.parse(new StringReader("<Empty/>"));
        assertEquals(0, parser.getPackages().size());
    }

    /**
     * Test parsing data with a single test package
     */
    public void testParse_package() throws Exception {
        TestResults parser = new TestResults();
        parser.parse(new StringReader(TEST_PACKAGE_FULL));
        assertEquals(1, parser.getPackages().size());
        TestPackageResult pkg = parser.getPackages().iterator().next();
        assertEquals("pkgName", pkg.getName());
        assertEquals("appPkgName", pkg.getAppPackageName());
        assertEquals("digValue", pkg.getDigest());
    }

    /**
     * Test parsing not well formed XML data
     */
    public void testParse_corrupt() throws Exception {
        TestResults parser = new TestResults();
        // missing TEST_PACKAGE_END
        try {
            parser.parse(new StringReader(RESULT_START + TEST_PACKAGE_START + RESULT_END));
            fail("ParseException not thrown");
        } catch (ParseException e) {
            // expected
        }
    }

    /**
     * Test parsing a result with a single failed test
     */
    public void testParse_test() throws Exception {
        TestResults parser = new TestResults();
        parser.parse(new StringReader(TEST_FULL));
        assertEquals(1, parser.getPackages().size());
        TestPackageResult pkg = parser.getPackages().iterator().next();
        TestSuite comSuite = pkg.getTestSuites().iterator().next();
        assertEquals("com", comSuite.getName());
        TestSuite exampleSuite = comSuite.getTestSuites().iterator().next();
        assertEquals("example", exampleSuite.getName());
        TestCase exampleCase = exampleSuite.getTestCases().iterator().next();
        assertEquals("ExampleTest", exampleCase.getName());
        Test exampleTest = exampleCase.getTests().iterator().next();
        assertEquals("testExample", exampleTest.getName());
        assertEquals("msg", exampleTest.getMessage());
        assertEquals("at ExampleTest.testExample()", exampleTest.getStackTrace());
    }
}
