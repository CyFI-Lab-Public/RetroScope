// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "android/jni/jni_utils.h"
#include "android/jni/platform_file_jni.h"
#include "base/file_path.h"
#include "base/logging.h"

namespace android {

JavaISWrapper::JavaISWrapper(const FilePath& path) {
  JNIEnv* env = jni::GetJNIEnv();
  jclass inputStreamClass = env->FindClass("java/io/InputStream");
  m_read = env->GetMethodID(inputStreamClass, "read", "([B)I");
  m_close = env->GetMethodID(inputStreamClass, "close", "()V");

  jclass bridgeClass = env->FindClass("android/webkit/JniUtil");
  jmethodID method = env->GetStaticMethodID(
      bridgeClass,
      "contentUrlStream",
      "(Ljava/lang/String;)Ljava/io/InputStream;");
  m_inputStream = env->NewGlobalRef(env->CallStaticObjectMethod(
      bridgeClass,
      method,
      jni::ConvertUTF8ToJavaString(env, path.value())));
  env->DeleteLocalRef(bridgeClass);
  env->DeleteLocalRef(inputStreamClass);
}

JavaISWrapper::~JavaISWrapper() {
  JNIEnv* env = jni::GetJNIEnv();
  env->CallVoidMethod(m_inputStream, m_close);
  jni::CheckException(env);
  env->DeleteGlobalRef(m_inputStream);
}

int JavaISWrapper::Read(char* out, int length) {
  JNIEnv* env = jni::GetJNIEnv();
  jbyteArray buffer = env->NewByteArray(length);

  int size = (int) env->CallIntMethod(m_inputStream, m_read, buffer);
  if (jni::CheckException(env) || size < 0) {
    env->DeleteLocalRef(buffer);
    return 0;
  }

  env->GetByteArrayRegion(buffer, 0, size, (jbyte*)out);
  env->DeleteLocalRef(buffer);
  return size;
}

uint64 contentUrlSize(const FilePath& name) {
  JNIEnv* env = jni::GetJNIEnv();
  jclass bridgeClass = env->FindClass("android/webkit/JniUtil");
  jmethodID method = env->GetStaticMethodID(
      bridgeClass,
      "contentUrlSize",
      "(Ljava/lang/String;)J");
  jlong length = env->CallStaticLongMethod(
      bridgeClass,
      method,
      jni::ConvertUTF8ToJavaString(env, name.value()));
  env->DeleteLocalRef(bridgeClass);

  return static_cast<uint64>(length);
}

}

