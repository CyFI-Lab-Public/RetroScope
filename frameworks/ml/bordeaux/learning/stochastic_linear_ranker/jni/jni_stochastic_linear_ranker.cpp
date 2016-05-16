/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "jni/jni_stochastic_linear_ranker.h"
#include "native/common_defs.h"
#include "native/sparse_weight_vector.h"
#include "native/stochastic_linear_ranker.h"

#include <vector>
#include <string>
using std::string;
using std::vector;
using std::hash_map;
using learning_stochastic_linear::StochasticLinearRanker;
using learning_stochastic_linear::SparseWeightVector;

void CreateSparseWeightVector(JNIEnv* env, const jobjectArray keys, const float* values,
    const int length, SparseWeightVector<string> * sample) {

  for (int i = 0; i < length; ++i) {
    jboolean iscopy;
    jstring s = (jstring) env->GetObjectArrayElement(keys, i);
    const char *key = env->GetStringUTFChars(s, &iscopy);
    sample->SetElement(key, static_cast<double>(values[i]));
    env->ReleaseStringUTFChars(s,key);
  }
}

void ConvertParameter2Object(JNIEnv* env, jobjectArray *keys, jobjectArray *values,
    const char * name , const char * paramValue, int index) {

    jstring jstrK = env->NewStringUTF(name);
    jstring jstrV = env->NewStringUTF(paramValue);
    env->SetObjectArrayElement(*keys, index, jstrK);
    env->SetObjectArrayElement(*values, index, jstrV);
}

void DecomposeSparseWeightVector(JNIEnv* env, jobjectArray *keys, jfloatArray *values,
    const int length, SparseWeightVector<string> *sample) {

  SparseWeightVector<string>::Wmap w_ = sample->GetMap();
  int i=0;
  for ( SparseWeightVector<string>::Witer_const iter = w_.begin();
    iter != w_.end(); ++iter) {
    std::string key = iter->first;
    float value = (float) iter->second;
    jstring jstr = env->NewStringUTF(key.c_str());
    env->SetObjectArrayElement(*keys, i, jstr);
    jfloat s[1];
    s[0] = value;
    env->SetFloatArrayRegion(*values, i, 1, s);
    i++;
  }
}

jboolean Java_android_bordeaux_learning_StochasticLinearRanker_nativeSetWeightClassifier(
    JNIEnv* env,
    jobject thiz,
    jobjectArray key_array_model,
    jfloatArray value_array_model,
    jfloat normalizer_model,
    jint paPtr) {

  StochasticLinearRanker<string>* classifier = (StochasticLinearRanker<string>*) paPtr;
  if (classifier && key_array_model && value_array_model && normalizer_model) {
    const int keys_m_len = env->GetArrayLength(key_array_model);
    jfloat* values_m = env->GetFloatArrayElements(value_array_model, NULL);
    const int values_m_len = env->GetArrayLength(value_array_model);

    if (values_m && key_array_model && values_m_len == keys_m_len) {
      SparseWeightVector<string> model;
      CreateSparseWeightVector(env, key_array_model, values_m, values_m_len, &model);
      model.SetNormalizer(normalizer_model);
      classifier->LoadWeights(model);
      env->ReleaseFloatArrayElements(value_array_model, values_m, JNI_ABORT);
      return JNI_TRUE;
    }
  }
  return JNI_FALSE;
}

