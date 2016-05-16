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
import android.hardware.cts.helpers.sensorTestOperations.VerifySignumOperation;

/**
 * Semi-automated test that focuses on characteristics associated with Accelerometer measurements.
 */
public class GyroscopeMeasurementTestActivity extends BaseSensorSemiAutomatedTestActivity {
    @Override
    protected void onRun() throws Throwable {
        appendText("Place the device in a flat surface with the screen facing the ceiling, make "
                + "sure the device aligns with the orientation specified for each scenario. Then "
                + "follow the instructions for each scenario:");

        verifyMeasurements(
                "leave the device static",
                true /*portrait*/,
                0, 0, 0);

        verifyMeasurements(
                "rotate the device clockwise",
                true /*portrait*/,
                0, 0, -1);

        verifyMeasurements(
                "rotate the device counter-clockwise",
                true /*portrait*/,
                0, 0, +1);

        verifyMeasurements(
                "rotate the device on its right until it stands on its side",
                true /*portrait*/,
                0, +1, 0);

        verifyMeasurements(
                "rotate the device on its left until it stands on its side",
                true /*portrait*/,
                0, -1, 0);

        verifyMeasurements(
                "rotate the device on its top until it stands perpendicular",
                false /*portrait*/,
                -1, 0, 0);

        verifyMeasurements(
                "rotate the device on its bottom until it stands perpendicular",
                false /*portrait*/,
                +1, 0, 0);
    }

    /**
     * This test verifies that the Gyroscope measures angular speeds with the right direction.
     * The test does not measure the range or scale, apart from filtering small readings that
     * deviate from zero.
     *
     * The test takes a set of samples from the sensor under test and calculates the mean of each
     * axis that the sensor data collects. It then compares it against the test expectations that
     * are represented by signed values. It verifies that the readings have the right direction.

     * The reference values are coupled to the orientation of the device. The test is susceptible to
     * errors when the device is not oriented properly, the device has moved to slowly, or it has
     * moved in more than the direction conducted.
     *
     * The error message associated with the test provides the required data needed to identify any
     * possible issue. It provides:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the values representing the expectation of the test
     * - the mean of values sampled from the sensor
     */
    private void verifyMeasurements(
            String scenarioInstructions,
            boolean usePortraitOrientation,
            double ... expectations) throws Throwable {
        final String orientation = usePortraitOrientation ? "Portrait": "Landscape";
        appendText(String.format("\n[Device orientation]: %s", orientation));
        appendText(String.format("Press 'Next' and %s.", scenarioInstructions));
        waitForUser();

        Thread.sleep(500 /*ms*/);
        VerifySignumOperation verifySignum = new VerifySignumOperation(
                getApplicationContext(),
                Sensor.TYPE_GYROSCOPE,
                SensorManager.SENSOR_DELAY_FASTEST,
                expectations,
                0.2 /*noiseThreshold*/);
        verifySignum.execute();
        logSuccess();
    }
}
