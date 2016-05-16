/*
 * Copyright 2013 The Android Open Source Project
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

#include <android/native_window.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>

#include <gtest/gtest.h>

#include <android/log.h>

#include "GLTestHelper.h"

#define LOG_TAG "EGLCreateContext_test"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

namespace android {

static int getGlVersion() {
    const char* s = (const char*)glGetString(GL_VERSION);
    if (!s)
        return 0;
    int major, minor;
    if (sscanf(s, "OpenGL ES %d.%d", &major, &minor) != 2)
        return 0;
    return major;
}

class EGLCreateContextTest : public ::testing::Test {

protected:

    EGLCreateContextTest()
    :   mEglDisplay(EGL_NO_DISPLAY),
        mEglConfig(0),
        mEglWindowSurface(EGL_NO_SURFACE),
        mEglContext(EGL_NO_CONTEXT)
    {}

    virtual void SetUp() {
        // static const EGLint SURFACE_ATTRIBS[] = {
        //     EGL_NONE
        // };

        mEglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        ASSERT_NE(EGL_NO_DISPLAY, mEglDisplay);

        EGLint major, minor;
        ASSERT_TRUE(eglInitialize(mEglDisplay, &major, &minor));

        EGLint numConfigs = 0;
        ASSERT_TRUE(eglChooseConfig(mEglDisplay, getConfigAttribs(),
                &mEglConfig, 1, &numConfigs));
        ASSERT_GE(1, numConfigs);
        ASSERT_NE((EGLConfig)0, mEglConfig);

        mEglWindowSurface = eglCreateWindowSurface(mEglDisplay, mEglConfig,
                GLTestHelper::getWindow(), getWindowSurfaceAttribs());
        ASSERT_EQ(EGL_SUCCESS, eglGetError());
        ASSERT_NE(EGL_NO_SURFACE, mEglWindowSurface);
    }

    virtual void TearDown() {
        if (mEglContext != EGL_NO_CONTEXT) {
            eglDestroyContext(mEglDisplay, mEglContext);
        }
        if (mEglWindowSurface != EGL_NO_SURFACE) {
            eglDestroySurface(mEglDisplay, mEglWindowSurface);
        }
        if (mEglDisplay != EGL_NO_DISPLAY) {
            eglMakeCurrent(mEglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE,
                    EGL_NO_CONTEXT);
            eglTerminate(mEglDisplay);
        }
        ASSERT_EQ(EGL_SUCCESS, eglGetError());
    }

    virtual const EGLint* getConfigAttribs() {
        static const EGLint ATTRIBS[] = {
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_NONE
        };
        return ATTRIBS;
    }

    virtual const EGLint* getWindowSurfaceAttribs() {
        return NULL;
    }

    EGLDisplay mEglDisplay;
    EGLConfig  mEglConfig;
    EGLSurface mEglWindowSurface;
    EGLContext mEglContext;
};

TEST_F(EGLCreateContextTest, BadAttributeFails) {
    // First check that we can successfully create a context
    EGLint attribs[5] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE, EGL_NONE, EGL_NONE,
    };
    mEglContext = eglCreateContext(mEglDisplay, mEglConfig, EGL_NO_CONTEXT,
            attribs);
    ASSERT_NE(mEglContext, EGL_NO_CONTEXT);
    ASSERT_EQ(eglGetError(), EGL_SUCCESS);
    ASSERT_EQ(EGL_TRUE, eglDestroyContext(mEglDisplay, mEglContext));
    mEglContext = EGL_NO_CONTEXT;

    // Now add an invalid attribute and make sure eglCreateContext fails
    attribs[2] = EGL_BAD_ATTRIBUTE; // error code, not a valid attribute
    mEglContext = eglCreateContext(mEglDisplay, mEglConfig, EGL_NO_CONTEXT,
            attribs);
    ASSERT_EQ(mEglContext, EGL_NO_CONTEXT);
    ASSERT_EQ(eglGetError(), EGL_BAD_ATTRIBUTE);
}

} // namespace android
