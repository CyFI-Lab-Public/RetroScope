// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "net/base/android_network_library.h"

#include "base/lazy_instance.h"
#include "base/logging.h"
#include "base/synchronization/lock.h"

using base::Lock;
using base::AutoLock;

namespace net {

class LibHolder {
 public:
  LibHolder() : lib_(NULL) {}
  ~LibHolder() {
    Reset();
  }
  void Register(AndroidNetworkLibrary* lib) {
    AutoLock lock(lock_);
    if (lib_) {
      LOG(WARNING) << "Ignoring duplicate call " << lib;
      delete lib;
      return;
    }
    lib_ = lib;
  }
  void Reset() {
    AutoLock lock(lock_);
    delete lib_;
    lib_ = NULL;
  }
  AndroidNetworkLibrary* GetLibrary() {
    AutoLock lock(lock_);
    return lib_;
  }

 private:
  AndroidNetworkLibrary* lib_;
  Lock lock_;
};

base::LazyInstance<LibHolder> g_holder(base::LINKER_INITIALIZED);

// static
void AndroidNetworkLibrary::RegisterSharedInstance(AndroidNetworkLibrary* lib) {
  g_holder.Get().Register(lib);
}

// static
void AndroidNetworkLibrary::UnregisterSharedInstance() {
  g_holder.Get().Reset();
}

// static
AndroidNetworkLibrary* AndroidNetworkLibrary::GetSharedInstance() {
  return g_holder.Get().GetLibrary();
}

AndroidNetworkLibrary::AndroidNetworkLibrary() {
}

AndroidNetworkLibrary::~AndroidNetworkLibrary() {
}

}  // namespace net
