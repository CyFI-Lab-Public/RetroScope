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

#include "error_codes.h"
#include "jni_defines.h"
#include "jpeg_writer.h"
#include "jpeg_reader.h"
#include "jpeg_config.h"
#include "outputstream_wrapper.h"
#include "inputstream_wrapper.h"

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

static jint OutputStream_setup(JNIEnv* env, jobject thiz, jobject out,
        jint width, jint height, jint format, jint quality) {
    // Get a reference to this object's class
    jclass thisClass = env->GetObjectClass(thiz);
    if (env->ExceptionCheck() || thisClass == NULL) {
        return J_EXCEPTION;
    }
    // Get field for storing C pointer
    jfieldID fidNumber = env->GetFieldID(thisClass, "JNIPointer", "J");
    if (NULL == fidNumber || env->ExceptionCheck()) {
        return J_EXCEPTION;
    }

    // Check size
    if (width <= 0 || height <= 0) {
        return J_ERROR_BAD_ARGS;
    }
    Jpeg_Config::Format fmt = static_cast<Jpeg_Config::Format>(format);
    // Check format
    switch (fmt) {
    case Jpeg_Config::FORMAT_GRAYSCALE:
    case Jpeg_Config::FORMAT_RGB:
    case Jpeg_Config::FORMAT_RGBA:
    case Jpeg_Config::FORMAT_ABGR:
        break;
    default:
        return J_ERROR_BAD_ARGS;
    }

    uint32_t w = static_cast<uint32_t>(width);
    uint32_t h = static_cast<uint32_t>(height);
    int32_t q = static_cast<int32_t>(quality);
    // Clamp quality to (0, 100]
    q = (q > 100) ? 100 : ((q < 1) ? 1 : q);

    JpegWriter* w_ptr = new JpegWriter();

    // Do JpegWriter setup.
    int32_t errorFlag = w_ptr->setup(env, out, w, h, fmt, q);
    if (env->ExceptionCheck() || errorFlag != J_SUCCESS) {
        delete w_ptr;
        return errorFlag;
    }

    // Store C pointer for writer
    env->SetLongField(thiz, fidNumber, reinterpret_cast<jlong>(w_ptr));
    if (env->ExceptionCheck()) {
        delete w_ptr;
        return J_EXCEPTION;
    }
    return J_SUCCESS;
}

static jint InputStream_setup(JNIEnv* env, jobject thiz, jobject dimens,
        jobject in, jint format) {
    // Get a reference to this object's class
    jclass thisClass = env->GetObjectClass(thiz);
    if (env->ExceptionCheck() || thisClass == NULL) {
        return J_EXCEPTION;
    }
    jmethodID setMethod = NULL;

    // Get dimensions object setter method
    if (dimens != NULL) {
        jclass pointClass = env->GetObjectClass(dimens);
        if (env->ExceptionCheck() || pointClass == NULL) {
            return J_EXCEPTION;
        }
        setMethod = env->GetMethodID(pointClass, "set", "(II)V");
        if (env->ExceptionCheck() || setMethod == NULL) {
            return J_EXCEPTION;
        }
    }
    // Get field for storing C pointer
    jfieldID fidNumber = env->GetFieldID(thisClass, "JNIPointer", "J");
    if (NULL == fidNumber || env->ExceptionCheck()) {
        return J_EXCEPTION;
    }
    Jpeg_Config::Format fmt = static_cast<Jpeg_Config::Format>(format);
    // Check format
    switch (fmt) {
    case Jpeg_Config::FORMAT_GRAYSCALE:
    case Jpeg_Config::FORMAT_RGB:
    case Jpeg_Config::FORMAT_RGBA:
    case Jpeg_Config::FORMAT_ABGR:
        break;
    default:
        return J_ERROR_BAD_ARGS;
    }

    JpegReader* r_ptr = new JpegReader();
    int32_t w = 0, h = 0;
    // Do JpegReader setup.
    int32_t errorFlag = r_ptr->setup(env, in, &w, &h, fmt);
    if (env->ExceptionCheck() || errorFlag != J_SUCCESS) {
        delete r_ptr;
        return errorFlag;
    }

    // Set dimensions to return
    if (dimens != NULL) {
        env->CallVoidMethod(dimens, setMethod, static_cast<jint>(w),
                static_cast<jint>(h));
        if (env->ExceptionCheck()) {
            delete r_ptr;
            return J_EXCEPTION;
        }
    }
    // Store C pointer for reader
    env->SetLongField(thiz, fidNumber, reinterpret_cast<jlong>(r_ptr));
    if (env->ExceptionCheck()) {
        delete r_ptr;
        return J_EXCEPTION;
    }
    return J_SUCCESS;
}

