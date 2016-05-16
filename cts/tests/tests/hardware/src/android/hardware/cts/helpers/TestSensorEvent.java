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

package android.hardware.cts.helpers;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

/**
 * Test class to wrap SensorEvent.
 * It currently only provides a way to clone SensorEvent data, but in the future it can contain
 * verifications and test checks.
 */
public class TestSensorEvent {
    public final Sensor sensor;
    public final long timestamp;
    public final int accuracy;
    public final float values[];

    public TestSensorEvent(SensorEvent event) {
        values = new float[event.values.length];
        System.arraycopy(event.values, 0, values, 0, event.values.length);

        sensor = event.sensor;
        timestamp = event.timestamp;
        accuracy = event.accuracy;
    }
}
