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
import com.android.tradefed.util.ArrayUtil;

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data structure that represents a "TestCase" XML element and its children.
 */
class TestCase extends AbstractXmlPullParser {

    static final String TAG = "TestCase";

    private String mName;

    Map<String, Test> mChildTestMap = new LinkedHashMap<String, Test>();

    /**
     * Create a {@link TestCase}
     * @param testCaseName
     */
    public TestCase(String testCaseName) {
        setName(testCaseName);
    }

    public TestCase() {
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    /**
     * Gets the child tests
     */
    public Collection<Test> getTests() {
        return mChildTestMap.values();
    }

    /**
     * @param testName
     * @param insertIfMissing
     * @return
     */
    public Test findTest(String testName, boolean insertIfMissing) {
        Test t = mChildTestMap.get(testName);
        if (t == null && insertIfMissing) {
            t = new Test(testName);
            mChildTestMap.put(t.getName(), t);
        }
        return t;
    }

    /**
     * Serialize this object and all its contents to XML.
     *
     * @param serializer
     * @throws IOException
     */
    public void serialize(KXmlSerializer serializer) throws IOException {
        serializer.startTag(CtsXmlResultReporter.ns, TAG);
        serializer.attribute(CtsXmlResultReporter.ns, "name", getName());
        // unused
        serializer.attribute(CtsXmlResultReporter.ns, "priority", "");
        for (Test t : mChildTestMap.values()) {
            t.serialize(serializer);
        }
       serializer.endTag(CtsXmlResultReporter.ns, TAG);
    }

    /**
     * Populates this class with test case result data parsed from XML.
     *
     * @param parser the {@link XmlPullParser}. Expected to be pointing at start
     *            of a TestCase tag
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
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals(Test.TAG)) {
                Test test = new Test();
                test.parse(parser);
                mChildTestMap.put(test.getName(), test);
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(TAG)) {
                return;
            }
            eventType = parser.next();
        }
    }

    /**
     * Adds tests contained in this result that have the given <var>resultFilter</var>.
     *
     * @param tests the {@link Collection} of {@link TestIdentifier}s to add to
     * @param parentSuiteNames a {@link Deque} of parent suite names. Used to
     *            construct the full class name of the test
     * @param resultFilter the {@link CtsTestStatus} to filter by
     */
    void addTestsWithStatus(Collection<TestIdentifier> tests, Deque<String> parentSuiteNames,
            CtsTestStatus resultFilter) {
        if (getName() != null) {
            parentSuiteNames.addLast(getName());
        }
        String fullClassName = ArrayUtil.join(".", parentSuiteNames);
        for (Test test : mChildTestMap.values()) {
            if (resultFilter.equals(test.getResult())) {
                tests.add(new TestIdentifier(fullClassName, test.getName()));
            }
        }
        if (getName() != null) {
            parentSuiteNames.removeLast();
        }
    }

    /**
     * Count the number of tests in this {@link TestCase} with given status.
     *
     * @param status
     * @return the test count
     */
    public int countTests(CtsTestStatus status) {
        int total = 0;
        for (Test test : mChildTestMap.values()) {
            if (test.getResult().equals(status)) {
                total++;
            }
        }
        return total;
    }
}
