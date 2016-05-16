/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.quicksearchbox.util;

import java.util.LinkedList;

/**
 * Creates {@link MockNamedTaskExecutor} instances.
 */
public class MockNamedTaskExecutorFactory implements Factory<NamedTaskExecutor> {

    private final LinkedList<MockNamedTaskExecutor> mExecutors
            = new LinkedList<MockNamedTaskExecutor>();

    /**
     * Runs one task in each executor.
     */
    public void runNext() {
        for (MockNamedTaskExecutor executor : mExecutors) {
            executor.runNext();
        }
    }

    public NamedTaskExecutor create() {
        MockNamedTaskExecutor executor = new MockNamedTaskExecutor();
        mExecutors.add(executor);
        return executor;
    }

}
