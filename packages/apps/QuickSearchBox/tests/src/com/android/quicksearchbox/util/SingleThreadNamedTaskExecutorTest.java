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

import android.test.suitebuilder.annotation.MediumTest;

import java.util.concurrent.Executors;

import junit.framework.TestCase;

/**
 * Tests for {@link SingleThreadNamedTaskExecutor}.
 */
@MediumTest
public class SingleThreadNamedTaskExecutorTest extends TestCase {

    private SingleThreadNamedTaskExecutor mExecutor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mExecutor = new SingleThreadNamedTaskExecutor(Executors.defaultThreadFactory());
    }

    public void testExecute() throws Exception {
        MockTask a1 = addTask("a", 1);
        MockTask a2 = addTask("a", 2);

        // This just checks that the tasks run, and run in the right order
        a1.waitForCompletion();
        a2.waitForCompletion();
    }

    private MockTask addTask(String name, int id) {
        MockTask task = new MockTask(name, id);
        mExecutor.execute(task);
        return task;
    }

}
