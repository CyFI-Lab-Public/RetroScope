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

int value(int r, int g, int b) {
    return MAX(r, MAX(g, b));
}

int isRed(unsigned char *src, int p) {
    int b = src[p + 2];
    int g = src[p + 1];
    int r = src[p];
    int max = MAX(g, b);

    return ((r * 100 / (max + 2) > 160) & (max < 80));
}

void findPossible(unsigned char *src, unsigned char *mask, int iw, int ih,
        short *rect) {
    int recX = rect[0], recY = rect[1], recW = rect[2], recH = rect[3];
    int y, x;

    for (y = 0; y < recH; y++) {
        int sy = (recY + y) * iw;
        for (x = 0; x < recW; x++) {
            int p = (recX + x + sy) * 4;

            int b = src[p + 2];
            int g = src[p + 1];
            int r = src[p];
            mask[x + y * recW] = (
                    mask[x + y * recW] > 0 && (value(r, g, b) > 240) ? 1 : 0);

        }

    }
}

void findReds(unsigned char *src, unsigned char *mask, int iw, int ih,
        short *rect) {
    int recX = rect[0], recY = rect[1], recW = rect[2], recH = rect[3];
    int y, x;

    for (y = 0; y < recH; y++) {
        int sy = (recY + y) * iw;
        for (x = 0; x < recW; x++) {
            int p = (recX + x + sy) * 4;

            mask[x + y * recW] = ((isRed(src, p)) ? 1 : 0);

        }

    }
}

void dialateMaskIfRed(unsigned char *src, int iw, int ih, unsigned char *mask,
        unsigned char *out, short *rect) {
    int recX = rect[0], recY = rect[1], recW = rect[2], recH = rect[3];
    int y, x;

    for (y = 1; y < recH - 1; y++) {
        int row = recW * y;
        int sy = (recY + y) * iw;
        for (x = 1; x < recW - 1; x++) {
            int p = (recX + x + sy) * 4;

            char b = (mask[row + x] | mask[row + x + 1] | mask[row + x - 1]
                    | mask[row + x - recW] | mask[row + x + recW]);
            if (b != 0 && isRed(src, p))
                out[row + x] = 1;
            else
                out[row + x] = mask[row + x];
        }
    }
}

void dialateMask(unsigned char *mask, unsigned char *out, int mw, int mh) {
    int y, x;
    for (y = 1; y < mh - 1; y++) {
        int row = mw * y;
        for (x = 1; x < mw - 1; x++) {
            out[row + x] = (mask[row + x] | mask[row + x + 1]
                    | mask[row + x - 1] | mask[row + x - mw]
                    | mask[row + x + mw]);
        }
    }
}

void stuff(int r, int g, int b, unsigned char *img, int off) {
    img[off + 2] = b;
    img[off + 1] = g;
    img[off] = r;
}

void filterRedEye(unsigned char *src, unsigned char *dest, int iw, int ih, short *rect) {
    int recX = rect[0], recY = rect[1], recW = rect[2], recH = rect[3];
    unsigned char *mask1 = (unsigned char *) malloc(recW * recH);
    unsigned char *mask2 = (unsigned char *)malloc(recW*recH);
    int QUE_LEN = 100;
    int y, x, i;

    rect[0] = MAX(rect[0],0);
    rect[1] = MAX(rect[1],0);
    rect[2] = MIN(rect[2]+rect[0],iw)-rect[0];
    rect[3] = MIN(rect[3]+rect[1],ih)-rect[1];

    findReds(src, mask2, iw, ih, rect);
    dialateMask(mask2, mask1, recW, recH);
    dialateMask(mask1, mask2, recW, recH);
    dialateMask(mask2, mask1, recW, recH);
    dialateMask(mask1, mask2, recW, recH);
    findPossible(src, mask2, iw, ih, rect);
    dialateMask(mask2, mask1, recW, recH);

    for (i = 0; i < 12; i++) {
        dialateMaskIfRed(src, iw, ih, mask1, mask2, rect);
        dialateMaskIfRed(src, iw, ih, mask2, mask1, rect);
    }
    dialateMask(mask1, mask2, recW, recH);
    dialateMask(mask2, mask1, recW, recH);

    for (y = 3; y < recH-3; y++) {
        int sy = (recY + y) * iw;
        for (x = 3; x < recW-3; x++) {
            int p = (recX + x + sy) * 4;

            int b = src[p + 2];
            int g = src[p + 1];
            int r = src[p];

            if (mask1[x + y * recW] != 0) {
                int m = MAX(g,b);
                float rr = (r - m) / (float) m;
                if (rr > .7f && g < 60 && b < 60) {
                    dest[p + 2] = (0);
                    dest[p + 1] = (0);
                    dest[p] = (0);
                } else {
                    if (mask2[x + y * recW] != 0) {
                        stuff(r / 2, g / 2, b / 2, dest, p);
                    } else
                        stuff((2 * r) / 3, (2 * g) / 3, (2 * b) / 3, dest, p);
                }

            } else
                stuff(r, g, b, dest, p);

            //dest[p + 2] = dest[p + 1] =dest[p]=src[p];
        }

    }

    free(mask1);
    free(mask2);
}


