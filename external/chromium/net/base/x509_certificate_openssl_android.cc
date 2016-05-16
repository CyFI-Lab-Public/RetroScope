// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "net/base/x509_certificate.h"

#include "base/logging.h"
#include "net/base/android_network_library.h"
#include "net/base/cert_status_flags.h"
#include "net/base/cert_verify_result.h"
#include "net/base/net_errors.h"

namespace net {

int X509Certificate::Verify(const std::string& hostname,
                            int flags,
                            CertVerifyResult* verify_result) const {
  verify_result->Reset();

  AndroidNetworkLibrary* lib = AndroidNetworkLibrary::GetSharedInstance();
  if (!lib) {
    LOG(ERROR) << "Rejecting verify as no net library installed";
    verify_result->cert_status |= ERR_CERT_INVALID;
    return MapCertStatusToNetError(verify_result->cert_status);
  }

  OSCertHandles cert_handles(intermediate_ca_certs_);
  // Make sure the peer's own cert is the first in the chain, if it's not
  // already there.
  if (cert_handles.empty() || cert_handles[0] != cert_handle_)
    cert_handles.insert(cert_handles.begin(), cert_handle_);

  std::vector<std::string> cert_bytes;
  cert_bytes.reserve(cert_handles.size());
  for (OSCertHandles::const_iterator it = cert_handles.begin();
       it != cert_handles.end(); ++it) {
    cert_bytes.push_back(GetDEREncodedBytes(*it));
  }


  if (IsPublicKeyBlacklisted(verify_result->public_key_hashes)) {
    verify_result->cert_status |= CERT_STATUS_AUTHORITY_INVALID;
    return MapCertStatusToNetError(verify_result->cert_status);
  }

  // TODO(joth): Fetch the authentication type from SSL rather than hardcode.
  AndroidNetworkLibrary::VerifyResult result =
      lib->VerifyX509CertChain(cert_bytes, hostname, "RSA");
  switch (result) {
    case AndroidNetworkLibrary::VERIFY_OK:
      return OK;
    case AndroidNetworkLibrary::VERIFY_BAD_HOSTNAME:
      verify_result->cert_status |= CERT_STATUS_COMMON_NAME_INVALID;
      break;
    case AndroidNetworkLibrary::VERIFY_NO_TRUSTED_ROOT:
      verify_result->cert_status |= CERT_STATUS_AUTHORITY_INVALID;
      break;
    case AndroidNetworkLibrary::VERIFY_INVOCATION_ERROR:
    default:
      verify_result->cert_status |= ERR_CERT_INVALID;
      break;
  }
  return MapCertStatusToNetError(verify_result->cert_status);
}

}  // namespace net

