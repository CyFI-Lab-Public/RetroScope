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
 * Native implementation for the StaticNonce class. See the test code
 * in JniStaticTest for more info.
 */

#include <jni.h>
#include <JNIHelp.h>

#include <stdbool.h>
#include <string.h>

// public static native void nop();
static void StaticNonce_nop(JNIEnv *env, jclass clazz) {
    // This space intentionally left blank.
}

// public static native boolean returnBoolean();
static jboolean StaticNonce_returnBoolean(JNIEnv *env, jclass clazz) {
    return (jboolean) true;
}

// public static native byte returnByte();
static jbyte StaticNonce_returnByte(JNIEnv *env, jclass clazz) {
    return (jbyte) 123;
}

// public static native short returnShort();
static jshort StaticNonce_returnShort(JNIEnv *env, jclass clazz) {
    return (jshort) -12345;
}

// public static native char returnChar();
static jchar StaticNonce_returnChar(JNIEnv *env, jclass clazz) {
    return (jchar) 34567;
}

// public static native int returnInt();
static jint StaticNonce_returnInt(JNIEnv *env, jclass clazz) {
    return 12345678;
}

// public static native long returnLong();
static jlong StaticNonce_returnLong(JNIEnv *env, jclass clazz) {
    return (jlong) -1098765432109876543LL;
}

// public static native float returnFloat();
static jfloat StaticNonce_returnFloat(JNIEnv *env, jclass clazz) {
    return (jfloat) -98765.4321F;
}

// public static native double returnDouble();
static jdouble StaticNonce_returnDouble(JNIEnv *env, jclass clazz) {
    return 12345678.9;
}

// public static native Object returnNull();
static jobject StaticNonce_returnNull(JNIEnv *env, jclass clazz) {
    return NULL;
}

// public static native String returnString();
static jstring StaticNonce_returnString(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, "blort");
}

// public static native short[] returnShortArray();
static jshortArray StaticNonce_returnShortArray(JNIEnv *env, jclass clazz) {
    static jshort contents[] = { 10, 20, 30 };

    jshortArray result = (*env)->NewShortArray(env, 3);

    if (result == NULL) {
        return NULL;
    }

    (*env)->SetShortArrayRegion(env, result, 0, 3, contents);
    return result;
}

// public static native String[] returnStringArray();
static jobjectArray StaticNonce_returnStringArray(JNIEnv *env, jclass clazz) {
    static int indices[] = { 0, 50, 99 };
    static const char *contents[] = { "blort", "zorch", "fizmo" };

    jclass stringClass = (*env)->FindClass(env, "java/lang/String");

    if ((*env)->ExceptionOccurred(env) != NULL) {
        return NULL;
    }

    if (stringClass == NULL) {
        jniThrowException(env, "java/lang/AssertionError",
                "class String not found");
        return NULL;
    }

    jobjectArray result = (*env)->NewObjectArray(env, 100, stringClass, NULL);

    if (result == NULL) {
        return NULL;
    }

    jsize i;
    for (i = 0; i < 3; i++) {
        jstring s = (*env)->NewStringUTF(env, contents[i]);

        if (s == NULL) {
            return NULL;
        }

        (*env)->SetObjectArrayElement(env, result, indices[i], s);

        if ((*env)->ExceptionOccurred(env) != NULL) {
            return NULL;
        }
    }

    return result;
}

// public static native Class returnThisClass();
static jclass StaticNonce_returnThisClass(JNIEnv *env, jclass clazz) {
    return clazz;
}

// public static native StaticNonce returnInstance();
static jobject StaticNonce_returnInstance(JNIEnv *env, jclass clazz) {
    jmethodID id = (*env)->GetMethodID(env, clazz, "<init>", "()V");

    if ((*env)->ExceptionOccurred(env) != NULL) {
        return NULL;
    }
    
    if (id == NULL) {
        jniThrowException(env, "java/lang/AssertionError",
                "constructor not found");
        return NULL;
    }

    return (*env)->NewObjectA(env, clazz, id, NULL);
}

// public static native boolean takeBoolean(boolean v);
static jboolean StaticNonce_takeBoolean(JNIEnv *env, jclass clazz,
        jboolean v) {
    return v == true;
}

// public static native boolean takeByte(byte v);
static jboolean StaticNonce_takeByte(JNIEnv *env, jclass clazz, jbyte v) {
    return v == -99;
}

// public static native boolean takeShort(short v);
static jboolean StaticNonce_takeShort(JNIEnv *env, jclass clazz, jshort v) {
    return v == 19991;
}

