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
#include <time.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>

#include "Log.h"
#include "Settings.h"
#include "StringUtil.h"
#include "FileUtil.h"


// This class is used by Log. So we cannot use LOG? macros here.
#define _LOGD_(x...) do { fprintf(stderr, x); fprintf(stderr, "\n"); } while(0)

// reported generated under reports/YYYY_MM_DD_HH_MM_SS dir
const char reportTopDir[] = "reports";
android::String8 FileUtil::mDirPath;

bool FileUtil::prepare(android::String8& dirPath)
{
    if (mDirPath.length() != 0) {
        dirPath = mDirPath;
        _LOGD_("mDirPath %s", mDirPath.string());
        return true;
    }

    time_t timeNow = time(NULL);
    if (timeNow == ((time_t)-1)) {
        _LOGD_("time error");
       return false;
    }
    // tm is allocated in static buffer, and should not be freed.
    struct tm* tm = localtime(&timeNow);
    if (tm == NULL) {
        _LOGD_("localtime error");
        return false;
    }
    int result = mkdir(reportTopDir, S_IRWXU);
    if ((result == -1) && (errno != EEXIST)) {
        _LOGD_("mkdir of topdir failed, error %d", errno);
        return false;
    }
    android::String8 reportTime;
    if (reportTime.appendFormat("%04d_%02d_%02d_%02d_%02d_%02d", tm->tm_year + 1900,
                tm->tm_mon + 1, tm->tm_mday, tm->tm_hour, tm->tm_min, tm->tm_sec) != 0) {
            return false;
    }
    Settings::Instance()->addSetting(Settings::EREPORT_TIME, reportTime);
    android::String8 path;
    if (path.appendFormat("%s/%s", reportTopDir, reportTime.string()) != 0) {
        return false;
    }
    result = mkdir(path.string(), S_IRWXU);
    if ((result == -1) && (errno != EEXIST)) {
        _LOGD_("mkdir of report dir failed, error %d", errno);
        return false;
    }
    mDirPath = path;
    dirPath = path;

    return true;
}

FileUtil::FileUtil()
{
    mBuffer = new char[DEFAULT_BUFFER_SIZE];
    if (mBuffer == NULL) {
        // cannot use ASSERT here, just crash
        *(char*)0 = 0;
    }
    mBufferSize = DEFAULT_BUFFER_SIZE;
}

FileUtil::~FileUtil()
{
    if (mFile.is_open()) {
        mFile.close();
    }
    delete[] mBuffer;
}

bool FileUtil::init(const char* fileName)
{
    if (fileName == NULL) {
        return true;
    }

    mFile.open(fileName, std::ios::out | std::ios::trunc);
    if (!mFile.is_open()) {
            return false;
        }
    return true;
}

bool FileUtil::doVprintf(bool fileOnly, int logLevel, const char *fmt, va_list ap)
{
    // prevent messed up log in multi-thread env. Still multi-line logs can be messed up.
    android::Mutex::Autolock lock(mWriteLock);
    while (1) {
        int start = 0;
        if (logLevel != -1) {
            mBuffer[0] = '0' + logLevel;
            mBuffer[1] = '>';
            start = 2;
        }
        int size;
        size = vsnprintf(mBuffer + start, mBufferSize - start - 2, fmt, ap); // 2 for \n\0
        if (size < 0) {
            fprintf(stderr, "FileUtil::vprintf failed");
            return false;
        }
        if ((size + start + 2) > mBufferSize) {
            //default buffer does not fit, increase buffer size and retry
            delete[] mBuffer;
            mBuffer = new char[2 * size];
            if (mBuffer == NULL) {
                // cannot use ASSERT here, just crash
                *(char*)0 = 0;
            }
            mBufferSize = 2 * size;
            // re-try
            continue;
        }
        size += start;
        mBuffer[size] = '\n';
        size++;
        mBuffer[size] = 0;

        if (!fileOnly) {
            fprintf(stdout, "%s", mBuffer);
        }
        if (mFile.is_open()) {
            mFile<<mBuffer;
        }
        return true;
    }
}

bool FileUtil::doPrintf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    bool result = doVprintf(false, -1, fmt, ap);
    va_end(ap);
    return result;
}
