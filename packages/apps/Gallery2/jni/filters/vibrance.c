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

void JNIFUNCF(ImageFilterVibrance, nativeApplyFilter, jobject bitmap, jint width, jint height,  jfloat vibrance)
{
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    int i;
    int len = width * height * 4;
    float Rf = 0.2999f;
    float Gf = 0.587f;
    float Bf = 0.114f;
    float Vib = vibrance/100.f;
    float S  = Vib+1;
    float MS = 1.0f - S;
    float Rt = Rf * MS;
    float Gt = Gf * MS;
    float Bt = Bf * MS;
    float R, G, B;
    for (i = 0; i < len; i+=4)
    {
        int r = destination[RED];
        int g = destination[GREEN];
        int b = destination[BLUE];
        float red = (r-MAX(g, b))/256.f;
        float sx = (float)(Vib/(1+exp(-red*3)));
        S = sx+1;
        MS = 1.0f - S;
        Rt = Rf * MS;
        Gt = Gf * MS;
        Bt = Bf * MS;
        int t = (r + g) / 2;
        R = r;
        G = g;
        B = b;

        float Rc = R * (Rt + S) + G * Gt + B * Bt;
        float Gc = R * Rt + G * (Gt + S) + B * Bt;
        float Bc = R * Rt + G * Gt + B * (Bt + S);

        destination[RED] = CLAMP(Rc);
        destination[GREEN] = CLAMP(Gc);
        destination[BLUE] = CLAMP(Bc);
    }
    AndroidBitmap_unlockPixels(env, bitmap);
}
