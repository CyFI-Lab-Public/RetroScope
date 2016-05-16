// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "android_webview/browser/gpu_memory_handle_impl.h"

#include "android_webview/public/browser/draw_gl.h"
#include "base/basictypes.h"

namespace android_webview {

namespace {

// Provides hardware rendering functions from the Android glue layer.
AwDrawGLFunctionTable* g_gl_draw_functions = NULL;
}

GpuMemoryHandleImpl::GpuMemoryHandleImpl(int buffer_id)
    : buffer_id_(buffer_id) {
}

GpuMemoryHandleImpl::~GpuMemoryHandleImpl() {
  g_gl_draw_functions->release_graphic_buffer(buffer_id_);
}

void* GpuMemoryHandleImpl::GetNativeHandle() const {
  return g_gl_draw_functions->get_native_buffer(buffer_id_);
}

int GpuMemoryHandleImpl::GetBufferId() const {
  return buffer_id_;
}

// static
void GpuMemoryHandleImpl::SetAwDrawGLFunctionTable(
    AwDrawGLFunctionTable* table) {
  g_gl_draw_functions = table;
}

}  // namespace android_webview
