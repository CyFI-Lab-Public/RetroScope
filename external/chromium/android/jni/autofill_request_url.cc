// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "android/jni/autofill_request_url.h"
#include "android/jni/jni_utils.h"

namespace android {

std::string AutofillRequestUrl::GetQueryUrl() {
  JNIEnv* env = android::jni::GetJNIEnv();
  jclass bridgeClass = env->FindClass("android/webkit/JniUtil");
  jmethodID method = env->GetStaticMethodID(bridgeClass, "getAutofillQueryUrl", "()Ljava/lang/String;");
  jstring autofill_query_url = static_cast<jstring>(env->CallStaticObjectMethod(bridgeClass, method));
  std::string request_url = android::jni::JstringToStdString(env, autofill_query_url);
  env->DeleteLocalRef(autofill_query_url);
  env->DeleteLocalRef(bridgeClass);

  return request_url;
}

} // namespace android
