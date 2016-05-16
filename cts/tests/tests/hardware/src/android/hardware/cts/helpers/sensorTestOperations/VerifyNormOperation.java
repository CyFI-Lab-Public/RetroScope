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

/**
 * Test Operation class that validates the norm of a given sensor.
 * The operation relies in the number of axes each sensor type reports.
 */
public class VerifyNormOperation extends SensorTestOperation {
    private SensorManagerTestVerifier mSensor;
    private int mAxisCount;
    private double mReferenceValue;
    private double mThreshold;

    public VerifyNormOperation(
            Context context,
            int sensorType,
            int samplingRateInUs,
            float referenceValue,
            float threshold) {
        mSensor = new SensorManagerTestVerifier(
                context,
                sensorType,
                samplingRateInUs,
                0 /*reportLatencyInUs*/);
        // set expectations
        mAxisCount = SensorTestInformation.getAxisCount(mSensor.getUnderlyingSensor().getType());
        mReferenceValue = referenceValue;
        mThreshold = threshold;
    }

    @Override
    public void doWork() {
        TestSensorEvent event = mSensor.collectEvents(1)[0];
        StringBuilder valuesBuilder = new StringBuilder();
        double norm = 0.0;

        for(int i = 0; i < mAxisCount; ++i) {
            float value = event.values[i];
            norm += Math.pow(value, 2);

            valuesBuilder.append(value);
            valuesBuilder.append(", ");
        }
        norm = Math.sqrt(norm);

        String message = SensorCtsHelper.formatAssertionMessage(
                "Norm",
                this,
                mSensor.getUnderlyingSensor(),
                "expected:%f, threshold:%f, actual:%f ( %s)",
                mReferenceValue,
                mThreshold,
                norm,
                valuesBuilder.toString());
        Assert.assertTrue(message, Math.abs(mReferenceValue - norm) <= mThreshold);
    }
}
