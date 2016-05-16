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

import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A consumer that consumes a fixed number of values. When the expected number of values
 * has been consumed, further values are rejected.
 */
public class BarrierConsumer<A> implements Consumer<A> {

    private final Lock mLock = new ReentrantLock();
    private final Condition mNotFull = mLock.newCondition();

    private final int mExpectedCount;

    // Set to null when getValues() returns.
    private ArrayList<A> mValues;

    /**
     * Constructs a new BarrierConsumer.
     *
     * @param expectedCount The number of values to consume.
     */
    public BarrierConsumer(int expectedCount) {
        mExpectedCount = expectedCount;
        mValues = new ArrayList<A>(expectedCount);
    }

    /**
     * Blocks until the expected number of results is available, or until the thread is
     * interrupted. This method should not be called multiple times.
     *
     * @return A list of values, never {@code null}.
     */
    public ArrayList<A> getValues() {
        mLock.lock();
        try {
            try {
                while (!isFull()) {
                    mNotFull.await();
                }
            } catch (InterruptedException ex) {
                // Return the values that we've gotten so far
            }
            ArrayList<A> values = mValues;
            mValues = null;  // mark that getValues() has returned
            return values;
        } finally {
            mLock.unlock();
        }
    }

    public boolean consume(A value) {
        mLock.lock();
        try {
            // Do nothing if getValues() has alrady returned,
            // or enough values have already been consumed
            if (mValues == null || isFull()) {
                return false;
            }
            mValues.add(value);
            if (isFull()) {
                // Wake up any thread waiting in getValues()
                mNotFull.signal();
            }
            return true;
        } finally {
            mLock.unlock();
        }
    }

    private boolean isFull() {
        return mValues.size() == mExpectedCount;
    }
}
