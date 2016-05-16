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

import java.util.List;

public class Log {

    private static LogOutput sLogoutput = null;

    public static void setOutput(LogOutput logOutput) {
        sLogoutput = logOutput;
    }

    public static void verbose(String s) {
        if (sLogoutput != null) {
            sLogoutput.verbose(s);
        }
    }

    public static void warn(String message) {
        if (sLogoutput != null) {
            sLogoutput.warn(message);
        }
    }

    /**
     * Warns, and also puts a list of strings afterwards.
     */
    public static void warn(String message, List<String> list) {
        if (sLogoutput != null) {
            sLogoutput.warn(message, list);
        }
    }

    public static void info(String s) {
        if (sLogoutput != null) {
            sLogoutput.info(s);
        }
    }

    public static void info(String message, Throwable throwable) {
        if (sLogoutput != null) {
            sLogoutput.info(message, throwable);
        }
    }

    public static void nativeOutput(String outputLine) {
        if (sLogoutput != null) {
            sLogoutput.nativeOutput(outputLine);
        }

    }
}
