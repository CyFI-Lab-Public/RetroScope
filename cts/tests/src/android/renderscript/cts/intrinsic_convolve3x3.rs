/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include "shared.rsh"

int32_t gWidth;
int32_t gHeight;
rs_allocation gIn;

float gCoeffs[9];

uchar4 __attribute__((kernel)) convolve_U4(uint32_t x, uint32_t y) {
    uint32_t x1 = min((int32_t)x+1, gWidth-1);
    uint32_t x2 = max((int32_t)x-1, 0);
    uint32_t y1 = min((int32_t)y+1, gHeight-1);
    uint32_t y2 = max((int32_t)y-1, 0);

    float4 p00 = convert_float4(rsGetElementAt_uchar4(gIn, x1, y1));
    float4 p01 = convert_float4(rsGetElementAt_uchar4(gIn, x, y1));
    float4 p02 = convert_float4(rsGetElementAt_uchar4(gIn, x2, y1));
    float4 p10 = convert_float4(rsGetElementAt_uchar4(gIn, x1, y));
    float4 p11 = convert_float4(rsGetElementAt_uchar4(gIn, x, y));
    float4 p12 = convert_float4(rsGetElementAt_uchar4(gIn, x2, y));
    float4 p20 = convert_float4(rsGetElementAt_uchar4(gIn, x1, y2));
    float4 p21 = convert_float4(rsGetElementAt_uchar4(gIn, x, y2));
    float4 p22 = convert_float4(rsGetElementAt_uchar4(gIn, x2, y2));
    p00 *= gCoeffs[0];
    p01 *= gCoeffs[1];
    p02 *= gCoeffs[2];
    p10 *= gCoeffs[3];
    p11 *= gCoeffs[4];
    p12 *= gCoeffs[5];
    p20 *= gCoeffs[6];
    p21 *= gCoeffs[7];
    p22 *= gCoeffs[8];

    p00 += p01;
    p02 += p10;
    p11 += p12;
    p20 += p21;

    p22 += p00;
    p02 += p11;

    p20 += p22;
    p20 += p02;
    p20 += 0.5f;

    p20 = clamp(p20, 0.f, 255.f);
    return convert_uchar4(p20);
}

uchar3 __attribute__((kernel)) convolve_U3(uint32_t x, uint32_t y) {
    uint32_t x1 = min((int32_t)x+1, gWidth-1);
    uint32_t x2 = max((int32_t)x-1, 0);
    uint32_t y1 = min((int32_t)y+1, gHeight-1);
    uint32_t y2 = max((int32_t)y-1, 0);

    float3 p00 = convert_float3(rsGetElementAt_uchar3(gIn, x1, y1));
    float3 p01 = convert_float3(rsGetElementAt_uchar3(gIn, x, y1));
    float3 p02 = convert_float3(rsGetElementAt_uchar3(gIn, x2, y1));
    float3 p10 = convert_float3(rsGetElementAt_uchar3(gIn, x1, y));
    float3 p11 = convert_float3(rsGetElementAt_uchar3(gIn, x, y));
    float3 p12 = convert_float3(rsGetElementAt_uchar3(gIn, x2, y));
    float3 p20 = convert_float3(rsGetElementAt_uchar3(gIn, x1, y2));
    float3 p21 = convert_float3(rsGetElementAt_uchar3(gIn, x, y2));
    float3 p22 = convert_float3(rsGetElementAt_uchar3(gIn, x2, y2));
    p00 *= gCoeffs[0];
    p01 *= gCoeffs[1];
    p02 *= gCoeffs[2];
    p10 *= gCoeffs[3];
    p11 *= gCoeffs[4];
    p12 *= gCoeffs[5];
    p20 *= gCoeffs[6];
    p21 *= gCoeffs[7];
    p22 *= gCoeffs[8];

    p00 += p01;
    p02 += p10;
    p11 += p12;
    p20 += p21;

    p22 += p00;
    p02 += p11;

    p20 += p22;
    p20 += p02;
    p20 += 0.5f;

    p20 = clamp(p20, 0.f, 255.f);
    return convert_uchar3(p20);
}

