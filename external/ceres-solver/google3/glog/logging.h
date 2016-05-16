// Copyright 2012 Google Inc. All Rights Reserved.
// Author: keir@google.com (Keir Mierle)
//
// This is a shim header that redirects the Ceres includes of "glog/logging.h"
// to the google3 logging headers.

#ifndef GLOG_LOGGING_H_
#define GLOG_LOGGING_H_

#include "base/logging.h"

namespace google {

inline void InitGoogleLogging(const char* argv0) {
  // The gflags shim in //third_party/ceres/google/gflags/gflags.h
  // already calls InitGoogle, which gets the logging initialized.
}

}  // namespace google

#endif  // GLOG_LOGGING_H_
