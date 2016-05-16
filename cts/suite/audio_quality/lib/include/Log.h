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

#ifndef CTSAUDIO_LOG_H
#define CTSAUDIO_LOG_H

#include <stdio.h>
#include <iostream>
#include <fstream>

#include "FileUtil.h"

class Log: public FileUtil {
public:
    enum LogLevel {
        ELogV = 0,
        ELogD = 1,
        ELogI = 2,
        ELogW = 3,
        ELogE = 4
    };

    static Log* Instance(const char* dirName = NULL);
    static void Finalize();


    void printf(LogLevel level, const char* fmt, ...);
    void setLogLevel(LogLevel level);
    LogLevel getLogLevel() {
        return mLogLevel;
    };
private:
    Log();
    virtual ~Log();
    virtual bool init(const char* dirName);

private:
    static Log* mInstance;
    LogLevel mLogLevel;
};

#define LOGE(x...) do { Log::Instance()->printf(Log::ELogE, x); \
    Log::Instance()->printf(Log::ELogE, "  file %s line %d", __FILE__, __LINE__); } while(0)
#define LOGW(x...) do { Log::Instance()->printf(Log::ELogW, x); } while(0)
#define LOGI(x...) do { Log::Instance()->printf(Log::ELogI, x); } while(0)
#define LOGD(x...) do { Log::Instance()->printf(Log::ELogD, x); } while(0)
#define LOGV(x...) do { Log::Instance()->printf(Log::ELogV, x); } while(0)

#define MSG(x...) do { Log::Instance()->printf(Log::ELogE, x); } while(0)

#define ASSERT(cond) if(!(cond)) {  Log::Instance()->printf(Log::ELogE, \
        "assertion failed %s %d", __FILE__, __LINE__); \
    Log::Finalize(); \
    *(char*)0 = 0; /* this will crash */};

#endif // CTSAUDIO_LOG_H
