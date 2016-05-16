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

package android.permission.cts;

import android.permission.cts.FileUtils.FileStatus;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Verify the read system log require specific permissions.
 */
public class NoReadLogsPermissionTest extends AndroidTestCase {
    /**
     * Verify that we'll only get our logs without the READ_LOGS permission.
     *
     * We test this by examining the logs looking for ActivityManager lines.
     * Since ActivityManager runs as a different UID, we shouldn't see
     * any of those log entries.
     *
     * @throws IOException
     */
    @MediumTest
    public void testLogcat() throws IOException {
        Process logcatProc = null;
        BufferedReader reader = null;
        try {
            logcatProc = Runtime.getRuntime().exec(new String[]
                    {"logcat", "-d", "ActivityManager:* *:S" });

            reader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()));

            int lineCt = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("--------- beginning of /dev/log")) {
                    lineCt++;
                }
            }

            // no permission get an empty log buffer.
            // Logcat returns only one line:
            // "--------- beginning of /dev/log/main"

            assertEquals("Unexpected logcat entries. Are you running the "
                       + "the latest logger.c from the Android kernel?",
                    0, lineCt);

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public void testLogFilePermissions() {
        File logDir = new File("/dev/log");
        File[] logFiles = logDir.listFiles();
        assertTrue("Where are the log files? Please check that they are not world readable.",
                logFiles.length > 0);

        FileStatus status = new FileStatus();
        for (File log : logFiles) {
            if (FileUtils.getFileStatus(log.getAbsolutePath(), status, false)) {
                assertEquals("Log file " + log.getAbsolutePath() + " should have user root.",
                        0, status.uid);
                assertTrue("Log file " + log.getAbsolutePath() + " should have group log.",
                        "log".equals(FileUtils.getGroupName(status.gid)));
            }
        }
    }
}
