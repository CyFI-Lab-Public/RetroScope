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
import android.util.TimingLogger;

public class TimingLoggerTest extends AndroidTestCase{
    private static final String LOG_TAG = "TimingLoggerTest";
    private static final int SLEEPING_MSEC = 100;

    public void testTimingLogger() {
        TimingLogger timings = new TimingLogger(LOG_TAG, "testTimingLogger");

        for (int i = 0; i < 3; i++) {
            if (1 == i) {
                timings.reset(LOG_TAG, "testReset");
            } else if (2 == i) {
                timings.reset();
            }

            sleep();
            timings.addSplit("fisrt sleep");

            sleep();
            timings.addSplit("second sleep");

            sleep();
            timings.addSplit("third sleep");

            timings.dumpToLog();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(SLEEPING_MSEC);
        } catch (InterruptedException e) {

        }
    }
}
