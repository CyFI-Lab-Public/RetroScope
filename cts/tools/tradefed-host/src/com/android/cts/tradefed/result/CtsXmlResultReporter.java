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

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.tradefed.device.DeviceInfoCollector;
import com.android.cts.tradefed.testtype.CtsTest;
import com.android.cts.tradefed.util.CtsHostStore;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.build.IFolderBuildInfo;
import com.android.tradefed.config.Option;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.InputStreamSource;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.result.LogFileSaver;
import com.android.tradefed.result.TestSummary;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.StreamUtil;

import org.kxml2.io.KXmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes results to an XML files in the CTS format.
 * <p/>
 * Collects all test info in memory, then dumps to file when invocation is complete.
 * <p/>
 * Outputs xml in format governed by the cts_result.xsd
 */
public class CtsXmlResultReporter implements ITestInvocationListener {
    private static final String LOG_TAG = "CtsXmlResultReporter";

    static final String TEST_RESULT_FILE_NAME = "testResult.xml";
    private static final String CTS_RESULT_FILE_VERSION = "4.4";
    private static final String[] CTS_RESULT_RESOURCES = {"cts_result.xsl", "cts_result.css",
        "logo.gif", "newrule-green.png"};

    /** the XML namespace */
    static final String ns = null;

    static final String RESULT_TAG = "TestResult";
    static final String PLAN_ATTR = "testPlan";
    static final String STARTTIME_ATTR = "starttime";

    private static final String REPORT_DIR_NAME = "output-file-path";
    @Option(name=REPORT_DIR_NAME, description="root file system path to directory to store xml " +
            "test results and associated logs. If not specified, results will be stored at " +
            "<cts root>/repository/results")
    protected File mReportDir = null;

    // listen in on the plan option provided to CtsTest
    @Option(name = CtsTest.PLAN_OPTION, description = "the test plan to run.")
    private String mPlanName = "NA";

    // listen in on the continue-session option provided to CtsTest
    @Option(name = CtsTest.CONTINUE_OPTION, description = "the test result session to continue.")
    private Integer mContinueSessionId = null;

    @Option(name = "quiet-output", description = "Mute display of test results.")
    private boolean mQuietOutput = false;

    @Option(name = "result-server", description = "Server to publish test results.")
    private String mResultServer;

    protected IBuildInfo mBuildInfo;
    private String mStartTime;
    private String mDeviceSerial;
    private TestResults mResults = new TestResults();
    private TestPackageResult mCurrentPkgResult = null;
    private boolean mIsDeviceInfoRun = false;
    private ResultReporter mReporter;
    private File mLogDir;
    private String mSuiteName;

    private static final Pattern mCtsLogPattern = Pattern.compile("(.*)\\+\\+\\+\\+(.*)");

