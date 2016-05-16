/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.hardware.cts.helpers.sensorTestOperations;

import junit.framework.Assert;

import android.content.Context;

import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorManagerTestVerifier;
import android.hardware.cts.helpers.SensorTestInformation;
import android.hardware.cts.helpers.SensorTestOperation;
import android.hardware.cts.helpers.TestSensorEvent;

import android.util.Log;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import java.util.concurrent.TimeUnit;

/**
 * Test Operation class that validates the max sampling rate of a given sensor.
 *
 * Remarks:
 * - In order to guarantee proper results in any environment, the maximum sampling rate supported by
 *   the Sensor is used, this guarantees the frequency reference for the test.
 */
public class VerifyMaximumFrequencyOperation extends SensorTestOperation {
    protected SensorManagerTestVerifier mSensor;
    protected long mExpectedTimestampInNs;
    protected long mThresholdPercentage;
    protected long mThresholdInNs;

    public VerifyMaximumFrequencyOperation(
            Context context,
            int sensorType,
            int reportLatencyInUs,
            int thresholdPercentageOfNs) throws InvalidParameterException {
        if(thresholdPercentageOfNs < 0) {
            throw new InvalidParameterException("thresholdPercentageOfNs needs to be >= 0");
        }
        // use the max sampling frequency the sensor reports to guarantee the results
        int maxSamplingRateInUs = SensorTestInformation.getMaxSamplingRateInUs(context, sensorType);
        mSensor = new SensorManagerTestVerifier(
                context,
                sensorType,
                maxSamplingRateInUs,
                reportLatencyInUs);
        // set expectations
        mExpectedTimestampInNs = TimeUnit.NANOSECONDS.convert(
                maxSamplingRateInUs,
                TimeUnit.MICROSECONDS);
        mThresholdPercentage = thresholdPercentageOfNs;
        mThresholdInNs = mExpectedTimestampInNs / mThresholdPercentage;
    }

    @Override
    public void doWork() {
        TestSensorEvent events[] = mSensor.collectEvents(100);
        ArrayList<Long> timestampDelayValues = new ArrayList<Long>();
        Double frequencyMeanInUs = SensorCtsHelper.getAverageTimestampDelayWithValues(
                events,
                timestampDelayValues);

        if(Math.abs(mExpectedTimestampInNs - frequencyMeanInUs) > mThresholdInNs) {
            for(long value : timestampDelayValues) {
                Log.e(LOG_TAG, "TimestampDelay: " + value);
            }
            String message = SensorCtsHelper.formatAssertionMessage(
                    "Frequency",
                    this,
                    mSensor.getUnderlyingSensor(),
                    "expected:%dns(%.2fHz), actual:%fns(%.2fHz), threshold:%dns(%d%%)",
                    mExpectedTimestampInNs,
                    SensorCtsHelper.getFrequencyInHz(mExpectedTimestampInNs),
                    frequencyMeanInUs,
                    SensorCtsHelper.getFrequencyInHz(frequencyMeanInUs),
                    mThresholdInNs,
                    mThresholdPercentage);
            Assert.fail(message);
        }
    }
}
