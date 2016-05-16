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

import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.Log;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.testtype.testdefs.XmlDefsTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO: Merge change to add package prefix to tradefed's GTestResultParser.
 *       Otherwise this file should be the same as TradeFed's version.
 *
 * Parses the 'raw output mode' results of native tests using GTest that run from shell, and informs
 * a ITestRunListener of the results.
 * <p>Sample format of output expected:
 *
 * <pre>
 * [==========] Running 15 tests from 1 test case.
 * [----------] Global test environment set-up.
 * [----------] 15 tests from MessageTest
 * [ RUN      ] MessageTest.DefaultConstructor
 * [       OK ] MessageTest.DefaultConstructor (1 ms)
 * [ RUN      ] MessageTest.CopyConstructor
 * external/gtest/test/gtest-message_test.cc:67: Failure
 * Value of: 5
 * Expected: 2
 * external/gtest/test/gtest-message_test.cc:68: Failure
 * Value of: 1 == 1
 * Actual: true
 * Expected: false
 * [  FAILED  ] MessageTest.CopyConstructor (2 ms)
 *  ...
 * [ RUN      ] MessageTest.DoesNotTakeUpMuchStackSpace
 * [       OK ] MessageTest.DoesNotTakeUpMuchStackSpace (0 ms)
 * [----------] 15 tests from MessageTest (26 ms total)
 *
 * [----------] Global test environment tear-down
 * [==========] 15 tests from 1 test case ran. (26 ms total)
 * [  PASSED  ] 6 tests.
 * [  FAILED  ] 9 tests, listed below:
 * [  FAILED  ] MessageTest.CopyConstructor
 * [  FAILED  ] MessageTest.ConstructsFromCString
 * [  FAILED  ] MessageTest.StreamsCString
 * [  FAILED  ] MessageTest.StreamsNullCString
 * [  FAILED  ] MessageTest.StreamsString
 * [  FAILED  ] MessageTest.StreamsStringWithEmbeddedNUL
 * [  FAILED  ] MessageTest.StreamsNULChar
 * [  FAILED  ] MessageTest.StreamsInt
 * [  FAILED  ] MessageTest.StreamsBasicIoManip
 * 9 FAILED TESTS
 * </pre>
 *
 * <p>where the following tags are used to signal certain events:
 * <pre>
 * [==========]: the first occurrence indicates a new run started, including the number of tests
 *                  to be expected in this run
 * [ RUN      ]: indicates a new test has started to run; a series of zero or more lines may
 *                  follow a test start, and will be captured in case of a test failure or error
 * [       OK ]: the preceding test has completed successfully, optionally including the time it
 *                  took to run (in ms)
 * [  FAILED  ]: the preceding test has failed, optionally including the time it took to run (in ms)
 * [==========]: the preceding test run has completed, optionally including the time it took to run
 *                  (in ms)
 * </pre>
 *
 * All other lines are ignored.
 */
public class GeeTestResultParser extends MultiLineReceiver {
    private static final String LOG_TAG = "GTestResultParser";

    // Variables to keep track of state
    private TestResult mCurrentTestResult = null;
    private int mNumTestsRun = 0;
    private int mNumTestsExpected = 0;
    private long mTotalRunTime = 0;
    private boolean mTestInProgress = false;
    private boolean mTestRunInProgress = false;
    private final String mTestRunName;
    private final Collection<ITestRunListener> mTestListeners;

    /** Fake adding a package prefix if the test listener needs it. */
    private String mFakePackagePrefix = "";

    /** True if start of test has already been reported to listener. */
    private boolean mTestRunStartReported = false;

    /** True if current test run has been canceled by user. */
    private boolean mIsCancelled = false;

    private String mCoverageTarget = null;

    /**
     * Test result data
     */
    private static class TestResult {
        private String mTestName = null;
        private String mTestClass = null;
        private StringBuilder mStackTrace = null;
        @SuppressWarnings("unused")
        private Long mRunTime = null;

        /** Returns whether expected values have been parsed
         *
         * @return true if all expected values have been parsed
         */
        boolean isComplete() {
            return mTestName != null && mTestClass != null;
        }

        /** Returns whether there is currently a stack trace
         *
         * @return true if there is currently a stack trace, false otherwise
         */
        boolean hasStackTrace() {
            return mStackTrace != null;
        }

