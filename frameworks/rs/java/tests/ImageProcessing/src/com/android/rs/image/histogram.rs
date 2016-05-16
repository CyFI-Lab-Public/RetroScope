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

#include "ip.rsh"

rs_allocation gSrc;
rs_allocation gDest;
rs_allocation gSums;
rs_allocation gSum;

int gWidth;
int gHeight;
int gStep;
int gSteps;

void __attribute__((kernel)) pass1(int in, uint x, uint y) {
    for (int i=0; i < (256); i++) {
        rsSetElementAt_int(gSums, 0, i, y);
    }

    for (int i = 0; i < gStep; i++) {
        int py = y*gStep + i;
        if (py >= gHeight) return;

        for (int px=0; px < gWidth; px++) {
            uchar4 c = rsGetElementAt_uchar4(gSrc, px, py);
            int lum = (77 * c.r + 150 * c.g + 29 * c.b) >> 8;

            int old = rsGetElementAt_int(gSums, lum, y);
            rsSetElementAt_int(gSums, old+1, lum, y);
        }
    }
}

int __attribute__((kernel)) pass2(uint x) {
    int sum = 0;
    for (int i=0; i < gSteps; i++) {
        sum += rsGetElementAt_int(gSums, x, i);
    }
    return sum;
}

void rescale() {
    int maxv = 0;

    for (int i=0; i < 256; i++) {
        maxv = max(maxv, rsGetElementAt_int(gSum, i));
    }
    float overMax = (1.f / maxv) * gHeight;

    for (int i=0; i < 256; i++) {
        int t = rsGetElementAt_int(gSum, i);
        t = gHeight - (overMax * rsGetElementAt_int(gSum, i));
        t = max(0, t);
        rsSetElementAt_int(gSum, t, i);
    }
}

static const uchar4 gClear = {0, 0, 0, 0xff};

uchar4 __attribute__((kernel)) clear() {
    return gClear;
}

uchar4 __attribute__((kernel)) draw(uint x, uint y) {
    int l = rsGetElementAt_int(gSum, x >> 2);
    if (y > l) {
        return 0xff;
    }
    return gClear;
}
