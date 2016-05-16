/*
 * Copyright (C) 2009 The Android Open Source Project
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

/**
 * Runs tasks that have a name tag.
 */
public interface NamedTaskExecutor {

    /**
     * Schedules a task for execution. Implementations should not throw
     * {@link java.util.concurrent.RejectedExecutionException} if the task
     * cannot be run. They should drop it silently instead.
     */
    void execute(NamedTask task);

    /**
     * Stops any unstarted tasks from running. Implementations of this method must be
     * idempotent.
     */
    void cancelPendingTasks();

    /**
     * Shuts down this executor, freeing any resources that it owns. The executor
     * may not be used after calling this method. Implementations of this method must be
     * idempotent.
     */
    void close();

}
