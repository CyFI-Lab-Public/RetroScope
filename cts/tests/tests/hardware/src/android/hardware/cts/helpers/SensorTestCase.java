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

import android.test.AndroidTestCase;

import android.util.Log;

/**
 * Test Case class that handles gracefully sensors that are not available in the device.
 */
public abstract class SensorTestCase extends AndroidTestCase {
    protected final String LOG_TAG = "TestRunner";

    protected SensorTestCase() {}

    @Override
    public void runTest() throws Throwable {
        try {
            super.runTest();
        } catch (SensorNotSupportedException e) {
            // the sensor is not supported/available in the device, log a warning and skip the test
            Log.w(LOG_TAG, e.getMessage());
        }
    }
}
