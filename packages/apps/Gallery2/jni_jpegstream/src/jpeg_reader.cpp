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

#include "jpeg_reader.h"
#include "error_codes.h"
#include "jpeg_hook.h"

#include <setjmp.h>

JpegReader::JpegReader() :
                mInfo(),
                mErrorManager(),
                mScanlineBuf(NULL),
                mScanlineIter(NULL),
                mScanlineBuflen(0),
                mScanlineUnformattedBuflen(0),
                mScanlineBytesRemaining(0),
                mFormat(),
                mFinished(false),
                mSetup(false) {}

JpegReader::~JpegReader() {
    if (reset() != J_SUCCESS) {
        LOGE("Failed to destroy compress object, JpegReader may leak memory.");
    }
}

int32_t JpegReader::setup(JNIEnv *env, jobject in, int32_t* width, int32_t* height,
        Jpeg_Config::Format format) {
    if (mFinished || mSetup) {
        return J_ERROR_FATAL;
    }
    if (env->ExceptionCheck()) {
        return J_EXCEPTION;
    }

    // Setup error handler
    SetupErrMgr(reinterpret_cast<j_common_ptr>(&mInfo), &mErrorManager);
    // Set jump address for error handling
    if (setjmp(mErrorManager.setjmp_buf)) {
        return J_ERROR_FATAL;
    }

    // Call libjpeg setup
    jpeg_create_decompress(&mInfo);

    // Setup our data source object, this allocates java global references
    int32_t flags = MakeSrc(&mInfo, env, in);
    if (flags != J_SUCCESS) {
        LOGE("Failed to make source with error code: %d ", flags);
        return flags;
    }

    // Reads jpeg file header
    jpeg_read_header(&mInfo, TRUE);
    jpeg_calc_output_dimensions(&mInfo);

    const int components = (static_cast<int>(format) & 0xff);

    // Do setup for input format
    switch (components) {
    case 1:
        mInfo.out_color_space = JCS_GRAYSCALE;
        mScanlineUnformattedBuflen = mInfo.output_width;
        break;
    case 3:
    case 4:
        mScanlineUnformattedBuflen = mInfo.output_width * components;
        if (mInfo.jpeg_color_space == JCS_CMYK
                || mInfo.jpeg_color_space == JCS_YCCK) {
            // Always use cmyk for output in a 4 channel jpeg.
            // libjpeg has a builtin cmyk->rgb decoder.
            mScanlineUnformattedBuflen = mInfo.output_width * 4;
            mInfo.out_color_space = JCS_CMYK;
        } else {
            mInfo.out_color_space = JCS_RGB;
        }
        break;
    default:
        return J_ERROR_BAD_ARGS;
    }

    mScanlineBuflen = mInfo.output_width * components;
    mScanlineBytesRemaining = mScanlineBuflen;
    mScanlineBuf = (JSAMPLE *) (mInfo.mem->alloc_small)(
            reinterpret_cast<j_common_ptr>(&mInfo), JPOOL_PERMANENT,
            mScanlineUnformattedBuflen * sizeof(JSAMPLE));
    mScanlineIter = mScanlineBuf;
    jpeg_start_decompress(&mInfo);

    // Output image dimensions
    if (width != NULL) {
        *width = mInfo.output_width;
    }
    if (height != NULL) {
        *height = mInfo.output_height;
    }

    mFormat = format;
    mSetup = true;
    return J_SUCCESS;
}

