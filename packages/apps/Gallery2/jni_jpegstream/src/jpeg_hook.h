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

#ifndef LIBJPEG_HOOK_H_
#define LIBJPEG_HOOK_H_

extern "C" {
#include "jinclude.h"
#include "jpeglib.h"
#include "jerror.h"
}

#include "inputstream_wrapper.h"
#include "outputstream_wrapper.h"

#include <stdint.h>

/**
 * DestManager holds the libjpeg destination manager struct and
 * a holder with a java OutputStream.
 */
typedef struct {
    struct jpeg_destination_mgr mgr;
    OutputStreamWrapper *outStream;
} DestManager;

// Initializes the DestManager struct, sets up the jni refs
int32_t MakeDst(j_compress_ptr cinfo, JNIEnv *env, jobject outStream);

/**
 * Updates the jni env pointer. This should be called in the beginning of any
 * JNI method in jpegstream.cpp before CleanDst or any of the libjpeg functions
 * that can trigger a call to an OutputStreamWrapper method.
 */
void UpdateDstEnv(j_compress_ptr cinfo, JNIEnv* env);

// Cleans the jni refs.  To wipe the compress object call jpeg_destroy_compress
void CleanDst(j_compress_ptr cinfo);

/**
 * SourceManager holds the libjpeg source manager struct and a
 * holder with a java InputStream.
 */
typedef struct {
    struct jpeg_source_mgr mgr;
    boolean start_of_file;
    InputStreamWrapper *inStream;
} SourceManager;

// Initializes the SourceManager struct, sets up the jni refs
int32_t MakeSrc(j_decompress_ptr cinfo, JNIEnv *env, jobject inStream);

/**
 * Updates the jni env pointer. This should be called in the beginning of any
 * JNI method in jpegstream.cpp before CleanSrc or any of the libjpeg functions
 * that can trigger a call to an InputStreamWrapper method.
 */
void UpdateSrcEnv(j_decompress_ptr cinfo, JNIEnv* env);

// Cleans the jni refs.  To wipe the decompress object, call jpeg_destroy_decompress
void CleanSrc(j_decompress_ptr cinfo);

#endif // LIBJPEG_HOOK_H_
