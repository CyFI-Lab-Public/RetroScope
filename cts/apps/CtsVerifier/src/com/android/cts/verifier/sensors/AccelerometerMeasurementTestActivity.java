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

package com.android.cts.verifier.sensors;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.sensorTestOperations.VerifyMeasurementsOperation;

import java.util.concurrent.TimeUnit;

/**
 * Semi-automated test that focuses on characteristics associated with Accelerometer measurements.
 */
public class AccelerometerMeasurementTestActivity extends BaseSensorSemiAutomatedTestActivity {
    @Override
    protected void onRun() throws Throwable {
        verifyMeasurements(
                "Place the device in a flat surface with the screen facing the ceiling",
                0, 0, SensorManager.STANDARD_GRAVITY);

        delayedVerifyMeasurements(
                "Press 'Next' and place the device in a flat surface with the screen facing it",
                0, 0, -SensorManager.STANDARD_GRAVITY);

        verifyMeasurements(
                "Place the device in a flat surface resting vertically on its right side",
                -SensorManager.STANDARD_GRAVITY, 0, 0);

        verifyMeasurements(
                "Place the device in a flat surface resting vertically on its left side",
                SensorManager.STANDARD_GRAVITY, 0, 0);

        verifyMeasurements(
                "Place the device in a flat surface resting vertically on its top side",
                0, -SensorManager.STANDARD_GRAVITY, 0);

        verifyMeasurements(
                "Place the device in a flat surface resting vertically on its bottom side",
                0, SensorManager.STANDARD_GRAVITY, 0);
    }

    /**
     * This test verifies that the Accelerometer measurements are close to the expected reference
     * values (range and scale).
     *
     * The test takes a set of samples from the sensor under test and calculates the mean of each
     * axis that the sensor data collects. It then compares it against the test expectations that
     * are represented by reference values and a threshold.

     * The reference values are coupled to the orientation of the device. The test is susceptible to
     * errors when the device is not oriented properly, or the units in which the data are reported
     * and the expectations are set are different.
     *
     * The error message associated with the test provides the required data needed to identify any
     * possible issue. It provides:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the values representing the expectation of the test
     * - the mean of values sampled from the sensor
     */
    private void verifyMeasurements(double ... expectations) throws Throwable {
        Thread.sleep(500 /*ms*/);
        VerifyMeasurementsOperation verifyMeasurements = new VerifyMeasurementsOperation(
                getApplicationContext(),
                Sensor.TYPE_ACCELEROMETER,
                SensorManager.SENSOR_DELAY_FASTEST,
                0 /*reportLatencyInUs*/,
                expectations,
                1.95f /* m / s^2 */);
        verifyMeasurements.execute();
        logSuccess();
    }

    private void delayedVerifyMeasurements(
            String message,
            double ... expectations) throws Throwable {
        appendText(String.format("\n%s.", message));
        appendText("A sound will be played once the verification is complete...");
        waitForUser();
        Thread.sleep(TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS));

        try {
            verifyMeasurements(expectations);
        } finally {
            playSound();
        }
    }

    private void verifyMeasurements(String message, double ... expectations) throws Throwable {
        appendText(String.format("\n%s.", message));
        appendText("Press 'Next' when ready and keep the device steady.");
        waitForUser();

        verifyMeasurements(expectations);
    }
}
