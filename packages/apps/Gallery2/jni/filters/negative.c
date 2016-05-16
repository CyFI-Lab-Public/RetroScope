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

#include "filters.h"

void JNIFUNCF(ImageFilterNegative, nativeApplyFilter, jobject bitmap, jint width, jint height)
{
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);

    int tot_len = height * width * 4;
    int i;
    char * dst = destination;
    for (i = 0; i < tot_len; i+=4) {
        dst[RED] = 255 - dst[RED];
        dst[GREEN] = 255 - dst[GREEN];
        dst[BLUE] = 255 - dst[BLUE];
    }
    AndroidBitmap_unlockPixels(env, bitmap);
}