static JpegWriter* getWPtr(JNIEnv* env, jobject thiz, jfieldID* fid) {
    jclass thisClass = env->GetObjectClass(thiz);
    if (env->ExceptionCheck() || thisClass == NULL) {
        return NULL;
    }
    jfieldID fidNumber = env->GetFieldID(thisClass, "JNIPointer", "J");
    if (NULL == fidNumber || env->ExceptionCheck()) {
        return NULL;
    }
    jlong ptr = env->GetLongField(thiz, fidNumber);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    // Get writer C pointer out of java field.
    JpegWriter* w_ptr = reinterpret_cast<JpegWriter*>(ptr);
    if (fid != NULL) {
        *fid = fidNumber;
    }
    return w_ptr;
}

static JpegReader* getRPtr(JNIEnv* env, jobject thiz, jfieldID* fid) {
    jclass thisClass = env->GetObjectClass(thiz);
    if (env->ExceptionCheck() || thisClass == NULL) {
        return NULL;
    }
    jfieldID fidNumber = env->GetFieldID(thisClass, "JNIPointer", "J");
    if (NULL == fidNumber || env->ExceptionCheck()) {
        return NULL;
    }
    jlong ptr = env->GetLongField(thiz, fidNumber);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    // Get reader C pointer out of java field.
    JpegReader* r_ptr = reinterpret_cast<JpegReader*>(ptr);
    if (fid != NULL) {
        *fid = fidNumber;
    }
    return r_ptr;
}

static void OutputStream_cleanup(JNIEnv* env, jobject thiz) {
    jfieldID fidNumber = NULL;
    JpegWriter* w_ptr = getWPtr(env, thiz, &fidNumber);
    if (w_ptr == NULL) {
        return;
    }
    // Update environment
    w_ptr->updateEnv(env);
    // Destroy writer object
    delete w_ptr;
    w_ptr = NULL;
    // Set the java field to null
    env->SetLongField(thiz, fidNumber, reinterpret_cast<jlong>(w_ptr));
}

static void InputStream_cleanup(JNIEnv* env, jobject thiz) {
    jfieldID fidNumber = NULL;
    JpegReader* r_ptr = getRPtr(env, thiz, &fidNumber);
    if (r_ptr == NULL) {
        return;
    }
    // Update environment
    r_ptr->updateEnv(env);
    // Destroy the reader object
    delete r_ptr;
    r_ptr = NULL;
    // Set the java field to null
    env->SetLongField(thiz, fidNumber, reinterpret_cast<jlong>(r_ptr));
}

static jint OutputStream_writeInputBytes(JNIEnv* env, jobject thiz,
        jbyteArray inBuffer, jint offset, jint inCount) {
    JpegWriter* w_ptr = getWPtr(env, thiz, NULL);
    if (w_ptr == NULL) {
        return J_EXCEPTION;
    }
    // Pin input buffer
    jbyte* in_buf = (jbyte*) env->GetByteArrayElements(inBuffer, 0);
    if (env->ExceptionCheck() || in_buf == NULL) {
        return J_EXCEPTION;
    }

    int8_t* in_bytes = static_cast<int8_t*>(in_buf);
    int32_t in_len = static_cast<int32_t>(inCount);
    int32_t off = static_cast<int32_t>(offset);
    in_bytes += off;
    int32_t written = 0;

    // Update environment
    w_ptr->updateEnv(env);
    // Write out and unpin buffer.
    written = w_ptr->write(in_bytes, in_len);
    env->ReleaseByteArrayElements(inBuffer, in_buf, JNI_ABORT);
    return written;
}