uchar2 __attribute__((kernel)) convolve_U2(uint32_t x, uint32_t y) {
    uint32_t x1 = min((int32_t)x+1, gWidth-1);
    uint32_t x2 = max((int32_t)x-1, 0);
    uint32_t y1 = min((int32_t)y+1, gHeight-1);
    uint32_t y2 = max((int32_t)y-1, 0);

    float2 p00 = convert_float2(rsGetElementAt_uchar2(gIn, x1, y1));
    float2 p01 = convert_float2(rsGetElementAt_uchar2(gIn, x, y1));
    float2 p02 = convert_float2(rsGetElementAt_uchar2(gIn, x2, y1));
    float2 p10 = convert_float2(rsGetElementAt_uchar2(gIn, x1, y));
    float2 p11 = convert_float2(rsGetElementAt_uchar2(gIn, x, y));
    float2 p12 = convert_float2(rsGetElementAt_uchar2(gIn, x2, y));
    float2 p20 = convert_float2(rsGetElementAt_uchar2(gIn, x1, y2));
    float2 p21 = convert_float2(rsGetElementAt_uchar2(gIn, x, y2));
    float2 p22 = convert_float2(rsGetElementAt_uchar2(gIn, x2, y2));
    p00 *= gCoeffs[0];
    p01 *= gCoeffs[1];
    p02 *= gCoeffs[2];
    p10 *= gCoeffs[3];
    p11 *= gCoeffs[4];
    p12 *= gCoeffs[5];
    p20 *= gCoeffs[6];
    p21 *= gCoeffs[7];
    p22 *= gCoeffs[8];

    p00 += p01;
    p02 += p10;
    p11 += p12;
    p20 += p21;

    p22 += p00;
    p02 += p11;

    p20 += p22;
    p20 += p02;
    p20 += 0.5f;

    p20 = clamp(p20, 0.f, 255.f);
    return convert_uchar2(p20);
}

uchar __attribute__((kernel)) convolve_U1(uint32_t x, uint32_t y) {
    uint32_t x1 = min((int32_t)x+1, gWidth-1);
    uint32_t x2 = max((int32_t)x-1, 0);
    uint32_t y1 = min((int32_t)y+1, gHeight-1);
    uint32_t y2 = max((int32_t)y-1, 0);

    float p00 = rsGetElementAt_uchar(gIn, x1, y1);
    float p01 = rsGetElementAt_uchar(gIn, x, y1);
    float p02 = rsGetElementAt_uchar(gIn, x2, y1);
    float p10 = rsGetElementAt_uchar(gIn, x1, y);
    float p11 = rsGetElementAt_uchar(gIn, x, y);
    float p12 = rsGetElementAt_uchar(gIn, x2, y);
    float p20 = rsGetElementAt_uchar(gIn, x1, y2);
    float p21 = rsGetElementAt_uchar(gIn, x, y2);
    float p22 = rsGetElementAt_uchar(gIn, x2, y2);
    p00 *= gCoeffs[0];
    p01 *= gCoeffs[1];
    p02 *= gCoeffs[2];
    p10 *= gCoeffs[3];
    p11 *= gCoeffs[4];
    p12 *= gCoeffs[5];
    p20 *= gCoeffs[6];
    p21 *= gCoeffs[7];
    p22 *= gCoeffs[8];

    p00 += p01;
    p02 += p10;
    p11 += p12;
    p20 += p21;

    p22 += p00;
    p02 += p11;

    p20 += p22;
    p20 += p02;
    p20 += 0.5f;

    p20 = clamp(p20, 0.f, 255.f);
    return (uchar)p20;
}

float4 __attribute__((kernel)) convolve_F4(uint32_t x, uint32_t y) {
    uint32_t x1 = min((int32_t)x+1, gWidth-1);
    uint32_t x2 = max((int32_t)x-1, 0);
    uint32_t y1 = min((int32_t)y+1, gHeight-1);
    uint32_t y2 = max((int32_t)y-1, 0);

    float4 p00 = rsGetElementAt_float4(gIn, x1, y1) * gCoeffs[0];
    float4 p01 = rsGetElementAt_float4(gIn, x, y1) * gCoeffs[1];
    float4 p02 = rsGetElementAt_float4(gIn, x2, y1) * gCoeffs[2];
    float4 p10 = rsGetElementAt_float4(gIn, x1, y) * gCoeffs[3];
    float4 p11 = rsGetElementAt_float4(gIn, x, y) * gCoeffs[4];
    float4 p12 = rsGetElementAt_float4(gIn, x2, y) * gCoeffs[5];
    float4 p20 = rsGetElementAt_float4(gIn, x1, y2) * gCoeffs[6];
    float4 p21 = rsGetElementAt_float4(gIn, x, y2) * gCoeffs[7];
    float4 p22 = rsGetElementAt_float4(gIn, x2, y2) * gCoeffs[8];

    p00 += p01;
    p02 += p10;
    p11 += p12;
    p20 += p21;

    p22 += p00;
    p02 += p11;

    p20 += p22;
    p20 += p02;
    return p20;
}

