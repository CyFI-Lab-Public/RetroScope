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
import android.hardware.cts.helpers.SensorTestOperation;
import android.hardware.cts.helpers.TestSensorEvent;

/**
 * Test Operation class that validates the ordering of sensor events.
 */
public class VerifyEventOrderingOperation extends SensorTestOperation {
    private SensorManagerTestVerifier mSensor;

    public VerifyEventOrderingOperation(
            Context context,
            int sensorType,
            int samplingRateInUs,
            int reportLatencyInUs) {
        mSensor = new SensorManagerTestVerifier(
                context,
                sensorType,
                samplingRateInUs,
                reportLatencyInUs);
    }

    @Override
    public void doWork() {
        TestSensorEvent events[] = mSensor.collectEvents(100);
        for(int i = 1; i < events.length; ++i) {
            long previousTimestamp = events[i-1].timestamp;
            long timestamp = events[i].timestamp;
            // allow two identical timestamps to be considered in order, in case the resolution of
            // the timestamp is not granular enough
            if(previousTimestamp > timestamp) {
                String message = SensorCtsHelper.formatAssertionMessage(
                        "Ordering",
                        this,
                        mSensor.getUnderlyingSensor(),
                        "position:%d, previous:%d, timestamp:%d",
                        i,
                        previousTimestamp,
                        timestamp);
                Assert.fail(message);
            }
        }
    }
}
