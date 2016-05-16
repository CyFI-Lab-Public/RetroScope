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

#define LOG_TAG "GLTest"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#include <android/log.h>
#include <android/native_window.h>
#include <GLTestHelper.h>

// this listener is used to forward the subset of
// gtest output needed to generate CTS results
class CTSGTestListener : public EmptyTestEventListener {
public:
    CTSGTestListener(JNIEnv *env, jobject activity)
        : mActivity(activity), mEnv(env) {

        jclass clazz = env->FindClass(
              "android/test/wrappedgtest/WrappedGTestActivity");
        mSendStatusID = env->GetMethodID(clazz, "sendStatus",
              "(Ljava/lang/String;)V");
        mMessageBuffer = new char[2048];
    }

    ~CTSGTestListener() {
        delete[] mMessageBuffer;
    }

private:
    jobject   mActivity;
    JNIEnv *  mEnv;
    jmethodID mSendStatusID;
    char *    mMessageBuffer;

    virtual void OnTestIterationStart(const UnitTest& unit_test,
            int iteration) {
        snprintf(mMessageBuffer, sizeof(char) * 2048,
                "[==========] Running %i tests from %i test cases.",
                unit_test.test_to_run_count(),
                unit_test.test_case_to_run_count());

        mEnv->CallVoidMethod(mActivity, mSendStatusID,
                mEnv->NewStringUTF(mMessageBuffer));
    }

    virtual void OnTestStart(const TestInfo& test_info) {
        snprintf(mMessageBuffer, sizeof(char) * 2048, "[ RUN      ] %s.%s",
                test_info.test_case_name(), test_info.name());

        mEnv->CallVoidMethod(mActivity, mSendStatusID,
                mEnv->NewStringUTF(mMessageBuffer));
    }

    virtual void OnTestPartResult(const TestPartResult& result) {
        if (result.type() == TestPartResult::kSuccess) {
            return;
        }

        snprintf(mMessageBuffer, sizeof(char) * 2048, "%s:%i: Failure\n%s",
                result.file_name(), result.line_number(), result.message());

        mEnv->CallVoidMethod(mActivity, mSendStatusID,
                mEnv->NewStringUTF(mMessageBuffer));
    }

    virtual void OnTestEnd(const TestInfo& test_info) {
        const char * result = test_info.result()->Passed() ?
                "[       OK ] " : "[  FAILED  ] ";

        snprintf(mMessageBuffer, sizeof(char) * 2048, "%s%s.%s (%lli ms)",
                result, test_info.test_case_name(), test_info.name(),
                test_info.result()->elapsed_time());

        mEnv->CallVoidMethod(mActivity, mSendStatusID,
                mEnv->NewStringUTF(mMessageBuffer));
    }

    virtual void OnTestIterationEnd(const UnitTest& unit_test, int iteration) {
        snprintf(mMessageBuffer, sizeof(char) * 2048,
                "[==========] %i tests from %i test cases ran. (%lli ms total)",
                unit_test.test_to_run_count(),
                unit_test.test_case_to_run_count(), unit_test.elapsed_time());

        mEnv->CallVoidMethod(mActivity, mSendStatusID,
                mEnv->NewStringUTF(mMessageBuffer));
    }
};

// this listener is similar to the default gtest listener
// but it outputs the results to the log instead of stdout
class LogGTestListener : public EmptyTestEventListener {

private:
    virtual void OnTestIterationStart(const UnitTest& unit_test,
            int iteration) {
        LOGI("[==========] Running %i tests from %i test cases.\n",
                unit_test.test_to_run_count(),
                unit_test.test_case_to_run_count());
    }

    virtual void OnEnvironmentsSetUpStart(const UnitTest& unit_test) {
        LOGI("[==========] Global test environment set-up.\n");
    }

    virtual void OnTestCaseStart(const TestCase& test_case) {
        LOGI("[----------] %i tests from %s\n",
                test_case.test_to_run_count(),
                test_case.name());

    }

    virtual void OnTestStart(const TestInfo& test_info) {
        LOGI("[ RUN      ] %s.%s\n", test_info.test_case_name(),
                test_info.name());

    }

    virtual void OnTestPartResult(const TestPartResult& result) {
        if (result.type() == TestPartResult::kSuccess) {
            return;
        }

        LOGI("%s:%i: Failure\n%s\n", result.file_name(), result.line_number(),
                result.message());
    }

    virtual void OnTestEnd(const TestInfo& test_info) {
        const char * result = test_info.result()->Passed() ?
                "[       OK ] " : "[  FAILED  ] ";

        LOGI("%s%s.%s (%lli ms)\n", result, test_info.test_case_name(),
                test_info.name(), test_info.result()->elapsed_time());
    }


    virtual void OnTestCaseEnd(const TestCase& test_case) {
        LOGI("[----------] %i tests from %s (%lli ms total)\n",
                test_case.test_to_run_count(), test_case.name(),
                test_case.elapsed_time());

    }

    virtual void OnEnvironmentsTearDownStart(const UnitTest& unit_test) {
        LOGI("[==========] Global test environment tear-down.\n");
    }

