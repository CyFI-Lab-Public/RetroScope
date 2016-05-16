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

#define LOG_TAG "EGLCleanup"
#include <android/log.h>
#include <jni.h>

#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <gtest/gtest.h>

#include <GLTestHelper.h>

#include <pthread.h>

#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace android {

/**
 * Tests EGL cleanup edge cases.
 */
class EGLCleanupTest : public ::testing::Test {
protected:
    EGLCleanupTest() {}

    virtual void SetUp() {
        // Termination of a terminated display is defined to be a no-op.
        // Android uses a refcounted implementation, so terminate it a few
        // times to make sure it's really dead.  Without this, we might not
        // get all the way into the driver eglTerminate implementation
        // when we call eglTerminate.
        EGLDisplay disp = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        if (disp != EGL_NO_DISPLAY) {
            ALOGD("speculative terminate");
            eglTerminate(disp);
            eglTerminate(disp);
            eglTerminate(disp);
        }
    }
    virtual void TearDown() {}
};

/**
 * Perform an operation and then start a new thread.
 *
 * The trick here is that some code may be helpfully releasing storage in
 * pthread_key destructors.  Those run after the thread returns out of the
 * initial function, but before the thread fully exits.  We want them to
 * run concurrently with the next thread's initialization so we can confirm
 * that the specified behavior of eglTerminate vs. eglInitialize holds.
 */
class ChainedThread {
public:
    enum TestType {
        TEST_CORRECT,
        TEST_NO_RELEASE_CURRENT
    };

    ChainedThread(TestType testType) : mEglDisplay(EGL_NO_DISPLAY),
            mEglSurface(EGL_NO_SURFACE), mEglContext(EGL_NO_CONTEXT),
            mTestType(testType), mIteration(0), mResult(true) {
        pthread_mutex_init(&mLock, NULL);
        pthread_cond_init(&mCond, NULL);
    }
    ~ChainedThread() {
        // could get fancy and clean up the mutex
    }

    /* start here */
    bool start() {
        lock();
        bool result = startThread_l();
        unlock();
        return result;
    }

    /* waits until test is done; when finished, call getResult() */
    bool waitForEnd() {
        lock();
        int err = pthread_cond_wait(&mCond, &mLock);
        if (err != 0) {
            ALOGW("pthread_cond_wait failed: %d", err);
        }
        unlock();
        return err == 0;
    }

    /* returns the result; true means success */
    bool getResult() {
        return mResult;
    }

private:
    enum { MAX_ITERATIONS = 1000 };

    EGLDisplay mEglDisplay;
    EGLSurface mEglSurface;
    EGLContext mEglContext;

    TestType mTestType;
    int mIteration;
    bool mResult;
    pthread_mutex_t mLock;
    pthread_cond_t mCond;

    // Assertions set a flag in Java and return from the current method (which
    // must be declared to return void).  They do not throw a C++ exception.
    //
    // Because we're running in a separate thread, which is not attached to
    // the VM, the assert macros don't help us much.  We could attach to the
    // VM (by linking to libdvm.so and calling a global function), but the
    // assertions won't cause the test to stop, which makes them less valuable.
    //
    // So instead we just return a boolean out of functions that can fail.

    /* call this to start the test */
    bool startThread_l() {
        pthread_attr_t attr;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);

