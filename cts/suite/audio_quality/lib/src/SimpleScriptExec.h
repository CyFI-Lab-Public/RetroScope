/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#ifndef CTSAUDIO_SIMPLESCRIPTEXEC_H
#define CTSAUDIO_SIMPLESCRIPTEXEC_H

#include <sys/types.h>
#include <regex.h>

#include <utils/String8.h>

/**
 * Utility class for executing simple scripts
 * which prints ___CTS_AUDIO_PASS___ string in output
 */
class SimpleScriptExec {
public:
    static const char* PYTHON_PATH;

    static bool checkPythonEnv();
    /**
     * run given script
     * @param script full path of the script
     * @param param arguments to pass
     * @param result
     */
    static bool runScript(const android::String8& script, const android::String8& param,
            android::String8& result);

    /**
     * check if the given str include magic words for pass.
     * @param str
     * @param reMatch pattern to match in re besides the pass string
     * @param nmatch number of substring pattern match elements. It should be in POSIX
     *        extended RE syntax, no \d nor [:digit:]
     * @param pmatch pattern match elements
     * @return true if passed
     */
    static bool checkIfPassed(const android::String8& str, const android::String8& reMatch,
            int nmatch = 0, regmatch_t pmatch[] = NULL);
};


#endif // CTSAUDIO_SIMPLESCRIPTEXEC_H
