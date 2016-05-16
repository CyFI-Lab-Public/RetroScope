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

import android.hardware.cts.helpers.SensorCtsHelper;
import android.hardware.cts.helpers.SensorTestCase;

import android.hardware.cts.helpers.sensorTestOperations.VerifyJitteringOperation;
import android.hardware.cts.helpers.sensorTestOperations.VerifyMaximumFrequencyOperation;

/**
 * Verifies several properties of the sampling rate of the different sensors in the platform.
 */
public class SensorFrequencyTests extends SensorTestCase {
    private int mSensorType;
    private int mReportLatencyInUs;
    private int mThresholdPercentageOfNs;

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
        createMaxFrequencyExpectedTestCases(testSuite);
        // TODO: tests are a unreliable in the lab
        //createMaxFrequencyTestCases(testSuite);
        //createJitteringTestCases(testSuite);

        return testSuite;
    }

    /**
     * Max frequency test cases.
     */
    private static void createMaxFrequencyTestCases(TestSuite testSuite) {
        int testDefinitionMatrix[][] = {
                // { SensorType, ReportLatencyInUs, ThresholdPercentageOfNs },
                { Sensor.TYPE_ACCELEROMETER, 0, 10 },
                { Sensor.TYPE_GYROSCOPE, 0, 10 },
                { Sensor.TYPE_MAGNETIC_FIELD, 0, 10 },
        };

        for(int definition[] : testDefinitionMatrix) {
            SensorFrequencyTests test = new SensorFrequencyTests();
            test.mSensorType = definition[0];
            test.mReportLatencyInUs = definition[1];
            test.mThresholdPercentageOfNs = definition[2];
            test.setName("testMaxFrequency");
            testSuite.addTest(test);
        }
    }

    /**
     * This test verifies that the Sensor under test can sample and report data at the Maximum
     * frequency (sampling rate) it advertises.
     *
     * The test takes a set of samples from the sensor under test, and calculates the mean of the
     * frequency at which the events are reported. The frequency between events is calculated by
     * looking at the delta between the timestamps associated with each event.
     *
     * The test is susceptible to errors if the Sensor is not capable to sample data at the maximum
     * rate it supports, or the sensor events are not timestamped at the Hardware level.
     *
     * The assertion associated with the test provides the required data to identify:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the expected frequency
     * - the observed frequency
     * In addition to that, the device's debug output (adb logcat) dumps the set of timestamp deltas
     * associated with the set of data gathered from the Sensor under test.
     */
    public void testMaxFrequency() throws Throwable {
        VerifyMaximumFrequencyOperation operation = new VerifyMaximumFrequencyOperation(
                this.getContext(),
                mSensorType,
                mReportLatencyInUs,
                mThresholdPercentageOfNs);
        operation.execute();
    }

    /**
     * Jittering test cases.
     */
    private static void createJitteringTestCases(TestSuite testSuite) {
        int testDefinitionMatrix[][] = {
                // { SensorType, ReportLatencyInUs, ThresholdPercentageOfNs },
                { Sensor.TYPE_ACCELEROMETER, 0, 10 },
                { Sensor.TYPE_GYROSCOPE, 0, 10 },
                { Sensor.TYPE_MAGNETIC_FIELD, 0, 10 },
        };

        for(int definition[] : testDefinitionMatrix) {
            SensorFrequencyTests test = new SensorFrequencyTests();
            test.mSensorType = definition[0];
            test.mReportLatencyInUs = definition[1];
            test.mThresholdPercentageOfNs = definition[2];
            test.setName("testJittering");
            testSuite.addTest(test);
        }
    }

    /**
     * This test verifies that the event jittering associated with the sampled data reported by the
     * Sensor under test, aligns with the requirements imposed in the CDD.
     * This test characterizes how the sensor behaves while sampling data at a specific rate.
     *
     * The test takes a set of samples from the sensor under test, using the maximum sampling rate
     * advertised by the Sensor under test. It then compares the 95%ile associated with the
     * jittering of the timestamps, with an expected value.
     *
     * The test is susceptible to errors if the sensor events are not timestamped at the Hardware
     * level.
     *
     * The assertion associated with the failure provides the following information:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the expectation of the test with respect of the 95%ile
     * - the calculated 95%ile jittering
     * Additionally, the device's debug output (adb logcat) dumps the set of jitter values
     * calculated.
     */
    public void testJittering() throws Throwable {
        VerifyJitteringOperation operation = new VerifyJitteringOperation(
                this.getContext(),
                mSensorType,
                mReportLatencyInUs,
                mThresholdPercentageOfNs);
        operation.execute();
    }

    /**
     * Max Frequency expected Test Cases.
     */
    private int mExpectedSamplingRateInUs;

    private static void createMaxFrequencyExpectedTestCases(TestSuite testSuite) {
        int testDefinitionMatrix[][] = {
                // { SensorType, ExpectedSamplingRateInUs },
                { Sensor.TYPE_ACCELEROMETER, 10000 /* 100 Hz */ },
                { Sensor.TYPE_GYROSCOPE, 10000 /* 100 Hz */ },
                { Sensor.TYPE_MAGNETIC_FIELD, 100000 /* 10 Hz */ },
        };

        for(int definition[] : testDefinitionMatrix) {
            SensorFrequencyTests test = new SensorFrequencyTests();
            test.mSensorType = definition[0];
            test.mExpectedSamplingRateInUs = definition[1];
            test.setName("testMaxFrequencyExpected");
            testSuite.addTest(test);
        }
    }

    /**
     * This test verifies that the sensor's maximum advertised frequency (sampling rate) complies
     * with the required frequency set in the CDD.
     * This characterizes that the sensor is able to provide data at the rate the platform requires
     * it.
     *
     * The test simply compares the sampling rate specified in the CDD with the maximum sampling
     * rate advertised by the Sensor under test.
     *
     * The test can fail if the Sensor Hardware does not support the sampling rate required by the
     * platform.
     *
     * The assertion associated with the test failure contains:
     * - the thread id on which the failure occurred
     * - the sensor type and sensor handle that caused the failure
     * - the expected maximum sampling rate
     * - the observed maximum sampling rate
     */
    public void testMaxFrequencyExpected() {
        Sensor sensor = SensorCtsHelper.getSensor(this.getContext(), mSensorType);
        int samplingRateInUs = sensor.getMinDelay();
        String message = String.format(
                "samplingRateInUs| expected:%d, actual:%d",
                mExpectedSamplingRateInUs,
                samplingRateInUs);
        assertTrue(message, mExpectedSamplingRateInUs >= samplingRateInUs);
    }
}