        /**
         * Returns the stack trace of the current test.
         *
         * @return a String representation of the current test's stack trace; if there is not
         * a current stack trace, it returns an error string. Use {@link TestResult#hasStackTrace}
         * if you need to know whether there is a stack trace.
         */
        String getTrace() {
            if (hasStackTrace()) {
                return mStackTrace.toString();
            } else {
                Log.e(LOG_TAG, "Could not find stack trace for failed test");
                return new Throwable("Unknown failure").toString();
            }
        }

        /** Provides a more user readable string for TestResult, if possible */
        @Override
        public String toString() {
            StringBuilder output = new StringBuilder();
            if (mTestClass != null ) {
                output.append(mTestClass);
                output.append('#');
            }
            if (mTestName != null) {
                output.append(mTestName);
            }
            if (output.length() > 0) {
                return output.toString();
            }
            return "unknown result";
        }
    }

    /** Internal helper struct to store parsed test info. */
    private static class ParsedTestInfo {
        String mTestName = null;
        String mTestClassName = null;
        String mTestRunTime = null;

        public ParsedTestInfo(String testName, String testClassName, String testRunTime) {
            mTestName = testName;
            mTestClassName = testClassName;
            mTestRunTime = testRunTime;
        }
    }

    /** Prefixes used to demarcate and identify output. */
    private static class Prefixes {
        @SuppressWarnings("unused")
        private static final String INFORMATIONAL_MARKER = "[----------]";
        private static final String START_TEST_RUN_MARKER = "[==========] Running";
        private static final String TEST_RUN_MARKER = "[==========]";
        private static final String START_TEST_MARKER = "[ RUN      ]";
        private static final String OK_TEST_MARKER = "[       OK ]";
        private static final String FAILED_TEST_MARKER = "[  FAILED  ]";
    }

    /**
     * Creates the GTestResultParser.
     *
     * @param testRunName the test run name to provide to
     *            {@link ITestRunListener#testRunStarted(String, int)}
     * @param listeners informed of test results as the tests are executing
     */
    public GeeTestResultParser(String testRunName, Collection<ITestRunListener> listeners) {
        mTestRunName = testRunName;
        mTestListeners = new ArrayList<ITestRunListener>(listeners);
    }

    /**
     * Creates the GTestResultParser for a single listener.
     *
     * @param testRunName the test run name to provide to
     *            {@link ITestRunListener#testRunStarted(String, int)}
     * @param listener informed of test results as the tests are executing
     */
    public GeeTestResultParser(String testRunName, ITestRunListener listener) {
        mTestRunName = testRunName;
        mTestListeners = new ArrayList<ITestRunListener>(1);
        mTestListeners.add(listener);
    }

    /**
     * Package prefix to be added to test names when they are reported like
     * "android.nativemedia." You may need to add the dot if you need it.
     */
    public void setFakePackagePrefix(String prefix) {
        mFakePackagePrefix = prefix;
    }

    /**
     * Returns the current TestResult for test in progress, or a new default one.
     *
     * @return The TestResult for the current test run
     */
    private TestResult getCurrentTestResult() {
        if (mCurrentTestResult == null) {
            mCurrentTestResult = new TestResult();
        }
        return mCurrentTestResult;
    }


