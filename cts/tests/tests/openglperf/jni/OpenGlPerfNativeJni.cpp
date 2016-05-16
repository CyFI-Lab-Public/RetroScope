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

#include <EGL/egl.h>
#define EGL_EGLEXT_PROTOTYPES // for egl*Sync*
#include <EGL/eglext.h>
#include <cutils/log.h>
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

typedef EGLSyncKHR EGLAPIENTRY (*TypeEglCreateSyncKHR)(EGLDisplay dpy, \
    EGLenum type, const EGLint *attrib_list);
typedef EGLBoolean EGLAPIENTRY (*TypeEglDestroySyncKHR)(EGLDisplay dpy, \
    EGLSyncKHR sync);
typedef EGLint EGLAPIENTRY (*TypeEglClientWaitSyncKHR)(EGLDisplay dpy, \
    EGLSyncKHR sync, EGLint flags, EGLTimeKHR timeout);

static TypeEglCreateSyncKHR mEglCreateSyncKHR = NULL;
static TypeEglClientWaitSyncKHR mEglClientWaitSyncKHR = NULL;
static TypeEglDestroySyncKHR mEglDestroySyncKHR = NULL;
static bool mInitialized = false;
static bool mEglKhrFenceSyncSupported = false;

bool IsEglKHRFenceSyncSupported(EGLDisplay& display)
{
    if (!mInitialized) {
        const char* eglExtensions = eglQueryString(display, EGL_EXTENSIONS);
        if (eglExtensions && strstr(eglExtensions, "EGL_KHR_fence_sync")) {
            mEglCreateSyncKHR = (TypeEglCreateSyncKHR) eglGetProcAddress("eglCreateSyncKHR");
            mEglClientWaitSyncKHR =
                    (TypeEglClientWaitSyncKHR) eglGetProcAddress("eglClientWaitSyncKHR");
            mEglDestroySyncKHR = (TypeEglDestroySyncKHR) eglGetProcAddress("eglDestroySyncKHR");
            if (mEglCreateSyncKHR != NULL && mEglClientWaitSyncKHR != NULL
                    && mEglDestroySyncKHR != NULL) {
                mEglKhrFenceSyncSupported = true;
            }
        }
        mInitialized = true;
    }
    return mEglKhrFenceSyncSupported;
}

extern "C" JNIEXPORT \
jboolean JNICALL Java_android_openglperf_cts_OpenGlPerfNative_waitForEglCompletion(JNIEnv* env,
        jclass clazz, jlong waitTimeInNs)
{
    EGLDisplay dpy = eglGetCurrentDisplay();
    if (!IsEglKHRFenceSyncSupported(dpy)) {
        return JNI_FALSE;
    }
    EGLSyncKHR sync = mEglCreateSyncKHR(dpy, EGL_SYNC_FENCE_KHR, NULL);
    if (sync == EGL_NO_SYNC_KHR) {
        return JNI_FALSE;
    }
    jboolean res = JNI_TRUE;
    EGLint result = mEglClientWaitSyncKHR(dpy, sync, EGL_SYNC_FLUSH_COMMANDS_BIT_KHR, waitTimeInNs);
    if (result == EGL_FALSE) {
        ALOGE("FrameCompletion: error waiting for fence: %#x", eglGetError());
        res = JNI_FALSE;
    } else if (result == EGL_TIMEOUT_EXPIRED_KHR) {
        ALOGE("FrameCompletion: timeout waiting for fence");
        res = JNI_FALSE;
    }
    mEglDestroySyncKHR(dpy, sync);
    return res;
}