// public static native boolean takeChar(char v);
static jboolean StaticNonce_takeChar(JNIEnv *env, jclass clazz, jchar v) {
    return v == 999;
}

// public static native boolean takeInt(int v);
static jboolean StaticNonce_takeInt(JNIEnv *env, jclass clazz, jint v) {
    return v == -999888777;
}

// public static native boolean takeLong(long v);
static jboolean StaticNonce_takeLong(JNIEnv *env, jclass clazz, jlong v) {
    return v == 999888777666555444LL;
}

// public static native boolean takeFloat(float v);
static jboolean StaticNonce_takeFloat(JNIEnv *env, jclass clazz, jfloat v) {
    return v == -9988.7766F;
}

// public static native boolean takeDouble(double v);
static jboolean StaticNonce_takeDouble(JNIEnv *env, jclass clazz, jdouble v) {
    return v == 999888777.666555;
}

// public static native boolean takeNull(Object v);
static jboolean StaticNonce_takeNull(JNIEnv *env, jclass clazz, jobject v) {
    return v == NULL;
}

// public static native boolean takeString(String v);
static jboolean StaticNonce_takeString(JNIEnv *env, jclass clazz, jstring v) {
    if (v == NULL) {
        return false;
    }
    
    const char *utf = (*env)->GetStringUTFChars(env, v, NULL);
    jboolean result = (strcmp("fuzzbot", utf) == 0);

    (*env)->ReleaseStringUTFChars(env, v, utf);
    return result;
}

// public static native boolean takeThisClass(Class v);
static jboolean StaticNonce_takeThisClass(JNIEnv *env, jclass clazz,
        jclass v) {
    return (*env)->IsSameObject(env, clazz, v);
}

// public static native boolean takeIntLong(int v1, long v2);
static jboolean StaticNonce_takeIntLong(JNIEnv *env, jclass clazz,
        jint v1, jlong v2) {
    return (v1 == 914) && (v2 == 9140914091409140914LL);
}

// public static native boolean takeLongInt(long v1, int v2);
static jboolean StaticNonce_takeLongInt(JNIEnv *env, jclass clazz,
        jlong v1, jint v2) {
    return (v1 == -4321LL) && (v2 == 12341234);
}

// public static native boolean takeOneOfEach(boolean v0, byte v1, short v2,
//         char v3, int v4, long v5, String v6, float v7, double v8,
//         int[] v9);
static jboolean StaticNonce_takeOneOfEach(JNIEnv *env, jclass clazz,
        jboolean v0, jbyte v1, jshort v2, jchar v3, jint v4, jlong v5,
        jstring v6, jfloat v7, jdouble v8, jintArray v9) {
    jsize length;
    jboolean result;
    
    if ((v0 != false) || (v1 != 1) || (v2 != 2) || (v3 != 3) ||
            (v4 != 4) || (v5 != 5) || (v7 != 7.0f) || (v8 != 8.0)) {
        return false;
    }

    length = (*env)->GetStringUTFLength(env, v6);

    if (length != 3) {
        jniThrowException(env, "java/lang/AssertionError",
                "bad string length");
        return false;
    }

    const char *utf = (*env)->GetStringUTFChars(env, v6, NULL);
    result = (strncmp("six", utf, 3) == 0);

    (*env)->ReleaseStringUTFChars(env, v6, utf);

    if (! result) {
        return false;
    }

    length = (*env)->GetArrayLength(env, v9);
    if (length != 2) {
        jniThrowException(env, "java/lang/AssertionError",
                "bad array length");
        return false;
    }

    jint *elements = (*env)->GetIntArrayElements(env, v9, NULL);
    result = (elements[0] == 9) && (elements[1] == 10);
    (*env)->ReleaseIntArrayElements(env, v9, elements, JNI_ABORT);

    return result;
}

