/*
 * Copyright (C) 2011 The Android Open Source Project
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

#ifndef LEARNINGFW_JNI_MULTICLASS_PA_H
#define LEAENINGFW_JNI_MULTICLASS_PA_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_android_bordeaux_learning_MulticlassPA_initNativeClassifier(JNIEnv* env,
                                                  jobject thiz,
                                                  jint num_classes,
                                                  jint num_dims,
                                                  jfloat aggressiveness);


JNIEXPORT jboolean JNICALL
Java_android_bordeaux_learning_MulticlassPA_deleteNativeClassifier(JNIEnv* env,
                                                    jobject thiz,
                                                    jint paPtr);

JNIEXPORT jboolean JNICALL
Java_android_bordeaux_learning_MulticlassPA_nativeSparseTrainOneExample(JNIEnv* env,
                                                         jobject thiz,
                                                         jintArray index_array,
                                                         jfloatArray value_array,
                                                         jint target,
                                                         jint paPtr);

JNIEXPORT jint JNICALL
Java_android_bordeaux_learning_MulticlassPA_nativeSparseGetClass(JNIEnv* env,
                                                  jobject thiz,
                                                  jintArray index_array,
                                                  jfloatArray value_array,
                                                  jint paPtr);


#ifdef __cplusplus
}
#endif

#endif /* ANDROID_LERNINGFW_JNI_MULTICLASS_PA_H */
