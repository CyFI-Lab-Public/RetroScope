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

import android.database.DataSetObservable;
import android.os.Handler;

/**
 * A version of {@link DataSetObservable} that performs callbacks on given {@link Handler}.
 */
public class AsyncDataSetObservable extends DataSetObservable {

    private final Handler mHandler;

    private final Runnable mChangedRunnable = new Runnable() {
        public void run() {
            AsyncDataSetObservable.super.notifyChanged();
        }
    };

    private final Runnable mInvalidatedRunnable = new Runnable() {
        public void run() {
            AsyncDataSetObservable.super.notifyInvalidated();
        }
    };

    /**
     * @param handler Handler to run callbacks on.
     */
    public AsyncDataSetObservable(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void notifyChanged() {
        if (mHandler == null) {
            super.notifyChanged();
        } else {
            mHandler.post(mChangedRunnable);
        }
    }

    @Override
    public void notifyInvalidated() {
        if (mHandler == null) {
            super.notifyInvalidated();
        } else {
            mHandler.post(mInvalidatedRunnable);
        }
    }

}
