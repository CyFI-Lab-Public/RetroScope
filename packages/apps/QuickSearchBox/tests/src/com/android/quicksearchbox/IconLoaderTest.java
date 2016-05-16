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

package com.android.quicksearchbox;

import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.NowOrLater;

import android.test.AndroidTestCase;

/**
 * Base class for tests for {@link IconLoader} subclasses.
 *
 */
public abstract class IconLoaderTest extends AndroidTestCase {

    protected IconLoader mLoader;

    @Override
    protected void setUp() throws Exception {
        mLoader = create();
    }

    protected abstract IconLoader create() throws Exception;

    public void testGetIcon() {
        assertNull(mLoader.getIcon(null));
        assertNull(mLoader.getIcon(""));
        assertNull(mLoader.getIcon("0"));
        assertNotNull(mLoader.getIcon(String.valueOf(android.R.drawable.star_on)));
    }

    public void assertNull(NowOrLater<?> value) {
        if (value.haveNow()) {
            assertNull(value.getNow());
        } else {
            AssertConsumer<Object> consumer = new AssertConsumer<Object>();
            value.getLater(consumer);
            consumer.assertNull();
        }
    }

    public void assertNotNull(NowOrLater<?> value) {
        if (value.haveNow()) {
            assertNotNull(value.getNow());
        } else {
            AssertConsumer<Object> consumer = new AssertConsumer<Object>();
            value.getLater(consumer);
            consumer.assertNotNull();
        }
    }

    protected static class AssertConsumer<C> implements Consumer<C> {
        private boolean mConsumed = false;
        private Object mValue;
        public boolean consume(Object value) {
            synchronized(this) {
                mConsumed = true;
                mValue = value;
                this.notifyAll();
            }
            return true;
        }
        public synchronized Object waitFor() {
            if (!mConsumed) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                }
            }
            return mValue;
        }
        public void assertNull() {
            IconLoaderTest.assertNull(waitFor());
        }
        public void assertNotNull() {
            IconLoaderTest.assertNotNull(waitFor());
        }
        public AssertConsumer<C> reset() {
            mConsumed = false;
            mValue = null;
            return this;
        }
    }

}
