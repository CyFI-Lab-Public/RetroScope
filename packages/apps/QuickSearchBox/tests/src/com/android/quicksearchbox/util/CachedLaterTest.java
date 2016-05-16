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

import com.android.quicksearchbox.ConsumerTrap;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

/**
 * Tests for {@link CachedLater}.
 */
@SmallTest
public class CachedLaterTest extends TestCase {

    private Harness mHarness;
    private ConsumerTrap<String> mTrap;
    private ConsumerTrap<String> mTrap2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mHarness = new Harness();
        mTrap = new ConsumerTrap<String>();
        mTrap2 = new ConsumerTrap<String>();
    }

    public void testGetLaterAsync() {
        mHarness.getLater(mTrap);
        // The consumer should not have been called here
        assertNull(mTrap.getValue());
        mHarness.finishCreate("foo");
        assertEquals("foo", mTrap.getValue());
    }

    public void testGetLaterSync() {
        mHarness.getLater(mTrap);  // trigger create()
        mHarness.finishCreate("foo");
        mHarness.getLater(mTrap2);  // sync call
        assertEquals("foo", mTrap2.getValue());
    }

    public void testGetLaterWhileCreating() {
        mHarness.getLater(mTrap);  // trigger create()
        mHarness.getLater(mTrap2);  // register a second consumer
        mHarness.finishCreate("foo");
        assertEquals("foo", mTrap.getValue());
        assertEquals("foo", mTrap2.getValue());
    }

    public void testGetLaterAfterClear() {
        mHarness.getLater(mTrap);  // trigger create()
        mHarness.finishCreate("foo");
        mHarness.clear();
        mHarness.getLater(mTrap2);
        mHarness.finishCreate("bar");
        assertEquals("bar", mTrap2.getValue());
    }

    public void testHaveNow() {
        assertFalse(mHarness.haveNow());
        mHarness.getLater(mTrap);
        assertFalse(mHarness.haveNow());
        mHarness.finishCreate("foo");
        assertTrue(mHarness.haveNow());
    }

    public void testClear() {
        mHarness.getLater(mTrap);  // trigger create()
        mHarness.finishCreate("foo");
        mHarness.clear();
        assertFalse(mHarness.haveNow());
        mHarness.getLater(mTrap2);
        assertNull(mTrap2.getValue());
    }

    private static class Harness extends CachedLater<String> {

        private boolean mCreatePending = false;

        protected void create() {
            mCreatePending = true;
        }

        public void finishCreate(String str) {
            if (mCreatePending) {
                store(str);
                mCreatePending = false;
            }
        }

    }

}
