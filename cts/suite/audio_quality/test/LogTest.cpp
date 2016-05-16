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

#include <stdint.h>
#include <gtest/gtest.h>

#include <UniquePtr.h>

#include "Log.h"



class LogTest : public testing::Test {
public:

};


TEST_F(LogTest, logTest) {
    Log::LogLevel level = Log::Instance()->getLogLevel();

    // following lines should match. no automatic test yet..
    // TODO make it automatic?
    Log::Instance()->setLogLevel(Log::ELogV);
    printf("printf %d %d %d %d %d %d\n", 0, 1, 2, 3, 4, 5);
    LOGD(  "logd   %d %d %d %d %d %d", 0, 1, 2, 3, 4, 5);
    LOGV(  "logv   %d %d %d %d %d %d", 0, 1, 2, 3, 4, 5);
    LOGI(  "logi   %d %d %d %d %d %d", 0, 1, 2, 3, 4, 5);
    LOGW(  "logw   %d %d %d %d %d %d", 0, 1, 2, 3, 4, 5);
    LOGE(  "loge   %d %d %d %d %d %d", 0, 1, 2, 3, 4, 5);

    int64_t a = 0;
    int64_t b = 1;
    int64_t c = 2;
    int64_t d = 3;
    int64_t e = 4;
    int64_t f = 5;
    printf("printf %lld %lld %lld %lld %lld %lld\n", a, b, c, d, e, f);
    LOGD(  "logd   %lld %lld %lld %lld %lld %lld", a, b, c, d, e, f);
    LOGV(  "logv   %lld %lld %lld %lld %lld %lld", a, b, c, d, e, f);
    LOGI(  "logi   %lld %lld %lld %lld %lld %lld", a, b, c, d, e, f);
    LOGW(  "logw   %lld %lld %lld %lld %lld %lld", a, b, c, d, e, f);
    LOGE(  "loge   %lld %lld %lld %lld %lld %lld", a, b, c, d, e, f);

    Log::Instance()->setLogLevel(level);
}



