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

import junit.framework.Assert;


/**
 * A simple executor that maintains a queue and executes one task synchronously every
 * time {@link #runNext()} is called. This gives us predictable scheduling for the tests to
 * avoid timeouts waiting for threads to finish.
 */
public class MockNamedTaskExecutor implements NamedTaskExecutor {

    private final MockExecutor mExecutor = new MockExecutor();

    public void execute(NamedTask task) {
        mExecutor.execute(task);
    }

    public void cancelPendingTasks() {
        mExecutor.cancelPendingTasks();
    }

    public void close() {
        mExecutor.close();
    }

    public boolean runNext() {
        return mExecutor.runNext();
    }

    public void assertPendingTaskCount(int expected) {
        Assert.assertEquals("Wrong number of pending tasks",
                expected, mExecutor.countPendingTasks());
    }

    public void assertDone() {
        assertPendingTaskCount(0);
    }
}
