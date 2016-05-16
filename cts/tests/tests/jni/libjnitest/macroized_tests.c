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
 * These are all tests of JNI, but where the JNI calls themselves are
 * represented as macro invocations. There are both C and C++ files which
 * include this file. A #if at the top of this file (immediately below)
 * detects which language is being used and defines macros accordingly.
 *
 * This file also defines a static method called runAllTests(), which
 * does what it says.
 */

#ifndef INCLUDED_FROM_WRAPPER
#error "This file should only be compiled by including it from a wrapper file."
#endif

#include "helper.h"
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>


/** reference to test class {@code InstanceFromNative} */
static jclass InstanceFromNative;

/** reference to test class {@code StaticFromNative} */
static jclass StaticFromNative;

/** reference to field {@code InstanceFromNative.theOne} */
static jfieldID InstanceFromNative_theOne;

/** reference to string {@code "biscuits"} */
static jstring biscuits;

/**
 * how to call a method: (virtual, direct, static) x (standard, array of
 * args, va_list) */
typedef enum {
    VIRTUAL_PLAIN, VIRTUAL_ARRAY, VIRTUAL_VA,
    DIRECT_PLAIN, DIRECT_ARRAY, DIRECT_VA,
    STATIC_PLAIN, STATIC_ARRAY, STATIC_VA,
} callType;

/*
 * CALL() calls the JNI function with the given name, using a JNIEnv
 * pointer named "env", and passing along any other arguments given.
 */
#ifdef __cplusplus
#define CALL(name, args...) env->name(args)
#else
/*
 * Note: The space before the comma below is significant with respect to
 * the processing of the ## operator.
 */
#define CALL(name, args...) (*env)->name(env , ## args)
#endif


/**
 * Simple assert-like macro which returns NULL if the two values are
 * equal, or an error message if they aren't.
 */
#define FAIL_IF_UNEQUAL(printfType, expected, actual)          \
    ((expected) == (actual)) ? NULL :                          \
        failure("expected " printfType " but got " printfType, \
                expected, actual);


/**
 * Initializes the static variables. Returns NULL on success or an
 * error string on failure.
 */
static char *initializeVariables(JNIEnv *env) {
    jclass clazz;
    jfieldID field;

    clazz = CALL(FindClass, "android/jni/cts/StaticFromNative");
    if (clazz == NULL) {
        return failure("could not find StaticFromNative");
    }

    StaticFromNative = (jclass) CALL(NewGlobalRef, clazz);

    clazz = CALL(FindClass, "android/jni/cts/InstanceFromNative");
    if (clazz == NULL) {
        return failure("could not find InstanceFromNative");
    }

    InstanceFromNative = (jclass) CALL(NewGlobalRef, clazz);

    field = CALL(GetStaticFieldID, InstanceFromNative, "theOne",
            "Landroid/jni/cts/InstanceFromNative;");
    if (field == NULL) {
        return failure("could not find InstanceFromNative.theOne");
    }

    InstanceFromNative_theOne = field;

    biscuits = CALL(NewStringUTF, "biscuits");
    if (biscuits == NULL) {
        return failure("could not construct string");
    }

    biscuits = (jstring) CALL(NewGlobalRef, biscuits);

    return NULL;
}

/**
 * Gets the standard instance of InstanceFromNative.
 */
static jobject getStandardInstance(JNIEnv *env) {
    return CALL(GetStaticObjectField, InstanceFromNative,
            InstanceFromNative_theOne);
}

/**
 * Looks up a static method on StaticFromNative.
 */
static jmethodID findStaticMethod(JNIEnv *env, char **errorMsg,
        const char *name, const char *sig) {
    jmethodID result = CALL(GetStaticMethodID, StaticFromNative,
            name, sig);

    if (result == NULL) {
        *errorMsg = failure("could not find static test method %s:%s",
                name, sig);
    }

    return result;
}

/**
 * Looks up an instance method on InstanceFromNative.
 */
static jmethodID findInstanceMethod(JNIEnv *env, char **errorMsg,
        const char *name, const char *sig) {
    jmethodID result = CALL(GetMethodID, InstanceFromNative, name, sig);

    if (result == NULL) {
        *errorMsg = failure("could not find instance test method %s:%s",
                name, sig);
    }

    return result;
}

/**
 * Looks up either an instance method on InstanceFromNative or a
 * static method on StaticFromNative, depending on the given
 * call type.
 */
