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

__inline__ int  interp(unsigned char  *src, int p , int *off ,float dr,float dg, float db){

    float fr00 = (src[p+off[0]])*(1-dr)+(src[p+off[1]])*dr;
    float fr01 = (src[p+off[2]])*(1-dr)+(src[p+off[3]])*dr;
    float fr10 = (src[p+off[4]])*(1-dr)+(src[p+off[5]])*dr;
    float fr11 = (src[p+off[6]])*(1-dr)+(src[p+off[7]])*dr;
    float frb0 = fr00 * (1-db)+fr01*db;
    float frb1 = fr10 * (1-db)+fr11*db;
    float frbg = frb0 * (1-dg)+frb1*dg;

    return (int)frbg ;
}

void JNIFUNCF(ImageFilterFx, nativeApplyFilter, jobject bitmap, jint width, jint height,
        jobject lutbitmap, jint lutwidth, jint lutheight,
        jint start, jint end)
{
    char* destination = 0;
    char* lut = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    AndroidBitmap_lockPixels(env, lutbitmap, (void**) &lut);
    unsigned char * rgb = (unsigned char * )destination;
    unsigned char * lutrgb = (unsigned char * )lut;
    int lutdim_r   = lutheight;
    int lutdim_g   = lutheight;;
    int lutdim_b   = lutwidth/lutheight;;
    int STEP = 4;

    int off[8] =  {
            0,
            STEP*1,
            STEP*lutdim_r,
            STEP*(lutdim_r + 1),
            STEP*(lutdim_r*lutdim_b),
            STEP*(lutdim_r*lutdim_b+1),
            STEP*(lutdim_r*lutdim_b+lutdim_r),
            STEP*(lutdim_r*lutdim_b+lutdim_r + 1)
    };

    float scale_R = (lutdim_r-1.f)/256.f;
    float scale_G = (lutdim_g-1.f)/256.f;
    float scale_B = (lutdim_b-1.f)/256.f;

    int i;
    for (i = start; i < end; i+= STEP)
    {
        int r = rgb[RED];
        int g = rgb[GREEN];
        int b = rgb[BLUE];

        float fb = b*scale_B;
        float fg = g*scale_G;
        float fr = r*scale_R;
        int lut_b = (int)fb;
        int lut_g = (int)fg;
        int lut_r = (int)fr;
        int p = lut_r+lut_b*lutdim_r+lut_g*lutdim_r*lutdim_b;
        p*=STEP;
        float dr = fr-lut_r;
        float dg = fg-lut_g;
        float db = fb-lut_b;
        rgb[RED]   = clamp(interp(lutrgb,p  ,off,dr,dg,db));
        rgb[GREEN] = clamp(interp(lutrgb,p+1,off,dr,dg,db));
        rgb[BLUE]  = clamp(interp(lutrgb,p+2,off,dr,dg,db));

    }

    AndroidBitmap_unlockPixels(env, bitmap);
    AndroidBitmap_unlockPixels(env, lutbitmap);
}
