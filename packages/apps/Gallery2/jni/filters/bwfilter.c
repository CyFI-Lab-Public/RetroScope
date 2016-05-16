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

void JNIFUNCF(ImageFilterBwFilter, nativeApplyFilter, jobject bitmap, jint width, jint height, jint rw, jint gw, jint bw)
{
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    unsigned char * rgb = (unsigned char * )destination;
    float sr = rw;
    float sg = gw;
    float sb = bw;

    float min = MIN(sg,sb);
    min = MIN(sr,min);
    float max =  MAX(sg,sb);
    max = MAX(sr,max);
    float avg = (min+max)/2;
    sb /= avg;
    sg /= avg;
    sr /= avg;
    int i;
    int len = width * height * 4;

    for (i = 0; i < len; i+=4)
    {
        float r = sr *rgb[RED];
        float g = sg *rgb[GREEN];
        float b = sb *rgb[BLUE];
        min = MIN(g,b);
        min = MIN(r,min);
        max = MAX(g,b);
        max = MAX(r,max);
        avg =(min+max)/2;
        rgb[RED]   = CLAMP(avg);
        rgb[GREEN] = rgb[RED];
        rgb[BLUE]  = rgb[RED];
    }
    AndroidBitmap_unlockPixels(env, bitmap);
}
