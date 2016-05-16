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

#include "helper.h"

#include <cutils/log.h>

#include <stdarg.h>
#include <stdlib.h>

#define LOG_TAG "cts"

/* See helper.h for docs. */
char *failure(const char *format, ...) {
    va_list args;
    char *result;

    va_start(args, format);
    LOG_PRI_VA(ANDROID_LOG_ERROR, LOG_TAG, format, args);
    va_end(args);

    va_start(args, format);
    int count = vasprintf(&result, format, args);
    va_end(args);

    if (count < 0) {
        return NULL;
    }

    return result;
}

/* See helper.h for docs. */
char *runJniTests(JNIEnv *env, ...) {
    va_list args;
    char *result = NULL;

    va_start(args, env);

    for (;;) {
        const char *name = va_arg(args, const char *);
        if (name == NULL) {
            break;
        }

        JniTestFunction *function = va_arg(args, JniTestFunction *);

        ALOGI("running %s", name);

        char *oneResult = function(env);
        if (oneResult != NULL) {
            char *newResult;
            asprintf(&newResult, "%s%s: %s\n",
                    (result == NULL) ? "" : result,
                    name, oneResult);
            free(result);
            if (newResult == NULL) {
                // Shouldn't happen, but deal as gracefully as possible.
                return NULL;
            }
            result = newResult;
        }

        jthrowable oneException = (*env)->ExceptionOccurred(env);
        if (oneException != NULL) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }

    va_end(args);

    return result;
}
