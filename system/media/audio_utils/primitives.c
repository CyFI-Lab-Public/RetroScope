/*
 * Copyright (C) 2011 The Android Open Source Project
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

#include <audio_utils/primitives.h>

void ditherAndClamp(int32_t* out, const int32_t *sums, size_t c)
{
    size_t i;
    for (i=0 ; i<c ; i++) {
        int32_t l = *sums++;
        int32_t r = *sums++;
        int32_t nl = l >> 12;
        int32_t nr = r >> 12;
        l = clamp16(nl);
        r = clamp16(nr);
        *out++ = (r<<16) | (l & 0xFFFF);
    }
}

void memcpy_to_i16_from_u8(int16_t *dst, const uint8_t *src, size_t count)
{
    dst += count;
    src += count;
    while (count--) {
        *--dst = (int16_t)(*--src - 0x80) << 8;
    }
}

void memcpy_to_u8_from_i16(uint8_t *dst, const int16_t *src, size_t count)
{
    while (count--) {
        *dst++ = (*src++ >> 8) + 0x80;
    }
}

void memcpy_to_i16_from_i32(int16_t *dst, const int32_t *src, size_t count)
{
    while (count--) {
        *dst++ = *src++ >> 16;
    }
}

void memcpy_to_i16_from_float(int16_t *dst, const float *src, size_t count)
{
    while (count--) {
        float f = *src++;
        int16_t i;
        if (f > 1.0) {
            i = 32767;
        } else if (f < -1.0) {
            i = -32768;
        } else {
            // does not specifically handle NaN
            i = f * 32767.0;
        }
        *dst++ = i;
    }
}

void downmix_to_mono_i16_from_stereo_i16(int16_t *dst, const int16_t *src, size_t count)
{
    while (count--) {
        *dst++ = (int16_t)(((int32_t)src[0] + (int32_t)src[1]) >> 1);
        src += 2;
    }
}

void upmix_to_stereo_i16_from_mono_i16(int16_t *dst, const int16_t *src, size_t count)
{
    while (count--) {
        int32_t temp = *src++;
        dst[0] = temp;
        dst[1] = temp;
        dst += 2;
    }
}
