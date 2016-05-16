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
import java.util.concurrent.Executor;

/**
 * A simple executor that maintains a queue and executes one task synchronously every
 * time {@link #runNext()} is called. This gives us predictable scheduling for the tests to
 * avoid timeouts waiting for threads to finish.
 */
public class MockExecutor implements Executor {

    private final LinkedList<Runnable> mQueue = new LinkedList<Runnable>();

    private boolean mClosed = false;

    public void execute(Runnable task) {
        if (mClosed) throw new IllegalStateException("closed");
        mQueue.addLast(task);
    }

    public void cancelPendingTasks() {
        mQueue.clear();
    }

    public void close() {
        cancelPendingTasks();
        mClosed = true;
    }

    public int countPendingTasks() {
        return mQueue.size();
    }

    public boolean runNext() {
        if (mQueue.isEmpty()) {
            return false;
        }
        Runnable command = mQueue.removeFirst();
        command.run();
        return true;
    }

}
