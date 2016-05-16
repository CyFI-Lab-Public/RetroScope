// Copyright 2012 Google Inc. All Rights Reserved.
// Author: sameeragarwal@google.com (Sameer Agarwal)
//
// This shim file serves two purposes.
//
// 1. Translate the gflags includes used by the OSS version of Ceres
// so that it links into the google3 version.
//
// 2. Call InitGoogle when ParseCommandLineFlags is called. This is
// needed because while google3 binaries call InitGoogle and that call
// initializes the logging and command line handling amongst other
// things, the open source versions of gflags and glog are distributed
// separately and require separate initialization. By hijacking this
// function, and calling InitGoogle, we can compile all the example
// code that ships with Ceres without any modifications. This
// modification will have no impact on google3 binaries using Ceres,
// as they will never call google::ParseCommandLineFlags.

#ifndef GFLAGS_GFLAGS_H_
#define GFLAGS_GFLAGS_H_

#include "base/init_google.h"
#include "base/commandlineflags.h"

namespace google {

inline void ParseCommandLineFlags(int* argc,
                                  char*** argv,
                                  const bool remove_flags) {
  InitGoogle(**argv, argc, argv, remove_flags);
}

}  // namespace google

#endif  // GFLAGS_GFLAGS_H_