jboolean Java_android_bordeaux_learning_StochasticLinearRanker_nativeSetParameterClassifier(
    JNIEnv* env,
    jobject thiz,
    jstring key,
    jstring value,
    jint paPtr) {

  StochasticLinearRanker<string>* classifier = (StochasticLinearRanker<string>*) paPtr;
  jboolean iscopy;
  const char *cKey = env->GetStringUTFChars(key, &iscopy);
  const char *cValue = env->GetStringUTFChars(value, &iscopy);
  float v;
  if (strcmp(cKey, ITR_NUM) == 0){
    sscanf(cValue, "%f", &v);
    classifier->SetIterationNumber((uint64) v);
    return JNI_TRUE;
  }
  else if (strcmp(cKey, NORM_CONSTRAINT) == 0){
    sscanf(cValue, "%f", &v);
    classifier->SetNormConstraint((double) v);
    return JNI_TRUE;
  }
  else if (strcmp(cKey, REG_TYPE) == 0){
    if (strcmp(cValue, REG_TYPE_L0 ) == 0)
      classifier->SetRegularizationType(learning_stochastic_linear::L0);
    else if (strcmp(cValue, REG_TYPE_L1 ) == 0)
      classifier->SetRegularizationType(learning_stochastic_linear::L1);
    else if (strcmp(cValue, REG_TYPE_L2 ) == 0)
      classifier->SetRegularizationType(learning_stochastic_linear::L2);
    else if (strcmp(cValue, REG_TYPE_L1L2 ) == 0)
      classifier->SetRegularizationType(learning_stochastic_linear::L1L2);
    else if (strcmp(cValue, REG_TYPE_L1LInf ) == 0)
      classifier->SetRegularizationType(learning_stochastic_linear::L1LInf);
    else {
      ALOGE("Error: %s is not a Regularization Type", cValue);
      return JNI_FALSE;
    }
    return JNI_TRUE;
  }
  else if (strcmp(cKey, LAMBDA) == 0){
    sscanf(cValue, "%f", &v);
    classifier->SetLambda((double) v);
    return JNI_TRUE;
  }
  else if (strcmp(cKey, UPDATE_TYPE) == 0){
    if (strcmp(cValue, UPDATE_TYPE_FULL_CS) == 0)
      classifier->SetUpdateType(learning_stochastic_linear::FULL_CS);
    else if (strcmp(cValue, UPDATE_TYPE_CLIP_CS) == 0)
      classifier->SetUpdateType(learning_stochastic_linear::CLIP_CS);
    else if (strcmp(cValue, UPDATE_TYPE_REG_CS ) == 0)
      classifier->SetUpdateType(learning_stochastic_linear::REG_CS);
    else if (strcmp(cValue, UPDATE_TYPE_SL) == 0)
      classifier->SetUpdateType(learning_stochastic_linear::SL);
    else if (strcmp(cValue, UPDATE_TYPE_ADAPTIVE_REG) == 0)
      classifier->SetUpdateType(learning_stochastic_linear::ADAPTIVE_REG);
    else {
      ALOGE("Error: %s is not an Update Type", cValue);
      return JNI_FALSE;
    }
    return JNI_TRUE;
  }
  else if (strcmp(cKey, ADAPT_MODE) == 0){
    if (strcmp(cValue, ADAPT_MODE_CONST ) == 0)
      classifier->SetAdaptationMode(learning_stochastic_linear::CONST);
    else if (strcmp(cValue, ADAPT_MODE_INV_LINEAR ) == 0)
      classifier->SetAdaptationMode(learning_stochastic_linear::INV_LINEAR);
    else if (strcmp(cValue, ADAPT_MODE_INV_QUADRATIC ) == 0)
      classifier->SetAdaptationMode(learning_stochastic_linear::INV_QUADRATIC);
    else if (strcmp(cValue, ADAPT_MODE_INV_SQRT ) == 0)
      classifier->SetAdaptationMode(learning_stochastic_linear::INV_SQRT);
    else {
      ALOGE("Error: %s is not an Adaptation Mode", cValue);
      return JNI_FALSE;
    }
    return JNI_TRUE;
  }
  else if (strcmp(cKey, KERNEL_TYPE) == 0){
    if (strcmp(cValue, KERNEL_TYPE_LINEAR ) == 0)
      classifier->SetKernelType(learning_stochastic_linear::LINEAR);
    else if (strcmp(cValue, KERNEL_TYPE_POLY ) == 0)
      classifier->SetKernelType(learning_stochastic_linear::POLY);
    else if (strcmp(cValue, KERNEL_TYPE_RBF ) == 0)
      classifier->SetKernelType(learning_stochastic_linear::RBF);
    else {
      ALOGE("Error: %s is not a Kernel Type", cValue);
      return JNI_FALSE;
    }
    return JNI_TRUE;
  }
  else if (strcmp(cKey, KERNEL_PARAM) == 0){
    sscanf(cValue, "%f", &v);
    classifier->SetKernelParam((double) v);
    return JNI_TRUE;
  }
  else if (strcmp(cKey, KERNEL_GAIN) == 0){
    sscanf(cValue, "%f", &v);
    classifier->SetKernelGain((double) v);
    return JNI_TRUE;
  }
  else if (strcmp(cKey, KERNEL_BIAS) == 0){
    sscanf(cValue, "%f", &v);
    classifier->SetKernelBias((double) v);
    return JNI_TRUE;
  }
  else if (strcmp(cKey, LOSS_TYPE) == 0){
    if (strcmp(cValue, LOSS_TYPE_PAIRWISE ) == 0)
      classifier->SetRankLossType(learning_stochastic_linear::PAIRWISE);
    else if (strcmp(cValue, LOSS_TYPE_RECIPROCAL_RANK ) == 0)
      classifier->SetRankLossType(learning_stochastic_linear::RECIPROCAL_RANK);
    else {
      ALOGE("Error: %s is not a Kernel Type", cValue);
      return JNI_FALSE;
    }
    return JNI_TRUE;
  }
  else if (strcmp(cKey, ACC_PROB) == 0){
    sscanf(cValue, "%f", &v);
    classifier->SetAcceptanceProbability((double) v);
    return JNI_TRUE;
  }
  else if (strcmp(cKey, MIN_BATCH_SIZE) == 0){
    sscanf(cValue, "%f", &v);
    classifier->SetMiniBatchSize((uint64) v);
    return JNI_TRUE;
  }
  else if (strcmp(cKey, GRAD_L0_NORM) == 0){
    sscanf(cValue, "%f", &v);
    classifier->SetGradientL0Norm((int32) v);
    return JNI_TRUE;
  }
  ALOGE("Error: %s is not a ranker parameter", cKey);
  return JNI_FALSE;
}

