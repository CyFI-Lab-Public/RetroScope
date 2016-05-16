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


import java.util.HashMap;

/**
 * Uses a separate executor for each task name.
 */
public class PerNameExecutor implements NamedTaskExecutor {

    private final Factory<NamedTaskExecutor> mExecutorFactory;
    private HashMap<String, NamedTaskExecutor> mExecutors;

    /**
     * @param executorFactory Used to run the commands.
     */
    public PerNameExecutor(Factory<NamedTaskExecutor> executorFactory) {
        mExecutorFactory = executorFactory;
    }

    public synchronized void cancelPendingTasks() {
        if (mExecutors == null) return;
        for (NamedTaskExecutor executor : mExecutors.values()) {
            executor.cancelPendingTasks();
        }
    }

    public synchronized void close() {
        if (mExecutors == null) return;
        for (NamedTaskExecutor executor : mExecutors.values()) {
            executor.close();
        }
    }

    public synchronized void execute(NamedTask task) {
        if (mExecutors == null) {
            mExecutors = new HashMap<String, NamedTaskExecutor>();
        }
        String name = task.getName();
        NamedTaskExecutor executor = mExecutors.get(name);
        if (executor == null) {
            executor = mExecutorFactory.create();
            mExecutors.put(name, executor);
        }
        executor.execute(task);
    }

}
