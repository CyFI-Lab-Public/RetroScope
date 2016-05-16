/*
 * Copyright 2012, The Android Open Source Project
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

#ifndef ANDROID_FIXUP_H
#define ANDROID_FIXUP_H

#include <unistd.h>

typedef __uint16_t __u16;
typedef __uint32_t __u32;
typedef __uint64_t __u64;
typedef __int64_t  __s64;

#define strndup(str, size) strdup(str)

#endif /* ANDROID_FIXUP_H */