// public static native boolean takeCoolHandLuke(
//         int v1, int v2, int v3, int v4,
//         int v5, int v6, int v7, int v8, int v9,
//         int v10, int v11, int v12, int v13, int v14,
//         int v15, int v16, int v17, int v18, int v19,
//         int v20, int v21, int v22, int v23, int v24,
//         int v25, int v26, int v27, int v28, int v29,
//         int v30, int v31, int v32, int v33, int v34,
//         int v35, int v36, int v37, int v38, int v39,
//         int v40, int v41, int v42, int v43, int v44,
//         int v45, int v46, int v47, int v48, int v49,
//         int v50);
static jboolean StaticNonce_takeCoolHandLuke(JNIEnv *env, jclass clazz,
        jint v1, jint v2, jint v3, jint v4,
        jint v5, jint v6, jint v7, jint v8, jint v9,
        jint v10, jint v11, jint v12, jint v13, jint v14,
        jint v15, jint v16, jint v17, jint v18, jint v19,
        jint v20, jint v21, jint v22, jint v23, jint v24,
        jint v25, jint v26, jint v27, jint v28, jint v29,
        jint v30, jint v31, jint v32, jint v33, jint v34,
        jint v35, jint v36, jint v37, jint v38, jint v39,
        jint v40, jint v41, jint v42, jint v43, jint v44,
        jint v45, jint v46, jint v47, jint v48, jint v49,
        jint v50) {
    return (v1 == 1) && (v2 == 2) && (v3 == 3) &&
        (v4 == 4) && (v5 == 5) && (v6 == 6) && (v7 == 7) &&
        (v8 == 8) && (v9 == 9) &&
        (v10 == 10) && (v11 == 11) && (v12 == 12) && (v13 == 13) &&
        (v14 == 14) && (v15 == 15) && (v16 == 16) && (v17 == 17) &&
        (v18 == 18) && (v19 == 19) &&
        (v20 == 20) && (v21 == 21) && (v22 == 22) && (v23 == 23) &&
        (v24 == 24) && (v25 == 25) && (v26 == 26) && (v27 == 27) &&
        (v28 == 28) && (v29 == 29) &&
        (v30 == 30) && (v31 == 31) && (v32 == 32) && (v33 == 33) &&
        (v34 == 34) && (v35 == 35) && (v36 == 36) && (v37 == 37) &&
        (v38 == 38) && (v39 == 39) &&
        (v40 == 40) && (v41 == 41) && (v42 == 42) && (v43 == 43) &&
        (v44 == 44) && (v45 == 45) && (v46 == 46) && (v47 == 47) &&
        (v48 == 48) && (v49 == 49) &&
        (v50 == 50);
}

static JNINativeMethod methods[] = {
    // name, signature, function
    { "nop",               "()V", StaticNonce_nop },
    { "returnBoolean",     "()Z", StaticNonce_returnBoolean },
    { "returnByte",        "()B", StaticNonce_returnByte },
    { "returnShort",       "()S", StaticNonce_returnShort },
    { "returnChar",        "()C", StaticNonce_returnChar },
    { "returnInt",         "()I", StaticNonce_returnInt },
    { "returnLong",        "()J", StaticNonce_returnLong },
    { "returnFloat",       "()F", StaticNonce_returnFloat },
    { "returnDouble",      "()D", StaticNonce_returnDouble },
    { "returnNull",        "()Ljava/lang/Object;", StaticNonce_returnNull },
    { "returnString",      "()Ljava/lang/String;", StaticNonce_returnString },
    { "returnShortArray",  "()[S", StaticNonce_returnShortArray },
    { "returnStringArray", "()[Ljava/lang/String;",
      StaticNonce_returnStringArray },
    { "returnThisClass",   "()Ljava/lang/Class;",
      StaticNonce_returnThisClass },
    { "returnInstance",    "()Landroid/jni/cts/StaticNonce;",
      StaticNonce_returnInstance },
    { "takeBoolean",       "(Z)Z", StaticNonce_takeBoolean },
    { "takeByte",          "(B)Z", StaticNonce_takeByte },
    { "takeShort",         "(S)Z", StaticNonce_takeShort },
    { "takeChar",          "(C)Z", StaticNonce_takeChar },
    { "takeInt",           "(I)Z", StaticNonce_takeInt },
    { "takeLong",          "(J)Z", StaticNonce_takeLong },
    { "takeFloat",         "(F)Z", StaticNonce_takeFloat },
    { "takeDouble",        "(D)Z", StaticNonce_takeDouble },
    { "takeNull",          "(Ljava/lang/Object;)Z", StaticNonce_takeNull },
    { "takeString",        "(Ljava/lang/String;)Z", StaticNonce_takeString },
    { "takeThisClass",     "(Ljava/lang/Class;)Z", StaticNonce_takeThisClass },
    { "takeIntLong",       "(IJ)Z", StaticNonce_takeIntLong },
    { "takeLongInt",       "(JI)Z", StaticNonce_takeLongInt },
    { "takeOneOfEach",     "(ZBSCIJLjava/lang/String;FD[I)Z",
      StaticNonce_takeOneOfEach },
    { "takeCoolHandLuke",
      "(IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII)Z",
      StaticNonce_takeCoolHandLuke },
};

int register_StaticNonce(JNIEnv *env) {
    return jniRegisterNativeMethods(
            env, "android/jni/cts/StaticNonce",
            methods, sizeof(methods) / sizeof(JNINativeMethod));
}
