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

#include "jpeg_hook.h"
#include "jpeg_writer.h"
#include "error_codes.h"

#include <setjmp.h>
#include <assert.h>

JpegWriter::JpegWriter() : mInfo(),
                           mErrorManager(),
                           mScanlineBuf(NULL),
                           mScanlineIter(NULL),
                           mScanlineBuflen(0),
                           mScanlineBytesRemaining(0),
                           mFormat(),
                           mFinished(false),
                           mSetup(false) {}

JpegWriter::~JpegWriter() {
    if (reset() != J_SUCCESS) {
        LOGE("Failed to destroy compress object, may leak memory.");
    }
}

const int32_t JpegWriter::DEFAULT_X_DENSITY = 300;
const int32_t JpegWriter::DEFAULT_Y_DENSITY = 300;
const int32_t JpegWriter::DEFAULT_DENSITY_UNIT = 1;

int32_t JpegWriter::setup(JNIEnv *env, jobject out, int32_t width, int32_t height,
        Jpeg_Config::Format format, int32_t quality) {
    if (mFinished || mSetup) {
        return J_ERROR_FATAL;
    }
    if (env->ExceptionCheck()) {
        return J_EXCEPTION;
    }
    if (height <= 0 || width <= 0 || quality <= 0 || quality > 100) {
        return J_ERROR_BAD_ARGS;
    }
    // Setup error handler
    SetupErrMgr(reinterpret_cast<j_common_ptr>(&mInfo), &mErrorManager);

    // Set jump address for error handling
    if (setjmp(mErrorManager.setjmp_buf)) {
        return J_ERROR_FATAL;
    }

    // Setup cinfo struct
    jpeg_create_compress(&mInfo);

    // Setup global java refs
    int32_t flags = MakeDst(&mInfo, env, out);
    if (flags != J_SUCCESS) {
        return flags;
    }

    // Initialize width, height, and color space
    mInfo.image_width = width;
    mInfo.image_height = height;
    const int components = (static_cast<int>(format) & 0xff);
    switch (components) {
    case 1:
        mInfo.input_components = 1;
        mInfo.in_color_space = JCS_GRAYSCALE;
        break;
    case 3:
    case 4:
        mInfo.input_components = 3;
        mInfo.in_color_space = JCS_RGB;
        break;
    default:
        return J_ERROR_BAD_ARGS;
    }

    // Set defaults
    jpeg_set_defaults(&mInfo);
    mInfo.density_unit = DEFAULT_DENSITY_UNIT; // JFIF code for pixel size units:
                             // 1 = in, 2 = cm
    mInfo.X_density = DEFAULT_X_DENSITY; // Horizontal pixel density
    mInfo.Y_density = DEFAULT_Y_DENSITY; // Vertical pixel density

    // Set compress quality
    jpeg_set_quality(&mInfo, quality, TRUE);

    mFormat = format;

    // Setup scanline buffer
    mScanlineBuflen = width * components;
    mScanlineBytesRemaining = mScanlineBuflen;
    mScanlineBuf = (JSAMPLE *) (mInfo.mem->alloc_small)(
            reinterpret_cast<j_common_ptr>(&mInfo), JPOOL_PERMANENT,
            mScanlineBuflen * sizeof(JSAMPLE));
    mScanlineIter = mScanlineBuf;

    // Start compression
    jpeg_start_compress(&mInfo, TRUE);
    mSetup = true;
    return J_SUCCESS;
}

int32_t JpegWriter::write(int8_t* bytes, int32_t length) {
    if (!mSetup) {
        return J_ERROR_FATAL;
    }
    if (mFinished) {
        return 0;
    }
    // Set jump address for error handling
    if (setjmp(mErrorManager.setjmp_buf)) {
        return J_ERROR_FATAL;
    }
    if (length < 0 || bytes == NULL) {
        return J_ERROR_BAD_ARGS;
    }

    int32_t total_length = length;
    JSAMPROW row_pointer[1];
    while (mInfo.next_scanline < mInfo.image_height) {
        if (length < mScanlineBytesRemaining) {
            // read partial scanline and return
            memcpy((void*) mScanlineIter, (void*) bytes,
                    length * sizeof(int8_t));
            mScanlineBytesRemaining -= length;
            mScanlineIter += length;
            return total_length;
        } else if (length > 0) {
            // read full scanline
            memcpy((void*) mScanlineIter, (void*) bytes,
                    mScanlineBytesRemaining * sizeof(int8_t));
            bytes += mScanlineBytesRemaining;
            length -= mScanlineBytesRemaining;
            mScanlineBytesRemaining = 0;
        }
        // Do in-place pixel formatting
        formatPixels(static_cast<uint8_t*>(mScanlineBuf), mScanlineBuflen);
        row_pointer[0] = mScanlineBuf;
        // Do compression
        if (jpeg_write_scanlines(&mInfo, row_pointer, 1) != 1) {
            return J_ERROR_FATAL;
        }
        // Reset scanline buffer
        mScanlineBytesRemaining = mScanlineBuflen;
        mScanlineIter = mScanlineBuf;
    }
    jpeg_finish_compress(&mInfo);
    mFinished = true;
    return total_length - length;
}

// Does in-place pixel formatting
void JpegWriter::formatPixels(uint8_t* buf, int32_t len) {
    //  Assumes len is a multiple of 4 for RGBA and ABGR pixels.
    assert((len % 4) == 0);
    uint8_t* d = buf;
    switch (mFormat) {
    case Jpeg_Config::FORMAT_RGBA: {
        // Strips alphas
        for (int i = 0; i < len / 4; ++i, buf += 4) {
            *d++ = buf[0];
            *d++ = buf[1];
            *d++ = buf[2];
        }
        break;
    }
    case Jpeg_Config::FORMAT_ABGR: {
        // Strips alphas and flips endianness
        if (len / 4 >= 1) {
            *d++ = buf[3];
            uint8_t tmp = *d;
            *d++ = buf[2];
            *d++ = tmp;
        }
        for (int i = 1; i < len / 4; ++i, buf += 4) {
            *d++ = buf[3];
            *d++ = buf[2];
            *d++ = buf[1];
        }
        break;
    }
    default: {
        // Do nothing
        break;
    }
    }
}

void JpegWriter::updateEnv(JNIEnv *env) {
    UpdateDstEnv(&mInfo, env);
}

int32_t JpegWriter::reset() {
    // Set jump address for error handling
    if (setjmp(mErrorManager.setjmp_buf)) {
        return J_ERROR_FATAL;
    }
    // Clean up global java references
    CleanDst(&mInfo);
    // Wipe compress struct, free memory pools
    jpeg_destroy_compress(&mInfo);
    mFinished = false;
    mSetup = false;
    return J_SUCCESS;
}
