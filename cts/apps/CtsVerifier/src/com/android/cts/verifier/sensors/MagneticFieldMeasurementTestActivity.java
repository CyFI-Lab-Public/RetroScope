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

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.cts.helpers.SensorManagerTestVerifier;
import android.hardware.cts.helpers.sensorTestOperations.VerifyNormOperation;
import android.hardware.cts.helpers.sensorTestOperations.VerifyStandardDeviationOperation;

/**
 * Semi-automated test that focuses characteristics associated with Accelerometer measurements.
 * These test cases require calibration of the sensor before performing the verifications.
 * Also, it is recommended to execute these tests outdoors, or at least far from magnetic
 * disturbances.
 */
public class MagneticFieldMeasurementTestActivity extends BaseSensorSemiAutomatedTestActivity {
    @Override
    protected void onRun() throws Throwable {
        calibrateMagnetometer();

        appendText("Verifying the Norm...");
        verifyNorm();

        appendText("\nVerifying the Standard Deviation...");
        verifyStandardDeviation();
    }

    private void calibrateMagnetometer() {
        SensorManagerTestVerifier magnetometer = new SensorManagerTestVerifier(
                this.getApplicationContext(),
                Sensor.TYPE_MAGNETIC_FIELD,
                SensorManager.SENSOR_DELAY_NORMAL,
                0 /*reportLatencyInUs*/) {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float values[] = event.values;
                clearText();
                appendText(
                        "Please calibrate the Magnetometer by moving it in 8 shapes in different " +
                                "orientations.");
                appendText(
                        String.format("->  (%.2f, %.2f, %.2f) uT", values[0], values[1], values[2]),
                        Color.GRAY);
                appendText("Then leave the device in a flat surface and press Next...\n");
            }
        };
        magnetometer.registerListener();
        waitForUser();
        magnetometer.unregisterListener();
    }

    /**
     * This test verifies that the Norm of the sensor data is close to the expected reference value.
     * The units of the reference value are dependent on the type of sensor.
     * This test is used to verify that the data reported by the sensor is close to the expected
     * range and scale.
     *
     * The test takes a sample from the sensor under test and calculates the Euclidean Norm of the
     * vector represented by the sampled data. It then compares it against the test expectations
     * that are represented by a reference value and a threshold.
     *
     * The test is susceptible to errors when the Sensor under test is uncalibrated, or the units in
     * which the data are reported and the expectations are set are different.
     *
     * The assertion associated with the test provides the required data needed to identify any
     * possible issue. It provides:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the values representing the expectation of the test
     * - the values sampled from the sensor
     */
    private void verifyNorm() throws Throwable {
        float expectedMagneticFieldEarth =
                (SensorManager.MAGNETIC_FIELD_EARTH_MAX + SensorManager.MAGNETIC_FIELD_EARTH_MIN) / 2;
        float magneticFieldEarthThreshold =
                expectedMagneticFieldEarth - SensorManager.MAGNETIC_FIELD_EARTH_MIN;
        VerifyNormOperation verifyNorm = new VerifyNormOperation(
                this.getApplicationContext(),
                Sensor.TYPE_MAGNETIC_FIELD,
                SensorManager.SENSOR_DELAY_FASTEST,
                expectedMagneticFieldEarth,
                magneticFieldEarthThreshold);
        verifyNorm.execute();
        logSuccess();
    }

    /**
     * This test verifies that the standard deviation of a set of sampled data from a particular
     * sensor falls into the expectations defined in the CDD. The verification applies to each axis
     * of the sampled data reported by the Sensor under test.
     * This test is used to validate the requirement imposed by the CDD to Sensors in Android. And
     * characterizes how the Sensor behaves while static.
     *
     * The test takes a set of samples from the sensor under test, and calculates the Standard
     * Deviation for each of the axes the Sensor reports data for. The StdDev is compared against
     * the expected value documented in the CDD.
     *
     * The test is susceptible to errors if the device is moving while the test is running, or if
     * the Sensor's sampled data indeed falls into a large StdDev.
     *
     * The assertion associated with the test provides the required data to identify any possible
     * issue. It provides:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the expectation of the test
     * - the std dev calculated and the axis it applies to
     * Additionally, the device's debug output (adb logcat) dumps the set of values associated with
     * the failure to help track down the issue.
     */
    private void verifyStandardDeviation() throws Throwable {
        VerifyStandardDeviationOperation verifyStdDev = new VerifyStandardDeviationOperation(
                this.getApplicationContext(),
                Sensor.TYPE_MAGNETIC_FIELD,
                SensorManager.SENSOR_DELAY_FASTEST,
                0 /*reportLatencyInUs*/,
                2f /* uT */);
        verifyStdDev.execute();
        logSuccess();
    }
}