int32_t JpegReader::read(int8_t* bytes, int32_t offset, int32_t count) {
    if (!mSetup) {
        return J_ERROR_FATAL;
    }
    if (mFinished) {
        return J_DONE;
    }
    // Set jump address for error handling
    if (setjmp(mErrorManager.setjmp_buf)) {
        return J_ERROR_FATAL;
    }
    if (count <= 0) {
        return J_ERROR_BAD_ARGS;
    }
    int32_t total_length = count;
    while (mInfo.output_scanline < mInfo.output_height) {
        if (count < mScanlineBytesRemaining) {
            // read partial scanline and return
            if (bytes != NULL) {
                // Treat NULL bytes as a skip
                memcpy((void*) (bytes + offset), (void*) mScanlineIter,
                        count * sizeof(int8_t));
            }
            mScanlineBytesRemaining -= count;
            mScanlineIter += count;
            return total_length;
        } else if (count > 0) {
            // read full scanline
            if (bytes != NULL) {
                // Treat NULL bytes as a skip
                memcpy((void*) (bytes + offset), (void*) mScanlineIter,
                        mScanlineBytesRemaining * sizeof(int8_t));
                bytes += mScanlineBytesRemaining;
            }
            count -= mScanlineBytesRemaining;
            mScanlineBytesRemaining = 0;
        }
        // Scanline buffer exhausted, read next scanline
        if (jpeg_read_scanlines(&mInfo, &mScanlineBuf, 1) != 1) {
            // Always read full scanline, no IO suspension
            return J_ERROR_FATAL;
        }
        // Do in-place pixel formatting
        formatPixels(static_cast<uint8_t*>(mScanlineBuf),
                mScanlineUnformattedBuflen);

        // Reset iterators
        mScanlineIter = mScanlineBuf;
        mScanlineBytesRemaining = mScanlineBuflen;
    }

    // Read all of the scanlines
    jpeg_finish_decompress(&mInfo);
    mFinished = true;
    return total_length - count;
}

void JpegReader::updateEnv(JNIEnv *env) {
    UpdateSrcEnv(&mInfo, env);
}

// Does in-place pixel formatting
void JpegReader::formatPixels(uint8_t* buf, int32_t len) {
    uint8_t *iter = buf;

    // Do cmyk->rgb conversion if necessary
    switch (mInfo.out_color_space) {
    case JCS_CMYK:
        // Convert CMYK to RGB
        int r, g, b, c, m, y, k;
        for (int i = 0; i < len; i += 4) {
            c = buf[i + 0];
            m = buf[i + 1];
            y = buf[i + 2];
            k = buf[i + 3];
            // Handle fmt for weird photoshop markers
            if (mInfo.saw_Adobe_marker) {
                r = (k * c) / 255;
                g = (k * m) / 255;
                b = (k * y) / 255;
            } else {
                r = (255 - k) * (255 - c) / 255;
                g = (255 - k) * (255 - m) / 255;
                b = (255 - k) * (255 - y) / 255;
            }
            *iter++ = r;
            *iter++ = g;
            *iter++ = b;
        }
        break;
    case JCS_RGB:
        iter += (len * 3 / 4);
        break;
    case JCS_GRAYSCALE:
    default:
        return;
    }

    // Do endianness and alpha for output format
    if (mFormat == Jpeg_Config::FORMAT_RGBA) {
        // Set alphas to 255
        uint8_t* end = buf + len - 1;
        for (int i = len - 1; i >= 0; i -= 4) {
            buf[i] = 255;
            buf[i - 1] = *--iter;
            buf[i - 2] = *--iter;
            buf[i - 3] = *--iter;
        }
    } else if (mFormat == Jpeg_Config::FORMAT_ABGR) {
        // Reverse endianness and set alphas to 255
        uint8_t* end = buf + len - 1;
        int r, g, b;
        for (int i = len - 1; i >= 0; i -= 4) {
            b = *--iter;
            g = *--iter;
            r = *--iter;
            buf[i] = r;
            buf[i - 1] = g;
            buf[i - 2] = b;
            buf[i - 3] = 255;
        }
    }
}

int32_t JpegReader::reset() {
    // Set jump address for error handling
    if (setjmp(mErrorManager.setjmp_buf)) {
        return J_ERROR_FATAL;
    }
    // Clean up global java references
    CleanSrc(&mInfo);
    // Wipe decompress struct, free memory pools
    jpeg_destroy_decompress(&mInfo);
    mFinished = false;
    mSetup = false;
    return J_SUCCESS;
}

