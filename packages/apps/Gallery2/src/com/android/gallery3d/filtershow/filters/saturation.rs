/*
 * Copyright (C) 2012 Unknown
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

#define MAX_CHANELS 7
#define MAX_HUE 4096
static const int ABITS = 4;
static const int HSCALE = 256;
static const int k1=255 << ABITS;
static const int k2=HSCALE << ABITS;

static const float Rf = 0.2999f;
static const float Gf = 0.587f;
static const float Bf = 0.114f;

rs_matrix3x3 colorMatrix_min;
rs_matrix3x3 colorMatrix_max;

int mNumberOfLines;
// input data
int saturation[MAX_CHANELS];
float sat[MAX_CHANELS];

float satLut[MAX_HUE];
// generated data


void setupGradParams() {

    int master = saturation[0];
    int max = master+saturation[1];
    int min = max;

    // calculate the minimum and maximum saturation
    for (int i = 1; i < MAX_CHANELS; i++) {
       int v = master+saturation[i];
       if (max < v) {
         max = v;
       }
       else if (min > v) {
         min = v;
       }
    }
    // generate a lookup table for all hue 0 to 4K  which goes from 0 to 1 0=min sat 1 = max sat
    min = min - 1;
    for(int i = 0; i < MAX_HUE ; i++) {
       float p =  i * 6 / (float)MAX_HUE;
       int ip = ((int)(p + .5f)) % 6;
       int v = master + saturation[ip + 1];
       satLut[i] = (v - min)/(float)(max - min);
    }

    float S = 1 + max / 100.f;
    float MS = 1 - S;
    float Rt = Rf * MS;
    float Gt = Gf * MS;
    float Bt = Bf * MS;
    float b = 1.f;

    // Generate 2 color matrix one at min sat and one at max
    rsMatrixSet(&colorMatrix_max, 0, 0, b * (Rt + S));
    rsMatrixSet(&colorMatrix_max, 1, 0, b * Gt);
    rsMatrixSet(&colorMatrix_max, 2, 0, b * Bt);
    rsMatrixSet(&colorMatrix_max, 0, 1, b * Rt);
    rsMatrixSet(&colorMatrix_max, 1, 1, b * (Gt + S));
    rsMatrixSet(&colorMatrix_max, 2, 1, b * Bt);
    rsMatrixSet(&colorMatrix_max, 0, 2, b * Rt);
    rsMatrixSet(&colorMatrix_max, 1, 2, b * Gt);
    rsMatrixSet(&colorMatrix_max, 2, 2, b * (Bt + S));

    S = 1 + min / 100.f;
    MS = 1-S;
    Rt = Rf * MS;
    Gt = Gf * MS;
    Bt = Bf * MS;
    b = 1;

    rsMatrixSet(&colorMatrix_min, 0, 0, b * (Rt + S));
    rsMatrixSet(&colorMatrix_min, 1, 0, b * Gt);
    rsMatrixSet(&colorMatrix_min, 2, 0, b * Bt);
    rsMatrixSet(&colorMatrix_min, 0, 1, b * Rt);
    rsMatrixSet(&colorMatrix_min, 1, 1, b * (Gt + S));
    rsMatrixSet(&colorMatrix_min, 2, 1, b * Bt);
    rsMatrixSet(&colorMatrix_min, 0, 2, b * Rt);
    rsMatrixSet(&colorMatrix_min, 1, 2, b * Gt);
    rsMatrixSet(&colorMatrix_min, 2, 2, b * (Bt + S));
}

static ushort rgb2hue( uchar4 rgb)
{
    int iMin,iMax,chroma;

    int ri = rgb.r;
    int gi = rgb.g;
    int bi = rgb.b;
    short rv,rs,rh;

    if (ri > gi) {
        iMax = max (ri, bi);
        iMin = min (gi, bi);
    } else {
        iMax = max (gi, bi);
        iMin = min (ri, bi);
    }

    rv = (short) (iMax << ABITS);

    if (rv == 0) {
        return 0;
    }

    chroma = iMax - iMin;
    rs = (short) ((k1 * chroma) / iMax);
    if (rs == 0) {
        return 0;
    }

    if ( ri == iMax ) {
        rh  = (short) ((k2 * (6 * chroma + gi - bi))/(6 * chroma));
        if (rh >= k2) {
           rh -= k2;
        }
        return rh;
    }

    if (gi  == iMax) {
        return(short) ((k2 * (2 * chroma + bi - ri)) / (6 * chroma));
    }

    return (short) ((k2 * (4 * chroma + ri - gi)) / (6 * chroma));
}

uchar4 __attribute__((kernel)) selectiveAdjust(const uchar4 in, uint32_t x,
    uint32_t y) {
    float4 pixel = rsUnpackColor8888(in);

    float4 wsum = pixel;
    int hue = rgb2hue(in);

    float t = satLut[hue];
        pixel.xyz = rsMatrixMultiply(&colorMatrix_min ,pixel.xyz) * (1 - t) +
            t * (rsMatrixMultiply(&colorMatrix_max ,pixel.xyz));

    pixel.a = 1.0f;
    return rsPackColorTo8888(clamp(pixel, 0.f, 1.0f));
}