    void PrintFailedTests(const UnitTest& unit_test) {
        const int failed_test_count = unit_test.failed_test_count();
        if (failed_test_count == 0) {
            return;
        }

        for (int i = 0; i < unit_test.total_test_case_count(); ++i) {
            const TestCase& test_case = *unit_test.GetTestCase(i);

            if (!test_case.should_run() || test_case.failed_test_count() == 0) {
                continue;
            }

            for (int j = 0; j < test_case.total_test_count(); ++j) {
                const TestInfo& test_info = *test_case.GetTestInfo(j);
                if (!test_info.should_run() || test_info.result()->Passed()) {
                    continue;
                }
                LOGI("[  FAILED  ] %s.%s\n", test_case.name(),
                        test_info.name());
            }
        }
    }
    virtual void OnTestIterationEnd(const UnitTest& unit_test, int iteration) {
        LOGI("[==========] %i tests from %i test cases ran. (%lli ms total)\n",
                unit_test.test_to_run_count(),
                unit_test.test_case_to_run_count(), unit_test.elapsed_time());

        LOGI("[  PASSED  ] %i tests\n", unit_test.successful_test_count());

        if(unit_test.Passed()) {
            return;
        }

        LOGI("[  FAILED  ] %i tests, listed below:\n",
                unit_test.failed_test_count());

        PrintFailedTests(unit_test);

        LOGI("\n%2d FAILED TESTS\n", unit_test.failed_test_count());
    }
};

ANativeWindow* GLTestHelper::mWindow;

ANativeWindow* GLTestHelper::getWindow() {
    return mWindow;
}

void GLTestHelper::setWindow(JNIEnv *env, jobject obj, jobject surface) {
    mWindow = ANativeWindow_fromSurface(env, surface);
}

int GLTestHelper::runGTests(TestEventListener * listener, char * filter) {

    if (filter) {
        ::testing::GTEST_FLAG(filter) = filter;
    }

    int argc = 0;
    InitGoogleTest(&argc, (char**)NULL);

    TestEventListeners& listeners = UnitTest::GetInstance()->listeners();
    delete listeners.Release(listeners.default_result_printer());

    listeners.Append(listener);
    int result = RUN_ALL_TESTS();
    return result;
}

int GLTestHelper::runTests(JNIEnv *env, jobject obj, jstring filter) {
    LogGTestListener * listener = new LogGTestListener();

    char * filter_cstr = NULL;

    // set filter if there is one
    if (filter) {
       filter_cstr = new char[512];
       const char * ptr = env->GetStringUTFChars(filter, NULL);
       snprintf(filter_cstr, sizeof(char) * 512, "%s", ptr);
       env->ReleaseStringUTFChars(filter, ptr);
    }

    int result = runGTests(listener, filter_cstr);

    if (filter_cstr) {
        delete[] filter_cstr;
    }

    delete listener;
    return result;
}

int GLTestHelper::runTestsCTS(JNIEnv *env, jobject obj, jobject activity) {
    CTSGTestListener * listener = new CTSGTestListener(env, activity);
    int result = runGTests(listener, NULL);
    delete listener;
    return result;
}

int GLTestHelper::registerNative(JNIEnv * env) {

    jclass clazz = env->FindClass("com/android/opengl/cts/GLTestActivity");

    jthrowable exception = env->ExceptionOccurred();
    // CTS class not found, assume stand-alone application
    if (exception) {
        env->ExceptionClear();

        if (!env->IsInstanceOf(env->ExceptionOccurred(),
                    env->FindClass("java/lang/NoClassDefFoundError"))) {
            env->Throw(exception);
        }

        //
        JNINativeMethod standaloneMethods[] = {
            // name, signature, function
            { "setSurface", "(Landroid/view/Surface;)V", (void*)(GLTestHelper::setWindow) },
            { "runTests", "(Ljava/lang/String;)V", (void*)(GLTestHelper::runTests) },
        };

        return env->RegisterNatives(
                env->FindClass("com/android/gltest/GLTestActivity"),
                standaloneMethods,
                sizeof(standaloneMethods) / sizeof(JNINativeMethod));
    }

    // GLTestActivity methods
    JNINativeMethod glTestActMethods[] = {
        // name, signature, function
        { "setSurface", "(Landroid/view/Surface;)V", (void*)(GLTestHelper::setWindow) },
    };

    int result = env->RegisterNatives(clazz, glTestActMethods,
                         sizeof(glTestActMethods) / sizeof(JNINativeMethod));

    if (result) {
        return result;
    }

    // WrappedGTestActivity methods
    JNINativeMethod wrappedGTestActMethods[] = {
        // name, signature, function
        { "runTests", "(Landroid/test/wrappedgtest/WrappedGTestActivity;)I",
            (void*)(GLTestHelper::runTestsCTS) },
    };

    return env->RegisterNatives(
            env->FindClass("android/test/wrappedgtest/WrappedGTestActivity"),
            wrappedGTestActMethods,
            sizeof(wrappedGTestActMethods) / sizeof(JNINativeMethod));
}
