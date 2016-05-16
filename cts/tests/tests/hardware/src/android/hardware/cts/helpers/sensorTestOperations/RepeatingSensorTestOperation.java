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

import android.hardware.cts.helpers.SensorTestOperation;

/**
 * High level SensorTestOperation that executes the inner operation in a loop.
 */
public class RepeatingSensorTestOperation extends SensorTestOperation {
    private final SensorTestOperation mSensorTestOperation;
    private final int mRepetitionCount;

    public RepeatingSensorTestOperation(SensorTestOperation operation, int repetitionCount) {
        mSensorTestOperation = operation;
        mRepetitionCount = repetitionCount;
    }

    @Override
    protected void doWork() throws Throwable {
        for(int i = 0; i < mRepetitionCount; ++i) {
            mSensorTestOperation.execute();
        }
    }
}
