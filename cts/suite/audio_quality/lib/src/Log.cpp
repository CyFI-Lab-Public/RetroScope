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
#include <stdio.h>
#include <stdarg.h>

#include "StringUtil.h"
#include "Log.h"

Log* Log::mInstance = NULL;

#define ASSERT_PLAIN(cond) if(!(cond)) { fprintf(stderr, \
        "assertion failed %s %d", __FILE__, __LINE__); \
    *(char*)0 = 0; /* this will crash */};

Log* Log::Instance(const char* dirName)
{
    if (!mInstance) {
        mInstance = new Log();
        ASSERT_PLAIN(mInstance->init(dirName));
    }
    return mInstance;
}
void Log::Finalize()
{
    delete mInstance;
    mInstance = NULL;
}
void Log::printf(LogLevel level, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    FileUtil::doVprintf(level < mLogLevel, level, fmt, ap);
    va_end(ap);
}

void Log::setLogLevel(LogLevel level)
{
    mLogLevel = level;
}

Log::Log()
    : mLogLevel(ELogV)
{
    ::fprintf(stderr, "Log level %d\n", mLogLevel);
}

Log::~Log()
{

}

bool Log::init(const char* dirName)
{
    if (dirName == NULL) {
        return true;
    }
    android::String8 logFile;
    if (logFile.appendFormat("%s/log.txt", dirName) != 0) {
        return false;
    }
    return FileUtil::init(logFile.string());
}



