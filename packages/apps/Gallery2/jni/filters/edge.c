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

void JNIFUNCF(ImageFilterEdge, nativeApplyFilter, jobject bitmap, jint width, jint height, jfloat p)
{
    char* destination = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);

    // using contrast function:
    // f(v) = exp(-alpha * v^beta)
    // use beta ~ 1

    float const alpha = 5.0f;
    float const beta = p;
    float const c_min = 100.0f;
    float const c_max = 500.0f;

    // pixels must be 4 bytes
    char * dst = destination;

    int j, k;
    char * ptr = destination;
    int row_stride = 4 * width;

    // set 2 row buffer (avoids bitmap copy)
    int buf_len = 2 * row_stride;
    char buf[buf_len];
    int buf_row_ring = 0;

    // set initial buffer to black
    memset(buf, 0, buf_len * sizeof(char));
    for (j = 3; j < buf_len; j+=4) {
        *(buf + j) = 255;  // set initial alphas
    }

    // apply sobel filter
    for (j = 1; j < height - 1; j++) {

        for (k = 1; k < width - 1; k++){
            int loc = j * row_stride + k * 4;

            float bestx = 0.0f;
            int l;
            for (l = 0; l < 3; l++) {
                float tmp = 0.0f;
                tmp += *(ptr + (loc - row_stride + 4 + l));
                tmp += *(ptr + (loc + 4 + l)) * 2.0f;
                tmp += *(ptr + (loc + row_stride + 4 + l));
                tmp -= *(ptr + (loc - row_stride - 4 + l));
                tmp -= *(ptr + (loc - 4 + l)) * 2.0f;
                tmp -= *(ptr + (loc + row_stride - 4 + l));
                if (fabs(tmp) > fabs(bestx)) {
                    bestx = tmp;
                }
            }

            float besty = 0.0f;
            for (l = 0; l < 3; l++) {
                float tmp = 0.0f;
                tmp -= *(ptr + (loc - row_stride - 4 + l));
                tmp -= *(ptr + (loc - row_stride + l)) * 2.0f;
                tmp -= *(ptr + (loc - row_stride + 4 + l));
                tmp += *(ptr + (loc + row_stride - 4 + l));
                tmp += *(ptr + (loc + row_stride + l)) * 2.0f;
                tmp += *(ptr + (loc + row_stride + 4 + l));
                if (fabs(tmp) > fabs(besty)) {
                    besty = tmp;
                }
            }

            // compute gradient magnitude
            float mag = sqrt(bestx * bestx + besty * besty);

            // clamp
            mag = MIN(MAX(c_min, mag), c_max);

            // scale to [0, 1]
            mag = (mag - c_min) / (c_max - c_min);

            float ret = 1.0f - exp (- alpha * pow(mag, beta));
            ret = 255 * ret;

            int off = k * 4;
            *(buf + buf_row_ring + off) = ret;
            *(buf + buf_row_ring + off + 1) = ret;
            *(buf + buf_row_ring + off + 2) = ret;
            *(buf + buf_row_ring + off + 3) = *(ptr + loc + 3);
        }

        buf_row_ring += row_stride;
        buf_row_ring %= buf_len;

        if (j - 1 >= 0) {
            memcpy((dst + row_stride * (j - 1)), (buf + buf_row_ring), row_stride * sizeof(char));
        }

    }
    buf_row_ring += row_stride;
    buf_row_ring %= buf_len;
    int second_last_row = row_stride * (height - 2);
    memcpy((dst + second_last_row), (buf + buf_row_ring), row_stride * sizeof(char));

    // set last row to black
    int last_row = row_stride * (height - 1);
    memset((dst + last_row), 0, row_stride * sizeof(char));
    for (j = 3; j < row_stride; j+=4) {
        *(dst + last_row + j) = 255;  // set alphas
    }
    AndroidBitmap_unlockPixels(env, bitmap);
}