static jint InputStream_readDecodedBytes(JNIEnv* env, jobject thiz,
        jbyteArray inBuffer, jint offset, jint inCount) {
    JpegReader* r_ptr = getRPtr(env, thiz, NULL);
    if (r_ptr == NULL) {
        return J_EXCEPTION;
    }
    // Pin input buffer
    jbyte* in_buf = (jbyte*) env->GetByteArrayElements(inBuffer, 0);
    if (env->ExceptionCheck() || in_buf == NULL) {
        return J_EXCEPTION;
    }
    int8_t* in_bytes = static_cast<int8_t*>(in_buf);
    int32_t in_len = static_cast<int32_t>(inCount);
    int32_t off = static_cast<int32_t>(offset);
    int32_t read = 0;

    // Update environment
    r_ptr->updateEnv(env);
    // Read into buffer
    read = r_ptr->read(in_bytes, off, in_len);

    // Unpin buffer
    if (read < 0) {
        env->ReleaseByteArrayElements(inBuffer, in_buf, JNI_ABORT);
    } else {
        env->ReleaseByteArrayElements(inBuffer, in_buf, JNI_COMMIT);
    }
    return read;
}

static jint InputStream_skipDecodedBytes(JNIEnv* env, jobject thiz,
        jint bytes) {
    if (bytes <= 0) {
        return J_ERROR_BAD_ARGS;
    }
    JpegReader* r_ptr = getRPtr(env, thiz, NULL);
    if (r_ptr == NULL) {
        return J_EXCEPTION;
    }

    // Update environment
    r_ptr->updateEnv(env);
    int32_t skip = 0;
    // Read with null buffer to skip
    skip = r_ptr->read(NULL, 0, bytes);
    return skip;
}

static const char *outClassPathName =
        "com/android/gallery3d/jpegstream/JPEGOutputStream";
static const char *inClassPathName =
        "com/android/gallery3d/jpegstream/JPEGInputStream";

static JNINativeMethod writeMethods[] = { { "setup",
        "(Ljava/io/OutputStream;IIII)I", (void*) OutputStream_setup }, {
        "cleanup", "()V", (void*) OutputStream_cleanup }, { "writeInputBytes",
        "([BII)I", (void*) OutputStream_writeInputBytes } };

static JNINativeMethod readMethods[] = { { "setup",
        "(Landroid/graphics/Point;Ljava/io/InputStream;I)I",
        (void*) InputStream_setup }, { "cleanup", "()V",
        (void*) InputStream_cleanup }, { "readDecodedBytes", "([BII)I",
        (void*) InputStream_readDecodedBytes }, { "skipDecodedBytes", "(I)I",
        (void*) InputStream_skipDecodedBytes } };

static int registerNativeMethods(JNIEnv* env, const char* className,
        JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Error: GetEnv failed in JNI_OnLoad");
        return -1;
    }
    if (!registerNativeMethods(env, outClassPathName, writeMethods,
            sizeof(writeMethods) / sizeof(writeMethods[0]))) {
        LOGE("Error: could not register native methods for JPEGOutputStream");
        return -1;
    }
    if (!registerNativeMethods(env, inClassPathName, readMethods,
            sizeof(readMethods) / sizeof(readMethods[0]))) {
        LOGE("Error: could not register native methods for JPEGInputStream");
        return -1;
    }
    // cache method IDs for OutputStream
    jclass outCls = env->FindClass("java/io/OutputStream");
    if (outCls == NULL) {
        LOGE("Unable to find class 'OutputStream'");
        return -1;
    }
    jmethodID cachedWriteFun = env->GetMethodID(outCls, "write", "([BII)V");
    if (cachedWriteFun == NULL) {
        LOGE("Unable to find write function in class 'OutputStream'");
        return -1;
    }
    OutputStreamWrapper::setWriteMethodID(cachedWriteFun);

    // cache method IDs for InputStream
    jclass inCls = env->FindClass("java/io/InputStream");
    if (inCls == NULL) {
        LOGE("Unable to find class 'InputStream'");
        return -1;
    }
    jmethodID cachedReadFun = env->GetMethodID(inCls, "read", "([BII)I");
    if (cachedReadFun == NULL) {
        LOGE("Unable to find read function in class 'InputStream'");
        return -1;
    }
    jmethodID cachedSkipFun = env->GetMethodID(inCls, "skip", "(J)J");
    if (cachedSkipFun == NULL) {
        LOGE("Unable to find skip function in class 'InputStream'");
        return -1;
    }
    InputStreamWrapper::setReadSkipMethodIDs(cachedReadFun, cachedSkipFun);
    return JNI_VERSION_1_6;
}

#ifdef __cplusplus
}
#endif
