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

package vogar.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for working with threads.
 */
public final class Threads {
    private Threads() {}

    public static ThreadFactory daemonThreadFactory(final String name) {
        return new ThreadFactory() {
            private int nextId = 0;
            public synchronized Thread newThread(Runnable r) {
                Thread thread = new Thread(r, name + "-" + (nextId++));
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    public static ExecutorService threadPerCpuExecutor(String name) {
        return fixedThreadsExecutor(name, Runtime.getRuntime().availableProcessors());
    }

    public static ExecutorService fixedThreadsExecutor(String name, int count) {
        ThreadFactory threadFactory = daemonThreadFactory(name);

        return new ThreadPoolExecutor(count, count, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(Integer.MAX_VALUE), threadFactory) {
            @Override protected void afterExecute(Runnable runnable, Throwable throwable) {                if (throwable != null) {
                    Log.info("Unexpected failure from " + runnable, throwable);
                }
            }
        };
    }
}