jint Java_android_bordeaux_learning_StochasticLinearRanker_nativeGetLengthClassifier(
  JNIEnv* env,
  jobject thiz,
  jint paPtr) {

  StochasticLinearRanker<string>* classifier = (StochasticLinearRanker<string>*) paPtr;
  SparseWeightVector<string> M_weights;
  classifier->SaveWeights(&M_weights);

  SparseWeightVector<string>::Wmap w_map = M_weights.GetMap();
  int len = w_map.size();
  return len;
}

std::string ConvertFloat2String(float v){
    std::stringstream converter;
    converter << v;
    return converter.str();
}

void Java_android_bordeaux_learning_StochasticLinearRanker_nativeGetParameterClassifier(
    JNIEnv* env,
    jobject thiz,
    jobjectArray key_array_param,
    jobjectArray value_array_param,
    jint paPtr){

  std::string s;
  StochasticLinearRanker<string>* classifier = (StochasticLinearRanker<string>*) paPtr;
  s = ConvertFloat2String((float) classifier->GetIterationNumber());
  ConvertParameter2Object(env, &key_array_param, &value_array_param, ITR_NUM, s.c_str(), 0 );

  s = ConvertFloat2String((float) classifier->GetNormContraint());
  ConvertParameter2Object(env, &key_array_param, &value_array_param, NORM_CONSTRAINT, s.c_str(), 1 );

  float value = (float) classifier->GetRegularizationType();
  switch ((int) value) {
    case learning_stochastic_linear::L0 :
      s = REG_TYPE_L0;
      break;
    case learning_stochastic_linear::L1 :
      s = REG_TYPE_L1;
      break;
    case learning_stochastic_linear::L2 :
      s = REG_TYPE_L2;
      break;
    case learning_stochastic_linear::L1L2 :
      s = REG_TYPE_L1L2;
      break;
    case learning_stochastic_linear::L1LInf :
      s = REG_TYPE_L1LInf;
      break;
  }
  ConvertParameter2Object(env, &key_array_param, &value_array_param, REG_TYPE, s.c_str(), 2 );

  s = ConvertFloat2String((float) classifier->GetLambda());
  ConvertParameter2Object(env, &key_array_param, &value_array_param, LAMBDA, s.c_str(), 3 );

  value = (float) classifier->GetUpdateType();
  switch ((int) value) {
    case learning_stochastic_linear::FULL_CS :
      s = UPDATE_TYPE_FULL_CS;
      break;
    case learning_stochastic_linear::CLIP_CS :
      s = UPDATE_TYPE_CLIP_CS;
      break;
    case learning_stochastic_linear::REG_CS :
      s = UPDATE_TYPE_REG_CS;
      break;
    case learning_stochastic_linear::SL :
      s = UPDATE_TYPE_SL;
      break;
    case learning_stochastic_linear::ADAPTIVE_REG :
      s = UPDATE_TYPE_ADAPTIVE_REG;
      break;
  }
  ConvertParameter2Object(env, &key_array_param, &value_array_param, UPDATE_TYPE, s.c_str(), 4 );

  value = (float) classifier->GetAdaptationMode();
  switch ((int) value) {
    case learning_stochastic_linear::CONST :
      s = ADAPT_MODE_CONST;
      break;
    case learning_stochastic_linear::INV_LINEAR :
      s = ADAPT_MODE_INV_LINEAR;
      break;
    case learning_stochastic_linear::INV_QUADRATIC :
      s = ADAPT_MODE_INV_QUADRATIC;
      break;
    case learning_stochastic_linear::INV_SQRT :
      s = ADAPT_MODE_INV_SQRT;
      break;
  }
  ConvertParameter2Object(env, &key_array_param, &value_array_param, ADAPT_MODE, s.c_str(), 5 );

  value = (float) classifier->GetKernelType();
  switch ((int) value) {
    case learning_stochastic_linear::LINEAR :
      s = KERNEL_TYPE_LINEAR;
      break;
    case learning_stochastic_linear::POLY :
      s = KERNEL_TYPE_POLY;
      break;
    case learning_stochastic_linear::RBF :
      s = KERNEL_TYPE_RBF;
      break;
  }
  ConvertParameter2Object(env, &key_array_param, &value_array_param, KERNEL_TYPE, s.c_str(), 6 );

  s = ConvertFloat2String((float) classifier->GetKernelParam());
  ConvertParameter2Object(env, &key_array_param, &value_array_param, KERNEL_PARAM, s.c_str(), 7 );

  s = ConvertFloat2String((float) classifier->GetKernelGain());
  ConvertParameter2Object(env, &key_array_param, &value_array_param, KERNEL_GAIN, s.c_str(), 8 );

  s = ConvertFloat2String((float)classifier->GetKernelBias());
  ConvertParameter2Object(env, &key_array_param, &value_array_param, KERNEL_BIAS, s.c_str(), 9 );

  value = (float) classifier->GetRankLossType();
  switch ((int) value) {
    case learning_stochastic_linear::PAIRWISE :
      s = LOSS_TYPE_PAIRWISE;
      break;
    case learning_stochastic_linear::RECIPROCAL_RANK :
      s = LOSS_TYPE_RECIPROCAL_RANK;
      break;
  }
  ConvertParameter2Object(env, &key_array_param, &value_array_param, LOSS_TYPE, s.c_str(), 10 );

  s = ConvertFloat2String((float) classifier->GetAcceptanceProbability());
  ConvertParameter2Object(env, &key_array_param, &value_array_param, ACC_PROB, s.c_str(), 11 );

  s = ConvertFloat2String((float) classifier->GetMiniBatchSize());
  ConvertParameter2Object(env, &key_array_param, &value_array_param, MIN_BATCH_SIZE, s.c_str(), 12 );

  s = ConvertFloat2String((float) classifier->GetGradientL0Norm());
  ConvertParameter2Object(env, &key_array_param, &value_array_param, GRAD_L0_NORM, s.c_str(), 13 );
}