float3 __attribute__((kernel)) convolve_F3(uint32_t x, uint32_t y) {
    uint32_t x1 = min((int32_t)x+1, gWidth-1);
    uint32_t x2 = max((int32_t)x-1, 0);
    uint32_t y1 = min((int32_t)y+1, gHeight-1);
    uint32_t y2 = max((int32_t)y-1, 0);

    float3 p00 = rsGetElementAt_float3(gIn, x1, y1) * gCoeffs[0];
    float3 p01 = rsGetElementAt_float3(gIn, x, y1) * gCoeffs[1];
    float3 p02 = rsGetElementAt_float3(gIn, x2, y1) * gCoeffs[2];
    float3 p10 = rsGetElementAt_float3(gIn, x1, y) * gCoeffs[3];
    float3 p11 = rsGetElementAt_float3(gIn, x, y) * gCoeffs[4];
    float3 p12 = rsGetElementAt_float3(gIn, x2, y) * gCoeffs[5];
    float3 p20 = rsGetElementAt_float3(gIn, x1, y2) * gCoeffs[6];
    float3 p21 = rsGetElementAt_float3(gIn, x, y2) * gCoeffs[7];
    float3 p22 = rsGetElementAt_float3(gIn, x2, y2) * gCoeffs[8];

    p00 += p01;
    p02 += p10;
    p11 += p12;
    p20 += p21;

    p22 += p00;
    p02 += p11;

    p20 += p22;
    p20 += p02;
    return p20;
}

float2 __attribute__((kernel)) convolve_F2(uint32_t x, uint32_t y) {
    uint32_t x1 = min((int32_t)x+1, gWidth-1);
    uint32_t x2 = max((int32_t)x-1, 0);
    uint32_t y1 = min((int32_t)y+1, gHeight-1);
    uint32_t y2 = max((int32_t)y-1, 0);

    float2 p00 = rsGetElementAt_float2(gIn, x1, y1) * gCoeffs[0];
    float2 p01 = rsGetElementAt_float2(gIn, x, y1) * gCoeffs[1];
    float2 p02 = rsGetElementAt_float2(gIn, x2, y1) * gCoeffs[2];
    float2 p10 = rsGetElementAt_float2(gIn, x1, y) * gCoeffs[3];
    float2 p11 = rsGetElementAt_float2(gIn, x, y) * gCoeffs[4];
    float2 p12 = rsGetElementAt_float2(gIn, x2, y) * gCoeffs[5];
    float2 p20 = rsGetElementAt_float2(gIn, x1, y2) * gCoeffs[6];
    float2 p21 = rsGetElementAt_float2(gIn, x, y2) * gCoeffs[7];
    float2 p22 = rsGetElementAt_float2(gIn, x2, y2) * gCoeffs[8];

    p00 += p01;
    p02 += p10;
    p11 += p12;
    p20 += p21;

    p22 += p00;
    p02 += p11;

    p20 += p22;
    p20 += p02;
    return p20;
}

float __attribute__((kernel)) convolve_F1(uint32_t x, uint32_t y) {
    uint32_t x1 = min((int32_t)x+1, gWidth-1);
    uint32_t x2 = max((int32_t)x-1, 0);
    uint32_t y1 = min((int32_t)y+1, gHeight-1);
    uint32_t y2 = max((int32_t)y-1, 0);

    float p00 = rsGetElementAt_float(gIn, x1, y1) * gCoeffs[0];
    float p01 = rsGetElementAt_float(gIn, x, y1) * gCoeffs[1];
    float p02 = rsGetElementAt_float(gIn, x2, y1) * gCoeffs[2];
    float p10 = rsGetElementAt_float(gIn, x1, y) * gCoeffs[3];
    float p11 = rsGetElementAt_float(gIn, x, y) * gCoeffs[4];
    float p12 = rsGetElementAt_float(gIn, x2, y) * gCoeffs[5];
    float p20 = rsGetElementAt_float(gIn, x1, y2) * gCoeffs[6];
    float p21 = rsGetElementAt_float(gIn, x, y2) * gCoeffs[7];
    float p22 = rsGetElementAt_float(gIn, x2, y2) * gCoeffs[8];

    p00 += p01;
    p02 += p10;
    p11 += p12;
    p20 += p21;

    p22 += p00;
    p02 += p11;

    p20 += p22;
    p20 += p02;
    return p20;
}


