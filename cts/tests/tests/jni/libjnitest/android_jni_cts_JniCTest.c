/*
 * Copyright (C) 2009 The Android Open Source Project
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

/*
 * Native implementation for the JniCTest class.
 */

#include <jni.h>
#include <JNIHelp.h>


/*
 * This includes source for all the tests as well as for a runner
 * function. See the comment in that file for more information.
 */
#define INCLUDED_FROM_WRAPPER 1
#include "macroized_tests.c"


// private static native String runAllTests();
static jstring JniCTest_runAllTests(JNIEnv *env, jclass clazz) {
    // Simply call the static function defined in "macroized_tests.c".
    return runAllTests(env);
}

static JNINativeMethod methods[] = {
    // name, signature, function
    { "runAllTests", "()Ljava/lang/String;", JniCTest_runAllTests },
};

int register_JniCTest(JNIEnv *env) {
    return jniRegisterNativeMethods(
            env, "android/jni/cts/JniCTest",
            methods, sizeof(methods) / sizeof(JNINativeMethod));
}
