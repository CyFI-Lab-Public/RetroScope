/*
 * Copyright 2013, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef BCC_SUPPORT_PROPERTIES_H
#define BCC_SUPPORT_PROPERTIES_H

#include <stdint.h>
#include <stdlib.h>

#if !defined(RS_SERVER) && defined(HAVE_ANDROID_OS)
#include <cutils/properties.h>
#endif

static inline uint32_t getProperty(const char *str) {
#if !defined(RS_SERVER) && defined(HAVE_ANDROID_OS)
    char buf[PROPERTY_VALUE_MAX];
    property_get(str, buf, "0");
    return atoi(buf);
#else
    return 0;
#endif
}

#endif // BCC_SUPPORT_PROPERTIES_H
