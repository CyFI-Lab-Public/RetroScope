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

#ifndef ANDROID_AUDIO_PRIMITIVES_H
#define ANDROID_AUDIO_PRIMITIVES_H

#include <stdint.h>
#include <stdlib.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

/**
 * Dither and clamp pairs of 32-bit input samples (sums) to 16-bit output samples (out).
 * Each 32-bit input sample is a signed fixed-point Q19.12.
 * The .12 fraction is dithered, and the integer portion is then clamped to Q15.
 * For interleaved stereo, c is the number of sample pairs,
 * and out is an array of interleaved pairs of 16-bit samples per channel.
 * For mono, c is the number of samples / 2, and out is an array of 16-bit samples.
 * The name "dither" is a misnomer; the current implementation does not actually dither
 * but uses truncation.  This may change.
 * The out and sums buffers must either be completely separate (non-overlapping), or
 * they must both start at the same address.  Partially overlapping buffers are not supported.
 */
void ditherAndClamp(int32_t* out, const int32_t *sums, size_t c);

/* Expand and copy samples from unsigned 8-bit offset by 0x80 to signed 16-bit.
 * Parameters:
 *  dst     Destination buffer
 *  src     Source buffer
 *  count   Number of samples to copy
 * The destination and source buffers must either be completely separate (non-overlapping), or
 * they must both start at the same address.  Partially overlapping buffers are not supported.
 */
void memcpy_to_i16_from_u8(int16_t *dst, const uint8_t *src, size_t count);

/* Shrink and copy samples from signed 16-bit to unsigned 8-bit offset by 0x80.
 * Parameters:
 *  dst     Destination buffer
 *  src     Source buffer
 *  count   Number of samples to copy
 * The destination and source buffers must either be completely separate (non-overlapping), or
 * they must both start at the same address.  Partially overlapping buffers are not supported.
 * The conversion is done by truncation, without dithering, so it loses resolution.
 */
void memcpy_to_u8_from_i16(uint8_t *dst, const int16_t *src, size_t count);

/* Shrink and copy samples from signed 32-bit fixed-point Q0.31 to signed 16-bit Q0.15.
 * Parameters:
 *  dst     Destination buffer
 *  src     Source buffer
 *  count   Number of samples to copy
 * The destination and source buffers must either be completely separate (non-overlapping), or
 * they must both start at the same address.  Partially overlapping buffers are not supported.
 * The conversion is done by truncation, without dithering, so it loses resolution.
 */
void memcpy_to_i16_from_i32(int16_t *dst, const int32_t *src, size_t count);

/* Shrink and copy samples from single-precision floating-point to signed 16-bit.
 * Each float should be in the range -1.0 to 1.0.  Values outside that range are clamped.
 * Parameters:
 *  dst     Destination buffer
 *  src     Source buffer
 *  count   Number of samples to copy
 * The destination and source buffers must either be completely separate (non-overlapping), or
 * they must both start at the same address.  Partially overlapping buffers are not supported.
 * The conversion is done by truncation, without dithering, so it loses resolution.
 */
void memcpy_to_i16_from_float(int16_t *dst, const float *src, size_t count);

/* Downmix pairs of interleaved stereo input 16-bit samples to mono output 16-bit samples.
 * Parameters:
 *  dst     Destination buffer
 *  src     Source buffer
 *  count   Number of stereo frames to downmix
 * The destination and source buffers must be completely separate (non-overlapping).
 * The current implementation truncates the sum rather than dither, but this may change.
 */
void downmix_to_mono_i16_from_stereo_i16(int16_t *dst, const int16_t *src, size_t count);

/* Upmix mono input 16-bit samples to pairs of interleaved stereo output 16-bit samples by
 * duplicating.
 * Parameters:
 *  dst     Destination buffer
 *  src     Source buffer
 *  count   Number of mono samples to upmix
 * The destination and source buffers must be completely separate (non-overlapping).
 */
void upmix_to_stereo_i16_from_mono_i16(int16_t *dst, const int16_t *src, size_t count);

/**
 * Clamp (aka hard limit or clip) a signed 32-bit sample to 16-bit range.
 */
static inline int16_t clamp16(int32_t sample)
{
    if ((sample>>15) ^ (sample>>31))
        sample = 0x7FFF ^ (sample>>31);
    return sample;
}

/**
 * Multiply-accumulate 16-bit terms with 32-bit result: return a + in*v.
 */
static inline
int32_t mulAdd(int16_t in, int16_t v, int32_t a)
{
#if defined(__arm__) && !defined(__thumb__)
    int32_t out;
    asm( "smlabb %[out], %[in], %[v], %[a] \n"
         : [out]"=r"(out)
         : [in]"%r"(in), [v]"r"(v), [a]"r"(a)
         : );
    return out;
#else
    return a + in * (int32_t)v;
#endif
}

/**
 * Multiply 16-bit terms with 32-bit result: return in*v.
 */
static inline
int32_t mul(int16_t in, int16_t v)
{
#if defined(__arm__) && !defined(__thumb__)
    int32_t out;
    asm( "smulbb %[out], %[in], %[v] \n"
         : [out]"=r"(out)
         : [in]"%r"(in), [v]"r"(v)
         : );
    return out;
#else
    return in * (int32_t)v;
#endif
}

/**
 * Similar to mulAdd, but the 16-bit terms are extracted from a 32-bit interleaved stereo pair.
 */
static inline
int32_t mulAddRL(int left, uint32_t inRL, uint32_t vRL, int32_t a)
{
#if defined(__arm__) && !defined(__thumb__)
    int32_t out;
    if (left) {
        asm( "smlabb %[out], %[inRL], %[vRL], %[a] \n"
             : [out]"=r"(out)
             : [inRL]"%r"(inRL), [vRL]"r"(vRL), [a]"r"(a)
             : );
    } else {
        asm( "smlatt %[out], %[inRL], %[vRL], %[a] \n"
             : [out]"=r"(out)
             : [inRL]"%r"(inRL), [vRL]"r"(vRL), [a]"r"(a)
             : );
    }
    return out;
#else
    if (left) {
        return a + (int16_t)(inRL&0xFFFF) * (int16_t)(vRL&0xFFFF);
    } else {
        return a + (int16_t)(inRL>>16) * (int16_t)(vRL>>16);
    }
#endif
}

/**
 * Similar to mul, but the 16-bit terms are extracted from a 32-bit interleaved stereo pair.
 */
static inline
int32_t mulRL(int left, uint32_t inRL, uint32_t vRL)
{
#if defined(__arm__) && !defined(__thumb__)
    int32_t out;
    if (left) {
        asm( "smulbb %[out], %[inRL], %[vRL] \n"
             : [out]"=r"(out)
             : [inRL]"%r"(inRL), [vRL]"r"(vRL)
             : );
    } else {
        asm( "smultt %[out], %[inRL], %[vRL] \n"
             : [out]"=r"(out)
             : [inRL]"%r"(inRL), [vRL]"r"(vRL)
             : );
    }
    return out;
#else
    if (left) {
        return (int16_t)(inRL&0xFFFF) * (int16_t)(vRL&0xFFFF);
    } else {
        return (int16_t)(inRL>>16) * (int16_t)(vRL>>16);
    }
#endif
}

__END_DECLS

#endif  // ANDROID_AUDIO_PRIMITIVES_H
