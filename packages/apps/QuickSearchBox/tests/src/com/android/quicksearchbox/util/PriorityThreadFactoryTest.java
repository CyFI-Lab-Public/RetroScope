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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.concurrent.ThreadFactory;

/**
 * Tests for {@link PriorityThreadFactory}.
 */
@MediumTest
public class PriorityThreadFactoryTest extends AndroidTestCase {

    public void testPriority() throws InterruptedException {
        priorityTest(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        priorityTest(android.os.Process.THREAD_PRIORITY_DEFAULT);
        priorityTest(android.os.Process.THREAD_PRIORITY_FOREGROUND);
    }

    /**
     * Helper method for {@link #testPriority()}.
     */
    private void priorityTest(int priority) throws InterruptedException {
        ThreadFactory factory = new PriorityThreadFactory(priority);
        CheckPriorityRunnable r = new CheckPriorityRunnable();
        Thread t = factory.newThread(r);
        t.start();
        assertEquals(priority, r.getPriority());
    }

    /**
     * Helper class for {@link #priorityTest(int)}.
     */
    private static class CheckPriorityRunnable implements Runnable {
        private Integer mPriority = null;
        public synchronized int getPriority() throws InterruptedException {
            while (mPriority == null) {
                wait();
            }
            return mPriority.intValue();
        }
        public synchronized void run() {
            int tid = android.os.Process.myTid();
            mPriority = new Integer(android.os.Process.getThreadPriority(tid));
            notify();
        }
    }

}
