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
#include <stdlib.h>

#include "Log.h"
#include "StringUtil.h"

std::vector<android::String8>* StringUtil::split(const android::String8& str, char delimiter)
{
    std::vector<android::String8>* tokens = new std::vector<android::String8>();
    unsigned int lastTokenEnd = 0;
    for (unsigned int i = 0; i < str.length(); i++) {
        if (str[i] == delimiter) {
            if ((i - lastTokenEnd) > 0) {
                tokens->push_back(substr(str, lastTokenEnd, i - lastTokenEnd));
            }
            lastTokenEnd = i + 1; // 1 for skipping delimiter
        }
    }
    if (lastTokenEnd < str.length()) {
        tokens->push_back(substr(str, lastTokenEnd, str.length() - lastTokenEnd));
    }
    return tokens;
}

android::String8 StringUtil::substr(const android::String8& str, size_t pos, size_t n)
{
    size_t l = str.length();

    if (pos >= l) {
        android::String8 resultDummy;
        return resultDummy;
    }
    if ((pos + n) > l) {
        n = l - pos;
    }
    android::String8 result(str.string() + pos, n);
    return result;
}

int StringUtil::compare(const android::String8& str, const char* other)
{
    return strcmp(str.string(), other);
}

bool StringUtil::endsWith(const android::String8& str, const char* other)
{
    size_t l1 = str.length();
    size_t l2 = strlen(other);
    const char* data = str.string();
    if (l2 > l1) {
        return false;
    }
    size_t iStr = l1 - l2;
    size_t iOther = 0;
    for(; iStr < l1; iStr++) {
        if (data[iStr] != other[iOther]) {
            return false;
        }
        iOther++;
    }
    return true;
}
