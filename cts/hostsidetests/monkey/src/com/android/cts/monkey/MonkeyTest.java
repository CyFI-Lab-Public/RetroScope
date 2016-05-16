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

package com.android.cts.monkey;

import com.android.tradefed.device.DeviceNotAvailableException;

import java.util.Scanner;

public class MonkeyTest extends AbstractMonkeyTest {

    private static final String MONKEY = "@(>.<)@";
    private static final String HUMAN = "(^_^)";

    public void testIsMonkey() throws Exception {
        mDevice.executeShellCommand(MONKEY_CMD + " -p " + PKGS[0] + " 500");
        assertIsUserAMonkey(true);
    }

    public void testNotMonkey() throws Exception {
        mDevice.executeShellCommand("am start -W -a android.intent.action.MAIN "
                + "-n com.android.cts.monkey/com.android.cts.monkey.MonkeyActivity");
        assertIsUserAMonkey(false);
    }

    private void assertIsUserAMonkey(boolean isMonkey) throws DeviceNotAvailableException {
        String logs = mDevice.executeAdbCommand("logcat", "-d", "MonkeyActivity:I", "*:S");
        boolean monkeyLogsFound = false;
        Scanner s = new Scanner(logs);
        try {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.contains(isMonkey ? MONKEY : HUMAN)) {
                    monkeyLogsFound = true;
                }
            }
            assertTrue(monkeyLogsFound);
        } finally {
            s.close();
        }
    }
}
