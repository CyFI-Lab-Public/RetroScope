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

import android.database.DataSetObserver;

import junit.framework.Assert;

/**
 * A {@link DataSetObserver} that knows whether it has been notified.
 */
public class MockDataSetObserver extends DataSetObserver {

    private int mChangedCount = 0;
    private int mInvalidatedCount = 0;

    public void assertNotChanged() {
        Assert.assertFalse("onChanged() was called", mChangedCount > 0);
    }

    public void assertNotInvalidated() {
        Assert.assertFalse("onInvalidated() was called", mInvalidatedCount > 0);
    }

    public void assertChanged() {
        Assert.assertTrue("onChanged() was not called", mChangedCount > 0);
    }

    public void assertInvalidated() {
        Assert.assertTrue("onInvalidated() was not called", mInvalidatedCount > 0);
    }

    @Override
    public void onChanged() {
        mChangedCount++;
    }

    @Override
    public void onInvalidated() {
        mInvalidatedCount++;
    }

}
