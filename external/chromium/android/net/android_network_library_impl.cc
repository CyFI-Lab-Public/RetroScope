// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "android/net/android_network_library_impl.h"

#include "base/logging.h"
#include "android/jni/jni_utils.h"

using namespace android;

namespace {

const char* const kClassPathName = "android/net/http/CertificateChainValidator";

// Convert X509 chain to DER format bytes.
jobjectArray GetCertificateByteArray(
    JNIEnv* env,
    const std::vector<std::string> cert_chain) {
  size_t count = cert_chain.size();
  DCHECK_GT(count, 0U);
  // TODO(joth): See if we can centrally cache common classes like this, e.g.
  // as JniConstants does.
  jclass byte_array_class = env->FindClass("[B");
  jobjectArray joa = env->NewObjectArray(count, byte_array_class, NULL);
  if (joa == NULL)
    return NULL;

  for (size_t i = 0; i < count; ++i) {
    size_t len = cert_chain[i].length();

    jbyteArray byte_array = env->NewByteArray(len);
    if (!byte_array) {
      env->DeleteLocalRef(joa);
      return NULL;
    }

    jbyte* bytes = env->GetByteArrayElements(byte_array, NULL);
    DCHECK(bytes);
    size_t copied = cert_chain[i].copy(reinterpret_cast<char*>(bytes), len);
    DCHECK_EQ(copied, len);
    env->ReleaseByteArrayElements(byte_array, bytes, 0);
    env->SetObjectArrayElement(joa, i, byte_array);
    env->DeleteLocalRef(byte_array);
  }
  return joa;
}

}  // namespace

AndroidNetworkLibraryImpl::VerifyResult
    AndroidNetworkLibraryImpl::VerifyX509CertChain(
        const std::vector<std::string>& cert_chain,
        const std::string& hostname,
        const std::string& auth_type) {
  if (!cert_verifier_class_)
    return VERIFY_INVOCATION_ERROR;

  JNIEnv* env = jni::GetJNIEnv();
  DCHECK(env);

  static jmethodID verify_fn = env->GetStaticMethodID(
      cert_verifier_class_, "verifyServerCertificates",
      "([[BLjava/lang/String;Ljava/lang/String;)Landroid/net/http/SslError;");
  if (jni::CheckException(env)) {
    LOG(ERROR) << "verifyServerCertificates method not found; skipping";
    return VERIFY_INVOCATION_ERROR;
  }
  DCHECK(verify_fn);

  jobjectArray chain_byte_array = GetCertificateByteArray(env, cert_chain);
  if (!chain_byte_array)
    return VERIFY_INVOCATION_ERROR;

  jstring host_string = jni::ConvertUTF8ToJavaString(env, hostname);
  DCHECK(host_string);
  jstring auth_string = jni::ConvertUTF8ToJavaString(env, auth_type);
  DCHECK(auth_string);

  jobject error = env->CallStaticObjectMethod(cert_verifier_class_, verify_fn,
                                              chain_byte_array, host_string,
                                              auth_string);
  env->DeleteLocalRef(chain_byte_array);
  env->DeleteLocalRef(host_string);
  env->DeleteLocalRef(auth_string);

  VerifyResult result = VERIFY_INVOCATION_ERROR;
  if (!jni::CheckException(env)) {
    if (!error) {
      result = VERIFY_OK;
    } else {
      jclass error_class = env->GetObjectClass(error);
      DCHECK(error_class);
      static jmethodID error_fn = env->GetMethodID(error_class,
                                                   "getPrimaryError", "()I");
      if (error_fn) {
        int code = env->CallIntMethod(error, error_fn);
        if (!jni::CheckException(env)) {
          if (code == 2) {  // SSL_IDMISMATCH == 2
            result = VERIFY_BAD_HOSTNAME;
          } else if (code == 3) {  // SSL_UNTRUSTED == 3
            result = VERIFY_NO_TRUSTED_ROOT;
          }
        }
      }
      env->DeleteLocalRef(error);
    }
  } else {
    // an uncaught exception has happened in java code, clear it and return
    // a proper error
    env->ExceptionClear();
    result = VERIFY_INVOCATION_ERROR;
  }
  // TODO(joth): This balances the GetJNIEnv call; we need to detach as
  // currently this method is called in chrome from a worker pool thread that
  // may shutdown at anytime. However this assumption should not be baked in
  // here: another user of the function may not want to have their thread
  // detached at this point.
  jni::DetachFromVM();
  return result;
}

// static
void AndroidNetworkLibraryImpl::InitWithApplicationContext(JNIEnv* env,
                                                           jobject context) {
  // Currently ignoring |context| as it is not needed (but remains in signature
  // for API consistency with the equivalent method on class AndroidOS).
  if (!net::AndroidNetworkLibrary::GetSharedInstance())
    net::AndroidNetworkLibrary::RegisterSharedInstance(
        new AndroidNetworkLibraryImpl(env));
}

AndroidNetworkLibraryImpl::AndroidNetworkLibraryImpl(JNIEnv* env)
    : cert_verifier_class_(NULL) {
  jclass cls = env->FindClass(kClassPathName);
  if (jni::CheckException(env) || !cls) {
      NOTREACHED() << "Unable to load class " << kClassPathName;
  } else {
    cert_verifier_class_ = static_cast<jclass>(env->NewGlobalRef(cls));
    env->DeleteLocalRef(cls);
  }
}

AndroidNetworkLibraryImpl::~AndroidNetworkLibraryImpl() {
  if (cert_verifier_class_)
    jni::GetJNIEnv()->DeleteGlobalRef(cert_verifier_class_);
}

