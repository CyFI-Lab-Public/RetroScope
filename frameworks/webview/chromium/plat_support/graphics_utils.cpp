/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Provides a webviewchromium glue layer adapter from the internal Android
// graphics types into the types the chromium stack expects, and back.

#define LOG_TAG "webviewchromium_plat_support"

#include "android_webview/public/browser/draw_gl.h"
#include "android_webview/public/browser/draw_sw.h"

#include <cstdlib>
#include <jni.h>
#include <utils/Log.h>
#include <utils/UniquePtr.h>
#include "graphic_buffer_impl.h"
#include "GraphicsJNI.h"
#include "SkCanvasStateUtils.h"
#include "SkGraphics.h"
#include "SkPicture.h"

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

namespace android {
namespace {

class PixelInfo : public AwPixelInfo {
 public:
  PixelInfo(SkCanvas* canvas);
  ~PixelInfo();
};


PixelInfo::PixelInfo(SkCanvas* canvas) {
  memset(this, 0, sizeof(AwPixelInfo));
  version = kAwPixelInfoVersion;
  state = SkCanvasStateUtils::CaptureCanvasState(canvas);
}

PixelInfo::~PixelInfo() {
  if (state)
    SkCanvasStateUtils::ReleaseCanvasState(state);
}

AwPixelInfo* GetPixels(JNIEnv* env, jobject java_canvas) {
  SkCanvas* canvas = GraphicsJNI::getNativeCanvas(env, java_canvas);
  if (!canvas)
    return NULL;

  // Workarounds for http://crbug.com/271096: SW draw only supports
  // translate & scale transforms, and a simple rectangular clip.
  // (This also avoids significant wasted time in calling
  // SkCanvasStateUtils::CaptureCanvasState when the clip is complex).
  if (!canvas->getTotalClip().isRect() ||
      (canvas->getTotalMatrix().getType() &
                ~(SkMatrix::kTranslate_Mask | SkMatrix::kScale_Mask))) {
    return NULL;
  }

  UniquePtr<PixelInfo> pixels(new PixelInfo(canvas));
  return pixels->state ? pixels.release() : NULL;
}

void ReleasePixels(AwPixelInfo* pixels) {
  delete static_cast<PixelInfo*>(pixels);
}

jobject CreatePicture(JNIEnv* env, SkPicture* picture) {
  jclass clazz = env->FindClass("android/graphics/Picture");
  jmethodID constructor = env->GetMethodID(clazz, "<init>", "(IZ)V");
  ALOG_ASSERT(clazz);
  ALOG_ASSERT(constructor);
  return env->NewObject(clazz, constructor, picture, false);
}

bool IsSkiaVersionCompatible(SkiaVersionFunction function) {
  bool compatible = false;
  if (function && function == &SkGraphics::GetVersion) {
    int android_major, android_minor, android_patch;
    SkGraphics::GetVersion(&android_major, &android_minor, &android_patch);

    int chromium_major, chromium_minor, chromium_patch;
    (*function)(&chromium_major, &chromium_minor, &chromium_patch);

    compatible = android_major == chromium_major &&
                 android_minor == chromium_minor &&
                 android_patch == chromium_patch;
  }
  return compatible;
}

jint GetDrawSWFunctionTable(JNIEnv* env, jclass) {
  static const AwDrawSWFunctionTable function_table = {
      &GetPixels,
      &ReleasePixels,
      &CreatePicture,
      &IsSkiaVersionCompatible,
  };
  return reinterpret_cast<jint>(&function_table);
}

jint GetDrawGLFunctionTable(JNIEnv* env, jclass) {
  static const AwDrawGLFunctionTable function_table = {
    &GraphicBufferImpl::Create,
    &GraphicBufferImpl::Release,
    &GraphicBufferImpl::MapStatic,
    &GraphicBufferImpl::UnmapStatic,
    &GraphicBufferImpl::GetNativeBufferStatic,
    &GraphicBufferImpl::GetStrideStatic,
  };
  return reinterpret_cast<jint>(&function_table);
}

const char kClassName[] = "com/android/webview/chromium/GraphicsUtils";
const JNINativeMethod kJniMethods[] = {
    { "nativeGetDrawSWFunctionTable", "()I",
        reinterpret_cast<void*>(GetDrawSWFunctionTable) },
    { "nativeGetDrawGLFunctionTable", "()I",
        reinterpret_cast<void*>(GetDrawGLFunctionTable) },
};

}  // namespace

void RegisterGraphicsUtils(JNIEnv* env) {
  jclass clazz = env->FindClass(kClassName);
  LOG_ALWAYS_FATAL_IF(!clazz, "Unable to find class '%s'", kClassName);

  int res = env->RegisterNatives(clazz, kJniMethods, NELEM(kJniMethods));
  LOG_ALWAYS_FATAL_IF(res < 0, "register native methods failed: res=%d", res);
}

}  // namespace android
