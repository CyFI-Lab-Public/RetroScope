/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.util;


public class MeasureTime {
    /**
     * measure time taken for each run for given count
     * @param count
     * @param run
     * @return array of time taken in each run in msec.
     * @throws IOException
     */
    public static double[] measure(int count, MeasureRun run)  throws Exception {
        double[] result = new double[count];

        for (int i = 0; i < count; i++) {
            run.prepare(i);
            long start = System.currentTimeMillis();
            run.run(i);
            long end =  System.currentTimeMillis();
            result[i] = end - start;
        }
        return result;
    }
}