static jmethodID findAppropriateMethod(JNIEnv *env, char **errorMsg,
        callType ct, const char *name, const char *sig) {
    if ((ct == STATIC_PLAIN) || (ct == STATIC_ARRAY) ||
            (ct == STATIC_VA)) {
        return findStaticMethod(env, errorMsg, name, sig);
    } else {
        return findInstanceMethod(env, errorMsg, name, sig);
    }
}


/*
 * The tests.
 */

// TODO: Missing functions:
//   AllocObject

static char *help_CallBooleanMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "returnBoolean", "()Z");

    if (method == NULL) {
        return msg;
    }

    jboolean result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallBooleanMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallBooleanMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallBooleanMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualBooleanMethod, o, InstanceFromNative,
                    method);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualBooleanMethodA, o, InstanceFromNative,
                    method, NULL);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualBooleanMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticBooleanMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticBooleanMethodA, StaticFromNative, method,
                    NULL);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticBooleanMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return FAIL_IF_UNEQUAL("%d", true, result);
}

TEST_DECLARATION(CallBooleanMethod) {
    return help_CallBooleanMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallBooleanMethodA) {
    return help_CallBooleanMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallBooleanMethodV) {
    return help_CallBooleanMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualBooleanMethod) {
    return help_CallBooleanMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualBooleanMethodA) {
    return help_CallBooleanMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualBooleanMethodV) {
    return help_CallBooleanMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticBooleanMethod) {
    return help_CallBooleanMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticBooleanMethodA) {
    return help_CallBooleanMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticBooleanMethodV) {
    return help_CallBooleanMethod(env, STATIC_VA);
}

static char *help_CallByteMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "returnByte", "()B");

    if (method == NULL) {
        return msg;
    }

    jbyte result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallByteMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallByteMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallByteMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualByteMethod, o, InstanceFromNative,
                    method);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualByteMethodA, o, InstanceFromNative,
                    method, NULL);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualByteMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticByteMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticByteMethodA, StaticFromNative, method,
                    NULL);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticByteMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return FAIL_IF_UNEQUAL("%d", 14, result);
}

TEST_DECLARATION(CallByteMethod) {
    return help_CallByteMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallByteMethodA) {
    return help_CallByteMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallByteMethodV) {
    return help_CallByteMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualByteMethod) {
    return help_CallByteMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualByteMethodA) {
    return help_CallByteMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualByteMethodV) {
    return help_CallByteMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticByteMethod) {
    return help_CallByteMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticByteMethodA) {
    return help_CallByteMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticByteMethodV) {
    return help_CallByteMethod(env, STATIC_VA);
}

static char *help_CallShortMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "returnShort", "()S");

    if (method == NULL) {
        return msg;
    }

    jshort result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallShortMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallShortMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallShortMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualShortMethod, o, InstanceFromNative,
                    method);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualShortMethodA, o, InstanceFromNative,
                    method, NULL);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualShortMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticShortMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticShortMethodA, StaticFromNative, method,
                    NULL);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticShortMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return FAIL_IF_UNEQUAL("%d", -608, result);
}

TEST_DECLARATION(CallShortMethod) {
    return help_CallShortMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallShortMethodA) {
    return help_CallShortMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallShortMethodV) {
    return help_CallShortMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualShortMethod) {
    return help_CallShortMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualShortMethodA) {
    return help_CallShortMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualShortMethodV) {
    return help_CallShortMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticShortMethod) {
    return help_CallShortMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticShortMethodA) {
    return help_CallShortMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticShortMethodV) {
    return help_CallShortMethod(env, STATIC_VA);
}

static char *help_CallCharMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "returnChar", "()C");

    if (method == NULL) {
        return msg;
    }

    jchar result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallCharMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallCharMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallCharMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualCharMethod, o, InstanceFromNative,
                    method);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualCharMethodA, o, InstanceFromNative,
                    method, NULL);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualCharMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticCharMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticCharMethodA, StaticFromNative, method,
                    NULL);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticCharMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return FAIL_IF_UNEQUAL("%d", 9000, result);
}

TEST_DECLARATION(CallCharMethod) {
    return help_CallCharMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallCharMethodA) {
    return help_CallCharMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallCharMethodV) {
    return help_CallCharMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualCharMethod) {
    return help_CallCharMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualCharMethodA) {
    return help_CallCharMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualCharMethodV) {
    return help_CallCharMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticCharMethod) {
    return help_CallCharMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticCharMethodA) {
    return help_CallCharMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticCharMethodV) {
    return help_CallCharMethod(env, STATIC_VA);
}

