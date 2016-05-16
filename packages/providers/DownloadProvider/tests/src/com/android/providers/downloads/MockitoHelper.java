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

package com.android.providers.downloads;

import android.util.Log;

/**
 * Helper for Mockito-based test cases.
 */
public final class MockitoHelper {
    private static final String TAG = "MockitoHelper";

    private ClassLoader mOriginalClassLoader;
    private Thread mContextThread;

    /**
     * Creates a new helper, which in turn will set the context classloader so
     * it can load Mockito resources.
     *
     * @param packageClass test case class
     */
    public void setUp(Class<?> packageClass) throws Exception {
        // makes a copy of the context classloader
        mContextThread = Thread.currentThread();
        mOriginalClassLoader = mContextThread.getContextClassLoader();
        ClassLoader newClassLoader = packageClass.getClassLoader();
        Log.v(TAG, "Changing context classloader from " + mOriginalClassLoader
                + " to " + newClassLoader);
        mContextThread.setContextClassLoader(newClassLoader);
    }

    /**
     * Restores the context classloader to the previous value.
     */
    public void tearDown() throws Exception {
        Log.v(TAG, "Restoring context classloader to " + mOriginalClassLoader);
        mContextThread.setContextClassLoader(mOriginalClassLoader);
    }
}
