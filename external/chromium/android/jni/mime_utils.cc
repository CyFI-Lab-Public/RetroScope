// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "android/jni/mime_utils.h"

#include "android/jni/jni_utils.h"

// Accessors for the various JNI binding objects.
namespace {

static jclass get_class(JNIEnv* env) {
  static jclass clazz = env->FindClass("libcore/net/MimeUtils");
  return clazz;
}

static jmethodID guess_mime_type_from_extension(JNIEnv* env) {
  static jmethodID method = env->GetStaticMethodID(get_class(env),
      "guessMimeTypeFromExtension", "(Ljava/lang/String;)Ljava/lang/String;");
  return method;
}

static jmethodID guess_extension_from_mime_type(JNIEnv* env) {
  static jmethodID method = env->GetStaticMethodID(get_class(env),
      "guessExtensionFromMimeType", "(Ljava/lang/String;)Ljava/lang/String;");
  return method;
}

} // namespace

namespace android {

bool MimeUtils::GuessMimeTypeFromExtension(const std::string& extension,
    std::string* result) {
  JNIEnv* env = jni::GetJNIEnv();
  jstring jExtension = env->NewStringUTF(extension.c_str());
  jobject jResult = env->CallStaticObjectMethod(
      get_class(env), guess_mime_type_from_extension(env), jExtension);
  env->DeleteLocalRef(jExtension);
  if (jResult) {
    *result = jni::JstringToStdString(env, static_cast<jstring>(jResult));
    env->DeleteLocalRef(jResult);
  }
  return jResult;
}

bool MimeUtils::GuessExtensionFromMimeType(const std::string& mimeType,
    std::string* result) {
  JNIEnv* env = jni::GetJNIEnv();
  jstring jMimeType = env->NewStringUTF(mimeType.c_str());
  jobject jResult = env->CallStaticObjectMethod(
      get_class(env), guess_extension_from_mime_type(env), jMimeType);
  env->DeleteLocalRef(jMimeType);
  if (jResult) {
    *result = jni::JstringToStdString(env, static_cast<jstring>(jResult));
    env->DeleteLocalRef(jResult);
  }
  return jResult;
}

} // namespace android