static char *help_CallIntMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "returnInt", "()I");

    if (method == NULL) {
        return msg;
    }

    jint result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallIntMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallIntMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallIntMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualIntMethod, o, InstanceFromNative,
                    method);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualIntMethodA, o, InstanceFromNative,
                    method, NULL);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualIntMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticIntMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticIntMethodA, StaticFromNative, method,
                    NULL);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticIntMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return FAIL_IF_UNEQUAL("%d", 4004004, result);
}

TEST_DECLARATION(CallIntMethod) {
    return help_CallIntMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallIntMethodA) {
    return help_CallIntMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallIntMethodV) {
    return help_CallIntMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualIntMethod) {
    return help_CallIntMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualIntMethodA) {
    return help_CallIntMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualIntMethodV) {
    return help_CallIntMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticIntMethod) {
    return help_CallIntMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticIntMethodA) {
    return help_CallIntMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticIntMethodV) {
    return help_CallIntMethod(env, STATIC_VA);
}

static char *help_CallLongMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "returnLong", "()J");

    if (method == NULL) {
        return msg;
    }

    jlong result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallLongMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallLongMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallLongMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualLongMethod, o, InstanceFromNative,
                    method);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualLongMethodA, o, InstanceFromNative,
                    method, NULL);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualLongMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticLongMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticLongMethodA, StaticFromNative, method,
                    NULL);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticLongMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return FAIL_IF_UNEQUAL("%lld", -80080080087LL, result);
}

TEST_DECLARATION(CallLongMethod) {
    return help_CallLongMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallLongMethodA) {
    return help_CallLongMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallLongMethodV) {
    return help_CallLongMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualLongMethod) {
    return help_CallLongMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualLongMethodA) {
    return help_CallLongMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualLongMethodV) {
    return help_CallLongMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticLongMethod) {
    return help_CallLongMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticLongMethodA) {
    return help_CallLongMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticLongMethodV) {
    return help_CallLongMethod(env, STATIC_VA);
}

static char *help_CallFloatMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "returnFloat", "()F");

    if (method == NULL) {
        return msg;
    }

    jfloat result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallFloatMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallFloatMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallFloatMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualFloatMethod, o, InstanceFromNative,
                    method);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualFloatMethodA, o, InstanceFromNative,
                    method, NULL);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualFloatMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticFloatMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticFloatMethodA, StaticFromNative, method,
                    NULL);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticFloatMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return FAIL_IF_UNEQUAL("%g", 2.5e22f, result);
}

TEST_DECLARATION(CallFloatMethod) {
    return help_CallFloatMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallFloatMethodA) {
    return help_CallFloatMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallFloatMethodV) {
    return help_CallFloatMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualFloatMethod) {
    return help_CallFloatMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualFloatMethodA) {
    return help_CallFloatMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualFloatMethodV) {
    return help_CallFloatMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticFloatMethod) {
    return help_CallFloatMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticFloatMethodA) {
    return help_CallFloatMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticFloatMethodV) {
    return help_CallFloatMethod(env, STATIC_VA);
}

static char *help_CallDoubleMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "returnDouble", "()D");

    if (method == NULL) {
        return msg;
    }

    jdouble result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallDoubleMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallDoubleMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallDoubleMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualDoubleMethod, o, InstanceFromNative,
                    method);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualDoubleMethodA, o, InstanceFromNative,
                    method, NULL);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualDoubleMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticDoubleMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticDoubleMethodA, StaticFromNative, method,
                    NULL);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticDoubleMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return FAIL_IF_UNEQUAL("%g", 7.503e100, result);
}

TEST_DECLARATION(CallDoubleMethod) {
    return help_CallDoubleMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallDoubleMethodA) {
    return help_CallDoubleMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallDoubleMethodV) {
    return help_CallDoubleMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualDoubleMethod) {
    return help_CallDoubleMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualDoubleMethodA) {
    return help_CallDoubleMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualDoubleMethodV) {
    return help_CallDoubleMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticDoubleMethod) {
    return help_CallDoubleMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticDoubleMethodA) {
    return help_CallDoubleMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticDoubleMethodV) {
    return help_CallDoubleMethod(env, STATIC_VA);
}

