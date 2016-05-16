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

import android.hardware.cts.helpers.sensorTestOperations.VerifyNormOperation;
import android.hardware.cts.helpers.sensorTestOperations.VerifyStandardDeviationOperation;

/**
 * Verifies several properties of the sensor measurements.
 */
public class SensorMeasurementTests extends SensorTestCase {
    private int mSensorType;
    private int mSamplingRateInUs;

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
        createEventNormTestCases(testSuite);
        createStandardDeviationTestCases(testSuite);

        return testSuite;
    }

    /**
     * SensorEvent Norm test cases.
     *
     * Regress:
     * - b/9503957
     * - b/9611609
     */
    private float mReferenceValue;
    private float mThreshold;

    private static void createEventNormTestCases(TestSuite testSuite) {
        Object testDefinitionMatrix[][] = {
                // { SensorType, SamplingRateInUs, ReferenceValue, Threshold },
                { Sensor.TYPE_ACCELEROMETER,
                        SensorManager.SENSOR_DELAY_FASTEST,
                        SensorManager.STANDARD_GRAVITY,
                        1.5f /* m / s^2 */},
                { Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_FASTEST, 0.0f, 2.5f /* dps */ },
        };

        for(Object definition[] : testDefinitionMatrix)  {
            SensorMeasurementTests test = new SensorMeasurementTests();
            test.mSensorType = (Integer)definition[0];
            test.mSamplingRateInUs = (Integer)definition[1];
            test.mReferenceValue = (Float)definition[2];
            test.mThreshold = (Float)definition[3];
            test.setName("testEventNorm");
            testSuite.addTest(test);
        }
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
    public void testEventNorm() throws Throwable {
        VerifyNormOperation operation = new VerifyNormOperation(
                this.getContext(),
                mSensorType,
                mSamplingRateInUs,
                mReferenceValue,
                mThreshold);
        operation.execute();
    }

    /**
     * SensorEvent Standard Deviation test cases.
     */
    private int mReportLatencyInUs;
    private float mExpectedStandardDeviation;

    private static void createStandardDeviationTestCases(TestSuite testSuite) {
        Object testDefinitionMatrix[][] = {
                // { SensorType, SamplingRateInUs, ReportLatencyInUs, ExpectedStandardDeviation },
                { Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST, 0, 1f /* m/s^2 */ },
                { Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_FASTEST, 0, 0.5f /* dps */ },
        };

        for(Object definition[] : testDefinitionMatrix) {
            SensorMeasurementTests test = new SensorMeasurementTests();
            test.mSensorType = (Integer)definition[0];
            test.mSamplingRateInUs = (Integer)definition[1];
            test.mReportLatencyInUs = (Integer)definition[2];
            test.mExpectedStandardDeviation = (Float)definition[3];
            test.setName("testStandardDeviation");
            testSuite.addTest(test);
        }
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
    public void testStandardDeviation() throws Throwable {
        VerifyStandardDeviationOperation operation = new VerifyStandardDeviationOperation(
                this.getContext(),
                mSensorType,
                mSamplingRateInUs,
                mReportLatencyInUs,
                mExpectedStandardDeviation);
        operation.execute();
    }
}
