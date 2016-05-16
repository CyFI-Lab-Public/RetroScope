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
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.config.Option;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.InputStreamSource;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.result.TestSummary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Class that sends a HTTP POST multipart/form-data request containing details
 * about a test failure.
 */
public class IssueReporter implements ITestInvocationListener {

    private static final int BUGREPORT_SIZE = 500 * 1024;

    private static final String PRODUCT_NAME_KEY = "buildName";
    private static final String BUILD_TYPE_KEY = "build_type";
    private static final String BUILD_ID_KEY = "buildID";

    @Option(name = "issue-server", description = "Server url to post test failures to.")
    private String mServerUrl;

    private final ExecutorService mReporterService = Executors.newCachedThreadPool();

    private Issue mCurrentIssue;
    private String mBuildId;
    private String mBuildType;
    private String mProductName;

    @Override
    public void testFailed(TestFailure status, TestIdentifier test, String trace) {
        mCurrentIssue = new Issue();
        mCurrentIssue.mTestName = test.toString();
        mCurrentIssue.mStackTrace = trace;
    }

    @Override
    public void testLog(String dataName, LogDataType dataType, InputStreamSource dataStream) {
        if (dataName.startsWith("bug-")) {
            try {
                setBugReport(dataStream);
            } catch (IOException e) {
                CLog.e(e);
            }
        }
    }

    /**
     * Set the bug report for the current test failure. GZip it to save space.
     * This is only called when the --bugreport option is enabled.
     */
    private void setBugReport(InputStreamSource dataStream) throws IOException {
        if (mCurrentIssue != null) {
            // Only one bug report can be stored at a time and they are gzipped to
            // about 0.5 MB so there shoudn't be any memory leak bringing down CTS.
            InputStream input = null;
            try {
                input = dataStream.createInputStream();
                mCurrentIssue.mBugReport = getBytes(input, BUGREPORT_SIZE);
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        } else {
            CLog.e("setBugReport is getting called on an empty issue...");
        }
    }

    /**
     * @param input that will be gzipped and returne as a byte array
     * @param size of the output expected
     * @return the byte array with the input's data
     * @throws IOException
     */
    static byte[] getBytes(InputStream input, int size) throws IOException {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(size);
        GZIPOutputStream gzipOutput = new GZIPOutputStream(byteOutput);
        for (byte[] buffer = new byte[1024]; ; ) {
            int numRead = input.read(buffer);
            if (numRead < 0) {
                break;
            }
            gzipOutput.write(buffer, 0, numRead);
        }
        gzipOutput.close();
        return byteOutput.toByteArray();
    }

    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        if (mCurrentIssue != null) {
            mReporterService.submit(mCurrentIssue);
            mCurrentIssue = null;
        }
    }

    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        setDeviceMetrics(runMetrics);
    }

    /** Set device information. Populated once when the device info app runs. */
    private void setDeviceMetrics(Map<String, String> metrics) {
        if (metrics.containsKey(BUILD_ID_KEY)) {
            mBuildId = metrics.get(BUILD_ID_KEY);
        }
        if (metrics.containsKey(BUILD_TYPE_KEY)) {
            mBuildType = metrics.get(BUILD_TYPE_KEY);
        }
        if (metrics.containsKey(PRODUCT_NAME_KEY)) {
            mProductName = metrics.get(PRODUCT_NAME_KEY);
        }
    }

    @Override
    public void invocationEnded(long elapsedTime) {
        try {
            mReporterService.shutdown();
            if (!mReporterService.awaitTermination(1, TimeUnit.MINUTES)) {
                CLog.i("Some issues could not be reported...");
            }
        } catch (InterruptedException e) {
            CLog.e(e);
        }
    }

    class Issue implements Callable<Void> {

        private String mTestName;
        private String mStackTrace;
        private byte[] mBugReport;

        @Override
        public Void call() throws Exception {
            if (isEmpty(mServerUrl)
                    || isEmpty(mBuildId)
                    || isEmpty(mBuildType)
                    || isEmpty(mProductName)
                    || isEmpty(mTestName)
                    || isEmpty(mStackTrace)) {
                return null;
            }

            new MultipartForm(mServerUrl)
                    .addFormValue("productName", mProductName)
                    .addFormValue("buildType", mBuildType)
                    .addFormValue("buildId", mBuildId)
                    .addFormValue("testName", mTestName)
                    .addFormValue("stackTrace", mStackTrace)
                    .addFormFile("bugReport", "bugreport.txt.gz", mBugReport)
                    .submit();

            return null;
        }

        private boolean isEmpty(String value) {
            return value == null || value.trim().isEmpty();
        }
    }

    @Override
    public void invocationStarted(IBuildInfo buildInfo) {
    }

    @Override
    public void testRunStarted(String name, int numTests) {
    }

    @Override
    public void testStarted(TestIdentifier test) {
    }

    @Override
    public void testRunFailed(String arg0) {
    }

    @Override
    public void testRunStopped(long elapsedTime) {
    }

    @Override
    public void invocationFailed(Throwable cause) {
    }

    @Override
    public TestSummary getSummary() {
        return null;
    }
}
