/*
 * Copyright 2012 The Android Open Source Project
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

import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.tradefed.log.LogUtil.CLog;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

public class WrappedGTestResultParser extends GeeTestResultParser {

    private boolean mInstrumentationError;

    /**
     * Creates the WrappedGTestResultParser.
     *
     * @param testRunName the test run name to provide to
     *            {@link ITestRunListener#testRunStarted(String, int)}
     * @param listeners informed of test results as the tests are executing
     */
    public WrappedGTestResultParser(String testRunName, Collection<ITestRunListener> listeners) {
        super(testRunName, listeners);
    }

    /**
     * Creates the WrappedGTestResultParser for a single listener.
     *
     * @param testRunName the test run name to provide to
     *            {@link ITestRunListener#testRunStarted(String, int)}
     * @param listener informed of test results as the tests are executing
     */
    public WrappedGTestResultParser(String testRunName, ITestRunListener listener) {
        super(testRunName, listener);
    }

    /**
     * Strips the instrumentation information and then forwards
     * the raw gtest output to the {@link GeeTestResultParser}.
     */
    @Override
    public void processNewLines(String[] lines) {
        if (mInstrumentationError) {
            return;
        }

        String[] gtestOutput = parseInstrumentation(lines);
        super.processNewLines(gtestOutput);
    }

    /**
     * Parses raw instrumentation output and returns the
     * contained gtest output
     *
     * @param lines the raw instrumentation output
     * @return the gtest output
     */
    public String[] parseInstrumentation(String[] lines) {
        List<String> output = new ArrayList<String>();
        boolean readMultiLine = false;
        for (String line : lines) {

            if (line.startsWith("INSTRUMENTATION_RESULT: ")) {
                CLog.e("Instrumentation Error:");
                mInstrumentationError = true;
            }

            if (mInstrumentationError) {
                CLog.e(line);
                continue;
            }

            if (line.startsWith("INSTRUMENTATION_STATUS: gtest=")) {
                output.add(line.replace("INSTRUMENTATION_STATUS: gtest=", ""));
                readMultiLine = true;
                continue;
            }

            if (line.startsWith("INSTRUMENTATION_")) {
                readMultiLine = false;
                continue;
            }

            if (readMultiLine) {
                output.add(line);
            }
        }

        return output.toArray(new String[output.size()]);
    }
}

