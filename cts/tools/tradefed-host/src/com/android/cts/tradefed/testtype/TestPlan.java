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
import com.android.tradefed.util.ArrayUtil;
import com.android.tradefed.util.xml.AbstractXmlParser;

import org.kxml2.io.KXmlSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link TestPlan}.
 */
public class TestPlan extends AbstractXmlParser implements ITestPlan {

    /**
     * Map of uri names found in plan, and their excluded tests
     */
    private Map<String, TestFilter> mUriExcludedTestsMap;

    private static final String ENTRY_TAG = "Entry";
    private static final String TEST_DELIM = ";";
    private static final String METHOD_DELIM = "#";
    private static final String EXCLUDE_ATTR = "exclude";
    private static final String URI_ATTR = "uri";

    private final String mName;

    /**
     * SAX callback object. Handles parsing data from the xml tags.
     */
    private class EntryHandler extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes)
                throws SAXException {
            if (ENTRY_TAG.equals(localName)) {
                final String entryUriValue = attributes.getValue(URI_ATTR);
                TestFilter filter = parseExcludedTests(attributes.getValue(EXCLUDE_ATTR));
                mUriExcludedTestsMap.put(entryUriValue, filter);
            }
        }

        /**
         * Parse the semi colon separated list of tests to exclude.
         * <p/>
         * Expected format:
         * testClassName[#testMethodName][;testClassName2...]
         *
         * @param excludedString the excluded string list
         * @return
         */
        private TestFilter parseExcludedTests(String excludedString) {
            TestFilter filter = new TestFilter();
            if (excludedString != null) {
                String[] testStrings = excludedString.split(TEST_DELIM);
                for (String testString : testStrings) {
                    String[] classMethodPair = testString.split(METHOD_DELIM);
                    if (classMethodPair.length == 2) {
                        filter.addExcludedTest(new TestIdentifier(classMethodPair[0],
                                classMethodPair[1]));
                    } else {
                        filter.addExcludedClass(testString);
                    }
                }
            }
            return filter;
        }
    }

    public TestPlan(String name) {
        mName = name;
        // Uses a LinkedHashMap to have predictable iteration order
        mUriExcludedTestsMap = new LinkedHashMap<String, TestFilter>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return mName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getTestUris() {
        return mUriExcludedTestsMap.keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestFilter getExcludedTestFilter(String uri) {
        return mUriExcludedTestsMap.get(uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPackage(String uri) {
        mUriExcludedTestsMap.put(uri, new TestFilter());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DefaultHandler createXmlHandler() {
        return new EntryHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addExcludedTest(String uri, TestIdentifier testToExclude) {
        TestFilter filter = mUriExcludedTestsMap.get(uri);
        if (filter != null) {
            filter.addExcludedTest(testToExclude);
        } else {
            throw new IllegalArgumentException(String.format("Could not find package %s", uri));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addExcludedTests(String uri, Collection<TestIdentifier> excludedTests) {
        TestFilter filter = mUriExcludedTestsMap.get(uri);
        if (filter != null) {
            filter.getExcludedTests().addAll(excludedTests);
        } else {
            throw new IllegalArgumentException(String.format("Could not find package %s", uri));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(OutputStream stream) throws IOException {
        KXmlSerializer serializer = new KXmlSerializer();
        serializer.setOutput(stream, "UTF-8");
        serializer.startDocument("UTF-8", false);
        serializer.setFeature(
                "http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag(null, "TestPlan");
        serializer.attribute(null, "version", "1.0");
        for (Map.Entry<String, TestFilter> packageEntry : mUriExcludedTestsMap.entrySet()) {
            serializer.startTag(null, ENTRY_TAG);
            serializer.attribute(null, "uri", packageEntry.getKey());
            serializeFilter(serializer, packageEntry.getValue());
            serializer.endTag(null, ENTRY_TAG);
        }
        serializer.endTag(null, "TestPlan");
        serializer.endDocument();
    }

    /**
     * Adds an xml attribute containing {@link TestFilter} contents.
     * <p/>
     * If {@link TestFilter} is empty, no data will be outputted.
     *
     * @param serializer
     * @param value
     * @throws IOException
     */
    private void serializeFilter(KXmlSerializer serializer, TestFilter testFilter)
            throws IOException {
        if (!testFilter.hasExclusion()) {
            return;
        }
        List<String> exclusionStrings = new ArrayList<String>();
        exclusionStrings.addAll(testFilter.getExcludedClasses());
        for (TestIdentifier test : testFilter.getExcludedTests()) {
            // TODO: this relies on TestIdentifier.toString() using METHOD_DELIM.
            exclusionStrings.add(test.toString());
        }
        String exclusionAttrValue = ArrayUtil.join(TEST_DELIM, exclusionStrings);
        serializer.attribute(null, EXCLUDE_ATTR, exclusionAttrValue);
    }
}
