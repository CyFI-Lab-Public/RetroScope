/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.cts.tradefed.result;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for formatting times as strings.
 */
class TimeUtil {

    /**
     * Return a prettified version of the given elapsed time
     * @return
     */
    static String formatElapsedTime(long elapsedTimeMs) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMs) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMs) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedTimeMs);
        StringBuilder time = new StringBuilder();
        if (hours > 0) {
            time.append(hours);
            time.append("h ");
        }
        if (minutes > 0) {
            time.append(minutes);
            time.append("m ");
        }
        time.append(seconds);
        time.append("s");

        return time.toString();
    }

    /**
     * Return the current timestamp as a {@link String} suitable for displaying.
     * <p/>
     * Example: Fri Aug 20 15:13:03 PDT 2010
     */
    static String getTimestamp() {
        return getTimestamp(System.currentTimeMillis());
    }

    /**
     * Return the given time as a {@link String} suitable for displaying.
     * <p/>
     * Example: Fri Aug 20 15:13:03 PDT 2010
     *
     * @param time the epoch time in ms since midnight Jan 1, 1970
     */
    static String getTimestamp(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        return dateFormat.format(new Date(time));
    }

    /**
     * Return the current timestamp in a compressed format, used to uniquely identify results.
     * <p/>
     * Example: 2010.08.16_11.42.12
     */
    static String getResultTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
        return dateFormat.format(new Date());
    }

}
