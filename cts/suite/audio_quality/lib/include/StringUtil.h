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


#ifndef CTSAUDIO_STRINGUTIL_H
#define CTSAUDIO_STRINGUTIL_H

#include <utils/String8.h>
#include <vector>

/**
 * Utility class for implementing missing features from android::String8
 */
class StringUtil {
public:
    /// split the given string with given delimiter and return the vector of string
    /// it may return NULL if memory alloc fails.
    /// If vector is not NULL, there will be at least one string
    static std::vector<android::String8>* split(const android::String8& str, char delimiter);
    /// This function will return zero length string if pos is invalid.
    static android::String8 substr(const android::String8& str, size_t pos, size_t n);
    static int compare(const android::String8& str, const char* other);
    static bool endsWith(const android::String8& str, const char* other);
};


#endif // CTSAUDIO_STRINGUTIL_H
