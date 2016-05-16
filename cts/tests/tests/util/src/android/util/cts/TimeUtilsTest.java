/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.util.TimeUtils;

import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;

public class TimeUtilsTest extends TestCase {
    public void testUnitedStates() throws Exception {
        String[] mainstream = new String[] {
            "America/New_York", // Eastern
            "America/Chicago", // Central
            "America/Denver", // Mountain
            "America/Los_Angeles", // Pacific
            "America/Anchorage", // Alaska
            "Pacific/Honolulu", // Hawaii, no DST
        };

        for (String name : mainstream) {
            TimeZone tz = TimeZone.getTimeZone(name);
            Calendar c = Calendar.getInstance(tz);
            TimeZone guess;

            c.set(2008, Calendar.OCTOBER, 20, 12, 00, 00);
            guess = guessTimeZone(c, "us");
            assertEquals(name, guess.getID());

            c.set(2009, Calendar.JANUARY, 20, 12, 00, 00);
            guess = guessTimeZone(c, "us");
            assertEquals(name, guess.getID());
        }
    }

    public void testWeirdUnitedStates() throws Exception {
        String[] weird = new String[] {
            "America/Phoenix", // Mountain, no DST
            "America/Adak", // Same as Hawaii, but with DST
        };

        for (String name : weird) {
            TimeZone tz = TimeZone.getTimeZone(name);
            Calendar c = Calendar.getInstance(tz);
            TimeZone guess;

            c.set(2008, Calendar.OCTOBER, 20, 12, 00, 00);
            guess = guessTimeZone(c, "us");
            assertEquals(name, guess.getID());
        }
    }

    public void testOld() throws Exception {
        String[] old = new String[] {
            "America/Indiana/Indianapolis", // Eastern, formerly no DST
        };

        for (String name : old) {
            TimeZone tz = TimeZone.getTimeZone(name);
            Calendar c = Calendar.getInstance(tz);
            TimeZone guess;

            c.set(2005, Calendar.OCTOBER, 20, 12, 00, 00);
            guess = guessTimeZone(c, "us");
            assertEquals(name, guess.getID());
        }
    }

    public void testWorldWeird() throws Exception {
        String[] world = new String[] {
            // Distinguisable from Sydney only when DST not in effect
            "au", "Australia/Lord_Howe",
        };

        for (int i = 0; i < world.length; i += 2) {
            String country = world[i];
            String name = world[i + 1];

            TimeZone tz = TimeZone.getTimeZone(name);
            Calendar c = Calendar.getInstance(tz);
            TimeZone guess;

            c.set(2009, Calendar.JULY, 20, 12, 00, 00);
            guess = guessTimeZone(c, country);
            assertEquals(name, guess.getID());
        }
    }

    private static TimeZone guessTimeZone(Calendar c, String country) {
        return TimeUtils.getTimeZone(c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET),
                                     c.get(Calendar.DST_OFFSET) != 0,
                                     c.getTimeInMillis(),
                                     country);
    }
}
