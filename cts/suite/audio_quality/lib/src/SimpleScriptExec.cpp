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

#include <stdlib.h>
#include <stdio.h>

#include "Log.h"
#include "StringUtil.h"

#include "SimpleScriptExec.h"

const char* SimpleScriptExec::PYTHON_PATH = "/usr/bin/python";
const char* PASS_MAGIC_WORD = "___CTS_AUDIO_PASS___";

bool SimpleScriptExec::checkPythonEnv()
{
    android::String8 script("test_description/conf/check_conf.py");
    android::String8 param;
    android::String8 result;
    if (!runScript(script, param, result)) {
        return false;
    }

    android::String8 rePattern;
    return checkIfPassed(result, rePattern);
}

bool SimpleScriptExec::checkIfPassed(const android::String8& str, const android::String8& reMatch,
        int nmatch, regmatch_t pmatch[])
{
    android::String8 match;
    match.append(PASS_MAGIC_WORD);
    match.append(reMatch);
    LOGV("re match %s", match.string());
    regex_t re;
    int cflags = REG_EXTENDED;
    if (nmatch == 0) {
        cflags |= REG_NOSUB;
    }
    if (regcomp(&re, match.string(), cflags) != 0) {
        LOGE("regcomp failed");
        return false;
    }
    bool result = false;
    if (regexec(&re, str.string(), nmatch, pmatch, 0) == 0) {
        // match found. passed
        result = true;
    }
    regfree(&re);
    return result;
}

bool SimpleScriptExec::runScript(const android::String8& script, const android::String8& param,
        android::String8& result)
{
    FILE *fpipe;
    android::String8 command;
    command.appendFormat("%s %s %s", PYTHON_PATH, script.string(), param.string());
    const int READ_SIZE = 1024;
    char buffer[READ_SIZE];
    size_t len = 0;

    if ( !(fpipe = (FILE*)popen(command.string(),"r")) ) {
        LOGE("cannot execute python");
        return false;
    }
    result.clear();
    while((len = fread(buffer, 1, READ_SIZE, fpipe)) > 0) {
        result.append(buffer, len);
    }
    pclose(fpipe);

    return true;
}



