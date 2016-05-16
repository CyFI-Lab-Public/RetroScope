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

double fastevalPoly(double *poly,int n, double x){

    double f =x;
    double sum = poly[0]+poly[1]*f;
    int i;
    for (i = 2; i < n; i++) {
        f*=x;
        sum += poly[i]*f;
    }
    return sum;
}

void rgb2hsv( unsigned char *rgb,int rgbOff,unsigned short *hsv,int hsvOff)
{
    int iMin,iMax,chroma;
    int ABITS = 4;
    int HSCALE = 256;

    int k1=255 << ABITS;
    int k2=HSCALE << ABITS;

    int ri = rgb[rgbOff+0];
    int gi = rgb[rgbOff+1];
    int bi = rgb[rgbOff+2];
    short rv,rs,rh;

    if (ri > gi) {
        iMax = MAX (ri, bi);
        iMin = MIN (gi, bi);
    } else {
        iMax = MAX (gi, bi);
        iMin = MIN (ri, bi);
    }

    chroma = iMax - iMin;
    // set value
    rv = (short)( iMax << ABITS);

    // set saturation
    if (rv == 0)
        rs = 0;
    else
        rs = (short)((k1*chroma)/iMax);

    // set hue
    if (rs == 0)
        rh = 0;
    else {
        if ( ri == iMax ) {
            rh  = (short)( (k2*(6*chroma+gi - bi))/(6*chroma));
            if (rh >= k2) rh -= k2;
        } else if (gi  == iMax)
            rh  = (short)( (k2*(2*chroma+bi - ri ))/(6*chroma));
        else // (bi == iMax )
                    rh  = (short)( (k2*(4*chroma+ri - gi ))/(6*chroma));
    }
    hsv[hsvOff+0] = rv;
    hsv[hsvOff+1] = rs;
    hsv[hsvOff+2] = rh;
}

void hsv2rgb(unsigned short *hsv,int hsvOff, unsigned char *rgb,int rgbOff)
{
    int ABITS = 4;
    int HSCALE = 256;
    int m;
    int H,X,ih,is,iv;
    int k1=255<<ABITS;
    int k2=HSCALE<<ABITS;
    int k3=1<<(ABITS-1);
    int rr=0;
    int rg=0;
    int rb=0;
    short cv = hsv[hsvOff+0];
    short cs = hsv[hsvOff+1];
    short ch = hsv[hsvOff+2];

    // set chroma and min component value m
    //chroma = ( cv * cs )/k1;
    //m = cv - chroma;
    m = ((int)cv*(k1 - (int)cs ))/k1;

    // chroma  == 0 <-> cs == 0 --> m=cv
    if (cs == 0) {
        rb = ( rg = ( rr =( cv >> ABITS) ));
    } else {
        ih=(int)ch;
        is=(int)cs;
        iv=(int)cv;

        H = (6*ih)/k2;
        X = ((iv*is)/k2)*(k2- abs(6*ih- 2*(H>>1)*k2 - k2)) ;

        // removing additional bits --> unit8
        X=( (X+iv*(k1 - is ))/k1 + k3 ) >> ABITS;
        m=m >> ABITS;

        // ( chroma + m ) --> cv ;
        cv=(short) (cv >> ABITS);
        switch (H) {
        case 0:
            rr = cv;
            rg = X;
            rb = m;
            break;
        case 1:
            rr = X;
            rg = cv;
            rb = m;
            break;
        case 2:
            rr = m;
            rg = cv;
            rb = X;
            break;
        case 3:
            rr = m;
            rg = X;
            rb = cv;
            break;
        case 4:
            rr = X;
            rg = m;
            rb = cv;
            break;
        case 5:
            rr = cv;
            rg = m ;
            rb = X;
            break;
        }
    }
    rgb[rgbOff+0] =  rr;
    rgb[rgbOff+1] =  rg;
    rgb[rgbOff+2] =  rb;
}