void Java_android_bordeaux_learning_StochasticLinearRanker_nativeGetWeightClassifier(
  JNIEnv* env,
  jobject thiz,
  jobjectArray key_array_model,
  jfloatArray value_array_model,
  jfloat normalizer,
  jint paPtr) {

  StochasticLinearRanker<string>* classifier = (StochasticLinearRanker<string>*) paPtr;
  SparseWeightVector<string> M_weights;
  classifier->SaveWeights(&M_weights);
  SparseWeightVector<string>::Wmap w_map = M_weights.GetMap();
  int array_len = w_map.size();

  normalizer = M_weights.GetNormalizer();
  DecomposeSparseWeightVector(env, &key_array_model, &value_array_model, array_len, &M_weights);
}

jint Java_android_bordeaux_learning_StochasticLinearRanker_initNativeClassifier(JNIEnv* env,
                             jobject thiz) {
  StochasticLinearRanker<string>* classifier = new StochasticLinearRanker<string>();
  return ((jint) classifier);
}

jboolean Java_android_bordeaux_learning_StochasticLinearRanker_deleteNativeClassifier(JNIEnv* env,
                               jobject thiz,
                               jint paPtr) {
  StochasticLinearRanker<string>* classifier = (StochasticLinearRanker<string>*) paPtr;
  delete classifier;
  return JNI_TRUE;
}

