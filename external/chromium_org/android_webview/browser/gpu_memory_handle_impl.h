// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef ANDROID_WEBVIEW_BROWSER_GPU_MEMORY_HANDLE_IMPL_H_
#define ANDROID_WEBVIEW_BROWSER_GPU_MEMORY_HANDLE_IMPL_H_

#include "base/compiler_specific.h"
#include "ui/gfx/gpu_memory_handle.h"

struct AwDrawGLFunctionTable;

namespace android_webview {

class GpuMemoryHandleImpl : public gfx::GpuMemoryHandle {
 public:
  explicit GpuMemoryHandleImpl(int buffer_id);

  static void SetAwDrawGLFunctionTable(AwDrawGLFunctionTable* table);

  // Overridden from gfx::GpuMemoryHandleAndroid:
  virtual void* GetNativeHandle() const OVERRIDE;
  virtual int GetBufferId() const OVERRIDE;
 private:
  virtual ~GpuMemoryHandleImpl();
  int buffer_id_;
};

}  // namespace android_webview

#endif  // ANDROID_WEBVIEW_BROWSER_GPU_MEMORY_HANDLE_IMPL_H_
