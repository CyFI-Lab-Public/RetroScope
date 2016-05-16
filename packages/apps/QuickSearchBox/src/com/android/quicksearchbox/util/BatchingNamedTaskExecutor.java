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


import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes NamedTasks in batches of a given size.  Tasks are queued until
 * executeNextBatch is called.
 */
public class BatchingNamedTaskExecutor implements NamedTaskExecutor {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.BatchingNamedTaskExecutor";

    private final NamedTaskExecutor mExecutor;

    /** Queue of tasks waiting to be dispatched to mExecutor **/
    private final ArrayList<NamedTask> mQueuedTasks = new ArrayList<NamedTask>();

    /**
     * Creates a new BatchingSourceTaskExecutor.
     *
     * @param executor A SourceTaskExecutor for actually executing the tasks.
     */
    public BatchingNamedTaskExecutor(NamedTaskExecutor executor) {
        mExecutor = executor;
    }

    public void execute(NamedTask task) {
        synchronized (mQueuedTasks) {
            if (DBG) Log.d(TAG, "Queuing " + task);
            mQueuedTasks.add(task);
        }
    }

    private void dispatch(NamedTask task) {
        if (DBG) Log.d(TAG, "Dispatching " + task);
        mExecutor.execute(task);
    }

    /**
     * Instructs the executor to submit the next batch of results.
     * @param batchSize the maximum number of entries to execute.
     */
    public void executeNextBatch(int batchSize) {
        NamedTask[] batch = new NamedTask[0];
        synchronized (mQueuedTasks) {
            int count = Math.min(mQueuedTasks.size(), batchSize);
            List<NamedTask> nextTasks = mQueuedTasks.subList(0, count);
            batch = nextTasks.toArray(batch);
            nextTasks.clear();
            if (DBG) Log.d(TAG, "Dispatching batch of " + count);
        }

        for (NamedTask task : batch) {
            dispatch(task);
        }
    }

    /**
     * Cancel any unstarted tasks running in this executor.  This instance 
     * should not be re-used after calling this method.
     */
    public void cancelPendingTasks() {
        synchronized (mQueuedTasks) {
            mQueuedTasks.clear();
        }
    }

    public void close() {
        cancelPendingTasks();
        mExecutor.close();
    }
}
