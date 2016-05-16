// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef NET_BASE_ANDROID_NETWORK_LIBRARY_H_
#define NET_BASE_ANDROID_NETWORK_LIBRARY_H_
#pragma once

#include <string>
#include <vector>

#include "base/basictypes.h"
#include "net/base/net_export.h"

namespace net {

class NET_EXPORT AndroidNetworkLibrary {
 public:
  static void RegisterSharedInstance(AndroidNetworkLibrary* lib);
  static void UnregisterSharedInstance();
  static AndroidNetworkLibrary* GetSharedInstance();

  enum VerifyResult {
    VERIFY_OK,
    VERIFY_BAD_HOSTNAME,
    VERIFY_NO_TRUSTED_ROOT,
    VERIFY_INVOCATION_ERROR,
  };
  // |cert_chain| is DER encoded chain of certificates, with the server's own
  // certificate listed first.
  // |hostname| is validated against the supplied cert. |auth_type| is as per
  // the Java X509Certificate.checkServerTrusted method.
  virtual VerifyResult VerifyX509CertChain(
      const std::vector<std::string>& cert_chain,
      const std::string& hostname,
      const std::string& auth_type) = 0;

 protected:
  friend class LibHolder;
  AndroidNetworkLibrary();
  virtual ~AndroidNetworkLibrary();  // use UnregisterSharedInstance()

  DISALLOW_COPY_AND_ASSIGN(AndroidNetworkLibrary);
};

}  // namespace net

#endif  // NET_BASE_ANDROID_NETWORK_LIBRARY_H_
