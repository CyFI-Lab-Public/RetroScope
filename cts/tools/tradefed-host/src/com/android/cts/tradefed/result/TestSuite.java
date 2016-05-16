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
import com.android.tradefed.result.TestResult;

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data structure that represents a "TestSuite" XML element and its children.
 */
class TestSuite extends AbstractXmlPullParser {

    static final String TAG = "TestSuite";

    private String mName;

    // use linked hash map for predictable iteration order
    Map<String, TestSuite> mChildSuiteMap = new LinkedHashMap<String, TestSuite>();
    Map<String, TestCase> mChildTestCaseMap = new LinkedHashMap<String, TestCase>();

    /**
     * @param testSuite
     */
    public TestSuite(String suiteName) {
        mName = suiteName;
    }

    public TestSuite() {
    }

    /**
     * @return the name of this suite
     */
    public String getName() {
        return mName;
    }

    /**
     * Set the name of this suite
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Insert the given test result into this suite.
     *
     * @param suiteNames list of remaining suite names for this test
     * @param testClassName the test class name
     * @param testName the test method name
     * @param testResult the {@link TestResult}
     */
    public Test findTest(List<String> suiteNames, String testClassName, String testName,
            boolean insertIfMissing) {
        if (suiteNames.size() <= 0) {
            // no more package segments
            TestCase testCase = getTestCase(testClassName);
            return testCase.findTest(testName, insertIfMissing);
        } else {
            String rootName = suiteNames.remove(0);
            TestSuite suite = getTestSuite(rootName);
            return suite.findTest(suiteNames, testClassName, testName, insertIfMissing);
        }
    }

    /**
     * Gets all the child {@link TestSuite}s
     */
    public Collection<TestSuite> getTestSuites() {
        return mChildSuiteMap.values();
    }

    /**
     * Gets all the child {@link TestCase}s
     */
    public Collection<TestCase> getTestCases() {
        return mChildTestCaseMap.values();
    }

    /**
     * Get the child {@link TestSuite} with given name, creating if necessary.
     *
     * @param suiteName
     * @return the {@link TestSuite}
     */
    private TestSuite getTestSuite(String suiteName) {
        TestSuite testSuite = mChildSuiteMap.get(suiteName);
        if (testSuite == null) {
            testSuite = new TestSuite(suiteName);
            mChildSuiteMap.put(suiteName, testSuite);
        }
        return testSuite;
    }

    /**
     * Get the child {@link TestCase} with given name, creating if necessary.
     * @param testCaseName
     * @return
     */
    private TestCase getTestCase(String testCaseName) {
        TestCase testCase = mChildTestCaseMap.get(testCaseName);
        if (testCase == null) {
            testCase = new TestCase(testCaseName);
            mChildTestCaseMap.put(testCaseName, testCase);
        }
        return testCase;
    }

    /**
     * Serialize this object and all its contents to XML.
     *
     * @param serializer
     * @throws IOException
     */
    public void serialize(KXmlSerializer serializer) throws IOException {
        if (mName != null) {
            serializer.startTag(CtsXmlResultReporter.ns, TAG);
            serializer.attribute(CtsXmlResultReporter.ns, "name", mName);
        }
        for (TestSuite childSuite : mChildSuiteMap.values()) {
            childSuite.serialize(serializer);
        }
        for (TestCase childCase : mChildTestCaseMap.values()) {
            childCase.serialize(serializer);
        }
        if (mName != null) {
            serializer.endTag(CtsXmlResultReporter.ns, TAG);
        }
    }

    /**
     * Populates this class with suite result data parsed from XML.
     *
     * @param parser the {@link XmlPullParser}. Expected to be pointing at start
     *            of a TestSuite tag
     */
    @Override
    void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (!parser.getName().equals(TAG)) {
            throw new XmlPullParserException(String.format(
                    "invalid XML: Expected %s tag but received %s", TAG, parser.getName()));
        }
        setName(getAttribute(parser, "name"));
        int eventType = parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(TestSuite.TAG)) {
                TestSuite suite = new TestSuite();
                suite.parse(parser);
                insertSuite(suite);
            } else if (eventType == XmlPullParser.START_TAG && parser.getName().equals(
                    TestCase.TAG)) {
                TestCase testCase = new TestCase();
                testCase.parse(parser);
                insertTestCase(testCase);
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(TAG)) {
                return;
            }
            eventType = parser.next();
        }
    }

    /**
     * Adds a child {@link TestCase}.
     */
    public void insertTestCase(TestCase testCase) {
        mChildTestCaseMap.put(testCase.getName(), testCase);
    }

    /**
     * Adds a child {@link TestSuite}.
     */
    public void insertSuite(TestSuite suite) {
        mChildSuiteMap.put(suite.getName(), suite);
    }


    /**
     * Adds tests contained in this result that have the given <var>resultFilter</var>
     *
     * @param tests the {@link Collection} of {@link TestIdentifier}s to add to
     * @param parentSuiteNames a {@link Deque} of parent suite names. Used to construct the full
     * class name of the test
     * @param resultFilter the {@link CtsTestStatus} to filter by
     */
    void addTestsWithStatus(Collection<TestIdentifier> tests, Deque<String> parentSuiteNames,
            CtsTestStatus resultFilter) {
        if (getName() != null) {
            parentSuiteNames.addLast(getName());
        }
        for (TestSuite suite : mChildSuiteMap.values()) {
            suite.addTestsWithStatus(tests, parentSuiteNames, resultFilter);
        }
        for (TestCase testCase : mChildTestCaseMap.values()) {
            testCase.addTestsWithStatus(tests, parentSuiteNames, resultFilter);
        }
        if (getName() != null) {
            parentSuiteNames.removeLast();
        }
    }

    /**
     * Count the number of tests in this {@link TestSuite} with given status.
     *
     * @param status
     * @return the test count
     */
    public int countTests(CtsTestStatus status) {
        int total = 0;
        for (TestSuite suite : mChildSuiteMap.values()) {
            total += suite.countTests(status);
        }
        for (TestCase testCase : mChildTestCaseMap.values()) {
            total += testCase.countTests(status);
        }
        return total;
    }
}
