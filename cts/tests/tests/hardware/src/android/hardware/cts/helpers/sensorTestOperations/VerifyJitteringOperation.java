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
 * Test Operation class that validates the sampling rate jittering of a given sensor.
 *
 * Remarks:
 * - In order to guarantee proper results in any environment, the maximum sampling rate supported by
 *   the Sensor is used, this guarantees the frequency reference for the test.
 */
public class VerifyJitteringOperation extends SensorTestOperation {
    protected SensorManagerTestVerifier mSensor;
    protected long mExpectedtimestampInNs;
    protected long mThresholdPercentage;
    protected long mThresholdInNs;

    public VerifyJitteringOperation(
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
        mExpectedtimestampInNs = TimeUnit.NANOSECONDS.convert(
                maxSamplingRateInUs,
                TimeUnit.MICROSECONDS);
        mThresholdPercentage = thresholdPercentageOfNs;
        mThresholdInNs = mExpectedtimestampInNs / mThresholdPercentage;
    }

    @Override
    public void doWork() {
        TestSensorEvent events[] = mSensor.collectEvents(100);
        ArrayList<Double> jitterValues = new ArrayList<Double>();
        double jitterMean = SensorCtsHelper.getJitterMean(events, jitterValues);
        double percentile95InNs = SensorCtsHelper.get95PercentileValue(jitterValues);

        if(percentile95InNs > mThresholdInNs) {
            for(double jitter : jitterValues) {
                Log.e(LOG_TAG, "Jitter: " + jitter);
            }
            double actualPercentValue = (percentile95InNs * 100) / jitterMean;
            String message = SensorCtsHelper.formatAssertionMessage(
                    "Jitter(95%%ile)",
                    this,
                    mSensor.getUnderlyingSensor(),
                    "expected:%dns(%d%%), actual:%fns(%.2f%%)",
                    mThresholdInNs,
                    mThresholdPercentage,
                    percentile95InNs,
                    actualPercentValue);
            Assert.fail(message);
        }
    }
}
