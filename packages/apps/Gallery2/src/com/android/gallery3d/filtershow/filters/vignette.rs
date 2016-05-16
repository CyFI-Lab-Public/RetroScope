/*
 * Copyright (C) 2013 Unknown
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

#pragma version(1)
#pragma rs java_package_name(com.android.gallery3d.filtershow.filters)

uint32_t inputWidth;
uint32_t inputHeight;
float centerx;
float centery;
float radiusx;
float radiusy;
float strength;
float finalBright;
float finalSaturation;
float finalContrast;
float finalSubtract;
rs_matrix3x3 colorMatrix;
float scalex;
float scaley;
float offset;
static const float Rf = 0.2999f;
static const float Gf = 0.587f;
static const float Bf = 0.114f;


void setupVignetteParams() {
    int k = 0;

    scalex = 1.f / radiusx;
    scaley = 1.f / radiusy;

    float S = 1 + finalSaturation / 100.f;
    float MS = 1 - S;
    float Rt = Rf * MS;
    float Gt = Gf * MS;
    float Bt = Bf * MS;

    float b = 1 + finalBright / 100.f;
    float c = 1 + finalContrast / 100.f;
    b *= c;
    offset = .5f - c / 2.f - finalSubtract / 100.f;
    rsMatrixSet(&colorMatrix, 0, 0, b * (Rt + S));
    rsMatrixSet(&colorMatrix, 1, 0, b * Gt);
    rsMatrixSet(&colorMatrix, 2, 0, b * Bt);
    rsMatrixSet(&colorMatrix, 0, 1, b * Rt);
    rsMatrixSet(&colorMatrix, 1, 1, b * (Gt + S));
    rsMatrixSet(&colorMatrix, 2, 1, b * Bt);
    rsMatrixSet(&colorMatrix, 0, 2, b * Rt);
    rsMatrixSet(&colorMatrix, 1, 2, b * Gt);
    rsMatrixSet(&colorMatrix, 2, 2, b * (Bt + S));
}

uchar4 __attribute__((kernel)) vignette(const uchar4 in, uint32_t x,  uint32_t y) {
    float4 pixel = rsUnpackColor8888(in);
    float radx = (x - centerx) * scalex;
    float rady = (y - centery) * scaley;
    float dist = strength * (sqrt(radx * radx + rady * rady) - 1.f);
    float t  =  (1.f + dist / sqrt(1.f + dist* dist)) * .5f;
    float4 wsum = pixel;
    wsum.xyz = wsum.xyz * (1 - t) + t * (rsMatrixMultiply(&colorMatrix, wsum.xyz) + offset);
    wsum.a = 1.0f;
    uchar4 out = rsPackColorTo8888(clamp(wsum, 0.f, 1.0f));
    return out;
}