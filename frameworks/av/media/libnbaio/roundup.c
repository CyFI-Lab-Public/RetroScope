/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include <media/nbaio/roundup.h>

unsigned roundup(unsigned v)
{
    // __builtin_clz is undefined for zero input
    if (v == 0) {
        v = 1;
    }
    int lz = __builtin_clz((int) v);
    unsigned rounded = ((unsigned) 0x80000000) >> lz;
    // 0x800000001 and higher are actually rounded _down_ to prevent overflow
    if (v > rounded && lz > 0) {
        rounded <<= 1;
    }
    return rounded;
}
