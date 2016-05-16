/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.mail.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

/**
 * Class to queue tasks.  This delay the specified task by the specified time, up to the maximum
 * duration from the time that the first request was received
 */
public abstract class DelayedTaskHandler extends Handler {
    private final int mDelayMs;

    private static final long UNSET_TIME = -1;

    /**
     * Tracks the time when a task was queued, when there had been no pending task.
     */
    private long mLastTaskExecuteTime = UNSET_TIME;

    public DelayedTaskHandler(Looper looper, int defaultDelayMs) {
        super(looper);
        mDelayMs = defaultDelayMs;
    }

    /**
     * Schedule the task to be run after the default delay.
     */
    public void scheduleTask() {
        final long currentTime = SystemClock.elapsedRealtime();
        removeMessages(0);
        if (mLastTaskExecuteTime == UNSET_TIME ||
                ((mLastTaskExecuteTime + mDelayMs) <  currentTime)) {
            // If this is the first task that has been queued or if the last task ran more than
            // long enough ago that we don't want to delay this task, run it now
            sendEmptyMessage(0);
        } else {
            // Otherwise delay this task for the specify delay duration
            sendEmptyMessageDelayed(0, mDelayMs);
        }
    }

    @Override
    public void dispatchMessage(Message msg) {
        onTaskExecution();
        performTask();
    }

    /**
     * Called when the task managed by this handler is executed.  This method can also be called
     * to indicate that the task has been started externally.
     *
     * This updates the handler's internal timestamp.
     */
    public void onTaskExecution() {
        mLastTaskExecuteTime = SystemClock.elapsedRealtime();
    }

    /**
     * Method to perform the needed task.
     */
    protected abstract void performTask();
}
