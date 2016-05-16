// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef UI_GFX_GPU_MEMORY_HANDLE_H_
#define UI_GFX_GPU_MEMORY_HANDLE_H_

#include "base/memory/ref_counted.h"
#include "ui/base/ui_export.h"

namespace gfx {

// A class that encapsulates the lifetime management of a
// GpuMemory and allows it to be shared across threads.
// It is useful when a thread that didn't allocate the memory
// needs to access the memory asynchronously. Even though the
// creator thread attempts to free the memory, as long as
// the accessor thread holds a reference the memory will not
// be freed.
class UI_EXPORT GpuMemoryHandle
    : public base::RefCountedThreadSafe<GpuMemoryHandle> {
 public:
  GpuMemoryHandle();
  virtual void* GetNativeHandle() const = 0;
  virtual int GetBufferId() const = 0;

 protected:
  friend class base::RefCountedThreadSafe<GpuMemoryHandle>;
  virtual ~GpuMemoryHandle();
};

}  // namespace gfx

#endif  // UI_GFX_GPU_MEMORY_HANDLE_H_
