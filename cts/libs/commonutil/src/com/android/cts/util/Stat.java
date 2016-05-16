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

import java.util.Arrays;

/**
 * Utilities for doing statistics
 *
 */
public class Stat {

    /**
     * Collection of statistical propertirs like average, max, min, and stddev
     */
    public static class StatResult {
        public double mAverage;
        public double mMin;
        public double mMax;
        public double mStddev;
        public int mDataCount;
        public StatResult(double average, double min, double max, double stddev, int dataCount) {
            mAverage = average;
            mMin = min;
            mMax = max;
            mStddev = stddev;
            mDataCount = dataCount;
        }
    }

    /**
     * Calculate statistics properties likes average, min, max, and stddev for the given array
     */
    public static StatResult getStat(double[] data) {
        double average = data[0];
        double min = data[0];
        double max = data[0];
        double eX2 = data[0] * data[0]; // will become E[X^2]
        for (int i = 1; i < data.length; i++) {
            average += data[i];
            eX2 += data[i] * data[i];
            if (data[i] > max) {
                max = data[i];
            }
            if (data[i] < min) {
                min = data[i];
            }
        }
        average /= data.length;
        eX2 /= data.length;
        // stddev = sqrt(E[X^2] - (E[X])^2)
        double stddev = Math.sqrt(eX2 - average * average);
        return new StatResult(average, min, max, stddev, data.length);
    }

    /**
     * Calculate statistics properties likes average, min, max, and stddev for the given array
     * while rejecting outlier +/- median * rejectionThreshold.
     * rejectionThreshold should be bigger than 0.0 and be lowerthan 1.0
     */
    public static StatResult getStatWithOutlierRejection(double[] data, double rejectionThreshold) {
        double[] dataCopied = Arrays.copyOf(data, data.length);
        Arrays.sort(dataCopied);
        int medianIndex = dataCopied.length / 2;
        double median;
        if (dataCopied.length % 2 == 1) {
            median = dataCopied[medianIndex];
        } else {
            median = (dataCopied[medianIndex - 1] + dataCopied[medianIndex]) / 2.0;
        }
        double thresholdMin = median * (1.0 - rejectionThreshold);
        double thresholdMax = median * (1.0 + rejectionThreshold);

        double average = 0.0;
        double min = median;
        double max = median;
        double eX2 = 0.0; // will become E[X^2]
        int validDataCounter = 0;
        for (int i = 0; i < data.length; i++) {
            if ((data[i] > thresholdMin) && (data[i] < thresholdMax)) {
                validDataCounter++;
                average += data[i];
                eX2 += data[i] * data[i];
                if (data[i] > max) {
                    max = data[i];
                }
                if (data[i] < min) {
                    min = data[i];
                }
            }
            //TODO report rejected data
        }
        double stddev;
        if (validDataCounter > 0) {
            average /= validDataCounter;
            eX2 /= validDataCounter;
            // stddev = sqrt(E[X^2] - (E[X])^2)
            stddev = Math.sqrt(eX2 - average * average);
        } else { // both median is showing too much diff
            average = median;
            stddev = 0; // don't care
        }

        return new StatResult(average, min, max, stddev, validDataCounter);
    }

    /**
     * return the average value of the passed array
     */
    public static double getAverage(double[] data) {
        double sum = data[0];
        for (int i = 1; i < data.length; i++) {
            sum += data[i];
        }
        return sum / data.length;
    }

    /**
     * return the minimum value of the passed array
     */
    public static double getMin(double[] data) {
        double min = data[0];
        for (int i = 1; i < data.length; i++) {
            if (data[i] < min) {
                min = data[i];
            }
        }
        return min;
    }

    /**
     * return the maximum value of the passed array
     */
    public static double getMax(double[] data) {
        double max = data[0];
        for (int i = 1; i < data.length; i++) {
            if (data[i] > max) {
                max = data[i];
            }
        }
        return max;
    }
}
