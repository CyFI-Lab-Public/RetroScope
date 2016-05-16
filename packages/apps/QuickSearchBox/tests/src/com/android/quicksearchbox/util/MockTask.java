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

import static junit.framework.Assert.assertEquals;

/**
 * A task that knows how many times it has been run.
 */
public class MockTask implements NamedTask {
    private final String mName;
    private final int mId;
    private int mRunCount = 0;

    public MockTask(String name, int id) {
        mName = name;
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public synchronized void run() {
        mRunCount++;
        notifyAll();
    }

    public synchronized int getRunCount() {
        return mRunCount;
    }

    public synchronized void waitForCompletion() throws InterruptedException {
        while (mRunCount == 0) {
            wait();
        }
    }

    @Override
    public String toString() {
        return mName + mId;
    }

    public void assertRunCount(String message, int count) {
        assertEquals(message + ": " + toString() + " bad run count", count, getRunCount());
    }

    public void assertRanNever(String message) {
        assertRunCount(message, 0);
    }

    public void assertRanOnce(String message) {
        assertRunCount(message, 1);
    }

    public static void assertRanNever(String message, MockTask... tasks) {
        for (MockTask task : tasks) {
            task.assertRanNever(message);
        }
    }

    public static void assertRanOnce(String message, MockTask... tasks) {
        for (MockTask task : tasks) {
            task.assertRanOnce(message);
        }
    }

}
