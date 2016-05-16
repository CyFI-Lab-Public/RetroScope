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

#include <math.h>
#include "filters.h"

unsigned char clamp(int c)
{
    int N = 255;
    c &= ~(c >> 31);
    c -= N;
    c &= (c >> 31);
    c += N;
    return  (unsigned char) c;
}

int clampMax(int c,int max)
{
    c &= ~(c >> 31);
    c -= max;
    c &= (c >> 31);
    c += max;
    return  c;
}

void JNIFUNCF(ImageFilterContrast, nativeApplyFilter, jobject bitmap, jint width, jint height, jfloat bright)
{
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    unsigned char * rgb = (unsigned char * )destination;
    int i;
    int len = width * height * 4;
    float m =  (float)pow(2, bright/100.);
    float c =  127-m*127;

    for (i = 0; i < len; i+=4) {
        rgb[RED]   = clamp((int)(m*rgb[RED]+c));
        rgb[GREEN] = clamp((int)(m*rgb[GREEN]+c));
        rgb[BLUE]  = clamp((int)(m*rgb[BLUE]+c));
    }
    AndroidBitmap_unlockPixels(env, bitmap);
}

