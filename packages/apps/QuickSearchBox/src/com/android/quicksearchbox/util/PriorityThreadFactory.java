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

import android.os.Process;

import java.util.concurrent.ThreadFactory;

/**
 * A thread factory that creates threads with a given thread priority.
 */
public class PriorityThreadFactory implements ThreadFactory {

    private final int mPriority;

    /**
     * Creates a new thread factory.
     *
     * @param priority The thread priority of the threads created by this factory.
     *        For values, see {@link Process}.
     */
    public PriorityThreadFactory(int priority) {
        mPriority = priority;
    }

    public Thread newThread(Runnable r) {
        return new Thread(r) {
            @Override
            public void run() {
                Process.setThreadPriority(mPriority);
                super.run();
            }
        };
    }

}