    public void setReportDir(File reportDir) {
        mReportDir = reportDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationStarted(IBuildInfo buildInfo) {
        mBuildInfo = buildInfo;
        if (!(buildInfo instanceof IFolderBuildInfo)) {
            throw new IllegalArgumentException("build info is not a IFolderBuildInfo");
        }
        IFolderBuildInfo ctsBuild = (IFolderBuildInfo)buildInfo;
        CtsBuildHelper ctsBuildHelper = getBuildHelper(ctsBuild);
        mDeviceSerial = buildInfo.getDeviceSerial() == null ? "unknown_device" :
            buildInfo.getDeviceSerial();
        if (mContinueSessionId != null) {
            CLog.d("Continuing session %d", mContinueSessionId);
            // reuse existing directory
            TestResultRepo resultRepo = new TestResultRepo(ctsBuildHelper.getResultsDir());
            mResults = resultRepo.getResult(mContinueSessionId);
            if (mResults == null) {
                throw new IllegalArgumentException(String.format("Could not find session %d",
                        mContinueSessionId));
            }
            mPlanName = resultRepo.getSummaries().get(mContinueSessionId).getTestPlan();
            mStartTime = resultRepo.getSummaries().get(mContinueSessionId).getStartTime();
            mReportDir = resultRepo.getReportDir(mContinueSessionId);
        } else {
            if (mReportDir == null) {
                mReportDir = ctsBuildHelper.getResultsDir();
            }
            mReportDir = createUniqueReportDir(mReportDir);

            mStartTime = getTimestamp();
            logResult("Created result dir %s", mReportDir.getName());
        }
        mSuiteName = ctsBuildHelper.getSuiteName();
        mReporter = new ResultReporter(mResultServer, mSuiteName);

        // TODO: allow customization of log dir
        // create a unique directory for saving logs, with same name as result dir
        File rootLogDir = getBuildHelper(ctsBuild).getLogsDir();
        mLogDir = new File(rootLogDir, mReportDir.getName());
        mLogDir.mkdirs();
    }

    /**
     * Create a unique directory for saving results.
     * <p/>
     * Currently using legacy CTS host convention of timestamp directory names. In case of
     * collisions, will use {@link FileUtil} to generate unique file name.
     * <p/>
     * TODO: in future, consider using LogFileSaver to create build-specific directories
     *
     * @param parentDir the parent folder to create dir in
     * @return the created directory
     */
    private static synchronized File createUniqueReportDir(File parentDir) {
        // TODO: in future, consider using LogFileSaver to create build-specific directories

        File reportDir = new File(parentDir, TimeUtil.getResultTimestamp());
        if (reportDir.exists()) {
            // directory with this timestamp exists already! Choose a unique, although uglier, name
            try {
                reportDir = FileUtil.createTempDir(TimeUtil.getResultTimestamp() + "_", parentDir);
            } catch (IOException e) {
                CLog.e(e);
                CLog.e("Failed to create result directory %s", reportDir.getAbsolutePath());
            }
        } else {
            if (!reportDir.mkdirs()) {
                // TODO: consider throwing an exception
                CLog.e("mkdirs failed when attempting to create result directory %s",
                        reportDir.getAbsolutePath());
            }
        }
        return reportDir;
    }

    /**
     * Helper method to retrieve the {@link CtsBuildHelper}.
     * @param ctsBuild
     */
    CtsBuildHelper getBuildHelper(IFolderBuildInfo ctsBuild) {
        CtsBuildHelper buildHelper = new CtsBuildHelper(ctsBuild.getRootDir());
        try {
            buildHelper.validateStructure();
        } catch (FileNotFoundException e) {
            // just log an error - it might be expected if we failed to retrieve a build
            CLog.e("Invalid CTS build %s", ctsBuild.getRootDir());
        }
        return buildHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testLog(String dataName, LogDataType dataType, InputStreamSource dataStream) {
        try {
            File logFile = getLogFileSaver().saveAndZipLogData(dataName, dataType,
                    dataStream.createInputStream());
            logResult(String.format("Saved log %s", logFile.getName()));
        } catch (IOException e) {
            CLog.e("Failed to write log for %s", dataName);
        }
    }

    /**
     * Return the {@link LogFileSaver} to use.
     * <p/>
     * Exposed for unit testing.
     */
    LogFileSaver getLogFileSaver() {
        return new LogFileSaver(mLogDir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunStarted(String name, int numTests) {
        if (mCurrentPkgResult != null && !name.equals(mCurrentPkgResult.getAppPackageName())) {
            // display results from previous run
            logCompleteRun(mCurrentPkgResult);
        }
        mIsDeviceInfoRun = name.equals(DeviceInfoCollector.APP_PACKAGE_NAME);
        if (mIsDeviceInfoRun) {
            logResult("Collecting device info");
        } else  {
            if (mCurrentPkgResult == null || !name.equals(mCurrentPkgResult.getAppPackageName())) {
                logResult("-----------------------------------------");
                logResult("Test package %s started", name);
                logResult("-----------------------------------------");
            }
            mCurrentPkgResult = mResults.getOrCreatePackage(name);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(TestIdentifier test) {
        mCurrentPkgResult.insertTest(test);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailed(TestFailure status, TestIdentifier test, String trace) {
        mCurrentPkgResult.reportTestFailure(test, CtsTestStatus.FAIL, trace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        collectCtsResults(test, testMetrics);
        mCurrentPkgResult.reportTestEnded(test);
        Test result = mCurrentPkgResult.findTest(test);
        String stack = result.getStackTrace() == null ? "" : "\n" + result.getStackTrace();
        logResult("%s#%s %s %s", test.getClassName(), test.getTestName(), result.getResult(),
                stack);
    }

    /**
     * Collect Cts results for both device and host tests to the package result.
     * @param test test ran
     * @param testMetrics test metrics which can contain performance result for device tests
     */
    private void collectCtsResults(TestIdentifier test, Map<String, String> testMetrics) {
        // device test can have performance results in testMetrics
        String perfResult = CtsReportUtil.getCtsResultFromMetrics(testMetrics);
        // host test should be checked in CtsHostStore.
        if (perfResult == null) {
            perfResult = CtsHostStore.removeCtsResult(mDeviceSerial, test.toString());
        }
        if (perfResult != null) {
            // CTS result is passed in Summary++++Details format.
            // Extract Summary and Details, and pass them.
            Matcher m = mCtsLogPattern.matcher(perfResult);
            if (m.find()) {
                mCurrentPkgResult.reportPerformanceResult(test, CtsTestStatus.PASS, m.group(1),
                        m.group(2));
            } else {
                logResult("CTS Result unrecognizable:" + perfResult);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        if (mIsDeviceInfoRun) {
            mResults.populateDeviceInfoMetrics(runMetrics);
        } else {
            mCurrentPkgResult.populateMetrics(runMetrics);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationEnded(long elapsedTime) {
        // display the results of the last completed run
        if (mCurrentPkgResult != null) {
            logCompleteRun(mCurrentPkgResult);
        }
        if (mReportDir == null || mStartTime == null) {
            // invocationStarted must have failed, abort
            CLog.w("Unable to create XML report");
            return;
        }

        File reportFile = getResultFile(mReportDir);
        createXmlResult(reportFile, mStartTime, elapsedTime);
        copyFormattingFiles(mReportDir);
        zipResults(mReportDir);

        try {
            mReporter.reportResult(reportFile);
        } catch (IOException e) {
            CLog.e(e);
        }
    }

    private void logResult(String format, Object... args) {
        if (mQuietOutput) {
            CLog.i(format, args);
        } else {
            Log.logAndDisplay(LogLevel.INFO, mDeviceSerial, String.format(format, args));
        }
    }

    private void logCompleteRun(TestPackageResult pkgResult) {
        if (pkgResult.getAppPackageName().equals(DeviceInfoCollector.APP_PACKAGE_NAME)) {
            logResult("Device info collection complete");
            return;
        }
        logResult("%s package complete: Passed %d, Failed %d, Not Executed %d",
                pkgResult.getAppPackageName(), pkgResult.countTests(CtsTestStatus.PASS),
                pkgResult.countTests(CtsTestStatus.FAIL),
                pkgResult.countTests(CtsTestStatus.NOT_EXECUTED));
    }

    /**
     * Creates a report file and populates it with the report data from the completed tests.
     */
    private void createXmlResult(File reportFile, String startTimestamp, long elapsedTime) {
        String endTime = getTimestamp();
        OutputStream stream = null;
        try {
            stream = createOutputResultStream(reportFile);
            KXmlSerializer serializer = new KXmlSerializer();
            serializer.setOutput(stream, "UTF-8");
            serializer.startDocument("UTF-8", false);
            serializer.setFeature(
                    "http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.processingInstruction("xml-stylesheet type=\"text/xsl\"  " +
                    "href=\"cts_result.xsl\"");
            serializeResultsDoc(serializer, startTimestamp, endTime);
            serializer.endDocument();
            String msg = String.format("XML test result file generated at %s. Passed %d, " +
                    "Failed %d, Not Executed %d", mReportDir.getName(),
                    mResults.countTests(CtsTestStatus.PASS),
                    mResults.countTests(CtsTestStatus.FAIL),
                    mResults.countTests(CtsTestStatus.NOT_EXECUTED));
            logResult(msg);
            logResult("Time: %s", TimeUtil.formatElapsedTime(elapsedTime));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to generate report data");
        } finally {
            StreamUtil.closeStream(stream);
        }
    }

    /**
     * Output the results XML.
     *
     * @param serializer the {@link KXmlSerializer} to use
     * @param startTime the user-friendly starting time of the test invocation
     * @param endTime the user-friendly ending time of the test invocation
     * @throws IOException
     */
    private void serializeResultsDoc(KXmlSerializer serializer, String startTime, String endTime)
            throws IOException {
        serializer.startTag(ns, RESULT_TAG);
        serializer.attribute(ns, PLAN_ATTR, mPlanName);
        serializer.attribute(ns, STARTTIME_ATTR, startTime);
        serializer.attribute(ns, "endtime", endTime);
        serializer.attribute(ns, "version", CTS_RESULT_FILE_VERSION);
        serializer.attribute(ns, "suite", mSuiteName);

        mResults.serialize(serializer);
        // TODO: not sure why, but the serializer doesn't like this statement
        //serializer.endTag(ns, RESULT_TAG);
    }

    private File getResultFile(File reportDir) {
        return new File(reportDir, TEST_RESULT_FILE_NAME);
    }

    /**
     * Creates the output stream to use for test results. Exposed for mocking.
     */
    OutputStream createOutputResultStream(File reportFile) throws IOException {
        logResult("Created xml report file at file://%s", reportFile.getAbsolutePath());
        return new FileOutputStream(reportFile);
    }

    /**
     * Copy the xml formatting files stored in this jar to the results directory
     *
     * @param resultsDir
     */
    private void copyFormattingFiles(File resultsDir) {
        for (String resultFileName : CTS_RESULT_RESOURCES) {
            InputStream configStream = getClass().getResourceAsStream(String.format("/report/%s",
                    resultFileName));
            if (configStream != null) {
                File resultFile = new File(resultsDir, resultFileName);
                try {
                    FileUtil.writeToFile(configStream, resultFile);
                } catch (IOException e) {
                    Log.w(LOG_TAG, String.format("Failed to write %s to file", resultFileName));
                }
            } else {
                Log.w(LOG_TAG, String.format("Failed to load %s from jar", resultFileName));
            }
        }
    }

    /**
     * Zip the contents of the given results directory.
     *
     * @param resultsDir
     */
    private void zipResults(File resultsDir) {
        try {
            // create a file in parent directory, with same name as resultsDir
            File zipResultFile = new File(resultsDir.getParent(), String.format("%s.zip",
                    resultsDir.getName()));
            FileUtil.createZip(resultsDir, zipResultFile);
        } catch (IOException e) {
            Log.w(LOG_TAG, String.format("Failed to create zip for %s", resultsDir.getName()));
        }
    }

    /**
     * Get a String version of the current time.
     * <p/>
     * Exposed so unit tests can mock.
     */
    String getTimestamp() {
        return TimeUtil.getTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunFailed(String errorMessage) {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testRunStopped(long elapsedTime) {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invocationFailed(Throwable cause) {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestSummary getSummary() {
        return null;
    }
}
