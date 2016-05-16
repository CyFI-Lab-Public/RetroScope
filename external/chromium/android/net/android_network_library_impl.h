// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef ANDROID_NET_ANDROID_NETWORK_LIBRARY_IMPL_H_
#define ANDROID_NET_ANDROID_NETWORK_LIBRARY_IMPL_H_

#pragma once

#include <jni.h>

#include <string>
#include <vector>

#include "net/base/android_network_library.h"
#include "net/base/net_export.h"

class NET_EXPORT AndroidNetworkLibraryImpl : public net::AndroidNetworkLibrary {
 public:
  static void InitWithApplicationContext(JNIEnv* env, jobject context);

  virtual VerifyResult VerifyX509CertChain(
      const std::vector<std::string>& cert_chain,
      const std::string& hostname,
      const std::string& auth_type);

 private:
  explicit AndroidNetworkLibraryImpl(JNIEnv* env);
  virtual ~AndroidNetworkLibraryImpl();

  jclass cert_verifier_class_;

  DISALLOW_COPY_AND_ASSIGN(AndroidNetworkLibraryImpl);
};

#endif  // ANDROID_NET_ANDROID_NETWORK_LIBRARY_IMPL_H_
