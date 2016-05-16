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

import com.android.ddmlib.testrunner.ITestRunListener.TestFailure;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IFolderBuildInfo;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.XmlResultReporter;
import com.android.tradefed.util.FileUtil;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Unit tests for {@link XmlResultReporter}.
 */
public class CtsXmlResultReporterTest extends TestCase {

    private CtsXmlResultReporter mResultReporter;
    private ByteArrayOutputStream mOutputStream;
    private File mReportDir;
    private IFolderBuildInfo mMockBuild;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mOutputStream = new ByteArrayOutputStream();
        mResultReporter = new CtsXmlResultReporter() {
            @Override
            OutputStream createOutputResultStream(File reportDir) throws IOException {
                return mOutputStream;
            }

            @Override
            String getTimestamp() {
                return "ignore";
            }
        };
        // TODO: use mock file dir instead
        mReportDir = FileUtil.createTempDir("foo");
        mResultReporter.setReportDir(mReportDir);
        mMockBuild = EasyMock.createNiceMock(IFolderBuildInfo.class);
        EasyMock.expect(mMockBuild.getDeviceSerial()).andStubReturn(null);
        EasyMock.replay(mMockBuild);
    }

    @Override
    protected void tearDown() throws Exception {
        if (mReportDir != null) {
            FileUtil.recursiveDelete(mReportDir);
        }
        super.tearDown();
    }

    /**
     * A simple test to ensure expected output is generated for test run with no tests.
     */
    public void testEmptyGeneration() {
        final String expectedHeaderOutput = "<?xml version='1.0' encoding='UTF-8' standalone='no' ?>" +
            "<?xml-stylesheet type=\"text/xsl\" href=\"cts_result.xsl\"?>";
        final String expectedTestOutput =
            "<TestResult testPlan=\"NA\" starttime=\"ignore\" endtime=\"ignore\" version=\"1.13\"> ";
        final String expectedSummaryOutput =
            "<Summary failed=\"0\" notExecuted=\"0\" timeout=\"0\" pass=\"0\" />";
        final String expectedEndTag = "</TestResult>";
        mResultReporter.invocationStarted(mMockBuild);
        mResultReporter.invocationEnded(1);
        String actualOutput = getOutput();
        assertTrue(actualOutput.startsWith(expectedHeaderOutput));
        assertTrue(String.format("test output did not contain expected test result. Got %s",
                actualOutput), actualOutput.contains(expectedTestOutput));
        assertTrue(String.format("test output did not contain expected test summary. Got %s",
                actualOutput), actualOutput.contains(expectedSummaryOutput));
        assertTrue(String.format("test output did not contain expected TestResult end tag. Got %s",
                actualOutput), actualOutput.endsWith(expectedEndTag));
    }

    /**
     * A simple test to ensure expected output is generated for test run with a single passed test.
     */
    public void testSinglePass() {
        Map<String, String> emptyMap = Collections.emptyMap();
        final TestIdentifier testId = new TestIdentifier("com.foo.FooTest", "testFoo");
        mResultReporter.invocationStarted(mMockBuild);
        mResultReporter.testRunStarted("run", 1);
        mResultReporter.testStarted(testId);
        mResultReporter.testEnded(testId, emptyMap);
        mResultReporter.testRunEnded(3000, emptyMap);
        mResultReporter.invocationEnded(1);
        String output =  getOutput();
        CLog.d("Actual output: %s", output);
        System.out.println(output);
        // TODO: consider doing xml based compare
        assertTrue(output.contains(
                "<Summary failed=\"0\" notExecuted=\"0\" timeout=\"0\" pass=\"1\" />"));
        assertTrue(output.contains("<TestPackage name=\"\" appPackageName=\"run\" digest=\"\">"));
        assertTrue(output.contains("<TestCase name=\"FooTest\" priority=\"\">"));

        final String testCaseTag = String.format(
                "<Test name=\"%s\" result=\"pass\"", testId.getTestName());
        assertTrue(output.contains(testCaseTag));
    }

    /**
     * A simple test to ensure expected output is generated for test run with a single failed test.
     */
    public void testSingleFail() {
        Map<String, String> emptyMap = Collections.emptyMap();
        final TestIdentifier testId = new TestIdentifier("FooTest", "testFoo");
        final String trace = "this is a trace\nmore trace\nyet more trace";
        mResultReporter.invocationStarted(mMockBuild);
        mResultReporter.testRunStarted("run", 1);
        mResultReporter.testStarted(testId);
        mResultReporter.testFailed(TestFailure.FAILURE, testId, trace);
        mResultReporter.testEnded(testId, emptyMap);
        mResultReporter.testRunEnded(3, emptyMap);
        mResultReporter.invocationEnded(1);
        String output =  getOutput();
        System.out.print(getOutput());
        // TODO: consider doing xml based compare
        assertTrue(output.contains(
                "<Summary failed=\"1\" notExecuted=\"0\" timeout=\"0\" pass=\"0\" />"));
        final String failureTag =
            "<FailedScene message=\"this is a trace&#10;more trace\">     " +
            "<StackTrace>this is a tracemore traceyet more trace</StackTrace>";
        assertTrue(output.contains(failureTag));
    }

    /**
     * Gets the output produced, stripping it of extraneous whitespace characters.
     */
    private String getOutput() {
        String output = mOutputStream.toString();
        // ignore newlines and tabs whitespace
        output = output.replaceAll("[\\r\\n\\t]", "");
        // replace two ws chars with one
        return output.replaceAll("  ", " ");
    }
}
