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

#include "ip.rsh"
#pragma rs_fp_relaxed

rs_allocation gBlur;

static float gOverWm1;
static float gOverHm1;
static uchar gLutR[256];
static uchar gLutG[256];
static uchar gLutB[256];

void setup() {
    int w = rsAllocationGetDimX(gBlur);
    int h = rsAllocationGetDimY(gBlur);
    gOverWm1 = 1.f / w;
    gOverHm1 = 1.f / h;

    for (int x=0; x < 256; x++) {
        gLutR[x] = x;//255-x;
        gLutG[x] = x;//255-x;
        gLutB[x] = x;//255-x;
    }
}

uchar4 __attribute__((kernel)) process(uchar4 in, uint32_t x, uint32_t y) {
    float2 xyDist;
    xyDist.x = (x * gOverWm1 - 0.5f);
    xyDist.y = (y * gOverHm1 - 0.5f);

    // color
    float4 v1 = rsUnpackColor8888(in);
    float4 v2 = rsUnpackColor8888(rsGetElementAt_uchar4(gBlur, x, y));

    float dist = dot(xyDist, xyDist) * 1.4f;
    float pdist = native_powr(dist, 2.7f * 0.5f);
    //float pdist = powr(dist, 2.7f * 0.5f);

    pdist = clamp(pdist, 0.f, 1.f);
    v1 = mix(v1, v2, dist * 2.f);
    v1 *= 1.f - pdist;

    // apply curve
    uchar4 out = rsPackColorTo8888(v1);

    out.r = gLutR[out.r];
    out.g = gLutG[out.g];
    out.b = gLutB[out.b];
    return out;
}


