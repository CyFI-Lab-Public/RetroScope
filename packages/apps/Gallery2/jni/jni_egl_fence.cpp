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

#include "jni_egl_fence.h"

#include <android/log.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <string.h>

#define  ALOGE(...)  __android_log_print(ANDROID_LOG_ERROR,"egl_fence",__VA_ARGS__)

typedef EGLSyncKHR EGLAPIENTRY (*TypeEglCreateSyncKHR)(EGLDisplay dpy,
    EGLenum type, const EGLint *attrib_list);
typedef EGLBoolean EGLAPIENTRY (*TypeEglDestroySyncKHR)(EGLDisplay dpy,
    EGLSyncKHR sync);
typedef EGLint EGLAPIENTRY (*TypeEglClientWaitSyncKHR)(EGLDisplay dpy,
    EGLSyncKHR sync, EGLint flags, EGLTimeKHR timeout);
static TypeEglCreateSyncKHR FuncEglCreateSyncKHR = NULL;
static TypeEglClientWaitSyncKHR FuncEglClientWaitSyncKHR = NULL;
static TypeEglDestroySyncKHR FuncEglDestroySyncKHR = NULL;
static bool initialized = false;
static bool egl_khr_fence_sync_supported = false;

bool IsEglKHRFenceSyncSupported() {
  if (!initialized) {
    EGLDisplay display = eglGetCurrentDisplay();
    const char* eglExtensions = eglQueryString(eglGetCurrentDisplay(), EGL_EXTENSIONS);
    if (eglExtensions && strstr(eglExtensions, "EGL_KHR_fence_sync")) {
      FuncEglCreateSyncKHR = (TypeEglCreateSyncKHR) eglGetProcAddress("eglCreateSyncKHR");
      FuncEglClientWaitSyncKHR = (TypeEglClientWaitSyncKHR) eglGetProcAddress("eglClientWaitSyncKHR");
      FuncEglDestroySyncKHR = (TypeEglDestroySyncKHR) eglGetProcAddress("eglDestroySyncKHR");
      if (FuncEglCreateSyncKHR != NULL && FuncEglClientWaitSyncKHR != NULL
          && FuncEglDestroySyncKHR != NULL) {
        egl_khr_fence_sync_supported = true;
      }
    }
    initialized = true;
  }
  return egl_khr_fence_sync_supported;
}

void
Java_com_android_gallery3d_photoeditor_FilterStack_nativeEglSetFenceAndWait(JNIEnv* env,
                                                                          jobject thiz) {
  if (!IsEglKHRFenceSyncSupported()) return;
  EGLDisplay display = eglGetCurrentDisplay();

  // Create a egl fence and wait for egl to return it.
  // Additional reference on egl fence sync can be found in:
  // http://www.khronos.org/registry/vg/extensions/KHR/EGL_KHR_fence_sync.txt
  EGLSyncKHR fence = FuncEglCreateSyncKHR(display, EGL_SYNC_FENCE_KHR, NULL);
  if (fence == EGL_NO_SYNC_KHR) {
    return;
  }

  EGLint result = FuncEglClientWaitSyncKHR(display,
                                       fence,
                                       EGL_SYNC_FLUSH_COMMANDS_BIT_KHR,
                                       EGL_FOREVER_KHR);
  if (result == EGL_FALSE) {
    ALOGE("EGL FENCE: error waiting for fence: %#x", eglGetError());
  }
  FuncEglDestroySyncKHR(display, fence);
}
