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
#ifndef JPEG_WRITER_H_
#define JPEG_WRITER_H_

#include "jerr_hook.h"
#include "jni_defines.h"
#include "jpeg_config.h"

#include <stdint.h>

/**
 * JpegWriter wraps libjpeg's compression functionality and a
 * java OutputStream object.  Write calls result in input data
 * being compressed and written to the OuputStream.
 */
class JpegWriter {
public:
    JpegWriter();
    ~JpegWriter();

    /**
     * Call setup with a valid OutputStream reference, bitmap height and
     * width, pixel format, and compression quality in range (0, 100].
     *
     * Returns J_SUCCESS on success or a negative error code.
     */
    int32_t setup(JNIEnv *env, jobject out, int32_t width, int32_t height,
            Jpeg_Config::Format format, int32_t quality);

    /**
     * Compresses bytes from the input buffer.
     *
     * ***This method will result in bytes being written to the OutputStream***
     *
     * Returns J_SUCCESS on success or a negative error code.
     */
    int32_t write(int8_t* bytes, int32_t length);

    /**
     * Updates the environment pointer.  Call this before write or reset
     * in any jni function call.
     */
    void updateEnv(JNIEnv *env);

    /**
     * Frees any java global references held by the JpegWriter, destroys
     * the compress structure, and frees allocations in libjpeg's pools.
     */
    int32_t reset();

    static const int32_t DEFAULT_X_DENSITY;
    static const int32_t DEFAULT_Y_DENSITY;
    static const int32_t DEFAULT_DENSITY_UNIT;
private:
    void formatPixels(uint8_t* buf, int32_t len);
    struct jpeg_compress_struct mInfo;
    ErrManager mErrorManager;

    JSAMPLE* mScanlineBuf;
    JSAMPLE* mScanlineIter;
    int32_t mScanlineBuflen;
    int32_t mScanlineBytesRemaining;

    Jpeg_Config::Format mFormat;
    bool mFinished;
    bool mSetup;
};

#endif // JPEG_WRITER_H_
