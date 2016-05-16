/*
 * Copyright The Android Open Source Project
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

import java.security.InvalidParameterException;

/**
 * Test Operation class that validates the measurements of a a given sensor.
 * The operation relies on the number of axes each sensor type reports.
 * The verification calculates the mean for each axis on the measurements, and verifies that they
 * fall into the expected intervals.
 */
public class VerifyMeasurementsOperation extends SensorTestOperation {
    private final SensorManagerTestVerifier mSensor;
    private final int mAxisCount;
    private final double mReferenceValues[];
    private final double mThreshold;

    public VerifyMeasurementsOperation(
            Context context,
            int sensorType,
            int samplingRateInUs,
            int reportLatencyInUs,
            double referenceValues[],
            float threshold) {
        mAxisCount = SensorTestInformation.getAxisCount(sensorType);
        if(mAxisCount != referenceValues.length) {
            throw new InvalidParameterException(
                    String.format("%d reference values are expected.", mAxisCount));
        }
        mSensor = new SensorManagerTestVerifier(
                context,
                sensorType,
                samplingRateInUs,
                reportLatencyInUs);
        // set expectations
        mReferenceValues = referenceValues;
        mThreshold = threshold;
    }

    @Override
    public void doWork() {
        final String VALUE_SEPARATOR = ", ";
        TestSensorEvent events[] = mSensor.collectEvents(100);
        double measuredValues[] = new double[mReferenceValues.length];
        SensorCtsHelper.getMeans(events, measuredValues);

        boolean success = true;
        StringBuilder referenceValuesBuilder = new StringBuilder();
        StringBuilder measuredValuesBuilder = new StringBuilder();
        for(int i = 0; i < mReferenceValues.length; i++) {
            double reference = mReferenceValues[i];
            double measurement = measuredValues[i];
            double delta = Math.abs(reference - measurement);
            success &= (delta <= mThreshold);
            referenceValuesBuilder.append(reference);
            referenceValuesBuilder.append(VALUE_SEPARATOR);
            measuredValuesBuilder.append(measurement);
            measuredValuesBuilder.append(VALUE_SEPARATOR);
        }
        if(!success) {
            String message = SensorCtsHelper.formatAssertionMessage(
                    "Measurement",
                    this,
                    mSensor.getUnderlyingSensor(),
                    "expected:( %s), threshold:%f, actual: ( %s)",
                    referenceValuesBuilder.toString(),
                    mThreshold,
                    measuredValuesBuilder.toString());
            Assert.fail(message);
        }
    }
}
