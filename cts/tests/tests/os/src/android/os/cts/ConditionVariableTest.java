/*
 * Copyright (C) 2008 The Android Open Source Project
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
package android.os.cts;

import junit.framework.TestCase;
import android.os.ConditionVariable;

public class ConditionVariableTest extends TestCase {
    private static final int WAIT_TIME = 3000;
    private static final int BLOCK_TIME = 1000;
    private static final int BLOCK_TIME_DELTA = 200;
    private static final int SLEEP_TIME = 1000;
    private static final int TOLERANCE_MS = BLOCK_TIME;
    private ConditionVariable mConditionVariable;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mConditionVariable = new ConditionVariable();
    }

    public void testConstructor() {
        assertFalse(mConditionVariable.block(BLOCK_TIME));
        assertFalse(new ConditionVariable(false).block(BLOCK_TIME));
        assertTrue(new ConditionVariable(true).block(BLOCK_TIME));
    }

    public void testConditionVariable() throws Throwable {
        // test open then block(long)
        mConditionVariable.open();
        long time = System.currentTimeMillis();
        assertTrue(mConditionVariable.block(BLOCK_TIME));
        assertTrue(System.currentTimeMillis() - time < TOLERANCE_MS);

        // test close then block(long)
        mConditionVariable.close();
        time = System.currentTimeMillis();
        assertFalse(mConditionVariable.block(BLOCK_TIME));
        assertTrue(System.currentTimeMillis() - time >= BLOCK_TIME);

        // test block then open
        time = System.currentTimeMillis();
        TestThread t = new TestThread(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
                mConditionVariable.open();
            }
        });

        t.start();
        mConditionVariable.block();
        long timeDelta = System.currentTimeMillis() - time;
        assertTrue(timeDelta >= BLOCK_TIME && timeDelta <= BLOCK_TIME + BLOCK_TIME_DELTA);
        t.joinAndCheck(WAIT_TIME);

        time = System.currentTimeMillis();
        t = new TestThread(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(BLOCK_TIME >> 1);
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
                mConditionVariable.open();
            }
        });
        t.start();

        assertTrue(mConditionVariable.block(BLOCK_TIME));
        t.joinAndCheck(WAIT_TIME);
    }
}
