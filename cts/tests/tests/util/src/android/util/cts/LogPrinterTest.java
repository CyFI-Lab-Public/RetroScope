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
import android.util.LogPrinter;

public class LogPrinterTest extends AndroidTestCase {
    private final String mTag="LogPrinterTest";
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testConstructor() {
        int[] priorities = { Log.ASSERT, Log.DEBUG, Log.ERROR, Log.INFO,
                Log.VERBOSE, Log.WARN };
        for (int i = 0; i < priorities.length; i++) {
            new LogPrinter(priorities[i], mTag);
        }
    }

    public void testPrintln() {
        LogPrinter logPrinter = new LogPrinter(Log.DEBUG, mTag);
        String mMessage = "testMessage";
        logPrinter.println(mMessage);
    }

}
