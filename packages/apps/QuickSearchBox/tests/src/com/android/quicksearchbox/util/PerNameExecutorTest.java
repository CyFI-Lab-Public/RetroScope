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

import static com.android.quicksearchbox.util.MockTask.assertRanNever;
import static com.android.quicksearchbox.util.MockTask.assertRanOnce;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

/**
 * Tests for {@link PerNameExecutor}.
 */
@SmallTest
public class PerNameExecutorTest extends TestCase {

    private MockNamedTaskExecutorFactory mExecutorFactory;
    private NamedTaskExecutor mExecutor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mExecutorFactory = new MockNamedTaskExecutorFactory();
        mExecutor = new PerNameExecutor(mExecutorFactory);
    }

    public void testExecute() {
        MockTask a1 = addTask("a", 1);
        MockTask a2 = addTask("a", 2);
        MockTask a3 = addTask("a", 3);
        MockTask b1 = addTask("b", 1);

        assertRanNever("step 0", a1, a2, a3, b1);
        step(); // run a1, b1
        assertRanOnce("step 1", a1, b1);
        assertRanNever("step 1", a2, a3);
        step(); // run a2
        assertRanOnce("step 2", a1, b1, a2);
        assertRanNever("step 2", a3);
        step(); // run a3
        assertRanOnce("step 3", a1, b1, a2, a3);
        step(); // should do nothing
        assertRanOnce("step 4", a1, b1, a2, a3);
    }

    public void testCancelPendingTasks() {
        MockTask a1 = addTask("a", 1);
        MockTask a2 = addTask("a", 2);
        MockTask b1 = addTask("b", 1);

        step(); // run a1, b1
        assertRanOnce("step 1", a1, b1);
        assertRanNever("step 1", a2);
        mExecutor.cancelPendingTasks(); // cancel a2
        assertRanOnce("step 1, after cancel", a1, b1);
        assertRanNever("step 1, after cancel", a2);
        step(); // do nothing
        assertRanOnce("step 2", a1, b1);
        assertRanNever("step 2", a2);

        MockTask a3 = addTask("a" , 3);
        MockTask c1 = addTask("c" , 1);

        assertRanNever("step 2, new tasks", a3, c1, a2);
        step(); // run a3, c1
        assertRanOnce("step 3", a1, b1, a3, c1);
        assertRanNever("step 3", a2);
        step(); // do nothing
        assertRanOnce("step 4", a1, b1, a3, c1);
        assertRanNever("step 4", a2);
    }

    public void testClose() {
        MockTask a1 = new MockTask("a", 1);
        mExecutor.execute(a1);

        assertRanNever("before close()", a1);
        mExecutor.close();
        assertRanNever("after close() 1", a1);
        mExecutor.close();
        assertRanNever("after close() 2", a1);
    }

    private MockTask addTask(String name, int id) {
        MockTask task = new MockTask(name, id);
        mExecutor.execute(task);
        return task;
    }

    /**
     * Run one task for each name.
     */
    private void step() {
        mExecutorFactory.runNext();
    }

}
