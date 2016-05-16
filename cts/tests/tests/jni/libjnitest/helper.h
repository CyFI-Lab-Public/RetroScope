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

#ifndef HELPER_H
#define HELPER_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/** A JNI test function */
typedef char *JniTestFunction(JNIEnv *env);

/**
 * Used as arguments to runTest(), it takes a simple name and expands
 * it to both a string name and function pointer.
 */
#define RUN_TEST(name) #name, test_##name

/**    
 * Standard function delcaration for a test of the JNI function with
 * the given name. The function is static, returns a (char *), and
 * takes a (JNIEnv *) named "env".
 */
#define TEST_DECLARATION(name) static char *test_##name(JNIEnv *env)
    
/**
 * Logs and returns an error message, passed and formatted in printf()
 * style. The returned string should be freed by calling free().
 *
 * @param format printf format string
 * @param ... printf-style arguments
 * @return an allocated (char *) containing the formatted result
 */
char *failure(const char *format, ...) __attribute__((format(printf, 1, 2)));

/**
 * Runs a list of tests. It will run all the tests, collecting as output
 * information about all the failures. If non-null, the return value should
 * be freed by calling free().
 *
 * @param env the JNI environment to pass to tests
 * @param ... the tests to run as name-function pairs, ending with a NULL
 * @return a string containing information about all the failures
 */
char *runJniTests(JNIEnv *env, ...);

#ifdef __cplusplus
}
#endif

#endif // HELPER_H
