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

package android.util.cts;

import android.test.AndroidTestCase;
import android.util.Log;

/**
 * 
 * Test Log
 *
 */
public class LogTest extends AndroidTestCase{
    private static final String TAG = "LogTest";

    public void testLogOperations() {
        final String msg = "Test Log operations.";
        Exception tr = null;
        try {
            throw new Exception();
        } catch (Exception e) {
            tr = e;
        }
        // println and getStackTraceString are called in e w i d v
        Log.e(TAG, msg);
        Log.e(TAG, msg, tr);
        Log.w(TAG, msg);
        Log.w(TAG, tr);
        Log.w(TAG, msg, tr);
        Log.i(TAG, msg);
        Log.i(TAG, msg, tr);
        Log.d(TAG, msg);
        Log.d(TAG, msg, tr);
        Log.v(TAG, msg);
        Log.v(TAG, msg, tr);
        assertTrue(Log.isLoggable(TAG, Log.INFO));
    }
}
