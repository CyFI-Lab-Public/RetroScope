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

import java.util.ArrayList;

/**
 * A test operation that groups a set of SensorTestOperations and allows to execute them all in
 * parallel.
 * This class can be combined to compose other primitive SensorTestOperations.
 */
public class ParallelCompositeSensorTestOperation extends SensorTestOperation {
    private final ArrayList<SensorTestOperation> mOperations = new ArrayList<SensorTestOperation>();

    /**
     * There is no synchronization
     * @param operations
     */
    public void add(SensorTestOperation ... operations) {
        synchronized (mOperations) {
            for(SensorTestOperation operation : operations) {
                mOperations.add(operation);
            }
        }
    }

    @Override
    protected void doWork() throws Throwable {
        synchronized (mOperations) {
            for(SensorTestOperation operation : mOperations) {
                operation.start();
            }
            for(SensorTestOperation operation : mOperations) {
                operation.waitForCompletion();
            }
        }
    }
}
