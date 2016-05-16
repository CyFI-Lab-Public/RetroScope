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
#ifndef JPEG_READER_H_
#define JPEG_READER_H_

#include "jerr_hook.h"
#include "jni_defines.h"
#include "jpeg_config.h"

#include <stdint.h>

/**
 * JpegReader wraps libjpeg's decompression functionality and a
 * java InputStream object.  Read calls return data from the
 * InputStream that has been decompressed.
 */
class JpegReader {
public:
    JpegReader();
    ~JpegReader();

    /**
     * Call setup with a valid InputStream reference and pixel format.
     * If this method is successful, the contents of width and height will
     * be set to the dimensions of the bitmap to be read.
     *
     * ***This method will result in the jpeg file header being read
     * from the InputStream***
     *
     * Returns J_SUCCESS on success or a negative error code.
     */
    int32_t setup(JNIEnv *env, jobject in, int32_t* width, int32_t* height,
            Jpeg_Config::Format format);

    /**
     * Decompresses bytes from the InputStream and writes at most count
     * bytes into the buffer, bytes, starting at some offset.  Passing a
     * NULL as the bytes pointer effectively skips those bytes.
     *
     * ***This method will result in bytes being read from the InputStream***
     *
     * Returns the number of bytes written into the input buffer or a
     * negative error code.
     */
    int32_t read(int8_t * bytes, int32_t offset, int32_t count);

    /**
     * Updates the environment pointer.  Call this before read or reset
     * in any jni function call.
     */
    void updateEnv(JNIEnv *env);

    /**
     * Frees any java global references held by the JpegReader, destroys
     * the decompress structure, and frees allocations in libjpeg's pools.
     */
    int32_t reset();

private:
    void formatPixels(uint8_t* buf, int32_t len);
    struct jpeg_decompress_struct mInfo;
    ErrManager mErrorManager;

    JSAMPLE* mScanlineBuf;
    JSAMPLE* mScanlineIter;
    int32_t mScanlineBuflen;
    int32_t mScanlineUnformattedBuflen;
    int32_t mScanlineBytesRemaining;

    Jpeg_Config::Format mFormat;
    bool mFinished;
    bool mSetup;
};

#endif // JPEG_READER_H_
