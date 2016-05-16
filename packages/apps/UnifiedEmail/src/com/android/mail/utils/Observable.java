/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.mail.utils;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

/**
 * A Utility class to register observers and return logging and counts for the number of registered
 * observers.
 */
public class Observable extends DataSetObservable {
    protected static final String LOG_TAG = LogTag.getLogTag();
    private final String mName;

    public Observable(String name) {
        mName = name;
    }

    @Override
    public void registerObserver(DataSetObserver observer) {
        final int count = mObservers.size();
        super.registerObserver(observer);
        LogUtils.d(LOG_TAG, "IN register(%s)Observer: %s before=%d after=%d",
                mName,  observer, count, mObservers.size());
    }

    @Override
    public void unregisterObserver(DataSetObserver observer) {
        final int count = mObservers.size();
        super.unregisterObserver(observer);
        LogUtils.d(LOG_TAG, "IN unregister(%s)Observer: %s before=%d after=%d",
                mName, observer, count, mObservers.size());
    }
}