static char *help_CallVoidMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "nop", "()V");

    if (method == NULL) {
        return msg;
    }

    // For these, we simply consider "success" to be "didn't crash."

    switch (ct) {
        case VIRTUAL_PLAIN: {
            CALL(CallVoidMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            CALL(CallVoidMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            CALL(CallVoidMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            CALL(CallNonvirtualVoidMethod, o, InstanceFromNative,
                    method);
            break;
        }
        case DIRECT_ARRAY: {
            CALL(CallNonvirtualVoidMethodA, o, InstanceFromNative,
                    method, NULL);
            break;
        }
        case DIRECT_VA: {
            CALL(CallNonvirtualVoidMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            CALL(CallStaticVoidMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            CALL(CallStaticVoidMethodA, StaticFromNative, method,
                    NULL);
            break;
        }
        case STATIC_VA: {
            CALL(CallStaticVoidMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return NULL;
}

TEST_DECLARATION(CallVoidMethod) {
    return help_CallVoidMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallVoidMethodA) {
    return help_CallVoidMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallVoidMethodV) {
    return help_CallVoidMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualVoidMethod) {
    return help_CallVoidMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualVoidMethodA) {
    return help_CallVoidMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualVoidMethodV) {
    return help_CallVoidMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticVoidMethod) {
    return help_CallVoidMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticVoidMethodA) {
    return help_CallVoidMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticVoidMethodV) {
    return help_CallVoidMethod(env, STATIC_VA);
}

static char *help_CallObjectMethod(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "returnString", "()Ljava/lang/String;");

    if (method == NULL) {
        return msg;
    }

    jstring result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = (jstring) CALL(CallObjectMethod, o, method);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = (jstring) CALL(CallObjectMethodA, o, method, NULL);
            break;
        }
        case VIRTUAL_VA: {
            result = (jstring) CALL(CallObjectMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = (jstring)
                CALL(CallNonvirtualObjectMethod, o, InstanceFromNative,
                        method);
            break;
        }
        case DIRECT_ARRAY: {
            result = (jstring)
                CALL(CallNonvirtualObjectMethodA, o, InstanceFromNative,
                        method, NULL);
            break;
        }
        case DIRECT_VA: {
            result = (jstring)
                CALL(CallNonvirtualObjectMethodV, o, InstanceFromNative,
                        method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = (jstring)
                CALL(CallStaticObjectMethod, StaticFromNative, method);
            break;
        }
        case STATIC_ARRAY: {
            result = (jstring)
                CALL(CallStaticObjectMethodA, StaticFromNative, method, NULL);
            break;
        }
        case STATIC_VA: {
            result = (jstring)
                CALL(CallStaticObjectMethodV, StaticFromNative, method, args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    if (result == NULL) {
        return failure("got null from call");
    }

    const char *utf = CALL(GetStringUTFChars, result, NULL);

    if (strcmp(utf, "muffins") == 0) {
        msg = NULL;
    } else {
        msg = failure("unexpected string: %s", utf);
    }

    CALL(ReleaseStringUTFChars, result, utf);

    return msg;
}

TEST_DECLARATION(CallObjectMethod) {
    return help_CallObjectMethod(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(CallObjectMethodA) {
    return help_CallObjectMethod(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(CallObjectMethodV) {
    return help_CallObjectMethod(env, VIRTUAL_VA);
}

TEST_DECLARATION(CallNonvirtualObjectMethod) {
    return help_CallObjectMethod(env, DIRECT_PLAIN);
}

TEST_DECLARATION(CallNonvirtualObjectMethodA) {
    return help_CallObjectMethod(env, DIRECT_ARRAY);
}

TEST_DECLARATION(CallNonvirtualObjectMethodV) {
    return help_CallObjectMethod(env, DIRECT_VA);
}

TEST_DECLARATION(CallStaticObjectMethod) {
    return help_CallObjectMethod(env, STATIC_PLAIN);
}

TEST_DECLARATION(CallStaticObjectMethodA) {
    return help_CallObjectMethod(env, STATIC_ARRAY);
}

TEST_DECLARATION(CallStaticObjectMethodV) {
    return help_CallObjectMethod(env, STATIC_VA);
}

static char *help_TakeOneOfEach(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "takeOneOfEach", "(DFJICSBZLjava/lang/String;)Z");

    if (method == NULL) {
        return msg;
    }

    jvalue jargs[] = {
        {d: 0.0}, {f: 1.0f}, {j: 2LL}, {i: 3}, {c: 4}, {s: 5}, {b: 6},
        {z: true}, {l: biscuits}
    };

    jboolean result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallBooleanMethod, o, method,
                    0.0, 1.0f, 2LL, 3, (jchar) 4, (jshort) 5, (jbyte) 6,
                    (jboolean) true, biscuits);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallBooleanMethodA, o, method, jargs);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallBooleanMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualBooleanMethod, o, InstanceFromNative,
                    method,
                    0.0, 1.0f, 2LL, 3, (jchar) 4, (jshort) 5, (jbyte) 6,
                    (jboolean) true, biscuits);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualBooleanMethodA, o, InstanceFromNative,
                    method, jargs);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualBooleanMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticBooleanMethod, StaticFromNative, method,
                    0.0, 1.0f, 2LL, 3, (jchar) 4, (jshort) 5, (jbyte) 6,
                    (jboolean) true, biscuits);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticBooleanMethodA, StaticFromNative, method,
                    jargs);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticBooleanMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return FAIL_IF_UNEQUAL("%d", true, result);
}

TEST_DECLARATION(TakeOneOfEach) {
    return help_TakeOneOfEach(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(TakeOneOfEachA) {
    return help_TakeOneOfEach(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(TakeOneOfEachV) {
    return help_TakeOneOfEach(env, VIRTUAL_VA,
            0.0, 1.0f, 2LL, 3, (jchar) 4, (jshort) 5, (jbyte) 6,
            (jboolean) true, biscuits);
}

TEST_DECLARATION(NonvirtualTakeOneOfEach) {
    return help_TakeOneOfEach(env, DIRECT_PLAIN);
}

TEST_DECLARATION(NonvirtualTakeOneOfEachA) {
    return help_TakeOneOfEach(env, DIRECT_ARRAY);
}

TEST_DECLARATION(NonvirtualTakeOneOfEachV) {
    return help_TakeOneOfEach(env, DIRECT_VA,
            0.0, 1.0f, 2LL, 3, (jchar) 4, (jshort) 5, (jbyte) 6,
            (jboolean) true, biscuits);
}

TEST_DECLARATION(StaticTakeOneOfEach) {
    return help_TakeOneOfEach(env, STATIC_PLAIN);
}

TEST_DECLARATION(StaticTakeOneOfEachA) {
    return help_TakeOneOfEach(env, STATIC_ARRAY);
}

TEST_DECLARATION(StaticTakeOneOfEachV) {
    return help_TakeOneOfEach(env, STATIC_VA,
            0.0, 1.0f, 2LL, 3, (jchar) 4, (jshort) 5, (jbyte) 6,
            (jboolean) true, biscuits);
}

static char *help_TakeCoolHandLuke(JNIEnv *env, callType ct, ...) {
    va_list args;
    va_start(args, ct);

    char *msg;
    jobject o = getStandardInstance(env);
    jmethodID method = findAppropriateMethod(env, &msg, ct,
            "takeCoolHandLuke",
            "(IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII)Z");

    if (method == NULL) {
        return msg;
    }

    jvalue jargs[] = {
        {i: 1}, {i: 2}, {i: 3}, {i: 4},
        {i: 5}, {i: 6}, {i: 7}, {i: 8}, {i: 9}, 
        {i: 10}, {i: 11}, {i: 12}, {i: 13}, {i: 14},
        {i: 15}, {i: 16}, {i: 17}, {i: 18}, {i: 19}, 
        {i: 20}, {i: 21}, {i: 22}, {i: 23}, {i: 24},
        {i: 25}, {i: 26}, {i: 27}, {i: 28}, {i: 29}, 
        {i: 30}, {i: 31}, {i: 32}, {i: 33}, {i: 34},
        {i: 35}, {i: 36}, {i: 37}, {i: 38}, {i: 39}, 
        {i: 40}, {i: 41}, {i: 42}, {i: 43}, {i: 44},
        {i: 45}, {i: 46}, {i: 47}, {i: 48}, {i: 49},
        {i: 50}
    };

    jboolean result;

    switch (ct) {
        case VIRTUAL_PLAIN: {
            result = CALL(CallBooleanMethod, o, method,
                    1, 2, 3, 4, 5, 6, 7, 8, 9,
                    10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                    20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
                    30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
                    40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                    50);
            break;
        }
        case VIRTUAL_ARRAY: {
            result = CALL(CallBooleanMethodA, o, method, jargs);
            break;
        }
        case VIRTUAL_VA: {
            result = CALL(CallBooleanMethodV, o, method, args);
            break;
        }
        case DIRECT_PLAIN: {
            result = CALL(CallNonvirtualBooleanMethod, o, InstanceFromNative,
                    method,
                    1, 2, 3, 4, 5, 6, 7, 8, 9,
                    10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                    20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
                    30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
                    40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                    50);
            break;
        }
        case DIRECT_ARRAY: {
            result = CALL(CallNonvirtualBooleanMethodA, o, InstanceFromNative,
                    method, jargs);
            break;
        }
        case DIRECT_VA: {
            result = CALL(CallNonvirtualBooleanMethodV, o, InstanceFromNative,
                    method, args);
            break;
        }
        case STATIC_PLAIN: {
            result = CALL(CallStaticBooleanMethod, StaticFromNative, method,
                    1, 2, 3, 4, 5, 6, 7, 8, 9,
                    10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                    20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
                    30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
                    40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                    50);
            break;
        }
        case STATIC_ARRAY: {
            result = CALL(CallStaticBooleanMethodA, StaticFromNative, method,
                    jargs);
            break;
        }
        case STATIC_VA: {
            result = CALL(CallStaticBooleanMethodV, StaticFromNative, method,
                    args);
            break;
        }
        default: {
            return failure("shouldn't happen");
        }
    }

    va_end(args);

    return FAIL_IF_UNEQUAL("%d", true, result);
}

TEST_DECLARATION(TakeCoolHandLuke) {
    return help_TakeCoolHandLuke(env, VIRTUAL_PLAIN);
}

TEST_DECLARATION(TakeCoolHandLukeA) {
    return help_TakeCoolHandLuke(env, VIRTUAL_ARRAY);
}

TEST_DECLARATION(TakeCoolHandLukeV) {
    return help_TakeCoolHandLuke(env, VIRTUAL_VA,
            1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            50);
}

TEST_DECLARATION(NonvirtualTakeCoolHandLuke) {
    return help_TakeCoolHandLuke(env, DIRECT_PLAIN);
}

TEST_DECLARATION(NonvirtualTakeCoolHandLukeA) {
    return help_TakeCoolHandLuke(env, DIRECT_ARRAY);
}

TEST_DECLARATION(NonvirtualTakeCoolHandLukeV) {
    return help_TakeCoolHandLuke(env, DIRECT_VA,
            1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            50);
}

TEST_DECLARATION(StaticTakeCoolHandLuke) {
    return help_TakeCoolHandLuke(env, STATIC_PLAIN);
}

TEST_DECLARATION(StaticTakeCoolHandLukeA) {
    return help_TakeCoolHandLuke(env, STATIC_ARRAY);
}

TEST_DECLARATION(StaticTakeCoolHandLukeV) {
    return help_TakeCoolHandLuke(env, STATIC_VA,
            1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            50);
}

TEST_DECLARATION(DefineClass) {
    // Android implementations should always return NULL.
    jclass clazz = CALL(DefineClass, "foo", NULL, NULL, 0);

    if (clazz != NULL) {
        return failure("Expected NULL but got %p", clazz);
    }

    return NULL;
}

// TODO: Missing functions:
//   DeleteLocalRef
//   DeleteWeakGlobalRef
//   EnsureLocalCapacity
//   ExceptionCheck
//   ExceptionClear
//   ExceptionDescribe
//   ExceptionOccurred
//   FatalError (Note: impossible to test in this framework)
//   FindClass
//   FromReflectedField
//   FromReflectedMethod
//   GetArrayLength
//   GetBooleanArrayElements
//   GetBooleanArrayRegion
//   GetBooleanField
//   GetByteArrayElements
//   GetByteArrayRegion
//   GetByteField
//   GetCharArrayElements
//   GetCharArrayRegion
//   GetCharField
//   GetDirectBufferAddress
//   GetDirectBufferCapacity
//   GetDoubleArrayElements
//   GetDoubleArrayRegion
//   GetDoubleField
//   GetFieldID
//   GetFloatArrayElements
//   GetFloatArrayRegion
//   GetFloatField
//   GetIntArrayElements
//   GetIntArrayRegion
//   GetIntField
//   GetJavaVM
//   GetLongArrayElements
//   GetLongArrayRegion
//   GetLongField
//   GetMethodID
//   GetObjectArrayElement
//   GetObjectClass
//   GetObjectField
//   GetObjectRefType (since 1.6)
//   GetPrimitiveArrayCritical
//   GetShortArrayElements
//   GetShortArrayRegion
//   GetShortField
//   GetStaticBooleanField
//   GetStaticByteField
//   GetStaticCharField
//   GetStaticDoubleField
//   GetStaticFieldID
//   GetStaticFloatField
//   GetStaticIntField
//   GetStaticLongField
//   GetStaticMethodID
//   GetStaticObjectField
//   GetStaticShortField
//   GetStringChars
//   GetStringCritical
//   GetStringLength
//   GetStringRegion
//   GetStringUTFChars
//   GetStringUTFLength
//   GetStringUTFRegion
//   GetSuperclass

TEST_DECLARATION(GetVersion) {
    // Android implementations should all be at version 1.6.
    jint version = CALL(GetVersion);

    if (version != JNI_VERSION_1_6) {
        return failure("Expected JNI_VERSION_1_6 but got 0x%x", version);
    }

    return NULL;
}

// TODO: Missing functions:
//   IsAssignableFrom
//   IsInstanceOf
//   IsSameObject
//   MonitorEnter
//   MonitorExit
//   NewBooleanArray
//   NewByteArray
//   NewCharArray
//   NewDirectByteBuffer
//   NewDoubleArray
//   NewFloatArray
//   NewGlobalRef
//   NewIntArray
//   NewLocalRef
//   NewLongArray
//   NewObject
//   NewObjectA
//   NewObjectArray
//   NewObjectV
//   NewShortArray
//   NewString
//   NewStringUTF
//   NewWeakGlobalRef
//   PopLocalFrame
//   PushLocalFrame
//   RegisterNatives
//   ReleaseBooleanArrayElements
//   ReleaseByteArrayElements
//   ReleaseCharArrayElements
//   ReleaseDoubleArrayElements
//   ReleaseFloatArrayElements
//   ReleaseIntArrayElements
//   ReleaseLongArrayElements
//   ReleasePrimitiveArrayCritical
//   ReleaseShortArrayElements
//   ReleaseStringChars
//   ReleaseStringCritical
//   ReleaseStringUTFChars
//   SetBooleanArrayRegion
//   SetBooleanField
//   SetByteArrayRegion
//   SetByteField
//   SetCharArrayRegion
//   SetCharField
//   SetDoubleArrayRegion
//   SetDoubleField
//   SetFloatArrayRegion
//   SetFloatField
//   SetIntArrayRegion
//   SetIntField
//   SetLongArrayRegion
//   SetLongField
//   SetObjectArrayElement
//   SetObjectField
//   SetShortArrayRegion
//   SetShortField
//   SetStaticBooleanField
//   SetStaticByteField
//   SetStaticCharField
//   SetStaticDoubleField
//   SetStaticFloatField
//   SetStaticIntField
//   SetStaticLongField
//   SetStaticObjectField
//   SetStaticShortField
//   Throw
//   ThrowNew
//   ToReflectedField
//   ToReflectedMethod
//   UnregisterNatives



/**
 * Runs all the tests, returning NULL if they all succeeded, or
 * a string listing information about all the failures.
 */
static jstring runAllTests(JNIEnv *env) {
    char *result = initializeVariables(env);

    if (CALL(ExceptionOccurred)) {
        CALL(ExceptionDescribe);
        CALL(ExceptionClear);
    }

    if (result == NULL) {
        result = runJniTests(env,
                RUN_TEST(CallBooleanMethod),
                RUN_TEST(CallBooleanMethodA),
                RUN_TEST(CallBooleanMethodV),
                RUN_TEST(CallNonvirtualBooleanMethod),
                RUN_TEST(CallNonvirtualBooleanMethodA),
                RUN_TEST(CallNonvirtualBooleanMethodV),
                RUN_TEST(CallStaticBooleanMethod),
                RUN_TEST(CallStaticBooleanMethodA),
                RUN_TEST(CallStaticBooleanMethodV),

                RUN_TEST(CallByteMethod),
                RUN_TEST(CallByteMethodA),
                RUN_TEST(CallByteMethodV),
                RUN_TEST(CallNonvirtualByteMethod),
                RUN_TEST(CallNonvirtualByteMethodA),
                RUN_TEST(CallNonvirtualByteMethodV),
                RUN_TEST(CallStaticByteMethod),
                RUN_TEST(CallStaticByteMethodA),
                RUN_TEST(CallStaticByteMethodV),

                RUN_TEST(CallShortMethod),
                RUN_TEST(CallShortMethodA),
                RUN_TEST(CallShortMethodV),
                RUN_TEST(CallNonvirtualShortMethod),
                RUN_TEST(CallNonvirtualShortMethodA),
                RUN_TEST(CallNonvirtualShortMethodV),
                RUN_TEST(CallStaticShortMethod),
                RUN_TEST(CallStaticShortMethodA),
                RUN_TEST(CallStaticShortMethodV),

                RUN_TEST(CallCharMethod),
                RUN_TEST(CallCharMethodA),
                RUN_TEST(CallCharMethodV),
                RUN_TEST(CallNonvirtualCharMethod),
                RUN_TEST(CallNonvirtualCharMethodA),
                RUN_TEST(CallNonvirtualCharMethodV),
                RUN_TEST(CallStaticCharMethod),
                RUN_TEST(CallStaticCharMethodA),
                RUN_TEST(CallStaticCharMethodV),

                RUN_TEST(CallIntMethod),
                RUN_TEST(CallIntMethodA),
                RUN_TEST(CallIntMethodV),
                RUN_TEST(CallNonvirtualIntMethod),
                RUN_TEST(CallNonvirtualIntMethodA),
                RUN_TEST(CallNonvirtualIntMethodV),
                RUN_TEST(CallStaticIntMethod),
                RUN_TEST(CallStaticIntMethodA),
                RUN_TEST(CallStaticIntMethodV),

                RUN_TEST(CallLongMethod),
                RUN_TEST(CallLongMethodA),
                RUN_TEST(CallLongMethodV),
                RUN_TEST(CallNonvirtualLongMethod),
                RUN_TEST(CallNonvirtualLongMethodA),
                RUN_TEST(CallNonvirtualLongMethodV),
                RUN_TEST(CallStaticLongMethod),
                RUN_TEST(CallStaticLongMethodA),
                RUN_TEST(CallStaticLongMethodV),

                RUN_TEST(CallFloatMethod),
                RUN_TEST(CallFloatMethodA),
                RUN_TEST(CallFloatMethodV),
                RUN_TEST(CallNonvirtualFloatMethod),
                RUN_TEST(CallNonvirtualFloatMethodA),
                RUN_TEST(CallNonvirtualFloatMethodV),
                RUN_TEST(CallStaticFloatMethod),
                RUN_TEST(CallStaticFloatMethodA),
                RUN_TEST(CallStaticFloatMethodV),

                RUN_TEST(CallDoubleMethod),
                RUN_TEST(CallDoubleMethodA),
                RUN_TEST(CallDoubleMethodV),
                RUN_TEST(CallNonvirtualDoubleMethod),
                RUN_TEST(CallNonvirtualDoubleMethodA),
                RUN_TEST(CallNonvirtualDoubleMethodV),
                RUN_TEST(CallStaticDoubleMethod),
                RUN_TEST(CallStaticDoubleMethodA),
                RUN_TEST(CallStaticDoubleMethodV),

                RUN_TEST(CallVoidMethod),
                RUN_TEST(CallVoidMethodA),
                RUN_TEST(CallVoidMethodV),
                RUN_TEST(CallNonvirtualVoidMethod),
                RUN_TEST(CallNonvirtualVoidMethodA),
                RUN_TEST(CallNonvirtualVoidMethodV),
                RUN_TEST(CallStaticVoidMethod),
                RUN_TEST(CallStaticVoidMethodA),
                RUN_TEST(CallStaticVoidMethodV),

                RUN_TEST(CallObjectMethod),
                RUN_TEST(CallObjectMethodA),
                RUN_TEST(CallObjectMethodV),
                RUN_TEST(CallNonvirtualObjectMethod),
                RUN_TEST(CallNonvirtualObjectMethodA),
                RUN_TEST(CallNonvirtualObjectMethodV),
                RUN_TEST(CallStaticObjectMethod),
                RUN_TEST(CallStaticObjectMethodA),
                RUN_TEST(CallStaticObjectMethodV),

                RUN_TEST(TakeOneOfEach),
                RUN_TEST(TakeOneOfEachA),
                RUN_TEST(TakeOneOfEachV),
                RUN_TEST(NonvirtualTakeOneOfEach),
                RUN_TEST(NonvirtualTakeOneOfEachA),
                RUN_TEST(NonvirtualTakeOneOfEachV),
                RUN_TEST(StaticTakeOneOfEach),
                RUN_TEST(StaticTakeOneOfEachA),
                RUN_TEST(StaticTakeOneOfEachV),

                RUN_TEST(TakeCoolHandLuke),
                RUN_TEST(TakeCoolHandLukeA),
                RUN_TEST(TakeCoolHandLukeV),
                RUN_TEST(NonvirtualTakeCoolHandLuke),
                RUN_TEST(NonvirtualTakeCoolHandLukeA),
                RUN_TEST(NonvirtualTakeCoolHandLukeV),
                RUN_TEST(StaticTakeCoolHandLuke),
                RUN_TEST(StaticTakeCoolHandLukeA),
                RUN_TEST(StaticTakeCoolHandLukeV),

                RUN_TEST(DefineClass),
                RUN_TEST(GetVersion),
                NULL);

        // TODO: Add more tests, above.
    }

    if (result != NULL) {
        jstring s = CALL(NewStringUTF, result);
        free(result);
        return s;
    }

    return NULL;
}
