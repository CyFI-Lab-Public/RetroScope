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

import java.util.Scanner;

public class SeedTest extends AbstractMonkeyTest {

    public void testSeed() throws Exception {
        String cmd1 = MONKEY_CMD + " -s 1337 -v -p " + PKGS[0] + " 500";
        String out1 = mDevice.executeShellCommand(cmd1);
        String out2 = mDevice.executeShellCommand(cmd1);
        assertOutputs(out1, out2);

        String cmd2 = MONKEY_CMD + " -s 3007 -v -p " + PKGS[0] + " 125";
        String out3 = mDevice.executeShellCommand(cmd2);
        String out4 = mDevice.executeShellCommand(cmd2);
        assertOutputs(out3, out4);
    }

    private void assertOutputs(String out1, String out2) {
        Scanner s1 = new Scanner(out1);
        Scanner s2 = new Scanner(out2);
        int numEvents = 0;
        while (true) {
            String line1 = getNextLine(s1);
            String line2 = getNextLine(s2);
            if (line1 != null || line2 != null) {
                assertEquals(line1, line2);
                numEvents++;
            } else {
                break;
            }
        }
        assertTrue(numEvents > 0);
    }

    private String getNextLine(Scanner sc) {
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.startsWith(":Sending")) {
                return line;
            }
        }
        return null;
    }
}
