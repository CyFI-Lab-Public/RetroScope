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

#include "jni/jni_multiclass_pa.h"
#include "native/multiclass_pa.h"

#include <vector>

using learningfw::MulticlassPA;
using std::vector;
using std::pair;

void CreateIndexValuePairs(const int* indices, const float* values,
                           const int length, vector<pair<int, float> >* pairs) {
  pairs->clear();

  for (int i = 0; i < length; ++i) {
    pair<int, float> new_pair(indices[i], values[i]);
    pairs->push_back(new_pair);
  }
}

jint Java_android_bordeaux_learning_MulticlassPA_initNativeClassifier(JNIEnv* env,
                                                       jobject thiz,
                                                       jint num_classes,
                                                       jint num_dims,
                                                       jfloat aggressiveness) {
  MulticlassPA* classifier = new MulticlassPA(num_classes,
                                              num_dims,
                                              aggressiveness);
  return ((jint) classifier);
}


jboolean Java_android_bordeaux_learning_MulticlassPA_deleteNativeClassifier(JNIEnv* env,
                                                             jobject thiz,
                                                             jint paPtr) {
  MulticlassPA* classifier = (MulticlassPA*) paPtr;
  delete classifier;
  return JNI_TRUE;
}

jboolean Java_android_bordeaux_learning_MulticlassPA_nativeSparseTrainOneExample(JNIEnv* env,
                                                                  jobject thiz,
                                                                  jintArray index_array,
                                                                  jfloatArray value_array,
                                                                  jint target,
                                                                  jint paPtr) {
  MulticlassPA* classifier = (MulticlassPA*) paPtr;

  if (classifier && index_array && value_array) {

    jfloat* values = env->GetFloatArrayElements(value_array, NULL);
    jint* indices = env->GetIntArrayElements(index_array, NULL);
    const int value_len = env->GetArrayLength(value_array);
    const int index_len = env->GetArrayLength(index_array);

    if (values && indices && value_len == index_len) {
      vector<pair<int, float> > inputs;

      CreateIndexValuePairs(indices, values, value_len, &inputs);
      classifier->SparseTrainOneExample(inputs, target);
      env->ReleaseIntArrayElements(index_array, indices, JNI_ABORT);
      env->ReleaseFloatArrayElements(value_array, values, JNI_ABORT);

      return JNI_TRUE;
    }
    env->ReleaseIntArrayElements(index_array, indices, JNI_ABORT);
    env->ReleaseFloatArrayElements(value_array, values, JNI_ABORT);
  }

  return JNI_FALSE;
}


jint Java_android_bordeaux_learning_MulticlassPA_nativeSparseGetClass(JNIEnv* env,
                                                       jobject thiz,
                                                       jintArray index_array,
                                                       jfloatArray value_array,
                                                       jint paPtr) {

  MulticlassPA* classifier = (MulticlassPA*) paPtr;

  if (classifier && index_array && value_array) {

    jfloat* values = env->GetFloatArrayElements(value_array, NULL);
    jint* indices = env->GetIntArrayElements(index_array, NULL);
    const int value_len = env->GetArrayLength(value_array);
    const int index_len = env->GetArrayLength(index_array);

    if (values && indices && value_len == index_len) {
      vector<pair<int, float> > inputs;
      CreateIndexValuePairs(indices, values, value_len, &inputs);
      env->ReleaseIntArrayElements(index_array, indices, JNI_ABORT);
      env->ReleaseFloatArrayElements(value_array, values, JNI_ABORT);
      return classifier->SparseGetClass(inputs);
    }
    env->ReleaseIntArrayElements(index_array, indices, JNI_ABORT);
    env->ReleaseFloatArrayElements(value_array, values, JNI_ABORT);
  }

  return -1;
}