        pthread_t newThread;
        int err = pthread_create(&newThread, &attr, ChainedThread::func,
                (void*) this);
        return (err == 0);
    }

    /* thread entry point */
    static void* func(void* arg) {
        ChainedThread* obj = static_cast<ChainedThread*>(arg);
        obj->doWork();
        return NULL;
    }

    bool lock() {
        int err = pthread_mutex_lock(&mLock);
        if (err != 0) {
            ALOGW("pthread_mutex_lock failed: %d", err);
        }
        return err == 0;
    }

    bool unlock() {
        int err = pthread_mutex_unlock(&mLock);
        if (err != 0) {
            ALOGW("pthread_mutex_unlock failed: %d", err);
        }
        return err == 0;
    }

    /* main worker */
    void doWork() {
        lock();

        if ((mIteration % 25) == 0) {
            ALOGD("iteration %d\n", mIteration);
        }

        mIteration++;
        bool result = runTest_l();
        if (!result) {
            ALOGW("failed at iteration %d, stopping test", mIteration);
            mResult = false;
            mIteration = MAX_ITERATIONS;
        }

        if (mIteration < MAX_ITERATIONS) {
            // still going, try to start the next one
            if (!startThread_l()) {
                ALOGW("Unable to start thread at iter=%d", mIteration);
                mResult = false;
                mIteration = MAX_ITERATIONS;
            }
        }

        if (mIteration >= MAX_ITERATIONS) {
            ALOGD("Test ending, signaling main thread");
            pthread_cond_signal(&mCond);
        }

        unlock();
    }

    /* setup, use, release EGL */
    bool runTest_l() {
        if (!eglSetup()) {
            return false;
        }
        if (!eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface,
                mEglContext))
        {
            ALOGW("eglMakeCurrent failed: 0x%x", eglGetError());
            return false;
        }
        if (!eglRelease_l()) {
            return false;
        }

        return true;
    }

    /*
     * Sets up EGL.  Creates a 1280x720 pbuffer, which is large enough to
     * cause a rapid and highly visible memory leak if we fail to discard it.
     */
    bool eglSetup() {
        static const EGLint kConfigAttribs[] = {
                EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
                EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL_RED_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_BLUE_SIZE, 8,
                EGL_NONE
        };
        static const EGLint kContextAttribs[] = {
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL_NONE
        };
        static const EGLint kPbufferAttribs[] = {
                EGL_WIDTH, 1280,
                EGL_HEIGHT, 720,
                EGL_NONE
        };

        //usleep(25000);

        mEglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL_NO_DISPLAY) {
            ALOGW("eglGetDisplay failed: 0x%x", eglGetError());
            return false;
        }

        EGLint majorVersion, minorVersion;
        if (!eglInitialize(mEglDisplay, &majorVersion, &minorVersion)) {
            ALOGW("eglInitialize failed: 0x%x", eglGetError());
            return false;
        }

        EGLConfig eglConfig;
        EGLint numConfigs = 0;
        if (!eglChooseConfig(mEglDisplay, kConfigAttribs, &eglConfig,
                1, &numConfigs)) {
            ALOGW("eglChooseConfig failed: 0x%x", eglGetError());
            return false;
        }

        mEglSurface = eglCreatePbufferSurface(mEglDisplay, eglConfig,
                kPbufferAttribs);
        if (mEglSurface == EGL_NO_SURFACE) {
            ALOGW("eglCreatePbufferSurface failed: 0x%x", eglGetError());
            return false;
        }

        mEglContext = eglCreateContext(mEglDisplay, eglConfig, EGL_NO_CONTEXT,
                kContextAttribs);
        if (mEglContext == EGL_NO_CONTEXT) {
            ALOGW("eglCreateContext failed: 0x%x", eglGetError());
            return false;
        }

        return true;
    }

    /*
     * Releases EGL.  How we do that depends on the type of test we're
     * running.
     */
    bool eglRelease_l() {
        if (mEglDisplay == EGL_NO_DISPLAY) {
            ALOGW("No display to release");
            return false;
        }

        switch (mTestType) {
        case TEST_CORRECT:
            eglTerminate(mEglDisplay);
            eglReleaseThread();
            break;
        case TEST_NO_RELEASE_CURRENT:
            eglDestroyContext(mEglDisplay, mEglContext);
            eglDestroySurface(mEglDisplay, mEglSurface);
            eglTerminate(mEglDisplay);
            break;
        default:
            ALOGE("Unknown test type %d", mTestType);
            break;
        }

        int err = eglGetError();
        if (err != EGL_SUCCESS) {
            ALOGW("eglRelease failed: 0x%x", err);
            return false;
        }
        return true;
    }
};


/* do things correctly */
TEST_F(EGLCleanupTest, TestCorrect) {
    ALOGI("Starting TEST_CORRECT");
    ChainedThread cht(ChainedThread::TEST_CORRECT);

    // start initial thread
    ASSERT_TRUE(cht.start());

    // wait for the end
    cht.waitForEnd();
    bool result = cht.getResult();
    ASSERT_TRUE(result);
}

/* try it without un-currenting the surfaces and context
TEST _F(EGLCleanupTest, TestNoReleaseCurrent) {
    ALOGI("Starting TEST_NO_RELEASE_CURRENT");
    ChainedThread cht(ChainedThread::TEST_NO_RELEASE_CURRENT);

    // start initial thread
    ASSERT_TRUE(cht.start());

    // wait for the end
    cht.waitForEnd();
    bool result = cht.getResult();
    ASSERT_TRUE(result);
}
*/

} // namespace android
