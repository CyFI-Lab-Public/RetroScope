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

#include <gtest/gtest.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <GLTestHelper.h>


namespace android {

class GLTest : public ::testing::Test {

protected:

    GLTest():
            mEglDisplay(EGL_NO_DISPLAY),
            mEglSurface(EGL_NO_SURFACE),
            mEglContext(EGL_NO_CONTEXT) {
    }


   virtual void SetUp() {
        mEglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        ASSERT_EQ(EGL_SUCCESS, eglGetError());
        ASSERT_NE(EGL_NO_DISPLAY, mEglDisplay);

        EGLint majorVersion;
        EGLint minorVersion;
        EXPECT_TRUE(eglInitialize(mEglDisplay, &majorVersion, &minorVersion));
        ASSERT_EQ(EGL_SUCCESS, eglGetError());

        EGLint numConfigs = 0;
        EXPECT_TRUE(eglChooseConfig(mEglDisplay, getConfigAttribs(), &mGlConfig,
                1, &numConfigs));
        ASSERT_EQ(EGL_SUCCESS, eglGetError());

        char* displaySecsEnv = getenv("GLTEST_DISPLAY_SECS");
        if (displaySecsEnv != NULL) {
            mDisplaySecs = atoi(displaySecsEnv);
            if (mDisplaySecs < 0) {
                mDisplaySecs = 0;
            }
        } else {
            mDisplaySecs = 0;
        }

        if (mDisplaySecs > 0) {
            mEglSurface = eglCreateWindowSurface(mEglDisplay, mGlConfig,
                    GLTestHelper::getWindow(), NULL);
        } else {
            EGLint pbufferAttribs[] = {
                EGL_WIDTH, getSurfaceWidth(),
                EGL_HEIGHT, getSurfaceHeight(),
                EGL_NONE };

            mEglSurface = eglCreatePbufferSurface(mEglDisplay, mGlConfig,
                    pbufferAttribs);
        }
        ASSERT_EQ(EGL_SUCCESS, eglGetError());
        ASSERT_NE(EGL_NO_SURFACE, mEglSurface);

        mEglContext = eglCreateContext(mEglDisplay, mGlConfig, EGL_NO_CONTEXT,
                getContextAttribs());
        ASSERT_EQ(EGL_SUCCESS, eglGetError());
        ASSERT_NE(EGL_NO_CONTEXT, mEglContext);

        EXPECT_TRUE(eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface,
                mEglContext));
        ASSERT_EQ(EGL_SUCCESS, eglGetError());

        EGLint w, h;
        EXPECT_TRUE(eglQuerySurface(mEglDisplay, mEglSurface, EGL_WIDTH, &w));
        ASSERT_EQ(EGL_SUCCESS, eglGetError());
        EXPECT_TRUE(eglQuerySurface(mEglDisplay, mEglSurface, EGL_HEIGHT, &h));
        ASSERT_EQ(EGL_SUCCESS, eglGetError());

        glViewport(0, 0, w, h);
        ASSERT_EQ(GLenum(GL_NO_ERROR), glGetError());
    }

    virtual void TearDown() {
        // Display the result
        if (mDisplaySecs > 0 && mEglSurface != EGL_NO_SURFACE) {
            eglSwapBuffers(mEglDisplay, mEglSurface);
            sleep(mDisplaySecs);
        }

        if (mEglContext != EGL_NO_CONTEXT) {
            eglDestroyContext(mEglDisplay, mEglContext);
        }
        if (mEglSurface != EGL_NO_SURFACE) {
            eglDestroySurface(mEglDisplay, mEglSurface);
        }
        if (mEglDisplay != EGL_NO_DISPLAY) {
            eglMakeCurrent(mEglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE,
                    EGL_NO_CONTEXT);
            eglTerminate(mEglDisplay);
        }
        ASSERT_EQ(EGL_SUCCESS, eglGetError());
    }

    virtual EGLint const* getConfigAttribs() {
        static EGLint sDefaultConfigAttribs[] = {
            EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 16,
            EGL_STENCIL_SIZE, 8,
            EGL_NONE };

        return sDefaultConfigAttribs;
    }

    virtual EGLint const* getContextAttribs() {
        static EGLint sDefaultContextAttribs[] = {
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL_NONE };

        return sDefaultContextAttribs;
    }

    virtual EGLint getSurfaceWidth() {
        return 512;
    }

    virtual EGLint getSurfaceHeight() {
        return 512;
    }

    bool checkPixel(GLubyte * actual, GLubyte * expected, int tolerance) {
        for (int i = 0; i < 4; i++) {
            if (abs(actual[i] - expected[i]) > tolerance) {
                return false;
            }
        }
        return true;
    }

    ::testing::AssertionResult AssertPixel(const char* a_expr,
            const char* e_expr, const char* t_expr, GLubyte * actual,
            GLubyte * expected, int tolerance) {

        if (checkPixel(actual, expected, tolerance)) {
            return ::testing::AssertionSuccess();
        }

        return ::testing::AssertionFailure()
            << "Pixel comparison failed with tolerance " << tolerance << "\n"
            << "Actual: r=" << (int)actual[0] << " g=" << (int)actual[1]
            << " b=" << (int)actual[2] << " a=" << (int)actual[3] << "\n"
            << "Expected: r=" << (int)expected[0] << " g=" << (int)expected[1]
            << " b=" << (int)expected[2] << " a=" << (int)expected[3] << "\n";
    }

    int mDisplaySecs;

    EGLDisplay mEglDisplay;
    EGLSurface mEglSurface;
    EGLContext mEglContext;
    EGLConfig  mGlConfig;
};

TEST_F(GLTest, ClearColorTest) {
    glClearColor(0.2, 0.2, 0.2, 0.2);
    glClear(GL_COLOR_BUFFER_BIT);
    GLubyte expected[4] = { 51, 51, 51, 51 };
    GLubyte pixel[4];
    glReadPixels(0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
    ASSERT_EQ(GLenum(GL_NO_ERROR), glGetError());
    ASSERT_PRED_FORMAT3(AssertPixel, pixel, expected, 2);
}

} // namespace android
