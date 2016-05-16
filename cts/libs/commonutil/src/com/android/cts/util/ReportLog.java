/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.util;

import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;


/**
 * Utility class to print performance measurement result back to host.
 * For now, throws know exception with message.
 *
 * Format:
 * Message = summary log SUMMARY_SEPARATOR [LOG_SEPARATOR log]*
 * summary = message|target|unit|type|value, target can be " " if there is no target set.
 * log for array = classMethodName:line_number|message|unit|type|space seSummaryparated values
 */
public class ReportLog {
    private static final String LOG_SEPARATOR = "+++";
    private static final String SUMMARY_SEPARATOR = "++++";
    private static final String LOG_ELEM_SEPARATOR = "|";

    private List<String> mMessages = new LinkedList<String> ();
    private String mSummary = null;
    protected static int mDepth = 3;

    /**
     * print array of values to output log
     */
    public void printArray(String message, double[] values, ResultType type,
            ResultUnit unit) {
        doPrintArray(message, values, type, unit);
    }

    /**
     * Print a value to output log
     */
    public void printValue(String message, double value, ResultType type,
            ResultUnit unit) {
        double[] vals = { value };
        doPrintArray(message, vals, type, unit);
    }

    private void doPrintArray(String message, double[] values, ResultType type,
    ResultUnit unit) {
        StringBuilder builder = new StringBuilder();
        // note mDepth + 1 as this function will be called by printVaue or printArray
        // and we need caller of printValue / printArray
        builder.append(getClassMethodNames(mDepth + 1, true) + LOG_ELEM_SEPARATOR + message +
                LOG_ELEM_SEPARATOR + type.getXmlString() + LOG_ELEM_SEPARATOR +
                unit.getXmlString() + LOG_ELEM_SEPARATOR);
        for (double v : values) {
            builder.append(v);
            builder.append(" ");
        }
        mMessages.add(builder.toString());
        printLog(builder.toString());
    }

    /**
     * record the result of benchmarking with performance target.
     * Depending on the ResultType, the function can fail if the result
     * does not meet the target. For example, for the type of HIGHER_BETTER,
     * value of 1.0 with target of 2.0 will fail.
     *
     * @param message message to be printed in the final report
     * @param target target performance for the benchmarking
     * @param value measured value
     * @param type
     * @param unit
     */
    public void printSummaryWithTarget(String message, double target, double value,
            ResultType type, ResultUnit unit) {
        mSummary = message + LOG_ELEM_SEPARATOR + target + LOG_ELEM_SEPARATOR + type.getXmlString()
                + LOG_ELEM_SEPARATOR + unit.getXmlString() + LOG_ELEM_SEPARATOR + value;
        boolean resultOk = true;
        if (type == ResultType.HIGHER_BETTER) {
            resultOk = value >= target;
        } else if (type == ResultType.LOWER_BETTER) {
            resultOk = value <= target;
        }
        if (!resultOk) {
            Assert.fail("Measured result " + value + " does not meet perf target " + target +
                    " with type " + type.getXmlString());
        }
    }

    /**
     * For standard report summary without target value.
     * Note that this function will not fail as there is no target.
     * @param messsage
     * @param value
     * @param type type of the value
     * @param unit unit of the data
     */
    public void printSummary(String message, double value, ResultType type,
            ResultUnit unit) {
        mSummary = message + LOG_ELEM_SEPARATOR + " " + LOG_ELEM_SEPARATOR + type.getXmlString() +
                LOG_ELEM_SEPARATOR + unit.getXmlString() + LOG_ELEM_SEPARATOR + value;
    }

    protected String generateReport() {
        if ((mSummary == null) && mMessages.isEmpty()) {
            // just return empty string
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(mSummary);
        builder.append(SUMMARY_SEPARATOR);
        for (String entry : mMessages) {
            builder.append(entry);
            builder.append(LOG_SEPARATOR);
        }
        // delete the last separator
        if (builder.length() >= LOG_SEPARATOR.length()) {
            builder.delete(builder.length() - LOG_SEPARATOR.length(), builder.length());
        }
        mSummary = null;
        mMessages.clear();
        return builder.toString();
    }

    /**
     * calculate rate per sec for given change happened during given timeInMSec.
     * timeInSec with 0 value will be changed to small value to prevent divide by zero.
     * @param change total change of quality for the given duration timeInMSec.
     * @param timeInMSec
     * @return
     */
    public static double calcRatePerSec(double change, double timeInMSec) {
        if (timeInMSec == 0) {
            return change * 1000.0 / 0.001; // do not allow zero
        } else {
            return change * 1000.0 / timeInMSec;
        }
    }

    /**
     * array version of calcRatePerSecArray
     */
    public static double[] calcRatePerSecArray(double change, double[] timeInMSec) {
        double[] result = new double[timeInMSec.length];
        change *= 1000.0;
        for (int i = 0; i < timeInMSec.length; i++) {
            if (timeInMSec[i] == 0) {
                result[i] = change / 0.001;
            } else {
                result[i] = change / timeInMSec[i];
            }
        }
        return result;
    }

    /**
     * copy array from src to dst with given offset in dst.
     * dst should be big enough to hold src
     */
    public static void copyArray(double[] src, double[] dst, int dstOffset) {
        for (int i = 0; i < src.length; i++) {
            dst[dstOffset + i] = src[i];
        }
    }

    /**
     * get classname#methodname from call stack of the current thread
     */
    public static String getClassMethodNames() {
        return getClassMethodNames(mDepth, false);
    }

    private static String getClassMethodNames(int depth, boolean addLineNumber) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String names = elements[depth].getClassName() + "#" + elements[depth].getMethodName() +
                (addLineNumber ? ":" + elements[depth].getLineNumber() : "");
        return names;
    }

    /**
     * to be overridden by child to print message to be passed
     */
    protected void printLog(String msg) {

    }
}
