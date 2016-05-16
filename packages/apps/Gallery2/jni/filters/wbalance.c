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

void estmateWhite(unsigned char *src, int len, int *wr, int *wb, int *wg){

    int STEP = 4;
    int RANGE = 256;
    int *histR = (int *) malloc(256*sizeof(int));
    int *histG = (int *) malloc(256*sizeof(int));
    int *histB = (int *) malloc(256*sizeof(int));
    int i;
    for (i = 0; i < 255; i++) {
        histR[i] = histG[i] = histB[i] =0;
    }

    for (i = 0; i < len; i+=STEP) {
        histR[(src[RED])]++;
        histG[(src[GREEN])]++;
        histB[(src[BLUE])]++;
    }
    int min_r = -1, min_g = -1,min_b = -1;
    int max_r = 0, max_g = 0,max_b = 0;
    int sum_r = 0,sum_g=0,sum_b=0;

    for (i = 1; i < RANGE-1; i++) {
        int r = histR[i];
        int g = histG[i];
        int b = histB[i];
        sum_r += r;
        sum_g += g;
        sum_b += b;

        if (r>0){
            if (min_r < 0) min_r = i;
            max_r = i;
        }
        if (g>0){
            if (min_g < 0) min_g = i;
            max_g = i;
        }
        if (b>0){
            if (min_b < 0) min_b = i;
            max_b = i;
        }
    }

    int sum15r = 0,sum15g=0,sum15b=0;
    int count15r = 0,count15g=0,count15b=0;
    int tmp_r = 0,tmp_g=0,tmp_b=0;

    for (i = RANGE-2; i >0; i--) {
        int r = histR[i];
        int g = histG[i];
        int b = histB[i];
        tmp_r += r;
        tmp_g += g;
        tmp_b += b;

        if ((tmp_r > sum_r/20) && (tmp_r < sum_r/5)) {
            sum15r += r*i;
            count15r += r;
        }
        if ((tmp_g > sum_g/20) && (tmp_g < sum_g/5)) {
            sum15g += g*i;
            count15g += g;
        }
        if ((tmp_b > sum_b/20) && (tmp_b < sum_b/5)) {
            sum15b += b*i;
            count15b += b;
        }

    }
    free(histR);
    free(histG);
    free(histB);

    if ((count15r>0) && (count15g>0) && (count15b>0) ){
        *wr = sum15r/count15r;
        *wb = sum15g/count15g;
        *wg = sum15b/count15b;
    }else {
        *wg  = *wb = *wr=255;
    }
}

void estmateWhiteBox(unsigned char *src, int iw, int ih, int x,int y, int *wr, int *wb, int *wg){
    int r;
    int g;
    int b;
    int sum;
    int xp,yp;
    int bounds = 5;
    if (x<0) x = bounds;
    if (y<0) y = bounds;
    if (x>=(iw-bounds)) x = (iw-bounds-1);
    if (y>=(ih-bounds)) y = (ih-bounds-1);
    int startx = x - bounds;
    int starty = y - bounds;
    int endx = x + bounds;
    int endy = y + bounds;

    for(yp= starty;yp<endy;yp++) {
        for(xp= startx;xp<endx;xp++) {
            int i = 4*(xp+yp*iw);
            r += src[RED];
            g += src[GREEN];
            b += src[BLUE];
            sum++;
        }
    }
    *wr = r/sum;
    *wg = g/sum;
    *wb = b/sum;
}

void JNIFUNCF(ImageFilterWBalance, nativeApplyFilter, jobject bitmap, jint width, jint height, int locX,int locY)
{
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    int i;
    int len = width * height * 4;
    unsigned char * rgb = (unsigned char * )destination;
    int wr;
    int wg;
    int wb;

    if (locX==-1)
        estmateWhite(rgb,len,&wr,&wg,&wb);
    else
        estmateWhiteBox(rgb, width, height,locX,locY,&wr,&wg,&wb);

    int min = MIN(wr, MIN(wg, wb));
    int max = MAX(wr, MAX(wg, wb));
    float avg = (min+max)/2.f;
    float scaleR =  avg/wr;
    float scaleG =  avg/wg;
    float scaleB =  avg/wb;

    for (i = 0; i < len; i+=4)
    {
        int r = rgb[RED];
        int g = rgb[GREEN];
        int b = rgb[BLUE];

        float Rc =  r*scaleR;
        float Gc =  g*scaleG;
        float Bc =  b*scaleB;

        rgb[RED]   = clamp(Rc);
        rgb[GREEN] = clamp(Gc);
        rgb[BLUE]  = clamp(Bc);
    }
    AndroidBitmap_unlockPixels(env, bitmap);
}
