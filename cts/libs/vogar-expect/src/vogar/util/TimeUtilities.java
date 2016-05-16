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

package vogar.util;

/**
 * Utilities to make it easier to work with ISO 8601 dates and times.
 * This is a subset of the original class from http://software.jessies.org/salma-hayek/ --- please submit fixes upstream.
 */
public class TimeUtilities {
    /**
     * Returns the ISO 8601-format String corresponding to the given duration (measured in milliseconds).
     */
    public static String msToIsoString(long duration) {
        long milliseconds = duration % 1000;
        duration /= 1000;
        long seconds = duration % 60;
        duration /= 60;
        long minutes = duration % 60;
        duration /= 60;
        long hours = duration;

        StringBuilder result = new StringBuilder("P");
        if (hours != 0) {
            result.append(hours);
            result.append('H');
        }
        if (result.length() > 1 || minutes != 0) {
            result.append(minutes);
            result.append('M');
        }
        result.append(seconds);
        if (milliseconds != 0) {
            result.append('.');
            result.append(milliseconds);
        }
        result.append('S');
        return result.toString();
    }
    
    /**
     * Returns a string representation of the given number of milliseconds.
     */
    public static String msToString(long ms) {
        return nsToString(ms * 1000000);
    }
    
    /**
     * Returns a string representation of the given number of nanoseconds.
     */
    public static String nsToString(long ns) {
        if (ns < 1000L) {
            return Long.toString(ns) + "ns";
        } else if (ns < 1000000L) {
            return Long.toString(ns/1000L) + "us";
        } else if (ns < 1000000000L) {
            return Long.toString(ns/1000000L) + "ms";
        } else if (ns < 60000000000L) {
            return String.format("%.2fs", nsToS(ns));
        } else {
            long duration = ns;
            long nanoseconds = duration % 1000;
            duration /= 1000;
            long microseconds = duration % 1000;
            duration /= 1000;
            long milliseconds = duration % 1000;
            duration /= 1000;
            long seconds = duration % 60;
            duration /= 60;
            long minutes = duration % 60;
            duration /= 60;
            long hours = duration % 24;
            duration /= 24;
            long days = duration;
            
            StringBuilder result = new StringBuilder();
            if (days != 0) {
                result.append(days);
                result.append('d');
            }
            if (result.length() > 1 || hours != 0) {
                result.append(hours);
                result.append('h');
            }
            if (result.length() > 1 || minutes != 0) {
                result.append(minutes);
                result.append('m');
            }
            result.append(seconds);
            result.append('s');
            return result.toString();
        }
    }
    
    /**
     * Converts nanoseconds into (fractional) seconds.
     */
    public static double nsToS(long ns) {
        return ((double) ns)/1000000000.0;
    }

    private TimeUtilities() {
    }
}