    /**
     * Clears out the current TestResult.
     */
    private void clearCurrentTestResult() {
        mCurrentTestResult = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processNewLines(String[] lines) {
        for (String line : lines) {
            parse(line);
        }
    }

    /**
     * Parse an individual output line.
     *
     * @param line  Text output line
     */
    private void parse(String line) {
        String message = null;

        if (mTestRunInProgress || line.startsWith(Prefixes.TEST_RUN_MARKER)) {
            if (line.startsWith(Prefixes.START_TEST_MARKER)) {
                // Individual test started
                message = line.substring(Prefixes.START_TEST_MARKER.length()).trim();
                processTestStartedTag(message);
            }
            else if (line.startsWith(Prefixes.OK_TEST_MARKER)) {
                // Individual test completed successfully
                message = line.substring(Prefixes.OK_TEST_MARKER.length()).trim();
                processOKTag(message);
                clearCurrentTestResult();
            }
            else if (line.startsWith(Prefixes.FAILED_TEST_MARKER)) {
                // Individual test completed with failure
                message = line.substring(Prefixes.FAILED_TEST_MARKER.length()).trim();
                processFailedTag(message);
                clearCurrentTestResult();
            }
            else if (line.startsWith(Prefixes.START_TEST_RUN_MARKER)) {
                // Test run started
                // Make sure to leave the "Running" in the string
                message = line.substring(Prefixes.TEST_RUN_MARKER.length()).trim();
                processRunStartedTag(message);
            }
            else if (line.startsWith(Prefixes.TEST_RUN_MARKER)) {
                // Test run ended
                // This is for the end of the test suite run, so make sure this else-if is after the
                // check for START_TEST_SUITE_MARKER
                message = line.substring(Prefixes.TEST_RUN_MARKER.length()).trim();
                processRunCompletedTag(message);
            }
            else if (testInProgress()) {
                // Note this does not handle the case of an error outside an actual test run
                appendTestOutputLine(line);
            }
        }
    }

    /**
     * Returns true if test run canceled.
     *
     * @see IShellOutputReceiver#isCancelled()
     */
    public boolean isCancelled() {
        return mIsCancelled;
    }

    /**
     * Requests cancellation of test run.
     */
    public void cancel() {
        mIsCancelled = true;
    }

    /**
     * Returns whether we're in the middle of running a test.
     *
     * @return True if a test was started, false otherwise
     */
    private boolean testInProgress() {
        return mTestInProgress;
    }

    /**
     * Set state to indicate we've started running a test.
     *
     */
    private void setTestStarted() {
        mTestInProgress = true;
    }

    /**
     * Set state to indicate we've started running a test.
     *
     */
    private void setTestEnded() {
        mTestInProgress = false;
    }

    /**
     * Reports the start of a test run, and the total test count, if it has not been previously
     * reported.
     */
    private void reportTestRunStarted() {
        // if start test run not reported yet
        if (!mTestRunStartReported) {
            for (ITestRunListener listener : mTestListeners) {
                listener.testRunStarted(mTestRunName, mNumTestsExpected);
            }
            mTestRunStartReported = true;
        }
    }

    /**
     * Reports the end of a test run, and resets that test
     */
    private void reportTestRunEnded() {
        for (ITestRunListener listener : mTestListeners) {
            listener.testRunEnded(mTotalRunTime, getRunMetrics());
        }
        mTestRunStartReported = false;
    }

    /**
     * Create the run metrics {@link Map} to report.
     *
     * @return a {@link Map} of run metrics data
     */
    private Map<String, String> getRunMetrics() {
        Map<String, String> metricsMap = new HashMap<String, String>();
        if (mCoverageTarget != null) {
            metricsMap.put(XmlDefsTest.COVERAGE_TARGET_KEY, mCoverageTarget);
        }
        return metricsMap;
    }

    /**
     * Parse the test identifier (class and test name), and optional time info.
     *
     * @param identifier Raw identifier of the form classname.testname, with an optional time
     *          element in the format of (XX ms) at the end
     * @return A ParsedTestInfo representing the parsed info from the identifier string.
     *
     *          If no time tag was detected, then the third element in the array (time_in_ms) will
     *          be null. If the line failed to parse properly (eg: could not determine name of
     *          test/class) then an "UNKNOWN" string value will be returned for the classname and
     *          testname. This method guarantees a string will always be returned for the class and
     *          test names (but not for the time value).
     */
    private ParsedTestInfo parseTestIdentifier(String identifier) {
        ParsedTestInfo returnInfo = new ParsedTestInfo("UNKNOWN_CLASS", "UNKNOWN_TEST", null);

        Pattern timePattern = Pattern.compile(".*(\\((\\d+) ms\\))");  // eg: (XX ms)
        Matcher time = timePattern.matcher(identifier);

        // Try to find a time
        if (time.find()) {
            String timeString = time.group(2);  // the "XX" in "(XX ms)"
            String discardPortion = time.group(1);  // everything after the test class/name
            identifier = identifier.substring(0, identifier.lastIndexOf(discardPortion)).trim();
            returnInfo.mTestRunTime = timeString;
        }

        String[] testId = identifier.split("\\.");
        if (testId.length < 2) {
            Log.e(LOG_TAG, "Could not detect the test class and test name, received: " +
                    identifier);
        }
        else {
            returnInfo.mTestClassName = testId[0];
            returnInfo.mTestName = testId[1];
        }
        return returnInfo;
    }

    /**
     * Parses and stores the test identifier (class and test name).
     *
     * @param identifier Raw identifier
     */
    private void processRunStartedTag(String identifier) {
        // eg: (Running XX tests from 1 test case.)
        Pattern numTestsPattern = Pattern.compile("Running (\\d+) test[s]? from .*");
        Matcher numTests = numTestsPattern.matcher(identifier);

        // Try to find number of tests
        if (numTests.find()) {
            try {
                mNumTestsExpected = Integer.parseInt(numTests.group(1));
            }
            catch (NumberFormatException e) {
                Log.e(LOG_TAG, "Unable to determine number of tests expected, received: " +
                        numTests.group(1));
            }
        }
        if (mNumTestsExpected > 0) {
          reportTestRunStarted();
          mNumTestsRun = 0;
          mTestRunInProgress = true;
        }
    }

    /**
     * Processes and informs listener when we encounter a tag indicating that a test suite is done.
     *
     * @param identifier Raw log output from the suite ended tag
     */
    private void processRunCompletedTag(String identifier) {
        Pattern timePattern = Pattern.compile(".*\\((\\d+) ms total\\)");  // eg: (XX ms total)
        Matcher time = timePattern.matcher(identifier);

        // Try to find the total run time
        if (time.find()) {
            try {
                mTotalRunTime = Long.parseLong(time.group(1));
            }
            catch (NumberFormatException e) {
                Log.e(LOG_TAG, "Unable to determine the total running time, received: " +
                        time.group(1));
            }
        }
        reportTestRunEnded();
        mTestRunInProgress = false;
    }

    /**
     * Processes and informs listener when we encounter a tag indicating that a test has started.
     *
     * @param identifier Raw log output of the form classname.testname, with an optional time (x ms)
     */
    private void processTestStartedTag(String identifier) {
        ParsedTestInfo parsedResults = parseTestIdentifier(identifier);
        TestResult testResult = getCurrentTestResult();
        testResult.mTestClass = parsedResults.mTestClassName;
        testResult.mTestName = parsedResults.mTestName;

        TestIdentifier testId = new TestIdentifier(mFakePackagePrefix + testResult.mTestClass,
                testResult.mTestName);

        for (ITestRunListener listener : mTestListeners) {
            listener.testStarted(testId);
        }
        setTestStarted();
    }

    /**
     * Helper method to do the work necessary when a test has ended.
     *
     * @param identifier Raw log output of the form "classname.testname" with an optional (XX ms)
     *          at the end indicating the running time.
     * @param testPassed Indicates whether the test passed or failed (set to true if passed, false
     *          if failed)
     */
    private void doTestEnded(String identifier, boolean testPassed) {
        ParsedTestInfo parsedResults = parseTestIdentifier(identifier);
        TestResult testResult = getCurrentTestResult();
        TestIdentifier testId = new TestIdentifier(mFakePackagePrefix + testResult.mTestClass,
                testResult.mTestName);

        // Error - trying to end a test when one isn't in progress
        if (!testInProgress()) {
            Log.e(LOG_TAG, "Test currently not in progress when trying to end test: " + identifier);
            return;
        }

        // Save the run time for this test if one exists
        if (parsedResults.mTestRunTime != null) {
            try {
                testResult.mRunTime = new Long(parsedResults.mTestRunTime);
            }
            catch (NumberFormatException e) {
                Log.e(LOG_TAG, "Test run time value is invalid, received: " +
                        parsedResults.mTestRunTime);
            }
        }

        // Check that the test result is for the same test/class we're expecting it to be for
        boolean encounteredUnexpectedTest = false;
        if (!testResult.isComplete()) {
            Log.e(LOG_TAG, "No test/class name is currently recorded as running!");
        }
        else {
            if (testResult.mTestClass.compareTo(parsedResults.mTestClassName) != 0) {
                Log.e(LOG_TAG, "Name for current test class does not match class we started " +
                        "with, expected: " + testResult.mTestClass + " but got: " +
                        parsedResults.mTestClassName);
                encounteredUnexpectedTest = true;
            }
            if (testResult.mTestName.compareTo(parsedResults.mTestName) != 0) {
                Log.e(LOG_TAG, "Name for current test does not match test we started with," +
                        "expected: " + testResult.mTestName + " bug got: " +
                        parsedResults.mTestName);
                encounteredUnexpectedTest = true;
            }
        }

        if (encounteredUnexpectedTest) {
            // If the test name of the result changed from what we started with, report that
            // the last known test failed, regardless of whether we received a pass or fail tag.
            for (ITestRunListener listener : mTestListeners) {
                listener.testFailed(ITestRunListener.TestFailure.ERROR, testId,
                                mCurrentTestResult.getTrace());
            }
        }
        else if (!testPassed) {  // test failed
            for (ITestRunListener listener : mTestListeners) {
                listener.testFailed(ITestRunListener.TestFailure.FAILURE, testId,
                                mCurrentTestResult.getTrace());
            }
        }
        // For all cases (pass or fail), we ultimately need to report test has ended
        Map <String, String> emptyMap = Collections.emptyMap();
        for (ITestRunListener listener : mTestListeners) {
            // @TODO: Add reporting of test run time to ITestRunListener
            listener.testEnded(testId, emptyMap);
        }

        setTestEnded();
        ++mNumTestsRun;
    }

    /**
     * Processes and informs listener when we encounter the OK tag.
     *
     * @param identifier Raw log output of the form "classname.testname" with an optional (XX ms)
     *          at the end indicating the running time.
     */
    private void processOKTag(String identifier) {
        doTestEnded(identifier, true);
    }

    /**
     * Processes and informs listener when we encounter the FAILED tag.
     *
     * @param identifier Raw log output of the form "classname.testname" with an optional (XX ms)
     *          at the end indicating the running time.
     */
    private void processFailedTag(String identifier) {
        doTestEnded(identifier, false);
    }

    /**
     * Appends the test output to the current TestResult.
     *
     * @param line Raw test result line of output.
     */
    private void appendTestOutputLine(String line) {
        TestResult testResult = getCurrentTestResult();
        if (testResult.mStackTrace == null) {
            testResult.mStackTrace = new StringBuilder();
        }
        else {
            testResult.mStackTrace.append("\r\n");
        }
        testResult.mStackTrace.append(line);
    }

    /**
     * Process an instrumentation run failure
     *
     * @param errorMsg The message to output about the nature of the error
     */
    private void handleTestRunFailed(String errorMsg) {
        errorMsg = (errorMsg == null ? "Unknown error" : errorMsg);
        Log.i(LOG_TAG, String.format("Test run failed: %s", errorMsg));
        String testRunStackTrace = "";

        // Report that the last known test failed
        if ((mCurrentTestResult != null) && (mCurrentTestResult.isComplete())) {
            // current test results are cleared out after every complete test run,
            // if it's not null, assume the last test caused this and report as a test failure
            TestIdentifier testId = new TestIdentifier(mCurrentTestResult.mTestClass,
                    mCurrentTestResult.mTestName);

            // If there was any stack trace during the test run, append it to the "test failed"
            // error message so we have an idea of what caused the crash/failure.
            Map<String, String> emptyMap = Collections.emptyMap();
            if (mCurrentTestResult.hasStackTrace()) {
                testRunStackTrace = mCurrentTestResult.getTrace();
            }
            for (ITestRunListener listener : mTestListeners) {
                listener.testFailed(ITestRunListener.TestFailure.ERROR, testId,
                        "No test results.\r\n" + testRunStackTrace);
                listener.testEnded(testId, emptyMap);
            }
            clearCurrentTestResult();
        }
        // Report the test run failed
        for (ITestRunListener listener : mTestListeners) {
            listener.testRunFailed(errorMsg);
            listener.testRunEnded(mTotalRunTime, getRunMetrics());
        }
    }

    /**
     * Called by parent when adb session is complete.
     */
    @Override
    public void done() {
        super.done();
        if (mNumTestsExpected > mNumTestsRun) {
            handleTestRunFailed(String.format("Test run incomplete. Expected %d tests, received %d",
                    mNumTestsExpected, mNumTestsRun));
        }
        else if (mTestRunInProgress) {
            handleTestRunFailed("No test results");
        }
    }

    /**
     * Sets the coverage target for this test.
     * <p/>
     * Will be sent as a metric to test listeners.
     *
     * @param coverageTarget the coverage target
     */
    public void setCoverageTarget(String coverageTarget) {
        mCoverageTarget = coverageTarget;
    }
}
