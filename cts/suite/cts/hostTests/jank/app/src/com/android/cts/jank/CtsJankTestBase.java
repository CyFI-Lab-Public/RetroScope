/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.cts.jank;

import android.os.Bundle;
import android.util.Log;

import com.android.uiautomator.platform.JankTestBase;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class CtsJankTestBase extends JankTestBase {
    private final static String TAG = CtsJankTestBase.class.getName();
    protected final static String START_CMD = "am start -W -a android.intent.action.MAIN -n %s";
    protected final static String STOP_CMD = "am force-stop %s";
    protected final static String INTENT_STRING_EXTRA = " --es %s %s";
    protected final static String INTENT_BOOLEAN_EXTRA = " --ez %s %b";
    protected final static String INTENT_INTEGER_EXTRA = " --ei %s %d";
    protected static long SLEEP_TIME = 2000; // 2 seconds
    protected static int NUM_ITERATIONS = 5;
    protected static int TRACE_TIME = 5;

    @Override
    protected String getPropertyString(Bundle params, String key)
            throws FileNotFoundException, IOException {
        if (key.equals("iteration")) {
            return NUM_ITERATIONS + "";
        }
        if (key.equals("tracetime")) {
            return TRACE_TIME + "";
        }
        return super.getPropertyString(params, key);
    }

    protected void runShellCommand(String command) throws Exception {
        Process p = null;
        Scanner out = null;
        Scanner err = null;
        try {
            p = Runtime.getRuntime().exec(command);

            StringBuilder outStr = new StringBuilder();
            StringBuilder errStr = new StringBuilder();
            out = new Scanner(p.getInputStream());
            err = new Scanner(p.getErrorStream());
            boolean read = true;
            while (read) {
                if (out.hasNextLine()) {
                    outStr.append(out.nextLine());
                    outStr.append("\n");
                } else if (err.hasNextLine()) {
                    errStr.append(err.nextLine());
                    errStr.append("\n");
                } else {
                    read = false;
                }
            }
            Log.i(TAG, command);
            if (outStr.length() > 0) {
                Log.i(TAG, outStr.toString());
            }
            if (errStr.length() > 0) {
                Log.e(TAG, errStr.toString());
            }
        } finally {
            if (p != null) {
                int status = p.waitFor();
                if (status != 0) {
                    throw new RuntimeException(
                            String.format("Run shell command: %s, status: %s", command, status));
                }
                p.destroy();
                p = null;
            }
            if (out != null) {
                out.close();
            }
            if (err != null) {
                err.close();
            }
        }
    }
}
