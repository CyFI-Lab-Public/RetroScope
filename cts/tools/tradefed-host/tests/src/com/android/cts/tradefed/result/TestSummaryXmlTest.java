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

package com.android.cts.tradefed.result;

import com.android.tradefed.util.xml.AbstractXmlParser.ParseException;

import java.io.Reader;
import java.io.StringReader;

import junit.framework.TestCase;

/**
 * Unit tests for {@link TestSummaryXml}.
 */
public class TestSummaryXmlTest extends TestCase {

    static final String TEST_DATA =
        "<TestResult>" +
            "<Summary failed=\"1\" notExecuted=\"2\" pass=\"3\" timeout=\"4\"/>" +
        "</TestResult>";

    static final String MISSING_DATA =
        "<TestResult>" +
            "<Foo failed=\"1\" notExecuted=\"2\" pass=\"3\" timeout=\"4\"/>" +
        "</TestResult>";

    public void testConstructor() throws ParseException  {
        TestSummaryXml result = new TestSummaryXml(1, "2011-11-01");
        assertEquals(1, result.getId());
        assertEquals("2011-11-01", result.getTimestamp());
    }

    /**
     * Simple test for parsing summary data
     */
    public void testParse() throws ParseException  {
        TestSummaryXml result = new TestSummaryXml(1, "2011-11-01");
        result.parse(getStringAsReader(TEST_DATA));
        // expect failed and timeout to be summed
        assertEquals(5, result.getNumFailed());
        assertEquals(2, result.getNumIncomplete());
        assertEquals(3, result.getNumPassed());
    }

    /**
     *  Test data where Summary tag is missing
     */
    public void testParse_missing() {
        TestSummaryXml result = new TestSummaryXml(1, "2011-11-01");
        try {
            result.parse(getStringAsReader(MISSING_DATA));
            fail("ParseException not thrown");
        } catch (ParseException e) {
            // expected
        }
    }

    private Reader getStringAsReader(String input) {
        return new StringReader(input);
    }
}
