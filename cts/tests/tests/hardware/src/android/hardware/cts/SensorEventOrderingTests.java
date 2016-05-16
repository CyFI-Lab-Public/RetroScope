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

package android.hardware.cts;

import junit.framework.Test;
import junit.framework.TestSuite;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import android.hardware.cts.helpers.SensorTestCase;

import android.hardware.cts.helpers.sensorTestOperations.VerifyEventOrderingOperation;

/**
 * Verifies the proper ordering in time of sensor events.
 */
public class SensorEventOrderingTests extends SensorTestCase {
    /**
     * Builder for the test suite.
     * This is the method that will build dynamically the set of test cases to execute.
     * Each 'base' test case is composed by three parts:
     * - the matrix definition
     * - the test method that will execute the test case
     * - a static method that will combine both and add test case instances to the test suite
     */
    public static Test suite() {
        TestSuite testSuite = new TestSuite();

        // add test generation routines
        createEventOrderingTestCases(testSuite);

        return testSuite;
    }

    /**
     * Event ordering test cases.
     */
    private int mSensorType;
    private int mSamplingRateInUs;
    private int mReportLatencyInUs;

    private static void createEventOrderingTestCases(TestSuite testSuite) {
        int testDefinitionMatrix[][] = {
                // { SensorType, SamplingRateInUs, ReportLatencyInUs },
                { Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_MAGNETIC_FIELD, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_LINEAR_ACCELERATION, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_RELATIVE_HUMIDITY, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_AMBIENT_TEMPERATURE, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_GAME_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_GYROSCOPE_UNCALIBRATED, SensorManager.SENSOR_DELAY_FASTEST, 0 },
                { Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_FASTEST,0 },
        };

        for(int definition[] : testDefinitionMatrix) {
            SensorEventOrderingTests test = new SensorEventOrderingTests();
            test.mSensorType = definition[0];
            test.mSamplingRateInUs = definition[1];
            test.mReportLatencyInUs = definition[2];
            test.setName("testEventOrdering");
            testSuite.addTest(test);
        }
    }

    /**
     * This test verifies the ordering of the sampled data reported by the Sensor under test.
     * This test is used to guarantee that sensor data is reported in the order it occurs, and that
     * events are always reported in order.
     *
     * The test takes a set of samples from the Sensor under test, and then it verifies that each
     * event's timestamp is in the future compared with the previous event. At the end of the
     * validation, the full set of events is verified to be ordered by timestamp as they are
     * generated.
     *
     * The test can be susceptible to errors if the sensor sampled data is not timestamped at the
     * Hardware level. Or events sampled at high rates are added to the FIFO without controlling the
     * appropriate ordering of the events.
     *
     * The assertion associated with the test provides the information of the two consecutive events
     * that cause the test to fail.
     */
    public void testEventOrdering() throws Throwable {
        VerifyEventOrderingOperation operation = new VerifyEventOrderingOperation(
                this.getContext(),
                mSensorType,
                mSamplingRateInUs,
                mReportLatencyInUs);
        operation.execute();
    }
}