jboolean Java_android_bordeaux_learning_StochasticLinearRanker_nativeUpdateClassifier(
  JNIEnv* env,
  jobject thiz,
  jobjectArray key_array_positive,
  jfloatArray value_array_positive,
  jobjectArray key_array_negative,
  jfloatArray value_array_negative,
  jint paPtr) {
  StochasticLinearRanker<string>* classifier = (StochasticLinearRanker<string>*) paPtr;

  if (classifier && key_array_positive && value_array_positive &&
      key_array_negative && value_array_negative) {

    const int keys_p_len = env->GetArrayLength(key_array_positive);
    jfloat* values_p = env->GetFloatArrayElements(value_array_positive, NULL);
    const int values_p_len = env->GetArrayLength(value_array_positive);
    jfloat* values_n = env->GetFloatArrayElements(value_array_negative, NULL);
    const int values_n_len = env->GetArrayLength(value_array_negative);
    const int keys_n_len = env->GetArrayLength(key_array_negative);

    if (values_p && key_array_positive && values_p_len == keys_p_len &&
      values_n && key_array_negative && values_n_len == keys_n_len) {

      SparseWeightVector<string> sample_pos;
      SparseWeightVector<string> sample_neg;
      CreateSparseWeightVector(env, key_array_positive, values_p, values_p_len, &sample_pos);
      CreateSparseWeightVector(env, key_array_negative, values_n, values_n_len, &sample_neg);
      classifier->UpdateClassifier(sample_pos, sample_neg);
      env->ReleaseFloatArrayElements(value_array_negative, values_n, JNI_ABORT);
      env->ReleaseFloatArrayElements(value_array_positive, values_p, JNI_ABORT);

      return JNI_TRUE;
    }
    env->ReleaseFloatArrayElements(value_array_negative, values_n, JNI_ABORT);
    env->ReleaseFloatArrayElements(value_array_positive, values_p, JNI_ABORT);
  }
  return JNI_FALSE;
}

jfloat Java_android_bordeaux_learning_StochasticLinearRanker_nativeScoreSample(
  JNIEnv* env,
  jobject thiz,
  jobjectArray key_array,
  jfloatArray value_array,
  jint paPtr) {

  StochasticLinearRanker<string>* classifier = (StochasticLinearRanker<string>*) paPtr;

  if (classifier && key_array && value_array) {

    jfloat* values = env->GetFloatArrayElements(value_array, NULL);
    const int values_len = env->GetArrayLength(value_array);
    const int keys_len = env->GetArrayLength(key_array);

    if (values && key_array && values_len == keys_len) {
      SparseWeightVector<string> sample;
      CreateSparseWeightVector(env, key_array, values, values_len, &sample);
      env->ReleaseFloatArrayElements(value_array, values, JNI_ABORT);
      return classifier->ScoreSample(sample);
    }
  }
  return -1;
}
